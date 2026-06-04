package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.AdiantamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AdiantamentoMapperTest {

    private final AdiantamentoMapper mapper = new AdiantamentoMapper();

    @Test
    void deveMapearEntityParaDomain() {
        AdiantamentoEntity entity = new AdiantamentoEntity();
        entity.setId(1L);
        entity.setFuncionarioId(10L);
        entity.setDescricao("Adiantamento de emergência");
        entity.setValorTotal(new BigDecimal("600.00"));
        entity.setValorParcela(new BigDecimal("200.00"));
        entity.setNumParcelas(3);
        entity.setParcelasPagas(1);
        entity.setDataInicio(LocalDate.of(2026, 1, 1));
        entity.setAtivo(true);
        entity.setCriadoEm(LocalDateTime.of(2026, 1, 1, 9, 0));

        Adiantamento domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getFuncionarioId()).isEqualTo(10L);
        assertThat(domain.getDescricao()).isEqualTo("Adiantamento de emergência");
        assertThat(domain.getValorTotal()).isEqualByComparingTo("600.00");
        assertThat(domain.getValorParcela()).isEqualByComparingTo("200.00");
        assertThat(domain.getNumParcelas()).isEqualTo(3);
        assertThat(domain.getParcelasPagas()).isEqualTo(1);
        assertThat(domain.getDataInicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(domain.getAtivo()).isTrue();
        assertThat(domain.getCriadoEm()).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 0));
    }

    @Test
    void deveMapearDomainParaEntity() {
        Adiantamento domain = Adiantamento.builder()
                .id(2L)
                .funcionarioId(20L)
                .descricao("Empréstimo")
                .valorTotal(new BigDecimal("1000.00"))
                .valorParcela(new BigDecimal("500.00"))
                .numParcelas(2)
                .parcelasPagas(0)
                .dataInicio(LocalDate.of(2026, 3, 1))
                .ativo(true)
                .build();

        AdiantamentoEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getFuncionarioId()).isEqualTo(20L);
        assertThat(entity.getDescricao()).isEqualTo("Empréstimo");
        assertThat(entity.getValorTotal()).isEqualByComparingTo("1000.00");
        assertThat(entity.getValorParcela()).isEqualByComparingTo("500.00");
        assertThat(entity.getNumParcelas()).isEqualTo(2);
        assertThat(entity.getParcelasPagas()).isEqualTo(0);
        assertThat(entity.getAtivo()).isTrue();
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
        Adiantamento original = Adiantamento.builder()
                .id(5L)
                .funcionarioId(15L)
                .descricao("Material escolar")
                .valorTotal(new BigDecimal("300.00"))
                .valorParcela(new BigDecimal("100.00"))
                .numParcelas(3)
                .parcelasPagas(2)
                .dataInicio(LocalDate.of(2026, 4, 1))
                .ativo(true)
                .build();

        AdiantamentoEntity entity = mapper.toEntity(original);
        Adiantamento roundTrip = mapper.toDomain(entity);

        assertThat(roundTrip.getId()).isEqualTo(original.getId());
        assertThat(roundTrip.getFuncionarioId()).isEqualTo(original.getFuncionarioId());
        assertThat(roundTrip.getDescricao()).isEqualTo(original.getDescricao());
        assertThat(roundTrip.getValorTotal()).isEqualByComparingTo(original.getValorTotal());
        assertThat(roundTrip.getValorParcela()).isEqualByComparingTo(original.getValorParcela());
        assertThat(roundTrip.getNumParcelas()).isEqualTo(original.getNumParcelas());
        assertThat(roundTrip.getParcelasPagas()).isEqualTo(original.getParcelasPagas());
        assertThat(roundTrip.getAtivo()).isEqualTo(original.getAtivo());
    }

    @Test
    void deveUsarZeroParaParcelasPagasNullaNoEntity() {
        Adiantamento domain = Adiantamento.builder()
                .funcionarioId(1L)
                .descricao("Teste")
                .valorTotal(new BigDecimal("100.00"))
                .valorParcela(new BigDecimal("100.00"))
                .numParcelas(1)
                .parcelasPagas(null) // null → deve ser 0 no entity
                .dataInicio(LocalDate.of(2026, 1, 1))
                .ativo(true)
                .build();

        AdiantamentoEntity entity = mapper.toEntity(domain);

        assertThat(entity.getParcelasPagas()).isEqualTo(0);
    }
}
