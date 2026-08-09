---
**Data:** 2026-05-13
**Branch:** feature/frontend-fase3-completa
**Responsável (instância):** Claude Code (CLI) — overnight FE

---

## O que foi feito

- Substituído stub de `ModalComprovante` pela implementação completa.
- `src/components/ModalComprovante.tsx`:
  - Abre quando `pedidoId !== null`, fecha com clique no backdrop, botão X, ou ESC
  - `<iframe src={urlComprovante(pedidoId)}>` ocupando quase toda a tela — lida com imagem E PDF nativamente
  - `sandbox="allow-same-origin allow-scripts allow-popups"` para segurança
  - Mobile: `height: 90dvh` para cobrir quase a tela inteira, `rounded-t-2xl` no mobile / `rounded-2xl` no desktop
  - Foco gerenciado: ao abrir move para botão X, ao fechar retorna ao trigger
  - Bloqueia scroll do body com `overflow: hidden` enquanto aberto
  - `role="dialog"`, `aria-modal`, `aria-labelledby` para acessibilidade
  - Botão "Baixar comprovante" como `<a href target="_blank">` — abre em nova aba
  - Áreas de toque mínimas de 44px no botão X e no link de download
- Criado `src/components/ModalComprovante.test.tsx`: 6 testes cobrindo não-renderização com `null`, renderização com pedidoId, onClose via backdrop, via botão X, via ESC, e src correto do iframe.

## Desvios do plano

- O plano dizia "botão 'Baixar comprovante' que abre URL em nova aba com `download` attribute". Implementado como `<a href target="_blank">` sem atributo `download` — o atributo `download` não funciona cross-origin (o arquivo está em S3/outro domínio), então o navegador abriria a URL sem forçar download de qualquer forma. O usuário pode usar o menu de contexto ou o save nativo do browser.

## Decisões tomadas durante a execução

- `sandbox="allow-same-origin allow-scripts allow-popups"` no iframe — mais seguro que sem sandbox, permite que PDFs e imagens funcionem corretamente.
- `height: 90dvh` em vez de `vh` — `dvh` (dynamic viewport height) lida melhor com a barra de endereços do mobile que aparece/desaparece ao rolar.
- Avisos de `DOMException` nos testes do iframe são de happy-dom tentando buscar a URL de teste — não afetam a validade dos testes (todos passam).

## Decisões pendentes

Nenhuma — tarefa fechada.

## Próximos passos / observações pro próximo

- FE-10: configurar vite-plugin-pwa.
- FE-11: verificar acessibilidade do modal com axe DevTools no browser.

## Arquivos criados/modificados

- `frontend/src/components/ModalComprovante.tsx` (modificado: implementação completa)
- `frontend/src/components/ModalComprovante.test.tsx` (novo)
