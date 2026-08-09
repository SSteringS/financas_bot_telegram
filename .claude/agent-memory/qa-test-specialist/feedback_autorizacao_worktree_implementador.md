---
name: QA está autorizado a executar testes no worktree do implementador
description: Permissão permanente do humano pra qa-test-specialist rodar Bash/Read em coisas de teste automatizado fora do worktree planner
type: feedback
---

O `qa-test-specialist` está autorizado a fazer **Read e Bash** (mas não Edit/Write, salvo pedido explícito) no worktree do implementador (`C:\Users\satya\src\financas_bot_telegram\`) **quando o objetivo é executar ou inspecionar testes automatizados** — typicamente E2E Playwright que precisam de `node_modules`, `.env.e2e` e stack subida lá.

**Why:** o humano em 2026-06-03 confirmou que coisas relacionadas a testes automatizados fazem parte do território do QA, então a regra geral do CLAUDE.md raiz ("Claude para e pergunta antes de tocar paths no mount do implementador") não se aplica nessa categoria. A regra ainda vale pra qualquer outra coisa (código de produção, mudar config do app, etc.).

**How to apply:**
- Pode rodar `npm run e2e`, `npm run e2e:full`, `npm run e2e:ui`, `tsx e2e/scripts/*` direto via Bash com cwd no worktree implementador, sem pedir permissão a cada vez.
- Pode ler `.e2e-pids`, `playwright-report/`, specs e fixtures via Read absoluto.
- **NÃO** pode editar `frontend/src/` (código de produção) nem `financas_bot_telegram/src/main/` (back) — sai do escopo de "teste automatizado".
- **NÃO** pode rodar comandos que mudem estado git no worktree implementador (commit, push, branch) sem permissão explícita — autorização é pra execução de teste, não pra operação git.
- Se em dúvida sobre se algo conta como "teste automatizado", pergunta.
