# Estado Atual do Backend

Levantamento feito em 2026-05-10, lendo direto o código de `financas_bot_telegram/src/`. Substitui as suposições que estavam na `especificacao-tecnica.md` baseadas no resumo verbal inicial. Quando houver divergência entre este documento e a `especificacao-tecnica.md`, **este documento ganha** — é o reflexo da realidade. A especificação técnica deve ser revisada com base no que está aqui.

---

## 1. Resumo executivo

O backend está mais maduro do que o resumo inicial sugeria. O fluxo do bot Telegram (registrar pedido, registrar comprovante) está completo e funcional, com hexagonal aplicada de forma majoritariamente correta. **Não existe ainda nenhum endpoint REST de consulta** — apesar do nome da branch `feature/api-consulta-pedidos-comprovantes` sugerir o contrário, o PR #45 só adicionou os arquivos `CLAUDE.md` de instrução. O scaffold da camada de visualização (modelo, endpoints REST, auth, pre-signed URL) ainda está integralmente pendente.

A boa notícia é que muito do que `FASE-3-VISUALIZACAO.md` propunha encaixa direto na arquitetura existente. A má notícia é que a especificação técnica precisa de pequenos mas importantes ajustes — algumas premissas estavam erradas (stack, nomenclatura, modelo de dados existente).

---

## 2. Stack real (vs. assumida)

| Item | Assumido na especificação | Real |
|---|---|---|
| Framework web | Spring **WebFlux** | Spring **MVC** (`spring-boot-starter-web`) |
| Versão Spring Boot | 3.4.5 | 3.4.5 ✓ |
| Java | 21 | 21 ✓ |
| Build | Maven | Maven ✓ |
| ORM | JPA | JPA (Spring Data) ✓ |
| Banco | MySQL 8 | MySQL 8 (driver `mysql-connector-j`) ✓ |
| Lombok | não mencionado | sim (`@Getter`, `@Setter`, `@Data`, `@Builder`) |
| Validation | não mencionado | sim (`spring-boot-starter-validation`) |
| OpenAPI/Swagger | manual (`docs/api/openapi.yaml`) | já tem `springdoc-openapi-starter-webmvc-ui` 2.5.0 — Swagger UI auto-gerado em `/swagger-ui.html` |
| AWS S3 | sim | sim (`spring-cloud-aws-starter-s3`) |
| AWS Secrets Manager | sim | sim (`spring-cloud-aws-starter-secrets-manager`) |

**Implicação crítica:** todos os exemplos de código WebFlux na `especificacao-tecnica.md` (`CorsWebFilter`, `ReactiveSecurityContext`, etc) precisam ser reescritos pra equivalentes em Spring MVC (`CorsConfigurationSource`, `SecurityContextHolder`, `OncePerRequestFilter`).

Houve uma migração explícita de WebFlux pra MVC no commit `e38df3b fix: substituir WebFlux por Spring MVC e documentar separação de ambientes`. A decisão deve estar registrada em algum lugar; vale criar um ADR retroativo em `docs/decisions/` consolidando essa escolha.

**Bonus:** com `springdoc-openapi`, o passo de "criar `docs/api/openapi.yaml`" da BE-04 muda — não precisa escrever YAML à mão, basta anotar os controllers (`@Operation`, `@Parameter`) e o YAML/Swagger UI sai automaticamente.

---

## 3. Estrutura de pacotes

Hexagonal aplicada. Pacote raiz: `br.com.satyan.stering.saita.financasbottelegram`.

```
financasbottelegram/
├── FinancasBotTelegramApplication.java     (main)
├── adapters/
│   ├── in/
│   │   └── telegram/
│   │       ├── controller/
│   │       │   └── TelegramWebhookController.java   (POST /webhook)
│   │       ├── exception/                            (5 exceções específicas do Telegram)
│   │       ├── exceptionhandler/
│   │       │   └── GlobalTelegramExceptionHandler.java
│   │       ├── service/
│   │       │   └── UpdateOrchestratorService.java   (escolhe qual strategy aplicar)
│   │       └── strategy/
│   │           ├── UpdateProcessingStrategy.java     (interface)
│   │           ├── PaymentRequestStrategy.java       (regex "150.00 Almoço")
│   │           └── PaymentProofStrategy.java         (regex "#123 PIX")
│   └── out/
│       ├── persistence/
│       │   ├── PedidoPagamentoJpaRepository.java
│       │   ├── PedidoPagamentoRepositoryAdapter.java
│       │   ├── ComprovanteJpaRepository.java
│       │   └── ComprovanteRepositoryAdapter.java
│       ├── s3/service/
│       │   ├── S3ImageUploadService.java
│       │   └── S3UploadException.java
│       └── telegram/service/
│           ├── TelegramFileDownloaderService.java
│           ├── TelegramMessageSenderService.java
│           └── TelegramFileDownloadException.java
├── application/
│   ├── dto/
│   │   ├── PaymentMessageDTO.java
│   │   └── PaymentCategory.java                      (CLASSE VAZIA — placeholder)
│   ├── exceptions/                                    (3 exceções de regra de negócio)
│   ├── mapper/                                        (PASTA VAZIA)
│   ├── port/
│   │   ├── in/
│   │   │   └── TelegramPortIn.java
│   │   └── out/
│   │       ├── PedidoPagamentoRepository.java        (extends JpaRepository — leak)
│   │       ├── PedidoPagamentoRepositoryPort.java    (interface limpa)
│   │       ├── ComprovanteRepository.java            (extends JpaRepository — leak)
│   │       └── ComprovanteRepositoryPort.java        (interface limpa)
│   ├── services/
│   │   ├── SalvarPedidoPagamentoServiceImpl.java
│   │   └── RegistrarComprovanteServiceImpl.java
│   └── usecases/
│       ├── SalvarPedidoPagamentoUsecase.java
│       └── RegistrarComprovanteUsecase.java
├── domain/
│   ├── entity/
│   │   ├── PedidoPagamento.java                      (entidade JPA — não puro POJO)
│   │   └── Comprovante.java                          (entidade JPA — não puro POJO)
│   ├── enums/
│   │   └── StatusPedido.java                         (PENDENTE, PAGO, CANCELADO)
│   ├── exceptions/
│   │   └── PedidoNaoEncontradoException.java
│   └── model/
│       └── TelegramMediaGroup.java
└── infra/
    └── AppConfig.java
```

### Observações arquiteturais

**Adesão à hexagonal: 75%.** Boa separação entre `adapters/`, `application/`, `domain/`, `infra/`. Strategies estão no lado certo (in/telegram). Services e usecases estão na camada de aplicação. Mas:

1. **Domain entity tem anotações JPA** (`@Entity`, `@Table`, `@Column`) — quebra a regra hexagonal estrita de domínio puro sem framework. Isso é uma escolha pragmática comum em projetos Spring/JPA, mas vale registrar como decisão consciente.

2. **Port `PedidoPagamentoRepository` extends `JpaRepository`** — leak de Spring Data Framework no port hexagonal. Existe uma versão "limpa" `PedidoPagamentoRepositoryPort` em paralelo. Mesmo padrão duplicado pra Comprovante. Parece refactor parado no meio. **Decisão pendente:** continuar com os ports limpos (e remover os JpaRepository expostos) ou aceitar o vazamento.

3. **Pasta `application/mapper/` está vazia** — provavelmente reservada pra futuros mappers de DTO ↔ Domain.

4. **`PaymentCategory.java` é uma classe vazia** — placeholder pra categoria de pagamento. Não é usada efetivamente; o tipo de pagamento real é capturado como string livre em `Comprovante.tipoPagamento`.

5. **`StatusPedido` tem `CANCELADO` mesmo sem fluxo de cancelamento implementado** — o enum já prevê o terceiro estado, embora a especificação tenha definido "cancelamento fora do MVP". Pode ter sido design defensivo; nenhum código atual faz transição pra CANCELADO.

---

## 4. Modelo de dados real

### Schema atual (de `src/main/resources/schema.sql` + migrations)

**Tabela `pedidos_pagamento`:**

| Coluna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT AUTO_INCREMENT PK | |
| `telegram_user_id` | VARCHAR(255) | quem enviou no Telegram |
| `telegram_message_id` | VARCHAR(255) | ID da mensagem original |
| `file_id_telegram` | VARCHAR(255) | ID do arquivo no Telegram (foto do pedido) |
| `imagem_url` | TEXT | URL no S3 |
| `valor` | DECIMAL(10,2) | |
| `descricao` | TEXT | |
| `status` | VARCHAR(50) | enum: PENDENTE, PAGO, CANCELADO |
| `data_criacao` | DATETIME | `@CreationTimestamp` |

**Tabela `comprovantes`:**

| Coluna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT AUTO_INCREMENT PK | |
| `pedido_id` | BIGINT NOT NULL FK | aponta pra `pedidos_pagamento.id` |
| `file_id_telegram` | VARCHAR(255) | |
| `imagem_url` | TEXT | URL no S3 do comprovante |
| `tipo_pagamento` | VARCHAR(255) | string livre (`pix`, `boleto`, `ted`...) |
| `data_pagamento` | DATETIME | `@CreationTimestamp` (gerado quando comprovante é criado) |

### Diferenças vs. o que `especificacao-tecnica.md` (V2 proposta) assume

| Item proposto na V2 | Estado real |
|---|---|
| Tabela `requisitante` | NÃO EXISTE. Só `telegram_user_id` em `pedidos_pagamento`. |
| Coluna `requisitante_id` em pedido | NÃO EXISTE. |
| `data_pedido DATE` em pedido | NÃO EXISTE com esse nome. Existe `data_criacao DATETIME` que serve o mesmo propósito. |
| `data_pagamento DATE` em pedido | NÃO EXISTE em pedido. Existe em `comprovantes.data_pagamento` (DATETIME). |
| `tipo ENUM(BOLETO,PIX,TED,AGENDAMENTO,OUTRO)` em pedido | NÃO EXISTE em pedido. Existe `tipo_pagamento VARCHAR(255)` em **comprovante**, livre. |
| Tabela `auth_token` | NÃO EXISTE. |

### Pontos de atenção do schema atual

- **Tipos de data**: a V2 propõe `DATE` (sem hora) mas o existente é `DATETIME`. Se for filtrar por dia no front (`de=2026-05-01&ate=2026-05-31`), `DATETIME` funciona, só precisa de `DATE(data_criacao)` nas queries. Não é problema, é só preferência.
- **`telegram_user_id` é VARCHAR(255)**: superdimensionado. Não importa pro MVP.
- **Sem soft delete, sem `updated_at`**: aceitável pra esse caso de uso.
- **Sem índices customizados na schema atual**: para volume atual (poucas centenas) tudo bem. Quando passar de alguns milhares de linhas (1 ano de uso), vale criar índice em `(telegram_user_id, data_criacao DESC)`.

---

## 5. Endpoints REST existentes

**Apenas um:** `POST /webhook`

- Implementado em `TelegramWebhookController.java`
- Recebe um `Update` do Telegram (deserialização da biblioteca `telegrambots-meta`)
- Valida que tem mensagem e usuário
- Autoriza o usuário contra `telegram.allowed-user-ids` (lista hardcoded em properties)
- Roteia pra `UpdateOrchestratorService` que escolhe a strategy
- Retorna `200 OK` vazio

**Endpoints da camada de visualização (`/api/v1/pedidos`, `/api/v1/comprovante`, `/api/v1/auth/*`, `/api/v1/resumo`):** **NENHUM EXISTE.** A `especificacao-tecnica.md` propõe; nada foi construído ainda.

**Swagger UI:** disponível em `/swagger-ui.html` quando aplicação rodando (graças ao `springdoc-openapi`). Hoje só mostra o `/webhook`.

---

## 6. Configuração

### Profiles

| Profile | Arquivo | Origem das credenciais |
|---|---|---|
| default | `application.properties` | placeholders `CHANGE_ME` |
| dev | `application-dev.properties` | local (gitignored) — tem `application-dev.properties.example` versionado |
| prod | `application-prod.properties` | AWS Secrets Manager via `spring.config.import=aws-secretsmanager:finbot-prod-secrets` |

### Configurações relevantes em prod

- Porta: 8443 (HTTPS, certificado em `/opt/finbot/keystore.p12`)
- `ssl.key-store-password=finbot123` — **TODO segurança:** essa senha está hardcoded no properties. Deveria estar no Secrets Manager. Não bloqueia o front, mas é dívida pendente.
- `spring.jpa.hibernate.ddl-auto=none` em prod (correto — sem DDL automático)
- `spring.jpa.hibernate.ddl-auto=update` no default (perigoso pra prod, mas acaba sobrescrito pelo profile)
- `telegram.allowed-user-ids=7436345622` em prod — apenas 1 user ID autorizado (deve ser o seu pessoal)

### Migrações SQL

Não tem **Flyway** nem **Liquibase**. Os arquivos `migration.sql` e `migration_s3_integration.sql` são scripts manuais. `schema.sql` é o esquema base. **Implicação:** a migração V2 proposta na `especificacao-tecnica.md` precisa ou ser aplicada manualmente em dev/prod, ou (melhor) entrar a oportunidade de adicionar Flyway agora antes de fazer mais migrations.

**Recomendação forte:** adicionar Flyway antes de fazer a próxima migration. O custo é 1 dependência + 1 pasta `db/migration/`, e a partir daí toda evolução de schema fica versionada e automática. Sem isso, esse projeto vai acumular `migration_xxx.sql` soltos e ninguém vai saber qual foi aplicado onde.

---

## 7. Comparação detalhada com `FASE-3-VISUALIZACAO.md`

Análise tarefa-por-tarefa do que `plans/FASE-3-VISUALIZACAO.md` propõe versus o que existe:

### Backend

| Tarefa | Status | Observações |
|---|---|---|
| BE-01 — Migration SQL (requisitante, datas, categoria, auth_token) | **A fazer**, mas **rever** | Precisa adicionar Flyway antes. Schema diverge da V2 proposta — ajustar. Considerar se vale criar `requisitante` agora ou adiar (já tem `telegram_user_id` que serve como identificador único). |
| BE-02 — Domain entities + Repositories | **Parcialmente feito** | `PedidoPagamento`, `Comprovante`, `StatusPedido` já existem. Falta `Requisitante` (se for criar) e os campos novos de data/categoria. |
| BE-03 — Extração de tipo da legenda | **A fazer** | `PaymentRequestStrategy` parseia só valor + descrição (regex `^(\d+([.,]\d{1,2})?)\s+(.+)$`). Precisa estender pra capturar tipo. |
| BE-04 — OpenAPI + DTOs | **A fazer** | `springdoc` já instalado — DTOs anotados geram OpenAPI automático, dispensa YAML manual. |
| BE-05 a BE-09 — Endpoints REST + service de pre-signed URL | **A fazer** | Nada existe ainda. |
| BE-10 — Tabela auth_token + service | **A fazer** | |
| BE-11 a BE-13 — Auth, JWT, CORS | **A fazer** | Reescrever exemplos de WebFlux pra MVC. |
| BE-14 — Testes integração | **A fazer**. Atual só tem `FinancasBotTelegramApplicationTests` (smoke test). Pipeline pula testes (`-DskipTests`). |

### Pontos onde o plano precisa ser ajustado

1. **Reescrever todos os exemplos de WebFlux pra Spring MVC** na `especificacao-tecnica.md` seção "CORS e endpoints autenticados" e na BE-12 (filter de auth).

2. **Considerar adiar `requisitante`** — argumento contrário à criação imediata: hoje só existe 1 usuário, e `telegram_user_id` já cumpre o papel de identificador. Argumento a favor: pensar em multi-requisitante (pai + outros familiares) cedo. Decisão pendente, vale ADR.

3. **Adicionar Flyway antes da migration V2** — vira uma tarefa BE-00 que precede tudo.

4. **OpenAPI vai ser auto-gerado** com `springdoc` — BE-04 muda de "escrever YAML" pra "anotar DTOs e controllers com `@Operation`/`@Schema`/`@Parameter`".

5. **Manter `tipo_pagamento` como VARCHAR ou virar enum**? Hoje é livre em comprovante. Padronizar como enum (BOLETO/PIX/TED/AGENDAMENTO/OUTRO) requer migration + parsing + casting de strings antigas. Vale a pena: facilita filtro no front.

6. **Resolver o conflito de ports** (`PedidoPagamentoRepository` vs `PedidoPagamentoRepositoryPort`). Sugestão: **manter só o `*Port`** e refatorar uses pra ele. Antes de adicionar mais código novo, fechar essa pendência.

7. **Resolver `PaymentCategory.java` vazio** — ou implementa a classe (vira enum ou objeto com hierarquia), ou apaga.

### Frontend

| Tarefa | Status | Observações |
|---|---|---|
| FE-01 — Scaffold | **Feito** (commit `69f1dbe`). Vite + React + TS + Tailwind + ESLint + Prettier. |
| FE-02 — Tipos TS + MSW mock setup | **Em andamento ou feito** — o commit `bedca8e` trouxe arquivos de MSW (`mocks/handlers.ts`, `mocks/browser.ts`, `mockServiceWorker.js`) e tipos (`api/tipos.ts`). Vale validar contra o critério da tarefa. |
| FE-03 a FE-11 | A fazer. |

---

## 8. Recomendações imediatas

Antes de começar qualquer tarefa BE-* nova, **fechar 4 pendências baratas que destravam o resto:**

1. **Adicionar Flyway** (~30 min). Vira o backbone de toda evolução de schema.
2. **Resolver dual port** (`PedidoPagamentoRepository` vs `PedidoPagamentoRepositoryPort`) — escolher um e remover o outro. ~20 min.
3. **Decidir sobre `PaymentCategory`** — implementar como enum ou apagar. ~10 min.
4. **Reativar testes na pipeline** com H2 — está marcado como `-DskipTests` temporário. Quando começar a adicionar endpoints REST de leitura, ter testes vivendo é essencial.

Depois disso, seguir o plano da Fase 3 com os ajustes desta análise. Vou propor uma versão atualizada do `FASE-3-VISUALIZACAO.md` em uma próxima passada — esta análise é a base para reescrever.

---

## 9. Pendências de configuração / segurança / dívida técnica

Anotando aqui pra não esquecer. Não bloqueia a Fase 3, mas vale tratar antes de "GA":

- `server.ssl.key-store-password=finbot123` hardcoded em `application-prod.properties` — mover pro Secrets Manager.
- `telegram.allowed-user-ids` hardcoded em prod (`7436345622`) — quando adicionar o pai, vai precisar de fonte mutável (banco ou Secrets Manager).
- Pipeline com `-DskipTests` — reativar testes (com H2 ou Testcontainers).
- Sem Flyway/Liquibase — adicionar.
- Sem rate limiting na `/webhook` — Telegram protege via token, mas qualquer endpoint REST público vai precisar pensar em rate limit (especialmente o de auth).
- Logs em produção sem `slf4j` em algum nível estruturado (JSON) — quando o tráfego crescer, parsing fica difícil.
- Sem health check específico — só o `/actuator/health` padrão (se ativado). Vale conferir e expor adequadamente pra ELB/CloudFront se necessário.

---

## 10. Próximos passos sugeridos (do meu lado)

1. Atualizar `plans/FASE-3-VISUALIZACAO.md` aplicando os ajustes desta análise.
2. Criar ADR retroativo `0001-spring-mvc-em-vez-de-webflux.md` em `docs/decisions/`.
3. Atualizar `architecture/especificacao-tecnica.md` substituindo exemplos de WebFlux por MVC.
4. Quando a tarefa BE-04 estiver na hora de executar, decidir formalmente o tema OpenAPI (springdoc anotado vs YAML manual).

Ao terminar de ler este documento, me diga se concorda com os pontos levantados, especialmente:

- (a) Adiar `requisitante` ou manter na BE-01?
- (b) Adicionar Flyway agora? (forte recomendação minha)
- (c) Manter `tipo_pagamento` livre ou virar enum?
- (d) Continuar com domain entities anotadas (JPA) ou refatorar pra POJO + JPA entity separada?
- (e) Resolver dual port agora (eliminar `*Repository extends JpaRepository`) ou deixar pra depois?
