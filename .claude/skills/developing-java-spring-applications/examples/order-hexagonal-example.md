# Example: Order feature in Hexagonal architecture

## Table of contents

- Domain
- Ports
- Application service
- Adapter in (web)
- Adapter out (persistence)

## Domain

```java
public record OrderId(Long value) {
    public static OrderId generate() {
        return new OrderId(null);
    }
}

public record CustomerId(Long value) {}

public record Order(OrderId id, CustomerId customerId, BigDecimal total, OrderStatus status) {
    public static Order create(CustomerId customerId, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order total must be positive");
        }
        return new Order(OrderId.generate(), customerId, total, OrderStatus.CREATED);
    }
}
```

## Ports

```java
public interface PlaceOrderUseCase {
    Order placeOrder(CustomerId customerId, BigDecimal total);
}

public interface GetOrderUseCase {
    Order getOrder(OrderId id);
}

public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}
```

## Application service

```java
@Service
public class OrderApplicationService implements PlaceOrderUseCase, GetOrderUseCase {
    private final OrderRepositoryPort repository;

    public OrderApplicationService(OrderRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Order placeOrder(CustomerId customerId, BigDecimal total) {
        Order order = Order.create(customerId, total);
        return repository.save(order);
    }

    @Override
    public Order getOrder(OrderId id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
```

## Adapter in (web)

```java
public record OrderRequest(@NotNull Long customerId, @NotNull @Positive BigDecimal total) {}

public record OrderResponse(Long id, Long customerId, BigDecimal total, String status) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.id().value(), order.customerId().value(), order.total(), order.status().name());
    }
}

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase, GetOrderUseCase getOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        Order order = placeOrderUseCase.placeOrder(new CustomerId(request.customerId()), request.total());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        Order order = getOrderUseCase.getOrder(new OrderId(id));
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
```

## Adapter out (persistence)

```java
@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    protected OrderJpaEntity() {}

    static OrderJpaEntity fromDomain(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.id = order.id().value();
        entity.customerId = order.customerId().value();
        entity.total = order.total();
        entity.status = order.status();
        return entity;
    }

    Order toDomain() {
        return new Order(new OrderId(id), new CustomerId(customerId), total, status);
    }
}
```

```java
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {}

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

Next step: use the `writing-java-unit-tests` skill to cover `OrderApplicationService` with a mocked `OrderRepositoryPort`, and `OrderController` with `@WebMvcTest`.
