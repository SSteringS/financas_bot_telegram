# Hexagonal Architecture (Ports and Adapters) for Spring Boot

## Table of contents

- Package layout
- Dependency rule
- Ports and adapters
- Example: Order resource

## Package layout

```
src/main/java/com/company/orders/
  domain/
    Order.java
    OrderId.java
  application/
    port/in/
      PlaceOrderUseCase.java
    port/out/
      OrderRepositoryPort.java
    PlaceOrderService.java
  adapter/
    in/web/
      OrderController.java
      OrderRequest.java
      OrderResponse.java
    out/persistence/
      OrderJpaEntity.java
      OrderJpaRepository.java
      OrderRepositoryAdapter.java
```

## Dependency rule

Dependencies point inward only: `adapter -> application -> domain`. The `domain` package never imports Spring, JPA, or any framework type; it contains plain Java only. `application` depends on `domain` and on its own ports (interfaces), never on a concrete adapter.

## Ports and adapters

- Port in (`port/in`): an interface the application layer exposes to be called by driving adapters (e.g. a web controller).
- Port out (`port/out`): an interface the application layer requires from driven adapters (e.g. persistence, messaging).
- Adapter in: implements the entry point (REST controller) and calls a port-in use case.
- Adapter out: implements a port-out interface using a concrete technology (JPA, HTTP client, etc.).

## Example: Order resource

Domain (plain Java, no framework import):

```java
public record Order(OrderId id, CustomerId customerId, BigDecimal total) {
    public static Order create(CustomerId customerId, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order total must be positive");
        }
        return new Order(OrderId.generate(), customerId, total);
    }
}
```

Port in (use case contract):

```java
public interface PlaceOrderUseCase {
    Order placeOrder(CustomerId customerId, BigDecimal total);
}
```

Port out (what the application needs from persistence):

```java
public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}
```

Application service (implements the port in, depends only on the port out):

```java
@Service
public class PlaceOrderService implements PlaceOrderUseCase {
    private final OrderRepositoryPort repository;

    public PlaceOrderService(OrderRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Order placeOrder(CustomerId customerId, BigDecimal total) {
        Order order = Order.create(customerId, total);
        return repository.save(order);
    }
}
```

Adapter in (web controller, driving side):

```java
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final PlaceOrderUseCase placeOrderUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        Order order = placeOrderUseCase.placeOrder(request.customerId(), request.total());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }
}
```

Adapter out (persistence, driven side):

```java
@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {
    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity saved = jpaRepository.save(OrderJpaEntity.fromDomain(order));
        return saved.toDomain();
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(OrderJpaEntity::toDomain);
    }
}
```

See [examples/order-hexagonal-example.md](../examples/order-hexagonal-example.md) for the full feature, including the JPA entity and mapping methods.
