# FIX — Esconder `@RequisitanteId` do Swagger UI

**Data:** 2026-05-13
**Branch:** feature/backend-polish-evo07
**Commit/PR:** (ver commit deste arquivo)
**Responsável (instância):** Claude Code (CLI)

---

## O que foi feito

- Adicionado bloco `static { SpringDocUtils.getConfig().addAnnotationsToIgnore(RequisitanteId.class); }` em `OpenApiConfig.java`.
- Adicionados imports: `SpringDocUtils` (springdoc) e `RequisitanteId` (infra/security).
- O endpoint `GET /api/v1/pedidos` (e demais que usam `@RequisitanteId`) agora não exibe mais o parâmetro `requisitanteId` no Swagger UI — o parâmetro era exibido como query param editável mas era ignorado pelo servidor (o valor real vem do JWT via `RequisitanteIdArgumentResolver`).

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

Nenhuma decisão local relevante — o fix é exatamente o previsto: 1 bloco static, 2 imports.

---

## Decisões pendentes (esperando humano)

Smoke test manual (baixo risco, mas confirma visualmente):
- Subir app em dev e acessar `http://localhost:8080/swagger-ui.html`
- No endpoint `GET /api/v1/pedidos`, a seção "Parameters" **não deve ter** o campo `requisitanteId`
- Os parâmetros de filtro (`status`, `tipo`, `de`, `ate`, `busca`, `page`, `tamanho`) devem continuar aparecendo normalmente

---

## Próximos passos / observações pro próximo

Nenhum. Fix autocontido.

---

## Arquivos criados/modificados

- `financas_bot_telegram/src/main/java/.../infra/OpenApiConfig.java` (modificado: bloco static + 2 imports)
- `docs/status/FIX-hide-requisitanteid-swagger.md` (novo)
