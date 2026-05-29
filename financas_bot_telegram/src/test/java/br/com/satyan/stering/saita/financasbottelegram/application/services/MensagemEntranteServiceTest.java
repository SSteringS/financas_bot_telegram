package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.strategy.MensagemProcessingStrategy;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MensagemEntranteServiceTest {

    @Mock private MensagemProcessingStrategy strategyA;
    @Mock private MensagemProcessingStrategy strategyB;
    @Mock private MensagemProcessadaService mensagemProcessadaService;

    private PaymentMessageDTO dto(Long chatId) {
        return PaymentMessageDTO.builder()
            .canal(Canal.TELEGRAM)
            .externalId("msg-" + chatId)
            .chatId(chatId)
            .caption("qualquer")
            .build();
    }

    @Test
    void deveDespacharParaAPrimeiraStrategyQueSuportar() {
        PaymentMessageDTO dto = dto(100L);
        when(mensagemProcessadaService.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(true);
        when(strategyA.supports(dto)).thenReturn(false);
        when(strategyB.supports(dto)).thenReturn(true);
        MensagemEntranteService service = new MensagemEntranteService(List.of(strategyA, strategyB), mensagemProcessadaService);

        service.processar(dto);

        verify(strategyB).process(dto);
        verify(strategyA, never()).process(dto);
    }

    @Test
    void deveDespacharParaPrimeiraStrategyQuandoAmbasSuportam() {
        PaymentMessageDTO dto = dto(100L);
        when(mensagemProcessadaService.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(true);
        when(strategyA.supports(dto)).thenReturn(true);
        MensagemEntranteService service = new MensagemEntranteService(List.of(strategyA, strategyB), mensagemProcessadaService);

        service.processar(dto);

        verify(strategyA).process(dto);
        verify(strategyB, never()).supports(dto);
    }

    @Test
    void deveLancarInvalidMessageFormatExceptionQuandoNenhumaStrategySuportar() {
        PaymentMessageDTO dto = dto(200L);
        when(mensagemProcessadaService.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(true);
        when(strategyA.supports(dto)).thenReturn(false);
        when(strategyB.supports(dto)).thenReturn(false);
        MensagemEntranteService service = new MensagemEntranteService(List.of(strategyA, strategyB), mensagemProcessadaService);

        assertThatThrownBy(() -> service.processar(dto))
                .isInstanceOf(InvalidMessageFormatException.class)
                .satisfies(ex -> {
                    InvalidMessageFormatException ime = (InvalidMessageFormatException) ex;
                    org.assertj.core.api.Assertions.assertThat(ime.getChatId()).isEqualTo(200L);
                });
    }

    @Test
    void deveLancarInvalidMessageFormatExceptionComListaVazia() {
        PaymentMessageDTO dto = dto(300L);
        when(mensagemProcessadaService.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(true);
        MensagemEntranteService service = new MensagemEntranteService(List.of(), mensagemProcessadaService);

        assertThatThrownBy(() -> service.processar(dto))
                .isInstanceOf(InvalidMessageFormatException.class);
    }

    @Test
    void deveSaltarProcessamentoQuandoClaimFalhar() {
        PaymentMessageDTO dto = dto(400L);
        when(mensagemProcessadaService.tentarClaim(Canal.TELEGRAM, dto.getExternalId())).thenReturn(false);
        MensagemEntranteService service = new MensagemEntranteService(List.of(strategyA, strategyB), mensagemProcessadaService);

        service.processar(dto);

        verify(strategyA, never()).supports(dto);
        verify(strategyB, never()).supports(dto);
    }
}
