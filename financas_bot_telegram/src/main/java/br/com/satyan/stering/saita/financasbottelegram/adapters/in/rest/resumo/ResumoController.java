package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.resumo;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ResumoMesDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ResumoMesUseCase;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.RequisitanteId;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumo")
public class ResumoController {

    private final ResumoMesUseCase useCase;

    public ResumoController(ResumoMesUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @Operation(summary = "Resumo do mês corrente: pendentes + pagos")
    public ResumoMesDTO obter(@RequisitanteId Long requisitanteId) {
        return useCase.obter(requisitanteId);
    }
}
