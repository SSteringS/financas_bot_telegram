package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.ComprovanteMapper;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComprovanteRepositoryAdapterTest {

    @Mock private ComprovanteJpaRepository jpaRepository;
    @Mock private PedidoPagamentoJpaRepository pedidoJpaRepository;
    @Mock private ComprovanteMapper mapper;
    @InjectMocks private ComprovanteRepositoryAdapter adapter;

    @Test
    void deveSalvarComprovanteBuscandoPedidoEntityParaFK() {
        Comprovante domain = Comprovante.builder().pedidoId(1L).tipoPagamento("PIX").build();
        PedidoPagamentoEntity pedidoEntity = new PedidoPagamentoEntity();
        pedidoEntity.setId(1L);
        ComprovanteEntity entity = new ComprovanteEntity();
        ComprovanteEntity saved = new ComprovanteEntity();
        saved.setId(10L);
        Comprovante resultado = Comprovante.builder().id(10L).pedidoId(1L).build();

        when(pedidoJpaRepository.findById(1L)).thenReturn(Optional.of(pedidoEntity));
        when(mapper.toEntity(eq(domain), eq(pedidoEntity))).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(resultado);

        Comprovante retornado = adapter.save(domain);

        assertThat(retornado.getId()).isEqualTo(10L);
        verify(pedidoJpaRepository).findById(1L);
        verify(mapper).toEntity(eq(domain), eq(pedidoEntity));
        verify(jpaRepository).save(entity);
    }

    @Test
    void deveLancarExcecaoQuandoPedidoNaoExisteAoSalvarComprovante() {
        Comprovante domain = Comprovante.builder().pedidoId(99L).build();
        when(pedidoJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(PedidoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void devePassarPedidoEntityCorretaParaMapper() {
        Comprovante domain = Comprovante.builder().pedidoId(5L).build();
        PedidoPagamentoEntity pedidoEntity = new PedidoPagamentoEntity();
        pedidoEntity.setId(5L);
        ComprovanteEntity entity = new ComprovanteEntity();
        ComprovanteEntity saved = new ComprovanteEntity();
        Comprovante resultado = Comprovante.builder().pedidoId(5L).build();

        when(pedidoJpaRepository.findById(5L)).thenReturn(Optional.of(pedidoEntity));
        when(mapper.toEntity(any(), any())).thenReturn(entity);
        when(jpaRepository.save(any())).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(resultado);

        adapter.save(domain);

        verify(mapper).toEntity(eq(domain), eq(pedidoEntity));
    }
}
