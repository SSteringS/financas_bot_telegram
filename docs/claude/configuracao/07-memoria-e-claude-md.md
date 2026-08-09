<!-- Fonte: https://code.claude.com/docs/pt/memory | Baixado em: 2026-08-06 | slug: memory -->

# Como Claude se lembra do seu projeto

> Dê a Claude instruções persistentes com arquivos CLAUDE.md e deixe Claude acumular aprendizados automaticamente com memória automática.

Cada sessão do Claude Code começa com uma janela de contexto limpa. Dois mecanismos carregam conhecimento entre sessões:

* **Arquivos CLAUDE.md**: instruções que você escreve para dar a Claude contexto persistente
* **Memória automática**: notas que Claude escreve para si mesma com base em suas correções e preferências

Esta página cobre como:

* [Escrever e organizar arquivos CLAUDE.md](#claude-md-files)
* [Escopear regras para tipos de arquivo específicos](#organize-rules-with-claude/rules/) com `.claude/rules/`
* [Configurar memória automática](#auto-memory) para que Claude tome notas automaticamente
* [Solucionar problemas](#troubleshoot-memory-issues) quando as instruções não estão sendo seguidas

<h2 id="claude-md-vs-auto-memory">
  CLAUDE.md vs memória automática
</h2>

Claude Code tem dois sistemas de memória complementares. Ambos são carregados no início de cada conversa. Claude os trata como contexto, não como configuração imposta. Para bloquear uma ação independentemente do que Claude decidir, use um [hook PreToolUse](https://code.claude.com/docs/pt/hooks-guide) em vez disso. Quanto mais específicas e concisas forem suas instruções, mais consistentemente Claude as seguirá.

|                  | Arquivos CLAUDE.md                                                 | Memória automática                                                              |
| :--------------- | :----------------------------------------------------------------- | :------------------------------------------------------------------------------ |
| **Quem escreve** | Você                                                               | Claude                                                                          |
| **O que contém** | Instruções e regras                                                | Aprendizados e padrões                                                          |
| **Escopo**       | Projeto, usuário ou organização                                    | Por repositório, compartilhado entre worktrees                                  |
| **Carregado em** | Cada sessão                                                        | Cada sessão (primeiras 200 linhas ou 25KB)                                      |
| **Usar para**    | Padrões de codificação, fluxos de trabalho, arquitetura do projeto | Comandos de compilação, insights de depuração, preferências que Claude descobre |

Use arquivos CLAUDE.md quando quiser guiar o comportamento de Claude. A memória automática permite que Claude aprenda com suas correções sem esforço manual.

Subagents também podem manter sua própria memória automática. Veja [configuração de subagent](https://code.claude.com/docs/pt/sub-agents#enable-persistent-memory) para detalhes.

<h2 id="claude-md-files">
  Arquivos CLAUDE.md
</h2>

Arquivos CLAUDE.md são arquivos markdown que dão a Claude instruções persistentes para um projeto, seu fluxo de trabalho pessoal ou toda a sua organização. Você escreve esses arquivos em texto simples; Claude os lê no início de cada sessão.

<h3 id="when-to-add-to-claude-md">
  Quando adicionar a CLAUDE.md
</h3>

Trate CLAUDE.md como o lugar onde você escreve o que teria que re-explicar. Adicione a ele quando:

* Claude comete o mesmo erro uma segunda vez
* Uma revisão de código encontra algo que Claude deveria saber sobre esta base de código
* Você digita a mesma correção ou esclarecimento no chat que digitou na sessão anterior
* Um novo colega de equipe precisaria do mesmo contexto para ser produtivo

Mantenha-o com fatos que Claude deve manter em cada sessão: comandos de compilação, convenções, layout do projeto, regras "sempre faça X". Se uma entrada é um procedimento de múltiplas etapas ou só importa para uma parte da base de código, mova-a para uma [skill](https://code.claude.com/docs/pt/skills) ou uma [regra com escopo de caminho](#organize-rules-with-claude/rules/) em vez disso. A [visão geral da extensão](https://code.claude.com/docs/pt/features-overview#build-your-setup-over-time) cobre quando usar cada mecanismo.

<h3 id="choose-where-to-put-claude-md-files">
  Escolha onde colocar arquivos CLAUDE.md
</h3>

Arquivos CLAUDE.md podem estar em vários locais, cada um com um escopo diferente. A tabela abaixo lista-os em ordem de carregamento, do escopo mais amplo para o mais específico, então uma instrução de projeto aparece em contexto após uma instrução de usuário.

| Escopo                    | Localização                                                                                                                                                           | Propósito                                                             | Exemplos de caso de uso                                                               | Compartilhado com                        |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------- | ---------------------------------------- |
| **Política gerenciada**   | • macOS: `/Library/Application Support/ClaudeCode/CLAUDE.md`<br />• Linux e WSL: `/etc/claude-code/CLAUDE.md`<br />• Windows: `C:\Program Files\ClaudeCode\CLAUDE.md` | Instruções em toda a organização gerenciadas por TI/DevOps            | Padrões de codificação da empresa, políticas de segurança, requisitos de conformidade | Todos os usuários da organização         |
| **Instruções do usuário** | `~/.claude/CLAUDE.md`                                                                                                                                                 | Preferências pessoais para todos os projetos                          | Preferências de estilo de código, atalhos de ferramentas pessoais                     | Apenas você (todos os projetos)          |
| **Instruções do projeto** | `./CLAUDE.md` ou `./.claude/CLAUDE.md`                                                                                                                                | Instruções compartilhadas pela equipe para o projeto                  | Arquitetura do projeto, padrões de codificação, fluxos de trabalho comuns             | Membros da equipe via controle de versão |
| **Instruções locais**     | `./CLAUDE.local.md`                                                                                                                                                   | Preferências pessoais específicas do projeto; adicione a `.gitignore` | Suas URLs de sandbox, dados de teste preferidos                                       | Apenas você (projeto atual)              |

Arquivos CLAUDE.md e CLAUDE.local.md no diretório acima do diretório de trabalho são carregados completamente no lançamento. Arquivos em subdiretórios são carregados sob demanda quando Claude lê arquivos nesses diretórios. Veja [Como arquivos CLAUDE.md são carregados](#how-claude-md-files-load) para a ordem de resolução completa.

Para projetos grandes, você pode dividir instruções em arquivos específicos de tópicos usando [regras de projeto](#organize-rules-with-claude/rules/). As regras permitem que você escope instruções para tipos de arquivo específicos ou subdiretórios.

<h3 id="set-up-a-project-claude-md">
  Configure um CLAUDE.md de projeto
</h3>

Um CLAUDE.md de projeto pode ser armazenado em `./CLAUDE.md` ou `./.claude/CLAUDE.md`. Crie este arquivo e adicione instruções que se apliquem a qualquer pessoa trabalhando no projeto: comandos de compilação e teste, padrões de codificação, decisões arquitetônicas, convenções de nomenclatura e fluxos de trabalho comuns. Essas instruções são compartilhadas com sua equipe através do controle de versão, então foque em padrões de nível de projeto em vez de preferências pessoais.

<Tip>
  Execute `/init` para gerar um CLAUDE.md inicial automaticamente. Claude analisa sua base de código e cria um arquivo com comandos de compilação, instruções de teste e convenções de projeto que descobre. Se um CLAUDE.md já existe, `/init` sugere melhorias em vez de sobrescrever. Refine a partir daí com instruções que Claude não descobriria por conta própria.

  Defina `CLAUDE_CODE_NEW_INIT=1` para ativar um fluxo interativo de múltiplas fases. `/init` pergunta quais artefatos configurar: arquivos CLAUDE.md, skills e hooks. Em seguida, explora sua base de código com um subagent, preenche lacunas por meio de perguntas de acompanhamento e apresenta uma proposta revisável antes de escrever qualquer arquivo.
</Tip>

<h3 id="write-effective-instructions">
  Escreva instruções eficazes
</h3>

Arquivos CLAUDE.md são carregados na janela de contexto no início de cada sessão, consumindo tokens junto com sua conversa. A [visualização da janela de contexto](https://code.claude.com/docs/pt/context-window) mostra onde CLAUDE.md é carregado em relação ao resto do contexto de inicialização. Como são contexto em vez de configuração imposta, como você escreve as instruções afeta o quão confiável Claude as segue. Instruções específicas, concisas e bem estruturadas funcionam melhor.

**Tamanho**: alvo de menos de 200 linhas por arquivo CLAUDE.md. Arquivos mais longos consomem mais contexto e reduzem a aderência. Se suas instruções estão crescendo muito, use [regras com escopo de caminho](#path-specific-rules) para que as instruções sejam carregadas apenas quando Claude trabalha com arquivos correspondentes. Você também pode dividir conteúdo em [importações](#import-additional-files) para organização, embora arquivos importados ainda sejam carregados e entrem na janela de contexto no lançamento.

**Estrutura**: use cabeçalhos markdown e bullets para agrupar instruções relacionadas. Claude escaneia a estrutura da mesma forma que os leitores fazem: seções organizadas são mais fáceis de seguir do que parágrafos densos.

**Especificidade**: escreva instruções que sejam concretas o suficiente para verificar. Por exemplo:

* "Use indentação de 2 espaços" em vez de "Formate o código adequadamente"
* "Execute `npm test` antes de fazer commit" em vez de "Teste suas alterações"
* "Manipuladores de API vivem em `src/api/handlers/`" em vez de "Mantenha os arquivos organizados"

**Consistência**: se duas regras se contradizem, Claude pode escolher uma arbitrariamente. Revise seus arquivos CLAUDE.md, arquivos CLAUDE.md aninhados em subdiretórios e [`.claude/rules/`](#organize-rules-with-claude/rules/) periodicamente para remover instruções desatualizadas ou conflitantes. Em monorepos, use [`claudeMdExcludes`](#exclude-specific-claude-md-files) para pular arquivos CLAUDE.md de outras equipes que não são relevantes para seu trabalho.

<h3 id="import-additional-files">
  Importe arquivos adicionais
</h3>

Arquivos CLAUDE.md podem importar arquivos adicionais usando a sintaxe `@path/to/import`. Arquivos importados são expandidos e carregados em contexto no lançamento junto com o CLAUDE.md que os referencia.

Caminhos relativos e absolutos são permitidos. Caminhos relativos são resolvidos em relação ao arquivo contendo a importação, não ao diretório de trabalho. Arquivos importados podem importar recursivamente outros arquivos, com uma profundidade máxima de quatro saltos.

A análise de importação ignora spans de código Markdown e blocos de código cercados. Para mencionar um caminho em seu CLAUDE.md sem importá-lo, envolva-o em backticks: escrever `` `@README` `` mantém o texto literal, enquanto `@README` fora de backticks importa o arquivo.

Para trazer um README, package.json e um guia de fluxo de trabalho, referencie-os com a sintaxe `@` em qualquer lugar do seu CLAUDE.md:

```text theme={null}
Veja @README para visão geral do projeto e @package.json para comandos npm disponíveis para este projeto.

# Instruções Adicionais
- fluxo de trabalho git @docs/git-instructions.md
```

Para preferências pessoais por projeto que não devem ser verificadas no controle de versão, crie um `CLAUDE.local.md` na raiz do projeto. Ele é carregado junto com `CLAUDE.md` e é tratado da mesma forma. Adicione `CLAUDE.local.md` ao seu `.gitignore` para que não seja confirmado; executar `/init` e escolher a opção pessoal faz isso para você.

Se você trabalha em múltiplos git worktrees do mesmo repositório, um `CLAUDE.local.md` ignorado pelo git só existe no worktree onde você o criou. Para compartilhar instruções pessoais entre worktrees, importe um arquivo do seu diretório home em vez disso:

```text theme={null}
# Preferências Individuais
- @~/.claude/my-project-instructions.md
```

<Warning>
  A primeira vez que Claude Code encontra importações externas em um projeto, mostra um diálogo de aprovação listando os arquivos. Se você recusar, as importações permanecem desabilitadas e o diálogo não aparece novamente.
</Warning>

Para uma abordagem mais estruturada para organizar instruções, veja [`.claude/rules/`](#organize-rules-with-claude/rules/).

<h3 id="agents-md">
  AGENTS.md
</h3>

Claude Code lê `CLAUDE.md`, não `AGENTS.md`. Se seu repositório já usa `AGENTS.md` para outros agentes de codificação, crie um `CLAUDE.md` que o importe para que ambas as ferramentas leiam as mesmas instruções sem duplicá-las. Você também pode adicionar instruções específicas do Claude Code abaixo da importação. Claude carrega o arquivo importado no início da sessão, depois anexa o resto:

```markdown CLAUDE.md theme={null}
@AGENTS.md

## Claude Code

Use plan mode para alterações em `src/billing/`.
```

Um symlink também funciona se você não precisar adicionar conteúdo específico do Claude Code:

```bash theme={null}
ln -s AGENTS.md CLAUDE.md
```

No Windows, criar um symlink requer privilégios de Administrador ou Modo de Desenvolvedor, então use a importação `@AGENTS.md` em vez disso.

Executar [`/init`](https://code.claude.com/docs/pt/commands) em um repositório que já tem um `AGENTS.md` o lê e incorpora as partes relevantes no `CLAUDE.md` gerado. Ele também lê outras configurações de ferramentas como `.cursorrules`, `.devin/rules/` e `.windsurfrules`.

<h3 id="how-claude-md-files-load">
  Como arquivos CLAUDE.md são carregados
</h3>

Claude Code lê arquivos CLAUDE.md caminhando para cima na árvore de diretórios a partir do seu diretório de trabalho atual, verificando cada diretório ao longo do caminho para arquivos `CLAUDE.md` e `CLAUDE.local.md`. Isso significa que se você executar Claude Code em `foo/bar/`, ele carrega instruções de `foo/bar/CLAUDE.md`, `foo/CLAUDE.md` e qualquer arquivo `CLAUDE.local.md` ao lado deles.

Todos os arquivos descobertos são concatenados em contexto em vez de se sobreporem. Dentro da árvore de diretórios, o conteúdo é ordenado da raiz do sistema de arquivos até seu diretório de trabalho. Para o exemplo `foo/bar/`, `foo/CLAUDE.md` aparece em contexto antes de `foo/bar/CLAUDE.md`, então as instruções mais próximas de onde você lançou Claude são lidas por último. Dentro de cada diretório, `CLAUDE.local.md` é anexado após `CLAUDE.md`, então suas notas pessoais são a última coisa que Claude lê naquele nível.

Claude também descobre arquivos `CLAUDE.md` e `CLAUDE.local.md` em subdiretórios sob seu diretório de trabalho atual. Em vez de carregá-los no lançamento, eles são incluídos quando Claude lê arquivos nesses subdiretórios.

Se você trabalha em um grande monorepo onde arquivos CLAUDE.md de outras equipes são capturados, use [`claudeMdExcludes`](#exclude-specific-claude-md-files) para pular. Para o layout completo de arquivos CLAUDE.md de raiz e por diretório e regras, veja [Monorepos e repositórios grandes](https://code.claude.com/docs/pt/large-codebases).

Comentários HTML em nível de bloco (`<!-- notas do mantenedor -->`) em arquivos CLAUDE.md são removidos antes do conteúdo ser injetado no contexto de Claude. Use-os para deixar notas para mantenedores humanos sem gastar tokens de contexto neles. Comentários dentro de blocos de código são preservados. Quando você abre um arquivo CLAUDE.md diretamente com a ferramenta Read, os comentários permanecem visíveis.

<h4 id="load-from-additional-directories">
  Carregue de diretórios adicionais
</h4>

A flag `--add-dir` dá a Claude acesso a diretórios adicionais fora do seu diretório de trabalho principal. Por padrão, arquivos CLAUDE.md desses diretórios não são carregados.

Para também carregar arquivos de memória de diretórios adicionais, defina a variável de ambiente `CLAUDE_CODE_ADDITIONAL_DIRECTORIES_CLAUDE_MD`:

```bash theme={null}
CLAUDE_CODE_ADDITIONAL_DIRECTORIES_CLAUDE_MD=1 claude --add-dir ../shared-config
```

Isso carrega `CLAUDE.md`, `.claude/CLAUDE.md`, `.claude/rules/*.md` e `CLAUDE.local.md` do diretório adicional. `CLAUDE.local.md` é ignorado se você excluir `local` de [`--setting-sources`](https://code.claude.com/docs/pt/cli-reference).

<h3 id="organize-rules-with-claude/rules/">
  Organize regras com `.claude/rules/`
</h3>

Para projetos maiores, você pode organizar instruções em múltiplos arquivos usando o diretório `.claude/rules/`. Isso mantém as instruções modulares e mais fáceis para as equipes manterem. As regras também podem ser [escopadas para caminhos de arquivo específicos](#path-specific-rules), então elas só são carregadas em contexto quando Claude trabalha com arquivos correspondentes, reduzindo ruído e economizando espaço de contexto.

<Note>
  As regras são carregadas em contexto a cada sessão ou quando arquivos correspondentes são abertos. Para instruções específicas de tarefa que não precisam estar em contexto o tempo todo, use [skills](https://code.claude.com/docs/pt/skills) em vez disso, que só são carregadas quando você as invoca ou quando Claude determina que são relevantes para seu prompt.
</Note>

<h4 id="set-up-rules">
  Configure regras
</h4>

Coloque arquivos markdown no diretório `.claude/rules/` do seu projeto. Cada arquivo deve cobrir um tópico, com um nome de arquivo descritivo como `testing.md` ou `api-design.md`. Todos os arquivos `.md` são descobertos recursivamente, então você pode organizar regras em subdiretórios como `frontend/` ou `backend/`:

```text theme={null}
seu-projeto/
├── .claude/
│   ├── CLAUDE.md           # Instruções principais do projeto
│   └── rules/
│       ├── code-style.md   # Diretrizes de estilo de código
│       ├── testing.md      # Convenções de teste
│       └── security.md     # Requisitos de segurança
```

Regras sem [frontmatter `paths`](#path-specific-rules) são carregadas no lançamento com a mesma prioridade que `.claude/CLAUDE.md`.

<h4 id="path-specific-rules">
  Regras específicas de caminho
</h4>

As regras podem ser escopadas para arquivos específicos usando frontmatter YAML com o campo `paths`. Essas regras condicionais só se aplicam quando Claude está trabalhando com arquivos correspondentes aos padrões especificados.

```markdown theme={null}
---
paths:
  - "src/api/**/*.ts"
---

# Regras de Desenvolvimento de API

- Todos os endpoints de API devem incluir validação de entrada
- Use o formato de resposta de erro padrão
- Inclua comentários de documentação OpenAPI
```

Regras sem um campo `paths` são carregadas incondicionalmente e se aplicam a todos os arquivos. Regras com escopo de caminho são acionadas quando Claude lê arquivos correspondentes ao padrão, não em cada uso de ferramenta. A partir da v2.1.198, a correspondência também funciona quando Claude alcança um arquivo através de um caminho vinculado simbolicamente para o diretório do projeto, por exemplo em um checkout vinculado simbolicamente.

Use padrões glob no campo `paths` para corresponder arquivos por extensão, diretório ou qualquer combinação:

| Padrão                 | Corresponde                                        |
| ---------------------- | -------------------------------------------------- |
| `**/*.ts`              | Todos os arquivos TypeScript em qualquer diretório |
| `src/**/*`             | Todos os arquivos sob o diretório `src/`           |
| `*.md`                 | Arquivos Markdown na raiz do projeto               |
| `src/components/*.tsx` | Componentes React em um diretório específico       |

Você pode especificar múltiplos padrões e usar expansão de chaves para corresponder múltiplas extensões em um padrão:

```markdown theme={null}
---
paths:
  - "src/**/*.{ts,tsx}"
  - "lib/**/*.ts"
  - "tests/**/*.test.ts"
---
```

Sintaxe glob trata `[` como o início de uma expressão de colchete como `[abc]`. Um padrão com um `[` que não pode ser lido como uma expressão de colchete, como `photos [2024/**`, é inválido: ele não corresponde a nada, e os outros padrões da regra continuam funcionando. Para corresponder um `[` literal em um nome de arquivo, escape-o como `photos \[2024/**`. Antes da v2.1.207, um padrão inválido fazia a ferramenta Read falhar para cada arquivo em que a regra era avaliada, em vez de não corresponder a nada.

<h4 id="share-rules-across-projects-with-symlinks">
  Compartilhe regras entre projetos com symlinks
</h4>

O diretório `.claude/rules/` suporta symlinks, então você pode manter um conjunto compartilhado de regras e vinculá-las em múltiplos projetos. Symlinks são resolvidos e carregados normalmente, e symlinks circulares são detectados e tratados graciosamente.

Este exemplo vincula tanto um diretório compartilhado quanto um arquivo individual:

```bash theme={null}
ln -s ~/shared-claude-rules .claude/rules/shared
ln -s ~/company-standards/security.md .claude/rules/security.md
```

<h4 id="user-level-rules">
  Regras de nível de usuário
</h4>

Regras pessoais em `~/.claude/rules/` se aplicam a cada projeto na sua máquina. Use-as para preferências que não são específicas do projeto:

```text theme={null}
~/.claude/rules/
├── preferences.md    # Suas preferências pessoais de codificação
└── workflows.md      # Seus fluxos de trabalho preferidos
```

Regras de nível de usuário são carregadas antes das regras de projeto, dando às regras de projeto prioridade mais alta.

<h3 id="manage-claude-md-for-large-teams">
  Gerencie CLAUDE.md para grandes equipes
</h3>

Para organizações implantando Claude Code em equipes, você pode centralizar instruções e controlar quais arquivos CLAUDE.md são carregados.

<h4 id="deploy-organization-wide-claude-md">
  Implante CLAUDE.md em toda a organização
</h4>

As organizações podem implantar um CLAUDE.md gerenciado centralmente que se aplica a todos os usuários em uma máquina. Este arquivo não pode ser excluído por configurações individuais.

<Steps>
  <Step title="Crie o arquivo no local da política gerenciada">
    * macOS: `/Library/Application Support/ClaudeCode/CLAUDE.md`
    * Linux e WSL: `/etc/claude-code/CLAUDE.md`
    * Windows: `C:\Program Files\ClaudeCode\CLAUDE.md`
  </Step>

  <Step title="Implante com seu sistema de gerenciamento de configuração">
    Use MDM, Group Policy, Ansible ou ferramentas similares para distribuir o arquivo entre máquinas de desenvolvedores. Veja [configurações gerenciadas](https://code.claude.com/docs/pt/permissions#managed-settings) para outras opções de configuração em toda a organização.
  </Step>
</Steps>

A chave `claudeMd` permite que você coloque conteúdo CLAUDE.md gerenciado diretamente dentro de `managed-settings.json` em vez de implantar um arquivo separado.

**Escopo**: cada sessão de Claude Code na máquina, em cada repositório. Para orientação específica do repositório, confirme um CLAUDE.md de projeto em vez disso.

**Precedência**: igual a um arquivo CLAUDE.md gerenciado. Carrega antes de CLAUDE.md de usuário e projeto.

**Onde é honrado**: apenas configurações gerenciadas e de política. Definir `claudeMd` em configurações de usuário, projeto ou local não tem efeito.

O exemplo abaixo adiciona instruções comportamentais diretamente em um arquivo de configurações gerenciadas:

```json theme={null}
{
  "claudeMd": "Always run `make lint` before committing.\nNever push directly to main."
}
```

Um CLAUDE.md gerenciado e [configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files) servem a propósitos diferentes. Use configurações para imposição técnica e CLAUDE.md para orientação comportamental:

| Preocupação                                                       | Configure em                                                       |
| :---------------------------------------------------------------- | :----------------------------------------------------------------- |
| Bloqueie ferramentas, comandos ou caminhos de arquivo específicos | Configurações gerenciadas: `permissions.deny`                      |
| Imponha isolamento de sandbox                                     | Configurações gerenciadas: `sandbox.enabled`                       |
| Variáveis de ambiente e roteamento de provedor de API             | Configurações gerenciadas: `env`                                   |
| Método de autenticação e bloqueio de organização                  | Configurações gerenciadas: `forceLoginMethod`, `forceLoginOrgUUID` |
| Diretrizes de estilo de código e qualidade                        | CLAUDE.md gerenciado                                               |
| Lembretes de manipulação de dados e conformidade                  | CLAUDE.md gerenciado                                               |
| Instruções comportamentais para Claude                            | CLAUDE.md gerenciado                                               |

Regras de configurações são impostas pelo cliente independentemente do que Claude decide fazer. Instruções de CLAUDE.md moldam o comportamento de Claude, mas não são uma camada de imposição rígida.

<h4 id="exclude-specific-claude-md-files">
  Exclua arquivos CLAUDE.md específicos
</h4>

Em grandes monorepos, arquivos CLAUDE.md ancestrais podem conter instruções que não são relevantes para seu trabalho. A configuração `claudeMdExcludes` permite que você pule arquivos específicos por caminho ou padrão glob.

Este exemplo exclui um CLAUDE.md de nível superior e um diretório de regras de uma pasta pai. Adicione-o a `.claude/settings.local.json` para que a exclusão permaneça local à sua máquina:

```json theme={null}
{
  "claudeMdExcludes": [
    "**/monorepo/CLAUDE.md",
    "/home/user/monorepo/other-team/.claude/rules/**"
  ]
}
```

Padrões são correspondidos contra caminhos de arquivo absolutos usando sintaxe glob. Você pode configurar `claudeMdExcludes` em qualquer [camada de configurações](https://code.claude.com/docs/pt/settings#settings-files): usuário, projeto, local ou política gerenciada. Arrays são mesclados entre camadas.

Arquivos CLAUDE.md de política gerenciada não podem ser excluídos. Isso garante que as instruções em toda a organização sempre se apliquem independentemente das configurações individuais.

<h2 id="auto-memory">
  Memória automática
</h2>

A memória automática permite que Claude acumule conhecimento entre sessões sem você escrever nada. Claude salva notas para si mesma enquanto trabalha: comandos de compilação, insights de depuração, notas de arquitetura, preferências de estilo de código e hábitos de fluxo de trabalho. Claude não salva algo a cada sessão. Ela decide o que vale a pena lembrar com base em se a informação seria útil em uma conversa futura.

<h3 id="enable-or-disable-auto-memory">
  Ative ou desative a memória automática
</h3>

A memória automática está ativada por padrão. Para alterná-la, abra `/memory` em uma sessão e use o toggle de memória automática, ou defina `autoMemoryEnabled` nas configurações do seu projeto:

```json theme={null}
{
  "autoMemoryEnabled": false
}
```

Para desabilitar a memória automática via variável de ambiente, defina `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1`.

<h3 id="storage-location">
  Local de armazenamento
</h3>

Cada projeto obtém seu próprio diretório de memória em `~/.claude/projects/<project>/memory/`. O caminho `<project>` é derivado do repositório git, então todos os worktrees e subdiretórios dentro do mesmo repositório compartilham um diretório de memória automática. Fora de um repositório git, a raiz do projeto é usada em vez disso.

Para armazenar memória automática em um local diferente, defina `autoMemoryDirectory` em seu `settings.json`. Ele é lido de qualquer [escopo de configurações](https://code.claude.com/docs/pt/settings#settings-precedence): usuário, projeto, local, política, ou `--settings`.

```json theme={null}
{
  "autoMemoryDirectory": "~/my-custom-memory-dir"
}
```

O valor deve ser um caminho absoluto ou começar com `~/`. Quando definido no `.claude/settings.json` ou `.claude/settings.local.json` de um projeto, o valor é respeitado apenas após você aceitar o diálogo de confiança do workspace para essa pasta, o mesmo gate que governa hooks.

O diretório contém um ponto de entrada `MEMORY.md` e arquivos de tópico opcionais:

```text theme={null}
~/.claude/projects/<project>/memory/
├── MEMORY.md          # Índice conciso, carregado em cada sessão
├── debugging.md       # Notas detalhadas sobre padrões de depuração
├── api-conventions.md # Decisões de design de API
└── ...                # Qualquer outro arquivo de tópico que Claude cria
```

`MEMORY.md` atua como um índice do diretório de memória. Claude lê e escreve arquivos neste diretório ao longo de sua sessão, usando `MEMORY.md` para acompanhar o que está armazenado onde.

A memória automática é local da máquina. Todos os worktrees e subdiretórios dentro do mesmo repositório git compartilham um diretório de memória automática. Os arquivos não são compartilhados entre máquinas ou ambientes em nuvem.

<h3 id="how-it-works">
  Como funciona
</h3>

As primeiras 200 linhas de `MEMORY.md`, ou os primeiros 25KB, o que vier primeiro, são carregados no início de cada conversa. Conteúdo além desse limite não é carregado no início da sessão. Claude mantém `MEMORY.md` conciso movendo notas detalhadas para arquivos de tópico separados.

Este limite se aplica apenas a `MEMORY.md`. Arquivos CLAUDE.md são carregados completamente independentemente do comprimento, embora arquivos mais curtos produzam melhor aderência.

Arquivos de tópico como `debugging.md` ou `patterns.md` não são carregados na inicialização. Claude os lê sob demanda usando suas ferramentas de arquivo padrão quando precisa da informação.

Claude lê e escreve arquivos de memória durante sua sessão. Quando você vê "Writing memory" ou "Recalled memory" na interface do Claude Code, Claude está ativamente atualizando ou lendo de `~/.claude/projects/<project>/memory/`.

<h3 id="audit-and-edit-your-memory">
  Audite e edite sua memória
</h3>

Arquivos de memória automática são markdown simples que você pode editar ou deletar a qualquer momento. Execute [`/memory`](#view-and-edit-with-%2Fmemory) para navegar e abrir arquivos de memória de dentro de uma sessão.

<h2 id="view-and-edit-with-/memory">
  Visualize e edite com `/memory`
</h2>

O comando `/memory` lista todos os arquivos CLAUDE.md, CLAUDE.local.md e rules carregados em sua sessão atual, permite que você alterne a memória automática ativada ou desativada, e fornece um link para abrir a pasta de memória automática. Selecione qualquer arquivo para abri-lo no seu editor.

Quando você pede a Claude para lembrar algo, como "sempre use pnpm, não npm" ou "lembre-se de que os testes de API requerem uma instância local de Redis," Claude salva em memória automática. Para adicionar instruções a CLAUDE.md em vez disso, peça a Claude diretamente, como "adicione isto a CLAUDE.md," ou edite o arquivo você mesmo via `/memory`.

<h2 id="troubleshoot-memory-issues">
  Solucione problemas de memória
</h2>

Estes são os problemas mais comuns com CLAUDE.md e memória automática, junto com passos para depurá-los.

<h3 id="claude-isn’t-following-my-claude-md">
  Claude não está seguindo meu CLAUDE.md
</h3>

O conteúdo de CLAUDE.md é entregue como uma mensagem de usuário após o prompt do sistema, não como parte do próprio prompt do sistema. Claude o lê e tenta segui-lo, mas não há garantia de conformidade estrita, especialmente para instruções vagas ou conflitantes.

Para depurar:

* Execute `/memory` para verificar se seus arquivos CLAUDE.md e CLAUDE.local.md estão sendo carregados. Se um arquivo não estiver listado, Claude não pode vê-lo.
* Verifique se o CLAUDE.md relevante está em um local que é carregado para sua sessão (veja [Escolha onde colocar arquivos CLAUDE.md](#choose-where-to-put-claude-md-files)).
* Torne as instruções mais específicas. "Use indentação de 2 espaços" funciona melhor do que "formate o código adequadamente."
* Procure por instruções conflitantes entre arquivos CLAUDE.md. Se dois arquivos dão orientação diferente para o mesmo comportamento, Claude pode escolher um arbitrariamente.

Se a instrução é algo que deve ser executado em um ponto específico, como antes de cada commit ou após cada edição de arquivo, escreva-a como um [hook](https://code.claude.com/docs/pt/hooks-guide) em vez disso. Hooks são executados como comandos shell em eventos de ciclo de vida fixos e se aplicam independentemente do que Claude decidir fazer.

Para instruções que você quer no nível do prompt do sistema, use [`--append-system-prompt`](https://code.claude.com/docs/pt/cli-reference#system-prompt-flags). Isso deve ser passado a cada invocação, então é mais adequado para scripts e automação do que para uso interativo.

<Tip>
  Use o hook [`InstructionsLoaded`](https://code.claude.com/docs/pt/hooks#instructionsloaded) para registrar exatamente quais arquivos de instrução são carregados, quando são carregados e por quê. Isso é útil para depurar regras específicas de caminho ou arquivos carregados preguiçosamente em subdiretórios.
</Tip>

<h3 id="i-don’t-know-what-auto-memory-saved">
  Não sei o que a memória automática salvou
</h3>

Execute `/memory` e selecione a pasta de memória automática para navegar o que Claude salvou. Tudo é markdown simples que você pode ler, editar ou deletar.

<h3 id="my-claude-md-is-too-large">
  Meu CLAUDE.md é muito grande
</h3>

Arquivos com mais de 200 linhas consomem mais contexto e podem reduzir a aderência. Use [regras com escopo de caminho](#path-specific-rules) para carregar instruções apenas quando Claude trabalha com arquivos correspondentes, ou reduza conteúdo que não é necessário em cada sessão. Dividir em [importações `@path`](#import-additional-files) ajuda na organização, mas não reduz contexto, já que arquivos importados são carregados no lançamento.

O checkup [`/doctor`](https://code.claude.com/docs/pt/commands#all-commands) propõe cortes para um CLAUDE.md verificado: ele corta conteúdo que Claude pode derivar da base de código, como layouts de diretório, listas de dependências e visões gerais de arquitetura, e mantém armadilhas, justificativa e convenções que diferem dos padrões de ferramentas. A verificação de corte requer Claude Code v2.1.206 ou posterior.

<h3 id="instructions-seem-lost-after-/compact">
  Instruções parecem perdidas após `/compact`
</h3>

CLAUDE.md de raiz de projeto sobrevive à compactação: após `/compact`, Claude relê do disco e reinjecta no contexto. Arquivos CLAUDE.md aninhados em subdiretórios não são reinjetados automaticamente; eles recarregam na próxima vez que Claude lê um arquivo naquele subdiretório.

Se uma instrução desapareceu após compactação, ela foi dada apenas em conversa ou vive em um CLAUDE.md aninhado que ainda não recarregou. Adicione instruções apenas de conversa a CLAUDE.md para torná-las persistir. Veja [O que sobrevive à compactação](https://code.claude.com/docs/pt/context-window#what-survives-compaction) para o detalhamento completo.

Veja [Escreva instruções eficazes](#write-effective-instructions) para orientação sobre tamanho, estrutura e especificidade.

<h2 id="related-resources">
  Recursos relacionados
</h2>

* [Debug sua configuração](https://code.claude.com/docs/pt/debug-your-config): diagnostique por que CLAUDE.md ou configurações não estão tendo efeito
* [Skills](https://code.claude.com/docs/pt/skills): empacote fluxos de trabalho repetíveis que carregam sob demanda
* [Settings](https://code.claude.com/docs/pt/settings): configure o comportamento do Claude Code com arquivos de configurações
* [Memória de subagent](https://code.claude.com/docs/pt/sub-agents#enable-persistent-memory): deixe subagents manter sua própria memória automática
