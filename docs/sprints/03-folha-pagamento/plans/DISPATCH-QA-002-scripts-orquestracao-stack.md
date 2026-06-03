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
Task: QA-002 — Scripts de orquestração da stack (subir/derrubar/healthcheck/mock-telegram).

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

Criar 4 scripts TypeScript em frontend/e2e/scripts/ + 1 fixture de imagem:

### 1. aguardar-saude.ts
- Função: aguardarHealthcheck(url: string, timeoutMs: number): Promise<void>
- Polling com backoff exponencial (delay dobra a cada tentativa, cap em 5s)
- Se timeout: throw Error com mensagem "Healthcheck falhou: <url> não respondeu em <timeout>ms"

### 2. mock-telegram.ts
Servidor HTTP local que simula os endpoints de download de arquivo do Telegram.
Roda como processo SEPARADO (detached) na porta E2E_MOCK_TELEGRAM_PORT (default: 9090).

Rotas:
- GET /bot*/getFile?file_id=* → JSON: { ok: true, result: { file_path: "test/mock_foto.jpg" } }
- GET /file/bot*/test/mock_foto.jpg → bytes do arquivo frontend/e2e/fixtures/test-photo.jpg (Content-Type: image/jpeg)
- GET /health → 200 OK (plain text "ok")
- Qualquer outra rota → 404

O servidor lê o JPEG de `path.join(__dirname, '../fixtures/test-photo.jpg')`.

### 3. subir-stack.ts
Sequência de subida:
1. Carregar .env.e2e via dotenv (para que TELEGRAM_API_URL e demais vars entrem em process.env)
2. Verificar TCP em localhost:3306 (connect, 5s timeout) → falha rápida com msg clara se MySQL não responde
3. Spawn mock-telegram.ts como processo DETACHED:
   tsx e2e/scripts/mock-telegram.ts
   Aguardar: aguardarHealthcheck('http://localhost:9090/health', 5000)
   Salvar PID no objeto de PIDs
4. Spawn Spring Boot: mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ⚠️ Usar env: { ...process.env } no spawn para Spring Boot herdar TELEGRAM_API_URL do .env.e2e
   Aguardar: aguardarHealthcheck('http://localhost:8080/actuator/health', 60000)
   Salvar PID
5. Spawn Vite: vite --port 5173 --strictPort
   Aguardar: aguardarHealthcheck('http://localhost:5173', 30000)
   Salvar PID
6. Escrever frontend/.e2e-pids (JSON com 3 campos: mockTelegram, spring, vite)
7. Sair com exit code 0

### 4. derrubar-stack.ts
- Ler frontend/.e2e-pids
- SIGTERM para cada PID (mockTelegram, spring, vite); aguardar 5s; SIGKILL se ainda rodando
- Remover frontend/.e2e-pids

### Fixture: frontend/e2e/fixtures/test-photo.jpg
JPEG mínimo válido (1×1 pixel, ~150 bytes) — commitado no repo como fixture estática.
Servido pelo mock server para qualquer requisição de download de arquivo.
Gerar com sharp ou usar bytes hardcoded de JPEG válido mínimo.

## VARIÁVEL DE AMBIENTE DO SPRING BOOT

O Spring Boot usa Relaxed Binding: a propriedade `telegram.api.url` (ou equivalente)
é sobrescrita automaticamente pela env var correspondente em UPPERCASE com `_` no lugar de `.`

ANTES de commitar, fazer grep no backend para descobrir o nome exato da propriedade:
  Grep("telegram.*url", path="financas_bot_telegram/src/main/resources/application.properties")
  Grep("telegram.*url", path="financas_bot_telegram/src/main/resources/application.properties", case_insensitive=true)
  Grep("telegramApiUrl\|telegramApiBaseUrl\|telegram_api", path="financas_bot_telegram/src", type="java")

Converter para env var: substituir `.` e `-` por `_`, uppercase.
Ex: telegram.api.url → TELEGRAM_API_URL

Usar ESSE nome (verificado) em .env.e2e.example.

## REGRAS DURAS

1. Branch: `feature/qa-002-scripts-orquestracao-stack` saindo de `origin/integration/03-folha-pagamento`.
2. Território: SÓ `frontend/` para scripts e fixtures. Zero `financas_bot_telegram/` (Spring herda env var, sem alterar nenhum arquivo do back).
3. Atualizar frontend/package.json: script `e2e:full` para a sequência:
   tsx e2e/scripts/subir-stack.ts && playwright test; tsx e2e/scripts/derrubar-stack.ts
   (o ; garante que derrubar sempre roda mesmo se playwright falhar)
4. Adicionar `frontend/.e2e-pids` ao .gitignore.
5. Adicionar ao frontend/.env.e2e.example (com o nome da variável VERIFICADO no passo acima):
   TELEGRAM_API_URL=http://localhost:9090
   E2E_MOCK_TELEGRAM_PORT=9090
6. test-photo.jpg DEVE ser commitado (é fixture estática, não artefato gerado).
7. 1 commit: `feat(QA-002): scripts de orquestracao da stack (subir/derrubar/healthcheck/mock-telegram)`.
8. PR: `feature/qa-002-scripts-orquestracao-stack → integration/03-folha-pagamento`.
   Implementador pode aceitar o próprio PR (regra CLAUDE.md).

## VALIDAÇÕES

Com MySQL e Java disponíveis localmente:
1. tsx e2e/scripts/aguardar-saude.ts http://localhost:8080/actuator/health 10000 → exit 0 se back up
2. curl http://localhost:9090/health → 200 OK
3. curl "http://localhost:9090/bot123/getFile?file_id=TEST" → JSON com ok:true e file_path
4. curl http://localhost:9090/file/bot123/test/mock_foto.jpg → bytes JPEG (Content-Type: image/jpeg)
5. tsx e2e/scripts/subir-stack.ts → sobe mock+back+front; .e2e-pids tem 3 PIDs
6. tsx e2e/scripts/derrubar-stack.ts → mata os 3 processos
7. npm run e2e:full → ciclo completo, exit 0, aviso "no tests found" (esperado)

Com MySQL PARADO:
8. tsx e2e/scripts/subir-stack.ts → falha em < 5s com mensagem clara sobre MySQL

npm test (Vitest) deve continuar verde.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/QA-002-scripts-orquestracao-stack.md`

testes_novos: 0. exige_e2e_full: false.
Anotar qual validação manual foi feita e resultado.
Anotar o nome da env var encontrado no grep (TELEGRAM_API_URL ou outro).

## SE QUEBRAR

Cenário — mvnw não encontrado:
  Verificar se o script usa caminho relativo correto (../financas_bot_telegram/mvnw ou similar).
  Usar caminho absoluto via process.env ou caminho relativo ao worktree.

Cenário — porta 5173 ocupada:
  --strictPort faz vite falhar imediatamente com mensagem clara. OK.

Cenário — mock-telegram.ts não termina de subir antes do healthcheck:
  Aumentar timeout de 5s para 10s. O server é simples — se não sobe em 10s, há bug.

Cenário — Spring Boot não lê TELEGRAM_API_URL:
  Verificar se o nome da env var bate exatamente com a propriedade (Relaxed Binding é case-sensitive no sentido de que deve seguir a convenção).
  Adicionar log de startup no script: console.log('TELEGRAM_API_URL:', process.env.TELEGRAM_API_URL) antes do spawn.

Pare ao final do status report. PR para integration.
```

---

## Notas pro humano

- **Após merge desta:** QA-003 (lote B) pode iniciar se BE-023 já estiver em develop. QA-004 espera QA-002 + QA-003.
- **Estimativa:** 1-2h.

## Referências

- `docs/sprints/03-folha-pagamento/plans/QA-002-scripts-orquestracao-stack.md`
- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §4 (QA-002)
