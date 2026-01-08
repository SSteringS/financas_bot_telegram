# Especificação de Design Técnico: Backend (MVP - Serverless)

## 1. Escopo e Objetivos
Desenvolver o núcleo de processamento (Backend) para um bot de gestão de pagamentos via Telegram. O sistema cataloga pedidos de pagamento (prints/boletos) e vincula comprovantes a esses pedidos através de um fluxo assistido pelo usuário.

- **Foco:** Baixo custo operacional, escalabilidade serverless e rastreabilidade de obrigações financeiras.
- **Usuários:** Acesso restrito via Whitelist de IDs do Telegram.

## 2. Stack Tecnológica
- **Linguagem:** Java 21 (LTS).
- **Framework:** Spring Boot 3.4.5 (com AWS Serverless Java Container).
- **Banco de Dados:** Amazon RDS MySQL (db.t4g.micro).
- **Armazenamento:** Amazon S3 (Imagens de pedidos e comprovantes).
- **Infraestrutura:** AWS Lambda + SnapStart via Function URL (HTTPS direto).
- **Segurança:** AWS Secrets Manager para gestão de tokens e credenciais.

## 3. Arquitetura e Design Patterns
A aplicação segue os princípios da **Arquitetura Hexagonal** (Ports & Adapters) e utiliza o **Strategy Pattern** para processamento de mensagens:
- **UpdateOrchestratorService:** Orquestrador central que identifica o tipo de `Update` do Telegram (Texto simples, Foto, Documento) e delega para a estratégia correta.
- **UpdateProcessingStrategy:** Interface comum para as estratégias de negócio:
    - `PaymentRequestStrategy`: Processa novos pedidos de pagamento.
    - `PaymentProofStrategy`: Processa e vincula comprovantes a pedidos existentes.

## 4. Fluxos de Negócio

### 4.1. Recebimento de Novo Pedido (Ingestão)
1. O usuário recebe um pedido via WhatsApp e o encaminha/envia para o Telegram.
2. Se for uma imagem/print, deve conter a legenda no formato: `VALOR DESCRIÇÃO` (ex: `150.00 Energia`).
3. O sistema valida o usuário na **Whitelist**.
4. A imagem é salva no S3 e um registro é criado no RDS com status `PENDENTE`.
5. O bot responde com: "Registrado! Pedido #ID".

### 4.2. Registro de Comprovante (Conciliação)
1. O usuário envia a foto do comprovante de pagamento.
2. A legenda deve seguir o padrão: `#ID TIPO_PAGAMENTO` (ex: `#45 PIX`).
3. O sistema extrai o ID via Regex, busca o pedido original e salva o comprovante no S3.
4. O status do pedido no RDS é atualizado para `PAGO`.

## 5. Tratamento de Erros e Exceções (UX & Resiliência)
O sistema utiliza um `GlobalTelegramExceptionHandler` para garantir que o usuário nunca fique sem resposta:

- **UnauthorizedUserException:** "🚫 Você não tem permissão para usar este bot."
- **InvalidMessageFormatException:** "😕 Formato de mensagem inválido. Para registrar um pedido, use `VALOR DESCRIÇÃO`. Para comprovante, use `#ID TIPO_PAGAMENTO`."
- **PedidoNaoEncontradoException:** "⚠️ Pedido não encontrado com o ID informado."
- **InvalidCaptionException:** "❌ A legenda da foto não segue os padrões esperados."
- **DatabaseException:** "🚨 Erro interno ao acessar o banco de dados. Tente novamente mais tarde."

## 6. Dicionário de Mensagens (Interface)

### 6.1. Sucesso
- **Novo Pedido:** `Registrado! Pedido #{id}`
- **Pagamento:** `Pagamento registrado com sucesso para o pedido #{id}!`

### 6.2. Erros de Entrada
- **Legenda Faltando:** `❌ Por favor, envie uma legenda com a foto.`
- **ID Inválido:** `⚠️ ID do pedido inválido na legenda.`

## 7. Rastreabilidade para Product Discovery (Morpheus)
Para fins de análise de usabilidade, o sistema monitora:
1. **Lead Time:** Tempo entre a criação do pedido (Zona WhatsApp) e a liquidação (Zona Telegram).
2. **Taxa de Erro de Input:** Frequência com que o usuário recebe mensagens de "Formato Inválido".
3. **Volume de Operação:** Total de arquivos custodiados no S3 vs. registros no RDS.

## 8. Segurança
- **Whitelist Dinâmica:** Carregada via variável de ambiente `TELEGRAM_ALLOWED_USER_IDS`.
- **Isolamento de Rede:** RDS em subnet privada, acessível apenas pela Lambda.