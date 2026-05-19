package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import org.springframework.stereotype.Component;

@Component
public class ComprovanteMapper {

    public Comprovante toDomain(ComprovanteEntity entity) {
        if (entity == null) return null;
        return Comprovante.builder()
                .id(entity.getId())
                .pedidoId(entity.getPedido() != null ? entity.getPedido().getId() : null)
                .fileIdTelegram(entity.getFileIdTelegram())
                .imagemUrl(entity.getImagemUrl())
                .tipoArquivo(entity.getTipoArquivo() != null ? entity.getTipoArquivo() : TipoArquivo.IMAGEM)
                .tipoPagamento(entity.getTipoPagamento())
                .dataPagamento(entity.getDataPagamento())
                .build();
    }

    public ComprovanteEntity toEntity(Comprovante domain, PedidoPagamentoEntity pedidoEntity) {
        if (domain == null) return null;
        ComprovanteEntity entity = new ComprovanteEntity();
        entity.setId(domain.getId());
        entity.setPedido(pedidoEntity);
        entity.setFileIdTelegram(domain.getFileIdTelegram());
        entity.setImagemUrl(domain.getImagemUrl());
        entity.setTipoArquivo(domain.getTipoArquivo() != null ? domain.getTipoArquivo() : TipoArquivo.IMAGEM);
        entity.setTipoPagamento(domain.getTipoPagamento());
        entity.setDataPagamento(domain.getDataPagamento());
        return entity;
    }
}
