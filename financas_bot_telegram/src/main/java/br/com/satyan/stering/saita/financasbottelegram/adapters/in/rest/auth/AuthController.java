package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest.auth;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.AuthExchangeRequest;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.AuthMeResponse;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.RequisitanteDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.RequisitanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ExchangeTokenUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.CookieFactory;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.JwtService;
import br.com.satyan.stering.saita.financasbottelegram.infra.security.RequisitanteId;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final ExchangeTokenUseCase exchangeUseCase;
    private final JwtService jwtService;
    private final CookieFactory cookieFactory;
    private final RequisitanteRepositoryPort requisitanteRepo;

    public AuthController(
            ExchangeTokenUseCase exchangeUseCase,
            JwtService jwtService,
            CookieFactory cookieFactory,
            RequisitanteRepositoryPort requisitanteRepo) {
        this.exchangeUseCase = exchangeUseCase;
        this.jwtService = jwtService;
        this.cookieFactory = cookieFactory;
        this.requisitanteRepo = requisitanteRepo;
    }

    @PostMapping("/exchange")
    @Operation(summary = "Troca token de uso único por sessão (cookie HTTP-only)")
    public ResponseEntity<AuthMeResponse> exchange(@Valid @RequestBody AuthExchangeRequest req) {
        Requisitante requisitante = exchangeUseCase.exchange(req.token());
        String jwt = jwtService.gerar(requisitante.getId());
        ResponseCookie cookie = cookieFactory.criar(jwt);

        AuthMeResponse body = new AuthMeResponse(
                new RequisitanteDTO(requisitante.getId(), requisitante.getNome()));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o requisitante autenticado")
    public ResponseEntity<AuthMeResponse> me(@RequisitanteId Long requisitanteId) {
        Requisitante r = requisitanteRepo.findById(requisitanteId)
                .orElseThrow(() -> new IllegalStateException(
                        "Requisitante " + requisitanteId + " do JWT não existe"));
        return ResponseEntity.ok(new AuthMeResponse(new RequisitanteDTO(r.getId(), r.getNome())));
    }
}
