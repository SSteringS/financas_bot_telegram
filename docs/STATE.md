# STATE — onde o projeto está agora

> **Doc vivo de orientação.** Existe pra uma sessão que começa fria (planner, back, front ou reviewer) se situar em 1 minuto, sem re-derivar contexto. **Curto de propósito.** Detalhe mora nos planos (`docs/plans/`), status reports (`docs/status/`) e ADRs (`docs/decisions/`).
>
> **Última atualização:** 2026-05-26 (pelo planner).
> **Fonte:** este resumo é derivado dos status reports em `docs/status/`. O **estado real de merge em `develop` é do humano** (ele é o integrador — ADR 0004). Quando um status diz "aguardando revisão / não mergeado", está marcado abaixo.

---

## Fase atual

**Fase 3 — Camada de visualização** (plano-mãe: `docs/plans/FASE-3-VISUALIZACAO.md`).
Adiciona uma camada de leitura pro requisitante (Pedro): API REST + front React (PWA) + deploy em AWS, com auth por link mágico. A operação do bot de Telegram continua igual.

Sub-fases: **3a Backend** → **3b Front** → **3c Deploy** → **3d Evolução pós-MVP** (não fazer agora).

---

## Onde estamos

### 3a — Backend: **concluída** (conforme status reports)
API REST completa: DTOs+OpenAPI (BE-04), listagem/detalhe/resumo (BE-05/06/09), pre-signed URL + endpoints de imagem (BE-07/08), auth completa (token admin → exchange JWT → filtro → CORS, BE-10 a BE-13), testes de integração com Testcontainers (BE-14), handler genérico de exceções do webhook (BE-15, gate de deploy), e o resumo parametrizado por mês/busca (BE-16). Base (Flyway, refatoração de persistência, backfill de testes) em BE-00/00B/01a. Polish + EVO-07 (aceitar document/PDF) na branch `feature/backend-polish-evo07` (212 testes, BUILD SUCCESS).

### 3b — Front: **concluída** (conforme status reports)
Fase 3b inteira (FE-03 a FE-11) na branch `feature/frontend-fase3-completa`. Em cima dela, **FE-12** corrigiu os dois bugs da revisão (contadores que colapsavam ao filtrar; header travado no mês corrente) ligando o front ao contrato `mes`/`busca`/`todos` da BE-16. **FE-12 estava como "aguardando revisão, não mergeado" no seu último status** — confirmar com o humano se já entrou em `develop`.

### 3c — Deploy: **em andamento**
- **DEP-00** (domínio): ✅ resolvido — domínio é **`satyansaita.com`**.
- **DEP-01** (Route 53 + ACM): ✅ concluído.
- **DEP-02** (S3 + CloudFront do front): ✅ concluído. Front no apex `satyansaita.com` (bucket `finbot-frontend-prod-776658251579`, distribuição `E1WG4Q8MG3V9HY`), HTTPS válido, SPA fallback ok.
- **DEP-03** (subdomínio `api.satyansaita.com` → EC2 atrás de proxy reverso): ⏳ pendente.
- **DEP-04** (GitHub Actions: build + sync S3 + invalidate CloudFront, via OIDC): ⏳ pendente. Consome `frontend_bucket_name` e `cloudfront_distribution_id` do DEP-02.
- **DEP-05** (prod do back: CORS `allowed-origin` + cookie `Domain=.satyansaita.com`): ⏳ pendente.
- **DEP-06** (teste E2E em prod): ⏳ pendente.

---

## Próximos passos prováveis

1. Confirmar/efetuar merge das branches da Fase 3 em `develop` (back polish/EVO-07 e front, incluindo FE-12).
2. Tocar o trilho de deploy restante: **DEP-03 → DEP-04 → DEP-05 → DEP-06**.
3. Itens de workflow (ver abaixo).

---

## Pendências bloqueantes / ações manuais antes do deploy

- ⚠️ **Secrets Manager:** adicionar a chave `keystore_password` (valor `finbot123`) no secret `finbot-prod-secrets`. Sem isso o app **não sobe em prod** (origem: `FIX-keystore-password-secret`).
- **Ícones do PWA são placeholders** — trocar os PNGs (`icone-192/512`, `apple-touch-icon`) por arte real antes do deploy do front.
- Demais débitos: `docs/PENDENCIAS-TECNICAS.md`.

---

## Workflow / processo (meta, não-produto)

Backlog vivo em `docs/plans/BACKLOG-evolucao-workflow.md`. Estado dos itens em voo:

- **FIX-gitattributes-eol:** ✅ feito (mata o drift CRLF/LF que aparecia no `terraform plan`). Falta só o PR pra `develop`.
- **CI-01** (gate de CI no caminho pra `develop`): plano escrito. **Decidido (2026-05-26): PR→develop, com o CI rodando no PR.** Branch protection **adiada** (humano não vai ligar por ora) → o gate é **informativo, não bloqueante**. Pendente: execução do `ci.yml` pelo back. (Ligar branch protection no GitHub fica pra quando o humano quiser tornar o gate bloqueante.)
- **Codegen do `tipos.ts`** (FE-13): plano escrito (`docs/plans/FE-13-codegen-tipos-openapi.md`). Pendente execução pelo front.
- **Script de métricas** dos status reports: ✅ `docs/scripts/metricas_status.py`. **Regex de task-id** aceita `CI-`: ✅.
- **Reviewer:** **adotado pra toda task (2026-05-26)** — revisão independente em sessão separada antes do merge (ADR 0005). Já em uso.
- **Governança:** ADR 0004 (taxonomia), ADR 0005 (sessões por papel). Retroativos: ADR 0006 (hostnames), ADR 0007 (reporting com gates), ADR 0008 (Terraform módulo único — `Accepted`).

---

## Mapa rápido de onde mora o quê

| Preciso de… | Vou em… |
|---|---|
| O que construir (spec de task) | `docs/plans/` |
| O que foi feito (execução) | `docs/status/` |
| Decisão arquitetural canônica | `docs/decisions/` (ADRs) |
| Regra que o agente obedece | `CLAUDE.md` |
| Conceito pra revisitar | `docs/aprendizado/` |
| Definição de pronto / gates | `docs/runbooks/PRE-MERGE-CHECKLIST.md` |
| Instruções por papel | `docs/roles/` |
| Débito técnico conhecido | `docs/PENDENCIAS-TECNICAS.md` |
</content>
</invoke>
