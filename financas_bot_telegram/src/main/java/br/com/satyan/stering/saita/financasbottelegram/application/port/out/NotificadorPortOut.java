package br.com.satyan.stering.saita.financasbottelegram.application.port.out;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;

public interface NotificadorPortOut {
    Canal getCanal();
    void notificar(NotificacaoDTO notificacao);
}
