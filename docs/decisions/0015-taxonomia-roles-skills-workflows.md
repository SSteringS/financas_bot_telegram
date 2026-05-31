---
adr: 0015
titulo: "Taxonomia roles × skills (e o lugar de workflows)"
data: 2026-05-30
atualizado: 2026-05-31
status: Accepted
decisores: humano-com-arquiteto-e-engenheiro-de-ia
relacionado: [0004, 0005, 0007, 0011]
supersedes: null
superseded_by: null
---

# ADR 0015 — Taxonomia roles × skills (e o lugar de workflows)

> **Refina** ADR 0005 (sessões especializadas por papel) e ADR 0011 (adoção do arquiteto). Promove o item #10 do `docs/plans/BACKLOG-evolucao-workflow.md`. Implementado na sprint 02b (kaizen) pelo engenheiro de IA.
>
> **Atualização 2026-05-31:** reescrita pós-implementação para refletir a realidade do que foi entregue. A versão original descrevia um design intermediário (`docs/skills/` como home das skills, `docs/roles/` como driver de comportamento). O eng-ia implementou no **formato canônico da Anthropic** (`.claude/agents/`, `.claude/skills/`), que é mais limpo e mais alinhado com o tooling. Esta versão registra o estado real.

---

## Contexto

O ADR 0005 criou `docs/roles/<papel>.md` — arquivos-delta de instrução por papel, lidos no boot de cada sessão especializada. Funcionou: separou Reviewer do planner, deu identidade clara a cada agente.

Pós-MVP, três sinais convergentes mostraram que **a unidade "role"** estava começando a sobrecarregar:

1. **Roles misturavam três coisas diferentes.** `reviewer.md` carregava (a) identidade, (b) capacidade técnica ("como ler arquitetura hexagonal"), (c) sequência operacional. Cada dimensão tem público e ciclo de vida diferentes.
2. **Capacidades técnicas são reutilizáveis entre papéis.** "Ler arquitetura hexagonal" não é só do Reviewer — Architect e planner também precisam. Forçar dentro de um role duplica conteúdo.
3. **~70% dos master-prompts de dispatch era boilerplate genérico** (análise de 2026-05-29 dos `MASTER-PROMPT-overnight-*` e `DISPATCH-*`): boot sequence, regras duras, criação de branch, escrita de status. Repetido literal em cada dispatch.

O estudo do humano com o Gandalf (2026-05-30) e o curso *Introduction to Agent Skills* (Anthropic Academy) consolidaram a taxonomia formal. O engenheiro de IA implementou na sprint 02b usando o **formato canônico da Anthropic** para agents e skills.

---

## Decisão

### 1. Taxonomia adotada

Três conceitos com fronteiras explícitas:

- **Agent** (`.claude/agents/<papel>.md`) — *quem o agente é + como é invocado*. Define identidade, território, ferramentas, model, skills disponíveis e `initialPrompt`. **É o driver ativo de comportamento**. Invocado via `--agent` flag na thread principal ou via `Agent()` tool como subagente.
- **Skill** (`.claude/skills/<skill>/SKILL.md`) — *capacidade técnica reutilizável OU densa-mas-rara*, carregada sob demanda por progressive disclosure. Pode ser usada por múltiplos agentes (reuso) ou por um único agente em situação específica (gestão de contexto).
- **Workflow / Runbook** (`docs/runbooks/<nome>.md`) — *sequência operacional repetível*. Runbook já cumpre esse papel (PRE-MERGE-CHECKLIST, PREP-WA, ROTEIRO-TESTES-BACKEND). Critério: runbook = processo organizacional; skill = conhecimento técnico que o agente aplica.

**`docs/roles/<papel>.md`** continua existindo como **documentação de referência para o humano** — descreve o papel em linguagem natural, intenção, território, fronteiras. Não é lido pelo agente como instrução ativa; o driver ativo é `.claude/agents/<papel>.md`.

### 2. Estrutura real de agents e skills

```
.claude/
  agents/
    planner.md           ← agent ativo: tools, model, skills_available, initialPrompt
    backend.md
    frontend.md
    reviewer.md
    architect.md
    engenheiro-de-ia.md
  skills/
    arquitetura-hexagonal/
      SKILL.md           ← skill ativa: frontmatter YAML + corpo
    ciclo-de-sprint/
      SKILL.md
    escrita-de-dispatch/
      SKILL.md
    escrita-de-plano-completo/
      SKILL.md
    ecossistema-spring/
      SKILL.md
    ... (16 skills no total ao encerrar sprint 02b)

docs/
  roles/
    planner.md           ← documentação de referência (humano lê; agente NÃO carrega como instrução)
    backend.md
    ...
  skills/
    README.md            ← spec conceitual: quando criar skill, schema, critérios (humano consulta)
```

### 3. Como agents são invocados

Dois modos de uso:

- **`--agent <papel>`** — lança o agente na **thread principal** da sessão Claude Code. O agente carrega seu `initialPrompt`, tools e skills_available. É o modo padrão pra trabalho de uma sessão dedicada (ex.: `--agent planner` abre o planner, `--agent backend` abre o backend).
- **`Agent()` tool** — invoca um subagente a partir de outro agente (ex.: backend chama reviewer ao fim da implementação). O subagente recebe o prompt da chamada + seu próprio `initialPrompt`.

### 4. Formato do agent file (`.claude/agents/<papel>.md`)

```yaml
---
name: <papel>
description: "Terceira pessoa, descreve o que faz + quando usar. Lido pelo orquestrador pra decidir se invoca."
tools: Read, Write, Edit, Grep, Glob, Bash, AskUserQuestion, ...
model: sonnet
memory: project
skills_available: [skill-1, skill-2, ...]   # skills carregadas sob demanda (progressive disclosure)
initialPrompt: |
  Boot sequence obrigatório + regras específicas do papel
---

# Papel: <Nome>
...corpo em markdown (identidade, faz, não faz, checklist)...
```

- **`description:`** — o orquestrador lê pra decidir qual agente invocar. Precisa ser específico.
- **`skills_available:`** — lista de skills disponíveis. O agente carrega o frontmatter de cada uma no boot; o **corpo** da skill só é lido quando o gatilho do `description` da skill bate (progressive disclosure).
- **`initialPrompt:`** — instrução de boot embutida no agent file. Substitui o dispatch externo pra sessões comuns.

### 5. Formato da skill file (`.claude/skills/<skill>/SKILL.md`)

```yaml
---
name: <skill-kebab-case>
description: "O que a skill faz + quando carregar. Gatilho literal e específico."
allowed-tools: Read, Grep, Glob, Bash
load_pattern: shared | contextual
used_by: [agente-1, agente-2]
created: YYYY-MM-DD
status: ativa | draft | depreciada
---

# Skill — <Nome>

## Quando carregar
...gatilho expandido...

## Resumo da capacidade
...

## Pontos-chave / checklist
...
```

- **`load_pattern: shared`** — ≥2 agentes consomem (motivo: reuso).
- **`load_pattern: contextual`** — 1 agente, mas só em contexto específico (motivo: gestão de contexto). `used_by` tamanho 1 só é válido com `contextual`.
- Limite: ~150 linhas (frontmatter + corpo). Se crescer além, considerar split.
- Pasta por skill (não arquivo flat) — formato canônico Anthropic que permite adicionar arquivos auxiliares se necessário.

### 6. Critério de nascimento de skill (regra do reuso OU regra do contexto raro)

Skill nasce quando **≥1 das duas condições** bate:

- **(a) Reuso** — ≥2 agentes usam a capacidade **ou** ≥2 dispatches/master-prompts repetem o mesmo bloco. `load_pattern: shared`.
- **(b) Contexto raro** — 1 agente usa a capacidade, mas **só em contexto específico** e o conteúdo é **não-trivial** (>30 linhas como heurística). `load_pattern: contextual`.

Anti-pattern: skill que não passa em nenhuma das duas = decoração; o conteúdo fica no agent file.

Migração **forward-only**. Não reescrever agent files existentes. Extrair quando a dor aparecer.

### 7. Feedback loop de skills (como sabemos se está funcionando)

Não há log automático de "skill X foi carregada". Rastreamento é convencional:

- **No dispatch:** lista explicitamente quais skills o agente deve carregar pra task. Aí o acionamento fica rastreável pelo artefato de planejamento.
- **Na retro:** seção fixa `## Agents & Skills` com 3 perguntas:
  1. Qual skill foi mais útil nesta sprint?
  2. Qual falhou ou não foi ativada quando deveria?
  3. Algum padrão se repetiu 2x e virou candidato a skill nova?
- **Métrica de efeito (proxy):** frequência de drift detectado pelo humano que uma skill cobrente deveria ter pego. Se a skill funciona, esse número cai.

Critério de parada: skill sem sinal de utilidade por 2 sprints consecutivas → deletar.

### 8. Métricas de adoção

| Métrica | Alvo | Como medir |
|---|---|---|
| Tamanho médio do agent file | estável ou caindo com skills extraídas | `wc -l .claude/agents/*.md` |
| Tamanho médio do dispatch | ~80 linhas → ~30 | `wc -l docs/sprints/*/plans/DISPATCH-*.md` |
| Drift detectado-pelo-humano coberto por skill | tendência queda | auditoria manual na retro |
| Skills com `load_pattern: shared` e `used_by` tamanho 1 | zero (violação da regra) | grep YAML |
| Skills ativas com >150 linhas | zero (sinal de split necessário) | `wc -l .claude/skills/*/SKILL.md` |

Se em 1 mês: (a) >8 skills acumuladas **e** (b) nenhuma métrica mexeu → parar e auditar. Critério de parada explícito.

---

## Razões

- **Formato canônico da Anthropic** (`.claude/agents/`, `.claude/skills/<skill>/SKILL.md`) garante compatibilidade nativa com Claude Code sem precisar de convenção paralela. O tooling já sabe onde olhar.
- **`--agent` na thread principal** é o mecanismo mais simples pra sessão dedicada — sem dispatch externo, sem ler manualmente o role no boot.
- **`docs/roles/` como documentação** preserva a legibilidade humana sem sobrecarregar o contexto do agente. Humano lê pra entender fronteiras; agente já carrega seu `initialPrompt`.
- **Separação por dimensão cognitiva** (quem × o que × como) bate com a estrutura natural do trabalho.
- **Dois motivos válidos pra skill** (reuso + gestão de contexto) — o curso Anthropic Skills é categórico nos dois. Restringir só ao reuso atropelava o caso da skill densa-mas-rara de uso único.
- **Feedback loop convencional** (dispatch + retro) é realista — não existe log automático de skill carregada; tentar automatizar cria overhead sem ganho.

---

## Consequências

**Positivas:**
- Agent files em `.claude/agents/` são a fonte da verdade de comportamento. Um lugar só pra atualizar tools, model, skills.
- Skills em `.claude/skills/` ficam no formato que o Claude Code já reconhece nativamente.
- `docs/roles/` preservado como documentação sem virar gargalo de manutenção dupla.
- Master-prompts encolhem ao referenciar skills em vez de colar boilerplate.
- Feedback loop de retro + dispatch dá sinal de valor sem overhead de tooling.

**Negativas / custos:**
- Dupla manutenção `docs/roles/` + `.claude/agents/` pra cada papel. Mitigado pela regra: `docs/roles/` não precisa ser atualizado a cada mudança funcional — só quando a descrição conceitual do papel mudar.
- Gatilho de skill exige disciplina no `description` — vago anula o progressive disclosure.
- `docs/roles/` e `.claude/agents/` podem divergir se não houver disciplina de sincronizar quando o papel muda estruturalmente.

---

## Alternativas consideradas

- **`docs/skills/` como home das skills** (design original desta ADR antes da implementação): descartado pelo eng-ia em favor do formato canônico Anthropic. `.claude/skills/` é onde o Claude Code nativo lê skills; colocar em `docs/` exigiria referência explícita em cada dispatch.
- **`docs/roles/` como driver ativo** (status quo pré-implementação): descartado — o `initialPrompt` embutido no `.claude/agents/` é mais direto e alinhado com o mecanismo nativo. Manter `docs/roles/` como doc é o melhor dos dois mundos.
- **Arquivo flat `<skill>.md`** em vez de pasta `<skill>/SKILL.md`: descartado em favor do formato canônico Anthropic (pasta por skill permite arquivos auxiliares).
- **Só `--agent`, sem `Agent()` tool**: os dois coexistem — `--agent` pra thread principal, `Agent()` pra subagentes (ex.: backend chama reviewer).
- **Log automático de skills carregadas**: sem suporte nativo no Claude Code; overhead de instrumentação não justifica o ganho. Convencional (dispatch + retro) cobre.

---

## Referências

- ADR 0005 (sessões especializadas) · ADR 0007 (frontmatter YAML) · ADR 0011 (adoção do arquiteto) — esta ADR refina sem revogar.
- `docs/plans/BACKLOG-evolucao-workflow.md` itens #8 e #10 (inputs originais).
- `.claude/agents/` — agent files ativos (fonte da verdade de comportamento por papel).
- `.claude/skills/` — skills ativas (16 ao encerrar sprint 02b).
- `docs/skills/README.md` — spec conceitual (quando criar skill, schema, critérios).
- `docs/roles/` — documentação de referência humana por papel.
- `docs/aprendizado/taxonomia-agent-skill-workflow.md` — conceito formativo.
- `docs/aprendizado/curso-anthropic-agent-skills.md` — notas do curso Introduction to Agent Skills (Anthropic Academy).
- Estudo com Gandalf (ChatGPT, 2026-05-30) — origem da taxonomia formal.
