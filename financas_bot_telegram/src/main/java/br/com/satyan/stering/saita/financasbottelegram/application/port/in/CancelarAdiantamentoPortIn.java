package br.com.satyan.stering.saita.financasbottelegram.application.port.in;

/**
 * Port de entrada para cancelar um adiantamento ativo (soft cancel: ativo=false).
 */
public interface CancelarAdiantamentoPortIn {

    void cancelar(Long adiantamentoId);
}
