package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import java.util.List;
import java.util.Optional;

/**
 * Port de saída para persistência de {@link Funcionario}.
 *
 * <p>Interface pura — sem anotações Spring. Implementada pelo adapter JPA
 * {@code FuncionarioRepositoryAdapter}.
 */
public interface FuncionarioRepositoryPortOut {

    Funcionario save(Funcionario funcionario);

    Optional<Funcionario> findById(Long id);

    /** Retorna apenas funcionários com {@code ativo=true}. */
    Optional<Funcionario> findAtivoById(Long id);

    List<Funcionario> findAllAtivos();

    void deleteById(Long id);
}
