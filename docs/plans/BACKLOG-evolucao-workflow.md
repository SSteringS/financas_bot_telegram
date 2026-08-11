# Backlog — evolução do workflow (melhorias de processo)

Itens de **profissionalização do workflow** que saíram da auditoria de 2026-05-26 (ver ADR `0004`). Não são features do produto (essas estão em `FASE-3-VISUALIZACAO.md`) — são melhorias de processo/tooling. Ordem = prioridade aproximada de impacto.

> Guardado pra sobreviver à separação de sessões. Quando um item virar task formal, promover pra um plano próprio e marcar aqui.

## Em voo (já planejados)

- **CI-01 — gate de CI no PR pra `develop`.** Plano em `docs/plans/CI-01-gate-pr-develop.md`. **Decisão (2026-05-26): PR→develop com o CI rodando no PR; branch protection adiada** → gate informativo, não bloqueante por ora. Pendente: execução do `ci.yml` pelo back. (Ligar branch protection no GitHub quando quiser tornar bloqueante.)
- **FIX-gitattributes-eol.** ✅ **Feito** (ver `docs/sprints/01-mvp/status/FIX-gitattributes-eol.md`) — drift CRLF/LF eliminado, `terraform plan` limpo. Pendente só o PR pra `develop`.
- **FE-13 — codegen do `tipos.ts` a partir do OpenAPI.** ✅ Plano escrito em `docs/plans/FE-13-codegen-tipos-openapi.md` (promovido do item #1 abaixo). Pendente: execução pelo front.

## A escrever (do codegen pra frente)

### 1. Codegen do `tipos.ts` a partir do OpenAPI — ✅ promovido pra plano
**O quê:** gerar os tipos TS do front a partir do `/v3/api-docs` (springdoc) em vez de manter `tipos.ts` à mão. **Por quê:** mata a classe de bug de drift front/back (o `mesAtual→mes` da FE-12). **Esforço:** médio. **Prioridade:** alta. **Plano:** `docs/plans/FE-13-codegen-tipos-openapi.md` (ferramenta escolhida: `openapi-typescript`; estratégia: snapshot do spec versionado + geração do arquivo; check de drift no CI fica como fase 2). Pendente execução pelo front.

### 2. `docs/STATE.md` — doc de orientação ("onde estamos agora") — ✅ feito
**O quê:** um resumo curto e vivo: fase atual, o que está em `develop`, o que está em voo, próximos passos. **Por quê:** agente que começa frio re-deriva contexto; isso orienta rápido. **Feito:** `docs/STATE.md` criado (2026-05-26). Manter atualizado a cada mudança relevante de estado.

### 3. Script de métricas sobre o frontmatter dos status reports — ✅ feito
**O quê:** script que lê `docs/sprints/<NN>/status/*.md`, extrai o frontmatter e agrega (tasks por `estado`, `desvios` por área, soma de `testes_total`, taxa de gate-fail). **Feito:** `docs/scripts/metricas_status.py` (+ `docs/scripts/README.md`). Métricas escolhidas: cobertura do schema canônico, estado das tasks, gate-fails, desvios/pendências, soma de testes. **Achado da 1ª rodada:** só 12/36 reports têm frontmatter e só 1 (DEP-02) segue o schema canônico — os demais são anteriores ao `_TEMPLATE`/ADR 0007. O schema vale dos novos pra frente; a métrica serve pra ver a adoção subir.

### 4. Formalizar o template de task (intake) — ✅ feito
**O quê:** adicionar ao padrão de plano os campos que faltam: **origem**, **riscos**, **prioridade** (já temos contexto/critérios/dependências/branch). **Por quê:** estruturar o input contract (mesmo princípio do status report). **Feito:** `docs/plans/_TEMPLATE.md` criado com bloco de Intake (origem, prioridade, esforço, território/quem executa, branch, dependências, riscos) + seções de corpo. O plano `FE-13` já segue o formato.

### 5. ADRs retroativos pras decisões arquiteturais já tomadas — ✅ feito (com 1 ressalva)
**O quê:** promover a ADR decisões que ficaram só em `aprendizado/`/`CLAUDE.md`. **Feito:**
- **ADR 0006** — front no apex + API em subdomínio (hostnames separados). `Accepted`.
- **ADR 0007** — reporting com gates / status report como output contract. `Accepted`.
- **ADR 0008** — Terraform como módulo raiz único. `Proposed` — o fato está verificado, mas a justificativa original não estava registrada; **aguarda homologação do humano** pra virar `Accepted`.

### 6. Arquivos de papel pra sessões especializadas (`docs/roles/`) — ✅ feito + Reviewer adotado
**O quê:** instruções por papel pra reduzir context dilution e viés do planejador. **Feito:** ADR `0005` + arquivos `docs/roles/{planner,backend,frontend,reviewer}.md`. Localização ficou `docs/roles/` (não `.claude/`, que é protegido pra escrita da sessão de planejamento). **Reviewer adotado (2026-05-26):** revisão independente em sessão separada passa a ser obrigatória pra toda task antes do merge (registrado no `CLAUDE.md`, seção pré-merge). `architect.md`/`qa.md` seguem adiados.

### 7. Pequeno — incluir prefixo `CI` no regex de task-id — ✅ feito
**O quê:** atualizar o regex em `PRE-MERGE-CHECKLIST.md` pra aceitar `CI-`. **Feito:** regex de `branch_convencao` no `PRE-MERGE-CHECKLIST.md` e o comentário de schema no `docs/templates/_TEMPLATE-status.md` agora incluem `ci`/`CI`.

### 8. Checklist arquitetural explícito no `reviewer.md` — ✅ feito (WF-03, 2026-05-30)
**O quê:** evoluir `docs/roles/reviewer.md` pra incluir um **bloco específico de smells arquiteturais** que o Reviewer deve procurar ativamente: violação de direção de dependência hexagonal (application importando de infra), vazamento de internals de adapter pra application (ex.: justificar decisões de application com raciocínio JPA/JDBC), repos/queries direto em controller, etc.

**Por quê:** descoberto em 2026-05-28 na revisão da BE-19a — o humano flagrou `MensagemProcessadaService` (application) dependendo direto de `JdbcTemplate` (infra). Reviewer aprovou comportamento mas **não pegou** a violação arquitetural. Promovido pra `FIX-idempotencia-porta-application` (sprint 02). Sinaliza que o Reviewer tem rede em comportamento/testes/gates mas o "olho arquitetural" precisa virar explícito também — não pode depender de instinto.

**Esforço:** baixo (atualizar `reviewer.md` com bullets/checklist concreto). **Prioridade:** alta — toda task que mexer em camada nova arrisca repetir o mesmo tipo de drift até resolver.

**Pra discussão na RETRO-02:**
- Definir os 4–6 smells mais valiosos a checar explicitamente (não virar lista interminável).
- Decidir se vira **seção no `reviewer.md`** (delta enxuto) ou **runbook próprio** que o Reviewer puxa quando a task toca camadas de arquitetura.
- Medir no próximo ciclo se a frequência de drift detectado-pelo-humano cai.

### 9. Numeração sequencial zero-padded pra TODO task-id — ✅ feito (WF-04, 2026-05-31)

**Atualização 2026-05-29 — adoção parcial:** o humano decidiu adotar **agora**, forward-only, **só pra FIX e HOTFIX** com **3 dígitos zero-padded** (FIX-001, HOTFIX-001). Branch passa a ser `fix/<id3d>-<slug>` e `hotfix/<id3d>-<slug>`. Os planos FIX já escritos mas não-mergeados (`FIX-idempotencia-porta-application`, `FIX-padronizar-restclient-builder`) ficam no formato slug-only legado. Registrado em `CLAUDE.md` (Fluxo de branches) + `docs/templates/_TEMPLATE-status.md` (schema). A discussão restante (zero-padding pra **BE/FE/DEP/EVO/CI** + tratamento de bifurcações tipo `BE-19a` + script `next-task-id.sh`) segue pra RETRO-02.
**Atualização 2026-05-31 — WF-04:** estendido para **todos os prefixos** (BE/FE/DEP/EVO/CI) — 3 dígitos zero-padded forward-only. Branch vira `feature/<prefix>-NNN-<slug>`. `CLAUDE.md`, `PRE-MERGE-CHECKLIST.md` e `_TEMPLATE-status.md` atualizados. Legado (e.g. `BE-17`, `FE-14`) não é renomeado.

**O quê (proposta original):** padronizar task-id em formato **zero-padded sequencial global**: `BE-0001`, `BE-0002`, `FIX-0001`, `HOTFIX-0001`, `FE-0001`, `DEP-0001`, `CI-0001` etc. — em vez do formato atual `BE-17`, `BE-19a`, `FIX-idempotencia-porta-application` (slug-based pros FIX).

**Por quê:** o humano levantou (2026-05-29) que **fica melhor pra enxergar e achar a mais recente**. Hoje:
- BEs são numerados (`BE-17`, `BE-18`, `BE-19`, `BE-19a` — o "a" já é gambiarra de bifurcação) mas sem zero-pad → ordenação alfabética não bate com cronológica (`BE-2` vem depois de `BE-19`).
- FIX/HOTFIX usam slug (`FIX-gitattributes-eol`, `FIX-idempotencia-porta-application`, `FIX-padronizar-restclient-builder`) → impossível saber qual é o mais recente sem `ls -lt`, e o slug repete info que já está no título do plano.
- Ordenação previsível ajuda Reviewer, planner e humano a localizar o "último BE", "último FIX" sem precisar varrer.

**Esforço:** baixo a médio. **Migração**: provavelmente **só forward** (novas tasks no formato novo; legado fica como está, ou rename opcional dos últimos N). Mexe em: `CLAUDE.md` (convenção de branch), `_TEMPLATE.md` (header), `PRE-MERGE-CHECKLIST.md` (regex), `metricas_status.py` (regex). **Prioridade:** média — não bloqueia nada, mas quanto antes resolver, menos legado pra carregar.

**Pra discussão na RETRO-02:**
- Largura do padding: `0001` (4 dígitos, suporta 9999) parece confortável; `001` (3 dígitos) pode bastar se o projeto for menor que parece.
- **Sequência única por prefixo** (BE tem sua sequência, FIX tem outra) **ou única global** (uma só, atravessando prefixos — `0001-BE`, `0002-FIX`)? A primeira é mais comum (estilo Jira); a segunda dá ordem temporal absoluta mas mistura tipos no `ls`.
- O que fazer com bifurcações tipo `BE-19a` (filha tardia de uma task já fechada): manter sufixo letra ou alocar novo número sequencial? (Vot pessoal: novo número — sufixo letra é o que está atrapalhando a leitura.)
- Migrar histórico ou só daqui pra frente? (Vot pessoal: forward-only; legado fica.)
- Como descobrir o "próximo número" sem race: script `docs/scripts/next-task-id.sh BE` que varre `docs/sprints/*/plans/` + `docs/plans/` e devolve o próximo. Custo baixo, evita colisão entre sessões paralelas.

### 10. Separar configurações dos agentes em roles × skills × workflows — ✅ promovido pra ADR 0015 (`Proposed`, 2026-05-30)

**Status:** virou ADR `0015-taxonomia-roles-skills-workflows.md` (Proposed) + spec em `docs/skills/README.md` + aprendizado em `docs/aprendizado/taxonomia-agent-skill-workflow.md`. Decisões da ADR: (a) adota roles × skills, runbook fica como workflow; (b) regra do 2x + métricas + critério de parada controlam overengineering; (c) skill piloto = `leitura-arquitetura-hexagonal` (cobre o item #8 deste backlog); (d) forward-only — roles existentes não são reescritos; (e) `.claude/agents/` segue reservado (ADR 0005 §1 vigente). **Pendente:** homologação humana (`Proposed → Accepted`) + tarefa pra escrever a skill piloto. Discussão original preservada abaixo pra rastreabilidade.

---



**O quê:** explorar se vale dividir o que hoje é "tudo dentro de `docs/roles/`" em três dimensões conceituais:

- **Roles** — *quem o agente é*. Identidade: território, voz, o que ele otimiza, como ele se comporta. (Hoje: `docs/roles/{planner,backend,frontend,reviewer,architect}.md`.)
- **Skills** — *o que o agente sabe fazer*. Pacotes de capacidade carregados sob demanda: "como escrever um status report", "como fazer review arquitetural", "como compor um master prompt de overnight", "como navegar um runbook de smoke test". Disclosure progressivo — não pesa no contexto se não tá em uso.
- **Workflows** — *como processos fluem*. Receitas multi-passo: como uma FIX vai de PENDENCIAS até merge, como um overnight é despachado, como uma sprint é encerrada, como uma RETRO é conduzida.

**Por quê (ideia do humano, 2026-05-29):** vem do estudo dele de padrões de design de sistemas multiagente, não de erro concreto. Mas tem sinais no nosso próprio repo que sugerem que a separação pode ajudar:

- `docs/roles/reviewer.md` hoje mistura **identidade** ("o que é um Reviewer, postura"), **skill** ("como ler arquitetura hexagonal e detectar smells" — item #8 do backlog quer adicionar) e **workflow** ("ordem dos checks numa revisão"). Cada uma dessas dimensões tem público e ciclo de vida diferentes.
- `docs/runbooks/` é uma mistura de **workflow** (PRE-MERGE-CHECKLIST, smoke test do Telegram, PREP-WA) e **skill** (ROTEIRO-TESTES-BACKEND, ROTEIRO-FRONTEND são mais "como fazer" do que "passo a passo de processo").
- O Cowork/Anthropic skills system já materializa essa separação na ferramenta (skills com `SKILL.md`, progressive disclosure, gatilhos explícitos). Adotar a mesma taxonomia internamente daria isomorfismo entre nosso repo e como o agente consome instrução.
- O item #8 (checklist arquitetural) tá tratado como "delta no reviewer.md" mas é fundamentalmente uma **skill** ("como ler arquitetura") que **vários papéis** (Reviewer hoje, Architect amanhã, talvez planner antes de aprovar plano) poderiam carregar. Forçar dentro de uma role limita reuso.

**Esforço:** médio a alto. Não é só renomear pasta — é **redesenhar como os agentes carregam contexto**. Provavelmente exige: (a) ADR registrando a taxonomia; (b) reestruturar `docs/roles/` (talvez virar `docs/agents/{roles,skills,workflows}/`); (c) revisar referências cruzadas em todos os planos e templates; (d) decidir critério de gatilho de skill (palavras-chave? região do código? heurística?). **Prioridade:** média — sistema funciona hoje, mas o custo de migrar cresce com cada role/runbook novo que escrevemos no padrão atual.

**Pra discussão na RETRO-02:**

- Vale a separação ou é over-engineering pro tamanho do projeto? (Argumento a favor: 5 roles × instruções que crescem é onde a confusão começa. Argumento contra: hoje o repo é navegável, talvez padronizar README de cada role com 3 seções fixas — "identidade", "skills carregadas", "workflows próprios" — resolva sem mover arquivo.)
- Se separar, **uma skill é compartilhada entre roles ou é dona de uma role?** (Ex.: "ler arquitetura hexagonal" — Reviewer e Architect compartilham? E o planner usa quando avalia plano antes de despachar?)
- **Mapeamento com a infra do Cowork:** as skills do Cowork (`docx`, `xlsx`, `consolidate-memory`, etc.) ficam **fora** do nosso conceito de skill (são mecânicas de ferramenta), ou a gente unifica e nossas skills viram `.skill` instaláveis também? (Provavelmente fora — Cowork skills são output-format/tool-handling, nossas seriam domain knowledge — mas vale debater.)
- **Workflows herdam de quem?** Hoje os runbooks são meio "doc do projeto", meio "instrução pro agente". Se virar workflow, ele é carregado por uma role específica (ex.: PRE-MERGE-CHECKLIST é workflow da Reviewer + dos implementadores) ou é entidade independente que qualquer role aciona?
- Critério de parada: a separação só compensa se reduzir **context bloat** ou **drift** mensuráveis. Definir antes da RETRO o que olharíamos pra dizer "deu certo" (ex.: tamanho médio de role.md cai, número de "skill genérica precisou ser duplicada em 2 roles" cai pra zero).

**Insumo concreto — análise dos prompts de dispatch (2026-05-29):**

Comparando os `MASTER-PROMPT-overnight-*.md` (multi-task) e o primeiro `DISPATCH-*.md` (single-task) escritos até agora, **~70% do conteúdo é boilerplate genérico** ("como um Claude implementador trabalha aqui") e **~30% é específico da task**. Padrões que se repetem em todos os prompts e seriam candidatos diretos a extrair:

- **Boot sequence:** "leia nesta ordem CLAUDE.md → role.md → STATE.md → sprint README → plano(s)". Idêntica em todos. Candidato a `workflows/agent-bootstrap.md`.
- **Regras duras:** code-only, sem `terraform apply`, sem SSH, sem merge, sem push direto, território da instância. Idem todos. Candidato a `workflows/implementer-guardrails.md`.
- **Criação de branch:** `git fetch && git checkout -b <branch> develop` (forma nova com worktree). Idem todos pós-2026-05-29. Candidato a `skills/criar-branch-worktree.md`.
- **Status report:** "escreva em status/<task>.md seguindo `_TEMPLATE.md`, frontmatter válido, anote X/Y/Z". Idem todos. Já tem `_TEMPLATE.md`; faltaria só a skill "como preencher por tipo de task".
- **Validação local:** mvn test + mvn package (back), npm test + npm run build (front), terraform fmt/validate (infra). Por stack. Candidato a `skills/validacao-local-por-stack.md`.
- **Handling de falha:** "se quebrar, registre `estado: parcial` no status, NÃO force". Idem todos. Candidato a rule no `workflows/implementer-guardrails.md`.
- **Encerramento:** "pare ao final do status report; não mergeie". Idem todos.

**Hipótese:** se esses pacotes virarem workflows/skills referenciáveis, os prompts colapsam pra algo tipo:

```
Você é o Claude do back. Execute <plano>, seguindo o workflow `implementer-single-task`.

Específico desta task:
- Branch: ...
- Critério de aceitação chave: ...
- Decisão a tomar no caminho: ...
```

O resto vem por referência ao workflow + skills que ele carrega. Reduz o prompt de ~80 linhas pra ~15. Atualizar o protocolo em **um** lugar (workflow) propaga pra todos os dispatches automaticamente.

**Risco a debater na RETRO-02:** se o workflow ficar muito genérico, vira mais um arquivo pra navegar sem economia real. Se ficar muito específico por tipo de task (single vs overnight vs FIX vs hotfix), reproduz a explosão de arquivos só com nome diferente. O sweet spot provavelmente é **1 workflow base ("implementer-execution") + deltas curtos por modo** (overnight adiciona resumo final, FIX adiciona regra de branch slug-only legado, etc.).

**Material a olhar antes da RETRO-02:** `docs/sprints/01-mvp/plans/MASTER-PROMPT-overnight-deploy.md`, `docs/sprints/02-canal-whatsapp/plans/MASTER-PROMPT-overnight-sprint02-1.md`, `MASTER-PROMPT-overnight-sprint02-2.md`, `DISPATCH-FIX-idempotencia-porta-application.md`. Diffar lado a lado pra ver o que é boilerplate idêntico (extrair) vs específico (manter inline).

### 11. Mudanças em `.claude/` derivadas da QA-012 — **levar ao `ai-engineer`**

**Origem:** revisão humana do status report e da avaliação da QA-012 (2026-08-10). Quatro mudanças, todas em `.claude/`, que é **território do humano e do `ai-engineer`** — nenhuma pode ser feita pelo planner sem autorização explícita e específica. Este item é o pacote de contexto para essa conversa.

**Contexto que as quatro compartilham:** a QA-012 foi o piloto do mutation testing. O que ela produziu de mais valioso não foi o número, foi o mapeamento de **quais regras do projeto existiam só em prosa**. As decisões abaixo já foram tomadas pelo humano; falta escrever, no lugar certo, por quem tem território.

#### 11.1 — `writing-java-unit-tests`: asserção sobre **valor**, não sobre ocorrência de chamada

**Achado.** Das quatro classes do piloto, `PaymentRequestStrategy` e `PaymentProofStrategy` mataram **20/20 mutantes** — zero sobreviventes. O motivo é o mesmo nas duas, e é replicável: os testes verificam **o valor dos argumentos** que chegam ao colaborador, não apenas que o colaborador foi chamado.

O **mecanismo** difere, e isso importa porque não existe um jeito canônico:

- `PaymentRequestStrategyTest` usa `ArgumentCaptor` (3 ocorrências) e assere campo a campo o `PedidoPagamento` construído — valor, descrição, status, `telegramUserId`, `fileIdTelegram`, `imagemUrl`, `requisitanteId`, `dataPedido`.
- `PaymentProofStrategyTest` **não usa `ArgumentCaptor`** (zero ocorrências). Fixa cada argumento com `eq(...)` dentro do próprio `verify` — `execute(eq(123L), eq("PIX"), eq("file_xyz"), any(), eq(TipoArquivo.IMAGEM), eq(12345L))`. A strategy chama um usecase com parâmetros soltos; não há objeto de domínio a capturar.

`ArgumentCaptor` e `eq(...)` são dois caminhos para o mesmo lugar. O que mata mutante é **asserção sobre valor**; `verify(mock).metodo(any())` não mata nada.

**Lacuna na skill hoje.** A `writing-java-unit-tests` §Mandatory rules tem a regra do eixo **oposto** — *"Keep `verify(...)` calls limited to interactions that matter for the behavior being tested, not incidental calls"*, que é sobre **não sobre-verificar**. Não existe regra dizendo para asserir o valor. Um agente que siga a skill ao pé da letra pode escrever `verify(mock).metodo(any())` e estar formalmente correto.

**Proposta:** 1 regra em §Mandatory rules + exemplo comparativo em `references/java-junit-mockito.md` mostrando os dois mecanismos lado a lado, com o contraexemplo (`any()`) explícito.

#### 11.2 — `artifact-report-contract`: rótulo qualitativo não pode contradizer o número ao lado

**Achado.** O Reviewer pegou, na QA-012, uma seção que rotulava `LegendaParser` como "cobertura **alta**" com **94%** e `PaymentRequestStrategy` como "cobertura **baixa**" com **96%** — a classe rotulada "baixa" tinha cobertura **maior**. O conteúdo dos dois casos estava correto; o rótulo destruía o argumento. E a tabela-resumo no fim da seção era indexada por esses mesmos rótulos.

**Proposta:** 1 linha em §Mandatory rules — *rótulo qualitativo deve ser consistente com o dado quantitativo apresentado junto dele*.

**Contra-argumento a considerar na conversa, honestamente:** o Reviewer pegou isso **sem a regra existir**. O valor marginal de escrever é baixo. O custo também é (uma linha), e a classe de erro é real e recorrente em relatório com muitos números. Decisão do `ai-engineer` sobre se paga.

#### 11.3 — `planner.md`: perguntar sobre o gate de mutação ao escrever o plano

**Decisão do humano (2026-08-10):** o gate de mutation testing é **opcional e por task**, não global. O critério é `test strength ≥ 80%` medido **apenas sobre as classes alteradas** pela task — código antigo não entra no denominador. A justificativa completa (por que `test strength` e não mutation score cru; por que só o código alterado; por que 80% e não 100%) está em `financas_bot_telegram/CLAUDE.md` §"Critério de mutation testing — gate opcional".

**O que falta, e é território do `ai-engineer`:** a regra de que **o planner deve perguntar ao humano, ao escrever o plano de uma task de backend, se aquela task adota o gate de mutação**. Sem isso o gate é opcional na teoria e inexistente na prática — ninguém lembra de oferecê-lo.

**Onde mora:** `.claude/agents/planner.md`, no Required Workflow Pattern (junto dos passos que já definem `review_required` / `qa_required`) ou como ponto de aprovação humana explícito. Provavelmente vira um campo `mutation_gate` no frontmatter do plano, espelhando `qa_required` — **mas isso encosta no schema de `artifact-report-contract` e no `_TEMPLATE-plano.md`**, então a mudança não é só no agente. Ponto a fechar com o `ai-engineer`.

#### 11.4 — Referências penduradas: as skills apontam para `.github/instructions/`, que não existe — **correção necessária**

> **Decisão do humano (2026-08-10): isso precisa mudar.** Não é item para avaliar se vale — é defeito a corrigir. O que falta decidir é **qual das duas correções** abaixo, e isso é do `ai-engineer`.

**Achado colateral do mapeamento** (2026-08-10, verificado por `ls`): duas skills remetem decisões a um caminho inexistente neste repo —

- `artifact-report-contract/SKILL.md:29` → *"Deciding where a file is saved or how it is named — that is defined in `.github/instructions/artifact-placement-and-naming.instructions.md`"*
- `workflow-gates-core/SKILL.md:29` → mesma referência

`.github/` neste repo contém **apenas `workflows/`**. Não há `instructions/`. As regras reais de placement e naming vivem em `CLAUDE.md` (raiz), `docs/runbooks/PRE-MERGE-CHECKLIST.md` e ADR 0010.

**Por que importa:** um agente que siga a skill vai procurar um arquivo que não existe. O comportamento nesse caso é indefinido — na melhor hipótese ele improvisa, na pior ele para. É resíduo de portabilidade (as skills foram escritas em formato agnóstico, mirando um layout VS Code/Copilot que este repo não adota).

**As duas correções possíveis:**

| | (a) Apontar as skills para os docs reais | (b) Criar `.github/instructions/…` de fato |
|---|---|---|
| Custo | 2 linhas | arquivo novo + manutenção |
| Duplicação de regra | nenhuma — aponta para a fonte única | risco alto: passa a existir uma segunda cópia das regras de placement, que vai divergir |
| Portabilidade das skills | perde — as skills passam a citar caminhos deste repo | preserva o formato agnóstico original |
| Quem mantém sincronizado | ninguém precisa | alguém precisa, e é o modo de falha conhecido |

**Recomendação do planner (a decisão é do `ai-engineer`):** opção **(a)**. A portabilidade que a (b) preserva é hipotética — não há outro repo consumindo estas skills — enquanto a duplicação que ela cria é certa. E o repo já tem precedente do estrago: `docs/experiments/…/05-instrumentacao-e-harness.md` desatualizou em dois meses justamente por descrever estado que vive em outro lugar (ver §"Itens ainda não detalhados" do backlog da sprint 04).

**Escopo da correção:** `artifact-report-contract/SKILL.md:29` e `workflow-gates-core/SKILL.md:29`. Vale varrer as demais skills atrás de outras referências a `.github/` antes de fechar — `creating-agents/SKILL.md:34` cita `.github/agents/` num contexto histórico (formato VS Code arquivado) que provavelmente é legítimo, mas merece conferência.

---

## Fora deste backlog (rastreado em outro lugar)

- Deploy DEP-03 a DEP-06 → `FASE-3-VISUALIZACAO.md`.
- Headers de segurança no CloudFront, rotação do token Telegram, `keystore_password` no Secrets Manager → `PENDENCIAS-TECNICAS.md`.
