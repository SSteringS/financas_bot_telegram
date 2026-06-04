---
task: BE-027
titulo: "CadastrarAdiantamento + endpoints adiantamentos + cancelamento"
data: 2026-06-04
branch: feature/be-027-cadastrar-adiantamento
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 332
  testes_novos: 12
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - null
pr: null
desvios: 0
pendencias_humano: 0
---

# BE-027 — CadastrarAdiantamento + endpoints adiantamentos + cancelamento

## O que foi feito

Entregues os dois use cases de adiantamento e três endpoints REST:

- **`CadastrarAdiantamentoPortIn` / `CadastrarAdiantamentoServiceImpl`**: valida funcionário ativo, valida consistência matemática do plano (`|valorTotal - valorParcela × numParcelas| ≤ 0.01`), persiste com `parcelasPagas=0` e `ativo=true`.
- **`CancelarAdiantamentoPortIn` / `CancelarAdiantamentoServiceImpl`**: verifica se adiantamento existe (404), se já está quitado (`parcelasPagas == numParcelas`) lança `AdiantamentoJaQuitadoException` (409), caso contrário faz soft cancel (`ativo=false`).
- **DTOs**: `AdiantamentoRequest` (Bean Validation), `AdiantamentoResponse` (com campo calculado `parcelasRestantes`).
- **`FolhaController`** atualizado com 3 endpoints: `POST /{id}/adiantamentos`, `GET /{id}/adiantamentos`, `DELETE /adiantamentos/{adiantamentoId}`.
- **`RestExceptionHandler`** recebe handlers para `AdiantamentoNaoEncontradoException` → 404 e `AdiantamentoJaQuitadoException` → 409.
- **Testes**: `CadastrarAdiantamentoServiceImplTest` (7 testes) e `CancelarAdiantamentoServiceImplTest` (5 testes) — 12 novos testes. Suite total: 332 testes, 0 falhas.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

- `validarConsistenciaMatemática` foi deixada com visibilidade package-private (`static void`) em vez de `private` para permitir teste direto sem instanciar o serviço com mocks — simplicidade de teste sem quebrar o encapsulamento do pacote.
- O método `deveAceitarPlanoDentroDaToleranciaComArredondamento` testa o caso-limite exato (diferença = 0.01), confirmando que a condição é `<=` e não `<`.
- `parcelasPagas = null` tratado como "nenhuma parcela paga" no cancelamento — o check `parcelasPagas.equals(numParcelas)` é protegido por null-check no `CancelarAdiantamentoServiceImpl`, evitando NPE.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **BE-028** pode iniciar. Precisa de `FecharMesUseCase` 11 passos `@Transactional`. Atenção à questão `requisitante_id NOT NULL` em `pedidos_pagamento` — ao criar o pedido FOLHA no fechamento, ou faz migration V6b pra tornar a coluna nullable, ou usa um valor sentinela. Decisão precisa ser tomada no início de BE-028.
- `AdiantamentoRepositoryPortOut` está com `findAtivosParaFechamento(Long funcionarioId)` sem implementação ainda — BE-028 precisará implementar isso no adapter.

---

## Padrões técnicos (BE e FE: obrigatório; DEP/CI/EVO: omitir)

**SOLID:**
- **SRP**: `CadastrarAdiantamentoServiceImpl` faz só o cadastro; `CancelarAdiantamentoServiceImpl` faz só o cancelamento — nenhum serviço acumula responsabilidades.
- **OCP / DIP**: ambos os serviços dependem de interfaces (`FuncionarioRepositoryPortOut`, `AdiantamentoRepositoryPortOut`) — nenhum conhece `JdbcTemplate`, `JpaRepository` ou qualquer detalhe de infraestrutura.

**Design Patterns:**
- **Fail-fast validation**: `validarConsistenciaMatemática` é chamada antes de qualquer acesso ao banco — mesma invariante está no CHECK constraint do banco, mas a validação antecipada evita que `DataIntegrityViolationException` vaze para a API com mensagem opaca.
- **Soft delete**: `ativo=false` em vez de `DELETE` real — mesmo padrão usado em `FuncionarioRepositoryAdapter.deleteById()`. Preserva histórico de adiantamentos cancelados para eventual exibição no frontend.

**Arquitetura hexagonal:**
- `FolhaController` (adapter in) → chama `CadastrarAdiantamentoPortIn` / `CancelarAdiantamentoPortIn` (ports in) — nunca acessa `AdiantamentoRepositoryPortOut` para a operação de cancelamento diretamente; o controller chama o use case, que chama o port out.
- Exceção: para o `GET /adiantamentos`, o controller chama `AdiantamentoRepositoryPortOut.findAtivosParaFuncionario()` diretamente (sem use case intermediário), igual ao que o `FolhaController` faz para `GET /vales`. Decisão consciente: consulta sem lógica de negócio não justifica use case próprio (trade-off de simplicidade vs. pureza hexagonal).

---

## Arquivos criados/modificados

- `application/port/in/CadastrarAdiantamentoPortIn.java` (novo)
- `application/port/in/CancelarAdiantamentoPortIn.java` (novo)
- `application/services/CadastrarAdiantamentoServiceImpl.java` (novo)
- `application/services/CancelarAdiantamentoServiceImpl.java` (novo)
- `application/dto/AdiantamentoRequest.java` (novo)
- `application/dto/AdiantamentoResponse.java` (novo)
- `domain/exceptions/AdiantamentoNaoEncontradoException.java` (novo)
- `domain/exceptions/AdiantamentoJaQuitadoException.java` (novo)
- `adapters/in/rest/folha/FolhaController.java` (modificado: adicionados 3 endpoints de adiantamento)
- `adapters/in/rest/RestExceptionHandler.java` (modificado: handlers 404 e 409 para adiantamento)
- `test/.../CadastrarAdiantamentoServiceImplTest.java` (novo: 7 testes)
- `test/.../CancelarAdiantamentoServiceImplTest.java` (novo: 5 testes)
