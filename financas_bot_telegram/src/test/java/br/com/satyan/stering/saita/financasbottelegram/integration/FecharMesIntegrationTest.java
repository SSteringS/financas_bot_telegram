package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Testes de integração do endpoint {@code POST /api/funcionarios/{id}/fechamentos}.
 *
 * <p>Usa MySQL real via Testcontainers (herdado de {@link AbstractIntegrationTest}).
 * Verifica fluxo completo E2E com banco real: criação de Pedido FOLHA, marcação
 * de vales como fechados e idempotência (409 no segundo fechamento do mesmo mês).
 */
class FecharMesIntegrationTest extends AbstractIntegrationTest {

    private Long funcionarioId;

    @BeforeEach
    void setUp() {
        // Criar funcionário de teste
        jdbcTemplate.update("""
                INSERT INTO funcionario (nome, salario_base, forma_pagamento, chave_pix, ativo)
                VALUES ('Maria Teste', 2000.00, 'PIX', 'maria@pix.com', TRUE)
                """);
        funcionarioId = jdbcTemplate.queryForObject(
                "SELECT id FROM funcionario WHERE nome = 'Maria Teste' ORDER BY id DESC LIMIT 1",
                Long.class);
    }

    @AfterEach
    void tearDown() {
        if (funcionarioId != null) {
            // Ordem importa: FK constraints
            jdbcTemplate.update("DELETE FROM pedidos_pagamento WHERE funcionario_id = ?", funcionarioId);
            jdbcTemplate.update("DELETE FROM adiantamento WHERE funcionario_id = ?", funcionarioId);
            jdbcTemplate.update("DELETE FROM funcionario WHERE id = ?", funcionarioId);
        }
    }

    @Test
    void deveFecharMesComSucesso() {
        // Criar vale de R$150 para o funcionário em maio/2026
        jdbcTemplate.update("""
                INSERT INTO pedidos_pagamento
                    (funcionario_id, valor, descricao, status, categoria, fechado, data_pedido, data_criacao)
                VALUES (?, 150.00, 'Vale alimentacao', 'PENDENTE', 'VALE', FALSE, '2026-05-10', NOW())
                """, funcionarioId);

        String requestBody = """
                { "mes": "2026-05", "ajuste": 0.00 }
                """;

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/funcionarios/" + funcionarioId + "/fechamentos",
                requestBodyEntity(requestBody),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("PENDENTE");
        assertThat(resp.getBody()).contains("Salario base");
        assertThat(resp.getBody()).contains("Liquido");

        // Vale deve estar marcado como fechado
        Boolean valeFechado = jdbcTemplate.queryForObject(
                "SELECT fechado FROM pedidos_pagamento WHERE funcionario_id = ? AND categoria = 'VALE'",
                Boolean.class, funcionarioId);
        assertThat(valeFechado).isTrue();

        // Pedido FOLHA deve ter sido criado
        Integer countFolha = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedidos_pagamento WHERE funcionario_id = ? AND categoria = 'FOLHA'",
                Integer.class, funcionarioId);
        assertThat(countFolha).isEqualTo(1);
    }

    @Test
    void deveRetornar409NoFechamentoDuplicado() {
        String requestBody = """
                { "mes": "2026-04", "ajuste": 0.00 }
                """;

        // Primeiro fechamento
        ResponseEntity<String> primeiro = restTemplate.postForEntity(
                "/api/funcionarios/" + funcionarioId + "/fechamentos",
                requestBodyEntity(requestBody),
                String.class);
        assertThat(primeiro.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Segundo fechamento do mesmo mês → 409
        ResponseEntity<String> segundo = restTemplate.postForEntity(
                "/api/funcionarios/" + funcionarioId + "/fechamentos",
                requestBodyEntity(requestBody),
                String.class);
        assertThat(segundo.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(segundo.getBody()).contains("FECHAMENTO_DUPLICADO");
    }

    @Test
    void deveRetornar404ParaFuncionarioInexistente() {
        String requestBody = """
                { "mes": "2026-06", "ajuste": 0.00 }
                """;

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/funcionarios/999999/fechamentos",
                requestBodyEntity(requestBody),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deveListarFechamentosAnteriores() {
        // Fecha dois meses diferentes
        restTemplate.postForEntity(
                "/api/funcionarios/" + funcionarioId + "/fechamentos",
                requestBodyEntity("{ \"mes\": \"2026-03\", \"ajuste\": 0.00 }"),
                String.class);
        restTemplate.postForEntity(
                "/api/funcionarios/" + funcionarioId + "/fechamentos",
                requestBodyEntity("{ \"mes\": \"2026-02\", \"ajuste\": 0.00 }"),
                String.class);

        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/funcionarios/" + funcionarioId + "/fechamentos",
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Março deve aparecer antes de fevereiro (mes_referencia DESC)
        assertThat(resp.getBody()).containsPattern("2026-03.*2026-02");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private org.springframework.http.HttpEntity<String> requestBodyEntity(String json) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new org.springframework.http.HttpEntity<>(json, headers);
    }
}
