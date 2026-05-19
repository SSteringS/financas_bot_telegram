-- V3: adiciona tipo_arquivo em comprovantes para o front saber como renderizar (imagem inline vs PDF iframe)
ALTER TABLE comprovantes
    ADD COLUMN tipo_arquivo ENUM('IMAGEM', 'PDF') NOT NULL DEFAULT 'IMAGEM' AFTER imagem_url;
