---
task: QA-007
titulo: "Remover @axe-core/playwright — a11y fora do gate E2E"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-03
branch_alvo: feature/qa-007-remover-axe-core-a11y-do-gate
integration_branch: integration/03-folha-pagamento
prioridade: media
esforco: baixo
territorio: front
estado: concluido
depende_de: []
bloqueia: []
skills_dispatched: []
exige_e2e_full: false
fluxos_qa: []
---

# QA-007 — Remover `@axe-core/playwright` — a11y fora do gate E2E

## Intake

- **Origem:** decisão de sprint 2026-06-03 — acessibilidade retirada dos gates E2E da sprint 03. Tasks relacionadas a a11y (`a11y-home.spec.ts` e inline `checkA11y` no fluxo feliz) canceladas.
- **Por quê agora:** `@axe-core/playwright` foi instalado em QA-001 para suportar a spec de a11y. Com a spec cancelada, a dependência ficaria ociosa e poderia causar confusão futura. Remover agora deixa o `package.json` limpo.
- **Esforço:** mínimo — 1 linha em `package.json` + `npm install`.

---

## Contexto

QA-001 instalou `@axe-core/playwright@^4.10.1` em `frontend/package.json`. O plano original de QA-004 usaria essa lib em duas frentes:

1. `a11y-home.spec.ts` — spec standalone de acessibilidade.
2. `checkA11y` inline em `site-fluxo-feliz.spec.ts` ("bonus").

Ambas foram **canceladas** quando a decisão de retirar a11y do gate foi tomada (2026-06-03). QA-004 foi atualizado para entregar apenas `site-fluxo-feliz.spec.ts` (sem checkA11y) e `webhook-cenarios.spec.ts`. Como nenhuma spec usará `@axe-core/playwright`, a dependência deve ser removida.

---

## Decisão / abordagem

Remover `@axe-core/playwright` de `frontend/package.json` e atualizar `package-lock.json`.

```bash
cd frontend
npm uninstall @axe-core/playwright
```

---

## Escopo / arquivos

### Modificar
- `frontend/package.json` — remover `@axe-core/playwright` das `devDependencies`.
- `frontend/package-lock.json` — atualizado automaticamente pelo `npm uninstall`.

### Não tocar
- `frontend/src/` — zero código de produção.
- `frontend/e2e/` — nenhuma spec usa a lib (QA-004 já atualizado para não criar `a11y-home.spec.ts`).
- `financas_bot_telegram/` — zero mudanças no back.

---

## Testes

- `npm test` (Vitest) verde após remover a dependência.
- `npm run build` verde.

`testes_novos: 0`.

---

## Critérios de aceitação

- [ ] `@axe-core/playwright` ausente em `frontend/package.json`.
- [ ] `npm test` verde.
- [ ] `npm run build` verde.
- [ ] Branch: `feature/qa-007-remover-axe-core-a11y-do-gate` saindo de `integration/03-folha-pagamento`.
- [ ] 1 commit: `chore(QA-007): remove @axe-core/playwright — a11y fora do gate E2E`.
- [ ] Status report em `docs/sprints/03-folha-pagamento/status/QA-007-*.md`.

---

## Fora de escopo

- Remover outros pacotes de acessibilidade não instalados.
- Adicionar qualquer spec de a11y — decidido que fica fora dos gates desta sprint.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| `npm uninstall` quebra deps transitivas | Baixa | Baixo | `npm test` e `npm run build` validam integridade pós-remoção |

---

## Coordenação

- **Pode rodar em paralelo com:** QA-004 (territórios disjuntos dentro de `frontend/e2e/`; QA-004 não importa mais axe-core).
- **Depende sequencialmente de:** nada.
- **Bloqueia:** nada.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (`build: ok`, `lint: ok`, `testes: ok`, `branch_convencao: ok`, `territorio: ok`), status report válido, revisão do Reviewer. PR `feature → integration/03-folha-pagamento`.

---

## Referências

- `docs/sprints/03-folha-pagamento/plans/QA-004-specs-mvp-3-cenarios.md` — escopo atualizado (sem a11y)
- `docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md`
