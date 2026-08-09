<!-- Fonte: https://code.claude.com/docs/pt/fast-mode | Baixado em: 2026-08-06 | slug: fast-mode -->

# Acelere respostas com modo rápido

> Obtenha respostas mais rápidas do Opus no Claude Code alternando o modo rápido.

<Note>
  O modo rápido está em [visualização de pesquisa](#research-preview). O recurso, preços e disponibilidade podem mudar com base no feedback.
</Note>

O modo rápido é uma configuração de alta velocidade para Claude Opus, tornando o modelo até 2,5x mais rápido a um custo maior por token. Ative-o com `/fast` quando você precisar de velocidade para trabalho interativo como iteração rápida ou depuração ao vivo, e desative-o quando o custo importa mais do que a latência.

O modo rápido não é um modelo diferente. Ele usa Claude Opus com uma configuração de API diferente que prioriza a velocidade sobre a eficiência de custo. Você obtém qualidade e capacidades idênticas com respostas mais rápidas. O modo rápido é suportado no Opus 4.8 e Opus 4.7. Não está disponível no Sonnet, Haiku ou outros modelos.

<Warning>
  O modo rápido para Opus 4.7 está descontinuado desde 25 de junho de 2026 e será removido em 24 de julho de 2026. Após a remoção, solicitações de modo rápido no Opus 4.7 retornam um erro e não voltam para o Opus 4.7 padrão. Migre para Opus 4.8 para manter a aceleração.
</Warning>

O que você precisa saber:

* Use `/fast` para alternar o modo rápido no CLI do Claude Code. O modo rápido não é suportado na extensão VS Code.
* O preço do modo rápido por MTok de entrada/saída é \$10/\$50 no Opus 4.8 e \$30/\$150 no Opus 4.7.
* Disponível para todos os usuários do Claude Code em planos de assinatura (Pro/Max/Team/Enterprise) e Claude Console.
* Para usuários do Claude Code em planos de assinatura (Pro/Max/Team/Enterprise), o modo rápido está disponível apenas via créditos de uso e não está incluído nos limites de taxa de assinatura.

<h2 id="toggle-fast-mode">
  Alternar modo rápido
</h2>

Alterne o modo rápido de uma destas formas:

* Digite `/fast` e pressione Tab para alternar ativado ou desativado
* Defina `"fastMode": true` no seu [arquivo de configurações do usuário](https://code.claude.com/docs/pt/settings)

Por padrão, o modo rápido que você ativa em uma sessão interativa persiste entre sessões. No [modo não interativo](https://code.claude.com/docs/pt/headless), com a flag `-p`, `/fast` funciona apenas em uma sessão iniciada com modo rápido em seu valor [`--settings`](https://code.claude.com/docs/pt/cli-reference#cli-flags), por exemplo `claude -p --settings '{"fastMode": true}'`; a alternância então se aplica apenas a essa sessão e não é salva como seu padrão, e em qualquer outra sessão não interativa o comando relata que o modo rápido não está disponível. Você pode configurar o modo rápido para ser redefinido a cada sessão. Consulte [require per-session opt-in](#require-per-session-opt-in) para obter detalhes.

Para melhor eficiência de custo, ative o modo rápido no início de uma sessão em vez de alternar no meio da conversa. Consulte [understand the cost tradeoff](#understand-the-cost-tradeoff) para obter detalhes.

Quando você ativa o modo rápido:

* Se você estiver em um modelo diferente, o Claude Code alterna automaticamente para o Opus
* Você verá uma mensagem de confirmação: "Fast mode ON"
* Um pequeno ícone `↯` aparece ao lado do prompt enquanto o modo rápido está ativo
* Execute `/fast` novamente a qualquer momento para verificar se o modo rápido está ativado ou desativado

Quando você desativa o modo rápido com `/fast` novamente, você permanece no Opus. O modelo não reverte para seu modelo anterior. Para alternar para um modelo diferente, use `/model`.

Alternar para um modelo que não suporta modo rápido desativa o modo rápido. Alternar de volta para um modelo Opus suportado o ativa novamente quando sua preferência de modo rápido salva está ativada, a mesma preferência que uma nova sessão inicia por padrão. Com [per-session opt-in](#require-per-session-opt-in) configurado, alternar de volta não ativa o modo rápido novamente; execute `/fast` para reativá-lo. O modo rápido nunca é ativado para uma sessão cuja preferência salva está desativada, e o ícone `↯` e a confirmação `Fast mode ON` aparecem sempre que ele é ativado. Antes da v2.1.208, o modo rápido permanecia desativado depois que você alternava de volta até executar `/fast` novamente.

O Opus 4.8 é o padrão do modo rápido no Claude Code v2.1.154 e posterior. Na v2.1.142 até v2.1.153, o modo rápido usa como padrão o Opus 4.7.

<h2 id="understand-the-cost-tradeoff">
  Entender o tradeoff de custo
</h2>

O modo rápido tem preços por token mais altos do que o Opus padrão, com o multiplicador variando por modelo:

| Modelo   | Entrada (MTok) | Saída (MTok) |
| -------- | -------------- | ------------ |
| Opus 4.8 | \$10           | \$50         |
| Opus 4.7 | \$30           | \$150        |

O preço do modo rápido é fixo em toda a janela de contexto de 1M token. Para a taxa padrão do Opus para comparar, consulte a [referência de preços do Claude](https://platform.claude.com/docs/pt/about-claude/pricing).

A primeira vez que você ativa o modo rápido em uma conversa, você paga o preço total do token de entrada não armazenado em cache do modo rápido para todo o contexto da conversa. Quanto mais profundo você estiver em uma conversa, mais isso custa, portanto ativar o modo rápido desde o início é mais barato. O custo se aplica uma vez por conversa, portanto desativar e ativar o modo rápido novamente mais tarde não o repete. Para o mecanismo, consulte [como o modo rápido interage com o cache de prompt](https://code.claude.com/docs/pt/prompt-caching#turning-on-fast-mode).

<h2 id="decide-when-to-use-fast-mode">
  Decidir quando usar o modo rápido
</h2>

O modo rápido é melhor para trabalho interativo onde a latência de resposta importa mais do que o custo:

* Iteração rápida em mudanças de código
* Sessões de depuração ao vivo
* Trabalho sensível ao tempo com prazos apertados

O modo padrão é melhor para:

* Tarefas autônomas longas onde a velocidade importa menos
* Processamento em lote ou pipelines CI/CD
* Cargas de trabalho sensíveis ao custo

<h3 id="fast-mode-vs-effort-level">
  Modo rápido vs nível de esforço
</h3>

O modo rápido e o nível de esforço afetam a velocidade de resposta, mas de formas diferentes:

| Configuração                    | Efeito                                                                                                      |
| ------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| **Modo rápido**                 | Mesma qualidade de modelo, latência mais baixa, custo mais alto                                             |
| **Nível de esforço mais baixo** | Menos tempo de pensamento, respostas mais rápidas, qualidade potencialmente mais baixa em tarefas complexas |

Você pode combinar ambos: use o modo rápido com um [nível de esforço](https://code.claude.com/docs/pt/model-config#adjust-effort-level) mais baixo para máxima velocidade em tarefas diretas.

<h2 id="requirements">
  Requisitos
</h2>

O modo rápido requer todos os seguintes:

* **Apenas API Anthropic ou assinatura**: o modo rápido está disponível através da API do Anthropic Console e para planos de assinatura Claude usando créditos de uso. Não está disponível no Amazon Bedrock, Google Cloud's Agent Platform, Microsoft Foundry ou Claude Platform na AWS.
* **Créditos de uso ativados**: sua conta deve ter créditos de uso ativados, o que permite cobrança além do uso incluído no seu plano. Para contas individuais, ative isso nas suas [configurações de cobrança do Console](https://platform.claude.com/settings/billing). Para Teams e Enterprise, um administrador deve ativar créditos de uso para a organização.

<Note>
  O uso do modo rápido é cobrado diretamente nos créditos de uso, mesmo que você tenha uso restante no seu plano. Isso significa que os tokens do modo rápido não contam contra o uso incluído do seu plano e são cobrados à taxa do modo rápido desde o primeiro token.
</Note>

* **Habilitação de proprietário para Team e Enterprise**: o modo rápido está desativado por padrão para organizações Team e Enterprise. Um proprietário deve explicitamente [ativar o modo rápido](#enable-fast-mode-for-your-organization) antes que os usuários possam acessá-lo.

<Note>
  Se o modo rápido não tiver sido ativado para sua organização, o comando `/fast` mostrará "Fast mode has been disabled by your organization." Se a lista de permissões [`availableModels`](https://code.claude.com/docs/pt/model-config#restrict-model-selection) da sua organização excluir o modelo Opus do modo rápido, `/fast` é recusado com "is not in your organization's allowed models". A exceção é uma sessão já em execução em um modelo Opus permitido que suporte modo rápido: `/fast` ativa o modo rápido no seu modelo atual em vez de alternar modelos.
</Note>

<h3 id="enable-fast-mode-for-your-organization">
  Ativar modo rápido para sua organização
</h3>

Onde você ativa o modo rápido depende de qual produto sua organização usa:

* **Console** (clientes de API): um administrador o ativa em [Preferências do Claude Code](https://platform.claude.com/claude-code/preferences)
* **Claude AI** (Team e Enterprise): um proprietário o ativa em [Admin Settings > Claude Code](https://claude.ai/admin-settings/claude-code)

Outra opção para desativar completamente o modo rápido é definir `CLAUDE_CODE_DISABLE_FAST_MODE=1`. Consulte [Variáveis de ambiente](https://code.claude.com/docs/pt/env-vars).

<h3 id="require-per-session-opt-in">
  Exigir opt-in por sessão
</h3>

Por padrão, o modo rápido que um usuário ativa em uma sessão interativa persiste entre sessões: ele permanece ativado em futuras sessões. Para alterar isso, defina `fastModePerSessionOptIn` como `true` em qualquer [arquivo de configurações](https://code.claude.com/docs/pt/settings#settings-files), o que faz com que cada sessão comece com o modo rápido desativado e exija que os usuários o ativem explicitamente com `/fast`. Os proprietários em planos [Team](https://claude.com/pricing?utm_source=claude_code\&utm_medium=docs\&utm_content=fast_mode_teams#team-&-enterprise) ou [Enterprise](https://anthropic.com/contact-sales?utm_source=claude_code\&utm_medium=docs\&utm_content=fast_mode_enterprise) podem implantá-lo em toda a organização através de [configurações gerenciadas pelo servidor](https://code.claude.com/docs/pt/server-managed-settings).

```json theme={null}
{
  "fastModePerSessionOptIn": true
}
```

Isso é útil para controlar custos em organizações onde os usuários executam várias sessões simultâneas. Os usuários ainda podem ativar o modo rápido com `/fast` quando precisam de velocidade, mas ele é redefinido no início de cada nova sessão. A preferência de modo rápido do usuário ainda é salva, portanto remover essa configuração restaura o comportamento padrão persistente.

<h2 id="handle-rate-limits">
  Lidar com limites de taxa
</h2>

O modo rápido tem limites de taxa separados do Opus padrão. O modo rápido no Opus 4.8 e Opus 4.7 compartilham o mesmo pool de limite de taxa: o uso em qualquer um deles é extraído dos mesmos limites. Quando você atinge o limite de taxa do modo rápido ou fica sem créditos de uso:

1. O modo rápido automaticamente volta para velocidade padrão
2. O ícone `↯` fica cinza para indicar cooldown
3. Você continua trabalhando com velocidade e preços padrão
4. Quando o cooldown expira, o modo rápido é automaticamente reativado

Para desativar o modo rápido manualmente em vez de esperar pelo cooldown, execute `/fast` novamente.

<h2 id="research-preview">
  Research preview
</h2>

O modo rápido é um recurso de visualização de pesquisa. Isso significa:

* O recurso pode mudar com base no feedback
* A disponibilidade e preços estão sujeitos a alterações
* A configuração de API subjacente pode evoluir

Relate problemas ou feedback através de seus canais de suporte Anthropic usuais.

<h2 id="see-also">
  Veja também
</h2>

* [Configuração de modelo](https://code.claude.com/docs/pt/model-config): alterne modelos e ajuste níveis de esforço
* [Gerenciar custos efetivamente](https://code.claude.com/docs/pt/costs): rastreie o uso de tokens e reduza custos
* [Configuração da linha de status](https://code.claude.com/docs/pt/statusline): exiba informações de modelo e contexto
