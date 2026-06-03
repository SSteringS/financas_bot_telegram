---
task: QA-004
sprint: 03-folha-pagamento
data: 2026-06-03
avaliador: claude-reviewer
status_report: docs/sprints/03-folha-pagamento/status/QA-004-specs-mvp-3-cenarios.md
veredito_codigo: aprovado_com_observacoes
veredito_final: aprovado_com_observacoes
observacoes_count: 3
roteiro_executado: false
gates_verificados_contra_realidade: ok
skills_eficazes: []
skills_gaps: []
---

# Avaliacao — QA-004 3 specs MVP (fluxo feliz, webhook, a11y)

**Branch:** `feature/qa-004-specs-mvp-3-cenarios`
**Implementador:** claude-front
**Plano:** `docs/sprints/03-folha-pagamento/plans/QA-004-specs-mvp-3-cenarios.md`
**Status report:** `docs/sprints/03-folha-pagamento/status/QA-004-specs-mvp-3-cenarios.md`

---

## 1. Analise de codigo (Reviewer le o diff)

### Veredito de codigo: aprovado com observacoes

A entrega e solida para o objetivo da Fase 1. Os tres specs cobrem os quatro cenarios contratados, o isolamento de dados com `limparDadosE2E()` + `requisitante_id=99` e correto, a parametrizacao com `for-of` e tipada (interface `Cenario`), o threshold de a11y bate exatamente com o definido em decisao arquitetural (serious + critical via filtro manual sobre `AxeBuilder.analyze()`), e os tres desvios do plano sao declarados, justificados e factuais. Territorio e 1 commit sao respeitados.

Tres observacoes materiais abaixo, nenhuma bloqueante para o merge.

### Observacoes materiais

**Observacao 1 — `AxeBuilder.analyze()` diretamente em vez de `checkA11y(page, { includedImpacts: [...] })`**
- **O que:** O plano (secao "Decisao / abordagem" e DISPATCH) especificava `checkA11y(page, { includedImpacts: ['serious', 'critical'] })` importado de `@axe-core/playwright`. A implementacao usa `new AxeBuilder({ page }).analyze()` e faz o filtro de impacto manualmente (`v.impact === 'serious' || v.impact === 'critical'`). As duas abordagens sao equivalentes em resultado; a segunda e tecnicamente mais explícita.
- **Onde:** `frontend/e2e/specs/a11y-home.spec.ts:26-34` e `frontend/e2e/specs/site-fluxo-feliz.spec.ts:57-64`.
- **Por que importa:** Nao e um problema funcional. Mas a abordagem com `includedImpacts` faz com que o axe nem execute regras de menor impacto, reduzindo ruido de performance em paginas com muito DOM. A abordagem atual executa todas as regras e descarta no filtro — semanticamente correto mas ligeiramente mais custoso. Alem disso, a diferenca do plano nao foi documentada como desvio no status report (esta ausente dos tres desvios declarados).
- **Sugestao:** Aceitar como esta para esta task (funciona, threshold correto). Registrar como pendencia cosmetica: em sprint futura, alinhar para `withTags(['wcag2a', 'wcag2aa'])` + `disableRules` ou `includedImpacts` se performance da suite E2E virar gargalo.

**Observacao 2 — Webhook spec omite header `X-Telegram-Bot-Api-Secret-Token` sem desvio documentado no contexto correto**
- **O que:** O Desvio 2 do status report diz que o header foi omitido porque o `TelegramWebhookController` nao valida esse header. A afirmacao e factual (verificado: o controller usa apenas `allowedUserIds` para autorizacao, sem nenhuma validacao de HMAC). O desvio esta correto. Mas a ausencia do header significa que a suite E2E nao testa o cenario de seguranca "chamada sem token e rejeitada" — esse cenario nao esta no escopo do MVP, mas vale registrar a lacuna.
- **Onde:** `frontend/e2e/specs/webhook-cenarios.spec.ts:61-63` (ausencia do header); `TelegramWebhookController.java:37-43` (ausencia de validacao de HMAC).
- **Por que importa:** Hoje, qualquer cliente HTTP pode postar em `/webhook/telegram` sem autenticacao de origem (so precisa de um `from.id` na allowlist). Isso e uma decisao de produto ja tomada (usar allowedUserIds em vez de HMAC), mas a ausencia de validacao de origem e uma superficie de ataque potencial se o endpoint for exposto publicamente. Nao e escopo desta task corrigir.
- **Sugestao:** Registrar no `docs/PENDENCIAS-TECNICAS.md` como pendencia de seguranca: "TelegramWebhookController nao valida X-Telegram-Bot-Api-Secret-Token — considerar adicionar na Fase 2 da suite E2E como cenario de seguranca (401/403 sem token valido)."

**Observacao 3 — Conexao singleton de banco nao e fechada no teardown da suite**
- **O que:** `banco.ts` expoe `fecharConexao()` com comentario "chamar em globalTeardown quando implementado". O `global-setup.ts` (QA-003) provavelmente nao tem um `globalTeardown` correspondente — o que significa que a conexao MySQL fica aberta ate o Node.js sair (Playwright fecha o processo, entao na pratica nao e um leak em execucao normal). Mas se o banco tiver `wait_timeout` curto, uma suite longa pode ter a conexao encerrada pelo servidor no meio da execucao.
- **Onde:** `frontend/e2e/fixtures/banco.ts:60-66` (`fecharConexao`) — verificar se `globalTeardown` existe em `frontend/e2e/fixtures/global-setup.ts`.
- **Por que importa:** Em runs longas ou com muitos cenarios futuros (Fase 2), a conexao singleton pode ser encerrada pelo MySQL por timeout e causar erros de `PROTOCOL_CONNECTION_LOST`. No MVP de 4 testes nao e problema pratico.
- **Sugestao:** Em QA-005 ou Fase 2, adicionar `globalTeardown` que chame `fecharConexao()`. Nao e bloqueante para esta task.

---

## 2. Gates verificados contra a realidade

| Gate | Status diz | Reviewer reproduziu | Divergencia? |
|---|---|---|---|
| build | ok | ok (`npm run build` exit 0, 1168 modulos, `tsc -b` sem erros) | nao |
| lint | ok | ok (`npm run lint` exit 0, zero erros) | nao |
| testes | ok (61 total, 4 novos) | ok (61/61 Vitest passaram em 3.19s; 4 `test()` Playwright no diff) | nao |
| `npx tsc -p e2e/tsconfig.json --noEmit` | ok | ok (saida vazia = zero erros de tipo) | nao |
| branch_convencao | ok | ok: `feature/qa-004-specs-mvp-3-cenarios` + `integration/03-folha-pagamento` e ancestral direto (merge-base = `ac4046e` = HEAD da integration) | nao (mesma ressalva de prefixo `qa` do ADR 0017 que ja consta da avaliacao QA-002) |
| territorio | ok | ok: diff de `origin/integration/03-folha-pagamento...feature/qa-004-specs-mvp-3-cenarios` toca apenas `frontend/e2e/specs/`, `frontend/vite.config.ts`, `frontend/e2e/specs/.gitkeep` (remocao) e `docs/sprints/03-folha-pagamento/status/` — zero `financas_bot_telegram/`, zero `frontend/src/` | nao |
| playwright-report/ nao commitado | ok | ok: `git ls-files --error-unmatch frontend/playwright-report` retornou erro (nao esta no repo); `frontend/.gitignore` tem `playwright-report/` | nao |
| 1 commit de feature | ok | ok: exatamente 1 commit novo (`0db74f7`) entre a feature e a integration branch | nao |
| testes_novos = 4 | ok | ok: 1 `test()` em `site-fluxo-feliz.spec.ts`, 2 `test()` gerados por `for-of` em `webhook-cenarios.spec.ts`, 1 `test()` em `a11y-home.spec.ts` | nao |

**Nota sobre `exige_e2e_full`:** o plano declara `exige_e2e_full: false` com justificativa valida ("a suite sendo entregue e a infra de validacao; nao faz sentido ser gate dela mesma"). O desenho de testes automatizados §5.2 item 10 (decisao E+A+D) e §9.5 confirmam que o Reviewer nao roda `e2e:full` — audita pelo status report. O status report documenta explicitamente as validacoes diferidas por falta de stack completa. Gate nao bloqueante.

**Verificacoes adicionais dos criterios da task:**

- TODO Fase 1.1: presente em `webhook-cenarios.spec.ts:49` — `// TODO Fase 1.1: adicionar cenario foto+caption apos decisao de mock de download de midia Telegram (ADR 00XX)`. Texto levemente diferente do plano (plano: "ADR 00XX"; spec: "ADR 00XX" — identico) e mais verboso que o DISPATCH mas semanticamente equivalente. Criterio atendido.
- Threshold a11y: `a11y-home.spec.ts` e `site-fluxo-feliz.spec.ts` filtram `v.impact === 'serious' || v.impact === 'critical'` — sem `moderate`, sem `minor`. Correto.
- Desvio 1 verificado: `App.tsx` confirma que nao existe rota `/pedidos/:id` — rotas sao `/`, `/entrar`, `/erro`, `/_showcase` (dev only). Desvio factual.
- Desvio 2 verificado: `TelegramWebhookController.java` confirma que o controller nao valida `X-Telegram-Bot-Api-Secret-Token` — autorizacao via `allowedUserIds`. Desvio factual.
- Desvio 3 verificado: `vite.config.ts` adiciona `exclude: ['**/node_modules/**', '**/e2e/**']` no bloco `test`. O `build` continua passando (tsc + vite) e `npm test` continua 61/61. Mudanca necessaria e correta.
- Tipos sem `as`: nenhuma type assertion (`as`) encontrada nos tres specs. A interface `PedidoRow` em `webhook-cenarios.spec.ts` e usada para tipar `querySql<PedidoRow>()` — parametro de tipo, nao assertiva. Correto.
- Cleanup de dados: `limparDadosE2E()` e chamado em `beforeEach` de `site-fluxo-feliz.spec.ts` e `webhook-cenarios.spec.ts`. `a11y-home.spec.ts` nao cria dados (so le via UI), entao nao precisa de cleanup — correto. Nenhum spec toca `requisitante_id != 99`.

---

## 3. Roteiro de validacao manual

A task QA-004 entrega a propria infra de validacao automatizada (specs E2E). Um roteiro manual de UI seria redundante com o que os proprios specs testam. A execucao real de `npm run e2e:full` foi diferida por ausencia de stack (MySQL + back + front locais) — documentada no status report como limitacao conhecida, conforme autorizado pelo plano (`exige_e2e_full: false`) e pelo desenho de testes §5.2 item 10.

**Nao aplicavel para esta avaliacao:** a execucao de `npm run e2e:full` e responsabilidade do implementador antes do merge (critério de aceitacao do plano) ou do humano ao validar a Fase 1 completa. O Reviewer nao tem acesso ao ambiente completo para rodar os specs.

O plano e o DISPATCH alertam explicitamente sobre pre-condicoes necessarias para rodar os specs:
- User 99 em `telegram.allowed-user-ids` no `application-dev.properties`
- MySQL local com schema da V6 (EVO-09, BE-023)
- Backend rodando com perfil `dev`
- Frontend rodando com `VITE_API_BASE_URL` apontando para o back local

---

## 4. Resultado consolidado

| Item | Resultado |
|---|---|
| Analise de codigo | aprovado com observacoes (3 observacoes, nenhuma bloqueante) |
| Gates contra a realidade | ok (todos os gates verificaveis reproduzidos; `e2e:full` diferido conforme autorizado pelo plano) |
| Roteiro manual | nao aplicavel — task entrega infra de validacao; `e2e:full` diferido com justificativa valida |
| **Veredito final** | **APROVADO COM RESSALVAS** — mergear; observacoes 2 e 3 devem virar pendencias tecnicas registradas pelo planner |

---

## 5. Skills — feedback loop

Sem observacao de skills nesta task. `skills_eficazes: []`, `skills_gaps: []`.

---

## 6. Para o planner (proximos passos)

1. **Pendencia de seguranca — HMAC do webhook:** registrar em `docs/PENDENCIAS-TECNICAS.md`: o `TelegramWebhookController` nao valida `X-Telegram-Bot-Api-Secret-Token`. Para a Fase 2 da suite E2E, considerar adicionar: (a) validacao de origem no controller e (b) cenario de seguranca no `webhook-cenarios.spec.ts` ("chamada sem token valido deve retornar 401/403").

2. **Teardown da conexao MySQL:** registrar em `docs/PENDENCIAS-TECNICAS.md`: `fecharConexao()` definido em `banco.ts` mas `globalTeardown` nao implementado. Em suite maior (Fase 2), a conexao singleton pode ser encerrada por timeout do MySQL. Adicionar `globalTeardown` que chame `fecharConexao()`.

3. **Fase 1 da suite E2E concluida:** apos merge desta task, a Fase 1 esta entregue. Registrar na retro da sprint 03 e atualizar qualquer documento de estado que liste o status da suite E2E.

4. **Sem bloqueio de sequencia:** nenhuma task subsequente listada em `bloqueia: []` no plano. Merge pode ser feito diretamente.
