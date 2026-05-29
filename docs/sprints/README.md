# Sprints (entregas)

Cada sprint é **orientada a entrega** (goal-based, **sem timebox**): existe um *deliverable* que define o fim. Esta pasta guarda os **contratos task-level de cada ciclo** — o que é efêmero/bounded a uma entrega. O conhecimento cumulativo (ADRs, aprendizado, arquitetura, runbooks, retros, débito) fica **global**, na raiz de `docs/`. Regra e critério: **ADR 0010**.

## Estrutura de cada sprint

```
docs/sprints/<NN-nome>/
  README.md       ← objetivo (a entrega que define "pronto") + índice das tasks + link da retro
  plans/          ← planos das tasks (input contracts)
  status/         ← status reports (output contracts)
  avaliacoes/     ← revisões do Reviewer
```

## Convenção

- Nome: `NN-slug` (`01-mvp`, `02-...`), numerado.
- O `README.md` da sprint declara **o objetivo/entrega** (substitui a timebox) e indexa as tasks.
- Task-id (`BE-05`, `DEP-03`…) é **global e sequencial** — a pasta agrupa, não renumera.
- Fica **fora** daqui (global): templates (`_TEMPLATE.md`), backlog de workflow, planos parqueados/futuros, ADRs, aprendizado, arquitetura, runbooks, retrospectivas, PENDENCIAS.
- `metricas_status.py` varre `docs/status/` **e** `docs/sprints/*/status/`.

## Sprints

- [`01-mvp/`](01-mvp/) — MVP (Fase 3, camada de visualização). ✅ entregue (E2E em prod). Retro: `docs/retrospectivas/RETRO-01-mvp-fase3.md`.
- [`02-canal-whatsapp/`](02-canal-whatsapp/) — Canal WhatsApp + notificação de pagamento + observability. 🔜 em discovery/planejamento.
</content>
