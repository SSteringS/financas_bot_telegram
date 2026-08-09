<!-- Fonte: https://code.claude.com/docs/pt/plugins | Baixado em: 2026-08-06 | slug: plugins -->

# Criar plugins

> Crie plugins personalizados para estender Claude Code com skills, agents, hooks e MCP servers.

Plugins permitem que você estenda Claude Code com funcionalidade personalizada que pode ser compartilhada entre projetos e equipes. Este guia cobre a criação de seus próprios plugins com skills, agents, hooks e MCP servers.

Procurando instalar plugins existentes? Veja [Descobrir e instalar plugins](https://code.claude.com/docs/pt/discover-plugins). Para especificações técnicas completas, veja [Referência de plugins](https://code.claude.com/docs/pt/plugins-reference).

<h2 id="when-to-use-plugins-vs-standalone-configuration">
  Quando usar plugins vs configuração independente
</h2>

Claude Code suporta duas maneiras de adicionar skills, agents e hooks personalizados:

| Abordagem                                                 | Nomes de skills      | Melhor para                                                                                                               |
| :-------------------------------------------------------- | :------------------- | :------------------------------------------------------------------------------------------------------------------------ |
| **Independente** (diretório `.claude/`)                   | `/hello`             | Fluxos de trabalho pessoais, personalizações específicas do projeto, experimentos rápidos                                 |
| **Plugins** (diretórios com `.claude-plugin/plugin.json`) | `/plugin-name:hello` | Compartilhamento com colegas de equipe, distribuição para a comunidade, lançamentos versionados, reutilizável em projetos |

**Use configuração independente quando**:

* Você está personalizando Claude Code para um único projeto
* A configuração é pessoal e não precisa ser compartilhada
* Você está experimentando com skills ou hooks antes de empacotá-los
* Você quer nomes de skills curtos como `/hello` ou `/deploy`

**Use plugins quando**:

* Você quer compartilhar funcionalidade com sua equipe ou comunidade
* Você precisa dos mesmos skills/agents em múltiplos projetos
* Você quer controle de versão e atualizações fáceis para suas extensões
* Você está distribuindo através de um marketplace
* Você está ok com skills com namespace como `/my-plugin:hello` (namespacing previne conflitos entre plugins)

<Tip>
  Comece com configuração independente em `.claude/` para iteração rápida, depois [converta para um plugin](#convert-existing-configurations-to-plugins) quando estiver pronto para compartilhar.
</Tip>

<h2 id="quickstart">
  Início rápido
</h2>

Este início rápido o guia através da criação de um plugin com um skill personalizado. Você criará um manifesto (o arquivo de configuração que define seu plugin), adicionará um skill e o testará localmente usando a flag `--plugin-dir`.

<h3 id="prerequisites">
  Pré-requisitos
</h3>

* Claude Code [instalado e autenticado](https://code.claude.com/docs/pt/quickstart#step-1-install-claude-code)

<Note>
  Se você não vir o comando `/plugin`, atualize Claude Code para a versão mais recente. Veja [Troubleshooting](https://code.claude.com/docs/pt/troubleshooting) para instruções de atualização.
</Note>

<h3 id="create-your-first-plugin">
  Crie seu primeiro plugin
</h3>

<Steps>
  <Step title="Crie o diretório do plugin">
    Cada plugin vive em seu próprio diretório contendo seus skills, agents ou hooks, opcionalmente ao lado de um manifesto `.claude-plugin/plugin.json`. A localização não importa para este início rápido porque você apontará Claude Code para o diretório com `--plugin-dir` na etapa de teste. Crie-o em qualquer lugar conveniente, como uma pasta de rascunho ou um diretório de projetos:

    ```bash theme={null}
    mkdir my-first-plugin
    ```

    As etapas restantes são executadas a partir do diretório pai e fazem referência a caminhos como `my-first-plugin/...` relativos a ele.
  </Step>

  <Step title="Crie o manifesto do plugin">
    O arquivo de manifesto em `.claude-plugin/plugin.json` define a identidade do seu plugin: seu nome, descrição e versão. Claude Code usa esses metadados para exibir seu plugin no gerenciador de plugins.

    Crie o diretório `.claude-plugin` dentro da pasta do seu plugin:

    ```bash theme={null}
    mkdir my-first-plugin/.claude-plugin
    ```

    Depois crie `my-first-plugin/.claude-plugin/plugin.json` com este conteúdo:

    ```json my-first-plugin/.claude-plugin/plugin.json theme={null}
    {
      "name": "my-first-plugin",
      "description": "A greeting plugin to learn the basics",
      "version": "1.0.0",
      "author": {
        "name": "Your Name"
      }
    }
    ```

    | Campo         | Propósito                                                                                                                                                                                                                                                                                    |
    | :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | `name`        | Identificador único e namespace de skill. Skills são prefixados com isso (ex: `/my-first-plugin:hello`).                                                                                                                                                                                     |
    | `description` | Mostrado no gerenciador de plugins ao navegar ou instalar plugins.                                                                                                                                                                                                                           |
    | `version`     | Opcional. Se definido, os usuários recebem atualizações apenas quando você incrementa este campo. Se omitido e seu plugin é distribuído via git, o SHA do commit é usado e cada commit conta como uma nova versão. Veja [gerenciamento de versão](https://code.claude.com/docs/pt/plugins-reference#version-management). |
    | `author`      | Opcional. Útil para atribuição.                                                                                                                                                                                                                                                              |

    Para campos adicionais como `homepage`, `repository` e `license`, veja o [esquema de manifesto completo](https://code.claude.com/docs/pt/plugins-reference#plugin-manifest-schema).
  </Step>

  <Step title="Adicione um skill">
    Skills vivem no diretório `skills/`. Cada skill é uma pasta contendo um arquivo `SKILL.md`. O nome da pasta se torna o nome do skill, prefixado com o namespace do plugin (`hello/` em um plugin nomeado `my-first-plugin` cria `/my-first-plugin:hello`).

    Crie um diretório de skill na pasta do seu plugin:

    ```bash theme={null}
    mkdir -p my-first-plugin/skills/hello
    ```

    Depois crie `my-first-plugin/skills/hello/SKILL.md` com este conteúdo:

    ```markdown my-first-plugin/skills/hello/SKILL.md theme={null}
    ---
    description: Greet the user with a friendly message
    disable-model-invocation: true
    ---

    Greet the user warmly and ask how you can help them today.
    ```
  </Step>

  <Step title="Teste seu plugin">
    Execute Claude Code com a flag `--plugin-dir` para carregar seu plugin:

    ```bash theme={null}
    claude --plugin-dir ./my-first-plugin
    ```

    Uma vez que Claude Code inicia, tente seu novo skill:

    ```shell theme={null}
    /my-first-plugin:hello
    ```

    Você verá Claude responder com uma saudação. Execute `/help` para ver seu skill listado sob o namespace do plugin.

    <Note>
      **Por que namespacing?** Plugin skills são sempre com namespace (como `/my-first-plugin:hello`) para prevenir conflitos quando múltiplos plugins têm skills com o mesmo nome.

      Para mudar o prefixo de namespace, atualize o campo `name` em `plugin.json`.
    </Note>
  </Step>

  <Step title="Adicione argumentos de skill">
    Torne seu skill dinâmico aceitando entrada do usuário. O placeholder `$ARGUMENTS` captura qualquer texto que o usuário fornece após o nome do skill.

    Atualize seu arquivo `SKILL.md`:

    ```markdown my-first-plugin/skills/hello/SKILL.md theme={null}
    ---
    description: Greet the user with a personalized message
    ---

    # Hello Skill

    Greet the user named "$ARGUMENTS" warmly and ask how you can help them today. Make the greeting personal and encouraging.
    ```

    Execute `/reload-plugins` para pegar as mudanças, depois tente o skill com seu nome:

    ```shell theme={null}
    /my-first-plugin:hello Alex
    ```

    Claude o saudará pelo nome. Para mais sobre passar argumentos para skills, veja [Skills](https://code.claude.com/docs/pt/skills#pass-arguments-to-skills).
  </Step>
</Steps>

Você criou e testou com sucesso um plugin com estes componentes-chave:

* **Manifesto do plugin** (`.claude-plugin/plugin.json`): descreve os metadados do seu plugin
* **Diretório de skills** (`skills/`): contém seus skills personalizados
* **Argumentos de skill** (`$ARGUMENTS`): captura entrada do usuário para comportamento dinâmico

<Tip>
  A flag `--plugin-dir` é útil para desenvolvimento e testes. Quando estiver pronto para compartilhar seu plugin com outros, veja [Criar e distribuir um marketplace de plugins](https://code.claude.com/docs/pt/plugin-marketplaces).
</Tip>

<h2 id="develop-a-plugin-in-your-skills-directory">
  Desenvolva um plugin em seu diretório de skills
</h2>

Em vez de passar `--plugin-dir` em cada inicialização, você pode manter um plugin em seu diretório de skills e fazer com que Claude Code o carregue automaticamente. `claude plugin init` cria um:

```bash theme={null}
claude plugin init my-tool
```

Isso cria `~/.claude/skills/my-tool/` com um manifesto `.claude-plugin/plugin.json` e um `SKILL.md` inicial. Na próxima sessão ele carrega como `my-tool@skills-dir` sem nenhuma etapa de marketplace ou instalação.

Para as regras de carregamento automático, escopo pessoal vs. projeto, o requisito de confiança do workspace e como atualizar ou remover um, veja [Plugins do diretório de skills](https://code.claude.com/docs/pt/plugins-reference#skills-directory-plugins).

<h2 id="plugin-structure-overview">
  Visão geral da estrutura do plugin
</h2>

Você criou um plugin com um skill, mas plugins podem incluir muito mais: agents personalizados, hooks, MCP servers, LSP servers e monitores de background.

<Warning>
  **Erro comum**: Não coloque `commands/`, `agents/`, `skills/` ou `hooks/` dentro do diretório `.claude-plugin/`. Apenas `plugin.json` vai dentro de `.claude-plugin/`. Todos os outros diretórios devem estar no nível raiz do plugin.

  A raiz do plugin é o diretório individual do próprio plugin: aquele que contém `.claude-plugin/plugin.json`. Nunca é `~/.claude/`. Por exemplo, Claude Code não lê um `.mcp.json` colocado em `~/.claude/.mcp.json`.
</Warning>

| Diretório         | Localização    | Propósito                                                                              |
| :---------------- | :------------- | :------------------------------------------------------------------------------------- |
| `.claude-plugin/` | Raiz do plugin | Contém manifesto `plugin.json` (opcional se componentes usam localizações padrão)      |
| `skills/`         | Raiz do plugin | Skills como diretórios `<name>/SKILL.md`                                               |
| `commands/`       | Raiz do plugin | Skills como arquivos Markdown simples. Use `skills/` para novos plugins                |
| `agents/`         | Raiz do plugin | Definições de agent personalizadas                                                     |
| `hooks/`          | Raiz do plugin | Manipuladores de eventos em `hooks.json`                                               |
| `.mcp.json`       | Raiz do plugin | Configurações de MCP server                                                            |
| `.lsp.json`       | Raiz do plugin | Configurações de LSP server para inteligência de código                                |
| `monitors/`       | Raiz do plugin | Configurações de monitor de background em `monitors.json`                              |
| `bin/`            | Raiz do plugin | Executáveis adicionados ao `PATH` da ferramenta Bash enquanto o plugin está habilitado |
| `settings.json`   | Raiz do plugin | [Configurações](https://code.claude.com/docs/pt/settings) padrão aplicadas quando o plugin é habilitado            |

Um plugin que fornece exatamente um skill pode colocar `SKILL.md` diretamente na raiz do plugin em vez de criar um diretório `skills/`. Claude Code o carrega como um único skill e usa o campo `name` do frontmatter para o nome de invocação. Use o layout `skills/` para plugins que podem crescer para mais de um skill.

<Note>
  **Próximos passos**: Pronto para adicionar mais recursos? Vá para [Desenvolver plugins mais complexos](#develop-more-complex-plugins) para adicionar agents, hooks, MCP servers e LSP servers. Para especificações técnicas completas de todos os componentes do plugin, veja [Referência de plugins](https://code.claude.com/docs/pt/plugins-reference).
</Note>

<h2 id="develop-more-complex-plugins">
  Desenvolver plugins mais complexos
</h2>

Uma vez que você está confortável com plugins básicos, você pode criar extensões mais sofisticadas.

<h3 id="add-skills-to-your-plugin">
  Adicione Skills ao seu plugin
</h3>

Plugins podem incluir [Agent Skills](https://code.claude.com/docs/pt/skills) para estender as capacidades do Claude. Skills são invocados por modelo: Claude os usa automaticamente com base no contexto da tarefa.

Adicione um diretório `skills/` na raiz do seu plugin com pastas de Skill contendo arquivos `SKILL.md`:

```text theme={null}
my-plugin/
├── .claude-plugin/
│   └── plugin.json
└── skills/
    └── code-review/
        └── SKILL.md
```

Cada `SKILL.md` contém frontmatter YAML e instruções. Inclua uma `description` para que Claude saiba quando usar o skill:

```yaml theme={null}
---
description: Reviews code for best practices and potential issues. Use when reviewing code, checking PRs, or analyzing code quality.
---

When reviewing code, check for:
1. Code organization and structure
2. Error handling
3. Security concerns
4. Test coverage
```

Após instalar o plugin, execute `/reload-plugins` para carregar os Skills. Para orientação completa de autoria de Skill incluindo divulgação progressiva e restrições de ferramentas, veja [Agent Skills](https://code.claude.com/docs/pt/skills).

<h3 id="add-lsp-servers-to-your-plugin">
  Adicione LSP servers ao seu plugin
</h3>

<Tip>
  Para linguagens comuns como TypeScript, Python e Rust, instale os plugins LSP pré-construídos do marketplace oficial. Crie plugins LSP personalizados apenas quando você precisar de suporte para linguagens não cobertas.
</Tip>

Plugins LSP (Language Server Protocol) dão ao Claude inteligência de código em tempo real. Se você precisar suportar uma linguagem que não tem um plugin LSP oficial, você pode criar um próprio adicionando um arquivo `.lsp.json` ao seu plugin:

```json .lsp.json theme={null}
{
  "go": {
    "command": "gopls",
    "args": ["serve"],
    "extensionToLanguage": {
      ".go": "go"
    }
  }
}
```

Usuários instalando seu plugin devem ter o binário do language server instalado em sua máquina.

Para opções de configuração LSP completas, veja [LSP servers](https://code.claude.com/docs/pt/plugins-reference#lsp-servers).

<h3 id="add-background-monitors-to-your-plugin">
  Adicione monitores de background ao seu plugin
</h3>

Monitores de background permitem que seu plugin observe logs, arquivos ou status externo em background e notifique Claude conforme eventos chegam. Claude Code inicia cada monitor automaticamente quando o plugin está ativo, então você não precisa instruir Claude a iniciar a observação.

Adicione um arquivo `monitors/monitors.json` na raiz do plugin com um array de entradas de monitor:

```json monitors/monitors.json theme={null}
[
  {
    "name": "error-log",
    "command": "tail -F ./logs/error.log",
    "description": "Application error log"
  }
]
```

Cada linha de stdout do `command` é entregue ao Claude como uma notificação durante a sessão. Para o esquema completo, incluindo o trigger `when` e substituição de variáveis, veja [Monitors](https://code.claude.com/docs/pt/plugins-reference#monitors).

<h3 id="ship-default-settings-with-your-plugin">
  Envie configurações padrão com seu plugin
</h3>

Plugins podem incluir um arquivo `settings.json` na raiz do plugin para aplicar configuração padrão quando o plugin é habilitado. Atualmente, apenas as chaves `agent` e `subagentStatusLine` são suportadas.

Definir `agent` ativa um dos [agents personalizados](https://code.claude.com/docs/pt/sub-agents) do plugin como a thread principal, aplicando seu prompt de sistema, restrições de ferramentas e modelo. Isso permite que um plugin mude como Claude Code se comporta por padrão quando habilitado.

```json settings.json theme={null}
{
  "agent": "security-reviewer"
}
```

Este exemplo ativa o agent `security-reviewer` definido no diretório `agents/` do plugin. Configurações de `settings.json` têm prioridade sobre `settings` declarados em `plugin.json`. Chaves desconhecidas são silenciosamente ignoradas.

<h3 id="organize-complex-plugins">
  Organize plugins complexos
</h3>

Para plugins com muitos componentes, organize sua estrutura de diretório por funcionalidade. Para layouts de diretório completos e padrões de organização, veja [Estrutura de diretório do plugin](https://code.claude.com/docs/pt/plugins-reference#plugin-directory-structure).

<h3 id="test-your-plugins-locally">
  Teste seus plugins localmente
</h3>

Use a flag `--plugin-dir` para testar plugins durante o desenvolvimento. Isso carrega seu plugin diretamente sem exigir instalação.

```bash theme={null}
claude --plugin-dir ./my-plugin
```

A flag também aceita um arquivo `.zip` do diretório do plugin, que requer Claude Code v2.1.128 ou posterior.

```bash theme={null}
claude --plugin-dir ./my-plugin.zip
```

Quando um plugin `--plugin-dir` tem o mesmo nome que um plugin marketplace instalado, a cópia local tem precedência para essa sessão. Isso permite que você teste mudanças em um plugin que você já tem instalado sem desinstalá-lo primeiro. A exceção é plugins que configurações gerenciadas forçadamente habilitam ou desabilitam: `--plugin-dir` não pode substituir aqueles.

Conforme você faz mudanças no seu plugin, execute `/reload-plugins` para pegar as atualizações sem reiniciar. Isso recarrega plugins, skills, agents, hooks, plugin MCP servers e plugin LSP servers. Teste seus componentes de plugin:

* Tente seus skills com `/plugin-name:skill-name`
* Verifique que agents aparecem em `/context` sob Custom Agents, ou @-mencione um pelo seu nome com escopo
* Verifique que hooks funcionam como esperado

<Tip>
  Você pode carregar múltiplos plugins de uma vez especificando a flag múltiplas vezes:

  ```bash theme={null}
  claude --plugin-dir ./plugin-one --plugin-dir ./plugin-two
  ```
</Tip>

Para testar um plugin que já está empacotado como um arquivo `.zip` e hospedado em uma URL, como um artefato de compilação de CI, use `--plugin-url` em vez disso. Claude Code busca o arquivo no início e o carrega apenas para essa sessão. Se a busca falhar ou o arquivo for inválido, Claude Code relata um erro de carregamento de plugin e inicia sem ele. As mesmas [considerações de confiança](https://code.claude.com/docs/pt/discover-plugins#security) se aplicam como para qualquer fonte de plugin: apenas aponte esse flag para arquivos que você controla ou confia.

Para carregar múltiplos plugins, repita a flag para cada URL:

```bash theme={null}
claude --plugin-url https://example.com/my-plugin.zip --plugin-url https://example.com/other.zip
```

Ou passe URLs separadas por espaço como um argumento entre aspas:

```bash theme={null}
claude --plugin-url "https://example.com/my-plugin.zip https://example.com/other.zip"
```

<h3 id="debug-plugin-issues">
  Depure problemas de plugin
</h3>

Se seu plugin não está funcionando como esperado:

1. **Verifique a estrutura**: Certifique-se de que seus diretórios estão na raiz do plugin, não dentro de `.claude-plugin/`
2. **Teste componentes individualmente**: Verifique cada skill, agent e hook separadamente
3. **Use ferramentas de validação e depuração**: Veja [Ferramentas de depuração e desenvolvimento](https://code.claude.com/docs/pt/plugins-reference#debugging-and-development-tools) para comandos CLI e técnicas de troubleshooting

<h3 id="share-your-plugins">
  Compartilhe seus plugins
</h3>

Quando seu plugin estiver pronto para compartilhar:

1. **Adicione documentação**: Inclua um `README.md` com instruções de instalação e uso
2. **Escolha uma estratégia de versionamento**: Decida se deve definir uma `version` explícita ou confiar no SHA do commit git. Veja [gerenciamento de versão](https://code.claude.com/docs/pt/plugins-reference#version-management)
3. **Crie ou use um marketplace**: Distribua através de [marketplaces de plugins](https://code.claude.com/docs/pt/plugin-marketplaces) para instalação
4. **Teste com outros**: Tenha membros da equipe testarem o plugin antes de distribuição mais ampla

Uma vez que seu plugin está em um marketplace, outros podem instalá-lo usando as instruções em [Descobrir e instalar plugins](https://code.claude.com/docs/pt/discover-plugins). Para manter um plugin interno à sua equipe, hospede o marketplace em um [repositório privado](https://code.claude.com/docs/pt/plugin-marketplaces#private-repositories).

<h3 id="submit-your-plugin-to-the-community-marketplace">
  Envie seu plugin para o marketplace da comunidade
</h3>

A Anthropic mantém dois marketplaces públicos para plugins do Claude Code:

* **`claude-plugins-official`**: um conjunto curado de plugins mantidos pela Anthropic. Registrado automaticamente na primeira vez que você inicia Claude Code interativamente. Um script não-interativo que é executado antes desse primeiro lançamento deve adicioná-lo explicitamente com `claude plugin marketplace add anthropics/claude-plugins-official`.
* **`claude-community`**: o marketplace público da comunidade onde envios de terceiros chegam após revisão. Os usuários o adicionam com `/plugin marketplace add anthropics/claude-plugins-community` e instalam a partir dele como `@claude-community`.

Para enviar seu plugin para revisão do marketplace da comunidade, use um dos formulários no aplicativo:

* **claude.ai**: [claude.ai/admin-settings/directory/submissions/plugins/new](https://claude.ai/admin-settings/directory/submissions/plugins/new)
* **Console**: [platform.claude.com/plugins/submit](https://platform.claude.com/plugins/submit)

O formulário claude.ai requer uma organização Team ou Enterprise e acesso ao gerenciamento de diretório; proprietários de organização têm esse acesso por padrão. Autores individuais que não fazem parte de uma organização Team ou Enterprise podem usar o formulário Console em vez disso.

Execute `claude plugin validate` localmente antes de enviar. O pipeline de revisão executa a mesma verificação em cada envio, junto com triagem de segurança automatizada.

Plugins aprovados são fixados a um SHA de commit específico no catálogo [`anthropics/claude-plugins-community`](https://github.com/anthropics/claude-plugins-community), e CI aumenta o pino automaticamente conforme você envia novos commits para seu repositório. O catálogo público sincroniza todas as noites a partir do pipeline de revisão, então pode haver um atraso entre aprovação e seu plugin aparecer em `marketplace.json`. Para verificar se seu plugin já é instalável, procure por seu nome no [catálogo da comunidade](https://github.com/anthropics/claude-plugins-community/blob/main/.claude-plugin/marketplace.json).

O marketplace oficial, `claude-plugins-official`, é curado separadamente. A Anthropic decide quais plugins incluir a seu critério. Não há processo de aplicação, e o formulário de envio não adiciona plugins ao marketplace oficial.

Se a Anthropic listar seu plugin no marketplace oficial, seu CLI pode solicitar aos usuários do Claude Code que o instalem. Veja [Recomende seu plugin a partir de seu CLI](https://code.claude.com/docs/pt/plugin-hints).

<Note>
  Para especificações técnicas completas, técnicas de depuração e estratégias de distribuição, veja [Referência de plugins](https://code.claude.com/docs/pt/plugins-reference).
</Note>

<h2 id="convert-existing-configurations-to-plugins">
  Converta configurações existentes para plugins
</h2>

Se você já tem skills ou hooks em seu diretório `.claude/`, você pode convertê-los em um plugin para compartilhamento e distribuição mais fáceis.

<h3 id="migration-steps">
  Passos de migração
</h3>

<Steps>
  <Step title="Crie a estrutura do plugin">
    Crie um novo diretório de plugin na raiz do seu projeto, ao lado da pasta `.claude/` existente, para que os caminhos relativos `cp` na próxima etapa sejam resolvidos:

    ```bash theme={null}
    mkdir -p my-plugin/.claude-plugin
    ```

    Crie o arquivo de manifesto em `my-plugin/.claude-plugin/plugin.json`:

    ```json my-plugin/.claude-plugin/plugin.json theme={null}
    {
      "name": "my-plugin",
      "description": "Migrated from standalone configuration",
      "version": "1.0.0"
    }
    ```
  </Step>

  <Step title="Copie seus arquivos existentes">
    Copie suas configurações existentes para o diretório do plugin:

    ```bash theme={null}
    # Copy commands
    cp -r .claude/commands my-plugin/

    # Copy agents (if any)
    cp -r .claude/agents my-plugin/

    # Copy skills (if any)
    cp -r .claude/skills my-plugin/
    ```
  </Step>

  <Step title="Migre hooks">
    Se você tem hooks em suas configurações, crie um diretório de hooks:

    ```bash theme={null}
    mkdir my-plugin/hooks
    ```

    Crie `my-plugin/hooks/hooks.json` com sua configuração de hooks. Copie o objeto `hooks` de seu `.claude/settings.json` ou `settings.local.json`, já que o formato é o mesmo. O comando recebe entrada de hook como JSON em stdin, então use `jq` para extrair o caminho do arquivo:

    ```json my-plugin/hooks/hooks.json theme={null}
    {
      "hooks": {
        "PostToolUse": [
          {
            "matcher": "Write|Edit",
            "hooks": [{ "type": "command", "command": "jq -r '.tool_input.file_path' | xargs npm run lint:fix" }]
          }
        ]
      }
    }
    ```
  </Step>

  <Step title="Teste seu plugin migrado">
    Carregue seu plugin para verificar se tudo funciona:

    ```bash theme={null}
    claude --plugin-dir ./my-plugin
    ```

    Teste cada componente: execute seus comandos, verifique que agents aparecem em `/context`, e verifique que hooks disparam corretamente.
  </Step>
</Steps>

<h3 id="what-changes-when-migrating">
  O que muda ao migrar
</h3>

| Independente (`.claude/`)                 | Plugin                                  |
| :---------------------------------------- | :-------------------------------------- |
| Disponível apenas em um projeto           | Pode ser compartilhado via marketplaces |
| Arquivos em `.claude/commands/`           | Arquivos em `plugin-name/commands/`     |
| Hooks em `settings.json`                  | Hooks em `hooks/hooks.json`             |
| Deve copiar manualmente para compartilhar | Instale com `/plugin install`           |

<Note>
  Após migrar, remova os arquivos originais de `.claude/` para evitar duplicatas. As definições de agents em `.claude/agents/` do projeto e do usuário substituem agents com o mesmo nome do plugin, portanto a versão do plugin só entra em vigor uma vez que os originais são removidos. Skills de plugin são nomeados como `/plugin-name:skill-name`, portanto o `/skill-name` original e a cópia do plugin permanecem disponíveis em vez de um substituir o outro.
</Note>

<h2 id="next-steps">
  Próximos passos
</h2>

Agora que você entende o sistema de plugins do Claude Code, aqui estão caminhos sugeridos para diferentes objetivos:

<h3 id="for-plugin-users">
  Para usuários de plugins
</h3>

* [Descobrir e instalar plugins](https://code.claude.com/docs/pt/discover-plugins): navegue em marketplaces e instale plugins
* [Configurar marketplaces de equipe](https://code.claude.com/docs/pt/discover-plugins#configure-team-marketplaces): configure plugins no nível do repositório para sua equipe

<h3 id="for-plugin-developers">
  Para desenvolvedores de plugins
</h3>

* [Criar e distribuir um marketplace](https://code.claude.com/docs/pt/plugin-marketplaces): empacote e compartilhe seus plugins
* [Referência de plugins](https://code.claude.com/docs/pt/plugins-reference): especificações técnicas completas
* Mergulhe mais fundo em componentes específicos do plugin:
  * [Skills](https://code.claude.com/docs/pt/skills): detalhes de desenvolvimento de skill
  * [Subagents](https://code.claude.com/docs/pt/sub-agents): configuração e capacidades de agent
  * [Hooks](https://code.claude.com/docs/pt/hooks): manipulação de eventos e automação
  * [MCP](https://code.claude.com/docs/pt/mcp): integração de ferramentas externas
