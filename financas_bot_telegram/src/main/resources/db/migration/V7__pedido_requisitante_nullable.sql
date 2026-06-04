-- ============================================================
-- V7: Torna requisitante_id nullable em pedidos_pagamento
--
-- Motivo: Pedidos categoria=FOLHA são gerados pelo sistema
-- (FecharMesUseCase) sem um requisitante humano. A coluna foi
-- criada como NOT NULL em V2 para pedidos do Telegram, mas a
-- extensão STI exige que a FK seja opcional para registros FOLHA.
--
-- instant DDL no MySQL 8 (sem lock de tabela para MODIFY NULL)
-- ============================================================

ALTER TABLE pedidos_pagamento
    MODIFY COLUMN requisitante_id BIGINT NULL;
