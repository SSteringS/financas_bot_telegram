---
task: QA-008
titulo: "Estabilização da suíte E2E — workers serializados + update_id em int32"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-04
branch_alvo: feature/qa-008-estabilizacao-suite-e2e
integration_branch: integration/03-folha-pagamento
prioridade: alta
esforco: baixo
territorio: front
estado: pronto-pra-execucao
depende_de: [QA-004, QA-007]
bloqueia: []
skills_dispatched: []
exige_e2e_full: true
fluxos_qa: []
---

# QA-008 — Estabilização da suíte E2E (workers serializados + update_id em int32)

> **Status: pronto-pra-execucao.** Bug puramente de tooling de teste (não toca código de produto). Implementador pode pegar direto. Planner valida posicionamento de sprint (`03-folha-pagamento` é placeholder — ajustar se for mover pra sprint 04 ou follow-up).

---

## Intake

- **Origem:** descoberto durante a primeira execução automatizada real da suíte E2E em 2026-06-04, na sessão do qa-test-specialist com o humano. Dos 3 testes do MVP, **2 falharam** por dois bugs independentes na suíte. Saída do `npx playwright test`:
  - `webhook: texto puro retorna 200 sem criar pedido` — FAIL: `Expected length: 0, Received length: 2` (viu pedidos `E2E Boleto Energia` + `E2E PIX Maria` que o spec do fluxo feliz semeou em paralelo).
  - `fluxo feliz: login → home → comprovante` — FAIL: `getByText('E2E Boleto Energia') not found` (worker do webhook deletou os pedidos do fluxo feliz no `beforeEach`).
  - Bonus: log do backend registrou duas `JsonMappingException: Numeric value (1780541445233) out of range of int` durante a execução — não derrubou o teste (webhook respondeu 200 mesmo assim), mas é sintoma do mesmo bug.
- **Por quê agora:** sem isso a suíte E2E é instável e o critério "3x verde consecutivas" do QA-004 nunca é atingido. Bloqueia a confiança no gate E2E como ferramenta de proteção pré-merge.
- **Esforço:** baixo (~30-45 min). Duas mudanças mecânicas em dois arquivos. Sem decisão pendente.
- **Riscos resumidos:** zero risco funcional. Mudança só em tooling de teste. `workers: 1` deixa a suíte ~2x mais lenta (de ~6s pra ~12s com 3 testes), perda irrelevante.

---

## Contexto

### Bug A — Race condition entre workers paralelos

Os dois specs da suíte MVP operam no **mesmo** `requisitante_id=99`:

- `frontend/e2e/specs/site-fluxo-feliz.spec.ts` — `beforeEach` faz `limparDadosE2E()` + `semearPedidos([...])`.
- `frontend/e2e/specs/webhook-cenarios.spec.ts` — `beforeEach` faz `limparDadosE2E()`.

`playwright.config.ts` atual não declara `workers` — Playwright usa o default (`os.cpus()/2` ou similar; na máquina do humano deu **2 workers**). Quando os dois specs rodam em paralelo no mesmo banco, o `limparDadosE2E()` de um worker apaga os pedidos seedados pelo outro. Sequência observada na execução real:

```
Worker A (site-fluxo-feliz)        Worker B (webhook texto puro)
beforeEach: limpar                
beforeEach: semear 2 pedidos       
loginE2E, goto('/')                beforeEach: limpar ← deleta pedidos do A
expect 'E2E Boleto Energia'        POST webhook
  → não acha → FAIL                expect pedidos.length === 0
                                    → vê 2 pedidos (do A, antes do delete) → FAIL
```

A decisão arquitetural original da suíte (ver `docs/architecture/desenho-testes-automatizados.md`) foi usar **1 único requisitante de teste** pra simplicidade. Coerente com essa decisão, a suíte precisa rodar serial.

### Bug B — `update_id` estourando int32 em `payloads-telegram.ts`

Em `frontend/e2e/fixtures/payloads-telegram.ts:18-21`:

```typescript
let _updateCounter = Date.now();   // ~1.78 trilhão em 2026
function nextUpdateId(): number {
  return _updateCounter++;
}
```

`Date.now()` retorna milissegundos epoch (~1.78 × 10¹² hoje). Max int32 = 2.147 × 10⁹. **Estoura int32 sempre** (estoura desde ~25/01/1970).

O `Update` DTO do backend vem da lib `org.telegram.telegrambots.meta.api.objects.Update` (rubenlagus/TelegramBots), que declara `update_id` como `Integer` — coerente com a [Telegram Bot API spec](https://core.telegram.org/bots/api#update) que define `update_id` como "32-bit integer".

**Conclusão:** não é bug do backend nem da lib. Em produção, o Telegram **garante** que `update_id` cabe em int32. O bug é do fixture do teste, que gera valor irrealista.

Por que o teste retornou 200 mesmo com o `JsonMappingException`? Olhando o `TelegramWebhookController`, ele declara `@RequestBody Update update` — quando Jackson falha em deserializar, o Spring lança `HttpMessageNotReadableException` antes do handler do controller rodar. Provavelmente algum filtro/exception handler global converte pra 200 (consistente com ADR 0003: webhook nunca 5xx). Mas isso é especulação — não importa pra esta task porque a correção é no teste, não no backend.

---

## Decisão / abordagem

Duas mudanças mecânicas, mesma branch, mesmo PR.

### Bug A — `workers: 1`

Adicionar `workers: 1` ao `defineConfig` de `frontend/playwright.config.ts`. Comentário curto explicando por quê (1 requisitante compartilhado entre specs).

### Bug B — gerador int32-safe

Trocar o gerador de `update_id` em `frontend/e2e/fixtures/payloads-telegram.ts` por:

```typescript
// Base random em int32 evita colisão com runs anteriores que possam ter persistido
// updates em mensagem_processada (a tabela não é limpa pelo cleanup do E2E).
// Cabe em int32 com folga (Telegram API garante int32 — ver docs).
let _updateCounter = Math.floor(Math.random() * 1_000_000_000);
function nextUpdateId(): number {
  return _updateCounter++;
}
```

Justificativa do número:
- Base random até 10⁹ deixa pelo menos 10⁹ valores até estourar int32 — folga gigante pra qualquer execução real (suíte tem 3 testes, mesmo escalando pra centenas continua seguro).
- Random reduz colisão entre runs com `mensagem_processada` (o `banco.ts` documenta que essa tabela não é limpa — ver linha 91).
- Counter incremental dentro do run preserva unicidade entre cenários (o sticker e o texto puro precisam de `update_id` distinto).

---

## Escopo / arquivos

### Modificar

- `frontend/playwright.config.ts` — adicionar `workers: 1` no `defineConfig` com comentário 1-linha explicativo.
- `frontend/e2e/fixtures/payloads-telegram.ts` — substituir `Date.now()` por gerador int32-safe (3 linhas afetadas: declaração de `_updateCounter`, comentário, possivelmente nome da função se for ajustado).

### Não tocar

- `frontend/src/` — zero código de produto.
- `financas_bot_telegram/` — zero código de back. O bug B foi inicialmente diagnosticado como bug do back; revisão mostrou que é tooling de teste.
- Os 3 arquivos de spec (`site-fluxo-feliz`, `webhook-cenarios`) — não precisam mudar. As mudanças no fixture/config são transparentes pra eles.
- `frontend/e2e/fixtures/banco.ts`, `auth.ts`, `global-setup.ts` — não tocam neste fix.

---

## Testes

A própria suíte é o teste. Validação:

1. `npm run e2e:full` verde com **3 testes passando** (1 fluxo feliz + 2 webhook).
2. `npm run e2e:full` verde **3 vezes consecutivas** em ambiente limpo — critério de não-flakiness original do QA-004 que agora se torna alcançável.
3. Tempo da suíte com `workers: 1`: esperado ~10-15s (vs ~6s com 2 workers). Aceitável.
4. Conferir log do backend durante execução — não pode aparecer `JsonMappingException: Numeric value (...) out of range of int`. Esse log limpo confirma o fix do bug B.

`testes_novos: 0` — não adiciona testes, estabiliza os existentes.

---

## Critérios de aceitação

- [ ] `frontend/playwright.config.ts` contém `workers: 1` no `defineConfig`, com comentário explicativo.
- [ ] `frontend/e2e/fixtures/payloads-telegram.ts` não usa `Date.now()` como base do gerador de `update_id`; usa `Math.floor(Math.random() * 1_000_000_000)` ou equivalente int32-safe documentado.
- [ ] `npm run e2e:full` verde **3 vezes consecutivas** em ambiente limpo (sem flakiness).
- [ ] Cada execução completa com 3 testes passando (`1 fluxo feliz + 2 webhook`).
- [ ] Log do backend durante a execução **não** contém `JsonMappingException` relacionado a `Numeric value (...) out of range of int`.
- [ ] `npm test` (Vitest) continua verde — nenhuma regressão em testes de unit.
- [ ] `npm run build` (TypeScript + Vite) sem erros.
- [ ] Branch: `feature/qa-008-estabilizacao-suite-e2e` saindo de `origin/integration/03-folha-pagamento` (após QA-004 e QA-007 mergeados em integration).
- [ ] Status report `docs/sprints/03-folha-pagamento/status/QA-008-estabilizacao-suite-e2e-workers-e-update-id.md` com frontmatter válido. Mencionar no corpo que a execução real validou `3x consecutivas verde`.

---

## Fora de escopo (explicitamente)

- **Investigar como o backend transforma `JsonMappingException` em HTTP 200.** Coerente com ADR 0003 (webhook nunca 5xx), provavelmente já tratado por handler global. Não é bug do back.
- **Trocar tipo `Update.update_id` na lib oficial do TelegramBots.** Não tocamos código de lib externa, e a spec do Telegram garante int32.
- **Refatorar a suíte pra usar requisitantes distintos por spec (id=99, 98, 97...).** Alternativa de design que permitiria workers paralelos no futuro; fora desta task. Se a suíte crescer e workers paralelos virarem necessário, abre QA-NNN dedicada.
- **Documentar o protocolo do Telegram (`update_id` é int32) em algum lugar.** Comentário curto no `payloads-telegram.ts` já é suficiente — não vira doc separada.
- **Adicionar limpeza de `mensagem_processada` no cleanup do E2E.** O `banco.ts:91` documenta deliberadamente que essa tabela não é limpa (usa update_id único por run pra evitar colisão). Com o gerador random base, essa garantia se mantém.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| `workers: 1` deixar a suíte lenta demais quando crescer pra 50+ testes | Baixa (suíte é pequena) | Médio (afeta DX) | Quando crescer, refatorar pra requisitantes distintos. Hoje a suíte é 3 testes, é prematuro |
| Random na base do counter colidir com run anterior em `mensagem_processada` | Muito baixa (1 em 10⁹) | Baixo (1 teste falha por uniqueness, fácil de identificar) | Aceito. Se acontecer 1 vez em 1000 runs, irrelevante |
| Implementador esquecer o comentário explicativo no `workers: 1` | Média | Baixo | Critério de aceitação exige comentário; reviewer audita |
| Mexer no `payloads-telegram.ts` introduzir regressão na geração de `message_id` (que também está no mesmo arquivo) | Baixa | Médio | Critério: cada execução de `e2e:full` valida que os 3 testes passam, incluindo o sticker que depende do `message_id` |

---

## Coordenação

- **Pode rodar em paralelo com:** qualquer task que não toque `frontend/e2e/`, `frontend/playwright.config.ts` ou `frontend/e2e/fixtures/payloads-telegram.ts`.
- **Depende sequencialmente de:** QA-004 (entregou os specs que esta task estabiliza) + QA-007 (removeu a11y, condição que permite que a suíte de 3 testes seja a versão final). Ambos já mergeados.
- **Bloqueia:** confiança operacional no gate E2E. Sem isso, não dá pra adicionar `e2e:full` ao PRE-MERGE-CHECKLIST (que é o objetivo de QA-006).
- **Atenção pro Reviewer:** verificar que (a) `workers: 1` ficou com comentário; (b) o gerador novo do `update_id` realmente cabe em int32 com folga; (c) rodar `e2e:full` 3 vezes localmente antes de aprovar — é o critério duro; (d) não houve mudança acidental em código de produto (`frontend/src/`).
- **Após merge:** atualizar contagem na memória do qa-test-specialist (`project_estado_testes_2026-06-01.md` → atualizar pra refletir que a suíte E2E está estável). Se QA-006 (pre-merge gate) estava bloqueado por instabilidade, destrava.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, revisão do Reviewer. PR `feature → <branch-base-definida-pelo-planner-ou-direto-develop-se-for-fix-puro>`.

Critério duro adicional: a execução de validação (3x `e2e:full` consecutivas verde) **deve ser registrada no status report** como evidência. Sem essa evidência, task não fecha.

`fluxos_qa: []` — task de tooling pura, sem fluxo de produto a validar via QA externo.

---

## Referências

- `docs/sprints/03-folha-pagamento/plans/QA-004-specs-mvp-3-cenarios.md` — entregou os specs que esta task estabiliza.
- `docs/sprints/03-folha-pagamento/plans/QA-007-remover-axe-core-a11y-do-gate.md` — última task que mexeu na suíte (removeu `@axe-core/playwright`).
- `docs/architecture/desenho-testes-automatizados.md` — decisão original de 1 requisitante compartilhado; justifica `workers: 1`.
- `docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md` — convenção do prefixo QA-NNN.
- `frontend/playwright.config.ts` — arquivo modificado (bug A).
- `frontend/e2e/fixtures/payloads-telegram.ts` — arquivo modificado (bug B).
- `frontend/e2e/fixtures/banco.ts:91` — comentário que documenta a invariante "mensagem_processada não é limpa; usa update_id único por run".
- `financas_bot_telegram/src/main/java/.../adapters/in/telegram/controller/TelegramWebhookController.java:17` — confirma que `Update` vem de `org.telegram.telegrambots.meta.api.objects` (lib externa).
- [Telegram Bot API — Update object](https://core.telegram.org/bots/api#update) — spec oficial que define `update_id` como Integer (32-bit). Citado no `payloads-telegram.ts` como comentário inline pelo implementador.
- Evidência da execução real que descobriu os dois bugs: conversa do qa-test-specialist com o humano em 2026-06-04 (sessão de demo da suíte automatizada).
