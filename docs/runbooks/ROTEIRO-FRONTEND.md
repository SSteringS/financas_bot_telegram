# Roteiro de Desenvolvimento do Front-end

Guia passo-a-passo pra desenvolver o front (React PWA) usando Claude Code no IntelliJ Ultimate, isolado do backend. Cobre desde "qual editor abrir" até o último deploy. Cada bloco de tarefa traz um prompt pronto pra colar no Claude Code.

---

## 0. IDE escolhida: IntelliJ Ultimate

Você já tem licença Ultimate e está usando o mesmo IntelliJ pro backend. Vamos manter o front no mesmo IntelliJ — e idealmente no **mesmo workspace** do projeto `bot-financas`. Razões:

1. Atalhos e UI já familiares — zero curva de aprendizado.
2. Backend Java e front TypeScript convivem no mesmo workspace, com navegação cruzada (Ctrl+Click em URLs/contratos consegue saltar entre os dois).
3. Refactoring TS no IntelliJ é melhor que no VSCode — rename, extract, move file com update automático de imports.
4. Tailwind, React, TypeScript, ESLint, Prettier — tudo suportado nativamente no Ultimate (com Tailwind precisando de um plugin extra que vamos instalar abaixo).
5. HTTP client, terminal, git, banco — tudo já integrado no IntelliJ.

VSCode segue sendo a alternativa válida da comunidade React; mas, com Ultimate na mão e backend já lá, IntelliJ é o caminho de menor fricção.

---

## 1. Setup do ambiente (uma vez só)

### 1.1. Instalar Node.js LTS

1. Abrir https://nodejs.org no navegador
2. Baixar versão **LTS** (no momento desta escrita: 20.x ou 22.x — qualquer LTS atual serve)
3. Instalar com as opções padrão (deixe marcado "Add to PATH")
4. Validar abrindo um terminal (PowerShell, cmd, ou WSL) e rodando:

```
node --version
npm --version
```

Esperado: ambos retornam uma versão. Se algo falhar, reabrir o terminal (PATH só atualiza pra terminais novos).

### 1.2. Plugins do IntelliJ pra front-end

Abrir IntelliJ → `File → Settings` (ou `Ctrl+Alt+S`) → `Plugins` → aba `Marketplace`.

Instalar (se ainda não estiver instalado):

- **Tailwind CSS** (publicado por JetBrains) — autocomplete e preview de classes Tailwind
- **Prettier** (geralmente já vem) — formatador
- **Claude Code** (publicado por Anthropic) — você provavelmente já tem, mas confirme

Após instalar, `OK` e reiniciar IntelliJ se solicitado.

Os seguintes já vêm habilitados por padrão no Ultimate, só conferir:
- **JavaScript and TypeScript**
- **Node.js**
- **Styled Components / CSS**
- **HTTP Client**

### 1.3. Configurar Prettier como formatador padrão

Após o scaffold do projeto (FE-01) instalar Prettier, voltar aqui e configurar:

1. `Ctrl+Alt+S` → `Languages & Frameworks` → `JavaScript` → `Prettier`
2. **Run for files**: `{**/*,*}.{js,ts,jsx,tsx,json,css,html,md}`
3. Marcar **On 'Reformat Code' action**
4. Marcar **On save**
5. **Prettier package**: deve detectar automaticamente o `node_modules/prettier` do projeto

### 1.4. Configurar ESLint

1. `Ctrl+Alt+S` → `Languages & Frameworks` → `JavaScript` → `Code Quality Tools` → `ESLint`
2. **Automatic ESLint configuration** (deixe selecionado — ele detecta o `.eslintrc` do projeto)
3. Marcar **Run eslint --fix on save**

### 1.5. Configurar git (se ainda não)

No terminal:

```
git config --global user.name "Seu Nome"
git config --global user.email "seu-email@exemplo.com"
```

### 1.6. Confirmar que o Claude Code está conectado no IntelliJ

1. Na barra lateral direita, deve haver um ícone do Claude. Se não, `View → Tool Windows → Claude Code` ou `Ctrl+Alt+'`
2. Se não estiver logado, fazer login com sua conta

Como alternativa, dá pra usar Claude Code via CLI (terminal):
```
npm install -g @anthropic-ai/claude-code
claude --version
```
Dentro de uma pasta do projeto, `claude` abre uma sessão interativa. Funciona bem se você prefere o fluxo via terminal. As duas formas usam a mesma cota — escolha por preferência de UX.

---

## 2. Estratégia: desenvolver front em paralelo com backend

Você disse que o backend já tem bastante coisa pronta, mas as partes específicas dessa Fase 3 (auth, novos endpoints, migration) podem não estar 100%. **O front pode rodar antes do backend estar pronto** se a gente mockar a API.

A ferramenta padrão pra isso é **MSW (Mock Service Worker)**: ela intercepta chamadas `fetch` no navegador e responde com dados fakes que você define. O código de produção não muda — você liga/desliga o MSW por variável de ambiente.

**Plano**: começar do FE-01 ao FE-09 com MSW respondendo dados fakes. Quando o backend tiver os endpoints prontos, desliga MSW (`VITE_USE_MOCK=false`) e o front passa a falar com o backend real, sem mexer em código de feature.

Isso é a primeira coisa que o Claude Code vai configurar (no FE-02).

---

## 3. Setup inicial do projeto (manual + Claude Code)

### 3.1. Decidir estrutura: monorepo ou repo separado

**Recomendo monorepo** — pasta `frontend/` dentro do repo `financas_bot_telegram` que você já tem. Vantagens: 1 só repo, commits podem cruzar back+front quando preciso, contexto único, e o IntelliJ trata os dois como subprojetos no mesmo workspace.

Estrutura final:
```
financas_bot_telegram/
├── src/main/java/...                          (backend Spring que você já tem)
├── infra/                                     (Terraform que você já tem)
├── pom.xml, mvnw, mvnw.cmd                    (Maven)
├── frontend/                                  (NOVO — projeto React)
│   ├── src/
│   ├── package.json
│   ├── vite.config.ts
│   └── ...
└── docs/                                      (planejamento e referência)
    ├── README.md                              (regras desta pasta)
    ├── architecture/                          (fonte da verdade técnica)
    │   ├── especificacao-tecnica.md
    │   └── design-proposals/                  (mockups das variantes A/B/C)
    ├── plans/                                 (planos de fase)
    │   └── FASE-3-VISUALIZACAO.md
    ├── runbooks/                              (roteiros de execução)
    │   └── ROTEIRO-FRONTEND.md                (este arquivo)
    ├── status/                                (relatórios pós-tarefa)
    └── decisions/                             (ADRs — decisões com data e contexto)
```

### 3.2. Abrir o projeto no IntelliJ

Se você já abre o `bot-financas` no IntelliJ pro backend, ótimo — é nessa mesma janela que vai trabalhar.

Se quiser garantir que o IntelliJ enxergue a futura pasta `frontend/` como subprojeto:

1. Após FE-01 criar a pasta `frontend/`, ir em `File → Project Structure → Modules`
2. Clicar **+** → **Import Module** → selecionar `frontend/package.json` → IntelliJ marca como módulo Node.js
3. Aplicar

Isso é opcional; o IntelliJ Ultimate detecta `package.json` automaticamente em pastas filhas e oferece marcar como módulo na primeira vez que você abrir um arquivo TS.

### 3.3. Abrir o terminal integrado

`Alt+F12` abre o terminal integrado já no diretório do projeto. Dá pra abrir múltiplas abas de terminal — útil pra ter um rodando `npm run dev` enquanto você usa outro pra git.

Pra navegar pra pasta do front quando estiver criada:
```
cd frontend
```

### 3.4. Confirmar branch `develop`

```
git status
git branch
```

Se não está em `develop`:
```
git checkout develop
git pull
```

### 3.5. Criar branch de trabalho do front

```
git checkout -b feature/frontend-setup
```

### 3.6. Iniciar Claude Code no projeto

**Se usando extensão IntelliJ**: clicar no ícone do Claude na barra lateral direita (ou `Ctrl+Alt+'`). Ele já entende o projeto inteiro.

**Se usando CLI**: no terminal integrado, rodar `claude`. Sessão interativa abre.

---

## 4. Workflow com Claude Code (uma tarefa por vez)

### 4.1. Anatomia de uma sessão

O fluxo recomendado pra cada tarefa do `docs/plans/FASE-3-VISUALIZACAO.md`:

1. **Cole o prompt da tarefa** (templates abaixo) na conversa do Claude Code
2. Ele lê o `docs/plans/FASE-3-VISUALIZACAO.md`, a especificação técnica e os mockups, e implementa
3. **Você revisa o diff** — IntelliJ mostra um marcador no gutter (lateral) e abre uma janela de diff lado-a-lado quando você clica. Ou, mais formal: `View → Tool Windows → Git` → aba `Local Changes` mostra todos os arquivos modificados, clique pra abrir diff
4. Se algo não bate com o esperado, **peça ajustes específicos** ("o botão de comprovante deveria ter padding maior no mobile" etc)
5. Quando satisfeito, **roda os testes/validação**: `npm run lint`, `npm run dev`, abre no navegador
6. **Commit + push**: `Ctrl+K` no IntelliJ abre o diálogo de commit (ou pelo terminal):
```
git add .
git commit -m "[FE-XX] descrição curta"
git push origin feature/frontend-setup
```
7. Marca a tarefa como `[x]` no `docs/plans/FASE-3-VISUALIZACAO.md`
8. Vai pra próxima tarefa

### 4.2. Quando o Claude Code atrasa ou trava

- **Não confia no que ele disse que fez** — sempre verifica os arquivos. As vezes ele afirma ter implementado e não implementou direito.
- Se a sessão ficar muito longa (>30 min de chat), **inicia uma nova sessão**. Contextos longos pioram a qualidade.
- Se ele insiste num caminho ruim, **rejeita explicitamente**: "Não faça assim. Quero que você use TanStack Query, não useEffect manual."

### 4.3. Política de commits

Um commit por tarefa do plano. Mensagem padrão:
```
[FE-05] Componente PedidoCard
```

Push frequente (a cada tarefa) pra ter backup remoto.

### 4.4. Atalhos úteis do IntelliJ pro front

- `Shift+Shift` — Search Everywhere (achar arquivo, classe, símbolo, ação)
- `Ctrl+Shift+F` — Find in Path (buscar em todos os arquivos)
- `Ctrl+B` ou `Ctrl+Click` — ir pra definição
- `Alt+F7` — Find Usages (achar onde algo é usado)
- `Shift+F6` — Refactor → Rename
- `Ctrl+Alt+L` — formatar arquivo (Prettier vai rodar)
- `Ctrl+Alt+O` — organizar imports
- `Ctrl+Tab` — alternar entre arquivos abertos
- `F12` — abrir Tool Window anterior em foco
- `Alt+1` — focar Project (árvore de arquivos)
- `Alt+F12` — terminal
- `Ctrl+E` — arquivos recentes

---

## 5. Tarefas — prompts prontos pro Claude Code

Pra cada tarefa abaixo: **copie o prompt** e cole no Claude Code. Ele vai ler os arquivos de contexto e executar. Após terminar, siga o checklist de validação.

### FE-01 — Scaffold do projeto

**Prompt:**

```
Vou trabalhar na tarefa FE-01 do arquivo docs/plans/FASE-3-VISUALIZACAO.md.

Antes de começar, leia:
1. docs/plans/FASE-3-VISUALIZACAO.md (a tarefa FE-01 inteira)
2. docs/architecture/especificacao-tecnica.md (seção 4 — estrutura do projeto React)

Crie a pasta `frontend/` na raiz do repo e faça scaffold de um projeto Vite + React + TypeScript com Tailwind CSS.

Stack obrigatória:
- Vite
- React 18
- TypeScript em modo strict
- Tailwind CSS
- ESLint + Prettier

Não instale TanStack Query, React Router, ou MSW ainda — isso virá em tarefas posteriores. Foco apenas em deixar `npm run dev` funcionando com uma página "Hello Finbot" que aplica uma classe Tailwind pra confirmar que o pipeline tá ok.

Crie também:
- .env.development com VITE_API_BASE_URL=http://localhost:8080
- .env.production com VITE_API_BASE_URL=https://api.finbot.dom.br (placeholder, será trocado quando domínio for definido)
- .gitignore apropriado pro projeto

Ao terminar, me diga exatamente quais comandos rodar pra validar que tudo funciona.
```

**Validação manual:**

1. No terminal integrado (`Alt+F12`), dentro de `frontend/`:
```
npm run dev
```
2. Abre `http://localhost:5173` no navegador — deve mostrar "Hello Finbot" estilizado com Tailwind
3. `Ctrl+C` no terminal pra parar o servidor
4. `npm run lint` deve passar sem erros
5. Commit:
```
cd ..
git add .
git commit -m "[FE-01] Scaffold Vite + React + TS + Tailwind"
git push -u origin feature/frontend-setup
```
6. Após o scaffold, voltar à seção 1.3 e 1.4 deste roteiro pra ativar Prettier e ESLint do IntelliJ apontando pro `node_modules` do projeto

---

### FE-02 — Tipos TypeScript dos contratos da API + Setup MSW

**Prompt:**

```
Vou trabalhar na tarefa FE-02 do docs/plans/FASE-3-VISUALIZACAO.md, com uma extensão: também vou adicionar MSW pra mockar a API enquanto o backend não está pronto.

Leia:
1. docs/plans/FASE-3-VISUALIZACAO.md tarefa FE-02
2. docs/architecture/especificacao-tecnica.md seção 2 (contratos da API)

Faça duas coisas:

PARTE A — Tipos TypeScript:
Crie src/api/tipos.ts com interfaces e enums correspondentes a TODOS os endpoints da seção 2 da especificação. Inclui: PedidoResumo, PedidoDetalhe, ResumoMes, Pagina<T>, Requisitante, Erro, AuthExchangeRequest, AuthMeResponse. Enums: StatusPedido (PENDENTE | PAGO), TipoPagamento (BOLETO | PIX | TED | AGENDAMENTO | OUTRO).

PARTE B — Setup do MSW pra mock da API:
Instale MSW (`msw`) como devDependency.
Crie src/mocks/handlers.ts com handlers pra todos os endpoints retornando dados fakes realistas (uns 15 pedidos espalhados em maio/abril 2026, mistura de status e tipos).
Crie src/mocks/browser.ts pra inicialização no browser.
Configure src/main.tsx pra ligar o MSW se VITE_USE_MOCK=true.
Adicione VITE_USE_MOCK=true em .env.development.
Documente em README.md (na pasta frontend/) como ligar/desligar o mock.

Ao terminar, me diga como validar que MSW tá interceptando.
```

**Validação manual:**

1. `npm run dev` (no `frontend/`)
2. Abrir `http://localhost:5173`, abrir DevTools (`F12`) → Console
3. Deve aparecer log do MSW tipo `[MSW] Mocking enabled.`
4. No console, rodar manualmente: `fetch('http://localhost:8080/api/v1/pedidos').then(r => r.json()).then(console.log)` — deve retornar JSON com pedidos fakes
5. Commit:
```
git add .
git commit -m "[FE-02] Tipos TS + MSW mock setup"
git push
```

---

### FE-03 — API client (fetch wrapper)

**Prompt:**

```
Vou trabalhar na FE-03 do docs/plans/FASE-3-VISUALIZACAO.md.

Leia docs/plans/FASE-3-VISUALIZACAO.md tarefa FE-03.

Crie:
- src/api/client.ts: wrapper sobre fetch com get<T>(path, params) e post<T>(path, body), credentials:'include' sempre. Em 401 dispara um event customizado que será capturado pelo router depois pra navegar pra /erro?motivo=sessao-expirada (por enquanto, só console.warn). Em outros erros lança ApiError(codigo, mensagem).
- src/api/pedidos.ts: funções listarPedidos(filtros), buscarPedido(id), urlFotoPedido(id), urlComprovante(id), obterResumo()
- src/api/auth.ts: exchangeToken(token), obterMe()

Use a base URL de import.meta.env.VITE_API_BASE_URL.

Não esqueça: TODAS as funções retornam tipados conforme src/api/tipos.ts.

Adicione testes unitários simples usando vitest (instale como devDep). Pelo menos um teste por função do client.ts garantindo serialização correta dos params.
```

**Validação manual:**

1. `npm test` deve passar
2. No console do navegador (com `npm run dev` rodando):
```js
import('./src/api/pedidos').then(m => m.listarPedidos({}).then(console.log))
```
deve retornar dados do MSW
3. Commit `[FE-03] API client + tests`

---

### FE-04 — Roteamento e AuthGuard

**Prompt:**

```
Vou trabalhar na FE-04 do docs/plans/FASE-3-VISUALIZACAO.md.

Leia a tarefa FE-04 inteira no docs/plans/FASE-3-VISUALIZACAO.md.

Instale react-router-dom.

Crie:
- src/paginas/Entrar.tsx: pega ?t= da URL, chama exchangeToken, em sucesso navega pra /, em erro pra /erro?motivo=token-invalido. Mostra estado de loading enquanto faz a troca.
- src/paginas/Erro.tsx: lê ?motivo= e mostra mensagem amigável. Se motivo=precisa-link: "Peça um novo link pro Satyan pelo zap." Se motivo=sessao-expirada: similar. Se motivo=token-invalido: "Esse link já foi usado ou expirou. Peça um novo."
- src/paginas/Home.tsx (placeholder com texto "Home — em construção")
- src/components/AuthGuard.tsx: componente wrapper que verifica useAuth, se não autenticado redireciona pra /erro?motivo=precisa-link
- src/hooks/useAuth.ts: hook que chama obterMe() na montagem (com cache de sessão), retorna { requisitante, status: 'loading'|'autenticado'|'nao-autenticado' }
- src/App.tsx: configura BrowserRouter com rotas / (protegida por AuthGuard, renderiza Home), /entrar, /erro

Adicione um handler no MSW pra /api/v1/auth/me e /api/v1/auth/exchange retornando dados fakes consistentes com o tipo Requisitante.

Visual ainda simples — vamos polir nas próximas tarefas. Foco é fluxo correto.
```

**Validação manual:**

1. `npm run dev`
2. Abrir `http://localhost:5173/` — deveria redirecionar pra `/erro?motivo=precisa-link` (porque MSW retorna 401 pra `/auth/me` se não tem cookie — ajustar handler do MSW se preciso pra simular fluxo)
3. Abrir `http://localhost:5173/entrar?t=fake-token` — deveria fazer "exchange", redirecionar pra `/`, e mostrar "Home — em construção"
4. Recarregar `/` — não redireciona pra erro porque sessão "persistiu" no MSW
5. Commit `[FE-04] Roteamento + AuthGuard`

---

### FE-05 — Componente PedidoCard

**Prompt:**

```
Vou trabalhar na FE-05 do docs/plans/FASE-3-VISUALIZACAO.md.

Antes de começar, ABRA E LEIA o arquivo docs/architecture/design-proposals/variante-c-timeline.html no navegador (ou peça pra eu te mostrar). Esse é o design de referência. O cartão de pedido aparece várias vezes lá. PRECISA replicar fielmente o visual: foto pequena, descrição, valor grande, status badge (Pendente=âmbar, Pago=verde), datas, e (somente se PAGO) o botão verde grande "Ver comprovante" largura total.

Leia também a tarefa FE-05 no docs/plans/FASE-3-VISUALIZACAO.md.

Crie:
- src/components/PedidoCard.tsx (props: pedido: PedidoResumo, onAbrirComprovante: () => void)
- src/components/StatusBadge.tsx
- src/lib/formato.ts: funções formatarMoeda(numero) → "R$ 1.234,56", formatarData(iso) → "4 de maio", formatarDataRelativa(iso) → "Hoje"|"Ontem"|"4 de maio"

Botão "Ver comprovante" só aparece se status === 'PAGO'. Botão deve replicar o visual da variante C atualizada (o que tem ícone de download e largura total, classe começando com w-full bg-emerald-600).

Crie também src/paginas/_Showcase.tsx — uma rota /_showcase (apenas em dev) que renderiza vários PedidoCard com dados de exemplo, pra eu poder revisar visualmente. Adicione essa rota no App.tsx com um IF NODE_ENV/import.meta.env.DEV pra não vazar em prod.

Acessibilidade: botão tem aria-label, área de toque mínima 44x44.
```

**Validação manual:**

1. `npm run dev`, abrir `http://localhost:5173/_showcase`
2. Comparar lado-a-lado com `docs/architecture/design-proposals/variante-c-timeline.html`. Deve ficar visualmente idêntico nos cartões.
3. Verificar mobile: DevTools (`F12`) → toggle device toolbar → iPhone 12 → cartões empilham bem
4. Commit `[FE-05] PedidoCard + StatusBadge + formato`

---

### FE-06 — Filtros (Status, Mês, Busca)

**Prompt:**

```
Tarefa FE-06 do docs/plans/FASE-3-VISUALIZACAO.md.

Crie:
- src/components/FiltroStatus.tsx (props: value: 'TUDO'|'PENDENTE'|'PAGO', onChange, contadores: {tudo, pendente, pago})
- src/components/SeletorMes.tsx (carrossel horizontal scroll, mostra últimos 12 meses formatados "Maio", "Abril", etc, props: value: string YYYY-MM, onChange)
- src/components/BarraBusca.tsx (props: value, onChange, debounce 300ms internamente)

Visual: replicar a aparência da variante C (chips arredondados pra status, pills horizontais pra mês, input com ícone de lupa).

Adicione esses componentes ao /_showcase pra eu validar visualmente.
```

**Validação manual:**

1. Abrir `/_showcase`, conferir filtros visualmente
2. Mexer num filtro: console deve logar a mudança (showcase pode ter handlers só de log)
3. Buscar com debounce: digitar "abc" rápido — só dispara onChange uma vez 300ms depois
4. Commit `[FE-06] Filtros: status, mês, busca`

---

### FE-07 — Página Home com TanStack Query

**Prompt:**

```
Tarefa FE-07 do docs/plans/FASE-3-VISUALIZACAO.md.

Instale @tanstack/react-query.

Implemente:
- src/hooks/usePedidos.ts: useQuery que chama listarPedidos(filtros) com keepPreviousData:true
- src/hooks/useResumo.ts: useQuery que chama obterResumo()
- src/components/Timeline.tsx: recebe pedidos, agrupa por data_pedido, renderiza headers de data ("Hoje", "Ontem", "4 de maio") seguidos dos PedidoCard daquela data
- src/components/CarregandoLista.tsx: skeleton de 3 cartões
- src/components/ListaVazia.tsx: ilustração simples + texto "Nenhum pedido neste filtro"
- src/paginas/Home.tsx: tela completa
  - No topo: CabecalhoApp com saudação (depois fazemos)
  - SeletorMes fixo (sticky)
  - FiltroStatus
  - BarraBusca
  - Timeline com pedidos agrupados
  - Botão "Carregar mais" no fim, se há mais páginas
- Estado dos filtros sincronizado com URL search params (use useSearchParams do react-router)
- src/main.tsx: configurar QueryClientProvider

Visual: replicar mockup variante C o mais fiel possível.
```

**Validação manual:**

1. `npm run dev`, abrir `/`
2. Lista renderiza com dados do MSW
3. Mexer no filtro de status muda a URL e a lista
4. Mudar mês: URL atualiza, lista recarrega
5. Buscar: debounce funciona, lista filtra
6. Botão "Carregar mais" funciona
7. Recarregar página: filtros persistem (vem da URL)
8. Mobile: tudo funciona bem
9. Commit `[FE-07] Home com lista, filtros, paginação`

---

### FE-08 — Cabeçalho com resumo

**Prompt:**

```
Tarefa FE-08 do docs/plans/FASE-3-VISUALIZACAO.md.

Crie src/components/CabecalhoApp.tsx que:
- Mostra "Olá, {requisitante.nome}" usando useAuth
- Mostra logo abaixo: "{X} pedidos nos últimos 30 dias" — usar useResumo
- Loading skeleton enquanto carrega
- Visual: branco, simples, baseado no header da variante C

Integre na Home.tsx no topo, acima dos filtros.
```

**Validação manual:**

1. Abrir `/` — header aparece com nome e contagem
2. Recarregar — pequeno flash de skeleton, depois conteúdo
3. Commit `[FE-08] Cabeçalho com resumo`

---

### FE-09 — Modal de comprovante

**Prompt:**

```
Tarefa FE-09 do docs/plans/FASE-3-VISUALIZACAO.md, com a decisão fixada: opção (a) — modal sobre a tela com imagem grande e botão de download.

Crie:
- src/components/ModalComprovante.tsx
  - Props: pedidoId: number | null, onClose: () => void
  - Quando pedidoId !== null, abre modal
  - Conteúdo: <iframe src={urlComprovante(pedidoId)}> que ocupa quase a tela toda
  - Iframe lida com imagem E PDF nativamente — assim cobre tanto PIX (screenshot) quanto boleto (PDF) sem precisar de pdf.js
  - Botão "Baixar comprovante" abaixo do iframe que abre URL em nova aba com download
  - Botão X de fechar no canto, ESC fecha, click no backdrop fecha
  - Mobile: 100% da tela. Desktop: max-w-2xl centralizado
  - Acessibilidade: foco vai pro X ao abrir, retorna ao trigger ao fechar, aria-modal, aria-labelledby

Atualize Home.tsx pra ter estado local pedidoIdAberto: number | null e passar pro ModalComprovante. PedidoCard.onAbrirComprovante seta pedidoIdAberto=pedido.id.

Adicione handler do MSW pra /api/v1/pedidos/{id}/comprovante: retorna 302 com Location apontando pra uma URL de imagem placeholder (ex: https://placehold.co/600x800/png).
```

**Validação manual:**

1. Abrir `/`, clicar em "Ver comprovante" de qualquer pedido pago
2. Modal abre com a imagem placeholder
3. Botão "Baixar" abre nova aba com a imagem
4. ESC fecha
5. Click fora fecha
6. Mobile: ocupa tela inteira
7. Commit `[FE-09] Modal de comprovante`

---

### FE-10 — PWA: manifest + service worker

**Prompt:**

```
Tarefa FE-10 do docs/plans/FASE-3-VISUALIZACAO.md.

Instale vite-plugin-pwa.

Configure:
- vite.config.ts: adicionar plugin PWA com manifesto
- manifest:
  - name: "Pagamentos"
  - short_name: "Pagamentos"
  - description: "Histórico de pagamentos"
  - theme_color: "#18181b" (zinc-900)
  - background_color: "#ffffff"
  - display: "standalone"
  - start_url: "/"
  - icons: 192x192 e 512x512 (use placeholders por enquanto, podemos trocar por ícones de verdade depois)
- workbox: precache do shell + assets, NÃO cachear /api/* (sempre fresh)
- public/icone-192.png e public/icone-512.png (gere placeholders simples ou copie de ícones do lucide)

Atualize index.html com:
- <link rel="apple-touch-icon">
- <meta name="theme-color">
- <meta name="mobile-web-app-capable" content="yes">
- title: "Pagamentos"
```

**Validação manual:**

1. `npm run build && npm run preview`
2. Abrir `http://localhost:4173` no Chrome
3. DevTools → Lighthouse → categoria "Progressive Web App" → Run audit
4. "Installable" deve passar
5. Em Chrome desktop, ícone de "instalar" aparece na barra de URL — clicar e instalar
6. Commit `[FE-10] PWA installable`

---

### FE-11 — Acessibilidade e revisão final

**Prompt:**

```
Tarefa FE-11 do docs/plans/FASE-3-VISUALIZACAO.md.

Faça uma passagem de acessibilidade no projeto inteiro:

1. Adicione "lang=pt-BR" no <html>
2. Verifique todos os botões: têm aria-label se só têm ícone? Texto se têm texto.
3. Inputs têm <label> ou aria-label
4. Modal: foco gerenciado, aria-modal, aria-labelledby, ESC fecha, foco retorna
5. Navegação por teclado: Tab passa por todos os controles em ordem lógica, sem armadilhas
6. Contraste: passe pelos componentes principais e ajuste cores se preciso (texto cinza claro em fundo branco geralmente é o problema)
7. Ícones decorativos: aria-hidden="true"

Documente o que mudou em um arquivo CHANGELOG-acessibilidade.md.
```

**Validação manual (você faz):**

1. Instalar a extensão **axe DevTools** no Chrome (https://www.deque.com/axe/devtools/)
2. Abrir `/` em prod (`npm run build && npm run preview`)
3. DevTools → axe DevTools → Scan ALL of my page
4. Resolver issues de "Critical" e "Serious"
5. Lighthouse → Acessibilidade → ≥ 95
6. Commit `[FE-11] Acessibilidade`

---

## 6. Após terminar todas as tarefas

### 6.1. Testar tudo de ponta a ponta com mock

1. `cd frontend && npm run build && npm run preview`
2. Abrir no celular (mesma rede WiFi, IP do PC: o terminal mostra o IP local)
3. Adicionar à tela inicial
4. Navegar como se fosse o pai

### 6.2. Trocar mock por backend real (quando backend estiver pronto)

No `.env.development` mudar `VITE_USE_MOCK=true` pra `false`.

Se o backend tiver problemas, debug no console: a request agora vai pra `localhost:8080`. Verificar no DevTools se o status é 200, headers de cookie, CORS etc.

Dica: o IntelliJ Ultimate tem **HTTP Client** integrado (`Tools → HTTP Client → Create Request in HTTP Client`). Útil pra testar endpoints da API direto da IDE sem abrir Postman/Insomnia. Crie um arquivo `test-api.http` na raiz e escreva requests:
```
GET http://localhost:8080/api/v1/pedidos
Cookie: finbot_session=...
```

### 6.3. Merge da feature branch

```
cd ..
git checkout develop
git pull
git merge feature/frontend-setup
git push
```

Ou abrir PR no GitHub se preferir review formal — IntelliJ tem integração com GitHub no menu `Git → GitHub → Create Pull Request`.

---

## 7. Rodando o front no celular durante o desenvolvimento

Pra testar como o pai vai ver:

1. Estar na mesma WiFi do celular
2. No terminal do PC: `npm run dev -- --host` (o `--host` expõe na rede local)
3. Vite mostra dois URLs: um `localhost` e um `Network: http://192.168.x.x:5173`
4. Abrir o segundo URL no celular
5. Pode adicionar à tela inicial após FE-10

---

## 8. Gotchas comuns

**"Dependência X não funciona"**: rodar `Remove-Item -Recurse -Force node_modules; Remove-Item package-lock.json; npm install` (PowerShell no Windows). Em WSL/bash: `rm -rf node_modules package-lock.json && npm install`.

**Tailwind não aplica classe**: provavelmente o plugin do IntelliJ não detectou o config, ou Tailwind não escaneou o arquivo. Verificar `tailwind.config.js`, campo `content` deve incluir `./src/**/*.{ts,tsx,html}`. No IntelliJ, `File → Invalidate Caches and Restart` resolve quando o autocomplete de classe Tailwind some.

**MSW não intercepta**: confirmar que `VITE_USE_MOCK=true` no `.env.development`, e que `main.tsx` chama o setup do MSW. DevTools → Network → request deve aparecer com `(from service worker)` ou similar.

**CORS error ao desligar mock**: backend precisa de CORS configurado pra `http://localhost:5173`. Ver tarefa BE-13 do backend.

**Tipo do TS reclamando de import.meta.env**: criar `src/vite-env.d.ts` com `/// <reference types="vite/client" />`.

**TanStack Query refetch demais**: ajustar `staleTime` no queryClient (default é 0). Pra esse projeto, `staleTime: 30_000` (30s) faz sentido — cache curto, mas evita refetch a cada hover.

**Cookie não persiste em dev (localhost)**: cookies SameSite=Lax + Secure em http://localhost geralmente funcionam, mas alguns navegadores são chatos. Se virar problema, em dev usar Secure=false (configuração só de dev no backend).

**IntelliJ marca o `frontend/node_modules` como conteúdo a indexar**: pesa muito. Marcar como excluído: `right-click na pasta node_modules → Mark Directory as → Excluded`. Reduz uso de RAM e melhora velocidade de busca.

**IntelliJ não reconhece JSX em `.tsx`**: `Settings → Editor → File Types` → certificar que `*.tsx` está em "TypeScript JSX".

---

## 9. Quando parar e me consultar

Pare e me consulte (não pro Claude Code) se:

- O backend mudar o contrato e o front não bater
- Decidir mudar de variante de design
- Considerar adicionar uma feature fora do plano (cancelamento, edição etc)
- O Claude Code insistir num caminho técnico que parece estranho e você não tem certeza
- Bater num bloqueio de mais de 1h tentando resolver

Não tente "improvisar" decisões de produto sozinho com o Claude Code — ele é executor, não arquiteto.

---

## 10. Resumo da ordem de execução

```
FE-01 (scaffold)
  ↓
FE-02 (types + MSW)
  ↓
FE-03 (API client)
  ↓
FE-04 (router + auth) ─────┐
                            ↓
FE-05 (PedidoCard)        FE-06 (Filtros)
       ↓                       ↓
       └────────┬──────────────┘
                ↓
            FE-07 (Home)
                ↓
            FE-08 (Cabeçalho)
                ↓
            FE-09 (Modal)
                ↓
            FE-10 (PWA)
                ↓
            FE-11 (a11y)
```

Tempo estimado se for solo, com Claude Code fazendo o pesado: 2-3 finais de semana (uns 15-20h efetivos). Se rodar 1 tarefa por noite (1-2h), umas 2 semanas.

Boa sorte. Reporte aqui quando terminar a primeira (FE-01) pra a gente ajustar qualquer coisa que tenha aparecido.
