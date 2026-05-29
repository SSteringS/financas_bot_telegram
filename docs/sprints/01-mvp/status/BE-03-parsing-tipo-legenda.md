# BE-03 — Extração de tipo (BOLETO/PIX/TED/AGENDAMENTO) da legenda do Telegram

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- `LegendaParser` criado em `domain/service/` — classe pura, sem dependências Spring:
  - `parseTipo(String legenda)` — busca primeira ocorrência de palavra-chave, case-insensitive
  - Sem match → `TipoPagamento.OUTRO` (nunca `null`)
  - Suporta: boleto, pix, ted, agendamento
- `PaymentRequestStrategy.parsePedido()` — adicionado `.tipo(LegendaParser.parseTipo(text))` no builder
- 9 testes de unidade em `LegendaParserTest` cobrindo todos os critérios do plano
- 171 testes totais, todos verdes

---

## mvn test — resultado

```
Tests run: 171, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Desvios do plano

Nenhum.

---

## Próximos passos

- BE-15: handler genérico de exceções

---

## Arquivos criados/modificados

**Novos (produção):**
- `domain/service/LegendaParser.java`

**Modificados:**
- `adapters/in/telegram/strategy/PaymentRequestStrategy.java` (+ parseTipo no builder)

**Novos (testes):**
- `LegendaParserTest` (9 cenários)
