# Avaliação — FE-13: Codegen de tipos via openapi-typescript

**Data:** 2026-05-26  
**Reviewer:** claude-reviewer (sessão independente — ADR 0005)  
**Branch revisada:** `feature/fe-13-codegen-tipos-openapi`  
**Status report:** `docs/status/FE-13-codegen-tipos-openapi.md`  
**Plano:** `docs/plans/FE-13-codegen-tipos-openapi.md`

---

## Veredito

**Reprovado — bloqueante de processo.**

O único bloqueante é a origem da branch. O código em si está correto e os critérios de aceitação de qualidade foram todos atendidos. A correção é operacional (rebase de 2 commits), não requer reescrever código.

---

## O que foi verificado

### Gates verificados contra a realidade

| Gate | Reportado | Verificado | Resultado |
|---|---|---|---|
| `build` | `ok` | `npm run build` → ✓ built in 3.46s, sem erro TS | ✓ |
| `lint` | `ok` | `npm run lint` → sem saída de erro (exit 0) | ✓ |
| `testes` | `ok` | `npm test -- --run` → 43 passed (10 test files), 0 failures | ✓ |
| `testes_total: 43` | `43` | Confirmado: exatamente 43 testes passando | ✓ |
| `testes_novos: 0` | `0` | Nenhum teste novo adicionado — correto | ✓ |
| `territorio` | `ok` | `git diff --name-only origin/develop...HEAD` → `frontend/`, `docs/status/` — território correto do front | ✓ |
| `branch_convencao` | `ok` | **FALHOU — ver abaixo** | ✗ |

### Drift check (critério de aceitação)

`npm run gen:types && git diff --exit-code src/api/tipos-gerados.ts` → exit 0. Regeneração a partir de `openapi.json` produz exatamente o arquivo commitado. ✓

### Campos FE-12 (critério de aceitação)

`ResumoMes` em `tipos.ts` deriva de `S['ResumoMesDTO']` e expõe `mes`, `todos`, `pendentes`, `pagos`. Sem `mesAtual`. O incidente que motivou a task está mitigado. ✓

### Qualidade do código

- `tipos.ts` como fachada: correto. Re-exporta/estreita do gerado sem mudar os imports do app. ✓
- Enums TS (`StatusPedido`, `TipoPagamento`) mantidos para runtime (o spec usa string literal union, não enum). Decisão documentada e correta. ✓
- `Pagina<T>` mantido genérico (o spec expõe `PaginaDTOPedidoResumoDTO` sem parâmetro de tipo). Decisão documentada e correta. ✓
- `CANCELADO` existe no spec mas não está em `StatusPedido` — comentado no código. Aceitável como decisão deliberada.
- `gen:api` usa `curl` diretamente (não `node`/`npx`) — suficiente e simples. ✓
- `openapi-typescript@6.7.6` (v7 requer Node ≥20.19, ambiente tem 20.15.0) — desvio documentado. ✓

---

## Bloqueante: `branch_convencao` — branch NÃO saiu de `develop`

### O problema

`git log --graph feature/fe-13-codegen-tipos-openapi` mostra:

```
* e2de52c docs(FE-13): status report
* 3aa3d62 feat(FE-13): codegen ...
* bc11371 docs: status CI-01 ...          ← commit da CI-01
* ba19455 fix(CI-01): corrige ...          ← commit da CI-01
* 23bc3d0 feat(CI-01): gate de CI ...     ← commit da CI-01
* f773c1b mudança no workflow ...
...
```

A FE-13 foi criada a partir de `feature/ci-01-gate-pr-develop` (após o commit `bc11371`), não de `develop`. O `git merge-base --is-ancestor origin/develop HEAD` retornou falso — `origin/develop` não é ancestral do HEAD da FE-13.

O status report reporta `branch_convencao: ok` — **incorreto**.

### A regra violada

CLAUDE.md: *"Uma tarefa = uma branch nova a partir de `develop`"* e *"Se uma tarefa exigir exceção... o plano deve declarar `> EXCEÇÃO DE BRANCH:`"*. Não há tal declaração no plano.

### Impacto prático no merge

Baixo: a CI-01 já está em `develop` (via merge commit `b754603`). Quando o PR da FE-13 for aberto, git reconhecerá que os commits do CI-01 já estão em develop e o merge será limpo. Mas o histórico ficará com os commits do CI-01 duplicados na linhagem da FE-13.

### Correção esperada

Rebase dos 2 commits de FE-13 em cima do `develop` atual:

```bash
git rebase origin/develop
# resolve conflitos, se houver (improvável — frontend/ não conflita com CI-01)
git push --force-with-lease origin feature/fe-13-codegen-tipos-openapi
```

Após o rebase, `git log --graph` deve mostrar apenas `3aa3d62` e `e2de52c` (+ commits de develop), sem os commits do CI-01 na linhagem da FE-13.

Atualizar `branch_convencao: ok` no status report (vai continuar ok — só o ponto de origem muda).

---

## Observações não-bloqueantes

**1. `commits` lista 1 de 2**

O frontmatter lista `[3aa3d62]`. O segundo commit (`e2de52c`, o status report) não pode ser incluído (hash desconhecido ao escrever o arquivo). Aceitável — mesmo padrão de CI-01.

**2. `openapi.json` inclui schemas Telegram**

O snapshot inclui centenas de schemas do Telegram Bot API (porque o back expõe `/webhook` e o springdoc introspeta tudo). Isso é barulho no arquivo mas não é bug — é consequência do back expor o endpoint de webhook. Considerar anotar no `gen:api` que o snapshot contém schemas de terceiros.

---

## Critérios de aceitação do plano

| Critério | Atendido? |
|---|---|
| `openapi-typescript` instalado como devDependency | ✓ |
| `npm run gen:types` gera `tipos-gerados.ts` sem erro | ✓ |
| `openapi.json` commitado | ✓ |
| `tipos.ts` deriva dos tipos gerados, build e testes continuam verdes | ✓ |
| Prova de não-drift: exit 0 | ✓ |
| Campos da FE-12: `mes`, `todos`, `pendentes`, `pagos` (sem `mesAtual`) | ✓ |
| README/comentário com ciclo de atualização | ✓ (cabeçalho de `tipos.ts`) |

---

## Ação requerida

1. `git rebase origin/develop` na branch `feature/fe-13-codegen-tipos-openapi`.
2. `git push --force-with-lease`.
3. Verificar que `git merge-base --is-ancestor origin/develop HEAD` retorna verdadeiro pós-rebase.
4. Nenhuma alteração de código necessária — só histórico.
