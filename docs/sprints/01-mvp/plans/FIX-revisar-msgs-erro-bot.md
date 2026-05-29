# FIX — Revisar mensagens de erro/ajuda do bot

> Varrer todas as strings que o bot envia ao chat do usuário e atualizar pra refletir as features atuais (especialmente extração de tipo da legenda via BE-03, que não aparece na ajuda atual).

---

## Contexto

A `PaymentRequestStrategy.parsePedido()` lança `IllegalArgumentException("Formato inválido. Use: pedido <valor> <descrição>")` quando a legenda não bate com o regex. **A mensagem não menciona que dá pra incluir o tipo (boleto, pix, ted, agendamento) na descrição** — a feature da BE-03 fica invisível pro usuário.

Similar pra outras strings esparramadas pelo código que podem estar desatualizadas.

---

## Mudanças propostas

### 1. `PaymentRequestStrategy.parsePedido()`

Substituir:

```java
throw new IllegalArgumentException("Formato inválido. Use: pedido <valor> <descrição>");
```

Por:

```java
throw new InvalidMessageFormatException(
    "❌ Formato inválido.\n\n" +
    "Use: `<valor> <descrição>`\n\n" +
    "*Exemplos:*\n" +
    "• `100 boleto Energia`\n" +
    "• `200 pix Maria`\n" +
    "• `1500 ted construtora`\n" +
    "• `300 agendamento luz`\n" +
    "• `50 Almoço` (sem tipo vira OUTRO)\n\n" +
    "O tipo é detectado automaticamente pela palavra-chave (boleto/pix/ted/agendamento) na descrição.",
    update.getMessage().getChatId()  // chatId pro handler global mandar a mensagem
);
```

**Atenção:** mudar de `IllegalArgumentException` pra `InvalidMessageFormatException` (já existe em `adapters/in/telegram/exception/`). A `IllegalArgumentException` não é capturada com chatId pelo handler atual, então não chega resposta amigável pro usuário.

Validar via teste que a mensagem nova chega ao usuário.

### 2. `PaymentProofStrategy` — mensagem de formato inválido

Já tem mensagem decente em `InvalidCaptionException`:

```
"Formato da legenda inválido.\nUse: `#<id_pedido> <tipo_pagamento>`\n\n*Exemplo:* `#123 pix`"
```

Vale só conferir se faz sentido manter como está ou alinhar com a verbosidade da nova mensagem do pedido. **Sugestão:** atualizar pra ser igualmente didática:

```
"❌ Formato da legenda inválido.\n\n" +
"Use: `#<id_pedido> <tipo_pagamento>`\n\n" +
"*Exemplos:*\n" +
"• `#123 pix`\n" +
"• `#456 boleto`\n" +
"• `#789 ted`\n\n" +
"O `<id_pedido>` é o número que apareceu quando você registrou o pedido original."
```

### 3. Mensagem de erro genérico (BE-15 handler)

O fallback final do `GlobalTelegramExceptionHandler.handleAnyOther` provavelmente manda algo tipo "❌ Houve um problema processando sua mensagem". Verificar se a mensagem está adequada e atualizá-la pra incluir um "tente novamente" mais claro.

### 4. Mensagem de sucesso ao registrar pedido

Hoje em `PaymentRequestStrategy.process()` tem:

```java
String successMessage = String.format(
    "✅ Pedido de pagamento registrado com sucesso!\n\n*ID do Pedido:* `%d`\n*Valor:* R$ %.2f\n*Descrição:* %s",
    pedidoSalvo.getId(),
    pedidoSalvo.getValor(),
    pedidoSalvo.getDescricao()
);
```

Vale incluir o **tipo detectado**, pra o usuário ver que o parser funcionou:

```java
String successMessage = String.format(
    "✅ Pedido de pagamento registrado!\n\n" +
    "*ID:* `%d`\n*Valor:* R$ %.2f\n*Descrição:* %s\n*Tipo:* %s",
    pedidoSalvo.getId(), pedidoSalvo.getValor(), pedidoSalvo.getDescricao(), pedidoSalvo.getTipo()
);
```

Se `tipo` for `OUTRO`, considerar adicionar dica "(o tipo não foi detectado; inclua 'boleto', 'pix', 'ted' ou 'agendamento' na descrição da próxima vez)".

### 5. Mensagem de sucesso ao registrar comprovante

Já tem mensagem em `PaymentProofStrategy.process()`:

```java
"✅ Comprovante de pagamento para o *Pedido ID %d* foi registrado com sucesso!"
```

Tá adequada. Pode incluir o tipo do pagamento que ele detectou:

```java
"✅ Comprovante registrado!\n\n*Pedido:* #%d\n*Tipo:* %s"
```

---

## Verificações no código

Antes de mudar, fazer um grep amplo pra ver se há outras strings esparramadas:

```bash
cd financas_bot_telegram
grep -rn "sendMessage\|❌\|✅" src/main/java/ | grep -v ".java:[0-9]*: \*" | head -30
```

Lista todos os pontos que mandam mensagem pro usuário. Revisar caso a caso pra ver se faz sentido atualizar.

---

## Critério de aceitação

- [ ] `PaymentRequestStrategy.parsePedido()` lança `InvalidMessageFormatException` com mensagem nova, exemplificando os tipos
- [ ] `PaymentProofStrategy.process()` na legenda inválida também tem mensagem nova com exemplos
- [ ] Mensagem de sucesso ao registrar pedido inclui o tipo detectado
- [ ] (Opcional) Mensagem de sucesso ao registrar comprovante inclui o tipo
- [ ] Todos os testes unitários continuam passando (`./mvnw test`)
- [ ] Atualizar os testes que afirmavam mensagens antigas (provavelmente em `PaymentRequestStrategyTest`)
- [ ] Smoke test manual: mandar mensagem inválida pelo Telegram em dev, ver se a nova mensagem chega no chat

---

## Fora de escopo

- Criar um comando `/ajuda` no bot que envie o quick reference (vale como evolução futura mas exige mais infra — listar isso como nova pendência se sentir falta)
- Internacionalização (i18n) das mensagens
- Refatorar pra centralizar strings em arquivo `messages.properties`

---

## Status report

`docs/status/FIX-revisar-msgs-erro-bot.md`. Cobrir:
- Lista de strings encontradas e mudanças aplicadas em cada
- Output de `./mvnw test` (após atualizar testes antigos)
- Confirmação de smoke test manual no Telegram

Atualizar `docs/PENDENCIAS-TECNICAS.md` movendo este item pra "Itens resolvidos".
