---
name: A11y automatizada fora de escopo enquanto audience for fechada
description: Decisão de descopar testes a11y automatizados (axe-core) — vale enquanto app for usado só por Pedro e filho
type: project
---

A11y automatizada (axe-core via Playwright) está **fora de escopo** do projeto enquanto a audiência for fechada (2 usuários conhecidos: humano + Pedro/pai). Decisão tomada em 2026-06-03 durante a primeira execução real da suíte E2E.

**O que sai do escopo:**
- Spec `frontend/e2e/specs/a11y-home.spec.ts` — deletada
- Bloco axe em `site-fluxo-feliz.spec.ts` (AxeBuilder + filter de violations) — removido
- Dep `@axe-core/playwright` no `package.json` — removido
- **Decisão 11** do desenho de testes E2E (threshold `serious + critical`) — revogada

**O que FICA (importante):**
- `aria-label` em botões do `PedidoCard` e `ModalArquivo` — mantidos porque as próprias specs E2E usam `getByRole('button', { name: /aria-label/ })` pra interagir. Não é a11y pelo a11y; é seletor de teste.
- `role="dialog"` e `aria-modal="true"` em `ModalArquivo` — mantidos pelo mesmo motivo (`getByRole('dialog')` no teste do fluxo feliz).
- A11y como convenção de marcação semântica fica; a11y como gate automatizado sai.

**Trigger de revisão:** essa decisão volta à mesa se:
- App for liberado pra qualquer usuário fora do círculo (mesmo amigos estendidos)
- For oferecido a empresa/cliente
- Humano ou Pedro relatar dificuldade real de leitura no app

**Why:** YAGNI explícito. Pra app privado sem conformidade legal (WCAG não obrigatório), axe falhando em cada CI adiciona atrito sem proteger risco real do produto (domínio financeiro + isolamento de dados é o que importa). Custo de manter > valor entregue na audience atual.

**How to apply:**
- NUNCA propor adicionar a11y automatizada em sprints futuras a menos que o trigger de revisão tenha sido atendido.
- Em planos de teste de novas features, omitir seção de a11y automatizada.
- Se encontrar problema visual real (ex: contraste ruim) em review, recomendar como FIX de UX, não como bug de a11y — separar as duas conversas.
- Manter convenções de marcação semântica (`aria-label`, `role`, `aria-modal`) porque servem ao teste E2E. Não desencorajar implementadores de usar.
- Esta decisão deve estar formalizada em ADR (proposta: 0018) e refletida em `docs/architecture/desenho-testes-automatizados.md` — confirmar com humano se a formalização foi feita antes de tratar como estável.
