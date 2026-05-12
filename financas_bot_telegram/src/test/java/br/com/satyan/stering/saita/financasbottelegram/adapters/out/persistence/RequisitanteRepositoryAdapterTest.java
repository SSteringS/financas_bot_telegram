package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.RequisitanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.RequisitanteMapper;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequisitanteRepositoryAdapterTest {

    @Mock private RequisitanteJpaRepository jpaRepository;
    @Mock private RequisitanteMapper mapper;
    @InjectMocks private RequisitanteRepositoryAdapter adapter;

    @Test
    void deveRetornarRequisitanteQuandoEncontrado() {
        RequisitanteEntity entity = new RequisitanteEntity();
        entity.setId(1L);
        Requisitante domain = Requisitante.builder().id(1L).nome("Satyan").build();

        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Requisitante> result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getNome()).isEqualTo("Satyan");
    }

    @Test
    void deveRetornarVazioQuandoNaoEncontrado() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Requisitante> result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void deveVerificarExistencia() {
        when(jpaRepository.existsById(1L)).thenReturn(true);
        when(jpaRepository.existsById(99L)).thenReturn(false);

        assertThat(adapter.existsById(1L)).isTrue();
        assertThat(adapter.existsById(99L)).isFalse();

        verify(jpaRepository).existsById(1L);
        verify(jpaRepository).existsById(99L);
    }
}
