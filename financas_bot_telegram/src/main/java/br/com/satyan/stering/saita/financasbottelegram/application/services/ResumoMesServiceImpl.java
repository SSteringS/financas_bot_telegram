package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.AgregadoStatus;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoPagamentoJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoMesDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoStatusDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ResumoMesUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ResumoMesServiceImpl implements ResumoMesUseCase {

    private final PedidoPagamentoJpaRepository repo;
    private final Clock clock;

    public ResumoMesServiceImpl(PedidoPagamentoJpaRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    public ResumoMesDTO obter(Long requisitanteId, YearMonth ym, String busca) {
        YearMonth mes = ym != null ? ym : YearMonth.now(clock);
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();

        List<AgregadoStatus> agregados;
        if (busca != null && !busca.isBlank()) {
            String pattern = "%" + busca.toLowerCase() + "%";
            agregados = repo.agregarPorStatusNoIntervaloComBusca(requisitanteId, inicio, fim, pattern);
        } else {
            agregados = repo.agregarPorStatusNoIntervalo(requisitanteId, inicio, fim);
        }

        Map<StatusPedido, AgregadoStatus> porStatus = agregados.stream()
                .collect(Collectors.toMap(AgregadoStatus::status, Function.identity()));

        ResumoStatusDTO pendentes = toDTO(porStatus.get(StatusPedido.PENDENTE));
        ResumoStatusDTO pagos = toDTO(porStatus.get(StatusPedido.PAGO));
        ResumoStatusDTO todos = new ResumoStatusDTO(
                pendentes.quantidade() + pagos.quantidade(),
                pendentes.total().add(pagos.total()));

        String mesStr = String.format("%04d-%02d", mes.getYear(), mes.getMonthValue());
        return new ResumoMesDTO(mesStr, todos, pendentes, pagos);
    }

    private ResumoStatusDTO toDTO(AgregadoStatus a) {
        if (a == null) return new ResumoStatusDTO(0, BigDecimal.ZERO);
        return new ResumoStatusDTO((int) a.quantidade(), a.total());
    }
}
