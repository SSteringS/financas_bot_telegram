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

- **Claude do back:** toca apenas em `financas_bot_telegram/`, `infra/`, `finbot.service`, `.github/workflows/`
- **Claude do front:** toca apenas em `frontend/`
- **Claude de planejamento:** toca apenas em `docs/` — commita direto em `develop`
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
    FASE-3-VISUALIZACAO.md         ← plano da fase atual
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
- PR: branch → `develop` → revisão → merge → PR `develop → main` dispara o deploy
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
