# Finbot Frontend

PWA de consulta de pedidos de pagamento e comprovantes.

## Desenvolvimento

```bash
npm install
npm run dev
# Abre em http://localhost:5173
```

## Mock da API (MSW)

Em desenvolvimento, as chamadas à API são interceptadas pelo [MSW](https://mswjs.io/) e respondidas com dados fakes. Isso permite desenvolver o frontend sem precisar do backend rodando.

### Ligar/desligar o mock

Edite `.env.development`:

```env
# Mock ligado (padrão em dev)
VITE_USE_MOCK=true

# Mock desligado — chamadas vão para VITE_API_BASE_URL de verdade
VITE_USE_MOCK=false
```

Reinicie o servidor de dev após mudar a variável.

### Confirmar que o MSW está ativo

Abra `http://localhost:5173`, abra DevTools → Console. Deve aparecer:

```
[MSW] Mocking enabled.
```

Todas as requisições para `http://localhost:8080/api/v1/*` serão interceptadas e respondidas com dados fakes enquanto `VITE_USE_MOCK=true`.

### Dados fakes disponíveis

- 15 pedidos espalhados em maio e abril de 2026
- Mix de status: PENDENTE e PAGO
- Mix de tipos: BOLETO, PIX, TED, AGENDAMENTO
- Usuário: Pedro Marques (id: 1)
- Auth: qualquer token passado para `/auth/exchange` é aceito

## Scripts

| Comando | Descrição |
|---|---|
| `npm run dev` | Dev server em localhost:5173 |
| `npm run build` | Build de produção em `dist/` |
| `npm run preview` | Preview do build de produção |
| `npm run lint` | ESLint |
| `npm run lint:fix` | ESLint com auto-fix |
