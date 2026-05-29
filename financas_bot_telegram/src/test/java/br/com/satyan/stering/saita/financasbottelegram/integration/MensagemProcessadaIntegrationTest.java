package br.com.satyan.stering.saita.financasbottelegram.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.satyan.stering.saita.financasbottelegram.application.services.MensagemProcessadaService;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class MensagemProcessadaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MensagemProcessadaService mensagemProcessadaService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void limparTabela() {
        jdbcTemplate.execute("DELETE FROM mensagem_processada");
    }

    @Test
    void primeiraClaim_retornaTrue() {
        boolean resultado = mensagemProcessadaService.tentarClaim(Canal.WHATSAPP, "wamid.PRIMEIRO");
        assertThat(resultado).isTrue();
    }

    @Test
    void claimDuplicada_retornaFalse() {
        mensagemProcessadaService.tentarClaim(Canal.WHATSAPP, "wamid.DUP");
        boolean segundo = mensagemProcessadaService.tentarClaim(Canal.WHATSAPP, "wamid.DUP");
        assertThat(segundo).isFalse();
    }

    @Test
    void canaisDiferentes_ambosRetornamTrue() {
        boolean resultadoWa = mensagemProcessadaService.tentarClaim(Canal.WHATSAPP, "wamid.SHARED");
        boolean resultadoTg = mensagemProcessadaService.tentarClaim(Canal.TELEGRAM, "wamid.SHARED");
        assertThat(resultadoWa).isTrue();
        assertThat(resultadoTg).isTrue();
    }

    @Test
    void rollback_removeClaim_permitindoRetentativa() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        txTemplate.execute(status -> {
            mensagemProcessadaService.tentarClaim(Canal.TELEGRAM, "rollback-id");
            status.setRollbackOnly();
            return null;
        });

        // Após rollback o claim não foi persistido — nova tentativa deve ter sucesso
        boolean aposRollback = mensagemProcessadaService.tentarClaim(Canal.TELEGRAM, "rollback-id");
        assertThat(aposRollback).isTrue();
    }
}
