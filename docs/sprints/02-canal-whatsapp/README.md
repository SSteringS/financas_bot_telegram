# Sprint 02 — Canal WhatsApp + notificação de pagamento + observability

**Status:** 🟢 fechamento (escopo revisado 2026-05-29; FIX-001 e BE-17b mergeados 2026-05-30).

**Objetivo revisado (2026-05-29) — o que define "pronto" desta sprint:** o **código completo** do canal WhatsApp está **deployado em prod** (mesmo que **inerte** — endpoints respondendo 403/200 silencioso por sentinelas defensivas), a observability está externalizada (CloudWatch agent + log metric filter + métricas Micrometer), e o front ganhou UX upgrades (botão "ver foto/PDF original"). **"WhatsApp vivo em prod"** — smoke E2E real contra Meta com chip dedicado + Business Verification — **saiu pra próxima sprint** quando esses bloqueios externos estiverem resolvidos.

**Por que o escopo virou:** o humano (PO) não consegue comprar chip dedicado pra WhatsApp Business imediatamente, e o Test number da Meta sofre restrição BR 130497 (Business não-verificada não envia business-initiated). Detalhes em `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md`. Faz mais sentido empilhar o código pronto em prod (com defaults defensivos) do que segurar tudo esperando o chip.

## Frentes do ciclo

1. **EVO-01 — Canal WhatsApp (código).** Adapter de entrada + saída + idempotência + roteamento por `canalPreferido`. **Provedor decidido:** WhatsApp Cloud API oficial da Meta (ADR registrado). **Status:** ✅ código completo mergeado em `develop` (BE-17, BE-18, BE-19, BE-19a, BE-21a, BE-17b PR #79, FIX-001 PR #78). Falta apenas PR `develop → main` pra deploy.
2. **EVO-02 — Notificação automática de pagamento (parcial).** Evento + roteamento por canalPreferido prontos (BE-21a). **`WhatsAppNotificadorImpl` + templates Meta saíram pra próxima sprint** (depende de BE-20 + Business Verification).
3. **Observability.** ✅ DEP-09 (CloudWatch agent + log metric filter `finbot/app/errors`) mergeado; ✅ BE-22 (Micrometer custom metrics ~$7/mês) mergeado. Pendências futuras: BE-22b (alarmes), BE-22c (timers chamadas externas), BE-22d (timer queries DB).
4. **UX no front.** **FE-14** (botão "ver foto/PDF original do pedido") — única task de produto pendente; pronto pra dispatch. Território disjunto do back.

## Estado de cada task

Ver `docs/STATE.md` §"Onde estamos na sprint 02" pra status atualizado de cada task (mergeada / pronta pra dispatch / parqueada).

## Próximos passos imediatos

1. **Despachar FE-14** pro front (única task de produto pendente).
2. **Abrir PR `develop → main`** pra deploy em prod do canal WhatsApp inerte + BE-17b + BE-22 + FIX-001 — **já desbloqueado** pelos merges de hoje. Pode ir em paralelo com FE-14 (FE-14 só toca `frontend/`, deploy é só do back).
3. **RETRO-02** — escrever retrospectiva incluindo itens #8/#9/#10 do `docs/plans/BACKLOG-evolucao-workflow.md` + lições aprendidas (sync gremlin do OneDrive, dois worktrees, convenção FIX-NNN forward-only).
4. **Abrir sprint 03.** Candidatos a escopo: EVO-09 (folha de pagamento — refinar com PO/arquiteto), BE-22b/c/d (alarmes + timers), itens de workflow não cobertos na retro, BE-20 + BE-21b + EVO-02 completa **se** chip + Business Verification chegarem.

## Estrutura

`plans/` · `status/` · `avaliacoes/` desta sprint estão aqui (ADR 0010). Retro do ciclo virá em `docs/retrospectivas/RETRO-02-*.md`.

## Tópicos abertos pra RETRO-02

Registrados em `docs/plans/BACKLOG-evolucao-workflow.md`:
- Item #8 — checklist arquitetural explícito no `reviewer.md`.
- Item #9 — completar adoção da numeração zero-padded pros outros prefixos (BE, FE, DEP, EVO, CI).
- Item #10 — separar configurações de agentes em **roles × skills × workflows**.

Adicionais surgidos durante a sprint (a registrar no backlog se relevantes):
- **Sync gremlin do OneDrive** — writes em arquivos novos podem se perder se Cowork estiver no worktree errado. Mitigação adotada: worktree dedicado pro planner (CLAUDE.md §Worktrees git).
- **Convenção FIX-NNN forward-only** — inaugurada em FIX-001; planos retroativos não são renomeados (CLAUDE.md §Fluxo de branches).
