<!-- Fonte: https://code.claude.com/docs/pt/common-workflows | Baixado em: 2026-08-06 | slug: common-workflows -->

# Fluxos de trabalho comuns

> Guias passo a passo para explorar bases de código, corrigir bugs, refatorar, testar e outras tarefas cotidianas com Claude Code.

Esta página coleta receitas curtas para desenvolvimento cotidiano. Para orientação de nível superior sobre prompting e gerenciamento de contexto, consulte [Melhores práticas](https://code.claude.com/docs/pt/best-practices).

Esta página aborda:

* [Receitas de prompt](#prompt-recipes) para explorar código, corrigir bugs, refatorar, testar, PRs e documentação
* [Retomar conversas anteriores](#resume-previous-conversations) para que uma tarefa possa abranger múltiplas sessões
* [Executar sessões paralelas com worktrees](#run-parallel-sessions-with-worktrees) para que edições simultâneas não colidam
* [Planejar antes de editar](#plan-before-editing) para revisar alterações antes de tocarem o disco
* [Delegar pesquisa para subagents](#delegate-research-to-subagents) para manter seu contexto principal limpo
* [Canalizar Claude em scripts](#pipe-claude-into-scripts) para CI e processamento em lote

<h2 id="prompt-recipes">
  Receitas de prompt
</h2>

Estes são padrões de prompt para tarefas cotidianas como explorar código desconhecido, depuração, refatoração, escrita de testes e criação de PRs. Cada um funciona em qualquer superfície do Claude Code; adapte a redação ao seu projeto.

<h3 id="understand-new-codebases">
  Entender novas bases de código
</h3>

Para configurar Claude Code em um monorepo ou base de código grande, consulte [Monorepos e repositórios grandes](https://code.claude.com/docs/pt/large-codebases).

<h4 id="get-a-quick-codebase-overview">
  Obter uma visão geral rápida da base de código
</h4>

Suponha que você acabou de ingressar em um novo projeto e precisa entender sua estrutura rapidamente.

<Steps>
  <Step title="Navegue até o diretório raiz do projeto">
    ```bash theme={null}
    cd /path/to/project 
    ```
  </Step>

  <Step title="Inicie Claude Code">
    ```bash theme={null}
    claude 
    ```
  </Step>

  <Step title="Peça uma visão geral de alto nível">
    ```text theme={null}
    give me an overview of this codebase
    ```
  </Step>

  <Step title="Aprofunde-se em componentes específicos">
    ```text theme={null}
    explain the main architecture patterns used here
    ```

    ```text theme={null}
    what are the key data models?
    ```

    ```text theme={null}
    how is authentication handled?
    ```
  </Step>
</Steps>

<Tip>
  Dicas:

  * Comece com perguntas amplas e depois estreite para áreas específicas
  * Pergunte sobre convenções de codificação e padrões usados no projeto
  * Solicite um glossário de termos específicos do projeto
</Tip>

<h4 id="find-relevant-code">
  Encontrar código relevante
</h4>

Suponha que você precise localizar código relacionado a um recurso ou funcionalidade específica.

<Steps>
  <Step title="Peça ao Claude para encontrar arquivos relevantes">
    ```text theme={null}
    find the files that handle user authentication
    ```
  </Step>

  <Step title="Obtenha contexto sobre como os componentes interagem">
    ```text theme={null}
    how do these authentication files work together?
    ```
  </Step>

  <Step title="Entenda o fluxo de execução">
    ```text theme={null}
    trace the login process from front-end to database
    ```
  </Step>
</Steps>

<Tip>
  Dicas:

  * Seja específico sobre o que você está procurando
  * Use linguagem de domínio do projeto
  * Instale um [plugin de inteligência de código](https://code.claude.com/docs/pt/discover-plugins#code-intelligence) para sua linguagem para dar ao Claude navegação precisa de "ir para definição" e "encontrar referências"
</Tip>

***

<h3 id="fix-bugs-efficiently">
  Corrigir bugs com eficiência
</h3>

Suponha que você tenha encontrado uma mensagem de erro e precise encontrar e corrigir sua origem.

<Steps>
  <Step title="Compartilhe o erro com Claude">
    ```text theme={null}
    I'm seeing an error when I run npm test
    ```
  </Step>

  <Step title="Peça recomendações de correção">
    ```text theme={null}
    suggest a few ways to fix the @ts-ignore in user.ts
    ```
  </Step>

  <Step title="Aplique a correção">
    ```text theme={null}
    update user.ts to add the null check you suggested
    ```
  </Step>
</Steps>

<Tip>
  Dicas:

  * Diga ao Claude o comando para reproduzir o problema e obtenha um rastreamento de pilha
  * Mencione quaisquer etapas para reproduzir o erro
  * Deixe Claude saber se o erro é intermitente ou consistente
</Tip>

***

<h3 id="refactor-code">
  Refatorar código
</h3>

Suponha que você precise atualizar código antigo para usar padrões e práticas modernas.

<Steps>
  <Step title="Identifique código legado para refatoração">
    ```text theme={null}
    find deprecated API usage in our codebase
    ```
  </Step>

  <Step title="Obtenha recomendações de refatoração">
    ```text theme={null}
    suggest how to refactor utils.js to use modern JavaScript features
    ```
  </Step>

  <Step title="Aplique as alterações com segurança">
    ```text theme={null}
    refactor utils.js to use ES2024 features while maintaining the same behavior
    ```
  </Step>

  <Step title="Verifique a refatoração">
    ```text theme={null}
    run tests for the refactored code
    ```
  </Step>
</Steps>

<Tip>
  Dicas:

  * Peça ao Claude para explicar os benefícios da abordagem moderna
  * Solicite que as alterações mantenham compatibilidade com versões anteriores quando necessário
  * Faça refatoração em pequenos incrementos testáveis
</Tip>

***

<h3 id="work-with-tests">
  Trabalhar com testes
</h3>

Suponha que você precise adicionar testes para código não coberto.

<Steps>
  <Step title="Identifique código não testado">
    ```text theme={null}
    find functions in NotificationsService.swift that are not covered by tests
    ```
  </Step>

  <Step title="Gere scaffolding de teste">
    ```text theme={null}
    add tests for the notification service
    ```
  </Step>

  <Step title="Adicione casos de teste significativos">
    ```text theme={null}
    add test cases for edge conditions in the notification service
    ```
  </Step>

  <Step title="Execute e verifique os testes">
    ```text theme={null}
    run the new tests and fix any failures
    ```
  </Step>
</Steps>

Claude pode gerar testes que seguem os padrões e convenções existentes do seu projeto. Ao solicitar testes, seja específico sobre qual comportamento você quer verificar. Claude examina seus arquivos de teste existentes para corresponder ao estilo, frameworks e padrões de asserção já em uso.

Para cobertura abrangente, peça ao Claude para identificar casos extremos que você pode ter perdido. Claude pode analisar seus caminhos de código e sugerir testes para condições de erro, valores de limite e entradas inesperadas que são fáceis de negligenciar.

***

<h3 id="create-pull-requests">
  Criar pull requests
</h3>

Você pode criar pull requests pedindo ao Claude diretamente ("create a pr for my changes"), ou guiar Claude através disso passo a passo:

<Steps>
  <Step title="Resuma suas alterações">
    ```text theme={null}
    summarize the changes I've made to the authentication module
    ```
  </Step>

  <Step title="Gere uma pull request">
    ```text theme={null}
    create a pr
    ```
  </Step>

  <Step title="Revise e refine">
    ```text theme={null}
    enhance the PR description with more context about the security improvements
    ```
  </Step>
</Steps>

Quando você cria uma PR usando `gh pr create`, a sessão é automaticamente vinculada a essa PR. Para retornar a ela mais tarde, execute `claude --from-pr 123`, substituindo 123 pelo número da PR, ou cole a URL da PR no seletor [`/resume`](https://code.claude.com/docs/pt/sessions#use-the-session-picker).

<Tip>
  Revise a PR gerada por Claude antes de enviar e peça ao Claude para destacar riscos ou considerações potenciais.
</Tip>

<h3 id="handle-documentation">
  Lidar com documentação
</h3>

Suponha que você precise adicionar ou atualizar documentação para seu código.

<Steps>
  <Step title="Identifique código não documentado">
    ```text theme={null}
    find functions without proper JSDoc comments in the auth module
    ```
  </Step>

  <Step title="Gere documentação">
    ```text theme={null}
    add JSDoc comments to the undocumented functions in auth.js
    ```
  </Step>

  <Step title="Revise e melhore">
    ```text theme={null}
    improve the generated documentation with more context and examples
    ```
  </Step>

  <Step title="Verifique a documentação">
    ```text theme={null}
    check if the documentation follows our project standards
    ```
  </Step>
</Steps>

<Tip>
  Dicas:

  * Especifique o estilo de documentação que você deseja (JSDoc, docstrings, etc.)
  * Peça por exemplos na documentação
  * Solicite documentação para APIs públicas, interfaces e lógica complexa
</Tip>

***

<h3 id="work-in-notes-and-non-code-folders">
  Trabalhar em notas e pastas não-código
</h3>

Claude Code funciona em qualquer diretório. Execute-o dentro de um cofre de notas, uma pasta de documentação ou qualquer coleção de arquivos markdown para pesquisar, editar e reorganizar conteúdo da mesma forma que você faria com código.

O diretório `.claude/` e `CLAUDE.md` ficam ao lado dos diretórios de configuração de outras ferramentas sem conflito. Claude lê arquivos novamente em cada chamada de ferramenta, então vê edições que você faz em outro aplicativo na próxima vez que lê esse arquivo.

***

<h3 id="work-with-images">
  Trabalhar com imagens
</h3>

Suponha que você precise trabalhar com imagens em sua base de código e queira ajuda do Claude para analisar o conteúdo da imagem.

<Steps>
  <Step title="Adicione uma imagem à conversa">
    Você pode usar qualquer um destes métodos:

    1. Arraste e solte uma imagem na janela do Claude Code
    2. Copie uma imagem e cole-a no CLI com Ctrl+V. No macOS, Cmd+V também funciona no iTerm2.
    3. Forneça um caminho de imagem ao Claude. Por exemplo, "Analyze this image: /path/to/your/image.png"
  </Step>

  <Step title="Peça ao Claude para analisar a imagem">
    ```text theme={null}
    What does this image show?
    ```

    ```text theme={null}
    Describe the UI elements in this screenshot
    ```

    ```text theme={null}
    Are there any problematic elements in this diagram?
    ```
  </Step>

  <Step title="Use imagens para contexto">
    ```text theme={null}
    Here's a screenshot of the error. What's causing it?
    ```

    ```text theme={null}
    This is our current database schema. How should we modify it for the new feature?
    ```
  </Step>

  <Step title="Obtenha sugestões de código do conteúdo visual">
    ```text theme={null}
    Generate CSS to match this design mockup
    ```

    ```text theme={null}
    What HTML structure would recreate this component?
    ```
  </Step>
</Steps>

<Tip>
  Dicas:

  * Use imagens quando descrições de texto seriam pouco claras ou complicadas
  * Inclua capturas de tela de erros, designs de UI ou diagramas para melhor contexto
  * Você pode trabalhar com múltiplas imagens em uma conversa
  * A análise de imagem funciona com diagramas, capturas de tela, mockups e muito mais
  * Quando Claude referencia imagens (por exemplo, `[Image #1]`), `Cmd+Click` (Mac) ou `Ctrl+Click` (Windows/Linux) o link para abrir a imagem em seu visualizador padrão
</Tip>

***

<h3 id="reference-files-and-directories">
  Referenciar arquivos e diretórios
</h3>

Use @ para incluir rapidamente arquivos ou diretórios sem esperar que Claude os leia.

<Steps>
  <Step title="Referencie um único arquivo">
    ```text theme={null}
    Explain the logic in @src/utils/auth.js
    ```

    Isso inclui o conteúdo completo do arquivo na conversa.
  </Step>

  <Step title="Referencie um diretório">
    ```text theme={null}
    What's the structure of @src/components?
    ```

    Isso fornece uma listagem de diretório com informações de arquivo.
  </Step>

  <Step title="Referencie recursos MCP">
    ```text theme={null}
    Show me the data from @github:repos/owner/repo/issues
    ```

    Isso busca dados de servidores MCP conectados usando o formato @server:resource. Consulte [recursos MCP](https://code.claude.com/docs/pt/mcp#use-mcp-resources) para detalhes.
  </Step>
</Steps>

<Tip>
  Dicas:

  * Os caminhos de arquivo podem ser relativos ou absolutos
  * Referências de arquivo @ adicionam `CLAUDE.md` no diretório do arquivo e diretórios pai ao contexto
  * Referências de diretório mostram listagens de arquivo, não conteúdos
  * Você pode referenciar múltiplos arquivos em uma única mensagem (por exemplo, "@file1.js and @file2.js")
</Tip>

***

<h3 id="run-claude-on-a-schedule">
  Executar Claude em um cronograma
</h3>

Suponha que você queira que Claude lide com uma tarefa automaticamente em uma base recorrente, como revisar PRs abertas todas as manhãs, auditar dependências semanalmente ou verificar falhas de CI durante a noite.

Escolha uma opção de agendamento com base em onde você quer que a tarefa seja executada:

| Opção                                                       | Onde é executado                         | Melhor para                                                                                                                                                                                                                                      |
| :---------------------------------------------------------- | :--------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [Rotinas](https://code.claude.com/docs/pt/routines)                                     | Infraestrutura gerenciada pela Anthropic | Tarefas que devem ser executadas mesmo quando seu computador está desligado. Também podem ser acionadas por chamadas de API ou eventos do GitHub além de um cronograma. Configure em [claude.ai/code/routines](https://claude.ai/code/routines). |
| [Tarefas agendadas no desktop](https://code.claude.com/docs/pt/desktop-scheduled-tasks) | Sua máquina, via aplicativo desktop      | Tarefas que precisam de acesso direto a arquivos locais, ferramentas ou alterações não confirmadas.                                                                                                                                              |
| [GitHub Actions](https://code.claude.com/docs/pt/github-actions)                        | Seu pipeline de CI                       | Tarefas vinculadas a eventos de repositório como PRs abertos, ou cronogramas cron que devem viver junto com sua configuração de fluxo de trabalho.                                                                                               |
| [`/loop`](https://code.claude.com/docs/pt/scheduled-tasks)                              | A sessão CLI atual                       | Polling rápido enquanto uma sessão está aberta. As tarefas são canceladas quando você inicia uma nova conversa; `--resume` e `--continue` restauram as não expiradas.                                                                            |

<Tip>
  Ao escrever prompts para tarefas agendadas, seja explícito sobre o que o sucesso parece e o que fazer com os resultados. A tarefa é executada autonomamente, então não pode fazer perguntas de esclarecimento. Por exemplo: "Review open PRs labeled `needs-review`, leave inline comments on any issues, and post a summary in the `#eng-reviews` Slack channel."
</Tip>

***

<h3 id="ask-claude-about-its-capabilities">
  Pergunte ao Claude sobre suas capacidades
</h3>

Claude tem acesso integrado à sua documentação e pode responder perguntas sobre seus próprios recursos e limitações.

<h4 id="example-questions">
  Perguntas de exemplo
</h4>

```text theme={null}
can Claude Code create pull requests?
```

```text theme={null}
how does Claude Code handle permissions?
```

```text theme={null}
what skills are available?
```

```text theme={null}
how do I use MCP with Claude Code?
```

```text theme={null}
how do I configure Claude Code for Amazon Bedrock?
```

```text theme={null}
what are the limitations of Claude Code?
```

<Note>
  Claude fornece respostas baseadas em documentação para essas perguntas. Para demonstrações práticas, execute `/powerup` para lições interativas com demos animadas, ou consulte as seções de fluxo de trabalho específicas acima.
</Note>

<Tip>
  Dicas:

  * Claude sempre tem acesso à documentação mais recente do Claude Code, independentemente da versão que você está usando
  * Faça perguntas específicas para obter respostas detalhadas
  * Claude pode explicar recursos complexos como integração MCP, configurações empresariais e fluxos de trabalho avançados
</Tip>

***

<h2 id="resume-previous-conversations">
  Retomar conversas anteriores
</h2>

Quando uma tarefa abrange múltiplas sessões, retome de onde parou em vez de re-explicar o contexto. Claude Code salva cada conversa localmente.

```bash theme={null}
claude --continue
```

Isso retoma a sessão mais recente no diretório atual; se não houver uma ainda, ele imprime `No conversation found to continue` e sai. Use `claude --resume` para escolher de uma lista, ou `/resume` de dentro de uma sessão em execução. Consulte [Gerenciar sessões](https://code.claude.com/docs/pt/sessions) para nomeação, ramificação e referência completa do seletor.

<h2 id="run-parallel-sessions-with-worktrees">
  Executar sessões paralelas com worktrees
</h2>

Trabalhe em um recurso em um terminal enquanto Claude corrige um bug em outro, sem que as edições colidam. Cada worktree é um checkout separado em seu próprio branch.

```bash theme={null}
claude --worktree feature-auth
```

Execute o mesmo comando com um nome diferente em um segundo terminal para iniciar uma sessão paralela isolada. Consulte [Worktrees](https://code.claude.com/docs/pt/worktrees) para limpeza, `.worktreeinclude` e suporte a VCS não-git. Para monitorar sessões paralelas de uma tela em vez de terminais separados, consulte [agentes em segundo plano](https://code.claude.com/docs/pt/agent-view).

<h2 id="plan-before-editing">
  Planejar antes de editar
</h2>

Para alterações que você quer revisar antes de tocarem o disco, mude para plan mode. Claude lê arquivos e propõe um plano, mas não faz edições até que você aprove.

```bash theme={null}
claude --permission-mode plan
```

Você também pode pressionar `Shift+Tab` durante uma sessão para alternar para plan mode. Consulte [Plan mode](https://code.claude.com/docs/pt/permission-modes#analyze-before-you-edit-with-plan-mode) para o fluxo de aprovação e edição do plano em seu editor de texto.

<h2 id="delegate-research-to-subagents">
  Delegar pesquisa para subagents
</h2>

Explorar uma base de código grande preenche seu contexto com leituras de arquivo. Delegue a exploração para que apenas os achados retornem.

```text theme={null}
use a subagent to investigate how our auth system handles token refresh
```

O subagent lê arquivos em sua própria janela de contexto e relata um resumo. Consulte [Subagents](https://code.claude.com/docs/pt/sub-agents) para definir agentes personalizados com suas próprias ferramentas e prompts.

<h2 id="pipe-claude-into-scripts">
  Canalizar Claude em scripts
</h2>

Execute Claude de forma não interativa para CI, hooks de pré-commit ou processamento em lote. Stdin e stdout funcionam como qualquer ferramenta Unix.

```bash theme={null}
git log --oneline -20 | claude -p "summarize these recent commits"
```

Consulte [Modo não interativo](https://code.claude.com/docs/pt/headless) para formatos de saída, flags de permissão e padrões de fan-out.

<h2 id="next-steps">
  Próximos passos
</h2>

<CardGroup cols={2}>
  <Card title="Melhores práticas" icon="lightbulb" href="/docs/pt/best-practices">
    Padrões para aproveitar ao máximo Claude Code
  </Card>

  <Card title="Gerenciar sessões" icon="rotate-left" href="/docs/pt/sessions">
    Retomar, nomear e ramificar conversas
  </Card>

  <Card title="Worktrees" icon="code-branch" href="/docs/pt/worktrees">
    Executar sessões paralelas isoladas
  </Card>

  <Card title="Estender Claude Code" icon="puzzle-piece" href="/docs/pt/features-overview">
    Adicionar skills, hooks, MCP, subagents e plugins
  </Card>
</CardGroup>
