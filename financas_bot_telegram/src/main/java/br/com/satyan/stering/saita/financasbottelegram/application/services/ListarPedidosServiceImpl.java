package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.ComprovanteJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoPagamentoJpaRepository;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.PedidoSpecs;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.ListarPedidosFiltro;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaginaDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.dto.PedidoResumoDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.ListarPedidosUseCase;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ListarPedidosServiceImpl implements ListarPedidosUseCase {

    private final PedidoPagamentoJpaRepository jpaRepository;
    private final ComprovanteJpaRepository comprovanteRepo;

    public ListarPedidosServiceImpl(
            PedidoPagamentoJpaRepository jpaRepository,
            ComprovanteJpaRepository comprovanteRepo) {
        this.jpaRepository = jpaRepository;
        this.comprovanteRepo = comprovanteRepo;
    }

    @Override
    public PaginaDTO<PedidoResumoDTO> listar(ListarPedidosFiltro filtro, Long requisitanteId) {
        int tamanho = Math.min(
                filtro.tamanho() <= 0 ? ListarPedidosFiltro.TAMANHO_DEFAULT : filtro.tamanho(),
                ListarPedidosFiltro.TAMANHO_MAX);
        int page = Math.max(filtro.page(), 0);

        Specification<PedidoPagamentoEntity> spec = Specification
                .where(PedidoSpecs.doRequisitante(requisitanteId))
                .and(PedidoSpecs.comStatus(filtro.status()))
                .and(PedidoSpecs.comTipos(filtro.tipos()))
                .and(PedidoSpecs.dataPedidoDesde(filtro.de()))
                .and(PedidoSpecs.dataPedidoAte(filtro.ate()))
                .and(PedidoSpecs.comBusca(filtro.busca()));

        Pageable pageable = PageRequest.of(page, tamanho,
                Sort.by(Sort.Direction.DESC, "dataPedido")
                        .and(Sort.by(Sort.Direction.DESC, "id")));

        Page<PedidoPagamentoEntity> pageResult = jpaRepository.findAll(spec, pageable);

        List<Long> ids = pageResult.getContent().stream()
                .map(PedidoPagamentoEntity::getId)
                .toList();
        Set<Long> comComprovante = ids.isEmpty() ? Set.of() : comprovanteRepo.findPedidoIdsByPedidoIdIn(ids);

        List<PedidoResumoDTO> items = pageResult.getContent().stream()
                .map(e -> toResumoDTO(e, comComprovante.contains(e.getId())))
                .toList();

        return new PaginaDTO<>(items, pageResult.getTotalElements(), page, tamanho, pageResult.getTotalPages());
    }

    private PedidoResumoDTO toResumoDTO(PedidoPagamentoEntity e, boolean temComprovante) {
        return new PedidoResumoDTO(
                e.getId(), e.getValor(), e.getDescricao(), e.getTipo(), e.getStatus(),
                e.getDataPedido(), e.getDataPagamento(), temComprovante);
    }
}
