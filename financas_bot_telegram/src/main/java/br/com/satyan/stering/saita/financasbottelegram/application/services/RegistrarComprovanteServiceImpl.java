package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.BusinessRuleException;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.DatabaseException;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.ComprovanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.event.ComprovanteRegistradoEvent;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarComprovanteServiceImpl implements RegistrarComprovanteUsecase {

    private final ComprovanteRepositoryPort comprovanteRepository;
    private final PedidoPagamentoRepositoryPort pedidoPagamentoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RegistrarComprovanteServiceImpl(
        ComprovanteRepositoryPort comprovanteRepository,
        PedidoPagamentoRepositoryPort pedidoPagamentoRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.comprovanteRepository = comprovanteRepository;
        this.pedidoPagamentoRepository = pedidoPagamentoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Comprovante execute(Long pedidoId, String tipoPagamento, String fileIdTelegram, String imagemUrl, TipoArquivo tipoArquivo, Long chatId) {
        try {
            PedidoPagamento pedido = pedidoPagamentoRepository.findById(pedidoId)
                    .orElseThrow(() -> new PedidoNaoEncontradoException("Pedido com ID #" + pedidoId + " não encontrado.", chatId));

            validarStatusDoPedido(pedido, chatId);

            pedido.setStatus(StatusPedido.PAGO);
            pedidoPagamentoRepository.save(pedido);

            Comprovante comprovante = Comprovante.builder()
                    .pedidoId(pedidoId)
                    .tipoPagamento(tipoPagamento)
                    .fileIdTelegram(fileIdTelegram)
                    .imagemUrl(imagemUrl)
                    .tipoArquivo(tipoArquivo != null ? tipoArquivo : TipoArquivo.IMAGEM)
                    .build();

            Comprovante salvo = comprovanteRepository.save(comprovante);

            eventPublisher.publishEvent(new ComprovanteRegistradoEvent(
                salvo.getId(),
                pedidoId,
                pedido.getRequisitanteId(),
                chatId != null ? chatId.toString() : ""
            ));

            return salvo;
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
