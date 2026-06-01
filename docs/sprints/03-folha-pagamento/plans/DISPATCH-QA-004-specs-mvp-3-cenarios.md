# DISPATCH — QA-004-specs-mvp-3-cenarios (single-task)

> **Lote B — despachar somente após QA-002 E QA-003 mergeadas na integration branch.**
> É o entregável final da Fase 1. **Inclui os 3 cenários de webhook: texto puro + sticker + foto+caption.**
> O mock Telegram (QA-002) resolve o download de arquivo via `TELEGRAM_API_URL=http://localhost:9090`.

---

## Pré-condições (git)

- `feature/qa-002-scripts-orquestracao-stack` mergeada em `integration/03-folha-pagamento`.
- `feature/qa-003-fixtures-banco-auth-payloads` mergeada em `integration/03-folha-pagamento`.
  Confirmar: `git log origin/integration/03-folha-pagamento --oneline | grep -E "qa-002|qa-003"`

---

## O prompt (cole tudo numa sessão `--agent frontend`)

```
Task: QA-004 — 3 specs MVP (site-fluxo-feliz, webhook-cenarios, a11y-home).

Localize e leia o plano:
  Glob("docs/sprints/03-folha-pagamento/plans/QA-004-specs-mvp-3-cenarios.md")

Leia também:
- docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md §4 (QA-004)
- docs/architecture/desenho-testes-automatizados.md §8.1, §8.2, §8.3, §9.2

## ATENÇÃO — BRANCH

Branch a partir de `integration/03-folha-pagamento` (que já tem QA-001, QA-002, QA-003):
  git fetch
  git checkout -b feature/qa-004-specs-mvp-3-cenarios origin/integration/03-folha-pagamento

## A TASK

Criar 3 specs em frontend/e2e/specs/.

### site-fluxo-feliz.spec.ts (1 test)
1. loginE2E(page) via fixture de auth
2. page.goto('/') → verificar home carregou (título ou elemento identificador)
3. Clicar num pedido → verificar navegação para detalhe (/pedidos/:id ou similar)
4. Clicar em "ver comprovante" → verificar modal/preview abre
5. checkA11y inline: import { checkA11y } from '@axe-core/playwright'; checkA11y(page, { includedImpacts: ['serious', 'critical'] })

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

### a11y-home.spec.ts (1 test)
1. loginE2E(page)
2. page.goto('/')
3. checkA11y(page, { includedImpacts: ['serious', 'critical'] })
4. Nenhuma violação serious+critical → pass

### playwright.config.ts (atualizar o de QA-001)
Adicionar:
- reporter: [['html'], ['list']]
- outputDir: './playwright-report'

### .gitignore
- Confirmar que `frontend/playwright-report/` está ignorado (QA-001 deveria ter feito — verificar)

## REGRAS DURAS

1. Branch: `feature/qa-004-specs-mvp-3-cenarios` saindo de `origin/integration/03-folha-pagamento`.
2. Território: SÓ `frontend/`. Zero `financas_bot_telegram/`. Zero frontend/src/.
3. Os 3 cenários de webhook DEVEM estar na tabela: texto puro, sticker, foto+caption.
4. playwright-report/ NÃO commitado — verificar .gitignore.
5. 1 commit: `feat(QA-004): 3 specs MVP — fluxo feliz, webhook (3 cenarios), a11y`.
6. PR: `feature/qa-004-specs-mvp-3-cenarios → integration/03-folha-pagamento`.

## VALIDAÇÃO DE ESTABILIDADE (obrigatória)

Rodar npm run e2e:full 3 vezes seguidas em ambiente limpo → 3 verdes consecutivos.
Se qualquer execução falhar por flakiness (não por bug real): identificar e corrigir antes de commitar.

Quando uma spec falhar intencionalmente (alterar assert para forçar erro): abrir playwright-report/index.html — deve mostrar screenshot + trace navegável.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/QA-004-specs-mvp-3-cenarios.md`

testes_novos: 5 (1 fluxo feliz + 3 webhook + 1 a11y).
Anotar resultado das 3 execuções consecutivas (timestamp, duração, resultado).

## SE QUEBRAR

Cenário — HMAC header inválido (back rejeita 403):
  Verificar como back valida o header. Pode ser X-Telegram-Bot-Api-Secret-Token ou outro.
  Usar o segredo correto de .env.e2e.

Cenário — checkA11y encontra violações no DOM do app:
  Listar as violações no status report. Se for bug real de a11y: criar pendência pro planner.
  NÃO silenciar a violação ajustando o threshold — só ajustar se for falso positivo comprovado.

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

- **Fase 1 da suíte E2E entregue após o merge desta.** Inclui os 3 cenários de webhook. Registrar na retro da sprint 03.
- **Mock Telegram:** o cenário foto+caption depende do mock server (QA-002) estar rodando. Se o teste falhar com erro de conexão recusada em `:9090`, verificar se `subir-stack.ts` subiu o mock corretamente.
- **Estimativa:** 2-3h.

## Referências

- `docs/sprints/03-folha-pagamento/plans/QA-004-specs-mvp-3-cenarios.md`
- `docs/architecture/desenho-testes-automatizados.md` §8.1..8.3
