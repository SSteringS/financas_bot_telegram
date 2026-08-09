<!-- Fonte: https://code.claude.com/docs/pt/agents | Baixado em: 2026-08-06 | slug: agents -->

# Executar agentes em paralelo

> Compare as formas como Claude Code pode assumir múltiplas tarefas simultaneamente: subagentes, visualização de agentes, equipes de agentes e workflows dinâmicos.

[Subagentes](https://code.claude.com/docs/pt/sub-agents), [visualização de agentes](https://code.claude.com/docs/pt/agent-view), [equipes de agentes](https://code.claude.com/docs/pt/agent-teams) e [workflows dinâmicos](https://code.claude.com/docs/pt/workflows) cada um paraleliza o trabalho de uma forma diferente. O correto depende de se você quer permanecer em cada conversa você mesmo, delegar tarefas e verificar depois, ou ter Claude coordenando um grupo de trabalhadores para você.

| Abordagem                                 | O que oferece                                                                                                                                                             | Use quando                                                                                                                                                                                                                                                               |
| :---------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [Subagentes](https://code.claude.com/docs/pt/sub-agents)              | Trabalhadores delegados dentro de uma sessão que fazem uma tarefa secundária em seu próprio contexto e retornam um resumo                                                 | Uma tarefa secundária inundaria sua conversa principal com resultados de pesquisa, logs ou conteúdos de arquivo que você não consultará novamente                                                                                                                        |
| [Visualização de agentes](https://code.claude.com/docs/pt/agent-view) | Uma tela para despachar e monitorar sessões em execução em segundo plano, aberta com `claude agents`. Visualização de pesquisa                                            | Você tem várias tarefas independentes e quer delegá-las, verificar o status rapidamente e intervir apenas quando uma precisar de você                                                                                                                                    |
| [Equipes de agentes](https://code.claude.com/docs/pt/agent-teams)     | Múltiplas sessões coordenadas com uma lista de tarefas compartilhada e mensagens entre agentes, gerenciadas por um líder. Experimental e desabilitado por padrão          | Você quer que Claude divida um projeto em partes, as atribua e mantenha os trabalhadores sincronizados                                                                                                                                                                   |
| [Workflows dinâmicos](https://code.claude.com/docs/pt/workflows)      | Um script que executa muitos subagentes e verifica seus resultados, para um trabalho muito grande para coordenar em um único turno ou que precisa de mais de uma passagem | Uma tarefa cresce além de um punhado de subagentes, ou você quer que as descobertas sejam verificadas uma contra a outra: uma auditoria em toda a base de código, uma migração de 500 arquivos, pesquisa com verificação cruzada ou um plano elaborado de vários ângulos |

Em cada abordagem, os trabalhadores são sessões Claude. Para envolver uma ferramenta diferente, exponha-a ao Claude como um [servidor MCP](https://code.claude.com/docs/pt/mcp).

Duas ferramentas adicionais suportam este trabalho sem serem uma forma de executar agentes em si:

* [Worktrees](https://code.claude.com/docs/pt/worktrees) dão a cada sessão um checkout git separado, para que sessões paralelas nunca editem os mesmos arquivos. Use-as para sessões que você executa você mesmo. A visualização de agentes move automaticamente cada sessão despachada para seu próprio worktree, e subagentes que você gera podem cada um receber um também.
* [`/batch`](https://code.claude.com/docs/pt/commands) é uma [skill](https://code.claude.com/docs/pt/skills) que tem Claude dividir uma grande mudança em 5 a 30 subagentes isolados em worktree que cada um abre um pull request. É um uso empacotado de subagentes e worktrees, não um estilo de coordenação separado.

Alguns outros recursos executam Claude sem você dirigir cada passo, mas resolvem um problema diferente do que dividir trabalho entre agentes:

* Um [comando bash em segundo plano](https://code.claude.com/docs/pt/interactive-mode#background-bash-commands) executa um comando shell sem bloquear a conversa. Ele não gera um agente.
* Um [subagente bifurcado](https://code.claude.com/docs/pt/sub-agents#fork-the-current-conversation) é um subagente que herda seu contexto de conversa completo em vez de começar do zero. É uma forma de gerar um subagente, não uma superfície separada.
* Uma [rotina](https://code.claude.com/docs/pt/routines) executa uma sessão em um cronograma na nuvem da Anthropic, não em paralelo em sua máquina.

<Note>
  Executar várias sessões ou subagentes simultaneamente multiplica o uso de tokens. Veja [Custos](https://code.claude.com/docs/pt/costs) para detalhes de uso e limite de taxa.
</Note>

<h2 id="choose-an-approach">
  Escolha uma abordagem
</h2>

A abordagem correta depende de quem coordena o trabalho, se os trabalhadores precisam se comunicar e se editam os mesmos arquivos:

* **Quem coordena o trabalho?**
  * Claude delega e coleta resultados dentro de uma conversa: [subagentes](https://code.claude.com/docs/pt/sub-agents)
  * Você entrega tarefas independentes e verifica depois: [visualização de agentes](https://code.claude.com/docs/pt/agent-view)
  * Claude planeja, atribui e supervisiona um grupo de trabalhadores: [equipes de agentes](https://code.claude.com/docs/pt/agent-teams), experimental e desabilitado por padrão
  * Um script mantém o plano em vez do julgamento turno a turno de Claude: [fluxos de trabalho dinâmicos](https://code.claude.com/docs/pt/workflows). Veja [como fluxos de trabalho se comparam a subagentes e skills](https://code.claude.com/docs/pt/workflows#when-to-use-a-workflow)
* **Os trabalhadores precisam conversar um com o outro?** Subagentes relatam resultados de volta para a conversa que os gerou, e sessões de visualização de agentes relatam apenas para você. Companheiros de equipe em uma equipe de agentes compartilham uma lista de tarefas e se mensageiam diretamente.
* **As tarefas tocam os mesmos arquivos?** Isole o trabalho com [worktrees](https://code.claude.com/docs/pt/worktrees). Subagentes e sessões que você executa você mesmo podem cada um usar um worktree separado. Equipes de agentes não isolam companheiros de equipe em worktrees, então [particione o trabalho](https://code.claude.com/docs/pt/agent-teams#avoid-file-conflicts) para que cada companheiro de equipe possua um conjunto diferente de arquivos.

<h2 id="check-on-running-work">
  Verifique o trabalho em execução
</h2>

O comando para verificar o trabalho em execução depende de qual abordagem você usou:

* Para sessões em segundo plano, `claude agents` abre [visualização de agentes](https://code.claude.com/docs/pt/agent-view): uma tela mostrando cada sessão, seu estado e quais precisam de sua entrada.
* Para subagentes na sessão atual, subagentes em segundo plano nomeados aparecem na digitação de menção @- com seu status. A partir da v2.1.198, `/agents` não abre mais um painel; imprime um aviso apontando para os locais dos arquivos de subagentes. Para [criar e editar subagentes personalizados](https://code.claude.com/docs/pt/sub-agents#configure-subagents), peça ao Claude ou edite os arquivos diretamente. Apesar do nome similar, `/agents` é separado de `claude agents`.
* Para qualquer coisa em execução em segundo plano da sessão atual, `/tasks` lista cada item e permite que você verifique, se anexe ou interrompa. A lista também inclui subagentes que terminaram.
* Para fluxos de trabalho dinâmicos, `/workflows` lista execuções em andamento e concluídas, a fase em que cada uma está e quantos agentes terminaram.

Para uma visualização de desktop de todas as suas sessões, veja [sessões paralelas no aplicativo desktop](https://code.claude.com/docs/pt/desktop#work-in-parallel-with-sessions).

<h2 id="learn-more">
  Saiba mais
</h2>

Cada guia abaixo cobre configuração e configuração para uma abordagem:

* [Criar subagentes personalizados](https://code.claude.com/docs/pt/sub-agents): defina especialistas reutilizáveis e controle quais ferramentas eles podem usar.
* [Gerenciar agentes com visualização de agentes](https://code.claude.com/docs/pt/agent-view): despache sessões, observe seu estado e se anexe quando uma precisar de você.
* [Orquestrar equipes de agentes](https://code.claude.com/docs/pt/agent-teams): configure um líder e companheiros de equipe, atribua tarefas e revise seu trabalho.
* [Orquestrar fluxos de trabalho dinâmicos](https://code.claude.com/docs/pt/workflows): execute um fluxo de trabalho agrupado ou deixe Claude escrever um que execute muitos subagentes e verifique suas descobertas um contra o outro.
* [Executar sessões paralelas com worktrees](https://code.claude.com/docs/pt/worktrees): inicie Claude em um checkout isolado, controle o que é copiado e limpe depois.
