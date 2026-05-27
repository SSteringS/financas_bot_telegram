# ADR 0005 — Sessões especializadas por papel (`docs/roles/`)

**Data:** 2026-05-26
**Status:** Aceito
**Decisores:** humano (com ajuda do Claude planejador)
**Relacionado:** estende o ADR `0004` (governança do workflow)

---

## Contexto

O ADR 0004 fixou a topologia (planejador + implementadores + humano integrador) e decidiu por **verificação independente** em vez de pipeline fixa de papéis. Faltava o **mecanismo concreto** dessa separação.

Auditoria com ChatGPT (2026-05-26) apontou que o planejador acumula coordenação + arquitetura + documentação + revisão + validação + QA, gerando **diluição de contexto, viés de confirmação e revisão rasa** conforme o contexto cresce. A proposta: especializar **sessões** (não modelos) via arquivos de instrução por papel.

Nuance técnica do Claude Code considerada: `CLAUDE.md` é carregado automaticamente (memória global); `.claude/` tem subdiretórios reservados (`agents/`, `commands/`, `settings`). Subagents nativos (`.claude/agents/`) rodam *dentro* de uma sessão — mas o setup real é **multi-sessão** (planejador no desktop, back/front no IntelliJ). Logo, arquivos de papel lidos por sessões novas encaixam melhor agora; subagents nativos ficam pra evolução.

---

## Decisão

### 1. Papéis vivem em `docs/roles/`

Arquivos de instrução por papel em `docs/roles/`. Motivo da escolha sobre `.claude/roles/`: (a) o planejador é o mantenedor desses arquivos e a sessão de planejamento (Cowork) **não consegue escrever em `.claude/`** (caminho protegido pelo Claude Code); (b) os arquivos são **lidos sob demanda** (a sessão recebe a instrução de ler), então a localização é funcionalmente indiferente; (c) `docs/` já é a área de conhecimento compartilhada. `.claude/` fica **reservado** pra subagents nativos (`.claude/agents/`) no futuro.

Papéis iniciais: `planner.md`, `backend.md`, `frontend.md`, `reviewer.md`. `architect.md` e `qa.md` ficam **adiados** (arquitetura cabe no planner; QA cabe no checklist do implementador + reviewer) até a dor justificar.

### 2. Arquivo de papel é DELTA, nunca cópia

**`CLAUDE.md` é a fonte única das regras globais.** Cada arquivo de papel contém só o *delta* do papel — objetivo, o que faz / o que NÃO faz, restrições específicas, checklist do papel, e **referências** aos docs canônicos (`CLAUDE.md`, `PRE-MERGE-CHECKLIST.md`, ADRs). **Proibido copiar** regra que já está no CLAUDE.md ou nos canônicos — referencia. Isso evita divergência.

### 3. Bootstrap de sessão

Uma sessão vira agente especializado lendo **`CLAUDE.md` + `docs/roles/<papel>.md` + o doc da task**. Ex.: *"Leia CLAUDE.md e docs/roles/reviewer.md. Você atuará como Reviewer."*

### 4. O Reviewer é separado do Planner

A maior mudança: a **revisão sai das mãos do planejador**. O Reviewer é uma sessão **separada**, deliberadamente adversarial, que verifica a entrega **contra o código/testes reais**, não contra o status report. É o mecanismo concreto da "verificação independente" do ADR 0004.

### 5. Rollout incremental

Formalizar primeiro os 3 papéis que já existem (planner/backend/frontend) — só escrever o comportamento atual. Depois adicionar o Reviewer (o ganho real). Segurar architect/qa.

---

## Razões

- Separação cognitiva por sessão ataca diretamente diluição de contexto e viés de confirmação.
- Arquivo-delta + referência mantém DRY e evita o anti-padrão de role files que divergem do CLAUDE.md.
- Multi-sessão (arquivos de papel) encaixa no setup real; subagents nativos seriam force-fit agora.
- Reviewer separado é o ponto de maior retorno — não a quantidade de papéis.

---

## Consequências

**Positivas:**
- Revisão mais independente e crítica; menos viés do planejador.
- Sessões focadas, reproduzíveis ("leia X + papel Y").
- Terreno preparado pra subagents nativos no futuro.

**Negativas:**
- Mais uma superfície de persistência (`docs/roles/`) que pode divergir — mitigado pela regra "delta + referência".
- A separação só funciona com disciplina do humano em **abrir a sessão certa pro trabalho certo** (usar a sessão "backend" pra revisar anula o ganho).
- Custo de manter os arquivos de papel atualizados quando o CLAUDE.md/canônicos mudam (minimizado por não duplicar).

---

## Alternativas consideradas

- **Subagents nativos (`.claude/agents/`) agora:** adiado. Pressupõe orquestração dentro de uma sessão; o setup é multi-sessão. Vira evolução futura pro Reviewer/Critic on-demand.
- **Os 6 papéis de uma vez (planner/backend/frontend/reviewer/architect/qa):** descartado. É a pipeline fixa que o ADR 0004 já recusou; cria complexidade sem retorno no estágio atual.
- **Manter tudo no planner:** descartado — é o problema que motivou a decisão.

---

## Referências

- ADR `0004` (governança do workflow e taxonomia)
- `docs/runbooks/PRE-MERGE-CHECKLIST.md`, `docs/aprendizado/structured-outputs.md`
- `docs/roles/*.md` (arquivos de papel criados nesta decisão)
- Proposta de auditoria com ChatGPT, 2026-05-26
