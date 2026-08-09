package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Testes de integração para CRUD de funcionários.
 *
 * <p>Cobre os fluxos: POST (criação), GET (listagem e busca por ID), PUT (atualização)
 * e DELETE (soft delete) usando MySQL real via Testcontainers e autenticação JWT.
 * URLs: {@code /api/v1/funcionarios/**}.
 */
class FuncionarioCRUDIntegrationTest extends AbstractIntegrationTest {

    private String cookie;
    private Long funcionarioId;

    @BeforeEach
    void setUp() {
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

    // ── Helper para PUT autenticado (não está em AbstractIntegrationTest) ─────

    private <T> ResponseEntity<T> putAutenticado(String url, String cookie, String jsonBody,
            Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(jsonBody, headers), responseType);
    }

    // ── Testes ────────────────────────────────────────────────────────────────

    @Test
    void deveCriarFuncionarioPix() {
        String body = """
                {"nome":"Ana CRUD","salarioBase":2500.00,"formaPagamento":"PIX","chavePix":"ana.crud@pix.com"}
                """;

        ResponseEntity<String> resp = postAutenticado(
                "/api/v1/funcionarios", cookie, body, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).contains("Ana CRUD");

        // Persiste no banco
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM funcionario WHERE nome = 'Ana CRUD' AND ativo = TRUE",
                Integer.class);
        assertThat(count).isEqualTo(1);

        // Guarda id para tearDown
        funcionarioId = jdbcTemplate.queryForObject(
                "SELECT id FROM funcionario WHERE nome = 'Ana CRUD' ORDER BY id DESC LIMIT 1",
                Long.class);
    }

    @Test
    void deveListarFuncionariosAtivos() {
        // Criar funcionário para garantir que a lista não está vazia
        jdbcTemplate.update("""
                INSERT INTO funcionario (nome, salario_base, forma_pagamento, chave_pix, ativo)
                VALUES ('Lista Teste', 1900.00, 'PIX', 'lista@pix.com', TRUE)
                """);
        funcionarioId = jdbcTemplate.queryForObject(
                "SELECT id FROM funcionario WHERE nome = 'Lista Teste' ORDER BY id DESC LIMIT 1",
                Long.class);

        ResponseEntity<String> resp = getAutenticado(
                "/api/v1/funcionarios", cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Lista Teste");
    }

    @Test
    void deveBuscarFuncionarioPorId() {
        jdbcTemplate.update("""
                INSERT INTO funcionario (nome, salario_base, forma_pagamento, chave_pix, ativo)
                VALUES ('Busca ID', 2000.00, 'PIX', 'buscaid@pix.com', TRUE)
                """);
        funcionarioId = jdbcTemplate.queryForObject(
                "SELECT id FROM funcionario WHERE nome = 'Busca ID' ORDER BY id DESC LIMIT 1",
                Long.class);

        ResponseEntity<String> resp = getAutenticado(
                "/api/v1/funcionarios/" + funcionarioId, cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Busca ID");
    }

    @Test
    void deveRetornar404ParaFuncionarioInexistente() {
        ResponseEntity<String> resp = getAutenticado(
                "/api/v1/funcionarios/999999", cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deveAtualizarSalarioDoFuncionario() {
        jdbcTemplate.update("""
                INSERT INTO funcionario (nome, salario_base, forma_pagamento, chave_pix, ativo)
                VALUES ('Atualizar Teste', 1800.00, 'PIX', 'atualizar@pix.com', TRUE)
                """);
        funcionarioId = jdbcTemplate.queryForObject(
                "SELECT id FROM funcionario WHERE nome = 'Atualizar Teste' ORDER BY id DESC LIMIT 1",
                Long.class);

        String bodyAtualizado = """
                {"nome":"Atualizar Teste","salarioBase":2800.00,
                 "formaPagamento":"PIX","chavePix":"atualizar@pix.com"}
                """;

        ResponseEntity<String> resp = putAutenticado(
                "/api/v1/funcionarios/" + funcionarioId, cookie, bodyAtualizado, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("2800");

        // Confirma no banco
        Double salarioBanco = jdbcTemplate.queryForObject(
                "SELECT salario_base FROM funcionario WHERE id = ?",
                Double.class, funcionarioId);
        assertThat(salarioBanco).isEqualTo(2800.00);
    }

    @Test
    void deveDesativarFuncionarioViaSoftDelete() {
        jdbcTemplate.update("""
                INSERT INTO funcionario (nome, salario_base, forma_pagamento, chave_pix, ativo)
                VALUES ('Deletar Teste', 1700.00, 'PIX', 'deletar@pix.com', TRUE)
                """);
        funcionarioId = jdbcTemplate.queryForObject(
                "SELECT id FROM funcionario WHERE nome = 'Deletar Teste' ORDER BY id DESC LIMIT 1",
                Long.class);

        ResponseEntity<Void> resp = deleteAutenticado(
                "/api/v1/funcionarios/" + funcionarioId, cookie);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verifica soft delete — ativo=false
        Boolean ativo = jdbcTemplate.queryForObject(
                "SELECT ativo FROM funcionario WHERE id = ?",
                Boolean.class, funcionarioId);
        assertThat(ativo).isFalse();
    }

    @Test
    void deveRetornar401SemAutenticacao() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/v1/funcionarios", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
