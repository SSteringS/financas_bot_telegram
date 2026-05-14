# BE-11 — POST /api/v1/auth/exchange + JWT + cookie HTTP-only

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- Adicionadas dependências JJWT 0.12.6 (jjwt-api, jjwt-impl, jjwt-jackson) ao `pom.xml`
- Criado `JwtService` — gera JWT HS256 com `sub=requisitanteId`, TTL configurável (180 dias), valida assinatura, detecta necessidade de renovação
- Criado `CookieFactory` — cria cookie HTTP-only `finbot_session` com parâmetros de segurança configuráveis
- Criada exceção `AuthTokenInvalidoException` no domain
- Criado `ExchangeTokenUseCase` (interface) + `ExchangeTokenServiceImpl`:
  - Hash do token plain via `HashService`, busca no banco
  - Valida: token existente, não expirado, não usado
  - Marca `usado_em` no token e persiste
  - Retorna o `Requisitante` dono do token
- Criado `AuthController` em `/api/v1/auth`:
  - `POST /exchange` aceita `{"token": "..."}`, devolve cookie + body `{"requisitante": {...}}`
- Criado `RestExceptionHandler` com `@RestControllerAdvice(basePackages = "...rest")`:
  - `AuthTokenInvalidoException` → 401 + `{"codigo": "TOKEN_INVALIDO", ...}`
  - `MethodArgumentNotValidException` → 400 + `{"codigo": "VALIDACAO", ...}`
- Atualizados `application.properties`, `application-prod.properties`, `application-dev.properties.example`
- 4 classes de teste novas; 110 testes totais, todos verdes

---

## mvn test — resultado

```
Tests run: 110, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

- `app.cookie.domain` usa `finbot.satyan.com.br` como placeholder em prod — ajustar quando o domínio real for definido.
- `RestExceptionHandler` é isolado dos controllers do bot (`adapters.in.telegram`) via `basePackages`.

---

## ⚠️ AÇÃO NECESSÁRIA ANTES DO DEPLOY EM PRODUÇÃO

O `application-prod.properties` injeta `${jwt_secret}` do Secrets Manager (`finbot-prod-secrets`).
**Essa chave ainda não existe no secret.** Antes de fazer deploy, adicionar no console AWS:

```
Chave: jwt_secret
Valor: <gerar com: openssl rand -base64 48>
```

Exemplo de valor (NÃO usar — gerar um novo):
```
k9Xv2mNpQrZsWdFgHjLtUyEaBcOiKl4nMoP7qRsTvXwYzA=
```

**Total de chaves a adicionar ao `finbot-prod-secrets` antes do deploy:**
- `admin_api_key` (BE-10) — gerar com `openssl rand -base64 32`
- `jwt_secret` (BE-11) — gerar com `openssl rand -base64 48`

---

## Decisões pendentes

Nenhuma — tarefa fechada.

---

## Próximos passos

- BE-12: Filter JWT para autenticar endpoints protegidos + `GET /api/v1/auth/me`
  - `JwtService.validarERetornarRequisitanteId()` e `precisaRenovar()` já estão prontos
  - `CookieFactory.criarExpirado()` está pronto para logout

---

## Arquivos criados/modificados

**Novos (produção):**
- `domain/exceptions/AuthTokenInvalidoException.java`
- `infra/security/JwtService.java`
- `infra/security/CookieFactory.java`
- `application/usecases/ExchangeTokenUseCase.java`
- `application/services/ExchangeTokenServiceImpl.java`
- `adapters/in/rest/auth/AuthController.java`
- `adapters/in/rest/RestExceptionHandler.java`

**Modificados:**
- `pom.xml` (JJWT 0.12.6)
- `resources/application.properties`
- `resources/application-prod.properties`
- `resources/application-dev.properties.example`

**Novos (testes):**
- `JwtServiceTest`, `CookieFactoryTest`
- `ExchangeTokenServiceImplTest`
- `AuthControllerTest`
