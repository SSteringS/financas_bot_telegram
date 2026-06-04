package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.AtualizarFuncionarioPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.FuncionarioNaoEncontradoException;
import org.springframework.stereotype.Service;

/**
 * Implementação do use case de atualização de funcionário.
 *
 * <p>Atualiza apenas campos mutáveis: salário, dados bancários, observações.
 * Dados de identidade (nome, forma de pagamento) também podem ser atualizados.
 * Valida as regras PIX/TED após a atualização.
 */
@Service
public class AtualizarFuncionarioServiceImpl implements AtualizarFuncionarioPortIn {

    private final FuncionarioRepositoryPortOut repository;

    public AtualizarFuncionarioServiceImpl(FuncionarioRepositoryPortOut repository) {
        this.repository = repository;
    }

    @Override
    public Funcionario atualizar(Long id, Funcionario dadosAtualizados) {
        Funcionario existente = repository.findById(id)
                .orElseThrow(() -> new FuncionarioNaoEncontradoException(id));

        // Atualiza campos fornecidos (null = não alterar)
        if (dadosAtualizados.getNome() != null) {
            existente.setNome(dadosAtualizados.getNome());
        }
        if (dadosAtualizados.getSalarioBase() != null) {
            existente.setSalarioBase(dadosAtualizados.getSalarioBase());
        }
        if (dadosAtualizados.getFormaPagamento() != null) {
            existente.setFormaPagamento(dadosAtualizados.getFormaPagamento());
        }
        if (dadosAtualizados.getChavePix() != null) {
            existente.setChavePix(dadosAtualizados.getChavePix());
        }
        if (dadosAtualizados.getBanco() != null) {
            existente.setBanco(dadosAtualizados.getBanco());
        }
        if (dadosAtualizados.getAgencia() != null) {
            existente.setAgencia(dadosAtualizados.getAgencia());
        }
        if (dadosAtualizados.getConta() != null) {
            existente.setConta(dadosAtualizados.getConta());
        }
        if (dadosAtualizados.getTipoConta() != null) {
            existente.setTipoConta(dadosAtualizados.getTipoConta());
        }
        if (dadosAtualizados.getContaPropria() != null) {
            existente.setContaPropria(dadosAtualizados.getContaPropria());
        }
        if (dadosAtualizados.getObsPagamento() != null) {
            existente.setObsPagamento(dadosAtualizados.getObsPagamento());
        }
        if (dadosAtualizados.getDiaPagamentoReferencia() != null) {
            existente.setDiaPagamentoReferencia(dadosAtualizados.getDiaPagamentoReferencia());
        }

        // Revalida dados bancários após atualização
        CadastrarFuncionarioServiceImpl.validarDadosPagamento(existente);

        return repository.save(existente);
    }
}
