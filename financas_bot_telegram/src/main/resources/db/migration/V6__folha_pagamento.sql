-- ============================================================
-- V6: Folha de pagamento
-- Revisado pos-analise DBA (2026-05-31):
--   + CHECK constraints em todas as tabelas
--   + atualizado_em em funcionario (rastreabilidade de salario)
--   + mes_referencia + UNIQUE INDEX para idempotencia no banco
--   + Sem pedido_folha_meta (invariante por CHECK em pedidos_pagamento)
--   + Sem fechamento_mes (snapshot em observacao do Pedido FOLHA)
-- ============================================================

-- 1. Funcionario domestico
CREATE TABLE funcionario (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome                     VARCHAR(120)                NOT NULL,
    salario_base             DECIMAL(10,2)               NOT NULL,
    forma_pagamento          ENUM('PIX','TED')           NOT NULL,
    chave_pix                VARCHAR(255)                NULL,
    banco                    VARCHAR(50)                 NULL,
    agencia                  VARCHAR(20)                 NULL,
    conta                    VARCHAR(30)                 NULL,
    tipo_conta               ENUM('CORRENTE','POUPANCA') NULL,
    conta_propria            BOOLEAN NOT NULL DEFAULT TRUE,
    obs_pagamento            VARCHAR(500)                NULL,
    dia_pagamento_referencia INT                         NULL,
    ativo                    BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em                DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_func_pix CHECK (
        forma_pagamento <> 'PIX' OR chave_pix IS NOT NULL
    ),
    CONSTRAINT chk_func_ted CHECK (
        forma_pagamento <> 'TED'
        OR (banco IS NOT NULL AND agencia IS NOT NULL AND conta IS NOT NULL AND tipo_conta IS NOT NULL)
    ),
    CONSTRAINT chk_func_dia CHECK (
        dia_pagamento_referencia IS NULL OR dia_pagamento_referencia BETWEEN 1 AND 31
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Plano de adiantamento parcelado
CREATE TABLE adiantamento (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    funcionario_id BIGINT        NOT NULL,
    descricao      VARCHAR(255)  NOT NULL,
    valor_total    DECIMAL(10,2) NOT NULL,
    valor_parcela  DECIMAL(10,2) NOT NULL,
    num_parcelas   INT           NOT NULL,
    parcelas_pagas INT           NOT NULL DEFAULT 0,
    data_inicio    DATE          NOT NULL,
    ativo          BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_adiant_funcionario   FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
    CONSTRAINT chk_adiant_consistencia CHECK (ABS(valor_total - (valor_parcela * num_parcelas)) <= 0.01),
    CONSTRAINT chk_adiant_parcelas     CHECK (parcelas_pagas BETWEEN 0 AND num_parcelas),
    CONSTRAINT chk_adiant_valores      CHECK (valor_total > 0 AND valor_parcela > 0 AND num_parcelas > 0),
    INDEX idx_adiant_func_ativo (funcionario_id, ativo, data_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Extensao minima em pedidos_pagamento (STI com CHECK + idempotencia via UNIQUE INDEX)
-- ADD COLUMN NULL = instant DDL no MySQL 8 (sem lock de tabela)
ALTER TABLE pedidos_pagamento
    ADD COLUMN categoria      ENUM('VALE','FOLHA') NULL          AFTER status,
    ADD COLUMN funcionario_id BIGINT               NULL          AFTER categoria,
    ADD COLUMN fechado        BOOLEAN NOT NULL DEFAULT FALSE      AFTER funcionario_id,
    ADD COLUMN observacao     VARCHAR(1000)        NULL          AFTER fechado,
    ADD COLUMN mes_referencia DATE                 NULL          AFTER observacao,
    ADD CONSTRAINT fk_pedido_funcionario FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
    ADD CONSTRAINT chk_pedido_folha CHECK (
        categoria IS NULL
        OR (categoria IN ('VALE','FOLHA') AND funcionario_id IS NOT NULL)
    ),
    ADD CONSTRAINT chk_pedido_folha_mes CHECK (
        categoria <> 'FOLHA' OR mes_referencia IS NOT NULL
    ),
    -- NULLs sao distintos no UNIQUE MySQL:
    --   VALE (mes_referencia=NULL) coexistem; FOLHA garante unicidade por funcionario/mes
    ADD UNIQUE INDEX uq_folha_por_mes (funcionario_id, mes_referencia),
    ADD INDEX idx_pedido_folha (funcionario_id, categoria, fechado, data_criacao DESC);
