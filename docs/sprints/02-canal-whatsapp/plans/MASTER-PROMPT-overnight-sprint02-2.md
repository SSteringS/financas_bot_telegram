# MASTER PROMPT — Overnight 2 da sprint 02 (BE-19a + BE-18 + BE-21a)

Cole o bloco abaixo numa sessão nova do **Claude do back** (Claude Code no IntelliJ/CLI, ideal com `claude --dangerously-skip-permissions`) antes de dormir. Produz **só o código** de três tasks **independentes** entre si, todas saindo de `develop`, parando antes de qualquer apply/SSH/merge. Os manuais (deploy, smoke test, Reviewer) ficam pra você de manhã — checklist no fim.

> **Por que essas três:** todas dependem **só do BE-17** (já em `develop`) e **não dependem entre si** no código — então cada uma sai em sua própria branch a partir de `develop`, sem violar a regra de "branch-a-partir-de-branch" do `CLAUDE.md` (incidente FE-12). E juntas formam o backbone funcional da sprint 02: **BE-19a** (idempotência), **BE-18** (saída WhatsApp), **BE-21a** (eventos + Telegram notif).
>
> **O ganho de produto:** a BE-21a, quando mergeada e deployada, **entrega EVO-02 funcional pelo Telegram** — Pedro começa a receber notificações automáticas no canal que já está aprovado, **sem esperar a verificação Meta**. Bypass elegante do gargalo do erro 130497 (BR).

> **Por que code-only:** sessão desacompanhada não roda `terraform apply`, SSH, console AWS nem merge. Escreve código, valida local (`mvn`/`shellcheck`), commita, escreve status report. Reviewer + deploy ficam com o humano.

---

```
Você é o Claude do back deste projeto. Leia, nesta ordem:
- CLAUDE.md (regras globais)
- docs/roles/backend.md (seu papel)
- docs/STATE.md
- docs/sprints/02-canal-whatsapp/README.md (objetivo da sprint)
- docs/architecture/adapter-whatsapp-cloud-api.md (a spec do arquiteto — §4.3 pra BE-19a, §5 pra BE-18, §3 e §6 pra BE-21a)
- docs/architecture/estado-atual.md §3, §5, §6 (mapa do código atual pós-BE-17 — referência pra padrões a espelhar)
- docs/decisions/0003-controller-webhook-nunca-retorna-5xx.md (relação com BE-19a)
- docs/decisions/0013-* (multi-canal)
- docs/decisions/0014-notificacoes-via-eventos-in-process.md (a decisão arquitetural da BE-21a)
- docs/aprendizado/whatsapp-restricoes-pais-business-nao-verificada.md (por que BE-21a entrega Telegram primeiro)
- docs/sprints/02-canal-whatsapp/plans/BE-19a-idempotencia-mensagem-processada.md
- docs/sprints/02-canal-whatsapp/plans/BE-18-adapter-saida-whatsapp.md
- docs/sprints/02-canal-whatsapp/plans/BE-21a-eventos-notificador-canalpreferido.md

## REGRAS DURAS (não violar)
1. CODE-ONLY. NÃO rode `terraform apply`. NÃO faça SSH. NÃO mexa no console/CLI da AWS nem da Meta. NÃO faça `git push` pra `develop` nem merge. NÃO rode testes em produção.
2. Validação permitida é LOCAL: `mvn test`, `mvn package -DskipTests`. Testes de integração com Testcontainers são bem-vindos (e exigidos pela BE-21a por causa do AFTER_COMMIT).
3. UMA BRANCH POR TASK, criada **a partir de `develop`** — NUNCA a partir de outra feature branch (incidente FE-12). Pra cada task:
   `git checkout develop && git pull && git checkout -b feature/<id>-<slug>`.
   UM commit por task no padrão `feat(<ID>): ...`.
4. Ao terminar CADA task: escreva o status report em `docs/sprints/02-canal-whatsapp/status/<ID>.md` com frontmatter válido (modelo em `docs/status/_TEMPLATE.md`), e PARE — não mergeie.
5. Território: só `financas_bot_telegram/`. NÃO toque em `frontend/`, infra, ou docs estruturais.
6. Se bater numa decisão de produto ou ambiguidade que o plano não cobre, NÃO invente: registra como pendência no status report daquela task (`estado: bloqueado`) e segue pra próxima.

## ORDEM E ESCOPO

Ordem deliberada: do mais bounded pro mais denso. Mantém momentum.

### Task 1 — BE-19a (idempotência)
Branch: `feature/be-19a-idempotencia-mensagem-processada`
Siga `docs/sprints/02-canal-whatsapp/plans/BE-19a-idempotencia-mensagem-processada.md`. Resumo do escopo:
- Migração Flyway V4 (ou V5 se V4 já existir — conferir!) com tabela `mensagem_processada (canal, id_externo)` + unique key.
- Entity + Repository.
- `MensagemProcessadaService.tentarClaim(canal, idExterno)` que tenta INSERT e retorna boolean (false em DataIntegrityViolationException).
- Integrar no `MensagemEntranteService` (criado pela BE-17): claim no início do processamento; falha do claim = descarta silently (log INFO, não ERROR).
- Criar enum `CanalMensagem` (ou `Canal` se a BE-21a já tiver criado o enum unificado — coordenar via comentário no status report).
- Testes Testcontainers: insert duplicado, reentrega da mesma mensagem não cria duplicata, rollback do negócio remove o claim.

### Task 2 — BE-18 (adapter saída WhatsApp)
Branch: `feature/be-18-adapter-saida-whatsapp` (sai de `develop`, NÃO da BE-19a).
Siga `docs/sprints/02-canal-whatsapp/plans/BE-18-adapter-saida-whatsapp.md`. Resumo:
- `WhatsAppMessageSenderService.enviarTexto(waId, body)` + `enviarTemplate(waId, name, lang, params)`.
- `WhatsAppMediaDownloaderService.baixar(mediaId)` (2 GETs com Bearer).
- `WhatsAppApiException` com `errorCode` (Meta) + `httpStatus` + body.
- DTOs request/response em `adapters/out/whatsapp/dto/`.
- Properties placeholders: `whatsapp.graph-api-base-url`, `whatsapp.graph-api-version` (v20.0), `whatsapp.phone-number-id`, `whatsapp.access-token`. Em `application-prod.properties`, referenciar Secrets Manager.
- HTTP client: ESPELHAR o padrão do `TelegramMessageSenderService` existente (provavelmente RestTemplate). Confirmar antes de codar.
- Testes com mocks (sem call real à Graph API). Mapear pelo menos: response 400 com `error.code=130497` → exception carregando code; response 401 → exception identificável; response 500 → exception com body.

### Task 3 — BE-21a (eventos + porta + canalPreferido + Telegram impl) — a mais densa
Branch: `feature/be-21a-eventos-notificador-canalpreferido` (sai de `develop`, NÃO das anteriores).
Siga `docs/sprints/02-canal-whatsapp/plans/BE-21a-eventos-notificador-canalpreferido.md`. Resumo:
- Migração Flyway próxima (V5 ou V6, depende do número usado em BE-19a): `ALTER TABLE requisitante ADD COLUMN canal_preferido VARCHAR(20) NOT NULL DEFAULT 'TELEGRAM'`.
- `domain/event/ComprovanteRegistradoEvent.java` (record com comprovanteId, pedidoId, requisitanteId — pequeno e estável).
- `domain/model/Canal.java` enum (UNIFICAR com BE-19a se BE-19a criou `CanalMensagem`; caso contrário, criar `Canal` aqui e BE-19a se ajusta no PR).
- `domain/model/Requisitante.java`: add campo `canalPreferido`. Atualizar entity + mapper.
- `application/port/out/NotificadorPortOut.java` + `NotificacaoDTO`.
- `application/event/NotificacaoComprovanteListener.java` com `@TransactionalEventListener(AFTER_COMMIT)` + `@Async("notificacaoExecutor")` + try/catch + log ERROR + TODO marcado pra BE-22 (métrica de falha).
- `application/config/AsyncConfig.java` com `ThreadPoolTaskExecutor` bounded (core 2, max 4, queue 100, nome `notif-`).
- `adapters/out/telegram/notificador/TelegramNotificadorImpl.java` implementando `NotificadorPortOut` — wrap do `TelegramMessageSenderService` existente. Mensagem: "✅ Comprovante registrado para o pedido #{pedidoId}. Veja em https://satyansaita.com".
- Wire: `RegistrarComprovanteServiceImpl` injeta `ApplicationEventPublisher` e publica `ComprovanteRegistradoEvent` após salvar.
- Mecanismo do `Map<Canal, NotificadorPortOut>` no listener — usar `@Qualifier` no field com chave-de-enum, ou nome do bean. ESCOLHER e anotar no status report.
- Testes COM TRANSAÇÃO REAL (Testcontainers + `@SpringBootTest`): commit dispara listener; rollback NÃO dispara; falha no notificador não afeta a tx do usecase (já comitou).

## SE ALGUMA COISA QUEBRAR

- Migração da BE-19a quebrar: registra erro no status, `estado: parcial`, segue pra BE-18.
- BE-18 esbarrar em ambiguidade de HTTP client (RestTemplate vs WebClient): ler o Telegram sender, escolher o mesmo padrão, anotar no status.
- BE-21a falhar no teste de AFTER_COMMIT por falta de transação real: NÃO esconda mockando — registre como pendência (gotcha conhecida do ADR 0014 §riscos) e siga.
- Se a numeração da migração conflitar entre BE-19a e BE-21a (ambas precisam de número próximo), coordene no momento: BE-19a usa V4, BE-21a usa V5 (ou ajustar conforme V atual no diretório).
- Qualquer ambiguidade entre plano e estado-atual: registrar e seguir/parar conforme o caso.

## AO FINAL

Escreva `docs/sprints/02-canal-whatsapp/status/_RESUMO-overnight-sprint02-2.md` com:
- Tabela: task | branch | commit | resultado das checagens locais | estado.
- Decisões tomadas durante a sessão (ex.: enum `Canal` unificado ou separado; HTTP client escolhido; números das migrações).
- CHECKLIST MANUAL PRA MANHÃ (humano), na ordem:
  1. Smoke test do BE-17 em dev (se ainda não fez) — `docs/runbooks/RUNBOOK-smoke-test-telegram-cadeia.md`. Depois deploy do BE-17 pra prod.
  2. Revisar BE-19a (Reviewer) → merge develop → main → deploy. Validar idempotência com smoke test cenário 6.
  3. Revisar BE-18 (Reviewer) → merge.
  4. Revisar BE-21a (Reviewer — alto risco, toca usecase + eventos novos) → merge → deploy → **mandar mensagem pro bot Telegram com comprovante** → confirmar que Pedro recebe a notificação automática (EVO-02 parcial funcionando).
- Pendências/decisões que precisam de você (se houver).

NÃO mergeie nada. Pare aqui.
```

---

## Notas pra você (humano), fora do prompt

- **Três branches independentes** (cada uma de `develop`). Nenhuma viola "branch-a-partir-de-branch".
- **BE-17b (rename rota Telegram)** segue **fora** desta overnight — quero ele junto da janela de deploy com você, pra o `setWebhook` ser coordenado.
- **BE-19 (entrada WhatsApp)** **depende** de BE-19a, BE-18 e PREP-WA — não pode entrar nesta overnight, vai pra próxima quando essas duas mergeadas.
- **BE-22 (Micrometer)** vai esperar todas estas mergeadas pra instrumentar nos pontos certos sem conflito de merge.
- **PREP-WA e verificação Meta** seguem em paralelo, manual seu.
- **Próxima overnight provável (depois das mergeadas):** BE-19 (entrada WhatsApp, depende de PREP-WA pra teste real) + BE-22 (Micrometer).

## Sobre a entrega da EVO-02 via Telegram

A BE-21a tem um efeito que vale destacar: **assim que mergeada e deployada, Pedro começa a receber notificação automática toda vez que um comprovante for registrado, pelo Telegram**. Sem esperar a verificação Meta, sem template, sem novo número. É a EVO-02 parcial entregando valor real à família já antes do gargalo da Meta resolver. Depois, com a verificação aprovada + template aprovado + BE-21b mergeada, o canal padrão dos requisitantes pode migrar pra WhatsApp se quiser.

## Referências

- Planos: BE-19a, BE-18, BE-21a (linkados acima).
- Spec: `docs/architecture/adapter-whatsapp-cloud-api.md`.
- ADRs: 0003, 0013, 0014.
- Padrão de overnight anterior: `docs/sprints/02-canal-whatsapp/plans/MASTER-PROMPT-overnight-sprint02-1.md`.
</content>
