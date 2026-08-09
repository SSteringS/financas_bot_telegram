<!-- Fonte: https://code.claude.com/docs/pt/glossary | Baixado em: 2026-08-06 | slug: glossary -->

# Glossário

> Definições da terminologia do Claude Code. Aprenda o que significam agentic loop, compaction, CLAUDE.md, hooks, subagents, MCP e outros conceitos principais.

Este glossário define a terminologia do Claude Code. Cada entrada vincula à página onde o conceito é abordado em profundidade. Para conceitos em nível de modelo como tokens, temperature e RAG, consulte o [glossário da plataforma](https://platform.claude.com/docs/pt/about-claude/glossary).

<h2 id="a">
  A
</h2>

<h3 id="agent-teams">
  Agent teams
</h3>

Múltiplas sessões independentes do Claude Code coordenadas por um líder de equipe, com uma lista de tarefas compartilhada e mensagens ponto a ponto. Diferentemente de [subagents](#subagent), que executam dentro de uma única sessão e relatam apenas ao pai, os membros da equipe têm cada um sua própria janela de contexto e você pode interagir com qualquer um deles diretamente. Agent teams são experimentais e devem ser habilitados definindo `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`.

Saiba mais: [Run agent teams](https://code.claude.com/docs/pt/agent-teams)

<h3 id="agentic-coding">
  Agentic coding
</h3>

Um fluxo de trabalho onde a IA pode ler arquivos, executar comandos e fazer alterações autonomamente enquanto você observa, redireciona ou se afasta, em contraste com assistentes baseados em chat que apenas respondem com texto que você deve aplicar você mesmo. Claude Code é agentic porque possui [tools](#tool) que permitem agir, não apenas aconselhar.

Saiba mais: [How Claude Code works](https://code.claude.com/docs/pt/how-claude-code-works)

<h3 id="agentic-harness">
  Agentic harness
</h3>

As tools, gerenciamento de contexto e ambiente de execução que transformam um modelo de linguagem em um agente de codificação capaz. Claude Code é o harness; Claude é o modelo dentro dele. O harness fornece acesso a arquivos, execução de shell, gating de permissões, carregamento de memória e o loop que encadeia ações juntas.

Saiba mais: [How Claude Code works](https://code.claude.com/docs/pt/how-claude-code-works)

<h3 id="agentic-loop">
  Agentic loop
</h3>

O ciclo que Claude percorre para cada tarefa: reunir contexto, tomar ação, verificar resultados e repetir até terminar. Cada uso de tool retorna informações que informam o próximo passo. Você pode interromper o loop em qualquer ponto para redirecionar. A maioria dos pontos de extensão, incluindo [hooks](#hook), [skills](#skill) e [MCP](#mcp-model-context-protocol), se conectam a fases específicas deste loop.

Saiba mais: [How Claude Code works](https://code.claude.com/docs/pt/how-claude-code-works#the-agentic-loop)

<h3 id="artifact">
  Artifact
</h3>

Uma página web ao vivo e interativa que Claude Code publica de sua sessão para uma URL privada em claude.ai, para que você possa ver a saída visualmente ou compartilhá-la em vez de ler texto de terminal. A página é atualizada no local quando a sessão republica. Os artefatos que você cria a partir do Claude Code aparecem na mesma galeria que os artefatos criados em conversas do claude.ai. O compartilhamento depende do seu plano: em Pro e Max, um link público que qualquer pessoa pode abrir; em Team e Enterprise, compartilhamento dentro de sua organização, além de links públicos uma vez que um Owner os habilita.

Saiba mais: [Share session output as artifacts](https://code.claude.com/docs/pt/artifacts)

<h3 id="auto-memory">
  Auto memory
</h3>

Notas que Claude escreve para si mesmo com base em suas correções e preferências, armazenadas por repositório git em `~/.claude/projects/`. Todos os worktrees do mesmo repositório compartilham um diretório de auto memory. As primeiras 200 linhas ou 25 KB do índice `MEMORY.md` carregam no início de cada sessão. Auto memory é a contrapartida escrita por Claude para [CLAUDE.md](#claude-md), que você escreve.

Saiba mais: [Auto memory](https://code.claude.com/docs/pt/memory#auto-memory)

<h3 id="auto-mode">
  Auto mode
</h3>

Um [permission mode](#permission-mode) onde um modelo classificador separado revisa ações em segundo plano, para que a maioria seja executada sem prompts de aprovação; regras de solicitação explícita ainda solicitam. O classificador bloqueia escalação de escopo, infraestrutura não confiável e [prompt injection](#prompt-injection). Ele nunca vê resultados de tool, então instruções injetadas não podem influenciar suas decisões.

Saiba mais: [Eliminate prompts with auto mode](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode)

<h2 id="b">
  B
</h2>

<h3 id="bare-mode">
  Bare mode
</h3>

Uma flag de inicialização, `--bare`, que pula a descoberta automática de hooks, skills, plugins, servidores MCP, auto memory e CLAUDE.md. Apenas flags que você passa explicitamente têm efeito. Recomendado para CI e chamadas com script onde você precisa de comportamento idêntico entre máquinas independentemente da configuração local.

Saiba mais: [Start faster with bare mode](https://code.claude.com/docs/pt/headless#start-faster-with-bare-mode)

<h3 id="bundled-skills">
  Bundled skills
</h3>

Playbooks baseados em prompt incluídos com Claude Code, como `/batch`, `/code-review`, `/debug` e `/loop`. Diferentemente de comandos built-in, que executam lógica fixa, bundled skills dão a Claude um prompt detalhado e deixam que ele orquestre o trabalho, então podem gerar agentes, ler arquivos e se adaptar à sua base de código.

Saiba mais: [Bundled skills](https://code.claude.com/docs/pt/skills#bundled-skills)

<h2 id="c">
  C
</h2>

<h3 id="channel">
  Channel
</h3>

Um [MCP server](#mcp-model-context-protocol) que envia eventos para sua sessão em execução para que Claude possa reagir a coisas que acontecem enquanto você está longe do terminal. Channels podem ser bidirecionais: Claude lê um evento de entrada e responde de volta através do mesmo channel. Telegram, Discord e iMessage estão incluídos na visualização de pesquisa.

Saiba mais: [Channels](https://code.claude.com/docs/pt/channels)

<h3 id="checkpoint">
  Checkpoint
</h3>

Um ponto de restauração criado a cada prompt que você envia. Claude Code captura snapshots de arquivos antes de cada edição para que um checkpoint possa revertê-los. Pressione `Esc` duas vezes ou execute `/rewind` para restaurar código, conversa ou ambos para um ponto anterior, ou para resumir parte da conversa a partir de uma mensagem selecionada. Checkpoints são salvos com a conversa, portanto uma sessão retomada ainda pode `/rewind` para eles. Eles são separados do git e não rastreiam alterações feitas através da ferramenta Bash.

Saiba mais: [Checkpointing](https://code.claude.com/docs/pt/checkpointing)

<h3 id="claude-directory">
  `.claude` directory
</h3>

O diretório onde Claude Code lê configuração com escopo de projeto: settings, hooks, skills, subagents, rules e auto memory. Um projeto tem `.claude/` em sua raiz; seus padrões em nível de usuário estão em `~/.claude/`.

Saiba mais: [The `.claude` directory](https://code.claude.com/docs/pt/claude-directory)

<h3 id="claude-md">
  CLAUDE.md
</h3>

Um arquivo markdown de instruções persistentes que você escreve para Claude, carregado no início de cada sessão como uma mensagem de usuário após o prompt do sistema. Coloque convenções de projeto, notas de arquitetura e regras "sempre faça X" aqui. CLAUDE.md na raiz do projeto sobrevive a [compaction](#compaction) e é relido fresco do disco depois.

Você pode colocar CLAUDE.md no escopo do projeto em `./CLAUDE.md` ou `./.claude/CLAUDE.md`, no escopo do usuário em `~/.claude/CLAUDE.md`, ou como [managed policy](#managed-settings) para sua organização. Todos os arquivos descobertos são concatenados no contexto em vez de se sobreporem, ordenados do escopo mais amplo para o mais específico.

Saiba mais: [CLAUDE.md files](https://code.claude.com/docs/pt/memory#claude-md-files)

<h3 id="command">
  Command
</h3>

Uma instrução reutilizável que você invoca digitando `/name` no prompt. Comandos built-in como `/clear`, `/model` e `/compact` controlam a sessão. Você pode definir seus próprios comandos como arquivos em `.claude/commands/`, ou instalá-los de um [plugin](#plugin). [Skills](#skill) são a forma recomendada de empacotar comandos multi-etapa.

Saiba mais: [Commands](https://code.claude.com/docs/pt/commands) · [Skills](https://code.claude.com/docs/pt/skills)

<h3 id="compaction">
  Compaction
</h3>

Sumarização automática de sua conversa quando a [context window](#context-window) se aproxima de seu limite. Saídas de tool mais antigas são limpas primeiro, depois a conversa é sumarizada. CLAUDE.md na raiz do projeto e auto memory sobrevivem a compaction e recarregam do disco; instruções dadas apenas em conversa podem ser perdidas. Execute `/compact` para disparar manualmente, opcionalmente com um foco como `/compact focus on the API changes`.

Saiba mais: [What survives compaction](https://code.claude.com/docs/pt/context-window#what-survives-compaction) · [When context fills up](https://code.claude.com/docs/pt/how-claude-code-works#when-context-fills-up)

<h3 id="context-window">
  Context window
</h3>

A memória de trabalho para uma sessão, contendo histórico de conversa, conteúdos de arquivo, saídas de comando, CLAUDE.md, auto memory, skills carregadas e instruções do sistema. Conforme você trabalha, o contexto se enche até que [compaction](#compaction) o resuma. Execute `/context` para ver o que está usando espaço. Para o conceito de modelo subjacente, consulte o [glossário da plataforma](https://platform.claude.com/docs/pt/about-claude/glossary#context-window).

Saiba mais: [Explore the context window](https://code.claude.com/docs/pt/context-window)

<h2 id="d">
  D
</h2>

<h3 id="dispatch">
  Dispatch
</h3>

Um roteador de tarefas iniciado por telefone que gera uma sessão do Claude Code no aplicativo Desktop quando você envia uma tarefa de codificação do aplicativo móvel Claude. Seu prompt é roteado para a ferramenta certa automaticamente. Disponível em planos Pro e Max.

Saiba mais: [Sessions from Dispatch](https://code.claude.com/docs/pt/desktop#sessions-from-dispatch)

<h2 id="e">
  E
</h2>

<h3 id="effort-level">
  Effort level
</h3>

Uma configuração que controla quanto do orçamento de pensamento de raciocínio adaptativo Claude usa em cada turno. Esforço mais alto significa mais tokens de pensamento e raciocínio mais profundo; esforço mais baixo é mais rápido e barato. Effort é suportado em Fable 5, em Opus 4.6 e posterior, e em Sonnet 4.6 e posterior.

Saiba mais: [Adjust effort level](https://code.claude.com/docs/pt/model-config#adjust-effort-level)

<h3 id="extended-thinking">
  Extended thinking
</h3>

Raciocínio passo a passo visível que o modelo realiza antes de responder. Você pode ajustá-lo com o [effort level](#effort-level), ou limitar tokens de pensamento com `MAX_THINKING_TOKENS` em modelos com um orçamento de pensamento fixo. Thinking aparece em texto itálico cinza no terminal.

Saiba mais: [Use extended thinking](https://code.claude.com/docs/pt/model-config#extended-thinking)

<h2 id="h">
  H
</h2>

<h3 id="hook">
  Hook
</h3>

Um manipulador definido pelo usuário que executa automaticamente em um ponto específico do ciclo de vida do Claude Code, como antes de uma tool ser executada, após uma edição de arquivo ou no início da sessão. Manipuladores podem ser um comando shell, endpoint HTTP, tool MCP, prompt LLM ou subagent. Hooks são determinísticos: eles disparam em pontos de ciclo de vida fixos em vez de à discrição do modelo.

Uma configuração de hook tem três níveis:

* **Hook event**: o ponto do ciclo de vida
* **Matcher**: filtra quais eventos o disparam
* **Hook handler**: o que executa

Saiba mais: [Get started with hooks](https://code.claude.com/docs/pt/hooks-guide) · [Hooks reference](https://code.claude.com/docs/pt/hooks)

<h2 id="m">
  M
</h2>

<h3 id="managed-settings">
  Managed settings
</h3>

Configurações impostas em toda a organização por TI ou DevOps, entregues pelos servidores da Anthropic através do console de administração ou implantadas em dispositivos em um caminho em nível de SO fora de `~/.claude`. Os usuários não podem substituir managed settings de escopos com menor precedência. A entrega gerenciada pelo servidor se aplica em [configurações elegíveis](https://code.claude.com/docs/pt/server-managed-settings#platform-availability); consulte [Considerações de segurança](https://code.claude.com/docs/pt/server-managed-settings#security-considerations). Use isso para políticas de segurança, requisitos de conformidade ou ferramentas padronizadas em uma frota.

Saiba mais: [Server-managed settings](https://code.claude.com/docs/pt/server-managed-settings) · [Settings files](https://code.claude.com/docs/pt/settings#settings-files)

<h3 id="mcp-model-context-protocol">
  MCP (Model Context Protocol)
</h3>

Um padrão aberto para conectar tools de IA a fontes de dados externas e serviços. Servidores MCP dão a Claude novas tools para Slack, Jira, bancos de dados, navegadores e centenas de outras integrações. Você conecta servidores via `/mcp` ou adicionando-os a `.mcp.json`. Para o protocolo em si, consulte o [glossário da plataforma](https://platform.claude.com/docs/pt/about-claude/glossary#mcp-model-context-protocol).

Saiba mais: [Model Context Protocol](https://code.claude.com/docs/pt/mcp)

<h3 id="mcp-tool-search">
  MCP Tool Search
</h3>

Um mecanismo de economia de contexto que adia schemas de tool MCP até serem necessários. Apenas nomes de tool carregam na inicialização; Claude busca o schema completo sob demanda quando decide usar uma tool específica. Isso evita que servidores MCP ociosos consumam muito contexto.

Saiba mais: [Scale with MCP Tool Search](https://code.claude.com/docs/pt/mcp#scale-with-mcp-tool-search)

<h2 id="n">
  N
</h2>

<h3 id="non-interactive-mode">
  Non-interactive mode
</h3>

Um modo que executa um único prompt e sai sem uma sessão conversacional, invocado com `-p` ou `--print`. Usado para CI, scripts e piping. A execução ainda é salva como uma sessão retomável, a menos que você passe `--no-session-persistence`. O [Agent SDK](https://code.claude.com/docs/pt/agent-sdk/overview) é o equivalente em Python e TypeScript. Anteriormente chamado de headless mode.

Saiba mais: [Run Claude Code programmatically](https://code.claude.com/docs/pt/headless)

<h2 id="o">
  O
</h2>

<h3 id="output-style">
  Output style
</h3>

Uma configuração que modifica o prompt do sistema de Claude para alterar comportamento de resposta, tom ou formato. Output styles desligam as partes específicas de engenharia de software do prompt do sistema padrão, diferentemente de [CLAUDE.md](#claude-md) que é entregue como uma mensagem de usuário seguindo o prompt do sistema. Estilos built-in incluem Default, Proactive, Explanatory e Learning.

Saiba mais: [Output styles](https://code.claude.com/docs/pt/output-styles)

<h2 id="p">
  P
</h2>

<h3 id="permission-mode">
  Permission mode
</h3>

O comportamento de aprovação de linha de base para a sessão. Cicle com `Shift+Tab` na CLI ou use o seletor de modo em VS Code, Desktop e claude.ai. Os modos disponíveis são `default`, `acceptEdits`, `plan`, `auto`, `dontAsk` e `bypassPermissions`.

O modo `default` é rotulado como Manual na CLI e nas extensões VS Code e JetBrains, e Claude Code aceita `manual` como um alias para o valor.

Saiba mais: [Escolha um permission mode](https://code.claude.com/docs/pt/permission-modes)

<h3 id="permission-rule">
  Permission rule
</h3>

Uma entrada de settings que permite, pergunta sobre ou nega uma invocação de tool com base no nome da tool e padrão de argumento. Regras são avaliadas deny→ask→allow, primeira correspondência vence. Permission rules são controles de granulação fina em camadas sobre o [permission mode](#permission-mode) mais amplo.

Saiba mais: [Configure permissions](https://code.claude.com/docs/pt/permissions)

<h3 id="plan-mode">
  Plan mode
</h3>

Um [permission mode](#permission-mode) onde Claude pesquisa e propõe alterações sem editar seus arquivos de origem. Pode ler, pesquisar e executar comandos de exploração, depois apresenta um plano para aprovação antes de tocar em qualquer coisa. Entre em plan mode com `/plan` ou pressionando `Shift+Tab`.

Saiba mais: [Analise antes de editar com plan mode](https://code.claude.com/docs/pt/permission-modes#analyze-before-you-edit-with-plan-mode)

<h3 id="plugin">
  Plugin
</h3>

Um pacote de skills, hooks, subagents e servidores MCP empacotados como uma unidade instalável única. Plugin skills são nomeados como `plugin-name:skill-name` para que múltiplos plugins coexistam. Distribua plugins entre equipes via um [marketplace](https://code.claude.com/docs/pt/plugin-marketplaces).

Saiba mais: [Plugins](https://code.claude.com/docs/pt/plugins)

<h3 id="project-trust">
  Project trust
</h3>

Um diálogo aceitando um diretório antes que Claude Code carregue sua configuração. A aceitação é salva por diretório de projeto, exceto seu diretório home, onde a confiança é mantida apenas para a sessão atual e o prompt reaparece a cada inicialização. Trust gates auto-instalação de plugins de marketplace e execução de hooks definidos pelo projeto. Confiar em um diretório significa que seus arquivos `.claude/settings.json`, `.mcp.json` e outros arquivos de config têm efeito.

Saiba mais: [The `.claude` directory](https://code.claude.com/docs/pt/claude-directory)

<h3 id="prompt-injection">
  Prompt injection
</h3>

Instruções hostis incorporadas em um arquivo, página web ou resultado de tool que tentam redirecionar Claude para ações que você nunca pediu. As defesas do Claude Code incluem o sistema de permissões, detecção de injeção de comando e verificação de confiança. [Auto mode](#auto-mode) adiciona uma sonda do lado do servidor que escaneia resultados de tool para conteúdo suspeito e um classificador que nunca vê resultados de tool, então texto injetado não pode influenciar suas decisões de aprovação.

Saiba mais: [Proteja-se contra prompt injection](https://code.claude.com/docs/pt/security#protect-against-prompt-injection)

<h2 id="r">
  R
</h2>

<h3 id="remote-control">
  Remote Control
</h3>

Uma forma de continuar uma sessão local do Claude Code do seu telefone ou navegador via claude.ai. Seu código fica em sua máquina; apenas a UI é remota. Diferente de Claude Code na web, que executa em um sandbox na nuvem.

Saiba mais: [Remote Control](https://code.claude.com/docs/pt/remote-control)

<h3 id="rules">
  Rules
</h3>

Arquivos de instrução modular em `.claude/rules/` que carregam junto com CLAUDE.md. Uma rule pode ser com escopo de caminho com frontmatter YAML `paths:` para que carregue apenas quando Claude lê um arquivo correspondente, mantendo o contexto enxuto até que seja relevante.

Saiba mais: [Organize rules with `.claude/rules/`](https://code.claude.com/docs/pt/memory#organize-rules-with-claude/rules/)

<h2 id="s">
  S
</h2>

<h3 id="sandboxing">
  Sandboxing
</h3>

Isolamento de filesystem e rede em nível de SO para a tool Bash. Comandos executam dentro de um limite que você define antecipadamente, para que Claude possa trabalhar livremente dentro dele sem prompts de aprovação por comando. Sandboxing é uma camada separada de [permission rules](#permission-rule).

Saiba mais: [Sandboxing](https://code.claude.com/docs/pt/sandboxing)

<h3 id="session">
  Session
</h3>

Uma conversa vinculada ao seu diretório atual, com sua própria [context window](#context-window) independente. Sessões podem ser retomadas com `claude -c`, bifurcadas com `--fork-session` para preservar histórico sob um novo ID de sessão, ou executadas em paralelo entre terminais. Executar `/clear` inicia uma nova sessão; a anterior fica armazenada e está disponível via `/resume`. A transcrição de cada sessão é armazenada em `~/.claude/projects/`.

Saiba mais: [Work with sessions](https://code.claude.com/docs/pt/how-claude-code-works#work-with-sessions)

<h3 id="settings-layers">
  Settings layers
</h3>

A hierarquia que Claude Code lê configuração, em ordem de precedência de mais alta para mais baixa: [managed policy](#managed-settings), argumentos de linha de comando, settings locais em `.claude/settings.local.json`, settings de projeto em `.claude/settings.json`, depois settings de usuário em `~/.claude/settings.json`. Arrays se mesclam entre camadas; escalares em uma camada mais alta substituem as mais baixas.

Saiba mais: [Settings files](https://code.claude.com/docs/pt/settings#settings-files)

<h3 id="skill">
  Skill
</h3>

Um arquivo `SKILL.md` contendo instruções, conhecimento ou um fluxo de trabalho que Claude adiciona ao seu toolkit. Claude carrega uma skill automaticamente quando relevante, ou você a invoca diretamente com `/skill-name`. Skills seguem o padrão aberto Agent Skills; Claude Code o estende com controle de invocação e execução de subagent.

Skills são o sucessor recomendado para comandos customizados. Um arquivo em `.claude/commands/deploy.md` e um em `.claude/skills/deploy/SKILL.md` ambos criam `/deploy` e funcionam da mesma forma; arquivos de comando existentes continuam funcionando.

Saiba mais: [Extend Claude with skills](https://code.claude.com/docs/pt/skills)

<h3 id="subagent">
  Subagent
</h3>

Um assistente de IA especializado que executa em sua própria context window com um prompt do sistema customizado, acesso a tool específico e permissões independentes. Funciona em uma tarefa delegada e retorna um resumo para a conversa principal. Use subagents para manter grandes explorações fora do seu contexto primário ou para executar pesquisa paralela. Diferente de [agent teams](#agent-teams), onde cada agente é uma sessão independente completa com a qual você pode falar diretamente.

Subagents built-in incluem Explore, Plan e propósito geral.

Saiba mais: [Create custom subagents](https://code.claude.com/docs/pt/sub-agents)

<h3 id="surface">
  Surface
</h3>

Qualquer lugar onde você acessa Claude Code: a CLI, VS Code, JetBrains, Desktop ou claude.ai. Todas as surfaces compartilham o mesmo engine, então seu CLAUDE.md, settings e skills funcionam da mesma forma entre elas. Slack e a extensão Chrome são integrações que se conectam a uma surface em vez de surfaces em si.

Saiba mais: [Platforms and integrations](https://code.claude.com/docs/pt/platforms)

<h2 id="t">
  T
</h2>

<h3 id="teleport">
  Teleport
</h3>

Um comando, `/teleport`, que puxa uma sessão Claude Code na nuvem para seu terminal local. Claude busca o branch, carrega o histórico de conversa e retoma do último estado da sessão web. A direção reversa é `--cloud`, que envia uma tarefa local para executar na web.

Saiba mais: [Da web para o terminal](https://code.claude.com/docs/pt/claude-code-on-the-web#from-web-to-terminal)

<h3 id="tool">
  Tool
</h3>

Uma ação que Claude pode tomar: ler um arquivo, editar código, executar um comando shell, pesquisar a web, gerar um subagent. Tools são o que tornam Claude Code agentic. Sem elas, Claude pode apenas responder com texto. Cada uso de tool retorna um resultado que informa a próxima decisão de Claude no [agentic loop](#agentic-loop).

Saiba mais: [Tools available to Claude](https://code.claude.com/docs/pt/tools-reference)

<h3 id="turn">
  Turn
</h3>

Uma resposta completa de Claude dentro de uma [session](#session). Um turn começa quando você envia uma mensagem e termina quando Claude termina de responder, com qualquer número de chamadas de [tool](#tool) no meio. [Stop hooks](#hook) são acionados no final de cada turn. Uma session consiste em muitos turns, e o [agentic loop](#agentic-loop) descreve o que acontece dentro de um.

Saiba mais: [How Claude Code works](https://code.claude.com/docs/pt/how-claude-code-works#the-agentic-loop)

<h2 id="v">
  V
</h2>

<h3 id="verification-loop">
  Verification loop
</h3>

Como uma sessão sabe que o trabalho está realmente feito em vez de apenas plausível. Você dá a Claude uma verificação que ele pode executar, como um conjunto de testes, uma compilação ou uma comparação de screenshot, e Claude itera até que a verificação passe em vez de parar após uma tentativa. Um verification loop é o pré-requisito para [`/goal`](https://code.claude.com/docs/pt/goal), execuções desassistidas e [dynamic workflows](https://code.claude.com/docs/pt/workflows): sem um, a única coisa decidindo que o agente terminou é o próprio agente.

Saiba mais: [Give Claude a way to verify its work](https://code.claude.com/docs/pt/best-practices#give-claude-a-way-to-verify-its-work)

<h2 id="w">
  W
</h2>

<h3 id="worktree-isolation">
  Worktree isolation
</h3>

Um modo de isolamento que executa Claude em um git worktree separado em `.claude/worktrees/`, habilitado com a flag `-w` ou `isolation: worktree` na config de subagent. Alterações ficam em um branch separado em um diretório separado, para que agentes paralelos não sobrescrevam os arquivos uns dos outros.

Saiba mais: [Run parallel sessions with git worktrees](https://code.claude.com/docs/pt/worktrees)

***

<h2 id="deprecated-and-renamed-terms">
  Termos descontinuados e renomeados
</h2>

Estes termos aparecem em docs mais antigas, posts de blog e conteúdo da comunidade. Use o nome atual ao pesquisar neste site.

| Old term        | Now called                                    | Notes                                |
| --------------- | --------------------------------------------- | ------------------------------------ |
| Headless mode   | [Non-interactive mode](#non-interactive-mode) | Same `-p` flag, same behavior        |
| Custom commands | [Skills](#skill)                              | `.claude/commands/` files still work |
| Slash commands  | Commands                                      | "Slash" dropped from product copy    |
