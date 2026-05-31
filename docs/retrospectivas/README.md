# Retrospectivas

Retros de fim de ciclo/etapa — estilo Scrum. Olham pra **trás** (o que entregou, o que foi bem, o que melhorar) pra alimentar o **próximo** ciclo.

Não confundir com:
- `docs/avaliacoes/` — revisão de **uma entrega** (pelo Reviewer).
- `docs/sprints/<NN>/status/` — relatório de **uma task**.
- `docs/PENDENCIAS-TECNICAS.md` — débito técnico.

Aqui é o **olhar agregado do ciclo**: workflow, aprendizados, e as **ações** que carregamos adiante.

## Como conduzir o fechamento

Seguir `docs/runbooks/RUNBOOK-fechamento-sprint.md` — sequência de 6 passos: métricas de tasks (script), agregação de skills (B+C), 4 perguntas de agents & skills, escrita da retro, atualização do backlog de workflow, aprovação humana.

## Convenção

- Arquivo por retro: `RETRO-NN-<contexto>.md`, numerado (`RETRO-01-...`).
- Estrutura sugerida: resultado · dados/métricas · o que foi bem · o que melhorar · aprendizados · incidentes · ações pra próxima etapa.
- Usar dados reais quando der (ex.: `docs/scripts/metricas_status.py`). O sistema de métricas amadurece a cada retro — começar simples, escolher indicadores que mudem comportamento.
- A próxima retro mede se as **ações** da anterior avançaram.

## Índice

- [`RETRO-01-mvp-fase3.md`](RETRO-01-mvp-fase3.md) — MVP da Fase 3 (camada de visualização) no ar; primeira retro do projeto.
- [`RETRO-02-canal-whatsapp.md`](RETRO-02-canal-whatsapp.md) — Sprint 02: código WhatsApp em prod (inerte) + observability + UX; primeira retro com comparação direta com a anterior.
- [`RETRO-02b-kaizen-workflow.md`](RETRO-02b-kaizen-workflow.md) — Sprint 02b: kaizen de processo — 7/8 WF tasks concluídas, 16 skills criadas, sprint kaizen confirmada como modelo.
