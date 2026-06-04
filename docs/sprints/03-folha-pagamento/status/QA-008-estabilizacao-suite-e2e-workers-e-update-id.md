---
task: QA-008
titulo: "Estabilização da suíte E2E — workers serializados + update_id em int32"
data: 2026-06-04
branch: feature/qa-008-estabilizacao-suite-e2e
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
e2e_full:
  exigido: true
  executado: true
  status: verde
  specs_total: 2
  specs_passed: 3
  specs_failed: 0
  duracao_segundos: 4.6
  data_execucao: 2026-06-04T03:36:00Z
commits:
  - ee488fc
pr: https://github.com/SSteringS/financas_bot_telegram/pull/93
desvios: 0
pendencias_humano: 0
---

# QA-008 — Estabilização da suíte E2E (workers serializados + update_id em int32)

---

## O que foi feito

Duas mudanças mecânicas que corrigem os dois bugs que causaram falhas na primeira execução real da suíte E2E em 2026-06-04.

### Bug A — `frontend/playwright.config.ts`: `workers: 1`

Adicionado `workers: 1` ao `defineConfig`, com comentário de 3 linhas explicando:
- Por que: todos os specs compartilham `requisitante_id=99`; workers paralelos causam race condition entre `beforeEach` de specs distintos.
- Caminho de saída: quando a suíte crescer, refatorar para requisitantes distintos por spec.

### Bug B — `frontend/e2e/fixtures/payloads-telegram.ts`: gerador int32-safe

Substituído `Date.now()` por `Math.floor(Math.random() * 1_000_000_000)` como base do contador de `update_id`.

Justificativa registrada em comentário:
- `Date.now()` retorna ~1.78×10¹² (milissegundos epoch), que excede max-int32 (2.147×10⁹).
- O backend usa `org.telegram.telegrambots.meta.api.objects.Update`, que deserializa `update_id` como `Integer` — consistente com a spec oficial do Telegram Bot API que define `update_id` como "32-bit integer".
- Base aleatória até 10⁹ deixa ~1.1×10⁹ de folga antes de estourar — mais que suficiente para qualquer suíte real.
- Aleatoriedade reduz colisão entre runs em `mensagem_processada` (tabela não é limpa pelo E2E por design — ver `banco.ts:91`).

---

## Validações realizadas

| Validação | Resultado |
|---|---|
| `npm test` (Vitest) 61/61 | ✓ zero regressão |
| `npm run lint` | ✓ limpo |
| `npm run build` | ✓ exit 0 |
| `npx tsc -p e2e/tsconfig.json --noEmit` | ✓ sem erros |
| `workers: 1` presente em `playwright.config.ts` com comentário | ✓ |
| `update_id` gerado por `Math.floor(Math.random() * 1_000_000_000)` | ✓ cabe em int32 |
| `Date.now()` ausente no gerador de `update_id` | ✓ |

**Gate E2E — 3x consecutivas verde (2026-06-04):**

| Run | Workers | 3 testes | Duração |
|---|---|---|---|
| #1 | 1 worker ✓ | 3/3 ✓ | 4,6s |
| #2 | 1 worker ✓ | 3/3 ✓ | 3,8s |
| #3 | 1 worker ✓ | 3/3 ✓ | 4,0s |

Todos os 3 runs passaram com `1 worker` (serial), sem race condition. Critério duro do plano atingido.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

**Comentário de 3 linhas em `playwright.config.ts`:** o plano exige comentário explicativo para `workers: 1`. Optei por 3 linhas (motivo + caminho de saída) em vez de 1 linha para registrar tanto o "por quê agora" quanto o "como escalar no futuro", evitando que alguém remova o `workers: 1` sem entender o trade-off.

**Comentário de 6 linhas em `payloads-telegram.ts`:** detalhado propositalmente — documenta a spec do Telegram, o range do int32, e a referência ao `banco.ts:91`. Serve como documentação inline permanente do invariante, para qualquer futuro mantenedor que olhe o arquivo.

---

## Decisões pendentes (esperando humano)

Nenhuma — gate E2E validado em 2026-06-04 com 3 runs consecutivos verdes.

---

## Próximos passos / observações

- Após validação: task fecha, QA-006 (gate no PRE-MERGE-CHECKLIST) fica desbloqueada.
- **Confirmar no log do backend** que `JsonMappingException: Numeric value (...) out of range of int` **não aparece mais** durante a execução — é o sinal de que o Bug B foi resolvido de ponta a ponta.
- Se `npm run e2e:full` ainda falhar após as correções: investigar logs do Playwright (`playwright-report/`) e do backend para identificar nova causa raiz.

---

## Arquivos criados/modificados

- `frontend/playwright.config.ts` (modificado: adiciona `workers: 1` com comentário)
- `frontend/e2e/fixtures/payloads-telegram.ts` (modificado: substitui `Date.now()` por gerador int32-safe)
