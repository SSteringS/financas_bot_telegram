package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IsolamentoRequisitanteIntegrationTest extends AbstractIntegrationTest {

    private Long req2Id;
    private String cookieReq1;
    private String cookieReq2;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM comprovantes WHERE pedido_id IN (SELECT id FROM pedidos_pagamento WHERE requisitante_id = 1)");
        jdbcTemplate.update("DELETE FROM pedidos_pagamento WHERE requisitante_id = 1");

        jdbcTemplate.update(
                "INSERT INTO requisitante (nome, telefone, ativo, criado_em) VALUES ('Req2 IT', '+5500000000002', true, NOW())");
        req2Id = jdbcTemplate.queryForObject(
                "SELECT id FROM requisitante WHERE nome = 'Req2 IT' ORDER BY id DESC LIMIT 1", Long.class);

        jdbcTemplate.update("DELETE FROM pedidos_pagamento WHERE requisitante_id = ?", req2Id);

        jdbcTemplate.update("""
                INSERT INTO pedidos_pagamento (requisitante_id, valor, descricao, status, tipo, data_pedido, data_criacao)
                VALUES (1, 100.00, 'Pedido Req1', 'PENDENTE', 'PIX', CURDATE(), NOW()),
                       (?, 200.00, 'Pedido Req2', 'PENDENTE', 'BOLETO', CURDATE(), NOW())
                """, req2Id);

        cookieReq1 = autenticarComo(1L);
        cookieReq2 = autenticarComo(req2Id);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM comprovantes WHERE pedido_id IN (SELECT id FROM pedidos_pagamento WHERE requisitante_id = ?)", req2Id);
        jdbcTemplate.update("DELETE FROM pedidos_pagamento WHERE requisitante_id = ?", req2Id);
        jdbcTemplate.update("DELETE FROM auth_token WHERE requisitante_id = ?", req2Id);
        jdbcTemplate.update("DELETE FROM requisitante WHERE id = ?", req2Id);
    }

    @Test
    void requisitante1NaoVePedidosDoRequisitante2() {
        ResponseEntity<String> resp = getAutenticado("/api/v1/pedidos", cookieReq1, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Pedido Req1").doesNotContain("Pedido Req2");
    }

    @Test
    void requisitante2NaoVePedidosDoRequisitante1() {
        ResponseEntity<String> resp = getAutenticado("/api/v1/pedidos", cookieReq2, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Pedido Req2").doesNotContain("Pedido Req1");
    }

    @Test
    void requisitante1NaoAcessaPedidoDoRequisitante2() {
        Long pedidoReq2 = jdbcTemplate.queryForObject(
                "SELECT id FROM pedidos_pagamento WHERE requisitante_id = ? ORDER BY id DESC LIMIT 1",
                Long.class, req2Id);

        ResponseEntity<String> resp = getAutenticado("/api/v1/pedidos/" + pedidoReq2, cookieReq1, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void resumoDeRequisitante1NaoIncluidDadosDoRequisitante2() {
        ResponseEntity<String> resp = getAutenticado("/api/v1/resumo", cookieReq1, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).doesNotContain("200");
    }
}
