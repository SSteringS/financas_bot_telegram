# JUnit 5, Mockito, and AssertJ for Java/Spring

## Table of contents

- Plain unit test with Mockito
- Asserting collaborator arguments
- Controller slice test with MockMvc
- Repository slice test with @DataJpaTest
- Naming and structure

## Plain unit test with Mockito

Default choice for services, use cases, and any class whose dependencies can be mocked.

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
    void should_saveOrder_when_totalIsPositive() {
        CustomerId customerId = new CustomerId(1L);
        BigDecimal total = new BigDecimal("100.00");
        given(repository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        Order result = service.placeOrder(customerId, total);

        assertThat(result.total()).isEqualByComparingTo(total);
        verify(repository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().customerId()).isEqualTo(customerId);
        assertThat(orderCaptor.getValue().total()).isEqualByComparingTo(total);
    }

    @Test
    void should_throwException_when_totalIsNotPositive() {
        CustomerId customerId = new CustomerId(1L);

        assertThatThrownBy(() -> service.placeOrder(customerId, BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");

        verifyNoInteractions(repository);
    }
}
```

## Asserting collaborator arguments

When the interaction with a collaborator is the behavioral assertion of the test, assert the argument values. Two mechanisms cover the two shapes; pick by the shape of the collaborator's signature.

### 1. `ArgumentCaptor` — the class under test builds a domain object

Use when the collaborator receives an object assembled inside the class under test, and the fields of that object are the behavior.

```java
ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

verify(repository).save(orderCaptor.capture());

Order savedOrder = orderCaptor.getValue();
assertThat(savedOrder.customerId()).isEqualTo(customerId);
assertThat(savedOrder.total()).isEqualByComparingTo(total);
```

### 2. `eq(...)` matchers — the collaborator takes loose parameters

Use when there is no object to capture, only separate arguments. Pin each meaningful argument inside the `verify` itself. Mockito requires that if one argument uses a matcher, all of them do, so arguments the test cannot pin (a clock-derived timestamp, for example) stay as `any(...)`.

```java
verify(notifier).notify(eq(customerId), eq("ORDER_PLACED"), eq(total), any(Instant.class));
```

### Counterexample — what not to write

```java
// Weak: proves the call happened, not that the values handed over are correct.
// A wrong customerId or a wrong total still passes this test.
verify(repository).save(any(Order.class));
```

`any(...)` is legitimate for stubbing (`given(repository.save(any(Order.class)))...`) and for arguments the test cannot determine. It is not a substitute for asserting the values the behavior produces.

## Controller slice test with MockMvc

Use `@WebMvcTest` to test a controller in isolation, mocking the use case it depends on. Serialize the request body with the injected `ObjectMapper` instead of hand-writing JSON strings.

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlaceOrderUseCase placeOrderUseCase;

    @Test
    void should_returnCreated_when_requestIsValid() throws Exception {
        OrderRequest request = new OrderRequest(1L, new BigDecimal("50.00"));
        Order order = Order.create(new CustomerId(1L), new BigDecimal("50.00"));
        given(placeOrderUseCase.placeOrder(any(), any())).willReturn(order);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value(1));
    }
}
```

## Repository slice test with @DataJpaTest

Use `@DataJpaTest` only when the goal is verifying a custom query or mapping, not full application wiring.

```java
@DataJpaTest
class OrderJpaRepositoryTest {
    @Autowired
    private OrderJpaRepository repository;

    @Test
    void should_persistAndAssignId_when_saveIsCalled() {
        OrderJpaEntity entity = new OrderJpaEntity();

        OrderJpaEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
    }
}
```

## Naming and structure

- Method names describe behavior: `should_ExpectedBehavior_when_StateUnderTest`.
- Follow Arrange-Act-Assert; a blank line between each section is enough, comments are optional.
- One assertion topic per test; use multiple `assertThat` calls only when they check the same behavior from different angles.
