# BE-17b — Renomear rota Telegram `/webhook` → `/webhook/telegram` (simetria com WhatsApp)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** spec do arquiteto `docs/architecture/adapter-whatsapp-cloud-api.md` §4.0 (convenção de rotas — namespace por canal nos dois adapters, em vez de Telegram na raiz + WhatsApp aninhado). Conhecido desde a quebra original da sprint 02 (MASTER-PROMPT overnight 1 cita BE-17b).
> - **Prioridade:** baixa. Cosmético/estrutural — não bloqueia BE-19/BE-20 (a BE-19 já entrou em `/webhook/whatsapp`, então hoje convivem `/webhook` Telegram + `/webhook/whatsapp` WhatsApp, assimetria leve). Útil de fazer cedo enquanto o WhatsApp ainda não tá vivo em prod com volume, pra evitar dois renames de configuração externa.
> - **Esforço:** **baixo** (~1-2h). Mudança mecânica de uma linha de annotation + setWebhook coordenado + atualizar testes que batem `/webhook`.
> - **Território / quem executa:** `financas_bot_telegram/src/main/java/.../adapters/in/telegram/controller/TelegramWebhookController.java` + testes correspondentes → **Claude do back**. Passo `setWebhook` na Meta é **manual do humano**.
> - **Branch:** `feature/be-17b-renomear-rota-telegram`, a partir de `develop`.
> - **Dependências:** **BE-17 em `develop`** ✅ (a refatoração da porta agnóstica já existe; este é o passo restante do conjunto BE-17).
> - **Riscos:** **médio operacional** (não técnico). Entre o deploy do código novo e a chamada `setWebhook` apontando pra nova URL, o Telegram **não entrega mensagens** — fica enviando POSTs pra `/webhook` que devolve 404, e a Meta entra em retry. Mitigação: janela de deploy curta, executar `setWebhook` imediatamente após confirmar deploy ok, rollback fácil (`setWebhook` aponta de volta pra `/webhook` se preciso, e o deploy reverso restaura a annotation).

---

## Contexto

Hoje:
- `POST /webhook` → Telegram (raiz, sem qualificador — herdado de quando só existia um canal).
- `GET/POST /webhook/whatsapp` → WhatsApp (nova rota da BE-19, aninhada).

A assimetria não é problema funcional, mas:
- Quebra a regra "cada canal tem seu namespace explícito" da spec §4.0.
- Confunde leitura: um stranger lendo o código vê `/webhook` e pergunta "de que canal?".
- Dificulta adicionar um terceiro canal no futuro (Discord faria `/webhook/discord`? Mas Telegram fica como exceção?).

Fix: mover Telegram pra `/webhook/telegram` pra paridade.

**Quem pode mudar do lado externo:** o Telegram entrega no URL configurado via `setWebhook` (API do bot). Mudar o URL aceito pelo nosso código sem mudar o `setWebhook` faz o Telegram entregar no URL antigo → 404 → retry → fila trava. Tem que ser deploy **coordenado**.

## Decisão / abordagem

**Mudança de uma linha** no `TelegramWebhookController` + atualizar testes + documentar o passo manual de `setWebhook` no status report. O Telegram aceita re-`setWebhook` quantas vezes quiser; idempotente.

```java
// Antes
@PostMapping("/webhook")
public ResponseEntity<Void> receberMensagem(...)

// Depois
@PostMapping("/webhook/telegram")
public ResponseEntity<Void> receberMensagem(...)
```

**Não é rename inócuo** — exige a chamada externa de `setWebhook` na mesma janela. Documentar como passo operacional pós-merge no status.

## Escopo / arquivos

### Modificar

- `adapters/in/telegram/controller/TelegramWebhookController.java` — única troca: `@PostMapping("/webhook")` → `@PostMapping("/webhook/telegram")`.
- `src/test/.../TelegramWebhookControllerTest.java` — substituir as URLs do MockMvc (`post("/webhook")` → `post("/webhook/telegram")`).
- Eventuais outros testes de integração que batem `/webhook` direto — `grep -rn '"/webhook"' src/test` antes do commit.

### Não tocar

- Nada do WhatsApp adapter.
- Nada da porta `MensagemEntrantePortIn` nem `MensagemEntranteService`.
- Mapper, strategies, exceções.
- Properties (a URL não tá em property; está hard-coded na annotation).

## Critérios de aceitação

- [ ] `TelegramWebhookController` responde no novo path `/webhook/telegram`.
- [ ] Path antigo `/webhook` **não responde mais** (esperado 404 ou Method Not Allowed) — confirmar com teste.
- [ ] Testes do Telegram controller passam apontando pro path novo.
- [ ] `mvn test` verde (sem regressão); `testes_total` mantém a mesma ordem de grandeza (mudança não adiciona testes novos significativos; ajusta os existentes).
- [ ] `mvn package -DskipTests` ok.
- [ ] Branch saiu de `develop` (fluxo novo: `git fetch && git checkout -b feature/be-17b-renomear-rota-telegram develop`).
- [ ] Território só `financas_bot_telegram/`.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/BE-17b.md` com frontmatter válido **+ inclui os comandos manuais pra o humano executar pós-merge**:
  ```bash
  # Pós-deploy do código novo em prod (a app respondendo em /webhook/telegram):
  curl -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
    -d "url=https://api.satyansaita.com/webhook/telegram"

  # Confirmar:
  curl "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getWebhookInfo"
  ```
  E observar o resultado: `"url": "https://api.satyansaita.com/webhook/telegram"`, sem `pending_update_count` grande nem `last_error_message`.

## Janela operacional (humano executa)

Pós-merge + deploy do back, **na mesma janela**:

1. Aguardar GitHub Actions deploy pra prod terminar.
2. Smoke rápido: `curl https://api.satyansaita.com/webhook/telegram -X POST -d 'x' -i` → esperar 400/415 (existe mas payload inválido) — confirma que a rota nova está montada.
3. Executar `setWebhook` apontando pra nova URL (comando no status report).
4. Mandar uma mensagem teste pro bot do Telegram (via app pessoal).
5. Confirmar no `getWebhookInfo` que `last_error_message` está vazio e `pending_update_count` zerou.

**Tempo total da janela:** ~5 min entre deploy ok e mensagem teste recebida.

**Rollback (se algo der errado):**
- Revert do commit + redeploy: restaura `/webhook` na app.
- `setWebhook` apontando de volta: `curl -X POST .../setWebhook -d "url=https://api.satyansaita.com/webhook"`.
- Sem perda de dados — só perde mensagens que chegaram durante a janela.

## Fora de escopo (explicitamente)

- Mover allow-list do Telegram pra outro lugar (continua em property `telegram.allowed-user-ids`).
- Mexer no path do WhatsApp (já `/webhook/whatsapp`).
- Adicionar versioning na rota (ex.: `/v1/webhook/telegram`) — esperar virar necessidade real.
- Configurar a URL via property (`telegram.webhook-path`) — hardcoded é suficiente.
- Smoke test E2E completo do Telegram em prod (já existe `RUNBOOK-smoke-telegram.md`).

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Esquecer de chamar `setWebhook` após deploy | Média | Médio (mensagens perdem durante a janela) | Checklist no status report; smoke obrigatório; alarme `finbot/app/errors` da DEP-09 capta 404s repetidos. |
| `setWebhook` falha (token inválido ou rede) | Baixa | Médio | Comando no status report inclui `getWebhookInfo` pra confirmar; reexecutar é idempotente. |
| Algum teste obsoleto bate `/webhook` direto e quebra silencioso | Média | Baixo | `grep -rn '"/webhook"' src/test` antes do commit; CI roda os testes. |
| Cliente externo (script de monitoramento, ferramenta antiga) bate `/webhook` direto | Baixa | Baixo | Não temos cliente externo do nosso lado fora do Telegram. Confirmar no Reviewer. |
| Pedro está mandando mensagem no exato momento da janela | Baixa | Baixo | Janela de 5 min. Aviso prévio opcional pro Pedro. |

## Coordenação

- **Pode ir antes ou depois de BE-19/BE-20** — BE-19 já entregou `/webhook/whatsapp`; BE-17b só ajusta o lado Telegram. Não há conflito de merge.
- **Recomendação:** rodar **antes** de BE-20 (que coloca WhatsApp em prod). Razão: BE-20 vai chamar configuração externa na Meta; ter Telegram já na rota simétrica reduz "duas mudanças de URL externa em curto espaço de tempo".
- **Após merge:** smoke do Telegram (já registrado em `RUNBOOK-smoke-telegram.md`). Atualizar `docs/STATE.md` mencionando "rota Telegram agora simétrica em `/webhook/telegram`".

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report com **comandos manuais documentados**, e **revisão do Reviewer** (baixo risco técnico mas alta coordenação operacional). Abrir PR pra `develop`; **não mergear sozinho**.

## Referências

- Spec: `docs/architecture/adapter-whatsapp-cloud-api.md` §4.0 (convenção de rotas e nota sobre `setWebhook` coordenado).
- BE-17 (porta agnóstica) — já em develop ✅; este é o item residual.
- `docs/runbooks/RUNBOOK-smoke-telegram.md` — smoke reusável pós-deploy.
- Telegram Bot API `setWebhook`: <https://core.telegram.org/bots/api#setwebhook>.
- Próxima task lógica depois desta: **BE-20** (deploy WhatsApp em prod + Meta config).
