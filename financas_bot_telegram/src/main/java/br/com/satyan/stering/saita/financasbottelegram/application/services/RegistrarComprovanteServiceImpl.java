package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.BusinessRuleException;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.DatabaseException;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.ComprovanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Comprovante;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class RegistrarComprovanteServiceImpl implements RegistrarComprovanteUsecase {

    private final ComprovanteRepositoryPort comprovanteRepository;
    private final PedidoPagamentoRepositoryPort pedidoPagamentoRepository;

    public RegistrarComprovanteServiceImpl(ComprovanteRepositoryPort comprovanteRepository, PedidoPagamentoRepositoryPort pedidoPagamentoRepository) {
        this.comprovanteRepository = comprovanteRepository;
        this.pedidoPagamentoRepository = pedidoPagamentoRepository;
    }

    @Override
    public Comprovante execute(Long pedidoId, String tipoPagamento, String fileIdTelegram, String imagemUrl, Long chatId) {
        try {
            PedidoPagamento pedido = pedidoPagamentoRepository.findById(pedidoId)
                    .orElseThrow(() -> new PedidoNaoEncontradoException("Pedido com ID #" + pedidoId + " não encontrado.", chatId));

            validarStatusDoPedido(pedido, chatId);

            pedido.setStatus(StatusPedido.PAGO);
            pedidoPagamentoRepository.save(pedido);

            Comprovante comprovante = new Comprovante();
            comprovante.setPedido(pedido);
            comprovante.setTipoPagamento(tipoPagamento);
            comprovante.setFileIdTelegram(fileIdTelegram);
            comprovante.setImagemUrl(imagemUrl);

            return comprovanteRepository.save(comprovante);
        } catch (DataAccessException e) {
            throw new DatabaseException("Ocorreu um erro interno ao tentar salvar os dados.", chatId, e);
        }
    }

    private void validarStatusDoPedido(PedidoPagamento pedido, Long chatId) {
        if (pedido.getStatus() == StatusPedido.PAGO) {
            throw new BusinessRuleException("Este pedido já foi marcado como pago.", chatId);
        }
    }
}
