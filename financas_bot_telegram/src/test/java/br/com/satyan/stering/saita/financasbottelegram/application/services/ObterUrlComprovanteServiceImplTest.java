package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.ComprovanteJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoPagamentoJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.StorageService;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.ComprovanteNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoAutorizadoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObterUrlComprovanteServiceImplTest {

    @Mock private PedidoPagamentoJpaRepository pedidoRepo;
    @Mock private ComprovanteJpaRepository comprovanteRepo;
    @Mock private StorageService storage;

    private ObterUrlComprovanteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ObterUrlComprovanteServiceImpl(pedidoRepo, comprovanteRepo, storage);
    }

    private PedidoPagamentoEntity pedido(Long id, Long reqId) {
        PedidoPagamentoEntity e = new PedidoPagamentoEntity();
        e.setId(id);
        e.setRequisitanteId(reqId);
        e.setValor(new BigDecimal("100.00"));
        e.setStatus(StatusPedido.PAGO);
        e.setDataPedido(LocalDate.of(2026, 5, 1));
        return e;
    }

    private ComprovanteEntity comprovante(String imagemUrl) {
        ComprovanteEntity c = new ComprovanteEntity();
        c.setId(10L);
        c.setImagemUrl(imagemUrl);
        return c;
    }

    @Test
    void deveRetornarPresignedUrlDoComprovante() {
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido(1L, 42L)));
        when(comprovanteRepo.findFirstByPedidoIdOrderByIdDesc(1L))
                .thenReturn(Optional.of(comprovante("pedidos/20260501/comprovante.jpg")));
        when(storage.gerarUrlTemporariaParaLeitura(eq("pedidos/20260501/comprovante.jpg"), any()))
                .thenReturn("https://s3.../presigned-comprovante");

        String url = service.obter(1L, 42L);

        assertThat(url).isEqualTo("https://s3.../presigned-comprovante");
    }

    @Test
    void deveLancarPedidoNaoEncontradoQuandoIdNaoExiste() {
        when(pedidoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obter(99L, 42L))
                .isInstanceOf(PedidoNaoEncontradoException.class);
    }

    @Test
    void deveLancarPedidoNaoAutorizadoQuandoPertenceAOutroRequisitante() {
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido(1L, 99L)));

        assertThatThrownBy(() -> service.obter(1L, 42L))
                .isInstanceOf(PedidoNaoAutorizadoException.class);
    }

    @Test
    void deveLancarComprovanteNaoEncontradoQuandoNaoExiste() {
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido(1L, 42L)));
        when(comprovanteRepo.findFirstByPedidoIdOrderByIdDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obter(1L, 42L))
                .isInstanceOf(ComprovanteNaoEncontradoException.class);
    }
}
