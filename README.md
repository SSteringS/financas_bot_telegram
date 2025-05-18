# financas_bot_telegram
Bot do telegram, responsável por registrar pagamentos em um banco de dados

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