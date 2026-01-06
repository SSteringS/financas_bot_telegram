package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Comprovante;

public interface RegistrarComprovanteUsecase {
    Comprovante execute(Long pedidoId, String tipoPagamento, String fileIdTelegram, Long chatId);
}

