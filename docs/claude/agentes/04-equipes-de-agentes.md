<!-- Fonte: https://code.claude.com/docs/pt/agent-teams | Baixado em: 2026-08-06 | slug: agent-teams -->

# Orquestre equipes de sessões Claude Code

> Coordene múltiplas instâncias Claude Code trabalhando juntas como uma equipe, com tarefas compartilhadas, mensagens entre agentes e gerenciamento centralizado.

<Warning>
  Equipes de agentes são experimentais e desabilitadas por padrão. Ative-as adicionando `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS` ao seu [settings.json](https://code.claude.com/docs/pt/settings) ou ambiente. Sem essa variável, nenhuma equipe é configurada no início da sessão, nenhum diretório de equipe é escrito, e Claude não gera ou propõe companheiros de equipe. Equipes de agentes têm [limitações conhecidas](#limitations) em torno de retomada de sessão, coordenação de tarefas e comportamento de encerramento.
</Warning>

Equipes de agentes permitem que você coordene múltiplas instâncias Claude Code trabalhando juntas. Uma sessão atua como o líder da equipe, coordenando o trabalho, atribuindo tarefas e sintetizando resultados. Os companheiros de equipe trabalham independentemente, cada um em sua própria context window, e se comunicam diretamente uns com os outros.

Diferentemente de [subagents](https://code.claude.com/docs/pt/sub-agents), que são executados dentro de uma única sessão e podem apenas relatar de volta ao agente principal, você também pode interagir com companheiros de equipe individuais diretamente sem passar pelo líder.

<Note>
  Esta página descreve equipes de agentes a partir da v2.1.178. Com `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS` definido, gerar um companheiro de equipe não requer mais uma etapa de configuração, e a limpeza acontece automaticamente quando a sessão sai. Antes da v2.1.178, você pedia ao Claude para criar e nomear uma equipe primeiro, e Claude usava as ferramentas `TeamCreate` e `TeamDelete` para configurá-la e removê-la. Ambas as ferramentas não existem mais. A entrada `team_name` na ferramenta Agent é aceita mas ignorada, e o campo `team_name` em `TaskCreated`, `TaskCompleted`, e `TeammateIdle` [payloads de hook](https://code.claude.com/docs/pt/hooks#taskcreated) carrega o nome derivado da sessão e está descontinuado.
</Note>

<h2 id="when-to-use-agent-teams">
  Quando usar equipes de agentes
</h2>

Equipes de agentes são mais eficazes para tarefas onde a exploração paralela adiciona valor real. Veja [exemplos de casos de uso](#use-case-examples) para cenários completos. Os casos de uso mais fortes são:

* **Pesquisa e revisão**: múltiplos companheiros de equipe podem investigar diferentes aspectos de um problema simultaneamente, depois compartilhar e desafiar as descobertas uns dos outros
* **Novos módulos ou recursos**: companheiros de equipe podem possuir cada um uma peça separada sem se atrapalharem
* **Depuração com hipóteses concorrentes**: companheiros de equipe testam diferentes teorias em paralelo e convergem para a resposta mais rapidamente
* **Coordenação entre camadas**: mudanças que abrangem frontend, backend e testes, cada uma de propriedade de um companheiro de equipe diferente

Equipes de agentes adicionam sobrecarga de coordenação e usam significativamente mais tokens do que uma única sessão. Funcionam melhor quando os companheiros de equipe podem operar independentemente. Para tarefas sequenciais, edições no mesmo arquivo ou trabalho com muitas dependências, uma única sessão ou [subagents](https://code.claude.com/docs/pt/sub-agents) são mais eficazes.

<h3 id="compare-with-subagents">
  Comparar com subagents
</h3>

Tanto equipes de agentes quanto [subagents](https://code.claude.com/docs/pt/sub-agents) permitem que você paralelizar o trabalho, mas operam de forma diferente. Escolha com base em se seus trabalhadores precisam se comunicar uns com os outros:

<Frame caption="Subagents apenas relatam resultados de volta ao agente principal e nunca falam uns com os outros. Em equipes de agentes, os companheiros de equipe compartilham uma lista de tarefas, reivindicam trabalho e se comunicam diretamente uns com os outros.">
  <img src="https://mintcdn.com/claude-code/nsvRFSDNfpSU5nT7/images/subagents-vs-agent-teams-light.png?fit=max&auto=format&n=nsvRFSDNfpSU5nT7&q=85&s=2f8db9b4f3705dd3ab931fbe2d96e42a" className="dark:hidden" alt="Diagrama comparando arquiteturas de subagent e equipe de agentes. Subagents são gerados pelo agente principal, fazem trabalho e relatam resultados de volta. Equipes de agentes coordenam através de uma lista de tarefas compartilhada, com companheiros de equipe se comunicando diretamente uns com os outros." width="4245" height="1615" data-path="images/subagents-vs-agent-teams-light.png" />

  <img src="https://mintcdn.com/claude-code/nsvRFSDNfpSU5nT7/images/subagents-vs-agent-teams-dark.png?fit=max&auto=format&n=nsvRFSDNfpSU5nT7&q=85&s=d573a037540f2ada6a9ae7d8285b46fd" className="hidden dark:block" alt="Diagrama comparando arquiteturas de subagent e equipe de agentes. Subagents são gerados pelo agente principal, fazem trabalho e relatam resultados de volta. Equipes de agentes coordenam através de uma lista de tarefas compartilhada, com companheiros de equipe se comunicando diretamente uns com os outros." width="4245" height="1615" data-path="images/subagents-vs-agent-teams-dark.png" />
</Frame>

|                   | Subagents                                                  | Agent teams                                                       |
| :---------------- | :--------------------------------------------------------- | :---------------------------------------------------------------- |
| **Context**       | Context window própria; resultados retornam ao chamador    | Context window própria; totalmente independente                   |
| **Communication** | Relatam resultados de volta apenas ao agente principal     | Companheiros de equipe se mensageiam diretamente                  |
| **Coordination**  | Agente principal gerencia todo o trabalho                  | Lista de tarefas compartilhada com auto-coordenação               |
| **Best for**      | Tarefas focadas onde apenas o resultado importa            | Trabalho complexo que requer discussão e colaboração              |
| **Token cost**    | Menor: resultados resumidos de volta ao contexto principal | Maior: cada companheiro de equipe é uma instância Claude separada |

Use subagents quando você precisa de trabalhadores rápidos e focados que relatem de volta. Use equipes de agentes quando os companheiros de equipe precisam compartilhar descobertas, desafiar uns aos outros e coordenar por conta própria.

<h2 id="enable-agent-teams">
  Ativar equipes de agentes
</h2>

Equipes de agentes são desabilitadas por padrão. Ative-as definindo a variável de ambiente `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS` como `1`, seja no seu ambiente de shell ou através de [settings.json](https://code.claude.com/docs/pt/settings):

```json settings.json theme={null}
{
  "env": {
    "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"
  }
}
```

<h2 id="start-your-first-agent-team">
  Inicie sua primeira equipe de agentes
</h2>

Após ativar equipes de agentes, descreva a tarefa e os companheiros de equipe que você deseja em linguagem natural. Claude os cria e coordena o trabalho com base no seu prompt.

Este exemplo funciona bem porque os três papéis são independentes e podem explorar o problema sem esperar um pelo outro:

```text theme={null}
I'm designing a CLI tool that helps developers track TODO comments across
their codebase. Spawn three teammates to explore this from different angles:
one on UX, one on technical architecture, one playing devil's advocate.
```

A partir daí, Claude popula uma [lista de tarefas compartilhada](https://code.claude.com/docs/pt/interactive-mode#task-list), cria companheiros de equipe para cada perspectiva, faz com que explorem o problema e sintetiza descobertas quando terminar.

O terminal do líder lista companheiros de equipe no painel do agente abaixo da entrada do prompt. A partir do painel:

* **Setas para cima e para baixo**: selecione um companheiro de equipe
* **Enter**: abra a transcrição do companheiro de equipe selecionado e envie uma mensagem diretamente
* **Escape**: interrompa o turno atual do companheiro de equipe selecionado

A partir da v2.1.199, a linha de um companheiro de equipe ocioso permanece no painel enquanto qualquer companheiro de equipe ou subagente ainda estiver trabalhando, para que você possa selecioná-lo para revisar sua transcrição ou enviar-lhe mais trabalho. Quando todos os agentes no painel estão ociosos, as linhas ociosas se ocultam após 30 segundos e reaparecem no próximo turno do companheiro de equipe; o companheiro de equipe continua em execução e endereçável enquanto oculto. Na v2.1.181 até v2.1.198, uma linha ociosa se ocultava 30 segundos após seu próprio turno terminar, mesmo enquanto outros companheiros de equipe ainda estavam trabalhando; linhas ociosas não são ocultadas em versões anteriores à v2.1.181.

Quando mais de três companheiros de equipe estão ociosos ao mesmo tempo, as linhas além das três primeiras se recolhem em uma única linha que conta os companheiros de equipe recolhidos, como `2 idle agents` quando cinco estão ociosos. Selecione-a e pressione Enter para expandir as linhas recolhidas, ou pressione Esc para recolhê-las novamente. Companheiros de equipe trabalhando, companheiros de equipe que falharam e o companheiro de equipe que você está visualizando sempre mantêm suas próprias linhas.

Se você quiser cada companheiro de equipe em seu próprio painel dividido, veja [Escolha um modo de exibição](#choose-a-display-mode).

<h2 id="control-your-agent-team">
  Controle sua equipe de agentes
</h2>

Diga ao líder o que você quer em linguagem natural. Ele lida com coordenação de equipe, atribuição de tarefas e delegação com base em suas instruções.

<h3 id="choose-a-display-mode">
  Escolha um modo de exibição
</h3>

Equipes de agentes suportam dois modos de exibição:

* **In-process**: todos os companheiros de equipe são executados dentro do seu terminal principal. Use as teclas de seta para cima e para baixo no painel de agentes para selecionar um companheiro de equipe, depois pressione Enter para visualizá-lo e digite para enviar mensagens para ele diretamente. Funciona em qualquer terminal, nenhuma configuração extra necessária.
* **Split panes**: cada companheiro de equipe recebe seu próprio painel. Você pode ver a saída de todos de uma vez e clicar em um painel para interagir diretamente. Requer tmux ou iTerm2.

<Note>
  `tmux` tem limitações conhecidas em certos sistemas operacionais e tradicionalmente funciona melhor no macOS. Usar `tmux -CC` no iTerm2 é o ponto de entrada sugerido para `tmux`.
</Note>

O padrão é `"in-process"`. Antes da v2.1.179, o padrão era `"auto"`, portanto sessões atualizadas que anteriormente abriam split panes agora permanecem em um terminal, a menos que você defina o modo explicitamente. Defina `"auto"` para ativar split panes quando você já estiver executando dentro de uma sessão tmux ou seu terminal for iTerm2, voltando para in-process caso contrário. A configuração `"tmux"` ativa o modo split-pane e detecta automaticamente se deve usar tmux ou iTerm2 com base no seu terminal.

A partir da v2.1.186, defina `"iterm2"` para usar explicitamente split panes nativos do iTerm2. Este modo requer o [CLI `it2`](https://github.com/mkusaka/it2) e mostra um erro com o comando de instalação se `it2` estiver faltando. O prompt de configuração que oferece instalar `it2` ou mudar para tmux aparece em `"auto"` ou `"tmux"` quando seu terminal é iTerm2 e tmux está disponível como fallback.

Para substituir o padrão, defina [`teammateMode`](https://code.claude.com/docs/pt/settings#available-settings) em `~/.claude/settings.json`:

```json theme={null}
{
  "teammateMode": "auto"
}
```

Para definir o modo para uma única sessão, passe como um sinalizador:

```bash theme={null}
claude --teammate-mode auto
```

O modo split-pane requer [tmux](https://github.com/tmux/tmux/wiki) ou iTerm2 com o CLI [`it2`](https://github.com/mkusaka/it2). Para instalar manualmente:

* **tmux**: instale através do gerenciador de pacotes do seu sistema. Veja o [wiki tmux](https://github.com/tmux/tmux/wiki/Installing) para instruções específicas da plataforma.
* **iTerm2**: instale o CLI [`it2`](https://github.com/mkusaka/it2), depois ative a API Python em **iTerm2 → Settings → General → Magic → Enable Python API**.

<h3 id="specify-teammates-and-models">
  Especifique companheiros de equipe e modelos
</h3>

Claude decide o número de companheiros de equipe a gerar com base em sua tarefa, ou você pode especificar exatamente o que deseja:

```text theme={null}
Spawn 4 teammates to refactor these modules in parallel. Use Sonnet for
each teammate.
```

Os companheiros de equipe não herdam a seleção `/model` do líder por padrão. Para alterar o modelo usado quando o prompt não especifica um, defina **Default teammate model** em `/config`. Escolha **Default (leader's model)** para que os companheiros de equipe sigam o modelo atual do líder.

Os companheiros de equipe herdam o [nível de esforço](https://code.claude.com/docs/pt/model-config#adjust-effort-level) do líder. No modo split-pane isso se aplica a partir da v2.1.186; versões anteriores não passavam o esforço da sessão do líder para companheiros de equipe em split-pane.

<h3 id="require-plan-approval-for-teammates">
  Exigir aprovação de plano para companheiros de equipe
</h3>

Para tarefas complexas ou arriscadas, você pode exigir que os companheiros de equipe planejem antes de implementar. O companheiro de equipe trabalha em modo de plano somente leitura até que o líder aprove sua abordagem:

```text theme={null}
Spawn an architect teammate to refactor the authentication module.
Require plan approval before they make any changes.
```

Quando um companheiro de equipe termina o planejamento, ele envia uma solicitação de aprovação de plano ao líder. O líder revisa o plano e o aprova ou o rejeita com feedback. Se rejeitado, o companheiro de equipe permanece em modo de plano, revisa com base no feedback e resubmete. Uma vez aprovado, o companheiro de equipe sai do modo de plano e começa a implementação.

O líder toma decisões de aprovação autonomamente. Para influenciar o julgamento do líder, dê a ele critérios no seu prompt, como "apenas aprove planos que incluam cobertura de testes" ou "rejeite planos que modifiquem o esquema do banco de dados".

<h3 id="talk-to-teammates-directly">
  Fale com companheiros de equipe diretamente
</h3>

Cada companheiro de equipe é uma sessão Claude Code completa e independente. Você pode enviar mensagens para qualquer companheiro de equipe diretamente para dar instruções adicionais, fazer perguntas de acompanhamento ou redirecionar sua abordagem.

* **Modo in-process**: use as teclas de seta para cima e para baixo no painel de agentes para selecionar um companheiro de equipe, depois pressione Enter para visualizar sua sessão e digite para enviar uma mensagem. Pressione `x` em um companheiro de equipe selecionado para interrompê-lo. Pressione Ctrl+T para alternar a lista de tarefas.
* **Modo split-pane**: clique em um painel de companheiro de equipe para interagir com sua sessão diretamente. Cada companheiro de equipe tem uma visualização completa de seu próprio terminal.

Enquanto você está visualizando um companheiro de equipe in-process, texto simples e [skills](https://code.claude.com/docs/pt/skills) vão para esse companheiro de equipe, mas comandos integrados ainda são executados na sessão do líder.

O modelo e modo rápido de um companheiro de equipe são fixos quando ele é gerado, portanto `/model` e `/fast` apenas alteram as configurações do líder. A partir da v2.1.199, digitar qualquer comando enquanto visualiza um companheiro de equipe mostra um aviso de que a alteração se aplica ao líder; versões anteriores a aplicavam ao líder sem indicação. `/effort` ainda se aplica aos turnos posteriores do companheiro de equipe visualizado, porque os companheiros de equipe seguem o [nível de esforço](https://code.claude.com/docs/pt/model-config#adjust-effort-level) do líder.

<h3 id="assign-and-claim-tasks">
  Atribuir e reivindicar tarefas
</h3>

A lista de tarefas compartilhada coordena o trabalho em toda a equipe. O líder cria tarefas e os companheiros de equipe as trabalham. As tarefas têm três estados: pendente, em progresso e concluída. As tarefas também podem depender de outras tarefas: uma tarefa pendente com dependências não resolvidas não pode ser reivindicada até que essas dependências sejam concluídas.

O líder pode atribuir tarefas explicitamente ou os companheiros de equipe podem auto-reivindicar:

* **Líder atribui**: diga ao líder qual tarefa dar a qual companheiro de equipe
* **Auto-reivindicar**: após terminar uma tarefa, um companheiro de equipe pega a próxima tarefa não atribuída e desbloqueada por conta própria

A reivindicação de tarefas usa bloqueio de arquivo para evitar condições de corrida quando múltiplos companheiros de equipe tentam reivindicar a mesma tarefa simultaneamente.

<h3 id="shut-down-teammates">
  Encerrar companheiros de equipe
</h3>

Para encerrar graciosamente a sessão de um companheiro de equipe, refira-se a ele pelo nome. Por exemplo, com um companheiro de equipe chamado pesquisador:

```text theme={null}
Ask the researcher teammate to shut down
```

O líder envia uma solicitação de encerramento. O companheiro de equipe pode aprovar, saindo graciosamente, ou rejeitar com uma explicação.

Os diretórios compartilhados da equipe são limpos automaticamente quando a sessão termina, portanto não há uma etapa de limpeza separada. Veja [Architecture](#architecture) para saber quais diretórios são removidos e quais persistem para sessões retomadas.

<h3 id="enforce-quality-gates-with-hooks">
  Aplicar gates de qualidade com hooks
</h3>

Use [hooks](https://code.claude.com/docs/pt/hooks) para aplicar regras quando os companheiros de equipe terminam o trabalho ou as tarefas são criadas ou concluídas:

* [`TeammateIdle`](https://code.claude.com/docs/pt/hooks#teammateidle): é executado quando um companheiro de equipe está prestes a ficar ocioso. Saia com código 2 para enviar feedback e manter o companheiro de equipe trabalhando.
* [`TaskCreated`](https://code.claude.com/docs/pt/hooks#taskcreated): é executado quando uma tarefa está sendo criada. Saia com código 2 para evitar criação e enviar feedback.
* [`TaskCompleted`](https://code.claude.com/docs/pt/hooks#taskcompleted): é executado quando uma tarefa está sendo marcada como concluída. Saia com código 2 para evitar conclusão e enviar feedback.

<h2 id="how-agent-teams-work">
  Como funcionam as equipes de agentes
</h2>

Esta seção cobre a arquitetura e a mecânica por trás das equipes de agentes. Se você quiser começar a usá-las, veja [Controle sua equipe de agentes](#control-your-agent-team) acima.

<h3 id="how-claude-starts-agent-teams">
  Como Claude inicia equipes de agentes
</h3>

Uma equipe de agentes se forma quando o primeiro companheiro de equipe é gerado, com a sessão principal atuando como líder. Existem duas maneiras pelas quais os companheiros de equipe são gerados:

* **Você solicita companheiros de equipe**: dê ao Claude uma tarefa que se beneficie do trabalho paralelo e peça explicitamente por companheiros de equipe. Claude os gera com base em suas instruções.
* **Claude propõe companheiros de equipe**: se Claude determinar que sua tarefa se beneficiaria do trabalho paralelo, pode sugerir gerar companheiros de equipe. Você confirma antes que ele proceda.

Em ambos os casos, você permanece no controle. Claude não gerará companheiros de equipe sem sua aprovação.

<h3 id="architecture">
  Arquitetura
</h3>

Uma equipe de agentes consiste em:

| Componente    | Papel                                                                                          |
| :------------ | :--------------------------------------------------------------------------------------------- |
| **Team lead** | A sessão Claude Code principal que gera companheiros de equipe e coordena o trabalho           |
| **Teammates** | Instâncias Claude Code separadas que cada uma trabalha em tarefas atribuídas                   |
| **Task list** | Lista compartilhada de itens de trabalho que os companheiros de equipe reivindicam e completam |
| **Mailbox**   | Sistema de mensagens para comunicação entre agentes                                            |

Veja [Escolha um modo de exibição](#choose-a-display-mode) para opções de configuração de exibição. As mensagens dos companheiros de equipe chegam ao líder automaticamente.

A caixa de correio de cada agente é um arquivo JSON em `~/.claude/teams/{team-name}/inboxes/{agent-name}.json`. Claude Code valida cada entrada quando lê um arquivo de caixa de correio. Entradas que não correspondem ao formato de mensagem são relatadas como erros e removidas do arquivo; as mensagens válidas ainda são entregues. Antes da v2.1.207, uma única entrada de caixa de correio malformada causava um erro repetido a cada segundo e bloqueava a entrega para essa caixa de correio até que você deletasse o arquivo manualmente.

O sistema gerencia dependências de tarefas automaticamente. Quando um companheiro de equipe completa uma tarefa da qual outras tarefas dependem, as tarefas bloqueadas são desbloqueadas sem intervenção manual.

Equipes e tarefas são armazenadas localmente sob um nome derivado da sessão. O nome é `session-` seguido pelos primeiros oito caracteres do ID da sessão:

* **Team config**: `~/.claude/teams/{team-name}/config.json`
* **Task list**: `~/.claude/tasks/{team-name}/`

Claude Code gera ambas automaticamente na inicialização da sessão e as atualiza conforme os companheiros de equipe entram, ficam ociosos ou saem. O diretório de configuração da equipe é removido quando a sessão termina. O diretório da lista de tarefas persiste localmente e nunca é carregado, portanto as sessões retomadas mantêm suas tarefas. A retenção é governada pelo mesmo [`cleanupPeriodDays`](https://code.claude.com/docs/pt/settings#available-settings) que você já controla para transcrições de sessão.

A configuração da equipe contém estado de tempo de execução, como IDs de sessão e IDs de painel tmux, portanto não a edite manualmente ou a crie previamente: suas alterações são sobrescritas na próxima atualização de estado.

Para definir papéis de companheiros de equipe reutilizáveis, use [definições de subagent](#use-subagent-definitions-for-teammates) em vez disso.

A configuração da equipe contém um array `members` com o nome de cada companheiro de equipe, ID do agente e tipo de agente. Os companheiros de equipe podem ler este arquivo para descobrir outros membros da equipe.

Não há equivalente em nível de projeto da configuração da equipe. Um arquivo como `.claude/teams/teams.json` no seu diretório de projeto não é reconhecido como configuração; Claude o trata como um arquivo ordinário.

<h3 id="use-subagent-definitions-for-teammates">
  Use subagent definitions for teammates
</h3>

Ao gerar um companheiro de equipe, você pode referenciar um tipo de [subagent](https://code.claude.com/docs/pt/sub-agents) de qualquer [escopo de subagent](https://code.claude.com/docs/pt/sub-agents#choose-the-subagent-scope): projeto, usuário, plugin ou definido por CLI. Isso permite que você defina um papel uma vez, como um revisor de segurança ou executor de testes, e o reutilize tanto como um subagent delegado quanto como um companheiro de equipe de equipe de agentes.

Para usar uma definição de subagent, mencione-a pelo nome ao pedir ao Claude para gerar o companheiro de equipe:

```text theme={null}
Spawn a teammate using the security-reviewer agent type to audit the auth module.
```

O companheiro de equipe honra a lista de permissão `tools` dessa definição e `model`, e o corpo da definição é anexado ao prompt do sistema do companheiro de equipe como instruções adicionais em vez de substituí-lo. Ferramentas de coordenação de equipe como `SendMessage` e as ferramentas de gerenciamento de tarefas estão sempre disponíveis para um companheiro de equipe, mesmo quando `tools` restringe outras ferramentas.

<Note>
  Os campos frontmatter `skills` e `mcpServers` em uma definição de subagent não são aplicados quando essa definição é executada como um companheiro de equipe. Os companheiros de equipe carregam skills e MCP servers de suas configurações de projeto e usuário, assim como uma sessão regular.
</Note>

<h3 id="permissions">
  Permissões
</h3>

Os companheiros de equipe começam com as configurações de permissão do líder. Se o líder for executado com `--dangerously-skip-permissions`, todos os companheiros de equipe também. Após gerar, você pode alterar modos de companheiros de equipe individuais, mas não pode definir modos por companheiro de equipe no tempo de geração.

Quando um agente envia uma mensagem para outro sobre `SendMessage`, o agente receptor é informado de que veio de outra sessão Claude, não de você. Um companheiro de equipe não pode aprovar um prompt de permissão ou fornecer consentimento em seu nome, e um companheiro de equipe que foi negado uma ação não pode retransmiti-la para outro companheiro de equipe para contornar a verificação. Em [modo automático](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode), o classificador trata uma reivindicação de aprovação retransmitida de outro agente como entrada não confiável em vez de confirmação de você.

Os prompts de permissão de companheiros de equipe aparecem na sessão líder, portanto aprove-os lá você mesmo. [Aprovação de plano](#require-plan-approval-for-teammates) é a exceção projetada: a sessão líder concede aprovações de plano de companheiros de equipe sem um prompt separado para você.

<h3 id="context-and-communication">
  Context e comunicação
</h3>

Cada companheiro de equipe tem sua própria context window. Quando gerado, um companheiro de equipe carrega o mesmo contexto de projeto que uma sessão regular: CLAUDE.md, MCP servers e skills. Ele também recebe o prompt de geração do líder. O histórico de conversa do líder não é transferido.

**Como os companheiros de equipe compartilham informações:**

* **Entrega automática de mensagens**: quando os companheiros de equipe enviam mensagens, elas são entregues automaticamente aos destinatários. O líder não precisa fazer polling para atualizações.
* **Notificações de ociosidade**: quando um companheiro de equipe termina e para, ele notifica automaticamente o líder. A partir da v2.1.198, um companheiro de equipe cuja vez termina em um erro de API notifica o líder que falhou e inclui o texto do erro, em vez de parecer terminar normalmente.
* **Lista de tarefas compartilhada**: todos os agentes podem ver o status da tarefa e reivindicar trabalho disponível.
* **Mensagens de companheiros de equipe**: envie uma mensagem para um companheiro de equipe específico pelo nome. Para alcançar todos, envie uma mensagem por destinatário.

O líder atribui a cada companheiro de equipe um nome quando o gera, e qualquer companheiro de equipe pode enviar mensagens para qualquer outro por esse nome. Para obter nomes previsíveis que você possa referenciar em prompts posteriores, diga ao líder como chamar cada companheiro de equipe em sua instrução de geração.

<h3 id="token-usage">
  Uso de tokens
</h3>

Equipes de agentes usam significativamente mais tokens do que uma única sessão. Cada companheiro de equipe tem sua própria context window, e o uso de tokens escala com o número de companheiros de equipe ativos. Para pesquisa, revisão e trabalho de novos recursos, os tokens extras geralmente valem a pena. Para tarefas rotineiras, uma única sessão é mais econômica. Veja [custos de token de equipe de agentes](https://code.claude.com/docs/pt/costs#agent-team-token-costs) para orientação de uso.

<h2 id="use-case-examples">
  Exemplos de casos de uso
</h2>

Estes exemplos mostram como as equipes de agentes lidam com tarefas onde a exploração paralela adiciona valor.

<h3 id="run-a-parallel-code-review">
  Executar uma revisão de código paralela
</h3>

Um único revisor tende a gravitar em torno de um tipo de problema por vez. Dividir critérios de revisão em domínios independentes significa que segurança, desempenho e cobertura de testes recebem atenção completa simultaneamente. O prompt atribui a cada companheiro de equipe uma lente distinta para que não se sobreponham:

```text theme={null}
Spawn three teammates to review PR #142:
- One focused on security implications
- One checking performance impact
- One validating test coverage
Have them each review and report findings.
```

Cada revisor trabalha a partir do mesmo PR, mas aplica um filtro diferente. O líder sintetiza descobertas em todos os três após terminarem.

<h3 id="investigate-with-competing-hypotheses">
  Investigar com hipóteses concorrentes
</h3>

Quando a causa raiz é incerta, um único agente tende a encontrar uma explicação plausível e parar de procurar. O prompt combate isso tornando os companheiros de equipe explicitamente adversários: o trabalho de cada um não é apenas investigar sua própria teoria, mas desafiar as dos outros.

```text theme={null}
Users report the app exits after one message instead of staying connected.
Spawn 5 agent teammates to investigate different hypotheses. Have them talk to
each other to try to disprove each other's theories, like a scientific
debate. Update the findings doc with whatever consensus emerges.
```

A estrutura de debate é o mecanismo-chave aqui. A investigação sequencial sofre de ancoragem: uma vez que uma teoria é explorada, a investigação subsequente é enviesada em relação a ela.

Com múltiplos investigadores independentes tentando ativamente desprovar uns aos outros, a teoria que sobrevive é muito mais provável de ser a causa raiz real.

<h2 id="best-practices">
  Melhores práticas
</h2>

<h3 id="give-teammates-enough-context">
  Dê aos companheiros de equipe contexto suficiente
</h3>

Os companheiros de equipe carregam contexto de projeto automaticamente, incluindo CLAUDE.md, MCP servers e skills, mas não herdam o histórico de conversa do líder. Veja [Context e comunicação](#context-and-communication) para detalhes. Inclua detalhes específicos da tarefa no prompt de geração:

```text theme={null}
Spawn a security reviewer teammate with the prompt: "Review the authentication module
at src/auth/ for security vulnerabilities. Focus on token handling, session
management, and input validation. The app uses JWT tokens stored in
httpOnly cookies. Report any issues with severity ratings."
```

<h3 id="choose-an-appropriate-team-size">
  Escolha um tamanho de equipe apropriado
</h3>

Não há limite rígido no número de companheiros de equipe, mas restrições práticas se aplicam:

* **Custos de token escalam linearmente**: cada companheiro de equipe tem sua própria context window e consome tokens independentemente. Veja [custos de token de equipe de agentes](https://code.claude.com/docs/pt/costs#agent-team-token-costs) para detalhes.
* **Sobrecarga de coordenação aumenta**: mais companheiros de equipe significa mais comunicação, coordenação de tarefas e potencial para conflitos
* **Retornos decrescentes**: além de um certo ponto, companheiros de equipe adicionais não aceleram o trabalho proporcionalmente

Comece com 3-5 companheiros de equipe para a maioria dos fluxos de trabalho. Isso equilibra o trabalho paralelo com coordenação gerenciável. Os exemplos neste guia usam 3-5 companheiros de equipe porque esse intervalo funciona bem em diferentes tipos de tarefas.

Ter 5-6 [tasks](https://code.claude.com/docs/pt/agent-teams#architecture) por companheiro de equipe mantém todos produtivos sem alternância de contexto excessiva. Se você tiver 15 tarefas independentes, 3 companheiros de equipe é um bom ponto de partida.

Escale apenas quando o trabalho genuinamente se beneficiar de ter companheiros de equipe trabalhando simultaneamente. Três companheiros de equipe focados frequentemente superam cinco dispersos.

<h3 id="size-tasks-appropriately">
  Dimensione tarefas apropriadamente
</h3>

* **Muito pequeno**: sobrecarga de coordenação excede o benefício
* **Muito grande**: companheiros de equipe trabalham muito tempo sem check-ins, aumentando o risco de esforço desperdiçado
* **Bem dimensionado**: unidades auto-contidas que produzem um entregável claro, como uma função, um arquivo de teste ou uma revisão

<Tip>
  O líder divide o trabalho em tarefas e as atribui aos companheiros de equipe automaticamente. Se não estiver criando tarefas suficientes, peça a ele para dividir o trabalho em pedaços menores. Ter 5-6 tarefas por companheiro de equipe mantém todos produtivos e permite que o líder reatribua trabalho se alguém ficar preso.
</Tip>

<h3 id="wait-for-teammates-to-finish">
  Espere os companheiros de equipe terminarem
</h3>

Às vezes, o líder começa a implementar tarefas em vez de esperar pelos companheiros de equipe. Se você notar isso:

```text theme={null}
Wait for your teammates to complete their tasks before proceeding
```

<h3 id="start-with-research-and-review">
  Comece com pesquisa e revisão
</h3>

Se você é novo em equipes de agentes, comece com tarefas que têm limites claros e não requerem escrever código: revisar um PR, pesquisar uma biblioteca ou investigar um bug. Essas tarefas mostram o valor da exploração paralela sem os desafios de coordenação que vêm com a implementação paralela.

<h3 id="avoid-file-conflicts">
  Evite conflitos de arquivo
</h3>

Dois companheiros de equipe editando o mesmo arquivo leva a sobrescrita. Divida o trabalho para que cada companheiro de equipe possua um conjunto diferente de arquivos.

<h3 id="monitor-and-steer">
  Monitore e direcione
</h3>

Verifique o progresso dos companheiros de equipe, redirecione abordagens que não estão funcionando e sintetize descobertas conforme chegam. Deixar uma equipe executar sem supervisão por muito tempo aumenta o risco de esforço desperdiçado.

<h2 id="troubleshooting">
  Troubleshooting
</h2>

<h3 id="teammates-not-appearing">
  Companheiros de equipe não aparecem
</h3>

Se os companheiros de equipe não aparecerem depois que você pedir ao Claude para criar uma equipe:

* No modo in-process, os companheiros de equipe aparecem no painel de agentes abaixo da entrada de prompt. Use as teclas de seta para cima e para baixo para selecionar um e pressione Enter para visualizá-lo.
* Uma linha de companheiro de equipe que desapareceu após ficar inativa foi ocultada, não interrompida. As linhas inativas se ocultam 30 segundos após o painel inteiro ficar inativo e reaparecem na próxima vez do companheiro de equipe. Quando mais de três companheiros de equipe estão inativos, suas linhas excedentes se recolhem em uma única linha `N idle agents` que Enter expande. Envie uma mensagem ao companheiro de equipe pelo nome para trazer uma linha oculta de volta.
* Verifique se a tarefa que você deu ao Claude era complexa o suficiente para justificar uma equipe. Claude decide se deve gerar companheiros de equipe com base na tarefa.
* Se você explicitamente solicitou split panes, certifique-se de que tmux está instalado e disponível no seu PATH:
  ```bash theme={null}
  which tmux
  ```
* Para iTerm2, verifique se o CLI `it2` está instalado e a API Python está ativada nas preferências do iTerm2.

<h3 id="too-many-permission-prompts">
  Muitos prompts de permissão
</h3>

Solicitações de permissão de companheiros de equipe surgem para o líder, o que pode criar atrito. Pré-aprove operações comuns nas suas [configurações de permissão](https://code.claude.com/docs/pt/permissions) antes de gerar companheiros de equipe para reduzir interrupções.

<h3 id="teammates-stopping-on-errors">
  Companheiros de equipe parando em erros
</h3>

Os companheiros de equipe podem parar após encontrar erros em vez de se recuperar. Verifique sua saída selecionando o companheiro de equipe no painel de agentes e pressionando Enter no modo in-process, ou clicando no painel no modo split, depois:

* Dê a eles instruções adicionais diretamente
* Gere um companheiro de equipe de substituição para continuar o trabalho

A partir da v2.1.198, uma mensagem do líder ou de outro companheiro de equipe acorda um companheiro de equipe in-process que está aguardando para tentar novamente uma solicitação de API com falha, para que ele tente novamente imediatamente em vez de aguardar o atraso de repetição completo.

<h3 id="lead-shuts-down-before-work-is-done">
  Líder encerra antes do trabalho estar pronto
</h3>

O líder pode decidir que a equipe terminou antes de todas as tarefas estarem realmente completas. Se isso acontecer, diga a ele para continuar. Você também pode dizer ao líder para esperar os companheiros de equipe terminarem antes de prosseguir se ele começar a fazer trabalho em vez de delegar.

<h3 id="orphaned-tmux-sessions">
  Sessões tmux órfãs
</h3>

Se uma sessão tmux persistir após a equipe terminar, pode não ter sido totalmente limpa. Liste as sessões e mate a criada pela equipe:

```bash theme={null}
tmux ls
tmux kill-session -t <session-name>
```

<h2 id="limitations">
  Limitações
</h2>

Equipes de agentes são experimentais. Limitações atuais a serem observadas:

* **Sem retomada de sessão com companheiros de equipe in-process**: `/resume` e `/rewind` não restauram companheiros de equipe in-process. Após retomar uma sessão, o líder pode tentar enviar mensagens para companheiros de equipe que não existem mais. Se isso acontecer, diga ao líder para gerar novos companheiros de equipe.
* **Status da tarefa pode ficar atrasado**: os companheiros de equipe às vezes falham em marcar tarefas como concluídas, o que bloqueia tarefas dependentes. Se uma tarefa parecer presa, verifique se o trabalho está realmente pronto e atualize o status da tarefa manualmente ou diga ao líder para dar um empurrão ao companheiro de equipe.
* **Encerramento pode ser lento**: os companheiros de equipe terminam sua solicitação atual ou chamada de ferramenta antes de encerrar, o que pode levar tempo.
* **Uma equipe por sessão**: uma sessão tem exatamente uma equipe, com escopo para essa sessão. Você não pode criar equipes nomeadas adicionais ou compartilhar uma equipe entre sessões.
* **Sem equipes aninhadas**: os companheiros de equipe não podem gerar seus próprios companheiros de equipe. Apenas o líder pode gerenciar a equipe.
* **Sem subagentes em segundo plano de companheiros de equipe in-process**: os próprios subagentes de um companheiro de equipe in-process são executados em primeiro plano. Pedir por um em segundo plano, seja com `run_in_background` ou uma definição de subagente que define `background: true`, retorna um erro, porque o trabalho em segundo plano de um companheiro de equipe não pode sobreviver ao processo do líder. Subagentes lançados da conversa principal seguem o [padrão de segundo plano](https://code.claude.com/docs/pt/sub-agents#run-subagents-in-foreground-or-background).
* **Líder é fixo**: a sessão principal é o líder por sua vida útil. Você não pode promover um companheiro de equipe a líder ou transferir liderança.
* **Permissões definidas no tempo de geração**: todos os companheiros de equipe começam com o modo de permissão do líder. Você pode alterar modos de companheiros de equipe individuais após gerar, mas não pode definir modos por companheiro de equipe no tempo de geração.
* **Split panes requerem tmux ou iTerm2**: o modo in-process padrão funciona em qualquer terminal. O modo split-pane não é suportado no terminal integrado do VS Code, Windows Terminal ou Ghostty.

<Tip>
  **`CLAUDE.md` funciona normalmente**: os companheiros de equipe leem arquivos `CLAUDE.md` de seu diretório de trabalho. Use isso para fornecer orientação específica do projeto a todos os companheiros de equipe.
</Tip>

<h2 id="next-steps">
  Próximos passos
</h2>

Explore abordagens relacionadas para trabalho paralelo e delegação:

* **Delegação leve**: [subagents](https://code.claude.com/docs/pt/sub-agents) geram agentes auxiliares para pesquisa ou verificação dentro de sua sessão, melhor para tarefas que não precisam de coordenação entre agentes
* **Sessões paralelas manuais**: [Git worktrees](https://code.claude.com/docs/pt/worktrees) permitem que você execute múltiplas sessões Claude Code você mesmo sem coordenação de equipe automatizada
* **Comparar abordagens**: veja a comparação [subagent vs agent team](https://code.claude.com/docs/pt/features-overview#compare-similar-features) para um detalhamento lado a lado
