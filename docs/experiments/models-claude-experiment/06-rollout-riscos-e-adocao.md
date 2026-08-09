# Rollout, métricas de adoção e riscos

## 1. Plano de rollout

### FASE 0 — Marco zero (bloqueante, ~1 semana)

Detalhada em `01-marco-zero-e-release.md`. Resumo: congelar o estado (0A) → construir rede de segurança de deploy (0B) → executar a release de 208 commits (0C) → verificar (0D) → congelar a janela de tasks (0E).

### FASE 1 — Pré-requisitos de validade (~1 sprint)

| # | Ação | Nota |
|---|---|---|
| 1.1 | Corrigir os 4 agents com `model` inválido; normalizar `tools` para o formato correto; substituir `handoffs`/`agents` por mecanismo suportado; declarar `effort` explícito | **Delegar à Skill `creating-agents`.** Artefato de subagente sai pela Skill correspondente, não da memória de um agente |
| 1.2 | **Verificar empiricamente** que o pin pegou, lendo `message.model` no JSONL de um run de teste | Sem esse teste, tudo depois é inválido |
| 1.3 | Instalar **JaCoCo**, **PIT** e **PMD**; configurar `test:coverage` do Vitest | Código real → passa pelo fluxo normal com reviewer e QA, fora do experimento |
| 1.4 | Criar `.claude/settings.json` com hooks `SubagentStart`/`SubagentStop` (PowerShell) + `collect_usage.py` + teste de regressão do schema JSONL | Instrumentação construída do zero |
| 1.5 | Fixar allowlist de permissões completa; proibir `Explore` nos agents do experimento; registrar IDs completos das versões de modelo | |
| 1.6 | Isolamento por run: worktree dedicado, snapshot e wipe de `.claude/agent-memory/`, artefatos em `experiments/model-mix/runs/<run_id>/` | Contaminação entre runs é o confounder mais fácil de cometer |
| 1.7 | Adicionar `rounds_reviewer` e `rounds_qa` ao `docs/templates/_TEMPLATE-status.md` | Métricas P2 e P3 não existem hoje em lugar nenhum |

### FASE 2 — Specs congeladas e pré-registro (gate humano)

Escrever as 3 specs de produto (humano). Classificar complexidade com 2 avaliadores independentes. Escrever `experiments/model-mix/PROTOCOL.md` com hipóteses H1–H4, endpoints primários, matriz de runs, rubricas R1–R5 com âncoras, seed de aleatorização, plano de análise e critérios de parada. **Commit e tag.** O hash é citado na publicação.

**Nenhum run inicia sem aprovação humana explícita deste pré-registro.**

### FASE 3 — Piloto

2 runs (`all-sonnet` e `all-opus`, feature média). Valida a coleta ponta a ponta, mede variância e custo real, **recalibra n**. Piloto é descartado da análise.

### FASE 4 — Screening (16 runs, feature média)
### FASE 5 — Confirmação (18 runs, features baixa e alta)
### FASE 6 — Avaliação cega

Auditor independente + rubrica humana + juiz LLM. Desbloqueio apenas ao final.

### FASE 7 — Análise, publicação, dataset e scripts

## 2. Segurança de migração

- **Nenhum branch `exp/*` é mergeado.** Um run é escolhido a posteriori e **reimplementado pelo fluxo normal**, com Reviewer e QA. O experimento não vira atalho de merge.
- `docs/sprints/` **não recebe** artefato de run experimental.
- As mudanças da Fase 1 (`pom.xml`, CI, `settings.json`, template de status) passam pelos gates normais **antes** do piloto.
- **Ponto de abandono explícito ao fim da Fase 3:** se o custo do piloto extrapolar o orçamento, cair para a variante enxuta de 20 runs, ou para o desenho C (âncoras + mixes), assumindo a perda de poder discriminante.

## 3. Métricas de adoção

| Dimensão | Baseline verificado | Alvo | Parada / ajuste |
|---|---|---|---|
| **Defasagem de produção** | **208 commits · 73 dias · schema V3 vs V7** | 0 commits no t0 | Smoke de 0D falhando → rollback, experimento não inicia |
| Verificação pós-deploy automatizada | `systemctl is-active` — 1 checagem, zero HTTP | ≥ 4 asserções (health, API autenticada, webhook, frontend) | — |
| Runbook de rollback | **não existe** | existe e foi **testado em dev** | Não testar não conta |
| Cobertura backend | **`na` em 100% dos 23 status reports** (JaCoCo ausente) | valor numérico em ≥ 95% dos runs | JaCoCo/PIT inviável → substituir Q2/Q3 por Q4 + R2 e declarar na publicação |
| Pinagem de modelo verificada | **0%** — 4 de 4 papéis com valor inválido | 100% dos runs com `message.model` == pinado | Qualquer divergência invalida o run; > 10% inválidos → parar e corrigir o harness |
| Dataset de tokens por papel | **não existe** | 100% dos runs com vetor K1 completo | Coleta falhando em > 1 run → congelar execução |
| Rodadas de reviewer e QA | **não registradas em lugar nenhum** | 100% dos runs | — |
| Runs invalidados por confounder | desconhecido | ≤ 10% | **> 20% → parar, corrigir, reiniciar a fase** |
| Concordância juiz LLM × humano | não medida | **α ≥ 0,6** | abaixo → juiz vira material descritivo, não evidência |
| Precisão de custo | — | meia-largura do IC de **K2** ≤ 15% da média | acima → mais réplicas ou menos features |
| Intervenções humanas por run | não medidas | ≤ 1 | > 2 em média → o fluxo não é autônomo o bastante; reportar como achado |
| **Adoção pós-experimento** | agents sem modelo pinado | mix que reduza K2 em ≥ 30% sem piorar Q1 além do IC vira default em `.claude/agents/*` | Resultado **expira** a cada troca de versão de modelo; revalidar |

## 4. Obstáculos e incógnitas

### Bloqueadores duros

1. **Os 4 agents da cadeia não pinam modelo válido.** Sem as ações 1.1 e 1.2, o experimento mede a mesma célula 34 vezes.
2. **Comportamento do Claude Code com `model` inválido é não documentado** — silencioso, erro ou fallback? Determinar empiricamente.
3. **`handoffs`/`agents` não são spec:** não se sabe hoje como a orquestração planner→backend→reviewer→qa realmente acontece. Precisa ser observado antes de ser controlado.

### Riscos de produção

4. **A release de 208 commits é irreversível na prática hoje.** Único ponto do plano com risco de perda de dados reais.
5. **Drift de infra não aplicado** — `terraform apply` antes do deploy da aplicação.
6. **Webhook do Telegram com cert self-signed** — falha silenciosa, já ocorreu.
7. **Token do Telegram exposto e não rotacionado**, prioridade alta.

### Riscos do desenho

8. **Efeito de teto em `Contatos`.** O template `Funcionario` é forte demais. A variante B2 mitiga, mas o risco sobrevive.
9. **Sem preço, não há ranking único.** A publicação entrega uma fronteira de Pareto, não um "o melhor mix é X". Uma manchete de ranking único exigiria uma função de peso — e ela é, necessariamente, preço.
10. **n pequeno.** Conclusões sobre qualidade serão descritivas com IC, nunca p-valor.
11. **Avaliador único.** O autor é também juiz. Cegamento e juiz LLM de outra família mitigam parcialmente; **recrutar 1 revisor externo** para a subamostra é barato e muda o peso da publicação.
12. **Complexidade confundida com domínio** — a feature alta é outro subdomínio, não só mais complexa. Insolúvel com 3 features.
13. **Validade externa:** 1 repositório, 1 stack (Java/Spring), 1 autor, 1 janela temporal de versões de modelo. Declarar como estudo de caso.
14. **Construto:** cobertura não é qualidade. Por isso Q3 (mutation score) e Q1 (defeitos escapados) carregam o peso, não Q2.

### Riscos de harness

15. **Formato JSONL é observação de disco, não contrato.** Uma atualização do Claude Code quebra a coleta no meio do experimento. Mitigação: registrar a versão (campo `version` está no JSONL) e travar atualizações durante a execução.
16. **Temperatura e seed não documentados** — provavelmente não configuráveis. A variância entre réplicas é irredutível. Declarar na publicação, não esconder.
17. **`docs/claude/` não existe** e 9 arquivos o citam como fonte. Dívida documental que afeta a rastreabilidade das afirmações.
