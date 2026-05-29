-- V4: tabela de idempotência canal-agnóstica (ADR 0013 + spec §4.3)
-- Garante que o mesmo id_externo (wamid WhatsApp / update_id Telegram) não seja processado duas vezes.
-- Claim-then-process na mesma transação: falha do claim = mensagem já processada (descarte silencioso).
CREATE TABLE mensagem_processada (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    canal VARCHAR(20) NOT NULL,
    id_externo VARCHAR(255) NOT NULL,
    processado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_canal_idexterno (canal, id_externo)
) ENGINE=InnoDB;
