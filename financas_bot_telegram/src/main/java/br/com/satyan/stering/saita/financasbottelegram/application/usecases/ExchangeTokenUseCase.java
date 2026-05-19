package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.Requisitante;

public interface ExchangeTokenUseCase {
    Requisitante exchange(String tokenPlain);
}
