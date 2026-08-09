# BE-00 — Adotar Flyway

**Data:** 2026-05-10
**Branch:** feature/api-consulta-pedidos-comprovantes
**Commit/PR:** (ver commit desta tarefa)
**Responsável (instância):** Claude Code (CLI) — back

---

## O que foi feito

- Adicionadas dependências `flyway-core` e `flyway-mysql` ao `pom.xml` (versão gerenciada pelo Spring Boot BOM)
- Criada `src/main/resources/db/migration/V1__initial_schema.sql` com o schema consolidado (estado atual de prod)
- `application.properties`: `ddl-auto` alterado de `update` para `none`; adicionadas configs base do Flyway (`enabled=true`, `locations`)
- `application-dev.properties.example`: `ddl-auto` para `none`, dialect corrigido de `MySQL8Dialect` para `MySQLDialect`, adicionado bloco Flyway com `baseline-on-migrate`
- `application-prod.properties`: adicionado bloco Flyway com `baseline-on-migrate`
- Removidos do classpath (via `git rm`): `schema.sql`, `migration.sql`, `migration_s3_integration.sql`
- Validação em dev concluída com sucesso (logs abaixo)

---

## Desvios do plano

Nenhum. O V1 exato do plano foi aplicado sem ajustes — o schema atual em dev bateu com o descrito no `BE-00-adotar-flyway.md`.

---

## Decisões tomadas durante a execução

- `spring.jpa.hibernate.ddl-auto=none` também foi aplicado no `application.properties` base (antes estava `update`). O plano não mencionava explicitamente esse arquivo, mas é necessário para consistência — sem isso, o Hibernate tentaria gerenciar o schema no perfil default.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

Um aviso menor aparece nos logs que não bloqueia nada:
```
MySQLDialect does not need to be specified explicitly using 'hibernate.dialect'
```
Pode ser removido apagando `spring.jpa.properties.hibernate.dialect` dos properties. Fica como dívida técnica baixa.

---

## Próximos passos / observações pro próximo

- BE-01 está liberada.
- O `application-dev.properties` local (gitignored) também precisa ser atualizado pelo desenvolvedor com as configs de Flyway baseline — o `.example` já reflete isso.
- Em prod, no primeiro deploy com Flyway ativo, o comportamento esperado é: Flyway cria `flyway_schema_history` e marca V1 como baseline sem rodar o SQL. Não requer nenhuma ação manual.
- Aviso nos logs: `MySQL 8.4 is newer than this version of Flyway and support has not been tested` — apenas warning informativo, não impacta funcionamento.

---

## Output dos logs do Flyway em dev (startup com banco vazio)

```
INFO  o.f.c.FlywayExecutor         : Database: jdbc:mysql://localhost:3306/financas_bot_telegram_db (MySQL 8.4)
WARN  o.f.c.internal.database.base.Database : Flyway upgrade recommended: MySQL 8.4 is newer than this version of Flyway...
INFO  o.f.c.i.s.JdbcTableSchemaHistory : Schema history table `financas_bot_telegram_db`.`flyway_schema_history` does not exist yet
INFO  o.f.c.i.c.DbValidate         : Successfully validated 1 migration (execution time 00:00.013s)
INFO  o.f.c.i.s.JdbcTableSchemaHistory : Creating Schema History table `financas_bot_telegram_db`.`flyway_schema_history` ...
INFO  o.f.c.i.c.DbMigrate          : Current version of schema `financas_bot_telegram_db`: << Empty Schema >>
INFO  o.f.c.i.c.DbMigrate          : Migrating schema `financas_bot_telegram_db` to version "1 - initial schema"
INFO  o.f.c.i.c.DbMigrate          : Successfully applied 1 migration to schema `financas_bot_telegram_db`, now at version v1 (execution time 00:00.034s)
```

App subiu em **4.6 segundos** (vs. 71 segundos antes, quando havia timeout de conexão com RDS).

---

## Arquivos criados/modificados

- `financas_bot_telegram/src/main/resources/db/migration/V1__initial_schema.sql` (novo)
- `financas_bot_telegram/pom.xml` (modificado: +flyway-core, +flyway-mysql)
- `financas_bot_telegram/src/main/resources/application.properties` (modificado: ddl-auto=none, +Flyway)
- `financas_bot_telegram/src/main/resources/application-dev.properties.example` (modificado: ddl-auto=none, dialect corrigido, +Flyway baseline)
- `financas_bot_telegram/src/main/resources/application-prod.properties` (modificado: +Flyway baseline)
- `financas_bot_telegram/src/main/resources/schema.sql` (removido)
- `financas_bot_telegram/src/main/resources/migration.sql` (removido)
- `financas_bot_telegram/src/main/resources/migration_s3_integration.sql` (removido)
