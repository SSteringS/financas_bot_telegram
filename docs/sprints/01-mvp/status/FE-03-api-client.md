---
**Data:** 2026-05-13
**Branch:** feature/frontend-fase3-completa
**Responsável (instância):** Claude Code (CLI) — overnight FE

---

## O que foi feito

- Criado `src/api/client.ts`: wrapper sobre `fetch` com `get<T>` e `post<T>`, `credentials: 'include'` em todas as chamadas. Em 401, dispara evento customizado `finbot:sessao-expirada` (capturado pelo App.tsx em FE-04) e lança `ApiError`. Em outros erros, lança `ApiError(codigo, mensagem)`.
- Criado `src/api/pedidos.ts`: funções `listarPedidos(filtros)`, `buscarPedido(id)`, `urlFotoPedido(id)`, `urlComprovante(id)`, `obterResumo()`. As duas URLs (foto e comprovante) são funções que retornam string, não chamam fetch (o browser segue o redirect 302 do backend diretamente).
- Criado `src/api/auth.ts`: funções `exchangeToken(token)` e `obterMe()`.
- Criado `src/api/client.test.ts`: 6 testes cobrindo serialização de params, credentials, erro 4xx, evento 401, e POST com body JSON.
- Instalado `vitest`, `happy-dom`, `@testing-library/react`, `@testing-library/jest-dom`.
- Adicionado `src/test-setup.ts` e scripts `test`/`test:watch` no `package.json`.
- Atualizado `vite.config.ts` para incluir configuração do vitest com `happy-dom` (jsdom foi descartado por incompatibilidade ESM/CJS com vitest 4.x).

## Desvios do plano

- O plano mencionava "redirecionar para `/erro?motivo=sessao-expirada`" no 401. Em vez de fazer `window.location.href` direto no `client.ts` (acoplamento com roteamento), disparamos um `CustomEvent('finbot:sessao-expirada')` que o router (a ser implementado em FE-04) vai capturar. Assim o client permanece testável e sem dependência do React Router.
- `urlFotoPedido` e `urlComprovante` retornam `string` (não fazem fetch) — o browser faz o redirect 302 automaticamente ao usar a URL em `<img src>` ou `<iframe src>`.

## Decisões tomadas durante a execução

- Usado `happy-dom` em vez de `jsdom` como ambiente de testes porque vitest 4.x tem incompatibilidade ESM/CJS com a versão de `html-encoding-sniffer` instalada pelo `jsdom`.
- Params `undefined` são filtrados silenciosamente antes de appendar na URL — não viram strings `"undefined"` na querystring.

## Decisões pendentes

Nenhuma — tarefa fechada.

## Próximos passos / observações pro próximo

- FE-04: lembrar de escutar `window.addEventListener('finbot:sessao-expirada', ...)` no App.tsx ou no AuthGuard para navegar para `/erro?motivo=sessao-expirada`.

## Arquivos criados/modificados

- `frontend/src/api/client.ts` (novo)
- `frontend/src/api/pedidos.ts` (novo)
- `frontend/src/api/auth.ts` (novo)
- `frontend/src/api/client.test.ts` (novo)
- `frontend/src/test-setup.ts` (novo)
- `frontend/vite.config.ts` (modificado: adicionado config vitest)
- `frontend/package.json` (modificado: scripts test + devDeps vitest/happy-dom)
