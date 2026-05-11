package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PedidoPagamentoMapperTest {

    private final PedidoPagamentoMapper mapper = new PedidoPagamentoMapper();

    @Test
    void deveMappearEntityParaDomain() {
        PedidoPagamentoEntity entity = new PedidoPagamentoEntity();
        entity.setId(1L);
        entity.setRequisitanteId(1L);
        entity.setTelegramUserId("user123");
        entity.setTelegramMessageId("msg456");
        entity.setFileIdTelegram("file789");
        entity.setImagemUrl("https://s3.example.com/img.jpg");
        entity.setValor(new BigDecimal("150.00"));
        entity.setDescricao("Almoço");
        entity.setStatus(StatusPedido.PENDENTE);
        entity.setTipo(TipoPagamento.PIX);
        entity.setDataPedido(LocalDate.of(2026, 5, 11));
        entity.setDataPagamento(LocalDate.of(2026, 5, 11));
        entity.setDataCriacao(LocalDateTime.of(2026, 5, 11, 12, 0));

        PedidoPagamento domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getRequisitanteId()).isEqualTo(1L);
        assertThat(domain.getTelegramUserId()).isEqualTo("user123");
        assertThat(domain.getTelegramMessageId()).isEqualTo("msg456");
        assertThat(domain.getFileIdTelegram()).isEqualTo("file789");
        assertThat(domain.getImagemUrl()).isEqualTo("https://s3.example.com/img.jpg");
        assertThat(domain.getValor()).isEqualByComparingTo("150.00");
        assertThat(domain.getDescricao()).isEqualTo("Almoço");
        assertThat(domain.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(domain.getTipo()).isEqualTo(TipoPagamento.PIX);
        assertThat(domain.getDataPedido()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(domain.getDataPagamento()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(domain.getDataCriacao()).isEqualTo(LocalDateTime.of(2026, 5, 11, 12, 0));
    }

    @Test
    void deveMappearDomainParaEntity() {
        PedidoPagamento domain = PedidoPagamento.builder()
                .id(2L)
                .requisitanteId(1L)
                .telegramUserId("u1")
                .telegramMessageId("m1")
                .fileIdTelegram("f1")
                .imagemUrl("https://s3.example.com/x.jpg")
                .valor(new BigDecimal("99.90"))
                .descricao("Jantar")
                .status(StatusPedido.PAGO)
                .tipo(TipoPagamento.TED)
                .dataPedido(LocalDate.of(2026, 5, 11))
                .build();

        PedidoPagamentoEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getRequisitanteId()).isEqualTo(1L);
        assertThat(entity.getTelegramUserId()).isEqualTo("u1");
        assertThat(entity.getValor()).isEqualByComparingTo("99.90");
        assertThat(entity.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(entity.getDescricao()).isEqualTo("Jantar");
        assertThat(entity.getTipo()).isEqualTo(TipoPagamento.TED);
        assertThat(entity.getDataPedido()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(entity.getDataPagamento()).isNull();
    }

    @Test
    void deveRetornarNullParaEntityNula() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void deveRetornarNullParaDomainNulo() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void devePreservarRoundTrip() {
        PedidoPagamento original = PedidoPagamento.builder()
                .id(5L)
                .requisitanteId(1L)
                .valor(new BigDecimal("50.00"))
                .descricao("Café")
                .telegramUserId("xyz")
                .status(StatusPedido.PENDENTE)
                .tipo(TipoPagamento.PIX)
                .dataPedido(LocalDate.of(2026, 5, 11))
                .build();

        PedidoPagamento roundTrip = mapper.toDomain(mapper.toEntity(original));

        assertThat(roundTrip.getId()).isEqualTo(original.getId());
        assertThat(roundTrip.getRequisitanteId()).isEqualTo(original.getRequisitanteId());
        assertThat(roundTrip.getValor()).isEqualByComparingTo(original.getValor());
        assertThat(roundTrip.getDescricao()).isEqualTo(original.getDescricao());
        assertThat(roundTrip.getTelegramUserId()).isEqualTo(original.getTelegramUserId());
        assertThat(roundTrip.getStatus()).isEqualTo(original.getStatus());
        assertThat(roundTrip.getTipo()).isEqualTo(original.getTipo());
        assertThat(roundTrip.getDataPedido()).isEqualTo(original.getDataPedido());
    }
}
