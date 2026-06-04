package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AdiantamentoRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.AdiantamentoJaQuitadoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.AdiantamentoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelarAdiantamentoServiceImplTest {

    @Mock
    private AdiantamentoRepositoryPortOut repository;

    private CancelarAdiantamentoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CancelarAdiantamentoServiceImpl(repository);
    }

    @Test
    void deveCancelarAdiantamentoAtivo() {
        Adiantamento adiantamentoAtivo = Adiantamento.builder()
                .id(1L)
                .funcionarioId(1L)
                .descricao("Adiantamento")
                .valorTotal(new BigDecimal("300.00"))
                .valorParcela(new BigDecimal("100.00"))
                .numParcelas(3)
                .parcelasPagas(1)  // 1 de 3 — ainda não quitado
                .dataInicio(LocalDate.of(2026, 6, 1))
                .ativo(true)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(adiantamentoAtivo));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelar(1L);

        ArgumentCaptor<Adiantamento> captor = ArgumentCaptor.forClass(Adiantamento.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isFalse();
    }

    @Test
    void deveCancelarAdiantamentoSemParcelasPagas() {
        Adiantamento adiantamentoNovo = Adiantamento.builder()
                .id(2L)
                .numParcelas(2)
                .parcelasPagas(0)  // nunca descontado
                .ativo(true)
                .build();

        when(repository.findById(2L)).thenReturn(Optional.of(adiantamentoNovo));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> service.cancelar(2L)).doesNotThrowAnyException();
        verify(repository).save(any());
    }

    @Test
    void deveRejeitarCancelamentoDeAdiantamentoJaQuitado() {
        Adiantamento adiantamentoQuitado = Adiantamento.builder()
                .id(3L)
                .numParcelas(3)
                .parcelasPagas(3)  // igual a numParcelas — já quitado
                .ativo(true)
                .build();

        when(repository.findById(3L)).thenReturn(Optional.of(adiantamentoQuitado));

        assertThatThrownBy(() -> service.cancelar(3L))
                .isInstanceOf(AdiantamentoJaQuitadoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveRejeitarCancelamentoDeAdiantamentoNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(99L))
                .isInstanceOf(AdiantamentoNaoEncontradoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveCancelarAdiantamentoComParcelasPagasNulas() {
        // parcelasPagas = null — não deve lançar NullPointerException, deve cancelar
        Adiantamento adiantamentoSemParcelas = Adiantamento.builder()
                .id(4L)
                .numParcelas(3)
                .parcelasPagas(null)
                .ativo(true)
                .build();

        when(repository.findById(4L)).thenReturn(Optional.of(adiantamentoSemParcelas));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> service.cancelar(4L)).doesNotThrowAnyException();
        verify(repository).save(any());
    }
}
