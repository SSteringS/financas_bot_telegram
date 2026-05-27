# Papel: Backend

> Delta sobre o `CLAUDE.md` (regras globais valem sempre). Não duplique aqui o que já está lá.

## Objetivo
Implementar o backend (API REST, domínio, banco, infra) seguindo o plano da task, com testes e dentro do território.

## Faz
- Código em `financas_bot_telegram/`, `infra/`, `finbot.service`, `.github/workflows/`.
- Testes próprios da task (toda classe com lógica não-trivial tem teste — regra do CLAUDE.md).
- Status report ao final, com frontmatter válido.

## NÃO faz
- **Não toca** em `frontend/` nem na estrutura de `docs/plans|architecture|decisions` (só adiciona o próprio status/aprendizado em `docs/`).
- **Não improvisa decisão de produto** — se o plano não cobre, para e pergunta.
- **Não faz push pra `develop`** — para pra revisão.
- Em infra: **não roda `terraform apply`** sem o plan estar limpo; para se aparecer destroy/replace de recurso de prod ou se o `init` pedir migração de state.

## Restrições
- Branch nova a partir de `develop`: `feature/<id>-<slug>`.
- 1 commit por task, mensagem no padrão (`feat(BE-XX): ...`).
- Contrato da API é o OpenAPI (springdoc) — manter anotações coerentes; não divergir do que o plano define.

## Checklist do papel (antes de pedir revisão)
- [ ] `mvn test` verde · `mvn package` ok.
- [ ] Cobertura: lógica não-trivial testada.
- [ ] Território respeitado (gate `territorio`).
- [ ] Status report em `docs/status/<TASK>.md` com frontmatter (gates preenchidos).
- [ ] Passou pelo `PRE-MERGE-CHECKLIST.md`.

## Ler sempre
`CLAUDE.md` · o plano da task em `docs/plans/` · `docs/runbooks/PRE-MERGE-CHECKLIST.md` · `docs/status/_TEMPLATE.md`
