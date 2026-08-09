package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMessageDTO {
    /** Canal de origem — preenchido pelo mapper de cada canal. */
    private Canal canal;

    /** ID externo da mensagem (message_id do Telegram; wamid do WhatsApp) — base para idempotência. */
    private String externalId;

    /** Destino para envio de respostas. Long = chatId Telegram. Para WhatsApp, este campo será repensado em BE-18. */
    private Long chatId;

    /** Identificador do remetente (userId Telegram; waId WhatsApp). */
    private String fromId;

    /** Texto da legenda (photo/document) ou corpo da mensagem (text). Null se não houver. */
    private String caption;

    /** Bytes do arquivo já baixados pelo mapper do canal. Null para mensagens texto-puro. */
    private byte[] fileBytes;

    /** Extensão do arquivo (jpg, pdf, png…). Null para mensagens texto-puro. */
    private String fileExtension;

    /** Tipo do arquivo (IMAGEM ou PDF). Null para mensagens texto-puro. */
    private TipoArquivo tipoArquivo;

    /** Referência do arquivo no canal de origem (fileId Telegram; media_id WhatsApp) — persiste no banco. */
    private String mediaId;
}
