<!-- Fonte: https://code.claude.com/docs/pt/permission-modes | Baixado em: 2026-08-06 | slug: permission-modes -->

# Escolha um modo de permissão

> Controle se Claude pede permissão antes de editar arquivos ou executar comandos. Alterne modos com Shift+Tab na CLI ou use o seletor de modo no VS Code, Desktop e claude.ai.

Quando Claude quer editar um arquivo, executar um comando shell ou fazer uma solicitação de rede, ele pausa e pede que você aprove a ação. Os modos de permissão controlam com que frequência essa pausa acontece. O modo que você escolhe molda o fluxo de uma sessão: o modo Manual faz você revisar cada ação conforme ela chega, enquanto modos mais flexíveis permitem que Claude trabalhe em trechos mais longos e ininterruptos e relate quando terminar. Escolha mais supervisão para trabalhos sensíveis, ou menos interrupções quando você confia na direção.

<h2 id="available-modes">
  Modos disponíveis
</h2>

Cada modo faz uma compensação diferente entre conveniência e supervisão. A tabela abaixo mostra o que Claude pode fazer sem um prompt de permissão em cada modo.

| Modo                                                                | O que é executado sem perguntar                                                                            | Melhor para                                      |
| :------------------------------------------------------------------ | :--------------------------------------------------------------------------------------------------------- | :----------------------------------------------- |
| `default`                                                           | Apenas leituras                                                                                            | Começar, trabalho sensível                       |
| [`acceptEdits`](#auto-approve-file-edits-with-acceptedits-mode)     | Leituras, edições de arquivo e comandos comuns do sistema de arquivos (`mkdir`, `touch`, `mv`, `cp`, etc.) | Iterando sobre código que você está revisando    |
| [`plan`](#analyze-before-you-edit-with-plan-mode)                   | Apenas leituras                                                                                            | Explorando uma base de código antes de alterá-la |
| [`auto`](#eliminate-prompts-with-auto-mode)                         | Tudo, com verificações de segurança em segundo plano                                                       | Tarefas longas, reduzindo fadiga de prompts      |
| [`dontAsk`](#allow-only-pre-approved-tools-with-dontask-mode)       | Apenas ferramentas pré-aprovadas                                                                           | CI restrito e scripts                            |
| [`bypassPermissions`](#skip-all-checks-with-bypasspermissions-mode) | Tudo                                                                                                       | Apenas contêineres isolados e VMs                |

O modo que revisa cada ação é nomeado **Manual** na CLI, em `claude --help`, nas extensões VS Code e JetBrains, e no aplicativo de desktop. Seu valor de configuração é `default`, que é o que hooks e integrações SDK usam. A CLI aceita `manual` como um alias em qualquer lugar onde você digita o valor, por exemplo `claude --permission-mode manual` ou `"defaultMode": "manual"`. O rótulo Manual e o alias `manual` requerem Claude Code v2.1.200 ou posterior. O rótulo do aplicativo de desktop não depende da sua versão da CLI.

Em todos os modos, exceto `bypassPermissions`, as gravações em [caminhos protegidos](#protected-paths) nunca são auto-aprovadas, protegendo o estado do repositório e a configuração do próprio Claude contra corrupção acidental.

Os modos definem a linha de base. Sobreponha [regras de permissão](https://code.claude.com/docs/pt/permissions#manage-permissions) no topo para pré-aprovar ou bloquear ferramentas específicas. Regras de negação, regras de solicitação explícita, a [configuração `ask` da organização em ferramentas de conector](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) e o marcador [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) se aplicam em todos os modos, incluindo `bypassPermissions`. Regras de permissão não têm efeito nesse modo porque tudo mais já está aprovado.

<h2 id="switch-permission-modes">
  Alternar modos de permissão
</h2>

Você pode alternar modos durante uma sessão, na inicialização ou como padrão persistente. O modo é definido através desses controles, não pedindo ao Claude no chat. Selecione sua interface abaixo para ver como alterá-lo.

<Tabs>
  <Tab title="CLI">
    **Durante uma sessão**: pressione `Shift+Tab` para alternar `default` → `acceptEdits` → `plan`. O modo atual aparece na barra de status. O modo manual, `default` nesse ciclo, mostra um badge cinza `⏸ manual mode on`. Antes da v2.1.203, a barra de status não mostrava nenhum badge no modo Manual.

    Nem todo modo está no ciclo padrão:

    * `auto`: aparece quando sua conta atende aos [requisitos do modo automático](#eliminate-prompts-with-auto-mode); alternar para ele muda os modos sem um prompt de confirmação
    * `bypassPermissions`: aparece depois que você inicia com `--permission-mode bypassPermissions`, `--dangerously-skip-permissions` ou `--allow-dangerously-skip-permissions`; a variante `--allow-` adiciona o modo ao ciclo sem ativá-lo
    * `dontAsk`: nunca aparece no ciclo; defina-o com `--permission-mode dontAsk`

    Os modos opcionais habilitados se encaixam após `plan`, com `bypassPermissions` primeiro e `auto` por último. Se você tiver ambos habilitados, você alternará através de `bypassPermissions` a caminho de `auto`.

    **Na inicialização**: passe o modo como uma flag.

    ```bash theme={null}
    claude --permission-mode plan
    ```

    **Como padrão**: defina `defaultMode` em [settings](https://code.claude.com/docs/pt/settings#settings-files).

    ```json theme={null}
    {
      "permissions": {
        "defaultMode": "acceptEdits"
      }
    }
    ```

    A mesma flag `--permission-mode` funciona com `-p` para [execuções não-interativas](https://code.claude.com/docs/pt/headless).
  </Tab>

  <Tab title="VS Code">
    **Durante uma sessão**: clique no indicador de modo na parte inferior da caixa de prompt.

    **Como padrão**: defina `claudeCode.initialPermissionMode` nas configurações do VS Code, ou use o painel de configurações da extensão Claude Code.

    O indicador de modo mostra esses rótulos, mapeados para o modo que cada um se aplica:

    | Rótulo da UI           | Modo                |
    | :--------------------- | :------------------ |
    | Manual                 | `default`           |
    | Editar automaticamente | `acceptEdits`       |
    | Plan                   | `plan`              |
    | Auto                   | `auto`              |
    | Bypass permissions     | `bypassPermissions` |

    Antes da v2.1.205, a extensão rotulava `plan` como Plan mode e `auto` como Auto mode.

    O modo automático aparece no indicador de modo quando sua conta atende a todos os requisitos listados na [seção de modo automático](#eliminate-prompts-with-auto-mode). A configuração `claudeCode.initialPermissionMode` não aceita `auto`. Para iniciar no modo automático por padrão, defina `defaultMode` em suas [configurações de usuário](https://code.claude.com/docs/pt/settings#settings-files). Claude Code ignora `defaultMode: "auto"` nas configurações de projeto e locais.

    Bypass permissions requer o toggle **Allow dangerously skip permissions** nas configurações da extensão antes de aparecer no indicador de modo.

    Veja o [guia do VS Code](https://code.claude.com/docs/pt/vs-code) para detalhes específicos da extensão.
  </Tab>

  <Tab title="JetBrains">
    O plugin JetBrains executa Claude Code no terminal do IDE, então alternar modos funciona da mesma forma que na CLI: pressione `Shift+Tab` para alternar, ou passe `--permission-mode` ao iniciar.
  </Tab>

  <Tab title="Desktop">
    **Durante uma sessão**: use o seletor de modo ao lado do botão enviar. Nem todo modo aparece no seletor:

    * **Auto**: aparece quando sua conta atende aos [requisitos do modo automático](#eliminate-prompts-with-auto-mode)
    * **Bypass permissions**: requer o toggle **Allow bypass permissions mode** nas configurações do Desktop nos planos Pro e Max; nos planos Team e Enterprise, a política da organização controla isso

    Para detalhes específicos do desktop, veja [Escolher um modo de permissão](https://code.claude.com/docs/pt/desktop#choose-a-permission-mode) no guia do Desktop.

    **Como padrão**: defina `defaultMode` em [settings](https://code.claude.com/docs/pt/settings#settings-files). O aplicativo desktop lê os mesmos arquivos de configurações que a CLI e aplica o modo a novas sessões locais.

    Um modo que você escolhe no seletor de modo é lembrado por pasta e tem precedência sobre `defaultMode` para essa pasta. Plan é a exceção: escolhê-lo se aplica apenas à sessão atual.

    Este exemplo define o modo Plan como padrão para novas sessões locais:

    ```json theme={null}
    {
      "permissions": {
        "defaultMode": "plan"
      }
    }
    ```
  </Tab>

  <Tab title="Web and mobile">
    Use o dropdown de modo ao lado da caixa de prompt em [claude.ai/code](https://claude.ai/code) ou no aplicativo móvel. Prompts de permissão aparecem no claude.ai para aprovação. Quais modos aparecem depende de onde a sessão é executada:

    * **Sessões em nuvem** em [Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web): Accept edits, Plan e Auto. Accept edits corresponde ao modo `default`: o ambiente em nuvem pré-aprova edições de arquivo independentemente do modo, então o dropdown mostra Accept edits em vez de Manual. Sessões em nuvem ainda honram `defaultMode: "acceptEdits"` das configurações. O modo automático aparece apenas quando sua organização o permite e o modelo selecionado o suporta. Bypass permissions não está disponível.
    * **Sessões de [Remote Control](https://code.claude.com/docs/pt/remote-control)** em sua máquina local: Manual, Accept edits e Plan. Você não pode selecionar Auto ou Bypass permissions do aplicativo. O dropdown mostra o modo em que a sessão local está, incluindo um modo definido do terminal, e atualiza quando o modo muda no aplicativo ou no terminal. A única exceção é Bypass permissions: a sessão nunca relata esse modo ao claude.ai, então alternar para ele do terminal não muda o que o dropdown mostra. Antes da v2.1.202, sessões conectadas com `/remote-control` ou `claude --remote-control` não relatavam seu modo, então claude.ai e o aplicativo móvel poderiam mostrar um modo em que a sessão não estava. A incompatibilidade afetava apenas o rótulo: Claude Code gerou prompts de permissão a partir do modo real da sessão, e eles ainda apareciam no aplicativo para aprovação.

    Para Remote Control, você também pode definir o modo inicial ao iniciar o host:

    ```bash theme={null}
    claude remote-control --permission-mode acceptEdits
    ```
  </Tab>
</Tabs>

<h2 id="auto-approve-file-edits-with-acceptedits-mode">
  Auto-aprovar edições de arquivo com o modo acceptEdits
</h2>

O modo `acceptEdits` permite que Claude crie e edite arquivos em seu diretório de trabalho sem solicitar confirmação. A barra de status mostra `⏵⏵ accept edits on` enquanto este modo está ativo.

Além das edições de arquivo, o modo `acceptEdits` auto-aprova comandos Bash comuns do sistema de arquivos: `mkdir`, `touch`, `rm`, `rmdir`, `mv`, `cp` e `sed`. Esses comandos também são auto-aprovados quando prefixados com variáveis de ambiente seguras como `LANG=C` ou `NO_COLOR=1`, ou wrappers de processo como `timeout`, `nice` ou `nohup`. Como as edições de arquivo, a auto-aprovação se aplica apenas a caminhos dentro de seu diretório de trabalho ou `additionalDirectories`. Caminhos fora desse escopo, gravações em [caminhos protegidos](#protected-paths) e todos os outros comandos Bash, exceto o [conjunto integrado somente leitura](https://code.claude.com/docs/pt/permissions#read-only-commands), ainda solicitam confirmação.

Quando a [ferramenta PowerShell](https://code.claude.com/docs/pt/tools-reference#powershell-tool) está habilitada, o modo `acceptEdits` também auto-aprova `Set-Content`, `Add-Content`, `Clear-Content` e `Remove-Item` em caminhos no escopo, junto com seus aliases comuns. As mesmas regras de escopo e caminho protegido se aplicam.

Use `acceptEdits` quando você quiser revisar as alterações em seu editor ou via `git diff` depois, em vez de aprovar cada edição inline.

Pressione `Shift+Tab` uma vez a partir do modo Manual para entrar nele, ou comece com ele diretamente:

```bash theme={null}
claude --permission-mode acceptEdits
```

<h2 id="analyze-before-you-edit-with-plan-mode">
  Analise antes de editar com o modo plan
</h2>

O modo plan instrui Claude a pesquisar e propor alterações sem realizá-las. Claude lê arquivos, executa comandos shell para explorar e escreve um plano, mas não edita sua fonte. Os prompts de permissão se aplicam como fazem no modo Manual, a menos que [modo automático](https://code.claude.com/docs/pt/auto-mode-config) esteja disponível e `useAutoModeDuringPlan` esteja ativado, que é o padrão. Com o modo automático ativo, o classificador aprova comandos somente leitura, como pesquisas e leituras de arquivo, sem solicitar. As edições permanecem bloqueadas de qualquer forma até que você aprove o plano.

Entre no modo plan pressionando `Shift+Tab` ou prefixando um único prompt com `/plan`. Você também pode iniciar no modo plan a partir da CLI:

```bash theme={null}
claude --permission-mode plan
```

Pressione `Shift+Tab` novamente para sair do modo plan sem aprovar um plano.

<h3 id="review-and-approve-a-plan">
  Revise e aprove um plano
</h3>

Quando o plano estiver pronto, Claude o apresenta e pergunta como proceder. A partir desse prompt você pode:

* Aprovar e iniciar no modo automático
* Aprovar e aceitar edições
* Aprovar e revisar cada edição manualmente
* Continuar planejando com feedback
* Refinar com [Ultraplan](https://code.claude.com/docs/pt/ultraplan) para revisão baseada em navegador

Aprovar um plano sai do modo plan e muda a sessão para o modo de permissão que cada opção de aprovação descreve, então Claude começa a editar. Para planejar novamente, volte ao modo plan com `Shift+Tab`, ou prefixe seu próximo prompt com `/plan`.

Pressione `Ctrl+G` para abrir o plano proposto em seu editor de texto padrão e editá-lo diretamente antes de Claude prosseguir. Quando [`showClearContextOnPlanAccept`](https://code.claude.com/docs/pt/settings#available-settings) está ativado, cada opção de aprovação também oferece limpar o contexto de planejamento primeiro.

Aceitar um plano também nomeia a sessão a partir do conteúdo do plano automaticamente, a menos que você já tenha definido um nome com `--name` ou `/rename`.

<h3 id="set-plan-mode-as-the-default">
  Defina o modo plan como padrão
</h3>

Para tornar o modo plan o padrão para um projeto, defina `defaultMode` em `.claude/settings.json`:

```json theme={null}
{
  "permissions": {
    "defaultMode": "plan"
  }
}
```

<h2 id="eliminate-prompts-with-auto-mode">
  Elimine prompts de permissão com modo automático
</h2>

O modo automático permite que Claude execute sem prompts de permissão rotineiros. Um modelo classificador separado revisa as ações antes de serem executadas, bloqueando qualquer coisa que ultrapasse sua solicitação, tenha como alvo infraestrutura não reconhecida ou pareça impulsionada por conteúdo hostil que Claude leu. As [regras de solicitação](https://code.claude.com/docs/pt/permissions#manage-permissions) explícitas ainda forçam um prompt.

Remoções direcionadas ao diretório raiz do sistema de arquivos ou ao diretório home, como `rm -rf /` e `rm -rf ~`, solicitam aprovação em vez de ir para o classificador. Este prompt também é acionado quando o comando contém substituição de comando com `$(...)` ou backticks, ou substituição de processo com `<(...)`, independentemente de a remoção estar dentro da substituição, como em `echo "$(rm -rf ~)"`, ou em outro lugar no mesmo comando. Antes de v2.1.208, comandos contendo essas formas iam para o classificador em vez de solicitar.

O modo automático também incentiva Claude a continuar trabalhando sem parar para fazer perguntas de esclarecimento, embora Claude ainda pergunte quando seu prompt ou uma skill depende explicitamente disso. Para um comportamento mais autônomo mantendo prompts de permissão, defina o [estilo de saída Proativo](https://code.claude.com/docs/pt/output-styles) em vez disso.

<Warning>
  O modo automático reduz prompts de permissão, mas não garante segurança. Use-o para tarefas em que você confia na direção geral, não como substituto para revisão em operações sensíveis.
</Warning>

O modo automático está disponível apenas quando sua conta atende a todos esses requisitos:

* **Plano**: Todos os planos.
* **Proprietário**: em Team e Enterprise, um Proprietário deve habilitá-lo nas [configurações de administrador do Claude Code](https://claude.ai/admin-settings/claude-code) antes que os usuários possam ativá-lo. Os administradores também podem desativar o modo automático definindo `permissions.disableAutoMode` como `"disable"` nas [configurações gerenciadas](https://code.claude.com/docs/pt/permissions#managed-settings). Para a aba Code do aplicativo desktop, `disableAutoMode` é o controle no nível da organização, e o toggle de configurações de administrador não se aplica.
* **Modelo**: na API Anthropic, Claude Opus 4.6 ou posterior, ou Sonnet 4.6 ou posterior. No Amazon Bedrock, na Agent Platform do Google Cloud, no Microsoft Foundry e em sessões do [gateway de aplicativos Claude](https://code.claude.com/docs/pt/claude-apps-gateway) conectadas, apenas Claude Sonnet 5, Opus 4.7 e Opus 4.8. Modelos mais antigos, incluindo Sonnet 4.5, Opus 4.5, Haiku e modelos claude-3, não são suportados em nenhum provedor.
* **Provedor**: disponível por padrão na API Anthropic, Amazon Bedrock, Agent Platform do Google Cloud, Microsoft Foundry e sessões do gateway de aplicativos Claude conectadas. Na v2.1.158 até v2.1.206, o modo automático estava desativado em todos esses provedores, exceto na API Anthropic, até que você definisse `CLAUDE_CODE_ENABLE_AUTO_MODE=1`; v2.1.207 removeu o requisito.

Se Claude Code relatar o modo automático como indisponível, um desses requisitos não foi atendido; isso não é uma interrupção transitória. Uma mensagem separada que nomeia um modelo e diz que o modo automático "não consegue determinar a segurança" de uma ação é uma interrupção transitória do classificador; consulte a [referência de erros](https://code.claude.com/docs/pt/errors#auto-mode-cannot-determine-the-safety-of-an-action).

Se você definir `defaultMode: "auto"` nas [configurações](https://code.claude.com/docs/pt/settings#available-settings) e a sessão começar no modo `default` sem erro, a configuração provavelmente está em `.claude/settings.json` ou `.claude/settings.local.json`. Claude Code v2.1.142 e posterior ignoram `auto` desses arquivos para que um repositório não possa se conceder modo automático. Mova-o para `~/.claude/settings.json`.

<h3 id="enable-auto-mode-on-bedrock-agent-platform-or-foundry">
  Modo automático no Bedrock, Agent Platform ou Foundry
</h3>

No [Amazon Bedrock](https://code.claude.com/docs/pt/amazon-bedrock), na [Agent Platform do Google Cloud](https://code.claude.com/docs/pt/google-vertex-ai), no [Microsoft Foundry](https://code.claude.com/docs/pt/microsoft-foundry) e em sessões do [gateway de aplicativos Claude](https://code.claude.com/docs/pt/claude-apps-gateway) conectadas, o modo automático aparece no ciclo `Shift+Tab` por padrão. Aparecer no ciclo não muda o modo em que uma sessão começa: as sessões ainda começam em seu [`defaultMode`](https://code.claude.com/docs/pt/settings#available-settings), que é Manual a menos que você o altere. Apenas Claude Sonnet 5, Opus 4.7 e Opus 4.8 são suportados nesses provedores.

Para tornar o modo automático o modo de início padrão, defina `"permissions": {"defaultMode": "auto"}` nas configurações de usuário ou gerenciadas.

Para impedir que desenvolvedores usem o modo automático, defina `disableAutoMode` como `"disable"` nas [configurações gerenciadas](https://code.claude.com/docs/pt/permissions#managed-settings). Isso remove `auto` do ciclo `Shift+Tab` e rejeita `--permission-mode auto` na inicialização.

Na v2.1.158 até v2.1.206, o modo automático estava desativado nesses provedores até que você definisse `CLAUDE_CODE_ENABLE_AUTO_MODE=1`, e Claude Code ignorava `defaultMode: "auto"` nesses provedores a menos que a variável também fosse definida. A variável ainda é aceita para compatibilidade e não tem efeito a partir de v2.1.207 em diante.

<h3 id="what-the-classifier-blocks-by-default">
  O que o classificador bloqueia por padrão
</h3>

O classificador confia em seu diretório de trabalho e nos remotes que foram configurados para ele quando a sessão começou. Um remote adicionado ou reorientado durante a sessão com `git remote add` ou `git remote set-url` não é confiável, e tudo mais é tratado como externo até que você [configure infraestrutura confiável](https://code.claude.com/docs/pt/auto-mode-config). Antes de v2.1.200, remotes adicionados no meio da sessão também eram confiáveis.

**Bloqueado por padrão**:

* Download e execução de código, como `curl | bash`
* Envio de dados sensíveis para endpoints externos
* Implantações e migrações de produção
* Exclusão em massa no armazenamento em nuvem
* Concessão de permissões de IAM ou repositório
* Modificação de infraestrutura compartilhada
* Destruição irreversível de arquivos que existiam antes da sessão
* Force push
* Pushing para o branch padrão do repositório quando o push contém conteúdo sensível, como segredos ou dados pessoais ou confiados, contém alterações ocultadas ou mal descritas em relação ao que você pediu, contém conteúdo portado ou lido pela primeira vez de fora do repositório, ou contorna uma pull request, revisão ou verificação que você pediu. Um push simples para o branch padrão não é bloqueado por si só, e limpar um push sinalizado requer nomear o conteúdo sinalizado ou a revisão contornada, não apenas o push. O classificador é uma camada: as [regras `permissions.deny`](https://code.claude.com/docs/pt/permissions#manage-permissions) se aplicam em todos os modos e podem bloquear pushes para o branch padrão completamente, e a proteção de branch do próprio remote ainda se aplica. Antes de v2.1.203, qualquer push direto para o branch padrão era bloqueado
* `git reset --hard`, `git checkout -- .`, `git restore .`, `git clean -fd`, `git stash drop` ou `git stash clear`, que o classificador presume descartaria alterações não confirmadas
* `git commit --amend` quando o commit no HEAD não foi criado nesta sessão
* A partir de v2.1.198, `git commit --amend` quando o commit no HEAD já foi enviado. Uma reword apenas de mensagem não é bloqueada: `--amend -m` sem nada recém-preparado, em um commit que Claude criou durante esta sessão
* `terraform destroy`, `pulumi destroy`, `cdk destroy` ou `terragrunt destroy`, e aplicação de um plano que destrói recursos

Claude Code v2.1.195 e posterior bloqueiam mais categorias por padrão. Várias dependem de entradas de [ambiente](https://code.claude.com/docs/pt/auto-mode-config#define-trusted-infrastructure), como destinos remotos sensíveis e escopos de IaC protegidos, que você pode restringir a nomes concretos.

* Escrita em um gerenciador de segredos, ou alteração de registros DNS ou certificados TLS
* Mesclagem de uma pull request que nenhum humano aprovou, aprovação da própria pull request do Claude ou desabilitação de verificações de CI
* Postagem de um comentário que é em si um comando para automação, como `atlantis apply` ou `/deploy` ou `/merge` de um bot
* Alternância, aumento gradual (ramp) ou exclusão de um sinalizador de recurso de produção
* Aplicação de alterações de infraestrutura a um escopo de IaC protegido, ou drenagem e remoção de nós de cluster
* Escritas em um cluster de computação compartilhado que vão além do recurso que você nomeou, como um seletor de rótulo ou `--all` que captura trabalhos de outros usuários
* Criação de recursos Kubernetes que executam em cada nó ou interceptam tráfego de cluster, como DaemonSets e webhooks de admissão
* Shells interativos ou port-forwards em um destino remoto sensível
* Abertura de um túnel ou shell reverso que torna um serviço local acessível da internet pública
* Impressão de uma credencial ou token ao vivo na transcrição ou em um arquivo
* Acesso a um local listado como local de dados sensíveis em seu [ambiente](https://code.claude.com/docs/pt/auto-mode-config#define-trusted-infrastructure), ou cópia de dados de um. A partir de v2.1.198, isso também bloqueia o envio de dados de um para um público que a entrada exclui
* Roteamento de uma instalação de pacote em torno de seu registro de pacotes interno para um registro público. A partir de v2.1.198, isso também se aplica quando você disse a Claude que um registro interno ou espelho existe na conversa, não apenas quando um está listado em seu ambiente
* Execução de um comando com um sinalizador que desativa uma proteção de segurança, como `--insecure`
* Lançamento de um loop de agente autônomo que executa sem aprovação humana ou sandbox, como um iniciado com `--dangerously-skip-permissions` ou `--no-sandbox`. A partir de v2.1.198, isso também cobre a execução de um agente de terceiros ou harness de avaliação com isolamento e aprovação por ação desabilitados, como um runner iniciado com `--yes-always`
* Ações do [Claude no Chrome](https://code.claude.com/docs/pt/chrome) que poderiam enviar conteúdo da página, cookies ou credenciais fora da origem

Claude Code v2.1.198 e posterior também bloqueiam estes por padrão:

* Exclusão de arquivos em `/tmp`, `$TMPDIR` ou outro diretório compartilhado de rascunho ou cache por wildcard, glob ou filtro de idade em vez de por um caminho nomeado específico
* Inclusão de detalhes sensíveis em conteúdo enviado, carregado, publicado ou escrito para outras pessoas ou sistemas compartilhados, quando sua própria mensagem não autorizou esses detalhes para esse destinatário. Corpos de PR e issue, mensagens de commit e comentários contam como esse tipo de conteúdo de saída quando o repositório está fora do limite de confiança ou é público, incluindo repositórios públicos de sua própria organização; caminhos de arquivo internos, nomes de código, dados de resposta de API ao vivo, como e-mails ou identificadores de conta, e identificadores de infraestrutura contam como detalhes sensíveis. O escopo de PR, issue e mensagem de commit requer Claude Code v2.1.200 ou posterior. Dados pessoais ao vivo de uma resposta de API em um corpo de PR ou issue, como um endereço de e-mail, um identificador de conta ou organização, ou uma métrica de uso, requer que você nomeie esses detalhes e o destinatário independentemente da visibilidade ou limite de confiança do repositório. Essa verificação requer Claude Code v2.1.203 ou posterior
* Envio de pressionamentos de tecla para o próprio painel tmux do Claude Code para conduzir sua própria interface, que o classificador trata como Claude alterando suas próprias permissões ou supervisão

Claude Code v2.1.200 e posterior também bloqueiam estes por padrão:

* Comentário, exclusão ou aprovação forçada de um teste ou asserção que protege comportamento de segurança, como autenticação, controle de acesso, validação de entrada ou sandboxing
* Exclusão ou desmontagem de um recurso com estado que Claude não criou na sessão, quando nenhuma regra de exclusão mais específica se aplica e você não nomeou esse recurso
* Reorientação de uma URL de base de API, endpoint de proxy, receptor de webhook ou espelho de registro em um host de terceiros que não se encaixa na tarefa, incluindo em arquivos de exemplo como `.env.example`
* Alteração de para onde os pushes vão com `git remote set-url` ou `git remote add`, a menos que você tenha nomeado o novo remote
* Pushing de segredos ou dados pessoais ou confiados para um repositório conhecido como público, ou pushing de material confidencial lá que não faz parte do próprio trabalho desse repositório. O próprio assunto de um repositório de dotfiles é a única exceção para dados pessoais ou confiados, e conteúdo de um repositório privado chegando a qualquer superfície pública é bloqueado da mesma forma; ambos os refinamentos requerem Claude Code v2.1.203 ou posterior. Antes de v2.1.203, dados pessoais eram agrupados com material confidencial e bloqueados apenas quando não faziam parte do próprio trabalho desse repositório. Quando a visibilidade de um repositório não é estabelecida, o classificador não bloqueia apenas nisso; ele julga o conteúdo contra as outras regras em vez disso
* Abertura de uma pull request contra um repositório ou organização diferente, fork com `gh repo fork` ou pushing para um repositório de terceiros, a menos que você tenha nomeado esse alvo externo

Claude Code v2.1.203 e posterior também bloqueiam estes por padrão:

* Conteúdo de um armazenamento local sensível, ou de um arquivo cujo nome, caminho ou tipo o marca como sensível, entrando em um commit, um push, texto de PR ou issue, um gist ou paste, ou um package publish, a menos que você tenha nomeado tanto a origem quanto o destino. Transcrições de sessão e logs de conversa, pastas com ponto de credenciais e configuração, como chaves SSH, credenciais em nuvem, perfis de navegador e histórico de shell, e exportações de dados do usuário contam, e o repositório ser privado não o limpa

Claude Code v2.1.205 e posterior também bloqueiam estes por padrão:

* Escrita em transcrições de sessão do Claude Code, os arquivos de histórico `.jsonl` sob `~/.claude/projects/` ou seu diretório de configuração configurado, seja diretamente ou através de um comando de shell. A regra também cobre as linhas de metadados que Claude Code acrescenta a cada entrada de transcrição para suas próprias verificações. Uma transcrição é estado de sessão que Claude Code escreve, não um arquivo de trabalho, e uma entrada adulterada atinge cada verificação posterior uma vez que você retoma a sessão, então o modo automático bloqueia essas escritas como defesa em profundidade. Ler uma transcrição não é bloqueado
* Uma exclusão forçada recursiva, como `rm -rf "$VAR"` ou `Remove-Item -Recurse -Force $dir` cujo alvo é uma variável de shell, ou um glob enraizado em uma, que não é atribuído em nenhum lugar na conversa que o classificador vê. O valor veio apenas da saída de comando anterior, que o classificador nunca recebe, então o classificador não pode verificar o alvo de exclusão contra as outras regras de exclusão. O classificador lê a conversa em vez da saída de comando por design, então bloqueia a chamada em vez de adivinhar o alvo. O bloqueio é limpo quando você nomeia o caminho exato sendo deletado, ou quando Claude re-executa a exclusão com o caminho literal resolvido escrito no comando. Exclusões cujo alvo o classificador pode resolver não são afetadas

**Permitido por padrão**:

* Operações de arquivo local em seu diretório de trabalho
* Instalação de dependências declaradas em seus arquivos de lock ou manifestos
* Leitura de `.env` e envio de credenciais para sua API correspondente
* Solicitações HTTP somente leitura
* Pushing para o branch em que você começou ou um que Claude criou
* Pushes rotineiros para o branch padrão do repositório. Antes de v2.1.203, qualquer push direto para o branch padrão era bloqueado

Claude Code v2.1.195 e posterior também permitem estes por padrão:

* Exclusão dos trabalhos exatos que Claude criou anteriormente na mesma sessão
* Leitura, revisão ou escrita de código, configs e modelos de ameaça relacionados à segurança como parte de sua tarefa
* Mensagens entre agentes trabalhando juntos na mesma sessão multi-agente
* Envio de dados para os domínios confiáveis, buckets e serviços que você lista em [`environment`](https://code.claude.com/docs/pt/auto-mode-config#define-trusted-infrastructure). Isso cobre apenas fluxo de dados, não operações destrutivas ou de credencial na mesma infraestrutura
* [Claude no Chrome](https://code.claude.com/docs/pt/chrome) navegação para um domínio interno confiável, localhost ou uma URL que você nomeou

As solicitações de acesso à rede do sandbox são roteadas através do classificador em vez de serem permitidas por padrão. {{/* min-version: 2.1.198 */}}A partir de v2.1.198, o classificador reutiliza seu veredicto para um host e porta de rede em vez de re-executar em cada conexão:

* Um allow é reutilizado até que novo conteúdo entre na conversa, ponto em que esse host é verificado novamente
* Na CLI interativa, um deny é descartado quando o turno termina
* No [modo não interativo](https://code.claude.com/docs/pt/headless) e sessões do Agent SDK não há limite de turno, então um deny é reutilizado para o resto da execução
* Alterar seu modo de permissão ou regras descarta todos os veredictos em cache

Execute `claude auto-mode defaults` para ver as listas de regras completas. Se ações rotineiras forem bloqueadas, um administrador pode adicionar repos, buckets e serviços confiáveis através da configuração `autoMode.environment`: consulte [Configurar modo automático](https://code.claude.com/docs/pt/auto-mode-config).

Pushing para seu branch de trabalho, fazendo um push rotineiro para o branch padrão do repositório, e criando uma pull request que corresponde à sua solicitação são executados sem um prompt. O classificador bloqueia um push apenas quando ele carrega risco, como um force push ou conteúdo que contorna uma revisão que você configurou. Para exigir um checkpoint humano antes dessas ações enquanto permanece no modo automático, adicione regras `permissions.ask`: consulte [Limites comuns](https://code.claude.com/docs/pt/auto-mode-config#common-boundaries).

<h3 id="boundaries-you-state-in-conversation">
  Limites que você declara na conversa
</h3>

O classificador trata os limites que você declara na conversa como um sinal de bloqueio. Se você disser a Claude "não faça push" ou "espere até que eu revise antes de implantar", o classificador bloqueia ações correspondentes mesmo quando as regras padrão as permitiriam. Um limite permanece em vigor até que você o levante em uma mensagem posterior. O próprio julgamento do Claude de que uma condição foi atendida não o levanta.

Os limites não são armazenados como regras. O classificador os relê da transcrição em cada verificação, então um limite pode ser perdido se a [compactação de contexto](https://code.claude.com/docs/pt/costs#reduce-token-usage) remover a mensagem que o declarou. Para uma garantia rígida, adicione uma [regra de negação](https://code.claude.com/docs/pt/permissions#permission-rule-syntax) em vez disso.

<h3 id="when-auto-mode-falls-back">
  Quando o modo automático volta
</h3>

Cada ação negada mostra uma notificação e aparece em `/permissions` na aba Recently denied, onde você pode pressionar `r` para tentar novamente com uma aprovação manual.

Se o classificador bloquear uma ação 3 vezes seguidas ou 20 vezes no total, o modo automático pausa e Claude Code retoma o prompt. Aprovar a ação solicitada retoma o modo automático. Esses limites não são configuráveis. Qualquer ação permitida redefine o contador consecutivo, enquanto o contador total persiste para a sessão e redefine apenas quando seu próprio limite dispara um fallback.

No [modo não interativo](https://code.claude.com/docs/pt/headless) com a flag `-p`, bloqueios repetidos abortam a sessão, pois não há usuário para solicitar.

Bloqueios repetidos geralmente significam que o classificador está perdendo contexto sobre sua infraestrutura. Use `/feedback` para relatar falsos positivos, ou peça a um administrador para [configurar infraestrutura confiável](https://code.claude.com/docs/pt/auto-mode-config).

<AccordionGroup>
  <Accordion title="Como o classificador avalia ações">
    Cada ação passa por uma ordem de decisão fixa. O primeiro passo correspondente vence:

    1. Ações correspondentes a suas [regras de permissão, solicitação ou negação](https://code.claude.com/docs/pt/permissions#manage-permissions) resolvem imediatamente. Escritas em [caminhos protegidos](#protected-paths) são roteadas para o classificador mesmo quando uma regra de permissão corresponde. Ferramentas de conector [que sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) e ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) o solicitam diretamente mesmo quando uma regra de permissão corresponde. Regras de solicitação com escopo de conteúdo voltam para um prompt de permissão
    2. Ações somente leitura e edições de arquivo em seu diretório de trabalho são auto-aprovadas, exceto escritas em [caminhos protegidos](#protected-paths)
    3. Tudo mais vai para o classificador. Uma ferramenta de conector [que sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) pula o classificador e o solicita diretamente, então uma aprovação exigida pela organização nunca é auto-aprovada. {{/* min-version: 2.1.199 */}}A partir de v2.1.199, uma ferramenta MCP marcada com [`_meta["anthropic/requiresUserInteraction"]`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) também pula o classificador e o solicita diretamente, então uma etapa de consentimento nunca é auto-aprovada em nome do autor da ferramenta
    4. Se o classificador bloquear, Claude recebe o motivo e tenta uma alternativa

    Ao entrar no modo automático, regras de permissão amplas que concedem execução de código arbitrária são descartadas:

    * Blanket `Bash(*)` ou `PowerShell(*)`
    * Intérpretes com wildcard como `Bash(python*)`
    * Comandos de execução do gerenciador de pacotes
    * Regras de permissão `Agent`

    Regras estreitas como `Bash(npm test)` são mantidas. Regras descartadas são restauradas quando você sai do modo automático.

    O classificador vê mensagens de usuário, chamadas de ferramenta e conteúdo de seu CLAUDE.md. Os resultados da ferramenta são removidos, então conteúdo hostil em um arquivo ou página da web não pode manipulá-lo diretamente. Uma sonda separada do lado do servidor verifica os resultados da ferramenta recebidos e sinaliza conteúdo suspeito antes que Claude o leia. Para mais sobre como essas camadas funcionam juntas, consulte o [anúncio do modo automático](https://claude.com/blog/auto-mode) e o [aprofundamento de engenharia](https://www.anthropic.com/engineering/claude-code-auto-mode).
  </Accordion>

  <Accordion title="Como o modo automático lida com subagentes">
    O classificador verifica o trabalho do [subagente](https://code.claude.com/docs/pt/sub-agents) em três pontos:

    1. Antes de um subagente começar, a descrição da tarefa delegada é avaliada, então uma tarefa que parece perigosa é bloqueada no tempo de spawn.
    2. Enquanto o subagente executa, cada uma de suas ações passa pelo classificador com as mesmas regras que a sessão pai, e qualquer `permissionMode` no frontmatter do subagente é ignorado.
    3. Quando o subagente termina, o classificador revisa seu histórico de ação completo; se essa verificação de retorno sinalizar uma preocupação, um aviso de segurança é adicionado aos resultados do subagente.

    {{/* min-version: 2.1.178 */}}

    A etapa 1 requer Claude Code v2.1.178 ou posterior. Versões anteriores aplicavam o classificador nas etapas 2 e 3, mas não avaliavam a descrição da tarefa antes do subagente começar.
  </Accordion>

  <Accordion title="Custo e latência">
    O classificador executa em um modelo configurado pelo servidor que é independente de sua seleção de `/model`, então alternar modelos não muda a disponibilidade do classificador. As chamadas do classificador contam para seu uso de token. Cada verificação envia uma porção da transcrição mais a ação pendente, adicionando uma volta antes da execução. Leituras e edições de diretório de trabalho fora de caminhos protegidos pulam o classificador, então a sobrecarga vem principalmente de comandos de shell e operações de rede. {{/* min-version: 2.1.198 */}}A partir de v2.1.198, um veredicto de rede de sandbox para um host e porta é reutilizado em vez de re-classificado em cada conexão, então conexões repetidas ao mesmo host não adicionam cada uma uma verificação. [O que o classificador bloqueia por padrão](#what-the-classifier-blocks-by-default) descreve quanto tempo um allow e um deny duram.
  </Accordion>
</AccordionGroup>

<h2 id="allow-only-pre-approved-tools-with-dontask-mode">
  Permitir apenas ferramentas pré-aprovadas com modo dontAsk
</h2>

Se você definir o modo `dontAsk`, Claude Code nega automaticamente toda chamada de ferramenta que de outra forma solicitaria confirmação. Claude executa apenas ações que correspondem às suas regras `permissions.allow`, [comandos Bash somente leitura](https://code.claude.com/docs/pt/permissions#read-only-commands) e chamadas aprovadas por um [hook PreToolUse](https://code.claude.com/docs/pt/permissions#extend-permissions-with-hooks). Use este modo para pipelines de CI ou ambientes restritos onde você pré-define exatamente o que Claude pode fazer; a sessão nunca aguarda entrada. A barra de status mostra `⏵⏵ don't ask on` enquanto este modo está ativo.

Claude Code nega chamadas que correspondem às suas regras explícitas de [`ask`](https://code.claude.com/docs/pt/permissions#manage-permissions) em vez de solicitar confirmação. Também nega a ferramenta integrada `AskUserQuestion` e ferramentas de conector [que sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools), mesmo que suas regras de permissão correspondam a elas. Nega ferramentas MCP marcadas com [`_meta["anthropic/requiresUserInteraction"]`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) da mesma forma, porque seu cartão de aprovação precisa de uma resposta que este modo nunca coleta; isso requer Claude Code v2.1.199 ou posterior.

Sessões em nuvem no [Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web) ignoram `defaultMode: "dontAsk"`; consulte [bypassPermissions](#skip-all-checks-with-bypasspermissions-mode) para detalhes.

Defina-o na inicialização com a flag:

```bash theme={null}
claude --permission-mode dontAsk
```

<h2 id="skip-all-checks-with-bypasspermissions-mode">
  Ignorar todas as verificações com o modo bypassPermissions
</h2>

O modo `bypassPermissions` desativa prompts de permissão e verificações de segurança para que as chamadas de ferramentas sejam executadas imediatamente, incluindo escritas em [caminhos protegidos](#protected-paths). Antes da v2.1.126, escritas em caminhos protegidos ainda solicitavam neste modo.

As [regras ask](https://code.claude.com/docs/pt/permissions#manage-permissions) explícitas e ferramentas de conector [que sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) ainda forçam um prompt neste modo. Ferramentas MCP marcadas com [`_meta["anthropic/requiresUserInteraction"]`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) também ainda solicitam; isso requer Claude Code v2.1.199 ou posterior.

Remoções direcionadas à raiz do sistema de arquivos ou diretório inicial, como `rm -rf /` e `rm -rf ~`, ainda solicitam como um disjuntor contra erros do modelo. O disjuntor também é acionado quando o comando contém substituição de comando com `$(...)` ou backticks, ou substituição de processo com `<(...)`, independentemente de a remoção estar dentro da substituição, como em `echo "$(rm -rf ~)"`, ou em outro lugar no mesmo comando. A forma simples, digitada como seu próprio comando, solicitou neste modo desde que o disjuntor foi introduzido; antes da v2.1.208, comandos contendo essas formas não solicitavam.

<Warning>
  Use este modo apenas em ambientes isolados como contêineres, VMs ou dev containers sem acesso à internet, onde Claude Code não pode danificar seu sistema host.
</Warning>

Você não pode entrar em `bypassPermissions` a partir de uma sessão que foi iniciada sem um dos sinalizadores de habilitação; reinicie com um para habilitá-lo:

```bash theme={null}
claude --permission-mode bypassPermissions
```

O sinalizador `--dangerously-skip-permissions` é equivalente.

No Linux e macOS, Claude Code recusa iniciar neste modo quando executado como root ou sob `sudo`:

```text theme={null}
--dangerously-skip-permissions cannot be used with root/sudo privileges for security reasons
```

A verificação é ignorada automaticamente dentro de uma sandbox reconhecida. Para executar autonomamente em um contêiner, use a configuração [dev container](https://code.claude.com/docs/pt/devcontainer), que executa Claude Code como um usuário não-root.

[Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web) não honra `defaultMode: "bypassPermissions"` ou `"dontAsk"` de seus arquivos de configuração, portanto as configurações verificadas de um repositório não podem iniciar uma sessão na nuvem no modo bypass-permissions. A configuração é ignorada silenciosamente e a sessão inicia no modo mostrado no dropdown de modo. Consulte [Alternar modos de permissão](#switch-permission-modes) para saber quais modos as sessões na nuvem oferecem.

<Warning>
  `bypassPermissions` não oferece proteção contra injeção de prompt ou ações não intencionais. Para verificações de segurança em segundo plano com muito menos prompts de permissão, use [modo automático](#eliminate-prompts-with-auto-mode) em vez disso. Administradores podem bloquear este modo definindo `permissions.disableBypassPermissionsMode` como `"disable"` em [configurações gerenciadas](https://code.claude.com/docs/pt/permissions#managed-settings).
</Warning>

<h2 id="protected-paths">
  Caminhos protegidos
</h2>

As gravações em um pequeno conjunto de caminhos nunca são aprovadas automaticamente, em todos os modos, exceto `bypassPermissions`. Isso evita corrupção acidental do estado do repositório e da configuração própria do Claude.

| Modo                             | Gravações em caminhos protegidos |
| :------------------------------- | :------------------------------- |
| `default`, `acceptEdits`, `plan` | Solicitado                       |
| `auto`                           | Roteado para o classificador     |
| `dontAsk`                        | Negado                           |
| `bypassPermissions`              | Permitido                        |

As regras [`permissions.allow`](https://code.claude.com/docs/pt/permissions#manage-permissions) em arquivos de configuração não pré-aprovam gravações em caminhos protegidos. A verificação de segurança é executada antes de Claude Code avaliar as regras de permissão dos arquivos de configuração, portanto, uma entrada como `Edit(.claude/**)` em `~/.claude/settings.json` ou `.claude/settings.json` não altera o resultado por modo na tabela acima. Nos modos que solicitam, o prompt para uma gravação em `.claude/` oferece **Sim, e permitir que Claude edite suas próprias configurações para esta sessão**, o que aprova gravações posteriores em `.claude/` nessa sessão sem solicitar novamente.

Diretórios protegidos:

* `.git`
* `.config/git`
* `.vscode`
* `.idea`
* `.husky`
* `.cargo`
* `.devcontainer`
* `.yarn`
* `.mvn`
* `.claude`, exceto por `.claude/worktrees` onde Claude armazena seus próprios git worktrees

Arquivos protegidos:

* `.gitconfig`, `.gitmodules`
* `.bashrc`, `.bash_profile`, `.bash_login`, `.bash_aliases`, `.bash_logout`, `.zshrc`, `.zprofile`, `.zshenv`, `.zlogin`, `.zlogout`, `.profile`, `.envrc`
* `.npmrc`, `.yarnrc`, `.yarnrc.yml`, `.pnp.cjs`, `.pnp.loader.mjs`, `.pnpmfile.cjs`, `bunfig.toml`, `.bunfig.toml`
* `.bazelrc`, `.bazelversion`, `.bazeliskrc`
* `.pre-commit-config.yaml`, `lefthook.yml`, `lefthook.yaml`, `.lefthook.yml`, `.lefthook.yaml`
* `gradle-wrapper.properties`, `maven-wrapper.properties`
* `.devcontainer.json`
* `.ripgreprc`, `pyrightconfig.json`
* `.mcp.json`, `.claude.json`

<h2 id="see-also">
  Veja também
</h2>

* [Permissões](https://code.claude.com/docs/pt/permissions): regras de permitir, perguntar e negar; políticas gerenciadas
* [Configurar modo automático](https://code.claude.com/docs/pt/auto-mode-config): informe ao classificador qual infraestrutura sua organização confia
* [Hooks](https://code.claude.com/docs/pt/hooks): lógica de permissão personalizada via hooks `PreToolUse` e `PermissionRequest`
* [Ultraplan](https://code.claude.com/docs/pt/ultraplan): execute o modo plan em uma sessão Claude Code na web com revisão baseada em navegador
* [Segurança](https://code.claude.com/docs/pt/security): salvaguardas e melhores práticas
* [Sandboxing](https://code.claude.com/docs/pt/sandboxing): isolamento de sistema de arquivos e rede para comandos Bash
* [Modo não interativo](https://code.claude.com/docs/pt/headless): execute Claude Code com a flag `-p`
