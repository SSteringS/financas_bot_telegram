# BE-15 — Handler genérico de exceções pra não travar a fila do Telegram

> **Gate obrigatório antes do deploy da API (DEP-*).** Rede de segurança que garante que **o controller do webhook NUNCA retorna 5xx**. Sem isso, qualquer bug futuro pode travar a fila do bot por dias.

---

## Contexto

Histórico do projeto: em maio/2026 uma mensagem antiga com PDF (que o Telegram entrega como `document`, não `photo`) entrou na fila e disparou `NullPointerException` no `extractHighestQualityImageFileId`. O Spring retornou 500 pro Telegram, que considerou entrega falha e ficou retentando indefinidamente. Apenas intervenção manual com `setWebhook?drop_pending_updates=true` resolveu.

Esta tarefa estabelece o princípio arquitetural: **o webhook do Telegram nunca retorna 5xx**. Toda exceção não-mapeada vira 200 + mensagem amigável pro usuário no chat + log estruturado pra debugging.

---

## Pré-requisitos

- Nenhum. Pode rodar em paralelo com qualquer outra tarefa.

---

## Arquivos esperados

**Modificados:**
- `adapters/in/telegram/exceptionhandler/GlobalTelegramExceptionHandler.java` — adicionar `@ExceptionHandler(Exception.class)` como fallback final

**Novos:**
- `docs/decisions/0003-controller-webhook-nunca-retorna-5xx.md` — ADR registrando a decisão

**Tests:**
- Atualizar `GlobalTelegramExceptionHandlerTest` — adicionar testes pros novos cenários

---

## Código-chave

### Handler genérico em `GlobalTelegramExceptionHandler`

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<Void> handleAnyOther(Exception e, HttpServletRequest request) {
    logger.error("Exceção não mapeada no processamento de Update do Telegram", e);

    // Tentar extrair o chatId do request body pra mandar mensagem amigável.
    // Se não conseguir extrair, ainda retorna 200 (mas usuário não recebe feedback).
    Long chatId = extrairChatIdSeguramente(request);
    if (chatId != null) {
        try {
            telegramMessageSenderService.sendMessage(chatId,
                    "❌ Houve um problema processando sua mensagem. " +
                    "Tente novamente em alguns instantes ou avise o admin.");
        } catch (Exception sendErr) {
            // Falha no sendMessage NÃO deve propagar nem mudar o status code.
            logger.error("Falha ao enviar mensagem amigável após erro original", sendErr);
        }
    } else {
        logger.warn("Não consegui extrair chatId do request — usuário não receberá feedback");
    }

    // SEMPRE 200 — esse é o ponto principal desta tarefa.
    return ResponseEntity.ok().build();
}

private Long extrairChatIdSeguramente(HttpServletRequest request) {
    // O Update já foi parseado pelo Spring antes do exception handler rodar.
    // A forma mais robusta é o controller setar o Update num attribute do request
    // antes de delegar pra strategy, e o handler ler de lá.
    //
    // Alternativa: ler o body novamente. Mas o body já foi consumido.
    //
    // Solução: o TelegramWebhookController.receberMensagem antes de chamar
    // updateOrchestratorService.process, faz:
    //   request.setAttribute("__update", update);
    // E aqui a gente lê:
    Object attr = request.getAttribute("__update");
    if (attr instanceof Update update && update.getMessage() != null) {
        return update.getMessage().getChatId();
    }
    return null;
}
```

### Mudança em `TelegramWebhookController`

```java
@PostMapping("/webhook")
public ResponseEntity<Void> receberMensagem(@RequestBody Update update, HttpServletRequest request) {
    logger.info("Recebendo mensagem do Telegram: {}", update);
    request.setAttribute("__update", update);  // <-- NOVO

    validateRequest(update);
    authorizeUser(update);
    updateOrchestratorService.process(update);
    logger.info("Mensagem do usuário {} processada com sucesso.", update.getMessage().getFrom().getId());

    return ResponseEntity.ok().build();
}
```

(O parâmetro `HttpServletRequest request` é injetado pelo Spring automaticamente.)

---

## ADR `0003-controller-webhook-nunca-retorna-5xx.md`

Criar em `docs/decisions/` baseado no `_TEMPLATE.md`. Conteúdo essencial:

**Contexto:** incidente da fila travada por mensagem antiga com PDF (maio/2026). O Telegram retenta indefinidamente quando recebe não-2xx, transformando bugs no bot em paralisação total da entrada de mensagens.

**Decisão:** o controller `/webhook` SEMPRE retorna 200, mesmo em caso de exceção não-mapeada. Erros são tratados internamente: log + mensagem amigável pro usuário via `sendMessage`.

**Consequências positivas:** fila nunca trava por bugs. Bot é resiliente a falhas inesperadas.

**Consequências negativas:** se a aplicação está saudável mas o **banco** ou o **S3** estão fora, o usuário recebe "Houve um problema..." e a mensagem é perdida. Considerar evolução futura: dead-letter log (gravar Updates que falharam em arquivo/tabela pra retry manual).

---

## Critério de aceitação

- [ ] `@ExceptionHandler(Exception.class)` adicionado em `GlobalTelegramExceptionHandler` como fallback final (depois de todos os specific handlers existentes)
- [ ] Handler logga `ERROR` com stack trace completo
- [ ] Handler tenta `sendMessage` com mensagem amigável pro `chatId` do Update; se falhar, loga mas não propaga
- [ ] Handler retorna `200 OK` SEMPRE — verificável via teste
- [ ] `TelegramWebhookController.receberMensagem` seta o Update como attribute do request antes de delegar (permite o handler ler depois)
- [ ] Testes novos:
  - NPE jogada do meio do `process()` → handler captura, retorna 200, `sendMessage` é chamado com chatId
  - RuntimeException genérica jogada → idem
  - Exception jogada + `sendMessage` também falha → ainda retorna 200, loga 2 erros
  - Update sem `message` (não dá pra extrair chatId) → ainda retorna 200, NÃO chama `sendMessage`, loga warning
- [ ] `./mvnw test` passa
- [ ] ADR `0003-controller-webhook-nunca-retorna-5xx.md` criado em `docs/decisions/` seguindo o template

---

## Fora de escopo (mas considerar pra evolução futura)

- Dead-letter log: gravar em arquivo/tabela os Updates que dispararam exceção, com timestamp e stack. Permite retry manual e auditoria. Pode ser uma EVO-09 quando aparecer necessidade.
- Métricas (contador de exceções não-mapeadas) — útil pra alertas, mas overkill agora.

---

## Status report

`docs/status/BE-15-handler-generico-excecoes.md`. Output dos 4 testes novos + confirmação visual via logs locais (rodar app, simular erro, ver os logs em ERROR e o "200 OK" no response). Próximo: BE-14.
