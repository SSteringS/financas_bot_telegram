# BE-05 — GET /api/v1/pedidos (listagem com filtros + paginação)

> Primeiro endpoint REST de leitura. Já sai protegido por JWT (BE-12 já tá em develop). Front vai consumir esse endpoint pra montar a tela principal da variante C.

---

## Contexto

Listagem de pedidos do requisitante autenticado, com:
- Filtro de status (`PENDENTE` | `PAGO` | `TODOS`)
- Filtro de tipo (lista repetível)
- Intervalo de datas (`de`, `ate` sobre `data_pedido`)
- Busca textual (`busca` — LIKE em descricao + match exato em valor se numérico)
- Paginação (`page` 0-indexed, `tamanho` default 20, max 50)
- Ordenação default: `dataPedido DESC, id DESC`

Cada item retorna `PedidoResumoDTO` (já existe da BE-04), com `temComprovante` indicando se existe comprovante associado.

---

## Pré-requisitos

- BE-04 ✓ (DTOs disponíveis)
- BE-12 ✓ (filter de auth — endpoint sai protegido)
- BE-13 ✓ (CORS — front pode consumir)

---

## Arquivos esperados

**Novos:**
- `application/dto/ListarPedidosFiltro.java` (record com query params)
- `application/usecases/ListarPedidosUseCase.java` (interface)
- `application/services/ListarPedidosServiceImpl.java`
- `adapters/in/rest/pedido/PedidoController.java` (vai receber mais endpoints nas próximas BEs)
- `adapters/out/persistence/PedidoPagamentoQueryRepository.java` (Spring Data com Specification ou @Query)

**Modificados:**
- `application/port/out/PedidoPagamentoRepositoryPort.java` — adicionar método `listar(filtro, requisitanteId, pageable)`
- `adapters/out/persistence/PedidoPagamentoRepositoryAdapter.java` — implementar o método novo

**Tests:**
- `ListarPedidosServiceImplTest`
- `PedidoControllerListarTest`
- `PedidoPagamentoRepositoryAdapterListarTest` (com `@DataJpaTest` + H2)

---

## Código-chave

### `ListarPedidosFiltro`

```java
package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import java.time.LocalDate;
import java.util.List;

public record ListarPedidosFiltro(
        StatusPedido status,        // null = TODOS
        List<TipoPagamento> tipos,  // null/vazia = todos os tipos
        LocalDate de,               // null = sem limite inferior
        LocalDate ate,              // null = sem limite superior
        String busca,               // null/vazia = sem filtro
        int page,
        int tamanho
) {
    public static int TAMANHO_DEFAULT = 20;
    public static int TAMANHO_MAX = 50;
}
```

### Query repository (Specification API)

```java
package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PedidoPagamentoQueryRepository
        extends JpaRepository<PedidoPagamentoEntity, Long>, JpaSpecificationExecutor<PedidoPagamentoEntity> {
}
```

**Nota:** essa interface coexiste com `PedidoPagamentoJpaRepository` existente. Você pode unificar (fazer o JpaRepository existente estender também o `JpaSpecificationExecutor`) ou manter separado. Recomendo **unificar** — uma só `PedidoPagamentoJpaRepository extends JpaRepository<...>, JpaSpecificationExecutor<...>`. Aí apaga o `PedidoPagamentoQueryRepository` que estava sendo criado.

### Specifications

```java
public final class PedidoSpecs {
    private PedidoSpecs() {}

    public static Specification<PedidoPagamentoEntity> doRequisitante(Long requisitanteId) {
        return (root, query, cb) -> cb.equal(root.get("requisitanteId"), requisitanteId);
    }

    public static Specification<PedidoPagamentoEntity> comStatus(StatusPedido status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<PedidoPagamentoEntity> comTipos(List<TipoPagamento> tipos) {
        return (root, query, cb) -> (tipos == null || tipos.isEmpty()) ? null : root.get("tipo").in(tipos);
    }

    public static Specification<PedidoPagamentoEntity> dataPedidoDesde(LocalDate de) {
        return (root, query, cb) -> de == null ? null : cb.greaterThanOrEqualTo(root.get("dataPedido"), de);
    }

    public static Specification<PedidoPagamentoEntity> dataPedidoAte(LocalDate ate) {
        return (root, query, cb) -> ate == null ? null : cb.lessThanOrEqualTo(root.get("dataPedido"), ate);
    }

    public static Specification<PedidoPagamentoEntity> comBusca(String busca) {
        return (root, query, cb) -> {
            if (busca == null || busca.isBlank()) return null;
            String like = "%" + busca.toLowerCase() + "%";
            Predicate likeDesc = cb.like(cb.lower(root.get("descricao")), like);
            try {
                BigDecimal valor = new BigDecimal(busca.replace(",", "."));
                return cb.or(likeDesc, cb.equal(root.get("valor"), valor));
            } catch (NumberFormatException e) {
                return likeDesc;
            }
        };
    }
}
```

### `ListarPedidosServiceImpl`

```java
@Service
public class ListarPedidosServiceImpl implements ListarPedidosUseCase {

    private final PedidoPagamentoJpaRepository jpaRepository;
    private final PedidoPagamentoMapper mapper;
    private final ComprovanteJpaRepository comprovanteRepo;

    // construtor

    @Override
    public PaginaDTO<PedidoResumoDTO> listar(ListarPedidosFiltro filtro, Long requisitanteId) {
        int tamanho = Math.min(
                filtro.tamanho() <= 0 ? ListarPedidosFiltro.TAMANHO_DEFAULT : filtro.tamanho(),
                ListarPedidosFiltro.TAMANHO_MAX);
        int page = Math.max(filtro.page(), 0);

        Specification<PedidoPagamentoEntity> spec = Specification
                .where(PedidoSpecs.doRequisitante(requisitanteId))
                .and(PedidoSpecs.comStatus(filtro.status()))
                .and(PedidoSpecs.comTipos(filtro.tipos()))
                .and(PedidoSpecs.dataPedidoDesde(filtro.de()))
                .and(PedidoSpecs.dataPedidoAte(filtro.ate()))
                .and(PedidoSpecs.comBusca(filtro.busca()));

        Pageable pageable = PageRequest.of(page, tamanho,
                Sort.by(Sort.Direction.DESC, "dataPedido").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<PedidoPagamentoEntity> pageResult = jpaRepository.findAll(spec, pageable);

        // pre-compute existência de comprovante por id (1 query em vez de N)
        List<Long> ids = pageResult.getContent().stream().map(PedidoPagamentoEntity::getId).toList();
        Set<Long> comComprovante = comprovanteRepo.findPedidoIdsByPedidoIdIn(ids);

        List<PedidoResumoDTO> items = pageResult.getContent().stream()
                .map(e -> toResumoDTO(e, comComprovante.contains(e.getId())))
                .toList();

        return new PaginaDTO<>(items, pageResult.getTotalElements(), page, tamanho, pageResult.getTotalPages());
    }

    private PedidoResumoDTO toResumoDTO(PedidoPagamentoEntity e, boolean temComprovante) {
        return new PedidoResumoDTO(
                e.getId(), e.getValor(), e.getDescricao(), e.getTipo(), e.getStatus(),
                e.getDataPedido(), e.getDataPagamento(), temComprovante);
    }
}
```

Em `ComprovanteJpaRepository` adicionar:

```java
@Query("SELECT c.pedido.id FROM ComprovanteEntity c WHERE c.pedido.id IN :ids")
Set<Long> findPedidoIdsByPedidoIdIn(@Param("ids") List<Long> ids);
```

### `PedidoController`

```java
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final ListarPedidosUseCase listarUseCase;

    public PedidoController(ListarPedidosUseCase listarUseCase) {
        this.listarUseCase = listarUseCase;
    }

    @GetMapping
    @Operation(summary = "Lista pedidos do requisitante autenticado, com filtros e paginação")
    public PaginaDTO<PedidoResumoDTO> listar(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(name = "tipo", required = false) List<TipoPagamento> tipos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int tamanho,
            @RequisitanteId Long requisitanteId) {

        return listarUseCase.listar(
                new ListarPedidosFiltro(status, tipos, de, ate, busca, page, tamanho),
                requisitanteId);
    }
}
```

---

## Critério de aceitação

- [ ] `GET /api/v1/pedidos` sem cookie → 401 (filter da BE-12 cuida disso)
- [ ] Com cookie válido sem filtros → 200 com `PaginaDTO<PedidoResumoDTO>`, default 20 itens, ordenado data_pedido DESC
- [ ] Filtro `status=PAGO` retorna só PAGO
- [ ] Filtro `tipo=PIX&tipo=TED` retorna PIX OR TED
- [ ] Filtro `de=2026-04-01&ate=2026-04-30` retorna só de abril
- [ ] Filtro `busca=energia` faz LIKE em descrição
- [ ] Filtro `busca=287.50` (numérico) faz match em descrição OR valor
- [ ] `tamanho=200` é capado em 50 (max)
- [ ] `temComprovante` reflete realidade (true se existe comprovante na tabela `comprovantes` apontando pro id)
- [ ] **Isolamento por requisitante:** sem cara de admin, ninguém vê pedidos de outro requisitante. Testar criando seed de 2 requisitantes e pedidos de cada — o token do requisitante 1 não vê pedidos do 2.
- [ ] N+1 evitado: apenas 2 queries totais (1 pra listar pedidos, 1 pra buscar comprovantes)
- [ ] Swagger UI mostra o endpoint com todos os parâmetros e descrições
- [ ] `./mvnw test` passa

---

## Fora de escopo

- Endpoint de detalhe (BE-06)
- Pre-signed URL (BE-07)
- Foto/comprovante (BE-08)
- Resumo (BE-09)

---

## Status report

`docs/status/BE-05-listar-pedidos.md`. Cobrir:

- Sumário do `mvn test`
- Exemplos de curl testados (sem filtros, com cada filtro, paginado)
- Output JSON de uma listagem real (recortar 1-2 items)
- Próximo passo: BE-06
