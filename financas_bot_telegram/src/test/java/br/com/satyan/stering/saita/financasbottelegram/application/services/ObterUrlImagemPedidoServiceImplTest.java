package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoPagamentoJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.StorageService;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.ImagemNaoEncontradaException;
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
class ObterUrlImagemPedidoServiceImplTest {

    @Mock private PedidoPagamentoJpaRepository pedidoRepo;
    @Mock private StorageService storage;

    private ObterUrlImagemPedidoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ObterUrlImagemPedidoServiceImpl(pedidoRepo, storage);
    }

    private PedidoPagamentoEntity entity(Long id, Long reqId, String imagemUrl) {
        PedidoPagamentoEntity e = new PedidoPagamentoEntity();
        e.setId(id);
        e.setRequisitanteId(reqId);
        e.setValor(new BigDecimal("100.00"));
        e.setStatus(StatusPedido.PENDENTE);
        e.setDataPedido(LocalDate.of(2026, 5, 1));
        e.setImagemUrl(imagemUrl);
        return e;
    }

    @Test
    void deveRetornarPresignedUrlQuandoPedidoTemImagem() {
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(entity(1L, 42L, "pedidos/20260501/abc.jpg")));
        when(storage.gerarUrlTemporariaParaLeitura(eq("pedidos/20260501/abc.jpg"), any())).thenReturn("https://s3.../presigned");

        String url = service.obter(1L, 42L);

        assertThat(url).isEqualTo("https://s3.../presigned");
    }

    @Test
    void deveLancarPedidoNaoEncontradoQuandoIdNaoExiste() {
        when(pedidoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obter(99L, 42L))
                .isInstanceOf(PedidoNaoEncontradoException.class);
    }

    @Test
    void deveLancarPedidoNaoAutorizadoQuandoPedidoPertenceAOutroRequisitante() {
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(entity(1L, 99L, "pedidos/abc.jpg")));

        assertThatThrownBy(() -> service.obter(1L, 42L))
                .isInstanceOf(PedidoNaoAutorizadoException.class);
    }

    @Test
    void deveLancarImagemNaoEncontradaQuandoImagemUrlEhNula() {
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(entity(1L, 42L, null)));

        assertThatThrownBy(() -> service.obter(1L, 42L))
                .isInstanceOf(ImagemNaoEncontradaException.class);
    }

    @Test
    void deveLancarImagemNaoEncontradaQuandoImagemUrlEhVazia() {
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(entity(1L, 42L, "")));

        assertThatThrownBy(() -> service.obter(1L, 42L))
                .isInstanceOf(ImagemNaoEncontradaException.class);
    }
}
