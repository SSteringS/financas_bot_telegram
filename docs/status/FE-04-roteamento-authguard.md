---
**Data:** 2026-05-13
**Branch:** feature/frontend-fase3-completa
**Responsável (instância):** Claude Code (CLI) — overnight FE

---

## O que foi feito

- Instalado `react-router-dom` e `date-fns`.
- Criado `src/hooks/useAuth.ts`: hook que chama `obterMe()` na montagem, retorna `{ requisitante, status }`. Cache de sessão simples via variáveis de módulo — evita chamada dupla ao API em componentes irmãos. Exporta `invalidarCacheAuth()` que a página Entrar chama após o exchange.
- Criado `src/components/AuthGuard.tsx`: componente wrapper que redireciona para `/erro?motivo=precisa-link` se `status === 'nao-autenticado'`. Mostra spinner de loading enquanto aguarda.
- Criado `src/paginas/Entrar.tsx`: captura `?t=` da URL, chama `exchangeToken`, invalida cache de auth, navega para `/` em sucesso ou `/erro?motivo=token-invalido` em falha. Usa `useRef` para evitar dupla execução em StrictMode.
- Criado `src/paginas/Erro.tsx`: lê `?motivo=` e exibe mensagem amigável conforme mapa de mensagens pré-definido.
- Criado `src/paginas/Home.tsx`: placeholder "Home — em construção".
- Atualizado `src/App.tsx`: configura `BrowserRouter` com rotas `/`, `/entrar`, `/erro`, e `/_showcase` (dev only). Inclui `SessaoExpiradaListener` que escuta o evento `finbot:sessao-expirada` disparado pelo `client.ts` em 401 e navega para a página de erro.
- Criado `src/hooks/useAuth.test.ts`: 3 testes cobrindo loading inicial, autenticação com sucesso e falha.

## Desvios do plano

- O plano sugeria que o AuthGuard "verificasse useAuth, se não autenticado redireciona pra /erro". Implementado com `useEffect` + `useNavigate` em vez de `<Navigate>` direto porque renderizar `<Navigate>` antes do `status` estar definido causaria loop. A abordagem com `useEffect` é mais robusta.
- O cache de sessão foi implementado via variáveis de módulo (simples e funciona para SPA single-tab), não com Context/localStorage. É suficiente para o caso de uso.

## Decisões tomadas durante a execução

- `useRef` em `Entrar.tsx` para evitar dupla chamada ao exchange em StrictMode (React 18 monta e desmonta componentes duas vezes em dev).
- O ouvinte `finbot:sessao-expirada` foi colocado em componente separado `SessaoExpiradaListener` dentro do BrowserRouter, pois precisa de `useNavigate` (que só funciona dentro de um Router).

## Decisões pendentes

Nenhuma — tarefa fechada.

## Próximos passos / observações pro próximo

- FE-05: pode usar `useAuth` e as paginas/componentes já estão configurados. O App.tsx já tem rota `/_showcase` esperando os componentes de showcase.
- FE-07: quando implementar a Home real, substituir o placeholder em `src/paginas/Home.tsx`.

## Arquivos criados/modificados

- `frontend/src/hooks/useAuth.ts` (novo)
- `frontend/src/hooks/useAuth.test.ts` (novo)
- `frontend/src/components/AuthGuard.tsx` (novo)
- `frontend/src/paginas/Entrar.tsx` (novo)
- `frontend/src/paginas/Erro.tsx` (novo)
- `frontend/src/paginas/Home.tsx` (novo)
- `frontend/src/App.tsx` (modificado: BrowserRouter + rotas)
- `frontend/package.json` (modificado: react-router-dom, date-fns)
