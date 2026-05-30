# [TASK-ID] — Título curto da task

> **Não edite este arquivo.** Copie pra `<TASK-ID>-slug-curto.md` (ex: `BE-17-export-csv.md`) e preencha lá.
>
> **Por que este template existe:** o plano é o *contrato de entrada* da task — quanto mais especificado, menor a variância de quem executa (mesmo princípio do status report como *output contract*; ver `docs/aprendizado/structured-outputs.md`). O checklist do planner (`docs/roles/planner.md`) exige que todo plano tenha **origem, critérios de aceite, dependências, riscos, branch e coordenação**. O bloco de Intake abaixo materializa isso.
>
> Mantenha enxuto: preencha o que se aplica, apague o resto. Um plano bom cabe numa leitura.

---

> **Intake (contrato de entrada da task)**
>
> - **Origem:** de onde veio a demanda (item de backlog, incidente, revisão, pedido do humano). Link pro doc/plano/status de origem quando houver.
> - **Prioridade:** alta | média | baixa — e por quê em uma frase.
> - **Esforço:** alto | médio | baixo (estimativa grosseira).
> - **Território / quem executa:** pasta(s) de código + instância responsável (`claude-back` | `claude-front`). Se cruzar territórios, explicar quem faz o quê.
> - **Branch:** `feature/<id>-<slug>` (ou `fix/`, `hotfix/`), **a partir de `develop`**. Uma task = uma branch nova (regra do `CLAUDE.md`). Declarar `> EXCEÇÃO DE BRANCH:` com justificativa se precisar fugir disso.
> - **Dependências:** tasks/condições que precisam estar prontas antes. "Nenhuma" é resposta válida.
> - **Riscos:** o que pode dar errado e a mitigação de cada um. Pelo menos os 1–2 principais.

---

## Contexto

Qual o problema, por que agora, o que está em jogo. O suficiente pra quem executa entender o *porquê* — não só o *o quê*.

## Decisão / abordagem

A escolha técnica central (ferramenta, padrão, caminho) e **por que essa e não as alternativas**. Se houver decisão arquitetural de verdade (cross-cutting, com consequências), ela vira **ADR** (`docs/decisions/`) — o plano referencia o ADR, não o substitui.

## Escopo / arquivos

O que entra e o que **não** entra. Lista de arquivos a criar/modificar (caminhos reais), agrupados por adicionar / modificar / não-tocar.

## Critérios de aceitação

Lista verificável — cada item deve ser checável por alguém que não é quem escreveu. Inclua a **prova** (comando, cenário de teste, evidência) quando der. Tudo que tiver lógica não-trivial precisa de teste (regra do `CLAUDE.md`).

- [ ] Critério 1 (como verificar)
- [ ] Critério 2 (como verificar)

## Coordenação

Com quem essa task conversa (outra instância, outra task em paralelo, decisão do humano). Pontos de sincronização e contratos compartilhados.

## Definição de pronto

Passar pelos gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build, lint, testes, convenção de branch, território) e escrever o status report em `docs/sprints/<NN>/status/<TASK-ID>-*.md` com frontmatter válido (`docs/templates/_TEMPLATE-status.md`). `estado: concluido` só com todos os gates `ok`/`na` e `pendencias_humano: 0`. **Não** mergear sozinho — abrir PR pra `develop` e parar pra revisão (salvo instrução explícita).

## Referências

Planos, ADRs, aprendizados, status reports e docs de arquitetura relevantes.
</content>
