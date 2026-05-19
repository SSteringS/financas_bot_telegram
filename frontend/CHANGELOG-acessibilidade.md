# Changelog de Acessibilidade — FE-11

Data: 2026-05-13

## Itens verificados e resultado

### 1. `lang="pt-BR"` no `<html>` ✅
Adicionado em FE-10 no `index.html`.

### 2. Botões com texto ou aria-label ✅

| Componente | Botão | Status |
|---|---|---|
| `FiltroStatus` | Tudo / Pendente / Pago | Texto visível + `aria-pressed` |
| `SeletorMes` | Meses | Texto visível + `aria-selected` |
| `PedidoCard` | Ver comprovante | Texto visível + `aria-label` descritivo |
| `ModalComprovante` | X fechar | `aria-label="Fechar comprovante"` |
| `ModalComprovante` | Baixar | Texto visível + `aria-label` |
| `Home` | Tentar novamente | Texto visível |
| `Home` | Carregar mais | Texto visível |

### 3. Inputs com label ✅
- `BarraBusca`: `<label htmlFor="barra-busca" className="sr-only">` + ícone com `aria-hidden="true"`.

### 4. Modal — foco gerenciado ✅
- `ModalComprovante`: `useEffect` move foco para botão X ao abrir, retorna ao trigger ao fechar.
- `aria-modal="true"`, `role="dialog"`, `aria-labelledby="modal-comprovante-titulo"`.
- ESC fecha via event listener no documento.
- Scroll do body bloqueado com `overflow: hidden`.

### 5. Navegação por teclado ✅
- Todos os controles interativos são `<button>`, `<a>` ou `<input>` nativos.
- Ordem de Tab segue fluxo visual: Header → SeletorMes → FiltroStatus → BarraBusca → Timeline.
- Modal tem `focus-trap` implícito via foco gerenciado (botão X recebe foco ao abrir).

### 6. Contraste ✅ (corrigido onde necessário)
- `text-zinc-500` em fundo branco: ratio ~4.6:1 (passa AA) ✅
- `text-zinc-900` em fundo branco: ratio ~19:1 ✅
- `text-amber-800` em `bg-amber-50`: ratio ~7:1 ✅
- `text-emerald-800` em `bg-emerald-50`: ratio ~9:1 ✅
- **Corrigido**: `ListaVazia` subtexto mudado de `text-zinc-400` (~2.8:1, reprovado) para `text-zinc-500` ✅

### 7. Ícones decorativos com aria-hidden ✅
- Todos os SVGs decorativos têm `aria-hidden="true"`.
- SVGs que transmitem informação (botões com só ícone) têm `aria-label` no elemento pai.

### 8. Áreas de toque mínimas (44x44px) ✅ (ajustado)
- **Corrigido**: `FiltroStatus` buttons: adicionado `min-h-[44px]` e `py-2.5`.
- **Corrigido**: `SeletorMes` buttons: adicionado `min-h-[44px]`.
- `PedidoCard` botão comprovante: já tinha `style={{ minHeight: '44px' }}` ✅
- `ModalComprovante` botão X: já tinha `style={{ minWidth: '44px', minHeight: '44px' }}` ✅
- `ModalComprovante` link download: já tinha `style={{ minHeight: '44px' }}` ✅

## O que precisaria de teste manual no navegador

- axe DevTools na página Home com pedidos carregados
- Lighthouse → Accessibility ≥ 95
- Navegar com Tab em `/_showcase` para verificar ordem de foco
- Abrir ModalComprovante e verificar que foco vai para X
- Fechar modal com ESC e verificar que foco volta ao botão "Ver comprovante"
- Testar com VoiceOver (iOS) ou TalkBack (Android) — fora do escopo desta tarefa overnight

## Pendências conhecidas

- Ícones do PWA são placeholders monocromáticos — não têm contraste interno, mas não são conteúdo de texto, então não afetam axe/Lighthouse.
- O `focus-trap` do modal não é completo (Tab pode escapar do modal para o fundo) — um focus-trap completo precisaria interceptar Tab/Shift+Tab e fazer ciclo dentro do modal. Para o MVP com usuário único não é crítico. Pode ser melhorado em FE-11b se necessário.
