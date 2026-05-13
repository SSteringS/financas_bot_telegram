package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exceptionhandler;

import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.BusinessRuleException;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.DatabaseException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidCaptionException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidMessageFormatException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.InvalidUpdateException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.exception.PhotoProcessingException;
import br.com.satyan.stering.saita.financasbottelegram.application.exceptions.UnauthorizedUserException;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.domain.exceptions.PedidoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestControllerAdvice(basePackages = "br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram")
public class GlobalTelegramExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalTelegramExceptionHandler.class);

    private final TelegramMessageSenderService telegramMessageSenderService;

    public GlobalTelegramExceptionHandler(TelegramMessageSenderService telegramMessageSenderService) {
        this.telegramMessageSenderService = telegramMessageSenderService;
    }

    @ExceptionHandler(UnauthorizedUserException.class)
    public ResponseEntity<Void> handleUnauthorizedUser(UnauthorizedUserException ex, WebRequest request) {
        logger.warn("Usuário não autorizado tentou acessar o bot. Chat ID: {}", ex.getChatId());
        telegramMessageSenderService.sendMessage(ex.getChatId(), "🚫 Você não tem permissão para usar este bot.");
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(InvalidUpdateException.class)
    public ResponseEntity<Void> handleInvalidUpdate(InvalidUpdateException ex, WebRequest request) {
        logger.warn("Update inválido recebido: {}", ex.getMessage());
        // Não podemos responder ao usuário pois provavelmente não temos o chat_id
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(InvalidMessageFormatException.class)
    public ResponseEntity<Void> handleInvalidMessageFormat(InvalidMessageFormatException ex, WebRequest request) {
        logger.warn("Formato de mensagem inválido recebido do Chat ID {}: {}", ex.getChatId(), ex.getMessage());
        telegramMessageSenderService.sendMessage(ex.getChatId(), "🚫 Formato de mensagem inválido. " + ex.getMessage());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<Void> handlePedidoNaoEncontrado(PedidoNaoEncontradoException ex, WebRequest request) {
        logger.warn("Tentativa de registrar comprovante para pedido inexistente. Chat ID {}: {}", ex.getChatId(), ex.getMessage());
        telegramMessageSenderService.sendMessage(ex.getChatId(), "⚠️ " + ex.getMessage());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(PhotoProcessingException.class)
    public ResponseEntity<Void> handlePhotoProcessing(PhotoProcessingException ex, WebRequest request) {
        logger.error("Erro ao processar foto para o Chat ID {}: {}", ex.getChatId(), ex.getMessage());
        telegramMessageSenderService.sendMessage(ex.getChatId(), "⚠️ " + ex.getMessage());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(InvalidCaptionException.class)
    public ResponseEntity<Void> handleInvalidCaption(InvalidCaptionException ex, WebRequest request) {
        logger.warn("Legenda inválida recebida do Chat ID {}: {}", ex.getChatId(), ex.getMessage());
        telegramMessageSenderService.sendMessage(ex.getChatId(), "❌ " + ex.getMessage());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Void> handleBusinessRuleException(BusinessRuleException ex, WebRequest request) {
        logger.warn("Violação de regra de negócio para o Chat ID {}: {}", ex.getChatId(), ex.getMessage());
        telegramMessageSenderService.sendMessage(ex.getChatId(), "⚠️ " + ex.getMessage());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<Void> handleDatabaseException(DatabaseException ex, WebRequest request) {
        logger.error("Erro de banco de dados para o Chat ID {}: {}", ex.getChatId(), ex.getMessage(), ex.getCause());
        telegramMessageSenderService.sendMessage(ex.getChatId(), "🚨 Ocorreu um erro interno ao acessar o banco de dados. A equipe de desenvolvimento já foi notificada.");
        return ResponseEntity.status(500).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleAnyOther(Exception e, HttpServletRequest request) {
        logger.error("Exceção não mapeada no processamento de Update do Telegram", e);

        Long chatId = extrairChatIdSeguramente(request);
        if (chatId != null) {
            try {
                telegramMessageSenderService.sendMessage(chatId,
                        "❌ Houve um problema processando sua mensagem. " +
                        "Tente novamente em alguns instantes ou avise o admin.");
            } catch (Exception sendErr) {
                logger.error("Falha ao enviar mensagem amigável após erro original", sendErr);
            }
        } else {
            logger.warn("Não consegui extrair chatId do request — usuário não receberá feedback");
        }

        return ResponseEntity.ok().build();
    }

    private Long extrairChatIdSeguramente(HttpServletRequest request) {
        Object attr = request.getAttribute("__update");
        if (attr instanceof Update update && update.getMessage() != null) {
            return update.getMessage().getChatId();
        }
        return null;
    }
}
