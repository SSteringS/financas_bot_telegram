---
# ─── Frontmatter (schema obrigatório — parseável por script) ───
# Tipos e valores válidos estão em docs/runbooks/PRE-MERGE-CHECKLIST.md
task: BE-005                      # ^(BE|FE|DEP|FIX|HOTFIX|EVO|CI|WF|QA)-\d{3}[a-z]?$  (QA desde 2026-06-01 — ADR 0017)
titulo: "Listar pedidos com filtros"
data: 2026-05-23                  # YYYY-MM-DD
branch: feature/be-005-listar-pedidos
responsavel: claude-back          # claude-back | claude-front | claude-plan | humano
estado: concluido                 # concluido | parcial | bloqueado
gates:
  build: ok                       # ok | fail | na
  lint: ok                        # ok | fail | na
  testes: ok                      # ok | fail | na
  testes_total: 67                # int — total de testes que rodaram
  testes_novos: 12                # int — testes adicionados nesta tarefa
  cobertura_pct: na               # number | na — cobertura de LINHA das classes de PRODUÇÃO que a task tocou (regra provisória, QA-014; `na` quando a task não toca classe de produção)
                                  # BE: ./financas_bot_telegram/mvnw clean jacoco:prepare-agent test jacoco:report -f financas_bot_telegram/pom.xml -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false
                                  #     o `clean` é obrigatório: prepare-agent roda com append=true e soma ao jacoco.exec de runs anteriores
                                  #     → target/site/jacoco/index.html (número é unit-only: SUBESTIMA a real). Detalhe: ROTEIRO-TESTES-BACKEND.md §Camada 1.6
                                  # FE: jest --coverage
                                  # Informativo, não bloqueante. Recorte "classes tocadas" é manual até o item #7 da sprint 04 automatizar.
  branch_convencao: ok            # ok | fail — bate com feature/<id>-<slug> a partir de develop
  territorio: ok                  # ok | fail — tocou só na pasta do território da instância
commits:
  - 18b3f92
pr: null                          # URL do PR ou null
desvios: 0                        # int — quantidade de desvios do plano (detalhar na seção abaixo)
pendencias_humano: 0              # int — decisões aguardando humano (detalhar na seção abaixo)
---

# [TASK-ID] — Título curto da tarefa

> **Não edite este arquivo.** Copie pra `<TASK-ID>-<slug>.md` (ex: `BE-05-listar-pedidos.md`), preencha o frontmatter acima e as seções abaixo.
>
> **Regra de "concluído":** `estado: concluido` só é válido se TODOS os gates relevantes estiverem `ok` (ou `na` quando não se aplica) e `pendencias_humano: 0`. Se algum gate falhar ou houver pendência, use `parcial` ou `bloqueado`. Ver `docs/runbooks/PRE-MERGE-CHECKLIST.md`.

---

## O que foi feito

Prosa curta do que efetivamente entrou no commit. Não repita o critério de aceitação — conte a realidade.

---

## Desvios do plano

Cada desvio precisa de justificativa. O número aqui deve bater com `desvios:` no frontmatter. Se não houve, escreva "Nenhum." e mantenha `desvios: 0`.

---

## Decisões tomadas durante a execução

Decisões locais (nome de variável, escolha de helper, organização de arquivo) que não merecem ADR mas vale registrar.

---

## Decisões pendentes (esperando humano)

Se há qualquer ação que só o humano pode executar — decisão de produto, push/merge, configuração de infra externa, aprovação de PR — registre aqui com uma linha por item. Use `estado: bloqueado` enquanto a ação não for concluída. O número de itens deve bater com `pendencias_humano:`. Se não há nada, escreva "Nenhuma — tarefa fechada." e mantenha `pendencias_humano: 0`.

---

## Próximos passos / observações pro próximo

Gotchas e atalhos que o próximo implementador (ou o planejador) precisa saber.

---

## Padrões técnicos (BE e FE: obrigatório; DEP/CI/EVO: omitir)

> Preencher **apenas em tasks BE-\*, FE-\* ou FIX/\* que envolvem lógica não-trivial**.
>
> **BE:** princípios SOLID aplicados e onde; design patterns usados e por quê; encaixe na arquitetura hexagonal; trade-offs conscientes.
>
> **FE:** padrão React escolhido (Compound Components, custom hook, Context, etc.) e por quê — o problema que o padrão resolveu; referência canônica quando aplicável (ex.: Kent C. Dodds, React Docs, TkDodo); trade-offs conscientes.

_(preencher conforme o tipo de task acima)_

---

## Arquivos criados/modificados

Lista resumida (o git diff tem o exaustivo). Útil pro planejador escanear rápido.

- `caminho/do/arquivo.ext` (novo | modificado: motivo curto)
