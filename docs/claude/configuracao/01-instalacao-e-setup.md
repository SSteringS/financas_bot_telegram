<!-- Fonte: https://code.claude.com/docs/pt/setup | Baixado em: 2026-08-06 | slug: setup -->

# Configuração avançada

> Requisitos do sistema, instalação específica da plataforma, gerenciamento de versão e desinstalação do Claude Code.

Esta página cobre requisitos do sistema, detalhes de instalação específicos da plataforma, atualizações e desinstalação. Para um guia passo a passo de sua primeira sessão, consulte o [guia de início rápido](https://code.claude.com/docs/pt/quickstart). Se você nunca usou um terminal antes, consulte o [guia de terminal](https://code.claude.com/docs/pt/terminal-guide).

<h2 id="system-requirements">
  Requisitos do sistema
</h2>

Claude Code é executado nas seguintes plataformas e configurações:

* **Sistema operacional**:
  * macOS 13.0+
  * Windows 10 1809+ ou Windows Server 2019+
  * Ubuntu 20.04+
  * Debian 10+
  * Alpine Linux 3.19+
* **Hardware**: 4 GB+ de RAM, processador x64 ou ARM64
* **Rede**: conexão com a internet obrigatória. Consulte [configuração de rede](https://code.claude.com/docs/pt/network-config#network-access-requirements).
* **Shell**: Bash, Zsh, PowerShell ou CMD.
* **Localização**: [países suportados pela Anthropic](https://www.anthropic.com/supported-countries)

<h3 id="additional-dependencies">
  Dependências adicionais
</h3>

* **ripgrep**: geralmente incluído com Claude Code. Se a busca falhar, consulte [solução de problemas de busca e descoberta](https://code.claude.com/docs/pt/troubleshooting#search-and-discovery-issues).

<h2 id="install-claude-code">
  Instalar Claude Code
</h2>

<Tip>
  Prefere uma interface gráfica? O [aplicativo de desktop](https://code.claude.com/docs/pt/desktop-quickstart) permite que você use Claude Code sem o terminal. Baixe-o para [macOS](https://claude.ai/api/desktop/darwin/universal/dmg/latest/redirect?utm_source=claude_code\&utm_medium=docs), [Windows](https://claude.com/download?utm_source=claude_code\&utm_medium=docs) ou [Linux](https://code.claude.com/docs/pt/desktop-linux).

  Novo no terminal? Consulte o [guia de terminal](https://code.claude.com/docs/pt/terminal-guide) para instruções passo a passo.
</Tip>

To install Claude Code, use one of the following methods:

<Tabs>
  <Tab title="Native Install (Recommended)">
    **macOS, Linux, WSL:**

    ```bash theme={null}
    curl -fsSL https://claude.ai/install.sh | bash
    ```

    **Windows PowerShell:**

    ```powershell theme={null}
    irm https://claude.ai/install.ps1 | iex
    ```

    **Windows CMD:**

    ```batch theme={null}
    curl -fsSL https://claude.ai/install.cmd -o install.cmd && install.cmd && del install.cmd
    ```

    If you see `The token '&&' is not a valid statement separator`, you're in PowerShell, not CMD. If you see `'irm' is not recognized as an internal or external command`, you're in CMD, not PowerShell. Your prompt shows `PS C:\` when you're in PowerShell and `C:\` without the `PS` when you're in CMD.

    If the install command fails with `syntax error near unexpected token '<'`, a `403`, or another curl error, see [Troubleshoot installation](https://code.claude.com/docs/en/troubleshoot-install#find-your-error) to match the error to a fix and for alternative install methods.

    [Git for Windows](https://git-scm.com/downloads/win) is recommended on native Windows so Claude Code can use the Bash tool. If Git for Windows is not installed, Claude Code uses PowerShell as the shell tool instead. WSL setups do not need Git for Windows.

    <Info>
      Native installations automatically update in the background to keep you on the latest version.
    </Info>
  </Tab>

  <Tab title="Homebrew">
    ```bash theme={null}
    brew install --cask claude-code
    ```

    Homebrew offers two casks. `claude-code` tracks the stable release channel, which is typically about a week behind and skips releases with major regressions. `claude-code@latest` tracks the latest channel and receives new versions as soon as they ship.

    <Info>
      Homebrew installations do not auto-update. Run `brew upgrade claude-code` or `brew upgrade claude-code@latest`, depending on which cask you installed, to get the latest features and security fixes.
    </Info>
  </Tab>

  <Tab title="WinGet">
    ```powershell theme={null}
    winget install Anthropic.ClaudeCode
    ```

    <Info>
      WinGet installations do not auto-update. Run `winget upgrade Anthropic.ClaudeCode` periodically to get the latest features and security fixes.
    </Info>
  </Tab>
</Tabs>

You can also install with [apt, dnf, or apk](https://code.claude.com/docs/en/setup#install-with-linux-package-managers) on Debian, Fedora, RHEL, and Alpine.

Após a conclusão da instalação, abra um terminal no projeto em que deseja trabalhar e inicie Claude Code:

```bash theme={null}
claude
```

Se você encontrar algum problema durante a instalação, consulte [Solucionar problemas de instalação e login](https://code.claude.com/docs/pt/troubleshoot-install).

<h3 id="set-up-on-windows">
  Configurar no Windows
</h3>

Você pode executar Claude Code nativamente no Windows ou dentro do WSL. Escolha com base em onde seus projetos estão localizados e quais recursos você precisa:

| Opção          | Requer                                                                  | [Sandboxing](https://code.claude.com/docs/pt/sandboxing) | Quando usar                                                    |
| -------------- | ----------------------------------------------------------------------- | ---------------------------- | -------------------------------------------------------------- |
| Windows nativo | Nenhum; [Git for Windows](https://git-scm.com/downloads/win) é opcional | Não suportado                | Projetos e ferramentas nativas do Windows                      |
| WSL 2          | WSL 2 habilitado                                                        | Suportado                    | Cadeias de ferramentas Linux ou execução de comando em sandbox |
| WSL 1          | WSL 1 habilitado                                                        | Não suportado                | Se WSL 2 não estiver disponível                                |

**Opção 1: Windows nativo**

Execute o comando de instalação a partir do PowerShell ou CMD. Você não precisa executar como Administrador. Instalar [Git for Windows](https://git-scm.com/downloads/win) é opcional. Ele habilita a [ferramenta Bash](https://code.claude.com/docs/pt/tools-reference#bash-tool-behavior) fornecendo Git Bash.

Se você instalar a partir do PowerShell ou CMD apenas afeta qual comando de instalação você executa. Seu prompt mostra `PS C:\Users\SeuNome>` no PowerShell e `C:\Users\SeuNome>` sem o `PS` no CMD. Se você é novo no terminal, o [guia de terminal](https://code.claude.com/docs/pt/terminal-guide#windows) orienta cada etapa.

Após a instalação, inicie `claude` a partir de qualquer terminal.

* **Sem Git for Windows**, Claude Code executa comandos shell através da [ferramenta PowerShell](https://code.claude.com/docs/pt/tools-reference#powershell-tool).
* **Com Git for Windows**, Claude Code usa Git Bash para a [ferramenta Bash](https://code.claude.com/docs/pt/tools-reference#bash-tool-behavior). Se Claude Code não conseguir encontrar Git Bash, defina o caminho em seu [arquivo settings.json](https://code.claude.com/docs/pt/settings):

  ```json theme={null}
  {
    "env": {
      "CLAUDE_CODE_GIT_BASH_PATH": "C:\\Program Files\\Git\\bin\\bash.exe"
    }
  }
  ```

Quando Git for Windows está instalado, a ferramenta PowerShell está sendo lançada progressivamente como uma opção adicional ao lado do Bash. Defina `CLAUDE_CODE_USE_POWERSHELL_TOOL=1` para aceitar ou `0` para recusar. Consulte [ferramenta PowerShell](https://code.claude.com/docs/pt/tools-reference#powershell-tool) para configuração e limitações.

**Opção 2: WSL**

Abra sua distribuição WSL e execute o instalador Linux a partir das [instruções de instalação](#install-claude-code) acima. Você instala e inicia `claude` dentro do terminal WSL, não a partir do PowerShell ou CMD.

<h3 id="alpine-linux-and-musl-based-distributions">
  Alpine Linux e distribuições baseadas em musl
</h3>

O instalador nativo no Alpine e outras distribuições baseadas em musl/uClibc requer `libgcc`, `libstdc++` e `ripgrep`. Instale-os usando o gerenciador de pacotes da sua distribuição e defina `USE_BUILTIN_RIPGREP=0`.

Este exemplo instala os pacotes necessários no Alpine:

```bash theme={null}
apk add libgcc libstdc++ ripgrep
```

Em seguida, defina `USE_BUILTIN_RIPGREP` como `0` em seu arquivo [`settings.json`](https://code.claude.com/docs/pt/settings#available-settings):

```json theme={null}
{
  "env": {
    "USE_BUILTIN_RIPGREP": "0"
  }
}
```

<h2 id="verify-your-installation">
  Verificar sua instalação
</h2>

Após a instalação, confirme que Claude Code está funcionando:

```bash theme={null}
claude --version
```

Se isso falhar com `command not found` ou outro erro, consulte [Solucionar problemas de instalação e login](https://code.claude.com/docs/pt/troubleshoot-install).

Para uma verificação mais detalhada de sua instalação e configuração, execute [`claude doctor`](https://code.claude.com/docs/pt/troubleshooting#get-more-help):

```bash theme={null}
claude doctor
```

<h2 id="authenticate">
  Autenticar
</h2>

Claude Code requer uma conta Pro, Max, Team, Enterprise ou Console. O plano gratuito do Claude.ai não inclui acesso ao Claude Code. Você também pode usar Claude Code com um provedor de API de terceiros como [Amazon Bedrock](https://code.claude.com/docs/pt/amazon-bedrock), [Google Cloud's Agent Platform](https://code.claude.com/docs/pt/google-vertex-ai) ou [Microsoft Foundry](https://code.claude.com/docs/pt/microsoft-foundry).

Após a instalação, faça login executando `claude` e seguindo os prompts do navegador. Consulte [Autenticação](https://code.claude.com/docs/pt/authentication) para todos os tipos de conta e opções de configuração de equipe.

<h2 id="update-claude-code">
  Atualizar Claude Code
</h2>

As instalações nativas são atualizadas automaticamente em segundo plano. Você pode [configurar o canal de lançamento](#configure-release-channel) para controlar se recebe atualizações imediatamente ou em um cronograma estável com atraso, ou [desabilitar atualizações automáticas](#disable-auto-updates) completamente. As instalações do Homebrew, WinGet e [gerenciador de pacotes Linux](#install-with-linux-package-managers) requerem atualizações manuais por padrão.

<h3 id="auto-updates">
  Atualizações automáticas
</h3>

Claude Code verifica atualizações na inicialização e periodicamente durante a execução. As atualizações são baixadas e instaladas em segundo plano, depois entram em vigor na próxima vez que você inicia Claude Code.

Execute `claude doctor` para ver o resultado da tentativa de atualização mais recente.

No macOS e Linux, o instalador nativo gerencia o inicializador em `~/.local/bin/claude` como um symlink para `~/.local/share/claude/versions/`. Se você substituir esse inicializador com seu próprio script ou symlink, a auto-atualização e `claude update` o deixam no lugar: novas versões ainda são instaladas no diretório `versions/`, e seu inicializador decide qual versão é executada. Antes da v2.1.207, o auto-atualizador substituía um inicializador personalizado nesse caminho com seu próprio symlink a cada atualização.

Com um inicializador personalizado, Claude Code também mantém todas as versões instaladas no disco porque não consegue determinar qual versão o inicializador precisa. `claude doctor` relata um inicializador que o instalador nativo não criou.

Para permitir que Claude Code gerencie o inicializador novamente, remova `~/.local/bin/claude` e execute `claude update`.

Se uma instalação global do npm não conseguir fazer auto-atualização porque o diretório global do npm não é gravável, Claude Code mostra um aviso único na inicialização, e `claude doctor` lista as correções disponíveis. Consulte [erros de permissão durante a instalação](https://code.claude.com/docs/pt/troubleshoot-install#permission-errors-during-installation) para obter detalhes.

<Note>
  As instalações do Homebrew, WinGet, apt, dnf e apk não são atualizadas automaticamente por padrão; veja abaixo para optar por Homebrew e WinGet. Para atualizar o Homebrew manualmente, execute `brew upgrade claude-code` ou `brew upgrade claude-code@latest`, dependendo de qual cask você instalou. Para WinGet, execute `winget upgrade Anthropic.ClaudeCode`. Para gerenciadores de pacotes Linux, consulte os comandos de atualização em [Instalar com gerenciadores de pacotes Linux](#install-with-linux-package-managers).

  Para que Claude Code execute o comando de atualização para você no Homebrew ou WinGet, defina [`CLAUDE_CODE_PACKAGE_MANAGER_AUTO_UPDATE`](https://code.claude.com/docs/pt/env-vars) como `1`. Claude Code então executa a atualização em segundo plano quando uma nova versão está disponível e mostra um prompt de reinicialização em caso de sucesso. A atualização visa apenas o pacote Claude Code e não afeta outro software que você tenha instalado.

  No WinGet, a atualização pode falhar enquanto Claude Code está em execução porque o Windows bloqueia o executável. Nesse caso, Claude Code mostra o comando manual em vez disso. apt, dnf e apk continuam a exigir uma atualização manual porque esses comandos precisam de privilégios elevados.

  **Problema conhecido:** Claude Code pode notificá-lo sobre atualizações antes que a nova versão esteja disponível nesses gerenciadores de pacotes. Se uma atualização falhar, aguarde e tente novamente mais tarde.

  O Homebrew mantém versões antigas no disco após atualizações. Execute `brew cleanup` periodicamente para recuperar espaço em disco.
</Note>

<h3 id="configure-release-channel">
  Configurar canal de lançamento
</h3>

Controle qual canal de lançamento Claude Code segue para atualizações automáticas e `claude update` com a configuração `autoUpdatesChannel`:

* `"latest"`, o padrão: receba novos recursos assim que forem lançados
* `"stable"`: use uma versão que normalmente tem cerca de uma semana de idade, pulando lançamentos com regressões importantes

Configure isso via `/config` → **Canal de atualização automática**, ou adicione-o ao seu [arquivo settings.json](https://code.claude.com/docs/pt/settings):

```json theme={null}
{
  "autoUpdatesChannel": "stable"
}
```

Para implantações empresariais, você pode impor um canal de lançamento consistente em toda a sua organização usando [configurações gerenciadas](https://code.claude.com/docs/pt/permissions#managed-settings).

As instalações do Homebrew escolhem um canal pelo nome do cask em vez dessa configuração: `claude-code` rastreia estável e `claude-code@latest` rastreia mais recente.

<h3 id="pin-a-minimum-version">
  Fixar uma versão mínima
</h3>

A configuração `minimumVersion` estabelece um piso. As atualizações automáticas em segundo plano e `claude update` recusam instalar qualquer versão abaixo desse valor, portanto mudar para o canal `"stable"` não faz downgrade se você já estiver em um build `"latest"` mais recente.

Mudar de `"latest"` para `"stable"` via `/config` solicita que você escolha ficar na versão atual ou permitir o downgrade. Escolher ficar define `minimumVersion` para essa versão. Mudar de volta para `"latest"` limpa isso.

Adicione-o ao seu [arquivo settings.json](https://code.claude.com/docs/pt/settings) para fixar um piso explicitamente:

```json theme={null}
{
  "autoUpdatesChannel": "stable",
  "minimumVersion": "2.1.100"
}
```

Em [configurações gerenciadas](https://code.claude.com/docs/pt/permissions#managed-settings), isso impõe um mínimo em toda a organização que as configurações de usuário e projeto não podem substituir.

O pino `minimumVersion` apenas restringe atualizações. Para fazer Claude Code recusar iniciar fora de um intervalo de versão, use as configurações gerenciadas `requiredMinimumVersion` e `requiredMaximumVersion` em vez disso. As atualizações também respeitam o teto `requiredMaximumVersion`. Consulte [configurações disponíveis](https://code.claude.com/docs/pt/settings#available-settings).

<h3 id="disable-auto-updates">
  Desabilitar atualizações automáticas
</h3>

Defina `DISABLE_AUTOUPDATER` como `"1"` na chave `env` do seu arquivo [`settings.json`](https://code.claude.com/docs/pt/settings#available-settings):

```json theme={null}
{
  "env": {
    "DISABLE_AUTOUPDATER": "1"
  }
}
```

`DISABLE_AUTOUPDATER` apenas interrompe a verificação em segundo plano; `claude update` e `claude install` ainda funcionam. Para bloquear todos os caminhos de atualização, incluindo atualizações manuais, defina [`DISABLE_UPDATES`](https://code.claude.com/docs/pt/env-vars) em vez disso. Use isso quando você distribuir Claude Code através de seus próprios canais e precisar que os usuários permaneçam na versão que você fornece.

<h3 id="update-manually">
  Atualizar manualmente
</h3>

Para aplicar uma atualização imediatamente sem aguardar a próxima verificação em segundo plano, execute:

```bash theme={null}
claude update
```

<h2 id="advanced-installation-options">
  Opções avançadas de instalação
</h2>

Essas opções são para fixação de versão, gerenciadores de pacotes Linux, npm e verificação da integridade do binário.

<h3 id="install-a-specific-version">
  Instalar uma versão específica
</h3>

O instalador nativo aceita um número de versão específico ou um canal de lançamento (`latest` ou `stable`). O canal que você escolhe no momento da instalação se torna seu padrão para atualizações automáticas. Consulte [configurar canal de lançamento](#configure-release-channel) para mais informações.

Para instalar a versão mais recente (padrão):

<Tabs>
  <Tab title="macOS, Linux, WSL">
    ```bash theme={null}
    curl -fsSL https://claude.ai/install.sh | bash
    ```
  </Tab>

  <Tab title="Windows PowerShell">
    ```powershell theme={null}
    irm https://claude.ai/install.ps1 | iex
    ```
  </Tab>

  <Tab title="Windows CMD">
    ```batch theme={null}
    curl -fsSL https://claude.ai/install.cmd -o install.cmd && install.cmd && del install.cmd
    ```
  </Tab>
</Tabs>

Para instalar a versão estável:

<Tabs>
  <Tab title="macOS, Linux, WSL">
    ```bash theme={null}
    curl -fsSL https://claude.ai/install.sh | bash -s stable
    ```
  </Tab>

  <Tab title="Windows PowerShell">
    ```powershell theme={null}
    & ([scriptblock]::Create((irm https://claude.ai/install.ps1))) stable
    ```
  </Tab>

  <Tab title="Windows CMD">
    ```batch theme={null}
    curl -fsSL https://claude.ai/install.cmd -o install.cmd && install.cmd stable && del install.cmd
    ```
  </Tab>
</Tabs>

Para instalar um número de versão específico:

<Tabs>
  <Tab title="macOS, Linux, WSL">
    ```bash theme={null}
    curl -fsSL https://claude.ai/install.sh | bash -s 2.1.89
    ```
  </Tab>

  <Tab title="Windows PowerShell">
    ```powershell theme={null}
    & ([scriptblock]::Create((irm https://claude.ai/install.ps1))) 2.1.89
    ```
  </Tab>

  <Tab title="Windows CMD">
    ```batch theme={null}
    curl -fsSL https://claude.ai/install.cmd -o install.cmd && install.cmd 2.1.89 && del install.cmd
    ```
  </Tab>
</Tabs>

<h3 id="install-with-linux-package-managers">
  Instalar com gerenciadores de pacotes Linux
</h3>

Claude Code publica repositórios apt, dnf e apk assinados. Cada repositório oferece dois canais: `stable` oferece uma versão que é tipicamente cerca de uma semana antiga, pulando lançamentos com regressões maiores, e `latest` oferece cada lançamento assim que é lançado. Os comandos abaixo configuram o canal `stable`, que se adequa à maioria dos usuários; cada aba também mostra a URL do repositório `latest`. As instalações do gerenciador de pacotes não são atualizadas automaticamente através do Claude Code; as atualizações chegam através do seu fluxo de trabalho de atualização do sistema normal.

Todos os repositórios são assinados com a [chave de assinatura de lançamento do Claude Code](#binary-integrity-and-code-signing). Antes de confiar na chave, verifique-a conforme descrito em cada aba.

<Tabs>
  <Tab title="apt">
    Para Debian e Ubuntu. Os comandos de instalação abaixo baixam a chave de assinatura com `curl`, que instalações recentes de Debian e Ubuntu podem não incluir. Se o download falhar com `sudo: curl: command not found`, instale curl primeiro:

    ```bash theme={null}
    sudo apt install curl
    ```

    Os comandos a seguir configuram o canal `stable`:

    ```bash theme={null}
    sudo install -d -m 0755 /etc/apt/keyrings
    sudo curl -fsSL https://downloads.claude.ai/keys/claude-code.asc \
      -o /etc/apt/keyrings/claude-code.asc
    echo "deb [signed-by=/etc/apt/keyrings/claude-code.asc] https://downloads.claude.ai/claude-code/apt/stable stable main" \
      | sudo tee /etc/apt/sources.list.d/claude-code.list
    sudo apt update
    sudo apt install claude-code
    ```

    Para usar o canal `latest` em vez disso, tanto o caminho da URL quanto o nome do suite mudam. Use esta linha `deb`:

    ```bash theme={null}
    echo "deb [signed-by=/etc/apt/keyrings/claude-code.asc] https://downloads.claude.ai/claude-code/apt/latest latest main" \
      | sudo tee /etc/apt/sources.list.d/claude-code.list
    ```

    Verifique a impressão digital da chave GPG antes de confiar nela: `gpg --show-keys /etc/apt/keyrings/claude-code.asc` deve relatar `31DD DE24 DDFA B679 F42D 7BD2 BAA9 29FF 1A7E CACE`.

    Para atualizar mais tarde, execute `sudo apt update && sudo apt upgrade claude-code`.
  </Tab>

  <Tab title="dnf">
    Para Fedora e RHEL. Os comandos a seguir configuram o canal `stable`:

    ```bash theme={null}
    sudo tee /etc/yum.repos.d/claude-code.repo <<'EOF'
    [claude-code]
    name=Claude Code
    baseurl=https://downloads.claude.ai/claude-code/rpm/stable
    enabled=1
    gpgcheck=1
    gpgkey=https://downloads.claude.ai/keys/claude-code.asc
    EOF
    sudo dnf install claude-code
    ```

    Para usar o canal `latest` em vez disso, defina `baseurl` para o repositório `latest`:

    ```ini theme={null}
    baseurl=https://downloads.claude.ai/claude-code/rpm/latest
    ```

    dnf baixa a chave na primeira instalação e solicita que você confirme a impressão digital. Verifique se ela corresponde a `31DD DE24 DDFA B679 F42D 7BD2 BAA9 29FF 1A7E CACE` antes de aceitar.

    Para atualizar mais tarde, execute `sudo dnf upgrade claude-code`.
  </Tab>

  <Tab title="apk">
    Para Alpine Linux. Os comandos a seguir configuram o canal `stable`:

    ```sh theme={null}
    wget -O /etc/apk/keys/claude-code.rsa.pub \
      https://downloads.claude.ai/keys/claude-code.rsa.pub
    echo "https://downloads.claude.ai/claude-code/apk/stable" >> /etc/apk/repositories
    apk add claude-code
    ```

    Para mudar para o canal `latest`, remova a linha do repositório `stable` e adicione o repositório `latest`:

    ```sh theme={null}
    sed -i '\|downloads.claude.ai/claude-code/apk/stable|d' /etc/apk/repositories
    echo "https://downloads.claude.ai/claude-code/apk/latest" >> /etc/apk/repositories
    ```

    Verifique a chave baixada com `sha256sum /etc/apk/keys/claude-code.rsa.pub`, que deve relatar `395759c1f7449ef4cdef305a42e820f3c766d6090d142634ebdb049f113168b6`.

    Para atualizar mais tarde, execute `apk update && apk upgrade claude-code`.
  </Tab>
</Tabs>

<h3 id="install-with-npm">
  Instalar com npm
</h3>

Você também pode instalar Claude Code como um pacote npm global. A partir da v2.1.198, o pacote npm requer [Node.js 22 ou posterior](https://nodejs.org/en/download). Em uma versão mais antiga do Node.js, npm imprime um aviso `EBADENGINE` durante a instalação em vez de falhar; a instalação é concluída e `claude` ainda funciona, já que o pacote baixa um binário nativo que não usa seu Node.js em tempo de execução.

```bash theme={null}
npm install -g @anthropic-ai/claude-code
```

O pacote npm instala o mesmo binário nativo que o instalador autônomo. npm puxa o binário através de uma dependência opcional por plataforma como `@anthropic-ai/claude-code-darwin-arm64`, e uma etapa postinstall o vincula no lugar. O binário `claude` instalado não invoca Node em si.

As plataformas de instalação npm suportadas são `darwin-arm64`, `darwin-x64`, `linux-x64`, `linux-arm64`, `linux-x64-musl`, `linux-arm64-musl`, `win32-x64` e `win32-arm64`. Seu gerenciador de pacotes deve permitir dependências opcionais. Consulte [solução de problemas](https://code.claude.com/docs/pt/troubleshoot-install#native-binary-not-found-after-npm-install) se o binário estiver faltando após a instalação.

Para atualizar uma instalação npm, execute `npm install -g @anthropic-ai/claude-code@latest`. Evite `npm update -g`, que respeita o intervalo semver da instalação original e pode não movê-lo para a versão mais recente.

<Warning>
  NÃO use `sudo npm install -g` pois isso pode levar a problemas de permissão e riscos de segurança. Se você encontrar erros de permissão, consulte [solução de problemas de erros de permissão](https://code.claude.com/docs/pt/troubleshoot-install#permission-errors-during-installation).
</Warning>

<h3 id="binary-integrity-and-code-signing">
  Integridade binária e assinatura de código
</h3>

Cada lançamento publica um `manifest.json` contendo checksums SHA256 para cada binário de plataforma. O manifesto é assinado com uma chave GPG da Anthropic, portanto verificar a assinatura no manifesto verifica transitivamente cada binário que ele lista.

<h4 id="verify-the-manifest-signature">
  Verificar a assinatura do manifesto
</h4>

As etapas 1-3 requerem um shell POSIX com `gpg` e `curl`. No Windows, execute-as no Git Bash ou WSL. A etapa 4 inclui uma opção PowerShell.

<Steps>
  <Step title="Baixar e importar a chave pública">
    A chave de assinatura de lançamento é publicada em uma URL fixa.

    ```bash theme={null}
    curl -fsSL https://downloads.claude.ai/keys/claude-code.asc | gpg --import
    ```

    Exiba a impressão digital da chave importada.

    ```bash theme={null}
    gpg --fingerprint security@anthropic.com
    ```

    Confirme que a saída inclui esta impressão digital:

    ```text theme={null}
    31DD DE24 DDFA B679 F42D  7BD2 BAA9 29FF 1A7E CACE
    ```
  </Step>

  <Step title="Baixar o manifesto e a assinatura">
    Defina `VERSION` para o lançamento que você deseja verificar.

    ```bash theme={null}
    REPO=https://downloads.claude.ai/claude-code-releases
    VERSION=2.1.89
    curl -fsSLO "$REPO/$VERSION/manifest.json"
    curl -fsSLO "$REPO/$VERSION/manifest.json.sig"
    ```
  </Step>

  <Step title="Verificar a assinatura">
    Verifique a assinatura destacada contra o manifesto.

    ```bash theme={null}
    gpg --verify manifest.json.sig manifest.json
    ```

    Um resultado válido relata `Good signature from "Anthropic Claude Code Release Signing <security@anthropic.com>"`.

    `gpg` também imprime `WARNING: This key is not certified with a trusted signature!` para qualquer chave recém-importada. Isso é esperado. A linha `Good signature` confirma que a verificação criptográfica passou. A comparação de impressão digital na Etapa 1 confirma que a chave em si é autêntica.
  </Step>

  <Step title="Verificar o binário contra o manifesto">
    Compare o checksum SHA256 do binário com o valor listado em `platforms.<platform>.checksum` em `manifest.json`. Os comandos abaixo assumem um binário `claude` no diretório atual. Para verificar um binário nativo instalado em vez disso, execute o comando contra `~/.local/share/claude/versions/VERSION`, substituindo VERSION pelo lançamento que você definiu na Etapa 2.

    <Tabs>
      <Tab title="Linux">
        ```bash theme={null}
        sha256sum claude
        ```
      </Tab>

      <Tab title="macOS">
        ```bash theme={null}
        shasum -a 256 claude
        ```
      </Tab>

      <Tab title="Windows PowerShell">
        ```powershell theme={null}
        (Get-FileHash claude.exe -Algorithm SHA256).Hash.ToLower()
        ```
      </Tab>
    </Tabs>
  </Step>
</Steps>

<Note>
  As assinaturas de manifesto estão disponíveis para lançamentos de `2.1.89` em diante. Lançamentos anteriores publicam checksums em `manifest.json` sem uma assinatura destacada.
</Note>

<h4 id="platform-code-signatures">
  Assinaturas de código de plataforma
</h4>

Além do manifesto assinado, os binários individuais carregam assinaturas de código nativas da plataforma onde suportado.

* **macOS**: assinado por "Anthropic PBC" e autenticado pela Apple. Verifique com `codesign --verify --verbose ./claude`.
* **Windows**: assinado por "Anthropic, PBC". Verifique com `Get-AuthenticodeSignature .\claude.exe`.
* **Linux**: os binários não são individualmente assinados com código. Se você baixar diretamente do bucket `claude-code-releases` ou usar o instalador nativo, verifique a integridade com a assinatura de manifesto acima. Se você instalar com [apt, dnf ou apk](#install-with-linux-package-managers), seu gerenciador de pacotes verifica assinaturas automaticamente usando a chave de assinatura do repositório.

<h2 id="uninstall-claude-code">
  Desinstalar Claude Code
</h2>

Para remover Claude Code, siga as instruções para seu método de instalação. Se `claude` ainda for executado depois, você provavelmente tem uma segunda instalação ou um alias de shell residual de um instalador mais antigo. Consulte [Verificar instalações conflitantes](https://code.claude.com/docs/pt/troubleshoot-install#check-for-conflicting-installations) para encontrá-lo e removê-lo.

<h3 id="native-installation">
  Instalação nativa
</h3>

Remova o binário Claude Code e os arquivos de versão:

<Tabs>
  <Tab title="macOS, Linux, WSL">
    ```bash theme={null}
    rm -f ~/.local/bin/claude
    rm -rf ~/.local/share/claude
    ```
  </Tab>

  <Tab title="Windows PowerShell">
    ```powershell theme={null}
    Remove-Item -Path "$env:USERPROFILE\.local\bin\claude.exe" -Force
    Remove-Item -Path "$env:USERPROFILE\.local\share\claude" -Recurse -Force
    ```
  </Tab>
</Tabs>

<h3 id="homebrew-installation">
  Instalação do Homebrew
</h3>

Remova o cask do Homebrew que você instalou. Se você instalou o cask estável:

```bash theme={null}
brew uninstall --cask claude-code
```

Se você instalou o cask mais recente:

```bash theme={null}
brew uninstall --cask claude-code@latest
```

<h3 id="winget-installation">
  Instalação do WinGet
</h3>

Remova o pacote WinGet:

```powershell theme={null}
winget uninstall Anthropic.ClaudeCode
```

<h3 id="apt-/-dnf-/-apk">
  apt / dnf / apk
</h3>

Remova o pacote e a configuração do repositório:

<Tabs>
  <Tab title="apt">
    ```bash theme={null}
    sudo apt remove claude-code
    sudo rm /etc/apt/sources.list.d/claude-code.list /etc/apt/keyrings/claude-code.asc
    ```
  </Tab>

  <Tab title="dnf">
    ```bash theme={null}
    sudo dnf remove claude-code
    sudo rm /etc/yum.repos.d/claude-code.repo
    ```
  </Tab>

  <Tab title="apk">
    ```sh theme={null}
    apk del claude-code
    sed -i '\|downloads.claude.ai/claude-code/apk|d' /etc/apk/repositories
    rm /etc/apk/keys/claude-code.rsa.pub
    ```
  </Tab>
</Tabs>

<h3 id="npm">
  npm
</h3>

Remova o pacote npm global:

```bash theme={null}
npm uninstall -g @anthropic-ai/claude-code
```

<h3 id="remove-configuration-files">
  Remover arquivos de configuração
</h3>

<Warning>
  Remover arquivos de configuração excluirá todas as suas configurações, ferramentas permitidas, configurações do servidor MCP e histórico de sessão.
</Warning>

A extensão VS Code, o plugin JetBrains e o aplicativo de desktop também escrevem em `~/.claude/`. Se algum deles ainda estiver instalado, o diretório será recriado na próxima vez que for executado. Para remover Claude Code completamente, desinstale a [extensão VS Code](https://code.claude.com/docs/pt/vs-code#uninstall-the-extension), o plugin JetBrains e o aplicativo de desktop antes de excluir esses arquivos.

Para remover as configurações e dados em cache do Claude Code:

<Tabs>
  <Tab title="macOS, Linux, WSL">
    ```bash theme={null}
    # Remover configurações de usuário e estado
    rm -rf ~/.claude
    rm ~/.claude.json

    # Remover configurações específicas do projeto (execute a partir do diretório do seu projeto)
    rm -rf .claude
    rm -f .mcp.json
    ```
  </Tab>

  <Tab title="Windows PowerShell">
    ```powershell theme={null}
    # Remover configurações de usuário e estado
    Remove-Item -Path "$env:USERPROFILE\.claude" -Recurse -Force
    Remove-Item -Path "$env:USERPROFILE\.claude.json" -Force

    # Remover configurações específicas do projeto (execute a partir do diretório do seu projeto)
    Remove-Item -Path ".claude" -Recurse -Force
    Remove-Item -Path ".mcp.json" -Force
    ```
  </Tab>
</Tabs>
