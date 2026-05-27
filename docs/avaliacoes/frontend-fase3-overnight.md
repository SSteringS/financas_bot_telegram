# Avaliação — Frontend Fase 3 (Overnight)

**Tarefas avaliadas:** FE-03 a FE-11 (9 tarefas)
**Branch:** `feature/frontend-fase3-completa`
**Período:** 2026-05-13 → 2026-05-14
**Implementador:** Claude Code (CLI) — sessão overnight

---

## Nota final: **9.0 / 10**

Trabalho profissional, completo, transparente e de alta qualidade. Pequenas oportunidades de melhoria em cobertura de testes de componentes complexos e validação runtime que não foram exploradas. Nenhuma improvisação de decisão de produto. Documentação de limitações exemplar.

---

## Critérios e notas por dimensão

| Dimensão | Peso | Nota | Subtotal |
|---|---|---|---|
| Completude da entrega | 15% | 10 | 1.50 |
| Aderência ao plano e ao CLAUDE.md | 15% | 10 | 1.50 |
| Qualidade do código (arquitetura, idiomático) | 15% | 8 | 1.20 |
| Cobertura e qualidade de testes | 15% | 7 | 1.05 |
| Acessibilidade e UX | 10% | 9 | 0.90 |
| Documentação e transparência | 15% | 10 | 1.50 |
| Robustez (tratamento de erro, edge cases) | 10% | 9 | 0.90 |
| Processo (commits, branch, fluxo) | 5% | 10 | 0.50 |
| **Total** | **100%** | | **9.05** |

---

## O que foi excelente

### 1. Completude (10/10)

- **Todas as 9 tarefas finalizadas**, nenhuma marcada como parada ou parcial
- **Status report em cada uma** (`FE-03-*.md` até `FE-11-*.md`)
- **Resumo overnight completo** (`_RESUMO-overnight-front.md`) com:
  - Tabela tarefa → commit hash → status
  - Pendências técnicas novas claramente listadas
  - Comportamentos estranhos notados (incompatibilidade vitest@jsdom, console warnings de iframe nos testes)
  - Estado final da estrutura
  - Guia de "como revisar"
- 1 commit por tarefa, padrão `feat(FE-XX): título curto` respeitado
- Branch única `feature/frontend-fase3-completa`, sem push pra develop

### 2. Aderência ao plano (10/10)

- Todos os critérios de aceitação dos prompts em `ROTEIRO-FRONTEND.md` cumpridos
- Pequenos desvios todos **justificados nos status reports**:
  - AuthGuard usando `useEffect+useNavigate` em vez de `<Navigate>` (evita loop antes de status definido)
  - Cache de auth via variáveis de módulo em vez de Context
  - `happy-dom` em vez de `jsdom` (incompatibilidade ESM/CJS do vitest)
- **Zero decisão de produto improvisada** — quando o plano não especificava, o implementador escolheu o caminho conservador e documentou
- Respeitou estritamente o território: tocou só em `frontend/` e `docs/status/`

### 3. Arquitetura (parte do 8/10 em código)

- **Separação limpa** entre `api/`, `components/`, `hooks/`, `lib/`, `paginas/`, `mocks/`
- **Hooks com responsabilidade única** (`useAuth`, `usePedidos`, `useResumo`)
- **Components pequenos e focados** — `PedidoCard`, `StatusBadge`, `BarraBusca` não passam de 60-80 linhas cada
- **Sincronização de filtros com URL search params** na Home — refresh do navegador preserva estado, comportamento maduro
- **Custom event `finbot:sessao-expirada`** desacopla `client.ts` do router. Solução elegante pro problema "cliente HTTP não pode chamar `useNavigate` direto"

### 4. Maturidade técnica em detalhes

Detalhes que mostram que o implementador pensou nos casos não-óbvios:

- `useRef` em `Entrar.tsx` evitando dupla execução do exchange em React StrictMode (modo dev monta/desmonta componentes 2x)
- `cleanup` do `document.body.style.overflow` no `useEffect` do modal
- `triggerRef.current` no modal pra retornar foco ao elemento anterior ao fechar
- `sandbox="allow-same-origin allow-scripts allow-popups"` no iframe (mitigação XSS conservadora)
- `e.stopPropagation()` no painel do modal (clicar fora fecha, clicar dentro não)
- Lógica condicional fina no `PedidoCard`: "Pago no mesmo dia" vs "Pedido em X · pago em Y" quando datas diferem

### 5. Acessibilidade (9/10)

- **Tarefa dedicada (FE-11) levada a sério** — não foi item check-the-box
- Contraste corrigido onde reprovava AA (`text-zinc-400` → `text-zinc-500`)
- Área de toque mínima 44px em todos os botões
- `aria-label` em botões com ícone, `aria-hidden="true"` em SVGs decorativos
- `<label className="sr-only">` na busca
- Modal com `role="dialog"`, `aria-modal`, `aria-labelledby`, ESC, foco gerenciado
- **CHANGELOG-acessibilidade.md** criado documentando todos os itens verificados
- **Pendência conhecida documentada:** focus-trap completo (Tab cycle) não implementado, justificado como aceitável pra MVP mobile

### 6. Documentação (10/10)

Status reports são realmente úteis:

- Cada um lista **arquivos modificados** (não exaustivo, mas relevante)
- **Desvios do plano** explícitos com justificativa
- **Decisões tomadas** registradas (não decisões de produto — decisões técnicas locais)
- **Limitações conhecidas** declaradas (não escondeu nada)
- Resumo overnight tem qualidade de PR description profissional

Comportamento estranho mencionado no resumo (vitest+jsdom incompatibility, happy-dom alternative) ajuda o próximo desenvolvedor a não tropeçar no mesmo problema.

### 7. Processo (10/10)

- Branch única, commits cirúrgicos, mensagens padronizadas
- Sem push intermediário (esperou humano fazer revisão)
- Status reports incluídos no mesmo commit da feature (não commits separados)
- Nenhum arquivo `.env` com credencial real commitado
- Não tentou tocar em `financas_bot_telegram/` nem em `docs/architecture/`

---

## O que poderia melhorar

### 1. Cobertura de testes desigual (7/10)

**25 testes em 5 arquivos** — bom volume, mas com gaps importantes:

- ✅ `client.test.ts` — 6 testes cobrindo o fetch wrapper bem
- ✅ `useAuth.test.ts` — 3 testes cobrindo loading/sucesso/falha
- ✅ `formato.test.ts` — testes pros helpers (formatarMoeda, formatarData)
- ✅ `BarraBusca.test.tsx` — debounce coberto
- ✅ `ModalComprovante.test.tsx` — interação mais complexa

**Ausentes** (vale o esforço de adicionar):
- **`Home.tsx`** — componente principal com `useState`, `useCallback`, `useSearchParams`, `useQuery`. Lógica de paginação, sincronização URL, contadores. **Não trivial e sem teste.**
- **`Timeline.tsx`** — agrupa pedidos por data com headers ("Hoje", "Ontem", "X de maio"). Lógica de formatação de data + comparação. Mereceria teste.
- **`SeletorMes.tsx`** — gera lista de meses recentes. Lógica não trivial.
- **`AuthGuard.tsx`** — redirecionamento condicional. Importante pro fluxo de auth.
- **`Entrar.tsx`** — exchange flow + invalidação de cache + StrictMode handling. Lógica complexa.

A regra do CLAUDE.md diz "componente com estado/efeitos/eventos → teste obrigatório". Vários dos sem-teste ferem essa regra.

### 2. Validação runtime ignorada (parte do 8/10 em código)

O CLAUDE.md menciona **Zod** no stack ("Validação de runtime de respostas"), mas o implementador **não instalou nem usou**. Em `Home.tsx`:

```typescript
const filtroStatus = (searchParams.get('status') as FiltroStatusValue) ?? 'TUDO'
```

Type assertion (`as`) sem validação real. Se alguém passar `?status=lixo` na URL, o tipo "diz" que é `FiltroStatusValue` mas o valor é `"lixo"` — vai bater no `statusParaApi` retornando `"todos"` (default no `if/else`), o que funciona acidentalmente. Mas é fragilidade.

Idealmente:
```typescript
const filtroStatus = parseFiltroStatus(searchParams.get('status'))
function parseFiltroStatus(v: string | null): FiltroStatusValue {
  if (v === 'PENDENTE' || v === 'PAGO') return v
  return 'TUDO'
}
```

Mesma coisa pra `mes` (formato `YYYY-MM` mas não validado). E pras respostas da API — Zod faria o `client.get<T>` virar `client.get(path, schema)` retornando validado.

### 3. Contadores enganosos no `FiltroStatus`

Pendência declarada no resumo (item 3): "Pendente (3)" mostra o que tem na página corrente, não o total do mês. Isso é **funcionalmente um bug menor** — texto confunde usuário.

Soluções:
- (a) Endpoint adicional `/api/v1/resumo-por-status` específico — overhead, mas correto
- (b) Reusar o `/api/v1/resumo` (já retorna pendentes.quantidade e pagos.quantidade do mês) — bem mais barato
- (c) Esconder o contador até ter dado real

Opção (b) é trivial — basta o `FiltroStatus` consumir o `useResumo` e mostrar `pendentes.quantidade` e `pagos.quantidade`. Implementador podia ter feito isso sem código novo.

### 4. Cache de auth com mutável global

Em `useAuth.ts`:
```typescript
let cachedRequisitante: Requisitante | null = null
let cachedStatus: StatusAuth = 'loading'
```

Funciona, mas:
- Em testes paralelos pode ter race condition (testes que mexem em status diferentes contaminam um ao outro)
- Em SSR / hot reload pode dar comportamento estranho

Alternativas mais robustas:
- `React.Context` com `useReducer`
- TanStack Query como source of truth (já está no stack pra outras coisas)

O implementador documentou que escolheu o simples conscientemente. Aceitável, mas vale registrar como pendência técnica.

### 5. Ícones PWA são placeholders monocromáticos

Já registrado como pendência (item 1 do resumo). Não bloqueia o merge, mas precisa ser tratado antes de "lançar pro pai".

### 6. Focus-trap incompleto no modal

Implementador documentou explicitamente. Trade-off aceitável pra mobile-first user, mas pendência válida.

---

## Pontos pra ajustar no fluxo de desenvolvimento

Aprendizados pra próximas sessões:

### A. Reforçar a regra de testes no prompt

A regra estava no CLAUDE.md ("componentes com estado, efeitos, eventos: teste obrigatório") mas o implementador escolheu não cobrir Home, Timeline, AuthGuard. Próximo prompt overnight pode incluir explicitamente:

> "Antes de marcar uma tarefa como concluída, verifique se todos os componentes/hooks com lógica não-trivial (useState, useEffect, useCallback, parsing, condicionais não-óbvias) têm teste. Se algum não tiver, ESCREVE antes de commitar — mesmo que seja teste simples."

### B. Pedir validação Zod onde mencionada no stack

Se Zod está listado no stack, o prompt deveria pedir uso real. Sugestão pro CLAUDE.md futuro:

> "Sempre que o front consome dado externo (response da API, query param da URL, localStorage), validar com Zod antes de tratar como tipado. Type assertion (`as`) sem validação é proibido."

### C. Push de qualidade mais cedo

O implementador entregou MVP completo. Mas algumas decisões "simples conscientes" (cache global, contadores locais) ficaram como dívida. Pra próximas sessões, vale o prompt explicitamente convidar a **questionar simplicidade vs robustez** e escolher a robusta quando barata.

### D. Critérios de "done" mais rígidos

Adicionar à definição de pronto:

- [ ] `npm test` verde
- [ ] `npm run lint` verde
- [ ] `npm run build` verde
- [ ] Status report criado
- [ ] **Cobertura nominal:** todo componente/hook com lógica não-trivial tem ao menos 1 teste
- [ ] **Sem type assertion (`as`) sem validação correspondente**
- [ ] Mobile testado em DevTools (390px viewport)

### E. Avaliação contínua

Esta é a primeira avaliação formal de overnight. Sugestão: **avaliar todo overnight** com este formato. Acumular relatórios em `docs/avaliacoes/` pra ver padrões ao longo do tempo:
- Recorrência de gaps (ex: "sempre falta teste em X")
- Implementadores melhorando (ou regredindo) com o tempo
- Que tipo de prompt produz melhor resultado

---

## Recomendação pra esta entrega especificamente

**Mergear `feature/frontend-fase3-completa` em `develop`**: ✅ sim, com 1-2 correções pré-merge:

1. **Adicionar teste pelo menos pra `Home.tsx`** — é o componente principal e crítico
2. **Mudar `FiltroStatus` pra consumir contadores do `useResumo`** em vez de calcular sobre página atual (fix de UX)

Outras pendências (Zod, focus-trap, ícones PWA, validação de URL params) podem ficar em `docs/PENDENCIAS-TECNICAS.md` pra tratamento futuro.

Após esse ajuste pequeno, **merge confiável** — qualidade de código é alta, arquitetura sólida, e a base está preparada pra evolução.

---

## Comparação com expectativa

A expectativa quando passei o prompt overnight era: **conclusão das 9 tarefas com qualidade aceitável e documentação adequada pra revisão de manhã**.

A entrega **superou** em transparência e documentação (resumo overnight de altíssima qualidade), **atendeu** em arquitetura e UX (mobile-first, acessibilidade), e **ficou ligeiramente abaixo** em rigor de testes em componentes complexos.

Compreensível, dado o contexto: 9 tarefas em uma noite com pressão de tempo, foco em entregar funcionalidades. Sessão sem essa pressão (uma tarefa por vez) provavelmente teria cobertura maior.

---

## Histórico de avaliações neste arquivo

| Data | Tarefa(s) | Nota |
|---|---|---|
| 2026-05-14 | FE-03 a FE-11 (overnight) | 9.0 |
