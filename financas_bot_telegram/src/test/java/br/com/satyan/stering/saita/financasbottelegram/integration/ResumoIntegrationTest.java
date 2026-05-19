package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoMesDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ResumoIntegrationTest extends AbstractIntegrationTest {

    private String cookie;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM comprovantes WHERE pedido_id IN (SELECT id FROM pedidos_pagamento WHERE requisitante_id = 1)");
        jdbcTemplate.update("DELETE FROM pedidos_pagamento WHERE requisitante_id = 1");

        // 2 pendentes maio, 1 pago maio, 1 pago abril, 1 cancelado maio
        jdbcTemplate.update("""
                INSERT INTO pedidos_pagamento (requisitante_id, valor, descricao, status, tipo, data_pedido, data_criacao)
                VALUES (1, 100.00, 'boleto Energia',  'PENDENTE',  'BOLETO', '2026-05-10', NOW()),
                       (1, 200.00, 'pix Maria',       'PENDENTE',  'PIX',    '2026-05-15', NOW()),
                       (1, 500.00, 'ted Construtora', 'PAGO',      'TED',    '2026-05-20', NOW()),
                       (1, 999.00, 'boleto Agua',     'PAGO',      'BOLETO', '2026-04-05', NOW()),
                       (1, 150.00, 'cancelado',       'CANCELADO', 'OUTRO',  '2026-05-01', NOW())
                """);

        cookie = autenticarComo(1L);
    }

    @Test
    void resumo_comMesEspecifico_retornaAgregadoCorreto() {
        ResponseEntity<ResumoMesDTO> resp = getAutenticado("/api/v1/resumo?mes=2026-05", cookie, ResumoMesDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResumoMesDTO body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.mes()).isEqualTo("2026-05");
        assertThat(body.pendentes().quantidade()).isEqualTo(2);
        assertThat(body.pendentes().total().doubleValue()).isEqualTo(300.00);
        assertThat(body.pagos().quantidade()).isEqualTo(1);
        assertThat(body.pagos().total().doubleValue()).isEqualTo(500.00);
        assertThat(body.todos().quantidade()).isEqualTo(3);
        assertThat(body.todos().total().doubleValue()).isEqualTo(800.00);
    }

    @Test
    void resumo_comBusca_filtraPorDescricao() {
        ResponseEntity<ResumoMesDTO> resp = getAutenticado("/api/v1/resumo?mes=2026-05&busca=boleto", cookie, ResumoMesDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResumoMesDTO body = resp.getBody();
        assertThat(body.pendentes().quantidade()).isEqualTo(1);
        assertThat(body.pendentes().total().doubleValue()).isEqualTo(100.00);
        assertThat(body.pagos().quantidade()).isZero();
        assertThat(body.todos().quantidade()).isEqualTo(1);
    }

    @Test
    void resumo_semPedidosNoMes_retornaZeros() {
        ResponseEntity<ResumoMesDTO> resp = getAutenticado("/api/v1/resumo?mes=2020-01", cookie, ResumoMesDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResumoMesDTO body = resp.getBody();
        assertThat(body.pendentes().quantidade()).isZero();
        assertThat(body.pagos().quantidade()).isZero();
        assertThat(body.todos().quantidade()).isZero();
    }

    @Test
    void resumo_mesInvalido_retorna400() {
        ResponseEntity<String> resp = getAutenticado("/api/v1/resumo?mes=2026-13", cookie, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("MES_INVALIDO");
    }

    @Test
    void resumo_semCookie_retorna401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/v1/resumo", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
