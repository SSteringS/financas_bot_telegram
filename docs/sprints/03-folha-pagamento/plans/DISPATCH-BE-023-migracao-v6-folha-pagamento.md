# DISPATCH — BE-023-migracao-v6-folha-pagamento (single-task)

> **Sobre este arquivo:** prompt pronto pra colar numa sessão nova do **Claude do back** (iniciada com `--agent backend`) pra executar a BE-023.
> O agente `backend` já tem `initialPrompt` configurado que carrega o plano e cria a branch — o prompt abaixo é o trigger mínimo + regras adicionais específicas desta task.
>
> **Quando usar:** assim que o humano quiser abrir a sprint 03. BE-023 não tem pré-condições de código — é o gate de entrada da sprint.

---

## Pré-condições

- `develop` atualizado com os commits da sprint 02b (WF-01..WF-08, ADR 0015). ✅ (já commitados)
- Spec `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` disponível. ✅
- ADR 0016 em `docs/decisions/0016-evo09-folha-pagamento.md` disponível. ✅
- **Não** requer nenhuma task de código anterior — é DDL puro.

---

## O prompt (cole tudo a partir daqui numa sessão iniciada com `--agent backend`)

```
Task: BE-023 — Migração V6 — DDL folha de pagamento.

Localize e leia o plano completo (o initialPrompt já orienta o Glob).
Antes de escrever o arquivo SQL, leia também:
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §2 (SQL exato, revisado DBA)
- docs/decisions/0016-evo09-folha-pagamento.md (contexto arquitetural)
- docs/architecture/estado-atual-dev.md (schema atual — confirmar se `categoria` já existe em `pedidos_pagamento` e com qual tipo)

## A TASK

Criar o arquivo `V6__folha_pagamento.sql` com o SQL completo da spec §2.
Três blocos em sequência:
1. `CREATE TABLE funcionario` (13 colunas + 3 CHECK constraints)
2. `CREATE TABLE adiantamento` (10 colunas + 3 CHECK constraints + 1 INDEX)
3. `ALTER TABLE pedidos_pagamento` (5 ADD COLUMN + 2 ADD CONSTRAINT CHECK + ADD UNIQUE INDEX + ADD INDEX)

Copiar o SQL da spec fiel — sem inventar variações. Se a spec e o `estado-atual-dev.md` divergirem na coluna `categoria` (tipo ou existência), registrar no status report e ajustar o ALTER para não conflitar.

## REGRAS DURAS

1. Branch: `feature/be-023-migracao-v6-folha-pagamento` (o initialPrompt já instrui a criação — confirmar que saiu de `develop`).
2. Território: SÓ `financas_bot_telegram/src/main/resources/db/migration/`. Nenhum arquivo Java. Nenhum arquivo de `frontend/`.
3. NÃO editar V1..V5 existentes.
4. 1 commit, padrão `feat(BE-023): migracao V6 DDL folha de pagamento (funcionario, adiantamento, ALTER pedidos_pagamento)`.
5. NÃO mergeie. PR pra `develop` após status report + Reviewer.

## VERIFICAÇÃO ANTES DE CRIAR O ARQUIVO

Rodar `SHOW CREATE TABLE pedidos_pagamento` em dev (via `mysql -u ... -p ...` ou pelo datasource do Spring em modo debug) para confirmar:
- Coluna `categoria` existe? Com qual tipo? Se já for `ENUM('VALE','FOLHA')`, o ALTER pode precisar de ajuste.
- Coluna `funcionario_id` já existe? Se sim, pular esse ADD COLUMN ou usar `ADD COLUMN IF NOT EXISTS` (MySQL 8 suporta).

Se não tiver acesso direto ao banco em dev agora, usar como proxy: rodar `mvn test` — se Flyway reclamar de coluna duplicada, ajustar antes de commitar.

## VALIDAÇÃO OBRIGATÓRIA

1. `mvn spring-boot:run` (ou `mvn test`) sobe sem erro de Flyway — a linha `Successfully applied 1 migration to schema (V6)` deve aparecer no log.
2. `SHOW CREATE TABLE funcionario` — confirmar 13 colunas + 3 CHECKs.
3. `SHOW CREATE TABLE adiantamento` — confirmar 10 colunas + 3 CHECKs + INDEX `idx_adiant_funcionario`.
4. `SHOW CREATE TABLE pedidos_pagamento` — confirmar +5 colunas, UNIQUE INDEX `uq_folha_por_mes`, INDEX `idx_pedido_folha`.
5. Teste manual de integridade: `INSERT INTO funcionario (nome, salario_base, forma_pagamento, conta_propria, ativo, criado_em, atualizado_em) VALUES ('Teste', 1000.00, 'PIX', TRUE, TRUE, NOW(), NOW())` → deve FALHAR com CHECK constraint (chave_pix NULL com PIX).
6. `mvn test` verde — testes existentes não quebram (não há código Java novo a testar).

## STATUS REPORT

Escrever em `docs/sprints/03-folha-pagamento/status/BE-023-migracao-v6-folha-pagamento.md` seguindo `docs/templates/_TEMPLATE-status.md`.

Frontmatter obrigatório: `testes_total`, `testes_novos` (ambos 0 — DDL puro), `build`, estado final.

Anotar no corpo:
- Resultado do `SHOW CREATE TABLE pedidos_pagamento` antes da migração (coluna `categoria` existia?).
- Se houve ajuste no ALTER (ex: `IF NOT EXISTS`) — qual e por quê.
- Resultado das validações 1-5 acima.
- Seção `## Padrões e decisões técnicas` (obrigatória pelo role backend) — para DDL, descrever as decisões de schema: por que STI em `pedidos_pagamento` (vs tabela separada), por que UNIQUE INDEX para idempotência (vs só check no código), por que CHECK constraints no banco.

## SE QUEBRAR

Cenário 1 — **Flyway rejeita V6 por conflito de coluna/tipo:**
- Identificar qual coluna conflita (ex: `categoria` já existe com tipo diferente).
- Ajustar o ALTER para usar `MODIFY COLUMN` se o tipo precisa mudar OU `ADD COLUMN IF NOT EXISTS` se a coluna já existe com o tipo correto.
- Anotar a divergência no status report.
- Se a mudança de tipo for destrutiva (dados existentes incompatíveis), PARE e abra discussão via AskUserQuestion — não destrua dados de prod.

Cenário 2 — **Testes existentes quebram após V6:**
- Verificar se algum teste usa `pedidos_pagamento` com schema hardcoded.
- Se sim, ajustar os testes pra aceitar as novas colunas NULL.
- Anotar no status report quais testes foram ajustados e por quê.

Cenário 3 — **`categoria` já existe com enum diferente (ex: `ENUM('URGENTE','NORMAL')`):**
- PARE imediatamente. Use AskUserQuestion para reportar o conflito.
- NÃO altere tipo de coluna com dados existentes sem confirmação explícita.

O agente reviewer (chamado pelo initialPrompt ao final) vai verificar: SQL fiel à spec, CHECKs corretos (especialmente `chk_adiant_consistencia` com tolerância 0.01), UNIQUE INDEX no lugar certo.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano (fora do prompt)

- **Estimativa de duração da sessão:** 30-60 min. É DDL puro — o trabalho real é copiar o SQL da spec §2 e rodar as validações. O tempo extra vai aparecer se houver conflito de schema (coluna `categoria`).
- **Como iniciar:** `claude --agent backend` no worktree do implementador (`C:\Users\satya\src\financas_bot_telegram`). Colar o prompt acima. O `initialPrompt` do agent faz o boot.
- **Próxima task após merge:** BE-024 (entidades JPA + repositórios). Pode ser despachada imediatamente após o PR da BE-023 mergear — nenhuma outra condição.
- **Decisão §7 (vales na lista do Pedro):** não afeta BE-023. O schema da BE-023 é agnóstico à decisão — `categoria=VALE` e `categoria=FOLHA` ficam registrados independentemente de como a query do Pedro vai filtrar.
- **ADR 0016:** ainda `Proposed`. Homologar antes de despachar tasks de implementação Java (BE-024+). A BE-023 é só DDL e pode ser executada antes, pois o schema já foi revisado pelo arquiteto.

---

## Referências

- Plano completo: `docs/sprints/03-folha-pagamento/plans/BE-023-migracao-v6-folha-pagamento.md`
- SQL canônico: `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §2
- ADR 0016: `docs/decisions/0016-evo09-folha-pagamento.md`
- CLAUDE.md §"Worktrees git" + §"Fluxo de branches"
- `docs/runbooks/PRE-MERGE-CHECKLIST.md`
- `docs/templates/_TEMPLATE-status.md`
- Agent: `.claude/agents/backend.md`
