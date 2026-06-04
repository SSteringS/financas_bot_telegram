package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import java.util.List;
import java.util.Optional;

/**
 * Port de saída para persistência de {@link Adiantamento}.
 *
 * <p>Interface pura — sem anotações Spring. Implementada pelo adapter JPA
 * {@code AdiantamentoRepositoryAdapter}.
 */
public interface AdiantamentoRepositoryPortOut {

    Adiantamento save(Adiantamento adiantamento);

    Optional<Adiantamento> findById(Long id);

    /** Retorna adiantamentos com {@code ativo=true} para o funcionário informado. */
    List<Adiantamento> findAtivosParaFuncionario(Long funcionarioId);

    /**
     * Retorna adiantamentos ativos com parcelas a pagar no mês de referência.
     * Usado pelo {@code FecharMesUseCase} para calcular descontos.
     */
    List<Adiantamento> findAtivosParaFechamento(Long funcionarioId);
}
