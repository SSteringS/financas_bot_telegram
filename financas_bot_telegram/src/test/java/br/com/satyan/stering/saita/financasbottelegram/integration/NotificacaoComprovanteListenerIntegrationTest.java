package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class NotificacaoComprovanteListenerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RegistrarComprovanteUsecase registrarComprovanteService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final Long REQUISITANTE_ID = 1L;
    private static final Long CHAT_ID = 7436345622L;
    private Long pedidoId;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM comprovantes");
        jdbcTemplate.update("DELETE FROM pedidos_pagamento WHERE requisitante_id = ?", REQUISITANTE_ID);

        jdbcTemplate.update(
            "INSERT INTO pedidos_pagamento (requisitante_id, telegram_user_id, valor, descricao, status, data_pedido, data_criacao)" +
            " VALUES (?, ?, 100.00, 'Teste BE-21a', 'PENDENTE', CURDATE(), NOW())",
            REQUISITANTE_ID, CHAT_ID.toString()
        );
        pedidoId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void sucesso_aposCommit_listenerDispara() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return null; })
            .when(telegramMessageSenderService).sendMessage(anyLong(), anyString());

        registrarComprovanteService.execute(pedidoId, "PIX", null, "https://url.test/img.jpg", TipoArquivo.IMAGEM, CHAT_ID);

        boolean disparou = latch.await(5, TimeUnit.SECONDS);
        assertThat(disparou).as("Listener deve disparar após o commit da transação").isTrue();
        verify(telegramMessageSenderService).sendMessage(anyLong(), anyString());
    }

    @Test
    void rollback_listenerNaoDispara() throws Exception {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        // execute() participa da transação via REQUIRED; setRollbackOnly reverte tudo
        try {
            txTemplate.execute(status -> {
                registrarComprovanteService.execute(pedidoId, "PIX", null, "url", TipoArquivo.IMAGEM, CHAT_ID);
                status.setRollbackOnly();
                return null;
            });
        } catch (Exception ignored) {
            // UnexpectedRollbackException esperada — rollback ocorreu
        }

        // Aguarda um tempo para garantir que o listener não foi invocado assincronamente
        Thread.sleep(500);

        verify(telegramMessageSenderService, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    void falhaNoListener_naoAfetaTransacaoDoUsecase() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            throw new RuntimeException("Telegram API fora do ar");
        }).when(telegramMessageSenderService).sendMessage(anyLong(), anyString());

        // execute() deve completar com sucesso mesmo que o listener falhe depois
        var comprovante = registrarComprovanteService.execute(pedidoId, "PIX", null, "url", TipoArquivo.IMAGEM, CHAT_ID);

        assertThat(comprovante).isNotNull();
        assertThat(comprovante.getId()).isNotNull();

        // Listener deve ter tentado (pode ter falhado internamente)
        latch.await(5, TimeUnit.SECONDS);
        verify(telegramMessageSenderService).sendMessage(anyLong(), anyString());
    }
}
