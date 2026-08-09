<!-- Fonte: https://code.claude.com/docs/pt/worktrees | Baixado em: 2026-08-06 | slug: worktrees -->

# Executar sessões paralelas com worktrees

> Isole sessões paralelas do Claude Code em worktrees git separadas para que as alterações não colidam. Abrange o sinalizador `--worktree`, isolamento de subagentes, `.worktreeinclude`, limpeza e hooks de VCS não-git.

Uma [git worktree](https://git-scm.com/docs/git-worktree) é um diretório de trabalho separado com seus próprios arquivos e branch, compartilhando o mesmo histórico de repositório e remoto que seu checkout principal. Executar cada sessão do Claude Code em sua própria worktree significa que edições em uma sessão nunca tocam arquivos em outra, para que você possa ter Claude construindo um recurso em um terminal enquanto corrige um bug em um segundo.

Esta página aborda isolamento de worktree na CLI. Tudo abaixo assume um repositório git. Para outros sistemas de controle de versão, consulte [Controle de versão não-git](#non-git-version-control). O [aplicativo desktop](https://code.claude.com/docs/pt/desktop#work-in-parallel-with-sessions) cria uma worktree para cada nova sessão automaticamente.

Worktrees são uma das várias maneiras de executar Claude em paralelo. Elas isolam edições de arquivo, enquanto [subagentes](https://code.claude.com/docs/pt/sub-agents) e [equipes de agentes](https://code.claude.com/docs/pt/agent-teams) coordenam o trabalho em si. Consulte [Executar agentes em paralelo](https://code.claude.com/docs/pt/agents) para comparar as abordagens, ou pule para [Isolar subagentes com worktrees](#isolate-subagents-with-worktrees) para usar worktrees e subagentes juntos.

<h2 id="start-claude-in-a-worktree">
  Inicie Claude em uma worktree
</h2>

Passe `--worktree` ou `-w` para criar uma worktree isolada e iniciar Claude nela. Por padrão, a worktree é criada em `.claude/worktrees/<value>/` na raiz do seu repositório, em um novo branch nomeado `worktree-<value>`:

```bash theme={null}
claude --worktree feature-auth
```

Para colocar worktrees em outro lugar, configure um hook [`WorktreeCreate`](#non-git-version-control). Execute o comando novamente com um nome diferente em outro terminal para iniciar uma segunda sessão isolada:

```bash theme={null}
claude --worktree bugfix-123
```

Se você omitir o nome, Claude gera um como `bright-running-fox`:

```bash theme={null}
claude --worktree
```

Você também pode pedir ao Claude para "trabalhar em uma worktree" durante uma sessão, e ele criará uma com a ferramenta [`EnterWorktree`](https://code.claude.com/docs/pt/tools-reference). Uma vez em uma worktree, Claude pode alternar diretamente para outra em `.claude/worktrees/` chamando `EnterWorktree` com o caminho de destino. A worktree anterior permanece no disco intacta.

Entrar em um caminho fora do diretório `.claude/worktrees/` do repositório solicita sua aprovação primeiro, porque move o diretório de trabalho da sessão, acesso de escrita e configuração do projeto, como `CLAUDE.md` e configurações para esse local. Uma regra de permissão [`EnterWorktree`](https://code.claude.com/docs/pt/permissions) ou escolher "não perguntar novamente" não suprime este prompt; apenas o modo `bypassPermissions` o ignora. Antes da v2.1.206, Claude podia entrar em qualquer caminho de worktree existente sem perguntar.

A partir da v2.1.198, entrar ou sair de uma worktree também realoca a transcrição da sessão para o armazenamento de projeto desse diretório, da mesma forma que [`/cd`](https://code.claude.com/docs/pt/commands) faz, então `/desktop` e `--resume` encontram a sessão lá depois. Worktrees criadas por um hook [`WorktreeCreate`](#non-git-version-control) são excluídas e mantêm a transcrição no diretório de inicialização.

Worktrees funcionam com [sandboxing](https://code.claude.com/docs/pt/sandboxing#filesystem-isolation) ativado: a sandbox permite escritas no diretório compartilhado `.git` do repositório principal para que comandos como `git commit` possam atualizar refs e o índice de dentro de uma worktree vinculada.

Antes de usar `--worktree` interativamente em um diretório pela primeira vez, aceite o diálogo de confiança do workspace executando `claude` uma vez nesse diretório. Se a confiança ainda não foi aceita, `--worktree` sai com um erro e solicita que você execute `claude` no diretório primeiro. Execuções não interativas com `-p` pulam a [verificação de confiança](https://code.claude.com/docs/pt/security), então `claude -p --worktree` prossegue sem ela.

Se Claude Code não conseguir entrar no diretório da worktree na inicialização, por exemplo porque um hook [`WorktreeCreate`](https://code.claude.com/docs/pt/hooks#worktreecreate) imprimiu algo diferente do diretório que criou, ou porque o diretório foi deletado após ser configurado, Claude Code imprime um erro nomeando o caminho e sai com código 1. Antes da v2.1.205, isso causava crash na sessão, e com `-p` travava por cerca de 30 segundos antes de sair com código 0.

Plugins instalados no [escopo do projeto](https://code.claude.com/docs/pt/plugins-reference#plugin-installation-scopes) do checkout principal também carregam em worktrees do mesmo repositório, então você não precisa reinstalá-los por worktree. Isso se aplica se você criar a worktree com `--worktree` ou com `git worktree add`. Requer Claude Code v2.1.200 ou posterior.

<Tip>
  Adicione `.claude/worktrees/` ao seu `.gitignore` para que o conteúdo da worktree não apareça como arquivos não rastreados no seu checkout principal.
</Tip>

<h3 id="choose-the-base-branch">
  Escolha o branch base
</h3>

Worktrees fazem branch a partir do branch padrão do seu repositório, `origin/HEAD`, para que começem de uma árvore limpa correspondendo ao remoto. Quando nada buscou o repositório nos últimos 24 horas, Claude Code atualiza `origin/HEAD` com uma busca do branch padrão, limitada a cinco segundos, e usa a ref localmente armazenada em cache se a busca falhar. Se nenhum remoto estiver configurado, ou `origin/HEAD` não estiver armazenado em cache localmente e não puder ser buscado, a worktree volta para seu `HEAD` local atual.

A atualização requer Claude Code v2.1.208 ou posterior; antes disso, uma worktree nova usava qualquer `origin/HEAD` que já estivesse armazenado em cache localmente.

Para sempre fazer branch a partir do `HEAD` local, defina `worktree.baseRef` como `"head"` em [configurações](https://code.claude.com/docs/pt/settings#worktree-settings). Definir `baseRef` como `"head"` faz com que novas worktrees carreguem seus commits não enviados e estado de branch de recurso, o que é útil ao isolar subagentes que precisam operar em trabalho em andamento. Quando a sessão é executada dentro de uma worktree vinculada, `"head"` resolve para o `HEAD` dessa worktree, não para o checkout principal. A configuração aceita apenas `"fresh"` ou `"head"`, não refs git arbitrárias:

```json theme={null}
{
  "worktree": {
    "baseRef": "head"
  }
}
```

Para fazer branch a partir de um pull request específico, passe o número do PR prefixado com `#`, ou uma URL completa de pull request do GitHub. Claude Code busca `pull/<number>/head` de `origin` e cria a worktree em `.claude/worktrees/pr-<number>`:

```bash theme={null}
claude --worktree "#1234"
```

Para controle total sobre como as worktrees são criadas, configure um hook [`WorktreeCreate`](https://code.claude.com/docs/pt/hooks#worktreecreate), que substitui completamente a lógica padrão de `git worktree`.

<h3 id="reuse-a-worktree-name">
  Reutilize um nome de worktree
</h3>

Reutilizar um nome de worktree cujo diretório já existe retoma essa worktree.

Uma worktree retomada redefine para a [base atual](#choose-the-base-branch) em vez de retomar em sua ponta antiga quando todos os seguintes se aplicam:

* Não tem alterações não confirmadas ou arquivos não rastreados.
* Ainda está no branch que Claude Code criou para ela.
* Nunca confirmou, ou seu pull request foi mesclado e seu branch remoto foi deletado.

Antes da v2.1.208, um nome reutilizado sempre retomava a worktree antiga em sua ponta antiga.

<h2 id="copy-gitignored-files-into-worktrees">
  Copie arquivos ignorados pelo git em worktrees
</h2>

Uma worktree é um checkout fresco, então arquivos não rastreados como `.env` ou `.env.local` do seu repositório principal não estão presentes. Para copiá-los automaticamente quando Claude cria uma worktree, adicione um arquivo `.worktreeinclude` à raiz do seu projeto.

O arquivo usa sintaxe `.gitignore`. Apenas arquivos que correspondem a um padrão e também são ignorados pelo git são copiados, então arquivos rastreados nunca são duplicados.

Este `.worktreeinclude` copia dois arquivos env e uma configuração de segredos em cada nova worktree:

```text .worktreeinclude theme={null}
.env
.env.local
config/secrets.json
```

Isso se aplica a worktrees criadas com `--worktree`, [worktrees de subagentes](#isolate-subagents-with-worktrees), e sessões paralelas no [aplicativo desktop](https://code.claude.com/docs/pt/desktop#work-in-parallel-with-sessions).

<h2 id="isolate-subagents-with-worktrees">
  Isole subagentes com worktrees
</h2>

Subagentes podem executar em suas próprias worktrees para que edições paralelas não entrem em conflito. Peça ao Claude para "usar worktrees para seus agentes", ou defina permanentemente em um [subagente personalizado](https://code.claude.com/docs/pt/sub-agents#supported-frontmatter-fields) adicionando `isolation: worktree` ao frontmatter. Cada subagente obtém uma worktree temporária que é removida automaticamente quando o subagente termina sem alterações.

As worktrees de subagentes usam a mesma [branch base](#choose-the-base-branch) que `--worktree`, portanto elas fazem branch da branch padrão do seu repositório, a menos que `worktree.baseRef` seja definido como `"head"`.

<h2 id="clean-up-worktrees">
  Limpe worktrees
</h2>

Quando você sai de uma sessão de worktree, a limpeza depende se você fez alterações:

* **Sem alterações não confirmadas, sem arquivos não rastreados e sem novos commits**: a worktree e seu branch são removidos automaticamente. Se a sessão tiver um [nome](https://code.claude.com/docs/pt/sessions#name-your-sessions), Claude solicita em vez disso para que você possa manter a worktree para depois
* **Alterações não confirmadas, arquivos não rastreados ou novos commits existem**: Claude solicita que você mantenha ou remova a worktree. Manter preserva o diretório e branch para que você possa retornar mais tarde. Remover exclui o diretório da worktree e seu branch, descartando todas as alterações não confirmadas, arquivos não rastreados e commits
* **Execuções não interativas**: worktrees criadas com `--worktree` junto com `-p` não são limpas automaticamente, pois não há prompt de saída. Remova-as com `git worktree remove`

Worktrees que Claude criou para subagentes e [sessões em segundo plano](https://code.claude.com/docs/pt/agent-view#how-file-edits-are-isolated) são removidas automaticamente uma vez que são mais antigas que sua configuração [`cleanupPeriodDays`](https://code.claude.com/docs/pt/settings#available-settings), desde que não tenham alterações não confirmadas, nenhum arquivo não rastreado e nenhum commit não enviado. Worktrees que você cria com `--worktree` nunca são removidas por esta varredura.

Enquanto um agente está em execução, Claude executa `git worktree lock` em sua worktree para que a limpeza simultânea não possa removê-la. O bloqueio é liberado quando o agente termina. Para limpar uma worktree que a varredura mantém, execute `git worktree remove`, adicionando `--force` se a worktree tiver alterações não confirmadas ou arquivos não rastreados.

No Windows, antes de remover uma worktree, Claude Code remove qualquer junção NTFS ou symlink de diretório em qualquer profundidade dentro dela como uma entrada de link, para que remover a worktree não exclua os arquivos para os quais um link aponta. Antes da v2.1.205, Claude Code removia apenas links de nível superior como entradas de link, e remover uma worktree com uma junção aninhada em um subdiretório poderia excluir o conteúdo do diretório para o qual o link apontava fora da worktree.

<h2 id="manage-worktrees-manually">
  Gerencie worktrees manualmente
</h2>

Para controle total sobre localização de worktree e configuração de branch, crie worktrees com Git diretamente. Isso é útil quando você precisa fazer checkout de um branch existente específico ou colocar a worktree fora do repositório.

Crie uma worktree em um novo branch:

```bash theme={null}
git worktree add ../project-feature-a -b feature-a
```

Crie uma worktree a partir de um branch existente:

```bash theme={null}
git worktree add ../project-bugfix bugfix-123
```

Inicie Claude na worktree:

```bash theme={null}
cd ../project-feature-a && claude
```

Liste suas worktrees:

```bash theme={null}
git worktree list
```

Remova uma quando terminar com ela:

```bash theme={null}
git worktree remove ../project-feature-a
```

Consulte a [documentação de git worktree](https://git-scm.com/docs/git-worktree) para a referência completa de comandos. Lembre-se de inicializar seu ambiente de desenvolvimento em cada nova worktree: instale dependências, configure ambientes virtuais, ou execute o que quer que a configuração do seu projeto exija.

<h2 id="non-git-version-control">
  Controle de versão não-git
</h2>

Isolamento de worktrees usa git por padrão. Para SVN, Perforce, Mercurial, ou outros sistemas, configure os hooks [`WorktreeCreate` e `WorktreeRemove`](https://code.claude.com/docs/pt/hooks#worktreecreate) para fornecer lógica de criação e limpeza personalizada. Como o hook substitui o comportamento padrão do git, [`.worktreeinclude`](#copy-gitignored-files-into-worktrees) não é processado quando você usa `--worktree`. Copie quaisquer arquivos de configuração local dentro do seu script de hook.

Este hook `WorktreeCreate` lê o nome da worktree de stdin, faz checkout de uma cópia de trabalho SVN fresca, e imprime o caminho do diretório para que Claude Code possa usá-lo como o diretório de trabalho da sessão:

```json theme={null}
{
  "hooks": {
    "WorktreeCreate": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "bash -c 'NAME=$(jq -r .name); DIR=\"$HOME/.claude/worktrees/$NAME\"; svn checkout https://svn.example.com/repo/trunk \"$DIR\" >&2 && echo \"$DIR\"'"
          }
        ]
      }
    ]
  }
}
```

Emparelhe-o com um hook `WorktreeRemove` para limpar quando a sessão terminar. Consulte a [referência de hooks](https://code.claude.com/docs/pt/hooks#worktreecreate) para o esquema de entrada e um exemplo de remoção.

<h2 id="see-also">
  Veja também
</h2>

Worktrees lidam com isolamento de arquivo. As páginas relacionadas abaixo cobrem delegação de trabalho para esses checkouts isolados e alternância entre as sessões que você cria:

* [Subagentes](https://code.claude.com/docs/pt/sub-agents): delegue trabalho para agentes isolados dentro de uma sessão
* [Equipes de agentes](https://code.claude.com/docs/pt/agent-teams): coordene múltiplas sessões do Claude automaticamente
* [Gerencie sessões](https://code.claude.com/docs/pt/sessions): nomeie, retome e alterne entre conversas
* [Sessões paralelas do desktop](https://code.claude.com/docs/pt/desktop#work-in-parallel-with-sessions): sessões apoiadas por worktree no aplicativo desktop
