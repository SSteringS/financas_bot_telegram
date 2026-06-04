# DISPATCH — QA-008 estabilização suíte E2E (single-task)

> **Urgente.** A suíte E2E falha por race condition (workers paralelos) e `update_id` estourando int32.
> Duas mudanças mecânicas em dois arquivos — sem decisão pendente.
> Depende de QA-004 e QA-007 mergeados em `integration/03-folha-pagamento`.

---

## Pré-condições (git)

- `feature/qa-004-specs-mvp-3-cenarios` mergeada em `integration/03-folha-pagamento` ✅
- `feature/qa-007-remover-axe-core-a11y-do-gate` mergeada em `integration/03-folha-pagamento` ✅
  Confirmar: `git log origin/integration/03-folha-pagamento --oneline | grep -E "qa-004|qa-007"`

---

## O prompt (cole tudo numa sessão `--agent frontend`)

```
Task: QA-008 — Estabilização da suíte E2E (workers serializados + update_id em int32).

Leia o plano:
  docs/sprints/03-folha-pagamento/plans/QA-008-estabilizacao-suite-e2e-workers-e-update-id.md

## BRANCH

  git fetch
  git checkout -b feature/qa-008-estabilizacao-suite-e2e origin/integration/03-folha-pagamento

## A TASK

Duas mudanças, mesma branch, mesmo commit.

### Mudança 1 — frontend/playwright.config.ts

Adicionar `workers: 1` no `defineConfig`. Incluir comentário 1-linha explicando por quê
(compartilhamento de requisitante_id=99 entre specs exige execução serial).

Exemplo:
  export default defineConfig({
    workers: 1, // specs compartilham requisitante_id=99 — paralelo causa race no banco
    ...
  });

### Mudança 2 — frontend/e2e/fixtures/payloads-telegram.ts

Localizar o gerador de update_id (usa Date.now() como base). Trocar por:

  // Base aleatória dentro de int32 — evita colisão com runs anteriores em mensagem_processada
  // e respeita a spec do Telegram que define update_id como Integer (32-bit).
  let _updateCounter = Math.floor(Math.random() * 1_000_000_000);

Manter o incremento sequencial dentro do run (o _updateCounter++ na função nextUpdateId).

## VALIDAÇÃO OBRIGATÓRIA

Após as mudanças, rodar npm run e2e:full 3 vezes seguidas:

  npm run e2e:full   # execução 1
  npm run e2e:full   # execução 2
  npm run e2e:full   # execução 3

Critério: todas as 3 verdes, sem flakiness.
Conferir o log do backend durante uma das execuções — NÃO pode aparecer:
  JsonMappingException: Numeric value (...) out of range of int

## REGRAS DURAS

1. Branch: `feature/qa-008-estabilizacao-suite-e2e` saindo de `origin/integration/03-folha-pagamento`.
2. Território: SÓ `frontend/playwright.config.ts` e `frontend/e2e/fixtures/payloads-telegram.ts`.
3. Zero mudanças em specs, fixtures de banco/auth, ou código de produto.
4. 1 commit: `fix(QA-008): workers serializados + update_id int32-safe`.
5. NÃO mergeie. PR para `integration/03-folha-pagamento` após status report + Reviewer.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/QA-008-estabilizacao-suite-e2e-workers-e-update-id.md`

testes_novos: 0
Registrar resultado das 3 execuções consecutivas: timestamp, duração, resultado.
Mencionar explicitamente que o log do backend não apresentou JsonMappingException.

Pare ao final. Não mergeie.
```

---

## ⚠️ Nota especial pro Reviewer desta task

- Verificar que `workers: 1` tem comentário explicativo (critério duro).
- Confirmar que `payloads-telegram.ts` não usa mais `Date.now()` como base do counter.
- Rodar `npm run e2e:full` localmente pelo menos 1x antes de aprovar.
- Confirmar que zero mudança em `frontend/src/` ou `financas_bot_telegram/`.

---

## Notas pro humano

- **Estimativa:** 30-45 minutos (incluindo as 3 execuções de validação).
- **Desbloqueio:** após merge, a suíte E2E torna-se confiável como gate. QA-006 (que adicionou e2e:full ao PRE-MERGE-CHECKLIST) fica operacional de fato.
- **Por que sai de integration e não de develop:** QA-004 entregou as specs; sem elas, não há o que estabilizar.

## Referências

- `docs/sprints/03-folha-pagamento/plans/QA-008-estabilizacao-suite-e2e-workers-e-update-id.md`
- `frontend/playwright.config.ts` — mudança 1
- `frontend/e2e/fixtures/payloads-telegram.ts` — mudança 2
