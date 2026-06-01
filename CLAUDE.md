# CLAUDE.md — Raiz do Repositório

## Visão geral do projeto

Bot de finanças para Telegram com frontend web. Monorepo com múltiplos módulos e instâncias de Claude Code colaborando em paralelo.

## Instâncias do Claude Code — quem faz o quê

| Instância | Worktree (pasta no disco) | Branch | Responsabilidade |
|---|---|---|---|
| Claude do **back** | `C:\Users\satya\src\financas_bot_telegram` | `feature/be-NNN-*` / `fix/NNN-*` / `hotfix/NNN-*` | API REST, domínio, banco, deploy. Pasta de código: `financas_bot_telegram/` + `infra/` + `finbot.service` + `.github/workflows/` |
| Claude do **front** | `C:\Users\satya\src\financas_bot_telegram` (mesmo worktree do back; territórios disjuntos por pasta) | `feature/fe-*` | UI, componentes, chamadas à API. Pasta de código: `frontend/` |
| Claude de **planejamento** | `C:\Users\satya\src\financas_bot_telegram-planner` (worktree dedicado, fixo em `develop`) | `develop` (commita direto) | Documentação, especificações técnicas, decisões de arquitetura, planos de feature |
| Claude **Reviewer** | qualquer worktree (sessão somente-leitura) | a branch sob revisão | Revisão independente antes do merge (ADR 0005) |

## Regra de ouro — isolamento por pasta

O isolamento vale pra **código**. `docs/` é área **compartilhada** — todas as instâncias escrevem ali.

- **Claude do back:** código apenas em `financas_bot_telegram/`, `infra/`, `finbot.service`, `.github/workflows/`
- **Claude do front:** código apenas em `frontend/`
- **Claude de planejamento:** commita direto em `develop` e é o **mantenedor** da estrutura de `docs/` (planos, architecture, decisions)
- **`docs/` é escrita por todos:** qualquer instância adiciona seu status report em `docs/sprints/<NN>/status/` e pode registrar aprendizados em `docs/aprendizado/`. Mudanças estruturais em `docs/plans/`, `docs/architecture/` e `docs/decisions/` ficam com o planejamento.
- Arquivos da raiz (`CLAUDE.md`, `.gitignore`, `TODO.md`) podem ser editados por qualquer instância quando necessário

## Estrutura da pasta docs/

Organizada e mantida pelo Claude de planejamento:

```
docs/
  README.md                        ← índice geral
  PENDENCIAS-TECNICAS.md           ← débitos técnicos conhecidos a revisitar
  architecture/
    especificacao-tecnica.md       ← spec técnica detalhada por feature
    fluxo-autenticacao.md          ← diagrama de sequência da auth
    design-proposals/              ← variantes visuais e de UX
  plans/
    BACKLOG-produto.md         ← backlog de evoluções de produto (features futuras)
    BACKLOG-evolucao-workflow.md  ← backlog de melhorias de processo/workflow
    BE-NNN-*.md                     ← planos individuais de tarefas
  runbooks/
    ROTEIRO-FRONTEND.md            ← guia passo a passo para o front
    ROTEIRO-TESTES-BACKEND.md      ← roteiro de testes em camadas
  decisions/
    _TEMPLATE.md                   ← template para registrar decisões (ADRs)
  sprints/
    <NN>-<slug>/plans/             ← planos da sprint (ADR 0010)
    <NN>-<slug>/status/            ← status reports da sprint
    <NN>-<slug>/avaliacoes/        ← relatórios do Reviewer da sprint
  templates/
    _TEMPLATE-status.md            ← template canônico de status report (schema ADR 0007)
  aprendizado/                     ← biblioteca pessoal de conceitos discutidos
    README.md                      ← índice + convenções
    <topico>.md                    ← um arquivo por tópico, kebab-case
  avaliacoes/                      ← relatórios de avaliação das entregas dos implementadores
    README.md                      ← convenções
    <area>-<contexto>.md           ← um arquivo por entrega avaliada
```

**Back e front devem consultar `docs/` antes de implementar** — lá estão as especificações, contratos de API e decisões já tomadas.

## Pasta `docs/aprendizado/` — biblioteca pessoal de conceitos

Cada dúvida técnica não-trivial que o humano levanta e o Claude de planejamento responde com explicação aprofundada vira um arquivo de resumo em `docs/aprendizado/<topico>.md`. Esses arquivos servem pra o humano revisitar no futuro (refresh memorial, base pra aprofundar, ou referência rápida em outra discussão).

**Comportamento obrigatório do Claude de planejamento:**

Sempre que uma resposta a uma dúvida técnica do humano tiver substância de aprendizado (não é uma pergunta-resposta trivial nem operacional), o Claude **DEVE**, ao final da resposta, criar ou atualizar um arquivo correspondente em `docs/aprendizado/`. Cada arquivo deve conter:

- **Contexto da dúvida** — onde no projeto isso apareceu e qual era a pergunta original
- **Resumo destilado** — a explicação compactada, mais curta que a resposta original do chat
- **Pontos-chave** — bullets que cabem em uma "régua mental" rápida
- **Pra aprofundar** (opcional) — tópicos relacionados ou conceitos pra estudar mais

Regras:

- Nome do arquivo em kebab-case descrevendo o tópico (ex: `cookies-samesite.md`, `jwt-vs-api-key.md`)
- Auto-contido — quem ler o arquivo deve entender sem precisar reler a conversa original
- Se o tópico já tem arquivo, **atualizar** com nova nuance/exemplo/correção em vez de duplicar
- Se uma conversa cobriu múltiplos sub-tópicos, criar arquivos **separados** em vez de um único arquivo enorme
- Não criar arquivos pra perguntas operacionais ("onde fica X no repo?", "qual comando rodar?") — só pra dúvidas conceituais/técnicas

O índice em `docs/aprendizado/README.md` deve listar todos os tópicos existentes, agrupados por categoria. Atualizar o índice junto da criação de cada arquivo novo.

## Worktrees git

O repo opera com **dois worktrees git compartilhando o mesmo `.git/`**, pra permitir sessões do planner e dos implementadores rodarem em paralelo sem trocar branch a cada vez.

| Worktree | Pasta no disco | Branch fixa | Quem usa |
|---|---|---|---|
| **Planner** | `C:\Users\satya\src\financas_bot_telegram-planner` | `develop` (sempre) | Claude de planejamento; Cowork do humano pra trabalho em docs |
| **Implementador** | `C:\Users\satya\src\financas_bot_telegram` | branch de feature ativa | Claude do back, Claude do front |

**Consequências práticas:**

- `develop` está **checado no worktree do planner** — ninguém mais pode dar `git checkout develop` em outro worktree (git recusa: "is already checked out").
- Implementadores criam branch a partir da **branch `integration/<NN>-<slug>` da sprint** sem fazer `git checkout` antes:

```
git fetch
git checkout -b feature/<id>-<slug> origin/integration/<NN>-<slug>
```

- A branch `integration/<NN>-<slug>` é criada pelo planner no início de cada sprint (commit direto em develop, depois pusha). O implementador recebe o nome dela no dispatch.

- O Cowork do humano (sessão de planejamento) **deve estar apontando pro worktree do planner**. Se Cowork for aberto no worktree do implementador por engano, writes em arquivos novos podem se perder quando o implementador trocar de branch. **Confirmar isso no início de qualquer sessão de planejamento.**
- **Escrita defensiva é obrigatória pro planner.** As tools `Write`/`Edit` do Cowork podem truncar arquivos silenciosamente e o mount FUSE do sandbox serve view defasada do disco — diagnóstico em `docs/aprendizado/cowork-write-truncamento.md`, regras operacionais em `docs/roles/planner.md` §"Escrita defensiva de arquivos (workaround Cowork)".

## Acesso ao git pelo Cowork (planner)

Em sessões do planner, o Cowork do humano monta **dois worktrees** ao mesmo tempo:

1. `C:\Users\satya\src\financas_bot_telegram-planner` — **worktree de trabalho** do planner. Único lugar onde Claude usa `Edit` / `Write` pra modificar arquivos.
2. `C:\Users\satya\src\financas_bot_telegram` — **worktree do implementador**. Montado **somente** pra que o `.git/` real (que mora aqui dentro) fique acessível ao sandbox do Claude, permitindo rodar comandos `git` direto sem precisar pedir ao humano que cole saídas no chat. **Claude NUNCA usa `Edit` / `Write` em paths dentro deste mount.** Se um path de tool começar com `C:\Users\satya\src\financas_bot_telegram\` (sem o sufixo `-planner`), Claude **para e pergunta** antes de prosseguir.

### Comandos git permitidos sem confirmação (leitura)

`git status`, `git log`, `git diff`, `git show`, `git fetch`, `git branch -a`, `git ls-files`, `git blame`. Sempre rodar a partir do worktree do planner — git resolve o `.git` apontado e funciona transparentemente.

### Comandos git que mudam estado — Claude PERGUNTA ANTES via `AskUserQuestion`

`git add`, `git commit`, `git push`, `git pull`, `git cherry-pick`, `git checkout -b`, `git restore`, `git rm`. Nunca executar de surpresa — o humano deve confirmar cada mudança de estado.

### PROIBIDOS sem ordem explícita e específica do humano

`git reset --hard`, `git rebase`, `git push --force` (ou `--force-with-lease`), `git stash drop`, `git checkout -- <path>` (sobrescreve trabalho local), qualquer manipulação de reflog/gc/prune. Mesmo com confirmação, Claude descreve o impacto exato antes de executar.

### Race com sessões paralelas

Se houver sessão do Claude do back/front ativa no worktree do implementador (trocando branches), o planner **espera ela terminar** antes de rodar `fetch`/`pull`/`cherry-pick`/`push` pra evitar pegar estado transitório do `.git/` compartilhado. Leitura (`git log`, `git status`) é segura mas pode mostrar HEAD intermediário — interpretar com cautela.

## Fluxo de branches

```
main (protegida — só via PR)
 └── develop  ← planejamento commita aqui direto (no worktree planner)
      ├── integration/03-evo-09                        ← criada pelo planner no início da sprint
      │    ├── feature/be-017-renomear-rota-telegram   ← back  ┐ PR feature→integration:
      │    └── feature/fe-014-botao-ver-arquivo-original ← front ┘ implementador abre e aceita
      ├── fix/001-whatsapp-defaults-deploy-safe        ← back → develop direto (padrão zero-padded)
      └── hotfix/NNN-<slug>                            ← emergências → develop direto
```
> **PR integration→develop:** Claude (planner) abre ao fim da sprint; humano revisa e aceita. Este é o único gate humano no fluxo de features.

- Back e front sempre criam branch a partir de `integration/<NN>-<slug>` da sprint (ver "Worktrees git" pro fluxo correto). FIX e HOTFIX saem direto de `develop`.
- **Padrão de nome de branch:**
  - `feature/<id>-<slug>` para tarefas planejadas — id de **3 dígitos zero-padded**: `be-NNN`, `fe-NNN`, `dep-NNN`, `evo-NNN`, `ci-NNN`, `qa-NNN`. Ex: `feature/be-017-nova-feature`, `feature/qa-001-setup-playwright`. Convenção **forward-only desde 2026-05-31** (BE/FE/DEP/EVO/CI) e **desde 2026-06-01** (QA). `QA-NNN` = tooling/infra de qualidade que não modifica comportamento de feature (ver ADR 0017).
  - `fix/NNN-<slug>` para FIXes — id de **3 dígitos zero-padded** (`001`, `002`, ..., `099`, `100`). Ex: `fix/001-whatsapp-defaults-deploy-safe`. Convenção **forward-only desde 2026-05-29**: FIXes anteriores ficam com slug-only (`fix/idempotencia-porta-application`, `fix/gitattributes-eol`) e **não** são renomeados.
  - `hotfix/NNN-<slug>` para HOTFIXes — mesma regra de 3 dígitos zero-padded. Ex: `hotfix/001-document-vs-photo`. Convenção forward-only — hotfixes anteriores ficam com nome legado.
  - `integration/<NN>-<slug>` para branch intermediária de sprint — NN-slug deve bater com o identificador da sprint (`docs/sprints/<NN>-<slug>/`). Ex: `integration/03-evo-09`. Criada pelo **planner** ao abrir a sprint; deletada após o merge integration→develop ser aceito pelo humano.
- **Uma tarefa = uma branch nova** a partir de `integration/<NN>-<slug>` (features) ou de `develop` (fix/hotfix), nomeada com o padrão acima. Não reaproveitar branch guarda-chuva de outra tarefa.
- **Precedência:** estas convenções prevalecem sobre qualquer plano de tarefa em `docs/plans/`. Um plano **não** deve sobrescrevê-las silenciosamente. Se uma tarefa exigir exceção (ex.: depende de código que só existe numa branch ainda não mergeada), o plano deve declarar `> EXCEÇÃO DE BRANCH:` com a justificativa — e o caminho preferido é mergear a dependência em `develop` antes de começar.
- **Fluxo de PR em dois níveis (atualizado 2026-06-01):**
  - **feature → integration:** implementador abre o PR e pode aceitá-lo sem revisão humana. Permite que features sequenciais se encadeiem sem conflito.
  - **integration → develop:** Claude (planner) abre o PR ao fim da sprint; **humano revisa e aceita** — este é o gate de qualidade humano.
  - **fix/hotfix → develop:** PR direto (não passam pela integration branch).
  O CI (CI-01) roda nos PRs e mostra verde/vermelho. **Branch protection NÃO está ligada por ora** — gate é informativo, não bloqueante. PR `develop → main` dispara o deploy. Ver `docs/plans/CI-01-gate-pr-develop.md` e ADR 0004 §5.
- **Definição de pronto / pré-merge:** antes de mergear, (1) passar pelos gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build, lint, testes, convenção de branch, território); (2) escrever o status report com frontmatter válido conforme `docs/templates/_TEMPLATE-status.md`; (3) **revisão independente pelo Reviewer** — sessão separada (ADR 0005), obrigatória pra toda task antes do merge (decisão 2026-05-26). `estado: concluido` só vale com todos os gates ok e zero pendência.
- Back e front devem fazer merge de `integration/<NN>-<slug>` na feature branch regularmente para pegar código de features anteriores já integradas

## CI/CD

- Pipeline em `.github/workflows/deploy.yml`
- Dispara no merge para `main`
- Faz build do JAR do back + deploy via SSH na EC2 (`3.228.138.109`)
- Pipeline roda testes unitários no merge para `main`. Cobertura mínima: toda classe com lógica não-trivial tem teste; tarefas BE-* devem incluir testes próprios como critério de aceitação

## Infraestrutura (produção)

- **EC2:** t4g.micro, Amazon Linux 2023, Java 21, systemd (`finbot.service`)
- **RDS:** MySQL 8.0, default VPC, acesso restrito ao Elastic IP da EC2
- **S3:** `bot-financas-pagamentos-satyan` (prod) / `bot-financas-pagamentos-dev` (dev)
- **Secrets Manager:** `finbot-prod-secrets` (telegram_token, db_host, db_username, db_password)
- **HTTPS:** porta 8443, certificado auto-assinado em `/opt/finbot/keystore.p12`
