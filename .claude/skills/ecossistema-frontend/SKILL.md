---
name: ecossistema-frontend
description: >
  Ferramentas do ecossistema frontend do projeto — Vite (config, proxy, env vars),
  TypeScript strict, Jest + React Testing Library (o que testar e como), MSW (manter
  contrato com OpenAPI), OpenAPI codegen (quando regenerar). Carregar quando a task
  envolve configuracao de tooling, setup ou ajuste de testes, ou integracao com API.
load_pattern: contextual
used_by: [frontend]
created: 2026-05-30
adr: 0015
status: ativa
---

# Skill — Ecossistema Frontend

## Quando carregar (gatilho explícito)

- Task cria ou modifica config de Vite, TypeScript, ESLint ou Jest.
- Task adiciona ou ajusta testes de componente/hook.
- Task consome endpoint novo ou modificado da API (contrato OpenAPI).
- **Sinal concreto:** `vite.config.ts`, `jest.config`, `msw`, `render(`, `userEvent`,
  `openapi-typescript`, `import.meta.env`.

## Vite

```ts
// vite.config.ts — alias + proxy pra dev
resolve: { alias: { '@': path.resolve(__dirname, 'src') } }
server: { proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } } }
```

- `VITE_` prefix expõe variável ao bundle — **nunca** secrets com `VITE_`.
- `.env.local` (não versionado) para valores locais; `.env` para defaults versionados.
- Alias `@` espelha em `tsconfig.json → paths: { "@/*": ["src/*"] }`.

## TypeScript — Pontos de Atenção

- `strict: true` obrigatório — inclui `noImplicitAny`, `strictNullChecks`.
- `as` (type assertion) só com type guard imediatamente antes — nunca sem validação.
- Dados externos (API, localStorage) entram como `unknown`, não `any`.
- Tipos da API: usar os gerados pelo codegen (OpenAPI), nunca redefinir manualmente.

## Jest + React Testing Library

**O que testar:** comportamento do usuário, não implementação interna.
- Certo: "quando clico Salvar com campos válidos, chama `onSubmit`".
- Errado: "o estado interno `isLoading` é `true` após o clique".

**Hierarquia de seletores:**
`getByRole > getByLabelText > getByPlaceholderText > getByText > getByTestId`

**`userEvent` vs `fireEvent`:** preferir `userEvent` — simula interação real.
`fireEvent` para eventos sem equivalente (`scroll`, eventos sintéticos específicos).

```ts
await userEvent.type(input, 'texto');
await waitFor(() => expect(screen.getByText('Salvo!')).toBeInTheDocument());
```

**Não testar:** implementação de libs externas, CSS/estilo, snapshots de componentes
que mudam frequentemente (custo de manutenção alto).

## MSW — Manter o Contrato

**Regra:** handler MSW espelha o contrato OpenAPI — nunca inventar formato.

```ts
http.get('/api/pedidos', () =>
  HttpResponse.json([{ id: 1, descricao: 'Teste', valor: 100.0 }])
  // shape EXATA do PedidoResponse do OpenAPI
);
```

Handler divergente do contrato cria falso positivo no teste — pior que não ter teste.

## OpenAPI Codegen

- **Quando regenerar:** após qualquer merge em `develop` que altere o OpenAPI do backend.
- **Nunca editar o arquivo gerado** — sobrescrito na próxima geração.
- Se o tipo gerado parecer errado: corrigir no OpenAPI do backend, não no gerado.

## Checklist de tooling

- [ ] `npm test` verde · `npm run build` sem erro TS · `npm run lint` limpo.
- [ ] Endpoint novo: handler MSW criado espelhando o contrato OpenAPI.
- [ ] OpenAPI mudou: tipos regenerados.
- [ ] Sem `as any` — type guard se necessário.
- [ ] Variável de ambiente nova: adicionada ao `.env.example` (sem valor real).

## Ler junto

- Skill `boas-praticas-react` — como estruturar o componente sendo testado.
- Skill `seguranca-web-frontend` — proxy Vite e env vars sem secrets.
- `docs/architecture/especificacao-tecnica.md` — stack frontend declarada do projeto.
