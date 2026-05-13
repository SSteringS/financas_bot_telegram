# BE-13 — CORS configurado pra o front consumir a API

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- Atualizado `WebMvcConfig` com `addCorsMappings()`:
  - Mapping `/api/v1/**`
  - `allowedOrigins(${app.cors.allowed-origin})` — sem hardcode
  - Methods: GET, POST, PUT, DELETE, OPTIONS
  - Headers: Content-Type, Authorization, X-Requested-With
  - `exposedHeaders("Set-Cookie")` — necessário para o front receber o cookie de renovação
  - `allowCredentials(true)` — necessário para o front enviar cookies
  - `maxAge(3600)`
- Webhook (`/webhook`) e admin (`/admin/**`) não mapeados — continuam sem CORS
- Adicionado `app.cors.allowed-origin=http://localhost:5173` em `application.properties`
- Adicionado `app.cors.allowed-origin=https://finbot.satyan.com.br` em `application-prod.properties` (placeholder até o domínio real)
- Adicionado `app.cors.allowed-origin` em `application-dev.properties.example`
- Adicionado `app.cors.allowed-origin=http://localhost:5173` em `application-test.properties`
- Criado `CorsConfigTest` — 3 testes de configuração (smoke tests)
- 127 testes totais, todos verdes

---

## mvn test — resultado

```
Tests run: 127, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Nota sobre o teste de preflight

O `CorsConfigTest` cobre configuração (sem exceção, resolver registrado). Validação do preflight OPTIONS com headers `Access-Control-Allow-Origin` e `Access-Control-Allow-Credentials` será coberta pelos testes de integração da BE-14. Em desenvolvimento, verificar com:

```bash
curl -si -X OPTIONS http://localhost:8080/api/v1/pedidos \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" | grep -i access-control
```

---

## Desvios do plano

Nenhum.

---

## Decisões pendentes

- O domínio definitivo do front em produção ainda não está definido. O placeholder `https://finbot.satyan.com.br` deve ser atualizado quando o domínio real for registrado.

---

## Próximos passos

- BE-05: primeiro endpoint REST de leitura (`GET /api/v1/pedidos`) — infraestrutura completa (auth + CORS) está pronta.

---

## Arquivos criados/modificados

**Modificados:**
- `infra/security/WebMvcConfig.java` (addCorsMappings)
- `resources/application.properties`
- `resources/application-prod.properties`
- `resources/application-dev.properties.example`
- `test/resources/application-test.properties`

**Novos (testes):**
- `CorsConfigTest`
