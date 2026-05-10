# 💸 Controle Pagamentos - Bot Telegram
Bot do Telegram desenvolvido em Java com Spring Boot, responsável por registrar pagamentos a partir de imagens enviadas pelos usuários. O bot agrupa as imagens de um mesmo pagamento, armazena os comprovantes em um bucket S3 particionado por data, e salva no MySQL as URLs das imagens, data/hora, categoria e demais informações relevantes do pagamento.
## 🚀 Fluxo do MVP

1. Usuário envia mensagem ao bot com duas imagens e uma categoria.
2. O bot recebe a mensagem e aciona o webhook (`/webhook`).
3. O webhook processa a mensagem:
    - Agrupa as imagens do mesmo pagamento.
    - Salva as imagens em um bucket S3, particionando por nome do mês e dia.
    - Salva no MySQL (RDS) as URLs das imagens, data/hora de gravação e categoria.

**🧩 Componentes principais:**
- Bot Telegram
- Webhook (Spring Controller)
- Serviço de Agrupamento (in-memory)
- Serviço de Upload S3
- Serviço de Persistência MySQL

## 🏗️ MVP de Infraestrutura

- Deploy automatizado no AWS ECS Fargate via pipeline CI/CD.
- Pipeline aciona ao fazer push no Git:
  - Build e versionamento da imagem Docker.
  - Salva a imagem no Amazon ECR.
  - Executa testes unitários com cobertura mínima de 99% (exceto classes configuradas para não cobrir).
  - Realiza deploy blue/green no ECS Fargate.
- Toda infraestrutura provisionada como código usando Terraform.

**🧩 Componentes principais de Infraestrutura:**
- AWS ECS Fargate (orquestração e execução dos containers)
- Amazon ECR (repositório de imagens Docker)
- AWS CodePipeline/CodeBuild (pipeline CI/CD)
- Terraform (infraestrutura como código)
- Amazon S3 (armazenamento de comprovantes)
- Amazon RDS (banco de dados relacional)

## 💻 Como rodar localmente

**Pré-requisitos:**
- Java 17+ e Maven instalados
- Conta na AWS e um bucket S3 criado (necessário para upload das imagens)
- Banco de dados MySQL disponível (local ou RDS) para persistência dos dados
- Token de bot do Telegram

**Configuração:**
- Defina as variáveis de ambiente necessárias:
    - `TELEGRAM_TOKEN` com o token do seu bot
    - Configurações de acesso à AWS (credenciais e região)
    - Configurações de acesso ao banco MySQL (URL, usuário, senha)
- Ajuste o nome do bucket S3 no código se necessário

**Execução:**
1. Clone o repositório
2. Execute `mvn spring-boot:run` na raiz do projeto

### 🚀 Testes Locais com Ngrok (Automatizado)

Para facilitar os testes locais e o recebimento de webhooks do Telegram, foi criado o script `setup_webhook.sh`. Ele automatiza o processo de iniciar o `ngrok`, obter a URL pública e configurar o webhook do seu bot.

**Como usar:**

1.  **Defina a variável de ambiente `TELEGRAM_BOT_TOKEN`**:
    No Git Bash, você pode exportar a variável para a sessão atual:
    ```bash
    export TELEGRAM_BOT_TOKEN="<SEU_TOKEN_AQUI>"
    ```
    Para uma configuração permanente, adicione a linha acima ao seu arquivo `~/.bashrc` ou `~/.bash_profile`.

2.  **Execute o script**:
    ```bash
    ./financas_bot_telegram/setup_webhook.sh
    ```

O script irá:
- Iniciar o `ngrok` na porta `8080`.
- Obter a URL HTTPS pública gerada pelo `ngrok`.
- Chamar a API do Telegram para registrar o webhook do seu bot, apontando para a sua aplicação rodando localmente.

**Observações:**
- Para testar o fluxo completo (upload no S3 e gravação no banco), é obrigatório ter o bucket S3 e o banco MySQL configurados e acessíveis
- O webhook do Telegram deve estar apontando para o endpoint `/webhook` da sua aplicação

# Estrutura de pastas
```bash
br.com.satyan.stering.saita
│
├── application         # Casos de uso (serviços de aplicação)
│
├── domain              # Entidades, agregados, repositórios e lógica de negócio
│
├── infrastructure      # Implementações técnicas (JPA, Telegram, etc)
│   ├── config          # Configurações do Spring e beans
│   ├── persistence     # Implementações dos repositórios (JPA, MySQL)
│   └── telegram        # Integração com a API do Telegram
│
├── adapters            # Adaptadores de entrada e saída
│   ├── in              # Entradas (ex: controllers REST, listeners)
│   └── out             # Saídas (ex: gateways, clients externos)
│
└── financasBotTelegramApplication.java # Classe principal do Spring Boot
```
## 📦 Backlog por Feature

### 1. Multi-entrada (Adapters)
- [ ] **Bot Discord**: Criar um bot no Discord como novo adapter de entrada.
    - [ ] Novo controller específico para o Discord.
    - [ ] Reutilizar os mesmos usecases/core do projeto (agnóstico à tecnologia).

### 2. Análise de Dados e Enriquecimento
- [ ] **Adicionar valor do pagamento** ao registro na base.
- [ ] **Salvar mais dados do pagamento** para análise comportamental:
    - [ ] Data/hora do pagamento
    - [ ] Categoria/subcategoria
    - [ ] Método de pagamento (ex: Pix, cartão, boleto)
    - [ ] Identificador do pagador (opcional, se fizer sentido)
    - [ ] Localização (se disponível)
    - [ ] Observações/comentários
- [ ] **Ferramentas para análise de dados**:
    - [ ] Conectar com Amazon QuickSight (dashboard e BI)
    - [ ] Exportação automática para S3 em formato analisável (CSV/Parquet)
    - [ ] Integração com Amazon SageMaker para análises preditivas/IA
    - [ ] Integração com AWS Athena para consultas SQL sobre dados no S3

### 3. Notificações e Integrações
- [ ] **Notificação automática**: Após salvar o comprovante, enviar para um usuário específico no Telegram (isso é possível via API do Telegram).
    - [ ] Configurar ID do usuário destino via variável de ambiente ou configuração.

curl -X POST "https://api.telegram.org/bot7632697875:AAEN6LRjPK1fLdMUhlV_cdNWxs0Gw4QyWao/setWebhook?url=https://656f85fd4b93.ngrok-free.app/webhook"