# BE-03 — Extração de `tipo` (BOLETO/PIX/TED/AGENDAMENTO) da legenda do Telegram

> Tarefa pequena e independente. Hoje todo pedido novo entra com `tipo=null`. Esta tarefa enriquece o parser pra detectar a palavra-chave na legenda e setar o enum correspondente.

---

## Pré-requisitos

- `TipoPagamento` enum existe (BE-00B)
- `PaymentRequestStrategy.parsePedido()` existe

---

## Decisões

- Detectar **uma única** palavra-chave por legenda (a primeira que encontrar). Se duas aparecem (`"100 pix boleto"`), prevalece a primeira na string.
- Match **case-insensitive**.
- Sem palavra-chave detectada → `tipo = TipoPagamento.OUTRO` (não `null`). Decisão consciente: melhor o front receber um valor explícito que `null`. Migration pré-existente preserva pedidos antigos como `null`, novos vêm com OUTRO no mínimo.
- Acentuação ignorada (não temos palavras com acento nos tipos, mas o parser deveria tratar — `agendamento` é a única que poderia variar).

---

## Arquivos esperados

**Novos:**
- `domain/service/LegendaParser.java` — classe pura sem dependências, com método estático `parseTipo(String legenda)` retornando `TipoPagamento`
- `LegendaParserTest.java`

**Modificados:**
- `PaymentRequestStrategy.parsePedido()` — chamar `LegendaParser.parseTipo(text)` e setar no pedido

---

## Código-chave

### `LegendaParser`

```java
package br.com.satyan.stering.saita.financasbottelegram.domain.service;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LegendaParser {

    // LinkedHashMap pra preservar ordem de iteração — não relevante aqui, mas defensivo
    private static final Map<String, TipoPagamento> PALAVRAS_CHAVE = new LinkedHashMap<>();
    static {
        PALAVRAS_CHAVE.put("boleto", TipoPagamento.BOLETO);
        PALAVRAS_CHAVE.put("pix", TipoPagamento.PIX);
        PALAVRAS_CHAVE.put("ted", TipoPagamento.TED);
        PALAVRAS_CHAVE.put("agendamento", TipoPagamento.AGENDAMENTO);
    }

    private LegendaParser() {}

    /**
     * Detecta o tipo de pagamento na legenda. Procura a PRIMEIRA palavra-chave
     * que aparece (do início da string), case-insensitive. Sem match → OUTRO.
     */
    public static TipoPagamento parseTipo(String legenda) {
        if (legenda == null || legenda.isBlank()) return TipoPagamento.OUTRO;
        String alvo = legenda.toLowerCase();

        // Encontra a primeira ocorrência de qualquer palavra-chave
        int posicaoMaisCedo = Integer.MAX_VALUE;
        TipoPagamento tipoEncontrado = TipoPagamento.OUTRO;

        for (Map.Entry<String, TipoPagamento> entry : PALAVRAS_CHAVE.entrySet()) {
            int pos = alvo.indexOf(entry.getKey());
            if (pos >= 0 && pos < posicaoMaisCedo) {
                posicaoMaisCedo = pos;
                tipoEncontrado = entry.getValue();
            }
        }

        return tipoEncontrado;
    }
}
```

### Atualização em `PaymentRequestStrategy.parsePedido()`

```java
import br.com.satyan.stering.saita.financasbottelegram.domain.service.LegendaParser;
// ...

private PedidoPagamento parsePedido(Message message) {
    String text = message.getCaption().trim();
    Matcher matcher = PEDIDO_PATTERN.matcher(text);

    if (!matcher.matches()) {
        throw new IllegalArgumentException("Formato inválido. Use: pedido <valor> <descrição>");
    }

    String valorStr = matcher.group(1).replace(',', '.');
    BigDecimal valor = new BigDecimal(valorStr);
    String descricao = matcher.group(3);

    return PedidoPagamento.builder()
        .valor(valor)
        .descricao(descricao)
        .telegramUserId(message.getFrom().getId().toString())
        .telegramMessageId(message.getMessageId().toString())
        .status(StatusPedido.PENDENTE)
        .requisitanteId(1L)
        .dataPedido(LocalDate.now())
        .tipo(LegendaParser.parseTipo(text))   // <-- mudança aqui
        .build();
}
```

---

## Critério de aceitação

- [ ] `LegendaParser.parseTipo("150.00 Almoço boleto")` → `BOLETO`
- [ ] `LegendaParser.parseTipo("200 pix maria")` → `PIX`
- [ ] `LegendaParser.parseTipo("1500 TED construtora silva")` → `TED` (case insensitive)
- [ ] `LegendaParser.parseTipo("300 agendamento luz")` → `AGENDAMENTO`
- [ ] `LegendaParser.parseTipo("100 Almoço")` → `OUTRO` (sem palavra-chave)
- [ ] `LegendaParser.parseTipo("100 BOLETO pix")` → `BOLETO` (primeiro a aparecer, mesmo com caixa alta)
- [ ] `LegendaParser.parseTipo("")` → `OUTRO`
- [ ] `LegendaParser.parseTipo(null)` → `OUTRO`
- [ ] Bot integrado: enviar mensagem `100 pix Maria` pelo Telegram em dev, verificar via SQL que `tipo='PIX'` no registro novo
- [ ] Bot integrado: enviar mensagem `100 Compras supermercado` (sem palavra-chave), verificar `tipo='OUTRO'`
- [ ] `PaymentRequestStrategyTest` atualizado pra cobrir os novos casos
- [ ] `./mvnw test` passa

---

## Fora de escopo

- Sinônimos (transferência, qrcode, etc) — pode vir em iteração futura
- Detecção de tipo no comprovante (`PaymentProofStrategy` já parseia tipo da legenda — verificar se faz sentido reusar `LegendaParser` lá; manter consistente)

---

## Status report

`docs/status/BE-03-parsing-tipo-legenda.md`. Tabela com 8+ cenários de teste, output do smoke test no banco mostrando registros novos com tipo correto. Próximo: BE-15.
