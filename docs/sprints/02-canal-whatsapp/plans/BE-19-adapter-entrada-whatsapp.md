# BE-19 — Adapter de entrada WhatsApp (webhook GET+POST, signature, mapper, allow-list)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** spec do arquiteto (`docs/architecture/adapter-whatsapp-cloud-api.md` §3, §4, §8) — item 4 da sequência sugerida do §10. Frente principal de produto restante da sprint 02 (EVO-01 = "ter o canal WhatsApp end-to-end").
> - **Prioridade:** alta — fecha o circuito end-to-end do WhatsApp (junto com PREP-WA + BE-20). Tudo o resto da sprint 02 (BE-20 deploy/smoke + EVO-02 templates) depende disso.
> - **Esforço:** **alto** — task densa. ~6–10 horas de coding (controller + 2 endpoints + signature validator + mapper + DTOs do envelope Meta + global exception handler + properties + testes). Comparável ao BE-18.
> - **Território / quem executa:** `financas_bot_telegram/src/main/java/.../adapters/in/whatsapp/` (criar do zero) + `application/exceptions/` e `application/services/MensagemEntranteService` (toques pontuais) + `application*.properties` → **Claude do back**.
> - **Branch:** `feature/be-19-adapter-entrada-whatsapp`, a partir de `develop`.
> - **Dependências:**
>   - **BE-17 em `develop`** ✅ (porta `MensagemEntrantePortIn` + DTO + strategies em `application/`).
>   - **BE-18 em `develop`** ✅ (sender + downloader que o mapper e o exception handler vão consumir).
>   - **BE-19a em `develop`** ✅ (idempotência embutida no `MensagemEntranteService` via `IdempotenciaMensagemPort` — o adapter de entrada do WhatsApp herda dedup de graça).
>   - **BE-21a em `develop`** ✅ (`Canal.WHATSAPP` enum disponível).
>   - **FIX-padronizar-restclient-builder em `develop`** ✅ (convenção do `RestClient.Builder` consolidada).
>   - **Não depende de PREP-WA concluído** pra escrever o código: os testes são contra mocks (`MockRestServiceServer`, `MockMvc`). Mas **executar o end-to-end** com a Meta real é BE-20, que depende de PREP-WA estar avançado (fase 7+ do runbook, com Test number ativo).
> - **Riscos:** médios — área nova e segurança-sensível (assinatura HMAC; sempre 200 com descarte silencioso de inválido; coordenação com ADR 0003). Mitigação: padrão do Telegram já existe pra copiar (controller + global exception handler + mapper); arquitetura está toda spec-ed pelo §4; testes com `MockMvc` + `MockRestServiceServer` cobrem os caminhos importantes; BE-20 (smoke real) é depois.

---

## Contexto

Após overnight 2 (BE-17 + BE-18 + BE-19a + BE-21a mergeados) e FIX-padronizar (RestClient.Builder), o backend já tem:

- **Porta agnóstica** `MensagemEntrantePortIn.processar(PaymentMessageDTO)` em `application/port/in/`, implementada por `MensagemEntranteService` que faz **idempotência (claim-then-process via `IdempotenciaMensagemPort`) + dispatch pra strategy** dentro de uma única transação.
- **DTO canal-agnóstico** `PaymentMessageDTO` com `canal: Canal`, `externalId`, `chatId`, `fromId`, `caption`, `fileBytes`, `fileExtension`, `tipoArquivo`, `mediaId`.
- **Strategies** (`PaymentRequestStrategy`, `PaymentProofStrategy`) em `application/strategy/`, agnósticas de canal — operam sobre `PaymentMessageDTO`.
- **Adapter de saída WhatsApp** em `adapters/out/whatsapp/`: `WhatsAppMessageSenderService` (texto livre + template) + `WhatsAppMediaDownloaderService` (2 passos: media_id → URL → bytes). Já usa `RestClient.Builder` (padrão da spec §5.3).
- **Enum `Canal`** com `TELEGRAM` e `WHATSAPP`.
- **Properties** com `whatsapp.graph-api-base-url`, `whatsapp.graph-api-version`, `whatsapp.phone-number-id`, `whatsapp.access-token` (em `application-prod.properties`, valores via Secrets Manager).
- **Padrão referencial** completo do Telegram em `adapters/in/telegram/`: controller + mapper + exception handler — todo o desenho que o WhatsApp espelha.

**O que falta** (= escopo desta task): o **adapter de entrada do WhatsApp**, equivalente ao do Telegram mas com as especificidades da Cloud API (handshake GET, signature HMAC sobre raw body, envelope da Meta, alvo "sempre 200" do ADR 0003 com descarte silencioso de payload não-autêntico, feedback obrigatório ao usuário em falha não-mapeada).

## Decisão / abordagem

**Espelhar a estrutura do Telegram, com 3 desvios específicos da Cloud API:**

1. **GET `/webhook/whatsapp` pra handshake** — é endpoint de configuração, não de evento; **pode retornar 403** se `verify_token` não bater (spec §4.1). Lê query params `hub.mode`, `hub.verify_token`, `hub.challenge`.
2. **POST `/webhook/whatsapp` com assinatura X-Hub-Signature-256 obrigatória** — HMAC-SHA256 do **corpo bruto** com `whatsapp.app-secret`. Validar **antes** de desserializar. Falha → log `WARN` + **200** (descarte silencioso, não 4xx — spec §4.2). Implementação: receber `@RequestBody byte[]` direto no método; desserializar com Jackson **depois** da validação. Mais idiomático que `ContentCachingRequestWrapper` filter e mantém a lógica no controller.
3. **Allow-list por `wa_id`** (número de telefone do remetente) — equivalente ao `telegram.allowed-user-ids`, propriedade `whatsapp.allowed-wa-ids` (lista CSV nas properties). Usuário não-autorizado → `UnauthorizedUserException` (já existe em `application/exceptions/`), handler envia mensagem amigável + 200.

**Fluxo do POST:**

```
1. Receber byte[] raw + header X-Hub-Signature-256
2. MetaSignatureValidator.validate(rawBody, signature) — HMAC-SHA256 com app-secret
   → inválido: WARN + return 200 (sem desserializar)
3. ObjectMapper.readValue(rawBody, WhatsAppWebhookPayload.class)
4. Pra cada entry[].changes[].value.messages[]:
   a. Autorizar wa_id (allow-list) → UnauthorizedUserException se não
   b. WhatsAppMessageMapper.toPaymentMessageDTO(message) — chama WhatsAppMediaDownloaderService se tiver media_id
   c. mensagemEntrantePortIn.processar(dto) — o port faz claim de idempotência + strategy dispatch
5. statuses[] (entregue/lido/falhou): só log INFO (sem ação de negócio — observability fica pra BE-22)
6. Return 200 (sempre)
```

**Por que NÃO retornar 5xx em erro de infra (divergindo do Telegram que ainda tem isso pra `DatabaseException`):** ADR 0003 + spec §4.2 são claros — sempre 200. A Meta **retenta** em não-2xx e a fila trava; preferimos perder a mensagem (caminho do §4.5: avisar o usuário pra reenviar) do que travar fila. O `GlobalWhatsAppExceptionHandler` retorna **200 em tudo** — incluindo `DatabaseException`. (O fato de o `GlobalTelegramExceptionHandler` retornar 500 em `DatabaseException` é débito técnico do Telegram — registrar como pendência futura, **não corrigir nesta task**.)

**Feedback ao usuário em falha não-mapeada** (spec §4.5): o handler genérico extrai `wa_id` do request attribute (mesmo padrão `__update` do Telegram, agora um `__whatsapp_payload` ou similar), tenta enviar via `WhatsAppMessageSenderService` uma mensagem "⚠️ Não consegui registrar sua mensagem agora. Pode reenviar daqui a pouco, por favor?" (best-effort, segunda falha vira só log). Retorna 200.

**O Telegram não muda nesta task.** A rota `/webhook` continua igual (o rename pra `/webhook/telegram` é a BE-17b, fora de escopo aqui — espera deploy coordenado com `setWebhook` da Meta). **Resultado:** após BE-19 deployar, vamos ter `POST /webhook` (Telegram) + `GET/POST /webhook/whatsapp` (WhatsApp) convivendo. A simetria de rotas vira na BE-17b.

## Escopo / arquivos

### Criar

**Pacote `adapters/in/whatsapp/`:**

- `controller/WhatsAppWebhookController.java`
  - `@GetMapping("/webhook/whatsapp")` — handshake (params `hub.mode`, `hub.verify_token`, `hub.challenge`)
  - `@PostMapping("/webhook/whatsapp")` — recebe `@RequestBody byte[]` + `@RequestHeader("X-Hub-Signature-256") String` + `HttpServletRequest`
  - Recebe via construtor: `MetaSignatureValidator`, `WhatsAppMessageMapper`, `MensagemEntrantePortIn`, `ObjectMapper`, `@Value("${whatsapp.verify-token}")`, `@Value("${whatsapp.allowed-wa-ids}") List<String>`

- `security/MetaSignatureValidator.java`
  - `@Component`
  - `boolean isValid(byte[] rawBody, String signatureHeader)` — extrai o prefixo `sha256=`, calcula `HmacUtils.hmacSha256Hex(appSecret, rawBody)` (Apache commons-codec, já no classpath transitivamente), compara em **constant-time** (`MessageDigest.isEqual`). Receber `appSecret` via `@Value("${whatsapp.app-secret}")`.

- `dto/` — record/POJOs do envelope Meta. Sugestão (records são suficientes; o sender já usa records-like DTOs):
  - `WhatsAppWebhookPayload(String object, List<Entry> entry)`
  - `Entry(String id, List<Change> changes)`
  - `Change(String field, Value value)`
  - `Value(String messagingProduct, Metadata metadata, List<Contact> contacts, List<Message> messages, List<Status> statuses)`
  - `Metadata(String displayPhoneNumber, String phoneNumberId)`
  - `Contact(String waId, Profile profile)`
  - `Profile(String name)`
  - `Message(String from, String id, String timestamp, String type, Text text, Image image, Document document)`
  - `Text(String body)`
  - `Image(String id, String mimeType, String sha256, String caption)`
  - `Document(String id, String mimeType, String sha256, String filename, String caption)`
  - `Status(String id, String status, String timestamp, String recipientId)` — só pra log
  - Usar `@JsonNaming(SnakeCaseStrategy)` ou `@JsonProperty` nos campos com snake_case (`messaging_product`, `phone_number_id`, `wa_id`, `display_phone_number`, `mime_type`). **Configurar `ObjectMapper` no Spring boot pra ser tolerante a `unknown_properties: false`** (a Meta evolui o envelope; ignorar campos desconhecidos previne quebra).

- `mapper/WhatsAppMessageMapper.java`
  - `@Component`
  - `PaymentMessageDTO toPaymentMessageDTO(Message message)` — espelha o `TelegramMessageMapper`:
    - **Texto puro** (`type=text`): `caption = message.text.body`, sem `fileBytes`/`tipoArquivo`/`mediaId`.
    - **Image** (`type=image`): chama `WhatsAppMediaDownloaderService.download(message.image.id)` → bytes; `caption = message.image.caption`; `tipoArquivo = TipoArquivo.IMAGEM`; `fileExtension = extensaoDeMime(message.image.mimeType)` (helper pode espelhar o do Telegram).
    - **Document** (`type=document`): mesma lógica do Telegram — `application/pdf` → PDF; `image/*` → IMAGEM com extensão derivada do mime; outros → `TipoArquivoNaoSuportadoException` (já existe em `adapters/in/telegram/exception/` — ou criar equivalente em `adapters/in/whatsapp/exception/`; ver "Tratamento de exceptions duplicadas" abaixo).
    - **Outros tipos** (`audio`, `video`, `location`, etc.): por enquanto **rejeitar** com `TipoArquivoNaoSuportadoException` (escopo do MVP é foto/documento + texto).
    - Preencher sempre: `canal=Canal.WHATSAPP`, `externalId=message.id` (= `wamid`, chave de idempotência), `chatId=Long.parseLong(message.from)` ou guardar como String (ver "Decisão a tomar" abaixo), `fromId=message.from`.

- `exception/` — exceções específicas do canal WhatsApp:
  - `InvalidWhatsAppPayloadException` (corpo desserializado mas estrutura inesperada, ex.: `messages` vazio + `statuses` ausente)
  - `WhatsAppMediaDownloadException` (falha no download dos 2 passos da Graph API — pode reusar `WhatsAppApiException` que já existe em `adapters/out/whatsapp/service/`? Ver "Decisão a tomar")
  - **Avaliar reuso** das exceções `PhotoProcessingException`, `TipoArquivoNaoSuportadoException`, `InvalidCaptionException` do Telegram — semânticas idênticas, hoje moram em `adapters/in/telegram/exception/`. **Decisão do implementador** (anotar no status): manter no Telegram + copiar pro WhatsApp (duplicação), ou **mover pra `application/exceptions/`** (compartilhado canal-agnóstico) e ambos importam. Recomendo a segunda — mas é refator extra de escopo limitado e o implementador decide se entra agora ou vira FIX-separada.

- `exceptionhandler/GlobalWhatsAppExceptionHandler.java`
  - `@RestControllerAdvice(basePackages = "...adapters.in.whatsapp")`
  - Espelha os handlers do Telegram, mas:
    - **Sempre retorna 200** — inclusive `DatabaseException` (vide "Decisão" acima).
    - Sender = `WhatsAppMessageSenderService.enviarTexto(waId, msg)`.
    - Extrai `wa_id` do request attribute (definido pelo controller antes do mapper, ex.: `request.setAttribute("__whatsapp_wa_id", waId)`).
  - Mapear: `UnauthorizedUserException`, `InvalidMessageFormatException`, `PedidoNaoEncontradoException`, `BusinessRuleException`, `DatabaseException` (200, não 500), `Exception` (generic — feedback "tente de novo"), eventuais exceções específicas (`InvalidWhatsAppPayloadException`, `WhatsAppMediaDownloadException`).

### Modificar

- `src/main/resources/application.properties` — adicionar `whatsapp.verify-token=CHANGE_ME`, `whatsapp.app-secret=CHANGE_ME`, `whatsapp.allowed-wa-ids=CHANGE_ME` (CSV de números no formato internacional sem `+`, ex.: `5511999998888,5511988887777`).
- `src/main/resources/application-dev.properties.example` — espelhar.
- `src/main/resources/application-prod.properties` — adicionar `whatsapp.verify-token=${whatsapp_verify_token}`, `whatsapp.app-secret=${whatsapp_app_secret}`, `whatsapp.allowed-wa-ids=${whatsapp_allowed_wa_ids}` (ou hardcode os números se for trivial e tu já tiver lista — implementador decide).
- `MensagemEntranteService.ERROR_MESSAGE` (em `application/services/`) — **revisar a string**. Hoje cita "envie a foto com a legenda no formato" — pode ficar igual, ou ajustar pra "envie a foto/documento" (já vale pros dois canais). **Decisão do implementador** — anotar.

### Não tocar (escopo limitado)

- `TelegramWebhookController` nem o pacote `adapters/in/telegram/` — BE-17b é separada.
- `adapters/out/whatsapp/` — só consumir; não alterar `WhatsAppMessageSenderService` nem `WhatsAppMediaDownloaderService`. Se descobrir bug no sender/downloader durante BE-19, abrir FIX separada.
- `MensagemEntranteService.processar()` core — ele já está pronto pra receber `Canal.WHATSAPP` no DTO (idempotência canal-agnóstica via BE-19a). Ajuste só na string `ERROR_MESSAGE` se decidir.
- Strategies em `application/strategy/` — já são canal-agnósticas.
- Banco / migrations — `mensagem_processada` já existe e funciona pros dois canais.
- Eventos (BE-21a) — `NotificacaoComprovanteListener` já roteia por `canalPreferido`; quando um pedido vier via WhatsApp, o `requisitante.canalPreferido` resolve quem recebe a notificação. **Mas atenção:** o `TelegramNotificadorImpl` existe; **`WhatsAppNotificadorImpl` (pra EVO-02) ainda não** — não é escopo desta task; é BE-21b ou similar.
- `application/exceptions/UnauthorizedUserException`, `BusinessRuleException`, `DatabaseException`, `PedidoNaoEncontradoException` — já existem e são canal-agnósticos. Só consumir.

## Testes

- **Unit `MetaSignatureValidatorTest`:** assinatura correta passa, assinatura modificada falha, assinatura sem prefixo `sha256=` falha, body vazio/null trata graceful.
- **Unit `WhatsAppMessageMapperTest`:** texto puro → DTO com `caption` e sem media; image com caption → DTO com `fileBytes` (mocking do downloader), `tipoArquivo=IMAGEM`; document `application/pdf` → `tipoArquivo=PDF`; document `image/jpeg` → `tipoArquivo=IMAGEM`; document de tipo não-suportado → `TipoArquivoNaoSuportadoException`; outros types (audio/video/location) → `TipoArquivoNaoSuportadoException`.
- **`@WebMvcTest WhatsAppWebhookControllerTest`** (mockando `MetaSignatureValidator`, `WhatsAppMessageMapper`, `MensagemEntrantePortIn`, `WhatsAppMessageSenderService`):
  - `GET /webhook/whatsapp` com `hub.mode=subscribe` + `hub.verify_token` correto → **200 + body = challenge**.
  - `GET /webhook/whatsapp` com token errado → **403**.
  - `POST /webhook/whatsapp` com assinatura válida + payload de pedido válido (image + caption "150.50 Almoço") → **200**, port chamado uma vez com DTO esperado.
  - `POST /webhook/whatsapp` com assinatura inválida → **200**, port **não** chamado, log WARN registrado.
  - `POST /webhook/whatsapp` com `wa_id` não-autorizado → **200**, sender chama mensagem "🚫 sem permissão".
  - `POST /webhook/whatsapp` com payload só de `statuses[]` (sem `messages[]`) → **200**, port não chamado, log INFO.
  - `POST /webhook/whatsapp` que dispara `Exception` genérica no port → **200**, sender chama feedback "tente de novo".
- **Não precisa de Testcontainers** — o teste de idempotência é da BE-19a; aqui o foco é o adapter de entrada.

**`testes_total`** deve subir de ~248 (estado pós-FIX-padronizar) pra ~270-280 (estimativa: 20-30 testes novos). Anotar `testes_total` e `testes_novos` no status report.

## Critérios de aceitação

- [ ] Endpoint `GET /webhook/whatsapp` responde **200 + challenge** com token correto, **403** com token errado.
- [ ] Endpoint `POST /webhook/whatsapp` valida `X-Hub-Signature-256` antes de desserializar, retorna **200** em assinatura inválida (descarte silencioso + log WARN).
- [ ] `POST /webhook/whatsapp` retorna **200 em TODOS os caminhos** — válido, inválido, exceção não-mapeada, falha de infra. Nenhum 4xx/5xx.
- [ ] Mapper monta `PaymentMessageDTO` com `canal=WHATSAPP`, `externalId=wamid` (chave de idempotência), `fileBytes` populado pra image/document, `caption` populado pra texto e mídia.
- [ ] Idempotência herda do `MensagemEntranteService` (mensagem repetida com mesmo `wamid` → ignorada).
- [ ] Allow-list por `whatsapp.allowed-wa-ids` funciona; `wa_id` não-autorizado dispara `UnauthorizedUserException`.
- [ ] Properties novas em todos os profiles + `.example`.
- [ ] `mvn test` verde, com `testes_total` ≥ 270 e `testes_novos` ≥ 20.
- [ ] `mvn package -DskipTests` ok.
- [ ] Branch saiu de `develop` (fluxo novo do CLAUDE.md: `git fetch && git checkout -b feature/be-19-adapter-entrada-whatsapp develop`).
- [ ] Território: só `financas_bot_telegram/`. Não toca `frontend/` nem `infra/`.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/BE-19.md` com frontmatter válido, anotando:
  - Decisão sobre reuso de exceções `PhotoProcessing`/`TipoArquivoNaoSuportado` (mover pra `application/exceptions/` ou duplicar).
  - Decisão sobre o `ERROR_MESSAGE` (manter "envie a foto" ou ajustar pra "foto/documento").
  - Decisão sobre chatId (`Long` vs `String` no DTO — wa_id é número longo, talvez não caiba em `Long`).
  - Eventuais TODOs deixados pra BE-20 (smoke real) ou pra BE-21b (`WhatsAppNotificadorImpl` pra EVO-02).

## Fora de escopo (explicitamente)

- **BE-17b** (renomear rota Telegram `/webhook` → `/webhook/telegram` + `setWebhook` coordenado) — task separada. BE-19 deixa o Telegram intocado em `/webhook`.
- **BE-20** (deploy + config webhook na Meta + smoke real com Test number / Pedro) — depende desta + PREP-WA + cert HTTPS (DEP-03 ou Caddy/DEP-08).
- **BE-21b / EVO-02 via WhatsApp** — `WhatsAppNotificadorImpl` (envio de notificação após `ComprovanteRegistradoEvent` via WhatsApp template) é task separada. Esta BE-19 só recebe; quem responde é o `NotificadorPortOut` já existente do Telegram (se `canalPreferido=TELEGRAM`) ou um novo do WhatsApp (BE-21b).
- **Templates de utilidade na Meta** — submissão/aprovação do template é manual no console da Meta, dependência de BE-21b.
- **BE-22 / observability do listener** — Micrometer + counters; lá entra também o timer fim-a-fim da entrada WhatsApp.
- **`DatabaseException` 500 do Telegram** — débito técnico conhecido; corrigir aqui violaria escopo. Registrar pendência separada.
- **Mover exceções compartilhadas Telegram→application** — opcional dentro desta task; se virar trabalho real, vira FIX separada.
- **Validação de timestamp** (descartar mensagens antigas demais) — Meta entrega `timestamp`; descartar > N dias seria heurística defensiva, mas a idempotência por `wamid` já cobre o caso prático de redelivery. Fora de escopo.
- **Endpoint `/health` ou Actuator** — está em `PENDENCIAS-TECNICAS.md`, fora de escopo aqui.

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| HMAC implementado errado (assinatura inválida sempre / sempre válida) | Média | Alto (segurança) | Teste unit dedicado com vetor conhecido. Constant-time comparison (`MessageDigest.isEqual`). Testar com payload real do Test number na BE-20. |
| Raw body lido de forma incorreta (Spring desserializa antes da validação) | Média | Alto | Usar `@RequestBody byte[]` direto. Garantir que Jackson **só** roda **depois** da validação no controller. Teste valida que controller com assinatura inválida não chama o mapper (= não desserializou). |
| Envelope da Meta tem campo desconhecido e Jackson explode | Média | Médio | `ObjectMapper` configurado com `FAIL_ON_UNKNOWN_PROPERTIES=false` (provavelmente já é default). Testar com payload contendo campos extras. |
| `wa_id` excede `Long` ou tem dígito não-numérico | Baixa | Médio | Inspecionar formato real (números E.164 sem `+`, ex.: `5511999998888` cabe em Long mas no limite). **Decisão do implementador**: manter `chatId: Long` no DTO ou migrar pra `String`. Anotar. |
| ADR 0003 violado por engano (algum handler retornando 4xx/5xx) | Baixa | Alto | Teste de cada caminho garante 200. Code review verifica. |
| Mensagem de feedback em falha do sender entra em loop (sender falha → handler tenta enviar feedback → falha de novo) | Baixa | Médio | Handler envolve o `sendMessage` em try/catch interno, segunda falha → só log, não propaga. Padrão já existe no Telegram. |
| Mídia downloader trava o request (Graph API lenta) | Média | Médio | Spec aceita por ora (mapeamento síncrono); BE-22 vai instrumentar timer. Timeout do `RestClient` configurado (já no Builder do BE-18). |
| Concorrência de mensagens do mesmo `wa_id` causa lock no banco | Baixa | Baixo | Unique key em `mensagem_processada(canal, id_externo)` resolve via DuplicateKeyException → caminho "já processado". BE-19a testou. |

## Coordenação

- **Pode rodar em paralelo com:** PREP-WA (manual, do humano na Meta); FIX-idempotencia-porta-application (atualmente em execução pelo back — outra branch, território disjunto exceto pelo `MensagemEntranteService`; coordenar via merge ordering: FIX mergeia primeiro, BE-19 rebaseia se precisar).
- **Depende sequencialmente de:** nada novo em código (todas as deps em `develop`).
- **Bloqueia:** BE-20 (deploy + smoke real), BE-21b (`WhatsAppNotificadorImpl` pra EVO-02 — esse depende do canal já estar funcionando entrada+saída).
- **Atenção pro Reviewer (insumo pro checklist arquitetural — item #8 do backlog):**
  - Direção de dependência: `adapters/in/whatsapp/` importa de `application/` ✅, mas **não** o contrário.
  - Adapter sabe da porta (`MensagemEntrantePortIn`) e do DTO; **não** importa strategy nem usecase.
  - Sender de saída (`WhatsAppMessageSenderService`) consumido pelo exception handler — ok (adapter de entrada usando adapter de saída é padrão pro feedback).
  - **Sempre 200** em todos os caminhos do POST. Reviewer roda os testes pra confirmar.
  - Validação de assinatura ANTES de desserializar (não depois).
- **Após merge:** atualizar `docs/STATE.md` com "BE-19 mergeado; próximo é BE-20 (deploy + smoke real com Test number) + paralelo PREP-WA precisa estar em fase 7+".

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, e **revisão obrigatória do Reviewer** (segurança-sensível: HMAC + sempre-200 + handshake). Abrir PR pra `develop`; **não mergear sozinho**.

## Referências

- Spec: `docs/architecture/adapter-whatsapp-cloud-api.md` §3 (estrutura de pacotes), §4 (adapter entrada — handshake, signature, fluxo POST, envelope, idempotência, caminho de falha), §8 (config/security).
- ADR 0003 (controller webhook sempre 200) · ADR 0013 (multi-canal coexiste) · ADR 0014 (eventos in-process — contexto pra EVO-02 que vem depois).
- Padrão referencial: `adapters/in/telegram/controller/TelegramWebhookController.java` + `adapters/in/telegram/mapper/TelegramMessageMapper.java` + `adapters/in/telegram/exceptionhandler/GlobalTelegramExceptionHandler.java`.
- BE-17 (porta agnóstica) · BE-18 (sender + downloader) · BE-19a (idempotência) · BE-21a (eventos + canalPreferido) — todos em develop, contexto consolidado.
- `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md` — restrição BR só afeta envio business-initiated; recebimento e resposta na janela 24h funcionam mesmo com Business não-verificada. BE-19 não é bloqueado por isso.
- `docs/runbooks/RUNBOOK-prep-wa-meta-cloud-api.md` — preparação manual da Meta; BE-19 deve estar deployado antes da fase 8 (configurar webhook).
- Próxima task após esta: **BE-20** (deploy + configurar webhook na Meta + smoke real com Test number).
