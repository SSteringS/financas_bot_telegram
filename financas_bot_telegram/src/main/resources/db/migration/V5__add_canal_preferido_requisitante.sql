-- BE-21a: canal preferido para roteamento de notificações
-- Default TELEGRAM garante backfill dos registros existentes sem UPDATE explícito.
-- V5 porque V4 é reservado para BE-19a (mensagem_processada).
ALTER TABLE requisitante
    ADD COLUMN canal_preferido VARCHAR(20) NOT NULL DEFAULT 'TELEGRAM';
