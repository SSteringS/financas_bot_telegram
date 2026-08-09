# MVC Architecture for Spring Boot

## Table of contents

- Package layout
- Layer responsibilities
- Dependency direction
- Example: Order resource

## Package layout

Default layout for a Spring MVC (layered) application, organized by layer:

```
src/main/java/com/company/orders/
  controller/
    OrderController.java
  service/
    OrderService.java
  repository/
    OrderRepository.java
  model/
    Order.java
  dto/
    OrderRequest.java
    OrderResponse.java
  mapper/
    OrderMapper.java
  config/
  exception/
    OrderNotFoundException.java
    GlobalExceptionHandler.java
```

Keep this layout unless the project already uses a different one; do not change an existing convention without confirming with the user.

## Layer responsibilities

- `controller`: receives HTTP requests, validates input shape, maps DTOs to/from the service layer. No business logic here.
- `service`: holds business logic and transaction boundaries (`@Transactional`). Depends on repository interfaces, never on the controller.
- `repository`: Spring Data JPA interfaces; no business logic, only persistence access and query definitions.
- `model`: JPA entities; kept internal to the service/repository layers, never returned directly by the controller.
- `dto`: request/response shapes exposed at the controller boundary.
- `mapper`: converts between entities and DTOs (manually or with MapStruct).

## Dependency direction

`controller -> service -> repository -> model`. A layer only depends on the layer directly below it; `repository` and `model` never depend on `service` or `controller`.

## Example: Order resource

```java
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

```java
@Service
public class OrderService {
    private final OrderRepository repository;
    private final OrderMapper mapper;

    public OrderService(OrderRepository repository, OrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        Order order = mapper.toEntity(request);
        Order saved = repository.save(order);
        return mapper.toResponse(saved);
    }
}
```

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
}
```

See [examples/order-mvc-example.md](../examples/order-mvc-example.md) for the full feature, including DTOs, mapper, and exception handling.
