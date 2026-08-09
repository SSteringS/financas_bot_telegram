<!-- Fonte: https://code.claude.com/docs/pt/features-overview | Baixado em: 2026-08-06 | slug: features-overview -->

# Estender Claude Code

> Entenda quando usar CLAUDE.md, Skills, subagents, hooks, MCP e plugins.

Claude Code combina um modelo que raciocina sobre seu código com [ferramentas integradas](https://code.claude.com/docs/pt/how-claude-code-works#tools) para operações de arquivo, busca, execução e acesso à web. As ferramentas integradas cobrem a maioria das tarefas de codificação. Este guia cobre a camada de extensão: recursos que você adiciona para personalizar o que Claude sabe, conectá-lo a serviços externos e automatizar fluxos de trabalho.

<Note>
  Para saber como o loop agentic principal funciona, consulte [Como Claude Code funciona](https://code.claude.com/docs/pt/how-claude-code-works).
</Note>

**Novo no Claude Code?** Comece com [CLAUDE.md](https://code.claude.com/docs/pt/memory) para convenções de projeto, depois adicione outras extensões [conforme gatilhos específicos surgirem](#build-your-setup-over-time).

<h2 id="overview">
  Visão geral
</h2>

As extensões se conectam a diferentes partes do loop agentic:

* **[CLAUDE.md](https://code.claude.com/docs/pt/memory)** adiciona contexto persistente que Claude vê a cada sessão
* **[Skills](https://code.claude.com/docs/pt/skills)** adicionam conhecimento reutilizável e fluxos de trabalho invocáveis
* **[Code intelligence](https://code.claude.com/docs/pt/tools-reference#lsp-tool-behavior)** conecta Claude a um language server para navegação em nível de símbolo e erros de tipo em tempo real
* **[MCP](https://code.claude.com/docs/pt/mcp)** conecta Claude a serviços e ferramentas externas
* **[Subagents](https://code.claude.com/docs/pt/sub-agents)** executam seus próprios loops em contexto isolado, retornando resumos
* **[Agent teams](https://code.claude.com/docs/pt/agent-teams)** coordenam múltiplas sessões independentes com tarefas compartilhadas e mensagens ponto a ponto
* **[Hooks](https://code.claude.com/docs/pt/hooks-guide)** disparam em eventos de ciclo de vida e podem executar um script, solicitação HTTP, prompt ou subagent
* **[Plugins](https://code.claude.com/docs/pt/plugins)** e **[marketplaces](https://code.claude.com/docs/pt/plugin-marketplaces)** empacotam e distribuem esses recursos

[Skills](https://code.claude.com/docs/pt/skills) são a extensão mais flexível. Uma skill é um arquivo markdown contendo conhecimento, fluxos de trabalho ou instruções. Você pode invocar skills com um comando como `/deploy`, ou Claude pode carregá-las automaticamente quando relevante. Skills podem ser executadas em sua conversa atual ou em contexto isolado via subagents.

<h2 id="match-features-to-your-goal">
  Corresponder recursos ao seu objetivo
</h2>

Os recursos variam de contexto sempre ativo que Claude vê a cada sessão, a capacidades sob demanda que você ou Claude podem invocar, a automação em segundo plano que é executada em eventos específicos. A tabela abaixo mostra o que está disponível e quando cada um faz sentido.

| Recurso                                                        | O que faz                                                          | Quando usar                                                                                | Exemplo                                                                                                         |
| -------------------------------------------------------------- | ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------- |
| **CLAUDE.md**                                                  | Contexto persistente carregado a cada conversa                     | Convenções de projeto, regras "sempre faça X"                                              | "Use pnpm, não npm. Execute testes antes de fazer commit."                                                      |
| **Skill**                                                      | Instruções, conhecimento e fluxos de trabalho que Claude pode usar | Conteúdo reutilizável, documentos de referência, tarefas repetíveis                        | `/deploy` executa sua lista de verificação de implantação; skill de documentação de API com padrões de endpoint |
| **Subagent**                                                   | Contexto de execução isolado que retorna resultados resumidos      | Isolamento de contexto, tarefas paralelas, trabalhadores especializados                    | Tarefa de pesquisa que lê muitos arquivos mas retorna apenas descobertas principais                             |
| **[Agent teams](https://code.claude.com/docs/pt/agent-teams)**                             | Coordenar múltiplas sessões independentes do Claude Code           | Pesquisa paralela, desenvolvimento de novos recursos, depuração com hipóteses concorrentes | Gerar revisores para verificar segurança, desempenho e testes simultaneamente                                   |
| **[Code intelligence](https://code.claude.com/docs/pt/tools-reference#lsp-tool-behavior)** | Navegação e diagnósticos do language server                        | Linguagens tipadas, grandes bases de código onde grep é lento ou impreciso                 | Ir para a definição de um símbolo em vez de ler o arquivo inteiro                                               |
| **MCP**                                                        | Conectar a serviços externos                                       | Dados ou ações externas                                                                    | Consultar seu banco de dados, postar no Slack, controlar um navegador                                           |
| **Hook**                                                       | Script, solicitação HTTP, prompt ou subagent disparado por eventos | Automação que deve ser executada em cada evento correspondente                             | Executar ESLint após cada edição de arquivo                                                                     |
| **[Artifact](https://code.claude.com/docs/pt/artifacts)**                                  | Publicar saída de sessão como uma página web privada e interativa  | Saída que você quer ver ou compartilhar visualmente em vez de como texto de terminal       | Uma linha do tempo de incidente que se atualiza conforme Claude investiga                                       |

**[Plugins](https://code.claude.com/docs/pt/plugins)** são a camada de empacotamento. Um plugin agrupa skills, hooks, subagents e servidores MCP em uma única unidade instalável. Skills de plugin são nomeadas (como `/my-plugin:review`) para que múltiplos plugins possam coexistir. Use plugins quando quiser reutilizar a mesma configuração em múltiplos repositórios ou distribuir para outros via um **[marketplace](https://code.claude.com/docs/pt/plugin-marketplaces)**.

<h3 id="build-your-setup-over-time">
  Construir sua configuração ao longo do tempo
</h3>

Você não precisa configurar tudo antecipadamente. Cada recurso tem um gatilho reconhecível, e a maioria das equipes os adiciona aproximadamente nesta ordem:

| Gatilho                                                                                  | Adicionar                                                                                           |
| :--------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------------- |
| Claude erra uma convenção ou comando duas vezes                                          | Adicione a [CLAUDE.md](https://code.claude.com/docs/pt/memory)                                                                  |
| Você continua digitando o mesmo prompt para iniciar uma tarefa                           | Salve como uma [skill](https://code.claude.com/docs/pt/skills) invocável pelo usuário                                           |
| Você cola o mesmo playbook ou procedimento de múltiplas etapas no chat pela terceira vez | Capture como uma [skill](https://code.claude.com/docs/pt/skills)                                                                |
| Você continua copiando dados de uma aba do navegador que Claude não consegue ver         | Conecte esse sistema como um [servidor MCP](https://code.claude.com/docs/pt/mcp)                                                |
| Claude lê muitos arquivos para encontrar onde um símbolo é definido ou usado             | Instale um [plugin de code intelligence](https://code.claude.com/docs/pt/discover-plugins#code-intelligence) para sua linguagem |
| Uma tarefa secundária inunda sua conversa com saída que você não consultará novamente    | Encaminhe através de um [subagent](https://code.claude.com/docs/pt/sub-agents)                                                  |
| Você quer que algo aconteça toda vez sem pedir                                           | Escreva um [hook](https://code.claude.com/docs/pt/hooks-guide)                                                                  |
| Um segundo repositório precisa da mesma configuração                                     | Empacote como um [plugin](https://code.claude.com/docs/pt/plugins)                                                              |

Os mesmos gatilhos dizem quando atualizar o que você já tem. Um erro repetido ou um comentário de revisão recorrente é uma edição de CLAUDE.md, não uma correção única no chat. Um fluxo de trabalho que você continua ajustando manualmente é uma skill que precisa de outra revisão.

<h3 id="compare-similar-features">
  Comparar recursos similares
</h3>

Alguns recursos podem parecer similares. Para uma análise mais profunda sobre como escolher entre eles, consulte [Steering Claude Code: when to use CLAUDE.md, skills, hooks, and subagents](https://claude.com/blog/steering-claude-code-skills-hooks-rules-subagents-and-more) no blog. Aqui está como diferenciá-los.

<Tabs>
  <Tab title="Skill vs Subagent">
    Skills e subagents resolvem problemas diferentes:

    * **Skills** são conteúdo reutilizável que você pode carregar em qualquer contexto
    * **Subagents** são trabalhadores isolados que são executados separadamente de sua conversa principal

    | Aspecto                                                 | Skill                                                        | Subagent                                                                           |
    | ------------------------------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------------- |
    | **O que é**                                             | Instruções, conhecimento ou fluxos de trabalho reutilizáveis | Trabalhador isolado com seu próprio contexto                                       |
    | **Benefício principal**                                 | Compartilhar conteúdo entre contextos                        | Isolamento de contexto. O trabalho acontece separadamente, apenas o resumo retorna |
    | **Impacto da [janela de contexto](https://code.claude.com/docs/pt/context-window)** | Adiciona à sua janela principal                              | Usa uma janela separada com seus próprios tokens de entrada e saída                |
    | **Melhor para**                                         | Material de referência, fluxos de trabalho invocáveis        | Tarefas que leem muitos arquivos, trabalho paralelo, trabalhadores especializados  |

    **Skills podem ser referência ou ação.** Skills de referência fornecem conhecimento que Claude usa ao longo de sua sessão (como seu guia de estilo de API). Skills de ação dizem a Claude para fazer algo específico (como `/deploy` que executa seu fluxo de trabalho de implantação).

    **Use um subagent** quando você precisar de isolamento de contexto ou quando sua janela de contexto estiver ficando cheia. O subagent pode ler dezenas de arquivos ou executar buscas extensas, mas sua conversa principal recebe apenas um resumo. Como o trabalho do subagent não consome seu contexto principal, isso também é útil quando você não precisa que o trabalho intermediário permaneça visível. Subagents personalizados podem ter suas próprias instruções e podem pré-carregar skills.

    **Eles podem se combinar.** Um subagent pode pré-carregar skills específicas (campo `skills:`). Uma skill pode ser executada em contexto isolado usando `context: fork`. Consulte [Skills](https://code.claude.com/docs/pt/skills) para detalhes.
  </Tab>

  <Tab title="CLAUDE.md vs Skill">
    Ambos armazenam instruções, mas carregam de forma diferente e servem a propósitos diferentes.

    | Aspecto                              | CLAUDE.md                      | Skill                                                 |
    | ------------------------------------ | ------------------------------ | ----------------------------------------------------- |
    | **Carrega**                          | A cada sessão, automaticamente | Sob demanda                                           |
    | **Pode incluir arquivos**            | Sim, com importações `@path`   | Sim, com importações `@path`                          |
    | **Pode disparar fluxos de trabalho** | Não                            | Sim, com `/<name>`                                    |
    | **Melhor para**                      | Regras "sempre faça X"         | Material de referência, fluxos de trabalho invocáveis |

    **Coloque em CLAUDE.md** se Claude sempre deve saber: convenções de codificação, comandos de compilação, estrutura do projeto, regras "nunca faça X".

    **Coloque em uma skill** se for material de referência que Claude precisa às vezes (documentação de API, guias de estilo) ou um fluxo de trabalho que você dispara com `/<name>` (deploy, review, release).

    **Regra prática:** Mantenha CLAUDE.md com menos de 200 linhas. Se estiver crescendo, mova conteúdo de referência para skills ou divida em arquivos [`.claude/rules/`](https://code.claude.com/docs/pt/memory#organize-rules-with-claude%2Frules%2F).
  </Tab>

  <Tab title="CLAUDE.md vs Rules vs Skills">
    Todos os três armazenam instruções, mas carregam de forma diferente:

    | Aspecto         | CLAUDE.md                                      | `.claude/rules/`                                              | Skill                                                 |
    | --------------- | ---------------------------------------------- | ------------------------------------------------------------- | ----------------------------------------------------- |
    | **Carrega**     | A cada sessão                                  | A cada sessão, ou quando arquivos correspondentes são abertos | Sob demanda, quando invocado ou relevante             |
    | **Escopo**      | Projeto inteiro                                | Pode ser limitado a caminhos de arquivo                       | Específico da tarefa                                  |
    | **Melhor para** | Convenções principais e comandos de compilação | Diretrizes específicas de linguagem ou diretório              | Material de referência, fluxos de trabalho repetíveis |

    **Use CLAUDE.md** para instruções que cada sessão precisa: comandos de compilação, convenções de teste, arquitetura do projeto.

    **Use rules** para manter CLAUDE.md focado. Rules com [frontmatter `paths`](https://code.claude.com/docs/pt/memory#path-specific-rules) carregam apenas quando Claude trabalha com arquivos correspondentes, economizando contexto.

    **Use skills** para conteúdo que Claude só precisa às vezes, como documentação de API ou uma lista de verificação de implantação que você dispara com `/<name>`.
  </Tab>

  <Tab title="Subagent vs Agent team">
    Ambos paralelizam o trabalho, mas são arquitetonicamente diferentes:

    * **Subagents** são executados dentro de sua sessão e relatam resultados de volta ao seu contexto principal
    * **Agent teams** são sessões independentes do Claude Code que se comunicam entre si

    | Aspecto            | Subagent                                                        | Agent team                                                        |
    | ------------------ | --------------------------------------------------------------- | ----------------------------------------------------------------- |
    | **Contexto**       | Sua própria janela de contexto; resultados retornam ao chamador | Sua própria janela de contexto; totalmente independente           |
    | **Comunicação**    | Relata resultados de volta apenas ao agente principal           | Companheiros de equipe se mensageiam diretamente                  |
    | **Coordenação**    | Agente principal gerencia todo o trabalho                       | Lista de tarefas compartilhada com auto-coordenação               |
    | **Melhor para**    | Tarefas focadas onde apenas o resultado importa                 | Trabalho complexo que requer discussão e colaboração              |
    | **Custo de token** | Menor: resultados resumidos de volta ao contexto principal      | Maior: cada companheiro de equipe é uma instância Claude separada |

    **Use um subagent** quando você precisar de um trabalhador rápido e focado: pesquisar uma pergunta, verificar uma afirmação, revisar um arquivo. O subagent faz o trabalho e retorna um resumo. Sua conversa principal fica limpa.

    **Use um agent team** quando companheiros de equipe precisam compartilhar descobertas, desafiar um ao outro e se coordenar independentemente. Agent teams são melhores para pesquisa com hipóteses concorrentes, revisão de código paralela e desenvolvimento de novos recursos onde cada companheiro de equipe possui uma peça separada.

    **Ponto de transição:** Se você está executando subagents paralelos mas atingindo limites de contexto, ou se seus subagents precisam se comunicar entre si, agent teams são o próximo passo natural.

    <Note>
      Agent teams são experimentais e desabilitados por padrão. Consulte [agent teams](https://code.claude.com/docs/pt/agent-teams) para configuração e limitações atuais.
    </Note>
  </Tab>

  <Tab title="MCP vs Skill">
    MCP conecta Claude a serviços externos. Skills estendem o que Claude sabe, incluindo como usar esses serviços efetivamente.

    | Aspecto      | MCP                                                                  | Skill                                                                                              |
    | ------------ | -------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
    | **O que é**  | Protocolo para conectar a serviços externos                          | Conhecimento, fluxos de trabalho e material de referência                                          |
    | **Fornece**  | Ferramentas e acesso a dados                                         | Conhecimento, fluxos de trabalho, material de referência                                           |
    | **Exemplos** | Integração Slack, consultas de banco de dados, controle de navegador | Lista de verificação de revisão de código, fluxo de trabalho de implantação, guia de estilo de API |

    Esses resolvem problemas diferentes e funcionam bem juntos:

    **MCP** dá a Claude ferramentas específicas para um sistema externo, com a conexão e autenticação tratadas pelo servidor.

    **Skills** dão a Claude conhecimento sobre como usar essas ferramentas efetivamente, além de fluxos de trabalho que você pode disparar com `/<name>`. Uma skill pode incluir o esquema do banco de dados da sua equipe e padrões de consulta, ou um fluxo de trabalho `/post-to-slack` com as regras de formatação de mensagem da sua equipe.

    Exemplo: Um servidor MCP conecta Claude ao seu banco de dados. Uma skill ensina a Claude seu modelo de dados, padrões de consulta comuns e quais tabelas usar para diferentes tarefas.
  </Tab>

  <Tab title="Hook vs Skill">
    Um hook dispara em um evento de ciclo de vida; uma skill é carregada em contexto para Claude aplicar.

    | Aspecto               | Hook                                                                                   | Skill                                                                                              |
    | --------------------- | -------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
    | **Executa**           | Um comando shell, solicitação HTTP, prompt LLM ou subagent                             | Instruções que Claude lê e segue                                                                   |
    | **Disparado por**     | [Eventos de ciclo de vida](https://code.claude.com/docs/pt/hooks#hook-events) como `PostToolUse` ou `SessionStart` | Você digitando `/<name>`, ou Claude correspondendo a descrição à sua tarefa                        |
    | **Determinismo**      | Sempre dispara em seu evento; o gatilho é garantido                                    | Claude interpreta as instruções; o resultado pode variar                                           |
    | **Custo de contexto** | Zero a menos que o hook retorne saída                                                  | Descrição carrega a cada sessão; conteúdo completo carrega quando usado                            |
    | **Melhor para**       | Linting após edições, bloqueio de comandos inseguros, logging, notificações            | Fluxos de trabalho que precisam de raciocínio, material de referência, tarefas de múltiplas etapas |

    **Use um hook** quando a ação deve acontecer da mesma forma toda vez e não precisa Claude pensar. Por exemplo: formatar ao salvar, rejeitar `rm -rf /`, postar uma mensagem Slack quando uma sessão termina.

    **Use uma skill** quando Claude deve decidir como aplicar os passos, ou quando o conteúdo é conhecimento em vez de um script. Por exemplo: uma lista de verificação `/release`, seu guia de estilo de API, um playbook de depuração.

    **Coloque guardrails em hooks.** Uma instrução como "nunca edite `.env`" em CLAUDE.md ou uma skill é um pedido, não uma garantia. Um hook `PreToolUse` que bloqueia a edição é execução. Se uma regra deve valer toda vez, faça um hook em vez de uma instrução de prompt.

    **Saída de hook entra em contexto.** Um hook `PostToolUse` que executa seu linter alimenta resultados de volta como texto que Claude lê; uma skill `/fix-lint` diz a Claude como resolvê-los.
  </Tab>
</Tabs>

<h3 id="understand-how-features-layer">
  Entender como os recursos se sobrepõem
</h3>

Os recursos podem ser definidos em múltiplos níveis: em toda a máquina, por projeto, via plugins ou através de políticas gerenciadas. Você também pode aninhar arquivos CLAUDE.md em subdiretórios ou colocar skills em pacotes específicos de um monorepo. Quando o mesmo recurso existe em múltiplos níveis, aqui está como eles se sobrepõem:

* **Arquivos CLAUDE.md** são aditivos: todos os níveis contribuem conteúdo ao contexto de Claude simultaneamente. Arquivos do seu diretório de trabalho e acima carregam no lançamento; subdiretórios carregam conforme você trabalha neles. Quando as instruções entram em conflito, Claude usa julgamento para reconciliá-las, com instruções mais específicas tipicamente tendo precedência. Consulte [como arquivos CLAUDE.md carregam](https://code.claude.com/docs/pt/memory#how-claude-md-files-load).
* **Skills e subagents** substituem por nome: quando o mesmo nome existe em múltiplos níveis, uma definição vence com base na prioridade (gerenciado > usuário > projeto para skills; gerenciado > sinalizador CLI > projeto > usuário > plugin para subagents). Skills de plugin são [nomeadas](https://code.claude.com/docs/pt/plugins#add-skills-to-your-plugin) para evitar conflitos. Consulte [descoberta de skill](https://code.claude.com/docs/pt/skills#where-skills-live) e [escopo de subagent](https://code.claude.com/docs/pt/sub-agents#choose-the-subagent-scope).
* **Servidores MCP** substituem por nome: local > projeto > usuário. Consulte [escopo MCP](https://code.claude.com/docs/pt/mcp#scope-hierarchy-and-precedence).
* **Hooks** se mesclam: todos os hooks registrados disparam para seus eventos correspondentes independentemente da fonte. Consulte [hooks](https://code.claude.com/docs/pt/hooks-guide).

<h3 id="combine-features">
  Combinar recursos
</h3>

Cada extensão resolve um problema diferente: CLAUDE.md lida com contexto sempre ativo, skills lidam com conhecimento sob demanda e fluxos de trabalho, MCP lida com conexões externas, subagents lidam com isolamento e hooks lidam com automação. Configurações reais combinam eles com base em seu fluxo de trabalho.

Por exemplo, você pode usar CLAUDE.md para convenções de projeto, uma skill para seu fluxo de trabalho de implantação, MCP para conectar ao seu banco de dados e um hook para executar linting após cada edição. Cada recurso lida com o que é melhor.

| Padrão                 | Como funciona                                                                                     | Exemplo                                                                                             |
| ---------------------- | ------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| **Skill + MCP**        | MCP fornece a conexão; uma skill ensina a Claude como usá-la bem                                  | MCP conecta ao seu banco de dados, uma skill documenta seu esquema e padrões de consulta            |
| **Skill + Subagent**   | Uma skill gera subagents para trabalho paralelo                                                   | Skill `/audit` inicia subagents de segurança, desempenho e estilo que trabalham em contexto isolado |
| **CLAUDE.md + Skills** | CLAUDE.md contém regras sempre ativas; skills contêm material de referência carregado sob demanda | CLAUDE.md diz "siga nossas convenções de API," uma skill contém o guia de estilo de API completo    |
| **Hook + MCP**         | Um hook dispara ações externas através de MCP                                                     | Hook pós-edição envia uma notificação Slack quando Claude modifica arquivos críticos                |

<h2 id="understand-context-costs">
  Entender custos de contexto
</h2>

Cada recurso que você adiciona consome algum contexto de Claude. Muito pode preencher sua janela de contexto, mas também pode adicionar ruído que torna Claude menos eficaz; skills podem não disparar corretamente, ou Claude pode perder o controle de suas convenções. Entender esses trade-offs ajuda você a construir uma configuração eficaz. Para uma visualização interativa de como esses recursos se combinam em uma sessão em execução, consulte [Explorar a janela de contexto](https://code.claude.com/docs/pt/context-window).

<h3 id="context-cost-by-feature">
  Custo de contexto por recurso
</h3>

Cada recurso tem uma estratégia de carregamento e custo de contexto diferentes:

| Recurso               | Quando carrega                        | O que carrega                                                    | Custo de contexto                                 |
| --------------------- | ------------------------------------- | ---------------------------------------------------------------- | ------------------------------------------------- |
| **CLAUDE.md**         | Início da sessão                      | Conteúdo completo                                                | A cada requisição                                 |
| **Skills**            | Início da sessão + quando usado       | Descrições no início, conteúdo completo quando usado             | Baixo (descrições a cada requisição)\*            |
| **Servidores MCP**    | Início da sessão                      | Nomes de ferramentas; esquemas completos sob demanda             | Baixo até uma ferramenta ser usada                |
| **Code intelligence** | Após edições de arquivo e sob demanda | Diagnósticos após edições; localizações de símbolos sob consulta | Baixo; reduz leituras de arquivo em outro lugar   |
| **Subagents**         | Quando gerado                         | Contexto fresco com skills especificadas                         | Isolado da sessão principal                       |
| **Hooks**             | No disparo                            | Nada (executa externamente)                                      | Zero, a menos que hook retorne contexto adicional |

\*Por padrão, descrições de skill carregam no início da sessão para que Claude possa decidir quando usá-las. Defina `disable-model-invocation: true` no frontmatter de uma skill para ocultá-la de Claude inteiramente até que você a invoque manualmente. Isso reduz o custo de contexto para zero para skills que você só dispara você mesmo. Para uma skill que você não escreveu, defina [`skillOverrides`](https://code.claude.com/docs/pt/skills#override-skill-visibility-from-settings) em configurações para fazer o mesmo sem editar seu arquivo.

<h3 id="understand-how-features-load">
  Entender como os recursos carregam
</h3>

Cada recurso carrega em diferentes pontos em sua sessão. As abas abaixo explicam quando cada um carrega e o que entra em contexto.

<img src="https://mintcdn.com/claude-code/ikqp3_70mqIahteV/images/context-loading.svg?fit=max&auto=format&n=ikqp3_70mqIahteV&q=85&s=aab139e750494a237ae2e0c8f9139b0a" alt="Carregamento de contexto: CLAUDE.md carrega no início da sessão e permanece em cada requisição. Nomes de ferramentas MCP carregam no início com esquemas completos adiados até o uso. Skills carregam descrições no início, conteúdo completo na invocação. Subagents obtêm contexto isolado. Hooks são executados externamente." width="720" height="382" data-path="images/context-loading.svg" />

<Tabs>
  <Tab title="CLAUDE.md">
    **Quando:** Início da sessão

    **O que carrega:** Conteúdo completo de todos os arquivos CLAUDE.md (níveis gerenciado, usuário e projeto).

    **Herança:** Claude lê arquivos CLAUDE.md do seu diretório de trabalho até a raiz e descobre aninhados em subdiretórios conforme acessa esses arquivos. Consulte [Como arquivos CLAUDE.md carregam](https://code.claude.com/docs/pt/memory#how-claude-md-files-load) para detalhes.

    <Tip>Mantenha CLAUDE.md com menos de 200 linhas. Mova material de referência para skills, que carregam sob demanda.</Tip>
  </Tab>

  <Tab title="Skills">
    Skills são capacidades extras no kit de ferramentas de Claude. Podem ser material de referência (como um guia de estilo de API) ou fluxos de trabalho invocáveis que você dispara com `/<name>` (como `/deploy`). Claude Code inclui [skills agrupadas](https://code.claude.com/docs/pt/commands) como `/code-review`, `/batch` e `/debug` que funcionam imediatamente. Você também pode criar as suas próprias. Claude usa skills quando apropriado, ou você pode invocar uma diretamente.

    **Quando:** Depende da configuração da skill. Por padrão, descrições carregam no início da sessão e conteúdo completo carrega quando usado. Para skills apenas de usuário (`disable-model-invocation: true`), nada carrega até que você as invoque.

    **O que carrega:** Para skills invocáveis por modelo, Claude vê nomes e descrições em cada requisição. Quando você invoca uma skill com `/<name>` ou Claude a carrega automaticamente, o conteúdo completo carrega em sua conversa.

    **Como Claude escolhe skills:** Claude corresponde sua tarefa contra descrições de skill para decidir quais são relevantes. Se descrições forem vagas ou se sobrepuserem, Claude pode carregar a skill errada ou perder uma que ajudaria. Para dizer a Claude para usar uma skill específica, invoque-a com `/<name>`. Skills com `disable-model-invocation: true` são invisíveis a Claude até que você as invoque.

    **Custo de contexto:** Baixo até ser usado. Skills apenas de usuário têm custo zero até invocação.

    **Em subagents:** Skills funcionam diferentemente em subagents. Em vez de carregamento sob demanda, skills listadas no campo `skills` do subagent são totalmente pré-carregadas em seu contexto no lançamento. Subagents ainda podem descobrir e invocar skills de projeto, usuário e plugin não listadas através da ferramenta Skill.

    <Tip>Use `disable-model-invocation: true` para skills com efeitos colaterais. Isso economiza contexto e garante que apenas você as dispare.</Tip>
  </Tab>

  <Tab title="Servidores MCP">
    **Quando:** Início da sessão.

    **O que carrega:** Nomes de ferramentas de servidores conectados. Esquemas JSON completos permanecem adiados até Claude precisar de uma ferramenta específica.

    **Custo de contexto:** [Busca de ferramentas](https://code.claude.com/docs/pt/mcp#scale-with-mcp-tool-search) está ativada por padrão, então ferramentas MCP ociosas consomem contexto mínimo.

    <Tip>Execute `/mcp` para ver status de conexão e custos de token por servidor. Claude Code [reconecta a servidores remotos automaticamente](https://code.claude.com/docs/pt/mcp#automatic-reconnection) se eles caírem, e você pode desconectar servidores que você não está usando ativamente.</Tip>
  </Tab>

  <Tab title="Code intelligence">
    **Quando:** Após edições de arquivo, e sob demanda quando Claude navega pelo código.

    **O que carrega:** Erros de tipo e avisos após cada edição de arquivo. Informações de definição, referência e tipo quando Claude procura um símbolo.

    **Custo de contexto:** Baixo. Consultas de símbolos frequentemente substituem leituras amplas de arquivo, então o uso de contexto líquido pode diminuir.

    <Tip>A ferramenta LSP fica inativa até que você instale um [plugin de code intelligence](https://code.claude.com/docs/pt/discover-plugins#code-intelligence) para sua linguagem.</Tip>
  </Tab>

  <Tab title="Subagents">
    **Quando:** Sob demanda, quando você ou Claude gera um para uma tarefa.

    **O que carrega:** Contexto fresco e isolado contendo:

    * O prompt do sistema do agente, não o prompt do sistema completo de Claude Code
    * Conteúdo completo de skills listadas no campo `skills:` do agente
    * CLAUDE.md e status git, exceto os agentes Explore e Plan integrados [omitem ambos](https://code.claude.com/docs/pt/sub-agents#what-loads-at-startup)
    * Qualquer contexto que o agente principal passa no prompt

    **Custo de contexto:** Isolado da sessão principal. Subagents não herdam seu histórico de conversa ou skills invocadas.

    <Tip>Use subagents para trabalho que não precisa de seu contexto de conversa completo. Seu isolamento previne inchar sua sessão principal.</Tip>
  </Tab>

  <Tab title="Hooks">
    **Quando:** No disparo. Hooks disparam em eventos de ciclo de vida específicos como execução de ferramenta, limites de sessão, envio de prompt, solicitações de permissão e compactação. Consulte [Hooks](https://code.claude.com/docs/pt/hooks) para a lista completa.

    **O que carrega:** Nada por padrão. Hooks são executados fora da conversa principal.

    **Custo de contexto:** Zero, a menos que o hook retorne saída que seja adicionada como mensagens à sua conversa.

    <Tip>Hooks são ideais para efeitos colaterais (linting, logging) que não precisam afetar o contexto de Claude.</Tip>
  </Tab>
</Tabs>

<h2 id="learn-more">
  Saiba mais
</h2>

Cada recurso tem seu próprio guia com instruções de configuração, exemplos e opções de configuração.

<CardGroup cols={2}>
  <Card title="CLAUDE.md" icon="file-lines" href="/docs/pt/memory">
    Armazenar contexto de projeto, convenções e instruções
  </Card>

  <Card title="Skills" icon="brain" href="/docs/pt/skills">
    Dar a Claude expertise de domínio e fluxos de trabalho reutilizáveis
  </Card>

  <Card title="Subagents" icon="users" href="/docs/pt/sub-agents">
    Descarregar trabalho para contexto isolado
  </Card>

  <Card title="Agent teams" icon="network" href="/docs/pt/agent-teams">
    Coordenar múltiplas sessões trabalhando em paralelo
  </Card>

  <Card title="MCP" icon="plug" href="/docs/pt/mcp">
    Conectar Claude a serviços externos
  </Card>

  <Card title="Hooks" icon="bolt" href="/docs/pt/hooks-guide">
    Automatizar ações com hooks
  </Card>

  <Card title="Plugins" icon="puzzle-piece" href="/docs/pt/plugins">
    Empacotar e compartilhar conjuntos de recursos
  </Card>

  <Card title="Marketplaces" icon="store" href="/docs/pt/plugin-marketplaces">
    Hospedar e distribuir coleções de plugins
  </Card>
</CardGroup>
