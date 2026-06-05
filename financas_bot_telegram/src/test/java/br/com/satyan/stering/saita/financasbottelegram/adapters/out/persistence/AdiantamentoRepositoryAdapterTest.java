package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.AdiantamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.AdiantamentoMapper;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes unitários de {@link AdiantamentoRepositoryAdapter}.
 *
 * <p>Verifica que o adapter delega corretamente ao {@link AdiantamentoJpaRepository}
 * e passa os resultados pelo {@link AdiantamentoMapper}, mantendo domínio livre de JPA.
 */
@ExtendWith(MockitoExtension.class)
class AdiantamentoRepositoryAdapterTest {

    @Mock private AdiantamentoJpaRepository jpaRepository;
    @Mock private AdiantamentoMapper mapper;
    @InjectMocks private AdiantamentoRepositoryAdapter adapter;

    @Test
    void deveSalvarAdiantamentoPassandoPeloMapper() {
        Adiantamento domain = Adiantamento.builder()
                .funcionarioId(1L)
                .descricao("Adiantamento teste")
                .valorTotal(new BigDecimal("300.00"))
                .valorParcela(new BigDecimal("100.00"))
                .numParcelas(3)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .build();

        AdiantamentoEntity entity = new AdiantamentoEntity();
        AdiantamentoEntity saved = new AdiantamentoEntity();
        saved.setId(1L);
        Adiantamento resultado = Adiantamento.builder().id(1L).build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(resultado);

        Adiantamento retornado = adapter.save(domain);

        assertThat(retornado.getId()).isEqualTo(1L);
        verify(mapper).toEntity(domain);
        verify(jpaRepository).save(entity);
        verify(mapper).toDomain(saved);
    }

    @Test
    void deveBuscarPorIdExistente() {
        AdiantamentoEntity entity = new AdiantamentoEntity();
        entity.setId(5L);
        Adiantamento domain = Adiantamento.builder().id(5L).build();

        when(jpaRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Adiantamento> resultado = adapter.findById(5L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(5L);
    }

    @Test
    void deveRetornarVazioQuandoIdNaoExiste() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Adiantamento> resultado = adapter.findById(99L);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveBuscarAtivosParaFuncionario() {
        Long funcionarioId = 2L;

        AdiantamentoEntity e1 = new AdiantamentoEntity();
        e1.setId(10L);
        AdiantamentoEntity e2 = new AdiantamentoEntity();
        e2.setId(11L);

        Adiantamento d1 = Adiantamento.builder().id(10L).build();
        Adiantamento d2 = Adiantamento.builder().id(11L).build();

        when(jpaRepository.findByFuncionarioIdAndAtivoTrue(funcionarioId))
                .thenReturn(List.of(e1, e2));
        when(mapper.toDomain(e1)).thenReturn(d1);
        when(mapper.toDomain(e2)).thenReturn(d2);

        List<Adiantamento> resultado = adapter.findAtivosParaFuncionario(funcionarioId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(Adiantamento::getId).containsExactly(10L, 11L);
    }

    @Test
    void deveBuscarAtivosParaFechamento() {
        // findAtivosParaFechamento usa o mesmo query que findAtivosParaFuncionario
        Long funcionarioId = 3L;

        AdiantamentoEntity entity = new AdiantamentoEntity();
        entity.setId(20L);
        Adiantamento domain = Adiantamento.builder().id(20L).ativo(true).build();

        when(jpaRepository.findByFuncionarioIdAndAtivoTrue(funcionarioId))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Adiantamento> resultado = adapter.findAtivosParaFechamento(funcionarioId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(20L);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaAtivos() {
        when(jpaRepository.findByFuncionarioIdAndAtivoTrue(999L)).thenReturn(List.of());

        List<Adiantamento> resultado = adapter.findAtivosParaFuncionario(999L);

        assertThat(resultado).isEmpty();
    }
}
