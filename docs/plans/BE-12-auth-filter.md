# BE-12 — Filter de autenticação JWT (Spring MVC) + GET /api/v1/auth/me

> Cria o filter que lê o cookie `finbot_session`, valida o JWT, e injeta `requisitanteId` no contexto da request. Sem isso, BE-05 em diante não conseguem proteger seus endpoints. Também entrega o `GET /api/v1/auth/me` que dependia do filter rodando.

---

## Contexto

Spring MVC, não WebFlux. Vamos com **filter custom** (extends `OncePerRequestFilter`), **não** Spring Security — adicionar Spring Security pra um único caso de uso de cookie JWT é overengineering. O filter:

1. Aplica-se a `/api/v1/**` exceto `/api/v1/auth/exchange` (rota pública)
2. Lê cookie `finbot_session`
3. Se ausente → 401 com `ErroDTO`
4. Valida JWT via `JwtService` (criado em BE-11). Se inválido → 401
5. Injeta `requisitanteId` como atributo da request (`request.setAttribute("requisitanteId", id)`)
6. Se JWT está com mais da metade da validade gasta, renova: gera novo JWT e seta no header Set-Cookie

Endpoints subsequentes leem `requisitanteId` via parâmetro injetado no controller.

---

## Pré-requisitos

- BE-11 mergeada em develop (ou disponível em branch local)
- `JwtService`, `CookieFactory`, `AuthMeResponse`, `RequisitanteDTO` já existem

---

## Arquivos esperados

**Novos:**
- `infra/security/JwtAuthenticationFilter.java`
- `infra/security/RequisitanteIdArgumentResolver.java` (resolver pra `@RequisitanteId Long id` em controllers)
- `infra/security/RequisitanteId.java` (annotation custom)
- `infra/security/WebMvcConfig.java` (registra o resolver)
- `infra/security/FilterRegistration.java` (registra o filter, ou usar @Component pra ser auto-detectado)

**Modificados:**
- `adapters/in/rest/auth/AuthController.java` — adicionar handler `GET /me`
- `RestExceptionHandler.java` — adicionar handler pra `AuthRequiredException` (401)

**Tests:**
- `JwtAuthenticationFilterTest`
- `RequisitanteIdArgumentResolverTest`
- `AuthControllerMeTest`

---

## Código-chave

### `RequisitanteId` annotation

```java
package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequisitanteId {
}
```

### `JwtAuthenticationFilter`

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "finbot_session";
    private static final String ATTR_NAME = "requisitanteId";
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CookieFactory cookieFactory;

    public JwtAuthenticationFilter(JwtService jwtService, CookieFactory cookieFactory) {
        this.jwtService = jwtService;
        this.cookieFactory = cookieFactory;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Rotas públicas: webhook do Telegram, admin, swagger, actuator, auth/exchange
        return !path.startsWith("/api/v1/")
                || path.equals("/api/v1/auth/exchange");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String jwt = extrairCookie(req);
        if (jwt == null) {
            responder401(res, "SESSAO_AUSENTE", "Cookie de sessão ausente");
            return;
        }

        try {
            Long requisitanteId = jwtService.validarERetornarRequisitanteId(jwt);
            req.setAttribute(ATTR_NAME, requisitanteId);

            // Renovação automática
            if (jwtService.precisaRenovar(jwt)) {
                String novoJwt = jwtService.gerar(requisitanteId);
                res.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.criar(novoJwt).toString());
            }
        } catch (JwtException e) {
            log.debug("JWT inválido: {}", e.getMessage());
            responder401(res, "SESSAO_INVALIDA", "Cookie de sessão inválido ou expirado");
            return;
        }

        chain.doFilter(req, res);
    }

    private String extrairCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if (COOKIE_NAME.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void responder401(HttpServletResponse res, String codigo, String msg) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write(String.format("{\"codigo\":\"%s\",\"mensagem\":\"%s\"}", codigo, msg));
    }
}
```

### `RequisitanteIdArgumentResolver`

```java
@Component
public class RequisitanteIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequisitanteId.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest req = webRequest.getNativeRequest(HttpServletRequest.class);
        Object attr = req.getAttribute("requisitanteId");
        if (attr == null) throw new IllegalStateException("requisitanteId não está no request — filter não rodou?");
        return attr;
    }
}
```

### `WebMvcConfig`

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequisitanteIdArgumentResolver requisitanteIdResolver;

    public WebMvcConfig(RequisitanteIdArgumentResolver requisitanteIdResolver) {
        this.requisitanteIdResolver = requisitanteIdResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(requisitanteIdResolver);
    }
}
```

### `GET /api/v1/auth/me`

Adicionar no `AuthController` (criado em BE-11):

```java
@GetMapping("/me")
@Operation(summary = "Retorna o requisitante autenticado")
public ResponseEntity<AuthMeResponse> me(
        @RequisitanteId Long requisitanteId,
        @Autowired RequisitanteRepositoryPort requisitanteRepo) {

    Requisitante r = requisitanteRepo.findById(requisitanteId)
            .orElseThrow(() -> new IllegalStateException("Requisitante " + requisitanteId + " do JWT não existe"));

    return ResponseEntity.ok(new AuthMeResponse(new RequisitanteDTO(r.getId(), r.getNome())));
}
```

(Ajustar pra injetar o repo no construtor em vez de `@Autowired` no parâmetro — Spring funciona dos dois jeitos mas construtor é mais idiomático. Manter o `@RequisitanteId Long` no parâmetro.)

---

## Critério de aceitação

- [ ] Filter aplicado em `/api/v1/**` exceto `/api/v1/auth/exchange`
- [ ] Sem cookie → 401 com body `{"codigo":"SESSAO_AUSENTE","mensagem":"..."}`
- [ ] Com cookie inválido (assinatura errada / expirado / malformado) → 401 com body `{"codigo":"SESSAO_INVALIDA","mensagem":"..."}`
- [ ] Com cookie válido → request prossegue, `requisitanteId` injetável via `@RequisitanteId Long`
- [ ] `GET /api/v1/auth/me` com cookie válido retorna 200 + `{"requisitante":{"id":1,"nome":"Satyan Saita"}}`
- [ ] `GET /api/v1/auth/me` sem cookie retorna 401 (passa pelo filter)
- [ ] Renovação automática: ao receber request com JWT >50% expirado, response devolve novo cookie no Set-Cookie. **Não testar com mock de tempo — testar com JWT gerado com ttl curto e wait/Thread.sleep**.
- [ ] Webhook do Telegram (`POST /webhook`) **NÃO** passa pelo filter (continua público pra Telegram entregar)
- [ ] Admin endpoint (`POST /admin/api/v1/...`) **NÃO** passa pelo filter (continua protegido por X-Admin-Key)
- [ ] Swagger UI (`/swagger-ui/**`, `/v3/api-docs`) **NÃO** passa pelo filter
- [ ] `./mvnw test` passa

---

## Fora de escopo

- CORS (BE-13)
- Endpoints de leitura de pedidos (BE-05)

---

## Status report

`docs/status/BE-12-auth-filter.md`. Cobrir:

- Sumário do `mvn test`
- Cenários testados via curl: sem cookie, cookie inválido, cookie válido (chamando `/auth/me`)
- Próximo passo: BE-13
