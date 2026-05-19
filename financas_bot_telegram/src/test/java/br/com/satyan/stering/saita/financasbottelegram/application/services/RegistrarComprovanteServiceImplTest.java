package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.BusinessRuleException;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.ComprovanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrarComprovanteServiceImplTest {

    @Mock private ComprovanteRepositoryPort comprovanteRepository;
    @Mock private PedidoPagamentoRepositoryPort pedidoPagamentoRepository;
    @InjectMocks private RegistrarComprovanteServiceImpl service;

    private PedidoPagamento pedidoPendente(Long id) {
        return PedidoPagamento.builder()
                .id(id)
                .valor(new BigDecimal("200.00"))
                .descricao("Jantar")
                .status(StatusPedido.PENDENTE)
                .telegramUserId("user1")
                .build();
    }

    @Test
    void deveRegistrarComprovanteEAtualizarStatusDoPedido() {
        PedidoPagamento pedido = pedidoPendente(1L);
        when(pedidoPagamentoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        Comprovante comprovanteEsperado = Comprovante.builder().id(1L).pedidoId(1L).tipoPagamento("PIX").build();
        when(comprovanteRepository.save(any())).thenReturn(comprovanteEsperado);
        when(pedidoPagamentoRepository.save(any())).thenReturn(pedido);

        Comprovante resultado = service.execute(1L, "PIX", "file_123", "https://s3.example.com/img.jpg", TipoArquivo.IMAGEM, 999L);

        assertThat(resultado.getPedidoId()).isEqualTo(1L);
        assertThat(resultado.getTipoPagamento()).isEqualTo("PIX");

        ArgumentCaptor<PedidoPagamento> captor = ArgumentCaptor.forClass(PedidoPagamento.class);
        verify(pedidoPagamentoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusPedido.PAGO);
    }

    @Test
    void deveLancarExcecaoQuandoPedidoNaoEncontrado() {
        when(pedidoPagamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L, "PIX", "file", "url", TipoArquivo.IMAGEM, 999L))
                .isInstanceOf(PedidoNaoEncontradoException.class)
                .hasMessageContaining("99");
        verify(comprovanteRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoPedidoJaEstaPago() {
        PedidoPagamento pedidoPago = pedidoPendente(2L);
        pedidoPago.setStatus(StatusPedido.PAGO);
        when(pedidoPagamentoRepository.findById(2L)).thenReturn(Optional.of(pedidoPago));

        assertThatThrownBy(() -> service.execute(2L, "PIX", "file", "url", TipoArquivo.IMAGEM, 999L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pago");
        verify(comprovanteRepository, never()).save(any());
    }

    @Test
    void deveSalvarComprovanteComFileIdEImagemUrl() {
        PedidoPagamento pedido = pedidoPendente(3L);
        when(pedidoPagamentoRepository.findById(3L)).thenReturn(Optional.of(pedido));
        when(pedidoPagamentoRepository.save(any())).thenReturn(pedido);
        Comprovante salvo = Comprovante.builder().id(5L).pedidoId(3L).fileIdTelegram("fid").imagemUrl("http://url").build();
        when(comprovanteRepository.save(any())).thenReturn(salvo);

        service.execute(3L, "TED", "fid", "http://url", TipoArquivo.IMAGEM, 999L);

        ArgumentCaptor<Comprovante> captor = ArgumentCaptor.forClass(Comprovante.class);
        verify(comprovanteRepository).save(captor.capture());
        assertThat(captor.getValue().getFileIdTelegram()).isEqualTo("fid");
        assertThat(captor.getValue().getImagemUrl()).isEqualTo("http://url");
        assertThat(captor.getValue().getPedidoId()).isEqualTo(3L);
    }
}
