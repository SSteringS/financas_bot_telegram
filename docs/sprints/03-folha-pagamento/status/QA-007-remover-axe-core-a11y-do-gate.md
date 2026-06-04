---
task: QA-007
titulo: "Remover @axe-core/playwright — a11y fora do gate E2E"
data: 2026-06-03
branch: feature/qa-007-remover-axe-core-a11y-do-gate
responsavel: claude-front
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 61
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - 9d4f111
pr: null
desvios: 1
pendencias_humano: 0
---

# QA-007 — Remover `@axe-core/playwright` — a11y fora do gate E2E

---

## O que foi feito

**`npm uninstall @axe-core/playwright`** — dependência removida de `frontend/package.json` e `frontend/package-lock.json`.

**`frontend/e2e/specs/a11y-home.spec.ts`** — deletado (spec cancelada junto com a decisão de retirar a11y dos gates).

**`frontend/e2e/specs/site-fluxo-feliz.spec.ts`** — removidos:
- `import AxeBuilder from '@axe-core/playwright'`
- Bloco de a11y check inline (steps 5 do fluxo original)
- Renomeado o teste de `'fluxo feliz: login → home → comprovante (com a11y)'` para `'fluxo feliz: login → home → comprovante'`

O spec mantém o fluxo principal intacto: login → home → verifica pedidos → verifica "1 pedido pendente" → abre modal de comprovante.

---

## Validações realizadas

| Validação | Resultado |
|---|---|
| `@axe-core/playwright` ausente em `package.json` | ✓ |
| `npm test` (Vitest) 61/61 | ✓ zero regressão |
| `npm run lint` | ✓ limpo |
| `npm run build` | ✓ exit 0 |
| `npx tsc -p e2e/tsconfig.json --noEmit` | ✓ sem erros |

---

## Desvios do plano

**Desvio 1 (escopo expandido — decisão do humano):** o plano original do QA-007 previa apenas remover `@axe-core/playwright` do `package.json`, assumindo que QA-004 já teria entregado uma versão sem specs de a11y. Como QA-004 foi implementado com `a11y-home.spec.ts` e `AxeBuilder` inline (seguindo o plano original de QA-004), o escopo de QA-007 foi expandido a pedido do humano para incluir: deletar `a11y-home.spec.ts` e remover o uso de `AxeBuilder` em `site-fluxo-feliz.spec.ts`.

---

## Decisões tomadas durante a execução

**Manter o test de fluxo feliz:** o teste principal (`site-fluxo-feliz.spec.ts`) foi preservado sem o bloco de a11y. O fluxo login → home → modal continua válido e sem dependência de `@axe-core/playwright`.

**Contagem de testes:** com a deleção de `a11y-home.spec.ts` (1 test) e remoção do check inline do fluxo feliz, a suíte E2E passa de 4 para 2 testes Playwright ativos (`site-fluxo-feliz` + 2 cenários de `webhook-cenarios`). Não há `testes_novos` nesta task (só remoções).

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações

- A suíte E2E após QA-007: 2 specs ativas, 3 testes Playwright (1 fluxo feliz + 2 webhook).
- Se a11y for reintroduzida em sprint futura: reinstalar `@axe-core/playwright` e criar nova spec. O padrão de uso (`AxeBuilder.analyze()` + filtro `serious+critical`) está documentado no `docs/architecture/desenho-testes-automatizados.md` §8.3.

---

## Arquivos criados/modificados

- `frontend/package.json` (modificado: remove `@axe-core/playwright` de devDependencies)
- `frontend/package-lock.json` (modificado: atualizado automaticamente pelo npm uninstall)
- `frontend/e2e/specs/a11y-home.spec.ts` (deletado)
- `frontend/e2e/specs/site-fluxo-feliz.spec.ts` (modificado: remove import AxeBuilder + bloco a11y inline)
