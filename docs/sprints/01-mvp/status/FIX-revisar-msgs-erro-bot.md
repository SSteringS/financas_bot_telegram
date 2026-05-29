# FIX — Revisar mensagens de erro/ajuda do bot

**Data:** 2026-05-13
**Branch:** feature/backend-polish-evo07
**Commit/PR:** (ver commit deste arquivo)
**Responsável (instância):** Claude Code (CLI)

---

## O que foi feito

### `PaymentRequestStrategy`

- `parsePedido()`: trocado `IllegalArgumentException` por `InvalidMessageFormatException` (exception correta que o `GlobalTelegramExceptionHandler` sabe tratar com chatId). Mensagem nova inclui exemplos de todos os tipos (`boleto`, `pix`, `ted`, `agendamento`) e explica que sem tipo vira OUTRO.
- Mensagem de sucesso: inclui `*Tipo:* {tipo}` e, quando tipo for OUTRO, adiciona dica _"Tipo não detectado. Inclua palavra-chave na descrição para auto-categorizar."_

### `PaymentProofStrategy`

- `InvalidCaptionException` quando sem legenda: mensagem expandida com exemplos (`#123 pix`, `#456 boleto`, `#789 ted`) e explicação do `<id_pedido>`.
- `InvalidCaptionException` quando formato inválido: mesma mensagem expandida (consistente com o caso acima).
- Mensagem de sucesso: agora inclui `*Tipo:* {tipo_pagamento}` além do pedido ID.

### Testes

- Adicionados 2 novos testes em `PaymentRequestStrategyTest`:
  - `deveMostrarTipoNaMensagemDeSucesso`: verifica que mensagem de sucesso contém o tipo detectado (ex: "PIX")
  - `deveMostrarDicaQuandoTipoForOUTRO`: verifica que dica é exibida quando tipo é OUTRO
- Nenhum teste antigo precisou ser atualizado (nenhum assertava texto de mensagem específico).

---

## Desvios do plano

- Não incluí "❌ Formato inválido." no corpo da `InvalidMessageFormatException` — o handler já prefixa com "🚫 Formato de mensagem inválido." automaticamente, evitando duplicação.
- O comando `/ajuda` foi mantido fora de escopo conforme o plano.

---

## Decisões tomadas durante a execução

Mensagem da exception não deve duplicar o prefixo que o handler já adiciona. Exemplo do que o usuário vê no final: `"🚫 Formato de mensagem inválido. Use: '<valor> <descrição>'..."`.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada. Smoke test manual recomendado:
- Enviar foto com legenda inválida (ex: "apenas texto sem valor") pelo Telegram → deve chegar mensagem com exemplos
- Enviar foto com legenda válida (ex: "200 pix Maria") → deve chegar mensagem de sucesso com "Tipo: PIX"

---

## Próximos passos / observações pro próximo

Se quiser criar um comando `/ajuda` no futuro, ele pode reutilizar as mesmas strings de exemplos que agora estão nas exceptions.

---

## Arquivos criados/modificados

- `adapters/in/telegram/strategy/PaymentRequestStrategy.java` (modificado: exception type + mensagens)
- `adapters/in/telegram/strategy/PaymentProofStrategy.java` (modificado: mensagens)
- `adapters/in/telegram/strategy/PaymentRequestStrategyTest.java` (modificado: +2 testes)
- `docs/status/FIX-revisar-msgs-erro-bot.md` (novo)
