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
    public ResumoMesDTO obter(Long requisitanteId) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate inicio = hoje.withDayOfMonth(1);
        LocalDate fim = hoje.withDayOfMonth(hoje.lengthOfMonth());

        Map<StatusPedido, AgregadoStatus> porStatus = repo
                .agregarPorStatusNoIntervalo(requisitanteId, inicio, fim).stream()
                .collect(Collectors.toMap(AgregadoStatus::status, Function.identity()));

        ResumoStatusDTO pendentes = toDTO(porStatus.get(StatusPedido.PENDENTE));
        ResumoStatusDTO pagos = toDTO(porStatus.get(StatusPedido.PAGO));

        String mesAtual = String.format("%04d-%02d", hoje.getYear(), hoje.getMonthValue());
        return new ResumoMesDTO(mesAtual, pendentes, pagos);
    }

    private ResumoStatusDTO toDTO(AgregadoStatus a) {
        if (a == null) return new ResumoStatusDTO(0, BigDecimal.ZERO);
        return new ResumoStatusDTO((int) a.quantidade(), a.total());
    }
}
