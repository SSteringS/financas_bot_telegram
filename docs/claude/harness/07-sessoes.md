<!-- Fonte: https://code.claude.com/docs/pt/sessions | Baixado em: 2026-08-06 | slug: sessions -->

# Gerenciar sessões

> Nomeie, retome, ramifique e alterne entre conversas do Claude Code. Abrange `--continue`, `--resume`, `--from-pr`, o seletor `/resume`, nomeação de sessão, exportação de transcritos e onde os transcritos são armazenados.

Uma sessão é uma conversa salva vinculada a um diretório de projeto. Claude Code a armazena localmente conforme você trabalha, para que você possa retomar de onde parou, ramificar para tentar uma abordagem diferente ou alternar entre tarefas.

O [aplicativo desktop](https://code.claude.com/docs/pt/desktop#work-in-parallel-with-sessions), [Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web) e a [extensão VS Code](https://code.claude.com/docs/pt/vs-code#resume-past-conversations) mantêm seu próprio histórico de sessões. Esta página abrange a CLI.

<h2 id="resume-a-session">
  Retomar uma sessão
</h2>

As sessões são salvas continuamente em [arquivos de transcrição locais](#export-and-locate-session-data) conforme você trabalha, para que você possa retornar a uma após sair ou executar `/clear`. Use estes pontos de entrada:

| Comando                     | O que faz                                                         |
| :-------------------------- | :---------------------------------------------------------------- |
| `claude --continue`         | Retoma a sessão mais recente no diretório atual                   |
| `claude --resume`           | Abre o [seletor de sessão](#use-the-session-picker)               |
| `claude --resume <name>`    | Retoma a sessão nomeada diretamente                               |
| `claude --from-pr <number>` | Retoma a sessão vinculada a esse pull request                     |
| `/resume`                   | Alterna para uma conversa diferente de dentro de uma sessão ativa |

As sessões criadas com [`claude -p`](https://code.claude.com/docs/pt/headless) ou o [Agent SDK](https://code.claude.com/docs/pt/agent-sdk/overview) não aparecem no seletor de sessão, mas você ainda pode retomar uma passando seu ID de sessão para `claude --resume <session-id>`. Execute isto a partir do diretório em que a sessão foi iniciada: a busca de ID de sessão é limitada ao diretório do projeto atual e seus git worktrees, portanto uma sessão criada em outro lugar relata `No conversation found with session ID: <session-id>`.

<h3 id="where-the-session-picker-looks">
  Onde o seletor de sessão procura
</h3>

As sessões são armazenadas por diretório de projeto. Por padrão, o seletor de sessão mostra sessões interativas da worktree atual, além de sessões iniciadas em outro lugar que adicionaram o diretório atual com `/add-dir`. Use `Ctrl+W` para expandir para todas as worktrees do repositório ou `Ctrl+A` para expandir para cada projeto nesta máquina.

A partir da v2.1.169, mover uma sessão com [`/cd`](https://code.claude.com/docs/pt/commands) a relocata para o armazenamento de projeto do novo diretório, para que apareça no seletor desse diretório depois. A partir da v2.1.196, uma sessão movida fica fora do seletor do diretório antigo mesmo após uma falha ou saída forçada. Em versões anteriores, ela também poderia reaparecer na lista do diretório antigo após uma saída que não foi limpa quando o caminho antigo continha caracteres especiais como sublinhados.

Selecionar uma sessão de outra worktree do mesmo repositório a retoma no local. Selecionar uma sessão de um projeto não relacionado copia um comando `cd` e retoma para sua área de transferência.

Retomar por nome resolve no repositório atual e suas worktrees. Ambas as formas procuram por uma correspondência exata e a retomam diretamente mesmo que resida em uma worktree diferente:

| Comando                  | Correspondência exata | Nome ambíguo                                                                    |
| :----------------------- | :-------------------- | :------------------------------------------------------------------------------ |
| `claude --resume <name>` | Retoma diretamente    | Abre o seletor de sessão com o nome pré-preenchido como termo de pesquisa       |
| `/resume <name>`         | Retoma diretamente    | Relata um erro; execute `/resume` sem argumentos para abrir o seletor de sessão |

<h2 id="name-your-sessions">
  Nomeie suas sessões
</h2>

Dê às sessões nomes descritivos para que sejam encontráveis no seletor de sessão e retomáveis por nome. Isso é mais importante quando você está trabalhando em várias tarefas em paralelo.

| Quando                | Como definir o nome                                                                                                                                                               |
| :-------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Na inicialização      | `claude -n auth-refactor`                                                                                                                                                         |
| Durante uma sessão    | `/rename auth-refactor`. O nome também aparece na barra de prompt                                                                                                                 |
| Do seletor de sessão  | Destaque uma sessão e pressione `Ctrl+R`                                                                                                                                          |
| Na aceitação do plano | Aceitar um plano no [Plan Mode](https://code.claude.com/docs/pt/permission-modes#analyze-before-you-edit-with-plan-mode) nomeia a sessão a partir do conteúdo do plano, a menos que você já tenha definido um |

Depois que uma sessão é nomeada, retorne a ela com `claude --resume <name>` ou `/resume <name>`. Veja [Retomar uma sessão](#resume-a-session) para saber como a resolução de nomes se comporta entre worktrees.

As sessões interativas que você nunca nomeia ainda recebem um nome de exibição padrão quando iniciam. Requer Claude Code v2.1.196 ou posterior. O padrão combina o nome do diretório de trabalho com um sufixo de dois caracteres, por exemplo `my-app-3f`, e identifica a sessão em listagens de sessões em execução, como [agent view](https://code.claude.com/docs/pt/agent-view) e saída de `claude agents --json`.

O padrão não é um identificador de retomada: `claude --resume <name>`, `/resume <name>` e o seletor de sessão correspondem apenas aos nomes que você definiu. Nomear a sessão substitui o padrão.

<h2 id="use-the-session-picker">
  Use o seletor de sessão
</h2>

Execute `/resume` dentro de uma sessão ou `claude --resume` sem argumentos para abrir o seletor de sessão interativo. Use estes atalhos de teclado para navegar, pesquisar e expandir a lista:

| Atalho                                                    | Ação                                                                                                                                                                      |
| :-------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `↑` / `↓`                                                 | Navegar entre sessões                                                                                                                                                     |
| `→` / `←`                                                 | Expandir ou recolher sessões agrupadas                                                                                                                                    |
| `Enter`                                                   | Retomar a sessão destacada                                                                                                                                                |
| `Space`                                                   | Visualizar o conteúdo da sessão. `Ctrl+V` também funciona em terminais que não o capturam como colar                                                                      |
| `Ctrl+R`                                                  | Renomear a sessão destacada                                                                                                                                               |
| `/` ou qualquer caractere imprimível diferente de `Space` | Entrar no modo de pesquisa e filtrar sessões. Cole uma URL de pull ou merge request do GitHub, GitHub Enterprise, GitLab ou Bitbucket para encontrar a sessão que a criou |
| `Ctrl+A`                                                  | Mostrar sessões de todos os projetos nesta máquina. Pressione novamente para retornar ao repositório atual                                                                |
| `Ctrl+W`                                                  | Mostrar sessões de todas as worktrees do repositório atual. Pressione novamente para retornar à worktree atual. Mostrado apenas em repositórios com múltiplas worktrees   |
| `Ctrl+B`                                                  | Filtrar para sessões do branch git atual. Pressione novamente para mostrar todos os branches                                                                              |
| `Esc`                                                     | Sair do seletor de sessão ou modo de pesquisa                                                                                                                             |

Cada linha mostra o nome da sessão se definido, caso contrário, o resumo da conversa ou o primeiro prompt, junto com o tempo desde a última atividade, contagem de mensagens e branch git. O caminho do projeto aparece depois que você expande para todos os projetos com `Ctrl+A`.

As sessões bifurcadas criadas com `/branch`, `/rewind` ou `--fork-session` são agrupadas sob sua sessão raiz. Pressione `→` para expandir um grupo.

<h2 id="branch-a-session">
  Ramificar uma sessão
</h2>

Ramificar cria uma cópia da conversa até agora e o coloca nela, deixando o original intacto. Use-o para tentar uma abordagem diferente sem perder o caminho em que você estava.

De dentro de uma sessão, execute `/branch` com um nome opcional:

```text theme={null}
/branch try-streaming-approach
```

Se você omitir o nome, Claude Code nomeia o novo branch após o primeiro prompt na conversa. A partir da v2.1.198, isso também se aplica após [compaction](https://code.claude.com/docs/pt/how-claude-code-works#when-context-fills-up); versões anteriores voltavam para o nome literal `Branched conversation` em vez de procurar além do resumo de compaction para o primeiro prompt original.

Na linha de comando, combine `--continue` ou `--resume` com `--fork-session`:

```bash theme={null}
claude --continue --fork-session
```

A sessão original permanece inalterada e disponível no seletor de sessão. A confirmação `/branch` imprime dois IDs de sessão: o novo branch em que você está agora e o original. Para retornar ao original, passe seu ID para `/resume`, use o seletor de sessão ou execute `/resume <original-name>`. As permissões que você aprovou com "permitir para esta sessão" não são transferidas para o novo branch. Se você retomar a mesma sessão em dois terminais sem bifurcar, as mensagens de ambos se intercalam em um transcrição.

Para rewind baseado em checkpoint dentro de uma única sessão, veja [Checkpointing](https://code.claude.com/docs/pt/checkpointing).

<h2 id="manage-context-within-a-session">
  Gerenciar contexto dentro de uma sessão
</h2>

Estes comandos controlam o que está na janela de contexto sem deixar a sessão:

* **`/clear`**: comece do zero com um contexto vazio. A conversa anterior é salva e retomável com `/resume`, ou, no mesmo processo Claude Code, a partir da [entrada de sessão anterior do menu de rewind](https://code.claude.com/docs/pt/checkpointing#rewind-past-a-cleared-conversation)
* **`/compact [instructions]`**: substitua o histórico por um resumo, opcionalmente focado no que você especificar
* **`/context`**: mostrar o que está consumindo contexto atualmente

Para saber como a compactação interage com CLAUDE.md, skills e regras, veja o [guia de janela de contexto](https://code.claude.com/docs/pt/context-window). Para estratégias sobre quando limpar versus compactar, veja [Melhores práticas](https://code.claude.com/docs/pt/best-practices#manage-your-session).

<h2 id="export-and-locate-session-data">
  Exportar e localizar dados de sessão
</h2>

Execute `/export` para abrir um menu que permite copiar a conversa atual para sua área de transferência ou salvá-la como um arquivo de texto simples, com mensagens e saídas de ferramentas renderizadas como texto legível. Passe um nome de arquivo para pular o menu e escrever diretamente nesse arquivo.

<h3 id="access-conversations-from-scripts">
  Acessar conversas a partir de scripts
</h3>

`/export` produz uma transcrição renderizada para uma pessoa ler. As interfaces abaixo produzem dados estruturados para um script analisar: um resultado JSON de uma execução, o caminho para o arquivo de transcrição de uma sessão, ou um fluxo ao vivo de eventos. Escolha pelo que dispara o script:

* **Executar Claude uma vez e capturar o resultado**: invoque `claude -p` com [`--output-format json` ou `stream-json`](https://code.claude.com/docs/pt/headless#get-structured-output) para capturar o resultado, ID da sessão, uso e custo de uma execução não interativa como JSON estruturado.
* **Fazer uma pergunta a uma sessão existente**: passe um ID de sessão para [`claude -p --resume`](https://code.claude.com/docs/pt/headless#continue-conversations) para enviar um prompt de acompanhamento, como uma solicitação de resumo, e capturar a resposta estruturada.
* **Reagir a eventos de sessão**: leia o campo `transcript_path` que [hooks](https://code.claude.com/docs/pt/hooks#common-input-fields) e [comandos de linha de status](https://code.claude.com/docs/pt/statusline#available-data) recebem como entrada. Um hook `SessionEnd` pode arquivar a transcrição quando uma sessão termina.
* **Incorporar Claude em um aplicativo TypeScript ou Python**: use o [Agent SDK](https://code.claude.com/docs/pt/agent-sdk/overview) para receber cada mensagem programaticamente.

O exemplo abaixo usa a segunda interface. Ele envia um prompt de acompanhamento para uma sessão existente e lê a resposta com `jq`:

```bash theme={null}
claude -p --resume <session-id> --output-format json "summarize what we changed" | jq -r '.result'
```

<h3 id="where-transcripts-are-stored">
  Onde os transcritos são armazenados
</h3>

Por padrão, os transcritos são armazenados como JSONL em `~/.claude/projects/<project>/<session-id>.jsonl`, onde `<project>` é o caminho do seu diretório de trabalho com caracteres não alfanuméricos substituídos por `-`. Cada linha é um objeto JSON para uma mensagem, uso de ferramenta ou entrada de metadados. O formato de entrada é interno ao Claude Code e muda entre versões, portanto scripts que analisam esses arquivos diretamente podem quebrar em qualquer versão. Para construir sobre dados de sessão, use `/export` ou as [interfaces de script](#access-conversations-from-scripts) em vez disso.

A localização, retenção e comportamento de gravação são configuráveis:

| Para                                                | Defina                                                 | Onde                            |
| --------------------------------------------------- | ------------------------------------------------------ | ------------------------------- |
| Mover armazenamento para fora de `~/.claude`        | [`CLAUDE_CONFIG_DIR`](https://code.claude.com/docs/pt/env-vars)                    | Variável de ambiente            |
| Alterar a retenção de 30 dias                       | [`cleanupPeriodDays`](https://code.claude.com/docs/pt/settings#available-settings) | `settings.json`                 |
| Suprimir gravações de transcrição em todos os modos | [`CLAUDE_CODE_SKIP_PROMPT_HISTORY`](https://code.claude.com/docs/pt/env-vars)      | Variável de ambiente            |
| Suprimir gravações para uma execução não interativa | [`--no-session-persistence`](https://code.claude.com/docs/pt/cli-reference)        | Sinalizador CLI com `claude -p` |

<h2 id="see-also">
  Veja também
</h2>

Estas páginas cobrem mecânicas relacionadas de sessão e paralelismo:

* [Worktrees](https://code.claude.com/docs/pt/worktrees): execute sessões paralelas isoladas em branches separados
* [Checkpointing](https://code.claude.com/docs/pt/checkpointing): retroceda código e conversa para um ponto anterior
* [Janela de contexto](https://code.claude.com/docs/pt/context-window): o que preenche o contexto e o que sobrevive à compactação
* [Modo não interativo](https://code.claude.com/docs/pt/headless): comportamento de sessão sob `claude -p`
