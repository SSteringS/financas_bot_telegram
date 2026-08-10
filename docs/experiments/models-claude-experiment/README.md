# Experimento — Alocação de modelo por papel no fluxo multiagente

> Estado: **rascunho / aguardando aprovação humana**
> Data do desenho: 2026-08-08
> Autor do desenho: agente `ai-engineer` (delegações: Skill `harness-workflow-analyst`)
> Aprovação humana: **pendente** — nenhum run pode iniciar antes do pré-registro aprovado (Fase 2)

## Pergunta primária

> Dado um orçamento fixo, **em qual papel** do fluxo (planner → implementador → reviewer → QA) a alocação de um modelo mais caro produz o maior retorno em qualidade por token — e esse retorno depende da complexidade da tarefa?

## Índice

| Arquivo | Conteúdo |
|---|---|
| `README.md` | Este índice, resumo executivo, decisões pendentes |
| `01-marco-zero-e-release.md` | Estado atual de produção, plano de release, rollback, congelamento do baseline |
| `02-desenho-experimental.md` | Fatores, níveis, matriz fatorial fracionada, protocolo de execução de um run |
| `03-features.md` | As 3 features, rubrica de complexidade, esqueleto da spec de Contatos |
| `04-metricas.md` | Métricas quantitativas e qualitativas, protocolo de julgamento cego, plano de análise |
| `05-instrumentacao-e-harness.md` | Achados sobre Claude Code, bloqueadores, coleta de tokens, hooks |
| `06-rollout-riscos-e-adocao.md` | Plano de rollout por fases, métricas de adoção, obstáculos e incógnitas |
| `07-prompts-padronizados.md` | Templates de prompt por papel, parâmetros, higiene, vazamento de contexto |
| `specs/SPEC-*.md` | As três specs congeladas — entrada idêntica de todos os runs |

## Resumo executivo

O repositório já possui fluxo multiagente, gates, output contract e histórico de sprints. O que **não** possui, e que este desenho constrói: pinagem de modelo válida por papel, coleta de custo/tempo por papel, e métricas objetivas de qualidade de código.

O experimento roda o mesmo fluxo de desenvolvimento em **3 features de complexidade calculada** (baixa/média/alta), variando qual modelo cada papel usa, e mede custo (tokens), tempo e qualidade.

**Desenho escolhido:** fatorial fracionado `2^(4-1)` resolução IV (8 configurações) na feature de complexidade média para triagem dos efeitos principais, seguido de confirmação com 3 configurações nas features baixa e alta. Total de 34 runs (variante enxuta de 20 decidida após o piloto).

**Unidade de custo: tokens, não moeda.** Consequência metodológica: o resultado se publica como vetor e fronteira de Pareto, não como ranking único. Ver `04-metricas.md` §1.

**Escopo: backend apenas.** Não existe subagente `frontend` no repositório; restringir ao backend elimina esse bloqueador, reduz variância (uma stack só) e mantém o gate de território limpo.

## As três features

| Nível | Feature | Score |
|---|---|---|
| Baixa | Comprovante mal-formado sem `#` não vira pedido silencioso | 2 |
| Média | Contatos — agenda de chaves PIX (backend-only, sem backfill) | 5 |
| Alta | Extração do valor do boleto por código de barras | 7 |

**Fechadas com o humano em 2026-08-10.** Specs congeladas em `specs/`. Detalhamento e scores rastreáveis em `03-features.md`.

⚠️ Nenhuma alcança a faixa "Alta" da rubrica (≥8). Decisão deliberada: o projeto não tem feature de alta complexidade que seja genuinamente útil, e complexidade inventada foi vetada. A faixa estreita (2/5/7) é **limitação declarada**, não erro de seleção.

## Decisões do humano

### Decididas

1. **Estratégia de release: A1 — Big bang.** Decidida em 2026-08-08, contra a recomendação técnica original (A2). Riscos aceitos registrados em `01-marco-zero-e-release.md` §6. O passo a passo de execução está em `01-marco-zero-e-release.md` §4.

2. **As três features.** Fechadas em 2026-08-10. `Contatos` migrou de Alta para Média e perdeu o backfill (tabela `funcionario` vazia em produção). A Alta passou a ser extração de boleto por código de barras — local, determinística, sem custo.

### Pendentes

Nenhum trabalho de Fase 1 em diante deve iniciar antes destas respostas:

3. **Segundo avaliador de complexidade.** A rubrica exige dois avaliadores independentes; os scores atuais têm um só. Bloqueia a Fase 2.
4. **Decidir QA-010 / QA-011** — executar antes do marco zero ou depois do experimento. Executar no meio invalida a comparação.
5. **Vazamento de contexto experimental** — decidir entre aceitar como limitação, mover `docs/experiments/` para fora do repositório durante os runs, ou bloquear por permissão. Ver `07-prompts-padronizados.md` §5.
6. **Marco zero depois do FIX-006.** O deploy `develop → main` está bloqueado por falha de boot em produção; a tag do baseline precisa vir depois dele.

## Ordem obrigatória das fases

```
Fase 0  Marco zero (release + baseline congelado)     ← bloqueante
Fase 1  Pré-requisitos de validade (harness + tooling) ← bloqueante
Fase 2  Specs congeladas + pré-registro versionado     ← gate humano
Fase 3  Piloto (2 runs) → recalibra n
Fase 4  Screening (16 runs, feature média)
Fase 5  Confirmação (18 runs, features baixa e alta)
Fase 6  Avaliação cega
Fase 7  Análise, artigo, publicação de dataset e scripts
```

## Compromissos metodológicos assumidos

- **Pré-registro versionado.** Hipóteses, endpoints primários, rubricas e plano de análise são commitados e tagueados **antes** do primeiro run. O hash é citado na publicação.
- **Publicação do resultado nulo.** Se nenhuma configuração se distinguir do baseline além do ruído (hipótese H4), esse resultado será publicado.
- **Endpoints primários únicos.** Custo = K1/K2 (tokens). Qualidade = Q1 (defeitos escapados em auditoria cega). Todo o resto é secundário ou exploratório.
- **Cegamento.** Auditor e avaliador humano nunca sabem qual configuração produziu o diff que estão avaliando. Desbloqueio só após todos os scores gravados.
- **Nenhum branch experimental é mergeado.** Um run é escolhido a posteriori e reimplementado pelo fluxo normal, com Reviewer e QA.
