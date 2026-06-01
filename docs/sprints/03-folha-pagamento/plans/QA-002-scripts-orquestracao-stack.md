---
task: QA-002
titulo: "Scripts de orquestração da stack (subir/derrubar/healthcheck/mock-telegram)"
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

Quatro scripts em `frontend/e2e/scripts/` + 1 fixture de imagem:

**`aguardar-saude.ts`** — polling com backoff exponencial. Função `aguardarHealthcheck(url: string, timeoutMs: number)`. Tenta GET a cada N ms (N dobra a cada tentativa até 5s). Retorna `Promise<void>` ou lança erro com mensagem clara.

**`mock-telegram.ts`** — servidor HTTP local que simula os dois endpoints de download de arquivo do Telegram. Roda como processo separado (detached) na porta `E2E_MOCK_TELEGRAM_PORT` (default: `9090`).
- `GET /bot*/getFile?file_id=*` → `{ ok: true, result: { file_path: "test/mock_foto.jpg" } }`
- `GET /file/bot*/test/mock_foto.jpg` → bytes do JPEG em `frontend/e2e/fixtures/test-photo.jpg`
- `GET /health` → `200 OK` (usado pelo healthcheck de subida)
- Qualquer outra rota → `404`

**`subir-stack.ts`** — sequência de subida:
1. Carrega `.env.e2e` via `dotenv` (para herdar `TELEGRAM_API_URL` e demais variáveis no ambiente).
2. Verifica TCP em `localhost:3306` — falha rápida se MySQL não responde (5s timeout).
3. Spawn `tsx e2e/scripts/mock-telegram.ts` como processo detached. Aguarda `GET /health` (5s). Salva PID.
4. Spawn `mvnw spring-boot:run -Dspring-boot.run.profiles=dev` **com `env: { ...process.env }`** — Spring Boot herda `TELEGRAM_API_URL=http://localhost:9090` do `.env.e2e`. Aguarda `GET /actuator/health` (60s). Salva PID.
5. Spawn `vite --port 5173 --strictPort` (front). Aguarda `GET /` (30s). Salva PID.
6. Escreve todos os PIDs em `frontend/.e2e-pids` (JSON: `{ mockTelegram, spring, vite }`).

**`derrubar-stack.ts`** — lê `.e2e-pids`, envia SIGTERM para cada PID (incluindo `mockTelegram`), aguarda 5s, SIGKILL se necessário. Remove `.e2e-pids`.

O script `e2e:full` no `package.json` (criado em QA-001) é atualizado para: `tsx e2e/scripts/subir-stack.ts && playwright test; tsx e2e/scripts/derrubar-stack.ts` (a segunda chamada usa `;` para rodar mesmo se playwright falhar — semântica try/finally).

### Configuração Spring Boot via env var (sem novo arquivo no back)

O Spring Boot faz Relaxed Binding: a propriedade `telegram.api.url` (ou equivalente — verificar nome exato em `application.properties` ou no adapter) é sobrescrita pela variável de ambiente correspondente (`TELEGRAM_API_URL`, `TELEGRAM_API_BASE_URL`, etc.). O implementador deve:
1. Grep a propriedade no `financas_bot_telegram/src/main/resources/application.properties` e no adapter de download.
2. Converter para env var (substituir `.` e `-` por `_`, uppercase).
3. Usar esse nome em `.env.e2e.example` com valor `http://localhost:9090`.

Sem criar nenhum arquivo no backend.

---

## Escopo / arquivos

### Criar
- `frontend/e2e/scripts/aguardar-saude.ts`
- `frontend/e2e/scripts/mock-telegram.ts`
- `frontend/e2e/scripts/subir-stack.ts`
- `frontend/e2e/scripts/derrubar-stack.ts`
- `frontend/e2e/fixtures/test-photo.jpg` — JPEG mínimo válido (1×1 pixel, ~150 bytes), commitado no repo. Servido pelo mock server para qualquer requisição de download de arquivo.

### Modificar
- `frontend/package.json` — atualizar script `e2e:full` com a sequência correta (se QA-001 deixou placeholder).
- `frontend/.env.e2e.example` — adicionar:
  ```
  TELEGRAM_API_URL=http://localhost:9090
  E2E_MOCK_TELEGRAM_PORT=9090
  ```
  ⚠️ O nome exato da variável (`TELEGRAM_API_URL` ou outro) deve ser verificado no adapter do back antes de commitar.
- `.gitignore` — adicionar `frontend/.e2e-pids`.

### Não tocar
- `frontend/src/` — zero código de produção.
- `playwright.config.ts` — criado em QA-001, sem mudança aqui.
- `financas_bot_telegram/` — zero arquivos no back (Spring herda via env var).

---

## Testes

Não aplicável: scripts de infra sem lógica de negócio testável por unit. Validação por execução manual/integração na QA-004.

`testes_total` esperado: sem mudança. `testes_novos: 0`.

---

## Critérios de aceitação

- [ ] `tsx e2e/scripts/aguardar-saude.ts http://localhost:8080/actuator/health 10000` retorna exit 0 se back estiver up, exit 1 com mensagem clara se não.
- [ ] Mock Telegram sobe em `:9090`: `curl http://localhost:9090/health` retorna 200.
- [ ] Mock Telegram responde corretamente: `curl "http://localhost:9090/bot123/getFile?file_id=TEST"` retorna JSON com `ok: true` e `file_path`.
- [ ] Mock Telegram serve o JPEG: `curl http://localhost:9090/file/bot123/test/mock_foto.jpg` retorna bytes com `Content-Type: image/jpeg`.
- [ ] Com back, MySQL e mock Telegram rodando: `tsx e2e/scripts/subir-stack.ts` sobe tudo e encerra. `.e2e-pids` contém 3 PIDs (`mockTelegram`, `spring`, `vite`).
- [ ] `tsx e2e/scripts/derrubar-stack.ts` mata os 3 processos.
- [ ] Se MySQL não responde em `localhost:3306`: `subir-stack.ts` falha em < 5s com mensagem clara.
- [ ] Spring Boot sobe com `TELEGRAM_API_URL` do `.env.e2e` visível no ambiente do processo (verificar via log de startup ou propriedade configurada).
- [ ] `npm run e2e:full` executa o ciclo completo (0 specs, exit 0 com aviso "no tests found").
- [ ] `frontend/.e2e-pids` listado em `.gitignore`.
- [ ] `frontend/e2e/fixtures/test-photo.jpg` **está commitado** (é fixture estática, não artefato gerado).
- [ ] `npm test` (Vitest) continua verde.
- [ ] Branch: `feature/qa-002-scripts-orquestracao-stack` saindo de `integration/03-folha-pagamento`.
- [ ] Status report `docs/sprints/03-folha-pagamento/status/QA-002-scripts-orquestracao-stack.md` com frontmatter válido.

---

## Fora de escopo

- Specs (QA-004), fixtures de banco/auth (QA-003).
- Script de CI para GitHub Actions — Fase 3 (Docker indisponível localmente, decisão arquitetural §5.1 item 1).
- `kill_orphan_processes.sh` — sugestão de troubleshooting documentada no ROTEIRO-E2E (QA-005), não script real.
- Limpeza de S3 pós-teste — objetos de teste (`requisitante_id=99`) acumulam no bucket dev, aceito para Fase 1. Fase 2 pode adicionar prefix `e2e/` e cleanup via SDK.

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
