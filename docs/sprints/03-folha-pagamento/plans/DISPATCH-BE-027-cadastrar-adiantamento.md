# DISPATCH — BE-027-cadastrar-adiantamento (single-task)

> **Quando usar:** após BE-026 mergeada em `develop` (não em paralelo com ela).
> O `FolhaController.java` foi criado por BE-026 — esta task apenas adiciona endpoints nele.
> ⚠️ Serialização intencional: o plano original dizia "paralelo com BE-026", mas como ambas tocam
> `FolhaController.java`, o dispatch resolve serializando para evitar conflito de merge.

---

## Pré-condições (git)

- **`feature/be-024-entidades-jpa-repositorios-folha` mergeada em `develop`** — ports de Adiantamento precisam existir.
- **`feature/be-026-cadastrar-vale` mergeada em `develop`** — FolhaController.java deve existir para esta task adicionar endpoints nele sem conflito.
  Confirmar ambas: `git log origin/develop --oneline | grep -E "be-024|be-026"`.

---

## O prompt (cole tudo numa sessão `--agent backend`)

```
Task: BE-027 — CadastrarAdiantamento + endpoints adiantamentos + cancelamento.

Localize e leia o plano completo (initialPrompt orienta o Glob).
Leia também:
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §3 e §5
- docs/decisions/0016-evo09-folha-pagamento.md §4
- FolhaController.java existente (criado em BE-026 — já está em develop)

## A TASK

Adiantamento é um plano de desconto parcelado vinculado a um funcionário.

Criar:
1. CadastrarAdiantamentoPortIn + CancelarAdiantamentoPortIn (em application/port/in/).
2. CadastrarAdiantamentoUseCase + CancelarAdiantamentoUseCase (em application/usecase/).
3. DTOs: AdiantamentoRequest.java, AdiantamentoResponse.java.

Modificar:
4. FolhaController.java (já existe de BE-026) — ADICIONAR endpoints de adiantamento:
   - POST   /api/funcionarios/{id}/adiantamentos
   - GET    /api/funcionarios/{id}/adiantamentos
   - DELETE /api/adiantamentos/{adiantamentoId}

## REGRAS DURAS

1. Branch: `feature/be-027-cadastrar-adiantamento` saindo de `origin/develop`
   (develop já tem BE-026 com FolhaController.java).
2. Território: SÓ `financas_bot_telegram/`. Zero `frontend/`.
3. FolhaController.java JÁ EXISTE — apenas adicionar os 3 endpoints de adiantamento. Não reescrever.
4. Validação matemática NO use case (antes do banco):
   |valor_total - valor_parcela * num_parcelas| <= 0.01 → se falhar, rejeitar com 400.
5. DELETE de adiantamento: soft (ativo=false). Se já quitado (parcelas_pagas == num_parcelas) → 409.
6. 1 commit: `feat(BE-027): CadastrarAdiantamento + endpoints adiantamentos`.
7. NÃO mergeie. PR pra develop após status report + Reviewer.

## DECISÃO A TOMAR

**GET /api/funcionarios/{id}/adiantamentos:** retorna apenas ativos (padrão) ou incluir inativos via `?incluirInativos=true`?
Decidir com base no que o front (FE-016) vai precisar. Se incerto, usar ativos por padrão e documentar no status report.

## TESTES

- Unitários (mínimo 4): 
  CadastrarAdiantamentoUseCase: plano consistente (ok), plano inconsistente (400).
  CancelarAdiantamentoUseCase: cancelar ativo (ok), cancelar quitado (409).
- Integração (mínimo 2): POST cria, DELETE cancela.

`testes_novos` ≥ 6.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/BE-027-cadastrar-adiantamento.md`

Incluir:
- Decisão sobre `?incluirInativos` (adotado ou não) + justificativa.
- Confirmação que a validação matemática está no use case (com tolerância 0.01) e não apenas no banco.

## SE QUEBRAR

Cenário — FolhaController.java NÃO existe em develop:
  BE-026 não foi mergeada ainda. PARE. Verificar pré-condição git antes de continuar.

Cenário — conflito ao adicionar no FolhaController (outro campo/endpoint conflitante):
  Resolver o conflito localmente, anotar no status report e continuar.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano

- **Serializada após BE-026** (intencional — evita conflito em FolhaController.java).
- **Estimativa:** 1h. Estrutura análoga a BE-026.
- **Após merge desta + merge de BE-025:** BE-028 pode iniciar (precisa de ambas as ports: vale E adiantamento).
  BE-028 depende de BE-024 + BE-026 + BE-027 — aguardar as três antes de despachar.

## Referências

- `docs/sprints/03-folha-pagamento/plans/BE-027-cadastrar-adiantamento.md`
- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §3, §5
