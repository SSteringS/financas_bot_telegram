package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.PedidoPagamentoMapper;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PedidoPagamentoRepositoryAdapterTest {

    @Mock private PedidoPagamentoJpaRepository jpaRepository;
    @Mock private PedidoPagamentoMapper mapper;
    @InjectMocks private PedidoPagamentoRepositoryAdapter adapter;

    @Test
    void deveSalvarPedidoPassandoPeloMapper() {
        PedidoPagamento domain = PedidoPagamento.builder().valor(new BigDecimal("100.00")).build();
        PedidoPagamentoEntity entity = new PedidoPagamentoEntity();
        PedidoPagamentoEntity saved = new PedidoPagamentoEntity();
        saved.setId(1L);
        PedidoPagamento resultado = PedidoPagamento.builder().id(1L).build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(resultado);

        PedidoPagamento retornado = adapter.save(domain);

        assertThat(retornado.getId()).isEqualTo(1L);
        verify(mapper).toEntity(domain);
        verify(jpaRepository).save(entity);
        verify(mapper).toDomain(saved);
    }

    @Test
    void deveBuscarPedidoPorIdExistente() {
        PedidoPagamentoEntity entity = new PedidoPagamentoEntity();
        entity.setId(5L);
        PedidoPagamento domain = PedidoPagamento.builder().id(5L).build();

        when(jpaRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<PedidoPagamento> resultado = adapter.findById(5L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(5L);
    }

    @Test
    void deveRetornarVazioQuandoPedidoNaoExiste() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<PedidoPagamento> resultado = adapter.findById(99L);

        assertThat(resultado).isEmpty();
    }
}
