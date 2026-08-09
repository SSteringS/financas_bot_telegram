<!-- Fonte: https://code.claude.com/docs/pt/advisor | Baixado em: 2026-08-06 | slug: advisor -->

# Escale decisões difíceis com a ferramenta advisor

> Combine seu modelo principal com um modelo advisor mais forte que Claude consulta em momentos-chave durante uma tarefa.

<Note>
  A ferramenta advisor é experimental e requer a API Anthropic. Não está disponível no Amazon Bedrock, Claude Platform on AWS, na plataforma de agentes do Google Cloud ou no Microsoft Foundry. O comportamento, preços e disponibilidade podem mudar.
</Note>

A ferramenta advisor permite que Claude consulte um segundo modelo, tipicamente mais forte, em momentos-chave durante uma tarefa, como antes de se comprometer com uma abordagem, quando preso em um erro recorrente, ou antes de declarar uma tarefa concluída. O advisor recebe a conversa completa, incluindo cada chamada de ferramenta e resultado, e retorna orientação que Claude aplica antes de continuar.

O advisor é executado no servidor na infraestrutura da Anthropic como uma [server tool](https://platform.claude.com/docs/en/agents-and-tools/tool-use/advisor-tool), disponível para contas de assinatura e faturadas por API. Você escolhe qual modelo atua como advisor, e Claude decide quando chamá-lo.

Esta página cobre como ativar o advisor, quais emparelhamentos de modelos são aceitos, o que Claude mostra durante uma consulta, e como o uso do advisor é faturado.

<h2 id="when-to-use-the-advisor">
  Quando usar o advisor
</h2>

O advisor é adequado para tarefas longas e com múltiplas etapas onde a maioria dos turnos é rotineira, mas a qualidade do plano determina o resultado. Exemplos incluem grandes refatorações, sessões de depuração onde um erro continua recorrendo, e tarefas que você deseja verificadas independentemente antes de Claude declarar que estão concluídas.

Adiciona menos valor em tarefas curtas onde há pouco a planejar, ou em trabalho onde cada turno precisa do modelo mais forte. Para esses casos, [mude o modelo principal](https://code.claude.com/docs/pt/model-config#setting-your-model) em vez disso, ou veja [como o advisor se compara com opusplan e subagents](#compare-with-related-features) para outras formas de obter uma segunda opinião.

<h2 id="enable-the-advisor">
  Ativar o advisor
</h2>

Você pode definir o modelo advisor de três formas:

* **Comando `/advisor`**: defina ou altere o advisor no meio da sessão e salve-o como seu padrão
* **Configuração `advisorModel`**: configure um padrão persistente em seu [arquivo de configurações](https://code.claude.com/docs/pt/settings)
* **Flag `--advisor`**: defina o advisor para uma única sessão no lançamento

Se qualquer uma dessas opções definir um modelo advisor, o advisor será ativado para sessões cujo modelo principal [o suporta](#choose-an-advisor-model). Para parar de usá-lo, veja [Desativar o advisor](#turn-the-advisor-off).

<Note>
  Para usar Fable 5 como o advisor, você precisa de Claude Code v2.1.170 ou posterior e [acesso a Fable 5](https://code.claude.com/docs/pt/model-config#work-with-fable-5) para sua organização.
</Note>

<h3 id="use-the-/advisor-command">
  Use o comando `/advisor`
</h3>

Execute `/advisor` sem argumentos para abrir um seletor listando os modelos advisor disponíveis, ou passe o modelo diretamente:

```
/advisor opus
```

Sua seleção é salva em `advisorModel` nas configurações do usuário e persiste entre sessões. Se a allowlist [`availableModels`](https://code.claude.com/docs/pt/model-config#restrict-model-selection) da sua organização excluir o modelo advisor salvo, o advisor não será invocado até que você escolha um modelo permitido com `/advisor`. Se seu modelo principal atual não suportar o advisor, a seleção ainda é salva e ativada quando você muda para um [modelo principal compatível](#choose-an-advisor-model) com [`/model`](https://code.claude.com/docs/pt/model-config#setting-your-model).

<h3 id="set-advisormodel-in-settings">
  Defina `advisorModel` nas configurações
</h3>

Para configurar o advisor como padrão sem abrir uma sessão, defina-o em seu arquivo de configurações:

```json theme={null}
{
  "advisorModel": "opus"
}
```

<h3 id="use-the-advisor-flag">
  Use a flag `--advisor`
</h3>

Para definir o advisor para uma única sessão sem alterar sua configuração salva, inicie com a flag:

```bash theme={null}
claude --advisor opus
```

A flag tem precedência sobre a configuração `advisorModel` para essa sessão. Ela sai com um erro se o modelo principal da sessão não suportar o advisor, ou se o modelo advisor solicitado for excluído pela allowlist [`availableModels`](https://code.claude.com/docs/pt/model-config#restrict-model-selection) da sua organização.

<h2 id="choose-an-advisor-model">
  Escolha um modelo advisor
</h2>

O advisor deve ser pelo menos tão capaz quanto o modelo principal. Os advisors aceitos para cada modelo principal são:

| Modelo principal      | Advisors aceitos          | Notas                                                                                                                                                                                  |
| --------------------- | ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Haiku 4.5             | Fable, Opus, Sonnet       | Haiku pode chamar o advisor, mas não pode atuar como um                                                                                                                                |
| Sonnet 4.6            | Fable, Opus, Sonnet       |                                                                                                                                                                                        |
| Sonnet 5              | Fable, Opus, Sonnet 5     | Um advisor Sonnet 4.6 é rejeitado                                                                                                                                                      |
| Opus 4.6              | Fable, Opus, Sonnet 5     | Sonnet 5 e Opus 4.6 são classificados como igualmente capazes, então um Opus 4.6 principal aceita um advisor Sonnet 5                                                                  |
| Opus 4.7 ou posterior | Fable, Opus 4.7, Opus 4.8 | Opus 4.7 e Opus 4.8 são classificados como igualmente capazes, então qualquer um aceita o outro como um advisor. Um Opus 4.7 principal com um advisor Opus 4.6 ou Sonnet 5 é rejeitado |
| Fable 5 (v2.1.170+)   | Fable                     | Um advisor Opus ou Sonnet é rejeitado                                                                                                                                                  |

Fable 5 requer Claude Code v2.1.170 ou posterior e acesso a Fable 5, seja atuando como modelo principal ou advisor.

Defina o advisor como `opus`, `sonnet`, ou `fable`. Esses aliases resolvem para a versão mais recente de cada modelo. Você também pode passar um ID de modelo completo como `claude-opus-4-8`.

Subagentes herdam o advisor configurado e aplicam a mesma verificação de emparelhamento contra seu próprio modelo.

Claude Code valida o emparelhamento antes de enviar uma solicitação:

* Se o advisor for menos capaz que o modelo principal, o advisor não será anexado às solicitações do modelo principal. A saída do comando `/advisor` e uma notificação mostram isso. Subagentes cujo próprio modelo satisfaz o emparelhamento ainda podem usar o advisor.
* Se o modelo principal ou o advisor for um modelo que Claude Code não reconhece, o advisor não será anexado.

<h3 id="common-model-pairings">
  Emparelhamentos de modelos comuns
</h3>

Qualquer emparelhamento aceito funciona. Essas combinações equilibram custo contra capacidade de diferentes formas:

| Emparelhamento                    | Quando usar                                                                                                                                                                             |
| --------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Sonnet principal + advisor Opus   | Sonnet lida com trabalho rotineiro e escala planejamento, falhas ambíguas e verificações de conclusão para Opus                                                                         |
| Sonnet principal + advisor Fable  | Orientação Fable 5 em pontos de decisão sem executar Fable 5 em toda parte. Requer v2.1.170 ou posterior e acesso a Fable 5                                                             |
| Haiku principal + advisor Opus    | Modelo principal de menor custo com planejamento forte. Espere custo mais alto que Haiku sozinho, mas menor que mudar o modelo principal para Sonnet ou Opus                            |
| Opus principal + advisor Opus     | Um segundo Opus revisa o primeiro. Útil para tarefas de alto risco onde uma verificação independente importa mais que o custo                                                           |
| Fable principal + advisor Fable   | Emparelhamento de maior capacidade quando Fable 5 está disponível (v2.1.170+). Fable é um nível superior a Opus e Sonnet, então é o único advisor aceito para um modelo principal Fable |
| Sonnet principal + advisor Sonnet | Uma segunda opinião de menor custo para capturar oversights rotineiros                                                                                                                  |

<h2 id="when-claude-consults-the-advisor">
  Quando Claude consulta o advisor
</h2>

Claude decide quando chamar o advisor. Tende a consultar antes de se comprometer com uma abordagem, quando um erro continua recorrendo, e antes de declarar uma tarefa concluída, mas o tempo é orientado pelo modelo em vez de baseado em regras.

Você pode pedir uma consulta em seu prompt da mesma forma que solicitaria qualquer ferramenta, por exemplo `consult the advisor before you continue`. Não há configuração para limitar ou forçar chamadas do advisor; se você quiser que Claude consulte mais ou menos frequentemente durante uma tarefa, diga isso em suas instruções.

<h2 id="what-you-see-during-a-session">
  O que você vê durante uma sessão
</h2>

Quando Claude chama o advisor, a transcrição mostra uma linha `Advising` com o nome do modelo advisor enquanto a chamada está em andamento. Quando o resultado retorna, a linha confirma que o advisor revisou a conversa. Pressione `Ctrl+O` para expandi-la e ler a orientação completa do advisor.

Claude geralmente segue a orientação do advisor, mas se adapta quando sua própria evidência contradiz uma afirmação específica: se uma etapa recomendada falha quando tentada, ou o conteúdo do arquivo contradiz o conselho, Claude expõe o conflito em vez de seguir a orientação incondicionalmente.

O advisor sempre recebe a conversa completa, e Claude controla o tempo. Para mais controle ou uma configuração diferente, veja [como o advisor se compara com subagents e opusplan](#compare-with-related-features).

<h2 id="cost">
  Custo
</h2>

Cada chamada do advisor envia a conversa para o modelo advisor, então consome tokens nas taxas do modelo advisor além do uso do seu modelo principal. Com faturamento por API, tokens do advisor são cobrados nas taxas de entrada e saída do modelo advisor. Em planos de assinatura, o uso do advisor conta para os limites de uso do seu plano.

Claude chama o advisor em pontos de decisão em vez de em cada turno, então emparelhar um modelo principal mais rápido com um advisor mais forte tipicamente custa menos que executar o modelo mais forte em toda parte. O uso do advisor conta para os totais da sessão mostrados por [`/usage`](https://code.claude.com/docs/pt/costs#track-your-costs).

Para como tokens do advisor são reportados em respostas da API, veja [Usage and billing](https://platform.claude.com/docs/en/agents-and-tools/tool-use/advisor-tool#usage-and-billing) na documentação da API Claude.

<h2 id="impact-on-prompt-caching">
  Impacto no prompt caching
</h2>

Ativar ou desativar o advisor no meio da sessão não invalida o [prompt cache](https://code.claude.com/docs/pt/prompt-caching) do seu modelo principal. Diferentemente de [mudar modelo ou nível de esforço](https://code.claude.com/docs/pt/prompt-caching#actions-that-invalidate-the-cache), alternar `/advisor` mantém o prefixo em cache intacto, e a orientação retornada pelo advisor é armazenada em cache como parte da transcrição em turnos posteriores.

A própria leitura do advisor da conversa não é armazenada em cache. Cada chamada do advisor processa a transcrição completa novamente, sem reutilização entre chamadas.

<h2 id="requirements">
  Requisitos
</h2>

A ferramenta advisor requer todos os seguintes:

* **Apenas API Anthropic**: o advisor é uma ferramenta executada no servidor. Não está disponível no Amazon Bedrock, Claude Platform on AWS, Google Cloud's Agent Platform ou Microsoft Foundry. Através de um [LLM gateway](https://code.claude.com/docs/pt/llm-gateway) configurado com `ANTHROPIC_BASE_URL`, a disponibilidade depende se o gateway encaminha a solicitação intacta para a API Anthropic.
* **Modelo principal suportado**: Opus 4.6 ou posterior, Sonnet 4.6 ou posterior, ou Haiku 4.5. Fable 5 também se qualifica no Claude Code v2.1.170 ou posterior.

<h2 id="turn-the-advisor-off">
  Desativar o advisor
</h2>

Para parar de usar o advisor e limpar seu `advisorModel` salvo, execute `/advisor off` ou escolha **No advisor** no seletor `/advisor`:

```
/advisor off
```

Para desativar a ferramenta advisor inteiramente, defina `CLAUDE_CODE_DISABLE_ADVISOR_TOOL=1`. O comando `/advisor` fica indisponível e qualquer `advisorModel` configurado é ignorado. A flag `--advisor` é aceita mas não tem efeito; scripts existentes que a passam continuam funcionando sem erros. Veja [Environment variables](https://code.claude.com/docs/pt/env-vars).

<h2 id="compare-with-related-features">
  Compare com recursos relacionados
</h2>

O advisor é uma das várias formas de combinar forças de modelos. Escolha com base em quando você quer um segundo modelo envolvido.

| Abordagem                                                       | Quando o modelo mais forte é executado                                                                                                       | Como começa                                 |
| --------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------- |
| Ferramenta Advisor                                              | Em pontos de decisão no meio da tarefa                                                                                                       | Claude a chama quando precisa de orientação |
| [`opusplan`](https://code.claude.com/docs/pt/model-config#opusplan-model-setting)           | Durante plan mode quando [permitido por `availableModels`](https://code.claude.com/docs/pt/model-config#restrict-model-selection), depois muda para Sonnet para execução | Você entra em plan mode                     |
| [Subagents](https://code.claude.com/docs/pt/sub-agents#choose-a-model) com `model` definido | Para toda a subtarefa delegada                                                                                                               | Claude delega, ou você invoca o subagent    |
| [`/model`](https://code.claude.com/docs/pt/model-config#setting-your-model)                 | Para todos os turnos subsequentes                                                                                                            | Você muda de modelos                        |

<h2 id="see-also">
  Veja também
</h2>

* [Model configuration](https://code.claude.com/docs/pt/model-config): mude modelos, defina níveis de esforço, e use `opusplan`
* [Manage costs effectively](https://code.claude.com/docs/pt/costs): rastreie o uso de tokens entre modelos
* [Advisor tool in the Claude API](https://platform.claude.com/docs/en/agents-and-tools/tool-use/advisor-tool): entenda a ferramenta de servidor subjacente, ou use-a diretamente da Messages API
* [The advisor strategy](https://claude.com/blog/the-advisor-strategy): por que emparelhar um modelo principal rápido com um advisor mais forte funciona
