# BE-17 — Refactor estrutural: porta de entrada agnóstica + strategies/orchestrator em `application/`

> **Intake (contrato de entrada da task)**
>
> - **Origem:** `docs/architecture/adapter-whatsapp-cloud-api.md` §3 e §4 (refactor que destrava o adapter WhatsApp; multi-canal por ADR 0013). É a **fundação** da sprint 02.
> - **Prioridade:** alta — sem ela, BE-18/BE-19a/BE-19/BE-21a não começam.
> - **Esforço:** alto (mexe em vários arquivos, **move pacotes** entre camadas, renomeia interface, cria mapper novo no Telegram).
> - **Território / quem executa:** `financas_bot_telegram/src/main/java/` (e tests correspondentes) → **Claude do back**.
> - **Branch:** `feature/be-17-refactor-porta-agnostica`, a partir de `develop`.
> - **Dependências:** nenhuma de código.
> - **Riscos:**
>   1. **Quebrar o canal Telegram vivo.** Mexe na cadeia controller→orchestrator→strategy→usecase do Telegram que está em produção. Mitigação: **todos os testes existentes do Telegram precisam continuar verdes**; novos testes pras unidades criadas; **Reviewer obrigatório** antes de merge; **não inclui** rename de rota (BE-17b).
>   2. **Move de pacote confunde imports** em tests/lugares não óbvios. Mitigação: rodar `mvn test` e `mvn package` ao fim; varrer warnings de import quebrado.
>   3. **Drift com a spec.** Mitigação: implementador lê a §3 da spec antes de começar; nomes/pacotes batem exatamente com o desenho lá.

---

## Contexto

Hoje o orchestrator (`UpdateOrchestratorService`) e as strategies vivem em `adapters/in/telegram/` e são acoplados ao tipo `Update` do Telegram (acidente histórico — eram a única coisa que processava mensagens). A spec §3 explica por que isso precisa sair do adapter:

- **Strategies** decidem "pedido" vs "comprovante" pelo **formato da legenda** — classificação de **negócio**, não de canal. Pertencem a `application/strategy/`, compartilhadas por qualquer adapter de entrada.
- **Orchestrator** despacha entre strategies — também canal-agnóstico. Vira `MensagemEntranteService` em `application/services/`, implementando uma **porta de entrada agnóstica** (`MensagemEntrantePortIn`).
- **Adapter do Telegram** passa a só converter o `Update` em `PaymentMessageDTO` (via um **mapper novo**, `TelegramMessageMapper`) e chamar a porta.

Sem esse refactor, o WhatsApp teria que importar de `adapters/in/telegram/strategy/` (acoplamento errado) ou duplicar regra (defeito que a porta agnóstica veio resolver).

## Decisão / abordagem

**Mover/renomear conforme o desenho da spec §3, com `PaymentMessageDTO` como contrato canal-agnóstico entre adapter e core.** Cadeia pós-refactor:

```
TelegramWebhookController
  → TelegramMessageMapper (Update → PaymentMessageDTO)         ← NOVO
  → MensagemEntrantePortIn.processar(dto)                      ← interface (renomeada de TelegramPortIn)
  → MensagemEntranteService (impl em application/services)     ← MOVE de UpdateOrchestratorService
  → escolhe MensagemProcessingStrategy (interface)             ← MOVE de adapters/in/telegram/strategy
  → PaymentRequestStrategy / PaymentProofStrategy              ← MOVE; passam a depender de PaymentMessageDTO
  → usecases (inalterados)
```

**O que esta task NÃO faz** (deliberadamente):

- **Não renomeia a rota** do Telegram (`/webhook` → `/webhook/telegram`) — isso é **BE-17b**, separada pra reduzir blast radius e isolar o passo operacional do `setWebhook`.
- **Não cria nada de WhatsApp** — só a fundação. WhatsApp entra em BE-18/19/19a.
- **Não toca em usecases** nem em repositórios.

## Escopo / arquivos

> Nomes e pacotes seguem o desenho da spec §3. Antes de codar, ler `docs/architecture/estado-atual.md` §3 pra mapear nomes/pacotes reais (a spec descreve o destino; o estado-atual descreve o ponto de partida).

**Criar:**

- `application/port/in/MensagemEntrantePortIn.java` — interface (`void processar(PaymentMessageDTO dto)` ou equivalente).
- `application/services/MensagemEntranteService.java` — implementação que **escolhe a strategy** via lista injetada e despacha. Substitui `UpdateOrchestratorService`.
- `application/strategy/MensagemProcessingStrategy.java` — interface (renomeada de `UpdateProcessingStrategy` se for o nome atual). Passa a operar sobre `PaymentMessageDTO`.
- `adapters/in/telegram/mapper/TelegramMessageMapper.java` — `Update` → `PaymentMessageDTO`. Cobre os tipos hoje suportados (foto + caption, document/PDF pela EVO-07, texto).
- **`PaymentMessageDTO`** — se já não existir em `application/`, criar. Campos mínimos: identificador externo (pro Telegram = `update_id`/`message_id`; pro WhatsApp futuro = `wamid`), `chatId`/`from`, `caption`/`text`, referência de mídia (`media_id` ou bytes — alinhar com como o Telegram hoje passa).

**Mover (preservar histórico — `git mv` em vez de delete+create):**

- `adapters/in/telegram/strategy/PaymentRequestStrategy.java` → `application/strategy/PaymentRequestStrategy.java` (refatorada pra aceitar `PaymentMessageDTO`).
- `adapters/in/telegram/strategy/PaymentProofStrategy.java` → idem.
- `adapters/in/telegram/strategy/UpdateProcessingStrategy.java` → `application/strategy/MensagemProcessingStrategy.java` (renomeada + agnóstica).

**Refatorar:**

- `adapters/in/telegram/controller/TelegramWebhookController.java` — passa a chamar `TelegramMessageMapper` + `MensagemEntrantePortIn`. **Comportamento externo idêntico** (ainda `POST /webhook` — a rota muda na BE-17b).
- `adapters/in/telegram/service/UpdateOrchestratorService.java` — **remover** (substituído por `MensagemEntranteService`).
- Tests das strategies — refatorar pra passar `PaymentMessageDTO` em vez de `Update`.

**Não tocar:** `frontend/`, usecases, repositórios, infra, `application-prod.properties`, `pom.xml` (a menos que falte dep — improvável).

## Critérios de aceitação

- [ ] `mvn test` **verde**, incluindo **todos os testes existentes do Telegram** (controller + strategies). Nenhum teste apagado sem substituto.
- [ ] **Novos testes** pras unidades criadas:
  - `MensagemEntranteService` — escolha de strategy (pedido vs comprovante vs fallback) com `PaymentMessageDTO` mockado.
  - `TelegramMessageMapper` — Update com foto+caption → DTO correto; Update com document/PDF (EVO-07) → DTO correto; Update apenas texto → DTO correto; Update sem caption → comportamento esperado (provavelmente lança ou ignora — alinhar com o existente).
- [ ] `mvn package -DskipTests` ok.
- [ ] **Cobertura:** toda classe nova com lógica não-trivial tem teste (regra do CLAUDE.md).
- [ ] **Nenhum arquivo de WhatsApp criado** nesta task (verificar com `git diff --stat`).
- [ ] **Rota do Telegram inalterada** (`POST /webhook` no controller) — confirmar no diff que `@PostMapping` não mudou. O rename é BE-17b.
- [ ] Branch saiu de `develop` (`git merge-base --is-ancestor origin/develop HEAD` exit 0).
- [ ] Status report em `docs/sprints/02-canal-whatsapp/status/BE-17.md` com frontmatter válido (gates ok; `testes_total` + `testes_novos` preenchidos).

## Coordenação

- **BE-17b (rota + `setWebhook`)** vem depois, na mesma janela de deploy. Esta task **não** muda comportamento externo do webhook — Telegram em prod continua chegando em `/webhook` no controller refatorado.
- **BE-18 (saída WhatsApp) + BE-19a (idempotência) + BE-19 (entrada WhatsApp) + BE-21a (notificador)** dependem desta task estar mergeada em `develop`.
- **Reviewer obrigatório** (alto risco, toca o canal vivo). Conferir contra a realidade: rodar `mvn test`, ler o diff, validar que nada de comportamento externo do Telegram mudou.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md` (build, lint na, testes verdes, branch correta, território só `financas_bot_telegram/`), status report válido, e **revisão independente pelo Reviewer** — **obrigatória** dada a densidade do refactor e o risco no canal vivo. Abrir PR pra `develop`; **não mergear sozinho**.

## Referências

- `docs/architecture/adapter-whatsapp-cloud-api.md` §3 (desenho-alvo) e §4 (contexto da cadeia)
- `docs/architecture/estado-atual.md` §3 (ponto de partida — pacotes/nomes atuais)
- ADR 0013 (multi-canal — por que o refactor é permanente, não andaime)
- `docs/decisions/0003-controller-webhook-nunca-retorna-5xx.md` (a regra "sempre 200" do webhook continua valendo no controller refatorado)
</content>
