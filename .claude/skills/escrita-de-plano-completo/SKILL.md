---
name: escrita-de-plano-completo
description: Como escrever um plano de task técnico e completo que o implementador executa sem lacunas. Carregar quando o plano envolve feature nova, decisão técnica não óbvia, múltiplas camadas, ou risco de interpretação ambígua. Para tasks simples com precedente claro no repo (ex.: "adiciona campo Y seguindo o padrão Z"), o template sozinho é suficiente — não carregar esta skill.
load_pattern: contextual
used_by: [planner]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — Escrita de Plano Completo

## Quando carregar

- Feature nova sem precedente no repo (nova entidade, nova integração, novo canal).
- Task que toca múltiplas camadas ou múltiplos arquivos não-óbvios.
- Critérios de aceite difíceis de derivar sem entender o código existente.
- Risco de interpretação ambígua sobre onde algo deve morar.
- **Não carregar** para tasks simples com precedente claro — o template sozinho é suficiente.

## Relação com o template (importante)

`docs/templates/_TEMPLATE-plano.md` define a **estrutura**: campos obrigatórios, seções, ordem.
Esta skill define o **raciocínio**: como preencher cada campo de forma que o implementador não precise adivinhar.

```
Template  = OBRIGATÓRIO sempre (estrutura)
Esta skill = COMPLEMENTO quando o preenchimento não é óbvio (raciocínio)
```

Os dois se usam juntos: abrir o template, preencher campo a campo usando o raciocínio desta skill.

## Passo 1 — Entender onde a feature mora

Antes de escrever uma linha do plano, responder:

- Qual(ais) camada(s) serão modificadas?
- Existe porta/adapter similar que serve de padrão?
- Existe migration de banco? Qual é o risco de conflito com outras branches ativas?
- A task tem dependência de outra branch não mergeada?

Se a task toca `application/`, `domain/`, adapters novos ou cruza fronteiras de camada → **carregar skill `arquitetura-hexagonal`** antes de continuar. O plano não pode definir critérios de aceite em features hexagonais sem entender as regras de dependência.

Se a decisão técnica está em aberto (qual padrão usar, onde a feature mora) → chamar `@architect` como subagente antes de finalizar o plano.

## Passo 2 — Critérios de aceite verificáveis

**Anti-pattern:** "o endpoint funciona corretamente" — não verificável.

**Padrão:** critério que um Reviewer independente checa sem falar com o implementador.

Exemplos concretos:
- ✅ "`GET /pedidos?mes=2025-01` retorna 200 com campo `resumo.totalMes` (double ≥ 0) — teste unitário cobre caso vazio e caso com 3 pedidos."
- ✅ "Migração cria tabela `mensagem_processada` com `wamid VARCHAR(255) NOT NULL UNIQUE` — Flyway valida na inicialização."
- ✅ "`npm test` verde, `npm run build` sem erro TS — nenhum `as` sem validação no diff."
- ❌ "Implementar o serviço de resumo." (o quê exatamente?)
- ❌ "Testes passando." (quais testes? qual cobertura?)

Regra: critério de aceite = o que o Reviewer vai checar. Se o Reviewer não consegue verificar lendo o diff + rodando testes, o critério está vago.

## Passo 3 — Riscos reais (não genéricos)

**Anti-pattern:** "risco de bug" ou "risco de quebrar algo" — todo código tem isso.

**Padrão:** risco específico desta task com mitigação concreta.

Exemplos:
- ✅ "Migration conflita com BE-21b se mergarem na mesma janela → implementador deve fazer `git pull develop` antes de criar a migration."
- ✅ "`@ControllerAdvice` global pode mascarar a exception específica — verificar escopo antes de adicionar handler (lição BE-15b)."
- ✅ "RestClient builder configurado diferente de outros services — verificar `RestClientConfig` antes de adicionar novo."
- ❌ "Pode haver problemas de performance." (quando? em qual volume? o que fazer?)

## Passo 4 — Território e quem executa

Deixar explícito:
- Pastas que o implementador **pode** tocar.
- Pastas que **não deve** tocar (especialmente se a task está perto de uma fronteira).
- Se a task toca `infra/` além de `financas_bot_telegram/` → alertar: são dois territórios; considerar separar em duas tasks.

## O que o planner NÃO especifica

- Detalhes de implementação (qual método chamar, como estruturar o loop) — isso é do implementador.
- Boilerplate de boas práticas (usar DI, escrever testes) — já está em CLAUDE.md e no role do agente.
- Sequência interna de commits — o agente sabe que é 1 commit por task.

Sinal de micromanaging: o planner está escrevendo o "como" em vez do "o quê". Parar e perguntar se não é decisão técnica que deveria ir pro `@architect`.

## Checklist de plano completo

- [ ] Critério de aceite verificável por Reviewer independente?
- [ ] Riscos são específicos desta task (não genéricos)?
- [ ] Território explícito (pastas que toca e não toca)?
- [ ] Branch declarada (`feature/<id>-<slug> a partir de develop`)?
- [ ] Dependências de outras tasks/branches explicitadas?
- [ ] Task toca camadas? → carregou skill `arquitetura-hexagonal`?
- [ ] Decisão técnica aberta? → chamou `@architect` antes de finalizar?

## Ler junto

- `docs/templates/_TEMPLATE-plano.md` — estrutura obrigatória (sempre usar junto)
- Skill `arquitetura-hexagonal` — quando a feature cruza camadas hexagonais
- Skill `escrita-de-dispatch` — o dispatch que vem depois do plano
