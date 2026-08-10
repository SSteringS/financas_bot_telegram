# Métricas, protocolo de julgamento e plano de análise

## 1. Unidade de custo: tokens, não moeda

A decisão de medir custo em **tokens** tem uma consequência metodológica que fortalece a publicação, desde que assumida explicitamente:

**Token de Opus e token de Sonnet não são a mesma unidade.** Somar os dois produz um número sem significado como "custo". Portanto:

- O custo se publica como **vetor**, não como escalar único.
- Comparações **dentro do mesmo papel, entre configurações**, são válidas: mesmo papel, modelo diferente → diferença de volume de tokens é diferença de verbosidade e trabalho.
- Comparação de **total entre configurações** exigiria um peso — e esse peso é, necessariamente, preço.
- O resultado se apresenta como **fronteira de Pareto**, com o mix de modelos como rótulo de cada ponto. Quem lê aplica a própria tabela de preço.

Isso torna o resultado **durável**: preço muda a cada trimestre, contagem de token não.

## 2. Métricas quantitativas — coleta automática

### 2.1 Custo (tokens)

| ID | Métrica | Definição | Comparável entre configs? |
|---|---|---|---|
| **K1** | **Tokens por (papel, modelo, classe)** — endpoint primário de custo | `input_tokens`, `output_tokens`, `cache_creation_input_tokens`, `cache_read_input_tokens` agrupados por `agentType` e `message.model` | **Vetor.** Nunca somado entre modelos diferentes |
| **K2** | **Tokens de saída totais do run** | Σ `output_tokens` de todos os papéis | **Sim** — escalar legítimo, mede volume gerado, é agnóstico de modelo |
| **K3** | Tokens de entrada não-cacheados | `input_tokens` + `cache_creation_input_tokens` | Sim — mede trabalho real de contexto |
| **K4** | Tokens lidos de cache | `cache_read_input_tokens` | Covariável. **Sempre reportado à parte**, nunca somado sem rótulo |
| **K5** | Share de tokens de saída por papel | K2 do papel ÷ K2 total | Testa H3 |

Fonte: JSONL de subagente em `~/.claude/projects/<proj>/<sessionId>/subagents/agent-<agentId>.jsonl`, com join por `agentType` via o arquivo irmão `.meta.json`.

### 2.2 Tempo e esforço

| ID | Métrica | Definição | Fonte |
|---|---|---|---|
| **T1** | Wall-clock por papel | `SubagentStop − SubagentStart` | hook |
| **T2** | Lead time total do run | primeiro → último evento | hook / JSONL |
| **T3** | Turnos por papel | contagem de linhas `type:"assistant"` | JSONL |
| **T4** | Tool calls por papel e por tool | contagem | JSONL |
| **T5** | Eventos de compactação | contagem de `compact_boundary` + `preTokens` | JSONL — **covariável** |

### 2.3 Processo

| ID | Métrica | Definição | Fonte |
|---|---|---|---|
| **P1** | Gates de primeira tentativa | build/lint/test `ok` sem retry | hook `PostToolUse` |
| **P2** | Rodadas de reviewer | nº de ciclos até `aprovado` | **campo novo** `rounds_reviewer` no status report |
| **P3** | Rodadas de QA | idem | **campo novo** `rounds_qa` |
| **P4** | Achados críticos do reviewer | itens `CRITICO` na avaliação | `avaliacoes/` |
| **P5** | Intervenções humanas não previstas | contagem, log padronizado | operador |

### 2.4 Qualidade

| ID | Métrica | Definição | Fonte |
|---|---|---|---|
| **Q1** | **Defeitos escapados — endpoint primário de qualidade** | Defeitos encontrados por auditor independente **cego** no diff final, que nenhum papel do run flagrou | auditoria pós-hoc |
| **Q2** | Cobertura de linha e branch nas classes tocadas | Δ vs commit base | **JaCoCo (a instalar)** |
| **Q3** | **Mutation score** nas classes tocadas | mutantes mortos ÷ gerados | **PIT (a instalar)** — proxy de qualidade de teste muito superior a cobertura |
| **Q4** | Critérios de aceitação satisfeitos | binário por critério da spec congelada, **verificado executando** | script + verificação humana |
| **Q5** | Tamanho do diff | LOC +/−, arquivos, produção vs teste | `git diff --numstat` |
| **Q6** | Complexidade ciclomática média **nas classes tocadas** | granularidade de **classe**, não de método (decisão D2) · reportada separada para produção e teste | **PMD (a instalar)** |
| **Q7** | Violações de ruleset **nas classes tocadas** | contagem, Δ vs commit base · reportada separada para produção e teste | **PMD (a instalar)** — Checkstyle descartado |
| **Q8** | Fabricação de contrato | nº de campos ou comportamentos de API afirmados sem fonte rastreável | auditoria, usando os evidence labels `confirmed`/`inferred`/`unknown` da skill `artifact-report-contract` |
| **Q9** | Violação de território ou convenção de branch | gate | `PRE-MERGE-CHECKLIST` |

## 3. Métricas qualitativas — estruturadas, cegas, com escala

Qualitativo aqui **não** significa impressionista: significa julgamento humano registrado em rubrica ordinal, com concordância entre avaliadores medida.

| ID | Dimensão | Instrumento | Quem avalia |
|---|---|---|---|
| **R1** | Conformidade arquitetural — portas/adapters, direção de dependência, vazamento de infra no domínio | Likert 1–5, âncoras descritas por nível | Humano + juiz LLM |
| **R2** | Significância dos testes — asserção de comportamento vs. tautologia / mock-espelho | Likert 1–5 | Humano + juiz LLM |
| **R3** | Legibilidade, nomeação, coesão | Likert 1–5 | Humano + juiz LLM |
| **R4** | Qualidade do **plano** — completude, decomposição, testabilidade, riscos identificados | 4 sub-escalas 1–5 | Humano + juiz LLM |
| **R5** | Profundidade da **revisão** — verificou contra a realidade? adversarial? acionável? | 3 sub-escalas 1–5 | Humano + juiz LLM |
| **R6** | Taxonomia de falha | Codificação categórica de cada defeito: `premissa-errada` · `contrato-inventado` · `teste-ausente` · `violacao-arquitetural` · `gate-falsificado` · `escopo-extrapolado` | Humano |
| **R7** | Notas de fricção | Log livre por run — vira a seção narrativa da publicação | Operador |

## 4. Protocolo de julgamento

É aqui que a publicação ganha ou perde credibilidade.

- **Cegamento.** Auditor e avaliador humano recebem `run_id` anonimizado (ex.: `run-a7f3`), diff e spec — **nunca** a configuração de modelos. Branches renomeadas, artefatos limpos de qualquer menção a modelo. Desbloqueio apenas **depois** de todos os scores gravados.
- **Ordem aleatória** de apresentação dos diffs.
- **Dois avaliadores.** Humano e juiz LLM. Reportar concordância com **Krippendorff's α** para escalas ordinais. O juiz LLM só entra na análise se **α ≥ 0,6** contra o humano numa subamostra de 30%; abaixo disso, os scores do juiz viram material descritivo, não evidência.
- **Viés de auto-preferência.** Um juiz Opus favorece código escrito por Opus. Mitigação: o juiz deve ser de **fora do conjunto comparado** (outra família de modelo) ou, no mínimo, dois juízes de famílias diferentes com a divergência reportada abertamente. Isso vira seção da publicação, não nota de rodapé.
- **Endpoints primários únicos e pré-declarados:** custo = **K1/K2**, qualidade = **Q1**. Todo o resto é secundário ou exploratório. **Sem métrica composta ponderada arbitrariamente** — o resultado se apresenta como fronteira de Pareto. Esta é a defesa contra acusação de seleção conveniente de métrica.

## 5. Plano de análise — pré-registrado

- **Custo e tempo:** variância baixa. Efeitos principais do fatorial fracionado com intervalo de confiança bootstrap 95%. Suportam afirmação com n = 2–3.
- **Qualidade:** variância alta, n pequeno → **declarar exploratório**. Reportar tamanhos de efeito com IC, **nunca p-valores como prova**. Afirmações no formato "consistente com" / "não distinguível do ruído".
- **Poder:** calculado **após o piloto**, com o desvio-padrão observado. Regra pré-registrada: escolher n tal que a meia-largura do IC de **K2** fique ≤ 15% da média.
- **Mediação:** o efeito do planner é mediado pela qualidade do plano (R4). Reportar R4 como variável intermediária explica *por que* o planner importa — ou não.
- **Covariáveis de controle:** T5 (compactação), K4 (tokens de cache), ordem do run.

## 6. Apresentação do resultado

Fronteira de Pareto em três eixos, com o mix de modelos como rótulo de cada ponto:

```
tokens de saída (K2)  ×  tempo (T2)  ×  qualidade (Q1)
```

Tabelas de apoio: vetor K1 completo por papel e modelo · share K5 · rodadas P2/P3 · mutation score Q3 · rubricas R1–R5 com IC.
