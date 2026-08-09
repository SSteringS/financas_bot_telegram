<!-- Fonte: https://code.claude.com/docs/pt/troubleshooting | Baixado em: 2026-08-06 | slug: troubleshooting -->

# Troubleshooting

> Corrija o alto uso de CPU ou memória, travamentos, thrashing de auto-compact e problemas de pesquisa no Claude Code, e encontre a página correta para outros problemas.

Esta página cobre problemas de desempenho, estabilidade e pesquisa uma vez que Claude Code está em execução. Para outros problemas, comece com a página que corresponde ao local onde você está preso:

| Sintoma                                                                                                                                               | Ir para                                                                                  |
| :---------------------------------------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------- |
| `command not found`, falha na instalação, problemas de PATH, `EACCES`, erros de TLS                                                                   | [Troubleshoot installation and login](https://code.claude.com/docs/pt/troubleshoot-install)                          |
| Atualização ou falha de download de instalação com `The connection dropped while downloading the update` ou `aborted`                                 | [Error reference](https://code.claude.com/docs/pt/errors#the-connection-dropped-while-downloading-the-update)        |
| Loops de login, erros OAuth, `403 Forbidden`, "organization disabled", credenciais Amazon Bedrock, Google Cloud's Agent Platform ou Microsoft Foundry | [Troubleshoot installation and login](https://code.claude.com/docs/pt/troubleshoot-install#login-and-authentication) |
| Configurações não aplicadas, hooks não disparando, servidores MCP não carregando                                                                      | [Debug your configuration](https://code.claude.com/docs/pt/debug-your-config)                                        |
| `API Error: 5xx`, `529 Overloaded`, `429`, erros de validação de solicitação                                                                          | [Error reference](https://code.claude.com/docs/pt/errors)                                                            |
| `model not found` ou `you may not have access to it`                                                                                                  | [Error reference](https://code.claude.com/docs/pt/errors#theres-an-issue-with-the-selected-model)                    |
| Extensão VS Code não conectando ou detectando Claude                                                                                                  | [VS Code integration](https://code.claude.com/docs/pt/vs-code#fix-common-issues)                                     |
| Plugin JetBrains ou IDE não detectado                                                                                                                 | [JetBrains integration](https://code.claude.com/docs/pt/jetbrains#troubleshooting)                                   |
| Alto uso de CPU ou memória, respostas lentas, travamentos, pesquisa não encontrando arquivos                                                          | [Performance and stability](#performance-and-stability) abaixo                           |

Se você não tem certeza qual se aplica, execute `/doctor` dentro do Claude Code para uma verificação automatizada de sua instalação, configurações, extensões e uso de contexto; ele propõe correções que pode aplicar após você confirmar. Se `claude` não iniciar completamente, execute `claude doctor` do seu shell em vez disso. Execute `/mcp` para verificar o status do servidor MCP.

<h2 id="performance-and-stability">
  Desempenho e estabilidade
</h2>

Essas seções cobrem problemas relacionados ao uso de recursos, responsividade e comportamento de pesquisa.

<h3 id="high-cpu-or-memory-usage">
  Alto uso de CPU ou memória
</h3>

Claude Code é projetado para funcionar com a maioria dos ambientes de desenvolvimento, mas pode consumir recursos significativos ao processar grandes bases de código. Se você está experimentando problemas de desempenho:

1. Use `/compact` regularmente para reduzir o tamanho do contexto
2. Feche e reinicie Claude Code entre tarefas principais
3. Considere adicionar grandes diretórios de compilação ao seu arquivo `.gitignore`
4. Reinicie com [`claude --safe-mode`](https://code.claude.com/docs/pt/cli-reference#cli-flags) para verificar se um plugin, servidor MCP ou hook é a origem. Isso desabilita todas as personalizações para a sessão; se o uso diminuir, veja [Debug your configuration](https://code.claude.com/docs/pt/debug-your-config#test-against-a-clean-configuration) para encontrar qual é

Se o uso de memória permanecer alto após essas etapas, execute `/heapdump` para escrever um snapshot de heap JavaScript e um detalhamento de memória para `~/Desktop`. No Linux sem uma pasta Desktop, os arquivos são escritos em seu diretório home.

O detalhamento mostra tamanho do conjunto residente, heap JS, buffers de array e memória nativa não contabilizada, o que ajuda a identificar se o crescimento está em objetos JavaScript ou em código nativo. Para inspecionar retentores, abra o arquivo `.heapsnapshot` no Chrome DevTools em Memory → Load; o detalhamento é o arquivo terminado em `-diagnostics.json`.

<Warning>
  O arquivo `.heapsnapshot` contém todas as strings no processo. Não o anexe a um problema público ou o compartilhe. Anexe apenas o arquivo `-diagnostics.json` ao relatar um problema de memória no [GitHub](https://github.com/anthropics/claude-code/issues). Esse arquivo contém estatísticas de memória e nenhum conteúdo de conversa ou credenciais.
</Warning>

<h3 id="large-tables-are-cut-off-in-the-terminal">
  Tabelas grandes são cortadas no terminal
</h3>

Uma tabela Markdown com mais de 200 linhas renderiza suas primeiras 200 linhas seguidas por uma linha `… N more rows not shown`. Apenas a exibição é limitada: a tabela completa permanece na conversa, e [`/copy`](https://code.claude.com/docs/pt/commands) copia cada linha. Para uma tabela muito grande para ler no terminal, peça ao Claude para escrevê-la em um arquivo em vez disso. Antes da v2.1.208, Claude Code renderizava cada linha, então retomar uma sessão que continha uma tabela muito grande poderia travar enquanto a re-renderizava.

<h3 id="auto-compaction-stops-with-a-thrashing-error">
  Auto-compactação para com erro de thrashing
</h3>

Se você vir `Autocompact is thrashing: the context refilled to the limit...`, a compactação automática foi bem-sucedida mas um arquivo ou saída de ferramenta imediatamente refilled a janela de contexto várias vezes seguidas. Claude Code para de tentar novamente para evitar desperdiçar chamadas de API em um loop que não está fazendo progresso.

Para recuperar:

1. Peça ao Claude para ler o arquivo oversized em pedaços menores, como um intervalo de linha específico ou função, em vez do arquivo inteiro
2. Execute `/compact` com um foco que descarta a saída grande, por exemplo `/compact keep only the plan and the diff`
3. Mova o trabalho de arquivo grande para um [subagent](https://code.claude.com/docs/pt/sub-agents) para que ele execute em uma janela de contexto separada
4. Execute `/clear` se a conversa anterior não for mais necessária

<h3 id="command-hangs-or-freezes">
  Comando trava ou congela
</h3>

Se Claude Code parece não responsivo:

1. Pressione Ctrl+C para tentar cancelar a operação atual
2. Se não responsivo, você pode precisar fechar o terminal e reiniciar

Reiniciar não perde sua conversa. Execute `claude --resume` no mesmo diretório para retomar a sessão.

<h3 id="garbled-or-corrupted-text-in-an-editor’s-integrated-terminal">
  Texto garbled ou corrompido no terminal integrado de um editor
</h3>

Se os caracteres renderizam como caixas, manchas ou glifos incorretos ao executar Claude Code no terminal integrado do VS Code, Cursor ou Devin Desktop, o renderizador GPU do terminal é provavelmente a causa. Execute `/terminal-setup` dentro do Claude Code para definir `terminal.integrated.gpuAcceleration` como `"off"`, ou defina-o manualmente nas configurações do seu editor e recarregue a janela. Veja [Terminal configuration](https://code.claude.com/docs/pt/terminal-config) para as outras configurações que `/terminal-setup` escreve.

<h3 id="search-and-discovery-issues">
  Problemas de pesquisa e descoberta
</h3>

Se a ferramenta Search, menções `@file`, agentes personalizados ou skills personalizados não estão encontrando arquivos, o binário `ripgrep` incluído pode não ser executado em seu sistema. Instale o pacote `ripgrep` da sua plataforma e diga ao Claude Code para usá-lo em vez disso:

<Tabs>
  <Tab title="macOS">
    ```bash theme={null}
    brew install ripgrep
    ```
  </Tab>

  <Tab title="Ubuntu/Debian">
    ```bash theme={null}
    sudo apt install ripgrep
    ```
  </Tab>

  <Tab title="Alpine">
    ```bash theme={null}
    apk add ripgrep
    ```
  </Tab>

  <Tab title="Arch">
    ```bash theme={null}
    pacman -S ripgrep
    ```
  </Tab>

  <Tab title="Windows">
    ```powershell theme={null}
    winget install BurntSushi.ripgrep.MSVC
    ```
  </Tab>
</Tabs>

Depois defina `USE_BUILTIN_RIPGREP=0` em seu [environment](https://code.claude.com/docs/pt/env-vars).

<h3 id="slow-or-incomplete-search-results-on-wsl">
  Resultados de pesquisa lentos ou incompletos em WSL
</h3>

Penalidades de desempenho de leitura de disco ao [trabalhar entre sistemas de arquivos em WSL](https://learn.microsoft.com/en-us/windows/wsl/filesystems) podem resultar em menos correspondências do que o esperado ao usar Claude Code em WSL. A pesquisa ainda funciona, mas retorna menos resultados do que em um sistema de arquivos nativo.

<Note>
  `claude doctor` mostra Search como OK neste caso.
</Note>

**Soluções:**

1. **Envie pesquisas mais específicas**: reduza o número de arquivos pesquisados especificando diretórios ou tipos de arquivo: "Search for JWT validation logic in the auth-service package" ou "Find use of md5 hash in JS files".

2. **Mova o projeto para o sistema de arquivos Linux**: se possível, certifique-se de que seu projeto está localizado no sistema de arquivos Linux (`/home/`) em vez do sistema de arquivos do Windows (`/mnt/c/`).

3. **Use Windows nativo em vez disso**: considere executar Claude Code nativamente no Windows em vez de através de WSL, para melhor desempenho do sistema de arquivos.

<h2 id="get-more-help">
  Obtenha mais ajuda
</h2>

Se você está experimentando problemas não cobertos aqui:

1. Execute `/doctor` para uma verificação de configuração e `/mcp` para verificar o status do servidor MCP
2. Use o comando `/feedback` dentro do Claude Code para relatar problemas diretamente à Anthropic
3. Verifique o [repositório GitHub](https://github.com/anthropics/claude-code) para problemas conhecidos
4. Pergunte ao Claude diretamente sobre suas capacidades e recursos. Claude tem acesso integrado à sua documentação.
