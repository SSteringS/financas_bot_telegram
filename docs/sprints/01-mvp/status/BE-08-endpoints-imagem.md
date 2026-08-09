# BE-08 — Endpoints GET /pedidos/{id}/foto-pedido e GET /pedidos/{id}/comprovante

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- `ImagemNaoEncontradaException` criada em `domain/exceptions/`
- `ComprovanteNaoEncontradoException` criada em `domain/exceptions/`
- `ObterUrlImagemPedidoUseCase` (interface) criada
- `ObterUrlComprovanteUseCase` (interface) criada
- `ObterUrlImagemPedidoServiceImpl`:
  - Busca pedido → 404 se não existe
  - Verifica autoria → 403 se outro requisitante
  - Verifica `imagemUrl` → 404 IMAGEM_NAO_ENCONTRADA se nulo/vazio
  - Gera pre-signed URL via `StorageService.gerarUrlTemporariaParaLeitura()`
- `ObterUrlComprovanteServiceImpl`:
  - Busca pedido → 404/403
  - `comprovanteRepo.findFirstByPedidoIdOrderByIdDesc()` → 404 COMPROVANTE_NAO_ENCONTRADO se sem comprovante
  - Gera pre-signed URL
- `ComprovanteJpaRepository` — adicionado `Optional<ComprovanteEntity> findFirstByPedidoIdOrderByIdDesc(Long pedidoId)`
- `PedidoController` — adicionados `GET /{id}/foto-pedido` e `GET /{id}/comprovante` (302 redirect com Cache-Control)
- `RestExceptionHandler` — adicionados handlers 404 para `ImagemNaoEncontradaException` e `ComprovanteNaoEncontradoException`
- Testes de controller anteriores atualizados (construtor 4-args)
- 155 testes totais, todos verdes

---

## mvn test — resultado

```
Tests run: 155, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Desvios do plano

Nenhum.

---

## Próximos passos

- BE-09: endpoint de resumo (totais por mês/status)

---

## Arquivos criados/modificados

**Modificados:**
- `adapters/out/persistence/ComprovanteJpaRepository.java` (+ findFirstByPedidoIdOrderByIdDesc)
- `adapters/in/rest/pedido/PedidoController.java` (+ foto-pedido, + comprovante, + 2 use cases no construtor)
- `adapters/in/rest/RestExceptionHandler.java` (+ 2 handlers 404)
- `test/.../PedidoControllerListarTest.java` (atualizado construtor)
- `test/.../PedidoControllerDetalheTest.java` (atualizado construtor)

**Novos (produção):**
- `domain/exceptions/ImagemNaoEncontradaException.java`
- `domain/exceptions/ComprovanteNaoEncontradoException.java`
- `application/usecases/ObterUrlImagemPedidoUseCase.java`
- `application/usecases/ObterUrlComprovanteUseCase.java`
- `application/services/ObterUrlImagemPedidoServiceImpl.java`
- `application/services/ObterUrlComprovanteServiceImpl.java`

**Novos (testes):**
- `ObterUrlImagemPedidoServiceImplTest`
- `ObterUrlComprovanteServiceImplTest`
- `PedidoControllerImagemTest`
