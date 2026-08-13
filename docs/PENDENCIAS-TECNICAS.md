# Pendências técnicas

Lista de débitos técnicos conhecidos — coisas que **funcionam hoje** mas têm espaço pra melhoria, ajuste ou refactor. Não são bugs nem features; são itens de qualidade/manutenibilidade que valem revisitar quando houver folga ou quando o problema correlato aparecer.

Não confundir com `docs/plans/` (planos de tarefa ativos) nem com a seção "Fase 3d — Evolução pós-MVP" do FASE-3 (features futuras). Aqui é **dívida acumulada**, não evolução.

## Como usar

- Quando bater num assunto que vira pendência, adicionar item aqui com: descrição, contexto, fix sugerido, prioridade
- Quando decidir tratar um item, promover pra um plano em `docs/plans/BE-XX-*.md`
- Marcar item como `~~resolvido~~` (ou apagar) quando o plano correspondente for mergeado

---

## Itens abertos

### Config externalizada via `@Value` avulso, sem agrupamento nem validação de boot

**Contexto (identificado no diagnóstico do FIX-006, 2026-08-08):** as credenciais do WhatsApp são injetadas com `@Value` espalhados por 4 classes (`MetaSignatureValidator`, `WhatsAppWebhookController`, `WhatsAppMessageSenderService`, `WhatsAppMediaDownloaderService`), cada uma com sua própria política de default. Isso produziu dois problemas reais:

1. **Defaults inconsistentes** — `verify-token`/`app-secret`/`allowed-wa-ids` ganharam sentinela no FIX-001; `access-token`/`phone-number-id` ficaram fail-fast. A divergência não estava visível em lugar nenhum, só lendo as 4 classes.
2. **Falha em cascata, um deploy por vez** — Spring aborta no primeiro bean que falha, então cada deploy revelava só um placeholder quebrado. Ver `docs/aprendizado/spring-placeholder-aninhado-default.md`.

O FIX-006 resolve o sintoma (defaults no `.properties` + teste de regressão que varre placeholders sem default). A dívida estrutural permanece.

**Fix sugerido:** migrar pra `@ConfigurationProperties(prefix = "whatsapp")` + `@Validated`, com um record/classe única concentrando as 5 properties. Ganhos: política de default num lugar só, validação no boot com mensagem legível, e um ponto natural pro WARN de "sentinela ativo". Mesmo padrão se aplica a `telegram.*` e `app.*`.

**Esforço:** médio — toca 4 classes do adapter WhatsApp + testes. Fazer preferencialmente **junto** da BE-20 (ativação do canal), que já vai mexer nessa superfície.

**Prioridade:** média. Não é bug depois do FIX-006, mas é a causa raiz de duas quebras de deploy consecutivas.

---

### ~~Dois pacotes paralelos para enums de domínio (`domain/enums/` e `domain/vo/`)~~ ✅ resolvido em FIX-004

**Contexto (identificado na revisão da sprint 03, 2026-06-04):** o domínio tem dois pacotes para enums:
- `domain/enums/` — pré-existente: `StatusPedido`, `TipoArquivo`, `TipoPagamento`, `TipoUploadS3`
- `domain/vo/` — criado na sprint 03: `CategoriaPedido`, `FormaPagamento`

São convenções diferentes para a mesma coisa. `vo` vem de Value Object (DDD); `enums` é nomenclatura direta. Funcionam igual em código mas criam confusão: onde colocar o próximo enum?

**Fix sugerido:** escolher uma convenção e consolidar. Opção recomendada: mover os enums de `vo/` para `enums/` (mais enums → `enums/` já é o padrão do projeto) e deletar `vo/`. Exige atualizar imports em ~10 arquivos.

**Esforço:** baixo (~15 min com IDE ou `sed`).

**Prioridade:** baixa. Não afeta comportamento, só coerência estrutural.

---

### `DataIntegrityViolationException` importada na application layer (FecharMesServiceImpl)

**Contexto (identificado no review BE-026/029, 2026-06-04):** `FecharMesServiceImpl` (`application/services/`) importa `org.springframework.dao.DataIntegrityViolationException` (linha 21) para capturar violação de UNIQUE INDEX no passo 8 e traduzir para `FechamentoDuplicadoException`. Em arquitetura hexagonal estrita, esse catch deveria viver no adapter de saída (`PedidoPagamentoRepositoryAdapter.save()`), que traduz a exceção antes de ela chegar na application layer. Mesmo padrão existe em `RegistrarComprovanteServiceImpl` (pré-existente).

**Fix sugerido:** Mover o catch para `PedidoPagamentoRepositoryAdapter.save()`:
```java
// adapter catches DataIntegrityViolationException → lança PedidoDuplicadoPortException (domain exception)
// service só conhece PedidoDuplicadoPortException
```

**Esforço:** baixo — mas requer cuidado para não quebrar o `@Transactional` (a exceção deve ser lançada dentro da transação para garantir rollback).

**Prioridade:** baixa. O comportamento está correto e testado (B2 do FIX-003). É dívida arquitetural, não bug.

---

### `/api/funcionarios/**` sem autenticação JWT

**Contexto (identificado no review BE-026/029, 2026-06-04):** `JwtAuthenticationFilter.shouldNotFilter` só aplica JWT para paths `/api/v1/**`. Os endpoints `/api/funcionarios/**` (FolhaController, FuncionarioController) são servidos sem autenticação. Pré-existente desde BE-025 — aceito como design intencional desta fase.

**Fix sugerido:** Estender o filtro ou adicionar `HttpSecurity` com `authorizeRequests()` para cobrir `/api/funcionarios/**`. Alternativa: mover esses endpoints para `/api/v1/funcionarios/**` (mudança de URL — requer atualização no frontend).

**Esforço:** baixo a médio (depende se muda URL ou não).

**Prioridade:** ~~média~~ → **alta** — promovido pelo humano em 2026-06-04. Plano criado: `docs/sprints/03-folha-pagamento/plans/FIX-005-proteger-api-funcionarios-jwt.md`.

---

### Duplicação `parseMes`/`parseMesYearMonth` em FolhaController

**Contexto (identificado no review BE-026/029, 2026-06-04):** Dois métodos privados idênticos em `FolhaController` (linhas 156 e 167). Funciona, mas risco de divergência em manutenção futura.

**Fix sugerido:** Unificar em um único método `parseMesYearMonth`.

**Esforço:** trivial (3 linhas).

**Prioridade:** baixa.

---

### `terraform.tfstate` e `.tfstate.backup` commitados em `financas_bot_telegram/infra/`

**Contexto:** `terraform.tfstate`, `terraform.tfstate.backup` e a pasta `.terraform/` estão versionados no repositório. O backend remoto é S3 (`finbot-tfstate-satyans`), então esses arquivos locais são resíduo — a fonte da verdade é o state no S3. Ter o state local versionado cria risco: alguém pode confundir o arquivo local (potencialmente desatualizado) com o state real, ou pior, rodar `terraform` sem ter inicializado o backend S3 e sobrescrever o state remoto.

**Fix sugerido:** adicionar ao `.gitignore` da raiz (ou criar um `financas_bot_telegram/infra/.gitignore`):
```
**/.terraform/
*.tfstate
*.tfstate.backup
```
Depois remover os arquivos do tracking com `git rm --cached`.

**Esforço:** baixo (~10 min).

**Prioridade:** média. Não bloqueia nada hoje (o backend S3 funciona), mas é risco latente de confusão ou corrupção de state.

---

### ~~Substituir cert self-signed por Let's Encrypt + domínio real~~ ✅ resolvido em 2026-08-09

> **Resolvido em 2026-08-09.** O webhook foi repontado de `https://<ip>:8443` (self-signed, exigindo `certificate=@cert.pem` a cada registro) para `https://api.satyansaita.com/webhook/telegram`, atrás da Caddy com Let's Encrypt. `getWebhookInfo` agora reporta `has_custom_certificate: false`. Elimina a quebra silenciosa quando alguém roda `setWebhook` sem o parâmetro do certificado, e desacopla o webhook do recreate da EC2 (o `bootstrap.sh` regenera o keystore em todo replacement).
>
> **Pendente de formalização:** o `DEP-08-webhook-https-caddy.md` continua marcado como não executado e o `docs/architecture/estado-atual-prod.md:30` ainda descreve o webhook como self-signed na 8443. Atualizar ambos.

Contexto original abaixo, preservado.

### Substituir cert self-signed por Let's Encrypt + domínio real (histórico)

**Contexto:** hoje o bot usa cert auto-assinado em `/opt/finbot/keystore.p12`, e o webhook do Telegram precisa do cert registrado via `setWebhook` com parâmetro `certificate=@cert.pem`. Toda vez que alguém roda `setWebhook` sem o parâmetro, o cert "desregistra" e o webhook quebra silenciosamente (já aconteceu).

**Fix sugerido:**
1. Registrar domínio próprio (pendência da Fase 3 — DEP-00)
2. Apontar `bot.<dominio>.com.br` pro Elastic IP via Route 53
3. Configurar Let's Encrypt via certbot na EC2 OU usar ALB com cert ACM
4. Atualizar webhook pra `https://bot.<dominio>.com.br/webhook` (sem precisar uploadar cert)

**Esforço:** médio (~meio dia de trabalho de infra).

**Prioridade:** média. Vale fazer quando o domínio for definido (resolve pendência DEP-00 também).

---

### `telegram.allowed-user-ids` hardcoded em prod

**Contexto:** lista de usuários autorizados está em `application-prod.properties` como string. Adicionar/remover usuário exige novo deploy.

**Fix sugerido:** mover pra tabela ou pra Secrets Manager. Eventualmente, pra um endpoint admin que gerencia.

**Esforço:** médio.

**Prioridade:** baixa. Só vira problema quando aparecer 2º usuário do bot (multi-requisitante real). Por enquanto é 1 user.

---

### ~~Rotação do token do Telegram~~ ✅ resolvido em 2026-08-09

**Contexto:** durante o incidente de SSL/webhook, o token completo do bot foi colado no chat com o Claude. Mesmo o canal sendo razoavelmente seguro, token vazado é token vazado.

> **Resolvido em 2026-08-09**, durante o incidente de bot mudo (token revogado pela Telegram). Executado: `/revoke` no BotFather → novo token em `finbot-prod-secrets` → restart do `finbot.service` → `setWebhook` apontando para `https://api.satyansaita.com/webhook/telegram` (Caddy/Let's Encrypt, sem `certificate=`). Validado com tráfego real.

---

### Bot injetável: webhook do Telegram sem validação de `X-Telegram-Bot-Api-Secret-Token`

**Contexto (comprovado em 2026-08-09):** durante o diagnóstico do bot mudo, um `POST` sintético partindo da internet aberta para `https://api.satyansaita.com/webhook/telegram` foi **aceito e processado** pela aplicação. O `TelegramWebhookController` não valida nenhum segredo compartilhado — a única barreira é a whitelist `telegram.allowed-user-ids`, conferida contra o campo `message.from.id` que vem **no corpo que o atacante controla** (`TelegramWebhookController.java:51-56`).

Consequência: qualquer pessoa que descubra a URL cria pedido de pagamento no banco, e pode disparar upload para o S3. Já apontado como observação em `QA-004` sem tratamento.

**Fix sugerido:** o `setWebhook` aceita `secret_token`; o Telegram passa a enviar o header `X-Telegram-Bot-Api-Secret-Token` em toda entrega. Validar o header no controller (rejeitar 401/403 quando divergir), guardar o valor em `finbot-prod-secrets`, e re-registrar o webhook com o parâmetro. Aplicar o mesmo raciocínio ao endpoint WhatsApp — este já valida assinatura HMAC (`MetaSignatureValidator`), então serve de referência.

**Esforço:** baixo-médio — controller + config + teste + re-`setWebhook`.

**Prioridade:** **alta.** Não é teórico: a injeção foi executada com sucesso.

---

### Comprovante mal-formado (sem `#`) é classificado como pedido novo

**Contexto:** o webhook do bot recebe todas as mensagens num único endpoint e decide entre `PaymentRequestStrategy` e `PaymentProofStrategy` puramente pelo formato da legenda:

- `<valor> <descrição>` (regex `^(\d+([.,]\d{1,2})?)\s+(.+)$`) → pedido
- `#<id> <tipo>` (regex `#(\d+)\s+(.+)`) → comprovante

Se o usuário enviar foto de comprovante com legenda `123 pix` (esquecendo o `#`), o sistema **não dispara** a mensagem de erro descritiva prevista pra "comprovante mal-formado". A legenda cai no matcher do `PaymentRequestStrategy` (que aceita "número + texto"), e o bot salva um pedido novo com `valor=123` e `descricao="pix"`. Identificado no teste 4.4 do roteiro de validação manual do `feature/backend-polish-evo07`.

**Por que não corrigir agora:** comportamento é consequência do design atual de despacho por regex, está estável, e o único usuário do bot hoje já está treinado pra usar `#`. Não atrapalha no curto prazo.

**Fix sugerido (a decidir quando virar problema):**
- Detectar legendas que "parecem comprovante mal-formado" antes de cair no `PaymentRequestStrategy` (ex: começa com `#` mas não bate o regex completo) e responder com mensagem orientadora; ou
- Migrar a UI do bot pra comandos explícitos (`/pedido`, `/comprovante`) em vez de inferir por formato; ou
- Confirmar o tipo com botão inline antes de salvar quando a legenda for ambígua.

**Esforço:** médio. Decidir abordagem antes de implementar.

**Prioridade:** baixa. Vira problema relevante quando o bot expandir pra mais de um usuário ou quando a usabilidade do pai precisar de mais robustez.

---

### Headers de segurança no CloudFront (HSTS, CSP, etc.)

**Contexto:** o front será servido via CloudFront (DEP-02), mas a distribuição não envia headers de segurança. O caminho recomendado é um **Response Headers Policy** (`aws_cloudfront_response_headers_policy`, declarativo, sem código no edge) anexado ao cache behavior — ver `docs/aprendizado/cloudfront-functions-e-security-headers.md`. Surgiu na discussão pós-DEP-02.

**Fix sugerido:**
- Ganhos fáceis primeiro (baixo risco): `Strict-Transport-Security` (HSTS), `X-Content-Type-Options: nosniff`, `frame-options: DENY`, `Referrer-Policy`.
- `Content-Security-Policy` (CSP) **por último e com cuidado** — numa SPA é fácil quebrar estilos inline / bundle do Vite. Precisa calibrar `connect-src` pra liberar a API em `api.satyansaita.com` e testar contra o build real antes de aplicar. `preload` do HSTS só depois de HTTPS garantido em tudo.

**Esforço:** baixo pros headers simples (uma policy + anexar no behavior); médio pra CSP (calibração + teste).

**Prioridade:** média. **Não implementar agora** — o humano quer estudar o tema (sobretudo CSP) antes de aplicar. Não bloqueia o DEP-02 nem o deploy do MVP.

---

### Reavaliar fluxo de idempotência do webhook conforme a tabela cresce

**Contexto:** o adapter WhatsApp (e, após o refactor agnóstico, o Telegram) deduplica mensagens entrantes via tabela `mensagem_processada` no MySQL, com claim-then-process na mesma transação — ver `docs/architecture/adapter-whatsapp-cloud-api.md` §4.3. É uma **consulta/escrita SQL a mais por mensagem entrante**, não um cache. Decisão consciente: SQL resolve pro começo, mas é dívida a monitorar.

**Por que não tratar agora:** no volume atual (família, milhares de mensagens/ano) o custo é irrelevante e a unique key mantém o lookup eficiente. Otimizar agora seria especulação.

**Fix sugerido (quando virar problema, guiado por métricas — ver item abaixo):**
- Cache em memória (ex.: Caffeine com TTL curto) na frente do SQL pra absorver redeliveries quentes, mantendo o SQL como fonte persistente;
- Índice dedicado / revisão do plano de query se o `EXPLAIN` acusar;
- Limpeza/retenção agressiva de linhas antigas (job agendado);
- Particionamento por data, em último caso.

**Esforço:** baixo a médio, dependendo da abordagem.

**Prioridade:** baixa. Revisitar **com base nas métricas de latência de banco**, não por suposição.

---

### Instrumentar latência de consultas ao banco e de chamadas externas (métricas)

**Contexto:** a frente de observability da Sprint 02 prevê logs + alarmes, mas falta **medir tempo**. Com a idempotência adicionando query por mensagem, e o adapter WhatsApp fazendo chamadas externas (Graph API: envio + 2 passos de download de mídia; S3: upload), precisamos de visibilidade de latência pra (a) decidir o refactor de idempotência por dado e (b) enxergar lentidão/erro nos pontos fora do nosso controle. Ver `docs/architecture/adapter-whatsapp-cloud-api.md` §8.1.

**Fix sugerido:**
- Métricas de **tempo por consulta ao banco** (claim de idempotência + escritas de pedido/comprovante).
- Métricas de **tempo e taxa de erro de chamadas externas** (Graph API, S3).
- Métrica de **tempo de processamento fim-a-fim da mensagem entrante, segmentado por tipo** (`PEDIDO` vs `COMPROVANTE`), com dimensões `canal` e `resultado` (sucesso/falha). Os dois tipos carregam mídia (foto/PDF), então o custo bruto é parecido; segmentar é útil porque os caminhos são diferentes (strategies/usecases distintos, comprovante faz lookup do pedido pai), os modos de falha diferem, e deixa visível se uma regressão atinge um tipo mais que o outro. É a métrica de experiência real do usuário.
- Forma idiomática: **Micrometer** (já vem no Spring Boot) → export pro **CloudWatch** (IAM `CloudWatchAgentServerPolicy` já está na EC2). Calibrar pra não virar ruído.

**Esforço:** médio.

**Prioridade:** média. Casa com a frente de observability da Sprint 02 — o planner deve incorporar na task de observability, que a própria sprint sugere fazer **cedo** (rede de segurança pra debugar a migração de canal).

---

### Padronizar criação do `RestClient` via Builder (uniformizar Telegram com BE-18)

**Contexto:** o BE-18 (envio de mensagens HTTP) introduziu o sender com `RestClient.Builder` auto-configurado pelo Spring Boot, em vez do padrão atual do projeto (`AppConfig` expõe um `RestClient` singleton). A justificativa registrada no próprio status do BE-18 foi viabilizar `@RestClientTest` (slice de teste que amarra o Builder a um `MockRestServiceServer`). O próprio implementador classificou isso como "leve inconsistência" entre os adapters.

A análise do Arquiteto (2026-05-27) mostrou que **dá pra ter os dois lados**: manter o padrão "singleton no `AppConfig`" e **ainda** ganhar `@RestClientTest`, desde que o singleton seja **construído a partir do `RestClient.Builder` auto-configurado** (em vez de `RestClient.create()` direto). A slice intercepta o Builder; como o singleton vem dele, o mock vale pro singleton inteiro.

**Fix sugerido:**

1. No `AppConfig` do Telegram, trocar a criação direta pelo padrão de fábrica via Builder:

   ```java
   // de:
   @Bean
   RestClient telegramRestClient() {
       return RestClient.create(/* ... */);
   }

   // para:
   @Bean
   RestClient telegramRestClient(RestClient.Builder builder) {
       return builder
           .baseUrl(/* ... */)
           .defaultHeader(/* ... */)
           .build();
   }
   ```

2. Documentar a convenção no `AppConfig` com um comentário curto, pra adapters futuros (WhatsApp/Discord) seguirem o mesmo padrão de fábrica: **singleton, construído a partir do Builder auto-configurado**.

3. (Verificar) Segundo o status do BE-18, "os adapters Telegram não têm testes" — então provavelmente não há testes pra reescrever. Se houver, migrar pra `@RestClientTest` + `MockRestServiceServer`.

**Esforço:** baixo (~15–30 min).

**Prioridade:** baixa-média. Não bloqueia o WhatsApp; é alinhamento de convenção **antes que o segundo adapter consolide a divergência**. Bom candidato a **FIX rápido** (`FIX-padronizar-restclient-builder.md` ou similar), idealmente entrando junto ou antes do refactor da porta agnóstica da Sprint 02.

**Referências:**
- Status BE-18 (justificativa original do `RestClient.Builder`)
- `docs/architecture/adapter-whatsapp-cloud-api.md` §5.3 (convenção registrada)
- Discussão Arquiteto ↔ humano, 2026-05-27

---

### Coluna `pedido_pagamento.data_pagamento` é redundante (e mente via API)

**Contexto:** descoberto revisando o esquema em 2026-05-28. A coluna `pedido_pagamento.data_pagamento` (`LocalDate`) duplica informação que já vive em `comprovante.data_pagamento` (`LocalDateTime`). Pior: o estado atual da coluna é **inconsistente**:

- **Quem escreve no `pedido_pagamento.data_pagamento`:** ninguém no código de produção. Só o backfill da migração `V2` preencheu, baseado no `comprovante.data_pagamento` existente. O `RegistrarComprovanteServiceImpl` muda o `status` do pedido pra `PAGO`, mas **não** popula `data_pagamento`. Logo, pra todo pedido criado pelo bot pós-V2 (a maioria), o valor é sempre `NULL` mesmo após o pagamento.
- **Quem lê:** `ListarPedidosServiceImpl` e `BuscarPedidoServiceImpl`, que repassam o valor pros DTOs `PedidoResumoDTO` e `PedidoDetalheDTO` (`dataPagamento`, exposto na API REST com `@Schema "Data em que o pagamento foi efetuado"`). O front consome — e provavelmente vê `null` nesse campo pra todo pedido novo PAGO.

Resultado: a API expõe um campo que **mente** (`null` quando deveria ter dado).

**Fix sugerido (caminho A, recomendado):** dropar a coluna via Flyway, e derivar `dataPagamento` no service a partir do `comprovante.data_pagamento` (truncando `LocalDateTime` → `LocalDate`) quando o pedido estiver `PAGO`. Mantém o contrato da API (`PedidoResumoDTO.dataPagamento` continua existindo) com **valor real**, e elimina a duplicação — single source of truth fica no `comprovante`.

**Caminho B (alternativa, não recomendado):** popular a coluna no `RegistrarComprovanteServiceImpl` quando muda pra PAGO. Mata a inconsistência atual mas mantém a duplicação e o risco de divergência no futuro.

**Escopo do A:**
- Migração Flyway nova (drop column).
- `PedidoPagamentoEntity` e `PedidoPagamento` (domain) — remover campo.
- `PedidoPagamentoMapper` — remover linha.
- `ListarPedidosServiceImpl` e `BuscarPedidoServiceImpl` — derivar do comprovante (query/lookup quando `status=PAGO`).
- Testes — ajustar mappers e DTOs; testar a derivação.

**Esforço:** baixo-médio. **Prioridade:** **média.** Não bloqueia nada hoje (front aceita `null`), mas vira **alta** se o front começar a depender de "data de pagamento confiável" (ex.: agrupar por mês de pagamento, filtros).

---

### ~~Scripts E2E fora do type-check estático do TypeScript (QA-002 Obs 3)~~

**Contexto:** `frontend/e2e/tsconfig.json` existe mas não é referenciado em `frontend/tsconfig.json` (que só referencia `tsconfig.app.json` e `tsconfig.node.json`). Consequência: `npm run build` / `tsc -b` **não** verifica os scripts de E2E (`subir-stack.ts`, `aguardar-saude.ts`, `derrubar-stack.ts`). O `tsx` (via esbuild) os executa corretamente em runtime — mas erros de tipo só aparecem quando o script falha durante a execução dos testes, não na fase de build.

Adicionalmente, rodar `tsc -p e2e/tsconfig.json` isolado falha com TS5097 porque `e2e/tsconfig.json` herda de `tsconfig.app.json` (que tem `moduleResolution: bundler` com `allowImportingTsExtensions: true`) mas **desabilita** `allowImportingTsExtensions` — enquanto `subir-stack.ts` usa importações com extensão `.ts`.

**Identificado:** revisão QA-002 (Obs 3, 2026-06-01).

**Fix sugerido (duas opções):**
- **(a) Remover `e2e/tsconfig.json`** e aceitar que os scripts rodam via `tsx` sem type-check estático (mais honesto com a realidade atual).
- **(b) Corrigir `e2e/tsconfig.json`** para herdar de `tsconfig.node.json` em vez de `tsconfig.app.json`, e adicionar a referência em `tsconfig.json` — dá type-check real mas requer calibração das opções.

**Esforço:** baixo (~30 min).

**Prioridade:** baixa. Pode ser feito em QA-003, QA-004, ou como FIX separado pós-sprint 03.

> ~~**Resolvido em QA-003 (2026-06-03):** `e2e/tsconfig.json` corrigido — `allowImportingTsExtensions false→true`. O `tsconfig.json` principal ainda não referencia `e2e/tsconfig.json` explicitamente, mas o TS5097 que impedia a verificação estática isolada foi resolvido. Opção (b) parcialmente implementada.~~

---

### `playwright.config.ts`: `outputDir` e `outputFolder` do reporter HTML apontam pro mesmo diretório (QA-001 Obs 2)

**Contexto:** a implementação de QA-001 definiu `outputDir: './playwright-report'` e `reporter: [['html', { outputFolder: 'playwright-report' }]]`. O `outputDir` é onde o Playwright grava artifacts de debug (traces, vídeos, screenshots de falha) — o padrão semântico é `'./test-results'`. O `outputFolder` é onde o reporter HTML grava o relatório. Misturar os dois no mesmo diretório (`playwright-report/`) cria confusão ao inspecionar falhas: artifacts de debug e HTML ficam juntos.

O `.gitignore` já ignora `test-results/` separadamente — esse diretório será criado pelo Playwright de toda forma, ficando fora do `.gitignore` se `outputDir` permanecer em `playwright-report`.

**Identificado:** revisão QA-001 (Obs 2, 2026-06-01).

**Fix sugerido:** restaurar `outputDir` para `'./test-results'` (já no `.gitignore`) e manter `playwright-report/` apenas para o relatório HTML. Alteração de 1 linha em `frontend/playwright.config.ts`.

**Esforço:** baixíssimo (1 linha).

**Prioridade:** baixa. Não quebra nenhum teste. Ideal corrigir em QA-004 (já nota no plano) antes de a suíte estar em uso ativo.

---

### Link de consulta do comprovante (2ª mensagem do bot) está quebrado

**Contexto (reportado pelo humano em 2026-08-09):** ao enviar um comprovante (`#<id> <tipo>` + foto), o bot responde com **duas** mensagens. A segunda traz um link para consultar o comprovante, e esse link está **quebrado**.

**Decisão consciente:** não tratar agora. O bot voltou a funcionar após o incidente de revogação do token, e a prioridade é retomar o experimento em `docs/experiments/models-claude-experiment/`.

**Estado da investigação — atenção, há divergência não explicada:** inspecionando `PaymentProofStrategy.process()` na branch `fix/006-whatsapp-defaults-no-properties` (2026-08-09), existe **um único** `canalNotificadorPort.enviar()` (linha 94), e a mensagem que ele monta (linhas 91-93) **não contém link nenhum** — só `Pedido:` e `Tipo:`. A segunda mensagem observada em produção não foi localizada nas classes inspecionadas (`PaymentProofStrategy`, `CanalNotificadorPort`, `TelegramMessageSenderService`).

Hipóteses a testar, em ordem:
1. Produção roda build diferente do estado atual de `develop` (o deploy de 2026-08-08 levou 2,5 meses de código de uma vez).
2. A segunda mensagem vem de outro ponto do fluxo ainda não mapeado.
3. O que o humano chama de "segunda mensagem" é outra coisa (ex.: preview de link gerado pelo próprio Telegram a partir de uma URL do S3).

**Primeiro passo de quem pegar:** capturar o **texto literal** das duas mensagens no Telegram e cruzar com `journalctl -u finbot` no momento do envio. Sem isso, qualquer fix é chute — inclusive não está estabelecido se o link é de front (`satyansaita.com/...`), de API (`api.satyansaita.com/...`) ou uma presigned URL do S3 expirada. Existe `ObterUrlComprovanteServiceImpl` gerando presigned URL para leitura, o que torna a hipótese "URL expirada" plausível, mas **não verificada**.

**Esforço:** indefinido até reproduzir.

**Prioridade:** média. É bug funcional visível pro usuário final, mas contornável — o comprovante é consultável pelo site.

---

### Convenção `*IntegrationTest` não é verificada por nada

**Contexto (QA-012, débito #3 do implementador, endossado pelo Reviewer como prioritário — 2026-08-10):** o `excludedTestClasses` do `pitest-maven` no `pom.xml` filtra por `*IntegrationTest`. Essa exclusão **não é otimização, é requisito de viabilidade**: os testes de integração sobem MySQL 8 real via Testcontainers, e o PIT reexecuta a suíte que cobre cada mutante uma vez por mutante. Com container no caminho, o run não termina.

O problema: **a convenção de nome é a única coisa que segura essa exclusão, e nada a força.** Um teste de integração novo chamado `PedidoFluxoTest` (em vez de `PedidoFluxoIntegrationTest`) escapa do filtro e trava o run. Não há lint, não há gate de CI, e a falha só aparece quando alguém roda o PIT — possivelmente meses depois de o teste ter sido escrito.

**Fix sugerido:** teste que varre o classpath de teste e falha quando uma classe anotada com `@Testcontainers` (ou que estenda `AbstractIntegrationTest`) não termina em `IntegrationTest`. Roda dentro do `mvn test`, logo já cai no CI sem tooling novo. Duas implementações possíveis — ArchUnit (dependência nova, regra declarativa, extensível para outros invariantes de arquitetura) ou reflection puro (zero dependência, ~30 linhas, resolve só este caso).

**Mitigação já aplicada:** regra declarada em `financas_bot_telegram/CLAUDE.md` §"Convenções que valem hoje" e no comentário do `pom.xml`. **Declaração não é verificação** — o débito continua aberto até existir o teste.

**Esforço:** baixo. **Prioridade: alta** — é a dependência silenciosa de todo o ferramental de mutation testing da sprint 04.

---

### Repo não tem infraestrutura de captura de log em teste

**Contexto (QA-012, débito #2 — 2026-08-10):** o run do PIT revelou um mutante sobrevivente em `MetaSignatureValidator:28` — negar a condicional inverte **quando** sai o `logger.warn("whatsapp.app-secret nao configurado — canal WhatsApp inerte")`. Não é mutante equivalente: a saída observável muda, e esse warn é sinal operacional deliberado (sem secret, o canal WhatsApp fica inerte).

O que falta não é o teste, é a **capacidade** de escrevê-lo: nenhum teste do repo verifica logging, porque não há appender de captura montado (`ListAppender` do Logback, ou equivalente).

**Fix sugerido:** helper de teste com `ListAppender<ILoggingEvent>` anexado ao logger sob teste, exposto como regra/extensão JUnit reutilizável. Depois, cobrir os avisos operacionais que valem asserção — os de configuração ausente em primeiro lugar.

**Esforço:** baixo para a infra; o uso cresce depois. **Prioridade: baixa** — hoje impacta uma linha de aviso. Sobe se mais sinais operacionais passarem a depender de log.

---

### Piso de custo do PIT é a suíte inteira, e um contexto Spring sobe dentro do run

**Contexto (QA-012, débitos #7 e #8, este último levantado pelo Reviewer — 2026-08-10):** antes de mutar qualquer coisa, o PIT roda uma fase de cobertura que executa **todas as classes de teste não-excluídas uma vez** — 70 classes hoje, ~22 s dos ~48 s do run. Entre elas há testes que sobem contexto Spring Boot sobre **H2** (banner, slices `@WebMvcTest`, `SessionFactory` do Hibernate), que emitem stack trace de DDL do Hibernate (`SchemaDropperImpl`) no shutdown.

Duas consequências, ambas não-óbvias:

1. **Encolher `targetClasses` acelera a fase de mutação, não a de cobertura.** Isso **limita o ganho esperado do item #7 do backlog da sprint 04** ("escopo por diff"): quem dimensionar o mecanismo precisa contar com esse piso antes de estimar. Mitigações a avaliar lá: `targetTests` explícito além de `targetClasses`, e/ou análise incremental.
2. **O ruído de DDL no log é esperado, não falha.** Fácil de confundir com quebra por quem rodar o PIT pela primeira vez.

**Mitigação já aplicada:** ambos documentados em `docs/runbooks/ROTEIRO-TESTES-BACKEND.md` §Camada 1.5, junto da triagem obrigatória antes de incluir classe em `targetClasses`.

**Esforço:** o registro está feito; o trabalho real acontece no item #7 do backlog. **Prioridade: média** — é insumo de dimensionamento, não bug.

---

### PIT roda sem histórico incremental e foi medido em JVM diferente da do CI

**Contexto (QA-012, débitos #4 e #5 — 2026-08-10):** dois itens de reprodutibilidade do mesmo ferramental.

1. **Sem histórico incremental.** `historyInputLocation` / `historyOutputLocation` não estão configurados: todo run parte do zero. Irrelevante em 4 classes; vira problema quando o escopo crescer (item #7 do backlog).
2. **Números colhidos em JVM 23-ea, enquanto CI e produção usam Temurin 21.** O `pom.xml` compila com `--release 21`, então é bytecode 21 nos dois casos e o baseline **não fica inválido**. Mas se o PIT for para o CI, os números precisam ser recolhidos lá antes de serem tratados como comparáveis com os da QA-012.

**Fix sugerido:** configurar as duas properties de histórico junto do item #7; recolher o baseline no CI se e quando o PIT entrar lá.

**Esforço:** baixo. **Prioridade: baixa** — nenhum dos dois afeta a leitura do baseline atual.

---

> **Nota de consolidação (QA-012, 2026-08-10).** O status report e a avaliação listaram 9 débitos. Quatro viraram itens acima. Os outros cinco não viraram, com motivo: **#1** (`LegendaParser` sem caso com palavra-chave no índice 0) é o alvo concreto do item #2 do backlog da sprint 04, não débito solto; **#6** (`cobertura_pct: na` sem JaCoCo) é o item #6 do mesmo backlog; **#9** (três imprecisões textuais no status report) já foi corrigido na rodada 2 do Reviewer. Registrar aqui duplicaria backlog vivo com dívida.

---

### Dois schemas de plano paralelos e divergentes (`qa_required`/`qa_rationale` vs `fluxos_qa`)

**Contexto (identificado ao materializar a ADR 0021, 2026-08-10):** o repo mantém **dois** schemas de plano de task, vivos ao mesmo tempo e sem ponte entre eles:

- `.claude/skills/artifact-report-contract/templates/plan.md` §Quality Gates — usa `review_required` / `qa_required` / `qa_rationale`, em inglês, consumido pelos subagentes (`planner.md`, `backend.md`, `qa-test-specialist.md`, skill `workflow-gates-core`).
- `docs/templates/_TEMPLATE-plano.md` (ADR 0007) — usa `fluxos_qa` no frontmatter, em português, consumido pelas sessões de planejamento e pelo fluxo de sprint em `docs/sprints/<NN>/plans/`.

São representações diferentes da mesma decisão ("esta task passa por QA?"), com nomes, idioma e tipo de valor distintos (booleano + justificativa vs lista de flows).

O campo `mutation_gate` (com `mutation_rationale`) foi escrito **nos dois** em 2026-08-10, por **decisão explícita do humano**, aceitando a duplicação temporária em vez de unificar os schemas na mesma passagem — unificar é task própria, com raio maior (toca `.claude/`, território do humano + `ai-engineer`).

**Risco:** divergência futura. O próximo campo de gate pode entrar só num dos dois, ou entrar nos dois com semântica diferente, e nada verifica isso. Quem lê um plano não sabe qual schema é o canônico.

**Fix sugerido:** eleger um schema canônico e derivar o outro (ou apontar um para o outro por referência, sem redefinir campos). Decisão precisa passar pelo `ai-engineer` porque metade do território é `.claude/`.

**Origem:** item 11.3 do `docs/plans/BACKLOG-evolucao-workflow.md`. Ver também ADR 0021 §6 ("Onde cada parte mora").

**Esforço:** médio — não é só editar dois arquivos; exige alinhar os agentes e a skill que leem os campos.

**Prioridade:** média. Não quebra nada hoje (os dois campos estão sincronizados), mas a duplicação é a causa provável da próxima divergência.

---

### Migração da organização de `docs/` de sprint para feature

**Contexto (decidido pelo humano em 2026-08-11, ao corrigir o item 11.4):** o humano decidiu que a unidade organizadora de `docs/` deve migrar de **sprint** para **feature**, mas **adiou a migração** até o fechamento do ciclo atual. Enquanto isso, o layout vigente e autoritativo continua sendo o da ADR 0010 (`docs/sprints/<NN>-<slug>/{plans,status,avaliacoes}/`).

Na mesma passagem, todo o **contrato de placement e naming de artefatos** foi absorvido pela skill `.claude/skills/artifact-report-contract` (seção `## Artifact placement and naming` em `references/artifact-contract.md`), escrito **no layout SPRINT** — o único válido hoje. O campo de metadata dos templates passou de `feature: F<NN>-<slug>` para `sprint: <NN>-<slug>`.

**Consequência:** quando a migração acontecer, o contrato dentro da skill precisa ser reescrito por inteiro — tabela de caminhos canônicos, campo `sprint` dos 8 templates, e as referências a "sprint" nas skills `workflow-gates-core` e `artifact-report-contract`.

**Fix sugerido:** a migração exige **ADR nova ou emenda à ADR 0010** (a ADR 0010 é a decisão que estabelece o agrupamento por sprint; mudar a unidade organizadora sem tocá-la deixaria duas verdades no repo). O passo de reescrita da skill entra no plano da migração como item explícito, não como efeito colateral — `.claude/` é território do humano e do `ai-engineer`.

**Origem:** item 11.4 do `docs/plans/BACKLOG-evolucao-workflow.md`.

**Esforço:** alto — toca a estrutura de `docs/`, a ADR, a skill de contrato e os agentes que citam caminhos de artefato.

**Prioridade:** baixa por ora (adiada por decisão do humano); vira alta no fechamento do ciclo atual.

---

### Rotacionar os segredos que estiveram versionados (FIX-007)

**Contexto:** o FIX-007 removeu segredos do controle de versão, mas **não é remediação** — só interrompeu a exposição contínua. Os valores estiveram num repositório **público** e devem ser considerados coletados.

**A rotacionar:**

- `admin_api_key` de dev.
- `keystore_password` — em `finbot-prod-secrets` **e** reassinando o `/opt/finbot/keystore.p12` da EC2. ⚠️ Trocar só o valor no Secrets Manager **quebra o boot da aplicação**: o keystore existente continua com a senha antiga e só é regerado quando o arquivo não existe.
- Senha do MySQL local.

O token do Telegram gen-1 já foi rotacionado em 2026-08-09 (registrado como resolvido acima).

**Origem:** pendência humana 1 do status report do FIX-007, migrada para cá no fechamento da sprint 03 (2026-08-12, decisão do humano).

**Esforço:** baixo por segredo; o do keystore exige acesso à EC2.

**Prioridade:** **alta — é o item de segurança mais urgente em aberto.**

---

### `keystore_password` não confirmado em `finbot-prod-secrets` — recreate da EC2 pode ficar sem aplicação

**Contexto:** `docs/sprints/01-mvp/status/DEP-07.md:98` afirma que a chave existe, mas é relato de 2026-05 e **não foi verificado** no FIX-007 (a sessão não tinha credencial AWS).

**Por que virou risco agora:** o FIX-007 mudou o comportamento do `bootstrap.sh` — antes o provisionamento sempre seguia; agora, se a chave não vier, ele **aborta com `exit 1`**. O abort acontece **antes** do `systemctl start finbot`/`caddy`, então a instância recém-criada fica **sem aplicação e sem reverse proxy**. Reboot recupera só o Caddy (`systemctl enable`); o finbot não volta, porque `application-prod.properties:7` aponta para o keystore que não foi criado, e o `Restart=on-failure` o joga em crash-loop. `user_data` não reexecuta em reboot. Recuperar exige intervenção manual.

**Falhar alto continua correto** — gerar keystore com senha errada quebraria o boot de forma mais obscura.

**Fix sugerido:** confirmar a chave antes do próximo recreate. Avaliar separadamente se vale mover o bloco do keystore para depois do start dos serviços (decisão de planner, ainda não tomada).

**Diagnóstico, se acontecer:** `journalctl -u cloud-init` ou `/var/log/bootstrap.log`, prefixo `[bootstrap] ERRO:`.

**Origem:** pendência humana 2 do status report do FIX-007.

**Prioridade:** média — latente, só dispara em recreate da EC2.

---

### Validação funcional do webhook WhatsApp pós-deploy nunca foi registrada (FIX-006)

**Contexto:** o FIX-006 restaurou a produção derrubada em 2026-08-09 e o deploy está verde (run `31341256203`). Mas o critério de aceitação real da correção é **comportamental** e não automatizável, e não há registro de que tenha sido executado:

- `GET /webhook/whatsapp?hub.mode=subscribe&hub.verify_token=xxx&hub.challenge=foo` deve retornar **403**.
- Dois WARN de sentinela devem aparecer no boot (`journalctl`).

**Estado:** deploy verde comprova que a aplicação **sobe**; não comprova que a guarda fail-closed **funciona**.

**Origem:** pendência humana 2 do status report do FIX-006.

**Prioridade:** média — checagem de minutos, mas é a única evidência que falta da correção.

---

### QA-010 — cobertura de testes frontend nunca executada

**Contexto:** plano pronto em `docs/sprints/03-folha-pagamento/plans/QA-010-cobertura-testes-frontend.md`, `estado: pronto-pra-execucao`, **nunca despachado**. A sprint 03 foi fechada sem ele por decisão do humano (2026-08-12).

**O gap:** inventário de produção × teste (qa-test-specialist, 2026-06-04) achou ~14 arquivos de front sem cobertura, em 4 sub-áreas:

- **Hooks** — `usePedidos`, `useResumo`, `folha/useFolhaFuncionario`. Só `useAuth` tem teste. Sem cobertura, regressão em cache/invalidation do TanStack Query passa silenciosa.
- **API clients** — `auth.ts`, `folha.ts`, `pedidos.ts`. É onde o shape de request/response do back vira tipo do front; sem teste, mudança de contrato passa sem ninguém ver.
- **Componentes utilitários** — `CarregandoLista`, `FiltroStatus`, `ListaVazia`, `SeletorMes`, `StatusBadge`.
- **Componentes e páginas da folha** — 6 arquivos criados na EVO-09 sem teste.

**Risco funcional de executar:** zero — não toca código de produto. MSW já configurado.

**Esforço:** alto (~12-14h estimadas, 14 arquivos de teste).

**Prioridade:** média. O plano continua válido; se for retomado, revalidar o inventário antes (o front mudou desde 2026-06-04).

---

### QA-011 — expansão E2E de cenários positivos nunca executada

**Contexto:** plano pronto em `docs/sprints/03-folha-pagamento/plans/QA-011-expansao-e2e-cenarios-positivos.md`, `estado: pronto-pra-execucao`, **nunca despachado**. Fechado junto com a sprint 03 por decisão do humano (2026-08-12).

**O gap, que é o mais relevante desta dupla:** a suíte E2E cobre **3 testes** — 1 fluxo feliz de site e 2 cenários negativos de webhook. **Todo o caminho positivo do produto está descoberto:**

- foto+caption no Telegram → pedido aparece no site;
- canal WhatsApp inteiro;
- registro de pagamento por upload de comprovante;
- fluxo de folha (cadastrar funcionário → vale → adiantamento → fechar mês).

Ou seja: a suíte protege contra regressão em **detalhe**, não no **happy path principal**.

**Bloqueios já resolvidos:** BE-030 (URL `telegram.file.url` configurável) foi mergeada, e o FIX-005 front também — as 4 sub-áreas estavam desbloqueadas quando a sprint fechou. A abordagem de mock decidida é WireMock standalone na 8089 (ADR 0020).

**Esforço:** alto (~10-14h estimadas).

**Prioridade:** média-alta pelo valor de cobertura, mas sem bloqueio — nenhuma entrega atual depende dela.

---

### Gate de convenção de branch do CI é sensível a locale — pode aceitar maiúscula

**Contexto (descoberto na FIX-008, 2026-08-12, por teste executado):** a regex de `.github/workflows/ci.yml` valida o nome da branch com a faixa `[a-z0-9]`. Faixa de caracteres em `[[ =~ ]]` do bash **depende de `LC_COLLATE`**:

| `LC_COLLATE` | `feature/QA-013` |
|---|---|
| `en_US.UTF-8` | **ACEITA** ⚠️ |
| `C` / `C.UTF-8` | rejeita |

Sob collation `en_US`, `[a-z]` casa maiúscula pela ordem de collation, não pelo conjunto de caracteres.

**Consequência:** a convenção lowercase de nome de branch **pode não estar sendo aplicada de fato**, e o comportamento muda **em silêncio** se a imagem do runner do GitHub Actions trocar o default de locale. O gate parece mais rígido do que é.

**Não medido:** qual `LC_COLLATE` os runners `ubuntu-latest` usam hoje. O teste foi feito na máquina local (Git Bash, `en_US.UTF-8`). Confirmar antes de assumir que a convenção está ou não sendo aplicada em produção.

**Fix sugerido:** trocar a faixa por classe POSIX — `[[:lower:][:digit:]]`, que é imune a collation — ou fixar `LC_ALL=C` no step. A primeira é preferível: expressa a intenção sem depender de variável de ambiente.

**Origem:** achado colateral da FIX-008; declarado no corpo do PR #126 e no status report da task.

**Esforço:** baixo — uma linha.

**Prioridade:** média. Não quebra nada hoje, mas é gate de qualidade que pode estar passando o que deveria barrar — e a classe de erro (faixa de caracteres sensível a collation) reaparece em qualquer script de shell do repo.

---

### Allowlist do gate de branch diverge do fluxo documentado, recorrentemente

**Contexto:** duas ocorrências da mesma falha, na mesma linha do mesmo arquivo:

- **FIX-002** (sprint 03) — `ci-aceitar-integration-no-gate-de-branch`: o gate rejeitava `integration/**`.
- **FIX-008** (2026-08-12) — o gate rejeitava `develop`, quebrando o PR de sincronização `develop → integration`.

**Causa comum:** a allowlist de `ci.yml` **codifica o fluxo de branches**, e ninguém a atualiza quando o fluxo ganha um caminho novo. A descoberta é sempre por falha de CI num PR legítimo, nunca por revisão.

**O que a FIX-008 já fez:** documentou o caminho `develop → integration` no `CLAUDE.md`, que descrevia só três caminhos. Isso trata a ocorrência, não a classe.

**Fix sugerido para a classe:** derivar a allowlist do gate da documentação de fluxo, ou — mais realista — adicionar ao ritual de abertura de sprint uma conferência de que todo caminho de branch previsto passa no gate. Uma terceira ocorrência indica que o item merece solução estrutural, não mais um FIX pontual.

**Prioridade:** baixa individualmente, média como padrão. Cada ocorrência custa pouco; o custo real é o PR travado no meio de outra coisa.

---

### `toUpperCase()`/`toLowerCase()` sem `Locale` — 5 ocorrências, 2 com corrupção silenciosa de dado

**Contexto (QA-013, 2026-08-12 — achados do PMD, regra `UseLocaleWithCaseConversions`, com o comportamento sob `tr-TR` medido):** conversão de caixa sem `Locale` usa o locale **default da JVM**. Sob locale turco, `"pix".toUpperCase()` produz `"PİX"` — com `İ` (I com ponto), não `I`. **Nenhum dos casos lança exceção**; todos degradam em silêncio.

| Onde | O que acontece sob `tr-TR` | Gravidade |
|---|---|---|
| `LegendaParser:21` | `pix` deixa de casar e é classificado como `OUTRO` **em memória** | real, transitório |
| `PaymentProofStrategy:81` | **grava `PİX`** em `comprovantes.tipo_pagamento` (`VARCHAR(255)`, sem `CHECK`) | **real e persistente** |
| `PedidoController:64` | `StatusPedido.valueOf(status.toUpperCase())` — único `Enum.valueOf` assim em produção | hoje inócuo, **por acidente** |
| `PedidoSpecs:42` | filtro — efeito provável é registro sumir do resultado | **não lido individualmente** |
| `ResumoMesServiceImpl:38` | agregação — mesmo efeito provável | **não lido individualmente** |

**O caso mais grave é o `PaymentProofStrategy:81`**, e a razão é a persistência: o dado errado **fica no banco depois** de o locale ser corrigido. Os demais se resolvem sozinhos quando o ambiente volta ao normal.

> ⚠️ **Correção de rota do Reviewer (achado `F1`, rodada 1):** a primeira versão do status afirmava que essa linha alimentava `Enum.valueOf` e **derrubava** o processamento da mensagem. É **falso** — o valor trafega e é persistido como `String`, e o rastreamento independente confirmou que não há `Enum.valueOf` nesse fluxo. Corrupção silenciosa, não exceção. Registrado aqui porque o ranqueamento errado quase entrou neste arquivo como incidente de disponibilidade.

**Sobre o `PedidoController:64`:** é inócuo hoje **só porque** nenhum valor de `StatusPedido` (`PENDENTE`, `PAGO`, `CANCELADO`) contém a letra `i` — acidente, não proteção. E `PedidoController:65-67` já captura `IllegalArgumentException` devolvendo 400, então o modo de falha real é **mensagem enganosa**, não indisponibilidade. Não priorizar acima do que vale.

**Fix sugerido:** `Locale.ROOT` em todas as 5. Ler antes as duas de `PedidoSpecs` e `ResumoMesServiceImpl`, que ninguém abriu ainda.

**Esforço:** baixo — 5 linhas. **Prioridade:** **alta** para `PaymentProofStrategy:81` (corrompe dado persistido); média para as demais.

**Nota de método:** nem o PIT nem os testes atuais pegam esses bugs — os testes rodam no locale da máquina. Um teste sob `-Duser.language=tr` pegaria.

---

### `AvoidCatchingGenericException` — 10 ocorrências em produção, 1 em teste

**Contexto (QA-013, baseline do PMD):** é o **maior bloco do baseline** — 11 das 23 violações. Captura de `Exception`/`RuntimeException` genérica engole causa e dificulta diagnóstico.

**Por que importa além do estilo:** é exatamente o modo de falha que o experimento de alocação de modelo quer medir em código gerado por LLM. O baseline alto significa que o sinal do experimento vai competir com ruído pré-existente se a medição não for por diff.

**Fix sugerido:** não corrigir em bloco. Tratar quando o arquivo for tocado por outro motivo.

**Prioridade:** média.

---

### Dois métodos com complexidade alta confirmada por duas métricas independentes

**Contexto (QA-013, medição do PMD):**

| Método | Ciclomática | Outra métrica |
|---|---:|---|
| `AtualizarFuncionarioServiceImpl.atualizar` | 12 | **NPath 2048** (threshold 200) |
| `CadastrarFuncionarioServiceImpl.validarDadosPagamento` | 18 | **Cognitiva 17** |

**Por que os dois casos são diferentes entre si:**

- No primeiro, **NPath 2048** significa 2048 combinações de caminho. Cobertura de caminhos ali é inalcançável na prática — não é meta realista, e vale saber disso antes de alguém tentar.
- No segundo, ciclomática e cognitiva **concordam** (18 e 17). Quando as duas concordam, não é artefato de contagem: é complexidade real de leitura. A limitação conhecida da ciclomática — não distinguir `switch` largo e legível de aninhamento profundo — não se aplica aqui, justamente porque a cognitiva confirma.

**Fix sugerido:** extração de método, quando houver motivo para tocar nas classes. **Prioridade:** média.

---

### Nenhuma ferramenta do repositório mede conformidade arquitetural

**Contexto (QA-013, desvio 2):** o PMD **não enxerga** violação de arquitetura hexagonal. O débito conhecido de `DataIntegrityViolationException` importada na camada de aplicação (registrado neste arquivo) continua **invisível** para o ferramental, mesmo depois de instalado.

**O ponto que generaliza:** das três ferramentas da sprint 04 — PIT, PMD e (futuro) JaCoCo — **nenhuma** mede conformidade arquitetural. Cobrir exigiria configurar `LoosePackageCoupling` com a lista de pacotes, ou **ArchUnit**.

**Convergência com o item #8 do backlog da sprint:** o gate da convenção `*IntegrationTest` já colocou ArchUnit em cima da mesa, e o argumento a favor dele era exatamente escalar para os outros invariantes que hoje só vivem em prosa no `financas_bot_telegram/CLAUDE.md` — JPA vive no adapter, adapters não se conhecem, nada novo em `usecases/`. **Este débito é a segunda evidência a favor da mesma decisão.**

**Prioridade:** média — sobe se o item #8 escolher ArchUnit.

---

### Testcontainers não alcança o Docker na máquina de desenvolvimento

**Contexto (QA-013, 2026-08-12/13):** 48 testes em 11 classes `*IntegrationTest` falham **localmente** com `Could not find a valid Docker environment` (`NpipeSocketClientProviderStrategy`). Sem container, o contexto cai em H2 sem schema e os testes morrem em `Table "AUTH_TOKEN" not found`.

**Não é código, e isso foi provado por execução:** o CI rodou a suíte completa sobre o mesmo commit e devolveu `Tests run: 422, Failures: 0, Errors: 0` (run `31727999562`). Os testes estão sãos.

**A armadilha:** `docker info` responde normalmente no shell. Isso **não** garante que a JVM do Maven alcança o daemon — foi o que enganou a primeira análise.

**Efeito prático:** quem desenvolver nessa máquina só descobre quebra de integração **no CI**, depois de abrir o PR. E enquanto isso, toda task tende a fechar `parcial` por um gate que não reflete o repositório.

**Fix sugerido:** FIX de ambiente próprio — configurar `DOCKER_HOST`/`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` ou trocar a estratégia de descoberta. **Não** consertar de carona em task de feature.

**Prioridade:** **alta** — é o gate de teste local do repositório inteiro.

---

### `SimplifyBooleanReturns` excluída do ruleset por bug de versão, sem gatilho de reavaliação

**Contexto (QA-013 + Reviewer, verificado por experimento controlado):** a regra foi removida do ruleset porque, no **PMD 7.7.0**, a mensagem sai com placeholder não substituído **e com o operador trocado em 2 dos 4 formatos** — sugere `!c || e` onde o correto é `!c && e`. Saída não acionável e que induz a erro é ruído legítimo de remover.

**O problema:** a exclusão é consequência de um **defeito de versão**, não de propriedade permanente da regra. Sem gatilho, vira definitiva por inércia.

**Fix sugerido:** amarrar a reavaliação ao próximo upgrade de PMD — junto com nova curadoria e **hash novo**.

**Mesmo raciocínio vale para `MissingSerialVersionUID`:** 25 ocorrências, todas em `*Exception.java`, deliberadamente fora do ruleset. A justificativa procede hoje; se algum dia este stack serializar exceção, a decisão precisa ser revisitada. O gatilho está no status da QA-013, **não** no XML congelado — e é o XML que sobrevive.

**Prioridade:** baixa.

---

### Código defensivo nunca exercitado — 3 gaps confirmados por duas ferramentas independentes

**Contexto (QA-014, medição do JaCoCo; confirmados pelo Reviewer):** cobertura de **branch** revelou buracos que a cobertura de **linha** escondia — as três classes têm 90–100% de linha.

| Onde | O gap | Por que importa |
|---|---|---|
| `FecharMesServiceImpl` linhas 59, 83, 85, 86, 87 | os **5 guards de `null`** de `fechar(...)` nunca são exercitados com `null` — 100% de linha, **79% de branch** | código defensivo que **nunca foi provado defender**. É o maior dos três |
| `PaymentRequestStrategy` linhas 88 e 97 | o `throw new InvalidMessageFormatException` de `parsePedido` nunca é atingido no recorte unitário | **é a mensagem de erro que o usuário final vê** — o caminho não verificado é o que ele encontra quando erra |
| `MetaSignatureValidator` linhas 57-59 | `catch (NoSuchAlgorithmException \| InvalidKeyException)` sem cobertura | baixa — exige HMAC-SHA256 ausente da JVM. Já era o `NO_COVERAGE` da QA-012; **JaCoCo confirmou independentemente** |

**O que torna isso confiável:** PIT e JaCoCo apontaram **as mesmas linhas**, não só percentuais parecidos. Duas ferramentas com implementações independentes concordando linha a linha é evidência forte de que o gap é real.

**Fix sugerido:** os dois primeiros são alvo natural do item #2 do backlog da sprint 04, junto com o `LegendaParser`. O terceiro provavelmente não vale o teste.

**Prioridade:** média para os dois primeiros; baixa para o terceiro.

---

### Cobertura da suíte de integração nunca foi medida

**Contexto (QA-014):** o baseline de cobertura é **unit-only** por decisão de plano — comparabilidade com o PIT e independência do Docker. A consequência é que **a cobertura real do projeto é desconhecida**, não apenas menor.

Os pacotes mais penalizados no recorte unitário são exatamente os que existem para conversar com infraestrutura:

| Pacote | Linha | Branch |
|---|---:|---:|
| `adapters/in/whatsapp/exceptionhandler` | 4/53 = **8%** | 0/18 = **0%** |
| `adapters/in/rest` (`RestExceptionHandler`) | 8/28 = 29% | — |
| `adapters/out/persistence/idempotencia` | 4/10 = 40% | — |
| `adapters/out/persistence` | 74/97 = 76% | 0/18 = 0% |

⚠️ **Não afirmar que esses pacotes estão descobertos.** Eles podem ser cobertos pelos `*IntegrationTest` — isso **não foi medido e não é mensurável nesta máquina hoje** (débito do Docker/Testcontainers). O `GlobalWhatsAppExceptionHandler`, pior número do projeto, é **indeterminado**, não ruim.

**Desbloqueia com:** o FIX de ambiente do Docker. Enquanto ele não vier, o número real fica declarado como não medido.

**Prioridade:** média — herda a do débito de ambiente.

---

### Oito classes em 0% de cobertura no recorte unitário

**Contexto (QA-014, verificado pelo Reviewer):** `ApiTelegramClientException`, `WhatsAppContact`, `WhatsAppMetadata`, `WhatsAppProfile`, `InvalidWhatsAppPayloadException`, `WhatsAppMediaDownloadException`, `MensagemProcessadaEntity`, `TelegramFileDownloadException`.

**Impacto baixo, e o Reviewer confirmou o porquê:** todas têm de **1 a 6 linhas** e são exceções ou DTOs. Entram no registro só para não sumirem do radar.

**Fix sugerido:** candidatas naturais a **exclusão de escopo** quando o recorte por diff do item #7 for automatizado — não a alvo de teste.

**Prioridade:** baixa.

---

## Itens resolvidos

### ~~Esconder `@RequisitanteId` do Swagger UI~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(FIX-hide-requisitanteid-swagger)`). Adicionado bloco `static { SpringDocUtils.getConfig().addAnnotationsToIgnore(RequisitanteId.class); }` em `OpenApiConfig.java`.

### ~~Escopo dos `@RestControllerAdvice` (BE-15b)~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(BE-15b)`). `GlobalTelegramExceptionHandler` migrado de `@ControllerAdvice` para `@RestControllerAdvice(basePackages = "...adapters.in.telegram")`. `RestExceptionHandler` já tinha `basePackages` correto desde a BE-11.

### ~~`server.ssl.key-store-password` hardcoded em `application-prod.properties`~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(FIX-keystore-password-secret)`). Trocado `<keystore-password>` por `${keystore_password}`. **Ação manual obrigatória do humano antes do próximo deploy:** adicionar chave `keystore_password` com valor `<keystore-password>` no segredo `finbot-prod-secrets` no AWS Secrets Manager.

### ~~Revisar mensagens de erro/ajuda do bot~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(FIX-revisar-msgs-erro-bot)`). `PaymentRequestStrategy.parsePedido()` agora lança `InvalidMessageFormatException` (em vez de `IllegalArgumentException`) com mensagem didática e exemplos de todos os tipos. Mensagens de `InvalidCaptionException` em `PaymentProofStrategy` também atualizadas com exemplos. Mensagem de sucesso do pedido inclui o tipo detectado e dica quando OUTRO.
