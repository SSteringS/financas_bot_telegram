---
name: Estado de testes do projeto em 2026-06-01
description: Fotografia da base de testes do projeto no início do desenho da suíte E2E
type: project
---

Estado de testes mapeado em 2026-06-01, na conversa de desenho do fluxo E2E:

- **Backend (Java/Spring Boot):** ~52 arquivos de teste unitário (JUnit 5 + Mockito + MockMvc) + 7 suites de integração com Testcontainers (MySQL real). Suite de integração cobre: AuthFlow, IsolamentoRequisitante, PedidoDetalhe, PedidosList, Resumo, NotificacaoComprovanteListener, MensagemProcessada. Base classe: `AbstractIntegrationTest`.
- **Frontend (React/TS):** 12 arquivos de teste — **Vitest** (NÃO Jest, corrigindo prompt do agente) + React Testing Library + @testing-library/user-event + jest-dom. MSW v2 já instalado e configurado em `public/`. Coverage via `@vitest/coverage-v8`. Codegen de tipos a partir do OpenAPI via `openapi-typescript` (scripts `gen:api`, `gen:types`).
- **Gaps:** nenhum E2E automatizado front↔back, nenhum E2E automatizado de webhook, nenhum contract test ativo, nenhum a11y automatizado, nenhum teste de carga ou security scan.

**Why:** baseline pra medir evolução das fases futuras (Fase 1 MVP, Fase 2 ROTEIRO completo, Fase 3 pré-deploy).

**How to apply:** ao escrever próximo status report de tasks de teste, usar essa fotografia como baseline pra comparar `testes_total` e `testes_novos`. Ao recomendar ferramenta, conferir se já não está no projeto — Vitest, MSW e codegen OpenAPI já estão instalados; não propor de novo.
