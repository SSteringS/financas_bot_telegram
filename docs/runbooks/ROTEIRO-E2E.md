# Roteiro — Suíte E2E Automatizada (Playwright)

> **Status:** ativo a partir da sprint 03-folha-pagamento — Fase 1 completa.
> Referência arquitetural: `docs/architecture/desenho-testes-automatizados.md`.
> ADR relacionado: `docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md`.

---

## 1. Pré-requisitos

Antes de rodar a suíte, garantir que o ambiente local está pronto:

| Requisito | Como verificar |
|---|---|
| MySQL 8.x rodando em `localhost:3306` | `mysql -u root -e "select 1"` |
| Java 21 disponível no PATH | `java -version` |
| Node.js 20+ disponível | `node -v` |
| Maven Wrapper presente | `ls financas_bot_telegram/mvnw` |
| `.env.e2e` preenchido em `frontend/` | `cat frontend/.env.e2e` |

> **Docker NÃO é necessário.** A suíte usa MySQL e Java rodando diretamente no host.

### Arquivo `.env.e2e`

Copiar o template e preencher:

```bash
cp frontend/.env.e2e.example frontend/.env.e2e
# Editar frontend/.env.e2e com as credenciais do ambiente de DEV
```

Variáveis obrigatórias:

```env
E2E_FRONTEND_URL=http://localhost:5173
E2E_BACKEND_URL=http://localhost:8080
E2E_DB_HOST=localhost
E2E_DB_PORT=3306
E2E_DB_NAME=finbot_dev
E2E_DB_USER=...
E2E_DB_PASSWORD=...
E2E_ADMIN_SECRET=...
E2E_WEBHOOK_SECRET=...
TELEGRAM_API_URL=http://localhost:9090
E2E_MOCK_TELEGRAM_PORT=9090
```

> **`TELEGRAM_API_URL`** aponta o Spring Boot para o mock Telegram local (porta 9090) em vez da API real. Isso permite testar o cenário de foto+caption sem conexão externa. O mock server é iniciado automaticamente por `subir-stack.ts`.

> ⚠️ **Nunca preencher `.env.e2e` com credenciais de produção.** (Ver §7 abaixo.)

---

## 2. Como rodar

### Ciclo completo (recomendado)

```bash
cd frontend
npm run e2e:full
```

Este comando executa em sequência:

1. **`subir-stack.ts`** — verifica MySQL, sobe o **mock Telegram** (`:9090`), o backend Spring Boot (`dev` profile, com `TELEGRAM_API_URL` injetada via env) e o Vite dev server. Aguarda healthchecks antes de prosseguir.
2. **`playwright test`** — executa todas as specs em `e2e/specs/`.
3. **`derrubar-stack.ts`** — para todos os processos incluindo o mock Telegram (mesmo se os testes falharem).

> O `derrubar-stack` roda **sempre**, mesmo com testes vermelhos — o `;` entre os comandos garante isso.

### Só os testes (stack já rodando)

```bash
cd frontend
npm run e2e
```

Use quando o backend e o frontend já estão rodando (ex.: durante desenvolvimento ativo).

### Modo debug interativo

```bash
cd frontend
npm run e2e:ui
```

Abre o Playwright UI — permite ver cada passo, pausar, inspecionar o DOM, ver screenshots e traces em tempo real.

### Spec específica

```bash
cd frontend
npx playwright test e2e/specs/site-fluxo-feliz.spec.ts
```

---

## 3. Como interpretar o resultado

### Terminal

Saída de sucesso:

```
  ✓  site-fluxo-feliz.spec.ts (1 test)  1.2s
  ✓  webhook-cenarios.spec.ts (3 tests)  4.8s
  ✓  a11y-home.spec.ts (1 test)  0.8s

  5 passed (6.8s)
```

Saída de falha:

```
  ✗  webhook-cenarios.spec.ts (3 tests)  2.1s
    1 failed

  ● webhook recebe e processa: texto puro

    Expected: 200
    Received: 401
```

### Relatório HTML (com screenshots, vídeos e traces)

```bash
cd frontend
npm run e2e:report
```

Abre `frontend/playwright-report/index.html` no browser. Para cada teste com falha:

- **Screenshot** — captura do momento exato da falha.
- **Trace** — replay passo-a-passo navegável (DOM, rede, console).
- **Vídeo** — gravação do teste completo.

> O `playwright-report/` é gerado localmente e **não é commitado** (está no `.gitignore`).

### Códigos de saída

| Código | Significado |
|---|---|
| `0` | Todos os testes passaram |
| `1` | Um ou mais testes falharam |
| `1` (em `subir-stack.ts`) | Stack não subiu — verificar §4 (Troubleshooting) |

---

## 4. Troubleshooting

### MySQL não rodando

```
Error: Healthcheck falhou: localhost:3306 não respondeu em 5000ms
```

**Fix:** iniciar o MySQL localmente antes de rodar `npm run e2e:full`.

### `.env.e2e` ausente ou incompleto

```
Error: Arquivo .env.e2e não encontrado. Copie .env.e2e.example e preencha.
```

**Fix:** copiar `frontend/.env.e2e.example` → `frontend/.env.e2e` e preencher todas as variáveis.

### Porta ocupada (5173 ou 8080)

Vite usa `--strictPort` — falhará imediatamente com mensagem clara se a porta estiver ocupada.

**Fix:** parar o processo que está usando a porta antes de rodar o ciclo completo. No Windows:
```bash
netstat -ano | findstr :5173
taskkill /PID <pid> /F
```

### Stack travada no healthcheck

```
Error: Healthcheck falhou: http://localhost:8080/actuator/health não respondeu em 60000ms
```

O backend demorou mais de 60s para subir. Causas comuns:
- Primeira build do Maven (baixando dependências) — aguardar mais e rodar novamente.
- Erro de configuração do Spring (ver console do processo spawned).
- Banco de dados inacessível — verificar se MySQL está rodando.

**Fix:** rodar `tsx e2e/scripts/subir-stack.ts` manualmente e observar o output para identificar o erro.

### Mock Telegram não sobe (porta 9090)

```
Error: Healthcheck falhou: http://localhost:9090/health não respondeu em 5000ms
```

O processo `mock-telegram.ts` não iniciou corretamente. Causas comuns:
- Porta 9090 já ocupada por outro processo.
- `test-photo.jpg` ausente em `frontend/e2e/fixtures/` (não foi commitado).

**Fix:**
```bash
# Verificar porta ocupada
netstat -ano | findstr :9090
# Verificar que o JPEG existe
ls frontend/e2e/fixtures/test-photo.jpg
# Testar o mock manualmente
tsx frontend/e2e/scripts/mock-telegram.ts &
curl http://localhost:9090/health
curl "http://localhost:9090/bot123/getFile?file_id=TEST"
```

### Cenário foto+caption retorna 500 ou 422 no webhook

O Spring Boot tentou baixar o arquivo do Telegram real (não do mock). Causas:
- `TELEGRAM_API_URL` ausente ou incorreto no `.env.e2e`.
- `subir-stack.ts` não carregou o `.env.e2e` antes de spawnar o Spring Boot.

**Fix:** verificar `E2E_BACKEND_URL` e `TELEGRAM_API_URL` no `.env.e2e`. Confirmar que o Spring Boot foi spawned com `env: { ...process.env }`.

### Suíte passa localmente mas falha em outra máquina

Verificar se `.env.e2e` tem as credenciais corretas para **aquele** ambiente. Nunca compartilhar o `.env.e2e` — o `.env.e2e.example` é o template compartilhado.

### `npm run e2e:full` sai sem rodar testes ("no tests found")

Normal enquanto `e2e/specs/` está vazio. Isso ocorre após QA-001 (setup) mas antes de QA-004 (specs). Verificar que as specs existem em `frontend/e2e/specs/`.

---

## 5. Relação com o roteiro manual (`ROTEIRO-INTEGRACAO-FRONT-BACK.md`)

| Aspecto | Suíte E2E (este roteiro) | Roteiro manual |
|---|---|---|
| Tempo | ~2 min | 45-60 min |
| Automático | sim (`npm run e2e:full`) | não |
| Cobre | Camadas 1-4 (site fluxo feliz, webhook POST, a11y) | Camadas 1-5 |
| Telegram real | não (usa POST direto ao webhook) | sim (via ngrok) |
| Cobertura de a11y | sim (axe-core automático) | não |

**Regra:** a suíte E2E **substitui** o roteiro manual para todos os cenários cobertos. O roteiro manual permanece como:

1. **Fallback** quando a suíte não pode rodar (ex.: MySQL indisponível).
2. **Cobertura da Camada 5** (Telegram real, ngrok, upload de comprovante via bot) — esses cenários **não** têm automação na Fase 1.
3. **Diagnóstico manual** quando um teste E2E falha e o trace não é suficiente para identificar a causa.

---

## 6. Como adicionar um cenário novo

### Webhook (cenário parametrizado)

Abrir `frontend/e2e/specs/webhook-cenarios.spec.ts` e adicionar uma linha na tabela `cenarios`:

```typescript
const cenarios = [
  { nome: 'texto puro', payload: telegramUpdateTextoPuro({ fromUserId: 99, text: 'Oi bot' }) },
  { nome: 'sticker', payload: telegramUpdateSticker({ fromUserId: 99 }) },
  { nome: 'foto com caption', payload: telegramUpdateFotoLegenda({ fromUserId: 99, fileId: E2E_MOCK_FILE_ID, caption: 'comprovante fev' }) },
  // Adicionar novos cenários aqui — cada linha = 1 test() adicional
  { nome: 'novo cenario', payload: telegramUpdateNovoCenario({ fromUserId: 99 }) },
];
```

Também criar o factory correspondente em `frontend/e2e/fixtures/payloads-telegram.ts`.

> **Cenário com download de arquivo** (ex.: vídeo, documento): o mock Telegram (`:9090`) responde para qualquer `file_id` com o `test-photo.jpg`. Se o cenário exigir conteúdo diferente (ex.: PDF), estender o mock server para servir o arquivo correto baseado no `file_id` recebido.

### Fluxo de site novo

Criar um novo `.spec.ts` em `frontend/e2e/specs/`. O Playwright descobre specs automaticamente por glob.

### Checklist antes de commitar spec nova

- [ ] Spec roda 3 vezes seguidas verde em ambiente limpo.
- [ ] `limparDadosE2E()` chamado no início de cada teste que cria dados.
- [ ] Nenhum `requisitante_id` diferente de `99` nos asserts do banco.
- [ ] `playwright-report/` não está no diff do commit.

---

## 7. ⚠️ Aviso sobre dados — leia com atenção

**O cleanup automático limpa SOMENTE registros com `requisitante_id = 99`.**

O usuário E2E (id=99) é um sentinel dedicado, criado pelo `global-setup.ts` no início de cada rodada. Nenhuma operação da suíte toca dados de outros usuários.

**NUNCA rodar `npm run e2e:full` apontado para um banco de produção.** O `.env.e2e` deve sempre conter credenciais do ambiente de DEV local. Se suspeitar que `.env.e2e` aponta para produção: pare imediatamente, verificar `E2E_DB_HOST` e `E2E_DB_NAME`.

Se o cleanup falhar (ex.: conexão com banco perdida no meio), dados do `requisitante_id=99` podem ficar órfãos. Limpar manualmente se necessário:

```sql
DELETE FROM pedidos_pagamento WHERE requisitante_id = 99;
-- Nunca remover o próprio usuário 99 da tabela de requisitantes
-- (global-setup usa INSERT IGNORE — ele se auto-recria na próxima rodada)
```
