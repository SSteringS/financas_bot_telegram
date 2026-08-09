# Desenho experimental

## 1. Fatores e níveis

| Fator | Papel | Níveis |
|---|---|---|
| A | planner | `sonnet` (−1) / `opus` (+1) |
| B | implementador (backend) | `sonnet` (−1) / `opus` (+1) |
| C | reviewer | `sonnet` (−1) / `opus` (+1) |
| D | qa-test-specialist | `sonnet` (−1) / `opus` (+1) |

Espaço completo: 2⁴ = 16 configurações **por feature**.

**Congelados (não variam):** `effort` pinado igual em todos os papéis · allowlist de permissões · versão de modelo (IDs completos registrados) · commit base (`exp/base-t0`) · prompts de dispatch · spec de produto de cada feature.

## 2. Trade-off dos desenhos possíveis

| | **A — Fatorial completo** | **B — Screening fracionado + confirmação** (recomendado) | **C — Âncoras + mixes escolhidos (OFAT)** | **D — Fatorial de 2 fatores** |
|---|---|---|---|---|
| Desenho | 2⁴ = 16 configs × 3 features × 2 réplicas | 2^(4−1) res. IV (8 configs) na feature média ×2 réplicas + 3 configs nas features baixa/alta ×3 réplicas | all-sonnet, all-opus + 2 mixes por intuição × 3 features × 2 réplicas | Só implementador × reviewer (4 células), planner e QA pinados, × 3 features × 2 réplicas |
| Runs | **96** | **34** (variante enxuta: 20) | 24 | 24 |
| Estima | Todos os efeitos principais e todas as interações | 4 efeitos principais **limpos de interações de 2ª ordem**; interações de 2ª ordem aliasadas entre si | Nada isoladamente — mixes não formam base | Efeitos principais + interação dos 2 papéis escolhidos |
| Prós | Resposta completa | Responde à pergunta central com custo viável | Barato, narrativa simples | Barato, interação bem medida |
| Contras | Inviável; alto risco de virar dado parcial | Exige explicar o fracionamento na publicação | **Não isola qual papel causa o efeito** | Assume a priori que planner e QA não importam |
| Risco | Alto | **Médio** | Baixo de execução / **alto de validade** | Médio-baixo |

## 3. Matriz recomendada

### Fase Screening — feature MÉDIA · 2^(4−1) resolução IV · gerador D = ABC

| Config | planner | implementador | reviewer | QA |
|---|---|---|---|---|
| S1 | sonnet | sonnet | sonnet | sonnet |
| S2 | opus | sonnet | sonnet | opus |
| S3 | sonnet | opus | sonnet | opus |
| S4 | opus | opus | sonnet | sonnet |
| S5 | sonnet | sonnet | opus | opus |
| S6 | opus | sonnet | opus | sonnet |
| S7 | sonnet | opus | opus | sonnet |
| S8 | opus | opus | opus | opus |

8 configurações × 2 réplicas = **16 runs**. Estima os 4 efeitos principais limpos de interações de segunda ordem.

### Fase Confirmação — features BAIXA e ALTA

3 configurações × 2 features × 3 réplicas = **18 runs**:

- `all-sonnet` — piso de custo
- `all-opus` — teto de qualidade
- `best-mix` — vencedor do screening em qualidade por token

**Total: 34 runs.** Variante enxuta (screening com 1 réplica, confirmação com 2) = **20 runs**, decidida após o piloto medir a variância real.

## 4. Hipóteses pré-registradas

- **H1** — o efeito do modelo do *implementador* sobre defeitos escapados (Q1) é maior que o dos demais papéis, e cresce com a complexidade.
- **H2** — o efeito do modelo do *reviewer* é aproximadamente nulo na feature baixa e material na alta: há pouco a achar em tarefa simples.
- **H3** — o implementador concentra **mais de 60% dos tokens de saída** do run.
- **H4 (nula, pré-comprometida para publicação)** — nenhuma configuração se distingue do baseline `all-sonnet` além do ruído.

## 5. Separação entre constante e tratamento

Este é o ponto que mais facilmente se erra.

| Artefato | Quem produz | Quando | Papel |
|---|---|---|---|
| **Spec de produto** — o quê, critérios de aceitação numerados, decisões de negócio já tomadas | **Humano, manualmente** | Antes do t0, congelada | **Constante** — idêntica nos 34 runs |
| **Plano técnico** — como, decomposição em tasks, riscos, ordem | **Agente planner, dentro do run** | Durante cada run | **Tratamento** — é o fator A |

Refinar a feature até o nível de plano técnico mata o fator planner. Refine até "o quê", pare, congele.

## 6. Protocolo de execução de um run

```
run_id = <feature>-<config>-<replica>     ex.: MED-S3-r2
```

1. Worktree dedicado ao run; branch `exp/<run_id>` criada a partir da tag `exp/base-t0`. Registrar o SHA.
2. **Wipe de `.claude/agent-memory/`** — memória persistente entre runs é vazamento de informação. Restaurar snapshot fixo.
3. Configuração aplicada via `--agents '<JSON>'` (não editar arquivos versionados), com `model` e `effort` explícitos nos 4 papéis.
4. Sessão inicia com o mesmo prompt de dispatch, template literal congelado. **Grafo de execução forçado por @-mention**, nunca por linguagem natural — a delegação automática é não-determinística.
5. Ciclo do ADR 0019 roda normalmente: reviewer → correções → QA, com os limites de rodadas intactos (3 rodadas de reviewer, 2 loops completos).
6. Artefatos do run vão para `experiments/model-mix/runs/<run_id>/`. **Nunca** para `docs/sprints/` — contaminaria os runs seguintes.
7. Ao final, `collect_usage.py` varre o JSONL, agrega por `agentType` e grava `metrics.json`.
8. **Verificação de validade:** conferir `message.model` no JSONL. Se o modelo resolvido difere do modelo pinado, **o run é inválido e descartado**.
9. Branch `exp/*` **nunca** é mergeada.

## 7. Controle de variância

| Ameaça | Mitigação |
|---|---|
| Cache de prompt barateia a 2ª réplica | Espaçamento **≥ 90 min** entre runs da mesma feature (mata o TTL de 1h); reportar `cache_read_input_tokens` sempre à parte |
| Auto-compactação em runs longos | Detectar `compact_boundary` no JSONL; usar contagem e `preTokens` como covariável |
| Aprendizado do operador ao longo de 34 runs | Ordem sorteada com seed registrado, blocada por feature; protocolo scriptado |
| Sessões paralelas de implementador no worktree compartilhado | Runs **serializados**; nenhuma outra sessão ativa durante um run |
| Built-in `Explore` roda em Haiku fixo | Proibir `Explore` nos agentes do experimento; se ocorrer, marcar o run |
| Subagente em background auto-nega permissões | Allowlist fixa e completa antes do primeiro run |
| Estado do repo divergindo entre runs | Todo run parte de `exp/base-t0`; wipe de agent-memory; artefatos isolados |

## 8. Ordem e aleatorização

- Ordem de execução sorteada com **seed registrado no pré-registro**.
- Blocada por feature (todos os runs de uma feature no mesmo bloco).
- Espaçamento mínimo de 90 minutos entre runs da mesma feature.
- Nenhuma reordenação após o início. Qualquer desvio é registrado como incidente.
