package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.admin;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.GerarConviteResponse;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.GerarTokenConviteUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.RequisitanteNaoEncontradoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/v1")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final GerarTokenConviteUseCase useCase;
    private final String adminApiKey;

    public AdminController(
            GerarTokenConviteUseCase useCase,
            @Value("${app.admin.api-key}") String adminApiKey) {
        this.useCase = useCase;
        this.adminApiKey = adminApiKey;
    }

    @PostMapping("/requisitantes/{id}/convite")
    @Operation(summary = "Gera link mágico de convite pra um requisitante", security = @SecurityRequirement(name = "AdminApiKey"))
    public ResponseEntity<GerarConviteResponse> gerarConvite(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Key", required = false) String apiKey) {

        if (apiKey == null || !apiKey.equals(adminApiKey)) {
            log.warn("Tentativa de acesso admin com chave inválida ou ausente");
            return ResponseEntity.status(401).build();
        }

        try {
            String url = useCase.gerar(id);
            return ResponseEntity.ok(new GerarConviteResponse(url));
        } catch (RequisitanteNaoEncontradoException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
