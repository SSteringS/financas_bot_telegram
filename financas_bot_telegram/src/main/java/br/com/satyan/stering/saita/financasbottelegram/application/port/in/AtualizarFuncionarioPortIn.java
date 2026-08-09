package br.com.satyan.stering.saita.financasbottelegram.application.port.in;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;

/**
 * Port de entrada para atualizar dados de um funcionário doméstico.
 *
 * <p>Interface pura — sem anotações Spring. Implementada por {@code AtualizarFuncionarioServiceImpl}.
 */
public interface AtualizarFuncionarioPortIn {

    Funcionario atualizar(Long id, Funcionario dadosAtualizados);
}
