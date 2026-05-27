# Roteiro de teste de integração Front ↔ Back (local, manual)

**Objetivo:** validar o frontend consumindo o **backend real** (não mais o MSW), de ponta a ponta: auth por link mágico, listagem de pedidos, filtros, resumo, detalhe e comprovante.

**Branches assumidas neste teste:** tudo em `develop` (back e front), já com **BE-16** e **FE-12** mergeadas. O `/api/v1/resumo` aceita `mes`/`busca` e devolve `todos`; o front consome esse contrato via `useResumo(mes, busca)`.

---

## O que este teste valida

Encanamento de dados de ponta a ponta (auth, cookie, CORS, listagem, detalhe, comprovante, header) **e** os consertos da FE-12:

- **Bug A (contadores):** os contadores das pills devem ficar **estáveis** ao alternar Pendente/Pago — não colapsam mais, porque vêm de `resumo.todos/pendentes/pagos`, não de filtro sobre a página atual.
- **Bug B (header no mês):** trocar o mês no `SeletorMes` deve fazer o **header acompanhar**, porque o front agora passa `?mes=...` pro `/api/v1/resumo`.

---

## Pré-requisitos

### 1. MySQL local rodando

- Banco `financas_bot_telegram_db` em `localhost:3306`, usuário `root`, senha `Satyan123` (conforme `application-dev.properties`).
- Flyway aplica as migrations no boot (`baseline-on-migrate=true`). A V2 cria o requisitante `id=1` (Pedro Marques).
- **Precisa ter pedidos no banco pra ter o que listar.** Como o bot é webhook, ele não recebe mensagens em localhost — então o banco dev só terá pedidos se já existirem de testes anteriores, ou se você inserir manualmente. Ver seção "Semear dados" abaixo.

### 2. Backend no profile `dev`

- Sobe em **`http://localhost:8080`** (HTTP puro — dev não tem SSL nem `server.port` customizado).
- IntelliJ: na run config, garantir `-Dspring.profiles.active=dev` (ou `SPRING_PROFILES_ACTIVE=dev`).
- Terminal: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- **Seguro rodar local:** o bot é webhook (`TelegramWebhookController` → `POST /webhook`). O Telegram não alcança localhost, então subir o backend **não consome mensagens da prod** nem gera conflito.

### 3. Frontend apontando pro backend real (desligar o MSW)

No arquivo `frontend/.env.development`, trocar:

```
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_MOCK=false        ← era true
```

Depois **reiniciar** o `npm run dev` (Vite só relê env no restart).

> **Lembrete:** voltar `VITE_USE_MOCK=true` quando terminar, pra não quebrar o desenvolvimento com mock depois.

- O front precisa subir em **`http://localhost:5173`** exatamente. O CORS do backend (`WebMvcConfig`) libera **só** essa origem. Se o Vite subir em `5174` (porta 5173 ocupada), o CORS bloqueia tudo. Conferir a porta no log do `npm run dev`.

---

## Semear dados (só se o banco dev estiver sem pedidos)

Confira primeiro:

```sql
SELECT id, descricao, valor, status, data_pedido, s3_key_comprovante
FROM pedido WHERE requisitante_id = 1 ORDER BY data_pedido DESC;
```

Se vier vazio, insira alguns pedidos de teste (ajuste datas pro mês corrente, **2026-05**):

```sql
INSERT INTO pedido (requisitante_id, descricao, valor, status, data_pedido, tipo)
VALUES
 (1, 'Conta de luz',    342.10, 'PENDENTE', '2026-05-10', 'BOLETO'),
 (1, 'Mercado',         210.00, 'PENDENTE', '2026-05-15', 'PIX'),
 (1, 'Internet',         99.90, 'PAGO',     '2026-05-03', 'BOLETO');
```

> **Nota sobre comprovante:** o endpoint de comprovante faz `302` pra uma URL pré-assinada do S3. Se você semear um pedido PAGO **sem** um objeto real no bucket dev, o redirect funciona mas a imagem não carrega (S3 devolve 403/404). Pra testar o comprovante **visualmente**, use um pedido PAGO cujo `s3_key_comprovante` aponte pra um objeto que exista de fato no bucket `bot-financas-pagamentos-dev`. Caso não tenha, valide só o `302` (passo 5.2) e considere o render visual como "depende de dado real".

---

## Dados úteis pro teste

| Item | Valor |
|---|---|
| Base URL backend | `http://localhost:8080` |
| Base URL frontend | `http://localhost:5173` |
| Admin API key (dev) | `UJIL3/O6yVw8gHQt3kFdiVqrcAkVAs3xr0ybGmTEj/k=` |
| Requisitante de teste | `id = 1` (Pedro Marques) |
| Cookie de sessão | `finbot_session` (HttpOnly, SameSite=Lax, Secure=false em dev) |

---

## Parte 0 — Smoke test do backend (sem o front)

Confirma que a API está de pé antes de envolver o navegador.

| # | Passo | Resultado esperado |
|---|---|---|
| 0.1 | Abrir `http://localhost:8080/swagger-ui.html` | Swagger UI carrega listando os endpoints |
| 0.2 | No terminal: `curl -i http://localhost:8080/api/v1/resumo` | **401 ou 403** (sem cookie). Confirma que a API exige auth |
| 0.3 | Gerar convite (ver comando abaixo) | `200` com JSON `{ "url": "http://localhost:5173/entrar?t=<TOKEN>" }` |

Comando do passo 0.3:

```bash
curl -i -X POST http://localhost:8080/admin/api/v1/requisitantes/1/convite \
  -H "X-Admin-Key: UJIL3/O6yVw8gHQt3kFdiVqrcAkVAs3xr0ybGmTEj/k="
```

- Sem o header (ou com chave errada) → deve dar **401**. Teste rápido tirando o `-H`.
- O `url` retornado usa `localhost:5173` porque `app.frontend.base-url` aponta pra lá.
- **O token é de uso único e expira em 7 dias.** Cada login novo precisa de um token novo (gere de novo se reutilizar).

---

## Parte 1 — Fluxo de autenticação (link mágico)

| # | Passo | Resultado esperado |
|---|---|---|
| 1.1 | Com o `npm run dev` rodando, abrir `http://localhost:5173/` **direto** (sem cookie) | Redireciona pra `/erro?motivo=precisa-link` com mensagem amigável ("peça um novo link…") |
| 1.2 | Gerar um convite (Parte 0.3) e abrir a `url` retornada no navegador (`/entrar?t=<TOKEN>`) | Mostra "Verificando seu acesso…", chama `POST /api/v1/auth/exchange`, e redireciona pra `/` |
| 1.3 | DevTools → Application → Cookies → `localhost:8080` | Existe o cookie `finbot_session` (HttpOnly marcado) |
| 1.4 | DevTools → Network, recarregar `/` | `GET /api/v1/auth/me` retorna `200` com `{ requisitante: { id:1, nome:"Pedro Marques" } }` |

**Gotchas de auth:**
- Se o exchange der **CORS error** no console: confira que o front está em `5173` (não `5174`) e que `VITE_USE_MOCK=false`.
- Se o cookie **não aparecer**: verifique que a request do exchange tinha `credentials: include` (tem, no `client.ts`) e que a resposta trouxe `Set-Cookie`. Em dev o cookie é `Secure=false`, então funciona em HTTP.
- Token reutilizado → exchange dá `401` → cai em `/erro?motivo=token-invalido`. Gere um novo.

---

## Parte 2 — Home: listagem e header (resumo)

| # | Passo | Resultado esperado |
|---|---|---|
| 2.1 | Já autenticado, ver a Home | Header "Meus Pagamentos" + "Olá Pedro. N pedidos pendentes (R$ …)" vindo de `GET /api/v1/resumo` |
| 2.2 | Network: inspecionar `GET /api/v1/pedidos?...` | `200` com `Pagina<PedidoResumo>`; a lista renderiza os pedidos do banco agrupados por data |
| 2.3 | Conferir os valores | Valores, datas e status batem com o que está no MySQL (passo de semear) |
| 2.4 | Estado vazio: filtrar um mês sem pedidos | Mostra "Nenhum pedido neste filtro" sem quebrar |

---

## Parte 3 — Filtros (status, mês, busca)

| # | Passo | Resultado esperado |
|---|---|---|
| 3.1 | Clicar nas pills "Pendente" / "Pago" | A request `GET /api/v1/pedidos` repete com `status=...`; a **lista** filtra corretamente |
| 3.2 | **(Regressão Bug A)** Observar os contadores das pills ao alternar Pendente/Pago | Os contadores **permanecem estáveis** (ex: Tudo 3 · Pendente 2 · Pago 1) — não colapsam. Vêm do `/api/v1/resumo`, não da página atual |
| 3.3 | Digitar na busca (ex: "luz") | Após ~300ms (debounce), dispara `GET /api/v1/pedidos?busca=luz` **e** `GET /api/v1/resumo?...&busca=luz`; lista e contadores refletem só os matches |
| 3.4 | **(Regressão Bug B)** Trocar o mês no `SeletorMes` | A **lista** muda pro mês **e o header acompanha**. No Network, o `GET /api/v1/resumo` sai com `?mes=<mês selecionado>` — esta é a prova de que a FE-12 está conectada |
| 3.5 | Recarregar a página com filtros aplicados | Filtros persistem (estão na URL via search params); header e contadores remontam coerentes com a URL |

---

## Parte 4 — Detalhe do pedido

| # | Passo | Resultado esperado |
|---|---|---|
| 4.1 | Clicar num pedido da lista | `GET /api/v1/pedidos/{id}` retorna `200` com `PedidoDetalhe`; tela/seção de detalhe renderiza |
| 4.2 | Pedido inexistente (forçar id na URL, se aplicável) | Backend devolve `404`; front trata sem quebrar |

---

## Parte 5 — Comprovante (modal + S3)

| # | Passo | Resultado esperado |
|---|---|---|
| 5.1 | Num pedido **PAGO**, clicar em "Ver comprovante" | Abre o `ModalComprovante` com `<iframe src=".../api/v1/pedidos/{id}/comprovante">` |
| 5.2 | Network: a request do comprovante | `GET /api/v1/pedidos/{id}/comprovante` → **302** com `Location` apontando pra URL pré-assinada do S3 |
| 5.3 | Imagem/PDF renderiza no iframe | Carrega **se** o objeto existir no bucket dev (ver nota em "Semear dados"). Com key fake, fica em branco — ok pro teste de plumbing |
| 5.4 | Fechar modal (X, ESC, clique fora) | Fecha nas três formas; foco volta pro botão que abriu |
| 5.5 | Pedido **PENDENTE** | Não mostra botão de comprovante (só aparece em PAGO) |

---

## Parte 6 — Sessão expirada / 401

| # | Passo | Resultado esperado |
|---|---|---|
| 6.1 | DevTools → Application → Cookies → apagar `finbot_session` | — |
| 6.2 | Recarregar `/` ou disparar qualquer ação que chame a API | A primeira request dá `401`; o front dispara `finbot:sessao-expirada` e redireciona pra `/erro` |

---

## Troubleshooting rápido

| Sintoma | Causa provável | Correção |
|---|---|---|
| Tudo responde com dados "redondos demais" / não bate com o MySQL | MSW ainda ligado | `VITE_USE_MOCK=false` e reiniciar o `npm run dev` |
| `CORS error` no console | Front não está em `:5173`, ou origem diferente | Subir o Vite em 5173; matar processo que ocupa a porta |
| Exchange dá 200 mas cookie não persiste | Bloqueio de cookie de terceiros / origem errada | Conferir que é `localhost` nos dois (não `127.0.0.1` num e `localhost` no outro — são origens distintas pro cookie) |
| Lista sempre vazia | Banco dev sem pedidos | Semear dados (seção acima) |
| `401` ao gerar convite | `X-Admin-Key` ausente/errada | Usar a chave da tabela "Dados úteis" |
| Token "inválido" no /entrar | Token já usado ou expirado | Gerar um convite novo |
| Comprovante não renderiza | `s3_key` aponta pra objeto inexistente no bucket dev | Usar pedido com comprovante real, ou validar só o 302 |

---

## Checklist final

- [ ] Backend `dev` sobe em :8080, Swagger acessível
- [ ] Front em :5173 com `VITE_USE_MOCK=false`
- [ ] Convite gerado → login pelo `/entrar?t=` → cookie setado
- [ ] `/me` retorna o requisitante; Home lista pedidos reais
- [ ] Filtros (status/mês/busca) refletem na lista
- [ ] Detalhe abre; comprovante faz 302 pro S3
- [ ] 401 (cookie apagado) redireciona pro /erro
- [ ] **Revertido `VITE_USE_MOCK=true`** ao terminar

---

## Depois deste teste

Com BE-16 e FE-12 já em `develop` e a integração validada, os próximos passos são os **gates pré-deploy** e a **Fase 3c**:

1. Adicionar a chave `keystore_password` no segredo `finbot-prod-secrets` (AWS Secrets Manager) — **obrigatório antes do próximo deploy do backend**, senão ele não sobe (`application-prod.properties` lê `${keystore_password}`).
2. Rotacionar o token do Telegram (prioridade alta — débito técnico registrado).
3. Resolver o domínio (DEP-00) e seguir DEP-01 a DEP-06 (Route 53, ACM, S3+CloudFront, `api.<domínio>` na EC2, CORS/cookie de prod, GitHub Actions).
