<!-- Fonte: https://code.claude.com/docs/pt/model-config | Baixado em: 2026-08-06 | slug: model-config -->

# Configuração de modelo

> Saiba mais sobre a configuração do modelo Claude Code, incluindo aliases de modelo como `opusplan`

<h2 id="available-models">
  Modelos disponíveis
</h2>

Para a configuração `model` no Claude Code, você pode configurar:

* Um **alias de modelo**
* Um **nome de modelo**
  * API Anthropic: Um **[nome de modelo](https://platform.claude.com/docs/pt/about-claude/models/overview)** completo
  * Amazon Bedrock: um ARN de perfil de inferência
  * Microsoft Foundry: um nome de implantação
  * Google Cloud's Agent Platform: um nome de versão

Para orientação sobre qual modelo e nível de esforço se adequam a diferentes tipos de trabalho, consulte [Escolhendo um modelo Claude e nível de esforço no Claude Code](https://claude.com/blog/claude-model-and-effort-level-in-claude-code) no blog.

<Note>
  `ANTHROPIC_BASE_URL` altera para onde as solicitações são enviadas, não qual modelo as responde. Para rotear Claude através de um gateway LLM, consulte [gateways LLM](https://code.claude.com/docs/pt/llm-gateway).
</Note>

<h3 id="model-aliases">
  Aliases de modelo
</h3>

Os aliases de modelo fornecem uma maneira conveniente de selecionar configurações de modelo sem precisar lembrar dos números exatos da versão:

| Alias de modelo  | Comportamento                                                                                                                                                                                                                                                                                                                                            |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`default`**    | Valor especial que limpa qualquer substituição de modelo e reverte para o modelo recomendado para seu tipo de conta, ou para o [modelo padrão da organização](#organization-default-model) quando um administrador definiu um. Não é em si um alias de modelo                                                                                            |
| **`best`**       | Usa Fable 5 onde sua organização tem acesso a ele, caso contrário o modelo Opus mais recente                                                                                                                                                                                                                                                             |
| **`fable`**      | Usa Claude Fable 5 para suas tarefas mais difíceis e de longa duração                                                                                                                                                                                                                                                                                    |
| **`sonnet`**     | Usa o modelo Sonnet mais recente para tarefas de codificação diária                                                                                                                                                                                                                                                                                      |
| **`opus`**       | Usa o modelo Opus mais recente para tarefas de raciocínio complexo                                                                                                                                                                                                                                                                                       |
| **`haiku`**      | Usa o modelo Haiku rápido e eficiente para tarefas simples                                                                                                                                                                                                                                                                                               |
| **`sonnet[1m]`** | Usa Sonnet com uma [janela de contexto de 1 milhão de tokens](https://platform.claude.com/docs/pt/build-with-claude/context-windows#context-window-sizes-by-model) para sessões longas. Sem efeito quando `sonnet` já se resolve para Sonnet 5 com sua janela 1M nativa; atrás de um [gateway LLM](https://code.claude.com/docs/pt/llm-gateway), seleciona a janela 1M para Sonnet 5 |
| **`opus[1m]`**   | Usa Opus com uma [janela de contexto de 1 milhão de tokens](https://platform.claude.com/docs/pt/build-with-claude/context-windows#context-window-sizes-by-model) para sessões longas                                                                                                                                                                     |
| **`opusplan`**   | Modo especial que usa `opus` durante Plan Mode, depois muda para `sonnet` para execução                                                                                                                                                                                                                                                                  |

A versão para a qual os aliases `opus` e `sonnet` se resolvem depende do provedor:

| Provedor                                             | `opus`   | `sonnet`   |
| :--------------------------------------------------- | :------- | :--------- |
| API Anthropic                                        | Opus 4.8 | Sonnet 5   |
| [Claude Platform on AWS](https://code.claude.com/docs/pt/claude-platform-on-aws) | Opus 4.8 | Sonnet 4.6 |
| Amazon Bedrock, Google Cloud's Agent Platform        | Opus 4.8 | Sonnet 4.5 |
| Microsoft Foundry                                    | Opus 4.6 | Sonnet 4.5 |

Onde um alias se resolve para um modelo mais antigo, modelos mais recentes estão disponíveis selecionando o nome completo do modelo explicitamente ou definindo `ANTHROPIC_DEFAULT_OPUS_MODEL` ou `ANTHROPIC_DEFAULT_SONNET_MODEL`.

Antes da v2.1.207, `opus` se resolvia para Opus 4.7 no Claude Platform on AWS e para Opus 4.6 no Amazon Bedrock e Google Cloud's Agent Platform.

Os aliases apontam para a versão recomendada para seu provedor e são atualizados ao longo do tempo. Para fixar uma versão específica, use o nome completo do modelo, por exemplo `claude-opus-4-8`, ou defina a variável de ambiente correspondente como `ANTHROPIC_DEFAULT_OPUS_MODEL`.

<Note>
  Sonnet 5 requer Claude Code v2.1.197 ou posterior. Opus 4.8 requer v2.1.154 ou posterior. Execute `claude update` para atualizar.
</Note>

<h3 id="work-with-fable-5">
  Trabalhar com Fable 5
</h3>

[Claude Fable 5](https://platform.claude.com/docs/pt/about-claude/models/introducing-claude-fable-5-and-claude-mythos-5) é o modelo mais capaz no Claude Code, adequado para tarefas maiores que uma única sessão. Ele sustenta sessões autônomas longas, investiga antes de agir e verifica seu trabalho com mais frequência do que modelos menores.

Fable 5 não é o modelo padrão. Selecione-o com `/model fable`. Solicitações que seus classificadores de segurança sinalizam, mais frequentemente em domínios de cibersegurança e biologia, acionam [fallback automático de modelo](#automatic-model-fallback).

Para aproveitar ao máximo o Fable 5:

* **Descreva o resultado, não as etapas**: entregue-lhe o resultado que você deseja e deixe-o planejar o caminho. Para mantê-lo funcionando até que esse resultado se mantenha, [defina uma meta](https://code.claude.com/docs/pt/goal).
* **Entregue-lhe problemas ambíguos**: investigações de causa raiz, depuração de interrupções e decisões de arquitetura são onde a investigação e verificação extras compensam.
* **Pule os lembretes de verificação**: ele verifica seu próprio trabalho com menos solicitação, portanto lembretes para testar ou verificar geralmente são desnecessários.
* **Dimensione tarefas maiores**: dê-lhe trabalho que você normalmente dividiria em pedaços. Ele mantém sessões longas sem perder o fio.

<Note>
  Fable 5 requer Claude Code v2.1.170 ou posterior. Versões mais antigas não mostram Fable 5 no seletor de modelo e não podem selecioná-lo. Execute `claude update` para atualizar. Fable 5 não está disponível sob [retenção zero de dados](https://code.claude.com/docs/pt/zero-data-retention), onde o seletor `/model` o omite ou o mostra desabilitado.
</Note>

<h3 id="setting-your-model">
  Configurando seu modelo
</h3>

Você pode configurar seu modelo de várias maneiras, listadas em ordem de prioridade:

1. **Durante a sessão**: use `/model <alias|name>` para alternar imediatamente, ou execute `/model` sem argumentos para abrir o seletor. O seletor pede confirmação quando a conversa tem saída anterior, pois a próxima resposta relê o histórico completo sem contexto em cache
2. **Na inicialização**: inicie com `claude --model <alias|name>`
3. **Variável de ambiente**: defina `ANTHROPIC_MODEL=<alias|name>`
4. **Configurações**: configure permanentemente em seu arquivo de configurações usando o campo `model`

A partir da v2.1.153, `/model` salva sua escolha como padrão para novas sessões escrevendo o campo `model` em suas configurações de usuário. No seletor:

* `Enter`: alternar modelo e salvar como seu padrão
* `s`: alternar modelo apenas para esta sessão

Digitar `/model <name>` diretamente se comporta como `Enter`. Um modelo definido com `/model` em [modo não interativo](https://code.claude.com/docs/pt/headless), com o sinalizador `-p`, se aplica apenas à sessão atual e não é salvo como seu padrão. As configurações de projeto e gerenciadas ainda têm precedência e são reaplicadas no próximo lançamento. Um [modelo padrão da organização](#organization-default-model) que seu administrador configurou para substituir a seleção do usuário também é reaplicado no próximo lançamento.

Na v2.1.144 até v2.1.152, `/model` se aplicava apenas à sessão atual e `d` no seletor salvava um padrão.

O sinalizador `--model` e a variável de ambiente `ANTHROPIC_MODEL` se aplicam apenas à sessão que você inicia com eles. Para executar modelos diferentes em terminais diferentes ao mesmo tempo, inicie cada um com seu próprio sinalizador `--model` em vez de alternar com `/model`.

Os preços no seletor `/model` aparecem quando Claude Code fala com a API Anthropic, diretamente ou através de um [gateway LLM](https://code.claude.com/docs/pt/llm-gateway) que a proxeia, e o preço em uma linha é o preço do modelo que essa linha seleciona. Em [provedores de terceiros](https://code.claude.com/docs/pt/third-party-integrations) como Amazon Bedrock e no [gateway de aplicativos Claude](https://code.claude.com/docs/pt/claude-apps-gateway), seu provedor ou gateway determina o que você paga, portanto as linhas do seletor não mostram preço. O preço é apenas um rótulo de exibição; não afeta qual modelo uma linha seleciona ou o que seu provedor cobra. Antes da v2.1.206, [Claude Platform on AWS](https://code.claude.com/docs/pt/claude-platform-on-aws) e sessões de gateway mostravam preços de lista da Anthropic, e uma linha poderia mostrar o preço de um modelo diferente daquele que selecionava.

As sessões retomadas iniciadas com `claude --resume`, `--continue` ou o seletor `/resume` mantêm o modelo que estavam usando quando a transcrição foi salva, independentemente da configuração `model` atual. Se esse modelo foi descontinuado ou é excluído por [`availableModels`](#restrict-model-selection), a sessão cai para a ordem de precedência normal. Isso evita que a escolha `/model` de outra sessão altere o modelo ao retomar.

Um modelo que você escolhe para o novo lançamento com `--model` ou `ANTHROPIC_MODEL` ainda tem precedência sobre o modelo restaurado. A partir da v2.1.195, também tem uma variável da família [`ANTHROPIC_DEFAULT_OPUS_MODEL`](#environment-variables).

Quando o modelo ativo na inicialização vem das configurações do projeto ou gerenciadas em vez de sua própria seleção, o cabeçalho de inicialização mostra qual arquivo de configurações o definiu. Execute `/model` para substituir; a configuração do projeto ou gerenciada reaplicada no próximo lançamento.

Quando uma mudança de modelo é solicitada através do método `setModel()` do [Agent SDK](https://code.claude.com/docs/pt/agent-sdk/overview) ou por um aplicativo como o [Desktop app](https://code.claude.com/docs/pt/desktop) que executa o Claude Code CLI para você, Claude Code verifica se a string é uma que ele reconhece antes de salvá-la. Esta verificação requer Claude Code v2.1.200 ou posterior. Na API Anthropic, Claude Code reconhece:

* um alias de modelo
* uma entrada do seletor `/model`
* qualquer nome que comece com `claude-`
* um valor que você configurou como uma [opção de modelo personalizado](#add-a-custom-model-option) ou em [`modelOverrides`](#override-model-ids-per-version)

Claude Code rejeita uma string não reconhecida com `Model "<name>" is not a recognized model id.` e a sessão mantém seu modelo atual, em vez de salvar a string e falhar na próxima solicitação. Consulte [a referência de erro](https://code.claude.com/docs/pt/errors#model-is-not-a-recognized-model-id) para etapas de recuperação.

A verificação é executada apenas na API Anthropic. No Amazon Bedrock, Google Cloud's Agent Platform, Microsoft Foundry, [Claude Platform on AWS](https://code.claude.com/docs/pt/claude-platform-on-aws) e atrás de um [gateway LLM](https://code.claude.com/docs/pt/llm-gateway) ou um `ANTHROPIC_BASE_URL` personalizado, seu provedor ou gateway define os nomes dos modelos, portanto Claude Code passa qualquer string sem verificá-la. A verificação também não cobre o sinalizador `--model`, a variável de ambiente `ANTHROPIC_MODEL` ou a configuração `model`; um valor digitado incorretamente lá produz [There's an issue with the selected model](https://code.claude.com/docs/pt/errors#theres-an-issue-with-the-selected-model) na primeira solicitação em vez disso.

Quando o modelo solicitado tem uma data de aposentadoria programada ou é automaticamente remapeado para uma versão mais recente, Claude Code mostra um aviso que nomeia o modelo solicitado. As sessões interativas o mostram como um aviso de inicialização. A partir da v2.1.182, o mesmo aviso é escrito em stderr no [modo não interativo](https://code.claude.com/docs/pt/headless) ao usar o formato de saída de texto padrão. A verificação também cobre um `model` definido no [frontmatter de subagentos](https://code.claude.com/docs/pt/sub-agents). O aviso stderr é suprimido para `--output-format json` e `stream-json`; leia o modelo real do campo `modelUsage` da [mensagem de resultado](https://code.claude.com/docs/pt/headless#get-structured-output) em vez disso.

Exemplo de uso:

```bash theme={null}
# Iniciar com Opus
claude --model opus

# Alternar para Sonnet durante a sessão
/model sonnet
```

Exemplo de arquivo de configurações:

```json theme={null}
{
    "permissions": {
        ...
    },
    "model": "opus"
}
```

<h2 id="restrict-model-selection">
  Restringir seleção de modelo
</h2>

Os administradores corporativos podem usar `availableModels` em [configurações gerenciadas ou de política](https://code.claude.com/docs/pt/settings#settings-files) para restringir quais modelos os usuários podem selecionar. As entradas correspondem a uma família de modelo como `sonnet`, um prefixo de versão como `claude-sonnet-4-5`, ou um ID de modelo completo como `claude-sonnet-4-5-20250929`.

Quando `availableModels` é definido, a lista de permissões se aplica em todos os lugares onde um usuário pode especificar um modelo:

* **Modelo de sessão principal**: `/model`, o sinalizador `--model`, a variável de ambiente `ANTHROPIC_MODEL`, a configuração `model` e o modelo restaurado ao [retomar uma sessão](#setting-your-model)
* **Resolução de alias**: as variáveis de ambiente `ANTHROPIC_DEFAULT_OPUS_MODEL`, `ANTHROPIC_DEFAULT_SONNET_MODEL`, `ANTHROPIC_DEFAULT_HAIKU_MODEL` e `ANTHROPIC_DEFAULT_FABLE_MODEL` não podem redirecionar um alias permitido para um modelo fora da lista
* **Modo rápido**: `/fast` recusa alternar quando isso implicaria mudar implicitamente para um modelo Opus fora da lista, com a mensagem "não está nos modelos permitidos da sua organização"
* **Modelos de subagente**: o campo `model` em [subagente](https://code.claude.com/docs/pt/sub-agents#choose-a-model) frontmatter, o parâmetro `model` da ferramenta Agent, `CLAUDE_CODE_SUBAGENT_MODEL` e, na v2.1.197 e anterior, o seletor de modelo no assistente `/agents`&#x20;
* **Modelo de skill e comando**: o frontmatter `model` em [skills e comandos](https://code.claude.com/docs/pt/skills)
* **Modelo de advisor**: a configuração [`advisorModel`](https://code.claude.com/docs/pt/advisor) configurada e o sinalizador `--advisor`
* **Modelo de agente de fundo**: o modelo selecionado no [seletor de dispatch](https://code.claude.com/docs/pt/agent-view)

Na API Anthropic e [Claude Platform on AWS](https://code.claude.com/docs/pt/claude-platform-on-aws), um alias de família de modelo, `opus`, `sonnet`, `haiku` ou `fable`, se resolve para a versão mais recente de sua família que a lista de permissões permite. Quando a lista de permissões fixa versões específicas, por exemplo `["sonnet", "claude-opus-4-6"]`, tanto `/model opus` quanto `--model opus` selecionam Claude Opus 4.6, o Opus mais recente permitido, e mostram um aviso nomeando tanto os modelos solicitados quanto os substituídos. Antes da v2.1.205, um alias cuja versão mais recente lançada estava fora da lista era rejeitado ou substituído como qualquer outra seleção bloqueada, mesmo quando a lista permitia uma versão mais antiga.

Amazon Bedrock, Google Cloud's Agent Platform, Microsoft Foundry e [Mantle](https://code.claude.com/docs/pt/amazon-bedrock#use-the-mantle-endpoint) usam IDs de implantação específicos do provedor em vez de IDs de modelo Anthropic, portanto um alias bloqueado lá segue o comportamento de rejeição e substituição abaixo.

Claude Code trata qualquer outra seleção bloqueada de acordo com onde o modelo foi definido:

* **`/model`**: a mudança é rejeitada com um erro
* **sinalizador `--model`, `ANTHROPIC_MODEL` ou a configuração `model`**: o valor é substituído na inicialização com um aviso nomeando tanto o modelo solicitado quanto o substituído, e a sessão é iniciada no modelo padrão
* **Substituição de subagente, skill ou comando**: a substituição volta para o modelo herdado ou padrão em vez de falhar na solicitação
* **configuração `advisorModel`**: o advisor é desabilitado para a sessão
* **sinalizador `--advisor`**: Claude Code sai com um erro no lançamento

Os modelos excluídos são ocultados do seletor `/model`. Um ID de modelo completo na lista que não tem uma linha de seletor integrada, como uma versão mais antiga que a lista fixa, aparece no seletor `/model` como sua própria linha rotulada. Antes da v2.1.199, tal ID era selecionável apenas digitando `/model <id>`.

As mudanças de modelo que Claude Code faz em seu nome são verificadas da mesma forma:

* **[Cadeias de modelo de fallback](#fallback-model-chains)**: elementos fora da lista de permissões são descartados
* **Atualizações de modo de plano**: na API Anthropic e Claude Platform on AWS, uma atualização como [`opusplan`](#opusplan-model-setting) para um modelo excluído usa a versão mais recente permitida da família de atualização. Em provedores com IDs de modelo específicos do provedor, e quando nenhuma versão é permitida, a atualização é ignorada e o planejamento continua no modelo da sessão
* **[Fallback automático de modelo](#automatic-model-fallback)**: um fallback cujo alvo é excluído não é executado, portanto a solicitação sinalizada termina com uma recusa
* **[Modo rápido](https://code.claude.com/docs/pt/fast-mode)**: habilitar modo rápido é recusado quando o modelo em que a sessão seria executada depois está fora da lista de permissões

```json theme={null}
{
  "availableModels": ["sonnet", "haiku"]
}
```

<h3 id="surface-coverage">
  Cobertura de superfície
</h3>

Cada superfície impõe a lista de permissões que recebe. Qual mecanismo de entrega alcança cada superfície difere:

| Mecanismo de entrega                                                                               | CLI e IDE | Sessões locais do Desktop | Sessões web, mobile e cloud | Agent SDK e não-interativo | Cowork                  |
| :------------------------------------------------------------------------------------------------- | :-------- | :------------------------ | :-------------------------- | :------------------------- | :---------------------- |
| [Configurações gerenciadas pelo servidor](https://code.claude.com/docs/pt/server-managed-settings) do console de administração | Imposto   | Imposto                   | Imposto                     | Imposto                    | Não entregue            |
| [Arquivos de configurações gerenciadas ou MDM](https://code.claude.com/docs/pt/settings#settings-files)                        | Imposto   | Imposto                   | Não entregue                | Imposto                    | Imposto onde implantado |

* Sessões em nuvem, em [Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web) ou no aplicativo Desktop, são executadas em VMs gerenciadas pela Anthropic: as configurações implantadas no seu dispositivo não as alcançam, portanto entregue a lista de permissões através de configurações gerenciadas pelo servidor. Uma mudança de modelo no meio da sessão em uma sessão em nuvem é rejeitada quando o modelo solicitado é excluído pela lista de permissões. A rejeição do lado do servidor na criação da sessão se aplica a [restrições de modelo da organização](#organization-model-restrictions), não à chave de configurações `availableModels`.
* Cowork, a aba de trabalho agentic no aplicativo Claude Desktop, não é uma superfície Claude Code e não recebe configurações gerenciadas pelo servidor por design. Um arquivo de configurações gerenciadas se aplica a sessões Cowork quando está presente onde a sessão é executada; sessões Cowork remotas são executadas em VMs gerenciadas pela Anthropic, onde um arquivo implantado no dispositivo não está presente.
* Sessões em [provedores de terceiros](https://code.claude.com/docs/pt/server-managed-settings#platform-availability) como Amazon Bedrock, Google Cloud's Agent Platform, Microsoft Foundry e [Claude Platform on AWS](https://code.claude.com/docs/pt/claude-platform-on-aws) não recebem configurações gerenciadas pelo servidor, portanto entregue a lista de permissões através de arquivos de configurações gerenciadas ou MDM lá.
* A entrega gerenciada pelo servidor também requer que a sessão se autentique com um login de organização ou uma chave de API configurada diretamente. Frotas que geram chaves apenas através de um script [`apiKeyHelper`](https://code.claude.com/docs/pt/settings#available-settings) devem entregar a lista de permissões através de arquivos de configurações gerenciadas ou MDM.
* A aba Desktop Code também hospeda [sessões SSH](https://code.claude.com/docs/pt/desktop#ssh-sessions), que leem o arquivo de configurações gerenciadas do host remoto em que são executadas. Veja [Configurações gerenciadas do Desktop](https://code.claude.com/docs/pt/desktop#managed-settings).
* Os seletores de modelo em claude.ai e no aplicativo Desktop ocultam ou desabilitam modelos excluídos pela lista de permissões da sua organização. O estado do seletor é uma conveniência para os usuários; a imposição acontece na sessão.

<h3 id="default-model-behavior">
  Comportamento do modelo padrão
</h3>

A opção Padrão no seletor de modelo não é afetada por `availableModels` a menos que [`enforceAvailableModels`](#enforce-the-allowlist-for-the-default-model) também esteja definido. Por si só, `availableModels` deixa Padrão disponível, resolvendo para o padrão de tempo de execução do sistema [baseado no nível de assinatura do usuário](#default-model-setting). Se o padrão do nível é um modelo que você pretende restringir, defina `enforceAvailableModels` também.

Um array `availableModels` vazio nunca ativa a imposição do modelo Padrão: com `availableModels: []`, as seleções de modelo nomeadas são bloqueadas, mas o modelo Padrão para o tipo de conta permanece utilizável independentemente de `enforceAvailableModels`.

<h3 id="enforce-the-allowlist-for-the-default-model">
  Impor a lista de permissões para o modelo Padrão
</h3>

Defina `enforceAvailableModels: true` junto com um `availableModels` não vazio em configurações gerenciadas para estender a lista de permissões à opção Padrão. Isso requer Claude Code v2.1.175 ou posterior.

```json theme={null}
{
  "availableModels": ["sonnet", "haiku"],
  "enforceAvailableModels": true
}
```

A opção Padrão resolve para o padrão do tipo de conta, ou para o [modelo padrão da organização](#organization-default-model) quando um administrador definiu um. Quando esse modelo não está na lista de permissões, a opção Padrão se resolve para a primeira entrada `availableModels` que nomeia um modelo permitido e disponível, e a linha Padrão do seletor `/model` mostra esse modelo. Isso se aplica em todos os lugares onde o padrão é alcançado: inicialização da sessão, seleção de Padrão em `/model`, a palavra-chave `"default"` em [cadeias de modelo de fallback](#fallback-model-chains) e o fallback usado quando uma seleção excluída é descartada.

`enforceAvailableModels` não tem efeito quando `availableModels` não está definido ou está vazio: com `availableModels: []`, o modelo Padrão para o tipo de conta permanece utilizável, portanto a configuração não pode bloquear os usuários de cada modelo. Quando `availableModels` é não vazio, mas nenhuma entrada se resolve para um modelo permitido e disponível, a imposição se degrada e Padrão cai para o padrão do tipo de conta, com um aviso visível apenas em `--debug`. Mantenha pelo menos uma entrada garantidamente disponível na lista para evitar isso.

Implante ambas as chaves na [fonte gerenciada de maior precedência](https://code.claude.com/docs/pt/settings#settings-precedence): as fontes gerenciadas implantadas pelo administrador não se mesclam, portanto um par colocado em um arquivo de configurações gerenciadas é ignorado quando o console de administração entrega qualquer configuração.

<h3 id="control-the-model-users-run-on">
  Controlar o modelo em que os usuários executam
</h3>

A configuração `model` é uma seleção inicial, não uma imposição. Ela define qual modelo está ativo quando uma sessão é iniciada, mas os usuários ainda podem abrir `/model` e escolher Padrão, que se resolve para o padrão do sistema para seu nível, independentemente do que `model` está definido, a menos que [`enforceAvailableModels`](#enforce-the-allowlist-for-the-default-model) o redirecione.

Para controlar totalmente a experiência do modelo, combine estas configurações:

* **`availableModels`**: restringe para quais modelos nomeados os usuários podem alternar
* **`enforceAvailableModels`**: estende a lista de permissões `availableModels` à opção Padrão, para que Padrão não possa se resolver para um modelo fora da lista
* **`model`**: define a seleção de modelo inicial quando uma sessão é iniciada
* **`ANTHROPIC_DEFAULT_SONNET_MODEL`** / **`ANTHROPIC_DEFAULT_OPUS_MODEL`** / **`ANTHROPIC_DEFAULT_HAIKU_MODEL`** / **`ANTHROPIC_DEFAULT_FABLE_MODEL`**: controlam para o que a opção Padrão e os aliases `sonnet`, `opus`, `haiku` e `fable` se resolvem

Este exemplo inicia os usuários em Sonnet 4.5, limita o seletor a Sonnet e Haiku, e garante que Padrão se resolve para um modelo na lista de permissões em vez do padrão do nível:

```json theme={null}
{
  "model": "claude-sonnet-4-5",
  "availableModels": ["claude-sonnet-4-5", "haiku"],
  "enforceAvailableModels": true,
  "env": {
    "ANTHROPIC_DEFAULT_SONNET_MODEL": "claude-sonnet-4-5"
  }
}
```

Sem `enforceAvailableModels` ou o bloco `env`, um usuário que seleciona Padrão no seletor obteria a versão mais recente para seu nível, contornando a fixação de versão em `model` e `availableModels`. As duas configurações cobrem escopos diferentes: `enforceAvailableModels` faz Padrão obedecer à lista de permissões, enquanto o bloco `env` fixa qual versão um alias permitido como `sonnet` se resolve. Use `enforceAvailableModels` sozinho quando restringir famílias de modelo é suficiente; adicione o bloco `env` quando você também precisar fixar uma versão específica.

<h3 id="merge-behavior">
  Comportamento de mesclagem
</h3>

Quando a [fonte de configurações gerenciadas de maior precedência](https://code.claude.com/docs/pt/server-managed-settings#settings-precedence) define `availableModels`, apenas essa lista se aplica: as entradas em configurações de usuário, projeto ou local não podem estendê-la, e as fontes gerenciadas implantadas pelo administrador não se mesclam entre si, portanto uma lista implantada em um arquivo de configurações gerenciadas é ignorada quando as configurações gerenciadas pelo servidor entregam qualquer chave. Caso contrário, as listas de configurações de usuário, projeto e local são [concatenadas e desduplicadas](https://code.claude.com/docs/pt/settings#settings-precedence) como outras configurações de array. A partir de Claude Code v2.1.175, a lista gerenciada substitui entradas de menor precedência; versões anteriores as mesclam.

Dentro da lista efetiva, uma entrada nomeando um modelo específico em uma família, seja um prefixo de versão ou um ID de modelo completo, desativa a entrada de wildcard dessa família: `["sonnet", "claude-sonnet-4-5"]` permite apenas versões Sonnet 4.5, não cada modelo Sonnet.

<h3 id="mantle-model-ids">
  IDs de modelo Mantle
</h3>

Quando o [endpoint Bedrock Mantle](https://code.claude.com/docs/pt/amazon-bedrock#use-the-mantle-endpoint) está habilitado, as entradas em `availableModels` que começam com `anthropic.` são adicionadas ao seletor `/model` como opções personalizadas e roteadas para o endpoint Mantle. Esta é uma exceção à correspondência de alias descrita em [Fixar modelos para implantações de terceiros](#pin-models-for-third-party-deployments). A configuração ainda restringe o seletor às entradas listadas, e um ID Mantle incorpora um nome de família, portanto conta como uma entrada específica e desativa o wildcard dessa família: junto com qualquer ID Mantle, liste os prefixos de versão ou IDs completos que você deseja manter selecionáveis. Veja [Comportamento de mesclagem](#merge-behavior).

<h3 id="organization-model-restrictions">
  Restrições de modelo da organização
</h3>

Os administradores da organização em planos Claude Enterprise restringem quais modelos os membros podem executar desabilitando modelos individuais no console de administração claude.ai. Esta restrição é entregue com os direitos da conta quando Claude Code se autentica, separada de qualquer lista `availableModels` em configurações, e o servidor impõe a mesma restrição independentemente quando uma sessão é criada. Requer Claude Code v2.1.187 ou posterior.

A restrição se aplica quando um membro faz login ou usa sua própria chave de API. Credenciais com escopo de organização, como chaves de serviço da organização, não estão vinculadas a um usuário, portanto a restrição não se aplica a elas.

O Claude Console não possui controle de restrição de modelo. Organizações sem um plano Claude Enterprise, incluindo aquelas cujos membros se autenticam através da API Anthropic, restringem modelos com [`availableModels`](#restrict-model-selection) em [configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files), adicionando [`enforceAvailableModels`](#enforce-the-allowlist-for-the-default-model) para cobrir a opção Padrão. Essas configurações são impostas pelo Claude Code em si, não pelo servidor.

Um modelo restrito é ocultado do seletor `/model`. Selecioná-lo pelo nome com `--model`, a variável de ambiente `ANTHROPIC_MODEL` ou a configuração `model` mostra o aviso `Model "<name>" is restricted by your organization's settings. Using <model> instead.` e a sessão é iniciada em um modelo permitido. Digitar `/model <name>` para um modelo restrito é rejeitado com `Model '<name>' is restricted by your organization's settings. Run /model to choose a different model.` e a sessão mantém seu modelo atual.

Um [alias de família de modelo](#restrict-model-selection) como `opus` se resolve para a versão mais recente de sua família que a organização permite, com o mesmo aviso de substituição. `/model <alias>` é rejeitado apenas quando cada versão de sua família é restrita; um alias definido com `--model`, `ANTHROPIC_MODEL` ou a configuração `model` ainda é substituído na inicialização nesse caso. Antes da v2.1.205, um alias de família era substituído ou rejeitado com base apenas em sua versão mais recente lançada, mesmo quando uma versão mais antiga era permitida.

As restrições se aplicam em toda a organização ou por função:

* Desabilitar um modelo no nível da organização o remove para cada membro.
* O acesso no nível de função concede diferentes modelos a diferentes funções personalizadas, e um membro que possui várias funções pode usar qualquer modelo que uma de suas funções conceda.
* Os modelos Haiku estão sempre disponíveis e não podem ser desabilitados, portanto cada membro mantém pelo menos um modelo utilizável.
* Uma mudança de acesso entra em vigor em novas solicitações dentro de cerca de um minuto; o seletor `/model` reflete isso na próxima vez que uma sessão é iniciada.

Ambas as restrições se aplicam juntas: um modelo é selecionável apenas quando é permitido por `availableModels` e não é restrito pela organização. As restrições da organização são entregues a sessões na API Anthropic e implantações de [gateway LLM](https://code.claude.com/docs/pt/llm-gateway). Sessões em Amazon Bedrock, Google Cloud's Agent Platform, Microsoft Foundry e Claude Platform on AWS não as recebem, portanto use `availableModels` nesses provedores.

<h2 id="organization-default-model">
  Modelo padrão da organização
</h2>

Os administradores da organização em planos Claude Enterprise podem definir um modelo padrão para membros do Claude Code a partir do console de administração claude.ai, para toda a organização ou por função personalizada. Quando um é definido, a opção Padrão resolve para esse modelo em vez do [padrão do tipo de conta](#default-model-setting). Requer Claude Code v2.1.196 ou posterior.

A linha Padrão no seletor `/model` mostra o nome do modelo padrão da organização com o rótulo Padrão da organização. O rótulo lê Padrão da organização se o administrador definiu o padrão para toda a organização ou para sua função. Um padrão de função cobre membros dessa função personalizada e tem precedência sobre o padrão em toda a organização; quando várias de suas funções definem diferentes padrões, o modelo mais capaz se aplica.

O modelo padrão da organização é um ponto de partida, não uma restrição, e qualquer outra seleção de modelo tem precedência sobre ele:

* o sinalizador `--model` e a variável de ambiente `ANTHROPIC_MODEL`
* um valor `model` em [configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files) ou fornecido através de `--settings`
* um valor `model` em suas configurações de usuário, projeto ou local, incluindo um modelo que você salva com `/model`

Os administradores também podem configurar o modelo padrão da organização para substituir a seleção do usuário. Com a substituição ativada, ele tem precedência sobre o valor `model` em configurações de usuário, projeto e local, portanto um modelo que você salva com `/model` se aplica para a sessão atual e o modelo padrão da organização retorna no próximo lançamento. Quando sua seleção difere, `/model` mostra `Your organization's default (<model>) applies on restart`. O sinalizador `--model`, `ANTHROPIC_MODEL`, configurações gerenciadas e `--settings` ainda têm precedência mesmo com a substituição ativada. A substituição está disponível para um conjunto limitado de organizações; pergunte ao seu time de contas Anthropic sobre disponibilidade.

Para limitar quais modelos os membros podem selecionar, use [restrições de modelo da organização](#organization-model-restrictions) ou [`availableModels`](#restrict-model-selection) em vez disso.

Claude Code lê o modelo padrão da organização uma vez na inicialização, portanto um padrão que o administrador altera no meio da sessão entra em vigor no próximo lançamento.

Quando o modelo padrão da organização não substitui a seleção do usuário, o primeiro lançamento interativo após o administrador alterá-lo limpa a chave `model` de suas configurações de usuário uma vez, para que o novo padrão se aplique. Ele não altera nada mais no arquivo, e um modelo que você salva com `/model` após esse lançamento é mantido.

O modelo padrão da organização passa pelas mesmas verificações de restrição que qualquer outro modelo Padrão antes de ser adotado:

* [`availableModels`](#restrict-model-selection) por si só nunca restringe a opção Padrão, portanto um padrão da organização fora da lista de permissões ainda se aplica. Quando [`enforceAvailableModels`](#enforce-the-allowlist-for-the-default-model) também está definido, um padrão da organização fora da lista de permissões é remapeado para a primeira entrada da lista de permissões, como qualquer outro Padrão
* um padrão da organização que [restrições de modelo da organização](#organization-model-restrictions) negam para sua conta é substituído pelo modelo mais recente permitido em sua família, ou uma família de menor custo quando cada versão dela é restrita
* um padrão da organização que não está disponível para sua conta, como Fable 5 sob [retenção zero de dados](https://code.claude.com/docs/pt/zero-data-retention), é ignorado, e a opção Padrão resolve para o padrão do tipo de conta

A partir da v2.1.199, quando o modelo padrão da organização é uma família de modelo diferente do padrão usual do tipo de conta, o seletor `/model` mantém uma linha separada para essa família usual, para que você ainda possa alternar para ela para uma sessão. Na v2.1.196 até v2.1.198, essa linha está faltando no seletor.

O modelo padrão da organização é entregue a sessões autenticadas com a API Anthropic. Sessões em implantações de [gateway LLM](https://code.claude.com/docs/pt/llm-gateway), Amazon Bedrock, Agent Platform do Google Cloud, Microsoft Foundry e Claude Platform on AWS não o recebem. Para definir um padrão nesses deployments, use a chave `model` em [configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files) em vez disso.

<h2 id="organization-effort-limits">
  Limites de esforço da organização
</h2>

Os administradores da organização em planos Claude Enterprise podem definir um [nível de esforço](#adjust-effort-level) máximo por modelo para cada função personalizada, junto com [restrições de modelo da organização](#organization-model-restrictions) no nível de função. Os níveis acima do limite não são oferecidos no seletor `/effort`, e nomear um nível mais alto com `--effort` ou `/effort` é executado no limite em vez disso. Em sessões interativas e execuções `--print` em texto simples, um aviso nomeia os níveis solicitado e aplicado; com saída `json` ou `stream-json` ou em agentes de fundo, o limite se aplica silenciosamente. Os limites são por modelo, portanto alternar modelos pode alterar quais níveis estão disponíveis. Quando várias de suas funções concedem o mesmo modelo, o limite menos restritivo se aplica. Requer Claude Code v2.1.195 ou posterior.

Os limites de esforço são entregues junto com [restrições de modelo da organização](#organization-model-restrictions) e seguem a mesma disponibilidade de provedor: sessões em Amazon Bedrock, Agent Platform do Google Cloud, Microsoft Foundry e Claude Platform on AWS não os recebem.

<h2 id="special-model-behavior">
  Comportamento especial do modelo
</h2>

<h3 id="default-model-setting">
  Configuração do modelo `default`
</h3>

O comportamento de `default` depende do tipo de sua conta:

* **Max, Team Premium, Enterprise pagamento conforme o uso e API Anthropic**: padrão para Opus 4.8
* **Claude Platform na AWS, Amazon Bedrock e Google Cloud's Agent Platform**: padrão para Opus 4.8
* **Pro, Team Standard e assentos de assinatura Enterprise**: padrão para Sonnet 5
* **Microsoft Foundry**: padrão para Sonnet 4.5

Enterprise pagamento conforme o uso significa uma organização Enterprise cobrada por uso em vez de por assento de assinatura.

Antes de v2.1.207, `default` resolvido para Opus 4.7 em Claude Platform na AWS e para Sonnet 4.5 em Amazon Bedrock e Google Cloud's Agent Platform.

Quando um administrador definiu um [modelo padrão da organização](#organization-default-model), `default` resolve para esse modelo em vez do padrão do tipo de conta acima. Requer Claude Code v2.1.196 ou posterior.

Quando as configurações gerenciadas [aplicam a lista de permissões para o modelo Padrão](#enforce-the-allowlist-for-the-default-model) e o padrão do tipo de conta não está em `availableModels`, `default` resolve para o Padrão aplicado em vez do padrão do tipo de conta acima. Quando ambos se aplicam, o modelo padrão da organização substitui o padrão do tipo de conta primeiro e a imposição então se aplica a ele: um padrão da organização na lista de permissões é mantido, enquanto um fora da lista resolve para o Padrão aplicado.

Fable 5 não é o modelo padrão em nenhum tipo de conta. As sessões usam Fable 5 apenas depois que você o escolhe, com `/model fable`, uma configuração `model` ou o alias `best` onde Fable 5 está disponível. Escolhê-lo com `/model` o salva como o modelo selecionado em suas configurações de usuário, portanto as sessões posteriores começam em Fable 5 até que você altere os modelos.

<h3 id="opusplan-model-setting">
  Configuração do modelo `opusplan`
</h3>

O alias de modelo `opusplan` fornece uma abordagem híbrida automatizada:

* **Em Plan Mode**: usa `opus` para raciocínio complexo e decisões de arquitetura
* **Em modo de execução**: muda automaticamente para `sonnet` para geração de código e implementação

Isso oferece o melhor dos dois mundos: o raciocínio superior do Opus para planejamento e a eficiência do Sonnet para execução.

A fase Opus do Plan Mode usa a mesma janela de contexto da configuração do modelo `opus`. Nos níveis de assinatura onde Opus é [automaticamente atualizado para contexto 1M](#extended-context), `opusplan` recebe a atualização em Plan Mode também. Para forçar contexto 1M para ambas as fases quando você não está em um nível de atualização automática, defina o modelo para `opusplan[1m]`.

Quando [`availableModels`](#restrict-model-selection) exclui o Opus mais recente, mas permite uma versão mais antiga, por exemplo `["sonnet", "claude-opus-4-6"]`, `opusplan` usa o Opus mais recente permitido para planejamento e permanece apenas em Sonnet quando todo Opus é excluído. Uma sessão Haiku que normalmente seria atualizada para Sonnet em Plan Mode da mesma forma usa o Sonnet mais recente permitido e permanece em Haiku apenas quando todo Sonnet é excluído. Antes de v2.1.205, Plan Mode permanecia no modelo da sessão sempre que a versão mais recente da família de atualização era excluída, mesmo quando a lista de permissões permitia uma mais antiga.

A substituição de uma versão mais antiga permitida se aplica na API Anthropic e [Claude Platform na AWS](https://code.claude.com/docs/pt/claude-platform-on-aws). Na Amazon Bedrock, Google Cloud's Agent Platform, Microsoft Foundry e Mantle, cujas implantações usam IDs de modelo específicos do provedor, Plan Mode permanece no modelo da sessão sempre que o modelo de atualização é excluído.

Para uma abordagem híbrida onde Claude decide no meio da tarefa quando consultar um segundo modelo em vez de alternar no limite do plano, veja a [ferramenta advisor](https://code.claude.com/docs/pt/advisor).

<h3 id="fallback-model-chains">
  Cadeias de modelo de fallback
</h3>

Quando o modelo primário está sobrecarregado, indisponível ou retorna outro erro de servidor não retentável, Claude Code pode alternar para um modelo de fallback em vez de falhar na solicitação. Erros de autenticação, faturamento, limite de taxa, tamanho de solicitação e transporte nunca acionam uma alternância; esses seguem sua manipulação de erro e retry normal.

Configure um ou mais modelos de fallback e Claude Code os tenta em ordem, mostrando um aviso quando alterna. A alternância dura apenas para a volta atual, portanto sua próxima mensagem tenta o modelo primário primeiro novamente. As cadeias são limitadas a três modelos após remoção de duplicatas, e entradas extras são ignoradas.

Defina uma cadeia para uma sessão com o sinalizador `--fallback-model`, que aceita uma lista separada por vírgulas:

```bash theme={null}
claude --fallback-model sonnet,haiku
```

Para persistir uma cadeia entre sessões, defina `fallbackModel` em [configurações](https://code.claude.com/docs/pt/settings) como uma matriz:

```json theme={null}
{
  "fallbackModel": ["claude-sonnet-5", "claude-haiku-4-5"]
}
```

O sinalizador `--fallback-model` tem precedência sobre a configuração `fallbackModel`. Cada elemento aceita um nome de modelo ou alias, e `"default"` se expande para o modelo padrão.

Dois casos fazem um elemento ser ignorado:

* **Modelo indisponível**: um modelo que não pode ser alcançado, como um modelo aposentado fixado em configurações, é ignorado e Claude Code continua para o próximo elemento.
* **Fora da lista de permissões**: um elemento não permitido por [`availableModels`](#restrict-model-selection) é descartado quando a cadeia é lida e nunca é tentado.

<h3 id="automatic-model-fallback">
  Fallback automático de modelo
</h3>

Esta seção cobre fallback baseado em conteúdo de Fable 5. Para fallback baseado em disponibilidade quando um modelo está sobrecarregado ou indisponível, veja [Cadeias de modelo de fallback](#fallback-model-chains).

Fable 5 é executado com classificadores de segurança para conteúdo de cibersegurança e biologia. Quando um classificador sinaliza uma solicitação, Claude Code executa novamente essa solicitação no modelo Opus padrão do seu provedor e mostra um aviso na transcrição. Na API Anthropic, implantações de [gateway LLM](https://code.claude.com/docs/pt/llm-gateway) e [Claude Platform na AWS](https://code.claude.com/docs/pt/claude-platform-on-aws), esse modelo é Opus 4.8. No [gateway de aplicativos Claude](https://code.claude.com/docs/pt/claude-apps-gateway), é Opus 4.7 a menos que você aponte o alias [`opus`](#environment-variables) para outro modelo.

A sessão então continua nesse modelo Opus. Para retornar a Fable 5, execute `/model fable`.

O alvo de fallback é verificado contra [`availableModels`](#restrict-model-selection). Quando é bloqueado, nenhum fallback ocorre. A recusa aparece como um erro normal e o modelo da sessão permanece inalterado.

<h4 id="check-what-triggered-fallback">
  Verificar o que acionou fallback
</h4>

O fallback pode ser acionado na primeira solicitação de uma sessão, antes de você enviar algo incomum, porque a primeira solicitação carrega contexto do espaço de trabalho, como seu conteúdo CLAUDE.md e status do git. Um repositório que contém material de segurança ou biologia pode acionar o classificador apenas nesse contexto.

Para verificar se as personalizações são o gatilho, inicie uma sessão com `claude --safe-mode`, que desabilita personalizações como CLAUDE.md, skills, servidores MCP e hooks. O status do git e nomes de diretórios não são personalizações e ainda estão inclusos.

<h4 id="ask-before-switching">
  Perguntar antes de alternar
</h4>

Para decidir o que acontece cada vez que uma solicitação é sinalizada, em vez de alternar automaticamente, execute `/config` e desative "switch models when a message is flagged". Uma solicitação sinalizada então pausa a sessão com duas opções: alternar para o modelo Opus ou editar o prompt e tentar novamente em Fable 5.

Alguns casos se comportam diferentemente:

* Se ambos os modelos sinalizarem a mesma solicitação, você pode editar o prompt e tentar novamente, ou iniciar uma nova sessão.
* Em sessões móveis [Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web), editar e tentar novamente não é suportado. Alterne modelos ou continue a sessão de um navegador de desktop ou do aplicativo de desktop.
* Em [modo não interativo](https://code.claude.com/docs/pt/cli-reference#cli-flags) e integrações SDK que não podem mostrar o prompt, uma solicitação sinalizada encerra a volta com uma recusa em vez disso.
* Quando o alvo de fallback é bloqueado por [`availableModels`](#restrict-model-selection), o prompt não é mostrado. A solicitação sinalizada termina com a recusa, o mesmo que fallback automático quando o alvo é bloqueado.

<h4 id="enable-fallback-on-bedrock-agent-platform-and-foundry">
  Habilitar fallback em Bedrock, Agent Platform e Foundry
</h4>

Em [Amazon Bedrock](https://code.claude.com/docs/pt/amazon-bedrock), [Google Cloud's Agent Platform](https://code.claude.com/docs/pt/google-vertex-ai) e [Microsoft Foundry](https://code.claude.com/docs/pt/microsoft-foundry), IDs de modelo são específicos do provedor, portanto o fallback automático opera apenas quando Claude Code pode identificar ambos os modelos envolvidos:

* Claude Code deve reconhecer o modelo atual como Fable 5: o ID do modelo contém `claude-fable-5`, corresponde ao valor de `ANTHROPIC_DEFAULT_FABLE_MODEL` ou é mapeado com [`modelOverrides`](#override-model-ids-per-version).
* O alvo de fallback deve resolver para um modelo Opus: o valor de `ANTHROPIC_DEFAULT_OPUS_MODEL` se definido, caso contrário uma entrada Opus 4.8 na lista de modelos do provedor.

Se qualquer modelo não puder ser identificado, Claude Code não alterna automaticamente. A solicitação sinalizada termina com uma mensagem de recusa, e você pode alternar modelos com [`/model`](#setting-your-model) e tentar novamente. Para habilitar fallback automático nesses provedores, defina `ANTHROPIC_DEFAULT_FABLE_MODEL` para seu ID de modelo Fable 5 e `ANTHROPIC_DEFAULT_OPUS_MODEL` para seu ID de modelo Opus 4.8.

<h4 id="security-research-and-biology-workloads">
  Pesquisa de segurança e cargas de trabalho de biologia
</h4>

Cargas de trabalho em segurança ofensiva ou biologia, incluindo testes de penetração, exercícios Capture the Flag (CTF) e bases de código adjacentes à biologia, acionam fallback frequentemente, geralmente na primeira solicitação. Para trabalho substancial de biologia, espere que quase todas as solicitações sejam redirecionadas.

Este é o roteamento esperado para esses domínios, não uma sinalização de conta. Se sua organização precisa de capacidade de classe Fable para este trabalho, pergunte ao seu time de contas Anthropic sobre programas de acesso confiável.

<h3 id="adjust-effort-level">
  Ajustar nível de esforço
</h3>

[Níveis de esforço](https://platform.claude.com/docs/pt/build-with-claude/effort) controlam raciocínio adaptativo, que permite que o modelo decida se e quanto pensar em cada etapa com base na complexidade da tarefa. Esforço menor é mais rápido e mais barato para tarefas diretas, enquanto esforço maior fornece raciocínio mais profundo para problemas complexos.

Os níveis de esforço disponíveis dependem do modelo. Modelos não listados aqui não suportam esforço:

| Modelo                        | Níveis                                  |
| :---------------------------- | :-------------------------------------- |
| Fable 5                       | `low`, `medium`, `high`, `xhigh`, `max` |
| Sonnet 5, Opus 4.8 e Opus 4.7 | `low`, `medium`, `high`, `xhigh`, `max` |
| Opus 4.6 e Sonnet 4.6         | `low`, `medium`, `high`, `max`          |

Se você definir um nível que o modelo ativo não suporta, Claude Code volta para o nível mais alto suportado no ou abaixo do que você definiu. Por exemplo, `xhigh` é executado como `high` em Opus 4.6. Sua organização também pode limitar quais níveis estão disponíveis para um modelo; veja [Limites de esforço da organização](#organization-effort-limits).

O esforço padrão é `high` em Fable 5, Sonnet 5, Opus 4.8, Opus 4.6 e Sonnet 4.6, e `xhigh` em Opus 4.7.

Quando você executa Fable 5, Opus 4.8 ou Opus 4.7 pela primeira vez, Claude Code aplica o esforço padrão desse modelo mesmo que você tenha definido anteriormente um nível diferente para outro modelo: `high` em Fable 5 e Opus 4.8, e `xhigh` em Opus 4.7. Execute `/effort` novamente para escolher um nível diferente após alternar. Esse padrão é mantido entre sessões até que você faça uma escolha de esforço explícita, como executar `/effort` em uma sessão interativa ou iniciar com `--effort`.

`low`, `medium`, `high` e `xhigh` persistem entre sessões quando você os define em uma sessão interativa. Um nível definido com `/effort` em [modo não interativo](https://code.claude.com/docs/pt/headless), com o sinalizador `-p`, se aplica apenas à sessão atual e não é salvo como seu padrão. Um `/effort` não interativo também não pode liberar a retenção de padrão do modelo acima: em Fable 5, Opus 4.8 e Opus 4.7 ele relata `Not applied` e a sessão permanece no esforço padrão do modelo, portanto passe `--effort` na inicialização em vez disso. `max` fornece o raciocínio mais profundo sem restrição no gasto de tokens e se aplica apenas à sessão atual, exceto quando definido através da variável de ambiente `CLAUDE_CODE_EFFORT_LEVEL`.

O menu `/effort` também oferece `ultracode`. Ultracode é uma configuração de Claude Code em vez de um nível de esforço do modelo: envia `xhigh` para o modelo e adicionalmente tem Claude orquestrar [fluxos de trabalho dinâmicos](https://code.claude.com/docs/pt/workflows) para tarefas substanciais. Se aplica apenas à sessão atual.

Você pode ativar ultracode através de qualquer um dos seguintes:

* **`/effort`**: execute `/effort ultracode` ou selecione-o no menu
* **Sinalizador `--effort`**: inicie com `claude --effort ultracode`, que inicia a sessão em esforço `xhigh` com ultracode ativado
* **`--settings` ou uma solicitação de controle do Agent SDK**: passe `"ultracode": true`. Uma solicitação [`applyFlagSettings()`](https://code.claude.com/docs/pt/agent-sdk/typescript#applyflagsettings) também aceita `effortLevel: "ultracode"`

Passar `ultracode` para o sinalizador `--effort` ou o valor `effortLevel` do Agent SDK requer Claude Code v2.1.203 ou posterior. Antes de v2.1.203, `--effort ultracode` imprimia `Unknown --effort value 'ultracode'` e a sessão começava no esforço padrão.

A configuração `effortLevel` persistida e a variável de ambiente `CLAUDE_CODE_EFFORT_LEVEL` não aceitam `ultracode`.

Quando ultracode não está disponível, por exemplo quando [fluxos de trabalho estão desativados](https://code.claude.com/docs/pt/workflows#turn-workflows-off), `--effort ultracode` define apenas esforço `xhigh`.

<h4 id="choose-an-effort-level">
  Escolher um nível de esforço
</h4>

Cada nível negocia gasto de tokens contra capacidade. O padrão é adequado para a maioria das tarefas de codificação; ajuste quando você quiser um equilíbrio diferente.

| Nível       | Quando usá-lo                                                                                                                                                                 |
| :---------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `low`       | Reserve para tarefas curtas, delimitadas, sensíveis à latência que não são sensíveis à inteligência                                                                           |
| `medium`    | Reduz o uso de tokens para trabalho sensível a custos que pode fazer concessões em inteligência                                                                               |
| `high`      | Equilibra o uso de tokens e inteligência. Padrão em Fable 5, Sonnet 5, Opus 4.8, Opus 4.6 e Sonnet 4.6                                                                        |
| `xhigh`     | Raciocínio mais profundo com gasto de tokens mais alto. Padrão em Opus 4.7                                                                                                    |
| `max`       | Pode melhorar o desempenho em tarefas exigentes, mas pode mostrar retornos decrescentes e é propenso a pensar demais. Teste antes de adotar amplamente                        |
| `ultracode` | Uma configuração de Claude Code que planeja um [fluxo de trabalho dinâmico](https://code.claude.com/docs/pt/workflows) para cada tarefa substancial com raciocínio `xhigh` por mensagem. Apenas de sessão |

A escala de esforço é calibrada por modelo, portanto o mesmo nome de nível não representa o mesmo valor subjacente entre modelos.

<h4 id="use-ultrathink-for-one-off-deep-reasoning">
  Usar ultrathink para raciocínio profundo único
</h4>

Inclua `ultrathink` em qualquer lugar em seu prompt para solicitar raciocínio mais profundo nessa volta sem alterar sua configuração de esforço de sessão. Claude Code reconhece a palavra-chave e adiciona uma instrução no contexto. O nível de esforço enviado para a API permanece inalterado. Outras frases como "think", "think hard" e "think more" são passadas como texto de prompt ordinário e não são reconhecidas como palavras-chave.

<h4 id="set-the-effort-level">
  Definir o nível de esforço
</h4>

Você pode alterar o esforço através de qualquer um dos seguintes:

* **`/effort`**: execute `/effort` sem argumentos para abrir um controle deslizante interativo, `/effort` seguido por um nome de nível para defini-lo diretamente, ou `/effort auto` para redefinir para o padrão do modelo
* **Em `/model`**: use as teclas de seta esquerda/direita para ajustar o controle deslizante de esforço ao selecionar um modelo
* **Sinalizador `--effort`**: passe um nome de nível para defini-lo para uma única sessão ao iniciar Claude Code
* **Variável de ambiente**: defina `CLAUDE_CODE_EFFORT_LEVEL` para um nome de nível ou `auto`
* **Configurações**: defina `effortLevel` para `low`, `medium`, `high` ou `xhigh` em seu arquivo de configurações. `max` e `ultracode` são [apenas de sessão](#adjust-effort-level) e não são aceitos aqui
* **Frontmatter de skill e subagent**: defina `effort` em um arquivo markdown de [skill](https://code.claude.com/docs/pt/skills#frontmatter-reference) ou [subagent](https://code.claude.com/docs/pt/sub-agents#supported-frontmatter-fields) para substituir o nível de esforço quando esse skill ou subagent é executado

A variável de ambiente tem precedência sobre todos os outros métodos, depois seu nível configurado, depois o padrão do modelo. O esforço de frontmatter se aplica quando esse skill ou subagent está ativo, substituindo o nível de sessão, mas não a variável de ambiente.

O controle deslizante de esforço aparece em `/model` quando um modelo suportado é selecionado. O nível de esforço atual também é exibido ao lado do logo e spinner, por exemplo "with low effort", para que você possa confirmar qual configuração está ativa sem abrir `/model`.

<h4 id="adaptive-reasoning-and-fixed-thinking-budgets">
  Raciocínio adaptativo e orçamentos de pensamento fixos
</h4>

O raciocínio adaptativo torna o pensamento opcional em cada etapa, portanto Claude pode responder mais rápido a prompts rotineiros e reservar pensamento mais profundo para etapas que se beneficiam dele. Se você quiser que Claude pense mais ou menos frequentemente do que o nível atual produz, você pode dizer isso diretamente em seu prompt ou em `CLAUDE.md`; o modelo responde a essa orientação dentro de sua configuração de esforço.

Fable 5, Sonnet 5 e Opus 4.7 e posterior sempre usam raciocínio adaptativo. O modo de orçamento de pensamento fixo e `CLAUDE_CODE_DISABLE_ADAPTIVE_THINKING` não se aplicam a eles.

Em Opus 4.6 e Sonnet 4.6, você pode definir `CLAUDE_CODE_DISABLE_ADAPTIVE_THINKING=1` para reverter para o orçamento de pensamento fixo anterior controlado por `MAX_THINKING_TOKENS`. Veja [variáveis de ambiente](https://code.claude.com/docs/pt/env-vars).

<h3 id="extended-thinking">
  Pensamento estendido
</h3>

Pensamento estendido é o raciocínio que Claude emite antes de responder. Em modelos que suportam [raciocínio adaptativo](#adjust-effort-level), o nível de esforço é o controle principal para quanto pensamento acontece; as configurações abaixo ativam ou desativam o pensamento e controlam como ele é exibido.

| Controle                                 | Como defini-lo                                                                                                                                                                                                                                                                                                                                                                                         |
| :--------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Alternar para a sessão atual             | Pressione `Option+T` no macOS ou `Alt+T` no Windows e Linux                                                                                                                                                                                                                                                                                                                                            |
| Definir o padrão global                  | Execute `/config` e alterne o modo de pensamento. Salvo como `alwaysThinkingEnabled` em `~/.claude/settings.json`                                                                                                                                                                                                                                                                                      |
| Desabilitar independentemente do esforço | Defina [`MAX_THINKING_TOKENS=0`](https://code.claude.com/docs/pt/env-vars), que desativa o pensamento na API Anthropic exceto em Fable 5. Em [provedores de terceiros](https://code.claude.com/docs/pt/third-party-integrations) isso omite o parâmetro `thinking` em vez disso, e modelos de raciocínio adaptativo ainda podem pensar. Outros valores se aplicam apenas com um [orçamento de pensamento fixo](#adaptive-reasoning-and-fixed-thinking-budgets) |

O pensamento não pode ser desativado em Fable 5. O alternador de sessão, `alwaysThinkingEnabled` e `MAX_THINKING_TOKENS=0` não têm efeito lá, e Fable 5 decide por etapa quanto pensar com base no nível de esforço.

A saída de pensamento é recolhida por padrão. Pressione `Ctrl+O` para alternar o modo verboso e ver o raciocínio como texto em itálico cinzento. Sessões interativas na API Anthropic recebem blocos de pensamento redigidos por padrão, portanto defina `showThinkingSummaries: true` em [configurações](https://code.claude.com/docs/pt/settings) se você quiser os resumos completos disponíveis quando expandir. Você é cobrado por todos os tokens de pensamento gerados, mesmo quando recolhidos ou redigidos.

<h3 id="extended-context">
  Contexto estendido
</h3>

Fable 5, Sonnet 5, Opus 4.6 e posterior, e Sonnet 4.6, suportam uma [janela de contexto de 1 milhão de tokens](https://platform.claude.com/docs/pt/build-with-claude/context-windows#context-window-sizes-by-model) para sessões longas com grandes bases de código.

A disponibilidade varia por modelo e plano. Na API Anthropic, Fable 5, Sonnet 5, Opus 4.8 e Opus 4.7 sempre são executados com a janela 1M. Nos planos Max, Team e Enterprise, Opus é automaticamente atualizado para contexto 1M sem configuração adicional. Isso se aplica aos assentos Team Standard e Team Premium. Sonnet 4.6 com contexto 1M não faz parte da atualização automática e requer [créditos de uso](https://support.claude.com/pt/articles/12429409-extra-usage-for-paid-claude-plans) em todos os planos de assinatura, incluindo Max.

| Plano                          | Opus com contexto 1M                                                                                        | Sonnet 4.6 com contexto 1M                                                                                  |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| Max, Team e Enterprise         | Incluído na assinatura                                                                                      | Requer [créditos de uso](https://support.claude.com/pt/articles/12429409-extra-usage-for-paid-claude-plans) |
| Pro                            | Requer [créditos de uso](https://support.claude.com/pt/articles/12429409-extra-usage-for-paid-claude-plans) | Requer [créditos de uso](https://support.claude.com/pt/articles/12429409-extra-usage-for-paid-claude-plans) |
| API e pagamento conforme o uso | Acesso completo                                                                                             | Acesso completo                                                                                             |

Para desabilitar completamente o contexto 1M, defina `CLAUDE_CODE_DISABLE_1M_CONTEXT=1`. Isso remove variantes de modelo 1M do seletor de modelo. Veja [variáveis de ambiente](https://code.claude.com/docs/pt/env-vars).

A janela de contexto 1M usa preços de modelo padrão sem prêmio para tokens além de 200K. Para planos onde o contexto estendido está incluído em sua assinatura, o uso permanece coberto por sua assinatura. Para planos que acessam contexto estendido através de créditos de uso, os tokens são cobrados para créditos de uso.

Se sua conta suporta contexto 1M, a opção aparece no seletor de modelo (`/model`) nas versões mais recentes do Claude Code. Se você não a vir, tente reiniciar sua sessão.

Você também pode usar o sufixo `[1m]` com aliases de modelo ou nomes de modelo completos:

```bash theme={null}
# Use o alias opus[1m] ou sonnet[1m]
/model opus[1m]
/model sonnet[1m]

# Ou anexe [1m] a um nome de modelo completo
/model claude-opus-4-8[1m]
```

<h4 id="sonnet-5-context-window">
  Janela de contexto do Sonnet 5
</h4>

Na API Anthropic, Sonnet 5 sempre é executado com a janela de contexto 1M. Não há variante de 200K, nenhum sufixo `[1m]` para selecionar e nenhum crédito de uso necessário em qualquer plano. As sessões fazem compactação automática antes que a janela encha, em cerca de 967K tokens por padrão; defina [`CLAUDE_CODE_AUTO_COMPACT_WINDOW`](https://code.claude.com/docs/pt/env-vars) para escolher um limite diferente.

Duas configurações orçam a janela em 200K em vez disso e fazem compactação automática nesse limite:

* **Gateway LLM**: quando `ANTHROPIC_BASE_URL` aponta para um [gateway](https://code.claude.com/docs/pt/llm-gateway), Claude Code não pode verificar o suporte a 1M. Para usar a janela completa, selecione Sonnet 5 (1M context) no seletor de modelo, que mapeia para `sonnet[1m]`.
* **`CLAUDE_CODE_DISABLE_1M_CONTEXT=1`**: trata sessões Sonnet 5 como tendo uma janela de 200K, para implantações que precisam limitar o contexto.

<h2 id="checking-your-current-model">
  Verificando seu modelo atual
</h2>

Você pode ver qual modelo está usando atualmente em dois lugares:

* Na [linha de status](https://code.claude.com/docs/pt/statusline), se você tiver uma configurada
* Em `/status`, que também exibe as informações de sua conta

<h2 id="add-a-custom-model-option">
  Adicionar uma opção de modelo personalizado
</h2>

Use `ANTHROPIC_CUSTOM_MODEL_OPTION` para adicionar uma única entrada personalizada ao seletor `/model` sem substituir os aliases integrados. Isso é útil para testar IDs de modelo que Claude Code não lista por padrão. Para implantações de gateway LLM, Claude Code pode preencher o seletor a partir do endpoint `/v1/models` do gateway quando `CLAUDE_CODE_ENABLE_GATEWAY_MODEL_DISCOVERY=1` está definido, portanto essa variável é necessária apenas quando a descoberta está desabilitada ou não retorna o modelo que você deseja. Consulte [descoberta de modelo de gateway](https://code.claude.com/docs/pt/llm-gateway-protocol#model-discovery).

Este exemplo define todas as três variáveis para tornar uma implantação Opus roteada por gateway selecionável:

```bash theme={null}
export ANTHROPIC_CUSTOM_MODEL_OPTION="my-gateway/claude-opus-4-8"
export ANTHROPIC_CUSTOM_MODEL_OPTION_NAME="Opus via Gateway"
export ANTHROPIC_CUSTOM_MODEL_OPTION_DESCRIPTION="Custom deployment routed through the internal LLM gateway"
```

A entrada personalizada aparece na parte inferior do seletor `/model`. `ANTHROPIC_CUSTOM_MODEL_OPTION_NAME` e `ANTHROPIC_CUSTOM_MODEL_OPTION_DESCRIPTION` são opcionais. Se omitidos, o ID do modelo é usado como o nome e a descrição padrão é `Custom model (<model-id>)`.

Claude Code ignora a validação para o ID do modelo definido em `ANTHROPIC_CUSTOM_MODEL_OPTION`, portanto você pode usar qualquer string que seu endpoint de API aceite. Quando [`availableModels`](#restrict-model-selection) está definido, inclua o ID do modelo personalizado na lista de permissões também: a entrada personalizada é filtrada do seletor e uma seleção `--model` dela é rejeitada como qualquer outro modelo excluído. Um ID personalizado que incorpora um nome de família, como `my-gateway/claude-opus-4-8`, conta como uma entrada específica para essa família e desabilita seu curinga, portanto também liste as versões que você pretende manter selecionáveis. Consulte [Comportamento de mesclagem](#merge-behavior).

<h2 id="environment-variables">
  Variáveis de ambiente
</h2>

Você pode usar as seguintes variáveis de ambiente para controlar os nomes de modelo para os quais os aliases mapeiam. Cada valor deve ser um nome de modelo completo, ou o identificador equivalente para seu provedor de API.

| Variável de ambiente             | Descrição                                                                                                                                                                                                                                                                                                                                                                                                       |
| -------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ANTHROPIC_DEFAULT_FABLE_MODEL`  | O modelo a usar para `fable`, e o ID de modelo que Claude Code reconhece como Fable 5 para [fallback automático de modelo](#automatic-model-fallback) em provedores de terceiros                                                                                                                                                                                                                                |
| `ANTHROPIC_DEFAULT_OPUS_MODEL`   | O modelo a usar para `opus`, ou para `opusplan` quando Plan Mode está ativo.                                                                                                                                                                                                                                                                                                                                    |
| `ANTHROPIC_DEFAULT_SONNET_MODEL` | O modelo a usar para `sonnet`, ou para `opusplan` quando Plan Mode não está ativo.                                                                                                                                                                                                                                                                                                                              |
| `ANTHROPIC_DEFAULT_HAIKU_MODEL`  | O modelo a usar para `haiku`, ou [funcionalidade de fundo](https://code.claude.com/docs/pt/costs#background-token-usage)                                                                                                                                                                                                                                                                                                                    |
| `CLAUDE_CODE_SUBAGENT_MODEL`     | O modelo a usar para todos os [subagents](https://code.claude.com/docs/pt/sub-agents#choose-a-model), [agent teams](https://code.claude.com/docs/pt/agent-teams), e os agentes que um [workflow](https://code.claude.com/docs/pt/workflows) executa. Aceita um alias como `haiku` ou um nome de modelo completo, e substitui tanto o parâmetro `model` por invocação quanto o frontmatter `model` da definição do subagent. Defina como `inherit` para usar resolução de modelo normal em vez disso |

Nota: `ANTHROPIC_SMALL_FAST_MODEL` está descontinuado em favor de `ANTHROPIC_DEFAULT_HAIKU_MODEL`.

<h3 id="pin-models-for-third-party-deployments">
  Fixar modelos para implantações de terceiros
</h3>

Ao implantar Claude Code através de [Amazon Bedrock](https://code.claude.com/docs/pt/amazon-bedrock), [Google Cloud's Agent Platform](https://code.claude.com/docs/pt/google-vertex-ai), [Microsoft Foundry](https://code.claude.com/docs/pt/microsoft-foundry), ou [Claude Platform on AWS](https://code.claude.com/docs/pt/claude-platform-on-aws), fixe versões de modelo antes de lançar para usuários.

Sem fixação, Claude Code usa aliases de modelo como `fable`, `opus`, `sonnet` e `haiku` que resolvem para um ID de modelo padrão integrado para cada provedor. Esse padrão pode ficar atrás da versão mais recente do Anthropic, e o modelo para o qual aponta pode ainda não estar habilitado na conta de um usuário. Quando o padrão não está disponível, os usuários de Amazon Bedrock e Google Cloud's Agent Platform veem um aviso e a sessão volta para uma versão anterior do modelo padrão, ou para o modelo Sonnet padrão quando o padrão é um modelo Opus e nenhuma versão Opus está disponível. Os usuários de Microsoft Foundry veem erros em vez disso, porque Microsoft Foundry não tem verificação de inicialização equivalente.

<Warning>
  Defina as variáveis de ambiente de modelo para IDs de versão específicos como parte de sua configuração inicial. Fixar permite que você controle quando seus usuários se movem para um novo modelo.
</Warning>

Use as seguintes variáveis de ambiente com IDs de modelo específicos de versão para seu provedor:

| Provedor                      | Exemplo                                                              |
| :---------------------------- | :------------------------------------------------------------------- |
| Amazon Bedrock                | `export ANTHROPIC_DEFAULT_OPUS_MODEL='us.anthropic.claude-opus-4-8'` |
| Google Cloud's Agent Platform | `export ANTHROPIC_DEFAULT_OPUS_MODEL='claude-opus-4-8'`              |
| Microsoft Foundry             | `export ANTHROPIC_DEFAULT_OPUS_MODEL='claude-opus-4-8'`              |

Aplique o mesmo padrão para `ANTHROPIC_DEFAULT_FABLE_MODEL`, `ANTHROPIC_DEFAULT_SONNET_MODEL` e `ANTHROPIC_DEFAULT_HAIKU_MODEL`. Para IDs de modelo atuais e legados em todos os provedores, veja [Visão geral de modelos](https://platform.claude.com/docs/en/about-claude/models/overview). Para atualizar usuários para uma nova versão de modelo, atualize essas variáveis de ambiente e reimplante.

Para habilitar [contexto estendido](#extended-context) para um modelo fixado, anexe `[1m]` ao ID do modelo em `ANTHROPIC_DEFAULT_OPUS_MODEL` ou `ANTHROPIC_DEFAULT_SONNET_MODEL`:

```bash theme={null}
export ANTHROPIC_DEFAULT_OPUS_MODEL='claude-opus-4-8[1m]'
```

O sufixo `[1m]` aplica a janela de contexto 1M a todo o uso dos aliases `opus` e `sonnet`, incluindo a fase Opus do modo de plano de [`opusplan`](#opusplan-model-setting).

* Claude Code remove o sufixo antes de enviar o ID do modelo para seu provedor.
* Apenas anexe `[1m]` quando o modelo subjacente [suportar contexto 1M](https://platform.claude.com/docs/en/build-with-claude/context-windows#context-window-sizes-by-model).
* O sufixo é lido por variável, não por modelo. No Amazon Bedrock, Google Cloud's Agent Platform e Microsoft Foundry, um ID de modelo sem `[1m]` em uma variável usa contexto 200K mesmo se outra variável define o mesmo modelo com o sufixo. Sonnet 5 sempre é executado com a janela 1M nesses provedores e nunca precisa do sufixo.

<Note>
  Uma lista de permissões `availableModels` entregue através de [MDM ou um arquivo de configurações gerenciado](https://code.claude.com/docs/pt/settings#settings-files) ainda se aplica ao usar provedores de terceiros; [configurações gerenciadas pelo servidor não são entregues lá](https://code.claude.com/docs/pt/server-managed-settings#platform-availability). A filtragem corresponde a um alias de modelo como `opus`, um prefixo de versão como `claude-opus-4-8`, ou o ID de modelo completo em forma de provedor. Prefixos específicos do provedor como `us.anthropic.` não são removidos, então para permitir um modelo específico, liste o mesmo ID em forma de provedor que o seletor mostra, ou mapeie através de [`modelOverrides`](#override-model-ids-per-version). Qualquer sufixo `[1m]` é removido tanto da entrada da lista de permissões quanto do modelo solicitado antes da correspondência.
</Note>

<h3 id="customize-pinned-model-display-and-capabilities">
  Personalizar exibição e capacidades do modelo fixado
</h3>

Quando você fixa um modelo em um provedor de terceiros, o ID específico do provedor aparece como está no seletor `/model` e Claude Code pode não reconhecer quais recursos o modelo suporta. Você pode substituir o nome de exibição e declarar capacidades com variáveis de ambiente complementares para cada modelo fixado.

Essas variáveis têm efeito em provedores de terceiros, como Amazon Bedrock, Google Cloud's Agent Platform e Microsoft Foundry. As variáveis `_NAME` e `_DESCRIPTION` também têm efeito quando `ANTHROPIC_BASE_URL` aponta para um [gateway LLM](https://code.claude.com/docs/pt/llm-gateway). Elas não têm efeito ao conectar diretamente a `api.anthropic.com`.

| Variável de ambiente                                  | Descrição                                                                                                                |
| ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| `ANTHROPIC_DEFAULT_OPUS_MODEL_NAME`                   | Nome de exibição para o modelo Opus fixado no seletor `/model`. Padrão para o ID do modelo quando não definido           |
| `ANTHROPIC_DEFAULT_OPUS_MODEL_DESCRIPTION`            | Descrição de exibição para o modelo Opus fixado no seletor `/model`. Padrão para `Custom Opus model` quando não definido |
| `ANTHROPIC_DEFAULT_OPUS_MODEL_SUPPORTED_CAPABILITIES` | Lista separada por vírgulas de capacidades que o modelo Opus fixado suporta                                              |

Os mesmos sufixos `_NAME`, `_DESCRIPTION` e `_SUPPORTED_CAPABILITIES` estão disponíveis para `ANTHROPIC_DEFAULT_SONNET_MODEL`, `ANTHROPIC_DEFAULT_HAIKU_MODEL`, `ANTHROPIC_DEFAULT_FABLE_MODEL` e `ANTHROPIC_CUSTOM_MODEL_OPTION`.

Claude Code habilita recursos como [níveis de esforço](#adjust-effort-level) e [pensamento estendido](#extended-thinking) correspondendo o ID do modelo contra padrões conhecidos. IDs específicos do provedor, como ARNs Amazon Bedrock ou nomes de implantação personalizados, geralmente não correspondem a esses padrões, deixando recursos suportados desabilitados. Defina `_SUPPORTED_CAPABILITIES` para informar ao Claude Code quais recursos o modelo realmente suporta:

| Valor de capacidade    | Habilita                                                                                      |
| ---------------------- | --------------------------------------------------------------------------------------------- |
| `effort`               | [Níveis de esforço](#adjust-effort-level) e o comando `/effort`                               |
| `xhigh_effort`         | O nível de esforço `xhigh`                                                                    |
| `max_effort`           | O nível de esforço `max`                                                                      |
| `thinking`             | [Pensamento estendido](#extended-thinking)                                                    |
| `adaptive_thinking`    | Raciocínio adaptativo que aloca dinamicamente o pensamento com base na complexidade da tarefa |
| `interleaved_thinking` | Pensamento entre chamadas de ferramenta                                                       |

Quando `_SUPPORTED_CAPABILITIES` é definido, as capacidades listadas são habilitadas e as capacidades não listadas são desabilitadas para o modelo fixado correspondente. Quando a variável não está definida, Claude Code volta para detecção integrada baseada no ID do modelo.

Este exemplo fixa Opus para um ARN de modelo personalizado Amazon Bedrock, define um nome amigável e declara suas capacidades:

```bash theme={null}
export ANTHROPIC_DEFAULT_OPUS_MODEL='arn:aws:bedrock:us-east-1:123456789012:custom-model/abc'
export ANTHROPIC_DEFAULT_OPUS_MODEL_NAME='Opus via Bedrock'
export ANTHROPIC_DEFAULT_OPUS_MODEL_DESCRIPTION='Opus 4.7 routed through a Bedrock custom endpoint'
export ANTHROPIC_DEFAULT_OPUS_MODEL_SUPPORTED_CAPABILITIES='effort,xhigh_effort,max_effort,thinking,adaptive_thinking,interleaved_thinking'
```

<h3 id="override-model-ids-per-version">
  Substituir IDs de modelo por versão
</h3>

As variáveis de ambiente no nível de família acima configuram um ID de modelo por alias de família. Se você precisar mapear várias versões dentro da mesma família para IDs de provedor distintos, use a configuração `modelOverrides` em vez disso.

`modelOverrides` mapeia IDs de modelo Anthropic individuais para as strings específicas do provedor que Claude Code envia para a API do seu provedor. Quando um usuário seleciona um modelo mapeado no seletor `/model`, Claude Code usa seu valor configurado em vez do padrão integrado.

Isso permite que administradores corporativos roteiem cada versão de modelo para um ARN de perfil de inferência Amazon Bedrock específico, nome de versão Google Cloud's Agent Platform ou nome de implantação Microsoft Foundry para governança, alocação de custos ou roteamento regional.

Defina `modelOverrides` em seu [arquivo de configurações](https://code.claude.com/docs/pt/settings#settings-files):

```json theme={null}
{
  "modelOverrides": {
    "claude-opus-4-7": "arn:aws:bedrock:us-east-2:123456789012:application-inference-profile/opus-prod",
    "claude-opus-4-6": "arn:aws:bedrock:us-east-2:123456789012:application-inference-profile/opus-46-prod",
    "claude-sonnet-4-6": "arn:aws:bedrock:us-east-2:123456789012:application-inference-profile/sonnet-prod"
  }
}
```

As chaves devem ser IDs de modelo Anthropic conforme listado na [Visão geral de modelos](https://platform.claude.com/docs/en/about-claude/models/overview). Para IDs de modelo datados, inclua o sufixo de data exatamente como aparece lá. Chaves desconhecidas são ignoradas.

As substituições substituem os IDs de modelo integrados que suportam cada entrada no seletor `/model`. No Amazon Bedrock, as entradas `modelOverrides` têm precedência sobre qualquer perfil de inferência que Claude Code descobre automaticamente na inicialização. Claude Code passa valores que já são específicos do provedor, como ARNs de perfil de inferência Amazon Bedrock ou nomes de implantação Microsoft Foundry, para o provedor como estão.

As substituições também se aplicam quando você passa um ID de modelo Anthropic diretamente através de `--model`, a variável de ambiente `ANTHROPIC_MODEL`, ou uma variável de ambiente `ANTHROPIC_DEFAULT_*_MODEL`. No Amazon Bedrock, Google Cloud's Agent Platform e [Mantle](https://code.claude.com/docs/pt/amazon-bedrock#use-the-mantle-endpoint), um ID de modelo Anthropic sem entrada `modelOverrides` resolve para o mesmo ID específico do provedor que a linha do seletor `/model` para essa versão, quando o provedor suporta essa versão. Mantle suporta um subconjunto de versões. Para um ID de modelo Anthropic fora desse subconjunto, Claude Code envia o ID bruto para Mantle sem mapeá-lo, a menos que uma entrada `modelOverrides` o cubra. Antes da v2.1.200, `--model` e os valores de variável de ambiente chegavam ao provedor como estavam sem passar pelo mapa de substituição.

`modelOverrides` funciona junto com `availableModels`. A lista de permissões é avaliada contra o ID de modelo Anthropic, não o valor de substituição, então uma entrada como `"opus"` em `availableModels` continua a corresponder mesmo quando versões do Opus são mapeadas para ARNs. Quando `enforceAvailableModels` é definido em configurações gerenciadas, o Padrão imposto é resolvido através de `modelOverrides` apenas da [fonte gerenciada de precedência mais alta](https://code.claude.com/docs/pt/server-managed-settings#settings-precedence). O mapeamento de um administrador, como uma versão fixada para um ARN de perfil de inferência, é honrado no Padrão imposto. Substituições de configurações de usuário ou projeto não o afetam.

Quando `availableModels` é definido em [configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files), apenas `modelOverrides` dessa fonte gerenciada se aplicam a um ID de modelo Anthropic passado diretamente através de `--model` ou das variáveis de ambiente acima. Claude Code ignora substituições em configurações de usuário ou projeto para esses IDs, e nunca resolve um ID que a lista gerenciada exclui através de `modelOverrides` de qualquer fonte de configurações. Essa restrição de fonte gerenciada requer Claude Code v2.1.200 ou posterior. Veja [Restringir seleção de modelo](#restrict-model-selection) para como IDs bloqueados são tratados.

<h3 id="prompt-caching-configuration">
  Configuração de prompt caching
</h3>

Claude Code usa automaticamente [prompt caching](https://code.claude.com/docs/pt/prompt-caching) para otimizar o desempenho e reduzir custos. Você pode desabilitar prompt caching globalmente ou para níveis de modelo específicos:

| Variável de ambiente            | Descrição                                                                                                                |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| `DISABLE_PROMPT_CACHING`        | Defina como `1` para desabilitar prompt caching para todos os modelos. Tem precedência sobre as configurações por modelo |
| `DISABLE_PROMPT_CACHING_HAIKU`  | Defina como `1` para desabilitar prompt caching apenas para modelos Haiku                                                |
| `DISABLE_PROMPT_CACHING_SONNET` | Defina como `1` para desabilitar prompt caching apenas para modelos Sonnet                                               |
| `DISABLE_PROMPT_CACHING_OPUS`   | Defina como `1` para desabilitar prompt caching apenas para modelos Opus                                                 |
| `DISABLE_PROMPT_CACHING_FABLE`  | Defina como `1` para desabilitar prompt caching apenas para modelos Fable                                                |

Para alterar o TTL do cache ou aprender o que dispara uma falha de cache, veja [Como Claude Code usa prompt caching](https://code.claude.com/docs/pt/prompt-caching).
