---
adr: 0015
titulo: "Taxonomia roles × skills (e o lugar de workflows)"
data: 2026-05-30
status: Proposed
decisores: humano-com-arquiteto
relacionado: [0004, 0005, 0007, 0011]
supersedes: null
superseded_by: null
---

# ADR 0015 — Taxonomia roles × skills (e o lugar de workflows)

> **Refina** ADR 0005 (sessões especializadas por papel) e ADR 0011 (adoção do arquiteto). Promove o item #10 do `docs/plans/BACKLOG-evolucao-workflow.md`. Materializado em `docs/skills/README.md` (spec da nova pasta) e refletido no `docs/roles/engenheiro-de-ia.md` (primeiro role escrito já com `skills:` e `skills_available:` no frontmatter).

---

## Contexto

O ADR 0005 criou `docs/roles/<papel>.md` — arquivos-delta de instrução por papel, lidos no boot de cada sessão especializada. Funcionou: separou Reviewer do planner, deu identidade clara a cada agente.

Pós-MVP, três sinais convergentes mostram que **a unidade "role"** está começando a sobrecarregar:

1. **Roles misturam três coisas diferentes.** `reviewer.md` hoje carrega (a) identidade ("quem é o Reviewer, postura"), (b) capacidade técnica ("como ler arquitetura hexagonal e detectar smells" — item #8 do backlog quer adicionar isso), (c) sequência operacional ("ordem dos checks numa revisão"). Cada dimensão tem público e ciclo de vida diferentes — inflar uma faz crescer todas.
2. **Capacidades técnicas são reutilizáveis entre papéis.** "Ler arquitetura hexagonal" não é só do Reviewer — o Architect usa quando desenha, o planner usa quando aprova plano que toca camadas. Forçar dentro de um role limita reuso e duplica conteúdo se vários papéis precisarem.
3. **~70% dos master-prompts de dispatch é boilerplate genérico** (item #10 do backlog, análise de 2026-05-29 dos `MASTER-PROMPT-overnight-*` e `DISPATCH-*`): boot sequence, regras duras de implementador, criação de branch, escrita de status, validação local. Repetido literal em cada dispatch. Atualizar o protocolo exige mexer em N lugares.

O estudo do humano com o Gandalf (IA de estudo, ChatGPT, 2026-05-30) propôs taxonomia formal **Agent / Role / Skill / Workflow** baseada no curso oficial da Anthropic sobre Subagents. O curso *Introduction to Agent Skills* (Anthropic Academy, lido em 2026-05-30 — ver `docs/aprendizado/curso-anthropic-agent-skills.md`) consolidou dois motivos pra existência de skill: **reuso** ("stop repeating yourself") **e** **gestão de contexto** ("progressive disclosure to keep context windows efficient" — carrega só quando o gatilho bate, mesmo que o consumidor seja único). Esta ADR adota o núcleo da taxonomia, calibrada pro nosso setup multi-sessão, incorporando ambos os motivos.

---

## Decisão

### 1. Taxonomia adotada

Três conceitos persistidos no repo, com fronteiras explícitas:

- **Role** (`docs/roles/<papel>.md`) — *quem o agente é*. Identidade, território, postura, o que faz / o que NÃO faz, checklist do papel. Continua como ADR 0005 definiu.
- **Skill** (`docs/skills/<skill>.md`) — *capacidade técnica reutilizável OU densa-mas-rara*, carregada sob demanda. Pode ser usada por múltiplos roles (caso de reuso) ou por um único role em situação específica (caso de gestão de contexto). **Novo** nesta ADR.
- **Workflow / Runbook** (`docs/runbooks/<nome>.md`) — *sequência operacional repetível*. **Não criar pasta nova** `docs/workflows/`. Runbook já cumpre esse papel (PRE-MERGE-CHECKLIST, PREP-WA, ROTEIRO-TESTES-BACKEND). Critério: runbook = processo organizacional + agentes; skill = conhecimento técnico que o agente aplica.

### 2. Mapeamento Role/Sessão/Subagent (calibragem importante)

O Gandalf trata "role" e "agent" como equivalentes. **No nosso setup, não são.** Três níveis distintos:

- **Role** = conceito (Reviewer, Backend, Architect).
- **Sessão especializada** = nossa implementação **atual**. Sessão Claude (Cowork desktop / Claude Code no IntelliJ) lendo `CLAUDE.md` + `docs/roles/<papel>.md` + doc da task no bootstrap. ADR 0005 §1 justificou.
- **Subagent nativo** (`.claude/agents/<papel>.md`, invocado via Task tool dentro de uma sessão) = implementação **futura possível**, não substituto. Cowork não escreve em `.claude/`, setup é multi-sessão, role é lido sob demanda. Sem urgência de migrar.

Esta ADR mantém o status quo do ADR 0005 §1 nesse ponto. Subagent nativo continua reservado pra evolução.

### 3. Estrutura de `docs/skills/`

Flat (sem sub-pastas). Um arquivo por skill em kebab-case. README na pasta com índice + convenções (espelho do `docs/aprendizado/`).

```
docs/skills/
  README.md                            ← índice + schema + convenções (a spec desta ADR)
  leitura-arquitetura-hexagonal.md     ← skill piloto (extraída do item #8 do backlog)
  agent-bootstrap.md                   ← (próxima, se as métricas justificarem)
  escrita-status-report.md             ← (próxima, se as métricas justificarem)
  criar-branch-worktree.md             ← (próxima, se as métricas justificarem)
  validacao-local-por-stack.md         ← (próxima, se as métricas justificarem)
```

Cada SKILL.md tem **frontmatter YAML obrigatório** seguido do corpo em markdown. Isso espelha o padrão do **Anthropic Skills system** (campos canônicos `name` + `description` são lidos pra decidir trigger via progressive disclosure) e alinha com a ADR 0007 (nossos status reports já usam frontmatter YAML).

Campos do frontmatter (detalhe completo em `docs/skills/README.md`):

- **`name`** *(obrigatório, canônico Anthropic)* — kebab-case, idêntico ao nome do arquivo sem `.md`.
- **`description`** *(obrigatório, canônico Anthropic)* — terceira pessoa, descreve **o que faz + quando carregar**. Esta string é o gatilho lido pelo agente; precisa ser específica.
- **`load_pattern`** *(obrigatório, extensão nossa)* — **`shared`** (≥2 consumidores, motivo reuso) **ou** **`contextual`** (1 consumidor, motivo gestão de contexto — carrega só em situação específica). Veta `always` (se sempre carrega, fica no role). Esta classificação é o que governa a regra do 2x revisada (§4).
- **`used_by`** *(obrigatório, extensão nossa)* — lista de roles que carregam a skill. Tamanho 1 só é válido com `load_pattern: contextual`.
- **`created`**, **`adr`**, **`status`** — rastreabilidade e ciclo de vida.

Corpo do markdown (estrutura padrão): "Quando carregar" (expande o `description`), "Resumo da capacidade", "Pontos-chave / checklist", "Exemplos" (opcional), "Ler junto" (opcional). Limite duro: ~150 linhas (frontmatter + corpo).

### 4. Critério de nascimento (regra do reuso OU regra do contexto raro)

Esta ADR **expande** a regra do 2x original. Skill nasce quando **≥1 das duas condições** bate:

- **(a) Reuso** — ≥2 roles usam a capacidade **ou** ≥2 dispatches/master-prompts repetem o mesmo bloco de instrução. `load_pattern: shared`. Exige `used_by` ≥ 2.
- **(b) Contexto raro** — 1 role usa a capacidade, mas **só em contexto específico** (não em toda invocação do role), **e** o conteúdo é **não-trivial** (>30 linhas como heurística inicial, calibrar com o piloto). `load_pattern: contextual`. Aceita `used_by` = 1 desde que o `description` deixe o gatilho **literal e específico** ("carregar quando task toca camada X").

Anti-pattern continua proibido: skill com tudo isso falhando = decoração; fica no role.

**Operacionalização do "≥2 dispatches"** (motivo da regra (a)): a contagem precisa ser **citada literalmente no `description` da skill** (`"... carregar quando ... (extraído de DISPATCH-X, role Y)"`). Sem citação explícita, a skill não conta esses dispatches como consumidores — vira `used_by` tamanho 1 e cai no critério (b) `contextual`, ou viola a regra do 2x.

Migração **forward-only**. Não reescrever roles existentes. Quando uma seção de role.md virar pesada (ex.: item #8 do backlog) **e** outro papel também precisar dela (caso (a)) **ou** ela só ser usada em situação específica e for densa (caso (b)), extrair pra skill.

### 5. Carregamento de contexto (gatilho explícito) — fonte da verdade no frontmatter do role

Cada role.md declara skills no **frontmatter YAML** do próprio role, seguindo o padrão Anthropic Skills + Claude Code subagent:

```yaml
---
name: <papel>
description: ...
tools: ...
skills: [<skill-always-on-1>, ...]              # carregadas no bootstrap (= padrão Anthropic canônico)
skills_available: [<skill-on-demand-1>, ...]    # disponíveis pra carregar via progressive disclosure
---
```

- **`skills:`** alinha com o canônico Anthropic (`skills:` no frontmatter de subagent é literalmente esse). Quando o role virar subagent nativo, esse campo migra direto sem ajuste.
- **`skills_available:`** é extensão nossa (mesma classe que `used_by` na skill). Lista skills cujo metadata (frontmatter) o agente lê no boot; o **corpo da skill** só carrega quando o gatilho do `description` bate (progressive disclosure manual). Quando virar subagent nativo, este campo vira documentação ou regra de instrução no corpo (subagent nativo não tem on-demand interno).

O corpo do role tem seção `## Skills` que **explica o porquê** de cada carregamento (gatilho esperado, motivação), mas a **lista canônica é o frontmatter** — atualiza num lugar só, parseável por script.

Esta é a versão manual do progressive disclosure do Skills system da Anthropic: por default, só metadata (frontmatter da skill) é considerada; corpo expande quando triggered. A fonte da verdade do gatilho é o `description` da própria skill — atualiza num lugar só.

### 6. Métricas de adoção (decidir baseline ANTES de migrar)

Critério de "deu certo" pré-acordado, pra evitar adotar por estética:

| Métrica | Alvo | Como medir |
|---|---|---|
| Tamanho médio do role file | cair ≥20% após extrair 3 skills | `wc -l docs/roles/*.md`, baseline hoje vs 1 mês |
| Tamanho médio do master-prompt | ~80 linhas → ~30 | `wc -l docs/sprints/*/plans/MASTER-PROMPT-*.md` e `DISPATCH-*.md` |
| Drift detectado-pelo-humano que role/skill já cobriam | tendência queda | proxy: nº de `desvios` em status reports cobertos por texto em role/skill — auditoria manual mensal |
| Skills duplicadas em 2+ roles | zero | grep cruzado role × skill, ou check manual na revisão de skill nova |
| Skills com `used_by` tamanho 1 **e** `load_pattern: shared` | zero (sinal de violação da regra do reuso) | parser YAML do frontmatter (extensão do `metricas_status.py`) |
| Bootstrap de sessão percebido | "menos correção inicial" | informal, anota no caderno do humano |

Skills com `used_by` tamanho 1 **e** `load_pattern: contextual` **não são violação** — são exatamente o caso da gestão de contexto da regra (b).

Se em 1 mês: (a) acumulamos >8 skills **e** (b) nenhuma métrica mexeu → **paramos e auditamos**. Critério de parada explícito.

### 7. Skill piloto

**`docs/skills/leitura-arquitetura-hexagonal.md`** — extraída do que viraria o item #8 do backlog (smells de violação de dependência hexagonal, descoberto na BE-19a). É a melhor candidata porque:

- Tem dor concreta documentada (BE-19a flagrou `MensagemProcessadaService` dependendo de `JdbcTemplate`).
- Cabe no schema sem inflar (~80-120 linhas estimadas).
- **Nasce como `load_pattern: contextual`** — só o Reviewer usa por ora, mas só em tasks que tocam `application/` ou `infra/` (gatilho específico). Não é toda revisão que precisa.
- **Migrará pra `load_pattern: shared`** quando Architect adotar (cenário esperado quando a sprint trouxer mais tasks de camadas).

Demonstra **as duas faces da regra revisada**: nasce sob (b), evolui pra (a). Documenta a migração na própria skill (campo `status` + nota).

Resto das 4 skills propostas (#2–#5 da seção 3) ficam pra **depois do piloto bater as métricas** — não escalar antes de medir.

---

## Razões

- **Separação por dimensão cognitiva** (quem × o que × como) bate com a estrutura natural do trabalho. É a mesma lógica que motivou separar Reviewer do planner (ADR 0005) e Architect do planner (ADR 0011), só num nível de granularidade menor.
- **Dois motivos válidos pra skill** (reuso + gestão de contexto) — o curso *Introduction to Agent Skills* da Anthropic é categórico nos dois. Restringir a regra do 2x só ao reuso atropelava o segundo motivo legítimo (progressive disclosure pra capacidade densa-mas-rara).
- **Reuso real** — capacidades como "ler arquitetura hex" servem múltiplos papéis. Forçar no role copia conteúdo ou priva o papel da capacidade.
- **Análogo a context engineering / RAG** — skill carregada sob demanda evita inflar o role base. A intuição do Gandalf bate com o que a Anthropic publicou sobre o Skills system (progressive disclosure).
- **Frontmatter YAML alinha com padrão Anthropic + ADR 0007** — mesma forma já usada em status reports; permite tooling (extensão do `metricas_status.py`) sem retrabalho.
- **Sem inventar workflows/** — já temos runbook fazendo o papel. Não criar 5ª categoria sobreposta.
- **Regra revisada + métricas + parada** controla overengineering. Sem isso, a separação vira fim em si.
- **Forward-only** preserva o investimento em roles existentes. Migração nasce de dor, não de bonito.

---

## Consequências

**Positivas:**
- Roles ficam enxutos (identidade + delta operacional), skills crescem por demanda.
- Master-prompts encolhem ao referenciar skills/runbooks em vez de colar boilerplate.
- Capacidades técnicas (ex.: arquitetura hex) ganham home de primeira classe, citáveis por qualquer papel.
- Skills densas-mas-raras (uso único em contexto específico) deixam de inflar o role base — caso da gestão de contexto fica coberto.
- Frontmatter YAML padroniza metadata, permite scripts (futuros) lerem skill sem parsear markdown.
- Métricas pré-definidas dão sinal claro pra continuar/parar — não é fé.

**Negativas / custos:**
- Mais uma superfície de persistência (`docs/skills/`). Mitigado pelas duas regras (a)/(b) com critérios distintos.
- Gatilho de skill exige disciplina nas descrições — `description` ruim (vago) anula o progressive disclosure.
- Critério runbook vs skill pode ficar borrado em casos limítrofes — revisar caso a caso, registrar precedentes no `docs/skills/README.md`.
- Skill `contextual` exige disciplina extra: gatilho do `description` precisa ser **literal**, senão vira lixo on-demand mal disparado.
- Spec da skill (schema) é nova convenção — exige iteração nas primeiras 2–3 skills antes de estabilizar.

---

## Alternativas consideradas

- **Manter tudo no role (status quo):** descartado — é a dor que esta ADR endereça (item #8 + item #10 do backlog).
- **Regra do 2x estrita (só reuso ≥2 consumidores):** descartado durante revisão — atropela o motivo "gestão de contexto" do curso Anthropic Skills. Capacidades densas-mas-raras de 1 role ficariam inline e pesariam toda invocação do role.
- **Criar `docs/workflows/` como 4ª categoria:** descartado. Sobrepõe com `docs/runbooks/`. Critério runbook=processo / skill=capacidade já cobre.
- **Migrar pra `.claude/agents/` (subagents nativos):** descartado por ora. ADR 0005 §1 explicou — Cowork não escreve em `.claude/`, setup é multi-sessão, role é lido sob demanda. Subagent nativo fica reservado.
- **Skill sem frontmatter YAML (só markdown):** descartado. Quebra alinhamento com Anthropic Skills system + ADR 0007, e impede tooling sem parser de markdown.
- **Reescrever role files inteiros nesta migração:** descartado. Forward-only é mais barato e respeita o que já funciona.
- **Adotar todas as 5 skills propostas no item #10 de uma vez:** descartado. Piloto primeiro, mede, escala.

---

## Referências

- ADR 0005 (sessões especializadas) · ADR 0007 (status report com frontmatter YAML) · ADR 0011 (adoção do arquiteto) — esta ADR refina sem revogar.
- `docs/plans/BACKLOG-evolucao-workflow.md` item #10 (input de 2026-05-29 com análise dos dispatches) e item #8 (skill piloto sai daí).
- `docs/skills/README.md` (spec materializada por esta decisão).
- `docs/roles/engenheiro-de-ia.md` (primeiro role escrito já com `skills:` e `skills_available:` no frontmatter — aplicação da §5).
- `docs/aprendizado/taxonomia-agent-skill-workflow.md` (conceito formativo da discussão).
- `docs/aprendizado/curso-anthropic-agent-skills.md` (notas do curso Introduction to Agent Skills da Anthropic Academy — fonte dos dois motivos pra skill nascer).
- Estudo com Gandalf (ChatGPT, 2026-05-30) — origem da taxonomia formal.
- Anthropic Skills system (frontmatter `name` + `description`, progressive disclosure, `skills:` no subagent) — análogo conceitual e fonte do schema canônico.
