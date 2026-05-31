---
# ─── Frontmatter (schema obrigatório — parseável) ───
adr: 0016                          # int — número sequencial
titulo: "Camada subagent reviewer (Reviewer-sub + Reviewer externo)"
data: 2026-05-30                   # YYYY-MM-DD
status: Proposed                   # Proposed | Accepted | Rejected | Superseded | Deprecated
decisores: humano                  # quem homologa (humano | humano-com-arquiteto | etc.)
relacionado: [0004, 0005, 0011, 0015]   # ADRs relacionados (não-superseding)
supersedes: null                   # null | <NNNN> se esta ADR substitui outra
superseded_by: null                # null | <NNNN> se foi substituída
---

# ADR <NNNN> — Título

> **Não edite este arquivo.** Copie pra `<NNNN>-<slug>.md` na pasta `docs/decisions/`, preencha o frontmatter e as seções abaixo. Forward-only: ADRs antigos (sem frontmatter) ficam como estão.

---

## Contexto

O problema que motiva a decisão. Estado atual do repo/workflow. Sinais que mostram a dor (drift observado, conversa que recorreu, métrica que piorou). Restrições já dadas (ADRs anteriores, decisões do humano, infra fixa). **Não é "decisão" ainda** — só descrição do que existe e o que pressiona.

---

## Decisão

O que se decide, em linguagem direta. Para decisões maiores, subdividir em sub-blocos numerados:

### 1. Sub-decisão 1
### 2. Sub-decisão 2
### 3. Sub-decisão 3

Cada sub-decisão é auto-contida e materializada em algum lugar do repo (arquivo novo, mudança de regra, métrica). Se uma sub-decisão depende de outra, deixar explícito.

---

## Razões

Por que esta decisão e não outra. Bullets curtos com a lógica. Cita princípios canônicos do projeto quando aplicável (ADR 0004 sobre persistência, ADR 0005 sobre independência, ADR 0007 sobre sintaxe ≠ semântica).

---

## Consequências

**Positivas:**
- O que melhora.
- Que dor essa decisão fecha.

**Negativas / custos:**
- O que piora ou complica.
- Que disciplina nova exige.
- Que ambiguidade nova introduz.

**Métricas pra avaliar adoção** (opcional mas recomendado — ADR 0015 §6 estabeleceu o princípio):
- Baseline + alvo + critério de parada explícito. Sem isso, decisão vira fé.

---

## Alternativas consideradas

Cada alternativa em 1-3 linhas com razão de descarte. Bom registro pra quem ler em 6 meses e perguntar "por que não X?".

- **Alternativa A:** descartada — razão.
- **Alternativa B:** descartada — razão.
- **Manter status quo:** descartado — é a dor que esta ADR endereça.

---

## Referências

- ADRs relacionados (cita por número + nome).
- Aprendizados em `docs/aprendizado/` que dão substrato conceitual.
- Specs em `docs/architecture/` afetadas.
- Discussões externas (curso, paper, conversa cross-AI) com data.
- Skills/runbooks materializados ou afetados pela decisão.
