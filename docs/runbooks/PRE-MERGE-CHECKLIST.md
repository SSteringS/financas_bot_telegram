# Checklist de pré-merge (gates verificáveis)

Define a **definição de pronto** de qualquer tarefa BE/FE/DEP antes de mergear em `develop`. Cada gate é **machine-checkable**: tem um comando e uma condição de passagem. Os gates aqui mapeiam 1:1 com o bloco `gates:` do frontmatter do status report (`docs/status/_TEMPLATE.md`).

Conceito: o status report é um *output schema* (forma garantida, parseável). Este checklist é a *validação* desse output — porque schema válido não garante verdade (um report pode dizer `testes: ok` sem que seja). O Claude de planejamento (ou um script) confere os gates contra a realidade antes do merge.

## Definição de pronto

`estado: concluido` no frontmatter só é válido quando **todos os gates relevantes** estão `ok` (ou `na` quando não se aplicam) **e** `pendencias_humano: 0`. Caso contrário, o estado é `parcial` (entregou parte) ou `bloqueado` (esperando decisão/humano).

## Gates

| Gate (campo no frontmatter) | Como verificar | Passa quando |
|---|---|---|
| `build` | Back: `./mvnw -q -DskipTests package` · Front: `npm run build` | Sai com código 0, sem erro de compilação/TS |
| `lint` | Front: `npm run lint` · Back: `na` (não há linter configurado) | Sem erros. `na` quando a stack não tem linter |
| `testes` + `testes_total` + `testes_novos` | Back: `./mvnw test` · Front: `npm test` | Todos verdes. Anotar o total e quantos foram adicionados nesta tarefa |
| `branch_convencao` | `git rev-parse --abbrev-ref HEAD` e `git merge-base --is-ancestor origin/develop HEAD` | Nome bate `^feature/(be\|fe\|dep\|fix\|hotfix\|evo)-\d+[a-z]?-` **e** saiu de `develop` (develop é ancestral). Ver regra de branch no CLAUDE.md |
| `territorio` | `git diff --name-only origin/develop...HEAD` | Todos os caminhos alterados estão dentro do território da instância (ver tabela abaixo) |

### Territórios (pra o gate `territorio`)

Isolamento vale pra **código**. `docs/` é compartilhada — toda instância pode escrever ali (status report próprio, aprendizados). Mudança estrutural em `docs/plans/`, `docs/architecture/`, `docs/decisions/` é do planejamento.

| Instância (`responsavel`) | Caminhos de código permitidos | + sempre |
|---|---|---|
| `claude-back` | `financas_bot_telegram/`, `infra/`, `finbot.service`, `.github/workflows/` | `docs/` |
| `claude-front` | `frontend/` | `docs/` |
| `claude-plan` | — | `docs/` (mantenedor da estrutura) |
| qualquer | arquivos da raiz (`CLAUDE.md`, `.gitignore`, `TODO.md`) quando necessário | — |

O gate `territorio` falha quando uma instância altera **código fora** do seu território (ex.: o front mexendo em `financas_bot_telegram/`). Mudança em `docs/` nunca dispara o gate.

## Checklist operacional (rodar antes de abrir PR / pedir merge)

- [ ] `build` verde
- [ ] `lint` verde (ou `na`)
- [ ] `testes` verdes; `testes_total` e `testes_novos` preenchidos
- [ ] Todo componente/classe com lógica não-trivial tem ao menos 1 teste (regra do CLAUDE.md)
- [ ] `branch_convencao` ok (nome correto + saiu de develop)
- [ ] `territorio` ok (não vazou pra fora da pasta da instância)
- [ ] Status report criado em `docs/status/<TASK-ID>-*.md` com frontmatter válido
- [ ] `desvios` e `pendencias_humano` no frontmatter batem com as seções em prosa
- [ ] `estado` coerente com os gates (só `concluido` se tudo ok e sem pendência)
- [ ] **Não** fez push pra `develop` — parou pra revisão (salvo instrução explícita)

## Agregação (o ganho de ter schema)

Como o frontmatter é YAML parseável, dá pra montar um painel do projeto sem esforço — um script lê todos os `docs/status/*.md`, extrai o frontmatter e responde coisas como: quais tarefas estão `bloqueado`, quantos `desvios` abertos por área, soma de `testes_total`, quais branches fogem da convenção. Não precisa existir agora; o schema só deixa a porta aberta pra isso.

## Relação com outros docs

- Schema do status report: `docs/status/_TEMPLATE.md`
- Convenção de branch e territórios: `CLAUDE.md` (seção "Fluxo de branches" e "Regra de ouro")
- Por que validar mesmo com schema válido: `docs/aprendizado/structured-outputs.md` (sintaxe ≠ semântica)
