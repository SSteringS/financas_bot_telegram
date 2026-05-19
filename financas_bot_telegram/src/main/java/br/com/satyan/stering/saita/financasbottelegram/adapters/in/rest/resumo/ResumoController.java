package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.resumo;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoMesDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ResumoMesUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.MesFormatoInvalidoException;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.RequisitanteId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumo")
public class ResumoController {

    private final ResumoMesUseCase useCase;

    public ResumoController(ResumoMesUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @Operation(summary = "Resumo do mês: todos, pendentes e pagos — com filtros opcionais de mês e busca")
    public ResumoMesDTO obter(
            @RequisitanteId Long requisitanteId,
            @Parameter(description = "Mês no formato YYYY-MM. Default: mês corrente.", example = "2026-05")
            @RequestParam(required = false) String mes,
            @Parameter(description = "Filtra por texto na descrição (contém, case-insensitive).", example = "boleto")
            @RequestParam(required = false) String busca) {

        YearMonth yearMonth = null;
        if (mes != null && !mes.isBlank()) {
            try {
                yearMonth = YearMonth.parse(mes);
            } catch (DateTimeParseException e) {
                throw new MesFormatoInvalidoException(mes);
            }
        }
        return useCase.obter(requisitanteId, yearMonth, busca);
    }
}
