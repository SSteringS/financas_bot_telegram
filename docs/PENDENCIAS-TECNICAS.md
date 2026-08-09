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

## Itens resolvidos

### ~~Esconder `@RequisitanteId` do Swagger UI~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(FIX-hide-requisitanteid-swagger)`). Adicionado bloco `static { SpringDocUtils.getConfig().addAnnotationsToIgnore(RequisitanteId.class); }` em `OpenApiConfig.java`.

### ~~Escopo dos `@RestControllerAdvice` (BE-15b)~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(BE-15b)`). `GlobalTelegramExceptionHandler` migrado de `@ControllerAdvice` para `@RestControllerAdvice(basePackages = "...adapters.in.telegram")`. `RestExceptionHandler` já tinha `basePackages` correto desde a BE-11.

### ~~`server.ssl.key-store-password` hardcoded em `application-prod.properties`~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(FIX-keystore-password-secret)`). Trocado `finbot123` por `${keystore_password}`. **Ação manual obrigatória do humano antes do próximo deploy:** adicionar chave `keystore_password` com valor `finbot123` no segredo `finbot-prod-secrets` no AWS Secrets Manager.

### ~~Revisar mensagens de erro/ajuda do bot~~

Resolvido em `feature/backend-polish-evo07` (commit `fix(FIX-revisar-msgs-erro-bot)`). `PaymentRequestStrategy.parsePedido()` agora lança `InvalidMessageFormatException` (em vez de `IllegalArgumentException`) com mensagem didática e exemplos de todos os tipos. Mensagens de `InvalidCaptionException` em `PaymentProofStrategy` também atualizadas com exemplos. Mensagem de sucesso do pedido inclui o tipo detectado e dica quando OUTRO.
