# BE-15 — Handler genérico de exceções pra não travar a fila do Telegram

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- `GlobalTelegramExceptionHandler` — adicionado `@ExceptionHandler(Exception.class)` como fallback final:
  - Loga `ERROR` com stack trace
  - Extrai `chatId` de `request.getAttribute("__update")` (Update injetado pelo controller)
  - `sendMessage` amigável ao usuário; se falhar, loga mas não propaga
  - Retorna 200 SEMPRE
  - `extrairChatIdSeguramente()`: helper que usa instanceof pattern matching (Java 21)
- `TelegramWebhookController.receberMensagem()` — adicionado `HttpServletRequest request` + `request.setAttribute("__update", update)` antes do processamento
- `GlobalTelegramExceptionHandlerTest` — 4 novos testes:
  - NPE com Update com chatId → retorna 200, chama sendMessage
  - RuntimeException + sendMessage falha → ainda retorna 200
  - Update sem message → retorna 200, NÃO chama sendMessage
  - Sem atributo no request → retorna 200, NÃO chama sendMessage
- `TelegramWebhookControllerTest` — atualizado para passar `MockHttpServletRequest` ao método
- ADR `docs/decisions/0003-controller-webhook-nunca-retorna-5xx.md` criado
- 175 testes totais, todos verdes

---

## mvn test — resultado

```
Tests run: 175, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Desvios do plano

Nenhum.

---

## Próximos passos

- BE-14: testes de integração

---

## Arquivos criados/modificados

**Modificados:**
- `adapters/in/telegram/exceptionhandler/GlobalTelegramExceptionHandler.java` (+ handleAnyOther + extrairChatIdSeguramente)
- `adapters/in/telegram/controller/TelegramWebhookController.java` (+ HttpServletRequest, + setAttribute)
- `test/.../GlobalTelegramExceptionHandlerTest.java` (+ 4 testes + imports)
- `test/.../TelegramWebhookControllerTest.java` (+ MockHttpServletRequest)

**Novos:**
- `docs/decisions/0003-controller-webhook-nunca-retorna-5xx.md` (ADR)
