package br.com.satyan.stering.saita.financasbottelegram.domain.exceptions;

/**
 * Lançada ao tentar cancelar um adiantamento que já foi quitado
 * ({@code parcelasPagas == numParcelas}).
 */
public class AdiantamentoJaQuitadoException extends RuntimeException {

    public AdiantamentoJaQuitadoException(Long id) {
        super("Adiantamento já quitado e não pode ser cancelado: id=" + id);
    }
}
