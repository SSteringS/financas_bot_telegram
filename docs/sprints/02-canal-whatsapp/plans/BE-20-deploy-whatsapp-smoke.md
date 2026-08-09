# BE-20 — Deploy WhatsApp em prod + configurar webhook na Meta + smoke E2E (fim-a-fim EVO-01)

> 🅿️ **PARKED — 2026-05-29.** Esta task **sai da sprint 02** por bloqueio externo: (a) Test number da Meta sofre restrição BR 130497 (Business não-verificada não envia business-initiated); (b) chip dedicado pra WhatsApp Business real **não comprável imediatamente**. Sem chip + sem Business Verification, smoke E2E não roda.
>
> **Mecanismo de parqueamento** (não jogamos fora):
> - O **código** do canal WhatsApp (BE-19 incluso) **deploya em prod** mesmo assim — defaults sentinelas no `@Value` mantêm endpoints inertes (ver "Atualização 2026-05-29 — escopo deploy-safe" no plano BE-19).
> - Esta task entra na **sprint vigente quando o chip chegar** + Business Verification estiver concluída. O plano abaixo continua válido sem alteração, só muda de sprint.
> - PREP-WA fases 4 (Business Verification, lead time externo) e 7 (Secrets populados com valores reais) precisam estar concluídas antes desta task entrar em execução.
>
> **Critério de "pode entrar na sprint":** chip pronto + Business Verification concluída + secrets `whatsapp_verify_token`, `whatsapp_app_secret`, `whatsapp_allowed_wa_ids` populados na AWS com valores reais (substituindo os sentinelas `NAO_CONFIGURADO` da defesa do BE-19).
>
> **O que NÃO entra junto:** BE-21b (`WhatsAppNotificadorImpl` + templates) e EVO-02 completa (`canalPreferido` Pedro → WHATSAPP) também ficaram parqueadas e dependem desta task primeiro. Quando BE-20 entrar, planejar BE-21b em seguida.

> **Intake (contrato de entrada da task)**
>
> - **Origem:** spec do arquiteto `docs/architecture/adapter-whatsapp-cloud-api.md` §10 (sequência sugerida, item 5: "fim-a-fim do canal — registrar pedido/comprovante via WhatsApp"). BE-18 status anotou explicitamente: *"Integração real com a Meta só na BE-20"*. RUNBOOK-prep-wa-meta-cloud-api.md Fase 8 só roda **após** BE-19 deployado — exatamente o gatilho desta task.
> - **Prioridade:** alta — é a task que **fecha o circuito EVO-01** (canal WhatsApp **vivo em prod**). Sem ela, BE-17/BE-18/BE-19/BE-19a/BE-21a estão prontos no código mas não geram um pedido WhatsApp real.
> - **Esforço:** **médio operacional, baixo de código** (~2–4h). 80% manual humano (Secrets Manager + console Meta + smoke); 20% Claude (popular `whatsapp.allowed-wa-ids` em `application-prod.properties` se for hardcoded, atualizar `STATE.md` e `RUNBOOK-prep-wa-meta-cloud-api.md` pós-execução).
> - **Território / quem executa:** primariamente **humano** (console Meta + AWS Secrets Manager + Telegram pessoal pra smoke); ajustes pontuais de properties → **Claude do back** se necessário.
> - **Branch:** `feature/be-20-deploy-whatsapp-smoke` (curtinha — provavelmente só ajuste de properties + atualização de docs). Pode até dispensar branch se a única mudança for em docs.
> - **Dependências (DURAS):**
>   - **BE-19 mergeada em `develop` + deployada em prod** ✅ (sem isso, fase 8 do PREP-WA falha — handshake da Meta retorna 403/timeout).
>   - **PREP-WA fases 1–7 concluídas** ⏳ (Meta App + Test number ativo + System User token + verify_token + 4 secrets na AWS).
>   - **Cert HTTPS válido em `api.satyansaita.com`** ✅ (DEP-03 + Caddy/LE — já em prod desde a sprint 01).
>   - **`whatsapp.allowed-wa-ids` definido** (lista de números autorizados) — humano decide a lista de números (mínimo: o próprio dele pra smoke). Se BE-19 deixou isso configurado via Secrets/properties, basta popular; se virou property hardcoded, ajustar.
> - **Riscos:**
>   1. **Handshake da Meta falha** (verify_token diferente / app não respondendo / DNS errado) — fase 8 do runbook documenta como debugar.
>   2. **Restrição BR 130497** se tentar enviar business-initiated antes da Business Verification (ver `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md`). Smoke desta task é só sobre **entrada** + **resposta na janela 24h**, não envio business-initiated → não bloqueia. Anotar pra EVO-02.
>   3. **Test number da Meta com 5 destinatários permitidos** — humano precisa ter número pessoal já adicionado como recipient permitido (fase 5/9 do runbook).
>   4. **Mensagens reais perdidas** se entre deploy e config do webhook alguém tentar mandar — N/A na prática (WhatsApp **ainda não tem volume** — é estreia).

---

## Contexto

Após BE-19 em develop + prod, o backend tem:
- `GET/POST /webhook/whatsapp` funcionando contra **mocks** localmente.
- Adapter de entrada validando signature, mapeando envelope, plugando na porta agnóstica.
- Adapter de saída (BE-18) com sender + downloader prontos.
- Idempotência (BE-19a) cobrindo `wamid`.
- Notificação async (BE-21a) com Telegram já roteando por `canalPreferido`.

Falta o **circuito real** funcionar: a Meta entregar de fato uma mensagem no webhook em prod, o back processar, e responder. Isso depende de configuração externa na Meta (fase 8 da PREP-WA) que só vale **com a app deployada respondendo o handshake**.

Esta task é primariamente **operacional/manual** — está mais perto de um runbook que de um plano de código. Modelo: similar ao `DEP-06-smoke-test-e2e-prod.md` (runbook E2E manual).

## Decisão / abordagem

**Sequenciamento sem volta atrás** (cada passo testa o anterior):

1. Garantir BE-19 mergeada + deployada (status de `STATE.md` confirma).
2. Garantir secrets WhatsApp populadas (humano via console Secrets Manager — PREP-WA fase 7).
3. Restart da app em prod se necessário pra recarregar secrets (depende de como Spring Cloud AWS está configurado — provavelmente pega no boot).
4. Smoke das credenciais via `curl` direto na Graph API (PREP-WA fase 9) — **não depende do nosso webhook** — confirma `access-token` válido + `phone-number-id` certo.
5. Configurar webhook na Meta (PREP-WA fase 8) — handshake bate, app responde, Meta aceita.
6. Subscribe ao field `messages` no console Meta.
7. Smoke entrada: enviar mensagem pelo Test number Meta pra nosso bot → ver chegando no log `/finbot/app` em CW + ver pedido criado no banco.
8. Smoke fim-a-fim: enviar legenda válida ("150.00 Almoço com cliente" + foto) → ver pedido registrado + (se canalPreferido do requisitante for TELEGRAM) sem notificação ainda; se WHATSAPP, mensagem de notificação chegando no Test number remetente. **Atenção:** o `WhatsAppNotificadorImpl` (BE-21b — não existe ainda) é necessário pra notificação WhatsApp; sem ele, smoke de notificação WA fica fora desta task.
9. Pedro (opcional, se ele topar): adicionar `wa_id` do Pedro à allow-list, ele manda mensagem real, valida UX no app dele.

**Decisão sobre canal preferido:** o smoke desta task usa **`canalPreferido = TELEGRAM`** no requisitante de teste (humano). Assim valida:
- Entrada WhatsApp → registra pedido → log estruturado → banco ok.
- Notificação async dispara via Telegram (canalPreferido) — circuito notificação testado.

Avaliar mudar `canalPreferido` pra WHATSAPP só quando BE-21b (notificador WhatsApp) existir.

## Escopo / arquivos

### Modificar (provável — verificar como BE-19 deixou)

- `src/main/resources/application-prod.properties` — popular `whatsapp.allowed-wa-ids=<lista CSV>` se BE-19 deixou hardcoded. Se está vindo de secret (`${whatsapp_allowed_wa_ids}`), criar a chave no Secrets Manager.
- `docs/STATE.md` — após smoke ok, atualizar mencionando "WhatsApp Test number vivo em prod; entrada validada; notificação WA pendente de BE-21b".
- `docs/runbooks/RUNBOOK-prep-wa-meta-cloud-api.md` — marcar fases 8 e 9 como `[x]` concluídas se foram nesta task (manter o runbook como source-of-truth de "como fizemos").

### Não tocar

- Código de adapter de entrada/saída/porta — já está pronto.
- Schema do banco.
- Terraform — secrets já existem (`finbot-prod-secrets`); só adicionar chaves novas no console AWS é manual.

## Passos manuais (humano executa — checklist do status report)

Pré-condições verificadas:
- [ ] BE-19 em `develop` + deployada em `main` + EC2 respondendo `GET /webhook/whatsapp?hub.mode=...&hub.verify_token=<errado>&hub.challenge=foo` com **403** (smoke do handshake — antes de configurar na Meta, prova que o controller está lá).
- [ ] PREP-WA fases 1–7 concluídas (Meta App criado, Test number ativo, System User Token gerado, verify_token gerado, 4 secrets populadas).
- [ ] Confirmar `whatsapp.allowed-wa-ids` está populado com o seu `wa_id` pessoal.

Execução:
1. **PREP-WA Fase 8** — configurar webhook na Meta:
   - App → painel WhatsApp → *Configuration* → **Webhook** → *Edit*.
   - Callback URL: `https://api.satyansaita.com/webhook/whatsapp`.
   - Verify token: o mesmo gerado na fase 6.3 + salvo em Secrets.
   - *Save* → Meta dispara GET handshake → app responde 200+challenge → config aceita.
   - Subscribe aos fields: `messages` (obrigatório) + `message_template_status_update` (útil pra EVO-02).
2. **PREP-WA Fase 9** — smoke de saída (validar credenciais via curl, sem usar nosso código):
   - Painel *API Setup* → copiar `curl` exemplo, substituir token pelo System User Token (não o temporário).
   - Adicionar seu número pessoal como Recipient permitido (até 5 grátis).
   - Rodar `curl` → recebe "hello_world" no WhatsApp pessoal → confirma `access-token` + `phone-number-id` funcionam.
3. **Smoke entrada** (o coração desta task):
   - Responder "hello_world" pelo WhatsApp pessoal mandando uma mensagem qualquer ("teste") pro Test number do bot.
   - Console CW Logs Insights, log group `/finbot/app`, query: `fields @timestamp, message | filter message like /whatsapp/ | sort @timestamp desc | limit 20`.
   - Esperar ver: linha de log do controller chegando + log do mapper desserializando + log do `MensagemEntranteService.processar()`.
4. **Smoke fim-a-fim — pedido válido com foto**:
   - WhatsApp pessoal → enviar foto + legenda "150.00 Almoço teste BE-20".
   - Console MySQL (ou via app pessoal no front): confirmar pedido criado.
   - Conferir log: `tipo=PEDIDO, canal=WHATSAPP, externalId=wamid.xxx, valor=150.00, descricao=Almoço teste BE-20`.
   - Conferir tabela `mensagem_processada`: linha `canal=WHATSAPP, id_externo=wamid.xxx`.
5. **Smoke notificação async** (canalPreferido=TELEGRAM):
   - Anexar comprovante ao pedido criado: WhatsApp → foto + legenda "#<id> PIX teste".
   - Comprovante registra → `ComprovanteRegistradoEvent` publica → listener dispara → notificação chega via **Telegram** (canalPreferido).
   - Conferir notificação chegou no Telegram pessoal com link pro front.
6. **(Opcional) Pedro entra** — adicionar `wa_id` do Pedro à allow-list, restart, validar UX dele.

Pós-smoke:
- [ ] Atualizar `STATE.md` e runbook como anotado em "Modificar".
- [ ] Status report com prints/screenshots/logs anexados se possível.

## Critérios de aceitação

- [ ] Webhook configurado e aceito na Meta (handshake passa).
- [ ] Smoke fim-a-fim do **pedido** via WhatsApp Test number: pedido aparece no banco; log estruturado mostra a cadeia.
- [ ] Smoke fim-a-fim do **comprovante** via WhatsApp: comprovante anexa ao pedido; notificação async chega no Telegram (canalPreferido=TELEGRAM).
- [ ] Idempotência funciona: mandar a **mesma mensagem 2x** (reenviar foto) → segundo evento descartado pela tabela `mensagem_processada` (log INFO "já processada").
- [ ] Allow-list funciona: pedir alguém **não autorizado** mandar mensagem pro bot → resposta "🚫 sem permissão" + log WARN. (Se não houver alguém pra testar, simular via Test number sandbox ou anotar como pendência.)
- [ ] `STATE.md` atualizado.
- [ ] Runbook PREP-WA atualizado com `[x]` nas fases concluídas.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/BE-20.md` com frontmatter válido + log dos smokes + observações.

## Fora de escopo (explicitamente)

- **`WhatsAppNotificadorImpl`** (notificação async via WhatsApp como canal preferido) — é **BE-21b**, depende de templates da Meta aprovados (`message_template_status_update` da fase 8). EVO-02 completa quando BE-21b existir.
- **Submissão de templates de utilidade na Meta** — manual no console, processo de aprovação de 1-3 dias. Pode rodar em paralelo com BE-20 mas não é critério desta task.
- **Migração do canal preferido do Pedro pra WHATSAPP** — depende de BE-21b + EVO-02 completa.
- **Adicionar número real (não Test)** — depende de **Business Verification** da Meta (Fase 4 do runbook, lead time externo). Adiar até verification concluída.
- **Carga/stress test** — volume da família é desprezível; não testar.

## Riscos & mitigações (operacional)

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Handshake falha (verify_token mismatch / app down / DNS errado) | Média | Médio | Fase 8 do runbook lista 3 causas comuns. `curl` direto no handshake antes de configurar na Meta isolaria. |
| `allowed-wa-ids` mal-formado (com `+`, com espaço) | Média | Médio (smoke do humano vira 401 silencioso) | Documentar formato no commit (`E.164 sem +`). Smoke valida — se falhar, ajustar e reiniciar. |
| Restrição 130497 (BR Business não-verificada) impede resposta | Baixa (smoke é só janela 24h — não business-initiated) | Médio | `aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md` é claro: resposta na janela vale; só envio business-initiated bloqueia. |
| Test number expira / cap de 5 destinatários | Baixa | Baixo | Test number é gratuito; 5 dest cobre humano + Pedro + sobrar. |
| Restart da app perde mensagens em-vôo da Meta | Baixa | Baixo | Meta retenta; nosso webhook idempotente via `wamid`. |
| Logs/métricas sem aparecer no CW | Baixa | Médio (cego sem) | DEP-09 + agente já rodando há semanas — confirmado via Telegram. Se quebrar, é regressão de infra, fora do escopo desta. |

## Coordenação

- **Bloqueia:** BE-21b (notificador WhatsApp), EVO-02 completa, migração de Pedro pra canalPreferido WHATSAPP.
- **Não bloqueia:** continuação de outras frentes do back (BE-22 micrometer pode ir em paralelo).
- **Depende externamente:** PREP-WA (humano fazendo na Meta). Se PREP-WA não tá pronto, esta task fica em espera externa.
- **Ordem sugerida na sprint:** **depois** de BE-17b (pra rotas estarem simétricas em prod quando o WhatsApp entrar) e **antes** de planejar BE-21b (precisa do canal vivo pra escolher template).
- **Janela:** ~1h dedicada (humano + tarde calma). Smoke em si é rápido (~5 mensagens).

## Definição de pronto

Smoke completo no status report (com prints do log/banco/console Meta + notificação Telegram recebida) + `STATE.md` atualizado. **Não envolve PR de código** se a única mudança for em docs/properties; abrir PR só se houve mudança em `application-prod.properties` ou similar. Reviewer foca em (a) validar o smoke (não apenas confiar no log), (b) confirmar que `STATE.md` reflete a realidade.

## Referências

- Runbook canônico desta task: `docs/runbooks/RUNBOOK-prep-wa-meta-cloud-api.md` (especialmente fases 8 e 9).
- Spec: `docs/architecture/adapter-whatsapp-cloud-api.md` §4, §10 item 5.
- Precedente de "task = runbook E2E em prod": `docs/sprints/01-mvp/runbooks/DEP-06-smoke-test-e2e-prod.md` (se houver — senão `docs/runbooks/DEP-06-*.md`).
- Aprendizado: `docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md` (restrições BR — confirma que o smoke desta task funciona mesmo sem Business Verification).
- `docs/aprendizado/whatsapp-modelo-mensagens.md` (janela de 24h, templates).
- BE-19 (a task que entregou o controller que esta task vai colocar em prod).
- Próxima task lógica: **BE-21b** (notificador WhatsApp + EVO-02 completa).
