---
ambiente: prod
ultimo_deploy: null         # preenchido pelo arquiteto quando acionado pelo planner
ultimo_review: 2026-05-30
---

# Estado Atual — Produção (EC2)

> Atualizado pelo arquiteto quando o planner informa que uma task foi deployed em prod.
> EC2: t4g.micro · Amazon Linux 2023 · Java 21 · `finbot.service` (systemd)

## Como atualizar

Quando o planner informar "task X foi deployed em prod":
1. Ler o status report da task em `docs/sprints/<NN>/status/`.
2. Comparar com a seção relevante deste arquivo.
3. Atualizar as seções afetadas — só o que mudou.
4. Atualizar o campo `ultimo_deploy` no frontmatter.

## Stack em produção

| Item | Versão / detalhe |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| Framework web | Spring MVC |
| Banco | MySQL 8.0 (RDS, default VPC) |
| S3 | `bot-financas-pagamentos-satyan` |
| Secrets Manager | `finbot-prod-secrets` |
| HTTPS | porta 8443, certificado auto-assinado `/opt/finbot/keystore.p12` |

## Features em produção

> Listar o que está efetivamente deployado e funcionando em prod.
> Atualizar cada vez que um deploy acontece.

### Canal Telegram
- [ ] Registrar pedido via mensagem Telegram — **status: confirmar**
- [ ] Registrar comprovante via foto Telegram — **status: confirmar**

### API REST (visualização)
- [ ] Endpoints de consulta de pedidos — **status: confirmar**

> ⚠️ Este arquivo precisa ser preenchido na próxima sessão do arquiteto.
> Referência: ler `estado-atual-dev.md` e o histórico de deploys para reconstruir.

## Defasagem em relação a develop

> Quando prod lagar atrás de dev, listar o que está em develop mas não em prod.
> Isso ajuda o planner a saber o que falta deployar.

(a preencher)
