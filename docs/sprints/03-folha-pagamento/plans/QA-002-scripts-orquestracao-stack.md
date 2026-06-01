---
task: QA-002
titulo: "Scripts de orquestração da stack (subir/derrubar/healthcheck)"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-01
branch_alvo: feature/qa-002-scripts-orquestracao-stack
integration_branch: integration/03-folha-pagamento
prioridade: alta
esforco: medio
territorio: front
estado: pronto-pra-execucao
depende_de: [QA-001]
bloqueia: [QA-004]
skills_dispatched: []
exige_e2e_full: false
lote: A
---

# QA-002 — Scripts de orquestração da stack

## Intake

- **Origem:** spec QA Fase 1 §4 (QA-002). Lote A — independente da sprint 03 de produto.
- **Por quê agora:** o comando `npm run e2e:full` precisa dos scripts de subir/derrubar stack para funcionar. Sem eles, QA-004 não pode rodar o ciclo completo.
- **Esforço:** médio — lógica de spawn de processos, healthcheck com polling exponencial, orquestração try/finally.
- **Riscos resumidos:** race condition entre subir back e front (back demora mais); timeout de healthcheck se MySQL local não estiver rodando; processo back ficando órfão após Ctrl+C.

---

## Contexto

Após QA-001, `package.json` tem os scripts `e2e:full` e `e2e` mas os scripts TypeScript referenciados ainda não existem. Esta task os cria.

Stack local esperada:
- MySQL 8.0 em `localhost:3306` (rodado pelo desenvolvedor antes de iniciar)
- Back Spring Boot em `localhost:8080`
- Front Vite em `localhost:5173`

Os scripts usam `tsx` (já instalado em QA-001) para executar TypeScript direto.

---

## Decisão / abordagem

Três scripts em `frontend/e2e/scripts/`:

**`aguardar-saude.ts`** — polling com backoff exponencial. Função `aguardarHealthcheck(url: string, timeoutMs: number)`. Tenta GET a cada N ms (N dobra a cada tentativa até 5s). Retorna `Promise<void>` ou lança erro com mensagem clara.

**`subir-stack.ts`** — spawn `mvnw spring-boot:run` (back, profile dev) em background, aguarda `GET /actuator/health` (60s). Spawn `vite` (front) em background, aguarda `GET /` (30s). Falha rápida se `localhost:3306` não responde (TCP connect, 5s). Salva PIDs em `.e2e-pids` na raiz de `frontend/`.

**`derrubar-stack.ts`** — lê `.e2e-pids`, envia SIGTERM pros processos, aguarda saída (timeout 5s), SIGKILL se necessário. Remove `.e2e-pids`.

O script `e2e:full` no `package.json` (criado em QA-001) é atualizado para: `tsx e2e/scripts/subir-stack.ts && playwright test; tsx e2e/scripts/derrubar-stack.ts` (a segunda chamada usa `;` para rodar mesmo se playwright falhar — semântica try/finally).

---

## Escopo / arquivos

### Criar
- `frontend/e2e/scripts/aguardar-saude.ts`
- `frontend/e2e/scripts/subir-stack.ts`
- `frontend/e2e/scripts/derrubar-stack.ts`

### Modificar
- `frontend/package.json` — atualizar script `e2e:full` com a sequência correta (se QA-001 deixou placeholder).
- `.gitignore` — adicionar `frontend/.e2e-pids`.

### Não tocar
- `frontend/src/` — zero código de produção.
- `playwright.config.ts` — criado em QA-001, sem mudança aqui.

---

## Testes

Não aplicável: scripts de infra sem lógica de negócio testável por unit. Validação por execução manual/integração na QA-004.

`testes_total` esperado: sem mudança. `testes_novos: 0`.

---

## Critérios de aceitação

- [ ] `tsx e2e/scripts/aguardar-saude.ts http://localhost:8080/actuator/health 10000` retorna exit 0 se back estiver up, exit 1 com mensagem clara se não.
- [ ] Com back e MySQL rodando: `tsx e2e/scripts/subir-stack.ts` sobe ambos e encerra (verifica-se manualmente que back+front estão respondendo).
- [ ] `tsx e2e/scripts/derrubar-stack.ts` mata os processos iniciados.
- [ ] Se MySQL não responde em `localhost:3306`: `subir-stack.ts` falha em < 5s com mensagem `"MySQL não respondeu em localhost:3306 — suba seu MySQL local e rode de novo"`.
- [ ] `npm run e2e:full` executa o ciclo completo (0 specs, exit 0 com aviso "no tests found").
- [ ] `frontend/.e2e-pids` listado em `.gitignore`.
- [ ] `npm test` (Vitest) continua verde.
- [ ] Branch: `feature/qa-002-scripts-orquestracao-stack` saindo de `integration/03-folha-pagamento`.
- [ ] Status report `docs/sprints/03-folha-pagamento/status/QA-002-scripts-orquestracao-stack.md` com frontmatter válido.

---

## Fora de escopo

- Specs (QA-004), fixtures (QA-003).
- Script de CI para GitHub Actions — Fase 3 (Docker indisponível localmente, decisão arquitetural §5.1 item 1).
- `kill_orphan_processes.sh` — sugestão de troubleshooting documentada no ROTEIRO-E2E (QA-005), não script real.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Back demora >60s pra subir (JVM cold start) | Média | Médio | Timeout configurável via env `E2E_BACK_TIMEOUT_MS`; default 60s; aumentar pra 90s se necessário |
| Processo back fica órfão após `Ctrl+C` | Baixa | Baixo | try/finally no `e2e:full`; `.e2e-pids` permite retomar; documentar no ROTEIRO-E2E |
| `vite` trocando de porta se 5173 estiver ocupada | Baixa | Médio | Fixar porta via `--port 5173 --strictPort` no spawn |

---

## Coordenação

- **Pode rodar em paralelo com:** QA-003 (lote B, após BE-023 mergear), QA-005, QA-006.
- **Depende sequencialmente de:** QA-001 (package.json com tsx e scripts registrados).
- **Bloqueia:** QA-004 (precisa dos scripts para `npm run e2e:full`).
- **Atenção pro Reviewer:** validar manualmente o ciclo `subir + derrubar` pelo menos uma vez antes de aprovar; confirmar que `.e2e-pids` está no `.gitignore`.
- **Após merge:** QA-004 pode iniciar (junto com QA-003 se BE-023 já estiver em develop).

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR `feature → integration/03-folha-pagamento`.

`exige_e2e_full: false`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §4 (QA-002)
- `docs/architecture/desenho-testes-automatizados.md` §14.2 (ciclo e2e:full)
