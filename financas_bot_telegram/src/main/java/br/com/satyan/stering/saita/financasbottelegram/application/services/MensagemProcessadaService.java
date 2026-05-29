package br.com.satyan.stering.saita.financasbottelegram.application.services;

import br.com.satyan.stering.saita.financasbottelegram.domain.model.CanalMensagem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// JdbcTemplate (em vez de JPA) evita invalidação de sessão Hibernate ao capturar DuplicateKeyException.
// A transação é a do chamador (REQUIRED) — claim e negócio commitam juntos (spec §4.3).
@Service
public class MensagemProcessadaService {

    private static final Logger logger = LoggerFactory.getLogger(MensagemProcessadaService.class);

    private final JdbcTemplate jdbcTemplate;

    public MensagemProcessadaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean tentarClaim(CanalMensagem canal, String idExterno) {
        try {
            jdbcTemplate.update(
                "INSERT INTO mensagem_processada (canal, id_externo) VALUES (?, ?)",
                canal.name(), idExterno
            );
            return true;
        } catch (DuplicateKeyException e) {
            logger.info("Mensagem já processada — canal={} idExterno={} (dedup, descarte silencioso)",
                canal, idExterno);
            return false;
        }
    }
}
