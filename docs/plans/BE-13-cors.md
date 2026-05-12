# BE-13 — CORS configurado pra o front consumir a API

> Pequena tarefa de configuração. Sem isso, o front (rodando em `http://localhost:5173` em dev e em outro domínio em prod) não consegue chamar a API por causa da política Same-Origin do browser.

---

## Contexto

Spring MVC. Configuração via `WebMvcConfigurer.addCorsMappings` no `WebMvcConfig` já criado na BE-12 (ou em uma classe nova, tanto faz). Origin permitido vem de property — em dev é `http://localhost:5173`, em prod será o domínio do front.

**Importante:** como o front vai mandar cookie (`finbot_session`) nos requests, precisa de `allowCredentials=true`. Isso implica que **`allowedOrigins` não pode ser `*`** — tem que listar origins específicas. Browsers rejeitam wildcard + credentials.

---

## Pré-requisitos

- BE-12 mergeada (ou disponível em branch local)
- Filter de auth funcionando

---

## Arquivos esperados

**Modificados:**
- `infra/security/WebMvcConfig.java` — adicionar `addCorsMappings`
- `application.properties` — `app.cors.allowed-origin=http://localhost:5173`
- `application-prod.properties` — `app.cors.allowed-origin=${cors_allowed_origin}` ou um valor fixo quando o domínio do front for definido
- `application-dev.properties.example` — exemplo

**Tests:**
- `CorsConfigTest` (verificar preflight OPTIONS e response headers)

---

## Código-chave

### Atualizar `WebMvcConfig`

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequisitanteIdArgumentResolver requisitanteIdResolver;
    private final String corsAllowedOrigin;

    public WebMvcConfig(
            RequisitanteIdArgumentResolver requisitanteIdResolver,
            @Value("${app.cors.allowed-origin}") String corsAllowedOrigin) {
        this.requisitanteIdResolver = requisitanteIdResolver;
        this.corsAllowedOrigin = corsAllowedOrigin;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(requisitanteIdResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins(corsAllowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-Requested-With")
                .exposedHeaders("Set-Cookie")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

CORS NÃO se aplica a `/webhook` (Telegram não usa browser) nem a `/admin/**` (chamado via curl/script).

---

## Critério de aceitação

- [ ] Preflight `OPTIONS /api/v1/pedidos` com `Origin: http://localhost:5173` retorna 200 com headers:
  - `Access-Control-Allow-Origin: http://localhost:5173`
  - `Access-Control-Allow-Credentials: true`
  - `Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS` (ou subset relevante)
  - `Access-Control-Max-Age: 3600`
- [ ] Preflight com `Origin: http://outro-dominio.com` retorna sem o header `Access-Control-Allow-Origin` (browser vai bloquear)
- [ ] Requisição real GET `/api/v1/auth/me` com `Origin: http://localhost:5173` e cookie volta com `Access-Control-Allow-Origin: http://localhost:5173` no response
- [ ] `app.cors.allowed-origin` lido de property, sem hardcode no Java
- [ ] Webhook `/webhook` e admin `/admin/**` **não** estão sujeitos a CORS (rotas server-to-server, sem browser envolvido)
- [ ] `./mvnw test` passa, incluindo teste de preflight via `MockMvc`

---

## Fora de escopo

- Endpoints de leitura de pedidos (BE-05+)
- Definir o domínio final de prod (vai entrar no `app.cors.allowed-origin` quando o domínio existir; até lá, valor placeholder)

---

## Status report

`docs/status/BE-13-cors.md`. Cobrir:

- Output do preflight OPTIONS testado com curl: `curl -I -X OPTIONS http://localhost:8080/api/v1/pedidos -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: GET"`
- Confirmação de que origin diferente não retorna o ACAO header
- Próximo passo: BE-05 (primeiro endpoint REST de leitura, agora com auth + CORS prontos)
