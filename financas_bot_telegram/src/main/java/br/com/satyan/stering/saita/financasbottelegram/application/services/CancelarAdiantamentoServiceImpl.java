package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CancelarAdiantamentoPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AdiantamentoRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.AdiantamentoJaQuitadoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.AdiantamentoNaoEncontradoException;
import org.springframework.stereotype.Service;

/**
 * Implementação do use case de cancelamento de adiantamento.
 *
 * <p>Soft cancel: seta {@code ativo=false}. Não permite cancelar adiantamento já quitado
 * ({@code parcelasPagas == numParcelas}).
 */
@Service
public class CancelarAdiantamentoServiceImpl implements CancelarAdiantamentoPortIn {

    private final AdiantamentoRepositoryPortOut repository;

    public CancelarAdiantamentoServiceImpl(AdiantamentoRepositoryPortOut repository) {
        this.repository = repository;
    }

    @Override
    public void cancelar(Long adiantamentoId) {
        Adiantamento adiantamento = repository.findById(adiantamentoId)
                .orElseThrow(() -> new AdiantamentoNaoEncontradoException(adiantamentoId));

        // Verificar se já está quitado
        if (adiantamento.getParcelasPagas() != null
                && adiantamento.getNumParcelas() != null
                && adiantamento.getParcelasPagas().equals(adiantamento.getNumParcelas())) {
            throw new AdiantamentoJaQuitadoException(adiantamentoId);
        }

        // Soft cancel
        adiantamento.setAtivo(false);
        repository.save(adiantamento);
    }
}
