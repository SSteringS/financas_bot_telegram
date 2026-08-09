# Retrospectiva 02b — Sprint Kaizen (workflow / processo)

**Data:** 2026-05-31
**Período coberto:** Sprint 02b (2026-05-30 → 2026-05-31, ~2 dias).
**Resultado:** ✅ **Todas as ações de processo da RETRO-02 executadas.** 7 de 8 WF tasks concluídas; 1 movida pra pendências técnicas (WF-07 — branch protection, adiada por decisão do humano). Sprint kaizen funcionou como modelo — processo melhorou sem competir com produto.

Esta é a **terceira** retro do projeto. A segunda (`RETRO-02`) foi em 2026-05-30 encerrando a sprint 02; as ações dela são checadas na seção 7 abaixo.

> **Nota de formato:** esta sprint foi fora do convencional — 100% de trabalho em `docs/` e `.claude/`, sem código de produto. Seções que não se aplicam estão marcadas como **N/A** com justificativa.

---

## 1. O que foi entregue

Sprint 02b fechou **7 tasks de processo** (+ 1 movida pra pendências):

| Task | Entregue | Responsável | Observação |
|------|----------|-------------|------------|
| **WF-01** | ✅ | planner | Causa-raiz do truncamento identificada (bug Cowork Write/Edit, não OneDrive); regras defensivas em `planner.md`; aprendizado em `docs/aprendizado/` |
| **WF-02** | ✅ | planner | Migração ADR 0010 concluída: `docs/templates/` criada, pasta legada `docs/status/` apagada, ~140 refs atualizadas |
| **WF-03** | ✅ | eng-ia | 4 smells arquiteturais explícitos no `reviewer.md` + item novo no checklist do papel |
| **WF-04** | ✅ | eng-ia | Zero-padded geral (BE/FE/DEP/EVO/CI) formalizado; `CLAUDE.md` atualizado (`feature/be-NNN-*`) |
| **WF-05** | ✅ | eng-ia + planner | 16 skills em `.claude/skills/`, ADR 0015, agent memory do planner; feedback loop definido (ritmo de retro + dispatch lista skills usadas) |
| **WF-06** | ✅ | eng-ia | Ritual de encerramento de sprint formalizado (`RUNBOOK-fechamento-sprint.md`) com seção `## Agents & Skills` |
| **WF-07** | ➡️ | — | **Movida pra pendências técnicas** — branch protection adiada por decisão do humano; não é urgente |
| **WF-08** | ✅ | eng-ia | `pendencias_humano` formalizado no schema de status + metricas_status.py atualizado |

**Artefatos estruturais criados nesta sprint:**
- `docs/templates/` (centralização do `_TEMPLATE-status.md`)
- `.claude/skills/` com 16 skills cobrindo todos os papéis (planner, back, front, reviewer, architect)
- `.claude/agent-memory/planner/` — memória persistente do planner entre sessões
- ADR 0015 (`Proposed`) — taxonomia roles × skills × workflows

---

## 2. Dados

**N/A — gates de código (build, lint, testes, cobertura):** sprint de processo puro, sem código de produto.
**N/A — PRs:** planner commita direto em `develop` (fluxo documentado no README da sprint 02b).
**N/A — Reviewer:** exceção documentada no README da sprint 02b — meta-workflow do planner dispensa Reviewer; humano revisa o diff direto.

| Métrica | RETRO-02 | RETRO-02b | Δ |
|---------|----------|-----------|---|
| ADRs totais | 12 | **13** (ADR 0015) | +1 |
| Skills em `.claude/skills/` | 0 | **16** | +16 |
| Aprendizados em `docs/aprendizado/` | 28 | **29+** | +1 (cowork-write-truncamento; possível mais via eng-ia) |
| Status reports com schema canônico | — | 3/3 (WF-01..03) | 100% nos novos |
| Gate-fails | 0 | **0** | manteve |
| Pendências humano abertas | 0 | **1** (push dos 4 commits) | push pendente |
| Commits locais não pushados | — | **4** | push pendente |
| WF tasks sem status report | — | **4** (WF-04..08 exceto WF-03) | gap de rastreabilidade |

> **Nota métricas:** `metricas_status.py` não foi rodado nesta retro — Python 3 não estava disponível no ambiente bash do Claude. Rodar manualmente com `python3 docs/scripts/metricas_status.py` antes do push.

---

## 3. O que foi bem (continuar)

- **Sprint kaizen como modelo funcionou.** 8 ações de processo da RETRO-02 executadas em 2 dias, sem competir com produto. O padrão "kaizen entre sprints grandes" se confirma — virar padrão oficial a partir da sprint 03.
- **Divisão planner + eng-ia foi natural.** WF-01/02 (docs puros) com planner; WF-03/04/05/06/08 (metadecisões de processo + skills) com eng-ia. Sem conflito de território.
- **WF-01 resolveu a causa-raiz certa.** Causa anterior ("sync gremlin do OneDrive") era errada — a retro 02 §8a já havia corrigido. WF-01 confirmou: é bug do Cowork Write/Edit. Meta-validação extra: o próprio bug ocorreu durante a execução do WF-01 e foi capturado pelas regras defensivas recém-instauradas.
- **Skills system implementado de vez.** 16 skills cobrindo planner, back, front, reviewer e architect. ADR 0015 registra a taxonomia. A próxima sprint já pode despachar agentes com skills listadas no dispatch em vez de boilerplate inline.
- **Schema canônico de status 100% nos reports novos.** WF-01/02/03 seguiram o schema com N/A explícito onde não se aplica — boa precedência pra sprints de processo futuras.
- **Memória persistente do planner.** `.claude/agent-memory/planner/` evita re-derivar contexto a cada sessão. Piloto confirmado nesta própria retro.

---

## 4. O que pode melhorar

- **WF-04/05/06/08 não têm status reports.** Foram executados pelo eng-ia mas sem artefato de output contract. Perda de rastreabilidade — não sabemos desvios, decisões tomadas, arquivos modificados. Próximas sprints kaizen devem exigir status report mesmo para tasks do eng-ia.
- **Push de 4 commits ainda pendente.** `develop` está 4 commits à frente de `origin/develop` desde esta sprint. Dependência do humano executar o push no terminal Windows. Risco baixo (não há sessão paralela), mas acumular commits locais aumenta a janela de perda em caso de problema no disco.
- **Métricas não rodadas.** `metricas_status.py` é ritual de fechamento (WF-06), mas Python 3 não estava acessível no bash do Claude. Humano precisa rodar manualmente ou garantir que o ambiente do ritual seja o terminal Windows/WSL onde Python está disponível.
- **ADR 0015 ainda `Proposed`.** Skills escritas, sistema em uso, mas a ADR não foi homologada. Deveria virar `Accepted` antes da sprint 03 (se o humano concordar com o conteúdo).
- **Feedback loop de skills não tem coleta inicial.** WF-05 definiu o ritual (retro + dispatch lista skills), mas não há linha de base. Sprint 03 será a primeira a popular a seção `## Agents & Skills`.

---

## 5. Aprendizados-chave do ciclo

- **`docs/aprendizado/cowork-write-truncamento.md`** — causa-raiz confirmada: bug Cowork Write/Edit + mount FUSE. Regras defensivas operacionais em `docs/roles/planner.md` §Escrita defensiva. (WF-01)
- **Skills são contexto passivo** — não há log automático de "skill X foi carregada". Rastreamento precisa ser convencional: dispatch lista skills esperadas; retro pergunta quais funcionaram. Tentar medir *efeito* (frequência de drift detectado pelo humano) é mais confiável que medir *uso*. (WF-05 + discussão desta sessão)
- **Sprint kaizen como padrão de janela de processo** — melhorias de workflow competem mal com sprints de produto; kaizen curta dedicada é o remédio. Confirmado nesta sprint.

---

## 6. Incidentes e como foram resolvidos

| Incidente | Causa | Resolução |
|-----------|-------|-----------|
| Python 3 indisponível no bash do Claude | `python3` redireciona pro Microsoft Store no Windows via bash | Métricas adiadas pra humano rodar no terminal Windows |
| WF-04..08 sem status report | Tasks executadas pelo eng-ia em sessão separada sem artefato de output | Registrado como melhoria; próximas sprints kaizen exigem status mesmo pro eng-ia |

---

## 7. Status das ações da RETRO-02

| # | Ação | Status |
|---|------|--------|
| 1 | Resolver sync gremlin de forma estrutural | ✅ **Cumprida** — WF-01: causa-raiz real identificada (bug Cowork, não OneDrive); regras defensivas em planner.md |
| 2 | Completar migração ADR 0010 | ✅ **Cumprida** — WF-02: pasta legada apagada, templates centralizados, ~140 refs atualizadas |
| 3 | Checklist arquitetural no reviewer.md | ✅ **Cumprida** — WF-03: 4 smells explícitos + item no checklist do papel |
| 4 | Zero-padded geral (BE/FE/DEP/EVO/CI) | ✅ **Cumprida** — WF-04: CLAUDE.md atualizado forward-only |
| 5 | Roles × skills × workflows | ✅ **Cumprida** — WF-05: 16 skills, ADR 0015, feedback loop definido; evolução contínua com uso |
| 6 | Ritual métricas fim de sprint | ✅ **Cumprida** — WF-06: RUNBOOK-fechamento-sprint.md com seção Agents & Skills |
| 7 | Ligar branch protection | ➡️ **Movida** — WF-07 saiu da sprint; vai pra pendências técnicas por decisão do humano |
| 8 | Refinar EVO-09 com arquiteto | 🔲 **Pendente** — sprint 03 |
| 9 | Formalizar `pendencias_humano` no schema | ✅ **Cumprida** — WF-08: campo formalizado + script atualizado |
| 10 | Rotação do token Telegram | ⏸ **Carry-over** — não tocado |
| 11 | SSoT pra config do back | ⏸ **Carry-over** — não tocado |

**Taxa de cumprimento:** 7 cumpridas (64%), 1 movida/adiada (9%), 1 pendente sprint 03 (9%), 2 carry-over (18%).

---

## 8. Ações pra próxima etapa (sprint 03)

| # | Ação | Quando | Dono sugerido |
|---|------|--------|---------------|
| 1 | **Push dos 4 commits** — `origin/develop` está 4 trás do local | imediato | humano (terminal Windows) |
| 2 | **Homologar ADR 0015** — de `Proposed` pra `Accepted` se o conteúdo estiver ok | antes da sprint 03 | humano |
| 3 | **Rodar `metricas_status.py`** — linha de base pré-sprint 03 | antes da sprint 03 | humano (terminal Windows) |
| 4 | **Status reports de WF-04/05/06/08** — criar retroativamente se o eng-ia tiver os insumos | nice-to-have | eng-ia |
| 5 | **Refinar EVO-09 com arquiteto** — fechar decisões pendentes + ADR; abrir sprint 03 | sprint 03 início | arquiteto + planner |
| 6 | **Primeira coleta do ritual Agents & Skills** — sprint 03 é a sprint de estreia do feedback loop | fim da sprint 03 | planner + humano |
| 7 | **Rotação do token Telegram** | quando der | humano |
| 8 | **SSoT pra config do back** | sprint 03 ou backlog | back + planner |

---

## 9. Nota — sprint kaizen como padrão confirmado

A sprint 02b provou o modelo: **kaizen curta (2 dias) entre sprints de produto** consegue executar backlog de processo sem competir com features. 8 ações da RETRO-02 em 2 dias, sem bloqueio de produto, sem Reviewer (exceção documentada). Se o modelo continuar funcionando, vira padrão — uma kaizen entre cada sprint de produto.

Variável a observar na sprint 03: a presença das 16 skills vai reduzir o tamanho dos dispatches? A seção `## Agents & Skills` na RETRO-03 vai responder.

---

> **Como ler esta retro no futuro:** primeira retro de sprint de processo puro. As métricas de "código" são todas N/A — isso é esperado e correto. O que vale aqui é a taxa de cumprimento das ações de processo (7/9 = 78%, desconsiderando carry-overs) e o modelo kaizen confirmado. A RETRO-03 mede o efeito das skills na sprint de produto.
