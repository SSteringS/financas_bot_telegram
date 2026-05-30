---
task: BE-17b
titulo: "Renomear rota Telegram /webhook → /webhook/telegram"
data: 2026-05-30
branch: feature/be-17b-renomear-rota-telegram
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 222
  testes_novos: 0
  branch_convencao: ok
  territorio: ok
commits:
  - ac9ced6
pr: null
desvios: 0
pendencias_humano: 0
---

# BE-17b — Renomear rota Telegram `/webhook` → `/webhook/telegram`

## O que foi feito

Uma linha mudada no controller + um teste atualizado:

- **`TelegramWebhookController`**: `@PostMapping("/webhook")` → `@PostMapping("/webhook/telegram")`.
- **`JwtAuthenticationFilterTest`**: método `naoDeveAplicarFiltroEmWebhook` renomeado pra `naoDeveAplicarFiltroEmWebhookTelegram` e URI atualizada pra `/webhook/telegram`.

Nenhum outro arquivo tocado — `grep -rn '"/webhook"' src/test` confirmou que só o teste do filtro referenciava o path antigo.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

- O `JwtAuthenticationFilter.shouldNotFilter` usa lógica genérica (`!path.startsWith("/api/v1/")`), não lista `/webhook` explicitamente. O teste apenas usava `/webhook` como exemplo representativo — atualizar o URI mantém o teste semanticamente correto.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## ⚠️ Passo operacional obrigatório pós-deploy

**Esta mudança exige `setWebhook` imediatamente após o deploy.** Entre o deploy do código e a chamada abaixo, o Telegram entrega na rota antiga e recebe 404.

### Janela (~5 min)

```bash
# 1. Confirmar que a rota nova está montada (espera 400/415 — existe mas sem payload)
curl -X POST https://satyansaita.com/webhook/telegram -d 'x' -i

# 2. Atualizar o webhook no Telegram
curl -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
  -d "url=https://satyansaita.com/webhook/telegram"

# 3. Confirmar
curl "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getWebhookInfo"
```

Resultado esperado em `getWebhookInfo`:
- `"url": "https://satyansaita.com/webhook/telegram"`
- `last_error_message` vazio ou ausente
- `pending_update_count` zerado (pode demorar alguns segundos)

```bash
# 4. Smoke: mandar mensagem de teste pro bot via app do Telegram e confirmar resposta.
```

### Rollback (se algo der errado)

```bash
# Reverter o setWebhook pra URL antiga:
curl -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
  -d "url=https://satyansaita.com/webhook"
# + revert do commit + redeploy restaura /webhook na app.
```

---

## Próximos passos / observações pro próximo

- Após confirmar `setWebhook` ok, atualizar `docs/STATE.md` anotando "rota Telegram agora `/webhook/telegram`".
- Próxima task lógica: **BE-20** (deploy WhatsApp em prod + Meta config).

---

## Arquivos criados/modificados

- `adapters/in/telegram/controller/TelegramWebhookController.java` (modificado: path `/webhook/telegram`)
- `test/.../infra/security/JwtAuthenticationFilterTest.java` (modificado: URI do teste atualizada)
