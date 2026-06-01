# Spec efêmera vs. arquitetura durável

## Contexto da dúvida

Surgiu na sessão do arquiteto em 2026-06-01, ao avaliar se o documento `docs/architecture/desenho-testes-automatizados.md` era suficiente pro planner escrever os planos de task QA-NNN da Fase 1 da suíte E2E.

A dúvida operacional era simples: "está pronto pra dar pro planner?". A resposta envolveu uma decisão de **onde** escrever o que falta — refinar o documento durável existente, ou criar um documento novo? A escolha trouxe à tona a régua que distingue os dois tipos de artefato.

## Resumo destilado

O projeto tem dois lugares para spec técnica, com propósito e ciclo de vida diferentes:

- **Arquitetura durável** (`docs/architecture/<topico>.md`): captura o **estado-alvo** que vai continuar sendo verdade depois que sprints específicas passarem. Vale a régua "isso ainda vai ser verdade daqui a 6 meses?" do `architecture/README.md`. Exemplo: `desenho-testes-automatizados.md` define a pirâmide de testes, ferramentas, decisões base — tudo vale enquanto o projeto existir e ninguém mudar de stack.
- **Spec efêmera por sprint** (`docs/sprints/<NN>/specs/<slug>.md`): captura o **input concreto pro planner** quebrar em tasks de **uma sprint específica**. Tem IDs propostos, dependências, critérios de aceite por task, sequenciamento intra-sprint. Vive enquanto a sprint vive; depois é histórico (vira artefato de rastreabilidade da decisão, mas não é "fonte da verdade" mais).

Tentar misturar os dois leva a dois sintomas comuns:

1. **Arquitetura durável vira to-do list.** Se o documento durável tem uma seção "Fase 1: 7 tasks pra fazer", essa seção vira fóssil quando a Fase 1 entrega. Quem ler daí a 3 sprints fica confuso ("isso já foi feito?"). O documento durável precisa **permanecer atemporal**.
2. **Spec efêmera vira tratado conceitual.** Se a spec da sprint repete trade-offs entre ferramentas, glossário de Playwright, etc., ela fica longa e o planner perde tempo lendo o que já estava decidido. O planner quer **mastigar tasks**, não reler arquitetura.

O padrão acertado é: arquitetura durável **decide e justifica**; spec efêmera **operacionaliza** referenciando a arquitetura por path. Cada uma faz uma coisa só, e bem.

## Pontos-chave

- **Régua da arquitetura durável:** "isso vai ser verdade em 6 meses?" Se sim, fica em `docs/architecture/`. Se não, é efêmero.
- **Régua da spec efêmera:** "isso é input pro planner desta sprint?" Se sim, vai em `docs/sprints/<NN>/specs/`. Se vai continuar valendo depois da sprint, talvez seja arquitetura.
- **Acoplamento por referência, não por duplicação:** a spec efêmera **cita** seções da arquitetura por path (`§7.2 de desenho-testes-automatizados.md`), não copia.
- **Inconsistências internas no documento durável devem ser corrigidas no próprio durável** — nunca contorná-las na spec efêmera. Senão a próxima leitura do durável propaga erro.
- **Decisões que duram (ex.: prefixo novo de task) viram ADR.** Não ficam só na spec efêmera nem no durável — ADR é o lugar canônico de decisão arquitetural (ADR 0004).
- **O fluxo padrão:** humano levanta dor → arquiteto refina spec efêmera (input pro planner) → planner escreve N planos `BE-NNN/FE-NNN/QA-NNN` copiando `_TEMPLATE-plano.md` → implementador executa. Espelha o que aconteceu com EVO-09 (spec `sprints/03/specs/evo-09-folha-pagamento.md` → planos `BE-023..BE-029, FE-015..FE-017`).
- **Arquitetura ganha "Atualização YYYY-MM-DD" quando precisa de correção retroativa** — sem reescrever o histórico, mas deixando claro o que mudou e por quê.

## Aplicação concreta — caso 2026-06-01

O `desenho-testes-automatizados.md` (durável) tinha:

- ✅ Decisões consolidadas com trade-offs
- ✅ Pirâmide-alvo
- ✅ Código exemplo das 3 specs
- ❌ Inconsistência interna (Sec.10 listava task de back de seed que a Decisão 8 eliminou) → **corrigido no próprio durável**
- ❌ Nomes de tabela desalinhados com schema real → **corrigido no próprio durável**
- ❌ Decisões pendentes não classificadas por urgência → **corrigido no próprio durável**

O que faltava era operacionalização específica da Fase 1 (IDs `QA-NNN` propostos, dependências, lotes, critérios de aceite por task) → **escrito em `sprints/03-folha-pagamento/specs/qa-suite-e2e-fase1.md`** (efêmera, vive na sprint, morre quando a Fase 1 entregar).

Como o prefixo `QA-NNN` é decisão de governance que afeta CLAUDE.md e templates → **virou ADR 0017 `Proposed`** (canônico, separado).

Três artefatos com 3 propósitos distintos, cada um no seu lugar. Nenhuma duplicação.

## Pra aprofundar

- ADR 0004 §taxonomia: onde cada tipo de artefato vive e quem é dono.
- `docs/architecture/README.md` §"O que NÃO fica aqui": critério de distinção explícito.
- `docs/aprendizado/taxonomia-agent-skill-workflow.md`: análoga com fronteira agent/skill/workflow.
- ADR 0010 (organização doc por sprint): contexto da pasta `sprints/<NN>/specs/`.
