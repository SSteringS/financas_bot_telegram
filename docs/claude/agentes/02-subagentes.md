<!-- Fonte: https://code.claude.com/docs/pt/sub-agents | Baixado em: 2026-08-06 | slug: sub-agents -->

# Criar subagentes personalizados

> Crie e use subagentes de IA especializados no Claude Code para fluxos de trabalho específicos de tarefas e gerenciamento de contexto aprimorado.

Subagentes são assistentes de IA especializados que lidam com tipos específicos de tarefas. Use um quando uma tarefa secundária inundaria sua conversa principal com resultados de pesquisa, logs ou conteúdos de arquivo que você não referenciará novamente: o subagente faz esse trabalho em seu próprio contexto e retorna apenas o resumo. Defina um subagente personalizado quando você continua gerando o mesmo tipo de worker com as mesmas instruções.

Cada subagente é executado em sua própria janela de contexto com um prompt de sistema personalizado, acesso a ferramentas específicas e permissões independentes. Quando Claude encontra uma tarefa que corresponde à descrição de um subagente, ele delega para esse subagente, que funciona independentemente e retorna resultados. Para ver a economia de contexto na prática, a [visualização da janela de contexto](https://code.claude.com/docs/pt/context-window) apresenta uma sessão onde um subagente lida com pesquisa em sua própria janela separada.

<Note>
  Subagentes funcionam dentro de uma única sessão. Para executar muitas sessões independentes em paralelo e monitorá-las de um único lugar, consulte [agentes em segundo plano](https://code.claude.com/docs/pt/agent-view). Para sessões que se comunicam entre si, consulte [equipes de agentes](https://code.claude.com/docs/pt/agent-teams).
</Note>

Subagentes ajudam você a:

* **Preservar contexto** mantendo exploração e implementação fora de sua conversa principal
* **Aplicar restrições** limitando quais ferramentas um subagente pode usar
* **Reutilizar configurações** entre projetos com subagentes no nível do usuário
* **Especializar comportamento** com prompts de sistema focados para domínios específicos
* **Controlar custos** roteando tarefas para modelos mais rápidos e baratos como Haiku

Claude usa a descrição de cada subagente para decidir quando delegar tarefas. Quando você cria um subagente, escreva uma descrição clara para que Claude saiba quando usá-lo.

Claude Code inclui vários subagentes integrados como Explore, Plan e general-purpose. Você também pode criar subagentes personalizados para lidar com tarefas específicas.

<h2 id="built-in-subagents">
  Subagentes integrados
</h2>

Claude Code inclui subagentes integrados que Claude usa automaticamente quando apropriado. Cada um herda as permissões da conversa pai com restrições de ferramentas adicionais.

Explore e Plan pulam seus arquivos CLAUDE.md e o status git da sessão pai para manter a pesquisa rápida e econômica. Todos os outros subagentes integrados e [subagentes personalizados](#configure-subagents) carregam ambos. Para o detalhamento completo do que chega a um subagente, consulte [o que é carregado na inicialização](#what-loads-at-startup).

<Tabs>
  <Tab title="Explore">
    Um agente rápido e somente leitura otimizado para pesquisar e analisar bases de código.

    * **Model**: herda da conversa principal, limitado a Opus na Claude API, portanto Explore nunca é executado em um modelo mais caro do que aquele que você já escolheu para a sessão
    * **Tools**: ferramentas somente leitura; Write e Edit são negados
    * **Purpose**: descoberta de arquivos, pesquisa de código, exploração de base de código

    A partir da v2.1.198, Explore herda o modelo da conversa principal em vez de sempre ser executado em Haiku. Na Claude API, o modelo herdado é limitado a Opus: uma conversa principal em um nível superior executa Explore em Opus, e uma conversa principal em Sonnet ou Haiku executa Explore nesse mesmo modelo. Em qualquer outro provedor, como [Amazon Bedrock, Google Cloud's Agent Platform, Microsoft Foundry, ou Claude Platform on AWS](https://code.claude.com/docs/pt/third-party-integrations), Explore herda o modelo da conversa principal diretamente.

    Um [subagente de usuário ou projeto](#choose-the-subagent-scope) nomeado `Explore` substitui o integrado e mantém seu próprio campo `model`, portanto defina um com `model: haiku` para manter a exploração em um modelo de menor custo.

    Claude delega para Explore quando precisa pesquisar ou entender uma base de código sem fazer alterações. Isso mantém os resultados da exploração fora do contexto da sua conversa principal.

    Ao invocar Explore, Claude especifica um nível de minuciosidade: **quick** para buscas direcionadas, **medium** para exploração equilibrada, ou **very thorough** para análise abrangente.
  </Tab>

  <Tab title="Plan">
    Um agente de pesquisa usado durante [plan mode](https://code.claude.com/docs/pt/permission-modes#analyze-before-you-edit-with-plan-mode) para reunir contexto antes de apresentar um plano.

    * **Model**: herda da conversa principal
    * **Tools**: ferramentas somente leitura; Write e Edit são negados
    * **Purpose**: pesquisa de base de código para planejamento

    Quando você está em plan mode e Claude precisa entender sua base de código, ele delega a pesquisa para o subagente Plan para que a saída de exploração permaneça em uma janela de contexto separada enquanto a conversa principal permanece somente leitura.
  </Tab>

  <Tab title="General-purpose">
    Um agente capaz para tarefas complexas e multi-etapas que requerem exploração e ação.

    * **Model**: herda da conversa principal
    * **Tools**: todas as ferramentas
    * **Purpose**: pesquisa complexa, operações multi-etapas, modificações de código

    Claude delega para general-purpose quando a tarefa requer exploração e modificação, raciocínio complexo para interpretar resultados, ou múltiplas etapas dependentes.
  </Tab>

  <Tab title="Other">
    Claude Code inclui agentes auxiliares adicionais para tarefas específicas. Estes são normalmente invocados automaticamente, então você não precisa usá-los diretamente.

    | Agent             | Model  | When Claude uses it                                                   |
    | :---------------- | :----- | :-------------------------------------------------------------------- |
    | statusline-setup  | Sonnet | Quando você executa `/statusline` para configurar sua linha de status |
    | claude-code-guide | Haiku  | Quando você faz perguntas sobre recursos do Claude Code               |
  </Tab>
</Tabs>

Os subagentes integrados são registrados por padrão em sessões interativas. Para restringi-los:

* Para bloquear um tipo integrado específico, adicione-o a `permissions.deny` conforme mostrado em [Desabilitar subagentes específicos](#disable-specific-subagents).
* Para impedir que Claude delegue a qualquer subagente, negue a ferramenta `Agent` em si com [`permissions.deny`](https://code.claude.com/docs/pt/permissions#tool-specific-permission-rules).
* Para remover apenas os subagentes integrados `Explore` e `Plan`, defina [`CLAUDE_CODE_DISABLE_EXPLORE_PLAN_AGENTS=1`](https://code.claude.com/docs/pt/env-vars). Claude lê e explora arquivos diretamente em vez de delegar para eles. Requer Claude Code v2.1.198 ou posterior.
* Em [modo não interativo](https://code.claude.com/docs/pt/headless) e no [Agent SDK](https://code.claude.com/docs/pt/agent-sdk/overview), defina [`CLAUDE_AGENT_SDK_DISABLE_BUILTIN_AGENTS=1`](https://code.claude.com/docs/pt/env-vars) para remover todos os tipos integrados e fornecer apenas os seus próprios.

Além desses subagentes integrados, você pode criar os seus próprios com prompts personalizados, restrições de ferramentas, modos de permissão, hooks e skills. As seções a seguir mostram como começar e personalizar subagentes.

<h2 id="quickstart-create-your-first-subagent">
  Quickstart: criar seu primeiro subagente
</h2>

Subagentes são arquivos Markdown com frontmatter YAML. Para criar um, peça ao Claude para escrevê-lo para você, ou [escreva o arquivo você mesmo](#write-subagent-files).

A partir da v2.1.198, o comando `/agents` não abre mais o assistente de criação interativo; executá-lo imprime um lembrete para pedir ao Claude ou editar `.claude/agents/` diretamente. Os arquivos de subagente, campos de frontmatter e os locais `.claude/agents/` e `~/.claude/agents/` permanecem inalterados; apenas o assistente de terminal foi removido.

Este passo a passo cria um subagente no nível do usuário que revisa código e sugere melhorias.

<Steps>
  <Step title="Peça ao Claude para criar o subagente">
    No Claude Code, descreva o subagente que você deseja e onde salvá-lo:

    ```text wrap theme={null}
    Create a personal code-improver subagent in ~/.claude/agents/ that scans
    files and suggests improvements for readability, performance, and best
    practices. It should explain each issue, show the current code, and
    provide an improved version. Make it read-only and have it use Sonnet.
    ```

    Claude escreve o arquivo com um `name`, uma `description`, uma lista de `tools`, um `model` e um prompt de sistema.
  </Step>

  <Step title="Revise o arquivo">
    Abra `~/.claude/agents/code-improver.md` e confirme que o frontmatter corresponde ao que você pediu. O resultado se parece com isto:

    ```markdown theme={null}
    ---
    name: code-improver
    description: Scans files and suggests improvements for readability, performance, and best practices. Use after writing or modifying code.
    tools: Read, Grep, Glob
    model: sonnet
    ---

    You are a code improvement specialist. For each issue you find, explain
    the problem, show the current code, and provide an improved version.
    ```

    Como o arquivo está em `~/.claude/agents/`, o subagente está disponível em todos os projetos em sua máquina. Para limitá-lo a um projeto, mova-o para o diretório `.claude/agents/` desse projeto. [Escolha o escopo do subagente](#choose-the-subagent-scope) compara os dois.
  </Step>

  <Step title="Teste-o">
    Peça ao Claude para delegar para o novo subagente:

    ```text wrap theme={null}
    Use the code-improver agent to suggest improvements in this project
    ```

    Claude delega para seu novo subagente, que verifica a base de código e retorna sugestões de melhoria.

    Se Claude não conseguir encontrar o novo subagente, reinicie o Claude Code e tente novamente. Isso acontece apenas quando `~/.claude/agents/` não existia antes da sessão começar, porque uma sessão em execução não detecta um diretório `agents` recém-criado.
  </Step>
</Steps>

Agora você tem um subagente que pode usar em qualquer projeto em sua máquina para analisar bases de código e sugerir melhorias.

Você também pode escrever arquivos de subagente manualmente, defini-los via flags CLI ou distribuí-los através de plugins. As seções a seguir cobrem todas as opções de configuração.

<Note>
  No Claude Code v2.1.197 e anterior, `/agents` abre um assistente interativo com uma aba **Running** que lista subagentes ativos e uma aba **Library** para criá-los, editá-los e deletá-los.&#x20;
</Note>

<h2 id="configure-subagents">
  Configurar subagentes
</h2>

A localização do arquivo de um subagente determina quem tem acesso a ele, e seu frontmatter determina o que ele pode fazer. Esta seção aborda onde os arquivos de subagente residem e cada campo que eles suportam.

<h3 id="choose-the-subagent-scope">
  Escolher o escopo do subagente
</h3>

Armazene arquivos de subagente em locais diferentes dependendo do escopo. Quando múltiplos subagentes compartilham o mesmo nome, Claude Code usa o que está no local de prioridade mais alta.

| Location                     | Scope                   | Priority    | How to create                                 |
| :--------------------------- | :---------------------- | :---------- | :-------------------------------------------- |
| Managed settings             | Organization-wide       | 1 (highest) | Deployed via [managed settings](https://code.claude.com/docs/pt/settings) |
| `--agents` CLI flag          | Current session         | 2           | Pass JSON when launching Claude Code          |
| `.claude/agents/`            | Current project         | 3           | Ask Claude, or create the file manually       |
| `~/.claude/agents/`          | All your projects       | 4           | Ask Claude, or create the file manually       |
| Plugin's `agents/` directory | Where plugin is enabled | 5 (lowest)  | Installed with [plugins](https://code.claude.com/docs/pt/plugins)         |

**Subagentes de projeto** (`.claude/agents/`) são ideais para subagentes específicos de uma base de código. Verifique-os no controle de versão para que sua equipe possa usá-los e melhorá-los colaborativamente.

Subagentes de projeto são descobertos caminhando para cima a partir do diretório de trabalho atual, portanto cada `.claude/agents/` entre lá e a raiz do repositório é verificado. A partir da v2.1.178, quando mais de um desses diretórios aninhados define o mesmo `name`, Claude Code usa a definição mais próxima do diretório de trabalho.

Diretórios adicionados com `--add-dir` também são verificados: uma pasta `.claude/agents/` dentro de um diretório adicionado carrega junto com subagentes de projeto. Veja [Diretórios adicionais](https://code.claude.com/docs/pt/permissions#additional-directories-grant-file-access-not-configuration) para quais outros tipos de configuração carregam de `--add-dir`. Para compartilhar subagentes entre projetos sem `--add-dir`, use `~/.claude/agents/` ou um [plugin](https://code.claude.com/docs/pt/plugins).

**Subagentes de usuário** (`~/.claude/agents/`) são subagentes pessoais disponíveis em todos os seus projetos.

Claude Code verifica `.claude/agents/` e `~/.claude/agents/` recursivamente, para que você possa organizar definições em subpastas como `agents/review/` ou `agents/research/`. O caminho do subdiretório não afeta como um subagente é identificado ou invocado, porque a identidade vem apenas do campo `name` do frontmatter.

Mantenha valores de `name` únicos em toda a árvore: se dois arquivos sob o mesmo diretório `.claude/agents/`, incluindo suas subpastas, declaram o mesmo nome, Claude Code carrega apenas um deles, escolhido pela ordem de leitura do sistema de arquivos em vez de uma precedência documentada. Entre diretórios de projeto aninhados, a definição mais próxima do diretório de trabalho vence, conforme descrito acima. O verificador de configuração [`/doctor`](https://code.claude.com/docs/pt/commands#all-commands) relata arquivos no mesmo diretório que compartilham um nome e propõe renomear ou remover todos exceto um. Antes da v2.1.205, `/doctor` abria uma tela de diagnósticos que listava duplicatas e mostrava qual definição estava ativa.

Diretórios `agents/` de plugin também são verificados recursivamente. Diferentemente dos escopos de projeto e usuário, uma subpasta dentro do diretório `agents/` de um plugin se torna parte do [identificador com escopo](#invoke-subagents-explicitly): um arquivo em `agents/review/security.md` no plugin `my-plugin` se registra como `my-plugin:review:security`.

**Subagentes definidos por CLI** são passados como JSON ao iniciar Claude Code. Eles existem apenas para essa sessão e não são salvos em disco, tornando-os úteis para testes rápidos ou scripts de automação. Você pode definir múltiplos subagentes em uma única chamada `--agents`:

<Tabs>
  <Tab title="macOS, Linux, WSL">
    ```bash theme={null}
    claude --agents '{
      "code-reviewer": {
        "description": "Expert code reviewer. Use proactively after code changes.",
        "prompt": "You are a senior code reviewer. Focus on code quality, security, and best practices.",
        "tools": ["Read", "Grep", "Glob", "Bash"],
        "model": "sonnet"
      },
      "debugger": {
        "description": "Debugging specialist for errors and test failures.",
        "prompt": "You are an expert debugger. Analyze errors, identify root causes, and provide fixes."
      }
    }'
    ```
  </Tab>

  <Tab title="Windows PowerShell">
    ```powershell theme={null}
    claude --agents @'
    {
      "code-reviewer": {
        "description": "Expert code reviewer. Use proactively after code changes.",
        "prompt": "You are a senior code reviewer. Focus on code quality, security, and best practices.",
        "tools": ["Read", "Grep", "Glob", "Bash"],
        "model": "sonnet"
      },
      "debugger": {
        "description": "Debugging specialist for errors and test failures.",
        "prompt": "You are an expert debugger. Analyze errors, identify root causes, and provide fixes."
      }
    }
    '@
    ```
  </Tab>
</Tabs>

O flag `--agents` aceita JSON com os mesmos campos de [frontmatter](#supported-frontmatter-fields) que subagentes baseados em arquivo: `description`, `prompt`, `tools`, `disallowedTools`, `model`, `permissionMode`, `mcpServers`, `hooks`, `maxTurns`, `skills`, `initialPrompt`, `memory`, `effort`, `background`, `isolation` e `color`. Use `prompt` para o prompt de sistema, equivalente ao corpo markdown em subagentes baseados em arquivo.

**Subagentes gerenciados** são implantados por administradores da organização. Coloque arquivos markdown em `.claude/agents/` dentro do [diretório de configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files), usando o mesmo formato de frontmatter que subagentes de projeto e usuário. Definições gerenciadas têm precedência sobre subagentes de projeto e usuário com o mesmo nome.

**Subagentes de plugin** vêm de [plugins](https://code.claude.com/docs/pt/plugins) que você instalou. Eles carregam junto com seus subagentes personalizados e aparecem na digitação de @-menção sob seu nome com escopo. Veja a [referência de componentes de plugin](https://code.claude.com/docs/pt/plugins-reference#agents) para detalhes sobre como criar subagentes de plugin.

<Note>
  Por razões de segurança, subagentes de plugin não suportam os campos de frontmatter `hooks`, `mcpServers` ou `permissionMode`. Estes campos são ignorados ao carregar agentes de um plugin. Se você precisar deles, copie o arquivo do agente para `.claude/agents/` ou `~/.claude/agents/`. Você também pode adicionar regras a [`permissions.allow`](https://code.claude.com/docs/pt/settings#permission-settings) em `settings.json` ou `settings.local.json`, mas estas regras se aplicam a toda a sessão, não apenas ao subagente do plugin.
</Note>

Definições de subagente de qualquer um desses escopos também estão disponíveis para [equipes de agentes](https://code.claude.com/docs/pt/agent-teams#use-subagent-definitions-for-teammates): ao gerar um colega de trabalho, você pode referenciar um tipo de subagente e o colega de trabalho usa suas `tools` e `model`, com o corpo da definição anexado ao prompt de sistema do colega de trabalho como instruções adicionais. Veja [equipes de agentes](https://code.claude.com/docs/pt/agent-teams#use-subagent-definitions-for-teammates) para quais campos de frontmatter se aplicam nesse caminho.

<h3 id="write-subagent-files">
  Escrever arquivos de subagente
</h3>

Arquivos de subagente usam frontmatter YAML para configuração, seguido pelo prompt de sistema em Markdown:

<Note>
  Claude Code observa `~/.claude/agents/` e `.claude/agents/`. Quando você adiciona ou edita um arquivo de subagente no disco, ou pede a Claude para escrever um para você, Claude Code detecta a alteração em alguns segundos e a próxima delegação usa a definição atualizada, sem necessidade de reinicialização.

  Dois casos ainda precisam de uma reinicialização:

  * O observador cobre apenas diretórios que existiam quando a sessão começou, portanto após criar o primeiro arquivo de agente de um escopo em um novo diretório `agents`, reinicie para carregá-lo.
  * Sessões iniciadas com `--disable-slash-commands` não observam esses diretórios.
</Note>

```markdown theme={null}
---
name: code-reviewer
description: Reviews code for quality and best practices
tools: Read, Glob, Grep
model: sonnet
---

You are a code reviewer. When invoked, analyze the code and provide
specific, actionable feedback on quality, security, and best practices.
```

O frontmatter define os metadados e configuração do subagente. O corpo se torna o prompt de sistema que guia o comportamento do subagente. Subagentes recebem apenas este prompt de sistema mais detalhes básicos de ambiente como diretório de trabalho, não o prompt de sistema completo do Claude Code.

Em [modo não interativo](https://code.claude.com/docs/pt/headless), o flag [`--append-subagent-system-prompt`](https://code.claude.com/docs/pt/cli-reference#cli-flags) anexa o texto que você fornece ao final do prompt de sistema de cada subagente, incluindo subagentes aninhados. Requer Claude Code v2.1.205 ou posterior.

Um subagente começa no diretório de trabalho atual da conversa principal. Dentro de um subagente, comandos `cd` não persistem entre chamadas de ferramentas Bash ou PowerShell e não afetam o diretório de trabalho da conversa principal. Para dar ao subagente uma cópia isolada do repositório em vez disso, defina [`isolation: worktree`](#supported-frontmatter-fields).

Um subagente com `isolation: worktree` executa seus comandos Bash e PowerShell dentro de seu worktree. Um comando cujo diretório de trabalho se resolve para seu checkout principal, por exemplo porque o diretório worktree foi removido enquanto o subagente estava em execução, falha com um erro. Antes da v2.1.203, tal comando poderia ser executado no checkout principal.

<h4 id="supported-frontmatter-fields">
  Campos de frontmatter suportados
</h4>

Os seguintes campos podem ser usados no frontmatter YAML. Apenas `name` e `description` são obrigatórios.

| Field             | Required | Description                                                                                                                                                                                                                                                                                                                                                                     |
| :---------------- | :------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `name`            | Yes      | Identificador único usando letras minúsculas e hífens. [Hooks](https://code.claude.com/docs/pt/hooks#subagentstart) recebem este valor como `agent_type`. O nome do arquivo não precisa corresponder                                                                                                                                                                                                        |
| `description`     | Yes      | Quando Claude deve delegar para este subagente                                                                                                                                                                                                                                                                                                                                  |
| `tools`           | No       | [Ferramentas](#available-tools) que o subagente pode usar. Herda todas as ferramentas se omitido. Se nenhuma entrada na lista se resolver para uma ferramenta, o subagente falha ao iniciar com um erro nomeando as entradas. Para pré-carregar Skills no contexto, use o campo `skills` em vez de listar `Skill` aqui                                                          |
| `disallowedTools` | No       | Ferramentas a negar, removidas da lista herdada ou especificada                                                                                                                                                                                                                                                                                                                 |
| `model`           | No       | [Modelo](#choose-a-model) a usar: `sonnet`, `opus`, `haiku`, `fable`, um ID de modelo completo (por exemplo, `claude-opus-4-8`), ou `inherit`. Padrão: `inherit`                                                                                                                                                                                                                |
| `permissionMode`  | No       | [Modo de permissão](#permission-modes): `default`, `acceptEdits`, `auto`, `dontAsk`, `bypassPermissions`, `plan`, ou `manual` como um alias para `default`. O alias `manual` requer Claude Code v2.1.200 ou posterior. Ignorado para [subagentes de plugin](#choose-the-subagent-scope)                                                                                         |
| `maxTurns`        | No       | Número máximo de turnos de agente antes do subagente parar                                                                                                                                                                                                                                                                                                                      |
| `skills`          | No       | [Skills](https://code.claude.com/docs/pt/skills) a pré-carregar no contexto do subagente na inicialização. O conteúdo completo da skill é injetado, não apenas a descrição. Subagentes ainda podem invocar skills de projeto, usuário e plugin não listadas através da ferramenta Skill                                                                                                                     |
| `mcpServers`      | No       | [MCP servers](https://code.claude.com/docs/pt/mcp) disponíveis para este subagente. Cada entrada é um nome de servidor referenciando um servidor já configurado (por exemplo, `"slack"`) ou uma definição inline com o nome do servidor como chave e uma [configuração completa de MCP server](https://code.claude.com/docs/pt/mcp#installing-mcp-servers) como valor. Ignorado para [subagentes de plugin](#choose-the-subagent-scope) |
| `hooks`           | No       | [Lifecycle hooks](#define-hooks-for-subagents) com escopo para este subagente. Ignorado para [subagentes de plugin](#choose-the-subagent-scope)                                                                                                                                                                                                                                 |
| `memory`          | No       | [Escopo de memória persistente](#enable-persistent-memory): `user`, `project`, ou `local`. Habilita aprendizado entre sessões                                                                                                                                                                                                                                                   |
| `background`      | No       | Defina como `true` para sempre executar este subagente como uma [tarefa em background](#run-subagents-in-foreground-or-background), mesmo quando Claude precisa de seu resultado imediatamente. Quando não definido, Claude escolhe, e a partir da v2.1.198 ele executa subagentes em background por padrão                                                                     |
| `effort`          | No       | Nível de esforço quando este subagente está ativo. Sobrescreve o nível de esforço da sessão. Padrão: herda da sessão. Opções: `low`, `medium`, `high`, `xhigh`, `max`; os níveis disponíveis dependem do modelo                                                                                                                                                                 |
| `isolation`       | No       | Defina como `worktree` para executar o subagente em um [git worktree](https://code.claude.com/docs/pt/worktrees) temporário, dando-lhe uma cópia isolada do repositório ramificada por padrão a partir de sua [branch padrão](https://code.claude.com/docs/pt/worktrees#choose-the-base-branch) em vez do `HEAD` da sessão pai. O worktree é automaticamente limpo se o subagente não fizer alterações                                  |
| `color`           | No       | Cor de exibição para o subagente na lista de tarefas e transcrição. Aceita `red`, `blue`, `green`, `yellow`, `purple`, `orange`, `pink`, ou `cyan`                                                                                                                                                                                                                              |
| `initialPrompt`   | No       | Auto-enviado como o primeiro turno do usuário quando este agente é executado como o agente da sessão principal (via `--agent` ou a configuração `agent`). [Comandos](https://code.claude.com/docs/pt/commands) e [skills](https://code.claude.com/docs/pt/skills) são processados. Preposto a qualquer prompt fornecido pelo usuário                                                                                                    |

<h3 id="choose-a-model">
  Escolher um modelo
</h3>

O campo `model` controla qual [modelo de IA](https://code.claude.com/docs/pt/model-config) o subagente usa:

* **Alias de modelo**: Use um dos aliases disponíveis: `sonnet`, `opus`, `haiku`, ou `fable`
* **ID de modelo completo**: Use um ID de modelo completo como `claude-opus-4-8` ou `claude-sonnet-5`. Aceita os mesmos valores que o flag `--model`
* **inherit**: Use o mesmo modelo que a conversa principal
* **Omitido**: Se não especificado, padrão é `inherit` (usa o mesmo modelo que a conversa principal)

Quando Claude invoca um subagente, ele também pode passar um parâmetro `model` para essa invocação específica. Claude Code resolve o modelo do subagente nesta ordem:

1. A variável de ambiente [`CLAUDE_CODE_SUBAGENT_MODEL`](https://code.claude.com/docs/pt/model-config#environment-variables), quando definida para um alias de modelo ou ID de modelo
2. O parâmetro `model` por invocação
3. O frontmatter `model` da definição do subagente
4. O modelo da conversa principal

A partir da v2.1.196, definir `CLAUDE_CODE_SUBAGENT_MODEL` para `inherit` é o mesmo que deixá-lo indefinido: a resolução continua com o parâmetro `model` por invocação, depois o frontmatter. Em versões anteriores, `inherit` forçava subagentes para o modelo da conversa principal e ignorava ambas essas fontes.

Claude Code verifica o valor da variável de ambiente, parâmetro por invocação e valores de frontmatter contra a lista de permissões [`availableModels`](https://code.claude.com/docs/pt/model-config#restrict-model-selection) da sua organização. Um valor que se resolve para um modelo excluído é ignorado e o subagente é executado no modelo herdado em vez disso.

A partir da v2.1.198, subagentes também herdam a configuração de [pensamento estendido](https://code.claude.com/docs/pt/model-config#extended-thinking) da conversa principal: se o pensamento está ativado em sua sessão, está ativado para o subagente, e se está desativado, permanece desativado. Não há configuração de pensamento por subagente. Antes da v2.1.198, subagentes eram executados com pensamento estendido desabilitado independentemente da configuração da conversa principal.

<h3 id="control-subagent-capabilities">
  Controlar capacidades do subagente
</h3>

Você pode controlar o que subagentes podem fazer através de acesso a ferramentas, modos de permissão e regras condicionais.

<h4 id="available-tools">
  Ferramentas disponíveis
</h4>

Subagentes herdam as [ferramentas internas](https://code.claude.com/docs/pt/tools-reference) e ferramentas MCP disponíveis na conversa principal por padrão. As seguintes ferramentas dependem da interface ou estado de sessão da conversa principal e não estão disponíveis para subagentes, mesmo quando listadas no campo `tools`:

* `AskUserQuestion`
* `EnterPlanMode`
* `ExitPlanMode`, a menos que o [`permissionMode`](#permission-modes) do subagente seja `plan`
* `ScheduleWakeup`
* `WaitForMcpServers`

Para restringir ferramentas, use o campo `tools` como uma lista de permissões ou o campo `disallowedTools` como uma lista de negação. Este exemplo usa `tools` para permitir exclusivamente Read, Grep, Glob e Bash. O subagente não pode editar arquivos, escrever arquivos ou usar qualquer ferramenta MCP:

```yaml theme={null}
---
name: safe-researcher
description: Research agent with restricted capabilities
tools: Read, Grep, Glob, Bash
---
```

Este exemplo usa `disallowedTools` para herdar todas as ferramentas da conversa principal exceto Write e Edit. O subagente mantém Bash, ferramentas MCP e tudo mais:

```yaml theme={null}
---
name: no-writes
description: Inherits every tool except file writes
disallowedTools: Write, Edit
---
```

Se ambos forem definidos, `disallowedTools` é aplicado primeiro, depois `tools` é resolvido contra o pool restante. Uma ferramenta listada em ambos é removida.

Quando nada na lista `tools` se resolve para uma ferramenta, por exemplo porque cada entrada está com erro de digitação ou nomeia uma ferramenta que não está disponível para subagentes, Claude Code recusa iniciar o subagente e a ferramenta Agent retorna um erro nomeando as entradas não resolvidas. Antes da v2.1.208, esse subagente era iniciado sem ferramentas e poderia retornar um resultado vazio ou confuso.

Ambos os campos aceitam padrões de nível de servidor MCP além de nomes de ferramentas exatos: `mcp__<server>` ou `mcp__<server>__*` concede ou remove todas as ferramentas do servidor nomeado. Em `disallowedTools`, `mcp__*` também remove todas as ferramentas MCP de qualquer servidor. Este exemplo remove todas as ferramentas do servidor MCP `github` enquanto mantém ferramentas de outros servidores e todas as ferramentas integradas:

```yaml theme={null}
---
name: local-only
description: Inherits every tool except those from the github MCP server
disallowedTools: mcp__github
---
```

<h4 id="restrict-which-subagents-can-be-spawned">
  Restringir quais subagentes podem ser gerados
</h4>

Quando um agente é executado como thread principal com `claude --agent`, ele pode gerar subagentes usando a ferramenta Agent. Para restringir quais tipos de subagente ele pode gerar, use a sintaxe `Agent(agent_type)` no campo `tools`.

<Note>Na versão 2.1.63, a ferramenta Task foi renomeada para Agent. Referências existentes de `Task(...)` em configurações e definições de agente ainda funcionam como aliases.</Note>

```yaml theme={null}
---
name: coordinator
description: Coordinates work across specialized agents
tools: Agent(worker, researcher), Read, Bash
---
```

Esta é uma lista de permissões: apenas os subagentes `worker` e `researcher` podem ser gerados. Se o agente tentar gerar qualquer outro tipo, a solicitação falha e o agente vê apenas os tipos permitidos em seu prompt. Para bloquear agentes específicos enquanto permite todos os outros, use [`permissions.deny`](#disable-specific-subagents) em vez disso.

Para permitir gerar qualquer subagente sem restrições, use `Agent` sem parênteses:

```yaml theme={null}
tools: Agent, Read, Bash
```

Se `Agent` for omitido da lista `tools` inteiramente, o agente não pode gerar nenhum subagente.

A sintaxe de lista de permissões `Agent(agent_type)` se aplica apenas a um agente executado como thread principal com `claude --agent`. Em uma definição de subagente, listar `Agent` em `tools` permite que esse subagente [gere subagentes aninhados](#spawn-nested-subagents), mas qualquer lista de tipo dentro dos parênteses é ignorada.

<h4 id="scope-mcp-servers-to-a-subagent">
  Escopo de MCP servers para um subagente
</h4>

Use o campo `mcpServers` para dar a um subagente acesso a [MCP](https://code.claude.com/docs/pt/mcp) servers que não estão disponíveis na conversa principal. Servidores inline definidos aqui são conectados quando o subagente inicia e desconectados quando termina. Referências de string compartilham a conexão da sessão pai.

<Note>
  O campo `mcpServers` se aplica em ambos os contextos onde um arquivo de agente pode ser executado:

  * Como um subagente, gerado através da ferramenta Agent ou uma @-menção
  * Como a sessão principal, iniciada com [`--agent`](#invoke-subagents-explicitly) ou a configuração `agent`

  Quando o agente é a sessão principal, definições de servidor inline se conectam na inicialização junto com servidores de [`.mcp.json`](https://code.claude.com/docs/pt/mcp) e arquivos de configurações.
</Note>

Cada entrada na lista é uma definição de servidor inline ou uma string referenciando um MCP server já configurado em sua sessão:

```yaml theme={null}
---
name: browser-tester
description: Tests features in a real browser using Playwright
mcpServers:
  # Inline definition: scoped to this subagent only
  - playwright:
      type: stdio
      command: npx
      args: ["-y", "@playwright/mcp@latest"]
  # Reference by name: reuses an already-configured server
  - github
---

Use the Playwright tools to navigate, screenshot, and interact with pages.
```

Definições inline usam o mesmo schema que entradas de servidor `.mcp.json`, com chave pelo nome do servidor, e suportam os tipos `stdio`, `http`, `sse` e `ws`.

Para manter um MCP server fora da conversa principal inteiramente e evitar que suas descrições de ferramentas consumam contexto lá, defina-o inline aqui em vez de em `.mcp.json`. O subagente obtém as ferramentas; a conversa pai não.

A partir da v2.1.153, as restrições de MCP que se aplicam à sessão principal também cobrem servidores declarados no frontmatter do subagente:

* [`--strict-mcp-config`](https://code.claude.com/docs/pt/cli-reference) e [`--bare`](https://code.claude.com/docs/pt/cli-reference)
* [Configuração de MCP gerenciada pela empresa](https://code.claude.com/docs/pt/managed-mcp)
* [Políticas `allowedMcpServers` e `deniedMcpServers`](https://code.claude.com/docs/pt/managed-mcp#policy-based-control-with-allowlists-and-denylists)

Quando um destes bloqueia um servidor, Claude Code o ignora e mostra um aviso nomeando os servidores bloqueados.

Restrições de configurações gerenciadas se aplicam a cada subagente independentemente de como é definido. `--strict-mcp-config` não filtra servidores que você passa inline via `--agents` ou a opção `agents` do SDK, já que esses são entrada explícita do chamador.

<h4 id="permission-modes">
  Modos de permissão
</h4>

O campo `permissionMode` controla como o subagente lida com prompts de permissão. Subagentes herdam o contexto de permissão da conversa principal e podem sobrescrever o modo, exceto quando o modo pai tem precedência conforme descrito abaixo.

| Mode                | Behavior                                                                                                                                                                                                                                                                                                                                                              |
| :------------------ | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `default`           | Verificação de permissão padrão com prompts                                                                                                                                                                                                                                                                                                                           |
| `acceptEdits`       | Auto-aceitar edições de arquivo e comandos comuns do sistema de arquivos para caminhos no diretório de trabalho ou `additionalDirectories`                                                                                                                                                                                                                            |
| `auto`              | [Auto mode](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode): um classificador de IA avalia cada chamada de ferramenta                                                                                                                                                                                                                                          |
| `dontAsk`           | Auto-negar prompts de permissão. Ferramentas explicitamente permitidas ainda funcionam; `AskUserQuestion`, ferramentas de conector [sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools), e ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) são negadas mesmo se você as permitiu |
| `bypassPermissions` | Pular prompts de permissão                                                                                                                                                                                                                                                                                                                                            |
| `plan`              | Plan mode (exploração somente leitura)                                                                                                                                                                                                                                                                                                                                |

<Warning>
  Use `bypassPermissions` com cuidado. Ele pula prompts de permissão, permitindo que o subagente execute operações sem aprovação, incluindo escritas em `.git`, `.config/git`, `.claude`, `.vscode`, `.idea`, `.husky`, `.cargo`, `.devcontainer`, `.yarn` e `.mvn`.

  Regras [`ask`](https://code.claude.com/docs/pt/permissions#manage-permissions) explícitas, ferramentas de conector [sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools), ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool), e remoções de diretório raiz e home como `rm -rf /` ainda solicitam. Veja [modos de permissão](https://code.claude.com/docs/pt/permission-modes#skip-all-checks-with-bypasspermissions-mode) para detalhes.
</Warning>

Se o pai usar `bypassPermissions` ou `acceptEdits`, isso tem precedência e não pode ser sobrescrito. Se o pai usar [auto mode](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode), o subagente herda auto mode e qualquer `permissionMode` em seu frontmatter é ignorado: o classificador avalia as chamadas de ferramentas do subagente com as mesmas regras de bloqueio e permissão que a sessão pai.

<h4 id="preload-skills-into-subagents">
  Pré-carregar skills em subagentes
</h4>

Use o campo `skills` para injetar conteúdo de skill no contexto de um subagente na inicialização. Isso dá ao subagente conhecimento de domínio sem exigir que ele descubra e carregue skills durante a execução.

```yaml theme={null}
---
name: api-developer
description: Implement API endpoints following team conventions
skills:
  - api-conventions
  - error-handling-patterns
---

Implement API endpoints. Follow the conventions and patterns from the preloaded skills.
```

O conteúdo completo de cada skill listada é injetado no contexto do subagente na inicialização. Este campo controla quais skills são pré-carregadas, não quais skills o subagente pode acessar: sem ele, o subagente ainda pode descobrir e invocar skills de projeto, usuário e plugin através da ferramenta Skill durante a execução. Para impedir que um subagente invoque skills inteiramente, omita `Skill` da lista [`tools`](#available-tools) ou adicione-o a `disallowedTools`.

Você não pode pré-carregar skills que definem [`disable-model-invocation: true`](https://code.claude.com/docs/pt/skills#control-who-invokes-a-skill), já que pré-carregar extrai do mesmo conjunto de skills que Claude pode invocar. Se uma skill listada estiver faltando ou desabilitada, Claude Code a ignora e registra um aviso no log de debug.

<Note>
  Isto é o inverso de [executar uma skill em um subagente](https://code.claude.com/docs/pt/skills#run-skills-in-a-subagent). Com `skills` em um subagente, o subagente controla o prompt de sistema e carrega conteúdo de skill. Com `context: fork` em uma skill, o conteúdo de skill é injetado no agente que você especificar. Ambos usam o mesmo sistema subjacente.
</Note>

<h4 id="enable-persistent-memory">
  Habilitar memória persistente
</h4>

O campo `memory` dá ao subagente um diretório persistente que sobrevive entre conversas. O subagente usa este diretório para construir conhecimento ao longo do tempo, como padrões de base de código, insights de debugging e decisões arquiteturais.

```yaml theme={null}
---
name: code-reviewer
description: Reviews code for quality and best practices
memory: user
---

You are a code reviewer. As you review code, update your agent memory with
patterns, conventions, and recurring issues you discover.
```

Escolha um escopo baseado em quão amplamente a memória deve se aplicar:

| Scope     | Location                                      | Use when                                                                                              |
| :-------- | :-------------------------------------------- | :---------------------------------------------------------------------------------------------------- |
| `user`    | `~/.claude/agent-memory/<name-of-agent>/`     | o subagente deve lembrar aprendizados entre todos os projetos                                         |
| `project` | `.claude/agent-memory/<name-of-agent>/`       | o conhecimento do subagente é específico do projeto e compartilhável via controle de versão           |
| `local`   | `.claude/agent-memory-local/<name-of-agent>/` | o conhecimento do subagente é específico do projeto mas não deve ser verificado no controle de versão |

Quando memória está habilitada:

* O prompt de sistema do subagente inclui instruções para ler e escrever no diretório de memória.
* O prompt de sistema do subagente também inclui as primeiras 200 linhas ou 25KB de `MEMORY.md` no diretório de memória, o que for menor, com instruções para curar `MEMORY.md` se exceder esse limite.
* Ferramentas Read, Write e Edit são automaticamente habilitadas para que o subagente possa gerenciar seus arquivos de memória.

<h5 id="persistent-memory-tips">
  Dicas de memória persistente
</h5>

* `project` é o escopo padrão recomendado. Ele torna o conhecimento do subagente compartilhável via controle de versão.
* Peça ao subagente para consultar sua memória antes de começar o trabalho: "Review this PR, and check your memory for patterns you've seen before."
* Peça ao subagente para atualizar sua memória após completar uma tarefa: "Now that you're done, save what you learned to your memory." Ao longo do tempo, isso constrói uma base de conhecimento que torna o subagente mais eficaz.
* Inclua instruções de memória diretamente no arquivo markdown do subagente para que ele mantenha proativamente sua própria base de conhecimento:

  ```markdown theme={null}
  Update your agent memory as you discover codepaths, patterns, library
  locations, and key architectural decisions. This builds up institutional
  knowledge across conversations. Write concise notes about what you found
  and where.
  ```

<h4 id="conditional-rules-with-hooks">
  Regras condicionais com hooks
</h4>

Para controle mais dinâmico sobre uso de ferramentas, use hooks `PreToolUse` para validar operações antes de serem executadas. Isso é útil quando você precisa permitir algumas operações de uma ferramenta enquanto bloqueia outras.

Este exemplo cria um subagente que apenas permite consultas de banco de dados somente leitura. O hook `PreToolUse` executa o script especificado em `command` antes de cada comando Bash ser executado:

```yaml theme={null}
---
name: db-reader
description: Execute read-only database queries
tools: Bash
hooks:
  PreToolUse:
    - matcher: "Bash"
      hooks:
        - type: command
          command: "./scripts/validate-readonly-query.sh"
---
```

Claude Code [passa entrada de hook como JSON](https://code.claude.com/docs/pt/hooks#pretooluse-input) via stdin para comandos de hook. O script de validação lê este JSON, extrai o comando Bash e [sai com código 2](https://code.claude.com/docs/pt/hooks#exit-code-2-behavior-per-event) para bloquear operações de escrita:

```bash theme={null}
#!/bin/bash
# ./scripts/validate-readonly-query.sh

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# Block SQL write operations (case-insensitive)
if echo "$COMMAND" | grep -iE '\b(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE)\b' > /dev/null; then
  echo "Blocked: Only SELECT queries are allowed" >&2
  exit 2
fi

exit 0
```

Veja [Hook input](https://code.claude.com/docs/pt/hooks#pretooluse-input) para o schema de entrada completo e [exit codes](https://code.claude.com/docs/pt/hooks#exit-code-output) para como códigos de saída afetam o comportamento. No Windows, escreva scripts de hook em PowerShell e adicione `shell: powershell` à entrada de hook conforme mostrado em [executando hooks em PowerShell](https://code.claude.com/docs/pt/hooks#windows-powershell-tool).

<h4 id="disable-specific-subagents">
  Desabilitar subagentes específicos
</h4>

Você pode impedir que Claude use subagentes específicos adicionando-os ao array `deny` em suas [configurações](https://code.claude.com/docs/pt/settings#permission-settings). Use o formato `Agent(subagent-name)` onde `subagent-name` corresponde ao campo name do subagente.

```json theme={null}
{
  "permissions": {
    "deny": ["Agent(Explore)", "Agent(my-custom-agent)"]
  }
}
```

Isso funciona para subagentes integrados e personalizados. Você também pode usar o flag CLI `--disallowedTools`:

```bash theme={null}
claude --disallowedTools "Agent(Explore)"
```

Veja [documentação de Permissões](https://code.claude.com/docs/pt/permissions#tool-specific-permission-rules) para mais detalhes sobre regras de permissão.

<h3 id="define-hooks-for-subagents">
  Definir hooks para subagentes
</h3>

Subagentes podem definir [hooks](https://code.claude.com/docs/pt/hooks) que são executados durante o ciclo de vida do subagente. Existem duas formas de configurar hooks:

* **No frontmatter do subagente**: defina hooks que são executados apenas enquanto esse subagente específico está ativo
* **Em `settings.json`**: defina hooks que são executados na sessão principal quando subagentes iniciam ou param

<h4 id="hooks-in-subagent-frontmatter">
  Hooks no frontmatter do subagente
</h4>

Defina hooks diretamente no arquivo markdown do subagente. Estes hooks são executados apenas enquanto esse subagente específico está ativo e são limpos quando termina.

<Note>
  Hooks de frontmatter disparam quando o agente é gerado como um subagente através da ferramenta Agent ou uma @-menção, e quando o agente é executado como a sessão principal via [`--agent`](#invoke-subagents-explicitly) ou a configuração `agent`. No caso de sessão principal, eles são executados junto com qualquer hook definido em [`settings.json`](https://code.claude.com/docs/pt/hooks).
</Note>

Todos os [eventos de hook](https://code.claude.com/docs/pt/hooks#hook-events) são suportados. Os eventos mais comuns para subagentes são:

| Event         | Matcher input      | When it fires                                                                    |
| :------------ | :----------------- | :------------------------------------------------------------------------------- |
| `PreToolUse`  | Nome da ferramenta | Antes do subagente usar uma ferramenta                                           |
| `PostToolUse` | Nome da ferramenta | Depois do subagente usar uma ferramenta                                          |
| `Stop`        | (nenhum)           | Quando o subagente termina (convertido para `SubagentStop` em tempo de execução) |

Este exemplo valida comandos Bash com o hook `PreToolUse` e executa um linter após edições de arquivo com `PostToolUse`:

```yaml theme={null}
---
name: code-reviewer
description: Review code changes with automatic linting
hooks:
  PreToolUse:
    - matcher: "Bash"
      hooks:
        - type: command
          command: "./scripts/validate-command.sh $TOOL_INPUT"
  PostToolUse:
    - matcher: "Edit|Write"
      hooks:
        - type: command
          command: "./scripts/run-linter.sh"
---
```

Quando o agente é invocado como um subagente, hooks `Stop` no frontmatter são automaticamente convertidos para eventos `SubagentStop`.

<h4 id="project-level-hooks-for-subagent-events">
  Hooks no nível do projeto para eventos de subagente
</h4>

Configure hooks em `settings.json` que respondem a eventos de ciclo de vida de subagente na sessão principal.

| Event           | Matcher input          | When it fires                         |
| :-------------- | :--------------------- | :------------------------------------ |
| `SubagentStart` | Nome do tipo de agente | Quando um subagente começa a execução |
| `SubagentStop`  | Nome do tipo de agente | Quando um subagente completa          |

Ambos os eventos suportam matchers para direcionar tipos de agente específicos por nome. O valor do matcher é o `name` do frontmatter do agente para subagentes no nível de projeto e usuário, ou o identificador com escopo de plugin como `my-plugin:db-agent` para [subagentes de plugin](https://code.claude.com/docs/pt/plugins). Um nome com escopo contém dois-pontos, portanto é avaliado como uma [expressão regular sem âncora](https://code.claude.com/docs/pt/hooks#matcher-patterns); ancorá-lo com `^` e `$`, como em `^my-plugin:db-agent$`, para corresponder apenas a esse agente.

Este exemplo executa um script de configuração apenas quando o subagente `db-agent` inicia, e um script de limpeza quando qualquer subagente para:

```json theme={null}
{
  "hooks": {
    "SubagentStart": [
      {
        "matcher": "db-agent",
        "hooks": [
          { "type": "command", "command": "./scripts/setup-db-connection.sh" }
        ]
      }
    ],
    "SubagentStop": [
      {
        "hooks": [
          { "type": "command", "command": "./scripts/cleanup-db-connection.sh" }
        ]
      }
    ]
  }
}
```

Um matcher com hífens como `db-agent` corresponde exatamente no Claude Code v2.1.195 ou posterior. Em versões anteriores, é avaliado como uma expressão regular sem âncora e também dispara para qualquer tipo de agente que o contenha, como `prod-db-agent`; ancorá-lo como `^db-agent$` nessas versões.

Veja [Hooks](https://code.claude.com/docs/pt/hooks) para o formato de configuração de hook completo.

<h2 id="work-with-subagents">
  Trabalhar com subagentes
</h2>

<h3 id="understand-automatic-delegation">
  Entender delegação automática
</h3>

Claude delega automaticamente tarefas baseado na descrição da tarefa em sua solicitação, no campo `description` em configurações de subagente e no contexto atual. Para encorajar delegação proativa, inclua frases como "use proactively" no campo description do seu subagente.

<h3 id="invoke-subagents-explicitly">
  Invocar subagentes explicitamente
</h3>

Quando delegação automática não é suficiente, você pode solicitar um subagente você mesmo. Três padrões escalam de uma sugestão única para um padrão padrão em toda a sessão:

* **Linguagem natural**: nomeie o subagente em seu prompt; Claude decide se deve delegar
* **@-mention**: garante que o subagente seja executado para uma tarefa
* **Em toda a sessão**: toda a sessão usa o prompt de sistema, restrições de ferramentas e modelo do subagente via flag `--agent` ou configuração `agent`

Para linguagem natural, não há sintaxe especial. Nomeie o subagente e Claude normalmente delega:

```text wrap theme={null}
Use the test-runner subagent to fix failing tests
Have the code-reviewer subagent look at my recent changes
```

**@-mention o subagente.** Digite `@` e escolha o subagente do typeahead, da mesma forma que você @-menciona arquivos. Isso garante que esse subagente específico seja executado em vez de deixar a escolha para Claude:

```text wrap theme={null}
@"code-reviewer (agent)" look at the auth changes
```

Sua mensagem completa ainda vai para Claude, que escreve o prompt de tarefa do subagente baseado no que você pediu. O @-mention controla qual subagente Claude invoca, não qual prompt ele recebe.

Subagentes fornecidos por um [plugin](https://code.claude.com/docs/pt/plugins) habilitado aparecem no typeahead sob seu nome com escopo, como `my-plugin:code-reviewer` ou `my-plugin:review:security` quando o plugin [organiza agentes em subpastas](#choose-the-subagent-scope). Subagentes em background nomeados atualmente em execução na sessão também aparecem no typeahead, mostrando seu status ao lado do nome.

Você também pode digitar a menção manualmente sem usar o picker: `@agent-<name>` para subagentes locais, ou `@agent-` seguido pelo nome com escopo para subagentes de plugin, por exemplo `@agent-my-plugin:code-reviewer`.

**Execute toda a sessão como um subagente.** Passe [`--agent <name>`](https://code.claude.com/docs/pt/cli-reference) para iniciar uma sessão onde a thread principal em si assume o prompt de sistema, restrições de ferramentas e modelo do subagente:

```bash theme={null}
claude --agent code-reviewer
```

O prompt de sistema do subagente substitui completamente o prompt de sistema padrão do Claude Code, da mesma forma que [`--system-prompt`](https://code.claude.com/docs/pt/cli-reference) faz. Arquivos `CLAUDE.md` e memória de projeto ainda carregam através do fluxo de mensagem normal. O nome do agente aparece como `@<name>` no cabeçalho de inicialização para que você possa confirmar que está ativo.

Isso funciona com subagentes integrados e personalizados, e a escolha persiste quando você retoma a sessão.

Para um subagente fornecido por plugin, você pode passar apenas o nome do agente e Claude Code o encontrará:

```bash theme={null}
claude --agent security-reviewer
```

Se múltiplos plugins fornecem agentes com o mesmo nome, passe o nome com escopo para desambiguar:

```bash theme={null}
claude --agent my-plugin:security-reviewer
```

Se o plugin coloca o agente em uma subpasta de seu diretório `agents/`, inclua a subpasta no nome com escopo, por exemplo `claude --agent my-plugin:review:security`.

Para torná-lo o padrão para cada sessão em um projeto, defina `agent` em `.claude/settings.json`:

```json theme={null}
{
  "agent": "code-reviewer"
}
```

O flag CLI sobrescreve a configuração se ambos estiverem presentes.

<h3 id="run-subagents-in-foreground-or-background">
  Executar subagentes em foreground ou background
</h3>

Subagentes podem ser executados em foreground ou background:

* **Subagentes em foreground** bloqueiam a conversa principal até completar. Prompts de permissão são passados para você conforme surgem.
* **Subagentes em background** são executados concorrentemente enquanto você continua trabalhando. A partir da v2.1.186, quando um subagente em background atinge uma chamada de ferramenta que precisa de permissão, o prompt aparece em sua sessão principal e nomeia o subagente que está pedindo. Aprove para deixar o subagente continuar, ou pressione Esc para negar essa chamada de ferramenta sem parar o subagente. Antes da v2.1.186, subagentes em background auto-negavam qualquer chamada de ferramenta que teria solicitado.

A partir da v2.1.198, subagentes são executados em background por padrão. Claude executa um subagente em foreground quando precisa do resultado antes de continuar. O padrão muda onde um subagente é executado, não o que é permitido fazer: subagentes em background ainda exibem cada prompt de permissão em sua sessão principal. Antes da v2.1.198, Claude escolhia entre foreground e background baseado na tarefa.

Você também pode direcionar isso você mesmo:

* Peça a Claude para executar uma tarefa em background ou em foreground
* Pressione **Ctrl+B** para colocar uma tarefa em execução em background

Um subagente em background que completa fica listado em [`/tasks`](https://code.claude.com/docs/pt/commands), marcado como concluído e classificado abaixo do trabalho em execução, até que a sessão limpe sua lista de tarefas. Sua visualização de detalhes fica aberta quando o subagente termina. Subagentes que falham ou que você para deixam a lista. Antes da v2.1.208, um subagente concluído deixava a lista no momento em que terminava e sua visualização de detalhes fechava.

Para desabilitar toda a funcionalidade de tarefa em background, defina a variável de ambiente `CLAUDE_CODE_DISABLE_BACKGROUND_TASKS` para `1`. Veja [Variáveis de ambiente](https://code.claude.com/docs/pt/env-vars).

Quando [`CLAUDE_CODE_FORK_SUBAGENT`](#fork-the-current-conversation) está definido para `1`, cada spawn de subagente é executado em background e o campo frontmatter `background` não tem efeito, porque o modo fork remove o parâmetro `run_in_background` da ferramenta `Agent`. `CLAUDE_CODE_DISABLE_BACKGROUND_TASKS` tem precedência sobre o modo fork e mantém spawns de subagente em foreground.

<h3 id="api-errors-in-subagents">
  Erros de API em subagentes
</h3>

A partir da v2.1.199, um subagente cuja execução termina em um erro de API, como um limite de uso ou um erro de servidor repetido, relata essa falha de volta para Claude em vez de retornar o texto de erro como se fossem os achados do subagente. O que Claude recebe depende de onde o subagente foi executado:

* **Foreground**: se um limite de taxa, sobrecarga ou erro de servidor corta um subagente que já produziu saída de texto, a ferramenta Agent retorna essa saída parcial com uma nota de que o subagente foi cortado e não completou sua tarefa. Um subagente que não produziu nada, ou cuja única saída foram chamadas de ferramenta, falha com [`Agent terminated early due to an API error`](https://code.claude.com/docs/pt/errors#agent-terminated-early-due-to-an-api-error), seguido pelo detalhe do erro. Na v2.1.199, um limite de taxa, sobrecarga ou erro de servidor que cortou a forma de chamadas de ferramenta apenas retornou um resultado parcial vazio contendo apenas a nota de corte em vez disso.
* **Background**: o subagente é marcado como falho, e a mensagem que Claude recebe quando termina nomeia o erro de API e inclui a última saída do subagente, então o trabalho parcial não é perdido.

Uma vez que o erro de API subjacente seja resolvido, peça a Claude para tentar novamente a tarefa ou [retomar o subagente](#resume-subagents).

<h3 id="common-patterns">
  Padrões comuns
</h3>

<h4 id="isolate-high-volume-operations">
  Isolar operações de alto volume
</h4>

Um dos usos mais eficazes para subagentes é isolar operações que produzem grandes quantidades de saída. Executar testes, buscar documentação ou processar arquivos de log podem consumir contexto significativo. Ao delegar esses para um subagente, a saída verbosa fica no contexto do subagente enquanto apenas o resumo relevante retorna para sua conversa principal.

```text wrap theme={null}
Use a subagent to run the test suite and report only the failing tests with their error messages
```

<h4 id="run-parallel-research">
  Executar pesquisa em paralelo
</h4>

Para investigações independentes, gere múltiplos subagentes para trabalhar simultaneamente:

```text wrap theme={null}
Research the authentication, database, and API modules in parallel using separate subagents
```

Cada subagente explora sua área independentemente, então Claude sintetiza os achados. Isso funciona melhor quando os caminhos de pesquisa não dependem um do outro.

<Warning>
  Quando subagentes completam, seus resultados retornam para sua conversa principal. Executar muitos subagentes que cada um retorna resultados detalhados pode consumir contexto significativo.
</Warning>

Para tarefas que precisam de paralelismo sustentado ou excedem sua janela de contexto, [equipes de agentes](https://code.claude.com/docs/pt/agent-teams) dão a cada worker seu próprio contexto independente.

<h4 id="chain-subagents">
  Encadear subagentes
</h4>

Para fluxos de trabalho multi-etapas, peça a Claude para usar subagentes em sequência. Cada subagente completa sua tarefa e retorna resultados para Claude, que então passa contexto relevante para o próximo subagente.

```text wrap theme={null}
Use the code-reviewer subagent to find performance issues, then use the optimizer subagent to fix them
```

<h3 id="choose-between-subagents-and-main-conversation">
  Escolher entre subagentes e conversa principal
</h3>

Use a **conversa principal** quando:

* A tarefa precisa de frequente ida e volta ou refinamento iterativo
* Múltiplas fases compartilham contexto significativo, como planejamento, implementação e testes
* Você está fazendo uma mudança rápida e direcionada
* Latência importa. Subagentes começam do zero e podem precisar de tempo para reunir contexto

Use **subagentes** quando:

* A tarefa produz saída verbosa que você não precisa em seu contexto principal
* Você quer aplicar restrições de ferramentas específicas ou permissões
* O trabalho é auto-contido e pode retornar um resumo

Considere [Skills](https://code.claude.com/docs/pt/skills) em vez disso quando você quer prompts reutilizáveis ou fluxos de trabalho que são executados no contexto da conversa principal em vez de contexto de subagente isolado.

Para uma pergunta rápida sobre algo já em sua conversa, use [`/btw`](https://code.claude.com/docs/pt/interactive-mode#side-questions-with-%2Fbtw) em vez de um subagente. Ele vê seu contexto completo mas não tem acesso a ferramentas, e a resposta é descartada em vez de adicionada ao histórico.

<h3 id="spawn-nested-subagents">
  Gerar subagentes aninhados
</h3>

A partir do Claude Code v2.1.172, um subagente pode gerar seus próprios subagentes. Use isso quando uma tarefa delegada em si se divide em subtarefas paralelas, como um subagente revisor que distribui um verificador por descoberta, para que a saída intermediária nunca alcance sua conversa principal. Apenas o resumo do subagente de nível superior retorna para você.

Um subagente aninhado é configurado da mesma forma que um de nível superior e é resolvido dos mesmos [escopos](#choose-the-subagent-scope). O painel de subagente abaixo da entrada de prompt mostra a árvore completa: cada linha exibe uma contagem `(+N)` de descendentes, e a partir da v2.1.193, abrir uma linha mostra os irmãos desse subagente e filhos diretos com um caminho de volta para `main`.

A profundidade é contada como o número de níveis de subagente abaixo da conversa principal, independentemente de cada nível ser executado em [foreground ou background](#run-subagents-in-foreground-or-background). Um subagente na profundidade cinco não recebe a ferramenta Agent e não pode gerar mais. O limite é fixo e não configurável.

A partir do Claude Code v2.1.187, a profundidade de um subagente em background é fixada quando ele é primeiro gerado, e [retomar](#resume-subagents) isso mais tarde não muda essa profundidade. Por exemplo, se sua conversa principal gera subagente A, e A gera um subagente em background B na profundidade dois, B ainda está na profundidade dois quando você o retoma diretamente da conversa principal. Retomar um subagente de um contexto mais raso não permite que ele gere níveis adicionais que o limite de profundidade já impediu.

Para prevenir um subagente específico de gerar outros, omita `Agent` de sua lista [`tools`](#available-tools) ou adicione-o a `disallowedTools`.

Um [fork](#fork-the-current-conversation) ainda não pode gerar outro fork. Pode gerar outros tipos de subagente, e esses contam para o limite de profundidade.

<h3 id="manage-subagent-context">
  Gerenciar contexto de subagente
</h3>

<h4 id="what-loads-at-startup">
  O que carrega na inicialização
</h4>

Cada subagente começa com uma janela de contexto fresca e isolada. Ele não vê seu histórico de conversa, as skills que você já invocou, ou os arquivos que Claude já leu. Claude compõe uma mensagem de delegação que resume a tarefa, e o subagente trabalha a partir daí. A exceção é um [fork](#fork-the-current-conversation), que herda a conversa pai em vez de começar do zero.

O contexto inicial de um subagente não-fork contém:

* **Prompt de sistema**: o prompt próprio do agente mais detalhes de ambiente que Claude Code acrescenta, não o prompt de sistema completo do Claude Code. Subagentes personalizados definem o seu no [corpo markdown](#write-subagent-files) ou campo `prompt`. Agentes integrados têm prompts predefinidos.
* **Mensagem de tarefa**: o prompt de delegação que Claude escreve quando passa o trabalho.
* **CLAUDE.md e memória**: cada nível da [hierarquia de memória](https://code.claude.com/docs/pt/memory#how-claude-md-files-load) que a conversa principal carrega, incluindo `~/.claude/CLAUDE.md`, regras de projeto, `CLAUDE.local.md` e arquivos de política gerenciados. Os agentes integrados Explore e Plan pulam isso.
* **Status do Git**: um snapshot tirado no início da sessão pai. Ausente quando o diretório de trabalho não é um repositório Git ou quando [`includeGitInstructions`](https://code.claude.com/docs/pt/settings#available-settings) é `false`. Explore e Plan pulam isso independentemente.
* **Skills pré-carregadas**: conteúdo completo de qualquer skill nomeada no campo [`skills`](#preload-skills-into-subagents) do agente. Agentes integrados não pré-carregam skills.
* **Roster de irmãos**: um lembrete de sistema listando `main` e cada outro agente nomeado na sessão, cada um um valor `to` válido para [`SendMessage`](#resume-subagents). Requer Claude Code v2.1.206 ou posterior. O roster aparece apenas quando as ferramentas do subagente incluem `SendMessage` e pelo menos um outro agente tem um nome, seja Claude o nomeou ao gerá-lo ou ele é executado como um colega de [equipe de agentes](https://code.claude.com/docs/pt/agent-teams). É um snapshot tirado quando o subagente começa, então agentes nomeados depois não aparecem.

Explore e Plan são os únicos subagentes que omitem CLAUDE.md e status do Git. Não há campo de frontmatter ou configuração por-agente para mudar quais agentes pulam isso.

A conversa principal lê resultados de Explore e Plan com contexto completo de CLAUDE.md, então a maioria das regras não precisa alcançar o subagente em si. Se uma regra deve, como "ignore o diretório `vendor/`", reafirme-a no prompt que você dá a Claude ao delegar.

<h4 id="resume-subagents">
  Retomar subagentes
</h4>

Cada invocação de subagente cria uma nova instância com contexto fresco. Para continuar o trabalho de um subagente existente em vez de começar do zero, peça a Claude para retomá-lo.

Subagentes retomados retêm seu histórico de conversa completo, incluindo todas as chamadas de ferramentas anteriores, resultados e raciocínio. O subagente continua exatamente de onde parou em vez de começar do zero.

Quando um subagente completa, Claude recebe seu ID de agente. Os agentes integrados Explore e Plan são de uma única execução e não retornam ID de agente, então eles não podem ser retomados; use `general-purpose` ou um subagente personalizado quando você precisar continuar o trabalho.

Claude usa a ferramenta `SendMessage` com o ID do agente ou nome do agente como campo `to` para retomá-lo. `SendMessage` não requer que [equipes de agentes](https://code.claude.com/docs/pt/agent-teams) estejam habilitadas; apenas mensagens de protocolo de equipe estruturadas como `shutdown_request` e `plan_approval_response` fazem.

Para retomar um subagente, peça a Claude para continuar o trabalho anterior:

```text wrap theme={null}
Use the code-reviewer subagent to review the authentication module
[Agent completes]

Continue that code review and now analyze the authorization logic
[Claude resumes the subagent with full context from previous conversation]
```

Um subagente parado que recebe um `SendMessage` auto-retoma em background sem exigir uma nova invocação de `Agent`. O mesmo se aplica a um subagente que Claude parou com a ferramenta `TaskStop`.

A partir da v2.1.191, um subagente que você parou você mesmo, com `x` em `/tasks` ou uma solicitação SDK `stop_task`, não auto-retoma. A chamada `SendMessage` retorna uma recusa dizendo a Claude que o agente foi cancelado. Digite na transcrição desse subagente no painel de subagente para retomá-lo você mesmo, o que limpa a parada para que chamadas `SendMessage` posteriores possam auto-retomá-lo novamente.

Retomar inicia uma nova execução do agente sob o mesmo ID, então um subagente que já tinha falhado ou completado mostra como em execução novamente na lista de tarefas e nos eventos de tarefa do Agent SDK. Antes da v2.1.205, ele continuava mostrando seu status anterior de falha ou conclusão enquanto a execução retomada estava funcionando.

A partir da v2.1.199, `SendMessage` verifica que um nome ainda se refere ao mesmo agente que alcançou anteriormente na conversa. Se um agente mais novo assumiu o nome, como um agente em background re-gerado que o reutilizou, Claude Code recusa o envio em vez de entregá-lo ao agente errado, e o erro relata qual agente o nome agora alcança para que Claude possa redirecionar. Para alcançar o agente anterior enquanto ainda está em execução, Claude o endereça pelo ID do agente do resultado de spawn. A verificação é escopo da conversa atual e é redefinida em `/clear`.

A partir da v2.1.198, um subagente trata mensagens do agente que o lançou como direção de tarefa normal, incluindo correções de curso no meio da tarefa, e age sobre elas dentro de suas próprias configurações de permissão. Dois limites ainda se mantêm independentemente de quem enviou a mensagem: nenhuma mensagem de qualquer agente conta como sua aprovação para um prompt de permissão pendente, e nenhuma mensagem de agente pode mudar as configurações de permissão, `CLAUDE.md` ou configuração de um subagente. Apenas o sistema de permissão ou suas próprias mensagens podem conceder aprovação.

Você também pode pedir a Claude pelo ID do agente se quiser referenciá-lo explicitamente, ou encontrar IDs nos arquivos de transcrição em `~/.claude/projects/{project}/{sessionId}/subagents/`. Cada transcrição é armazenada como `agent-{agentId}.jsonl`.

Transcrições de subagente persistem independentemente da conversa principal:

* **Compactação da conversa principal**: Quando a conversa principal se compacta, transcrições de subagente não são afetadas. Elas são armazenadas em arquivos separados.
* **Persistência de sessão**: Transcrições de subagente persistem dentro de sua sessão. Você pode [retomar um subagente](#resume-subagents) após reiniciar Claude Code retomando a mesma sessão.
* **Limpeza automática**: Transcrições são limpas baseado na configuração `cleanupPeriodDays`, que padrão é 30 dias.

<h4 id="auto-compaction">
  Auto-compactação
</h4>

Subagentes suportam compactação automática usando a mesma lógica que a conversa principal. A compactação é acionada sob as mesmas condições, e `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE` se aplica a subagentes também. Veja [variáveis de ambiente](https://code.claude.com/docs/pt/env-vars) para quando a sobrescrita entra em efeito.

Eventos de compactação são registrados em arquivos de transcrição de subagente:

```json theme={null}
{
  "type": "system",
  "subtype": "compact_boundary",
  "compactMetadata": {
    "trigger": "auto",
    "preTokens": 167189
  }
}
```

O valor `preTokens` mostra quantos tokens foram usados antes da compactação ocorrer.

<h2 id="fork-the-current-conversation">
  Bifurcar a conversa atual
</h2>

<Note>
  Subagentes bifurcados requerem Claude Code v2.1.117 ou posterior. A partir da v2.1.161, o comando `/fork` está habilitado por padrão; em versões anteriores, requer definir a variável de ambiente [`CLAUDE_CODE_FORK_SUBAGENT`](https://code.claude.com/docs/pt/env-vars) para `1`. Deixar Claude gerar bifurcações é experimental e pode mudar em versões futuras. Esta capacidade também pode ser habilitada em sessões interativas como parte de um lançamento em fases.
</Note>

Uma bifurcação é um subagente que herda toda a conversa até agora em vez de começar do zero. Isso remove o isolamento de entrada que subagentes de outra forma fornecem: uma bifurcação vê o mesmo prompt de sistema, ferramentas, modelo e histórico de mensagens que a sessão principal, para que você possa entregar uma tarefa secundária sem re-explicar a situação. As chamadas de ferramentas da bifurcação ainda ficam fora de sua conversa e apenas seu resultado final volta, para que sua janela de contexto principal permaneça limpa. Use uma bifurcação quando um subagente nomeado precisaria de muito contexto para ser útil, ou quando você quer tentar várias abordagens em paralelo a partir do mesmo ponto de partida.

Para controlar o modo de bifurcação independentemente do lançamento em fases, defina [`CLAUDE_CODE_FORK_SUBAGENT`](https://code.claude.com/docs/pt/env-vars) para `1` para habilitá-lo explicitamente ou para `0` para desabilitá-lo. A variável é respeitada em modo interativo e via SDK ou `claude -p`.

Habilitar o modo de bifurcação muda Claude Code de duas formas:

* Claude pode gerar uma bifurcação solicitando explicitamente o tipo de subagente `fork`. Gerações sem um tipo de subagente ainda usam o subagente [general-purpose](#built-in-subagents), e subagentes nomeados como Explore ainda geram como antes.
* Cada geração de subagente é executada em [background](#run-subagents-in-foreground-or-background), seja uma bifurcação ou um subagente nomeado. Defina `CLAUDE_CODE_DISABLE_BACKGROUND_TASKS` para `1` para manter gerações síncronas.

Você pode iniciar uma bifurcação você mesmo com `/fork` seguido de uma diretiva, com ou sem a variável definida. Claude Code nomeia a bifurcação a partir das primeiras palavras da diretiva. O exemplo a seguir bifurca a conversa para rascunhar casos de teste enquanto você continua com a implementação na sessão principal:

```text wrap theme={null}
/fork draft unit tests for the parser changes so far
```

A bifurcação aparece em um painel abaixo do seu prompt e é executada em background enquanto você continua trabalhando. Quando termina, seu resultado chega como uma mensagem em sua conversa principal. A próxima seção cobre os controles do painel para observar e orientar bifurcações enquanto são executadas.

<h3 id="observe-and-steer-running-forks">
  Observar e orientar bifurcações em execução
</h3>

Bifurcações em execução aparecem em um painel abaixo da entrada de prompt, com uma linha para a sessão principal e uma para cada bifurcação. Use estas teclas para interagir com o painel:

| Key       | Action                                                                             |
| :-------- | :--------------------------------------------------------------------------------- |
| `↑` / `↓` | Mover entre linhas                                                                 |
| `Enter`   | Abrir a transcrição da bifurcação selecionada e enviar mensagens de acompanhamento |
| `x`       | Descartar uma bifurcação terminada ou parar uma em execução                        |
| `Esc`     | Retornar foco para a entrada de prompt                                             |

Com a transcrição de uma bifurcação ou subagente aberta, mensagens de acompanhamento e [skills](https://code.claude.com/docs/pt/skills) vão para esse agente, mas comandos integrados ainda são executados em sua conversa principal. A partir da v2.1.199, digitar `/model` ou `/fast` nessa visualização mostra um aviso de que isso muda o modelo da conversa principal ou modo rápido, não do agente visualizado, em vez de executá-lo silenciosamente.

<h3 id="how-forks-differ-from-named-subagents">
  Como bifurcações diferem de subagentes nomeados
</h3>

Uma bifurcação herda tudo que a sessão principal tem no momento em que é gerada. Um subagente nomeado começa a partir de sua própria definição.

|                         | Bifurcação                           | Subagente nomeado                                                                                                       |
| :---------------------- | :----------------------------------- | :---------------------------------------------------------------------------------------------------------------------- |
| Context                 | Histórico de conversa completo       | Contexto fresco com o prompt que você passa                                                                             |
| System prompt and tools | Mesmo que a sessão principal         | Da [definição file](#write-subagent-files) do subagente                                                                 |
| Model                   | Mesmo que a sessão principal         | Do campo `model` do subagente                                                                                           |
| Permissions             | Prompts aparecem em seu terminal     | [Prompts aparecem em sua sessão principal](#run-subagents-in-foreground-or-background) quando em execução em background |
| Prompt cache            | Compartilhado com a sessão principal | Cache separado                                                                                                          |

Porque o prompt de sistema de uma bifurcação e as definições de ferramentas são idênticas ao pai, sua primeira solicitação reutiliza o [prompt cache](https://code.claude.com/docs/pt/prompt-caching#subagents-and-the-cache) do pai. Isso torna bifurcação mais barata do que gerar um subagente fresco para tarefas que precisam do mesmo contexto.

Quando Claude gera uma bifurcação através da ferramenta Agent, ele pode passar `isolation: "worktree"` para que as edições de arquivo da bifurcação sejam escritas em um git worktree separado em vez de seu checkout.

<h3 id="limitations">
  Limitações
</h3>

Definir `CLAUDE_CODE_FORK_SUBAGENT=1` habilita fork mode em sessões interativas, [modo não-interativo](https://code.claude.com/docs/pt/headless) e o Agent SDK; definir para `0` desabilita fork mode em todos os lugares, incluindo qualquer lançamento no servidor. Uma bifurcação não pode gerar bifurcações adicionais.

<h2 id="example-subagents">
  Subagentes de exemplo
</h2>

Estes exemplos demonstram padrões eficazes para construir subagentes. Use-os como pontos de partida, ou gere uma versão personalizada com Claude.

<Tip>
  **Melhores práticas:**

  * **Projete subagentes focados:** cada subagente deve se destacar em uma tarefa específica
  * **Escreva descrições detalhadas:** Claude usa a descrição para decidir quando delegar
  * **Limite acesso a ferramentas:** conceda apenas permissões necessárias para segurança e foco
  * **Verifique no controle de versão:** compartilhe subagentes de projeto com sua equipe
</Tip>

<h3 id="code-reviewer">
  Revisor de código
</h3>

Um subagente somente leitura que revisa código sem modificá-lo. Este exemplo mostra como projetar um subagente focado com acesso limitado a ferramentas que exclui Edit e Write, e um prompt detalhado que especifica exatamente o que procurar e como formatar a saída.

```markdown theme={null}
---
name: code-reviewer
description: Expert code review specialist. Proactively reviews code for quality, security, and maintainability. Use immediately after writing or modifying code.
tools: Read, Grep, Glob, Bash
model: inherit
---

You are a senior code reviewer ensuring high standards of code quality and security.

When invoked:
1. Run git diff to see recent changes
2. Focus on modified files
3. Begin review immediately

Review checklist:
- Code is clear and readable
- Functions and variables are well-named
- No duplicated code
- Proper error handling
- No exposed secrets or API keys
- Input validation implemented
- Good test coverage
- Performance considerations addressed

Provide feedback organized by priority:
- Critical issues (must fix)
- Warnings (should fix)
- Suggestions (consider improving)

Include specific examples of how to fix issues.
```

<h3 id="debugger">
  Debugger
</h3>

Um subagente que pode analisar e corrigir problemas. Diferentemente do revisor de código, este inclui Edit porque corrigir bugs requer modificar código. O prompt fornece um fluxo de trabalho claro de diagnóstico para verificação.

```markdown theme={null}
---
name: debugger
description: Debugging specialist for errors, test failures, and unexpected behavior. Use proactively when encountering any issues.
tools: Read, Edit, Bash, Grep, Glob
---

You are an expert debugger specializing in root cause analysis.

When invoked:
1. Capture error message and stack trace
2. Identify reproduction steps
3. Isolate the failure location
4. Implement minimal fix
5. Verify solution works

Debugging process:
- Analyze error messages and logs
- Check recent code changes
- Form and test hypotheses
- Add strategic debug logging
- Inspect variable states

For each issue, provide:
- Root cause explanation
- Evidence supporting the diagnosis
- Specific code fix
- Testing approach
- Prevention recommendations

Focus on fixing the underlying issue, not the symptoms.
```

<h3 id="data-scientist">
  Cientista de dados
</h3>

Um subagente específico de domínio para trabalho de análise de dados. Este exemplo mostra como criar subagentes para fluxos de trabalho especializados fora de tarefas de codificação típicas. Ele explicitamente define `model: sonnet` para análise mais capaz.

```markdown theme={null}
---
name: data-scientist
description: Data analysis expert for SQL queries, BigQuery operations, and data insights. Use proactively for data analysis tasks and queries.
tools: Bash, Read, Write
model: sonnet
---

You are a data scientist specializing in SQL and BigQuery analysis.

When invoked:
1. Understand the data analysis requirement
2. Write efficient SQL queries
3. Use BigQuery command line tools (bq) when appropriate
4. Analyze and summarize results
5. Present findings clearly

Key practices:
- Write optimized SQL queries with proper filters
- Use appropriate aggregations and joins
- Include comments explaining complex logic
- Format results for readability
- Provide data-driven recommendations

For each analysis:
- Explain the query approach
- Document any assumptions
- Highlight key findings
- Suggest next steps based on data

Always ensure queries are efficient and cost-effective.
```

<h3 id="database-query-validator">
  Validador de consulta de banco de dados
</h3>

Um subagente que permite acesso Bash mas valida comandos para permitir apenas consultas SQL somente leitura. Este exemplo mostra como usar hooks `PreToolUse` para validação condicional quando você precisa de controle mais fino do que o campo `tools` fornece.

```markdown theme={null}
---
name: db-reader
description: Execute read-only database queries. Use when analyzing data or generating reports.
tools: Bash
hooks:
  PreToolUse:
    - matcher: "Bash"
      hooks:
        - type: command
          command: "./scripts/validate-readonly-query.sh"
---

You are a database analyst with read-only access. Execute SELECT queries to answer questions about the data.

When asked to analyze data:
1. Identify which tables contain the relevant data
2. Write efficient SELECT queries with appropriate filters
3. Present results clearly with context

You cannot modify data. If asked to INSERT, UPDATE, DELETE, or modify schema, explain that you only have read access.
```

Claude Code [passa entrada de hook como JSON](https://code.claude.com/docs/pt/hooks#pretooluse-input) via stdin para comandos de hook. O script de validação lê este JSON, extrai o comando sendo executado e o verifica contra uma lista de operações de escrita SQL. Se uma operação de escrita é detectada, o script [sai com código 2](https://code.claude.com/docs/pt/hooks#exit-code-2-behavior-per-event) para bloquear execução e retorna uma mensagem de erro para Claude via stderr.

Crie o script de validação em qualquer lugar em seu projeto. O caminho deve corresponder ao campo `command` em sua configuração de hook:

```bash theme={null}
#!/bin/bash
# Blocks SQL write operations, allows SELECT queries

# Read JSON input from stdin
INPUT=$(cat)

# Extract the command field from tool_input using jq
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

if [ -z "$COMMAND" ]; then
  exit 0
fi

# Block write operations (case-insensitive)
if echo "$COMMAND" | grep -iE '\b(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE|REPLACE|MERGE)\b' > /dev/null; then
  echo "Blocked: Write operations not allowed. Use SELECT queries only." >&2
  exit 2
fi

exit 0
```

No macOS e Linux, torne o script executável:

```bash theme={null}
chmod +x ./scripts/validate-readonly-query.sh
```

No Windows, escreva o script de validação em PowerShell e adicione `shell: powershell` à entrada de hook. Veja [executando hooks em PowerShell](https://code.claude.com/docs/pt/hooks#windows-powershell-tool).

O hook recebe JSON via stdin com o comando Bash em `tool_input.command`. Código de saída 2 bloqueia a operação e alimenta a mensagem de erro de volta para Claude. Veja [Hooks](https://code.claude.com/docs/pt/hooks#exit-code-output) para detalhes sobre códigos de saída e [Hook input](https://code.claude.com/docs/pt/hooks#pretooluse-input) para o schema de entrada completo.

<h2 id="next-steps">
  Próximos passos
</h2>

Agora que você entende subagentes, explore estes recursos relacionados:

* [Distribuir subagentes com plugins](https://code.claude.com/docs/pt/plugins) para compartilhar subagentes entre equipes ou projetos
* [Executar Claude Code programaticamente](https://code.claude.com/docs/pt/headless) com o Agent SDK para CI/CD e automação
* [Usar MCP servers](https://code.claude.com/docs/pt/mcp) para dar aos subagentes acesso a ferramentas e dados externos
