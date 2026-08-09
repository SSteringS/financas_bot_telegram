---
name: seguranca-web-frontend
description: >
  Seguranca em aplicacoes web SPA — XSS, armazenamento de tokens (httpOnly cookie vs
  localStorage), Content Security Policy, validacao de input, CORS da perspectiva do
  front, riscos de scripts de terceiros. Carregar quando a task toca autenticacao, tokens,
  formularios com dados sensiveis, ou integracao com servicos externos.
load_pattern: shared
used_by: [frontend, reviewer]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — Segurança Web (Frontend)

## Quando carregar (gatilho explícito)

- Task envolve login, logout, refresh de token, ou proteção de rota.
- Task lida com dados sensíveis em formulário (senha, CPF, dados financeiros).
- Task integra script ou SDK de terceiro (analytics, chat, payment widget).
- Task configura headers HTTP no lado do cliente ou discute CSP.
- **Sinal concreto:** aparece `localStorage`, `sessionStorage`, `token`, `Authorization`,
  `dangerouslySetInnerHTML`, `eval`, script de terceiro, CORS error no console.
- **Reviewer:** ao avaliar PR que toca auth, tokens, formulários com dados sensíveis,
  integração com serviço externo ou script de terceiro — verificar que o caminho inseguro não foi escolhido.

## Resumo da capacidade

Identifica os vetores de ataque relevantes para SPAs e provê as decisões de mitigação
prontas — sem paranoia de checklist corporativo, focado no que realmente acontece em
apps React + REST API com autenticação por cookie ou token.

## Armazenamento de Tokens — httpOnly Cookie vs localStorage

| Critério | httpOnly Cookie | localStorage / sessionStorage |
|---|---|---|
| XSS pode roubar o token? | **Não** — JS não acessa | **Sim** — `localStorage.getItem('token')` |
| CSRF pode usar o token? | Sim (mitigar com SameSite) | Não |
| Suporte a `SameSite=Strict` | Sim — bloqueia CSRF cross-site | N/A |
| Risco principal | CSRF se SameSite não configurado | XSS rouba token, persiste sessão |
| Quando usar | **Padrão recomendado** para auth | Tokens de curta duração e não-auth (ex.: preferências de UI) |

**Recomendação: httpOnly + SameSite=Strict** — o padrão mais seguro para tokens de auth.
Se o fluxo envolver redirect externo (ex.: magic link, OAuth), usar `SameSite=Lax` que
permite redirect enquanto bloqueia CSRF iniciado por terceiros.
Verificar se já há decisão registrada na arquitetura do projeto antes de propor mudança.

**Anti-pattern crítico:** guardar JWT em `localStorage` em app financeira. XSS lê, exfiltra,
e o atacante tem sessão válida até expirar — sem como revogar sem blacklist no servidor.

## XSS — Cross-Site Scripting

**`dangerouslySetInnerHTML`:**
- Nunca com conteúdo de usuário sem sanitização.
- Se necessário (ex.: renderizar markdown convertido em HTML), usar `DOMPurify`:
  ```tsx
  import DOMPurify from 'dompurify';
  <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(userHtml) }} />
  ```
- React escapa strings em JSX automaticamente — o risco é só via `dangerouslySetInnerHTML`
  ou `eval`/`new Function`.

**URLs dinâmicas:**
```tsx
// Vulnerável — javascript: URL executa JS
<a href={userInput}>clique</a>

// Seguro — validar protocolo
const safeHref = userInput.startsWith('https://') ? userInput : '#';
```

## Content Security Policy (CSP)

CSP é configurado no servidor (header `Content-Security-Policy`) — o frontend não pode
definir CSP via JS (seria circular). O frontend **informa** o que precisa:

- Fontes externas de script, style, imagem que o app carrega (ex.: Google Fonts, CDN).
- Domínios de API que o app chama (`connect-src`).

Ao integrar SDK de terceiro, documentar os domínios que precisam ser liberados no CSP —
o backend/infra configura o header.

## Validação de Input (Frontend)

Validação no front é UX, não segurança — o backend valida novamente.

| Tipo | Abordagem |
|---|---|
| Formato (email, CPF, data) | Regex ou biblioteca (zod, yup) |
| Limites de tamanho | `maxLength` no input + validação de schema |
| Sanitização antes de enviar | Trim de espaços; nunca `eval` de input |
| Dados financeiros (valor) | Parsear para `number` / `Decimal` antes de enviar — nunca enviar string formatada com vírgula |

## CORS — O Que o Frontend Controla

O frontend **não configura CORS** — é política do servidor. O frontend só:
- Sinaliza o modo de request: `credentials: 'include'` quando precisa enviar cookies.
- Não pode contornar CORS do lado do cliente (qualquer "solução" viola o modelo de segurança).

Quando CORS bloquear em dev: usar o proxy do Vite (`vite.config.ts → server.proxy`)
em vez de desabilitar CORS no browser.

```ts
// vite.config.ts — proxy correto pra dev
server: {
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true }
  }
}
```

## Scripts de Terceiros

- Avaliar necessidade real — cada script de terceiro é superfície de ataque.
- Usar `integrity` (Subresource Integrity) em scripts carregados de CDN:
  ```html
  <script src="https://cdn.example.com/lib.js"
          integrity="sha384-<hash>"
          crossorigin="anonymous"></script>
  ```
- SDKs de analytics/chat carregados dinamicamente: checar política de privacidade e
  se o domínio precisa ser liberado no CSP do backend.

## Checklist de Segurança (antes do commit)

- [ ] Token de auth: não está em `localStorage` / `sessionStorage`?
- [ ] `dangerouslySetInnerHTML`: se usado, conteúdo passou por `DOMPurify`?
- [ ] URLs dinâmicas: protocolo validado antes de colocar em `href`/`src`?
- [ ] Dados financeiros: número parseado (não string formatada) antes de enviar?
- [ ] Script de terceiro novo: documentou domínios pra liberar no CSP?
- [ ] `credentials: 'include'` só presente quando cookie de auth é necessário?

## Ler junto

- Skill `ecossistema-frontend` — configuração de proxy Vite pra dev.
- `docs/architecture/fluxo-autenticacao.md` — fluxo de auth do projeto (ler para aplicar estes princípios ao contexto concreto).
