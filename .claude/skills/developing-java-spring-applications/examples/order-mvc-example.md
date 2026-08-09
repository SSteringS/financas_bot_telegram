# Example: Order feature in MVC architecture

## Table of contents

- Request and response DTOs
- Entity
- Mapper
- Repository
- Service
- Controller
- Exception handling

## Request and response DTOs

```java
public record OrderRequest(
    @NotNull Long customerId,
    @NotEmpty List<@Valid OrderItemRequest> items
) {}

public record OrderItemRequest(
    @NotNull Long productId,
    @Positive int quantity
) {}

public record OrderResponse(
    Long id,
    Long customerId,
    BigDecimal total,
    String status
) {}
```

## Entity

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    protected Order() {}

    public Order(Long customerId, BigDecimal total) {
        this.customerId = customerId;
        this.total = total;
        this.status = OrderStatus.CREATED;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public BigDecimal getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
}
```

## Mapper

```java
@Component
public class OrderMapper {
    public Order toEntity(OrderRequest request, BigDecimal total) {
        return new Order(request.customerId(), total);
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getTotal(), order.getStatus().name());
    }
}
```

## Repository

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
}
```

## Service

```java
@Service
public class OrderService {
    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final PricingService pricingService;

    public OrderService(OrderRepository repository, OrderMapper mapper, PricingService pricingService) {
        this.repository = repository;
        this.mapper = mapper;
        this.pricingService = pricingService;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        BigDecimal total = pricingService.calculateTotal(request.items());
        Order order = mapper.toEntity(request, total);
        Order saved = repository.save(order);
        return mapper.toResponse(saved);
    }

    public OrderResponse getById(Long id) {
        Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        return mapper.toResponse(order);
    }
}
```

## Controller

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

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }
}
```

## Exception handling

```java
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Order not found: " + id);
    }
}
```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }
}
```

Next step: use the `writing-java-unit-tests` skill to cover `OrderService` and `OrderController`.
