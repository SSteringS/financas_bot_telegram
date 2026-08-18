# Checklist de pré-merge (gates verificáveis)

Define a **definição de pronto** de qualquer tarefa BE/FE/DEP antes de mergear em `develop`. Cada gate é **machine-checkable**: tem um comando e uma condição de passagem. Os gates aqui mapeiam 1:1 com o bloco `gates:` do frontmatter do status report (`docs/templates/_TEMPLATE-status.md`).

Conceito: o status report é um *output schema* (forma garantida, parseável). Este checklist é a *validação* desse output — porque schema válido não garante verdade (um report pode dizer `testes: ok` sem que seja). O Claude de planejamento (ou um script) confere os gates contra a realidade antes do merge.

## Definição de pronto

`estado: concluido` no frontmatter só é válido quando **todos os gates relevantes** estão `ok` (ou `na` quando não se aplicam) **e** `pendencias_humano: 0`. Caso contrário, o estado é `parcial` (entregou parte) ou `bloqueado` (esperando decisão/humano).

## Gates

| Gate (campo no frontmatter) | Como verificar | Passa quando |
|---|---|---|
| `build` | Back: `./mvnw -q -DskipTests package` · Front: `npm run build` | Sai com código 0, sem erro de compilação/TS |
| `lint` | Front: `npm run lint` · Back: `./financas_bot_telegram/mvnw pmd:pmd -f financas_bot_telegram/pom.xml` (PMD, ruleset curado em `financas_bot_telegram/pmd-ruleset.xml`) | Front: sem erros. **Back: `ok` quando o comando roda e o relatório foi lido — não quando o número é zero.** O gate é **informativo, não bloqueante**: PMD não está ligado a fase de build nem ao CI, e o legado tem violações conhecidas de baseline (ver status da QA-013). Violação **nova, em código que a task alterou**, precisa ser corrigida ou justificada por escrito no status. `na` **exige motivo escrito** — ver §Regra do valor `na` |
| `testes` + `testes_total` + `testes_novos` | Back: `./mvnw test` · Front: `npm test` | Todos verdes. Anotar o total e quantos foram adicionados nesta tarefa |
| `cobertura_pct` | Back: `./financas_bot_telegram/mvnw clean jacoco:prepare-agent test jacoco:report -f financas_bot_telegram/pom.xml -Dtest='!*IntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false` → `target/site/jacoco/index.html` · Front: `jest --coverage` | Preenchido com a **cobertura de linha das classes de produção que a task tocou** (regra provisória, QA-014); `na` **exige motivo escrito** — ver §Regra do valor `na`. Gate **informativo, não bloqueante** — não existe threshold, e `jacoco:check` não está configurado de propósito. ⚠️ O `clean` é parte do comando: `prepare-agent` roda com `append=true` e **soma** ao `jacoco.exec` de runs anteriores, o que faz o número deixar de ser unit-only em silêncio. ⚠️ O número do back é **unit-only** (exclui `*IntegrationTest`) e portanto **subestima** a cobertura real. Cobertura 0% inesperada = `argLine` sobrescrito, não ausência de teste. Ver `ROTEIRO-TESTES-BACKEND.md` §Camada 1.6 |
| `branch_convencao` | `git rev-parse --abbrev-ref HEAD` e `git merge-base --is-ancestor origin/develop HEAD` | Nome bate `^(feature/(be|fe|dep|evo|ci|qa)-\d{3}[a-z]?-|fix/\d{3}-|hotfix/\d{3}-)` **e** develop é ancestral (direto para fix/hotfix/integration; via integration para feature/). Pattern: `^(feature/(be|fe|dep|evo|ci|qa)-\d{3}[a-z]?-|fix/\d{3}-|hotfix/\d{3}-|integration/\d{2}-)`. Ver CLAUDE.md |
| `territorio` | `git diff --name-only origin/develop...HEAD` | Todos os caminhos alterados estão dentro do território da instância (ver tabela abaixo) |

### Regra do valor `na` — obrigatória

**Decidida em 2026-08-18.** `na` estava significando duas coisas incompatíveis, e quem lê o report meses depois não distingue:

| O que aconteceu de verdade | Como estava | Como fica |
|---|---|---|
| O gate **não se aplica** à task (task de doc, ou nenhuma classe de produção tocada) | `na` | `na` **+ motivo**: `na — task de documentação, sem código Java` |
| O gate **se aplica mas não foi medido** (tocou Java fora do escopo do PMD; suíte vermelha; instrumentação quebrada; stack sem procedimento definido) | `na` | `na` **+ motivo**: `na — tocou Java fora do escopo configurado do ruleset, nada foi medido` |

Regras:

- **`na` sem motivo escrito ao lado, no corpo do status, é finding do Reviewer.** O valor sozinho não carrega a informação que importa.
- **Nenhum valor novo é criado no schema.** O campo continua aceitando número ou `na`; a distinção vive na prosa. Criar um terceiro valor exigiria mexer em todos os templates e em todos os reports já escritos, por um ganho que o motivo escrito já entrega.
- Vale para **`lint` e `cobertura_pct`** — os dois campos onde a ambiguidade foi observada. Se aparecer num terceiro, a regra se estende sem nova decisão.
- **"Não medi" nunca vira número.** Estimativa, valor de run anterior ou número medido para outra task são proibidos em qualquer gate.

### Territórios (pra o gate `territorio`)

Isolamento vale pra **código**. `docs/` é compartilhada — toda instância pode escrever ali (status report próprio, aprendizados). Mudança estrutural em `docs/plans/`, `docs/architecture/`, `docs/decisions/` é do planejamento.

| Instância (`responsavel`) | Caminhos de código permitidos | + sempre |
|---|---|---|
| `claude-back` | `financas_bot_telegram/`, `infra/`, `finbot.service`, `.github/workflows/`, `scripts/` | `docs/` |
| `claude-front` | `frontend/` | `docs/` |
| `claude-plan` | — | `docs/` (mantenedor da estrutura) |
| qualquer | arquivos da raiz (`CLAUDE.md`, `.gitignore`, `TODO.md`) quando necessário | — |

`scripts/` (scripts de repositório: coleta de custo, recorte de classes tocadas) foi acrescentado ao território de `claude-back` em 2026-08-18, ao planejar a QA-017 — o fluxo já estava definido no `README.md` da sprint 04 ("scripts do repositório → branch + PR + Reviewer"), mas a tabela não listava o caminho, e o gate `territorio` não sabia classificar a task.

O gate `territorio` falha quando uma instância altera **código fora** do seu território (ex.: o front mexendo em `financas_bot_telegram/`). Mudança em `docs/` nunca dispara o gate.

## Checklist operacional (rodar antes de abrir PR / pedir merge)

- [ ] `build` verde
- [ ] `lint` verde (ou `na`)
- [ ] `testes` verdes; `testes_total` e `testes_novos` preenchidos
- [ ] `cobertura_pct` preenchido (cobertura de linha das classes de produção tocadas) ou `na` justificado — informativo, não bloqueia
- [ ] Todo componente/classe com lógica não-trivial tem ao menos 1 teste (regra do CLAUDE.md)
- [ ] `branch_convencao` ok (nome correto + saiu de develop)
- [ ] `territorio` ok (não vazou pra fora da pasta da instância)
- [ ] Se o plano declara `exige_e2e_full: true`: `npm run e2e:full` verde, e campo `e2e_full` preenchido no status report (ver `docs/architecture/desenho-testes-automatizados.md` §9.2 para schema).
- [ ] **Se o diff toca `.claude/`:** validadores verdes — `python .claude/skills/creating-agents/scripts/validate_agent.py <arquivo-ou-diretório>` para mudança em `.claude/agents/`, e `python .claude/skills/creating-skills/scripts/validate_skill.py <diretório-da-skill>` para mudança em `.claude/skills/`. Sai com `OK` e sem warning novo
- [ ] Status report criado em `docs/sprints/<NN>/status/<TASK-ID>-*.md` com frontmatter válido
- [ ] `desvios` e `pendencias_humano` no frontmatter batem com as seções em prosa
- [ ] `estado` coerente com os gates (só `concluido` se tudo ok e sem pendência)
- [ ] **Não** fez push pra `develop` — parou pra revisão (salvo instrução explícita)

> **Duas checagens ainda não existem e entram nesta mesma linha quando existirem** (autorizado em 2026-08-18, implementação pendente):
> 1. **Ancoragem de path de asset de skill** — hoje o `validate_skill.py` resolve o link contra o diretório do arquivo, que é o modelo que o harness **não** usa; ele passa exatamente no defeito que a QA-015 provou em runtime. Enquanto não for corrigido, este validador é **falso verde** para paths de asset.
> 2. **Anti-drift do comando de cobertura** — igualdade literal do comando canônico entre `.claude/agents/*.md` e `ROTEIRO-TESTES-BACKEND.md` §Camada 1.6. Hoje existem três cópias e nada impede que divirjam.
>
> Plugar as duas no **CI** foi deliberadamente adiado: mexe em `.github/workflows/`, que é território do back.

## Checklist do Reviewer (auditoria independente)

Executar **numa sessão separada** depois que o implementador abre o PR. Gates bloqueantes impedem o merge se inconsistentes.

- [ ] `build` e `testes` verdes no CI (ou verificação local equivalente)
- [ ] `territorio` ok — diff do PR não vaza pra fora do território da instância
- [ ] Status report presente em `docs/sprints/<NN>/status/<TASK-ID>-*.md` com frontmatter válido
- [ ] `testes_novos` condiz com os arquivos adicionados/alterados
- [ ] `desvios` e `pendencias_humano` declarados — sem omissão intencional
- [ ] Se `exige_e2e_full: true` no plano: confirmar que `e2e_full.executado: true` e `e2e_full.status: verde` no status report (ver `docs/architecture/desenho-testes-automatizados.md` §9.2). Caso contrário, **rejeitar** com pendência bloqueante.
- [ ] `playwright-report/` **não** está no diff do PR (não deve ser commitado)

## Agregação (o ganho de ter schema)

Como o frontmatter é YAML parseável, dá pra montar um painel do projeto sem esforço — um script lê todos os `docs/sprints/<NN>/status/*.md`, extrai o frontmatter e responde coisas como: quais tarefas estão `bloqueado`, quantos `desvios` abertos por área, soma de `testes_total`, quais branches fogem da convenção. Não precisa existir agora; o schema só deixa a porta aberta pra isso.

## Relação com outros docs

- Schema do status report: `docs/templates/_TEMPLATE-status.md`
- Convenção de branch e territórios: `CLAUDE.md` (seção "Fluxo de branches" e "Regra de ouro")
- Por que validar mesmo com schema válido: `docs/aprendizado/structured-outputs.md` (sintaxe ≠ semântica)
