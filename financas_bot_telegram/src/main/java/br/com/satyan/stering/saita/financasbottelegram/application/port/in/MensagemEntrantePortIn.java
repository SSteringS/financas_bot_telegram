package br.com.satyan.stering.saita.financasbottelegram.application.port.in;

import br.com.satyan.stering.saita.financasbottelegram.application.dto.PaymentMessageDTO;

public interface MensagemEntrantePortIn {
    void processar(PaymentMessageDTO dto);
}
