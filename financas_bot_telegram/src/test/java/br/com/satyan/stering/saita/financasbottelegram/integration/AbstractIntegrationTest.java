package br.com.satyan.stering.saita.financasbottelegram.integration;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.AuthExchangeRequest;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.StorageService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.GerarTokenConviteUseCase;
import io.awspring.cloud.s3.S3Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("integration-test")
public abstract class AbstractIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("financas_bot_telegram_db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
    }

    @MockBean
    TelegramMessageSenderService telegramMessageSenderService;

    @MockBean
    S3Template s3Template;

    @MockBean
    StorageService storageService;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    GerarTokenConviteUseCase gerarConviteUseCase;

    protected String autenticarComo(Long requisitanteId) {
        String urlConvite = gerarConviteUseCase.gerar(requisitanteId);
        String token = urlConvite.substring(urlConvite.indexOf("?t=") + 3);

        ResponseEntity<String> exchangeResp = restTemplate.postForEntity(
                "/api/v1/auth/exchange",
                new AuthExchangeRequest(token),
                String.class);

        String setCookie = exchangeResp.getHeaders().getFirst("Set-Cookie");
        if (setCookie == null) {
            throw new IllegalStateException("Exchange não retornou cookie — status: " + exchangeResp.getStatusCode());
        }
        return setCookie.split(";")[0];
    }

    protected <T> ResponseEntity<T> getAutenticado(String url, String cookie, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }
}
