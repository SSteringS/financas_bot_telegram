<!-- Fonte: https://code.claude.com/docs/pt/costs | Baixado em: 2026-08-06 | slug: costs -->

# Gerencie custos de forma eficaz

> Rastreie o uso de tokens, defina limites de gastos da equipe e reduza os custos do Claude Code com gerenciamento de contexto, seleção de modelo, configurações de pensamento estendido e hooks de pré-processamento.

Claude Code cobra pelo consumo de tokens da API. Para preços do plano de assinatura (Pro, Max, Team, Enterprise), consulte [claude.com/pricing](https://claude.com/pricing). Os custos por desenvolvedor variam amplamente com base na seleção de modelo, tamanho da base de código e padrões de uso, como executar múltiplas instâncias ou automação.

Em implantações empresariais, o custo médio é de cerca de \$13 por desenvolvedor por dia ativo e \$150-250 por desenvolvedor por mês, com custos permanecendo abaixo de \$30 por dia ativo para 90% dos usuários. Para estimar gastos para sua própria equipe, comece com um pequeno grupo piloto e use as ferramentas de rastreamento abaixo para estabelecer uma linha de base antes de um lançamento mais amplo.

Esta página aborda como [rastrear seus custos](#track-your-costs), [gerenciar custos para sua organização](#manage-costs-for-your-organization) e [reduzir o uso de tokens](#reduce-token-usage).

<h2 id="track-your-costs">
  Rastreie seus custos
</h2>

<h3 id="using-the-/usage-command">
  Usando o comando `/usage`
</h3>

<Note>
  O bloco Session em `/usage` mostra o uso de tokens da API e é destinado a usuários de API. Assinantes do Claude Max e Pro têm uso incluído em sua assinatura, portanto, a figura de custo da sessão não é relevante para fins de faturamento. Os assinantes veem barras de uso do plano, estatísticas de atividade e um detalhamento de uso na mesma tela.
</Note>

O bloco Session no topo de `/usage` mostra estatísticas detalhadas de uso de tokens para sua sessão atual. A figura em dólares é uma estimativa calculada localmente a partir de contagens de tokens e pode diferir de sua fatura real. Para faturamento autorizado, consulte a página de Uso no [Claude Console](https://platform.claude.com/usage).

```text theme={null}
Total cost:            $0.55
Total duration (API):  6m 19.7s
Total duration (wall): 6h 33m 10.2s
Total code changes:    0 lines added, 0 lines removed
```

Em um plano Pro, Max, Team ou Enterprise, `/usage` também mostra um detalhamento do que conta contra seus limites de plano. Ele atribui o uso recente a skills, subagents, plugins e servidores MCP individuais, cada um mostrado como uma porcentagem do total. Pressione `d` ou `w` para alternar entre as últimas 24 horas e os últimos 7 dias. As figuras são aproximadas e calculadas a partir do histórico de sessão local nesta máquina, portanto, o uso de outros dispositivos ou claude.ai não está incluído.

Quando a solicitação dos seus limites de plano falha, na maioria das vezes porque o endpoint de uso está com limite de taxa, `/usage` mostra as últimas barras de uso que carregou nesta máquina nos últimos 60 minutos, junto com uma nota `Showing last-known usage` indicando há quanto tempo esses dados foram obtidos. Pressione `r` para tentar novamente; uma tentativa bem-sucedida substitui as últimas barras conhecidas por dados atualizados. Sem um snapshot dos últimos 60 minutos, `/usage` relata que o endpoint de uso está com limite de taxa e oferece o mesmo atalho de tentativa. Antes da v2.1.208, uma solicitação com limite de taxa em uma sessão que ainda não havia carregado uso sempre mostrava o erro sem barras.

Na [extensão do VS Code](https://code.claude.com/docs/pt/vs-code#check-account-and-usage), o mesmo detalhamento aparece no diálogo Account & usage com um alternador Day e Week. Requer Claude Code v2.1.174 ou posterior.

<h3 id="set-a-spend-limit-on-pro-and-max">
  Defina um limite de gastos em Pro e Max
</h3>

Nos planos Pro e Max, o comando `/usage-credits` abre um diálogo na CLI onde você gerencia [créditos de uso](https://support.claude.com/en/articles/12429409-extra-usage-for-paid-claude-plans). A partir do diálogo você pode:

* Ativar créditos de uso para sua conta
* Comprar mais créditos de uso, seja um pacote listado ou um valor personalizado
* Definir, alterar ou remover seu limite de gastos mensal
* Configurar auto-reload, que compra mais créditos de uso automaticamente quando seu saldo cai abaixo de um limite que você define

Em versões do Claude Code anteriores à v2.1.207 e em contas onde o diálogo na CLI não está disponível, `/usage-credits` abre a página de faturamento de créditos de uso em seu navegador. Nos planos Team e Enterprise, membros com acesso de faturamento obtêm a mesma página do navegador, e membros sem acesso de faturamento enviam uma solicitação da CLI pedindo ao seu administrador para ativar créditos de uso ou aumentar o limite.

Alterar o limite de gastos mensal requer acesso de faturamento na conta. Se você atingir o limite enquanto ainda tiver créditos de uso disponíveis, Claude Code o solicita a aumentar ou remover o limite para que você possa continuar sem sair da CLI.

Os valores que você digita no diálogo, como um valor de compra personalizado, o limite de gastos mensal ou o limite de auto-reload e o alvo, devem ser dígitos, opcionalmente seguidos por um ponto e um ou dois dígitos decimais, por exemplo `20` ou `20.50`. Qualquer outra entrada, incluindo vírgulas, mostra um erro inline e não é salva. Versões anteriores à v2.1.207 não mostram o diálogo e abrem a página de faturamento.

Claude Code pede que você digite `yes` para confirmar cada compra e cada mudança de auto-reload, independentemente do valor, e a confirmação de compra mostra o total após impostos que você está aprovando. Alterar o limite de gastos mensal pede a mesma confirmação digitada apenas acima de \$1.000, ou acima de 1.000 unidades de uma moeda de faturamento não-dólar americano. Antes da v2.1.208, compras e mudanças de auto-reload usavam esse limite também, portanto, valores menores passavam pelo fluxo de diálogo padrão sem a etapa extra de `yes` digitado.

Os campos de valor abrem pré-preenchidos com um valor sugerido, e o primeiro dígito que você digita substitui a sugestão em vez de anexá-la. A tela que ativa créditos de uso abre com Cancel selecionado, portanto, ativá-los requer uma seleção deliberada em vez de um Enter acidental. Ambos requerem Claude Code v2.1.208 ou posterior.

<h2 id="manage-costs-for-your-organization">
  Gerenciar custos para sua organização
</h2>

Quais controles você tem depende de como sua organização acessa Claude Code: um plano Claude for Teams ou Enterprise, o Claude Console, ou um provedor de nuvem. Nos planos Teams e Enterprise, o uso é extraído da cota de cada membro. No Console e em provedores de nuvem, o uso é faturado por token para sua organização. Se sua organização mistura métodos de login, cada desenvolvedor é medido de acordo com aquele com o qual se autenticou.

A tabela mapeia cada configuração para onde você vê gastos, onde você os limita e como você extrai números por usuário.

| Sua configuração                                                                                | Ver gastos                                                                                                                                           | Limitar gastos                                       | Relatório por usuário                                                                                                                                                                                                               |
| :---------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [Claude for Teams ou Enterprise](#claude-for-teams-and-enterprise)                              | [Relatório de gastos em análises da organização](https://support.claude.com/en/articles/12883420-view-usage-analytics-for-team-and-enterprise-plans) | Limites de gastos nas configurações de administrador | [CSV do relatório de gastos](https://support.claude.com/en/articles/12883420-view-usage-analytics-for-team-and-enterprise-plans); [Enterprise Analytics API](https://platform.claude.com/docs/en/api/admin/analytics) no Enterprise |
| [Claude Console (API)](#claude-console)                                                         | [Página de uso do Console](https://platform.claude.com/usage)                                                                                        | Limites de gastos do workspace                       | [Dashboard do Console](https://platform.claude.com/claude-code), [Claude Code Analytics API](https://platform.claude.com/docs/en/build-with-claude/claude-code-analytics-api)                                                       |
| [Amazon Bedrock, Plataforma de Agentes do Google Cloud, ou Microsoft Foundry](#cloud-providers) | Seu console de faturamento da nuvem                                                                                                                  | Controles de orçamento da sua nuvem                  | [OpenTelemetry](https://code.claude.com/docs/pt/monitoring-usage) ou um [gateway LLM](https://code.claude.com/docs/pt/llm-gateway)                                                                                                                                                          |

[Exportação OpenTelemetry](https://code.claude.com/docs/pt/monitoring-usage) funciona em todas as configurações e é a única opção que transmite métricas de token e custo por usuário para sua própria pilha de observabilidade em tempo quase real.

<h3 id="claude-for-teams-and-enterprise">
  Claude for Teams e Enterprise
</h3>

Nos planos Claude for Teams e Enterprise, o uso de Claude Code de cada membro é extraído de uma cota por assento que é redefinida em uma janela de cinco horas contínuas e uma janela semanal. A cota é compartilhada com Claude chat e Cowork, e seu tamanho depende do [nível de assento](https://support.claude.com/en/articles/11845131-use-claude-code-with-your-team-or-enterprise-plan) (Standard ou Premium). Seus controles ficam no console de administrador claude.ai, não no Claude Console.

* **Ver gastos**: o [relatório de gastos em análises da organização](https://support.claude.com/en/articles/12883420-view-usage-analytics-for-team-and-enterprise-plans) mostra gastos estimados por usuário e por modelo, com exportação CSV, atualizado diariamente. O relatório cobre gastos de créditos de uso e aparece uma vez que os créditos de uso são ativados. O uso dentro da cota por assento não é medido em dólares.
* **Ver adoção**: o [dashboard de análises](https://claude.ai/analytics/claude-code) mostra usuários ativos diários, sessões e métricas de contribuição, com exportação CSV de dados de contribuição. Veja [rastrear uso da equipe com análises](https://code.claude.com/docs/pt/analytics).
* **Limitar gastos**: a cota por assento é o teto padrão. Para permitir que membros continuem além disso, ative [créditos de uso](https://support.claude.com/en/articles/12429409-extra-usage-for-paid-claude-plans) e defina limites de gastos no nível da organização, grupo ou membro individual.
* **Extrair números por usuário**: no plano Enterprise, a [Enterprise Analytics API](https://platform.claude.com/docs/en/api/admin/analytics) retorna relatórios de uso e custo por usuário em todas as superfícies Claude, incluindo Claude Code. Um Proprietário Primário cria uma chave com o escopo `read:analytics` em [claude.ai/analytics/api-keys](https://claude.ai/analytics/api-keys). No plano Teams, exporte o [CSV do relatório de gastos](https://support.claude.com/en/articles/12883420-view-usage-analytics-for-team-and-enterprise-plans), que lista uso de tokens e gastos estimados por usuário e por modelo.

O [guia de consumo Claude Enterprise](https://support.claude.com/en/articles/14782391-claude-enterprise-consumption-guide) é a referência de planejamento para administradores. Ele explica como o consumo difere entre Claude chat, Claude Code e Cowork, e fornece pontos de partida em dólares por usuário para orçamento. Orçamente mais para um assento de codificação do que um assento de chat: cada turno de Claude Code carrega conteúdo de arquivo, chamadas de ferramenta e raciocínio em múltiplas etapas, então uma sessão de depuração pode consumir mais do que um dia de chat.

<h3 id="claude-console">
  Claude Console
</h3>

Organizações de API gerenciam gastos de Claude Code através de [workspaces](https://platform.claude.com/docs/en/build-with-claude/workspaces). Você pode [definir limites de gastos do workspace](https://platform.claude.com/docs/en/build-with-claude/workspaces#workspace-limits) no gasto total de Claude Code e [visualizar relatórios de custo e uso](https://platform.claude.com/docs/en/build-with-claude/workspaces#usage-and-cost-tracking) no Console.

<Note>
  Quando você autentica pela primeira vez o Claude Code com sua conta do Claude Console, um workspace chamado "Claude Code" é criado automaticamente para você. Este workspace fornece rastreamento e gerenciamento centralizado de custos para todo o uso do Claude Code em sua organização. Você não pode criar chaves de API para este workspace; é exclusivamente para autenticação e uso do Claude Code.

  Para organizações com limites de taxa personalizados, o tráfego do Claude Code neste workspace conta para os limites de taxa geral da API da sua organização. Você pode definir um [limite de taxa do workspace](https://platform.claude.com/docs/pt/api/rate-limits#setting-lower-limits-for-workspaces) na página Limits deste workspace no Claude Console para limitar a cota do Claude Code e proteger outras cargas de trabalho de produção.
</Note>

Para relatório por usuário, o [dashboard do Console](https://platform.claude.com/claude-code) mostra gastos e linhas aceitas por membro, e a [Claude Code Analytics API](https://platform.claude.com/docs/en/build-with-claude/claude-code-analytics-api) retorna as mesmas métricas diárias por usuário programaticamente com uma [chave de API de Administrador](https://platform.claude.com/settings/admin-keys). Veja [análises para clientes de API](https://code.claude.com/docs/pt/analytics#access-analytics-for-api-customers).

<h4 id="rate-limit-recommendations">
  Recomendações de limite de taxa
</h4>

Ao configurar Claude Code para equipes, considere estas recomendações de Token Por Minuto (TPM) e Requisição Por Minuto (RPM) por usuário com base no tamanho da sua organização:

| Tamanho da equipe | TPM por usuário | RPM por usuário |
| ----------------- | --------------- | --------------- |
| 1-5 usuários      | 200k-300k       | 5-7             |
| 5-20 usuários     | 100k-150k       | 2.5-3.5         |
| 20-50 usuários    | 50k-75k         | 1.25-1.75       |
| 50-100 usuários   | 25k-35k         | 0.62-0.87       |
| 100-500 usuários  | 15k-20k         | 0.37-0.47       |
| 500+ usuários     | 10k-15k         | 0.25-0.35       |

Por exemplo, se você tiver 200 usuários, você pode solicitar 20k TPM para cada usuário, ou 4 milhões de TPM total (200\*20.000 = 4 milhões).

O TPM por usuário diminui conforme o tamanho da equipe cresce porque menos usuários tendem a usar Claude Code simultaneamente em organizações maiores. Esses limites de taxa se aplicam no nível da organização, não por usuário individual, o que significa que usuários individuais podem consumir temporariamente mais do que sua cota calculada quando outros não estão usando ativamente o serviço.

<Note>
  Se você antecipar cenários com uso concorrente incomumente alto (como sessões de treinamento ao vivo com grandes grupos), você pode precisar de alocações de TPM mais altas por usuário.
</Note>

<h3 id="cloud-providers">
  Provedores de nuvem
</h3>

No Amazon Bedrock, na Plataforma de Agentes do Google Cloud e no Microsoft Foundry, Claude Code é faturado por token para sua conta de nuvem, e os controles de gastos ficam no console de faturamento do seu provedor de nuvem. Claude Code não envia métricas de sua nuvem de volta para Anthropic, então os [dashboards de análises](https://code.claude.com/docs/pt/analytics) e a Claude Code Analytics API não cobrem este uso.

Para atribuição de custo por usuário, você tem três opções:

* **OpenTelemetry**: [exporte métricas](https://code.claude.com/docs/pt/monitoring-usage) da máquina de cada desenvolvedor para sua própria pilha de observabilidade. Isso fornece contagens de tokens por usuário, custos e atividade de ferramenta independentemente do provedor.
* **Um gateway de aplicativos Claude**: um [gateway de aplicativos Claude](https://code.claude.com/docs/pt/claude-apps-gateway) auto-hospedado fornece atribuição de uso por usuário, métricas OTLP com contagens de tokens e [limites de gastos por usuário](https://code.claude.com/docs/pt/claude-apps-gateway-spend-limits) nesses provedores.
* **Um gateway LLM**: rotear todo o tráfego de Claude Code através de um proxy que rastreia gastos por chave. Vários grandes empresas relataram usar [LiteLLM](https://code.claude.com/docs/pt/llm-gateway), uma ferramenta de código aberto que [rastreia gastos por chave](https://docs.litellm.ai/docs/proxy/virtual_keys#tracking-spend). Este projeto não é afiliado à Anthropic e não foi auditado para segurança.

<h3 id="when-a-developer-asks-about-a-limit">
  Quando um desenvolvedor pergunta sobre um limite
</h3>

Desenvolvedores geralmente trazem perguntas sobre limites para seu administrador, então é útil saber qual teto eles atingiram. As três situações significam coisas diferentes:

* **"Você atingiu seu limite de sessão" ou "Você atingiu seu limite semanal"**: uma janela de uso baseada em assento em um plano de assinatura. Essas janelas são compartilhadas em todos os modelos, então mudar de modelo com `/model` não restaura o acesso, embora mantenha o desenvolvedor trabalhando após a mensagem específica do modelo "Você atingiu seu limite de Opus". A mensagem mostra quando a janela é redefinida, e o desenvolvedor pode executar `/usage-credits` para solicitar uso além da cota se você tiver [créditos de uso](https://support.claude.com/en/articles/12429409-extra-usage-for-paid-claude-plans) ativados. Veja [erros de limite de uso](https://code.claude.com/docs/pt/errors#youve-hit-your-session-limit).
* **Um aviso de contexto ou auto-compact**: não é um limite de uso. A conversa cresceu perto do tamanho máximo de entrada do modelo, e Claude Code resume o histórico mais antigo para liberar espaço. Aponte o desenvolvedor para [reduzir uso de tokens](#reduce-token-usage).
* **Gastos inesperadamente altos em um plano de API ou provedor de nuvem**: geralmente rastreia de volta para sessões longas que nunca foram limpas ou para Opus deixado como o modelo padrão. Os hábitos de maior impacto para compartilhar são limpar entre tarefas não relacionadas e corresponder o modelo ao trabalho, ambos cobertos em [reduzir uso de tokens](#reduce-token-usage).

<h3 id="agent-team-token-costs">
  Custos de tokens de equipes de agentes
</h3>

[Equipes de agentes](https://code.claude.com/docs/pt/agent-teams) geram múltiplas instâncias do Claude Code, cada uma com sua própria janela de contexto. O uso de tokens escala com o número de colegas de equipe ativos e quanto tempo cada um executa.

Para manter os custos das equipes de agentes gerenciáveis:

* Use Sonnet para colegas de equipe. Ele equilibra capacidade e custo para tarefas de coordenação.
* Mantenha equipes pequenas. Cada colega de equipe executa sua própria janela de contexto, portanto, o uso de tokens é aproximadamente proporcional ao tamanho da equipe.
* Mantenha prompts de geração focados. Colegas de equipe carregam CLAUDE.md, servidores MCP e skills automaticamente, mas tudo no prompt de geração adiciona ao seu contexto desde o início.
* Desligue colegas de equipe quando o trabalho estiver concluído. Cada colega de equipe ativo continua consumindo tokens até sair ou a sessão terminar.
* Equipes de agentes são desabilitadas por padrão. Defina `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` em seu [settings.json](https://code.claude.com/docs/pt/settings) ou ambiente para habilitá-las. Veja [habilitar equipes de agentes](https://code.claude.com/docs/pt/agent-teams#enable-agent-teams).

<h2 id="reduce-token-usage">
  Reduza o uso de tokens
</h2>

Os custos de tokens escalam com o tamanho do contexto: quanto mais contexto Claude processa, mais tokens você usa. Claude Code otimiza automaticamente os custos através do [prompt caching](https://code.claude.com/docs/pt/prompt-caching), que reduz custos para conteúdo repetido como prompts do sistema, e auto-compaction, que resume o histórico de conversa ao se aproximar dos limites de contexto.

As seguintes estratégias ajudam você a manter o contexto pequeno e reduzir custos por mensagem.

<h3 id="manage-context-proactively">
  Gerencie o contexto proativamente
</h3>

Use `/usage` para verificar seu uso atual de tokens, ou [configure sua linha de status](https://code.claude.com/docs/pt/statusline#context-window-usage) para exibi-la continuamente.

* **Limpe entre tarefas**: Use `/clear` para começar do zero ao mudar para trabalho não relacionado. Contexto obsoleto desperdiça tokens em cada mensagem subsequente. Use `/rename` antes de limpar para que você possa encontrar facilmente a sessão depois, então `/resume` para retornar a ela.
* **Adicione instruções de compactação personalizadas**: `/compact Focus on code samples and API usage` diz a Claude o que preservar durante a sumarização.

Você também pode personalizar o comportamento de compactação em seu arquivo CLAUDE.md na raiz do seu projeto:

```markdown theme={null}
# Compact instructions

When you are using compact, please focus on test output and code changes
```

<h3 id="choose-the-right-model">
  Escolha o modelo certo
</h3>

Sonnet lida bem com a maioria das tarefas de codificação e custa menos que Opus. Reserve Opus para decisões arquitetônicas complexas ou raciocínio em múltiplas etapas. Use `/model` para alternar modelos no meio da sessão, ou defina um padrão em `/config`. Para tarefas simples de subagente, especifique `model: haiku` em sua [configuração de subagente](https://code.claude.com/docs/pt/sub-agents#choose-a-model).

<h3 id="reduce-mcp-server-overhead">
  Reduza a sobrecarga do servidor MCP
</h3>

As definições de ferramentas MCP são [adiadas por padrão](https://code.claude.com/docs/pt/mcp#scale-with-mcp-tool-search), portanto apenas nomes de ferramentas entram no contexto até Claude usar uma ferramenta específica. Execute `/context` para ver o que está consumindo espaço.

* **Prefira ferramentas CLI quando disponíveis**: Ferramentas como `gh`, `aws`, `gcloud` e `sentry-cli` são ainda mais eficientes em contexto do que servidores MCP porque não adicionam nenhuma listagem por ferramenta. Claude pode executar comandos CLI diretamente.
* **Desabilite servidores não utilizados**: Execute `/mcp` para ver servidores configurados e desabilite qualquer um que você não esteja usando ativamente.

<h3 id="install-code-intelligence-plugins-for-typed-languages">
  Instale plugins de inteligência de código para linguagens tipadas
</h3>

[Plugins de inteligência de código](https://code.claude.com/docs/pt/discover-plugins#code-intelligence) dão a Claude navegação de símbolo precisa em vez de busca baseada em texto, reduzindo leituras de arquivo desnecessárias ao explorar código desconhecido. Uma única chamada "ir para definição" substitui o que poderia ser um grep seguido de leitura de múltiplos arquivos candidatos. Servidores de linguagem instalados também relatam erros de tipo automaticamente após edições, portanto Claude detecta erros sem executar um compilador.

<h3 id="offload-processing-to-hooks-and-skills">
  Descarregue o processamento para hooks e skills
</h3>

[Hooks](https://code.claude.com/docs/pt/hooks) personalizados podem pré-processar dados antes de Claude vê-los. Em vez de Claude ler um arquivo de log de 10.000 linhas para encontrar erros, um hook pode fazer grep para `ERROR` e retornar apenas linhas correspondentes, reduzindo contexto de dezenas de milhares de tokens para centenas.

Uma [skill](https://code.claude.com/docs/pt/skills) pode dar a Claude conhecimento de domínio para que não tenha que explorar. Por exemplo, uma skill "codebase-overview" poderia descrever a arquitetura do seu projeto, diretórios-chave e convenções de nomenclatura. Quando Claude invoca a skill, obtém este contexto imediatamente em vez de gastar tokens lendo múltiplos arquivos para entender a estrutura.

Por exemplo, este hook PreToolUse filtra a saída de teste para mostrar apenas falhas:

<Tabs>
  <Tab title="settings.json">
    Adicione isto ao seu [settings.json](https://code.claude.com/docs/pt/settings#settings-files) para executar o hook antes de cada comando Bash:

    ```json theme={null}
    {
      "hooks": {
        "PreToolUse": [
          {
            "matcher": "Bash",
            "hooks": [
              {
                "type": "command",
                "command": "~/.claude/hooks/filter-test-output.sh"
              }
            ]
          }
        ]
      }
    }
    ```
  </Tab>

  <Tab title="filter-test-output.sh">
    O hook chama este script. Crie a pasta com `mkdir -p ~/.claude/hooks`, salve o script abaixo como `~/.claude/hooks/filter-test-output.sh` e torne-o executável com `chmod +x ~/.claude/hooks/filter-test-output.sh`. Ele verifica se o comando é um executor de teste e o modifica para mostrar apenas falhas:

    ```bash theme={null}
    #!/bin/bash
    input=$(cat)
    cmd=$(echo "$input" | jq -r '.tool_input.command')

    # If running tests, filter to show only failures
    if [[ "$cmd" =~ ^(npm test|pytest|go test) ]]; then
      filtered_cmd="$cmd 2>&1 | grep -A 5 -E '(FAIL|ERROR|error:)' | head -100"
      echo "{\"hookSpecificOutput\":{\"hookEventName\":\"PreToolUse\",\"permissionDecision\":\"allow\",\"updatedInput\":{\"command\":\"$filtered_cmd\"}}}"
    else
      echo "{}"
    fi
    ```
  </Tab>
</Tabs>

<h3 id="move-instructions-from-claude-md-to-skills">
  Mova instruções de CLAUDE.md para skills
</h3>

Seu arquivo [CLAUDE.md](https://code.claude.com/docs/pt/memory) é carregado no contexto no início da sessão. Se contiver instruções detalhadas para fluxos de trabalho específicos (como revisões de PR ou migrações de banco de dados), esses tokens estão presentes mesmo quando você está fazendo trabalho não relacionado. [Skills](https://code.claude.com/docs/pt/skills) carregam sob demanda apenas quando invocadas, portanto mover instruções especializadas para skills mantém seu contexto base menor. Procure manter CLAUDE.md com menos de 200 linhas incluindo apenas essenciais.

<h3 id="adjust-extended-thinking">
  Ajuste o pensamento estendido
</h3>

O pensamento estendido é habilitado por padrão porque melhora significativamente o desempenho em tarefas complexas de planejamento e raciocínio. Tokens de pensamento são faturados como tokens de saída, e o orçamento padrão pode ser dezenas de milhares de tokens por solicitação dependendo do modelo. Para tarefas mais simples onde raciocínio profundo não é necessário, você pode reduzir custos baixando o [nível de esforço](https://code.claude.com/docs/pt/model-config#adjust-effort-level) com `/effort` ou em `/model`, desabilitando pensamento em `/config`, ou, em modelos com um [orçamento de pensamento fixo](https://code.claude.com/docs/pt/model-config#adaptive-reasoning-and-fixed-thinking-budgets), baixando o orçamento definindo a [variável de ambiente](https://code.claude.com/docs/pt/env-vars) `MAX_THINKING_TOKENS`, por exemplo `MAX_THINKING_TOKENS=8000`. Modelos de raciocínio adaptativo ignoram orçamentos diferentes de zero, portanto use níveis de esforço lá em vez disso. Desabilitar pensamento não está disponível no Fable 5, que sempre usa pensamento estendido.

<h3 id="delegate-verbose-operations-to-subagents">
  Delegue operações verbosas para subagentes
</h3>

Executar testes, buscar documentação ou processar arquivos de log pode consumir contexto significativo. Delegue estes para [subagentes](https://code.claude.com/docs/pt/sub-agents#isolate-high-volume-operations) para que a saída verbosa permaneça no contexto do subagente enquanto apenas um resumo retorna à sua conversa principal.

<h3 id="manage-agent-team-costs">
  Gerencie custos de equipes de agentes
</h3>

Equipes de agentes usam aproximadamente 7x mais tokens do que sessões padrão quando colegas de equipe executam em modo de plano, porque cada colega de equipe mantém sua própria janela de contexto e executa como uma instância Claude separada. Mantenha tarefas de equipe pequenas e auto-contidas para limitar o uso de tokens por colega de equipe. Veja [equipes de agentes](https://code.claude.com/docs/pt/agent-teams) para detalhes.

<h3 id="write-specific-prompts">
  Escreva prompts específicos
</h3>

Solicitações vagas como "melhorar esta base de código" disparam varredura ampla. Solicitações específicas como "adicionar validação de entrada à função de login em auth.ts" deixam Claude trabalhar eficientemente com leituras de arquivo mínimas.

<h3 id="work-efficiently-on-complex-tasks">
  Trabalhe eficientemente em tarefas complexas
</h3>

Para trabalho mais longo ou complexo, esses hábitos ajudam a evitar tokens desperdiçados por seguir o caminho errado:

* **Use modo de plano para tarefas complexas**: Pressione Shift+Tab para entrar em [modo de plano](https://code.claude.com/docs/pt/permission-modes#analyze-before-you-edit-with-plan-mode) antes da implementação. Claude explora a base de código e propõe uma abordagem para sua aprovação, prevenindo retrabalho caro quando a direção inicial está errada.
* **Corrija o curso cedo**: Se Claude começar a seguir a direção errada, pressione Escape para parar imediatamente. Use `/rewind` ou toque duplo em Escape para restaurar conversa e código para um checkpoint anterior.
* **Dê alvos de verificação**: Inclua casos de teste, cole capturas de tela ou defina saída esperada em seu prompt. Quando Claude pode verificar seu próprio trabalho, detecta problemas antes de você precisar solicitar correções.
* **Teste incrementalmente**: Escreva um arquivo, teste-o, depois continue. Isto detecta problemas cedo quando são baratos de corrigir.

<h2 id="background-token-usage">
  Uso de tokens em segundo plano
</h2>

Claude Code usa tokens para algumas funcionalidades em segundo plano mesmo quando ocioso:

* **Sumarização de conversa**: Trabalhos em segundo plano que resumem conversas anteriores para o recurso `claude --resume`
* **Processamento de comando**: Alguns comandos como `/usage` podem gerar solicitações para verificar status

Esses processos em segundo plano consomem uma pequena quantidade de tokens (tipicamente menos de \$0.04 por sessão) mesmo sem interação ativa.

<h2 id="understanding-changes-in-claude-code-behavior">
  Entendendo mudanças no comportamento do Claude Code
</h2>

Claude Code recebe regularmente atualizações que podem mudar como os recursos funcionam, incluindo relatório de custos. Execute `claude --version` para verificar sua versão atual. Para perguntas específicas de faturamento, entre em contato com o suporte da Anthropic através de sua [conta Console](https://platform.claude.com/login).
