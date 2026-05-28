package br.com.satyan.stering.saita.financasbottelegram.application.strategy;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;

public interface MensagemProcessingStrategy {
    boolean supports(PaymentMessageDTO dto);
    void process(PaymentMessageDTO dto);
}
