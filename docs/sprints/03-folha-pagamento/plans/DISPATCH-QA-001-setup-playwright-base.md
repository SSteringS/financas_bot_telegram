# DISPATCH — QA-001-setup-playwright-base (single-task)

> **Lote A — despachar imediatamente.** Não depende de nenhuma task de produto.
> ⚠️ **Branch de integration, não develop:**
> Feature branches QA-NNN saem de `integration/03-folha-pagamento` (ver CLAUDE.md §Fluxo de branches).
> A branch integration deve ser criada pelo planner antes de despachar. Confirmar existência:
> `git branch -a | grep integration/03-folha-pagamento`

---

## Pré-condições (git)

- Branch `integration/03-folha-pagamento` existe em `origin`. Confirmar com comando acima.
- Nenhuma task QA em voo que mexa em `package.json` ou `playwright.config.ts` ao mesmo tempo.

---

## O prompt (cole tudo numa sessão `--agent frontend`)

```
Task: QA-001 — Setup Playwright + config base.

Localize e leia o plano completo:
  Glob("docs/sprints/03-folha-pagamento/plans/QA-001-setup-playwright-base.md")

Leia também:
- docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md §4 (QA-001) — detalhe de cada arquivo a criar
- docs/architecture/desenho-testes-automatizados.md §7.1 — campos do .env.e2e.example
- docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md

## ATENÇÃO — BRANCH

⚠️ Branch a partir de `integration/03-folha-pagamento`, NÃO de `origin/develop`.
O initialPrompt deste agente diz `origin/develop` — IGNORAR para tasks QA.
Usar:
  git fetch
  git checkout -b feature/qa-001-setup-playwright-base origin/integration/03-folha-pagamento

## A TASK

Criar a infraestrutura base do Playwright. ZERO specs, ZERO fixtures, ZERO mudanças em src/.

Arquivos a criar:
1. frontend/playwright.config.ts (baseURL via E2E_FRONTEND_URL, globalSetup path, use: trace+screenshot+video, projects: Chromium only)
2. frontend/e2e/tsconfig.json (estende ../tsconfig.json)
3. frontend/.env.e2e.example (template versionado — mínimo 8 variáveis: E2E_FRONTEND_URL, E2E_BACKEND_URL, E2E_DB_HOST, E2E_DB_PORT, E2E_DB_NAME, E2E_DB_USER, E2E_DB_PASSWORD, E2E_ADMIN_SECRET)
4. frontend/e2e/specs/.gitkeep
5. frontend/e2e/fixtures/.gitkeep

Modificações em frontend/package.json:
- devDependencies: @playwright/test, @axe-core/playwright, tsx, mysql2, dotenv
- scripts: "e2e", "e2e:full", "e2e:ui", "e2e:report"

.gitignore: adicionar frontend/.env.e2e e frontend/playwright-report/

## REGRAS DURAS

1. Branch: `feature/qa-001-setup-playwright-base` saindo de `origin/integration/03-folha-pagamento`.
2. Território: SÓ `frontend/`. Zero `financas_bot_telegram/`.
3. ZERO código em frontend/src/ — só config + e2e/ + package.json.
4. 1 commit: `feat(QA-001): setup Playwright + config base`.
5. PR: `feature/qa-001-setup-playwright-base → integration/03-folha-pagamento` (NÃO para develop).
   PR pode ser aceito pelo próprio implementador (regra do CLAUDE.md §Fluxo de PR em dois níveis).
6. NÃO fazer PR pra develop — só para integration.

## VERIFICAÇÕES ANTES DE COMMITAR

1. `cd frontend && npx playwright test --list` executa sem erro (lista 0 specs — esperado).
2. `npm test` (Vitest) continua verde — package.json não quebrou os testes existentes.
3. `cat frontend/.env.e2e.example` mostra pelo menos 8 variáveis.
4. `frontend/.env.e2e` listado em .gitignore (grep para confirmar).
5. `frontend/playwright-report/` listado em .gitignore.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/QA-001-setup-playwright-base.md`

Frontmatter: testes_total (sem mudança), testes_novos: 0, exige_e2e_full: false.
Anotar se houve conflito com MSW ou Vitest ao adicionar as devDependencies.

## SE QUEBRAR

Cenário — `npm test` quebra após adicionar deps Playwright:
  Verificar se algum devDependency conflitou com versão de Vitest/RTL existente.
  Resolver conflito de versão antes de commitar. Anotar no status report.

Cenário — branch integration/03-folha-pagamento não existe:
  PARE. Contatar planner para criar a branch antes de continuar.

Pare ao final do status report. Abrir PR para integration (não para develop).
```

---

## Notas pro humano

- **Despachar imediatamente** — Lote A, sem bloqueio.
- **PR destino: `integration/03-folha-pagamento`**, não develop. O implementador pode aceitar o próprio PR (CLAUDE.md §Fluxo de PR em dois níveis).
- **Estimativa:** 30-60 min.
- **Após merge nesta integration:** despachar QA-002 (usa os scripts de package.json desta task).

## Referências

- `docs/sprints/03-folha-pagamento/plans/QA-001-setup-playwright-base.md`
- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §4
- `docs/architecture/desenho-testes-automatizados.md` §7.1
