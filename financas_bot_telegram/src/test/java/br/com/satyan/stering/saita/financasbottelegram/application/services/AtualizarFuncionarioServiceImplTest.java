package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.FormaPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes unitários de {@link AtualizarFuncionarioServiceImpl}.
 *
 * <p>Cobre os cenários principais do use case de atualização parcial (null = não altera),
 * verificando que a revalidação de dados bancários é acionada após o merge.
 */
@ExtendWith(MockitoExtension.class)
class AtualizarFuncionarioServiceImplTest {

    @Mock
    private FuncionarioRepositoryPortOut repository;

    private AtualizarFuncionarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AtualizarFuncionarioServiceImpl(repository);
    }

    @Test
    void deveAtualizarSalarioPix() {
        Funcionario existente = Funcionario.builder()
                .id(1L)
                .nome("Maria")
                .salarioBase(new BigDecimal("1800.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("maria@pix.com")
                .ativo(true)
                .build();

        Funcionario dadosAtualizados = Funcionario.builder()
                .salarioBase(new BigDecimal("2200.00"))
                .build(); // só atualiza salário; resto permanece

        Funcionario salvo = Funcionario.builder()
                .id(1L)
                .nome("Maria")
                .salarioBase(new BigDecimal("2200.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("maria@pix.com")
                .ativo(true)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenReturn(salvo);

        Funcionario resultado = service.atualizar(1L, dadosAtualizados);

        assertThat(resultado.getSalarioBase()).isEqualByComparingTo("2200.00");
        assertThat(resultado.getNome()).isEqualTo("Maria");
        verify(repository).save(any());
    }

    @Test
    void deveAtualizarMultiplosCamposSimultaneamente() {
        Funcionario existente = Funcionario.builder()
                .id(2L)
                .nome("João")
                .salarioBase(new BigDecimal("2000.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("joao@pix.com")
                .ativo(true)
                .build();

        Funcionario dadosAtualizados = Funcionario.builder()
                .nome("João Atualizado")
                .salarioBase(new BigDecimal("2500.00"))
                .chavePix("joao.novo@pix.com")
                .build();

        when(repository.findById(2L)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Funcionario resultado = service.atualizar(2L, dadosAtualizados);

        assertThat(resultado.getNome()).isEqualTo("João Atualizado");
        assertThat(resultado.getSalarioBase()).isEqualByComparingTo("2500.00");
        assertThat(resultado.getChavePix()).isEqualTo("joao.novo@pix.com");
    }

    @Test
    void deveLancarExcecaoParaFuncionarioInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, Funcionario.builder().nome("X").build()))
                .isInstanceOf(FuncionarioNaoEncontradoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveRejeitarAtualizacaoParaPixSemChavePix() {
        // Funcionário PIX sem chave — atualização deve falhar na revalidação
        Funcionario existente = Funcionario.builder()
                .id(3L)
                .nome("Ana")
                .salarioBase(new BigDecimal("1600.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("ana@pix.com")
                .ativo(true)
                .build();

        // Tenta remover a chave PIX deixando null — não é possível pois null = não altera
        // Mas troca forma de pagamento para PIX e limpa a chave manualmente via outro campo
        // Simula cenário onde chave_pix fica vazia após atualização de formaPagamento
        Funcionario dadosAtualizados = Funcionario.builder()
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("   ") // chave em branco — deve falhar na validação
                .build();

        when(repository.findById(3L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.atualizar(3L, dadosAtualizados))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chave_pix");

        verify(repository, never()).save(any());
    }

    @Test
    void deveRejeitarAtualizacaoParaTedSemBanco() {
        // Funcionário em PIX sendo migrado para TED sem banco
        Funcionario existente = Funcionario.builder()
                .id(4L)
                .nome("Carlos")
                .salarioBase(new BigDecimal("2200.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("carlos@pix.com")
                .ativo(true)
                .build();

        Funcionario dadosAtualizados = Funcionario.builder()
                .formaPagamento(FormaPagamento.TED)
                // banco, agencia, conta, tipoConta — todos ausentes (null = não altera)
                // mas o existente não tem esses campos → validação deve falhar
                .build();

        when(repository.findById(4L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.atualizar(4L, dadosAtualizados))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void deveMantorCamposNaoInformadosIntactos() {
        Funcionario existente = Funcionario.builder()
                .id(5L)
                .nome("Lucia")
                .salarioBase(new BigDecimal("1700.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("lucia@pix.com")
                .obsPagamento("Observação original")
                .diaPagamentoReferencia(5)
                .ativo(true)
                .build();

        // Só atualiza nome — todos os outros campos devem permanecer
        Funcionario dadosAtualizados = Funcionario.builder()
                .nome("Lucia Nova")
                .build();

        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Funcionario resultado = service.atualizar(5L, dadosAtualizados);

        assertThat(resultado.getNome()).isEqualTo("Lucia Nova");
        assertThat(resultado.getObsPagamento()).isEqualTo("Observação original");
        assertThat(resultado.getDiaPagamentoReferencia()).isEqualTo(5);
        assertThat(resultado.getChavePix()).isEqualTo("lucia@pix.com");
    }
}
