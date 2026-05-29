-- BE-21a: canal preferido para roteamento de notificações
-- Default TELEGRAM garante backfill dos registros existentes sem UPDATE explícito.
-- NOTA: se BE-19a mergear antes desta task, este arquivo precisará ser renumerado para V5.
ALTER TABLE requisitante
    ADD COLUMN canal_preferido VARCHAR(20) NOT NULL DEFAULT 'TELEGRAM';
