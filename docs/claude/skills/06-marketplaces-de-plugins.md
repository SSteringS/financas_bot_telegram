<!-- Fonte: https://code.claude.com/docs/pt/plugin-marketplaces | Baixado em: 2026-08-06 | slug: plugin-marketplaces -->

# Criar e distribuir um marketplace de plugins

> Crie e hospede marketplaces de plugins para distribuir extensões Claude Code em equipes e comunidades.

Um **marketplace de plugins** é um catálogo que permite distribuir plugins para outros. Os marketplaces fornecem descoberta centralizada, rastreamento de versão, atualizações automáticas e suporte para múltiplos tipos de fonte, incluindo repositórios git e caminhos locais. Este guia mostra como criar seu próprio marketplace para compartilhar plugins com sua equipe ou comunidade.

Procurando instalar plugins de um marketplace existente? Veja [Descobrir e instalar plugins pré-construídos](https://code.claude.com/docs/pt/discover-plugins).

<h2 id="overview">
  Visão geral
</h2>

Criar e distribuir um marketplace envolve:

1. **Criar plugins**: construir um ou mais plugins com skills, agents, hooks, MCP servers ou LSP servers. Este guia assume que você já tem plugins para distribuir; veja [Criar plugins](https://code.claude.com/docs/pt/plugins) para detalhes sobre como criá-los.
2. **Criar o arquivo de marketplace**: definir um `marketplace.json` que lista seus plugins e onde encontrá-los. Veja [Criar o arquivo de marketplace](#create-the-marketplace-file).
3. **Hospedar o marketplace**: fazer push para GitHub, GitLab ou outro host git. Veja [Hospedar e distribuir marketplaces](#host-and-distribute-marketplaces).
4. **Compartilhar com usuários**: usuários adicionam seu marketplace com `/plugin marketplace add` e instalam plugins individuais. Veja [Descobrir e instalar plugins](https://code.claude.com/docs/pt/discover-plugins).

Depois que seu marketplace estiver ativo, você pode atualizá-lo fazendo push de alterações para seu repositório. Os usuários atualizam sua cópia local com `/plugin marketplace update`.

<h2 id="walkthrough-create-a-local-marketplace">
  Passo a passo: criar um marketplace local
</h2>

Este exemplo cria um marketplace com um plugin: uma skill `quality-review` para revisões de código. Você criará a estrutura de diretórios, adicionará uma skill, criará o manifesto do plugin e o catálogo do marketplace, depois instalará e testará.

<Steps>
  <Step title="Criar a estrutura de diretórios">
    ```bash theme={null}
    mkdir -p my-marketplace/.claude-plugin
    mkdir -p my-marketplace/plugins/quality-review-plugin/.claude-plugin
    mkdir -p my-marketplace/plugins/quality-review-plugin/skills/quality-review
    ```
  </Step>

  <Step title="Criar a skill">
    Crie um arquivo `SKILL.md` que define o que a skill `quality-review` faz.

    ```markdown my-marketplace/plugins/quality-review-plugin/skills/quality-review/SKILL.md theme={null}
    ---
    description: Revisar código para bugs, segurança e desempenho
    ---

    Revise o código que selecionei ou as alterações recentes para:
    - Possíveis bugs ou casos extremos
    - Preocupações de segurança
    - Problemas de desempenho
    - Melhorias de legibilidade

    Seja conciso e acionável.
    ```
  </Step>

  <Step title="Criar o manifesto do plugin">
    Crie um arquivo `plugin.json` que descreve o plugin. O manifesto vai no diretório `.claude-plugin/`.

    ```json my-marketplace/plugins/quality-review-plugin/.claude-plugin/plugin.json theme={null}
    {
      "name": "quality-review-plugin",
      "description": "Adiciona uma skill quality-review para revisões rápidas de código",
      "version": "1.0.0"
    }
    ```

    <Note>
      Definir `version` significa que os usuários só recebem atualizações quando você altera este campo, então aumente-o em cada lançamento. Se você omitir `version` e hospedar este marketplace no git, cada commit conta automaticamente como uma nova versão. Veja [Resolução de versão](#version-resolution-and-release-channels) para escolher a abordagem correta.
    </Note>
  </Step>

  <Step title="Criar o arquivo de marketplace">
    Crie o catálogo de marketplace que lista seu plugin.

    ```json my-marketplace/.claude-plugin/marketplace.json theme={null}
    {
      "name": "my-plugins",
      "owner": {
        "name": "Seu Nome"
      },
      "plugins": [
        {
          "name": "quality-review-plugin",
          "source": "./plugins/quality-review-plugin",
          "description": "Adiciona uma skill quality-review para revisões rápidas de código"
        }
      ]
    }
    ```
  </Step>

  <Step title="Adicionar e instalar">
    Adicione o marketplace e instale o plugin.

    ```shell theme={null}
    /plugin marketplace add ./my-marketplace
    /plugin install quality-review-plugin@my-plugins
    ```
  </Step>

  <Step title="Experimentar">
    Selecione algum código em seu editor e execute sua nova skill. As skills do plugin são nomeadas com o nome do plugin.

    ```shell theme={null}
    /quality-review-plugin:quality-review
    ```
  </Step>
</Steps>

Para saber mais sobre o que os plugins podem fazer, incluindo hooks, agents, MCP servers e LSP servers, veja [Plugins](https://code.claude.com/docs/pt/plugins).

<Note>
  **Como os plugins são instalados**: Quando os usuários instalam um plugin, Claude Code copia o diretório do plugin para um local de cache. Isso significa que os plugins não podem referenciar arquivos fora de seu diretório usando caminhos como `../shared-utils`, porque esses arquivos não serão copiados.

  Se você precisar compartilhar arquivos entre plugins, use symlinks. Veja [Plugin caching and file resolution](https://code.claude.com/docs/pt/plugins-reference#plugin-caching-and-file-resolution) para detalhes.
</Note>

<h2 id="create-the-marketplace-file">
  Criar o arquivo de marketplace
</h2>

Crie `.claude-plugin/marketplace.json` na raiz do seu repositório. Este arquivo define o nome do seu marketplace, informações do proprietário e uma lista de plugins com suas fontes.

Cada entrada de plugin precisa no mínimo de um `name` e um `source` que diz ao Claude Code onde buscá-lo. Veja o [esquema completo](#marketplace-schema) abaixo para todos os campos disponíveis.

```json theme={null}
{
  "name": "company-tools",
  "owner": {
    "name": "DevTools Team",
    "email": "devtools@example.com"
  },
  "plugins": [
    {
      "name": "code-formatter",
      "source": "./plugins/formatter",
      "description": "Formatação automática de código ao salvar",
      "version": "2.1.0",
      "author": {
        "name": "DevTools Team"
      }
    },
    {
      "name": "deployment-tools",
      "source": {
        "source": "github",
        "repo": "company/deploy-plugin"
      },
      "description": "Ferramentas de automação de implantação"
    }
  ]
}
```

<h2 id="marketplace-schema">
  Esquema de marketplace
</h2>

<h3 id="required-fields">
  Campos obrigatórios
</h3>

| Campo     | Tipo   | Descrição                                                                                                                                                                                                                                                                                                                                                                                                                                                | Exemplo        |
| :-------- | :----- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------------- |
| `name`    | string | Identificador de marketplace (kebab-case, sem espaços). Isso é público: os usuários o veem ao instalar plugins (por exemplo, `/plugin install my-tool@your-marketplace`). Cada usuário pode registrar apenas um marketplace por nome: adicionar um segundo marketplace com o mesmo nome substitui o primeiro. Para publicar múltiplos plugins sob um nome de marketplace, liste-os todos em um único [`marketplace.json`](#create-the-marketplace-file). | `"acme-tools"` |
| `owner`   | object | Informações do mantenedor do marketplace ([veja campos abaixo](#owner-fields))                                                                                                                                                                                                                                                                                                                                                                           |                |
| `plugins` | array  | Lista de plugins disponíveis                                                                                                                                                                                                                                                                                                                                                                                                                             | Veja abaixo    |

<Note>
  **Nomes reservados**: os seguintes nomes de marketplace são reservados para uso oficial da Anthropic e não podem ser usados por marketplaces de terceiros: `claude-code-marketplace`, `claude-code-plugins`, `claude-plugins-official`, `claude-plugins-community`, `claude-community`, `anthropic-marketplace`, `anthropic-plugins`, `agent-skills`, `anthropic-agent-skills`, `knowledge-work-plugins`, `life-sciences`, `claude-for-legal`, `claude-for-financial-services`, `financial-services-plugins`, `first-party-plugins`, `healthcare`. Nomes que imitam marketplaces oficiais, como `official-claude-plugins` ou `anthropic-plugins-v2`, também são bloqueados. Reservar esses nomes impede que um marketplace de terceiros se apresente como uma fonte publicada pela Anthropic.

  Claude Code verifica novamente os nomes reservados toda vez que carrega um marketplace, não apenas quando você adiciona um. Um marketplace que foi registrado sob um desses nomes antes do nome se tornar reservado para de carregar e relata que está [registrado de uma fonte não confiável](https://code.claude.com/docs/pt/errors#marketplace-is-registered-from-an-untrusted-source). Remova esse marketplace e adicione-o novamente da fonte oficial da Anthropic. Um marketplace de terceiros afetado por um nome recém-reservado carrega novamente assim que você o adiciona novamente sob um nome diferente. Antes da v2.1.205, `first-party-plugins` e `healthcare` não eram reservados, e um marketplace já registrado sob um nome reservado continuava carregando.
</Note>

<h3 id="owner-fields">
  Campos do proprietário
</h3>

| Campo   | Tipo   | Obrigatório | Descrição                      |
| :------ | :----- | :---------- | :----------------------------- |
| `name`  | string | Sim         | Nome do mantenedor ou equipe   |
| `email` | string | Não         | Email de contato do mantenedor |

<h3 id="optional-fields">
  Campos opcionais
</h3>

| Campo                                 | Tipo   | Descrição                                                                                                                                                                                                                                                                                                                    |
| :------------------------------------ | :----- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `$schema`                             | string | URL do JSON Schema para autocompletar e validação do editor. Claude Code ignora este campo no momento do carregamento.                                                                                                                                                                                                       |
| `description`                         | string | Breve descrição do marketplace                                                                                                                                                                                                                                                                                               |
| `version`                             | string | Versão do manifesto do marketplace                                                                                                                                                                                                                                                                                           |
| `metadata.pluginRoot`                 | string | Diretório base adicionado aos caminhos de fonte de plugin relativos (por exemplo, `"./plugins"` permite escrever `"source": "formatter"` em vez de `"source": "./plugins/formatter"`)                                                                                                                                        |
| `allowCrossMarketplaceDependenciesOn` | array  | Outros marketplaces que plugins neste marketplace podem depender. Dependências de um marketplace não listado aqui são bloqueadas na instalação. Veja [Depender de um plugin de outro marketplace](https://code.claude.com/docs/pt/plugin-dependencies#depend-on-a-plugin-from-another-marketplace).                                                      |
| `renames`                             | object | Mapa de um antigo `name` de plugin para seu nome atual, ou para `null` se o plugin foi removido. Permite que usuários existentes migrem automaticamente quando você renomeia ou remove uma entrada em `plugins`. Veja [Renomear ou remover um plugin](#rename-or-remove-a-plugin). Requer Claude Code v2.1.193 ou posterior. |

`description` e `version` também são aceitos sob `metadata` para compatibilidade com versões anteriores.

<h2 id="plugin-entries">
  Entradas de plugin
</h2>

Cada entrada de plugin no array `plugins` descreve um plugin e onde encontrá-lo. Você pode incluir qualquer campo do [esquema de manifesto de plugin](https://code.claude.com/docs/pt/plugins-reference#plugin-manifest-schema), como `description`, `version`, `author`, `commands` e `hooks`, além destes campos específicos do marketplace: `source`, `category`, `tags`, `strict` e `relevance`.

<h3 id="required-fields-2">
  Campos obrigatórios
</h3>

| Campo    | Tipo           | Descrição                                                                                                                                                 |
| :------- | :------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `name`   | string         | Identificador de plugin (kebab-case, sem espaços). Isso é público: os usuários o veem ao instalar (por exemplo, `/plugin install my-plugin@marketplace`). |
| `source` | string\|object | Onde buscar o plugin (veja [Fontes de plugin](#plugin-sources) abaixo)                                                                                    |

<h3 id="optional-plugin-fields">
  Campos de plugin opcionais
</h3>

**Campos de metadados padrão:**

| Campo            | Tipo    | Descrição                                                                                                                                                                                                                                                                                                                                 |
| :--------------- | :------ | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `displayName`    | string  | Nome legível por humanos exibido em superfícies de UI. Volta para `name` quando omitido. Pode conter espaços e qualquer capitalização. Não é usado para namespacing ou lookup. Requer Claude Code v2.1.143 ou posterior.                                                                                                                  |
| `description`    | string  | Breve descrição do plugin                                                                                                                                                                                                                                                                                                                 |
| `version`        | string  | Versão do plugin. Se definido (aqui ou em `plugin.json`), o plugin é fixado a esta string e os usuários recebem atualizações apenas quando ela muda. Omita para usar o SHA do commit do git. Veja [Resolução de versão](#version-resolution-and-release-channels).                                                                        |
| `author`         | object  | Informações do autor do plugin (`name` obrigatório, `email` opcional)                                                                                                                                                                                                                                                                     |
| `homepage`       | string  | URL da página inicial ou documentação do plugin                                                                                                                                                                                                                                                                                           |
| `repository`     | string  | URL do repositório de código-fonte                                                                                                                                                                                                                                                                                                        |
| `license`        | string  | Identificador de licença SPDX (por exemplo, MIT, Apache-2.0)                                                                                                                                                                                                                                                                              |
| `keywords`       | array   | Tags para descoberta e categorização de plugins                                                                                                                                                                                                                                                                                           |
| `category`       | string  | Categoria do plugin para organização                                                                                                                                                                                                                                                                                                      |
| `tags`           | array   | Tags para pesquisabilidade                                                                                                                                                                                                                                                                                                                |
| `strict`         | boolean | Controla se `plugin.json` é a autoridade para definições de componentes (padrão: true). Veja [Strict mode](#strict-mode) abaixo.                                                                                                                                                                                                          |
| `relevance`      | object  | Sinais que informam ao Claude Code quando sugerir este plugin aos usuários. Tem efeito apenas para marketplaces que um administrador coloca na lista de permissões em configurações gerenciadas. Veja [Recomendar plugins para sua organização](https://code.claude.com/docs/pt/plugin-relevance). Requer Claude Code v2.1.152 ou posterior.                          |
| `defaultEnabled` | boolean | Se o plugin está habilitado após a instalação (padrão: true). Defina como `false` para instalar o plugin desabilitado até que o usuário opte por ativá-lo. Tem precedência sobre o mesmo campo no `plugin.json` do plugin. Veja [Default enablement](https://code.claude.com/docs/pt/plugins-reference#default-enablement). Requer Claude Code v2.1.154 ou posterior. |

**Campos de configuração de componentes:**

| Campo        | Tipo           | Descrição                                                                   |
| :----------- | :------------- | :-------------------------------------------------------------------------- |
| `skills`     | string\|array  | Caminhos personalizados para diretórios de skill contendo `<name>/SKILL.md` |
| `commands`   | string\|array  | Caminhos personalizados para arquivos de skill `.md` simples ou diretórios  |
| `agents`     | string\|array  | Caminhos personalizados para arquivos de agent                              |
| `hooks`      | string\|object | Configuração de hooks personalizada ou caminho para arquivo de hooks        |
| `mcpServers` | string\|object | Configurações de MCP server ou caminho para config de MCP                   |
| `lspServers` | string\|object | Configurações de LSP server ou caminho para config de LSP                   |

<h2 id="plugin-sources">
  Fontes de plugin
</h2>

As fontes de plugin informam ao Claude Code onde buscar cada plugin individual listado em seu marketplace. Elas são definidas no campo `source` de cada entrada de plugin em `marketplace.json`.

Depois que um plugin é clonado ou copiado para a máquina local, ele é copiado para o cache de plugin versionado local em `~/.claude/plugins/cache`.

| Fonte            | Tipo                                    | Campos                             | Notas                                                                                                                                                          |
| ---------------- | --------------------------------------- | ---------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Caminho relativo | `string` (por exemplo, `"./my-plugin"`) | nenhum                             | Diretório local dentro do repositório de marketplace. Deve começar com `./`. Resolvido relativamente à raiz do marketplace, não ao diretório `.claude-plugin/` |
| `github`         | object                                  | `repo`, `ref?`, `sha?`             |                                                                                                                                                                |
| `url`            | object                                  | `url`, `ref?`, `sha?`              | Fonte de URL Git                                                                                                                                               |
| `git-subdir`     | object                                  | `url`, `path`, `ref?`, `sha?`      | Subdiretório dentro de um repositório git. Clona esparsamente para minimizar largura de banda para monorepos                                                   |
| `npm`            | object                                  | `package`, `version?`, `registry?` | Instalado via `npm install`                                                                                                                                    |

<Note>
  **Fontes de marketplace vs fontes de plugin**: Estes são conceitos diferentes que controlam coisas diferentes.

  * **Fonte de marketplace**: onde buscar o próprio catálogo `marketplace.json`. Definido quando os usuários executam `/plugin marketplace add` ou em configurações `extraKnownMarketplaces`. Suporta `ref` (branch/tag) mas não `sha`.
  * **Fonte de plugin**: onde buscar um plugin individual listado no marketplace. Definido no campo `source` de cada entrada de plugin dentro de `marketplace.json`. Suporta tanto `ref` (branch/tag) quanto `sha` (commit exato).

  Por exemplo, um marketplace hospedado em `acme-corp/plugin-catalog` (fonte de marketplace) pode listar um plugin buscado de `acme-corp/code-formatter` (fonte de plugin). A fonte de marketplace e a fonte de plugin apontam para repositórios diferentes e são fixadas independentemente.
</Note>

Os tipos de fonte baseados em git abaixo são `github`, `url` e `git-subdir`. Quando tanto `ref` quanto `sha` são definidos em qualquer um deles, o `sha` é o pino efetivo. Claude Code busca e faz checkout do commit fixado diretamente.

Na maioria dos hosts git, incluindo GitHub, GitLab e Bitbucket, isso significa que a instalação é bem-sucedida mesmo se o branch ou tag nomeado por `ref` tenha sido deletado upstream, desde que o commit ainda seja alcançável a partir do repositório. Alguns servidores, como AWS CodeCommit, não suportam busca de commits por SHA. Nesses servidores, o `ref` ainda deve existir e o commit fixado deve ser alcançável a partir dele.

<h3 id="relative-paths">
  Caminhos relativos
</h3>

Para plugins no mesmo repositório, use um caminho começando com `./`:

```json theme={null}
{
  "name": "my-plugin",
  "source": "./plugins/my-plugin"
}
```

Os caminhos são resolvidos relativamente à raiz do marketplace, que é o diretório contendo `.claude-plugin/`. No exemplo acima, `./plugins/my-plugin` aponta para `<repo>/plugins/my-plugin`, mesmo que `marketplace.json` viva em `<repo>/.claude-plugin/marketplace.json`. Não use `../` para referenciar caminhos fora da raiz do marketplace.

<Note>
  Caminhos relativos são resolvidos contra uma cópia local do marketplace, então funcionam quando os usuários adicionam seu marketplace de uma fonte git ou um diretório local. Se os usuários adicionarem seu marketplace via URL direta para o arquivo `marketplace.json`, caminhos relativos não serão resolvidos, porque apenas esse arquivo é baixado. Para distribuição baseada em URL, use fontes GitHub, npm ou URL git. Veja [Troubleshooting](#plugins-with-relative-paths-fail-in-url-based-marketplaces) para detalhes.
</Note>

<h3 id="github-repositories">
  Repositórios GitHub
</h3>

```json theme={null}
{
  "name": "github-plugin",
  "source": {
    "source": "github",
    "repo": "owner/plugin-repo"
  }
}
```

Você pode fixar a um branch, tag ou commit específico:

```json theme={null}
{
  "name": "github-plugin",
  "source": {
    "source": "github",
    "repo": "owner/plugin-repo",
    "ref": "v2.0.0",
    "sha": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0"
  }
}
```

| Campo  | Tipo   | Descrição                                                                           |
| :----- | :----- | :---------------------------------------------------------------------------------- |
| `repo` | string | Obrigatório. Repositório GitHub no formato `owner/repo`                             |
| `ref`  | string | Opcional. Branch ou tag Git (padrão é o branch padrão do repositório)               |
| `sha`  | string | Opcional. SHA de commit git completo de 40 caracteres para fixar a uma versão exata |

<h3 id="git-repositories">
  Repositórios Git
</h3>

```json theme={null}
{
  "name": "git-plugin",
  "source": {
    "source": "url",
    "url": "https://gitlab.com/team/plugin.git"
  }
}
```

Você pode fixar a um branch, tag ou commit específico:

```json theme={null}
{
  "name": "git-plugin",
  "source": {
    "source": "url",
    "url": "https://gitlab.com/team/plugin.git",
    "ref": "main",
    "sha": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0"
  }
}
```

| Campo | Tipo   | Descrição                                                                                                                                                           |
| :---- | :----- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `url` | string | Obrigatório. URL completa do repositório git (`https://` ou `git@`). O sufixo `.git` é opcional, então URLs do Azure DevOps e AWS CodeCommit sem o sufixo funcionam |
| `ref` | string | Opcional. Branch ou tag Git (padrão é o branch padrão do repositório)                                                                                               |
| `sha` | string | Opcional. SHA de commit git completo de 40 caracteres para fixar a uma versão exata                                                                                 |

<h3 id="git-subdirectories">
  Subdiretórios Git
</h3>

Use `git-subdir` para apontar para um plugin que vive dentro de um subdiretório de um repositório git. Claude Code usa um clone parcial e esparso para buscar apenas o subdiretório, minimizando largura de banda para grandes monorepos.

```json theme={null}
{
  "name": "my-plugin",
  "source": {
    "source": "git-subdir",
    "url": "https://github.com/acme-corp/monorepo.git",
    "path": "tools/claude-plugin"
  }
}
```

Você pode fixar a um branch, tag ou commit específico:

```json theme={null}
{
  "name": "my-plugin",
  "source": {
    "source": "git-subdir",
    "url": "https://github.com/acme-corp/monorepo.git",
    "path": "tools/claude-plugin",
    "ref": "v2.0.0",
    "sha": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0"
  }
}
```

O campo `url` também aceita atalho GitHub (`owner/repo`) ou URLs SSH (`git@github.com:owner/repo.git`).

| Campo  | Tipo   | Descrição                                                                                                           |
| :----- | :----- | :------------------------------------------------------------------------------------------------------------------ |
| `url`  | string | Obrigatório. URL do repositório Git, atalho GitHub `owner/repo` ou URL SSH                                          |
| `path` | string | Obrigatório. Caminho do subdiretório dentro do repositório contendo o plugin (por exemplo, `"tools/claude-plugin"`) |
| `ref`  | string | Opcional. Branch ou tag Git (padrão é o branch padrão do repositório)                                               |
| `sha`  | string | Opcional. SHA de commit git completo de 40 caracteres para fixar a uma versão exata                                 |

<h3 id="npm-packages">
  Pacotes npm
</h3>

Plugins distribuídos como pacotes npm são instalados usando `npm install`. Isso funciona com qualquer pacote no registro npm público ou um registro privado que sua equipe hospeda.

```json theme={null}
{
  "name": "my-npm-plugin",
  "source": {
    "source": "npm",
    "package": "@acme/claude-plugin"
  }
}
```

Para fixar a uma versão específica, adicione o campo `version`:

```json theme={null}
{
  "name": "my-npm-plugin",
  "source": {
    "source": "npm",
    "package": "@acme/claude-plugin",
    "version": "2.1.0"
  }
}
```

Para instalar de um registro privado ou interno, adicione o campo `registry`:

```json theme={null}
{
  "name": "my-npm-plugin",
  "source": {
    "source": "npm",
    "package": "@acme/claude-plugin",
    "version": "^2.0.0",
    "registry": "https://npm.example.com"
  }
}
```

| Campo      | Tipo   | Descrição                                                                                               |
| :--------- | :----- | :------------------------------------------------------------------------------------------------------ |
| `package`  | string | Obrigatório. Nome do pacote ou pacote com escopo (por exemplo, `@org/plugin`)                           |
| `version`  | string | Opcional. Versão ou intervalo de versão (por exemplo, `2.1.0`, `^2.0.0`, `~1.5.0`)                      |
| `registry` | string | Opcional. URL de registro npm personalizado. Padrão é o registro npm do sistema (tipicamente npmjs.org) |

<h3 id="advanced-plugin-entries">
  Entradas de plugin avançadas
</h3>

Este exemplo mostra uma entrada de plugin usando muitos dos campos opcionais, incluindo caminhos personalizados para commands, agents, hooks e MCP servers:

```json theme={null}
{
  "name": "enterprise-tools",
  "source": {
    "source": "github",
    "repo": "company/enterprise-plugin"
  },
  "description": "Ferramentas de automação de fluxo de trabalho empresarial",
  "version": "2.1.0",
  "author": {
    "name": "Enterprise Team",
    "email": "enterprise@example.com"
  },
  "homepage": "https://docs.example.com/plugins/enterprise-tools",
  "repository": "https://github.com/company/enterprise-plugin",
  "license": "MIT",
  "keywords": ["enterprise", "workflow", "automation"],
  "category": "productivity",
  "commands": [
    "./commands/core/",
    "./commands/enterprise/",
    "./commands/experimental/preview.md"
  ],
  "agents": ["./agents/security-reviewer.md", "./agents/compliance-checker.md"],
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "${CLAUDE_PLUGIN_ROOT}/scripts/validate.sh"
          }
        ]
      }
    ]
  },
  "mcpServers": {
    "enterprise-db": {
      "command": "${CLAUDE_PLUGIN_ROOT}/servers/db-server",
      "args": ["--config", "${CLAUDE_PLUGIN_ROOT}/config.json"]
    }
  },
  "strict": false
}
```

Coisas importantes a notar:

* **`commands` e `agents`**: você pode especificar múltiplos diretórios ou arquivos individuais. Os caminhos são relativos à raiz do plugin.
* **`${CLAUDE_PLUGIN_ROOT}`**: use esta variável em hooks e configurações de MCP server para referenciar arquivos dentro do diretório de instalação do plugin. Isso é necessário porque os plugins são copiados para um local de cache quando instalados.
  * Veja a [tabela de substituição](https://code.claude.com/docs/pt/plugins-reference#environment-variables) para quais campos de configuração a substituem por tipo de servidor
  * Para dependências ou estado que devem sobreviver a atualizações de plugin, use [`${CLAUDE_PLUGIN_DATA}`](https://code.claude.com/docs/pt/plugins-reference#persistent-data-directory) em vez disso
* **`strict: false`**: como isso está definido como false, o plugin não precisa de seu próprio `plugin.json`. A entrada de marketplace define tudo. Veja [Strict mode](#strict-mode) abaixo.

Por padrão, as skills de um plugin são carregadas do diretório `skills/` sob sua `source`. Os caminhos listados no campo `skills` adicionam a essa varredura:

```json theme={null}
"skills": ["./skills/", "./extra-skills/"]
```

Quando várias entradas de plugin compartilham uma pasta `skills/` na raiz do marketplace (`source: "./"`), liste subdiretórios específicos em vez disso para que cada entrada carregue apenas suas próprias skills:

```json theme={null}
"source": "./",
"skills": ["./skills/code-review", "./skills/docs"]
```

Com uma `source` de raiz de marketplace, os caminhos listados são o conjunto completo para essa entrada, e outros diretórios na pasta `skills/` compartilhada não são carregados. Listar `./skills/` em si, ou a raiz do plugin, mantém a varredura completa. Se nenhum dos caminhos listados existir, a varredura padrão é executada em vez disso.

<h3 id="strict-mode">
  Strict mode
</h3>

O campo `strict` controla se `plugin.json` é a autoridade para definições de componentes (skills, agents, hooks, MCP servers, output styles).

| Valor           | Comportamento                                                                                                                                                      |
| :-------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `true` (padrão) | `plugin.json` é a autoridade. A entrada de marketplace pode complementá-lo com componentes adicionais, e ambas as fontes são mescladas.                            |
| `false`         | A entrada de marketplace é a definição completa. Se o plugin também tem um `plugin.json` que declara componentes, isso é um conflito e o plugin falha ao carregar. |

**Quando usar cada modo:**

* **`strict: true`**: o plugin tem seu próprio `plugin.json` e gerencia seus próprios componentes. A entrada de marketplace pode adicionar skills ou hooks extras no topo. Este é o padrão e funciona para a maioria dos plugins.
* **`strict: false`**: o operador do marketplace quer controle total. O repositório do plugin fornece arquivos brutos, e a entrada de marketplace define quais desses arquivos são expostos como skills, agents, hooks, etc. Útil quando o marketplace reestrutura ou curada os componentes de um plugin de forma diferente do que o autor do plugin pretendia.

<h2 id="host-and-distribute-marketplaces">
  Hospedar e distribuir marketplaces
</h2>

<h3 id="host-on-github-recommended">
  Hospedar no GitHub (recomendado)
</h3>

GitHub é a forma recomendada para hospedar e distribuir um marketplace:

1. **Criar um repositório**: configure um novo repositório para seu marketplace
2. **Adicionar arquivo de marketplace**: crie `.claude-plugin/marketplace.json` com suas definições de plugin
3. **Compartilhar com equipes**: os usuários adicionam seu marketplace com `/plugin marketplace add owner/repo`

**Benefícios**: controle de versão integrado, rastreamento de problemas e recursos de colaboração em equipe.

<h3 id="host-on-other-git-services">
  Hospedar em outros serviços git
</h3>

Qualquer serviço de hospedagem git funciona, como GitLab, Bitbucket e servidores auto-hospedados. Os usuários adicionam com a URL completa do repositório:

```shell theme={null}
/plugin marketplace add https://gitlab.com/company/plugins.git
```

<h3 id="private-repositories">
  Repositórios privados
</h3>

Claude Code suporta instalar plugins de repositórios privados. Para instalação manual e atualizações, Claude Code usa seus ajudantes de credencial git existentes, então acesso HTTPS via `gh auth login`, Keychain do macOS ou `git-credential-store` funciona da mesma forma que em seu terminal. Acesso SSH funciona desde que o host já esteja em seu arquivo `known_hosts` e a chave esteja carregada em `ssh-agent`, já que Claude Code suprime prompts SSH interativos para a impressão digital do host e passphrase da chave. O atalho `owner/repo` do GitHub clona por SSH por padrão; defina [`CLAUDE_CODE_PLUGIN_PREFER_HTTPS=1`](https://code.claude.com/docs/pt/env-vars#variables) para cloná-los via HTTPS em vez disso.

As atualizações automáticas em segundo plano funcionam de forma diferente. Por padrão, a atualização em segundo plano desabilita ajudantes de credencial git para seu `git pull`, então o pull não consegue autenticar em repositórios privados via HTTPS mesmo quando um ajudante está configurado. Remotos SSH não são afetados: uma chave carregada em `ssh-agent` autentica pulls em segundo plano da mesma forma que operações manuais. Quando o pull em segundo plano falha, Claude Code volta a re-clonar o marketplace do zero. O re-clone usa suas credenciais git armazenadas, mas pode [expirar em repositórios grandes](#git-operations-time-out), então atualizações automáticas de marketplace privado podem falhar intermitentemente.

Duas configurações fazem marketplaces privados se comportarem de forma previsível:

* Defina `CLAUDE_CODE_PLUGIN_KEEP_MARKETPLACE_ON_FAILURE=1` para manter o clone existente quando o pull em segundo plano falha, em vez de deletar e re-clonar. Seus plugins continuam funcionando a partir do último estado sincronizado, e atualizações manuais com `/plugin marketplace update` ainda fazem pull com suas credenciais.
* Configure um ajudante de credencial git, por exemplo com `gh auth setup-git` para GitHub, para que o fallback de re-clone possa autenticar sem solicitar.

Definir um token de provedor como `GITHUB_TOKEN` em seu ambiente não habilita autenticação em segundo plano por si só. Tokens têm efeito apenas através de um ajudante de credencial configurado, por exemplo o ajudante CLI `gh`, que lê `GH_TOKEN` e `GITHUB_TOKEN`.

Para fazer o pull em segundo plano autenticar via HTTPS, configure uma reescrita de URL git global. A reescrita incorpora um token na URL remota, então tem efeito mesmo que o pull em segundo plano desabilite ajudantes de credencial, e um pull bem-sucedido pula o fallback de re-clone. O exemplo a seguir reescreve a URL do repositório de marketplace para incluir um token de acesso:

```bash theme={null}
git config --global url."https://x-access-token:YOUR_TOKEN@github.com/acme-corp/plugins".insteadOf "https://github.com/acme-corp/plugins"
```

Escope a reescrita para o repositório de marketplace ou caminho de organização. Uma reescrita cuja base é apenas o host se aplica a cada fetch e push para esse host na máquina e substitui suas credenciais normais, incluindo pushes para seus próprios repositórios.

Cada provedor espera um nome de usuário diferente na URL reescrita, e o mesmo escopo de caminho se aplica a cada provedor. Para servidores auto-hospedados, substitua o nome do host pelo nome do host do seu servidor:

| Provedor  | Forma de URL reescrita                                            |
| :-------- | :---------------------------------------------------------------- |
| GitHub    | `https://x-access-token:YOUR_TOKEN@github.com/acme-corp/plugins`  |
| GitLab    | `https://oauth2:YOUR_TOKEN@gitlab.com/acme-corp/plugins`          |
| Bitbucket | `https://x-token-auth:YOUR_TOKEN@bitbucket.org/acme-corp/plugins` |

A reescrita armazena o token em texto simples em seu gitconfig, então use um token com acesso somente leitura ao repositório de marketplace.

<Note>
  Em ambientes CI/CD, configure um ajudante de credencial git antes de instalar plugins de repositórios privados. No GitHub Actions, exporte um token com acesso de leitura ao repositório de marketplace como `GH_TOKEN`, depois execute `gh auth setup-git`. O token de workflow padrão pode apenas acessar o repositório do próprio workflow, então um marketplace privado em outro repositório precisa de um token de acesso pessoal ou token de app. Uma reescrita de URL global configurada no pipeline também autentica o pull em segundo plano diretamente.
</Note>

<h3 id="test-locally-before-distribution">
  Testar localmente antes da distribuição
</h3>

Teste seu marketplace localmente antes de compartilhar:

```shell theme={null}
/plugin marketplace add ./my-marketplace
/plugin install quality-review-plugin@my-plugins
```

Para a gama completa de comandos add (GitHub, URLs Git, caminhos locais, URLs remotas), veja [Adicionar marketplaces](https://code.claude.com/docs/pt/discover-plugins#add-marketplaces).

<h3 id="require-marketplaces-for-your-team">
  Exigir marketplaces para sua equipe
</h3>

Você pode configurar seu repositório para que os membros da equipe sejam automaticamente solicitados a instalar seu marketplace quando confiarem na pasta do projeto. Adicione seu marketplace a `.claude/settings.json`:

```json theme={null}
{
  "extraKnownMarketplaces": {
    "company-tools": {
      "source": {
        "source": "github",
        "repo": "your-org/claude-plugins"
      }
    }
  }
}
```

Você também pode especificar quais plugins devem ser habilitados por padrão:

```json theme={null}
{
  "enabledPlugins": {
    "code-formatter@company-tools": true,
    "deployment-tools@company-tools": true
  }
}
```

Para opções de configuração completas, veja [Plugin settings](https://code.claude.com/docs/pt/settings#plugin-settings).

<Note>
  Se você usar uma fonte local `directory` ou `file` com um caminho relativo, o caminho é resolvido contra o checkout principal do seu repositório. Quando você executa Claude Code de um git worktree, o caminho ainda aponta para o checkout principal, então todos os worktrees compartilham o mesmo local de marketplace. O estado do marketplace é armazenado uma vez por usuário em `~/.claude/plugins/known_marketplaces.json`, não por projeto.
</Note>

<h3 id="pre-populate-plugins-for-containers">
  Pré-popular plugins para containers
</h3>

Para imagens de container e ambientes CI, você pode pré-popular um diretório de plugins no tempo de construção para que Claude Code inicie com marketplaces e plugins já disponíveis, sem clonar nada em tempo de execução. Defina a variável de ambiente `CLAUDE_CODE_PLUGIN_SEED_DIR` para apontar para este diretório.

Para colocar em camadas múltiplos diretórios seed, separe caminhos com `:` em Unix ou `;` no Windows. Claude Code procura cada diretório em ordem e usa o primeiro seed que contém um determinado marketplace ou cache de plugin.

O diretório seed espelha a estrutura de `~/.claude/plugins`:

```
$CLAUDE_CODE_PLUGIN_SEED_DIR/
  known_marketplaces.json
  marketplaces/<name>/...
  cache/<marketplace>/<plugin>/<version>/...
```

Para construir um diretório seed, execute Claude Code uma vez durante a construção da imagem, instale os plugins que você precisa, depois copie o diretório `~/.claude/plugins` resultante em sua imagem e aponte `CLAUDE_CODE_PLUGIN_SEED_DIR` para ele.

Para pular a etapa de cópia, defina `CLAUDE_CODE_PLUGIN_CACHE_DIR` para seu caminho de seed de destino durante a construção para que os plugins sejam instalados diretamente lá:

```bash theme={null}
CLAUDE_CODE_PLUGIN_CACHE_DIR=/opt/claude-seed claude plugin marketplace add your-org/plugins
CLAUDE_CODE_PLUGIN_CACHE_DIR=/opt/claude-seed claude plugin install my-tool@your-plugins
```

Então defina `CLAUDE_CODE_PLUGIN_SEED_DIR=/opt/claude-seed` no ambiente de tempo de execução do seu container para que Claude Code leia do seed na inicialização.

Na inicialização, Claude Code registra marketplaces encontrados no `known_marketplaces.json` do seed na configuração primária, e usa caches de plugin encontrados sob `cache/` no local sem re-clonar. Isso funciona tanto em modo interativo quanto em modo não-interativo com a flag `-p`.

Detalhes de comportamento:

* **Somente leitura**: o diretório seed nunca é escrito. As atualizações automáticas são desabilitadas para marketplaces seed já que git pull falharia em um sistema de arquivos somente leitura.
* **Entradas seed têm precedência**: marketplaces declarados no seed sobrescrevem qualquer entrada correspondente na configuração do usuário em cada inicialização. Para optar por não usar um plugin seed, use `/plugin disable` em vez de remover o marketplace.
* **Resolução de caminho**: Claude Code localiza conteúdo de marketplace sondando `$CLAUDE_CODE_PLUGIN_SEED_DIR/marketplaces/<name>/` em tempo de execução, não confiando em caminhos armazenados dentro do JSON do seed. Isso significa que o seed funciona corretamente mesmo quando montado em um caminho diferente de onde foi construído.
* **Mutação é bloqueada**: executar `/plugin marketplace remove` ou `/plugin marketplace update` contra um marketplace gerenciado por seed falha com orientação para pedir ao seu administrador para atualizar a imagem seed.
* **Compõe com configurações**: se `extraKnownMarketplaces` ou `enabledPlugins` declaram um marketplace que já existe no seed, Claude Code usa a cópia do seed em vez de clonar.

<h3 id="managed-marketplace-restrictions">
  Restrições de marketplace gerenciado
</h3>

Para organizações que exigem controle rigoroso sobre fontes de plugin, administradores podem restringir quais marketplaces de plugin os usuários podem adicionar usando a configuração [`strictKnownMarketplaces`](https://code.claude.com/docs/pt/settings#strictknownmarketplaces) em configurações gerenciadas. Para também rejeitar as flags CLI que carregam plugins, agentes e servidores MCP para uma única execução, combine com [`disableSideloadFlags`](https://code.claude.com/docs/pt/settings#available-settings). Para criar uma lista de permissões de quais plugins de marketplaces podem aparecer como sugestões de instalação contextual, defina [`pluginSuggestionMarketplaces`](https://code.claude.com/docs/pt/settings#available-settings).

Quando `strictKnownMarketplaces` é configurado em configurações gerenciadas, o comportamento de restrição depende do valor:

| Valor               | Comportamento                                                                                     |
| ------------------- | ------------------------------------------------------------------------------------------------- |
| Indefinido (padrão) | Sem restrições. Os usuários podem adicionar qualquer marketplace                                  |
| Array vazio `[]`    | Bloqueio completo. Os usuários não podem adicionar novos marketplaces                             |
| Lista de fontes     | Os usuários podem apenas adicionar marketplaces que correspondem exatamente à lista de permissões |

<h4 id="common-configurations">
  Configurações comuns
</h4>

Desabilitar todas as adições de marketplace:

```json theme={null}
{
  "strictKnownMarketplaces": []
}
```

Permitir apenas marketplaces específicos:

```json theme={null}
{
  "strictKnownMarketplaces": [
    {
      "source": "github",
      "repo": "acme-corp/approved-plugins"
    },
    {
      "source": "github",
      "repo": "acme-corp/security-tools",
      "ref": "v2.0"
    },
    {
      "source": "url",
      "url": "https://plugins.example.com/marketplace.json"
    }
  ]
}
```

Permitir todos os marketplaces de um servidor git interno usando correspondência de padrão regex no host. Esta é a abordagem recomendada para [GitHub Enterprise Server](https://code.claude.com/docs/pt/github-enterprise-server#plugin-marketplaces-on-ghes) ou instâncias GitLab auto-hospedadas:

```json theme={null}
{
  "strictKnownMarketplaces": [
    {
      "source": "hostPattern",
      "hostPattern": "^github\\.example\\.com$"
    }
  ]
}
```

Permitir marketplaces baseados em sistema de arquivos de um diretório específico usando correspondência de padrão regex no caminho:

```json theme={null}
{
  "strictKnownMarketplaces": [
    {
      "source": "pathPattern",
      "pathPattern": "^/opt/approved/"
    }
  ]
}
```

Use `".*"` como `pathPattern` para permitir qualquer caminho de sistema de arquivos enquanto ainda controla fontes de rede com `hostPattern`.

<Note>
  `strictKnownMarketplaces` restringe o que os usuários podem adicionar, mas não registra marketplaces por conta própria. Para tornar marketplaces permitidos disponíveis automaticamente sem usuários executarem `/plugin marketplace add`, combine com [`extraKnownMarketplaces`](https://code.claude.com/docs/pt/settings#extraknownmarketplaces) no mesmo `managed-settings.json`. Veja [Usando ambos juntos](https://code.claude.com/docs/pt/settings#strictknownmarketplaces).
</Note>

<h4 id="how-restrictions-work">
  Como as restrições funcionam
</h4>

As restrições são verificadas antes de qualquer operação de rede ou sistema de arquivos. A verificação é executada na adição de marketplace e na instalação, atualização, atualização e auto-atualização de plugin. Se um marketplace foi adicionado antes da política ser configurada e sua fonte não corresponder mais à lista de permissões, Claude Code recusa instalar ou atualizar plugins a partir dele. A mesma aplicação se aplica a `blockedMarketplaces`.

A lista de permissões usa correspondência exata para a maioria dos tipos de fonte. Para um marketplace ser permitido, todos os campos especificados devem corresponder exatamente:

* Para fontes GitHub: `repo` é obrigatório, e `ref` ou `path` também devem corresponder se especificados na lista de permissões
* Para fontes de URL: a URL completa deve corresponder exatamente
* Para fontes `hostPattern`: o host do marketplace é correspondido contra o padrão regex
* Para fontes `pathPattern`: o caminho do sistema de arquivos do marketplace é correspondido contra o padrão regex

A correspondência exata não normaliza URLs: uma barra à direita, sufixo `.git` ou forma `ssh://` versus `https://` são tratados como valores diferentes. Se o marketplace da sua organização pode ser clonado por mais de uma forma de URL, prefira uma entrada `hostPattern` em vez de uma URL literal para que todas as formas correspondam.

Como `strictKnownMarketplaces` é definido em [configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files), configurações individuais de usuários e projetos não podem substituir essas restrições.

Para detalhes de configuração completos incluindo todos os tipos de fonte suportados e comparação com `extraKnownMarketplaces`, veja a [referência strictKnownMarketplaces](https://code.claude.com/docs/pt/settings#strictknownmarketplaces).

<h3 id="version-resolution-and-release-channels">
  Resolução de versão e canais de lançamento
</h3>

As versões de plugin determinam caminhos de cache e detecção de atualização: se a versão resolvida corresponder ao que um usuário já tem, `/plugin update` e auto-atualização pulam o plugin.

Claude Code resolve a versão de um plugin a partir do primeiro destes que está definido:

1. `version` no `plugin.json` do plugin
2. `version` na entrada de marketplace do plugin
3. O SHA do commit git da fonte do plugin

Para os tipos de fonte baseados em git `github`, `url`, `git-subdir` e caminhos relativos dentro de um marketplace hospedado em git, você pode omitir `version` inteiramente e cada novo commit é tratado como uma nova versão. Esta é a configuração mais simples para plugins internos ou em desenvolvimento ativo.

<Warning>
  Definir `version` fixa o plugin. Se `plugin.json` declara `"version": "1.0.0"`, fazer push de novos commits sem alterar essa string não faz nada para usuários existentes, porque Claude Code vê a mesma versão e mantém a cópia em cache. Aumente o campo em cada lançamento, ou omita-o para usar o SHA do commit.

  Evite definir `version` em ambos `plugin.json` e a entrada de marketplace. O valor `plugin.json` sempre vence silenciosamente, então uma versão de manifesto obsoleta pode mascarar uma versão que você definiu em `marketplace.json`.
</Warning>

<h4 id="set-up-release-channels">
  Configurar canais de lançamento
</h4>

Para suportar canais de lançamento "stable" e "latest" para seus plugins, você pode configurar dois marketplaces que apontam para diferentes refs ou SHAs do mesmo repositório. Você pode então atribuir os dois marketplaces a diferentes grupos de usuários através de [configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files).

<Warning>
  Cada canal deve resolver para uma versão diferente. Se você usar versões explícitas, `plugin.json` deve declarar uma `version` diferente em cada ref fixado. Se você omitir `version`, os SHAs de commit distintos já distinguem os canais. Se dois refs resolverem para a mesma string de versão, Claude Code os trata como idênticos e pula a atualização.
</Warning>

<h5 id="example">
  Exemplo
</h5>

```json theme={null}
{
  "name": "stable-tools",
  "plugins": [
    {
      "name": "code-formatter",
      "source": {
        "source": "github",
        "repo": "acme-corp/code-formatter",
        "ref": "stable"
      }
    }
  ]
}
```

```json theme={null}
{
  "name": "latest-tools",
  "plugins": [
    {
      "name": "code-formatter",
      "source": {
        "source": "github",
        "repo": "acme-corp/code-formatter",
        "ref": "latest"
      }
    }
  ]
}
```

<h5 id="assign-channels-to-user-groups">
  Atribuir canais a grupos de usuários
</h5>

Atribua cada marketplace ao grupo de usuários apropriado através de configurações gerenciadas. Por exemplo, o grupo stable recebe:

```json theme={null}
{
  "extraKnownMarketplaces": {
    "stable-tools": {
      "source": {
        "source": "github",
        "repo": "acme-corp/stable-tools"
      }
    }
  }
}
```

O grupo early-access recebe `latest-tools` em vez disso:

```json theme={null}
{
  "extraKnownMarketplaces": {
    "latest-tools": {
      "source": {
        "source": "github",
        "repo": "acme-corp/latest-tools"
      }
    }
  }
}
```

<h4 id="pin-dependency-versions">
  Fixar versões de dependência
</h4>

Um plugin pode restringir suas dependências a um intervalo semver para que atualizações de uma dependência não quebrem o plugin dependente. Veja [Restringir versões de dependência de plugin](https://code.claude.com/docs/pt/plugin-dependencies) para a convenção de git-tag `{plugin-name}--v{version}`, sintaxe de intervalo e como múltiplas restrições na mesma dependência são combinadas.

<h3 id="rename-or-remove-a-plugin">
  Renomear ou remover um plugin
</h3>

O `name` de um plugin é seu identificador estável. Os usuários o referenciam em `enabledPlugins`, `pluginConfigs` e comandos `/plugin install`, então alterá-lo quebra cada instalação existente. Para alterar o rótulo mostrado na UI sem quebrar instalações, defina [`displayName`](#optional-plugin-fields) e mantenha `name` inalterado.

Se você deve alterar o `name` de um plugin, ou remover um plugin do array `plugins`, adicione uma entrada de nível superior `renames` para que usuários existentes migrem em vez de ver um erro `plugin-not-found`. A migração automática requer Claude Code v2.1.193 ou posterior. Mapeie cada nome anterior para seu nome atual, ou para `null` se o plugin não existir mais. O exemplo a seguir renomeia `formatter` para `code-formatter` e registra que `legacy-linter` foi removido:

```json theme={null}
{
  "name": "acme-tools",
  "owner": { "name": "Acme" },
  "plugins": [
    { "name": "code-formatter", "source": "./plugins/code-formatter" }
  ],
  "renames": {
    "formatter": "code-formatter",
    "legacy-linter": null
  }
}
```

Quando um usuário inicia Claude Code com o nome antigo ainda em suas configurações, Claude Code segue o mapa `renames`:

* Se a entrada aponta para um novo nome, Claude Code carrega o plugin sob seu novo nome e mostra um aviso de uma linha como `Renamed to "code-formatter" in the "acme-tools" marketplace`. Ele então reescreve a chave antiga para a chave nova nos escopos de configurações do usuário, projeto e local para ambos `enabledPlugins` e `pluginConfigs`, para que o aviso apareça uma vez.
* Para uma entrada `null`, Claude Code descarta a chave antiga e o aviso relata que o plugin foi removido do marketplace.
* Se o plugin renomeado usa uma fonte remota como `github` ou `npm`, Claude Code relata `plugin-cache-miss` após o renome e o usuário deve executar `/plugin install` uma vez para buscá-lo sob o novo nome.

Trate `renames` como histórico apenas para anexação: mantenha entradas antigas no lugar mesmo depois que você espera que cada usuário tenha migrado. Claude Code segue cadeias, então se você depois renomear `code-formatter` para `formatter-pro`, adicione uma segunda entrada em vez de editar a primeira. Um usuário que ainda tem o `formatter` original habilitado então resolve através de ambas as entradas para `formatter-pro`.

Execute `claude plugin validate .` após editar o mapa; ele rejeita qualquer entrada cuja cadeia forma um ciclo ou não termina em `null` ou um nome listado em `plugins`.

<Note>
  Configurações gerenciadas e de política são somente leitura para Claude Code, então plugins habilitados lá não podem ser reescritos automaticamente. O plugin renomeado ainda carrega cada sessão, mas o aviso de renome recorre até que um administrador atualize `enabledPlugins` no arquivo de configurações gerenciadas para usar o novo nome. O mesmo se aplica a plugins habilitados através de outras fontes somente leitura como `--add-dir`.
</Note>

Versões anteriores de Claude Code ignoram o campo `renames` e relatam `plugin-not-found` para o nome antigo.

<h2 id="validation-and-testing">
  Validação e testes
</h2>

Teste seu marketplace antes de compartilhar.

Valide a sintaxe JSON do seu marketplace:

```bash theme={null}
claude plugin validate .
```

Ou de dentro de Claude Code:

```shell theme={null}
/plugin validate .
```

Adicione o marketplace para testes:

```shell theme={null}
/plugin marketplace add ./path/to/marketplace
```

Instale um plugin de teste para verificar se tudo funciona:

```shell theme={null}
/plugin install test-plugin@marketplace-name
```

Para fluxos de trabalho completos de testes de plugin, veja [Testar seus plugins localmente](https://code.claude.com/docs/pt/plugins#test-your-plugins-locally). Para troubleshooting técnico, veja [Plugins reference](https://code.claude.com/docs/pt/plugins-reference).

<h2 id="manage-marketplaces-from-the-cli">
  Gerenciar marketplaces a partir da CLI
</h2>

Claude Code fornece subcomandos `claude plugin marketplace` não-interativos para scripting e automação. Estes são equivalentes aos comandos `/plugin marketplace` disponíveis dentro de uma sessão interativa.

<h3 id="plugin-marketplace-add">
  Plugin marketplace add
</h3>

Adicione um marketplace de um repositório GitHub, URL git, URL remota ou caminho local.

```bash theme={null}
claude plugin marketplace add <source> [options]
```

**Argumentos:**

* `<source>`: Atalho GitHub `owner/repo`, URL git, URL remota para um arquivo `marketplace.json` ou caminho de diretório local. Para fixar a um branch ou tag, anexe `@ref` ao atalho GitHub ou `#ref` a uma URL git

Uma URL deve incluir seu esquema. A partir de Claude Code v2.1.196, um host digitado sem um, como `gitlab.example.com/team/plugins`, é rejeitado como um atalho `owner/repo` inválido e o erro informa para adicionar `https://` ou usar `./` para um caminho local. Versões anteriores o interpretavam como um caminho de repositório GitHub e falham no momento do clone com um erro de não encontrado do GitHub.

**Opções:**

| Opção                 | Descrição                                                                                                                                      | Padrão |
| :-------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------- | :----- |
| `--scope <scope>`     | Onde declarar o marketplace: `user`, `project` ou `local`. Veja [Plugin installation scopes](https://code.claude.com/docs/pt/plugins-reference#plugin-installation-scopes) | `user` |
| `--sparse <paths...>` | Limitar checkout a diretórios específicos via git sparse-checkout. Útil para monorepos                                                         |        |

Adicione um marketplace do GitHub usando atalho `owner/repo`:

```bash theme={null}
claude plugin marketplace add acme-corp/claude-plugins
```

Fixe a um branch ou tag específico com `@ref`:

```bash theme={null}
claude plugin marketplace add acme-corp/claude-plugins@v2.0
```

Adicione de uma URL git em um host não-GitHub:

```bash theme={null}
claude plugin marketplace add https://gitlab.example.com/team/plugins.git
```

Adicione de uma URL remota que serve o arquivo `marketplace.json` diretamente:

```bash theme={null}
claude plugin marketplace add https://example.com/marketplace.json
```

Adicione de um diretório local para testes:

```bash theme={null}
claude plugin marketplace add ./my-marketplace
```

Declare o marketplace no escopo do projeto para que seja compartilhado com sua equipe via `.claude/settings.json`:

```bash theme={null}
claude plugin marketplace add acme-corp/claude-plugins --scope project
```

Para um monorepo, limite o checkout aos diretórios que contêm conteúdo de plugin:

```bash theme={null}
claude plugin marketplace add acme-corp/monorepo --sparse .claude-plugin plugins
```

<h3 id="plugin-marketplace-list">
  Plugin marketplace list
</h3>

Liste todos os marketplaces configurados.

```bash theme={null}
claude plugin marketplace list [options]
```

**Opções:**

| Opção    | Descrição       |
| :------- | :-------------- |
| `--json` | Saída como JSON |

Com `--json`, cada entrada inclui `name`, `source` e campos específicos da fonte: `repo` para fontes GitHub, `url` para fontes git e URL, e `path` para fontes locais. Fontes GitHub e git também incluem um campo `ref` quando o marketplace foi adicionado com um branch ou tag fixado.

<h3 id="plugin-marketplace-remove">
  Plugin marketplace remove
</h3>

Remova um marketplace configurado. O alias `rm` também é aceito.

```bash theme={null}
claude plugin marketplace remove <name> [options]
```

**Argumentos:**

* `<name>`: nome do marketplace a remover, conforme mostrado por `claude plugin marketplace list`. Este é o `name` de `marketplace.json`, não a fonte que você passou para `add`

**Opções:**

| Opção             | Descrição                                                                                                                                                                                                                                                                                                                                                                                                                                     | Padrão             |
| :---------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :----------------- |
| `--scope <scope>` | Restringir remoção a um único escopo de configurações: `user`, `project` ou `local`. Veja [Plugin installation scopes](https://code.claude.com/docs/pt/plugins-reference#plugin-installation-scopes). Quando omitido, a declaração é removida de cada escopo editável. Quando fornecido, apenas a declaração desse escopo é removida; o estado compartilhado, cache e dados de plugin instalado são preservados quando o marketplace ainda está declarado em outro escopo | (todos os escopos) |

<Warning>
  Remover um marketplace de seu último escopo restante também desinstala qualquer plugin que você instalou dele. Para atualizar um marketplace sem perder plugins instalados, use `claude plugin marketplace update` em vez disso.
</Warning>

<h3 id="plugin-marketplace-update">
  Plugin marketplace update
</h3>

Atualize marketplaces de suas fontes para recuperar novos plugins e mudanças de versão. Um marketplace adicionado com um branch ou tag `ref` é atualizado para o commit mais recente dessa ref, não para o branch padrão do repositório.

```bash theme={null}
claude plugin marketplace update [name]
```

**Argumentos:**

* `[name]`: nome do marketplace a atualizar, conforme mostrado por `claude plugin marketplace list`. Atualiza todos os marketplaces se omitido

Tanto `remove` quanto `update` falham quando executados contra um marketplace gerenciado por seed, que é somente leitura. Ao atualizar todos os marketplaces, entradas gerenciadas por seed são puladas e outros marketplaces ainda são atualizados. Para alterar plugins fornecidos por seed, peça ao seu administrador para atualizar a imagem seed. Veja [Pré-popular plugins para containers](#pre-populate-plugins-for-containers).

<h2 id="troubleshooting">
  Troubleshooting
</h2>

<h3 id="marketplace-not-loading">
  Marketplace não carregando
</h3>

**Sintomas**: Não consegue adicionar marketplace ou ver plugins dele

**Soluções**:

* Verifique se a URL do marketplace é acessível
* Verifique se `.claude-plugin/marketplace.json` existe no caminho especificado
* Garanta que a sintaxe JSON é válida usando `claude plugin validate` ou `/plugin validate`. Para verificar o frontmatter de skill, agent e command, execute o comando contra cada diretório de plugin
* Para repositórios privados, confirme que você tem permissões de acesso

<h3 id="marketplace-validation-errors">
  Erros de validação de marketplace
</h3>

Execute `claude plugin validate .` ou `/plugin validate .` do seu diretório de marketplace para verificar problemas. Quando apontado para um diretório de marketplace, o validador verifica `marketplace.json` para erros de schema, nomes de plugin duplicados e travessia de caminho de fonte. Para cada entrada cuja `source` é um caminho local, ele também valida o próprio `plugin.json` daquele plugin e avisa quando a `version` da entrada não corresponde à do `plugin.json`. Problemas encontrados no `plugin.json` de um plugin são prefixados com o índice da entrada, na forma `plugins[2] plugin.json →`.

A partir de Claude Code v2.1.196, a passagem por entrada também:

* inclui plugins cuja `source` é `.`
* executa quando `marketplace.json` está fora de um diretório `.claude-plugin`, resolvendo fontes contra o próprio diretório do arquivo
* relata os problemas de cada entrada mesmo quando outra parte do arquivo tem erros de schema

Versões anteriores pulam plugins na raiz do marketplace e apenas descem de um `.claude-plugin/marketplace.json`.

Para validar o `plugin.json` de um plugin individual e seus arquivos de skill, agent, command e hook, execute o comando contra o diretório do plugin em si, por exemplo `claude plugin validate ./plugins/my-plugin`. Erros comuns:

| Erro                                              | Causa                                                  | Solução                                                                                                                                                 |
| :------------------------------------------------ | :----------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `File not found: .claude-plugin/marketplace.json` | Manifesto ausente                                      | Crie `.claude-plugin/marketplace.json` com campos obrigatórios                                                                                          |
| `Invalid JSON syntax: Unexpected token...`        | Erro de sintaxe JSON em marketplace.json               | Verifique vírgulas ausentes, vírgulas extras ou strings não citadas                                                                                     |
| `Duplicate plugin name "x" found in marketplace`  | Dois plugins compartilham o mesmo nome                 | Dê a cada plugin um valor `name` único                                                                                                                  |
| `plugins[0].source: Path contains ".."`           | Caminho de fonte contém `..`                           | Use caminhos relativos à raiz do marketplace sem `..`. Veja [Caminhos relativos](#relative-paths)                                                       |
| `YAML frontmatter failed to parse: ...`           | YAML inválido em um arquivo de skill, agent ou command | Corrija a sintaxe YAML no bloco frontmatter. Em tempo de execução este arquivo carrega sem metadados. Relatado apenas ao validar um diretório de plugin |
| `Invalid JSON syntax: ...` (hooks.json)           | `hooks/hooks.json` malformado                          | Corrija a sintaxe JSON. Um `hooks/hooks.json` malformado previne o plugin inteiro de carregar. Relatado apenas ao validar um diretório de plugin        |

**Avisos** (não bloqueadores):

* `Marketplace has no plugins defined`: adicione pelo menos um plugin ao array `plugins`
* `No marketplace description provided`: adicione uma `description` de nível superior para ajudar os usuários a entender seu marketplace
* `Plugin name "x" is not kebab-case`: o nome do plugin contém letras maiúsculas, espaços ou caracteres especiais. Renomeie para apenas letras minúsculas, dígitos e hífens (por exemplo, `my-plugin`). Claude Code aceita outras formas, mas a sincronização de marketplace do claude.ai as rejeita.

<h3 id="plugin-installation-failures">
  Falhas de instalação de plugin
</h3>

**Sintomas**: Marketplace aparece mas a instalação do plugin falha

**Soluções**:

* Verifique se as URLs de fonte do plugin são acessíveis
* Verifique se os diretórios de plugin contêm arquivos obrigatórios
* Para fontes GitHub, garanta que repositórios são públicos ou você tem acesso
* Teste fontes de plugin manualmente clonando/baixando
* Se a fonte fixa tanto `ref` quanto `sha`, uma branch ou tag upstream deletada não bloqueia a instalação na maioria dos hosts git, incluindo GitHub, GitLab e Bitbucket. Em servidores que não suportam busca de commits por SHA, como AWS CodeCommit, o `ref` ainda deve existir e o commit fixado deve ser alcançável a partir dele. Se a instalação ainda falhar, confirme que o commit fixado ainda existe no repositório

<h3 id="private-repository-authentication-fails">
  Falha de autenticação de repositório privado
</h3>

**Sintomas**: Erros de autenticação ao instalar plugins de repositórios privados

**Soluções**:

Para instalação manual e atualizações:

* Verifique se você está autenticado com seu provedor git (por exemplo, execute `gh auth status` para GitHub)
* Verifique se seu ajudante de credencial está configurado corretamente: `git config --global credential.helper`
* Tente clonar o repositório manualmente para verificar se suas credenciais funcionam

Para atualizações automáticas em segundo plano:

* Por padrão, atualizações em segundo plano desabilitam ajudantes de credencial git para o pull, então o pull não consegue autenticar sobre HTTPS. Remotes SSH com uma chave carregada em `ssh-agent` ainda autenticam. Um pull falhado dispara uma re-clonagem do zero, que usa suas credenciais armazenadas mas pode expirar em repositórios grandes
* Defina `CLAUDE_CODE_PLUGIN_KEEP_MARKETPLACE_ON_FAILURE=1` para manter o clone existente quando o pull em segundo plano falhar
* Configure um ajudante de credencial git, por exemplo `gh auth setup-git`, então o fallback de re-clonagem consegue autenticar
* Se a re-clonagem expirar em um repositório grande, aumente o limite com [`CLAUDE_CODE_PLUGIN_GIT_TIMEOUT_MS`](#git-operations-time-out)
* Configure uma [reescrita de URL git](#private-repositories) escopo para o repositório de marketplace para que o pull em segundo plano autentique diretamente
* Ou atualize marketplaces privados manualmente com `/plugin marketplace update <name>`, que usa suas credenciais

<h3 id="marketplace-updates-fail-in-offline-environments">
  Atualizações de marketplace falham em ambientes offline
</h3>

**Sintomas**: `git pull` do marketplace falha em segundo plano e Claude Code tenta repetidamente uma re-clonagem que não consegue ter sucesso.

**Causa**: Por padrão, quando um `git pull` falha, Claude Code tenta uma re-clonagem do zero. Em ambientes offline ou airgapped, re-clonar falha da mesma forma, e a restauração do cache anterior depois é melhor esforço. A atualização é executada em segundo plano após a inicialização, então não atrasa a inicialização, mas cada sessão repete as tentativas falhadas e cada operação git pode esperar o [timeout de 120 segundos](#git-operations-time-out).

**Solução**: Defina `CLAUDE_CODE_PLUGIN_KEEP_MARKETPLACE_ON_FAILURE=1` para pular a tentativa de re-clonagem e continuar usando o cache existente quando o pull falhar:

```bash theme={null}
export CLAUDE_CODE_PLUGIN_KEEP_MARKETPLACE_ON_FAILURE=1
```

Com esta variável definida, Claude Code retém o clone obsoleto do marketplace em falha de `git pull` e continua usando o último estado conhecido como bom. Para implantações totalmente offline onde o repositório nunca será alcançável, use [`CLAUDE_CODE_PLUGIN_SEED_DIR`](#pre-populate-plugins-for-containers) para pré-popular o diretório de plugins no tempo de construção em vez disso.

<h3 id="git-operations-time-out">
  Operações Git expiram
</h3>

**Sintomas**: Instalação de plugin ou atualizações de marketplace falham com um erro de timeout como "Git clone timed out after 120s" ou "Git pull timed out after 120s".

**Causa**: Claude Code usa um timeout de 120 segundos para todas as operações git, incluindo clonagem de repositórios de plugin e puxar atualizações de marketplace. Repositórios grandes ou conexões de rede lentas podem exceder este limite.

**Solução**: Aumente o timeout usando a variável de ambiente `CLAUDE_CODE_PLUGIN_GIT_TIMEOUT_MS`. O valor está em milissegundos:

```bash theme={null}
export CLAUDE_CODE_PLUGIN_GIT_TIMEOUT_MS=300000  # 5 minutos
```

<h3 id="plugins-with-relative-paths-fail-in-url-based-marketplaces">
  Plugins com caminhos relativos falham em marketplaces baseados em URL
</h3>

**Sintomas**: Adicionou um marketplace via URL (como `https://example.com/marketplace.json`), mas plugins com fontes de caminho relativo como `"./plugins/my-plugin"` falham ao instalar com erros "path not found".

**Causa**: Marketplaces baseados em URL apenas baixam o próprio arquivo `marketplace.json`. Eles não baixam arquivos de plugin do servidor. Caminhos relativos na entrada de marketplace referenciam arquivos no servidor remoto que não foram baixados.

**Soluções**:

* **Use fontes externas**: Altere entradas de plugin para usar fontes GitHub, npm ou URL git em vez de caminhos relativos:
  ```json theme={null}
  { "name": "my-plugin", "source": { "source": "github", "repo": "owner/repo" } }
  ```
* **Use um marketplace baseado em Git**: Hospede seu marketplace em um repositório Git e adicione-o com a URL git. Marketplaces baseados em Git clonam o repositório inteiro, tornando caminhos relativos funcionarem corretamente.

<h3 id="files-not-found-after-installation">
  Arquivos não encontrados após instalação
</h3>

**Sintomas**: Plugin instala mas referências a arquivos falham, especialmente arquivos fora do diretório do plugin

**Causa**: Plugins são copiados para um diretório de cache em vez de serem usados no local. Caminhos que referenciam arquivos fora do diretório do plugin (como `../shared-utils`) não funcionarão porque esses arquivos não são copiados.

**Soluções**: Veja [Plugin caching and file resolution](https://code.claude.com/docs/pt/plugins-reference#plugin-caching-and-file-resolution) para workarounds incluindo symlinks e reestruturação de diretório.

Para ferramentas de debugging adicionais e problemas comuns, veja [Debugging and development tools](https://code.claude.com/docs/pt/plugins-reference#debugging-and-development-tools).

<h2 id="see-also">
  Veja também
</h2>

* [Descobrir e instalar plugins pré-construídos](https://code.claude.com/docs/pt/discover-plugins) - Instalando plugins de marketplaces existentes
* [Plugins](https://code.claude.com/docs/pt/plugins) - Criando seus próprios plugins
* [Plugins reference](https://code.claude.com/docs/pt/plugins-reference) - Especificações técnicas completas e esquemas
* [Plugin settings](https://code.claude.com/docs/pt/settings#plugin-settings) - Opções de configuração de plugin
* [strictKnownMarketplaces reference](https://code.claude.com/docs/pt/settings#strictknownmarketplaces) - Restrições de marketplace gerenciado
