# [TASK-ID] — Título curto da tarefa

> **Não edite este arquivo.** Copie pra `<TASK-ID>-titulo-curto.md` (ex: `BE-05-listar-pedidos.md`) e preencha lá.

---

**Data:** YYYY-MM-DD
**Branch:** feature/...
**Commit/PR:** abc1234 (ou link do PR)
**Responsável (instância):** Claude Code (IntelliJ) / Claude Code (CLI) / humano

---

## O que foi feito

Descreva em prosa curta o que efetivamente entrou no commit. Não é pra repetir o critério de aceitação — é pra contar a realidade.

Exemplo:
- Criei a migration V2 conforme planejado, com o ajuste citado abaixo.
- Validei `EXPLAIN` nos índices novos: ambos cobrindo.
- Apliquei em dev sem erro.

---

## Desvios do plano

Se você fez algo diferente do que estava em `plans/`, documente aqui. Cada desvio precisa de uma justificativa.

Exemplo:
- O plano pedia `data_pedido DATE NOT NULL DEFAULT (CURRENT_DATE)` mas o MySQL 8.0.13+ não aceita expressão como default em DATE — usei trigger ou backfill explícito.
- A coluna `tipo` foi nomeada `tipo_pagamento` em vez de `tipo` porque já existia uma coluna `tipo` no schema legado.

Se não houve desvio, escreva "Nenhum.".

---

## Decisões tomadas durante a execução

Decisões locais (que não mereceriam um ADR, mas que vale registrar). Geralmente envolvem nome de variável, escolha de helper, organização de arquivo.

Exemplo:
- Coloquei o mapper `PedidoEntity ↔ Pedido` na pasta `infra/persistence/mapper/` em vez de junto da entity, pra ficar mais isolado.
- Optei por `record` Java em vez de classe pra os DTOs — menos boilerplate.

---

## Decisões pendentes (esperando humano)

Se você bateu em algo que precisa de decisão de produto e não tem como inferir do plano + architecture, registre aqui e **não prossiga**.

Exemplo:
- O endpoint `/api/v1/resumo` deveria considerar pedidos com `data_pedido` no mês atual ou pedidos com `data_pagamento` no mês atual? O plano não especifica. Aguardando confirmação.

Se não há nada pendente, escreva "Nenhuma — tarefa fechada.".

---

## Próximos passos / observações pro próximo

Coisas que o próximo implementador (ou o planejador) precisa saber. Útil pra atalhos e gotchas que descobriu.

Exemplo:
- A tarefa BE-06 vai precisar do `requisitanteId` no contexto da request — já deixei o filter de auth preparado pra isso.
- Notei que o teste de integração demora 30s pra subir Testcontainers — talvez vale otimizar reuso entre testes em uma tarefa futura.

---

## Arquivos criados/modificados

Lista resumida (não precisa exaustiva — git diff já tem isso). Útil pra o planejador escanear rápido.

- `src/main/resources/db/migration/V2__add_requisitante_dates_categoria_auth.sql` (novo)
- `domain/Pedido.java` (modificado: campos novos)
- `infra/persistence/PedidoEntity.java` (modificado)
