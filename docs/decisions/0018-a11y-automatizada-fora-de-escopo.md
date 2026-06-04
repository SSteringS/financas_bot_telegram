---
adr: 0018
titulo: "A11y automatizada fora de escopo enquanto audience for fechada"
data: 2026-06-03
status: Proposed
decisores: humano
relacionado: [0017]
supersedes: null
superseded_by: null
---

# ADR 0018 — A11y automatizada fora de escopo enquanto audience for fechada

> Proposto pelo qa-test-specialist em 2026-06-03 após a primeira execução real da suíte E2E. O teste `site-fluxo-feliz.spec.ts` detectou uma violação `serious` legítima de contraste de cor (botão `bg-emerald-600 text-white` no `PedidoCard`/`ModalArquivo` em ratio 3.76:1, abaixo do mínimo WCAG AA de 4.5:1). O bug é real, mas a conversa que se seguiu revelou que o **gate automatizado** custa mais do que paga na audiência atual.

---

## Contexto

A suíte E2E MVP entregue na sprint 03 (QA-001 a QA-004) inclui dois pontos de verificação a11y via `@axe-core/playwright`:

1. Spec dedicada `frontend/e2e/specs/a11y-home.spec.ts` — varre `/` e `/erro` autenticadas.
2. Bloco inline em `frontend/e2e/specs/site-fluxo-feliz.spec.ts` — varre a home após seed de pedidos.

Threshold definido na **Decisão 11** do desenho de testes (`docs/architecture/desenho-testes-automatizados.md`): falhar em violações de impacto `serious` + `critical`; tolerar `minor` + `moderate`.

A audiência atual e prevista do produto é **fechada**: humano + Pedro (pai). Sem requisito de conformidade legal (WCAG não é obrigatório pra app privado), sem usuário desconhecido entrando, sem cliente externo.

A primeira execução real da suíte (2026-06-03) bateu numa violação `serious` legítima. O fix do produto é trivial (~2 linhas de Tailwind), mas a conversa expôs um trade-off mais profundo: cada futura violação `serious` vai virar atrito de merge sem proteger risco real do produto (que é domínio financeiro + isolamento de dados, não experiência sensorial).

A própria filosofia do `qa-test-specialist` (system prompt) já estabelece o princípio: "não proponha tipo de teste que o volume ou contexto não justifica". Aplicar essa régua ao próprio escopo atual leva a descopar a11y automatizada.

---

## Decisão

### 1. Descopar a11y automatizada da suíte E2E

Remover toda verificação automatizada de a11y (axe-core) das specs Playwright. O comportamento do produto fica inalterado; o que sai é o **gate**.

Mudanças concretas:

- Deletar `frontend/e2e/specs/a11y-home.spec.ts`.
- Em `frontend/e2e/specs/site-fluxo-feliz.spec.ts`: remover `import AxeBuilder from '@axe-core/playwright'` e o bloco que executa `new AxeBuilder({ page }).analyze()` + filtro de `serious|critical`. Renomear o teste de `'fluxo feliz: login → home → comprovante (com a11y)'` para `'fluxo feliz: login → home → comprovante'`.
- Remover `@axe-core/playwright` dos devDeps de `frontend/package.json` e atualizar `package-lock.json` (`npm install`).

### 2. Manter convenções de marcação semântica (aria-label, role)

`aria-label`, `role="dialog"`, `aria-modal="true"` e similares **permanecem** nos componentes. Justificativa: as próprias specs E2E usam `getByRole('button', { name: /ver comprovante de.../ })` e `getByRole('dialog')` como seletores estáveis. Remover essas marcações quebra o teste do fluxo feliz.

Em outras palavras: a11y como **convenção de marcação que serve ao próprio teste E2E** fica. A11y como **gate axe automatizado** sai. Os dois conceitos são separáveis e esta ADR só revoga o segundo.

### 3. Bug específico do contraste (botão verde) — fora desta ADR

A violação real detectada (`bg-emerald-600 text-white` em `PedidoCard.tsx:36` e `ModalArquivo.tsx:111`) não é resolvida aqui. Duas opções, ambas válidas e independentes desta ADR:

- Abrir `FIX-NNN` separado pra trocar `bg-emerald-600 hover:bg-emerald-700` por `bg-emerald-700 hover:bg-emerald-800` nos dois componentes, como melhoria de UX (contraste insuficiente afeta leitura em sol forte, modo escuro estranho, presbiopia).
- Deixar como está enquanto não incomodar o humano ou Pedro.

A escolha é de produto, não de QA.

### 4. Revogar Decisão 11 do desenho de testes

Atualizar `docs/architecture/desenho-testes-automatizados.md`:

- Marcar **Decisão 11** ("Threshold de a11y — falha em `serious + critical`") como **revogada por ADR 0018**, mantendo o texto original como histórico.
- Atualizar a tabela do MVP de specs (se houver) removendo `a11y-home.spec.ts` e o sufixo "com a11y" de `site-fluxo-feliz`.
- Atualizar qualquer outra menção a `@axe-core/playwright` / "a11y automatizada" no documento.

### 5. Trigger de revisão explícito

Esta decisão **deve voltar à mesa** se qualquer dos seguintes ocorrer:

- O produto for liberado pra qualquer usuário fora do círculo direto (mesmo amigos/família estendida).
- For oferecido a empresa, cliente ou qualquer audiência onde "usuário desconhecido" entra na conversa.
- Humano ou Pedro relatar dificuldade real de leitura/uso do app.

Quando um desses gatilhos disparar, esta ADR vira candidata a `Superseded` por uma nova ADR que reintroduza o gate (provavelmente com threshold mais brando inicial pra atravessar a primeira onda de dívida acumulada).

---

## Razões

- **Audiência fechada justifica YAGNI explícito.** Pra 2 usuários conhecidos sem conformidade legal, axe rodando em cada `e2e:full` adiciona atrito de merge sem proteger risco real do produto. O foco do produto é domínio financeiro (DECIMAL vs FLOAT, isolamento por `requisitante_id`, estados de pedido) e segurança (auth, IDOR). Tempo gasto endereçando violações a11y é tempo não gasto nessas áreas.
- **Coerência com a filosofia da própria role de QA.** O `qa-test-specialist` system prompt diz literalmente "não proponha tipo de teste que o volume ou contexto não justifica". Aplicar a régua a si mesmo é consistente.
- **A11y como marcação semântica continua útil.** As specs E2E dependem de `aria-label` e `role` pra ter seletores estáveis. Removendo o gate sem remover a marcação, o produto mantém boa hygiene de markup sem o custo do gate.
- **Custo de remoção é baixo agora; aumenta com tempo.** Quanto mais código gerado assumindo o gate ativo (futuras specs, componentes pensados pra passar axe), maior o custo de remover depois. Decidir agora minimiza arrependimento.
- **Trigger de revisão preserva opcionalidade.** A ADR não fecha a porta — define explicitamente quando reabrir. Decisão consciente, não omissão.

---

## Consequências

**Positivas:**
- Suíte E2E fica mais rápida e mais focada no que importa pro produto.
- Bugs como o contraste do botão verde deixam de bloquear merge; viram conversas de UX separadas.
- Onboarding mental do implementador de FE simplifica: não precisa antecipar regras WCAG ao escrever componente novo.
- Memória persistente do `qa-test-specialist` registra a decisão (`project_a11y_fora_de_escopo.md`) — futuras sessões não vão re-empurrar a11y automatizada.

**Negativas / custos:**
- Perde gate automatizado contra regressão visual em casos de contraste, foco, hierarquia semântica. Risco aceito porque audiência conhece o produto.
- Trade-off explícito: se a audiência abrir um dia, vai existir backlog de dívida a11y acumulado pra varrer antes da reabertura responsável. Trigger de revisão (Sec. 5) endereça parcialmente.
- Sinal cultural: o projeto declara que a11y não é prioridade automatizada agora. Não muda nada em audiência fechada, mas precisa ser comunicado se um colaborador externo entrar.

**Métricas pra avaliar adoção:**
- Baseline (hoje): 1 spec dedicada a11y + 1 bloco inline a11y; 1 violação `serious` aberta.
- Pós-adoção: 0 specs a11y; bloco inline removido; dep `@axe-core/playwright` removida; tempo total de `e2e:full` reduzido (estimativa: -2 a -5s).
- Critério de revisão: trigger da Sec. 5 disparou. Não há "métrica de continuidade" — a decisão fica até trigger.

---

## Alternativas consideradas

- **Manter o gate e relaxar threshold pra `critical` only.** Descartada. Faz o gate passar agora mas reabre a porta pro mesmo problema com qualquer violação `critical` futura. Vira tapete pra varrer dívida — exatamente o que a Decisão 11 original (Sec. 4 do desenho) rejeitou.
- **Manter o gate como warning (não fail).** Descartada. Resultado registrado mas nunca actionado é ruído puro — pior que não ter, porque polui o relatório sem proteger nada.
- **Suprimir a regra `color-contrast` no axe e manter o resto.** Descartada. Resolve o sintoma específico de hoje mas perpetua a confusão de propósito: pra quem o gate existe? Se audiência é fechada, regra nenhuma serve gate.
- **Skip temporário via `test.skip` com TODO.** Descartada. Código morto vira ruído visual e ninguém mexe. Pior que decidir conscientemente remover.
- **Manter status quo (gate ativo).** Descartada. É a dor que esta ADR endereça — cada violação `serious` futura vai gerar discussão de "vale arrumar ou pular?" que não precisa existir.

---

## Referências

- ADR 0017 — Prefixo QA-NNN para tasks de tooling/infra de qualidade (`docs/decisions/0017-prefixo-qa-tasks-tooling-qualidade.md`). Esta ADR usa o prefixo QA-NNN pra materializar a remoção (QA-007).
- `docs/architecture/desenho-testes-automatizados.md` — spec do desenho de testes; Decisão 11 é o que esta ADR revoga.
- `docs/sprints/03-folha-pagamento/plans/QA-007-descopar-a11y-automatizada.md` — plano de implementação desta ADR.
- `.claude/agent-memory/qa-test-specialist/project_a11y_fora_de_escopo.md` — registro de memória do qa-test-specialist (resumo da decisão pra futuras sessões).
- Componentes afetados (bug detectado pela última execução do gate antes da remoção): `frontend/src/components/PedidoCard.tsx:36` e `frontend/src/components/ModalArquivo.tsx:111`.
