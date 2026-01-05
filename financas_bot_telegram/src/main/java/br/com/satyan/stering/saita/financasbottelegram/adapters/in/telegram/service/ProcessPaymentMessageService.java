package br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service;

import br.com.satyan.stering.saita.financasbottelegram.application.usecases.RegistrarComprovanteUsecase;
import br.com.satyan.stering.saita.financasbottelegram.application.usecases.SalvarPedidoPagamentoUsecase;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.PedidoPagamento;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProcessPaymentMessageService {

    private final SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase;
    private final RegistrarComprovanteUsecase registrarComprovanteUsecase;
    private static final Pattern COMPROVANTE_PATTERN = Pattern.compile("#(\\d+)\\s+(.+)");

    public ProcessPaymentMessageService(SalvarPedidoPagamentoUsecase salvarPedidoPagamentoUsecase,
                                        RegistrarComprovanteUsecase registrarComprovanteUsecase) {
        this.salvarPedidoPagamentoUsecase = salvarPedidoPagamentoUsecase;
        this.registrarComprovanteUsecase = registrarComprovanteUsecase;
    }

    public void processPaymentMessage(String payload) {
        JSONObject messageJson = new JSONObject(payload).getJSONObject("message");

        String caption = messageJson.optString("caption", null);

        if (caption != null) {
            Matcher matcher = COMPROVANTE_PATTERN.matcher(caption);
            if (matcher.matches()) {
                handleComprovante(messageJson, matcher);
                return;
            }
        }

        handleNovoPedido(messageJson);
    }

    private void handleNovoPedido(JSONObject messageJson) {
        String telegramUserId = messageJson.getJSONObject("from").get("id").toString();
        String telegramMessageId = messageJson.get("message_id").toString();
        String descricao = messageJson.optString("caption", null);

        JSONArray photoArray = messageJson.getJSONArray("photo");
        String fileId = photoArray.getJSONObject(photoArray.length() - 1).getString("file_id");

        PedidoPagamento pedido = new PedidoPagamento();
        pedido.setTelegramUserId(telegramUserId);
        pedido.setTelegramMessageId(telegramMessageId);
        pedido.setFileIdTelegram(fileId);
        pedido.setDescricao(descricao);
        pedido.setStatus(StatusPedido.PENDENTE);

        salvarPedidoPagamentoUsecase.execute(pedido);
    }

    private void handleComprovante(JSONObject messageJson, Matcher matcher) {
        Long pedidoId = Long.parseLong(matcher.group(1));
        String tipoPagamento = matcher.group(2);

        JSONArray photoArray = messageJson.getJSONArray("photo");
        String fileId = photoArray.getJSONObject(photoArray.length() - 1).getString("file_id");

        registrarComprovanteUsecase.execute(pedidoId, tipoPagamento, fileId);
    }
}
