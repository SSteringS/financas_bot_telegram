# DEP-05 — CORS + cookie de produção (corrigir domínio)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** `docs/plans/BACKLOG-produto.md` (Fase 3c, DEP-05) + bug encontrado validando o DEP-03: `application-prod.properties` aponta pra `finbot.satyan.com.br` (domínio antigo) em vez de `satyansaita.com`.
> - **Prioridade:** alta — **bloqueia o front↔API**. Como está, o CORS recusa a origem do front e o cookie de sessão não cola.
> - **Esforço:** baixo (3 linhas de properties; o código já lê de config, nada hardcoded).
> - **Território / quem executa:** `financas_bot_telegram/` (`application-prod.properties`) → **Claude do back**.
> - **Branch:** `feature/dep-05-cors-cookie-prod`, a partir de `develop`.
> - **Dependências:** DEP-02 (front no apex `satyansaita.com`) ✅ e DEP-03 (`api.satyansaita.com` no ar) ✅.
> - **Riscos:** (1) `allowedOrigins` precisa bater **exatamente** (esquema + host, sem barra final) com a origem do front (`https://satyansaita.com`); (2) domínio de cookie errado quebra a sessão silenciosamente; (3) o efeito real só se confirma no E2E (DEP-06).

---

## Contexto

ADR 0006: front no apex `satyansaita.com`, API em `api.satyansaita.com`. São **origens diferentes** (cross-origin → CORS necessário) mas **mesmo site** (eTLD+1 igual → cookie `SameSite=Lax` flui se o `Domain` cobrir o apex). Hoje as três configs de prod apontam pro domínio antigo `finbot.satyan.com.br`, então nada disso funciona em prod.

Wiring confirmado (tudo via `@Value`, nada hardcoded):
- `WebMvcConfig` → `@Value("${app.cors.allowed-origin}")` → `.allowedOrigins(...)`.
- `CookieFactory` → `@Value("${app.cookie.domain:}")`.
- `GerarTokenConviteServiceImpl` → `@Value("${app.frontend.base-url}")` (monta o link mágico do convite).

Logo, **basta corrigir os valores no `application-prod.properties`** — sem mudança de código Java.

## Decisão / abordagem

Corrigir os três valores em `application-prod.properties`:

```properties
app.frontend.base-url=https://satyansaita.com
app.cors.allowed-origin=https://satyansaita.com
app.cookie.domain=satyansaita.com
```

Notas:
- `allowed-origin` **sem barra final** e com `https://` — o navegador compara a origem exata.
- `app.cookie.domain=satyansaita.com` faz o cookie valer pro apex **e** subdomínios (`api.`). Pela RFC 6265 o ponto inicial (`.satyansaita.com`) é obsoleto/ignorado — `satyansaita.com` já cobre. (O `aprendizado/front-api-hostnames-separados.md` menciona `.satyansaita.com`; funcionalmente equivalente.)
- `app.cookie.secure=true` já está setado (ok pra HTTPS).

## Escopo / arquivos

**Modificar:** `financas_bot_telegram/src/main/resources/application-prod.properties` (as 3 linhas).

**Verificar (sem necessariamente mudar):** `WebMvcConfig.java` (BE-13) e `CookieFactory.java` (BE-11) — confirmar que continuam lendo de `@Value` e que não há host hardcoded em nenhum outro ponto (`grep` por `satyansaita`/`finbot.satyan`/`allowedOrigins` no `src/main`).

**Não tocar:** código de produto além disso, nada de `frontend/`, nada de infra.

## Critérios de aceitação

- [ ] As 3 propriedades em `application-prod.properties` apontam pra `satyansaita.com` (frontend-url e allowed-origin com `https://`, sem barra final; cookie domain `satyansaita.com`).
- [ ] `grep` confirma que nenhum host está hardcoded no Java (tudo vem das properties).
- [ ] `mvn test` e `mvn package` verdes (não deve haver impacto em teste, mas rodar).
- [ ] **Verificação real (pós-deploy, no DEP-06 / pelo Reviewer):** preflight `OPTIONS` de `https://satyansaita.com` → `Access-Control-Allow-Origin: https://satyansaita.com` + `Access-Control-Allow-Credentials: true`; e o `Set-Cookie` do `/api/v1/auth/exchange` traz `Domain=satyansaita.com; Secure; HttpOnly; SameSite=Lax`.
- [ ] Status report em `docs/sprints/01-mvp/status/DEP-05.md` com frontmatter válido.

## Coordenação

- **Front (territorio separado):** o `frontend/.env.production` precisa apontar `VITE_API_BASE_URL=https://api.satyansaita.com` (hoje está `api.finbot.dom.br`). Os dois lados (CORS/cookie no back + base-url no front) têm que estar certos pro E2E passar. Alinhar com o Claude do front / o planner.
- **DEP-06** é quem confirma a cadeia inteira de ponta a ponta (login via link mágico, cookie persistindo, chamadas autenticadas).

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build/lint/testes verdes; mudança é de config mas rodar os testes), status report válido, e **revisão independente pelo Reviewer** — toca config de produção (CORS/cookie de segurança). Abrir PR pra `develop`; não mergear sozinho.

## Referências

- ADR 0006 (`docs/decisions/0006-front-e-api-em-hostnames-separados.md`) e `docs/aprendizado/front-api-hostnames-separados.md`, `cors-vs-samesite.md`, `cookies-samesite.md`.
- `docs/sprints/01-mvp/status/DEP-03.md` (onde o bug de domínio foi flagrado).
- `docs/plans/BACKLOG-produto.md` (DEP-05 original).
</content>
