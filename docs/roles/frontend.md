# Papel: Frontend

> Delta sobre o `CLAUDE.md` (regras globais valem sempre). Não duplique aqui o que já está lá.

## Objetivo
Implementar o frontend (UI, componentes, hooks, chamadas à API) seguindo o plano da task, com testes e dentro do território.

## Faz
- Código em `frontend/`.
- Testes (componente/hook com lógica não-trivial tem teste — regra do CLAUDE.md).
- Status report ao final, com frontmatter válido.

## NÃO faz
- **Não toca** em `financas_bot_telegram/`/infra nem na estrutura de `docs/plans|architecture|decisions` (só adiciona o próprio status/aprendizado em `docs/`).
- **Não improvisa decisão de produto** — se o plano não cobre, para e pergunta.
- **Não faz push pra `develop`** — para pra revisão.
- **Não usa `as` (type assertion) sem validação** correspondente (lição da avaliação overnight).
- **Não cria branch a partir de outra feature branch** — sempre a partir de `develop`, mesmo que a feature branch de dependência ainda não tenha sido mergeada. Se houver dependência de código ainda não em `develop`, declarar `> EXCEÇÃO DE BRANCH:` no plano e aguardar o merge da dependência antes de começar (regra do CLAUDE.md). Incidente: FE-13 foi criada a partir de `feature/ci-01-gate-pr-develop` em vez de `develop`, gerando violação de convenção detectada só na revisão.

## Restrições
- Branch nova a partir de `develop`: `feature/<id>-<slug>`.
- 1 commit por task, mensagem no padrão (`feat(FE-XX): ...`).
- Contrato vem do backend (OpenAPI). MSW é fonte de verdade **temporária** — manter rigorosamente o contrato real; divergência se alinha com o planner/back.

## Checklist do papel (antes de pedir revisão)
- [ ] Branch saiu de `develop`: `git merge-base --is-ancestor origin/develop HEAD` retorna exit 0.
- [ ] `npm test` verde · `npm run lint` limpo · `npm run build` sem erro TS.
- [ ] Componentes/hooks com lógica não-trivial testados.
- [ ] Mobile conferido (viewport ~390px).
- [ ] Status report em `docs/status/<TASK>.md` com frontmatter (gates preenchidos).
- [ ] Passou pelo `PRE-MERGE-CHECKLIST.md`.

## Ler sempre
`CLAUDE.md` · o plano da task em `docs/plans/` · `docs/runbooks/PRE-MERGE-CHECKLIST.md` · `docs/status/_TEMPLATE.md`
