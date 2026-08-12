# Example: Full test class for PlaceOrderService

## Table of contents

- Test class
- Coverage notes

## Test class

```java
@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {
    @Mock
    private OrderRepositoryPort repository;

    private PlaceOrderService service;

    @BeforeEach
    void setUp() {
        service = new PlaceOrderService(repository);
    }

    @Test
    void should_returnSavedOrder_when_totalIsPositive() {
        CustomerId customerId = new CustomerId(1L);
        BigDecimal total = new BigDecimal("100.00");
        given(repository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        Order result = service.placeOrder(customerId, total);

        assertThat(result.customerId()).isEqualTo(customerId);
        assertThat(result.total()).isEqualByComparingTo(total);
    }

    @Test
    void should_saveOrderWithCustomerIdAndTotal_when_orderIsPlaced() {
        CustomerId customerId = new CustomerId(1L);
        BigDecimal total = new BigDecimal("100.00");
        given(repository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        service.placeOrder(customerId, total);

        verify(repository, times(1)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.customerId()).isEqualTo(customerId);
        assertThat(savedOrder.total()).isEqualByComparingTo(total);
    }

    @Test
    void should_throwIllegalArgumentException_when_totalIsZero() {
        CustomerId customerId = new CustomerId(1L);

        assertThatThrownBy(() -> service.placeOrder(customerId, BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void should_throwIllegalArgumentException_when_totalIsNegative() {
        CustomerId customerId = new CustomerId(1L);

        assertThatThrownBy(() -> service.placeOrder(customerId, new BigDecimal("-10.00")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

## Coverage notes

- Happy path: order is saved and returned with correct fields.
- Interaction: `repository.save` is called exactly once, and the `Order` handed to it is captured so its `customerId` and `total` are asserted — the values are checked, not just the occurrence of the call.
- Edge cases: zero and negative totals both raise `IllegalArgumentException`.
- Not covered here: persistence correctness (belongs to a `@DataJpaTest` for `OrderJpaRepository`) and HTTP contract (belongs to a `@WebMvcTest` for `OrderController`).
