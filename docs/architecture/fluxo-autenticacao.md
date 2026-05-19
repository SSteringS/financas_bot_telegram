# Fluxo de Autenticação — visão de endpoints

Diagrama de sequência mostrando quem chama quem na autenticação do site, da geração do link mágico (pelo operador) até as requests autenticadas do pai consumindo a API.

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   VOCÊ (admin)                  FRONT (browser)              API    │
│       │                              │                         │    │
│       │  [1] POST /admin/...convite  │                         │    │
│       ├──────────────────────────────┼────────────────────────►│    │
│       │                              │                         │    │
│       │              {"url": "...?t=ABC"}                      │    │
│       │◄─────────────────────────────┼─────────────────────────┤    │
│       │                              │                         │    │
│       │  [2] envia link pelo zap     │                         │    │
│       │═════════════════════════════►│                         │    │
│       │                              │                         │    │
│       │   pai clica no link          │                         │    │
│       │                              │                         │    │
│       │                              │  [3] POST /auth/exchange│    │
│       │                              ├────────────────────────►│    │
│       │                              │  Body: {"token":"ABC"}  │    │
│       │                              │                         │    │
│       │                              │  Set-Cookie: finbot_session=...│
│       │                              │  Body: {"requisitante": {...}}│
│       │                              │◄────────────────────────┤    │
│       │                              │                         │    │
│       │                              │  [4] GET /auth/me       │    │
│       │                              │  Cookie: finbot_session │    │
│       │                              ├────────────────────────►│    │
│       │                              │  Body: {"requisitante": {...}}│
│       │                              │◄────────────────────────┤    │
│       │                              │                         │    │
│       │                              │  [5] GET /pedidos, etc  │    │
│       │                              │  Cookie: finbot_session │    │
│       │                              ├────────────────────────►│    │
│       │                              │  Body: ...              │    │
│       │                              │◄────────────────────────┤    │
│       │                              │                         │    │
└─────────────────────────────────────────────────────────────────────┘
```

**Legenda dos passos:**

1. **Admin gera convite** — `POST /admin/api/v1/requisitantes/{id}/convite` autenticado com `X-Admin-Key`. Cria token de uso único hashed em `auth_token`, retorna URL com token plain (válido por 7 dias).
2. **Operador envia URL pelo canal externo** (WhatsApp). Não envolve HTTP da aplicação.
3. **Exchange** — `POST /api/v1/auth/exchange` troca o token plain por sessão. Backend valida (hash, expiração, single-use), marca como usado, gera JWT, retorna como cookie `HttpOnly` de 180 dias.
4. **Verificação da sessão** — `GET /api/v1/auth/me` retorna dados do requisitante autenticado. Usado pelo front no boot pra confirmar sessão viva.
5. **Requisições subsequentes** — qualquer endpoint protegido (`/api/v1/pedidos`, `/api/v1/resumo`, etc) reusa o mesmo cookie. O `JwtAuthenticationFilter` valida em cada request, injeta `requisitanteId` no contexto, e o controller usa pra filtrar dados.

**Convenções gráficas:**

- `──►` requisição HTTP
- `◄──` resposta HTTP
- `══►` canal externo (zap, fora da aplicação)
