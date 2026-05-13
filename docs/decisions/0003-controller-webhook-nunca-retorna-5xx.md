# ADR 0003 — Controller /webhook SEMPRE retorna 200

**Data:** 2026-05-12
**Status:** Aceito

---

## Contexto

Em maio/2026, uma mensagem com PDF (entregue como `document`, não `photo`) entrou na fila do Telegram e disparou `NullPointerException` em `extractHighestQualityImageFileId`. O Spring retornou HTTP 500; o Telegram interpretou como falha e ficou retentando indefinidamente. A fila ficou paralisada até intervenção manual com `setWebhook?drop_pending_updates=true`.

O Telegram retenta entregas que recebem resposta não-2xx, podendo travar a entrada de todas as mensagens por horas ou dias em caso de bug persistente.

---

## Decisão

O controller `/webhook` SEMPRE retorna HTTP 200, mesmo em caso de exceção não-mapeada.

Toda exceção não-mapeada é tratada internamente:
1. Log `ERROR` com stack trace completo
2. Tentativa de `sendMessage` com mensagem amigável pro usuário via chatId do Update
3. Se o `sendMessage` também falhar, loga o erro secundário mas não propaga
4. Retorna 200 OK

Implementado via `@ExceptionHandler(Exception.class)` em `GlobalTelegramExceptionHandler` como handler de último recurso.

O `TelegramWebhookController` seta o `Update` como atributo do request (`request.setAttribute("__update", update)`) antes de processar, permitindo que o handler genérico extraia o `chatId` para feedback ao usuário.

---

## Consequências

**Positivas:**
- Fila do Telegram nunca trava por bugs na aplicação
- Bot é resiliente a falhas inesperadas
- Usuário recebe feedback amigável em vez de silêncio

**Negativas:**
- Se banco ou S3 estão fora, a mensagem é perdida sem possibilidade de retry automático. Considerar dead-letter log (EVO-09) para retry manual.
- Bugs "silenciosos" — se o log não for monitorado, falhas podem passar despercebidas. Mitigação: alertas no CloudWatch/Grafana nos logs ERROR.

---

## Alternativas consideradas

- **Retornar 5xx e deixar o Telegram retentar:** descartado. Causa paralisação da fila.
- **Fila interna com retry:** mais robusto, mas complexidade desnecessária neste estágio.
