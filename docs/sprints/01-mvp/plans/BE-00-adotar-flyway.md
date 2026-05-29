# BE-00 — Adotar Flyway pra gestão de migrations

> **Esta tarefa precede toda a Fase 3 (`FASE-3-VISUALIZACAO.md`)**. Executar antes da BE-01.

---

## Contexto

Hoje as migrations do banco são scripts SQL soltos em `src/main/resources/`:

- `schema.sql` — schema inicial das tabelas `pedidos_pagamento` e `comprovantes`
- `migration.sql` — adiciona coluna `valor`
- `migration_s3_integration.sql` — adiciona coluna `imagem_url` em pedido e comprovante

Esse modelo não tem rastreabilidade entre ambientes (dev vs prod), não previne aplicação dupla, e não escala pra projeto que está prestes a entrar numa fase de várias migrations consecutivas (Fase 3). A Fase 3 inteira depende de pelo menos uma migration de schema (BE-01), então é o momento certo de adotar Flyway antes de mais qualquer trabalho.

Decisão tomada: **abordagem baseline.** Em vez de tentar reconstruir a história em V1 + V2 + V3, criar uma única `V1__initial_schema.sql` que reflete o estado atual consolidado (com todas as colunas que foram sendo adicionadas). A partir daí, qualquer mudança nova é V2, V3, etc.

Não é necessário fazer backup do RDS antes — está combinado que não há dados importantes em prod ainda.

---

## Objetivo

Ao final desta tarefa:

1. Flyway está configurado e ativo em dev e prod
2. Banco em prod tem `flyway_schema_history` registrando V1 como aplicada (via baseline)
3. Banco em dev pode ser dropado e recriado com `mvn spring-boot:run` aplicando V1 do zero
4. Os SQLs antigos (`schema.sql`, `migration.sql`, `migration_s3_integration.sql`) foram removidos do classpath
5. A tarefa BE-01 da Fase 3 (próxima) vai criar `V2__add_requisitante_dates_categoria_auth.sql` na mesma pasta e o sistema continua funcionando

---

## Arquivos esperados ao final

**Adicionados:**
- `financas_bot_telegram/src/main/resources/db/migration/V1__initial_schema.sql`

**Modificados:**
- `financas_bot_telegram/pom.xml` (dependências Flyway)
- `financas_bot_telegram/src/main/resources/application.properties` (config Flyway base)
- `financas_bot_telegram/src/main/resources/application-dev.properties` (se existir)
- `financas_bot_telegram/src/main/resources/application-dev.properties.example` (mesmo)
- `financas_bot_telegram/src/main/resources/application-prod.properties` (config baseline)

**Removidos:**
- `financas_bot_telegram/src/main/resources/schema.sql`
- `financas_bot_telegram/src/main/resources/migration.sql`
- `financas_bot_telegram/src/main/resources/migration_s3_integration.sql`

---

## Passos de execução

### Passo 1 — Adicionar dependências no `pom.xml`

Dentro de `<dependencies>`, adicionar:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

Spring Boot 3.4.5 já gerencia a versão dessas duas via BOM — não precisa especificar `<version>`.

### Passo 2 — Criar a estrutura de pastas e a V1

Criar `financas_bot_telegram/src/main/resources/db/migration/V1__initial_schema.sql` com este conteúdo exato:

```sql
CREATE TABLE pedidos_pagamento (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    telegram_user_id    VARCHAR(255),
    telegram_message_id VARCHAR(255),
    file_id_telegram    VARCHAR(255),
    imagem_url          TEXT,
    valor               DECIMAL(10, 2),
    descricao           TEXT,
    status              VARCHAR(50),
    data_criacao        DATETIME
);

CREATE TABLE comprovantes (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    pedido_id        BIGINT       NOT NULL,
    file_id_telegram VARCHAR(255),
    imagem_url       TEXT,
    tipo_pagamento   VARCHAR(255),
    data_pagamento   DATETIME,
    FOREIGN KEY (pedido_id) REFERENCES pedidos_pagamento(id)
);
```

Esse SQL é o estado atual de prod (verificável conectando no RDS e olhando `DESCRIBE pedidos_pagamento` / `DESCRIBE comprovantes`). Se houver qualquer divergência, **parar e relatar** — não improvisar mudanças no SQL.

### Passo 3 — Configurar Flyway base em `application.properties`

Adicionar ao final:

```properties
# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

Não setar `baseline-on-migrate` aqui (default é false). Cada profile decide.

### Passo 4 — Configurar dev (`application-dev.properties` e `application-dev.properties.example`)

Adicionar:

```properties
# Flyway: em dev, banco pode estar vazio ou desatualizado.
# Se vazio, Flyway aplica V1 do zero. Se já tem schema, baseline-on-migrate evita erro.
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
spring.flyway.baseline-description=Schema inicial consolidado
```

Atualizar tanto `application-dev.properties` (se existir localmente) quanto `application-dev.properties.example` (versionado).

### Passo 5 — Configurar prod (`application-prod.properties`)

Adicionar:

```properties
# Flyway: prod já tem o schema aplicado. Marcar como baseline V1, sem rodar V1.
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
spring.flyway.baseline-description=Schema inicial consolidado
```

Não desabilitar Flyway em prod — ele precisa estar ativo pra rodar futuras migrations (V2 em diante) automaticamente nos próximos deploys.

### Passo 6 — Remover os SQLs antigos

Apagar do classpath:

```
git rm financas_bot_telegram/src/main/resources/schema.sql
git rm financas_bot_telegram/src/main/resources/migration.sql
git rm financas_bot_telegram/src/main/resources/migration_s3_integration.sql
```

(Ficam no histórico git pra arqueologia futura, mas saem do build.)

### Passo 7 — Validar localmente em dev

1. Garantir que o banco MySQL local de dev está rodando
2. Conectar e dropar: `DROP DATABASE financas_bot_telegram_db; CREATE DATABASE financas_bot_telegram_db;`
3. Rodar a aplicação: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
4. Logs esperados durante o startup:
   ```
   Flyway Community Edition X.X.X by Redgate
   Database: jdbc:mysql://localhost:3306/financas_bot_telegram_db (MySQL X.X)
   Successfully validated 1 migration (...)
   Creating Schema History table `financas_bot_telegram_db`.`flyway_schema_history` ...
   Current version of schema `financas_bot_telegram_db`: << Empty Schema >>
   Migrating schema `financas_bot_telegram_db` to version "1 - initial schema"
   Successfully applied 1 migration ...
   ```
5. Conectar no banco e confirmar:
   ```sql
   SELECT * FROM flyway_schema_history;
   -- Deve ter 1 linha: V1, "initial schema", success=1
   
   SHOW TABLES;
   -- Deve listar: comprovantes, flyway_schema_history, pedidos_pagamento
   
   DESCRIBE pedidos_pagamento;
   -- Confirmar todas as 9 colunas
   ```

### Passo 8 — NÃO testar em prod manualmente

A validação em prod é o próximo deploy normal. **Não tentar rodar a app contra o RDS de prod localmente nem nada do tipo.** O deploy via pipeline GitHub Actions vai exercitar o caminho `baseline-on-migrate` automaticamente quando o push pra main acontecer (após merge da BE-00 em develop e depois em main).

O comportamento esperado em prod no primeiro deploy com Flyway:
- App sobe
- Flyway detecta schema existente sem `flyway_schema_history`
- Por causa de `baseline-on-migrate=true`, cria `flyway_schema_history` e marca V1 como aplicada (sem rodar o SQL)
- App fica saudável

Se algo der errado em prod, abrir issue/avisar humano. **Não fazer hotfix no banco ou no código sem combinar.**

---

## Critério de aceitação

- [ ] `pom.xml` tem `flyway-core` e `flyway-mysql`
- [ ] `src/main/resources/db/migration/V1__initial_schema.sql` existe com o SQL esperado
- [ ] `application.properties` tem `spring.flyway.enabled=true` e `spring.flyway.locations=classpath:db/migration`
- [ ] `application-dev.properties` (e `.example`) e `application-prod.properties` têm config baseline
- [ ] `schema.sql`, `migration.sql`, `migration_s3_integration.sql` foram removidos do classpath
- [ ] Em dev, depois de dropar o banco, `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` sobe sem erro, cria `flyway_schema_history` com 1 linha (V1 success), e cria as 2 tabelas com colunas corretas
- [ ] Em dev, dar um restart no app NÃO deve gerar erro nem rerunear V1 (Flyway pula porque já aplicou)
- [ ] App continua funcionando: `POST /webhook` com payload de teste persiste pedido sem erro

---

## Fora de escopo desta tarefa

- **Não criar** V2 nem nenhuma migration nova nesta tarefa. A BE-01 cuida disso.
- **Não mexer** em nenhuma classe Java (`@Entity`, controllers, services). Se algum teste/build quebrar por causa de mudanças no schema, parar e relatar — não significa que precisa modificar código Java.
- **Não adicionar** colunas, índices, FKs novos no V1 que não estão no schema atual de prod. V1 é fielmente o estado atual; mudanças vêm em V2+.
- **Não testar contra RDS de prod localmente.** O deploy normal cuida disso.

---

## Reportar status

Ao terminar, criar `docs/status/BE-00-adotar-flyway.md` seguindo o template em `docs/status/_TEMPLATE.md`. Pontos a cobrir no relatório:

- Confirmação de cada item do critério de aceitação
- Output dos logs do Flyway no startup em dev (cole o trecho)
- Output do `SELECT * FROM flyway_schema_history;` em dev
- Qualquer ajuste que precisou fazer no V1 caso o schema atual de prod tenha alguma diferença em relação ao listado no Passo 2
- Próximo passo: BE-01 está liberada
