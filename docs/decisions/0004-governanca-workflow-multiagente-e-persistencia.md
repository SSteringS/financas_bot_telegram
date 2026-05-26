# ADR 0004 — Governança do workflow multiagente e taxonomia de persistência

**Data:** 2026-05-26
**Status:** Aceito
**Decisores:** humano (com ajuda do Claude planejador)

---

## Contexto

O workflow com múltiplos agentes (planejador no desktop + implementadores back/front no IntelliJ + humano orquestrando) cresceu organicamente e funciona, mas uma auditoria externa (discussão com ChatGPT sobre workflows de IA) expôs lacunas de governança:

- **Persistência fragmentada:** decisões e conhecimento espalhados por `docs/decisions/`, `docs/aprendizado/`, `CLAUDE.md`, `docs/plans/` — com fronteiras fuzzy. Decisões arquiteturais recentes (hostnames separados, módulo Terraform único, esquema de reporting) foram parar em `aprendizado/` ou no `CLAUDE.md` em vez de virar ADR.
- **Decisões cross-AI sem rota:** discussões de arquitetura com ChatGPT/Gemini não têm caminho garantido pro repo — risco de divergência arquitetural.
- **Revisão baseada em autorrelato:** o planejador tende a confiar no status report a menos que verifique ativamente.
- **Papéis acumulados:** o planejador faz spec + validação + crítica + revisão de integração, sem independência.

Precisava-se fixar a topologia e *onde cada tipo de informação mora*, antes que a fragmentação piore com o volume.

---

## Decisão

### 1. Topologia: workflow multiagente com orquestrador humano

Assumido explicitamente que **não** é um sistema multiagente autônomo. Os agentes não se comunicam direto; o **humano é o barramento e o integrador**.

- **Planejador (desktop):** specs, docs, `CLAUDE.md`, ADRs, revisão, roteiros de teste. Não implementa código.
- **Implementadores (back/front, IntelliJ):** código no seu território.
- **Humano:** decide tasks, faz os merges em `develop`, roda testes manuais, roteia contexto entre agentes.

Manter essa topologia nesta fase (projeto financeiro, solo) — automação de merge/integração não entra agora.

### 2. Taxonomia de persistência (onde cada coisa mora)

| Local | O que vai aqui |
|---|---|
| `docs/decisions/` (ADR) | **Decisão arquitetural** com alternativas e consequências; cross-cutting; **imutável** (revogar = novo ADR que *supersedes*) |
| `CLAUDE.md` | **Regra operacional** que o agente DEVE obedecer (território, branch, gates) |
| `docs/aprendizado/` | **Conceito formativo** pro humano revisitar |
| `docs/plans/` | **Spec de task** (input contract) |
| `docs/status/` | **Relatório de execução** (output contract, com frontmatter/gates) |
| `docs/avaliacoes/` | **Revisão/avaliação** de entrega |
| `docs/PENDENCIAS-TECNICAS.md` | **Débito técnico** conhecido |

**Regra de roteamento:** decisão arquitetural → ADR; regra que o agente segue → `CLAUDE.md`; aprendizado conceitual → `aprendizado/`; débito → `PENDENCIAS`. Um mesmo assunto pode gerar ADR (a decisão) + entrada em `aprendizado` (o conceito) — mas a **decisão canônica é o ADR**.

### 3. Dono único de arquitetura

O **planejador é o dono da arquitetura** e homologa decisões de **qualquer fonte** (humano, ChatGPT, Gemini) num ADR. Discussão arquitetural cross-AI que não vira ADR é **não-decisão** — não vincula o projeto.

### 4. Verificação independente pra alto risco

Em vez de uma pipeline fixa de 5 papéis (Planner→Agents→Validator→Critic→Integration Reviewer), usar **verificação independente sob demanda** pra tasks que tocam **dinheiro ou infra**: uma instância separada/fresca (ou o planejador num passo deliberadamente adversarial) confere a entrega contra a realidade, não contra o relatório.

### 5. Gate de pré-merge enforced por CI

Os gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` deixam de ser disciplina manual e passam a ser **enforced por CI** no caminho pra `develop` (ver plano `CI-01`).

---

## Razões

- A fragmentação de persistência cresce com o volume; definir fronteiras agora é barato, depois é refactor doloroso.
- ADR como decisão canônica resolve a divergência cross-AI: existe um lugar único e imutável de verdade arquitetural.
- Verificação independente ataca o "sintaxe ≠ semântica" (relatório válido pode mentir) sem o overhead de papéis fixos que um orquestrador solo não sustenta.
- Enforced > disciplina: gate manual escorrega com volume.

---

## Consequências

**Positivas:**
- Onde procurar/registrar cada coisa fica inequívoco.
- Decisões arquiteturais (inclusive vindas de outras IAs) param de se perder.
- Revisão de alto risco ganha independência.
- Confiabilidade do merge sobe quando o CI vira gate.

**Negativas:**
- Mais disciplina de roteamento (decidir "isso é ADR ou aprendizado?") — mitigado pela tabela acima.
- Promover decisões antigas a ADR retroativamente é trabalho pontual.
- O humano segue sendo o gargalo de merge/integração (decisão consciente nesta fase).

---

## Alternativas consideradas

- **Pipeline fixa de 5 papéis (sugestão do ChatGPT):** descartada nesta fase. Overhead de coordenação que um orquestrador solo não sustenta; o que falta é independência de verificação, não mais papéis.
- **Manter o status quo:** descartado. A fragmentação e a ausência de rota cross-AI só pioram com o tempo.
- **Automatizar o merge/integração agora:** descartado. Domínio financeiro + estágio do projeto pedem humano no loop.

---

## Referências

- `docs/aprendizado/structured-outputs.md` (output schema, sintaxe ≠ semântica)
- `docs/runbooks/PRE-MERGE-CHECKLIST.md` e `docs/status/_TEMPLATE.md`
- `docs/plans/CI-01-gate-pr-develop.md` (enforcement)
- Discussão de auditoria do workflow com humano + ChatGPT, 2026-05-26
