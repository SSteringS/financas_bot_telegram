---
task: QA-008
titulo: "Estabilização da suíte E2E — workers serializados + update_id em int32"
data: 2026-06-04
branch: feature/qa-008-estabilizacao-suite-e2e
responsavel: claude-front
estado: parcial
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
  executado: false
  status: nao-aplicavel
  specs_total: null
  specs_passed: null
  specs_failed: null
  duracao_segundos: null
  data_execucao: null
commits:
  - 031d55a
pr: https://github.com/SSteringS/financas_bot_telegram/pull/93
desvios: 0
pendencias_humano: 1
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

**Gate pendente (requer ambiente local):**
- `npm run e2e:full` verde 3x consecutivas — **não executado nesta sessão** (requer MySQL + backend + frontend rodando). Marcado como `pendencias_humano: 1`.

Esta validação é o **critério duro** do plano. O status permanece `parcial` até o humano confirmar as 3 execuções e atualizar o frontmatter (`e2e_full.executado: true`, `e2e_full.status: verde`, etc.).

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

**Comentário de 3 linhas em `playwright.config.ts`:** o plano exige comentário explicativo para `workers: 1`. Optei por 3 linhas (motivo + caminho de saída) em vez de 1 linha para registrar tanto o "por quê agora" quanto o "como escalar no futuro", evitando que alguém remova o `workers: 1` sem entender o trade-off.

**Comentário de 6 linhas em `payloads-telegram.ts`:** detalhado propositalmente — documenta a spec do Telegram, o range do int32, e a referência ao `banco.ts:91`. Serve como documentação inline permanente do invariante, para qualquer futuro mantenedor que olhe o arquivo.

---

## Decisões pendentes (esperando humano)

1. **Validação `e2e:full` 3x consecutivas:** rodar `cd frontend && npm run e2e:full` 3 vezes em ambiente limpo (MySQL + back + front). Se tudo verde, atualizar este status report:
   ```yaml
   estado: concluido
   e2e_full:
     exigido: true
     executado: true
     status: verde
     specs_total: 2
     specs_passed: 2   # (3 testes nos 2 specs)
     specs_failed: 0
     duracao_segundos: <medir>
     data_execucao: <YYYY-MM-DDTHH:MM:SSZ>
   pendencias_humano: 0
   ```
   E adicionar seção "Evidência E2E" com o resultado das 3 execuções.

---

## Próximos passos / observações

- Após validação: task fecha, QA-006 (gate no PRE-MERGE-CHECKLIST) fica desbloqueada.
- **Confirmar no log do backend** que `JsonMappingException: Numeric value (...) out of range of int` **não aparece mais** durante a execução — é o sinal de que o Bug B foi resolvido de ponta a ponta.
- Se `npm run e2e:full` ainda falhar após as correções: investigar logs do Playwright (`playwright-report/`) e do backend para identificar nova causa raiz.

---

## Arquivos criados/modificados

- `frontend/playwright.config.ts` (modificado: adiciona `workers: 1` com comentário)
- `frontend/e2e/fixtures/payloads-telegram.ts` (modificado: substitui `Date.now()` por gerador int32-safe)
