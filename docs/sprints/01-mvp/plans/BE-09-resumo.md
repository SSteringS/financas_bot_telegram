# BE-09 — GET /api/v1/resumo (agregado de pendentes + pagos do mês)

> Endpoint que alimenta o header do front (variante C): "Olá Pedro, 12 pedidos nos últimos 30 dias". Dois números: pendentes (qty + total) e pagos (qty + total), filtrados por mês corrente.

---

## Pré-requisitos

- BE-05 mergeada (estrutura de listagem por requisitante já testada)
- Filter de auth ativo

---

## Arquivos esperados

**Novos:**
- `application/usecases/ResumoMesUseCase.java`
- `application/services/ResumoMesServiceImpl.java`

**Modificados:**
- `PedidoController.java` — adicionar handler `GET /resumo` (ou criar `ResumoController.java` se preferir separar; vou indicar criar dentro de `PedidoController` pra reduzir surface)

Espera aí — `/api/v1/resumo` é fora de `/api/v1/pedidos`. Faz mais sentido **criar `ResumoController.java`** em `adapters/in/rest/resumo/` com `@RequestMapping("/api/v1/resumo")`.

**Tests:**
- `ResumoMesServiceImplTest`
- `ResumoControllerTest`

---

## Código-chave

### Query agregada em `PedidoPagamentoJpaRepository`

```java
@Query("""
       SELECT new br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.AgregadoStatus(
           p.status, COUNT(p), COALESCE(SUM(p.valor), 0))
       FROM PedidoPagamentoEntity p
       WHERE p.requisitanteId = :requisitanteId
         AND p.dataPedido >= :inicioMes
         AND p.dataPedido <= :fimMes
       GROUP BY p.status
       """)
List<AgregadoStatus> agregarPorStatusNoIntervalo(
        @Param("requisitanteId") Long requisitanteId,
        @Param("inicioMes") LocalDate inicioMes,
        @Param("fimMes") LocalDate fimMes);
```

Criar `AgregadoStatus` em `adapters/out/persistence/`:

```java
public record AgregadoStatus(StatusPedido status, long quantidade, BigDecimal total) {}
```

### `ResumoMesServiceImpl`

```java
@Service
public class ResumoMesServiceImpl implements ResumoMesUseCase {

    private final PedidoPagamentoJpaRepository repo;
    private final Clock clock;

    public ResumoMesServiceImpl(PedidoPagamentoJpaRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    public ResumoMesDTO obter(Long requisitanteId) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate inicio = hoje.withDayOfMonth(1);
        LocalDate fim = hoje.withDayOfMonth(hoje.lengthOfMonth());

        Map<StatusPedido, AgregadoStatus> porStatus = repo
                .agregarPorStatusNoIntervalo(requisitanteId, inicio, fim).stream()
                .collect(Collectors.toMap(AgregadoStatus::status, Function.identity()));

        ResumoStatusDTO pendentes = toDTO(porStatus.get(StatusPedido.PENDENTE));
        ResumoStatusDTO pagos = toDTO(porStatus.get(StatusPedido.PAGO));

        String mesAtual = String.format("%04d-%02d", hoje.getYear(), hoje.getMonthValue());
        return new ResumoMesDTO(mesAtual, pendentes, pagos);
    }

    private ResumoStatusDTO toDTO(AgregadoStatus a) {
        if (a == null) return new ResumoStatusDTO(0, BigDecimal.ZERO);
        return new ResumoStatusDTO((int) a.quantidade(), a.total());
    }
}
```

Injetar `Clock` permite testar com data fixa via `Clock.fixed(...)`. Adicionar bean em `AppConfig.java`:

```java
@Bean
public Clock systemClock() {
    return Clock.systemDefaultZone();
}
```

### `ResumoController`

```java
@RestController
@RequestMapping("/api/v1/resumo")
public class ResumoController {

    private final ResumoMesUseCase useCase;

    public ResumoController(ResumoMesUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @Operation(summary = "Resumo do mês corrente: pendentes + pagos")
    public ResumoMesDTO obter(@RequisitanteId Long requisitanteId) {
        return useCase.obter(requisitanteId);
    }
}
```

---

## Critério de aceitação

- [ ] `GET /api/v1/resumo` com cookie válido retorna 200 com `ResumoMesDTO`
- [ ] `mesAtual` no formato `YYYY-MM`
- [ ] `pendentes.quantidade` e `pendentes.total` somam só pedidos PENDENTE com `data_pedido` no mês atual
- [ ] `pagos.quantidade` e `pagos.total` somam só pedidos PAGO com `data_pedido` no mês atual
- [ ] Status `CANCELADO` é ignorado (não aparece em nenhum dos dois)
- [ ] Sem pedidos no mês → ambos retornam `{quantidade: 0, total: 0}`
- [ ] Isolamento: requisitante 1 só vê seus próprios agregados
- [ ] Query única usando GROUP BY (não 2 queries separadas) — verificável habilitando `spring.jpa.show-sql=true` em teste
- [ ] Sem cookie → 401
- [ ] `./mvnw test` passa, incluindo teste com `Clock.fixed` simulando mês específico

---

## Fora de escopo

- Resumo de meses passados (parametrizar por mês na URL fica pra futuro)
- Gráficos / comparações ano-a-ano

---

## Status report

`docs/status/BE-09-resumo.md`. Output dos testes + curl real mostrando JSON do resumo. **Marco**: depois desta tarefa, **toda a superfície da API REST de leitura está pronta**. Próximo: BE-03.
