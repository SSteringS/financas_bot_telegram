# Avaliação — CI-01: Gate de CI no PR pra develop

**Data:** 2026-05-26  
**Reviewer:** claude-reviewer (sessão independente — ADR 0005)  
**Branch revisada:** `feature/ci-01-gate-pr-develop`  
**Status report:** `docs/sprints/01-mvp/status/CI-01.md`  
**Plano:** `docs/plans/CI-01-gate-pr-develop.md`

---

## Veredito

**Aprovado com observações** — dois campos do frontmatter com schema incorreto; sem bloqueante de código ou processo.

---

## O que foi verificado

### Gates verificados contra a realidade

| Gate | Reportado | Verificado | Resultado |
|---|---|---|---|
| `build` | `ok` | `mvn package -DskipTests` — não rodei separado; confiando no CI run verde `26470912614` + testes compilando | ✓ |
| `lint` | `na` | Back não tem linter configurado | ✓ |
| `testes` | `ok` | `mvn test` local: 207 unitários verdes, 19 erros de integração por Docker ausente localmente. Docker não está rodando nesta máquina — confirmado por `Could not find a valid Docker environment`. CI run `26470912614` é a evidência autoritativa: todos os 4 jobs verdes. | ✓ |
| `branch_convencao` | `ok` | `git rev-parse --abbrev-ref HEAD` → `feature/ci-01-gate-pr-develop`. `git merge-base --is-ancestor origin/develop HEAD` → saiu de develop. PRE-MERGE-CHECKLIST regex `^feature/(be\|fe\|dep\|fix\|hotfix\|evo\|ci)-\d+[a-z]?-` → bate. | ✓ |
| `territorio` | `ok` | `git diff --name-only origin/develop...HEAD` → `.github/workflows/ci.yml` (back territory ✓), `financas_bot_telegram/src/test/...` (back territory ✓), `docs/sprints/01-mvp/status/CI-01.md` (shared ✓). `deploy.yml` não tocado (confirmado via `git diff`). | ✓ |

### Diff vs. plano

O `ci.yml` criado é **cópia fiel do plano** — nenhuma divergência de conteúdo. Verificado linha a linha.

### Correção do teste (`ba19455`)

`PedidoControllerListarTest.java:41` — parâmetro `StatusPedido.PAGO` → `"PAGO"` (string). Correto: o controller agora recebe `String` e chama `parseStatus()` internamente. A asserção na linha 47 (`filtro.status()).isEqualTo(StatusPedido.PAGO)`) permanece válida — verifica que `parseStatus("PAGO")` retorna o enum esperado. Desvio documentado e justificado corretamente.

---

## Issues encontradas

### Bloqueantes

Nenhum.

### Observações a corrigir antes do merge

**1. `testes_total: na` — schema incorreto**

O template define este campo como `int` (ex: `67`). A suite rodou 226 testes no CI (todos verdes no run `26470912614`). O valor correto é `226`. `na` não é válido para campos numéricos do frontmatter — deixa o campo não-parseável por script futuro.

**2. `testes_novos: na` — schema incorreto**

Pelo mesmo motivo: campo é `int`. Nesta task não foram adicionados testes novos (apenas um teste pré-existente foi corrigido), portanto o valor correto é `0`. `na` deve ser reservado para gates ok/fail/na, não para contadores.

---

## Observações não-bloqueantes

**3. `commits` lista 2 de 3**

O frontmatter lista `[23bc3d0, ba19455]`. A branch tem 3 commits:
- `23bc3d0` — feat: ci.yml
- `ba19455` — fix: PedidoControllerListarTest
- `bc11371` — docs: status CI-01

O terceiro é o próprio status report — não dá pra incluir o hash de um commit que ainda não existia. Aceitável; vale o registro.

**4. Regex CI ≠ regex PRE-MERGE-CHECKLIST (intencional, mas lacuna aberta)**

O `branch-name` job no `ci.yml` usa `^(feature|fix|hotfix)/[a-z0-9]+(-[a-z0-9.]+)*$` — não valida o padrão `<area>-<id>-<slug>`. O PRE-MERGE-CHECKLIST exige `^feature/(be|fe|dep|fix|hotfix|evo|ci)-\d+[a-z]?-`. O plano documenta isso como limitação da fase 1 com follow-up `CI-02`. A lacuna é intencional, mas não foi taskificada ainda.

---

## Critérios de aceitação do plano

| Critério | Atendido? |
|---|---|
| `ci.yml` criado | ✓ |
| Dispara em PR pra `develop` e push de feature | ✓ |
| Back roda quando `financas_bot_telegram/**` muda (paths-filter) | ✓ |
| Front roda quando `frontend/**` muda (paths-filter) | ✓ |
| Valida nome da branch | ✓ |
| PR de `docs/` puro não dispara back/front | ✓ (só `branch-name`) |
| Testado de verdade — run verde confirmado | ✓ (run `26470912614`) |

---

## Ação requerida

Corrigir no status report:
- `testes_total: na` → `testes_total: 226`
- `testes_novos: na` → `testes_novos: 0`

Pode ser um commit de fixup no status report na mesma branch antes de abrir o PR.
