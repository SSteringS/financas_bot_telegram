<!-- Fonte: https://code.claude.com/docs/pt/headless | Baixado em: 2026-08-06 | slug: headless -->

# Executar Claude Code programaticamente

> Use o Agent SDK para executar Claude Code programaticamente a partir da CLI, Python ou TypeScript.

O [Agent SDK](https://code.claude.com/docs/pt/agent-sdk/overview) oferece as mesmas ferramentas, loop de agente e gerenciamento de contexto que alimentam Claude Code. Está disponível como uma CLI para scripts e CI/CD, ou como pacotes [Python](https://code.claude.com/docs/pt/agent-sdk/python) e [TypeScript](https://code.claude.com/docs/pt/agent-sdk/typescript) para controle programático completo.

Para executar Claude Code em modo não interativo, passe `-p` com seu prompt e qualquer [opção de CLI](https://code.claude.com/docs/pt/cli-reference):

```bash theme={null}
claude -p "Find and fix the bug in auth.py" --allowedTools "Read,Edit,Bash"
```

Esta página aborda o uso do Agent SDK via CLI (`claude -p`). Para os pacotes SDK Python e TypeScript com saídas estruturadas, callbacks de aprovação de ferramentas e objetos de mensagem nativos, consulte a [documentação completa do Agent SDK](https://code.claude.com/docs/pt/agent-sdk/overview).

<h2 id="basic-usage">
  Uso básico
</h2>

Adicione o sinalizador `-p` (ou `--print`) a qualquer comando `claude` para executá-lo de forma não interativa. Todas as [opções de CLI](https://code.claude.com/docs/pt/cli-reference) funcionam com `-p`, incluindo:

* `--continue` para [continuar conversas](#continue-conversations)
* `--allowedTools` para [aprovar ferramentas automaticamente](#auto-approve-tools)
* `--output-format` para [saída estruturada](#get-structured-output)

Este exemplo faz uma pergunta ao Claude sobre sua base de código e imprime a resposta:

```bash theme={null}
claude -p "What does the auth module do?"
```

<h3 id="start-faster-with-bare-mode">
  Comece mais rápido com modo bare
</h3>

Adicione `--bare` para reduzir o tempo de inicialização pulando a descoberta automática de hooks, skills, plugins, servidores MCP, memória automática e CLAUDE.md. Sem ele, `claude -p` carrega o mesmo [contexto](https://code.claude.com/docs/pt/how-claude-code-works#the-context-window) que uma sessão interativa carregaria, incluindo qualquer coisa configurada no diretório de trabalho ou `~/.claude`.

O modo bare é útil para CI e scripts onde você precisa do mesmo resultado em cada máquina. Um hook no `~/.claude` de um colega de trabalho ou um servidor MCP no `.mcp.json` do projeto não serão executados, porque o modo bare nunca os lê. Apenas os sinalizadores que você passa explicitamente têm efeito.

Este exemplo executa uma tarefa de resumo única em modo bare e pré-aprova a ferramenta Read para que a chamada seja concluída sem um prompt de permissão:

```bash theme={null}
claude --bare -p "Summarize this file" --allowedTools "Read"
```

No modo bare, Claude tem acesso às ferramentas Bash, leitura de arquivo e edição de arquivo. Passe qualquer contexto que você precise com um sinalizador:

| Para carregar                | Use                                                     |
| ---------------------------- | ------------------------------------------------------- |
| Adições de prompt do sistema | `--append-system-prompt`, `--append-system-prompt-file` |
| Configurações                | `--settings <file-or-json>`                             |
| Servidores MCP               | `--mcp-config <file-or-json>`                           |
| Agentes personalizados       | `--agents <json>`                                       |
| Um plugin                    | `--plugin-dir <path>`, `--plugin-url <url>`             |

O modo bare pula leituras de OAuth e keychain. A autenticação do Anthropic deve vir de `ANTHROPIC_API_KEY` ou um `apiKeyHelper` no JSON passado para `--settings`. Amazon Bedrock, Google Cloud's Agent Platform e Microsoft Foundry usam suas credenciais de provedor usuais.

<Note>
  `--bare` é o modo recomendado para chamadas com script e SDK, e se tornará o padrão para `-p` em uma versão futura.
</Note>

<h3 id="background-tasks-at-exit">
  Tarefas em segundo plano ao sair
</h3>

Se Claude iniciar uma [tarefa Bash em segundo plano](https://code.claude.com/docs/pt/tools-reference#bash-tool-behavior) durante uma execução de `claude -p`, por exemplo um servidor de desenvolvimento ou uma compilação de observação, essa tarefa será encerrada cerca de cinco segundos após Claude retornar seu resultado final e stdin ter sido fechado. O período de carência permite que uma tarefa que termina logo após o resultado ainda entregue sua saída. Antes da v2.1.163, um processo em segundo plano que nunca sairia manteria a invocação de `claude -p` aberta indefinidamente.

[Subagentos](https://code.claude.com/docs/pt/sub-agents) em segundo plano e fluxos de trabalho estão isentos do período de carência de cinco segundos porque seu resultado faz parte da saída final, então `claude -p` aguarda sua conclusão. A partir da v2.1.182, essa espera é limitada a dez minutos por padrão para que um agente em segundo plano travado não possa manter o processo aberto indefinidamente. Ajuste o limite com [`CLAUDE_CODE_PRINT_BG_WAIT_CEILING_MS`](https://code.claude.com/docs/pt/env-vars), ou defina-o como `0` para aguardar sem limite.

<h2 id="examples">
  Exemplos
</h2>

Estes exemplos destacam padrões comuns de CLI. Para CI e outras chamadas com script, adicione [`--bare`](#start-faster-with-bare-mode) para que não captem o que quer que esteja configurado localmente.

<h3 id="pipe-data-through-claude">
  Canalizar dados através do Claude
</h3>

O modo não interativo lê stdin, então você pode canalizar dados e redirecionar a resposta como qualquer outra ferramenta de linha de comando.

Este exemplo canaliza um log de compilação para Claude e escreve a explicação em um arquivo:

```bash theme={null}
cat build-error.txt | claude -p 'concisely explain the root cause of this build error' > output.txt
```

Com `--output-format json`, a carga de resposta inclui `total_cost_usd` e um detalhamento de custo por modelo, para que os chamadores com script possam rastrear gastos por invocação sem consultar o [painel de uso](https://code.claude.com/docs/pt/costs).

<Note>
  A partir do Claude Code v2.1.128, stdin canalizado é limitado a 10MB. Se você exceder o limite, Claude Code sai com um erro claro e um status diferente de zero. Para trabalhar com entradas maiores, escreva o conteúdo em um arquivo e faça referência ao caminho do arquivo em seu prompt em vez de canalizá-lo.
</Note>

<h3 id="add-claude-to-a-build-script">
  Adicionar Claude a um script de compilação
</h3>

Você pode envolver uma chamada não interativa em um script para usar Claude como um linter ou revisor específico do projeto.

Este script `package.json` canaliza o diff contra `main` para Claude e pede que ele relate erros de digitação. Canalizar o diff significa que Claude não precisa de permissão Bash para lê-lo, e as aspas duplas escapadas mantêm o script portável para Windows:

```json theme={null}
{
  "scripts": {
    "lint:claude": "git diff main | claude -p \"you are a typo linter. for each typo in this diff, report filename:line on one line and the issue on the next. return nothing else.\""
  }
}
```

<h3 id="get-structured-output">
  Obter saída estruturada
</h3>

Use `--output-format` para controlar como as respostas são retornadas:

* `text` (padrão): saída de texto simples
* `json`: JSON estruturado com resultado, ID de sessão e metadados
* `stream-json`: JSON delimitado por quebra de linha para streaming em tempo real

Este exemplo retorna um resumo do projeto como JSON com metadados de sessão, com o resultado de texto no campo `result`:

```bash theme={null}
claude -p "Summarize this project" --output-format json
```

Para obter saída em conformidade com um esquema específico, use `--output-format json` com `--json-schema` e uma definição de [JSON Schema](https://json-schema.org/). A resposta inclui metadados sobre a solicitação (ID de sessão, uso, etc.) com a saída estruturada no campo `structured_output`.

Este exemplo extrai nomes de funções e os retorna como uma matriz de strings:

```bash theme={null}
claude -p "Extract the main function names from auth.py" \
  --output-format json \
  --json-schema '{"type":"object","properties":{"functions":{"type":"array","items":{"type":"string"}}},"required":["functions"]}'
```

Se o valor não for um JSON Schema válido, `claude` sai com `Error: --json-schema is not a valid JSON Schema` seguido pelo diagnóstico do validador. Claude Code aceita esquemas que usam a palavra-chave `format`, como `"format": "email"`, mas trata `format` como uma anotação e não a impõe. Antes da v2.1.205, Claude Code ignorava silenciosamente um esquema inválido e retornava texto não estruturado, e tratava qualquer esquema contendo `format` como inválido.

<Tip>
  Use uma ferramenta como [jq](https://jqlang.github.io/jq/) para analisar a resposta e extrair campos específicos:

  ```bash theme={null}
  # Extract the text result
  claude -p "Summarize this project" --output-format json | jq -r '.result'

  # Extract structured output
  claude -p "Extract function names from auth.py" \
    --output-format json \
    --json-schema '{"type":"object","properties":{"functions":{"type":"array","items":{"type":"string"}}},"required":["functions"]}' \
    | jq '.structured_output'
  ```
</Tip>

<h3 id="stream-responses">
  Respostas de stream
</h3>

Use `--output-format stream-json` com `--verbose` e `--include-partial-messages` para receber tokens conforme são gerados. Cada linha é um objeto JSON representando um evento:

```bash theme={null}
claude -p "Explain recursion" --output-format stream-json --verbose --include-partial-messages
```

A última linha do stream é uma mensagem `result` com o texto de resposta final, custo e metadados de sessão. Antes da v2.1.208, canalizar uma resposta grande poderia truncar a linha final e omitir a mensagem `result`.

O exemplo a seguir usa [jq](https://jqlang.github.io/jq/) para filtrar deltas de texto e exibir apenas o texto de streaming. O sinalizador `-r` produz strings brutas (sem aspas) e `-j` une sem quebras de linha para que os tokens façam streaming continuamente:

```bash theme={null}
claude -p "Write a poem" --output-format stream-json --verbose --include-partial-messages | \
  jq -rj 'select(.type == "stream_event" and .event.delta.type? == "text_delta") | .event.delta.text'
```

Quando uma solicitação de API falha com um erro que pode ser repetido, Claude Code emite um evento `system/api_retry` antes de tentar novamente. Você pode usar isso para exibir o progresso de repetição ou implementar lógica de backoff personalizada.

| Campo            | Tipo            | Descrição                                                                                                                                                                                                 |
| ---------------- | --------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `type`           | `"system"`      | tipo de mensagem                                                                                                                                                                                          |
| `subtype`        | `"api_retry"`   | identifica isso como um evento de repetição                                                                                                                                                               |
| `attempt`        | inteiro         | número da tentativa atual, começando em 1                                                                                                                                                                 |
| `max_retries`    | inteiro         | total de repetições permitidas                                                                                                                                                                            |
| `retry_delay_ms` | inteiro         | milissegundos até a próxima tentativa                                                                                                                                                                     |
| `error_status`   | inteiro ou nulo | código de status HTTP, ou `null` para erros de conexão sem resposta HTTP                                                                                                                                  |
| `error`          | string          | categoria de erro: `authentication_failed`, `oauth_org_not_allowed`, `billing_error`, `rate_limit`, `overloaded`, `invalid_request`, `model_not_found`, `server_error`, `max_output_tokens`, ou `unknown` |
| `uuid`           | string          | identificador único do evento                                                                                                                                                                             |
| `session_id`     | string          | sessão à qual o evento pertence                                                                                                                                                                           |

O evento `system/init` relata metadados de sessão incluindo o modelo, ferramentas, servidores MCP e plugins carregados. É o primeiro evento no stream a menos que eventos de inicialização o precedam:

* eventos `plugin_install`, quando [`CLAUDE_CODE_SYNC_PLUGIN_INSTALL`](https://code.claude.com/docs/pt/env-vars) está definido.
* [eventos `hook_started`, `hook_progress` e `hook_response`](https://code.claude.com/docs/pt/agent-sdk/typescript#sdkhookstartedmessage), enquanto um hook [`SessionStart`](https://code.claude.com/docs/pt/hooks#sessionstart) ou [`Setup`](https://code.claude.com/docs/pt/hooks#setup) configurado é executado. Estes fazem stream conforme o hook os produz. Claude Code v2.1.169 através v2.1.203 os entregou em um lote após o hook ser concluído, ainda à frente de `system/init`; v2.1.204 restaurou a entrega ao vivo.

O evento também carrega uma matriz `capabilities` opcional de strings nomeando os comportamentos do protocolo que esta versão do Claude Code implementa, como `interrupt_receipt_v1`. Verifique-a para detectar recursos em vez de comparar strings de versão, e ignore valores que você não reconheça. O campo requer Claude Code v2.1.205 ou posterior e está ausente em versões anteriores. Consulte [`SDKSystemMessage`](https://code.claude.com/docs/pt/agent-sdk/typescript#sdksystemmessage) para a lista de capacidades.

Use os campos de plugin para falhar CI quando um plugin não foi carregado:

| Campo           | Tipo  | Descrição                                                                                                                                                                                                                                                                                                                 |
| --------------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `plugins`       | array | plugins que foram carregados com sucesso, cada um com `name` e `path`                                                                                                                                                                                                                                                     |
| `plugin_errors` | array | erros de tempo de carregamento de plugin, cada um com `plugin`, `type` e `message`. Inclui versões de dependência insatisfeitas e falhas de carregamento de `--plugin-dir` como um caminho ausente ou arquivo inválido. Os plugins afetados são rebaixados e ausentes de `plugins`. A chave é omitida quando não há erros |

Quando [`CLAUDE_CODE_SYNC_PLUGIN_INSTALL`](https://code.claude.com/docs/pt/env-vars) está definido, Claude Code emite eventos `system/plugin_install` enquanto plugins do marketplace instalam antes da primeira volta. Use estes para exibir o progresso de instalação em sua própria UI.

| Campo        | Tipo                                                     | Descrição                                                                                                    |
| ------------ | -------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `type`       | `"system"`                                               | tipo de mensagem                                                                                             |
| `subtype`    | `"plugin_install"`                                       | identifica isso como um evento de instalação de plugin                                                       |
| `status`     | `"started"`, `"installed"`, `"failed"`, ou `"completed"` | `started` e `completed` envolvem a instalação geral; `installed` e `failed` relatam marketplaces individuais |
| `name`       | string, opcional                                         | nome do marketplace, presente em `installed` e `failed`                                                      |
| `error`      | string, opcional                                         | mensagem de falha, presente em `failed`                                                                      |
| `uuid`       | string                                                   | identificador único do evento                                                                                |
| `session_id` | string                                                   | sessão à qual o evento pertence                                                                              |

Para streaming programático com callbacks e objetos de mensagem, consulte [Stream responses in real-time](https://code.claude.com/docs/pt/agent-sdk/streaming-output) na documentação do Agent SDK.

<h3 id="auto-approve-tools">
  Aprovar ferramentas automaticamente
</h3>

Use `--allowedTools` para permitir que Claude use certas ferramentas sem solicitar. Este exemplo executa um conjunto de testes e corrige falhas, permitindo que Claude execute comandos Bash e leia/edite arquivos sem pedir permissão:

```bash theme={null}
claude -p "Run the test suite and fix any failures" \
  --allowedTools "Bash,Read,Edit"
```

Para definir uma linha de base para toda a sessão em vez de listar ferramentas individuais, passe um [modo de permissão](https://code.claude.com/docs/pt/permission-modes). `dontAsk` nega qualquer coisa não em suas regras `permissions.allow` ou no [conjunto de comandos somente leitura](https://code.claude.com/docs/pt/permissions#read-only-commands), o que é útil para execuções de CI bloqueadas. `AskUserQuestion`, ferramentas de conector [que sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools), e ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) são negadas mesmo quando uma regra de permissão corresponde.

`acceptEdits` permite que Claude escreva arquivos sem solicitar e também aprova automaticamente comandos comuns do sistema de arquivos, como `mkdir`, `touch`, `mv` e `cp`. Outros comandos de shell e solicitações de rede ainda precisam de uma entrada `--allowedTools` ou uma regra `permissions.allow`, caso contrário a execução é abortada quando uma é tentada:

```bash theme={null}
claude -p "Apply the lint fixes" --permission-mode acceptEdits
```

<h3 id="create-a-commit">
  Criar um commit
</h3>

Este exemplo revisa as alterações preparadas e cria um commit com uma mensagem apropriada:

```bash theme={null}
claude -p "Look at my staged changes and create an appropriate commit" \
  --allowedTools "Bash(git diff *),Bash(git log *),Bash(git status *),Bash(git commit *)"
```

O sinalizador `--allowedTools` usa [sintaxe de regra de permissão](https://code.claude.com/docs/pt/settings#permission-rule-syntax). O ` *` à direita habilita correspondência de prefixo, então `Bash(git diff *)` permite qualquer comando começando com `git diff`. O espaço antes de `*` é importante: sem ele, `Bash(git diff*)` também corresponderia a `git diff-index`.

<Note>
  Skills invocadas pelo usuário e comandos personalizados funcionam no modo `-p`: inclua `/skill-name` na string de prompt e Claude Code o expande antes de executar. Comandos integrados que abrem um diálogo interativo, como `/login`, não estão disponíveis no modo `-p`. `/model`, `/effort`, `/fast`, `/color` e `/rename` aceitam o valor como um argumento, por exemplo `/model sonnet`, e `/mcp` sem argumento imprime um resumo de texto do status do servidor; essas formas requerem Claude Code v2.1.205 ou posterior e seguem as notas de disponibilidade de cada comando]\(/pt/commands#all-commands). Para alterar uma configuração de uma invocação `-p`, passe `key=value` para `/config`, por exemplo `/config thinking=false`.
</Note>

<h3 id="customize-the-system-prompt">
  Personalizar o prompt do sistema
</h3>

Use `--append-system-prompt` para adicionar instruções mantendo o comportamento padrão do Claude Code. Este exemplo envia um diff de PR para Claude e o instrui a revisar vulnerabilidades de segurança:

```bash theme={null}
gh pr diff "$1" | claude -p \
  --append-system-prompt "You are a security engineer. Review for vulnerabilities." \
  --output-format json
```

Consulte [system prompt flags](https://code.claude.com/docs/pt/cli-reference#system-prompt-flags) para mais opções, incluindo `--system-prompt` para substituir completamente o prompt padrão.

<h3 id="continue-conversations">
  Continuar conversas
</h3>

Use `--continue` para continuar a conversa mais recente, ou `--resume` com um ID de sessão para continuar uma conversa específica. Este exemplo executa uma revisão e depois envia prompts de acompanhamento:

```bash theme={null}
# First request
claude -p "Review this codebase for performance issues"

# Continue the most recent conversation
claude -p "Now focus on the database queries" --continue
claude -p "Generate a summary of all issues found" --continue
```

Se você estiver executando várias conversas, capture o ID da sessão para retomar uma específica:

```bash theme={null}
session_id=$(claude -p "Start a review" --output-format json | jq -r '.session_id')
claude -p "Continue that review" --resume "$session_id"
```

Execute ambos os comandos do mesmo diretório: a busca de ID de sessão é limitada ao diretório do projeto atual e seus git worktrees. Consulte [Resume a session](https://code.claude.com/docs/pt/sessions#resume-a-session) para as regras de escopo completas.

<h2 id="next-steps">
  Próximas etapas
</h2>

* [Agent SDK quickstart](https://code.claude.com/docs/pt/agent-sdk/quickstart): construa seu primeiro agente com Python ou TypeScript
* [CLI reference](https://code.claude.com/docs/pt/cli-reference): todos os sinalizadores e opções de CLI
* [GitHub Actions](https://code.claude.com/docs/pt/github-actions): use o Agent SDK em fluxos de trabalho do GitHub
* [GitLab CI/CD](https://code.claude.com/docs/pt/gitlab-ci-cd): use o Agent SDK em pipelines do GitLab
