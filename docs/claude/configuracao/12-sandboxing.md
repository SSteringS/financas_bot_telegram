<!-- Fonte: https://code.claude.com/docs/pt/sandboxing | Baixado em: 2026-08-06 | slug: sandboxing -->

# Configurar a ferramenta Bash em sandbox

> Aprenda como a ferramenta Bash em sandbox do Claude Code fornece isolamento de sistema de arquivos e rede para execução de agentes mais segura e autônoma.

O sandbox Bash permite que Claude execute a maioria dos comandos shell sem parar para pedir permissão. Em vez de aprovar cada comando, você define quais arquivos e domínios de rede os comandos podem acessar, e o sistema operacional impõe esse limite para cada comando Bash e seus processos filhos.

<Note>
  Para comparar outras abordagens de isolamento, como dev containers, containers personalizados e máquinas virtuais, consulte [Sandbox environments](https://code.claude.com/docs/pt/sandbox-environments). Para reduzir prompts de permissão para ferramentas diferentes de Bash, consulte [permission modes](https://code.claude.com/docs/pt/permission-modes).
</Note>

<h2 id="get-started">
  Get started
</h2>

O sandbox é integrado ao Claude Code e é executado em macOS, Linux e WSL2. Windows nativo não é suportado. No Windows, execute Claude Code dentro de uma distribuição WSL2.

No macOS, não há nada para instalar: o sandboxing usa o framework Seatbelt integrado. No Linux e WSL2, o sandbox depende de dois pacotes, abordados em [Set up Linux and WSL2](#set-up-linux-and-wsl2). Mesmo que você ainda não os tenha instalado, você pode começar com `/sandbox`, porque seu painel mostra se algo está faltando.

<Steps>
  <Step title="Run /sandbox">
    Inicie uma sessão do Claude Code e execute o comando `/sandbox`:

    ```text theme={null}
    /sandbox
    ```

    Isso abre o painel de sandbox com três abas:

    * **Mode**: escolha como os comandos em sandbox são aprovados, abordado na próxima etapa
    * **Overrides**: escolha se os comandos que falham sob o sandbox podem voltar a ser executados sem sandbox. Esta é a configuração [`allowUnsandboxedCommands`](https://code.claude.com/docs/pt/settings#sandbox-settings)
    * **Config**: visualize as configurações de sandbox resolvidas

    Se o painel mostrar apenas uma aba Dependencies, um pacote necessário está faltando. Instale-o conforme descrito em [Set up Linux and WSL2](#set-up-linux-and-wsl2), reinicie Claude Code e execute `/sandbox` novamente.
  </Step>

  <Step title="Choose a mode">
    Na aba Mode, selecione auto-allow ou regular permissions. Auto-allow executa comandos em sandbox sem avisar, e regular permissions mantém os prompts de permissão regulares mesmo quando os comandos estão em sandbox. Consulte [Sandbox modes](#sandbox-modes) para saber quais comandos ainda solicitam no modo auto-allow.
  </Step>

  <Step title="Run a Bash command">
    Peça ao Claude para executar um comando, como uma compilação ou um conjunto de testes. Por padrão, os comandos dentro do sandbox podem escrever apenas no diretório de trabalho e no diretório temporário da sessão. Na primeira vez que um comando precisa de um novo domínio de rede, Claude Code solicita aprovação.

    Comandos que não podem ser executados em sandbox voltam ao fluxo de permissão regular. Para ampliar ou estreitar esses limites, consulte [Configure sandboxing](#configure-sandboxing).
  </Step>
</Steps>

Selecionar um modo no painel escreve nas configurações locais do seu projeto em `.claude/settings.local.json`, que se aplicam ao projeto atual e não são verificadas no git. Para habilitar o sandbox em todos os seus projetos, defina [`sandbox.enabled`](https://code.claude.com/docs/pt/settings#sandbox-settings) como `true` em suas configurações de usuário em `~/.claude/settings.json`. Para impor sandboxing para cada desenvolvedor em uma organização, use [managed settings](#enforce-sandboxing-with-managed-settings).

<Warning>
  Por padrão, se o sandbox não conseguir iniciar porque as dependências estão faltando ou a plataforma não é suportada, Claude Code mostra um aviso e executa comandos sem sandboxing. Para tornar isso uma falha difícil em vez disso, defina [`sandbox.failIfUnavailable`](https://code.claude.com/docs/pt/settings#sandbox-settings) como `true`. Isso é destinado a implantações gerenciadas que exigem sandboxing como um portão de segurança.
</Warning>

<h3 id="set-up-linux-and-wsl2">
  Set up Linux and WSL2
</h3>

No Linux e WSL2, o sandbox depende de dois pacotes:

* [`bubblewrap`](https://github.com/containers/bubblewrap): a ferramenta de sandboxing sem privilégios que impõe isolamento de sistema de arquivos
* [`socat`](http://www.dest-unreach.org/socat/): o relay usado para rotear tráfego de rede através do proxy de sandbox

Instale-os com o gerenciador de pacotes da sua distribuição:

<Tabs>
  <Tab title="Ubuntu/Debian">
    ```bash theme={null}
    sudo apt-get install bubblewrap socat
    ```
  </Tab>

  <Tab title="Fedora">
    ```bash theme={null}
    sudo dnf install bubblewrap socat
    ```
  </Tab>
</Tabs>

Após a instalação, a aba Dependencies em `/sandbox` mostra se `ripgrep`, `bubblewrap`, `socat` e o filtro seccomp estão disponíveis em sua plataforma. Ripgrep é incluído no binário nativo do Claude Code. O filtro seccomp é opcional e adiciona bloqueio de socket de domínio Unix. Instale-o com `npm install -g @anthropic-ai/sandbox-runtime` se estiver faltando.

Quando uma dependência necessária está faltando, a aba Dependencies é a única aba mostrada até que você a instale. A verificação de dependência é executada na inicialização, portanto reinicie Claude Code após instalar pacotes para que `/sandbox` os detecte.

<AccordionGroup>
  <Accordion title="Ubuntu 24.04 e posterior: permitir que bubblewrap crie namespaces de usuário">
    No Ubuntu 24.04 e posterior, a política padrão do AppArmor impede que bubblewrap crie os namespaces de usuário que precisa para isolamento.

    Para verificar se seu ambiente impõe essa restrição, incluindo dentro do WSL2, execute `sysctl kernel.apparmor_restrict_unprivileged_userns`. Se a chave não existir ou retornar `0`, pule esta etapa. Se retornar `1`, adicione um perfil AppArmor que conceda a `bwrap` essa capacidade:

    ```bash theme={null}
    sudo tee /etc/apparmor.d/bwrap > /dev/null <<'EOF'
    abi <abi/4.0>,
    include <tunables/global>

    profile bwrap /usr/bin/bwrap flags=(unconfined) {
      userns,
      include if exists <local/bwrap>
    }
    EOF
    ```

    O perfil se aplica apenas a `bwrap` em si, não aos comandos executados dentro do sandbox. Recarregue AppArmor para aplicá-lo:

    ```bash theme={null}
    sudo systemctl reload apparmor
    ```
  </Accordion>

  <Accordion title="Notas do WSL2">
    Verifique sua versão do WSL com `wsl -l -v` do PowerShell. Se você vir `Sandboxing requires WSL2`, sua distribuição está executando WSL1. Atualize-a para WSL2 ou execute Claude Code sem sandboxing.

    No WSL2, comandos em sandbox não podem iniciar binários do Windows como `cmd.exe`, `powershell.exe` ou qualquer coisa em `/mnt/c/`. WSL entrega esses para o host do Windows através de um socket Unix, que o sandbox bloqueia. Se um comando precisar invocar um binário do Windows, adicione-o a [`excludedCommands`](https://code.claude.com/docs/pt/settings#sandbox-settings) para que seja executado fora do sandbox.
  </Accordion>
</AccordionGroup>

<h3 id="sandbox-modes">
  Sandbox modes
</h3>

Claude Code oferece dois modos de sandbox:

**Modo auto-allow**: Comandos Bash tentarão ser executados dentro do sandbox e são automaticamente permitidos sem exigir permissão. Comandos que não podem ser colocados em sandbox, como aqueles que precisam de acesso à rede para hosts não permitidos, voltam ao fluxo de permissão regular, onde Claude Code verifica suas [permission rules](https://code.claude.com/docs/pt/permissions) e solicita sua aprovação para qualquer comando que essas regras não permitam.

Mesmo no modo auto-allow, o seguinte ainda se aplica:

* [Deny rules](https://code.claude.com/docs/pt/permissions) explícitas são sempre respeitadas
* Comandos `rm` ou `rmdir` que visam `/`, seu diretório home ou outros caminhos críticos do sistema ainda acionam um prompt de permissão
* [Ask rules](https://code.claude.com/docs/pt/permissions) com escopo de conteúdo como `Bash(git push *)` ainda forçam um prompt mesmo para comandos em sandbox
* Uma regra ask `Bash` simples, ou o formulário equivalente `Bash(*)`, é ignorada para comandos executados em sandbox; ainda se aplica a comandos que voltam ao fluxo de permissão regular

**Modo regular permissions**: Todos os comandos Bash passam pelo fluxo de permissão regular, mesmo quando em sandbox. Isso fornece mais controle, mas requer mais aprovações.

Em ambos os modos, o sandbox impõe as mesmas restrições de sistema de arquivos e rede. A diferença é apenas se os comandos em sandbox são aprovados automaticamente ou requerem permissão explícita.

O diretório temporário da sessão é gravável dentro do sandbox por padrão, junto com o diretório de trabalho. Claude Code define `$TMPDIR` para este diretório para comandos em sandbox, portanto ferramentas que escrevem arquivos temporários funcionam sem configuração extra. Comandos não em sandbox herdam o `$TMPDIR` do seu shell inalterado, o que significa que comandos em sandbox e não em sandbox resolvem `$TMPDIR` para diretórios diferentes. Para passar arquivos temporários entre os dois, escreva-os no diretório de trabalho em vez disso.

Alguns comandos não podem ser executados dentro do sandbox, como ferramentas que são incompatíveis com ele ou que precisam de um host que você não permitiu. Em vez de falhar na tarefa ou exigir que você desative o sandboxing, Claude Code inclui um escape hatch: quando um comando falha por causa de restrições de sandbox, Claude analisa a falha e pode tentar novamente o comando com o parâmetro `dangerouslyDisableSandbox`. O comando retentado é executado fora do sandbox, portanto passa pelo fluxo de permissão regular: no modo padrão você recebe um prompt de confirmação; no [auto mode](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode) o classificador avalia o comando subjacente em vez de solicitar sua aprovação. Para ser solicitado em cada retentativa sem sandbox mesmo no modo auto, adicione uma [ask rule](https://code.claude.com/docs/pt/permissions#match-by-input-parameter) para `Bash(dangerouslyDisableSandbox:true)`.

Você pode desabilitar esse escape hatch definindo `"allowUnsandboxedCommands": false` em suas [sandbox settings](https://code.claude.com/docs/pt/settings#sandbox-settings). Quando desabilitado, que a aba Overrides do `/sandbox` mostra como **Strict sandbox mode**, o parâmetro `dangerouslyDisableSandbox` é completamente ignorado e todos os comandos devem ser executados em sandbox ou estar explicitamente listados em `excludedCommands`.

<Info>
  O modo auto-allow funciona independentemente de sua configuração de permission mode. Mesmo que você não esteja no modo "accept edits", comandos Bash em sandbox serão executados automaticamente quando auto-allow estiver habilitado. Isso significa que comandos Bash que modificam arquivos dentro dos limites do sandbox serão executados sem avisar, mesmo quando ferramentas de edição de arquivo normalmente exigiriam aprovação.
</Info>

<h2 id="configure-sandboxing">
  Configure sandboxing
</h2>

Personalize o comportamento do sandbox através de seu arquivo `settings.json`. Consulte [Settings](https://code.claude.com/docs/pt/settings#sandbox-settings) para a referência de configuração completa.

Por padrão, comandos em sandbox podem escrever apenas no diretório de trabalho atual e no diretório temporário da sessão. Se comandos de subprocesso como `kubectl`, `terraform` ou `npm` precisarem escrever fora desses diretórios, use `sandbox.filesystem.allowWrite` para conceder acesso a caminhos específicos:

```json theme={null}
{
  "sandbox": {
    "enabled": true,
    "filesystem": {
      "allowWrite": ["~/.kube", "/tmp/build"]
    }
  }
}
```

Esses caminhos são impostos no nível do SO, portanto todos os comandos executados dentro do sandbox, incluindo seus processos filhos, os respeitam. Esta é a abordagem recomendada quando uma ferramenta precisa de acesso de escrita a um local específico, em vez de excluir a ferramenta do sandbox inteiramente com `excludedCommands`.

Quando o mesmo array de sistema de arquivos é definido em múltiplos [settings scopes](https://code.claude.com/docs/pt/settings#settings-precedence), os arrays são mesclados: caminhos de cada escopo são combinados, não substituídos.

Prefixos de caminho controlam como os caminhos são resolvidos:

| Prefixo             | Significado                                                                                              | Exemplo                                                                    |
| :------------------ | :------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------- |
| `/`                 | Caminho absoluto da raiz do sistema de arquivos                                                          | `/tmp/build` permanece `/tmp/build`                                        |
| `~/`                | Relativo ao diretório home                                                                               | `~/.kube` torna-se `$HOME/.kube`                                           |
| `./` ou sem prefixo | Relativo à raiz do projeto para configurações de projeto, ou a `~/.claude` para configurações de usuário | `./output` em `.claude/settings.json` resolve para `<project-root>/output` |

Esta sintaxe difere das [Read and Edit permission rules](https://code.claude.com/docs/pt/permissions#read-and-edit), que usam `//path` para absoluto e `/path` para relativo ao projeto. Os caminhos do sistema de arquivos do sandbox usam convenções padrão: `/tmp/build` é absoluto.

Você também pode negar acesso de escrita ou leitura usando `sandbox.filesystem.denyWrite` e `sandbox.filesystem.denyRead`, e permitir novamente caminhos específicos dentro de uma região negada usando `sandbox.filesystem.allowRead`. Quando as regras de leitura se sobrepõem, o caminho mais específico vence:

| Regras de exemplo                                      | Resultado                                                                                                                                                                                      |
| :----------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `"denyRead": ["~/"]` com `"allowRead": ["~/projects"]` | `~/projects` é legível e o resto do diretório home permanece bloqueado. O allow mais estreito reabre essa parte da região negada                                                               |
| `"allowRead": ["~/"]` com `"denyRead": ["~/.env"]`     | `~/.env` permanece bloqueado e o resto do diretório home é legível. Um deny exato se mantém dentro de um allow mais amplo, portanto um allow amplo não pode reexpor silenciosamente um segredo |

O exemplo abaixo bloqueia a leitura de todo o diretório home enquanto ainda permite leituras do projeto atual. Coloque-o no `.claude/settings.json` do seu projeto, porque o caminho relativo `.` resolve para a raiz do projeto apenas quando a configuração reside em configurações de projeto:

```json theme={null}
{
  "sandbox": {
    "enabled": true,
    "filesystem": {
      "denyRead": ["~/"],
      "allowRead": ["."]
    }
  }
}
```

O `.` em `allowRead` resolve para a raiz do projeto porque esta configuração reside em configurações de projeto. Se você colocasse a mesma configuração em `~/.claude/settings.json`, `.` resolveria para `~/.claude` em vez disso, e arquivos do projeto permaneceriam bloqueados pela regra `denyRead`.

<h3 id="protect-credentials">
  Protect credentials
</h3>

A configuração `sandbox.credentials` declara arquivos de credenciais e variáveis de ambiente a proteger de comandos em sandbox. Cada entrada nomeia um caminho de arquivo ou uma variável de ambiente e um `mode`. O bloco `credentials` dedicado mantém as regras de credenciais agrupadas e separadas das regras gerais do sistema de arquivos. Requer Claude Code v2.1.187 ou posterior.

Para entradas com `"mode": "deny"`, caminhos de arquivo são negados para leituras dentro do sandbox, a mesma restrição que `filesystem.denyRead` aplica, e variáveis de ambiente são removidas antes de cada comando em sandbox ser executado.

O exemplo abaixo bloqueia leituras do arquivo de credenciais AWS e do diretório SSH e remove `GITHUB_TOKEN` e `NPM_TOKEN` do ambiente de comandos em sandbox:

```json theme={null}
{
  "sandbox": {
    "enabled": true,
    "credentials": {
      "files": [
        { "path": "~/.aws/credentials", "mode": "deny" },
        { "path": "~/.ssh", "mode": "deny" }
      ],
      "envVars": [
        { "name": "GITHUB_TOKEN", "mode": "deny" },
        { "name": "NPM_TOKEN", "mode": "deny" }
      ]
    }
  }
}
```

Entradas de arquivo suportam apenas `"mode": "deny"`. Entradas de variáveis de ambiente também aceitam `"mode": "mask"`, descrito abaixo.

Os caminhos de arquivo seguem as mesmas [regras de prefixo](https://code.claude.com/docs/pt/settings#sandbox-path-prefixes) que as configurações `sandbox.filesystem.*`, e as entradas `deny` de cada [settings scope](https://code.claude.com/docs/pt/settings#settings-precedence) são mescladas. Uma entrada `deny` apenas restringe o acesso, portanto qualquer escopo pode adicionar uma, mas nenhum escopo pode remover uma que outro escopo adicionou.

Não há uma lista de negação de credenciais integrada, portanto apenas os arquivos e variáveis que você listar são restritos. A configuração afeta apenas comandos Bash em sandbox. Para remover credenciais da Anthropic e de provedores de nuvem de todos os subprocessos independentemente do sandboxing, defina [`CLAUDE_CODE_SUBPROCESS_ENV_SCRUB`](https://code.claude.com/docs/pt/env-vars).

<h4 id="mask-environment-variables">
  Mask environment variables
</h4>

`"mode": "mask"` protege uma credencial mantendo as ferramentas que se autenticam com ela funcionando. `deny` remove a variável inteiramente, o que também quebra ferramentas que precisam dela, como `gh` ou `npm`. Requer Claude Code v2.1.199 ou posterior.

Com `mask`, o comando em sandbox vê um valor sentinela por sessão em vez do real. Quando uma solicitação sai do sandbox para um dos `injectHosts` da credencial, o [sandbox proxy](#network-isolation) substitui o sentinela pelo valor real. O comando e tudo que ele registra nunca mantêm a credencial real, mas suas solicitações ainda se autenticam.

O proxy substitui a credencial dentro do conteúdo da solicitação, portanto tem que vê-lo. Defina [`network.tlsTerminate`](https://code.claude.com/docs/pt/settings#sandbox-settings) para que o proxy termine TLS em si. Sem isso, o mascaramento falha fechado: o comando ainda vê apenas o sentinela, mas o sentinela chega ao servidor inalterado e a autenticação falha. Claude Code relata essa configuração incorreta na inicialização.

O exemplo abaixo mascara dois tokens. `GH_TOKEN` é substituído apenas em solicitações para `api.github.com`, enquanto `NPM_TOKEN` não tem `injectHosts` e é substituído em solicitações para cada host em `network.allowedDomains`. Cada entrada `injectHosts` deve ser coberta por `network.allowedDomains`.

```json theme={null}
{
  "sandbox": {
    "enabled": true,
    "network": {
      "tlsTerminate": {},
      "allowedDomains": ["*.github.com", "registry.npmjs.org"]
    },
    "credentials": {
      "envVars": [
        { "name": "GH_TOKEN", "mode": "mask", "injectHosts": ["api.github.com"] },
        { "name": "NPM_TOKEN", "mode": "mask" }
      ]
    }
  }
}
```

Diferentemente de `deny`, o mascaramento autoriza o proxy a enviar sua credencial real para os hosts listados, portanto é honrado apenas a partir de configurações que você ou seu administrador controlam: configurações de usuário, configurações gerenciadas e o sinalizador CLI `--settings`. Entradas `mask`, `network.tlsTerminate` e [`credentials.allowPlaintextInject`](https://code.claude.com/docs/pt/settings#sandbox-settings) no `.claude/settings.json` ou `.claude/settings.local.json` de um repositório são ignoradas.

Quando a mesma variável é listada com `deny` em qualquer escopo, `deny` tem precedência.

<h2 id="how-sandboxing-works">
  Como o sandboxing funciona
</h2>

<h3 id="filesystem-isolation">
  Isolamento do sistema de arquivos
</h3>

A ferramenta Bash em sandbox restringe o acesso ao sistema de arquivos a diretórios específicos:

* **Comportamento padrão de escrita**: acesso de leitura e escrita ao diretório de trabalho atual e seus subdiretórios, além do diretório temporário da sessão para o qual `$TMPDIR` aponta
* **Comportamento padrão de leitura**: acesso de leitura a todo o computador, exceto certos diretórios negados. Observe que esse padrão ainda permite ler arquivos de credenciais como `~/.aws/credentials` e `~/.ssh/`. Use [`sandbox.credentials`](#protect-credentials) para bloquear leituras desses arquivos e desconfigurar variáveis de ambiente secretas, ou adicione os caminhos a `denyRead`.
* **Acesso bloqueado**: não é possível modificar arquivos fora do diretório de trabalho atual e do diretório temporário da sessão sem permissão explícita, incluindo arquivos de configuração de shell como `~/.bashrc` e binários do sistema em `/bin/`
* **Git worktrees**: quando o diretório de trabalho é um [git worktree vinculado](https://code.claude.com/docs/pt/worktrees), o sandbox também permite escritas no diretório `.git` compartilhado do repositório principal para que comandos como `git commit` possam atualizar refs e o índice. As escritas em `hooks/` e `config` dentro desse diretório permanecem negadas.
* **Configurável**: defina caminhos permitidos e negados personalizados através de configurações

Você pode conceder acesso de escrita a caminhos adicionais usando `sandbox.filesystem.allowWrite` em suas configurações. Essas restrições são impostas no nível do SO, portanto se aplicam a todos os comandos de subprocesso, incluindo ferramentas como `kubectl`, `terraform` e `npm`, não apenas às ferramentas de arquivo do Claude.

<h3 id="network-isolation">
  Isolamento de rede
</h3>

O acesso à rede é controlado através de um servidor proxy executado fora do sandbox:

* **Restrições de domínio**: nenhum domínio é pré-permitido. Na primeira vez que um comando precisa de um novo domínio, Claude Code solicita aprovação. A partir da v2.1.191, escolher Sim permite o host para o resto da sessão atual, portanto conexões posteriores ao mesmo host não solicitam novamente. Pré-permita domínios com [`allowedDomains`](https://code.claude.com/docs/pt/settings#sandbox-settings) para evitar o prompt inteiramente.
* **Managed lockdown**: se [`allowManagedDomainsOnly`](https://code.claude.com/docs/pt/settings#sandbox-settings) estiver definido em configurações gerenciadas, domínios não permitidos são bloqueados automaticamente em vez de solicitar, e apenas `allowedDomains` de configurações gerenciadas são honrados.
* **Suporte a proxy personalizado**: usuários avançados podem implementar regras personalizadas no tráfego de saída
* **Cobertura abrangente**: as restrições se aplicam a todos os scripts, programas e subprocessos gerados por comandos

<Note>
  O proxy integrado impõe a allowlist com base no nome de host solicitado e, por padrão, não termina ou inspeciona tráfego TLS. A configuração experimental [`network.tlsTerminate`](https://code.claude.com/docs/pt/settings#sandbox-settings), disponível no Claude Code v2.1.199 e posterior, faz com que o proxy integrado termine TLS em si mesmo, o que as entradas de credenciais [`mask`](#protect-credentials) exigem. Consulte [Limitações de segurança](#security-limitations) para as implicações do padrão, e [Configuração de proxy personalizado](#custom-proxy-configuration) se seu modelo de ameaça exigir inspeção TLS.
</Note>

<h3 id="os-level-enforcement">
  Imposição no nível do SO
</h3>

A ferramenta Bash em sandbox aproveita primitivos de segurança do sistema operacional:

* **macOS**: usa Seatbelt para imposição de sandbox
* **Linux**: usa [bubblewrap](https://github.com/containers/bubblewrap) para isolamento
* **WSL2**: usa bubblewrap, igual ao Linux

WSL1 não é suportado porque bubblewrap requer recursos de kernel disponíveis apenas no WSL2. Essas restrições no nível do SO garantem que todos os processos filhos gerados pelos comandos do Claude Code herdem os mesmos limites de segurança.

Esses mesmos primitivos estão disponíveis como o pacote autônomo [`@anthropic-ai/sandbox-runtime`](https://github.com/anthropic-experimental/sandbox-runtime), que a página [Sandbox environments](https://code.claude.com/docs/pt/sandbox-environments#sandbox-runtime) aborda como uma abordagem separada para envolver todo o processo do Claude Code.

<h2 id="how-sandboxing-relates-to-permissions-and-permission-modes">
  Como sandboxing se relaciona com permissões e modos de permissão
</h2>

Sandboxing, [regras de permissão](https://code.claude.com/docs/pt/permissions) e [modos de permissão](https://code.claude.com/docs/pt/permission-modes) são camadas complementares. As seções abaixo abrangem como o sandbox interage com cada uma.

<h3 id="permission-rules">
  Regras de permissão
</h3>

Regras de permissão e sandboxing controlam coisas diferentes:

* **Regras de permissão** controlam quais ferramentas Claude Code pode usar e são avaliadas antes de qualquer ferramenta ser executada. Elas se aplicam a todas as ferramentas: Bash, Read, Edit, WebFetch, MCP e outras.
* **Sandboxing** fornece imposição no nível do SO que restringe o que comandos Bash podem acessar no nível de sistema de arquivos e rede. Aplica-se apenas a comandos Bash e seus processos filhos.

As duas camadas também diferem em como são impostas. Claude Code avalia decisões de permissão antes de um comando ser executado, com base na string do comando e, no modo auto, no julgamento de um classificador separado sobre se o comando é seguro. O sistema operacional impõe o limite do sandbox no processo em execução, portanto se mantém independentemente do que o modelo escolheu executar e mesmo se um comando permitido faz mais do que seu nome sugere.

Restrições de sistema de arquivos e rede são configuradas através de configurações de sandbox e regras de permissão:

| Configuração ou regra                                          | O que faz                                                                                                    |
| :------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------- |
| `sandbox.filesystem.allowWrite`                                | Concede acesso de escrita de subprocesso a caminhos fora do diretório de trabalho                            |
| `sandbox.filesystem.denyWrite` e `sandbox.filesystem.denyRead` | Bloqueiam acesso de subprocesso a caminhos específicos                                                       |
| `sandbox.filesystem.allowRead`                                 | Permite novamente a leitura de caminhos específicos dentro de uma região `denyRead`                          |
| Regras de permissão `Edit`                                     | Concedem acesso de escrita a caminhos específicos, da mesma forma que `sandbox.filesystem.allowWrite` faz    |
| Regras de negação `Read` e `Edit`                              | Bloqueiam acesso a arquivos ou diretórios específicos                                                        |
| Regras de permissão e negação `WebFetch`                       | Controlam acesso a domínios                                                                                  |
| `allowedDomains` de sandbox                                    | Controla quais domínios comandos Bash podem alcançar                                                         |
| `deniedDomains` de sandbox                                     | Bloqueia domínios específicos mesmo quando um wildcard `allowedDomains` mais amplo permitiria de outra forma |

Caminhos de ambas as configurações `sandbox.filesystem` e regras de permissão são mesclados juntos na configuração final do sandbox.

O [diretório de exemplos do repositório claude-code](https://github.com/anthropics/claude-code/tree/main/examples/settings) inclui configurações de configurações iniciais para cenários de implantação comuns, incluindo exemplos específicos de sandbox. Use-os como pontos de partida e ajuste-os para suas necessidades.

<h3 id="permission-modes">
  Modos de permissão
</h3>

`/sandbox` não é um [modo de permissão](https://code.claude.com/docs/pt/permission-modes). Modos de permissão decidem se uma chamada de ferramenta é executada e se você é solicitado primeiro, enquanto o sandbox restringe o que um comando Bash pode acessar uma vez que é executado. Eles diferem no que controlam e o que substitui o prompt por ação:

|                                                                    | O que controla                                             | O que substitui o prompt                                                                                                                                                                                                                                                                                                                                                                                                                             |
| :----------------------------------------------------------------- | :--------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/sandbox`                                                         | O que um comando Bash pode acessar uma vez que é executado | O limite do sandbox em si, no [modo auto-allow](#sandbox-modes)                                                                                                                                                                                                                                                                                                                                                                                      |
| [Modo auto](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode) | Se cada chamada de ferramenta é executada                  | Um classificador que revisa ações                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `--dangerously-skip-permissions`                                   | Se cada chamada de ferramenta é executada                  | Nada. Verificações de [caminho protegido](https://code.claude.com/docs/pt/permission-modes#protected-paths) também são ignoradas; apenas [regras ask](https://code.claude.com/docs/pt/permissions#manage-permissions) explícitas, ferramentas de conector [que sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools), ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) e remover `/` ou seu diretório home ainda solicitam |

O [modo auto-allow](#sandbox-modes) do sandbox é separado do [modo auto](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode): auto-allow aprova comandos Bash porque o limite do sandbox os contém, enquanto modo auto usa um classificador para revisar ações. Os dois funcionam independentemente e podem ser combinados. Para escolher um limite de isolamento para execuções autônomas, consulte [Ambientes de sandbox](https://code.claude.com/docs/pt/sandbox-environments#how-isolation-relates-to-permission-modes).

<h2 id="configure-the-sandbox-for-your-organization">
  Configure the sandbox for your organization
</h2>

Administradores podem exigir sandboxing para cada usuário, impedir que desenvolvedores ampliem a política e rotear tráfego de sandbox através de um proxy corporativo.

<h3 id="enforce-sandboxing-with-managed-settings">
  Enforce sandboxing with managed settings
</h3>

Para exigir o sandbox para cada desenvolvedor, entregue as chaves `sandbox` através de [managed settings](https://code.claude.com/docs/pt/settings#settings-files), seja como um arquivo gerenciado pelo seu MDM ou através de [server-managed settings](https://code.claude.com/docs/pt/server-managed-settings) no Claude.ai.

A seguinte configuração de managed settings habilita o sandbox, recusa iniciar Claude Code se o sandbox não conseguir inicializar e impede que o modelo tente novamente comandos fora do sandbox:

```json theme={null}
{
  "sandbox": {
    "enabled": true,
    "failIfUnavailable": true,
    "allowUnsandboxedCommands": false
  }
}
```

As duas chaves além de `enabled` controlam o que acontece quando o sandbox não consegue executar um comando:

* **`failIfUnavailable`**: uma dependência faltante como bubblewrap no Linux bloqueia Claude Code de iniciar em vez de mostrar um aviso e voltar a execução sem sandbox
* **`allowUnsandboxedCommands: false`**: o escape hatch `dangerouslyDisableSandbox` é ignorado, portanto comandos que falham sob o sandbox não podem ser retentados fora dele

Duas adições valem a pena considerar junto com elas. Adicione `excludedCommands` para qualquer ferramenta aprovada pela organização que deve ser executada sem isolamento. Adicione entradas [`sandbox.credentials`](#protect-credentials) para diretórios de credenciais como `~/.aws` e `~/.ssh` e para variáveis de ambiente secretas, já que a política de leitura padrão ainda permite.

O sandbox não é executado no Windows nativo, portanto se sua frota inclui hosts Windows, escope esta configuração para macOS e Linux ou tenha esses usuários executarem Claude Code dentro do WSL2 ou um container.

<h3 id="keep-developers-from-widening-the-policy">
  Keep developers from widening the policy
</h3>

Para chaves booleanas como `enabled` e `failIfUnavailable`, Claude Code usa o valor gerenciado e ignora qualquer coisa que um desenvolvedor defina localmente. Para chaves de array como `excludedCommands` e `allowRead`, Claude Code mescla entradas de cada escopo, portanto um desenvolvedor pode anexar entradas que ampliem a política.

Defina `allowManagedReadPathsOnly` como `true` em configurações gerenciadas para que apenas entradas `allowRead` de configurações gerenciadas sejam honradas. Entradas `allowRead` de usuário, projeto e local são ignoradas. Isso impede que desenvolvedores ampliem o acesso de leitura além dos caminhos aprovados pela organização. Para bloquear domínios de rede para os valores gerenciados da mesma forma, defina [`allowManagedDomainsOnly`](https://code.claude.com/docs/pt/settings#sandbox-settings).

`excludedCommands` não tem um equivalente de lockdown apenas gerenciado, portanto um desenvolvedor sempre pode anexar entradas que executem comandos adicionais fora do sandbox. Mantenha a lista gerenciada estreita.

<h3 id="custom-proxy-configuration">
  Custom proxy configuration
</h3>

Para organizações que exigem segurança de rede avançada, você pode implementar um proxy personalizado para:

* Descriptografar e inspecionar tráfego HTTPS
* Aplicar regras de filtragem personalizadas
* Registrar todas as solicitações de rede
* Integrar com infraestrutura de segurança existente

Para apontar Claude Code para seu proxy, defina as portas de proxy em [sandbox settings](https://code.claude.com/docs/pt/settings#sandbox-settings):

```json theme={null}
{
  "sandbox": {
    "network": {
      "httpProxyPort": 8080,
      "socksProxyPort": 8081
    }
  }
}
```

<h2 id="troubleshooting">
  Troubleshooting
</h2>

Alguns comandos falham dentro do sandbox mesmo que funcionem fora dele. As correções abaixo abrangem os casos mais comuns.

* **Comandos falham com um erro host-not-allowed**: muitas ferramentas CLI precisam alcançar hosts específicos. Conceder permissão quando solicitado adiciona o host à sua lista de permitidos para que a ferramenta seja executada dentro do sandbox no futuro.
* **`jest` trava ou falha**: `watchman` é incompatível com o sandbox. Execute `jest --no-watchman` em vez disso.
* **CLIs baseadas em Go falham na verificação TLS no macOS**: ferramentas como `gh`, `gcloud` e `terraform` podem falhar na verificação TLS sob Seatbelt. Liste essas ferramentas em `excludedCommands` para executá-las fora do sandbox. Se você estiver usando `httpProxyPort` com um proxy MITM e CA personalizado, defina [`enableWeakerNetworkIsolation`](https://code.claude.com/docs/pt/settings#sandbox-settings) como `true` em vez disso.
* **`open`, `osascript`, ou fluxos de autenticação baseados em navegador falham com erro `-600` no macOS**: o sandbox bloqueia Apple Events por padrão. Defina [`allowAppleEvents`](https://code.claude.com/docs/pt/settings#sandbox-settings) como `true` em suas configurações de usuário, gerenciadas ou CLI para permitir. As configurações do projeto são ignoradas para esta chave. Habilitá-lo remove o isolamento de execução de código, pois comandos em sandbox podem então iniciar outras aplicações sem sandbox sem prompt do usuário e enviar comandos AppleScript para aplicações em execução, sujeito ao prompt de consentimento de automação do macOS (TCC). Alternativamente, adicione o comando a `excludedCommands` para executá-lo fora do sandbox.
* **Comandos `docker` falham**: `docker` é incompatível com o sandbox. Adicione `docker *` a `excludedCommands` para executá-lo fora do sandbox.
* **Bubblewrap falha ao iniciar dentro de um container**: em um container sem privilégios, bubblewrap não consegue montar um sistema de arquivos `/proc` fresco. Defina [`enableWeakerNestedSandbox`](https://code.claude.com/docs/pt/settings#sandbox-settings) como `true` para que o sandbox interno faça bind-mount do `/proc` existente do container em vez disso. Use esta configuração apenas quando o container externo já fornece o limite de isolamento que você precisa, pois expõe informações de processo a comandos em sandbox que uma montagem `/proc` fresca ocultaria.
* **Filtro seccomp no Linux**: o filtro seccomp é necessário para bloquear sockets de domínio Unix. A aba Dependencies em `/sandbox` mostra se está disponível. Se estiver faltando, execute `npm install -g @anthropic-ai/sandbox-runtime` para instalar o helper.
* **`--dangerously-skip-permissions` falha como root**: este sinalizador é bloqueado ao executar como root ou via sudo no Linux e macOS, porque acesso root combinado com nenhum prompt de permissão pode modificar qualquer arquivo ou serviço no sistema. A verificação é ignorada automaticamente dentro de um sandbox reconhecido. Para executar autonomamente em um container, use a configuração [dev container](https://code.claude.com/docs/pt/devcontainer), que executa Claude Code como um usuário não-root.

<h2 id="limitations">
  Limitações
</h2>

Sandboxing reduz risco, mas não é um limite de isolamento completo. Revise as limitações abaixo antes de confiar nele como um controle de segurança difícil.

<h3 id="security-limitations">
  Limitações de segurança
</h3>

* **Filtragem de rede**: o sandbox restringe quais domínios os processos podem se conectar. Por padrão, o proxy integrado não termina ou inspeciona TLS no tráfego de saída, portanto o conteúdo de conexões criptografadas não é examinado. A configuração experimental [`network.tlsTerminate`](https://code.claude.com/docs/pt/settings#sandbox-settings) termina TLS no proxy para [substituição de credenciais `mask`](#protect-credentials), mas não adiciona filtragem de conteúdo. Você é responsável por garantir que apenas domínios confiáveis sejam permitidos em sua política.

<Warning>
  Permitir domínios amplos como `github.com` pode criar caminhos para exfiltração de dados. Como o proxy toma sua decisão de permissão do nome de host fornecido pelo cliente sem inspecionar TLS, código executado dentro do sandbox pode potencialmente usar [domain fronting](https://en.wikipedia.org/wiki/Domain_fronting) ou técnicas similares para alcançar hosts fora da allowlist. Se seu modelo de ameaça exigir garantias mais fortes, configure um [custom proxy](#custom-proxy-configuration) que termine TLS e inspecione tráfego, e instale seu certificado CA dentro do sandbox. Isolamento de rede mais forte e consciente de TLS é uma área ativa de desenvolvimento.
</Warning>

* **Escalação de privilégio via Unix sockets**: a configuração `allowUnixSockets` pode inadvertidamente conceder acesso a serviços poderosos do sistema que poderiam levar a bypasses de sandbox. Por exemplo, permitir acesso a `/var/run/docker.sock` efetivamente concede acesso ao sistema host através do socket Docker. Considere cuidadosamente quaisquer Unix sockets que você permita através do sandbox.
* **Escalação de permissão de sistema de arquivos**: permissões de escrita de sistema de arquivos excessivamente amplas podem habilitar ataques de escalação de privilégio. Permitir escritas em diretórios contendo executáveis em `$PATH`, diretórios de configuração do sistema ou arquivos de configuração de shell do usuário como `.bashrc` ou `.zshrc` pode levar a execução de código em diferentes contextos de segurança quando outros usuários ou processos do sistema acessam esses arquivos.
* **Força do sandbox Linux**: a implementação Linux fornece isolamento forte de sistema de arquivos e rede, mas inclui um modo `enableWeakerNestedSandbox` que o habilita a funcionar dentro de ambientes Docker sem namespaces privilegiados, ou em hosts Linux onde namespaces de usuário sem privilégios são desabilitados por sysctl. Esta opção enfraquece consideravelmente a segurança e deve ser usada apenas quando isolamento adicional é de outra forma imposto.
* **Apple Events no macOS**: o sandbox macOS bloqueia Apple Events por padrão. A configuração `allowAppleEvents` remove essa restrição para que ferramentas como `open` e `osascript` funcionem, mas remove isolamento de execução de código: comandos em sandbox podem iniciar outras aplicações sem sandbox sem nenhum prompt do usuário, e podem enviar comandos AppleScript para aplicações em execução, sujeito ao prompt de consentimento de automação macOS por aplicativo (TCC). Isso é apenas honrado a partir de configurações de usuário, gerenciadas ou CLI. Configurações de projeto não podem habilitá-lo.
* **Arquivos de configurações protegidos**: o sandbox automaticamente nega acesso de escrita aos arquivos `settings.json` do Claude Code em cada escopo e ao diretório de configurações gerenciadas, portanto um comando em sandbox não pode modificar sua própria política.

<h3 id="platform-and-tool-compatibility">
  Compatibilidade de plataforma e ferramentas
</h3>

* **Suporte de plataforma**: suporta macOS, Linux e WSL2. WSL1 e Windows nativo não são suportados.
* **Overhead de desempenho**: mínimo, mas algumas operações de sistema de arquivos podem ser ligeiramente mais lentas.
* **Compatibilidade de ferramentas**: algumas ferramentas que exigem padrões de acesso específicos do sistema podem precisar de ajustes de configuração, ou podem precisar ser executadas fora do sandbox.

<h3 id="scope">
  Escopo
</h3>

O sandbox isola subprocessos Bash. Outras ferramentas operam sob limites diferentes:

* **Ferramentas de arquivo integradas**: Read, Edit e Write usam o sistema de permissão diretamente em vez de serem executadas através do sandbox. Consulte [permissions](https://code.claude.com/docs/pt/permissions).
* **Computer use**: quando Claude abre aplicativos e controla sua tela, ele é executado em seu desktop real em vez de em um ambiente isolado. Prompts de permissão por aplicativo controlam cada aplicativo. Consulte [computer use in the CLI](https://code.claude.com/docs/pt/computer-use) ou [computer use in Desktop](https://code.claude.com/docs/pt/desktop#let-claude-use-your-computer).
* **Variáveis de ambiente**: comandos Bash em sandbox herdam o ambiente do processo pai por padrão, incluindo quaisquer credenciais definidas lá. Use [`sandbox.credentials`](#protect-credentials) para remover ou mascarar variáveis específicas para comandos em sandbox, ou defina [`CLAUDE_CODE_SUBPROCESS_ENV_SCRUB`](https://code.claude.com/docs/pt/env-vars) para remover credenciais do Anthropic e do provedor de nuvem de todos os subprocessos.
* **Subagents**: [subagents](https://code.claude.com/docs/pt/sub-agents) são executados no mesmo processo que a sessão pai e usam a mesma configuração de sandbox. Comandos Bash dentro de um subagent são colocados em sandbox quando sandboxing está habilitado na sessão pai.

<Warning>
  Sandboxing eficaz requer isolamento tanto de sistema de arquivos quanto de rede. Sem isolamento de rede, um agente comprometido poderia exfiltrar arquivos sensíveis como chaves SSH. Sem isolamento de sistema de arquivos, um agente comprometido poderia fazer backdoor de recursos do sistema para obter acesso à rede. Quando você amplia os padrões, verifique que um caminho `allowWrite`, uma entrada `allowedDomains` ampla ou uma exceção `excludedCommands` não desfaz uma restrição no outro lado.
</Warning>

<h2 id="see-also">
  Veja também
</h2>

* [Sandbox environments](https://code.claude.com/docs/pt/sandbox-environments): compare o sandbox integrado com dev containers, containers e VMs
* [Security](https://code.claude.com/docs/pt/security): recursos de segurança abrangentes e melhores práticas
* [Permissions](https://code.claude.com/docs/pt/permissions): configuração de permissão e controle de acesso
* [Settings](https://code.claude.com/docs/pt/settings): referência de configuração completa
* [CLI reference](https://code.claude.com/docs/pt/cli-reference): opções de linha de comando
