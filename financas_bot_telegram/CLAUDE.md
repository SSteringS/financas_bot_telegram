# CLAUDE.md — Backend (financas_bot_telegram/)

## Responsabilidade

Este módulo é o backend do projeto. O Claude do **front não deve tocar nesta pasta**.

## Stack

- Java 21, Spring Boot 3.4.5, Spring MVC (não WebFlux)
- Spring Data JPA + MySQL (HikariCP)
- Spring Cloud AWS 3.2.0 (S3 + Secrets Manager)
- Maven

## Arquitetura hexagonal

```
adapters/
  in/
    telegram/          ← recebe updates do webhook do Telegram
      strategy/        ← Strategy por tipo de mensagem (PaymentRequest, PaymentProof)
  out/
    s3/service/        ← upload de imagens para S3
    telegram/service/  ← download de arquivos e envio de mensagens
application/
  usecases/            ← interfaces dos casos de uso (ports)
  services/            ← implementações dos casos de uso
domain/
  entity/              ← entidades JPA (PedidoPagamento, Comprovante)
infra/                 ← beans de configuração (RestClient, etc.)
```

Package raiz: `br.com.satyan.stering.saita.financasbottelegram`

## Perfis Spring

| Perfil | Banco | Bot | S3 |
|---|---|---|---|
| `dev` | MySQL local (3306) | Bot de dev | `bot-financas-pagamentos-dev` |
| `prod` | RDS via Secrets Manager | Bot de prod | `bot-financas-pagamentos-satyan` |

- `application.properties` — base, com placeholders `CHANGE_ME`
- `application-dev.properties` — local, **gitignored**, não commitar
- `application-dev.properties.example` — template para novos devs
- `application-prod.properties` — produção, credenciais via `${secret_key}`

## Como rodar localmente

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev -f financas_bot_telegram/pom.xml
```

Pré-requisitos: MySQL local rodando, `application-dev.properties` preenchido.

## Como buildar

```bash
mvn package -DskipTests -f financas_bot_telegram/pom.xml
```

## Endpoints REST (em desenvolvimento)

O bot Telegram consome o webhook em `POST /webhook`.
Os endpoints de consulta para o frontend estão sendo desenvolvidos na branch `feature/api-consulta-pedidos-comprovantes`.

## Regras importantes

- Nunca commitar `application-dev.properties` (gitignored)
- Nunca usar `ddl-auto=create` ou `update` em prod — usar migrations SQL manuais
- Manter a separação de camadas da arquitetura hexagonal — adapters não conhecem outros adapters
- Novos endpoints REST vão em `adapters/in/web/` (criar se não existir)
