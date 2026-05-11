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
                .telegramUserId(entity.getTelegramUserId())
                .telegramMessageId(entity.getTelegramMessageId())
                .fileIdTelegram(entity.getFileIdTelegram())
                .imagemUrl(entity.getImagemUrl())
                .valor(entity.getValor())
                .descricao(entity.getDescricao())
                .status(entity.getStatus())
                .dataCriacao(entity.getDataCriacao())
                .build();
    }

    public PedidoPagamentoEntity toEntity(PedidoPagamento domain) {
        if (domain == null) return null;
        PedidoPagamentoEntity entity = new PedidoPagamentoEntity();
        entity.setId(domain.getId());
        entity.setTelegramUserId(domain.getTelegramUserId());
        entity.setTelegramMessageId(domain.getTelegramMessageId());
        entity.setFileIdTelegram(domain.getFileIdTelegram());
        entity.setImagemUrl(domain.getImagemUrl());
        entity.setValor(domain.getValor());
        entity.setDescricao(domain.getDescricao());
        entity.setStatus(domain.getStatus());
        entity.setDataCriacao(domain.getDataCriacao());
        return entity;
    }
}
