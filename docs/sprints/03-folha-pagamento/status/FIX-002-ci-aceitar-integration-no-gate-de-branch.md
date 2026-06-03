---
task: FIX-002
titulo: "CI aceitar integration/* no gate de branch e nos triggers"
data: 2026-06-03
branch: fix/002-ci-aceitar-integration-no-gate-de-branch
responsavel: claude-back
estado: concluido
gates:
  build: na
  lint: na
  testes: na
  testes_total: 0
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - cc002a5
pr: null
desvios: 0
pendencias_humano: 0
---

# FIX-002 — CI aceitar `integration/*` no gate de branch e nos triggers

---

## O que foi feito

Uma única mudança em `.github/workflows/ci.yml` com 3 pontos cirúrgicos:

**Mudança 1 — Regex do job `branch-name`:**
```diff
-if [[ ! "$REF" =~ ^(feature|fix|hotfix)/[a-z0-9]+(-[a-z0-9.]+)*$ ]]; then
+if [[ ! "$REF" =~ ^(feature|fix|hotfix|integration)/[a-z0-9]+(-[a-z0-9.]+)*$ ]]; then
```
Permite que PRs com `head_ref = integration/<slug>` passem no gate de convenção de branch.

**Mudança 2 — Trigger `pull_request`:**
```diff
-    branches: [develop]
+    branches: [develop, 'integration/**']
```
CI agora dispara também em PRs `feature → integration`, fechando o feedback loop nesse passo.

**Mudança 3 — Trigger `push`:**
```diff
     - 'hotfix/**'
+    - 'integration/**'
```
CI roda em pushes diretos à integration (ex.: sincronização develop→integration pelo planner).

---

## Validação mental do regex

| Branch | Resultado esperado |
|---|---|
| `feature/be-024-entidades-jpa-repositorios-folha` | ✅ PASSA |
| `fix/001-whatsapp-defaults-deploy-safe` | ✅ PASSA |
| `hotfix/001-document-vs-photo` | ✅ PASSA |
| `integration/03-folha-pagamento` | ✅ PASSA (era o problema) |
| `main` | ✅ FALHA (correto) |
| `develop` | ✅ FALHA (correto) |

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

Nenhuma decisão adicional necessária — as 3 mudanças estavam completamente especificadas no plano.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- Após merge deste FIX em `develop`, o PR `integration/03-folha-pagamento → develop` pode ser aberto sem CI falhar no job `branch-name`.
- Este fix desbloqueava: BE-024, BE-025, BE-026, BE-027, BE-028, BE-029, FE-015, FE-016, FE-017.

---

## Padrões e decisões técnicas

Tarefa de CI/infra — sem lógica de aplicação. Não aplicável a padrões SOLID/hexagonal.

A mudança é **aditiva e não-destrutiva**: apenas novos prefixos aceitos no regex e novos branches nos triggers. Jobs `backend` e `frontend` continuam protegidos pelo `paths-filter` do job `changes` — não há risco de desperdício de Actions minutes por rodar CI desnecessariamente.

---

## Arquivos criados/modificados

- `.github/workflows/ci.yml` (modificado: 3 mudanças pontuais — regex + 2 triggers)
