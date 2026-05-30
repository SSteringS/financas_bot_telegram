# Templates

Pasta centraliza templates canônicos de documentos do projeto. Cada template carrega o schema obrigatório (parseável por script quando aplicável) e o esqueleto das seções esperadas.

## O que mora aqui

- [`_TEMPLATE-status.md`](_TEMPLATE-status.md) — template de **status report** (schema do ADR 0007). Usar copiando pra `docs/sprints/<NN>/status/<TASK-ID>-<slug>.md` e preenchendo. **Não edite o template** — só atualize quando o schema mudar (e atualize também o ADR 0007).

## Não está aqui (ainda)

Os templates de **ADR** (`docs/decisions/_TEMPLATE.md`) e de **plano** (`docs/plans/_TEMPLATE.md` se vier a existir) ficam por ora nas próprias pastas de destino. Centralizá-los aqui vira candidato a backlog de workflow — a decisão é se a redução de fricção (1 lugar pra atualizar schemas) compensa a quebra de localidade. Avaliar depois que `docs/templates/` tiver uso real.

## Convenções

- Nome do arquivo: `_TEMPLATE-<tipo>.md` (prefixo `_` mantém no topo da listagem alfabética; sufixo deixa explícito qual template é).
- Schema declarado em comentário no frontmatter; mudanças no schema = mudança no ADR correspondente.
