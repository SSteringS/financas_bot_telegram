# Spring Ecosystem Conventions

## Table of contents

- Dependency injection
- Input validation
- Exception handling
- Transactions
- Spring Data JPA
- Configuration properties
- Lombok and MapStruct

## Dependency injection

Use constructor injection only. With a single constructor, `@Autowired` is not required since Spring 4.3+. Never inject through fields or setters, since it hides required dependencies and blocks immutability.

```java
@Service
public class PricingService {
    private final DiscountCalculator discountCalculator;

    public PricingService(DiscountCalculator discountCalculator) {
        this.discountCalculator = discountCalculator;
    }
}
```

## Input validation

Validate at the boundary (controller or adapter-in), using Bean Validation annotations on the request DTO, not inside the domain.

```java
public record OrderRequest(
    @NotNull Long customerId,
    @NotNull @Positive BigDecimal total
) {}
```

Enable validation on the controller method with `@Valid`:

```java
@PostMapping
public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
}
```

## Exception handling

Centralize error mapping in one `@RestControllerAdvice`; never return raw stack traces or handle errors with scattered `try/catch` in controllers.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }
}
```

## Transactions

Place `@Transactional` on the service (MVC) or the application service implementing the use case (Hexagonal), never on the controller/adapter-in or on the repository. Keep transactions short; do not call external HTTP services inside a transactional method.

## Spring Data JPA

- Keep repository interfaces free of business logic; only persistence access and derived/custom queries.
- Prefer derived query methods (`findByCustomerId`) or `@Query` for complex reads; avoid loading full collections to filter in memory.
- In Hexagonal architecture, the JPA repository stays inside the adapter-out package and is never referenced outside it; the application layer only sees the port.

## Configuration properties

Group related configuration in a typed `@ConfigurationProperties` class instead of scattering `@Value` annotations.

```java
@ConfigurationProperties(prefix = "orders")
public record OrdersProperties(int maxItemsPerOrder, Duration paymentTimeout) {}
```

## Lombok and MapStruct

Detect existing usage from the build file before deciding:

- If `pom.xml` or `build.gradle` already depends on Lombok or MapStruct, follow that convention.
- If neither is present, default to explicit constructors and manual mapper methods; ask before introducing a new build dependency.
