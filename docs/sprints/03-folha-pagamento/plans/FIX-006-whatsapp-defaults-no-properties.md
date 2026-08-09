---
task: FIX-006
titulo: "Defaults dos placeholders WhatsApp no application-prod.properties (boot em prod sem os secrets)"
sprint: 03-folha-pagamento
data_planejamento: 2026-08-08
branch_alvo: fix/006-whatsapp-defaults-no-properties
prioridade: alta
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: []
skills_dispatched: []
integration_branch: null                   # FIX sai direto de develop
fluxos_qa: []
---

# FIX-006 — Defaults dos placeholders WhatsApp no `application-prod.properties`

## Intake

- **Origem:** falha de boot no deploy em prod (EC2), reportada pelo humano em 2026-08-08:
  ```
  Could not resolve placeholder 'whatsapp_app_secret' in value "${whatsapp_app_secret}"
    <-- "${whatsapp.app-secret:NAO_CONFIGURADO}"
  ```
  É **falha de premissa do FIX-001**, não regressão nova.
- **Por quê agora:** bloqueia todo deploy `develop → main`. Nenhuma entrega da sprint 02 ou 03 chega em prod até resolver.
- **Esforço:** baixo (~30 min). 5 linhas de properties + 1 teste de regressão.
- **Riscos resumidos:** mínimos. Mudança declarativa; o canal WhatsApp permanece inerte, que é o resultado desejado (humano **não** quer ativar o WhatsApp ainda).

---

## Contexto

### Por que o default do `@Value` não funcionou

O FIX-001 (`docs/sprints/02-canal-whatsapp/plans/FIX-001-whatsapp-defaults-deploy-safe.md`) adicionou defaults nas anotações:

```java
// MetaSignatureValidator:23
@Value("${whatsapp.app-secret:NAO_CONFIGURADO}") String appSecret
```

E declarou explicitamente (§Escopo, linha 79):

> `application-prod.properties` — **não muda** (continua referenciando `${whatsapp_*}`); o default do código cobre o caso de secret ausente.

**Essa premissa é falsa.** O default de um placeholder Spring só dispara quando a chave **não existe em nenhum PropertySource**. Em prod ela existe — `application-prod.properties:34` define `whatsapp.app-secret=${whatsapp_app_secret}`. Então:

1. Spring resolve `whatsapp.app-secret` → **acha**, com valor literal `${whatsapp_app_secret}`. O default `:NAO_CONFIGURADO` é descartado aqui.
2. Spring então resolve o **placeholder aninhado** `${whatsapp_app_secret}` contra o PropertySource do Secrets Manager.
3. A chave não está em `finbot-prod-secrets` → sem default → `IllegalArgumentException`, boot morre.

A seta `<--` na mensagem de erro é literalmente essa cadeia de resolução.

**Regra geral:** o default tem que estar no nível onde o placeholder **não resolve**, não no nível de cima.

### Por que as 5 properties, e não só as 3

`STATE.md:96` registra: *"PR `develop → main` (deploy sprint 02): não aberto ainda."* O status report do FIX-001 (linha 60) confirma: *"Este FIX deve mergear antes de BE-19 chegar em `main`"*.

**Conclusão (verificada por leitura de STATE.md + status report do FIX-001):** este deploy é a **primeira vez** que qualquer property `whatsapp.*` chega em prod. Logo o argumento do FIX-001 de que `whatsapp.access-token`/`whatsapp.phone-number-id` "já estão populados desde a BE-18" **não se sustenta** — a BE-18 nunca deployou.

`WhatsAppMessageSenderService:32-33` e `WhatsAppMediaDownloaderService:26` injetam essas duas **sem default em lugar nenhum**. E `RUNBOOK-prep-wa-meta-cloud-api.md` Fase 7 grava as 4 chaves num único passo, dependente da Fase 6 — que está atrás do bloqueio de chip + Business Verification (`STATE.md:97`).

**Inferência (não verificada — requer inspeção do secret):** se `whatsapp_app_secret` falta, `whatsapp_access_token` e `whatsapp_phone_number_id` provavelmente faltam também, pois vêm do mesmo passo do runbook. Corrigir só 3 arrisca um segundo deploy quebrado.

### Por que só `app_secret` apareceu no erro

Spring aborta no **primeiro** bean cujo placeholder falha. `MetaSignatureValidator` é `@Component` simples, construído cedo. Os demais (`verify-token` no controller, `access-token` nos services `out`) só falhariam depois. **Não** é evidência de que os outros resolvem.

---

## Decisão / abordagem

**Default no `application-prod.properties`, no ponto de referência ao secret da AWS.**

```properties
whatsapp.phone-number-id=${whatsapp_phone_number_id:NAO_CONFIGURADO}
whatsapp.access-token=${whatsapp_access_token:NAO_CONFIGURADO}
whatsapp.verify-token=${whatsapp_verify_token:NAO_CONFIGURADO}
whatsapp.app-secret=${whatsapp_app_secret:NAO_CONFIGURADO}
whatsapp.allowed-wa-ids=${whatsapp_allowed_wa_ids:}
```

**Manter** os defaults existentes nos `@Value` — servem pra testes e contextos sem o profile `prod`. Cinto e suspensório, custo zero.

**Comportamento resultante (canal inerte, como desejado):**

| Superfície | Com sentinela |
|---|---|
| `GET /webhook/whatsapp` (handshake) | sempre **403** — `hub.verify_token` nunca casa com `NAO_CONFIGURADO` |
| `POST /webhook/whatsapp` | sempre **200 silencioso + WARN** — HMAC com chave sentinela nunca casa |
| Envio/download via Graph API | nunca acionado — só dispara em resposta a mensagem entrante, que nunca passa da signature |

Perda aceita: `access-token`/`phone-number-id` deixam de ser fail-fast. **Mitigação:** log `WARN` no boot quando qualquer sentinela estiver ativo, pra a BE-20 não ativar o canal achando que está configurado.

### Guarda de regressão

Adicionar teste que varre `application-prod.properties` e falha se aparecer placeholder **sem default** fora de uma allow-list explícita de secrets genuinamente obrigatórios. Isso mata a classe inteira de bug — não só a ocorrência do WhatsApp — e roda sem AWS.

Allow-list obrigatória (secrets que **devem** derrubar o boot se faltarem): `db_host`, `db_username`, `db_password`, `telegram_token`, `keystore_password`, `admin_api_key`, `jwt_secret`.

---

## Escopo / arquivos

### Modificar
- `financas_bot_telegram/src/main/resources/application-prod.properties` — adicionar default `:NAO_CONFIGURADO` nas 4 properties `whatsapp.*` de credencial e `:` (vazio) em `allowed-wa-ids`. Comentário curto explicando por que o default mora aqui e não no `@Value`.
- `.../adapters/in/whatsapp/security/MetaSignatureValidator.java` — log `WARN` no construtor se `appSecret.equals("NAO_CONFIGURADO")`. Sem mudar lógica de validação.

### Criar
- `financas_bot_telegram/src/test/java/.../infra/ProdPropertiesPlaceholderDefaultsTest.java` — carrega `application-prod.properties` como resource, regex `\$\{([^}:]+)\}` sobre os **valores**, assert de que o conjunto encontrado ⊆ allow-list. Teste puro de recurso, **sem** subir contexto Spring e **sem** AWS.

### Não tocar
- Lógica de HMAC, handshake, mapper, allow-list runtime — só configuração declarativa.
- `application.properties` / `application-dev.properties.example` — já têm `CHANGE_ME` literal, não têm placeholder aninhado.
- Popular os secrets reais na AWS — é da BE-20, e o humano **não quer ativar o WhatsApp agora**.

---

## Testes

- **Unit (novo):** `ProdPropertiesPlaceholderDefaultsTest` — 1 teste. Falha se alguém reintroduzir placeholder sem default.
- **Unit (existentes):** `MetaSignatureValidatorTest`, `WhatsAppWebhookControllerTest` devem continuar verdes sem alteração. Se o WARN novo quebrar algum assert de log, ajustar o teste, não o código.
- **Manual (obrigatório, é o critério real):** boot em prod na EC2 sem os secrets WhatsApp populados.
- `testes_novos ≥ 1`; `testes_total` = baseline atual + 1.

---

## Critérios de aceitação

- [ ] `application-prod.properties` — as 5 properties `whatsapp.*` têm default no placeholder do secret.
- [ ] Nenhum outro placeholder sem default em `application-prod.properties` além da allow-list de secrets obrigatórios.
- [ ] `ProdPropertiesPlaceholderDefaultsTest` criado e verde; falha se o default for removido (verificar invertendo temporariamente).
- [ ] `MetaSignatureValidator` loga `WARN` no boot quando o sentinela está ativo.
- [ ] `mvn test` verde, sem regressão.
- [ ] `mvn package -DskipTests` ok.
- [ ] Branch `fix/006-whatsapp-defaults-no-properties` saiu de `develop`.
- [ ] Território só `financas_bot_telegram/`.
- [ ] Status report `docs/sprints/03-folha-pagamento/status/FIX-006-whatsapp-defaults-no-properties.md` com frontmatter válido.

**Validação em prod (humano, pós-merge):** app sobe; `GET /webhook/whatsapp?hub.mode=subscribe&hub.verify_token=xxx&hub.challenge=foo` → **403**.

---

## Fora de escopo

- Popular secrets reais na AWS — BE-20, bloqueada por chip.
- Migrar `@Value` pra `@ConfigurationProperties` com validação — melhoria legítima, vira item de pendência.
- Expor status do sentinela em `/actuator/info` — fica pra BE-22.
- Auditar `application-prod.properties` de outros domínios além do gate do teste novo.

---

## Riscos & mitigações

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| Secrets WhatsApp reais existem e o sentinela mascara erro futuro | Baixa | Médio | Default só entra quando a chave falta; se existir, vence. WARN no boot torna visível. |
| Perder fail-fast de `access-token`/`phone-number-id` | Média | Baixo | Canal inerte por design; WARN no boot + checklist da BE-20 cobrem. |
| Outro placeholder sem default quebra o próximo deploy | Média | Alto | É exatamente o que o teste novo previne. |
| Regex do teste pega `${}` em comentário e dá falso positivo | Baixa | Baixo | Ignorar linhas iniciadas com `#`; parsear via `Properties`, não linha a linha. |

---

## Coordenação

- **Paralelo:** não conflita com nenhuma task da sprint 03 (territórios disjuntos — ninguém mais mexe em `application-prod.properties`).
- **Bloqueia:** o PR `develop → main` (deploy das sprints 02 e 03).
- **Atenção pro Reviewer:**
  1. Confirmar que o default **de fato** resolve a cadeia aninhada — não aceitar "o `@Value` já tem default" como argumento, foi exatamente esse o erro do FIX-001.
  2. Confirmar que as 5 properties foram cobertas, não só as 3 do erro original.
  3. Verificar que o teste novo **falha** quando o default é removido — teste que não pega o bug não vale nada.
- **Após merge:** atualizar `STATE.md`; abrir PR `develop → main`; anotar na BE-20 que popular os secrets é pré-condição de ativação.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, **revisão do Reviewer** (ADR 0005). `fluxos_qa: []` → **sem gate QA**: mudança é configuração declarativa, sem superfície de comportamento nova; a validação real é boot em prod, não automatizável pela suíte. PR direto pra `develop` (FIX não passa por integration branch).

---

## Referências

- `docs/sprints/02-canal-whatsapp/plans/FIX-001-whatsapp-defaults-deploy-safe.md` — origem da premissa falha (§Escopo linha 79).
- `docs/sprints/02-canal-whatsapp/status/FIX-001-...md:60` — "deve mergear antes de BE-19 chegar em main".
- `docs/runbooks/RUNBOOK-prep-wa-meta-cloud-api.md` Fase 7 — as 4 chaves entram juntas.
- `docs/STATE.md:96-97` — deploy sprint 02 pendente; BE-20 bloqueada por chip.
- `docs/aprendizado/spring-placeholder-aninhado-default.md` — lição destilada.
