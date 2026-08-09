<!-- Fonte: https://code.claude.com/docs/pt/debug-your-config | Baixado em: 2026-08-06 | slug: debug-your-config -->

# Depure sua configuração

> Diagnostique por que CLAUDE.md, configurações, hooks, servidores MCP ou skills não estão tendo efeito. Use /context, /doctor, /hooks e /mcp para ver o que realmente foi carregado.

Quando Claude ignora uma instrução ou um recurso que você configurou não aparece, a causa geralmente é que o arquivo não foi carregado, foi carregado de um local diferente do esperado, ou outro arquivo o sobrescreveu. Este guia mostra como inspecionar o que Claude Code realmente carregou para que você possa estreitar qual se aplica.

Para problemas de instalação, autenticação e conectividade, consulte [Troubleshooting installation and login](https://code.claude.com/docs/pt/troubleshoot-install) em vez disso.

<h2 id="see-what-loaded-into-context">
  Veja o que foi carregado no contexto
</h2>

O comando `/context` mostra tudo que ocupa a janela de contexto para a sessão atual, dividido por categoria: prompt do sistema, arquivos de memória, skills, subagentes personalizados com a fonte de cada um carregado, ferramentas MCP e mensagens de conversa. Execute-o primeiro para confirmar se seu `CLAUDE.md`, regras ou descrições de skill estão presentes.

Para detalhes sobre uma categoria específica, acompanhe com o comando dedicado:

| Comando          | Mostra                                                                                                                                                                                                                                                                                   |
| :--------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/memory`        | Quais arquivos `CLAUDE.md` e rules foram carregados, além de entradas de auto-memória                                                                                                                                                                                                    |
| `/skills`        | Skills disponíveis de fontes de projeto, usuário e plugin                                                                                                                                                                                                                                |
| `/hooks`         | Configurações de hook ativas                                                                                                                                                                                                                                                             |
| `/mcp`           | Servidores MCP conectados e seu status                                                                                                                                                                                                                                                   |
| `/permissions`   | Regras de permissão e negação resolvidas atualmente em vigor                                                                                                                                                                                                                             |
| `/doctor`        | Diagnóstico de configuração: saúde da instalação, arquivos de configurações inválidos, extensões não utilizadas, nomes de [subagente](https://code.claude.com/docs/pt/sub-agents) duplicados no mesmo diretório e conteúdo `CLAUDE.md` verificado que Claude pode derivar da base de código, com correções propostas |
| `/debug [issue]` | Ativa o log de depuração para a sessão e solicita que Claude diagnostique usando a saída do log e caminhos de configurações                                                                                                                                                              |
| `/status`        | Fontes de configurações ativas, incluindo se as configurações gerenciadas estão em vigor                                                                                                                                                                                                 |

Se um arquivo de memória estiver faltando em `/memory`, verifique sua localização em relação a [como os arquivos CLAUDE.md são carregados](https://code.claude.com/docs/pt/memory#how-claude-md-files-load). Os arquivos `CLAUDE.md` do subdiretório são carregados sob demanda quando Claude lê um arquivo nesse diretório com a ferramenta Read, não no início da sessão.

Se `/memory` confirmar que o arquivo foi carregado mas Claude ainda não está seguindo uma instrução particular, o problema provavelmente é como a instrução é escrita e não se foi carregada. CLAUDE.md funciona bem para o tipo de orientação que você daria a um novo colega de equipe, como convenções de projeto, comandos de compilação e onde os arquivos pertencem.

A aderência diminui quando uma instrução é vaga o suficiente para ser interpretada de várias maneiras, quando dois arquivos dão direções conflitantes, ou quando o arquivo cresceu o suficiente para que regras individuais recebam menos atenção. [Escreva instruções eficazes](https://code.claude.com/docs/pt/memory#write-effective-instructions) cobre os padrões de especificidade, tamanho e estrutura que mantêm a aderência alta.

<Note>
  CLAUDE.md e permissões resolvem problemas diferentes. CLAUDE.md diz a Claude como seu projeto funciona para que ele tome boas decisões. [Permissões](https://code.claude.com/docs/pt/permissions) e [hooks](https://code.claude.com/docs/pt/hooks) aplicam limites independentemente do que Claude decide. Use CLAUDE.md para "fazemos assim aqui". Use permissões ou hooks para limites de segurança e qualquer coisa que nunca deve acontecer, onde você precisa de uma garantia em vez de orientação.
</Note>

<h2 id="check-resolved-settings">
  Verifique as configurações resolvidas
</h2>

As configurações se mesclam entre escopos gerenciados, de usuário, de projeto e locais. As configurações gerenciadas sempre vencem quando presentes. Entre o resto, o escopo mais próximo substitui o mais amplo na ordem local, depois projeto, depois usuário. Algumas configurações também podem ser definidas por sinalizadores de linha de comando ou [variáveis de ambiente](https://code.claude.com/docs/pt/env-vars), que atuam como outra camada de substituição. Quando uma configuração não parece se aplicar, o valor que você definiu geralmente está sendo substituído por outro escopo ou uma variável de ambiente.

Execute `/doctor` para verificar sua configuração e instalação. Ele relata o que encontra, incluindo arquivos de configurações inválidos, instalações duplicadas, extensões não utilizadas e conteúdo de `CLAUDE.md` verificado que Claude pode derivar da base de código, depois propõe correções que aplica apenas após você confirmar. A verificação de corte de `CLAUDE.md` requer Claude Code v2.1.206 ou posterior. Antes da v2.1.205, `/doctor` abria uma tela de diagnósticos somente leitura e pressionar `f` enviava o relatório a Claude para corrigir.

Do terminal, `claude doctor` imprime diagnósticos de instalação e configurações somente leitura sem iniciar uma sessão.

Execute `/status` para ver quais fontes de configurações estão ativas, incluindo se as configurações gerenciadas estão em vigor. Para entender qual escopo vence para uma chave específica, consulte [Como os escopos interagem](https://code.claude.com/docs/pt/settings#how-scopes-interact).

<h2 id="check-mcp-servers">
  Verifique os servidores MCP
</h2>

Execute `/mcp` para ver cada servidor configurado, seu status de conexão e se você o aprovou para o projeto atual. Um servidor pode ser definido corretamente mas ainda não fornecer ferramentas por alguns motivos comuns:

* Servidores com escopo de projeto em `.mcp.json` requerem uma aprovação única. Se o prompt foi descartado, o servidor permanece desabilitado até que você o aprove em `/mcp`.
* Um servidor que falha ao iniciar aparece como falho em `/mcp`. Caminhos de arquivo relativos em `command` ou `args` são uma causa frequente, pois são resolvidos em relação ao diretório de onde você iniciou Claude Code em vez da localização de `.mcp.json`.
* Um servidor que aparece como conectado mas lista zero ferramentas iniciou com sucesso mas não está retornando uma lista de ferramentas. Selecione **Reconnect** em `/mcp`. Se a contagem permanecer em zero, execute `claude --debug mcp` para ver a saída stderr do servidor.

Para localizações de configuração e regras de escopo, consulte [MCP](https://code.claude.com/docs/pt/mcp).

<h2 id="check-hooks">
  Verifique hooks
</h2>

Execute `/hooks` para listar cada hook registrado para a sessão atual, agrupado por evento. Se um hook que você definiu não aparecer, ele não está sendo lido: hooks vão sob a chave `"hooks"` em um arquivo de configurações, não em um arquivo autônomo.

Se o hook aparecer mas não disparar, o matcher é a causa usual. Verifique-o para estes erros:

* O campo `matcher` é uma única string que usa `|` para corresponder a vários nomes de ferramentas, por exemplo `"Edit|Write"`. Um separador `,` é equivalente, então `"Edit,Write"` corresponde às mesmas ferramentas. Antes da v2.1.191, uma vírgula passava para avaliação de regex e o matcher nunca correspondia, então use `|` se você não estiver na v2.1.191 ainda.
* Um nome de ferramenta digitado incorretamente produz um matcher que não corresponde a nada, então o hook falha silenciosamente.
* Um valor de array é um erro de schema: Claude Code mostra um aviso de erro de configurações e rejeita o arquivo de configurações do usuário, projeto ou local inteiro, `claude doctor` relata a falha de validação, e nenhum hook desse arquivo aparece em `/hooks`. Em [configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files), apenas a entrada inválida é removida e os outros hooks do arquivo ainda se aplicam.

As edições em `settings.json` entram em vigor na sessão em execução após um breve atraso de estabilidade de arquivo. Você não precisa reiniciar. Se `/hooks` ainda mostrar a definição antiga alguns segundos após salvar, execute `/hooks` novamente para atualizar a visualização.

Se `/hooks` mostrar o hook mas ele ainda não disparar, o próximo passo é observar a avaliação do hook ao vivo. Inicie uma sessão com `claude --debug hooks` e dispare a chamada de ferramenta. O log de depuração registra cada evento, quais matchers foram verificados, e o código de saída e saída do hook. Consulte [Debug hooks](https://code.claude.com/docs/pt/hooks#debug-hooks) para o formato do log e [troubleshooting de hooks](https://code.claude.com/docs/pt/hooks-guide#limitations-and-troubleshooting) para padrões de falha comuns.

<h2 id="test-against-a-clean-configuration">
  Teste contra uma configuração limpa
</h2>

Comece com [`claude --safe-mode`](https://code.claude.com/docs/pt/cli-reference#cli-flags), que inicia uma sessão com todas as personalizações desabilitadas, incluindo `CLAUDE.md`, skills, plugins, hooks, servidores MCP e comandos e agentes personalizados. Autenticação, seleção de modelo, ferramentas integradas e permissões funcionam normalmente. Se o problema desaparecer no modo seguro, uma dessas superfícies é a causa; use as verificações direcionadas acima para descobrir qual. O modo seguro ainda aplica hooks gerenciados e política de configurações da sua organização. Plugins gerenciados, skills, `CLAUDE.md` e servidores MCP são desativados.

Se o problema persistir no modo seguro, ou suas configurações em si forem suspeitas, compare contra uma sessão que não carrega nada de sua configuração usual. Aponte [`CLAUDE_CONFIG_DIR`](https://code.claude.com/docs/pt/env-vars) para um diretório vazio para contornar tudo sob `~/.claude` e inicie a partir de um diretório que não tenha pasta `.claude`, `.mcp.json` ou `CLAUDE.md` para que a configuração do projeto também seja ignorada.

```bash theme={null}
cd /tmp && CLAUDE_CONFIG_DIR=/tmp/claude-clean claude
```

A sessão limpa não tem configurações de usuário ou projeto, hooks, servidores MCP, plugins ou memória.

* As configurações gerenciadas ainda se aplicam se sua organização as implanta, pois vivem em um caminho do sistema fora de `~/.claude`
* No Linux e Windows, você será solicitado a fazer login novamente porque as credenciais são armazenadas sob o diretório de configuração
* No macOS, as credenciais estão no Keychain e são transferidas para a sessão limpa

Se o problema desaparecer aqui, a causa está em algum lugar em seus arquivos reais `~/.claude` ou `.claude` do projeto. Reintroduza-os um de cada vez, copiando arquivos para o diretório temporário ou iniciando a partir de seu projeto, para encontrar qual. Se persistir na sessão limpa, a causa está fora de sua configuração de usuário e projeto. Execute `/status` para verificar se as configurações gerenciadas estão em vigor, procure por [variáveis de ambiente](https://code.claude.com/docs/pt/env-vars) que afetam Claude Code e consulte [Solução de problemas](https://code.claude.com/docs/pt/troubleshooting).

<h2 id="check-common-causes">
  Verifique as causas comuns
</h2>

A maioria das surpresas de configuração rastreia um pequeno conjunto de regras de localização e sintaxe. Verifique estas antes de assumir um bug:

| Sintoma                                                                       | Causa                                                                                                                                | Correção                                                                                                                                                                                                                                                                                            |
| :---------------------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Hook nunca dispara                                                            | `matcher` é um array JSON em vez de uma string                                                                                       | Use uma única string com `\|` para corresponder a várias ferramentas, por exemplo `"Edit\|Write"`. Consulte [padrões de matcher](https://code.claude.com/docs/pt/hooks#matcher-patterns).                                                                                                                                       |
| Hook nunca dispara                                                            | `matcher` usa `,` como separador em uma versão anterior a v2.1.191                                                                   | Claude Code v2.1.191 ou posterior trata `,` como um separador de lista como `\|`. Versões anteriores avaliam uma vírgula como um caractere literal, então `"Edit,Write"` não corresponde a nada. Use `\|` em vez disso, ou atualize Claude Code.                                                    |
| Hook nunca dispara                                                            | O valor de `matcher` está em minúsculas, por exemplo `"bash"`                                                                        | A correspondência diferencia maiúsculas de minúsculas. Os nomes das ferramentas são capitalizados: `Bash`, `Edit`, `Write`, `Read`.                                                                                                                                                                 |
| Hook nunca dispara                                                            | Hooks estão definidos em um arquivo autônomo em vez de em `settings.json`                                                            | Não há arquivo de hooks autônomo para configuração de projeto ou usuário. Defina hooks sob a chave `"hooks"` em `settings.json`. Apenas [plugins](https://code.claude.com/docs/pt/plugins-reference#hooks) carregam um `hooks/hooks.json` separado. Consulte [configuração de hook](https://code.claude.com/docs/pt/hooks).                                 |
| Permissões, hooks ou env definidos globalmente são ignorados                  | A configuração foi adicionada a `~/.claude.json`                                                                                     | `~/.claude.json` contém estado do aplicativo e alternâncias de UI. `permissions`, `hooks` e `env` pertencem a `~/.claude/settings.json`. Estes são dois arquivos diferentes.                                                                                                                        |
| Um valor de `settings.json` parece ser ignorado                               | A mesma chave está definida em `settings.local.json`                                                                                 | `settings.local.json` substitui `settings.json`, e ambos substituem `~/.claude/settings.json`. Consulte [precedência de configurações](https://code.claude.com/docs/pt/settings#how-scopes-interact).                                                                                                                           |
| Skill não aparece em `/skills`                                                | O arquivo de skill está em `.claude/skills/name.md` em vez de em uma pasta                                                           | Use uma pasta com `SKILL.md` dentro: `.claude/skills/name/SKILL.md`.                                                                                                                                                                                                                                |
| Skill aparece em `/skills` mas Claude nunca o invoca                          | Skill tem `disable-model-invocation: true` em seu frontmatter, ou sua descrição não corresponde a como você frasa a solicitação      | Verifique o badge em `/skills`: um rótulo "user-only" significa que Claude não o acionará por conta própria. Consulte [invocação de skill](https://code.claude.com/docs/pt/skills).                                                                                                                                             |
| As instruções de `CLAUDE.md` do subdiretório parecem ser ignoradas            | Os arquivos do subdiretório são carregados sob demanda, não no início da sessão                                                      | Eles são carregados quando Claude lê um arquivo nesse diretório com a ferramenta Read, não no lançamento e não ao escrever ou criar arquivos lá. Consulte [como os arquivos CLAUDE.md são carregados](https://code.claude.com/docs/pt/memory#how-claude-md-files-load).                                                         |
| Subagente ignora as instruções de `CLAUDE.md`                                 | Os agentes Explore e Plan integrados pulam `CLAUDE.md`. Subagentes personalizados o carregam da mesma forma que a conversa principal | Para Explore ou Plan, reafirme a instrução em seu prompt de delegação. Para um subagente personalizado, coloque instruções críticas no corpo do arquivo do agente, que se torna o prompt do sistema do agente. Consulte [o que é carregado na inicialização](https://code.claude.com/docs/pt/sub-agents#what-loads-at-startup). |
| A lógica de limpeza nunca é executada no final da sessão                      | Nenhum hook `SessionEnd` configurado                                                                                                 | Adicione um hook `SessionEnd` em `settings.json`. Consulte a [lista de eventos de hook](https://code.claude.com/docs/pt/hooks#hook-events).                                                                                                                                                                                     |
| Servidores MCP em `.mcp.json` nunca são carregados                            | O arquivo está sob `.claude/` ou usa o formato de configuração do Claude Desktop                                                     | A configuração MCP do projeto vai na raiz do repositório como `.mcp.json`, não dentro de `.claude/`. Consulte [configuração MCP](https://code.claude.com/docs/pt/mcp).                                                                                                                                                          |
| Servidores MCP adicionados sob `mcpServers` em `settings.json` nunca aparecem | `settings.json` não lê uma chave `mcpServers`                                                                                        | Defina servidores de projeto em `.mcp.json` na raiz do repositório, ou execute `claude mcp add --scope user` para servidores com escopo de usuário. Consulte [configuração MCP](https://code.claude.com/docs/pt/mcp).                                                                                                           |
| Servidor MCP do projeto adicionado mas não aparece                            | O prompt de aprovação única foi descartado                                                                                           | Servidores com escopo de projeto requerem aprovação. Execute `/mcp` para ver o status e aprovar.                                                                                                                                                                                                    |
| Servidor MCP falha ao iniciar de alguns diretórios                            | `command` ou `args` usa um caminho de arquivo relativo                                                                               | Use caminhos absolutos para scripts locais. Executáveis em seu `PATH` como `npx` ou `uvx` funcionam como estão.                                                                                                                                                                                     |
| Servidor MCP inicia sem variáveis de ambiente esperadas                       | As variáveis estão em `settings.json` `env`, que não se propaga para processos filhos MCP                                            | Defina `env` por servidor dentro de `.mcp.json` em vez disso.                                                                                                                                                                                                                                       |
| A regra de negação `Bash(rm *)` não bloqueia `/bin/rm` ou `find -delete`      | As regras de prefixo correspondem à string de comando literal, não ao executável subjacente                                          | Adicione padrões explícitos para cada variante, ou use um [hook PreToolUse](https://code.claude.com/docs/pt/hooks-guide) ou o [sandbox](https://code.claude.com/docs/pt/sandboxing) para uma garantia difícil.                                                                                                                                              |

<h2 id="related-resources">
  Recursos relacionados
</h2>

Para referência completa em cada superfície de configuração, consulte a página dedicada:

* **[Referência do diretório `.claude`](https://code.claude.com/docs/pt/claude-directory)**: cada localização de arquivo de configuração e o que o lê
* **[Configurações](https://code.claude.com/docs/pt/settings)**: ordem de precedência e a lista completa de chaves
* **[Referência de hooks](https://code.claude.com/docs/pt/hooks)**: nomes de eventos, payloads e formato de saída `--debug hooks`
* **[MCP](https://code.claude.com/docs/pt/mcp)**: configuração de servidor, aprovação e saída `/mcp`
* **[Solucionar problemas de instalação e login](https://code.claude.com/docs/pt/troubleshoot-install)**: `comando não encontrado`, PATH e problemas de autenticação
* **[Solução de problemas](https://code.claude.com/docs/pt/troubleshooting)**: desempenho, travamentos e problemas de busca
