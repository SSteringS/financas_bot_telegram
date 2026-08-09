# Instrumentação e achados sobre o harness

> Fonte: Skill `harness-workflow-analyst`, consultada em 2026-08-08.
>
> **Ressalva metodológica da própria Skill:** `docs/claude/` — a fonte de verdade que ela exige — **não existe neste repositório**, embora 9 arquivos a citem. Ela respondeu a partir dos espelhos oficiais em `docs/aprendizado/documentacao-antropic-subagents.md` e `docs/aprendizado/doc-antropic-visao-agente.md`, marcando o restante como não documentado. Itens marcados `UNCONFIRMED` precisam de validação em `code.claude.com/docs` antes de virarem dependência do harness.

## 1. Bloqueadores de validade

| # | Achado | Impacto |
|---|---|---|
| **B1** | `planner`, `backend`, `reviewer` e `qa-test-specialist` — **a cadeia inteira do experimento** — declaram `model: GPT-5.4 (copilot)`, valor inválido para Claude Code (listado explicitamente como campo proibido, formato VS Code Custom Agent) | **Fatal.** Hipótese default: caem em `inherit` → setting da sessão. Todas as condições rodariam o mesmo modelo. **O experimento mediria a mesma célula 34 vezes.** |
| **B2** | **Não existe subagente `frontend`** — só role doc em `docs/roles/frontend.md` | O lane front do fluxo não é executável por agente. Motivo pelo qual o experimento é backend-only |
| **B3** | `handoffs` e `agents` **não são campos da spec** de subagente (são VS Code Custom Agent; o substituto é `SendMessage`) | A orquestração planner→backend→reviewer→qa provavelmente não acontece pelos campos declarados. O grafo de execução não está fixado |
| **B4** | `effort` (`low`/`medium`/`high`/`xhigh`/`max`) **herda da sessão** quando não declarado; nenhum dos 6 agents declara | Segunda variável livre, confunde com o fator modelo |
| **B5** | Built-in **`Explore` roda em Haiku fixo**; `Plan` e `general-purpose` herdam | Se o implementador spawnar Explore, esse trecho da execução sai do controle experimental |
| **B6** | Subagentes em background **auto-negam** tool calls que pediriam permissão. `.claude/settings.json` não existe — só `settings.local.json` com allowlist | Um run falha por permissão e outro não → variância que não é do modelo |
| **B7** | **Zero hooks configurados.** `SubagentStart`/`SubagentStop` existem, aceitam matcher por agent type e recebem `agent_type` no payload. Windows exige `shell: powershell` | Instrumentação de tempo por papel precisa ser construída do zero, mas é viável e documentada |
| **B8** | Subagente nomeado tem **cache de prompt separado** do main — o custo por papel é atribuível. Mas entre réplicas da mesma feature o cache barateia a 2ª rodada | Confounder de custo, mitigável por espaçamento ≥ 90 min e por reportar K4 à parte |
| **B9** | Auto-compactação a ~95% da capacidade, logada como `compact_boundary` com `preTokens` | Feature alta pode compactar e a baixa não → custo não-linear. Detectável, vira covariável T5 |
| **B10** | Delegação por linguagem natural é **não-determinística**; `@-mention` garante que o subagente roda para a task | Grafo precisa ser forçado por @-mention |
| **B11** | `--agents '<JSON>'` define subagentes só para aquela sessão, incluindo `model`, sem salvar em disco | **Lever mais limpo para variar modelo por réplica** sem editar arquivos versionados |

## 2. Estado atual de `.claude/agents/`

| Arquivo | `name` | `model` declarado | `tools` |
|---|---|---|---|
| `ai-engineer.md` | `ai-engineer` | `[opus, sonnet]` (array — `UNCONFIRMED` contra o espelho oficial) | `Read, Grep, Glob, Agent` (formato correto) |
| `backend.agent.md` | `backend` | **`GPT-5.4 (copilot)` — inválido** | `['read','edit','search','execute','agent','todo']` (formato incorreto) |
| `planner.agent.md` | `planner` | **`GPT-5.4 (copilot)` — inválido** | formato incorreto |
| `reviewer.agent.md` | `reviewer` | **`GPT-5.4 (copilot)` — inválido** | formato incorreto |
| `qa-test-specialist.agent.md` | `qa-test-specialist` | **`GPT-5.4 (copilot)` — inválido** | formato incorreto |
| `code-documentation-analyst.agent.md` | `code-documentation-analyst` | `opus` | formato incorreto |

Nenhum declara `effort`, `permissionMode`, `hooks`, `memory`, `skills`, `maxTurns`, `background`, `isolation` ou `mcpServers`.

**Atenção:** o validador do próprio repo (`.claude/skills/creating-agents/scripts/validate_agent.py`) retorna `OK` para os seis, porque foi escrito para aceitar deliberadamente ambos os formatos durante a migração. **`OK` do validador não significa que o modelo está pinado.**

## 3. Onde estão os dados de custo

> Observação empírica em disco, **não contrato documentado**. Exige teste de regressão que falhe se as chaves mudarem.

- Transcript principal: `~/.claude/projects/<proj>/<sessionId>.jsonl`
- Subagentes: `~/.claude/projects/<proj>/<sessionId>/subagents/agent-<agentId>.jsonl`
- Irmão de metadados: `agent-<agentId>.meta.json` contendo `agentType`, `description`, `toolUseId`, `spawnDepth` — **`agentType` é o identificador de papel para o dataset**
- Linhas `type:"assistant"` trazem: `timestamp`, `sessionId`, `uuid`, `parentUuid`, `requestId`, `version`, `gitBranch`, `cwd`, `effort`, `isSidechain`; nos arquivos de subagente somam `agentId` e `attributionAgent`
- **`message.model`** traz o ID resolvido — **é a verificação de que o modelo pinado de fato pegou**
- **`message.usage`** traz: `input_tokens`, `output_tokens`, `cache_creation_input_tokens`, `cache_read_input_tokens`, `cache_creation.{ephemeral_5m_input_tokens, ephemeral_1h_input_tokens}`, `service_tier`, `speed`, `server_tool_use`, e um array `iterations[]` com a mesma quebra por iteração

**Não há campo de duração por mensagem.** Duração = delta de `timestamp` entre a primeira e a última linha. Turnos = contagem de linhas `type:"assistant"`.

## 4. Instrumentação a construir

1. **`.claude/settings.json`** com hooks `SubagentStart` e `SubagentStop`, matcher por agent type, comandos em PowerShell, escrevendo em `run_events.jsonl` — carimbo de papel e wall-clock.
2. **`collect_usage.py`** — varre `<sessionId>.jsonl` e `subagents/*.jsonl`, faz join por `agentId`/`agentType` via `.meta.json`, agrega `usage` por `(agentType, model)` e grava `metrics.json` por run.
3. **Teste de regressão do schema JSONL** — falha se as chaves esperadas sumirem. Obrigatório, porque o formato é observado e não publicado.
4. **Verificador de pin** — compara `message.model` de cada papel com a configuração declarada do run; marca o run como inválido se divergir.

## 5. Tooling de qualidade a instalar

| Ferramenta | Situação atual | Ação |
|---|---|---|
| **JaCoCo** | **Não existe** no `pom.xml`. O comando `./mvnw jacoco:report` que o agente QA é instruído a rodar **falha** | Instalar. Sem isso, Q2 é impossível e `cobertura_pct` continua `na` em 100% dos reports |
| **PIT (pitest)** | Não existe | Instalar. Habilita Q3, o melhor proxy de qualidade de teste |
| **PMD** | Não existe. `lint: na` para backend é oficial no `PRE-MERGE-CHECKLIST` | Instalar ruleset mínimo. Habilita Q6 e Q7 |
| **Cobertura frontend** | `@vitest/coverage-v8` está em devDependencies, mas **não há script `test:coverage` nem bloco `coverage:` em `vite.config.ts`** | Configurar (relevante só se o lane front entrar no futuro) |

## 6. Pontos cegos — validar antes de depender

Nada abaixo deve ser assumido sem antes buscar em `code.claude.com/docs`:

1. **`docs/claude/` inteiro** — a fonte de verdade que 9 arquivos citam não existe
2. **Telemetria OpenTelemetry** — cobertura zero no repo. Variáveis de ambiente, métricas emitidas, granularidade e atributos de subagente/modelo desconhecidos
3. **`/cost` e `/usage`** — existência, campos, granularidade
4. **Schema de saída do headless** — campos de `--output-format json` e `stream-json`; se trazem custo, tokens ou duração
5. **`ANTHROPIC_MODEL`** e a ordem de precedência completa do modelo da sessão principal
6. **Temperatura, seed, qualquer garantia de reprodutibilidade** — não documentados
7. **Lista completa de eventos de hook** — se `SessionEnd` e `PreCompact` existem; schema de payload além de `agent_type`
8. **Se o payload de hook carrega o modelo resolvido** — hoje só `agent_type` é garantido, então o join modelo×papel depende do JSONL
9. **Comportamento com valor de `model` inválido** (o caso `GPT-5.4 (copilot)`) — silencioso? erro? fallback?
10. **Aliases e array de fallback `model: [a, b]`** — em uso em `ai-engineer.md`, ausentes do espelho oficial. `UNCONFIRMED`
11. **Estabilidade do formato JSONL** — observação de disco, não contrato publicado
