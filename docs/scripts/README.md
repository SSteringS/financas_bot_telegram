# docs/scripts/

Ferramentas de processo (meta) mantidas pelo planner. Operam sobre os docs do
projeto — não são código de produto (back/front).

## `metricas_status.py`

Lê o frontmatter dos `docs/sprints/<NN>/status/*.md` e agrega métricas que mudam comportamento
(estado das tasks, gate-fails, desvios/pendências, soma de testes) + cobertura do
schema canônico. Materializa a ideia do ADR 0007: o status report é um *output
contract* parseável, então dá pra montar um painel sem esforço.

```bash
python3 docs/scripts/metricas_status.py            # painel em texto
python3 docs/scripts/metricas_status.py --json     # mesmo dado em JSON
python3 docs/scripts/metricas_status.py --dir docs/status
```

Sem dependência obrigatória (usa PyYAML se houver, senão um parser mínimo). Robusto
a CRLF e a reports antigos sem frontmatter.

### Leitura do resultado

`cobertura do schema` mostra quantos reports seguem o schema canônico (`estado` +
bloco `gates`). Hoje a maioria é anterior ao `docs/templates/_TEMPLATE-status.md` / ADR 0007,
então aparece como `frontmatter não-canônico` (convenção antiga, ex.: `status:` em
vez de `estado:`) ou `sem frontmatter (legado)`. **Não é dívida a corrigir
retroativamente** — o schema vale dos reports novos pra frente. O número serve pra
ver a adoção subir com o tempo, não pra refazer report antigo.
