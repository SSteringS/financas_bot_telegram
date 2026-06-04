package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.vo.FormaPagamento;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CadastrarFuncionarioServiceImplTest {

    @Mock
    private FuncionarioRepositoryPortOut repository;

    private CadastrarFuncionarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CadastrarFuncionarioServiceImpl(repository);
    }

    @Test
    void deveCadastrarFuncionarioPixComSucesso() {
        Funcionario funcionario = Funcionario.builder()
                .nome("Maria")
                .salarioBase(new BigDecimal("1800.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("123.456.789-00")
                .build();

        Funcionario savedFuncionario = Funcionario.builder()
                .id(1L).nome("Maria").salarioBase(new BigDecimal("1800.00"))
                .formaPagamento(FormaPagamento.PIX).chavePix("123.456.789-00").build();
        when(repository.save(any())).thenReturn(savedFuncionario);

        Funcionario resultado = service.cadastrar(funcionario);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(repository).save(any(Funcionario.class));
    }

    @Test
    void deveCadastrarFuncionarioTedComSucesso() {
        Funcionario funcionario = Funcionario.builder()
                .nome("João")
                .salarioBase(new BigDecimal("2200.00"))
                .formaPagamento(FormaPagamento.TED)
                .banco("001")
                .agencia("1234")
                .conta("56789-0")
                .tipoConta("CORRENTE")
                .build();

        Funcionario savedFuncionario = Funcionario.builder()
                .id(2L).nome("João").salarioBase(new BigDecimal("2200.00"))
                .formaPagamento(FormaPagamento.TED).build();
        when(repository.save(any())).thenReturn(savedFuncionario);

        Funcionario resultado = service.cadastrar(funcionario);

        assertThat(resultado.getId()).isEqualTo(2L);
    }

    @Test
    void deveRejeitarPixSemChavePix() {
        Funcionario funcionario = Funcionario.builder()
                .nome("Ana")
                .salarioBase(new BigDecimal("1500.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix(null) // faltando!
                .build();

        assertThatThrownBy(() -> service.cadastrar(funcionario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chave_pix");
    }

    @Test
    void deveRejeitarTedSemBanco() {
        Funcionario funcionario = Funcionario.builder()
                .nome("Carlos")
                .salarioBase(new BigDecimal("2000.00"))
                .formaPagamento(FormaPagamento.TED)
                .banco(null) // faltando!
                .agencia("1234")
                .conta("56789-0")
                .tipoConta("CORRENTE")
                .build();

        assertThatThrownBy(() -> service.cadastrar(funcionario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("banco");
    }

    @Test
    void deveRejeitarTedSemTipoConta() {
        Funcionario funcionario = Funcionario.builder()
                .nome("Pedro")
                .salarioBase(new BigDecimal("1900.00"))
                .formaPagamento(FormaPagamento.TED)
                .banco("001")
                .agencia("5678")
                .conta("12345-6")
                .tipoConta(null) // faltando!
                .build();

        assertThatThrownBy(() -> service.cadastrar(funcionario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo_conta");
    }

    @Test
    void deveSetarContaPropriaComoTrueQuandoNull() {
        Funcionario funcionario = Funcionario.builder()
                .nome("Lucia")
                .salarioBase(new BigDecimal("1600.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("lucia@pix.com")
                .contaPropria(null) // deve virar true
                .build();

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Funcionario resultado = service.cadastrar(funcionario);

        assertThat(resultado.getContaPropria()).isTrue();
    }

    @Test
    void deveRejeitarPixComChavePixEmBranco() {
        Funcionario funcionario = Funcionario.builder()
                .nome("Teste")
                .salarioBase(new BigDecimal("1000.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("   ")
                .build();

        assertThatThrownBy(() -> service.cadastrar(funcionario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chave_pix");
    }
}
