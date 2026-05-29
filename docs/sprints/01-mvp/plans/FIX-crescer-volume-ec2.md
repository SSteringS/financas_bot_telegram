# FIX — Crescer o volume raiz da EC2 (2 GB → 10 GB gp3)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** incidente de deploy (2026-05). O pipeline do back falhou no passo `sudo mv /tmp/app.jar /opt/finbot/app.jar` com `No space left on device`. Diagnóstico: o volume raiz da EC2 tem **2 GB** (`lsblk`: `nvme0n1` 2G, partição única `nvme0n1p1` em `/`) — pequeno demais pra app Java + logs + cache de pacotes. **Não há espaço escondido pra recuperar** (a partição já ocupa o disco inteiro).
> - **Prioridade:** alta — **deploy do back está bloqueado** enquanto o disco estiver cheio. (Sem outage: o `mv` falhou antes do `restart`, então a app segue rodando o JAR antigo.)
> - **Esforço:** baixo-médio (Terraform trivial + 2 comandos na instância; o cuidado é não recriar a instância).
> - **Território / quem executa:** `financas_bot_telegram/infra/ec2.tf` (Terraform) + comandos na própria EC2 via SSH → **Claude do back** + **humano** (quem tem o SSH roda os comandos na instância).
> - **Branch:** `fix/crescer-volume-ec2`, a partir de `develop`.
> - **Dependências:** nenhuma.
> - **Riscos:**
>   1. **Recriar a instância de produção por engano.** Aumentar `volume_size` é *in-place*; mas adicionar `encrypted` (se o volume atual é não-criptografado) ou certas mudanças **forçam replace** → destrói a EC2. Mitigação: **rodar `terraform plan` e confirmar `~` (update in-place), nunca `-/+` (replace)**. Se aparecer replace, PARAR.
>   2. **`growpart` falhar por disco 100% cheio.** Precisa de uns MB livres pra reescrever a tabela de partição. Mitigação: liberar espaço antes (já feito na mitigação imediata — ver abaixo).
>   3. **Janela de 6 h da AWS** pra modificar o mesmo volume. Mitigação: acertar 10 GB de uma vez (não fatiar em 2 aumentos).

---

## Mitigação imediata (já aplicada / aplicável agora, sem esperar o plano)

Pra desbloquear o deploy na hora, liberar um respiro na instância:

```bash
sudo rm -f /tmp/app.jar
sudo journalctl --vacuum-size=50M
sudo dnf clean all
df -h /
```

Isso resolve o sintoma temporariamente. O FIX abaixo resolve a causa (volume pequeno).

## Contexto — as três camadas

"Aumentar o disco" mexe em três coisas empilhadas, e crescer uma não cresce as de cima:

1. **Volume EBS** (o disco virtual na AWS) — hoje 2 GB; é o que o Terraform aumenta.
2. **Partição** (`nvme0n1p1`) — continua marcada como 2 GB até `growpart`.
3. **Filesystem** (xfs, no AL2023) — precisa de `xfs_growfs` pra enxergar o espaço novo.

Tudo é **online** — sem reboot, sem detach, app continua de pé.

## Decisão / abordagem

**Crescer o volume raiz pra 10 GB, em gp3, via Terraform; depois `growpart` + `xfs_growfs` na instância; e capar o journald pra não reencher.**

### Por que 10 GB (e não 20)
Paga-se pelo tamanho **provisionado**, não pelo usado. O footprint real (OS ~2 GB + Java ~0,3 GB + JAR ~80 MB + logs capados + cache dnf) fica em ~3–4 GB steady-state. 10 GB dá ~6 GB de folga pra logs, `dnf update` e o JAR duplo do deploy, custando ~US$ 0,80/mês (gp3, us-east-1) — ~US$ 0,64/mês a mais que os 2 GB de hoje. 20 GB seria folga demais pra esse uso (~US$ 1,60/mês). 8 GB é o piso aceitável; 10 GB é o ponto frugal-mas-seguro. Não fatiar em aumentos pequenos por causa da janela de 6 h da AWS.

### Por que gp3
Se o volume atual for gp2, migrar pra gp3 é ~20% mais barato por GB **e** entrega 3000 IOPS / 125 MB-s de baseline de graça (gp2 amarra IOPS ao tamanho — um volume de 2 GB gp2 tem IOPS péssimo). Mudança gp2→gp3 é in-place. Confirmar o tipo atual com `aws ec2 describe-volumes`; se já for gp3, manter.

## Escopo / arquivos

**Terraform (`financas_bot_telegram/infra/ec2.tf`):** adicionar o bloco no `aws_instance.finbot_app`:

```hcl
  root_block_device {
    volume_size = 10
    volume_type = "gp3"
  }
```

**Não** adicionar `encrypted` (forçaria replace se o volume atual for não-criptografado). Manter o `lifecycle { ignore_changes = [ami] }` existente.

**Na instância (SSH — humano/back; documentar no status report):**

```bash
# depois do terraform apply e do volume sair de 'creating'
sudo growpart /dev/nvme0n1 1     # estende a partição 1 no espaço novo
sudo xfs_growfs /                # estende o filesystem xfs (cresce pelo mountpoint)
df -h /                          # deve mostrar ~10G
```

**Prevenção — capar o journald** (`/etc/systemd/journald.conf`):

```
SystemMaxUse=200M
```
```bash
sudo systemctl restart systemd-journald
```

**Não tocar:** nada de código de produto. Rotação de log da app (logback) fica como follow-up opcional (mudança na config da app, não nesta task de infra).

## Critérios de aceitação

- [ ] `terraform plan` mostra **update in-place** do volume (`~`), **sem** destroy/replace da instância. (Gate de segurança — se mostrar replace, abortar e replanejar.)
- [ ] `terraform apply` ok; volume vai pra 10 GB.
- [ ] `sudo growpart /dev/nvme0n1 1` e `sudo xfs_growfs /` rodados; `df -h /` mostra ~10 GB com bastante espaço livre.
- [ ] Volume é gp3 (confirmado via `describe-volumes` ou console).
- [ ] `journald.conf` com `SystemMaxUse=200M` aplicado; `journalctl --disk-usage` respeitando o teto após restart.
- [ ] **Deploy do back re-disparado e verde** — o `mv`/`restart` completa e `systemctl is-active finbot` retorna `active`.
- [ ] App sem downtime durante o processo (continuou no JAR antigo até o redeploy).
- [ ] Status report em `docs/status/FIX-crescer-volume-ec2.md` com frontmatter válido, incluindo os comandos rodados na instância e o `df -h` antes/depois.

## Coordenação

- **Humano:** o `terraform apply` e os comandos `growpart`/`xfs_growfs`/journald rodam contra produção — quem tem credencial/SSH executa. O Claude do back prepara o diff do `ec2.tf` e o roteiro; o humano aplica (ou autoriza).
- **Ordem obrigatória:** liberar espaço (mitigação imediata) → `terraform apply` → esperar o volume → `growpart` → `xfs_growfs` → re-deploy.
- Sem dependência com DEP-03/04, mas desbloqueia qualquer deploy do back daqui pra frente.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build/lint/testes = `na`; a evidência é o `terraform plan` in-place limpo + `df -h` pós-resize + deploy verde), status report válido, e **revisão independente pelo Reviewer** — toca infra de produção e tem risco de replace de instância (alto risco, revisão obrigatória). Abrir PR pra `develop`; não mergear sozinho.

## Referências

- Incidente: deploy do back, `No space left on device` no `mv` pra `/opt/finbot/app.jar`.
- `financas_bot_telegram/infra/ec2.tf` (instância sem `root_block_device` → usava o default de 2 GB da AMI).
- `docs/runbooks/RUNBOOK-disco-cheio-ec2.md` (roteiro de triagem rápida, se existir/quando for criado).
- AWS: "Request modifications to your EBS volumes" e "Extend a Linux file system after resizing a volume" (`growpart` + `xfs_growfs`).
</content>
