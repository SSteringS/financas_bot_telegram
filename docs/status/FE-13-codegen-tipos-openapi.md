---
task: FE-13
titulo: "Codegen de tipos via openapi-typescript"
data: 2026-05-26
branch: feature/fe-13-codegen-tipos-openapi
responsavel: claude-front
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 43
  testes_novos: 0
  branch_convencao: ok
  territorio: ok
commits:
  - 0340b83
pr: null
desvios: 1
pendencias_humano: 0
---

# FE-13 — Codegen de tipos via openapi-typescript

---

## O que foi feito

- Instalado `openapi-typescript@6.7.6` como devDependency (v7 exigiria Node ≥20.19; projeto usa 20.15.0).
- Capturado snapshot do contrato em `frontend/openapi.json` a partir do backend rodando em `http://localhost:8080/v3/api-docs`.
- Gerado `frontend/src/api/tipos-gerados.ts` via `npm run gen:types` — arquivo marcado como não-editável.
- Reescrito `frontend/src/api/tipos.ts` como fachada:
  - Campos obrigatórios fixados com `Required<Pick<S['...DTO'], ...>>`.
  - Enums TS (`StatusPedido`, `TipoPagamento`) mantidos para uso em runtime — não derivados do spec (que usa string literal union).
  - `Pagina<T>` preservado como genérico do front (o spec expõe `PaginaDTOPedidoResumoDTO` sem parâmetro de tipo).
- Adicionados scripts `gen:api` e `gen:types` ao `package.json`.
- Prova de não-drift executada: `npm run gen:types && git diff --exit-code src/api/tipos-gerados.ts` → exit 0.
- Build, lint e 43 testes passando sem alterações no resto do app (nenhum import quebrado).

---

## Desvios do plano

1. **`openapi-typescript` versão 6 em vez de 7** — `npm warn EBADENGINE`: v7 requer Node ≥20.19; ambiente usa 20.15.0. Instalado v6.7.6 (API idêntica para o uso desta tarefa). Anotado no cabeçalho de `tipos.ts` e no commit.

---

## Decisões tomadas durante a execução

- `Pagina<T>` não foi derivado do spec (`PaginaDTOPedidoResumoDTO`) pois o backend não expõe tipo genérico; mantê-lo genérico no front evita duplicar interface para cada paginação futura.
- `tipo` e `status` de `PedidoResumo` declarados diretamente como enums TS (não via `extends` do campo do DTO) para preservar compatibilidade com comparações em runtime no app.
- `_StatusItem` declarado com `_` prefixo (convenção de privado no módulo) pois é usado apenas para compor `ResumoMes`.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- Quando o Node for atualizado para ≥20.19, considerar upgrade para `openapi-typescript@7` (`npm install --save-dev openapi-typescript@latest`).
- Ciclo de atualização do contrato: backend muda spec → `npm run gen:api` (back rodando em :8080) → `npm run gen:types` → corrigir quebras de tipo em `tipos.ts` → commitar diffs.
- A prova de não-drift (`npm run gen:types && git diff --exit-code src/api/tipos-gerados.ts`) pode ser adicionada ao CI como gate pré-merge.

---

## Arquivos criados/modificados

- `frontend/openapi.json` (novo: snapshot do contrato OpenAPI v3)
- `frontend/src/api/tipos-gerados.ts` (novo: gerado por openapi-typescript, não editar)
- `frontend/src/api/tipos.ts` (modificado: reescrito como fachada derivada do spec)
- `frontend/package.json` (modificado: devDependency + scripts gen:api/gen:types)
- `frontend/package-lock.json` (modificado: lockfile atualizado pela instalação)
