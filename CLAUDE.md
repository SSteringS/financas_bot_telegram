# CLAUDE.md — Raiz do Repositório

## Visão geral do projeto

Bot de finanças para Telegram com frontend web. Monorepo com dois módulos independentes:

| Pasta | Responsável | Tecnologia |
|---|---|---|
| `financas_bot_telegram/` | Claude do **back** | Java 21 + Spring Boot |
| `frontend/` | Claude do **front** | A definir |

## Regra de ouro — isolamento por pasta

- **Claude do back:** toca apenas em `financas_bot_telegram/` e arquivos de infra (`infra/`, `finbot.service`, `.github/workflows/`)
- **Claude do front:** toca apenas em `frontend/`
- Arquivos da raiz (`CLAUDE.md`, `.gitignore`, `TODO.md`) podem ser editados por ambos quando necessário

## Fluxo de branches

```
main (protegida — só via PR)
 └── develop
      ├── feature/api-consulta-pedidos-comprovantes   ← back
      └── feature/frontend-tela-inicial               ← front
```

- Sempre criar branch a partir de `develop`
- Padrão: `feature/descricao-curta`, `fix/descricao-curta`, `hotfix/descricao-curta`
- PR: branch → `develop` → revisão → merge → PR `develop → main` dispara o deploy

## CI/CD

- Pipeline em `.github/workflows/deploy.yml`
- Dispara no merge para `main`
- Faz build do JAR do back + deploy via SSH na EC2 (`3.228.138.109`)
- Testes pulados temporariamente (`-DskipTests`) — reativar com H2 antes da próxima feature

## Infraestrutura (produção)

- **EC2:** t4g.micro, Amazon Linux 2023, Java 21, systemd (`finbot.service`)
- **RDS:** MySQL 8.0, default VPC, acesso restrito ao Elastic IP da EC2
- **S3:** `bot-financas-pagamentos-satyan` (prod) / `bot-financas-pagamentos-dev` (dev)
- **Secrets Manager:** `finbot-prod-secrets` (telegram_token, db_host, db_username, db_password)
- **HTTPS:** porta 8443, certificado auto-assinado em `/opt/finbot/keystore.p12`
