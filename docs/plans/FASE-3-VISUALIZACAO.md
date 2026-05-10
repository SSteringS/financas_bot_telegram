# Fase 3 — Camada de Visualização

Plano de desenvolvimento do site que o Pedro (pai) vai usar pra consultar pedidos e baixar comprovantes. Cada tarefa abaixo é atômica: pode ser passada pro Claude Code do IntelliJ pra executar isoladamente. Ordem importa — respeitar dependências indicadas.

---

## Contexto rápido

O sistema hoje é um bot de Telegram que registra pedidos de pagamento e comprovantes em MySQL/S3, operado só por mim (filho). Esta fase adiciona uma camada de **leitura pro requisitante** (meu pai), que recebe um link, abre no celular e vê histórico + comprovantes sem precisar interagir com bot.

A operação do bot continua igual. O que muda:
- Modelo de dados ganha conceito de `requisitante`, datas separadas (pedido vs pagamento), e tipo de pagamento
- Surge uma API REST consumida pelo front
- Surge um front React (PWA) hospedado em S3+CloudFront
- Surge fluxo de auth via link mágico (gerado por mim, manualmente, e enviado pelo zap)

---

## Decisões fixadas (não revisitar nesta fase)

| Tema | Decisão |
|---|---|
| Backend | Manter Spring Boot WebFlux, MySQL, S3, Secrets Manager |
| Front | React 18 + Vite + TypeScript + Tailwind + TanStack Query |
| Hospedagem do front | S3 + CloudFront (Terraform no mesmo repo de infra) |
| Auth do site | Link mágico via zap → JWT em cookie HTTP-only de 180 dias |
| Categorias de pedido | Enum: `BOLETO`, `PIX`, `TED`, `AGENDAMENTO`, `OUTRO` |
| Visualização de comprovante | Modal com imagem grande e botão de download |
| Cancelamento de pedido | Fora do MVP |
| Notificação automática | Fora do MVP (vai pra evolução, junto da migração pro WhatsApp) |
| Variante de design | C — timeline cronológica (ver `docs/architecture/design-proposals/variante-c-timeline.html`) |

---

## Decisão pendente

- **Domínio do site**: ainda não tem. **Não bloqueia desenvolvimento** — backend e front podem rodar localmente. Bloqueia apenas a fase de deploy (3c). Resolver até o início da DEP-01.

---

## Estrutura geral das fases

```
Fase 3a — Backend (REST API, auth, migrations)
   ↓
Fase 3b — Front (React app contra API real)
   ↓
Fase 3c — Deploy (S3, CloudFront, domínio, GitHub Actions)
   ↓
Fase 3d — Evolução pós-MVP (não fazer agora)
```

3a e 3b podem rodar em paralelo a partir do momento em que o contrato da API está fechado (após BE-04). Se trabalhar sozinho, fazer sequencial é mais simples.

---

# Fase 3a — Backend

## BE-01 — Migration SQL: requisitante, datas, categoria, auth_token

**Contexto**: o schema atual não tem requisitante explícito, separação entre data_pedido/data_pagamento, tipo de pagamento, nem suporte a token de auth. Esta migration introduz tudo.

**Arquivos**:
- `src/main/resources/db/migration/V2__add_requisitante_dates_categoria_auth.sql` (criar)
- Verificar se Flyway está configurado no `pom.xml` e `application*.properties` — se não estiver, adicionar.

**Critério de aceitação**:
- Tabela `requisitante` criada com colunas `id`, `nome`, `telefone`, `email`, `ativo`, `criado_em`
- Linha inicial inserida: `(1, 'Pedro Marques', '+5511XXXXXXXX', NULL, TRUE, now())` — ajustar telefone pro real
- Tabela `pedido` alterada com `requisitante_id BIGINT NOT NULL DEFAULT 1`, `data_pedido DATE NOT NULL`, `data_pagamento DATE NULL`, `tipo ENUM(...)`
- FK `pedido.requisitante_id → requisitante.id` criada
- Índices `idx_pedido_requisitante_data` e `idx_pedido_status_data` criados
- Backfill de `data_pedido` a partir do timestamp de criação atual da tabela
- Backfill de `data_pagamento` pros pedidos PAGOS, baseado no timestamp do comprovante
- Tabela `auth_token` criada com `token_hash CHAR(64) PK`, `requisitante_id`, `criado_em`, `expira_em`, `usado_em NULL`
- Aplicar em dev (e validar via `mysql` cli que tudo bateu); aplicar em prod manualmente após dev validar
- `EXPLAIN SELECT * FROM pedido WHERE requisitante_id=1 AND data_pedido >= '2026-01-01' ORDER BY data_pedido DESC LIMIT 20;` deve usar `idx_pedido_requisitante_data`

**Dependências**: nenhuma. **Primeira tarefa da fase.**

---

## BE-02 — Domain entities + Repositories pros novos campos

**Contexto**: os campos novos da migration precisam aparecer no domínio Java. Manter a hexagonal — domain puro, sem dependência de Spring Data.

**Arquivos**:
- `domain/Pedido.java` — adicionar `requisitanteId`, `dataPedido`, `dataPagamento`, `tipo`
- `domain/Requisitante.java` — criar
- `domain/TipoPagamento.java` — enum
- `application/port/out/RequisitanteRepository.java` — port
- `infra/persistence/JpaRequisitanteRepository.java` — adapter Spring Data
- `infra/persistence/RequisitanteEntity.java` + mapper
- `infra/persistence/PedidoEntity.java` — adicionar campos
- Mapper `PedidoEntity ↔ Pedido` atualizado

**Critério de aceitação**:
- Compilação ok, testes existentes passando
- `RequisitanteRepository.findById(Long)` retorna `Optional<Requisitante>`
- Pedido tem getters/builders pros novos campos
- Fluxo atual do bot continua salvando pedido (com `requisitante_id=1` hardcoded por enquanto, e `data_pedido = LocalDate.now()`)
- Testes: pelo menos 1 teste unitário do mapper Pedido com os novos campos

**Dependências**: BE-01

---

## BE-03 — Extração de tipo (BOLETO/PIX/TED/AGENDAMENTO) da legenda

**Contexto**: hoje o bot recebe legenda tipo `150.00 Almoço` ou `#123 PIX`. Precisamos enriquecer a parsing pra também detectar o tipo de pagamento (no caso de pedidos novos) e setar `tipo` no Pedido.

**Arquivos**:
- `domain/parsing/ParserLegenda.java` — adicionar lógica
- `domain/parsing/ParserLegendaTest.java` — testes

**Critério de aceitação**:
- Legenda tipo `150.00 Almoço boleto` → tipo=BOLETO
- Legenda `200.00 PIX maria` → tipo=PIX
- Legenda `1500.00 TED construtora` → tipo=TED
- Legenda `300.00 agendamento luz` → tipo=AGENDAMENTO
- Legenda sem palavra-chave → tipo=OUTRO
- Case insensitive
- 5+ casos de teste cobrindo todos os tipos + edge cases

**Dependências**: BE-02

---

## BE-04 — Contrato OpenAPI da API + DTOs

**Contexto**: definir o contrato formal antes de implementar endpoints. Isso desbloqueia a Fase 3b (front) pra rodar contra mock se necessário.

**Arquivos**:
- `docs/api/openapi.yaml` (criar) — contrato OpenAPI 3.1 cobrindo todos os endpoints da seção 2 da `docs/architecture/especificacao-tecnica.md`
- `application/dto/PedidoResumoDTO.java` — DTO da listagem
- `application/dto/PedidoDetalheDTO.java`
- `application/dto/ResumoMesDTO.java`
- `application/dto/AuthExchangeRequest.java`
- `application/dto/AuthMeResponse.java`
- `application/dto/PaginaDTO.java<T>` — wrapper genérico de paginação

**Critério de aceitação**:
- OpenAPI valida em https://editor.swagger.io
- DTOs têm Bean Validation (`@NotNull`, `@Size`, etc) onde aplicável
- DTOs usam `Jackson` annotations consistentes (`@JsonProperty` pra camelCase)
- Cobre todos os endpoints listados na especificação técnica
- Não inclui endpoints de cancelamento/delete (fora do MVP)

**Dependências**: BE-02. **Marco**: a partir daqui o front pode começar.

---

## BE-05 — Service e endpoint GET /api/v1/pedidos (listagem com filtros)

**Contexto**: endpoint mais usado da API. Suporta filtros de status, tipo, intervalo de datas, busca, paginação.

**Arquivos**:
- `application/usecase/ListarPedidosUseCase.java`
- `infra/web/PedidoController.java`
- `infra/persistence/PedidoQueryRepository.java` — query com `@Query` ou Specification API
- Testes unitários do use case + teste de integração do controller

**Critério de aceitação**:
- `GET /api/v1/pedidos` retorna 200 com formato `PaginaDTO<PedidoResumoDTO>`
- Aceita query params `status`, `tipo` (repetível), `de`, `ate`, `busca`, `page`, `tamanho`
- Filtra automaticamente por `requisitante_id` extraído do JWT (mockar autenticação por enquanto se BE-09 ainda não rodou — usar `@WithMockUser` ou similar com `requisitanteId=1`)
- Default: `tamanho=20`, `page=0`, ordem `data_pedido DESC, id DESC`
- `tamanho` máximo 50 (validação rejeita maior com 400)
- Busca textual cobre `descricao` (LIKE `%busca%`); se busca for número parseável, também filtra `valor = busca`
- Testes: pelo menos 5 cenários (sem filtro, status pendente, intervalo de datas, busca por texto, paginação)

**Dependências**: BE-04

---

## BE-06 — Endpoint GET /api/v1/pedidos/{id}

**Contexto**: detalhe de um pedido específico. Verifica que o requisitante autenticado é dono do pedido.

**Arquivos**:
- `application/usecase/BuscarPedidoUseCase.java`
- Adicionar handler em `PedidoController.java`

**Critério de aceitação**:
- Retorna 200 com `PedidoDetalheDTO`
- Retorna 404 se id não existe
- Retorna 403 se pedido pertence a outro `requisitante_id`
- Teste: 3 cenários (ok, 404, 403)

**Dependências**: BE-04

---

## BE-07 — Service de pre-signed URL pro S3

**Contexto**: nunca expor URL direto do S3. Endpoints de imagem geram pre-signed URL temporária.

**Arquivos**:
- `application/port/out/StorageService.java` — adicionar método `String gerarUrlTemporaria(String s3Key, Duration ttl)`
- `infra/storage/S3StorageService.java` — implementar usando `S3Presigner` do AWS SDK v2

**Critério de aceitação**:
- Recebe `s3Key` (path do objeto), retorna URL HTTPS válida por 10 minutos
- Usa AWS SDK v2 `S3Presigner` com método GET
- Teste: usa LocalStack ou mock do `S3Presigner` e valida que a URL gerada tem `X-Amz-Signature` e expiração correta

**Dependências**: nenhuma (pode rodar em paralelo com BE-05/06)

---

## BE-08 — Endpoints de imagem (foto-pedido e comprovante)

**Contexto**: dois endpoints que retornam 302 redirect pra pre-signed URL.

**Arquivos**:
- Adicionar handlers em `PedidoController.java`

**Critério de aceitação**:
- `GET /api/v1/pedidos/{id}/foto-pedido` retorna 302 com `Location` apontando pra pre-signed URL do S3 do `s3KeyPedido`
- `GET /api/v1/pedidos/{id}/comprovante` retorna 302 com `Location` da pre-signed URL do `s3KeyComprovante`; retorna 404 se status != PAGO
- Header `Cache-Control: private, max-age=600`
- Verificação de `requisitante_id` igual à listagem
- Teste: ok, 404 quando comprovante não existe, 403 quando outro requisitante

**Dependências**: BE-06, BE-07

---

## BE-09 — Endpoint GET /api/v1/resumo

**Contexto**: agregação simples pra o cabeçalho do site (3 pendentes, R$ 7.230 etc).

**Arquivos**:
- `application/usecase/ResumoMesUseCase.java`
- Handler em controller

**Critério de aceitação**:
- Retorna `ResumoMesDTO` com `mesAtual`, `pendentes.{quantidade,total}`, `pagos.{quantidade,total}`
- Considera apenas pedidos do `requisitante_id` autenticado, mês corrente
- Teste: cenário com 3 pendentes + 9 pagos no mês

**Dependências**: BE-04

---

## BE-10 — Tabela e service de auth_token + endpoint admin pra gerar

**Contexto**: gerar tokens de uso único pra mandar pelo zap.

**Arquivos**:
- `domain/AuthToken.java`
- `application/usecase/GerarTokenConviteUseCase.java`
- `infra/persistence/AuthTokenRepository.java`
- `infra/web/AdminController.java` (ou comando CLI; recomendo controller protegido por header `X-Admin-Key` lido do Secrets Manager pra simplicidade)
- `application/port/out/HashService.java` + impl

**Critério de aceitação**:
- `POST /admin/api/v1/requisitantes/{id}/convite` (com header `X-Admin-Key` válido) retorna `{ "url": "https://finbot.dom.br/entrar?t=ABC..." }`
- Gera token random 32 bytes base64url
- Armazena hash SHA-256 em `auth_token` com `expira_em = now() + 7 days`
- Sem header ou header inválido → 401
- Teste: gera token, valida que está no banco hashed

**Dependências**: BE-04

---

## BE-11 — POST /api/v1/auth/exchange + JWT + cookie

**Contexto**: troca o token de uso único por sessão de longa duração.

**Arquivos**:
- `application/usecase/ExchangeTokenUseCase.java`
- `infra/web/AuthController.java`
- `infra/security/JwtService.java` (gerar/validar JWT HS256)
- `infra/security/CookieFactory.java` (cria cookie HTTP-only)
- Configurar segredo do JWT no Secrets Manager (chave `jwt_secret`, 32 bytes random)

**Critério de aceitação**:
- `POST /api/v1/auth/exchange` com body `{ "token": "..." }` valida hash, marca `usado_em`, retorna 200 com cookie `finbot_session` (HttpOnly, Secure, SameSite=Lax, Max-Age=15552000)
- Body resposta: `{ "requisitante": { "id": 1, "nome": "Pedro Marques" } }`
- 401 se token expirado, já usado, ou hash inválido
- Renovação automática: se JWT tem >50% da validade gasta, devolver novo cookie em qualquer request autenticado
- Teste: fluxo completo + 3 cenários de 401

**Dependências**: BE-10

---

## BE-12 — Filter/middleware de autenticação WebFlux

**Contexto**: garantir que endpoints `/api/v1/**` (exceto `/auth/exchange`) exigem JWT válido e injetam `requisitanteId` no contexto da request.

**Arquivos**:
- `infra/security/JwtAuthenticationWebFilter.java`
- `infra/security/SecurityConfig.java`

**Critério de aceitação**:
- Lê cookie `finbot_session`, valida JWT, popula `ReactiveSecurityContext` com `requisitanteId`
- Endpoints anônimos: `/api/v1/auth/exchange`, `/admin/**` (auth dele é por header próprio), `/actuator/health`
- Demais endpoints: 401 se cookie ausente/inválido
- `GET /api/v1/auth/me` retorna `{ "requisitante": { ... } }` ou 401
- Use cases acessam `requisitanteId` via parâmetro injetado de `@AuthenticationPrincipal` ou similar — não buscar do banco a cada request

**Dependências**: BE-11

---

## BE-13 — CORS config

**Contexto**: front em domínio diferente da API.

**Arquivos**:
- `infra/web/CorsConfig.java`

**Critério de aceitação**:
- Allowed origin: lido de property `app.cors.allowed-origin` (em dev: `http://localhost:5173`; em prod: `https://finbot.dom.br`)
- Allowed methods: GET, POST
- Allowed headers: Content-Type, Authorization
- `allowCredentials = true`
- `maxAge = 3600`
- Teste: preflight OPTIONS retorna headers corretos

**Dependências**: BE-11

---

## BE-14 — Testes de integração E2E com Testcontainers

**Contexto**: garantir que migrações + endpoints + auth funcionam juntos.

**Arquivos**:
- `src/test/java/.../integration/AppIntegrationTest.java`
- `pom.xml` — adicionar dependências testcontainers + mysql

**Critério de aceitação**:
- Sobe MySQL via Testcontainers, aplica migrations Flyway
- Cenário 1: gera token admin → exchange → lista pedidos → 200
- Cenário 2: tenta listar sem cookie → 401
- Cenário 3: tenta acessar comprovante de outro requisitante → 403
- Cenário 4: filtro por data + status retorna apenas itens corretos
- Cenário 5: paginação retorna `total`, `totalPaginas` corretos

**Dependências**: BE-12, BE-13

---

# Fase 3b — Front

## FE-01 — Scaffold do projeto Vite + React + TS + Tailwind

**Contexto**: bootstrap do projeto.

**Arquivos** (em pasta nova `frontend/` na raiz do repo, ou repo separado se preferir):
- `package.json`, `vite.config.ts`, `tsconfig.json`, `tailwind.config.js`, `postcss.config.js`
- `src/main.tsx`, `src/App.tsx`, `src/index.css`
- `index.html`
- `.gitignore`, `.env.development`, `.env.production`

**Critério de aceitação**:
- `npm install && npm run dev` sobe em `localhost:5173` mostrando "Hello Finbot"
- Tailwind funciona (uma classe `bg-blue-500` aplica cor)
- TypeScript strict mode habilitado
- Variável `VITE_API_BASE_URL` lida em `src/lib/ambiente.ts`
- ESLint + Prettier configurados, `npm run lint` passa

**Dependências**: BE-04 (precisa do contrato fechado pros types)

---

## FE-02 — Tipos TS dos contratos da API

**Contexto**: criar types correspondentes ao OpenAPI da BE-04.

**Arquivos**:
- `src/api/tipos.ts`

**Critério de aceitação**:
- Interfaces para: `PedidoResumo`, `PedidoDetalhe`, `ResumoMes`, `Pagina<T>`, `Requisitante`, `Erro`
- Enum `StatusPedido` (`PENDENTE | PAGO`) e `TipoPagamento` (`BOLETO | PIX | TED | AGENDAMENTO | OUTRO`)
- Tipo `ListarPedidosFiltro` com query params

**Dependências**: FE-01, BE-04

---

## FE-03 — API client (fetch wrapper)

**Contexto**: camada que abstrai o fetch, injeta cookies, trata erro 401 (sessão expirada).

**Arquivos**:
- `src/api/client.ts`
- `src/api/pedidos.ts`
- `src/api/auth.ts`

**Critério de aceitação**:
- `client.get<T>(path, params)` e `client.post<T>(path, body)` funcionam
- `credentials: 'include'` em todas as requests (cookies)
- Em 401, redireciona pra `/erro?motivo=sessao-expirada`
- Em outros erros, lança `ApiError` com `codigo` e `mensagem`
- Funções: `listarPedidos(filtros)`, `buscarPedido(id)`, `urlFotoPedido(id)`, `urlComprovante(id)`, `obterResumo()`, `exchangeToken(token)`, `obterMe()`

**Dependências**: FE-02

---

## FE-04 — Roteamento e AuthGuard

**Contexto**: configurar React Router com proteção de rotas autenticadas.

**Arquivos**:
- `src/App.tsx` — definir rotas
- `src/paginas/Entrar.tsx`
- `src/paginas/Erro.tsx`
- `src/paginas/Home.tsx` (placeholder)
- `src/components/AuthGuard.tsx`
- `src/hooks/useAuth.ts`

**Critério de aceitação**:
- Rota `/entrar?t=...`: chama `exchangeToken`, em sucesso navega pra `/`; em erro navega pra `/erro?motivo=token-invalido`
- Rota `/`: protegida pelo AuthGuard. Se sem sessão, navega pra `/erro?motivo=precisa-link`
- Rota `/erro`: mostra mensagem amigável conforme `motivo` query param
- `useAuth` retorna `{ requisitante, status }` (status: `loading | autenticado | nao-autenticado`)
- Visual da página `Erro` com instrução clara: "Peça um novo link pro filho pelo zap"

**Dependências**: FE-03

---

## FE-05 — Componente PedidoCard (variante C)

**Contexto**: replicar fielmente o cartão da `docs/architecture/design-proposals/variante-c-timeline.html` em React.

**Arquivos**:
- `src/components/PedidoCard.tsx`
- `src/components/StatusBadge.tsx`
- `src/lib/formato.ts` (formatarMoeda, formatarData, formatarRelativo)

**Critério de aceitação**:
- Props: `pedido: PedidoResumo`, `onAbrirComprovante: () => void`
- Renderiza fiel ao mockup C (descrição, valor grande, status badge, datas, botão verde grande de "Ver comprovante" se PAGO)
- Botão de comprovante só aparece se `status === 'PAGO'`
- Datas formatadas em pt-BR ("4 de maio")
- Storybook ou página de showcase opcional pra revisão visual
- Acessibilidade: botão tem aria-label, área de toque mínima 44x44px

**Dependências**: FE-01

---

## FE-06 — Componentes de filtro (Status, Mês, Busca)

**Contexto**: filtros do topo da Home conforme variante C.

**Arquivos**:
- `src/components/FiltroStatus.tsx` (3 pills: Tudo / Pendente / Pago)
- `src/components/SeletorMes.tsx` (carrossel horizontal de meses)
- `src/components/BarraBusca.tsx` (input com debounce de 300ms)

**Critério de aceitação**:
- Cada componente é controlled (recebe value + onChange)
- SeletorMes mostra últimos 12 meses, marca o ativo
- BarraBusca debounce funcional (testar com fake timers)
- Visual fiel ao mockup C

**Dependências**: FE-01

---

## FE-07 — Página Home com integração TanStack Query

**Contexto**: tela principal. Lista pedidos da API com filtros aplicados.

**Arquivos**:
- `src/paginas/Home.tsx`
- `src/hooks/usePedidos.ts`
- `src/components/Timeline.tsx` (agrupa pedidos por dia com headers de data)
- `src/components/CarregandoLista.tsx` + `src/components/ListaVazia.tsx`

**Critério de aceitação**:
- Estado de filtros (status, mês, busca) sincronizado com URL via search params
- `usePedidos(filtros)` usa `useQuery` com `keepPreviousData: true`
- Pedidos agrupados por `data_pedido` com header tipo "Hoje", "Ontem", "4 de maio", "15 de abril"
- Header sticky no topo com estado de carregamento sutil
- Estado vazio: ilustração + texto "Nenhum pedido neste filtro"
- Estado de erro: botão "tentar novamente"
- Paginação: "Carregar mais" no fim da lista (não infinite scroll, mais previsível)

**Dependências**: FE-03, FE-05, FE-06

---

## FE-08 — Header com ResumoMes

**Contexto**: bloco superior da Home mostrando "Olá Pedro" + "12 pedidos nos últimos 30 dias".

**Arquivos**:
- `src/components/CabecalhoApp.tsx`
- `src/hooks/useResumo.ts`

**Critério de aceitação**:
- Mostra nome do `requisitante.nome`
- Mostra resumo do mês conforme `/api/v1/resumo`
- Loading skeleton enquanto carrega

**Dependências**: FE-03, FE-04

---

## FE-09 — Modal de comprovante

**Contexto**: ao clicar em "Ver comprovante", abre modal sobre a tela com a imagem grande e botão de download.

**Arquivos**:
- `src/components/ModalComprovante.tsx`

**Critério de aceitação**:
- Recebe `pedidoId`, abre modal com `<iframe src={urlComprovante(id)}>` (iframe lida com imagem e PDF nativamente)
- Botão "Baixar comprovante" abre URL em nova aba com `download` attribute
- Botão de fechar (X no canto + ESC + click fora)
- Mobile: ocupa 100% da tela
- Desktop: max-width 600px, centrado
- Loading state enquanto iframe carrega
- aria-label e foco gerenciado corretamente (foco no botão de fechar ao abrir, retorna ao trigger ao fechar)

**Dependências**: FE-05

---

## FE-10 — PWA: manifest + service worker + ícones

**Contexto**: o site precisa ser instalável na tela inicial do celular do Pedro.

**Arquivos**:
- `vite.config.ts` — adicionar `vite-plugin-pwa`
- `public/manifest.webmanifest`
- `public/icone-192.png`, `public/icone-512.png`, `public/apple-touch-icon.png` (gerar ou usar placeholder)

**Critério de aceitação**:
- Lighthouse "Installable" passa
- No Chrome do Android, aparece prompt "Adicionar à tela inicial"
- Aberto da tela inicial, abre fullscreen sem barra do navegador
- Service worker faz cache do shell + assets estáticos com `workbox-precaching`
- Cache de respostas da API: NÃO (sempre fresh, é dado financeiro)

**Dependências**: FE-07

---

## FE-11 — Acessibilidade e revisão final

**Contexto**: passar checklist de a11y antes de deploy.

**Arquivos**: vários (ajustes pontuais)

**Critério de aceitação**:
- Contraste mínimo AA (verificar com axe DevTools)
- Todos os botões e links têm texto acessível ou `aria-label`
- Navegação por teclado: Tab passa por todos os controles, Enter aciona, ESC fecha modal
- `lang="pt-BR"` no `<html>`
- Sem warnings do `axe` nas páginas Home e Modal
- Lighthouse acessibilidade ≥ 95

**Dependências**: FE-09, FE-10

---

# Fase 3c — Deploy

## DEP-00 — Decisão e registro do domínio (TAREFA MANUAL)

**Contexto**: bloqueante pra fase de deploy, mas não pra desenvolvimento. Não passa pra Claude Code; é decisão pessoal + cadastro no Registro.br.

**Critério de conclusão**:
- Domínio escolhido e registrado
- DNS apontando pra Route 53 (atualizar nameservers no Registro.br)

**Dependências**: nenhuma. **Resolver até início da DEP-01.**

---

## DEP-01 — Hosted zone Route 53 + ACM cert

**Contexto**: criar zona DNS e certificado HTTPS.

**Arquivos**:
- `infra/dns.tf`
- `infra/acm.tf` (provider aliased pra `us-east-1` — requisito CloudFront)

**Critério de aceitação**:
- `aws_route53_zone` criada
- `aws_acm_certificate` criado em `us-east-1` cobrindo `finbot.dom.br` e `*.finbot.dom.br`
- Validação DNS automática via `aws_route53_record` + `aws_acm_certificate_validation`
- `terraform plan` limpo, `terraform apply` sem erro

**Dependências**: DEP-00

---

## DEP-02 — S3 bucket + CloudFront distribution

**Contexto**: hospedagem do front compilado.

**Arquivos**:
- `infra/frontend.tf` (conforme esqueleto na seção 5 da especificação técnica)

**Critério de aceitação**:
- Bucket privado, sem acesso público
- CloudFront com OAC apontando pro bucket
- Custom error responses 403/404 → /index.html (SPA fallback)
- Aliases do domínio configurados, certificate ACM associado
- `aws_route53_record` apontando `finbot.dom.br` pro CloudFront
- Subir um `index.html` de teste manualmente, abrir no domínio, retorna 200 sobre HTTPS

**Dependências**: DEP-01

---

## DEP-03 — Subdomínio api.finbot.dom.br apontando pra EC2

**Contexto**: API consumida pelo front precisa de hostname próprio.

**Arquivos**:
- `infra/dns.tf` (adicionar record A)
- Nginx ou Caddy na EC2 com cert Let's Encrypt (ou ALB com ACM, dependendo do que já existe)

**Critério de aceitação**:
- `https://api.finbot.dom.br/actuator/health` retorna 200
- Cert HTTPS válido
- API atrás de proxy reverso (não expor Spring direto na 8443)

**Dependências**: DEP-02 (e EC2 da Fase 2 em pé)

---

## DEP-04 — GitHub Actions: deploy do front

**Contexto**: pipeline pra build + sync S3 + invalidate CloudFront.

**Arquivos**:
- `.github/workflows/deploy-frontend.yml`
- `infra/iam-github-oidc.tf` (OIDC trust + role com permissions S3+CloudFront)

**Critério de aceitação**:
- Push pra `main` na pasta `frontend/` dispara workflow
- Workflow: `npm ci`, `npm run build`, `aws s3 sync dist/ s3://...`, invalidate
- Autenticação via OIDC (sem long-lived secrets)
- Em ~5 min, mudança de código está em produção

**Dependências**: DEP-02

---

## DEP-05 — Configurar prod do backend pra cookie do site

**Contexto**: backend precisa saber o domínio do front pra CORS e pra cookie.

**Arquivos**:
- `application-prod.properties` (lido do Secrets Manager ou env)
- Atualizar `app.cors.allowed-origin = https://finbot.dom.br`
- Atualizar geração de cookie pra `Domain=.finbot.dom.br` se quiser compartilhar entre subdomínios

**Critério de aceitação**:
- Front em prod consegue chamar API e cookie é setado
- CORS preflight passa

**Dependências**: DEP-03

---

## DEP-06 — Teste E2E em produção

**Contexto**: gerar token via admin endpoint, mandar link real pro celular, abrir como o Pedro abriria.

**Critério de aceitação**:
- Geração de token via `curl` retorna URL `https://finbot.dom.br/entrar?t=...`
- Abrir no celular: faz exchange, redireciona pra Home, lista pedidos reais
- Abrir um comprovante: modal abre com imagem ou PDF carregando
- Adicionar à tela inicial: ícone aparece, abre fullscreen
- Fechar e reabrir: ainda autenticado (cookie persistiu)

**Dependências**: DEP-04, DEP-05

---

# Fase 3d — Evolução pós-MVP

Estas tarefas **não fazem parte do MVP**. Listadas aqui pra não esquecer e pra evitar que entrem por engano antes da hora.

## EVO-01 — Migração do bot de Telegram pra WhatsApp

Substituir o adapter de Telegram por adapter de WhatsApp Business API (ou ferramenta como Z-API/Evolution API). A arquitetura hexagonal já isola isso na porta de entrada. Decisão de provider em aberto.

## EVO-02 — Notificação automática de comprovante via zap

Quando comprovante é registrado, enviar mensagem automática pro `requisitante.telefone` com link `https://finbot.dom.br/?destacar={pedidoId}`. Depende de EVO-01 (precisa do canal).

## EVO-03 — OCR pra extrair valores de boletos automaticamente

Receber foto de boleto, rodar OCR (Textract da AWS é tentador pelo ecossistema), preencher valor automaticamente, pedir confirmação.

## EVO-04 — Resumos mensais via comando no bot

Comando `/resumo abril` retorna agregado de gastos do mês. Pode evoluir pra gráficos.

## EVO-05 — App "real" via Capacitor

Wrap do React em Capacitor pra publicar nas lojas. Considerar quando PWA instalável já não estiver mais sendo suficiente (provavelmente nunca, mas registrado pra quando aparecer demanda).

## EVO-06 — Multi-requisitante com convite self-service

Hoje só existe Pedro. Se outras pessoas (mãe, irmão, etc) começarem a usar, criar fluxo onde você cria requisitantes pelo bot e gera links. Backend já está preparado pra isso desde BE-01.

---

# Notas finais pra Claude Code

- Cada tarefa BE-* e FE-* é dimensionada pra 1-3 horas de trabalho focado. Se algo demorar mais, parar e reavaliar escopo.
- Antes de começar uma tarefa, verificar que dependências listadas estão `done`.
- Após terminar uma tarefa, atualizar este arquivo marcando como `[x]` (sugiro adicionar checkboxes na próxima passagem).
- Commits: 1 commit por tarefa, mensagem `[BE-05] listagem de pedidos com filtros`.
- Push em branch `feature/<task-id>-<slug>`, abrir PR pra `develop`, mergear quando verde.
- Se precisar de decisão de produto que não está aqui, parar e consultar o humano — não inventar.
