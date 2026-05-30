# ADR 0010 — Organização da documentação por sprint (entrega)

**Data:** 2026-05-27
**Status:** Accepted
**Decisores:** humano (com ajuda do Claude planejador)
**Relacionado:** estende o ADR 0004 (taxonomia de persistência — *o que* mora onde). Este define *como agrupar no tempo* (por ciclo de entrega).

---

## Contexto

O trabalho passa a ser organizado em **sprints orientadas a entrega** (goal-based, **sem timebox**): cada sprint é definida por um **deliverable** (ex.: "MVP no ar"), não por um período fixo. A sprint termina quando a entrega está pronta.

Com o MVP fechado, o `docs/` plano começou a virar ruído: **44 planos** e **43 status reports** num diretório só, a maioria já entregue. Misturar o conjunto ativo com o histórico atrapalha a legibilidade. Mas jogar *tudo* em pastas de sprint quebraria o conhecimento **cumulativo** (ADRs numerados, aprendizados, arquitetura) que vive *acima* das sprints e é referenciado entre elas.

---

## Decisão

Dividir a documentação em duas famílias, por um critério único:

> **"Esse doc pertence a UMA entrega e vira ruído depois, ou ele acumula e é referenciado ENTRE entregas?"**
> Bounded ao ciclo → **sprint-scoped**. Cumulativo/canônico → **global**.

### Sprint-scoped — `docs/sprints/<NN-nome>/`
Os **contratos task-level de um ciclo**:
- `plans/` — planos das tasks daquela sprint (input contracts).
- `status/` — status reports daquela sprint (output contracts).
- `avaliacoes/` — revisões do Reviewer daquela sprint.
- `README.md` — **o objetivo/entrega que define "pronto"** (substitui a timebox) + índice das tasks + ponteiro pra retro.

### Global — raiz de `docs/`
O **conhecimento cumulativo, canônico e cross-cutting**:
- `decisions/` (ADRs — numeração sequencial, imutáveis, referenciados de qualquer sprint)
- `aprendizado/` (conceitos formativos, atualizados ao longo do tempo)
- `architecture/` (a verdade do sistema *como é hoje*)
- `runbooks/` (referência operacional reusada)
- `retrospectivas/` (série lida entre sprints)
- `PENDENCIAS-TECNICAS.md` (débito atravessa sprints)
- `CLAUDE.md`, `roles/`, `scripts/`, e os **`_TEMPLATE.md`** (plano/status/ADR) — infra de processo reusada
- `STATE.md` (ponteiro do "agora")
- Planos **cross-cutting não-task** (ex.: `BACKLOG-evolucao-workflow.md`) ficam num `docs/plans/` global, junto dos templates.

### Regras de borda
- **Templates não se movem** — ficam no lar global (evita churn de referências que apontam pra `docs/plans/_TEMPLATE.md` etc.).
- **Planos parqueados/futuros** (ex.: DEP-08 parqueado) e backlogs cross-cutting ficam no `docs/plans/` global até serem puxados pra uma sprint.
- A **retro** de uma sprint vive na série global `docs/retrospectivas/`; o README da sprint aponta pra ela.
- **Numeração de task-id** (BE-05, DEP-03…) segue **global e sequencial** — a pasta da sprint agrupa, não renumera.

---

## Migração (este momento)

Ponto de inflexão natural (MVP fechado): **arquivar a Fase 3 em `docs/sprints/01-mvp/`**.

- Vão pra `docs/sprints/01-mvp/{plans,status,avaliacoes}/`: os planos e status das tasks BE/FE/DEP/FIX/HOTFIX/EVO-07/CI-01 **entregues**, o master-plan `BACKLOG-produto.md`, os `MASTER-PROMPT-overnight-*`, e as avaliações do ciclo.
- **Ficam no global** `docs/plans/`: `_TEMPLATE.md`, `BACKLOG-evolucao-workflow.md`, e planos ainda não entregues/parqueados (DEP-08; DEP-07 se ainda não mergeado).
- `docs/status/_TEMPLATE.md` e `docs/avaliacoes/README.md`/`_TEMPLATE.md` ficam globais.
- A próxima sprint (`02-*`) nasce com a estrutura nova quando o deliverable for definido (decisão de produto pendente).

A movimentação é `git mv` (executada pelo humano; o planner não roda git).

---

## Razões

- **Legibilidade do conjunto ativo:** a pasta da sprint corrente mostra só o que está em jogo; o histórico fica arquivado por entrega.
- **Preserva o cumulativo:** ADRs, aprendizado e arquitetura continuam num lugar canônico, sem fragmentar referências nem a numeração.
- **Encaixa no goal-based:** o README de objetivo de cada sprint materializa "o que define pronto" — o que substitui a timebox.

---

## Consequências

**Positivas:**
- Conjunto ativo legível; histórico navegável por entrega.
- Conhecimento canônico intacto e referenciável de qualquer sprint.

**Negativas / custos:**
- Reorg pontual (um `git mv` em lote agora).
- Ajustar quem varre `docs/status`: o `metricas_status.py` passa a olhar `docs/status/` **e** `docs/sprints/*/status/`.
- Atualizar a descrição da estrutura no `CLAUDE.md` e no `docs/README.md`.
- Status→plan da mesma task agora convivem na mesma pasta de sprint (na verdade, simplifica).

**Follow-ups disparados:**
1. `docs/scripts/metricas_status.py` — glob backward-compatível (global + sprints). ✅ nesta leva.
2. `CLAUDE.md` (seção "Estrutura da pasta docs/") + `docs/README.md` — refletir `sprints/`. ✅ nesta leva.
3. `git mv` do MVP pra `01-mvp/` — humano.

---

## Alternativas consideradas

- **Tudo em pastas de sprint:** descartado — fragmenta ADRs/aprendizado/arquitetura e quebra a numeração e as referências cruzadas.
- **Nada em pastas de sprint (manter plano):** descartado — o ruído cresce a cada ciclo (já em 44 planos).
- **Subpasta por sprint dentro de `plans/` e `status/` separadamente:** viável, mas `docs/sprints/<NN>/` agrupa os três tipos (plans+status+avaliacoes) e o objetivo da sprint num lugar só — mais coeso.

---

## Referências

- ADR 0004 (taxonomia de persistência) — este ADR a estende no eixo temporal.
- `docs/retrospectivas/RETRO-01-mvp-fase3.md` (a retro que fechou a sprint 01).
- `docs/sprints/README.md` (convenção operacional das pastas de sprint).
</content>
