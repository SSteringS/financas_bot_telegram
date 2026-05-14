package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoPagamentoJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.StorageService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ObterUrlImagemPedidoUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.ImagemNaoEncontradaException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoAutorizadoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class ObterUrlImagemPedidoServiceImpl implements ObterUrlImagemPedidoUseCase {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final PedidoPagamentoJpaRepository pedidoRepo;
    private final StorageService storage;

    public ObterUrlImagemPedidoServiceImpl(PedidoPagamentoJpaRepository pedidoRepo,
                                            StorageService storage) {
        this.pedidoRepo = pedidoRepo;
        this.storage = storage;
    }

    @Override
    public String obter(Long pedidoId, Long requisitanteId) {
        PedidoPagamentoEntity p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));

        if (!p.getRequisitanteId().equals(requisitanteId)) {
            throw new PedidoNaoAutorizadoException(pedidoId, requisitanteId);
        }

        if (p.getImagemUrl() == null || p.getImagemUrl().isBlank()) {
            throw new ImagemNaoEncontradaException("Pedido " + pedidoId + " não tem imagem associada");
        }

        return storage.gerarUrlTemporariaParaLeitura(p.getImagemUrl(), TTL);
    }
}
