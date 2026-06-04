---
adr: 0019
titulo: "Workflow reviewer→QA com loop de correção e limites de rounds"
data: 2026-06-04
status: Accepted
decisores: humano
relacionado: [0004, 0005, 0017]
supersedes: null
superseded_by: null
---

# ADR 0019 — Workflow reviewer→QA com loop de correção e limites de rounds

---

## Contexto

O workflow atual (ADR 0005) define que toda task passa por revisão independente antes do merge, mas não especifica:

1. **Como o implementador chama o subagente reviewer** — sem protocolo de chamada, o prompt do implementador pode carregar viés involuntário (framing positivo, pré-justificativa de decisões) que ancora o reviewer antes de começar.

2. **O que acontece quando o reviewer reprova** — o fluxo atual não define o loop de correção; na prática o implementador corrige e reabre o PR sem nova rodada formal de review.

3. **Quando e como o QA entra** — ADR 0017 criou o prefixo `QA-NNN` e o campo `fluxos_qa` nos planos, mas não definiu onde o QA encaixa no ciclo de vida de cada task. O QA hoje age em tarefas dedicadas de tooling, não como gate por task.

4. **Comportamento em batches/overnight** — o overnight da sprint 03 encadeou 6 tasks sem nenhum loop reviewer→QA entre elas; o review aconteceu só depois, externamente. Isso adiou a detecção de problemas.

**Sinal observado:** na sprint 03, a inconsistência entre `domain/vo/` e `domain/enums/` foi detectada pelo humano no code review manual — não pelo reviewer automatizado. Diagnóstico: o reviewer foi chamado pelo próprio implementador sem protocolo de chamada neutro, gerando viés de confirmação mesmo em sessão tecnicamente isolada.

---

## Decisão

### 1. Workflow padrão por task (isolada ou dentro de batch)

Toda task percorre o seguinte ciclo **antes de abrir o PR para a integration branch**:

```
implementar
    ↓
[ROUND reviewer]
    ↓ encontrou issues críticos?
   sim → implementar correção → voltar ao [ROUND reviewer]  (máx. 3 rounds no total)
    ↓ não (OK ou só observações não-bloqueantes)
[ROUND QA]  ← só se fluxos_qa ≠ [] no plano
    ↓ encontrou issues críticos?
   sim → implementar correção → [ROUND reviewer] → [ROUND QA]  (máx. 2 loops completos)
    ↓ não (aprovado ou aprovado_com_ajustes)
push + PR → integration_branch
```

**Definição de "issue crítico":** qualquer item que o reviewer classifica como bloqueante para o merge, ou que o QA classifica como `CRITICO` na seção 7.3 da avaliação.

**Se `fluxos_qa: []` no plano:** o round QA não acontece. O ciclo é apenas reviewer (máx. 3 rounds) → push.

### 2. Limites de rounds e comportamento no limite

| Ciclo | Limite | Comportamento ao estourar |
|---|---|---|
| Reviewer sozinho | 3 rounds | Parar. Status report com `estado: bloqueado`. Descrever o que o round 3 encontrou. Não abrir PR. Escalar ao humano. |
| Loop reviewer+QA completo | 2 loops | Parar. Status report com `estado: bloqueado`. Não abrir PR. Escalar ao humano. |

"Estouro" significa que o round do limite ainda encontrou issues críticos. Se o round do limite passa, o fluxo continua normalmente.

### 3. QA retenta a suíte completa (não só os fluxos que falharam)

Em qualquer re-execução do QA (após correção), o QA roda **todos os fluxos do plano mais quaisquer adicionais que julgou necessários na primeira rodada**. Não re-executar apenas os que falharam — re-executa tudo. Razão: uma correção pode quebrar um fluxo que passava antes.

### 4. Protocolo de chamada de subagente sem viés

O implementador segue um protocolo neutro ao chamar o reviewer e o QA. Proibido no prompt de chamada:

- Afirmar que o trabalho está correto ("implementei conforme o plano", "está funcionando")
- Pré-justificar decisões que o reviewer deveria questionar
- Usar linguagem que ancora aprovação ("verificar se está ok", "confirmar que segui o padrão")

Obrigatório no prompt de chamada:

- Task ID e sprint
- Lista objetiva dos arquivos modificados (sem julgamento de valor)
- Caminho do status report (para o reviewer ler o diff declarado pelo implementador)
- Instrução explícita de **procurar problemas** e confrontar com o projeto existente
- Referência ao role doc do papel chamado (`docs/roles/reviewer.md` ou `docs/roles/qa.md`)

**Template canônico de chamada do reviewer** (implementador usa literalmente ou com variações mínimas):

```
Revisar a implementação da task <TASK-ID> — sprint <NN>.
Arquivos modificados: <lista objetiva>
Status report: <path>
Role: docs/roles/reviewer.md

Procurar problemas, inconsistências com padrões do projeto e violações arquiteturais.
Não confirmar que está correto — questionar o que merecer ser questionado.
Não há contexto adicional além do que está no status report e nos arquivos listados.
```

**Template canônico de chamada do QA** (após reviewer OK):

```
Executar fluxos QA para a task <TASK-ID> — sprint <NN>.
fluxos_qa do plano: <lista do frontmatter do plano>
Avaliação do reviewer: <path da avaliação>
Role: docs/roles/qa.md

Executar todos os fluxos listados mais qualquer outro que julgar necessário.
Reportar tudo que encontrar — não confirmar aprovação.
Não há contexto adicional além do que está na avaliação e nos fluxos definidos.
```

### 5. Comportamento em batches e overnight

Em execuções encadeadas (múltiplas tasks na mesma sessão do implementador), o ciclo reviewer→QA acontece **após cada task**, antes de mergear e iniciar a próxima. A sequência é:

```
task N: implementar → reviewer → (QA) → merge → task N+1: implementar → reviewer → (QA) → merge → ...
```

O overnight não deve encadear merges sem o ciclo reviewer→QA por task. Se o ciclo de uma task bloquear (limite de rounds atingido), a sessão para nessa task e não avança para a próxima.

---

## Razões

- **Viés de confirmação:** mesmo em sessão isolada, o prompt do implementador ancora o reviewer. Protocolo neutro reduz esse efeito sem exigir intermediação humana em cada task.
- **Feedback loop tardio:** detectar problema após merge em integration custa mais que detectar antes. O loop reviewer→QA por task captura antes.
- **Limites de rounds explícitos:** sem limite, o loop pode girar indefinidamente. Com limite e escalada ao humano, há ponto de parada claro.
- **Suíte completa no retry:** correções têm efeito colateral; re-executar só o que falhou dá falsa segurança.
- **Batch coverage:** a sprint 03 mostrou que tasks encadeadas sem loop intermediário chegam ao humano com múltiplos problemas acumulados. Encadear o loop por task distribui a detecção.

---

## Consequências

**Positivas:**
- Problemas detectados antes do merge em integration, reduzindo retorno tardio.
- Protocolo de chamada neutro reduz viés de confirmação sistematicamente.
- Limites explícitos evitam loops infinitos e garantem escalada ao humano quando necessário.
- Batches overnight chegam ao humano com tasks individualmente validadas.

**Negativas / custos:**
- Cada task agora consome mais tempo de execução (reviewer + QA por task).
- O implementador precisa conhecer e seguir o protocolo de chamada — discipline nova.
- Tasks sem `fluxos_qa` não ganham o gate QA, mas o campo já é obrigatório em todos os planos (ADR 0017 + _TEMPLATE-plano.md).
- Dispatches de overnight existentes (DISPATCH-OVERNIGHT-BACK/FRONT) foram escritos antes desta ADR — precisam ser atualizados pelo engenheiro de IA.

**Métricas pra avaliar adoção:**
- Baseline: sprint 03 — 1 inconsistência `vo/enum` detectada pelo humano, não pelo reviewer automático.
- Alvo: 0 problemas na seção §8 das avaliações que o reviewer não tenha flagado antes.
- Critério de parada: após 3 sprints com o novo protocolo, se §8 continuar trazendo surpresas que o reviewer perdeu, revisar o template de chamada.

---

## Alternativas consideradas

- **Manter o fluxo atual (reviewer ao final, sem QA por task):** descartado — é a dor que esta ADR endereça. A sprint 03 evidenciou.
- **Revisor humano por task:** descartado — inviável no contexto de overnight e velocidade de execução.
- **Sessão 100% separada para o reviewer (não subagente):** descartado — overhead de sessão manual entre cada task inviabiliza batches. Subagente com protocolo neutro é o equilíbrio entre independência e automação.
- **Loop ilimitado de rounds:** descartado — sem limite, o implementador pode girar indefinidamente sem progredir. Escalada ao humano é necessária como ponto de parada.
- **QA re-executa só os fluxos que falharam:** descartado — correções têm efeitos colaterais; re-executar só o que falhou dá falsa segurança e viola o princípio de suíte completa.

---

## Referências

- ADR 0004 — Governança do workflow (gate humano integration→develop)
- ADR 0005 — Sessões especializadas por papel (reviewer como sessão isolada)
- ADR 0017 — Prefixo QA-NNN e campo `fluxos_qa` (quando o gate QA se aplica)
- `docs/roles/reviewer.md` — papel a ser atualizado com protocolo de recebimento de chamada neutra
- `docs/roles/qa.md` — papel a ser criado ou atualizado
- `docs/roles/backend.md`, `frontend.md` — protocolo de chamada a ser adicionado
- `CLAUDE.md §Definição de pronto` — atualizar para referenciar esta ADR
- `docs/sprints/03-folha-pagamento/avaliacoes/BE-026-029-sprint03-folha.md §8` — evidência que motivou esta ADR
- Discussão humano ↔ planner, 2026-06-04 — diagnóstico do viés de confirmação no prompt de chamada

## Materialização (para o engenheiro de IA)

Após homologação pelo humano, o engenheiro de IA deve atualizar:

1. `docs/roles/backend.md` — protocolo de chamada do reviewer e QA (templates canônicos do §4 desta ADR)
2. `docs/roles/frontend.md` — idem
3. `docs/roles/reviewer.md` — instrução de como interpretar chamada neutra; não presumir contexto além do que está no prompt
4. `docs/roles/qa.md` — idem (criar se não existir)
5. `CLAUDE.md §Definição de pronto` — referenciar o ciclo reviewer→QA→loop e os limites de rounds
6. `docs/runbooks/PRE-MERGE-CHECKLIST.md` — adicionar item "ciclo reviewer→QA concluído (ou fluxos_qa: [])"
7. Dispatches futuros — incluir o loop por task (não apenas ao final de um batch)
8. `DISPATCH-OVERNIGHT-BACK.md` e `DISPATCH-OVERNIGHT-FRONT.md` — atualizar protocolo de cada task para incluir o ciclo
