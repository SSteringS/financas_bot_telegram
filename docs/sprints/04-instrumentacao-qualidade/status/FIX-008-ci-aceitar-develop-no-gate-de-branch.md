---
task: FIX-008
titulo: "Aceitar develop no gate de convenção de branch do CI"
data: 2026-08-12
branch: fix/008-ci-aceitar-develop-no-gate-de-branch
responsavel: claude-plan
estado: parcial
gates:
  build: na
  lint: na
  testes: na
  testes_total: 0
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: excecao-autorizada
commits:
  - 2a37299
  - 7cddde7
pr: https://github.com/SSteringS/financas_bot_telegram/pull/126
desvios: 1
pendencias_humano: 1
---

# FIX-008 — Aceitar `develop` no gate de convenção de branch do CI

> **Por que `estado: parcial`:** o PR #126 está aberto e não mergeado. Não há gate falhando nem pendência técnica — falta apenas o merge, que é ação do humano. Vira `concluido` quando mergear.

---

## O que foi feito

O gate `branch-name` do `ci.yml` rejeitava o PR #125 (`develop` → `integration/04`) com `Branch 'develop' fora da convenção`. A regex de `ci.yml:38` exige prefixo `feature|fix|hotfix|integration`, e em `pull_request` o `github.head_ref` vale `develop`.

Adicionado early-exit que aceita `develop` antes da regex, com o raciocínio comentado no próprio arquivo. Em commit separado, o caminho `develop → integration` foi documentado no `CLAUDE.md`, que descrevia só três caminhos de fluxo e não previa o sync.

**A liberação não afrouxa o gate, e isso é demonstrável:** o workflow só dispara em `pull_request` com base `develop` ou `integration/**` (`ci.yml:5`), e PR de `develop` para `develop` não existe — logo `head == develop` **implica** `base == integration/**` por construção. O bloco `push` (`ci.yml:7-11`) não lista `develop`, então `github.ref_name` nunca chega ao check valendo `develop`. A permissão nasce escopada no caso legítimo sem precisar de condicional sobre a base.

### Verificação executada antes do commit

| O quê | Como | Resultado |
|---|---|---|
| YAML válido | `python -c "yaml.safe_load(...)"` | parseia |
| Lógica do gate | simulação em bash contra 12 nomes de branch | 12/12 conforme esperado |
| Gate no CI real | `gh pr checks 126` | `branch-name` **pass** em 2s |

Aceitas na simulação: `develop`, `feature/qa-013-…`, `fix/008-…`, `integration/04-…`, `hotfix/001-…`.
Rejeitadas: `main`, `Develop`, `develop-x`, `develop/foo`, `minha-branch`, `feature/`.

---

## Desvios do plano

**1 desvio — não houve plano.** A correção foi executada direto, a partir de autorização explícita do humano no chat, sem plano em `docs/sprints/*/plans/`. O humano optou pela rota (a) — planner aplica direto — sobre a rota (b) — plano + dispatch para o `backend` com Reviewer — que era a recomendação do planner e o precedente da FIX-002.

Justificativa registrada do humano: *"é uma alteração rápida e não mexe no código da aplicação"*.

**Consequências a declarar, não a esconder:**

- **Sem revisão independente.** A ADR 0005 torna o Reviewer obrigatório para mudança de código antes do merge. Esta task não passou por ele. Mitigação parcial: a lógica foi simulada contra 12 casos e o gate real foi verificado verde no PR — mas simulação do autor não substitui revisão adversarial.
- **Território `.github/workflows/` é do `claude-back`.** A escrita aqui foi feita pelo planner sob autorização explícita e específica, na forma que o `CLAUDE.md` exige ("pedida na hora e para a mudança específica"). Por isso `territorio: excecao-autorizada` em vez de `ok`.

---

## Decisões tomadas durante a execução

- **Early-exit em vez de somar `develop` à alternância da regex.** A regex descreve **formato de slug**; `develop` é caso nomeado com motivo próprio. Dentro da alternância viraria uma exceção sem contexto, e o comentário no arquivo existe justamente para impedir que a próxima pessoa a remova por parecer gratuita.
- **Dois commits separados.** `2a37299` corrige o CI; `7cddde7` documenta o fluxo no `CLAUDE.md`. Separados para que a documentação seja descartável sem perder a correção.
- **Worktree temporário para não tirar o planner de `develop`.** O `CLAUDE.md` fixa o worktree do planner em `develop`, então `git checkout -b` ali quebraria o invariante. Criado `finbot-fix-008` como worktree efêmero, usado e removido. O `git worktree remove` falhou com lock do Windows (`Permission denied`); o diretório foi removido manualmente e o estado final conferido — dois worktrees registrados, planner em `develop`, working tree limpo.
- **Bug de locale não corrigido** — ver §Próximos passos.

---

## Decisões pendentes (esperando humano)

**1 pendência.**

1. **Mergear o PR #126 e, em seguida, o #125.** Nesta ordem: o #125 só passa no gate depois que a correção estiver em `develop`. Nenhum dos dois foi mergeado.

---

## Próximos passos / observações pro próximo

**É a segunda ocorrência da mesma classe de falha.** A **FIX-002** foi literalmente `ci-aceitar-integration-no-gate-de-branch`, na mesma linha do mesmo arquivo. O padrão: **a allowlist do gate codifica o fluxo de branches, e ninguém a atualiza quando o fluxo ganha um caminho novo.** É por isso que o commit do `CLAUDE.md` faz parte desta FIX — corrigir só o gate deixaria a armadilha montada para o próximo caminho.

**Bug pré-existente encontrado e deliberadamente não corrigido:** a faixa `[a-z0-9]` da regex é **sensível a locale**.

| `LC_COLLATE` | `feature/QA-013` |
|---|---|
| `en_US.UTF-8` | **ACEITA** ⚠️ |
| `C` / `C.UTF-8` | rejeita |

Sob collation `en_US`, `[a-z]` casa maiúscula por ordem de collation. Ou seja, a convenção lowercase **pode não estar sendo aplicada**, dependendo do locale do runner — e isso muda em silêncio se a imagem do GitHub Actions trocar o default. Não foi introduzido nesta task e corrigi-lo aqui seria scope creep. Fix natural: `[[:lower:][:digit:]]` ou `LC_ALL=C` fixado no step. Registrado em `docs/PENDENCIAS-TECNICAS.md`.

---

## Padrões técnicos

Não se aplica — a mudança é configuração de CI e documentação de fluxo, sem lógica de produção.

---

## Arquivos criados/modificados

- `.github/workflows/ci.yml` (modificado: early-exit aceitando `develop` no job `branch-name`, com o raciocínio comentado)
- `CLAUDE.md` (modificado: documenta o PR de sincronização `develop → integration` no §Fluxo de branches, no diagrama e na nota)
