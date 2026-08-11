# CLAUDE.md — Backend (financas_bot_telegram/)

## Responsabilidade

Este módulo é o backend do projeto. O Claude do **front não deve tocar nesta pasta**.

## Stack

- Java 21, Spring Boot 3.4.5, Spring MVC (não WebFlux)
- Spring Data JPA + MySQL (HikariCP)
- Spring Cloud AWS 3.2.0 (S3 + Secrets Manager)
- Maven

## Arquitetura hexagonal

Package raiz: `br.com.satyan.stering.saita.financasbottelegram`

```
adapters/
  in/
    rest/              ← API REST consumida pelo frontend
      RestExceptionHandler.java   ← @RestControllerAdvice restrito a adapters.in.rest
      admin/ auth/ pedido/ resumo/ funcionario/ folha/
    telegram/          ← webhook do Telegram
      controller/ mapper/ exception/ exceptionhandler/
    whatsapp/          ← webhook da Meta Cloud API
      controller/ mapper/ dto/ security/ exception/ exceptionhandler/
  out/
    persistence/       ← JpaRepository + RepositoryAdapter + entity/ + mapper/
    s3/service/        ← upload para S3
    telegram/          ← service/ (envio, download) + notificador/
    whatsapp/          ← service/ (envio, download de mídia) + dto/
application/
  port/in/             ← ports de entrada (padrão ATUAL)
  port/out/            ← ports de saída (repositórios, storage, notificador)
  services/            ← implementações dos casos de uso (@Service)
  strategy/            ← Strategy por tipo de mensagem (PaymentRequest, PaymentProof)
  dto/                 ← DTOs Lombok, sufixo Request/Response
  usecases/            ← ⚠️ LEGADO — interfaces de caso de uso do padrão antigo
  exceptions/ config/
domain/
  model/               ← POJOs de domínio (PedidoPagamento, Comprovante, Requisitante...)
  entity/              ← ⚠️ POJOs de domínio da folha (Funcionario, Adiantamento) — ver nota
  enums/               ← StatusPedido, TipoArquivo, FormaPagamento, CategoriaPedido...
  service/             ← LegendaParser (lógica de domínio pura)
  event/ exceptions/
infra/
  security/            ← JwtService, JwtAuthenticationFilter, CookieFactory,
                         @RequisitanteId + ArgumentResolver, WebMvcConfig
  AppConfig.java       ← RestClient e beans de infra
  OpenApiConfig.java
```

### Convenções que valem hoje

- **Ports:** o padrão atual é `application/port/in/XxxPortIn` (interface) + `application/services/XxxServiceImpl` (`@Service`). O pacote **`application/usecases/` é legado** — ainda usado pelas verticais antigas (pedido, resumo, auth). **Não criar coisa nova ali.**
- **JPA vive no adapter, não no domínio.** As entidades anotadas com `@Entity` ficam em `adapters/out/persistence/entity/` (`FuncionarioEntity`, `ComprovanteEntity`...). `domain/` tem POJOs puros, sem anotação de persistência.
- **Persistência = 3 arquivos por agregado:** `XxxJpaRepository` (Spring Data) + `XxxRepositoryAdapter` (`@Component`, implementa o port out) + `XxxMapper` (`@Component`, **escrito à mão** — o repo não usa MapStruct).
- **DTOs:** classes Lombok em `application/dto/`, sufixo `Request`/`Response`; response com factory estática `from(...)`. **Não há records.**
- **Validação:** Bean Validation no DTO para regras simples; validação **condicional / cross-field vai no service**, lançando `IllegalArgumentException` → 400 `PARAMETRO_INVALIDO`.
- **Erros REST:** `RestExceptionHandler` com payload `ErroDTO = {codigo, mensagem}`. Cada canal tem seu próprio advice, restrito por `basePackages`.
- **Testes de integração:** Testcontainers com **MySQL 8 real** (não H2), `AbstractIntegrationTest` com container singleton, Flyway rodando de verdade, auth JWT real via `autenticarComo(Long)`, limpeza no `@AfterEach` respeitando ordem de FK.
- **Nome de teste de integração é obrigatório, não estilo.** Toda classe de teste que sobe Testcontainers (ou estende `AbstractIntegrationTest`) **deve** terminar em `IntegrationTest`. O `excludedTestClasses` do PIT no `pom.xml` filtra por esse sufixo; um teste de integração com outro nome escapa do filtro e **trava o run do mutation testing** (um container por mutante). A convenção é hoje a única coisa que segura essa exclusão — ver `docs/PENDENCIAS-TECNICAS.md`, "Convenção `*IntegrationTest` não é verificada por nada".

> ⚠️ **Débito conhecido:** `domain/model/` e `domain/entity/` são dois pacotes paralelos para a mesma coisa — `entity/` surgiu na sprint 03 (folha) e `model/` é o original. Ao criar POJO de domínio novo, usar **`domain/model/`**. Registrado em `docs/PENDENCIAS-TECNICAS.md`.

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

## Endpoints

### Webhooks (públicos — sem JWT)

| Rota | Canal |
|---|---|
| `POST /webhook/telegram` | Telegram |
| `POST /webhook/whatsapp` | Meta Cloud API (valida `X-Hub-Signature-256`) |
| `GET /webhook/whatsapp` | handshake de verificação da Meta |

### API REST

| Prefixo | Controller |
|---|---|
| `/api/v1/auth` | `AuthController` |
| `/api/v1/pedidos` | `PedidoController` |
| `/api/v1/resumo` | `ResumoController` |
| `/api/v1/funcionarios` | `FuncionarioController` + `FolhaController` |
| `/admin/api/v1` | `AdminController` (protegido por API key, não por JWT) |

### Autenticação — allowlist positiva

`JwtAuthenticationFilter.shouldNotFilter` usa **allowlist**: tudo sob `/api/` exige JWT, **exceto** `/api/v1/auth/exchange`. `/webhook/**` e `/actuator/**` são públicos.

**Consequência prática:** um controller novo sob `/api/v1/**` já nasce protegido, sem tocar no filtro.

## Migrations

Flyway em `src/main/resources/db/migration/`, formato `V<n>__snake_case.sql`. **Próxima disponível: `V8`** (V1–V7 em uso).

Convenções: MySQL 8 / InnoDB / utf8mb4, `BIGINT AUTO_INCREMENT`, colunas `criado_em`/`atualizado_em`, `CHECK` nomeadas espelhando as regras do service, prefixos `fk_` / `chk_` / `uq_` / `idx_`, cabeçalho em comentário explicando o porquê e o impacto de lock.

## Regras importantes

- Nunca commitar `application-dev.properties` (gitignored)
- Nunca usar `ddl-auto=create` ou `update` em prod — usar migrations Flyway
- Manter a separação de camadas — adapters não conhecem outros adapters
- Novos endpoints REST vão em `adapters/in/rest/<recurso>/`
- **Default de property que aponta pra secret vai no `.properties`, não no `@Value`** — placeholder aninhado não herda o default do externo. Ver `docs/aprendizado/spring-placeholder-aninhado-default.md`

## Critério de mutation testing — gate opcional

| Aspecto | Valor |
|---|---|
| **Métrica** | `test strength` (mortos ÷ **cobertos**), **não** mutation score cru |
| **Piso** | **80%** |
| **Escopo da medição** | **apenas as classes alteradas pela task** — código antigo não entra no denominador |
| **Natureza** | **gate opcional, por task.** O planner pergunta ao humano, ao escrever o plano, se a task adota o gate. Só vale quando o plano declara que adota |
| **Equivalentes** | sobrevivente classificado como equivalente **com demonstração escrita** não conta contra o piso |

**100% não é alcançável** — mutante equivalente (mudança sem efeito observável) é impossível de matar por construção.

- Razão da decisão, trade-offs e alternativas descartadas: **ADR 0021**
- Como rodar, triagem de escopo e leitura do relatório: `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` §Camada 1.5
