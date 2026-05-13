package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PedidoDetalheIntegrationTest extends AbstractIntegrationTest {

    private String cookie;
    private Long pedidoId;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM comprovantes WHERE pedido_id IN (SELECT id FROM pedidos_pagamento WHERE requisitante_id = 1)");
        jdbcTemplate.update("DELETE FROM pedidos_pagamento WHERE requisitante_id = 1");

        jdbcTemplate.update("""
                INSERT INTO pedidos_pagamento (requisitante_id, valor, descricao, status, tipo, data_pedido, data_criacao)
                VALUES (1, 300.00, 'Aluguel', 'PENDENTE', 'TED', CURDATE(), NOW())
                """);

        pedidoId = jdbcTemplate.queryForObject(
                "SELECT id FROM pedidos_pagamento WHERE descricao = 'Aluguel' AND requisitante_id = 1 ORDER BY id DESC LIMIT 1",
                Long.class);

        cookie = autenticarComo(1L);
    }

    @Test
    void buscarPedido_existente_retorna200() {
        ResponseEntity<String> resp = getAutenticado("/api/v1/pedidos/" + pedidoId, cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Aluguel");
    }

    @Test
    void buscarPedido_inexistente_retorna404() {
        ResponseEntity<String> resp = getAutenticado("/api/v1/pedidos/99999", cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void buscarPedidoDeOutroRequisitante_retorna403() {
        jdbcTemplate.update(
                "INSERT INTO requisitante (nome, telefone, ativo, criado_em) VALUES ('Outro', '+5500000000000', true, NOW())");
        Long outroReqId = jdbcTemplate.queryForObject(
                "SELECT id FROM requisitante WHERE nome = 'Outro' ORDER BY id DESC LIMIT 1", Long.class);

        jdbcTemplate.update("""
                INSERT INTO pedidos_pagamento (requisitante_id, valor, descricao, status, tipo, data_pedido, data_criacao)
                VALUES (?, 100.00, 'Pedido do outro', 'PENDENTE', 'PIX', CURDATE(), NOW())
                """, outroReqId);

        Long pedidoOutro = jdbcTemplate.queryForObject(
                "SELECT id FROM pedidos_pagamento WHERE descricao = 'Pedido do outro' ORDER BY id DESC LIMIT 1",
                Long.class);

        ResponseEntity<String> resp = getAutenticado("/api/v1/pedidos/" + pedidoOutro, cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        jdbcTemplate.update("DELETE FROM pedidos_pagamento WHERE id = ?", pedidoOutro);
        jdbcTemplate.update("DELETE FROM requisitante WHERE id = ?", outroReqId);
    }
}
