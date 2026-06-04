---
task: FIX-002
titulo: "CI aceitar integration/* no gate de branch e nos triggers"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-03
branch_alvo: fix/002-ci-aceitar-integration-no-gate-de-branch
prioridade: alta
esforco: baixo
territorio: back
estado: concluido
depende_de: []
bloqueia: [BE-024, BE-025, BE-026, BE-027, BE-028, BE-029, FE-015, FE-016, FE-017]
skills_dispatched: []
integration_branch: null
fluxos_qa: []
---

# FIX-002 — CI aceitar `integration/*` no gate de branch e nos triggers

## Intake

- **Origem:** PR `integration/03-folha-pagamento → develop` bloqueado pelo job `branch-name` do `ci.yml`. Diagnosticado em 2026-06-03 após adoção do fluxo feature→integration→develop (documentado no `CLAUDE.md §Fluxo de branches`).
- **Por quê agora:** bloqueia o merge de integration→develop ao fim da sprint — é o gate humano de qualidade. Sem esta correção, nenhuma sprint com integration branch consegue fechar.
- **Esforço:** baixíssimo — 3 linhas no `ci.yml`.
- **Riscos resumidos:** risco zero de regressão funcional (só adiciona prefixo aceito e triggers). Risco leve de CI rodar em mais contextos (feature→integration), que é o comportamento desejado.

---

## Contexto

O `ci.yml` (`.github/workflows/ci.yml`) tem um job `branch-name` com esta validação:

```bash
REF="${{ github.head_ref || github.ref_name }}"
if [[ ! "$REF" =~ ^(feature|fix|hotfix)/[a-z0-9]+(-[a-z0-9.]+)*$ ]]; then
  echo "::error::Branch '$REF' fora da convenção..."
  exit 1
fi
```

O grupo de prefixos `(feature|fix|hotfix)` não inclui `integration`. Quando o PR `integration/03-folha-pagamento → develop` é aberto, `github.head_ref = integration/03-folha-pagamento`, que não bate no regex → CI falha → PR bloqueado.

Adicionalmente, o trigger `pull_request: branches: [develop]` só dispara CI em PRs que **chegam** em develop — ou seja, CI não roda quando uma feature branch abre PR para a integration branch (feature→integration). Isso deixa o feedback loop mais lento nesse passo.

---

## Decisão / abordagem

Três mudanças mínimas e cirúrgicas no `ci.yml`:

### 1. Regex do `branch-name` — adicionar `integration`

```bash
# De:
^(feature|fix|hotfix)/[a-z0-9]+(-[a-z0-9.]+)*$

# Para:
^(feature|fix|hotfix|integration)/[a-z0-9]+(-[a-z0-9.]+)*$
```

Cobre `integration/03-folha-pagamento` e qualquer `integration/<NN>-<slug>` futuro.

### 2. Trigger `pull_request` — adicionar `integration/**`

```yaml
# De:
pull_request:
  branches: [develop]

# Para:
pull_request:
  branches: [develop, 'integration/**']
```

CI passa a rodar também em PRs feature→integration, dando feedback antes do merge na integration.

### 3. Trigger `push` — adicionar `integration/**`

```yaml
# De:
push:
  branches:
    - 'feature/**'
    - 'fix/**'
    - 'hotfix/**'

# Para:
push:
  branches:
    - 'feature/**'
    - 'fix/**'
    - 'hotfix/**'
    - 'integration/**'
```

CI roda em pushes diretos à integration (ex.: quando o planner sincroniza develop→integration).

---

## Escopo / arquivos

### Modificar
- `.github/workflows/ci.yml` — 3 mudanças pontuais descritas acima.

### Não tocar
- `.github/workflows/deploy.yml` — deploy só dispara em merge para `main`; não precisa de ajuste.
- `.github/workflows/deploy-frontend.yml` — idem.
- Qualquer arquivo fora de `.github/`.

---

## Testes

Não aplicável (sem código de produção). Validação: abrir um PR de `fix/002-...` para `develop` e confirmar que o job `branch-name` passa. Após merge, confirmar que o PR `integration/03-folha-pagamento → develop` tem CI verde.

`testes_novos: 0`.

---

## Critérios de aceitação

- [ ] Job `branch-name` passa em PR com `head_ref = integration/03-folha-pagamento`.
- [ ] Job `branch-name` continua passando em PR com `head_ref = feature/be-024-...`.
- [ ] Job `branch-name` continua passando em PR com `head_ref = fix/001-...`.
- [ ] CI dispara em PR `feature/be-024 → integration/03-folha-pagamento` (pull_request trigger).
- [ ] CI dispara em PR `integration/03-folha-pagamento → develop` (pull_request trigger).
- [ ] `build` e `testes` (backend/frontend) continuam rodando condicionalmente por `paths-filter`.
- [ ] Branch: `fix/002-ci-aceitar-integration-no-gate-de-branch` saindo de `develop`.
- [ ] 1 commit: `fix(CI): aceitar integration/* no gate de branch e nos triggers`.
- [ ] Status report em `docs/sprints/03-folha-pagamento/status/FIX-002-*.md`.

---

## Fora de escopo

- Branch protection rules no GitHub (configuração da UI do repositório) — não é arquivo de código; decidir separadamente.
- Qualquer mudança nos jobs `backend` ou `frontend` — estão corretos.
- Adicionar outros prefixos além de `integration` — não há demanda agora.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| CI rodar em mais PRs (feature→integration) e consumir mais Actions minutes | Baixa | Baixo | `paths-filter` já limita jobs `backend`/`frontend` — só roda o necessário |
| Regex aceitar branch mal-formada com prefixo `integration` | Baixa | Baixo | O restante do pattern `[a-z0-9]+(-[a-z0-9.]+)*` ainda valida o slug |

---

## Coordenação

- **Pode rodar em paralelo com:** QA-004 (front, território disjunto).
- **Depende sequencialmente de:** nada — sai direto de `develop`.
- **Bloqueia:** merge de `integration/03-folha-pagamento → develop` ao fim da sprint. Todas as BE/FE da sprint 03 ficam presas se este FIX não entrar antes do fechamento.
- **Atenção pro Reviewer:** validar que o regex não quebrou casos existentes. Rodar mentalmente: `feature/be-024-...`, `fix/001-...`, `hotfix/001-...`, `integration/03-folha-pagamento`.
- **Após merge:** PR `integration → develop` pode ser aberto. Atualizar `STATE.md`.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build `na`, lint `na`, testes `na`, branch_convencao `ok`, territorio `ok`), status report válido, revisão do Reviewer. PR `fix/002-... → develop` (fix vai direto para develop — não passa por integration).

---

## Referências

- `.github/workflows/ci.yml` — arquivo a modificar
- `CLAUDE.md §Fluxo de branches` — regra canônica feature→integration→develop
- `CLAUDE.md §CI/CD` — contexto do pipeline
