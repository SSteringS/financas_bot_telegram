package br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.notificador;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.NotificacaoDTO;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.NotificadorPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramNotificadorImpl implements NotificadorPortOut {

    private static final Logger logger = LoggerFactory.getLogger(TelegramNotificadorImpl.class);

    private final TelegramMessageSenderService telegramSender;

    public TelegramNotificadorImpl(TelegramMessageSenderService telegramSender) {
        this.telegramSender = telegramSender;
    }

    @Override
    public Canal getCanal() {
        return Canal.TELEGRAM;
    }

    @Override
    public void notificar(NotificacaoDTO notificacao) {
        Long chatId = Long.parseLong(notificacao.destinatarioCanalId());
        String mensagem = String.format(
            "✅ Comprovante registrado para o pedido #%d.\nVeja em %s",
            notificacao.pedidoId(),
            notificacao.linkSite()
        );
        logger.info("Notificando via Telegram chatId={} pedidoId={}", chatId, notificacao.pedidoId());
        telegramSender.sendMessage(chatId, mensagem);
    }
}
