# BE-08 — Endpoints `GET /pedidos/{id}/foto-pedido` e `GET /pedidos/{id}/comprovante`

> Dois endpoints que retornam 302 redirect pra pre-signed URLs do S3. Front faz `<img src="...">` ou `<iframe src="...">` e o browser segue o redirect transparente.

---

## Pré-requisitos

- BE-06 (detalhe) mergeada — usa `BuscarPedidoUseCase` e validação de autoria
- BE-07 (pre-signed URL) mergeada — usa `StorageService`

---

## Arquivos esperados

**Novos:**
- `application/usecases/ObterUrlImagemPedidoUseCase.java`
- `application/usecases/ObterUrlComprovanteUseCase.java`
- `application/services/ObterUrlImagemPedidoServiceImpl.java`
- `application/services/ObterUrlComprovanteServiceImpl.java`

**Modificados:**
- `PedidoController.java` — adicionar handlers `GET /{id}/foto-pedido` e `GET /{id}/comprovante`

**Tests:**
- `ObterUrlImagemPedidoServiceImplTest`
- `ObterUrlComprovanteServiceImplTest`
- `PedidoControllerImagemTest`

---

## Código-chave

### `ObterUrlImagemPedidoServiceImpl`

```java
@Service
public class ObterUrlImagemPedidoServiceImpl implements ObterUrlImagemPedidoUseCase {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final PedidoPagamentoJpaRepository pedidoRepo;
    private final StorageService storage;

    @Override
    public String obter(Long pedidoId, Long requisitanteId) {
        PedidoPagamentoEntity p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));

        if (!p.getRequisitanteId().equals(requisitanteId)) {
            throw new PedidoNaoAutorizadoException(pedidoId, requisitanteId);
        }

        if (p.getImagemUrl() == null || p.getImagemUrl().isBlank()) {
            throw new ImagemNaoEncontradaException("Pedido " + pedidoId + " não tem imagem associada");
        }

        return storage.gerarUrlTemporariaParaLeitura(p.getImagemUrl(), TTL);
    }
}
```

### `ObterUrlComprovanteServiceImpl`

```java
@Service
public class ObterUrlComprovanteServiceImpl implements ObterUrlComprovanteUseCase {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final PedidoPagamentoJpaRepository pedidoRepo;
    private final ComprovanteJpaRepository comprovanteRepo;
    private final StorageService storage;

    @Override
    public String obter(Long pedidoId, Long requisitanteId) {
        PedidoPagamentoEntity p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));

        if (!p.getRequisitanteId().equals(requisitanteId)) {
            throw new PedidoNaoAutorizadoException(pedidoId, requisitanteId);
        }

        ComprovanteEntity c = comprovanteRepo.findFirstByPedidoIdOrderByIdDesc(pedidoId)
                .orElseThrow(() -> new ComprovanteNaoEncontradoException(pedidoId));

        return storage.gerarUrlTemporariaParaLeitura(c.getImagemUrl(), TTL);
    }
}
```

Em `ComprovanteJpaRepository`:

```java
Optional<ComprovanteEntity> findFirstByPedidoIdOrderByIdDesc(Long pedidoId);
```

(Mais recente comprovante, se houver múltiplos. Hoje há só um por pedido mas a query é defensiva.)

### Handlers em `PedidoController`

```java
@GetMapping("/{id}/foto-pedido")
@Operation(summary = "Redireciona pra pre-signed URL da foto original do pedido")
public ResponseEntity<Void> fotoPedido(
        @PathVariable Long id,
        @RequisitanteId Long requisitanteId) {
    String url = obterUrlImagemPedidoUseCase.obter(id, requisitanteId);
    return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, url)
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=600")
            .build();
}

@GetMapping("/{id}/comprovante")
@Operation(summary = "Redireciona pra pre-signed URL do comprovante do pedido")
public ResponseEntity<Void> comprovante(
        @PathVariable Long id,
        @RequisitanteId Long requisitanteId) {
    String url = obterUrlComprovanteUseCase.obter(id, requisitanteId);
    return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, url)
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=600")
            .build();
}
```

### Handlers em `RestExceptionHandler`

```java
@ExceptionHandler(ImagemNaoEncontradaException.class)
public ResponseEntity<ErroDTO> handleImagemNaoEncontrada(ImagemNaoEncontradaException e) {
    return ResponseEntity.status(404).body(new ErroDTO("IMAGEM_NAO_ENCONTRADA", e.getMessage()));
}

@ExceptionHandler(ComprovanteNaoEncontradoException.class)
public ResponseEntity<ErroDTO> handleComprovanteNaoEncontrado(ComprovanteNaoEncontradoException e) {
    return ResponseEntity.status(404).body(new ErroDTO("COMPROVANTE_NAO_ENCONTRADO", e.getMessage()));
}
```

Criar as duas exceções em `domain/exceptions/` se ainda não existirem.

---

## Critério de aceitação

- [ ] `GET /api/v1/pedidos/123/foto-pedido` (pedido existe, mesmo requisitante, com `imagem_url`) → 302 com `Location: <pre-signed URL>` e `Cache-Control: private, max-age=600`
- [ ] Browser/curl seguindo o redirect baixa o objeto do S3 sem credenciais
- [ ] Mesmo endpoint, pedido não existe → 404
- [ ] Mesmo endpoint, pedido de outro requisitante → 403
- [ ] Mesmo endpoint, pedido sem `imagem_url` → 404 com `{"codigo":"IMAGEM_NAO_ENCONTRADA"}`
- [ ] `GET /api/v1/pedidos/123/comprovante` (PAGO com comprovante) → 302 análogo
- [ ] Mesmo endpoint, pedido PENDENTE sem comprovante → 404 com `{"codigo":"COMPROVANTE_NAO_ENCONTRADO"}`
- [ ] Pre-signed URL expira após 10 min (testar manualmente)
- [ ] `./mvnw test` passa

---

## Fora de escopo

- Suporte a múltiplos comprovantes por pedido (futuro)
- Validação de mimeType (EVO-07)
- Resumo (BE-09)

---

## Status report

`docs/status/BE-08-endpoints-imagem.md`. Curl pros 2 endpoints + verificação que redirect funciona end-to-end (curl segue redirect e baixa). Próximo: BE-09.
