# DISPATCH — Sessão executora da sprint kaizen (WF-01 → WF-02 → WF-03)

> **Sobre este arquivo:** prompt pronto pra colar numa sessão nova do **Cowork** (modo planner-executor desta sprint) pra executar as 3 primeiras tasks da sprint kaizen em sequência. Cada task vira branch própria e PR próprio — humano revisa cada PR (Reviewer dispensado nesta sprint).
>
> **Quando colar:** depois que o commit `docs(kaizen): refina WF-01..WF-03 + README + STATE` mergear em `develop`.
>
> **Duração esperada:** ~3h (WF-01 ~1h + WF-02 ~1.5h + WF-03 ~30min). Pode parar entre tasks se ficar tarde.

---

## Pré-condições

- Commit dos planos refinados em `develop` (`docs(kaizen): refina WF-01..WF-03 + README + STATE`).
- Worktree do planner em `develop` (CLAUDE.md §Worktrees git).
- Sessão **nova** de Cowork (não a do planning — pra começar com contexto limpo). Mounts iguais aos atuais (planner + implementador).

---

## O prompt (cole tudo a partir daqui na sessão nova)

```
Você é o Claude do planejamento, modo executor da sprint kaizen (sprint 02b). Esta sessão executa 3 tasks em sequência (WF-01 → WF-02 → WF-03). Cada uma vira branch própria + PR próprio; o humano revisa cada PR direto (Reviewer dispensado nesta sprint — ver `docs/sprints/02b-kaizen-workflow/README.md` §"Exceção do ADR 0005").

Leia, NESTA ORDEM:

- CLAUDE.md (regras globais — atenção especial às seções "Worktrees git" e "Acesso ao git pelo Cowork")
- docs/roles/planner.md (papel atual)
- docs/STATE.md (estado do projeto)
- docs/sprints/02b-kaizen-workflow/README.md (objetivo da sprint kaizen + exceção do Reviewer)
- docs/retrospectivas/RETRO-02-canal-whatsapp.md §4, §8, §8a (contexto da causa-raiz dos truncamentos)

## REGRAS DURAS

1. **Escrita defensiva:** ainda não temos a regra mergeada (é o que a WF-01 produz), mas **aplique-a desde já**:
   - Writes em arquivos importantes (planos, ADRs, status, CLAUDE.md, roles, aprendizado, RETRO) sempre via `cat > arquivo << 'EOF' ... EOF` no bash (`mcp__workspace__bash`).
   - Após cada write, verificar com `wc -l && tail -3 arquivo`. Se não bater com expectativa, refazer via bash.
   - Edit/Write tool só pra mudanças mínimas (< 20 linhas) em arquivos pequenos (< 100 linhas).

2. **`git status` no sandbox pode mentir.** Mount FUSE mostra arquivos defasados. Fonte da verdade é o terminal Windows do humano. Quando em dúvida sobre estado do repo, pedir confirmação humano via `git status` no Git Bash.

3. **Sandbox não deleta arquivos.** Operações `rm`/`mv` em arquivos do worktree precisam do humano (terminal Windows). A WF-02 tem um bloco de comandos pronto pra ele colar.

4. **Branch por task.** Cada WF-NN sai de `develop` com `git fetch && git checkout -b feature/wf-NN-<slug> develop`. Não acumular múltiplas tasks na mesma branch.

5. **Reviewer dispensado.** Você não escreve avaliação. Humano revisa o PR direto.

6. **Pare entre tasks pra humano revisar.** Após cada PR aberto, **anuncie pro humano "WF-NN pronta pra review"** e aguarde sinal antes de começar a próxima.

## SEQUÊNCIA

### Etapa 1 — WF-01 (Investigar e mitigar truncamento de writes do Cowork)

Plano: `docs/sprints/02b-kaizen-workflow/plans/WF-01-investigar-truncamento-cowork.md`.

Branch: `feature/wf-01-investigar-truncamento-cowork`.

O que entrega:
1. `docs/aprendizado/cowork-write-truncamento.md` novo (contexto + sintomas A/B/C com evidência + workarounds + bug report rascunho em inglês como apêndice).
2. `docs/aprendizado/README.md` indexa o arquivo novo (criar categoria "ferramental / ambiente" se não existir).
3. `docs/roles/planner.md` ganha seção "Escrita defensiva de arquivos (workaround Cowork)" com 3 regras (ver plano §"Escopo > Modificar").
4. `CLAUDE.md` corrigido — substitui menções a "sync gremlin do OneDrive" pelo diagnóstico correto + referência ao aprendizado e ao planner.md.

Commit: `docs(WF-01): aprendizado + regras de escrita defensiva + correção CLAUDE.md`.

Status report: `docs/sprints/02b-kaizen-workflow/status/WF-01.md` (frontmatter conforme `docs/status/_TEMPLATE.md` ainda — a movimentação pra `docs/templates/` é da WF-02).

PR contra `develop`. **PARAR. Avisar humano.**

### Etapa 2 — WF-02 (Completar migração ADR 0010)

Plano: `docs/sprints/02b-kaizen-workflow/plans/WF-02-completar-migracao-adr-0010.md`.

Branch: `feature/wf-02-completar-migracao-adr-0010`.

O que entrega (em ordem):
1. Criar `docs/templates/_TEMPLATE-status.md` (conteúdo de `docs/status/_TEMPLATE.md`) + `docs/templates/README.md` curto.
2. Copiar `docs/status/DEP-07.md` pra `docs/sprints/01-mvp/status/DEP-07.md`.
3. Sobrescrever `docs/sprints/01-mvp/status/_RESUMO-overnight-deploy.md` pela versão de `docs/status/_RESUMO-overnight-deploy.md` (a mais recente — 132 linhas, "sessão do Reviewer").
4. Atualizar refs `docs/status/...` → `docs/sprints/01-mvp/status/...` (ou `docs/templates/_TEMPLATE-status.md`) nos 20 arquivos da tabela do plano.
5. Adicionar bloco "Execução" em `docs/decisions/0010-organizacao-doc-por-sprint.md` (data + lista do que foi feito + link pra esta task).

Commit: `docs(WF-02): cria docs/templates/, migra DEP-07 e _RESUMO, atualiza 20 refs`.

Status report: `docs/sprints/02b-kaizen-workflow/status/WF-02.md`. **IMPORTANTE:** o status report deve já usar o novo path do template (`docs/templates/_TEMPLATE-status.md`) — meta-validação.

**Antes do PR:** colar o "Runbook do humano" do fim do plano WF-02 — humano roda no Git Bash o `rm -rf docs/status/` + sanity checks. Aguardar confirmação dele que rodou.

PR contra `develop`. **PARAR. Avisar humano.**

### Etapa 3 — WF-03 (Checklist arquitetural no reviewer.md)

Plano: `docs/sprints/02b-kaizen-workflow/plans/WF-03-checklist-arquitetural-reviewer.md`.

Branch: `feature/wf-03-checklist-arquitetural-reviewer`.

O que entrega:
1. `docs/roles/reviewer.md` ganha seção `## Smells arquiteturais (em tasks que mexem em camadas)` com os 4 bullets do texto proposto no plano (literal — texto já está pronto, copiar com o markdown intacto).
2. `docs/roles/reviewer.md` `## Checklist do papel` ganha item novo: `[ ] Em tasks que mexem em camadas (adapter, port, application): passei pelo bloco "Smells arquiteturais".`
3. `docs/plans/BACKLOG-evolucao-workflow.md` item #8 marcado ✅ feito com link pra esta task e pra `reviewer.md`.

Commit: `docs(WF-03): checklist arquitetural no reviewer.md (4 smells)`.

Status report: `docs/sprints/02b-kaizen-workflow/status/WF-03.md`.

PR contra `develop`. **PARAR. Avisar humano.**

## VALIDAÇÃO LOCAL OBRIGATÓRIA (após cada PR)

Sem código de produto → sem `mvn test` / `npm test`. Verificação é semântica:

- `wc -l` de cada arquivo modificado bate com expectativa do plano?
- `git diff --stat` mostra **só** os arquivos previstos no plano?
- Após WF-02, `python3 docs/scripts/metricas_status.py` roda sem erro?
- Após WF-02, `grep -rn "docs/status/" docs/ CLAUDE.md` retorna 0 matches?
- Após WF-03, novo item do checklist no reviewer.md está renderizando markdown corretamente?

## STATUS REPORT (3 — um por task)

Cada um em `docs/sprints/02b-kaizen-workflow/status/WF-NN.md` seguindo `docs/status/_TEMPLATE.md` (ou já o de `docs/templates/` se a WF-02 mergeou). Frontmatter válido (`estado`, `gates.build`, `gates.lint`, `gates.testes` → `na` pra tasks só-doc; `branch_convencao` + `territorio` → `ok`; `desvios` + `pendencias_humano`).

Como `gates.testes_total` e `gates.testes_novos` não fazem sentido pra tasks só-doc, marcar como `na`.

## SE QUEBRAR

- **Truncamento detectado no `tail -3`:** refazer via bash heredoc imediatamente. Não deixar passar.
- **Conflito ao criar branch (develop checked out no planner-worktree):** seguir o fluxo do CLAUDE.md (`git fetch && git checkout -b feature/wf-NN-<slug> develop`). Se ainda quebrar, parar e pedir humano.
- **Mount FUSE mostra arquivo modificado que não foi tocado:** confirmar com humano via terminal Windows. Não fazer `git restore` no sandbox.
- **Refs faltantes na WF-02 (grep encontra `docs/status/` depois de "terminar"):** atualizar e abrir comentário no status report. Não ignorar.

Pare ao final de cada PR. Não mergeie.
```

---

## Notas pro humano (fora do prompt)

- **Estimativa total:** ~3h. WF-01 ~1h (escrita densa). WF-02 ~1.5h (movimentação + 20 refs). WF-03 ~30min (texto curto).
- **Pode fazer em 1 sessão ou parar entre tasks** — DISPATCH foi escrito pra parar e avisar você entre PRs.
- **Você revisa cada PR direto.** Sem Reviewer separado (decisão da kaizen). Critério: ler o diff completo no GitHub + sanity-checks do plano.
- **Após WF-02 mergear:** próxima sessão de planning já pode usar `docs/templates/_TEMPLATE-status.md` como referência.
- **Após WF-03 mergear:** as 5 WF-NN restantes (WF-04..WF-08) podem ser refinadas na sessão do planning (esta aqui, ou outra que herda o contexto via STATE.md + README da kaizen).

## Referências

- Planos das 3 tasks (em `docs/sprints/02b-kaizen-workflow/plans/`).
- `docs/sprints/02b-kaizen-workflow/README.md` (escopo da sprint + exceção do Reviewer).
- RETRO-02 §8 (ações que deram origem às tasks).
- Modelo: `docs/sprints/02-canal-whatsapp/plans/MASTER-PROMPT-overnight-sprint02-1.md` (estilo de multi-task sequence).
