---
task: BE-023
titulo: "Migração V6 — DDL folha de pagamento"
sprint: 03-folha-pagamento
data_planejamento: 2026-05-31
branch_alvo: feature/be-023-migracao-v6-folha-pagamento
prioridade: alta
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: [BE-024]
skills_dispatched: [arquitetura-hexagonal]
---

# BE-023 — Migração V6 — DDL folha de pagamento

## Intake

- **Origem:** spec EVO-09 §2 + ADR 0016 §1-4. Decisão arquitetural: STI em `pedidos_pagamento` com CHECK constraints + entidades novas `funcionario` e `adiantamento`.
- **Por quê agora:** habilita todas as tasks de BE seguintes (BE-024..BE-028). Gate de entrada da sprint 03.
- **Esforço:** baixo — SQL já está escrito na spec, Flyway aplica automaticamente no startup.
- **Riscos resumidos:** ADD COLUMN NULL é instant DDL no MySQL 8 (sem lock de tabela). UNIQUE INDEX `uq_folha_por_mes` não conflita com dados existentes (NULLs são distintos no MySQL).

---

## Contexto

Schema atual relevante (ver `docs/architecture/estado-atual-dev.md`):
- `pedidos_pagamento` — tabela central. Colunas existentes: id, valor, descricao, status, tipo, s3KeyPedido, s3KeyComprovante, data_criacao, data_pagamento, requisitante_id, categoria (se já existe), etc.
- Não existem tabelas `funcionario` nem `adiantamento`.

A migração é **aditiva e não-destrutiva** — nenhum dado existente é alterado.

---

## Decisão / abordagem

Criar o arquivo `V6__folha_pagamento.sql` com o SQL exato da spec §2 (já revisado pelo DBA). Não inventar variações — copiar fiel.

Três partes:
1. `CREATE TABLE funcionario` com CHECK constraints de integridade bancária.
2. `CREATE TABLE adiantamento` com CHECK constraints matemáticas.
3. `ALTER TABLE pedidos_pagamento` — 5 colunas novas (NULL) + 2 CHECKs + UNIQUE INDEX + INDEX.

**Verificar antes de rodar:** se `pedidos_pagamento` já tem coluna `categoria` com outro tipo/enum, ajustar o ALTER para não conflitar. Consultar `docs/architecture/estado-atual-dev.md` e rodar `SHOW CREATE TABLE pedidos_pagamento` em dev.

---

## Escopo / arquivos

### Criar
- `financas_bot_telegram/src/main/resources/db/migration/V6__folha_pagamento.sql` — SQL exato da spec §2.

### Não tocar
- `V1` a `V5` — migrations existentes. Flyway aplica em ordem; não editar histórico.
- Código Java — nenhuma classe nesta task.

---

## Testes

- **Manual:** subir a aplicação localmente, verificar no log do Flyway que V6 foi aplicada sem erro.
- **Verificação:** rodar `SHOW CREATE TABLE funcionario`, `SHOW CREATE TABLE adiantamento` e `SHOW CREATE TABLE pedidos_pagamento` em dev para confirmar DDL.
- **Teste de integridade (manual):** tentar INSERT em `funcionario` com `forma_pagamento='PIX'` e `chave_pix=NULL` — deve falhar com CHECK constraint.
- Não aplicável: testes unitários JUnit (não há código Java nesta task).

`testes_total` esperado: 0 (automáticos). `testes_novos`: 0.

---

## Critérios de aceitação

- [ ] `V6__folha_pagamento.sql` criado com o SQL da spec §2 (3 blocos: funcionario, adiantamento, ALTER pedidos_pagamento).
- [ ] Aplicação sobe sem erro de Flyway em dev (`mvn spring-boot:run` ou `mvn test`).
- [ ] `SHOW CREATE TABLE funcionario` confirma: 13 colunas + 3 CHECK constraints.
- [ ] `SHOW CREATE TABLE adiantamento` confirma: 10 colunas + 3 CHECK constraints + 1 INDEX.
- [ ] `SHOW CREATE TABLE pedidos_pagamento` confirma: +5 colunas, +2 CHECKs, +UNIQUE INDEX `uq_folha_por_mes`, +INDEX `idx_pedido_folha`.
- [ ] CHECK de integridade funciona: INSERT com PIX sem chave_pix → erro.
- [ ] `mvn test` verde (testes existentes não quebram).
- [ ] Branch saiu de `develop` no formato `feature/be-023-migracao-v6-folha-pagamento`.
- [ ] Território respeitado: apenas `financas_bot_telegram/src/main/resources/db/migration/`.
- [ ] Status report `docs/sprints/03-folha-pagamento/status/BE-023-migracao-v6-folha-pagamento.md` com frontmatter válido.

---

## Fora de escopo

- Entidades JPA, repositórios, use cases, controllers — BE-024 em diante.
- Aplicação em prod — após merge em develop e PR develop→main.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| `pedidos_pagamento` já tem coluna `categoria` com tipo diferente | Média | Médio | Verificar `SHOW CREATE TABLE` antes; ajustar ALTER se necessário |
| UNIQUE INDEX conflita com dados existentes | Baixa | Médio | NULLs são distintos no MySQL — sem conflito; confirmar com SELECT antes |
| Versão do Flyway não aceita CHECK constraints MySQL | Baixa | Baixo | Flyway 9+ suporta; verificar `pom.xml` |

---

## Coordenação

- **Pode rodar em paralelo com:** nada (é o gate de entrada).
- **Depende sequencialmente de:** nada.
- **Bloqueia:** BE-024 (e todos os subsequentes indiretamente).
- **Atenção pro Reviewer:** confirmar que o SQL da spec foi copiado fiel, sem variações. Verificar que CHECK constraints estão corretas (especialmente `chk_adiant_consistencia` com tolerância 0.01).
- **Após merge:** despachar BE-024 imediatamente.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, **revisão do Reviewer** (sessão separada — ADR 0005). PR pra `develop`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §2 (SQL completo)
- `docs/decisions/0016-evo09-folha-pagamento.md` (ADR)
- `docs/architecture/estado-atual-dev.md` (schema atual)
