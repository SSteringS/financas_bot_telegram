# Especificação de Design Técnico: Backend (MVP)

## 1. Escopo e Objetivos
Desenvolver o núcleo de processamento (Backend) para um bot de gestão de pagamentos via Telegram. O sistema deve ser capaz de receber imagens de cobranças, catalogá-las e, posteriormente, vincular comprovantes de pagamento a esses pedidos.

- **Foco:** Agilidade no desenvolvimento e arquitetura pronta para nuvem (AWS).
- **Usuários:** Acesso restrito via Whitelist (ID do Telegram).

## 2. Stack Tecnológica
- **Linguagem:** Java 21 (LTS)
- **Framework:** Spring Boot 3.x
- **Banco de Dados:** MySQL 8.0
- **Integração:** Telegram Bots Library (via Webhook)
- **Infraestrutura (Target):** AWS ECS Fargate, AWS RDS (MySQL), Terraform.

## 3. Arquitetura de Dados (ERD)

### Tabela: `pedidos_pagamento`
Responsável por armazenar a solicitação inicial da conta a ser paga.

- `id` (BIGINT, PK, Auto-increment)
- `telegram_user_id` (VARCHAR) - Identificação de quem enviou.
- `telegram_message_id` (VARCHAR) - Referência da mensagem original.
- `file_id_telegram` (VARCHAR) - ID da imagem no servidor do Telegram.
- `valor` (DECIMAL(10, 2)) - Valor do pagamento.
- `descricao` (TEXT) - Texto opcional enviado com a foto.
- `status` (ENUM: 'PENDENTE', 'PAGO', 'CANCELADO') - Default: 'PENDENTE'.
- `data_criacao` (TIMESTAMP) - Default: CURRENT_TIMESTAMP.

### Tabela: `comprovantes`
Responsável por armazenar a confirmação do pagamento.

- `id` (BIGINT, PK, Auto-increment)
- `pedido_id` (BIGINT, FK) - Relaciona com `pedidos_pagamento(id)`.
- `file_id_telegram` (VARCHAR) - ID da imagem do comprovante.
- `tipo_pagamento` (VARCHAR) - Ex: PIX, Boleto, Cartão.
- `data_pagamento` (TIMESTAMP) - Data/hora do envio do comprovante.

## 4. Fluxos de Negócio (Endpoints Webhook)

### 4.1. Recebimento de Novo Pedido
1. O sistema recebe um Webhook do Telegram com um objeto `Photo`.
2. Valida se o `from.id` está na lista de usuários permitidos.
3. Persiste os dados na tabela `pedidos_pagamento` com status `PENDENTE`.
4. O bot responde confirmando o recebimento e informando o ID do Pedido (Ex: "Registrado! Pedido #45").

### 4.2. Registro de Comprovante
1. O usuário envia uma foto com uma legenda contendo o ID (Ex: "#45 PIX").
2. O sistema utiliza Regex para extrair o ID e o tipo de pagamento.
3. Busca o pedido correspondente no banco.
4. Cria o registro em `comprovantes` e altera o status do pedido para `PAGO`.
5. O bot responde confirmando a baixa do pagamento.

