package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.vo.CategoriaPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.vo.FormaPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CadastrarValeServiceImplTest {

    @Mock
    private FuncionarioRepositoryPortOut funcionarioRepository;

    @Mock
    private PedidoPagamentoRepositoryPort pedidoRepository;

    private CadastrarValeServiceImpl service;

    private final Funcionario funcionarioAtivo = Funcionario.builder()
            .id(1L)
            .nome("Maria")
            .salarioBase(new BigDecimal("1800.00"))
            .formaPagamento(FormaPagamento.PIX)
            .chavePix("maria@pix.com")
            .ativo(true)
            .build();

    @BeforeEach
    void setUp() {
        service = new CadastrarValeServiceImpl(funcionarioRepository, pedidoRepository);
    }

    @Test
    void deveCriarValeParaFuncionarioAtivo() {
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionarioAtivo));

        PedidoPagamento valeInput = PedidoPagamento.builder()
                .descricao("Vale alimentação")
                .valor(new BigDecimal("150.00"))
                .dataPedido(LocalDate.of(2026, 5, 10))
                .build();

        PedidoPagamento salvo = PedidoPagamento.builder()
                .id(10L)
                .funcionarioId(1L)
                .categoria(CategoriaPedido.VALE)
                .descricao("Vale alimentação")
                .valor(new BigDecimal("150.00"))
                .status(StatusPedido.PENDENTE)
                .fechado(false)
                .dataPedido(LocalDate.of(2026, 5, 10))
                .build();

        when(pedidoRepository.save(any())).thenReturn(salvo);

        PedidoPagamento resultado = service.cadastrar(1L, valeInput);

        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getCategoria()).isEqualTo(CategoriaPedido.VALE);
        assertThat(resultado.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(resultado.getFechado()).isFalse();
    }

    @Test
    void deveSalvarValeComCamposCorretos() {
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionarioAtivo));

        PedidoPagamento valeInput = PedidoPagamento.builder()
                .descricao("Vale transporte")
                .valor(new BigDecimal("200.00"))
                .build();

        when(pedidoRepository.save(any())).thenAnswer(inv -> {
            PedidoPagamento p = inv.getArgument(0);
            return PedidoPagamento.builder()
                    .id(1L).funcionarioId(p.getFuncionarioId())
                    .categoria(p.getCategoria()).status(p.getStatus())
                    .fechado(p.getFechado()).descricao(p.getDescricao())
                    .valor(p.getValor()).dataPedido(p.getDataPedido())
                    .build();
        });

        PedidoPagamento resultado = service.cadastrar(1L, valeInput);

        ArgumentCaptor<PedidoPagamento> captor = ArgumentCaptor.forClass(PedidoPagamento.class);
        verify(pedidoRepository).save(captor.capture());

        PedidoPagamento valeParaSalvar = captor.getValue();
        assertThat(valeParaSalvar.getFuncionarioId()).isEqualTo(1L);
        assertThat(valeParaSalvar.getCategoria()).isEqualTo(CategoriaPedido.VALE);
        assertThat(valeParaSalvar.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(valeParaSalvar.getFechado()).isFalse();
        assertThat(valeParaSalvar.getDataPedido()).isNotNull(); // default hoje
    }

    @Test
    void deveRejeitarFuncionarioInativo() {
        when(funcionarioRepository.findAtivoById(99L)).thenReturn(Optional.empty());

        PedidoPagamento vale = PedidoPagamento.builder()
                .descricao("Vale")
                .valor(new BigDecimal("100.00"))
                .build();

        assertThatThrownBy(() -> service.cadastrar(99L, vale))
                .isInstanceOf(FuncionarioNaoEncontradoException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void deveRejeitarValorNegativo() {
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionarioAtivo));

        PedidoPagamento vale = PedidoPagamento.builder()
                .descricao("Vale")
                .valor(new BigDecimal("-50.00"))
                .build();

        assertThatThrownBy(() -> service.cadastrar(1L, vale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }

    @Test
    void deveRejeitarValorZero() {
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionarioAtivo));

        PedidoPagamento vale = PedidoPagamento.builder()
                .descricao("Vale")
                .valor(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> service.cadastrar(1L, vale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }

    @Test
    void deveUsarDataHojeQuandoDataPedidoNaoInformada() {
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionarioAtivo));

        PedidoPagamento valeInput = PedidoPagamento.builder()
                .descricao("Vale")
                .valor(new BigDecimal("100.00"))
                .dataPedido(null) // não informado
                .build();

        ArgumentCaptor<PedidoPagamento> captor = ArgumentCaptor.forClass(PedidoPagamento.class);
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cadastrar(1L, valeInput);

        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getDataPedido()).isEqualTo(LocalDate.now());
    }
}
