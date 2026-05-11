-- ============================================================
-- V2: requisitante, datas, tipo de pagamento, auth_token
-- ============================================================

-- 1. Tabela requisitante
CREATE TABLE requisitante (
    id        BIGINT       AUTO_INCREMENT PRIMARY KEY,
    nome      VARCHAR(255) NOT NULL,
    telefone  VARCHAR(20)  NULL,
    email     VARCHAR(255) NULL,
    ativo     BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Linha inicial — ajustar telefone para o real antes de rodar em prod
INSERT INTO requisitante (id, nome, telefone, email, ativo, criado_em)
VALUES (1, 'Satyan Saita', '+5548991825902', NULL, TRUE, NOW());

-- 3. Novos campos em pedidos_pagamento
--    Adicionados como NULL primeiro para permitir backfill antes de impor NOT NULL
ALTER TABLE pedidos_pagamento
    ADD COLUMN requisitante_id BIGINT                                           NOT NULL DEFAULT 1,
    ADD COLUMN data_pedido     DATE                                             NULL,
    ADD COLUMN data_pagamento  DATE                                             NULL,
    ADD COLUMN tipo            ENUM('BOLETO','PIX','TED','AGENDAMENTO','OUTRO') NULL;

-- 4. Backfill: data_pedido a partir do timestamp de criação
UPDATE pedidos_pagamento
SET data_pedido = COALESCE(DATE(data_criacao), CURRENT_DATE);

-- 5. Backfill: data_pagamento para pedidos PAGO, a partir do comprovante mais antigo
UPDATE pedidos_pagamento pp
    INNER JOIN (
        SELECT pedido_id, DATE(MIN(data_pagamento)) AS data_comp
        FROM comprovantes
        WHERE data_pagamento IS NOT NULL
        GROUP BY pedido_id
    ) c ON c.pedido_id = pp.id
SET pp.data_pagamento = c.data_comp
WHERE pp.status = 'PAGO';

-- 6. Tornar data_pedido obrigatório após backfill garantir que não há NULLs
ALTER TABLE pedidos_pagamento
    MODIFY COLUMN data_pedido DATE NOT NULL;

-- 7. FK requisitante_id → requisitante.id
ALTER TABLE pedidos_pagamento
    ADD CONSTRAINT fk_pedido_requisitante
        FOREIGN KEY (requisitante_id) REFERENCES requisitante (id);

-- 8. Índices para as queries de listagem
CREATE INDEX idx_pedido_requisitante_data ON pedidos_pagamento (requisitante_id, data_pedido);
CREATE INDEX idx_pedido_status_data       ON pedidos_pagamento (status, data_pedido);

-- 9. Tabela auth_token (link mágico)
CREATE TABLE auth_token (
    token_hash      CHAR(64)  NOT NULL PRIMARY KEY,
    requisitante_id BIGINT    NOT NULL,
    criado_em       DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em       DATETIME  NOT NULL,
    usado_em        DATETIME  NULL,
    CONSTRAINT fk_auth_token_requisitante
        FOREIGN KEY (requisitante_id) REFERENCES requisitante (id)
);
