# Sprint 03 — Folha de Pagamento

**Status:** 🟡 em planejamento (2026-05-31)

**Objetivo:** entregar a gestão de folha de pagamento doméstica — cadastro de funcionários, vales, adiantamentos parcelados e fechamento mensal.

**Spec técnica:** `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md`
**ADR:** `docs/decisions/0016-evo09-folha-pagamento.md` (`Proposed` — aguarda homologação)

---

## Escopo

### Backend (7 tasks)

| ID | Slug | Porte | Depende de |
|----|------|-------|------------|
| BE-023 | migracao-v6-folha-pagamento | P | — |
| BE-024 | entidades-jpa-repositorios-folha | M | BE-023 |
| BE-025 | crud-funcionario | M | BE-024 |
| BE-026 | cadastrar-vale | P | BE-024 |
| BE-027 | cadastrar-adiantamento | P | BE-024 |
| BE-028 | fechar-mes | M | BE-024, BE-026, BE-027 |
| BE-029 | testes-fechar-mes | M | BE-028 |

### Frontend (3 tasks)

| ID | Slug | Porte | Depende de |
|----|------|-------|------------|
| FE-015 | tela-funcionarios | M | BE-025 |
| FE-016 | tela-folha-funcionario | G | BE-026, BE-027, BE-028 |
| FE-017 | modal-fechamento | M | BE-028 |

---

## Ordem de despacho sugerida

1. **BE-023** (habilita tudo)
2. **BE-024** (habilita todos os BE seguintes)
3. **BE-025 + BE-026 + BE-027** (paralelos)
4. **BE-028** (depende de 024, 026, 027)
5. **BE-029** (depende de 028)
6. **FE-015** (pode iniciar em paralelo com BE-025)
7. **FE-016 + FE-017** (dependem de BE-028)

---

## Decisão pendente

> ⚠️ Seção 7 da spec: **vales aparecem na lista do Pedro?** (Opção A — não / Opção B — com tag visual). Bloqueia BE-026 e FE-016. Resolver com PO antes de despachar essas duas tasks.

---

## Fluxo de git desta sprint

Padrão normal de produto: `feature/be-NNN-<slug>` e `feature/fe-NNN-<slug>`, PR pra `develop`, Reviewer obrigatório antes do merge.

---

## Ritual de encerramento

Seguir `docs/runbooks/RUNBOOK-fechamento-sprint.md` — inclui seção `## Agents & Skills` (primeira coleta do feedback loop, estreia na sprint 03).
