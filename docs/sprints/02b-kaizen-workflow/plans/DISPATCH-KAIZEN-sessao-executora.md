# DISPATCH — Sessão executora da sprint kaizen (WF-01 → WF-02 → WF-03)

> **Sobre este arquivo:** prompt pronto pra colar numa sessão nova do **Cowork** (modo planner-executor desta sprint) pra executar as 3 primeiras tasks da sprint kaizen em sequência. Como são tasks de meta-workflow do planner, o fluxo segue a regra do planner: **commit direto em `develop`**, sem branch de feature, sem PR. O humano revisa o diff de cada commit (`git show HEAD`) e dá o `git push` quando aprovar.
>
> **Quando colar:** depois que `docs(kaizen): abre sprint 02b — refina WF-01..WF-03 + dispatch executor + atualiza STATE` mergear/subir em `origin/develop`.
>
> **Duração esperada:** ~3h (WF-01 ~1h + WF-02 ~1.5h + WF-03 ~30min). Pode parar entre tasks.

---

## Pré-condições

- Último commit dos planos refinados visível em `origin/develop` (`git fetch && git log --oneline origin/develop -3`).
- Worktree do planner em `develop`, working tree limpo (`git status` no Git Bash).
- Sessão **nova** de Cowork (não a do planning — pra começar com contexto limpo). Mounts: planner + implementador (igual ao atual).

---

## O prompt (cole tudo a partir daqui na sessão nova)

```
Você é o Claude do planejamento, modo executor da sprint kaizen (sprint 02b). Esta sessão executa 3 tasks em sequência (WF-01 → WF-02 → WF-03). Como são tasks de meta-workflow do planner (só docs), o fluxo segue a regra natural do planner: **commit direto em `develop`**, **sem branch de feature**, **sem PR**. O humano revisa o diff (`git show HEAD`) e dá o `git push` quando aprovar.

Leia, NESTA ORDEM:

- CLAUDE.md (regras globais — atenção especial às seções "Instâncias do Claude Code" [planner commita direto em develop], "Worktrees git" e "Acesso ao git pelo Cowork")
- docs/roles/planner.md (papel atual)
- docs/STATE.md (estado do projeto — sprint 02b em execução)
- docs/sprints/02b-kaizen-workflow/README.md (objetivo da sprint kaizen)
- docs/retrospectivas/RETRO-02-canal-whatsapp.md §4, §8, §8a (contexto da causa-raiz dos truncamentos)

## REGRAS DURAS

1. **Fluxo de git de cada task:** commit direto em `develop` (estamos no worktree do planner; develop já checked out). **Não criar branch.** **Não abrir PR.** Após cada commit:
   - Anunciar pro humano: "WF-NN commitada. Diff: `git show HEAD --stat`. Aguardando revisão e push."
   - **Não executar `git push`.** Humano roda no terminal Windows quando aprovar.
   - Esperar humano sinalizar antes de começar a próxima.

2. **Escrita defensiva** (ainda não temos a regra mergeada — é o que a WF-01 produz, mas aplique-a desde já):
   - Writes em arquivos importantes (planos, ADRs, status, CLAUDE.md, roles, aprendizado, RETRO) sempre via `cat > arquivo << 'EOF' ... EOF` no bash (`mcp__workspace__bash`).
   - Após cada write, verificar com `wc -l && tail -3 arquivo`. Se não bater com expectativa, refazer via bash.
   - Edit/Write tool só pra mudanças mínimas (< 20 linhas) em arquivos pequenos (< 100 linhas).

3. **`git status` no sandbox pode mentir.** Mount FUSE mostra arquivos defasados. Fonte da verdade é o terminal Windows do humano. Quando em dúvida sobre estado do repo, pedir confirmação humano via `git status` no Git Bash. Pra commits internos use `GIT_DIR` + `GIT_WORK_TREE` apontando pro mount do implementador (ver CLAUDE.md §Acesso ao git pelo Cowork).

4. **Sandbox não deleta arquivos.** Operações `rm`/`mv` em arquivos do worktree precisam do humano (terminal Windows). A WF-02 tem um bloco de comandos pronto pra ele colar antes do commit final dessa task.

5. **1 commit por task.** Cada WF-NN vira um único commit em `develop`. Mensagem: `docs(WF-NN): <descrição curta>`. Sem `--amend`, sem múltiplos commits.

6. **Reviewer dispensado.** Você não escreve avaliação. Humano revisa o `git show HEAD` direto.

7. **Pare entre tasks.** Após cada commit, aguarde humano confirmar push antes de iniciar a próxima.

## SEQUÊNCIA

### Etapa 1 — WF-01 (Investigar e mitigar truncamento de writes do Cowork)

Plano: `docs/sprints/02b-kaizen-workflow/plans/WF-01-investigar-truncamento-cowork.md`.

O que entrega:
1. `docs/aprendizado/cowork-write-truncamento.md` novo (contexto + sintomas A/B/C com evidência + workarounds + bug report rascunho em inglês como apêndice).
2. `docs/aprendizado/README.md` indexa o arquivo novo (criar categoria "ferramental / ambiente" se não existir).
3. `docs/roles/planner.md` ganha seção "Escrita defensiva de arquivos (workaround Cowork)" com 3 regras (ver plano §"Escopo > Modificar").
4. `CLAUDE.md` corrigido — substitui menções a "sync gremlin do OneDrive" pelo diagnóstico correto + referência ao aprendizado e ao planner.md.

Commit: `docs(WF-01): aprendizado + regras de escrita defensiva + correção CLAUDE.md`.

Status report: `docs/sprints/02b-kaizen-workflow/status/WF-01.md` (frontmatter conforme `docs/status/_TEMPLATE.md` ainda — migração pra `docs/templates/` é da WF-02). Campo `branch_convencao: na` (commit direto sem branch).

**Anunciar pro humano. Aguardar push.**

### Etapa 2 — WF-02 (Completar migração ADR 0010)

Plano: `docs/sprints/02b-kaizen-workflow/plans/WF-02-completar-migracao-adr-0010.md`.

O que entrega (em ordem):
1. Criar `docs/templates/_TEMPLATE-status.md` (conteúdo de `docs/status/_TEMPLATE.md`) + `docs/templates/README.md` curto.
2. Copiar `docs/status/DEP-07.md` pra `docs/sprints/01-mvp/status/DEP-07.md`.
3. Sobrescrever `docs/sprints/01-mvp/status/_RESUMO-overnight-deploy.md` pela versão de `docs/status/_RESUMO-overnight-deploy.md` (a mais recente — 132 linhas, "sessão do Reviewer").
4. Atualizar refs `docs/status/...` → `docs/sprints/01-mvp/status/...` (ou `docs/templates/_TEMPLATE-status.md`) nos 20 arquivos da tabela do plano.
5. Adicionar bloco "Execução" em `docs/decisions/0010-organizacao-doc-por-sprint.md` (data + lista do que foi feito + link pra esta task).

Commit: `docs(WF-02): cria docs/templates/, migra DEP-07 e _RESUMO, atualiza 20 refs`.

Status report: `docs/sprints/02b-kaizen-workflow/status/WF-02.md`. **IMPORTANTE:** o status report deve já usar o novo path do template (`docs/templates/_TEMPLATE-status.md`) — meta-validação. Campo `branch_convencao: na`.

**Antes do commit:** colar o "Runbook do humano" do fim do plano WF-02 no chat — humano roda no Git Bash o `rm -rf docs/status/` + sanity checks. Aguardar confirmação dele que rodou. **Só então** fazer o commit (que vai incluir a deleção da pasta capturada pelo git).

**Após commit, anunciar pro humano. Aguardar push.**

### Etapa 3 — WF-03 (Checklist arquitetural no reviewer.md)

Plano: `docs/sprints/02b-kaizen-workflow/plans/WF-03-checklist-arquitetural-reviewer.md`.

O que entrega:
1. `docs/roles/reviewer.md` ganha seção `## Smells arquiteturais (em tasks que mexem em camadas)` com os 4 bullets do texto proposto no plano (literal — texto já está pronto, copiar com o markdown intacto).
2. `docs/roles/reviewer.md` `## Checklist do papel` ganha item novo: `[ ] Em tasks que mexem em camadas (adapter, port, application): passei pelo bloco "Smells arquiteturais".`
3. `docs/plans/BACKLOG-evolucao-workflow.md` item #8 marcado ✅ feito com link pra esta task e pra `reviewer.md`.

Commit: `docs(WF-03): checklist arquitetural no reviewer.md (4 smells)`.

Status report: `docs/sprints/02b-kaizen-workflow/status/WF-03.md`. Campo `branch_convencao: na`.

**Anunciar pro humano. Aguardar push.**

## VALIDAÇÃO LOCAL OBRIGATÓRIA (após cada commit, antes de anunciar)

Sem código de produto → sem `mvn test` / `npm test`. Verificação é semântica:

- `wc -l` de cada arquivo modificado bate com expectativa do plano?
- `git show HEAD --stat` mostra **só** os arquivos previstos no plano?
- Após WF-02, `python3 docs/scripts/metricas_status.py` roda sem erro?
- Após WF-02, `grep -rn "docs/status/" docs/ CLAUDE.md` retorna 0 matches?
- Após WF-03, novo item do checklist no reviewer.md está renderizando markdown corretamente?

## STATUS REPORT (3 — um por task)

Cada um em `docs/sprints/02b-kaizen-workflow/status/WF-NN.md` seguindo `docs/status/_TEMPLATE.md` (ou já o de `docs/templates/` se a WF-02 commitou). Frontmatter válido:

- `estado: concluido`
- `gates.build`, `gates.lint`, `gates.testes` → `na` (tasks só-doc)
- `gates.testes_total`, `gates.testes_novos` → `na`
- `gates.branch_convencao` → `na` (commit direto sem branch)
- `gates.territorio` → `ok` (só `docs/`)
- `desvios: 0` (a menos que tenha)
- `pendencias_humano: 1` (o push)

## SE QUEBRAR

- **Truncamento detectado no `tail -3`:** refazer via bash heredoc imediatamente. Não deixar passar.
- **Mount FUSE mostra arquivo modificado que não foi tocado:** confirmar com humano via terminal Windows. Não fazer `git restore` no sandbox.
- **Refs faltantes na WF-02 (grep encontra `docs/status/` depois de "terminar"):** atualizar e abrir comentário no status report. Não ignorar.
- **Humano rejeita o diff no review:** registrar feedback dele no chat, ajustar via novo commit (ainda em develop, sem `--amend` porque o anterior já pode ter saído pra ele revisar). Não force-push.

Pare ao final de cada commit. Aguarde push.
```

---

## Notas pro humano (fora do prompt)

- **Estimativa total:** ~3h. WF-01 ~1h, WF-02 ~1.5h, WF-03 ~30min.
- **Você revisa cada commit antes do push.** Comando: `git show HEAD` (mostra diff completo) ou `git show HEAD --stat` (só arquivos). Aceitou? `git push`. Não aceitou? Diz no chat o que tá errado, eu/executor faz commit novo de ajuste.
- **Após WF-02 commitada e pushada:** próxima sessão de planejamento já pode usar `docs/templates/_TEMPLATE-status.md` como referência.
- **Após WF-03 commitada e pushada:** as 5 WF-NN restantes (WF-04..WF-08) podem ser refinadas na sessão do planning (esta aqui, ou outra que herda contexto via STATE.md + README da kaizen).
- **Pode parar entre as 3** — DISPATCH foi escrito pra parar e avisar.

## Referências

- Planos das 3 tasks (em `docs/sprints/02b-kaizen-workflow/plans/`).
- `docs/sprints/02b-kaizen-workflow/README.md` (escopo da sprint).
- RETRO-02 §8 (ações que deram origem às tasks).
- CLAUDE.md §Instâncias (planner commita direto em develop).
- Modelo: `docs/sprints/02-canal-whatsapp/plans/MASTER-PROMPT-overnight-sprint02-1.md` (estilo de multi-task sequence — mas aquele era pra back, que usa branch + PR).
