package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AdiantamentoRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException;
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
class CadastrarAdiantamentoServiceImplTest {

    @Mock
    private FuncionarioRepositoryPortOut funcionarioRepository;

    @Mock
    private AdiantamentoRepositoryPortOut adiantamentoRepository;

    private CadastrarAdiantamentoServiceImpl service;

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
        service = new CadastrarAdiantamentoServiceImpl(funcionarioRepository, adiantamentoRepository);
    }

    // ── Testes de cadastro ─────────────────────────────────────────────────────

    @Test
    void deveCadastrarAdiantamentoComPlanoConsistente() {
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionarioAtivo));

        Adiantamento input = Adiantamento.builder()
                .descricao("Adiantamento de férias")
                .valorTotal(new BigDecimal("300.00"))
                .valorParcela(new BigDecimal("100.00"))
                .numParcelas(3)
                .dataInicio(LocalDate.of(2026, 6, 1))
                .build();

        Adiantamento salvo = Adiantamento.builder()
                .id(10L)
                .funcionarioId(1L)
                .descricao("Adiantamento de férias")
                .valorTotal(new BigDecimal("300.00"))
                .valorParcela(new BigDecimal("100.00"))
                .numParcelas(3)
                .parcelasPagas(0)
                .dataInicio(LocalDate.of(2026, 6, 1))
                .ativo(true)
                .build();

        when(adiantamentoRepository.save(any())).thenReturn(salvo);

        Adiantamento resultado = service.cadastrar(1L, input);

        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getAtivo()).isTrue();
        assertThat(resultado.getParcelasPagas()).isEqualTo(0);
    }

    @Test
    void deveSalvarAdiantamentoComCamposCorretos() {
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionarioAtivo));

        Adiantamento input = Adiantamento.builder()
                .descricao("Empréstimo")
                .valorTotal(new BigDecimal("200.00"))
                .valorParcela(new BigDecimal("100.00"))
                .numParcelas(2)
                .dataInicio(LocalDate.of(2026, 7, 1))
                .build();

        when(adiantamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cadastrar(1L, input);

        ArgumentCaptor<Adiantamento> captor = ArgumentCaptor.forClass(Adiantamento.class);
        verify(adiantamentoRepository).save(captor.capture());

        Adiantamento paraSalvar = captor.getValue();
        assertThat(paraSalvar.getFuncionarioId()).isEqualTo(1L);
        assertThat(paraSalvar.getAtivo()).isTrue();
        assertThat(paraSalvar.getParcelasPagas()).isEqualTo(0);
        assertThat(paraSalvar.getDescricao()).isEqualTo("Empréstimo");
    }

    @Test
    void deveRejeitarPlanoInconsistente() {
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionarioAtivo));

        // valorTotal=300, valorParcela=100, numParcelas=4 → 100*4=400 ≠ 300 → diferença 100 > 0.01
        Adiantamento input = Adiantamento.builder()
                .descricao("Plano inconsistente")
                .valorTotal(new BigDecimal("300.00"))
                .valorParcela(new BigDecimal("100.00"))
                .numParcelas(4)
                .dataInicio(LocalDate.of(2026, 6, 1))
                .build();

        assertThatThrownBy(() -> service.cadastrar(1L, input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inconsistente");
    }

    @Test
    void deveRejeitarFuncionarioInativo() {
        when(funcionarioRepository.findAtivoById(99L)).thenReturn(Optional.empty());

        Adiantamento input = Adiantamento.builder()
                .descricao("Adiantamento")
                .valorTotal(new BigDecimal("100.00"))
                .valorParcela(new BigDecimal("100.00"))
                .numParcelas(1)
                .dataInicio(LocalDate.of(2026, 6, 1))
                .build();

        assertThatThrownBy(() -> service.cadastrar(99L, input))
                .isInstanceOf(FuncionarioNaoEncontradoException.class);
    }

    // ── Testes de validarConsistenciaMatemática (método package-private) ──────

    @Test
    void deveAceitarPlanoDentroDaTolerancia() {
        // 100.00 * 3 = 300.00; diferença = |300.00 - 300.00| = 0 ≤ 0.01
        Adiantamento a = Adiantamento.builder()
                .valorTotal(new BigDecimal("300.00"))
                .valorParcela(new BigDecimal("100.00"))
                .numParcelas(3)
                .build();

        // Não deve lançar exceção
        CadastrarAdiantamentoServiceImpl.validarConsistenciaMatemática(a);
    }

    @Test
    void deveAceitarPlanoDentroDaToleranciaComArredondamento() {
        // 33.33 * 3 = 99.99; |100.00 - 99.99| = 0.01 == tolerância (<=, não <)
        Adiantamento a = Adiantamento.builder()
                .valorTotal(new BigDecimal("100.00"))
                .valorParcela(new BigDecimal("33.33"))
                .numParcelas(3)
                .build();

        // Diferença exata = 0.01, deve ser aceito
        CadastrarAdiantamentoServiceImpl.validarConsistenciaMatemática(a);
    }

    @Test
    void deveRejeitarPlanoComDiferencaAcimaDeTolerancia() {
        // 100.01 * 3 = 300.03; |300.00 - 300.03| = 0.03 > 0.01
        Adiantamento a = Adiantamento.builder()
                .valorTotal(new BigDecimal("300.00"))
                .valorParcela(new BigDecimal("100.01"))
                .numParcelas(3)
                .build();

        assertThatThrownBy(() -> CadastrarAdiantamentoServiceImpl.validarConsistenciaMatemática(a))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inconsistente");
    }
}
