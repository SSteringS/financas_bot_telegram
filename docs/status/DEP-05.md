---
task: DEP-05
titulo: "CORS + cookie de produção (corrigir domínio)"
data: 2026-05-27
branch: feature/dep-05-cors-cookie-prod
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 226
  testes_novos: 0
  branch_convencao: ok
  territorio: ok
commits:
  - 30dd0b1
pr: null
desvios: 0
pendencias_humano: 0
---

# DEP-05 — CORS + cookie de produção (corrigir domínio)

## O que foi feito

Corrigidos três valores em `financas_bot_telegram/src/main/resources/application-prod.properties`:

```properties
app.frontend.base-url=https://satyansaita.com
app.cors.allowed-origin=https://satyansaita.com
app.cookie.domain=satyansaita.com
```

Todos vinham de `finbot.satyan.com.br` (domínio antigo), que bloqueava CORS e impedia o cookie de sessão de colar no navegador.

Grep confirmou que nenhum host está hardcoded no Java — os três pontos de uso (`WebMvcConfig`, `CookieFactory`, `GerarTokenConviteServiceImpl`) leem exclusivamente de `@Value`. Nenhuma mudança de código necessária.

`mvn test`: 226 testes, 0 falhas, BUILD SUCCESS.
`mvn package -DskipTests`: BUILD SUCCESS.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

- `app.cookie.domain=satyansaita.com` (sem ponto inicial) — conforme o plano: RFC 6265 torna o ponto inicial obsoleto, e `satyansaita.com` já cobre o apex e subdomínios (`api.`).

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **Front (território separado):** `frontend/.env.production` ainda tem `VITE_API_BASE_URL=https://api.finbot.dom.br` — precisa virar `https://api.satyansaita.com` antes do build de prod valer. Escopo do Claude do front.
- **DEP-06** é quem confirma a cadeia ponta a ponta: preflight CORS + Set-Cookie com `Domain=satyansaita.com`.
- Verificação real pós-deploy (para o Reviewer confirmar em prod):
  - Preflight `OPTIONS` de `https://satyansaita.com` → `Access-Control-Allow-Origin: https://satyansaita.com` + `Access-Control-Allow-Credentials: true`.
  - `Set-Cookie` do `/api/v1/auth/exchange` → `Domain=satyansaita.com; Secure; HttpOnly; SameSite=Lax`.

---

## Arquivos criados/modificados

- `financas_bot_telegram/src/main/resources/application-prod.properties` (modificado: 3 propriedades de domínio)
