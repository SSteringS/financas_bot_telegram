> ## Documentation Index
> Fetch the complete documentation index at: https://code.claude.com/docs/llms.txt
> Use this file to discover all available pages before exploring further.

# Estenda Claude com skills

> Crie, gerencie e compartilhe skills para estender as capacidades do Claude no Claude Code. Inclui comandos personalizados e skills agrupadas.

Skills estendem o que Claude pode fazer. Crie um arquivo `SKILL.md` com instruções, e Claude o adiciona ao seu kit de ferramentas. Claude usa skills quando relevante, ou você pode invocar uma diretamente com `/skill-name`.

Crie uma skill quando você fica colando o mesmo manual, checklist ou procedimento de múltiplas etapas no chat, ou quando uma seção de CLAUDE.md cresceu em um procedimento em vez de um fato. Diferentemente do conteúdo de CLAUDE.md, o corpo de uma skill carrega apenas quando é usado, então material de referência longo custa quase nada até você precisar dele.

<Note>
  Para comandos integrados como `/help` e `/compact`, e skills agrupadas como `/debug` e `/code-review`, consulte a [referência de comandos](/docs/pt/commands).

  **Comandos personalizados foram mesclados em skills.** Um arquivo em `.claude/commands/deploy.md` e uma skill em `.claude/skills/deploy/SKILL.md` ambos criam `/deploy` e funcionam da mesma forma. Seus arquivos `.claude/commands/` existentes continuam funcionando. Skills adicionam recursos opcionais: um diretório para arquivos de suporte, frontmatter para [controlar se você ou Claude invoca eles](#control-who-invokes-a-skill), e a capacidade de Claude carregá-los automaticamente quando relevante.
</Note>

Skills do Claude Code seguem o padrão aberto [Agent Skills](https://agentskills.io), que funciona em múltiplas ferramentas de IA. Claude Code estende o padrão com recursos adicionais como [controle de invocação](#control-who-invokes-a-skill), [execução de subagent](#run-skills-in-a-subagent), e [injeção de contexto dinâmico](#inject-dynamic-context).

<h2 id="bundled-skills">
  Skills agrupadas
</h2>

Claude Code inclui um conjunto de skills agrupadas que estão disponíveis em cada sessão, a menos que desabilitadas com a configuração [`disableBundledSkills`](/docs/pt/settings#available-settings), incluindo `/doctor`, `/code-review`, `/batch`, `/debug`, `/loop`, e `/claude-api`. Diferentemente da maioria dos comandos integrados, que executam lógica fixa diretamente, skills agrupadas são baseadas em prompt: elas dão ao Claude instruções detalhadas e deixam que ele orquestre o trabalho usando suas ferramentas. Você invoca elas da mesma forma que qualquer outra skill, digitando `/` seguido do nome da skill.

O checkup de configuração [`/doctor`](/docs/pt/commands#all-commands) é a única exceção ao `disableBundledSkills` no Claude Code v2.1.205 e posterior: ele permanece digitável quando a configuração está ativada. Para ocultá-lo, defina a variável de ambiente `DISABLE_DOCTOR_COMMAND` ou uma entrada [`skillOverrides`](#override-skill-visibility-from-settings) de `"doctor": "off"`. Antes da v2.1.205, `/doctor` era um comando integrado em vez de uma skill agrupada.

Skills agrupadas estão listadas junto com comandos integrados na [referência de comandos](/docs/pt/commands), marcadas como **Skill** na coluna Propósito.

<h3 id="run-and-verify-your-app">
  Execute e verifique seu aplicativo
</h3>

Três skills agrupadas trabalham juntas para iniciar seu aplicativo e confirmar alterações em relação ao aplicativo em execução em vez de apenas testes:

| Skill                  | Propósito                                                                                                                                    |
| :--------------------- | :------------------------------------------------------------------------------------------------------------------------------------------- |
| `/run`                 | Inicie e conduza seu aplicativo para ver uma alteração funcionando                                                                           |
| `/verify`              | Compile e execute seu aplicativo para confirmar que uma alteração de código faz o que deveria, sem recorrer a testes ou verificações de tipo |
| `/run-skill-generator` | Ensine ao `/run` e `/verify` como compilar e iniciar seu projeto                                                                             |

Todas as três skills requerem Claude Code v2.1.145 ou posterior.

`/run` e `/verify` funcionam sem configuração. Eles inferem o lançamento do tipo de seu projeto (CLI, servidor, TUI, orientado por navegador) e do que está em seu README, `package.json`, ou `Makefile`. Essa inferência se torna pouco confiável para projetos que precisam de algo além de um lançamento padrão: um banco de dados, um arquivo env, uma sessão gráfica, uma compilação em várias etapas.

`/run-skill-generator` registra a receita em vez disso. Ele coloca seu aplicativo em execução a partir de um ambiente limpo, captura o que funcionou (os comandos de instalação, as variáveis de ambiente, o script de lançamento) e o confirma como uma skill por projeto em `.claude/skills/run-<name>/`. Depois disso, `/run`, `/verify` e qualquer outro agente no repositório seguem a receita registrada em vez de redescobri-la. Execute `/run-skill-generator` uma vez por projeto e novamente se o processo de compilação ou lançamento mudar.

<h2 id="getting-started">
  Começando
</h2>

<h3 id="create-your-first-skill">
  Crie sua primeira skill
</h3>

Este exemplo cria uma skill que resume as mudanças não confirmadas em seu repositório git e sinaliza qualquer coisa arriscada. Ele puxa o diff ao vivo para o prompt antes de Claude lê-lo, então a resposta é fundamentada em sua árvore de trabalho real em vez do que Claude pode adivinhar a partir de arquivos abertos. Claude carrega a skill automaticamente quando você pergunta sobre suas mudanças, ou você pode invocá-la diretamente com `/summarize-changes`.

<Steps>
  <Step title="Crie o diretório da skill">
    Crie um diretório para a skill em sua pasta de skills pessoais. Skills pessoais estão disponíveis em todos os seus projetos.

    ```bash theme={null}
    mkdir -p ~/.claude/skills/summarize-changes
    ```
  </Step>

  <Step title="Escreva SKILL.md">
    Cada skill precisa de um arquivo `SKILL.md` com duas partes: frontmatter YAML entre marcadores `---` que diz ao Claude quando usar a skill, e conteúdo markdown com as instruções que Claude segue quando a skill é executada. O nome do diretório se torna o comando que você digita, e a `description` ajuda Claude a decidir quando carregar a skill automaticamente.

    Salve isto em `~/.claude/skills/summarize-changes/SKILL.md`:

    ```yaml theme={null}
    ---
    description: Summarizes uncommitted changes and flags anything risky. Use when the user asks what changed, wants a commit message, or asks to review their diff.
    ---

    ## Current changes

    !`git diff HEAD`

    ## Instructions

    Summarize the changes above in two or three bullet points, then list any risks you notice such as missing error handling, hardcoded values, or tests that need updating. If the diff is empty, say there are no uncommitted changes.
    ```

    A linha `` !`git diff HEAD` `` usa [injeção de contexto dinâmico](#inject-dynamic-context): Claude Code executa o comando e substitui a linha por sua saída antes de Claude ver o conteúdo da skill, então as instruções chegam com o diff atual já embutido.
  </Step>

  <Step title="Teste a skill">
    Abra um projeto git, faça uma pequena edição em qualquer arquivo e inicie Claude Code executando `claude`. Você pode testar a skill de duas formas.

    **Deixe Claude invocá-la automaticamente** perguntando algo que corresponda à descrição:

    ```text theme={null}
    What did I change?
    ```

    **Ou invoque-a diretamente** com o nome da skill:

    ```text theme={null}
    /summarize-changes
    ```

    De qualquer forma, Claude deve responder com um breve resumo de sua edição e uma lista de riscos.
  </Step>
</Steps>

<h3 id="where-skills-live">
  Onde as skills vivem
</h3>

Onde você armazena uma skill determina quem pode usá-la:

| Localização | Caminho                                                           | Aplica-se a                          |
| :---------- | :---------------------------------------------------------------- | :----------------------------------- |
| Enterprise  | Consulte [configurações gerenciadas](/docs/pt/settings#settings-files) | Todos os usuários em sua organização |
| Pessoal     | `~/.claude/skills/<skill-name>/SKILL.md`                          | Todos os seus projetos               |
| Projeto     | `.claude/skills/<skill-name>/SKILL.md`                            | Apenas este projeto                  |
| Plugin      | `<plugin>/skills/<skill-name>/SKILL.md`                           | Onde o plugin está habilitado        |

Quando skills compartilham o mesmo nome em diferentes níveis, enterprise substitui pessoal, e pessoal substitui projeto. Uma skill em qualquer um desses níveis também substitui uma skill agrupada com o mesmo nome. Por exemplo, uma skill `code-review` na `.claude/skills/` do seu projeto substitui a `/code-review` agrupada. Skills de plugin usam um namespace `plugin-name:skill-name`, então não podem conflitar com outros níveis. Se você tem arquivos em `.claude/commands/`, eles funcionam da mesma forma, mas se uma skill e um comando compartilham o mesmo nome, a skill tem precedência.

Skills também carregam de diretórios `.claude/skills/` aninhados abaixo de seu diretório de trabalho. Quando Claude lê ou edita um arquivo em um subdiretório, skills do `.claude/skills/` desse subdiretório se tornam disponíveis. Isso permite que um pacote de monorepo forneça suas próprias skills que se aplicam ao trabalhar nesse pacote, mesmo que a sessão tenha começado na raiz do repositório.

Se uma skill aninhada compartilha um nome com outra skill, ambas permanecem disponíveis. Por exemplo, com uma skill `deploy` na raiz do projeto e outra em `apps/web/.claude/skills/`:

* A aninhada aparece sob um nome qualificado por diretório, `apps/web:deploy`.
* Sua descrição diz qual diretório ela se aplica.
* Claude escolhe a variante que corresponde aos arquivos em que está trabalhando.

Digitar `/deploy` executa a skill da raiz do projeto. Digite o nome qualificado `/apps/web:deploy` para executar a variante aninhada explicitamente.

Quando você ou Claude invocam o nome não qualificado, a skill da raiz do projeto carrega, e Claude Code anexa uma lista das variantes qualificadas por diretório ao seu conteúdo com uma instrução para também invocar qualquer variante cujo diretório contenha os arquivos em que Claude está trabalhando. Uma skill aninhada, portanto, ainda se aplica ao trabalho em seu diretório quando apenas o nome não qualificado é invocado. Requer Claude Code v2.1.203 ou posterior.

Uma entrada `<skill-name>` nas localizações enterprise, pessoal ou projeto pode ser um symlink para um diretório em outro lugar no disco. Claude Code segue o symlink e lê `SKILL.md` do diretório de destino, e se o mesmo destino for acessível de mais de um local, Claude Code carrega a skill uma vez. Skills de plugin lidam com symlinks de forma diferente; consulte [Compartilhe arquivos dentro de um marketplace com symlinks](/docs/pt/plugins-reference#share-files-within-a-marketplace-with-symlinks).

<Note>
  Adicione um `.claude-plugin/plugin.json` a uma pasta de skill e ela carrega como um [plugin](/docs/pt/plugins-reference#skills-directory-plugins) nomeado `<name>@skills-dir`, para que possa agrupar agents, hooks e servidores MCP. Em um `.claude/skills/` de projeto, isso requer aceitar o diálogo de confiança do workspace primeiro.
</Note>

<h4 id="live-change-detection">
  Detecção de mudança ao vivo
</h4>

Claude Code observa diretórios de skills para mudanças de arquivo. Adicionar, editar ou remover uma skill em `~/.claude/skills/`, o projeto `.claude/skills/`, ou um `.claude/skills/` dentro de um diretório `--add-dir` entra em efeito dentro da sessão atual sem reiniciar. Criar um diretório de skills de nível superior que não existia quando a sessão começou requer reiniciar Claude Code para que o novo diretório possa ser observado.

<Note>
  A detecção de mudança ao vivo cobre apenas o texto `SKILL.md`. Para uma pasta de skill que também é um [plugin](/docs/pt/plugins-reference#skills-directory-plugins), mudanças em `hooks/`, `.mcp.json`, `agents/` e `output-styles/` precisam de `/reload-plugins` para entrar em efeito.
</Note>

<h4 id="automatic-discovery-from-parent-and-nested-directories">
  Descoberta automática de diretórios aninhados e pais
</h4>

Project skills carregam de `.claude/skills/` em seu diretório inicial e em cada diretório pai até a raiz do repositório, então iniciar Claude em um subdiretório ainda pega skills definidas na raiz. Quando você trabalha com arquivos em subdiretórios abaixo de seu diretório inicial, Claude Code também descobre skills de diretórios `.claude/skills/` aninhados sob demanda. Por exemplo, se você está editando um arquivo em `packages/frontend/`, Claude Code também procura por skills em `packages/frontend/.claude/skills/`. Isso suporta configurações de monorepo onde pacotes têm suas próprias skills.

Cada skill é um diretório com `SKILL.md` como ponto de entrada:

```text theme={null}
my-skill/
├── SKILL.md           # Instruções principais (obrigatório)
├── template.md        # Template para Claude preencher
├── examples/
│   └── sample.md      # Exemplo de saída mostrando formato esperado
└── scripts/
    └── validate.sh    # Script que Claude pode executar
```

O `SKILL.md` contém as instruções principais e é obrigatório. Outros arquivos são opcionais e permitem que você construa skills mais poderosas: templates para Claude preencher, exemplos de saída mostrando o formato esperado, scripts que Claude pode executar, ou documentação de referência detalhada. Referencie esses arquivos de seu `SKILL.md` para que Claude saiba o que eles contêm e quando carregá-los. Consulte [Adicione arquivos de suporte](#add-supporting-files) para mais detalhes.

<Note>
  Arquivos em `.claude/commands/` ainda funcionam e suportam o mesmo [frontmatter](#frontmatter-reference). Skills são recomendadas já que suportam recursos adicionais como arquivos de suporte.
</Note>

<h4 id="skills-from-additional-directories">
  Skills de diretórios adicionais
</h4>

O sinalizador `--add-dir` e o comando `/add-dir` [concedem acesso a arquivos](/docs/pt/permissions#additional-directories-grant-file-access-not-configuration) em vez de descoberta de configuração, mas skills são uma exceção: `.claude/skills/` dentro de um diretório adicionado é carregado automaticamente. Esta exceção se aplica apenas a `--add-dir` e `/add-dir`. A configuração `permissions.additionalDirectories` em `settings.json` concede acesso a arquivos apenas e não carrega skills. Consulte [Detecção de mudança ao vivo](#live-change-detection) para como edições são detectadas durante uma sessão.

Outra configuração `.claude/` como comandos e estilos de saída não é carregada de diretórios adicionais. Consulte a [tabela de exceções](/docs/pt/permissions#additional-directories-grant-file-access-not-configuration) para a lista completa do que é e não é carregado, e as formas recomendadas de compartilhar configuração entre projetos.

<Note>
  Arquivos CLAUDE.md de diretórios `--add-dir` não são carregados por padrão. Para carregá-los, defina `CLAUDE_CODE_ADDITIONAL_DIRECTORIES_CLAUDE_MD=1`. Consulte [Carregar de diretórios adicionais](/docs/pt/memory#load-from-additional-directories).
</Note>

<h2 id="configure-skills">
  Configurar skills
</h2>

Skills são configuradas através de frontmatter YAML no topo de `SKILL.md` e o conteúdo markdown que segue.

<h3 id="types-of-skill-content">
  Tipos de conteúdo de skill
</h3>

Arquivos de skill podem conter qualquer instrução, mas pensar em como você quer invocá-los ajuda a guiar o que incluir:

**Conteúdo de referência** adiciona conhecimento que Claude aplica ao seu trabalho atual. Convenções, padrões, guias de estilo, conhecimento de domínio. Este conteúdo é executado inline para que Claude possa usá-lo junto com seu contexto de conversa.

```yaml theme={null}
---
name: api-conventions
description: API design patterns for this codebase
---

When writing API endpoints:
- Use RESTful naming conventions
- Return consistent error formats
- Include request validation
```

**Conteúdo de tarefa** dá ao Claude instruções passo a passo para uma ação específica, como implantações, commits ou geração de código. Estas são frequentemente ações que você quer invocar diretamente com `/skill-name` em vez de deixar Claude decidir quando executá-las. Adicione `disable-model-invocation: true` para evitar que Claude a dispare automaticamente.

```yaml theme={null}
---
name: deploy
description: Deploy the application to production
context: fork
disable-model-invocation: true
---

Deploy the application:
1. Run the test suite
2. Build the application
3. Push to the deployment target
```

Seu `SKILL.md` pode conter qualquer coisa, mas pensar em como você quer que a skill seja invocada (por você, por Claude, ou ambos) e onde você quer que seja executada (inline ou em um subagent) ajuda a guiar o que incluir. Para skills complexas, você também pode [adicionar arquivos de suporte](#add-supporting-files) para manter a skill principal focada.

Mantenha o corpo em si conciso. Uma vez que uma skill carrega, seu conteúdo [permanece em contexto entre turnos](#skill-content-lifecycle), então cada linha é um custo de token recorrente. Declare o que fazer em vez de narrar como ou por que, e aplique o mesmo teste de concisão que você faria para [conteúdo de CLAUDE.md](/docs/pt/best-practices#write-an-effective-claude-md).

<h3 id="frontmatter-reference">
  Referência de frontmatter
</h3>

Além do conteúdo markdown, você pode configurar o comportamento da skill usando campos de frontmatter YAML entre marcadores `---` no topo de seu arquivo `SKILL.md`:

```yaml theme={null}
---
name: my-skill
description: What this skill does
disable-model-invocation: true
allowed-tools: Read Grep
---

Your skill instructions here...
```

Todos os campos são opcionais. Apenas `description` é recomendado para que Claude saiba quando usar a skill.

| Campo                      | Obrigatório | Descrição                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| :------------------------- | :---------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `name`                     | Não         | Nome de exibição mostrado em listagens de skills. Padrão é o nome do diretório. Consulte [Como uma skill obtém seu nome de comando](#how-a-skill-gets-its-command-name) para como isso difere do nome que você digita para invocar a skill.                                                                                                                                                                                                                    |
| `description`              | Recomendado | O que a skill faz e quando usá-la. Claude usa isso para decidir quando aplicar a skill. Se omitido, usa o primeiro parágrafo do conteúdo markdown. Coloque o caso de uso principal na frente: o texto combinado de `description` e `when_to_use` é truncado em 1.536 caracteres na listagem de skills para reduzir o uso de contexto.                                                                                                                          |
| `when_to_use`              | Não         | Contexto adicional para quando Claude deve invocar a skill, como frases de gatilho ou solicitações de exemplo. Anexado a `description` na listagem de skills e conta para o limite de 1.536 caracteres.                                                                                                                                                                                                                                                        |
| `argument-hint`            | Não         | Dica mostrada durante autocomplete para indicar argumentos esperados. Exemplo: `[issue-number]` ou `[filename] [format]`.                                                                                                                                                                                                                                                                                                                                      |
| `arguments`                | Não         | Argumentos posicionais nomeados para [substituição `$name`](#available-string-substitutions) no conteúdo da skill. Aceita uma string separada por espaços ou uma lista YAML. Nomes mapeiam para posições de argumento em ordem.                                                                                                                                                                                                                                |
| `disable-model-invocation` | Não         | Defina como `true` para evitar que Claude carregue automaticamente esta skill. Use para fluxos de trabalho que você quer disparar manualmente com `/name`. Também evita que a skill seja [pré-carregada em subagents](/docs/pt/sub-agents#preload-skills-into-subagents). A partir da v2.1.196, também evita que a skill seja executada quando uma [tarefa agendada](/docs/pt/scheduled-tasks) dispara com a skill como seu prompt. Padrão: `false`.                     |
| `user-invocable`           | Não         | Defina como `false` para ocultar do menu `/`. Use para conhecimento de fundo que usuários não devem invocar diretamente. Padrão: `true`.                                                                                                                                                                                                                                                                                                                       |
| `allowed-tools`            | Não         | Ferramentas que Claude pode usar sem pedir permissão quando esta skill está ativa. Aceita uma string separada por espaços ou vírgulas, ou uma lista YAML.                                                                                                                                                                                                                                                                                                      |
| `disallowed-tools`         | Não         | Ferramentas removidas do pool disponível de Claude enquanto esta skill está ativa. Use para skills autônomas que nunca devem chamar certas ferramentas, como `AskUserQuestion` para um loop de fundo. Aceita uma string separada por espaços ou vírgulas, ou uma lista YAML. A restrição é limpa quando você envia sua próxima mensagem.                                                                                                                       |
| `model`                    | Não         | Modelo a usar quando esta skill está ativa. A sobrescrita se aplica pelo resto da volta atual e não é salva em configurações; o modelo de sessão retoma em seu próximo prompt. Aceita os mesmos valores que [`/model`](/docs/pt/model-config), ou `inherit` para manter o modelo ativo. Um valor excluído pela lista de permissão [`availableModels`](/docs/pt/model-config#restrict-model-selection) da sua organização não é usado e a sessão mantém seu modelo atual. |
| `effort`                   | Não         | [Nível de esforço](/docs/pt/model-config#adjust-effort-level) quando esta skill está ativa. Sobrescreve o nível de esforço da sessão. Padrão: herda da sessão. Opções: `low`, `medium`, `high`, `xhigh`, `max`; os níveis disponíveis dependem do modelo.                                                                                                                                                                                                           |
| `context`                  | Não         | Defina como `fork` para executar em um contexto de subagent bifurcado.                                                                                                                                                                                                                                                                                                                                                                                         |
| `agent`                    | Não         | Qual tipo de subagent usar quando `context: fork` está definido.                                                                                                                                                                                                                                                                                                                                                                                               |
| `hooks`                    | Não         | Hooks com escopo para o ciclo de vida desta skill. Consulte [Hooks em skills e agents](/docs/pt/hooks#hooks-in-skills-and-agents) para formato de configuração.                                                                                                                                                                                                                                                                                                     |
| `paths`                    | Não         | Padrões glob que limitam quando esta skill é ativada. Aceita uma string separada por vírgulas ou uma lista YAML. Quando definido, Claude carrega a skill automaticamente apenas ao trabalhar com arquivos que correspondem aos padrões. Usa o mesmo formato que [regras específicas de caminho](/docs/pt/memory#path-specific-rules).                                                                                                                               |
| `shell`                    | Não         | Shell a usar para `` !`command` `` e blocos ` ```! ` nesta skill. Aceita `bash` (padrão) ou `powershell`. Definir `powershell` executa comandos shell inline via PowerShell no Windows. Requer `CLAUDE_CODE_USE_POWERSHELL_TOOL=1`.                                                                                                                                                                                                                            |

<h4 id="how-a-skill-gets-its-command-name">
  Como uma skill obtém seu nome de comando
</h4>

O comando que você digita para invocar uma skill vem de onde o arquivo de skill reside. O campo frontmatter `name` define o rótulo de exibição mostrado em listagens de skills e, exceto para um `SKILL.md` raiz de plugin, não muda o que você digita após `/`.

A tabela abaixo mostra de onde o nome do comando vem para cada layout:

| Localização da skill                                                                                        | Fonte do nome do comando                                                                       | Exemplo                                                                                                                                                 |
| :---------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Diretório de skill sob `~/.claude/skills/` ou `.claude/skills/`                                             | Nome do diretório                                                                              | `.claude/skills/deploy-staging/SKILL.md` → `/deploy-staging`                                                                                            |
| [Diretório `.claude/skills/` aninhado](#where-skills-live), quando o nome entra em conflito com outra skill | Caminho do subdiretório relativo ao diretório de trabalho, depois o nome do diretório de skill | `apps/web/.claude/skills/deploy/SKILL.md` → `/apps/web:deploy`                                                                                          |
| Arquivo sob `.claude/commands/`                                                                             | Nome do arquivo sem extensão                                                                   | `.claude/commands/deploy.md` → `/deploy`                                                                                                                |
| Subdiretório `skills/` do plugin                                                                            | Nome do diretório, com namespace pelo plugin                                                   | `my-plugin/skills/review/SKILL.md` → `/my-plugin:review`                                                                                                |
| `SKILL.md` raiz do plugin                                                                                   | Frontmatter `name`, com o nome do diretório do plugin como fallback                            | `my-plugin/SKILL.md` com `name: review` → `/my-plugin:review`. Consulte [Regras de comportamento de caminho](/docs/pt/plugins-reference#path-behavior-rules) |

O caso raiz do plugin é o único lugar onde `name` define o nome do comando, porque não há diretório de skill para obtê-lo. Se `name` não estiver definido no frontmatter, o nome do diretório do plugin é usado em seu lugar.

<h4 id="available-string-substitutions">
  Substituições de string disponíveis
</h4>

Skills suportam substituição de string para valores dinâmicos no conteúdo da skill:

| Variável                | Descrição                                                                                                                                                                                                                                                                                                                         |
| :---------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `$ARGUMENTS`            | Todos os argumentos passados ao invocar a skill. Se `$ARGUMENTS` não estiver presente no conteúdo, argumentos são anexados como `ARGUMENTS: <value>`.                                                                                                                                                                             |
| `$ARGUMENTS[N]`         | Acesse um argumento específico por índice baseado em 0, como `$ARGUMENTS[0]` para o primeiro argumento.                                                                                                                                                                                                                           |
| `$N`                    | Abreviação para `$ARGUMENTS[N]`, como `$0` para o primeiro argumento ou `$1` para o segundo.                                                                                                                                                                                                                                      |
| `$name`                 | Argumento nomeado declarado na lista de frontmatter [`arguments`](#frontmatter-reference). Nomes mapeiam para posições em ordem, então com `arguments: [issue, branch]` o placeholder `$issue` expande para o primeiro argumento e `$branch` para o segundo.                                                                      |
| `${CLAUDE_SESSION_ID}`  | O ID da sessão atual. Útil para logging, criação de arquivos específicos da sessão, ou correlação de saída de skill com sessões.                                                                                                                                                                                                  |
| `${CLAUDE_EFFORT}`      | O nível de esforço atual: `low`, `medium`, `high`, `xhigh`, ou `max`. Ultracode não é um nível distinto e é relatado como `xhigh`. Use isso para adaptar instruções de skill à configuração de esforço ativo.                                                                                                                     |
| `${CLAUDE_SKILL_DIR}`   | O diretório contendo o arquivo `SKILL.md` da skill. Para skills de plugin, este é o subdiretório da skill dentro do plugin, não a raiz do plugin. Use isso em comandos de injeção bash para referenciar scripts ou arquivos agrupados com a skill, independentemente do diretório de trabalho atual.                              |
| `${CLAUDE_PROJECT_DIR}` | O diretório raiz do projeto. Este é o mesmo caminho que [hooks](/docs/pt/hooks#reference-scripts-by-path) e servidores MCP recebem como `CLAUDE_PROJECT_DIR`. Use isso para referenciar scripts ou arquivos locais do projeto, como `${CLAUDE_PROJECT_DIR}/.claude/hooks/helper.sh`, independentemente de onde a skill está instalada. |

A substituição `${CLAUDE_PROJECT_DIR}` requer Claude Code v2.1.196 ou posterior. Ela se aplica tanto ao corpo da skill quanto ao frontmatter [`allowed-tools`](#frontmatter-reference), para que uma regra de permissão como `Bash(${CLAUDE_PROJECT_DIR}/scripts/lint.sh *)` se resolva para o mesmo caminho que o corpo da skill usa.

Argumentos indexados usam quoting no estilo shell, então envolva valores de múltiplas palavras em aspas para passá-los como um único argumento. Por exemplo, `/my-skill "hello world" second` faz `$0` expandir para `hello world` e `$1` para `second`. O placeholder `$ARGUMENTS` sempre expande para a string de argumento completa conforme digitada.

Para incluir um `$` literal antes de um dígito, `ARGUMENTS`, ou um nome de argumento declarado, como `$1.00` em prosa, escape-o com uma barra invertida: `\$1.00`. Uma barra invertida antes de qualquer outro `$` é deixada inalterada. Apenas uma única barra invertida diretamente antes do token a escapa. Uma barra invertida duplicada como `\\$1` deixa ambas as barras invertidas no lugar, e `$1` ainda expande para o valor do argumento.

**Exemplo usando substituições:**

```yaml theme={null}
---
name: session-logger
description: Log activity for this session
---

Log the following to logs/${CLAUDE_SESSION_ID}.log:

$ARGUMENTS
```

<h3 id="add-supporting-files">
  Adicione arquivos de suporte
</h3>

Skills podem incluir múltiplos arquivos em seu diretório. Isso mantém `SKILL.md` focado no essencial enquanto deixa Claude acessar material de referência detalhado apenas quando necessário. Documentos de referência grandes, especificações de API, ou coleções de exemplos não precisam carregar em contexto toda vez que a skill é executada.

```text theme={null}
my-skill/
├── SKILL.md (obrigatório - visão geral e navegação)
├── reference.md (documentação de API detalhada - carregada quando necessário)
├── examples.md (exemplos de uso - carregados quando necessário)
└── scripts/
    └── helper.py (script utilitário - executado, não carregado)
```

Referencie arquivos de suporte de `SKILL.md` para que Claude saiba o que cada arquivo contém e quando carregá-lo:

```markdown theme={null}
## Additional resources

- For complete API details, see [reference.md](reference.md)
- For usage examples, see [examples.md](examples.md)
```

<Tip>Mantenha `SKILL.md` com menos de 500 linhas. Mova material de referência detalhado para arquivos separados.</Tip>

<h3 id="control-who-invokes-a-skill">
  Controle quem invoca uma skill
</h3>

Por padrão, tanto você quanto Claude podem invocar qualquer skill. Você pode digitar `/skill-name` para invocá-la diretamente, e Claude pode carregá-la automaticamente quando relevante para sua conversa. Dois campos de frontmatter permitem que você restrinja isso:

* **`disable-model-invocation: true`**: Apenas você pode invocar a skill. Use isso para fluxos de trabalho com efeitos colaterais ou que você quer controlar o tempo, como `/commit`, `/deploy`, ou `/send-slack-message`. Você não quer que Claude decida fazer deploy porque seu código parece pronto.

* **`user-invocable: false`**: Apenas Claude pode invocar a skill. Use isso para conhecimento de fundo que não é acionável como um comando. Uma skill `legacy-system-context` explica como um sistema antigo funciona. Claude deve saber disso quando relevante, mas `/legacy-system-context` não é uma ação significativa para usuários tomarem.

Este exemplo cria uma skill de deploy que apenas você pode disparar. Se você definir `disable-model-invocation: true`, Claude não pode executar a skill automaticamente:

```yaml theme={null}
---
name: deploy
description: Deploy the application to production
disable-model-invocation: true
---

Deploy $ARGUMENTS to production:

1. Run the test suite
2. Build the application
3. Push to the deployment target
4. Verify the deployment succeeded
```

Aqui está como os dois campos afetam invocação e carregamento de contexto:

| Frontmatter                      | Você pode invocar | Claude pode invocar | Quando carregado em contexto                                         |
| :------------------------------- | :---------------- | :------------------ | :------------------------------------------------------------------- |
| (padrão)                         | Sim               | Sim                 | Descrição sempre em contexto, skill completa carrega quando invocada |
| `disable-model-invocation: true` | Sim               | Não                 | Descrição não em contexto, skill completa carrega quando você invoca |
| `user-invocable: false`          | Não               | Sim                 | Descrição sempre em contexto, skill completa carrega quando invocada |

<Note>
  Em uma sessão regular, descrições de skills são carregadas em contexto para que Claude saiba o que está disponível, mas conteúdo completo de skill apenas carrega quando invocado. [Subagents com skills pré-carregadas](/docs/pt/sub-agents#preload-skills-into-subagents) funcionam diferentemente: o conteúdo completo da skill é injetado na inicialização.
</Note>

<h3 id="skill-content-lifecycle">
  Ciclo de vida do conteúdo de skill
</h3>

Quando você ou Claude invoca uma skill, o conteúdo `SKILL.md` renderizado entra na conversa como uma única mensagem e permanece lá pelo resto da sessão. Claude Code não relê o arquivo de skill em voltas posteriores, então escreva orientação que deve se aplicar durante uma tarefa como instruções permanentes em vez de etapas únicas.

Quando Claude re-invoca uma skill cujo conteúdo renderizado é idêntico à cópia já em contexto, Claude Code adiciona uma nota breve de que a skill já está carregada em vez de uma segunda cópia do conteúdo. Quando o conteúdo renderizado difere, porque os argumentos mudaram ou um comando de [contexto dinâmico](#inject-dynamic-context) produziu nova saída, Claude Code anexa o conteúdo completo novamente. Antes da v2.1.202, cada re-invocação anexava outra cópia completa das instruções da skill.

[Auto-compactação](/docs/pt/how-claude-code-works#when-context-fills-up) carrega skills invocadas para frente dentro de um orçamento de token. Quando a conversa é resumida para liberar contexto, Claude Code reanexa a invocação mais recente de cada skill após o resumo, mantendo os primeiros 5.000 tokens de cada. Skills reanexadas compartilham um orçamento combinado de 25.000 tokens. Claude Code preenche este orçamento começando da skill invocada mais recentemente, então skills mais antigas podem ser descartadas inteiramente após compactação se você invocou muitas em uma sessão.

Se uma skill parece parar de influenciar comportamento após a primeira resposta, o conteúdo geralmente ainda está presente e o modelo está escolhendo outras ferramentas ou abordagens. Fortaleça a `description` da skill e instruções para que o modelo continue preferindo-a, ou use [hooks](/docs/pt/hooks) para impor comportamento deterministicamente. Se a skill é grande ou você invocou várias outras depois dela, re-invoque-a após compactação para restaurar o conteúdo completo.

<h3 id="pre-approve-tools-for-a-skill">
  Pré-aprove ferramentas para uma skill
</h3>

O campo `allowed-tools` concede permissão para as ferramentas listadas enquanto a skill está ativa, para que Claude possa usá-las sem solicitar sua aprovação. Ele não restringe quais ferramentas estão disponíveis: cada ferramenta permanece chamável, e suas [configurações de permissão](/docs/pt/permissions) ainda governam ferramentas que não estão listadas.

Para skills verificadas em um diretório `.claude/skills/` de um projeto, `allowed-tools` entra em vigor após você aceitar o diálogo de confiança do workspace para essa pasta, o mesmo que regras de permissão em `.claude/settings.json`. Revise skills de projeto antes de confiar em um repositório, já que uma skill pode conceder a si mesma acesso amplo a ferramentas.

Esta skill deixa Claude executar comandos git sem aprovação por uso sempre que você invoca:

```yaml theme={null}
---
name: commit
description: Stage and commit the current changes
disable-model-invocation: true
allowed-tools: Bash(git add *) Bash(git commit *) Bash(git status *)
---
```

Para remover ferramentas do pool disponível de Claude enquanto uma skill está ativa, liste-as em `disallowed-tools` no frontmatter da skill. A restrição é limpa quando você envia sua próxima mensagem. Para bloquear ferramentas em todas as skills e prompts, adicione regras de negação em suas [configurações de permissão](/docs/pt/permissions).

<h3 id="pass-arguments-to-skills">
  Passe argumentos para skills
</h3>

Tanto você quanto Claude podem passar argumentos ao invocar uma skill. Argumentos estão disponíveis via placeholder `$ARGUMENTS`.

Esta skill corrige um problema do GitHub por número. O placeholder `$ARGUMENTS` é substituído por qualquer coisa que siga o nome da skill:

```yaml theme={null}
---
name: fix-issue
description: Fix a GitHub issue
disable-model-invocation: true
---

Fix GitHub issue $ARGUMENTS following our coding standards.

1. Read the issue description
2. Understand the requirements
3. Implement the fix
4. Write tests
5. Create a commit
```

Quando você executa `/fix-issue 123`, Claude recebe "Fix GitHub issue 123 following our coding standards..."

Se você invocar uma skill com argumentos mas a skill não incluir `$ARGUMENTS`, Claude Code anexa `ARGUMENTS: <your input>` ao final do conteúdo da skill para que Claude ainda veja o que você digitou.

Você também pode empilhar várias skills no início de uma mensagem. A partir da v2.1.199, digitar `/code-review /fix-issue 123` carrega ambas as skills e passa o texto final `123` como `$ARGUMENTS` para cada uma delas. Em versões anteriores, apenas a primeira skill carregava e recebia `/fix-issue 123` como texto de argumento literal.

Claude Code expande a primeira skill mais até cinco mais empilhadas depois dela. A expansão para quando o primeiro token não é uma skill invocável pelo usuário inline, então uma skill que é executada como um [subagent bifurcado](#run-skills-in-a-subagent) ou uma cujos argumentos podem começar com um comando slash, como `/loop`, também termina ali; esse token e tudo depois dele se tornam o texto de argumento para cada skill expandida.

Para acessar argumentos individuais por posição, use `$ARGUMENTS[N]` ou a forma mais curta `$N`:

```yaml theme={null}
---
name: migrate-component
description: Migrate a component from one framework to another
---

Migrate the $ARGUMENTS[0] component from $ARGUMENTS[1] to $ARGUMENTS[2].
Preserve all existing behavior and tests.
```

Executar `/migrate-component SearchBar React Vue` substitui `$ARGUMENTS[0]` com `SearchBar`, `$ARGUMENTS[1]` com `React`, e `$ARGUMENTS[2]` com `Vue`. A mesma skill usando a abreviação `$N`:

```yaml theme={null}
---
name: migrate-component
description: Migrate a component from one framework to another
---

Migrate the $0 component from $1 to $2.
Preserve all existing behavior and tests.
```

<h2 id="advanced-patterns">
  Padrões avançados
</h2>

<h3 id="inject-dynamic-context">
  Injete contexto dinâmico
</h3>

A sintaxe `` !`<command>` `` executa comandos shell antes do conteúdo da skill ser enviado para Claude. A saída do comando substitui o placeholder, para que Claude receba dados reais, não o comando em si.

Esta skill resume um pull request buscando dados de PR ao vivo com o GitHub CLI. Os comandos `` !`gh pr diff` `` e outros são executados primeiro, e sua saída é inserida no prompt:

```yaml theme={null}
---
name: pr-summary
description: Summarize changes in a pull request
context: fork
agent: Explore
allowed-tools: Bash(gh *)
---

## Pull request context
- PR diff: !`gh pr diff`
- PR comments: !`gh pr view --comments`
- Changed files: !`gh pr diff --name-only`

## Your task
Summarize this pull request...
```

Quando esta skill é executada:

1. Cada `` !`<command>` `` é executado imediatamente (antes de Claude ver qualquer coisa)
2. A saída substitui o placeholder no conteúdo da skill
3. Claude recebe o prompt totalmente renderizado com dados reais de PR

Isto é pré-processamento, não algo que Claude executa. Claude apenas vê o resultado final.

A substituição é executada uma vez sobre o arquivo original. A saída do comando é inserida como texto simples e não é verificada novamente para placeholders `` !`<command>` `` adicionais, portanto um comando não pode emitir um placeholder para uma passagem posterior expandir.

A forma inline é reconhecida apenas quando `!` aparece no início de uma linha ou imediatamente após espaço em branco. Se `!` segue outro caractere, como em `` KEY=!`cmd` ``, o placeholder é deixado como texto literal e o comando não é executado.

Para comandos de múltiplas linhas, use um bloco de código cercado aberto com ` ```! ` em vez da forma inline:

````markdown theme={null}
## Environment
```!
node --version
npm --version
git status --short
```
````

Para desabilitar este comportamento para skills e comandos personalizados de fontes de usuário, projeto, plugin ou [diretório adicional](#skills-from-additional-directories), defina `"disableSkillShellExecution": true` em [configurações](/docs/pt/settings). Cada comando é substituído com `[shell command execution disabled by policy]` em vez de ser executado. Skills agrupadas e gerenciadas não são afetadas. Esta configuração é mais útil em [configurações gerenciadas](/docs/pt/permissions#managed-settings), onde usuários não podem sobrescrevê-la.

<Tip>
  Para solicitar raciocínio mais profundo quando uma skill é executada, inclua `ultrathink` em qualquer lugar no conteúdo da skill. Consulte [Use ultrathink for one-off deep reasoning](/docs/pt/model-config#use-ultrathink-for-one-off-deep-reasoning).
</Tip>

<h3 id="run-skills-in-a-subagent">
  Execute skills em um subagent
</h3>

Adicione `context: fork` ao seu frontmatter quando você quer que uma skill seja executada em isolamento. O conteúdo da skill se torna o prompt que dirige o subagent. Ele não terá acesso ao seu histórico de conversa.

<Warning>
  `context: fork` apenas faz sentido para skills com instruções explícitas. Se sua skill contém diretrizes como "use estas convenções de API" sem uma tarefa, o subagent recebe as diretrizes mas nenhum prompt acionável, e retorna sem saída significativa.
</Warning>

Skills e [subagents](/docs/pt/sub-agents) trabalham juntos em duas direções:

| Abordagem                   | Prompt do sistema          | Tarefa                          | Também carrega                                     |
| :-------------------------- | :------------------------- | :------------------------------ | :------------------------------------------------- |
| Skill com `context: fork`   | Do tipo de agent           | Conteúdo de SKILL.md            | CLAUDE.md, exceto quando o agent é Explore ou Plan |
| Subagent com campo `skills` | Corpo markdown do subagent | Mensagem de delegação do Claude | Skills pré-carregadas + CLAUDE.md                  |

Com `context: fork`, você escreve a tarefa em sua skill e escolhe um tipo de agent para executá-la. Os agents integrados Explore e Plan [pulam CLAUDE.md e git status](/docs/pt/sub-agents#what-loads-at-startup) para manter seu contexto pequeno, portanto uma skill bifurcada usando `agent: Explore` vê apenas o conteúdo de SKILL.md e o prompt do sistema do próprio agent. Para o inverso, onde você define um subagent personalizado que usa skills como material de referência, consulte [Subagents](/docs/pt/sub-agents#preload-skills-into-subagents).

<h4 id="example-research-skill-using-explore-agent">
  Exemplo: Skill de pesquisa usando agent Explore
</h4>

Esta skill executa pesquisa em um agent Explore bifurcado. O conteúdo da skill se torna a tarefa, e o agent fornece ferramentas somente leitura otimizadas para exploração de codebase:

```yaml theme={null}
---
name: deep-research
description: Research a topic thoroughly
context: fork
agent: Explore
---

Research $ARGUMENTS thoroughly:

1. Find relevant files using Glob and Grep
2. Read and analyze the code
3. Summarize findings with specific file references
```

Quando esta skill é executada:

1. Um novo contexto isolado é criado
2. O subagent recebe o conteúdo da skill como seu prompt ("Research \$ARGUMENTS thoroughly...")
3. O campo `agent` determina o ambiente de execução (modelo, ferramentas e permissões)
4. Resultados são resumidos e retornados para sua conversa principal

O campo `agent` especifica qual configuração de subagent usar. As opções incluem agents integrados (`Explore`, `Plan`, `general-purpose`) ou qualquer subagent personalizado de `.claude/agents/`. Se omitido, usa `general-purpose`.

<h3 id="restrict-claude’s-skill-access">
  Restrinja acesso de skill do Claude
</h3>

Por padrão, Claude pode invocar qualquer skill que não tenha `disable-model-invocation: true` definido. Skills que definem `allowed-tools` concedem a Claude acesso a essas ferramentas sem aprovação por uso quando a skill está ativa. Suas [configurações de permissão](/docs/pt/permissions) ainda governam comportamento de aprovação de linha de base para todas as outras ferramentas. Alguns comandos integrados também estão disponíveis através da ferramenta Skill, incluindo `/init`, `/review`, e `/security-review`. Outros comandos integrados como `/compact` não estão.

Três formas de controlar quais skills Claude pode invocar:

**Desabilite todas as skills** negando a ferramenta Skill em `/permissions`:

```text theme={null}
# Add to deny rules:
Skill
```

**Permita ou negue skills específicas** usando [regras de permissão](/docs/pt/permissions):

```text theme={null}
# Allow only specific skills
Skill(commit)
Skill(review-pr *)

# Deny specific skills
Skill(deploy *)
```

Sintaxe de permissão: `Skill(name)` para correspondência exata, `Skill(name *)` para correspondência de prefixo com qualquer argumento.

**Oculte skills individuais** adicionando `disable-model-invocation: true` ao seu frontmatter. Isso remove a skill do contexto do Claude inteiramente.

<Note>
  O campo `user-invocable` apenas controla visibilidade de menu, não acesso à ferramenta Skill. Use `disable-model-invocation: true` para bloquear invocação programática.
</Note>

<h3 id="override-skill-visibility-from-settings">
  Substitua visibilidade de skill a partir de configurações
</h3>

A configuração `skillOverrides` controla visibilidade de skill a partir de suas [configurações](/docs/pt/settings) em vez do frontmatter da própria skill. Use-a para skills cujo SKILL.md você não quer editar, como aquelas verificadas em um repositório de projeto compartilhado ou fornecidas por um servidor MCP. O menu `/skills` escreve para você: destaque uma skill e pressione `Space` para alternar estados, depois `Enter` para salvar em `.claude/settings.local.json`.

Cada chave é um nome de skill e cada valor é um de quatro estados:

| Valor                   | Listado para Claude | No menu `/` |
| :---------------------- | :------------------ | :---------- |
| `"on"`                  | Nome e descrição    | Sim         |
| `"name-only"`           | Apenas nome         | Sim         |
| `"user-invocable-only"` | Oculto              | Sim         |
| `"off"`                 | Oculto              | Oculto      |

A partir da v2.1.199, `"off"` também oculta a skill das listas de comandos anunciadas para clientes [Remote Control](/docs/pt/remote-control) e para chamadores [Agent SDK](/docs/pt/agent-sdk/slash-commands), não apenas o menu `/` do terminal. Invocar uma skill oculta pelo seu nome completo ainda retorna o erro `skillOverrides` em vez de executá-la.

Uma skill que está ausente de `skillOverrides` é tratada como `"on"`. O exemplo abaixo colapsa uma skill para seu nome e desativa outra inteiramente:

```json theme={null}
{
  "skillOverrides": {
    "legacy-context": "name-only",
    "deploy": "off"
  }
}
```

Skills de plugin não são afetadas por `skillOverrides`. Gerencie aquelas através de `/plugin` em vez disso.

<h2 id="evaluate-and-iterate-on-a-skill">
  Avalie e itere em uma skill
</h2>

Ver uma skill disparar diz que Claude a encontrou, não que ela fez o que você pretendia. Para saber que uma skill está funcionando, meça duas coisas separadamente: se Claude a invoca nos prompts que deveria, e se a saída corresponde ao que você espera quando o faz.

A verificação de ambos é uma comparação de linha de base. Colete alguns prompts realistas, execute cada um em uma sessão nova com a skill disponível e novamente com ela [desabilitada](#override-skill-visibility-from-settings), e compare os resultados. Uma sessão nova é importante porque contexto restante da autoria da skill mascarará lacunas nas instruções escritas.

<h3 id="run-evals-with-skill-creator">
  Execute avaliações com skill-creator
</h3>

O [plugin `skill-creator`](https://github.com/anthropics/claude-plugins-official/tree/main/plugins/skill-creator) automatiza o loop de comparação dentro do Claude Code. Instale-o do marketplace oficial:

```text theme={null}
/plugin install skill-creator@claude-plugins-official
```

Se Claude Code relatar que o plugin não é encontrado em nenhum marketplace, seu marketplace está ausente ou desatualizado. Execute `/plugin marketplace update claude-plugins-official` para atualizá-lo, ou `/plugin marketplace add anthropics/claude-plugins-official` se você não o adicionou antes. Depois tente novamente a instalação.

Após instalar, execute `/reload-plugins` para tornar as skills do plugin disponíveis na sessão atual. Depois peça ao Claude para avaliar uma skill existente, por exemplo `evaluate my summarize-changes skill with skill-creator`. O plugin o guia através da escrita de casos de teste e executa o loop:

* **Casos de teste**: armazena prompts, arquivos de entrada e comportamento esperado em `evals/evals.json` dentro do diretório da skill
* **Execuções isoladas**: gera um [subagent](/docs/pt/sub-agents) por caso de teste para que cada execução comece com um contexto limpo, e registra contagem de token e duração
* **Classificação**: verifica cada asserção contra a saída e escreve passar ou falhar com evidência para `grading.json`
* **Benchmark**: agrega taxa de aprovação, tempo e tokens para com-skill versus sem-skill em `benchmark.json` para que você possa comparar a melhoria de taxa de aprovação contra a sobrecarga de token e tempo
* **Comparação de versão**: executa um cego A/B entre duas versões da skill para que você possa confirmar que uma edição é uma melhoria antes de confirmá-la
* **Ajuste de descrição**: gera prompts should-trigger e should-not-trigger, mede a taxa de acerto, e propõe edições de descrição quando a skill se ativa em solicitações erradas
* **Visualizador de revisão**: abre um relatório HTML onde você inspeciona cada saída e registra feedback qualitativo que a próxima iteração lê

Para o formato do arquivo eval e o fluxo de trabalho de iteração completo, consulte [Evaluating skill output quality](https://agentskills.io/skill-creation/evaluating-skills) em agentskills.io. Para contexto sobre o benchmark e modos de comparação, consulte o [anúncio de skill-creator](https://claude.com/blog/improving-skill-creator-test-measure-and-refine-agent-skills).

<h2 id="share-skills">
  Compartilhe skills
</h2>

Skills podem ser distribuídas em diferentes escopos dependendo do seu público:

* **Skills de projeto**: Faça commit de `.claude/skills/` para controle de versão
* **Plugins**: Crie um diretório `skills/` em seu [plugin](/docs/pt/plugins)
* **Gerenciado**: Implante em toda a organização através de [configurações gerenciadas](/docs/pt/settings#settings-files)

<h3 id="generate-visual-output">
  Gere saída visual
</h3>

Skills podem agrupar e executar scripts em qualquer linguagem, dando ao Claude capacidades além do que é possível em um único prompt. Um padrão poderoso é gerar saída visual: arquivos HTML interativos que abrem em seu navegador para explorar dados, depurar ou criar relatórios.

Este exemplo cria um explorador de codebase: uma visualização de árvore interativa onde você pode expandir e recolher diretórios, ver tamanhos de arquivo em um relance, e identificar tipos de arquivo por cor.

Crie o diretório da Skill:

```bash theme={null}
mkdir -p ~/.claude/skills/codebase-visualizer/scripts
```

Salve isto em `~/.claude/skills/codebase-visualizer/SKILL.md`. A descrição diz ao Claude quando ativar esta Skill, e as instruções dizem ao Claude para executar o script agrupado. O caminho do script usa [`${CLAUDE_SKILL_DIR}`](#available-string-substitutions) para que seja resolvido corretamente se a skill estiver instalada no nível pessoal, de projeto ou de plugin:

````yaml theme={null}
---
name: codebase-visualizer
description: Generate an interactive collapsible tree visualization of your codebase. Use when exploring a new repo, understanding project structure, or identifying large files.
allowed-tools: Bash(python3 *)
---

# Codebase Visualizer

Generate an interactive HTML tree view that shows your project's file structure with collapsible directories.

## Usage

Run the visualization script from your project root:

```bash
python3 ${CLAUDE_SKILL_DIR}/scripts/visualize.py .
```

This creates `codebase-map.html` in the current directory and opens it in your default browser.

## What the visualization shows

- **Collapsible directories**: Click folders to expand/collapse
- **File sizes**: Displayed next to each file
- **Colors**: Different colors for different file types
- **Directory totals**: Shows aggregate size of each folder
````

Salve isto em `~/.claude/skills/codebase-visualizer/scripts/visualize.py`. Este script varre uma árvore de diretório e gera um arquivo HTML auto-contido com:

* Uma **barra lateral de resumo** mostrando contagem de arquivos, contagem de diretórios, tamanho total e número de tipos de arquivo
* Um **gráfico de barras** dividindo o codebase por tipo de arquivo (top 8 por tamanho)
* Uma **árvore recolhível** onde você pode expandir e recolher diretórios, com indicadores de tipo de arquivo codificados por cor

O script requer Python 3 mas usa apenas bibliotecas integradas, então não há pacotes para instalar:

```python expandable theme={null}
#!/usr/bin/env python3
"""Generate an interactive collapsible tree visualization of a codebase."""

import json
import sys
import webbrowser
from html import escape
from pathlib import Path
from collections import Counter

IGNORE = {'.git', 'node_modules', '__pycache__', '.venv', 'venv', 'dist', 'build'}

def scan(path: Path, stats: dict) -> dict:
    result = {"name": path.name, "children": [], "size": 0}
    try:
        for item in sorted(path.iterdir()):
            if item.name in IGNORE or item.name.startswith('.'):
                continue
            if item.is_file():
                size = item.stat().st_size
                ext = item.suffix.lower() or '(no ext)'
                result["children"].append({"name": item.name, "size": size, "ext": ext})
                result["size"] += size
                stats["files"] += 1
                stats["extensions"][ext] += 1
                stats["ext_sizes"][ext] += size
            elif item.is_dir():
                stats["dirs"] += 1
                child = scan(item, stats)
                if child["children"]:
                    result["children"].append(child)
                    result["size"] += child["size"]
    except PermissionError:
        pass
    return result

def generate_html(data: dict, stats: dict, output: Path) -> None:
    ext_sizes = stats["ext_sizes"]
    total_size = sum(ext_sizes.values()) or 1
    sorted_exts = sorted(ext_sizes.items(), key=lambda x: -x[1])[:8]
    colors = {
        '.js': '#f7df1e', '.ts': '#3178c6', '.py': '#3776ab', '.go': '#00add8',
        '.rs': '#dea584', '.rb': '#cc342d', '.css': '#264de4', '.html': '#e34c26',
        '.json': '#6b7280', '.md': '#083fa1', '.yaml': '#cb171e', '.yml': '#cb171e',
        '.mdx': '#083fa1', '.tsx': '#3178c6', '.jsx': '#61dafb', '.sh': '#4eaa25',
    }
    lang_bars = "".join(
        f'<div class="bar-row"><span class="bar-label">{ext}</span>'
        f'<div class="bar" style="width:{(size/total_size)*100}%;background:{colors.get(ext,"#6b7280")}"></div>'
        f'<span class="bar-pct">{(size/total_size)*100:.1f}%</span></div>'
        for ext, size in sorted_exts
    )
    def fmt(b):
        if b < 1024: return f"{b} B"
        if b < 1048576: return f"{b/1024:.1f} KB"
        return f"{b/1048576:.1f} MB"

    html = f'''<!DOCTYPE html>
<html><head>
  <meta charset="utf-8"><title>Codebase Explorer</title>
  <style>
    body {{ font: 14px/1.5 system-ui, sans-serif; margin: 0; background: #1a1a2e; color: #eee; }}
    .container {{ display: flex; height: 100vh; }}
    .sidebar {{ width: 280px; background: #252542; padding: 20px; border-right: 1px solid #3d3d5c; overflow-y: auto; flex-shrink: 0; }}
    .main {{ flex: 1; padding: 20px; overflow-y: auto; }}
    h1 {{ margin: 0 0 10px 0; font-size: 18px; }}
    h2 {{ margin: 20px 0 10px 0; font-size: 14px; color: #888; text-transform: uppercase; }}
    .stat {{ display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #3d3d5c; }}
    .stat-value {{ font-weight: bold; }}
    .bar-row {{ display: flex; align-items: center; margin: 6px 0; }}
    .bar-label {{ width: 55px; font-size: 12px; color: #aaa; }}
    .bar {{ height: 18px; border-radius: 3px; }}
    .bar-pct {{ margin-left: 8px; font-size: 12px; color: #666; }}
    .tree {{ list-style: none; padding-left: 20px; }}
    details {{ cursor: pointer; }}
    summary {{ padding: 4px 8px; border-radius: 4px; }}
    summary:hover {{ background: #2d2d44; }}
    .folder {{ color: #ffd700; }}
    .file {{ display: flex; align-items: center; padding: 4px 8px; border-radius: 4px; }}
    .file:hover {{ background: #2d2d44; }}
    .size {{ color: #888; margin-left: auto; font-size: 12px; }}
    .dot {{ width: 8px; height: 8px; border-radius: 50%; margin-right: 8px; }}
  </style>
</head><body>
  <div class="container">
    <div class="sidebar">
      <h1>📊 Summary</h1>
      <div class="stat"><span>Files</span><span class="stat-value">{stats["files"]:,}</span></div>
      <div class="stat"><span>Directories</span><span class="stat-value">{stats["dirs"]:,}</span></div>
      <div class="stat"><span>Total size</span><span class="stat-value">{fmt(data["size"])}</span></div>
      <div class="stat"><span>File types</span><span class="stat-value">{len(stats["extensions"])}</span></div>
      <h2>By file type</h2>
      {lang_bars}
    </div>
    <div class="main">
      <h1>📁 {escape(data["name"])}</h1>
      <ul class="tree" id="root"></ul>
    </div>
  </div>
  <script>
    const data = {json.dumps(data)};
    const colors = {json.dumps(colors)};
    function fmt(b) {{ if (b < 1024) return b + ' B'; if (b < 1048576) return (b/1024).toFixed(1) + ' KB'; return (b/1048576).toFixed(1) + ' MB'; }}
    function esc(s) {{ return s.replace(/[&<>"']/g, c => ({{"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}}[c])); }}
    function render(node, parent) {{
      if (node.children) {{
        const det = document.createElement('details');
        det.open = parent === document.getElementById('root');
        det.innerHTML = `<summary><span class="folder">📁 ${{esc(node.name)}}</span><span class="size">${{fmt(node.size)}}</span></summary>`;
        const ul = document.createElement('ul'); ul.className = 'tree';
        node.children.sort((a,b) => (b.children?1:0)-(a.children?1:0) || a.name.localeCompare(b.name));
        node.children.forEach(c => render(c, ul));
        det.appendChild(ul);
        const li = document.createElement('li'); li.appendChild(det); parent.appendChild(li);
      }} else {{
        const li = document.createElement('li'); li.className = 'file';
        li.innerHTML = `<span class="dot" style="background:${{colors[node.ext]||'#6b7280'}}"></span>${{esc(node.name)}}<span class="size">${{fmt(node.size)}}</span>`;
        parent.appendChild(li);
      }}
    }}
    data.children.forEach(c => render(c, document.getElementById('root')));
  </script>
</body></html>'''
    output.write_text(html)

if __name__ == '__main__':
    target = Path(sys.argv[1] if len(sys.argv) > 1 else '.').resolve()
    stats = {"files": 0, "dirs": 0, "extensions": Counter(), "ext_sizes": Counter()}
    data = scan(target, stats)
    out = Path('codebase-map.html')
    generate_html(data, stats, out)
    print(f'Generated {out.absolute()}')
    webbrowser.open(f'file://{out.absolute()}')
```

Para testar, abra Claude Code em qualquer projeto e peça "Visualize this codebase." Claude executa o script, gera `codebase-map.html`, e abre em seu navegador.

Este padrão funciona para qualquer saída visual: gráficos de dependência, relatórios de cobertura de testes, documentação de API, ou visualizações de esquema de banco de dados. O script agrupado faz o trabalho enquanto Claude lida com orquestração.

<h2 id="troubleshooting">
  Solução de problemas
</h2>

<h3 id="skill-not-triggering">
  Skill não dispara
</h3>

Se Claude não usa sua skill quando esperado:

1. Verifique se a descrição inclui palavras-chave que usuários naturalmente diriam
2. Verifique se a skill aparece em `What skills are available?`
3. Tente reformular sua solicitação para corresponder mais de perto à descrição
4. Invoque-a diretamente com `/skill-name` se a skill é invocável pelo usuário

Se o YAML do frontmatter está malformado, Claude Code carrega o corpo da skill com metadados vazios, então `/skill-name` ainda funciona, mas Claude não tem `description` para corresponder. Execute com `--debug` para ver o erro de análise.

<h3 id="skill-triggers-too-often">
  Skill dispara muito frequentemente
</h3>

Se Claude usa sua skill quando você não quer:

1. Torne a descrição mais específica
2. Adicione `disable-model-invocation: true` se você quer apenas invocação manual

<h3 id="skill-descriptions-are-cut-short">
  Descrições de skills são cortadas
</h3>

Claude Code carrega uma listagem de nomes de skills e descrições em contexto para que Claude saiba o que está disponível. A listagem sempre contém todos os nomes de skills, mas se você tem muitas skills, Claude Code encurta as descrições para caber no orçamento de caracteres da listagem, o que pode remover as palavras-chave que Claude precisa para corresponder sua solicitação. O orçamento escala em 1% da janela de contexto do modelo. Quando a listagem transborda, Claude Code remove descrições começando com as skills que você invoca menos, então as skills que você usa mais mantêm seu texto completo.

Execute `/doctor` para uma estimativa do custo de contexto da listagem e seus maiores contribuidores. Quando a listagem excede seu orçamento, Claude Code também escreve um aviso para o log de debug, visível com [`--debug`](/docs/pt/cli-reference#cli-flags).

A linha Skills em `/context` relata o tamanho da listagem após o orçamento ser aplicado, então corresponde ao que o modelo recebe. Antes da v2.1.196, a linha contava o texto completo de cada descrição e poderia mostrar um valor várias vezes maior do que o orçamento configurado.

Para aumentar o orçamento, defina a configuração [`skillListingBudgetFraction`](/docs/pt/settings#available-settings) (por exemplo, `0.02` = 2%) ou a variável de ambiente `SLASH_COMMAND_TOOL_CHAR_BUDGET` para uma contagem de caracteres fixa. Para liberar orçamento para outras skills, defina entradas de baixa prioridade como `"name-only"` em [`skillOverrides`](#override-skill-visibility-from-settings) para que sejam listadas sem uma descrição. Você também pode aparar o texto de `description` e `when_to_use` na fonte: coloque o caso de uso principal na frente, já que o texto combinado de cada entrada é limitado a 1.536 caracteres independentemente do orçamento. O limite é configurável com [`skillListingMaxDescChars`](/docs/pt/settings#available-settings).

<h2 id="related-resources">
  Recursos relacionados
</h2>

* **[Depure sua configuração](/docs/pt/debug-your-config)**: diagnostique por que uma skill não está aparecendo ou sendo acionada
* **[Avaliando a qualidade de saída de skill](https://agentskills.io/skill-creation/evaluating-skills)**: o formato do arquivo eval e fluxo de trabalho de iteração em agentskills.io
* **[Melhores práticas de autoria de skill](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices)**: orientação de escrita que se aplica em produtos Claude
* **[Subagents](/docs/pt/sub-agents)**: delegue tarefas para agents especializados
* **[Plugins](/docs/pt/plugins)**: empacote e distribua skills com outras extensões
* **[Hooks](/docs/pt/hooks)**: automatize fluxos de trabalho em torno de eventos de ferramentas
* **[Memory](/docs/pt/memory)**: gerencie arquivos CLAUDE.md para contexto persistente
* **[Comandos](/docs/pt/commands)**: referência para comandos integrados e skills agrupadas
* **[Permissões](/docs/pt/permissions)**: controle acesso a ferramentas e skills
* **[Claude Tag skills](https://claude.com/docs/claude-tag/admins/skills-repo)**: skills de projeto confirmadas em um repositório também são carregadas quando esse repositório é usado em um canal Claude Tag
