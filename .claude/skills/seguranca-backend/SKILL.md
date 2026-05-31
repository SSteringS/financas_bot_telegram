---
name: seguranca-backend
description: >
  Seguranca no backend — fluxo de auth do projeto (magic link, JWT cookie, admin key,
  isolamento de dados por requisitante_id), vulnerabilidades especificas a monitorar,
  OWASP Top 10 aplicado ao contexto, Spring Security, secrets, input validation.
  Carregar quando a task toca auth, tokens, filtros de seguranca, endpoints admin,
  queries de dados de usuario, ou configuracao de CORS/Actuator.
load_pattern: shared
used_by: [backend, reviewer]
created: 2026-05-31
adr: 0015
status: ativa
---

# Skill — Segurança Backend

## Quando carregar (gatilho explícito)

- Task cria ou modifica endpoint de auth (`/auth/exchange`, `/auth/me`, `/admin/...`).
- Task adiciona endpoint que acessa dados de usuário (queries por `requisitante_id`).
- Task mexe em `JwtAuthenticationFilter`, Spring Security config, ou CORS.
- Task usa variável de config que pode ser secret (JWT key, admin key, DB password).
- **Reviewer:** PR que toca qualquer camada de segurança — verificar antes de aprovar.
- **Sinal concreto:** `JwtAuthenticationFilter`, `X-Admin-Key`, `auth_token`, `finbot_session`,
  `SecurityFilterChain`, `@Valid`, `Secrets Manager`, `CORS`, `/actuator`.

## Fluxo de auth do projeto — referência rápida

```
Admin → POST /admin/.../convite (X-Admin-Key)
          └─ gera token plain (32 bytes random)
          └─ salva token_hash = SHA-256(plain) em auth_token (single-use, 7 dias)
          └─ retorna URL com token plain

Usuário → POST /api/v1/auth/exchange { token: "plain" }
            └─ valida: SHA-256(plain) == token_hash?
            └─ valida: não expirado, não usado
            └─ marca como usado (atomico — previne race condition)
            └─ gera JWT { sub: requisitanteId, exp: 180 dias }
            └─ Set-Cookie: finbot_session=<jwt>; HttpOnly; Secure; SameSite=Lax

Requests subsequentes → Cookie finbot_session presente
  └─ JwtAuthenticationFilter: valida JWT, injeta requisitanteId no SecurityContext
  └─ Controllers: leem requisitanteId do contexto — NUNCA do body/query param do front
  └─ Queries: sempre filtradas por requisitante_id — front não pode ver dados de outro
```

## Vulnerabilidades críticas do fluxo — o que verificar

| Risco | Sintoma no código | Impacto |
|---|---|---|
| **Token plain em banco** | coluna `token` (não `token_hash`) em `auth_token` | Token vaza em dump de DB → autenticação comprometida |
| **Single-use não atômico** | UPDATE e SELECT separados sem transação | Race condition → mesmo link usado duas vezes |
| **requisitante_id do front** | `@RequestParam Long reqId` ou body com `requisitanteId` | Usuário A vê dados de B passando o id de B |
| **JWT secret fraco/hardcoded** | `secret = "minha-chave"` em properties | JWT forjável offline |
| **Token de convite em log** | `log.info("token={}", tokenPlain)` | Token vaza em log → link hijack |
| **Admin key hardcoded** | `adminKey = "admin123"` em `application.properties` | Qualquer pessoa lê o repo e vira admin |
| **SameSite=Lax em mutações** | POSTs sensíveis sem CSRF token extra | CSRF em requests cross-site iniciadas por terceiros |

## OWASP Top 10 — aplicado ao projeto

| OWASP | Relevância no projeto | Mitigação já em place |
|---|---|---|
| **A01 Broken Access Control** | Query sem filtro por `requisitante_id` | `JwtAuthenticationFilter` + queries sempre filtradas |
| **A02 Cryptographic Failures** | Token plain em banco; JWT secret fraco | SHA-256 no token; secret via Secrets Manager |
| **A03 Injection** | SQL injection em queries nativas | Spring Data JDBC usa queries parametrizadas por default — risco só em `@Query` com concatenação de string |
| **A05 Security Misconfiguration** | CORS com `*`; Actuator exposto; endpoint admin público | CORS com origins explícitas; Actuator só `health`/`metrics` |
| **A07 Identification & Auth Failures** | Token reutilizado; JWT sem expiração; session fixation | single-use enforced; `exp` no JWT; novo JWT a cada exchange |
| **A09 Security Logging Failures** | Token/JWT/senha em log | Nunca logar tokens, JWTs, senhas, CPF |

## Spring Security — o que conferir

**Filter chain:**
```java
// Ordem correta: JWT filter ANTES dos filtros de Spring Security padrão
http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

// Endpoints públicos (sem auth)
.requestMatchers("/api/v1/auth/exchange", "/api/v1/auth/me").permitAll()
// Admin: só com X-Admin-Key (filter separado)
.requestMatchers("/admin/**").hasRole("ADMIN")  // ou filtro customizado
// Resto: autenticado
.anyRequest().authenticated()
```

**CORS — nunca `*` com `credentials: true`:**
```java
config.setAllowedOrigins(List.of("https://finbot.exemplo.com")); // origin explícita
config.setAllowCredentials(true); // necessário pro cookie
// allowedOrigins("*") + allowCredentials(true) = erro de segurança E de CORS spec
```

**Actuator — o que expor em prod:**
- ✅ `health`, `metrics`, `info`
- ❌ `env`, `beans`, `mappings`, `heapdump`, `threaddump` — expõem internals e secrets

## Secrets — regras de ouro

- JWT secret, admin key, DB password: **sempre via Secrets Manager** (`finbot-prod-secrets`), nunca em `application.properties` versionado.
- Acesso em runtime via Spring Cloud AWS ou `@Value("${...}")` apontando pra env var injetada pelo systemd.
- **Detectar no PR:** qualquer `password`, `secret`, `key`, `token` com valor literal em properties/yml versionado = reprovação imediata.

## Input validation

```java
// Todo controller que recebe body
@PostMapping("/pedidos")
public ResponseEntity<?> criar(@Valid @RequestBody PedidoRequest req) { ... }

// Anotações mínimas nos DTOs
record PedidoRequest(
    @NotBlank String descricao,
    @Positive BigDecimal valor,          // financeiro: nunca negativo ou zero
    @Min(1) @Max(12) int mes
) {}
```

**Dados financeiros:** validar range (> 0, limite razoável). Nunca processar valor enviado
como string formatada com vírgula — parsear antes.

**Nunca confiar em id do front:** `requisitanteId` vem do JWT, não do request body/param.

## Checklist — implementador e reviewer

- [ ] Token de convite salvo como hash (SHA-256) — nunca o plain.
- [ ] Single-use: UPDATE de `usado_em` atômico com a validação (mesma transação).
- [ ] `requisitanteId` lido do `SecurityContext` — nunca de param/body do front.
- [ ] JWT secret vem de Secrets Manager / env var — não de properties versionado.
- [ ] Nenhum log contém token, JWT, senha, CPF, dados sensíveis.
- [ ] CORS: `allowedOrigins` explícito; nunca `*` com `allowCredentials(true)`.
- [ ] Actuator: só `health`/`metrics`/`info` em prod.
- [ ] `@Valid` em todos os controllers que recebem `@RequestBody`.
- [ ] Queries de dados de usuário filtradas por `requisitante_id` do contexto.

## Ler junto

- `docs/architecture/fluxo-autenticacao.md` — diagrama de sequência completo.
- `docs/architecture/especificacao-tecnica.md` — spec do JWT, cookie e auth_token.
- Skill `ecossistema-spring` — Spring Security no contexto do ecossistema Spring.
