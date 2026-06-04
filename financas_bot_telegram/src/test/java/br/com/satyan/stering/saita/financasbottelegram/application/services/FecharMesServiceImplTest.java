package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AdiantamentoRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FechamentoDuplicadoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.vo.CategoriaPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.vo.FormaPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FecharMesServiceImplTest {

    @Mock
    private FuncionarioRepositoryPortOut funcionarioRepository;

    @Mock
    private PedidoPagamentoRepositoryPort pedidoRepository;

    @Mock
    private AdiantamentoRepositoryPortOut adiantamentoRepository;

    private FecharMesServiceImpl service;

    private static final YearMonth MES = YearMonth.of(2026, 5);
    private static final LocalDate PRIMEIRO_DO_MES = LocalDate.of(2026, 5, 1);

    private final Funcionario funcionario = Funcionario.builder()
            .id(1L)
            .nome("Maria")
            .salarioBase(new BigDecimal("2000.00"))
            .formaPagamento(FormaPagamento.PIX)
            .chavePix("maria@pix.com")
            .ativo(true)
            .build();

    @BeforeEach
    void setUp() {
        service = new FecharMesServiceImpl(funcionarioRepository, pedidoRepository, adiantamentoRepository);
    }

    // ── (1) Fechamento padrão ─────────────────────────────────────────────────

    @Test
    void deveFazerFechamentoComValesEAdiantamentos() {
        when(pedidoRepository.existsFolha(1L, PRIMEIRO_DO_MES)).thenReturn(false);
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionario));

        PedidoPagamento vale1 = pedido(10L, new BigDecimal("80.00"));
        PedidoPagamento vale2 = pedido(11L, new BigDecimal("70.00"));
        when(pedidoRepository.findValesAbertos(eq(1L), any(), any()))
                .thenReturn(List.of(vale1, vale2)); // totalVales = 150

        Adiantamento adiant1 = adiantamento(20L, new BigDecimal("50.00"), 3, 1);
        Adiantamento adiant2 = adiantamento(21L, new BigDecimal("30.00"), 2, 0);
        when(adiantamentoRepository.findAtivosParaFechamento(1L))
                .thenReturn(List.of(adiant1, adiant2)); // totalParcelas = 80

        // valorFinal = 2000 - 150 - 80 + 0 = 1770
        PedidoPagamento salvo = pedidoFolha(100L, new BigDecimal("1770.00"));
        when(pedidoRepository.save(any())).thenReturn(salvo);
        when(adiantamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PedidoPagamento resultado = service.fechar(1L, MES, BigDecimal.ZERO);

        assertThat(resultado.getId()).isEqualTo(100L);
        assertThat(resultado.getValor()).isEqualByComparingTo("1770.00");

        // passo 9: markAllClosed com IDs dos vales
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(pedidoRepository).markAllClosed(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(10L, 11L);

        // passo 10: 2 adiantamentos atualizados
        verify(adiantamentoRepository, times(2)).save(any());
    }

    @Test
    void deveSalvarPedidoFolhaComCamposCorretos() {
        when(pedidoRepository.existsFolha(1L, PRIMEIRO_DO_MES)).thenReturn(false);
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionario));
        when(pedidoRepository.findValesAbertos(any(), any(), any())).thenReturn(List.of());
        when(adiantamentoRepository.findAtivosParaFechamento(any())).thenReturn(List.of());
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.fechar(1L, MES, BigDecimal.ZERO);

        ArgumentCaptor<PedidoPagamento> captor = ArgumentCaptor.forClass(PedidoPagamento.class);
        verify(pedidoRepository).save(captor.capture());

        PedidoPagamento paraSalvar = captor.getValue();
        assertThat(paraSalvar.getCategoria()).isEqualTo(CategoriaPedido.FOLHA);
        assertThat(paraSalvar.getFuncionarioId()).isEqualTo(1L);
        assertThat(paraSalvar.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(paraSalvar.getMesReferencia()).isEqualTo(PRIMEIRO_DO_MES);
        assertThat(paraSalvar.getObservacao()).isNotBlank();
        assertThat(paraSalvar.getRequisitanteId()).isNull(); // sistema, sem requisitante
    }

    // ── (2) Adiantamento quitado na última parcela ────────────────────────────

    @Test
    void deveDesativarAdiantamentoNaUltimaParcela() {
        when(pedidoRepository.existsFolha(1L, PRIMEIRO_DO_MES)).thenReturn(false);
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionario));
        when(pedidoRepository.findValesAbertos(any(), any(), any())).thenReturn(List.of());

        // numParcelas=3, parcelasPagas=2 → após incremento fica 3 (quitado)
        Adiantamento ultimaParcela = adiantamento(30L, new BigDecimal("100.00"), 3, 2);
        when(adiantamentoRepository.findAtivosParaFechamento(1L))
                .thenReturn(List.of(ultimaParcela));

        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(adiantamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.fechar(1L, MES, BigDecimal.ZERO);

        ArgumentCaptor<Adiantamento> captor = ArgumentCaptor.forClass(Adiantamento.class);
        verify(adiantamentoRepository).save(captor.capture());

        Adiantamento atualizado = captor.getValue();
        assertThat(atualizado.getParcelasPagas()).isEqualTo(3);
        assertThat(atualizado.getAtivo()).isFalse(); // quitado
    }

    // ── (3) Sem vales nem adiantamentos ──────────────────────────────────────

    @Test
    void deveFazerFechamentoSemValesNemAdiantamentos() {
        when(pedidoRepository.existsFolha(1L, PRIMEIRO_DO_MES)).thenReturn(false);
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionario));
        when(pedidoRepository.findValesAbertos(any(), any(), any())).thenReturn(List.of());
        when(adiantamentoRepository.findAtivosParaFechamento(any())).thenReturn(List.of());

        when(pedidoRepository.save(any())).thenAnswer(inv -> {
            PedidoPagamento p = inv.getArgument(0);
            return PedidoPagamento.builder().id(1L).valor(p.getValor())
                    .observacao(p.getObservacao()).mesReferencia(p.getMesReferencia())
                    .status(p.getStatus()).categoria(p.getCategoria())
                    .funcionarioId(p.getFuncionarioId()).build();
        });

        PedidoPagamento resultado = service.fechar(1L, MES, BigDecimal.ZERO);

        assertThat(resultado.getValor()).isEqualByComparingTo("2000.00"); // salário base
        verify(pedidoRepository, never()).markAllClosed(anyList());
        verify(adiantamentoRepository, never()).save(any());
    }

    // ── (4) Ajuste positivo ───────────────────────────────────────────────────

    @Test
    void deveAplicarAjustePositivo() {
        when(pedidoRepository.existsFolha(1L, PRIMEIRO_DO_MES)).thenReturn(false);
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionario));
        when(pedidoRepository.findValesAbertos(any(), any(), any())).thenReturn(List.of());
        when(adiantamentoRepository.findAtivosParaFechamento(any())).thenReturn(List.of());
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.fechar(1L, MES, new BigDecimal("200.00")); // bônus R$200

        ArgumentCaptor<PedidoPagamento> captor = ArgumentCaptor.forClass(PedidoPagamento.class);
        verify(pedidoRepository).save(captor.capture());
        // 2000 + 200 = 2200
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("2200.00");
    }

    // ── (5) Ajuste negativo ───────────────────────────────────────────────────

    @Test
    void deveAplicarAjusteNegativo() {
        when(pedidoRepository.existsFolha(1L, PRIMEIRO_DO_MES)).thenReturn(false);
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionario));
        when(pedidoRepository.findValesAbertos(any(), any(), any())).thenReturn(List.of());
        when(adiantamentoRepository.findAtivosParaFechamento(any())).thenReturn(List.of());
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.fechar(1L, MES, new BigDecimal("-150.00")); // desconto extra

        ArgumentCaptor<PedidoPagamento> captor = ArgumentCaptor.forClass(PedidoPagamento.class);
        verify(pedidoRepository).save(captor.capture());
        // 2000 - 150 = 1850
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("1850.00");
    }

    // ── (6) Valor final negativo (vales > salário) ───────────────────────────

    @Test
    void devePermitirValorFinalNegativo() {
        when(pedidoRepository.existsFolha(1L, PRIMEIRO_DO_MES)).thenReturn(false);
        when(funcionarioRepository.findAtivoById(1L)).thenReturn(Optional.of(funcionario));

        // vale de R$2500 > salário de R$2000
        PedidoPagamento valeGrande = pedido(50L, new BigDecimal("2500.00"));
        when(pedidoRepository.findValesAbertos(any(), any(), any())).thenReturn(List.of(valeGrande));
        when(adiantamentoRepository.findAtivosParaFechamento(any())).thenReturn(List.of());
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PedidoPagamento resultado = service.fechar(1L, MES, BigDecimal.ZERO);

        // Domínio permite valor negativo
        assertThat(resultado.getValor()).isEqualByComparingTo("-500.00");
    }

    // ── (7) Fechamento duplicado ──────────────────────────────────────────────

    @Test
    void deveRejeitarFechamentoDuplicado() {
        when(pedidoRepository.existsFolha(1L, PRIMEIRO_DO_MES)).thenReturn(true);

        assertThatThrownBy(() -> service.fechar(1L, MES, BigDecimal.ZERO))
                .isInstanceOf(FechamentoDuplicadoException.class)
                .hasMessageContaining("2026-05");

        verify(pedidoRepository, never()).save(any());
    }

    // ── (8) Funcionário inativo ───────────────────────────────────────────────

    @Test
    void deveRejeitarFuncionarioInativo() {
        when(pedidoRepository.existsFolha(99L, PRIMEIRO_DO_MES)).thenReturn(false);
        when(funcionarioRepository.findAtivoById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.fechar(99L, MES, BigDecimal.ZERO))
                .isInstanceOf(FuncionarioNaoEncontradoException.class);

        verify(pedidoRepository, never()).save(any());
    }

    // ── gerarTextoFechamento ──────────────────────────────────────────────────

    @Test
    void deveGerarTextoFechamentoComFormatoCorreto() {
        String texto = FecharMesServiceImpl.gerarTextoFechamento(
                new BigDecimal("2000.00"),
                new BigDecimal("150.00"), 3,
                new BigDecimal("80.00"), 2,
                BigDecimal.ZERO,
                new BigDecimal("1770.00"));

        assertThat(texto).contains("Salario base: R$ 2.000,00");
        assertThat(texto).contains("Vales: R$ 150,00 (3 vales)");
        assertThat(texto).contains("Adiantamentos: R$ 80,00 (2 parcelas)");
        assertThat(texto).contains("Ajuste: R$ 0,00");
        assertThat(texto).contains("Liquido: R$ 1.770,00");
    }

    @Test
    void deveUsarSingularParaUmValeEUmaParcela() {
        String texto = FecharMesServiceImpl.gerarTextoFechamento(
                new BigDecimal("1000.00"),
                new BigDecimal("50.00"), 1,
                new BigDecimal("100.00"), 1,
                BigDecimal.ZERO,
                new BigDecimal("850.00"));

        assertThat(texto).contains("(1 vale)");
        assertThat(texto).contains("(1 parcela)");
    }

    @Test
    void deveGerarTextoParaFechamentoSemDescontos() {
        String texto = FecharMesServiceImpl.gerarTextoFechamento(
                new BigDecimal("1800.00"),
                BigDecimal.ZERO, 0,
                BigDecimal.ZERO, 0,
                BigDecimal.ZERO,
                new BigDecimal("1800.00"));

        assertThat(texto).contains("Salario base: R$ 1.800,00");
        assertThat(texto).contains("(0 vales)");
        assertThat(texto).contains("(0 parcelas)");
        assertThat(texto).contains("Liquido: R$ 1.800,00");
    }

    // ── Helpers de teste ─────────────────────────────────────────────────────

    private PedidoPagamento pedido(Long id, BigDecimal valor) {
        return PedidoPagamento.builder()
                .id(id)
                .valor(valor)
                .dataPedido(LocalDate.of(2026, 5, 15))
                .build();
    }

    private PedidoPagamento pedidoFolha(Long id, BigDecimal valor) {
        return PedidoPagamento.builder()
                .id(id)
                .valor(valor)
                .categoria(CategoriaPedido.FOLHA)
                .status(StatusPedido.PENDENTE)
                .mesReferencia(PRIMEIRO_DO_MES)
                .dataPedido(PRIMEIRO_DO_MES)
                .build();
    }

    private Adiantamento adiantamento(Long id, BigDecimal valorParcela, int numParcelas, int parcelasPagas) {
        return Adiantamento.builder()
                .id(id)
                .funcionarioId(1L)
                .valorParcela(valorParcela)
                .numParcelas(numParcelas)
                .parcelasPagas(parcelasPagas)
                .dataInicio(LocalDate.of(2026, 1, 1)) // anterior a maio/26
                .ativo(true)
                .build();
    }
}
