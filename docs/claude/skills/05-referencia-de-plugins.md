<!-- Fonte: https://code.claude.com/docs/pt/plugins-reference | Baixado em: 2026-08-06 | slug: plugins-reference -->

# Referência de plugins

> Referência técnica completa para o sistema de plugins do Claude Code, incluindo esquemas, comandos CLI e especificações de componentes.

<Tip>
  Procurando instalar plugins? Veja [Descobrir e instalar plugins](https://code.claude.com/docs/pt/discover-plugins). Para criar plugins, veja [Plugins](https://code.claude.com/docs/pt/plugins). Para distribuir plugins, veja [Marketplaces de plugins](https://code.claude.com/docs/pt/plugin-marketplaces).
</Tip>

Esta referência fornece especificações técnicas completas para o sistema de plugins do Claude Code, incluindo esquemas de componentes, comandos CLI e ferramentas de desenvolvimento.

Um **plugin** é um diretório independente de componentes que estende o Claude Code com funcionalidade personalizada. Os componentes do plugin incluem skills, agents, hooks, servidores MCP, servidores LSP e monitors.

<h2 id="plugin-components-reference">
  Referência de componentes de plugin
</h2>

<h3 id="skills">
  Skills
</h3>

Os plugins adicionam skills ao Claude Code, criando atalhos `/name` que você ou Claude podem invocar.

**Localização**: Diretório `skills/` ou `commands/` na raiz do plugin, ou um único arquivo `SKILL.md` na raiz do plugin

**Formato de arquivo**: Skills são diretórios com `SKILL.md`; comandos são arquivos markdown simples

**Estrutura de skill**:

```text theme={null}
skills/
├── pdf-processor/
│   ├── SKILL.md
│   ├── reference.md (opcional)
│   └── scripts/ (opcional)
└── code-reviewer/
    └── SKILL.md
```

**Comportamento de integração**:

* Skills e comandos são descobertos automaticamente quando o plugin é instalado
* Claude pode invocá-los automaticamente com base no contexto da tarefa
* Skills podem incluir arquivos de suporte ao lado de SKILL.md

Se um plugin não tem diretório `skills/` e nenhum campo manifest `skills`, um `SKILL.md` na raiz do plugin é carregado como uma única skill. Defina o campo frontmatter `name` para controlar o nome de invocação da skill. Sem ele, Claude Code volta para o nome do diretório de instalação, que para plugins instalados do marketplace é uma string de versão que muda a cada atualização. Para plugins que fornecem mais de uma skill, use o layout de diretório `skills/` mostrado acima.

Para detalhes completos, veja [Skills](https://code.claude.com/docs/pt/skills).

<h3 id="agents">
  Agents
</h3>

Os plugins podem fornecer subagents especializados para tarefas específicas que Claude pode invocar automaticamente quando apropriado.

**Localização**: Diretório `agents/` na raiz do plugin

**Formato de arquivo**: Arquivos markdown descrevendo capacidades do agent

**Estrutura de agent**:

```markdown theme={null}
---
name: agent-name
description: No que este agent se especializa e quando Claude deve invocá-lo
model: sonnet
effort: medium
maxTurns: 20
disallowedTools: Write, Edit
---

Prompt de sistema detalhado para o agent descrevendo seu papel, expertise e comportamento.
```

Os agents de plugin suportam campos frontmatter `name`, `description`, `model`, `effort`, `maxTurns`, `tools`, `disallowedTools`, `skills`, `memory`, `background` e `isolation`. O único valor válido de `isolation` é `"worktree"`. Por razões de segurança, `hooks`, `mcpServers` e `permissionMode` não são suportados para agents fornecidos por plugin.

**Pontos de integração**:

* Agents aparecem na typeahead [@-mention](https://code.claude.com/docs/pt/sub-agents#invoke-subagents-explicitly) sob seu nome com escopo, como `my-plugin:code-reviewer`, uma vez que o plugin está habilitado
* Claude pode invocar agents automaticamente com base no contexto da tarefa
* Agents podem ser invocados manualmente por usuários
* Agents de plugin funcionam ao lado de agents Claude integrados

Para detalhes completos, veja [Subagents](https://code.claude.com/docs/pt/sub-agents).

<h3 id="hooks">
  Hooks
</h3>

Os plugins podem fornecer manipuladores de eventos que respondem a eventos do Claude Code automaticamente.

**Localização**: `hooks/hooks.json` na raiz do plugin, ou inline em plugin.json

**Formato**: Configuração JSON com matchers de eventos e ações

**Configuração de hook**:

```json theme={null}
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "\"${CLAUDE_PLUGIN_ROOT}\"/scripts/format-code.sh"
          }
        ]
      }
    ]
  }
}
```

Os hooks de plugin respondem aos mesmos eventos de ciclo de vida que [hooks definidos pelo usuário](https://code.claude.com/docs/pt/hooks):

| Event                 | When it fires                                                                                                                                          |
| :-------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SessionStart`        | When a session begins or resumes                                                                                                                       |
| `Setup`               | When you start Claude Code with `--init-only`, or with `--init` or `--maintenance` in `-p` mode. For one-time preparation in CI or scripts             |
| `UserPromptSubmit`    | When you submit a prompt, before Claude processes it                                                                                                   |
| `UserPromptExpansion` | When a user-typed command expands into a prompt, before it reaches Claude. Can block the expansion                                                     |
| `PreToolUse`          | Before a tool call executes. Can block it                                                                                                              |
| `PermissionRequest`   | When a tool call needs a permission decision                                                                                                           |
| `PermissionDenied`    | When a tool call is denied by the auto mode classifier. Return `{retry: true}` to tell the model it may retry the denied tool call                     |
| `PostToolUse`         | After a tool call succeeds                                                                                                                             |
| `PostToolUseFailure`  | After a tool call fails                                                                                                                                |
| `PostToolBatch`       | After a full batch of parallel tool calls resolves, before the next model call                                                                         |
| `Notification`        | When Claude Code sends a notification                                                                                                                  |
| `MessageDisplay`      | While assistant message text is displayed                                                                                                              |
| `SubagentStart`       | When a subagent is spawned                                                                                                                             |
| `SubagentStop`        | When a subagent finishes                                                                                                                               |
| `TaskCreated`         | When a task is being created via `TaskCreate`                                                                                                          |
| `TaskCompleted`       | When a task is being marked as completed                                                                                                               |
| `Stop`                | When Claude finishes responding                                                                                                                        |
| `StopFailure`         | When the turn ends due to an API error. Output and exit code are ignored                                                                               |
| `TeammateIdle`        | When an [agent team](https://code.claude.com/docs/en/agent-teams) teammate is about to go idle                                                                                     |
| `InstructionsLoaded`  | When a CLAUDE.md or `.claude/rules/*.md` file is loaded into context. Fires at session start and when files are lazily loaded during a session         |
| `ConfigChange`        | When a configuration file changes during a session                                                                                                     |
| `CwdChanged`          | When the working directory changes, for example when Claude executes a `cd` command. Useful for reactive environment management with tools like direnv |
| `DirectoryAdded`      | When a working directory is added mid-session via `/add-dir` or the SDK `register_repo_root` control request                                           |
| `FileChanged`         | When a watched file changes on disk. The `matcher` field specifies which filenames to watch                                                            |
| `WorktreeCreate`      | When a worktree is being created via `--worktree`, `isolation: "worktree"`, or for a background session. Replaces default git behavior                 |
| `WorktreeRemove`      | When a worktree is being removed at session exit, when a subagent finishes, or when you delete a background session                                    |
| `PreCompact`          | Before context compaction                                                                                                                              |
| `PostCompact`         | After context compaction completes                                                                                                                     |
| `Elicitation`         | When an MCP server requests user input during a tool call                                                                                              |
| `ElicitationResult`   | After a user responds to an MCP elicitation, before the response is sent back to the server                                                            |
| `SessionEnd`          | When a session terminates                                                                                                                              |

**Tipos de hook**:

* `command`: executar comandos shell ou scripts
* `http`: enviar o JSON do evento como uma solicitação POST para uma URL
* `mcp_tool`: chamar uma ferramenta em um servidor [MCP](https://code.claude.com/docs/pt/mcp) configurado
* `prompt`: avaliar um prompt com um LLM (usa placeholder `$ARGUMENTS` para contexto)
* `agent`: executar um verificador agentic com ferramentas para tarefas de verificação complexas

Os hooks que visam o próprio servidor [MCP agrupado](#mcp-servers) do plugin devem usar seus nomes com escopo. Os matchers de ferramenta e campos `if` usam o nome de ferramenta com escopo `mcp__plugin_<plugin-name>_<server-name>__<tool>`, e o campo `server` de um hook `mcp_tool` usa `plugin:<plugin-name>:<server-name>`. Um matcher escrito contra a chave de servidor simples nunca dispara. Veja [Match MCP tools](https://code.claude.com/docs/pt/hooks#match-mcp-tools) e [Plugin-provided MCP servers](https://code.claude.com/docs/pt/mcp#plugin-provided-mcp-servers).

<h3 id="mcp-servers">
  MCP servers
</h3>

Os plugins podem agrupar servidores Model Context Protocol (MCP) para conectar Claude Code com ferramentas e serviços externos.

**Localização**: `.mcp.json` na raiz do plugin, ou inline em plugin.json

**Formato**: Configuração padrão de servidor MCP

**Configuração de servidor MCP**:

```json theme={null}
{
  "mcpServers": {
    "plugin-database": {
      "command": "${CLAUDE_PLUGIN_ROOT}/servers/db-server",
      "args": ["--config", "${CLAUDE_PLUGIN_ROOT}/config.json"],
      "env": {
        "DB_PATH": "${CLAUDE_PLUGIN_ROOT}/data"
      }
    },
    "plugin-api-client": {
      "command": "npx",
      "args": ["@company/mcp-server", "--plugin-mode"]
    }
  }
}
```

**Comportamento de integração**:

* Servidores MCP de plugin iniciam automaticamente quando o plugin é habilitado
* Servidores aparecem como ferramentas MCP padrão no kit de ferramentas de Claude
* Capacidades do servidor se integram perfeitamente com as ferramentas existentes de Claude
* Servidores de plugin podem ser configurados independentemente de servidores MCP do usuário

<h3 id="lsp-servers">
  LSP servers
</h3>

<Tip>
  Procurando usar plugins LSP? Instale-os do marketplace oficial: procure por "lsp" na aba Discover do `/plugin`. Esta seção documenta como criar plugins LSP para linguagens não cobertas pelo marketplace oficial.
</Tip>

Os plugins podem fornecer servidores [Language Server Protocol](https://microsoft.github.io/language-server-protocol/) (LSP) para dar a Claude inteligência de código em tempo real enquanto trabalha em seu codebase.

A integração LSP fornece:

* **Diagnósticos instantâneos**: Claude vê erros e avisos imediatamente após cada edição
* **Navegação de código**: ir para definição, encontrar referências e informações de hover
* **Consciência de linguagem**: informações de tipo e documentação para símbolos de código

**Localização**: `.lsp.json` na raiz do plugin, ou inline em `plugin.json`

**Formato**: Configuração JSON mapeando nomes de servidores de linguagem para suas configurações

**Formato de arquivo `.lsp.json`**:

```json theme={null}
{
  "go": {
    "command": "gopls",
    "args": ["serve"],
    "extensionToLanguage": {
      ".go": "go"
    }
  }
}
```

**Inline em `plugin.json`**:

```json theme={null}
{
  "name": "my-plugin",
  "lspServers": {
    "go": {
      "command": "gopls",
      "args": ["serve"],
      "extensionToLanguage": {
        ".go": "go"
      }
    }
  }
}
```

**Campos obrigatórios:**

| Campo                 | Descrição                                                     |
| :-------------------- | :------------------------------------------------------------ |
| `command`             | O binário LSP a executar (deve estar em PATH)                 |
| `extensionToLanguage` | Mapeia extensões de arquivo para identificadores de linguagem |

**Campos opcionais:**

| Campo                   | Descrição                                                                                                                                                                                         |
| :---------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `args`                  | Argumentos de linha de comando para o servidor LSP                                                                                                                                                |
| `transport`             | Transporte de comunicação: `stdio` (padrão) ou `socket`                                                                                                                                           |
| `env`                   | Variáveis de ambiente a definir ao iniciar o servidor                                                                                                                                             |
| `initializationOptions` | Opções passadas ao servidor durante a inicialização                                                                                                                                               |
| `settings`              | Configurações passadas via `workspace/didChangeConfiguration`                                                                                                                                     |
| `workspaceFolder`       | Caminho da pasta de workspace para o servidor                                                                                                                                                     |
| `startupTimeout`        | Tempo máximo para aguardar inicialização do servidor (milissegundos)                                                                                                                              |
| `shutdownTimeout`       | Tempo máximo para aguardar desligamento gracioso (milissegundos). Quando o tempo limite se esgota, Claude Code encerra o processo do servidor. Quando não definido, nenhum tempo limite se aplica |
| `restartOnCrash`        | Se deve reiniciar o servidor após ele falhar. Padrão é `true`. Defina como `false` para deixar um servidor que falhou parado em vez de reiniciá-lo                                                |
| `maxRestarts`           | Número máximo de tentativas de reinicialização antes de desistir                                                                                                                                  |
| `diagnostics`           | Se deve enviar diagnósticos para o contexto de Claude após edições (padrão `true`). Defina como `false` para manter navegação de código mas suprimir injeção automática de diagnósticos.          |

`restartOnCrash` e `shutdownTimeout` requerem Claude Code v2.1.205 ou posterior. Antes de v2.1.205, o schema de configuração aceitava ambas as opções, mas definir qualquer uma delas fazia Claude Code pular esse servidor LSP inteiramente na inicialização, com o motivo visível apenas na saída `claude --debug`.

**Múltiplos servidores para a mesma extensão**: quando mais de um servidor LSP habilitado declara a mesma extensão de arquivo em `extensionToLanguage`, quer os servidores venham de um plugin ou de plugins diferentes, o primeiro servidor registrado manipula arquivos com essa extensão e os outros nunca iniciam. A interface `/plugin` mostra um aviso nomeando o plugin cujo servidor está ativo.

**Servidores que falham ao inicializar**: Claude Code pula um servidor cuja configuração é inválida, por exemplo um que falta `command` ou `extensionToLanguage`, e os outros servidores configurados ainda iniciam. Execute `claude --debug` para ver por que um servidor foi pulado.

Um servidor pulado não reclama suas extensões de arquivo, então outro servidor válido que declara a mesma extensão, do mesmo plugin ou de um plugin diferente, ainda manipula esses arquivos. Antes de v2.1.205, um servidor que falhou ao inicializar ainda reivindicava suas extensões e bloqueava outro servidor válido para a mesma extensão.

<Warning>
  **Você deve instalar o binário do servidor de linguagem separadamente.** Plugins LSP configuram como Claude Code se conecta a um servidor de linguagem, mas não incluem o servidor em si. Se você vir `Executable not found in $PATH` na aba Errors do `/plugin`, instale o binário necessário para sua linguagem.
</Warning>

**Plugins LSP disponíveis:**

| Plugin              | Servidor de linguagem      | Comando de instalação                                                                        |
| :------------------ | :------------------------- | :------------------------------------------------------------------------------------------- |
| `pyright-lsp`       | Pyright (Python)           | `pip install pyright` ou `npm install -g pyright`                                            |
| `typescript-lsp`    | TypeScript Language Server | `npm install -g typescript-language-server typescript`                                       |
| `rust-analyzer-lsp` | rust-analyzer              | [Veja instalação de rust-analyzer](https://rust-analyzer.github.io/manual.html#installation) |

Instale o servidor de linguagem primeiro, depois instale o plugin do marketplace.

<h3 id="monitors">
  Monitors
</h3>

Os plugins podem declarar monitors de fundo que Claude Code inicia automaticamente quando o plugin está ativo. Cada monitor executa um comando shell pela duração da sessão e entrega cada linha stdout a Claude como uma notificação, para que Claude possa reagir a entradas de log, mudanças de status ou eventos pesquisados sem ser solicitado a iniciar o watch em si.

Os monitors de plugin usam o mesmo mecanismo que a [ferramenta Monitor](https://code.claude.com/docs/pt/tools-reference#monitor-tool) e compartilham suas restrições de disponibilidade. Eles são executados apenas em sessões CLI interativas, executados sem sandbox no mesmo nível de confiança que [hooks](#hooks), e são ignorados em hosts onde a ferramenta Monitor não está disponível.

**Localização**: `monitors/monitors.json` na raiz do plugin, ou inline em `plugin.json`

**Formato**: Array JSON de entradas de monitor

O seguinte `monitors/monitors.json` monitora um endpoint de status de implantação e um log de erro local:

```json theme={null}
[
  {
    "name": "deploy-status",
    "command": "\"${CLAUDE_PLUGIN_ROOT}\"/scripts/poll-deploy.sh",
    "description": "Mudanças de status de implantação"
  },
  {
    "name": "error-log",
    "command": "tail -F ./logs/error.log",
    "description": "Log de erro da aplicação",
    "when": "on-skill-invoke:debug"
  }
]
```

Para declarar monitors inline, defina `experimental.monitors` em `plugin.json` para o mesmo array. Para carregar de um caminho não padrão, defina `experimental.monitors` para uma string de caminho relativo como `"./config/monitors.json"`. Monitors são um [componente experimental](#experimental-components).

**Campos obrigatórios:**

| Campo         | Descrição                                                                                                                      |
| :------------ | :----------------------------------------------------------------------------------------------------------------------------- |
| `name`        | Identificador único dentro do plugin. Previne processos duplicados quando o plugin recarrega ou uma skill é invocada novamente |
| `command`     | Comando shell executado como um processo de fundo persistente no diretório de trabalho da sessão                               |
| `description` | Resumo breve do que está sendo monitorado. Mostrado no painel de tarefas e em resumos de notificação                           |

**Campos opcionais:**

| Campo  | Descrição                                                                                                                                                                                                                      |
| :----- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `when` | Controla quando o monitor inicia. `"always"` o inicia no início da sessão e no recarregamento do plugin, e é o padrão. `"on-skill-invoke:<skill-name>"` o inicia na primeira vez que a skill nomeada neste plugin é despachada |

O valor `command` suporta as [substituições de caminho](#environment-variables) `${CLAUDE_PLUGIN_ROOT}`, `${CLAUDE_PLUGIN_DATA}` e `${CLAUDE_PROJECT_DIR}`, mais qualquer `${ENV_VAR}` do ambiente. Prefixe o comando com `cd "${CLAUDE_PLUGIN_ROOT}" && ` se o script precisa ser executado do próprio diretório do plugin.

Um comando `command` de monitor não pode referenciar valores [`${user_config.*}`](#user-configuration). O comando é executado através de um shell, então Claude Code rejeita o monitor com um [erro](https://code.claude.com/docs/pt/errors#plugin-command-references-user-config) em vez de substituir o valor. Processos de monitor não recebem variáveis de ambiente `CLAUDE_PLUGIN_OPTION_<KEY>`, então faça o script de monitor ler o valor de um arquivo de configuração que ele possui. Antes de v2.1.207, comandos de monitor substituíam valores `${user_config.*}`.

Desabilitar um plugin no meio da sessão não para monitors que já estão em execução. Eles param quando a sessão termina.

<h3 id="themes">
  Themes
</h3>

Os plugins podem fornecer temas de cor que aparecem em `/theme` ao lado das predefinições integradas e dos temas locais do usuário. Um tema é um arquivo JSON em `themes/` com uma predefinição `base` e um mapa esparso `overrides` de tokens de cor. Themes são um [componente experimental](#experimental-components).

```json theme={null}
{
  "name": "Dracula",
  "base": "dark",
  "overrides": {
    "claude": "#bd93f9",
    "error": "#ff5555",
    "success": "#50fa7b"
  }
}
```

Selecionar um tema de plugin persiste `custom:<plugin-name>:<slug>` na configuração do usuário. Temas de plugin são somente leitura; pressionar `Ctrl+E` em um em `/theme` o copia para `~/.claude/themes/` para que o usuário possa editar a cópia.

***

<h2 id="plugin-installation-scopes">
  Escopos de instalação de plugin
</h2>

Quando você instala um plugin, você escolhe um **escopo** que determina onde o plugin está disponível e quem mais pode usá-lo:

| Escopo    | Arquivo de configurações                                 | Caso de uso                                                |
| :-------- | :------------------------------------------------------- | :--------------------------------------------------------- |
| `user`    | `~/.claude/settings.json`                                | Plugins pessoais disponíveis em todos os projetos (padrão) |
| `project` | `.claude/settings.json`                                  | Plugins de equipe compartilhados via controle de versão    |
| `local`   | `.claude/settings.local.json`                            | Plugins específicos do projeto, gitignored                 |
| `managed` | [Configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files) | Plugins gerenciados (somente leitura, apenas atualizar)    |

Os plugins usam o mesmo sistema de escopo que outras configurações do Claude Code. Para instruções de instalação e flags de escopo, veja [Instalar plugins](https://code.claude.com/docs/pt/discover-plugins#install-plugins). Para uma explicação completa de escopos, veja [Escopos de configuração](https://code.claude.com/docs/pt/settings#configuration-scopes).

***

<h2 id="skills-directory-plugins">
  Plugins de diretório de skills
</h2>

Qualquer pasta sob um diretório de skills que contenha um manifesto `.claude-plugin/plugin.json` é carregada como um plugin nomeado `<name>@skills-dir` na próxima sessão, sem marketplace e sem etapa de instalação. Crie um com [`plugin init`](#plugin-init). Ao contrário de uma instalação do marketplace, o plugin é descoberto no local em vez de ser copiado para o cache de plugin.

Uma árvore de diretório de skills suporta três coisas distintas:

| O que você tem                                | O que é                                                                                 |
| :-------------------------------------------- | :-------------------------------------------------------------------------------------- |
| `<skills-dir>/foo/SKILL.md` sem manifesto     | Uma [skill](https://code.claude.com/docs/pt/skills) simples nomeada `foo`                                           |
| `<skills-dir>/foo/.claude-plugin/plugin.json` | Um plugin `foo@skills-dir`, que pode agrupar suas próprias skills, agents, hooks e mais |
| `<plugin>/skills/bar/SKILL.md`                | Uma skill `bar` empacotada dentro de um plugin                                          |

<h3 id="choose-where-the-plugin-loads-from">
  Escolha de onde o plugin carrega
</h3>

| Diretório de skills     | Escopo  | Carrega                                                                                           |
| :---------------------- | :------ | :------------------------------------------------------------------------------------------------ |
| `~/.claude/skills/`     | pessoal | Em cada projeto, já que a localização é apenas sua                                                |
| `<cwd>/.claude/skills/` | projeto | Apenas depois que você aceita o [diálogo de confiança](https://code.claude.com/docs/pt/settings) do workspace para essa pasta |

Um plugin de escopo de projeto é verificado no repositório e alcança cada colaborador que o clona. Como esse conteúdo vem do repositório em vez de você, ele carrega apenas após o mesmo portão de confiança que governa `.claude/settings.json`, e componentes que executam código são ainda mais restritos:

* Servidores MCP que declara passam pela [mesma aprovação por servidor](https://code.claude.com/docs/pt/mcp) que um `.mcp.json` de projeto
* Servidores LSP iniciam apenas depois que você confia no workspace
* [Monitors de fundo](#monitors) não carregam

Plugins de escopo pessoal não têm nenhuma dessas restrições.

<Warning>
  Plugins `@skills-dir` de escopo de projeto carregam apenas de `.claude/skills/` do diretório onde você inicia Claude Code. Eles não [caminham até a raiz do repositório](https://code.claude.com/docs/pt/skills#automatic-discovery-from-parent-and-nested-directories) da maneira que skills e comandos simples fazem, então iniciar de um subdiretório perde um plugin que vive na raiz do repo. Inicie da raiz do repositório, ou execute `/reload-plugins` após mudar de diretório.
</Warning>

<h3 id="edit-reload-and-disable-a-skills-directory-plugin">
  Editar, recarregar e desabilitar um plugin de diretório de skills
</h3>

As alterações que você faz no `SKILL.md` de uma skill têm efeito imediatamente na sessão atual. Alterações em outros componentes do plugin, como `hooks/`, `.mcp.json`, `agents/` e `output-styles/`, não têm. Execute `/reload-plugins` ou reinicie Claude Code para pegá-las. Veja [Detecção de mudança ao vivo](https://code.claude.com/docs/pt/skills#live-change-detection).

Para parar de carregar um plugin de diretório de skills, delete sua pasta ou desabilite-o por nome. Não há etapa de `uninstall` porque nada foi instalado de um marketplace.

```bash theme={null}
claude plugin disable my-tool@skills-dir
```

***

<h2 id="plugin-manifest-schema">
  Esquema de manifesto de plugin
</h2>

O arquivo `.claude-plugin/plugin.json` define os metadados e configuração do seu plugin. Esta seção documenta todos os campos e opções suportados.

O manifesto é opcional. Se omitido, Claude Code descobre automaticamente componentes em [localizações padrão](#file-locations-reference) e deriva o nome do plugin do nome do diretório. Use um manifesto quando você precisar fornecer metadados ou caminhos de componentes personalizados.

<h3 id="complete-schema">
  Esquema completo
</h3>

```json theme={null}
{
  "name": "plugin-name",
  "displayName": "Plugin Name",
  "version": "1.2.0",
  "description": "Brief plugin description",
  "author": {
    "name": "Author Name",
    "email": "author@example.com",
    "url": "https://github.com/author"
  },
  "homepage": "https://docs.example.com/plugin",
  "repository": "https://github.com/author/plugin",
  "license": "MIT",
  "keywords": ["keyword1", "keyword2"],
  "skills": "./custom/skills/",
  "commands": ["./custom/commands/special.md"],
  "agents": ["./custom/agents/reviewer.md"],
  "hooks": "./config/hooks.json",
  "mcpServers": "./mcp-config.json",
  "outputStyles": "./styles/",
  "lspServers": "./.lsp.json",
  "experimental": {
    "themes": "./themes/",
    "monitors": "./monitors.json"
  },
  "dependencies": [
    "helper-lib",
    { "name": "secrets-vault", "version": "~2.1.0" }
  ]
}
```

<h3 id="required-fields">
  Campos obrigatórios
</h3>

Se você incluir um manifesto, `name` é o único campo obrigatório.

| Campo  | Tipo   | Descrição                                                                                                                                                                                                                                      | Exemplo              |
| :----- | :----- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------------------- |
| `name` | string | Identificador único (kebab-case, sem espaços). Quando uma [entrada do marketplace](https://code.claude.com/docs/pt/plugin-marketplaces#plugin-entries) lista o plugin com um nome diferente, o nome da entrada do marketplace é o que `enabledPlugins` usa e `/plugin` usa | `"deployment-tools"` |

Este nome é usado para namespacing de componentes. Por exemplo, na UI, o agent `agent-creator` para o plugin com nome `plugin-dev` aparecerá como `plugin-dev:agent-creator`.

<h3 id="unrecognized-fields">
  Campos não reconhecidos
</h3>

Claude Code ignora campos de nível superior que não reconhece. Você pode manter metadados de outro ecossistema em `plugin.json` e o plugin ainda carrega. Isso torna prático manter um manifesto que funciona como um manifesto de extensão VS Code ou Cursor, um `package.json` npm, ou um manifesto de pacote MCPB/DXT.

`claude plugin validate` relata campos não reconhecidos como avisos, não erros. Se um campo está um ou dois caracteres diferente de um reconhecido, o aviso sugere o nome provavelmente pretendido. Um plugin com apenas avisos de campo não reconhecido ainda passa na validação e carrega em tempo de execução.

Campos com o tipo errado ainda falham. Por exemplo, um valor `keywords` que é uma string em vez de um array é um erro de carregamento, e `claude plugin validate` o relata como tal.

Passe `--strict` para tratar avisos como erros. Use-o em CI para detectar um nome de campo digitado incorretamente ou um campo deixado de outro manifesto de ferramenta antes de publicar, mesmo que o plugin carregasse em tempo de execução.

```bash theme={null}
claude plugin validate ./my-plugin --strict
```

<h3 id="metadata-fields">
  Campos de metadados
</h3>

| Campo            | Tipo    | Descrição                                                                                                                                                                                                                                                                                                                                                                                | Exemplo                                                           |
| :--------------- | :------ | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :---------------------------------------------------------------- |
| `$schema`        | string  | URL do JSON Schema para autocomplete e validação do editor. Claude Code ignora este campo no momento do carregamento.                                                                                                                                                                                                                                                                    | `"https://json.schemastore.org/claude-code-plugin-manifest.json"` |
| `displayName`    | string  | Nome legível por humanos mostrado no seletor `/plugin` e outras superfícies de UI. Volta para `name` quando omitido. Ao contrário de `name`, pode conter espaços e qualquer capitalização. Não é usado para namespacing ou busca. Requer Claude Code v2.1.143 ou posterior.                                                                                                              | `"Deployment Tools"`                                              |
| `version`        | string  | Opcional. Versão semântica. Definir isso fixa o plugin para essa string de versão, então os usuários só recebem atualizações quando você a incrementa. Se omitido, Claude Code volta para o SHA do commit git, então cada commit é tratado como uma nova versão. Se também definido na entrada do marketplace, `plugin.json` vence. Veja [Gerenciamento de versão](#version-management). | `"2.1.0"`                                                         |
| `description`    | string  | Explicação breve do propósito do plugin                                                                                                                                                                                                                                                                                                                                                  | `"Deployment automation tools"`                                   |
| `author`         | object  | Informações do autor                                                                                                                                                                                                                                                                                                                                                                     | `{"name": "Dev Team", "email": "dev@company.com"}`                |
| `homepage`       | string  | URL de documentação                                                                                                                                                                                                                                                                                                                                                                      | `"https://docs.example.com"`                                      |
| `repository`     | string  | URL do código-fonte                                                                                                                                                                                                                                                                                                                                                                      | `"https://github.com/user/plugin"`                                |
| `license`        | string  | Identificador de licença                                                                                                                                                                                                                                                                                                                                                                 | `"MIT"`, `"Apache-2.0"`                                           |
| `keywords`       | array   | Tags de descoberta                                                                                                                                                                                                                                                                                                                                                                       | `["deployment", "ci-cd"]`                                         |
| `defaultEnabled` | boolean | Se o plugin inicia em um estado habilitado quando o usuário não definiu um. Padrão é `true`. Veja [Habilitação padrão](#default-enablement). Requer Claude Code v2.1.154 ou posterior.                                                                                                                                                                                                   | `false`                                                           |

<h3 id="default-enablement">
  Habilitação padrão
</h3>

Defina `defaultEnabled: false` em `plugin.json` para enviar um plugin que instala desabilitado. O usuário o ativa com `claude plugin enable <plugin>` ou a interface `/plugin`. Use isso para plugins que adicionam custo ou escopo que um usuário deve optar por usar, como um que se conecta a um serviço externo. Isso requer Claude Code v2.1.154 ou posterior. Versões anteriores ignoram o campo e habilitam o plugin na instalação.

`defaultEnabled` é o fallback quando nada mais decidiu o estado do plugin. Duas coisas têm precedência sobre ele:

* **A configuração do usuário**: uma entrada para o plugin em `enabledPlugins` em qualquer escopo de configurações. Uma vez escrita, persiste entre atualizações e reinstalações de plugin, então mudar `defaultEnabled` em uma versão posterior não inverte um usuário existente.
* **Um requisito de dependência**: quando um plugin é necessário por outro que está ativo, Claude Code escreve `true` para ele no momento da instalação ou habilitação. Isso lhe dá uma configuração explícita, então seu próprio padrão não se aplica mais. Veja [Habilitar ou desabilitar um plugin com dependências](https://code.claude.com/docs/pt/plugin-dependencies#enable-or-disable-a-plugin-with-dependencies).

O mesmo campo pode aparecer na entrada do marketplace de um plugin, onde tem precedência sobre o valor em `plugin.json`. Veja [Campos de plugin opcionais](https://code.claude.com/docs/pt/plugin-marketplaces#optional-plugin-fields).

<h3 id="component-path-fields">
  Campos de caminho de componente
</h3>

| Campo                   | Tipo                  | Descrição                                                                                                                                                                                          | Exemplo                                              |
| :---------------------- | :-------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------- |
| `skills`                | string\|array         | Diretórios de skill personalizados contendo `<name>/SKILL.md`. Adiciona ao padrão `skills/`. Veja [Regras de comportamento de caminho](#path-behavior-rules) para a exceção de raiz do marketplace | `"./custom/skills/"`                                 |
| `commands`              | string\|array         | Arquivos de skill `.md` planos personalizados ou diretórios (substitui padrão `commands/`)                                                                                                         | `"./custom/cmd.md"` ou `["./cmd1.md"]`               |
| `agents`                | string\|array         | Arquivos de agent personalizados (substitui padrão `agents/`)                                                                                                                                      | `"./custom/agents/reviewer.md"`                      |
| `hooks`                 | string\|array\|object | Caminhos de configuração de hooks ou configuração inline                                                                                                                                           | `"./my-extra-hooks.json"`                            |
| `mcpServers`            | string\|array\|object | Caminhos de configuração MCP ou configuração inline                                                                                                                                                | `"./my-extra-mcp-config.json"`                       |
| `outputStyles`          | string\|array         | Arquivos/diretórios de estilo de saída personalizados (substitui padrão `output-styles/`)                                                                                                          | `"./styles/"`                                        |
| `lspServers`            | string\|array\|object | Configurações [Language Server Protocol](https://microsoft.github.io/language-server-protocol/) para inteligência de código (ir para definição, encontrar referências, etc.)                       | `"./.lsp.json"`                                      |
| `experimental.themes`   | string\|array         | Arquivos/diretórios de tema de cor (substitui padrão `themes/`). Veja [Temas](#themes)                                                                                                             | `"./themes/"`                                        |
| `experimental.monitors` | string\|array         | Configurações de [Monitor](https://code.claude.com/docs/pt/tools-reference#monitor-tool) de fundo que iniciam automaticamente quando o plugin está ativo. Veja [Monitors](#monitors)                                           | `"./monitors.json"`                                  |
| `userConfig`            | object                | Valores configuráveis pelo usuário solicitados no momento da habilitação. Veja [Configuração do usuário](#user-configuration)                                                                      | Veja abaixo                                          |
| `channels`              | array                 | Declarações de canal para injeção de mensagens (estilo Telegram, Slack, Discord). Veja [Canais](#channels)                                                                                         | Veja abaixo                                          |
| `dependencies`          | array                 | Outros plugins que este plugin requer, opcionalmente com restrições de versão semver. Veja [Restringir versões de dependência de plugin](https://code.claude.com/docs/pt/plugin-dependencies)                                  | `[{ "name": "secrets-vault", "version": "~2.1.0" }]` |

<h3 id="experimental-components">
  Componentes experimentais
</h3>

Componentes sob a chave `experimental`, `themes` e `monitors`, têm um esquema de manifesto que pode mudar entre versões enquanto se estabilizam. Onde você os declara é uma migração separada: o nível superior ainda funciona, `claude plugin validate` avisa, e uma versão futura exigirá `experimental.*`.

<h3 id="user-configuration">
  Configuração do usuário
</h3>

O campo `userConfig` declara valores que Claude Code solicita ao usuário quando o plugin é habilitado. Use isso em vez de exigir que os usuários editem manualmente `settings.json`.

```json theme={null}
{
  "userConfig": {
    "api_endpoint": {
      "type": "string",
      "title": "API endpoint",
      "description": "O endpoint de API da sua equipe"
    },
    "api_token": {
      "type": "string",
      "title": "API token",
      "description": "Token de autenticação de API",
      "sensitive": true
    }
  }
}
```

As chaves devem ser identificadores válidos. Cada opção suporta estes campos:

| Campo         | Obrigatório | Descrição                                                                                       |
| :------------ | :---------- | :---------------------------------------------------------------------------------------------- |
| `type`        | Sim         | Um de `string`, `number`, `boolean`, `directory`, ou `file`                                     |
| `title`       | Sim         | Rótulo mostrado no diálogo de configuração                                                      |
| `description` | Sim         | Texto de ajuda mostrado abaixo do campo                                                         |
| `sensitive`   | Não         | Se `true`, mascara entrada e armazena o valor em armazenamento seguro em vez de `settings.json` |
| `required`    | Não         | Se `true`, validação falha quando o campo está vazio                                            |
| `default`     | Não         | Valor usado quando o usuário não fornece nada                                                   |
| `multiple`    | Não         | Para tipo `string`, permite um array de strings                                                 |
| `min` / `max` | Não         | Limites para tipo `number`                                                                      |

Cada valor está disponível para substituição como `${user_config.KEY}` em configurações de servidor MCP e LSP e comandos de hook. Valores não sensíveis também podem ser substituídos em conteúdo de skill e agent. Todos os valores são exportados para processos de hook como variáveis de ambiente `CLAUDE_PLUGIN_OPTION_<KEY>`, onde `<KEY>` é a chave de opção em maiúsculas.

Campos que executam em um shell rejeitam `${user_config.*}`: substituir um valor configurado em um comando shell deixaria o shell executar o que quer que esse valor contenha, então o componente falha com um [erro](https://code.claude.com/docs/pt/errors#plugin-command-references-user-config) em vez disso. Cada campo rejeitado tem uma forma alternativa de passar o valor:

| Campo rejeitado                                                              | Como passar o valor                                                                                                       |
| :--------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------ |
| Comandos de hook de forma shell                                              | Use [forma exec](https://code.claude.com/docs/pt/hooks#exec-form-and-shell-form) com `args`, ou leia `CLAUDE_PLUGIN_OPTION_<KEY>` do ambiente do hook |
| Comandos de [Monitor](#monitors)                                             | Leia o valor de um arquivo de configuração no script                                                                      |
| MCP [`headersHelper`](https://code.claude.com/docs/pt/mcp#use-dynamic-headers-for-custom-authentication) | Leia o valor de um arquivo de configuração no script                                                                      |

Antes de v2.1.207, esses campos substituíam valores `${user_config.KEY}`; atualize plugins que dependiam disso.

Valores não sensíveis são armazenados sob a chave [`pluginConfigs`](https://code.claude.com/docs/pt/settings#pluginconfigs) em `settings.json` como `pluginConfigs[<plugin-id>].options`. Claude Code escreve a chave para configurações do usuário e a lê de volta de configurações do usuário, a flag `--settings` e configurações gerenciadas apenas; entradas em `.claude/settings.json` ou `.claude/settings.local.json` de um projeto são ignoradas. Antes de v2.1.207, Claude Code também lia configurações de projeto e local.

Valores sensíveis vão para o Keychain do macOS, ou para `~/.claude/.credentials.json` em plataformas onde nenhum keychain suportado está disponível. O armazenamento em keychain é compartilhado com tokens OAuth e tem um limite total aproximado de 2 KB, então mantenha valores sensíveis pequenos.

<h3 id="channels">
  Canais
</h3>

O campo `channels` permite que um plugin declare um ou mais canais de mensagem que injetam conteúdo na conversa. Cada canal se vincula a um servidor MCP que o plugin fornece.

```json theme={null}
{
  "channels": [
    {
      "server": "telegram",
      "userConfig": {
        "bot_token": {
          "type": "string",
          "title": "Bot token",
          "description": "Token do bot Telegram",
          "sensitive": true
        },
        "owner_id": {
          "type": "string",
          "title": "Owner ID",
          "description": "Seu ID de usuário Telegram"
        }
      }
    }
  ]
}
```

O campo `server` é obrigatório e deve corresponder a uma chave em `mcpServers` do plugin. O `userConfig` opcional por canal usa o mesmo esquema que o campo de nível superior, permitindo que o plugin solicite tokens de bot ou IDs de proprietário quando o plugin é habilitado.

<h3 id="path-behavior-rules">
  Regras de comportamento de caminho
</h3>

Se um caminho personalizado substitui ou estende o diretório padrão do plugin depende do campo:

* **Substitui o padrão**: `commands`, `agents`, `outputStyles`, `experimental.themes`, `experimental.monitors`. Por exemplo, quando o manifesto especifica `commands`, o diretório padrão `commands/` não é verificado. Para manter o padrão e adicionar mais, liste-o explicitamente: `"commands": ["./commands/", "./extras/"]`
* **Adiciona ao padrão**: `skills`. O diretório padrão `skills/` é sempre verificado, e diretórios listados em `skills` são carregados junto com ele. Exceção: para uma [entrada do marketplace cuja `source` resolve para a raiz do marketplace](https://code.claude.com/docs/pt/plugin-marketplaces#advanced-plugin-entries), declarar subdiretórios específicos substitui a verificação padrão `skills/`
* **Regras de mesclagem próprias**: [hooks](#hooks), [MCP servers](#mcp-servers) e [LSP servers](#lsp-servers). Veja cada seção para como múltiplas fontes se combinam

Quando um plugin tem tanto uma pasta padrão quanto a chave de manifesto correspondente, Claude Code v2.1.140 e posterior sinaliza a pasta ignorada em `claude plugin list` e a visualização de detalhes `/plugin`. O plugin ainda carrega usando os caminhos do manifesto. Claude Code não avisa quando a chave de manifesto aponta para a pasta padrão, por exemplo `"commands": ["./commands/deploy.md"]`, porque esse caminho nomeia a pasta explicitamente.

Para todos os campos de caminho:

* Todos os caminhos devem ser relativos à raiz do plugin e começar com `./`
* Componentes de caminhos personalizados usam as mesmas regras de nomenclatura e namespacing
* Múltiplos caminhos podem ser especificados como arrays
* Quando um caminho de skill aponta para um diretório que contém um `SKILL.md` diretamente, por exemplo `"skills": ["./"]` apontando para a raiz do plugin, o campo frontmatter `name` em `SKILL.md` determina o nome de invocação da skill. Isso fornece um nome estável independentemente do diretório de instalação. Se `name` não estiver definido no frontmatter, o nome base do diretório é usado como fallback.

Um plugin que tem um `SKILL.md` em sua raiz, nenhum subdiretório `skills/`, e nenhum campo de manifesto `skills` é carregado automaticamente como um plugin de skill único em Claude Code v2.1.142 e posterior. Você não precisa definir `"skills": ["./"]` em `plugin.json` para este layout. O nome de invocação da skill segue a mesma regra acima: o campo frontmatter `name`, ou o nome base do diretório como fallback.

**Exemplos de caminho**:

```json theme={null}
{
  "commands": [
    "./specialized/deploy.md",
    "./utilities/batch-process.md"
  ],
  "agents": [
    "./custom-agents/reviewer.md",
    "./custom-agents/tester.md"
  ]
}
```

<h3 id="environment-variables">
  Variáveis de ambiente
</h3>

Claude Code fornece três variáveis para referenciar caminhos:

| Variável                | Resolve para                                                                                                              | Use para                                                                                         |
| :---------------------- | :------------------------------------------------------------------------------------------------------------------------ | :----------------------------------------------------------------------------------------------- |
| `${CLAUDE_PLUGIN_ROOT}` | Caminho absoluto para o diretório de instalação do plugin                                                                 | Scripts, binários e arquivos de configuração agrupados com o plugin                              |
| `${CLAUDE_PLUGIN_DATA}` | [Diretório persistente](#persistent-data-directory) que sobrevive a atualizações de plugin, criado na primeira referência | Dependências instaladas como `node_modules` ou ambientes virtuais Python, código gerado e caches |
| `${CLAUDE_PROJECT_DIR}` | A raiz do projeto                                                                                                         | Scripts e arquivos de configuração locais do projeto                                             |

Todos os três são exportados como variáveis de ambiente para processos de hook e para subprocessos de servidor MCP e LSP. Quais campos substituem-nos inline depende do componente do plugin:

| Componente do plugin               | Campos onde placeholders resolvem            |
| :--------------------------------- | :------------------------------------------- |
| Conteúdo de skill e agent          | Em qualquer lugar onde o placeholder aparece |
| Comandos de hook e monitor         | Em qualquer lugar onde o placeholder aparece |
| Servidores MCP `stdio`             | `command`, `args`, `env`                     |
| Servidores MCP `http`, `sse`, `ws` | `url`, `headers`, `headersHelper`            |
| Servidores LSP                     | `command`, `args`, `env`, `workspaceFolder`  |

Em comandos de hook, use [forma exec](https://code.claude.com/docs/pt/hooks#exec-form-and-shell-form) com `args` para que cada caminho seja passado como um argumento sem citação. Em hooks de forma shell e comandos de monitor, envolva as variáveis em aspas duplas, como em `"${CLAUDE_PROJECT_DIR}/scripts/server.sh"`. Este hook de forma shell executa um script agrupado com um plugin:

```json theme={null}
{
  "hooks": {
    "PostToolUse": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "\"${CLAUDE_PLUGIN_ROOT}\"/scripts/process.sh"
          }
        ]
      }
    ]
  }
}
```

`${CLAUDE_PLUGIN_ROOT}` muda quando o plugin é atualizado. O diretório da versão anterior permanece no disco por aproximadamente sete dias após uma atualização antes da limpeza, mas trate-o como efêmero e não escreva estado lá.

Quando um plugin é atualizado no meio de uma sessão, comandos de hook, monitors, servidores MCP e servidores LSP continuam usando o caminho da versão anterior. Execute `/reload-plugins` para alternar hooks, servidores MCP e servidores LSP para o novo caminho; monitors requerem uma reinicialização de sessão.

Servidores MCP também podem chamar a solicitação `roots/list` para ler os diretórios de trabalho da sessão em tempo de execução. Veja [o que `roots/list` retorna e quando Claude Code notifica o servidor de mudanças](https://code.claude.com/docs/pt/mcp#option-3-add-a-local-stdio-server).

<h4 id="persistent-data-directory">
  Diretório de dados persistente
</h4>

O diretório `${CLAUDE_PLUGIN_DATA}` resolve para `~/.claude/plugins/data/{id}/`, onde `{id}` é o identificador do plugin com caracteres fora de `a-z`, `A-Z`, `0-9`, `_` e `-` substituídos por `-`. Para um plugin instalado como `formatter@my-marketplace`, o diretório é `~/.claude/plugins/data/formatter-my-marketplace/`.

Um uso comum é instalar dependências de linguagem uma vez e reutilizá-las em sessões e atualizações de plugin. Como o diretório de dados sobrevive a qualquer versão única de plugin, uma verificação de existência de diretório sozinha não pode detectar quando uma atualização muda o manifesto de dependência do plugin. O padrão recomendado compara o manifesto agrupado contra uma cópia no diretório de dados e reinstala quando diferem.

Este hook `SessionStart` instala `node_modules` na primeira execução e novamente sempre que uma atualização de plugin inclui um `package.json` alterado:

```json theme={null}
{
  "hooks": {
    "SessionStart": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "diff -q \"${CLAUDE_PLUGIN_ROOT}/package.json\" \"${CLAUDE_PLUGIN_DATA}/package.json\" >/dev/null 2>&1 || (cd \"${CLAUDE_PLUGIN_DATA}\" && cp \"${CLAUDE_PLUGIN_ROOT}/package.json\" . && npm install) || rm -f \"${CLAUDE_PLUGIN_DATA}/package.json\""
          }
        ]
      }
    ]
  }
}
```

O `diff` sai com código diferente de zero quando a cópia armazenada está faltando ou difere da agrupada, cobrindo tanto a primeira execução quanto atualizações que mudam dependências. Se `npm install` falhar, o `rm` final remove o manifesto copiado para que a próxima sessão tente novamente.

Scripts agrupados em `${CLAUDE_PLUGIN_ROOT}` podem então executar contra o `node_modules` persistido:

```json theme={null}
{
  "mcpServers": {
    "routines": {
      "command": "node",
      "args": ["${CLAUDE_PLUGIN_ROOT}/server.js"],
      "env": {
        "NODE_PATH": "${CLAUDE_PLUGIN_DATA}/node_modules"
      }
    }
  }
}
```

O diretório de dados é deletado automaticamente quando você desinstala o plugin do último escopo onde está instalado. A interface `/plugin` mostra o tamanho do diretório e solicita confirmação antes de deletar. O CLI deleta por padrão; passe [`--keep-data`](#plugin-uninstall) para preservá-lo.

***

<h2 id="plugin-caching-and-file-resolution">
  Cache de plugin e resolução de arquivo
</h2>

Os plugins são especificados de uma de duas maneiras:

* Através de `claude --plugin-dir` ou `claude --plugin-url`, pela duração de uma sessão.
* Através de um marketplace, instalado para sessões futuras.

Para fins de segurança e verificação, Claude Code copia plugins do *marketplace* para o **cache de plugin** local do usuário (`~/.claude/plugins/cache`) em vez de usá-los no local. Entender esse comportamento é importante ao desenvolver plugins que referenciam arquivos externos.

Cada versão instalada é um diretório separado no cache. Quando você atualiza ou desinstala um plugin, o diretório de versão anterior é marcado como órfão e removido automaticamente 7 dias depois. O período de carência permite que sessões Claude Code concorrentes que já carregaram a versão antiga continuem funcionando sem erros.

As ferramentas Glob e Grep de Claude pulam diretórios de versão órfã durante buscas, então resultados de arquivo não incluem código de plugin desatualizado.

<h3 id="path-traversal-limitations">
  Limitações de travessia de caminho
</h3>

Plugins instalados não podem referenciar arquivos fora de seu diretório. Caminhos que atravessam fora da raiz do plugin (como `../shared-utils`) não funcionarão após a instalação porque esses arquivos externos não são copiados para o cache.

<h3 id="share-files-within-a-marketplace-with-symlinks">
  Compartilhar arquivos dentro de um marketplace com symlinks
</h3>

Se seu plugin precisa compartilhar arquivos com outras partes do mesmo marketplace, você pode criar links simbólicos dentro de seu diretório de plugin. Como um symlink é tratado quando o plugin é copiado para o cache depende de onde seu alvo é resolvido:

* **Dentro do próprio diretório do plugin:** o symlink é preservado como um symlink relativo no cache, então ele continua resolvendo para o alvo copiado em tempo de execução.
* **Em outro lugar dentro do mesmo marketplace:** o symlink é desreferenciado. O conteúdo do alvo é copiado para o cache em seu lugar. Isso permite que o diretório `skills/` de um meta-plugin seja vinculado a skills definidas por outros plugins no marketplace.
* **Fora do marketplace:** o symlink é ignorado por segurança. Isso impede que plugins puxem arquivos arbitrários do host, como caminhos do sistema, para o cache.

Para plugins instalados com `--plugin-dir` ou de um caminho local, apenas symlinks que são resolvidos dentro do próprio diretório do plugin são preservados. Todos os outros são ignorados.

O seguinte comando cria um link de dentro de um plugin do marketplace para uma skill compartilhada definida por um plugin irmão. No Windows, use `mklink /D` de um Prompt de Comando elevado ou ative o Modo de Desenvolvedor:

```bash theme={null}
ln -s ../../shared-plugin/skills/foo ./skills/foo
```

Isso fornece flexibilidade enquanto mantém os benefícios de segurança do sistema de cache.

***

<h2 id="plugin-directory-structure">
  Estrutura de diretório de plugin
</h2>

<h3 id="standard-plugin-layout">
  Layout de plugin padrão
</h3>

Um plugin completo segue esta estrutura:

```text theme={null}
enterprise-plugin/
├── .claude-plugin/           # Diretório de metadados (opcional)
│   └── plugin.json             # manifesto de plugin
├── skills/                   # Skills
│   ├── code-reviewer/
│   │   └── SKILL.md
│   └── pdf-processor/
│       ├── SKILL.md
│       └── scripts/
├── commands/                 # Skills como arquivos .md planos
│   ├── status.md
│   └── logs.md
├── agents/                   # Definições de subagent
│   ├── security-reviewer.md
│   ├── performance-tester.md
│   └── compliance-checker.md
├── output-styles/            # Definições de estilo de saída
│   └── terse.md
├── themes/                   # Definições de tema de cor
│   └── dracula.json
├── monitors/                 # Configurações de monitor de fundo
│   └── monitors.json
├── hooks/                    # Configurações de hook
│   ├── hooks.json           # Configuração de hook principal
│   └── security-hooks.json  # Hooks adicionais
├── bin/                      # Executáveis de plugin adicionados a PATH
│   └── my-tool               # Invocável como comando bare na ferramenta Bash
├── settings.json            # Configurações padrão para o plugin
├── .mcp.json                # Definições de servidor MCP
├── .lsp.json                # Configurações de servidor LSP
├── scripts/                 # Scripts de hook e utilitário
│   ├── security-scan.sh
│   ├── format-code.py
│   └── deploy.js
├── LICENSE                  # Arquivo de licença
└── CHANGELOG.md             # Histórico de versão
```

<Warning>
  O diretório `.claude-plugin/` contém o arquivo `plugin.json`. Todos os outros diretórios (commands/, agents/, skills/, output-styles/, themes/, monitors/, hooks/) devem estar na raiz do plugin, não dentro de `.claude-plugin/`.
</Warning>

Um arquivo `CLAUDE.md` na raiz do plugin não é carregado como contexto do projeto. Os plugins contribuem contexto através de skills, agents e hooks em vez de CLAUDE.md. Para enviar instruções que sejam carregadas no contexto do Claude, coloque-as em uma [skill](#skills).

<h3 id="file-locations-reference">
  Referência de localizações de arquivo
</h3>

| Componente        | Localização padrão           | Propósito                                                                                                                                                                                      |
| :---------------- | :--------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Manifesto**     | `.claude-plugin/plugin.json` | Metadados e configuração de plugin (opcional)                                                                                                                                                  |
| **Skills**        | `skills/`                    | Skills com estrutura `<name>/SKILL.md`                                                                                                                                                         |
| **Commands**      | `commands/`                  | Skills como arquivos Markdown planos. Use `skills/` para novos plugins                                                                                                                         |
| **Agents**        | `agents/`                    | Arquivos Markdown de Subagent                                                                                                                                                                  |
| **Output styles** | `output-styles/`             | Definições de estilo de saída                                                                                                                                                                  |
| **Themes**        | `themes/`                    | Definições de tema de cor                                                                                                                                                                      |
| **Hooks**         | `hooks/hooks.json`           | Configuração de hook                                                                                                                                                                           |
| **MCP servers**   | `.mcp.json`                  | Definições de servidor MCP                                                                                                                                                                     |
| **LSP servers**   | `.lsp.json`                  | Configurações de servidor de linguagem                                                                                                                                                         |
| **Monitors**      | `monitors/monitors.json`     | Configurações de monitor de fundo                                                                                                                                                              |
| **Executáveis**   | `bin/`                       | Executáveis adicionados ao `PATH` da ferramenta Bash. Arquivos aqui são invocáveis como comandos bare em qualquer chamada de ferramenta Bash enquanto o plugin está habilitado                 |
| **Configurações** | `settings.json`              | Configuração padrão aplicada quando o plugin é habilitado. Atualmente apenas as chaves [`agent`](https://code.claude.com/docs/pt/sub-agents) e [`subagentStatusLine`](https://code.claude.com/docs/pt/statusline#subagent-status-lines) são suportadas |

***

<h2 id="cli-commands-reference">
  Referência de comandos CLI
</h2>

Claude Code fornece comandos CLI para gerenciamento de plugin não interativo, útil para scripting e automação.

<h3 id="plugin-init">
  plugin init
</h3>

Crie um novo plugin em `~/.claude/skills/<name>/`. Na próxima sessão do Claude Code, ele carrega automaticamente como `<name>@skills-dir` e aparece em `/plugin` e `claude plugin list` sem etapa de instalação.

Veja [Plugins de diretório de skills](#skills-directory-plugins) para requisitos de escopo e confiança.

```bash theme={null}
claude plugin init <name> [options]
```

**Argumentos:**

* `<name>`: Nome do plugin. Torna-se o namespace de skill e o nome do diretório sob `~/.claude/skills/`, então não pode conter espaços ou separadores de caminho.

**Opções:**

| Opção                    | Descrição                                                                                                                 | Padrão                  |
| :----------------------- | :------------------------------------------------------------------------------------------------------------------------ | :---------------------- |
| `--description <text>`   | Descrição do manifesto                                                                                                    |                         |
| `--author <name>`        | Nome do autor                                                                                                             | `git config user.name`  |
| `--author-email <email>` | Email do autor                                                                                                            | `git config user.email` |
| `--with <components...>` | Também criar pastas de componentes. Valores válidos: `skills`, `agents`, `hooks`, `mcp`, `lsp`, `output-style`, `channel` |                         |
| `-f, --force`            | Sobrescrever um `.claude-plugin/` existente no alvo                                                                       |                         |
| `-h, --help`             | Exibir ajuda para comando                                                                                                 |                         |

**Aliases:** `new`

Cada valor `--with` adiciona um arquivo inicial para esse componente, pronto para editar:

| Componente     | O que cria                                                                                                    |
| :------------- | :------------------------------------------------------------------------------------------------------------ |
| `skills`       | Uma skill extra nomeada `<name>:example` ao lado da padrão                                                    |
| `agents`       | Uma definição de subagent `agents/`                                                                           |
| `hooks`        | Um `hooks/hooks.json` com um manipulador de evento de amostra                                                 |
| `mcp`          | Um `.mcp.json` com exemplos de servidor HTTP e stdio                                                          |
| `lsp`          | Um exemplo `.lsp.json` de language-server                                                                     |
| `output-style` | Um `output-styles/<name>.md` que se aplica automaticamente enquanto o plugin está habilitado                  |
| `channel`      | Um [canal](https://code.claude.com/docs/pt/channels) baseado em MCP: um servidor stdio (`server.ts`), seu `.mcp.json` e um `package.json` |

O plugin criado usa a fonte `@skills-dir` em vez de um marketplace. Administradores podem bloquear essa fonte com `strictKnownMarketplaces` ou adicionando `{"source": "skills-dir"}` a `blockedMarketplaces` em [configurações gerenciadas](https://code.claude.com/docs/pt/plugin-marketplaces#managed-marketplace-restrictions). Quando bloqueado, `plugin init` falha antes de escrever.

**Exemplos:**

```bash theme={null}
# Criar um plugin mínimo
claude plugin init my-helper

# Criar com pastas de skill e hook
claude plugin init my-helper --with skills hooks

# Sobrescrever um scaffold existente
claude plugin init my-helper --force
```

<h3 id="plugin-install">
  plugin install
</h3>

Instale um plugin dos marketplaces disponíveis.

```bash theme={null}
claude plugin install <plugin> [options]
```

**Argumentos:**

* `<plugin>`: Nome do plugin ou `plugin-name@marketplace-name` para um marketplace específico

**Opções:**

| Opção                 | Descrição                                           | Padrão |
| :-------------------- | :-------------------------------------------------- | :----- |
| `-s, --scope <scope>` | Escopo de instalação: `user`, `project`, ou `local` | `user` |
| `-h, --help`          | Exibir ajuda para comando                           |        |

O escopo determina qual arquivo de configurações o plugin instalado é adicionado. Por exemplo, `--scope project` escreve em `enabledPlugins` em .claude/settings.json, tornando o plugin disponível para todos que clonam o repositório do projeto.

**Exemplos:**

```bash theme={null}
# Instalar em escopo de usuário (padrão)
claude plugin install formatter@my-marketplace

# Instalar em escopo de projeto (compartilhado com equipe)
claude plugin install formatter@my-marketplace --scope project

# Instalar em escopo local (gitignored)
claude plugin install formatter@my-marketplace --scope local
```

<h3 id="plugin-uninstall">
  plugin uninstall
</h3>

Remova um plugin instalado.

```bash theme={null}
claude plugin uninstall <plugin> [options]
```

**Argumentos:**

* `<plugin>`: Nome do plugin ou `plugin-name@marketplace-name`

**Opções:**

| Opção                 | Descrição                                                                                                      | Padrão |
| :-------------------- | :------------------------------------------------------------------------------------------------------------- | :----- |
| `-s, --scope <scope>` | Desinstalar do escopo: `user`, `project`, ou `local`                                                           | `user` |
| `--keep-data`         | Preservar o [diretório de dados persistente](#persistent-data-directory) do plugin                             |        |
| `--prune`             | Também remover dependências auto-instaladas que nenhum outro plugin requer. Veja [plugin prune](#plugin-prune) |        |
| `-y, --yes`           | Pular o prompt de confirmação `--prune`. Necessário quando stdin ou stdout não é um TTY                        |        |
| `-h, --help`          | Exibir ajuda para comando                                                                                      |        |

**Aliases:** `remove`, `rm`

Por padrão, desinstalar do último escopo restante também deleta o diretório `${CLAUDE_PLUGIN_DATA}` do plugin. Use `--keep-data` para preservá-lo, por exemplo ao reinstalar após testar uma nova versão.

<h3 id="plugin-prune">
  plugin prune
</h3>

Remova dependências de plugin auto-instaladas que não são mais necessárias por nenhum plugin instalado. Dependências que Claude Code puxou para satisfazer o campo [`dependencies`](https://code.claude.com/docs/pt/plugin-dependencies) de outro plugin são removidas; plugins que você instalou diretamente nunca são tocados.

```bash theme={null}
claude plugin prune [options]
```

**Opções:**

| Opção                 | Descrição                                                                     | Padrão |
| :-------------------- | :---------------------------------------------------------------------------- | :----- |
| `-s, --scope <scope>` | Limpar no escopo: `user`, `project`, ou `local`                               | `user` |
| `--dry-run`           | Listar o que seria removido sem remover nada                                  |        |
| `-y, --yes`           | Pular o prompt de confirmação. Necessário quando stdin ou stdout não é um TTY |        |
| `-h, --help`          | Exibir ajuda para comando                                                     |        |

**Aliases:** `autoremove`

O comando lista dependências órfãs e pede confirmação antes de removê-las. Para remover um plugin e limpar suas dependências em uma etapa, execute `claude plugin uninstall <plugin> --prune`.

<Note>
  `claude plugin prune` requer Claude Code v2.1.121 ou posterior.
</Note>

<h3 id="plugin-enable">
  plugin enable
</h3>

Habilite um plugin desabilitado. Se o plugin declara [dependências](https://code.claude.com/docs/pt/plugin-dependencies), Claude Code as habilita transitivamente no mesmo escopo, e o comando falha quando uma dependência não está instalada.

```bash theme={null}
claude plugin enable <plugin> [options]
```

**Argumentos:**

* `<plugin>`: Nome do plugin ou `plugin-name@marketplace-name`

**Opções:**

| Opção                 | Descrição                                            | Padrão |
| :-------------------- | :--------------------------------------------------- | :----- |
| `-s, --scope <scope>` | Escopo para habilitar: `user`, `project`, ou `local` | `user` |
| `-h, --help`          | Exibir ajuda para comando                            |        |

<h3 id="plugin-disable">
  plugin disable
</h3>

Desabilite um plugin sem desinstalá-lo. Falha quando outro plugin habilitado [depende de](https://code.claude.com/docs/pt/plugin-dependencies#enable-or-disable-a-plugin-with-dependencies) o alvo. A mensagem de erro inclui um comando encadeado que desabilita cada dependente primeiro.

```bash theme={null}
claude plugin disable <plugin> [options]
```

**Argumentos:**

* `<plugin>`: Nome do plugin ou `plugin-name@marketplace-name`

**Opções:**

| Opção                 | Descrição                                              | Padrão |
| :-------------------- | :----------------------------------------------------- | :----- |
| `-s, --scope <scope>` | Escopo para desabilitar: `user`, `project`, ou `local` | `user` |
| `-h, --help`          | Exibir ajuda para comando                              |        |

<h3 id="plugin-update">
  plugin update
</h3>

Atualize um plugin para a versão mais recente.

```bash theme={null}
claude plugin update <plugin> [options]
```

**Argumentos:**

* `<plugin>`: Nome do plugin ou `plugin-name@marketplace-name`

**Opções:**

| Opção                 | Descrição                                                       | Padrão |
| :-------------------- | :-------------------------------------------------------------- | :----- |
| `-s, --scope <scope>` | Escopo para atualizar: `user`, `project`, `local`, ou `managed` | `user` |
| `-h, --help`          | Exibir ajuda para comando                                       |        |

***

<h3 id="plugin-list">
  plugin list
</h3>

Liste plugins instalados com sua versão, marketplace de origem e status de habilitação.

```bash theme={null}
claude plugin list [options]
```

**Opções:**

| Opção         | Descrição                                                    | Padrão |
| :------------ | :----------------------------------------------------------- | :----- |
| `--json`      | Saída como JSON                                              |        |
| `--available` | Incluir plugins disponíveis de marketplaces. Requer `--json` |        |
| `-h, --help`  | Exibir ajuda para comando                                    |        |

Dentro de uma sessão interativa, `/plugin list` imprime a mesma listagem inline. O formulário interativo aceita `--enabled` ou `--disabled` para mostrar apenas plugins nesse estado, e `ls` como abreviação para `list`.

<h3 id="plugin-details">
  plugin details
</h3>

Mostre o inventário de componentes de um plugin e o custo de token projetado. A saída lista todos os componentes que o plugin contribui, agrupados como Skills, Agents, Hooks, servidores MCP e servidores LSP, juntamente com uma estimativa de quantos tokens ele adiciona a cada sessão. O grupo Skills inclui entradas tanto de `skills/` quanto de `commands/`.

```bash theme={null}
claude plugin details <name>
```

**Argumentos:**

* `<name>`: Nome do plugin ou `plugin-name@marketplace-name`

**Opções:**

| Opção        | Descrição                 | Padrão |
| :----------- | :------------------------ | :----- |
| `-h, --help` | Exibir ajuda para comando |        |

A saída mostra dois valores de custo para cada componente:

* **Always-on:** tokens adicionados a cada sessão pelo texto de listagem do plugin, como descrições de skill, descrições de agent e nomes de comando, independentemente de qualquer componente disparar.
* **On-invoke:** tokens que um componente custa quando dispara. Mostrado por componente, não como total do plugin, porque uma sessão típica invoca apenas um subconjunto de componentes.

Este exemplo mostra como a saída se parece para um plugin com duas skills:

```
dependency-guard 1.2.0
  Dependency analysis for Claude Code sessions
  Source: dependency-guard@example-marketplace

Component inventory
  Skills (2)  scan-dependencies, review-changes
  Agents (0)
  Hooks (1)  (harness-only — no model context cost)
  MCP servers (0)
  LSP servers (0)

Projected token cost
  Always-on:   ~180 tok   added to every session

Per-component (rounded)
  component            always-on  on-invoke
  scan-dependencies        ~100      ~2400
  review-changes            ~80      ~1800

  On-invoke cost is paid each time a skill or agent fires.
  Token counts are estimates and may differ from actual usage.
```

O total always-on é calculado via API `count_tokens` para seu modelo ativo. Os números por componente são dimensionados proporcionalmente a partir desse total. Se a API estiver inacessível, o comando volta para uma estimativa baseada em caracteres.

<h3 id="plugin-tag">
  plugin tag
</h3>

Crie uma tag git de lançamento para o plugin no diretório atual. Execute de dentro da pasta do plugin. Veja [Tag plugin releases](https://code.claude.com/docs/pt/plugin-dependencies#tag-plugin-releases-for-version-resolution).

```bash theme={null}
claude plugin tag [options]
```

**Opções:**

| Opção         | Descrição                                                                 | Padrão |
| :------------ | :------------------------------------------------------------------------ | :----- |
| `--push`      | Enviar a tag para o remoto após criá-la                                   |        |
| `--dry-run`   | Imprimir o que seria marcado sem criar a tag                              |        |
| `-f, --force` | Criar a tag mesmo que a árvore de trabalho esteja suja ou a tag já exista |        |
| `-h, --help`  | Exibir ajuda para comando                                                 |        |

***

<h2 id="debugging-and-development-tools">
  Ferramentas de depuração e desenvolvimento
</h2>

<h3 id="debugging-commands">
  Comandos de depuração
</h3>

Use `claude --debug` para ver detalhes de carregamento de plugin:

Isso mostra:

* Quais plugins estão sendo carregados
* Quaisquer erros em manifestos de plugin
* Registro de skill, agent e hook
* Inicialização de servidor MCP

<h3 id="common-issues">
  Problemas comuns
</h3>

| Problema                            | Causa                               | Solução                                                                                                                                                                      |
| :---------------------------------- | :---------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Plugin não carregando               | `plugin.json` inválido              | Execute `claude plugin validate` ou `/plugin validate` para verificar `plugin.json`, frontmatter de skill/agent/comando e `hooks/hooks.json` para erros de sintaxe e esquema |
| Skills não aparecendo               | Estrutura de diretório errada       | Garanta `skills/` ou `commands/` na raiz do plugin, não dentro de `.claude-plugin/`                                                                                          |
| Hooks não disparando                | Script não executável               | Execute `chmod +x script.sh`                                                                                                                                                 |
| Servidor MCP falha                  | `${CLAUDE_PLUGIN_ROOT}` ausente     | Use variável para todos os caminhos de plugin                                                                                                                                |
| Erros de caminho                    | Caminhos absolutos usados           | Todos os caminhos devem ser relativos e começar com `./`                                                                                                                     |
| LSP `Executable not found in $PATH` | Servidor de linguagem não instalado | Instale o binário (ex: `npm install -g typescript-language-server typescript`)                                                                                               |

<h3 id="example-error-messages">
  Exemplos de mensagens de erro
</h3>

**Erros de validação de manifesto**:

* `Invalid JSON syntax: Unexpected token } in JSON at position 142`: verificar vírgulas ausentes, vírgulas extras ou strings não citadas
* `Plugin has an invalid manifest file at .claude-plugin/plugin.json. Validation errors: name: Required`: um campo obrigatório está faltando
* `Plugin has a corrupt manifest file at .claude-plugin/plugin.json. JSON parse error: ...`: erro de sintaxe JSON

**Erros de carregamento de plugin**:

* `Warning: No commands found in plugin my-plugin custom directory: ./cmds. Expected .md files or SKILL.md in subdirectories.`: caminho de comando existe mas não contém arquivos de comando válidos
* `Plugin directory not found at path: ./plugins/my-plugin. Check that the marketplace entry has the correct path.`: o caminho `source` em marketplace.json aponta para um diretório inexistente
* `Plugin my-plugin has conflicting manifests: both plugin.json and marketplace entry specify components.`: remover definições de componentes duplicadas ou remover `strict: false` na entrada do marketplace

<h3 id="hook-troubleshooting">
  Solução de problemas de hook
</h3>

**Script de hook não executando**:

1. Verificar se o script é executável: `chmod +x ./scripts/your-script.sh`
2. Verificar a linha shebang: Primeira linha deve ser `#!/bin/bash` ou `#!/usr/bin/env bash`
3. Verificar se o caminho usa `${CLAUDE_PLUGIN_ROOT}`: `"command": "\"${CLAUDE_PLUGIN_ROOT}\"/scripts/your-script.sh"`
4. Testar o script manualmente: `./scripts/your-script.sh`

**Hook não disparando em eventos esperados**:

1. Verificar se o nome do evento está correto (sensível a maiúsculas): `PostToolUse`, não `postToolUse`
2. Verificar se o padrão de matcher corresponde às suas ferramentas: `"matcher": "Write|Edit"` para operações de arquivo
3. Confirmar se o tipo de hook é válido: `command`, `http`, `mcp_tool`, `prompt`, ou `agent`

<h3 id="mcp-server-troubleshooting">
  Solução de problemas de servidor MCP
</h3>

**Servidor não iniciando**:

1. Verificar se o comando existe e é executável
2. Verificar se todos os caminhos usam variável `${CLAUDE_PLUGIN_ROOT}`
3. Verificar os logs do servidor MCP: `claude --debug` mostra erros de inicialização
4. Testar o servidor manualmente fora do Claude Code

**Ferramentas do servidor não aparecendo**:

1. Garantir que o servidor está adequadamente configurado em `.mcp.json` ou `plugin.json`
2. Verificar se o servidor implementa o protocolo MCP corretamente
3. Verificar timeouts de conexão na saída de depuração

<h3 id="directory-structure-mistakes">
  Erros de estrutura de diretório
</h3>

**Sintomas**: Plugin carrega mas componentes (skills, agents, hooks) estão faltando.

**Estrutura correta**: Componentes devem estar na raiz do plugin, não dentro de `.claude-plugin/`. Apenas `plugin.json` pertence em `.claude-plugin/`.

```text theme={null}
my-plugin/
├── .claude-plugin/
│   └── plugin.json      ← Apenas manifesto aqui
├── commands/            ← No nível raiz
├── agents/              ← No nível raiz
└── hooks/               ← No nível raiz
```

Se seus componentes estão dentro de `.claude-plugin/`, mova-os para a raiz do plugin.

**Checklist de depuração**:

1. Executar `claude --debug` e procurar por mensagens "loading plugin"
2. Verificar se cada diretório de componente está listado na saída de depuração
3. Verificar se as permissões de arquivo permitem ler os arquivos de plugin

***

<h2 id="distribution-and-versioning-reference">
  Referência de distribuição e versionamento
</h2>

<h3 id="version-management">
  Gerenciamento de versão
</h3>

Claude Code usa a versão do plugin como a chave de cache que determina se uma atualização está disponível. Quando você executa `/plugin update` ou a atualização automática é acionada, Claude Code calcula a versão atual e ignora a atualização se ela corresponder ao que já está instalado.

A versão é resolvida a partir do primeiro destes que está definido:

1. O campo `version` no `plugin.json` do plugin
2. O campo `version` na entrada do marketplace do plugin em `marketplace.json`
3. O SHA do commit git do plugin, para fontes `github`, `url`, `git-subdir` e relative-path em um marketplace hospedado em git
4. `unknown`, para fontes `npm` ou diretórios locais não dentro de um repositório git

Isso oferece duas maneiras de versionar um plugin:

| Abordagem                | Como                                                                    | Comportamento de atualização                                                                                                                                                            | Melhor para                                            |
| :----------------------- | :---------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------- |
| **Versão explícita**     | Defina `"version": "2.1.0"` em `plugin.json`                            | Os usuários recebem atualizações apenas quando você aumenta este campo. Enviar novos commits sem aumentá-lo não tem efeito, e `/plugin update` relata "já está na versão mais recente". | Plugins publicados com ciclos de lançamento estáveis   |
| **Versão SHA do commit** | Omita `version` tanto de `plugin.json` quanto da entrada do marketplace | Os usuários recebem atualizações em cada novo commit para a fonte git do plugin                                                                                                         | Plugins internos ou de equipe em desenvolvimento ativo |

<Warning>
  Se você definir `version` em `plugin.json`, você deve aumentá-lo toda vez que quiser que os usuários recebam alterações. Enviar novos commits sozinho não é suficiente, porque Claude Code vê a mesma string de versão e mantém a cópia em cache. Se você está iterando rapidamente, deixe `version` indefinido para que o SHA do commit git seja usado em vez disso.
</Warning>

Se você usar versões explícitas, siga [versionamento semântico](https://semver.org) (`MAJOR.MINOR.PATCH`): aumente MAJOR para mudanças de quebra, MINOR para novos recursos, PATCH para correções de bugs. Documente as alterações em um `CHANGELOG.md`.

***

<h2 id="see-also">
  Veja também
</h2>

* [Plugins](https://code.claude.com/docs/pt/plugins) - Tutoriais e uso prático
* [Marketplaces de plugins](https://code.claude.com/docs/pt/plugin-marketplaces) - Criando e gerenciando marketplaces
* [Skills](https://code.claude.com/docs/pt/skills) - Detalhes de desenvolvimento de skill
* [Subagents](https://code.claude.com/docs/pt/sub-agents) - Configuração e capacidades de agent
* [Hooks](https://code.claude.com/docs/pt/hooks) - Manipulação de eventos e automação
* [MCP](https://code.claude.com/docs/pt/mcp) - Integração de ferramenta externa
* [Configurações](https://code.claude.com/docs/pt/settings) - Opções de configuração para plugins
