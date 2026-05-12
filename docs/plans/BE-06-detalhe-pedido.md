# BE-06 — GET /api/v1/pedidos/{id} (detalhe de um pedido)

> Endpoint de detalhe. Retorna um único pedido, com isolamento por requisitante.

---

## Pré-requisitos

- BE-05 mergeada (ou disponível em branch local)
- `PedidoDetalheDTO` existe (BE-04)
- Filter de auth ativo (BE-12)

---

## Arquivos esperados

**Novos:**
- `application/usecases/BuscarPedidoUseCase.java`
- `application/services/BuscarPedidoServiceImpl.java`
- `domain/exceptions/PedidoNaoAutorizadoException.java`

**Modificados:**
- `PedidoController.java` — adicionar handler `GET /{id}`
- `RestExceptionHandler.java` — handler pra `PedidoNaoEncontradoException` (404) e `PedidoNaoAutorizadoException` (403)
- `PedidoPagamentoMapper` — adicionar `toDetalheDTO` (ou reusar `toDomain` + mapper de domain→DTO; manter consistente com BE-05)

**Tests:**
- `BuscarPedidoServiceImplTest`
- `PedidoControllerDetalheTest`

---

## Código-chave

### `BuscarPedidoServiceImpl`

```java
@Service
public class BuscarPedidoServiceImpl implements BuscarPedidoUseCase {

    private final PedidoPagamentoJpaRepository jpaRepository;
    private final ComprovanteJpaRepository comprovanteRepo;

    @Override
    public PedidoDetalheDTO buscar(Long pedidoId, Long requisitanteId) {
        PedidoPagamentoEntity entity = jpaRepository.findById(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));

        if (!entity.getRequisitanteId().equals(requisitanteId)) {
            throw new PedidoNaoAutorizadoException(pedidoId, requisitanteId);
        }

        boolean temComprovante = comprovanteRepo.existsByPedidoId(pedidoId);

        return new PedidoDetalheDTO(
                entity.getId(), entity.getValor(), entity.getDescricao(),
                entity.getTipo(), entity.getStatus(),
                entity.getDataPedido(), entity.getDataPagamento(),
                temComprovante);
    }
}
```

Em `ComprovanteJpaRepository` adicionar:

```java
boolean existsByPedidoId(Long pedidoId);
```

### Handler em `PedidoController`

```java
@GetMapping("/{id}")
@Operation(summary = "Detalhe de um pedido específico")
public PedidoDetalheDTO buscar(
        @PathVariable Long id,
        @RequisitanteId Long requisitanteId) {
    return buscarUseCase.buscar(id, requisitanteId);
}
```

### Handlers em `RestExceptionHandler`

```java
@ExceptionHandler(PedidoNaoEncontradoException.class)
public ResponseEntity<ErroDTO> handlePedidoNaoEncontrado(PedidoNaoEncontradoException e) {
    return ResponseEntity.status(404)
            .body(new ErroDTO("PEDIDO_NAO_ENCONTRADO", e.getMessage()));
}

@ExceptionHandler(PedidoNaoAutorizadoException.class)
public ResponseEntity<ErroDTO> handlePedidoNaoAutorizado(PedidoNaoAutorizadoException e) {
    return ResponseEntity.status(403)
            .body(new ErroDTO("ACESSO_NEGADO", "Pedido pertence a outro requisitante"));
}
```

Cuidado com a `PedidoNaoEncontradoException` existente — ela já tem o construtor `(String, Long chatId)` herdado do uso pelo bot Telegram. Reusar e adicionar construtor `(Long pedidoId)` ou criar variante. Verificar antes de modificar.

---

## Critério de aceitação

- [ ] `GET /api/v1/pedidos/123` com cookie válido e pedido pertencendo ao requisitante → 200 com `PedidoDetalheDTO`
- [ ] Mesmo endpoint, id inexistente → 404 com `{"codigo":"PEDIDO_NAO_ENCONTRADO", "mensagem":"..."}`
- [ ] Mesmo endpoint, id de pedido de OUTRO requisitante → 403 com `{"codigo":"ACESSO_NEGADO"}`
- [ ] Sem cookie → 401 (filter)
- [ ] Swagger UI mostra o endpoint
- [ ] `./mvnw test` passa

---

## Fora de escopo

- Endpoints de imagem (BE-07/08)
- Resumo (BE-09)

---

## Status report

`docs/status/BE-06-detalhe-pedido.md`. Cobrir resultado dos testes e curl dos 3 cenários (200, 404, 403). Próximo: BE-07.
