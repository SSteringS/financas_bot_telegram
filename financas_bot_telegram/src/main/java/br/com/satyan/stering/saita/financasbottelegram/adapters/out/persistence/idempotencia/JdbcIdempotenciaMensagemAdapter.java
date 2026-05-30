package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.idempotencia;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.IdempotenciaMensagemPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.Canal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// JdbcTemplate (em vez de JPA) evita invalidação de sessão Hibernate ao capturar DuplicateKeyException.
// A transação é a do chamador (REQUIRED) — claim e negócio commitam juntos (spec §4.3).
@Component
public class JdbcIdempotenciaMensagemAdapter implements IdempotenciaMensagemPort {

    private static final Logger logger = LoggerFactory.getLogger(JdbcIdempotenciaMensagemAdapter.class);

    private final JdbcTemplate jdbcTemplate;

    public JdbcIdempotenciaMensagemAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public boolean tentarClaim(Canal canal, String idExterno) {
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
