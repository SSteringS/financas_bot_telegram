<!-- Fonte: https://code.claude.com/docs/pt/interactive-mode | Baixado em: 2026-08-06 | slug: interactive-mode -->

# Modo interativo

> Referência completa para atalhos de teclado, modos de entrada e recursos interativos em sessões do Claude Code.

<h2 id="keyboard-shortcuts">
  Atalhos de teclado
</h2>

<Note>
  Os atalhos de teclado podem variar por plataforma e terminal. Em [renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen), pressione `?` no visualizador de transcrição para ver os atalhos disponíveis lá.

  **Usuários de macOS**: Os atalhos da tecla Option/Alt (`Alt+B`, `Alt+F`, `Alt+Y`, `Alt+M`, `Alt+P`) exigem configurar Option como Meta no seu terminal:

  * **iTerm2**: Configurações → Profiles → Keys → General → defina Left/Right Option key para "Esc+"
  * **Apple Terminal**: Configurações → Profiles → Keyboard → marque "Use Option as Meta Key"
  * **VS Code**: defina `"terminal.integrated.macOptionIsMeta": true` nas configurações do VS Code

  Veja [Configuração de terminal](https://code.claude.com/docs/pt/terminal-config) para detalhes.
</Note>

<h3 id="general-controls">
  Controles gerais
</h3>

| Atalho                                                  | Descrição                                                                                                                                                                 | Contexto                                                                                                                                                                                                                                                                                                                                               |
| :------------------------------------------------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Ctrl+C`                                                | Interromper, ou limpar entrada                                                                                                                                            | Interrompe uma operação em execução. Se nada estiver em execução, o primeiro pressionamento limpa a entrada do prompt e um segundo pressionamento sai do Claude Code                                                                                                                                                                                   |
| `Ctrl+X Ctrl+K`                                         | Encerrar todos os [subagentes em segundo plano](https://code.claude.com/docs/pt/sub-agents#run-subagents-in-foreground-or-background) nesta sessão. Pressione duas vezes em 3 segundos para confirmar | Controle de subagente                                                                                                                                                                                                                                                                                                                                  |
| `Ctrl+D`                                                | Sair da sessão do Claude Code                                                                                                                                             | Sinal EOF                                                                                                                                                                                                                                                                                                                                              |
| `Ctrl+G` ou `Ctrl+X Ctrl+E`                             | Abrir no editor de texto padrão                                                                                                                                           | Edite seu prompt ou resposta personalizada no seu editor de texto padrão. `Ctrl+X Ctrl+E` é a ligação nativa do readline. Ative Mostrar última resposta em editor externo em `/config` para adicionar a resposta anterior do Claude como contexto comentado com `#` acima do seu prompt; o bloco de comentário é removido quando você salva            |
| `Ctrl+L`                                                | Redesenhar tela                                                                                                                                                           | Força um redesenho completo do terminal. A entrada e o histórico de conversa são mantidos. Use isto para recuperar se a exibição ficar corrompida ou parcialmente em branco                                                                                                                                                                            |
| `Ctrl+O`                                                | Alternar visualizador de transcrição                                                                                                                                      | Mostra uso e execução de ferramentas detalhados, com um timestamp e o modelo usado em cada mensagem do assistente. Também expande chamadas MCP, que se contraem para uma única linha como "Chamou slack 3 vezes" por padrão                                                                                                                            |
| `Ctrl+R`                                                | Pesquisa reversa no histórico de comandos                                                                                                                                 | Pesquise através de comandos anteriores interativamente                                                                                                                                                                                                                                                                                                |
| `Ctrl+V` ou `Cmd+V` (iTerm2) ou `Alt+V` (Windows e WSL) | Colar imagem da área de transferência                                                                                                                                     | Insere um chip `[Image #N]` no cursor para que você possa referenciá-lo posicionalmente no seu prompt. No WSL, tanto `Ctrl+V` quanto `Alt+V` estão vinculados; use `Alt+V` se seu terminal interceptar `Ctrl+V`                                                                                                                                        |
| `Ctrl+B`                                                | Tarefas em execução em segundo plano                                                                                                                                      | Coloca comandos bash e agentes em segundo plano. Usuários de Tmux pressione duas vezes                                                                                                                                                                                                                                                                 |
| `Ctrl+T`                                                | Alternar lista de tarefas do Claude                                                                                                                                       | Mostrar ou ocultar a [lista de tarefas do Claude](#task-list) na área de status. Isto não é a visualização de tarefa em segundo plano; use [`/tasks`](https://code.claude.com/docs/pt/commands) para ver shells e subagentes em execução                                                                                                                                           |
| `Left/Right arrows`                                     | Ciclar através de abas de diálogo                                                                                                                                         | Navegue entre abas em diálogos de permissão e menus                                                                                                                                                                                                                                                                                                    |
| `Up/Down arrows` ou `Ctrl+P`/`Ctrl+N`                   | Mover cursor ou navegar histórico de comandos                                                                                                                             | Quando a entrada abrange mais de uma linha visual, seja envolvida ou multilinha, primeiro move o cursor dentro do prompt. Uma vez que o cursor está na primeira ou última linha visual, pressionar novamente navega pelo histórico de comandos. A partir da v2.1.169, entrada de linha única envolvida se comporta da mesma forma que multilinha       |
| `Esc`                                                   | Interromper Claude, ou fechar um diálogo                                                                                                                                  | Pare a resposta atual ou chamada de ferramenta no meio da vez para que você possa redirecionar. Claude mantém o trabalho realizado até agora. Quando um diálogo como um prompt de permissão está aberto, `Esc` fecha o diálogo em vez de interromper Claude. Antes da v2.1.202, `Esc` em alguns diálogos interrompia Claude e deixava o diálogo aberto |
| `Esc` + `Esc`                                           | Limpar rascunho de entrada, ou retroceder                                                                                                                                 | Quando a entrada do prompt contém texto, duplo `Esc` limpa e salva o rascunho no histórico para que `Up` o recupere. Quando a entrada está vazia, duplo `Esc` abre o [menu de retrocesso](https://code.claude.com/docs/pt/checkpointing) para restaurar ou resumir código e conversa de um ponto anterior                                                                          |
| `Shift+Tab` ou `Alt+M` (algumas configurações)          | Alternar modos de permissão                                                                                                                                               | Alternar entre `default` (rotulado Manual no indicador de modo), `acceptEdits`, `plan` e qualquer modo que você tenha ativado, como `auto` ou `bypassPermissions`. Veja [modos de permissão](https://code.claude.com/docs/pt/permission-modes).                                                                                                                                    |
| `Option+P` (macOS) ou `Alt+P` (Windows/Linux)           | Alternar modelo                                                                                                                                                           | Alternar modelos sem limpar seu prompt                                                                                                                                                                                                                                                                                                                 |
| `Option+T` (macOS) ou `Alt+T` (Windows/Linux)           | Alternar pensamento estendido                                                                                                                                             | Ativar ou desativar modo de pensamento estendido. Não tem efeito no Fable 5, que sempre usa pensamento estendido. A partir da v2.1.132 este atalho funciona no macOS sem configurar Option como Meta                                                                                                                                                   |
| `Option+O` (macOS) ou `Alt+O` (Windows/Linux)           | Alternar modo rápido                                                                                                                                                      | Ativar ou desativar [modo rápido](https://code.claude.com/docs/pt/fast-mode)                                                                                                                                                                                                                                                                                                       |

<h3 id="text-editing">
  Edição de texto
</h3>

| Atalho                  | Descrição                                 | Contexto                                                                                                                                                                                             |
| :---------------------- | :---------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Ctrl+A`                | Mover cursor para o início da linha atual | Em entrada multilinha, move para o início da linha lógica atual                                                                                                                                      |
| `Ctrl+E`                | Mover cursor para o final da linha atual  | Em entrada multilinha, move para o final da linha lógica atual                                                                                                                                       |
| `Ctrl+K`                | Deletar até o final da linha              | Armazena texto deletado para colar                                                                                                                                                                   |
| `Ctrl+U`                | Deletar do cursor até o início da linha   | Armazena texto deletado para colar. Repita para limpar entre linhas em entrada multilinha. No macOS, emuladores de terminal incluindo iTerm2 e Terminal.app mapeiam `Cmd+Backspace` para este atalho |
| `Ctrl+W`                | Deletar palavra anterior                  | Armazena texto deletado para colar. No Windows, `Ctrl+Backspace` também deleta a palavra anterior                                                                                                    |
| `Ctrl+Y`                | Colar texto deletado                      | Cole texto deletado com `Ctrl+K`, `Ctrl+U` ou `Ctrl+W`                                                                                                                                               |
| `Alt+Y` (após `Ctrl+Y`) | Ciclar histórico de cola                  | Após colar, cicle através de texto deletado anteriormente. Requer [Option como Meta](#keyboard-shortcuts) no macOS                                                                                   |
| `Alt+B`                 | Mover cursor uma palavra para trás        | Navegação de palavra. Requer [Option como Meta](#keyboard-shortcuts) no macOS                                                                                                                        |
| `Alt+F`                 | Mover cursor uma palavra para frente      | Navegação de palavra. Requer [Option como Meta](#keyboard-shortcuts) no macOS                                                                                                                        |

<h3 id="theme-and-display">
  Tema e exibição
</h3>

| Atalho   | Descrição                                          | Contexto                                                                                                               |
| :------- | :------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------- |
| `Ctrl+T` | Alternar destaque de sintaxe para blocos de código | Funciona apenas dentro do menu seletor `/theme`. Controla se o código nas respostas do Claude usa coloração de sintaxe |

<h3 id="multiline-input">
  Entrada multilinha
</h3>

| Método                | Atalho            | Contexto                                                                                          |
| :-------------------- | :---------------- | :------------------------------------------------------------------------------------------------ |
| Escape rápido         | `\` + `Enter`     | Funciona em todos os terminais                                                                    |
| Tecla Option          | `Option+Enter`    | Após ativar [Option como Meta](https://code.claude.com/docs/pt/terminal-config#enable-option-key-shortcuts-on-macos) no macOS |
| Shift+Enter           | `Shift+Enter`     | Nativo em iTerm2, WezTerm, Ghostty, Kitty, Warp, Apple Terminal, Windows Terminal                 |
| Sequência de controle | `Ctrl+J`          | Funciona em qualquer terminal sem configuração                                                    |
| Modo de cola          | Colar diretamente | Para blocos de código, logs                                                                       |

<Tip>
  Shift+Enter funciona sem configuração em iTerm2, WezTerm, Ghostty, Kitty, Warp, Apple Terminal e Windows Terminal. Para VS Code, Cursor, Devin Desktop, Alacritty e Zed, execute `/terminal-setup` para instalar o atalho.
</Tip>

<h3 id="quick-commands">
  Comandos rápidos
</h3>

| Atalho        | Descrição                    | Notas                                                              |
| :------------ | :--------------------------- | :----------------------------------------------------------------- |
| `/` no início | Comando ou skill             | Veja [comandos](#commands) e [skills](https://code.claude.com/docs/pt/skills)                  |
| `!` no início | Modo Bash                    | Execute comandos diretamente e adicione saída de execução à sessão |
| `@`           | Menção de caminho de arquivo | Ativar preenchimento automático de caminho de arquivo              |

<h3 id="transcript-viewer">
  Visualizador de transcrição
</h3>

Quando o visualizador de transcrição está aberto (alternado com `Ctrl+O`), estes atalhos estão disponíveis. Em [renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen), pressione `?` para mostrar o painel de referência de atalho de teclado completo dentro do visualizador. `Ctrl+E` pode ser reatribuído via [`transcript:toggleShowAll`](https://code.claude.com/docs/pt/keybindings).

| Atalho               | Descrição                                                                                                                                                                                                                                         |
| :------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `?`                  | Alternar o painel de ajuda de atalho de teclado. Requer [renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen)                                                                                                                                              |
| `{` / `}`            | Pular para o prompt do usuário anterior ou próximo, como movimento de parágrafo vim. Requer [renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen)                                                                                                          |
| `Ctrl+E`             | Alternar mostrar todo o conteúdo                                                                                                                                                                                                                  |
| `[`                  | Escrever a conversa completa no scrollback nativo do seu terminal para que `Cmd+F`, modo de cópia do tmux e outras ferramentas nativas possam pesquisá-lo. Requer [renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen#search-and-review-the-conversation) |
| `v`                  | Escrever a conversa em um arquivo temporário e abri-lo em `$VISUAL` ou `$EDITOR`. Requer [renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen)                                                                                                             |
| `q`, `Ctrl+C`, `Esc` | Sair da visualização de transcrição. Todos os três podem ser reatribuídos via [`transcript:exit`](https://code.claude.com/docs/pt/keybindings)                                                                                                                                |

<h3 id="voice-input">
  Entrada de voz
</h3>

| Atalho                  | Descrição      | Notas                                                                                                                                                                                                               |
| :---------------------- | :------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Manter ou tocar `Space` | Ditação de voz | Requer que [ditação de voz](https://code.claude.com/docs/pt/voice-dictation) esteja ativada. Mantenha pressionado para gravar, ou execute `/voice tap` para alternância por toque. [Reatribuível](https://code.claude.com/docs/pt/voice-dictation#rebind-the-dictation-key) |

<h2 id="commands">
  Comandos
</h2>

Digite `/` no Claude Code para ver todos os comandos disponíveis, ou digite `/` seguido de qualquer letra para filtrar. O menu `/` mostra tudo que você pode invocar: comandos integrados, [skills](https://code.claude.com/docs/pt/skills) agrupados e criados por usuários, e comandos contribuídos por [plugins](https://code.claude.com/docs/pt/plugins) e [servidores MCP](https://code.claude.com/docs/pt/mcp#use-mcp-prompts-as-commands). Nem todos os comandos integrados são visíveis para todos os usuários, pois alguns dependem de sua plataforma ou plano.

Na [renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen#use-the-mouse), o comando `/` e as listas de sugestão de arquivo `@` também respondem ao mouse: passar o mouse destaca uma linha e clicar a aceita.

Veja a [referência de comandos](https://code.claude.com/docs/pt/commands) para a lista completa de comandos incluídos no Claude Code.

<h2 id="vim-editor-mode">
  Modo editor Vim
</h2>

Ative edição no estilo vim via `/config` → Editor mode.

<h3 id="mode-switching">
  Alternância de modo
</h3>

| Comando | Ação                                 | Do modo        |
| :------ | :----------------------------------- | :------------- |
| `Esc`   | Entrar no modo NORMAL                | INSERT, VISUAL |
| `i`     | Inserir antes do cursor              | NORMAL         |
| `I`     | Inserir no início da linha           | NORMAL         |
| `a`     | Inserir após o cursor                | NORMAL         |
| `A`     | Inserir no final da linha            | NORMAL         |
| `o`     | Abrir linha abaixo                   | NORMAL         |
| `O`     | Abrir linha acima                    | NORMAL         |
| `v`     | Iniciar seleção visual por caractere | NORMAL         |
| `V`     | Iniciar seleção visual por linha     | NORMAL         |

<h3 id="remap-insert-mode-key-sequences">
  Remapear sequências de teclas no modo INSERT
</h3>

A configuração [`vimInsertModeRemaps`](https://code.claude.com/docs/pt/settings#available-settings) mapeia uma sequência de duas teclas no modo INSERT para Escape, então um mapeamento como `jj` o retorna ao modo NORMAL. Requer Claude Code v2.1.208 ou posterior.

O seguinte exemplo de `~/.claude/settings.json` ativa o modo vim e mapeia `jj` para Escape:

```json theme={null}
{
  "editorMode": "vim",
  "vimInsertModeRemaps": { "jj": "<Esc>" }
}
```

Cada chave tem exatamente dois caracteres imprimíveis digitados em sequência, e `"<Esc>"` é o único alvo suportado. Entradas com um comprimento ou alvo diferente são ignoradas.

Digitar o primeiro caractere de uma sequência o insere normalmente. Pressionar o segundo caractere dentro de um segundo remove esse caractere pendente e muda para o modo NORMAL, deixando nenhum caractere em sua entrada. Após a janela de um segundo, ou se uma chave diferente seguir, ambos os caracteres permanecem como texto literal, então você ainda pode digitar uma palavra contendo a sequência pausando entre as duas teclas.

Claude Code lê essa configuração do seu arquivo de configurações do usuário, da flag `--settings` e de [configurações gerenciadas](https://code.claude.com/docs/pt/permissions#managed-settings) apenas. Entradas no `.claude/settings.json` ou `.claude/settings.local.json` de um projeto são ignoradas, então um repositório verificado não pode remapear seus pressionamentos de tecla.

<h3 id="navigation-normal-mode">
  Navegação (modo NORMAL)
</h3>

| Comando         | Ação                                                                                                                                                                                   |
| :-------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `h`/`j`/`k`/`l` | Mover esquerda/baixo/cima/direita                                                                                                                                                      |
| `Space`         | Mover para a direita                                                                                                                                                                   |
| `w`             | Próxima palavra                                                                                                                                                                        |
| `e`             | Final da palavra                                                                                                                                                                       |
| `b`             | Palavra anterior                                                                                                                                                                       |
| `0`             | Início da linha                                                                                                                                                                        |
| `$`             | Final da linha                                                                                                                                                                         |
| `^`             | Primeiro caractere não em branco                                                                                                                                                       |
| `gg`            | Início da entrada                                                                                                                                                                      |
| `G`             | Final da entrada                                                                                                                                                                       |
| `f{char}`       | Pular para próxima ocorrência do caractere                                                                                                                                             |
| `F{char}`       | Pular para ocorrência anterior do caractere                                                                                                                                            |
| `t{char}`       | Pular para logo antes da próxima ocorrência do caractere                                                                                                                               |
| `T{char}`       | Pular para logo após a ocorrência anterior do caractere                                                                                                                                |
| `;`             | Repetir último movimento f/F/t/T                                                                                                                                                       |
| `,`             | Repetir último movimento f/F/t/T em reverso                                                                                                                                            |
| `/`             | Abrir busca de histórico reverso, igual a `Ctrl+R`. A partir da v2.1.191, o prompt de busca vazio mostra uma dica: pressione `Esc` depois `i` depois `/` para abrir o menu de comandos |

<Note>
  No modo normal vim, se o cursor estiver no início ou final da entrada e não puder se mover mais, `j`/`k` e as setas de navegação navegam pelo histórico de comandos.
</Note>

<h3 id="editing-normal-mode">
  Edição (modo NORMAL)
</h3>

| Comando        | Ação                                |
| :------------- | :---------------------------------- |
| `x`            | Deletar caractere                   |
| `dd`           | Deletar linha                       |
| `D`            | Deletar até o final da linha        |
| `dw`/`de`/`db` | Deletar palavra/até final/para trás |
| `cc`           | Mudar linha                         |
| `C`            | Mudar até o final da linha          |
| `cw`/`ce`/`cb` | Mudar palavra/até final/para trás   |
| `yy`/`Y`       | Yancar (copiar) linha               |
| `yw`/`ye`/`yb` | Yancar palavra/até final/para trás  |
| `p`            | Colar após o cursor                 |
| `P`            | Colar antes do cursor               |
| `>>`           | Indentar linha                      |
| `<<`           | Desindentação de linha              |
| `J`            | Juntar linhas                       |
| `u`            | Desfazer                            |
| `.`            | Repetir última mudança              |

<h3 id="text-objects-normal-mode">
  Objetos de texto (modo NORMAL)
</h3>

Objetos de texto funcionam com operadores como `d`, `c` e `y`:

| Comando   | Ação                                                       |
| :-------- | :--------------------------------------------------------- |
| `iw`/`aw` | Palavra interna/ao redor                                   |
| `iW`/`aW` | PALAVRA interna/ao redor (delimitada por espaço em branco) |
| `i"`/`a"` | Aspas duplas internas/ao redor                             |
| `i'`/`a'` | Aspas simples internas/ao redor                            |
| `i(`/`a(` | Parênteses internos/ao redor                               |
| `i[`/`a[` | Colchetes internos/ao redor                                |
| `i{`/`a{` | Chaves internas/ao redor                                   |

<h3 id="visual-mode">
  Modo visual
</h3>

Pressione `v` para seleção por caractere ou `V` para seleção por linha. Os movimentos estendem a seleção e os operadores atuam diretamente sobre ela.

| Comando          | Ação                                                      |
| :--------------- | :-------------------------------------------------------- |
| `d`/`x`          | Deletar seleção                                           |
| `y`              | Yancar seleção                                            |
| `c`/`s`          | Mudar seleção                                             |
| `p`              | Substituir seleção pelo conteúdo do registro              |
| `r{char}`        | Substituir cada caractere selecionado por `{char}`        |
| `~`/`u`/`U`      | Alternar, minúsculas ou maiúsculas na seleção             |
| `>`/`<`          | Indentar ou desindentação de linhas selecionadas          |
| `J`              | Juntar linhas selecionadas                                |
| `o`              | Trocar cursor e âncora                                    |
| `iw`/`aw`/`i"`/… | Selecionar um objeto de texto                             |
| `v`/`V`          | Alternar entre seleção por caractere e por linha, ou sair |

O modo visual por bloco com `Ctrl+V` não é suportado.

<h2 id="command-history">
  Histórico de comandos
</h2>

Claude Code mantém histórico de comandos para a sessão atual:

* O histórico de entrada é armazenado por diretório de trabalho
* O histórico de entrada é redefinido quando você executa `/clear` para iniciar uma nova sessão. A conversa da sessão anterior é preservada e pode ser retomada.
* Enviar o mesmo prompt duas vezes seguidas registra uma entrada de histórico, então pressionar Seta para cima vai para o prompt anterior distinto
* Use as setas Para cima/Para baixo para navegar (veja atalhos de teclado acima)
* Expansão de histórico com `!` está desabilitada por padrão

<h3 id="reverse-search-with-ctrl-r">
  Pesquisa reversa com Ctrl+R
</h3>

Pressione `Ctrl+R` para pesquisar interativamente através do seu histórico de comandos:

1. **Iniciar pesquisa**: pressione `Ctrl+R` para ativar pesquisa de histórico reverso
2. **Digitar consulta**: insira texto para pesquisar em comandos anteriores. O termo de pesquisa é destacado nos resultados correspondentes
3. **Navegar correspondências**: pressione `Ctrl+R` novamente para ciclar através de correspondências mais antigas
4. **Mudar escopo**: a pesquisa padrão é de prompts de todos os projetos. Pressione `Ctrl+S` para alternar o escopo entre esta sessão, este projeto e todos os projetos
5. **Aceitar correspondência**:
   * Pressione `Tab` ou `Esc` para aceitar a correspondência atual e continuar editando
   * Pressione `Enter` para aceitar e executar o comando imediatamente
6. **Cancelar pesquisa**:
   * Pressione `Ctrl+C` para cancelar e restaurar sua entrada original
   * Pressione `Backspace` em pesquisa vazia para cancelar

A pesquisa carrega os 100 prompts únicos mais recentes no escopo selecionado, com duplicatas recolhidas para a ocorrência mais recente. Os prompts correspondentes são exibidos com o termo de pesquisa destacado, para que você possa encontrar e reutilizar entradas anteriores.

Aceitar uma correspondência ou cancelar a pesquisa entra em vigor imediatamente, mesmo enquanto Claude Code ainda está carregando o histórico. Antes da v2.1.202, aceitar ou cancelar durante esse carregamento poderia relatar um erro interno.

<h2 id="background-bash-commands">
  Comandos bash em segundo plano
</h2>

Claude Code suporta execução de comandos bash em segundo plano, permitindo que você continue trabalhando enquanto processos de longa duração são executados.

<h3 id="how-backgrounding-works">
  Como o segundo plano funciona
</h3>

Quando Claude Code executa um comando em segundo plano, ele executa o comando de forma assíncrona e retorna imediatamente um ID de tarefa em segundo plano. Claude Code pode responder a novos prompts enquanto o comando continua sendo executado em segundo plano.

Para executar comandos em segundo plano, você pode:

* Solicitar ao Claude Code para executar um comando em segundo plano
* Pressionar `Ctrl+B` para mover uma invocação regular da ferramenta Bash para o segundo plano. Usuários de Tmux devem pressionar `Ctrl+B` duas vezes devido à tecla de prefixo do tmux.

**Recursos principais:**

* A saída é escrita em um arquivo e Claude pode recuperá-la usando a ferramenta Read
* Tarefas em segundo plano têm IDs únicos para rastreamento e recuperação de saída
* Tarefas em segundo plano são limpas automaticamente quando Claude Code sai. Colocar a sessão em segundo plano em vez de sair entrega-as à sessão em segundo plano, onde continuam sendo executadas. Veja [colocar uma sessão em execução em segundo plano](https://code.claude.com/docs/pt/agent-view#from-inside-a-session)
* Tarefas em segundo plano são automaticamente encerradas se a saída exceder 5GB, com uma nota em stderr explicando o motivo
* A partir da v2.1.193, em macOS e Linux, tarefas em segundo plano em execução são encerradas quando o sistema operacional sinaliza pressão de memória, desde que a sessão tenha ficado ociosa por pelo menos 30 minutos sem nenhuma volta ou subagente em execução. Defina [`CLAUDE_CODE_DISABLE_BG_SHELL_PRESSURE_REAP`](https://code.claude.com/docs/pt/env-vars) para `1` para desativar isso

Para desabilitar toda a funcionalidade de tarefa em segundo plano, defina a variável de ambiente `CLAUDE_CODE_DISABLE_BACKGROUND_TASKS` para `1`. Veja [Variáveis de ambiente](https://code.claude.com/docs/pt/env-vars) para detalhes.

**Comandos comuns em segundo plano:**

* Ferramentas de compilação (webpack, vite, make)
* Gerenciadores de pacotes (npm, yarn, pnpm)
* Executores de teste (jest, pytest)
* Servidores de desenvolvimento
* Processos de longa duração (docker, terraform)

<h3 id="shell-mode-with-prefix">
  Modo shell com prefixo `!`
</h3>

Execute comandos shell diretamente sem passar por Claude prefixando sua entrada com `!`:

```bash theme={null}
! npm test
! git status
! ls -la
```

Modo shell:

* Adiciona o comando e sua saída ao contexto de conversa
* Mostra progresso e saída em tempo real
* Suporta o mesmo segundo plano `Ctrl+B` para comandos de longa duração
* Não requer que Claude interprete ou aprove o comando
* Suporta preenchimento automático baseado em histórico: digite um comando parcial e pressione `Tab` para completar a partir de comandos `!` anteriores no projeto atual
* Suporta preenchimento automático de caminho de arquivo ao vivo a partir da v2.1.193 em todas as plataformas: digite um token contendo uma barra invertida, como `./src/` ou `~/`, para ver uma lista suspensa de arquivos e diretórios correspondentes, depois pressione `Tab` para aceitar. Use barras invertidas no Windows também; a lista suspensa é acionada por `/`, não `\`
* Saia com `Escape`, `Backspace` ou `Ctrl+U` em um prompt vazio
* Colar texto que começa com `!` em um prompt vazio entra no modo shell automaticamente, correspondendo ao comportamento digitado `!`

A partir da v2.1.186, Claude responde à saída do comando automaticamente assim que ela chega à transcrição, para que você possa executar `! npm test` e obter uma explicação das falhas sem um segundo prompt. A resposta custa o mesmo que enviar um prompt normal. Para restaurar o comportamento anterior onde a saída é adicionada ao contexto sem uma resposta, defina [`respondToBashCommands`](https://code.claude.com/docs/pt/settings#available-settings) para `false` em `settings.json`. Antes da v2.1.186, o modo shell sempre adicionava saída ao contexto sem uma resposta.

Isto é útil para operações rápidas de shell mantendo contexto de conversa.

<h2 id="prompt-suggestions">
  Sugestões de prompt
</h2>

Quando você abre uma sessão pela primeira vez, um comando de exemplo acinzentado aparece na entrada de prompt para ajudá-lo a começar. Claude Code escolhe isto do histórico git do seu projeto, então reflete arquivos nos quais você trabalhou recentemente.

Após Claude responder, as sugestões continuam aparecendo com base no seu histórico de conversa, como uma etapa de acompanhamento de uma solicitação de várias partes ou uma continuação natural do seu fluxo de trabalho.

* Pressione `Tab` ou `Right arrow` para colocar a sugestão na entrada de prompt, depois `Enter` para enviar
* Comece a digitar para descartá-la

A sugestão é executada como uma solicitação em segundo plano que reutiliza o cache de prompt da conversa pai, então o custo adicional é mínimo. Claude Code pula a geração de sugestão quando o cache está frio para evitar custo desnecessário.

As sugestões são automaticamente puladas após a primeira volta de uma conversa e em plan mode. No modo de impressão, elas estão desativadas por padrão. Passe [`--prompt-suggestions`](https://code.claude.com/docs/pt/cli-reference#cli-flags) com `--output-format stream-json --verbose` para emitir uma mensagem `prompt_suggestion` após cada volta.

Para desabilitar sugestões de prompt inteiramente, defina a variável de ambiente ou alterne a configuração em `/config`:

```bash theme={null}
export CLAUDE_CODE_ENABLE_PROMPT_SUGGESTION=false
```

<h2 id="side-questions-with-/btw">
  Perguntas laterais com /btw
</h2>

Use `/btw` para fazer uma pergunta rápida sobre seu trabalho atual sem adicionar ao histórico de conversa. Isto é útil quando você quer uma resposta rápida mas não quer bagunçar o contexto principal ou desviar Claude de uma tarefa de longa duração.

```
/btw what was the name of that config file again?
```

Perguntas laterais têm visibilidade completa da conversa atual, então você pode perguntar sobre código que Claude já leu, decisões que tomou anteriormente, ou qualquer outra coisa da sessão. A pergunta e resposta são efêmeras: aparecem em uma sobreposição descartável e nunca entram no histórico de conversa.

* **Disponível enquanto Claude está trabalhando**: você pode executar `/btw` mesmo enquanto Claude está processando uma resposta. A pergunta lateral é executada independentemente e não interrompe a volta principal.
* **Sem acesso a ferramentas**: perguntas laterais respondem apenas a partir do que já está em contexto. Claude não pode ler arquivos, executar comandos ou pesquisar ao responder uma pergunta lateral.
* **Resposta única**: não há voltas de acompanhamento na sobreposição. Para continuar a thread, divida-a em sua própria sessão com `f`.
* **Custo baixo**: a pergunta lateral reutiliza o cache de prompt da conversa pai, então o custo adicional é mínimo.

Perguntas laterais anteriores da mesma sessão aparecem como uma lista esmaecida acima da resposta atual. Elas ficam fora do histórico de conversa mas permanecem visíveis na sobreposição até você limpá-las.

Assim que a resposta aparecer, a sobreposição aceita estas teclas.

| Tecla                      | Ação                                                                                                                                                                                                                                                                                     |
| :------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Space`, `Enter`, `Escape` | Descartar a resposta e retornar ao prompt                                                                                                                                                                                                                                                |
| `Up` / `Down`              | Rolar a resposta                                                                                                                                                                                                                                                                         |
| `Left` / `Right`           | Alternar entre esta resposta e suas respostas anteriores de `/btw` da sessão. `Left` move para respostas mais antigas e `Right` retorna para a atual. Requer Claude Code v2.1.187 ou posterior                                                                                           |
| `c`                        | Copiar a resposta para sua área de transferência como Markdown bruto. Use isto em vez de seleção com mouse, que captura a renderização do terminal com quebra de linha rígida em vez do texto de origem                                                                                  |
| `f`                        | Dividir em uma nova sessão. A divisão herda a conversa pai mais esta pergunta e resposta como voltas de transcrição reais, então você pode continuar com acesso completo a ferramentas. A sessão original é preservada em [`/resume`](https://code.claude.com/docs/pt/commands). Disponível apenas em sessões locais |
| `x`                        | Limpar a lista de trocas `/btw` anteriores mostradas acima da resposta atual                                                                                                                                                                                                             |

`/btw` é o inverso de um [subagent](https://code.claude.com/docs/pt/sub-agents): vê sua conversa completa mas não tem ferramentas, enquanto um subagent tem ferramentas completas mas começa com contexto vazio. Use `/btw` para perguntar sobre o que Claude já sabe desta sessão; use um subagent para descobrir algo novo.

<h2 id="task-list">
  Lista de tarefas
</h2>

A lista de tarefas é a lista de verificação de Claude: itens que Claude criou para planejar trabalho em várias etapas, com indicadores mostrando o que está pendente, em progresso ou completo. É separada da visualização de tarefa em segundo plano. Para ver shells em execução e subagentes, use [`/tasks`](https://code.claude.com/docs/pt/commands) em vez disso.

* Pressione `Ctrl+T` para alternar a visualização da lista de tarefas. A exibição mostra até cinco tarefas por vez. Quando Claude ainda não criou nenhum item de lista de verificação, o alternador não tem efeito visível porque não há nada para exibir
* Para ver todas as tarefas ou limpá-las, peça ao Claude diretamente: "show me all tasks" ou "clear all tasks"
* As tarefas persistem através de compactações de contexto, ajudando Claude a se manter organizado em projetos maiores
* Para compartilhar uma lista de tarefas entre sessões, defina `CLAUDE_CODE_TASK_LIST_ID` para usar um diretório nomeado em `~/.claude/tasks/`: `CLAUDE_CODE_TASK_LIST_ID=my-project claude`

<h2 id="session-recap">
  Resumo de sessão
</h2>

Quando você retorna ao terminal após se afastar, Claude Code mostra um resumo de uma linha do que aconteceu na sessão até agora. O resumo é gerado em segundo plano uma vez que pelo menos três minutos tenham passado desde a última volta concluída e o terminal esteja desfocado, então está pronto quando você volta. Os resumos aparecem apenas uma vez que a sessão tenha pelo menos três voltas, e nunca duas vezes seguidas.

Execute `/recap` para gerar um resumo sob demanda. Para desativar resumos automáticos, abra `/config` e desabilite **Session recap**.

O resumo de sessão está ativado por padrão para todos os planos e provedores. O resumo é sempre pulado em modo não interativo.

<h2 id="pr-review-status">
  Status de revisão de PR
</h2>

Ao trabalhar em uma branch com um pull request aberto, Claude Code exibe um link de PR clicável no rodapé, como "PR #446". O link tem um sublinhado colorido indicando o estado de revisão:

* Verde: aprovado
* Amarelo: revisão pendente
* Vermelho: mudanças solicitadas
* Cinza: rascunho

O badge desaparece assim que o pull request é mesclado ou fechado. `Cmd+click` (macOS) ou `Ctrl+click` (Windows/Linux) no link para abrir o pull request no seu navegador. O status é atualizado a cada 60 segundos e imediatamente após um comando `gh pr` ou `git push` ser executado na sessão.

<Note>
  O status de PR requer que o CLI `gh` esteja instalado e autenticado (`gh auth login`).
</Note>

<h2 id="see-also">
  Veja também
</h2>

* [Skills](https://code.claude.com/docs/pt/skills) - Prompts e fluxos de trabalho personalizados
* [Checkpointing](https://code.claude.com/docs/pt/checkpointing) - Retroceder edições do Claude e restaurar estados anteriores
* [Referência CLI](https://code.claude.com/docs/pt/cli-reference) - Sinalizadores e opções de linha de comando
* [Configurações](https://code.claude.com/docs/pt/settings) - Opções de configuração
* [Gerenciamento de memória](https://code.claude.com/docs/pt/memory) - Gerenciando arquivos CLAUDE.md
