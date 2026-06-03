---
task: QA-001
titulo: "Setup Playwright + config base"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-01
branch_alvo: feature/qa-001-setup-playwright-base
integration_branch: integration/03-folha-pagamento
prioridade: alta
esforco: baixo
territorio: front
estado: pronto-pra-execucao
depende_de: []
bloqueia: [QA-002, QA-003]
skills_dispatched: []
exige_e2e_full: false
lote: A
---

# QA-001 — Setup Playwright + config base

## Intake

- **Origem:** spec QA Fase 1 §4 (QA-001). Lote A — independente da sprint 03.
- **Por quê agora:** sem o setup do Playwright nada mais na Fase 1 pode ser entregue. É o gate de entrada da suíte E2E.
- **Esforço:** baixo — instalação de deps + criação de 4-5 arquivos de config. Zero specs, zero fixtures.
- **Riscos resumidos:** MSW pode interferir com Playwright se compartilharem config; precisa de tsconfig separado em `e2e/`.

---

## Contexto

O projeto não tem suíte E2E hoje. O teste manual equivalente é o `docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md` (45-60 min). Esta task cria a infraestrutura base que as tasks QA-002..004 vão usar.

Pasta alvo: `frontend/e2e/` (disjunta de `frontend/src/`).

---

## Decisão / abordagem

Instalar `@playwright/test`, `@axe-core/playwright`, `tsx`, `mysql2`, `dotenv` como devDependencies em `frontend/package.json`.

`playwright.config.ts` com:
- `baseURL` via `E2E_FRONTEND_URL` (env var do `.env.e2e`)
- `globalSetup: './e2e/fixtures/global-setup.ts'` (será criado em QA-003 — referência por path, não importação)
- `use: { trace: 'on-first-retry', screenshot: 'only-on-failure', video: 'retain-on-failure' }`
- `projects: [{ name: 'chromium' }]` (só Chromium no MVP)

`.env.e2e.example` versionado (template de credenciais — ver spec arquitetural §7.1 para campos exatos).

---

## Escopo / arquivos

### Criar
- `frontend/playwright.config.ts` — config conforme spec §4.QA-001.
- `frontend/e2e/tsconfig.json` — estende `../tsconfig.json` + ajustes pra `@playwright/test`.
- `frontend/.env.e2e.example` — template versionado com TODOS os campos necessários (E2E_FRONTEND_URL, E2E_BACKEND_URL, E2E_DB_HOST, E2E_DB_PORT, E2E_DB_NAME, E2E_DB_USER, E2E_DB_PASSWORD, E2E_ADMIN_KEY).
- `frontend/e2e/specs/.gitkeep` — pasta vazia com placeholder.
- `frontend/e2e/fixtures/.gitkeep` — pasta vazia com placeholder.

### Modificar
- `frontend/package.json` — adicionar devDependencies (`@playwright/test`, `@axe-core/playwright`, `tsx`, `mysql2`, `dotenv`) e scripts:
  - `"e2e": "playwright test"`
  - `"e2e:full": "tsx e2e/scripts/subir-stack.ts && playwright test; tsx e2e/scripts/derrubar-stack.ts"` (scripts criados em QA-002)
  - `"e2e:ui": "playwright test --ui"`
  - `"e2e:report": "playwright show-report"`
- `.gitignore` da raiz (ou `frontend/.gitignore`) — adicionar `frontend/.env.e2e` e `frontend/playwright-report/`.

### Não tocar
- `frontend/src/` — zero mudanças em código de produção.
- `frontend/vite.config.ts`, `frontend/tsconfig.json` raiz — não alterar configs existentes.

---

## Testes

Não aplicável: task de setup. Nenhum spec ou test file nesta task.

`testes_total` esperado: sem mudança (0 novos testes). `testes_novos: 0`.

---

## Critérios de aceitação

- [ ] `cd frontend && npx playwright test --list` executa sem erro de importação/configuração (lista 0 specs — não há specs ainda).
- [ ] `cat frontend/.env.e2e.example` mostra TODOS os campos do template (mínimo 8 variáveis).
- [ ] `frontend/.env.e2e` listado em `.gitignore` (não vai pra repo).
- [ ] `frontend/playwright-report/` listado em `.gitignore`.
- [ ] `npm run e2e -- --help` mostra help do Playwright sem erro.
- [ ] Confirmação que MSW não interfere: `playwright.config.ts` não inclui service worker (config isolada de `vite.config.ts`).
- [ ] `npm test` (Vitest) continua verde — nenhum teste existente quebrou.
- [ ] Branch: `feature/qa-001-setup-playwright-base` saindo de `integration/03-folha-pagamento`.
- [ ] Status report `docs/sprints/03-folha-pagamento/status/QA-001-setup-playwright-base.md` com frontmatter válido.

---

## Fora de escopo

- Specs (QA-004), fixtures (QA-003), scripts de orquestração (QA-002).
- `playwright-report/` — não commitado (`.gitignore`).
- Configuração do CI para rodar Playwright — Fase 3.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| MSW service worker carregado pelo Playwright inadvertidamente | Baixa | Alto | tsconfig separado em `e2e/`; playwright.config não referencia src/ |
| Versão do Playwright incompatível com Node atual | Baixa | Médio | Usar `@playwright/test@latest`; verificar Node ≥ 20 antes |
| `globalSetup` referencia arquivo que não existe ainda (QA-003) | Certa | Baixo | OK esperado — `playwright test --list` funciona sem o globalSetup existir fisicamente; spec apenas valida config syntax |

---

## Coordenação

- **Pode rodar em paralelo com:** qualquer task BE/FE/QA da sprint 03.
- **Depende sequencialmente de:** nada — Lote A.
- **Bloqueia:** QA-002 (usa os scripts de `package.json`), QA-003 (usa estrutura de `e2e/fixtures/`).
- **Atenção pro Reviewer:** confirmar que `frontend/.env.e2e` está no `.gitignore`; confirmar que `npm test` (Vitest) continua verde após as mudanças no `package.json`.
- **Após merge:** despachar QA-002 (depende dos scripts do package.json) e liberar QA-003 se BE-023 já estiver em develop.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR `feature → integration/03-folha-pagamento`.

`exige_e2e_full: false` — a suíte sendo construída não é gate dela mesma (regra transversal da Fase 1).

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §4 (QA-001)
- `docs/architecture/desenho-testes-automatizados.md` §7.1 (.env.e2e campos)
- `docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md`
