# BE-10 — Entidade Requisitante + auth_token + endpoint admin pra gerar link mágico

> Primeira tarefa do bloco de auth. Cria a entidade `Requisitante` (parte B da BE-02 absorvida) e a entidade `AuthToken`, mais um endpoint admin que gera um link mágico apontando pro front. **Não cria** ainda o `POST /api/v1/auth/exchange` — esse é da BE-11.

---

## Contexto

Pra autenticar o requisitante (pai) no front sem ele lidar com senha, vamos usar **link mágico via WhatsApp**: o admin (você) gera um token único, manda como `?t=<token>` no link, o front troca esse token por uma sessão de longa duração (JWT em cookie HTTP-only). Esta tarefa entrega a primeira parte: a tabela `auth_token`, o domain pra Requisitante (que foi adiado da BE-02), e um endpoint admin `POST /admin/api/v1/requisitantes/{id}/convite` protegido por header `X-Admin-Key`.

---

## Pré-requisitos

- BE-04 (DTOs + springdoc) mergeada em develop ✓
- Tabela `requisitante` e `auth_token` já existem no banco via V2 da BE-01 ✓
- Tabela `pedidos_pagamento` já tem `requisitante_id` (default 1) ✓

---

## Arquivos esperados

**Domain POJOs** em `domain/model/`:
- `Requisitante.java` (POJO puro)
- `AuthToken.java` (POJO puro)

**JPA entities** em `adapters/out/persistence/entity/`:
- `RequisitanteEntity.java`
- `AuthTokenEntity.java`

**Mappers** em `adapters/out/persistence/mapper/`:
- `RequisitanteMapper.java`
- `AuthTokenMapper.java`

**Repositórios Spring Data** em `adapters/out/persistence/`:
- `RequisitanteJpaRepository.java`
- `AuthTokenJpaRepository.java`

**Adapters** em `adapters/out/persistence/`:
- `RequisitanteRepositoryAdapter.java`
- `AuthTokenRepositoryAdapter.java`

**Ports** em `application/port/out/`:
- `RequisitanteRepositoryPort.java`
- `AuthTokenRepositoryPort.java`

**Use case + service:**
- `application/usecases/GerarTokenConviteUseCase.java` (interface)
- `application/services/GerarTokenConviteServiceImpl.java`

**Hash service:**
- `application/port/out/HashService.java` (interface)
- `infra/security/Sha256HashService.java` (impl com SHA-256)

**Controller admin:**
- `adapters/in/rest/admin/AdminController.java`
- Novo DTO `GerarConviteResponse.java` em `application/dto/`

**Config:**
- Adicionar `app.admin.api-key=CHANGE_ME` em `application.properties`
- Adicionar `app.admin.api-key=${admin_api_key}` em `application-prod.properties` (injetado pelo Secrets Manager)
- Adicionar chave `admin_api_key` no `finbot-prod-secrets` (32 bytes random, base64) — **anotar no status report pra você setar no console AWS**
- Adicionar `app.frontend.base-url=http://localhost:5173` em dev e `https://finbot.<dominio>` em prod (placeholder)

**Testes:**
- `RequisitanteMapperTest`
- `AuthTokenMapperTest`
- `RequisitanteRepositoryAdapterTest`
- `AuthTokenRepositoryAdapterTest`
- `Sha256HashServiceTest`
- `GerarTokenConviteServiceImplTest`
- `AdminControllerTest`

---

## Modelo de dados (já existe no banco, só mapear)

```
requisitante: id BIGINT PK, nome VARCHAR(255), telefone VARCHAR(20), email VARCHAR(255), ativo BOOLEAN, criado_em DATETIME

auth_token: token_hash CHAR(64) PK, requisitante_id BIGINT FK, criado_em DATETIME, expira_em DATETIME, usado_em DATETIME NULL
```

---

## Código-chave

### `domain/model/Requisitante.java`

```java
package br.com.satyan.stering.saita.financasbottelegram.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Requisitante {
    private Long id;
    private String nome;
    private String telefone;
    private String email;
    private boolean ativo;
    private LocalDateTime criadoEm;
}
```

### `domain/model/AuthToken.java`

```java
package br.com.satyan.stering.saita.financasbottelegram.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthToken {
    private String tokenHash;       // PK
    private Long requisitanteId;
    private LocalDateTime criadoEm;
    private LocalDateTime expiraEm;
    private LocalDateTime usadoEm;  // null se ainda não usado

    public boolean estaExpirado(LocalDateTime agora) {
        return agora.isAfter(expiraEm);
    }

    public boolean foiUsado() {
        return usadoEm != null;
    }
}
```

### JPA entities

Espelhar o schema das tabelas. `AuthTokenEntity` tem `@Id` em `tokenHash` (`@Column(name = "token_hash", length = 64)`), e `@ManyToOne` em `requisitanteId` apontando pra `RequisitanteEntity`. Mesmo padrão que ComprovanteEntity já usa.

### `application/port/out/RequisitanteRepositoryPort.java`

```java
public interface RequisitanteRepositoryPort {
    Optional<Requisitante> findById(Long id);
    boolean existsById(Long id);
}
```

### `application/port/out/AuthTokenRepositoryPort.java`

```java
public interface AuthTokenRepositoryPort {
    AuthToken save(AuthToken token);
    Optional<AuthToken> findByTokenHash(String tokenHash);
}
```

### `Sha256HashService`

```java
@Service
public class Sha256HashService implements HashService {
    @Override
    public String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes); // 64 chars hex
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
}
```

### `GerarTokenConviteServiceImpl`

```java
@Service
public class GerarTokenConviteServiceImpl implements GerarTokenConviteUseCase {

    private static final Duration TTL = Duration.ofDays(7);
    private static final int TOKEN_BYTES = 32;
    private final SecureRandom random = new SecureRandom();

    private final RequisitanteRepositoryPort requisitanteRepo;
    private final AuthTokenRepositoryPort tokenRepo;
    private final HashService hashService;
    private final String frontendBaseUrl;

    public GerarTokenConviteServiceImpl(
            RequisitanteRepositoryPort requisitanteRepo,
            AuthTokenRepositoryPort tokenRepo,
            HashService hashService,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.requisitanteRepo = requisitanteRepo;
        this.tokenRepo = tokenRepo;
        this.hashService = hashService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public String gerar(Long requisitanteId) {
        if (!requisitanteRepo.existsById(requisitanteId)) {
            throw new RequisitanteNaoEncontradoException(requisitanteId);
        }

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        random.nextBytes(tokenBytes);
        String tokenPlain = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = hashService.hash(tokenPlain);

        LocalDateTime agora = LocalDateTime.now();
        tokenRepo.save(AuthToken.builder()
                .tokenHash(tokenHash)
                .requisitanteId(requisitanteId)
                .criadoEm(agora)
                .expiraEm(agora.plus(TTL))
                .build());

        return frontendBaseUrl + "/entrar?t=" + tokenPlain;
    }
}
```

Criar também `RequisitanteNaoEncontradoException` em `domain/exceptions/`.

### `AdminController`

```java
@RestController
@RequestMapping("/admin/api/v1")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final GerarTokenConviteUseCase useCase;
    private final String adminApiKey;

    public AdminController(
            GerarTokenConviteUseCase useCase,
            @Value("${app.admin.api-key}") String adminApiKey) {
        this.useCase = useCase;
        this.adminApiKey = adminApiKey;
    }

    @PostMapping("/requisitantes/{id}/convite")
    @Operation(summary = "Gera link mágico de convite pra um requisitante", security = @SecurityRequirement(name = "AdminApiKey"))
    public ResponseEntity<GerarConviteResponse> gerarConvite(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Key", required = false) String apiKey) {

        if (apiKey == null || !apiKey.equals(adminApiKey)) {
            log.warn("Tentativa de acesso admin com chave inválida ou ausente");
            return ResponseEntity.status(401).build();
        }

        String url = useCase.gerar(id);
        return ResponseEntity.ok(new GerarConviteResponse(url));
    }
}
```

Criar `GerarConviteResponse.java` como record em `application/dto/` com um único campo `url`.

---

## Critério de aceitação

- [ ] Domain POJOs, JPA entities, mappers, ports, adapters criados pra Requisitante e AuthToken
- [ ] `RequisitanteRepositoryAdapter.findById(1L)` retorna `Optional.of(Requisitante{nome="Satyan Saita"})` (o seed da V2)
- [ ] `Sha256HashService.hash("teste")` retorna string hex de 64 chars determinística
- [ ] `GerarTokenConviteServiceImpl.gerar(1L)`:
  - Cria registro em `auth_token` com hash SHA-256 do token
  - Retorna URL `${frontend.base-url}/entrar?t=<token-plain>`
  - Token plain é random 32 bytes em base64url
  - `expira_em` = `criado_em + 7 dias`
  - `usado_em` é null
- [ ] `GerarTokenConviteServiceImpl.gerar(999L)` (ID inexistente) lança `RequisitanteNaoEncontradoException`
- [ ] `POST /admin/api/v1/requisitantes/1/convite` com header `X-Admin-Key: <chave-correta>` retorna 200 com `{"url": "..."}`
- [ ] Mesmo endpoint sem header ou com header errado retorna 401
- [ ] `application.properties` tem `app.admin.api-key=CHANGE_ME` e `app.frontend.base-url=http://localhost:5173`
- [ ] `application-prod.properties` tem `app.admin.api-key=${admin_api_key}` (do Secrets Manager) — **status report deve lembrar de adicionar a chave `admin_api_key` no segredo `finbot-prod-secrets` antes do deploy**
- [ ] Status report inclui: linha aleatória de exemplo gerada por `openssl rand -base64 32` pro usuário usar como `admin_api_key` em prod
- [ ] `./mvnw test` passa com novos testes verdes (7 classes novas, ~25 testes)
- [ ] Swagger UI mostra o endpoint admin com aba de "Authorize" pra colar a X-Admin-Key

---

## Fora de escopo

- `POST /api/v1/auth/exchange` (BE-11)
- JWT (BE-11)
- Filter de autenticação (BE-12)
- CORS (BE-13)
- Endpoints de leitura de pedidos (BE-05+)

---

## Status report

`docs/status/BE-10-auth-token-admin.md` seguindo `_TEMPLATE.md`. Cobrir:

- Sumário do `mvn test`
- Output de `curl -X POST http://localhost:8080/admin/api/v1/requisitantes/1/convite -H "X-Admin-Key: $CHAVE"` mostrando a URL gerada
- Output do `SELECT token_hash, requisitante_id, expira_em FROM auth_token;` confirmando registro
- **Atenção: anotar a `admin_api_key` aleatória que deve ser adicionada ao `finbot-prod-secrets` antes do deploy em prod**
- Próximo passo: BE-11
