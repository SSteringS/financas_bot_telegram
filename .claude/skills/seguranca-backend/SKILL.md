---
name: seguranca-backend
description: >
  Seguranca no backend Spring Boot — padroes de autenticacao (magic link, JWT cookie,
  API key admin), isolamento de dados por tenant, OWASP Top 10 para APIs REST,
  configuracao do Spring Security filter chain, gerenciamento de secrets, input
  validation. Carregar quando a task toca autenticacao, tokens, filtros de seguranca,
  endpoints protegidos, queries de dados de usuario, ou configuracao de CORS/Actuator.
load_pattern: shared
used_by: [backend, reviewer]
created: 2026-05-31
adr: 0015
status: ativa
---

# Skill — Segurança Backend

## Quando carregar (gatilho explícito)

- Task cria ou modifica endpoint de autenticação ou troca de credencial por sessão.
- Task adiciona endpoint que acessa dados específicos de um usuário/tenant.
- Task configura Spring Security, filter chain customizado ou CORS.
- Task usa variável que pode ser secret (JWT key, admin key, DB password).
- **Reviewer:** PR que toca qualquer camada de segurança — verificar antes de aprovar.
- **Sinal concreto:** `JwtAuthenticationFilter`, `SecurityFilterChain`, `@Valid`,
  secrets manager, CORS, `/actuator`, tokens de auth, cookies de sessão.

## Magic Link — implementação segura

```
Geração:
  token_plain = random(32 bytes) — enviado no link, nunca salvo no banco
  token_hash  = SHA-256(token_plain) — salvo no banco
  expiry      = now + TTL definido pela regra de negócio (ex.: 7 dias)
  single_use  = false

Troca:
  1. Receber token_plain do usuário
  2. Calcular SHA-256(token_plain)
  3. Buscar registro por hash (não por plain)
  4. Validar: não expirado, não usado
  5. Marcar como usado — UPDATE atômico (mesma transação da validação)
  6. Gerar sessão/JWT para o usuário autenticado
```

**Riscos críticos do padrão:**
- **Token plain no banco** → vaza com dump do DB. Sempre salvar o hash.
- **Single-use não atômico** → race condition: mesmo link usado duas vezes. UPDATE + validação em transação única.
- **Token em log** → `log.info("token={}", plain)` expõe o link. Nunca logar tokens.

## JWT Cookie — atributos de segurança

```
Set-Cookie: <nome>=<jwt>; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=<segundos>
```

| Atributo | Por quê |
|---|---|
| `HttpOnly` | JS não acessa — XSS não consegue exfiltrar o token |
| `Secure` | Enviado só via HTTPS |
| `SameSite=Lax` | Bloqueia CSRF cross-site; permite redirect externo (magic link) |
| `SameSite=Strict` | Mais seguro, mas quebra fluxos de redirect externo |

**Claims JWT obrigatórios:** `sub` (ID estável do usuário), `exp` (expiração), `iat` (emissão).
Não colocar dados sensíveis (email, nome) em claims sem necessidade — JWT é decodificável sem a chave.

## Isolamento de dados por tenant/usuário

**Regra de ouro:** tenantId / userId vem sempre do `SecurityContext` (JWT validado) — nunca de parâmetro do request.

```java
// ERRADO — front pode forjar qualquer ID
@GetMapping("/recursos")
public List<Recurso> listar(@RequestParam Long userId) { ... }

// CORRETO — ID extraído do JWT pelo filter
@GetMapping("/recursos")
public List<Recurso> listar(Authentication auth) {
    Long userId = (Long) auth.getPrincipal();
    return recursoService.listarPor(userId);
}
```

**Queries de dados devem sempre filtrar por tenant:**
```sql
-- ERRADO
SELECT * FROM recursos WHERE mes = :mes

-- CORRETO
SELECT * FROM recursos WHERE tenant_id = :tenantId AND mes = :mes
```

## OWASP Top 10 — APIs REST Spring Boot

| Categoria OWASP | Como aparece em Spring Boot | Mitigação |
|---|---|---|
| **A01 Broken Access Control** | Query sem filtro por tenant; ID de usuário no body/param | tenantId sempre do SecurityContext; filtrar todas as queries |
| **A02 Cryptographic Failures** | Token plain no banco; JWT secret fraco ou hardcoded | Salvar hash; secret via secrets manager; ≥ 256 bits |
| **A03 Injection** | Concatenação de string em `@Query` nativo | Spring Data usa queries parametrizadas por default; risco só em native query com concat |
| **A05 Security Misconfiguration** | CORS `*`; Actuator exposto; endpoint admin público | CORS com origins explícitas; Actuator: só health/metrics/info |
| **A07 Auth Failures** | Token reutilizável; JWT sem expiração; link permanente | Single-use enforced; `exp` no JWT; TTL no token |
| **A09 Logging Failures** | Token, JWT, senha, CPF em log | Nunca logar dados sensíveis |

## Spring Security — padrões de configuração

**JWT filter — posicionamento correto:**
```java
http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

**Proteção de endpoints:**
```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/public/**", "/auth/exchange").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated()
);
```

**CORS — regra crítica:**
```java
// CORRETO — origin explícita obrigatória com credentials
config.setAllowedOrigins(List.of("https://app.dominio.com"));
config.setAllowCredentials(true);

// ERRADO — viola CORS spec E abre cookies para qualquer origem
config.setAllowedOrigins(List.of("*"));
config.setAllowCredentials(true);
```

**Actuator em prod:** expor só `health`, `metrics`, `info`.
Nunca: `env`, `beans`, `mappings`, `heapdump`, `threaddump` — expõem internals e podem vazar secrets.

## Secrets — regras de ouro

- Nunca em `application.properties` versionado — usar secrets manager ou variáveis de ambiente injetadas pelo runtime.
- Acesso em runtime via `@Value("${...}")` apontando para variável de ambiente.
- **Detectar no PR:** `password`, `secret`, `key`, `token` com valor literal em properties/yml = reprovação imediata.

## Input Validation

```java
// @Valid obrigatório em todos os controllers que recebem @RequestBody
@PostMapping("/recursos")
public ResponseEntity<?> criar(@Valid @RequestBody RecursoRequest req) { ... }

// DTOs — anotações mínimas
record RecursoRequest(
    @NotBlank String descricao,
    @Positive BigDecimal valor,   // financeiro: nunca negativo ou zero
    @Min(1) @Max(12) int mes
) {}
```

Dados financeiros: validar range e tipo. Nunca receber como string formatada com vírgula — converter antes.

## Checklist — implementador e reviewer

- [ ] Tokens armazenados como hash (ex.: SHA-256) — nunca o valor plain.
- [ ] Single-use: validação e marcação como usado atômicas (mesma transação).
- [ ] tenantId/userId lido do `SecurityContext` — nunca de param/body do request.
- [ ] JWT secret vem de secrets manager / env var — não de properties versionado.
- [ ] Nenhum log contém token, JWT, senha, CPF, dados sensíveis.
- [ ] CORS: `allowedOrigins` explícito; nunca `*` com `allowCredentials(true)`.
- [ ] Actuator: só `health`/`metrics`/`info` em produção.
- [ ] `@Valid` em todos os controllers que recebem `@RequestBody`.
- [ ] Queries de dados de usuário filtradas pelo tenantId do SecurityContext.

## Ler junto

- Skill `ecossistema-spring` — Spring Security no contexto do ecossistema Spring Boot.
- `docs/architecture/fluxo-autenticacao.md` — ler para aplicar estes princípios ao fluxo específico do projeto.
