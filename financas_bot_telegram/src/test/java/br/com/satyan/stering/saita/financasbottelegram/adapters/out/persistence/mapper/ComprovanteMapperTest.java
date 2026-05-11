package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ComprovanteMapperTest {

    private final ComprovanteMapper mapper = new ComprovanteMapper();

    @Test
    void deveMappearEntityComPedidoParaDomain() {
        PedidoPagamentoEntity pedidoEntity = new PedidoPagamentoEntity();
        pedidoEntity.setId(10L);

        ComprovanteEntity entity = new ComprovanteEntity();
        entity.setId(1L);
        entity.setPedido(pedidoEntity);
        entity.setFileIdTelegram("file_001");
        entity.setImagemUrl("https://s3.example.com/comp.jpg");
        entity.setTipoPagamento("PIX");
        entity.setDataPagamento(LocalDateTime.of(2026, 5, 10, 15, 30));

        Comprovante domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getPedidoId()).isEqualTo(10L);
        assertThat(domain.getFileIdTelegram()).isEqualTo("file_001");
        assertThat(domain.getImagemUrl()).isEqualTo("https://s3.example.com/comp.jpg");
        assertThat(domain.getTipoPagamento()).isEqualTo("PIX");
        assertThat(domain.getDataPagamento()).isEqualTo(LocalDateTime.of(2026, 5, 10, 15, 30));
    }

    @Test
    void deveMappearDomainParaEntityComPedidoEntity() {
        Comprovante domain = Comprovante.builder()
                .id(2L)
                .pedidoId(20L)
                .fileIdTelegram("file_002")
                .imagemUrl("https://s3.example.com/x.jpg")
                .tipoPagamento("TED")
                .build();

        PedidoPagamentoEntity pedidoEntity = new PedidoPagamentoEntity();
        pedidoEntity.setId(20L);

        ComprovanteEntity entity = mapper.toEntity(domain, pedidoEntity);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getPedido()).isSameAs(pedidoEntity);
        assertThat(entity.getFileIdTelegram()).isEqualTo("file_002");
        assertThat(entity.getTipoPagamento()).isEqualTo("TED");
    }

    @Test
    void deveTratarEntityComPedidoNuloSemNpe() {
        ComprovanteEntity entity = new ComprovanteEntity();
        entity.setId(3L);
        entity.setPedido(null);
        entity.setTipoPagamento("PIX");

        Comprovante domain = mapper.toDomain(entity);

        assertThat(domain.getPedidoId()).isNull();
    }

    @Test
    void deveRetornarNullParaEntityNula() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void deveRetornarNullParaDomainNulo() {
        assertThat(mapper.toEntity(null, new PedidoPagamentoEntity())).isNull();
    }
}
