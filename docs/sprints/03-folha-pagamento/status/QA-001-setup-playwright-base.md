---
task: QA-001
titulo: "Setup Playwright + config base"
data: 2026-06-01
branch: feature/qa-001-setup-playwright-base
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
  - 450729d
pr: null
desvios: 1
pendencias_humano: 0
---

# QA-001 — Setup Playwright + config base

---

## O que foi feito

Infraestrutura base do Playwright instalada em `frontend/`. Zero mudanças em `src/`.

**Dependências adicionadas** em `frontend/package.json` (devDependencies):
- `@playwright/test ^1.52.0`
- `@axe-core/playwright ^4.10.1`
- `tsx ^4.19.3`
- `mysql2 ^3.14.0`
- `dotenv ^16.5.0`

**Scripts adicionados** em `package.json`:
- `"e2e": "playwright test"`
- `"e2e:full": "tsx e2e/scripts/subir-stack.ts && playwright test; tsx e2e/scripts/derrubar-stack.ts"`
- `"e2e:ui": "playwright test --ui"`
- `"e2e:report": "playwright show-report"`

**`frontend/playwright.config.ts`** criado com:
- `baseURL` via `E2E_FRONTEND_URL` (fallback `http://localhost:5173`)
- `globalSetup: './e2e/fixtures/global-setup.ts'`
- `use: { trace: 'on-first-retry', screenshot: 'only-on-failure', video: 'retain-on-failure' }`
- `projects: [{ name: 'chromium' }]` — só Chromium no MVP
- Carrega `.env.e2e` via `dotenv.config({ path: '.env.e2e' })` (silencioso se arquivo ausente)

**`frontend/e2e/tsconfig.json`** criado — estende `../tsconfig.app.json` com ajustes para Playwright:
- `moduleResolution: "node"` (Playwright não usa bundler)
- `allowImportingTsExtensions: false`

**`frontend/.env.e2e.example`** versionado com 8 variáveis:
`E2E_DB_HOST`, `E2E_DB_PORT`, `E2E_DB_USER`, `E2E_DB_PASSWORD`, `E2E_DB_NAME`, `E2E_ADMIN_KEY`, `E2E_BACKEND_URL`, `E2E_FRONTEND_URL`.

**Pastas criadas:**
- `frontend/e2e/specs/.gitkeep`
- `frontend/e2e/fixtures/.gitkeep`

**`frontend/e2e/fixtures/global-setup.ts`** criado como stub no-op — ver desvio abaixo.

**`frontend/.gitignore`** atualizado: adicionado `frontend/.env.e2e` e `frontend/playwright-report/`.

**Vitest:** 61 testes, 12 arquivos — nenhuma regressão.

---

## Desvios do plano

**Desvio 1 (técnico, não de produto):** o plano previa deixar `e2e/fixtures/.gitkeep` apenas como placeholder e referenciar `global-setup.ts` por path sem o arquivo existir. Na prática, Playwright resolve `globalSetup` no carregamento do config (mesmo com `--list`), lançando `MODULE_NOT_FOUND`. Solução: criado um stub no-op `global-setup.ts` (7 linhas, apenas o export async vazio com comentário "será substituído em QA-003"). Isso não viola nenhuma regra do plano — o plano já antecipava que o arquivo seria criado em QA-003; o stub é o mínimo para que a config carregue sem erro.

---

## Decisões tomadas durante a execução

**Dotenv explícito:** em vez de `import 'dotenv/config'` (que carrega `.env` padrão do diretório), usei `dotenv.config({ path: '.env.e2e' })` em `playwright.config.ts`. Isso carrega o arquivo correto sem exigir que o usuário defina `DOTENV_CONFIG_PATH` como variável de ambiente antes de rodar. Comportamento silencioso quando `.env.e2e` não existe — Playwright funciona sem credenciais para `--list` e `--help`.

**Playwright `--list` com 0 specs:** retorna exit code 1 com mensagem "No tests found / Total: 0 tests in 0 files". Isso é comportamento normal do Playwright quando não há specs — não é erro de configuração. O critério de aceitação ("executa sem erro de importação/configuração") é satisfeito.

**`test-results/`** adicionado ao `.gitignore` junto com `playwright-report/` — o Playwright cria esse diretório com artifacts de falha (screenshots, vídeos, traces). Não commitado.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **QA-002** pode ser despachado imediatamente — usa os scripts de `package.json` desta task.
- **QA-003** (lote B) aguarda BE-023 mergeada em develop — vai substituir o stub `global-setup.ts` com a implementação real (`garantirRequisitanteE2E()`).
- `playwright install chromium` foi rodado manualmente para instalar o browser. Em ambiente novo, rodar esse comando antes de `npm run e2e`.
- O `e2e/tsconfig.json` usa `moduleResolution: "node"` — diferente do `tsconfig.app.json` que usa `"bundler"`. Isso é necessário porque o Playwright não passa pelo pipeline do Vite.
- MSW não interfere com Playwright: a config do Playwright é completamente separada de `vite.config.ts` e não carrega service workers.

---

## Padrões técnicos

Task de setup — sem lógica não-trivial. Config declarativa. Omitir seção de padrões.

---

## Arquivos criados/modificados

- `frontend/package.json` (modificado: +5 devDependencies, +4 scripts)
- `frontend/playwright.config.ts` (novo: config Playwright)
- `frontend/e2e/tsconfig.json` (novo: tsconfig isolado para E2E)
- `frontend/.env.e2e.example` (novo: template de credenciais, 8 variáveis)
- `frontend/e2e/specs/.gitkeep` (novo: placeholder da pasta de specs)
- `frontend/e2e/fixtures/.gitkeep` (novo: placeholder da pasta de fixtures)
- `frontend/e2e/fixtures/global-setup.ts` (novo: stub no-op — substituído em QA-003)
- `frontend/.gitignore` (modificado: +.env.e2e, +playwright-report/, +test-results/)
