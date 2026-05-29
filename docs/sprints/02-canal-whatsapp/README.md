# Sprint 02 — Canal WhatsApp + notificação de pagamento + observability

**Status:** 🔜 em discovery/planejamento (PO = humano).

**Objetivo (o que define "pronto"):** o bot/notificações da família passam a viver no **WhatsApp** (onde eles já estão), o Pedro **recebe automaticamente** um aviso quando um comprovante é registrado, e o time deixa de voar cego — **logs e alarmes externalizados** (sem precisar de SSH na EC2).

## Frentes do ciclo

1. **EVO-01 — Canal WhatsApp.** Migrar (ou adicionar) o canal de Telegram pra WhatsApp. A arquitetura hexagonal isola o adapter de entrada. **Decisão de provider em aberto** (Cloud API oficial da Meta vs Z-API/Evolution não-oficiais) — é o **primeiro trabalho do arquiteto** e provavelmente vira **ADR** antes de qualquer código.
2. **EVO-02 — Notificação automática de pagamento.** Quando um comprovante é registrado, disparar mensagem pro requisitante com link pro site. **Depende do canal (EVO-01).**
3. **Observability.** Externalizar logs + alarmes básicos (disco/CPU/ERROR). Caminho provável: **CloudWatch** (a IAM `CloudWatchAgentServerPolicy` já está na EC2). **Fazer cedo no ciclo** — é rede de segurança pra debugar a migração arriscada da EVO-01. Ver `docs/aprendizado/observability-logs-externalizar.md` e ação #4 da RETRO-01.

## Ordem sugerida

Observability **primeiro** (habilitador de debug) → decisão de provider do WhatsApp (ADR) → adapter de entrada (EVO-01) → notificação (EVO-02).

## Estrutura

`plans/` · `status/` · `avaliacoes/` desta sprint entram aqui conforme as tasks forem planejadas (ADR 0010). Retro do ciclo virá em `docs/retrospectivas/RETRO-02-*.md`.

## Pendências de produto (PO)

- Nome/escopo fechado das tasks (aguardando uso real do MVP + decisão do humano).
- Provider do WhatsApp: trade-off custo/risco (ToS) vs simplicidade — comparativo a cargo do arquiteto.
</content>
