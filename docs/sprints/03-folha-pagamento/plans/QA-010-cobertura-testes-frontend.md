---
task: QA-010
titulo: "Cobertura de testes frontend — hooks + api clients + componentes + páginas"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-04
branch_alvo: feature/qa-010-cobertura-testes-frontend
integration_branch: null
prioridade: alta
esforco: alto
territorio: front
estado: pronto-pra-execucao
depende_de: [QA-008]
bloqueia: []
skills_dispatched: [qualidade-de-testes, boas-praticas-react, ecossistema-frontend]
fluxos_qa: []
---

# QA-010 — Cobertura de testes frontend (hooks + api clients + componentes + páginas)

> **Status: pronto-pra-execucao.** Gaps de cobertura sem decisão pendente. Planner valida posicionamento de sprint e prioridade vs outras tasks de feature ativas.

---

## Intake

- **Origem:** análise de gaps de teste executada pelo qa-test-specialist em 2026-06-04, depois que a suíte E2E ficou estável (QA-008). Inventário de produção vs testes revelou ~14 arquivos sem cobertura no front. Esta task consolida **todos os gaps de front** num único bundle (Opção B do levantamento — granularidade média).
- **Por quê agora:** depois da EVO-09 (folha de pagamento), o front ganhou ~10 arquivos novos (componentes folha + páginas + hook `useFolhaFuncionario`) sem teste. Hooks de produção antigos (`usePedidos`, `useResumo`) também nunca tiveram cobertura. API clients (`auth.ts`, `folha.ts`, `pedidos.ts`) sem teste = contrato com back não validado do lado front.
- **Esforço:** alto (~12-14h, ~2 dias). 14 arquivos de teste novos. Divisão em 4 sub-áreas (hooks, api clients, componentes utilitários, componentes/páginas da folha).
- **Riscos resumidos:** zero risco funcional (não toca código de produto). MSW já está configurado e usado pelos testes existentes — pré-condição satisfeita.

---

## Contexto

Inventário cruzado (produção × teste) identificou os seguintes gaps no front:

### Sub-área A — Hooks (3 gaps)

`frontend/src/hooks/` tem 4 hooks. Apenas 1 tem teste (`useAuth.test.ts`).

Sem teste:
- `usePedidos.ts` — usa TanStack Query. Carrega lista de pedidos com filtros + paginação. Cache, invalidation, refetch.
- `useResumo.ts` — usa TanStack Query. Carrega resumo do mês.
- `folha/useFolhaFuncionario.ts` — query da folha de funcionário (sprint 03).

Hook = lógica de query + cache + refetch + estado de loading/error. Sem teste, regressão silenciosa em cache invalidation passa pra prod.

### Sub-área B — API clients (3 gaps)

`frontend/src/api/` tem 4 arquivos `.ts` de cliente. Apenas `client.ts` tem teste.

Sem teste:
- `auth.ts` — funções de login/logout/me. Contrato com `/api/v1/auth/*`.
- `folha.ts` — funções de funcionário/vale/adiantamento/fechamento. Contrato com `/api/funcionarios/*`.
- `pedidos.ts` — funções de listagem/detalhe/imagem de pedido. Contrato com `/api/v1/pedidos/*`.

API client = ponto onde shape de request/response do back é traduzido pra tipos do front. Sem teste, mudança de contrato silenciosa passa.

### Sub-área C — Componentes utilitários (5 gaps)

`frontend/src/components/` tem componentes simples sem teste:
- `CarregandoLista.tsx` — placeholder de loading.
- `FiltroStatus.tsx` — seletor de status (PENDENTE, PAGO, todos).
- `ListaVazia.tsx` — placeholder de "sem pedidos".
- `SeletorMes.tsx` — seletor de mês.
- `StatusBadge.tsx` — badge visual de status.

Cada um é pequeno (~50-100 linhas), mas usados em produção. Se quebrarem, UX degrada silenciosa (loading não aparece, badge errado, etc.).

### Sub-área D — Componentes e páginas da folha (6 gaps)

Sprint 03 (EVO-09) entregou área de folha de pagamento com cobertura parcial. Cobertos: `FuncionarioForm`, `ModalFechamento`, `FolhaFuncionario`.

Sem teste:
- `frontend/src/components/folha/AdiantamentoForm.tsx` — formulário de cadastro de adiantamento.
- `frontend/src/components/folha/AdiantamentosSection.tsx` — seção de listagem + ações.
- `frontend/src/components/folha/FechamentosSection.tsx` — seção de fechamentos anteriores.
- `frontend/src/components/folha/ValeForm.tsx` — formulário de cadastro de vale.
- `frontend/src/components/folha/ValesSection.tsx` — seção de listagem de vales.

Sem teste (páginas):
- `frontend/src/paginas/Erro.tsx` — página de erro genérico.
- `frontend/src/paginas/folha/FuncionariosPage.tsx` — listagem de funcionários.
- `frontend/src/paginas/folha/FolhaFuncionarioPage.tsx` — detalhe da folha de um funcionário.

---

## Decisão / abordagem

Adotar **paridade com testes existentes** como guia de estilo. Stack já estabelecida:
- **Vitest + React Testing Library** (não Jest — projeto usa Vitest).
- **MSW v2** pra mock de API em hooks/clients.
- **`@testing-library/user-event`** pra interação.
- **`renderHook`** do RTL pra hooks com TanStack Query.

### Sub-área A — Hooks

Modelo referencial: `useAuth.test.ts`.

Padrão por hook:
- Setup com `QueryClientProvider` (TanStack Query exige).
- Mock de endpoint via MSW.
- Cenários por hook:
  - Estado de loading inicial.
  - Sucesso (dados carregados).
  - Erro (4xx, 5xx, network).
  - (Onde aplicável) refetch após invalidation.
  - (`usePedidos`) variação de filtros muda query key.

### Sub-área B — API clients

Modelo referencial: `client.test.ts`.

Padrão por arquivo:
- Mock via MSW de cada endpoint que o módulo chama.
- Cenários:
  - Happy path (resposta 200, shape correto).
  - Erro (resposta 4xx/5xx — função lança exceção correta).
  - Headers/cookies enviados conforme esperado (auth com credentials, content-type).

### Sub-área C — Componentes utilitários

Modelo referencial: `BarraBusca.test.tsx` ou `StatusBadge` se existir um similar.

Padrão por componente:
- Render com props mínimas e verificar UI.
- Render com props variadas (`status='PAGO'` mostra badge verde, etc.).
- Para componentes interativos (`FiltroStatus`, `SeletorMes`): simular interação e verificar callback.

### Sub-área D — Componentes folha + páginas

Modelo referencial: `FuncionarioForm.test.tsx` e `ModalFechamento.test.tsx`.

Padrão por componente:
- Render inicial (estado vazio do form).
- Preencher campos + submit → callback chamado com payload correto.
- Validação de input (campo obrigatório, valor inválido) → erro mostrado, submit bloqueado.

Padrão por página:
- Render com mock de hook/API correspondente.
- Estado de loading visível.
- Estado de erro visível com mensagem clara.
- Estado de sucesso com dados visíveis.

---

## Escopo / arquivos

### Criar

**Hooks (3 arquivos):**
- `frontend/src/hooks/usePedidos.test.ts`
- `frontend/src/hooks/useResumo.test.ts`
- `frontend/src/hooks/folha/useFolhaFuncionario.test.ts`

**API clients (3 arquivos):**
- `frontend/src/api/auth.test.ts`
- `frontend/src/api/folha.test.ts`
- `frontend/src/api/pedidos.test.ts`

**Componentes utilitários (5 arquivos):**
- `frontend/src/components/CarregandoLista.test.tsx`
- `frontend/src/components/FiltroStatus.test.tsx`
- `frontend/src/components/ListaVazia.test.tsx`
- `frontend/src/components/SeletorMes.test.tsx`
- `frontend/src/components/StatusBadge.test.tsx`

**Componentes folha (5 arquivos):**
- `frontend/src/components/folha/AdiantamentoForm.test.tsx`
- `frontend/src/components/folha/AdiantamentosSection.test.tsx`
- `frontend/src/components/folha/FechamentosSection.test.tsx`
- `frontend/src/components/folha/ValeForm.test.tsx`
- `frontend/src/components/folha/ValesSection.test.tsx`

**Páginas (3 arquivos):**
- `frontend/src/paginas/Erro.test.tsx`
- `frontend/src/paginas/folha/FuncionariosPage.test.tsx`
- `frontend/src/paginas/folha/FolhaFuncionarioPage.test.tsx`

**Total:** 19 arquivos de teste novos.

### Não tocar

- **Zero código de produção** (`frontend/src/**/*.{ts,tsx}` que não seja `.test.*`). Se durante a escrita o implementador descobrir bug, **abrir FIX-NNN separado**.
- **Suíte E2E** (`frontend/e2e/`) — fora desta task (E2E vai em QA-011).
- **MSW handlers existentes** (`frontend/public/mockServiceWorker.js`, `frontend/src/mocks/`) — extender se faltar handler para um endpoint, mas não refatorar o que já existe.

---

## Testes

A própria task entrega testes. Estimativa:

- **Sub-área A** (3 hooks): ~3-5 testes cada = ~12-15 testes.
- **Sub-área B** (3 API clients): ~3-4 testes cada = ~9-12 testes.
- **Sub-área C** (5 componentes utilitários): ~2-3 testes cada = ~10-15 testes.
- **Sub-área D** (5 componentes folha + 3 páginas): ~3-5 testes cada = ~24-40 testes.

**`testes_novos` esperado:** 55-80 testes.
**`testes_total` esperado pós-task:** ~70-95 (era 12 antes — quase 6x crescimento).

Cobertura Vitest alvo:
- Cada hook ≥ 80% line coverage.
- Cada API client ≥ 80% line coverage.
- Componentes utilitários ≥ 80%.
- Componentes folha e páginas ≥ 70% (cobertura de happy + erro principal; UI exaustiva é diminishing return).

---

## Critérios de aceitação

- [ ] 19 arquivos de teste criados nos paths declarados em §"Escopo / arquivos".
- [ ] `npm test` verde com `testes_total` ≥ 60 e `testes_novos` ≥ 50.
- [ ] `npm test -- --coverage` mostra:
  - `usePedidos`, `useResumo`, `useFolhaFuncionario` cada ≥ 80% line coverage
  - `api/auth.ts`, `api/folha.ts`, `api/pedidos.ts` cada ≥ 80%
  - Componentes utilitários listados ≥ 80% cada
  - Componentes/páginas folha listados ≥ 70% cada
- [ ] Zero mudanças em código de produção (`frontend/src/**/*.{ts,tsx}` que não seja `.test.*`). Se bug for descoberto, **abrir FIX-NNN separado** e referenciar no status report.
- [ ] `npm run build` (TypeScript + Vite) sem erros.
- [ ] Suíte E2E (`npm run e2e:full`) continua verde — não há regressão.
- [ ] Branch `feature/qa-010-cobertura-testes-frontend` saindo da branch correta (planner define).
- [ ] Status report `docs/sprints/<sprint>/status/QA-010-cobertura-testes-frontend.md` com frontmatter válido. `testes_novos` ≥ 50; corpo lista por sub-área.

---

## Fora de escopo (explicitamente)

- **Testes E2E novos** — vão em QA-011.
- **Testes de cobertura na pasta `mocks/` ou nos arquivos de configuração do Vite** — fora.
- **Refatoração de componentes existentes** — se descobrir code smell, abre FIX-NNN, não mistura.
- **Testes de a11y** — descopado pela ADR 0018.
- **Testes de performance/lighthouse** — descopado pelo princípio geral.
- **Cobertura de páginas `_Showcase`** — é página de exibição interna do design system, não tem testes.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Escopo de 19 arquivos drift | Média | Médio | Sub-áreas A/B/C/D em commits separados; cada uma fecha sozinha; review incremental |
| Hook com TanStack Query exige setup complexo (QueryClient + Provider) | Média | Baixo | `useAuth.test.ts` já tem o padrão estabelecido — copiar |
| Componente da folha depender de estado global complexo (Context, Router) | Média | Médio | `FuncionarioForm.test.tsx`, `ModalFechamento.test.tsx` mostram o padrão de setup; copiar wrapper |
| MSW handler faltando pra endpoint novo | Baixa | Baixo | Adicionar handler local ao teste (`server.use(http.post('/api/...', ...))`) — padrão MSW v2 |
| Bug descoberto no produto durante escrita de teste | Média | Médio | Critério: abre FIX-NNN separado. Reviewer audita |

---

## Coordenação

- **Pode rodar em paralelo com:** QA-009 (back — território totalmente disjunto), QA-011 (E2E — pasta diferente), tasks FE-* de feature que não toquem nos arquivos de teste novos.
- **Depende sequencialmente de:** QA-008 (suíte E2E estável — pra confiar nos testes de regressão). Já mergeada.
- **Bloqueia:** confiança na cobertura de front. Sem esta task, o gate `npm test` no PR não captura regressão em hooks/api/componentes da folha.
- **Atenção pro Reviewer:** verificar que (a) cada arquivo segue o modelo referencial citado; (b) zero diff em código de produção (`src/**/*.{ts,tsx}` que não seja `.test.*`); (c) cobertura bate o critério; (d) testes de hooks usam `QueryClientProvider` (não estado mockado raso).
- **Após merge:** atualizar memória do qa-test-specialist (`project_estado_testes_2026-06-01.md`) com nova baseline (`testes_total` de front).

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido conforme `_TEMPLATE-status.md`, revisão do Reviewer.

`fluxos_qa: []` — task de tooling pura, sem fluxo de produto a validar via QA externo.

---

## Referências

- Análise de gaps original — conversa qa-test-specialist com humano em 2026-06-04 (sessão pós-QA-008).
- Modelos referenciais:
  - `frontend/src/hooks/useAuth.test.ts` (para hooks com TanStack Query)
  - `frontend/src/api/client.test.ts` (para API clients)
  - `frontend/src/components/BarraBusca.test.tsx` (para componentes utilitários simples)
  - `frontend/src/components/folha/FuncionarioForm.test.tsx` (para formulários da folha)
  - `frontend/src/components/folha/ModalFechamento.test.tsx` (para componentes folha com lógica)
  - `frontend/src/paginas/Home.test.tsx` (para páginas com dados)
- Skills relevantes para o implementador: `boas-praticas-react`, `qualidade-de-testes`, `ecossistema-frontend`.
