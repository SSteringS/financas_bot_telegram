# DISPATCH — QA-004-specs-mvp-3-cenarios (single-task)

> **Lote B — despachar somente após QA-002 E QA-003 em `develop`.**
> É o entregável final da Fase 1. **2 specs: fluxo feliz + webhook (3 cenários: texto puro + sticker + foto+caption).**
> O mock Telegram (QA-002) resolve o download de arquivo via `TELEGRAM_API_URL=http://localhost:9090`.
>
> ⚠️ **a11y cancelada (QA-007, 2026-06-03):** não criar `a11y-home.spec.ts`. Não usar `@axe-core/playwright`. Zero import de `checkA11y`.
>
> ⚠️ **Nota de branch (2026-06-03):** QA-003 foi mergeada diretamente em `develop` (PR #85/#86), não em `integration`. Por isso esta task **parte de `develop`** (que tem QA-001+002+003) — o PR alvo continua sendo `integration/03-folha-pagamento`. Exceção única; a partir de QA-004 todas as features voltam ao fluxo feature→integration.

---

## Pré-condições (git)

- `feature/qa-002-scripts-orquestracao-stack` mergeada em `develop` ✅ (via integration PR #84).
- `feature/qa-003-fixtures-banco-auth-payloads` mergeada em `develop` ✅ (PR #85/#86).
  Confirmar: `git log origin/develop --oneline | grep -E "qa-002|qa-003"`

---

## O prompt (cole tudo numa sessão `--agent frontend`)

```
Task: QA-004 — 2 specs MVP (site-fluxo-feliz, webhook-cenarios).
ATENÇÃO: a11y cancelada — não criar a11y-home.spec.ts, não importar @axe-core/playwright.

Localize e leia o plano:
  Glob("docs/sprints/03-folha-pagamento/plans/QA-004-specs-mvp-3-cenarios.md")

Leia também:
- docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md §4 (QA-004)
- docs/architecture/desenho-testes-automatizados.md §8.1, §8.2, §8.3, §9.2

## ATENÇÃO — BRANCH

Branch a partir de `develop` (QA-003 foi para develop direto; integration não tem as fixtures):
  git fetch
  git checkout -b feature/qa-004-specs-mvp-3-cenarios origin/develop

O PR alvo continua sendo `integration/03-folha-pagamento` (não develop direto).

## A TASK

Criar 3 specs em frontend/e2e/specs/.

### site-fluxo-feliz.spec.ts (1 test)
1. loginE2E(page) via fixture de auth
2. page.goto('/') → verificar home carregou (título ou elemento identificador)
3. Clicar num pedido → verificar navegação para detalhe (/pedidos/:id ou similar)
4. Clicar em "ver comprovante" → verificar modal/preview abre

NÃO adicionar checkA11y — a11y cancelada (QA-007).

### webhook-cenarios.spec.ts (3 tests — estrutura parametrizada)
import { telegramUpdateTextoPuro, telegramUpdateSticker, telegramUpdateFotoLegenda, E2E_MOCK_FILE_ID } from '../fixtures/payloads-telegram';

const cenarios = [
  { nome: 'texto puro', payload: telegramUpdateTextoPuro({ fromUserId: 99, text: 'Oi bot' }) },
  { nome: 'sticker', payload: telegramUpdateSticker({ fromUserId: 99 }) },
  { nome: 'foto com caption', payload: telegramUpdateFotoLegenda({ fromUserId: 99, fileId: E2E_MOCK_FILE_ID, caption: 'comprovante fev' }) },
];

for (const c of cenarios) {
  test(`webhook recebe e processa: ${c.nome}`, async ({ request }) => {
    // 1. Cleanup antes
    await limparDadosE2E();
    // 2. POST webhook com header HMAC correto (usar segredo de .env.e2e)
    const resp = await request.post(`${E2E_BACKEND_URL}/webhook/telegram`, {
      data: c.payload,
      headers: { 'X-Telegram-Bot-Api-Secret-Token': process.env.E2E_WEBHOOK_SECRET }
    });
    expect(resp.status()).toBe(200);
    // 3. Verificar pedido criado no banco
    const rows = await querySql('SELECT * FROM pedidos_pagamento WHERE requisitante_id = 99 ORDER BY id DESC LIMIT 1');
    expect(rows.length).toBe(1);
  });
}

### playwright.config.ts
`reporter` e `outputDir` já foram adicionados pela implementação de QA-001 — **não recriar**.
Verificar se `outputDir: './playwright-report'` (atual) está causando confusão com os artifacts de debug;
se preferir, mover para `'./test-results'` (padrão semântico do Playwright — já está no .gitignore).
Caso contrário, aceitar como está e seguir.

### .gitignore
- Confirmar que `frontend/playwright-report/` está ignorado (QA-001 deveria ter feito — verificar)

## REGRAS DURAS

1. Branch: `feature/qa-004-specs-mvp-3-cenarios` saindo de `origin/develop`.
2. Território: SÓ `frontend/`. Zero `financas_bot_telegram/`. Zero frontend/src/.
3. Os 3 cenários de webhook DEVEM estar na tabela: texto puro, sticker, foto+caption.
4. playwright-report/ NÃO commitado — verificar .gitignore.
5. Zero import de `@axe-core/playwright` — a11y cancelada (QA-007).
6. 1 commit: `feat(QA-004): 2 specs MVP — fluxo feliz, webhook (3 cenarios)`.
7. PR: `feature/qa-004-specs-mvp-3-cenarios → integration/03-folha-pagamento` (NÃO para develop direto).

## VALIDAÇÃO DE ESTABILIDADE (obrigatória)

Rodar npm run e2e:full 3 vezes seguidas em ambiente limpo → 3 verdes consecutivos.
Se qualquer execução falhar por flakiness (não por bug real): identificar e corrigir antes de commitar.

Quando uma spec falhar intencionalmente (alterar assert para forçar erro): abrir playwright-report/index.html — deve mostrar screenshot + trace navegável.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/QA-004-specs-mvp-3-cenarios.md`

testes_novos: 4 (1 fluxo feliz + 3 webhook).
Anotar resultado das 3 execuções consecutivas (timestamp, duração, resultado).

## SE QUEBRAR

Cenário — HMAC header inválido (back rejeita 403):
  Verificar como back valida o header. Pode ser X-Telegram-Bot-Api-Secret-Token ou outro.
  Usar o segredo correto de .env.e2e.

Cenário — flakiness em loginE2E (cookie expira):
  Usar storageState do Playwright para reutilizar sessão entre testes do mesmo spec.

Pare ao final. PR para integration. Reviewer deve rodar manualmente antes de aprovar.
```

---

## ⚠️ Nota especial pro Reviewer desta task

- **Rodar `npm run e2e:full` manualmente** antes de aprovar.
- Verificar que `playwright-report/` **não está no diff** do PR.
- Confirmar que `webhook-cenarios.spec.ts` tem os 3 cenários: texto puro, sticker **e foto+caption**.
- Verificar que o cenário foto+caption usa `E2E_MOCK_FILE_ID` (não file_id hardcoded).
- Verificar que cleanup não afeta dados com `requisitante_id=1`.

---

## Notas pro humano

- **Fase 1 da suíte E2E entregue após o merge desta.** 2 specs, 4 testes (fluxo feliz + 3 cenários de webhook). Registrar na retro da sprint 03.
- **Mock Telegram:** o cenário foto+caption depende do mock server (QA-002) estar rodando. Se o teste falhar com erro de conexão recusada em `:9090`, verificar se `subir-stack.ts` subiu o mock corretamente.
- **Estimativa:** 2-3h.

## Referências

- `docs/sprints/03-folha-pagamento/plans/QA-004-specs-mvp-3-cenarios.md`
- `docs/architecture/desenho-testes-automatizados.md` §8.1..8.3
