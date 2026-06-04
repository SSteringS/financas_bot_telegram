package br.com.satyan.stering.saita.financasbottelegram.application.port.in;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;

/**
 * Port de entrada para cadastrar um plano de adiantamento parcelado.
 */
public interface CadastrarAdiantamentoPortIn {

    Adiantamento cadastrar(Long funcionarioId, Adiantamento adiantamento);
}
