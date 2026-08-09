<!-- Fonte: https://code.claude.com/docs/pt/permissions | Baixado em: 2026-08-06 | slug: permissions -->

# Configurar permissões

> Controle o que Claude Code pode acessar e fazer com regras de permissão refinadas, modos e políticas gerenciadas.

Claude Code suporta permissões refinadas para que você possa especificar exatamente o que o agente pode fazer e o que não pode. As configurações de permissão podem ser verificadas no controle de versão e distribuídas para todos os desenvolvedores da sua organização, bem como personalizadas por desenvolvedores individuais.

<h2 id="permission-system">
  Sistema de permissões
</h2>

Claude Code usa um sistema de permissões em camadas para equilibrar poder e segurança:

| Tipo de ferramenta     | Exemplo                   | Aprovação necessária                                                                 | Comportamento de "Sim, não pergunte novamente"     |
| :--------------------- | :------------------------ | :----------------------------------------------------------------------------------- | :------------------------------------------------- |
| Somente leitura        | Leitura de arquivos, Grep | Não, dentro do [diretório de trabalho e diretórios adicionais](#working-directories) | N/A                                                |
| Comandos Bash          | Execução de shell         | Sim, exceto um conjunto integrado de [comandos somente leitura](#read-only-commands) | Permanentemente por diretório de projeto e comando |
| Modificação de arquivo | Edit/Write de arquivos    | Sim                                                                                  | Até o final da sessão                              |

Em um prompt de permissão do Bash ou PowerShell, pressione `Ctrl+E` para mostrar uma explicação do comando: o que ele faz, por que Claude está executando-o e o que pode dar errado, rotulado como **Risco baixo**, **Risco médio** ou **Risco alto**. Claude Code envia o comando e a própria descrição de Claude da chamada para o modelo gerar a explicação apenas quando você pressiona `Ctrl+E`, não em cada prompt. Mostrar a explicação não executa o comando; pressione `Ctrl+E` novamente para ocultá-la.

Para desativar o atalho, defina [`permissionExplainerEnabled`](https://code.claude.com/docs/pt/settings#global-config-settings) como `false` em `~/.claude.json`.

<h2 id="manage-permissions">
  Gerenciar permissões
</h2>

Você pode visualizar e gerenciar as permissões de ferramentas do Claude Code com `/permissions`. Esta interface lista todas as regras de permissão e o arquivo `settings.json` do qual cada regra é originária.

* As regras **Allow** permitem que Claude Code use a ferramenta especificada sem aprovação manual.
* As regras **Ask** solicitam confirmação sempre que Claude Code tenta usar a ferramenta especificada.
* As regras **Deny** impedem que Claude Code use a ferramenta especificada.

As regras são avaliadas em ordem: deny, depois ask, depois allow. A primeira correspondência nessa ordem determina o resultado, e a especificidade da regra não altera a ordem.

Uma regra deny ampla como `Bash(aws *)` bloqueia cada chamada correspondente, incluindo chamadas que também correspondem a uma regra allow mais estreita como `Bash(aws s3 ls)`, portanto uma regra deny não pode carregar exceções de lista de permissões. A mesma precedência se aplica entre ask e allow: uma regra ask correspondente solicita confirmação mesmo quando uma regra allow mais específica também corresponde à mesma chamada.

As regras deny se comportam de forma diferente dependendo se nomeiam uma ferramenta ou definem o escopo de um padrão dentro de uma. Um nome de ferramenta simples como `Bash` remove a ferramenta do contexto do Claude completamente, então Claude nunca a vê. Uma regra com escopo como `Bash(rm *)` deixa a ferramenta disponível e bloqueia chamadas correspondentes quando Claude tenta usá-las.

<Note>
  As regras de permissão são aplicadas pelo Claude Code, não pelo modelo. As instruções em seu prompt ou `CLAUDE.md` moldam o que Claude tenta fazer, mas não alteram o que Claude Code permite. Para conceder ou revogar acesso, use `/permissions`, as regras descritas aqui, um [modo de permissão](https://code.claude.com/docs/pt/permission-modes), ou um [hook PreToolUse](#extend-permissions-with-hooks).
</Note>

<h2 id="permission-modes">
  Modos de permissão
</h2>

Claude Code suporta vários modos de permissão que controlam como ele aprova chamadas de ferramentas. Veja [Modos de permissão](https://code.claude.com/docs/pt/permission-modes) para quando usar cada um. Defina o `defaultMode` em seus [arquivos de configuração](https://code.claude.com/docs/pt/settings#settings-files):

| Modo                | Descrição                                                                                                                                                                                                                                                                                                                                                                                                       |
| :------------------ | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `default`           | Comportamento padrão: solicita permissão no primeiro uso de cada ferramenta. Rotulado como Manual na CLI, nas extensões VS Code e JetBrains, e no aplicativo desktop, e Claude Code aceita `manual` como um alias. O rótulo e o alias requerem Claude Code v2.1.200 ou posterior. O rótulo do aplicativo desktop não depende da sua versão da CLI                                                               |
| `acceptEdits`       | Aceita automaticamente edições de arquivo e comandos comuns do sistema de arquivos como `mkdir`, `touch`, `mv` e `cp` para caminhos no diretório de trabalho ou `additionalDirectories`                                                                                                                                                                                                                         |
| `plan`              | Claude lê arquivos e executa comandos shell somente leitura para explorar, mas não edita seus arquivos de origem. Rotulado como Plan na CLI e na extensão VS Code                                                                                                                                                                                                                                               |
| `auto`              | Aprova automaticamente chamadas de ferramentas com verificações de segurança em segundo plano que verificam se as ações se alinham com sua solicitação                                                                                                                                                                                                                                                          |
| `dontAsk`           | Nega automaticamente ferramentas a menos que pré-aprovadas via `/permissions` ou regras `permissions.allow`. `AskUserQuestion`, ferramentas de conector [sua organização definida como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) e ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) são negadas mesmo se você as permitiu                      |
| `bypassPermissions` | Ignora prompts de permissão, exceto aqueles forçados por regras `ask` explícitas, ferramentas de conector [sua organização definida como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) e ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool). Remoções de diretório raiz e diretório inicial como `rm -rf /` também ainda solicitam como um disjuntor |

<Warning>
  O modo `bypassPermissions` ignora prompts de permissão, incluindo escritas em `.git`, `.config/git`, `.claude`, `.vscode`, `.idea`, `.husky`, `.cargo`, `.devcontainer`, `.yarn` e `.mvn`. Use este modo apenas em ambientes isolados como contêineres ou VMs onde Claude Code não pode causar danos.

  Alguns prompts ainda são acionados neste modo. Regras `ask` explícitas, ferramentas de conector [sua organização definida como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) e ferramentas MCP marcadas [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) ainda solicitam. Remoções direcionadas ao diretório raiz do sistema de arquivos ou diretório inicial, como `rm -rf /` e `rm -rf ~`, também solicitam como um disjuntor contra erro do modelo, incluindo quando o comando contém substituição de comando com `$(...)` ou backticks, ou substituição de processo com `<(...)`. Antes da v2.1.208, apenas a forma simples, como `rm -rf ~` digitado como seu próprio comando, solicitava; comandos que chegavam à remoção através de uma substituição não solicitavam.
</Warning>

Para evitar que o modo `bypassPermissions` ou `auto` seja usado, defina `permissions.disableBypassPermissionsMode` ou `permissions.disableAutoMode` como `"disable"` em qualquer [arquivo de configuração](https://code.claude.com/docs/pt/settings#settings-files). Estes são mais úteis em [configurações gerenciadas](#managed-settings) onde não podem ser substituídos.

<h2 id="permission-rule-syntax">
  Sintaxe de regra de permissão
</h2>

As regras de permissão seguem o formato `Tool` ou `Tool(specifier)`.

<h3 id="match-all-uses-of-a-tool">
  Corresponder todos os usos de uma ferramenta
</h3>

Para corresponder todos os usos de uma ferramenta, use apenas o nome da ferramenta sem parênteses:

| Regra      | Efeito                                              |
| :--------- | :-------------------------------------------------- |
| `Bash`     | Corresponde a todos os comandos Bash                |
| `WebFetch` | Corresponde a todas as solicitações de busca na web |
| `Read`     | Corresponde a todas as leituras de arquivo          |

`Bash(*)` é equivalente a `Bash` e corresponde a todos os comandos Bash. Como uma regra de negação, ambas as formas removem a ferramenta do contexto do Claude.

<h3 id="use-specifiers-for-fine-grained-control">
  Use especificadores para controle refinado
</h3>

Adicione um especificador entre parênteses para corresponder a usos específicos de ferramentas:

| Regra                          | Efeito                                                     |
| :----------------------------- | :--------------------------------------------------------- |
| `Bash(npm run build)`          | Corresponde ao comando exato `npm run build`               |
| `Read(./.env)`                 | Corresponde à leitura do arquivo `.env` no diretório atual |
| `WebFetch(domain:example.com)` | Corresponde a solicitações de busca para example.com       |

<h3 id="match-by-input-parameter">
  Corresponder por parâmetro de entrada
</h3>

As regras de negação e pergunta podem corresponder a um parâmetro de entrada de nível superior em qualquer ferramenta com `Tool(param:value)`. A regra corresponde quando Claude chama a ferramenta com esse parâmetro definido para esse valor exato. Uma regra de permissão para um valor de parâmetro não estabeleceria que a chamada é segura em geral, portanto as regras de permissão continuam a usar a sintaxe de especificador própria de cada ferramenta. Isso funciona para qualquer parâmetro escalar que a ferramenta aceita:

| Regra                          | Corresponde                                            |
| :----------------------------- | :----------------------------------------------------- |
| `Agent(model:opus)`            | Chamadas de Agent que solicitam o nível de modelo Opus |
| `Agent(isolation:worktree)`    | Chamadas de Agent que solicitam um git worktree        |
| `Bash(run_in_background:true)` | Chamadas Bash que executam em segundo plano            |

A correspondência de parâmetros segue estas regras:

* O nome do parâmetro deve ser um campo direto da entrada da ferramenta, como `model` na ferramenta Agent. Campos aninhados dentro de um objeto ou array não são correspondíveis
* Cada regra nomeia um parâmetro. Para controlar tanto `model` quanto `isolation`, escreva duas regras, `Agent(model:opus)` e `Agent(isolation:worktree)`, em vez de combiná-las em uma regra
* O valor suporta `*` como um caractere curinga que corresponde a qualquer sequência de caracteres, portanto `Agent(isolation:*)` corresponde a qualquer valor de isolamento explícito. Sem `*` a correspondência é exata
* Um parâmetro que o modelo omite nunca é correspondido, portanto `Agent(model:*)` não corresponde a uma chamada que deixa `model` não definido
* O valor é comparado com a entrada literal que Claude envia, antes de qualquer normalização. `Agent(model:opus)` corresponde ao alias `opus` mas não a um ID de modelo completo. Execute com [`--verbose`](https://code.claude.com/docs/pt/cli-reference) para ver os nomes e valores exatos dos parâmetros em cada chamada de ferramenta
* O espaço em branco ao redor do dois-pontos é ignorado

Campos que uma ferramenta já corresponde com suas próprias regras de canonicalização não são correspondíveis desta forma: `command` para Bash e PowerShell, `file_path` para Read, Edit e Write, `path` para Grep e Glob, `notebook_path` para NotebookEdit, e `url` para WebFetch. Uma regra como `Bash(command:rm *)` seria contornável por um comando composto, portanto Claude Code a ignora e emite um aviso de inicialização. Use `Bash(rm *)`, `Read(./path)` ou `WebFetch(domain:host)` em vez disso.

<h3 id="wildcard-patterns">
  Padrões com caracteres curinga
</h3>

As regras Bash suportam padrões glob com `*`. Caracteres curinga podem aparecer em qualquer posição no comando. Esta configuração permite comandos npm e git commit enquanto bloqueia git push:

```json theme={null}
{
  "permissions": {
    "allow": [
      "Bash(npm run *)",
      "Bash(git commit *)",
      "Bash(git * main)",
      "Bash(* --version)",
      "Bash(* --help *)"
    ],
    "deny": [
      "Bash(git push *)"
    ]
  }
}
```

O espaço antes de `*` importa: `Bash(ls *)` corresponde a `ls -la` mas não a `lsof`, enquanto `Bash(ls*)` corresponde a ambos. O sufixo `:*` é uma maneira equivalente de escrever um caractere curinga à direita, portanto `Bash(ls:*)` corresponde aos mesmos comandos que `Bash(ls *)`.

O diálogo de permissão escreve a forma separada por espaço quando você seleciona "Sim, não pergunte novamente" para um prefixo de comando. A forma `:*` é reconhecida apenas no final de um padrão. Em um padrão como `Bash(git:* push)`, o dois-pontos é tratado como um caractere literal e não corresponderá a comandos git.

<h3 id="tool-name-wildcards">
  Caracteres curinga no nome da ferramenta
</h3>

As regras de negação e pergunta também aceitam padrões glob na posição do nome da ferramenta. O padrão deve corresponder ao nome completo da ferramenta: `"*"` corresponde a todas as ferramentas, e `"mcp__*"` corresponde a todas as ferramentas MCP em todos os servidores. Uma ferramenta correspondida por uma regra de negação glob com nome simples é removida do contexto do Claude, o mesmo que um nome de ferramenta simples. Esta configuração nega todas as ferramentas MCP:

```json theme={null}
{
  "permissions": {
    "deny": [
      "mcp__*"
    ]
  }
}
```

As regras de permissão aceitam globs de nome de ferramenta apenas após um prefixo literal `mcp__<server>__`. O segmento do servidor deve estar livre de glob para que a regra nomeie um servidor específico que você configurou. `mcp__puppeteer__*` corresponde a todas as ferramentas do servidor `puppeteer`, e `mcp__github__get_*` corresponde às suas ferramentas `get_`. Um glob de permissão desancorado como `"*"`, `"B*"` ou `"mcp__*"` é ignorado com um aviso e não aprova automaticamente nada.

Uma regra de negação ou pergunta cujo nome de ferramenta não corresponde a nenhuma ferramenta conhecida produz um aviso de inicialização para detectar erros de digitação. Nomes de ferramentas contendo `_` ou `*` estão isentos da verificação.

O rótulo mostrado para uma ferramenta na transcrição e diálogo de permissão pode diferir do seu nome canônico. Por exemplo, a ferramenta rotulada `Stop Task` na transcrição tem o nome canônico `TaskStop`. As regras de permissão e [correspondências de hook](https://code.claude.com/docs/pt/hooks) correspondem apenas ao nome canônico, portanto uma regra escrita como `Stop Task` não corresponde. Para regras de negação e pergunta, o aviso de inicialização acima detecta a incompatibilidade. Use os nomes canônicos listados na [referência de ferramentas](https://code.claude.com/docs/pt/tools-reference).

<h2 id="tool-specific-permission-rules">
  Regras de permissão específicas da ferramenta
</h2>

<h3 id="bash">
  Bash
</h3>

As regras de permissão Bash suportam correspondência com caracteres curinga `*`. Caracteres curinga podem aparecer em qualquer posição no comando, incluindo no início, meio ou fim:

* `Bash(npm run build)` corresponde ao comando Bash exato `npm run build`
* `Bash(npm run test *)` corresponde a comandos Bash começando com `npm run test`
* `Bash(npm *)` corresponde a qualquer comando começando com `npm `
* `Bash(* install)` corresponde a qualquer comando terminando com ` install`
* `Bash(git * main)` corresponde a comandos como `git checkout main` e `git log --oneline main`

Um único `*` corresponde a qualquer sequência de caracteres incluindo espaços, portanto um caractere curinga pode abranger múltiplos argumentos. `Bash(git *)` corresponde a `git log --oneline --all`, e `Bash(git * main)` corresponde a `git push origin main` bem como `git merge main`.

Quando `*` aparece no final com um espaço antes dele (como `Bash(ls *)`), ele impõe um limite de palavra, exigindo que o prefixo seja seguido por um espaço ou fim de string. Por exemplo, `Bash(ls *)` corresponde a `ls -la` mas não a `lsof`. Em contraste, `Bash(ls*)` sem espaço corresponde a ambos `ls -la` e `lsof` porque não há restrição de limite de palavra.

<h4 id="compound-commands">
  Comandos compostos
</h4>

<Tip>
  Claude Code está ciente de operadores de shell, portanto uma regra como `Bash(safe-cmd *)` não lhe dará permissão para executar o comando `safe-cmd && other-cmd`. Os separadores de comando reconhecidos são `&&`, `||`, `;`, `|`, `|&`, `&` e quebras de linha. Uma regra deve corresponder a cada subcomando independentemente.
</Tip>

Quando você aprova um comando composto com "Sim, não pergunte novamente", Claude Code salva uma regra separada para cada subcomando que requer aprovação, em vez de uma única regra para a string completa. Por exemplo, aprovar `git status && npm test` salva uma regra para `npm test`, portanto futuras invocações de `npm test` são reconhecidas independentemente do que precede o `&&`. Subcomandos como `cd` em um subdiretório geram sua própria regra Read para esse caminho. Até 5 regras podem ser salvas para um único comando composto.

<h4 id="process-wrappers">
  Wrappers de processo
</h4>

Antes de corresponder regras Bash, Claude Code remove um conjunto fixo de wrappers de processo para que uma regra como `Bash(npm test *)` também corresponda a `timeout 30 npm test`. Os wrappers reconhecidos são `timeout`, `time`, `nice`, `nohup` e `stdbuf`.

`xargs` simples também é removido, portanto `Bash(grep *)` corresponde a `xargs grep pattern`. A remoção se aplica apenas quando `xargs` não tem flags: uma invocação como `xargs -n1 grep pattern` é correspondida como um comando `xargs`, portanto regras escritas para o comando interno não a cobrem.

Esta lista de wrapper é integrada e não é configurável. Executores de ambiente de desenvolvimento como `direnv exec`, `devbox run`, `mise exec`, `npx` e `docker exec` não estão na lista. Porque essas ferramentas executam seus argumentos como um comando, uma regra como `Bash(devbox run *)` corresponde a tudo que vem após `run`, incluindo `devbox run rm -rf .`. Para aprovar trabalho dentro de um executor de ambiente, escreva uma regra específica que inclua tanto o executor quanto o comando interno, como `Bash(devbox run npm test)`. Adicione uma regra por comando interno que você quer permitir.

Wrappers exec como `watch`, `setsid`, `ionice` e `flock` sempre solicitam e não podem ser auto-aprovados por uma regra de prefixo como `Bash(watch *)`. O mesmo se aplica a `find` com `-exec` ou `-delete`: uma regra `Bash(find *)` não cobre essas formas. Para aprovar uma invocação específica, escreva uma regra de correspondência exata para a string de comando completa.

<h4 id="read-only-commands">
  Comandos somente leitura
</h4>

Claude Code reconhece um conjunto integrado de comandos Bash como somente leitura e os executa sem um prompt de permissão em cada modo. Estes incluem `ls`, `cat`, `echo`, `pwd`, `head`, `tail`, `grep`, `find`, `wc`, `which`, `diff`, `stat`, `du`, `cd` e formas somente leitura de `git`. O conjunto não é configurável; para exigir um prompt para um desses comandos, adicione uma regra `ask` ou `deny` para ele.

Padrões glob sem aspas são permitidos para comandos cujas todas as flags são somente leitura, portanto `ls *.ts` e `wc -l src/*.py` são executados sem um prompt. Comandos com flags capazes de escrita ou execução, como `find`, `sort`, `sed` e `git`, ainda solicitam quando um glob sem aspas está presente porque o glob poderia expandir para uma flag como `-delete`.

Um `cd` em um caminho dentro do seu diretório de trabalho ou um [diretório adicional](#working-directories) também é somente leitura. Um comando composto como `cd packages/api && ls` é executado sem um prompt quando cada parte se qualifica por conta própria. Combinar `cd` com `git` em um comando composto solicita quando o `cd` muda para um diretório diferente, já que executar `git` em um novo diretório pode executar os hooks desse diretório. Um `cd` cujo alvo se resolve para o diretório de trabalho atual é uma no-op e não dispara este prompt.

Combinar `cd` com um redirecionamento de saída em um comando composto também solicita quando Claude Code não consegue determinar para qual diretório o alvo de redirecionamento se resolve após o `cd` ser executado. Um comando cujo único alvo de redirecionamento é `/dev/null`, como `cd app; grep -r pattern . 2>/dev/null`, não dispara este prompt, porque `/dev/null` não depende do diretório de trabalho.

<Warning>
  Padrões de permissão Bash que tentam restringir argumentos de comando são frágeis. Por exemplo, `Bash(curl http://github.com/ *)` pretende restringir curl a URLs do GitHub, mas não corresponderá a variações como:

  * Opções antes da URL: `curl -X GET http://github.com/...`
  * Protocolo diferente: `curl https://github.com/...`
  * Redirecionamentos: `curl -L http://bit.ly/xyz`, que redireciona para GitHub
  * Variáveis: `URL=http://github.com && curl $URL`
  * Espaços extras: `curl  http://github.com`

  Para filtragem de URL mais confiável, considere:

  * **Restringir ferramentas de rede Bash**: use regras deny para bloquear `curl`, `wget` e comandos similares, depois use a ferramenta WebFetch com permissão `WebFetch(domain:github.com)` para domínios permitidos
  * **Use hooks PreToolUse**: implemente um hook que valida URLs em comandos Bash e bloqueia domínios não permitidos
  * **Adicione orientação CLAUDE.md**: descreva seus padrões curl permitidos em `CLAUDE.md`. Isso molda o que Claude tenta mas não impõe um limite, portanto combine com uma das opções acima

  Observe que usar WebFetch sozinho não impede acesso à rede. Se Bash for permitido, Claude ainda pode usar `curl`, `wget` ou outras ferramentas para alcançar qualquer URL.
</Warning>

<h3 id="powershell">
  PowerShell
</h3>

As regras de permissão PowerShell usam a mesma forma que as regras Bash. Caracteres curinga com `*` correspondem em qualquer posição, o sufixo `:*` é equivalente a um ` *` final, e um `PowerShell` simples ou `PowerShell(*)` corresponde a cada comando. Esta configuração permite comandos `Get-ChildItem` e `git commit` enquanto bloqueia `Remove-Item`:

```json theme={null}
{
  "permissions": {
    "allow": [
      "PowerShell(Get-ChildItem *)",
      "PowerShell(git commit *)"
    ],
    "deny": [
      "PowerShell(Remove-Item *)"
    ]
  }
}
```

Aliases comuns são canonicalizados antes da correspondência. Uma regra escrita para o nome do cmdlet também corresponde a seus aliases, portanto `PowerShell(Get-ChildItem *)` corresponde a `gci`, `ls` e `dir` também. A correspondência é insensível a maiúsculas e minúsculas.

Claude Code analisa o AST do PowerShell e verifica cada comando em um comando composto independentemente. Os operadores de pipeline `|`, separadores de instrução `;` e nos operadores de cadeia PowerShell 7+ `&&` e `||` dividem um comando composto em subcomandos. Uma regra deve corresponder a cada subcomando para que o comando composto seja permitido.

<h3 id="read-and-edit">
  Read e Edit
</h3>

As regras `Edit` se aplicam a todas as ferramentas integradas que editam arquivos. Claude faz uma tentativa de melhor esforço para aplicar regras `Read` a todas as ferramentas integradas que leem arquivos como Grep e Glob, a menções `@file` em seus prompts, e à seleção e contexto de arquivo aberto que um [IDE](https://code.claude.com/docs/pt/vs-code#the-built-in-ide-mcp-server) conectado compartilha com Claude.

Uma regra deny `Read` também bloqueia a [ferramenta Edit](https://code.claude.com/docs/pt/errors#file-is-covered-by-a-read-deny-rule) no mesmo caminho, incluindo criar um novo arquivo lá. Write e NotebookEdit não são cobertos, portanto adicione uma regra deny `Edit` para caminhos que nenhuma ferramenta pode alterar. Requer Claude Code v2.1.208 ou posterior.

<Warning>
  As regras deny de Read e Edit se aplicam às ferramentas de arquivo integradas do Claude e aos comandos de arquivo que Claude Code reconhece em Bash, como `cat`, `head`, `tail` e `sed`. Elas não se aplicam a subprocessos arbitrários que leem ou escrevem arquivos indiretamente, como um script Python ou Node que abre arquivos por conta própria. Para imposição em nível de SO que bloqueia todos os processos de acessar um caminho, [ative o sandbox](https://code.claude.com/docs/pt/sandboxing).
</Warning>

As regras Read e Edit seguem a especificação [gitignore](https://git-scm.com/docs/gitignore) com quatro tipos de padrão distintos:

| Padrão             | Significado                                     | Exemplo                          | Corresponde                                                 |
| ------------------ | ----------------------------------------------- | -------------------------------- | ----------------------------------------------------------- |
| `//path`           | Caminho absoluto da raiz do sistema de arquivos | `Read(//Users/alice/secrets/**)` | `/Users/alice/secrets/**`                                   |
| `~/path`           | Caminho do diretório home                       | `Read(~/Documents/*.pdf)`        | `/Users/alice/Documents/*.pdf`                              |
| `/path`            | Caminho relativo à fonte de configurações       | `Edit(/src/**/*.ts)`             | `<raiz do projeto>/src/**/*.ts` em configurações de projeto |
| `path` ou `./path` | Caminho relativo ao diretório atual             | `Read(*.env)`                    | `<cwd>/*.env`                                               |

<Warning>
  Um padrão como `/Users/alice/file` não é um caminho absoluto. A barra inicial única ancora na fonte de configurações, não na raiz do sistema de arquivos. Use `//Users/alice/file` para caminhos absolutos.
</Warning>

Um padrão `/path` ancora no diretório associado ao arquivo de configurações que o define, portanto a mesma regra corresponde a locais diferentes dependendo de onde você a coloca:

| Regra definida em                                               | `/path` se resolve para       |
| :-------------------------------------------------------------- | :---------------------------- |
| Configurações de projeto ou local, como `.claude/settings.json` | `<raiz do projeto>/path`      |
| Configurações de usuário em `~/.claude/settings.json`           | `~/.claude/path`              |
| Um arquivo passado com `--settings <file>`                      | `<diretório do arquivo>/path` |
| Flags CLI, `/permissions` ou regras de sessão                   | `<cwd original>/path`         |

Uma regra deny como `Read(/secrets/**)` em configurações de usuário bloqueia `~/.claude/secrets/**`, não um diretório `secrets` em seu projeto. Para escrever uma regra em configurações de usuário que se aplique dentro de cada projeto, use um caminho absoluto `//` ou um caminho relativo à home `~/` em vez disso.

No Windows, os caminhos são normalizados para forma POSIX antes da correspondência. `C:\Users\alice` se torna `/c/Users/alice`, portanto use `//c/**/.env` para corresponder arquivos `.env` em qualquer lugar nessa unidade. Para corresponder em todas as unidades, use `//**/.env`.

Exemplos:

* `Edit(/docs/**)`: edita em `<projeto>/docs/`, não `/docs/` ou `<projeto>/.claude/docs/`
* `Read(~/.zshrc)`: lê o `.zshrc` do seu diretório home
* `Edit(//tmp/scratch.txt)`: edita o caminho absoluto `/tmp/scratch.txt`
* `Read(src/**)`: lê de `<diretório-atual>/src/`

Uma regra só corresponde a arquivos sob sua âncora, portanto a âncora determina o quão longe uma regra deny alcança. Nomes de arquivo simples seguem semântica gitignore e correspondem em qualquer profundidade, portanto `Read(.env)` e `Read(**/.env)` são equivalentes:

| Regra deny                      | Bloqueia                                                 | Não bloqueia                                            |
| ------------------------------- | -------------------------------------------------------- | ------------------------------------------------------- |
| `Read(.env)` ou `Read(**/.env)` | qualquer `.env` no ou sob o diretório atual              | `.env` em um diretório pai ou outro projeto             |
| `Read(//**/.env)`               | qualquer `.env` em qualquer lugar do sistema de arquivos | nada; a regra é ancorada na raiz do sistema de arquivos |

<Note>
  Em padrões gitignore, `*` corresponde dentro de um único segmento de caminho e pode aparecer em qualquer posição no padrão, enquanto `**` corresponde entre diretórios. Para permitir acesso a todos os arquivos, use apenas o nome da ferramenta sem parênteses: `Read`, `Edit` ou `Write`.
</Note>

Quando Claude acessa um symlink, as regras de permissão verificam dois caminhos: o próprio symlink e o arquivo para o qual ele se resolve. As regras allow e deny tratam esse par de forma diferente: as regras allow voltam a solicitar, enquanto as regras deny bloqueiam imediatamente.

* **Regras allow**: se aplicam apenas quando tanto o caminho do symlink quanto seu alvo correspondem. Um symlink dentro de um diretório permitido que aponta para fora dele ainda solicita.
* **Regras deny**: se aplicam quando o caminho do symlink ou seu alvo correspondem. Um symlink que aponta para um arquivo negado é ele próprio negado.

Por exemplo, com `Read(./project/**)` permitido e `Read(~/.ssh/**)` negado, um symlink em `./project/key` apontando para `~/.ssh/id_rsa` é bloqueado: o alvo falha na regra allow e corresponde à regra deny.

<h3 id="webfetch">
  WebFetch
</h3>

As regras WebFetch usam um prefixo `domain:` e correspondem ao hostname da URL solicitada. A correspondência é insensível a maiúsculas e minúsculas, suporta caracteres curinga `*`, e remove um `.` final tanto da regra quanto do hostname para que `example.com.` e `example.com` sejam tratados da mesma forma.

* `WebFetch(domain:example.com)` corresponde a solicitações para `example.com`
* `WebFetch(domain:*.example.com)` corresponde a qualquer subdomínio em qualquer profundidade, como `api.example.com` ou `a.b.example.com`, mas não a `example.com` em si
* `WebFetch(domain:*)` corresponde a cada domínio e é equivalente a uma regra `WebFetch` simples

Em qualquer posição que não seja um `*.` inicial ou um `*` simples, o caractere curinga corresponde apenas ao texto entre dois pontos. `WebFetch(domain:example.*)` corresponde a `example.org`, onde `*` se torna `org`, mas não a `example.evil.com`, onde `*` teria que se tornar `evil.com` e cruzar um ponto. Isso impede que um caractere curinga final corresponda a domínios que um atacante poderia registrar.

<h3 id="mcp">
  MCP
</h3>

As regras MCP usam o nome do servidor conforme configurado em Claude Code, opcionalmente seguido pelo nome de uma ferramenta desse servidor.

* `mcp__puppeteer` corresponde a qualquer ferramenta fornecida pelo servidor `puppeteer`
* `mcp__puppeteer__*` usa sintaxe com caracteres curinga e também corresponde a todas as ferramentas do servidor `puppeteer`
* `mcp__puppeteer__puppeteer_navigate` corresponde à ferramenta `puppeteer_navigate` fornecida pelo servidor `puppeteer`

Se sua organização definiu uma ferramenta [conector claude.ai](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) como `ask`, as regras allow para essa ferramenta não entram em vigor: Claude Code solicita em cada chamada, mesmo em modos `auto` e `bypassPermissions`. No modo `dontAsk`, que nunca solicita, Claude Code nega a chamada em vez disso. As ferramentas de conector aparecem como `mcp__claude_ai_<server>__<tool>`.

<h3 id="agent-subagents">
  Agent (subagents)
</h3>

Use regras `Agent(AgentName)` para controlar quais [subagents](https://code.claude.com/docs/pt/sub-agents) Claude pode usar:

* `Agent(Explore)` corresponde ao subagent Explore
* `Agent(Plan)` corresponde ao subagent Plan
* `Agent(my-custom-agent)` corresponde a um subagent personalizado chamado `my-custom-agent`

Adicione estas regras ao array `deny` em suas configurações ou use a flag CLI `--disallowedTools` para desabilitar agentes específicos. Para desabilitar o agente Explore:

```json theme={null}
{
  "permissions": {
    "deny": ["Agent(Explore)"]
  }
}
```

<h3 id="cd">
  Cd
</h3>

As regras `Cd` controlam para quais diretórios o [comando `/cd`](https://code.claude.com/docs/pt/commands) pode mover a sessão. `Cd` não é uma ferramenta invocável pelo modelo: Claude não pode chamá-la, e as regras se aplicam apenas quando você executa `/cd` você mesmo.

Uma regra deny `Cd` simples desabilita `/cd` inteiramente. Uma regra deny `Cd(<path-pattern>)` bloqueia alvos correspondentes. As regras deny verificam cada grafia do alvo, incluindo cada salto de symlink que ele se resolve através, portanto uma regra escrita para um caminho também bloqueia alvos que se resolvem para ele.

Adicionar qualquer regra allow `Cd` muda `/cd` para modo de lista de permissões: o diretório alvo resolvido deve corresponder a uma de suas regras allow, ou `/cd` recusa. Sem regras `Cd` configuradas, `/cd` mantém seu comportamento padrão e solicita que você confie em um diretório desconhecido.

Os padrões de caminho compartilham as âncoras `//`, `~/` e `/` das [regras Read e Edit](#read-and-edit), mas a correspondência é ancorada ao caminho do diretório inteiro em vez de estilo gitignore. `*` corresponde a exatamente um segmento de caminho e `**` corresponde entre segmentos. Um `/**` final também corresponde à sua raiz nomeada.

| Regra                 | Corresponde                                                | Não corresponde             |
| --------------------- | ---------------------------------------------------------- | --------------------------- |
| `Cd(~/code/*)`        | `~/code/app`                                               | `~/code/app/src`, `~/code`  |
| `Cd(~/code/**)`       | `~/code` e qualquer diretório sob ele                      | diretórios fora de `~/code` |
| `Cd(**/node_modules)` | qualquer diretório `node_modules` em qualquer profundidade | `node_modules/pkg`          |

<h2 id="extend-permissions-with-hooks">
  Estender permissões com hooks
</h2>

Os [hooks do Claude Code](https://code.claude.com/docs/pt/hooks-guide) fornecem uma maneira de registrar comandos de shell personalizados para realizar avaliação de permissão em tempo de execução. Quando Claude Code faz uma chamada de ferramenta, os hooks PreToolUse são executados antes do prompt de permissão. A saída do hook pode negar a chamada de ferramenta, forçar um prompt ou pular o prompt para deixar a chamada prosseguir.

As decisões do hook não contornam as regras de permissão. Claude Code avalia regras deny e ask independentemente do que um hook PreToolUse retorna: uma regra deny correspondente bloqueia a chamada, e uma regra ask correspondente ainda solicita mesmo quando o hook retornou `"allow"` ou `"ask"`. Isto preserva a precedência deny-first descrita em [Gerenciar permissões](#manage-permissions), incluindo regras deny definidas em configurações gerenciadas.

As ferramentas Connector que [sua organização definiu como `ask`](https://code.claude.com/docs/pt/mcp#organization-controls-on-connector-tools) e ferramentas MCP marcadas como [`requiresUserInteraction`](https://code.claude.com/docs/pt/mcp#require-approval-for-a-specific-tool) também ainda solicitam quando um hook retorna `"allow"`.

Um hook de bloqueio também tem precedência sobre regras allow. Um hook que sai com código 2 interrompe a chamada de ferramenta antes das regras de permissão serem avaliadas, portanto o bloqueio se aplica mesmo quando uma regra allow permitiria a chamada. Para executar todos os comandos Bash sem prompts exceto por alguns que você quer bloqueados, adicione `"Bash"` à sua lista allow e registre um hook PreToolUse que rejeita esses comandos específicos. Veja [Bloquear edições em arquivos protegidos](https://code.claude.com/docs/pt/hooks-guide#block-edits-to-protected-files) para um script de hook que você pode adaptar.

<h2 id="working-directories">
  Diretórios de trabalho
</h2>

Por padrão, Claude tem acesso a arquivos no diretório onde foi iniciado. Você pode estender este acesso:

* **Durante a inicialização**: use o argumento CLI `--add-dir <path>`
* **Durante a sessão**: use o comando `/add-dir`
* **Configuração persistente**: adicione a `additionalDirectories` em [arquivos de configuração](https://code.claude.com/docs/pt/settings#settings-files)

Arquivos em diretórios adicionais seguem as mesmas regras de permissão do diretório de trabalho original: eles se tornam legíveis sem prompts, e as permissões de edição de arquivo seguem o modo de permissão atual.

Em sessões em segundo plano no macOS, o host da sessão solicita acesso a pastas protegidas como `~/Desktop`, `~/Documents` e `~/Downloads` separadamente do seu terminal quando Claude precisa ler ou escrever arquivos lá; se as leituras falharem com `Operation not permitted`, consulte [como conceder acesso a pastas para sessões em segundo plano](https://code.claude.com/docs/pt/agent-view#background-sessions-can't-read-desktop-documents-or-downloads-on-macos).

Para alterar o diretório de trabalho primário da sessão em vez de adicionar outro, use [`/cd`](https://code.claude.com/docs/pt/commands). O comando `/cd` requer Claude Code v2.1.169 ou posterior. Diferentemente de `/add-dir`, ele realoca a sessão: o `CLAUDE.md` do novo diretório é carregado e `--resume` encontra a sessão a partir daí.

<h3 id="additional-directories-grant-file-access-not-configuration">
  Diretórios adicionais concedem acesso a arquivos, não configuração
</h3>

Adicionar um diretório estende onde Claude pode ler e editar arquivos. Não faz desse diretório uma raiz de configuração completa: a maioria da configuração `.claude/` não é descoberta de diretórios adicionais, embora alguns tipos sejam carregados como exceções.

Essas exceções se aplicam apenas a diretórios adicionados com o sinalizador `--add-dir` ou o comando `/add-dir`. Diretórios listados em `permissions.additionalDirectories` em um arquivo de configuração concedem apenas acesso a arquivos e não carregam nenhuma das configurações abaixo.

Os seguintes tipos de configuração são carregados de diretórios `--add-dir`:

| Configuração                                                                             | Carregado de `--add-dir`                                                                                                                                                        |
| :--------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| [Skills](https://code.claude.com/docs/pt/skills) em `.claude/skills/`                                                | Sim, com recarga ao vivo                                                                                                                                                        |
| [Subagentes](https://code.claude.com/docs/pt/sub-agents) em `.claude/agents/`                                        | Sim                                                                                                                                                                             |
| [Configurações](https://code.claude.com/docs/pt/settings) em `.claude/settings.json` e `.claude/settings.local.json` | Apenas chaves `enabledPlugins` e `extraKnownMarketplaces`                                                                                                                       |
| Arquivos [CLAUDE.md](https://code.claude.com/docs/pt/memory), `.claude/rules/` e `CLAUDE.local.md`                   | Apenas quando `CLAUDE_CODE_ADDITIONAL_DIRECTORIES_CLAUDE_MD=1` está definido. `CLAUDE.local.md` adicionalmente requer a fonte de configuração `local`, que é ativada por padrão |

Comandos e estilos de saída são descobertos do diretório de trabalho atual e seus pais, seu diretório de usuário em `~/.claude/` e configurações gerenciadas. Hooks e outras chaves `settings.json` são carregadas da pasta `.claude/` do diretório de trabalho atual sem fallback de diretório pai, juntamente com seu `~/.claude/settings.json` de usuário e configurações gerenciadas. Para compartilhar essa configuração entre projetos, use uma destas abordagens:

* **Configuração em nível de usuário**: coloque arquivos em `~/.claude/agents/`, `~/.claude/output-styles/` ou `~/.claude/settings.json` para torná-los disponíveis em cada projeto
* **Plugins**: empacote e distribua configuração como um [plugin](https://code.claude.com/docs/pt/plugins) que as equipes podem instalar
* **Inicie do diretório de configuração**: execute Claude Code do diretório contendo a configuração `.claude/` que você deseja

<h2 id="how-permissions-interact-with-sandboxing">
  Como as permissões interagem com sandboxing
</h2>

Permissões e [sandboxing](https://code.claude.com/docs/pt/sandboxing) são camadas de segurança complementares:

* **Permissões** controlam quais ferramentas Claude Code pode usar e quais arquivos ou domínios pode acessar. Elas se aplicam a todas as ferramentas, incluindo Bash, Read, Edit, WebFetch e MCP.
* **Sandboxing** fornece imposição em nível de SO que restringe o acesso do Bash à rede e sistema de arquivos. Aplica-se apenas a comandos Bash e seus processos filhos.

Use ambos para defesa em profundidade:

* As regras deny de permissão bloqueiam Claude de até tentar acessar recursos restritos
* As restrições de sandbox impedem que comandos Bash alcancem recursos fora dos limites definidos, mesmo se uma injeção de prompt contornar a tomada de decisão de Claude
* As restrições de sistema de arquivos no sandbox combinam as configurações [`sandbox.filesystem`](https://code.claude.com/docs/pt/sandboxing) com regras deny de Read e Edit; ambas são mescladas no limite final do sandbox
* As restrições de rede combinam regras de permissão WebFetch com as listas `allowedDomains` e `deniedDomains` do sandbox

Quando o sandboxing é ativado com `autoAllowBashIfSandboxed: true`, que é o padrão, comandos Bash em sandbox são executados sem solicitar mesmo se suas permissões incluem uma regra ask simples `Bash`, ou o [formulário equivalente `Bash(*)`](#match-all-uses-of-a-tool): o limite do sandbox substitui esse prompt de ferramenta inteira. Estas verificações ainda se aplicam:

* Regras ask com escopo de conteúdo como `Bash(git push *)` ainda forçam um prompt
* Regras deny explícitas ainda se aplicam
* Comandos `rm` ou `rmdir` que visam `/`, seu diretório inicial ou outros caminhos críticos do sistema ainda acionam um prompt

Comandos que não serão executados em sandbox, como comandos excluídos, respeitam a regra ask simples `Bash` como de costume. Veja [modos de sandbox](https://code.claude.com/docs/pt/sandboxing#sandbox-modes) para alterar este comportamento.

<h2 id="managed-settings">
  Configurações gerenciadas
</h2>

Para organizações que precisam de controle centralizado sobre a configuração do Claude Code, administradores podem implantar configurações gerenciadas que não podem ser substituídas por configurações de usuário ou projeto. Estas configurações de política seguem o mesmo formato que arquivos de configuração regulares e podem ser entregues através de políticas MDM/nível de SO, arquivos de configuração gerenciados, [configurações gerenciadas por servidor](https://code.claude.com/docs/pt/server-managed-settings), ou um [gateway de aplicativos Claude](https://code.claude.com/docs/pt/claude-apps-gateway) auto-hospedado. Veja [arquivos de configuração](https://code.claude.com/docs/pt/settings#settings-files) para mecanismos de entrega e locais de arquivo.

<h3 id="managed-only-settings">
  Configurações apenas gerenciadas
</h3>

As seguintes configurações são lidas apenas de configurações gerenciadas. Colocá-las em arquivos de configuração de usuário ou projeto não tem efeito.

| Configuração                                   | Descrição                                                                                                                                                                                                                                                                                                                                            |
| :--------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `allowAllClaudeAiMcps`                         | Quando `true`, conectores claude.ai carregam junto com um `managed-mcp.json` implantado em vez de serem suprimidos por seu controle exclusivo. Veja [Configuração MCP gerenciada](https://code.claude.com/docs/pt/managed-mcp)                                                                                                                                                   |
| `allowedChannelPlugins`                        | Lista de permissão de plugins de canal que podem enviar mensagens. Substitui a lista de permissão padrão da Anthropic quando definida. Requer `channelsEnabled: true`. Veja [Restringir quais plugins de canal podem ser executados](https://code.claude.com/docs/pt/channels#restrict-which-channel-plugins-can-run)                                                            |
| `allowManagedHooksOnly`                        | Quando `true`, apenas hooks gerenciados, hooks SDK e hooks de plugins força-ativados em configurações gerenciadas `enabledPlugins` são carregados. Hooks de usuário, projeto e todos os outros plugins são bloqueados                                                                                                                                |
| `allowManagedMcpServersOnly`                   | Quando `true`, apenas `allowedMcpServers` de configurações gerenciadas são respeitados. `deniedMcpServers` ainda se mescla de todas as fontes. Veja [Configuração MCP gerenciada](https://code.claude.com/docs/pt/managed-mcp)                                                                                                                                                   |
| `allowManagedPermissionRulesOnly`              | Quando `true`, impede que configurações de usuário e projeto definam regras de permissão `allow`, `ask` ou `deny`. Apenas regras em configurações gerenciadas se aplicam. Não afeta a lista de permissão do servidor MCP; para isso, defina `allowManagedMcpServersOnly`                                                                             |
| `blockedMarketplaces`                          | Lista de bloqueio de fontes de marketplace. Fontes bloqueadas são verificadas antes do download, portanto nunca tocam o sistema de arquivos. Veja [restrições de marketplace gerenciadas](https://code.claude.com/docs/pt/plugin-marketplaces#managed-marketplace-restrictions)                                                                                                  |
| `channelsEnabled`                              | Permitir [channels](https://code.claude.com/docs/pt/channels) para a organização. Veja [controles empresariais](https://code.claude.com/docs/pt/channels#enterprise-controls) para o padrão em cada plano                                                                                                                                                                                                    |
| `disableSideloadFlags`                         | Rejeitar os sinalizadores CLI `--plugin-dir`, `--plugin-url`, `--agents` e `--mcp-config` na inicialização. Sem isso, os usuários podem contornar `strictKnownMarketplaces` para uma única execução passando esses sinalizadores. Veja [`disableSideloadFlags`](https://code.claude.com/docs/pt/settings#available-settings). Requer Claude Code v2.1.193 ou posterior           |
| `forceRemoteSettingsRefresh`                   | Quando `true`, bloqueia a inicialização da CLI até que as configurações gerenciadas remotas sejam buscadas recentemente e sai se a busca falhar. Veja [imposição fail-closed](https://code.claude.com/docs/pt/server-managed-settings#enforce-fail-closed-startup)                                                                                                               |
| `pluginTrustMessage`                           | Mensagem personalizada anexada ao aviso de confiança de plugin mostrado antes da instalação                                                                                                                                                                                                                                                          |
| `sandbox.filesystem.allowManagedReadPathsOnly` | Quando `true`, apenas caminhos `filesystem.allowRead` de configurações gerenciadas são respeitados. `denyRead` ainda se mescla de todas as fontes                                                                                                                                                                                                    |
| `sandbox.network.allowManagedDomainsOnly`      | Quando `true`, apenas `allowedDomains` e regras allow `WebFetch(domain:...)` de configurações gerenciadas são respeitados. Domínios não permitidos são bloqueados automaticamente sem solicitar ao usuário. Domínios negados ainda se mesclam de todas as fontes                                                                                     |
| `strictKnownMarketplaces`                      | Controla quais marketplaces de plugin os usuários podem adicionar e instalar plugins. Veja [restrições de marketplace gerenciadas](https://code.claude.com/docs/pt/plugin-marketplaces#managed-marketplace-restrictions)                                                                                                                                                         |
| `strictPluginOnlyCustomization`                | Bloqueia skills, agents, hooks e servidores MCP de fontes de usuário e projeto, para que possam vir apenas de plugins ou configurações gerenciadas. `true` bloqueia todas as quatro superfícies; um array como `["skills", "hooks"]` bloqueia apenas as nomeadas. Veja [`strictPluginOnlyCustomization`](https://code.claude.com/docs/pt/settings#strictpluginonlycustomization) |
| `wslInheritsWindowsSettings`                   | Quando `true` na chave de registro HKLM do Windows ou `C:\Program Files\ClaudeCode\managed-settings.json`, WSL lê configurações gerenciadas da cadeia de política do Windows além de `/etc/claude-code`. Veja [Arquivos de configuração](https://code.claude.com/docs/pt/settings#settings-files)                                                                                |

`disableBypassPermissionsMode` é tipicamente colocado em configurações gerenciadas para impor política organizacional, mas funciona de qualquer escopo. Um usuário pode defini-lo em suas próprias configurações para se bloquear do modo bypass.

<Note>
  Em planos Team e Enterprise, um Owner ativa ou desativa [Remote Control](https://code.claude.com/docs/pt/remote-control) e [sessões web](https://code.claude.com/docs/pt/claude-code-on-the-web) em toda a organização em [configurações de admin do Claude Code](https://claude.ai/admin-settings/claude-code). Remote Control pode ser adicionalmente desativado por dispositivo com a configuração [`disableRemoteControl`](https://code.claude.com/docs/pt/settings#available-settings). Sessões web não têm chave de configurações gerenciadas por dispositivo.
</Note>

<h2 id="settings-precedence">
  Precedência de configurações
</h2>

As regras de permissão seguem a mesma [precedência de configurações](https://code.claude.com/docs/pt/settings#settings-precedence) que todas as outras configurações do Claude Code:

1. **Configurações gerenciadas**: não podem ser substituídas por nenhum outro nível, incluindo argumentos de linha de comando
2. **Argumentos de linha de comando**: substituições de sessão temporária
3. **Configurações de projeto local** (`.claude/settings.local.json`)
4. **Configurações de projeto compartilhado** (`.claude/settings.json`)
5. **Configurações de usuário** (`~/.claude/settings.json`)

Se uma ferramenta for negada em qualquer nível, nenhum outro nível pode permitir. Por exemplo, uma negação de configurações gerenciadas não pode ser substituída por `--allowedTools`, e `--disallowedTools` pode adicionar restrições além do que as configurações gerenciadas definem.

O mesmo se aplica entre escopos de configurações: se as configurações de usuário permitirem uma permissão e as configurações de projeto a negarem, a regra de negação a bloqueia. O inverso também é verdadeiro: uma negação no nível de usuário bloqueia uma permissão no nível de projeto, porque as regras de negação de qualquer escopo são avaliadas antes das regras de permissão.

Os hosts de incorporação podem fornecer política gerenciada adicional por meio da opção `managedSettings` do SDK quando [`parentSettingsBehavior`](https://code.claude.com/docs/pt/settings#settings-precedence) está definido como `"merge"`; os valores do incorporador podem apertar a política, mas não afrouxá-la.

<h2 id="project-allow-rules-and-workspace-trust">
  Regras de permissão do projeto e confiança do workspace
</h2>

As regras `permissions.allow` e as entradas `permissions.additionalDirectories` no `.claude/settings.json` de um projeto concedem capacidade, portanto Claude Code as aplica apenas após você aceitar o [diálogo de confiança do workspace](https://code.claude.com/docs/pt/security#additional-safeguards) para esse workspace. Até então, Claude Code lê as regras mas não as aplica. O diálogo de confiança lista as regras de permissão e diretórios adicionais que a pasta concederia para que você possa revisá-los antes de aceitar. As regras `deny` e `ask` não são afetadas, pois apenas restringem.

Claude Code salva a confiança por workspace, usando como chave a raiz do repositório git ou, fora de um repositório, o diretório a partir do qual você iniciou Claude Code. Quando você inicia no seu diretório inicial, a confiança é mantida apenas para a sessão atual e não é gravada em disco; consulte a nota sobre [salvaguardas adicionais](https://code.claude.com/docs/pt/security#additional-safeguards). Confiar em um diretório pai não aplica as regras de permissão de um projeto aninhado.

`.claude/settings.local.json` é seu próprio arquivo, portanto a verificação de confiança do workspace geralmente não se aplica a ele. Quando um repositório poderia ter fornecido o arquivo, como quando ele é confirmado no git ou `.claude` é um symlink, suas regras de permissão e diretórios adicionais passam pela verificação de confiança como as configurações do projeto.

Claude Code executa git para verificar se o repositório forneceu o arquivo, e executa essa verificação apenas em uma pasta coberta por um diálogo de confiança aceito, para essa pasta ou para um de seus diretórios pai. Em uma sessão interativa em uma pasta que você ainda não confiou, as regras de permissão e diretórios adicionais em `.claude/settings.local.json` passam pela verificação de confiança como as configurações do projeto até que você aceite o diálogo, a menos que a sessão seja executada no seu diretório de configuração pessoal conforme descrito abaixo. Das duas exceções abaixo, apenas a exceção do diretório de configuração se aplica antes do diálogo, porque não precisa executar git. Determinar que um diretório não está dentro de um repositório git usa a mesma verificação git, portanto a exceção de não estar dentro de um repositório entra em vigor uma vez que um diálogo de confiança cobrindo a pasta é aceito. Antes da v2.1.207, um `.claude/settings.local.json` não rastreado aplicava suas regras de permissão nessa pasta antes de você aceitar o diálogo.

As regras de permissão e diretórios adicionais em `.claude/settings.local.json` também se aplicam sem confiança do workspace em dois casos:

* O diretório a partir do qual você iniciou Claude Code não está dentro de um repositório git.
* A sessão é executada no seu diretório de configuração pessoal: seu diretório inicial ou qualquer diretório cujo subdiretório `.claude` você tenha definido como [`CLAUDE_CONFIG_DIR`](https://code.claude.com/docs/pt/env-vars).

Em ambos os casos, o arquivo é um que você criou em vez de um que um repositório poderia ter fornecido, e um `.claude/settings.local.json` confirmado no repositório ainda requer confiança do workspace. As versões 2.1.196 a 2.1.199 tratavam o arquivo como fornecido pelo repositório nesses workspaces, ignoravam suas regras de permissão e imprimiam um aviso [`this workspace has not been trusted`](https://code.claude.com/docs/pt/errors#workspace-has-not-been-trusted) para stderr. As duas exceções acima correspondem à v2.1.195 e anteriores e foram restauradas na v2.1.200.

Também a partir da v2.1.200, um workspace cujas regras de permissão ou diretórios adicionais ainda não são aplicados, mas que nunca mostrou o diálogo de confiança porque um diretório pai já era confiável, mostra o diálogo na próxima vez que você inicia Claude Code lá interativamente. O diálogo oferece duas opções:

* **Yes, I trust this folder**: salva a confiança para esse workspace e aplica as regras na mesma sessão.
* **No, continue without these permissions**: continua funcionando com essas regras ignoradas. O diálogo aparece novamente na próxima sessão.

No [modo não interativo](https://code.claude.com/docs/pt/headless) com `-p`, nenhum diálogo aparece e as regras permanecem ignoradas.

<h2 id="example-configurations">
  Configurações de exemplo
</h2>

Este [repositório](https://github.com/anthropics/claude-code/tree/main/examples/settings) inclui configurações de configuração inicial para cenários de implantação comuns. Use-as como pontos de partida e ajuste-as para suas necessidades.

<h2 id="see-also">
  Veja também
</h2>

* [Settings](https://code.claude.com/docs/pt/settings): referência de configuração completa incluindo a tabela de configurações de permissão
* [Configure auto mode](https://code.claude.com/docs/pt/auto-mode-config): diga ao classificador do modo auto qual infraestrutura sua organização confia
* [Sandboxing](https://code.claude.com/docs/pt/sandboxing): isolamento de rede e sistema de arquivos em nível de SO para comandos Bash
* [Authentication](https://code.claude.com/docs/pt/authentication): configure o acesso do usuário ao Claude Code
* [Security](https://code.claude.com/docs/pt/security): salvaguardas de segurança e melhores práticas
* [Hooks](https://code.claude.com/docs/pt/hooks-guide): automatize fluxos de trabalho e estenda avaliação de permissão
