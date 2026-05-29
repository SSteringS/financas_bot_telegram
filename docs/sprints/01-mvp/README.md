# Sprint 01 — MVP (Fase 3: camada de visualização)

**Objetivo (o que definiu "pronto"):** o Pedro (requisitante) acessa um site, entra por link mágico, vê o histórico de pedidos e baixa comprovantes — sem interagir com bot. A operação do bot de Telegram continua igual.

**Resultado:** ✅ **entregue.** MVP em produção (`satyansaita.com`), E2E validado em 2026-05-27 (login por link mágico, pedidos reais, comprovante, PWA, sessão persistindo).

**Retrospectiva:** `docs/retrospectivas/RETRO-01-mvp-fase3.md`.

---

## Escopo entregue

- **Backend (3a):** modelo de dados (requisitante/datas/tipo/auth_token), API REST + OpenAPI, auth por link mágico (token admin → JWT em cookie), pre-signed URLs S3, resumo por mês, handler genérico de exceções do webhook, testes de integração. + EVO-07 (document/PDF).
- **Front (3b):** React+Vite+TS+Tailwind+TanStack Query, PWA, timeline, modal de comprovante, auth guard, codegen de tipos do OpenAPI.
- **Deploy (3c):** Route 53 + ACM, S3 + CloudFront (apex), API atrás de Caddy/Let's Encrypt (`api.satyansaita.com`), pipeline do front via OIDC, CORS/cookie de prod, E2E.

## Índice

- `FASE-3-VISUALIZACAO.md` — master-plan da fase.
- `plans/` — planos das tasks BE/FE/DEP/FIX/HOTFIX/CI + master-prompts overnight.
- `status/` — status reports + resumos overnight.
- `avaliacoes/` — revisões do Reviewer do ciclo.

> Conteúdo movido pra cá a partir do `docs/plans`, `docs/status` e `docs/avaliacoes` globais quando o MVP foi arquivado (ADR 0010).
</content>
