package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaginaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PedidosListIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ObjectMapper objectMapper;

    private String cookie;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM comprovantes WHERE pedido_id IN (SELECT id FROM pedidos_pagamento WHERE requisitante_id = 1)");
        jdbcTemplate.update("DELETE FROM pedidos_pagamento WHERE requisitante_id = 1");

        jdbcTemplate.update("""
                INSERT INTO pedidos_pagamento (requisitante_id, valor, descricao, status, tipo, data_pedido, data_criacao)
                VALUES (1, 150.00, 'Conta de energia', 'PENDENTE', 'BOLETO', CURDATE(), NOW()),
                       (1, 200.00, 'Internet', 'PAGO', 'PIX', CURDATE(), NOW()),
                       (1, 50.00, 'Agua', 'CANCELADO', 'TED', CURDATE(), NOW())
                """);

        cookie = autenticarComo(1L);
    }

    @Test
    void listarSemFiltros_retornaTodosOsPedidosDoRequisitante() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/pedidos", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listarComFiltroStatus_retornaApenasStatusSolicitado() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/pedidos?status=PENDENTE", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("PENDENTE").doesNotContain("PAGO");
    }

    @Test
    void listarComBusca_retornaApenasComDescricaoCorrespondente() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/pedidos?busca=energia", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("energia");
    }

    @Test
    void listarSemCookie_retorna401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/v1/pedidos", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
