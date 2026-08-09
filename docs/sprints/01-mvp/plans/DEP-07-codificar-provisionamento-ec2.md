# DEP-07 — Codificar o provisionamento da EC2 (bootstrap versionado no user_data)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** observação do humano após o incidente do disco cheio — a configuração feita à mão na EC2 (Caddy, journald, `finbot.service`, keystore) não está em código e se perderia num recreate. Decisão de estratégia: **ADR 0009**.
> - **Prioridade:** alta — hoje a EC2 **não é recriável de forma confiável**; um recreate (planejado ou forçado) deixaria o serviço quebrado.
> - **Esforço:** médio (escrever um script idempotente que cubra tudo + religar o `user_data`; o cuidado é não recriar a instância no apply).
> - **Território / quem executa:** `financas_bot_telegram/infra/` → **Claude do back**.
> - **Branch:** `feature/dep-07-codificar-provisionamento-ec2`, a partir de `develop`.
> - **Dependências:** conceitual — captura a config do **DEP-03** (Caddy/Caddyfile) e da **FIX-crescer-volume-ec2** (cap do journald). Se esses ainda não estiverem finalizados, capturar a config *pretendida* deles. Idealmente escrever depois que ambos assentarem.
> - **Riscos:**
>   1. **Mudar `user_data` disparar replace da instância.** No provider AWS, alterar `user_data` é normalmente *update in-place* (metadata) e **não** re-executa nem recria — mas **confirmar no `terraform plan`** que é `~`, não `-/+`. Se vier replace, PARAR.
>   2. **Script não-idempotente quebrar um recreate.** Mitigação: check-before-install em cada passo; rodar `shellcheck`.
>   3. **Keystore do webhook** é difícil de reproduzir (cert muda → re-registrar no Telegram). Ver decisão abaixo.

---

## Contexto

Por ADR 0009, a config da EC2 passa a ser codificada. Hoje só Java + usuário `finbot` + `/opt/finbot` estão no `user_data`. **Não codificado** (sumiria num recreate):

- `finbot.service` — a unit systemd do app (instalada manualmente; o `deploy.yml` só dá `restart`).
- **Caddy** — binário ARM64 + `Caddyfile` + unit systemd (proxy reverso do DEP-03).
- **journald** — `SystemMaxUse=200M` em `/etc/systemd/journald.conf` (FIX do disco).
- **keystore.p12** — cert self-signed do webhook do Telegram em `/opt/finbot/`.

> **Escopo (ADR 0009):** codificar pra que o **próximo recreate** nasça pronto. **Não** reconciliar a instância atual agora (decisão do humano). O script não será rodado na máquina viva nesta task.

## Decisão / abordagem

**Criar `infra/provision/bootstrap.sh` (idempotente) com todo o provisionamento, e ligá-lo ao `user_data` do `aws_instance.finbot_app` via `templatefile()`** — assim dá pra interpolar variáveis (ex.: `domain_name` pro `Caddyfile`). Arquivos de config (Caddyfile, unit do finbot, journald) ficam como templates versionados no repo.

### Tratamento do keystore do webhook (ponto a decidir na execução)
O keystore self-signed é o item chato. Opções:
- **(a) Regenerar no boot** via `keytool`/`openssl` no script — reproduz, mas o cert muda, então o `setWebhook` do Telegram precisa ser re-registrado com o novo cert (passo manual pós-recreate; documentar no runbook).
- **(b) Guardar o keystore no Secrets Manager/S3** e o script baixa no boot — cert estável, mas adiciona um segredo a gerir.
- **(c, recomendada a médio prazo) Aposentar o keystore:** migrar o webhook pra trás do Caddy + Let's Encrypt (`bot.satyansaita.com`), eliminando a 8443/self-signed (débito em `PENDENCIAS-TECNICAS.md`). Aí o provisionamento do webhook vira "só Caddy", reproduzível de graça.

Pra DEP-07, recomendo **(a)** (regenerar no boot + nota de re-registrar o webhook), e registrar **(c)** como o caminho que torna isso trivial — fechar quando a migração do webhook entrar.

## Escopo / arquivos

**Criar (`financas_bot_telegram/infra/`):**

- `provision/bootstrap.sh` — script idempotente: instala Java (se ausente), cria usuário/pasta, instala Caddy (binário ARM64 + unit), escreve o `Caddyfile`, instala a unit `finbot.service`, aplica `SystemMaxUse` no journald, trata o keystore (opção a). Cada passo com guarda de idempotência.
- `provision/Caddyfile.tftpl` (ou inline) — template do Caddy com `api.${domain_name}`.
- `provision/finbot.service` — a unit systemd versionada.
- (opcional) `provision/journald.conf.d/finbot.conf` — drop-in com `SystemMaxUse=200M`.

**Modificar:**

- `ec2.tf` — `user_data = templatefile("${path.module}/provision/bootstrap.sh", { domain_name = var.domain_name, ... })`. Manter `lifecycle { ignore_changes = [ami] }`.

**Não tocar:** código de produto; nada de `frontend/`. Não rodar o script na instância atual (fora do escopo).

## Critérios de aceitação

- [ ] `infra/provision/bootstrap.sh` cobre **todos** os itens manuais (finbot.service, Caddy+Caddyfile+unit, journald, keystore) e passa no `shellcheck`.
- [ ] Script é **idempotente** — rodar duas vezes não duplica nem quebra (revisar cada passo).
- [ ] `ec2.tf` usa `templatefile()` pro `user_data`; `terraform plan` mostra **update in-place** (`~`), **sem** replace da instância. (Gate de segurança — se vier replace, abortar.)
- [ ] Verificação real (forte, recomendada): subir uma **instância descartável** com esse `user_data` (ou um `terraform plan`/`apply` num workspace de teste) e confirmar que ela boota com Caddy ativo, `finbot.service` instalado e journald capado — **sem SSH manual**. Se não for viável, no mínimo revisão linha a linha + shellcheck.
- [ ] Documentado no status report que a **instância atual não foi reconciliada** (escopo ADR 0009) e o que um recreate exigiria de manual residual (ex.: re-registrar webhook se keystore regenerado; primeiro deploy pra colocar o JAR).
- [ ] Status report em `docs/sprints/01-mvp/status/DEP-07.md` com frontmatter válido.

## Coordenação

- **Depende de DEP-03 e FIX-crescer-volume-ec2** pra capturar a config final do Caddy e do journald — se ainda não mergeados, capturar a config pretendida e alinhar com o planner.
- **`user_data` não afeta a instância viva** — esta task não muda nada no servidor de hoje; só prepara o próximo nascimento.
- Sincronia futura é disciplina: **todo novo passo manual no servidor deve ser refletido no `bootstrap.sh`** (regra que vale a pena ecoar no `CLAUDE.md` quando isso assentar).

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build/lint/testes = `na`; `shellcheck` + `terraform plan` in-place + a verificação da instância descartável são a evidência), status report válido, e **revisão independente pelo Reviewer** — toca o provisionamento de produção e tem risco de replace (alto risco). Abrir PR pra `develop`; não mergear sozinho.

## Referências

- ADR `0009` (`docs/decisions/0009-provisionamento-ec2-codificado.md`) — a estratégia.
- `docs/plans/DEP-03-api-subdominio-proxy.md` (Caddy a capturar) · `docs/plans/FIX-crescer-volume-ec2.md` (journald a capturar).
- `financas_bot_telegram/infra/ec2.tf` (user_data atual, mínimo).
- `docs/PENDENCIAS-TECNICAS.md` (migrar webhook pra Let's Encrypt — caminho que simplifica o keystore).
</content>
