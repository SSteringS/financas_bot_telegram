# HOTFIX-pedido-data-pedido-PATCH — Completar BE-01 no Java após merge

**Data:** 2026-05-11
**Branch:** hotfix/BE-01-completar-entity
**Commit/PR:** c1315b4 — abrir PR para `develop`
**Responsável (instância):** Claude Code (CLI)

---

## O que foi feito

BE-01 tinha entregado a migration V2 sem atualizar o código Java, causando `Field 'data_pedido' doesn't have a default value` em todo INSERT de pedido. Este patch fecha essa lacuna.

- Criado `domain/enums/TipoPagamento.java` com valores `BOLETO, PIX, TED, AGENDAMENTO, OUTRO` — espelha o ENUM SQL da V2
- `domain/model/PedidoPagamento.java` recebeu 4 campos: `requisitanteId (Long)`, `tipo (TipoPagamento)`, `dataPedido (LocalDate)`, `dataPagamento (LocalDate)`
- `adapters/out/persistence/entity/PedidoPagamentoEntity.java` idem, com anotações JPA (`@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` em `dataPedido`, `@Column(nullable = false)` em `requisitanteId`)
- `adapters/out/persistence/mapper/PedidoPagamentoMapper.java` mapeia os 4 campos em ambas as direções
- `PaymentRequestStrategy.parsePedido()` popula `requisitanteId = 1L` e `dataPedido = LocalDate.now()` ao construir o pedido

Testes: `PedidoPagamentoMapperTest` e `PaymentRequestStrategyTest` atualizados para cobrir os campos novos.

---

## Resultado dos testes

```
Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 19.978 s
```

O contexto do Spring (`FinancasBotTelegramApplicationTests`) também subiu sem erro — Hibernate gerou o DDL correto para `pedidos_pagamento` incluindo `requisitante_id bigint not null`, `data_pedido date not null`, `data_pagamento date`, e `tipo enum(...)`.

---

## Desvios do plano

Nenhum. Código implementado exatamente como especificado em `HOTFIX-pedido-data-pedido-PATCH.md`.

---

## Decisões tomadas durante a execução

- O campo `tipo` fica `null` em pedidos novos por enquanto — nullable no banco, portanto aceito. Extração de tipo da legenda é trabalho da próxima feature conforme o plano.
- Testes de serviço (`SalvarPedidoPagamentoServiceImplTest`, `RegistrarComprovanteServiceImplTest`) não foram alterados — usam builder com campos opcionais e não exercem lógica dos campos novos.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- Após merge em `develop`, fazer smoke test via Telegram: mandar foto com legenda `100.00 teste hotfix` e verificar no banco que `data_pedido` e `requisitante_id` estão preenchidos.
- Próxima feature: `FASE-3-VISUALIZACAO.md` — endpoints REST de consulta de pedidos. O `requisitanteId` já está disponível no domínio para filtrar por requisitante.
- Futura melhoria: extração de `tipo` da legenda (parsing de "pix/boleto/ted/agendamento" no caption) — foi colocada fora do escopo deste hotfix intencionalmente.

---

## Arquivos criados/modificados

- `domain/enums/TipoPagamento.java` (novo)
- `domain/model/PedidoPagamento.java` (modificado: +4 campos)
- `adapters/out/persistence/entity/PedidoPagamentoEntity.java` (modificado: +4 campos com JPA)
- `adapters/out/persistence/mapper/PedidoPagamentoMapper.java` (modificado: mapeia 4 campos novos)
- `adapters/in/telegram/strategy/PaymentRequestStrategy.java` (modificado: parsePedido popula requisitanteId e dataPedido)
- `adapters/out/persistence/mapper/PedidoPagamentoMapperTest.java` (modificado: fixtures e asserts dos novos campos)
- `adapters/in/telegram/strategy/PaymentRequestStrategyTest.java` (modificado: +2 assertions)
