package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.folha;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.whatsapp.service.WhatsAppMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CadastrarAdiantamentoPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CadastrarValePortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CancelarAdiantamentoPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.in.FecharMesPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AdiantamentoRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.CategoriaPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.CookieFactory;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.JwtService;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de controller para {@link FolhaController}.
 *
 * <p>Usa {@code @WebMvcTest} para exercitar a camada de filtros JWT junto com o
 * controller, garantindo que os endpoints protegidos retornam 401 sem cookie e
 * 200/201/204 com autenticação válida.
 *
 * <p>As 401 verificações validam que FIX-005 (JWT allowlist) está ativo nos
 * endpoints de folha.
 */
@WebMvcTest(FolhaController.class)
@TestPropertySource(properties = "app.cors.allowed-origin=http://localhost:3000")
class FolhaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Dependências do FolhaController (mockadas) ─────────────────────────────
    @MockBean private CadastrarValePortIn cadastrarValeUseCase;
    @MockBean private PedidoPagamentoRepositoryPort pedidoRepository;
    @MockBean private CadastrarAdiantamentoPortIn cadastrarAdiantamentoUseCase;
    @MockBean private CancelarAdiantamentoPortIn cancelarAdiantamentoUseCase;
    @MockBean private AdiantamentoRepositoryPortOut adiantamentoRepository;
    @MockBean private FecharMesPortIn fecharMesUseCase;

    // ── Dependências do JwtAuthenticationFilter (auto-carregado por @WebMvcTest) ─
    @MockBean private JwtService jwtService;
    @MockBean private CookieFactory cookieFactory;

    // ── Dependências de @ControllerAdvice globais carregados por @WebMvcTest ────
    // GlobalTelegramExceptionHandler e GlobalWhatsAppExceptionHandler são beans
    // que o @WebMvcTest carrega mesmo com basePackages restritos — suas dependências
    // precisam estar disponíveis no contexto de teste.
    @MockBean private TelegramMessageSenderService telegramMessageSenderService;
    @MockBean private WhatsAppMessageSenderService whatsAppMessageSenderService;

    /** Cookie de sessão injetado nos requests autenticados. */
    private static final Cookie SESSION = new Cookie("finbot_session", "valid-jwt");

    @BeforeEach
    void configurarJwtMock() {
        // JwtAuthenticationFilter aceita o token e seta requisitanteId=1 no request
        when(jwtService.validarERetornarRequisitanteId("valid-jwt")).thenReturn(1L);
        when(jwtService.precisaRenovar("valid-jwt")).thenReturn(false);
    }

    // ── POST /api/v1/funcionarios/{id}/vales ──────────────────────────────────

    @Test
    void postVale_deveRetornar201ComVale() throws Exception {
        PedidoPagamento vale = PedidoPagamento.builder()
                .id(10L).funcionarioId(1L)
                .descricao("Vale refeição").valor(new BigDecimal("80.00"))
                .status(StatusPedido.PENDENTE).categoria(CategoriaPedido.VALE)
                .fechado(false).dataPedido(LocalDate.of(2026, 5, 10))
                .build();
        when(cadastrarValeUseCase.cadastrar(eq(1L), any())).thenReturn(vale);

        mockMvc.perform(post("/api/v1/funcionarios/1/vales")
                        .cookie(SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Vale refeição\",\"valor\":80.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.descricao").value("Vale refeição"));
    }

    @Test
    void postVale_deveRetornar400ParaBodySemValor() throws Exception {
        // valor é @NotNull — Bean Validation deve rejeitar
        mockMvc.perform(post("/api/v1/funcionarios/1/vales")
                        .cookie(SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Vale\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postVale_deveRetornar400ParaDescricaoVazia() throws Exception {
        mockMvc.perform(post("/api/v1/funcionarios/1/vales")
                        .cookie(SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"\",\"valor\":50.00}"))
                .andExpect(status().isBadRequest());
    }

    /** FIX-005: endpoint protegido por JWT — sem cookie retorna 401. */
    @Test
    void postVale_deveRetornar401SemCookie() throws Exception {
        mockMvc.perform(post("/api/v1/funcionarios/1/vales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Vale\",\"valor\":50.00}"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/v1/funcionarios/{id}/vales ───────────────────────────────────

    @Test
    void getVales_deveRetornar200ComLista() throws Exception {
        PedidoPagamento vale = PedidoPagamento.builder()
                .id(2L).funcionarioId(1L)
                .descricao("Vale transporte").valor(new BigDecimal("120.00"))
                .status(StatusPedido.PENDENTE).categoria(CategoriaPedido.VALE)
                .fechado(false).dataPedido(LocalDate.of(2026, 5, 5))
                .build();
        when(pedidoRepository.findValesByFuncionarioAndPeriodo(eq(1L), any(), any()))
                .thenReturn(List.of(vale));

        mockMvc.perform(get("/api/v1/funcionarios/1/vales?mes=2026-05")
                        .cookie(SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].descricao").value("Vale transporte"));
    }

    @Test
    void getVales_deveRetornar400ParaMesFormatoInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/funcionarios/1/vales?mes=foobar")
                        .cookie(SESSION))
                .andExpect(status().isBadRequest());
    }

    /** FIX-005: endpoint protegido — sem cookie retorna 401. */
    @Test
    void getVales_deveRetornar401SemCookie() throws Exception {
        mockMvc.perform(get("/api/v1/funcionarios/1/vales"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/v1/funcionarios/{id}/adiantamentos ──────────────────────────

    @Test
    void postAdiantamento_deveRetornar201() throws Exception {
        Adiantamento adiantamento = Adiantamento.builder()
                .id(5L).funcionarioId(1L)
                .descricao("Empréstimo equipamento").valorTotal(new BigDecimal("600.00"))
                .valorParcela(new BigDecimal("200.00")).numParcelas(3).parcelasPagas(0)
                .dataInicio(LocalDate.of(2026, 3, 1)).ativo(true)
                .build();
        when(cadastrarAdiantamentoUseCase.cadastrar(eq(1L), any())).thenReturn(adiantamento);

        mockMvc.perform(post("/api/v1/funcionarios/1/adiantamentos")
                        .cookie(SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Empréstimo equipamento\",\"valorTotal\":600.00,"
                                + "\"valorParcela\":200.00,\"numParcelas\":3,"
                                + "\"dataInicio\":\"2026-03-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.parcelasRestantes").value(3));
    }

    @Test
    void postAdiantamento_deveRetornar400SemNumParcelas() throws Exception {
        // numParcelas é @NotNull @Min(1)
        mockMvc.perform(post("/api/v1/funcionarios/1/adiantamentos")
                        .cookie(SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Teste\",\"valorTotal\":600.00,"
                                + "\"valorParcela\":200.00,\"dataInicio\":\"2026-03-01\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/v1/funcionarios/{id}/adiantamentos ───────────────────────────

    @Test
    void getAdiantamentos_deveRetornar200ComLista() throws Exception {
        Adiantamento a = Adiantamento.builder()
                .id(1L).funcionarioId(1L)
                .descricao("Adiantamento ativo").valorTotal(new BigDecimal("300.00"))
                .valorParcela(new BigDecimal("100.00")).numParcelas(3).parcelasPagas(1)
                .dataInicio(LocalDate.of(2026, 1, 1)).ativo(true)
                .build();
        when(adiantamentoRepository.findAtivosParaFuncionario(1L)).thenReturn(List.of(a));

        mockMvc.perform(get("/api/v1/funcionarios/1/adiantamentos")
                        .cookie(SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].parcelasRestantes").value(2));
    }

    // ── DELETE /api/v1/funcionarios/adiantamentos/{id} ────────────────────────

    @Test
    void deleteAdiantamento_deveRetornar204() throws Exception {
        doNothing().when(cancelarAdiantamentoUseCase).cancelar(anyLong());

        mockMvc.perform(delete("/api/v1/funcionarios/adiantamentos/1")
                        .cookie(SESSION))
                .andExpect(status().isNoContent());
    }

    /** FIX-005: endpoint de cancelamento também é protegido. */
    @Test
    void deleteAdiantamento_deveRetornar401SemCookie() throws Exception {
        mockMvc.perform(delete("/api/v1/funcionarios/adiantamentos/1"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/v1/funcionarios/{id}/fechamentos ────────────────────────────

    @Test
    void postFechamento_deveRetornar200ComPedidoFolha() throws Exception {
        PedidoPagamento folha = PedidoPagamento.builder()
                .id(20L).funcionarioId(1L)
                .descricao("FOLHA 2026-05").valor(new BigDecimal("1850.00"))
                .status(StatusPedido.PENDENTE).categoria(CategoriaPedido.FOLHA)
                .dataPedido(LocalDate.of(2026, 5, 31))
                .build();
        when(fecharMesUseCase.fechar(eq(1L), any(), any())).thenReturn(folha);

        mockMvc.perform(post("/api/v1/funcionarios/1/fechamentos")
                        .cookie(SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mes\":\"2026-05\",\"ajuste\":0.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void postFechamento_deveRetornar400ParaMesFormatoInvalido() throws Exception {
        // parseMesYearMonth("foobar") lança IllegalArgumentException → 400
        mockMvc.perform(post("/api/v1/funcionarios/1/fechamentos")
                        .cookie(SESSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mes\":\"foobar\",\"ajuste\":0.00}"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/v1/funcionarios/{id}/fechamentos ─────────────────────────────

    @Test
    void getFechamentos_deveRetornar200ComLista() throws Exception {
        PedidoPagamento folha = PedidoPagamento.builder()
                .id(30L).funcionarioId(1L)
                .descricao("FOLHA 2026-04").valor(new BigDecimal("2000.00"))
                .status(StatusPedido.PENDENTE).categoria(CategoriaPedido.FOLHA)
                .dataPedido(LocalDate.of(2026, 4, 30))
                .build();
        when(pedidoRepository.findFolhasByFuncionario(1L)).thenReturn(List.of(folha));

        mockMvc.perform(get("/api/v1/funcionarios/1/fechamentos")
                        .cookie(SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30));
    }

    /** FIX-005: listagem de fechamentos também requer JWT. */
    @Test
    void getFechamentos_deveRetornar401SemCookie() throws Exception {
        mockMvc.perform(get("/api/v1/funcionarios/1/fechamentos"))
                .andExpect(status().isUnauthorized());
    }
}
