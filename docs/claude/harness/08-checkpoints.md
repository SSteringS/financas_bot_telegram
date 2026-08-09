<!-- Fonte: https://code.claude.com/docs/pt/checkpointing | Baixado em: 2026-08-06 | slug: checkpointing -->

# Checkpointing

> Rastreie, reverta e resuma as edições e conversas do Claude para gerenciar o estado da sessão.

Claude Code rastreia automaticamente as edições de arquivo do Claude conforme você trabalha, permitindo que você desfaça rapidamente as alterações e reverta para estados anteriores se algo sair do caminho.

<h2 id="how-checkpoints-work">
  Como o checkpointing funciona
</h2>

Conforme você trabalha com Claude, o checkpointing captura automaticamente o estado do seu código antes de cada prompt do usuário. Esta rede de segurança permite que você persiga tarefas ambiciosas e em larga escala sabendo que sempre pode retornar a um estado de código anterior.

<h3 id="automatic-tracking">
  Rastreamento automático
</h3>

Claude Code rastreia todas as alterações feitas por suas ferramentas de edição de arquivo:

* Cada prompt do usuário cria um novo checkpoint
* Claude Code mantém snapshots de arquivo para os 100 checkpoints mais recentes em uma sessão. Descartar um checkpoint mais antigo deleta os arquivos de snapshot que nenhum checkpoint restante referencia, exceto o primeiro snapshot de cada arquivo, que a extensão VS Code usa como baseline para seus diffs de sessão. Antes da v2.1.208, esses arquivos de snapshot substituídos permaneciam no disco até que a sessão fosse limpa.
* Os checkpoints são salvos com a conversa, para que uma sessão retomada ainda possa `/rewind` para eles
* Limpeza automática junto com as sessões após 30 dias (configurável)

<h3 id="rewind-and-summarize">
  Rewind e resumo
</h3>

Execute `/rewind`, ou pressione `Esc` duas vezes quando o campo de entrada de prompt estiver vazio, para abrir o menu de rewind.

<Note>
  Se o campo de entrada de prompt contiver texto, duplo `Esc` o limpa em vez de abrir o menu. O texto limpo é salvo no seu histórico de entrada, então pressione `Up` para recuperá-lo após terminar no menu de rewind.
</Note>

O menu de rewind lista cada prompt que você enviou durante a sessão. Selecione o ponto em que deseja agir e escolha uma ação:

* **Restaurar código e conversa**: reverte tanto o código quanto a conversa para esse ponto
* **Restaurar conversa**: reverte para essa mensagem mantendo o código atual
* **Restaurar código**: reverte as alterações de arquivo mantendo a conversa
* **Resumir a partir daqui**: compacta a conversa a partir deste ponto em diante em um resumo, liberando espaço da context window
* **Resumir até aqui**: compacta a conversa antes deste ponto em um resumo, mantendo as mensagens posteriores intactas
* **Nunca importa**: retorna à lista de mensagens sem fazer alterações

Após restaurar a conversa ou escolher Resumir a partir daqui, o prompt original da mensagem selecionada é restaurado no campo de entrada para que você possa reenviá-lo ou editá-lo.

Escolher Resumir até aqui o deixa no final da conversa com a entrada vazia.

<h4 id="rewind-past-a-cleared-conversation">
  Rewind passado uma conversa limpa
</h4>

Se você executou `/clear` anteriormente no mesmo processo Claude Code, o menu de rewind mostra uma entrada adicional no topo da lista rotulada `/resume <session-id> (sessão anterior)`. Selecione-a para retomar a conversa que estava ativa antes de `/clear` ser executado. A entrada está disponível até você sair do Claude Code ou retomar uma sessão diferente, e requer Claude Code v2.1.191 ou posterior. Em versões anteriores, execute `/resume` e escolha a sessão anterior da lista.

<h4 id="restore-vs-summarize">
  Restaurar vs. resumir
</h4>

As opções de restauração revertam o estado: elas desfazem alterações de código, histórico de conversa ou ambos. As opções de resumo compactam parte da conversa em um resumo gerado por IA sem alterar arquivos no disco:

* **Resumir a partir daqui**: as mensagens antes da mensagem selecionada permanecem intactas. A mensagem selecionada e tudo depois dela são substituídos por um resumo. Use isso para descartar uma discussão lateral mantendo o contexto inicial em detalhes completos.
* **Resumir até aqui**: as mensagens antes da mensagem selecionada são substituídas por um resumo. A mensagem selecionada e tudo depois dela permanecem intactas, e você permanece no final da conversa. Use isso para compactar a discussão de configuração inicial mantendo o trabalho recente em detalhes completos.

Em ambos os casos, as mensagens originais são preservadas na transcrição da sessão, para que Claude possa fazer referência aos detalhes se necessário. Você pode digitar instruções opcionais para orientar o que o resumo se concentra. Isso é semelhante ao `/compact`, mas direcionado: em vez de resumir toda a conversa, você escolhe qual lado da mensagem selecionada compactar.

<Note>
  Resumir mantém você na mesma sessão e compacta o contexto. Se você quiser ramificar e tentar uma abordagem diferente enquanto preserva a sessão original intacta, use [fork](https://code.claude.com/docs/pt/sessions#branch-a-session) em vez disso (`claude --continue --fork-session`).
</Note>

<h2 id="common-use-cases">
  Casos de uso comuns
</h2>

Os checkpoints são particularmente úteis quando:

* **Explorando alternativas**: tente diferentes abordagens de implementação sem perder seu ponto de partida
* **Recuperando de erros**: desfaça rapidamente as alterações que introduziram bugs ou quebraram a funcionalidade
* **Iterando em recursos**: experimente variações sabendo que você pode reverter para estados funcionais
* **Liberando espaço de contexto**: resuma uma sessão de depuração verbosa a partir do ponto médio em diante, mantendo suas instruções iniciais intactas

<h2 id="limitations">
  Limitações
</h2>

<h3 id="bash-command-changes-not-tracked">
  Alterações de comando Bash não rastreadas
</h3>

O checkpointing não rastreia arquivos modificados por comandos bash. Por exemplo, se Claude Code executar:

```bash theme={null}
rm file.txt
mv old.txt new.txt
cp source.txt dest.txt
```

Essas modificações de arquivo não podem ser desfeitas através de rewind. Apenas edições diretas de arquivo feitas através das ferramentas de edição de arquivo do Claude são rastreadas.

<h3 id="external-changes-not-tracked">
  Alterações externas não rastreadas
</h3>

O checkpointing rastreia apenas arquivos que foram editados na sessão atual. Alterações manuais que você faz em arquivos fora do Claude Code e edições de outras sessões simultâneas normalmente não são capturadas, a menos que aconteçam de modificar os mesmos arquivos da sessão atual.

<h3 id="not-a-replacement-for-version-control">
  Não é um substituto para controle de versão
</h3>

Os checkpoints são projetados para recuperação rápida no nível da sessão. Para histórico de versão permanente e colaboração:

* Continue usando controle de versão (ex. Git) para commits, branches e histórico de longo prazo
* Os checkpoints complementam mas não substituem o controle de versão adequado
* Pense em checkpoints como "desfazer local" e Git como "histórico permanente"

<h2 id="see-also">
  Veja também
</h2>

* [Modo interativo](https://code.claude.com/docs/pt/interactive-mode) - Atalhos de teclado e controles de sessão
* [Comandos](https://code.claude.com/docs/pt/commands) - Acessando checkpoints usando `/rewind`
* [Referência CLI](https://code.claude.com/docs/pt/cli-reference) - Opções de linha de comando
