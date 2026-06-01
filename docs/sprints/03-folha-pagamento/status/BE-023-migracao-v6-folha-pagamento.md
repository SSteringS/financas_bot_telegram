---
task: BE-023
titulo: "Migração V6 — DDL folha de pagamento"
data: 2026-06-01
branch: feature/be-023-migracao-v6-folha-pagamento
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 288
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - d01cf2f
pr: null
desvios: 1
pendencias_humano: 0
---

# BE-023 — Migração V6 — DDL folha de pagamento

## O que foi feito

Criado `V6__folha_pagamento.sql` com o SQL exato da spec §2 revisado pelo DBA (2026-05-31). Três blocos em sequência:

1. **`CREATE TABLE funcionario`** — 15 colunas (id, nome, salario_base, forma_pagamento, chave_pix, banco, agencia, conta, tipo_conta, conta_propria, obs_pagamento, dia_pagamento_referencia, ativo, criado_em, atualizado_em) + 3 CHECK constraints (chk_func_pix, chk_func_ted, chk_func_dia).
2. **`CREATE TABLE adiantamento`** — 10 colunas + 3 CHECK constraints (chk_adiant_consistencia, chk_adiant_parcelas, chk_adiant_valores) + 1 INDEX (idx_adiant_func_ativo).
3. **`ALTER TABLE pedidos_pagamento`** — 5 ADD COLUMN (categoria, funcionario_id, fechado, observacao, mes_referencia) + 1 FK (fk_pedido_funcionario) + 2 CHECK constraints (chk_pedido_folha, chk_pedido_folha_mes) + UNIQUE INDEX (uq_folha_por_mes) + INDEX (idx_pedido_folha).

**Schema antes da migração:** confirmado via leitura das migrations V1–V5 que `pedidos_pagamento` **não tem** coluna `categoria` — zero conflito. Nenhum ajuste ao SQL da spec foi necessário.

**Build:** `mvn -DskipTests package` → sucesso (código 0).

**Testes:** 288/288 verdes após `mvn clean test` (com Docker disponível). 262 unitários + 26 integração (Testcontainers/MySQL). O `mvn test` sem clean falhava com `Found more than one migration with version 4` — artifact stale no `target/` de antes do V5 ser renomeado. `mvn clean` resolveu. Flyway aplicou V1→V6 com sucesso no MySQL via Testcontainers.

---

## Desvios do plano

**1 desvio:** O plano (critério de aceitação) menciona "SHOW CREATE TABLE funcionario confirma: **13 colunas** + 3 CHECK constraints". A spec §2 revisada pelo DBA (2026-05-31) tem **15 colunas** — o DBA adicionou `criado_em` e `atualizado_em` na revisão pós-primeira-escrita. O SQL implementado segue a spec §2 (fonte canônica), não o critério literal do plano. O plano estava desatualizado em relação à revisão DBA.

---

## Decisões tomadas durante a execução

- **SQL copiado fiel da spec §2** — nenhuma variação criativa.
- **`categoria` não existia em `pedidos_pagamento`** — confirmado via leitura de V1–V5. O `AFTER status` funciona direto.
- **Tolerância 0.01 em `chk_adiant_consistencia`** — mantida exatamente como na spec: `ABS(valor_total - (valor_parcela * num_parcelas)) <= 0.01`. Essa tolerância cobre arredondamento de centavos quando `num_parcelas > 1`.
- **`data_criacao DESC` no INDEX composto** — copiado da spec; permite `ORDER BY data_criacao DESC` aproveitando o índice quando filtrado por `funcionario_id, categoria, fechado`.
- **`idx_adiant_func_ativo` inclui `data_inicio`** — permite range queries "adiantamentos ativos com início anterior ao mês de referência" de forma eficiente.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada. Flyway V6 aplicado com sucesso no MySQL via Testcontainers; 288/288 testes verdes.

---

## Próximos passos / observações pro próximo

- **Após merge em `develop`:** despachar **BE-024** imediatamente (entidades JPA + repositórios). É a única task desbloqueada diretamente.
- **BE-025, BE-026, BE-027** dependem de BE-024 e podem rodar em paralelo após BE-024 mergear.
- **Nota para BE-024 (detalhe importante):** a coluna `requisitante_id` em `pedidos_pagamento` é `NOT NULL DEFAULT 1` (V2). Pedidos FOLHA não têm requisitante natural — o `FecharMesUseCase` vai precisar de um valor para `requisitante_id`. Verificar antes de BE-028: usar o `requisitante_id` do operador que fechou, ou tornar a coluna nullable via ALTER. Registrado em ADR 0016 §riscos como item a verificar.
- **Testcontainers no CI:** os 26 testes de integração falham localmente por falta de Docker. O CI (GitHub Actions) tem Docker — os testes de integração devem passar lá. Confirmar.

---

## Padrões e decisões técnicas

**Esta task é DDL puro** — não há código Java. As decisões de padrão são arquiteturais de schema.

### Single Table Inheritance (STI) com CHECK constraints

**Problema:** o modelo precisa representar dois tipos de pedido no domínio de folha (VALE e FOLHA) sem criar tabelas separadas que duplicariam os campos `valor`, `status`, `data_criacao`, etc.

**Solução adotada:** STI em `pedidos_pagamento` com discriminador `categoria ENUM('VALE','FOLHA') NULL`. NULL semântico = pedido existente sem categoria (zero backfill). Referência: Fowler, PEAA, "Single Table Inheritance".

**Por que não tabela separada (`holerite`):** duplicaria campos de ciclo de vida do pedido; o fluxo de aprovação/comprovante é compartilhado; sem ganho de modelo justificando a duplicação.

**Por que não `pedido_folha_meta` (extensão 1:0..1):** a invariante "pedido VALE/FOLHA tem linha em meta" não é enforçável no banco sem triggers. STI com CHECK resolve o mesmo isolamento com menor complexidade (ADR 0016 §decisão 1).

### CHECK constraints como contrato de integridade

**Problema:** garantir que PIX sem `chave_pix`, TED sem dados bancários, adiantamentos matematicamente inconsistentes nunca entrem no banco — sem depender exclusivamente da validação da application layer.

**Solução:** CHECK constraints em todas as tabelas novas:
- `chk_func_pix` / `chk_func_ted`: integridade de dados bancários por forma de pagamento (DIP implícito — banco enforça a regra de domínio).
- `chk_adiant_consistencia`: `ABS(valor_total - valor_parcela * num_parcelas) <= 0.01` — tolerância para arredondamento de centavos.
- `chk_adiant_parcelas`: `parcelas_pagas BETWEEN 0 AND num_parcelas` — evita estado impossível.
- `chk_pedido_folha` / `chk_pedido_folha_mes`: invariantes STI (VALE/FOLHA exigem `funcionario_id`; FOLHA exige `mes_referencia`).

Referência: Winand, "Use The Index Luke" — CHECK constraints como parte do domínio, não só da aplicação.

### UNIQUE INDEX como mecanismo de idempotência

**Problema:** o `FecharMesUseCase` precisa garantir que não existam dois fechamentos para o mesmo funcionário/mês, mesmo em caso de duplo-clique ou race condition.

**Solução:** `UNIQUE INDEX uq_folha_por_mes (funcionario_id, mes_referencia)`. NULLs são distintos no MySQL — VALEs (mes_referencia=NULL) coexistem sem problema; apenas FELHAs (mes_referencia=YYYY-MM-01) são únicos por funcionário/mês.

**Por que não só verificação via query:** sujeita a race condition entre `SELECT` e `INSERT`. O UNIQUE INDEX é a guarda no banco; o check no service (existsFolhaNomes) é a guarda principal para mensagem de erro legível (ADR 0016 §decisão 3).

### Arquitetura hexagonal

Esta task é DDL puro — não há camadas de aplicação. A relevância hexagonal é indireta:
- O schema respeita o modelo de domínio da spec §3 (Funcionario como entidade raiz, Adiantamento como agregado dependente).
- `adiantamento` referencia `funcionario` por FK — reflete a relação de propriedade do domínio.
- As colunas STI em `pedidos_pagamento` são intencionalmente agnósticas à lógica de aplicação — são dados; o comportamento fica no `FecharMesUseCase` (BE-028).

---

## Arquivos criados/modificados

- `financas_bot_telegram/src/main/resources/db/migration/V6__folha_pagamento.sql` (novo — DDL completo da folha de pagamento)
