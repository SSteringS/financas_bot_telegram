---
task: DEP-06
titulo: "Teste E2E em produção (runbook manual)"
data: 2026-05-27
branch: develop
responsavel: humano
estado: concluido
gates:
  build: na
  lint: na
  testes: na
  testes_total: na
  testes_novos: na
  branch_convencao: na
  territorio: na
commits: []
pr: null
desvios: 0
pendencias_humano: 0
---

# DEP-06 — Teste E2E em produção

## O que foi feito

Runbook `docs/runbooks/RUNBOOK-dep06-e2e-prod.md` executado pelo humano em 2026-05-27.

Todos os cenários passaram:
- Link mágico solicitado e recebido por email ✅
- Exchange JWT → cookie `Domain=satyansaita.com` setado no browser ✅
- Chamadas autenticadas à API (`https://api.satyansaita.com`) sem erro CORS ✅
- Listagem de pedidos carregando corretamente no front (`https://satyansaita.com`) ✅

**Pré-requisitos que estavam no ar:**
- DEP-03: `api.satyansaita.com` → EC2 via Caddy/Let's Encrypt ✅
- DEP-05: CORS + cookie domain corrigidos em `application-prod.properties` ✅
- DEP-04: front buildado e deployado via GitHub Actions (S3 + CloudFront) ✅

## Decisões pendentes

Nenhuma — tarefa fechada.
