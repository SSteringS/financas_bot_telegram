# Sprint 02b — Kaizen (workflow / processo)

**Status:** 🟡 em execução (refinamento de 2026-05-30; ordem dispatchada na mesma data).

**Objetivo:** fechar os itens de **processo/tooling** que ficaram acumulando em "tópicos pra RETRO-02" durante a sprint 02 (#8, #9, #10 do `BACKLOG-evolucao-workflow.md`) + ações estruturais que saíram da RETRO-02 §8. Sprint curta dedicada a workflow, sem trabalho de produto.

**Por que existe:** durante a sprint 02, melhorias de processo competiram mal com produto e foram empilhadas. A retro registrou que isso vira dívida permanente se sprints de produto não tiverem janela paralela pra processo. A kaizen é o experimento — se funcionar, vira padrão (kaizen entre sprints grandes); se virar overhead, repensa na RETRO-03.

## Escopo

Refinadas + prontas pra execução (esta primeira leva):

1. **WF-01** — Investigar e mitigar truncamento de writes do Cowork (causa-raiz dos truncamentos; substitui "sync gremlin OneDrive").
2. **WF-02** — Completar migração ADR 0010 (limpar `docs/status/` legada, criar `docs/templates/`).
3. **WF-03** — Checklist arquitetural explícito no `reviewer.md` (item #8 do backlog).

A refinar depois desta leva mergear:

4. **WF-04** — Adoção zero-padded geral (item #9 do backlog).
5. **WF-05** — Roles × skills × workflows (item #10 do backlog — provavelmente envolve arquiteto).
6. **WF-06** — Ritual de métricas no fim de cada sprint.
7. **WF-07** — Ligar branch protection (CI-01 vira bloqueante).
8. **WF-08** — Formalizar `pendencias_humano` no schema de status.

## Ordem sugerida (primeira leva)

WF-01 → WF-02 → WF-03 numa sessão executora única (~3h total). Independentes entre si — ordem é pra continuidade narrativa (WF-01 estabelece a regra de escrita defensiva que WF-02 e WF-03 já podem aplicar).

## Fluxo de git desta sprint

Como todas as WF-NN são **meta-workflow do planner** (tocam só `docs/`, `CLAUDE.md`, `docs/roles/`), o fluxo segue a regra natural do planner (CLAUDE.md §Instâncias):

- **Commit direto em `develop`** — sem branch de feature, sem PR.
- **Humano revisa o diff** (`git show HEAD`) **antes de cada `git push`**.
- **Reviewer não entra** — papel do Reviewer é pra entregas de implementador (back/front/infra). Planner natural já trabalha sem Reviewer; humano é a revisão.

Isso difere das tasks de produto da sprint 02 (BE-/FE-/FIX-/HOTFIX-, todas em branch + PR + Reviewer). Sprint 03 (EVO-09) volta ao fluxo normal de produto.

## Estrutura

`plans/` · `status/` · `avaliacoes/` desta sprint estão aqui (ADR 0010). `avaliacoes/` provavelmente fica vazia (sem Reviewer). Retro do ciclo (junto da RETRO-03 da sprint 03 ou separada?) decidir no fim.

## Próximos passos

1. Sessão nova executora pega `DISPATCH-KAIZEN-sessao-executora.md` e executa WF-01 → WF-02 → WF-03 (commit direto em develop, parar entre tasks).
2. Humano revisa cada `git show HEAD` e dá push quando aprovar.
3. Sprint kaizen continua com refinamento das 5 restantes (WF-04..WF-08) na sessão do planner.
4. Quando todas commitadas e pushadas, fecha a sprint kaizen e abre a sprint 03 (EVO-09).
