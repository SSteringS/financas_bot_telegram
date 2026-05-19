---
**Data:** 2026-05-13
**Branch:** feature/frontend-fase3-completa
**Responsável (instância):** Claude Code (CLI) — overnight FE

---

## O que foi feito

- Instalado `vite-plugin-pwa` (1.3.0).
- Gerado `public/icone-192.png` (192x192), `public/icone-512.png` (512x512) e `public/apple-touch-icon.png` (180x180) — placeholders de cor sólida zinc-900 (#18181b), gerados via Node.js puro (zlib).
- Atualizado `vite.config.ts`: adicionado `VitePWA` com:
  - `manifest` completo (name, short_name, display: standalone, theme_color, icons)
  - `workbox`: `navigateFallback: 'index.html'`, precache de assets estáticos, `NetworkOnly` para URLs `/api/*`
  - `devOptions.enabled: false` — PWA desativado em dev para não interferir com MSW
- Atualizado `index.html`: adicionado `lang="pt-BR"`, `meta theme-color`, `apple-mobile-web-app-capable`, `apple-touch-icon`, ícone padrão.
- Build produz `dist/manifest.webmanifest`, `dist/sw.js`, `dist/workbox-*.js`.

## Desvios do plano

- Os ícones são placeholders monocromáticos (sem letra "P" como era a intenção). Os ícones reais precisam ser substituídos pelo humano com imagens adequadas. Documentado aqui.
- `devOptions.enabled: false`: o plano não mencionava isso, mas com MSW ativo no dev, o service worker do PWA causaria conflitos de interceptação de requests. Mantido desativado em dev.

## Decisões tomadas durante a execução

- `purpose: 'any'` para 192x192 e `purpose: 'any maskable'` para 512x512 — garante compatibilidade com Android (Chrome precisa de pelo menos um ícone maskable ≥ 512px).
- Ícones gerados em PNG puro via Node.js sem bibliotecas externas (canvas não disponível no ambiente).

## Decisões pendentes

Nenhuma — tarefa fechada. Os ícones precisam ser substituídos por artes reais antes do deploy, mas isso é tarefa de design, não de implementação.

## Próximos passos / observações pro próximo

- FE-11: acessibilidade final.
- Deploy (FE-10b): para testar Lighthouse "Installable", rodar `npm run build && npm run preview` e abrir no Chrome.

## Arquivos criados/modificados

- `frontend/public/icone-192.png` (novo)
- `frontend/public/icone-512.png` (novo)
- `frontend/public/apple-touch-icon.png` (novo)
- `frontend/vite.config.ts` (modificado: VitePWA adicionado)
- `frontend/index.html` (modificado: meta tags PWA)
- `frontend/package.json` (modificado: vite-plugin-pwa devDep)
