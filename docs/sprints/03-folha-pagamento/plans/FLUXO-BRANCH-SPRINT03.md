# Fluxo de branch — Sprint 03 (regra vigente a partir de 2026-06-03)

> Leia este arquivo antes de qualquer task BE/FE desta sprint.

---

## Regra

```
develop   ←  integration/03-folha-pagamento   ←  feature/be-NNN-*
                                               ←  feature/fe-NNN-*
                                               ←  feature/qa-NNN-*
                                               ←  fix/NNN-*   (quando fix for de sprint)
```

| De | Para | Quem abre o PR | Quem aceita |
|----|------|---------------|-------------|
| `feature/*` | `integration/03-folha-pagamento` | implementador (back/front) | implementador (sem revisão humana obrigatória — Reviewer valida o código) |
| `integration/03-folha-pagamento` | `develop` | planner | **humano** (gate de qualidade) |

**develop NÃO aceita PR direto de feature branch.** Toda feature passa por integration primeiro.

**fix/hotfix de emergência** que não pertencem à sprint saem de `develop` e vão direto para `develop` (fluxo legado — ver CLAUDE.md).

---

## Como criar a branch (passo a passo)

```bash
git fetch
git checkout -b feature/<prefix>-NNN-<slug> origin/integration/03-folha-pagamento
```

Confirmar ponto de partida:
```bash
git log --oneline -3
# deve mostrar os commits da integration (QA-001, QA-002 + commits base)
```

---

## Como abrir o PR

- **Base branch:** `integration/03-folha-pagamento`
- **Compare branch:** `feature/<prefix>-NNN-<slug>`
- **Título:** `feat(BE-NNN): <slug curto>` (ou `feat(FE-NNN):`, `feat(QA-NNN):`)
- Implementador pode aceitar o próprio PR após status report + revisão do Reviewer.

---

## Por que esta regra

Antes desta sprint, tasks BE iam direto para `develop`. Isso funciona mas remove a possibilidade de:
1. **Encadear features sequenciais sem conflito** — BE-025 pode mergear em integration enquanto BE-026 ainda está em voo; quando BE-026 abre PR, faz merge de integration na própria branch e resolve conflitos localmente.
2. **Gate humano claro** — o humano revisa o diff de `integration → develop` uma vez, não N PRs separados.
3. **Rollback limpo** — se uma feature tiver problema após o merge em integration, o planner pode abrir um revert sem tocar develop.

---

## Estado atual da integration (2026-06-03)

```
origin/integration/03-folha-pagamento
  afafdb5  Merge PR #83 (QA-002)
  d576310  Merge PR #82 (QA-001)
  55b3cf0  base (criada de develop pré-sprint)
```

> ⚠️ QA-003 está em `develop` mas **não** em `integration`. Isso ocorreu porque QA-003 bypassed o fluxo (PR #85/#86 direto para develop). Consequência: QA-004 precisa partir de `develop` (exceção única — ver DISPATCH-QA-004). Todas as demais tasks partem de `origin/integration/03-folha-pagamento`.

---

## Checklist antes de abrir o PR para integration

- [ ] `build` + `testes` verdes localmente
- [ ] `lint` verde (ou `na`)
- [ ] `git diff --name-only origin/integration/03-folha-pagamento...HEAD` — só arquivos do território desta instância
- [ ] Status report em `docs/sprints/03-folha-pagamento/status/<TASK-ID>-*.md` com frontmatter válido
- [ ] Branch saiu de `origin/integration/03-folha-pagamento` (não de develop)
- [ ] PR apontando para `integration/03-folha-pagamento` (não para develop)
