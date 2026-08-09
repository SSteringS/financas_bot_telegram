# BE-05 — GET /api/v1/pedidos (listagem com filtros + paginação)

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- `PedidoPagamentoJpaRepository` estendido com `JpaSpecificationExecutor<PedidoPagamentoEntity>`
- `ComprovanteJpaRepository` com nova query `findPedidoIdsByPedidoIdIn()` — evita N+1
- Criado `ListarPedidosFiltro` (record com status, tipos, de, ate, busca, page, tamanho)
- Criado `PedidoSpecs` com 6 Specifications: `doRequisitante`, `comStatus`, `comTipos`, `dataPedidoDesde`, `dataPedidoAte`, `comBusca` (com fallback numérico pra valor)
- Criado `ListarPedidosUseCase` (interface)
- Criado `ListarPedidosServiceImpl`:
  - Usa Specifications + `jpaRepository.findAll(spec, pageable)`
  - Tamanho capado em 50, default 20
  - 2 queries totais: listagem + `findPedidoIdsByPedidoIdIn()` pra `temComprovante`
  - Ordenação `dataPedido DESC, id DESC`
- Criado `PedidoController GET /api/v1/pedidos` com `@RequisitanteId Long` (protegido pelo filter da BE-12)
- 134 testes totais, todos verdes

---

## mvn test — resultado

```
Tests run: 134, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Desvios do plano

- O plano listava o port `PedidoPagamentoRepositoryPort.listar()` como modificado, mas o `ListarPedidosServiceImpl` usa o `PedidoPagamentoJpaRepository` diretamente (como mostrado no código-chave do plano). A service tem acesso ao JPA para usar Specifications — adicionar Specification ao port seria um vazamento de infraestrutura. Optei por seguir o código do plano.

---

## Decisões tomadas durante a execução

- Guarda-corpos no service: se `ids.isEmpty()`, retorna `Set.of()` sem chamar a query de comprovantes (evita erro do MySQL com `IN ()`).

---

## Decisões pendentes

Nenhuma — tarefa fechada.

---

## Próximos passos

- BE-06: `GET /api/v1/pedidos/{id}` — detalhe do pedido com lista de comprovantes

---

## Arquivos criados/modificados

**Modificados:**
- `adapters/out/persistence/PedidoPagamentoJpaRepository.java` (+ JpaSpecificationExecutor)
- `adapters/out/persistence/ComprovanteJpaRepository.java` (+ findPedidoIdsByPedidoIdIn)

**Novos (produção):**
- `application/dto/ListarPedidosFiltro.java`
- `adapters/out/persistence/PedidoSpecs.java`
- `application/usecases/ListarPedidosUseCase.java`
- `application/services/ListarPedidosServiceImpl.java`
- `adapters/in/rest/pedido/PedidoController.java`

**Novos (testes):**
- `ListarPedidosServiceImplTest`
- `PedidoControllerListarTest`
