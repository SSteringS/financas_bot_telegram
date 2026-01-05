package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.telegram.service.TelegramMessageSenderService;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import java.math.BigDecimal;
import org.hibernate.sql.Update;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProcessPaymentMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessPaymentMessageService.class);

    private final SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase;
    private final RegistrarComprovanteUsecase registrarComprovanteUsecase;
    private final TelegramMessageSenderService telegramMessageSenderService;

    private static final Pattern COMPROVANTE_PATTERN =
        Pattern.compile("#(\\d+)\\s+(.+)");
    public static final java.util.regex.Pattern PEDIDO_PAGAMENTO_PATTERN =
        Pattern.compile("^(\\d+([.,]\\d{1,2})?)\\s+(.+)$");

    public ProcessPaymentMessageService(SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase,
                                        RegistrarComprovanteUsecase registrarComprovanteUsecase, TelegramMessageSenderService telegramMessageSenderService) {
        this.salvarPedidoPagamentoUsecase = salvarPedidoPagamentoUsecase;
        this.registrarComprovanteUsecase = registrarComprovanteUsecase;
        this.telegramMessageSenderService = telegramMessageSenderService;
    }

    public void process(String payload) {
        JSONObject messageJson = new JSONObject(payload).getJSONObject("message");

        String caption = messageJson.optString("caption", null);

        //todo aplicar pattern para tratar mensagens diferentes
        if (caption != null) {
            Matcher matcherComprovante = COMPROVANTE_PATTERN.matcher(caption);
            Matcher matcherPedido = PEDIDO_PAGAMENTO_PATTERN.matcher(caption);
            if (matcherComprovante.matches()) {
                handleComprovante(messageJson, matcherComprovante);
                return;
            } else if(matcherPedido.matches()) {
                handleNovoPedido(messageJson, matcherPedido);
                return;
            }
        }
    }

    private void handleNovoPedido(JSONObject messageJson, Matcher matcher) {
        Long chatId = messageJson.getJSONObject("chat").getLong("id");

        try{
            String telegramUserId = messageJson.getJSONObject("from").get("id").toString();
            String telegramMessageId = messageJson.get("message_id").toString();
            String descricao = messageJson.optString("caption", null);
            BigDecimal valor = new BigDecimal(matcher.group(1).replace(",", "."));
            String Descricao = matcher.group(3);

            JSONArray photoArray = messageJson.getJSONArray("photo");
            String fileId = photoArray.getJSONObject(photoArray.length() - 1).getString("file_id");

            PedidoPagamento pedido = new PedidoPagamento();
            pedido.setTelegramUserId(telegramUserId);
            pedido.setTelegramMessageId(telegramMessageId);
            pedido.setFileIdTelegram(fileId);
            pedido.setDescricao(descricao);
            pedido.setStatus(StatusPedido.PENDENTE);
            pedido.setValor(valor);

            PedidoPagamento pedidoSalvo = salvarPedidoPagamentoUsecase.execute(pedido);

            if(pedidoSalvo != null && pedidoSalvo.getId() != null) {
                String confirmationMessage = String.format("Pedido de pagamento registrado com sucesso! ID do pedido: %d", pedidoSalvo.getId());
                telegramMessageSenderService.sendMessage(chatId, confirmationMessage);
                logger.info("Mensagem de confirmação enviada para o pedido ID {}.", pedidoSalvo.getId());
            } else {
                logger.error("Falha ao salvar o pedido, o objeto retornado ou seu ID é nulo.");
                telegramMessageSenderService.sendMessage(chatId, "❌ Ocorreu um erro ao salvar seu pedido. Tente novamente.");
            }
        } catch (Exception e) {
            logger.error("Erro ao processar novo pedido.", e);
            telegramMessageSenderService.sendMessage(chatId, "❌ Erro ao processar sua mensagem. Verifique o formato.");
        }


    }

    private void handleComprovante(JSONObject messageJson, Matcher matcher) {
        Long chatId = messageJson.getJSONObject("chat").getLong("id");
        try {
            Long pedidoId = Long.parseLong(matcher.group(1));
            String tipoPagamento = matcher.group(2);

            JSONArray photoArray = messageJson.getJSONArray("photo");
            String fileId = photoArray.getJSONObject(photoArray.length() - 1).getString("file_id");

            registrarComprovanteUsecase.execute(pedidoId, tipoPagamento, fileId);

            // Opcional: Enviar confirmação de recebimento do comprovante
            String confirmaMensagem = String.format("🧾 Comprovante para o pedido %d recebido!", pedidoId);
            telegramMessageSenderService.sendMessage(chatId, confirmaMensagem);
        } catch (Exception e) {
            logger.error("Erro ao processar comprovante.", e);
            telegramMessageSenderService.sendMessage(chatId, "❌ Erro ao processar seu comprovante.");
        }
    }
}
