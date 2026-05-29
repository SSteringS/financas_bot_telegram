package br.com.satyan.stering.saita.financasbottelegram.domain.event;

// Evento publicado após salvar comprovante. Mantido pequeno e estável para sobreviver
// a futuras migrações para outbox/broker (ADR 0014 §evolução).
// chatId: identificador de canal do destinatário (chatId Telegram; wa_id para WhatsApp futuro).
public record ComprovanteRegistradoEvent(
    Long comprovanteId,
    Long pedidoId,
    Long requisitanteId,
    String chatId
) {}
