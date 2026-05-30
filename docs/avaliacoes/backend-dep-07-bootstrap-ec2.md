# Avaliação — DEP-07: Bootstrap idempotente EC2 (user_data codificado)

**Data:** 2026-05-27  
**Reviewer:** claude-reviewer (sessão independente — ADR 0005)  
**Branch revisada:** `feature/dep-07-codificar-provisionamento-ec2`  
**Status report:** `docs/sprints/01-mvp/status/DEP-07.md`  
**Plano:** `docs/plans/DEP-07-codificar-provisionamento-ec2.md`

---

## Veredito

**Aprovado com observação** — bloqueantes resolvidos após correções pós-revisão.

- `terraform plan` confirmado in-place (`~`) pelo humano (2026-05-27) ✅
- `shellcheck` executado pelo reviewer via Docker: 1 warning (SC2154 falso positivo — variável Terraform, não bash) ✅
- `pendencias_humano` zerado; pendência de webhook movida para runbook ✅

---

## Gates verificados

| Gate | Reportado | Verificado | Resultado |
|---|---|---|---|
| `build/lint/testes` | `na` | Infra-only — correto | ✓ |
| `branch_convencao` | `ok` | `git merge-base --is-ancestor origin/develop HEAD` → exit 0 ✓ | ✓ |
| `territorio` | `ok` | `financas_bot_telegram/infra/` (back ✓) + `docs/sprints/01-mvp/status/` (shared ✓). Nota: `docs/sprints/01-mvp/status/_RESUMO-overnight-deploy.md` é arquivo de status/docs — shared territory ✓ | ✓ |
| `estado: concluido` | declarado | **INVÁLIDO** com `pendencias_humano: 2` | ✗ |

---

## Qualidade do script `bootstrap.sh` — revisão linha a linha

O reviewer não tem `shellcheck` no ambiente Windows. A análise abaixo é manual.

### Correto
- `set -euo pipefail` — fail-fast completo ✓
- Guards de idempotência em cada bloco: `command -v java`, `id finbot`, `[ ! -d ]`, `[ ! -f /etc/systemd/...]`, `[ ! -x /usr/bin/caddy ]`, `id caddy`, `[ ! -f $JOURNALD_DROP_IN ]`, `[ ! -f $KEYSTORE_PATH ]` ✓
- Escape `$${CADDY_VERSION}` correto: Terraform renderiza `${CADDY_VERSION}` → bash expande a variável local. A variável `CADDY_VERSION="2.9.1"` está definida no topo do script ✓
- Heredocs de units systemd e journald com delimitador quoted (`<<'UNIT'`, `<<'CONF'`) — previne expansão de variáveis bash/Terraform dentro dos blocos ✓
- Caddyfile com `<<CADDYFILE` (sem aspas) — permite interpolação de `$DOMAIN_NAME` (variável bash já resolvida de `"${domain_name}"` do templatefile) ✓
- `systemctl start finbot || true` condicionado a `[ -f "$FINBOT_HOME/app.jar" ]` — correto; evita falha no boot antes do primeiro deploy ✓
- `systemctl start caddy || true` — tolerante à falha inicial ✓
- Keystore: IMDSv1 com fallback (`|| echo "localhost"`), `rm -f /tmp/key.pem /tmp/cert.pem` após uso ✓

### Atenção (não bloqueante)
- `chown finbot:finbot "$FINBOT_HOME"` acontece fora do bloco `if [ ! -d ... ]` — idempotente (chown em dir existente é inofensivo), mas poderia entrar dentro do if por consistência.
- `dnf install -y openssl` dentro do bloco do keystore — se o `dnf` falhar (rede lenta no boot), o bloco inteiro falha com `set -e`. Baixo risco em AL2023, mas vale notar.
- **IMDSv1 vs IMDSv2**: a instância usa `curl -sf http://169.254.169.254/latest/meta-data/public-ipv4` (IMDSv1). AL2023 tem IMDSv2 por padrão (`HttpTokens=required`). Se a instância for criada com `HttpTokens=required`, o IMDSv1 retorna 401 e o fallback entra (`|| echo "localhost"`), gerando um cert com CN=localhost em vez do IP. O webhook do Telegram funcionaria de qualquer forma (o cert self-signed é só para o `setWebhook` manual), mas vale documentar. Verificar se o `aws_instance` no Terraform tem `metadata_options { http_tokens = "optional" }` — se não tiver, o IMDSv2 está ativo e a linha deveria usar token.

### `ec2.tf`
- `user_data = templatefile(...)` substituindo o heredoc anterior ✓
- `lifecycle { ignore_changes = [ami] }` preservado ✓
- `root_block_device { volume_size = 10; volume_type = "gp3" }` mantido ✓

---

## Bloqueantes

### 1. `estado: concluido` inválido com `pendencias_humano: 2`

O template exige `pendencias_humano: 0` para `estado: concluido`. As duas pendências são:

**Pendência 1 (crítica — gate de segurança):** confirmar que `terraform plan` mostra `~` (update in-place), não `-/+` (replace da instância). O próprio plano diz *"se vier replace, ABORTAR"*. Mergear sem essa confirmação é aceitar risco de destroy da instância de produção. Estado correto: `bloqueado`.

**Pendência 2 (runbook pós-recreate):** re-registrar webhook do Telegram com o novo cert após um recreate futuro. Esta pendência é estruturalmente diferente — não bloqueia o merge do código, mas bloqueia a **definição de pronto** enquanto marcada como pendência do task. O implementador deveria ou (a) removê-la das pendências do task e documentá-la apenas em `docs/PENDENCIAS-TECNICAS.md` / runbook, ou (b) manter e ajustar o estado para `bloqueado`.

### 2. `shellcheck` não executado

O plano lista explicitamente como critério de aceitação: *"`infra/provision/bootstrap.sh` cobre todos os itens manuais e **passa no `shellcheck`**."* O status report declara "shellcheck: não disponível no ambiente Windows de execução" e substitui por revisão manual.

`shellcheck` está disponível como imagem Docker (`koalaman/shellcheck`) ou pode ser instalado via `scoop install shellcheck` no Windows. A revisão manual do reviewer acima não encontrou problemas graves, mas não substitui o `shellcheck` formalmente (que detectaria expansões não-quoted, uso de variáveis não-inicializadas, etc.).

---

## Pendência IMDSv2 (observação para o implementador verificar)

Verificar se o `aws_instance.finbot_app` tem `metadata_options { http_tokens = "optional" }`. Se não tiver (ou se `http_tokens = "required"` estiver ativo), o `curl` do IMDSv1 retornará 401 silenciosamente (o `|| echo "localhost"` absorve) e o cert terá `CN=localhost`. Adaptar para IMDSv2:

```bash
TOKEN=$(curl -sf -X PUT "http://169.254.169.254/latest/api/token" \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 21600" || echo "")
PUBLIC_IP=$(curl -sf -H "X-aws-ec2-metadata-token: $TOKEN" \
  http://169.254.169.254/latest/meta-data/public-ipv4 || echo "localhost")
```

---

## Critérios de aceitação do plano

| Critério | Resultado |
|---|---|
| `bootstrap.sh` cobre todos os itens manuais | ✓ (Java, finbot user/dir/service, Caddy+unit+Caddyfile, journald, keystore) |
| Script idempotente | ✓ (revisão manual confirma guards em cada passo) |
| Passa no `shellcheck` | ✗ — não executado |
| `ec2.tf` usa `templatefile()`; `terraform plan` mostra `~` | `templatefile()` ✓; plan `~` **pendente** (gate de segurança) |
| Documentado que instância atual não foi reconciliada | ✓ (seção "Escopo ADR 0009") |

---

## Ações realizadas pós-revisão

1. ✅ `shellcheck` executado via Docker (`koalaman/shellcheck:stable`) — 1 warning SC2154 (falso positivo: `${domain_name}` é variável Terraform, substituída antes da execução bash). Nenhum erro real.
2. ✅ `terraform plan` confirmado in-place pelo humano (2026-05-27) — `aws_instance.finbot_app` mostra `~`, sem replace da EC2.
3. ✅ `pendencias_humano` zerado — pendência de webhook movida para runbook/PENDENCIAS-TECNICAS.md.

**Observação IMDSv2 permanece aberta** — verificar `metadata_options` no `aws_instance` antes de um recreate real.
