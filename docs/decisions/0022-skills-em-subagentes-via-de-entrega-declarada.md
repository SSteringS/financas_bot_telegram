---
adr: 0022
titulo: "Toda skill citada na prosa de um subagente precisa de via de entrega declarada no frontmatter"
data: 2026-08-16
status: Accepted
decisores: humano
relacionado: [0005, 0015]
supersedes: null
superseded_by: null
---

# ADR 0022 — Toda skill citada na prosa de um subagente precisa de via de entrega declarada no frontmatter

---

## Contexto

Existem **dois mecanismos independentes** pra uma skill chegar a um subagente:

1. **Pré-carga** — o nome da skill no campo `skills:` do frontmatter. Injeta o conteúdo **completo** do `SKILL.md` no contexto do subagente **na inicialização** (`docs/claude/agentes/02-subagentes.md` l. 290: "O conteúdo completo da skill é injetado, não apenas a descrição").
2. **Invocação sob demanda** — o modelo decide chamar a skill durante a execução, usando a ferramenta `Skill`.

O ponto que quebra a intuição: **`skills:` não é controle de acesso.** A mesma doc, na seção "Pré-carregar skills em subagentes" (l. 493), é explícita: o campo controla o que é pré-carregado, não o que o subagente pode acessar; para impedir o acesso, o caminho é omitir `Skill` da lista `tools` ou colocá-lo em `disallowedTools`. Quem barra é a ausência da **ferramenta**, não a ausência do **nome**.

E `tools:`, quando presente, é **allowlist exaustiva** — o que não está listado não existe pro agente; se o campo for omitido, o agente herda todas as built-in (l. 285).

Combinando os dois fatos: um agente com `tools:` explícito **sem** `Skill` e **sem** o nome da skill em `skills:` simplesmente **não alcança aquela skill**, por mais que a prosa do próprio agente mande usá-la.

**A falha é silenciosa.** O único erro de inicialização documentado é quando *nenhuma* entrada de `tools` se resolve (l. 285). Uma skill listada em `skills:` que esteja faltando ou desabilitada é apenas ignorada, com aviso só no log de debug (l. 495). Não existe validação cruzada entre a prosa do agente e o frontmatter. O agente sobe, roda, produz saída plausível e nunca aplica a skill — nem o humano nem o próprio agente notam.

**Incidente real (2026-08-16).** Uma auditoria dos 6 agentes de `.claude/agents/` encontrou **3 defeituosos**. O pior caso foi o `ai-engineer`: toda a seção "Mandatory delegations (do not answer from memory)" nomeava três skills — `harness-workflow-analyst`, `creating-skills`, `creating-agents` — enquanto o frontmatter não tinha a ferramenta `Skill` **nem** campo `skills:`. O agente cuja função é justamente **não** responder de memória só conseguia responder de memória. Ele ficou de fora do commit `105d955`, que distribuiu `skills:` para os outros cinco.

**O validador não pegou.** `scripts/validate_agent.py` (dentro da skill `creating-agents`) retornou `OK` / exit 0 para aquele arquivo quebrado **antes e depois** da correção. Ele valida a forma do frontmatter e a presença das seções do corpo, isoladamente — nunca cruza a prosa contra o frontmatter. Passar no validador era condição necessária e claramente insuficiente.

**Restrição de território:** `.claude/` é território do humano e do `ai-engineer` (`CLAUDE.md`, raiz). O planner registra a regra; a materialização em `.claude/agents/` e no `validate_agent.py` não é dele.

---

## Decisão

### 1. Toda skill citada na prosa de um subagente precisa de via de entrega declarada no frontmatter

Se o corpo do arquivo do agente nomeia uma skill, o frontmatter tem que garantir que ela chegue, por um dos dois caminhos:

- **pré-carga** — nome listado em `skills:`; ou
- **sob demanda** — `Skill` presente em `tools:` (ou `tools:` omitido, o que herda tudo).

Citar sem nenhuma das duas é defeito, não estilo.

### 2. O arquivo do agente declara, em prosa, qual mecanismo carrega cada skill

Não basta o frontmatter estar certo: o corpo diz qual é a via. É o padrão que `planner.md` e `backend.md` já usam — quem lê o agente entende se aquela skill já está no contexto desde o primeiro turno ou se depende de uma decisão de invocação.

### 3. Critério de escolha entre os dois mecanismos

Regra citada de `.claude/skills/creating-agents/FRONTMATTER.md:87-93`: pré-carga para skills "cujo conteúdo o subagente precisa desde o primeiro turno"; skills que só seriam acionadas por descoberta "devem ficar lazy".

Traduzindo pro nosso uso:

| Situação | Mecanismo |
|---|---|
| A skill é necessária em **toda** execução, desde o primeiro turno | pré-carga (`skills:`) |
| A skill é acionada por uma **condição** (tipo de task, achado, delegação) | sob demanda (`Skill` em `tools:`) |

Os dois podem coexistir no mesmo agente, para skills diferentes.

### 4. `validate_agent.py` ganha regra de validação cruzada

O validador passa a extrair os nomes de skill citados na prosa e a confrontá-los com o frontmatter, **falhando** quando uma skill nomeada está inalcançável pelos dois mecanismos. Passar no validador era necessário e não suficiente — esta regra fecha exatamente essa lacuna.

Materialização em `.claude/`, portanto **território do `ai-engineer`**. Até existir, a verificação é manual.

---

## Razões

- **A falha não tem sintoma.** Não há erro de inicialização, não há degradação visível de saída, não há log fora do modo debug. Um defeito sem sintoma só é pego por regra escrita e por validador — não por observação.
- **O caso `ai-engineer` mostra que o alvo preferencial é o agente mais crítico.** Quanto mais um agente depende de delegar em vez de responder de memória, mais dano causa quando a delegação não acontece — e mais plausível fica a saída errada, porque o modelo tem contexto suficiente pra parecer competente.
- **Declarar a via em prosa (§2) protege contra edição futura.** Quem editar `tools:` daqui a três meses precisa ver, no mesmo arquivo, que alguma skill dependia dali. Frontmatter correto sem a declaração é correto por acidente.
- **`skills:` como falso controle de acesso é armadilha de nome.** O campo se chama `skills` e listar coisas nele *parece* conceder acesso. Escrever a regra é mais barato do que esperar que cada pessoa releia a l. 493 da doc.
- **Mesmo princípio da ADR 0007:** schema válido não garante verdade. Aqui: validador verde não garante agente funcional. A correção é a mesma — mover a verificação pra onde a mentira mora, que neste caso é a distância entre prosa e frontmatter.

---

## Consequências

**Positivas:**

- Skill citada é skill que chega. Some a classe inteira de defeito "o agente manda usar X e não consegue usar X".
- A prosa do agente vira documentação verificável do próprio contrato de contexto, em vez de intenção.
- O `validate_agent.py` passa a ter poder de reprovar por algo semântico, não só por forma.
- Os 3 agentes defeituosos da auditoria de 2026-08-16 ficam cobertos por regra, não por memória de quem auditou.

**Negativas / custos:**

- **Pré-carga cobra token em TODO spawn.** O `SKILL.md` completo é injetado na inicialização; cada subagente nomeado mantém **cache de prompt separado** (`02-subagentes.md` l. 1023); e após compactação as skills são **truncadas em 5.000 tokens cada**, dentro de um orçamento de 25.000 tokens (`docs/claude/skills/01-skills.md` l. 397). Skill grande pré-carregada pode chegar cortada.
- **Pré-carregar abre mão explicitamente do "baixo custo de contexto até ser usada"**, que é a propriedade que torna skill barata (`docs/claude/harness/03-recursos-disponiveis.md` l. 271-273). §3 existe pra que essa troca seja consciente e não default.
- **Nenhum dos dois mecanismos carrega assets.** `references/`, `examples/`, `templates/`, `scripts/` **não** entram em contexto em modo nenhum — só o `SKILL.md`. O modelo segue o link markdown e lê com `Read`. Logo, pré-carregar **não** dá alcance extra sobre assets; o trade-off entre os mecanismos é puramente determinismo × tokens, nunca cobertura de conteúdo.
- **Mudança em `.claude/agents/` só vale a partir da PRÓXIMA sessão** — configuração de agente é lida no spawn. Corrigir um agente não conserta a sessão em curso.
- **Custo de disciplina no `ai-engineer`:** toda edição de prosa de agente que introduza nome de skill passa a exigir conferência do frontmatter, até a regra do §4 existir.

**Risco RESOLVIDO — verificado em runtime pela QA-015 em 2026-08-16, no mesmo dia desta ADR:**

> ⚠️ **O risco descrito abaixo era hipótese quando esta ADR foi escrita. Deixou de ser: ele se confirma.** A QA-015 rodou a verificação que este parágrafo pedia e o resultado é conclusivo — **o path relativo NÃO é ancorado no diretório da skill; ele resolve contra o diretório de trabalho da sessão** (a raiz do repo).
>
> **A evidência que fecha a questão** é um par falha/sucesso sobre o **mesmo arquivo existente**: `references/test-doubles-guidelines.md`, escrito exatamente como o `SKILL.md` de `writing-java-unit-tests` o publica (linhas 52 e 73), **falhou** com `File does not exist. Note: your current working directory is C:\Users\satya\src\financas_bot_telegram`; o path absoluto até `.claude/skills/writing-java-unit-tests/references/…` **funcionou**, 29 linhas lidas. Uma primeira tentativa com nome de arquivo chutado foi corretamente **descartada** pelo implementador como evidência ambígua.
>
> **Consequência para o §1 desta ADR:** a garantia de entrega vale para o corpo do `SKILL.md`, e **não** se estende aos assets — hoje um `SKILL.md` entregue corretamente aponta para o vazio em **15 links** (10 em `developing-java-spring-applications`, 5 em `writing-java-unit-tests`), que é onde estão todos os exemplos de código.
>
> **Correção candidata:** âncora explícita — `${CLAUDE_SKILL_DIR}` ou path a partir da raiz do repo. **Não aplicada:** mexe em `.claude/`, e exige autorização explícita do humano, que não foi pedida nem dada. Registrado em `docs/PENDENCIAS-TECNICAS.md`.
>
> **Nota de honestidade sobre o que a coleta NÃO provou:** o agente registrou não conseguir afirmar se o corpo dos `SKILL.md` pré-carregados estava em seu contexto — não há como um agente inspecionar o próprio contexto para provar ausência. O sinal indireto (precisou ler `_TEMPLATE-status.md` do disco, o que seria redundante se o `artifact-report-contract` estivesse em contexto) é **incerteza registrada, não conclusão**. A §1 desta ADR permanece apoiada na documentação, não em observação de runtime.

Texto original do risco, mantido para registro:

A doc oficial **nunca** diz como um link markdown relativo dentro de um `SKILL.md` (ex.: `references/mvc-architecture.md`) se resolve pra um caminho em disco. A substituição `${CLAUDE_SKILL_DIR}` é documentada apenas para injeção em bash (`docs/claude/skills/01-skills.md` l. 302). A única regra documentada de "relativo resolve contra o arquivo que contém" está escopada aos imports `@path` do CLAUDE.md (`docs/claude/configuracao/07-memoria-e-claude-md.md` l. 109) e **não** é estendida a skills.

Isso pesa porque as duas skills de Java concentram a maior parte do conteúdo em assets: em `developing-java-spring-applications`, `references/` + `examples/` somam ~3/4 dos bytes; em `writing-java-unit-tests`, cerca de 60%. Se o caminho não resolver, o agente recebe a política sem os padrões — e a §1 desta ADR terá garantido entrega de um `SKILL.md` que aponta pro vazio. ~~**Pendente de verificação em runtime na próxima task Java.**~~ → **Verificado e confirmado pela QA-015; ver o bloco acima.**

**Métricas pra avaliar adoção:**

- **Baseline (2026-08-16):** 3 de 6 agentes de `.claude/agents/` com pelo menos uma skill citada e inalcançável; `validate_agent.py` verde em todos.
- **Alvo:** 0 agentes nessa condição; `validate_agent.py` reprovando o arquivo do `ai-engineer` pré-correção quando rodado contra a versão do commit anterior a esta ADR (teste de regressão da própria regra).
- **Critério de parada:** se a regra do §4 gerar falso positivo em 2 agentes distintos (nome de skill citado em contexto que não é instrução de uso — ex.: prosa explicando o ecossistema), o extrator de nomes está grosseiro demais e precisa de marcação explícita no texto, não de mais heurística.

---

## Alternativas consideradas

- **Omitir `tools:` em todos os agentes (herda tudo, inclusive `Skill`):** descartado — resolve o alcance ao custo de jogar fora o princípio de menor privilégio que sustenta a especialização por papel (ADR 0005). O `reviewer` passaria a ter `Write` e `Bash` irrestritos por efeito colateral de uma correção sobre skills.
- **Pré-carregar tudo que é citado (`skills:` com todas):** descartado — paga token em todo spawn por skill condicional que talvez nunca seja usada, e esbarra no truncamento de 5.000 tokens/skill após compactação. Vira degradação silenciosa no lugar de ausência silenciosa.
- **Só `Skill` em `tools:`, nunca pré-carga:** descartado — para skill que precisa valer desde o primeiro turno, invocação sob demanda depende de o modelo *decidir* invocar. Onde a aplicação é obrigatória, determinismo vale o token.
- **Confiar no `validate_agent.py` como está:** descartado — é literalmente a dor. Ele deu `OK` no `ai-engineer` quebrado antes e depois da correção.
- **Só documentar o aprendizado, sem ADR:** descartado pelo humano — o aprendizado explica o mecanismo, mas não obriga ninguém. A auditoria mostrou que entender a doc não impediu que 3 de 6 agentes ficassem quebrados.

---

## Referências

- **ADR 0005** — sessões especializadas por papel. É o princípio de menor privilégio que impede a alternativa "omitir `tools:` em todos".
- **ADR 0015** — taxonomia roles/skills/workflows. Define o que é skill neste projeto; esta ADR cuida de como ela chega ao agente.
- **ADR 0007** — schema válido não garante verdade. Mesmo raciocínio aplicado a validador verde × agente funcional.
- **Substrato conceitual:** `docs/aprendizado/skills-em-subagentes-preload-vs-sob-demanda.md` — mecânica completa dos dois modos, por que a falha é silenciosa, e por que assets não são carregados em modo nenhum.
- **Fontes na doc oficial:** `docs/claude/agentes/02-subagentes.md` l. 285 (`tools` como allowlist exaustiva; único erro de inicialização), l. 290 (pré-carga injeta o SKILL.md completo), l. 493 e l. 495 (`skills:` não é controle de acesso; skill faltante só avisa em debug), l. 1023 (cache de prompt por subagente) · `docs/claude/skills/01-skills.md` l. 302 (`${CLAUDE_SKILL_DIR}`), l. 328 (assets existem pra não carregar), l. 393 (invocação injeta só o SKILL.md), l. 397 (truncamento em 5.000/25.000 tokens) · `docs/claude/harness/03-recursos-disponiveis.md` l. 271-273 (baixo custo de contexto até o uso).
- **Regra de escolha citada:** `.claude/skills/creating-agents/FRONTMATTER.md:87-93`.
- **Evidência do incidente:** commit `105d955` (distribuiu `skills:` para 5 dos 6 agentes, deixando o `ai-engineer` de fora) · auditoria dos 6 agentes de `.claude/agents/`, 2026-08-16.
- **Materialização (território do humano + `ai-engineer`):** `.claude/agents/*.md` (declaração da via em prosa, §2) · `.claude/skills/creating-agents/scripts/validate_agent.py` (regra de validação cruzada, §4) · `.claude/skills/creating-agents/FRONTMATTER.md` (critério do §3, já presente).
