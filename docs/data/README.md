---
ultima_migracao: 'V6__folha_pagamento (planejada — Sprint 03)'
ambiente: develop
ultimo_review: 2026-05-31
---

# docs/data — Modelo de Dados

Documentação **durável** do schema do banco de dados. Atualizada pelo arquiteto sempre
que uma migração Flyway é mergeada em develop.

## Arquivos

| Arquivo | O que contém |
|---|---|
| README.md | Este índice e convenções de atualização |
| schema-er.md | Diagrama ER completo + documentação de cada tabela, índices e constraints |

## Convenção de atualização

Toda task que contenha uma migration Flyway deve, após merge em develop:

1. Arquiteto é acionado pelo planner (subagente).
2. Arquiteto lê o status report em docs/sprints/NN/status/.
3. Arquiteto atualiza schema-er.md (novas tabelas, colunas, constraints, índices).
4. Atualiza o campo ultima_migracao no frontmatter de schema-er.md.
5. Atualiza docs/architecture/estado-atual-dev.md secao 4 (Modelo de dados).

## O que NÃO fica aqui

- DDL de migração específico de uma sprint → docs/sprints/NN/specs/slug.md
- Decisões de por que o modelo foi assim → docs/decisions/
- Estado atual da aplicação (features implementadas) → docs/architecture/estado-atual-dev.md
