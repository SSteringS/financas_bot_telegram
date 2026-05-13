package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.ComprovanteJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoPagamentoJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.ListarPedidosFiltro;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaginaDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoResumoDTO;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ListarPedidosServiceImplTest {

    @Mock private PedidoPagamentoJpaRepository jpaRepository;
    @Mock private ComprovanteJpaRepository comprovanteRepo;

    private ListarPedidosServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ListarPedidosServiceImpl(jpaRepository, comprovanteRepo);
    }

    private PedidoPagamentoEntity pedidoEntity(Long id) {
        PedidoPagamentoEntity e = new PedidoPagamentoEntity();
        e.setId(id);
        e.setValor(new BigDecimal("100.00"));
        e.setDescricao("Boleto energia");
        e.setStatus(StatusPedido.PENDENTE);
        e.setDataPedido(LocalDate.of(2026, 5, 1));
        return e;
    }

    @Test
    void deveRetornarPaginaComItens() {
        PedidoPagamentoEntity e1 = pedidoEntity(1L);
        PedidoPagamentoEntity e2 = pedidoEntity(2L);

        when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e1, e2)));
        when(comprovanteRepo.findPedidoIdsByPedidoIdIn(List.of(1L, 2L)))
                .thenReturn(Set.of(1L));

        ListarPedidosFiltro filtro = new ListarPedidosFiltro(null, null, null, null, null, 0, 20);
        PaginaDTO<PedidoResumoDTO> resultado = service.listar(filtro, 1L);

        assertThat(resultado.items()).hasSize(2);
        assertThat(resultado.items().get(0).temComprovante()).isTrue();
        assertThat(resultado.items().get(1).temComprovante()).isFalse();
        assertThat(resultado.total()).isEqualTo(2);
    }

    @Test
    void deveLimitarTamanhoAoMaximo() {
        when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ListarPedidosFiltro filtro = new ListarPedidosFiltro(null, null, null, null, null, 0, 200);
        PaginaDTO<PedidoResumoDTO> resultado = service.listar(filtro, 1L);

        assertThat(resultado.tamanho()).isEqualTo(ListarPedidosFiltro.TAMANHO_MAX);
    }

    @Test
    void deveUsarTamanhoDefaultQuandoZeroOuNegativo() {
        when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ListarPedidosFiltro filtro = new ListarPedidosFiltro(null, null, null, null, null, 0, 0);
        PaginaDTO<PedidoResumoDTO> resultado = service.listar(filtro, 1L);

        assertThat(resultado.tamanho()).isEqualTo(ListarPedidosFiltro.TAMANHO_DEFAULT);
    }

    @Test
    void deveRetornarPaginaVaziaQuandoNaoHaPedidos() {
        when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ListarPedidosFiltro filtro = new ListarPedidosFiltro(null, null, null, null, null, 0, 20);
        PaginaDTO<PedidoResumoDTO> resultado = service.listar(filtro, 1L);

        assertThat(resultado.items()).isEmpty();
        assertThat(resultado.total()).isZero();
    }
}
