<!-- Fonte: https://code.claude.com/docs/pt/hooks-guide | Baixado em: 2026-08-06 | slug: hooks-guide -->

# Automatizar ações com hooks

> Execute comandos shell automaticamente quando Claude Code edita arquivos, conclui tarefas ou precisa de entrada. Formate código, envie notificações, valide comandos e aplique regras do projeto.

Hooks são comandos shell definidos pelo usuário que executam em pontos específicos do ciclo de vida do Claude Code. Eles fornecem controle determinístico sobre o comportamento do Claude Code, garantindo que certas ações sempre aconteçam em vez de depender do LLM para escolher executá-las. Use hooks para aplicar regras do projeto, automatizar tarefas repetitivas e integrar Claude Code com suas ferramentas existentes.

Para decisões que exigem julgamento em vez de regras determinísticas, você também pode usar [hooks baseados em prompt](#prompt-based-hooks) ou [hooks baseados em agente](#agent-based-hooks) que usam um modelo Claude para avaliar condições.

Para outras formas de estender Claude Code, consulte [skills](https://code.claude.com/docs/pt/skills) para dar ao Claude instruções adicionais e comandos executáveis, [subagents](https://code.claude.com/docs/pt/sub-agents) para executar tarefas em contextos isolados e [plugins](https://code.claude.com/docs/pt/plugins) para empacotar extensões para compartilhar entre projetos.

<Tip>
  Este guia cobre casos de uso comuns e como começar. Para esquemas de eventos completos, formatos de entrada/saída JSON e recursos avançados como hooks assíncronos e hooks de ferramentas MCP, consulte a [referência de Hooks](https://code.claude.com/docs/pt/hooks).
</Tip>

<h2 id="set-up-your-first-hook">
  Configure seu primeiro hook
</h2>

Para criar um hook, adicione um bloco `hooks` a um [arquivo de configuração](#configure-hook-location). Este passo a passo cria um hook de notificação de desktop, para que você seja alertado sempre que Claude estiver aguardando sua entrada em vez de observar o terminal.

<Steps>
  <Step title="Adicione o hook às suas configurações">
    Abra `~/.claude/settings.json` e adicione um hook `Notification`. O exemplo abaixo usa `osascript` para macOS; consulte [Receba notificações quando Claude precisa de entrada](#get-notified-when-claude-needs-input) para comandos Linux e Windows.

    ```json theme={null}
    {
      "hooks": {
        "Notification": [
          {
            "matcher": "",
            "hooks": [
              {
                "type": "command",
                "command": "osascript -e 'display notification \"Claude Code needs your attention\" with title \"Claude Code\"'"
              }
            ]
          }
        ]
      }
    }
    ```

    Se seu arquivo de configuração já tem uma chave `hooks`, adicione `Notification` como um irmão das chaves de evento existentes em vez de substituir o objeto inteiro. Cada nome de evento é uma chave dentro do único objeto `hooks`:

    ```json theme={null}
    {
      "hooks": {
        "PostToolUse": [
          {
            "matcher": "Edit|Write",
            "hooks": [{ "type": "command", "command": "jq -r '.tool_input.file_path' | xargs npx prettier --write" }]
          }
        ],
        "Notification": [
          {
            "matcher": "",
            "hooks": [{ "type": "command", "command": "osascript -e 'display notification \"Claude Code needs your attention\" with title \"Claude Code\"'" }]
          }
        ]
      }
    }
    ```

    Você também pode pedir ao Claude para escrever o hook para você descrevendo o que deseja na CLI.
  </Step>

  <Step title="Verifique a configuração">
    Digite `/hooks` para abrir o navegador de hooks. Você verá uma lista de todos os eventos de hook disponíveis, com uma contagem ao lado de cada evento que tem hooks configurados. Selecione `Notification` para confirmar que seu novo hook aparece na lista. Selecionar o hook mostra seus detalhes: o evento, matcher, tipo, arquivo de origem e comando.
  </Step>

  <Step title="Teste o hook">
    Pressione `Esc` para retornar à CLI. Peça ao Claude para fazer algo que exija permissão, depois saia do terminal. Você deve receber uma notificação de desktop.
  </Step>
</Steps>

<Tip>
  O menu `/hooks` é somente leitura. Para adicionar, modificar ou remover hooks, edite seu JSON de configuração diretamente ou peça ao Claude para fazer a alteração.
</Tip>

<h2 id="what-you-can-automate">
  O que você pode automatizar
</h2>

Hooks permitem executar código em pontos-chave do ciclo de vida do Claude Code: formatar arquivos após edições, bloquear comandos antes de executarem, enviar notificações quando Claude precisa de entrada, injetar contexto no início da sessão e muito mais. Para a lista completa de eventos de hook, consulte a [referência de Hooks](https://code.claude.com/docs/pt/hooks#hook-lifecycle).

Cada exemplo inclui um bloco de configuração pronto para usar que você adiciona a um [arquivo de configuração](#configure-hook-location).

Para um exemplo de produção de hooks que executam uma revisão de modelo separada e alimentam as descobertas de volta à sessão, consulte [como o plugin `security-guidance` se integra com Claude Code](https://code.claude.com/docs/pt/security-guidance#how-the-plugin-integrates-with-claude-code).

<h3 id="get-notified-when-claude-needs-input">
  Receba notificações quando Claude precisa de entrada
</h3>

Receba uma notificação de desktop sempre que Claude terminar de trabalhar e precisar de sua entrada, para que você possa mudar para outras tarefas sem verificar o terminal.

Este hook usa o evento `Notification`, que dispara quando Claude está aguardando entrada ou permissão. Cada aba abaixo usa o comando de notificação nativo da plataforma. Adicione isto a `~/.claude/settings.json`:

<Tabs>
  <Tab title="macOS">
    ```json theme={null}
    {
      "hooks": {
        "Notification": [
          {
            "matcher": "",
            "hooks": [
              {
                "type": "command",
                "command": "osascript -e 'display notification \"Claude Code needs your attention\" with title \"Claude Code\"'"
              }
            ]
          }
        ]
      }
    }
    ```

    <Accordion title="Se nenhuma notificação aparecer">
      `osascript` roteia notificações através do aplicativo Script Editor integrado. Se o Script Editor não tiver permissão de notificação, o comando falha silenciosamente e macOS não solicitará que você o conceda. Execute isto no Terminal uma vez para fazer o Script Editor aparecer em suas configurações de notificação:

      ```bash theme={null}
      osascript -e 'display notification "test"'
      ```

      Nada aparecerá ainda. Abra **System Settings > Notifications**, encontre **Script Editor** na lista e ative **Allow Notifications**. Execute o comando novamente para confirmar que a notificação de teste aparece.
    </Accordion>
  </Tab>

  <Tab title="Linux">
    ```json theme={null}
    {
      "hooks": {
        "Notification": [
          {
            "matcher": "",
            "hooks": [
              {
                "type": "command",
                "command": "notify-send 'Claude Code' 'Claude Code needs your attention'"
              }
            ]
          }
        ]
      }
    }
    ```
  </Tab>

  <Tab title="Windows (PowerShell)">
    ```json theme={null}
    {
      "hooks": {
        "Notification": [
          {
            "matcher": "",
            "hooks": [
              {
                "type": "command",
                "command": "powershell.exe -Command \"[System.Reflection.Assembly]::LoadWithPartialName('System.Windows.Forms'); [System.Windows.Forms.MessageBox]::Show('Claude Code needs your attention', 'Claude Code')\""
              }
            ]
          }
        ]
      }
    }
    ```
  </Tab>
</Tabs>

O `matcher` vazio dispara em todos os tipos de notificação. Para disparar apenas em eventos específicos, defina-o como um destes valores:

| Matcher                | Dispara quando                                                                                                                            |
| :--------------------- | :---------------------------------------------------------------------------------------------------------------------------------------- |
| `permission_prompt`    | Claude precisa que você aprove um uso de ferramenta                                                                                       |
| `idle_prompt`          | Claude terminou e está aguardando seu próximo prompt                                                                                      |
| `auth_success`         | A autenticação é concluída                                                                                                                |
| `elicitation_dialog`   | Um servidor MCP abre um formulário de elicitação                                                                                          |
| `elicitation_complete` | Um formulário de elicitação MCP é enviado ou descartado                                                                                   |
| `elicitation_response` | Uma resposta de elicitação MCP é enviada de volta ao servidor                                                                             |
| `agent_needs_input`    | Uma sessão em segundo plano começa a aguardar sua entrada. Dispara apenas enquanto a [visualização de agente](https://code.claude.com/docs/pt/agent-view) está aberta |
| `agent_completed`      | Uma sessão em segundo plano termina ou falha. Dispara apenas enquanto a [visualização de agente](https://code.claude.com/docs/pt/agent-view) está aberta              |

Os matchers `agent_needs_input` e `agent_completed` exigem Claude Code v2.1.198 ou posterior.

Digite `/hooks` e selecione `Notification` para confirmar que o hook está registrado. Para o esquema de evento completo, consulte a [referência de Notification](https://code.claude.com/docs/pt/hooks#notification).

<h3 id="auto-format-code-after-edits">
  Formatar código automaticamente após edições
</h3>

Execute automaticamente [Prettier](https://prettier.io/) em cada arquivo que Claude edita, para que a formatação permaneça consistente sem intervenção manual.

Este hook usa o evento `PostToolUse` com um matcher `Edit|Write`, para que execute apenas após ferramentas de edição de arquivo. O comando extrai o caminho do arquivo editado com [`jq`](https://jqlang.github.io/jq/) e o passa para Prettier. Adicione isto a `.claude/settings.json` na raiz do seu projeto:

```json theme={null}
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "jq -r '.tool_input.file_path' | xargs npx prettier --write"
          }
        ]
      }
    ]
  }
}
```

No Claude Code v2.1.191 ou posterior, você também pode escrever o matcher como `Edit,Write`, já que `|` e `,` são separadores de lista intercambiáveis para matchers de nome de ferramenta nessas versões.

<Note>
  Os exemplos Bash nesta página usam `jq` para análise JSON. Instale-o com `brew install jq` no macOS, `apt-get install jq` no Debian e Ubuntu, ou consulte [downloads do `jq`](https://jqlang.github.io/jq/download/).
</Note>

<h3 id="block-edits-to-protected-files">
  Bloquear edições em arquivos protegidos
</h3>

Impeça que Claude modifique arquivos sensíveis como `.env`, `package-lock.json` ou qualquer coisa em `.git/`. Claude recebe feedback explicando por que a edição foi bloqueada, para que possa ajustar sua abordagem.

Este exemplo usa um arquivo de script separado que o hook chama. O script verifica o caminho do arquivo de destino contra uma lista de padrões protegidos e sai com código 2 para bloquear a edição.

<Steps>
  <Step title="Crie o script do hook">
    Salve isto em `.claude/hooks/protect-files.sh`:

    ```bash theme={null}
    #!/bin/bash
    # protect-files.sh

    INPUT=$(cat)
    FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

    PROTECTED_PATTERNS=(".env" "package-lock.json" ".git/")

    for pattern in "${PROTECTED_PATTERNS[@]}"; do
      if [[ "$FILE_PATH" == *"$pattern"* ]]; then
        echo "Blocked: $FILE_PATH matches protected pattern '$pattern'" >&2
        exit 2
      fi
    done

    exit 0
    ```
  </Step>

  <Step title="Torne o script executável no macOS e Linux">
    Scripts de hook devem ser executáveis para que Claude Code os execute:

    ```bash theme={null}
    chmod +x .claude/hooks/protect-files.sh
    ```
  </Step>

  <Step title="Registre o hook">
    Adicione um hook `PreToolUse` a `.claude/settings.json` que execute o script antes de qualquer chamada de ferramenta `Edit` ou `Write`:

    ```json theme={null}
    {
      "hooks": {
        "PreToolUse": [
          {
            "matcher": "Edit|Write",
            "hooks": [
              {
                "type": "command",
                "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/protect-files.sh"
              }
            ]
          }
        ]
      }
    }
    ```
  </Step>
</Steps>

<h3 id="re-inject-context-after-compaction">
  Re-injetar contexto após compactação
</h3>

Quando a janela de contexto do Claude fica cheia, a compactação resume a conversa para liberar espaço. Isto pode perder detalhes importantes. Use um hook `SessionStart` com um matcher `compact` para re-injetar contexto crítico após cada compactação.

Qualquer texto que seu comando escreve para stdout é adicionado ao contexto do Claude. Este exemplo lembra ao Claude as convenções do projeto e trabalho recente. Adicione isto a `.claude/settings.json` na raiz do seu projeto:

```json theme={null}
{
  "hooks": {
    "SessionStart": [
      {
        "matcher": "compact",
        "hooks": [
          {
            "type": "command",
            "command": "echo 'Reminder: use Bun, not npm. Run bun test before committing. Current sprint: auth refactor.'"
          }
        ]
      }
    ]
  }
}
```

Você pode substituir o `echo` por qualquer comando que produza saída dinâmica, como `git log --oneline -5` para mostrar commits recentes. Para injetar contexto em cada início de sessão, considere usar [CLAUDE.md](https://code.claude.com/docs/pt/memory) em vez disso. Para variáveis de ambiente, consulte [`CLAUDE_ENV_FILE`](https://code.claude.com/docs/pt/hooks#persist-environment-variables) na referência.

<h3 id="audit-configuration-changes">
  Auditar mudanças de configuração
</h3>

Rastreie quando arquivos de configuração ou skills mudam durante uma sessão. O evento `ConfigChange` dispara quando um processo externo ou editor modifica um arquivo de configuração, para que você possa registrar mudanças para conformidade ou bloquear modificações não autorizadas.

Este exemplo anexa cada mudança a um log de auditoria. Adicione isto a `~/.claude/settings.json`:

```json theme={null}
{
  "hooks": {
    "ConfigChange": [
      {
        "matcher": "",
        "hooks": [
          {
            "type": "command",
            "command": "jq -c '{timestamp: now | todate, source: .source, file: .file_path}' >> ~/claude-config-audit.log"
          }
        ]
      }
    ]
  }
}
```

O matcher filtra por tipo de configuração: `user_settings`, `project_settings`, `local_settings`, `policy_settings` ou `skills`. Para bloquear uma mudança de entrar em vigor, saia com código 2 ou retorne `{"decision": "block"}`. Consulte a [referência de ConfigChange](https://code.claude.com/docs/pt/hooks#configchange) para o esquema de entrada completo.

<h3 id="reload-environment-when-directory-or-files-change">
  Recarregar ambiente quando diretório ou arquivos mudam
</h3>

Alguns projetos definem variáveis de ambiente diferentes dependendo de qual diretório você está. Ferramentas como [direnv](https://direnv.net/) fazem isto automaticamente no seu shell, mas a ferramenta Bash do Claude não pega essas mudanças por conta própria.

Emparelhar um hook `SessionStart` com um hook `CwdChanged` corrige isto. `SessionStart` carrega as variáveis para o diretório em que você inicia, e `CwdChanged` as recarrega cada vez que Claude muda de diretório. Ambos escrevem para `CLAUDE_ENV_FILE`, que Claude Code executa como um preâmbulo de script antes de cada comando Bash. Adicione isto a `~/.claude/settings.json`:

```json theme={null}
{
  "hooks": {
    "SessionStart": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "direnv export bash > \"$CLAUDE_ENV_FILE\""
          }
        ]
      }
    ],
    "CwdChanged": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "direnv export bash > \"$CLAUDE_ENV_FILE\""
          }
        ]
      }
    ]
  }
}
```

Execute `direnv allow` uma vez em cada diretório que tenha um `.envrc` para que direnv tenha permissão para carregá-lo. Se você usar devbox ou nix em vez de direnv, o mesmo padrão funciona com `devbox shellenv` ou `devbox global shellenv` no lugar de `direnv export bash`.

Para reagir a arquivos específicos em vez de cada mudança de diretório, use `FileChanged` com um `matcher` listando os nomes de arquivo para observar, separados por `|`. Ao construir a lista de observação, Claude Code divide este valor em nomes de arquivo literais em vez de avaliá-lo como uma regex. Consulte [FileChanged](https://code.claude.com/docs/pt/hooks#filechanged) para como o mesmo valor também filtra quais grupos de hook executam quando um arquivo muda. Este exemplo observa `.envrc` e `.env` no diretório de trabalho:

```json theme={null}
{
  "hooks": {
    "FileChanged": [
      {
        "matcher": ".envrc|.env",
        "hooks": [
          {
            "type": "command",
            "command": "direnv export bash > \"$CLAUDE_ENV_FILE\""
          }
        ]
      }
    ]
  }
}
```

Consulte as entradas de referência [CwdChanged](https://code.claude.com/docs/pt/hooks#cwdchanged) e [FileChanged](https://code.claude.com/docs/pt/hooks#filechanged) para esquemas de entrada, saída `watchPaths` e detalhes de `CLAUDE_ENV_FILE`.

<h3 id="auto-approve-specific-permission-prompts">
  Aprovar automaticamente prompts de permissão específicos
</h3>

Pule o diálogo de aprovação para chamadas de ferramenta que você sempre permite. Este exemplo aprova automaticamente `ExitPlanMode`, a ferramenta que Claude chama quando termina de apresentar um plano e pede para prosseguir, para que você não seja solicitado toda vez que um plano estiver pronto.

Diferentemente dos exemplos de código de saída acima, a aprovação automática exige que seu hook escreva uma decisão JSON para stdout. Um hook `PermissionRequest` dispara quando Claude Code está prestes a mostrar um diálogo de permissão, e retornar `"behavior": "allow"` responde em seu nome.

O matcher restringe o hook apenas a `ExitPlanMode`, para que nenhum outro prompt seja afetado. Adicione isto a `~/.claude/settings.json`:

```json theme={null}
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "ExitPlanMode",
        "hooks": [
          {
            "type": "command",
            "command": "echo '{\"hookSpecificOutput\": {\"hookEventName\": \"PermissionRequest\", \"decision\": {\"behavior\": \"allow\"}}}'"
          }
        ]
      }
    ]
  }
}
```

Quando o hook aprova, Claude Code sai do modo de plano e restaura qualquer modo de permissão que estava ativo antes de você entrar no modo de plano. A transcrição mostra "Allowed by PermissionRequest hook" onde o diálogo teria aparecido. O caminho do hook sempre mantém a conversa atual: ele não pode limpar contexto e iniciar uma sessão de implementação fresca da forma que o diálogo pode.

Para definir um modo de permissão específico em vez disso, a saída do seu hook pode incluir um array `updatedPermissions` com uma entrada `setMode`. O valor `mode` é qualquer modo de permissão como `default`, `acceptEdits` ou `bypassPermissions`, e `destination: "session"` o aplica apenas para a sessão atual.

<Note>
  `bypassPermissions` só se aplica se a sessão foi iniciada com modo bypass já disponível: `--dangerously-skip-permissions`, `--permission-mode bypassPermissions`, `--allow-dangerously-skip-permissions`, ou `permissions.defaultMode: "bypassPermissions"` em configurações, e não desabilitado por [`permissions.disableBypassPermissionsMode`](https://code.claude.com/docs/pt/permissions#managed-settings). Nunca é persistido como `defaultMode`.
</Note>

Para mudar a sessão para `acceptEdits`, seu hook escreve este JSON para stdout:

```json theme={null}
{
  "hookSpecificOutput": {
    "hookEventName": "PermissionRequest",
    "decision": {
      "behavior": "allow",
      "updatedPermissions": [
        { "type": "setMode", "mode": "acceptEdits", "destination": "session" }
      ]
    }
  }
}
```

Mantenha o matcher o mais restrito possível. Corresponder a `.*` ou deixar o matcher vazio aprovaria automaticamente cada prompt de permissão, incluindo escritas de arquivo e comandos shell. Consulte a [referência de PermissionRequest](https://code.claude.com/docs/pt/hooks#permissionrequest-decision-control) para o conjunto completo de campos de decisão.

<h2 id="how-hooks-work">
  Como hooks funcionam
</h2>

Eventos de hook disparam em pontos específicos do ciclo de vida do Claude Code. Quando um evento dispara, todos os hooks correspondentes executam em paralelo, e comandos de hook idênticos são automaticamente desduplicados. A tabela abaixo mostra cada evento e quando dispara:

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

Cada hook tem um `type` que determina como ele executa. A maioria dos hooks usa `"type": "command"`, que executa um comando shell. Quatro outros tipos estão disponíveis:

* `"type": "http"`: POST dados de evento para uma URL. Consulte [HTTP hooks](#http-hooks).
* `"type": "mcp_tool"`: chamar uma ferramenta em um servidor MCP já conectado. Consulte [MCP tool hooks](https://code.claude.com/docs/pt/hooks#mcp-tool-hook-fields).
* `"type": "prompt"`: avaliação LLM de turno único. Consulte [Prompt-based hooks](#prompt-based-hooks).
* `"type": "agent"`: verificação multi-turno com acesso a ferramentas. Agent hooks são experimentais e podem mudar. Consulte [Agent-based hooks](#agent-based-hooks).

<h3 id="combine-results-from-multiple-hooks">
  Combinar resultados de múltiplos hooks
</h3>

Quando múltiplos hooks correspondem ao mesmo evento, o comando de cada hook executa até a conclusão antes do Claude Code mesclar os resultados. Um hook retornando `deny` não impede que hooks irmãos executem. Não confie em um `deny` de um hook para suprimir efeitos colaterais em outro hook.

Após todos os hooks correspondentes terminarem, Claude Code combina suas saídas. Para decisões de permissão `PreToolUse`, a resposta mais restritiva vence, na ordem `deny`, `defer`, `ask`, `allow`. Texto de `additionalContext` é mantido de cada hook e passado ao Claude junto.

O exemplo abaixo registra dois hooks `PreToolUse` em `Bash`. O primeiro anexa cada comando a um arquivo de log e sai com 0. O segundo executa um script que sai com 2 para negar quando o comando contém `rm -rf`:

```json theme={null}
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "jq -r .tool_input.command >> ~/.claude/bash.log"
          },
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/block-rm-rf.sh"
          }
        ]
      }
    ]
  }
}
```

Quando Claude tenta executar `rm -rf /tmp/build`, ambos os hooks executam em paralelo. O hook de logging escreve o comando em `~/.claude/bash.log` e sai com 0, o que não relata nenhuma decisão. O hook de proteção sai com 2, o que nega a chamada de ferramenta. O deny vence, então Claude Code bloqueia o comando e mostra ao Claude o stderr da proteção. A entrada de log ainda é escrita porque o hook de logging já executou.

<h3 id="read-input-and-return-output">
  Ler entrada e retornar saída
</h3>

Hooks se comunicam com Claude Code através de stdin, stdout, stderr e códigos de saída. Quando um evento dispara, Claude Code passa dados específicos do evento como JSON para stdin do seu script. Seu script lê esses dados, faz seu trabalho e diz ao Claude Code o que fazer a seguir através do código de saída.

<h4 id="hook-input">
  Entrada do hook
</h4>

Cada evento inclui campos comuns como `session_id` e `cwd`, mas cada tipo de evento adiciona dados diferentes. Por exemplo, quando Claude executa um comando Bash, um hook `PreToolUse` recebe algo assim em stdin:

```json theme={null}
{
  "session_id": "abc123",          // ID único para esta sessão
  "cwd": "/Users/sarah/myproject", // diretório de trabalho quando o evento disparou
  "hook_event_name": "PreToolUse", // qual evento acionou este hook
  "tool_name": "Bash",             // a ferramenta que Claude está prestes a usar
  "tool_input": {                  // os argumentos que Claude passou para a ferramenta
    "command": "npm test"          // para Bash, este é o comando shell
  }
}
```

Seu script pode analisar esse JSON e agir em qualquer um desses campos. Hooks `UserPromptSubmit` obtêm o texto `prompt` em vez disso, hooks `SessionStart` obtêm a `source` de `startup`, `resume`, `clear` ou `compact`, e assim por diante. Consulte [Campos de entrada comuns](https://code.claude.com/docs/pt/hooks#common-input-fields) na referência para campos compartilhados, e a seção de cada evento para esquemas específicos do evento.

<h4 id="hook-output">
  Saída do hook
</h4>

Seu script diz ao Claude Code o que fazer a seguir escrevendo para stdout ou stderr e saindo com um código específico. O seguinte hook `PreToolUse` bloqueia um comando:

```bash theme={null}
#!/bin/bash
INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command')

if echo "$COMMAND" | grep -q "drop table"; then
  echo "Blocked: dropping tables is not allowed" >&2  # stderr se torna feedback do Claude
  exit 2 # exit 2 = bloquear a ação
fi

exit 0  # exit 0 = nenhuma decisão; o fluxo de permissão normal se aplica
```

O código de saída determina o que acontece a seguir:

* **Exit 0**: o hook não relata objeção e a ação prossegue normalmente. Para um hook `PreToolUse` isso não aprova a chamada de ferramenta: o [fluxo de permissão](https://code.claude.com/docs/pt/permissions) normal ainda se aplica. Para hooks `UserPromptSubmit`, `UserPromptExpansion` e `SessionStart`, qualquer coisa que você escrever para stdout é adicionada ao contexto do Claude.
* **Exit 2**: a ação é bloqueada. Escreva um motivo para stderr, e Claude o recebe como feedback para que possa se ajustar. Alguns eventos não podem ser bloqueados: para `SessionStart`, `Setup`, `Notification` e outros, exit 2 mostra stderr ao usuário e a execução continua. Consulte [comportamento do código de saída 2 por evento](https://code.claude.com/docs/pt/hooks#exit-code-2-behavior-per-event) para a lista completa.
* **Qualquer outro código de saída**: a ação prossegue. A transcrição mostra um aviso `<hook name> hook error` seguido pela primeira linha de stderr; o stderr completo vai para o [log de debug](https://code.claude.com/docs/pt/hooks#debug-hooks).

<h4 id="structured-json-output">
  Saída JSON estruturada
</h4>

Códigos de saída lhe dão apenas bloquear ou ficar em silêncio. Para mais controle, saia com 0 e imprima um objeto JSON para stdout em vez disso.

<Note>
  Use exit 2 para bloquear com uma mensagem stderr, ou exit 0 com JSON para controle estruturado. Não misture: Claude Code ignora JSON quando você sai com 2.
</Note>

Por exemplo, um hook `PreToolUse` pode negar uma chamada de ferramenta e dizer ao Claude por quê, ou escalar para o usuário para aprovação:

```json theme={null}
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "Use rg instead of grep for better performance"
  }
}
```

Com `"deny"`, Claude Code cancela a chamada de ferramenta e alimenta `permissionDecisionReason` de volta ao Claude. Esses valores `permissionDecision` são específicos para `PreToolUse`:

* `"allow"`: pular o prompt de permissão interativo. Regras de negação e pedido, incluindo listas de negação gerenciadas por empresa, ainda se aplicam, assim como prompts para ferramentas de conector [que sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) e ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool)
* `"deny"`: cancelar a chamada de ferramenta e enviar o motivo ao Claude
* `"ask"`: mostrar o prompt de permissão ao usuário normalmente

Um quarto valor, `"defer"`, está disponível em [modo não-interativo](https://code.claude.com/docs/pt/headless) com a flag `-p`. Ele sai do processo com a chamada de ferramenta preservada para que um wrapper do Agent SDK possa coletar entrada e retomar. Consulte [Adiar uma chamada de ferramenta para depois](https://code.claude.com/docs/pt/hooks#defer-a-tool-call-for-later) na referência.

Retornar `"allow"` pula o prompt interativo mas não substitui [regras de permissão](https://code.claude.com/docs/pt/permissions#manage-permissions). Se uma regra de negação corresponder à chamada de ferramenta, a chamada é bloqueada mesmo quando seu hook retorna `"allow"`. Se uma regra de pedido corresponder, o usuário ainda é solicitado, assim como ferramentas de conector [que sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) e ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool). Isto significa que regras de negação de qualquer escopo de configuração, incluindo [configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files), sempre têm precedência sobre aprovações de hook.

Outros eventos usam padrões de decisão diferentes. Por exemplo, hooks `PostToolUse` e `Stop` usam um campo `decision: "block"` de nível superior, enquanto `PermissionRequest` usa `hookSpecificOutput.decision.behavior`. Consulte a [tabela de resumo](https://code.claude.com/docs/pt/hooks#decision-control) na referência para uma análise completa por evento.

Para hooks `UserPromptSubmit`, use `hookSpecificOutput.additionalContext` em vez disso para injetar texto no contexto do Claude. Aninhe `additionalContext` dentro de `hookSpecificOutput`; se você o colocar no nível superior do JSON, Claude Code o ignora silenciosamente. Por exemplo, esta saída adiciona o estado do branch atual a cada prompt:

```json theme={null}
{
  "hookSpecificOutput": {
    "hookEventName": "UserPromptSubmit",
    "additionalContext": "Current branch: release-42. Deploy freeze until Friday."
  }
}
```

Consulte [Controle de decisão UserPromptSubmit](https://code.claude.com/docs/pt/hooks#userpromptsubmit-decision-control) para a forma de saída completa, incluindo bloqueio de prompts e definição do título da sessão.

Hooks com `type: "prompt"` lidam com saída de forma diferente: consulte [Prompt-based hooks](#prompt-based-hooks).

<h3 id="filter-hooks-with-matchers">
  Filtrar hooks com matchers
</h3>

Sem um matcher, um hook dispara em cada ocorrência de seu evento. Matchers permitem restringir isso. Por exemplo, se você quer executar um formatador apenas após edições de arquivo, não após cada chamada de ferramenta, adicione um matcher ao seu hook `PostToolUse`:

```json theme={null}
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          { "type": "command", "command": "prettier --write ..." }
        ]
      }
    ]
  }
}
```

O matcher `"Edit|Write"` dispara apenas quando Claude usa a ferramenta `Edit` ou `Write`, não quando usa `Bash`, `Read` ou qualquer outra ferramenta. No Claude Code v2.1.191 ou posterior, uma vírgula separa alternativas da mesma forma, então `"Edit, Write"` é equivalente. Consulte [Padrões de matcher](https://code.claude.com/docs/pt/hooks#matcher-patterns) para como nomes simples e expressões regulares são avaliados.

<Note>
  Claude também pode criar ou modificar arquivos executando comandos shell através da ferramenta `Bash`. Se seu hook deve ver cada mudança de arquivo, como para varredura de conformidade ou registro de auditoria, adicione um hook [`Stop`](https://code.claude.com/docs/pt/hooks#stop) que varre a árvore de trabalho uma vez por turno. Para cobertura por chamada em vez disso, também corresponda `Bash` e tenha seu script listar arquivos modificados e não rastreados com `git status --porcelain`.
</Note>

Cada tipo de evento corresponde a um campo específico:

| Evento                                                                                                                                                          | O que o matcher filtra                                                                  | Valores de matcher de exemplo                                                                                                                                                       |
| :-------------------------------------------------------------------------------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PreToolUse`, `PostToolUse`, `PostToolUseFailure`, `PermissionRequest`, `PermissionDenied`                                                                      | nome da ferramenta                                                                      | `Bash`, `Edit\|Write`, `mcp__.*`                                                                                                                                                    |
| `SessionStart`                                                                                                                                                  | como a sessão começou                                                                   | `startup`, `resume`, `clear`, `compact`                                                                                                                                             |
| `Setup`                                                                                                                                                         | qual flag CLI acionou a configuração                                                    | `init`, `maintenance`                                                                                                                                                               |
| `SessionEnd`                                                                                                                                                    | por que a sessão terminou                                                               | `clear`, `resume`, `logout`, `prompt_input_exit`, `bypass_permissions_disabled`, `other`                                                                                            |
| `Notification`                                                                                                                                                  | tipo de notificação                                                                     | `permission_prompt`, `idle_prompt`, `auth_success`, `elicitation_dialog`, `elicitation_complete`, `elicitation_response`, `agent_needs_input`, `agent_completed`                    |
| `SubagentStart`                                                                                                                                                 | tipo de agente                                                                          | `general-purpose`, `Explore`, `Plan`, ou nomes de agentes personalizados                                                                                                            |
| `PreCompact`, `PostCompact`                                                                                                                                     | o que acionou a compactação                                                             | `manual`, `auto`                                                                                                                                                                    |
| `SubagentStop`                                                                                                                                                  | tipo de agente                                                                          | mesmos valores que `SubagentStart`                                                                                                                                                  |
| `ConfigChange`                                                                                                                                                  | fonte de configuração                                                                   | `user_settings`, `project_settings`, `local_settings`, `policy_settings`, `skills`                                                                                                  |
| `StopFailure`                                                                                                                                                   | tipo de erro                                                                            | `rate_limit`, `overloaded`, `authentication_failed`, `oauth_org_not_allowed`, `billing_error`, `invalid_request`, `model_not_found`, `server_error`, `max_output_tokens`, `unknown` |
| `InstructionsLoaded`                                                                                                                                            | motivo de carregamento                                                                  | `session_start`, `nested_traversal`, `path_glob_match`, `include`, `compact`                                                                                                        |
| `Elicitation`                                                                                                                                                   | nome do servidor MCP                                                                    | seus nomes de servidor MCP configurados                                                                                                                                             |
| `ElicitationResult`                                                                                                                                             | nome do servidor MCP                                                                    | mesmos valores que `Elicitation`                                                                                                                                                    |
| `FileChanged`                                                                                                                                                   | nomes de arquivo literais para observar (consulte [FileChanged](https://code.claude.com/docs/pt/hooks#filechanged)) | `.envrc\|.env`                                                                                                                                                                      |
| `UserPromptExpansion`                                                                                                                                           | nome do comando                                                                         | seus nomes de skill ou comando                                                                                                                                                      |
| `UserPromptSubmit`, `PostToolBatch`, `Stop`, `TeammateIdle`, `TaskCreated`, `TaskCompleted`, `WorktreeCreate`, `WorktreeRemove`, `CwdChanged`, `MessageDisplay` | sem suporte a matcher                                                                   | sempre dispara em cada ocorrência                                                                                                                                                   |

As abas abaixo mostram alguns matchers adicionais em diferentes tipos de evento.

<Tabs>
  <Tab title="Registrar cada comando Bash">
    Corresponda apenas chamadas de ferramenta `Bash` e registre cada comando em um arquivo. O evento `PostToolUse` dispara após o comando ser concluído, então `tool_input.command` contém o que foi executado. O hook recebe os dados do evento como JSON em stdin, e `jq -r '.tool_input.command'` extrai apenas a string de comando, que `>>` anexa ao arquivo de log:

    ```json theme={null}
    {
      "hooks": {
        "PostToolUse": [
          {
            "matcher": "Bash",
            "hooks": [
              {
                "type": "command",
                "command": "jq -r '.tool_input.command' >> ~/.claude/command-log.txt"
              }
            ]
          }
        ]
      }
    }
    ```
  </Tab>

  <Tab title="Corresponder ferramentas MCP">
    Ferramentas MCP usam uma convenção de nomenclatura diferente das ferramentas integradas: `mcp__<server>__<tool>`, onde `<server>` é o nome do servidor MCP e `<tool>` é a ferramenta que fornece. Por exemplo, `mcp__github__search_repositories` ou `mcp__filesystem__read_file`. Ferramentas de um [servidor MCP fornecido por plugin](https://code.claude.com/docs/pt/mcp#plugin-provided-mcp-servers) usam um segmento de servidor com escopo em vez disso, como `mcp__plugin_my-plugin_db__query`. Use um matcher regex para direcionar todas as ferramentas de um servidor específico, ou corresponder entre servidores com um padrão como `mcp__.*__write.*`. Consulte [Corresponder ferramentas MCP](https://code.claude.com/docs/pt/hooks#match-mcp-tools) na referência para a lista completa de exemplos.

    O comando abaixo extrai o nome da ferramenta da entrada JSON do hook com `jq` e o escreve para stderr. Escrever para stderr mantém stdout limpo para saída JSON e envia a mensagem para o [log de debug](https://code.claude.com/docs/pt/hooks#debug-hooks):

    ```json theme={null}
    {
      "hooks": {
        "PreToolUse": [
          {
            "matcher": "mcp__github__.*",
            "hooks": [
              {
                "type": "command",
                "command": "echo \"GitHub tool called: $(jq -r '.tool_name')\" >&2"
              }
            ]
          }
        ]
      }
    }
    ```
  </Tab>

  <Tab title="Limpar ao final da sessão">
    O evento `SessionEnd` suporta matchers na razão pela qual a sessão terminou. Este hook dispara apenas em `clear` (quando você executa `/clear`), não em saídas normais:

    ```json theme={null}
    {
      "hooks": {
        "SessionEnd": [
          {
            "matcher": "clear",
            "hooks": [
              {
                "type": "command",
                "command": "rm -f /tmp/claude-scratch-*.txt"
              }
            ]
          }
        ]
      }
    }
    ```
  </Tab>
</Tabs>

Para sintaxe completa de matcher, consulte a [referência de Hooks](https://code.claude.com/docs/pt/hooks#configuration).

<h4 id="filter-by-tool-name-and-arguments-with-the-if-field">
  Filtrar por nome de ferramenta e argumentos com o campo `if`
</h4>

O campo `if` usa [sintaxe de regra de permissão](https://code.claude.com/docs/pt/permissions) para filtrar hooks por nome de ferramenta e argumentos juntos, para que o processo do hook apenas seja gerado quando a chamada de ferramenta corresponder. Isto vai além de `matcher`, que filtra no nível do grupo apenas por nome de ferramenta.

Por exemplo, esta configuração executa um hook apenas quando Claude usa comandos `git` em vez de todos os comandos Bash:

```json theme={null}
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "if": "Bash(git *)",
            "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/check-git-policy.sh"
          }
        ]
      }
    ]
  }
}
```

Se seu hook deve executar depende da forma do seu padrão `if` e do comando Bash que Claude está invocando:

| padrão `if`        | Comando Bash           | Hook executa? | Por quê                                                                                                          |
| :----------------- | :--------------------- | :------------ | :--------------------------------------------------------------------------------------------------------------- |
| `Bash(git *)`      | `git push`             | sim           | nome do comando corresponde                                                                                      |
| `Bash(git *)`      | `npm test && git push` | sim           | cada subcomando é verificado; `git push` corresponde                                                             |
| `Bash(git *)`      | `echo $(git log)`      | sim           | comandos dentro de `$()` e backticks são verificados; `git log` corresponde                                      |
| `Bash(git *)`      | `echo $(date)`         | não           | nenhum subcomando corresponde a `git *`                                                                          |
| `Bash(git push *)` | `echo $(date)`         | sim           | padrões que especificam mais do que o nome do comando executam o hook mesmo assim em `$()`, backticks, ou `$VAR` |

O filtro também falha aberto, executando seu hook independentemente do padrão, quando o comando Bash não pode ser analisado. Como o filtro é melhor esforço, use o [sistema de permissão](https://code.claude.com/docs/pt/permissions) em vez de um hook para impor um allow ou deny difícil.

O campo `if` aceita os mesmos padrões que regras de permissão: `"Bash(git *)"`, `"Edit(*.ts)"`, e assim por diante. Para corresponder múltiplos nomes de ferramenta, use manipuladores separados cada um com seu próprio valor `if`, ou corresponda no nível `matcher` onde alternação de pipe é suportada.

`if` funciona apenas em eventos de ferramenta: `PreToolUse`, `PostToolUse`, `PostToolUseFailure`, `PermissionRequest` e `PermissionDenied`. Adicioná-lo a qualquer outro evento impede o hook de executar.

<h3 id="configure-hook-location">
  Configurar local do hook
</h3>

Onde você adiciona um hook determina seu escopo:

| Local                                                       | Escopo                                | Compartilhável                            |
| :---------------------------------------------------------- | :------------------------------------ | :---------------------------------------- |
| `~/.claude/settings.json`                                   | Todos os seus projetos                | Não, local para sua máquina               |
| `.claude/settings.json`                                     | Projeto único                         | Sim, pode ser commitado no repo           |
| `.claude/settings.local.json`                               | Projeto único                         | Não, gitignored quando Claude Code o cria |
| Configurações de política gerenciada                        | Organização inteira                   | Sim, controlado por admin                 |
| [Plugin](https://code.claude.com/docs/pt/plugins) `hooks/hooks.json`                    | Quando o plugin está habilitado       | Sim, empacotado com o plugin              |
| [Skill](https://code.claude.com/docs/pt/skills) ou [agente](https://code.claude.com/docs/pt/sub-agents) frontmatter | Enquanto a skill ou agente está ativo | Sim, definido no arquivo do componente    |

Execute [`/hooks`](https://code.claude.com/docs/pt/hooks#the-%2Fhooks-menu) no Claude Code para navegar por todos os hooks configurados agrupados por evento.

Para desabilitar hooks, defina `"disableAllHooks": true` no seu arquivo de configuração. Hooks configurados em configurações gerenciadas ainda executam a menos que `disableAllHooks` também esteja definido lá.

Se você editar arquivos de configuração diretamente enquanto Claude Code está em execução, o observador de arquivo normalmente pega mudanças de hook automaticamente.

<h2 id="prompt-based-hooks">
  Hooks baseados em prompt
</h2>

Para decisões que exigem julgamento em vez de regras determinísticas, use hooks `type: "prompt"`. Em vez de executar um comando shell, Claude Code envia seu prompt e os dados de entrada do hook para um modelo Claude (Haiku por padrão) para tomar a decisão. Você pode especificar um modelo diferente com o campo `model` se precisar de mais capacidade.

O único trabalho do modelo é retornar uma decisão sim/não como JSON:

* `"ok": true`: a ação prossegue
* `"ok": false`: o que acontece depende do evento:
  * `Stop` e `SubagentStop`: o `reason` é alimentado de volta ao Claude para que ele continue trabalhando
  * `PreToolUse`: a chamada de ferramenta é negada e o `reason` é retornado ao Claude como o erro da ferramenta, para que ele possa se ajustar e continuar
  * `PostToolUse`, `PostToolBatch`, `UserPromptSubmit` e `UserPromptExpansion`: o turno termina e o `reason` aparece no chat como uma linha de aviso

Este exemplo usa um hook `Stop` para perguntar ao modelo se todas as tarefas solicitadas estão completas. Se o modelo retornar `"ok": false`, Claude continua trabalhando e usa o `reason` como sua próxima instrução:

```json theme={null}
{
  "hooks": {
    "Stop": [
      {
        "hooks": [
          {
            "type": "prompt",
            "prompt": "Check if all tasks are complete. If not, respond with {\"ok\": false, \"reason\": \"what remains to be done\"}."
          }
        ]
      }
    ]
  }
}
```

Para opções de configuração completas, consulte [Hooks baseados em prompt](https://code.claude.com/docs/pt/hooks#prompt-based-hooks) na referência.

<h2 id="agent-based-hooks">
  Hooks baseados em agente
</h2>

<Warning>
  Hooks de agente são experimentais. Comportamento e configuração podem mudar em futuras versões. Para fluxos de trabalho de produção, prefira [hooks de comando](https://code.claude.com/docs/pt/hooks#command-hook-fields).
</Warning>

Quando a verificação exige inspecionar arquivos ou executar comandos, use hooks `type: "agent"`. Diferentemente de hooks de prompt que fazem uma única chamada LLM, hooks de agente geram um subagente que pode ler arquivos, pesquisar código e usar outras ferramentas para verificar condições antes de retornar uma decisão.

Hooks de agente usam o mesmo formato de resposta `"ok"` / `"reason"` que hooks de prompt, mas com um timeout padrão mais longo de 60 segundos e até 50 turnos de uso de ferramenta.

Este exemplo verifica que os testes passam antes de permitir que Claude pare:

```json theme={null}
{
  "hooks": {
    "Stop": [
      {
        "hooks": [
          {
            "type": "agent",
            "prompt": "Verify that all unit tests pass. Run the test suite and check the results. $ARGUMENTS",
            "timeout": 120
          }
        ]
      }
    ]
  }
}
```

Use hooks de prompt quando os dados de entrada do hook sozinhos são suficientes para tomar uma decisão. Use hooks de agente quando você precisa verificar algo contra o estado real da base de código.

Para opções de configuração completas, consulte [Hooks baseados em agente](https://code.claude.com/docs/pt/hooks#agent-based-hooks) na referência.

<h2 id="http-hooks">
  HTTP hooks
</h2>

Use `type: "http"` hooks para fazer POST de dados de evento para um endpoint HTTP em vez de executar um comando shell. O endpoint recebe o mesmo JSON que um hook de comando receberia em stdin, e retorna resultados através do corpo da resposta HTTP usando o mesmo formato JSON.

HTTP hooks são úteis quando você quer que um servidor web, função em nuvem ou serviço externo manipule a lógica do hook: por exemplo, um serviço de auditoria compartilhado que registra eventos de uso de ferramenta em toda uma equipe.

Este exemplo faz POST de cada uso de ferramenta para um serviço de logging local:

```json theme={null}
{
  "hooks": {
    "PostToolUse": [
      {
        "hooks": [
          {
            "type": "http",
            "url": "http://localhost:8080/hooks/tool-use",
            "headers": {
              "Authorization": "Bearer $MY_TOKEN"
            },
            "allowedEnvVars": ["MY_TOKEN"]
          }
        ]
      }
    ]
  }
}
```

O endpoint deve retornar um corpo de resposta JSON usando o mesmo [formato de saída](https://code.claude.com/docs/pt/hooks#json-output) que hooks de comando. Para bloquear uma chamada de ferramenta, retorne uma resposta 2xx com os campos `hookSpecificOutput` apropriados. Códigos de status HTTP sozinhos não podem bloquear ações.

Valores de header suportam interpolação de variável de ambiente usando sintaxe `$VAR_NAME` ou `${VAR_NAME}`. Apenas variáveis listadas no array `allowedEnvVars` são resolvidas; todas as outras referências `$VAR` permanecem vazias.

Para opções de configuração completas e manipulação de resposta, consulte [HTTP hooks](https://code.claude.com/docs/pt/hooks#http-hook-fields) na referência.

<h2 id="limitations-and-troubleshooting">
  Limitações e solução de problemas
</h2>

<h3 id="limitations">
  Limitações
</h3>

Tenha em mente estas restrições ao projetar hooks:

* Hooks de comando se comunicam apenas através de stdout, stderr e códigos de saída. Eles não podem disparar comandos `/` ou chamadas de ferramenta. Texto retornado via `additionalContext` é injetado como um lembrete do sistema que Claude lê como texto simples. HTTP hooks se comunicam através do corpo da resposta em vez disso.
* Os timeouts do hook variam por tipo. Substitua por hook com o campo `timeout` em segundos.
  * `command`, `http`, `mcp_tool`: 10 minutos. `UserPromptSubmit` reduz estes para 30 segundos, e `MessageDisplay` reduz estes para 10 segundos.
  * `prompt`: 30 segundos.
  * `agent`: 60 segundos.
* Hooks `PostToolUse` não podem desfazer ações já que a ferramenta já foi executada.
* Hooks `PermissionRequest` não disparam em [modo não-interativo](https://code.claude.com/docs/pt/headless) com a flag `-p`. Use hooks `PreToolUse` para decisões de permissão automatizadas.
* Hooks `Stop` disparam sempre que Claude termina de responder, não apenas na conclusão de tarefas. Eles não disparam em interrupções do usuário. Erros de API disparam [StopFailure](https://code.claude.com/docs/pt/hooks#stopfailure) em vez disso.
* Quando múltiplos hooks `PreToolUse` retornam [`updatedInput`](https://code.claude.com/docs/pt/hooks#pretooluse) para reescrever argumentos de uma ferramenta, o último a terminar vence. Como hooks executam em paralelo, a ordem é não-determinística. Evite ter mais de um hook modificando a entrada da mesma ferramenta.

<h3 id="hooks-and-permission-modes">
  Hooks e modos de permissão
</h3>

Hooks `PreToolUse` disparam antes de qualquer verificação de modo de permissão. Um hook que retorna `permissionDecision: "deny"` bloqueia a ferramenta mesmo em modo `bypassPermissions` ou com `--dangerously-skip-permissions`. Isto permite que você aplique política que usuários não podem contornar mudando seu modo de permissão.

O inverso não é verdadeiro: um hook retornando `"allow"` não contorna regras de negação de configurações, e não pode suprimir o prompt para ferramentas de conector [que sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) ou ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool). Hooks podem apertar restrições mas não afrouxá-las além do que regras de permissão permitem.

<h3 id="hook-not-firing">
  Hook não dispara
</h3>

O hook está configurado mas nunca executa.

* Execute `/hooks` e confirme que o hook aparece sob o evento correto
* Verifique que o padrão do matcher corresponde ao nome da ferramenta exatamente. Matchers são sensíveis a maiúsculas
* Verifique que você está acionando o tipo de evento correto: `PreToolUse` dispara antes da execução da ferramenta, `PostToolUse` dispara depois
* Se usar hooks `PermissionRequest` em modo não-interativo com a flag `-p`, mude para `PreToolUse` em vez disso

<h3 id="hook-error-in-output">
  Erro de hook na saída
</h3>

Você vê uma mensagem como "PreToolUse hook error: ..." na transcrição.

* Seu script saiu com um código não-zero inesperadamente. Teste-o manualmente canalizando JSON de amostra:
  ```bash theme={null}
  echo '{"tool_name":"Bash","tool_input":{"command":"ls"}}' | ./my-hook.sh
  echo $?  # Verifique o código de saída
  ```
* Se você vir "command not found", use caminhos absolutos ou `${CLAUDE_PROJECT_DIR}` para referenciar scripts. Para evitar quoting de shell completamente, adicione `"args": []` para mudar para [forma exec](https://code.claude.com/docs/pt/hooks#exec-form-and-shell-form), que gera o script diretamente sem um shell
* Se você vir "jq: command not found", instale `jq` ou use Python/Node.js para análise JSON
* Se o script não está executando em tudo, torne-o executável: `chmod +x ./my-hook.sh`

<h3 id="/hooks-shows-no-hooks-configured">
  `/hooks` mostra nenhum hook configurado
</h3>

Você editou um arquivo de configuração mas os hooks não aparecem no menu.

* Edições de arquivo são normalmente capturadas automaticamente. Se não tiverem aparecido após alguns segundos, o observador de arquivo pode ter perdido a mudança: reinicie sua sessão para forçar um recarregamento.
* Verifique que seu JSON é válido: vírgulas finais e comentários não são permitidos
* Confirme que o arquivo de configuração está no local correto: `.claude/settings.json` para hooks de projeto, `~/.claude/settings.json` para hooks globais

<h3 id="stop-hook-hits-the-block-cap">
  Stop hook atinge o limite de bloqueio
</h3>

Claude continua trabalhando em vez de parar, depois termina a rodada com um aviso de que o Stop hook bloqueou muitas vezes consecutivas sem progresso.

Claude Code substitui um Stop hook depois que ele bloqueia oito vezes seguidas sem progresso. Seu script de hook precisa verificar se já acionou uma continuação. Analise o campo `stop_hook_active` da entrada JSON e saia cedo se for `true`:

```bash theme={null}
#!/bin/bash
INPUT=$(cat)
if [ "$(echo "$INPUT" | jq -r '.stop_hook_active')" = "true" ]; then
  exit 0  # Permitir que Claude pare
fi
# ... resto da lógica do seu hook
```

Se seu hook legitimamente precisa de mais de oito iterações para convergir, aumente o limite com [`CLAUDE_CODE_STOP_HOOK_BLOCK_CAP`](https://code.claude.com/docs/pt/env-vars).

<h3 id="json-validation-failed">
  Validação JSON falhou
</h3>

Claude Code mostra um erro de análise JSON mesmo que seu script de hook produza JSON válido.

Quando Claude Code executa um hook de comando em forma de shell, um sem `args`, ele gera `sh -c` no macOS e Linux ou Git Bash no Windows por padrão. Este shell é não-interativo, mas Git Bash e algumas configurações, como `BASH_ENV` apontando para `~/.bashrc`, ainda fornecem seu perfil. Se esse perfil contiver instruções `echo` incondicionais, a saída é adicionada ao seu JSON do hook:

```text theme={null}
Shell ready on arm64
{"decision": "block", "reason": "Not allowed"}
```

Claude Code tenta analisar isto como JSON e falha. Para corrigir isto, envolva instruções echo no seu perfil shell para que executem apenas em shells interativos:

```bash theme={null}
# Em ~/.zshrc ou ~/.bashrc
if [[ $- == *i* ]]; then
  echo "Shell ready"
fi
```

A variável `$-` contém flags de shell, e `i` significa interativo. Hooks executam em shells não-interativos, então o echo é pulado.

<h3 id="debug-techniques">
  Técnicas de debug
</h3>

A visualização de transcrição, alternada com `Ctrl+O`, mostra um resumo de uma linha para cada hook que disparou: sucesso é silencioso, erros de bloqueio mostram stderr, e erros de não-bloqueio mostram um aviso `<hook name> hook error` seguido pela primeira linha de stderr.

Para detalhes de execução completos incluindo quais hooks corresponderam, seus códigos de saída, stdout e stderr, leia o log de debug. Inicie Claude Code com `claude --debug-file /tmp/claude.log` para escrever em um caminho conhecido, depois `tail -f /tmp/claude.log` em outro terminal. Se você iniciou sem essa flag, execute `/debug` no meio da sessão para habilitar logging e encontrar o caminho do log.

<h2 id="learn-more">
  Saiba mais
</h2>

* [Referência de Hooks](https://code.claude.com/docs/pt/hooks): esquemas de eventos completos, formato de saída JSON, hooks assíncronos e hooks de ferramentas MCP
* [Considerações de segurança](https://code.claude.com/docs/pt/hooks#security-considerations): revise antes de implantar hooks em ambientes compartilhados ou de produção
* [Exemplo de validador de comando Bash](https://github.com/anthropics/claude-code/blob/main/examples/hooks/bash_command_validator_example.py): implementação de referência completa
