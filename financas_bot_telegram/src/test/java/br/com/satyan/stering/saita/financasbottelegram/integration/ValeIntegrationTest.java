package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Testes de integração para os endpoints de vales.
 *
 * <p>Cobre o fluxo HTTP+DB completo: {@code POST /api/v1/funcionarios/{id}/vales}
 * (criação) e {@code GET /api/v1/funcionarios/{id}/vales?mes=YYYY-MM} (listagem
 * filtrada por mês). Usa MySQL real via Testcontainers e autenticação JWT via
 * {@link #autenticarComo(Long)}.
 */
class ValeIntegrationTest extends AbstractIntegrationTest {

    private Long funcionarioId;
    private String cookie;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                INSERT INTO funcionario (nome, salario_base, forma_pagamento, chave_pix, ativo)
                VALUES ('Maria Vales', 1800.00, 'PIX', 'mariavales@pix.com', TRUE)
                """);
        funcionarioId = jdbcTemplate.queryForObject(
                "SELECT id FROM funcionario WHERE nome = 'Maria Vales' ORDER BY id DESC LIMIT 1",
                Long.class);
        cookie = autenticarComo(1L);
    }

    @AfterEach
    void tearDown() {
        if (funcionarioId != null) {
            jdbcTemplate.update(
                    "DELETE FROM pedidos_pagamento WHERE funcionario_id = ?", funcionarioId);
            jdbcTemplate.update("DELETE FROM funcionario WHERE id = ?", funcionarioId);
        }
    }

    @Test
    void deveCadastrarValeEPersistirNoBanco() {
        String body = """
                {"descricao":"Vale alimentação","valor":150.00,"dataPedido":"2026-05-10"}
                """;

        ResponseEntity<String> resp = postAutenticado(
                "/api/v1/funcionarios/" + funcionarioId + "/vales",
                cookie, body, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).contains("Vale alimentação");
        assertThat(resp.getBody()).contains("VALE");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedidos_pagamento WHERE funcionario_id = ? AND categoria = 'VALE'",
                Integer.class, funcionarioId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void deveListarValesFiltradosPorMes() {
        // Vale em maio/2026 — deve aparecer
        jdbcTemplate.update("""
                INSERT INTO pedidos_pagamento
                    (funcionario_id, valor, descricao, status, categoria, fechado, data_pedido, data_criacao)
                VALUES (?, 100.00, 'Vale maio', 'PENDENTE', 'VALE', FALSE, '2026-05-15', NOW())
                """, funcionarioId);

        // Vale em junho/2026 — NÃO deve aparecer no filtro de maio
        jdbcTemplate.update("""
                INSERT INTO pedidos_pagamento
                    (funcionario_id, valor, descricao, status, categoria, fechado, data_pedido, data_criacao)
                VALUES (?, 200.00, 'Vale junho', 'PENDENTE', 'VALE', FALSE, '2026-06-01', NOW())
                """, funcionarioId);

        ResponseEntity<String> resp = getAutenticado(
                "/api/v1/funcionarios/" + funcionarioId + "/vales?mes=2026-05",
                cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Vale maio");
        assertThat(resp.getBody()).doesNotContain("Vale junho");
    }

    @Test
    void deveRetornarListaVaziaParaMesSemVales() {
        ResponseEntity<String> resp = getAutenticado(
                "/api/v1/funcionarios/" + funcionarioId + "/vales?mes=2024-01",
                cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo("[]");
    }

    @Test
    void deveRetornar400ParaMesFormatoInvalido() {
        ResponseEntity<String> resp = getAutenticado(
                "/api/v1/funcionarios/" + funcionarioId + "/vales?mes=invalido",
                cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deveRetornar401SemAutenticacao() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/v1/funcionarios/" + funcionarioId + "/vales",
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
