# CI-01 — Gate de CI no caminho pra `develop`

## Contexto

Hoje o único workflow (`deploy.yml`) roda **só em push pra `main`** — ou seja, **não existe gate algum no caminho pra `develop`**, que é onde a revisão deveria travar. Os gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` são disciplina manual. Conforme o volume cresce, disciplina escorrega. Esta task torna os gates **enforced** por CI. Decisão registrada no ADR `0004`.

> **Nota de taxonomia:** esta task introduz o prefixo `CI-` (tooling/processo), fora do conjunto BE/FE/DEP/FIX/HOTFIX/EVO. O gate **não** valida o prefixo do task-id na fase 1 (pra não criar problema de bootstrap). Atualizar o regex de task-id no `PRE-MERGE-CHECKLIST` pra incluir `CI` fica como follow-up pequeno.

## Branch

`feature/ci-01-gate-pr-develop` — nova, a partir de `develop`. Território: `.github/workflows/` → **Claude do back**.

## Dependência de processo (importante — ler antes)

Um workflow de CI só **trava merge** de verdade se duas coisas existirem:

1. Os merges pra `develop` passarem a ser via **Pull Request** (hoje o humano mergeia localmente — isso **não dispara** Action de PR).
2. **Branch protection** em `develop` exigindo o check do CI passar antes do merge.

Sem isso, o workflow roda mas é só informativo. **Decisão de processo a confirmar com o humano:** adotar PR→`develop` + branch protection. Enquanto não adotar, o gate roda em **push pra branches de feature** (ainda útil: avisa antes do merge), mas não é bloqueante.

Por isso o trigger cobre os dois cenários (PR e push de feature).

## Escopo

**Fase 1 (esta task):** build + lint + testes (back e front) + checagem de nome de branch.
**Fase 2 (follow-up, fora desta task):** validar que o PR adiciona/altera um `docs/sprints/<NN>/status/<TASK>.md` com frontmatter válido (gates) — exige script próprio.

## Arquivo: `.github/workflows/ci.yml` (criar)

```yaml
name: CI Gate (develop)

on:
  pull_request:
    branches: [develop]
  push:
    branches:
      - 'feature/**'
      - 'fix/**'
      - 'hotfix/**'

jobs:
  # Detecta o que mudou pra rodar só o necessário
  changes:
    runs-on: ubuntu-latest
    outputs:
      backend: ${{ steps.filter.outputs.backend }}
      frontend: ${{ steps.filter.outputs.frontend }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            backend:
              - 'financas_bot_telegram/**'
            frontend:
              - 'frontend/**'

  branch-name:
    runs-on: ubuntu-latest
    steps:
      - name: Validar convenção de branch
        run: |
          REF="${{ github.head_ref || github.ref_name }}"
          echo "Branch: $REF"
          if [[ ! "$REF" =~ ^(feature|fix|hotfix)/[a-z0-9]+(-[a-z0-9.]+)*$ ]]; then
            echo "::error::Branch '$REF' fora da convenção (feature/<slug>, fix/<slug>, hotfix/<slug>)."
            exit 1
          fi

  backend:
    needs: changes
    if: needs.changes.outputs.backend == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'
      - name: Testes (back)
        run: mvn test -f financas_bot_telegram/pom.xml
      - name: Build JAR (back)
        run: mvn package -DskipTests -f financas_bot_telegram/pom.xml

  frontend:
    needs: changes
    if: needs.changes.outputs.frontend == 'true'
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run lint
      - run: npm test
      - run: npm run build
```

### Notas de implementação

- **Testes do back sem DB externo:** o `mvn test` já roda assim no `deploy.yml`; os testes de integração (BE-14) sobem MySQL via Testcontainers, e o runner `ubuntu-latest` tem Docker. Espelhar o comando do `deploy.yml`.
- **`paths-filter`** evita rodar back num PR só de front e vice-versa. Se preferir simplicidade, dá pra remover o job `changes` e rodar back+front sempre — mais lento, mais simples. Manter o filtro é o recomendado.
- **Node 20** (LTS) cobre o Vite/React do projeto. Ajustar se o `package.json` exigir outra.
- O job `branch-name` roda em PR (`github.head_ref`) e em push (`github.ref_name`).

## Critérios de aceitação

- `.github/workflows/ci.yml` criado.
- Em PR pra `develop` (ou push numa branch de feature), o workflow dispara e:
  - roda testes + build do back **quando** `financas_bot_telegram/**` muda;
  - roda lint + test + build do front **quando** `frontend/**` muda;
  - valida o nome da branch.
- Um PR com teste quebrado / lint sujo / build falho fica **vermelho**.
- Um PR só de `docs/` não dispara back/front (só o `branch-name`).
- Testado de verdade: abrir um PR de teste (ou push numa branch de feature) e confirmar os jobs rodando e o resultado correto.

## Pós-implementação (decisão de processo, humano) — DECIDIDO (2026-05-26)

**Decisão do humano:** adotar **PR → `develop`** (abrir PR de cada feature branch; parar de mergear local). **Branch protection: adiada por ora** — o humano optou por *não* ligar agora.

Consequência prática: o `ci.yml` roda no PR e mostra verde/vermelho, mas **não bloqueia o merge** (é informativo). Travar o merge depende de ligar branch protection no GitHub, o que fica pra depois.

Ações:

- **Agora (back):** criar o `ci.yml` (esta task). O gate começa a rodar e dar feedback nos PRs.
- **Fluxo (humano):** abrir PR de cada feature branch pra `develop` em vez de merge local.
- **Adiado (humano, quando quiser tornar bloqueante):** ligar branch protection em `develop` exigindo os checks `backend`, `frontend`, `branch-name` + "require PR before merging".
- **Avaliar depois:** exigir o check também no PR `develop → main` (antes do deploy).

> Estado atual: gate **informativo**. A decisão de torná-lo bloqueante está tomada em princípio, mas a ativação (branch protection) foi adiada pelo humano.

## Coordenação

- Executado pelo Claude do **back** (`.github/workflows/` é território dele).
- **Atenção:** este workflow roda `mvn`/`npm` em CI mas **não faz deploy** — não tocar no `deploy.yml`.
- Status report em `docs/sprints/01-mvp/status/CI-01.md` com frontmatter válido (gates de código = `na` pro próprio workflow; a evidência é o print/descrição do run verde). **Não** mergear — parar pra revisão.
