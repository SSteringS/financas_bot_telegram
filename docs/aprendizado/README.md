# Aprendizado — biblioteca pessoal de conceitos

Pasta destinada a guardar resumos de conceitos técnicos que apareceram em discussões com o Claude de planejamento ao longo do projeto. Cada arquivo é auto-contido e serve pra:

- **Refresh** rápido de algo que você já entendeu mas pode esquecer
- **Base de estudo** se quiser ir mais a fundo no tópico
- **Referência cruzada** quando o tema aparece em outra conversa

Não é documentação de arquitetura (pra isso, `docs/architecture/`) nem plano de tarefa (pra isso, `docs/plans/`). É **conteúdo formativo**, organizado por tópico.

## Convenções

- **Um arquivo por tópico**, nome em kebab-case (ex: `cookies-samesite.md`)
- **Estrutura padrão** de cada arquivo:
  1. Contexto da dúvida — onde apareceu no projeto e qual era a pergunta original
  2. Resumo destilado — explicação curta e direta
  3. Pontos-chave — bullets pra revisão rápida
  4. Pra aprofundar (opcional) — tópicos relacionados ou conceitos pra estudar mais
- **Atualizar em vez de duplicar** — se o tópico volta a aparecer com nuance, edita o arquivo existente
- **Não inflar com perguntas operacionais** — só dúvidas conceituais/técnicas

## Índice por categoria

### Java / Spring fundamentos

- [`java-records.md`](java-records.md) — quando usar records vs classe com Lombok
- [`anotacoes-customizadas-java.md`](anotacoes-customizadas-java.md) — `@Target`, `@Retention`, `@interface`
- [`exception-handlers-scope.md`](exception-handlers-scope.md) — `@RestControllerAdvice` e `basePackages`
- [`jjwt-3-artefatos.md`](jjwt-3-artefatos.md) — por que a lib JWT tem 3 dependências
- [`argument-resolver-vs-requestparam.md`](argument-resolver-vs-requestparam.md) — por que `@RequisitanteId` ignora query param do mesmo nome
- [`eventos-in-process-spring.md`](eventos-in-process-spring.md) — `ApplicationEvents`, `@TransactionalEventListener(AFTER_COMMIT)`, `@Async`, trade-off de durabilidade; base do ADR 0014 (EVO-02)

### React / TypeScript fundamentos

- [`fetch-wrapper-pattern.md`](fetch-wrapper-pattern.md) — padrão de cliente HTTP em React (FE-03)
- [`react-hooks-e-renderizacao.md`](react-hooks-e-renderizacao.md) — modelo de re-render, useState/useEffect/useRef, StrictMode (FE-04)
- [`react-router-rotas-protegidas.md`](react-router-rotas-protegidas.md) — roteamento e padrão de rota protegida (FE-04)
- [`react-componentes-props-e-listas.md`](react-componentes-props-e-listas.md) — props, renderização condicional, listas com key (FE-05)
- [`react-controlled-components-e-debounce.md`](react-controlled-components-e-debounce.md) — componentes controlados e debounce (FE-06)
- [`react-tanstack-query.md`](react-tanstack-query.md) — gerenciamento de estado de servidor (FE-07)
- [`react-estado-derivado-e-composicao-de-hooks.md`](react-estado-derivado-e-composicao-de-hooks.md) — estado derivado e composição de hooks (FE-08)

### Ferramentas / fluxo

- [`git-reset-e-area-de-staging.md`](git-reset-e-area-de-staging.md) — index, modos de `git reset`, lock file
- [`git-line-endings-crlf-lf.md`](git-line-endings-crlf-lf.md) — CRLF vs LF, `.gitattributes`, renormalização; drift de `user_data` no Terraform
- [`spec-efemera-vs-arquitetura-duradoura.md`](spec-efemera-vs-arquitetura-duradoura.md) — régua "isso vai ser verdade em 6 meses?" distingue `docs/architecture/` (durável) de `docs/sprints/<NN>/specs/` (efêmera); ADR canoniza decisões transversais; caso prático 2026-06-01 do desenho de testes E2E

### Ferramental / ambiente

- [`cowork-write-truncamento.md`](cowork-write-truncamento.md) — Cowork `Write`/`Edit` truncam silenciosamente; mount FUSE serve view defasada e não permite delete; workaround via bash heredoc + `wc -l && tail`; bug report rascunho em inglês pra colar no feedback
- [`piloto-vscode-claude-code.md`](piloto-vscode-claude-code.md) — resultados do piloto de migração Cowork → VS Code + Claude Code (2026-05-30): escrita sem trunc confirmada, `.claude/` desbloqueado, multi-sessão nativa, lacunas (Office docs); implicações pro projeto
- [`yaml-compact-mapping-colon.md`](yaml-compact-mapping-colon.md) — `: ` (colon-space) dentro de plain scalar YAML causa "Nested mappings" no frontmatter de agent/skill files; sempre usar `".."` no campo `description`

### IA / engenharia de modelos

- [`structured-outputs.md`](structured-outputs.md) — saídas estruturadas de LLM (schema mode, semântica vs. sintaxe), aplicação no parser do bot e no workflow de Claudes
- [`taxonomia-agent-skill-workflow.md`](taxonomia-agent-skill-workflow.md) — fronteiras Agent/Role/Skill/Workflow em sistemas multi-agente; calibragens pro nosso setup multi-sessão; skill como context engineering / RAG; base da ADR 0015

### Infra / deploy / AWS

- [`hospedagem-spa-s3-cloudfront.md`](hospedagem-spa-s3-cloudfront.md) — SPA estática em S3+CloudFront: OAC, cert us-east-1, alias no apex, SPA fallback 403/404 (DEP-02)
- [`front-api-hostnames-separados.md`](front-api-hostnames-separados.md) — por que front (apex) e API (subdomínio) ficam separados: cache oposto, cross-origin vs same-site, tradeoff do single-origin
- [`cloudfront-functions-e-security-headers.md`](cloudfront-functions-e-security-headers.md) — edge compute (CloudFront Functions vs Lambda@Edge), redirect www→apex, Response Headers Policy (HSTS/CSP)

### Integrações / mensageria

- [`whatsapp-modelo-mensagens.md`](whatsapp-modelo-mensagens.md) — janela de 24h, categorias de template (serviço/utilidade/autenticação/marketing), oficial vs não-oficial; base do ADR 0012 (EVO-01) e do design da EVO-02

### Segurança web

- [`cookies-samesite.md`](cookies-samesite.md) — atributo SameSite e proteção CSRF
- [`cors-vs-samesite.md`](cors-vs-samesite.md) — diferença entre CORS e SameSite
- [`jwt-secret-vs-api-key.md`](jwt-secret-vs-api-key.md) — por que ter dois secrets diferentes
- [`admin-vs-requisitante.md`](admin-vs-requisitante.md) — dois papéis distintos de auth no sistema
- [`tls-self-signed-vs-ca.md`](tls-self-signed-vs-ca.md) — self-signed vs CA (Let's Encrypt) difere só na autenticação, não na encriptação; pinning (webhook Telegram) vs trust store (browser)
- [`observability-logs-externalizar.md`](observability-logs-externalizar.md) — externalizar logs (CloudWatch vs SaaS); custo desprezível no nosso volume → decidir por simplicidade; CloudWatch já tem IAM e vira casa de alarmes
- [`cloudwatch-metric-dimensions.md`](cloudwatch-metric-dimensions.md) — identidade de métrica = `(namespace, nome, conjunto_de_dimensões)` exato; `append_dimensions` no agent + alarme com subconjunto = `INSUFFICIENT_DATA` silencioso
- [`whatsapp-restricoes-pais-business-nao-verificada.md`](whatsapp-restricoes-pais-business-nao-verificada.md) — erro 130497: Business não-verificada não pode mandar mensagem **business-initiated** pra BR; só user-initiated (24h) flui; EVO-02 depende de Business Verification (MEI/CNPJ); Test number também sofre

## Como adicionar

O Claude de planejamento cria/atualiza arquivos aqui automaticamente quando uma discussão técnica acontece. Se você quiser puxar um tópico específico pra registrar, basta pedir explicitamente: "registra o aprendizado de X em `docs/aprendizado/`". Se sentir que um arquivo está incompleto, peça pra eu adicionar mais detalhe ou exemplos.

### Banco de dados / SQL

- [`yearmonth-para-date-sql.md`](yearmonth-para-date-sql.md) — como representar YearMonth (Java) como DATE no banco; convenção "primeiro dia do mês"; uso nos boundaries de query e no UNIQUE INDEX de idempotência (EVO-09)

### Telegram / bot

- [`telegram-conversas-multi-turno.md`](telegram-conversas-multi-turno.md) — estado multi-turno no bot (Map vs banco), CallbackQuery, padrao CommandRouter; contexto do EVO-09 (ADR 0016)
