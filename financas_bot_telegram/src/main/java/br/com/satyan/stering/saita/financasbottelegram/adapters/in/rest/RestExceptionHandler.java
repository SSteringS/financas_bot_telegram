package br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.ErroDTO;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.AuthTokenInvalidoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoAutorizadoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest")
public class RestExceptionHandler {

    @ExceptionHandler(AuthTokenInvalidoException.class)
    public ResponseEntity<ErroDTO> handleAuthInvalido(AuthTokenInvalidoException e) {
        return ResponseEntity.status(401)
                .body(new ErroDTO("TOKEN_INVALIDO", e.getMessage()));
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<ErroDTO> handlePedidoNaoEncontrado(PedidoNaoEncontradoException e) {
        return ResponseEntity.status(404)
                .body(new ErroDTO("PEDIDO_NAO_ENCONTRADO", e.getMessage()));
    }

    @ExceptionHandler(PedidoNaoAutorizadoException.class)
    public ResponseEntity<ErroDTO> handlePedidoNaoAutorizado(PedidoNaoAutorizadoException e) {
        return ResponseEntity.status(403)
                .body(new ErroDTO("ACESSO_NEGADO", "Pedido pertence a outro requisitante"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroDTO> handleValidacao(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErroDTO("VALIDACAO", msg));
    }
}
