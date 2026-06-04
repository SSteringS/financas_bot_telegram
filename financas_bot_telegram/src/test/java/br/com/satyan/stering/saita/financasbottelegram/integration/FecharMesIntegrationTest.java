package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AdiantamentoRepositoryPortOut;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
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

    /** Spy injetado para testar rollback transacional (B3). Reset em {@link #resetarSpy()}. */
    @SpyBean
    private AdiantamentoRepositoryPortOut adiantamentoSpyRepository;

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

    /** Garante que o spy não afete outros testes após B3. */
    @AfterEach
    void resetarSpy() {
        Mockito.reset(adiantamentoSpyRepository);
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

    // ── B3: atomicidade transacional ──────────────────────────────────────────

    /**
     * B3: falha no passo 10 (adiantamento.save) deve reverter toda a transação.
     *
     * <p>Usa {@link SpyBean} para injetar uma {@link RuntimeException} no momento em que
     * {@code FecharMesServiceImpl} tenta persistir o adiantamento atualizado.
     * A annotation {@code @Transactional} do use case deve garantir rollback completo:
     * o pedido FOLHA criado no passo 8 não pode aparecer no banco após a falha.
     */
    @Test
    void deveReverterTransacaoEmCasoDeErroNoPasso10() {
        // Criar adiantamento ativo para que o passo 10 seja atingido
        jdbcTemplate.update("""
                INSERT INTO adiantamento
                    (funcionario_id, descricao, valor_total, valor_parcela, num_parcelas,
                     parcelas_pagas, data_inicio, ativo)
                VALUES (?, 'Adiantamento B3', 300.00, 100.00, 3, 0, '2026-01-01', TRUE)
                """, funcionarioId);

        // Spy: lança RuntimeException quando save() é chamado no passo 10
        doThrow(new RuntimeException("falha simulada no passo 10 — B3"))
                .when(adiantamentoSpyRepository).save(any());

        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/funcionarios/" + funcionarioId + "/fechamentos",
                requestBodyEntity("{ \"mes\": \"2026-07\", \"ajuste\": 0.00 }"),
                String.class);

        // Transação deve ter sido revertida → 5xx do servidor
        assertThat(resp.getStatusCode().is5xxServerError()).isTrue();

        // Passo 8 (INSERT pedido FOLHA) deve ter sido revertido
        Integer countFolha = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedidos_pagamento WHERE funcionario_id = ? AND categoria = 'FOLHA'",
                Integer.class, funcionarioId);
        assertThat(countFolha)
                .as("Pedido FOLHA não deveria existir após rollback da transação")
                .isEqualTo(0);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private org.springframework.http.HttpEntity<String> requestBodyEntity(String json) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new org.springframework.http.HttpEntity<>(json, headers);
    }
}
