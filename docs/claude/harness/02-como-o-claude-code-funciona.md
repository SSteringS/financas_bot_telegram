<!-- Fonte: https://code.claude.com/docs/pt/how-claude-code-works | Baixado em: 2026-08-06 | slug: how-claude-code-works -->

# Como Claude Code funciona

> Entenda o loop agentic, as ferramentas integradas e como Claude Code interage com seu projeto.

Claude Code é um assistente agentic que funciona em seu terminal. Embora se destaque em codificação, pode ajudar com qualquer coisa que você possa fazer a partir da linha de comando: escrever documentação, executar compilações, pesquisar arquivos, pesquisar tópicos e muito mais.

Este guia cobre a arquitetura principal, capacidades integradas e [dicas para trabalhar efetivamente](#work-effectively-with-claude-code). Para instruções passo a passo, consulte [Fluxos de trabalho comuns](https://code.claude.com/docs/pt/common-workflows). Para recursos de extensibilidade como skills, MCP e hooks, consulte [Estender Claude Code](https://code.claude.com/docs/pt/features-overview).

<h2 id="the-agentic-loop">
  O loop agentic
</h2>

Quando você dá uma tarefa a Claude, ele trabalha através de três fases: **reunir contexto**, **tomar ação** e **verificar resultados**. Essas fases se misturam. Claude usa ferramentas ao longo do processo, seja pesquisando arquivos para entender seu código, editando para fazer alterações ou executando testes para verificar seu trabalho.

<img src="https://mintcdn.com/claude-code/ikqp3_70mqIahteV/images/agentic-loop.svg?fit=max&auto=format&n=ikqp3_70mqIahteV&q=85&s=4a30fb7ce2815012a9f27c955e2c6bb0" alt="Diagrama do loop agentic: Seu prompt leva Claude a reunir contexto, tomar ação, verificar resultados e repetir até que a tarefa seja concluída. Você pode interromper em qualquer ponto." width="720" height="280" data-path="images/agentic-loop.svg" />

O loop se adapta ao que você pede. Uma pergunta sobre sua base de código pode precisar apenas de coleta de contexto. Uma correção de bug passa por todas as três fases repetidamente. Uma refatoração pode envolver verificação extensiva. Claude decide o que cada etapa requer com base no que aprendeu da etapa anterior, encadeando dezenas de ações e se autocorrigindo ao longo do caminho.

Você também faz parte deste loop. Você pode interromper em qualquer ponto para orientar Claude em uma direção diferente, fornecer contexto adicional ou pedir que tente uma abordagem diferente. Claude trabalha autonomamente, mas permanece responsivo à sua entrada.

O loop agentic é alimentado por dois componentes: [modelos](#models) que raciocinam e [ferramentas](#tools) que agem. Claude Code serve como o **agentic harness** ao redor de Claude: fornece as ferramentas, gerenciamento de contexto e ambiente de execução que transformam um modelo de linguagem em um agente de codificação capaz.

<h3 id="models">
  Models
</h3>

Claude Code usa modelos Claude para entender seu código e raciocinar sobre tarefas. Claude pode ler código em qualquer linguagem, entender como os componentes se conectam e descobrir o que precisa mudar para alcançar seu objetivo. Para tarefas complexas, ele divide o trabalho em etapas, as executa e se ajusta com base no que aprende.

[Múltiplos modelos](https://code.claude.com/docs/pt/model-config) estão disponíveis com diferentes compensações. Sonnet lida bem com a maioria das tarefas de codificação. Opus fornece raciocínio mais forte para decisões arquitetônicas complexas. Mude com `/model` durante uma sessão ou comece com `claude --model <name>`.

Quando este guia diz "Claude escolhe" ou "Claude decide", é o modelo fazendo o raciocínio.

<h3 id="tools">
  Tools
</h3>

Ferramentas são o que tornam Claude Code agentic. Sem ferramentas, Claude pode apenas responder com texto. Com ferramentas, Claude pode agir: ler seu código, editar arquivos, executar comandos, pesquisar a web e interagir com serviços externos. Cada uso de ferramenta retorna informações que alimentam o loop, informando a próxima decisão de Claude.

As ferramentas integradas geralmente se enquadram em cinco categorias, cada uma representando um tipo diferente de agência.

| Categoria                  | O que Claude pode fazer                                                                                                                                                    |
| -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Operações de arquivo**   | Ler arquivos, editar código, criar novos arquivos, renomear e reorganizar                                                                                                  |
| **Pesquisa**               | Encontrar arquivos por padrão, pesquisar conteúdo com regex, explorar bases de código                                                                                      |
| **Execução**               | Executar comandos shell, iniciar servidores, executar testes, usar git                                                                                                     |
| **Web**                    | Pesquisar a web, buscar documentação, procurar mensagens de erro                                                                                                           |
| **Inteligência de código** | Ver erros de tipo e avisos após edições, pular para definições, encontrar referências (requer [plugins de inteligência de código](https://code.claude.com/docs/pt/discover-plugins#code-intelligence)) |

Essas são as capacidades principais. Claude também tem ferramentas para gerar subagents, fazer perguntas a você e outras tarefas de orquestração. Consulte [Ferramentas disponíveis para Claude](https://code.claude.com/docs/pt/tools-reference) para a lista completa.

Claude escolhe quais ferramentas usar com base em seu prompt e no que aprende ao longo do caminho. Quando você diz "corrigir os testes falhando", Claude pode:

1. Executar o conjunto de testes para ver o que está falhando
2. Ler a saída de erro
3. Pesquisar os arquivos de código-fonte relevantes
4. Ler esses arquivos para entender o código
5. Editar os arquivos para corrigir o problema
6. Executar os testes novamente para verificar

Cada uso de ferramenta dá a Claude novas informações que informam a próxima etapa. Este é o loop agentic em ação.

**Estendendo as capacidades base:** As ferramentas integradas são a base. Você pode estender o que Claude sabe com [skills](https://code.claude.com/docs/pt/skills), conectar a serviços externos com [MCP](https://code.claude.com/docs/pt/mcp), automatizar fluxos de trabalho com [hooks](https://code.claude.com/docs/pt/hooks) e delegar tarefas a [subagents](https://code.claude.com/docs/pt/sub-agents). Essas extensões formam uma camada sobre o loop agentic principal. Consulte [Estender Claude Code](https://code.claude.com/docs/pt/features-overview) para orientação sobre como escolher a extensão certa para suas necessidades.

<h2 id="what-claude-can-access">
  O que Claude pode acessar
</h2>

Este guia se concentra no terminal. Claude Code também funciona em [VS Code](https://code.claude.com/docs/pt/vs-code), [IDEs JetBrains](https://code.claude.com/docs/pt/jetbrains) e outros ambientes.

Quando você executa `claude` em um diretório, Claude Code ganha acesso a:

* **Seu projeto.** Arquivos em seu diretório e subdiretórios, além de arquivos em outro lugar com sua permissão.
* **Seu terminal.** Qualquer comando que você possa executar: ferramentas de compilação, git, gerenciadores de pacotes, utilitários do sistema, scripts. Se você pode fazer a partir da linha de comando, Claude também pode.
* **Seu estado git.** Branch atual, alterações não confirmadas e histórico de commits recentes.
* **Seu [CLAUDE.md](https://code.claude.com/docs/pt/memory).** Um arquivo markdown onde você armazena instruções específicas do projeto, convenções e contexto que Claude deve conhecer a cada sessão.
* **[Auto memory](https://code.claude.com/docs/pt/memory#auto-memory).** Aprendizados que Claude salva automaticamente conforme você trabalha, como padrões de projeto e suas preferências. As primeiras 200 linhas ou 25KB de MEMORY.md, o que vier primeiro, são carregadas no início de cada sessão.
* **Extensões que você configura.** [Servidores MCP](https://code.claude.com/docs/pt/mcp) para serviços externos, [skills](https://code.claude.com/docs/pt/skills) para fluxos de trabalho, [subagents](https://code.claude.com/docs/pt/sub-agents) para trabalho delegado e [Claude no Chrome](https://code.claude.com/docs/pt/chrome) para interação com navegador.

Como Claude vê seu projeto inteiro, pode trabalhar em todo ele. Quando você pede a Claude para "corrigir o bug de autenticação", ele pesquisa arquivos relevantes, lê múltiplos arquivos para entender o contexto, faz edições coordenadas entre eles, executa testes para verificar a correção e confirma as alterações se você pedir. Isso é diferente de assistentes de código inline que apenas veem o arquivo atual.

<h2 id="environments-and-interfaces">
  Ambientes e interfaces
</h2>

O loop agentic, ferramentas e capacidades descritos acima são os mesmos em qualquer lugar que você use Claude Code. O que muda é onde o código é executado e como você interage com ele.

<h3 id="execution-environments">
  Ambientes de execução
</h3>

Claude Code funciona em três ambientes, cada um com diferentes compensações para onde seu código é executado.

| Ambiente           | Onde o código é executado                        | Caso de uso                                                            |
| ------------------ | ------------------------------------------------ | ---------------------------------------------------------------------- |
| **Local**          | Sua máquina                                      | Padrão. Acesso completo aos seus arquivos, ferramentas e ambiente      |
| **Cloud**          | VMs gerenciadas pela Anthropic                   | Delegar tarefas, trabalhar em repositórios que você não tem localmente |
| **Remote Control** | Sua máquina, controlada a partir de um navegador | Use a interface web mantendo tudo local                                |

<h3 id="interfaces">
  Interfaces
</h3>

Você pode acessar Claude Code através do terminal, do [aplicativo desktop](https://code.claude.com/docs/pt/desktop), [extensões IDE](https://code.claude.com/docs/pt/vs-code), [claude.ai/code](https://claude.ai/code), [Remote Control](https://code.claude.com/docs/pt/remote-control), [Slack](https://code.claude.com/docs/pt/slack) e [pipelines CI/CD](https://code.claude.com/docs/pt/github-actions). A interface determina como você vê e interage com Claude, mas o loop agentic subjacente é idêntico. Consulte [Use Claude Code em qualquer lugar](https://code.claude.com/docs/pt/overview#use-claude-code-everywhere) para a lista completa.

<h2 id="work-with-sessions">
  Trabalhe com sessões
</h2>

Claude Code salva sua conversa localmente conforme você trabalha. Cada mensagem, uso de ferramenta e resultado é escrito em um arquivo JSONL em texto simples sob `~/.claude/projects/`, o que permite [retroceder](#undo-changes-with-checkpoints), [retomar e bifurcar](#resume-or-fork-sessions) sessões. Antes de Claude fazer alterações de código, ele também tira um snapshot dos arquivos afetados para que você possa reverter se necessário. Para caminhos, retenção e como limpar esses dados, consulte [dados de aplicação em `~/.claude`](https://code.claude.com/docs/pt/claude-directory#application-data).

**As sessões são independentes.** Cada nova sessão começa com uma janela de contexto fresca, sem o histórico de conversa de sessões anteriores. Claude pode persistir aprendizados entre sessões usando [auto memory](https://code.claude.com/docs/pt/memory#auto-memory), e você pode adicionar suas próprias instruções persistentes em [CLAUDE.md](https://code.claude.com/docs/pt/memory).

<h3 id="work-across-branches">
  Trabalhe entre branches
</h3>

Cada conversa de Claude Code é uma sessão vinculada ao seu diretório atual. O seletor `/resume` mostra sessões do worktree atual por padrão, com atalhos de teclado para ampliar a lista para outros worktrees ou projetos. Consulte [Gerenciar sessões](https://code.claude.com/docs/pt/sessions#use-the-session-picker) para a lista completa de atalhos do seletor e como funciona a resolução de nomes.

Claude vê os arquivos do seu branch atual. Quando você muda de branch, Claude vê os arquivos do novo branch, mas seu histórico de conversa permanece o mesmo. Claude se lembra do que você discutiu mesmo após mudar de branch.

Como as sessões estão vinculadas a diretórios, você pode executar sessões paralelas de Claude Code usando [git worktrees](https://code.claude.com/docs/pt/worktrees), que criam diretórios separados para branches individuais.

<h3 id="resume-or-fork-sessions">
  Retome ou bifurque sessões
</h3>

Retomar uma sessão com `claude --continue` ou `claude --resume` reabre-a sob o mesmo ID de sessão e anexa novas mensagens à conversa existente. Bifurcar com `--fork-session` ou `/branch` copia o histórico em um novo ID de sessão, deixando o original inalterado.

<img src="https://mintcdn.com/claude-code/ikqp3_70mqIahteV/images/session-continuity.svg?fit=max&auto=format&n=ikqp3_70mqIahteV&q=85&s=04ed0984a58e4127e05b3640265241a3" alt="Continuidade de sessão: retomar continua a mesma sessão, bifurcar cria um novo branch com um novo ID." width="560" height="280" data-path="images/session-continuity.svg" />

Para as flags de retomada, o seletor `/resume`, nomeação e o que acontece quando a mesma sessão está aberta em dois terminais, consulte [Gerenciar sessões](https://code.claude.com/docs/pt/sessions).

<h3 id="the-context-window">
  A janela de contexto
</h3>

A janela de contexto de Claude contém seu histórico de conversa, conteúdo de arquivos, saídas de comando, [CLAUDE.md](https://code.claude.com/docs/pt/memory), [auto memory](https://code.claude.com/docs/pt/memory#auto-memory), skills carregadas e instruções do sistema. Conforme você trabalha, o contexto se enche. Claude compacta automaticamente, mas instruções do início da conversa podem ser perdidas. Coloque regras persistentes em CLAUDE.md e execute `/context` para ver o que está usando espaço.

Para um passo a passo interativo do que é carregado e quando, consulte [Explore a janela de contexto](https://code.claude.com/docs/pt/context-window).

<h4 id="when-context-fills-up">
  Quando o contexto se enche
</h4>

Claude Code gerencia o contexto automaticamente conforme você se aproxima do limite. Ele limpa saídas de ferramentas mais antigas primeiro, depois resume a conversa se necessário. Suas solicitações e trechos de código-chave são preservados; instruções detalhadas do início da conversa podem ser perdidas. Coloque regras persistentes em CLAUDE.md em vez de confiar no histórico de conversa.

Para controlar o que é preservado durante a compactação, adicione uma seção "Compact Instructions" a CLAUDE.md ou execute `/compact` com um foco (como `/compact focus on the API changes`).

Se um único arquivo ou saída de ferramenta for tão grande que o contexto se reencheça imediatamente após cada resumo, Claude Code para de compactar automaticamente após algumas tentativas e mostra um erro em vez de fazer loop. Consulte [Auto-compactação para com um erro de thrashing](https://code.claude.com/docs/pt/troubleshooting#auto-compaction-stops-with-a-thrashing-error) para etapas de recuperação.

Execute `/context` para ver o que está usando espaço. Definições de ferramentas MCP são adiadas por padrão e carregadas sob demanda via [busca de ferramentas](https://code.claude.com/docs/pt/mcp#scale-with-mcp-tool-search), então apenas nomes de ferramentas consomem contexto até Claude usar uma ferramenta específica. Execute `/mcp` para verificar custos por servidor.

<h4 id="manage-context-with-skills-and-subagents">
  Gerencie contexto com skills e subagents
</h4>

Além da compactação, você pode usar outros recursos para controlar o que é carregado no contexto.

[Skills](https://code.claude.com/docs/pt/skills) carregam sob demanda. Claude vê descrições de skills no início da sessão, mas o conteúdo completo só carrega quando uma skill é usada. Para skills que você invoca manualmente, defina `disable-model-invocation: true` para manter descrições fora do contexto até que você precise delas. Para skills que você não escreveu, use [`skillOverrides`](https://code.claude.com/docs/pt/skills#override-skill-visibility-from-settings) para fazer o mesmo a partir das configurações.

[Subagents](https://code.claude.com/docs/pt/sub-agents) obtêm seu próprio contexto fresco, completamente separado de sua conversa principal. Seu trabalho não incha seu contexto. Quando terminado, eles retornam um resumo. Esse isolamento é por que subagents ajudam em sessões longas.

Consulte [custos de contexto](https://code.claude.com/docs/pt/features-overview#understand-context-costs) para o que cada recurso custa e [reduzir uso de tokens](https://code.claude.com/docs/pt/costs#reduce-token-usage) para dicas sobre como gerenciar contexto.

<h2 id="stay-safe-with-checkpoints-and-permissions">
  Fique seguro com checkpoints e permissões
</h2>

Claude tem dois mecanismos de segurança: checkpoints permitem que você desfaça alterações de arquivo e permissões controlam o que Claude pode fazer sem perguntar.

<h3 id="undo-changes-with-checkpoints">
  Desfaça alterações com checkpoints
</h3>

**Cada edição de arquivo é reversível.** Antes de Claude editar qualquer arquivo, ele tira um snapshot do conteúdo atual. Se algo der errado, pressione `Esc` duas vezes para retroceder a um estado anterior, ou peça a Claude para desfazer.

Checkpoints são separados do git e permanecem disponíveis quando você retoma uma conversa. Eles cobrem apenas alterações de arquivo. Ações que afetam sistemas remotos (bancos de dados, APIs, implantações) não podem ser checkpointed, é por isso que Claude pergunta antes de executar comandos com efeitos colaterais externos.

<h3 id="control-what-claude-can-do">
  Controle o que Claude pode fazer
</h3>

Pressione `Shift+Tab` para percorrer os modos de permissão:

* **Manual**: Claude pergunta antes de edições de arquivo e comandos shell
* **Accept edits**: Claude edita arquivos e executa comandos comuns do sistema de arquivos como `mkdir` e `mv` sem perguntar, ainda pergunta por outros comandos
* **Plan**: Claude explora e propõe um plano sem editar seus arquivos de origem
* **Auto**: Claude avalia todas as ações com verificações de segurança em segundo plano

Você também pode permitir comandos específicos em `.claude/settings.json` para que Claude não pergunte cada vez. Isso é útil para comandos confiáveis como `npm test` ou `git status`. As configurações podem ser escopo de políticas em toda a organização até preferências pessoais. Consulte [Permissões](https://code.claude.com/docs/pt/permissions) para detalhes.

***

<h2 id="work-effectively-with-claude-code">
  Trabalhe efetivamente com Claude Code
</h2>

Essas dicas ajudam você a obter melhores resultados de Claude Code.

<h3 id="ask-claude-code-for-help">
  Peça ajuda a Claude Code
</h3>

Claude Code pode ensinar você como usá-lo. Faça perguntas como "como configuro hooks?" ou "qual é a melhor maneira de estruturar meu CLAUDE.md?" e Claude explicará.

Comandos integrados também o guiam através da configuração:

* `/init` o guia através da criação de um CLAUDE.md para seu projeto
* `/doctor` executa uma verificação de configuração que diagnostica problemas de instalação e configuração e pode corrigi-los

<h3 id="it’s-a-conversation">
  É uma conversa
</h3>

Claude Code é conversacional. Você não precisa de prompts perfeitos. Comece com o que você quer, depois refine:

```text theme={null}
Corrigir o bug de login
```

\[Claude investiga, tenta algo]

```text theme={null}
Isso não é bem certo. O problema está no tratamento de sessão.
```

\[Claude ajusta a abordagem]

Quando a primeira tentativa não está certa, você não começa do zero. Você itera.

<h4 id="interrupt-and-steer">
  Interrompa e oriente
</h4>

Você pode redirecionar Claude em qualquer ponto sem esperar que o turno termine ou começar do zero:

* **Pressione `Esc`** para parar Claude imediatamente. A chamada de ferramenta em execução é cancelada e Claude aguarda sua próxima instrução.
* **Digite uma correção e pressione `Enter`** para enviá-la sem parar a ferramenta em execução. Claude a lê assim que a ação atual é concluída e se ajusta antes de decidir seu próximo passo.

<h3 id="be-specific-upfront">
  Seja específico desde o início
</h3>

Quanto mais preciso seu prompt inicial, menos correções você precisará. Referencie arquivos específicos, mencione restrições e aponte para padrões de exemplo.

```text theme={null}
O fluxo de checkout está quebrado para usuários com cartões expirados.
Verifique src/payments/ para o problema, especialmente atualização de token.
Escreva um teste falhando primeiro, depois corrija.
```

Prompts vagos funcionam, mas você gastará mais tempo orientando. Prompts específicos como o acima geralmente têm sucesso na primeira tentativa.

<h3 id="give-claude-something-to-verify-against">
  Dê a Claude algo para verificar
</h3>

Claude funciona melhor quando pode verificar seu próprio trabalho. Inclua casos de teste, cole screenshots da UI esperada ou defina a saída que você quer.

```text theme={null}
Implementar validateEmail. Casos de teste: 'user@example.com' → true,
'invalid' → false, 'user@.com' → false. Execute os testes depois.
```

Para trabalho visual, cole um screenshot do design e peça a Claude para comparar sua implementação com ele.

<h3 id="explore-before-implementing">
  Explore antes de implementar
</h3>

Para problemas complexos, separe pesquisa de codificação. Use plan mode (`Shift+Tab` duas vezes) para analisar a base de código primeiro:

```text theme={null}
Leia src/auth/ e entenda como lidamos com sessões.
Depois crie um plano para adicionar suporte OAuth.
```

Revise o plano, refine-o através de conversa, depois deixe Claude implementar. Essa abordagem de duas fases produz melhores resultados do que pular direto para código.

<h3 id="delegate-don’t-dictate">
  Delegue, não dite
</h3>

Pense em delegar a um colega capaz. Dê contexto e direção, depois confie em Claude para descobrir os detalhes:

```text theme={null}
O fluxo de checkout está quebrado para usuários com cartões expirados.
O código relevante está em src/payments/. Você pode investigar e corrigir?
```

Você não precisa especificar quais arquivos ler ou quais comandos executar. Claude descobre isso.

<h2 id="what’s-next">
  O que vem a seguir
</h2>

<CardGroup cols={2}>
  <Card title="Estender com recursos" icon="puzzle-piece" href="/docs/pt/features-overview">
    Adicione Skills, conexões MCP e comandos personalizados
  </Card>

  <Card title="Fluxos de trabalho comuns" icon="graduation-cap" href="/docs/pt/common-workflows">
    Guias passo a passo para tarefas típicas
  </Card>
</CardGroup>
