package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoMesDTO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

        jdbcTemplate.update("""
                INSERT INTO pedidos_pagamento (requisitante_id, valor, descricao, status, tipo, data_pedido, data_criacao)
                VALUES (1, 100.00, 'P1', 'PENDENTE', 'PIX', CURDATE(), NOW()),
                       (1, 200.00, 'P2', 'PENDENTE', 'BOLETO', CURDATE(), NOW()),
                       (1, 500.00, 'P3', 'PAGO', 'TED', CURDATE(), NOW()),
                       (1, 999.00, 'P4', 'CANCELADO', 'OUTRO', CURDATE(), NOW())
                """);

        cookie = autenticarComo(1L);
    }

    @Test
    void resumo_retorna200ComAgregadoCorreto() {
        ResponseEntity<ResumoMesDTO> resp = getAutenticado("/api/v1/resumo", cookie, ResumoMesDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResumoMesDTO body = resp.getBody();
        assertThat(body).isNotNull();

        String mesEsperado = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        assertThat(body.mesAtual()).isEqualTo(mesEsperado);

        assertThat(body.pendentes().quantidade()).isEqualTo(2);
        assertThat(body.pendentes().total().doubleValue()).isEqualTo(300.00);
        assertThat(body.pagos().quantidade()).isEqualTo(1);
        assertThat(body.pagos().total().doubleValue()).isEqualTo(500.00);
    }

    @Test
    void resumo_semPedidosNoMes_retornaZeros() {
        jdbcTemplate.update("DELETE FROM comprovantes WHERE pedido_id IN (SELECT id FROM pedidos_pagamento WHERE requisitante_id = 1)");
        jdbcTemplate.update("DELETE FROM pedidos_pagamento WHERE requisitante_id = 1");

        ResponseEntity<ResumoMesDTO> resp = getAutenticado("/api/v1/resumo", cookie, ResumoMesDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResumoMesDTO body = resp.getBody();
        assertThat(body.pendentes().quantidade()).isZero();
        assertThat(body.pagos().quantidade()).isZero();
    }

    @Test
    void resumo_semCookie_retorna401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/v1/resumo", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
