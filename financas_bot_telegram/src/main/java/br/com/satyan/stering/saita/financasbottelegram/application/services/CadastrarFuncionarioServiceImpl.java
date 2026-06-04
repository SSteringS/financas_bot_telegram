package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.CadastrarFuncionarioPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.vo.FormaPagamento;
import org.springframework.stereotype.Service;

/**
 * Implementação do use case de cadastro de funcionário.
 *
 * <p>Valida regras condicionais PIX/TED antes de persistir. Depende apenas de
 * {@link FuncionarioRepositoryPortOut} (DIP) — não conhece JPA diretamente.
 */
@Service
public class CadastrarFuncionarioServiceImpl implements CadastrarFuncionarioPortIn {

    private final FuncionarioRepositoryPortOut repository;

    public CadastrarFuncionarioServiceImpl(FuncionarioRepositoryPortOut repository) {
        this.repository = repository;
    }

    @Override
    public Funcionario cadastrar(Funcionario funcionario) {
        validarDadosPagamento(funcionario);

        // Defaults para campos opcionais
        if (funcionario.getContaPropria() == null) {
            funcionario.setContaPropria(true);
        }
        if (funcionario.getAtivo() == null) {
            funcionario.setAtivo(true);
        }

        return repository.save(funcionario);
    }

    /**
     * Valida campos obrigatórios condicionais à forma de pagamento.
     *
     * <p>PIX → {@code chavePix} obrigatório.
     * TED → {@code banco}, {@code agencia}, {@code conta}, {@code tipoConta} obrigatórios.
     */
    static void validarDadosPagamento(Funcionario f) {
        if (f.getFormaPagamento() == FormaPagamento.PIX) {
            if (f.getChavePix() == null || f.getChavePix().isBlank()) {
                throw new IllegalArgumentException("chave_pix é obrigatória para forma de pagamento PIX");
            }
        } else if (f.getFormaPagamento() == FormaPagamento.TED) {
            if (f.getBanco() == null || f.getBanco().isBlank()) {
                throw new IllegalArgumentException("banco é obrigatório para forma de pagamento TED");
            }
            if (f.getAgencia() == null || f.getAgencia().isBlank()) {
                throw new IllegalArgumentException("agencia é obrigatória para forma de pagamento TED");
            }
            if (f.getConta() == null || f.getConta().isBlank()) {
                throw new IllegalArgumentException("conta é obrigatória para forma de pagamento TED");
            }
            if (f.getTipoConta() == null || f.getTipoConta().isBlank()) {
                throw new IllegalArgumentException("tipo_conta é obrigatório para forma de pagamento TED");
            }
        }
    }
}
