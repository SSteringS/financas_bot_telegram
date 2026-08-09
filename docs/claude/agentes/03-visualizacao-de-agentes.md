<!-- Fonte: https://code.claude.com/docs/pt/agent-view | Baixado em: 2026-08-06 | slug: agent-view -->

# Gerenciar múltiplos agentes com agent view

> Despache e gerencie muitas sessões Claude Code a partir de uma tela. Agent view mostra o que cada sessão está fazendo e quais precisam de sua entrada.

Agent view, aberto com `claude agents`, é uma tela para todas as suas sessões em background: o que está em execução, o que precisa de sua entrada e o que está concluído. Despache novas sessões, observe seu estado rapidamente em vez de rolar pelos transcritos e intervenha apenas quando uma precisar de você. Cada sessão em background é uma conversa completa do Claude Code que continua em execução sem um terminal anexado, então você pode abri-la, responder e sair sempre que quiser.

<img src="https://mintcdn.com/claude-code/1B48Qz2Z9hac4SLG/images/agent-view-light.png?fit=max&auto=format&n=1B48Qz2Z9hac4SLG&q=85&s=7a186c96ed47d6700d084d77e786be65" className="dark:hidden" alt="Agent view em um terminal: o cabeçalho mostra Claude Code v2.1.140, o modelo, o diretório de trabalho e uma contagem de resumo. As sessões são agrupadas em Precisa de entrada, Trabalhando e Concluído, com uma entrada de despacho na parte inferior e um rodapé de dicas de atalhos de teclado." width="1772" height="780" data-path="images/agent-view-light.png" />

<img src="https://mintcdn.com/claude-code/1B48Qz2Z9hac4SLG/images/agent-view-dark.png?fit=max&auto=format&n=1B48Qz2Z9hac4SLG&q=85&s=a5bed7434bae368faea3a8f023b52aa2" className="hidden dark:block" alt="Agent view em um terminal: o cabeçalho mostra Claude Code v2.1.140, o modelo, o diretório de trabalho e uma contagem de resumo. As sessões são agrupadas em Precisa de entrada, Trabalhando e Concluído, com uma entrada de despacho na parte inferior e um rodapé de dicas de atalhos de teclado." width="1772" height="780" data-path="images/agent-view-dark.png" />

Use agent view quando você tiver várias tarefas independentes que Claude pode trabalhar sem você observar cada passo. Despache uma correção de bug, uma revisão de pull request e uma investigação de teste instável como três linhas, continue trabalhando em outra janela e verifique quando uma linha mostrar que precisa de você ou tem um resultado.

Quando você quer trabalhar de forma mais direta em qualquer sessão de um agente, anexe-se à linha para entrar na conversa completa.

Para comparar agent view com subagentes, equipes de agentes e worktrees, consulte [Executar agentes em paralelo](https://code.claude.com/docs/pt/agents).

<Note>
  Agent view está em visualização de pesquisa e requer Claude Code v2.1.139 ou posterior. Verifique sua versão com `claude --version`. A interface e os atalhos de teclado podem mudar conforme o recurso evolui.
</Note>

Esta página cobre:

* [Início rápido](#quick-start): dê a Claude uma tarefa para trabalhar em background, verifique-a e intervenha quando necessário
* [Monitorar sessões com agent view](#monitor-sessions-with-agent-view), incluindo ícones de estado, espiada e resposta, anexação, organização e atalhos de teclado
* [Despache novos agentes](#dispatch-new-agents) a partir de agent view, de dentro de uma sessão ou do seu shell
* [Gerenciar sessões do shell](#manage-sessions-from-the-shell) com `claude agents`, `claude attach` e comandos relacionados
* [Como as sessões em background são hospedadas](#how-background-sessions-are-hosted) pelo processo supervisor

<h2 id="quick-start">
  Início rápido
</h2>

Este passo a passo aborda o loop de visualização do agente principal: despache uma tarefa, observe sua linha ser atualizada conforme Claude trabalha, espreite para verificar e responda, e anexe para a conversa completa. A sessão que você despacha continua em execução após você fechar a visualização do agente, portanto você pode sair e voltar a ela.

<Steps>
  <Step title="Abrir visualização do agente">
    Do seu shell, execute:

    ```bash theme={null}
    claude agents
    ```

    A visualização do agente abre com uma entrada na parte inferior e uma tabela que se preenche conforme as sessões começam. Pressione `Esc` a qualquer momento para retornar ao seu shell. Suas sessões continuam em execução enquanto você está ausente e reaparecem na próxima vez que você abrir a visualização do agente.
  </Step>

  <Step title="Despache uma sessão">
    Digite um prompt descrevendo uma tarefa e pressione `Enter`. Uma nova sessão em background é iniciada nessa tarefa e aparece como uma linha mostrando se está funcionando, aguardando você ou concluída. A nova sessão usa o modelo mostrado no cabeçalho da visualização do agente e o mesmo [modo de permissão](#permission-mode-model-and-effort) que você obteria executando `claude` naquele diretório.

    Cada prompt que você digita aqui inicia sua própria sessão nova. Digitar outro prompt e pressionar `Enter` inicia uma segunda sessão ao lado da primeira em vez de enviar um acompanhamento para ela. Você pode executar várias em paralelo desta forma.

    Cada sessão usa sua cota de assinatura independentemente, portanto, consulte [Limitações](#limitations) antes de despachar muitas de uma vez.
  </Step>

  <Step title="Espreite e responda">
    Selecione uma linha com as teclas de seta e pressione `Space` para abrir o painel de espiada. Ele mostra a saída mais recente da sessão, ou a pergunta que está aguardando, em vez da transcrição completa. Digite uma resposta e pressione `Enter` para enviá-la sem sair da visualização do agente.
  </Step>

  <Step title="Anexar e desanexar">
    Pressione `Enter` ou `→` em uma linha para anexar quando quiser a conversa completa. A sessão assume o terminal como uma sessão interativa completa do Claude Code. Pressione `←` em um prompt vazio para desanexar e retornar à tabela.
  </Step>

  <Step title="Trazer uma sessão existente">
    Esta etapa precisa de uma sessão em execução. Se você seguiu as etapas anteriores, você não tem uma aberta neste terminal, portanto abra uma sessão regular `claude` em outro terminal e envie uma mensagem para ela primeiro. Para mover uma sessão que você já tem aberta para a visualização do agente, execute `/bg` dentro dela, ou pressione `←` em um prompt vazio para colocá-la em background e abrir a visualização do agente em uma etapa. A sessão continua em execução e aparece como uma linha ao lado das que você despachou.
  </Step>
</Steps>

Você pode usar `claude agents` como seu ponto de entrada principal em vez de `claude`: despache cada tarefa da visualização do agente, anexe quando quiser a conversa completa e pressione `←` para retornar à tabela.

Dentro de uma sessão regular `claude`, a dica `←` do rodapé do prompt conta os agentes em background que estão aguardando você, como `← 2 agents`, e retorna para `← for agents` quando nenhum precisa de entrada. Contagens acima de 99 aparecem como `99+`. A contagem é atualizada aproximadamente a cada dez segundos enquanto o terminal está em foco e imediatamente quando o foco retorna. Ela muda brevemente de cor quando se move e quando um agente é concluído, a menos que a configuração [`prefersReducedMotion`](https://code.claude.com/docs/pt/settings#available-settings) esteja ativada, e fica oculta no [modo leitor de tela](https://code.claude.com/docs/pt/accessibility). No [Amazon Bedrock, Google Cloud's Agent Platform e Microsoft Foundry](https://code.claude.com/docs/pt/third-party-integrations), a dica permanece em sua forma simples `← for agents` sem a contagem. Requer Claude Code v2.1.205 ou posterior.

<h2 id="monitor-sessions-with-agent-view">
  Monitorar sessões com agent view
</h2>

Execute `claude agents` para abrir agent view. Ele assume o terminal completo e lista cada sessão agrupada por estado, com sessões fixadas e as que precisam de você no topo. Cada linha mostra o nome da sessão, atividade atual e sua idade, contada a partir de quando a sessão foi criada; a idade de uma sessão concluída congela em quanto tempo a execução levou.

O nome é tingido com a cor definida por [`/color`](https://code.claude.com/docs/pt/commands) naquela sessão. A partir da v2.1.199, a cor é mantida quando você [coloca uma sessão em background](#from-inside-a-session) com `←` ou `/background`.

Por padrão, a lista mostra cada sessão em background que você iniciou, em todos os seus projetos. Uma sessão funcionando em um repositório e outra em um worktree diferente aparecem aqui, independentemente de qual diretório você abriu agent view. Para limitar a lista a um projeto, passe `--cwd`:

```bash theme={null}
claude agents --cwd ~/projects/my-app
```

Isso mostra apenas sessões iniciadas sob esse diretório. Uma sessão que [se moveu para um worktree](#how-file-edits-are-isolated) sob `~/projects/my-app/.claude/worktrees/` ainda conta como pertencente a `~/projects/my-app`.

Sessões interativas que você tem abertas em outros terminais não aparecem até que você as [coloque em background](#from-inside-a-session). [Subagents](https://code.claude.com/docs/pt/sub-agents) e [teammates](https://code.claude.com/docs/pt/agent-teams) que uma sessão gera não são listados como linhas separadas.

```text theme={null}
Pinned
  ✽ clawd walk cycle          Drawing the walk-cycle sprite frames          3m

Ready for review
  ∙ jump physics              Opened PR with collision fix                 #2048  2h

Needs input
  ✻ power-up design           double jump or wall climb?                    1m

Working
  ✽ collision detection       Adding swept-AABB checks to CollisionSystem   2m
  ✢ playtest level 3          run 12 · all checkpoints cleared           in 4m

Completed
  ✻ title screen              result: menu, options, and credits done       9m
  ∙ sound effects             result: 14 SFX exported to assets/audio       4h
  … 6 more
```

<h3 id="read-session-state">
  Ler estado da sessão
</h3>

Cada linha começa com um ícone cuja cor e animação mostram o estado da sessão:

| Estado      | Ícone mostra como | O que significa                                                                |
| :---------- | :---------------- | :----------------------------------------------------------------------------- |
| Working     | Animado           | Claude está executando ativamente ferramentas ou gerando uma resposta          |
| Needs input | Amarelo           | Claude está aguardando uma pergunta específica ou decisão de permissão de você |
| Idle        | Esmaecido         | A sessão não tem nada a fazer e está pronta para seu próximo prompt            |
| Completed   | Verde             | A tarefa foi concluída com sucesso                                             |
| Failed      | Vermelho          | A tarefa terminou com um erro                                                  |
| Stopped     | Cinza             | A sessão foi interrompida com `Ctrl+X` ou `claude stop`                        |

Separadamente, a forma do ícone mostra se o processo subjacente está em execução:

| Forma              | O que significa                                                                                                                       |
| :----------------- | :------------------------------------------------------------------------------------------------------------------------------------ |
| `✻` ou `✽` animado | O processo da sessão está vivo e responde imediatamente                                                                               |
| `∙`                | O processo saiu. Você ainda pode espreitar, responder ou anexar, e Claude reinicia de onde parou                                      |
| `✢`                | Uma sessão [`/loop`](https://code.claude.com/docs/pt/scheduled-tasks) dormindo entre iterações. A linha mostra sua contagem de execução e uma contagem regressiva |

O rótulo `#N` que pode aparecer na borda direita de uma linha é um [pull request que a sessão está vinculada a](#pull-request-status), não parte do ícone de estado.

O título da aba do terminal mostra a contagem de aguardando-entrada enquanto agent view está aberto: `2 awaiting input · claude agents` quando sessões precisam de entrada, ou `claude agents` quando nenhuma precisa.

A partir da v2.1.198, enquanto agent view está aberto, Claude Code também envia uma notificação através do seu [canal de notificação de terminal](https://code.claude.com/docs/pt/terminal-config#get-a-terminal-bell-or-notification) configurado quando uma sessão em background local começa a precisar de sua entrada, termina ou falha. Sessões que executam em um cronograma, como sessões [`/loop`](https://code.claude.com/docs/pt/scheduled-tasks), notificam apenas quando precisam de sua entrada. As notificações usam a mesma configuração [`preferredNotifChannel`](https://code.claude.com/docs/pt/settings#available-settings) que o resto do Claude Code e disparam o hook [`Notification`](https://code.claude.com/docs/pt/hooks#notification) com o tipo `agent_needs_input` ou `agent_completed`.

Sessões em background não precisam de nenhum terminal aberto para continuar funcionando. Um [processo supervisor](#the-supervisor-process) separado as executa, então você pode fechar agent view, fechar seu shell ou iniciar uma nova sessão interativa e seu trabalho despachado continua.

O estado da sessão persiste no disco através de atualizações automáticas e reinicializações do supervisor. As sessões também são preservadas quando sua máquina dorme. Seus processos retomam ao acordar e o supervisor se reconecta a eles em vez de tratar a lacuna de tempo como inatividade. Desligar ainda interrompe as sessões em execução; veja [Sessions show as failed after shutdown](#sessions-show-as-failed-after-shutdown) para saber como recuperá-las.

Quando você abre uma sessão que parou de responder, o supervisor reinicia seu processo e a sessão continua a resposta interrompida de onde parou. Uma sessão pode acabar nesse estado quando a máquina dorme enquanto está no meio de uma resposta. Requer Claude Code v2.1.200 ou posterior.

<h3 id="row-summaries">
  Resumos de linha
</h3>

O resumo de uma linha em cada linha é gerado por um [modelo Haiku-class](https://code.claude.com/docs/pt/model-config) para que a linha possa informar o que a sessão está fazendo, o que precisa ou o que produziu sem abrir o transcript. Enquanto uma sessão está ativamente funcionando, o texto da linha é atualizado no máximo uma vez a cada 15 segundos a partir da saída recente da própria sessão sem enviar uma solicitação de modelo, e o modelo escreve um resumo novo quando cada turno termina.

Uma linha funcionando mostra o que a sessão diz que está fazendo, e uma linha bloqueada mostra a pergunta que está fazendo. Durante um turno longo, o modelo também reescreve o resumo aproximadamente uma vez por minuto, aguardando o dobro do tempo após cada reescrita até quatro minutos, para que uma linha ocupada não continue mostrando um resumo desatualizado. Antes da v2.1.205, uma linha funcionando poderia mostrar uma invocação de ferramenta bruta em vez de um relatório, e uma sessão executando itens de trabalho paralelos mostrava uma contagem `done/total` como `2/5` antes do texto.

O texto de resumo preenche a largura restante da linha e é truncado apenas na borda direita do terminal; abra o [painel de espiada](#peek-and-reply) para ler uma sentença que a borda corta. Antes da v2.1.206, o texto era cortado em 64 colunas independentemente da largura do terminal.

Quando a lista é [agrupada por diretório](#organize-the-list), o resumo abre com o estado da sessão como uma palavra colorida, como `Needs input · double jump or wall climb?`. No agrupamento de estado padrão, o cabeçalho do grupo já nomeia o estado, então a linha mostra apenas o resumo. Antes da v2.1.205, linhas agrupadas por diretório não tinham palavra de estado.

Um turno cuja saída inteira não contém letras ou dígitos, como uma sessão [`/loop`](https://code.claude.com/docs/pt/scheduled-tasks) que imprime um símbolo solitário em uma iteração silenciosa, mantém o resumo e estado anteriores da linha. Antes da v2.1.205, esse turno era reclassificado e poderia virar uma sessão que estava aguardando sua entrada de volta para `Working`.

O resumo de fim de turno e cada reescrita de meio de turno são uma solicitação curta de Haiku-class através de seu provedor normal, cobrada e tratada sob os mesmos [termos de uso de dados](https://code.claude.com/docs/pt/data-usage) que a sessão em si. As atualizações de 15 segundos entre reescritas de modelo reutilizam a saída da própria sessão e não enviam uma solicitação. Em provedores de terceiros como Amazon Bedrock, Google Cloud's Agent Platform, Microsoft Foundry e gateways personalizados, a solicitação volta para o modelo principal da sessão quando nenhum modelo Haiku está configurado. Defina [`ANTHROPIC_DEFAULT_HAIKU_MODEL`](https://code.claude.com/docs/pt/model-config#environment-variables) para escolher o modelo para esses resumos nesses provedores.

<h3 id="pull-request-status">
  Status de pull request
</h3>

Quando uma sessão abre um pull request, um rótulo `#1234` aparece na borda direita da linha, vinculado ao pull request em terminais que suportam hiperlinks. O rótulo persiste quando você envia um acompanhamento para a sessão, então o pull request permanece visível enquanto a linha reverte para progresso ao vivo. Sessões em background que isolaram suas alterações em um worktree abrem esses pull requests por conta própria; [How file edits are isolated](#how-file-edits-are-isolated) cobre quando isso acontece e o que uma sessão nunca faz sem perguntar.

Uma sessão que trabalha em um pull request existente está vinculada a ele da mesma forma. Editar, comentar, fechar ou marcar um pull request como pronto com `gh` vincula o pull request que a saída do próprio comando nomeia, então um comando `gh` cuja saída capturada não nomeia nenhum pull request não cria um link; `gh pr merge` é o caso comum, porque imprime seu resultado apenas para um terminal interativo. Verificar um pull request com `gh pr checkout`, ou fazer push para um branch que tem um pull request aberto, vincula-o procurando esse branch com `gh pr view` em vez disso. Antes da v2.1.205, apenas pull requests que a sessão criou ou verificou foram vinculados, e um push vinculou um apenas quando o nome do branch local correspondia.

Claude Code lê o pull request da saída completa do comando, incluindo a parte salva em um arquivo quando a saída de um comando excede o limite inline. Antes da v2.1.205, um pull request criado em uma chamada Bash cuja saída excedia cerca de 30.000 caracteres não era vinculado.

Quando uma sessão está vinculada a mais de um pull request, o rótulo mostra uma contagem em vez disso, como `3 PRs`, colorido pelo pull request aberto que mais precisa de atenção. Abra o [painel de espiada](#peek-and-reply) para ver todos eles.

O número do pull request é colorido pelo seu status:

| Cor     | Status do pull request                                       |
| :------ | :----------------------------------------------------------- |
| Amarelo | Aguardando verificações ou revisão, ou verificações falharam |
| Verde   | Verificações passaram e nenhuma revisão está bloqueando      |
| Roxo    | Mesclado                                                     |
| Cinza   | Rascunho ou fechado                                          |

Para a maioria das tarefas, esta coluna é onde você coleta o resultado: revise e mescle o pull request quando seu número ficar verde.

<h3 id="peek-and-reply">
  Peek and reply
</h3>

Pressione `Space` em uma linha selecionada para abrir o painel de espiada. Ele abre com a sentença que a linha trunca na borda do terminal, e qual sentença é depende do estado da sessão:

* Uma sessão que está aguardando você: a pergunta exata que está fazendo, acima da entrada de resposta
* Uma sessão concluída: seu resultado
* Uma sessão funcionando: sua sentença de status completa

Quaisquer pull requests vinculados à sessão são listados em seguida. Para uma sessão que está aguardando você, uma linha como `waiting 3m` abaixo deles mostra há quanto tempo está aguardando, e é a única hora mostrada no painel. A idade na borda direita da linha é um número diferente: ela conta a partir de quando a sessão começou.

Na maioria das vezes, o painel de espiada é suficiente e você não precisa abrir o transcript completo.

Antes da v2.1.207, cada espiada abria com a sentença de status e um timestamp simples, e uma sessão bloqueada tinha sua pergunta aparecendo abaixo deles prefixada com o mesmo timestamp uma segunda vez.

Digite uma resposta no painel de espiada e pressione `Enter` para enviá-la para essa sessão. Quando a sessão está fazendo uma pergunta de múltipla escolha, o painel de espiada mostra as opções e você pode pressionar uma tecla numérica para escolher uma. Para outras sessões bloqueadas, pressione `Tab` para preencher a entrada com uma resposta sugerida que você pode editar antes de enviar. Prefixe uma resposta com `!` para enviar um comando Bash em vez disso.

Uma resposta que não pode ser entregue, porque o serviço em background está inacessível ou o envio falha, é salva e enviada para a sessão como seu próximo prompt quando seu processo começar novamente, e a mensagem de erro diz que a resposta foi salva. Uma resposta prefixada com `!` não é salva, porque o texto salvo chegaria à sessão como um prompt simples em vez de executar como um comando Bash.

Com [voice dictation](https://code.claude.com/docs/pt/voice-dictation) ativada, segure ou toque sua tecla push-to-talk enquanto a entrada de resposta está focada para ditar uma resposta em vez de digitá-la. O mesmo funciona na entrada de despacho na parte inferior de agent view.

Use `↑` e `↓` para espreitar sessões adjacentes sem fechar o painel, ou `→` para anexar.

<h3 id="attach-to-a-session">
  Anexar a uma sessão
</h3>

Pressione `Enter` ou `→` em uma linha selecionada para anexar. Agent view é substituído pela sessão interativa completa. Quando você anexa, Claude publica um breve resumo do que aconteceu enquanto você estava ausente.

Enquanto anexado, a sessão se comporta como qualquer outra sessão Claude Code: [comandos](https://code.claude.com/docs/pt/commands), atalhos de teclado e recursos todos funcionam, com as exceções abaixo.

Uma sessão em background recusa `/install-github-app` e a lista de configurações [`/mcp`](https://code.claude.com/docs/pt/mcp), incluindo suas ações de autenticação, se você está anexado ou respondendo do painel de espiada. A mensagem o direciona para uma sessão `claude` regular, e `/mcp reconnect <server>`, `/mcp enable` e `/mcp disable` ainda funcionam.

Sessões anexadas sempre renderizam em [modo fullscreen](https://code.claude.com/docs/pt/fullscreen), independentemente de sua configuração `tui`, porque uma sessão em background não tem scrollback de terminal para anexar. Role com `PgUp`, `PgDn` ou a roda do mouse, e pressione `Ctrl+O` para modo de transcript. O scroll nativo do seu terminal e o modo de cópia tmux mostram apenas o viewport atual, o mesmo que quando você executa qualquer aplicativo fullscreen.

Pressione `←` em um prompt vazio, ou execute `/exit`, para desanexar e retornar a agent view. A partir da v2.1.198, isso funciona da mesma forma se você abriu a sessão a partir de agent view ou com `claude attach <id>` a partir do seu shell.

`Ctrl+Z` também desanexa, mas volta para onde você começou: agent view se você anexou de lá, ou seu shell se você executou `claude attach`. Use `Ctrl+Z` quando um diálogo tem foco e não está respondendo a `←`.

`Ctrl+C` mantém seu comportamento de interrupção padrão enquanto anexado: ele cancela uma resposta em execução ou comando shell `!` em vez de desanexar. Pressionar `Ctrl+C` duas vezes em um prompt vazio desanexa, o mesmo que em qualquer sessão.

Desanexar nunca interrompe uma sessão em background: `←`, `Ctrl+Z`, `/exit` e duplo `Ctrl+C` ou duplo `Ctrl+D` a deixam em execução. Para encerrar uma sessão de dentro dela, execute `/stop`.

Em uma sessão em execução em primeiro plano, uma que você iniciou no terminal em vez de anexar a partir de agent view, pressionar `←` em um prompt vazio a coloca em background e abre agent view com essa linha selecionada, para que você possa alternar sessões sem sair do terminal. O mesmo pressionamento único desanexa uma sessão anexada.

Se uma ferramenta está em execução quando você pressiona `←`, Claude Code aguarda até cerca de dez segundos para que ela termine antes de colocar em background, e a resposta continua na sessão em background. Pressione `←` novamente para colocar em background imediatamente em vez de aguardar. Quando o trabalho em andamento não pode ser transferido para a sessão em background, o diálogo `Background this session?` aparece primeiro, o mesmo que com [`/background`](#from-inside-a-session).

O limite de dez segundos não se aplica enquanto [subagents](https://code.claude.com/docs/pt/sub-agents) estão em execução. Claude Code continua aguardando para que seu trabalho seja transferido, e mostra um aviso `Still backgrounding after the current tool` enquanto aguarda; pressione `←` novamente para colocar em background sem aguardar, o que reinicia os subagents do início. Antes da v2.1.203, a espera terminava após dez segundos e os subagents em execução eram reiniciados do início sem aviso.

A linha é criada mesmo a partir de uma sessão nova sem histórico de conversa, então `→` retorna a ela. Antes da v2.1.203, agent view mostrava uma dica de integração abaixo dessa linha quando era a única.

Você pode desativar este atalho com a configuração `leftArrowOpensAgents` em `/config`.

<h3 id="organize-the-list">
  Organizar a lista
</h3>

Agent view agrupa sessões para que as que precisam de entrada estejam no topo, com `Ready for review` e `Needs input` acima de `Working` e `Completed`. Esses nomes de grupo não mapeiam um-para-um para os [estados](#read-session-state) acima: uma sessão se move para `Ready for review` quando tem um pull request aberto, e `Completed` coleta sessões concluídas, falhadas e interrompidas juntas.

Pressione `Ctrl+S` para agrupar por diretório em vez disso. Sua escolha persiste entre execuções.

Dentro de um grupo:

* Pressione `Ctrl+T` para fixar uma sessão no topo e [manter seu processo em execução](#the-supervisor-process) enquanto inativo
* Pressione `Shift+↑` ou `Shift+↓` para reordenar sessões
* Pressione `Ctrl+R` para renomear uma sessão
* Pressione `Enter` em um cabeçalho de grupo para recolhê-lo

Para remover uma sessão da lista, pressione `Ctrl+X` para interrompê-la e `Ctrl+X` novamente dentro de dois segundos para deletá-la. Pressionar `Ctrl+X` em um cabeçalho de grupo deleta cada sessão naquele grupo após confirmação.

Deletar remove a sessão de agent view. Se Claude [criou um worktree](#how-file-edits-are-isolated) para a sessão, deletar remove esse worktree também, incluindo quaisquer alterações não confirmadas nele, então faça commit do trabalho que você quer manter primeiro. Um worktree que você criou você mesmo e iniciou a sessão dentro é deixado no lugar. O transcript de conversa fica em sua máquina local e permanece disponível através de `claude --resume`.

Deletar nunca remove um worktree com commits que não foram feitos push em lugar nenhum, ou um que outra sessão em execução reclama ou tem bloqueado. Claude Code mantém o worktree e a sessão, e o rodapé nomeia o caminho mantido e o motivo. Faça push dos commits, ou feche a outra sessão, então delete novamente.

Deletar também limpa a sessão da [lista de sessões do supervisor](#the-supervisor-process), se você deletar com `Ctrl+X` ou com [`claude rm`](#manage-sessions-from-the-shell) a partir do shell, então a remoção persiste entre reinicializações do supervisor. Antes da v2.1.206, remover uma sessão enquanto o supervisor estava reiniciando ou inacessível a deixava naquela lista, e o próximo supervisor reiniciava seu processo e mostrava a linha novamente.

Sessões concluídas que não cabem na tela se dobram em uma linha `… N more`. Falhas e sessões com um pull request aberto sempre permanecem visíveis. O grupo `Completed` preenche o espaço vertical deixado após os grupos ativos, e em um terminal curto o cabeçalho se compacta para uma única linha de resumo para que sessões que estão funcionando ou precisam de entrada permaneçam visíveis.

<h3 id="filter-sessions">
  Filtrar sessões
</h3>

Digite na entrada de despacho para filtrar em vez de despachar:

| Filtro                       | Mostra                                                                                                      |
| :--------------------------- | :---------------------------------------------------------------------------------------------------------- |
| `a:<name>`                   | Sessões executando o agente nomeado                                                                         |
| `s:<state>`                  | Sessões no estado fornecido, como `s:working`. Também aceita `s:blocked` para tudo que está aguardando você |
| `#<number>` ou uma URL de PR | A sessão trabalhando naquele pull request                                                                   |
| Qualquer outra URL           | A sessão cujo primeiro prompt continha essa URL                                                             |

<h3 id="keyboard-shortcuts">
  Atalhos de teclado
</h3>

Pressione `?` em agent view para ver cada atalho em contexto. A tabela abaixo os resume.

| Atalho                | Ação                                                                                            |
| :-------------------- | :---------------------------------------------------------------------------------------------- |
| `↑` / `↓`             | Mover entre linhas                                                                              |
| `Enter`               | Anexar à sessão selecionada, ou despachar se houver texto na entrada                            |
| `Space`               | Abrir ou fechar o painel de espiada para a sessão selecionada                                   |
| `Shift+Enter`         | Despachar e anexar imediatamente                                                                |
| `→`                   | Anexar à sessão selecionada                                                                     |
| `Alt+1`..`Alt+9`      | Anexar à sessão 1–9 no diretório da sessão focada                                               |
| `Tab`                 | Em uma entrada vazia, procurar todos os subagents. Caso contrário, aplicar a sugestão destacada |
| `Ctrl+S`              | Alternar agrupamento entre estado e diretório                                                   |
| `Ctrl+T`              | Fixar ou desafixar a sessão selecionada                                                         |
| `Ctrl+R`              | Renomear a sessão selecionada                                                                   |
| `Ctrl+G`              | Abrir o prompt de despacho em seu `$VISUAL` ou `$EDITOR`                                        |
| `Ctrl+X`              | Interromper a sessão; pressione novamente dentro de dois segundos para deletá-la                |
| `Shift+↑` / `Shift+↓` | Reordenar a sessão selecionada                                                                  |
| `Esc`                 | Fechar o painel de espiada, limpar a entrada ou sair                                            |
| `Ctrl+C`              | Limpar a entrada; pressione duas vezes para sair                                                |
| `?`                   | Mostrar todos os atalhos                                                                        |

<h2 id="dispatch-new-agents">
  Despachar novos agentes
</h2>

Você pode despachar novas sessões em background a partir de agent view, enviar uma sessão interativa existente para o background ou iniciar uma diretamente do shell.

<h3 id="from-agent-view">
  From agent view
</h3>

Digite um prompt na entrada na parte inferior de agent view e pressione `Enter` para iniciar uma nova sessão em background. A sessão é nomeada automaticamente a partir do prompt; renomeie-a mais tarde com `Ctrl+R`.

Um nome que a sessão recebe mais tarde também aparece em sua linha, incluindo o nome que Claude deriva quando você [aceita um plano](https://code.claude.com/docs/pt/permission-modes#review-and-approve-a-plan) naquela sessão. Antes da v2.1.207, uma sessão em background nomeada ao aceitar um plano mostrava esse nome em `/status` mas não em sua linha de agent view até que você a renomeasse você mesmo.

Cole uma imagem no prompt para incluir uma captura de tela ou diagrama com a tarefa.

Texto colado mais longo que 800 caracteres ou mais de duas linhas se recolhe para um placeholder `[Pasted text #N]` para que a entrada permaneça em uma linha; o texto completo é enviado quando você despacha. Para revisar ou editar o texto recolhido antes de despachar, cole o mesmo texto novamente e o placeholder se expande de volta para a entrada. Um lembrete `paste again to expand` aparece abaixo da entrada por alguns segundos após a colagem em terminais com pelo menos 90 colunas de largura. Antes da v2.1.207, colar o mesmo texto novamente adicionava um segundo placeholder em vez de expandir o primeiro.

Prefixe ou mencione partes do prompt para controlar como a sessão é iniciada:

| Entrada                                | Efeito                                                                                                                                                                                      |
| :------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `<agent-name> <prompt>`                | Se a primeira palavra corresponder a um nome de [subagent](https://code.claude.com/docs/pt/sub-agents) personalizado, esse subagent é executado como o agente principal da sessão com a configuração de seu frontmatter |
| `@<agent-name>`                        | Mencione um subagent personalizado em qualquer lugar do prompt para executá-lo como o agente principal                                                                                      |
| `@<repo>`                              | Mencione um repositório para executar a sessão lá. Veja [Dispatch to a specific directory](#dispatch-to-a-specific-directory) para saber quais repositórios são listados                    |
| `/<command>`                           | Sugerir [skills](https://code.claude.com/docs/pt/skills) e [commands](https://code.claude.com/docs/pt/commands) para despachar como o prompt                                                                                                        |
| `! <command>`                          | Execute um comando shell como um trabalho em background em vez de iniciar uma sessão Claude. O trabalho aparece como uma linha que você pode anexar, observar e desanexar                   |
| `#<number>` ou uma URL de pull request | Se uma sessão já está trabalhando naquele PR, selecione-a em vez de despachar                                                                                                               |
| `Shift+Enter`                          | Despachar e anexar imediatamente à nova sessão                                                                                                                                              |

Um pequeno conjunto de comandos é executado em agent view em si em vez de ser despachado:

* `/exit` e `/quit` fecham agent view
* `/logout` desconecta você
* `/model` define o [modelo de despacho](#set-the-model)
* A partir da v2.1.198, `/login` abre o diálogo de entrada para que você possa entrar novamente sem anexar a uma sessão

Skills, seus próprios comandos e built-ins que expandem prompts como `/init` são enviados para uma nova sessão em background como seu primeiro prompt. Outros comandos built-in mostram uma dica `attach to a session to run it` em vez disso. Tudo que você digitou permanece na entrada ao lado da dica para que você possa editá-lo. Antes da v2.1.203, a dica limpava a entrada e o texto digitado era perdido.

Empacotar uma tarefa recorrente como uma [skill](https://code.claude.com/docs/pt/skills) permite que você inicie o mesmo fluxo de trabalho a partir de agent view repetidamente sem redigitar o prompt.

Quando o mesmo `@name` corresponde tanto a um subagent quanto a um repositório irmão, o subagent tem precedência. A correspondência de primeira palavra também se aplica, portanto um prompt que começa com um de seus nomes de subagent despacha esse subagent em vez de tratar a palavra como texto simples. Use a forma `@` quando quiser ser explícito, ou comece o prompt com uma palavra diferente para evitar a correspondência.

<h4 id="dispatch-to-a-specific-directory">
  Dispatch to a specific directory
</h4>

Uma nova sessão é executada no diretório em que você abriu agent view. Para direcionar um diretório diferente, use qualquer um destes:

* Abra `claude agents` naquele diretório.
* Abra `claude agents` em um diretório pai e mencione um repositório filho com `@<repo>` no prompt. Digitando `@` lista estes destinos:

  * Repositórios Git um nível abaixo do diretório de lançamento
  * Os [git worktrees](https://code.claude.com/docs/pt/worktrees) registrados do repositório a partir do qual você iniciou que vivem dentro de sua árvore de diretórios, como os que Claude cria sob `.claude/worktrees/`, rotulados com seu branch verificado. Worktrees adicionados fora do repositório, como com `git worktree add ../feature`, não são listados
  * Qualquer diretório que já tenha uma sessão na lista

  Um diretório cujo nome contém um espaço não é listado. Antes da v2.1.203, worktrees registrados não eram listados, portanto despachar para um significava executar `claude --bg` a partir do diretório daquele worktree.
* Do shell, `cd` para o diretório e execute `claude --bg "<prompt>"`.

Quando agent view é agrupado por diretório, o diretório da linha destacada se torna o alvo de despacho, para que você possa rolar para um grupo e despachar nele sem redigitar o caminho.

<h3 id="from-inside-a-session">
  From inside a session
</h3>

Execute `/background` ou seu alias `/bg` para mover a conversa atual para uma sessão em background. Passe um prompt como `/bg run the test suite and fix any failures` para dar uma instrução adicional primeiro. Se Claude estiver respondendo quando você executar `/bg`, a resposta continua na sessão em background.

Sair de uma sessão interativa que ainda tem trabalho em background em execução, como subagents, comandos shell em background, workflows ou [monitors](https://code.claude.com/docs/pt/tools-reference#monitor-tool), mostra um diálogo `Background work is running` em vez de sair imediatamente. A partir da v2.1.198, o diálogo oferece `Move to background and exit` junto com `Exit anyway` e `Stay`. Escolhê-lo move a sessão para o background da mesma forma que `/background` faz, depois retorna você ao seu shell, para que o trabalho que pode ser transferido continue em execução e a sessão apareça em agent view. A opção não é mostrada quando agent view está [desativado](#turn-off-agent-view).

Colocar em background a partir de uma sessão interativa inicia um novo processo que retoma da conversa salva, e o trabalho em andamento se move para ele: comandos shell em background em execução, subagents em background, workflows dinâmicos e tarefas agendadas que você criou com [`/loop`](https://code.claude.com/docs/pt/scheduled-tasks) são transferidos para a sessão em background e continuam em execução lá. Um subagent se move junto com tudo que iniciou, portanto é transferido apenas quando todo esse trabalho pode se mover também, incluindo no Windows. Para parar o trabalho em andamento em vez de transferi-lo, defina a variável de ambiente [`CLAUDE_DISABLE_ADOPT=1`](https://code.claude.com/docs/pt/env-vars#variables); Claude Code então pede que você confirme antes de colocar em background.

Trabalho que não pode ser transferido, como um [monitor](https://code.claude.com/docs/pt/tools-reference#monitor-tool) em execução, é interrompido. Um subagent em background que possui um monitor é interrompido junto com ele. Quando algum desse trabalho está em execução, Claude Code mostra um diálogo `Background this session?` para que você possa confirmar antes de ser interrompido.

Uma vez em background, a sessão pode iniciar novos subagents, monitors e comandos em background, e esses continuam em execução em desanexações e reanexações posteriores.

As flags de configuração do lançamento original são transferidas para a sessão colocada em background, portanto seus servidores MCP, settings e modelo de fallback permanecem em vigor:

* `--mcp-config` e `--strict-mcp-config`
* `--settings`
* `--add-dir`
* `--plugin-dir`
* `--fallback-model`
* `--allow-dangerously-skip-permissions`

Diretórios que você adicionou durante a sessão com [`/add-dir`](https://code.claude.com/docs/pt/permissions#additional-directories-grant-file-access-not-configuration) também são transferidos.

Transferir `--allow-dangerously-skip-permissions` mantém `bypassPermissions` acessível na sessão colocada em background, mas não concede nada novo. O modo ainda requer a mesma aceitação interativa única descrita em [Permission mode, model, and effort](#permission-mode-model-and-effort) antes que qualquer sessão possa usá-lo.

<h3 id="from-your-shell">
  From your shell
</h3>

Passe `--bg` ou sua forma longa `--background` para iniciar uma sessão que vai direto para o background:

```bash theme={null}
claude --bg "investigate the flaky SettingsChangeDetector test"
```

O prompt é o argumento posicional, não um valor `-p`. A partir da v2.1.198, combinar `--bg` com `-p` ou `--print` é rejeitado com um erro antes de qualquer sessão ser criada, porque `--print` nunca inicia a sessão interativa à qual `claude agents` se anexa.

Para executar um subagent específico como o agente principal da sessão, combine `--bg` com `--agent`:

```bash theme={null}
claude --agent code-reviewer --bg "address review comments on PR 1234"
```

Passe `--name` para definir o nome de exibição da sessão em agent view em vez do gerado automaticamente:

```bash theme={null}
claude --bg --name "flaky-test-fix" "investigate the flaky SettingsChangeDetector test"
```

Após colocar em background, Claude imprime o ID curto da sessão e os comandos para gerenciá-la. Quando o serviço que hospeda sessões em background não está já em execução, `--bg` pode primeiro imprimir `Starting background service…` acima desta saída. Quando você passa `--name`, o nome aparece após o ID curto:

```text theme={null}
backgrounded · 7c5dcf5d · flaky-test-fix
  claude agents             list sessions
  claude attach 7c5dcf5d    open in this terminal
  claude logs 7c5dcf5d      show recent output
  claude stop 7c5dcf5d      stop this session
```

<h4 id="run-a-shell-command">
  Run a shell command
</h4>

Para executar um comando shell como um trabalho em background em vez de uma sessão Claude, digite `!` como o primeiro caractere da entrada de despacho de agent view. O `!` aparece como um prefixo e tudo que você digita após ele é o comando. O exemplo a seguir despacha `pytest -x` a partir da caixa de entrada de agent view:

```text theme={null}
! pytest -x
```

Pressione `Enter` para iniciar o trabalho. O mesmo trabalho também pode ser lançado diretamente do seu shell com `--exec`:

```bash theme={null}
claude --bg --exec 'pytest -x'
```

O comando é executado como um trabalho com suporte PTY e aparece como uma linha em agent view, com a linha de saída mais recente como seu status. Um trabalho shell executa o comando no lugar de Claude, portanto nenhum modelo é invocado e a saída não é enviada para nenhuma sessão.

Para ver a saída, anexe à linha, pressione `Space` para espreitar sem anexar, ou execute `claude logs <id>` do seu shell. A saída capturada permanece na memória e não é escrita em disco. A linha e sua saída são limpas automaticamente cerca de cinco minutos após o comando sair, portanto leia-a antes disso se precisar do resultado.

<h3 id="how-file-edits-are-isolated">
  How file edits are isolated
</h3>

Toda sessão em background, seja iniciada a partir de agent view, `/bg` ou `claude --bg`, inicia no seu diretório de trabalho. Antes de editar arquivos, Claude move a sessão para um [git worktree](https://code.claude.com/docs/pt/worktrees) isolado sob `.claude/worktrees/`, para que sessões paralelas possam ler o mesmo checkout, mas cada uma escreve no seu próprio.

Claude pula o worktree quando:

* A sessão já está dentro de um git worktree vinculado, seja Claude o criou sob `.claude/worktrees/` ou você o criou com `git worktree add` em outro lugar
* O diretório de trabalho não é um repositório git e nenhum hook [`WorktreeCreate`](https://code.claude.com/docs/pt/hooks#worktreecreate) está configurado
* A escrita está fora do diretório de trabalho

Para desativar o isolamento de worktree para um repositório onde git worktrees são impraticáveis, defina [`worktree.bgIsolation`](https://code.claude.com/docs/pt/settings#worktree-settings) como `"none"`. As sessões em background editam sua cópia de trabalho diretamente sem se mover para um worktree primeiro. Adicione a configuração ao `.claude/settings.json` do projeto:

```json theme={null}
{
  "worktree": {
    "bgIsolation": "none"
  }
}
```

Fora de um repositório git, as sessões escrevem no diretório de trabalho diretamente e não são isoladas uma da outra, portanto evite despachar sessões paralelas que editam os mesmos arquivos. Se você usar um sistema de controle de versão diferente, configure um hook [`WorktreeCreate`](https://code.claude.com/docs/pt/worktrees#non-git-version-control) e Claude isola edições da mesma forma que faz para git.

Quando o hook falha em um diretório que não é um repositório git, a sessão pula o isolamento para aquele diretório e edita o diretório de trabalho no local. Dentro de um repositório git, as escritas permanecem bloqueadas até que a sessão se isole. Antes da v2.1.203, uma sessão em background naquele estado não podia editar nenhum arquivo: toda escrita era rejeitada até que se isolasse, e o hook nunca podia isolar aquele diretório.

Deletar uma sessão remove ou mantém o worktree que Claude criou para ela, dependendo de como você a deleta e o que o worktree contém:

* Deletar em agent view com `Ctrl+X` duas vezes remove o worktree, incluindo quaisquer alterações não confirmadas, portanto confirme as alterações que você quer manter primeiro.
* Deletar do shell com [`claude rm`](#manage-sessions-from-the-shell) mantém um worktree que tem alterações não confirmadas, junto com sua linha de sessão.
* Nenhum caminho remove um worktree com commits que não são enviados em nenhum lugar: o worktree é [mantido junto com sua sessão](#organize-the-list) e a saída nomeia o caminho mantido e o motivo.
* Um worktree que você criou você mesmo e iniciou a sessão dentro é deixado no lugar de qualquer forma.

Para encontrar o caminho do worktree de uma sessão, espreite a sessão ou anexe e verifique seu diretório de trabalho.

Um [subagent](https://code.claude.com/docs/pt/sub-agents) que a sessão em background gera herda o diretório de trabalho da sessão, portanto suas edições de arquivo chegam ao worktree da sessão em vez de sua cópia de trabalho. Para dar a um subagent seu próprio worktree separado, defina [`isolation: worktree`](https://code.claude.com/docs/pt/sub-agents#supported-frontmatter-fields) em seu frontmatter ou passe `isolation: "worktree"` ao gerá-lo.

A partir da v2.1.198, uma sessão em background que isolou suas alterações de código em um worktree também confirma, envia seu próprio branch e abre um pull request de rascunho sem parar para perguntar. O rótulo [`#N`](#pull-request-status) aparece em sua linha quando o pull request é aberto. Nunca envia para `main` ou `master`, nunca força-envia ou mescla, e pula o pull request quando você disse para não abrir um ou o repositório não tem um remoto.

Uma sessão editando um checkout que não isolou a si mesma ainda pergunta antes de confirmar ou alternar branches. Isso se aplica quando o isolamento é definido como `"none"`, quando a movimentação do worktree falhou, ou quando a sessão foi iniciada dentro de um worktree que já existia.

<h3 id="set-the-model">
  Set the model
</h3>

O nome do modelo mostrado no cabeçalho de agent view é o padrão de despacho. Novas sessões que você inicia a partir da entrada usam este modelo, que vem da configuração [`model`](https://code.claude.com/docs/pt/settings#available-settings) em suas settings de usuário. Defina-o selecionando um modelo no seletor [`/model`](https://code.claude.com/docs/pt/model-config), ou edite a configuração diretamente.

Para substituir o padrão de despacho para toda a sessão de agent view, passe `--model` ao abrir agent view. Veja [Permission mode, model, and effort](#permission-mode-model-and-effort).

Para alterar o padrão de despacho de dentro de agent view, digite `/model` seguido de um nome de modelo na entrada de despacho e pressione `Enter`. O cabeçalho é atualizado para mostrar esse modelo com um marcador `(session)`, e as sessões que você despacha depois usam-no. Digite `/model default` para limpar a substituição e retornar ao padrão de despacho. Essa substituição dura o resto da execução atual de `claude agents` e não escreve no seu arquivo de settings. O exemplo a seguir despacha uma sessão em Opus e a próxima em Sonnet:

```text theme={null}
/model opus
refactor auth
/model sonnet
run the test suite
```

Cada sessão em background pode ser executada em um modelo diferente. Para substituir para uma sessão:

* Do shell, passe `--model` com `claude --bg`.
* Anexe a uma sessão em execução e execute `/model` para alternar: uma escolha do seletor, ou um `/model <name>` digitado, salva como seu padrão para novas sessões a menos que você pressione `s` no seletor para uma alternância apenas de sessão. Uma alternância apenas de sessão persiste se a sessão for reiniciada.
* Despache um [subagent](https://code.claude.com/docs/pt/sub-agents) cujo frontmatter define um campo `model`.

<h3 id="permission-mode-model-and-effort">
  Permission mode, model, and effort
</h3>

Uma sessão em background lê suas [settings](https://code.claude.com/docs/pt/settings) do diretório em que é executada, da mesma forma que se você tivesse iniciado `claude` lá. Isso inclui valores [`env`](https://code.claude.com/docs/pt/settings#available-settings) em settings de projeto, portanto uma `ANTHROPIC_MODEL` ou variável de provedor definida lá se aplica a sessões em background naquele diretório.

A seleção de provedor de nuvem, como `CLAUDE_CODE_USE_BEDROCK` ou `CLAUDE_CODE_USE_VERTEX`, e aliases `ANTHROPIC_DEFAULT_*_MODEL` seguem o shell que despachou a sessão. Se você exportar uma substituição de corpo de solicitação [`CLAUDE_CODE_EXTRA_BODY`](https://code.claude.com/docs/pt/env-vars) naquele shell, ela alcança a sessão da mesma forma. Antes da v2.1.206, workers em background ignoravam um `CLAUDE_CODE_EXTRA_BODY` exportado pelo shell.

Se você exportar um gateway `ANTHROPIC_BASE_URL` no shell de despacho, ele alcança a sessão também, junto com `ANTHROPIC_CUSTOM_HEADERS`, quando o supervisor é executado com o mesmo ambiente de gateway e a sessão é executada no diretório a partir do qual você despacha ou é sua própria sessão colocada em background com `←` ou `/background`. Esse é o caso normal quando o primeiro shell a abrir agent view ou despachar uma sessão em background é o shell de gateway. Despachar para um diretório diferente com `@repo` ou `--cwd` não carrega o gateway do shell; as [settings](https://code.claude.com/docs/pt/settings) daquele projeto fornecem o endpoint. Veja [o processo supervisor](#the-supervisor-process) para como sessões em background obtêm configurações de provedor e credenciais.

O [permission mode](https://code.claude.com/docs/pt/permissions) depende de como você iniciou a sessão. Colocar em background uma sessão existente com `/bg` ou `←` mantém o permission mode atual, portanto uma sessão que você alterou para `acceptEdits` ou `auto` permanece naquele modo após desanexar. Despachar a partir da entrada de agent view ou executar `claude --bg` do seu shell usa o `defaultMode` das settings daquele diretório, ou o `permissionMode` do [frontmatter do subagent despachado](https://code.claude.com/docs/pt/sub-agents#supported-frontmatter-fields).

O permission mode, modelo e esforço com os quais uma sessão em background foi iniciada, juntamente com os [flags de configuração que ela carrega](#from-inside-a-session), todos persistem quando o supervisor posteriormente [para e reinicia](#the-supervisor-process) seu processo. Uma sessão que você lançou com `claude --bg --dangerously-skip-permissions` ou `claude --bg --permission-mode bypassPermissions` permanece em `bypassPermissions` após esse reinício em vez de voltar ao `defaultMode` do diretório, e um modelo ou esforço que você alterou no meio da sessão com `/model` ou `/effort` é mantido.

Um esforço que a sessão obteve da configuração [`effortLevel`](https://code.claude.com/docs/pt/settings#available-settings) em vez de `--effort` ou `/effort` não é fixado no despacho: cada processo iniciado para a sessão lê a configuração novamente, portanto editar `effortLevel` em `settings.json` alcança sessões que você coloca em background com `←` ou `/bg` e seus reinícios posteriores. Antes da v2.1.203, colocar uma sessão em background registrava seu esforço derivado de settings como se você tivesse passado `--effort`, portanto edições posteriores de `effortLevel` nunca o alcançavam.

Um nome que você definiu com [`/rename`](https://code.claude.com/docs/pt/commands) ou `Ctrl+R` também persiste nesse reinício, portanto [`claude --resume <name>`](https://code.claude.com/docs/pt/sessions#name-your-sessions) ainda resolve a sessão. Antes da v2.1.202, o reinício revertia a sessão para o nome com o qual foi despachada e o novo nome parava de resolver.

Para definir padrões para cada sessão que você despacha a partir de agent view, passe qualquer um de `--permission-mode`, `--model`, `--effort` ou `--agent` ao abri-lo:

```bash theme={null}
claude agents --permission-mode plan --model opus --effort high
```

`--agent` define o [subagent](https://code.claude.com/docs/pt/sub-agents) usado quando um prompt de despacho não nomeia um, seja com `@name` ou como a primeira palavra. O padrão é a configuração [`agent`](https://code.claude.com/docs/pt/settings#available-settings) se uma estiver definida, caso contrário o agente `claude` integrado catch-all. Nomear um subagent na entrada de despacho substitui ambos.

`claude agents` também aceita `--dangerously-skip-permissions` como abreviação para `--permission-mode bypassPermissions`, e `--allow-dangerously-skip-permissions` para tornar `bypassPermissions` disponível no ciclo `Shift+Tab` de cada sessão despachada sem iniciar naquele modo. Ambos correspondem aos [flags CLI de nível superior](https://code.claude.com/docs/pt/cli-reference).

Os padrões ativos aparecem no rodapé abaixo da entrada de despacho.

Sem essas flags, a sessão usa o `defaultMode` das settings daquele diretório ou o `permissionMode` do [frontmatter do subagent despachado](https://code.claude.com/docs/pt/sub-agents#supported-frontmatter-fields), e o modelo mostrado no cabeçalho de agent view.

Usar `bypassPermissions` com `claude --bg --permission-mode` é recusado até que você tenha aceitado o aviso de bypass executando `claude --dangerously-skip-permissions` uma vez interativamente, já que esse modo permite que uma sessão que você não está observando aja sem aprovação. Passar `--dangerously-skip-permissions` ou `--permission-mode bypassPermissions` para `claude agents` mostra o mesmo aviso quando você não o aceitou antes, e aceitar aplica `bypassPermissions` às sessões que você inicia a partir da visualização. Passar `--allow-dangerously-skip-permissions` mostra o mesmo aviso também, e aceitar torna `bypassPermissions` disponível no ciclo `Shift+Tab` dessas sessões sem iniciá-las nele.

<h3 id="settings-plugins-and-mcp-servers">
  Settings, plugins, and MCP servers
</h3>

Agent view aceita os mesmos flags de configuração que `claude` para carregar settings, plugins, servidores MCP e diretórios adicionais. Cada flag se aplica a agent view em si e é passado para cada sessão que você despacha a partir dele, portanto um plugin ou servidor MCP que você carrega desta forma está disponível nessas sessões também.

| Flag                                                                                             | Efeito                                                                            |
| :----------------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------- |
| [`--settings <file-or-json>`](https://code.claude.com/docs/pt/settings)                                                      | Substituir settings para agent view e sessões despachadas                         |
| [`--add-dir <path>`](https://code.claude.com/docs/pt/permissions#additional-directories-grant-file-access-not-configuration) | Conceder acesso a arquivo a um diretório adicional                                |
| [`--plugin-dir <path>`](https://code.claude.com/docs/pt/plugins)                                                             | Carregar um plugin de um diretório local                                          |
| [`--mcp-config <file-or-json>`](https://code.claude.com/docs/pt/mcp)                                                         | Carregar servidores MCP de um arquivo de configuração ou string JSON              |
| `--strict-mcp-config`                                                                            | Usar apenas os servidores MCP de `--mcp-config`, ignorando outra configuração MCP |

Repita `--add-dir`, `--plugin-dir` ou `--mcp-config` uma vez por valor. A forma separada por espaço, como `--add-dir a b c`, não é suportada com `claude agents`.

O exemplo a seguir abre agent view com uma substituição de settings e um diretório extra:

```bash theme={null}
claude agents --settings ./ci-settings.json --add-dir ../shared-lib
```

<h2 id="manage-sessions-from-the-shell">
  Gerenciar sessões do shell
</h2>

Cada sessão em background tem um ID curto que você pode usar do shell. O ID é impresso quando você inicia uma sessão com `claude --bg`, e o ID de cada sessão é seu nome de diretório em `~/.claude/jobs/`. Esses comandos são úteis para scripts ou quando você não quer abrir agent view.

| Comando                      | Propósito                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| :--------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `claude agents`              | Abrir agent view                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `claude agents --cwd <path>` | Abrir agent view com escopo para sessões iniciadas em `<path>`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `claude agents --json`       | Imprimir sessões ativas como um array JSON e sair: cada sessão ativa, mais sessões em background que ainda estão funcionando ou bloqueadas mesmo quando seu processo saiu. Adicione `--all` para também incluir sessões em background concluídas. Cada entrada tem `cwd`, `kind` e `startedAt`. Entradas em background também têm `id`, utilizável com `claude attach`/`logs`/`stop`, e `state`: um de `working`, `blocked`, `done`, `failed` ou `stopped`. `pid` e `status` estão presentes apenas enquanto o processo está ativo, mais `waitingFor` quando status é `waiting`, que diz no que a sessão está bloqueada, como `permission prompt` ou `input needed`; `sessionId` e `name` aparecem quando definidos. Uma entrada interativa que você nunca nomeou carrega um `name` padrão construído a partir do nome do diretório de trabalho mais um sufixo de dois caracteres, como `my-app-3f`. Combine com `--cwd <path>` para filtrar |
| `claude attach <id>`         | Anexar a uma sessão neste terminal                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `claude logs <id>`           | Imprimir a saída recente da sessão                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `claude stop <id>`           | Interromper uma sessão. Também aceita `claude kill`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `claude respawn <id>`        | Reiniciar uma sessão, em execução ou interrompida, com sua conversa intacta, por exemplo, para usar um binário Claude Code atualizado                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `claude respawn --all`       | Reiniciar cada sessão em execução, por exemplo, para mover todas as sessões para um binário Claude Code atualizado de uma vez                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `claude rm <id>`             | Remover uma sessão da lista. Remove um worktree que Claude criou para a sessão se não houver alterações não confirmadas e nenhum commit que não seja enviado em nenhum lugar; caso contrário, a sessão também é mantida e o comando imprime o caminho do worktree e o motivo para que você possa resolvê-lo e executar `claude rm` novamente. Deixa um worktree que você criou por conta própria no lugar. A transcrição da conversa permanece em sua máquina local e continua disponível através de `claude --resume`                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `claude daemon status`       | Imprimir o estado do [supervisor](#the-supervisor-process), versão, diretório de socket e contagem de workers                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `claude daemon stop --any`   | Parar o processo supervisor e as sessões em background que ele hospeda. Passe `--keep-workers` para deixar as sessões em background em execução para que o próximo supervisor se reconecte a elas. O próximo `claude agents` ou `claude --bg` inicia um novo supervisor                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |

<h2 id="how-background-sessions-are-hosted">
  Como as sessões em background são hospedadas
</h2>

Toda sessão listada na visualização de agente é considerada uma sessão em background, independentemente de você estar atualmente anexado a ela. Em contraste, uma sessão iniciada executando `claude` diretamente está vinculada a esse terminal e termina quando ele fecha, a menos que você [a envie para o background](#from-inside-a-session).

<h3 id="the-supervisor-process">
  O processo supervisor
</h3>

Sessões em background são hospedadas por um processo supervisor por usuário, separado do seu terminal e da visualização de agente. O supervisor é iniciado automaticamente na primeira vez que você coloca uma sessão em background ou abre a visualização de agente, e você não o gerencia diretamente.

Quando uma atualização substituiu ou removeu o binário do qual um processo Claude Code em execução foi iniciado, esse processo inicia o supervisor a partir de outra cópia instalada, como o launcher `claude` instalado ou a versão mais recente no disco.

O supervisor mantém um processo worker pré-aquecido pronto para que um dispatch da visualização de agente ou `claude --bg` seja iniciado sem o atraso de um cold launch. Quando você faz um dispatch, o supervisor atribui o worker pré-aquecido à sua sessão, aplica o diretório, configurações e credenciais dessa sessão a ele e, em seguida, inicia um substituto para o próximo dispatch. Se nenhum worker pré-aquecido saudável estiver disponível, o supervisor inicia um novo processo.

O supervisor e suas sessões se autenticam com as mesmas credenciais armazenadas que suas sessões interativas e não fazem conexões de rede adicionais além da API do modelo. Variáveis de seleção de provedor, como `CLAUDE_CODE_USE_BEDROCK` e aliases `ANTHROPIC_DEFAULT_*_MODEL`, são lidas do shell que fez dispatch de cada sessão e são aplicadas ao seu worker.

O `PATH` do shell de dispatch é aplicado ao worker da mesma forma, então comandos de shell que a sessão executa encontram as mesmas ferramentas que seu terminal faz. Antes da v2.1.203, uma sessão em background mantinha o `PATH` do shell que primeiro iniciou o supervisor, então ferramentas adicionadas ao seu `PATH` desde então poderiam estar faltando, mais frequentemente no Windows.

Uma sessão em background não herda variáveis de endpoint de gateway, como `ANTHROPIC_BASE_URL` ou as variáveis de URL base equivalentes do Amazon Bedrock, Google Cloud's Agent Platform e Microsoft Foundry, do shell que iniciou o supervisor. Sem um gateway exportado no shell do qual você faz dispatch, a sessão usa suas credenciais armazenadas e quaisquer valores `env` no [settings](https://code.claude.com/docs/pt/settings) do diretório do projeto. Para apontar cada sessão em um projeto para um [gateway LLM](https://code.claude.com/docs/pt/llm-gateway), defina `ANTHROPIC_BASE_URL` no bloco `env` do `settings.json` do `.claude/` desse projeto.

Se você exportar um gateway `ANTHROPIC_BASE_URL` no shell do qual você faz dispatch, ele chega ao worker dessa sessão. `ANTHROPIC_CUSTOM_HEADERS` e a credencial exportada junto com eles são encaminhados com ele. Isso acontece quando o supervisor foi iniciado a partir de um ambiente com o mesmo gateway. O supervisor captura seu ambiente do primeiro shell que abre a visualização de agente ou faz dispatch de uma sessão em background, então começar a partir do shell do gateway lhe dá esse ambiente. O encaminhamento também se aplica apenas a sessões despachadas para o diretório do qual você está fazendo dispatch, ou colocadas em background de sua própria sessão com `←` ou `/background`: fazer dispatch para um diretório diferente com `@repo` ou `--cwd` não carrega o gateway do shell, e o bloco `env` do `settings.json` desse projeto fornece o endpoint. Quando o ambiente do supervisor carrega um gateway diferente ou nenhum, o worker mantém suas credenciais armazenadas contra o endpoint padrão em vez de misturar a credencial de um ambiente com o endpoint de outro. Antes da v2.1.203, o `ANTHROPIC_BASE_URL` do shell de dispatch era descartado enquanto a `ANTHROPIC_API_KEY` exportada junto com ele era mantida, então a chave do gateway era enviada para o endpoint padrão e cada solicitação falhava com um 401.

O endpoint encaminhado se aplica apenas a esse processo ativo e nunca é escrito no disco. Quando o supervisor interrompe uma sessão inativa e depois a reinicia, o processo reiniciado lê seu endpoint de suas configurações novamente: com um `ANTHROPIC_AUTH_TOKEN` do gateway, ele volta para suas credenciais armazenadas, e com uma `ANTHROPIC_API_KEY` emitida pelo gateway, pode falhar ao autenticar até que o gateway seja definido nas configurações.

Cada sessão em background é seu próprio processo Claude Code, gerenciado pelo supervisor em vez de estar vinculado ao seu terminal. Uma sessão que está ativamente funcionando, aguardando sua entrada ou tem um terminal anexado mantém seu processo em execução. Um comando de shell em background em execução, subagent, workflow dinâmico ou monitor conta como trabalho ativo, então um processo de longa duração, como um servidor de desenvolvimento, mantém a sessão ativa.

Depois que uma sessão termina e fica desanexada por cerca de uma hora, o supervisor interrompe seu processo para liberar recursos. Uma sessão que você [fixou](#organize-the-list) com `Ctrl+T` é isenta e mantém seu processo em execução enquanto inativo. O transcript e o estado permanecem no disco de qualquer forma, e na próxima vez que você anexar, espreitar ou responder a uma sessão parada, o supervisor inicia um novo processo de onde parou. Quando cada sessão terminou e nenhum terminal está conectado, o supervisor em si sai e é iniciado novamente na próxima vez que você precisar dele.

O trabalho em background que a sessão iniciou no nível superior é entregue quando seu processo é interrompido, reiniciado ou atualizado, incluindo no Windows. O próximo processo iniciado para essa sessão retoma o trabalho:

* Um comando de shell em background que terminou no meio é relatado como concluído com sua saída
* Um workflow dinâmico retoma de onde parou
* Um [subagent em background](https://code.claude.com/docs/pt/sub-agents#run-subagents-in-foreground-or-background) retoma de seu próprio transcript

A partir da v2.1.198, a entrega cobre todos os três. Antes da v2.1.198, cobria apenas comandos de shell e workflows, então um subagent em background parava com o processo e era relatado como falho no próximo despertar.

O trabalho cujo estado vive apenas dentro do próprio processo para com ele em vez de ser entregue. Isso inclui comandos de shell que um subagent iniciou, que o subagent retomado pode iniciar novamente, e [monitors](https://code.claude.com/docs/pt/tools-reference#monitor-tool) em execução, cujo fluxo de eventos não pode ser movido para outro processo.

Deletar a sessão interrompe tudo que ela entregou. Para parar todo o trabalho em background da sessão com o processo em vez de entregá-lo, defina a variável de ambiente [`CLAUDE_CODE_DISABLE_BG_EXIT_HANDOFF`](https://code.claude.com/docs/pt/env-vars#variables) como `1`.

Um processo reiniciado encontra a conversa de uma sessão que [se moveu para uma worktree](#how-file-edits-are-isolated) no meio da tarefa: quando o transcript não está onde a sessão começou, Claude Code também procura sob as worktrees registradas do repositório. Antes da v2.1.207, reabrir essa sessão da visualização de agente após seu processo ter parado poderia mostrar uma conversa vazia com apenas seu prompt original, com o transcript ainda intacto no disco; abrir a sessão novamente na v2.1.207 ou posterior a recupera.

Se uma sessão reiniciada volta mostrando apenas seu prompt original porque Claude Code leu mal seu transcript como vazio, o transcript de conversa é renomeado com um sufixo `.orphaned-` em vez de ser deletado, portanto permanece em sua máquina.

Uma linha vazia deixada ao pressionar `←` que nunca recebeu um prompt é removida completamente após cerca de cinco minutos para que a lista se limpe por conta própria. Sessões iniciadas com `claude --bg` e sessões aguardando um prompt de configuração, como um diálogo de confiança, não são removidas dessa forma.

Quando o host fica com pouca memória, o supervisor interrompe as sessões inativas não fixadas primeiro e interrompe as fixadas inativas apenas se isso não liberou nada.

O supervisor observa o binário Claude Code instalado no disco e reinicia para a nova versão após o [auto-updater](https://code.claude.com/docs/pt/setup#auto-updates) regular substituí-lo. Esta é uma observação de arquivo local, não uma verificação de rede. Sessões em background são processos desanexados, então continuam em execução durante a reinicialização e o novo supervisor se reconecta a elas. Uma sessão fixada inativa também é reiniciada no local para a nova versão para que ela pegue a atualização sem você se reconectar.

Depois que o novo supervisor assume, ele também reinicia as sessões inativas restantes para a nova versão, algumas de cada vez em background, após um curto atraso que permite que terminais anexados durante a reinicialização se reconectem primeiro. Uma sessão que está funcionando, aguardando sua entrada ou tem um terminal anexado não é interrompida; ela se move para a nova versão na próxima vez que seu processo reinicia. Antes da v2.1.206, o supervisor movia apenas algumas sessões inativas por minuto para uma nova versão, então as sessões poderiam continuar executando a versão antiga por um tempo após uma atualização.

Essas reinicializações apenas movem uma sessão para uma versão mais recente. Um supervisor executando uma versão mais antiga do Claude Code do que aquela com a qual o processo de uma sessão foi iniciado deixa esse processo sozinho; a sessão continua executando a versão mais recente até que um supervisor mais recente assume.

Executar `claude attach` enquanto o supervisor está reiniciando uma sessão, seja para uma atualização, um travamento ou uma migração, aguarda o processo de substituição em vez de falhar. Uma linha de status como `Agent is updating to the new Claude Code…` nomeia o que está aguardando e conta os segundos decorridos, e o comando se conecta assim que a sessão estiver pronta. Após cerca de 60 segundos, ele para de aguardar e relata um erro. Antes da v2.1.205, `claude attach` parou de tentar novamente após alguns segundos e imprimiu um erro enquanto a sessão ainda estava reiniciando.

<h3 id="where-state-is-stored">
  Onde o estado é armazenado
</h3>

O estado da sessão é armazenado sob seu diretório de configuração Claude Code. Se você definir [`CLAUDE_CONFIG_DIR`](https://code.claude.com/docs/pt/env-vars), o supervisor usa esse diretório em vez de `~/.claude` e é executado como uma instância separada com suas próprias sessões.

| Caminho                          | Conteúdo                                                                                                     |
| :------------------------------- | :----------------------------------------------------------------------------------------------------------- |
| `~/.claude/daemon.log`           | Log do supervisor                                                                                            |
| `~/.claude/daemon/roster.json`   | Lista de sessões em background em execução, usada para se reconectar após uma reinicialização                |
| `~/.claude/jobs/<id>/state.json` | Estado por sessão mostrado na visualização de agente                                                         |
| `~/.claude/jobs/<id>/tmp/`       | Diretório de rascunho por sessão. Escritas aqui não solicitam permissão. Removido quando a sessão é excluída |

Cada sessão em background tem a variável de ambiente `CLAUDE_JOB_DIR` definida para seu diretório `~/.claude/jobs/<id>`, então comandos de shell que a sessão executa podem escrever arquivos temporários em `$CLAUDE_JOB_DIR/tmp` sem colidir com sessões paralelas.

Para inspecionar este estado sem ler os arquivos diretamente, execute `claude daemon status`. Ele relata se o supervisor está acessível, seu ID de processo e versão, o diretório do socket e quantas sessões em background estão ativas.

O comando também avisa quando o supervisor em execução está em uma versão diferente do `claude` que você invocou, o que acontece após uma atualização que o supervisor ainda não reiniciou. O aviso mostra ambas as versões e diz para você executar `claude daemon stop --any` para pegar a nova versão. Quando Claude Code é instalado como um serviço do SO, o comando sugerido é `claude daemon stop` sem a flag.

Sessões sobrevivem a esse desajuste de versão intactas: uma versão mais antiga do Claude Code que atualiza o `state.json` de uma sessão preserva campos que não reconhece e mantém a sessão listada. A lista de sessões em `roster.json` segue a mesma regra: uma versão mais antiga que a reescreve preserva campos que uma versão mais nova escreveu, então sessões iniciadas pela versão mais nova permanecem acessíveis e continuam aceitando entrada após o supervisor reiniciar. Antes da v2.1.200, versões mais antigas poderiam descartar esses campos ao reescrever.

No Windows, `claude daemon status` expõe o erro de arquivo subjacente quando o arquivo de chave de pipe do daemon está bloqueado ou ilegível em vez de relatar uma falha de conexão genérica.

<h3 id="turn-off-agent-view">
  Desativar a visualização de agente
</h3>

Para desativar agentes em background e a visualização de agente completamente, defina a [configuração](https://code.claude.com/docs/pt/settings) `disableAgentView` como `true` ou defina a variável de ambiente `CLAUDE_CODE_DISABLE_AGENT_VIEW`. Os administradores podem impor isso através de [configurações gerenciadas](https://code.claude.com/docs/pt/permissions#managed-settings).

<h2 id="troubleshooting">
  Troubleshooting
</h2>

<h3 id="claude-agents-lists-subagents-instead-of-opening-agent-view">
  `claude agents` lista subagentes em vez de abrir a visualização de agentes
</h3>

Se `claude agents` imprime uma contagem seguida pelos seus subagentes configurados e depois sai, a visualização de agentes não está disponível no seu ambiente. Execute `claude update` para instalar a versão mais recente.

Se a visualização de agentes ainda não abrir após atualizar, verifique se ela foi [desativada](#turn-off-agent-view) por uma configuração ou variável de ambiente.

<h3 id="agent-view-opens-with-no-sessions">
  Agent view abre sem sessões
</h3>

Antes de você despachar sua primeira sessão, agent view mostra os cabeçalhos de seção vazios com uma descrição sob cada um, mais uma explicação de uma linha acima da entrada, no lugar da lista de sessões. Digite um prompt na entrada na parte inferior e pressione `Enter` para despachar sua primeira sessão.

<h3 id="backgrounding-shows-a-background-this-session-dialog">
  Backgrounding mostra um diálogo `Background this session?`
</h3>

Se pressionar `←` para colocar a sessão atual em background mostrar um diálogo `Background this session?`, a sessão tem trabalho em andamento que não pode se mover para a sessão em background, como um [monitor](https://code.claude.com/docs/pt/tools-reference#monitor-tool) em execução, e Claude Code não o interromperá silenciosamente. O diálogo nomeia o trabalho que será interrompido e, separadamente, conta as tarefas que são transferidas. Execute `/tasks` para ver tudo que está em execução, depois confirme para colocar em background mesmo assim ou escolha `Stay` para deixar o trabalho terminar primeiro. Veja [From inside a session](#from-inside-a-session) para quais tipos de tarefas são transferidos e quais são interrompidos.

<h3 id="prompt-rejected-as-too-short">
  Prompt rejected as too short
</h3>

A entrada de dispatch espera uma descrição de tarefa, não um abridor conversacional. Um prompt com menos de quatro caracteres é rejeitado com uma dica `Too short` para que um pressionamento de tecla acidental não inicie uma sessão. Descreva o que você quer que a sessão faça, como `investigate the flaky checkout test`.

<h3 id="sessions-show-as-failed-after-shutdown">
  Sessions show as failed after shutdown
</h3>

Desligar ou reiniciar sua máquina interrompe as sessões em execução em background, então elas mostram como falhadas quando você abre agent view novamente. Anexe, espreite ou responda a qualquer uma delas e a sessão reinicia de onde parou.

Sleep sozinho não causa isso. Sessões são preservadas durante o sleep e o supervisor se reconecta a elas ao acordar.

<h3 id="opening-a-session-says-the-conversation-is-already-open">
  Opening a session says the conversation is already open
</h3>

Abrir uma linha parada cuja conversa também está sendo mantida aberta por outro processo Claude Code não interativo em execução, por exemplo um worker em background para a mesma conversa que ainda está encerrando, mostra `This conversation is already open in another running Claude session` em vez de iniciar o processo da linha, porque dois processos não podem escrever no mesmo transcript. Responda na sessão que já tem a conversa aberta, ou saia dela e abra a linha novamente. Uma resposta que você digitou com a tentativa recusada não é perdida; ela é enviada na próxima vez que a sessão inicia.

Antes da v2.1.203, este estado iniciava um segundo processo mesmo assim. Esse processo saía com um erro `currently running as a background agent` e a linha mostrava como falhada.

<h3 id="a-session-fails-before-starting-with-a-possibly-low-memory-note">
  A session fails before starting with a `possibly low memory` note
</h3>

A partir da v2.1.199, quando o processo de uma sessão em background sai antes de terminar de iniciar e o host está com pouca memória, o status da linha nomeia a saída e adiciona `possibly low memory — free some up and retry`. Versões anteriores mostravam apenas o motivo da saída para essa falha.

A nota é uma hipótese, não uma causa confirmada. Claude Code a adiciona apenas quando o processo saiu silenciosamente, sem escrever um erro e sem ser interrompido por um sinal, e o host relatou pouca memória naquele momento. Quando o processo escreveu um erro antes de sair, a linha mostra esse erro em vez disso.

Libere memória na máquina, depois anexe, espreite ou responda à linha e o supervisor inicia um novo processo para a sessão. Quando a memória permanece baixa, o supervisor também [interrompe sessões ociosas](#the-supervisor-process) para liberar recursos por conta própria.

<h3 id="agent-view-says-the-background-service-did-not-respond">
  Agent view says the background service did not respond
</h3>

Se anexar, espreitar ou `claude logs` relatar que o serviço de background não respondeu, o processo supervisor provavelmente travou. Interrompa-o e deixe o próximo `claude agents` iniciar um novo. Para manter suas sessões em background em execução durante a reinicialização, passe `--keep-workers`:

```bash theme={null}
claude daemon stop --any --keep-workers
```

O novo supervisor se reconecta às sessões em execução. Sem `--keep-workers`, o comando também encerra as sessões em background. O sinalizador `--any` confirma que você deseja interromper um supervisor que foi iniciado sob demanda em vez de como um serviço instalado, que é o padrão.

Um supervisor que inicia mas não consegue aceitar conexões sai e libera seu bloqueio por conta própria, portanto o próximo `claude agents` inicia um novo sem essa parada manual. Os passos acima se aplicam quando um supervisor em execução trava.

No Windows, se o supervisor não responder à solicitação de parada, o comando imprime seu ID de processo. Encerre esse processo com `taskkill /PID <pid>` para concluir a recuperação. As sessões em background ainda são preservadas quando você passou `--keep-workers`.

<h3 id="dispatch-fails-with-could-not-resolve-authentication-method">
  Dispatch fails with `Could not resolve authentication method`
</h3>

Se um dispatch em background falhar com `Could not resolve authentication method` enquanto sessões interativas autenticam normalmente, o worker que recebeu o dispatch não pegou as credenciais. O supervisor fornece um snapshot de credencial fresco quando atribui um [pre-warmed worker](#the-supervisor-process), então este erro significa que nenhuma credencial armazenada estava disponível para o próprio processo supervisor. Confirme que você executou `/login` ou configurou uma chave de API, depois interrompa o supervisor:

```bash theme={null}
claude daemon stop --any --keep-workers
```

O próximo `claude agents` ou `claude --bg` inicia um novo supervisor que lê suas credenciais armazenadas. Se você autenticar com uma variável de ambiente como `ANTHROPIC_API_KEY` em vez de `/login`, execute esse próximo comando a partir de um shell onde a variável está definida.

Veja a [referência de erro](https://code.claude.com/docs/pt/errors#could-not-resolve-authentication-method) para a lista completa de causas e correções.

<h3 id="background-sessions-can’t-read-desktop-documents-or-downloads-on-macos">
  Background sessions can't read Desktop, Documents, or Downloads on macOS
</h3>

No macOS, o host da sessão em background é executado como seu próprio processo e solicita acesso a pastas protegidas separadamente do seu terminal. Se uma sessão em background relatar `Operation not permitted` ao ler `~/Desktop`, `~/Documents`, `~/Downloads` ou outro local protegido, conceda acesso em Configurações do Sistema em Privacidade e Segurança > Arquivos e Pastas, ou ative Acesso Total ao Disco para a entrada.

Com o instalador nativo, a entrada aparece como Claude Code e a concessão persiste entre atualizações. Com outros métodos de instalação, como Homebrew ou npm, a entrada mostra o caminho do binário e pode precisar ser concedida novamente após atualizar.

<h3 id="background-sessions-can’t-reach-local-network-hosts-on-macos">
  Background sessions can't reach local-network hosts on macOS
</h3>

No macOS 15 e posterior, o sistema bloqueia um processo de alcançar dispositivos na sua rede local até que você conceda permissão de Rede Local. Antes da v2.1.198, o host da sessão em background nunca solicitava essa permissão, então comandos direcionados a um endereço LAN falhavam com `connect: no route to host` mesmo que o mesmo comando funcionasse em um terminal em primeiro plano. A partir da v2.1.198, o primeiro comando em uma sessão em background que se conecta a um endereço de rede local dispara o prompt de permissão de Rede Local do macOS para Claude Code. Conceda uma vez e esses comandos alcançam hosts LAN da mesma forma que fazem em um terminal em primeiro plano.

<h3 id="a-session-is-slow-to-respond-after-attaching">
  A session is slow to respond after attaching
</h3>

Depois que uma sessão termina e fica desanexada por cerca de uma hora, o supervisor interrompe seu processo para liberar recursos. Anexar inicia um novo processo de onde parou e muda para a sessão imediatamente enquanto o processo reinicia. Sessões que estão funcionando, aguardando você ou [fixadas](#organize-the-list) não são interrompidas dessa forma, portanto fixe uma sessão com `Ctrl+T` para mantê-la responsiva.

Enquanto o processo inicia, a última tela da transcrição da sessão é mostrada com uma nota `Session is starting` abaixo dela, e a sessão ao vivo a substitui assim que estiver pronta.

<h3 id="claude/worktrees/-is-filling-up">
  `.claude/worktrees/` is filling up
</h3>

Deletar uma sessão em agent view remove a worktree que Claude criou para ela, e uma worktree que não pode ser removida com segurança [mantém sua linha de sessão](#organize-the-list) para que não seja órfã. `claude rm` mantém uma worktree que tem mudanças não commitadas, e sua linha de sessão, e imprime o caminho mantido. Liste entradas restantes com `git worktree list` no diretório do projeto e remova cada uma com `git worktree remove <path>`. Veja [Clean up worktrees](https://code.claude.com/docs/pt/worktrees#clean-up-worktrees).

<h2 id="limitations">
  Limitações
</h2>

Agent view está em visualização de pesquisa com as seguintes limitações:

* **Limites de taxa se aplicam**: sessões em background consomem o uso de sua assinatura da mesma forma que sessões interativas, então executar dez agentes em paralelo usa cota aproximadamente dez vezes mais rápido do que executar um.
* **Sessões são locais**: sessões em background são executadas em sua máquina. Elas são preservadas durante o sleep, mas param se a máquina desligar.
* **Worktrees criadas pelo Claude são deletadas com a sessão em agent view**: confirme as alterações antes de deletar uma sessão que editou arquivos em seu próprio worktree. Um worktree com commits que não foram enviados para lugar nenhum é mantido junto com a sessão. `claude rm` também mantém um worktree que tem alterações não confirmadas junto com sua sessão, e um worktree que você criou você mesmo é deixado no lugar.

<h2 id="related-resources">
  Recursos relacionados
</h2>

Para outras maneiras de executar Claude em paralelo, consulte:

* [Executar agentes em paralelo](https://code.claude.com/docs/pt/agents): compare agent view com subagentes, equipes de agentes e worktrees
* [Equipes de agentes](https://code.claude.com/docs/pt/agent-teams): coordene múltiplas sessões que se mensageiam
* [Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web): execute sessões em um ambiente de nuvem gerenciado em vez de localmente

<h2 id="version-history">
  Histórico de versões
</h2>

Agent view evoluiu rapidamente durante a visualização de pesquisa. Se você estiver em uma versão mais antiga do Claude Code, alguns comportamentos nesta página podem diferir; em particular, `claude agents` rejeita flags que ainda não suporta com um erro de `unknown option`. A tabela abaixo lista quando cada flag e comportamento foi adicionado.

| Versão   | Mudança                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| v2.1.208 | Anexar a uma sessão cujo processo parou mostra a última tela cheia de sua transcrição enquanto o processo inicia, em vez de apenas uma nota de `Session is starting`. Uma resposta que não pode ser entregue porque o serviço de fundo está inacessível ou o envio falha é salva e enviada como o próximo prompt da sessão quando seu processo inicia novamente; antes desta versão, uma resposta perdida enquanto o serviço de fundo estava inacessível era descartada. Um processo cujo próprio binário foi substituído por uma atualização ainda pode iniciar o supervisor, a partir do inicializador `claude` instalado ou da versão mais recente no disco, em vez de falhar até que Claude Code fosse reiniciado. Um supervisor executando uma versão mais antiga nunca reinicia uma sessão ociosa iniciada por uma versão mais nova em seu próprio binário mais antigo. Deletar uma sessão remove seu worktree mesmo depois que a sessão moveu o worktree para uma branch diferente, e mantém o worktree junto com a linha da sessão quando o worktree tem commits que não foram enviados para lugar nenhum ou outra sessão o reclama, em vez de destruir os commits ou deixar o worktree órfão. `/install-github-app` e a lista de configurações `/mcp` e suas ações de autenticação são recusadas em uma sessão de fundo com uma mensagem nomeando a alternativa; em v2.1.208 apenas, o seletor `/model` foi recusado da mesma forma e um `/model <name>` digitado mudou apenas essa sessão em vez de também salvar seu modelo padrão. |
| v2.1.207 | O painel de espiada abre com a sentença que a linha trunca, como a pergunta exata para uma sessão que está aguardando você, e mostra quanto tempo uma sessão bloqueada está aguardando como uma única linha `waiting 3m` em vez de prefixar o mesmo timestamp para a sentença de status e a pergunta. Colar o mesmo texto novamente na entrada de despacho expande o placeholder `[Pasted text #N]` colapsado em vez de adicionar um segundo. Uma sessão em background nomeada ao aceitar um plano mostra esse nome em sua linha. Uma sessão em background que se moveu para um worktree mantém sua conversa quando seu processo é reiniciado a partir de agent view.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| v2.1.206 | Resumos de linha preenchem a largura restante da linha e truncam apenas na borda direita do terminal em vez de em 64 colunas. Depois que o supervisor reinicia em uma nova versão do Claude Code, ele reinicia as sessões em background ociosas restantes nessa versão em background em vez de algumas por minuto. Deletar uma sessão com `Ctrl+X` ou `claude rm` também a limpa da lista de sessão do supervisor, para que a linha não reapareça mais após um reinício do supervisor.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| v2.1.205 | Resumos de linha mostram o relatório de uma linha da própria sessão, truncado em 64 colunas, em vez de uma invocação de ferramenta bruta ou uma contagem `done/total`; linhas agrupadas por diretório abrem com uma palavra de estado colorida. O painel de espiada abre com a sentença de status completa e, para uma sessão aguardando você, sua pergunta exata acima da entrada de resposta. Sessões que editam, comentam, fecham ou marcam um pull request como pronto com `gh` estão vinculadas a ele, não apenas aquelas que criam ou fazem checkout de um pull request, um push vincula um pull request mesmo quando o nome da branch local não corresponde, e um pull request cuja saída do comando de criação excedeu o limite inline também está vinculado. Um turno sem texto legível mantém o estado anterior da sessão em vez de revertê-lo para `Working`. `claude attach` aguarda até cerca de 60 segundos por uma sessão que está reiniciando, com uma linha de status nomeando o motivo, em vez de falhar.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| v2.1.203 | Um gateway `ANTHROPIC_BASE_URL` exportado no shell de despacho alcança as sessões despachadas a partir dele no mesmo diretório quando o supervisor compartilha esse gateway environment, em vez de ser descartado enquanto a chave de API exportada junto com ele era mantida. O `PATH` do shell de despacho é aplicado a cada worker da sessão. Pressionar `←` enquanto subagentes estão em execução aguarda por eles em vez de reiniciá-los após dez segundos. A lista vazia sempre mostra os cabeçalhos da seção com uma descrição sob cada um. Digitar `@` na entrada de despacho também lista os git worktrees registrados do repositório de inicialização que vivem dentro de sua árvore de diretórios. Um esforço herdado da configuração `effortLevel` segue edições posteriores dessa configuração em vez de ser fixado no despacho. Abrir uma sessão parada cuja conversa já está aberta em outra sessão em execução é recusado com uma mensagem em vez de falhar na linha. Um comando que não está disponível em agent view deixa o texto digitado na entrada. Um hook `WorktreeCreate` que falha fora de um repositório git não bloqueia mais a sessão de editar arquivos.                                                                                                                                                                                                                                                                                                                                                         |
| v2.1.202 | Um nome definido com `/rename` ou `Ctrl+R` em uma sessão em background persiste quando o supervisor para e reinicia seu processo, em vez de reverter para o nome com o qual a sessão foi despachada.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| v2.1.200 | Uma versão mais antiga do Claude Code que reescreve a lista de sessão em `roster.json` preserva campos escritos por uma versão mais nova, correspondendo à garantia existente de `state.json`, para que sessões iniciadas pela versão mais nova continuem aceitando entrada após o supervisor reiniciar. Quando você abre uma sessão que parou de responder, o supervisor reinicia seu processo e a sessão continua a resposta interrompida de onde parou.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| v2.1.199 | Uma sessão em background cujo processo sai antes de terminar de iniciar em um host com pouca memória mostra `possibly low memory — free some up and retry` no status de sua linha em vez de apenas o motivo de saída simples. Colocar uma sessão em background com `←` ou `/background` carrega seu `/color` para a nova linha.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| v2.1.198 | Agent view envia uma notificação através de `preferredNotifChannel` quando uma sessão em background precisa de entrada, termina ou falha, e dispara o hook `Notification` com o tipo `agent_needs_input` ou `agent_completed`. `←` e `/exit` dentro de `claude attach <id>` retornam para agent view em vez de sair para o shell; `Ctrl+Z` retorna para o shell. Uma sessão em background que isolou seu trabalho em um worktree faz commit, envia seu próprio branch isolado, nunca `main` ou `master`, e abre um pull request em rascunho quando termina em vez de perguntar primeiro. `/login` é executado em agent view e abre o diálogo de entrada. O diálogo de saída `Background work is running` oferece `Move to background and exit`. A entrega de saída também cobre subagentes em background, que retomam de sua transcrição no próximo despertar em vez de serem relatados como falhados. `claude --bg` combinado com `-p` ou `--print` é rejeitado com um erro.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| v2.1.196 | Um único pressionamento de `←` coloca em background uma sessão em primeiro plano; versões anteriores exigiam dois pressionamentos, com uma dica de rodapé e uma confirmação. `--dangerously-skip-permissions` passado para `claude agents` mostra o aviso de bypass em vez de ser silenciosamente descartado. Sessões interativas que você nunca nomeou carregam um nome padrão como `my-app-3f` em listagens de sessão e `claude agents --json`. Comandos shell em background e workflows dinâmicos sobrevivem ao processo da sessão sendo interrompido, reiniciado ou atualizado, incluindo no Windows; defina `CLAUDE_CODE_DISABLE_BG_EXIT_HANDOFF=1` para desativar a entrega. Um transcript lido mal como vazio no reinício é renomeado com um sufixo `.orphaned-` em vez de ser deletado.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| v2.1.195 | O trabalho em andamento é transferido quando você coloca em background uma sessão no Windows também; defina `CLAUDE_DISABLE_ADOPT=1` para interrompê-lo em vez disso. O grupo `Completed` preenche o espaço vertical restante e o cabeçalho se compacta em terminais curtos. Uma versão mais antiga do Claude Code não descarta mais campos `state.json` mais novos ou oculta essas sessões de `claude agents`. Anexar a uma sessão parada muda imediatamente em vez de mostrar uma tela em branco por até cinco segundos. Um supervisor que não consegue aceitar conexões sai e libera seu bloqueio por conta própria.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v2.1.174 | Sessões em background não herdam mais variáveis de endpoint de gateway como `ANTHROPIC_BASE_URL` do shell de inicialização do supervisor; o supervisor fornece um snapshot de credencial fresco para workers pré-aquecidos, corrigindo erros espúrios de `Could not resolve authentication method`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| v2.1.172 | `/model` na entrada de despacho define uma substituição de modelo de despacho com escopo de sessão.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| v2.1.161 | Resumos de linha mostram uma contagem `done/total` para itens de trabalho paralelos; o painel de espiada nomeia o item de trabalho paralelo que está em execução há mais tempo.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| v2.1.157 | `claude agents` aceita `--agent`; sessões despachadas honram a configuração `agent`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| v2.1.145 | Voice dictation suportada na entrada de resposta do painel de espiada e na entrada de despacho.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| v2.1.143 | Configuração `worktree.bgIsolation` adicionada; `claude agents` aceita `--allow-dangerously-skip-permissions`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v2.1.142 | `claude agents` aceita `--permission-mode`, `--model`, `--effort`, `--dangerously-skip-permissions`, `--settings`, `--add-dir`, `--plugin-dir`, `--mcp-config` e `--strict-mcp-config`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v2.1.141 | `claude agents` aceita `--cwd` para escopar a lista a um projeto.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| v2.1.139 | Agent view introduzido como uma visualização de pesquisa.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
