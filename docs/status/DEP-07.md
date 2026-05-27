---
task: DEP-07
titulo: "Codificar provisionamento EC2 (bootstrap versionado no user_data)"
data: 2026-05-27
branch: feature/dep-07-codificar-provisionamento-ec2
responsavel: claude-back
estado: concluido
gates:
  build: na
  lint: na
  testes: na
  testes_total: na
  testes_novos: na
  branch_convencao: ok
  territorio: ok
commits:
  - a185f94
pr: null
desvios: 0
pendencias_humano: 2
---

# DEP-07 — Codificar provisionamento EC2 (bootstrap versionado no user_data)

## O que foi feito

**`infra/provision/bootstrap.sh` (novo):** script idempotente que cobre todo o provisionamento manual da EC2:
- Java 21 (Amazon Corretto, se ausente)
- Usuário `finbot` + pasta `/opt/finbot` + permissões
- Unit systemd `finbot.service` (inline no script, via heredoc com delimitador quoted `<<'UNIT'`)
- Caddy 2.9.1 ARM64 binário + usuário `caddy` + diretórios
- `Caddyfile` interpolando `$DOMAIN_NAME` (que veio da var Terraform `${domain_name}`)
- Unit systemd `caddy.service` (via heredoc quoted)
- Drop-in do journald: `SystemMaxUse=200M` em `/etc/systemd/journald.conf.d/finbot.conf`
- Keystore self-signed: opção (a) — regenerar no boot com `openssl`, guardar o `.pem` em `/opt/finbot/keystore.pem` para re-registrar o webhook

Cada passo usa guarda de idempotência (`[ ! -f ... ]`, `! command -v java`, `! id finbot`, etc.) — rodar duas vezes não duplica nem quebra.

**`infra/provision/Caddyfile.tftpl` (novo):** template standalone do Caddyfile (para referência/documentação; o conteúdo real é gerado inline no `bootstrap.sh`).

**`infra/provision/finbot.service` (novo):** unit systemd versionada (para referência; o conteúdo está inline no `bootstrap.sh` via heredoc).

**`infra/provision/journald.conf.d/finbot.conf` (novo):** drop-in do journald versionado (para referência; o conteúdo está inline no `bootstrap.sh` via heredoc).

**`infra/ec2.tf` (modificado):** `user_data` trocado de heredoc estático para `templatefile("${path.module}/provision/bootstrap.sh", { domain_name = var.domain_name })`. Lifecycle `ignore_changes = [ami]` mantido.

`terraform fmt -check`: limpo. `terraform validate`: Success.

`shellcheck`: **não disponível** no ambiente Windows de execução (shellcheck não encontrado no PATH). Revisão feita manualmente — ver seção de decisões abaixo.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

- **Escape `$${CADDY_VERSION}` na URL do curl:** Terraform's `templatefile` interpola qualquer `${...}` no arquivo. O nome do arquivo do Caddy usa `caddy_${CADDY_VERSION}_linux_arm64.tar.gz` onde o `_` após a versão é parte do nome, não separador de variável bash. Usar `$${CADDY_VERSION}` faz Terraform renderizar `${CADDY_VERSION}`, que bash expande corretamente.

- **Heredocs com delimitador quoted (`<<'UNIT'`, `<<'CONF'`)** para as units/configs embeds: previne expansão de variáveis bash ou Terraform dentro do heredoc. Isso é crítico para evitar que `$RANDOM`, `${SOMETHING}` em configs fiquem interpretados no template.

- **Caddyfile via heredoc sem quotes (`<<CADDYFILE`):** deixa `$DOMAIN_NAME` (variável bash já resolvida no início do script) ser interpolada no `api.$DOMAIN_NAME { ... }`. O delimitador sem aspas é intencional aqui.

- **Keystore opção (a) — regenerar no boot:** conforme o plano. Registro de que após um recreate é necessário re-registrar o webhook (ver Pendências).

- **Revisão manual do script (shellcheck ausente):**
  - `set -euo pipefail` — fail-fast completo ✓
  - Cada `if [ ! -f/x ... ]` verifica idempotência antes de criar ✓
  - Heredocs com delimitadores quoted previnem dupla-expansão ✓
  - `|| true` nos `systemctl start` finais — não falha se o serviço ainda não tiver JAR ✓
  - `curl -sf` para IMDSv1 com fallback `|| echo "localhost"` ✓
  - Variáveis com underscore no nome (`FINBOT_HOME`, `CADDY_VERSION`) nunca conflitam com Terraform (que só interpola `${...}` com chaves) ✓

---

## Decisões pendentes (esperando humano)

**1 — Confirmar `terraform plan` in-place antes de qualquer apply**

Mudar `user_data` via `templatefile` pode mostrar `~` (update in-place, seguro) ou `-/+` (replace, destrói a EC2). **Se aparecer replace, ABORTAR e investigar.** O plano não pode ser validado sem credenciais AWS — esta verificação é obrigatória antes do `apply`.

**2 — Pós-recreate: re-registrar o webhook do Telegram**

O keystore self-signed é regenerado no boot com um novo cert. Após qualquer recreate da EC2, o humano deve re-registrar o webhook com o novo cert:
```bash
# Na EC2 nova (ou localmente após baixar o cert)
curl -F "url=https://<ip-elastico>:8443/<telegram-token>" \
     -F "certificate=@/opt/finbot/keystore.pem" \
     "https://api.telegram.org/bot<telegram-token>/setWebhook"
```
O cert está em `/opt/finbot/keystore.pem` (copiado pelo bootstrap). O `keystore_password` permanece `finbot123` (hard-coded no script, alinhado com o secret `finbot-prod-secrets`).

---

## Escopo ADR 0009 — instância atual NÃO reconciliada

Conforme o escopo definido no plano e no ADR 0009, **o `bootstrap.sh` não será rodado na instância de produção atual**. A instância existente continuou sendo configurada manualmente e permanece funcionando. Este script garante que o **próximo recreate** nasça corretamente provisionado.

Manual residual após um recreate:
1. `terraform apply` sobe a instância nova com o `user_data` (bootstrap executa automaticamente).
2. Pipeline de deploy (`deploy.yml`) coloca o JAR em `/opt/finbot/app.jar` e reinicia `finbot`.
3. Re-registrar webhook do Telegram com o novo cert (ver Pendência 2 acima).
4. Secret `keystore_password=finbot123` já existe em `finbot-prod-secrets` (confirmado).

---

## Próximos passos / observações pro próximo

- **Sincronia futura:** todo novo passo manual executado no servidor deve ser refletido no `bootstrap.sh` (convenção a ecoar no CLAUDE.md quando assentar — ADR 0009).
- **Médio prazo:** migrar webhook para Caddy+LE (`bot.satyansaita.com`) elimina o keystore self-signed e torna o recreate trivial (sem re-registrar). Registrado em `docs/PENDENCIAS-TECNICAS.md`.
- **shellcheck:** instalar na máquina de desenvolvimento ou usar GitHub Actions para rodar no CI. O script usa padrões conservadores (sem expansões exóticas) mas uma passagem formal pelo shellcheck é recomendada antes de um recreate real.

---

## Arquivos criados/modificados

- `financas_bot_telegram/infra/provision/bootstrap.sh` (novo: script principal idempotente)
- `financas_bot_telegram/infra/provision/Caddyfile.tftpl` (novo: template do Caddyfile para referência)
- `financas_bot_telegram/infra/provision/finbot.service` (novo: unit systemd do app para referência)
- `financas_bot_telegram/infra/provision/journald.conf.d/finbot.conf` (novo: drop-in do journald para referência)
- `financas_bot_telegram/infra/ec2.tf` (modificado: user_data via templatefile)
