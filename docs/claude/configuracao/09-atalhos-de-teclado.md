<!-- Fonte: https://code.claude.com/docs/pt/keybindings | Baixado em: 2026-08-06 | slug: keybindings -->

# Personalizar atalhos de teclado

> Personalize atalhos de teclado no Claude Code com um arquivo de configuração de keybindings.

Claude Code suporta atalhos de teclado personalizáveis. Execute `/keybindings` para criar ou abrir seu arquivo de configuração em `~/.claude/keybindings.json`.

<h2 id="configuration-file">
  Arquivo de configuração
</h2>

O arquivo de configuração de atalhos de teclado é um objeto com um array `bindings`. Cada bloco especifica um contexto e um mapa de sequências de teclas para ações.

<Note>As alterações no arquivo de atalhos de teclado são detectadas automaticamente e aplicadas sem reiniciar Claude Code.</Note>

| Campo      | Descrição                                                |
| :--------- | :------------------------------------------------------- |
| `$schema`  | URL opcional do JSON Schema para autocompletar do editor |
| `$docs`    | URL opcional de documentação                             |
| `bindings` | Array de blocos de vinculação por contexto               |

Este exemplo vincula `Ctrl+E` para abrir um editor externo no contexto de chat e desvincula `Ctrl+U`:

```json theme={null}
{
  "$schema": "https://www.schemastore.org/claude-code-keybindings.json",
  "$docs": "https://code.claude.com/docs/pt/keybindings",
  "bindings": [
    {
      "context": "Chat",
      "bindings": {
        "ctrl+e": "chat:externalEditor",
        "ctrl+u": null
      }
    }
  ]
}
```

<h2 id="contexts">
  Contextos
</h2>

Cada bloco de vinculação especifica um **contexto** onde as vinculações se aplicam:

| Contexto          | Descrição                                                 |
| :---------------- | :-------------------------------------------------------- |
| `Global`          | Aplica-se em qualquer lugar do aplicativo                 |
| `Chat`            | Área principal de entrada de chat                         |
| `Autocomplete`    | Menu de autocompletar está aberto                         |
| `Settings`        | Menu de configurações                                     |
| `Confirmation`    | Diálogos de permissão e confirmação                       |
| `Tabs`            | Componentes de navegação de abas                          |
| `Help`            | Menu de ajuda está visível                                |
| `Transcript`      | Visualizador de transcrição                               |
| `HistorySearch`   | Modo de busca de histórico (Ctrl+R)                       |
| `Task`            | Tarefa em segundo plano está em execução                  |
| `ThemePicker`     | Diálogo do seletor de tema                                |
| `Attachments`     | Navegação de anexo de imagem em diálogos de seleção       |
| `Footer`          | Navegação do indicador de rodapé (tarefas, equipes, diff) |
| `MessageSelector` | Seleção de mensagem do diálogo de retrocesso e resumo     |
| `DiffDialog`      | Navegação do visualizador de diff                         |
| `ModelPicker`     | Nível de esforço do seletor de modelo                     |
| `Select`          | Componentes genéricos de seleção/lista                    |
| `Plugin`          | Diálogo de plugin (procurar, descobrir, gerenciar)        |
| `Scroll`          | Rolagem de conversa e seleção de texto em modo tela cheia |

Antes da v2.1.205, um contexto `Doctor` e uma ação `doctor:fix` existiam para a tela de diagnósticos `/doctor`.

<h2 id="available-actions">
  Ações disponíveis
</h2>

As ações seguem um formato `namespace:action`, como `chat:submit` para enviar uma mensagem ou `app:toggleTodos` para mostrar a lista de tarefas. Cada contexto tem ações específicas disponíveis.

<h3 id="app-actions">
  Ações do aplicativo
</h3>

Ações disponíveis no contexto `Global`:

| Ação                   | Padrão         | Descrição                                                                                                                          |
| :--------------------- | :------------- | :--------------------------------------------------------------------------------------------------------------------------------- |
| `app:interrupt`        | Ctrl+C         | Cancelar operação atual                                                                                                            |
| `app:exit`             | Ctrl+D         | Sair do Claude Code                                                                                                                |
| `app:redraw`           | (desvinculado) | Forçar redesenho do terminal                                                                                                       |
| `app:toggleTodos`      | Ctrl+T         | Alternar visibilidade da lista de tarefas do Claude. Esta não é a visualização de tarefa em segundo plano [`/tasks`](https://code.claude.com/docs/pt/commands) |
| `app:toggleTranscript` | Ctrl+O         | Alternar transcrição detalhada                                                                                                     |

<h3 id="history-actions">
  Ações de histórico
</h3>

Ações para navegar no histórico de comandos:

| Ação               | Padrão | Descrição                  |
| :----------------- | :----- | :------------------------- |
| `history:search`   | Ctrl+R | Abrir busca de histórico   |
| `history:previous` | Up     | Item de histórico anterior |
| `history:next`     | Down   | Próximo item de histórico  |

<h3 id="chat-actions">
  Ações de chat
</h3>

Ações disponíveis no contexto `Chat`:

| Ação                  | Padrão                          | Descrição                                                                                                                                                                                      |
| :-------------------- | :------------------------------ | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `chat:cancel`         | Escape                          | Cancelar entrada atual                                                                                                                                                                         |
| `chat:clearInput`     | Ctrl+L                          | Forçar um redesenho de tela cheia, preservando a entrada. Na [renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen#clear-the-conversation), pressione duas vezes em dois segundos para executar `/clear` |
| `chat:clearScreen`    | Cmd+K                           | Na [renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen#clear-the-conversation), pressione duas vezes em dois segundos para executar `/clear`                                                           |
| `chat:killAgents`     | Ctrl+X Ctrl+K                   | Encerrar todos os [subagentes em segundo plano](https://code.claude.com/docs/pt/sub-agents#run-subagents-in-foreground-or-background) nesta sessão                                                                         |
| `chat:cycleMode`      | Shift+Tab\*                     | Ciclar modos de permissão                                                                                                                                                                      |
| `chat:modelPicker`    | Meta+P                          | Abrir seletor de modelo                                                                                                                                                                        |
| `chat:fastMode`       | Meta+O                          | Alternar modo rápido                                                                                                                                                                           |
| `chat:thinkingToggle` | Meta+T                          | Alternar pensamento estendido                                                                                                                                                                  |
| `chat:submit`         | Enter                           | Enviar mensagem                                                                                                                                                                                |
| `chat:newline`        | Ctrl+J                          | Inserir uma nova linha sem enviar                                                                                                                                                              |
| `chat:undo`           | Ctrl+\_, Ctrl+Shift+-           | Desfazer última ação                                                                                                                                                                           |
| `chat:externalEditor` | Ctrl+G, Ctrl+X Ctrl+E           | Abrir em editor externo                                                                                                                                                                        |
| `chat:stash`          | Ctrl+S                          | Guardar prompt atual                                                                                                                                                                           |
| `chat:imagePaste`     | Ctrl+V (Alt+V no Windows e WSL) | Colar imagem da área de transferência. No WSL, ambos os atalhos estão vinculados por padrão                                                                                                    |

\*No Windows sem modo VT (Node \<24.2.0/\<22.17.0, Bun \<1.2.23), o padrão é Meta+M.

<h3 id="autocomplete-actions">
  Ações de autocompletar
</h3>

Ações disponíveis no contexto `Autocomplete`:

| Ação                    | Padrão | Descrição         |
| :---------------------- | :----- | :---------------- |
| `autocomplete:accept`   | Tab    | Aceitar sugestão  |
| `autocomplete:dismiss`  | Escape | Descartar menu    |
| `autocomplete:previous` | Up     | Sugestão anterior |
| `autocomplete:next`     | Down   | Próxima sugestão  |

<h3 id="confirmation-actions">
  Ações de confirmação
</h3>

Ações disponíveis no contexto `Confirmation`:

| Ação                        | Padrão         | Descrição                                                                                                                           |
| :-------------------------- | :------------- | :---------------------------------------------------------------------------------------------------------------------------------- |
| `confirm:yes`               | Y, Enter       | Confirmar ação                                                                                                                      |
| `confirm:no`                | N, Escape      | Recusar ação                                                                                                                        |
| `confirm:previous`          | Up             | Opção anterior                                                                                                                      |
| `confirm:next`              | Down           | Próxima opção                                                                                                                       |
| `confirm:nextField`         | Tab            | Próximo campo                                                                                                                       |
| `confirm:previousField`     | (desvinculado) | Campo anterior                                                                                                                      |
| `confirm:toggle`            | Space          | Alternar seleção                                                                                                                    |
| `confirm:cycleMode`         | Shift+Tab      | Ciclar modos de permissão                                                                                                           |
| `confirm:toggleExplanation` | Ctrl+E         | Alternar uma [explicação gerada por modelo do comando](https://code.claude.com/docs/pt/permissions#permission-system) em prompts de permissão Bash e PowerShell |

<h3 id="permission-actions">
  Ações de permissão
</h3>

Ações disponíveis no contexto `Confirmation` para diálogos de permissão:

| Ação                     | Padrão         | Descrição                                                                                                                        |
| :----------------------- | :------------- | :------------------------------------------------------------------------------------------------------------------------------- |
| `permission:toggleDebug` | (desvinculado) | Alternar informações de depuração de permissão. O padrão anterior de Ctrl+D foi removido na v2.1.146 porque sombreava `app:exit` |

<h3 id="transcript-actions">
  Ações de transcrição
</h3>

Ações disponíveis no contexto `Transcript`:

| Ação                       | Padrão            | Descrição                           |
| :------------------------- | :---------------- | :---------------------------------- |
| `transcript:toggleShowAll` | Ctrl+E            | Alternar mostrar todo o conteúdo    |
| `transcript:exit`          | q, Ctrl+C, Escape | Sair da visualização de transcrição |

<h3 id="history-search-actions">
  Ações de busca de histórico
</h3>

Ações disponíveis no contexto `HistorySearch`:

| Ação                       | Padrão      | Descrição                                         |
| :------------------------- | :---------- | :------------------------------------------------ |
| `historySearch:next`       | Ctrl+R      | Próxima correspondência                           |
| `historySearch:accept`     | Escape, Tab | Aceitar seleção                                   |
| `historySearch:cancel`     | Ctrl+C      | Cancelar busca                                    |
| `historySearch:execute`    | Enter       | Executar comando selecionado                      |
| `historySearch:cycleScope` | Ctrl+S      | Ciclar escopo: sessão, projeto, em qualquer lugar |

<h3 id="task-actions">
  Ações de tarefa
</h3>

Ações disponíveis no contexto `Task`:

| Ação              | Padrão                | Descrição                                                                                                                        |
| :---------------- | :-------------------- | :------------------------------------------------------------------------------------------------------------------------------- |
| `task:background` | Ctrl+B, Ctrl+X Ctrl+B | Colocar tarefa atual em segundo plano. O acorde Ctrl+X Ctrl+B requer v2.1.169 ou posterior e evita o conflito de prefixo do tmux |

<h3 id="theme-actions">
  Ações de tema
</h3>

Ações disponíveis no contexto `ThemePicker`:

| Ação                             | Padrão | Descrição                    |
| :------------------------------- | :----- | :--------------------------- |
| `theme:toggleSyntaxHighlighting` | Ctrl+T | Alternar destaque de sintaxe |

<h3 id="help-actions">
  Ações de ajuda
</h3>

Ações disponíveis no contexto `Help`:

| Ação           | Padrão | Descrição            |
| :------------- | :----- | :------------------- |
| `help:dismiss` | Escape | Fechar menu de ajuda |

<h3 id="tabs-actions">
  Ações de abas
</h3>

Ações disponíveis no contexto `Tabs`:

| Ação            | Padrão          | Descrição    |
| :-------------- | :-------------- | :----------- |
| `tabs:next`     | Tab, Right      | Próxima aba  |
| `tabs:previous` | Shift+Tab, Left | Aba anterior |

<h3 id="attachments-actions">
  Ações de anexos
</h3>

Ações disponíveis no contexto `Attachments`:

| Ação                   | Padrão            | Descrição                   |
| :--------------------- | :---------------- | :-------------------------- |
| `attachments:next`     | Right             | Próximo anexo               |
| `attachments:previous` | Left              | Anexo anterior              |
| `attachments:remove`   | Backspace, Delete | Remover anexo selecionado   |
| `attachments:exit`     | Down, Escape      | Sair da navegação de anexos |

<h3 id="footer-actions">
  Ações de rodapé
</h3>

Ações disponíveis no contexto `Footer`:

| Ação                    | Padrão | Descrição                                          |
| :---------------------- | :----- | :------------------------------------------------- |
| `footer:next`           | Right  | Próximo item do rodapé                             |
| `footer:previous`       | Left   | Item anterior do rodapé                            |
| `footer:up`             | Up     | Navegar para cima no rodapé (desseleciona no topo) |
| `footer:down`           | Down   | Navegar para baixo no rodapé                       |
| `footer:openSelected`   | Enter  | Abrir item do rodapé selecionado                   |
| `footer:clearSelection` | Escape | Limpar seleção do rodapé                           |

<h3 id="message-selector-actions">
  Ações do seletor de mensagem
</h3>

Ações disponíveis no contexto `MessageSelector`:

| Ação                     | Padrão                                    | Descrição                 |
| :----------------------- | :---------------------------------------- | :------------------------ |
| `messageSelector:up`     | Up, K, Ctrl+P                             | Mover para cima na lista  |
| `messageSelector:down`   | Down, J, Ctrl+N                           | Mover para baixo na lista |
| `messageSelector:top`    | Ctrl+Up, Shift+Up, Meta+Up, Shift+K       | Pular para o topo         |
| `messageSelector:bottom` | Ctrl+Down, Shift+Down, Meta+Down, Shift+J | Pular para o final        |
| `messageSelector:select` | Enter                                     | Selecionar mensagem       |

<h3 id="diff-actions">
  Ações de diff
</h3>

Ações disponíveis no contexto `DiffDialog`:

| Ação                  | Padrão         | Descrição                                                                                                                                                          |
| :-------------------- | :------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `diff:dismiss`        | Escape         | Fechar visualizador de diff; da visualização de detalhes, retorna à lista de arquivos em vez disso                                                                 |
| `diff:previousSource` | Left           | Fonte de diff anterior                                                                                                                                             |
| `diff:nextSource`     | Right          | Próxima fonte de diff                                                                                                                                              |
| `diff:previousFile`   | Up, K          | Arquivo anterior na lista de arquivos; rolar para cima uma linha na visualização de detalhes                                                                       |
| `diff:nextFile`       | Down, J        | Próximo arquivo na lista de arquivos; rolar para baixo uma linha na visualização de detalhes                                                                       |
| `diff:viewDetails`    | Enter          | Visualizar detalhes do diff                                                                                                                                        |
| `diff:back`           | (desvinculado) | Voltar no visualizador de diff. Escape executa a ação de voltar via `diff:dismiss`. O padrão anterior de Left na visualização de detalhes foi removido na v2.1.203 |

A visualização de detalhes do diff também vincula atalhos de teclado no estilo pager às [ações de rolagem](#scroll-actions) padrão. Essas vinculações fazem parte do contexto `DiffDialog` e se aplicam apenas na visualização de detalhes; os padrões do contexto `Scroll` listados em [Ações de rolagem](#scroll-actions) permanecem inalterados.

| Ação                  | Padrão         | Descrição                                            |
| :-------------------- | :------------- | :--------------------------------------------------- |
| `scroll:pageUp`       | PageUp         | Rolar para cima metade da janela de visualização     |
| `scroll:pageDown`     | PageDown       | Rolar para baixo metade da janela de visualização    |
| `scroll:fullPageUp`   | Shift+Space, B | Rolar para cima uma janela de visualização completa  |
| `scroll:fullPageDown` | Space          | Rolar para baixo uma janela de visualização completa |
| `scroll:top`          | G, Home        | Pular para o topo                                    |
| `scroll:bottom`       | Shift+G, End   | Pular para o final                                   |

<h3 id="model-picker-actions">
  Ações do seletor de modelo
</h3>

Ações disponíveis no contexto `ModelPicker`:

| Ação                          | Padrão | Descrição                                     |
| :---------------------------- | :----- | :-------------------------------------------- |
| `modelPicker:decreaseEffort`  | Left   | Diminuir nível de esforço                     |
| `modelPicker:increaseEffort`  | Right  | Aumentar nível de esforço                     |
| `modelPicker:thisSessionOnly` | s      | Aplicar modelo destacado apenas a esta sessão |

<h3 id="select-actions">
  Ações de seleção
</h3>

Ações disponíveis no contexto `Select`:

| Ação              | Padrão          | Descrição        |
| :---------------- | :-------------- | :--------------- |
| `select:next`     | Down, J, Ctrl+N | Próxima opção    |
| `select:previous` | Up, K, Ctrl+P   | Opção anterior   |
| `select:accept`   | Enter           | Aceitar seleção  |
| `select:cancel`   | Escape          | Cancelar seleção |

<h3 id="plugin-actions">
  Ações de plugin
</h3>

Ações disponíveis no contexto `Plugin`:

| Ação              | Padrão | Descrição                                                                                           |
| :---------------- | :----- | :-------------------------------------------------------------------------------------------------- |
| `plugin:toggle`   | Space  | Alternar seleção de plugin                                                                          |
| `plugin:install`  | I      | Instalar plugins selecionados                                                                       |
| `plugin:favorite` | F      | Marcar o plugin selecionado como favorito para que seja classificado perto do topo da aba Instalado |

<h3 id="settings-actions">
  Ações de configurações
</h3>

Ações disponíveis no contexto `Settings`. As ações `select:accept` e `confirm:no` são reutilizadas dos contextos [Select](#select-actions) e [Confirmation](#confirmation-actions) com comportamento específico de Configurações: as alterações se aplicam a cada configuração assim que você a altera, portanto Escape fecha o painel com suas alterações salvas em vez de recusar.

| Ação              | Padrão       | Descrição                                               |
| :---------------- | :----------- | :------------------------------------------------------ |
| `settings:search` | /            | Entrar no modo de busca                                 |
| `settings:retry`  | R            | Tentar novamente carregar dados de uso em caso de erro  |
| `select:accept`   | Enter, Space | Alterar a configuração selecionada ou abrir seu submenu |
| `confirm:no`      | Escape       | Fechar o painel. As alterações já foram salvas          |

<h3 id="voice-actions">
  Ações de voz
</h3>

Ações disponíveis no contexto `Chat` quando a [ditação por voz](https://code.claude.com/docs/pt/voice-dictation) está ativada:

| Ação               | Padrão | Descrição                                                                  |
| :----------------- | :----- | :------------------------------------------------------------------------- |
| `voice:pushToTalk` | Space  | Ditar um prompt. Mantenha pressionado ou toque dependendo do modo `/voice` |

<h3 id="scroll-actions">
  Ações de rolagem
</h3>

Ações disponíveis no contexto `Scroll` quando a [renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen) está ativada:

| Ação                        | Padrão               | Descrição                                                                                                                                   |
| :-------------------------- | :------------------- | :------------------------------------------------------------------------------------------------------------------------------------------ |
| `scroll:lineUp`             | (desvinculado)       | Rolar para cima uma linha. A rolagem da roda do mouse dispara esta ação                                                                     |
| `scroll:lineDown`           | (desvinculado)       | Rolar para baixo uma linha. A rolagem da roda do mouse dispara esta ação                                                                    |
| `scroll:pageUp`             | PageUp               | Rolar para cima metade da altura da janela de visualização                                                                                  |
| `scroll:pageDown`           | PageDown             | Rolar para baixo metade da altura da janela de visualização                                                                                 |
| `scroll:top`                | Ctrl+Home            | Pular para o início da conversa                                                                                                             |
| `scroll:bottom`             | Ctrl+End             | Pular para a mensagem mais recente e reativar o auto-follow                                                                                 |
| `scroll:halfPageUp`         | (desvinculado)       | Rolar para cima metade da altura da janela de visualização. Mesmo comportamento que `scroll:pageUp`, fornecido para rebinds no estilo vi    |
| `scroll:halfPageDown`       | (desvinculado)       | Rolar para baixo metade da altura da janela de visualização. Mesmo comportamento que `scroll:pageDown`, fornecido para rebinds no estilo vi |
| `scroll:fullPageUp`         | (desvinculado)       | Rolar para cima a altura completa da janela de visualização                                                                                 |
| `scroll:fullPageDown`       | (desvinculado)       | Rolar para baixo a altura completa da janela de visualização                                                                                |
| `selection:copy`            | Ctrl+Shift+C / Cmd+C | Copiar o texto selecionado para a área de transferência                                                                                     |
| `selection:clear`           | (desvinculado)       | Limpar a seleção de texto ativa                                                                                                             |
| `selection:extendLeft`      | Shift+Left           | Estender a seleção ativa uma coluna para a esquerda                                                                                         |
| `selection:extendRight`     | Shift+Right          | Estender a seleção ativa uma coluna para a direita                                                                                          |
| `selection:extendUp`        | Shift+Up             | Estender a seleção ativa uma linha para cima. Rola a janela de visualização quando a seleção atinge a borda superior                        |
| `selection:extendDown`      | Shift+Down           | Estender a seleção ativa uma linha para baixo. Rola a janela de visualização quando a seleção atinge a borda inferior                       |
| `selection:extendLineStart` | Shift+Home           | Estender a seleção ativa para o início da linha                                                                                             |
| `selection:extendLineEnd`   | Shift+End            | Estender a seleção ativa para o final da linha                                                                                              |

<h2 id="keystroke-syntax">
  Sintaxe de sequência de teclas
</h2>

<h3 id="modifiers">
  Modificadores
</h3>

Use teclas modificadoras com o separador `+`:

* `ctrl` ou `control` - Tecla Control
* `shift` - Tecla Shift
* `alt`, `opt`, `option`, ou `meta` - Tecla Alt no Windows e Linux, tecla Option no macOS
* `cmd`, `command`, `super`, ou `win` - Tecla Command no macOS, tecla Windows no Windows, tecla Super no Linux

O grupo `cmd` é detectado apenas em terminais que relatam o modificador Super, como aqueles que suportam o protocolo de teclado Kitty ou o modo `modifyOtherKeys` do xterm. A maioria dos terminais não o envia, portanto use `ctrl` ou `meta` para atalhos de teclado que você deseja que funcionem em qualquer lugar.

Por exemplo:

```text theme={null}
ctrl+k          Ctrl + K
shift+tab       Shift + Tab
meta+p          Option + P no macOS, Alt + P em outros lugares
ctrl+shift+c    Múltiplos modificadores
```

<h3 id="uppercase-letters">
  Letras maiúsculas
</h3>

Uma letra maiúscula isolada implica Shift. Por exemplo, `K` é equivalente a `shift+k`. Isso é útil para atalhos de teclado no estilo vim, onde as teclas maiúsculas e minúsculas têm significados diferentes.

Letras maiúsculas com modificadores (por exemplo, `ctrl+K`) são tratadas como estilísticas e **não** implicam Shift: `ctrl+K` é o mesmo que `ctrl+k`.

<h3 id="chords">
  Acordes
</h3>

Acordes são sequências de sequências de teclas separadas por espaços:

```text theme={null}
ctrl+k ctrl+s   Pressione Ctrl+K, solte, depois Ctrl+S
```

<h3 id="special-keys">
  Teclas especiais
</h3>

* `escape` ou `esc` - Tecla Escape
* `enter` ou `return` - Tecla Enter
* `tab` - Tecla Tab
* `space` - Barra de espaço
* `up`, `down`, `left`, `right` - Teclas de seta
* `backspace`, `delete` - Teclas de exclusão

<h2 id="unbind-default-shortcuts">
  Desvinculação de atalhos padrão
</h2>

Defina uma ação como `null` para desvinculá-la de um atalho padrão:

```json theme={null}
{
  "bindings": [
    {
      "context": "Chat",
      "bindings": {
        "ctrl+s": null
      }
    }
  ]
}
```

Isso também funciona para vinculações de acordes. Desvinculando cada acorde que compartilha um prefixo libera esse prefixo para uso como uma vinculação de tecla única. Um acorde em qualquer contexto ativo mantém seu prefixo reservado, portanto você deve desvinculá-lo em cada contexto que o define.

A família padrão `Ctrl+X` abrange dois contextos: `ctrl+x ctrl+k` e `ctrl+x ctrl+e` em `Chat`, e `ctrl+x ctrl+b` em `Task`. Para recuperar `ctrl+x` como uma vinculação de tecla única, desvinculá todos eles:

```json theme={null}
{
  "bindings": [
    {
      "context": "Task",
      "bindings": {
        "ctrl+x ctrl+b": null
      }
    },
    {
      "context": "Chat",
      "bindings": {
        "ctrl+x ctrl+k": null,
        "ctrl+x ctrl+e": null,
        "ctrl+x": "chat:newline"
      }
    }
  ]
}
```

Se você desvinculá alguns, mas não todos os acordes em um prefixo, pressionar o prefixo ainda entra no modo de espera de acorde para as vinculações restantes.

<h2 id="reserved-shortcuts">
  Atalhos reservados
</h2>

Estes atalhos não podem ser revinculados:

| Atalho    | Motivo                                          |
| :-------- | :---------------------------------------------- |
| Ctrl+C    | Interrupção/cancelamento codificado             |
| Ctrl+D    | Saída codificada                                |
| Ctrl+M    | Idêntico a Enter em terminais (ambos enviam CR) |
| Caps Lock | Não entregue a aplicações de terminal           |

<h2 id="terminal-conflicts">
  Conflitos de terminal
</h2>

Alguns atalhos podem entrar em conflito com multiplexadores de terminal:

| Atalho | Conflito                                        |
| :----- | :---------------------------------------------- |
| Ctrl+B | Prefixo tmux (pressione duas vezes para enviar) |
| Ctrl+A | Prefixo GNU screen                              |
| Ctrl+Z | Suspensão de processo Unix (SIGTSTP)            |

<h2 id="vim-mode-interaction">
  Interação com modo vim
</h2>

Quando o modo vim está ativado via `/config` → Editor mode, atalhos de teclado e modo vim operam independentemente:

* **Modo vim** manipula entrada no nível de entrada de texto (movimento do cursor, modos, motions)
* **Atalhos de teclado** manipulam ações no nível de componente (alternar tarefas, enviar, etc.)
* A tecla Escape no modo vim muda INSERT para NORMAL; ela não dispara `chat:cancel`
* A maioria dos atalhos Ctrl+key passam pelo modo vim para o sistema de atalhos de teclado
* As chaves vim não são remapeáveis através do arquivo de atalhos de teclado. Para mapear uma sequência de dois-key no modo INSERT como `jj` para Escape, use a configuração [`vimInsertModeRemaps`](https://code.claude.com/docs/pt/interactive-mode#remap-insert-mode-key-sequences)
* No modo NORMAL do vim, `?` mostra o menu de ajuda (comportamento vim)
* No modo NORMAL do vim, `/` abre a busca de histórico, o mesmo que Ctrl+R no modo padrão

<h2 id="validation">
  Validação
</h2>

Claude Code valida seus atalhos de teclado e mostra avisos para:

* Erros de análise (JSON inválido ou estrutura)
* Nomes de contexto inválidos
* Conflitos de atalho reservado
* Conflitos de multiplexador de terminal
* Vinculações duplicadas no mesmo contexto

Claude Code relata avisos quando o arquivo é carregado e escreve cada um no log de depuração. Inicie Claude Code com [`--debug`](https://code.claude.com/docs/pt/cli-reference#cli-flags) para ver os detalhes.
