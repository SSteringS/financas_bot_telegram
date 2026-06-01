---
task: QA-004
titulo: "3 specs MVP — fluxo feliz, webhook (3 cenarios), a11y"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-01
branch_alvo: feature/qa-004-specs-mvp-3-cenarios
integration_branch: integration/03-folha-pagamento
prioridade: alta
esforco: medio
territorio: front
estado: pronto-pra-execucao
depende_de: [QA-002, QA-003]
bloqueia: []
skills_dispatched: []
exige_e2e_full: false
lote: B
---

# QA-004 — 3 specs MVP (site-fluxo-feliz, webhook-cenarios, a11y-home)

## Intake

- **Origem:** spec QA Fase 1 §4 (QA-004). **Lote B — depende de QA-002 e QA-003.**
- **Por quê agora:** é o entregável final da Fase 1. Substitui ~80% do ROTEIRO-INTEGRACAO-FRONT-BACK manual.
- **Esforço:** médio (~3-4h) — 3 specs, 4 `test()` no total, integração com fixtures, asserção via axe-core.
- **Riscos resumidos:** 3 cenários no webhook (texto puro + sticker + foto+caption). Mock Telegram (QA-002) resolve o download de arquivo — `TELEGRAM_API_URL` apontado para `:9090` via `.env.e2e`. S3 usa bucket dev real (credenciais `~/.aws`).

---

## Contexto

Após QA-001 (config) + QA-002 (scripts de stack) + QA-003 (fixtures), a infra está pronta. Esta task entrega as 3 specs que constituem o MVP da suíte E2E.

Contagem de `test()` nesta task (critério "testes E2E novos"):
- `site-fluxo-feliz.spec.ts` → 1 test
- `webhook-cenarios.spec.ts` → 3 tests (parametrizado: texto puro + sticker + foto+caption)
- `a11y-home.spec.ts` → 1 test
**Total: 5 testes novos.**

---

## Decisão / abordagem

Três specs em `frontend/e2e/specs/`:

**`site-fluxo-feliz.spec.ts`** — fluxo autenticado completo:
1. `loginE2E(page)` — injeta session via fixture.
2. `page.goto('/')` — verifica landing na home.
3. Clicar num pedido → verificar navegação para detalhe.
4. Clicar em "ver comprovante" → verificar que modal/preview abre.
5. Bonus inline a11y: `checkA11y(page, { includedImpacts: ['serious', 'critical'] })`.

**`webhook-cenarios.spec.ts`** — parametrizado com 3 cenários:
```typescript
const cenarios = [
  { nome: 'texto puro', payload: telegramUpdateTextoPuro({ fromUserId: 99, text: 'Oi' }) },
  { nome: 'sticker', payload: telegramUpdateSticker({ fromUserId: 99 }) },
  { nome: 'foto com caption', payload: telegramUpdateFotoLegenda({ fromUserId: 99, fileId: E2E_MOCK_FILE_ID, caption: 'comprovante fev' }) },
];
for (const c of cenarios) {
  test(`webhook recebe e processa: ${c.nome}`, async ({ request }) => { ... });
}
```
Cada cenário: POST para `E2E_BACKEND_URL/webhook/telegram` com payload + header HMAC simulado → verificar 200 + pedido criado no banco (via `querySql`). O cenário foto+caption depende do mock Telegram (QA-002) rodando em `:9090` — `TELEGRAM_API_URL=http://localhost:9090` no `.env.e2e` faz o back baixar do mock em vez do Telegram real.

**`a11y-home.spec.ts`** — standalone:
1. `loginE2E(page)`, navega para home.
2. `checkA11y(page, { includedImpacts: ['serious', 'critical'] })` — threshold: zero violações serious+critical.

Atualizar `playwright.config.ts` (de QA-001): adicionar `reporter: [['html'], ['list']]` e `outputDir: './playwright-report'`.

---

## Escopo / arquivos

### Criar
- `frontend/e2e/specs/site-fluxo-feliz.spec.ts`
- `frontend/e2e/specs/webhook-cenarios.spec.ts`
- `frontend/e2e/specs/a11y-home.spec.ts`

### Modificar
- `frontend/playwright.config.ts` — adicionar reporter + outputDir.

### Remover
- `frontend/e2e/specs/.gitkeep` (placeholder de QA-001)

### Não tocar
- `frontend/src/` — zero código de produção.
- `financas_bot_telegram/` — zero mudanças no back.

---

## Testes

**São os testes.** Esta task entrega 4 `test()` E2E.

Critério de estabilidade: `npm run e2e:full` verde **3 vezes consecutivas** em ambiente limpo (sem flakiness).

`testes_novos: 5` (1 fluxo feliz + 3 webhook + 1 a11y).

---

## Critérios de aceitação

- [ ] `npm run e2e:full` verde com 3 specs ativas (5 testes no total).
- [ ] `npm run e2e:full` verde 3 vezes seguidas em ambiente limpo — sem flakiness.
- [ ] `webhook-cenarios.spec.ts` tem os 3 cenários na tabela: texto puro, sticker e foto+caption.
- [ ] Cenário foto+caption usa `telegramUpdateFotoLegenda({ fromUserId: 99, fileId: E2E_MOCK_FILE_ID, caption: 'comprovante fev' })`.
- [ ] `a11y-home.spec.ts` usa threshold `serious+critical` (não `moderate` ou `minor`).
- [ ] Após spec com falha intencional: `playwright-report/index.html` mostra screenshot + trace navegável.
- [ ] `playwright-report/` **não** commitado (`.gitignore`).
- [ ] Cleanup (`limparDadosE2E`) roda antes/depois dos testes que criam dados — confirmar que `requisitante_id=1` não é afetado.
- [ ] `npm test` (Vitest) continua verde.
- [ ] Branch: `feature/qa-004-specs-mvp-3-cenarios` saindo de `integration/03-folha-pagamento`.
- [ ] Status report com frontmatter válido e `testes_novos: 4`.

---

## Fora de escopo

- Filtros, paginação, regressões de bugs FE-12 → Fase 2.
- Schemathesis (contract test) → Fase 2.
- Relatório HTML commitado — `.gitignore`.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| HMAC do webhook não bater (header inválido) | Média | Alto | Verificar como back valida HMAC; usar segredo do `.env.e2e` para assinar payload no teste |
| Flakiness em `loginE2E` (cookie expira entre steps) | Baixa | Médio | `storageState` do Playwright reutiliza sessão; configurar `globalSetup` corretamente |
| `checkA11y` retorna violações do MSW (service worker?) | Baixa | Baixo | Playwright não carrega SW do MSW em E2E (config separada); se aparecer, adicionar seletor de exclusão |

---

## Coordenação

- **Pode rodar em paralelo com:** BE-029 (testes de back — territorios disjuntos).
- **Depende sequencialmente de:** QA-002 (scripts de stack) + QA-003 (fixtures).
- **Bloqueia:** nada — entregável final do MVP E2E.
- **⚠️ Atenção pro Reviewer:** rodar `npm run e2e:full` manualmente antes de aprovar. Verificar que `playwright-report/` NÃO está commitado. Confirmar comentário TODO no webhook spec. Verificar que cleanup não afeta dados do Pedro.
- **Após merge:** Fase 1 da suíte E2E entregue. Registrar na retro da sprint 03.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR `feature → integration/03-folha-pagamento`.

`exige_e2e_full: false` — a suíte sendo entregue é a infra de validação; não faz sentido ser gate dela mesma.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md` §4 (QA-004)
- `docs/architecture/desenho-testes-automatizados.md` §8.1 (fluxo feliz), §8.2 (webhook), §8.3 (a11y), §9.2 (tabela e2e_full)
- `docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md`
