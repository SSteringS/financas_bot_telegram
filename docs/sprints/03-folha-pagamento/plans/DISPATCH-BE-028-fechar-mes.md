# DISPATCH — BE-028-fechar-mes (single-task)

> **Quando usar:** após BE-024, BE-026 E BE-027 **todas mergeadas** em `develop`.
> FecharMesUseCase depende dos ports de vales (BE-026) e adiantamentos (BE-027) que precisam existir.

---

## Pré-condições (git)

Confirmar as três antes de despachar:
```
git log origin/develop --oneline | grep -E "be-024|be-026|be-027"
```
Todos os três devem aparecer. Se algum faltar, aguardar o merge.

---

## O prompt (cole tudo numa sessão `--agent backend`)

```
Task: BE-028 — FecharMesUseCase + endpoint de fechamento.

Localize e leia o plano completo (initialPrompt orienta o Glob).
Leia também:
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §4 (algoritmo de 11 passos — fonte canônica)
- docs/decisions/0016-evo09-folha-pagamento.md §3 (idempotência)
- docs/architecture/estado-atual-dev.md (verificar requisitante_id)
- FolhaController.java existente (criado em BE-026, expandido em BE-027)
- PedidoPagamentoEntity.java (verificar campo requisitante_id)

## A TASK

Implementar o algoritmo central da folha de pagamento: fechar o mês de um funcionário.

### PASSO ZERO — verificar requisitante_id (ANTES de escrever qualquer use case)

Abrir PedidoPagamentoEntity.java e verificar:
  grep -n "requisitanteId\|requisitante_id" financas_bot_telegram/src/main/java/.../entity/PedidoPagamentoEntity.java

Se `requisitante_id` for NOT NULL sem default:
  Criar migration V6b__folha_requisitante_nullable.sql:
    ALTER TABLE pedidos_pagamento MODIFY COLUMN requisitante_id BIGINT NULL;
  Ou avaliar workaround (sentinel value). Registrar a decisão no status report ANTES de implementar passo 8.

Não prosseguir para o use case sem resolver isso — a transação vai falhar no passo 8 com ConstraintViolation.

### Criar:
1. FecharMesPortIn + ConsultarFolhaPortIn (em application/port/in/).
2. FecharMesUseCase (em application/usecase/) — algoritmo de 11 passos, @Transactional.
3. ConsultarFolhaUseCase.
4. FechamentoDuplicadoException + FuncionarioNaoEncontradoException (em domain/exception/ se não existirem).
5. DTOs: FecharMesRequest, PedidoFolhaResponse.

### Modificar:
6. FolhaController.java — adicionar:
   - POST /api/funcionarios/{id}/fechamentos (body: {mes: "2026-05", ajuste: 0.00})
   - GET  /api/funcionarios/{id}/fechamentos
7. Exception handler global — mapear FechamentoDuplicadoException → 409.

### Os 11 passos do FecharMesUseCase (implementar nesta ordem):
1. existsFolha(funcionarioId, mesReferencia) → se true, lança FechamentoDuplicadoException.
2. Buscar funcionário ativo ou lança FuncionarioNaoEncontradoException.
3. Calcular período: primeiro e último dia do mês.
4. Buscar vales abertos do período (findValesAbertos).
5. Buscar adiantamentos ativos (findAdiantamentosAtivos).
6. Calcular: valorFinal = salarioBase - totalVales - totalParcelas + ajuste.
7. Gerar texto observacao com breakdown (ver spec §4 para formato exato).
8. Criar Pedido FOLHA (categoria=FOLHA, status=PENDENTE, funcionario_id, mes_referencia, observacao).
9. Marcar vales como fechado=true (markAllClosed).
10. Para cada adiantamento: incrementar parcelas_pagas; se quitado (parcelas_pagas==num_parcelas), setar ativo=false.
11. Retornar o Pedido FOLHA criado.

## REGRAS DURAS

1. Branch: `feature/be-028-fechar-mes` saindo de `origin/develop`.
2. Território: SÓ `financas_bot_telegram/`. Zero `frontend/`.
3. FecharMesUseCase DEVE ter @Transactional — todos os 11 passos em uma transação.
4. DataIntegrityViolationException (UNIQUE INDEX race condition) → capturar e relançar como FechamentoDuplicadoException.
5. 1 commit: `feat(BE-028): FecharMesUseCase + endpoint de fechamento`.
6. NÃO mergeie. PR pra develop após status report + Reviewer.

## TESTES DESTA TASK

Smoke only (cobertura completa é BE-029):
- Compilação limpa.
- `mvn test` verde (testes existentes não quebram).
- 1-2 smoke tests do endpoint (happy path + 409).
`testes_novos` ≥ 2.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/BE-028-fechar-mes.md`

Incluir OBRIGATORIAMENTE:
- Decisão sobre `requisitante_id`: nullable por migration, sentinel value, ou outro — com justificativa.
- Se criou V6b migration: confirmar que Flyway aplicou e que `mvn test` verde.
- Confirmação que @Transactional está presente e que DataIntegrityViolationException é capturada.
- Seção `## Padrões e decisões técnicas`: Strategy/Template Method para os 11 passos, onde aplicou.

## SE QUEBRAR

Cenário — requisitante_id NOT NULL sem workaround:
  Criar V6b__folha_requisitante_nullable.sql ANTES de implementar passo 8. Flyway aplica automaticamente no `mvn test`.

Cenário — testes existentes quebram com V6b:
  Verificar se algum test insere pedido com requisitante_id NOT NULL hardcoded. Ajustar para aceitar null.

Cenário — DataIntegrityViolationException de outra constraint (não o UNIQUE INDEX):
  Investigar qual constraint falhou. Registrar no status report antes de tentar workaround.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano

- **Pré-condição tripla** (BE-024 + BE-026 + BE-027): não despachar até as três estarem em develop.
- **Risco alto:** `requisitante_id`. O agente vai verificar antes de implementar — mas o humano pode conferir antecipadamente rodando `SHOW CREATE TABLE pedidos_pagamento` para antecipar a necessidade de V6b.
- **Estimativa:** 1.5-2h (algoritmo de 11 passos + possível V6b migration).
- **Após merge:** despachar BE-029 + FE-016 + FE-017 (os três podem correr em paralelo entre si, mas FE-016 também depende de FE-015).

## Referências

- `docs/sprints/03-folha-pagamento/plans/BE-028-fechar-mes.md`
- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §4 e §5
- `docs/decisions/0016-evo09-folha-pagamento.md` §3
