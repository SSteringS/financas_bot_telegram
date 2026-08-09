# SOLID Principles for Java and Spring

## Table of contents

- Single Responsibility Principle
- Open/Closed Principle
- Liskov Substitution Principle
- Interface Segregation Principle
- Dependency Inversion Principle
- Checklist to apply on every class

## Single Responsibility Principle (SRP)

A class has exactly one reason to change.

Bad: a service that validates input, calculates a price, and sends an email.

```java
public class OrderService {
    public void placeOrder(Order order) {
        if (order.items().isEmpty()) {
            throw new IllegalArgumentException("Order must have items");
        }
        BigDecimal total = order.items().stream()
            .map(Item::price)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(total);
        emailSender.send(order.customerEmail(), "Order placed", "Total: " + total);
    }
}
```

Good: split validation, pricing, and notification into single-purpose collaborators.

```java
public class OrderService {
    private final OrderValidator validator;
    private final PricingService pricingService;
    private final OrderNotifier notifier;

    public OrderService(OrderValidator validator, PricingService pricingService, OrderNotifier notifier) {
        this.validator = validator;
        this.pricingService = pricingService;
        this.notifier = notifier;
    }

    public void placeOrder(Order order) {
        validator.validate(order);
        BigDecimal total = pricingService.calculateTotal(order);
        order.setTotal(total);
        notifier.notifyOrderPlaced(order);
    }
}
```

## Open/Closed Principle (OCP)

A class is open for extension, closed for modification. New behavior should be added through new implementations, not by editing existing conditionals.

Bad:

```java
public BigDecimal calculateDiscount(Customer customer, BigDecimal total) {
    if (customer.tier() == Tier.GOLD) {
        return total.multiply(new BigDecimal("0.10"));
    } else if (customer.tier() == Tier.SILVER) {
        return total.multiply(new BigDecimal("0.05"));
    }
    return BigDecimal.ZERO;
}
```

Good: extract a strategy per tier and register it, so a new tier does not touch existing code.

```java
public interface DiscountPolicy {
    boolean supports(Tier tier);
    BigDecimal apply(BigDecimal total);
}

public class DiscountCalculator {
    private final List<DiscountPolicy> policies;

    public DiscountCalculator(List<DiscountPolicy> policies) {
        this.policies = policies;
    }

    public BigDecimal calculateDiscount(Customer customer, BigDecimal total) {
        return policies.stream()
            .filter(policy -> policy.supports(customer.tier()))
            .findFirst()
            .map(policy -> policy.apply(total))
            .orElse(BigDecimal.ZERO);
    }
}
```

## Liskov Substitution Principle (LSP)

A subtype must be usable wherever its supertype is expected, without weakening guarantees.

Bad: a subclass that throws on a method the base type promises to support.

```java
public class ReadOnlyRepository implements OrderRepository {
    public void save(Order order) {
        throw new UnsupportedOperationException("read only");
    }
}
```

Good: split the interface so read-only clients depend only on what they use (this also applies ISP).

```java
public interface OrderReader {
    Optional<Order> findById(OrderId id);
}

public interface OrderWriter {
    void save(Order order);
}
```

## Interface Segregation Principle (ISP)

Clients should not depend on methods they do not use. Prefer small, role-specific interfaces over one large interface.

Bad: one repository interface forcing every implementation to support every operation.

```java
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
    void delete(OrderId id);
    List<Order> findAllByCustomer(CustomerId id);
}
```

Good: split by real client needs, as shown in `OrderReader`/`OrderWriter` above, or by capability (`OrderQueryRepository`, `OrderCommandRepository`).

## Dependency Inversion Principle (DIP)

High-level modules depend on abstractions, not on concrete low-level details. In Spring, this means depending on interfaces (ports) injected through the constructor.

Bad: a use case instantiating a concrete JPA repository directly.

```java
public class PlaceOrderUseCase {
    private final OrderJpaRepository repository = new OrderJpaRepository();
}
```

Good: depend on a port; the concrete adapter is wired by Spring.

```java
public class PlaceOrderUseCase {
    private final OrderRepositoryPort repository;

    public PlaceOrderUseCase(OrderRepositoryPort repository) {
        this.repository = repository;
    }
}
```

## Checklist to apply on every class

- Does the class have more than one reason to change? Split it.
- Does adding a new case require editing an existing conditional chain? Extract a strategy/polymorphism instead.
- Does a subtype narrow or break a contract the supertype promises? Redesign the hierarchy or the interface.
- Does an interface force implementers to support methods they cannot meaningfully implement? Split the interface.
- Does a class construct its own dependencies with `new` instead of receiving them? Inject the abstraction through the constructor.
