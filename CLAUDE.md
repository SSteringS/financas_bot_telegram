# CLAUDE.md — Raiz do Repositório

## Visão geral do projeto

Bot de finanças para Telegram com frontend web. Monorepo com múltiplos módulos e instâncias de Claude Code colaborando em paralelo.

## Instâncias do Claude Code — quem faz o quê

| Instância | Pasta de trabalho | Branch | Responsabilidade |
|---|---|---|---|
| Claude do **back** | `financas_bot_telegram/` + infra | `feature/back-*` | API REST, domínio, banco, deploy |
| Claude do **front** | `frontend/` | `feature/frontend-*` | UI, componentes, chamadas à API |
| Claude de **planejamento** | `docs/` | `develop` (direto) | Documentação, especificações técnicas, decisões de arquitetura, planos de feature |

## Regra de ouro — isolamento por pasta

O isolamento vale pra **código**. `docs/` é área **compartilhada** — todas as instâncias escrevem ali.

- **Claude do back:** código apenas em `financas_bot_telegram/`, `infra/`, `finbot.service`, `.github/workflows/`
- **Claude do front:** código apenas em `frontend/`
- **Claude de planejamento:** commita direto em `develop` e é o **mantenedor** da estrutura de `docs/` (planos, architecture, decisions)
- **`docs/` é escrita por todos:** qualquer instância adiciona seu status report em `docs/status/` e pode registrar aprendizados em `docs/aprendizado/`. Mudanças estruturais em `docs/plans/`, `docs/architecture/` e `docs/decisions/` ficam com o planejamento.
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
    BE-XX-*.md                     ← planos individuais de tarefas
  runbooks/
    ROTEIRO-FRONTEND.md            ← guia passo a passo para o front
    ROTEIRO-TESTES-BACKEND.md      ← roteiro de testes em camadas
  decisions/
    _TEMPLATE.md                   ← template para registrar decisões (ADRs)
  status/
    _TEMPLATE.md                   ← template para status de features
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

## Fluxo de branches

```
main (protegida — só via PR)
 └── develop  ← planejamento commita aqui direto
      ├── feature/api-consulta-pedidos-comprovantes   ← back
      └── feature/frontend-setup                      ← front
```

- Back e front sempre criam branch a partir de `develop`
- Padrão: `feature/descricao-curta`, `fix/descricao-curta`, `hotfix/descricao-curta`
- **Uma tarefa = uma branch nova** a partir de `develop`, nomeada `feature/<id-tarefa>-<slug>` (ex: `feature/fe-12-resumo-parametrizado`). Não reaproveitar branch guarda-chuva de outra tarefa.
- **Precedência:** estas convenções prevalecem sobre qualquer plano de tarefa em `docs/plans/`. Um plano **não** deve sobrescrevê-las silenciosamente. Se uma tarefa exigir exceção (ex.: depende de código que só existe numa branch ainda não mergeada), o plano deve declarar `> EXCEÇÃO DE BRANCH:` com a justificativa — e o caminho preferido é mergear a dependência em `develop` antes de começar.
- **PR no caminho pra `develop` (decisão 2026-05-26):** feature branches entram em `develop` via **Pull Request**. O CI (CI-01) roda no PR e mostra verde/vermelho. **Branch protection NÃO está ligada por ora** (decisão do humano de adiar) — ou seja, o gate é **informativo, não bloqueante**: nada impede tecnicamente o merge com CI vermelho; a disciplina é manual até ligar a proteção. PR `develop → main` dispara o deploy. Ver `docs/plans/CI-01-gate-pr-develop.md` e ADR 0004 §5. Quando a branch protection for ligada, o gate passa a travar o merge (e o commit direto de docs do planner em `develop` também passará a exigir PR).
- **Definição de pronto / pré-merge:** antes de mergear, (1) passar pelos gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build, lint, testes, convenção de branch, território); (2) escrever o status report com frontmatter válido conforme `docs/status/_TEMPLATE.md`; (3) **revisão independente pelo Reviewer** — sessão separada (ADR 0005), obrigatória pra toda task antes do merge (decisão 2026-05-26). `estado: concluido` só vale com todos os gates ok e zero pendência.
- Back e front devem fazer merge de `develop` na feature branch regularmente para pegar atualizações de docs

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
                                                                                                                                                                                                                                                                                                                                                                                                                                                                   