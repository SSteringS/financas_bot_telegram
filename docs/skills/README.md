# Skills � capacidades t�cnicas reutiliz�veis ou densas-mas-raras

> Pasta criada pela ADR `0015` (taxonomia roles � skills). Em ado��o � primeira skill � piloto, refinar com o uso.

## O que � skill

**Skill � capacidade t�cnica que o agente carrega sob demanda.** Diferente de role (identidade) e runbook (sequ�ncia operacional).

| Categoria | Pergunta que responde | Exemplo |
|---|---|---|
| **Role** (`docs/roles/`) | *Quem � o agente?* | "O Reviewer revisa entregas adversarialmente" |
| **Skill** (`docs/skills/`) | *O que o agente sabe fazer (e quando carregar)?* | "Como detectar smells de viola��o hexagonal � carregar quando task toca application/ ou infra/" |
| **Runbook** (`docs/runbooks/`) | *Que sequ�ncia seguimos?* | "Checklist pr�-merge: build ? lint ? testes ? ..." |

Se um arquivo responde *quem*, � role. Se responde *o que sei fazer (e em que situa��o)*, � skill. Se responde *que passos sigo*, � runbook.

## Quando criar skill (regra do reuso OU regra do contexto raro)

Skill nasce quando **=1 das duas condi��es** bate (ADR 0015 �4):

- **(a) Reuso (`load_pattern: shared`)** � =2 roles usam a capacidade **ou** =2 dispatches/master-prompts repetem o mesmo bloco. Exige `used_by` = 2.
- **(b) Contexto raro (`load_pattern: contextual`)** � 1 role usa, mas s� em situa��o espec�fica (n�o em toda invoca��o) **e** o conte�do � n�o-trivial (>30 linhas como heur�stica). Exige gatilho **literal** no `description` ("carregar quando ..."). Aceita `used_by` = 1.

Casos t�picos que justificam skill:

- **Reuso (a):** capacidade t�cnica densa que aparece no checklist de **mais de um papel** (ex.: leitura de arquitetura hex eventualmente compartilhada Reviewer + Architect + planner); bloco de instru��es **repetido literalmente** em =2 dispatches.
- **Contexto raro (b):** checklist denso usado por **um �nico role** mas s� em situa��o espec�fica que n�o acontece sempre (ex.: smells de arquitetura hex usados s� pelo Reviewer em PRs que tocam camadas � n�o em PR de CSS).

Casos que N�O justificam skill:

- Conhecimento usado por um �nico role **em toda invoca��o** ? fica no `role.md`.
- Conhecimento que sempre tem que estar carregado ? CLAUDE.md ou skill `always-on` no role (= `load_pattern: shared` listada em `skills:` no frontmatter do role).
- Conceito formativo pro humano ? `docs/aprendizado/`.
- Processo organizacional fixo ? `docs/runbooks/`.
- Decis�o arquitetural can�nica ? ADR em `docs/decisions/`.

**Operacionaliza��o do "=2 dispatches":** a contagem precisa ser **citada literalmente no `description`** (`"... carregar quando ... (extra�do de DISPATCH-X, role Y)"`). Sem cita, esses dispatches n�o contam � cai pra `contextual` ou viola a regra.

## Schema do SKILL.md

Cada skill � **um arquivo flat** em kebab-case (sem sub-pastas). O arquivo come�a com **frontmatter YAML obrigat�rio** seguido do corpo em markdown.

### Frontmatter YAML (obrigat�rio)

Espelha o padr�o do Anthropic Skills system (`name` + `description` s�o os campos can�nicos lidos por progressive disclosure pra decidir o trigger) e estende com campos nossos (alinhados com ADR 0007).

```yaml
---
name: leitura-arquitetura-hexagonal
description: Detecta smells de viola��o de depend�ncia na arquitetura hexagonal � application importando de infra, vazamento de internals de adapter pra application, repos/queries direto em controller. Carregar quando uma task toca c�digo em application/ ou infra/, ou quando o Reviewer est� avaliando PR que cruza camadas.
load_pattern: contextual    # shared (=2 consumidores) | contextual (1 consumidor, uso raro)
used_by: [reviewer]         # tamanho 1 s� � v�lido com load_pattern: contextual
created: 2026-05-30
adr: 0015
status: ativa               # ativa | deprecated | superseded
---
```

Campos:

- **`name`** *(obrigat�rio, can�nico Anthropic)* � kebab-case, **id�ntico ao nome do arquivo sem `.md`**. Identificador �nico.
- **`description`** *(obrigat�rio, can�nico Anthropic)* � descri��o em terceira pessoa que diz **o que a skill faz + quando carregar**. Este texto � o que o agente l� pra decidir o trigger; precisa ser espec�fico e direcion�vel. Em skill `contextual`, o gatilho precisa ser **literal** (n�o vago).
- **`load_pattern`** *(obrigat�rio, extens�o nossa)* � `shared` (motivo reuso, exige `used_by` = 2) **ou** `contextual` (motivo gest�o de contexto, aceita `used_by` = 1). Veta `always` � se sempre carrega, fica no role.
- **`used_by`** *(obrigat�rio, extens�o nossa)* � lista de roles que carregam esta skill. Se `load_pattern: shared`, **tamanho = 2 obrigat�rio**. Se `load_pattern: contextual`, tamanho 1 OK.
- **`created`** *(obrigat�rio, extens�o nossa)* � data ISO `YYYY-MM-DD` da cria��o. Pra rastreabilidade.
- **`adr`** *(opcional, extens�o nossa)* � ADR que motivou a skill, se aplic�vel. Ex.: `0015` (sem prefixo `ADR-`).
- **`status`** *(opcional, extens�o nossa)* � `ativa` (default), `deprecated` (substitu�da, vai sumir), `superseded` (apontar pra skill nova). Pra manuten��o evolutiva sem deletar arquivos cegamente.

Outros campos (ex.: `tags`, `last_reviewed`) ficam livres conforme a dor aparecer � frontmatter YAML aceita.

### Corpo (markdown)

Ap�s o frontmatter, o corpo segue esta estrutura:

```markdown
# Skill � <nome curto>

## Quando carregar (gatilho expl�cito)
Crit�rios literais que disparam o load. Repete e expande o que est� no `description` do frontmatter � esta se��o � pra leitura humana detalhada.

## Resumo da capacidade
Uma a tr�s frases. O que esta skill ensina a fazer.

## Pontos-chave / checklist
- Bullets acion�veis.
- Cada item, idealmente, verific�vel.

## Exemplos (opcional)
Caso concreto do nosso repo, com link pro arquivo/commit.

## Ler junto (opcional)
- ADRs relacionadas
- Outras skills complementares
- Runbooks que usam esta skill
```

**Limite duro: ~150 linhas (frontmatter + corpo).** Se passou, virou doc � quebrar em skills menores ou extrair parte pra aprendizado/architecture.

## Como carregar (role ? skill) � fonte da verdade no frontmatter do role

A lista can�nica de skills que um role carrega vive no **frontmatter YAML do `docs/roles/<papel>.md`**, alinhada com o padr�o Anthropic Skills + Claude Code subagent:

```yaml
---
name: <papel>
description: ...
tools: ...
skills: [skill-x, skill-y]              # always-on � carregadas no bootstrap junto do role
skills_available: [skill-a, skill-b]    # on-demand � agente l� metadata; corpo expande no gatilho
---
```

- **`skills:`** alinha com o can�nico Anthropic. Migra direto pro subagent nativo quando o role virar `.claude/agents/<papel>.md`.
- **`skills_available:`** � extens�o nossa, materializa o progressive disclosure manual: agente l� s� o `description` (frontmatter da skill) por default; corpo carrega quando o gatilho bate.

O corpo do role tem se��o `## Skills` que **explica o porqu�** de cada carregamento (motiva��o + gatilho esperado), mas a **fonte da verdade da lista � o frontmatter** � atualiza num lugar s�, parse�vel por script.

Isso replica o progressive disclosure do Skills system da Anthropic: agente l� s� metadata (frontmatter) por default; corpo expande quando triggered.

## M�tricas de ado��o (ADR 0015 �6)

Pr�-acordadas pra evitar adotar por est�tica. Baseline tirado **antes** de extrair a primeira skill.

| M�trica | Alvo |
|---|---|
| Tamanho m�dio de `docs/roles/*.md` | cair =20% ap�s 3 skills extra�das |
| Tamanho m�dio de master-prompt | ~80 linhas ? ~30 |
| Skills duplicadas em 2+ roles | zero |
| Drift detectado-pelo-humano cobertos por role/skill | tend�ncia queda |
| Skills com `used_by` tamanho 1 **e** `load_pattern: shared` | zero (viola��o da regra do reuso) |

Skills com `used_by` tamanho 1 **e** `load_pattern: contextual` **n�o s�o viola��o** � s�o exatamente o caso da gest�o de contexto.

**Crit�rio de parada:** se em 1 m�s acumularmos >8 skills e nenhuma m�trica mexer ? audita.

## Conven��es

- **Forward-only.** Roles existentes n�o s�o reescritos. Skill nasce quando dor + reuso (ou contexto raro denso) aparece.
- **Sem sub-pastas** dentro de `docs/skills/`. Estrutura plana ajuda a navegar.
- **Nome em kebab-case.** Descritivo, sem prefixo de categoria (n�o usar `arch-leitura-hex.md` � usar `leitura-arquitetura-hexagonal.md`). `name` no frontmatter = nome do arquivo sem `.md`.
- **Atualizar em vez de duplicar.** Se nuance nova, edita; se assunto diferente, cria skill separada.
- **Frontmatter YAML obrigat�rio.** Permite scripts (futuros) lerem metadata sem parsear markdown, igual o `metricas_status.py` faz com status reports.

## Localiza��o can�nica

As skills vivem em `.claude/skills/<name>/SKILL.md` � formato can�nico Anthropic, descoberto automaticamente pelo Claude Code via semantic matching. Esta pasta (`docs/skills/`) mant�m apenas este README como �ndice e refer�ncia de conven��es.

## �ndice

| Skill (em `.claude/skills/`) | load_pattern | used_by | Descri��o curta |
|---|---|---|---|
| `padroes-qualidade-codigo/SKILL.md` | shared | backend, architect, reviewer | SOLID + design patterns + boas pr�ticas. Quando usar cada pattern e o problema que ele resolve. |
| `arquitetura-hexagonal/SKILL.md` | shared | backend, architect, reviewer, planner | Estrutura de camadas, regras de depend�ncia, como implementar/projetar uma feature, detec��o de viola��es. Planner carrega ao escrever spec de feature que cruza camadas. |
| `ciclo-de-sprint/SKILL.md` | contextual | planner | Abertura, condu��o, fechamento, retro e kaizen � workflow espec�fico do projeto (n�o Scrum gen�rico). |
| `escrita-de-dispatch/SKILL.md` | contextual | planner | Como escrever DISPATCH/MASTER-PROMPT sem boilerplate. Resolve o problema dos ~70% gen�rico identificado no BACKLOG #10. |
| `escrita-de-plano-completo/SKILL.md` | contextual | planner | Como derivar crit�rios de aceite verific�veis, identificar riscos reais e definir territ�rio. Complementa o template � n�o substitui. |
| `ecossistema-spring/SKILL.md` | shared | backend, architect | Decis�es de biblioteca Spring Boot � Spring Data JDBC vs JPA, RestClient/WebClient/Feign, eventos, virtual threads, HikariCP sizing. |
| `jvm-e-performance/SKILL.md` | shared | backend, architect | JVM flags para t4g.micro (heap = 700 MB), GC, Micrometer, diagn�stico de thread dump e virtual threads. |
| `formatacao-java/SKILL.md` | contextual | backend | Naming por camada, ordering de imports/anota��es, records para DTOs, var, Javadoc m�nimo. |
| `boas-praticas-react/SKILL.md` | shared | frontend, reviewer | Padroes React (hooks, TypeScript, composicao, estado, performance, a11y) + justificativa tecnica para relatorio done. Reviewer usa ao avaliar PR FE. |
| `seguranca-web-frontend/SKILL.md` | shared | frontend, reviewer | XSS, armazenamento de tokens (httpOnly vs localStorage), CSP, validacao de input, CORS, scripts de terceiros. |
| `ecossistema-frontend/SKILL.md` | contextual | frontend | Vite, TypeScript strict, Jest+RTL (o que e como testar), MSW (contrato OpenAPI), codegen. |
| `qualidade-de-testes/SKILL.md` | shared | backend, frontend, reviewer | Testar comportamento nao implementacao, anti-patterns (mock excessivo, assertion ausente, snapshot fragil), cobertura que importa, nomenclatura. |
| `seguranca-backend/SKILL.md` | shared | backend, reviewer | Fluxo de auth do projeto (magic link, JWT cookie, isolamento por requisitante_id), OWASP Top 10 aplicado, Spring Security, secrets, input validation. |
| `otimizacao-custos-aws/SKILL.md` | contextual | architect | Pricing AWS dos servicos em uso, armadilhas de custo (CloudWatch+Micrometer), right-sizing, Savings Plans, lifecycle S3. ADR de infra obriga custo estimado. |
| `observabilidade/SKILL.md` | contextual | architect | Comparativo CloudWatch vs Grafana Cloud vs Datadog vs self-hosted, tres pilares, OpenTelemetry, decisao por custo e escala. |

## Rela��o com outras pastas

- `docs/roles/` � quem carrega as skills (via frontmatter `skills:` / `skills_available:`). Mant�m identidade e territ�rio.
- `docs/runbooks/` � sequ�ncia operacional. Pode referenciar skills (ex.: runbook de revis�o referencia skill de arquitetura).
- `docs/aprendizado/` � conceito formativo pro humano. Skill � instru��o pro **agente**; aprendizado � resumo pro **humano**. Podem coexistir e referenciar um ao outro.
- `docs/decisions/` � ADR define quando uma skill nasce/morre se a decis�o for arquitetural (ex.: esta pasta nasce na ADR 0015).
- `docs/architecture/` � arquitetura do **produto/sistema**. N�o confundir com arquitetura do nosso processo (que � aqui).
