CREATE TABLE pedidos_pagamento (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    telegram_user_id    VARCHAR(255),
    telegram_message_id VARCHAR(255),
    file_id_telegram    VARCHAR(255),
    imagem_url          TEXT,
    valor               DECIMAL(10, 2),
    descricao           TEXT,
    status              VARCHAR(50),
    data_criacao        DATETIME
);

CREATE TABLE comprovantes (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    pedido_id        BIGINT       NOT NULL,
    file_id_telegram VARCHAR(255),
    imagem_url       TEXT,
    tipo_pagamento   VARCHAR(255),
    data_pagamento   DATETIME,
    FOREIGN KEY (pedido_id) REFERENCES pedidos_pagamento(id)
);
