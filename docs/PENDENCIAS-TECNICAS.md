# Pendências técnicas

Lista de débitos técnicos conhecidos — coisas que **funcionam hoje** mas têm espaço pra melhoria, ajuste ou refactor. Não são bugs nem features; são itens de qualidade/manutenibilidade que valem revisitar quando houver folga ou quando o problema correlato aparecer.

Não confundir com `docs/plans/` (planos de tarefa ativos) nem com a seção "Fase 3d — Evolução pós-MVP" do FASE-3 (features futuras). Aqui é **dívida acumulada**, não evolução.

## Como usar

- Quando bater num assunto que vira pendência, adicionar item aqui com: descrição, contexto, fix sugerido, prioridade
- Quando decidir tratar um item, promover pra um plano em `docs/plans/BE-XX-*.md`
- Marcar item como `~~resolvido~~` (ou apagar) quando o plano correspondente for mergeado

---

## Itens abertos

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

### Substituir cert self-signed por Let's Encrypt + domínio real

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

### Rotação do token do Telegram

**Contexto:** durante o incidente de SSL/webhook, o token completo do bot foi colado no chat com o Claude. Mesmo o canal sendo razoavelmente seguro, token vazado é token vazado.

**Fix sugerido:** rotacionar no BotFather (`/revoke` → `/token`), atualizar `finbot-prod-secrets` com o novo, restart do `finbot.service`.

**Esforço:** baixo (~5 min).

**Prioridade:** alta. Fazer quando puder.

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

**Prioridade:** baixa-média. Não bloqueia o WhatsApp; é alinhamento de convenção **antes que o segundo adapter consolide a divergência**. Bom candidato a **FIX rápido** (`FIX-padronizar-restclient-builder.md` ou similar), idealmente entrando junto ou antes do refactor da porta agnóstica (BE-XX) da Sprint 02.

**Referências:**
- Status BE-18 (justificativa original do `RestClient.Builder`)
- `docs/architecture/adapter-whatsapp-cloud-api.md` §5 (convenção registrada)
- Discussão Arquiteto ↔ humano, 2026-05-27

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
