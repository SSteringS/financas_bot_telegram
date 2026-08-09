# BE-06 — GET /api/v1/pedidos/{id} (detalhe de um pedido)

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- `PedidoNaoEncontradoException` — adicionado construtor `(Long pedidoId)` com `chatId = null`
- `PedidoNaoAutorizadoException` criada em `domain/exceptions/`
- `BuscarPedidoUseCase` (interface) criado
- `BuscarPedidoServiceImpl`:
  - `jpaRepository.findById()` → 404 se não encontrado
  - Verifica `requisitanteId` → 403 se pertence a outro
  - `comprovanteRepo.existsByPedidoId()` para `temComprovante`
- `ComprovanteJpaRepository` — adicionado `boolean existsByPedidoId(Long pedidoId)` (derivado Spring Data, traversal `pedido.id`)
- `PedidoController` — adicionado `GET /{id}`, injeção de `BuscarPedidoUseCase` no construtor
- `RestExceptionHandler` — adicionados handlers: `PedidoNaoEncontradoException → 404`, `PedidoNaoAutorizadoException → 403`
- `PedidoControllerListarTest` — atualizado construtor do controller (agora recebe dois use cases)
- 141 testes totais, todos verdes

---

## mvn test — resultado

```
Tests run: 141, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Desvios do plano

Nenhum.

---

## Próximos passos

- BE-07: URL pré-assinada S3 para imagens de comprovante

---

## Arquivos criados/modificados

**Modificados:**
- `domain/exceptions/PedidoNaoEncontradoException.java` (+ construtor Long pedidoId)
- `adapters/out/persistence/ComprovanteJpaRepository.java` (+ existsByPedidoId)
- `adapters/in/rest/pedido/PedidoController.java` (+ GET /{id}, + BuscarPedidoUseCase)
- `adapters/in/rest/RestExceptionHandler.java` (+ handlers 404/403)
- `test/.../PedidoControllerListarTest.java` (atualizado construtor)

**Novos (produção):**
- `domain/exceptions/PedidoNaoAutorizadoException.java`
- `application/usecases/BuscarPedidoUseCase.java`
- `application/services/BuscarPedidoServiceImpl.java`

**Novos (testes):**
- `BuscarPedidoServiceImplTest`
- `PedidoControllerDetalheTest`
