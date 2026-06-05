package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.FuncionarioEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.FuncionarioMapper;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes unitários de {@link FuncionarioRepositoryAdapter}.
 *
 * <p>Verifica que o adapter delega ao {@link FuncionarioJpaRepository} e usa
 * {@link FuncionarioMapper} em todos os métodos públicos, incluindo soft delete.
 */
@ExtendWith(MockitoExtension.class)
class FuncionarioRepositoryAdapterTest {

    @Mock private FuncionarioJpaRepository jpaRepository;
    @Mock private FuncionarioMapper mapper;
    @InjectMocks private FuncionarioRepositoryAdapter adapter;

    @Test
    void deveSalvarFuncionarioPassandoPeloMapper() {
        Funcionario domain = Funcionario.builder()
                .nome("Maria")
                .salarioBase(new BigDecimal("1800.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("maria@pix.com")
                .build();

        FuncionarioEntity entity = new FuncionarioEntity();
        FuncionarioEntity saved = new FuncionarioEntity();
        saved.setId(1L);
        Funcionario resultado = Funcionario.builder().id(1L).nome("Maria").build();

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(resultado);

        Funcionario retornado = adapter.save(domain);

        assertThat(retornado.getId()).isEqualTo(1L);
        verify(mapper).toEntity(domain);
        verify(jpaRepository).save(entity);
        verify(mapper).toDomain(saved);
    }

    @Test
    void deveBuscarPorIdExistente() {
        FuncionarioEntity entity = new FuncionarioEntity();
        entity.setId(3L);
        Funcionario domain = Funcionario.builder().id(3L).nome("João").build();

        when(jpaRepository.findById(3L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Funcionario> resultado = adapter.findById(3L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("João");
    }

    @Test
    void deveRetornarVazioQuandoFindByIdNaoEncontra() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Funcionario> resultado = adapter.findById(99L);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveBuscarFuncionarioAtivoById() {
        FuncionarioEntity entity = new FuncionarioEntity();
        entity.setId(4L);
        entity.setAtivo(true);
        Funcionario domain = Funcionario.builder().id(4L).ativo(true).build();

        when(jpaRepository.findByIdAndAtivoTrue(4L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Funcionario> resultado = adapter.findAtivoById(4L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(4L);
    }

    @Test
    void deveRetornarVazioParaFuncionarioInativoEmFindAtivoById() {
        when(jpaRepository.findByIdAndAtivoTrue(5L)).thenReturn(Optional.empty());

        Optional<Funcionario> resultado = adapter.findAtivoById(5L);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveListarTodosFuncionariosAtivos() {
        FuncionarioEntity e1 = new FuncionarioEntity();
        e1.setId(1L);
        FuncionarioEntity e2 = new FuncionarioEntity();
        e2.setId(2L);

        Funcionario d1 = Funcionario.builder().id(1L).ativo(true).build();
        Funcionario d2 = Funcionario.builder().id(2L).ativo(true).build();

        when(jpaRepository.findAllByAtivoTrue()).thenReturn(List.of(e1, e2));
        when(mapper.toDomain(e1)).thenReturn(d1);
        when(mapper.toDomain(e2)).thenReturn(d2);

        List<Funcionario> resultado = adapter.findAllAtivos();

        assertThat(resultado).hasSize(2);
    }

    @Test
    void deveSoftDeleteSetandoAtivoFalse() {
        FuncionarioEntity entity = new FuncionarioEntity();
        entity.setId(6L);
        entity.setAtivo(true);

        when(jpaRepository.findById(6L)).thenReturn(Optional.of(entity));

        adapter.deleteById(6L);

        // Verifica que setAtivo(false) foi chamado e depois save foi invocado
        verify(jpaRepository).save(argThat(e -> Boolean.FALSE.equals(e.getAtivo())));
    }

    @Test
    void devePularDeleteSeEntidadeNaoExiste() {
        when(jpaRepository.findById(999L)).thenReturn(Optional.empty());

        adapter.deleteById(999L);

        // findById foi chamado, mas save não
        verify(jpaRepository).findById(999L);
        verify(jpaRepository, never()).save(argThat(e -> e != null));
    }
}
