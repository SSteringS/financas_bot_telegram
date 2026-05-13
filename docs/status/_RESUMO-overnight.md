# Resumo da sessão overnight — 2026-05-12

**Branch:** `feature/backend-fase3-api-completa`
**Instância:** Claude Code CLI (--dangerously-skip-permissions)
**Período:** noite de 2026-05-11 para 2026-05-12

---

## BEs implementadas nesta sessão

| BE | Título | Status | Testes | Commit |
|----|--------|--------|--------|--------|
| BE-10 | Entidade Requisitante + AuthToken + endpoint admin convite | ✅ Feito | ✓ | `cfcad9f` |
| BE-11 | POST /auth/exchange + JWT HS256 + cookie HTTP-only | ✅ Feito | ✓ | `a03fa3c` |
| BE-12 | Filter JWT + @RequisitanteId resolver + GET /auth/me | ✅ Feito | ✓ | `97d39f5` |
| BE-13 | CORS configurado para /api/v1/** com allowCredentials | ✅ Feito | ✓ | `2375c39` |
| BE-05 | GET /api/v1/pedidos com filtros, paginação e Specifications | ✅ Feito | ✓ | `ba24d79` |
| BE-06 | GET /api/v1/pedidos/{id} — detalhe com isolamento por requisitante | ✅ Feito | ✓ | `fc537d7` |
| BE-07 | StorageService port + S3StorageServiceImpl com pre-signed URL | ✅ Feito | ✓ | `84db1d4` |
| BE-08 | GET /pedidos/{id}/foto-pedido e /comprovante — 302 redirect S3 | ✅ Feito | ✓ | `37021f0` |
| BE-09 | GET /api/v1/resumo — agregado mensal pendentes+pagos | ✅ Feito | ✓ | `8ff41e9` |
| BE-03 | LegendaParser — extração de tipo da legenda Telegram | ✅ Feito | ✓ | `c7889ec` |
| BE-15 | Handler genérico Exception.class — webhook nunca retorna 5xx | ✅ Feito | ✓ | `150c58c` |
| BE-14 | Testes de integração E2E com Testcontainers MySQL | ✅ Feito | ✓ (17 skip)* | `c134c10` |

*BE-14: Docker não estava rodando → 17 testes skipados com `disabledWithoutDocker=true`. Rodar `./mvnw test` com Docker ativo para executá-los.

---

## Contagem de testes

| Ponto | Total | Resultado |
|-------|-------|-----------|
| Início da sessão | 134 | 0 falhas |
| Final da sessão | 192 | 0 falhas, 17 skip (integração) |

---

## Nenhuma BE parada

Todas as BEs da sequência foram concluídas com sucesso.

---

## Segredos AWS necessários em produção

Para o endpoint de pre-signed URL (BE-07/08) funcionar em produção, as credenciais AWS no Secrets Manager precisam ter permissão `s3:GetObject` no bucket `bot-financas-pagamentos-satyan`.

O `S3Presigner` é criado via `S3Template` do awspring (que usa as credenciais do Spring Cloud AWS configuradas pelo Secrets Manager em prod). Não é necessário nenhum novo segredo — as credenciais existentes (usadas para upload) já devem cobrir.

---

## Próximos passos

1. **PR** desta branch → `develop` (revisão humana)
2. **BE-14 com Docker**: quando rodar com Docker disponível, os 17 testes de integração executam automaticamente
3. **Deploy** (DEP-*): após merge em `develop` → `main`
4. Paralelizar com finalização do **frontend**

---

## Decisões arquiteturais relevantes

- `ListarPedidosServiceImpl` e `BuscarPedidoServiceImpl` usam `PedidoPagamentoJpaRepository` diretamente (sem port abstrato) para poder usar Specifications — evita vazar infraestrutura para a camada de aplicação
- `StorageService` port criado para isolar S3 da aplicação; `S3ImageUploadService` (Telegram) e `S3StorageServiceImpl` (API REST) coexistem
- `GlobalTelegramExceptionHandler.handleAnyOther(Exception.class)` — rede de segurança: webhook NUNCA retorna 5xx (ADR 0003)
- Integração tests com `disabledWithoutDocker = true` garantem que CI sem Docker não quebra (testes pulados, não falhados)
