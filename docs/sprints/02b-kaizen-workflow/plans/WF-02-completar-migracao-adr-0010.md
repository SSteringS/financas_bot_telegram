# WF-02 — Completar migração ADR 0010 (limpar `docs/status/` legada)

> **Intake**
>
> - **Origem:** ação #2 da RETRO-02 (§8) + achado da seção 4 (ADR 0010 ficou pela metade — `docs/status/` continua intacta com 46 cópias + DEP-07 não migrado).
> - **Prioridade:** alta. Sem isso o `metricas_status.py` precisa de lógica de dedupe (já adicionada na sessão da RETRO-02, mas é band-aid), e qualquer agente novo se confunde com a duplicação. ADR 0010 só vira verdade quando essa task termina.
> - **Esforço:** médio (~1.5h, sessão única do planner + 1 rodada de comandos no terminal Windows do humano). Mecânico depois de inventariado.
> - **Território / quem executa:** `docs/` inteiro → **Claude do planejamento** escreve + edita; **humano** executa os `mv`/`rm` no terminal Windows (sandbox não deleta).
> - **Branch:** `feature/wf-02-completar-migracao-adr-0010` a partir de `develop`. (Padrão antigo até WF-04 mergear.)
> - **Dependências:** nenhuma — independente de WF-01. Pode rodar em paralelo.
> - **Riscos:** baixos (movimentação de docs sem código). Risco residual: alguma referência fora do scan (commits antigos, PRs no GitHub) vira link quebrado.

---

## Contexto

ADR 0010 (2026-05-28) definiu a organização nova: status reports vão pra `docs/sprints/<NN>/status/` em vez de `docs/status/`. A migração inicial foi feita pra `docs/sprints/01-mvp/status/` (46 arquivos), mas **`docs/status/` continua intacta com as mesmas 46 cópias + 2 arquivos extras**:

- `DEP-07.md` — só existe em `docs/status/` (não migrado).
- `_TEMPLATE.md` — template global do schema canônico (ADR 0007).
- `_RESUMO-overnight-deploy.md` — **versão divergente**: a de `docs/status/` é mais recente (132 linhas, "sessão do Reviewer", pós-aprovação de PRs); a de `docs/sprints/01-mvp/status/` é pré-reviewer (128 linhas).

Diff entre as duas pastas (sessão da RETRO-02):

```
$ for f in docs/sprints/01-mvp/status/*.md; do
    base=$(basename $f)
    if [ -f docs/status/$base ]; then
      if cmp -s $f docs/status/$base; then echo "OK $base"
      else echo "DIFERE $base"; fi
    fi
  done | sort | uniq -c
     45 OK ...
      1 DIFERE _RESUMO-overnight-deploy.md
```

**45 dos 46 arquivos comuns são byte-a-byte idênticos.** Limpeza é segura — sem perda de informação.

Além disso, ~20 arquivos no repo referenciam `docs/status/` por path: CLAUDE.md, STATE.md, 1 aprendizado, 8 avaliações, 5 ADRs, 2 backlogs. Decisão da discovery 2026-05-30: **atualizar todos** (vivos + históricos) — quando o path some, o link some.

## Decisão / abordagem

Executar a migração que faltou da ADR 0010, sem deixar pasta legada nem path quebrado. Saídas:

1. **Pasta nova `docs/templates/`** com o template de status como primeiro morador.
2. **`docs/sprints/01-mvp/status/`** ganha `DEP-07.md` e a versão atualizada do `_RESUMO-overnight-deploy.md`.
3. **`docs/status/`** removida do disco.
4. **Todas as referências** atualizadas em massa via find-and-replace (com revisão por contexto).
5. **ADR 0010** ganha entrada de "Execução" registrando data e o que foi feito.

**Sobre `_TEMPLATE.md`:** decisão de design é centralizar templates em `docs/templates/`. Esta task move **apenas o de status** (escopo mínimo); `docs/decisions/_TEMPLATE.md` e `docs/plans/_TEMPLATE.md` ficam onde estão por ora. **A centralização total dos 3 vira candidato pro backlog de workflow** (não nesta task).

## Escopo / arquivos

### Criar

- **`docs/templates/`** — pasta nova.
- **`docs/templates/_TEMPLATE-status.md`** — cópia exata do conteúdo de `docs/status/_TEMPLATE.md`. Nome ganha sufixo `-status` pra ficar explícito quando o resto migrar (princípio: dentro de `docs/templates/`, nome diz o tipo).
- **`docs/templates/README.md`** (curto, ~10 linhas) — índice da pasta + nota: "Por ora só centraliza o template de status; centralização dos templates de ADR e plano fica pra task futura."

### Modificar (mover)

- **`docs/sprints/01-mvp/status/DEP-07.md`** — receber o conteúdo atual de `docs/status/DEP-07.md`. Avaliação `docs/avaliacoes/backend-dep-07-bootstrap-ec2.md` já está em paralelo (referência continua válida após update).
- **`docs/sprints/01-mvp/status/_RESUMO-overnight-deploy.md`** — sobrescrever pela versão de `docs/status/_RESUMO-overnight-deploy.md` (a mais recente, pós-reviewer).

### Modificar (atualizar referências `docs/status/` → path novo)

Lista validada por grep (sessão 2026-05-30; conferir e reexecutar antes do PR):

| Arquivo | Substituição |
|---|---|
| `CLAUDE.md` | menções a `docs/status/` → contextualizar: status reports vivem em `docs/sprints/<NN>/status/` ou referência ao template `docs/templates/_TEMPLATE-status.md` (depende do contexto local). |
| `docs/STATE.md` | mesmo critério. |
| `docs/aprendizado/structured-outputs.md` | menções a `docs/status/` (2 ocorrências) → `docs/sprints/<NN>/status/` (genérico) ou `docs/templates/_TEMPLATE-status.md`. |
| `docs/avaliacoes/backend-be-16.md` | `docs/status/BE-16.md` → `docs/sprints/01-mvp/status/BE-16.md`. |
| `docs/avaliacoes/backend-dep-01.md` | `docs/status/DEP-01.md` → `docs/sprints/01-mvp/status/DEP-01.md` (5 ocorrências; conferir cada). |
| `docs/avaliacoes/backend-dep-03-api-subdominio-proxy.md` | `docs/status/DEP-03.md` → `docs/sprints/01-mvp/status/DEP-03.md`. |
| `docs/avaliacoes/backend-dep-04-pipeline-deploy-front.md` | `docs/status/DEP-04.md` → `docs/sprints/01-mvp/status/DEP-04.md`. |
| `docs/avaliacoes/backend-dep-05-cors-cookie-prod.md` | `docs/status/DEP-05.md` → `docs/sprints/01-mvp/status/DEP-05.md`. |
| `docs/avaliacoes/backend-dep-07-bootstrap-ec2.md` | `docs/status/DEP-07.md` + `docs/status/_RESUMO-overnight-deploy.md` → versões em `docs/sprints/01-mvp/status/`. |
| `docs/avaliacoes/backend-polish-evo07.md` | conferir e atualizar. |
| `docs/avaliacoes/ci-01-gate-pr-develop.md` | conferir e atualizar. |
| `docs/avaliacoes/frontend-fase3-overnight.md` | conferir e atualizar. |
| `docs/avaliacoes/frontend-fe-13-codegen-openapi.md` | conferir e atualizar. |
| `docs/decisions/0004-governanca-workflow-multiagente-e-persistencia.md` | conferir e atualizar (ADR ativo, importante manter consistente). |
| `docs/decisions/0006-...md`, `0007-...md`, `0008-...md` | idem. |
| `docs/decisions/0010-organizacao-doc-por-sprint.md` | **adicionar bloco de "Execução"** com data 2026-05-30, lista do que foi feito, link pra esta task. |
| `docs/plans/BACKLOG-evolucao-workflow.md` | conferir e atualizar. |
| `docs/plans/BACKLOG-produto.md` | conferir e atualizar. |

**Critério de substituição:** sempre que o path apontar pra arquivo específico (`docs/status/<arquivo>.md`), substituir pelo path real em `docs/sprints/<NN>/status/<arquivo>.md`. Sempre que apontar pra **pasta** (`docs/status/`) genericamente, substituir pelo conceito (`docs/sprints/<NN>/status/`).

### Apagar (operação no terminal Windows do humano)

- Todos os 45 arquivos idênticos em `docs/status/` (depois de validar com `cmp -s` que ainda são idênticos no momento da execução).
- `docs/status/_TEMPLATE.md` (foi pra `docs/templates/`).
- `docs/status/_RESUMO-overnight-deploy.md` (versão nova foi pra sprint 01).
- `docs/status/DEP-07.md` (foi pra sprint 01).
- A pasta `docs/status/` em si.

**Não pode ser feito do sandbox do Cowork** (mount não permite delete — sintoma C do WF-01). Plano gera **bloco de comandos prontos pra colar no Git Bash** ao fim da seção de execução.

### Não tocar

- Os 46 arquivos em `docs/sprints/01-mvp/status/` (ficam onde estão; é o destino).
- Templates `docs/decisions/_TEMPLATE.md` e `docs/plans/_TEMPLATE.md` (centralização fica pra task futura).
- `metricas_status.py` — funciona com pasta legada ausente (o `_default_status_dirs()` checa `is_dir()` antes de adicionar). Lógica de dedupe vira no-op mas pode ficar como safety net.
- Pastas em `docs/sprints/02-canal-whatsapp/` e `docs/sprints/02b-kaizen-workflow/` (já estão certas).

## Critérios de aceitação

- [ ] `docs/templates/` criada com `_TEMPLATE-status.md` (conteúdo idêntico ao antigo `docs/status/_TEMPLATE.md`) + README curto.
- [ ] `docs/sprints/01-mvp/status/DEP-07.md` existe com conteúdo do antigo `docs/status/DEP-07.md`.
- [ ] `docs/sprints/01-mvp/status/_RESUMO-overnight-deploy.md` agora contém a versão pós-reviewer (132 linhas, "Atualizado em 2026-05-27 (sessão do Reviewer)").
- [ ] `docs/status/` não existe.
- [ ] `grep -rn "docs/status/" docs/ CLAUDE.md` retorna **0** matches (exceto blocos de "histórico" explícitos, se houver).
- [ ] ADR 0010 ganhou seção "Execução" datada 2026-05-30 listando o que foi feito.
- [ ] `python3 docs/scripts/metricas_status.py` roda sem erro, total de reports cai de 55 pra ~49-50 (eliminação das duplicatas + DEP-07 contabilizado uma vez só).
- [ ] Status report `docs/sprints/02b-kaizen-workflow/status/WF-02.md` com frontmatter válido conforme `docs/templates/_TEMPLATE-status.md` (já usando o novo path no próprio report — meta).
- [ ] Branch `feature/wf-02-completar-migracao-adr-0010` saiu de `develop`.
- [ ] Território só `docs/`.
- [ ] Humano aprovou (Reviewer dispensado pra sprint kaizen — exceção registrada em `docs/sprints/02b-kaizen-workflow/README.md`).

## Fora de escopo (explicitamente)

- **Centralizar `_TEMPLATE.md` de ADR e plano em `docs/templates/`** — escopo mínimo nesta task; centralização total vira candidato pro backlog de workflow (avaliar custo/benefício depois que `docs/templates/` existir e tiver uso).
- **Atualizar URLs em commits antigos / PRs do GitHub** — fora do nosso alcance; aceitar quebra de links históricos remotos.
- **Simplificar a lógica de dedupe de `metricas_status.py`** — funciona como no-op quando não há duplicatas; manter como safety net é trivial. Remoção fica como cleanup oportuno futuro.
- **Renomear `docs/sprints/01-mvp/`** ou mexer em outras sprints — escopo é só ADR 0010 ficar honesta.
- **Reescrever as avaliações legadas no estilo novo** — só atualizar paths; conteúdo histórico fica.

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Alguma referência fica fora do grep (ex.: comentário em código que aponta pra path) | Média | Baixo | Rodar grep abrangente (`docs/`, `CLAUDE.md`, `README.md`, scripts) duas vezes — antes da edição e antes do PR. |
| Versão "errada" do `_RESUMO-overnight-deploy.md` sobreviver | Baixa | Médio (perda de info do reviewer) | Já confirmado: versão de `docs/status/` é a mais recente (132 vs 128 linhas, "sessão do Reviewer" no cabeçalho). Conferir mais uma vez antes do `cp`. |
| Apagar pasta antes de atualizar refs deixa links quebrados temporariamente | Média | Baixo | Ordem prescrita no runbook: 1) criar/mover, 2) atualizar refs, 3) **só então** apagar. |
| Branch protection do CI-01 informativa não alerta sobre links quebrados | Média | Baixo | Grep manual cobre. CI-01 não checa markdown links (fora de escopo dele). |
| Humano esquecer de rodar o bloco de `rm` no terminal | Média | Baixo | Status report fica como `parcial: rm pendente` até confirmação visual. |

## Coordenação

- **Não toca código** — zero conflito com back/front.
- **Independente das outras WF-NN** — pode rodar em paralelo.
- **Após merge:** próximas sprints já documentam só `docs/sprints/<NN>/status/`; `docs/status/` deixa de ser caminho válido.
- **Humano (revisor) foca em:** (a) refs atualizadas em todos os 20 arquivos? (b) ADR 0010 ganhou seção de execução? (c) `metricas_status.py` ainda roda? (d) bloco de comandos pro humano executar `rm` está claro?

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report com frontmatter válido, revisão direta do humano (Reviewer dispensado nesta sprint). PR pra `develop`. **Não mergear sem confirmação do humano de que rodou o bloco de `rm` no terminal Windows** e validou via `ls docs/ | grep status` que sumiu.

## Runbook do humano (bloco final pra Git Bash)

> Cole no Git Bash em `C:\Users\satya\src\financas_bot_telegram-planner` quando o planner mandar (após edição dos refs estar pronta):
>
> ```bash
> # 1. Conferir que os 45 são idênticos (sanity check)
> diff_count=0
> for f in docs/sprints/01-mvp/status/*.md; do
>   base=$(basename "$f")
>   if [ -f "docs/status/$base" ] && ! cmp -s "$f" "docs/status/$base"; then
>     echo "DIFERE (não deveria): $base"
>     diff_count=$((diff_count+1))
>   fi
> done
> if [ $diff_count -ne 0 ]; then echo "ABORTAR: $diff_count diferenças inesperadas"; exit 1; fi
>
> # 2. Apagar pasta legada
> rm -rf docs/status/
>
> # 3. Verificar
> ls docs/ | grep -q "^status$" && echo "ERRO: pasta ainda existe" || echo "OK pasta apagada"
> git status
> ```

## Referências

- ADR 0010 (`docs/decisions/0010-organizacao-doc-por-sprint.md`).
- ADR 0007 (`docs/decisions/0007-reporting-com-gates-e-status-report-como-output-contract.md`) — schema do status report; convencional.
- RETRO-02 §4, §8 ação #2 (`docs/retrospectivas/RETRO-02-canal-whatsapp.md`).
- Sessão 2026-05-30 — inventário (45 idênticos, 1 divergente, DEP-07 + _TEMPLATE só na pasta legada).
- `docs/scripts/metricas_status.py` — script que revelou o problema da duplicação.
