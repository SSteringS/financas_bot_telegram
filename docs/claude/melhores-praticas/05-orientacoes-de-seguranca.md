<!-- Fonte: https://code.claude.com/docs/pt/security-guidance | Baixado em: 2026-08-06 | slug: security-guidance -->

# Detectar problemas de segurança enquanto Claude escreve código

> Instale o plugin security-guidance para que Claude revise suas próprias alterações de código em busca de vulnerabilidades e as corrija na mesma sessão.

O plugin security guidance faz Claude revisar suas próprias alterações de código em busca de vulnerabilidades comuns enquanto trabalha e corrigir o que encontra na mesma sessão. O plugin detecta problemas como injeção, desserialização insegura e APIs DOM inseguras antes do código chegar a um pull request, reduzindo quanto da revisão de segurança cai para revisores humanos a jusante.

Uma vez instalado, o plugin é executado automaticamente. Não há nada para invocar e nenhum comando separado para lembrar.

O plugin é o companheiro em sessão do [Code Review](https://code.claude.com/docs/pt/code-review), que é executado em pull requests. Este plugin reduz o que chega ao PR. Code Review detecta o que faz. Para saber como o plugin se integra com revisão sob demanda e varredura de CI, consulte [Como isso se encaixa com outras ferramentas de segurança](#how-this-fits-with-other-security-tools).

<h2 id="prerequisites">
  Pré-requisitos
</h2>

* Claude Code CLI versão 2.1.144 ou posterior
* Python 3.8 ou posterior no seu `PATH`. O plugin tenta `python3`, `python` e `py -3` nessa ordem
* Um repositório git para o diretório em que você trabalha. As revisões de fim de turno e commit fazem diff contra o estado do git e pulam silenciosamente fora de um repositório. A verificação de padrão por edição funciona em qualquer lugar

Na primeira execução, o plugin cria um ambiente virtual em `~/.claude/security/` e instala o Claude Agent SDK nele, o que requer `pip` e acesso à rede. Se essa instalação falhar, a revisão de commit volta para uma revisão única em vez de uma agêntica. No Windows, a etapa de ambiente virtual é ignorada, portanto a revisão de commit agêntica é executada apenas se `claude-agent-sdk` já for importável e, caso contrário, volta da mesma forma.

<h2 id="install-the-plugin">
  Instalar o plugin
</h2>

Em uma sessão Claude Code, instale do [marketplace oficial da Anthropic](https://code.claude.com/docs/pt/discover-plugins#official-anthropic-marketplace):

```text theme={null}
/plugin install security-guidance@claude-plugins-official
```

A instalação solicita um escopo. Escolha escopo de usuário para escrever o plugin em suas configurações de usuário, para que seja carregado em cada nova sessão local que você inicia nesta máquina. Se Claude Code relatar que o marketplace não foi encontrado, execute `/plugin marketplace add anthropics/claude-plugins-official` primeiro e tente novamente a instalação.

Em seguida, ative-o na sessão atual com `/reload-plugins`, que aplica alterações de plugin pendentes sem uma reinicialização:

```text theme={null}
/reload-plugins
```

<h3 id="enable-in-cloud-sessions-and-shared-repositories">
  Ativar em sessões na nuvem e repositórios compartilhados
</h3>

Plugins com escopo de usuário não são transferidos para [Claude Code na web](https://code.claude.com/docs/pt/claude-code-on-the-web), porque essas sessões são executadas na infraestrutura da Anthropic em vez de sua máquina. Para ativar o plugin lá, ou para ativá-lo para todos que clonam um repositório, declare-o nas configurações verificadas do projeto:

```json .claude/settings.json theme={null}
{
  "enabledPlugins": {
    "security-guidance@claude-plugins-official": true
  }
}
```

Os administradores podem ativar o plugin em toda a organização definindo [`enabledPlugins`](https://code.claude.com/docs/pt/settings#plugin-settings) em [configurações gerenciadas](https://code.claude.com/docs/pt/admin-setup).

<h2 id="what-the-plugin-checks">
  O que o plugin verifica
</h2>

O plugin revisa o trabalho de Claude em três pontos, cada um em uma profundidade diferente:

* [Em cada edição de arquivo](#on-each-file-edit): uma correspondência de padrão rápida para chamadas arriscadas, sem chamada de modelo
* [No final de cada turno](#at-the-end-of-each-turn): uma revisão de modelo em segundo plano de tudo que aquele turno alterou
* [Em cada commit ou push que Claude faz](#on-each-commit-or-push-claude-makes): uma revisão agêntica mais profunda que lê o código circundante

Você pode estender cada camada [adicionando suas próprias regras](#add-your-own-rules). As verificações integradas não podem ser removidas individualmente, mas você pode [desabilitar cada camada](#disable-or-uninstall) independentemente.

<h3 id="on-each-file-edit">
  Em cada edição de arquivo
</h3>

Quando Claude escreve em um arquivo, o plugin verifica o novo conteúdo em busca de padrões conhecidos arriscados. Esta é uma correspondência de padrão sem chamada de modelo, portanto não adiciona custo de uso.

Categorias de padrão de exemplo:

* Execução dinâmica de código: `eval(`, `new Function`, `os.system`, `child_process.exec`
* Desserialização insegura: `pickle`
* Injeção DOM: `dangerouslySetInnerHTML`, `.innerHTML =`, `document.write`
* Arquivos de fluxo de trabalho: edições em `.github/workflows/`, que podem conceder permissões no nível do repositório

A verificação é executada após o edit ser aplicado e anexa o aviso ao contexto de Claude para a próxima etapa. Cada aviso é acionado uma vez por padrão por arquivo por sessão, portanto correspondências repetidas no mesmo arquivo não inundam a conversa.

Você pode [adicionar seus próprios padrões](#add-custom-per-edit-patterns) a esta camada com um arquivo `security-patterns.yaml`.

<h3 id="at-the-end-of-each-turn">
  No final de cada turno
</h3>

Um turno é uma rodada de Claude respondendo: você envia uma mensagem, Claude trabalha e responde, e o turno termina. Após cada turno, o plugin calcula um git diff de tudo que mudou na árvore de trabalho durante o turno, incluindo alterações das ferramentas de edição de Claude, comandos Bash e subagentos, e o envia para uma revisão Claude separada focada em segurança. A revisão é executada em segundo plano, portanto a resposta de Claude não é atrasada. Se a revisão encontrar problemas, Claude é re-solicitado com as descobertas e as aborda como um acompanhamento.

Isso detecta problemas que uma correspondência de string não consegue, como:

* Bypass de autorização
* Referências diretas de objeto inseguras
* Injeção
* Falsificação de solicitação do lado do servidor
* Criptografia fraca

Você vê tanto a descoberta quanto a resolução de Claude diretamente em sua sessão. A revisão cobre até 30 arquivos alterados por turno e é acionada no máximo três vezes seguidas antes de ceder de volta para você.

<h3 id="on-each-commit-or-push-claude-makes">
  Em cada commit ou push que Claude faz
</h3>

Quando Claude executa `git commit` ou `git push` através de sua ferramenta Bash, o plugin executa uma revisão agêntica mais profunda da alteração em segundo plano. Esta revisão lê o código circundante, incluindo chamadores, sanitizadores e arquivos relacionados, para decidir se uma descoberta é real antes de relatá-la. O contexto extra mantém falsos positivos baixos em padrões que parecem perigosos isoladamente, mas são seguros em seu repositório.

Esta camada é acionada apenas em commits e pushes que Claude faz através de sua ferramenta Bash. Commits que você executa a partir de seu próprio shell, incluindo o escape de shell `!` dentro de uma sessão, não são revisados. As revisões de commit e push são limitadas a 20 por hora contínua. Se as descobertas da revisão de commit duplicarem o que a revisão de fim de turno já relatou, Claude não é re-solicitado, portanto um commit limpo não produz saída visível desta camada.

<h3 id="review-independence-and-limits">
  Independência de revisão e limites
</h3>

O plugin não pede à mesma instância de Claude que escreveu o código para se avaliar. A verificação por edição é uma correspondência de string determinística sem modelo envolvido. As revisões de fim de turno e commit são executadas como uma chamada Claude separada com um contexto novo e um prompt focado em segurança: o revisor começa a partir do diff, não tem investimento na abordagem original e é instruído apenas a encontrar problemas.

Nenhuma das camadas bloqueia escritas ou commits. As descobertas chegam ao Claude que escreve como instruções, Claude as aborda na conversa, e o modelo de revisão pode perder problemas. Trate o plugin como uma camada de defesa em profundidade, não uma solução de segurança completa. Consulte [Como isso se encaixa com outras ferramentas de segurança](#how-this-fits-with-other-security-tools).

<h2 id="add-your-own-rules">
  Adicione suas próprias regras
</h2>

O plugin tem dois pontos de extensão: um arquivo de orientação Markdown para as revisões apoiadas por modelo e um arquivo de padrões YAML ou JSON para a correspondência de string por edição. Ambos são aditivos. Você pode adicionar verificações, mas não pode desabilitar as integradas a partir desses arquivos.

<h3 id="add-guidance-for-the-model-backed-reviews">
  Adicione orientação para as revisões apoiadas por modelo
</h3>

Crie `.claude/claude-security-guidance.md` em seu projeto e descreva seu modelo de ameaça e lista de verificação de revisão em linguagem simples. As revisões apoiadas por modelo a carregam como contexto adicional ao lado da lista de verificação de vulnerabilidade integrada.

O exemplo a seguir é para um serviço web com rotas de administrador com portão de função e uma política de logging de dados do cliente:

```markdown .claude/claude-security-guidance.md theme={null}
# Security guidance for this repo

- Do not log `customer_id` or `account_number` at INFO level or above.
- All routes under `/admin` must call `require_role("admin")` before any database read.
- Use `crypto.timingSafeEqual` for token comparison instead of `===`.
```

Essas regras são orientação para o revisor, não guardrails determinísticos. O plugin apresenta violações como descobertas para Claude corrigir, mas não bloqueia escritas ou garante que cada violação seja detectada. A orientação é apenas aditiva: uma regra que diz ignorar uma classe de vulnerabilidade não suprime essas descobertas. Para aplicação rígida, combine o plugin com um [hook que bloqueia a edição](https://code.claude.com/docs/pt/hooks-guide#block-edits-to-protected-files) ou uma verificação de CI.

<h3 id="add-custom-per-edit-patterns">
  Adicione padrões personalizados por edição
</h3>

Crie `.claude/security-patterns.yaml` para adicionar regras regex ou substring à [verificação de padrão por edição](#on-each-file-edit). Estas são executadas como correspondências de string determinísticas ao lado dos padrões integrados:

```yaml .claude/security-patterns.yaml theme={null}
patterns:
  - rule_name: internal_api_key
    substrings: ["sk_live_", "AKIA"]
    reminder: "Hardcoded API key prefix. Load credentials from the secret manager."
  - rule_name: tenant_unfiltered_query
    regex: "\\.objects\\.all\\(\\)"
    paths: ["**/src/tenants/**"]
    reminder: "Multi-tenant code must filter by org_id."
```

| Campo           | Tipo   | Descrição                                                                                                                                                                                 |
| :-------------- | :----- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `rule_name`     | string | Identificador mostrado no aviso                                                                                                                                                           |
| `reminder`      | string | Texto de aviso anexado ao contexto de Claude, limitado a 1 KB                                                                                                                             |
| `regex`         | string | Regex Python correspondido contra o conteúdo editado                                                                                                                                      |
| `substrings`    | list   | Substrings literais; forneça isto ou `regex`                                                                                                                                              |
| `paths`         | list   | Padrões glob opcionais; a regra se aplica apenas aos arquivos correspondentes. Globs correspondem ao caminho completo do arquivo, portanto prefixe padrões relativos ao projeto com `**/` |
| `exclude_paths` | list   | Padrões glob opcionais para pular; mesma correspondência que `paths`                                                                                                                      |

O plugin também lê `.claude/security-patterns.yml` e `.claude/security-patterns.json` com o mesmo esquema. JSON funciona em qualquer instalação Python. Os formulários YAML requerem que PyYAML seja importável, que o plugin não instala para você. O plugin carrega até 50 regras personalizadas e pula regexes que parecem propensas a backtracking catastrófico.

<h3 id="rule-file-lookup-locations">
  Locais de pesquisa de arquivo de regra
</h3>

O plugin procura por `claude-security-guidance.md` e `security-patterns.yaml` nos mesmos locais, independentemente de como o plugin foi ativado:

| Escopo        | Caminho                                     | Notas                                   |
| :------------ | :------------------------------------------ | :-------------------------------------- |
| Usuário       | `~/.claude/claude-security-guidance.md`     | Aplica-se a cada projeto em sua máquina |
| Projeto       | `.claude/claude-security-guidance.md`       | Verificado com o repositório            |
| Projeto local | `.claude/claude-security-guidance.local.md` | Gitignored, para substituições pessoais |

O plugin carrega todos os locais que existem e os concatena, com um limite combinado de 8 KB para o arquivo de orientação. Os administradores podem distribuir regras em toda a organização empurrando o arquivo com escopo de usuário para `~/.claude/` através do gerenciamento de dispositivos. Os mesmos caminhos se aplicam a `security-patterns.yaml`.

<h2 id="usage-cost">
  Custo de uso
</h2>

A [verificação de padrão por edição](#on-each-file-edit) não faz chamada de modelo e não adiciona custo. As revisões de [fim de turno](#at-the-end-of-each-turn) e [commit](#on-each-commit-or-push-claude-makes) cada uma gasta uso de modelo adicional que conta para seu [uso](https://code.claude.com/docs/pt/costs) como qualquer outra solicitação Claude. A revisão de commit é agêntica e pode levar vários turnos de modelo por commit, limitada a 20 revisões por hora contínua. Espere aproximadamente uma chamada de revisão por turno que altera arquivos e uma revisão mais profunda por commit, ambas sujeitas aos limites acima.

Ambas as revisões apoiadas por modelo usam Claude Opus 4.7 por padrão. Defina `SECURITY_REVIEW_MODEL` para escolher um modelo diferente para a revisão de fim de turno e `SG_AGENTIC_MODEL` para a revisão de commit.

O plugin está disponível em todos os planos.

<h2 id="disable-or-uninstall">
  Desabilitar ou desinstalar
</h2>

Para desativar camadas individuais mantendo o resto, defina a variável de ambiente correspondente:

| Variável                        | Efeito                                                                         |
| :------------------------------ | :----------------------------------------------------------------------------- |
| `ENABLE_PATTERN_RULES=0`        | Desabilitar a [verificação de padrão por edição](#on-each-file-edit)           |
| `ENABLE_STOP_REVIEW=0`          | Desabilitar a [revisão de diff de fim de turno](#at-the-end-of-each-turn)      |
| `ENABLE_COMMIT_REVIEW=0`        | Desabilitar a [revisão de commit e push](#on-each-commit-or-push-claude-makes) |
| `ENABLE_CODE_SECURITY_REVIEW=0` | Desabilitar todas as revisões apoiadas por modelo de uma vez                   |
| `SECURITY_GUIDANCE_DISABLE=1`   | Desabilitar o plugin completamente sem desinstalar                             |

Para pausar o plugin em seu escopo de usuário:

```text theme={null}
/plugin disable security-guidance@claude-plugins-official
```

Para removê-lo de seu escopo de usuário:

```text theme={null}
/plugin uninstall security-guidance@claude-plugins-official
```

Se o plugin foi ativado através do `.claude/settings.json` de um projeto, desabilitá-lo de `/plugin` escreve uma substituição para seu `.claude/settings.local.json` em vez de editar o arquivo verificado, portanto o plugin permanece desativado para você enquanto os colegas de equipe não são afetados. O mesmo diálogo também oferece desinstalar o plugin para todos removendo-o do `.claude/settings.json` compartilhado; essa opção requer Claude Code v2.1.203 ou posterior. Se foi ativado através de [configurações gerenciadas](https://code.claude.com/docs/pt/admin-setup), apenas um administrador pode desabilitá-lo.

<h2 id="how-the-plugin-integrates-with-claude-code">
  Como o plugin se integra com Claude Code
</h2>

O plugin é construído inteiramente em [hooks](https://code.claude.com/docs/pt/hooks), o mecanismo para executar seu próprio código em pontos específicos do loop de Claude. Ele registra:

| Evento de Hook                                                   | Propósito                                                                                    |
| :--------------------------------------------------------------- | :------------------------------------------------------------------------------------------- |
| `SessionStart`                                                   | Inicializar o ambiente Python do plugin                                                      |
| `UserPromptSubmit`                                               | Capturar a linha de base da árvore de trabalho que a revisão de fim de turno faz diff contra |
| `PostToolUse` em `Edit`, `Write` e `NotebookEdit`                | Correspondência de padrão por edição                                                         |
| `Stop`                                                           | Revisão de diff de fim de turno, executada em segundo plano                                  |
| `PostToolUse` em `Bash`, filtrado para `git commit` e `git push` | Revisão de commit e push, executada em segundo plano                                         |

Se você construir seus próprios hooks, o [código-fonte do plugin](https://github.com/anthropics/claude-plugins-official/tree/main/plugins/security-guidance) é um exemplo funcional de executar uma chamada de modelo separada a partir de um hook e alimentar o resultado de volta para a sessão.

<h2 id="how-this-fits-with-other-security-tools">
  Como isso se encaixa com outras ferramentas de segurança
</h2>

O plugin é uma camada em uma abordagem de defesa em profundidade. Ele detecta problemas mais cedo, enquanto o código ainda está no editor, mas não é uma garantia e não substitui verificações posteriores. Uma pilha típica:

| Estágio         | Ferramenta                                                 | O que cobre                                                                                                           |
| :-------------- | :--------------------------------------------------------- | :-------------------------------------------------------------------------------------------------------------------- |
| Em sessão       | Plugin de orientação de segurança                          | Vulnerabilidades comuns em código que Claude escreve, corrigidas na mesma sessão                                      |
| Sob demanda     | [`/security-review`](https://code.claude.com/docs/pt/commands#all-commands)            | Uma passagem de segurança única no branch atual, executada quando você pede                                           |
| Em pull request | [Code Review](https://code.claude.com/docs/pt/code-review), planos Team e Enterprise   | Revisão de correção e segurança multi-agente com contexto completo do repositório                                     |
| Em CI           | Seus scanners de análise estática e dependência existentes | Regras específicas de linguagem, verificações de cadeia de suprimentos e aplicação de política que o plugin não tenta |

Cada estágio posterior detecta o que os anteriores perdem. O valor do plugin é reduzir o volume que chega a eles, não eliminar a necessidade deles.

<h2 id="troubleshooting">
  Solução de problemas
</h2>

O plugin escreve diagnósticos de tempo de execução em `~/.claude/security/log.txt`. Verifique lá primeiro se as revisões não estão aparecendo.

Razões comuns pelas quais uma camada de revisão pula sem uma mensagem na conversa:

* O diretório não é um repositório git: as revisões de fim de turno e commit requerem estado do git e pulam fora de um repositório
* A sessão não tem autenticação Anthropic: as revisões apoiadas por modelo pulam e apenas a verificação de padrão por edição é executada
* Um arquivo `security-patterns.yaml` está presente, mas PyYAML não é importável: o arquivo é ignorado. Use `security-patterns.json` em vez disso

<h2 id="related-resources">
  Recursos relacionados
</h2>

Para aprofundar-se nos tópicos que esta página toca:

* [Code Review](https://code.claude.com/docs/pt/code-review): configurar a revisão multi-agente no tempo de PR
* [Automatizar fluxos de trabalho com hooks](https://code.claude.com/docs/pt/hooks-guide): construir suas próprias verificações nos mesmos pontos de ciclo de vida
* [Descobrir e instalar plugins](https://code.claude.com/docs/pt/discover-plugins#official-anthropic-marketplace): procurar outros plugins oficiais
