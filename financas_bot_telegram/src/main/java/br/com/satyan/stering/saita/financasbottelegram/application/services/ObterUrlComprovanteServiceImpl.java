package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.ComprovanteJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoPagamentoJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.StorageService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ObterUrlComprovanteUseCase;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.ComprovanteNaoEncontradoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoAutorizadoException;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class ObterUrlComprovanteServiceImpl implements ObterUrlComprovanteUseCase {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final PedidoPagamentoJpaRepository pedidoRepo;
    private final ComprovanteJpaRepository comprovanteRepo;
    private final StorageService storage;

    public ObterUrlComprovanteServiceImpl(PedidoPagamentoJpaRepository pedidoRepo,
                                           ComprovanteJpaRepository comprovanteRepo,
                                           StorageService storage) {
        this.pedidoRepo = pedidoRepo;
        this.comprovanteRepo = comprovanteRepo;
        this.storage = storage;
    }

    @Override
    public String obter(Long pedidoId, Long requisitanteId) {
        PedidoPagamentoEntity p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));

        if (!p.getRequisitanteId().equals(requisitanteId)) {
            throw new PedidoNaoAutorizadoException(pedidoId, requisitanteId);
        }

        ComprovanteEntity c = comprovanteRepo.findFirstByPedidoIdOrderByIdDesc(pedidoId)
                .orElseThrow(() -> new ComprovanteNaoEncontradoException(pedidoId));

        return storage.gerarUrlTemporariaParaLeitura(c.getImagemUrl(), TTL);
    }
}
