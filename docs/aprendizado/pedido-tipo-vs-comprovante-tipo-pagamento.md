# `pedidos_pagamento.tipo` vs `comprovantes.tipo_pagamento` — duas colunas, dois conceitos

## Contexto da dúvida

Durante a camada 5 de testes (smoke E2E via Telegram), surgiu a observação de que "o pedido não tem tipo, só o comprovante tem". A confusão é entendível porque os dois conceitos têm nomes parecidos e moram em tabelas relacionadas.

## Resumo destilado

São **duas colunas diferentes**, em **duas tabelas diferentes**, capturando **dois conceitos diferentes**:

| Onde | Coluna | Tipo SQL | Quando é setado | Quem seta |
|---|---|---|---|---|
| `pedidos_pagamento` | `tipo` | `ENUM('BOLETO','PIX','TED','AGENDAMENTO','OUTRO')` | Quando o pedido é registrado (foto + legenda `100 boleto X`) | `LegendaParser.parseTipo()` extrai da legenda |
| `comprovantes` | `tipo_pagamento` | `VARCHAR(255)` | Quando o comprovante é registrado (foto + legenda `#123 PIX`) | Regex em `PaymentProofStrategy` extrai do padrão `#<id> <tipo>` |

### Conceitualmente

- **`pedido.tipo`** = "que tipo de pagamento o pai está PEDINDO". Captura a intenção.
- **`comprovante.tipo_pagamento`** = "como o pagamento foi efetivamente FEITO". Captura a execução.

Em teoria podem ser diferentes:
- Pai pede: `100 boleto Energia` → `pedido.tipo=BOLETO`
- Filho paga via PIX porque o boleto venceu: `#123 PIX` → `comprovante.tipo_pagamento='PIX'`

## Por que tipos diferentes de coluna

- `pedido.tipo` é **enum**, valores fechados. Vai mais cedo ou tarde virar filtro do front (variante C dos mockups previa "filtre por tipo: BOLETO, PIX..."). Enum garante consistência.
- `comprovante.tipo_pagamento` é **VARCHAR livre**. Reflete o texto que veio na legenda, mais permissivo (pode aparecer `"pix"`, `"transferência"`, `"qrcode"`, etc).

A normalização do comprovante pra enum poderia ser feita, mas é trabalho a mais sem ganho claro hoje. Quando o front quiser filtrar comprovante por tipo, aí pode entrar como evolução (ou pode usar o tipo do pedido associado).

## Pontos-chave

- **`pedido.tipo` SEMPRE existe** desde a V2 da BE-01 + hotfix de Java que completou os campos. É `NULL` em pedidos criados ANTES da BE-03 (parser não rodava); é populado por `LegendaParser` em pedidos criados DEPOIS.
- **`comprovante.tipo_pagamento` existe desde a versão original do projeto** (vem dos primórdios do bot). Sempre populado quando comprovante é criado.
- **Não confundir** ao escrever queries: filtrar pedido por tipo é `WHERE p.tipo = 'BOLETO'`; filtrar comprovante por tipo de pagamento é `WHERE c.tipo_pagamento = 'PIX'`.
- **O parser do pedido (`LegendaParser`)** procura a primeira palavra-chave da legenda (`boleto`, `pix`, `ted`, `agendamento`) case-insensitive. Sem match → `OUTRO`. Acentos não são suportados.
- **O parser do comprovante** segue o padrão `#<id> <tipo>` na legenda. Captura tudo após `#<id> ` como tipo (upper-case via `.toUpperCase()`).

## Pra aprofundar

- Modelar `comprovante.tipo_pagamento` como enum também (consistência). Vale virar pendência técnica futura se aparecer dor.
- "Pedido intencional" vs "comprovante real" — pattern de dois estágios em sistemas de pagamento (ex: Stripe diferencia `PaymentIntent` de `Charge`)
- Quando vale duplicar info entre tabelas relacionadas? Aqui o `tipo` aparece em duas tabelas com nomes parecidos mas valores potencialmente diferentes. É proposital — não duplicação, dois campos com semântica distinta.
