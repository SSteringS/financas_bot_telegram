# DISPATCH — FIX-002 CI aceitar integration/* (single-task)

> **Urgente.** O PR `integration/03-folha-pagamento → develop` está bloqueado pelo job
> `branch-name` do CI. Esta task desbloqueia o fechamento da sprint 03.
> Vai **direto pra `develop`** — fix não passa por integration (ver CLAUDE.md §Fluxo de branches).

---

## Pré-condições (git)

- Nenhuma. Pode iniciar imediatamente a partir de `develop`.

---

## O prompt (cole tudo numa sessão `--agent backend`)

```
Task: FIX-002 — Corrigir ci.yml para aceitar integration/* no gate de branch.

Leia o plano:
  docs/sprints/03-folha-pagamento/plans/FIX-002-ci-aceitar-integration-no-gate-de-branch.md

## BRANCH

  git fetch
  git checkout -b fix/002-ci-aceitar-integration-no-gate-de-branch origin/develop

## A TASK

Editar APENAS `.github/workflows/ci.yml`. Três mudanças cirúrgicas:

### Mudança 1 — Regex do job `branch-name`

Localizar o step "Validar convenção de branch". Trocar:

  ^(feature|fix|hotfix)/[a-z0-9]+(-[a-z0-9.]+)*$

Por:

  ^(feature|fix|hotfix|integration)/[a-z0-9]+(-[a-z0-9.]+)*$

### Mudança 2 — Trigger `pull_request`

Localizar:
  pull_request:
    branches: [develop]

Trocar por:
  pull_request:
    branches: [develop, 'integration/**']

### Mudança 3 — Trigger `push`

Localizar:
  push:
    branches:
      - 'feature/**'
      - 'fix/**'
      - 'hotfix/**'

Adicionar uma linha:
      - 'integration/**'

## REGRAS DURAS

1. Branch: `fix/002-ci-aceitar-integration-no-gate-de-branch` saindo de `origin/develop`.
2. Território: SÓ `.github/workflows/ci.yml`. Zero outros arquivos.
3. 1 commit: `fix(CI): aceitar integration/* no gate de branch e nos triggers`.
4. NÃO mergeie. PR pra `develop` após status report + Reviewer.
   (Fix vai direto pra develop — não passa por integration.)

## VALIDAÇÃO MENTAL (antes de commitar)

Rodar mentalmente o regex novo contra estes casos:
  - feature/be-024-entidades-jpa-repositorios-folha  → deve PASSAR ✓
  - fix/001-whatsapp-defaults-deploy-safe             → deve PASSAR ✓
  - hotfix/001-document-vs-photo                     → deve PASSAR ✓
  - integration/03-folha-pagamento                   → deve PASSAR ✓ (era o problema)
  - main                                             → deve FALHAR ✓
  - develop                                          → deve FALHAR ✓

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/FIX-002-ci-aceitar-integration-no-gate-de-branch.md`

testes_novos: 0 (sem código de produção — validação é o CI verde no próprio PR).

Pare ao final. Não mergeie.
```

---

## Notas pro humano

- **Estimativa:** 10 minutos.
- **Após merge:** PR `integration/03-folha-pagamento → develop` pode ser reaberto — o job `branch-name` vai passar.
- **O que o CI vai fazer diferente após o fix:** também vai rodar em PRs feature→integration, dando feedback antes do merge na integration branch.

## Referências

- `docs/sprints/03-folha-pagamento/plans/FIX-002-ci-aceitar-integration-no-gate-de-branch.md`
- `.github/workflows/ci.yml` (arquivo a modificar)
