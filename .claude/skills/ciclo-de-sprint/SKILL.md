---
name: ciclo-de-sprint
description: Gerenciamento do ciclo de sprint do projeto — abertura, condução, fechamento, retrospectiva e decisão de kaizen. Carregar quando o humano pede "abre a sprint X", "fecha a sprint", "escreve a retro", "o que falta fechar?", ou quando STATE.md precisa de atualização de fase. É o workflow específico deste projeto (worktrees, sessões especializadas, humano integrador) — não Scrum genérico.
load_pattern: contextual
used_by: [planner]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — Ciclo de Sprint

## Quando carregar

- Humano pede pra **abrir** uma sprint nova.
- Humano pede pra **fechar** a sprint corrente.
- Humano pede pra **escrever a retrospectiva**.
- `STATE.md` precisa de atualização de fase.
- Dúvida sobre **quando/como abrir kaizen**.
- Pedido de "o que falta fechar antes de passar pra próxima sprint?".

## O que é diferente do Scrum genérico

Não há standups, velocity points nem sprint review com stakeholders. As "cerimônias" são artefatos que o planner escreve; o humano integra (merge, push, deploy). Sprint de produto e sprint kaizen podem coexistir numeradas como `<NN>` e `<NN>b`.

## Abertura de sprint

Checklist em ordem:

1. Criar `docs/sprints/<NN>-<slug>/` com subpastas `plans/`, `status/`, `avaliacoes/`.
2. Escrever `README.md` da sprint: objetivo, escopo (lista de tasks), fluxo de git, estrutura de pastas.
3. Atualizar `docs/STATE.md`: sprint anterior → "fechada" (se não estava); nova → "em execução".
4. Refinar tasks candidatas do backlog: cada uma vira plano em `plans/` (usar skill `escrita-de-plano-completo` para specs não-triviais).
5. Checar dependências entre tasks: sinalizar no README quando uma bloqueia outra.

**Slug:** kebab-case descritivo. Kaizen recebe sufixo `b`/`c` na numeração da sprint de origem (ex.: `02b-kaizen-workflow`).

## Condução da sprint

- Tasks avançam via DISPATCH → implementor executa → reviewer valida → humano mergia.
- **Bloqueio externo:** registrar no README da sprint em `### Parqueado` + manter no STATE.md.
- **Impedimento interno:** sinalizar no plano da task como `> BLOQUEIO:` e escalar ao humano se não resolve em ≤1 sessão.
- STATE.md atualiza conforme tasks mergiam: mover de "pronto pra dispatch" → "mergeado em develop".

## Fechamento de sprint

Checklist em ordem:

1. Confirmar que todas as tasks têm status report com `estado: concluido` ou foram movidas explicitamente pro backlog.
2. Rodar `python3 docs/scripts/metricas_status.py` — capturar números pra retro.
3. Escrever `docs/retrospectivas/RETRO-<NN>-<slug>.md` (ver seção Retrospectiva abaixo).
4. Atualizar STATE.md: sprint → "fechada"; seção "Próximos passos" → sprint seguinte.
5. Decidir se abre kaizen (ver critério abaixo).

## Retrospectiva — o que capturar

Formato canônico (baseado nas retros existentes do repo):

- **§1 Resumo numérico:** tasks planejadas vs entregues, gate-fails, desvios, testes (saída do `metricas_status.py`).
- **§2 O que funcionou:** ≥3 bullets concretos com evidência do repo (ex.: "reviewer independente pegou drift arquitetural em BE-19a — virou FIX-idempotencia-porta-application").
- **§3 O que machucou:** ≥3 bullets com causa-raiz tentativa, não só sintoma.
- **§4 Ações:** cada ação tem *dono* (papel ou humano), *sprint alvo*, *critério de feito*. Ação sem dono = não-ação.
- **§5 Tópicos pro backlog:** items que não viraram ação mas merecem entrar no `BACKLOG-evolucao-workflow.md`.

Anti-pattern: retro genérica ("comunicação melhorou"). Cada bullet precisa de evidência do repo.

## Critério de kaizen

Abrir sprint kaizen quando **≥2 das condições** batem:

- ≥3 items de processo no backlog com prioridade alta.
- A retro gerou ≥2 ações de workflow sem sprint-alvo definida.
- Um impedimento recorrente foi identificado.
- Humano sinalizou explicitamente que quer atacar processo antes de produto.

## Ler junto

- `docs/plans/BACKLOG-evolucao-workflow.md` — items de processo pra kaizenss
- `docs/plans/BACKLOG-produto.md` — backlog de produto pra planejar sprint de produto
- `docs/scripts/metricas_status.py` — rodar no fechamento pra capturar métricas
- Skill `escrita-de-dispatch` — como despachar tasks da sprint
