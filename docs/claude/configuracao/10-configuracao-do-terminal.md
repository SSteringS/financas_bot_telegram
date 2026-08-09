<!-- Fonte: https://code.claude.com/docs/pt/terminal-config | Baixado em: 2026-08-06 | slug: terminal-config -->

# Configure seu terminal para Claude Code

> Corrija Shift+Enter para quebras de linha, obtenha um sinal sonoro do terminal quando Claude terminar, configure tmux, corresponda o tema de cores e ative o modo Vim na CLI do Claude Code.

Claude Code funciona em qualquer terminal sem configuração. Esta página é para quando algo específico não está se comportando da forma que você espera. Encontre seu sintoma abaixo. Se tudo já se sente certo, você não precisa desta página.

* [Shift+Enter envia em vez de inserir uma quebra de linha](#enter-multiline-prompts)
* [Atalhos da tecla Option não funcionam no macOS](#enable-option-key-shortcuts-on-macos)
* [Sem som ou alerta quando Claude termina](#get-a-terminal-bell-or-notification)
* [Você executa Claude Code dentro do tmux](#configure-tmux)
* [A exibição pisca ou a rolagem volta para cima](#switch-to-fullscreen-rendering)
* [Você quer teclas Vim no prompt](#edit-prompts-with-vim-keybindings)

Esta página é sobre fazer seu terminal enviar os sinais corretos para Claude Code. Para alterar quais teclas Claude Code responde, consulte [atalhos de teclado](https://code.claude.com/docs/pt/keybindings) em vez disso.

<h2 id="enter-multiline-prompts">
  Inserir prompts multilinhas
</h2>

Pressionar Enter envia sua mensagem. Para adicionar uma quebra de linha sem enviar, pressione Ctrl+J, ou digite `\` e depois pressione Enter. Ambos funcionam em todos os terminais sem configuração.

Na maioria dos terminais você também pode pressionar Shift+Enter, mas o suporte varia por emulador de terminal:

| Terminal                                                                | Shift+Enter para quebra de linha               |
| :---------------------------------------------------------------------- | :--------------------------------------------- |
| Ghostty, Kitty, iTerm2, WezTerm, Warp, Apple Terminal, Windows Terminal | Funciona sem configuração                      |
| VS Code, Cursor, Devin Desktop, Alacritty, Zed                          | Execute `/terminal-setup` uma vez              |
| gnome-terminal, JetBrains IDEs como PyCharm e Android Studio            | Não disponível; use Ctrl+J ou `\` depois Enter |

Para VS Code, Cursor, Devin Desktop, Alacritty e Zed, `/terminal-setup` escreve Shift+Enter e outros atalhos de teclado no arquivo de configuração do terminal. As vinculações existentes são mantidas no lugar; se você vir uma mensagem como `VSCode terminal Shift+Enter key binding already configured`, nenhuma alteração foi feita. Execute `/terminal-setup` diretamente no terminal do host em vez de dentro do tmux ou screen, pois ele precisa escrever na configuração do terminal do host.

Em VS Code, Cursor e Devin Desktop, `/terminal-setup` também atualiza duas configurações do editor: define `terminal.integrated.gpuAcceleration` como `"off"` para evitar texto distorcido no terminal integrado, e define `terminal.integrated.mouseWheelScrollSensitivity` para rolagem mais suave no [modo de tela cheia](https://code.claude.com/docs/pt/fullscreen). Para desfazer a alteração de aceleração de GPU, defina-a de volta para `"auto"` e recarregue a janela do editor.

Se você estiver executando dentro do tmux, Shift+Enter também requer a [configuração do tmux abaixo](#configure-tmux) mesmo quando o terminal externo a suporta.

Para vincular quebra de linha a uma tecla diferente, ou para trocar o comportamento para que Enter insira uma quebra de linha e Shift+Enter envie, mapeie as ações `chat:newline` e `chat:submit` em seu [arquivo de atalhos de teclado](https://code.claude.com/docs/pt/keybindings).

<h2 id="enable-option-key-shortcuts-on-macos">
  Ativar atalhos de tecla Option no macOS
</h2>

Alguns atalhos do Claude Code usam a tecla Option, como Option+Enter para uma quebra de linha ou Option+P para trocar modelos. No macOS, a maioria dos terminais não envia Option como um modificador por padrão, então esses atalhos não funcionam até que você o ative. A configuração do terminal para isso geralmente é rotulada como "Use Option as Meta Key"; Meta é o nome histórico do Unix para a tecla agora rotulada como Option ou Alt.

<Tabs>
  <Tab title="Apple Terminal">
    Abra Configurações → Perfis → Teclado e marque "Use Option as Meta Key".

    Se você aceitou o prompt de primeira execução do Claude Code que oferecia "Option+Enter para quebras de linha e sino visual", isso já foi feito. Esse prompt executa `/terminal-setup` para você, que ativa Option como Meta e muda o sino de áudio para um flash de tela visual em seu perfil do Apple Terminal.
  </Tab>

  <Tab title="iTerm2">
    Abra Configurações → Perfis → Teclas → Geral e defina a tecla Option Esquerda e a tecla Option Direita como "Esc+".

    Executar `/terminal-setup` no iTerm2 ativa "Applications in terminal may access clipboard" em Configurações → Geral → Seleção para que o comando `/copy` possa escrever na sua área de transferência do sistema. O comando detecta iTerm2 mesmo quando executado dentro do tmux. Reinicie o iTerm2 para que a alteração tenha efeito.
  </Tab>

  <Tab title="VS Code">
    Adicione `"terminal.integrated.macOptionIsMeta": true` às suas configurações do VS Code.
  </Tab>
</Tabs>

Para Ghostty, Kitty e outros terminais, procure por uma configuração Option-as-Alt ou Option-as-Meta no arquivo de configuração do terminal.

<h2 id="get-a-terminal-bell-or-notification">
  Obtenha um sino de terminal ou notificação
</h2>

Quando Claude termina uma tarefa ou pausa para um prompt de permissão, ele dispara um evento de notificação. Exibir isso como um sino de terminal ou notificação de desktop permite que você mude para outro trabalho enquanto uma tarefa longa é executada.

Por padrão, Claude Code envia uma notificação de desktop apenas em Ghostty, Kitty e iTerm2. Em outros terminais, defina [`preferredNotifChannel`](https://code.claude.com/docs/pt/settings#available-settings) como `"terminal_bell"` para tocar o sino do terminal, ou configure um [hook de Notificação](#play-a-sound-with-a-notification-hook) para um som personalizado ou comando.

A notificação de desktop chega à sua máquina local via SSH, portanto uma sessão remota ainda pode alertá-lo. Ghostty e Kitty a encaminham para seu centro de notificações do SO sem configuração adicional. iTerm2 requer que você ative o encaminhamento:

<Steps>
  <Step title="Abra as configurações de notificação do iTerm2">
    Vá para Configurações → Perfis → Terminal.
  </Step>

  <Step title="Ative alertas">
    Marque "Notification Center Alerts", depois clique em "Filter Alerts" e ative "Send escape sequence-generated alerts".
  </Step>
</Steps>

Se as notificações ainda não aparecerem, confirme que seu aplicativo de terminal tem permissão de notificação nas configurações do seu SO, e se você estiver executando dentro do tmux, [ative passthrough](#configure-tmux).

<h3 id="play-a-sound-with-a-notification-hook">
  Play a sound with a Notification hook
</h3>

Em qualquer terminal você pode configurar um [hook de Notificação](https://code.claude.com/docs/pt/hooks-guide#get-notified-when-claude-needs-input) para reproduzir um som ou executar um comando personalizado quando Claude precisar de sua atenção. Hooks são executados junto com a notificação de desktop em vez de substituí-la, portanto terminais que não recebem uma notificação de desktop, como Warp ou o terminal integrado do VS Code, podem usar um hook ou definir `preferredNotifChannel` como `"terminal_bell"` em vez disso.

O exemplo abaixo reproduz um som do sistema no macOS. O guia vinculado tem comandos de notificação de desktop para macOS, Linux e Windows.

```json ~/.claude/settings.json theme={null}
{
  "hooks": {
    "Notification": [
      {
        "hooks": [{ "type": "command", "command": "afplay /System/Library/Sounds/Glass.aiff" }]
      }
    ]
  }
}
```

<h2 id="configure-tmux">
  Configure tmux
</h2>

Quando Claude Code é executado dentro do tmux, duas coisas quebram por padrão: Shift+Enter envia em vez de inserir uma quebra de linha, e notificações de desktop e a [barra de progresso](https://code.claude.com/docs/pt/settings#available-settings) nunca chegam ao terminal externo. Adicione estas linhas a `~/.tmux.conf`, depois execute `tmux source-file ~/.tmux.conf` para aplicá-las ao servidor em execução:

```bash ~/.tmux.conf theme={null}
set -g allow-passthrough on
set -s extended-keys on
set -as terminal-features 'xterm*:extkeys'
```

A linha `allow-passthrough` permite que notificações e atualizações de progresso cheguem ao terminal externo em vez de serem engolidas pelo tmux. As linhas `extended-keys` permitem que tmux distinga Shift+Enter de Enter simples para que o atalho de quebra de linha funcione.

<h2 id="match-the-color-theme">
  Corresponder ao tema de cores
</h2>

Use o comando `/theme`, ou o seletor de tema em `/config`, para escolher um tema do Claude Code que corresponda ao seu terminal. Selecionar a opção auto detecta o fundo claro ou escuro do seu terminal, para que o tema siga as mudanças de aparência do SO sempre que seu terminal fizer. Claude Code não controla o esquema de cores do próprio terminal, que é definido pela aplicação de terminal.

Para personalizar o que aparece na parte inferior da interface, configure uma [linha de status personalizada](https://code.claude.com/docs/pt/statusline) que mostra o modelo atual, diretório de trabalho, branch do git ou outro contexto.

<h3 id="create-a-custom-theme">
  Criar um tema personalizado
</h3>

<Note>
  Temas personalizados requerem Claude Code v2.1.118 ou posterior.
</Note>

Além dos presets integrados, `/theme` lista todos os temas personalizados que você definiu e quaisquer temas contribuídos por [plugins](https://code.claude.com/docs/pt/plugins-reference#themes) instalados. Selecione **Novo tema personalizado…** no final da lista para criar um interativamente: você nomeia o tema e depois escolhe tokens de cor individuais para substituir. Pressione `Ctrl+E` enquanto um tema personalizado está destacado para editá-lo.

Cada tema personalizado é um arquivo JSON em `~/.claude/themes/`. O nome do arquivo sem a extensão `.json` é o slug do tema, e selecionar o tema armazena `custom:<slug>` como sua preferência de tema. O arquivo tem três campos opcionais:

| Campo       | Tipo   | Descrição                                                                                                                                     |
| :---------- | :----- | :-------------------------------------------------------------------------------------------------------------------------------------------- |
| `name`      | string | Rótulo de exibição mostrado em `/theme`. Padrão é o slug do nome do arquivo                                                                   |
| `base`      | string | Preset integrado do qual o tema começa: `dark`, `light`, `dark-daltonized`, `light-daltonized`, `dark-ansi`, ou `light-ansi`. Padrão é `dark` |
| `overrides` | object | Mapa de nomes de tokens de cor para valores de cor. Tokens não listados aqui caem através do preset base                                      |

Valores de cor aceitam `#rrggbb`, `#rgb`, `rgb(r,g,b)`, `ansi256(n)`, ou `ansi:<name>` onde `<name>` é um dos 16 nomes de cores ANSI padrão como `red` ou `cyanBright`. Tokens desconhecidos e valores de cor inválidos são ignorados, portanto um erro de digitação não pode quebrar a renderização.

O exemplo a seguir define um tema que mantém o preset escuro, mas recolore o acento do prompt, o texto de erro e o texto de sucesso:

```json ~/.claude/themes/dracula.json theme={null}
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

Claude Code monitora `~/.claude/themes/` e recarrega quando um arquivo muda, portanto edições feitas no seu editor se aplicam a uma sessão em execução sem necessidade de reinicialização.

A referência abaixo cobre os tokens que você pode definir em `overrides`. O editor interativo em `/theme` mostra os mesmos tokens com uma visualização ao vivo, além de alguns acentos de propósito único, como cores de tela de integração, que são omitidas aqui.

<Accordion title="Referência de tokens de cor">
  O exemplo a seguir combina tokens de vários dos grupos abaixo: o acento da marca, a borda do modo de plano, os fundos de diff e o fundo da mensagem em tela cheia.

  ```json ~/.claude/themes/midnight.json theme={null}
  {
    "name": "Midnight",
    "base": "dark",
    "overrides": {
      "claude": "#a78bfa",
      "planMode": "#38bdf8",
      "diffAdded": "#14532d",
      "diffRemoved": "#7f1d1d",
      "userMessageBackground": "#1e1b4b"
    }
  }
  ```

  <h4 id="text-and-accent-colors">
    Cores de texto e acento
  </h4>

  Controle o acento da marca primária e as tonalidades de texto em primeiro plano usadas em toda a interface.

  | Token         | Controla                                                                 |
  | :------------ | :----------------------------------------------------------------------- |
  | `claude`      | Acento da marca primária, usado para o spinner e rótulo do assistente    |
  | `text`        | Texto em primeiro plano padrão                                           |
  | `inverseText` | Texto desenhado sobre um fundo colorido, como badges de status           |
  | `inactive`    | Texto secundário como dicas, timestamps e itens desabilitados            |
  | `subtle`      | Bordas fracas e texto secundário de-enfatizado                           |
  | `suggestion`  | Sugestões de preenchimento automático e destaque de seleção em seletores |
  | `permission`  | Bordas de diálogo, incluindo prompts de permissão e seletores            |
  | `remember`    | Indicadores de memória e `CLAUDE.md`                                     |

  <h4 id="status-colors">
    Cores de status
  </h4>

  Sinalize estados de sucesso, falha e aviso em mensagens e indicadores.

  | Token     | Controla                                            |
  | :-------- | :-------------------------------------------------- |
  | `success` | Mensagens de sucesso e verificações aprovadas       |
  | `error`   | Mensagens de erro e falhas                          |
  | `warning` | Avisos, mensagens de cautela e a borda do modo auto |
  | `merged`  | Status de pull request mesclado                     |

  <h4 id="input-box-and-mode-indicators">
    Caixa de entrada e indicadores de modo
  </h4>

  Defina a cor da borda da caixa de entrada e o acento mostrado enquanto um modo de permissão ou indicador está ativo.

  | Token          | Controla                                                  |
  | :------------- | :-------------------------------------------------------- |
  | `promptBorder` | Borda da caixa de entrada no modo de permissão padrão     |
  | `planMode`     | Acento e borda do modo de plano                           |
  | `autoAccept`   | Acento e borda do modo aceitar-edições                    |
  | `bashBorder`   | Borda da caixa de entrada ao inserir um comando shell `!` |
  | `ide`          | Indicador de conexão IDE                                  |
  | `fastMode`     | Indicador de modo rápido                                  |

  <h4 id="diff-rendering">
    Renderização de diff
  </h4>

  Colora código adicionado e removido em edições e revisões de arquivo.

  | Token               | Controla                                                    |
  | :------------------ | :---------------------------------------------------------- |
  | `diffAdded`         | Fundo de linhas adicionadas                                 |
  | `diffRemoved`       | Fundo de linhas removidas                                   |
  | `diffAddedDimmed`   | Fundo de contexto inalterado perto de linhas adicionadas    |
  | `diffRemovedDimmed` | Fundo de contexto inalterado perto de linhas removidas      |
  | `diffAddedWord`     | Destaque em nível de palavra dentro de uma linha adicionada |
  | `diffRemovedWord`   | Destaque em nível de palavra dentro de uma linha removida   |

  <h4 id="fullscreen-mode">
    Modo tela cheia
  </h4>

  Aplique apenas no [modo de renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen), onde as mensagens têm um preenchimento de fundo.

  | Token                        | Controla                                                                |
  | :--------------------------- | :---------------------------------------------------------------------- |
  | `userMessageBackground`      | Fundo atrás de suas mensagens na transcrição                            |
  | `userMessageBackgroundHover` | Fundo atrás de uma mensagem enquanto pairada ou expandida               |
  | `messageActionsBackground`   | Fundo atrás da mensagem selecionada quando a barra de ações está aberta |
  | `bashMessageBackgroundColor` | Fundo atrás de entradas de comando shell `!` na transcrição             |
  | `memoryBackgroundColor`      | Fundo atrás de entradas de memória `#` na transcrição                   |
  | `selectionBg`                | Fundo do texto selecionado com o mouse                                  |

  <h4 id="usage-meter-and-speaker-labels">
    Medidor de uso e rótulos de alto-falante
  </h4>

  Ajuste a barra mostrada na visualização `/usage` e os rótulos que distinguem suas mensagens das de Claude.

  | Token              | Controla                                          |
  | :----------------- | :------------------------------------------------ |
  | `rate_limit_fill`  | Porção preenchida do medidor de uso               |
  | `rate_limit_empty` | Porção não preenchida do medidor de uso           |
  | `briefLabelYou`    | Cor do rótulo `You` em suas mensagens             |
  | `briefLabelClaude` | Cor do rótulo `Claude` em mensagens do assistente |

  <h4 id="shimmer-variants-and-subagent-colors">
    Variantes de shimmer e cores de subagentes
  </h4>

  Vários tokens têm uma variante de shimmer emparelhada que fornece a cor mais clara usada no gradiente animado do spinner. Substitua o shimmer junto com seu token base se a animação parecer incompatível.

  * `claude` e `claudeShimmer`
  * `warning` e `warningShimmer`
  * `permission` e `permissionShimmer`
  * `promptBorder` e `promptBorderShimmer`
  * `inactive` e `inactiveShimmer`
  * `fastMode` e `fastModeShimmer`

  Cada [subagente](https://code.claude.com/docs/pt/sub-agents) e tarefa paralela é mostrado em uma das oito cores nomeadas para que você possa diferenciá-los na transcrição. Os nomes dos tokens seguem o padrão `<color>_FOR_SUBAGENTS_ONLY`, onde `<color>` é `red`, `blue`, `green`, `yellow`, `purple`, `orange`, `pink`, ou `cyan`. Substitua estes para alterar a aparência de cada cor nomeada. Por exemplo, um subagente com `color: blue` em sua definição é desenhado usando o valor `blue_FOR_SUBAGENTS_ONLY`.

  As palavras-chave [`ultrathink`](https://code.claude.com/docs/pt/model-config#use-ultrathink-for-one-off-deep-reasoning) e [`ultraplan`](https://code.claude.com/docs/pt/ultraplan) na entrada do prompt são renderizadas com um gradiente arco-íris de sete cores. Os nomes dos tokens seguem o padrão `rainbow_<color>` e `rainbow_<color>_shimmer`, onde `<color>` é `red`, `orange`, `yellow`, `green`, `blue`, `indigo`, ou `violet`.
</Accordion>

<h2 id="switch-to-fullscreen-rendering">
  Mudar para renderização em tela cheia
</h2>

Se a exibição piscar ou a posição de rolagem pular enquanto Claude está trabalhando, mude para o [modo de renderização em tela cheia](https://code.claude.com/docs/pt/fullscreen). Ele desenha em uma tela separada que o terminal reserva para aplicativos em tela cheia em vez de anexar ao seu scrollback normal, o que mantém o uso de memória plano e adiciona suporte a mouse para rolagem e seleção. Neste modo você rola com o mouse ou PageUp dentro do Claude Code em vez de com o scrollback nativo do seu terminal; consulte a [página de tela cheia](https://code.claude.com/docs/pt/fullscreen#search-and-review-the-conversation) para saber como pesquisar e copiar.

Se o cintilamento for o único problema e seu terminal suportar saída sincronizada, mas não for detectado automaticamente, como o Emacs `eat`, defina [`CLAUDE_CODE_FORCE_SYNC_OUTPUT=1`](https://code.claude.com/docs/pt/env-vars) para parar o cintilamento sem alterar os renderizadores.

Execute `/tui fullscreen` para mudar e salvar a preferência. Sua conversa é relançada intacta e as sessões futuras começam em tela cheia. Você também pode definir a variável de ambiente `CLAUDE_CODE_NO_FLICKER` antes de iniciar Claude Code:

<CodeGroup>
  ```bash Bash and Zsh theme={null}
  CLAUDE_CODE_NO_FLICKER=1 claude
  ```

  ```powershell PowerShell theme={null}
  $env:CLAUDE_CODE_NO_FLICKER = "1"; claude
  ```

  ```json ~/.claude/settings.json theme={null}
  {
    "env": {
      "CLAUDE_CODE_NO_FLICKER": "1"
    }
  }
  ```
</CodeGroup>

<h2 id="paste-large-content">
  Colar conteúdo grande
</h2>

Quando você cola mais de 10.000 caracteres no prompt, Claude Code reduz a entrada para um placeholder `[Pasted text]` para que a caixa de entrada permaneça utilizável. O conteúdo completo ainda é enviado para Claude quando você envia.

O terminal integrado do VS Code pode descartar caracteres de colagens muito grandes antes de chegarem ao Claude Code, então prefira fluxos de trabalho baseados em arquivo lá. Para entradas muito grandes, como arquivos inteiros ou logs longos, escreva o conteúdo em um arquivo e peça ao Claude para lê-lo em vez de colar. Isso mantém a transcrição da conversa legível e permite que Claude referencie o arquivo por caminho em turnos posteriores.

<h2 id="edit-prompts-with-vim-keybindings">
  Editar prompts com atalhos de teclado Vim
</h2>

Claude Code inclui um modo de edição estilo Vim para a entrada do prompt. Ative-o através de `/config` → Editor mode, ou definindo [`editorMode`](https://code.claude.com/docs/pt/settings#available-settings) como `"vim"` em `~/.claude/settings.json`. Defina Editor mode de volta para `normal` para desativá-lo.

O modo Vim suporta um subconjunto de motions de modo NORMAL e VISUAL e operadores, como navegação `hjkl`, seleção `v`/`V`, e `d`/`c`/`y` com objetos de texto. Consulte a [referência do modo editor Vim](https://code.claude.com/docs/pt/interactive-mode#vim-editor-mode) para a tabela de teclas completa.

Motions de Vim não são remapeáveis através do arquivo de atalhos de teclado. Para mapear uma sequência de modo INSERT de duas teclas como `jj` para Escape, defina [`vimInsertModeRemaps`](https://code.claude.com/docs/pt/interactive-mode#remap-insert-mode-key-sequences) nas suas configurações de usuário.

Pressionar Enter ainda envia seu prompt no modo INSERT, diferentemente do Vim padrão. Use `o` ou `O` no modo NORMAL, ou Ctrl+J, para inserir uma quebra de linha em vez disso.

<h2 id="related-resources">
  Recursos relacionados
</h2>

* [Interactive mode](https://code.claude.com/docs/pt/interactive-mode): referência completa de atalhos de teclado e a tabela de teclas Vim
* [Keybindings](https://code.claude.com/docs/pt/keybindings): remapeie qualquer atalho do Claude Code, incluindo Enter e Shift+Enter
* [Fullscreen rendering](https://code.claude.com/docs/pt/fullscreen): detalhes sobre rolagem, pesquisa e cópia no modo tela cheia
* [Hooks guide](https://code.claude.com/docs/pt/hooks-guide): mais exemplos de hook de Notificação para Linux e Windows
* [Troubleshooting](https://code.claude.com/docs/pt/troubleshooting): correções para problemas fora da configuração do terminal
