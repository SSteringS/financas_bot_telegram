# FIX-001 — `@Value` defaults defensivos pros secrets WhatsApp (deploy-safe sem chip)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** identificado pós-merge da BE-19 (PR #76) em conversa com o humano (2026-05-29). A BE-19 entregou o adapter de entrada completo, mas as `@Value` dos 3 novos secrets (`whatsapp.verify-token`, `whatsapp.app-secret`, `whatsapp.allowed-wa-ids`) **não têm default** — a app falha no boot em prod se algum não estiver populado na AWS. Como BE-20 (smoke real com Meta) foi parqueada esperando chip + Business Verification (não-comprável imediatamente), precisamos da app rodando em prod **com os endpoints WhatsApp inertes** até o chip chegar.
> - **Prioridade:** **alta**. Bloqueia o deploy de BE-19 (e tudo que sair de develop pra `main` daqui pra frente) até resolver — porque o boot vai quebrar se algum dos 3 secrets faltar.
> - **Esforço:** **mínimo** (~15-20 min). Mudar 3 anotações + ajustar 1 teste se necessário + atualizar properties pra documentar os sentinelas.
> - **Território / quem executa:** `financas_bot_telegram/src/main/java/.../adapters/in/whatsapp/` (controller + signature validator) + `src/main/resources/application*.properties` → **Claude do back**.
> - **Branch:** `fix/001-whatsapp-defaults-deploy-safe`, a partir de `develop`. **Primeiro FIX no padrão novo (3 dígitos zero-padded — CLAUDE.md 2026-05-29).**
> - **Dependências:** **BE-19 em `develop`** ✅ (o código dos campos `@Value` existe; este FIX só adiciona defaults).
> - **Riscos:** mínimos. Mudança de uma anotação não altera comportamento em runtime quando o secret existe (default é fallback). Sentinela **inutiliza o WhatsApp**: handshake nunca casa, signature nunca valida. **Mas isso é o resultado desejado** — sem chip + sem Meta configurada, o adapter precisa estar inerte.

---

## Contexto

Pós-BE-19 a `WhatsAppWebhookController` injeta:

```java
@Value("${whatsapp.verify-token}") String verifyToken
@Value("${whatsapp.app-secret}") String appSecret
@Value("${whatsapp.allowed-wa-ids}") List<String> allowedWaIds
```

Em `application-prod.properties`:

```properties
whatsapp.verify-token=${whatsapp_verify_token}
whatsapp.app-secret=${whatsapp_app_secret}
whatsapp.allowed-wa-ids=${whatsapp_allowed_wa_ids}
```

Onde `${whatsapp_*}` são lidos via Spring Cloud AWS do secret `finbot-prod-secrets`. **Se qualquer um dos 3 não existir no secret, Spring Cloud AWS falha resolvendo a placeholder e o app não sobe.**

Estado atual no Secrets Manager (a confirmar pelo humano):
- `whatsapp_phone_number_id` ✅ (BE-18 depende)
- `whatsapp_access_token` ✅ (BE-18 depende)
- `whatsapp_verify_token` ❓ (BE-19 introduziu)
- `whatsapp_app_secret` ❓ (BE-19 introduziu)
- `whatsapp_allowed_wa_ids` ❓ (BE-19 introduziu)

PREP-WA fase 7 do runbook indica popular os 3 novos quando o chip chegar. Mas até lá, app boot quebra.

## Decisão / abordagem

**Defaults nas `@Value` do código — `${prop:sentinela}`.** Código se auto-defende; não depende de coordenação com console AWS.

```java
// Controller
@Value("${whatsapp.verify-token:NAO_CONFIGURADO}") String verifyToken
@Value("${whatsapp.allowed-wa-ids:}") List<String> allowedWaIds   // lista vazia = ninguém autorizado

// MetaSignatureValidator
@Value("${whatsapp.app-secret:NAO_CONFIGURADO}") String appSecret
```

Comportamento esperado **com os defaults ativos** (secrets ausentes em prod):
- `GET /webhook/whatsapp?hub.verify_token=X` — `X` nunca casa com `NAO_CONFIGURADO` → sempre **403**.
- `POST /webhook/whatsapp` — validador HMAC chama `hmacSha256Hex("NAO_CONFIGURADO", body)` que nunca casa com o `X-Hub-Signature-256` real da Meta → sempre **200 silencioso com log WARN**.
- Allow-list vazia → qualquer `wa_id` cai em `UnauthorizedUserException`, mas como o POST já é silenciosamente descartado pela signature inválida, nem chega aí.
- **Resultado:** endpoints existem em prod, respondem com 403/200, **inertes**.

Comportamento **com secrets reais populados** (quando BE-20 entrar): defaults nunca disparam (property resolve), funciona normal.

**Por que `NAO_CONFIGURADO` e não string vazia:** vazia poderia casar com mode/token vazio em algum edge case de envelope mal-formado da Meta; sentinela explícita garante que **nunca** vai casar. Também aparece em log se alguém testar.

## Escopo / arquivos

### Modificar

- `adapters/in/whatsapp/controller/WhatsAppWebhookController.java` — adicionar default `:NAO_CONFIGURADO` no `@Value` do `verify-token` e `:` (vazio = lista vazia) no `allowed-wa-ids`.
- `adapters/in/whatsapp/security/MetaSignatureValidator.java` — adicionar default `:NAO_CONFIGURADO` no `@Value` do `app-secret`.
- `src/main/resources/application.properties` — comentário documentando o comportamento dos sentinelas (próximo dev não estranha):
  ```
  # whatsapp.verify-token / app-secret / allowed-wa-ids — quando ausentes, code defaults pra
  # sentinelas que inutilizam o adapter (handshake 403, signature inválida silenciosa).
  # Populer com valores reais via Secrets Manager apenas quando for ativar o canal (BE-20).
  ```
- `src/main/resources/application-prod.properties` — **não muda** (continua referenciando `${whatsapp_*}`); o default do código cobre o caso de secret ausente.
- Testes do `WhatsAppWebhookControllerTest` e `MetaSignatureValidatorTest` — se algum teste depende do `verifyToken`/`appSecret` ser obrigatório, ajustar (provavelmente já passam com qualquer valor não-nulo).

### Não tocar

- Endpoint, lógica de mapper, exception handler, allow-list runtime — só defaults declarativos mudam.
- Properties de dev/example — já têm valores explícitos (`CHANGE_ME`), default não dispara em dev.
- BE-18 (sender + downloader) — `whatsapp.access-token` e `whatsapp.phone-number-id` **continuam obrigatórios** (já estão populados na AWS; senão BE-18 nunca teria deployado). Não adicionar default — fail-fast em ausência é o certo pra esses (canal de saída ativo desde BE-18, ao contrário de entrada que precisa de chip).

## Critérios de aceitação

- [ ] App sobe localmente com profile `prod` e **apenas** `whatsapp_phone_number_id` + `whatsapp_access_token` populados (os outros 3 secrets ausentes) — boot normal, log WARN opcional avisando sentinela ativo.
- [ ] `curl GET https://localhost/webhook/whatsapp?hub.mode=subscribe&hub.verify_token=qualquercoisa&hub.challenge=foo` → **403**.
- [ ] `curl POST https://localhost/webhook/whatsapp` com qualquer payload + qualquer `X-Hub-Signature-256` → **200 + log WARN** "assinatura inválida".
- [ ] Quando o secret `whatsapp_verify_token` for populado com valor real, comportamento volta ao normal (handshake casa, 200 com challenge).
- [ ] `mvn test` verde — sem regressão. Testes existentes do BE-19 passam.
- [ ] `mvn package -DskipTests` ok.
- [ ] Branch `fix/001-whatsapp-defaults-deploy-safe` saiu de `develop` (fluxo novo: `git fetch && git checkout -b fix/001-whatsapp-defaults-deploy-safe develop`).
- [ ] Território só `financas_bot_telegram/`.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/FIX-001-whatsapp-defaults-deploy-safe.md` com frontmatter válido + smoke local documentado (app sobe sem os 3 secrets).

## Fora de escopo (explicitamente)

- **Populer os secrets reais na AWS** — fica pra BE-20 (parqueada).
- **Tornar o `whatsapp.access-token` / `whatsapp.phone-number-id` também defaultados** — não precisa, já estão populados há sprints; fail-fast em ausência é correto.
- **Health check exposing the sentinel status** — opcional, fica pra Actuator do BE-22 (`/actuator/info` poderia mostrar "whatsapp: inerte"). Não bloqueia.
- **Validação de configuração** (tipo um `ConfigurationProperties` validator que avisa em log se sentinela está ativo) — nice-to-have, baixa prioridade.

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Secret real fica esquecido como sentinela quando BE-20 entrar | Baixa | Médio (canal não funciona em prod) | BE-20 inclui checklist "popular os 3 secrets reais e confirmar handshake 200". Log INFO no boot mostrando que sentinela ativo facilitaria detecção. |
| HMAC com chave "NAO_CONFIGURADO" gera signature legítima por acidente (impossível mas em tese) | ~zero | Alto | O atacante precisaria saber o sentinela exato e o app-secret esperado da Meta. Sentinela é hardcoded literal — sem coincidência possível. |
| Teste de unit espera exception em config ausente | Baixa | Baixo | Ajustar para o novo comportamento (default sentinela). |
| Default vazio em `allowed-wa-ids` é parseado como lista com string vazia (`[""]`) em vez de lista vazia | Média | Baixo (apenas mais uma rota de negação) | Conferir parsing do Spring. Se for o caso, normalizar no constructor: `allowedWaIds.removeIf(String::isBlank)`. |

## Coordenação

- **Despachar imediatamente** após FIX-idempotencia mergear (já mergeada ✅). Não há dep nova.
- **Pode rodar em paralelo com BE-17b / FE-14 / BE-22** (BE-22 também já em flight).
- **Atenção pro Reviewer:** verificar que o sentinela é uma string que **nunca** poderia ser legítima como verify-token / app-secret real. Confirmar que app boot funciona sem os 3 secrets.
- **Após merge + deploy:** confirmar via `curl` em prod que `/webhook/whatsapp` responde 403 (handshake) e 200 (POST). Anotar como pendência pro futuro BE-20: "popular os 3 secrets reais ANTES de configurar webhook na Meta".

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido com **smoke local documentado**, e **revisão do Reviewer** (mudança simples mas faz parte de superfície de segurança). Abrir PR pra `develop`; **não mergear sozinho**.

## Referências

- BE-19 (`docs/sprints/02-canal-whatsapp/plans/BE-19-adapter-entrada-whatsapp.md`) — task original; este FIX é follow-up.
- BE-20 (`docs/sprints/02-canal-whatsapp/plans/BE-20-deploy-whatsapp-smoke.md`) — task parqueada que vai despoluir os sentinelas com secrets reais.
- Spec: `docs/architecture/adapter-whatsapp-cloud-api.md` §4, §8 (config WhatsApp).
- Aprendizado: `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md` (motivo do parqueamento).
- CLAUDE.md §"Fluxo de branches" (convenção FIX-NNN 3 dígitos a partir de 2026-05-29 — **este é o primeiro FIX nesse padrão**).
- `docs/plans/BACKLOG-evolucao-workflow.md` item #9 (adoção parcial da numeração zero-padded).
