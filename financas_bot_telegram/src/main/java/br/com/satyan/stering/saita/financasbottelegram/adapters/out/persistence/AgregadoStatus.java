package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import java.math.BigDecimal;

public record AgregadoStatus(StatusPedido status, long quantidade, BigDecimal total) {}
