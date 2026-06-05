package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Testes de integração para os endpoints de adiantamentos.
 *
 * <p>Cobre POST (criação), DELETE (cancelamento/soft delete) e GET (listagem
 * de ativos) usando MySQL real via Testcontainers e autenticação JWT.
 * URLs: {@code /api/v1/funcionarios/{id}/adiantamentos} e
 * {@code /api/v1/funcionarios/adiantamentos/{adiantamentoId}}.
 */
class AdiantamentoIntegrationTest extends AbstractIntegrationTest {

    private Long funcionarioId;
    private String cookie;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                INSERT INTO funcionario (nome, salario_base, forma_pagamento, chave_pix, ativo)
                VALUES ('Carlos Adiant', 3000.00, 'PIX', 'carlos.adiant@pix.com', TRUE)
                """);
        funcionarioId = jdbcTemplate.queryForObject(
                "SELECT id FROM funcionario WHERE nome = 'Carlos Adiant' ORDER BY id DESC LIMIT 1",
                Long.class);
        cookie = autenticarComo(1L);
    }

    @AfterEach
    void tearDown() {
        if (funcionarioId != null) {
            jdbcTemplate.update(
                    "DELETE FROM pedidos_pagamento WHERE funcionario_id = ?", funcionarioId);
            jdbcTemplate.update("DELETE FROM adiantamento WHERE funcionario_id = ?", funcionarioId);
            jdbcTemplate.update("DELETE FROM funcionario WHERE id = ?", funcionarioId);
        }
    }

    @Test
    void deveCadastrarAdiantamentoEPersistirNoBanco() {
        String body = """
                {"descricao":"Empréstimo equipamento","valorTotal":600.00,
                 "valorParcela":200.00,"numParcelas":3,"dataInicio":"2026-03-01"}
                """;

        ResponseEntity<String> resp = postAutenticado(
                "/api/v1/funcionarios/" + funcionarioId + "/adiantamentos",
                cookie, body, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).contains("Empréstimo equipamento");
        assertThat(resp.getBody()).contains("parcelasRestantes");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM adiantamento WHERE funcionario_id = ? AND ativo = TRUE",
                Integer.class, funcionarioId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void deveCancelarAdiantamentoViaSoftDelete() {
        // Inserir adiantamento ativo diretamente no banco
        jdbcTemplate.update("""
                INSERT INTO adiantamento
                    (funcionario_id, descricao, valor_total, valor_parcela,
                     num_parcelas, parcelas_pagas, data_inicio, ativo)
                VALUES (?, 'Adiantamento para cancelar', 300.00, 100.00, 3, 0, '2026-01-01', TRUE)
                """, funcionarioId);

        Long adiantamentoId = jdbcTemplate.queryForObject(
                "SELECT id FROM adiantamento WHERE funcionario_id = ? ORDER BY id DESC LIMIT 1",
                Long.class, funcionarioId);

        ResponseEntity<Void> resp = deleteAutenticado(
                "/api/v1/funcionarios/adiantamentos/" + adiantamentoId, cookie);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verifica soft delete — ativo=false no banco
        Boolean ativo = jdbcTemplate.queryForObject(
                "SELECT ativo FROM adiantamento WHERE id = ?",
                Boolean.class, adiantamentoId);
        assertThat(ativo).isFalse();
    }

    @Test
    void deveListarApenaAdiantamentosAtivos() {
        jdbcTemplate.update("""
                INSERT INTO adiantamento
                    (funcionario_id, descricao, valor_total, valor_parcela,
                     num_parcelas, parcelas_pagas, data_inicio, ativo)
                VALUES (?, 'Adiantamento ativo', 300.00, 100.00, 3, 0, '2026-01-01', TRUE)
                """, funcionarioId);
        jdbcTemplate.update("""
                INSERT INTO adiantamento
                    (funcionario_id, descricao, valor_total, valor_parcela,
                     num_parcelas, parcelas_pagas, data_inicio, ativo)
                VALUES (?, 'Adiantamento quitado', 300.00, 100.00, 3, 3, '2025-01-01', FALSE)
                """, funcionarioId);

        ResponseEntity<String> resp = getAutenticado(
                "/api/v1/funcionarios/" + funcionarioId + "/adiantamentos",
                cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Adiantamento ativo");
        assertThat(resp.getBody()).doesNotContain("Adiantamento quitado");
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaAdiantamentos() {
        ResponseEntity<String> resp = getAutenticado(
                "/api/v1/funcionarios/" + funcionarioId + "/adiantamentos",
                cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo("[]");
    }

    @Test
    void deveRetornar401SemAutenticacaoNoCadastro() {
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/funcionarios/" + funcionarioId + "/adiantamentos",
                null, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
