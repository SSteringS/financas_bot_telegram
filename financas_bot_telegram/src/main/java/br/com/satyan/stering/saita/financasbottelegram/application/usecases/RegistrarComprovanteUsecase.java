package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoArquivo;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Comprovante;

public interface RegistrarComprovanteUsecase {
    Comprovante execute(Long pedidoId, String tipoPagamento, String fileIdTelegram, String imagemUrl, TipoArquivo tipoArquivo, Long chatId);
}
