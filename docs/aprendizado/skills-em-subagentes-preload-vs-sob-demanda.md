# Skills em subagentes — `skills:` é pré-carga, não permissão

## Contexto da dúvida

Revisando `.claude/agents/backend.md` (2026-08-16), notei que a seção em prosa
`## Skills to Apply` citava duas skills que **não estavam** no campo `skills:` do
frontmatter YAML. A pergunta foi: o agente ainda consegue invocar essas skills?

A investigação virou auditoria dos agentes do repo e revelou um defeito silencioso
em três deles. O caso mais limpo é o `planner`, cuja prosa afirma literalmente que
`reviewing-code-premises` "is invoked on demand" — só que o `tools:` do agente não
lista a ferramenta `Skill`, então essa invocação nunca poderia acontecer.

## Resumo destilado

Existem **dois mecanismos independentes** pra uma skill chegar a um subagente, e o
campo `skills:` do frontmatter é só um deles.

**1. Pré-carga (`skills:` no frontmatter).** Injeta o conteúdo **completo** do
`SKILL.md` no contexto do subagente **na inicialização**. `docs/claude/agentes/02-subagentes.md`,
tabela de frontmatter, linha `skills` (l. 290): "O conteúdo completo da skill é
injetado, não apenas a descrição."

**2. Invocação sob demanda (ferramenta `Skill`).** O modelo decide chamar a skill
durante a execução. Depende de o agente **ter** a ferramenta `Skill`.

O campo `skills:` **não é controle de acesso**. Mesma doc, seção "Pré-carregar
skills em subagentes" (l. 493): "Este campo controla quais skills são
pré-carregadas, não quais skills o subagente pode acessar (...). Para impedir que
um subagente invoque skills inteiramente, omita `Skill` da lista `tools` ou
adicione-o a `disallowedTools`." Ou seja: quem barra o acesso é a ausência da
ferramenta `Skill`, não a ausência do nome em `skills:`.

E `tools:`, quando presente, é **allowlist exaustiva** — o que não está listado não
existe pro agente. Se omitido, o agente herda todas as ferramentas built-in
(`02-subagentes.md` l. 285).

Combinando os dois fatos: um agente com `tools:` explícito **sem** `Skill` e **sem**
o nome da skill em `skills:` simplesmente **não alcança aquela skill** — por mais
que a prosa do agente mande usá-la.

### Por que ninguém percebe

A falha é **silenciosa**. O único erro de inicialização documentado é quando
*nenhuma* entrada de `tools` se resolve (l. 285) — e mesmo uma skill listada em
`skills:` que esteja faltando ou desabilitada é apenas ignorada, com aviso só no log
de debug (l. 495). Não há validação cruzada entre a prosa do agente e o frontmatter.
O agente roda, produz saída plausível e nunca aplica a skill; nem o humano nem o
próprio agente notam.

### A parte contraintuitiva: assets não são carregados em nenhum dos dois modos

Dá pra imaginar que pré-carregar "traz a skill inteira" e que invocar sob demanda
daria acesso melhor aos arquivos de apoio. **Errado nos dois lados** — os dois modos
tratam assets exatamente igual.

- Invocar uma skill coloca "o conteúdo `SKILL.md` renderizado" na conversa "como uma
  única mensagem" (`docs/claude/skills/01-skills.md`, seção "Ciclo de vida do
  conteúdo de skill", l. 393). **Só o SKILL.md.**
- Arquivos de apoio existem justamente pra **não** carregar: "Documentos de
  referência grandes, especificações de API, ou coleções de exemplos não precisam
  carregar em contexto toda vez que a skill é executada" (mesma doc, seção "Adicione
  arquivos de suporte", l. 328). O modelo segue o link markdown e lê o arquivo com
  `Read` quando precisa; scripts são "executado, não carregado".

Logo: `references/`, `examples/`, `templates/`, `scripts/` chegam do **mesmo jeito**
nos dois modos — modelo segue o link e lê. Ler um asset exige `Read`, **não** exige a
ferramenta `Skill`.

**Consequência prática:** pré-carregar não perde acesso a asset nenhum em relação a
invocar sob demanda. O trade-off entre os modos é só sobre determinismo vs. tokens,
nunca sobre alcance de conteúdo.

### Risco em aberto (NÃO CONFIRMADO)

A doc **nunca** diz como um link relativo dentro do `SKILL.md`
(`references/mvc-architecture.md`) se resolve pra um caminho em disco. A própria
existência da substituição `${CLAUDE_SKILL_DIR}` — descrita como servindo pra
"referenciar scripts ou arquivos agrupados com a skill, independentemente do
diretório de trabalho atual" (`docs/claude/skills/01-skills.md`, seção
"Substituições de string disponíveis", l. 302) — sugere que caminhos relativos
**não** são ancorados automaticamente no diretório da skill.

Isso importa aqui porque as duas skills de Java concentram a maior parte do conteúdo
em assets: em `developing-java-spring-applications`, `references/` + `examples/`
somam ~3/4 dos bytes (todos os exemplos de código estão lá) e em
`writing-java-unit-tests`, cerca de 60%. Se o caminho não resolver, o agente recebe a
política sem os padrões. **Pendente de verificação em runtime na próxima task Java.**
