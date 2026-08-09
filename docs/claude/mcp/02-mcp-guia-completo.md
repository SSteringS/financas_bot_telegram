<!-- Fonte: https://code.claude.com/docs/pt/mcp | Baixado em: 2026-08-06 | slug: mcp -->

# Conectar Claude Code a ferramentas via MCP

> Aprenda como conectar Claude Code às suas ferramentas com o Model Context Protocol.

Claude Code pode se conectar a centenas de ferramentas e fontes de dados externas através do [Model Context Protocol (MCP)](https://modelcontextprotocol.io/introduction), um padrão de código aberto para integrações de IA com ferramentas. Os servidores MCP dão ao Claude Code acesso às suas ferramentas, bancos de dados e APIs.

Conecte um servidor quando você se encontrar copiando dados para o chat de outra ferramenta, como um rastreador de problemas ou um painel de monitoramento. Uma vez conectado, Claude pode ler e agir nesse sistema diretamente em vez de trabalhar com o que você cola.

Se você está conectando seu primeiro servidor, comece com o [guia de início rápido do MCP](https://code.claude.com/docs/pt/mcp-quickstart) para um passo a passo detalhado. Esta página é a referência completa.

<h2 id="what-you-can-do-with-mcp">
  O que você pode fazer com MCP
</h2>

Com servidores MCP conectados, você pode pedir ao Claude Code para:

* **Implementar recursos de rastreadores de problemas**: "Adicione o recurso descrito no problema JIRA ENG-4521 e crie um PR no GitHub."
* **Analisar dados de monitoramento**: "Verifique Sentry e Statsig para verificar o uso do recurso descrito em ENG-4521."
* **Consultar bancos de dados**: "Encontre emails de 10 usuários aleatórios que usaram o recurso ENG-4521, com base no nosso banco de dados PostgreSQL."
* **Integrar designs**: "Atualize nosso modelo de email padrão com base nos novos designs do Figma que foram postados no Slack"
* **Automatizar fluxos de trabalho**: "Crie rascunhos do Gmail convidando esses 10 usuários para uma sessão de feedback sobre o novo recurso."
* **Reagir a eventos externos**: Um servidor MCP também pode atuar como um [canal](https://code.claude.com/docs/pt/channels) que envia mensagens para sua sessão, para que Claude reaja a mensagens do Telegram, chats do Discord ou eventos de webhook enquanto você está ausente.

<h2 id="find-and-build-mcp-servers">
  Encontre e crie servidores MCP
</h2>

Navegue por conectores revisados no [Diretório Anthropic](https://claude.ai/directory). Os conectores do Diretório usam a mesma infraestrutura MCP que Claude Code, então você pode adicionar qualquer servidor remoto listado lá com `claude mcp add`.

<Warning>
  Verifique se você confia em cada servidor antes de conectá-lo. Servidores que buscam conteúdo externo podem expô-lo ao [risco de injeção de prompt](https://code.claude.com/docs/pt/security#protect-against-prompt-injection).
</Warning>

Para criar seu próprio servidor, consulte o [guia do servidor MCP](https://modelcontextprotocol.io/docs/develop/build-server) para os fundamentos do protocolo e a [documentação de construção de conectores Claude](https://claude.com/docs/connectors/building) para autenticação, testes e envio ao Diretório.

Você também pode fazer com que Claude crie um servidor para você com o plugin oficial [`mcp-server-dev`](https://github.com/anthropics/claude-plugins-official/tree/main/plugins/mcp-server-dev).

<Steps>
  <Step title="Instale o plugin">
    Em uma sessão Claude Code, execute:

    ```
    /plugin install mcp-server-dev@claude-plugins-official
    ```

    Se Claude Code relatar que o marketplace não foi encontrado, execute `/plugin marketplace add anthropics/claude-plugins-official` primeiro e depois tente novamente a instalação. Após a instalação, execute `/reload-plugins` para ativá-lo na sessão atual.
  </Step>

  <Step title="Execute a skill de construção">
    ```
    /mcp-server-dev:build-mcp-server
    ```

    Claude pergunta sobre seu caso de uso e cria um servidor HTTP remoto ou servidor stdio local.
  </Step>
</Steps>

<h2 id="installing-mcp-servers">
  Instalando servidores MCP
</h2>

Os servidores MCP podem ser configurados de várias maneiras dependendo de suas necessidades:

<h3 id="option-1-add-a-remote-http-server">
  Opção 1: Adicionar um servidor HTTP remoto
</h3>

Servidores HTTP são a opção recomendada para conectar a servidores MCP remotos. Este é o transporte mais amplamente suportado para serviços baseados em nuvem.

```bash theme={null}
# Sintaxe básica
claude mcp add --transport http <name> <url>

# Exemplo real: Conectar ao Notion
claude mcp add --transport http notion https://mcp.notion.com/mcp

# Exemplo com token Bearer
claude mcp add --transport http secure-api https://api.example.com/mcp \
  --header "Authorization: Bearer your-token"
```

Ao configurar servidores MCP via JSON em `.mcp.json`, `~/.claude.json`, ou `claude mcp add-json`, o campo `type` aceita `streamable-http` como um alias para `http`. A especificação MCP usa o nome `streamable-http` para este transporte, portanto as configurações copiadas da documentação do servidor funcionam sem modificação.

Uma entrada JSON que tem uma `url` mas nenhum `type` é um erro de configuração, porque Claude Code lê uma entrada sem `type` como um servidor stdio. Claude Code ignora esse servidor e relata `MCP server "<name>" has a "url" but no "type"; add "type": "http" (or "sse" / "ws") to this entry`. Antes da v2.1.202, Claude Code relatava essa configuração incorreta como `command: expected string, received undefined`.

<h3 id="option-2-add-a-remote-sse-server">
  Opção 2: Adicionar um servidor SSE remoto
</h3>

<Warning>
  O transporte SSE (Server-Sent Events) está descontinuado. Use servidores HTTP em vez disso, quando disponível.
</Warning>

```bash theme={null}
# Sintaxe básica
claude mcp add --transport sse <name> <url>

# Exemplo real: Conectar ao Asana
claude mcp add --transport sse asana https://mcp.asana.com/sse

# Exemplo com cabeçalho de autenticação
claude mcp add --transport sse private-api https://api.company.com/sse \
  --header "X-API-Key: your-key-here"
```

<h3 id="option-3-add-a-local-stdio-server">
  Opção 3: Adicionar um servidor stdio local
</h3>

Servidores Stdio são executados como processos locais em sua máquina. Eles são ideais para ferramentas que precisam de acesso direto ao sistema ou scripts personalizados.

Claude Code define `CLAUDE_PROJECT_DIR` no ambiente do servidor gerado para a raiz do projeto, para que seu servidor possa resolver caminhos relativos ao projeto sem depender do diretório de trabalho. Este é o mesmo diretório que hooks recebem em sua variável `CLAUDE_PROJECT_DIR`. Leia-o de dentro do seu processo de servidor, por exemplo `process.env.CLAUDE_PROJECT_DIR` em Node ou `os.environ["CLAUDE_PROJECT_DIR"]` em Python.

`CLAUDE_PROJECT_DIR` é a raiz do projeto estável e não muda quando você adiciona ou remove diretórios de trabalho durante a sessão. Um servidor que limita seu próprio acesso ao sistema de arquivos a um conjunto de diretórios permitidos deve implementar a solicitação MCP `roots/list` em vez disso. Claude Code responde a `roots/list` com o diretório de inicialização da sessão mais cada [diretório de trabalho adicional](https://code.claude.com/docs/pt/permissions#working-directories) que você concedeu com `--add-dir`, `/add-dir`, ou a configuração `additionalDirectories`. Claude Code envia `notifications/roots/list_changed` quando esse conjunto muda. Antes da v2.1.203, `roots/list` retornava apenas o diretório de inicialização e Claude Code não enviava `notifications/roots/list_changed`.

Esta variável é definida no ambiente do servidor, não no ambiente do próprio Claude Code, portanto referenciá-la via expansão `${VAR}` em um `.mcp.json` com escopo de projeto ou usuário `command` ou `args` requer um padrão como `${CLAUDE_PROJECT_DIR:-.}`. As configurações MCP fornecidas por plugins substituem `${CLAUDE_PROJECT_DIR}` diretamente e não precisam do padrão.

```bash theme={null}
# Sintaxe básica
claude mcp add [options] <name> -- <command> [args...]

# Exemplo real: Adicionar servidor Airtable
claude mcp add --env AIRTABLE_API_KEY=YOUR_KEY --transport stdio airtable \
  -- npx -y airtable-mcp-server
```

<Note>
  **Importante: Separar argumentos do servidor com `--`**

  Para servidores stdio, o `--` (travessão duplo) separa as próprias opções do Claude, como `--transport`, `--env` e `--scope`, do comando e argumentos que executam o servidor. Tudo após `--` é passado para o servidor sem modificação.

  Por exemplo:

  * `claude mcp add --transport stdio myserver -- npx server` → executa `npx server`
  * `claude mcp add --env KEY=value --transport stdio myserver -- python server.py --port 8080` → executa `python server.py --port 8080` com `KEY=value` no ambiente

  Sem `--`, Claude Code tentaria analisar as flags do servidor, como `--port` acima, como suas próprias opções.

  `--env` aceita múltiplos pares `KEY=value`. Se o nome do servidor vem diretamente após `--env`, a CLI lê o nome como outro par e o rejeita, portanto coloque pelo menos uma outra opção entre `--env` e o nome do servidor, como nos exemplos acima.
</Note>

<h3 id="option-4-add-a-remote-websocket-server">
  Opção 4: Adicionar um servidor WebSocket remoto
</h3>

Servidores WebSocket mantêm uma conexão bidirecional persistente, o que é adequado para servidores MCP remotos que enviam eventos para Claude sem solicitação. Use HTTP em vez disso quando seu servidor apenas responde a solicitações, já que HTTP suporta OAuth e a flag `claude mcp add --transport`, enquanto WebSocket não suporta nenhum dos dois.

Configure servidores WebSocket em `.mcp.json` ou com `claude mcp add-json`:

```bash theme={null}
claude mcp add-json events-server \
  '{"type":"ws","url":"wss://mcp.example.com/socket","headers":{"Authorization":"Bearer YOUR_TOKEN"}}'
```

A entrada `type: "ws"` aceita os mesmos campos `url`, `headers`, `headersHelper`, `timeout` e `alwaysLoad` que `http`. A autenticação é apenas por cabeçalho, portanto passe um token estático em `headers` ou gere um no momento da conexão com [`headersHelper`](#use-dynamic-headers-for-custom-authentication). A flag `claude mcp add --transport` não aceita `ws`.

<h3 id="managing-your-servers">
  Gerenciando seus servidores
</h3>

Uma vez configurados, você pode gerenciar seus servidores MCP com estes comandos:

```bash theme={null}
# Listar todos os servidores configurados
claude mcp list

# Obter detalhes para um servidor específico
claude mcp get github

# Remover um servidor
claude mcp remove github

# (dentro do Claude Code) Verificar status do servidor
/mcp
```

Servidores com escopo de projeto de `.mcp.json` que estão aguardando sua aprovação aparecem em `claude mcp list` como `⏸ Pending approval`. Execute `claude` interativamente para revisar e aprovar. `claude mcp get <name>` mostra servidores pendentes como `⏸ Pending approval` e servidores rejeitados como `✗ Rejected`.

A partir da v2.1.196, `claude mcp list` e `claude mcp get` leem aprovações `.mcp.json` apenas de arquivos de configurações que não estão verificados no repositório até que você confie no workspace executando `claude` nele e aceitando a caixa de diálogo de confiança do workspace. Um repositório clonado não pode aprovar seus próprios servidores: [`enableAllProjectMcpServers` ou `enabledMcpjsonServers`](https://code.claude.com/docs/pt/settings#available-settings) confirmado no `.claude/settings.json` do projeto é ignorado em uma pasta não confiável, e o servidor permanece em `⏸ Pending approval` em vez de estar conectado e verificado de saúde.

As aprovações dessas fontes ainda se aplicam em uma pasta não confiável:

* seu `~/.claude/settings.json` do usuário
* configurações gerenciadas
* configurações passadas com `--settings`

As aprovações em um `.claude/settings.local.json` não rastreado também se aplicam, mas apenas depois que você aceita uma caixa de diálogo de confiança para essa pasta ou um de seus diretórios pai: Claude Code executa git para verificar se o arquivo é rastreado, e executa essa verificação apenas em uma pasta confiável. Em uma pasta que você nunca confiou, as aprovações do arquivo aguardam a caixa de diálogo de confiança a menos que a pasta seja seu diretório de configuração pessoal: seu diretório inicial, ou um diretório cujo `.claude` você definiu como [`CLAUDE_CONFIG_DIR`](https://code.claude.com/docs/pt/env-vars). Antes da v2.1.207, um `.claude/settings.local.json` não rastreado aprovava servidores em uma pasta que você nunca tinha confiado.

Uma entrada `disabledMcpjsonServers` em qualquer arquivo de configurações ainda rejeita o servidor.

O painel `/mcp` mostra a contagem de ferramentas ao lado de cada servidor conectado e sinaliza servidores que anunciam a capacidade de ferramentas, mas não expõem nenhuma ferramenta.

Um servidor remoto cuja configuração tem uma `url` vazia aparece como `not configured` em `/mcp`, em `claude mcp list` e no gerenciador [`/plugin`](https://code.claude.com/docs/pt/plugins), e Claude Code não tenta se conectar a ele. Um plugin pode incluir uma entrada de espaço reservado como esta para um conector que você configura depois, para que Claude Code não o relate como um erro ou um problema de configuração. A visualização de detalhes do servidor em `/mcp` lê `No URL configured for this server`; defina a `url` da entrada para se conectar. Antes da v2.1.208, Claude Code relatava uma `url` vazia como um problema de configuração com um prompt para reconectar.

Se sua solicitação precisar de ferramentas de um servidor que ainda está se conectando em segundo plano, Claude aguarda esse servidor antes de continuar. Com [pesquisa de ferramentas](#scale-with-mcp-tool-search) habilitada, que é o padrão, a espera acontece dentro da chamada `ToolSearch`. Em configurações sem pesquisa de ferramentas, como Plataforma de Agente do Google Cloud, um `ANTHROPIC_BASE_URL` personalizado, ou `ENABLE_TOOL_SEARCH=false`, Claude usa a ferramenta `WaitForMcpServers` em vez disso.

Alguns nomes de servidor são reservados para os servidores integrados do Claude Code: `workspace`, `claude-in-chrome`, `computer-use`, `Claude Preview` e `Claude Browser`. Se sua configuração define um servidor com um nome reservado, Claude Code o ignora no tempo de carregamento e mostra um aviso pedindo que você o renomeie. `claude mcp add` rejeita um nome reservado com um erro.

`Claude Preview` e `Claude Browser` nomeiam o servidor integrado que o [painel de visualização do aplicativo desktop Claude Code](https://code.claude.com/docs/pt/desktop#preview-your-app) usa. Antes da v2.1.205, `Claude Browser` não era reservado, portanto um servidor configurado pelo usuário poderia se registrar sob esse nome.

<h3 id="dynamic-tool-updates">
  Atualizações dinâmicas de ferramentas
</h3>

Claude Code suporta notificações MCP `list_changed`, permitindo que servidores MCP atualizem dinamicamente suas ferramentas, prompts e recursos disponíveis sem exigir que você se desconecte e reconecte. Quando um servidor MCP envia uma notificação `list_changed`, Claude Code atualiza automaticamente as capacidades disponíveis desse servidor.

<h3 id="automatic-reconnection">
  Reconexão automática
</h3>

Se um servidor HTTP ou SSE se desconectar durante a sessão, Claude Code se reconecta automaticamente com backoff exponencial: até cinco tentativas, começando com um atraso de um segundo e dobrando a cada vez. O servidor aparece como pendente em `/mcp` enquanto a reconexão está em andamento. Após cinco tentativas falhadas, o servidor é marcado como falho e você pode tentar novamente manualmente de `/mcp`. Servidores Stdio são processos locais e não são reconectados automaticamente.

O mesmo backoff se aplica quando um servidor HTTP ou SSE falha sua conexão inicial na inicialização. A partir da v2.1.121, Claude Code tenta novamente a conexão inicial até três vezes em erros transitórios, como uma resposta 5xx, uma conexão recusada ou um tempo limite, e então marca o servidor como falho se ainda não conseguir se conectar. Erros de autenticação e não encontrado não são retentados porque exigem uma mudança de configuração para serem resolvidos.

Quando um servidor configurado falha ao se conectar, Claude Code diz ao Claude qual servidor falhou e seu erro de conexão, incluindo em resultados `ToolSearch` que não encontram nenhuma ferramenta correspondente, para que Claude relate a falha de conexão em sua resposta. Requer [pesquisa de ferramentas](#scale-with-mcp-tool-search), que está habilitada por padrão. Em configurações sem pesquisa de ferramentas, como um `ANTHROPIC_BASE_URL` personalizado, `ENABLE_TOOL_SEARCH=false`, ou um modelo que não suporta pesquisa de ferramentas, e no Amazon Bedrock, Plataforma de Agente do Google Cloud e Microsoft Foundry, Claude Code não relata falhas de conexão de servidor ao Claude. Antes da v2.1.205, Claude Code não passava erros de conexão ao Claude, e Claude poderia responder como se as ferramentas do servidor falho nunca tivessem sido configuradas.

A partir da v2.1.191, as solicitações de descoberta de capacidade que são executadas após uma conexão bem-sucedida, como `tools/list`, `prompts/list` e `resources/list`, também tentam novamente erros de rede transitórios e erros de servidor até três vezes com backoff curto. Erros de autenticação, respostas 4xx e tempos limite de solicitação não são retentados.

<h3 id="push-messages-with-channels">
  Enviar mensagens com canais
</h3>

Um servidor MCP também pode enviar mensagens diretamente para sua sessão para que Claude possa reagir a eventos externos como resultados de CI, alertas de monitoramento ou mensagens de chat. Para habilitar isso, seu servidor declara a capacidade `claude/channel` e você a ativa com a flag `--channels` na inicialização. Veja [Canais](https://code.claude.com/docs/pt/channels) para usar um canal oficialmente suportado, ou [Referência de canais](https://code.claude.com/docs/pt/channels-reference) para construir o seu próprio.

<Tip>
  Dicas:

  * Use a flag `-s` ou `--scope` para especificar onde a configuração é armazenada:
    * `local` (padrão): Disponível apenas para você no projeto atual. Versões mais antigas chamavam este escopo de `project`
    * `project`: Compartilhado com todos no projeto via arquivo `.mcp.json`
    * `user`: Disponível para você em todos os projetos. Versões mais antigas chamavam este escopo de `global`
  * Defina variáveis de ambiente com flags `-e` ou `--env` (por exemplo, `-e KEY=value`)
  * As flags `--transport` e `--header` também aceitam formas curtas `-t` e `-H`
  * Configure o tempo limite de inicialização do servidor MCP usando a variável de ambiente `MCP_TIMEOUT` (por exemplo, `MCP_TIMEOUT=10000 claude` define um tempo limite de 10 segundos)
  * Defina um tempo limite de execução de ferramenta por servidor adicionando um campo `timeout` em milissegundos à entrada `.mcp.json` desse servidor, por exemplo `"timeout": 600000` para dez minutos. Isso substitui a variável de ambiente `MCP_TOOL_TIMEOUT` apenas para esse servidor
  * Claude Code exibe um aviso quando a saída da ferramenta MCP excede 10.000 tokens e limita a saída a 25.000 tokens por padrão. Para aumentar o limite, defina a variável de ambiente `MAX_MCP_OUTPUT_TOKENS` (por exemplo, `MAX_MCP_OUTPUT_TOKENS=50000`); o limite de aviso é fixo. Veja [Limites de saída MCP e avisos](#mcp-output-limits-and-warnings)
  * Use `/mcp` para autenticar com servidores remotos que exigem autenticação OAuth 2.0
</Tip>

O `timeout` por servidor é um limite de tempo de parede rígido por chamada de ferramenta, e notificações de progresso do servidor não o estendem. Valores abaixo de 1000 são ignorados e caem para `MCP_TOOL_TIMEOUT`, ou para seu padrão de cerca de 28 horas quando essa variável não está definida. Para um servidor HTTP, SSE ou [conector claude.ai](https://code.claude.com/docs/pt/mcp#use-mcp-servers-from-claude-ai) também há um segundo temporizador por solicitação que cobre cada solicitação até o primeiro byte de resposta do servidor. Esse temporizador é de 60 segundos a menos que você defina o `timeout` por servidor ou `MCP_TOOL_TIMEOUT`; definir um para 60 segundos ou superior aumenta o temporizador por solicitação para esse valor, um valor inferior não o encurta, e o padrão de 28 horas de um `MCP_TOOL_TIMEOUT` não definido nunca o alimenta. Servidores Stdio e WebSocket não têm temporizador por solicitação. Antes da v2.1.162, valores abaixo de 1000 eram arredondados para um segundo.

Um `timeout` por servidor de pelo menos 1000 também atua como um piso no tempo limite de inatividade descrito abaixo: Claude Code nunca aborta as chamadas de ferramenta desse servidor por inatividade mais cedo do que o `timeout` por servidor. Requer Claude Code v2.1.203 ou posterior.

Uma chamada de ferramenta para um servidor MCP que não envia resposta e nenhuma notificação de progresso pela janela de inatividade é abortada com um erro em vez de aguardar o limite de tempo de parede. O tempo limite de inatividade requer Claude Code v2.1.187 ou posterior. Aplica-se a todos os tipos de servidor, exceto servidores IDE e servidores em processo do SDK. A janela de inatividade padrão é de cinco minutos para servidores HTTP, SSE, WebSocket e [conector claude.ai](#use-mcp-servers-from-claude-ai), e de 30 minutos para servidores stdio. Antes da v2.1.203, servidores stdio eram isentos do tempo limite de inatividade.

Defina a variável de ambiente [`CLAUDE_CODE_MCP_TOOL_IDLE_TIMEOUT`](https://code.claude.com/docs/pt/env-vars) em milissegundos para alterar a janela de inatividade, ou defina-a como `0` para desabilitar a verificação.

<h3 id="plugin-provided-mcp-servers">
  Servidores MCP fornecidos por plugins
</h3>

[Plugins](https://code.claude.com/docs/pt/plugins) podem agrupar servidores MCP, fornecendo automaticamente ferramentas e integrações quando o plugin está habilitado. Os servidores MCP de plugins funcionam de forma idêntica aos servidores configurados pelo usuário.

**Como funcionam os servidores MCP de plugins**:

* Plugins definem servidores MCP em `.mcp.json` na raiz do plugin ou inline em `plugin.json`
* Quando um plugin está habilitado, seus servidores MCP iniciam automaticamente
* As ferramentas MCP do plugin aparecem junto com as ferramentas MCP configuradas manualmente
* Os servidores de plugins são gerenciados através da instalação de plugins, não comandos `/mcp`

**Exemplo de configuração MCP de plugin**:

Em `.mcp.json` na raiz do plugin:

```json theme={null}
{
  "mcpServers": {
    "database-tools": {
      "command": "${CLAUDE_PLUGIN_ROOT}/servers/db-server",
      "args": ["--config", "${CLAUDE_PLUGIN_ROOT}/config.json"],
      "env": {
        "DB_URL": "${DB_URL}"
      }
    }
  }
}
```

Ou inline em `plugin.json`:

```json theme={null}
{
  "name": "my-plugin",
  "mcpServers": {
    "plugin-api": {
      "command": "${CLAUDE_PLUGIN_ROOT}/servers/api-server",
      "args": ["--port", "8080"]
    }
  }
}
```

**Recursos de MCP de plugin**:

* **Ciclo de vida automático**: Na inicialização da sessão, os servidores para plugins habilitados se conectam automaticamente. Se você habilitar ou desabilitar um plugin durante uma sessão, execute `/reload-plugins` para conectar ou desconectar seus servidores MCP
* **Variáveis de caminho**: `${CLAUDE_PLUGIN_ROOT}` resolve para o diretório de instalação do plugin, `${CLAUDE_PLUGIN_DATA}` para seu diretório de [estado persistente](https://code.claude.com/docs/pt/plugins-reference#persistent-data-directory), e `${CLAUDE_PROJECT_DIR}` para a raiz do projeto estável. A substituição se aplica a:
  * servidores `stdio`: `command`, `args`, `env`
  * servidores `http`, `sse` e `ws`: `url`, `headers` e `headersHelper`. Antes da v2.1.195, `headersHelper` passava o espaço reservado como uma string literal
* **Acesso a variáveis de ambiente do usuário**: Acesso às mesmas variáveis de ambiente que servidores configurados manualmente
* **Múltiplos tipos de transporte**: Suporte para transportes stdio, SSE, HTTP e WebSocket, embora o suporte de transporte possa variar por servidor

**Visualizando servidores MCP de plugins**:

```bash theme={null}
# Dentro do Claude Code, veja todos os servidores MCP incluindo os de plugins
/mcp
```

Os servidores de plugins aparecem na lista com indicadores mostrando que vêm de plugins.

**Nomes de ferramentas MCP de plugin**:

As ferramentas de um servidor MCP agrupado em um plugin incluem tanto o nome do plugin quanto a chave do servidor em seu nome chamável. A forma completa é `mcp__plugin_<plugin-name>_<server-name>__<tool-name>`, onde qualquer caractere fora de `A-Z`, `a-z`, `0-9`, `_` e `-` é substituído por `_`. Para o servidor `database-tools` agrupado em um plugin chamado `my-plugin`, uma ferramenta `query` é chamável como:

```
mcp__plugin_my-plugin_database-tools__query
```

Use este nome completo ao referenciar a ferramenta em [regras de permissão](https://code.claude.com/docs/pt/permissions), na lista `allowed-tools` de uma skill, em um [campo `tools` de um subagente](https://code.claude.com/docs/pt/sub-agents#available-tools), ou em um [matcher de hook](https://code.claude.com/docs/pt/hooks#match-mcp-tools). Um matcher de hook escrito contra a chave do servidor simples, como `mcp__database-tools__.*`, nunca dispara para um servidor agrupado em um plugin.

O servidor em si se registra sob o nome com escopo `plugin:<plugin-name>:<server-name>`, como `plugin:my-plugin:database-tools`. Use esse nome onde um nome de servidor configurado é esperado, como em um [campo `server` de um hook `mcp_tool`](https://code.claude.com/docs/pt/hooks#mcp-tool-hook-fields).

**Benefícios dos servidores MCP de plugins**:

* **Distribuição agrupada**: Ferramentas e servidores empacotados juntos
* **Configuração automática**: Nenhuma configuração MCP manual necessária
* **Consistência da equipe**: Todos obtêm as mesmas ferramentas quando o plugin está instalado

Veja a [referência de componentes de plugins](https://code.claude.com/docs/pt/plugins-reference#mcp-servers) para detalhes sobre como agrupar servidores MCP com plugins.

<h2 id="mcp-installation-scopes">
  Escopos de instalação de MCP
</h2>

Os servidores MCP podem ser configurados em três escopos. O escopo que você escolhe controla em quais projetos o servidor é carregado e se a configuração é compartilhada com sua equipe. Os administradores também podem implantar servidores no nível empresarial via [configuração gerenciada](#managed-mcp-configuration).

| Escopo                    | Carrega em             | Compartilhado com equipe    | Armazenado em                  |
| ------------------------- | ---------------------- | --------------------------- | ------------------------------ |
| [Local](#local-scope)     | Apenas projeto atual   | Não                         | `~/.claude.json`               |
| [Projeto](#project-scope) | Apenas projeto atual   | Sim, via controle de versão | `.mcp.json` na raiz do projeto |
| [Usuário](#user-scope)    | Todos os seus projetos | Não                         | `~/.claude.json`               |

<h3 id="local-scope">
  Escopo local
</h3>

O escopo local é o padrão. Um servidor com escopo local carrega apenas no projeto onde você o adicionou e permanece privado para você. Claude Code o armazena em `~/.claude.json` sob o caminho desse projeto, então o mesmo servidor não aparecerá em seus outros projetos. Use o escopo local para servidores de desenvolvimento pessoal, configurações experimentais ou servidores com credenciais que você não deseja no controle de versão.

<Note>
  O termo "escopo local" para servidores MCP difere das configurações locais gerais. Os servidores MCP com escopo local são armazenados em `~/.claude.json` (seu diretório inicial), enquanto as configurações locais gerais usam `.claude/settings.local.json` (no diretório do projeto). Veja [Configurações](https://code.claude.com/docs/pt/settings#settings-files) para detalhes sobre localizações de arquivos de configuração.
</Note>

```bash theme={null}
# Adicionar um servidor com escopo local (padrão)
claude mcp add --transport http stripe https://mcp.stripe.com

# Especificar explicitamente escopo local
claude mcp add --transport http stripe --scope local https://mcp.stripe.com
```

O comando escreve o servidor na entrada do seu projeto atual dentro de `~/.claude.json`. O exemplo abaixo mostra o resultado quando você o executa de `/path/to/your/project`:

```json theme={null}
{
  "projects": {
    "/path/to/your/project": {
      "mcpServers": {
        "stripe": {
          "type": "http",
          "url": "https://mcp.stripe.com"
        }
      }
    }
  }
}
```

<h3 id="project-scope">
  Escopo de projeto
</h3>

Servidores com escopo de projeto permitem colaboração em equipe armazenando configurações em um arquivo `.mcp.json` no diretório raiz do seu projeto. Este arquivo é projetado para ser verificado no controle de versão, garantindo que todos os membros da equipe tenham acesso às mesmas ferramentas e serviços MCP. Quando você adiciona um servidor com escopo de projeto, Claude Code cria ou atualiza automaticamente este arquivo com a estrutura de configuração apropriada.

```bash theme={null}
# Adicionar um servidor com escopo de projeto
claude mcp add --transport http paypal --scope project https://mcp.paypal.com/mcp
```

O arquivo `.mcp.json` resultante segue um formato padronizado:

```json theme={null}
{
  "mcpServers": {
    "shared-server": {
      "command": "/path/to/server",
      "args": [],
      "env": {}
    }
  }
}
```

Por razões de segurança, Claude Code solicita aprovação antes de usar servidores com escopo de projeto de arquivos `.mcp.json`. Se você precisar redefinir essas escolhas de aprovação, use o comando `claude mcp reset-project-choices`.

<h3 id="user-scope">
  Escopo de usuário
</h3>

Servidores com escopo de usuário são armazenados em `~/.claude.json` e fornecem acessibilidade entre projetos, tornando-os disponíveis em todos os projetos em sua máquina enquanto permanecem privados para sua conta de usuário. Este escopo funciona bem para servidores de utilitários pessoais, ferramentas de desenvolvimento ou serviços que você usa frequentemente em diferentes projetos.

```bash theme={null}
# Adicionar um servidor de usuário
claude mcp add --transport http hubspot --scope user https://mcp.hubspot.com/anthropic
```

<h3 id="scope-hierarchy-and-precedence">
  Hierarquia de escopo e precedência
</h3>

Quando o mesmo servidor é definido em mais de um lugar, Claude Code se conecta a ele uma vez, usando a definição da fonte com maior precedência. A entrada inteira do servidor dessa fonte é usada; os campos não são mesclados entre escopos.

1. Escopo local
2. Escopo de projeto
3. Escopo de usuário
4. [Servidores fornecidos por plugins](https://code.claude.com/docs/pt/plugins)
5. [Conectores claude.ai](#use-mcp-servers-from-claude-ai)

Os três escopos correspondem duplicatas por nome. Plugins e conectores correspondem por endpoint, então um que aponta para a mesma URL ou comando que um servidor acima é tratado como uma duplicata.

<h3 id="environment-variable-expansion-in-mcp-json">
  Expansão de variáveis de ambiente em `.mcp.json`
</h3>

Claude Code suporta expansão de variáveis de ambiente em arquivos `.mcp.json`, permitindo que equipes compartilhem configurações mantendo flexibilidade para caminhos específicos da máquina e valores sensíveis como chaves de API.

**Sintaxe suportada:**

* `${VAR}`: expande para o valor da variável de ambiente `VAR`
* `${VAR:-default}`: expande para `VAR` se definida, caso contrário usa `default`

**Locais de expansão:**
As variáveis de ambiente podem ser expandidas em:

* `command`: o caminho do executável do servidor
* `args`: argumentos de linha de comando
* `env`: variáveis de ambiente passadas para o servidor
* `url`: para tipos de servidor HTTP
* `headers`: para autenticação de servidor HTTP

**Exemplo com expansão de variável:**

```json theme={null}
{
  "mcpServers": {
    "api-server": {
      "type": "http",
      "url": "${API_BASE_URL:-https://api.example.com}/mcp",
      "headers": {
        "Authorization": "Bearer ${API_KEY}"
      }
    }
  }
}
```

Se uma variável de ambiente necessária não estiver definida e não tiver um valor padrão, Claude Code deixa o texto literal `${VAR}` no valor e relata um aviso de variável ausente para esse servidor. A configuração ainda é carregada, então defina a variável ou adicione um fallback `:-default` para que o servidor inicie com o valor que você pretende.

<h2 id="practical-examples">
  Exemplos práticos
</h2>

<h3 id="example-monitor-errors-with-sentry">
  Exemplo: Monitorar erros com Sentry
</h3>

```bash theme={null}
claude mcp add --transport http sentry https://mcp.sentry.dev/mcp
```

Autentique com sua conta Sentry:

```text theme={null}
/mcp
```

Então depure problemas de produção:

```text theme={null}
Quais são os erros mais comuns nas últimas 24 horas?
```

```text theme={null}
Mostre-me o rastreamento de pilha para o erro ID abc123
```

```text theme={null}
Qual implantação introduziu esses novos erros?
```

<h3 id="example-connect-to-github-for-code-reviews">
  Exemplo: Conectar ao GitHub para revisões de código
</h3>

O servidor MCP remoto do GitHub autentica com um token de acesso pessoal do GitHub passado como cabeçalho. Para obter um, abra suas [configurações de token do GitHub](https://github.com/settings/personal-access-tokens), gere um novo token refinado com acesso aos repositórios com os quais você deseja que Claude trabalhe, então adicione o servidor:

```bash theme={null}
claude mcp add --transport http github https://api.githubcopilot.com/mcp/ \
  --header "Authorization: Bearer YOUR_GITHUB_PAT"
```

Então trabalhe com GitHub:

```text theme={null}
Revise o PR #456 e sugira melhorias
```

```text theme={null}
Crie um novo problema para o bug que acabamos de encontrar
```

```text theme={null}
Mostre-me todos os PRs abertos atribuídos a mim
```

<h3 id="example-query-your-postgresql-database">
  Exemplo: Consultar seu banco de dados PostgreSQL
</h3>

```bash theme={null}
claude mcp add --transport stdio db -- npx -y @bytebase/dbhub \
  --dsn "postgresql://readonly:pass@prod.db.com:5432/analytics"
```

Então consulte seu banco de dados naturalmente:

```text theme={null}
Qual é nossa receita total este mês?
```

```text theme={null}
Mostre-me o esquema para a tabela de pedidos
```

```text theme={null}
Encontre clientes que não fizeram uma compra em 90 dias
```

<h2 id="authenticate-with-remote-mcp-servers">
  Autenticar com servidores MCP remotos
</h2>

Muitos servidores MCP baseados em nuvem exigem autenticação. Claude Code suporta OAuth 2.0 para conexões seguras.

Claude Code marca um servidor remoto como necessitando autenticação quando o servidor responde com `401 Unauthorized` ou `403 Forbidden`. Para um servidor no qual você ainda não fez login, qualquer código de status o sinaliza em `/mcp` para que você possa completar o fluxo OAuth.

Quando uma solicitação para um servidor OAuth no qual você já fez login retorna `401 Unauthorized`, Claude Code atualiza o token armazenado, reconecta e tenta a solicitação novamente uma vez. Ele sinaliza o servidor em `/mcp` apenas se essa tentativa também falhar. Antes da v2.1.206, uma atualização de token que falhava por um motivo transitório, como um erro de rede, sinalizava um servidor OAuth como necessitando autenticação pelo resto da sessão, mesmo que seu token de atualização ainda fosse válido.

A partir da v2.1.195, quando uma atualização de token falha porque o servidor rejeita o token de atualização armazenado, Claude Code imediatamente mostra um aviso apontando para `/mcp`. O menu do servidor conectado lá oferece Re-autenticar, para que você possa fazer login novamente antes que a próxima chamada de ferramenta falhe.

Um servidor personalizado que retorna um cabeçalho `WWW-Authenticate` apontando para seu servidor de autorização obtém a mesma descoberta automática que qualquer outro servidor remoto.

A partir da v2.1.193, Claude Code também mostra um aviso de inicialização quando um ou mais servidores configurados precisam de autenticação, para que você não tenha que abrir `/mcp` para descobrir quais servidores precisam de login.

No modo não interativo não há painel `/mcp`, então Claude Code não pode executar o fluxo OAuth para você. A partir da v2.1.196, quando um servidor configurado precisa de autenticação durante uma execução `claude -p` ou Agent SDK com [busca de ferramentas](#scale-with-mcp-tool-search) ativada, que é o padrão, Claude Code informa ao Claude que as ferramentas do servidor estão indisponíveis até que você o autorize. Claude pode então nomear o servidor que precisa de login em vez de responder como se o servidor não estivesse configurado. Complete o login de uma sessão interativa com `/mcp` ou `claude mcp login <name>`.

Se você configurou `headers.Authorization` para o servidor e o servidor rejeita esse cabeçalho, Claude Code relata a conexão como falha em vez de voltar para OAuth. Verifique se o token é válido para o endpoint MCP, ou remova o cabeçalho para usar o fluxo OAuth.

<Steps>
  <Step title="Adicione o servidor que requer autenticação">
    Por exemplo:

    ```bash theme={null}
    claude mcp add --transport http sentry https://mcp.sentry.dev/mcp
    ```
  </Step>

  <Step title="Use o comando /mcp dentro do Claude Code">
    No Claude Code, use o comando:

    ```text theme={null}
    /mcp
    ```

    Então siga os passos no seu navegador para fazer login.
  </Step>
</Steps>

<Tip>
  Dicas:

  * Os tokens de autenticação são armazenados com segurança e atualizados automaticamente
  * Use "Clear authentication" no menu `/mcp` para revogar acesso
  * Se seu navegador não abrir automaticamente, copie a URL fornecida e abra-a manualmente
  * Se o redirecionamento do navegador falhar com um erro de conexão após autenticar, cole a URL de callback completa da barra de endereços do seu navegador no prompt de URL que aparece no Claude Code
  * A autenticação OAuth funciona com servidores HTTP
</Tip>

<h3 id="authenticate-from-the-command-line">
  Autenticar a partir da linha de comando
</h3>

A partir da v2.1.186, `claude mcp login <name>` executa o fluxo OAuth de um servidor configurado diretamente do seu shell, para que você não precise abrir o painel `/mcp` dentro de uma sessão.

```bash theme={null}
claude mcp login sentry
```

Para limpar credenciais armazenadas depois, execute `claude mcp logout <name>`.

A partir da v2.1.191, o comando detecta quando nenhum navegador local está disponível, como durante uma sessão SSH ou no Linux sem um servidor de exibição, e imprime a URL de autorização em vez de tentar abrir um navegador. Abra a URL na sua máquina local, depois cole a URL de redirecionamento completa da barra de endereços do seu navegador de volta no prompt. O comando precisa de um terminal interativo para a etapa de colagem, então conecte com `ssh -t`. Passe `--no-browser` para forçar o prompt de URL mesmo quando um navegador local é detectado.

```bash theme={null}
claude mcp login sentry --no-browser
```

<h3 id="use-a-fixed-oauth-callback-port">
  Usar uma porta de callback OAuth fixa
</h3>

Alguns servidores MCP exigem um URI de redirecionamento específico registrado antecipadamente. Por padrão, Claude Code escolhe uma porta aleatória disponível para o callback OAuth. Use `--callback-port` para fixar a porta para que corresponda a um URI de redirecionamento pré-registrado do formulário `http://localhost:PORT/callback`.

Você pode usar `--callback-port` sozinho (com registro dinâmico de cliente) ou junto com `--client-id` (com credenciais pré-configuradas).

```bash theme={null}
# Porta de callback fixa com registro dinâmico de cliente
claude mcp add --transport http \
  --callback-port 8080 \
  my-server https://mcp.example.com/mcp
```

<h3 id="use-pre-configured-oauth-credentials">
  Usar credenciais OAuth pré-configuradas
</h3>

Alguns servidores MCP não suportam configuração automática de OAuth via Registro Dinâmico de Cliente. Se você vir um erro como "Incompatible auth server: does not support dynamic client registration," o servidor requer credenciais pré-configuradas. Claude Code também suporta servidores que usam um Documento de Metadados de ID do Cliente (CIMD) em vez de Registro Dinâmico de Cliente, e descobre esses automaticamente. Se a descoberta automática falhar, registre um aplicativo OAuth através do portal do desenvolvedor do servidor primeiro, depois forneça as credenciais ao adicionar o servidor.

<Steps>
  <Step title="Registre um aplicativo OAuth com o servidor">
    Crie um aplicativo através do portal do desenvolvedor do servidor e anote seu ID do cliente e segredo do cliente.

    Muitos servidores também exigem um URI de redirecionamento. Se assim for, escolha uma porta e registre um URI de redirecionamento no formato `http://localhost:PORT/callback`. Use essa mesma porta com `--callback-port` na próxima etapa.
  </Step>

  <Step title="Adicione o servidor com suas credenciais">
    Escolha um dos seguintes métodos. A porta usada para `--callback-port` pode ser qualquer porta disponível. Ela apenas precisa corresponder ao URI de redirecionamento que você registrou na etapa anterior.

    <Tabs>
      <Tab title="claude mcp add">
        Use `--client-id` para passar o ID do cliente do seu aplicativo. A flag `--client-secret` solicita o segredo com entrada mascarada:

        ```bash theme={null}
        claude mcp add --transport http \
          --client-id your-client-id --client-secret --callback-port 8080 \
          my-server https://mcp.example.com/mcp
        ```
      </Tab>

      <Tab title="claude mcp add-json">
        Inclua o objeto `oauth` na configuração JSON e passe `--client-secret` como uma flag separada:

        ```bash theme={null}
        claude mcp add-json my-server \
          '{"type":"http","url":"https://mcp.example.com/mcp","oauth":{"clientId":"your-client-id","callbackPort":8080}}' \
          --client-secret
        ```
      </Tab>

      <Tab title="claude mcp add-json (apenas porta de callback)">
        Use `--callback-port` sem um ID de cliente para fixar a porta enquanto usa registro dinâmico de cliente:

        ```bash theme={null}
        claude mcp add-json my-server \
          '{"type":"http","url":"https://mcp.example.com/mcp","oauth":{"callbackPort":8080}}'
        ```
      </Tab>

      <Tab title="CI / variável de ambiente">
        Defina o segredo via variável de ambiente para pular o prompt interativo:

        ```bash theme={null}
        MCP_CLIENT_SECRET=your-secret claude mcp add --transport http \
          --client-id your-client-id --client-secret --callback-port 8080 \
          my-server https://mcp.example.com/mcp
        ```
      </Tab>
    </Tabs>
  </Step>

  <Step title="Autentique no Claude Code">
    Execute `/mcp` no Claude Code e siga o fluxo de login do navegador.
  </Step>
</Steps>

<Tip>
  Dicas:

  * O segredo do cliente é armazenado com segurança no seu chaveiro do sistema (macOS) ou em um arquivo de credenciais, não na sua configuração
  * Se o servidor usar um cliente OAuth público sem segredo, use apenas `--client-id` sem `--client-secret`
  * `--callback-port` pode ser usado com ou sem `--client-id`
  * Essas flags se aplicam apenas aos transportes HTTP e SSE. Elas não têm efeito em servidores stdio
  * Use `claude mcp get <name>` para verificar se as credenciais OAuth estão configuradas para um servidor
</Tip>

<h3 id="override-oauth-metadata-discovery">
  Substituir descoberta de metadados OAuth
</h3>

Aponte Claude Code para uma URL de metadados específica de servidor de autorização OAuth para contornar a cadeia de descoberta padrão. Defina `authServerMetadataUrl` quando os endpoints padrão do servidor MCP falharem, ou quando você deseja rotear a descoberta através de um proxy interno. Por padrão, Claude Code primeiro verifica os Metadados de Recurso Protegido RFC 9728 em `/.well-known/oauth-protected-resource`, depois volta para os metadados do servidor de autorização RFC 8414 em `/.well-known/oauth-authorization-server`.

Defina `authServerMetadataUrl` no objeto `oauth` da configuração do seu servidor em `.mcp.json`:

```json theme={null}
{
  "mcpServers": {
    "my-server": {
      "type": "http",
      "url": "https://mcp.example.com/mcp",
      "oauth": {
        "authServerMetadataUrl": "https://auth.example.com/.well-known/openid-configuration"
      }
    }
  }
}
```

A URL deve usar `https://`. Os `scopes_supported` da URL de metadados substituem os escopos que o servidor upstream anuncia.

<h3 id="restrict-oauth-scopes">
  Restringir escopos OAuth
</h3>

Defina `oauth.scopes` para fixar os escopos que Claude Code solicita durante o fluxo de autorização. Esta é a forma suportada de restringir um servidor MCP a um subconjunto aprovado pela equipe de segurança quando o servidor de autorização upstream anuncia mais escopos do que você deseja conceder. O valor é uma única string separada por espaço, correspondendo ao formato do parâmetro `scope` em RFC 6749 §3.3.

```json theme={null}
{
  "mcpServers": {
    "slack": {
      "type": "http",
      "url": "https://mcp.slack.com/mcp",
      "oauth": {
        "scopes": "channels:read chat:write search:read"
      }
    }
  }
}
```

`oauth.scopes` tem precedência sobre `authServerMetadataUrl` e os escopos que o servidor descobre em `/.well-known`. Deixe-o indefinido para permitir que o servidor MCP determine o conjunto de escopos solicitado.

A partir da v2.1.196, quando `oauth.scopes` não está definido, Claude Code solicita o escopo fornecido pelo cabeçalho `WWW-Authenticate` do servidor ou seus metadados de recurso protegido, e não envia nenhum parâmetro `scope` quando nenhum dos dois fornece um. Ele não solicita mais o catálogo completo de `scopes_supported` dos metadados do servidor de autorização descobertos automaticamente. Solicitar esse catálogo fez com que provedores de identidade que anunciam escopos apenas para administrador ou escopos de modelo rejeitassem a solicitação de autorização com um erro `invalid_scope`. Os metadados obtidos de um `authServerMetadataUrl` configurado ainda fornecem seus `scopes_supported` como os escopos solicitados.

Se o servidor de autorização anuncia `offline_access` em `scopes_supported`, Claude Code o acrescenta aos escopos fixados para que o token de acesso possa ser atualizado sem um novo login no navegador.

Se o servidor depois retorna um 403 `insufficient_scope` para uma chamada de ferramenta, Claude Code se autentica novamente com os mesmos escopos fixados. Amplie `oauth.scopes` quando uma ferramenta que você precisa requer um escopo fora do conjunto fixado.

<h3 id="use-dynamic-headers-for-custom-authentication">
  Usar cabeçalhos dinâmicos para autenticação personalizada
</h3>

Se seu servidor MCP usar um esquema de autenticação diferente de OAuth, como Kerberos, tokens de curta duração ou um SSO interno, use `headersHelper` para gerar cabeçalhos de solicitação no momento da conexão. Claude Code executa o comando e mescla sua saída nos cabeçalhos de conexão.

```json theme={null}
{
  "mcpServers": {
    "internal-api": {
      "type": "http",
      "url": "https://mcp.internal.example.com",
      "headersHelper": "/opt/bin/get-mcp-auth-headers.sh"
    }
  }
}
```

O comando também pode ser inline:

```json theme={null}
{
  "mcpServers": {
    "internal-api": {
      "type": "http",
      "url": "https://mcp.internal.example.com",
      "headersHelper": "echo '{\"Authorization\": \"Bearer '\"$(get-token)\"'\"}'"
    }
  }
}
```

**Requisitos:**

* O comando deve escrever um objeto JSON de pares chave-valor de string para stdout
* O comando é executado em um shell com um tempo limite de 10 segundos, a partir do diretório de trabalho atual da sessão. Use um caminho absoluto ou um comando em `PATH` para o script
* Cabeçalhos dinâmicos substituem qualquer `headers` estático com o mesmo nome

O auxiliar é executado novamente em cada conexão, no início da sessão e ao reconectar. Não há cache, então seu script é responsável por qualquer reutilização de token.

A partir da v2.1.193, se uma chamada de ferramenta retorna `401 Unauthorized` ou `403 Forbidden`, Claude Code automaticamente executa novamente o auxiliar, reconecta com os cabeçalhos atualizados e tenta novamente a chamada uma vez. Claude Code marca o servidor como necessitando autenticação em `/mcp` apenas se essa tentativa também falhar.

Claude Code define essas variáveis de ambiente ao executar o auxiliar:

| Variável                      | Valor                                                                                                                |
| :---------------------------- | :------------------------------------------------------------------------------------------------------------------- |
| `CLAUDE_CODE_MCP_SERVER_NAME` | o nome do servidor MCP                                                                                               |
| `CLAUDE_CODE_MCP_SERVER_URL`  | a URL do servidor MCP                                                                                                |
| `CLAUDE_PLUGIN_ROOT`          | o diretório raiz do plugin. Definido apenas quando um [plugin](https://code.claude.com/docs/pt/plugins-reference#mcp-servers) fornece o servidor |

Use essas para escrever um único script auxiliar que serve múltiplos servidores MCP.

Para um servidor fornecido por plugin, o auxiliar também é executado com seu diretório de trabalho definido para a raiz do plugin, para que um caminho `headersHelper` relativo seja resolvido dentro do diretório do plugin em vez de contra o diretório de trabalho da sessão. Requer Claude Code v2.1.195 ou posterior.

Um `headersHelper` fornecido por plugin não pode referenciar os valores [`${user_config.*}`](https://code.claude.com/docs/pt/plugins-reference#user-configuration) do plugin, porque o comando é executado através de um shell. Claude Code relata o servidor como mal configurado com um [erro](https://code.claude.com/docs/pt/errors#plugin-command-references-user-config) e não substitui o valor. Coloque `${user_config.KEY}` no campo `headers` do servidor, que não é analisado por shell, ou faça o script auxiliar ler o valor de seu próprio ambiente ou de um arquivo de configuração. Antes da v2.1.207, `headersHelper` substituía valores `${user_config.*}`.

<Note>
  `headersHelper` executa comandos shell arbitrários. Quando definido no escopo de projeto ou local, ele só é executado após você aceitar o diálogo de confiança do espaço de trabalho.
</Note>

<h2 id="add-mcp-servers-from-json-configuration">
  Adicionar servidores MCP de configuração JSON
</h2>

Se você tiver uma configuração JSON para um servidor MCP, você pode adicioná-la diretamente:

<Steps>
  <Step title="Adicione um servidor MCP de JSON">
    ```bash theme={null}
    # Sintaxe básica
    claude mcp add-json <name> '<json>'

    # Exemplo: Adicionar um servidor HTTP com configuração JSON
    claude mcp add-json weather-api '{"type":"http","url":"https://api.weather.com/mcp","headers":{"Authorization":"Bearer token"}}'

    # Exemplo: Adicionar um servidor stdio com configuração JSON
    claude mcp add-json local-weather '{"type":"stdio","command":"/path/to/weather-cli","args":["--api-key","abc123"],"env":{"CACHE_DIR":"/tmp"}}'

    # Exemplo: Adicionar um servidor HTTP com credenciais OAuth pré-configuradas
    claude mcp add-json my-server '{"type":"http","url":"https://mcp.example.com/mcp","oauth":{"clientId":"your-client-id","callbackPort":8080}}' --client-secret
    ```
  </Step>

  <Step title="Verifique se o servidor foi adicionado">
    ```bash theme={null}
    claude mcp get weather-api
    ```
  </Step>
</Steps>

<Tip>
  Dicas:

  * Certifique-se de que o JSON está adequadamente escapado no seu shell
  * O JSON deve estar em conformidade com o esquema de configuração do servidor MCP
  * Você pode usar `--scope user` para adicionar o servidor à sua configuração de usuário em vez da específica do projeto
</Tip>

<h2 id="import-mcp-servers-from-claude-desktop">
  Importar servidores MCP do Claude Desktop
</h2>

Se você já configurou servidores MCP no Claude Desktop, você pode importá-los:

<Steps>
  <Step title="Importe servidores do Claude Desktop">
    ```bash theme={null}
    # Sintaxe básica 
    claude mcp add-from-claude-desktop 
    ```
  </Step>

  <Step title="Selecione quais servidores importar">
    Após executar o comando, você verá um diálogo interativo que permite selecionar quais servidores você deseja importar.
  </Step>

  <Step title="Verifique se os servidores foram importados">
    ```bash theme={null}
    claude mcp list 
    ```
  </Step>
</Steps>

Os nomes de servidores adicionados através de comandos `claude mcp` podem conter apenas letras, números, hífens e sublinhados. O Claude Desktop não aplica essa restrição, portanto um servidor do Claude Desktop cujo nome contém qualquer outro caractere, como um espaço, não pode ser importado. A importação relata cada nome que rejeita e ainda importa os outros servidores que você selecionou. Antes da v2.1.205, o primeiro nome inválido interrompia a importação e nenhum dos servidores selecionados era adicionado.

<Tip>
  Dicas:

  * Este recurso funciona apenas em macOS e Windows Subsystem for Linux (WSL)
  * Ele lê o arquivo de configuração do Claude Desktop de sua localização padrão nessas plataformas
  * Use a flag `--scope user` para adicionar servidores à sua configuração de usuário
  * Os servidores importados mantêm os mesmos nomes que no Claude Desktop quando o nome contém apenas letras, números, hífens e sublinhados. O Claude Code relata um servidor cujo nome contém qualquer outro caractere e o ignora
  * Se servidores com os mesmos nomes já existirem, eles receberão um sufixo numérico (por exemplo, `server_1`)
</Tip>

<h2 id="use-mcp-servers-from-claude-ai">
  Usar servidores MCP do claude.ai
</h2>

Se você fez login no Claude Code com uma conta [claude.ai](https://claude.ai), os servidores MCP que você adicionou no claude.ai, conhecidos como [conectores](https://claude.com/docs/connectors), estão automaticamente disponíveis no Claude Code:

<Steps>
  <Step title="Configure servidores MCP no claude.ai">
    Adicione servidores em [claude.ai/customize/connectors](https://claude.ai/customize/connectors). Em planos Team e Enterprise, apenas administradores podem adicionar servidores.
  </Step>

  <Step title="Autentique o servidor MCP">
    Complete quaisquer etapas de autenticação necessárias no claude.ai.
  </Step>

  <Step title="Visualize e gerencie servidores no Claude Code">
    No Claude Code, use o comando:

    ```text theme={null}
    /mcp
    ```

    Os servidores do claude.ai aparecem na lista com indicadores mostrando que vêm do claude.ai.
  </Step>
</Steps>

A partir da v2.1.161, conectores aos quais você nunca fez login estão recolhidos atrás de uma linha `Show unused connectors` no final da seção claude.ai, para que uma lista provisionada pela organização não preencha o painel. Selecione a linha para expandi-los. Um conector ao qual você fez login antes permanece visível mesmo quando atualmente precisa de re-autenticação.

Os conectores do claude.ai são buscados apenas quando seu [método de autenticação](https://code.claude.com/docs/pt/authentication#authentication-precedence) ativo é sua assinatura do claude.ai. Eles não são carregados quando `ANTHROPIC_API_KEY`, `ANTHROPIC_AUTH_TOKEN`, `apiKeyHelper`, ou um provedor de terceiros como Amazon Bedrock ou Google Cloud's Agent Platform está ativo, mesmo que você tenha executado `/login` anteriormente.

Se `/mcp` não listar um conector que você adicionou, execute `/status` para confirmar qual método de autenticação está ativo, desdefina essa variável de ambiente ou remova a configuração `apiKeyHelper`, depois execute `/login` para selecionar sua conta do claude.ai.

Um servidor que você adicionou no Claude Code tem [precedência](#scope-hierarchy-and-precedence) sobre um conector do claude.ai que aponta para a mesma URL. Quando isso acontece, `/mcp` lista o conector como oculto e mostra como remover a duplicata se você preferir usar o conector.

Alguns conectores hospedados pela Anthropic, como Microsoft 365, Gmail e Google Calendar, não suportam OAuth local do Claude Code porque o provedor de identidade upstream aceita apenas a URL de redirecionamento que o claude.ai registrou. A partir da v2.1.162, autenticar um desses hosts em `/mcp` mostra uma mensagem direcionando você para conectá-lo em Configurações → Conectores no claude.ai. Uma vez conectado lá, o conector aparece no Claude Code automaticamente.

<h3 id="organization-controls-on-connector-tools">
  Controles da organização em ferramentas de conectores
</h3>

Sua organização pode definir controles por ferramenta em [conectores do claude.ai](https://claude.com/docs/connectors). O Claude Code lê essas configurações na inicialização e as aplica localmente. Execute `/mcp` para ver qual configuração se aplica a cada ferramenta em um conector.

* **Ferramenta definida como `ask`**: O Claude Code solicita em cada chamada com o motivo `Your organization requires approval for this tool`. O prompt aparece mesmo em [modos de permissão](https://code.claude.com/docs/pt/permissions#permission-modes) `acceptEdits`, `auto` e `bypassPermissions`, e nunca oferece uma opção para lembrar sua escolha. [Regras de permissão](https://code.claude.com/docs/pt/permissions) que correspondem à ferramenta também não pulam o prompt. No modo `dontAsk`, que nunca solicita, o Claude Code nega a chamada.
* **Ferramenta definida como `blocked`**: O Claude Code filtra a ferramenta antes do Claude vê-la, então ela nunca aparece na lista de ferramentas.

Aplicar esses controles requer Claude Code v2.1.129 ou posterior. Versões anteriores ignoram as configurações e aplicam o fluxo de permissão padrão.

<h3 id="disable-claude-ai-connectors">
  Desabilitar conectores do claude.ai
</h3>

Para desabilitar servidores MCP do claude.ai no Claude Code, defina [`disableClaudeAiConnectors`](https://code.claude.com/docs/pt/settings#available-settings) como `true` em qualquer escopo de configurações:

```json theme={null}
{
  "disableClaudeAiConnectors": true
}
```

Esta configuração usa semântica any-source-true: `true` em qualquer fonte de configurações tem precedência. Um `.claude/settings.json` de projeto verificado pode optar um repositório por fora de conectores em nuvem, mas um `false` em nível de projeto não pode re-habilitar conectores que um `true` em nível de usuário ou política desabilitou. Servidores passados explicitamente via `--mcp-config` não são afetados.

Você também pode definir a variável de ambiente `ENABLE_CLAUDEAI_MCP_SERVERS` como `false`, que tem o mesmo efeito para a sessão de shell atual:

```bash theme={null}
ENABLE_CLAUDEAI_MCP_SERVERS=false claude
```

Para bloquear conectores individuais do claude.ai em vez de todos eles, adicione-os a [`deniedMcpServers`](https://code.claude.com/docs/pt/managed-mcp) por nome ou por padrão de URL. Por exemplo, uma entrada `serverName` de `"claude.ai Slack"` bloqueia o conector Slack. Para alternar um conector ligado ou desligado apenas para o projeto atual, use o painel `/mcp`.

<Note>
  Estas configurações do lado do cliente governam sessões locais do Claude Code. Em sessões do [Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web), conectores do claude.ai são provisionados pelo host remoto e chegam como entradas explícitas `--mcp-config`, então `disableClaudeAiConnectors` não se aplica lá. URLs de conectores também são reescritas através do proxy de sessão, então um padrão `serverUrl` de `deniedMcpServers` direcionado à URL do fornecedor não corresponderá. Gerencie quais conectores uma sessão em nuvem pode usar a partir das configurações da sua organização no claude.ai.
</Note>

<h2 id="use-claude-code-as-an-mcp-server">
  Usar Claude Code como um servidor MCP
</h2>

Você pode usar Claude Code em si como um servidor MCP que outros aplicativos podem se conectar:

```bash theme={null}
# Inicie Claude como um servidor MCP stdio
claude mcp serve
```

Você pode usar isso no Claude Desktop adicionando esta configuração ao claude\_desktop\_config.json:

```json theme={null}
{
  "mcpServers": {
    "claude-code": {
      "type": "stdio",
      "command": "claude",
      "args": ["mcp", "serve"],
      "env": {}
    }
  }
}
```

<Warning>
  **Configurando o caminho do executável**: O campo `command` deve referenciar o executável do Claude Code. Se o comando `claude` não estiver no PATH do seu sistema, você precisará especificar o caminho completo para o executável.

  Para encontrar o caminho completo:

  ```bash theme={null}
  which claude
  ```

  Então use o caminho completo na sua configuração:

  ```json theme={null}
  {
    "mcpServers": {
      "claude-code": {
        "type": "stdio",
        "command": "/full/path/to/claude",
        "args": ["mcp", "serve"],
        "env": {}
      }
    }
  }
  ```

  Sem o caminho correto do executável, você encontrará erros como `spawn claude ENOENT`.
</Warning>

<Tip>
  Dicas:

  * O servidor fornece acesso às ferramentas do Claude como View, Edit, LS, etc.
  * No Claude Desktop, tente pedir ao Claude para ler arquivos em um diretório, fazer edições e muito mais.
  * Este servidor MCP apenas expõe as ferramentas do Claude Code ao seu cliente MCP, portanto seu próprio cliente é responsável por implementar confirmação do usuário para chamadas de ferramentas individuais.
</Tip>

<h2 id="mcp-output-limits-and-warnings">
  Limites de saída MCP e avisos
</h2>

Quando as ferramentas MCP produzem grandes saídas, Claude Code ajuda a gerenciar o uso de tokens para evitar sobrecarregar seu contexto de conversa:

* **Limite de aviso de saída**: Claude Code exibe um aviso quando qualquer saída de ferramenta MCP excede 10.000 tokens
* **Limite configurável**: você pode ajustar o máximo de tokens de saída MCP permitidos usando a variável de ambiente `MAX_MCP_OUTPUT_TOKENS`
* **Limite padrão**: o máximo padrão é 25.000 tokens
* **Escopo**: a variável de ambiente se aplica a ferramentas que não declaram seu próprio limite. Ferramentas que definem [`anthropic/maxResultSizeChars`](#raise-the-limit-for-a-specific-tool) usam esse valor em vez disso para conteúdo de texto, independentemente do que `MAX_MCP_OUTPUT_TOKENS` está definido. Ferramentas que retornam dados de imagem ainda estão sujeitas a `MAX_MCP_OUTPUT_TOKENS`

Para aumentar o limite para ferramentas que produzem grandes saídas:

```bash theme={null}
export MAX_MCP_OUTPUT_TOKENS=50000
claude
```

Isso é particularmente útil ao trabalhar com servidores MCP que:

* Consultam grandes conjuntos de dados ou bancos de dados
* Geram relatórios ou documentação detalhados
* Processam arquivos de log extensos ou informações de depuração

<h3 id="raise-the-limit-for-a-specific-tool">
  Aumentar o limite para uma ferramenta específica
</h3>

Se você está construindo um servidor MCP, você pode permitir que ferramentas individuais retornem resultados maiores do que o limite padrão de persistência em disco definindo `_meta["anthropic/maxResultSizeChars"]` na entrada da ferramenta em resposta `tools/list`. Claude Code aumenta o limite dessa ferramenta para o valor anotado, até um teto rígido de 500.000 caracteres.

Isso é útil para ferramentas que retornam saídas inerentemente grandes mas necessárias, como esquemas de banco de dados ou árvores de arquivos completas. Sem a anotação, resultados que excedem o limite padrão são persistidos em disco e substituídos por uma referência de arquivo na conversa.

```json theme={null}
{
  "name": "get_schema",
  "description": "Returns the full database schema",
  "_meta": {
    "anthropic/maxResultSizeChars": 200000
  }
}
```

A anotação se aplica independentemente de `MAX_MCP_OUTPUT_TOKENS` para conteúdo de texto, então os usuários não precisam aumentar a variável de ambiente para ferramentas que a declaram. Ferramentas que retornam dados de imagem ainda estão sujeitas ao limite de token.

<Warning>
  Se você encontrar frequentemente avisos de saída com servidores MCP específicos que você não controla, considere aumentar o limite `MAX_MCP_OUTPUT_TOKENS`. Você também pode pedir ao autor do servidor para adicionar a anotação `anthropic/maxResultSizeChars` ou para paginar suas respostas. A anotação não tem efeito em ferramentas que retornam conteúdo de imagem; para essas, aumentar `MAX_MCP_OUTPUT_TOKENS` é a única opção.
</Warning>

<h2 id="tool-input-schemas-with-a-root-level-combinator">
  Esquemas de entrada de ferramenta com um combinador de nível raiz
</h2>

Alguns servidores MCP declaram o esquema de entrada de uma ferramenta como uma união JSON Schema, com `anyOf`, `oneOf`, ou `allOf` no nível superior do esquema. A API Claude não aceita essas palavras-chave na raiz do esquema. Ela aceita combinadores aninhados dentro de `properties`, que Claude Code envia sem modificação.

A partir do Claude Code v2.1.195, ferramentas com um combinador de nível raiz permanecem disponíveis. Antes de enviar a ferramenta para a API, Claude Code achata o esquema em um único objeto e prepara uma frase à descrição da ferramenta que diz ao Claude quais grupos de parâmetros pertencem juntos:

* `allOf`: propriedades de cada ramo são mescladas, e a lista `required` de cada ramo ainda se aplica
* `anyOf` e `oneOf`: propriedades de cada ramo são mescladas, e a lista `required` de cada ramo é descrita na descrição da ferramenta em vez de ser aplicada pelo esquema

Seu servidor recebe quaisquer argumentos que Claude escolheu, então continue validando a combinação no lado do servidor.

Quando Claude Code não consegue produzir um esquema que a API aceita, ou em uma implantação que não recebe a configuração remota que ativa a reescrita, como uma máquina offline, ele ignora essa ferramenta, registra o motivo no log do servidor e deixa as outras ferramentas do servidor disponíveis. Versões anteriores a v2.1.195 ignoram cada ferramenta cujo esquema de entrada tem um `anyOf`, `oneOf`, ou `allOf` de nível raiz.

<h2 id="require-approval-for-a-specific-tool">
  Exigir aprovação para uma ferramenta específica
</h2>

Se você está construindo um servidor MCP, você pode marcar uma ferramenta como exigindo aprovação explícita a cada chamada definindo `_meta["anthropic/requiresUserInteraction"]` como `true` na entrada da ferramenta em resposta `tools/list`. O valor deve ser o booleano JSON `true`; qualquer outro valor é ignorado.

Claude Code mostra o prompt de permissão dessa ferramenta a cada chamada, mesmo em modos de permissão `acceptEdits`, `auto` e `bypassPermissions` [permission modes](https://code.claude.com/docs/pt/permissions#permission-modes), e não oferece uma opção "não pergunte novamente" para ela. [Regras de permissão](https://code.claude.com/docs/pt/permissions#permission-rule-syntax) que correspondem à ferramenta também não pulam o prompt. No modo `dontAsk`, que nunca solicita, Claude Code nega a chamada em vez disso.

O prompt tem que alcançar uma pessoa. No modo não interativo com [`--permission-prompt-tool`](https://code.claude.com/docs/pt/cli-reference#cli-flags), um resultado `allow` da ferramenta de prompt para uma ferramenta sinalizada é convertido em uma negação com a mensagem `MCP tool requires user interaction; not supported via --permission-prompt-tool`. O callback [`canUseTool`](https://code.claude.com/docs/pt/agent-sdk/permissions) do Agent SDK recebe essas chamadas e pode aprová-las, porque o host do SDK deve mostrá-las a um usuário.

Use isso para ferramentas cujo prompt de permissão é em si o ponto, como uma etapa de consentimento ou concessão de acesso onde aprovação automática significaria que nenhum humano nunca concordou. Outras ferramentas do mesmo servidor mantêm seu comportamento de permissão normal.

A seguinte entrada `tools/list` marca uma ferramenta como sempre exigindo aprovação.

```json theme={null}
{
  "name": "grant_access",
  "description": "Requests access to a protected resource",
  "_meta": {
    "anthropic/requiresUserInteraction": true
  }
}
```

A anotação `anthropic/requiresUserInteraction` requer Claude Code v2.1.199 ou posterior. Versões anteriores a ignoram e aplicam o fluxo de permissão padrão.

Quando uma sessão está conectada ao [Remote Control](https://code.claude.com/docs/pt/remote-control) ou a um host SDK, Claude Code marca a solicitação de permissão como exigindo interação do usuário, para que o cliente mostre o prompt de permissão da ferramenta para você responder em vez de uma ação de aprovação com um toque.

<h2 id="respond-to-mcp-elicitation-requests">
  Responder a solicitações de elicitação MCP
</h2>

Os servidores MCP podem solicitar entrada estruturada de você durante uma tarefa usando elicitação. Quando um servidor precisa de informações que não consegue obter por conta própria, Claude Code exibe um diálogo interativo e passa sua resposta de volta para o servidor. Nenhuma configuração é necessária do seu lado: diálogos de elicitação aparecem automaticamente quando um servidor os solicita.

Os servidores podem solicitar entrada de duas maneiras:

* **Modo de formulário**: Claude Code mostra um diálogo com campos de formulário definidos pelo servidor (por exemplo, um prompt de nome de usuário e senha). Preencha os campos e envie.
* **Modo de URL**: Claude Code abre uma URL do navegador para autenticação ou aprovação. Complete o fluxo no navegador, depois confirme no CLI.

Para responder automaticamente a solicitações de elicitação sem mostrar um diálogo, use o [hook `Elicitation`](https://code.claude.com/docs/pt/hooks#elicitation).

Se você está construindo um servidor MCP que usa elicitação, veja a [especificação de elicitação MCP](https://modelcontextprotocol.io/docs/learn/client-concepts#elicitation) para detalhes de protocolo e exemplos de esquema.

<h2 id="use-mcp-resources">
  Usar recursos MCP
</h2>

Os servidores MCP podem expor recursos que você pode referenciar usando menções @, semelhante a como você referencia arquivos.

<h3 id="reference-mcp-resources">
  Referenciar recursos MCP
</h3>

<Steps>
  <Step title="Liste recursos disponíveis">
    Digite `@` no seu prompt para ver recursos disponíveis de todos os servidores MCP conectados. Os recursos aparecem junto com arquivos no menu de preenchimento automático.
  </Step>

  <Step title="Referencie um recurso específico">
    Use o formato `@server:protocol://resource/path` para referenciar um recurso:

    ```text theme={null}
    Você pode analisar @github:issue://123 e sugerir uma correção?
    ```

    ```text theme={null}
    Por favor, revise a documentação da API em @docs:file://api/authentication
    ```
  </Step>

  <Step title="Múltiplas referências de recursos">
    Você pode referenciar múltiplos recursos em um único prompt:

    ```text theme={null}
    Compare @postgres:schema://users com @docs:file://database/user-model
    ```
  </Step>
</Steps>

<Tip>
  Dicas:

  * Os recursos são automaticamente buscados e incluídos como anexos quando referenciados
  * Os caminhos dos recursos são pesquisáveis por correspondência aproximada no preenchimento automático de menção @
  * Claude Code fornece automaticamente ferramentas para listar e ler recursos MCP quando os servidores os suportam
  * Os recursos podem conter qualquer tipo de conteúdo que o servidor MCP fornece (texto, JSON, dados estruturados, etc.)
</Tip>

<h2 id="scale-with-mcp-tool-search">
  Escalar com MCP Tool Search
</h2>

Tool Search mantém o uso de contexto MCP baixo adiando definições de ferramentas até que Claude precise delas. Apenas nomes de ferramentas e instruções do servidor são carregados no início da sessão, então adicionar mais servidores MCP tem impacto mínimo na sua janela de contexto. Claude Code não impõe um limite fixo de ferramentas por servidor; o limite prático é o seu orçamento de janela de contexto.

<h3 id="how-it-works">
  Como funciona
</h3>

Tool Search é ativado por padrão. As ferramentas MCP são adiadas em vez de carregadas no contexto antecipadamente, e Claude usa uma ferramenta de pesquisa para descobrir as relevantes quando uma tarefa precisa delas. Apenas as ferramentas que Claude realmente usa entram no contexto. Da sua perspectiva, as ferramentas MCP funcionam exatamente como antes.

Se você preferir carregamento baseado em limite, defina `ENABLE_TOOL_SEARCH=auto` para carregar esquemas antecipadamente quando se encaixarem em 10% da janela de contexto e adiar apenas o excesso. Veja [Configurar pesquisa de ferramentas](#configure-tool-search) para todas as opções.

<h3 id="for-mcp-server-authors">
  Para autores de servidores MCP
</h3>

Se você está construindo um servidor MCP, o campo de instruções do servidor se torna mais útil com Tool Search habilitado. As instruções do servidor ajudam Claude a entender quando pesquisar suas ferramentas, semelhante a como [skills](https://code.claude.com/docs/pt/skills) funcionam.

Adicione instruções de servidor claras e descritivas que expliquem:

* Que categoria de tarefas suas ferramentas lidam
* Quando Claude deve pesquisar suas ferramentas
* Capacidades principais do seu servidor

Claude Code trunca descrições de ferramentas e instruções de servidor em 2KB cada. Mantenha-as concisas para evitar truncamento, e coloque detalhes críticos perto do início.

<h3 id="configure-tool-search">
  Configurar pesquisa de ferramentas
</h3>

Tool Search é ativado por padrão: as ferramentas MCP são adiadas e descobertas sob demanda. Claude Code desabilita-o por padrão na Plataforma de Agentes do Google Cloud. Também é desabilitado quando `ANTHROPIC_BASE_URL` aponta para um host que não é de primeira parte, já que a maioria dos proxies não encaminha blocos `tool_reference`. Defina `ENABLE_TOOL_SEARCH` explicitamente para substituir qualquer fallback.

Definir [`CLAUDE_CODE_DISABLE_EXPERIMENTAL_BETAS`](https://code.claude.com/docs/pt/env-vars) mantém tool search desativado, e `ENABLE_TOOL_SEARCH` não pode substituí-lo. A variável remove o cabeçalho beta que as definições de ferramentas `defer_loading` e blocos de conteúdo `tool_reference` exigem.

Tool Search requer um modelo que suporte blocos `tool_reference`: Claude Sonnet 4.5, Claude Haiku 4.5, Claude Opus 4.5 e modelos posteriores. Veja [compatibilidade de modelo na documentação da API](https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-search-tool#model-compatibility) para a lista atual. Na Plataforma de Agentes do Google Cloud, tool search é suportado para Claude Sonnet 4.5 e posterior e Claude Opus 4.5 e posterior.

Controle o comportamento da pesquisa de ferramentas com a variável de ambiente `ENABLE_TOOL_SEARCH`:

| Valor          | Comportamento                                                                                                                                                                                                                                                                                                    |
| :------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| (não definido) | Todas as ferramentas MCP adiadas e carregadas sob demanda. Volta a carregar antecipadamente na Plataforma de Agentes do Google Cloud ou quando `ANTHROPIC_BASE_URL` é um host que não é de primeira parte                                                                                                        |
| `true`         | Todas as ferramentas MCP adiadas. Claude Code envia o cabeçalho beta mesmo na Plataforma de Agentes do Google Cloud e através de proxies. As solicitações falham em modelos da Plataforma de Agentes do Google Cloud anteriores a Sonnet 4.5 ou Opus 4.5, ou em proxies que não suportam blocos `tool_reference` |
| `auto`         | Modo de limite: ferramentas carregam antecipadamente se se encaixarem em 10% da janela de contexto, adiadas caso contrário                                                                                                                                                                                       |
| `auto:N`       | Modo de limite com uma porcentagem personalizada, onde `N` é 0-100. Por exemplo, `auto:5` para 5%                                                                                                                                                                                                                |
| `false`        | Todas as ferramentas MCP carregadas antecipadamente, sem adiamento                                                                                                                                                                                                                                               |

```bash theme={null}
# Use um limite personalizado de 5%
ENABLE_TOOL_SEARCH=auto:5 claude

# Desabilite a pesquisa de ferramentas completamente
ENABLE_TOOL_SEARCH=false claude
```

Ou defina o valor no seu [campo `env` de settings.json](https://code.claude.com/docs/pt/settings#available-settings).

Você também pode desabilitar a ferramenta `ToolSearch` especificamente:

```json theme={null}
{
  "permissions": {
    "deny": ["ToolSearch"]
  }
}
```

<h3 id="exempt-a-server-from-deferral">
  Isentar um servidor de adiamento
</h3>

Se as ferramentas de um servidor devem estar sempre visíveis para Claude sem uma etapa de pesquisa, defina `alwaysLoad` como `true` na configuração desse servidor. Cada ferramenta desse servidor então carrega no contexto no início da sessão independentemente da configuração `ENABLE_TOOL_SEARCH`. Use isso para um pequeno número de ferramentas que Claude precisa a cada turno, já que cada ferramenta antecipada consome contexto que estaria disponível para sua conversa.

A seguinte entrada `.mcp.json` isenta um servidor HTTP enquanto deixa outros servidores adiados:

```json theme={null}
{
  "mcpServers": {
    "core-tools": {
      "type": "http",
      "url": "https://mcp.example.com/mcp",
      "alwaysLoad": true
    }
  }
}
```

O campo `alwaysLoad` está disponível em todos os tipos de servidor e requer Claude Code v2.1.121 ou posterior. Um servidor MCP também pode marcar ferramentas individuais como sempre carregadas incluindo `"anthropic/alwaysLoad": true` no objeto `_meta` da ferramenta, que tem o mesmo efeito apenas para essa ferramenta.

Definir `alwaysLoad: true` também bloqueia a inicialização até que o servidor se conecte, limitado ao tempo limite de conexão padrão de 5 segundos. Isso se aplica mesmo que a inicialização MCP seja [não bloqueante por padrão](https://code.claude.com/docs/pt/env-vars), já que as ferramentas devem estar presentes quando o primeiro prompt é construído. Outros servidores continuam a se conectar em segundo plano.

<h2 id="use-mcp-prompts-as-commands">
  Usar prompts MCP como comandos
</h2>

Os servidores MCP podem expor prompts que se tornam disponíveis como comandos no Claude Code.

<h3 id="execute-mcp-prompts">
  Executar prompts MCP
</h3>

<Steps>
  <Step title="Descubra prompts disponíveis">
    Digite `/` para ver todos os comandos disponíveis, incluindo aqueles de servidores MCP. Os prompts MCP aparecem com o formato `/mcp__servername__promptname`.
  </Step>

  <Step title="Execute um prompt sem argumentos">
    ```text theme={null}
    /mcp__github__list_prs
    ```
  </Step>

  <Step title="Execute um prompt com argumentos">
    Muitos prompts aceitam argumentos. Passe-os separados por espaço após o comando:

    ```text theme={null}
    /mcp__github__pr_review 456
    ```

    ```text theme={null}
    /mcp__jira__create_issue "Bug no fluxo de login" high
    ```
  </Step>
</Steps>

<Tip>
  Dicas:

  * Os prompts MCP são descobertos dinamicamente de servidores conectados
  * Os argumentos são analisados com base nos parâmetros definidos do prompt
  * Os resultados do prompt são injetados diretamente na conversa
  * Os nomes do servidor e do prompt são normalizados, com espaços convertidos em sublinhados
</Tip>

<h2 id="managed-mcp-configuration">
  Configuração MCP gerenciada
</h2>

Para organizações que precisam de controle centralizado sobre quais servidores MCP os usuários podem se conectar, consulte [Configuração MCP gerenciada](https://code.claude.com/docs/pt/managed-mcp). Ela aborda a implantação de um conjunto fixo de servidores com `managed-mcp.json`, restrição de servidores com `allowedMcpServers` e `deniedMcpServers`, e o que os usuários veem quando um servidor é bloqueado.
