package br.com.satyan.stering.saita.financasbottelegram.domain.exceptions;

import java.time.YearMonth;

/**
 * Lançada quando se tenta fechar um mês que já possui Pedido FOLHA para o funcionário.
 *
 * <p>Mapeada para HTTP 409 pelo {@code RestExceptionHandler}.
 * Protege contra duplo-clique e requests repetidos (idempotência).
 */
public class FechamentoDuplicadoException extends RuntimeException {

    public FechamentoDuplicadoException(Long funcionarioId, YearMonth mes) {
        super("Fechamento já existe para funcionário id=" + funcionarioId + " no mês " + mes);
    }
}
