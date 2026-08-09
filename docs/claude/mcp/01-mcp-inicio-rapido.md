<!-- Fonte: https://code.claude.com/docs/pt/mcp-quickstart | Baixado em: 2026-08-06 | slug: mcp-quickstart -->

# Conectar a servidores MCP

> Adicione um servidor MCP ao Claude Code, verifique a conexão e encontre a configuração no disco.

O [Model Context Protocol (MCP)](https://modelcontextprotocol.io/introduction) permite que o Claude Code use ferramentas além do seu conjunto integrado, como pesquisar um rastreador de problemas, consultar um banco de dados ou controlar um navegador da web. Essas ferramentas vêm de servidores MCP, que são executados em sua máquina ou como serviços hospedados.

Este guia o orienta através da conexão de um servidor MCP de ponta a ponta com a CLI do Claude Code. Ao final, você terá um servidor conectado e respondendo, saberá onde sua configuração reside no disco e saberá como corrigir os erros de conexão mais comuns.

<Note>
  Você também pode adicionar servidores MCP de outras superfícies, incluindo o aplicativo desktop, VS Code e a web. Consulte [Conectar de outras superfícies](#connect-from-other-surfaces).
</Note>

Para cada forma de conectar e configurar servidores MCP no Claude Code, consulte a [referência MCP](https://code.claude.com/docs/pt/mcp).

<h2 id="before-you-begin">
  Antes de começar
</h2>

Certifique-se de que você tem:

* [Claude Code instalado](https://code.claude.com/docs/pt/quickstart) e autenticado
* Um terminal aberto em um diretório de projeto. Qualquer diretório funciona, incluindo um vazio.

<h2 id="add-and-verify-a-server">
  Adicionar e verificar um servidor
</h2>

O exemplo abaixo conecta ao [servidor MCP de documentação do Claude Code](https://code.claude.com/docs/mcp), um servidor hospedado com busca de texto completo sobre os documentos do Claude Code. Ele não requer autenticação ou nenhuma configuração especial, portanto funciona bem como primeiro servidor para testar o fluxo de configuração.

As etapas são as mesmas para qualquer servidor: adicione-o, verifique o status da conexão e use-o em uma sessão, com uma etapa de limpeza opcional no final. Alguns servidores adicionam uma etapa, como um login no navegador, mostrado em [Exemplos adicionais de servidor MCP](#additional-mcp-server-examples). Para mais servidores para conectar, navegue pelo [Diretório Anthropic](https://code.claude.com/docs/pt/mcp#find-and-build-mcp-servers).

<Steps>
  <Step title="Adicionar o servidor MCP">
    Registre o servidor com o Claude Code. Execute isto em seu terminal, não dentro de uma sessão `claude`: você está configurando o servidor antes de iniciar uma conversa.

    ```bash theme={null}
    claude mcp add --transport http claude-code-docs https://code.claude.com/docs/mcp
    ```

    As partes do comando:

    * `claude mcp add`: registra um servidor com o Claude Code.
    * `--transport http`: o servidor é hospedado em uma URL em vez de ser executado como um processo local.
    * `claude-code-docs`: um nome que você cria. Chamar o mesmo servidor de `docs` funcionaria de forma idêntica. O Claude Code usa o nome que você escolher para rotular as ferramentas do servidor na saída do Claude e para se referir ao servidor em comandos como `claude mcp remove`.
    * `https://code.claude.com/docs/mcp`: a URL onde o servidor é hospedado.

    O comando imprime uma confirmação como `Added HTTP MCP server claude-code-docs with URL: https://code.claude.com/docs/mcp to local config`. A parte `local config` significa que o servidor está registrado para você, neste projeto: se você iniciar o Claude Code em um projeto diferente, este servidor não está ativo lá. Para registrar um servidor uma vez para todos os seus projetos, adicione-o no escopo do usuário, coberto em [Alterar escopo do servidor](#change-server-scope).
  </Step>

  <Step title="Verificar o status da conexão">
    Confirme que o servidor aparece em sua lista de servidores e verifique seu status:

    ```bash theme={null}
    claude mcp list
    ```

    O servidor aparece com um indicador de status:

    | Status                             | Significado                                                                                                                                                                                 |
    | :--------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
    | `✓ Connected`                      | Pronto para usar. Isto é o que você deve ver para `claude-code-docs`                                                                                                                        |
    | `! Connected · tools fetch failed` | O servidor conectou mas não conseguiu listar suas ferramentas. Execute `claude mcp get <name>` para obter o detalhe do erro                                                                 |
    | `! Needs authentication`           | O servidor é alcançável mas precisa de um login no navegador, ou um token passado com `--header`. Consulte [Conectar um servidor que requer login](#connect-a-server-that-requires-sign-in) |
    | `✗ Failed to connect`              | O servidor não respondeu. Consulte [Troubleshooting](#troubleshooting)                                                                                                                      |
    | `✗ Connection error`               | A tentativa de conexão lançou um erro. Consulte [Troubleshooting](#troubleshooting)                                                                                                         |
    | `⏸ Pending approval`               | Um servidor com escopo de projeto que você ainda não aprovou. Consulte [Editar .mcp.json diretamente](#edit-mcp-json-directly)                                                              |
  </Step>

  <Step title="Usar o servidor">
    Inicie uma sessão e peça ao Claude para usar o novo servidor pelo nome:

    ```bash theme={null}
    claude
    ```

    ```text theme={null}
    Use the claude-code-docs server to look up what MCP_TIMEOUT does
    ```

    <Info>
      Você normalmente não precisa nomear um servidor em seu prompt, já que o Claude escolhe ferramentas relevantes por conta própria. Nomeá-lo aqui garante que a demonstração passe pelo novo servidor em vez de outra ferramenta, como web fetch, que poderia responder a mesma pergunta.
    </Info>

    A primeira vez que o Claude chama o servidor, ele pede permissão para usar a nova ferramenta. Aprove-a para continuar. A chamada de ferramenta na saída do Claude é rotulada com o nome do servidor, que é como você confirma que a resposta veio do servidor MCP em vez do conhecimento integrado do Claude.
  </Step>

  <Step title="Remover o servidor">
    Esta etapa é opcional. Quando terminar de experimentar, você pode remover o servidor:

    ```bash theme={null}
    claude mcp remove claude-code-docs
    ```

    <Note>
      Cada servidor conectado ocupa espaço na [janela de contexto do Claude](https://code.claude.com/docs/pt/how-claude-code-works#the-context-window) porque seus nomes de ferramentas e instruções do servidor são carregados em cada sessão. Remover servidores que você não usa mais mantém esse espaço livre.
    </Note>
  </Step>
</Steps>

<h2 id="where-servers-are-saved">
  Onde os servidores são salvos
</h2>

O comando `claude mcp add` escreve os detalhes do servidor em um arquivo de configuração. Por padrão, ele registra o servidor no escopo `local`: privado para você, ativo apenas no projeto atual. Passe `--scope user` para registrá-lo uma vez para todos os seus projetos, ou `--scope project` para compartilhá-lo com colegas de equipe. [Alterar escopo do servidor](#change-server-scope) percorre ambos.

<Note>
  `claude mcp add` funciona da mesma forma em cada shell, incluindo PowerShell e Command Prompt. Dentro de uma sessão `claude`, use o comando `/mcp` para verificar e gerenciar servidores que você já adicionou.
</Note>

Existem outras formas de adicionar um servidor, cada uma coberta posteriormente nesta página:

* [Adicionar um servidor local](#add-a-local-server): execute um programa em sua máquina em vez de conectar a uma URL.
* [Editar `.mcp.json` diretamente](#edit-mcp-json-directly): escreva a entrada JSON você mesmo em vez de usar o comando.
* [Conectar um servidor que requer login](#connect-a-server-that-requires-sign-in): adicione um servidor hospedado que precisa de um login no navegador antes de suas ferramentas funcionarem.

<h3 id="find-your-configuration-on-disk">
  Encontre sua configuração no disco
</h3>

O comando `claude mcp add` escreve o servidor em um dos três escopos, armazenados em dois arquivos, dependendo da flag `--scope`. Você não precisa editar esses arquivos diretamente, mas saber onde eles estão ajuda na depuração e controle de versão.

| Escopo    | Arquivo                                                      | Disponível para                            |
| :-------- | :----------------------------------------------------------- | :----------------------------------------- |
| `local`   | `~/.claude.json`, sob a entrada para este projeto            | Apenas você, apenas este projeto. O padrão |
| `project` | `.mcp.json` na raiz do seu projeto                           | Todos que clonam o projeto                 |
| `user`    | `~/.claude.json`, sob a chave `mcpServers` de nível superior | Apenas você, todos os projetos             |

No Windows, `~/.claude.json` resolve para `%USERPROFILE%\.claude.json`, tipicamente `C:\Users\YourName\.claude.json`. Se você definiu [`CLAUDE_CONFIG_DIR`](https://code.claude.com/docs/pt/env-vars), o Claude Code lê `.claude.json` de dentro desse diretório.

Execute `claude mcp get claude-code-docs` para ver qual escopo contém a definição de um servidor. Para como os escopos interagem quando o mesmo servidor é definido em mais de um, consulte [Escopos de instalação MCP](https://code.claude.com/docs/pt/mcp#mcp-installation-scopes).

<h2 id="change-server-scope">
  Alterar escopo do servidor
</h2>

O escopo de um servidor é fixo quando você o adiciona, portanto alterar o escopo significa remover a entrada e readicioná-la no novo. Ambos os casos abaixo começam removendo a entrada local do primeiro passo a passo, para que o servidor tenha apenas uma definição. Se você já o removeu no final desse passo a passo, pule este comando:

```bash theme={null}
claude mcp remove claude-code-docs --scope local
```

<h3 id="use-a-server-in-all-your-projects">
  Usar um servidor em todos os seus projetos
</h3>

Readicione o servidor no escopo `user` para torná-lo ativo em cada projeto que você abre, ainda privado para você:

```bash theme={null}
claude mcp add --scope user --transport http claude-code-docs https://code.claude.com/docs/mcp
```

<h3 id="share-a-server-with-your-team">
  Compartilhar um servidor com sua equipe
</h3>

Readicione o servidor no escopo `project`, que escreve em `.mcp.json` na raiz do projeto:

```bash theme={null}
claude mcp add --scope project --transport http claude-code-docs https://code.claude.com/docs/mcp
```

Confirme `.mcp.json` no controle de versão. Colegas de equipe que clonam o repositório e iniciam o Claude Code veem um prompt para aprovar o servidor, então ele se conecta para eles também.

<h2 id="additional-mcp-server-examples">
  Exemplos adicionais de servidor MCP
</h2>

O primeiro passo a passo usou um servidor hospedado que se conecta sem nenhum login. Os exemplos abaixo cobrem as outras duas formas comuns, com o mesmo fluxo de adicionar, verificar, usar.

<h3 id="add-a-local-server">
  Adicionar um servidor local
</h3>

Um servidor stdio local é um programa que o Claude Code inicia como um subprocesso em sua máquina, em vez de um serviço que ele alcança por uma URL. Use um para ferramentas que precisam de acesso a recursos locais como um navegador, seu sistema de arquivos ou um socket de banco de dados.

O [servidor MCP Playwright](https://github.com/microsoft/playwright-mcp) é um bom para tentar: ele dá ao Claude um navegador que ele pode navegar, clicar e ler, e não precisa de nenhuma conta. Ele é executado através de `npx`, portanto requer [Node.js](https://nodejs.org/en/download) 18 ou posterior.

<Steps>
  <Step title="Adicionar o servidor Playwright">
    Registre o servidor com o comando que o Claude Code deve executar para iniciá-lo:

    ```bash theme={null}
    claude mcp add playwright -- npx -y @playwright/mcp@latest
    ```

    Este comando difere do exemplo hospedado de três formas:

    * Não há flag `--transport`, porque servidores locais usam o transporte padrão `stdio`.
    * Tudo após o separador `--` é o comando que o Claude Code executa para iniciar o servidor.
    * `-y` diz ao `npx` para instalar o pacote sem solicitar.

    Playwright controla qualquer Chrome já instalado em sua máquina. Para usar um navegador diferente, anexe `--browser` com o nome do navegador, por exemplo `--browser firefox`, após `@playwright/mcp@latest`.
  </Step>

  <Step title="Verificar a conexão">
    A confirmação `Added` significa que a entrada foi salva, não que o comando é executado. Verifique a conexão:

    ```bash theme={null}
    claude mcp list
    ```

    A primeira verificação pode mostrar `✗ Failed to connect` enquanto `npx` baixa o pacote, portanto aguarde um momento e execute novamente.
  </Step>

  <Step title="Usar o navegador">
    Dê ao Claude uma tarefa que precisa do navegador:

    ```text theme={null}
    Use playwright to open https://example.com and tell me the page title
    ```

    Uma janela do navegador abre para que você possa vê-lo funcionar, e as chamadas de ferramenta na saída do Claude são rotuladas com o nome do servidor `playwright` e a ação, como `browser_navigate`.

    Tente apontá-lo para seu servidor de desenvolvimento local para verificar se uma página ainda é renderizada após uma alteração, ou peça que ele percorra um relatório de bug passo a passo.
  </Step>
</Steps>

<h3 id="connect-a-server-that-requires-sign-in">
  Conectar um servidor que requer login
</h3>

Serviços hospedados como Sentry, Linear e Notion executam seus servidores MCP atrás de OAuth: você adiciona a URL do servidor e depois faz login através de seu navegador.

As etapas abaixo usam Sentry como exemplo. Para conectar um serviço diferente, substitua sua URL, que você pode encontrar no [Diretório Anthropic](https://code.claude.com/docs/pt/mcp#find-and-build-mcp-servers) ou na documentação do serviço.

<Steps>
  <Step title="Adicionar o servidor">
    O comando `add` é o mesmo que para o servidor de documentação, com a URL do Sentry:

    ```bash theme={null}
    claude mcp add --transport http sentry https://mcp.sentry.dev/mcp
    ```

    Após adicionar, `claude mcp list` mostra o servidor com `! Needs authentication`. Isto é esperado: a próxima etapa completa o login.
  </Step>

  <Step title="Autenticar em seu navegador">
    Inicie uma sessão do Claude Code e abra o painel MCP:

    ```text theme={null}
    /mcp
    ```

    Selecione `sentry` da lista, pressione Enter e escolha `Authenticate`. Seu navegador abre para a página de login do Sentry. Aprove a conexão lá.

    De volta ao Claude Code, o status do servidor muda para conectado. Se o login falhar ou o navegador não abrir, consulte [Troubleshooting](#troubleshooting).
  </Step>

  <Step title="Usar o servidor">
    Peça ao Claude algo que precisa do serviço, como `What Sentry projects do I have access to?`, e procure por chamadas de ferramenta rotuladas com o nome do servidor `sentry` em sua saída.
  </Step>
</Steps>

Servidores que autenticam com um token estático em vez de OAuth pegam o token no momento da adição com `--header "Authorization: Bearer <token>"`. Consulte o [exemplo do GitHub](https://code.claude.com/docs/pt/mcp#example-connect-to-github-for-code-reviews) para uma versão trabalhada.

<h2 id="edit-mcp-json-directly">
  Editar .mcp.json diretamente
</h2>

Cada arquivo na [tabela de escopo](#find-your-configuration-on-disk) usa o mesmo formato JSON para entradas de servidor. Esta seção edita `.mcp.json`, o arquivo de escopo de projeto. É o que mais vale a pena escrever à mão porque é verificado no repositório, onde funciona como configuração como código para sua equipe.

Crie `.mcp.json` na raiz do seu projeto. O exemplo abaixo define ambos os servidores deste guia, o servidor de documentação hospedado alcançado por HTTP e o servidor Playwright como um processo `stdio` local:

```json theme={null}
{
  "mcpServers": {
    "claude-code-docs": {
      "type": "http",
      "url": "https://code.claude.com/docs/mcp"
    },
    "playwright": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@playwright/mcp@latest"]
    }
  }
}
```

Os campos diferem por tipo de servidor:

* Para servidores HTTP, `url` é o endpoint que o Claude Code se conecta.
* Para servidores stdio, `command` e `args` são o programa que ele executa.

Após salvar o arquivo, inicie uma nova sessão do Claude Code no projeto. O Claude Code lê `.mcp.json` na inicialização.

A primeira vez que o Claude Code vê um servidor com escopo de projeto, ele pede que você o aprove. O prompt existe para que um repositório que você clona não possa iniciar processos em sua máquina sem seu consentimento. Aprove o prompt, ou execute `/mcp` para aprovar depois se você o perdeu.

Depois de aprovado, execute `/mcp` e verifique se os servidores aparecem como conectados. Se um mostrar um erro em vez disso, consulte [Troubleshooting](#troubleshooting).

<h2 id="connect-from-other-surfaces">
  Conectar de outras superfícies
</h2>

Este guia usa os comandos CLI `claude mcp`, mas cada superfície do Claude Code pode se conectar a servidores MCP:

* **Aplicativo desktop Claude Code**: adicione servidores através da [UI de Conectores](https://code.claude.com/docs/pt/desktop#connect-external-tools).
* **Aplicativo de chat Claude Desktop**: um aplicativo separado do Claude Code. Para copiar servidores de seu `claude_desktop_config.json` para a CLI, execute `claude mcp add-from-claude-desktop` no macOS ou WSL.
* **VS Code**: consulte [Conectar a ferramentas externas com MCP](https://code.claude.com/docs/pt/vs-code#connect-to-external-tools-with-mcp).
* **Claude Code na web**: lê `.mcp.json` do seu repositório. Consulte [Editar .mcp.json diretamente](#edit-mcp-json-directly).
* **Claude.ai**: conectores que você adiciona em [claude.ai/customize/connectors](https://claude.ai/customize/connectors) são carregados automaticamente na CLI quando você faz login com essa conta. Consulte [Usar servidores MCP do Claude.ai](https://code.claude.com/docs/pt/mcp#use-mcp-servers-from-claude-ai).

<h2 id="troubleshooting">
  Troubleshooting
</h2>

Se um servidor não se conectar, verifique seu status com `/mcp` dentro de uma sessão ou `claude mcp list` do seu shell, depois combine o sintoma abaixo. O painel `/mcp` também permite que você se reconecte ou autentique sem sair da sessão.

<AccordionGroup>
  <Accordion title="/mcp shows No MCP servers configured">
    O Claude Code não encontrou nenhum servidor para o diretório atual. As causas mais comuns:

    * Você executou `claude mcp add` de um projeto diferente. Servidores com escopo local estão vinculados ao projeto onde você os adicionou: a raiz do repositório, ou o diretório exato se você não estava em um repositório git. Readicione o servidor do projeto em que você está agora, ou adicione-o com `--scope user` para que não esteja vinculado a um projeto.
    * Você editou um arquivo de configuração no caminho errado. Os arquivos corretos são `~/.claude.json` e `<project>/.mcp.json`. O Claude Code não lê caminhos como `~/.claude/.mcp.json`, `~/.claude/config/mcp.json`, `~/.claude/mcp.json`, ou `%APPDATA%\Claude\mcp.json`. Para servidores com escopo de usuário, execute `claude mcp add --scope user`, que escreve na chave `mcpServers` em `~/.claude.json`; para servidores com escopo de projeto, edite `.mcp.json` na raiz do projeto.
  </Accordion>

  <Accordion title="Status shows Failed to connect or Connection error">
    Ambos os status significam que o servidor não iniciou ou a URL não respondeu. Eles também podem aparecer para servidores HTTP que esperam um token em vez do login no navegador coberto em [Conectar um servidor que requer login](#connect-a-server-that-requires-sign-in).

    A partir da v2.1.191, um servidor HTTP que retorna `404 Not Found` mostra `MCP endpoint not found at <url>. Check the URL in your MCP config.` quando você seleciona o servidor em `/mcp`, com a URL que o Claude Code tentou. Versões anteriores mostram uma mensagem genérica `Error POSTing to endpoint` sem a URL. Compare a URL com o caminho do endpoint MCP documentado do servidor, depois execute `claude mcp remove <name>` e readicione com a URL correta.

    Para servidores HTTP, confirme que a URL é alcançável de sua máquina:

    ```bash theme={null}
    curl -I https://mcp.sentry.dev/mcp
    ```

    No PowerShell, use `curl.exe` em vez de `curl` para que a solicitação vá para o binário curl real em vez do alias `Invoke-WebRequest`.

    A resposta diz qual tipo de problema você tem:

    * Um `404` ou `405`: o servidor está ativo. Muitos endpoints MCP respondem apenas a solicitações POST, portanto isto ainda confirma que a URL é alcançável de sua máquina.
    * Um `401` ou `403`: o servidor está ativo e você precisa autenticar. Use o login no navegador em [Conectar um servidor que requer login](#connect-a-server-that-requires-sign-in), ou para servidores que pegam um token em vez disso, como o do GitHub, passe-o com `--header "Authorization: Bearer <token>"` no comando `claude mcp add`.
    * Nenhuma resposta: verifique a URL e sua rede.

    Para servidores stdio, execute o comando configurado diretamente em seu terminal para ver o erro subjacente. Para o servidor Playwright deste guia, execute:

    ```bash theme={null}
    npx -y @playwright/mcp@latest
    ```

    O que acontece a seguir diz onde o problema está:

    * O comando inicia e aguarda entrada: o servidor em si funciona. Execute `claude mcp get <name>` e confirme que o comando mostrado lá corresponde ao que você acabou de executar. Se o comando mostrado diferir do que você digitou, você provavelmente omitiu o separador `--` antes do comando do servidor. Remova o servidor e readicione-o com `--` no lugar. Se você escreveu `.mcp.json` à mão, verifique sua sintaxe e localização.
    * O comando erros: a mensagem nomeia o que está faltando, como Node.js ou um navegador.
  </Accordion>

  <Accordion title="Connection timed out at startup">
    O servidor levou mais tempo que o timeout de inicialização padrão de 30 segundos. A primeira execução de um servidor stdio pode ser lenta enquanto `npx` baixa o pacote. Aumente o limite com a variável de ambiente [`MCP_TIMEOUT`](https://code.claude.com/docs/pt/env-vars), em milissegundos:

    ```bash theme={null}
    MCP_TIMEOUT=60000 claude
    ```

    No PowerShell, defina a variável antes do comando na mesma linha:

    ```powershell theme={null}
    $env:MCP_TIMEOUT = "60000"; claude
    ```
  </Accordion>

  <Accordion title="Server already exists">
    Você já adicionou um servidor com esse nome no mesmo escopo. Remova a entrada existente primeiro ou escolha um nome diferente:

    ```bash theme={null}
    claude mcp remove claude-code-docs
    ```

    Se o nome existe em mais de um escopo, `remove` relata `exists in multiple scopes`. Passe `--scope` para escolher qual cópia deletar, por exemplo `claude mcp remove claude-code-docs --scope local`.
  </Accordion>

  <Accordion title="Server connects but no tools appear">
    Execute `/mcp` dentro de uma sessão e selecione o servidor para ver sua lista de ferramentas. Se a lista estiver vazia, o servidor iniciou mas não registrou nenhuma ferramenta, o que geralmente significa que está faltando uma variável de ambiente necessária como uma chave de API.

    Passe a variável com `--env KEY=value` em `claude mcp add`, ou no campo `env` da entrada `.mcp.json` do servidor. A documentação do servidor lista as variáveis que ele precisa.
  </Accordion>

  <Accordion title="Changes to .mcp.json don't take effect">
    O Claude Code lê `.mcp.json` na inicialização da sessão. Saia e reinicie a sessão após editar o arquivo.

    Se seus servidores ainda não aparecerem, execute `/mcp` e procure por um aviso de análise. O Claude Code pula entradas malformadas e mostra o campo ofensivo lá.

    Se você rejeitou anteriormente o servidor quando solicitado, redefina as aprovações do projeto:

    ```bash theme={null}
    claude mcp reset-project-choices
    ```
  </Accordion>

  <Accordion title="OAuth sign-in fails or browser doesn't open">
    Execute `/mcp`, selecione o servidor e escolha `Authenticate` novamente. Se o navegador não abrir automaticamente, copie a URL mostrada no terminal e abra-a manualmente. Consulte [Autenticar com servidores MCP remotos](https://code.claude.com/docs/pt/mcp#authenticate-with-remote-mcp-servers) para portas de callback fixas e credenciais pré-configuradas.
  </Accordion>
</AccordionGroup>

<h2 id="next-steps">
  Próximos passos
</h2>

Com um servidor conectado, explore o resto do que o MCP permite:

* [Encontre mais servidores MCP](https://code.claude.com/docs/pt/mcp#find-and-build-mcp-servers) no Diretório Anthropic
* [Compartilhe servidores com sua equipe](https://code.claude.com/docs/pt/mcp#mcp-installation-scopes) usando escopos de instalação
* [Gerencie acesso MCP para uma organização](https://code.claude.com/docs/pt/managed-mcp) com configurações gerenciadas e controles de política
* [Referencie recursos MCP](https://code.claude.com/docs/pt/mcp#use-mcp-resources) em prompts com menções @
* [Execute prompts MCP como comandos](https://code.claude.com/docs/pt/mcp#use-mcp-prompts-as-commands) do menu `/`
* [Construa seu próprio servidor](https://modelcontextprotocol.io/quickstart/server) com o SDK MCP
