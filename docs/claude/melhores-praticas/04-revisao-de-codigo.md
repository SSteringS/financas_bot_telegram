<!-- Fonte: https://code.claude.com/docs/pt/code-review | Baixado em: 2026-08-06 | slug: code-review -->

# Code Review

> Configure análises automatizadas de PR que detectam erros de lógica, vulnerabilidades de segurança e regressões usando análise multi-agente de sua base de código completa

<Note>
  Code Review está em visualização de pesquisa, disponível para assinaturas [Team e Enterprise](https://claude.ai/admin-settings/claude-code). Não está disponível para organizações com [Zero Data Retention](https://code.claude.com/docs/pt/zero-data-retention) ativado.
</Note>

Code Review analisa seus pull requests do GitHub e publica descobertas como comentários inline nas linhas de código onde encontrou problemas. Uma frota de agentes especializados examina as alterações de código no contexto de sua base de código completa, procurando por erros de lógica, vulnerabilidades de segurança, casos extremos quebrados e regressões sutis.

As descobertas são marcadas por severidade e não aprovam ou bloqueiam seu PR, portanto os fluxos de trabalho de revisão existentes permanecem intactos. Você pode ajustar o que Claude sinaliza adicionando um arquivo `CLAUDE.md` ou `REVIEW.md` ao seu repositório.

Para executar Claude em sua própria infraestrutura de CI em vez deste serviço gerenciado, consulte [GitHub Actions](https://code.claude.com/docs/pt/github-actions) ou [GitLab CI/CD](https://code.claude.com/docs/pt/gitlab-ci-cd). Para repositórios em uma instância GitHub auto-hospedada, consulte [GitHub Enterprise Server](https://code.claude.com/docs/pt/github-enterprise-server).

Esta página cobre:

* [Como as revisões funcionam](#how-reviews-work)
* [Configuração](#set-up-code-review)
* [Acionando revisões manualmente](#manually-trigger-reviews) com `@claude review` e `@claude review once`
* [Personalizando revisões](#customize-reviews) com `CLAUDE.md` e `REVIEW.md`
* [Preços](#pricing)
* [Troubleshooting](#troubleshooting) execuções falhadas e comentários ausentes
* [Revisando um diff localmente](#review-a-diff-locally) com o comando `/code-review`

<Note>
  Para revisar um diff localmente em seu terminal sem instalar o GitHub App, execute o comando `/code-review` em qualquer sessão Claude Code. Consulte [Revisar um diff localmente](#review-a-diff-locally).
</Note>

<h2 id="how-reviews-work">
  Como as revisões funcionam
</h2>

Depois que um administrador [ativa Code Review](#set-up-code-review) para sua organização, as revisões são acionadas quando um PR é aberto, em cada push ou quando solicitado manualmente, dependendo do comportamento configurado do repositório. Comentar `@claude review` [inicia revisões em um PR](#manually-trigger-reviews) em qualquer modo.

Quando uma revisão é executada, vários agentes analisam o diff e o código circundante em paralelo na infraestrutura da Anthropic. Cada agente procura por uma classe diferente de problema, então uma etapa de verificação verifica os candidatos contra o comportamento real do código para filtrar falsos positivos. Os resultados são desduplicados, classificados por severidade e publicados como comentários inline nas linhas específicas onde os problemas foram encontrados, com um resumo no corpo da revisão. Se nenhum problema for encontrado, Code Review atualiza a execução de verificação do GitHub para mostrar que nenhum problema foi detectado. Claude também pode publicar um breve comentário de confirmação no PR.

As revisões escalam em custo com o tamanho e complexidade do PR, completando em média em 20 minutos. Os administradores podem monitorar a atividade de revisão e gastos através do [painel de análise](#view-usage).

<h3 id="severity-levels">
  Níveis de severidade
</h3>

Cada descoberta é marcada com um nível de severidade:

| Marcador | Severidade    | Significado                                                             |
| :------- | :------------ | :---------------------------------------------------------------------- |
| 🔴       | Importante    | Um bug que deve ser corrigido antes de fazer merge                      |
| 🟡       | Nit           | Um problema menor, vale a pena corrigir mas não é bloqueante            |
| 🟣       | Pré-existente | Um bug que existe na base de código mas não foi introduzido por este PR |

As descobertas incluem uma seção de raciocínio estendido recolhível que você pode expandir para entender por que Claude sinalizou o problema e como verificou o problema.

<h3 id="rate-and-reply-to-findings">
  Avaliar e responder a descobertas
</h3>

Cada comentário de revisão do Claude chega com 👍 e 👎 já anexados para que ambos os botões apareçam na interface do GitHub para classificação com um clique. Clique em 👍 se a descoberta foi útil ou 👎 se estava errada ou ruidosa. A Anthropic coleta contagens de reações após o PR ser mesclado e as usa para ajustar o revisor. As reações não acionam uma re-revisão ou alteram nada no PR.

Responder a um comentário inline não solicita que Claude responda ou atualize o PR. Para agir em uma descoberta, corrija o código e faça push. Se o PR estiver inscrito em revisões acionadas por push, a próxima execução resolve a thread quando o problema for corrigido. Para solicitar uma revisão nova sem fazer push, comente `@claude review once` como um [comentário de PR de nível superior](#manually-trigger-reviews).

<h3 id="check-run-output">
  Saída de execução de verificação
</h3>

Além dos comentários de revisão inline, cada revisão popula a execução de verificação **Claude Code Review** que aparece junto com suas verificações de CI. Expanda seu link **Details** para ver um resumo de cada descoberta em um único lugar, classificado por severidade:

| Severidade    | Arquivo:Linha             | Problema                                                                 |
| ------------- | ------------------------- | ------------------------------------------------------------------------ |
| 🔴 Importante | `src/auth/session.ts:142` | Atualização de token corre com logout, deixando sessões obsoletas ativas |
| 🟡 Nit        | `src/auth/session.ts:88`  | `parseExpiry` retorna silenciosamente 0 em entrada malformada            |

Cada descoberta também aparece como uma anotação na aba **Files changed**, marcada diretamente nas linhas de diff relevantes. As descobertas Importantes são renderizadas com um marcador vermelho, nits com um aviso amarelo e bugs pré-existentes com um aviso cinza. Anotações e a tabela de severidade são escritas na execução de verificação independentemente dos comentários de revisão inline, portanto permanecem disponíveis mesmo se GitHub rejeitar um comentário inline em uma linha que se moveu.

A execução de verificação sempre é concluída com uma conclusão neutra, portanto nunca bloqueia a mesclagem através de regras de proteção de branch. Se você deseja bloquear mesclagens em descobertas de Code Review, leia o detalhamento de severidade da saída de execução de verificação em seu próprio CI. A última linha do texto Details é um comentário legível por máquina que seu fluxo de trabalho pode analisar com `gh` e jq:

```bash theme={null}
gh api repos/OWNER/REPO/check-runs/CHECK_RUN_ID \
  --jq '.output.text | split("bughunter-severity: ")[1] | split(" -->")[0] | fromjson'
```

Isso retorna um objeto JSON com contagens por severidade, por exemplo `{"normal": 2, "nit": 1, "pre_existing": 0}`. A chave `normal` contém a contagem de descobertas Importantes; um valor diferente de zero significa que Claude encontrou pelo menos um bug que vale a pena corrigir antes da mesclagem.

<h3 id="what-code-review-checks">
  O que Code Review verifica
</h3>

Por padrão, Code Review se concentra em correção: bugs que quebrariam a produção, não preferências de formatação ou cobertura de testes ausente. Você pode expandir o que verifica [adicionando arquivos de orientação](#customize-reviews) ao seu repositório.

<h2 id="set-up-code-review">
  Configurar Code Review
</h2>

Um Owner ativa Code Review uma vez para a organização e seleciona quais repositórios incluir.

<Steps>
  <Step title="Abrir configurações de administrador do Claude Code">
    Vá para [claude.ai/admin-settings/claude-code](https://claude.ai/admin-settings/claude-code) e encontre a seção Code Review. Você precisa da função Owner ou Primary Owner em sua organização Claude e permissão para instalar GitHub Apps em sua organização GitHub.
  </Step>

  <Step title="Iniciar configuração">
    Clique em **Setup**. Isso inicia o fluxo de instalação do GitHub App.
  </Step>

  <Step title="Instalar o Claude GitHub App">
    Siga os prompts para instalar o Claude GitHub App em sua organização GitHub. O app solicita estas permissões de repositório:

    * **Contents**: leitura e escrita
    * **Issues**: leitura e escrita
    * **Pull requests**: leitura e escrita

    Code Review usa acesso de leitura a conteúdos e acesso de escrita a pull requests. O conjunto de permissões mais amplo também suporta [GitHub Actions](https://code.claude.com/docs/pt/github-actions) se você ativar isso mais tarde.
  </Step>

  <Step title="Selecionar repositórios">
    Escolha quais repositórios ativar para Code Review. Se você não vir um repositório, certifique-se de que deu ao Claude GitHub App acesso a ele durante a instalação. Você pode adicionar mais repositórios mais tarde.
  </Step>

  <Step title="Definir gatilhos de revisão por repo">
    Após a conclusão da configuração, a seção Code Review mostra seus repositórios em uma tabela. Para cada repositório, use o dropdown **Review Behavior** para escolher quando as revisões são executadas:

    * **Once after PR creation**: a revisão é executada uma vez quando um PR é aberto ou marcado como pronto para revisão
    * **After every push**: a revisão é executada em cada push para o branch do PR, detectando novos problemas conforme o PR evolui e resolvendo automaticamente threads quando você corrige problemas sinalizados
    * **Manual**: as revisões começam apenas quando alguém [comenta `@claude review` ou `@claude review once` em um PR](#manually-trigger-reviews); `@claude review` também inscreve o PR em revisões em pushes subsequentes

    Revisar em cada push executa a maioria das revisões e custa mais. O modo Manual é útil para repositórios de alto tráfego onde você deseja optar PRs específicos para revisão, ou para começar a revisar seus PRs apenas quando estiverem prontos.
  </Step>
</Steps>

A tabela de repositórios também mostra o custo médio por revisão para cada repo com base na atividade recente. Use o menu de ações de linha para ativar ou desativar Code Review por repositório, ou para remover um repositório completamente.

Para verificar a configuração, abra um PR de teste. Se você escolheu um gatilho automático, uma execução de verificação chamada **Claude Code Review** aparece em alguns minutos. Se você escolheu Manual, comente `@claude review` no PR para iniciar a primeira revisão. Se nenhuma execução de verificação aparecer, confirme que o repositório está listado em suas configurações de administrador e que o Claude GitHub App tem acesso a ele.

<h2 id="manually-trigger-reviews">
  Acionando revisões manualmente
</h2>

Dois comandos de comentário iniciam uma revisão sob demanda. Ambos funcionam independentemente do gatilho configurado do repositório, portanto você pode usá-los para optar PRs específicos para revisão no modo Manual ou para obter uma re-revisão imediata em outros modos.

| Comando               | O que faz                                                                           |
| :-------------------- | :---------------------------------------------------------------------------------- |
| `@claude review`      | Inicia uma revisão e inscreve o PR em revisões acionadas por push a partir de então |
| `@claude review once` | Inicia uma única revisão sem inscrever o PR em pushes futuros                       |

Use `@claude review once` quando você deseja feedback sobre o estado atual de um PR mas não deseja que cada push subsequente incorra em uma revisão. Isso é útil para PRs de longa duração com pushes frequentes, ou quando você deseja uma segunda opinião única sem alterar o comportamento de revisão do PR.

Para qualquer comando acionar uma revisão:

* Poste-o como um comentário de PR de nível superior, não um comentário inline em uma linha de diff
* Coloque o comando no início do comentário, com `once` na mesma linha se você estiver usando a forma única
* Você deve ter acesso de proprietário, membro ou colaborador ao repositório
* O PR deve estar aberto

Diferentemente dos gatilhos automáticos, os gatilhos manuais são executados em PRs de rascunho, já que uma solicitação explícita sinaliza que você deseja a revisão agora independentemente do status de rascunho.

Se uma revisão já estiver em execução nesse PR, a solicitação é enfileirada até que a revisão em andamento seja concluída. Você pode monitorar o progresso através da execução de verificação no PR.

<h2 id="customize-reviews">
  Personalizar revisões
</h2>

Code Review lê dois arquivos do seu repositório para orientar o que sinaliza. Eles diferem em como influenciam fortemente a revisão:

* **`CLAUDE.md`**: instruções de projeto compartilhadas que Claude Code usa para todas as tarefas, não apenas revisões. Code Review o lê como contexto de projeto e sinaliza violações recém-introduzidas como nits.
* **`REVIEW.md`**: instruções exclusivas de revisão, injetadas diretamente em cada agente no pipeline de revisão como prioridade máxima. Use-o para alterar o que é sinalizado, em qual severidade e como as descobertas são relatadas.

<h3 id="claude-md">
  CLAUDE.md
</h3>

Code Review lê seus arquivos `CLAUDE.md` do repositório e trata violações recém-introduzidas como descobertas de [nível nit](#severity-levels). Isso funciona bidirecionalmente: se seu PR altera o código de uma forma que torna uma declaração `CLAUDE.md` desatualizada, Claude sinaliza que os docs precisam ser atualizados também.

Claude lê arquivos `CLAUDE.md` em cada nível de sua hierarquia de diretórios, portanto as regras no `CLAUDE.md` de um subdiretório se aplicam apenas aos arquivos sob esse caminho. Consulte a [documentação de memory](https://code.claude.com/docs/pt/memory) para mais informações sobre como `CLAUDE.md` funciona.

Para orientação específica de revisão que você não deseja aplicada a sessões gerais do Claude Code, use [`REVIEW.md`](#review-md) em vez disso.

<h3 id="review-md">
  REVIEW\.md
</h3>

`REVIEW.md` é um arquivo na raiz do seu repositório que substitui como Code Review se comporta no seu repo. Seu conteúdo é injetado no prompt do sistema de cada agente no pipeline de revisão como o bloco de instrução de prioridade máxima, tendo precedência sobre a orientação de revisão padrão.

Como é colado verbatim, `REVIEW.md` é instruções simples: a [sintaxe `@` import](https://code.claude.com/docs/pt/memory#import-additional-files) não é expandida e os arquivos referenciados não são lidos no prompt. Coloque as regras que você deseja aplicadas diretamente no arquivo.

<h4 id="what-you-can-tune">
  O que você pode ajustar
</h4>

`REVIEW.md` é markdown de forma livre, portanto qualquer coisa que você possa expressar como uma instrução de revisão está no escopo. Os padrões abaixo têm o maior impacto na prática.

**Severidade**: redefina o que 🔴 Importante significa para seu repo. A calibração padrão visa código de produção; um repo de docs, um repo de config ou um protótipo pode querer uma definição muito mais estreita. Declare explicitamente quais classes de descoberta são Importantes e quais são Nit no máximo. Você também pode escalar na outra direção, por exemplo tratando qualquer violação de `CLAUDE.md` como Importante em vez do nit padrão.

**Volume de nit**: limite quantos comentários 🟡 Nit uma única revisão publica. Prosa e arquivos de config podem ser polidos para sempre. Um limite como "relatar no máximo cinco nits, mencionar o resto como uma contagem no resumo" mantém as revisões acionáveis.

**Regras de pulo**: liste caminhos, padrões de branch e categorias de descoberta onde Claude não deve publicar descobertas. Candidatos comuns são código gerado, lockfiles, dependências vendidas e branches de autoria de máquina, junto com qualquer coisa que seu CI já aplique como linting ou verificação ortográfica. Para caminhos que justificam alguma revisão mas não escrutínio completo, defina uma barra mais alta em vez de pular completamente: "em `scripts/`, relatar apenas se próximo de certo e severo."

**Verificações específicas do repo**: adicione regras que você deseja sinalizadas em cada PR, como "novas rotas de API devem ter um teste de integração." Como `REVIEW.md` é injetado como prioridade máxima, essas chegam mais confiávelmente do que as mesmas regras em um `CLAUDE.md` longo.

**Barra de verificação**: exija evidência antes de uma classe de descoberta ser publicada. Por exemplo, "reivindicações de comportamento precisam de uma citação `file:line` na fonte, não uma inferência de nomenclatura" reduz falsos positivos que de outra forma custariam ao autor uma volta.

**Convergência de re-revisão**: diga a Claude como se comportar quando um PR já foi revisado. Uma regra como "após a primeira revisão, suprima nits novos e publique descobertas Importantes apenas" impede que uma correção de uma linha chegue à sétima rodada apenas por estilo.

**Forma de resumo**: peça para o corpo da revisão abrir com uma contagem de uma linha como `2 factual, 4 style`, e liderar com "sem problemas factuais" quando esse for o caso. O autor quer saber a forma do trabalho antes dos detalhes.

<h4 id="example">
  Exemplo
</h4>

Este `REVIEW.md` recalibra severidade para um serviço backend, limita nits, pula arquivos gerados e adiciona verificações específicas do repo.

```markdown theme={null}
# Instruções de revisão

## O que Importante significa aqui

Reserve Importante para descobertas que quebrariam comportamento, vazariam dados
ou bloqueariam um rollback: lógica incorreta, consultas de banco de dados sem escopo, PII
em logs ou mensagens de erro, e migrações que não são compatíveis com versões anteriores. Estilo, nomenclatura e sugestões de refatoração são Nit no máximo.

## Limite os nits

Relatar no máximo cinco Nits por revisão. Se você encontrou mais, diga "mais N
itens similares" no resumo em vez de publicá-los inline. Se tudo que você encontrou é um Nit, lidere o resumo com "Sem problemas bloqueantes."

## Não relatar

- Qualquer coisa que CI já aplique: lint, formatação, erros de tipo
- Arquivos gerados sob `src/gen/` e qualquer arquivo `*.lock`
- Código apenas de teste que intencionalmente viola regras de produção

## Sempre verificar

- Novas rotas de API têm um teste de integração
- Linhas de log não incluem endereços de email, IDs de usuário ou corpos de solicitação
- Consultas de banco de dados estão no escopo do chamador do tenant
```

<h4 id="keep-it-focused">
  Mantenha-o focado
</h4>

O comprimento tem um custo: um `REVIEW.md` longo dilui as regras que mais importam. Mantenha-o em instruções que alteram o comportamento de revisão e deixe contexto geral do projeto em `CLAUDE.md`.

<h2 id="view-usage">
  Ver uso
</h2>

Vá para [claude.ai/analytics/code-review](https://claude.ai/analytics/code-review) para ver a atividade de Code Review em toda sua organização. O painel mostra:

| Seção                | O que mostra                                                                                                       |
| :------------------- | :----------------------------------------------------------------------------------------------------------------- |
| PRs reviewed         | Contagem diária de pull requests revisados durante o intervalo de tempo selecionado                                |
| Cost weekly          | Gasto semanal em Code Review                                                                                       |
| Feedback             | Contagem de comentários de revisão que foram resolvidos automaticamente porque um desenvolvedor abordou o problema |
| Repository breakdown | Contagens por repo de PRs revisados e comentários resolvidos                                                       |

A tabela de repositórios nas configurações de administrador também mostra custo médio por revisão para cada repo. Os números de custo do painel são estimativas para monitorar atividade; para gasto preciso de fatura, consulte sua fatura da Anthropic.

<h2 id="pricing">
  Preços
</h2>

Code Review é faturado com base no uso de tokens. Cada revisão custa em média \$15-25, escalando com o tamanho do PR, complexidade da base de código e quantos problemas requerem verificação. O uso de Code Review é faturado separadamente através de [créditos de uso](https://support.claude.com/pt/articles/12429409-extra-usage-for-paid-claude-plans) e não conta contra o uso incluído do seu plano.

O gatilho de revisão que você escolhe afeta o custo total:

* **Once after PR creation**: é executado uma vez por PR
* **After every push**: é executado em cada push, multiplicando o custo pelo número de pushes
* **Manual**: sem revisões até que alguém comente `@claude review` em um PR

Em qualquer modo, comentar `@claude review` [opta o PR em revisões acionadas por push](#manually-trigger-reviews), portanto custo adicional acumula por push após esse comentário. Para executar uma única revisão sem inscrever em pushes futuros, comente `@claude review once` em vez disso.

Os custos aparecem em sua fatura da Anthropic independentemente de sua organização usar Amazon Bedrock ou Google Cloud's Agent Platform para outros recursos do Claude Code. Para definir um limite de gasto mensal para Code Review, vá para [claude.ai/admin-settings/usage](https://claude.ai/admin-settings/usage) e configure o limite para o serviço Claude Code Review.

Monitore gastos através do gráfico de custo semanal em [analytics](#view-usage) ou da coluna de custo médio por repo nas configurações de administrador.

<h2 id="troubleshooting">
  Troubleshooting
</h2>

As execuções de revisão são do melhor esforço. Uma execução falhada nunca bloqueia seu PR, mas também não tenta novamente por conta própria. Esta seção cobre como se recuperar de uma execução falhada e onde procurar quando a execução de verificação relata problemas que você não consegue encontrar.

<h3 id="retrigger-a-failed-or-timed-out-review">
  Retrigger uma revisão falhada ou com tempo limite excedido
</h3>

Quando a infraestrutura de revisão atinge um erro interno ou excede seu limite de tempo, a execução de verificação é concluída com um título de **Code review encountered an error** ou **Code review timed out**. A conclusão ainda é neutra, portanto nada bloqueia sua mesclagem, mas nenhuma descoberta é publicada.

Para executar a revisão novamente, comente `@claude review once` no PR. Isso inicia uma revisão nova sem inscrever o PR em pushes futuros. Se o PR já estiver inscrito em revisões acionadas por push, fazer push de um novo commit também inicia uma nova revisão.

O botão **Re-run** na aba Checks do GitHub não retrigger Code Review. Use o comando de comentário ou um novo push em vez disso.

<h3 id="review-didn’t-run-and-the-pr-shows-a-spend-cap-message">
  Revisão não foi executada e o PR mostra uma mensagem de limite de gasto
</h3>

Quando o limite de gasto mensal de sua organização é atingido, Code Review publica um único comentário no PR explicando que a revisão foi ignorada. As revisões retomam automaticamente no início do próximo período de faturamento, ou imediatamente quando um administrador aumenta o limite em [claude.ai/admin-settings/usage](https://claude.ai/admin-settings/usage).

<h3 id="find-issues-that-aren’t-showing-as-inline-comments">
  Encontrar problemas que não aparecem como comentários inline
</h3>

Se o título da execução de verificação disser que problemas foram encontrados mas você não vir comentários de revisão inline no diff, procure nestes outros locais onde as descobertas são exibidas:

* **Check run Details**: clique em **Details** ao lado da verificação Claude Code Review na aba Checks. A tabela de severidade lista cada descoberta com seu arquivo, linha e resumo independentemente de o comentário inline ter sido aceito.
* **Files changed annotations**: abra a aba **Files changed** no PR. As descobertas são renderizadas como anotações anexadas diretamente às linhas de diff, separadas dos comentários de revisão.
* **Review body**: se você fez push para o PR enquanto uma revisão estava em execução, algumas descobertas podem fazer referência a linhas que não existem mais no diff atual. Essas aparecem sob um cabeçalho **Additional findings** no texto do corpo da revisão em vez de como comentários inline.

<h2 id="review-a-diff-locally">
  Revisar um diff localmente
</h2>

O comando [`/code-review`](https://code.claude.com/docs/pt/commands) revisa um diff em seu terminal sem instalar o GitHub App. Execute-o em qualquer sessão Claude Code: ele relata bugs de correção e reutilização, simplificação e limpezas de eficiência. Por padrão, a revisão local cobre os commits de sua branch à frente de sua upstream mais quaisquer alterações não confirmadas na árvore de trabalho. Passe `--comment` para publicar descobertas como comentários PR inline, ou `--fix` para aplicar as descobertas à sua árvore de trabalho após a revisão.

Níveis de [esforço](https://code.claude.com/docs/pt/model-config#adjust-effort-level) mais baixos retornam menos descobertas com maior confiança, enquanto `high` até `max` fornecem cobertura mais ampla e podem incluir descobertas incertas. Sem um argumento de esforço, a revisão usa o esforço atual da sessão. Para revisar algo diferente do diff padrão, passe um alvo: um caminho de arquivo, um número de PR, um nome de branch ou um intervalo de ref como `main...my-feature`. A forma de intervalo de ref revisa o diff confirmado que um pull request de `my-feature` para `main` conteria, independentemente de como a upstream da branch está configurada.

`/code-review ultra --fix` executa a [ultrareview](https://code.claude.com/docs/pt/ultrareview) mais profunda na nuvem, então aplica suas descobertas à sua árvore de trabalho quando chegam de volta em sua sessão. Ultrareview usa seu próprio escopo: sua branch atual contra a branch padrão do repositório, mais quaisquer alterações não confirmadas e preparadas na árvore de trabalho.

O comando foi nomeado `/simplify` antes da v2.1.147, quando aplicava correções por padrão. A partir da v2.1.154, `/simplify` executa uma revisão separada apenas de limpeza que aplica correções sem procurar por bugs. Se você criou scripts com `/simplify` para busca de bugs, mude para `/code-review --fix`, que permanece inalterado.

<h2 id="related-resources">
  Recursos relacionados
</h2>

Code Review é projetado para funcionar junto com o resto do Claude Code. Se você deseja executar revisões localmente antes de abrir um PR, precisa de uma configuração auto-hospedada ou deseja aprofundar como `CLAUDE.md` molda o comportamento do Claude em todas as ferramentas, estas páginas são bons próximos passos:

* [Commands](https://code.claude.com/docs/pt/commands): execute `/code-review` em uma sessão local do Claude Code para verificar um diff antes de fazer push
* [GitHub Actions](https://code.claude.com/docs/pt/github-actions): execute Claude em seus próprios fluxos de trabalho do GitHub Actions para automação personalizada além de code review
* [GitLab CI/CD](https://code.claude.com/docs/pt/gitlab-ci-cd): integração Claude auto-hospedada para pipelines GitLab
* [Memory](https://code.claude.com/docs/pt/memory): como arquivos `CLAUDE.md` funcionam em Claude Code
* [Analytics](https://code.claude.com/docs/pt/analytics): rastreie o uso de Claude Code além de code review
