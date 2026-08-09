package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import org.springframework.stereotype.Component;

@Component
public class PedidoPagamentoMapper {

    public PedidoPagamento toDomain(PedidoPagamentoEntity entity) {
        if (entity == null) return null;
        return PedidoPagamento.builder()
                .id(entity.getId())
                .requisitanteId(entity.getRequisitanteId())
                .telegramUserId(entity.getTelegramUserId())
                .telegramMessageId(entity.getTelegramMessageId())
                .fileIdTelegram(entity.getFileIdTelegram())
                .imagemUrl(entity.getImagemUrl())
                .valor(entity.getValor())
                .descricao(entity.getDescricao())
                .status(entity.getStatus())
                .tipo(entity.getTipo())
                .dataPedido(entity.getDataPedido())
                .dataPagamento(entity.getDataPagamento())
                .dataCriacao(entity.getDataCriacao())
                // V6: folha de pagamento
                .categoria(entity.getCategoria())
                .funcionarioId(entity.getFuncionarioId())
                .fechado(entity.getFechado())
                .observacao(entity.getObservacao())
                .mesReferencia(entity.getMesReferencia())
                .build();
    }

    public PedidoPagamentoEntity toEntity(PedidoPagamento domain) {
        if (domain == null) return null;
        PedidoPagamentoEntity entity = new PedidoPagamentoEntity();
        entity.setId(domain.getId());
        entity.setRequisitanteId(domain.getRequisitanteId());
        entity.setTelegramUserId(domain.getTelegramUserId());
        entity.setTelegramMessageId(domain.getTelegramMessageId());
        entity.setFileIdTelegram(domain.getFileIdTelegram());
        entity.setImagemUrl(domain.getImagemUrl());
        entity.setValor(domain.getValor());
        entity.setDescricao(domain.getDescricao());
        entity.setStatus(domain.getStatus());
        entity.setTipo(domain.getTipo());
        entity.setDataPedido(domain.getDataPedido());
        entity.setDataPagamento(domain.getDataPagamento());
        entity.setDataCriacao(domain.getDataCriacao());
        // V6: folha de pagamento
        entity.setCategoria(domain.getCategoria());
        entity.setFuncionarioId(domain.getFuncionarioId());
        entity.setFechado(domain.getFechado() != null ? domain.getFechado() : false);
        entity.setObservacao(domain.getObservacao());
        entity.setMesReferencia(domain.getMesReferencia());
        return entity;
    }
}
