-- Script para adicionar coluna imagem_url nas tabelas de pedidos e comprovantes
-- Data: 2026-01-12
-- Propósito: Adicionar suporte para armazenar URLs de imagens no S3

ALTER TABLE pedidos_pagamento
ADD COLUMN imagem_url TEXT AFTER file_id_telegram;

ALTER TABLE comprovantes
ADD COLUMN imagem_url TEXT AFTER file_id_telegram;

-- Verificação das alterações
-- SELECT * FROM pedidos_pagamento LIMIT 1;
-- SELECT * FROM comprovantes LIMIT 1;

