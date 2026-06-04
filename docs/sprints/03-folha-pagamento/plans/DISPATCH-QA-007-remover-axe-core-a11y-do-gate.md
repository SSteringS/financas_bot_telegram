# DISPATCH — QA-007 remover axe-core (single-task)

> Retira `@axe-core/playwright` do `package.json` — a11y foi removida dos gates E2E desta sprint.
> Task mínima: 1 comando + confirmar que build e testes continuam verdes.

---

## Pré-condições (git)

- Nenhuma. Pode iniciar imediatamente a partir de `integration/03-folha-pagamento`.

---

## O prompt (cole tudo numa sessão `--agent frontend`)

```
Task: QA-007 — Remover @axe-core/playwright (a11y fora do gate E2E).

Leia o plano:
  docs/sprints/03-folha-pagamento/plans/QA-007-remover-axe-core-a11y-do-gate.md

## BRANCH

  git fetch
  git checkout -b feature/qa-007-remover-axe-core-a11y-do-gate origin/integration/03-folha-pagamento

## A TASK

Uma única mudança: remover `@axe-core/playwright` de `frontend/package.json`.

```bash
cd frontend
npm uninstall @axe-core/playwright
```

Isso atualiza automaticamente `package.json` e `package-lock.json`.

## VALIDAÇÃO

Após o uninstall, confirmar:

1. `npm test` — Vitest verde (sem erros de import).
2. `npm run build` — TypeScript e Vite compilam sem erro.

Se qualquer um falhar, investigar e corrigir antes de commitar.

## REGRAS DURAS

1. Branch: `feature/qa-007-remover-axe-core-a11y-do-gate` saindo de `origin/integration/03-folha-pagamento`.
2. Território: SÓ `frontend/package.json` e `frontend/package-lock.json`.
3. Zero outros arquivos — não criar specs, não modificar playwright.config.ts.
4. 1 commit: `chore(QA-007): remove @axe-core/playwright — a11y fora do gate E2E`.
5. NÃO mergeie. PR para `integration/03-folha-pagamento` após status report + Reviewer.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/QA-007-remover-axe-core-a11y-do-gate.md`

testes_novos: 0
Anotar resultado de `npm test` e `npm run build`.

Pare ao final. Não mergeie.
```

---

## Notas pro humano

- **Estimativa:** 5 minutos.
- **Pode rodar em paralelo com QA-004** — territórios disjuntos (QA-007 toca só package.json; QA-004 cria specs em e2e/specs/).
- **Por que remover agora:** `a11y-home.spec.ts` e o `checkA11y` inline do fluxo feliz foram cancelados. Dependência ociosa gera confusão. Limpar antes de QA-004 ser mergeado.

## Referências

- `docs/sprints/03-folha-pagamento/plans/QA-007-remover-axe-core-a11y-do-gate.md`
- `frontend/package.json` (arquivo a modificar)
