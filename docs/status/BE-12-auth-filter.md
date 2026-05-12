# BE-12 — Filter de autenticação JWT + GET /api/v1/auth/me

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- Criado `RequisitanteId.java` — annotation custom `@Target(PARAMETER)` para injetar `Long requisitanteId` em controllers
- Criado `JwtAuthenticationFilter extends OncePerRequestFilter`:
  - Aplica-se a `/api/v1/**` exceto `/api/v1/auth/exchange`
  - Extrai cookie `finbot_session`, valida JWT, injeta `requisitanteId` como atributo da request
  - Sem cookie → 401 `SESSAO_AUSENTE`; JWT inválido → 401 `SESSAO_INVALIDA`
  - Renovação automática: se `JwtService.precisaRenovar()` retorna true, seta novo cookie no header
  - Webhook, admin, swagger passam sem filtro
- Criado `RequisitanteIdArgumentResolver` — resolve `@RequisitanteId Long` lendo atributo da request
- Criado `WebMvcConfig` — registra o resolver
- Atualizado `AuthController`:
  - Adicionado `RequisitanteRepositoryPort` no construtor
  - Adicionado `GET /api/v1/auth/me` usando `@RequisitanteId Long requisitanteId`
- Testes: `JwtAuthenticationFilterTest` (8 testes), `RequisitanteIdArgumentResolverTest` (4), `AuthControllerMeTest` (2), e `AuthControllerTest` atualizado (construtor com 4 args)
- 124 testes totais, todos verdes

---

## mvn test — resultado

```
Tests run: 124, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Desvios do plano

- O plano mencionava `FilterRegistration.java` como opcional — usado `@Component` direto no filter, que é mais idiomático e suficiente.
- A renovação automática foi testada via mock de `JwtService.precisaRenovar()` (retorna true) em vez de `Thread.sleep` — mantém o teste determinístico.

---

## Decisões tomadas durante a execução

- `RequisitanteIdArgumentResolverTest` usa `when(parameter.getParameterType()).thenAnswer(inv -> Long.class)` para contornar o `getParameterType()` retornar tipo primitivo/raw via Mockito.

---

## Decisões pendentes

Nenhuma — tarefa fechada.

---

## Próximos passos

- BE-13: CORS — configurar `CorsConfigurationSource` para o front `http://localhost:5173` (dev) e `https://finbot.satyan.com.br` (prod)
- BE-05+: endpoints de leitura de pedidos já podem usar `@RequisitanteId Long requisitanteId`

---

## Arquivos criados/modificados

**Novos (produção):**
- `infra/security/RequisitanteId.java`
- `infra/security/JwtAuthenticationFilter.java`
- `infra/security/RequisitanteIdArgumentResolver.java`
- `infra/security/WebMvcConfig.java`

**Modificados:**
- `adapters/in/rest/auth/AuthController.java` (GET /me + construtor com RequisitanteRepositoryPort)

**Novos (testes):**
- `JwtAuthenticationFilterTest`
- `RequisitanteIdArgumentResolverTest`
- `AuthControllerMeTest`
- `AuthControllerTest` (atualizado para novo construtor)
