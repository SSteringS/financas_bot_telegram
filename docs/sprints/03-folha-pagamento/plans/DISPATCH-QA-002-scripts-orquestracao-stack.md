# DISPATCH — QA-002-scripts-orquestracao-stack (single-task)

> **Lote A — despachar após QA-001 mergear na integration branch.**
> Precisa do package.json com `tsx` instalado (de QA-001).

---

## Pré-condições (git)

- `feature/qa-001-setup-playwright-base` mergeada em `integration/03-folha-pagamento`.
  Confirmar: `git log origin/integration/03-folha-pagamento --oneline | grep qa-001`.

---

## O prompt (cole tudo numa sessão `--agent frontend`)

```
Task: QA-002 — Scripts de orquestração da stack.

Localize e leia o plano:
  Glob("docs/sprints/03-folha-pagamento/plans/QA-002-scripts-orquestracao-stack.md")

Leia também:
- docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md §4 (QA-002)
- docs/architecture/desenho-testes-automatizados.md §14.2 (ciclo e2e:full)

## ATENÇÃO — BRANCH

Branch a partir de `integration/03-folha-pagamento` (que já tem QA-001):
  git fetch
  git checkout -b feature/qa-002-scripts-orquestracao-stack origin/integration/03-folha-pagamento

## A TASK

Criar 3 scripts TypeScript em frontend/e2e/scripts/:

1. aguardar-saude.ts
   - Função: aguardarHealthcheck(url: string, timeoutMs: number): Promise<void>
   - Polling com backoff exponencial (delay dobra a cada tentativa, cap em 5s)
   - Se timeout: throw Error com mensagem "Healthcheck falhou: <url> não respondeu em <timeout>ms"

2. subir-stack.ts
   - Verificar MySQL em localhost:3306 (TCP connect, 5s timeout) → falha rápida com msg clara
   - Spawn: mvnw spring-boot:run -Dspring-boot.run.profiles=dev (background)
   - Aguardar: aguardarHealthcheck('http://localhost:8080/actuator/health', 60000)
   - Spawn: vite --port 5173 --strictPort (background)
   - Aguardar: aguardarHealthcheck('http://localhost:5173', 30000)
   - Salvar PIDs em frontend/.e2e-pids (JSON)
   - Sair com exit code 0

3. derrubar-stack.ts
   - Ler frontend/.e2e-pids
   - SIGTERM para cada PID; aguardar 5s; SIGKILL se ainda rodando
   - Remover frontend/.e2e-pids

## REGRAS DURAS

1. Branch: `feature/qa-002-scripts-orquestracao-stack` saindo de `origin/integration/03-folha-pagamento`.
2. Território: SÓ `frontend/`. Zero `financas_bot_telegram/`.
3. Atualizar frontend/package.json: script `e2e:full` para chamar subir-stack → playwright test ; derrubar-stack (o ; garante que derrubar sempre roda).
4. Adicionar `frontend/.e2e-pids` ao .gitignore.
5. 1 commit: `feat(QA-002): scripts de orquestracao da stack (subir/derrubar/healthcheck)`.
6. PR: `feature/qa-002-scripts-orquestracao-stack → integration/03-folha-pagamento`.
   Implementador pode aceitar o próprio PR (regra CLAUDE.md).

## VALIDAÇÕES

Com MySQL e Java disponíveis localmente:
1. tsx e2e/scripts/aguardar-saude.ts http://localhost:8080/actuator/health 10000 → exit 0 se back up
2. tsx e2e/scripts/subir-stack.ts → sobe back+front (verificação manual)
3. tsx e2e/scripts/derrubar-stack.ts → mata processos
4. npm run e2e:full → ciclo completo, exit 0, aviso "no tests found" (esperado — sem specs ainda)

Com MySQL PARADO:
5. tsx e2e/scripts/subir-stack.ts → falha em < 5s com mensagem clara sobre MySQL

npm test (Vitest) deve continuar verde.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/QA-002-scripts-orquestracao-stack.md`

testes_novos: 0. exige_e2e_full: false.
Anotar qual validação manual foi feita e resultado.

## SE QUEBRAR

Cenário — mvnw não encontrado:
  Verificar se o script usa caminho relativo correto (../financas_bot_telegram/mvnw ou similar).
  Usar caminho absoluto via process.env ou caminho relativo ao worktree.

Cenário — porta 5173 ocupada:
  --strictPort faz vite falhar imediatamente com mensagem clara. OK.

Pare ao final do status report. PR para integration.
```

---

## Notas pro humano

- **Após merge desta:** QA-003 (lote B) pode iniciar se BE-023 já estiver em develop. QA-004 espera QA-002 + QA-003.
- **Estimativa:** 1-2h.

## Referências

- `docs/sprints/03-folha-pagamento/plans/QA-002-scripts-orquestracao-stack.md`
- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §4 (QA-002)
