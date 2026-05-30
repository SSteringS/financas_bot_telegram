# Runbook — DEP-06: teste E2E em produção

Roteiro **manual** pro humano validar o fluxo completo do site em produção, como o Pedro (requisitante) faria. É o último passo da Fase 3c. Não é automatizável por sessão de Claude — exige celular real e o link mágico.

## Pré-requisitos (conferir antes de começar)

- [x] **FIX-volume** aplicado — disco da EC2 com espaço, deploy do back funcionando.
- [x] **`keystore_password`** adicionado no Secrets Manager (`finbot-prod-secrets`) — senão a app não sobe.
- [x] **Back deployado** com as mudanças do **DEP-05** (CORS/cookie apontando pra `satyansaita.com`).
- [x] **Front deployado** (DEP-04) com `VITE_API_BASE_URL=https://api.satyansaita.com` (corrigido).
- [x] **DEP-03** no ar: `curl -i https://api.satyansaita.com/api/v1/resumo` → 401 `SESSAO_AUSENTE`. ✅ (já validado)

## Passo 1 — Gerar o link mágico (token de convite)

No seu terminal (precisa da `X-Admin-Key` = valor de `admin_api_key` no Secrets Manager):

```bash
curl -i -X POST https://api.satyansaita.com/admin/api/v1/requisitantes/1/convite \
  -H "X-Admin-Key: <ADMIN_API_KEY>"
```

- Esperado: **200** com corpo `{"url":"https://satyansaita.com/entrar?t=<TOKEN>"}`.
- Se vier **401**: a `X-Admin-Key` está errada.
- Confira que a `url` usa **`satyansaita.com`** (não `finbot.satyan.com.br`) — se vier o domínio antigo, o `app.frontend.base-url` do DEP-05 não foi aplicado.

## Passo 2 — Abrir no celular (como o Pedro)

- [x] Abrir a `url` do passo 1 no navegador do **celular**.
- [x] A `/entrar?t=...` faz o exchange e **redireciona pra Home** (`/`).
- [x] A Home **lista os pedidos reais** (vindos da API, não mock).
- [x] O cabeçalho mostra o nome do requisitante + o resumo do mês.

## Passo 3 — Comprovante

- [x] Tocar em "Ver comprovante" num pedido **PAGO** → o modal abre com a imagem/PDF carregando (via pre-signed URL do S3).
- [x] O botão de download abre o arquivo.

## Passo 4 — PWA (instalável)

- [x] "Adicionar à tela inicial" aparece / funciona no Chrome do Android.
- [x] Aberto pela tela inicial, abre **fullscreen** (sem barra do navegador).

## Passo 5 — Persistência da sessão

- [x] Fechar e reabrir o app → **continua autenticado** (cookie de 180 dias persistiu).
- [x] (Opcional) Confirmar no DevTools que o cookie `finbot_session` tem `Domain=satyansaita.com; Secure; HttpOnly; SameSite=Lax`.

## Critério de conclusão (DEP-06)

Todos os passos acima ✅. Se algo falhar, anotar onde e qual o sintoma:

- **CORS bloqueado / erro de origem no console** → DEP-05 (allowed-origin) ou `.env.production` do front errados.
- **Login não persiste / 401 após exchange** → `Domain` do cookie errado (DEP-05) ou `Secure`/`SameSite`.
- **Imagem do comprovante não carrega** → pre-signed URL / permissão S3 (BE-07/08).
- **Link com domínio errado** → `app.frontend.base-url` (DEP-05).

Registrar o resultado num status report `docs/status/DEP-06.md` (mesmo sendo teste manual — vira evidência de que o MVP está no ar).

## Referências

- `docs/plans/BACKLOG-produto.md` (DEP-06) · `docs/plans/DEP-05-cors-cookie-prod.md` · `docs/plans/DEP-03-api-subdominio-proxy.md`
- Fluxo de auth: BE-10 (token admin), BE-11 (exchange + cookie), `docs/architecture/fluxo-autenticacao.md`
</content>
