<!-- Fonte: https://code.claude.com/docs/pt/statusline | Baixado em: 2026-08-06 | slug: statusline -->

# Personalize sua linha de status

> Configure uma barra de status personalizada para monitorar o uso da janela de contexto, custos e status do git no Claude Code

A linha de status é uma barra personalizável na parte inferior do Claude Code que executa qualquer script de shell que você configurar. Ela recebe dados de sessão JSON em stdin e exibe tudo o que seu script imprime, oferecendo uma visualização persistente e rápida do uso de contexto, custos, status do git ou qualquer outra coisa que você queira rastrear.

As linhas de status são úteis quando você:

* Quer monitorar o uso da janela de contexto enquanto trabalha
* Precisa rastrear custos de sessão
* Trabalha em várias sessões e precisa distingui-las
* Quer que a ramificação git e o status estejam sempre visíveis

A linha de status é renderizada em sua própria linha acima dos crachás de rodapé integrados e não os substitui. Para adicionar crachás de links clicáveis ao rodapé quando uma ID aparece na conversa, sem escrever um script, configure [`footerLinksRegexes`](https://code.claude.com/docs/pt/settings#footer-link-badges) em vez disso.

Aqui está um exemplo de uma [linha de status de múltiplas linhas](#display-multiple-lines) que exibe informações do git na primeira linha e uma barra de contexto codificada por cores na segunda.

<Frame>
  <img src="https://mintcdn.com/claude-code/nibzesLaJVh4ydOq/images/statusline-multiline.png?fit=max&auto=format&n=nibzesLaJVh4ydOq&q=85&s=60f11387658acc9ff75158ae85f2ac87" alt="Uma linha de status de múltiplas linhas mostrando nome do modelo, diretório, ramificação git na primeira linha, e uma barra de progresso de uso de contexto com custo e duração na segunda linha" width="776" height="212" data-path="images/statusline-multiline.png" />
</Frame>

Esta página orienta você sobre [configurar uma linha de status básica](#set-up-a-status-line), explica [como os dados fluem](#how-status-lines-work) do Claude Code para seu script, lista [todos os campos que você pode exibir](#available-data) e fornece [exemplos prontos para usar](#examples) para padrões comuns como status do git, rastreamento de custos e barras de progresso.

<h2 id="set-up-a-status-line">
  Configurar uma linha de status
</h2>

Use o [comando `/statusline`](#use-the-%2Fstatusline-command) para fazer com que o Claude Code gere um script para você, ou [crie manualmente um script](#manually-configure-a-status-line) e adicione-o às suas configurações.

<h3 id="use-the-/statusline-command">
  Use o comando /statusline
</h3>

O comando `/statusline` aceita instruções em linguagem natural descrevendo o que você quer exibir. O Claude Code gera um arquivo de script em `~/.claude/` e atualiza suas configurações automaticamente:

```text theme={null}
/statusline show model name and context percentage with a progress bar
```

<h3 id="manually-configure-a-status-line">
  Configure manualmente uma linha de status
</h3>

Adicione um campo `statusLine` às suas configurações de usuário (`~/.claude/settings.json`, onde `~` é seu diretório inicial) ou [configurações de projeto](https://code.claude.com/docs/pt/settings#settings-files). Defina `type` como `"command"` e aponte `command` para um caminho de script ou um comando de shell inline. Para um passo a passo completo de criação de um script, consulte [Construir uma linha de status passo a passo](#build-a-status-line-step-by-step).

```json theme={null}
{
  "statusLine": {
    "type": "command",
    "command": "~/.claude/statusline.sh",
    "padding": 2
  }
}
```

O campo `command` é executado em um shell, então você também pode usar comandos inline em vez de um arquivo de script. Este exemplo usa `jq` para analisar a entrada JSON e exibir o nome do modelo e a porcentagem de contexto:

```json theme={null}
{
  "statusLine": {
    "type": "command",
    "command": "jq -r '\"[\\(.model.display_name)] \\(.context_window.used_percentage // 0)% context\"'"
  }
}
```

O campo `padding` opcional adiciona espaçamento horizontal extra (em caracteres) ao conteúdo da linha de status. O padrão é `0`. Este preenchimento é além do espaçamento integrado da interface, então controla o recuo relativo em vez da distância absoluta da borda do terminal.

O campo `refreshInterval` opcional executa novamente seu comando a cada N segundos além das [atualizações orientadas por eventos](#how-status-lines-work). O mínimo é `1`. Defina isto quando sua linha de status exibir dados baseados em tempo, como um relógio, ou quando subagentes em segundo plano alterarem o estado do git enquanto a sessão principal está ociosa. Deixe sem definir para executar apenas em eventos.

O campo `hideVimModeIndicator` opcional suprime o texto integrado `-- INSERT --` abaixo do prompt. Defina isto como `true` quando seu script renderizar [`vim.mode`](#available-data) em si, para que o modo não seja exibido duas vezes.

<h3 id="disable-the-status-line">
  Desabilitar a linha de status
</h3>

Execute `/statusline` e peça para remover ou limpar sua linha de status (por exemplo, `/statusline delete`, `/statusline clear`, `/statusline remove it`). Você também pode excluir manualmente o campo `statusLine` do seu settings.json.

<h2 id="build-a-status-line-step-by-step">
  Construir uma linha de status passo a passo
</h2>

Este passo a passo mostra o que está acontecendo nos bastidores criando manualmente uma linha de status que exibe o modelo atual, diretório de trabalho e porcentagem de uso da janela de contexto.

<Note>Executar [`/statusline`](#use-the-%2Fstatusline-command) com uma descrição do que você quer configura tudo isso automaticamente para você.</Note>

Estes exemplos usam scripts Bash, que funcionam no macOS e Linux. No Windows, consulte [Configuração do Windows](#windows-configuration) para exemplos de PowerShell e Git Bash.

<Frame>
  <img src="https://mintcdn.com/claude-code/nibzesLaJVh4ydOq/images/statusline-quickstart.png?fit=max&auto=format&n=nibzesLaJVh4ydOq&q=85&s=696445e59ca0059213250651ad23db6b" alt="Uma linha de status mostrando nome do modelo, diretório e porcentagem de contexto" width="726" height="164" data-path="images/statusline-quickstart.png" />
</Frame>

<Steps>
  <Step title="Crie um script que leia JSON e imprima a saída">
    O Claude Code envia dados JSON para seu script via stdin. Este script usa [`jq`](https://jqlang.github.io/jq/), um analisador JSON de linha de comando que você pode precisar instalar, para extrair o nome do modelo, diretório e porcentagem de contexto, depois imprime uma linha formatada.

    Salve isto em `~/.claude/statusline.sh` (onde `~` é seu diretório inicial, como `/Users/username` no macOS ou `/home/username` no Linux):

    ```bash theme={null}
    #!/bin/bash
    # Read JSON data that Claude Code sends to stdin
    input=$(cat)

    # Extract fields using jq
    MODEL=$(echo "$input" | jq -r '.model.display_name')
    DIR=$(echo "$input" | jq -r '.workspace.current_dir')
    # The "// 0" provides a fallback if the field is null
    PCT=$(echo "$input" | jq -r '.context_window.used_percentage // 0' | cut -d. -f1)

    # Output the status line - ${DIR##*/} extracts just the folder name
    echo "[$MODEL] 📁 ${DIR##*/} | ${PCT}% context"
    ```
  </Step>

  <Step title="Torne-o executável">
    Marque o script como executável para que seu shell possa executá-lo:

    ```bash theme={null}
    chmod +x ~/.claude/statusline.sh
    ```
  </Step>

  <Step title="Adicione às configurações">
    Diga ao Claude Code para executar seu script como a linha de status. Adicione esta configuração a `~/.claude/settings.json`, que define `type` como `"command"` (significando "execute este comando de shell") e aponta `command` para seu script:

    ```json theme={null}
    {
      "statusLine": {
        "type": "command",
        "command": "~/.claude/statusline.sh"
      }
    }
    ```

    Sua linha de status aparece na parte inferior da interface. As configurações são recarregadas automaticamente, mas as alterações não aparecerão até sua próxima interação com o Claude Code.
  </Step>
</Steps>

<h2 id="how-status-lines-work">
  Como as linhas de status funcionam
</h2>

O Claude Code executa seu script e envia [dados de sessão JSON](#available-data) para ele via stdin. Seu script lê o JSON, extrai o que precisa e imprime texto para stdout. O Claude Code exibe tudo o que seu script imprime.

**Quando é atualizado**

Seu script é executado após cada nova mensagem do assistente, após `/compact` terminar, quando o modo de permissão muda ou quando o modo vim alterna. As atualizações são debounced em 300ms, significando que mudanças rápidas são agrupadas e seu script é executado uma vez que as coisas se estabilizam. Se uma nova atualização for acionada enquanto seu script ainda está em execução, a execução em andamento é cancelada. Se você editar seu script, as alterações não aparecerão até que sua próxima interação com o Claude Code acione uma atualização.

Estes gatilhos podem ficar silenciosos quando a sessão principal está ociosa, por exemplo enquanto um coordenador aguarda subagentes em segundo plano. Para manter segmentos baseados em tempo ou de origem externa atualizados durante períodos ociosos, defina [`refreshInterval`](#manually-configure-a-status-line) para também executar novamente o comando em um temporizador fixo.

**O que seu script pode exibir**

* **Múltiplas linhas**: cada instrução `echo` ou `print` é exibida como uma linha separada. Consulte o [exemplo de múltiplas linhas](#display-multiple-lines).
* **Cores**: use [códigos de escape ANSI](https://en.wikipedia.org/wiki/ANSI_escape_code#Colors) como `\033[32m` para verde (o terminal deve suportá-los). Consulte o [exemplo de status do git](#git-status-with-colors).
* **Links**: use [sequências de escape OSC 8](https://en.wikipedia.org/wiki/ANSI_escape_code#OSC) para tornar o texto clicável (Cmd+clique no macOS, Ctrl+clique no Windows/Linux). Requer um terminal que suporte hiperlinks como iTerm2, Kitty ou WezTerm. Consulte o [exemplo de links clicáveis](#clickable-links).

**Dimensionando a saída para o terminal**

O Claude Code captura a saída do seu script em vez de conectá-la diretamente ao terminal, portanto `tput cols` e a detecção de largura em nível de linguagem não podem ler o tamanho do terminal de dentro do script. Leia as variáveis de ambiente `COLUMNS` e `LINES` em vez disso. O Claude Code define estas variáveis para as dimensões atuais do terminal antes de executar seu script. Requer Claude Code v2.1.153 ou posterior.

<Note>A linha de status é executada localmente e não consome tokens de API. Ela se oculta temporariamente durante certas interações da interface, incluindo sugestões de preenchimento automático, o menu de ajuda e prompts de permissão.</Note>

<h2 id="available-data">
  Dados disponíveis
</h2>

O Claude Code envia os seguintes campos JSON para seu script via stdin:

| Campo                                                                            | Descrição                                                                                                                                                                                                                                                                                              |
| -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `model.id`, `model.display_name`                                                 | Identificador do modelo atual e nome de exibição                                                                                                                                                                                                                                                       |
| `cwd`, `workspace.current_dir`                                                   | Diretório de trabalho atual. Ambos os campos contêm o mesmo valor; `workspace.current_dir` é preferido para consistência com `workspace.project_dir`.                                                                                                                                                  |
| `workspace.project_dir`                                                          | Diretório onde o Claude Code foi iniciado, que pode diferir de `cwd` se o diretório de trabalho mudar durante uma sessão                                                                                                                                                                               |
| `workspace.added_dirs`                                                           | Diretórios adicionais adicionados via `/add-dir` ou `--add-dir`. Array vazio se nenhum foi adicionado                                                                                                                                                                                                  |
| `workspace.git_worktree`                                                         | Nome da git worktree quando o diretório atual está dentro de uma worktree vinculada criada com `git worktree add`. Ausente na worktree principal. Preenchido para qualquer git worktree, diferentemente de `worktree.*` que se aplica apenas a sessões `--worktree`                                    |
| `workspace.repo.host`, `workspace.repo.owner`, `workspace.repo.name`             | Identidade do repositório analisada a partir do remote `origin`, por exemplo `"github.com"`, `"anthropics"`, `"claude-code"`. Ausente fora de um repositório git ou quando nenhum remote `origin` está configurado                                                                                     |
| `cost.total_cost_usd`                                                            | Custo total estimado da sessão em USD, calculado no lado do cliente. Pode diferir de sua fatura real                                                                                                                                                                                                   |
| `cost.total_duration_ms`                                                         | Tempo total decorrido desde o início da sessão, em milissegundos                                                                                                                                                                                                                                       |
| `cost.total_api_duration_ms`                                                     | Tempo total gasto aguardando respostas de API em milissegundos                                                                                                                                                                                                                                         |
| `cost.total_lines_added`, `cost.total_lines_removed`                             | Linhas de código alteradas                                                                                                                                                                                                                                                                             |
| `context_window.total_input_tokens`, `context_window.total_output_tokens`        | Contagens de tokens atualmente na janela de contexto, da resposta de API mais recente. A entrada inclui leituras e escritas de cache. Antes da v2.1.132, estas eram totais cumulativos de sessão                                                                                                       |
| `context_window.context_window_size`                                             | Tamanho máximo da janela de contexto em tokens. 200000 por padrão, ou 1000000 para modelos com contexto estendido.                                                                                                                                                                                     |
| `context_window.used_percentage`                                                 | Porcentagem pré-calculada da janela de contexto usada                                                                                                                                                                                                                                                  |
| `context_window.remaining_percentage`                                            | Porcentagem pré-calculada da janela de contexto restante                                                                                                                                                                                                                                               |
| `context_window.current_usage`                                                   | Contagens de tokens da última chamada de API, descritas em [campos de janela de contexto](#context-window-fields)                                                                                                                                                                                      |
| `exceeds_200k_tokens`                                                            | Se a contagem total de tokens (tokens de entrada, cache e saída combinados) da resposta de API mais recente excede 200k. Este é um limite fixo independentemente do tamanho real da janela de contexto.                                                                                                |
| `effort.level`                                                                   | Nível de esforço de raciocínio atual (`low`, `medium`, `high`, `xhigh` ou `max`). Reflete o valor da sessão em tempo real, incluindo mudanças de `/effort` durante a sessão. Ultracode não é um nível distinto e relata como `xhigh`. Ausente quando o modelo atual não suporta o parâmetro de esforço |
| `thinking.enabled`                                                               | Se o pensamento estendido está habilitado para a sessão                                                                                                                                                                                                                                                |
| `rate_limits.five_hour.used_percentage`, `rate_limits.seven_day.used_percentage` | Porcentagem do limite de taxa de 5 horas ou 7 dias consumida, de 0 a 100                                                                                                                                                                                                                               |
| `rate_limits.five_hour.resets_at`, `rate_limits.seven_day.resets_at`             | Segundos de época Unix quando a janela de limite de taxa de 5 horas ou 7 dias é redefinida                                                                                                                                                                                                             |
| `session_id`                                                                     | Identificador único de sessão                                                                                                                                                                                                                                                                          |
| `session_name`                                                                   | Nome de sessão personalizado definido com a flag `--name` ou `/rename`. Ausente se nenhum nome personalizado foi definido                                                                                                                                                                              |
| `prompt_id`                                                                      | UUID identificando o prompt do usuário sendo processado no momento. Corresponde ao atributo [`prompt.id` em eventos OpenTelemetry](https://code.claude.com/docs/pt/monitoring-usage#event-correlation-attributes). Ausente até a primeira entrada do usuário. Requer Claude Code v2.1.196 ou posterior                             |
| `transcript_path`                                                                | Caminho para o arquivo de transcrição de conversa                                                                                                                                                                                                                                                      |
| `version`                                                                        | Versão do Claude Code                                                                                                                                                                                                                                                                                  |
| `output_style.name`                                                              | Nome do estilo de saída atual                                                                                                                                                                                                                                                                          |
| `vim.mode`                                                                       | Modo vim atual (`NORMAL`, `INSERT`, `VISUAL` ou `VISUAL LINE`) quando [modo vim](https://code.claude.com/docs/pt/interactive-mode#vim-editor-mode) está habilitado                                                                                                                                                                 |
| `agent.name`                                                                     | Nome do agente ao executar com a flag `--agent` ou configurações de agente configuradas                                                                                                                                                                                                                |
| `pr.number`, `pr.url`                                                            | Solicitação de pull aberta para o branch atual. Espelha o badge de PR na barra de status inferior. Ausente até que um PR seja encontrado, quando não em um repositório git, ou uma vez que o PR seja mesclado ou fechado                                                                               |
| `pr.review_state`                                                                | Status de revisão do PR aberto: `approved`, `pending`, `changes_requested` ou `draft`. Pode estar independentemente ausente mesmo quando `pr` está presente                                                                                                                                            |
| `worktree.name`                                                                  | Nome da worktree ativa. Presente apenas durante sessões `--worktree`                                                                                                                                                                                                                                   |
| `worktree.path`                                                                  | Caminho absoluto para o diretório da worktree                                                                                                                                                                                                                                                          |
| `worktree.branch`                                                                | Nome da ramificação git para a worktree (por exemplo, `"worktree-my-feature"`). Ausente para worktrees baseadas em hook                                                                                                                                                                                |
| `worktree.original_cwd`                                                          | O diretório em que o Claude estava antes de entrar na worktree                                                                                                                                                                                                                                         |
| `worktree.original_branch`                                                       | Ramificação git verificada antes de entrar na worktree. Ausente para worktrees baseadas em hook                                                                                                                                                                                                        |

<Accordion title="Esquema JSON completo">
  Seu comando de linha de status recebe esta estrutura JSON via stdin:

  ```json theme={null}
  {
    "cwd": "/current/working/directory",
    "session_id": "abc123...",
    "session_name": "my-session",
    "prompt_id": "550e8400-e29b-41d4-a716-446655440000",
    "transcript_path": "/path/to/transcript.jsonl",
    "model": {
      "id": "claude-opus-4-8",
      "display_name": "Opus"
    },
    "workspace": {
      "current_dir": "/current/working/directory",
      "project_dir": "/original/project/directory",
      "added_dirs": [],
      "git_worktree": "feature-xyz",
      "repo": {
        "host": "github.com",
        "owner": "anthropics",
        "name": "claude-code"
      }
    },
    "version": "2.1.90",
    "output_style": {
      "name": "default"
    },
    "cost": {
      "total_cost_usd": 0.01234,
      "total_duration_ms": 45000,
      "total_api_duration_ms": 2300,
      "total_lines_added": 156,
      "total_lines_removed": 23
    },
    "context_window": {
      "total_input_tokens": 15500,
      "total_output_tokens": 1200,
      "context_window_size": 200000,
      "used_percentage": 8,
      "remaining_percentage": 92,
      "current_usage": {
        "input_tokens": 8500,
        "output_tokens": 1200,
        "cache_creation_input_tokens": 5000,
        "cache_read_input_tokens": 2000
      }
    },
    "exceeds_200k_tokens": false,
    "effort": {
      "level": "high"
    },
    "thinking": {
      "enabled": true
    },
    "rate_limits": {
      "five_hour": {
        "used_percentage": 23.5,
        "resets_at": 1738425600
      },
      "seven_day": {
        "used_percentage": 41.2,
        "resets_at": 1738857600
      }
    },
    "vim": {
      "mode": "NORMAL"
    },
    "agent": {
      "name": "security-reviewer"
    },
    "pr": {
      "number": 1234,
      "url": "https://github.com/anthropics/claude-code/pull/1234",
      "review_state": "pending"
    },
    "worktree": {
      "name": "my-feature",
      "path": "/path/to/.claude/worktrees/my-feature",
      "branch": "worktree-my-feature",
      "original_cwd": "/path/to/project",
      "original_branch": "main"
    }
  }
  ```

  **Campos que podem estar ausentes** (não presentes em JSON):

  * `session_name`: aparece apenas quando um nome personalizado foi definido com `--name` ou `/rename`
  * `prompt_id`: aparece apenas após a primeira entrada do usuário
  * `workspace.git_worktree`: aparece apenas quando o diretório atual está dentro de uma git worktree vinculada
  * `workspace.repo`: aparece apenas dentro de um repositório git com um remote `origin` configurado
  * `effort`: aparece apenas quando o modelo atual suporta o parâmetro de esforço de raciocínio
  * `vim`: aparece apenas quando o modo vim está habilitado
  * `agent`: aparece apenas ao executar com a flag `--agent` ou configurações de agente configuradas
  * `pr`: aparece apenas enquanto um PR aberto é encontrado para o branch atual, e é removido uma vez que o PR seja mesclado ou fechado. `pr.review_state` pode estar independentemente ausente
  * `worktree`: aparece apenas durante sessões `--worktree`. Quando presente, `branch` e `original_branch` também podem estar ausentes para worktrees baseadas em hook
  * `rate_limits`: aparece apenas para assinantes Claude.ai (Pro/Max) após a primeira resposta de API na sessão. Cada janela (`five_hour`, `seven_day`) pode estar independentemente ausente. Use `jq -r '.rate_limits.five_hour.used_percentage // empty'` para lidar com ausência graciosamente.

  **Campos que podem ser `null`**:

  * `context_window.current_usage`: `null` antes da primeira chamada de API em uma sessão, e novamente após `/compact` até que a próxima chamada de API a repopule
  * `context_window.used_percentage`, `context_window.remaining_percentage`: podem ser `null` no início da sessão

  Trate campos ausentes com acesso condicional e valores nulos com padrões de fallback em seus scripts.
</Accordion>

<h3 id="context-window-fields">
  Campos de janela de contexto
</h3>

O objeto `context_window` descreve a janela de contexto em tempo real da resposta de API mais recente. A partir da v2.1.132, `total_input_tokens` e `total_output_tokens` refletem o uso de contexto atual, não totais cumulativos de sessão.

* **Totais combinados** (`total_input_tokens`, `total_output_tokens`): tokens atualmente na janela de contexto. `total_input_tokens` é a soma de `input_tokens`, `cache_creation_input_tokens` e `cache_read_input_tokens`; `total_output_tokens` são os tokens de saída da resposta mais recente. Ambos são `0` antes da primeira resposta de API.
* **Uso por componente** (`current_usage`): as mesmas contagens de tokens divididas por categoria. Use isto quando você precisar de acertos de cache separados da entrada fresca.

O objeto `current_usage` contém:

* `input_tokens`: tokens de entrada no contexto atual
* `output_tokens`: tokens de saída gerados
* `cache_creation_input_tokens`: tokens escritos no cache
* `cache_read_input_tokens`: tokens lidos do cache

Para o que os campos de cache significam e como são cobrados, consulte [verificar desempenho do cache](https://code.claude.com/docs/pt/prompt-caching#check-cache-performance).

O campo `used_percentage` é calculado apenas a partir de tokens de entrada: `input_tokens + cache_creation_input_tokens + cache_read_input_tokens`. Ele não inclui `output_tokens`.

Se você calcular a porcentagem de contexto manualmente a partir de `current_usage`, use a mesma fórmula apenas de entrada para corresponder a `used_percentage`.

O objeto `current_usage` é `null` antes da primeira chamada de API em uma sessão, e novamente imediatamente após `/compact` até que a próxima chamada de API a repopule.

<h2 id="examples">
  Exemplos
</h2>

Estes exemplos mostram padrões comuns de linha de status. Para usar qualquer exemplo:

1. Salve o script em um arquivo como `~/.claude/statusline.sh` (ou `.py`/`.js`)
2. Torne-o executável: `chmod +x ~/.claude/statusline.sh`
3. Adicione o caminho às suas [configurações](#manually-configure-a-status-line)

Os exemplos de Bash usam [`jq`](https://jqlang.github.io/jq/) para analisar JSON. Python e Node.js têm análise JSON integrada.

<h3 id="context-window-usage">
  Uso da janela de contexto
</h3>

Exiba o modelo atual e o uso da janela de contexto com uma barra de progresso visual. Cada script lê JSON de stdin, extrai o campo `used_percentage` e constrói uma barra de 10 caracteres onde blocos preenchidos (▓) representam o uso:

<Frame>
  <img src="https://mintcdn.com/claude-code/nibzesLaJVh4ydOq/images/statusline-context-window-usage.png?fit=max&auto=format&n=nibzesLaJVh4ydOq&q=85&s=15b58ab3602f036939145dde3165c6f7" alt="Uma linha de status mostrando nome do modelo e uma barra de progresso com porcentagem" width="448" height="152" data-path="images/statusline-context-window-usage.png" />
</Frame>

<CodeGroup>
  ```bash Bash theme={null}
  #!/bin/bash
  # Read all of stdin into a variable
  input=$(cat)

  # Extract fields with jq, "// 0" provides fallback for null
  MODEL=$(echo "$input" | jq -r '.model.display_name')
  PCT=$(echo "$input" | jq -r '.context_window.used_percentage // 0' | cut -d. -f1)

  # Build progress bar: printf -v creates a run of spaces, then
  # ${var// /▓} replaces each space with a block character
  BAR_WIDTH=10
  FILLED=$((PCT * BAR_WIDTH / 100))
  EMPTY=$((BAR_WIDTH - FILLED))
  BAR=""
  [ "$FILLED" -gt 0 ] && printf -v FILL "%${FILLED}s" && BAR="${FILL// /▓}"
  [ "$EMPTY" -gt 0 ] && printf -v PAD "%${EMPTY}s" && BAR="${BAR}${PAD// /░}"

  echo "[$MODEL] $BAR $PCT%"
  ```

  ```python Python theme={null}
  #!/usr/bin/env python3
  import json, sys

  # json.load reads and parses stdin in one step
  data = json.load(sys.stdin)
  model = data['model']['display_name']
  # "or 0" handles null values
  pct = int(data.get('context_window', {}).get('used_percentage', 0) or 0)

  # String multiplication builds the bar
  filled = pct * 10 // 100
  bar = '▓' * filled + '░' * (10 - filled)

  print(f"[{model}] {bar} {pct}%")
  ```

  ```javascript Node.js theme={null}
  #!/usr/bin/env node
  // Node.js reads stdin asynchronously with events
  let input = '';
  process.stdin.on('data', chunk => input += chunk);
  process.stdin.on('end', () => {
      const data = JSON.parse(input);
      const model = data.model.display_name;
      // Optional chaining (?.) safely handles null fields
      const pct = Math.floor(data.context_window?.used_percentage || 0);

      // String.repeat() builds the bar
      const filled = Math.floor(pct * 10 / 100);
      const bar = '▓'.repeat(filled) + '░'.repeat(10 - filled);

      console.log(`[${model}] ${bar} ${pct}%`);
  });
  ```
</CodeGroup>

<h3 id="git-status-with-colors">
  Status do git com cores
</h3>

Mostre a ramificação git com indicadores codificados por cores para arquivos preparados e modificados. Este script usa [códigos de escape ANSI](https://en.wikipedia.org/wiki/ANSI_escape_code#Colors) para cores de terminal: `\033[32m` é verde, `\033[33m` é amarelo e `\033[0m` redefine para padrão.

<Frame>
  <img src="https://mintcdn.com/claude-code/nibzesLaJVh4ydOq/images/statusline-git-context.png?fit=max&auto=format&n=nibzesLaJVh4ydOq&q=85&s=e656f34f90d1d9a1d0e220988914345f" alt="Uma linha de status mostrando modelo, diretório, ramificação git e indicadores coloridos para arquivos preparados e modificados" width="742" height="178" data-path="images/statusline-git-context.png" />
</Frame>

Cada script verifica se o diretório atual é um repositório git, conta arquivos preparados e modificados e exibe indicadores codificados por cores:

<CodeGroup>
  ```bash Bash theme={null}
  #!/bin/bash
  input=$(cat)

  MODEL=$(echo "$input" | jq -r '.model.display_name')
  DIR=$(echo "$input" | jq -r '.workspace.current_dir')

  GREEN='\033[32m'
  YELLOW='\033[33m'
  RESET='\033[0m'

  if git rev-parse --git-dir > /dev/null 2>&1; then
      BRANCH=$(git branch --show-current 2>/dev/null)
      STAGED=$(git diff --cached --numstat 2>/dev/null | wc -l | tr -d ' ')
      MODIFIED=$(git diff --numstat 2>/dev/null | wc -l | tr -d ' ')

      GIT_STATUS=""
      [ "$STAGED" -gt 0 ] && GIT_STATUS="${GREEN}+${STAGED}${RESET}"
      [ "$MODIFIED" -gt 0 ] && GIT_STATUS="${GIT_STATUS}${YELLOW}~${MODIFIED}${RESET}"

      echo -e "[$MODEL] 📁 ${DIR##*/} | 🌿 $BRANCH $GIT_STATUS"
  else
      echo "[$MODEL] 📁 ${DIR##*/}"
  fi
  ```

  ```python Python theme={null}
  #!/usr/bin/env python3
  import json, sys, subprocess, os

  data = json.load(sys.stdin)
  model = data['model']['display_name']
  directory = os.path.basename(data['workspace']['current_dir'])

  GREEN, YELLOW, RESET = '\033[32m', '\033[33m', '\033[0m'

  try:
      subprocess.check_output(['git', 'rev-parse', '--git-dir'], stderr=subprocess.DEVNULL)
      branch = subprocess.check_output(['git', 'branch', '--show-current'], text=True).strip()
      staged_output = subprocess.check_output(['git', 'diff', '--cached', '--numstat'], text=True).strip()
      modified_output = subprocess.check_output(['git', 'diff', '--numstat'], text=True).strip()
      staged = len(staged_output.split('\n')) if staged_output else 0
      modified = len(modified_output.split('\n')) if modified_output else 0

      git_status = f"{GREEN}+{staged}{RESET}" if staged else ""
      git_status += f"{YELLOW}~{modified}{RESET}" if modified else ""

      print(f"[{model}] 📁 {directory} | 🌿 {branch} {git_status}")
  except:
      print(f"[{model}] 📁 {directory}")
  ```

  ```javascript Node.js theme={null}
  #!/usr/bin/env node
  const { execSync } = require('child_process');
  const path = require('path');

  let input = '';
  process.stdin.on('data', chunk => input += chunk);
  process.stdin.on('end', () => {
      const data = JSON.parse(input);
      const model = data.model.display_name;
      const dir = path.basename(data.workspace.current_dir);

      const GREEN = '\x1b[32m', YELLOW = '\x1b[33m', RESET = '\x1b[0m';

      try {
          execSync('git rev-parse --git-dir', { stdio: 'ignore' });
          const branch = execSync('git branch --show-current', { encoding: 'utf8' }).trim();
          const staged = execSync('git diff --cached --numstat', { encoding: 'utf8' }).trim().split('\n').filter(Boolean).length;
          const modified = execSync('git diff --numstat', { encoding: 'utf8' }).trim().split('\n').filter(Boolean).length;

          let gitStatus = staged ? `${GREEN}+${staged}${RESET}` : '';
          gitStatus += modified ? `${YELLOW}~${modified}${RESET}` : '';

          console.log(`[${model}] 📁 ${dir} | 🌿 ${branch} ${gitStatus}`);
      } catch {
          console.log(`[${model}] 📁 ${dir}`);
      }
  });
  ```
</CodeGroup>

<h3 id="cost-and-duration-tracking">
  Rastreamento de custo e duração
</h3>

Rastreie os custos de API e o tempo decorrido da sua sessão. O campo `cost.total_cost_usd` acumula o custo estimado de todas as chamadas de API na sessão atual. O campo `cost.total_duration_ms` mede o tempo total decorrido desde o início da sessão, enquanto `cost.total_api_duration_ms` rastreia apenas o tempo gasto aguardando respostas de API.

Cada script formata o custo como moeda e converte milissegundos em minutos e segundos:

<Frame>
  <img src="https://mintcdn.com/claude-code/nibzesLaJVh4ydOq/images/statusline-cost-tracking.png?fit=max&auto=format&n=nibzesLaJVh4ydOq&q=85&s=e3444a51fe6f3440c134bd5f1f08ad29" alt="Uma linha de status mostrando nome do modelo, custo da sessão e duração" width="588" height="180" data-path="images/statusline-cost-tracking.png" />
</Frame>

<CodeGroup>
  ```bash Bash theme={null}
  #!/bin/bash
  input=$(cat)

  MODEL=$(echo "$input" | jq -r '.model.display_name')
  COST=$(echo "$input" | jq -r '.cost.total_cost_usd // 0')
  DURATION_MS=$(echo "$input" | jq -r '.cost.total_duration_ms // 0')

  COST_FMT=$(printf '$%.2f' "$COST")
  DURATION_SEC=$((DURATION_MS / 1000))
  MINS=$((DURATION_SEC / 60))
  SECS=$((DURATION_SEC % 60))

  echo "[$MODEL] 💰 $COST_FMT | ⏱️ ${MINS}m ${SECS}s"
  ```

  ```python Python theme={null}
  #!/usr/bin/env python3
  import json, sys

  data = json.load(sys.stdin)
  model = data['model']['display_name']
  cost = data.get('cost', {}).get('total_cost_usd', 0) or 0
  duration_ms = data.get('cost', {}).get('total_duration_ms', 0) or 0

  duration_sec = duration_ms // 1000
  mins, secs = duration_sec // 60, duration_sec % 60

  print(f"[{model}] 💰 ${cost:.2f} | ⏱️ {mins}m {secs}s")
  ```

  ```javascript Node.js theme={null}
  #!/usr/bin/env node
  let input = '';
  process.stdin.on('data', chunk => input += chunk);
  process.stdin.on('end', () => {
      const data = JSON.parse(input);
      const model = data.model.display_name;
      const cost = data.cost?.total_cost_usd || 0;
      const durationMs = data.cost?.total_duration_ms || 0;

      const durationSec = Math.floor(durationMs / 1000);
      const mins = Math.floor(durationSec / 60);
      const secs = durationSec % 60;

      console.log(`[${model}] 💰 $${cost.toFixed(2)} | ⏱️ ${mins}m ${secs}s`);
  });
  ```
</CodeGroup>

<h3 id="display-multiple-lines">
  Exibir múltiplas linhas
</h3>

Seu script pode exibir múltiplas linhas para criar uma exibição mais rica. Cada instrução `echo` produz uma linha separada na área de status.

<Frame>
  <img src="https://mintcdn.com/claude-code/nibzesLaJVh4ydOq/images/statusline-multiline.png?fit=max&auto=format&n=nibzesLaJVh4ydOq&q=85&s=60f11387658acc9ff75158ae85f2ac87" alt="Uma linha de status de múltiplas linhas mostrando nome do modelo, diretório, ramificação git na primeira linha, e uma barra de progresso de uso de contexto com custo e duração na segunda linha" width="776" height="212" data-path="images/statusline-multiline.png" />
</Frame>

Este exemplo combina várias técnicas: cores baseadas em limite (verde abaixo de 70%, amarelo 70-89%, vermelho 90%+), uma barra de progresso e informações de ramificação git. Cada instrução `print` ou `echo` cria uma linha separada:

<CodeGroup>
  ```bash Bash theme={null}
  #!/bin/bash
  input=$(cat)

  MODEL=$(echo "$input" | jq -r '.model.display_name')
  DIR=$(echo "$input" | jq -r '.workspace.current_dir')
  COST=$(echo "$input" | jq -r '.cost.total_cost_usd // 0')
  PCT=$(echo "$input" | jq -r '.context_window.used_percentage // 0' | cut -d. -f1)
  DURATION_MS=$(echo "$input" | jq -r '.cost.total_duration_ms // 0')

  CYAN='\033[36m'; GREEN='\033[32m'; YELLOW='\033[33m'; RED='\033[31m'; RESET='\033[0m'

  # Pick bar color based on context usage
  if [ "$PCT" -ge 90 ]; then BAR_COLOR="$RED"
  elif [ "$PCT" -ge 70 ]; then BAR_COLOR="$YELLOW"
  else BAR_COLOR="$GREEN"; fi

  FILLED=$((PCT / 10)); EMPTY=$((10 - FILLED))
  printf -v FILL "%${FILLED}s"; printf -v PAD "%${EMPTY}s"
  BAR="${FILL// /█}${PAD// /░}"

  MINS=$((DURATION_MS / 60000)); SECS=$(((DURATION_MS % 60000) / 1000))

  BRANCH=""
  git rev-parse --git-dir > /dev/null 2>&1 && BRANCH=" | 🌿 $(git branch --show-current 2>/dev/null)"

  echo -e "${CYAN}[$MODEL]${RESET} 📁 ${DIR##*/}$BRANCH"
  COST_FMT=$(printf '$%.2f' "$COST")
  echo -e "${BAR_COLOR}${BAR}${RESET} ${PCT}% | ${YELLOW}${COST_FMT}${RESET} | ⏱️ ${MINS}m ${SECS}s"
  ```

  ```python Python theme={null}
  #!/usr/bin/env python3
  import json, sys, subprocess, os

  data = json.load(sys.stdin)
  model = data['model']['display_name']
  directory = os.path.basename(data['workspace']['current_dir'])
  cost = data.get('cost', {}).get('total_cost_usd', 0) or 0
  pct = int(data.get('context_window', {}).get('used_percentage', 0) or 0)
  duration_ms = data.get('cost', {}).get('total_duration_ms', 0) or 0

  CYAN, GREEN, YELLOW, RED, RESET = '\033[36m', '\033[32m', '\033[33m', '\033[31m', '\033[0m'

  bar_color = RED if pct >= 90 else YELLOW if pct >= 70 else GREEN
  filled = pct // 10
  bar = '█' * filled + '░' * (10 - filled)

  mins, secs = duration_ms // 60000, (duration_ms % 60000) // 1000

  try:
      branch = subprocess.check_output(['git', 'branch', '--show-current'], text=True, stderr=subprocess.DEVNULL).strip()
      branch = f" | 🌿 {branch}" if branch else ""
  except:
      branch = ""

  print(f"{CYAN}[{model}]{RESET} 📁 {directory}{branch}")
  print(f"{bar_color}{bar}{RESET} {pct}% | {YELLOW}${cost:.2f}{RESET} | ⏱️ {mins}m {secs}s")
  ```

  ```javascript Node.js theme={null}
  #!/usr/bin/env node
  const { execSync } = require('child_process');
  const path = require('path');

  let input = '';
  process.stdin.on('data', chunk => input += chunk);
  process.stdin.on('end', () => {
      const data = JSON.parse(input);
      const model = data.model.display_name;
      const dir = path.basename(data.workspace.current_dir);
      const cost = data.cost?.total_cost_usd || 0;
      const pct = Math.floor(data.context_window?.used_percentage || 0);
      const durationMs = data.cost?.total_duration_ms || 0;

      const CYAN = '\x1b[36m', GREEN = '\x1b[32m', YELLOW = '\x1b[33m', RED = '\x1b[31m', RESET = '\x1b[0m';

      const barColor = pct >= 90 ? RED : pct >= 70 ? YELLOW : GREEN;
      const filled = Math.floor(pct / 10);
      const bar = '█'.repeat(filled) + '░'.repeat(10 - filled);

      const mins = Math.floor(durationMs / 60000);
      const secs = Math.floor((durationMs % 60000) / 1000);

      let branch = '';
      try {
          branch = execSync('git branch --show-current', { encoding: 'utf8', stdio: ['pipe', 'pipe', 'ignore'] }).trim();
          branch = branch ? ` | 🌿 ${branch}` : '';
      } catch {}

      console.log(`${CYAN}[${model}]${RESET} 📁 ${dir}${branch}`);
      console.log(`${barColor}${bar}${RESET} ${pct}% | ${YELLOW}$${cost.toFixed(2)}${RESET} | ⏱️ ${mins}m ${secs}s`);
  });
  ```
</CodeGroup>

<h3 id="clickable-links">
  Links clicáveis
</h3>

Este exemplo cria um link clicável para seu repositório GitHub. Ele lê a URL remota do git, converte o formato SSH para HTTPS com `sed` e envolve o nome do repositório em códigos de escape OSC 8. Mantenha Cmd (macOS) ou Ctrl (Windows/Linux) pressionado e clique para abrir o link em seu navegador.

<Frame>
  <img src="https://mintcdn.com/claude-code/nibzesLaJVh4ydOq/images/statusline-links.png?fit=max&auto=format&n=nibzesLaJVh4ydOq&q=85&s=4bcc6e7deb7cf52f41ab85a219b52661" alt="Uma linha de status mostrando um link clicável para um repositório GitHub" width="726" height="198" data-path="images/statusline-links.png" />
</Frame>

Cada script obtém a URL remota do git, converte o formato SSH para HTTPS e envolve o nome do repositório em códigos de escape OSC 8. A versão Bash usa `printf '%b'` que interpreta escapes de barra invertida de forma mais confiável que `echo -e` em diferentes shells:

<CodeGroup>
  ```bash Bash theme={null}
  #!/bin/bash
  input=$(cat)

  MODEL=$(echo "$input" | jq -r '.model.display_name')

  # Convert git SSH URL to HTTPS
  REMOTE=$(git remote get-url origin 2>/dev/null | sed 's/git@github.com:/https:\/\/github.com\//' | sed 's/\.git$//')

  if [ -n "$REMOTE" ]; then
      REPO_NAME=$(basename "$REMOTE")
      # OSC 8 format: \e]8;;URL\a then TEXT then \e]8;;\a
      # printf %b interprets escape sequences reliably across shells
      printf '%b' "[$MODEL] 🔗 \e]8;;${REMOTE}\a${REPO_NAME}\e]8;;\a\n"
  else
      echo "[$MODEL]"
  fi
  ```

  ```python Python theme={null}
  #!/usr/bin/env python3
  import json, sys, subprocess, re, os

  data = json.load(sys.stdin)
  model = data['model']['display_name']

  # Get git remote URL
  try:
      remote = subprocess.check_output(
          ['git', 'remote', 'get-url', 'origin'],
          stderr=subprocess.DEVNULL, text=True
      ).strip()
      # Convert SSH to HTTPS format
      remote = re.sub(r'^git@github\.com:', 'https://github.com/', remote)
      remote = re.sub(r'\.git$', '', remote)
      repo_name = os.path.basename(remote)
      # OSC 8 escape sequences
      link = f"\033]8;;{remote}\a{repo_name}\033]8;;\a"
      print(f"[{model}] 🔗 {link}")
  except:
      print(f"[{model}]")
  ```

  ```javascript Node.js theme={null}
  #!/usr/bin/env node
  const { execSync } = require('child_process');
  const path = require('path');

  let input = '';
  process.stdin.on('data', chunk => input += chunk);
  process.stdin.on('end', () => {
      const data = JSON.parse(input);
      const model = data.model.display_name;

      try {
          let remote = execSync('git remote get-url origin', { encoding: 'utf8', stdio: ['pipe', 'pipe', 'ignore'] }).trim();
          // Convert SSH to HTTPS format
          remote = remote.replace(/^git@github\.com:/, 'https://github.com/').replace(/\.git$/, '');
          const repoName = path.basename(remote);
          // OSC 8 escape sequences
          const link = `\x1b]8;;${remote}\x07${repoName}\x1b]8;;\x07`;
          console.log(`[${model}] 🔗 ${link}`);
      } catch {
          console.log(`[${model}]`);
      }
  });
  ```
</CodeGroup>

<h3 id="rate-limit-usage">
  Uso de limite de taxa
</h3>

Exiba o uso do limite de taxa de assinatura Claude.ai na linha de status. O objeto `rate_limits` contém `five_hour` (janela móvel de 5 horas) e `seven_day` (janelas semanais). Cada janela fornece `used_percentage` (0-100) e `resets_at` (segundos de época Unix quando a janela é redefinida).

Este campo está presente apenas para assinantes Claude.ai (Pro/Max) após a primeira resposta de API. Cada script trata o campo ausente graciosamente:

<CodeGroup>
  ```bash Bash theme={null}
  #!/bin/bash
  input=$(cat)

  MODEL=$(echo "$input" | jq -r '.model.display_name')
  # "// empty" produces no output when rate_limits is absent
  FIVE_H=$(echo "$input" | jq -r '.rate_limits.five_hour.used_percentage // empty')
  WEEK=$(echo "$input" | jq -r '.rate_limits.seven_day.used_percentage // empty')

  LIMITS=""
  [ -n "$FIVE_H" ] && LIMITS="5h: $(printf '%.0f' "$FIVE_H")%"
  [ -n "$WEEK" ] && LIMITS="${LIMITS:+$LIMITS }7d: $(printf '%.0f' "$WEEK")%"

  [ -n "$LIMITS" ] && echo "[$MODEL] | $LIMITS" || echo "[$MODEL]"
  ```

  ```python Python theme={null}
  #!/usr/bin/env python3
  import json, sys

  data = json.load(sys.stdin)
  model = data['model']['display_name']

  parts = []
  rate = data.get('rate_limits', {})
  five_h = rate.get('five_hour', {}).get('used_percentage')
  week = rate.get('seven_day', {}).get('used_percentage')

  if five_h is not None:
      parts.append(f"5h: {five_h:.0f}%")
  if week is not None:
      parts.append(f"7d: {week:.0f}%")

  if parts:
      print(f"[{model}] | {' '.join(parts)}")
  else:
      print(f"[{model}]")
  ```

  ```javascript Node.js theme={null}
  #!/usr/bin/env node
  let input = '';
  process.stdin.on('data', chunk => input += chunk);
  process.stdin.on('end', () => {
      const data = JSON.parse(input);
      const model = data.model.display_name;

      const parts = [];
      const fiveH = data.rate_limits?.five_hour?.used_percentage;
      const week = data.rate_limits?.seven_day?.used_percentage;

      if (fiveH != null) parts.push(`5h: ${Math.round(fiveH)}%`);
      if (week != null) parts.push(`7d: ${Math.round(week)}%`);

      console.log(parts.length ? `[${model}] | ${parts.join(' ')}` : `[${model}]`);
  });
  ```
</CodeGroup>

<h3 id="cache-expensive-operations">
  Cache de operações caras
</h3>

Seu script de linha de status é executado frequentemente durante sessões ativas. Comandos como `git status` ou `git diff` podem ser lentos, especialmente em repositórios grandes. Este exemplo armazena em cache informações do git em um arquivo temporário e apenas as atualiza a cada 5 segundos.

O nome do arquivo de cache precisa ser estável em invocações de linha de status dentro de uma sessão, mas único em sessões para que sessões simultâneas em repositórios diferentes não leiam o estado git em cache uma da outra. Identificadores baseados em processo como `$$`, `os.getpid()` ou `process.pid` mudam a cada invocação e derrotam o cache. Use o `session_id` da entrada JSON em vez disso: é estável para a vida útil de uma sessão e único por sessão.

Cada script verifica se o arquivo de cache está ausente ou mais antigo que 5 segundos antes de executar comandos git:

<CodeGroup>
  ```bash Bash theme={null}
  #!/bin/bash
  input=$(cat)

  MODEL=$(echo "$input" | jq -r '.model.display_name')
  DIR=$(echo "$input" | jq -r '.workspace.current_dir')
  SESSION_ID=$(echo "$input" | jq -r '.session_id')

  CACHE_FILE="/tmp/statusline-git-cache-$SESSION_ID"
  CACHE_MAX_AGE=5  # seconds

  cache_is_stale() {
      [ ! -f "$CACHE_FILE" ] || \
      # stat -f %m is macOS, stat -c %Y is Linux
      [ $(($(date +%s) - $(stat -f %m "$CACHE_FILE" 2>/dev/null || stat -c %Y "$CACHE_FILE" 2>/dev/null || echo 0))) -gt $CACHE_MAX_AGE ]
  }

  if cache_is_stale; then
      if git rev-parse --git-dir > /dev/null 2>&1; then
          BRANCH=$(git branch --show-current 2>/dev/null)
          STAGED=$(git diff --cached --numstat 2>/dev/null | wc -l | tr -d ' ')
          MODIFIED=$(git diff --numstat 2>/dev/null | wc -l | tr -d ' ')
          echo "$BRANCH|$STAGED|$MODIFIED" > "$CACHE_FILE"
      else
          echo "||" > "$CACHE_FILE"
      fi
  fi

  IFS='|' read -r BRANCH STAGED MODIFIED < "$CACHE_FILE"

  if [ -n "$BRANCH" ]; then
      echo "[$MODEL] 📁 ${DIR##*/} | 🌿 $BRANCH +$STAGED ~$MODIFIED"
  else
      echo "[$MODEL] 📁 ${DIR##*/}"
  fi
  ```

  ```python Python theme={null}
  #!/usr/bin/env python3
  import json, sys, subprocess, os, time

  data = json.load(sys.stdin)
  model = data['model']['display_name']
  directory = os.path.basename(data['workspace']['current_dir'])
  session_id = data['session_id']

  CACHE_FILE = f"/tmp/statusline-git-cache-{session_id}"
  CACHE_MAX_AGE = 5  # seconds

  def cache_is_stale():
      if not os.path.exists(CACHE_FILE):
          return True
      return time.time() - os.path.getmtime(CACHE_FILE) > CACHE_MAX_AGE

  if cache_is_stale():
      try:
          subprocess.check_output(['git', 'rev-parse', '--git-dir'], stderr=subprocess.DEVNULL)
          branch = subprocess.check_output(['git', 'branch', '--show-current'], text=True).strip()
          staged = subprocess.check_output(['git', 'diff', '--cached', '--numstat'], text=True).strip()
          modified = subprocess.check_output(['git', 'diff', '--numstat'], text=True).strip()
          staged_count = len(staged.split('\n')) if staged else 0
          modified_count = len(modified.split('\n')) if modified else 0
          with open(CACHE_FILE, 'w') as f:
              f.write(f"{branch}|{staged_count}|{modified_count}")
      except:
          with open(CACHE_FILE, 'w') as f:
              f.write("||")

  with open(CACHE_FILE) as f:
      branch, staged, modified = f.read().strip().split('|')

  if branch:
      print(f"[{model}] 📁 {directory} | 🌿 {branch} +{staged} ~{modified}")
  else:
      print(f"[{model}] 📁 {directory}")
  ```

  ```javascript Node.js theme={null}
  #!/usr/bin/env node
  const { execSync } = require('child_process');
  const fs = require('fs');
  const path = require('path');

  let input = '';
  process.stdin.on('data', chunk => input += chunk);
  process.stdin.on('end', () => {
      const data = JSON.parse(input);
      const model = data.model.display_name;
      const dir = path.basename(data.workspace.current_dir);
      const sessionId = data.session_id;

      const CACHE_FILE = `/tmp/statusline-git-cache-${sessionId}`;
      const CACHE_MAX_AGE = 5; // seconds

      const cacheIsStale = () => {
          if (!fs.existsSync(CACHE_FILE)) return true;
          return (Date.now() / 1000) - fs.statSync(CACHE_FILE).mtimeMs / 1000 > CACHE_MAX_AGE;
      };

      if (cacheIsStale()) {
          try {
              execSync('git rev-parse --git-dir', { stdio: 'ignore' });
              const branch = execSync('git branch --show-current', { encoding: 'utf8' }).trim();
              const staged = execSync('git diff --cached --numstat', { encoding: 'utf8' }).trim().split('\n').filter(Boolean).length;
              const modified = execSync('git diff --numstat', { encoding: 'utf8' }).trim().split('\n').filter(Boolean).length;
              fs.writeFileSync(CACHE_FILE, `${branch}|${staged}|${modified}`);
          } catch {
              fs.writeFileSync(CACHE_FILE, '||');
          }
      }

      const [branch, staged, modified] = fs.readFileSync(CACHE_FILE, 'utf8').trim().split('|');

      if (branch) {
          console.log(`[${model}] 📁 ${dir} | 🌿 ${branch} +${staged} ~${modified}`);
      } else {
          console.log(`[${model}] 📁 ${dir}`);
      }
  });
  ```
</CodeGroup>

<h3 id="windows-configuration">
  Configuração do Windows
</h3>

No Windows, o Claude Code executa comandos de linha de status através do Git Bash quando o Git Bash está instalado, ou através do PowerShell quando o Git Bash está ausente.

O Git Bash trata barras invertidas sem aspas como caracteres de escape, portanto um caminho no estilo Windows como `C:\Users\username\script.mjs` chega ao executor de script com seus separadores removidos e o comando falha sem um erro visível. Escreva caminhos de arquivo na string `command` com barras normais, conforme mostrado nos exemplos abaixo. O atalho `~` também funciona e se expande para seu diretório inicial do Windows.

Para executar um script PowerShell como sua linha de status, invoque-o via `powershell`. Isso funciona se o Claude Code rotear o comando através do Git Bash ou PowerShell:

<CodeGroup>
  ```json settings.json theme={null}
  {
    "statusLine": {
      "type": "command",
      "command": "powershell -NoProfile -File C:/Users/username/.claude/statusline.ps1"
    }
  }
  ```

  ```powershell statusline.ps1 theme={null}
  $input_json = $input | Out-String | ConvertFrom-Json
  $cwd = $input_json.cwd
  $model = $input_json.model.display_name
  $used = $input_json.context_window.used_percentage
  $dirname = Split-Path $cwd -Leaf

  if ($used) {
      Write-Host "$dirname [$model] ctx: $used%"
  } else {
      Write-Host "$dirname [$model]"
  }
  ```
</CodeGroup>

Ou execute um script Bash diretamente quando o Git Bash está instalado:

<CodeGroup>
  ```json settings.json theme={null}
  {
    "statusLine": {
      "type": "command",
      "command": "~/.claude/statusline.sh"
    }
  }
  ```

  ```bash statusline.sh theme={null}
  #!/usr/bin/env bash
  input=$(cat)
  cwd=$(echo "$input" | grep -o '"cwd":"[^"]*"' | cut -d'"' -f4)
  model=$(echo "$input" | grep -o '"display_name":"[^"]*"' | cut -d'"' -f4)
  dirname="${cwd##*[/\\]}"
  echo "$dirname [$model]"
  ```
</CodeGroup>

<h2 id="subagent-status-lines">
  Linhas de status de subagente
</h2>

A configuração `subagentStatusLine` renderiza um corpo de linha personalizado para cada [subagente](https://code.claude.com/docs/pt/sub-agents) mostrado no painel de agente abaixo do prompt. Use-a para substituir a linha padrão `name · description · token count` pela sua própria formatação.

```json theme={null}
{
  "subagentStatusLine": {
    "type": "command",
    "command": "~/.claude/subagent-statusline.sh"
  }
}
```

O comando é executado uma vez por tick de atualização com todas as linhas de subagente visíveis passadas como um único objeto JSON em stdin. A entrada inclui os [campos de hook base](https://code.claude.com/docs/pt/hooks#common-input-fields), um campo `columns` com a largura de linha utilizável e um array `tasks`. Cada tarefa tem `id`, `name`, `type`, `status`, `description`, `label`, `startTime`, `model`, `contextWindowSize`, `tokenCount`, `tokenSamples` e `cwd`.

O campo `model` por tarefa é o ID do modelo resolvido em que a tarefa é executada. `contextWindowSize` é a janela de contexto desse modelo em tokens, calculada da mesma forma que a `context_window.context_window_size` da linha de status principal, para que você possa renderizar uma porcentagem por linha a partir de `tokenCount`. Ambos os campos exigem Claude Code v2.1.205 ou posterior e são omitidos para uma tarefa cujo modelo ainda não foi resolvido.

Escreva uma linha JSON para stdout por linha que você queira substituir, na forma `{"id": "<task id>", "content": "<row body>"}`. A string `content` é renderizada como está, incluindo cores ANSI e hiperlinks OSC 8. Omita o `id` de uma tarefa para manter a renderização padrão para essa linha; emita uma string `content` vazia para ocultá-la.

Os mesmos portões de confiança e `disableAllHooks` que se aplicam a `statusLine` se aplicam aqui. Plugins podem enviar um `subagentStatusLine` padrão em seu [`settings.json`](https://code.claude.com/docs/pt/plugins-reference#standard-plugin-layout).

<h2 id="tips">
  Dicas
</h2>

* **Teste com entrada simulada**: `echo '{"model":{"display_name":"Opus"},"workspace":{"current_dir":"/home/user/project"},"context_window":{"used_percentage":25},"session_id":"test-session-abc"}' | ./statusline.sh`
* **Mantenha a saída curta**: a barra de status tem largura limitada, então saída longa pode ser truncada ou quebrada de forma estranha
* **Cache de operações lentas**: seu script é executado frequentemente durante sessões ativas, então comandos como `git status` podem causar atraso. Consulte o [exemplo de cache](#cache-expensive-operations) para saber como lidar com isso.

Projetos comunitários como [ccstatusline](https://github.com/sirmalloc/ccstatusline) e [starship-claude](https://github.com/martinemde/starship-claude) fornecem configurações pré-construídas com temas e recursos adicionais.

<h2 id="troubleshooting">
  Solução de problemas
</h2>

**Linha de status não aparecendo**

* Verifique se seu script é executável: `chmod +x ~/.claude/statusline.sh`
* Verifique se seu script produz saída para stdout, não stderr
* Execute seu script manualmente para verificar se produz saída
* No Windows com Git Bash instalado, barras invertidas no caminho `command` provavelmente estão sendo consumidas como caracteres de escape antes do script ser executado. Use barras normais no caminho. Veja [Configuração do Windows](#windows-configuration).
* Se `disableAllHooks` estiver definido como `true` em suas configurações, a linha de status também será desabilitada. Remova esta configuração ou defina-a como `false` para reabilitar.
* Execute `claude --debug` para registrar o código de saída e stderr da primeira invocação de linha de status em uma sessão
* Peça ao Claude para ler seu arquivo de configurações e executar o comando `statusLine` diretamente para descobrir erros

**Linha de status mostra `--` ou valores vazios**

* Os campos podem ser `null` antes da primeira resposta de API ser concluída
* Trate valores nulos em seu script com fallbacks como `// 0` em jq
* Reinicie o Claude Code se os valores permanecerem vazios após várias mensagens

**Porcentagem de contexto mostra valores inesperados**

* Use `used_percentage` para o estado de contexto mais simples e preciso
* A porcentagem de contexto pode diferir da saída `/context` devido a quando cada uma é calculada

**Links OSC 8 não clicáveis**

* Verifique se seu terminal suporta hiperlinks OSC 8 (iTerm2, Kitty, WezTerm)

* Terminal.app não suporta links clicáveis

* Se o texto do link aparecer mas não for clicável, o Claude Code pode não ter detectado suporte a hiperlink em seu terminal. Isto afeta comumente Windows Terminal e outros emuladores não na lista de detecção automática. Defina a variável de ambiente `FORCE_HYPERLINK` para substituir a detecção antes de iniciar o Claude Code:

  ```bash theme={null}
  FORCE_HYPERLINK=1 claude
  ```

  No PowerShell, defina a variável na sessão atual primeiro:

  ```powershell theme={null}
  $env:FORCE_HYPERLINK = "1"; claude
  ```

* Sessões SSH e tmux podem remover sequências OSC dependendo da configuração

* Se sequências de escape aparecerem como texto literal como `\e]8;;`, use `printf '%b'` em vez de `echo -e` para manipulação de escape mais confiável

**Falhas de exibição com sequências de escape**

* Sequências de escape complexas (cores ANSI, links OSC 8) podem ocasionalmente causar saída corrompida se se sobrepuserem com outras atualizações da interface
* Se você vir texto corrompido, tente simplificar seu script para saída de texto simples
* Linhas de status de múltiplas linhas com códigos de escape são mais propensas a problemas de renderização do que texto simples de linha única

**Confiança do espaço de trabalho necessária**

* O comando de linha de status só é executado se você aceitou o diálogo de confiança do espaço de trabalho para o diretório atual. Como `statusLine` executa um comando de shell, ele requer a mesma aceitação de confiança que hooks e outras configurações que executam shell.
* Se a confiança não for aceita, você verá a notificação `statusline skipped · restart to fix` em vez da saída da sua linha de status. Reinicie o Claude Code e aceite o prompt de confiança para habilitá-lo.

**Erros de script ou travamentos**

* Scripts que saem com códigos diferentes de zero ou não produzem saída fazem a linha de status ficar em branco
* Scripts lentos bloqueiam a linha de status de atualizar até que sejam concluídos. Mantenha scripts rápidos para evitar saída obsoleta.
* Se uma nova atualização for acionada enquanto um script lento está em execução, o script em andamento é cancelado
* Teste seu script independentemente com entrada simulada antes de configurá-lo

**Notificações compartilham a linha de status**

* Notificações do sistema como erros de servidor MCP e atualizações automáticas são exibidas no lado direito da mesma linha que sua linha de status. Notificações transitórias como o aviso de contexto baixo também circulam por esta área.
* Habilitar modo verbose adiciona um contador de tokens a esta área
* Em terminais estreitos, essas notificações podem truncar sua saída de linha de status
