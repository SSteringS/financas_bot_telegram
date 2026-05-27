package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.AgregadoStatus;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoPagamentoJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoMesDTO;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumoMesServiceImplTest {

    @Mock private PedidoPagamentoJpaRepository repo;

    private ResumoMesServiceImpl service;
    private static final Clock MAIO_2026 = Clock.fixed(
            Instant.parse("2026-05-15T10:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        service = new ResumoMesServiceImpl(repo, MAIO_2026);
    }

    @Test
    void deveUsarMesCorrenteQuandoMesNulo() {
        when(repo.agregarPorStatusNoIntervalo(eq(1L), any(), any())).thenReturn(List.of());

        ResumoMesDTO dto = service.obter(1L, null, null);

        assertThat(dto.mes()).isEqualTo("2026-05");
    }

    @Test
    void deveUsarMesEspecificoQuandoInformado() {
        when(repo.agregarPorStatusNoIntervalo(eq(1L),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)))).thenReturn(List.of());

        ResumoMesDTO dto = service.obter(1L, YearMonth.of(2026, 4), null);

        assertThat(dto.mes()).isEqualTo("2026-04");
        verify(repo).agregarPorStatusNoIntervalo(eq(1L),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)));
    }

    @Test
    void deveAggregarPedidosPendentesEPagos() {
        AgregadoStatus pendentes = new AgregadoStatus(StatusPedido.PENDENTE, 3, new BigDecimal("900.00"));
        AgregadoStatus pagos = new AgregadoStatus(StatusPedido.PAGO, 2, new BigDecimal("500.00"));
        when(repo.agregarPorStatusNoIntervalo(eq(1L), any(), any())).thenReturn(List.of(pendentes, pagos));

        ResumoMesDTO dto = service.obter(1L, null, null);

        assertThat(dto.pendentes().quantidade()).isEqualTo(3);
        assertThat(dto.pendentes().total()).isEqualByComparingTo("900.00");
        assertThat(dto.pagos().quantidade()).isEqualTo(2);
        assertThat(dto.pagos().total()).isEqualByComparingTo("500.00");
    }

    @Test
    void deveCalcularTodosComSomaDePendentesEPagos() {
        AgregadoStatus pendentes = new AgregadoStatus(StatusPedido.PENDENTE, 3, new BigDecimal("900.00"));
        AgregadoStatus pagos = new AgregadoStatus(StatusPedido.PAGO, 2, new BigDecimal("500.00"));
        when(repo.agregarPorStatusNoIntervalo(eq(1L), any(), any())).thenReturn(List.of(pendentes, pagos));

        ResumoMesDTO dto = service.obter(1L, null, null);

        assertThat(dto.todos().quantidade()).isEqualTo(5);
        assertThat(dto.todos().total()).isEqualByComparingTo("1400.00");
    }

    @Test
    void deveRetornarZerosQuandoSemPedidosNoMes() {
        when(repo.agregarPorStatusNoIntervalo(eq(1L), any(), any())).thenReturn(List.of());

        ResumoMesDTO dto = service.obter(1L, null, null);

        assertThat(dto.todos().quantidade()).isZero();
        assertThat(dto.todos().total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.pendentes().quantidade()).isZero();
        assertThat(dto.pagos().quantidade()).isZero();
    }

    @Test
    void deveUsarQueryComBuscaQuandoBuscaNaoNula() {
        when(repo.agregarPorStatusNoIntervaloComBusca(eq(1L), any(), any(), eq("%pix%")))
                .thenReturn(List.of());

        service.obter(1L, null, "PIX");

        verify(repo).agregarPorStatusNoIntervaloComBusca(eq(1L), any(), any(), eq("%pix%"));
    }

    @Test
    void deveUsarQuerySemBuscaQuandoBuscaNulaOuBranca() {
        when(repo.agregarPorStatusNoIntervalo(eq(1L), any(), any())).thenReturn(List.of());

        service.obter(1L, null, null);
        service.obter(1L, null, "   ");

        verify(repo, org.mockito.Mockito.times(2)).agregarPorStatusNoIntervalo(eq(1L), any(), any());
    }

    @Test
    void deveIgnorarStatusCancelado() {
        AgregadoStatus cancelados = new AgregadoStatus(StatusPedido.CANCELADO, 5, new BigDecimal("1000.00"));
        when(repo.agregarPorStatusNoIntervalo(eq(1L), any(), any())).thenReturn(List.of(cancelados));

        ResumoMesDTO dto = service.obter(1L, null, null);

        assertThat(dto.pendentes().quantidade()).isZero();
        assertThat(dto.pagos().quantidade()).isZero();
        assertThat(dto.todos().quantidade()).isZero();
    }

    @Test
    void deveChamarQueryComIntervaloDoMesCorreto() {
        when(repo.agregarPorStatusNoIntervalo(eq(1L),
                eq(LocalDate.of(2026, 5, 1)),
                eq(LocalDate.of(2026, 5, 31))))
                .thenReturn(List.of());

        service.obter(1L, null, null);

        verify(repo).agregarPorStatusNoIntervalo(eq(1L),
                eq(LocalDate.of(2026, 5, 1)),
                eq(LocalDate.of(2026, 5, 31)));
    }
}
