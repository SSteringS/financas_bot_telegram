<!-- Fonte: https://code.claude.com/docs/pt/auto-mode-config | Baixado em: 2026-08-06 | slug: auto-mode-config -->

# Configurar modo automático

> Diga ao classificador do modo automático quais repositórios, buckets e domínios sua organização confia. Defina o contexto do ambiente, substitua as regras de bloqueio e permissão padrão e inspecione sua configuração efetiva com os subcomandos da CLI do modo automático.

[Modo automático](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode) permite que Claude Code seja executado sem prompts de permissão rotineiros, roteando chamadas de ferramentas através de um classificador que bloqueia qualquer coisa irreversível, destrutiva ou direcionada para fora do seu ambiente. Regras de negação e solicitação explícita são avaliadas antes do classificador e ainda bloqueiam ou solicitam. Use o bloco de configurações `autoMode` para dizer ao classificador quais repositórios, buckets e domínios sua organização confia, para que ele pare de bloquear operações internas rotineiras.

<Note>
  Modo automático está disponível para todos os usuários em todos os provedores, incluindo a API Anthropic, Amazon Bedrock, Agent Platform do Google Cloud, Microsoft Foundry e sessões do [gateway de aplicativos Claude](https://code.claude.com/docs/pt/claude-apps-gateway) conectadas. Se Claude Code relatar que o modo automático não está disponível para sua conta, verifique os [requisitos completos](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode), que também cobrem os modelos suportados e a habilitação do Proprietário em planos Team e Enterprise. Nas versões v2.1.158 a v2.1.206, o modo automático no Amazon Bedrock, Agent Platform do Google Cloud, Microsoft Foundry e sessões do gateway de aplicativos Claude exigiam a definição de `CLAUDE_CODE_ENABLE_AUTO_MODE=1`; v2.1.207 removeu o requisito.
</Note>

Por padrão, o classificador confia apenas no diretório de trabalho e nos remotos configurados do repositório atual. Ações como enviar para a organização de controle de fonte da sua empresa ou escrever em um bucket de nuvem da equipe são bloqueadas até que você as adicione a `autoMode.environment`.

Para saber como ativar o modo automático e o que ele bloqueia por padrão, consulte [Modos de permissão](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode). Esta página é a referência de configuração.

Esta página cobre como:

* [Adicionar um checkpoint humano](#common-boundaries) para pushes e pull requests com `permissions.ask`
* [Escolher onde definir regras](#where-the-classifier-reads-configuration) em CLAUDE.md, configurações do usuário e configurações gerenciadas
* [Definir infraestrutura confiável](#define-trusted-infrastructure) com `autoMode.environment`
* [Substituir as regras de bloqueio e permissão](#override-the-block-and-allow-rules) quando os padrões não se adequam ao seu pipeline
* [Rotear todos os comandos shell através do classificador](#route-all-shell-commands-through-the-classifier) com `autoMode.classifyAllShell`
* [Inspecionar sua configuração efetiva](#inspect-the-defaults-and-your-effective-config) com os subcomandos `claude auto-mode`
* [Revisar negações](#review-denials) para saber o que adicionar a seguir

<h2 id="common-boundaries">
  Limites comuns
</h2>

O modo automático permite pushes para sua branch de trabalho, pushes rotineiros para a branch padrão do repositório e criação de pull request por padrão. O classificador bloqueia um push apenas quando ele apresenta risco, como um force push ou conteúdo que contorna uma revisão que você configurou. Se você quiser um checkpoint humano antes de cada push ou pull request, adicione regras de permissão: as receitas abaixo mantêm o modo automático ativado para tudo mais.

O mecanismo mais direto é [`permissions.ask`](https://code.claude.com/docs/pt/permissions#permission-rule-syntax). Regras ask com escopo de conteúdo como as abaixo são avaliadas antes do classificador e sempre forçam um prompt de permissão, mesmo em modo automático, porque uma regra ask explícita é sua intenção declarada de ser solicitado para essa ação. Adicione as regras em suas [settings](https://code.claude.com/docs/pt/settings#settings-files):

```json theme={null}
{
  "permissions": {
    "ask": [
      "Bash(git push *)",
      "Bash(gh pr create *)"
    ]
  }
}
```

Escolha o mecanismo que corresponde ao quão firme o limite precisa ser:

| Limite                        | Mecanismo                                                | Comportamento em modo automático                                                                                                                                                                                                   |
| :---------------------------- | :------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Solicitar antes da ação       | `permissions.ask`                                        | Sempre solicita para regras com escopo de conteúdo como a receita acima. O classificador não pode aprovar automaticamente uma ação correspondente.                                                                                 |
| Nunca executar a ação         | `permissions.deny`                                       | Bloqueia antes do classificador ser consultado. Nem o classificador nem a intenção do usuário podem substituí-lo.                                                                                                                  |
| Limite único para esta sessão | Declare na conversa, como "não faça push até eu revisar" | O classificador bloqueia ações correspondentes, mas o limite pode ser perdido se a [compactação de contexto](https://code.claude.com/docs/pt/costs#reduce-token-usage) remover a mensagem que o declarou. Use uma regra ask ou deny para uma garantia durável. |

<h2 id="where-the-classifier-reads-configuration">
  Onde o classificador lê a configuração
</h2>

O classificador lê o mesmo conteúdo [CLAUDE.md](https://code.claude.com/docs/pt/memory) que o próprio Claude carrega, portanto uma instrução como "nunca force push" no CLAUDE.md do seu projeto orienta tanto Claude quanto o classificador ao mesmo tempo. Comece lá para convenções de projeto e regras de comportamento.

Para regras que se aplicam em todos os projetos, como infraestrutura confiável ou regras de negação em toda a organização, use o bloco de configurações `autoMode`. O classificador lê `autoMode` dos seguintes escopos:

| Escopo                         | Arquivo                                                  | Use para                                                           |
| :----------------------------- | :------------------------------------------------------- | :----------------------------------------------------------------- |
| Um desenvolvedor               | `~/.claude/settings.json`                                | Infraestrutura confiável pessoal                                   |
| Em toda a organização          | [Configurações gerenciadas](https://code.claude.com/docs/pt/server-managed-settings) | Infraestrutura confiável distribuída para todos os desenvolvedores |
| Flag `--settings` ou Agent SDK | JSON inline                                              | Substituições por invocação para automação                         |

O classificador não lê `autoMode` das configurações do projeto em `.claude/settings.json` ou `.claude/settings.local.json`. Ambos os arquivos residem no diretório do repositório, portanto um repositório verificado ou uma etapa de compilação poderia injetar suas próprias regras de permissão. Antes da v2.1.207, o classificador também lia `.claude/settings.local.json`; mova qualquer bloco `autoMode` nesse arquivo para `~/.claude/settings.json`. Excluir `.claude/settings.local.json` também fecha o caso em que um repositório confirma o arquivo ou uma ferramenta local ou etapa de compilação o escreve.

As entradas de cada escopo são combinadas. Um desenvolvedor pode estender `environment`, `allow`, `soft_deny` e `hard_deny` com entradas pessoais, mas não pode remover entradas que as configurações gerenciadas fornecem. Como as regras de permissão atuam como exceções às regras de bloqueio suave dentro do classificador, uma entrada `allow` adicionada pelo desenvolvedor pode substituir uma entrada `soft_deny` da organização: a combinação é aditiva, não um limite de política rígida.

<Note>
  O classificador é um segundo portão que é executado após o [sistema de permissões](https://code.claude.com/docs/pt/permissions). Para ações que nunca devem ser executadas independentemente da intenção do usuário ou da configuração do classificador, use `permissions.deny` nas configurações gerenciadas, que bloqueia a ação antes do classificador ser consultado e não pode ser substituída.
</Note>

<h2 id="define-trusted-infrastructure">
  Definir infraestrutura confiável
</h2>

Para a maioria das organizações, `autoMode.environment` é o único campo que você precisa definir. Ele diz ao classificador quais repositórios, buckets e domínios são confiáveis: o classificador o usa para decidir o que significa "externo", portanto qualquer destino não listado é um alvo potencial de exfiltração.

A partir do Claude Code v2.1.198, `claude auto-mode defaults` imprime três tipos de entrada de ambiente. Versões anteriores a v2.1.195 imprimem apenas os primeiros cinco slots de confiança.

* **Slots de contexto**: descrevem sua organização, stack e postura de segurança para que o classificador leia as outras regras em seu contexto. Ao contrário dos outros dois tipos, os slots de contexto não têm regras próprias que os direcionem. Cada um padrão para `None configured` ou para a suposição conservadora nomeada ao lado:
  * **Organização**
  * **Uso principal do Claude Code**: padrão para desenvolvimento de software
  * **Provedor(es) de nuvem**
  * **Visibilidade do repositório**: um repositório é assumido como privado a menos que seu host remoto e nome indiquem o contrário, ou uma verificação de visibilidade anterior na conversa que o classificador lê mostre que é público. O classificador lê suas mensagens e os comandos que Claude executa, não sua saída, portanto a evidência tem que ser algo que ele possa ler, como sua própria mensagem nomeando o repositório como público; a saída de um `gh repo view` por si só não o alcança. A verificação de evidência de transcrição requer Claude Code v2.1.200 ou posterior
  * **Compartilhamento interno / hospedagem de snippet**: serviços públicos de paste e gist são tratados como fora do limite de confiança até que você nomeie um
  * **CLIs específicas da organização**
  * **Gerenciamento de segredos**
  * **Branches padrão / protegidos**: `main` e `master` são tratados como protegidos até que você nomeie outros
  * **Alvos de implantação de CI/CD**
  * **Postura de rede**
  * **Namespaces / ambientes de implantação protegidos**: volta para a heurística de alvos remotos sensíveis até que você nomeie eles
  * **Retenção de dados / desclassificação**
* **Slots de confiança**: nomeiam o que o classificador trata como dentro de seu limite. Os slots são Repositório confiável, Controle de código-fonte, Domínios internos confiáveis, Buckets de nuvem confiáveis, Serviços internos principais e Registro de pacotes interno. As entradas de repositório e controle de código-fonte padrão para o repositório de trabalho e seus remotes configurados. Todos os outros slots de confiança padrão para `None configured`, portanto nada mais é confiável até que você o adicione. A visibilidade de um repositório abrange apenas material confidencial: um repositório privado é um destino aceitável para material confidencial, mas tornar um repositório privado nunca limpa segredos ou dados pessoais ou confiados nele, e o classificador trata o conteúdo portado, repontado ou lido pela primeira vez de fora do repositório de trabalho como não sendo trabalho daquele repositório. Este escopo requer Claude Code v2.1.203 ou posterior.
* **Slots de sensibilidade**: nomeiam o que as regras de proteção tratam como alto risco. Os slots são Locais de dados sensíveis e públicos, Alvos remotos sensíveis e Escopos de IaC protegidos. Cada um padrão para uma heurística ampla, como tratar qualquer host ou namespace cujo nome carrega `prod` ou `production` como um alvo remoto sensível, portanto as regras de proteção estão ativas antes de você configurar qualquer coisa. Nomear alvos concretos em um slot de sensibilidade faz com que essas regras se apliquem aos alvos nomeados em vez da heurística.

Para adicionar suas próprias entradas junto com os padrões, inclua a string literal `"$defaults"` no array. As entradas padrão são inseridas nessa posição, portanto suas entradas personalizadas podem vir antes ou depois delas.

O exemplo a seguir mantém as entradas padrão e adiciona repositórios, buckets, domínios e serviços de uma organização.

```json theme={null}
{
  "autoMode": {
    "environment": [
      "$defaults",
      "Source control: github.example.com/acme-corp and all repos under it",
      "Trusted cloud buckets: s3://acme-build-artifacts, gs://acme-ml-datasets",
      "Trusted internal domains: *.corp.example.com, api.internal.example.com",
      "Key internal services: Jenkins at ci.example.com, Artifactory at artifacts.example.com"
    ]
  }
}
```

As entradas são prosa, não regex ou padrões de ferramenta. O classificador as lê como regras em linguagem natural. Escreva-as da forma como você descreveria sua infraestrutura para um novo engenheiro. Uma seção de ambiente completa cobre:

* **Organização**: o nome da sua empresa e para que Claude Code é usado principalmente, como desenvolvimento de software, automação de infraestrutura ou engenharia de dados
* **Controle de código-fonte**: todas as organizações GitHub, GitLab ou Bitbucket para as quais seus desenvolvedores fazem push
* **Provedores de nuvem e buckets confiáveis**: nomes de buckets ou prefixos dos quais Claude deve ser capaz de ler e escrever
* **Domínios internos confiáveis**: nomes de host para APIs, painéis e serviços dentro de sua rede, como `*.internal.example.com`
* **Serviços internos principais**: CI, registros de artefatos, índices de pacotes internos, ferramentas de incidentes
* **Registro de pacotes interno**: o registro npm, PyPI ou outro privado através do qual as instalações devem ser roteadas, portanto as instalações que o contornam para um registro público são bloqueadas
* **Locais de dados sensíveis e públicos**: os buckets, bancos de dados ou caminhos que contêm dados pessoais, dados comerciais confidenciais, credenciais, dados regulados ou material similarmente sensível, e os públicos com os quais os dados em cada local podem ser compartilhados, para que o classificador proteja esses locais em vez de adivinhar pelo conteúdo. Claude Code v2.1.195 através de v2.1.197 nomeiam esta entrada Locais de PII / dados regulados e cobrem apenas locais que contêm dados pessoais ou regulados, sem a dimensão de público
* **Alvos remotos sensíveis**: os namespaces, hosts ou contêineres que contam como produção, portanto shells remotos e port-forwards para eles precisam de sua aprovação explícita
* **Escopos de IaC protegidos**: os recursos de infraestrutura cuja aplicação ou destruição sempre devem exigir que você nomeie a mudança
* **Contexto adicional**: restrições de indústria regulada, infraestrutura multi-tenant ou requisitos de conformidade que afetam o que o classificador deve tratar como arriscado

As entradas Registro de pacotes interno, Locais de dados sensíveis e públicos, Alvos remotos sensíveis e Escopos de IaC protegidos exigem Claude Code v2.1.195 ou posterior. Versões anteriores ainda as leem como contexto simples, mas não têm as regras integradas que as direcionam.

Um modelo inicial útil: preencha os campos entre colchetes e remova as linhas que não se aplicam.

```json theme={null}
{
  "autoMode": {
    "environment": [
      "$defaults",
      "Organization: {COMPANY_NAME}. Primary use: {PRIMARY_USE_CASE, e.g. software development, infrastructure automation}",
      "Source control: {SOURCE_CONTROL, e.g. GitHub org github.example.com/acme-corp}",
      "Cloud provider(s): {CLOUD_PROVIDERS, e.g. AWS, GCP, Azure}",
      "Trusted cloud buckets: {TRUSTED_BUCKETS, e.g. s3://acme-builds, gs://acme-datasets}",
      "Trusted internal domains: {TRUSTED_DOMAINS, e.g. *.internal.example.com, api.example.com}",
      "Key internal services: {SERVICES, e.g. Jenkins at ci.example.com, Artifactory at artifacts.example.com}",
      "Additional context: {EXTRA, e.g. regulated industry, multi-tenant infrastructure, compliance requirements}"
    ]
  }
}
```

Quanto mais contexto específico você fornecer, melhor o classificador poderá distinguir operações internas rotineiras de tentativas de exfiltração.

Você não precisa preencher tudo de uma vez. Um rollout razoável: comece com os padrões e adicione sua organização de controle de código-fonte e serviços internos principais, o que resolve os falsos positivos mais comuns, como fazer push para seus próprios repositórios. Adicione domínios confiáveis e buckets de nuvem a seguir. Preencha o resto conforme os bloqueios surgirem.

<h2 id="override-the-block-and-allow-rules">
  Substituir as regras de bloqueio e permissão
</h2>

Três campos adicionais permitem que você substitua as listas de regras integradas do classificador:

* `autoMode.hard_deny`: limites de segurança incondicionais
* `autoMode.soft_deny`: ações destrutivas que a intenção do usuário pode contornar
* `autoMode.allow`: exceções às regras de bloqueio soft

Cada um é uma matriz de descrições em prosa, lidas como regras em linguagem natural. Para bloqueios baseados em padrões de ferramentas que são executados antes do classificador, use [`permissions.deny`](https://code.claude.com/docs/pt/permissions).

Dentro do classificador, a precedência funciona em quatro camadas:

* Regras `hard_deny` bloqueiam incondicionalmente. A intenção do usuário e exceções `allow` não se aplicam.
* Regras `soft_deny` bloqueiam em seguida. A intenção do usuário e exceções `allow` podem substituir estas.
* Regras `allow` então substituem regras `soft_deny` correspondentes como exceções.
* A intenção explícita do usuário substitui os bloqueios soft restantes: se a mensagem do usuário descreve direta e especificamente a ação exata que Claude está prestes a executar, o classificador a permite mesmo quando uma regra `soft_deny` corresponde.

Solicitações gerais não contam como intenção explícita. Pedir ao Claude para "limpar o repositório" não autoriza force-push, mas pedir ao Claude para "force-push este branch" autoriza.

Para afrouxar, adicione a `allow` quando o classificador sinalizar repetidamente um padrão rotineiro que as exceções padrão não cobrem. Para apertar, adicione a `soft_deny` para riscos destrutivos específicos do seu ambiente que os padrões perdem, ou a `hard_deny` para limites de segurança que nunca devem ser ultrapassados.

Para manter as regras integradas enquanto adiciona as suas próprias, inclua a string literal `"$defaults"` na matriz. As regras padrão são inseridas nessa posição, portanto suas regras personalizadas podem vir antes ou depois delas, e você continua a herdar atualizações conforme a lista integrada muda entre versões.

O exemplo a seguir mantém os padrões em todas as quatro listas e adiciona regras específicas da organização a cada uma.

```json theme={null}
{
  "autoMode": {
    "environment": [
      "$defaults",
      "Source control: github.example.com/acme-corp and all repos under it"
    ],
    "allow": [
      "$defaults",
      "Deploying to the staging namespace is allowed: staging is isolated from production and resets nightly",
      "Writing to s3://acme-scratch/ is allowed: ephemeral bucket with a 7-day lifecycle policy"
    ],
    "soft_deny": [
      "$defaults",
      "Never run database migrations outside the migrations CLI, even against dev databases",
      "Never modify files under infra/terraform/prod/: production infrastructure changes go through the review workflow"
    ],
    "hard_deny": [
      "$defaults",
      "Never send repository contents to third-party code-review APIs"
    ]
  }
}
```

<Danger>
  Definir qualquer um de `environment`, `allow`, `soft_deny` ou `hard_deny` sem `"$defaults"` substitui a lista padrão inteira para essa seção. Se você definir uma matriz sem `"$defaults"`, descartará as regras integradas para essa seção:

  * `soft_deny`: todas as regras de bloqueio soft integradas, incluindo force push, `curl | bash`, implantações em produção e bypass de auto-mode
  * `hard_deny`: a regra integrada de exfiltração de dados
</Danger>

Cada seção é avaliada independentemente, portanto definir `environment` sozinho deixa as listas padrão `allow`, `soft_deny` e `hard_deny` intactas.

Omita `"$defaults"` apenas quando você pretender assumir a propriedade total da lista. Para fazer isso com segurança, execute `claude auto-mode defaults` para imprimir as regras integradas, copie-as para seu arquivo de configurações e depois revise cada regra em relação ao seu próprio pipeline e tolerância ao risco.

<h2 id="route-all-shell-commands-through-the-classifier">
  Rotear todos os comandos shell através do classificador
</h2>

Por padrão, regras de permissão Bash e PowerShell estreitas como `Bash(npm test)` são mantidas no modo automático e resolvidas antes do classificador ser executado. O modo automático suspende apenas as regras amplas que concedem execução de código arbitrário, como `Bash(*)` ou intérpretes com caracteres curinga. Isso significa que uma regra estreita ainda pode deixar um argumento destrutivo passar sem o classificador vê-lo, por exemplo um caminho de script ou flag que o prefixo da regra não antecipou.

Defina `autoMode.classifyAllShell` como `true` para suspender todas as regras de permissão Bash e PowerShell enquanto o modo automático está ativo, para que o classificador avalie cada comando shell independentemente de sua lista de permissões.

```json theme={null}
{
  "autoMode": {
    "classifyAllShell": true
  }
}
```

Isso troca latência por cobertura: um comando que uma regra de permissão teria aprovado instantaneamente agora aguarda uma decisão do classificador, e cada comando shell conta como uma chamada do classificador.

A configuração se aplica apenas enquanto o modo automático está ativo, e suas regras de permissão se comportam normalmente em outros modos de permissão.

<Note>
  `autoMode.classifyAllShell` requer Claude Code v2.1.193 ou posterior. Versões anteriores ignoram a chave e continuam a manter regras de permissão shell estreitas no modo automático.
</Note>

<h2 id="inspect-the-defaults-and-your-effective-config">
  Inspecionar os padrões e sua configuração efetiva
</h2>

Três subcomandos CLI ajudam você a inspecionar e validar sua configuração.

Imprima as regras `environment`, `allow`, `soft_deny` e `hard_deny` integradas como JSON:

```bash theme={null}
claude auto-mode defaults
```

Para ler a redação completa de uma regra sem canalizar através de `jq`, passe `--label` com o início do rótulo da regra, como `claude auto-mode defaults --label 'Git Destructive'`. A correspondência é um prefixo case-insensitive no rótulo de cada regra, e seções sem correspondência são impressas como listas vazias. Requer Claude Code v2.1.208 ou posterior.

Imprima o que o classificador realmente usa como JSON, com suas configurações aplicadas onde definidas e padrões caso contrário:

```bash theme={null}
claude auto-mode config
```

Obtenha feedback de IA sobre suas regras `allow`, `soft_deny` e `hard_deny` personalizadas:

```bash theme={null}
claude auto-mode critique
```

Execute `claude auto-mode config` após salvar suas configurações para confirmar que as regras efetivas são o que você espera, com `"$defaults"` expandido no lugar. Se você escreveu regras personalizadas, `claude auto-mode critique` as revisa e sinaliza entradas que são ambíguas, redundantes ou provavelmente causarão falsos positivos.

Se você precisar remover ou reescrever uma regra integrada em vez de adicionar ao lado dela, salve a saída de `claude auto-mode defaults` em um arquivo, edite as listas e cole o resultado em seu arquivo de configurações no lugar de `"$defaults"`.

<h2 id="review-denials">
  Revisar negações
</h2>

Quando o modo automático nega uma chamada de ferramenta, a negação é registrada em `/permissions` na aba Recently denied. Pressione `r` em uma ação negada para marcá-la para retry: quando você sair do diálogo, Claude Code envia uma mensagem dizendo ao modelo que ele pode tentar novamente essa chamada de ferramenta e retoma a conversa.

No Claude Code v2.1.193 e posterior, o motivo do classificador para cada negação aparece ao lado da chamada de ferramenta bloqueada na transcrição, na notificação de negação e sob cada entrada na aba Recently denied. Use o motivo para decidir se a correção é uma entrada `environment`, uma exceção `allow` ou tentar novamente com intenção explícita em sua próxima mensagem.

Negações repetidas para o mesmo destino geralmente significam que o classificador está perdendo contexto. Adicione esse destino a `autoMode.environment`, depois execute `claude auto-mode config` para confirmar que teve efeito.

Para reagir a negações programaticamente, use o hook [`PermissionDenied`](https://code.claude.com/docs/pt/hooks#permissiondenied).

<h2 id="see-also">
  Veja também
</h2>

* [Permission modes](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode): o que é modo automático, o que ele bloqueia por padrão e como ativá-lo
* [Managed settings](https://code.claude.com/docs/pt/server-managed-settings): implante a configuração `autoMode` em toda a sua organização
* [Permissions](https://code.claude.com/docs/pt/permissions): regras de permissão, pergunta e negação que se aplicam antes do classificador ser executado
* [Settings](https://code.claude.com/docs/pt/settings): a referência de configurações completa, incluindo a chave `autoMode`
