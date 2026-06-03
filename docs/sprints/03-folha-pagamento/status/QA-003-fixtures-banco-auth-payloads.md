---
task: QA-003
titulo: "Fixtures — banco, auth, global-setup, payloads-telegram"
data: 2026-06-03
branch: feature/qa-003-fixtures-banco-auth-payloads
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
  - 57c493c
pr: null
desvios: 2
pendencias_humano: 0
---

# QA-003 — Fixtures (banco, auth, global-setup, payloads-telegram)

---

## O que foi feito

Quatro arquivos criados em `frontend/e2e/fixtures/`:

**`banco.ts`**
- Conexão singleton via `mysql2/promise`, credenciais de `.env.e2e`, erro explícito se vars ausentes.
- `garantirRequisitanteE2E()` — `INSERT IGNORE` do requisitante 99 (idempotente).
- `limparDadosE2E()` — DELETE em cascata de comprovantes → pedidos_pagamento → auth_token, sempre com filtro `requisitante_id = 99`. **Nunca toca `requisitante_id != 99`.**
- `semearPedidos(pedidos[])` — INSERT em `pedidos_pagamento` + comprovante se `sKeyComprovante` fornecido.
- `querySql<T>(sql, params?)` — query genérica para asserções.
- `fecharConexao()` — fecha a conexão (chamado em global-setup após init).

**`auth.ts`**
- `loginE2E(page)` — fluxo completo: POST `/admin/api/v1/requisitantes/99/convite` (header `X-Admin-Key`), extração do token via `new URL(url).searchParams.get('t')`, POST `/api/v1/auth/exchange`, parse do `Set-Cookie`, injeção de `finbot_session` via `page.context().addCookies()`.
- Erros explícitos em cada passo: vars ausentes, API retornou não-2xx, cookie ausente.

**`global-setup.ts`** (substitui o stub de QA-001)
- Valida existência física de `.env.e2e` via `fs.existsSync` antes de tudo.
- Lança `Error` claro com instrução `"Copie .env.e2e.example e preencha"` se ausente.
- Chama `garantirRequisitanteE2E()` e fecha a conexão ao final.

**`payloads-telegram.ts`**
- `telegramUpdateTextoPuro({ fromUserId, text })` — Update com `message.text`. `update_id` único por chamada (counter baseado em `Date.now()`).
- `telegramUpdateSticker({ fromUserId })` — Update com `message.sticker` (file_id sintético).
- TODO preservado: `// TODO Fase 1.1: adicionar telegramUpdateFotoLegenda após decisão de mock (ADR 00XX)`
- Exporta exatamente 2 factories no MVP.

**Removido:** `frontend/e2e/fixtures/.gitkeep` (placeholder de QA-001 — substituído pelos arquivos reais).

---

## Validações realizadas

| Validação | Resultado |
|---|---|
| `npm test` (Vitest) 61/61 | ✓ zero regressão |
| `npm run lint` | ✓ limpo |
| `npm run build` | ✓ exit 0 |
| `telegramUpdateTextoPuro` campos corretos | ✓ `update_id` number, `from.id=99`, `text` correto |
| `telegramUpdateSticker` campos corretos | ✓ `sticker.type='regular'`, `update_id` distinto do texto |
| `update_ids` únicos entre chamadas | ✓ counter incremental |
| Erro explícito com `.env.e2e` ausente | ✓ "Variáveis de banco ausentes. Verifique .env.e2e..." |
| `global-setup.ts` falha ruidosamente sem `.env.e2e` | ✓ (`.env.e2e` não existe no ambiente de CI desta sessão) |

**Validações que requerem `.env.e2e` preenchido** (não disponível nesta sessão):
- `garantirRequisitanteE2E()` idempotência (2x) — **não executado**, requer credenciais MySQL
- `limparDadosE2E()` preservação de Pedro (id=1) — **não executado**
- `loginE2E(page)` injeta cookie corretamente — **não executado**, requer backend rodando

Essas validações serão confirmadas no smoke do QA-004 (ciclo e2e:full completo).

---

## Desvios do plano

**Desvio 1 (técnico, não de produto):** `mensagem_processada` **não é limpa** em `limparDadosE2E()`. A spec do arquiteto (§7.2) mostrava `DELETE FROM mensagem_processada WHERE telegram_user_id = '99'`, mas a tabela V4 real não tem coluna `telegram_user_id` — tem `canal` e `id_externo` (o `update_id`). Como cada test run usa `update_id` únicos (counter em `payloads-telegram.ts`), não há colisão entre runs e a limpeza não é necessária. Se no futuro `mensagem_processada` precisar ser limpa, usar `DELETE WHERE canal='TELEGRAM' AND id_externo IN (select ids gerados no run)`.

**Desvio 2 (correção de infra, não de produto):** `e2e/tsconfig.json` corrigido — `allowImportingTsExtensions: false → true`. QA-001 setou explicitamente `false`, mas `global-setup.ts` (e `subir-stack.ts` de QA-002) usa extensão `.ts` no import path (padrão ESM/tsx necessário para runtime). Com `noEmit: true` já presente, `allowImportingTsExtensions: true` é válido e a correção elimina o erro `TS5097` que impedia o `tsc --noEmit` de passar nos arquivos e2e.

---

## Decisões tomadas durante a execução

**Schema verificado contra as migrations:** todos os campos do INSERT em `banco.ts` foram conferidos contra V1–V6. `requisitante` tem coluna `canal_preferido` (adicionada na V5) — incluída no INSERT com valor `'TELEGRAM'`. `pedidos_pagamento` tem `data_pedido DATE NOT NULL` (V2) — incluída no INSERT.

**`fecharConexao()` no global-setup:** após `garantirRequisitanteE2E()`, a conexão é fechada para liberar o socket. Cada test worker (Playwright) que precisar de banco abrirá sua própria conexão via `getConn()`. Evita keepalive desnecessário.

**dotenv carregado com path absoluto:** `banco.ts` e `auth.ts` calculam o path de `.env.e2e` usando `__dirname` + `resolve('../..')`. Isso funciona independentemente do cwd de onde o script é chamado — desde que estejam em `e2e/fixtures/`.

**`_smoke-banco.ts` não commitado:** criei um arquivo de smoke temporário para validação manual, mas o removi antes do commit — não é parte do escopo de QA-003.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo (QA-004)

- **Criar `.env.e2e`** a partir de `.env.e2e.example` antes de rodar os specs.
- `loginE2E(page)` depende do backend rodando (`E2E_BACKEND_URL=http://localhost:8080`) e do requisitante 99 existir no banco (garantido pelo `global-setup.ts`).
- `semearPedidos` insere com `data_pedido` obrigatório — usar `'YYYY-MM-DD'` no campo `dataPedido`.
- A propriedade `sKeyComprovante` é o `imagem_url` do comprovante (S3 key ou URL). Passar só para `status: 'PAGO'`.
- O campo `e2e_full` no frontmatter não é obrigatório ainda (per §9.2 do desenho-testes — fica para quando QA-004 e a infra completa estiverem em develop).

---

## Padrões e decisões técnicas

Esta task é infraestrutura de teste (QA), não feature React. Os padrões relevantes são de TypeScript/Node.js:

**Singleton pattern** — `banco.ts` usa uma variável module-level `_conn: Connection | null` com `getConn()` como acesso controlado. Problema resolvido: evitar abrir N conexões MySQL em paralelo quando múltiplos helpers chamam `banco.ts` no mesmo worker. Trade-off: é um singleton com estado global no módulo — ok para scripts de teste, mas evitar em código de produção.

**Fail-fast / Guard clauses** — `auth.ts` e `global-setup.ts` validam precondições no início (`!ADMIN_SECRET`, `!existsSync(envFile)`) e lançam `Error` com mensagem acionável antes de fazer qualquer I/O. Problema resolvido: falhas silenciosas (variável de ambiente não definida → fetch resulta em erro HTTP obscuro). Princípio aplicado: "fail loudly, fail early".

**Factory functions** — `payloads-telegram.ts` expõe `telegramUpdateTextoPuro` e `telegramUpdateSticker` como funções puras que retornam objetos tipados. Alternativa descartada: classes com `new TelegramUpdateBuilder()` seria over-engineering para objetos simples. Trade-off consciente: factories com `nextUpdateId()` têm side-effect (contador), mas a alternativa (passar `update_id` como parâmetro) adicionaria ruído desnecessário na maioria dos casos de uso.

**Separação de responsabilidades** — cada arquivo tem uma única responsabilidade: `banco.ts` = I/O MySQL, `auth.ts` = fluxo de autenticação, `global-setup.ts` = orquestração de bootstrap, `payloads-telegram.ts` = construção de dados sintéticos. `global-setup.ts` não contém SQL; `banco.ts` não contém lógica de auth. DIP aplicado: `global-setup.ts` depende da abstração exportada por `banco.ts`, não dos detalhes da conexão.

---

## Arquivos criados/modificados

- `frontend/e2e/fixtures/banco.ts` (novo: helpers DB com mysql2/promise)
- `frontend/e2e/fixtures/auth.ts` (novo: loginE2E via magic link)
- `frontend/e2e/fixtures/global-setup.ts` (modificado: substitui stub de QA-001)
- `frontend/e2e/fixtures/payloads-telegram.ts` (novo: 2 factories de Update)
- `frontend/e2e/fixtures/.gitkeep` (removido: placeholder de QA-001)
- `frontend/e2e/tsconfig.json` (modificado: `allowImportingTsExtensions: false → true`, corrige TS5097)
