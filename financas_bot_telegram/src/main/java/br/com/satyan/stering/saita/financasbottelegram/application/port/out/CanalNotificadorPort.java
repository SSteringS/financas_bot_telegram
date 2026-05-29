package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

// Porta mínima para envio de resposta ao remetente da mensagem.
// chatId usa Long (Telegram). Será redesenhada (canal-agnóstica) quando o adapter
// WhatsApp for adicionado em BE-18 — wa_id é String, não Long.
public interface CanalNotificadorPort {
    void enviar(Long chatId, String mensagem);
}
