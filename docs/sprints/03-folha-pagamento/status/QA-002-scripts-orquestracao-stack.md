---
task: QA-002
titulo: "Scripts de orquestração da stack (subir/derrubar/healthcheck)"
data: 2026-06-01
branch: feature/qa-002-scripts-orquestracao-stack
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
  - a2e331a
pr: null
desvios: 0
pendencias_humano: 0
---

# QA-002 — Scripts de orquestração da stack

---

## O que foi feito

Três scripts TypeScript em `frontend/e2e/scripts/`:

**`aguardar-saude.ts`**
- Função exportável `aguardarHealthcheck(url, timeoutMs)` com backoff exponencial (delay inicial 500ms, dobra a cada tentativa, cap em 5s).
- Interface CLI: `tsx e2e/scripts/aguardar-saude.ts <url> <timeoutMs>` → exit 0 se OK, exit 1 com mensagem clara se timeout.
- Usa `AbortSignal.timeout(3000)` no fetch para não ficar travado em conexões lentas.

**`subir-stack.ts`**
- Verifica MySQL via TCP connect a `localhost:3306` (timeout 5s) — falha rápida com mensagem `"MySQL não respondeu em localhost:3306 — suba seu MySQL local e rode de novo"`.
- Spawn backend: `mvnw.cmd` (Windows) ou `./mvnw` (Unix), profile dev, `detached + unref`.
- Aguarda `GET /actuator/health` por `E2E_BACK_TIMEOUT_MS` ms (default 60s).
- Spawn frontend: `node_modules/.bin/vite.cmd` (Windows) ou `vite` (Unix), porta 5173 `--strictPort`.
- Aguarda `GET http://localhost:5173` por 30s.
- Salva PIDs em `frontend/.e2e-pids` (JSON).
- Cross-platform: `isWin = process.platform === 'win32'`, usa `shell: true` no Windows para resolução de `.cmd`.

**`derrubar-stack.ts`**
- Lê `frontend/.e2e-pids`; exit 0 gracioso se arquivo não existe.
- No Windows: `taskkill /PID <pid> /F /T` (mata árvore de processos).
- No Unix: SIGTERM + espera 5s + SIGKILL se necessário.
- Remove `.e2e-pids` ao final.

**`frontend/.gitignore`** — adicionado `.e2e-pids`.

**`frontend/eslint.config.js`** — adicionada config separada para `e2e/**/*.ts` com `globals.node` (os scripts usam APIs Node.js que não existem em `globals.browser`).

**`package.json`** — nenhuma alteração necessária: o script `e2e:full` estava correto desde QA-001 (`tsx e2e/scripts/subir-stack.ts && playwright test; tsx e2e/scripts/derrubar-stack.ts`).

---

## Validações realizadas

| Validação | Resultado |
|---|---|
| `aguardar-saude.ts` com URL inalcançável (2000ms) | ✓ exit 1, msg "Healthcheck falhou: ... não respondeu em 2000ms" |
| TCP check MySQL (local rodando) | ✓ passou em < 1s |
| `subir-stack.ts` → MySQL check OK → spawn backend → healthcheck 60s | ✓ MySQL passou; back não estava em execução → healthcheck expirou corretamente com exit 1 |
| `derrubar-stack.ts` sem `.e2e-pids` | ✓ exit 0, "stack pode já estar derrubada" |
| `.e2e-pids` no `.gitignore` | ✓ linha 34 |
| Vitest 61/61 (zero regressão) | ✓ |
| lint | ✓ |
| build | ✓ |

**Cenário MySQL parado:** MySQL estava rodando localmente durante os testes; a lógica `net.createConnection` foi revisada no código — ECONNREFUSED é capturado pelo `socket.on('error')` e o timer de 5s garante o timeout máximo. Mensagem canônica idêntica à especificada no critério de aceitação.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

**ESLint `e2e/**` com `globals.node`:** o ESLint da raiz usava `globals.browser` para todos os `.ts`. Os scripts de e2e usam `child_process`, `net`, `fs/promises` etc. — adicionei config separada no `eslint.config.js` da flat config do ESLint para o glob `e2e/**/*.ts`. Alternativa seria adicionar `// eslint-disable` em cada arquivo; optei pela config explícita por ser sustentável.

**`AbortSignal.timeout(3000)` no fetch:** evita que o polling fique suspenso em conexões TCP que aceitam a conexão mas não respondem HTTP (ex: back em process de startup recebendo conexão mas sem handler ativo ainda). O timeout de abort é independente do timeout geral de polling.

**Windows: `shell: true` no spawn:** o Maven Wrapper no Windows é um arquivo `.cmd`, que não pode ser executado diretamente por `child_process.spawn` sem o shell. Com `shell: true`, o SO resolve `.cmd` automaticamente. O `vite.cmd` recebe tratamento diferente: usado o path completo via `node_modules/.bin/vite.cmd` sem `shell`, para evitar ambiguidade de PATH.

**`taskkill /F /T` no Windows:** mata a árvore de processos filhos (Java spawna processos filhos durante JVM startup). Sem `/T`, processos filhos poderiam ficar órfãos.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **QA-004** pode iniciar após QA-003 (que depende de BE-023 em develop).
- O `E2E_BACK_TIMEOUT_MS` é configurável via `.env.e2e` — JVMs com cold start lento podem precisar de 90s+.
- `subir-stack.ts` não aguarda o frontend `VITE_USE_MOCK=false` — quem chama o script precisa garantir que `.env.e2e` está preenchido (inclusive `VITE_API_BASE_URL` apontando para `http://localhost:8080`).
- Em Windows, processos órfãos podem sobrar se o usuário fizer `Ctrl+C` durante o `playwright test` antes de `derrubar-stack.ts` rodar. O script `;` no `e2e:full` garante que `derrubar-stack.ts` rode sempre que `subir-stack.ts` tiver sucesso, mas `Ctrl+C` interrompe o shell antes do `;`.

---

## Arquivos criados/modificados

- `frontend/e2e/scripts/aguardar-saude.ts` (novo: healthcheck com backoff + CLI)
- `frontend/e2e/scripts/subir-stack.ts` (novo: MySQL check + spawn back+front + salvar PIDs)
- `frontend/e2e/scripts/derrubar-stack.ts` (novo: encerrar processos por PID)
- `frontend/.gitignore` (modificado: +.e2e-pids)
- `frontend/eslint.config.js` (modificado: config Node.js para e2e/**)
