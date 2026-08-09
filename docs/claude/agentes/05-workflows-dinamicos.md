<!-- Fonte: https://code.claude.com/docs/pt/workflows | Baixado em: 2026-08-06 | slug: workflows -->

# Orquestre subagentos em escala com fluxos de trabalho dinâmicos

> Fluxos de trabalho dinâmicos orquestram muitos subagentos a partir de um script que Claude escreve e você pode executar novamente. Use-os para auditorias de base de código, grandes migrações e pesquisa com verificação cruzada.

<Note>
  Fluxos de trabalho dinâmicos exigem Claude Code v2.1.154 ou posterior e estão disponíveis em todos os planos pagos, com acesso à API Anthropic, e no Amazon Bedrock, Google Cloud's Agent Platform e Microsoft Foundry. No Pro, ative-os na linha Dynamic workflows em `/config`.
</Note>

Um fluxo de trabalho dinâmico é um script JavaScript que orquestra [subagentos](https://code.claude.com/docs/pt/sub-agents) em escala. Claude escreve o script para a tarefa que você descreve, e um runtime o executa em segundo plano enquanto sua sessão permanece responsiva.

Recorra a um fluxo de trabalho quando uma tarefa precisar de mais agentes do que uma conversa pode coordenar, ou quando você quiser que a orquestração seja codificada como um script que você possa ler e executar novamente. Os exemplos incluem uma varredura de bugs em toda a base de código, uma migração de 500 arquivos, uma pergunta de pesquisa que precisa ter fontes verificadas cruzadamente uma contra a outra, e um plano difícil que vale a pena ser elaborado de vários ângulos independentes antes de você se comprometer com um.

<h2 id="when-to-use-a-workflow">
  Quando usar um fluxo de trabalho
</h2>

[Subagentos](https://code.claude.com/docs/pt/sub-agents), [skills](https://code.claude.com/docs/pt/skills), [equipes de agentes](https://code.claude.com/docs/pt/agent-teams) e fluxos de trabalho podem todos executar uma tarefa com várias etapas. A diferença é quem mantém o plano:

|                                         | Subagentos                          | Skills                       | Equipes de agentes                                  | Fluxos de trabalho                         |
| :-------------------------------------- | :---------------------------------- | :--------------------------- | :-------------------------------------------------- | :----------------------------------------- |
| O que é                                 | Um worker Claude que spawna         | Instruções que Claude segue  | Um agente líder supervisionando sessões entre pares | Um script que o runtime executa            |
| Quem decide o que é executado a seguir  | Claude, turno por turno             | Claude, seguindo o prompt    | O agente líder, turno por turno                     | O script                                   |
| Onde os resultados intermediários vivem | Janela de contexto de Claude        | Janela de contexto de Claude | Uma lista de tarefas compartilhada                  | Variáveis de script                        |
| O que é repetível                       | A definição do worker               | As instruções                | A definição da equipe                               | A orquestração em si                       |
| Escala                                  | Algumas tarefas delegadas por turno | Igual aos subagentos         | Um punhado de pares de longa duração                | Dezenas a centenas de agentes por execução |
| Interrupção                             | Reinicia o turno                    | Reinicia o turno             | Os companheiros de equipe continuam executando      | Retomável na mesma sessão                  |

Um fluxo de trabalho move o plano para o código. Com subagentos, skills e equipes de agentes, Claude é o orquestrador: ele decide turno por turno o que spawnar ou atribuir a seguir, e cada resultado chega à janela de contexto. Um script de fluxo de trabalho mantém o loop, a ramificação e os resultados intermediários em si, então o contexto de Claude contém apenas a resposta final.

Mover o plano para o código também permite que um fluxo de trabalho aplique um padrão de qualidade repetível, não apenas execute mais agentes: ele pode ter agentes independentes revisando adversarialmente as descobertas um do outro antes de serem relatadas, ou elaborar um plano de vários ângulos e pesá-los um contra o outro, para que você obtenha um resultado mais confiável do que uma única passagem.

<h2 id="run-a-bundled-workflow">
  Executar um fluxo de trabalho agrupado
</h2>

A maneira mais rápida de ver um fluxo de trabalho em ação é executar `/deep-research`, o [fluxo de trabalho integrado](#bundled-workflows) que Claude Code inclui para investigar uma pergunta em muitas fontes. Você verá agentes trabalhando através de um conjunto de fases em segundo plano enquanto sua sessão permanece livre, e obterá um relatório no final em vez de uma transcrição turno por turno.

<Steps>
  <Step title="Executar o fluxo de trabalho">
    Execute `/deep-research` com uma pergunta que você deseja investigar. Ele distribui buscas na web em vários ângulos, busca e verifica cruzadamente as fontes que encontra, e sintetiza um relatório citado.

    ```text theme={null}
    /deep-research What changed in the Node.js permission model between v20 and v22?
    ```
  </Step>

  <Step title="Permitir fluxos de trabalho">
    Claude Code pergunta se deve permitir o fluxo de trabalho. Selecione **Sim** para continuar. O prompt exato depende do seu modo de permissão. Consulte [Aprovar o plano antes de ser executado](#approve-the-plan-before-it-runs) para as opções por modo.
  </Step>

  <Step title="Observar o progresso">
    A execução começa em segundo plano. Execute `/workflows`, use as setas para selecionar a execução e pressione Enter para abrir sua visualização de progresso:

    ```text theme={null}
    /workflows
    ```

    A visualização mostra cada fase com sua contagem de agentes, total de tokens e tempo decorrido. Aprofunde-se em qualquer fase para ver seus agentes e o que cada um encontrou. Consulte [Observar a execução](#watch-the-run) para o conjunto completo de controles.

    Você também pode observar no painel de tarefas abaixo da caixa de entrada: um resumo de progresso de uma linha aparece lá enquanto a execução está em andamento. Pressione a seta para baixo para focá-lo e depois Enter para expandir.
  </Step>

  <Step title="Ler o relatório">
    Quando a execução termina, o relatório chega em sua sessão. Ele cita as fontes de cada afirmação, com afirmações que não sobreviveram à verificação cruzada já filtradas.

    A partir da v2.1.196, quando os agentes verificadores não conseguem verificar uma afirmação, como após um limite de taxa ou erro de API, o relatório lista essa afirmação como não verificada em vez de contá-la como refutada.
  </Step>
</Steps>

Para executar um fluxo de trabalho para sua própria tarefa, [faça Claude escrever um](#have-claude-write-a-workflow), e uma vez que uma execução faça o que você queria, você pode [salvá-lo](#save-the-workflow-for-reuse) como um comando seu.

<h3 id="bundled-workflows">
  Fluxos de trabalho agrupados
</h3>

Claude Code inclui `/deep-research` como um fluxo de trabalho integrado:

| Comando                     | O que faz                                                                                                                                                                                                                                                                                                                                     |
| :-------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/deep-research <question>` | Distribui buscas na web em uma pergunta em vários ângulos, busca e verifica cruzadamente as fontes que encontra, vota em cada afirmação e retorna um relatório citado com afirmações que não sobreviveram à verificação cruzada filtradas. Requer que a [ferramenta WebSearch](https://code.claude.com/docs/pt/tools-reference#websearch-tool-behavior) esteja disponível |

[Fluxos de trabalho que você salva](#save-the-workflow-for-reuse) você mesmo se tornam comandos da mesma forma e aparecem no autocomplete `/` junto com os agrupados.

<h3 id="watch-the-run">
  Observar a execução
</h3>

Fluxos de trabalho são executados em segundo plano, então a sessão permanece responsiva enquanto os agentes trabalham. Execute `/workflows` a qualquer momento para listar fluxos de trabalho em execução e concluídos, depois selecione um para abrir sua visualização de progresso.

```text theme={null}
/workflows
```

A visualização de progresso mostra cada fase com suas contagens de agentes, totais de tokens e tempo decorrido. O rodapé lista a chave para cada ação:

| Chave          | Ação                                                                                                                     |
| :------------- | :----------------------------------------------------------------------------------------------------------------------- |
| `↑` / `↓`      | Selecionar uma fase ou agente                                                                                            |
| `Enter` ou `→` | Aprofundar-se na fase selecionada, depois em um agente para ler seu prompt, chamadas de ferramentas recentes e resultado |
| `Esc` ou `←`   | Voltar um nível. Na v2.1.203 até v2.1.205, `←` não voltava para fora de uma fase ou agente; use `Esc` nessas versões     |
| `j` / `k`      | Rolar dentro do detalhe do agente quando transborda                                                                      |
| `f`            | Filtrar a lista de agentes na fase selecionada por status. Pressione novamente para ciclar                               |
| `p`            | Pausar ou retomar a execução                                                                                             |
| `x`            | Parar o agente selecionado, ou parar todo o fluxo de trabalho quando o foco está na execução                             |
| `r`            | Reiniciar o agente em execução selecionado                                                                               |
| `s`            | [Salvar](#save-the-workflow-for-reuse) o script da execução como um comando                                              |

<h2 id="have-claude-write-a-workflow">
  Fazer Claude escrever um fluxo de trabalho
</h2>

Você pode fazer Claude escrever um fluxo de trabalho para sua tarefa de duas maneiras:

* [Peça um fluxo de trabalho](#ask-for-a-workflow-in-your-prompt) em seu prompt, seja com suas próprias palavras ou incluindo a palavra-chave `ultracode`, e Claude escreve um para a tarefa.
* [Deixe Claude decidir com ultracode](#let-claude-decide-with-ultracode): defina `/effort ultracode` e Claude planeja um fluxo de trabalho para cada tarefa substancial na sessão.

Você também pode executar um comando de fluxo de trabalho que já existe: um [fluxo de trabalho agrupado](#bundled-workflows) como `/deep-research`, ou um que você [salvou](#save-the-workflow-for-reuse).

<h3 id="ask-for-a-workflow-in-your-prompt">
  Peça um fluxo de trabalho em seu prompt
</h3>

Para executar uma única tarefa como um fluxo de trabalho sem alterar o nível de esforço da sessão, inclua a palavra-chave `ultracode` em seu prompt. Pedir com suas próprias palavras, por exemplo "use um fluxo de trabalho" ou "execute um fluxo de trabalho", também funciona: Claude trata uma solicitação direta como o mesmo opt-in. Antes da v2.1.160 a palavra-chave de gatilho literal era `workflow`; solicitações em linguagem natural funcionam em ambas as versões.

```text theme={null}
ultracode: audit every API endpoint under src/routes/ for missing auth checks
```

Claude Code destaca a palavra-chave em sua entrada e Claude escreve um script de fluxo de trabalho para a tarefa em vez de trabalhar através dela turno por turno. Se você não pretendia iniciar um fluxo de trabalho, pressione `Option+W` no macOS ou `Alt+W` no Windows e Linux para descartar o destaque para este prompt, ou pressione backspace enquanto o cursor está logo após a palavra-chave destacada. Para impedir que a palavra-chave dispare, desative o gatilho de palavra-chave Ultracode em `/config`.

Se a execução fez o que você queria, você pode [salvá-la como um comando](#save-the-workflow-for-reuse) depois.

Se você já tem um orquestrador construído de outra forma, como uma pasta de prompts de subagentos ou uma skill que distribui trabalho, você pode apontar Claude para ele e pedir um fluxo de trabalho que faça a mesma coisa.

<h3 id="let-claude-decide-with-ultracode">
  Deixe Claude decidir com ultracode
</h3>

Ultracode é uma configuração de Claude Code que combina `xhigh` [esforço de raciocínio](https://code.claude.com/docs/pt/model-config#adjust-effort-level) com orquestração automática de fluxo de trabalho. Com ele ativado, Claude planeja um fluxo de trabalho para cada tarefa substancial em vez de esperar você pedir.

```text theme={null}
/effort ultracode
```

Para iniciar uma sessão com ultracode já ativado, inicie com `claude --effort ultracode`. Requer Claude Code v2.1.203 ou posterior.

Com ultracode ativado, Claude decide quando uma tarefa justifica um fluxo de trabalho. Uma única solicitação pode se transformar em vários fluxos de trabalho seguidos: um para entender o código, um para fazer a alteração e um para verificá-la. Isso se aplica a cada tarefa na sessão, então cada solicitação usa mais tokens e leva mais tempo do que em níveis de esforço mais baixos.

Ultracode dura para a sessão atual e é redefinido quando você inicia uma nova. Volte com `/effort high` quando retornar ao trabalho de rotina. Está disponível em modelos que suportam `xhigh` [esforço](https://code.claude.com/docs/pt/model-config#adjust-effort-level); em outros modelos o menu `/effort` não o oferece.

<h3 id="approve-the-plan-before-it-runs">
  Aprovar o plano antes de ser executado
</h3>

Na CLI, o prompt por execução mostra as fases planejadas e estas opções:

* **Sim, execute**: inicie a execução
* **Sim, e não pergunte novamente para `<name>` em `<path>`**: inicie e pule este prompt para este fluxo de trabalho neste projeto a partir de agora
* **Ver script bruto**: leia o script antes de decidir
* **Não**: cancelar

`Ctrl+G` abre o script em seu editor. `Tab` permite que você ajuste o prompt antes da execução começar.

Se você vê este prompt depende do seu [modo de permissão](https://code.claude.com/docs/pt/permission-modes):

| Modo de permissão                            | Quando você é solicitado                                                                                                                                                                                       |
| :------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Padrão, aceitar edições                      | Cada execução, a menos que você tenha selecionado **Sim, e não pergunte novamente** para esse fluxo de trabalho neste projeto                                                                                  |
| Auto                                         | Apenas no primeiro lançamento. Qualquer **Sim** registra consentimento em suas configurações de usuário, e lançamentos posteriores começam sem solicitar. Ignorado completamente quando ultracode está ativado |
| Contornar permissões, `claude -p`, Agent SDK | Nunca. A execução começa imediatamente                                                                                                                                                                         |

No aplicativo Desktop, um cartão de aprovação mostra o nome do fluxo de trabalho, a lista de fases e um aviso de uso de token, com ações **Uma vez**, **Sempre** e **Negar**. A visualização de progresso aparece no painel de tarefas em segundo plano.

Seu modo de permissão controla apenas o prompt de lançamento acima. Os subagentos que o fluxo de trabalho spawna sempre são executados no modo `acceptEdits` e herdam sua [lista de permissão de ferramentas](https://code.claude.com/docs/pt/settings#permission-settings), independentemente do modo de sua sessão. As edições de arquivo são aprovadas automaticamente.

Comandos shell, buscas na web e ferramentas MCP que não estão em sua lista de permissão ainda podem solicitá-lo durante a execução. Para evitar isso em uma execução longa, adicione os comandos que os agentes precisam à sua lista de permissão antes de começar.

Em `claude -p` e no Agent SDK não há ninguém para solicitar, então as chamadas de ferramentas seguem suas regras de permissão configuradas sem confirmação interativa.

<h3 id="save-the-workflow-for-reuse">
  Salvar o fluxo de trabalho para reutilização
</h3>

Quando Claude escreve um fluxo de trabalho para uma tarefa que você repetirá, você pode salvar o script dessa execução como um comando. Um processo como uma revisão que você executa em cada branch então executa a mesma orquestração cada vez.

Execute `/workflows`, selecione a execução que você deseja manter e pressione `s`. Na caixa de diálogo de salvamento, Tab alterna entre os dois locais de salvamento:

* `.claude/workflows/` em seu projeto: compartilhado com todos que clonam o repositório
* `~/.claude/workflows/` em seu diretório inicial: disponível em cada projeto, visível apenas para você. Se você definir [`CLAUDE_CONFIG_DIR`](https://code.claude.com/docs/pt/env-vars), este local é o diretório `workflows/` sob esse caminho.

A caixa de diálogo de salvamento mostra o caminho resolvido para o local pessoal. Antes da v2.1.208, ela mostrava `~/.claude/workflows/` mesmo quando `CLAUDE_CONFIG_DIR` estava definido; o arquivo ainda era salvo sob o diretório configurado.

Pressione Enter para salvar. O fluxo de trabalho é executado como `/<name>` em futuras sessões de qualquer local.

Em um monorepo com vários diretórios `.claude/`, você pode manter fluxos de trabalho ao lado do pacote ao qual se aplicam. A partir da v2.1.178, salvar no local do projeto escreve no diretório `.claude/workflows/` mais próximo que já existe entre seu diretório de trabalho e a raiz do repositório, ou para a raiz do repositório se nenhum existir ainda. Os fluxos de trabalho do projeto também carregam de cada `.claude/workflows/` ao longo desse caminho, e quando mais de um define o mesmo nome Claude Code executa o mais próximo do diretório de trabalho.

Se um fluxo de trabalho de projeto e um fluxo de trabalho pessoal compartilham um nome, o do projeto é executado.

<h3 id="pass-input-to-a-saved-workflow">
  Passar entrada para um fluxo de trabalho salvo
</h3>

Um fluxo de trabalho salvo pode aceitar entrada através do parâmetro `args`. O script o lê como um global nomeado `args`. Use isso para fornecer uma pergunta de pesquisa, uma lista de caminhos de destino ou um objeto de configuração no momento da invocação em vez de editar o script para cada execução.

O prompt a seguir executa um fluxo de trabalho salvo com uma lista de números de problemas:

```text theme={null}
> Run /triage-issues on issues 1024, 1025, and 1030
```

Claude passa a lista como dados estruturados, então o script pode chamar métodos de array e objeto em `args` diretamente sem analisá-lo primeiro. Se `args` for omitido, o global é `undefined` dentro do script.

<h2 id="example-workflow-prompts">
  Exemplos de prompts de fluxo de trabalho
</h2>

Um fluxo de trabalho se encaixa melhor quando a tarefa é maior do que um agente pode manter em contexto, ou quando o mesmo passo precisa ser executado em muitos itens. Os prompts abaixo mostram formas comuns. Cada um pede a Claude para escrever e executar um fluxo de trabalho para essa tarefa; você não escreve o script você mesmo.

<h3 id="audit-many-files-for-the-same-issue">
  Auditar muitos arquivos para o mesmo problema
</h3>

Distribua um agente por arquivo, depois colete e verifique as descobertas.

```text theme={null}
> use a workflow to audit every route handler under src/routes/ for missing authentication checks, and adversarially verify each finding before reporting it
```

<h3 id="keep-fixing-until-a-check-passes">
  Continuar corrigindo até uma verificação passar
</h3>

Execute um verificador, corrija o que falhou e repita até passar ou parar de fazer progresso.

```text theme={null}
> use a workflow to run npx tsc --noEmit and keep fixing the reported errors until the type check passes or two rounds in a row make no progress
```

<h3 id="migrate-many-files-in-parallel">
  Migrar muitos arquivos em paralelo
</h3>

Descubra os arquivos a migrar, transforme cada um em uma cópia isolada para que as edições não entrem em conflito, e verifique cada resultado.

```text theme={null}
> use a workflow to migrate every component under src/components/ from styled-components to Tailwind, working on each file in its own isolated copy
```

<h3 id="review-every-changed-file-and-write-one-summary">
  Revisar cada arquivo alterado e escrever um resumo
</h3>

Execute um revisor por arquivo, depois entregue todas as descobertas a um agente que as classifica e deduplicar.

```text theme={null}
> use a workflow to review every file changed in this PR for correctness issues, then merge the per-file findings into one ranked summary
```

<h3 id="research-a-topic-across-many-sources">
  Pesquisar um tópico em muitas fontes
</h3>

Distribua leitores em changelogs, problemas e documentos, depois sintetize. O fluxo de trabalho `/deep-research` agrupado faz isso; você também pode descrever uma versão mais estreita.

```text theme={null}
> use a workflow to research how our three competitors handle rate limiting: read their public docs and recent changelog entries in parallel, then compare the approaches
```

<h3 id="find-issues-until-the-list-stops-growing">
  Encontrar problemas até a lista parar de crescer
</h3>

Continue pesquisando em rodadas e pare quando novas rodadas não encontrarem nada novo.

```text theme={null}
> use a workflow to find flaky tests in this repo: run the suite repeatedly, record which tests fail intermittently, and stop once two rounds in a row find nothing new
```

<h3 id="what-the-saved-script-looks-like">
  Como o script salvo se parece
</h3>

Quando você [salva um fluxo de trabalho](#save-the-workflow-for-reuse), o arquivo em `.claude/workflows/` contém um bloco `meta` seguido por um corpo de script que orquestra subagentos. Você geralmente não precisa editá-lo, mas aqui está a forma de um pequeno para que você possa reconhecer o que Claude gerou:

```javascript theme={null}
export const meta = {
  name: 'audit-routes',
  description: 'Audit every route handler for missing auth checks',
}

const found = await agent('List every .ts file under src/routes/.', {
  schema: { type: 'object', required: ['files'], properties: { files: { type: 'array', items: { type: 'string' } } } },
})

const audits = await pipeline(found.files, file =>
  agent(`Audit ${file} for missing authentication checks.`, { label: file }),
)

return audits.filter(Boolean)
```

O corpo é JavaScript simples com `await` de nível superior. `agent()` spawna um subagentos e `pipeline()` executa um por item em uma lista. Se você quiser editar um script manualmente, peça a Claude para orientá-lo através da alteração, ou consulte a entrada da ferramenta Workflow na [referência do Agent SDK](https://code.claude.com/docs/pt/agent-sdk/typescript) para o conjunto completo de opções.

<h2 id="how-a-workflow-runs">
  Como um fluxo de trabalho é executado
</h2>

O runtime do fluxo de trabalho executa o script em um ambiente isolado, separado de sua conversa. Os resultados intermediários permanecem em variáveis de script em vez de chegar ao contexto de Claude.

Cada execução escreve seu script em um arquivo sob o diretório da sua sessão em `~/.claude/projects/`. Claude recebe o caminho quando a execução começa, então você pode pedir por ele. Você pode abrir esse arquivo para ler a orquestração que Claude escreveu, compará-lo com o script de uma execução anterior, ou editá-lo e pedir a Claude para relançar a partir da versão editada.

O runtime rastreia o resultado de cada agente conforme a execução progride, o que é o que torna uma execução [retomável](#resume-after-a-pause) dentro da mesma sessão.

<h3 id="behavior-and-limits">
  Comportamento e limites
</h3>

O runtime aplica as seguintes restrições:

| Restrição                                                                      | Por quê                                                                                                                                                 |
| :----------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Sem entrada do usuário durante a execução                                      | Apenas prompts de permissão de agente podem pausar uma execução. Para aprovação entre estágios, execute cada estágio como seu próprio fluxo de trabalho |
| Sem acesso direto ao sistema de arquivos ou shell do próprio fluxo de trabalho | Agentes leem, escrevem e executam comandos. O script coordena os agentes                                                                                |
| Até 16 agentes simultâneos, menos em máquinas com núcleos de CPU limitados     | Limita o uso de recursos locais                                                                                                                         |
| 1.000 agentes totais por execução                                              | Previne loops descontrolados                                                                                                                            |

<h2 id="manage-runs">
  Gerenciar execuções
</h2>

Uma vez que uma execução começa, você a gerencia a partir da visualização `/workflows`, ou expandindo sua linha de progresso no painel de tarefas abaixo da caixa de entrada.

<h3 id="resume-after-a-pause">
  Retomar após uma pausa
</h3>

Se você parar uma execução, você pode retomá-la: agentes que já foram concluídos retornam seus resultados em cache, e o resto é executado ao vivo. Um agente que ainda estava em execução quando você parou não é salvo e começa novamente ao retomar, então um fluxo de trabalho que distribui trabalho entre muitos agentes pequenos preserva mais progresso do que um agente longo. Retome uma execução pausada de `/workflows` selecionando-a e pressionando `p`, ou peça a Claude para relançar o fluxo de trabalho com o mesmo script.

Retomar funciona dentro da mesma sessão de Claude Code. Se você sair de Claude Code enquanto um fluxo de trabalho está em execução, a próxima sessão inicia o fluxo de trabalho do zero.

<h3 id="cost">
  Custo
</h3>

Um fluxo de trabalho spawna muitos agentes, então uma única execução pode usar significativamente mais tokens do que trabalhar através da mesma tarefa em conversa. As execuções contam para o uso do seu plano e limites de taxa como qualquer outra sessão.

Para avaliar o gasto antes de se comprometer com uma tarefa grande, execute o fluxo de trabalho em um pequeno recorte primeiro: um diretório em vez de todo o repositório, ou uma pergunta estreita em vez de uma ampla. A visualização `/workflows` mostra o uso de tokens de cada agente conforme a execução progride, e você pode parar a execução lá a qualquer momento sem perder o trabalho concluído. Os [limites de agente](#behavior-and-limits) do runtime limitam quantos agentes uma única execução pode spawnar, o que limita o custo de um script descontrolado. Para manter cada execução menor por padrão, [defina uma diretriz de tamanho](#set-a-size-guideline) em `/config`.

Claude Code também sinaliza uma execução que cresce incomumente grande. Quando um fluxo de trabalho agenda mais de 25 agentes, ou sua projeção de token total passa 1,5 milhão, sua linha de progresso no painel de tarefas abaixo da caixa de entrada mostra um aviso `Large workflow`. O aviso aponta você para [`/workflows`](#watch-the-run), onde você pode parar a execução. Requer Claude Code v2.1.203 ou posterior.

O aviso é consultivo: ele não pausa ou limita a execução. Duas configurações mudam quando você o vê:

* Se você [defina uma diretriz de tamanho](#set-a-size-guideline), a contagem de agentes da diretriz substitui o limite de 25 agentes.
* Sessões com [ultracode](#let-claude-decide-with-ultracode) ativado não mostram o aviso, porque ativar ultracode já o opta para execuções grandes.

Cada agente em um fluxo de trabalho usa o modelo de sua sessão a menos que o script rotule um estágio para um diferente ou a variável de ambiente [`CLAUDE_CODE_SUBAGENT_MODEL`](https://code.claude.com/docs/pt/model-config#environment-variables) esteja definida, o que substitui ambos. Para controlar o custo do modelo:

* Verifique `/model` antes de uma execução grande se você geralmente muda para um modelo menor para trabalho de rotina
* Peça a Claude para usar um modelo menor para estágios que não precisam do mais forte quando você descreve a tarefa

<h3 id="set-a-size-guideline">
  Defina uma diretriz de tamanho
</h3>

A configuração Dynamic workflow size em `/config` mantém os fluxos de trabalho que Claude escreve em uma escala menor por padrão. Claude Code envia a configuração para Claude como conselho, então um prompt que chama por uma escala diferente ainda a substitui. Requer Claude Code v2.1.202 ou posterior.

Cada valor define a contagem de agentes que Claude visa nos scripts que escreve.

| Valor          | Orientação enviada para Claude |
| :------------- | :----------------------------- |
| `unrestricted` | Sem diretriz. Este é o padrão. |
| `small`        | Aim for fewer than 5 agents.   |
| `medium`       | Aim for fewer than 15 agents.  |
| `large`        | Aim for fewer than 50 agents.  |

As alterações entram em vigor no próximo prompt. Os [limites de agente do runtime](#behavior-and-limits) ainda se aplicam independentemente da configuração.

<h3 id="turn-workflows-off">
  Desativar fluxos de trabalho
</h3>

Fluxos de trabalho estão disponíveis na CLI, no aplicativo Desktop, nas extensões IDE, [modo não interativo](https://code.claude.com/docs/pt/headless) com `claude -p`, e no [Agent SDK](https://code.claude.com/docs/pt/agent-sdk/overview). As mesmas configurações de desativação se aplicam em cada superfície.

Para desativar fluxos de trabalho para você:

* Alterne Dynamic workflows desativado em `/config`. Persiste entre sessões.
* Defina `"disableWorkflows": true` em `~/.claude/settings.json`. Persiste entre sessões.
* Defina `CLAUDE_CODE_DISABLE_WORKFLOWS=1`. Lido na inicialização, então se aplica onde quer que você o defina.

Para desativar fluxos de trabalho para toda a sua organização, defina `"disableWorkflows": true` em [configurações gerenciadas](https://code.claude.com/docs/pt/server-managed-settings), ou use o alternador na página [configurações de administrador de Claude Code](https://claude.ai/admin-settings/claude-code).

Quando fluxos de trabalho estão desativados, os comandos de fluxo de trabalho agrupados não estão disponíveis, a palavra-chave `ultracode` não dispara mais uma execução, e `ultracode` é removido do menu `/effort`.

<h2 id="related-resources">
  Recursos relacionados
</h2>

* [Executar agentes em paralelo](https://code.claude.com/docs/pt/agents): comparar subagentos, visualização de agente, equipes de agentes e fluxos de trabalho
* [Criar subagentos personalizados](https://code.claude.com/docs/pt/sub-agents): a primitiva de worker que fluxos de trabalho orquestram
* [Gerenciar custos](https://code.claude.com/docs/pt/costs): como execuções multi-agente contam para limites de uso
