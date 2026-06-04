package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.FuncionarioEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import br.com.satyan.stering.saita.financasbottelegram.domain.vo.FormaPagamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FuncionarioMapperTest {

    private final FuncionarioMapper mapper = new FuncionarioMapper();

    @Test
    void deveMapearEntityParaDomain() {
        FuncionarioEntity entity = new FuncionarioEntity();
        entity.setId(1L);
        entity.setNome("Maria");
        entity.setSalarioBase(new BigDecimal("1800.00"));
        entity.setFormaPagamento(FormaPagamento.PIX);
        entity.setChavePix("123.456.789-00");
        entity.setContaPropria(true);
        entity.setAtivo(true);
        entity.setCriadoEm(LocalDateTime.of(2026, 1, 1, 10, 0));
        entity.setAtualizadoEm(LocalDateTime.of(2026, 5, 1, 10, 0));

        Funcionario domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getNome()).isEqualTo("Maria");
        assertThat(domain.getSalarioBase()).isEqualByComparingTo("1800.00");
        assertThat(domain.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(domain.getChavePix()).isEqualTo("123.456.789-00");
        assertThat(domain.getContaPropria()).isTrue();
        assertThat(domain.getAtivo()).isTrue();
        assertThat(domain.getCriadoEm()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(domain.getAtualizadoEm()).isEqualTo(LocalDateTime.of(2026, 5, 1, 10, 0));
    }

    @Test
    void deveMapearEntityTedParaDomain() {
        FuncionarioEntity entity = new FuncionarioEntity();
        entity.setId(2L);
        entity.setNome("João");
        entity.setSalarioBase(new BigDecimal("2200.00"));
        entity.setFormaPagamento(FormaPagamento.TED);
        entity.setBanco("001");
        entity.setAgencia("1234");
        entity.setConta("56789-0");
        entity.setTipoConta("CORRENTE");
        entity.setContaPropria(true);
        entity.setAtivo(true);

        Funcionario domain = mapper.toDomain(entity);

        assertThat(domain.getFormaPagamento()).isEqualTo(FormaPagamento.TED);
        assertThat(domain.getBanco()).isEqualTo("001");
        assertThat(domain.getAgencia()).isEqualTo("1234");
        assertThat(domain.getConta()).isEqualTo("56789-0");
        assertThat(domain.getTipoConta()).isEqualTo("CORRENTE");
    }

    @Test
    void deveMapearDomainParaEntity() {
        Funcionario domain = Funcionario.builder()
                .id(3L)
                .nome("Ana")
                .salarioBase(new BigDecimal("1500.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("ana@pix.com")
                .contaPropria(true)
                .ativo(true)
                .build();

        FuncionarioEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(3L);
        assertThat(entity.getNome()).isEqualTo("Ana");
        assertThat(entity.getSalarioBase()).isEqualByComparingTo("1500.00");
        assertThat(entity.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(entity.getChavePix()).isEqualTo("ana@pix.com");
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
    void devePreservarRoundTripPix() {
        Funcionario original = Funcionario.builder()
                .id(4L)
                .nome("Carlos")
                .salarioBase(new BigDecimal("2000.00"))
                .formaPagamento(FormaPagamento.PIX)
                .chavePix("carlos@banco.com")
                .contaPropria(true)
                .ativo(true)
                .build();

        FuncionarioEntity entity = mapper.toEntity(original);
        Funcionario roundTrip = mapper.toDomain(entity);

        assertThat(roundTrip.getId()).isEqualTo(original.getId());
        assertThat(roundTrip.getNome()).isEqualTo(original.getNome());
        assertThat(roundTrip.getSalarioBase()).isEqualByComparingTo(original.getSalarioBase());
        assertThat(roundTrip.getFormaPagamento()).isEqualTo(original.getFormaPagamento());
        assertThat(roundTrip.getChavePix()).isEqualTo(original.getChavePix());
        assertThat(roundTrip.getAtivo()).isEqualTo(original.getAtivo());
    }
}
