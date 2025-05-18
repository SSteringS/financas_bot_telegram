# financas_bot_telegram
Bot do telegram, responsável por registrar pagamentos em um banco de dados

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