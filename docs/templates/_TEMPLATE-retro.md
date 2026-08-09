---
# ─── Frontmatter (schema obrigatório — parseável) ───
sprint: 03-evo-09                          # NN-slug
titulo: "Folha de pagamento"               # título curto da sprint
data: 2026-06-01                           # YYYY-MM-DD — data da retro
periodo: "2026-05-31 → 2026-06-01"        # início → fim da sprint
resultado: parcial                         # sucesso | parcial | bloqueado
tasks_concluidas: 0                        # int
tasks_parciais: 0                          # int
tasks_bloqueadas: 0                        # int
gate_fails: 0                              # int
desvios_total: 0                           # int
testes_novos: 0                            # int
---

# Retrospectiva NN — <título>

> **Não edite este arquivo.** Copie pra `RETRO-NN-<slug>.md` em `docs/retrospectivas/`, preencha o frontmatter e as seções abaixo. Seguir `docs/runbooks/RUNBOOK-fechamento-sprint.md` para conduzir o ritual completo.

**Data:** YYYY-MM-DD
**Período coberto:** início → fim
**Resultado:** ✅ sucesso | 🟡 parcial | 🔴 bloqueado — frase de resultado em uma linha.

Esta é a **N-ésima** retro do projeto. A retro anterior (`RETRO-NN-1-*.md`) teve X ações; o status delas está na §7.

---

## 1. O que foi entregue

Lista de tasks concluídas com uma linha por item: ID + título + PR/commit se aplicável.

Parqueadas: o que ficou fora de escopo e por quê.

---

## 2. Dados (medidos pelo `metricas_status.py`)

Rodar: `python3 docs/scripts/metricas_status.py --dir docs/sprints/<NN>/status`

| Métrica | RETRO anterior | Esta RETRO | Δ |
|---|---|---|---|
| Tasks concluídas | — | — | — |
| Tasks parciais / bloqueadas | — | — | — |
| Gate-fails | — | — | — |
| Desvios totais | — | — | — |
| Pendências humano | — | — | — |
| Testes novos | — | — | — |
| Testes total acumulado | — | — | — |

> Copiar os números do output do script. Comparar com a coluna da retro anterior.

---

## 3. Agents & Skills

> Preenchido a partir dos Passos 2–3 do `RUNBOOK-fechamento-sprint.md`.

### Tabela de uso

| Skill | Dispatched | Eficazes | Gaps | Sinal |
|---|---|---|---|---|
| — | — | — | — | — |

### 4 perguntas

**a. Skill mais eficaz nesta sprint:**
_(qual + evidência concreta)_

**b. Skill que falhou ou não ativou quando deveria:**
_(qual + causa + ação tomada agora: description atualizada? skill deprecada?)_

**c. Candidato novo a skill (padrão repetido 2×):**
_(padrão + tasks onde apareceu + proposta de nome)_

**d. Skills com `status: deprecated` há 2+ sprints:**
_(listar + decisão: deletar ou reativar)_

Se nenhum sinal: "Sem observações de skills nesta sprint."

---

## 4. O que foi bem (continuar)

Bullets. O que funcionou e deve ser preservado no próximo ciclo.

---

## 5. O que pode melhorar

Bullets. Fricções, gargalos, inconsistências. Cada item que virar ação vai pra §8.

---

## 6. Aprendizados e incidentes

### Aprendizados

Novos arquivos criados em `docs/aprendizado/` nesta sprint (linkar). Conceitos que surgiram e valem registro.

Se nenhum: "Nenhum aprendizado novo registrado."

### Incidentes (omitir seção se não houver)

| Incidente | Causa | Resolução |
|---|---|---|
| — | — | — |

---

## 7. Status das ações da retro anterior

| # | Ação | Status |
|---|---|---|
| 1 | _copiar da retro anterior_ | ✅ Cumprida / 🟢 Parcial / ⏸ Não tocada |

**Taxa de cumprimento:** X cumpridas (Y%), Z parciais, W não tocadas.

---

## 8. Ações pra próxima etapa

Cada ação tem: descrição + responsável + sprint-alvo ou "contínuo".

| # | Ação | Responsável | Sprint-alvo |
|---|---|---|---|
| 1 | | planner / humano / implementador | sprint NN+1 |

> Ações de processo vão também pro `BACKLOG-evolucao-workflow.md` (Passo 5 do runbook).
