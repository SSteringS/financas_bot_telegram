# Structured outputs (saídas estruturadas de LLM)

## Contexto da dúvida

Durante o estudo do livro *AI Engineering: Building Applications with Foundation Models* (Chip Huyen), surgiu a pergunta: dá pra aplicar o conceito de **structured outputs** no fluxo de trabalho do finbot? A discussão aconteceu enquanto o Claude do back executava o DEP-01, num momento de pausa conceitual.

**Escopo do interesse (importante):** a aplicação pretendida é no **fluxo de desenvolvimento com os Claudes** (processo), **não** colocar um LLM dentro da aplicação finbot. A seção do parser do bot fica registrada como aplicação possível-mas-não-agora; o foco é a aplicação meta (workflow).

## Resumo destilado

**Structured output** = forçar o LLM a produzir saída num formato definido (JSON conforme schema, um enum, ou os argumentos de uma função). O ponto central é **onde** a estrutura é imposta, porque a garantia muda:

- **No prompt** ("responda em JSON com X, Y"): barato, mas sem garantia — o modelo escorrega.
- **Constrained decoding / JSON schema mode**: a API mascara, durante a geração, os tokens que violariam o schema. Garante a **forma**.
- **Pós-processamento + validação + retry**: valida o que voltou e re-pede se quebrou.
- **Function/tool calling**: é structured output disfarçado — o modelo devolve "qual função + quais argumentos" num formato fixo.

Princípio que governa tudo: **structured output garante a SINTAXE, não a SEMÂNTICA.** O JSON vem bem-formado; o valor dentro pode estar errado. Em domínio de dinheiro (como o finbot), isso obriga uma camada extra (validação + confirmação humana).

### Aplicação principal: o workflow de Claudes (planejamento ↔ implementadores)

Os artefatos do nosso fluxo são todos "saídas de agente" consumidas por outro agente — e quase todos são texto livre hoje:

- **Planos** (`docs/plans/`): eu produzo, o implementador consome → *input contract*.
- **Status reports** (`docs/status/`): o implementador produz, eu/humano consumimos → *output contract*.
- **Avaliações** (`docs/avaliacoes/`): eu produzo revisando o trabalho.
- **Branch/commit**: já têm convenção = já são um "schema".

Onde structured output se aplica:

**1. Status report como output schema.** Definir frontmatter obrigatório (`tarefa, branch, testes_passando(int), build(ok/fail), lint(ok/fail), veredito, desvios[]`) + corpo livre. Ganhos: (a) vira **agregável** — um script lê todos os `docs/status/*.md` e monta um painel; (b) o schema vira **checklist que força** o implementador a reportar o essencial — campo faltando = violação visível.

**2. O plano é o schema de entrada.** Quanto mais especificado, menor a variância do implementador — igual a um JSON schema reduzir a variância do LLM. O **incidente da FE-12** (branch errada) foi um campo mal-especificado no schema (seção Branch contradizia a convenção): input ambíguo → saída errada. A correção (regra de precedência no CLAUDE.md) foi desambiguar o schema.

**3. CLAUDE.md é o nosso "constrained decoding".** Constrained decoding mascara, na geração, os tokens que violam a gramática. O CLAUDE.md restringe o espaço de comportamento aceitável (território, branch, commit) — a gramática que todo Claude respeita enquanto "gera" trabalho. A regra de precedência adicionada = desambiguação dessa gramática.

**4. Eval do processo: separar sintaxe de semântica.** As avaliações ganham clareza separando **conformidade de processo** (branch certa? status report? 1 commit/tarefa? = sintaxe) de **qualidade do trabalho** (arquitetura, testes, correção = semântica). São falhas de natureza diferente.

### Sintaxe ≠ semântica (vale pro workflow também)

Um status report pode seguir o schema perfeitamente e ainda **mentir** ("226 testes passando" pode ser falso). O schema dá um report *parseável*, não *verdadeiro*. Por isso existem as avaliações e a insistência em **verificação independente** (reler diff, rodar testes de novo) em vez de confiar no autorrelato. No fluxo: o schema é o status report; a validação é a revisão do Claude de planejamento.

### Aplicação secundária (NÃO agora): parser do bot

Registrado pra não perder: o despacho por regex do bot poderia virar parser com structured output (`{ intencao, valor, descricao, tipo, pedido_id, confianca }`, enums garantidos), resolvendo o débito do "comprovante sem `#` vira pedido fantasma". **Mas não agora** — há um único usuário treinado, regex é grátis/determinístico, e seria over-engineering. Gatilho real: entrada natural/variada (EVO-01 WhatsApp, EVO-03 OCR). Padrão correto por ser dinheiro: `schema mode + confiança + confirmação inline`.

### A ponte com validação (Zod / Bean Validation)

Saída de LLM = **input externo não-confiável**, igual a resposta de API ou query param. "Valide na fronteira com um schema" (que a avaliação do front cobrou via Zod) é a mesma disciplina de validar structured output antes de confiar. O schema não dispensa validação — dá o formato pra validar contra.

## Pontos-chave

- Structured output pode ser imposto em 4 lugares: prompt (fraco), constrained decoding/JSON schema mode (garante forma), validação+retry, function/tool calling.
- **Garante sintaxe, não semântica** — vale tanto pra LLM quanto pro workflow: um status report schema-válido ainda pode mentir.
- **Aplicação principal = o workflow de Claudes**, não o produto: planos = schema de entrada; status reports = output schema (agregável + checklist forçado); CLAUDE.md = "constrained decoding" do comportamento; avaliação deve separar conformidade de processo (sintaxe) de qualidade (semântica).
- O incidente da FE-12 = campo ambíguo no "schema" (Branch) → saída errada; fix = desambiguar a gramática (precedência no CLAUDE.md).
- Verificação independente > autorrelato: o schema dá report parseável, a revisão dá verdade.
- Aplicação secundária (parser do bot) fica pra quando a entrada virar natural/variada (EVO-01); hoje regex resolve, trocar é over-engineering.
- Saída de LLM = input não-confiável → **sempre validar na fronteira** (mesma lógica do Zod/Bean Validation).

## Pra aprofundar

- Constrained/guided decoding na prática: bibliotecas como Outlines, Guidance, ou o "structured outputs" nativo da OpenAI (JSON Schema) e o tool use da Anthropic.
- Trade-off latência/custo/determinismo de LLM vs. parser tradicional.
- Human-in-the-loop e design de confirmação pra ações de alto risco (dinheiro).
- Evals de structured output: medir taxa de schema-válido **e** taxa de acerto semântico separadamente.
- Relação com `react-tanstack-query.md` e o débito de validação Zod (validação de fronteira).
