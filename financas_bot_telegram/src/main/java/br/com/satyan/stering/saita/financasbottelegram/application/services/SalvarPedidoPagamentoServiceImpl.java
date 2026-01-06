package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.BusinessRuleException;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class SalvarPedidoPagamentoServiceImpl implements SalvarPedidoPagamentoUsecase {

    private final PedidoPagamentoRepositoryPort pedidoPagamentoRepository;

    public SalvarPedidoPagamentoServiceImpl(PedidoPagamentoRepositoryPort pedidoPagamentoRepository) {
        this.pedidoPagamentoRepository = pedidoPagamentoRepository;
    }

    @Override
    public PedidoPagamento execute(PedidoPagamento pedidoPagamento, Long chatId) {
        validate(pedidoPagamento, chatId);
        return pedidoPagamentoRepository.save(pedidoPagamento);
    }

    private void validate(PedidoPagamento pedidoPagamento, Long chatId) {
        validateValor(pedidoPagamento, chatId);
        validateDescricao(pedidoPagamento, chatId);
        validateTelegramUserId(pedidoPagamento, chatId);
    }

    private void validateValor(PedidoPagamento pedidoPagamento, Long chatId) {
        if (pedidoPagamento.getValor() == null || pedidoPagamento.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("O valor do pedido deve ser maior que zero.", chatId);
        }
    }

    private void validateDescricao(PedidoPagamento pedidoPagamento, Long chatId) {
        if (pedidoPagamento.getDescricao() == null || pedidoPagamento.getDescricao().isBlank()) {
            throw new BusinessRuleException("A descrição do pedido não pode estar em branco.", chatId);
        }
    }

    private void validateTelegramUserId(PedidoPagamento pedidoPagamento, Long chatId) {
        if (pedidoPagamento.getTelegramUserId() == null || pedidoPagamento.getTelegramUserId().isBlank()) {
            throw new BusinessRuleException("Ocorreu um erro ao identificar seu usuário. Tente novamente.", chatId);
        }
    }
}

