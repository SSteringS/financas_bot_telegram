<!-- Fonte: https://code.claude.com/docs/pt/output-styles | Baixado em: 2026-08-06 | slug: output-styles -->

# Estilos de saída

> Adapte Claude Code para usos além da engenharia de software

Os estilos de saída alteram como Claude responde, não o que Claude sabe. Eles modificam o prompt do sistema para definir papel, tom e formato de saída. Use um quando você continua re-solicitando a mesma voz ou formato a cada turno, ou quando você quer que Claude atue como algo diferente de um engenheiro de software.

Um estilo de saída personalizado adiciona suas instruções ao prompt do sistema e permite que você escolha se deseja manter as instruções integradas de engenharia de software do Claude Code. Mantenha-as quando você está mudando como Claude se comunica, mas ainda está codificando, como sempre responder com um diagrama. Deixe-as de fora quando Claude não está fazendo engenharia de software, como um assistente de redação ou analista de dados.

Para instruções sobre seu projeto, convenções ou base de código, use [CLAUDE.md](https://code.claude.com/docs/pt/memory) em vez disso.

<h2 id="built-in-output-styles">
  Estilos de saída integrados
</h2>

O estilo de saída **Default** do Claude Code é o prompt do sistema existente, projetado para ajudá-lo a completar tarefas de engenharia de software com eficiência.

Existem três estilos de saída integrados adicionais:

* **Proactive**: Claude executa imediatamente, faz suposições razoáveis em vez de pausar para decisões rotineiras e prefere ação ao planejamento. Isso é uma orientação de execução autônoma mais forte do que o [modo automático](https://code.claude.com/docs/pt/permission-modes#eliminate-prompts-with-auto-mode) aplica, e funciona sem alterar seu modo de permissão, portanto você ainda vê prompts de permissão antes das ferramentas serem executadas.

* **Explanatory**: Fornece "Insights" educacionais entre ajudá-lo a completar tarefas de engenharia de software. Ajuda você a entender as escolhas de implementação e padrões da base de código.

* **Learning**: Modo colaborativo de aprender fazendo, onde Claude não apenas compartilhará "Insights" enquanto codifica, mas também pedirá que você contribua com pequenos e estratégicos pedaços de código. Claude Code adicionará marcadores `TODO(human)` no seu código para você implementar.

<h2 id="change-your-output-style">
  Altere seu estilo de saída
</h2>

Execute `/config` e selecione **Output style** para escolher um estilo de um menu. Sua seleção é salva em `.claude/settings.local.json` no [nível do projeto local](https://code.claude.com/docs/pt/settings).

<Note>O comando `/output-style` independente foi descontinuado na v2.1.73 e removido na v2.1.91. Use `/config` ou edite a configuração `outputStyle` diretamente.</Note>

Para definir um estilo sem o menu, edite o campo `outputStyle` diretamente em um arquivo de configurações:

```json theme={null}
{
  "outputStyle": "Explanatory"
}
```

O estilo de saída faz parte do prompt do sistema, que Claude Code lê uma vez no início da sessão. As alterações entram em vigor após `/clear` ou uma nova sessão. Consulte [Como Claude Code usa prompt caching](https://code.claude.com/docs/pt/prompt-caching#changing-output-style) para saber o que uma mudança de estilo de saída faz no cache.

<h2 id="create-a-custom-output-style">
  Crie um estilo de saída personalizado
</h2>

Um estilo de saída personalizado é um arquivo Markdown: frontmatter para metadados, depois as instruções a adicionar ao prompt do sistema.

<Steps>
  <Step title="Crie um arquivo Markdown">
    Salve-o em um de três níveis. O nome do arquivo se torna o nome do estilo, a menos que você defina `name` no frontmatter.

    * Usuário: `~/.claude/output-styles`
    * Projeto: `.claude/output-styles`
    * Política gerenciada: `.claude/output-styles` dentro do [diretório de configurações gerenciadas](https://code.claude.com/docs/pt/settings#settings-files)

    Os estilos de saída do projeto são carregados de cada `.claude/output-styles/` entre o diretório de trabalho e a raiz do repositório. A partir da v2.1.178, quando mais de um desses diretórios aninhados define um estilo com o mesmo nome, Claude Code usa o mais próximo do diretório de trabalho.
  </Step>

  <Step title="Adicione frontmatter e instruções">
    Decida se deseja manter as instruções de engenharia de software do Claude Code. Defina `keep-coding-instructions: true` se você está mudando como Claude se comunica, mas ainda quer que ele codifique da mesma forma. Deixe de fora se Claude não estará fazendo engenharia de software.

    Este exemplo lidera cada explicação com um diagrama enquanto mantém o comportamento de codificação do Claude:

    ```markdown theme={null}
    ---
    name: Diagrams first
    description: Lead every explanation with a diagram
    keep-coding-instructions: true
    ---

    When explaining code, architecture, or data flow, start with a Mermaid diagram showing the structure, then explain in prose.

    ## Diagram conventions

    Use `flowchart TD` for control flow and `sequenceDiagram` for request paths. Keep diagrams under 15 nodes.
    ```
  </Step>

  <Step title="Mude para seu estilo">
    Execute `/config` e selecione seu estilo em **Output style**. Ele entra em vigor após `/clear` ou na próxima vez que você iniciar uma sessão.
  </Step>
</Steps>

[Plugins](https://code.claude.com/docs/pt/plugins-reference) também podem enviar estilos de saída em um diretório `output-styles/`.

<h3 id="frontmatter">
  Frontmatter
</h3>

Os arquivos de estilo de saída suportam estes campos de frontmatter:

| Frontmatter                | Propósito                                                                                                                                                                                                                                                                                      | Padrão                   |
| :------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :----------------------- |
| `name`                     | Nome do estilo de saída, se não for o nome do arquivo                                                                                                                                                                                                                                          | Herda do nome do arquivo |
| `description`              | Descrição do estilo de saída, mostrada no seletor `/config`                                                                                                                                                                                                                                    | Nenhum                   |
| `keep-coding-instructions` | Manter as instruções integradas de engenharia de software do Claude Code                                                                                                                                                                                                                       | `false`                  |
| `force-for-plugin`         | Apenas estilos de saída de plugin: aplique este estilo automaticamente sempre que o plugin estiver habilitado, sem exigir que os usuários o selecionem. Substitui a configuração `outputStyle` do usuário. Se vários plugins habilitados definirem isso, Claude Code usa o primeiro carregado. | `false`                  |

<h2 id="how-output-styles-work">
  Como os estilos de saída funcionam
</h2>

Os estilos de saída modificam diretamente o prompt do sistema do Claude Code.

* Todos os estilos de saída têm suas próprias instruções personalizadas adicionadas ao final do prompt do sistema.
* Todos os estilos de saída acionam lembretes para Claude aderir às instruções do estilo de saída durante a conversa.
* Os estilos de saída personalizados deixam de fora as instruções integradas de engenharia de software do Claude Code, como como escopar mudanças, escrever comentários e verificar trabalho, a menos que `keep-coding-instructions` seja definido como `true`.

O uso de tokens depende do estilo. Adicionar instruções ao prompt do sistema aumenta os tokens de entrada, embora o prompt caching reduza esse custo após a primeira solicitação em uma sessão. Os estilos integrados Explanatory e Learning produzem respostas mais longas que Default por design, o que aumenta os tokens de saída. Para estilos personalizados, o uso de tokens de saída depende do que suas instruções dizem ao Claude para produzir.

<h2 id="comparisons-to-related-features">
  Comparações com recursos relacionados
</h2>

Vários recursos personalizam como Claude Code se comporta. Os estilos de saída modificam o prompt do sistema diretamente e se aplicam a cada resposta. Os outros adicionam instruções sem alterar o prompt do sistema padrão, ou os limitam a uma tarefa específica.

| Recurso                  | Como funciona                                                                | Use-o quando                                                                     |
| :----------------------- | :--------------------------------------------------------------------------- | :------------------------------------------------------------------------------- |
| Estilos de saída         | Modifica o prompt do sistema                                                 | Você quer um papel, tom ou formato de resposta padrão diferente a cada turno     |
| [CLAUDE.md](https://code.claude.com/docs/pt/memory)  | Adiciona uma mensagem do usuário após o prompt do sistema                    | Claude deve sempre saber suas convenções de projeto e contexto da base de código |
| `--append-system-prompt` | Anexa ao prompt do sistema sem remover nada                                  | Você quer uma adição única para uma única invocação                              |
| [Agents](https://code.claude.com/docs/pt/sub-agents) | Executa um subagente com seu próprio prompt do sistema, modelo e ferramentas | Você quer um auxiliar com escopo separado para uma tarefa focada                 |
| [Skills](https://code.claude.com/docs/pt/skills)     | Carrega instruções específicas de tarefas quando invocadas ou relevantes     | Você tem um fluxo de trabalho reutilizável                                       |

<h2 id="related-resources">
  Recursos relacionados
</h2>

* [Settings](https://code.claude.com/docs/pt/settings): onde o campo `outputStyle` reside e como a precedência de configurações funciona
* [Permission modes](https://code.claude.com/docs/pt/permission-modes): como o estilo Proactive se compara ao modo automático
* [Plugins](https://code.claude.com/docs/pt/plugins): empacote e distribua estilos de saída junto com skills, hooks e agents
* [Debug your configuration](https://code.claude.com/docs/pt/debug-your-config): diagnostique por que um estilo de saída não está entrando em vigor
