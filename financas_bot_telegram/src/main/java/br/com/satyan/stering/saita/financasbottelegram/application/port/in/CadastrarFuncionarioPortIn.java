package br.com.satyan.stering.saita.financasbottelegram.application.port.in;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;

/**
 * Port de entrada para cadastrar um funcionário doméstico.
 *
 * <p>Interface pura — sem anotações Spring. Implementada por {@code CadastrarFuncionarioServiceImpl}.
 */
public interface CadastrarFuncionarioPortIn {

    Funcionario cadastrar(Funcionario funcionario);
}
