package br.com.satyan.stering.saita.financasbottelegram.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.BusinessRuleException;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.PedidoPagamentoRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.PedidoPagamento;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalvarPedidoPagamentoServiceImplTest {

    @Mock private PedidoPagamentoRepositoryPort pedidoPagamentoRepository;
    @InjectMocks private SalvarPedidoPagamentoServiceImpl service;

    private PedidoPagamento pedidoValido() {
        return PedidoPagamento.builder()
                .valor(new BigDecimal("100.00"))
                .descricao("Almoço")
                .telegramUserId("123")
                .status(StatusPedido.PENDENTE)
                .build();
    }

    @Test
    void deveSalvarPedidoValido() {
        PedidoPagamento pedido = pedidoValido();
        PedidoPagamento salvo = PedidoPagamento.builder().id(1L).valor(pedido.getValor()).descricao(pedido.getDescricao()).build();
        when(pedidoPagamentoRepository.save(pedido)).thenReturn(salvo);

        PedidoPagamento resultado = service.execute(pedido, 999L);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(pedidoPagamentoRepository).save(pedido);
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        PedidoPagamento pedido = pedidoValido();
        pedido.setValor(null);

        assertThatThrownBy(() -> service.execute(pedido, 999L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("valor");
        verify(pedidoPagamentoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoParaValorZero() {
        PedidoPagamento pedido = pedidoValido();
        pedido.setValor(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.execute(pedido, 999L))
                .isInstanceOf(BusinessRuleException.class);
        verify(pedidoPagamentoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoParaDescricaoEmBranco() {
        PedidoPagamento pedido = pedidoValido();
        pedido.setDescricao("   ");

        assertThatThrownBy(() -> service.execute(pedido, 999L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("descrição");
        verify(pedidoPagamentoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoParaTelegramUserIdNulo() {
        PedidoPagamento pedido = pedidoValido();
        pedido.setTelegramUserId(null);

        assertThatThrownBy(() -> service.execute(pedido, 999L))
                .isInstanceOf(BusinessRuleException.class);
        verify(pedidoPagamentoRepository, never()).save(any());
    }
}
