# RUNBOOK — Fechamento de sprint

Sequência obrigatória de encerramento de cada sprint. Executada pelo **planner**; passo final aprovado pelo **humano**.

Referência: WF-06 (sprint kaizen, 2026-05-31). Alimenta `RETRO-NN-*.md` e `BACKLOG-evolucao-workflow.md`.

---

## Quando usar

Sempre que uma sprint fecha — produto, kaizen ou qualquer outra. Antes de abrir o README da próxima sprint.

**Tempo estimado:** 20–40 min (planner) + 10 min revisão (humano).

---

## Pré-condições

- Todas as tasks da sprint estão em `concluido`, `parcial` ou `bloqueado` (nenhuma `em-execucao`).
- Status reports e avaliações do Reviewer estão commitados em `docs/sprints/<NN>/status/` e `docs/sprints/<NN>/avaliacoes/`.

---

## Passo 1 — Métricas de tasks

**Quem:** planner.

Rodar o script de métricas apontando pra sprint que está fechando:

```bash
python3 docs/scripts/metricas_status.py --dir docs/sprints/<NN>/status
```

Anotar os números que vão pra tabela §2 da retro:

| Campo | De onde vem |
|---|---|
| Tasks concluídas / parciais / bloqueadas | `estados` no output |
| Gate-fails | `gate_fails` |
| Desvios totais | `desvios_total` |
| Pendências humano | `pendencias_humano_total` |
| Testes novos nesta sprint | `testes_novos` |
| Testes total acumulado | `testes_total` |

Comparar com os números da sprint anterior (estão na retro anterior, §2).

---

## Passo 2 — Agregação de skills (sinal B + C)

**Quem:** planner (leitura manual enquanto `metricas_status.py` não parseia os campos novos).

Para cada plano em `docs/sprints/<NN>/plans/`, coletar `skills_dispatched`.
Para cada avaliação em `docs/sprints/<NN>/avaliacoes/`, coletar `skills_eficazes` e `skills_gaps`.

Montar tabela de agregação:

| Skill | Dispatched | Eficazes | Gaps | Sinal |
|---|---|---|---|---|
| `arquitetura-hexagonal` | N | M | K | eficácia = M/N; gap rate = K/N |
| ... | | | | |

Skills com `eficazes = 0` e `dispatched > 0` por 2+ sprints consecutivas → candidatas a `status: deprecated`.

---

## Passo 3 — Agents & Skills: 4 perguntas

**Quem:** planner (rascunho) + humano (julgamento de produto nas perguntas a e c).

Responder as 4 perguntas que vão pra §3 da retro:

**a. Qual skill foi mais eficaz?**
Maior `eficazes/dispatched`. Confirmar com evidência qualitativa (ex: "nenhum drift hexagonal detectado nas 3 tasks que tocaram application/").

**b. Qual skill falhou ou não ativou quando deveria?**
`dispatched > 0, eficazes = 0` OU `gaps > 0` em skills não-dispatched. Se falhou: o `description` (gatilho) está certo? **Corrigir agora** antes de fechar a sprint — não deixar pra depois.

**c. Algum padrão novo se repetiu 2× e virou candidato a skill?**
Olhar seções `## Padrões técnicos` dos status reports BE/FE e seção `## Observações materiais` das avaliações. Se 2+ tasks descreveram o mesmo problema → candidato.

**d. Alguma skill com `status: deprecated` por 2 sprints?**
Verificar `.claude/skills/*/SKILL.md` com `status: deprecated`. Se já deprecated há 2 sprints e ninguém reclamou da ausência → deletar.

---

## Passo 4 — Escrever a retro

**Quem:** planner escreve; humano revisa.

Copiar `docs/templates/_TEMPLATE-retro.md` para `docs/retrospectivas/RETRO-NN-<slug>.md`.

Preencher **obrigatoriamente**: §1, §2 (com dados do Passo 1), §3 (com dados dos Passos 2–3), §7, §8.
Preencher conforme o que aconteceu: §4, §5, §6 (incidentes só se houver).

Atualizar `docs/retrospectivas/README.md` com o novo link.

---

## Passo 5 — Backlog de workflow

**Quem:** planner.

Para cada item novo de processo ou melhoria identificado na retro:
- Adicionar em `docs/plans/BACKLOG-evolucao-workflow.md` com prioridade (alta / média / baixa) e motivo.

Para cada item já existente no backlog que foi resolvido nesta sprint:
- Marcar `✅ feito (WF-XX, data)`.

---

## Passo 6 — Aprovação humana + abertura da próxima sprint

**Quem:** humano revisa retro e backlog; planner executa a abertura.

Checklist final (humano confirma cada item):

- [ ] Retro faz sentido — números batem com o que aconteceu
- [ ] Skills com problema identificadas e description atualizada (se necessário)
- [ ] Backlog de workflow atualizado
- [ ] Nenhuma task da sprint ficou `em-execucao` (todas fechadas ou explicitamente parqueadas)

Após aprovação: planner cria `docs/sprints/<NN+1>-<slug>/README.md` com escopo da próxima sprint.

---

## Relação com outros docs

- Template de retro: `docs/templates/_TEMPLATE-retro.md`
- Script de métricas: `docs/scripts/metricas_status.py`
- Backlog de workflow: `docs/plans/BACKLOG-evolucao-workflow.md`
- Schema de skill (lifecycle): `docs/skills/README.md` (campo `status`)
- Feedback loop de skills (campos B+C): `docs/templates/_TEMPLATE-plano.md` e `_TEMPLATE-avaliacao.md`
