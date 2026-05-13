package br.com.satyan.stering.saita.financasbottelegram.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import org.junit.jupiter.api.Test;

class LegendaParserTest {

    @Test
    void detectaBoleto() {
        assertThat(LegendaParser.parseTipo("150.00 Almoço boleto")).isEqualTo(TipoPagamento.BOLETO);
    }

    @Test
    void detectaPix() {
        assertThat(LegendaParser.parseTipo("200 pix maria")).isEqualTo(TipoPagamento.PIX);
    }

    @Test
    void detectaTedCaseInsensitive() {
        assertThat(LegendaParser.parseTipo("1500 TED construtora silva")).isEqualTo(TipoPagamento.TED);
    }

    @Test
    void detectaAgendamento() {
        assertThat(LegendaParser.parseTipo("300 agendamento luz")).isEqualTo(TipoPagamento.AGENDAMENTO);
    }

    @Test
    void retornaOutroSemPalavraChave() {
        assertThat(LegendaParser.parseTipo("100 Almoço")).isEqualTo(TipoPagamento.OUTRO);
    }

    @Test
    void primeiraOcorrenciaVenceQuandoHaDuas() {
        assertThat(LegendaParser.parseTipo("100 BOLETO pix")).isEqualTo(TipoPagamento.BOLETO);
    }

    @Test
    void retornaOutroParaStringVazia() {
        assertThat(LegendaParser.parseTipo("")).isEqualTo(TipoPagamento.OUTRO);
    }

    @Test
    void retornaOutroParaNull() {
        assertThat(LegendaParser.parseTipo(null)).isEqualTo(TipoPagamento.OUTRO);
    }

    @Test
    void detectaPixMaiusculo() {
        assertThat(LegendaParser.parseTipo("500 PIX aluguel")).isEqualTo(TipoPagamento.PIX);
    }
}
