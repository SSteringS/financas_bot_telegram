package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.ComprovanteJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoPagamentoJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoDetalheDTO;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
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
class BuscarPedidoServiceImplTest {

    @Mock private PedidoPagamentoJpaRepository jpaRepository;
    @Mock private ComprovanteJpaRepository comprovanteRepo;

    private BuscarPedidoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BuscarPedidoServiceImpl(jpaRepository, comprovanteRepo);
    }

    private PedidoPagamentoEntity entity(Long id, Long requisitanteId) {
        PedidoPagamentoEntity e = new PedidoPagamentoEntity();
        e.setId(id);
        e.setRequisitanteId(requisitanteId);
        e.setValor(new BigDecimal("150.00"));
        e.setDescricao("Boleto água");
        e.setStatus(StatusPedido.PENDENTE);
        e.setDataPedido(LocalDate.of(2026, 5, 10));
        return e;
    }

    @Test
    void deveRetornarDetalhesQuandoPedidoExisteEPertenceAoRequisitante() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity(1L, 42L)));
        when(comprovanteRepo.existsByPedidoId(1L)).thenReturn(true);

        PedidoDetalheDTO dto = service.buscar(1L, 42L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.valor()).isEqualByComparingTo("150.00");
        assertThat(dto.temComprovante()).isTrue();
    }

    @Test
    void deveLancarPedidoNaoEncontradoQuandoIdNaoExiste() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L, 42L))
                .isInstanceOf(PedidoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deveLancarPedidoNaoAutorizadoQuandoPedidoPertenceAOutroRequisitante() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity(1L, 99L)));

        assertThatThrownBy(() -> service.buscar(1L, 42L))
                .isInstanceOf(PedidoNaoAutorizadoException.class);
    }

    @Test
    void deveRetornarSemComprovanteQuandoNaoExiste() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity(1L, 42L)));
        when(comprovanteRepo.existsByPedidoId(1L)).thenReturn(false);

        PedidoDetalheDTO dto = service.buscar(1L, 42L);

        assertThat(dto.temComprovante()).isFalse();
    }
}
