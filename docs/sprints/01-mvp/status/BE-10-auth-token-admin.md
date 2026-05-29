# BE-10 — Entidade Requisitante + AuthToken + endpoint admin pra gerar link mágico

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- Criados POJOs de domínio `Requisitante` e `AuthToken` (com métodos `estaExpirado` e `foiUsado`)
- Criadas entidades JPA `RequisitanteEntity` e `AuthTokenEntity` (mapeando as tabelas já existentes no banco)
- Criados mappers `RequisitanteMapper` e `AuthTokenMapper`
- Criados repositórios Spring Data `RequisitanteJpaRepository` e `AuthTokenJpaRepository`
- Criados adapters `RequisitanteRepositoryAdapter` e `AuthTokenRepositoryAdapter`
- Criadas ports `RequisitanteRepositoryPort`, `AuthTokenRepositoryPort`, `HashService`
- Implementado `Sha256HashService` usando `MessageDigest` + `HexFormat.of().formatHex()`
- Implementado use case `GerarTokenConviteUseCase` (interface) + `GerarTokenConviteServiceImpl`:
  - Gera 32 bytes aleatórios via `SecureRandom`
  - Codifica em Base64URL sem padding (token plain → enviado na URL)
  - Salva apenas o hash SHA-256 do token (nunca o plain)
  - TTL = 7 dias, `usado_em` = null
- Criado `AdminController` em `/admin/api/v1` com `POST /requisitantes/{id}/convite` protegido por `X-Admin-Key`
- Criado DTO record `GerarConviteResponse(String url)`
- Criada exceção `RequisitanteNaoEncontradoException`
- Adicionados `app.admin.api-key=CHANGE_ME` e `app.frontend.base-url=http://localhost:5173` em `application.properties`
- Adicionados `app.admin.api-key=${admin_api_key}` e `app.frontend.base-url=https://finbot.satyan.com.br` em `application-prod.properties`
- Atualizado `OpenApiConfig` com `SecurityScheme` tipo API_KEY para `X-Admin-Key` (aparece como "Authorize" no Swagger UI)
- 7 classes de teste novas: 95 testes no total, todos verdes

---

## mvn test — resultado

```
Tests run: 95, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Desvios do plano

- O `AdminController` captura `RequisitanteNaoEncontradoException` e retorna 404 — o plano não especificava o comportamento para ID inválido no controller, mas é a resposta mais natural e foi adicionada.
- `AuthTokenRepositoryAdapter.save()` usa `getReferenceById` (não `findById`) para montar a FK sem carregar a entidade inteira — mais eficiente para um INSERT.

---

## Decisões tomadas durante a execução

- Usado `record` Java para `GerarConviteResponse` — zero boilerplate.
- Infra de segurança colocada em `infra/security/` para separar da config geral.

---

## ⚠️ AÇÃO NECESSÁRIA ANTES DO DEPLOY EM PRODUÇÃO

O `application-prod.properties` injeta `${admin_api_key}` do Secrets Manager (`finbot-prod-secrets`).
**Essa chave ainda não existe no secret.** Antes de fazer deploy, adicionar no console AWS:

```
Chave: admin_api_key
Valor: <gerar com: openssl rand -base64 32>
```

Exemplo de valor (NÃO usar esse em produção — gerar um novo):
```
kJ7mP2xQvR9nYsWgHdLtUzAeOiBfCkM4pVwXjNqTlDhE6c=
```

---

## Decisões pendentes

Nenhuma — tarefa fechada.

---

## Próximos passos

- BE-11: `POST /api/v1/auth/exchange` — troca o token plain por JWT em cookie HTTP-only.
  O `AuthTokenRepositoryPort.findByTokenHash()` já está disponível para BE-11 usar.
  A exceção `RequisitanteNaoEncontradoException` também já existe para reutilizar.

---

## Arquivos criados/modificados

**Novos (produção):**
- `domain/model/Requisitante.java`
- `domain/model/AuthToken.java`
- `domain/exceptions/RequisitanteNaoEncontradoException.java`
- `adapters/out/persistence/entity/RequisitanteEntity.java`
- `adapters/out/persistence/entity/AuthTokenEntity.java`
- `adapters/out/persistence/mapper/RequisitanteMapper.java`
- `adapters/out/persistence/mapper/AuthTokenMapper.java`
- `adapters/out/persistence/RequisitanteJpaRepository.java`
- `adapters/out/persistence/AuthTokenJpaRepository.java`
- `adapters/out/persistence/RequisitanteRepositoryAdapter.java`
- `adapters/out/persistence/AuthTokenRepositoryAdapter.java`
- `application/port/out/RequisitanteRepositoryPort.java`
- `application/port/out/AuthTokenRepositoryPort.java`
- `application/port/out/HashService.java`
- `application/usecases/GerarTokenConviteUseCase.java`
- `application/services/GerarTokenConviteServiceImpl.java`
- `application/dto/GerarConviteResponse.java`
- `adapters/in/rest/admin/AdminController.java`
- `infra/security/Sha256HashService.java`

**Modificados:**
- `resources/application.properties` (novas props admin + frontend)
- `resources/application-prod.properties` (novas props admin + frontend)
- `infra/OpenApiConfig.java` (SecurityScheme AdminApiKey)

**Novos (testes):**
- `RequisitanteMapperTest`, `AuthTokenMapperTest`
- `RequisitanteRepositoryAdapterTest`, `AuthTokenRepositoryAdapterTest`
- `Sha256HashServiceTest`
- `GerarTokenConviteServiceImplTest`
- `AdminControllerTest`
