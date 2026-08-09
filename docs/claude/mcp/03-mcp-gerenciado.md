<!-- Fonte: https://code.claude.com/docs/pt/managed-mcp | Baixado em: 2026-08-06 | slug: managed-mcp -->

# Controle o acesso ao servidor MCP para sua organização

> Restrinja quais servidores MCP os usuários podem adicionar ou conectar com arquivos de configuração gerenciados, listas de permissão e listas de bloqueio.

Por padrão, qualquer pessoa que execute Claude Code pode conectar qualquer [servidor MCP](https://code.claude.com/docs/pt/mcp) que escolher. A Anthropic analisa conectores em relação aos seus [critérios de listagem](https://claude.com/docs/connectors/building/review-criteria) antes de adicioná-los ao [Diretório Anthropic](https://claude.ai/directory), mas não realiza auditoria de segurança ou gerencia nenhum servidor MCP. Como administrador, você pode restringir quais servidores são executados em sua organização, desde a implantação de um conjunto fixo aprovado até a desabilitação completa do MCP.

Esta página aborda como:

* [Escolher um padrão](#choose-a-pattern) que corresponda ao nível de controle necessário
* [Implantar um conjunto de servidor fixo com `managed-mcp.json`](#exclusive-control-with-managed-mcp-json), incluindo como [desabilitar MCP completamente](#disable-mcp-entirely)
* [Controlar servidores com listas de permissão e listas de bloqueio](#policy-based-control-with-allowlists-and-denylists)
* [Informar aos usuários o que esperar](#how-restrictions-appear-to-users) quando uma restrição bloqueia um servidor
* [Monitorar quais servidores sua organização realmente usa](#monitor-mcp-usage)

<Note>
  A página [Segurança](https://code.claude.com/docs/pt/security) aborda o modelo de ameaça do MCP e como avaliar um servidor antes de aprová-lo. [Decidir o que aplicar](https://code.claude.com/docs/pt/admin-setup#decide-what-to-enforce) aborda restrições de MCP junto com os outros controles administrativos.
</Note>

<h2 id="choose-a-pattern">
  Escolher um padrão
</h2>

Claude Code suporta uma variedade de níveis de restrição. Cada padrão usa um ou ambos os mecanismos abordados abaixo: `managed-mcp.json` para implantar um conjunto fixo, e `allowedMcpServers`/`deniedMcpServers` para filtrar o que os usuários configuram.

| Padrão                           | O que faz                                                                                                    | Configurar                                                                                       |
| :------------------------------- | :----------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------------------------------- |
| **Desabilitar MCP**              | Nenhum servidor é carregado em lugar nenhum                                                                  | `managed-mcp.json` com um mapa de servidor vazio                                                 |
| **Implantação fixa**             | Cada usuário obtém os mesmos servidores e não pode adicionar outros                                          | `managed-mcp.json` com os servidores que você deseja                                             |
| **Catálogo aprovado**            | Publique uma lista de servidores aprovados; os usuários adicionam os que desejam, qualquer outro é bloqueado | `allowedMcpServers` + `allowManagedMcpServersOnly: true`                                         |
| **Apenas servidores de plugins** | Os servidores podem vir apenas de plugins; os usuários não podem adicionar os seus próprios                  | [`strictPluginOnlyCustomization`](https://code.claude.com/docs/pt/settings#strictpluginonlycustomization) com `mcp` na lista |
| **Lista de permissão suave**     | Aplicar uma lista de permissão que os usuários podem ampliar em suas próprias configurações                  | `allowedMcpServers` sem `allowManagedMcpServersOnly`                                             |
| **Apenas lista de bloqueio**     | Bloqueie servidores conhecidos como ruins, permita tudo o mais                                               | `deniedMcpServers`                                                                               |
| **Sem restrições**               | Os usuários adicionam qualquer coisa                                                                         | Não implante nenhuma configuração gerenciada de MCP                                              |

<Note>
  Claude Code não possui um registro de servidor MCP integrado que os usuários possam procurar e instalar. Para o padrão de catálogo aprovado, compartilhe a lista aprovada e seus comandos `claude mcp add` em algum lugar onde seus usuários a encontrem, como um wiki interno, ou distribua os servidores como plugins através de um [marketplace de plugins gerenciado](https://code.claude.com/docs/pt/plugin-marketplaces#managed-marketplace-restrictions) para que os usuários possam procurar e instalá-los em `/plugin`.
</Note>

<h2 id="exclusive-control-with-managed-mcp-json">
  Controle exclusivo com managed-mcp.json
</h2>

Se você implantar um arquivo `managed-mcp.json`, Claude Code carrega apenas os servidores que esse arquivo define. Os usuários não podem adicionar, modificar ou usar nenhum outro servidor MCP, incluindo servidores fornecidos por plugins. O arquivo também suprime conectores do claude.ai, a menos que você [permita-os junto com o conjunto gerenciado](#allow-claude-ai-connectors-alongside-the-managed-set).

Duas outras configurações podem filtrar ainda mais o conjunto gerenciado:

* `allowedMcpServers` e `deniedMcpServers` também se aplicam a servidores gerenciados, portanto um servidor gerenciado que não passar por eles não será carregado.
* O próprio `deniedMcpServers` de um usuário é mesclado a partir de suas configurações, portanto os usuários podem bloquear um servidor gerenciado para si mesmos.

Consulte [Como um servidor é avaliado](#how-a-server-is-evaluated) para a ordem completa de verificações.

`managed-mcp.json` é um arquivo autônomo, portanto não pode ser entregue através de [configurações gerenciadas pelo servidor](https://code.claude.com/docs/pt/server-managed-settings). Qualquer processo que possa escrever em um caminho do sistema com privilégios de administrador pode implantá-lo. Em escala, isso geralmente é feito através de ferramentas de gerenciamento de dispositivos, como Jamf ou um perfil de configuração no macOS, Política de Grupo ou Intune no Windows, ou seu gerenciamento de frota de escolha no Linux. Claude Code procura o arquivo em um destes caminhos:

| Plataforma  | Caminho                                                    |
| :---------- | :--------------------------------------------------------- |
| macOS       | `/Library/Application Support/ClaudeCode/managed-mcp.json` |
| Linux e WSL | `/etc/claude-code/managed-mcp.json`                        |
| Windows     | `C:\Program Files\ClaudeCode\managed-mcp.json`             |

O arquivo usa o mesmo formato que um arquivo de projeto [`.mcp.json`](https://code.claude.com/docs/pt/mcp#project-scope):

```json theme={null}
{
  "mcpServers": {
    "github": {
      "type": "http",
      "url": "https://api.githubcopilot.com/mcp/"
    },
    "sentry": {
      "type": "http",
      "url": "https://mcp.sentry.dev/mcp"
    },
    "company-internal": {
      "type": "stdio",
      "command": "/usr/local/bin/company-mcp-server",
      "args": ["--config", "/etc/company/mcp-config.json"],
      "env": {
        "COMPANY_API_URL": "https://internal.example.com"
      }
    }
  }
}
```

<h3 id="authenticate-with-per-user-credentials">
  Autenticar com credenciais por usuário
</h3>

Qualquer usuário na máquina pode ler este arquivo, portanto não armazene chaves de API ou outras credenciais em blocos `env`. Passe credenciais por usuário com uma destas alternativas:

* [Expansão `${VAR}`](https://code.claude.com/docs/pt/mcp#environment-variable-expansion-in-mcp-json) para ler segredos do ambiente de cada usuário.
* [OAuth ou cabeçalhos por usuário](https://code.claude.com/docs/pt/mcp#authenticate-with-remote-mcp-servers) para que cada usuário se autentique como si mesmo.
* [`headersHelper`](https://code.claude.com/docs/pt/mcp#use-dynamic-headers-for-custom-authentication) para gerar credenciais no momento da conexão.

<h3 id="validate-the-configuration">
  Validar a configuração
</h3>

Para confirmar que o arquivo está em vigor, execute duas verificações em uma máquina gerenciada:

1. `claude mcp list` mostra apenas os servidores em `managed-mcp.json`. Se os próprios servidores de um usuário ainda aparecerem, o arquivo não está sendo lido; verifique o caminho e as permissões.
2. `claude mcp add --transport http test https://example.com/mcp` falha com `Cannot add MCP server: enterprise MCP configuration is active and has exclusive control over MCP servers`. A URL não precisa ser um servidor real, pois a verificação de política rejeita o comando antes de qualquer coisa ser contatada.

<h3 id="disable-mcp-entirely">
  Desabilitar MCP completamente
</h3>

Implante um `managed-mcp.json` contendo um mapa de servidor vazio para bloquear todos os servidores MCP:

```json theme={null}
{
  "mcpServers": {}
}
```

Os usuários não veem nenhum servidor MCP em `/mcp`, e `claude mcp add` falha com o erro de política empresarial acima. Os servidores que os usuários configuraram anteriormente param de ser carregados na próxima vez que iniciam uma sessão, sem aviso de que a política é o motivo.

<h3 id="allow-claude-ai-connectors-alongside-the-managed-set">
  Permitir conectores do claude.ai junto com o conjunto gerenciado
</h3>

Implantar `managed-mcp.json` suprime [conectores do claude.ai](https://code.claude.com/docs/pt/mcp#use-mcp-servers-from-claude-ai) por padrão, incluindo conectores que um administrador configurou para a organização no console de administração do claude.ai. Para carregar esses conectores junto com os servidores em `managed-mcp.json`, defina `"allowAllClaudeAiMcps": true` em uma [fonte de configurações gerenciadas](https://code.claude.com/docs/pt/admin-setup#decide-how-settings-reach-devices). Requer Claude Code v2.1.149 ou posterior.

Com a configuração ativada, Claude Code carrega os mesmos conectores do claude.ai que carregaria se `managed-mcp.json` não fosse implantado. [Listas de permissão e listas de negação](#policy-based-control-with-allowlists-and-denylists) ainda se aplicam a esses conectores, portanto você pode bloquear específicos com `deniedMcpServers`. A configuração afeta apenas conectores do claude.ai; servidores fornecidos por plugins permanecem suprimidos.

Claude Code lê essa configuração apenas de camadas de política controladas por administrador: configurações gerenciadas pelo servidor, uma chave de registro plist implantada por MDM ou HKLM, ou um arquivo `managed-settings.json` do sistema. Colocá-la em configurações de usuário ou projeto não tem efeito, portanto os usuários não podem reabilitar conectores que o controle exclusivo suprimiu.

<h2 id="policy-based-control-with-allowlists-and-denylists">
  Controle baseado em política com listas de permissão e listas de bloqueio
</h2>

Listas de permissão e listas de bloqueio filtram quais servidores configurados podem ser carregados. Elas não são um registro: um servidor ainda precisa ser adicionado por um usuário, um plugin ou `managed-mcp.json` antes que a lista de permissão ou lista de bloqueio se aplique a ele. Para implantar servidores para usuários, use [`managed-mcp.json`](#exclusive-control-with-managed-mcp-json). Ambas as listas também filtram servidores passados com o sinalizador CLI [`--mcp-config`](https://code.claude.com/docs/pt/cli-reference#cli-flags); `--strict-mcp-config` limita quais arquivos de configuração são carregados e não contorna nenhuma das duas listas.

Para tornar a lista de permissão autoritária, defina `allowedMcpServers` e `allowManagedMcpServersOnly: true` juntos em uma [fonte de configurações gerenciadas](https://code.claude.com/docs/pt/admin-setup#decide-how-settings-reach-devices), como configurações gerenciadas pelo servidor ou um arquivo `managed-settings.json` implantado. [Restringir a lista de permissão apenas a configurações gerenciadas](#restrict-the-allowlist-to-managed-settings-only) mostra a configuração. Sem `allowManagedMcpServersOnly`, listas de permissão de todas as fontes de configurações são mescladas, incluindo o próprio `~/.claude/settings.json` de um usuário, portanto um usuário pode ampliar o que sua lista de permissão permite. Listas de bloqueio são mescladas de todas as fontes independentemente.

<Note>
  `allowManagedMcpServersOnly` é separado de `allowManagedPermissionRulesOnly`, que bloqueia apenas [regras de permissão](https://code.claude.com/docs/pt/permissions#managed-settings). Definir esse sinalizador não aplica a lista de permissão de MCP.
</Note>

<h3 id="match-servers-by-url-command-or-name">
  Corresponder servidores por URL, comando ou nome
</h3>

`allowedMcpServers` e `deniedMcpServers` são listas de entradas. Cada entrada é um objeto com uma única chave que identifica servidores por sua URL, seu comando ou seu nome:

| Chave           | Corresponde                                                                                | Use para                               |
| :-------------- | :----------------------------------------------------------------------------------------- | :------------------------------------- |
| `serverUrl`     | Uma URL de servidor remoto, exata ou com curingas `*`                                      | Servidores HTTP e SSE                  |
| `serverCommand` | O comando exato e argumentos que iniciam um servidor stdio                                 | Servidores stdio                       |
| `serverName`    | O rótulo atribuído pelo usuário. Correspondência exata apenas; curingas não são expandidos | Qualquer tipo, mas veja o Aviso abaixo |

Deixar `allowedMcpServers` não definido é diferente de defini-lo como uma matriz vazia:

| Configuração        | Não definido (padrão)          | Matriz vazia `[]`         | Preenchido                                   |
| :------------------ | :----------------------------- | :------------------------ | :------------------------------------------- |
| `allowedMcpServers` | Todos os servidores permitidos | Nenhum servidor permitido | Apenas servidores correspondentes permitidos |
| `deniedMcpServers`  | Nenhum servidor bloqueado      | Nenhum servidor bloqueado | Servidores correspondentes bloqueados        |

Veja [Entradas inválidas em configurações gerenciadas](https://code.claude.com/docs/pt/settings#invalid-entries-in-managed-settings) para saber o que acontece quando uma entrada falha na validação do esquema.

<Warning>
  Uma entrada `serverName`, em qualquer lista, não é um controle de segurança. O nome é o rótulo que um usuário atribui ao executar `claude mcp add` ou editar um arquivo de configuração, não o servidor subjacente, portanto um usuário pode chamar qualquer servidor de `github`. Para conectores claude.ai, o nome é o nome de exibição retornado por claude.ai, que pode mudar. Para aplicar quais servidores realmente são executados, adicione entradas `serverCommand` ou `serverUrl`.
</Warning>

A validação de `serverName` difere entre as duas listas:

* Em `deniedMcpServers`, `serverName` aceita qualquer string não vazia, portanto você pode bloquear [conectores claude.ai](https://code.claude.com/docs/pt/mcp#use-mcp-servers-from-claude-ai) por seu nome de exibição. Por exemplo, `{ "serverName": "claude.ai Slack" }` bloqueia o conector Slack. Prefira uma entrada `serverUrl` quando você precisar que a negação seja robusta a renomeações, ou quando um nome de conector colide e ganha um sufixo ` (N)`.
* Em `allowedMcpServers`, `serverName` é limitado a letras, números, hífens e sublinhados. Use `serverUrl` para adicionar um conector claude.ai à lista de permissão.

Para desativar todos os conectores claude.ai, veja [`disableClaudeAiConnectors`](https://code.claude.com/docs/pt/mcp#disable-claude-ai-connectors).

<h3 id="how-a-server-is-evaluated">
  Como um servidor é avaliado
</h3>

Antes de carregar um servidor, incluindo um de `managed-mcp.json`, Claude Code executa três verificações em ordem:

1. **Mesclar as listas.** Entradas de lista de permissão e lista de bloqueio de todas as fontes de configurações se combinam em uma lista de permissão e uma lista de bloqueio. Quando `allowManagedMcpServersOnly` é `true`, apenas a lista de permissão gerenciada é mantida; a lista de bloqueio sempre é mesclada de todas as fontes.
2. **Verificar a lista de bloqueio.** Um servidor que corresponde a qualquer entrada da lista de bloqueio, por URL, comando ou nome, é bloqueado. Nada substitui uma correspondência da lista de bloqueio.
3. **Verificar a lista de permissão.** Se `allowedMcpServers` não estiver definido em lugar nenhum, todos os servidores que passaram na lista de bloqueio são carregados. Se estiver definido, o que o servidor deve corresponder depende de seu tipo, mostrado na tabela abaixo.

| Tipo de servidor     | Permitido quando corresponde                                                                                                               |
| :------------------- | :----------------------------------------------------------------------------------------------------------------------------------------- |
| Remoto (HTTP ou SSE) | Uma entrada `serverUrl`. Uma correspondência `serverName` conta apenas quando a lista de permissão não contém entradas `serverUrl`         |
| Stdio                | Uma entrada `serverCommand`. Uma correspondência `serverName` conta apenas quando a lista de permissão não contém entradas `serverCommand` |

Três regras de correspondência se aplicam dentro dessas verificações:

* **Comandos correspondem exatamente.** Cada argumento, em ordem. `["npx", "-y", "server"]` não corresponde a `["npx", "server"]` ou `["npx", "-y", "server", "--flag"]`.
* **`serverCommand` e `serverUrl` valores se expandem antes de corresponder.** Tanto a entrada de política quanto o valor configurado do servidor passam pela mesma [expansão `${VAR}` e `${VAR:-default}`](https://code.claude.com/docs/pt/mcp#environment-variable-expansion-in-mcp-json) que `.mcp.json`, portanto uma entrada escrita como `["${HOME}/bin/server"]` corresponde a uma configuração de servidor que usa a mesma referência ou o caminho expandido. No Windows, faça referência a uma variável de ambiente que está definida lá, como `${USERPROFILE}` em vez de `${HOME}`. Os valores de `serverName` correspondem literalmente e nunca se expandem.
* **URLs suportam curingas `*`** em qualquer lugar do padrão, incluindo o esquema. A correspondência de nome de host não diferencia maiúsculas de minúsculas e ignora um ponto FQDN à direita, portanto `https://Mcp.Example.com/*` corresponde a `https://mcp.example.com/api`. Os caminhos permanecem sensíveis a maiúsculas e minúsculas.

| Padrão                      | Permite                                                                                      |
| :-------------------------- | :------------------------------------------------------------------------------------------- |
| `https://mcp.example.com/*` | Todos os caminhos em um domínio específico                                                   |
| `https://mcp.example.com`   | Também todos os caminhos nesse domínio. Um padrão sem caminho corresponde a qualquer caminho |
| `https://*.example.com/*`   | Qualquer subdomínio de `example.com`                                                         |
| `http://localhost:*/*`      | Qualquer porta em localhost                                                                  |
| `*://mcp.example.com/*`     | Qualquer esquema para um domínio específico                                                  |

Como a expansão `${VAR}` lê o próprio ambiente de processo do Claude Code, uma entrada de política `serverCommand` ou `serverUrl` que faz referência a uma variável se expande para qualquer valor que um usuário defina. Use URLs e comandos literais para entradas que você depende para aplicação.

<h3 id="example-configuration">
  Configuração de exemplo
</h3>

A configuração abaixo configura uma lista de permissão rígida com uma lista de bloqueio. As linhas destacadas alteram como o resto da lista é avaliado, e os textos explicativos após o bloco explicam cada uma:

```json {3,5,11} theme={null}
{
  "allowedMcpServers": [
    { "serverUrl": "https://api.githubcopilot.com/*" },
    { "serverUrl": "https://mcp.sentry.dev/*" },
    { "serverCommand": ["npx", "-y", "@modelcontextprotocol/server-filesystem", "."] },
    { "serverCommand": ["python", "/usr/local/bin/approved-server.py"] },
    { "serverUrl": "https://mcp.example.com/*" },
    { "serverUrl": "https://*.internal.example.com/*" }
  ],
  "deniedMcpServers": [
    { "serverName": "dangerous-server" },
    { "serverCommand": ["npx", "-y", "unapproved-package"] },
    { "serverUrl": "https://*.untrusted.example.com/*" }
  ]
}
```

* **Linha 3**: a primeira entrada `serverUrl`. Uma vez que existe uma, cada servidor remoto deve corresponder a um padrão de URL, portanto um usuário não pode obter um servidor remoto não listado dando-lhe um nome permitido.
* **Linha 5**: a primeira entrada `serverCommand`. Mesmo efeito para servidores stdio, portanto cada servidor local deve corresponder a um comando listado exatamente.
* **Linha 11**: uma entrada `serverName` na lista de bloqueio. Entradas da lista de bloqueio sempre se aplicam, portanto qualquer servidor nomeado `dangerous-server` é bloqueado independentemente de sua URL ou comando.

Uma entrada `serverName` nesta lista de permissão nunca corresponderia a nada, pois ambos os tipos de transporte já têm entradas mais rigorosas.

Os acordeões abaixo percorrem como um servidor é avaliado em relação a outras combinações de lista de permissão e lista de bloqueio.

<Accordion title="Lista de permissão apenas de URL">
  ```json theme={null}
  {
    "allowedMcpServers": [
      { "serverUrl": "https://mcp.example.com/*" },
      { "serverUrl": "https://*.internal.example.com/*" }
    ]
  }
  ```

  | Servidor                                                | Resultado                                                    |
  | :------------------------------------------------------ | :----------------------------------------------------------- |
  | Servidor HTTP em `https://mcp.example.com/api`          | Permitido: corresponde ao padrão de URL                      |
  | Servidor HTTP em `https://api.internal.example.com/mcp` | Permitido: corresponde ao subdomínio curinga                 |
  | Servidor HTTP em `https://external.example.com/mcp`     | Bloqueado: não corresponde a nenhum padrão de URL            |
  | Servidor stdio com qualquer comando                     | Bloqueado: sem entradas de nome ou comando para corresponder |
</Accordion>

<Accordion title="Lista de permissão apenas de comando">
  ```json theme={null}
  {
    "allowedMcpServers": [
      { "serverCommand": ["npx", "-y", "approved-package"] }
    ]
  }
  ```

  | Servidor                                               | Resultado                                         |
  | :----------------------------------------------------- | :------------------------------------------------ |
  | Servidor stdio com `["npx", "-y", "approved-package"]` | Permitido: corresponde ao comando                 |
  | Servidor stdio com `["node", "server.js"]`             | Bloqueado: não corresponde ao comando             |
  | Servidor HTTP nomeado `my-api`                         | Bloqueado: sem entradas de nome para corresponder |
</Accordion>

<Accordion title="Lista de permissão mista de nome e comando">
  ```json theme={null}
  {
    "allowedMcpServers": [
      { "serverName": "github" },
      { "serverCommand": ["npx", "-y", "approved-package"] }
    ]
  }
  ```

  | Servidor                                                                    | Resultado                                                                                    |
  | :-------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------- |
  | Servidor stdio nomeado `local-tool` com `["npx", "-y", "approved-package"]` | Permitido: corresponde ao comando                                                            |
  | Servidor stdio nomeado `local-tool` com `["node", "server.js"]`             | Bloqueado: entradas de comando existem mas não correspondem                                  |
  | Servidor stdio nomeado `github` com `["node", "server.js"]`                 | Bloqueado: servidores stdio devem corresponder a comandos quando entradas de comando existem |
  | Servidor HTTP nomeado `github`                                              | Permitido: corresponde ao nome                                                               |
  | Servidor HTTP nomeado `other-api`                                           | Bloqueado: nome não corresponde                                                              |
</Accordion>

<Accordion title="Lista de permissão apenas de nome">
  ```json theme={null}
  {
    "allowedMcpServers": [
      { "serverName": "github" },
      { "serverName": "internal-tool" }
    ]
  }
  ```

  | Servidor                                                    | Resultado                            |
  | :---------------------------------------------------------- | :----------------------------------- |
  | Servidor stdio nomeado `github` com qualquer comando        | Permitido: sem restrições de comando |
  | Servidor stdio nomeado `internal-tool` com qualquer comando | Permitido: sem restrições de comando |
  | Servidor HTTP nomeado `github`                              | Permitido: corresponde ao nome       |
  | Qualquer servidor nomeado `other`                           | Bloqueado: nome não corresponde      |
</Accordion>

<Accordion title="Lista de permissão com substituição de lista de bloqueio">
  ```json theme={null}
  {
    "allowedMcpServers": [
      { "serverUrl": "https://*.example.com/*" }
    ],
    "deniedMcpServers": [
      { "serverUrl": "https://staging.example.com/*" }
    ]
  }
  ```

  | Servidor                                           | Resultado                                                                                               |
  | :------------------------------------------------- | :------------------------------------------------------------------------------------------------------ |
  | Servidor HTTP em `https://mcp.example.com/api`     | Permitido: corresponde ao padrão de URL da lista de permissão, sem correspondência da lista de bloqueio |
  | Servidor HTTP em `https://staging.example.com/api` | Bloqueado: corresponde a ambos, mas a lista de bloqueio tem precedência                                 |
  | Servidor HTTP em `https://other.com/mcp`           | Bloqueado: não corresponde à lista de permissão                                                         |
</Accordion>

<h3 id="restrict-the-allowlist-to-managed-settings-only">
  Restringir a lista de permissão apenas a configurações gerenciadas
</h3>

Para tornar a lista de permissão gerenciada a única que se aplica, defina `allowManagedMcpServersOnly` no arquivo de configurações gerenciadas:

```json theme={null}
{
  "allowManagedMcpServersOnly": true,
  "allowedMcpServers": [
    { "serverUrl": "https://api.githubcopilot.com/*" },
    { "serverUrl": "https://*.internal.example.com/*" }
  ]
}
```

Quando `allowManagedMcpServersOnly` é `true`, listas de permissão de configurações de usuário, projeto e local são ignoradas. A lista de bloqueio ainda é mesclada de todas as fontes, portanto os usuários sempre podem bloquear servidores para si mesmos.

<h2 id="how-restrictions-appear-to-users">
  Como as restrições aparecem para os usuários
</h2>

Quando uma restrição bloqueia um servidor, o usuário vê um erro de `claude mcp add` ou o servidor para de ser carregado silenciosamente. Use esta tabela para reconhecer esses relatórios e para informar aos usuários o que esperar antes de implementar uma alteração:

| Restrição                                                                      | O que o usuário vê                                                                                         |
| :----------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------- |
| `managed-mcp.json` está presente e o usuário executa `claude mcp add`          | `Cannot add MCP server: enterprise MCP configuration is active and has exclusive control over MCP servers` |
| O servidor está em uma lista de bloqueio e o usuário executa `claude mcp add`  | `Cannot add MCP server "<name>": server is explicitly blocked by enterprise policy`                        |
| O servidor não está na lista de permissão e o usuário executa `claude mcp add` | `Cannot add MCP server "<name>": not allowed by enterprise policy`                                         |
| Um servidor configurado anteriormente agora é bloqueado pela política          | O servidor desaparece silenciosamente de `/mcp` e `claude mcp list` sem aviso                              |

No último caso, o usuário não recebe nenhum sinal de que a política é o motivo pelo qual seu servidor desapareceu, portanto informe aos usuários afetados quais servidores são bloqueados quando você implementar uma nova restrição.

<h2 id="monitor-mcp-usage">
  Monitorar o uso de MCP
</h2>

Quando [exportação OpenTelemetry](https://code.claude.com/docs/pt/monitoring-usage) está configurada, Claude Code pode registrar quais servidores MCP e ferramentas os usuários invocam. Defina `OTEL_LOG_TOOL_DETAILS=1` para incluir nomes de servidor MCP e ferramentas em eventos de ferramentas, depois agregue-os em seu coletor para ver quais servidores seus usuários realmente conectam. Consulte [Monitoramento](https://code.claude.com/docs/pt/monitoring-usage) para configurar o exportador e para o esquema de evento completo.

<h2 id="configuration-summary">
  Resumo de configuração
</h2>

Cada arquivo e configuração que esta página aborda, o que controla e como entregá-lo:

| Superfície                   | O que controla                                                                  | Onde fica                                                                                                                                                            | Como entregar                                                                                                                                                                                         |
| :--------------------------- | :------------------------------------------------------------------------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `managed-mcp.json`           | Conjunto de servidor fixo, controle exclusivo                                   | Caminho do sistema: `/Library/Application Support/ClaudeCode/`, `/etc/claude-code/`, ou `C:\Program Files\ClaudeCode\`                                               | MDM, GPO, gerenciamento de frota ou qualquer processo com privilégios de administrador. Não pode ser definido através de configurações gerenciadas pelo servidor                                      |
| `allowedMcpServers`          | Lista de permissão de servidores permitidos                                     | Qualquer [arquivo de configurações](https://code.claude.com/docs/pt/settings#settings-files); entradas de todas as fontes são mescladas a menos que `allowManagedMcpServersOnly` esteja definido | Para aplicação, uma [fonte de configurações gerenciadas](https://code.claude.com/docs/pt/admin-setup#decide-how-settings-reach-devices): configurações gerenciadas pelo servidor, `managed-settings.json`, perfil MDM ou registro |
| `deniedMcpServers`           | Lista de bloqueio de servidores bloqueados                                      | Qualquer arquivo de configurações; entradas de todas as fontes são mescladas                                                                                         | Mesmo que `allowedMcpServers`                                                                                                                                                                         |
| `allowManagedMcpServersOnly` | Bloqueia a lista de permissão apenas para fontes gerenciadas                    | Apenas fontes de configurações gerenciadas; a configuração não tem efeito em outro lugar                                                                             | Mesmo que `allowedMcpServers`                                                                                                                                                                         |
| `allowAllClaudeAiMcps`       | Carrega conectores claude.ai junto com `managed-mcp.json` em vez de suprimi-los | Apenas fontes de configurações gerenciadas; a configuração não tem efeito em outro lugar                                                                             | Mesmo que `allowedMcpServers`                                                                                                                                                                         |

<h2 id="related-resources">
  Recursos relacionados
</h2>

* [Decidir o que aplicar](https://code.claude.com/docs/pt/admin-setup#decide-what-to-enforce): restrições de MCP junto com regras de permissão, sandboxing e os outros controles de administrador
* [Conectar Claude Code a ferramentas via MCP](https://code.claude.com/docs/pt/mcp): a referência completa de MCP, incluindo transportes, escopos e autenticação
* [Configurações](https://code.claude.com/docs/pt/settings): a hierarquia de configurações e como as configurações gerenciadas têm precedência
* [Configurações gerenciadas pelo servidor](https://code.claude.com/docs/pt/server-managed-settings): entregar `allowedMcpServers` e `deniedMcpServers` do console de administrador do Claude.ai
* [Segurança](https://code.claude.com/docs/pt/security): o modelo de ameaça que esses controles defendem
* [Guia do Administrador Empresarial Claude](https://claude.com/resources/tutorials/claude-enterprise-administrator-guide): SSO, SCIM, gerenciamento de assentos e playbook de implementação
