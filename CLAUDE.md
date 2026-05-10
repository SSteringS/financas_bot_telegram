# CLAUDE.md — Raiz do Repositório

## Visão geral do projeto

Bot de finanças para Telegram com frontend web. Monorepo com múltiplos módulos e instâncias de Claude Code colaborando em paralelo.

## Instâncias do Claude Code — quem faz o quê

| Instância | Pasta de trabalho | Branch | Responsabilidade |
|---|---|---|---|
| Claude do **back** | `financas_bot_telegram/` + infra | `feature/back-*` | API REST, domínio, banco, deploy |
| Claude do **front** | `frontend/` | `feature/frontend-*` | UI, componentes, chamadas à API |
| Claude de **planejamento** | `docs/` | `develop` (direto) | Documentação, especificações técnicas, decisões de arquitetura, planos de feature |

## Regra de ouro — isolamento por pasta

- **Claude do back:** toca apenas em `financas_bot_telegram/`, `infra/`, `finbot.service`, `.github/workflows/`
- **Claude do front:** toca apenas em `frontend/`
- **Claude de planejamento:** toca apenas em `docs/` — commita direto em `develop`
- Arquivos da raiz (`CLAUDE.md`, `.gitignore`, `TODO.md`) podem ser editados por qualquer instância quando necessário

## Estrutura da pasta docs/

Organizada e mantida pelo Claude de planejamento:

```
docs/
  README.md                        ← índice geral
  architecture/
    especificacao-tecnica.md       ← spec técnica detalhada por feature
    design-proposals/              ← variantes visuais e de UX
  plans/
    FASE-3-VISUALIZACAO.md         ← plano da fase atual
  runbooks/
    ROTEIRO-FRONTEND.md            ← guia passo a passo para o front
  decisions/
    _TEMPLATE.md                   ← template para registrar decisões (ADRs)
  status/
    _TEMPLATE.md                   ← template para status de features
```

**Back e front devem consultar `docs/` antes de implementar** — lá estão as especificações, contratos de API e decisões já tomadas.

## Fluxo de branches

```
main (protegida — só via PR)
 └── develop  ← planejamento commita aqui direto
      ├── feature/api-consulta-pedidos-comprovantes   ← back
      └── feature/frontend-setup                      ← front
```

- Back e front sempre criam branch a partir de `develop`
- Padrão: `feature/descricao-curta`, `fix/descricao-curta`, `hotfix/descricao-curta`
- PR: branch → `develop` → revisão → merge → PR `develop → main` dispara o deploy
- Back e front devem fazer merge de `develop` na feature branch regularmente para pegar atualizações de docs

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
