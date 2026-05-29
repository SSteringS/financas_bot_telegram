# BE-14 — Testes de integração E2E com Testcontainers (MySQL real)

> Última tarefa do bloco de backend. Sobe MySQL via Testcontainers, aplica Flyway, exercita o fluxo HTTP completo (auth + listagem + detalhe + imagem + resumo) contra banco real. Garante que migrations, queries JPA, mappers e endpoints estão coesos end-to-end.

---

## Pré-requisitos

- Todas as BE anteriores mergeadas (BE-03, BE-05, BE-06, BE-07, BE-08, BE-09, BE-10, BE-11, BE-12, BE-13, BE-15)
- Docker rodando localmente (Testcontainers precisa)

---

## Dependências novas no `pom.xml`

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
```

---

## Arquivos esperados

**Novos:**
- `src/test/java/.../integration/AbstractIntegrationTest.java` — classe base com `@Testcontainers` + container MySQL + `@DynamicPropertySource` apontando datasource pro container
- `src/test/java/.../integration/AuthFlowIntegrationTest.java`
- `src/test/java/.../integration/PedidosListIntegrationTest.java`
- `src/test/java/.../integration/PedidoDetalheIntegrationTest.java`
- `src/test/java/.../integration/ImagemEndpointsIntegrationTest.java`
- `src/test/java/.../integration/ResumoIntegrationTest.java`
- `src/test/java/.../integration/IsolamentoRequisitanteIntegrationTest.java` — **importante**: 2 requisitantes, garante que A não vê dados de B em nenhum endpoint
- `src/test/resources/integration-test-data.sql` — seed de dados pra os testes (opcional, ou criar via @BeforeEach)

**Modificados:**
- `pom.xml`

---

## Código-chave

### `AbstractIntegrationTest`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    @Container
    @SuppressWarnings("resource")  // gerenciado pelo Testcontainers
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("financas_bot_telegram_db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);  // acelera reexecuções locais

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        // Outras configs pra teste:
        registry.add("app.admin.api-key", () -> "test-admin-key");
        registry.add("app.jwt.secret", () -> "test-jwt-secret-pra-integracao-min-32-chars");
        registry.add("app.jwt.ttl-dias", () -> "1");
        registry.add("app.frontend.base-url", () -> "http://localhost:5173");
        registry.add("app.cors.allowed-origin", () -> "http://localhost:5173");
        registry.add("telegram.allowed-user-ids", () -> "999");
        registry.add("telegram.bot-token", () -> "test-token");
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
```

Criar `application-integration-test.properties` em `src/test/resources/` se for útil pra config adicional.

### Exemplo: `AuthFlowIntegrationTest`

```java
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired GerarTokenConviteUseCase gerarConvite;

    @Test
    void fluxoCompletoDeAuth_geraToken_exchange_meRetornaRequisitante() {
        // 1. Garante que existe requisitante 1 (seed da V2)
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM requisitante WHERE id = 1", Integer.class);
        assertThat(count).isEqualTo(1);

        // 2. Gera convite
        String url = gerarConvite.gerar(1L);
        String token = url.substring(url.indexOf("?t=") + 3);

        // 3. Exchange
        ResponseEntity<String> exchangeResp = restTemplate.postForEntity(
                "/api/v1/auth/exchange", new AuthExchangeRequest(token), String.class);
        assertThat(exchangeResp.getStatusCode().value()).isEqualTo(200);

        String cookie = exchangeResp.getHeaders().getFirst("Set-Cookie");
        assertThat(cookie).contains("finbot_session=");
        assertThat(cookie).contains("HttpOnly");

        // 4. /me com o cookie
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        ResponseEntity<AuthMeResponse> meResp = restTemplate.exchange(
                "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), AuthMeResponse.class);
        assertThat(meResp.getStatusCode().value()).isEqualTo(200);
        assertThat(meResp.getBody().requisitante().id()).isEqualTo(1L);

        // 5. Token agora foi usado — segundo exchange falha
        ResponseEntity<String> exchangeNovamente = restTemplate.postForEntity(
                "/api/v1/auth/exchange", new AuthExchangeRequest(token), String.class);
        assertThat(exchangeNovamente.getStatusCode().value()).isEqualTo(401);
    }
}
```

### `IsolamentoRequisitanteIntegrationTest`

```java
class IsolamentoRequisitanteIntegrationTest extends AbstractIntegrationTest {

    @BeforeEach
    void seed() {
        // Cria 2 requisitantes (1 já existe; cria mais um)
        jdbcTemplate.update(
            "INSERT INTO requisitante (id, nome, telefone, ativo, criado_em) VALUES (?, ?, ?, ?, NOW())",
            2L, "Outro Requisitante", "+5511000000000", true);

        // Cria 1 pedido pra cada
        jdbcTemplate.update("""
            INSERT INTO pedidos_pagamento (requisitante_id, telegram_user_id, valor, descricao,
                                            status, data_pedido, data_criacao)
            VALUES (1, '999', 100.00, 'Pedido do req 1', 'PENDENTE', CURRENT_DATE, NOW()),
                   (2, '999', 200.00, 'Pedido do req 2', 'PENDENTE', CURRENT_DATE, NOW())
            """);
    }

    @Test
    void requisitante1NaoVePedidosDoRequisitante2() {
        // Auth como requisitante 1
        String cookie = autenticarComo(1L);

        ResponseEntity<PaginaDTO<PedidoResumoDTO>> resp = listarPedidos(cookie);
        assertThat(resp.getBody().items())
                .allMatch(p -> p.descricao().equals("Pedido do req 1"));
    }
    
    // Helper autenticarComo, listarPedidos, etc
}
```

---

## Critério de aceitação

- [ ] `./mvnw test` (sem perfil especial) **deve** continuar passando os 55+ testes unitários **e** rodar os novos testes de integração (Testcontainers sobe MySQL automaticamente — espera 30-60s na primeira execução)
- [ ] Cobertura mínima dos testes de integração:
  - Auth flow completo (gerar convite → exchange → /me)
  - Listagem com filtros (status, tipo, datas, busca, paginação) retornando dados corretos
  - Detalhe (200, 404, 403)
  - Endpoints de imagem (302 com Location apontando pra pre-signed URL — não precisa baixar o objeto S3 real; mockar StorageService se for o caso)
  - Resumo agregado por mês com dados conhecidos
  - **Isolamento por requisitante** — esse é o teste mais importante: 2 requisitantes, 2 pedidos, cada um só vê o seu
- [ ] Testcontainers configurado com `withReuse(true)` pra acelerar reexecuções locais
- [ ] CI roda os integration tests (verificar se Docker está disponível no runner do GitHub Actions; pode precisar adicionar setup-docker step)
- [ ] Sem race conditions: cada teste limpa/seeda seu próprio estado em `@BeforeEach`
- [ ] Tempo total de `./mvnw test` < 2 min (warm cache)

---

## Fora de escopo

- Testes de carga / performance
- Testes de webhook do Telegram (já cobertos por unit tests do BE-01a)
- Testes do front

---

## Status report

`docs/status/BE-14-testes-integracao.md`. Cobrir:

- Output completo de `./mvnw test` mostrando passos do Testcontainers + total
- Lista das 6 classes de integration test criadas com contagem de métodos
- Tempo de execução cold start vs warm
- **Marco final**: depois desta tarefa, **bloco backend da Fase 3 está completo**. Próximo: deploy (DEP-00 em diante). Ou paralelizar com finalização do frontend.
