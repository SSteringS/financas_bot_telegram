# CLAUDE.md — Frontend (frontend/)

## Responsabilidade

Este módulo é o frontend web do projeto. O Claude do **back não deve tocar nesta pasta**.

## Stack

| Ferramenta | Versão | Papel |
|---|---|---|
| Vite | 6.x | Build + dev server |
| React | 18 | UI |
| TypeScript | 5.x (strict mode) | Type safety |
| Tailwind CSS | 3.x | Styling |
| React Router | 7.x | Roteamento (instalado em FE-04) |
| TanStack Query | 5.x | Fetch + cache de servidor (instalado em FE-07) |
| MSW | 2.x | Mock da API em dev (instalado em FE-02) |
| Vitest | — | Testes unitários (instalado em FE-03) |
| date-fns | — | Formatação de datas ptBR |
| Zod | — | Validação de runtime de respostas |
| vite-plugin-pwa | — | PWA (instalado em FE-10) |

## Design

Variante C — timeline cronológica (`docs/design-proposals/variante-c-timeline.html`)

## Comunicação com o backend

- Base URL produção: `https://3.228.138.109:8443`
- Certificado auto-assinado — configurar `rejectUnauthorized: false` no cliente HTTP em dev, ou usar o certificado público (`finbot-cert.pem`) para validação
- Endpoints disponíveis: ver `docs/design-proposals/especificacao-tecnica.md` seção 2

## Estrutura

```
frontend/
  src/
    api/          (client, pedidos, auth, tipos)
    components/   (UI reutilizável)
    hooks/        (useAuth, usePedidos, useResumo)
    lib/          (formato.ts, ambiente.ts)
    mocks/        (MSW handlers — apenas dev)
    paginas/      (Home, Entrar, Erro)
    App.tsx
    main.tsx
    index.css
    vite-env.d.ts
  public/
  index.html
  package.json
  vite.config.ts
  tailwind.config.js
  CLAUDE.md
```

## Variáveis de ambiente

```
VITE_API_BASE_URL   URL base da API (obrigatório)
VITE_USE_MOCK       "true" para ligar MSW em dev (adicionado em FE-02)
```

## Fluxo de branches

- Sempre criar branch a partir de `develop`
- Padrão: `feature/frontend-descricao-curta`
- PR: branch → `develop`
- Nunca commitar direto na `main` ou `develop`

## Regras importantes

- Tocar apenas em `frontend/` — nunca editar arquivos em `financas_bot_telegram/`
- Não commitar `.env` com URLs ou tokens reais
- `.env.example` é o template público de variáveis de ambiente
