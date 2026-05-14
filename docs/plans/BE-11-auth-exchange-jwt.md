# BE-11 — POST /api/v1/auth/exchange + JWT + cookie HTTP-only

> Troca o token de uso único (gerado pela BE-10) por uma sessão de longa duração materializada como JWT em cookie HTTP-only. **Não cria** ainda o filter que valida o JWT em outros endpoints — esse é da BE-12.

---

## Contexto

O fluxo: o front recebe o link `?t=<token-plain>` (que você gerou via BE-10 e mandou pelo zap), captura o token na URL, chama `POST /api/v1/auth/exchange` com `{"token": "..."}` no body. Esta tarefa entrega:

1. Service que valida o token (verifica hash, expiração, se já foi usado, marca como usado)
2. Service que gera JWT HS256 com `sub=requisitanteId`, 180 dias de validade
3. Controller que recebe a requisição, faz a troca, devolve cookie HTTP-only `finbot_session=<jwt>` e body com dados do requisitante

---

## Pré-requisitos

- BE-10 mergeada em develop (em uma branch local, ou esperar usuário mergear)
- `AuthToken` domain + adapter funcionando
- `Requisitante` domain + adapter funcionando

---

## Dependências novas no `pom.xml`

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

---

## Arquivos esperados

**Novos:**
- `infra/security/JwtService.java` (gera + valida JWT)
- `infra/security/CookieFactory.java` (helper pra criar o cookie HTTP-only)
- `application/usecases/ExchangeTokenUseCase.java` (interface)
- `application/services/ExchangeTokenServiceImpl.java`
- `domain/exceptions/AuthTokenInvalidoException.java`
- `adapters/in/rest/auth/AuthController.java`

**Modificados:**
- `pom.xml` — adicionar jjwt
- `application.properties` — `app.jwt.secret=CHANGE_ME_min_32_chars` e `app.jwt.ttl-dias=180`
- `application-prod.properties` — `app.jwt.secret=${jwt_secret}` (Secrets Manager)
- `application-dev.properties.example` — exemplo de secret

**Tests:**
- `JwtServiceTest`
- `CookieFactoryTest`
- `ExchangeTokenServiceImplTest`
- `AuthControllerTest`

---

## Código-chave

### `JwtService`

```java
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.ttl-dias}") int ttlDias) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret precisa ter >= 32 chars");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofDays(ttlDias);
    }

    public String gerar(Long requisitanteId) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(requisitanteId))
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Long validarERetornarRequisitanteId(String jwt) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    /** True se o JWT está com mais da metade da validade gasta. Usado pra renovação automática. */
    public boolean precisaRenovar(String jwt) {
        Claims c = parseClaims(jwt);
        Instant iat = c.getIssuedAt().toInstant();
        Instant exp = c.getExpiration().toInstant();
        Instant meio = iat.plus(Duration.between(iat, exp).dividedBy(2));
        return Instant.now().isAfter(meio);
    }

    private Claims parseClaims(String jwt) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt).getPayload();
    }
}
```

### `CookieFactory`

```java
@Component
public class CookieFactory {

    private final int maxAgeSec;
    private final boolean secure;
    private final String domain;

    public CookieFactory(
            @Value("${app.jwt.ttl-dias}") int ttlDias,
            @Value("${app.cookie.secure:false}") boolean secure,
            @Value("${app.cookie.domain:}") String domain) {
        this.maxAgeSec = ttlDias * 24 * 60 * 60;
        this.secure = secure;
        this.domain = domain;
    }

    public ResponseCookie criar(String jwt) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from("finbot_session", jwt)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSec);
        if (!domain.isBlank()) b.domain(domain);
        return b.build();
    }

    public ResponseCookie criarExpirado() {
        return ResponseCookie.from("finbot_session", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .build();
    }
}
```

Em `application-prod.properties` adicionar `app.cookie.secure=true`. Em dev fica `false` (HTTP local).

### `ExchangeTokenServiceImpl`

```java
@Service
public class ExchangeTokenServiceImpl implements ExchangeTokenUseCase {

    private final AuthTokenRepositoryPort tokenRepo;
    private final RequisitanteRepositoryPort requisitanteRepo;
    private final HashService hashService;

    // construtor injetando os 3 acima

    @Override
    @Transactional
    public Requisitante exchange(String tokenPlain) {
        String tokenHash = hashService.hash(tokenPlain);
        AuthToken token = tokenRepo.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthTokenInvalidoException("token não encontrado"));

        LocalDateTime agora = LocalDateTime.now();
        if (token.estaExpirado(agora)) {
            throw new AuthTokenInvalidoException("token expirado");
        }
        if (token.foiUsado()) {
            throw new AuthTokenInvalidoException("token já foi usado");
        }

        token.setUsadoEm(agora);
        tokenRepo.save(token);

        return requisitanteRepo.findById(token.getRequisitanteId())
                .orElseThrow(() -> new IllegalStateException("Requisitante referenciado pelo token não existe"));
    }
}
```

### `AuthController`

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final ExchangeTokenUseCase exchangeUseCase;
    private final JwtService jwtService;
    private final CookieFactory cookieFactory;

    // construtor

    @PostMapping("/exchange")
    @Operation(summary = "Troca token de uso único por sessão (cookie HTTP-only)")
    public ResponseEntity<AuthMeResponse> exchange(
            @Valid @RequestBody AuthExchangeRequest req) {

        Requisitante requisitante = exchangeUseCase.exchange(req.token());

        String jwt = jwtService.gerar(requisitante.getId());
        ResponseCookie cookie = cookieFactory.criar(jwt);

        AuthMeResponse body = new AuthMeResponse(
                new RequisitanteDTO(requisitante.getId(), requisitante.getNome()));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }
}
```

`GET /api/v1/auth/me` vem na BE-12 (depende do filter ler o cookie). Não fazer aqui.

### Exception handler REST

Como `AuthTokenInvalidoException` precisa virar 401 com `ErroDTO` no body, criar um handler REST (separado do `GlobalTelegramExceptionHandler` que cuida do bot):

```java
// adapters/in/rest/RestExceptionHandler.java
@RestControllerAdvice(basePackages = "br.com.satyan.stering.saita.financasbottelegram.adapters.in.rest")
public class RestExceptionHandler {

    @ExceptionHandler(AuthTokenInvalidoException.class)
    public ResponseEntity<ErroDTO> handleAuthInvalido(AuthTokenInvalidoException e) {
        return ResponseEntity.status(401)
                .body(new ErroDTO("TOKEN_INVALIDO", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroDTO> handleValidacao(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErroDTO("VALIDACAO", msg));
    }
}
```

Importante: `basePackages = "...rest"` isola esse handler dos endpoints REST e não vaza pro webhook do Telegram (que tem seu próprio `@ControllerAdvice` em `adapters/in/telegram`).

---

## Critério de aceitação

- [ ] `JwtService.gerar(1L)` retorna JWT que ao ser parseado tem `sub=1` e `exp=now+180dias`
- [ ] `JwtService.validarERetornarRequisitanteId(<jwt-gerado>)` retorna `1L`
- [ ] `JwtService.validarERetornarRequisitanteId(<jwt-com-secret-diferente>)` lança `JwtException`
- [ ] `ExchangeTokenServiceImpl.exchange("invalido")` lança `AuthTokenInvalidoException`
- [ ] `ExchangeTokenServiceImpl.exchange(<token-expirado>)` lança `AuthTokenInvalidoException`
- [ ] `ExchangeTokenServiceImpl.exchange(<token-já-usado>)` lança `AuthTokenInvalidoException`
- [ ] `ExchangeTokenServiceImpl.exchange(<token-válido>)` retorna `Requisitante`, marca o token como usado (`usado_em != null` no banco)
- [ ] `POST /api/v1/auth/exchange` com body válido retorna 200 + header `Set-Cookie: finbot_session=...; HttpOnly; ...; Max-Age=15552000` + body `{"requisitante": {"id": 1, "nome": "Satyan Saita"}}`
- [ ] Mesmo endpoint com token inválido retorna 401 + `{"codigo": "TOKEN_INVALIDO", "mensagem": "..."}`
- [ ] Mesmo endpoint com body vazio retorna 400 + erro de validação
- [ ] `app.jwt.secret` carregado de Secrets Manager em prod; **status report alerta pra gerar segredo via `openssl rand -base64 48` e setar no `finbot-prod-secrets`**
- [ ] `./mvnw test` passa com novos testes verdes

---

## Fora de escopo

- `GET /api/v1/auth/me` (BE-12)
- Filter que valida JWT em endpoints protegidos (BE-12)
- Renovação automática de JWT (BE-12, baseado em `precisaRenovar`)
- CORS (BE-13)

---

## Status report

`docs/status/BE-11-auth-exchange-jwt.md`. Cobrir:

- Sumário do `mvn test`
- Exemplo de fluxo curl: `curl -X POST http://localhost:8080/api/v1/auth/exchange -H "Content-Type: application/json" -d '{"token":"<o-token-da-be-10>"}'` mostrando response com cookie no header
- **Atenção: instruir você (humano) a adicionar a chave `jwt_secret` no `finbot-prod-secrets`**
- Próximo passo: BE-12
