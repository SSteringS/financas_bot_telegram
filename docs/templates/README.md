# Templates

Pasta centraliza os templates canônicos dos 4 artefatos do workflow. Cada template carrega frontmatter YAML obrigatório (parseável por script) e o esqueleto das seções esperadas.

## Os 4 templates

| Template | Artefato | Autor | Quando | Mora em |
|---|---|---|---|---|
| [`_TEMPLATE-plano.md`](_TEMPLATE-plano.md) | **Plano** — input contract da task | Planner | Antes da implementação | `docs/sprints/<NN>/plans/<TASK-ID>-<slug>.md` |
| [`_TEMPLATE-status.md`](_TEMPLATE-status.md) | **Status report** — output contract do implementador (schema ADR 0007) | Implementador (back/front) | Ao terminar a implementação | `docs/sprints/<NN>/status/<TASK-ID>-<slug>.md` |
| [`_TEMPLATE-avaliacao.md`](_TEMPLATE-avaliacao.md) | **Avaliação** — verificação adversarial contra a realidade (ADR 0005) | Reviewer | Após o status, antes do merge | `docs/sprints/<NN>/avaliacoes/<TASK-ID>-<slug>.md` |
| [`_TEMPLATE-adr.md`](_TEMPLATE-adr.md) | **ADR** — decisão arquitetural canônica (ADR 0004) | Arquiteto/planner/engenheiro de IA propõe; humano homologa | Sempre que uma decisão cross-cutting é tomada | `docs/decisions/<NNNN>-<slug>.md` |

## Workflow por task (visão integrada)

```
                 Plano                  Status                 Avaliação
              (input contract)      (output contract)        (verificação)
                  ↑                       ↑                       ↑
Autor:          Planner               Implementador             Reviewer
Quando:         Antes                 Ao fim da                 Após status,
                                      implementação             antes do merge
Garante:        Reduz variância       Sintaxe (gates            Semântica (gates
                do implementador      autorreportados)          verificados contra
                                                                 a realidade)
```

ADR é ortogonal — nasce quando uma decisão atravessa tasks (não é por task).

## Convenções

- **Nome do arquivo de instância:** `<TASK-ID>-<slug>.md` (planos, status, avaliações) ou `<NNNN>-<slug>.md` (ADRs). Slug em kebab-case, descritivo, curto.
- **Prefixo `_` no nome do template:** mantém no topo da listagem alfabética; sufixo deixa explícito qual artefato é.
- **Não edite o template diretamente** — copie pra a pasta de destino, preencha frontmatter + seções, e instale o resultado lá. Mudança no schema só vale com atualização do ADR correspondente (0004 / 0005 / 0007).
- **Frontmatter YAML obrigatório em todos os 4** — parseável por script (`docs/scripts/metricas_status.py` extensível). Forward-only: artefatos antigos sem frontmatter ficam como estão.

## Relação com outros mecanismos do projeto

- Os 4 templates são o esqueleto **escrito**; os gates verificáveis (`build`, `testes`, `lint`, `branch_convencao`, `territorio`) ficam no `docs/runbooks/PRE-MERGE-CHECKLIST.md`.
- ADR 0007 fixa o status como output contract; ADR 0005 define o Reviewer (autor da avaliação); ADR 0004 define ADR como decisão canônica imutável após `Accepted`.
- Roles (`docs/roles/`) carregam estes templates por referência — backend.md/frontend.md/reviewer.md/planner.md/architect.md citam quais artefatos cada papel escreve.
