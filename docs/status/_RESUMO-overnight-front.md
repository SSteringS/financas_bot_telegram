# Resumo Overnight — Frontend Fase 3

**Data:** 2026-05-13 → 2026-05-14  
**Branch:** `feature/frontend-fase3-completa`  
**Commits:** 9 (um por tarefa FE-03 a FE-11)

---

## O que foi feito

Todas as 9 tarefas da Fase 3b concluídas com sucesso. A branch está pronta para revisão e merge em `develop`.

| Tarefa | Status | Commit |
|---|---|---|
| FE-03 — API client (fetch wrapper) | ✅ Concluída | `94a9932` |
| FE-04 — Roteamento + AuthGuard | ✅ Concluída | `32a4cfc` |
| FE-05 — PedidoCard + StatusBadge + formato.ts | ✅ Concluída | `fa7ebd8` |
| FE-06 — Filtros (Status, Mês, Busca) | ✅ Concluída | `06c7c74` |
| FE-07 — Home com TanStack Query | ✅ Concluída | `52c2f10` |
| FE-08 — CabecalhoApp com resumo | ✅ Concluída | `15bc8e9` |
| FE-09 — ModalComprovante | ✅ Concluída | `0d682c5` |
| FE-10 — PWA: manifest + service worker | ✅ Concluída | `e966e3e` |
| FE-11 — Acessibilidade e revisão final | ✅ Concluída | `cf51ed8` |

---

## O que ficou parado

Nada — todas as 9 tarefas foram concluídas.

---

## Pendências técnicas novas detectadas

1. **Ícones do PWA são placeholders** — `public/icone-192.png`, `public/icone-512.png` e `public/apple-touch-icon.png` são retângulos monocromáticos zinc-900 (#18181b). Precisam ser substituídos por artes reais antes do deploy. Nenhuma lógica de código muda — só trocar os arquivos PNG.

2. **Focus-trap incompleto no ModalComprovante** — ao pressionar Tab dentro do modal, o foco pode escapar para o fundo da página (que fica inacessível de qualquer forma com o overlay). Para o MVP com usuário único em mobile, não é crítico. Para desktop com teclado, pode ser melhorado adicionando interceptação de Tab/Shift+Tab.

3. **Contadores de FiltroStatus são baseados nos itens da página corrente**, não no total do banco — a API não retorna contagem por status separada no endpoint de listagem. Isso significa "Pendente (3)" mostra os 3 pendentes na página atual, não o total de pendentes do mês. Se quiser contadores globais, precisaria de um endpoint separado ou parâmetro extra.

4. **MSW não intercepta iframe do ModalComprovante** — Service workers não interceptam navigation requests de iframes por padrão. Em dev com `VITE_USE_MOCK=true`, clicar em "Ver comprovante" vai abrir o iframe com uma URL que não resolve (backend não está rodando). Em produção com backend real + S3, funciona perfeitamente. Para testar o modal em dev, recomenda-se subir o backend localmente.

5. **`download` attribute no link de download do ModalComprovante** — não foi adicionado porque o atributo `download` não funciona cross-origin (S3 está em domínio diferente). O usuário usa o "Salvar como" do browser.

---

## Comportamentos estranhos notados

- `vitest@4.1.6` com `jsdom` tinha incompatibilidade ESM/CJS (`html-encoding-sniffer`). Resolvido usando `happy-dom` como ambiente de teste. Todos os testes passam normalmente.
- happy-dom tenta fazer fetch do `src` do `<iframe>` durante os testes do ModalComprovante, resultando em `DOMException [NetworkError]` no console dos testes. Os testes **passam** normalmente — são apenas logs de aviso, não falhas.

---

## Estado final do projeto frontend

```
frontend/
  src/
    api/         client.ts, pedidos.ts, auth.ts, tipos.ts (FE-02 → FE-03)
    components/  AuthGuard, BarraBusca, CabecalhoApp, CarregandoLista,
                 FiltroStatus, ListaVazia, ModalComprovante, PedidoCard,
                 SeletorMes, StatusBadge, Timeline
    hooks/       useAuth, usePedidos, useResumo
    lib/         ambiente.ts, formato.ts
    mocks/       handlers.ts, browser.ts (FE-02)
    paginas/     Home, Entrar, Erro, _Showcase
    App.tsx, main.tsx, index.css
  public/
    icone-192.png, icone-512.png, apple-touch-icon.png (placeholders)
  CHANGELOG-acessibilidade.md
  vite.config.ts (vitest + vite-plugin-pwa)
  package.json (react, react-router-dom, date-fns, @tanstack/react-query,
                msw, vite-plugin-pwa, vitest, happy-dom, etc.)
```

**Testes:** 25 testes passando em 5 arquivos de teste  
**Lint:** 0 erros  
**Build:** OK — `dist/sw.js` gerado pelo workbox

---

## Como revisar

1. `git checkout feature/frontend-fase3-completa`
2. `cd frontend && npm install && npm run dev`
3. Abrir `http://localhost:5173/entrar?t=qualquer-coisa` → redireciona para `/`
4. Home carrega com dados do MSW (autenticação simulada)
5. Filtros por status/mês/busca sincronizam com URL
6. Clicar em "Ver comprovante" → modal abre (iframe pode não resolver sem backend)
7. Abrir `http://localhost:5173/_showcase` para ver todos os componentes isolados
