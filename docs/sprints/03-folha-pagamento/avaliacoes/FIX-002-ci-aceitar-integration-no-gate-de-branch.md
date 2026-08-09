---
task: FIX-002
sprint: 03-folha-pagamento
data: 2026-06-03
avaliador: qa-test-specialist
status_report: docs/sprints/03-folha-pagamento/status/FIX-002-ci-aceitar-integration-no-gate-de-branch.md
plano: docs/sprints/03-folha-pagamento/plans/FIX-002-ci-aceitar-integration-no-gate-de-branch.md
veredito_codigo: aprovado
veredito_final: aprovado
observacoes_count: 1
roteiro_executado: true
gates_verificados_contra_realidade: ok
skills_eficazes: []
skills_gaps: []
---

# Avaliação — FIX-002 CI aceitar `integration/*` no gate de branch e nos triggers

**Branch:** `fix/002-ci-aceitar-integration-no-gate-de-branch`
**PR:** https://github.com/SSteringS/financas_bot_telegram/pull/89
**Base:** `develop`
**Implementador:** claude-back
**Commit:** `df1fb95` (status report aponta `cc002a5` — mesmo conteúdo; ver observação 1)

---

## Veredito

**APROVADO.** Mudança cirúrgica, aditiva, sem regressão e que destrava o gate humano de fim de sprint. Recomendo merge.

---

## O que foi entregue (verificado contra a árvore de `fix/002-...`)

### Arquivos modificados
- `.github/workflows/ci.yml` — 5 linhas alteradas no total (+4/-2 efetivos), agrupadas em 3 pontos cirúrgicos.
- `docs/sprints/03-folha-pagamento/status/FIX-002-...md` — status report novo.

Confirmado via `git diff develop...fix/002-...` que **só estes 2 arquivos foram tocados**. Território OK.

### Diff verificado (ci.yml)

```diff
 on:
   pull_request:
-    branches: [develop]
+    branches: [develop, 'integration/**']
   push:
     branches:
       - 'feature/**'
       - 'fix/**'
       - 'hotfix/**'
+      - 'integration/**'
 ...
-          if [[ ! "$REF" =~ ^(feature|fix|hotfix)/[a-z0-9]+(-[a-z0-9.]+)*$ ]]; then
-            echo "::error::Branch '$REF' fora da convenção (feature/<slug>, fix/<slug>, hotfix/<slug>)."
+          if [[ ! "$REF" =~ ^(feature|fix|hotfix|integration)/[a-z0-9]+(-[a-z0-9.]+)*$ ]]; then
+            echo "::error::Branch '$REF' fora da convenção (feature/<slug>, fix/<slug>, hotfix/<slug>, integration/<slug>)."
             exit 1
           fi
```

Bate com o plano: regex + 2 triggers, conforme especificado.

---

## Roteiro executado (verificação adversarial)

### 1. Território
Confirmado: `git diff develop...fix/002 --name-only` retorna **apenas** `.github/workflows/ci.yml` e o status report em `docs/sprints/03-folha-pagamento/status/`. Nenhum arquivo fora do território.

### 2. Validação do regex contra a tabela do dispatch
Rodei o pattern novo (`^(feature|fix|hotfix|integration)/[a-z0-9]+(-[a-z0-9.]+)*$`) em bash contra um conjunto de branches reais:

| Branch | Esperado | Resultado |
|---|---|---|
| `feature/be-024-entidades-jpa-repositorios-folha` | PASSA | PASSA |
| `fix/001-whatsapp-defaults-deploy-safe` | PASSA | PASSA |
| `hotfix/001-document-vs-photo` | PASSA | PASSA |
| `integration/03-folha-pagamento` | PASSA (era o bug) | PASSA |
| `integration/03-evo-09` | PASSA | PASSA |
| `feature/qa-001-setup-playwright-base` | PASSA | PASSA |
| `fix/002-ci-aceitar-integration-no-gate-de-branch` | PASSA | PASSA |
| `main` | FALHA | FALHA |
| `develop` | FALHA | FALHA |

Regex está exatamente correto. Não introduz nem falso positivo nem falso negativo nos casos conhecidos.

### 3. YAML válido (parseado com PyYAML)
```
on.pull_request.branches = ['develop', 'integration/**']
on.push.branches        = ['feature/**', 'fix/**', 'hotfix/**', 'integration/**']
jobs                    = ['changes', 'branch-name', 'backend', 'frontend']
```
Estrutura preservada — os 4 jobs originais continuam declarados, indentação consistente, sem tabs, sem CRLF misturado.

### 4. Regressão em `backend` / `frontend`
Os jobs `backend` e `frontend` continuam dependendo de `needs: changes` com `if: needs.changes.outputs.<area> == 'true'`. O job `changes` usa `dorny/paths-filter@v3` com filtros em `financas_bot_telegram/**` e `frontend/**`. **Conclusão:** o aumento de cobertura dos triggers (passa a disparar em PRs `feature → integration`) **não** se traduz em desperdício de Actions minutes nos jobs caros — eles continuam pulando quando o diff não toca a respectiva área. Risco do plano (linha 145) corretamente endereçado pela arquitetura preexistente.

### 5. Branch e commit
- Branch nasce de `develop` (confirmado por `git merge-base develop fix/002-... = ec9000f`, que está em develop). OK.
- Nome segue convenção `fix/NNN-<slug>` (`fix/002-ci-aceitar-integration-no-gate-de-branch`). OK.
- 1 commit único na branch (`df1fb95`), com prefixo `fix(CI):` conforme dispatch. OK.

### 6. Status report
Frontmatter válido conforme `_TEMPLATE-status.md`:
- `estado: concluido`
- gates `build/lint/testes/cobertura_pct = na` (correto — task de infra sem código)
- `testes_total: 0`, `testes_novos: 0` (correto)
- `branch_convencao: ok`, `territorio: ok`
- `desvios: 0`, `pendencias_humano: 0`

Conteúdo descritivo bate com o diff (3 mudanças cirúrgicas) e com a tabela de validação do regex.

### 7. Critérios de aceitação do plano
Todos os 9 critérios do plano (linhas 121-129) foram cumpridos pelo diff. Não validei diretamente o "CI verde" em runtime (isso só acontece no GitHub Actions ao push), mas o conteúdo do workflow está sintaticamente e semanticamente correto.

---

## Observações

### Observação 1 — Hash do commit no status report está defasado (cosmético, sem impacto)

O status report (linha 18) registra `commits: [cc002a5]`, mas a HEAD atual da branch é `df1fb95`. Inspecionei `cc002a5` no objeto store e confirmei que **é exatamente o mesmo conteúdo de commit** (mesmo autor, data, mensagem e stat de arquivos) — só que o hash mudou depois de algum rebase/recompose interno antes do push final. Não há divergência de conteúdo entre o que está documentado e o que está na branch.

**Severidade:** baixa. Não justifica REJEITAR. Sugiro apenas que, em FIX futuros, o status report seja escrito depois do último `git push` para garantir que o hash já esteja estável. Pode ser corrigido em um amend ou no merge, mas não é bloqueio.

---

## Riscos avaliados

| Risco | Status |
|---|---|
| Regex aceitar branch mal-formada com prefixo `integration` | Mitigado — `[a-z0-9]+(-[a-z0-9.]+)*` continua validando o slug |
| Mais consumo de Actions minutes pelo trigger ampliado | Mitigado — `paths-filter` no job `changes` continua gateando `backend`/`frontend` |
| Quebrar PRs em aberto que ainda dependem do regex antigo | Não há — `integration` é prefixo novo, não tinha PR usando antes deste FIX |
| Falsos negativos em branches existentes | Validado contra tabela representativa — zero falhas |

---

## O que não foi avaliado (fora de escopo do Reviewer)

- Comportamento em runtime do GitHub Actions — só observável após push do FIX para `develop`. A validação aqui foi estática (parsing YAML + simulação local do regex).
- Branch protection rules — explicitamente fora de escopo do plano (linha 135).

---

## Decisão final

**APROVADO PARA MERGE.**

Mudança aditiva, cirúrgica, alinhada ao dispatch, com diff de 5 linhas em 1 arquivo de workflow, regex validado, YAML parseável, sem regressão nos jobs existentes, e que destrava o gate humano `integration → develop` da sprint 03. Status report válido. Único débito é cosmético (hash do commit no frontmatter está defasado por um rebase — mesmo conteúdo).
