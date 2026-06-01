---
name: Desenho da suíte E2E — decisões consolidadas
description: Decisões arquiteturais do desenho de testes E2E do projeto (orquestração, isolamento, ferramentas, ciclo)
type: project
---

Desenho de testes automatizados acordado com o humano em 2026-06-01. Spec em `docs/architecture/desenho-testes-automatizados.md`.

**Decisões consolidadas (6):**

1. **Seed do requisitante 99 — helper TS direto.** Sem migration Flyway, sem endpoint admin. O helper `e2e/fixtures/banco.ts` faz `INSERT IGNORE INTO requisitante (id=99, ...)` antes dos testes e cleanup `DELETE WHERE requisitante_id=99` no `beforeEach`. Mantém toda a infra de teste no escopo do front.

2. **Credenciais MySQL/admin — `.env.e2e` gitignored + `.env.e2e.example` versionado.** Segue o padrão Vite (`.env.development`, etc.) que o projeto já usa. Alinhado com convenção "secrets de dev não são commitados" (ver `project_convencao_secrets_dev.md`).

3. **Ciclo no PR — E + A + D combinados.** (E) Gating condicional: plano da task declara `exige_e2e: true|false` baseado em critérios (toca controller REST, DTO, auth, migration exposta via API, componente que consome API). (A) Implementador roda `e2e:full` quando exigido. (D) Reviewer NÃO roda — verifica via status report. Reviewer somente-leitura é propriedade defendida.

4. **Threshold de a11y — falha em `serious` + `critical`.** Tolera `minor` e `moderate` no início pra evitar falsos positivos de libs. Pode evoluir pra `moderate+` em sprint futura quando estabilizar. Snapshot baseline (alternativa D) descartado por virar tapete pra varrer dívida.

5. **Reportagem — HTML local + bloco no status report.** Status report ganha campo `e2e_full` no frontmatter (exigido, executado, status, specs_total, specs_passed, duracao_segundos, data_execucao) + seção textual com resumo dos specs. Reviewer audita pelo frontmatter. Não commitar `playwright-report/`.

6. **Cenários de webhook — spec parametrizada desde o MVP.** Em vez de N arquivos de spec por cenário, 1 arquivo com tabela de cenários (`describe.each` ou `forEach`). MVP inicia com 2-3 cenários (happy path foto+caption válida, texto puro, possivelmente sticker). Expansão na Fase 2 é trivial (1 linha na tabela). Limitação aceita: não cobre integração Telegram real (cert, setWebhook, ngrok) — isso continua na Camada 5 manual.

**Arquitetura base (anterior):**

- Stack local. MySQL persistente em `localhost:3306` (humano mantém rodando). Back e front subidos pelo agente via script Node (`npm run e2e:full`) que spawna `mvnw spring-boot:run -Dspring-boot.run.profiles=dev` e `vite`, espera healthcheck, roda Playwright, derruba processos. **Sem Docker Compose, sem CI.**
- Isolamento: requisitante `id=99 "E2E Tester"`. Cleanup deleta APENAS `requisitante_id=99`. Dados do Pedro (`id=1`) ficam intocados.
- Stack: Playwright + Chromium + @axe-core/playwright. Webhook E2E reusa Testcontainers (POST direto em `/webhook`, sem ngrok). Schemathesis fica na Fase 2.
- Estrutura: `frontend/e2e/` com `specs/`, `fixtures/`, `scripts/`, `playwright.config.ts`.
- MVP: 3 arquivos de spec — `site-fluxo-feliz.spec.ts`, `webhook-cenarios.spec.ts` (parametrizado, decisão 6), `a11y-home.spec.ts`.
- Gatilho: sob demanda local. Item novo no `docs/runbooks/PRE-MERGE-CHECKLIST.md`.

**Why:** humano quer validar que mudanças no backend não quebram o front sem precisar rodar manualmente o `ROTEIRO-INTEGRACAO-FRONT-BACK.md` (45-60 min). E2E automatizado reduz isso pra ~2 min.

**How to apply:** ao planejar tasks de testes E2E, respeitar essas convenções. Se requisitante 99 ainda não existir no banco, primeira execução do helper TS cria via INSERT IGNORE. Cleanup em `beforeEach`, não em `afterAll`. Status report sempre carrega frontmatter `e2e_full`. Reviewer nunca roda a suíte — só audita.
