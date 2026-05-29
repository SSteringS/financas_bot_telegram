# Spec técnica — Adapter de entrada/saída WhatsApp (Cloud API oficial)

> **Status:** esboço de arquitetura (Arquiteto, 2026-05-27). Pressupõe o **ADR 0012** homologado (provider = Cloud API oficial da Meta). É o "como" técnico que os planos de task (EVO-01/EVO-02) vão referenciar — **não é código**, e o planner quebra em tasks. Onde discordar, me corrige.

Referências de base: `docs/architecture/estado-atual.md` §3/§5/§6 · `docs/decisions/0003-controller-webhook-nunca-retorna-5xx.md` · `docs/decisions/0012-provider-whatsapp-cloud-api-oficial.md` · `docs/aprendizado/whatsapp-modelo-mensagens.md`.

---

## 1. Objetivo e escopo

Adicionar o **canal WhatsApp** ao bot, reaproveitando o núcleo de aplicação (usecases) sem alterá-lo. A hexagonal já isola o canal de entrada num adapter (`adapters/in/telegram`); este documento descreve o adapter equivalente pra WhatsApp Cloud API e a generalização da porta de entrada pra ficar **agnóstica de canal**.

**No escopo:** adapter de entrada (webhook), adapter de saída (envio + download de mídia), porta de entrada agnóstica, mapeamento pros usecases atuais.
**Fora do escopo (decisão de produto/PO):** migrar (desligar Telegram) vs. adicionar (rodar os dois). A arquitetura suporta ambos; ver §7.

---

## 2. Conceitos da Cloud API que moldam o design

Resumo operacional (detalhe conceitual em `aprendizado/whatsapp-modelo-mensagens.md`):

- **Entrada = webhook único** com **dois verbos**: `GET` pra *verificação* (handshake na configuração) e `POST` pra *receber eventos* (mensagens e status).
- **`POST` vem assinado**: header `X-Hub-Signature-256: sha256=<hmac>`, HMAC-SHA256 do corpo **bruto** usando o **App Secret**. Validar antes de processar.
- **Mídia em 2 passos**: a mensagem traz só um `media_id`; baixa-se em (1) `GET /{media_id}` → retorna URL temporária; (2) `GET <url>` com `Authorization: Bearer <token>` → binário.
- **Saída = Graph API**: `POST /{phone_number_id}/messages` com JSON.
- **Janela de 24h / templates**: responder dentro da janela (usuário falou) é mensagem de **serviço**, livre. Iniciar conversa fora da janela exige **template aprovado** — relevante só pra EVO-02 (§6).

---

## 3. Encaixe na arquitetura hexagonal

Estrutura-alvo de pacotes (espelha o Telegram em `estado-atual.md` §3). Raiz: `br.com.satyan.stering.saita.financasbottelegram`.

```
adapters/
├── in/
│   ├── telegram/                      ← REFATORADO (orchestrator + strategies SAEM daqui)
│   │   ├── controller/
│   │   │   └── TelegramWebhookController.java
│   │   ├── mapper/
│   │   │   └── TelegramMessageMapper.java          ← NOVO (Update do Telegram → PaymentMessageDTO)
│   │   ├── exception/
│   │   └── exceptionhandler/
│   │       └── GlobalTelegramExceptionHandler.java
│   └── whatsapp/                      ← NOVO
│       ├── controller/
│       │   └── WhatsAppWebhookController.java     (GET verify + POST eventos; SEMPRE 200 — ADR 0003)
│       ├── security/
│       │   └── MetaSignatureValidator.java        (valida X-Hub-Signature-256 sobre o corpo bruto)
│       ├── dto/                                    (envelope da Meta: WhatsAppWebhookPayload, etc.)
│       ├── mapper/
│       │   └── WhatsAppMessageMapper.java          (envelope Meta → PaymentMessageDTO)
│       ├── exception/
│       └── exceptionhandler/
│           └── GlobalWhatsAppExceptionHandler.java (handler de último recurso → 200)
└── out/
    └── whatsapp/                      ← NOVO
        └── service/
            ├── WhatsAppMessageSenderService.java       (POST /{phone_id}/messages)
            ├── WhatsAppMediaDownloaderService.java     (media_id → URL → binário)
            └── WhatsAppApiException.java

application/
├── port/
│   ├── in/
│   │   └── MensagemEntrantePortIn.java   ← RENOMEAR de TelegramPortIn (agnóstico de canal)
│   └── out/
│       ├── NotificadorPortOut.java        ← NOVO (abstrai "enviar mensagem ao usuário", p/ EVO-02)
│       └── ...                            (repos inalterados)
├── services/
│   ├── MensagemEntranteService.java       ← NOVO (impl de MensagemEntrantePortIn; orquestra dispatch)
│   └── ...                                (usecases services inalterados)
├── strategy/                              ← MOVIDO de adapters/in/telegram/strategy/
│   ├── MensagemProcessingStrategy.java         (interface, ex-UpdateProcessingStrategy)
│   ├── PaymentRequestStrategy.java             (regex "150.00 Almoço" + SalvarPedidoPagamentoUsecase)
│   └── PaymentProofStrategy.java               (regex "#123 PIX" + RegistrarComprovanteUsecase)
├── event/                                 ← NOVO (ADR 0014 — eventos in-process)
│   └── NotificacaoComprovanteListener.java     (@TransactionalEventListener AFTER_COMMIT + @Async)
└── usecases/                              (INALTERADO)

domain/
└── event/                                 ← NOVO (ADR 0014)
    └── ComprovanteRegistradoEvent.java         (POJO/record canal-agnóstico)
```

**Por que strategies saem dos adapters:** elas decidem entre "pedido" e "comprovante" pelo **formato da legenda**, o que é **classificação de negócio**, não de canal. Hoje vivem em `adapters/in/telegram/strategy/` por acidente histórico (eram acopladas ao tipo `Update` do Telegram). Pós-refactor, passam a depender de `PaymentMessageDTO` (canal-agnóstico) e vivem em `application/strategy/`, **compartilhadas** por todos os adapters de entrada. Se ficassem no adapter, o WhatsApp teria que importar de `adapters/in/telegram/strategy/` (acoplamento errado) ou duplicar (defeito que a porta agnóstica veio resolver).

**Por que o orchestrator também sai:** o orchestrator atual (`UpdateOrchestratorService` em `adapters/in/telegram/service/`) escolhe a strategy a partir do `Update` do Telegram. Pós-refactor, ele recebe `PaymentMessageDTO` e despacha — também canal-agnóstico. Vira `MensagemEntranteService` em `application/services/`, que **implementa** `MensagemEntrantePortIn`.

**Cadeia de chamada pós-refactor:** controller do canal X → mapper do canal X (envelope X → `PaymentMessageDTO`) → `MensagemEntrantePortIn.processar(dto)` → `MensagemEntranteService` escolhe strategy → strategy parseia + chama usecase. O adapter conhece só o canal e a porta; o miolo de dispatch e regra é compartilhado.

---

## 4. Adapter de ENTRADA — webhook

### 4.0 Convenção de rotas (refatoração do Telegram)

Hoje o Telegram está em `POST /webhook` (raiz, sem qualificador — `estado-atual.md` §5). Adicionar o WhatsApp na raiz colidiria, e aninhar só o WhatsApp (`/webhook/whatsapp`) deixaria o roteamento **assimétrico**. Convenção adotada: **namespace por canal nos dois**.

| Antes | Depois |
|---|---|
| `POST /webhook` (Telegram) | `POST /webhook/telegram` |
| — | `POST /webhook/whatsapp` (novo) + `GET /webhook/whatsapp` (handshake) |

Casa com a filosofia da migração (porta e strategies agnósticas de canal): cada provider tem envelope e validação próprios, então cada um tem rota explícita.

**Refatoração do Telegram (não é rename inócuo):** o Telegram entrega no URL registrado via `setWebhook`. Mudar o `@PostMapping` **e** re-registrar precisam acontecer na **mesma janela de deploy**:

1. Subir o código com a rota `/webhook/telegram`.
2. Chamar `setWebhook` apontando pro novo URL.

Fazer só um dos passos para a entrega do Telegram até o outro acontecer. É barato e reversível, mas é **passo operacional sequenciado** e mexe no **código do adapter Telegram** (território do implementador back), não só no adapter novo. Vira uma task própria (ver §10), idealmente **antes** ou junto da entrada do WhatsApp.

### 4.1 `GET /webhook/whatsapp` — verificação (handshake)

Chamado uma vez pela Meta ao configurar o webhook. Recebe query params `hub.mode`, `hub.verify_token`, `hub.challenge`. Se `hub.mode=subscribe` e `hub.verify_token` == token configurado (em Secrets Manager), responde **200 com o `hub.challenge` puro** (texto). Caso contrário, **403**. (Este endpoint **não** segue a regra "sempre 200" do ADR 0003 — ele é handshake de config, não recebimento de evento.)

### 4.2 `POST /webhook/whatsapp` — recebimento de eventos

Fluxo:

1. **Validar assinatura** (`MetaSignatureValidator`): HMAC-SHA256 do **corpo bruto** com o App Secret, comparar com `X-Hub-Signature-256`. Inválida → loga `WARN` e retorna **200** (não 4xx — não queremos retry da Meta nem expor diferença de comportamento; é descarte silencioso de payload não-autêntico). *Nota de implementação:* precisa do corpo **raw** antes da desserialização — usar `ContentCachingRequestFilter` ou ler o `byte[]` no controller.
2. **Desserializar** o envelope (`entry[].changes[].value.messages[]` / `.statuses[]`).
3. **Mapear + despachar**: para cada item em `messages[]`, o `WhatsAppMessageMapper` transforma o envelope da Meta em `PaymentMessageDTO` (DTO canal-agnóstico) e chama `MensagemEntrantePortIn.processar(dto)`. A implementação em `application/` (`MensagemEntranteService`) é que escolhe a strategy e chama o usecase — o adapter **não** conhece strategy nem usecase (ver §3). `statuses` (entregue/lido/falhou) → log/observability (útil pra EVO-02; sem ação de negócio no MVP).
4. **SEMPRE retornar 200** (ADR 0003): a Meta, como o Telegram, **retenta em não-2xx** e pode travar a fila. Toda exceção não-mapeada cai no `GlobalWhatsAppExceptionHandler` → log `ERROR` + (best-effort) mensagem amigável ao usuário via sender → 200.

### 4.3 Idempotência

A Meta entrega eventos **at-least-once**: o mesmo `wamid` (id da mensagem) pode chegar mais de uma vez — por redelivery espontâneo, por não ter recebido o 200 a tempo, ou por duas entregas concorrentes. Sem dedup, isso registra o **mesmo pedido/comprovante duas vezes**.

**Chave de idempotência = `wamid`** (o `id` de cada item em `messages[]`). Não dá pra usar unicidade de negócio: o usuário pode legitimamente mandar "150.00 Almoço" duas vezes, então valor+descrição não distingue duplicata real de reenvio. O `wamid` é o único identificador estável da entrega.

**Por que tabela persistente (e não cache em memória):** cache em memória se perde a cada **restart** (todo deploy reinicia o serviço) e não sobrevive a um redelivery pós-reinício. Precisa ser persistente → MySQL, que o projeto já usa.

**Tabela agnóstica de canal (ADR 0013):** como Telegram e WhatsApp coexistem, a tabela serve os dois. Telegram usa `update_id`, WhatsApp usa `wamid`, pelo mesmo mecanismo. Entra como **migração Flyway** (Flyway é pré-requisito recomendado em `estado-atual.md` §6).

```sql
-- Vx__criar_mensagem_processada.sql
CREATE TABLE mensagem_processada (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    canal VARCHAR(20) NOT NULL,            -- 'TELEGRAM' | 'WHATSAPP'
    id_externo VARCHAR(255) NOT NULL,      -- wamid (WhatsApp) ou update_id (Telegram)
    processado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_canal_idexterno (canal, id_externo)
) ENGINE=InnoDB;
```

**Fluxo — "claim-then-process" na MESMA transação:**

1. Extrair `wamid` da mensagem.
2. Logo no início da transação de processamento, tentar **inserir** a linha de claim:
   `INSERT INTO mensagem_processada (canal, id_externo) VALUES ('WHATSAPP', :wamid)`.
   - Se lançar `DataIntegrityViolationException` (violação da unique key) → **já processado** (ou processando concorrente) → **não processa**, retorna 200 e segue.
   - Se inserir com sucesso → segue pro passo 3.
3. Processar o negócio (registrar pedido/comprovante) — **dentro da mesma transação** da inserção do passo 2.
4. **Commit**: a linha de claim e a escrita de negócio commitam **juntas** (atomicidade).

**Por que mesma transação — a interação com o ADR 0003:** se a gente marcasse "processado" e commitasse *antes* de processar, e o processamento falhasse, o ADR 0003 (sempre 200, sem retry) faria a mensagem sumir **e** ficar marcada como feita — irrecuperável. Na mesma transação, falha no passo 3 → **rollback** remove o claim → se a Meta reentregar, há nova chance. (Mensagens que falham e *não* são reentregues continuam sendo perda conhecida do ADR 0003 — mitigação futura é dead-letter, EVO-09; idempotência não resolve isso, só garante que **sucesso não vira duplicata**.)

**Concorrência:** duas entregas do mesmo `wamid` em paralelo são serializadas pela **unique key** — a primeira `INSERT` vence; a segunda falha com duplicate-key e cai no caminho "já processado". Sem necessidade de lock aplicativo nem de coluna de status ("processando" vs "processado"): a existência da linha, garantida pela transação única, já é o estado.

**Escopo:** dedup vale pros eventos de `messages` (têm efeito de negócio). Eventos de `statuses` (entregue/lido/falhou) não disparam ação no MVP — não precisam de dedup agora (se forem persistidos pra EVO-02 no futuro, aplicar o mesmo padrão).

**Retenção (opcional):** a tabela cresce ~1 linha por mensagem. No volume da família é desprezível (milhares/ano). Limpeza de linhas antigas (ex.: job que apaga > 30–90 dias) é **opcional** e pode ficar pra depois — não é requisito de correção.

**Custo de performance (consciente):** cada mensagem entrante passa a ter **uma consulta/escrita a mais** no banco (o claim de idempotência via SQL, não cache). No volume atual isso é irrelevante e a unique key mantém o lookup O(log n). Mas é dívida a **monitorar** conforme a tabela cresce — por isso a frente de observability deve **medir a latência de cada consulta ao banco** (ver §8.1 e PENDENCIAS-TECNICAS). A decisão é: **SQL resolve pro começo**; reavaliar (cache em memória na frente do SQL, índice dedicado, particionamento, limpeza agressiva) **com base nas métricas**, não por suposição. Registrado como pendência técnica.

### 4.4 Esboço do envelope (entrada)

```jsonc
{
  "object": "whatsapp_business_account",
  "entry": [{
    "id": "<WABA_ID>",
    "changes": [{
      "field": "messages",
      "value": {
        "messaging_product": "whatsapp",
        "metadata": { "phone_number_id": "<PHONE_ID>" },
        "contacts": [{ "wa_id": "<numero_usuario>", "profile": { "name": "..." } }],
        "messages": [{
          "from": "<numero_usuario>",
          "id": "wamid.XXX",                  // idempotência
          "timestamp": "...",
          "type": "image",                    // text | image | document | ...
          "image": { "id": "<media_id>", "mime_type": "image/jpeg", "caption": "150.00 Almoço" }
        }]
      }
    }]
  }]
}
```

O `caption` carrega o texto que hoje as strategies parseiam (regex `^(\d+([.,]\d{1,2})?)\s+(.+)$` pro pedido; `#123 PIX` pro comprovante). A foto vem como `media_id` (não como conteúdo) → baixa via §5.2.

### 4.5 Caminho de falha — feedback obrigatório ao usuário (ADR 0003)

O ADR 0003 manda **sempre retornar 200**, mas isso tem uma contrapartida **obrigatória**: o usuário **precisa ser avisado** de que a mensagem não foi processada — senão ele acha que deu certo e o pedido/comprovante simplesmente some. Retornar 200 é pro Telegram/Meta; avisar o usuário é pra ele saber que precisa reenviar.

Em **falha não-mapeada** durante o processamento (banco fora, S3 fora, etc.):

1. **Rollback da transação** — inclui o claim de idempotência (§4.3). A mensagem **não** fica marcada como processada.
2. **Log `ERROR`** com stack trace + `wamid` (correlação).
3. **Best-effort: avisar o usuário** que **não** foi processado e pedir reenvio — mensagem honesta, nunca fingir sucesso. Ex.: *"⚠️ Não consegui registrar sua mensagem agora. Pode reenviar daqui a pouco, por favor?"*
4. Se o **próprio aviso** falhar (sender fora), loga o erro secundário e **não propaga** (ADR 0003).
5. **Retorna 200.**

**Por que isso fecha (parcialmente) o buraco do ADR 0003:** o ADR 0003 lista como negativa "mensagem perdida sem retry automático". Como a Meta **não reentrega** depois do 200, a recuperação é **manual** — e é exatamente o aviso ao usuário que a viabiliza. O reenvio do usuário gera um **`wamid` novo** → não é barrado pela dedup de idempotência (§4.3) → o retry funciona normalmente. Ou seja, **o feedback ao usuário É o mecanismo de recuperação** pra erro transitório de infra. (Para erro persistente, a mitigação robusta segue sendo o dead-letter / EVO-09.)

**Distinguir os dois tipos de erro na mensagem:**
- **Erro de negócio** (legenda mal-formada, formato inválido) → já tem mensagens didáticas com exemplos (resolvido em `feature/backend-polish-evo07`). Mensagem orienta o **formato correto**.
- **Erro de infra** (banco/S3/exceção inesperada) → é o caso desta seção. Mensagem orienta **"tente de novo"**, não "formato errado" — não confundir o usuário sugerindo que ele errou quando o problema é nosso.

---

## 5. Adapter de SAÍDA — Graph API

### 5.1 Envio de mensagem — `WhatsAppMessageSenderService`

`POST https://graph.facebook.com/{version}/{phone_number_id}/messages`, header `Authorization: Bearer <token>`. Dois modos:

- **Resposta na janela (mensagem de serviço)** — texto livre:
  ```json
  { "messaging_product": "whatsapp", "to": "<wa_id>", "type": "text", "text": { "body": "Pedido #123 registrado ✅" } }
  ```
- **Fora da janela** — só **template aprovado** (`type: "template"`). Relevante pra EVO-02 (§6).

Substitui o `TelegramMessageSenderService`. Mesma responsabilidade, contrato diferente.

### 5.2 Download de mídia — `WhatsAppMediaDownloaderService`

Dois passos (substitui `TelegramFileDownloaderService`):

1. `GET https://graph.facebook.com/{version}/{media_id}` (com Bearer) → JSON com `url` temporária.
2. `GET <url>` (com Bearer) → bytes. **A URL exige o header de auth** e expira rápido — baixar na hora.

A partir daí o fluxo é idêntico ao atual: bytes → `S3ImageUploadService` (inalterado).

---

## 6. Impacto na EVO-02 (notificação automática)

A EVO-02 dispara, quando um comprovante é registrado, uma mensagem ao requisitante (Pedro) com link pro site. **Modelo: eventos in-process (ADR 0014).** A `RegistrarComprovanteUsecase` **publica** um evento de domínio `ComprovanteRegistradoEvent`; um **listener** (`NotificacaoComprovanteListener` em `application/event/`), com `@TransactionalEventListener(phase = AFTER_COMMIT)` e `@Async`, resolve o **canal preferido** do requisitante (ADR 0013) e chama a `NotificadorPortOut` correspondente — via `Map<Canal, NotificadorPortOut>`, com um bean por canal. O usecase não sabe que existe notificação; o listener não sabe que existe usecase; cada adapter de saída só implementa a porta.

```
RegistrarComprovanteUsecase
  └── salvar(comprovante)                              [transação]
  └── eventPublisher.publish(ComprovanteRegistradoEvent)

[AFTER_COMMIT — assíncrono]
NotificacaoComprovanteListener.onComprovanteRegistrado(ev)
  ├── canal = requisitante.canalPreferido
  └── notificadores.get(canal).notificar(...)
```

Pontos críticos do desenho (detalhe em ADR 0014):

- **`AFTER_COMMIT` não-negociável** — evento dentro da transação dispararia o listener antes do commit; rollback geraria "notificado mas não registrado".
- **`@Async` desacopla latência** — registro do comprovante não espera Graph API; impacta diretamente a métrica fim-a-fim por tipo (§8.1).
- **Tratar exceção dentro do listener** — com `@Async`, exceções não propagam; sem tratamento, viram silêncio. Best-effort + log + métrica de falha (§8.1).
- **Sem durabilidade:** evento perdido em crash do JVM = notificação perdida. Aceito porque o front da Sprint 01 é fonte da verdade pro requisitante; evolução natural pra outbox se virar requisito (ADR 0014 §"Caminho de evolução").

O ponto da Meta (independente do mecanismo de eventos): **a notificação é iniciada pelo sistema**, não é resposta a uma mensagem do Pedro. Se o Pedro **não** falou com o bot nas últimas 24h, a mensagem cai **fora da janela** → precisa ser um **template de utilidade pré-aprovado** pela Meta (ex.: "Seu pagamento referente a {{1}} foi registrado. Detalhes: {{2}}"). Implicações pro planner:

- Task de **criação + submissão do template** (categoria **utilidade**, não marketing) com prazo de aprovação da Meta.
- O `NotificadorPortOut` precisa suportar **envio por template** (parâmetros), não só texto livre.
- Heurística opcional: se houver janela aberta, manda texto livre (grátis); senão, template. Começar **sempre com template** é mais simples e robusto.
- **Restrição vale só pro WhatsApp:** a janela/template acima é especificidade da Meta. Quando o canal escolhido pelo listener for Telegram, a `NotificadorPortOut` correspondente envia texto livre normal — sem janela, sem template. (Roteamento por canal preferido já descrito no início desta seção, via `Map<Canal, NotificadorPortOut>`.)

---

## 7. Estratégia multi-canal — DECIDIDO (ADR 0013)

**Decisão tomada (ADR 0013):** o **Telegram permanece** e o WhatsApp é **adicionado** (não é migração). Canal = adapter: cada novo canal entra como par de adapters sobre a porta agnóstica + usecases compartilhados. O **Discord** fica como **ideia de evolução futura** (sem compromisso).

Implicações que esta spec assume a partir daqui:

- **Conviver com ≥2 adapters de entrada** indefinidamente (dois webhooks/senders/validações). O refactor da porta agnóstica (`MensagemEntrantePortIn`) deixa de ser andaime de transição e vira **infra permanente**.
- **Roteamento de notificação (EVO-02):** com >1 canal, "avisar o Pedro" precisa saber **em qual canal**. Caminho provável: **canal preferido por requisitante** + fallback (ver §6).
- **Autorização por canal:** a allow-list do Telegram ganha equivalente por canal (`whatsapp.allowed-wa-ids`); considerar fonte única de identidade do requisitante mapeando IDs de cada canal.
- **Discord (futuro):** não é webhook-puro — bots usam tipicamente **Gateway (WebSocket)** persistente (ex.: JDA) ou **Interactions endpoint** HTTP. A porta agnóstica continua valendo, mas o **transporte de entrada difere** do padrão webhook-POST; o adapter Discord terá forma própria. Avaliar quando virar prioridade.

---

## 8. Configuração / segurança (novos itens)

Seguindo o padrão de profiles de `estado-atual.md` §6 (placeholders no default, Secrets Manager em prod):

| Config | Origem | Notas |
|---|---|---|
| `whatsapp.verify-token` | Secrets Manager (prod) | token do handshake `GET` |
| `whatsapp.app-secret` | Secrets Manager | valida `X-Hub-Signature-256` |
| `whatsapp.access-token` | Secrets Manager | Bearer da Graph API (atenção à validade/rotação) |
| `whatsapp.phone-number-id` | properties/secret | id do número na Cloud API |
| `whatsapp.allowed-wa-ids` | properties (ou banco) | equivalente ao `telegram.allowed-user-ids` (autorização) |

### 8.1 Observabilidade — requisito de métricas (insumo pra frente de observability)

A frente de observability da sprint (logs + alarmes) deve, **além de logs**, instrumentar **latência**. Requisito derivado desta spec (motivado pela query extra de idempotência e pelas chamadas externas do adapter):

- **Tempo de cada consulta ao banco** — incluindo o claim de idempotência (§4.3) e as escritas de pedido/comprovante. Permite ver a query de dedup degradar conforme `mensagem_processada` cresce, e decidir o refactor **por dado** (ver pendência).
- **Tempo de chamadas externas** — Graph API (envio e os 2 passos de download de mídia, §5) e S3 (upload). São os pontos de falha/lentidão fora do nosso controle; medir latência e taxa de erro é o que dá visibilidade pro caminho de falha (§4.5).
- **Tempo de processamento da mensagem entrante (fim-a-fim), por tipo** — do recebimento no webhook até a mensagem estar processada (pedido/comprovante gravado), **segmentado por tipo: `PEDIDO` vs `COMPROVANTE`**. É a métrica de experiência real do usuário. **Os dois tipos carregam mídia** (foto/PDF da conta no pedido; foto/PDF do comprovante de pagamento no comprovante) → ambos fazem parse + download via Graph API (2 passos) + upload no S3 + escrita no banco, então o custo bruto é parecido. O que muda entre os caminhos: strategies e usecases distintos, regex de parse diferente, e o **comprovante tem etapa adicional de lookup do pedido referenciado** (e validação de existência/estado). Segmentar é útil porque (a) deixa visível se uma regressão atinge um tipo mais que o outro, (b) modos de falha são diferentes (comprovante pode falhar com "pedido não encontrado" — caminho que pedido não tem), e (c) permite cruzar com canal/resultado pra perguntas como "comprovantes via WhatsApp estão lentos?" ou "qual tipo falha mais?". Dimensões sugeridas do timer: `tipo` (pedido/comprovante), `canal` (telegram/whatsapp), `resultado` (sucesso/falha). Ponto de medição: em volta da cadeia orquestrador → strategy → usecase (não inclui o tempo de rede da Meta até nós, que não controlamos).

Forma sugerida (não-vinculante, decisão do planner/implementador): **Micrometer** (já idiomático em Spring Boot) expondo timers, com export pro **CloudWatch** (a frente de observability já aponta pra lá; IAM `CloudWatchAgentServerPolicy` já está na EC2). Timers por consulta podem sair de instrumentação manual em pontos-chave ou de um aspecto/interceptor — calibrar pra não virar ruído. Isto é **requisito**, não plano; o planner incorpora na task de observability.

Pendências de infra/segurança a mapear:

- **Cert HTTPS válido** no webhook — hoje é **auto-assinado** na 8443 (`estado-atual.md` §6); a Meta exige cert confiável. Provável task de infra (cert real + domínio / proxy) **antes** de plugar o webhook.
- **Rotação do access token** da Graph API (tokens de sistema vs. de usuário) — definir estratégia; não deixar token de curta validade em prod.
- **Autorização de remetente**: replicar a allow-list que o Telegram já faz, agora por `wa_id`.

---

## 9. Riscos e dependências (pro planner)

- **Número dedicado** (não pode ser o mesmo logado no app comum) — bloqueia o início.
- **Verificação Meta Business** — processo externo, começar cedo.
- **Cert HTTPS válido** — provável task de infra antecedente.
- **Template de utilidade aprovado** — dependência da EVO-02.
- **Eventos in-process (ADR 0014):** configurar executor de `@Async` com pool bounded (ex.: 2-4 threads); revisão obrigatória de tratamento de exceção dentro do listener (não propaga via `@Async`); métrica de falha do listener (§8.1); testes da EVO-02 exigem transação real (`@TransactionalEventListener(AFTER_COMMIT)` não dispara com tx mockada).
- **Idempotência por `wamid`** — necessária (entrega at-least-once); tabela `mensagem_processada` agnóstica de canal via migração Flyway, claim-then-process na mesma transação (§4.3). Depende de adotar Flyway.
- **Refactor da porta de entrada** pra agnóstica (`TelegramPortIn` → `MensagemEntrantePortIn`) e das strategies pra dependerem do `PaymentMessageDTO`.
- **Rota do Telegram** `/webhook` → `/webhook/telegram` (§4.0): mexe no código do adapter Telegram + exige `setWebhook` coordenado no deploy.
- **Observability primeiro** (ordem da sprint) — rede de segurança pra debugar a migração.

---

## 10. Sequência sugerida de tasks (insumo pro planner quebrar)

1. Infra: cert HTTPS válido + número dedicado + verificação Meta Business.
2. Refactor: porta de entrada agnóstica + strategies sobre `PaymentMessageDTO` + **rota do Telegram `/webhook` → `/webhook/telegram`** (código + `setWebhook` na mesma janela — §4.0).
3. Adapter de saída (`WhatsAppMessageSenderService`, `WhatsAppMediaDownloaderService`).
4. Adapter de entrada (`WhatsAppWebhookController` GET+POST, `MetaSignatureValidator`, mapper, orchestrator) + idempotência.
5. Fim-a-fim do canal (EVO-01): registrar pedido/comprovante via WhatsApp.
6. EVO-02: template de utilidade aprovado + `NotificadorPortOut` por template + **publicação do `ComprovanteRegistradoEvent`** no usecase + **`NotificacaoComprovanteListener`** (AFTER_COMMIT + @Async) com roteamento por canal preferido — ADR 0014.
7. (PO) Desligar Telegram, se a decisão for migrar.
