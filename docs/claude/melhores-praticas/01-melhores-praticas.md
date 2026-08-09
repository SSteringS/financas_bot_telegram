<!-- Fonte: https://code.claude.com/docs/pt/best-practices | Baixado em: 2026-08-06 | slug: best-practices -->

# Melhores práticas para Claude Code

> Dicas e padrões para aproveitar ao máximo o Claude Code, desde a configuração do seu ambiente até o dimensionamento em sessões paralelas.

Claude Code é um ambiente de codificação agentic. Diferentemente de um chatbot que responde perguntas e espera, Claude Code pode ler seus arquivos, executar comandos, fazer alterações e trabalhar autonomamente através de problemas enquanto você observa, redireciona ou se afasta completamente.

Isso muda a forma como você trabalha. Em vez de escrever código você mesmo e pedir ao Claude para revisá-lo, você descreve o que deseja e Claude descobre como construir. Claude explora, planeja e implementa.

Mas essa autonomia ainda vem com uma curva de aprendizado. Claude trabalha dentro de certas restrições que você precisa entender.

Este guia cobre padrões que se mostraram eficazes nas equipes internas da Anthropic e para engenheiros usando Claude Code em vários codebases, linguagens e ambientes. Para saber como o loop agentic funciona nos bastidores, consulte [How Claude Code works](https://code.claude.com/docs/pt/how-claude-code-works).

***

A maioria das melhores práticas é baseada em uma restrição: a janela de contexto do Claude se enche rapidamente e o desempenho se degrada conforme ela se enche.

A janela de contexto do Claude contém toda a sua conversa, incluindo cada mensagem, cada arquivo que Claude lê e cada saída de comando. No entanto, isso pode se encher rapidamente. Uma única sessão de depuração ou exploração de codebase pode gerar e consumir dezenas de milhares de tokens.

Isso importa porque o desempenho do LLM se degrada conforme o contexto se enche. Quando a janela de contexto está ficando cheia, Claude pode começar a "esquecer" instruções anteriores ou cometer mais erros. A janela de contexto é o recurso mais importante a gerenciar. Para ver como uma sessão se enche na prática, [assista a um passo a passo interativo](https://code.claude.com/docs/pt/context-window) do que é carregado na inicialização e quanto cada leitura de arquivo custa. Rastreie o uso de contexto continuamente com uma [custom status line](https://code.claude.com/docs/pt/statusline), e veja [Reduce token usage](https://code.claude.com/docs/pt/costs#reduce-token-usage) para estratégias de redução do uso de tokens.

***

<h2 id="give-claude-a-way-to-verify-its-work">
  Dê ao Claude uma forma de verificar seu trabalho
</h2>

<Tip>
  Dê ao Claude uma verificação que ele possa executar: testes, uma compilação, uma captura de tela para comparar. É a diferença entre uma sessão que você observa e uma que você pode deixar sozinha.
</Tip>

Claude para quando o trabalho parece pronto. Sem uma verificação que ele possa executar, "parece pronto" é o único sinal disponível, e você se torna o loop de verificação: cada erro espera por você notar. Dê ao Claude algo que produz um resultado de sucesso ou falha, e o loop se fecha por conta própria. Claude faz o trabalho, executa a verificação, lê o resultado e itera até que a verificação passe.

A verificação é qualquer coisa que retorna um sinal que Claude pode ler na conversa: um conjunto de testes, um código de saída de compilação, um linter, um script que compara a saída com um fixture, ou uma [captura de tela do navegador](https://code.claude.com/docs/pt/chrome) comparada com um design.

| Estratégia                                 | Antes                                                   | Depois                                                                                                                                                                                                                  |
| ------------------------------------------ | ------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Forneça critérios de verificação**       | *"implemente uma função que valida endereços de email"* | *"escreva uma função validateEmail. exemplos de casos de teste: [user@example.com](mailto:user@example.com) é verdadeiro, inválido é falso, [user@.com](mailto:user@.com) é falso. execute os testes após implementar"* |
| **Verifique mudanças de UI visualmente**   | *"faça o dashboard parecer melhor"*                     | *"\[cole captura de tela] implemente este design. tire uma captura de tela do resultado e compare com o original. liste as diferenças e corrija-as"*                                                                    |
| **Aborde as causas raiz, não os sintomas** | *"a compilação está falhando"*                          | *"a compilação falha com este erro: \[cole erro]. corrija-o e verifique se a compilação é bem-sucedida. aborde a causa raiz, não suprima o erro"*                                                                       |

Depois que a verificação existe, decida o quão rigorosamente ela controla a parada:

* **Em um único prompt**: peça ao Claude para executar a verificação e iterar na mesma mensagem, como na tabela acima.
* **Em toda uma sessão**: defina a verificação como uma [condição `/goal`](https://code.claude.com/docs/pt/goal). Um avaliador separado a verifica novamente após cada turno e Claude continua trabalhando até que ela seja atendida.
* **Como um gate determinístico**: um [hook Stop](https://code.claude.com/docs/pt/hooks#stop) executa sua verificação como um script e bloqueia o turno de terminar até que passe. Claude Code substitui o hook e termina o turno após 8 bloqueios consecutivos.
* **Por uma segunda opinião**: um [subagente de verificação](https://code.claude.com/docs/pt/sub-agents) ou um [fluxo de trabalho dinâmico](https://code.claude.com/docs/pt/workflows) que verifica suas próprias descobertas tem um modelo fresco tentando refutar o resultado, para que o agente que faz o trabalho não seja o que o avalia.

Cada etapa troca configuração por atenção. A versão de prompt funciona em qualquer tarefa hoje. As versões `/goal` e Stop hook são o que permite que uma execução desatendida termine corretamente sem você.

Tenha Claude mostrar evidências em vez de afirmar sucesso: a saída do teste, o comando que executou e o que retornou, ou uma captura de tela do resultado. Revisar evidências é mais rápido do que executar novamente a verificação você mesmo, e funciona para sessões que você não estava observando.

***

<h2 id="explore-first-then-plan-then-code">
  Explore primeiro, depois planeje, depois codifique
</h2>

<Tip>
  Separe pesquisa e planejamento da implementação para evitar resolver o problema errado.
</Tip>

Deixar Claude pular direto para codificação pode produzir código que resolve o problema errado. Use [plan mode](https://code.claude.com/docs/pt/permission-modes#analyze-before-you-edit-with-plan-mode) para separar exploração de execução.

O fluxo de trabalho recomendado tem quatro fases:

<Steps>
  <Step title="Explore">
    Entre em plan mode. Claude lê arquivos e responde perguntas sem fazer alterações.

    ```txt claude (plan mode) theme={null}
    read /src/auth and understand how we handle sessions and login.
    also look at how we manage environment variables for secrets.
    ```
  </Step>

  <Step title="Plan">
    Peça ao Claude para criar um plano de implementação detalhado.

    ```txt claude (plan mode) theme={null}
    I want to add Google OAuth. What files need to change?
    What's the session flow? Create a plan.
    ```

    Pressione `Ctrl+G` para abrir o plano no seu editor de texto para edição direta antes de Claude prosseguir.
  </Step>

  <Step title="Implement">
    Saia de plan mode e deixe Claude codificar, verificando contra seu plano.

    ```txt claude (default mode) theme={null}
    implement the OAuth flow from your plan. write tests for the
    callback handler, run the test suite and fix any failures.
    ```
  </Step>

  <Step title="Commit">
    Peça ao Claude para fazer commit com uma mensagem descritiva e criar um PR.

    ```txt claude (default mode) theme={null}
    commit with a descriptive message and open a PR
    ```
  </Step>
</Steps>

<Callout>
  Plan mode é útil, mas também adiciona sobrecarga.

  Para tarefas onde o escopo é claro e a correção é pequena (como corrigir um erro de digitação, adicionar uma linha de log ou renomear uma variável) peça ao Claude para fazer isso diretamente.

  O planejamento é mais útil quando você está incerto sobre a abordagem, quando a mudança modifica vários arquivos ou quando você não está familiarizado com o código sendo modificado. Se você pudesse descrever o diff em uma frase, pule o plano.
</Callout>

***

<h2 id="provide-specific-context-in-your-prompts">
  Forneça contexto específico em seus prompts
</h2>

<Tip>
  Quanto mais precisas suas instruções, menos correções você precisará.
</Tip>

Claude pode inferir intenção, mas não pode ler sua mente. Referencie arquivos específicos, mencione restrições e aponte para padrões de exemplo.

| Estratégia                                                                                      | Antes                                                  | Depois                                                                                                                                                                                                                                                                                                                                                      |
| ----------------------------------------------------------------------------------------------- | ------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Escopo a tarefa.** Especifique qual arquivo, qual cenário e preferências de teste.            | *"adicione testes para foo.py"*                        | *"escreva um teste para foo.py cobrindo o caso extremo onde o usuário está desconectado. evite mocks."*                                                                                                                                                                                                                                                     |
| **Aponte para fontes.** Dirija Claude para a fonte que pode responder uma pergunta.             | *"por que ExecutionFactory tem uma API tão estranha?"* | *"procure no histórico git do ExecutionFactory e resuma como sua API chegou a ser assim"*                                                                                                                                                                                                                                                                   |
| **Referencie padrões existentes.** Aponte Claude para padrões em seu codebase.                  | *"adicione um widget de calendário"*                   | *"veja como os widgets existentes são implementados na página inicial para entender os padrões. HotDogWidget.php é um bom exemplo. siga o padrão para implementar um novo widget de calendário que permite ao usuário selecionar um mês e paginar para frente/trás para escolher um ano. construa do zero sem bibliotecas além das já usadas no codebase."* |
| **Descreva o sintoma.** Forneça o sintoma, a localização provável e como "corrigido" se parece. | *"corrija o bug de login"*                             | *"usuários relatam que o login falha após timeout de sessão. verifique o fluxo de autenticação em src/auth/, especialmente atualização de token. escreva um teste falhando que reproduz o problema, depois corrija-o"*                                                                                                                                      |

Prompts vagos podem ser úteis quando você está explorando e pode se dar ao luxo de corrigir o curso. Um prompt como `"o que você melhoraria neste arquivo?"` pode revelar coisas que você não teria pensado em perguntar.

<h3 id="provide-rich-content">
  Forneça conteúdo rico
</h3>

<Tip>
  Use `@` para referenciar arquivos, cole capturas de tela/imagens ou canalize dados diretamente.
</Tip>

Você pode fornecer dados ricos ao Claude de várias maneiras:

* **Referencie arquivos com `@`** em vez de descrever onde o código vive. Claude lê o arquivo antes de responder.
* **Cole imagens diretamente**. Copie/cole ou arraste e solte imagens no prompt.
* **Forneça URLs** para documentação e referências de API. Use `/permissions` para colocar na lista de permissões domínios frequentemente usados.
* **Canalize dados** executando `cat error.log | claude` para enviar conteúdos de arquivo diretamente.
* **Deixe Claude buscar o que precisa**. Diga ao Claude para puxar contexto ele mesmo usando comandos Bash, ferramentas MCP ou lendo arquivos.

***

<h2 id="configure-your-environment">
  Configure seu ambiente
</h2>

Alguns passos de configuração tornam Claude Code significativamente mais eficaz em todas as suas sessões. Para uma visão geral completa dos recursos de extensão e quando usar cada um, consulte [Extend Claude Code](https://code.claude.com/docs/pt/features-overview).

<h3 id="write-an-effective-claude-md">
  Escreva um CLAUDE.md eficaz
</h3>

<Tip>
  Execute `/init` para gerar um arquivo CLAUDE.md inicial baseado na estrutura do seu projeto atual, depois refine ao longo do tempo.
</Tip>

CLAUDE.md é um arquivo especial que Claude lê no início de cada conversa. Inclua comandos Bash, estilo de código e regras de fluxo de trabalho. Isso dá ao Claude contexto persistente que ele não pode inferir apenas do código.

O comando `/init` analisa seu codebase para detectar sistemas de compilação, frameworks de teste e padrões de código, dando a você uma base sólida para refinar.

Não há formato obrigatório para arquivos CLAUDE.md, mas mantenha-o curto e legível para humanos. Por exemplo:

```markdown CLAUDE.md theme={null}
# Code style
- Use ES modules (import/export) syntax, not CommonJS (require)
- Destructure imports when possible (eg. import { foo } from 'bar')

# Workflow
- Be sure to typecheck when you're done making a series of code changes
- Prefer running single tests, and not the whole test suite, for performance
```

CLAUDE.md é carregado a cada sessão, então inclua apenas coisas que se aplicam amplamente. Para conhecimento de domínio ou fluxos de trabalho que são apenas relevantes às vezes, use [skills](https://code.claude.com/docs/pt/skills) em vez disso. Claude os carrega sob demanda sem inchar cada conversa.

Mantenha-o conciso. Para cada linha, pergunte: *"Remover isso causaria Claude cometer erros?"* Se não, corte. Arquivos CLAUDE.md inchados causam Claude ignorar suas instruções reais!

| ✅ Inclua                                                                 | ❌ Exclua                                                    |
| ------------------------------------------------------------------------ | ----------------------------------------------------------- |
| Comandos Bash que Claude não pode adivinhar                              | Qualquer coisa que Claude possa descobrir lendo código      |
| Regras de estilo de código que diferem dos padrões                       | Convenções de linguagem padrão que Claude já conhece        |
| Instruções de teste e executores de teste preferidos                     | Documentação detalhada de API (link para docs em vez disso) |
| Etiqueta de repositório (nomenclatura de branch, convenções de PR)       | Informações que mudam frequentemente                        |
| Decisões arquitetônicas específicas do seu projeto                       | Explicações longas ou tutoriais                             |
| Peculiaridades do ambiente de desenvolvedor (variáveis env obrigatórias) | Descrições arquivo por arquivo do codebase                  |
| Armadilhas comuns ou comportamentos não óbvios                           | Práticas auto-evidentes como "escreva código limpo"         |

Se Claude continua fazendo algo que você não quer apesar de ter uma regra contra isso, o arquivo provavelmente é muito longo e a regra está sendo perdida. Se Claude faz perguntas que são respondidas em CLAUDE.md, a redação pode ser ambígua. Trate CLAUDE.md como código: revise-o quando as coisas dão errado, poda-o regularmente e teste mudanças observando se o comportamento do Claude realmente muda.

Você pode ajustar instruções adicionando ênfase (por exemplo, "IMPORTANTE" ou "VOCÊ DEVE") para melhorar a adesão. Verifique CLAUDE.md no git para que sua equipe possa contribuir. O arquivo aumenta em valor ao longo do tempo.

Arquivos CLAUDE.md podem importar arquivos adicionais usando a sintaxe `@path/to/import`:

```markdown CLAUDE.md theme={null}
See @README.md for project overview and @package.json for available npm commands.

# Additional Instructions
- Git workflow: @docs/git-instructions.md
- Personal overrides: @~/.claude/my-project-instructions.md
```

Você pode colocar arquivos CLAUDE.md em vários locais:

* **Pasta home (`~/.claude/CLAUDE.md`)**: aplica-se a todas as sessões Claude
* **Raiz do projeto (`./CLAUDE.md`)**: verifique no git para compartilhar com sua equipe
* **Raiz do projeto (`./CLAUDE.local.md`)**: notas pessoais específicas do projeto; adicione este arquivo ao seu `.gitignore` para que não seja compartilhado com sua equipe
* **Diretórios pai**: útil para monorepos onde tanto `root/CLAUDE.md` quanto `root/foo/CLAUDE.md` são puxados automaticamente
* **Diretórios filhos**: Claude puxa arquivos CLAUDE.md filhos sob demanda ao trabalhar com arquivos nesses diretórios

<h3 id="configure-permissions">
  Configure permissões
</h3>

<Tip>
  Use [auto mode](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode) para deixar um classificador lidar com aprovações, `/permissions` para colocar na lista de permissões comandos específicos, ou `/sandbox` para isolamento em nível de SO. Cada um reduz interrupções enquanto mantém você no controle.
</Tip>

Por padrão, Claude Code solicita permissão para ações que podem modificar seu sistema: gravações de arquivo, comandos Bash, ferramentas MCP, etc. Isso é seguro mas tedioso. Após a décima aprovação você realmente não está revisando mais, você está apenas clicando. Existem três maneiras de reduzir essas interrupções:

* **Auto mode**: um modelo classificador separado revisa comandos e bloqueia apenas o que parece arriscado: escalação de escopo, infraestrutura desconhecida ou ações impulsionadas por conteúdo hostil. Melhor quando você confia na direção geral de uma tarefa mas não quer clicar em cada passo
* **Listas de permissões**: permita ferramentas específicas que você sabe que são seguras, como `npm run lint` ou `git commit`
* **Sandboxing**: ative isolamento em nível de SO que restringe acesso ao sistema de arquivos e rede, permitindo Claude trabalhar mais livremente dentro de limites definidos

Leia mais sobre [permission modes](https://code.claude.com/docs/pt/permission-modes), [permission rules](https://code.claude.com/docs/pt/permissions) e [sandboxing](https://code.claude.com/docs/pt/sandboxing).

<h3 id="use-cli-tools">
  Use ferramentas CLI
</h3>

<Tip>
  Diga ao Claude Code para usar ferramentas CLI como `gh`, `aws`, `gcloud` e `sentry-cli` ao interagir com serviços externos.
</Tip>

Ferramentas CLI são a forma mais eficiente em contexto de interagir com serviços externos. Se você usa GitHub, instale o CLI `gh`. Claude sabe como usá-lo para criar issues, abrir pull requests e ler comentários. Sem `gh`, Claude ainda pode usar a API do GitHub, mas requisições não autenticadas frequentemente atingem limites de taxa.

Claude também é eficaz em aprender ferramentas CLI que não conhece. Tente prompts como `Use 'foo-cli-tool --help' to learn about foo tool, then use it to solve A, B, C.`

<h3 id="connect-mcp-servers">
  Conecte MCP servers
</h3>

<Tip>
  Execute `claude mcp add` para conectar ferramentas externas como Notion, Figma ou seu banco de dados.
</Tip>

Com [MCP servers](https://code.claude.com/docs/pt/mcp), você pode pedir ao Claude para implementar recursos de rastreadores de issues, consultar bancos de dados, analisar dados de monitoramento, integrar designs do Figma e automatizar fluxos de trabalho.

<h3 id="set-up-hooks">
  Configure hooks
</h3>

<Tip>
  Use hooks para ações que devem acontecer toda vez com zero exceções.
</Tip>

[Hooks](https://code.claude.com/docs/pt/hooks-guide) executam scripts automaticamente em pontos específicos do fluxo de trabalho do Claude. Diferentemente de instruções CLAUDE.md que são consultivas, hooks são determinísticos e garantem que a ação aconteça.

Claude pode escrever hooks para você. Tente prompts como *"Write a hook that runs eslint after every file edit"* ou *"Write a hook that blocks writes to the migrations folder."* Edite `.claude/settings.json` diretamente para configurar hooks manualmente, e execute `/hooks` para navegar o que está configurado.

<h3 id="create-skills">
  Crie skills
</h3>

<Tip>
  Crie arquivos `SKILL.md` em `.claude/skills/` para dar ao Claude conhecimento de domínio e fluxos de trabalho reutilizáveis.
</Tip>

[Skills](https://code.claude.com/docs/pt/skills) estendem o conhecimento do Claude com informações específicas do seu projeto, equipe ou domínio. Claude as aplica automaticamente quando relevante, ou você pode invocá-las diretamente com `/skill-name`.

Crie uma skill adicionando um diretório com um `SKILL.md` para `.claude/skills/`:

```markdown .claude/skills/api-conventions/SKILL.md theme={null}
---
name: api-conventions
description: REST API design conventions for our services
---
# API Conventions
- Use kebab-case for URL paths
- Use camelCase for JSON properties
- Always include pagination for list endpoints
- Version APIs in the URL path (/v1/, /v2/)
```

Skills também podem definir fluxos de trabalho reutilizáveis que você invoca diretamente:

```markdown .claude/skills/fix-issue/SKILL.md theme={null}
---
name: fix-issue
description: Fix a GitHub issue
disable-model-invocation: true
---
Analyze and fix the GitHub issue: $ARGUMENTS.

1. Use `gh issue view` to get the issue details
2. Understand the problem described in the issue
3. Search the codebase for relevant files
4. Implement the necessary changes to fix the issue
5. Write and run tests to verify the fix
6. Ensure code passes linting and type checking
7. Create a descriptive commit message
8. Push and create a PR
```

Execute `/fix-issue 1234` para invocá-la. Use `disable-model-invocation: true` para fluxos de trabalho com efeitos colaterais que você quer disparar manualmente.

<h3 id="create-custom-subagents">
  Crie subagents personalizados
</h3>

<Tip>
  Defina assistentes especializados em `.claude/agents/` que Claude pode delegar para tarefas isoladas.
</Tip>

[Subagents](https://code.claude.com/docs/pt/sub-agents) executam em seu próprio contexto com seu próprio conjunto de ferramentas permitidas. Eles são úteis para tarefas que leem muitos arquivos ou precisam de foco especializado sem poluir sua conversa principal.

```markdown .claude/agents/security-reviewer.md theme={null}
---
name: security-reviewer
description: Reviews code for security vulnerabilities
tools: Read, Grep, Glob, Bash
model: opus
---
You are a senior security engineer. Review code for:
- Injection vulnerabilities (SQL, XSS, command injection)
- Authentication and authorization flaws
- Secrets or credentials in code
- Insecure data handling

Provide specific line references and suggested fixes.
```

Diga ao Claude para usar subagents explicitamente: *"Use a subagent to review this code for security issues."*

<h3 id="install-plugins">
  Instale plugins
</h3>

<Tip>
  Execute `/plugin` para navegar no marketplace. Plugins adicionam skills, ferramentas e integrações sem configuração.
</Tip>

[Plugins](https://code.claude.com/docs/pt/plugins) agrupam skills, hooks, subagents e MCP servers em uma única unidade instalável da comunidade e Anthropic. Se você trabalha com uma linguagem tipada, instale um [code intelligence plugin](https://code.claude.com/docs/pt/discover-plugins#code-intelligence) para dar ao Claude navegação de símbolo precisa e detecção automática de erros após edições.

Para orientação sobre escolher entre skills, subagents, hooks e MCP, consulte [Extend Claude Code](https://code.claude.com/docs/pt/features-overview#match-features-to-your-goal).

***

<h2 id="communicate-effectively">
  Comunique-se efetivamente
</h2>

A forma como você se comunica com Claude Code impacta significativamente a qualidade dos resultados.

<h3 id="ask-codebase-questions">
  Faça perguntas sobre o codebase
</h3>

<Tip>
  Faça ao Claude perguntas que você faria a um engenheiro sênior.
</Tip>

Ao se integrar a um novo codebase, use Claude Code para aprendizado e exploração. Você pode fazer ao Claude o mesmo tipo de perguntas que faria a outro engenheiro:

* Como funciona o logging?
* Como faço um novo endpoint de API?
* O que `async move { ... }` faz na linha 134 de `foo.rs`?
* Quais casos extremos `CustomerOnboardingFlowImpl` trata?
* Por que este código chama `foo()` em vez de `bar()` na linha 333?

Usar Claude Code dessa forma é um fluxo de trabalho de integração eficaz, melhorando o tempo de ramp-up e reduzindo carga em outros engenheiros. Nenhum prompt especial necessário: faça perguntas diretamente.

<h3 id="let-claude-interview-you">
  Deixe Claude entrevistá-lo
</h3>

<Tip>
  Para recursos maiores, deixe Claude entrevistá-lo primeiro. Comece com um prompt mínimo e peça ao Claude para entrevistá-lo usando a ferramenta `AskUserQuestion`.
</Tip>

Claude pergunta sobre coisas que você pode não ter considerado ainda, incluindo implementação técnica, UI/UX, casos extremos e tradeoffs.

```text theme={null}
I want to build [brief description]. Interview me in detail using the AskUserQuestion tool.

Ask about technical implementation, UI/UX, edge cases, concerns, and tradeoffs. Don't ask obvious questions, dig into the hard parts I might not have considered.

Keep interviewing until we've covered everything, then write a complete spec to SPEC.md.
```

Uma vez que o spec está completo, comece uma nova sessão para executá-lo. A nova sessão tem contexto limpo focado inteiramente em implementação, e você tem um spec escrito para referenciar.

Os specs mais úteis são autossuficientes: eles nomeiam os arquivos e interfaces envolvidos, declaram o que está fora do escopo, e terminam com uma etapa de verificação de ponta a ponta que prova que o recurso funciona. O tempo gasto tornando o spec preciso compensa mais do que o tempo gasto observando a implementação.

***

<h2 id="manage-your-session">
  Gerencie sua sessão
</h2>

Conversas são persistentes e reversíveis. Use isso a seu favor!

<h3 id="course-correct-early-and-often">
  Corrija o curso cedo e frequentemente
</h3>

<Tip>
  Corrija Claude assim que notar que está saindo do caminho.
</Tip>

Os melhores resultados vêm de loops de feedback apertados. Embora Claude ocasionalmente resolva problemas perfeitamente na primeira tentativa, corrigi-lo rapidamente geralmente produz melhores soluções mais rápido.

* **`Esc`**: pare Claude no meio da ação com a tecla `Esc`. O contexto é preservado, então você pode redirecionar.
* **`Esc + Esc` ou `/rewind`**: pressione `Esc` duas vezes ou execute `/rewind` para abrir o menu de rewind e restaurar conversa e estado de código anterior, ou resumir a partir de uma mensagem selecionada.
* **`"Undo that"`**: peça ao Claude para reverter suas alterações.
* **`/clear`**: redefina contexto entre tarefas não relacionadas. Sessões longas com contexto irrelevante podem reduzir desempenho.

Se você corrigiu Claude mais de duas vezes no mesmo problema em uma sessão, o contexto está poluído com abordagens falhadas. Execute `/clear` e comece de novo com um prompt mais específico que incorpore o que você aprendeu. Uma sessão limpa com um prompt melhor quase sempre supera uma sessão longa com correções acumuladas.

<h3 id="manage-context-aggressively">
  Gerencie contexto agressivamente
</h3>

<Tip>
  Execute `/clear` entre tarefas não relacionadas para redefinir contexto.
</Tip>

Claude Code compacta automaticamente o histórico de conversa quando você se aproxima dos limites de contexto, o que preserva código e decisões importantes enquanto libera espaço.

Durante sessões longas, a janela de contexto do Claude pode se encher com conversa irrelevante, conteúdos de arquivo e comandos. Isso pode reduzir desempenho e às vezes distrair Claude.

* Use `/clear` frequentemente entre tarefas para redefinir a janela de contexto inteiramente
* Quando auto compaction dispara, Claude resume o que importa mais, incluindo padrões de código, estados de arquivo e decisões-chave
* Para mais controle, execute `/compact <instructions>`, como `/compact Focus on the API changes`
* Para compactar apenas parte da conversa, use `Esc + Esc` ou `/rewind`, selecione um checkpoint de mensagem e escolha **Summarize from here** ou **Summarize up to here**. O primeiro condensa mensagens daquele ponto em diante enquanto mantém contexto anterior intacto; o segundo condensa mensagens anteriores enquanto mantém as recentes em cheio. Veja [Restore vs. summarize](https://code.claude.com/docs/pt/checkpointing#restore-vs-summarize).
* Customize comportamento de compaction em CLAUDE.md com instruções como `"When compacting, always preserve the full list of modified files and any test commands"` para garantir que contexto crítico sobreviva à sumarização
* Para perguntas rápidas que não precisam ficar em contexto, use [`/btw`](https://code.claude.com/docs/pt/interactive-mode#side-questions-with-%2Fbtw). A resposta aparece em uma sobreposição dispensável e nunca entra no histórico de conversa, então você pode verificar um detalhe sem crescer contexto.

<h3 id="use-subagents-for-investigation">
  Use subagents para investigação
</h3>

<Tip>
  Delegue pesquisa com `"use subagents to investigate X"`. Eles exploram em um contexto separado, mantendo sua conversa principal limpa para implementação.
</Tip>

Como contexto é sua restrição fundamental, subagents são uma das ferramentas mais poderosas disponíveis. Quando Claude pesquisa um codebase ele lê muitos arquivos, todos os quais consomem seu contexto. Subagents executam em janelas de contexto separadas e relatam resumos:

```text theme={null}
Use subagents to investigate how our authentication system handles token
refresh, and whether we have any existing OAuth utilities I should reuse.
```

O subagent explora o codebase, lê arquivos relevantes e relata descobertas, tudo sem poluir sua conversa principal.

Você também pode usar subagents para verificação após Claude implementar algo:

```text theme={null}
use a subagent to review this code for edge cases
```

<h3 id="rewind-with-checkpoints">
  Rewind com checkpoints
</h3>

<Tip>
  Cada prompt que você envia cria um checkpoint. Você pode restaurar conversa, código ou ambos para qualquer checkpoint anterior.
</Tip>

Claude automaticamente faz snapshots de arquivos antes de cada mudança para que um checkpoint possa restaurá-los. Pressione Escape duas vezes ou execute `/rewind` para abrir o menu de rewind. Você pode restaurar apenas conversa, restaurar apenas código, restaurar ambos ou resumir a partir de uma mensagem selecionada. Veja [Checkpointing](https://code.claude.com/docs/pt/checkpointing) para detalhes.

Em vez de planejar cuidadosamente cada movimento, você pode dizer ao Claude para tentar algo arriscado. Se não funcionar, rewind e tente uma abordagem diferente. Checkpoints são salvos com a conversa, então você pode fechar seu terminal, retomar a sessão depois e ainda fazer rewind.

<Warning>
  Checkpoints apenas rastreiam mudanças feitas através das ferramentas de edição de arquivo do Claude. Mudanças feitas através de comandos Bash ou processos externos não são capturadas. Isso não é um substituto para git.
</Warning>

<h3 id="resume-conversations">
  Retome conversas
</h3>

<Tip>
  Nomeie sessões com `/rename` e trate-as como branches: cada fluxo de trabalho obtém seu próprio contexto persistente.
</Tip>

Claude Code salva conversas localmente, então quando uma tarefa abrange múltiplas sessões você não tem que re-explicar o contexto. Execute `claude --continue` para continuar a sessão mais recente, ou `claude --resume` para escolher de uma lista. Dê às sessões nomes descritivos como `oauth-migration` para que você possa encontrá-las depois. Veja [Manage sessions](https://code.claude.com/docs/pt/sessions) para o conjunto completo de controles de resume, branch e nomeação.

***

<h2 id="automate-and-scale">
  Automatize e dimensione
</h2>

Uma vez que você é eficaz com um Claude, multiplique sua saída com sessões paralelas, modo não-interativo e padrões de fan-out.

Tudo até agora assume um humano, um Claude e uma conversa. Mas Claude Code dimensiona horizontalmente. As técnicas nesta seção mostram como você pode fazer mais.

<h3 id="run-non-interactive-mode">
  Execute modo não-interativo
</h3>

<Tip>
  Use `claude -p "prompt"` em CI, pre-commit hooks ou scripts. Adicione `--output-format stream-json --verbose` para saída JSON em streaming.
</Tip>

Com `claude -p "your prompt"`, você pode executar Claude não-interativamente, sem um prompt interativo. A execução ainda cria uma sessão retomável a menos que você passe `--no-session-persistence`. [Modo não-interativo](https://code.claude.com/docs/pt/headless) é como você integra Claude em pipelines CI, pre-commit hooks ou qualquer fluxo de trabalho automatizado. Os formatos de saída permitem que você analise resultados programaticamente: texto simples, JSON ou JSON em streaming.

```bash theme={null}
# One-off queries
claude -p "Explain what this project does"

# Structured output for scripts
claude -p "List all API endpoints" --output-format json

# Streaming for real-time processing
claude -p "Analyze this log file" --output-format stream-json --verbose
```

<h3 id="run-multiple-claude-sessions">
  Execute múltiplas sessões Claude
</h3>

<Tip>
  Execute múltiplas sessões Claude em paralelo para acelerar desenvolvimento, executar experimentos isolados ou iniciar fluxos de trabalho complexos.
</Tip>

Escolha a abordagem paralela que se adequa a quanto de coordenação você quer fazer por conta própria:

* [Worktrees](https://code.claude.com/docs/pt/worktrees): execute sessões CLI separadas em checkouts git isolados para que edições não colidam
* [Aplicativo desktop](https://code.claude.com/docs/pt/desktop#work-in-parallel-with-sessions): gerencie múltiplas sessões locais visualmente, cada uma em seu próprio worktree
* [Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web): execute sessões na infraestrutura de nuvem gerenciada pela Anthropic em VMs isoladas
* [Equipes de agentes](https://code.claude.com/docs/pt/agent-teams): coordenação automatizada de múltiplas sessões com tarefas compartilhadas, mensagens e um líder de equipe

Além de paralelizar trabalho, múltiplas sessões habilitam fluxos de trabalho focados em qualidade. Um contexto fresco melhora revisão de código já que Claude não será enviesado para código que acabou de escrever.

Por exemplo, use um padrão Writer/Reviewer:

| Session A (Writer)                                                      | Session B (Reviewer)                                                                                                                                                     |
| ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `Implement a rate limiter for our API endpoints`                        |                                                                                                                                                                          |
|                                                                         | `Review the rate limiter implementation in @src/middleware/rateLimiter.ts. Look for edge cases, race conditions, and consistency with our existing middleware patterns.` |
| `Here's the review feedback: [Session B output]. Address these issues.` |                                                                                                                                                                          |

Você pode fazer algo similar com testes: ter um Claude escrever testes, depois outro escrever código para passá-los.

<h3 id="fan-out-across-files">
  Fan out entre arquivos
</h3>

<Tip>
  Faça loop através de tarefas chamando `claude -p` para cada uma. Use `--allowedTools` para escopear permissões para operações em lote.
</Tip>

Para grandes migrações ou análises, você pode distribuir trabalho entre muitas invocações Claude paralelas:

<Steps>
  <Step title="Generate a task list">
    Tenha Claude listar todos os arquivos que precisam ser migrados (por exemplo, `list all 2,000 Python files that need migrating`)
  </Step>

  <Step title="Write a script to loop through the list">
    ```bash theme={null}
    for file in $(cat files.txt); do
      claude -p "Migrate $file from React to Vue. Return OK or FAIL." \
        --allowedTools "Edit,Bash(git commit *)"
    done
    ```
  </Step>

  <Step title="Test on a few files, then run at scale">
    Refine seu prompt baseado no que dá errado com os primeiros 2-3 arquivos, depois execute no conjunto completo. A flag `--allowedTools` restringe o que Claude pode fazer, o que importa quando você está executando sem supervisão.
  </Step>
</Steps>

Você também pode integrar Claude em pipelines de dados/processamento existentes:

```bash theme={null}
claude -p "<your prompt>" --output-format json | your_command
```

Use `--verbose` para depuração durante desenvolvimento e desligue em produção.

<h3 id="run-autonomously-with-auto-mode">
  Execute autonomamente com auto mode
</h3>

Para execução ininterrupta com verificações de segurança em background, use [auto mode](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode). Um modelo classificador revisa comandos antes de serem executados, bloqueando escalação de escopo, infraestrutura desconhecida e ações impulsionadas por conteúdo hostil enquanto deixa trabalho rotineiro prosseguir sem prompts.

```bash theme={null}
claude --permission-mode auto -p "fix all lint errors"
```

Para execuções não-interativas com a flag `-p`, auto mode aborta se o classificador repetidamente bloqueia ações, já que não há usuário para recorrer. Veja [when auto mode falls back](https://code.claude.com/docs/pt/permission-modes#when-auto-mode-falls-back) para limites.

<h3 id="add-an-adversarial-review-step">
  Adicione uma etapa de revisão adversarial
</h3>

<Tip>
  Antes de tratar uma tarefa como concluída, tenha um subagente revisar o diff em um contexto fresco e relatar lacunas.
</Tip>

Quanto mais tempo Claude trabalha sem supervisão, mais uma verificação independente importa antes de você contar o trabalho como concluído. Um revisor executando em um contexto fresco de [subagente](https://code.claude.com/docs/pt/sub-agents) vê apenas o diff e os critérios que você fornece, não o raciocínio que produziu a mudança, então avalia o resultado em seus próprios termos.

Para uma verificação de correção, execute a skill [`/code-review`](https://code.claude.com/docs/pt/commands) incluída, que revisa o diff atual para bugs em um subagente fresco e retorna descobertas para a sessão. Para verificar o diff contra seu plano em vez disso, escreva o prompt de revisão você mesmo. Nomeie o trabalho a verificar, o plano a verificar contra e o que conta como uma descoberta:

```text theme={null}
Use a subagent to review the rate limiter diff against PLAN.md. Check that
every requirement is implemented, the listed edge cases have tests, and
nothing outside the task's scope changed. Report gaps, not style preferences.
```

Como o revisor executa como um subagente, a sessão implementadora recebe as lacunas diretamente e pode corrigi-las e revisar novamente sem você copiar descobertas entre janelas. Para execuções autônomas mais longas, uma [equipe de agentes](https://code.claude.com/docs/pt/agent-teams) pode manter este loop funcionando entre muitas tarefas enquanto você verifica os achados registrados.

<Callout>
  Um revisor solicitado a encontrar lacunas geralmente relatará algumas, mesmo quando o trabalho é sólido, porque é isso que foi pedido para fazer. Perseguir cada descoberta leva a over-engineering: camadas de abstração extras, código defensivo e testes para casos que não podem acontecer. Diga ao revisor para sinalizar apenas lacunas que afetam correção ou os requisitos declarados, e trate o resto como opcional.
</Callout>

***

<h2 id="avoid-common-failure-patterns">
  Evite padrões de falha comuns
</h2>

Estes são erros comuns. Reconhecê-los cedo economiza tempo:

* **A sessão da pia da cozinha.** Você começa com uma tarefa, depois pergunta ao Claude algo não relacionado, depois volta para a primeira tarefa. Contexto está cheio de informação irrelevante.
  > **Correção**: `/clear` entre tarefas não relacionadas.
* **Corrigindo repetidamente.** Claude faz algo errado, você corrige, ainda está errado, você corrige novamente. Contexto está poluído com abordagens falhadas.
  > **Correção**: Após duas correções falhadas, `/clear` e escreva um prompt inicial melhor incorporando o que você aprendeu.
* **O CLAUDE.md sobre-especificado.** Se seu CLAUDE.md é muito longo, Claude ignora metade dele porque regras importantes se perdem no ruído.
  > **Correção**: Poda impiedosamente. Se Claude já faz algo corretamente sem a instrução, delete-a ou converta-a para um hook.
* **A lacuna confiança-depois-verificação.** Claude produz uma implementação que parece plausível mas não trata casos extremos.
  > **Correção**: Sempre forneça verificação (testes, scripts, capturas de tela). Se você não pode verificar, não envie.
* **A exploração infinita.** Você pede ao Claude para "investigar" algo sem escopá-lo. Claude lê centenas de arquivos, enchendo o contexto.
  > **Correção**: Escopo investigações estreitamente ou use subagents para que a exploração não consuma seu contexto principal.

***

<h2 id="develop-your-intuition">
  Desenvolva sua intuição
</h2>

Os padrões neste guia não são gravados em pedra. Eles são pontos de partida que funcionam bem em geral, mas podem não ser ótimos para cada situação.

Às vezes você *deveria* deixar contexto acumular porque você está profundo em um problema complexo e o histórico é valioso. Às vezes você deveria pular planejamento e deixar Claude descobrir porque a tarefa é exploratória. Às vezes um prompt vago é exatamente certo porque você quer ver como Claude interpreta o problema antes de constrangê-lo.

Preste atenção ao que funciona. Quando Claude produz saída ótima, note o que você fez: a estrutura do prompt, o contexto que você forneceu, o modo que você estava. Quando Claude luta, pergunte por quê. O contexto era muito barulhento? O prompt muito vago? A tarefa muito grande para uma passagem?

Ao longo do tempo, você desenvolverá intuição que nenhum guia pode capturar. Você saberá quando ser específico e quando ser aberto, quando planejar e quando explorar, quando limpar contexto e quando deixá-lo acumular.

<h2 id="related-resources">
  Recursos relacionados
</h2>

* [How Claude Code works](https://code.claude.com/docs/pt/how-claude-code-works): o loop agentic, ferramentas e gerenciamento de contexto
* [Extend Claude Code](https://code.claude.com/docs/pt/features-overview): skills, hooks, MCP, subagents e plugins
* [Common workflows](https://code.claude.com/docs/pt/common-workflows): receitas passo a passo para depuração, teste, PRs e mais
* [CLAUDE.md](https://code.claude.com/docs/pt/memory): armazene convenções de projeto e contexto persistente
