package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.funcionario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.AtualizarFuncionarioPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CadastrarFuncionarioPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.FormaPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class FuncionarioControllerTest {

    @Mock
    private CadastrarFuncionarioPortIn cadastrarUseCase;

    @Mock
    private AtualizarFuncionarioPortIn atualizarUseCase;

    @Mock
    private FuncionarioRepositoryPortOut repository;

    private FuncionarioController controller;

    @BeforeEach
    void setUp() {
        controller = new FuncionarioController(cadastrarUseCase, atualizarUseCase, repository);
    }

    @Test
    void postDeveCriarFuncionarioERetornar201() {
        Funcionario criado = Funcionario.builder()
                .id(1L)
                .nome("Maria")
                .salarioBase(new BigDecimal("1800.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("123.456.789-00")
                .ativo(true)
                .contaPropria(true)
                .build();

        when(cadastrarUseCase.cadastrar(any())).thenReturn(criado);

        var request = new br.com.satyan.stering.saita.financasbottelegram.application.dto.FuncionarioRequest();
        request.setNome("Maria");
        request.setSalarioBase(new BigDecimal("1800.00"));
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setChavePix("123.456.789-00");

        ResponseEntity<?> response = controller.cadastrar(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        verify(cadastrarUseCase).cadastrar(any());
    }

    @Test
    void getDeveListarFuncionariosAtivos() {
        Funcionario f = Funcionario.builder()
                .id(1L)
                .nome("João")
                .salarioBase(new BigDecimal("2000.00"))
                .formaPagamento(FormaPagamento.TED)
                .ativo(true)
                .build();

        when(repository.findAllAtivos()).thenReturn(List.of(f));

        List<?> result = controller.listar();

        assertThat(result).hasSize(1);
    }

    @Test
    void getDeveRetornarFuncionarioPorId() {
        Funcionario f = Funcionario.builder()
                .id(1L)
                .nome("Ana")
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("ana@pix.com")
                .ativo(true)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(f));

        var response = controller.buscar(1L);

        assertThat(response.getNome()).isEqualTo("Ana");
    }

    @Test
    void deveLancarExcecaoQuandoFuncionarioNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.buscar(99L))
                .isInstanceOf(FuncionarioNaoEncontradoException.class);
    }

    @Test
    void deleteDeveDesativarFuncionario() {
        Funcionario f = Funcionario.builder().id(1L).nome("Carlos").ativo(true).build();
        when(repository.findById(1L)).thenReturn(Optional.of(f));

        controller.desativar(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void putDeveAtualizarERetornarFuncionario() {
        Funcionario atualizado = Funcionario.builder()
                .id(1L)
                .nome("Maria Nova")
                .salarioBase(new BigDecimal("2000.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("nova@pix.com")
                .ativo(true)
                .contaPropria(true)
                .build();

        when(atualizarUseCase.atualizar(eq(1L), any())).thenReturn(atualizado);

        var request = new br.com.satyan.stering.saita.financasbottelegram.application.dto.FuncionarioRequest();
        request.setNome("Maria Nova");
        request.setSalarioBase(new BigDecimal("2000.00"));
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setChavePix("nova@pix.com");

        var response = controller.atualizar(1L, request);

        assertThat(response.getSalarioBase()).isEqualByComparingTo("2000.00");
    }

    @Test
    void deleteDeveLancarExcecaoQuandoFuncionarioNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.desativar(99L))
                .isInstanceOf(FuncionarioNaoEncontradoException.class);
    }
}
