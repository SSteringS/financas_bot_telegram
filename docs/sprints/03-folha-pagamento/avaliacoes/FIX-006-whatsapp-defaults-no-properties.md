---
task: FIX-006
sprint: 03-folha-pagamento
data: 2026-08-09
avaliador: claude-reviewer
status_report: docs/sprints/03-folha-pagamento/status/FIX-006-whatsapp-defaults-no-properties.md
veredito_codigo: aprovado
veredito_final: aprovado
observacoes_count: 6                       # 6 levantadas; 0 abertas ao fim da rodada 3
rodadas: 3                                 # rodada 1 = §0–§7; rodada 2 (delta) = §8; rodada 3 (confirmação) = §9
roteiro_executado: false
gates_verificados_contra_realidade: divergente
skills_eficazes: []
skills_gaps: [seguranca-backend]
veredito_qa: nao_aplicavel
fluxos_qa_executados: []
fluxos_qa_adicionar: []
fluxos_qa_remover: []
---

# Avaliação — FIX-006 (defaults dos placeholders WhatsApp no `application-prod.properties`)

**Branch:** `fix/006-whatsapp-defaults-no-properties`
**Implementador:** claude-back
**Plano:** `docs/sprints/03-folha-pagamento/plans/FIX-006-whatsapp-defaults-no-properties.md`
**Status report:** **não existe** (ver Observação 2)

> Nota de localização: o dispatch pediu o artefato em `ia-docs/features/<FEATURE-FOLDER>/avaliacoes/`. Essa árvore não existe neste repositório; segui a convenção vigente do `CLAUDE.md` / `docs/roles/reviewer.md` (`docs/sprints/<NN>-<slug>/avaliacoes/`).

---

## 0. Checagem de premissas (feita antes de julgar o código)

| # | Premissa do plano | Resultado | Como foi verificado |
|---|---|---|---|
| P1 | O default no nível do `properties` resolve a cadeia aninhada que o default do `@Value` não resolve | **pass** | Reprodução isolada com Spring Framework 6.2.6 (a versão que o Boot 3.4.5 traz) |
| P2 | As 5 properties `whatsapp.*` precisam de default, não só as 3 do erro | **pass** | Leitura dos 4 consumidores + confirmação de que 2 deles não têm default no `@Value` |
| P3 | `allowed-wa-ids` vazio produz lista vazia, sem bypass de autorização | **pass** | Conversão real do `DefaultConversionService` executada |
| P4 | A allow-list de 7 secrets é a correta | **pass** | Comparação com o `application-prod.properties` do último deploy **bem-sucedido** (main @ `042deef`, 2026-05-27) |
| P5 | Os 48 erros de `*IntegrationTest` são ambientais e pré-existentes | **pass** | Daemon Docker comprovadamente fora do ar + os ITs usam `@ActiveProfiles("integration-test")`, não carregam `application-prod.properties` |
| P6 | "O canal fica inerte: POST nunca valida a assinatura" | **FAIL** | Ver Observação 1 — a sentinela é uma constante pública; o HMAC casa para quem a conhece |
| P7 | "O PR `develop → main` ainda não foi aberto" (base do §"Por que as 5 properties") | **FAIL** | PR #118 foi mergeado em 2026-08-09T00:10Z e o deploy **falhou** (`gh run 31285612236`). A conclusão do plano continua correta; a evidência citada, não |

### P1 — prova da cadeia de resolução (o ponto nº 1 do "Atenção pro Reviewer")

Não aceitei o argumento por leitura. Montei um `PropertySourcesPropertyResolver` com duas `PropertySource` — uma simulando `application-prod.properties`, outra simulando `finbot-prod-secrets` **sem** a chave `whatsapp_app_secret` — e resolvi `${whatsapp.app-secret:NAO_CONFIGURADO}` nos dois estados:

```
BEFORE fix  (prod='${whatsapp_app_secret}')
  -> THREW PlaceholderResolutionException: Could not resolve placeholder 'whatsapp_app_secret'
     in value "${whatsapp_app_secret}" <-- "${whatsapp.app-secret:NAO_CONFIGURADO}"

AFTER  fix  (prod='${whatsapp_app_secret:NAO_CONFIGURADO}')
  -> RESOLVED: 'NAO_CONFIGURADO'
```

A mensagem do estado "BEFORE" é **byte a byte a mesma** que o humano reportou em prod, seta `<--` inclusive. Isso fecha a questão em dois sentidos:

1. O diagnóstico do plano está certo. `PropertyPlaceholderHelper.parseStringValue` só usa o `defaultValue` quando `resolvePlaceholder(chave)` devolve `null`. Como `whatsapp.app-secret` **existe** no profile `prod`, o resolver devolve o literal `${whatsapp_app_secret}`, descarta o default do `@Value` e **recursa** sobre o valor devolvido. É nessa recursão que não há default — e é ali que morre.
2. A correção está no nível certo. Com o default dentro do placeholder aninhado, a recursão encontra o separador `:`, cai no default e devolve `NAO_CONFIGURADO`. Não há exceção.

Manter os defaults nos `@Value` é inofensivo e útil fora do profile `prod` — mas é **irrelevante** para o bug em questão, exatamente como o plano afirma.

### P4 — a allow-list, validada contra uma fonte autoritativa

Não avaliei a allow-list por plausibilidade. Fui buscar o último commit de `main` que produziu um deploy **verde** (`gh run list` → PR #68, 2026-05-27; commit `042deef`) e extraí os placeholders do `application-prod.properties` daquele ponto:

```
${admin_api_key}  ${db_host}  ${db_password}  ${db_username}
${jwt_secret}     ${keystore_password}        ${telegram_token}
```

São **exatamente** os 7 da constante `SECRETS_OBRIGATORIOS`, nem um a mais nem a menos. Como aquele boot subiu em prod, os 7 estão comprovadamente populados em `finbot-prod-secrets` — o que também derruba a preocupação de que o deploy vá simplesmente falhar na próxima chave. O único delta de placeholders entre `042deef` e a branch atual são as 5 `whatsapp.*`, todas agora com default. A allow-list está correta e nenhum placeholder foi rotulado errado.

---

## 1. Análise de código

### Veredito de código: **aprovado com observações**

A correção está certa, é mínima e ataca a causa raiz em vez do sintoma. O comentário no `application-prod.properties` explica o *porquê* do default morar ali — exatamente o tipo de contexto que impede a reincidência do erro do FIX-001. O teste de regressão é a melhor parte da entrega: é puro (sem contexto Spring, sem AWS, 13 ms), lê o recurso real de produção e generaliza para toda a classe de bug, não só para a ocorrência do WhatsApp. As mensagens de asserção dizem ao próximo dev o que fazer.

O que impede o "aprovado" limpo é uma afirmação de segurança que o plano faz, que o comentário do properties repete, e que **não é verdadeira** (Observação 1) — mais um conjunto de gates de processo que ainda não foram cumpridos (Observações 2 e 3).

### Observações materiais

**Observação 1 — a sentinela é uma chave HMAC pública; o gate de assinatura fica contornável**
- **O quê:** com o sentinela ativo, `MetaSignatureValidator` calcula HMAC-SHA256 usando a string literal `NAO_CONFIGURADO` como chave. Essa string está versionada no repositório (`application-prod.properties:41` e `MetaSignatureValidator.java:20,24`). Qualquer um que a conheça assina um corpo arbitrário e `isValid()` devolve **`true`**.
- **Onde:** `financas_bot_telegram/src/main/java/.../adapters/in/whatsapp/security/MetaSignatureValidator.java:24-53`; afirmação contrária em `application-prod.properties:34-35` e no plano §"Comportamento resultante" / §"Riscos".
- **Por quê importa:** o plano vende o estado como "canal inerte: POST sempre 200 silencioso — HMAC com chave sentinela nunca casa". Isso vale para tráfego legítimo da Meta, não para um atacante. O que fica alcançável por parte não autenticada, em `/webhook/whatsapp` (exposto — `JwtAuthenticationFilter.java:49` libera `/webhook*`):
  - desserialização Jackson de JSON controlado pelo atacante;
  - o laço de `value.statuses()` loga em `INFO` campos controlados pelo atacante **sem lançar exceção** → injeção/inundação de log sem limite por requisição (`WhatsAppWebhookController.java:95-98`);
  - `autorizarUsuario` lança `UnauthorizedUserException` → `GlobalWhatsAppExceptionHandler:34-42` chama `senderService.enviarTexto(waId, ...)` → **requisição HTTPS de saída para o graph.facebook.com disparada por terceiro não autenticado**, com o destinatário sob controle dele.

  Nada disso escreve no domínio (a lista `allowed-wa-ids` vazia barra, ver Observação nenhuma/P3), então não é escalação de privilégio. Mas é amplificação de saída + ruído de log alcançáveis remotamente, e o texto atual afirma que não existem. Note que o comentário foi gravado **dentro de um arquivo de produção**: quem ler depois vai confiar nele.
- **Sugestão (corrigir agora — o arquivo já está sendo tocado, são ~4 linhas):** falhar fechado em vez de calcular HMAC com chave conhecida. Guardar o booleano da sentinela no construtor e, em `isValid()`, `return false` de saída. Isso passa a **garantir** o comportamento que o plano já promete, em vez de descrevê-lo torto. Independentemente disso, **corrigir o texto** do comentário em `application-prod.properties:35` — de "POST nunca valida a assinatura" para algo como "POST descarta tudo silenciosamente porque nenhum `wa_id` está autorizado".

**Observação 2 — status report ausente; nada commitado na branch**
- **O quê:** `docs/sprints/03-folha-pagamento/status/FIX-006-*.md` não existe. `git log develop..HEAD` está vazio — as três alterações estão apenas na árvore de trabalho, sem commit.
- **Onde:** `docs/sprints/03-folha-pagamento/status/`; `git status --porcelain`.
- **Por quê importa:** critério de aceitação explícito do plano (§Critérios, último item) e gate obrigatório do `PRE-MERGE-CHECKLIST.md` ("Status report presente… com frontmatter válido"). Sem commit não há PR, e o gate `territorio` (`git diff --name-only origin/develop...HEAD`) não tem o que medir.
- **Sugestão:** corrigir agora — commitar apenas os 3 arquivos do backend e escrever o status report a partir de `docs/templates/_TEMPLATE-status.md` (`testes_total: 371`, `testes_novos: 2`).

**Observação 3 — `.claude/agents/backend.md` alterado, fora do escopo e fora do território**
- **O quê:** `model: inherit` → `model: opus`. Não tem relação com FIX-006.
- **Onde:** `.claude/agents/backend.md:5`.
- **Por quê importa:** `.claude/` não está entre os caminhos de `claude-back` no `PRE-MERGE-CHECKLIST.md` §Territórios (`financas_bot_telegram/`, `infra/`, `finbot.service`, `.github/workflows/`, mais `docs/` e arquivos da raiz). Se entrar no commit, o gate `territorio` fica divergente e a mudança de configuração viaja escondida dentro de um hotfix de produção.
- **Sugestão:** não commitar junto. Se a troca for desejada, vai em mudança própria com o humano ciente.

**Observação 4 — lacunas de cobertura do próprio guard (nenhuma delas é falso negativo de boot no arquivo atual)**
- **O quê:** testei o regex adversarialmente mutando o artefato de build e re-executando o teste. Resultados:

  | Forma injetada em `whatsapp.app-secret` | O teste pega? |
  |---|---|
  | `${whatsapp_app_secret}` (a inversão pedida) | **sim** — 2 testes falham |
  | `${whatsapp_app_secret:${fallback_secret}}` (default aninhado sem default) | **sim** |
  | `${whatsapp_app_secret:x}${extra_key}` (dois placeholders na mesma linha) | **sim** |
  | `${outer.${env}.secret}` (placeholder dentro da chave) | **sim** |
  | `${whatsapp_app_secret:}` (default vazio) | **não** |

  O último não é falso negativo de *boot* — `${x:}` resolve para `""` e o boot sobe (confirmado na reprodução do Spring). É uma lacuna **semântica**: `whatsapp.app-secret=${whatsapp_app_secret:}` passaria no guard e daria chave HMAC vazia, que em `SecretKeySpec` lança `IllegalArgumentException: Empty key` — e essa exceção **não** está no `catch` de `isValid()` (`MetaSignatureValidator.java:49` só pega `NoSuchAlgorithmException | InvalidKeyException`), então escaparia para o `handleAny`. Cenário hipotético, não presente no código.

  Duas outras lacunas reais: (a) o guard só varre `application-prod.properties`; `application.properties` (base, também ativo em `prod`) não é coberto — hoje não tem nenhum `${...}`, só literais `CHANGE_ME`, então não há exposição atual, mas nada impede a regressão; (b) o WARN novo do construtor não é asserido por nenhum teste — `MetaSignatureValidatorTest` sempre constrói com `"test-app-secret"`, então o ramo novo nunca executa na suíte.
- **Onde:** `ProdPropertiesPlaceholderDefaultsTest.java:36`; `MetaSignatureValidatorTest.java:15-21`.
- **Sugestão:** registrar como pendência técnica, não bloquear. Se a Observação 1 for acatada, o teste do ramo da sentinela deixa de ser cosmético (passa a asseverar `isValid(...) == false` com o sentinela, que é comportamento, não log) e aí vale como caso de teste de verdade.

**Observação 5 — a premissa P7 do plano está apoiada em `STATE.md` desatualizado, e prod está caído agora**
- **O quê:** o plano §"Por que as 5 properties" cita `STATE.md:96` ("PR `develop → main`: não aberto ainda") para concluir que nenhuma property `whatsapp.*` chegou em prod. O PR #118 `develop → main` foi mergeado em 2026-08-09T00:10Z e o deploy falhou (`sudo systemctl is-active finbot` → `activating`, exit 3).
- **Onde:** `docs/STATE.md:96`; run `31285612236` do workflow `deploy.yml`.
- **Por quê importa:** a **conclusão** do plano continua correta (as `whatsapp.*` nunca subiram com sucesso — o último deploy verde é de 2026-05-27), então a decisão de cobrir as 5 está bem fundamentada por outro caminho. Mas o enquadramento muda: FIX-006 não "bloqueia um deploy futuro", ele **restaura produção, que está em crash-loop desde ontem**. Isso mexe na prioridade e no risco de segurar o merge por refinamento.
- **Sugestão:** planner atualiza `STATE.md` (o PR foi aberto, mergeado e o deploy falhou) e trata FIX-006 como restauração de prod.

### Smells arquiteturais

Não aplicável — o diff não cruza camadas. Toca configuração declarativa, um log de construtor num adapter de entrada e um teste. Nenhum import novo, nenhuma assinatura alterada, `port`/`application`/`domain` intocados.

---

## 2. Gates verificados contra a realidade

Tudo abaixo foi **executado**, não lido. Divergências marcadas.

| Gate | Autorrelato | Reviewer reproduziu | Divergência? |
|---|---|---|---|
| `build` | ok | **ok** — `mvn -B package -DskipTests`, exit 0, `financas-bot-telegram-0.0.1-SNAPSHOT.jar` (81 MB) gerado | não |
| `testes` (unit) | 371, 0 falhas | **ok** — `mvn -B test -Dtest='!*IntegrationTest'`, `Tests run: 371, Failures: 0, Errors: 0`, `BUILD SUCCESS`, exit 0 | não |
| `testes_novos` | 2 | **ok** — `ProdPropertiesPlaceholderDefaultsTest`: `Tests run: 2, Failures: 0` | não |
| `testes` (full) | 419 / 48 erros, todos ambientais | **ok** — `mvn -B test -Dtest='*IntegrationTest'` → `Tests run: 48, Errors: 48`, todos `ExceptionInInitializerError → IllegalStateException: Could not find a valid Docker environment` | não |
| inversão do default | falha | **ok** — reproduzida (ver abaixo) | não |
| `lint` | na | na (backend sem linter) | não |
| `branch_convencao` | ok | **ok** — `fix/006-whatsapp-defaults-no-properties` casa `^fix/\d{3}-`; `origin/develop` é ancestral | não |
| `territorio` | ok | **divergente** — `.claude/agents/backend.md` na árvore de trabalho (Observação 3) | **sim** |
| status report | — | **divergente** — arquivo não existe (Observação 2) | **sim** |
| commits na branch | — | **divergente** — `git log develop..HEAD` vazio (Observação 2) | **sim** |

### Inversão do default — reproduzida de forma independente

Não confiei no relato. Reproduzi **sem alterar nenhum arquivo versionado**: mutei o artefato de build `target/classes/application-prod.properties` (ignorado pelo git, confirmado com `git check-ignore`) e rodei `mvn -o surefire:test -Dtest=ProdPropertiesPlaceholderDefaultsTest`, que não recopia recursos. Com `whatsapp.app-secret=${whatsapp_app_secret}`:

```
[ERROR] Tests run: 2, Failures: 2, Errors: 0
  placeholdersSemDefault_apenasSecretsObrigatorios:85
    [placeholders sem default em application-prod.properties derrubam o boot ...]
    Expecting : "whatsapp_app_secret", ["whatsapp_app_secret"]
  propriedadesWhatsApp_temDefaultNoPlaceholder:101
    [whatsapp.app-secret='${whatsapp_app_secret}' precisa de default no placeholder ...]
    Expecting value to be false but was true
[INFO] BUILD FAILURE
```

O guard pega o bug e a mensagem diz o que fazer. Ponto nº 3 do "Atenção pro Reviewer": **confirmado**. Arquivo restaurado ao final; `diff` entre `src/main/resources` e `target/classes` limpo e `git status` inalterado.

### Ponto nº 2 do "Atenção pro Reviewer" — as 5, não as 3

Confirmado, e as 2 além das 3 do erro original eram as mais perigosas: `WhatsAppMessageSenderService:32-33` e `WhatsAppMediaDownloaderService:26` injetam `whatsapp.access-token` / `whatsapp.phone-number-id` com `@Value` **sem default nenhum**. Sem a correção no properties, elas quebrariam o boot num segundo deploy assim que `MetaSignatureValidator` parasse de ser o primeiro a falhar. `whatsapp.allowed-wa-ids` também precisava do tratamento apesar de já ter `:` no `@Value` do controller — cai na mesma armadilha da chave existente com valor aninhado.

### Ponto 4 do dispatch — `allowed-wa-ids` vazio

Executei a conversão real do `DefaultConversionService` (String → `List<String>`), a mesma que o `@Value` usa:

```
convert('')        -> size=0 []
convert('5511999') -> size=1 [5511999]
StringUtils.commaDelimitedListToStringArray('').length = 0
```

Ou seja: lista **genuinamente vazia**, não `[""]`. O comentário em `WhatsAppWebhookController.java:52` ("Spring converte string vazia em lista com elemento vazio `[""]`") está factualmente errado, mas o filtro `isBlank` que ele justifica é inofensivo e ainda cobre `"  "` → `[""]`... na prática `[]`. **Sem bypass de autorização:** `allowedWaIds.contains(waId)` sobre lista vazia é sempre `false`, então todo `wa_id` — inclusive um `from: ""` forjado — cai em `UnauthorizedUserException`. Correção do comentário é cosmética; anoto sem exigir.

### P5 — por que os 48 erros não podem ser culpa desta mudança

Três evidências independentes, nenhuma delas "rodei na árvore limpa e deu igual":
1. O daemon Docker está fora do ar nesta máquina — `docker info` responde `Server: ERROR: ... open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`.
2. `AbstractIntegrationTest` sobe um `MySQLContainer` num bloco `static {}`; a falha é `ExceptionInInitializerError` no `<clinit>`, ou seja, **antes** de qualquer bean ou property ser tocado.
3. As 12 classes de IT usam `@ActiveProfiles("integration-test")` e o datasource vem de `@DynamicPropertySource`. Elas **nunca carregam** `application-prod.properties`. Uma mudança confinada a esse arquivo, a um log de construtor e a um teste novo não tem caminho causal até "Could not find a valid Docker environment".

O raciocínio do autorrelato se sustenta.

---

## 3. Roteiro de validação manual

**Pré-condições:** merge em `develop` → PR `develop → main` → deploy verde.

| # | Ação | Esperado | Resultado |
|---|---|---|---|
| 1.1 | `sudo systemctl is-active finbot` após o deploy | `active` (hoje: `activating`, crash-loop) | |
| 1.2 | `journalctl -u finbot` no boot | WARN `whatsapp.app-secret nao configurado — canal WhatsApp inerte` presente; nenhum `PlaceholderResolutionException` | |
| 1.3 | `GET /webhook/whatsapp?hub.mode=subscribe&hub.verify_token=xxx&hub.challenge=foo` | **403** | |
| 1.4 | Fluxo Telegram ponta a ponta (pedido + comprovante) | inalterado — a regressão que importa é o canal que já estava em produção | |

---

## 4. Resultado consolidado

| Item | Resultado |
|---|---|
| Análise de código | aprovado com observações |
| Premissas (P1–P7) | 5 pass, 2 fail (P6 material, P7 documental) |
| Gates contra a realidade | **divergente** (território, status report, commits) |
| Roteiro manual | pendente (só verificável em prod) |
| **Veredito final** | **ajustar antes de mergear** |

Nenhuma premissa **técnica** falhou: a correção faz o que promete e o guard pega a regressão. O que trava é (a) uma afirmação de segurança falsa gravada em arquivo de produção, (b) gates de processo não cumpridos.

**Ordem sugerida, considerando que prod está caído:**
1. Corrigir o texto do comentário em `application-prod.properties:34-35` (obrigatório — é 1 linha e hoje afirma o contrário do que acontece).
2. Aplicar o fail-closed em `MetaSignatureValidator.isValid()` (~4 linhas). Se o humano preferir restaurar prod primeiro, aceito virar FIX-007 de prioridade **alta**, mas registrado como decisão consciente, não esquecido.
3. Reverter/segregar `.claude/agents/backend.md`.
4. Commitar os 3 arquivos do backend + escrever o status report.
5. PR `fix/006-… → develop`, depois `develop → main`.

Nada disso invalida o trabalho já feito — os 4 primeiros itens somam menos de 15 minutos.

---

## 5. Skills — feedback loop

**Skills eficazes:** nenhuma registrada (`skills_dispatched: []` no plano).

**Skills com gap:**
- `seguranca-backend` — a Observação 1 é exatamente o alvo dessa skill: sentinela adotada como chave criptográfica sem avaliar que constante versionada é conhecimento público. O plano chegou a raciocinar sobre o comportamento resultante ("HMAC nunca casa") e errou justamente por não modelar o adversário. Um dispatch com essa skill provavelmente teria produzido o fail-closed junto com o WARN.

---

## 6. Para o planner

- **Débito técnico a consolidar em `docs/PENDENCIAS-TECNICAS.md`:**
  1. `MetaSignatureValidator` fail-closed com sentinela ativa (vira FIX-007 se não entrar no FIX-006).
  2. Guard de placeholders não cobre `application.properties` (base, ativo em `prod`) — hoje sem exposição, mas sem rede de proteção.
  3. `${x:}` (default vazio) passa no guard; para `app-secret` daria chave HMAC vazia → `IllegalArgumentException: Empty key` fora do `catch` de `isValid()`.
  4. Ramo do WARN da sentinela sem teste em `MetaSignatureValidatorTest`.
  5. Comentário factualmente errado em `WhatsAppWebhookController.java:52` sobre `[""]`.
  6. Migrar os `whatsapp.*` para `@ConfigurationProperties` com validação (já declarado fora de escopo no plano).
- **`STATE.md` desatualizado:** linha 96 diz que o PR `develop → main` não foi aberto. Foi — PR #118, mergeado 2026-08-09, deploy falho. **Produção está em crash-loop.** Atualizar antes do próximo planejamento; qualquer plano que se apoie nessa linha herda a premissa furada.
- **Pré-condição da BE-20:** confirmada e reforçada. Popular as 4 chaves em `finbot-prod-secrets` é pré-requisito de ativação; enquanto não, o WARN no boot é o sinal.
- **Ordem de merge:** FIX-006 não tem dependência de nenhuma task da sprint 03 (nenhuma outra toca `application-prod.properties`). Pode ir direto para `develop` assim que os ajustes da §4 estiverem prontos.

---

## 7. QA — Fluxos automatizados

Não aplicável: `fluxos_qa: []` no plano. Mudança declarativa de configuração; a validação real é o boot em prod, não automatizável pela suíte. `veredito_qa: nao_aplicavel`.

---

## 8. Rodada 2 — revisão do delta (fail-closed da Observação 1)

**Modo:** `delta-review`. Escopo: só o que mudou depois da rodada 1 — `MetaSignatureValidator.java`, os 2 testes novos em `MetaSignatureValidatorTest.java` e o comentário de `application-prod.properties`.

**Veredito do delta: aprovado.** A guarda faz exatamente o que promete e está provada por mutação. Um achado **novo** aparece no próprio texto corrigido (Observação 6) e o veredito final continua `ajustar antes de mergear` — agora por 2 itens, não 5.

### 8.1 A guarda fecha o buraco? (pergunta 1) — **sim, verificado**

`MetaSignatureValidator.isValid()` linhas 38-41: o `return false` é a **primeira instrução executável** do método, antes do `SecretKeySpec`/`Mac` e antes de qualquer `catch`. Não sobra caminho:

| Elo da cadeia de ataque da Observação 1 | Estado agora |
|---|---|
| `objectMapper.readValue(rawBody, …)` — desserialização de JSON do atacante | **inalcançável** — `WhatsAppWebhookController:76-79` devolve 200 e sai antes da linha 83 |
| laço `value.statuses()` logando campos do atacante em INFO (`:95-98`) | **inalcançável** |
| `autorizarUsuario` → `UnauthorizedUserException` → `GlobalWhatsAppExceptionHandler:34-42` → `senderService.enviarTexto(waId, …)` (chamada HTTPS de saída disparada por terceiro) | **inalcançável** |

Confirmei também que **existe um único call site** (`grep` por `signatureValidator` em `src/main/java`: só `WhatsAppWebhookController:76`), então não há rota alternativa que pule a guarda. O POST continua exposto sem autenticação (`JwtAuthenticationFilter.shouldNotFilter` libera `path.startsWith("/webhook")`) — é a guarda que passou a segurar, e não mais a esperança de que o HMAC não case.

Efeito colateral avaliado e aceito: um chamador anônimo agora provoca 2 linhas de WARN por requisição (`MetaSignatureValidator:39` + `WhatsAppWebhookController:77`) em vez de 1. É ruído de string fixa, sem conteúdo controlado pelo atacante — estritamente melhor que o estado anterior, em que ele conseguia injetar conteúdo próprio em INFO.

### 8.2 Os testes novos morrem se a guarda voltar atrás? (pergunta 2) — **1 sim, 1 não**

Verificado **por mutação, não por leitura**. Para não tocar a árvore do implementador, copiei `pom.xml` + `src/` para o scratchpad e mutei a cópia (`git status` do repo permaneceu inalterado).

| Mutante | `secretSentinela_assinaturaCalculadaComSentinela_…` | `secretSentinela_qualquerAssinatura_…` | Testes pré-existentes |
|---|---|---|---|
| **M1** — remover o bloco `if (!secretConfigurado) return false` de `isValid()` (= reverter o fix) | **FALHA** — `MetaSignatureValidatorTest:83 Expecting value to be false but was true` | **passa** | passam |
| **M2** — `secretConfigurado = false` sempre (guarda fecha demais) | passa | passa | **FALHAM 2** — `assinaturaCorreta_retornaTrue`, `bodyVazio_assinaturaCorreta_retornaTrue` |

Leitura dos resultados:

- `secretSentinela_assinaturaCalculadaComSentinela_retornaFalse` é **evidência de verdade**: mata M1, é o ataque exato descrito na Observação 1 (HMAC genuinamente correto com a chave pública) e a fixture não empresta nada do código sob teste — usa o literal `"NAO_CONFIGURADO"` digitado no teste, não a constante privada. Se alguém trocar o valor da sentinela só na produção, o teste acusa.
- `secretSentinela_qualquerAssinatura_retornaFalse` é **vacuoso em relação à guarda**: sobrevive a M1 porque as duas asserções já eram verdadeiras antes do fix — HMAC com `APP_SECRET` nunca casa com HMAC de chave sentinela, e o header `null` já morria no `if` pré-existente da linha 42. Não é nocivo (é um teste de fronteira legítimo), mas **não conte com ele como cobertura do fail-closed**; se um dia o outro for removido, a suíte fica verde com o buraco aberto. Não exijo mudança.
- M2 mostra que o caminho CONFIGURADO tem rede de proteção: fechar demais quebra 2 testes na hora.

### 8.3 Regressão no caso CONFIGURADO? (pergunta 3) — **nenhuma**

O diff no caminho do secret real acrescenta exatamente uma avaliação booleana que resulta `false`; daí para baixo o código é idêntico byte a byte (`git diff` do arquivo não toca nada entre as linhas 42 e 61). M2 confirma pelo lado oposto. A suíte inteira roda verde e `WhatsAppWebhookControllerTest` mantém as 9 asserções originais sem alteração.

### 8.4 O comentário corrigido está exato? (pergunta 4) — **metade sim, metade não** → Observação 6

**Observação 6 — o comentário ainda afirma "handshake GET retorna 403", e isso é falso pela mesma razão que motivou a Observação 1**
- **O quê:** a segunda frase agora está **certa** ("a sentinela é um literal versionado, portanto não é segredo — quem rejeita a assinatura é a guarda explícita em `MetaSignatureValidator`") — é uma descrição fiel do código. A primeira frase, não: `whatsapp.verify-token` cai na **mesma** sentinela pública e `WhatsAppWebhookController.handshake` (`:62`) compara com `verifyToken.equals(token)` sem nenhuma guarda equivalente.
- **Verificado por execução** (probe descartável na cópia do scratchpad, não commitado):

  ```
  handshake("subscribe", "NAO_CONFIGURADO", "<script>alert(1)</script>")
    -> PROBE_HANDSHAKE_STATUS=200 OK
    -> PROBE_HANDSHAKE_BODY=<script>alert(1)</script>

  MockMvc standaloneSetup, GET /webhook/whatsapp?hub.mode=subscribe
      &hub.verify_token=NAO_CONFIGURADO&hub.challenge=<script>alert(1)</script>
      com Accept: text/html
    -> MVC_STATUS=200
    -> MVC_CTYPE=text/html;charset=ISO-8859-1
    -> MVC_BODY=<script>alert(1)</script>
  ```

- **Por quê importa:** (a) o comentário mora num arquivo de produção e vende como garantia (`403`) algo que o código não garante — é o mesmo defeito de documentação que a rodada 1 apontou, apenas deslocado uma property para o lado; (b) o eco do `hub.challenge` volta com `Content-Type: text/html` e corpo sem escape, ou seja, uma primitiva com forma de **XSS refletido** na origem da API, alcançável sem autenticação enquanto o verify-token for a sentinela. O cookie de sessão é `httpOnly(true)` (`CookieFactory:25`), então não há roubo direto de token — mas script executando na origem faz requisição autenticada com o cookie anexado automaticamente. Severidade **média**, não crítica.
- **Nuance de escopo, e por que não é "pré-existente":** antes do FIX-006 o profile `prod` **não conseguia** chegar nesse estado — o boot morria em `${whatsapp_verify_token}` sem default. É esta mudança que torna "verify-token == literal público" um estado alcançável em produção. Por isso não aceito classificar como débito de outra task.
- **Sugestão, em ordem de preferência:**
  1. **Simétrico ao que já foi feito (~4 linhas, torna o comentário verdadeiro):** guardar o booleano da sentinela também no controller e devolver 403 incondicional no handshake quando `verify-token` não estiver configurado. Fecha o eco e faz a frase existente virar fato.
  2. **Mínimo aceitável (1 linha):** corrigir a frase — trocar "handshake GET retorna 403" por "handshake GET só responde 403 para quem não conhece a sentinela versionada; enquanto o verify-token não for populado, o eco do `hub.challenge` é alcançável".

  Não aceito a terceira opção (deixar como está): o texto sob revisão afirma o oposto do comportamento executado acima.

### 8.5 Gates re-executados nesta rodada

Tudo executado agora, não relido do autorrelato.

| Gate | Autorrelato do implementador | Reviewer reproduziu | Divergência? |
|---|---|---|---|
| `testes` (unit) | 373, 0 falhas, exit 0 | **ok** — `mvn -B -f financas_bot_telegram/pom.xml test -Dtest='!*IntegrationTest'` → `Tests run: 373, Failures: 0, Errors: 0`, `BUILD SUCCESS`, exit 0 | não |
| `MetaSignatureValidatorTest` 7 → 9 | 9 | **ok** — `Tests run: 9, Failures: 0` | não |
| `ProdPropertiesPlaceholderDefaultsTest` | 2, verde | **ok** — `Tests run: 2, Failures: 0` | não |
| `WhatsAppWebhookControllerTest` intacto | 9, verde | **ok** — `Tests run: 9, Failures: 0` | não |
| `build` | `mvn package -DskipTests` exit 0 | **ok** — `BUILD SUCCESS`, exit 0, `spring-boot:repackage` executado, jar de 81 MB regenerado | não |
| mutação M1/M2 | não relatado | **executado** (§8.2) | — |
| status report | "escrevendo agora" | **ainda ausente** em `docs/sprints/03-folha-pagamento/status/` | **sim** |
| commits na branch | — | **`git log develop..HEAD` continua vazio** — tudo na árvore de trabalho | **sim** |

### 8.6 Disposições que o implementador pediu para confirmar

| Item | Disposição proposta | Reviewer |
|---|---|---|
| Status report ainda não escrito | escrever agora, com o veredito final | **de acordo.** Continua sendo gate do `PRE-MERGE-CHECKLIST.md`. Números conferidos para citar: `testes_total: 373`, `testes_novos: 4` (2 do `ProdProperties…` + 2 da sentinela) |
| `.claude/agents/backend.md` — não commitar e não reverter | manter na árvore, fora do commit | **de acordo, é a chamada certa.** O diff é 1 linha (`model: inherit` → `model: opus`, linha 5), não é território de `claude-back` e não é seu para descartar. Cuidado operacional: commitar com `git add <paths>` explícitos, nunca `git add -A`/`.`. Como fica sem commit, o gate `territorio` (`git diff --name-only origin/develop...HEAD`) sai limpo |
| `WhatsAppWebhookController:52` (`[""]`) — débito | fora de escopo | **de acordo.** Comentário factualmente errado (comprovei na rodada 1 rodando o `DefaultConversionService`: `convert('') -> size=0`), mas o filtro `isBlank` é inofensivo e não há bypass. Débito, não bloqueio |
| `application.properties` base fora do guard — débito | fora de escopo | **de acordo.** Hoje o arquivo só tem literais `CHANGE_ME`, nenhum `${…}`; risco é de regressão futura, não exposição atual |
| `STATE.md:96` / PR #118 — registrar no status report | contexto real | **de acordo**, com os dados exatos abaixo |

### 8.7 Fatos de produção para citar no status report (verificados agora)

Use este texto; ele separa o que está no log do CI do que veio do `journalctl` relatado pelo humano.

- **Run que falhou:** `31285612236`, workflow `Deploy to Production` (`.github/workflows/deploy.yml`), evento `push` em `main`, commit `Merge pull request #118 from SSteringS/develop`, iniciado **2026-08-09T00:10:49Z**, duração **2m01s**, conclusão **failure**.
- **Passo que quebrou e sintoma literal no log do CI:** o step de deploy via SSH termina com `sudo systemctl restart finbot && sleep 20 && sudo systemctl is-active finbot`; a saída em `2026-08-09T00:12:47Z` foi **`activating`**, seguida de `##[error]Process completed with exit code 3`. `activating` (e não `active`) 20 s depois do restart = serviço em **crash-loop**, sendo re-executado pelo systemd.
- **Causa raiz, com a procedência correta:** o log do CI **não** contém o stack trace — ele só mostra `activating` / exit 3. A exceção veio do `journalctl` relatado pelo humano e foi **reproduzida byte a byte** por mim na rodada 1 com Spring Framework 6.2.6:
  `PlaceholderResolutionException: Could not resolve placeholder 'whatsapp_app_secret' in value "${whatsapp_app_secret}" <-- "${whatsapp.app-secret:NAO_CONFIGURADO}"`.
  Ao citar, deixe claro que o CI evidencia o crash-loop e a reprodução local evidencia o motivo.
- **Último deploy verde:** run `26519270281`, PR #68, **2026-05-27T14:57:45Z** — ou seja, produção está sem deploy bem-sucedido desde 27/05 e **caída desde 2026-08-09T00:12Z**.
- **`docs/STATE.md:96`** ainda diz `**PR develop → main** (deploy sprint 02): não aberto ainda`. Está **desatualizado**: foi aberto, mergeado (#118) e o deploy falhou. Enquadramento correto do FIX-006: **restauração de produção**, não prevenção de um deploy futuro.

### 8.8 Débito técnico consolidado após o delta

Atualiza a lista da §6:

1. ~~`MetaSignatureValidator` fail-closed~~ — **resolvido neste delta**, provado por mutação.
2. **NOVO — handshake GET com verify-token sentinela responde 200 e ecoa `hub.challenge` como `text/html`** (Observação 6). Se não entrar no FIX-006, vira FIX-007 de prioridade alta, e o comentário precisa ser corrigido de qualquer forma.
3. **NOVO — `secretSentinela_qualquerAssinatura_retornaFalse` sobrevive à mutação** que remove a guarda; é teste de fronteira, não de regressão. Registrar para não ser confundido com cobertura do fail-closed.
4. Guard de placeholders não cobre `application.properties` (base, ativo em `prod`).
5. `${x:}` (default vazio) passa no guard e é tratado como *configurado* pela guarda nova (`"".equals("NAO_CONFIGURADO")` é `false`). **Agora confirmado por execução**, não mais hipotético: `new MetaSignatureValidator("")` → `isValid` lança `java.lang.IllegalArgumentException: Empty key`, que **não** está no `catch` de `isValid()` (`NoSuchAlgorithmException | InvalidKeyException`) e escapa para `handleAny`. Continua sendo estado de configuração hipotético.
6. Comentário factualmente errado em `WhatsAppWebhookController:52` sobre `[""]`.
7. Migrar os `whatsapp.*` para `@ConfigurationProperties` com validação.
8. `STATE.md:96` desatualizado (planner).

### 8.9 Veredito final após o delta

| Item | Resultado |
|---|---|
| Código do delta (guarda + testes) | **aprovado** — correto, mínimo, provado por mutação |
| Regressão no caso configurado | **nenhuma** |
| Premissa P6 da rodada 1 (`fail`) | **corrigida** para o POST; **permanece falsa para o GET** (Observação 6) |
| Gates re-executados | build e testes **ok**; status report e commits **divergentes** |
| **Veredito final** | **ajustar antes de mergear** — 2 itens |

Falta, e só falta:
1. Observação 6 — guarda no handshake (~4 linhas, preferido) **ou** correção da frase do comentário (1 linha, mínimo aceitável). Não é opcional: o texto atual afirma o contrário do comportamento executado.
2. Status report + commit dos 4 arquivos do backend, depois PR `fix/006-… → develop` e `develop → main`.

Com prod caída desde 09/08 00:12Z, se o humano preferir mergear com a opção 2 (correção só do texto) e mandar a guarda do handshake como FIX-007, aceito — desde que registrado como decisão consciente no status report, não como esquecimento.

---

## 9. Rodada 3 — confirmação final (Observação 6 fechada pela opção 1)

**Modo:** `delta-review`. Escopo: guarda de handshake em `WhatsAppWebhookController`, o teste novo `handshake_verifyTokenSentinela_naoRefleteChallenge_retorna403`, o comentário reescrito em `WhatsAppWebhookController:61` e o status report — que passou a existir.

**Veredito da rodada 3: aprovado.** Nenhuma observação aberta. O implementador escolheu a opção 1 (guarda explícita), que era a preferida.

### 9.1 O teste novo é não-vacuoso? (pergunta 1) — **sim, provado por 3 mutantes**

Verificado **por mutação**, não por leitura. Copiei `pom.xml` + `src/` para o scratchpad e mutei a cópia; `git status` do repo permaneceu inalterado do início ao fim da rodada.

| Mutante | Efeito | Resultado |
|---|---|---|
| **M1** — remover o bloco `if (!verifyTokenConfigurado) { … return 403; }` de `handshake()` (= reverter o fix) | guarda ausente | **MATA** — `handshake_verifyTokenSentinela_naoRefleteChallenge_retorna403:71` → `expected: 403 FORBIDDEN but was: 200 OK`. Os outros 9 testes continuam verdes: o teste novo é o **único** que pega |
| **M2** — `this.verifyTokenConfigurado = false` sempre | guarda fecha demais | **MATA** — `handshake_tokenCorreto_retorna200ComChallenge:48` → `expected: 200 OK but was: 403 FORBIDDEN`. O caminho configurado tem rede |
| **M3** — guarda **movida para depois** do bloco `"subscribe".equals(mode) && verifyToken.equals(token)` | guarda presente, mas tarde demais | **MATA** — mesmo erro de M1 (`200 OK`). O teste não afere só a *existência* da guarda, afere a **ordem** |

Baseline restaurado ao final: `Tests run: 10, Failures: 0, Errors: 0 — BUILD SUCCESS`.

M3 é o mutante que fecha a pergunta 2 pelo lado do teste: um refactor futuro que mantenha a guarda mas a coloque abaixo da comparação de token quebra a suíte na hora. Isso é o oposto do que aconteceu com `secretSentinela_qualquerAssinatura_retornaFalse` (§8.2), que sobrevivia à reversão. **Este teste não é vacuoso.**

Independência de fixture: o teste usa o literal `"NAO_CONFIGURADO"` digitado nas linhas 66 e 69 do arquivo de teste, **não** a constante `SENTINELA_NAO_CONFIGURADO` do código sob teste. Não há premissa compartilhada — se alguém mudar o valor da sentinela só no código, o teste acusa.

### 9.2 A guarda é cedo o bastante? (pergunta 2) — **sim, verificado estruturalmente e por HTTP**

**Estrutural.** `grep -rn "challenge" src/main/java` devolve **um único ponto de reflexão**: `WhatsAppWebhookController:79 return ResponseEntity.ok(challenge)`. Ele está dentro do `if` da linha 77, que só é alcançado depois do `return` da linha 75. Não existe segundo `@GetMapping("/webhook/whatsapp")` no projeto (`grep` por mapping confirma: só as linhas 65 e 85 desta classe). Logo não há caminho alternativo.

**Por HTTP.** Repeti o ataque exato da Observação 6 via `MockMvc standaloneSetup` (probe descartável, só na cópia do scratchpad, nunca commitado):

```
GET /webhook/whatsapp?hub.mode=subscribe&hub.verify_token=NAO_CONFIGURADO
    &hub.challenge=<script>alert(1)</script>     (Accept: text/html)
  -> PROBE_SENT_STATUS=403
  -> PROBE_SENT_CTYPE=null
  -> PROBE_SENT_BODY=[]   PROBE_SENT_LEN=0

GET … &hub.verify_token=qualquer&hub.challenge=PWNED   (verify-token = sentinela)
  -> PROBE_SENT2_STATUS=403   PROBE_SENT2_BODY=[]
```

Na rodada 2 esse mesmo request devolvia `200`, `Content-Type: text/html;charset=ISO-8859-1` e o corpo `<script>alert(1)</script>` verbatim. Agora: 403, **corpo de comprimento zero e sem `Content-Type`**. A primitiva de XSS refletido está fechada — não sobra nem o eco nem o cabeçalho que o tornava renderizável.

**Caso configurado, sem regressão** (mesmo probe, verify-token real):

```
verify_token correto,  hub.challenge=1158201444  -> 200, body=[1158201444]
verify_token=NAO_CONFIGURADO contra token real   -> 403, body=[]
```

O handshake legítimo da Meta continua funcionando idêntico. M2 confirma pelo lado oposto. O diff no caminho configurado acrescenta exatamente uma avaliação booleana que resulta `false`.

Efeito colateral aceito: com a sentinela ativa o GET agora emite 1 WARN de string fixa por requisição. Sem conteúdo controlado pelo atacante — antes ele conseguia refletir o próprio payload no corpo da resposta, o que era estritamente pior.

### 9.3 O comentário do `application-prod.properties` está exato agora? (pergunta 3) — **sim, as duas frases**

| Trecho (linhas 34-37) | Verdadeiro? | Base |
|---|---|---|
| "handshake GET retorna 403" | **sim** | `WhatsAppWebhookController:73-76` — 403 incondicional com sentinela; provado por M1/M3 e pelo probe HTTP acima |
| "POST nunca valida a assinatura (200 silencioso)" | **sim** | `MetaSignatureValidator:38-41` retorna `false` antes do HMAC → `WhatsAppWebhookController:91-93` devolve 200; provado por mutação na §8.2 |
| "A sentinela é um literal versionado, portanto não é segredo" | **sim** | `application-prod.properties:40-43` e `MetaSignatureValidator:20` / `WhatsAppWebhookController:36` |
| "quem rejeita a assinatura é a guarda explícita em `MetaSignatureValidator`, não o HMAC calculado com ela" | **sim** | descrição fiel do código |

A Observação 6 está **fechada**. Não sobrou afirmação falsa em arquivo de produção — que era o defeito que atravessou as rodadas 1 e 2.

**Nit sem severidade (não exijo mudança):** a segunda frase credita a recusa só ao `MetaSignatureValidator`, porque está escopada à *assinatura*. Um leitor apressado pode inferir que o 403 do handshake vem da comparação de token, e não de uma guarda. Se um dia o arquivo for tocado de novo, trocar por "as guardas explícitas em `MetaSignatureValidator` e `WhatsAppWebhookController`" fecha a última ambiguidade. Nada aqui é falso hoje.

### 9.4 Gates re-executados na rodada 3

Tudo executado agora.

| Gate | Autorrelato | Reviewer reproduziu | Divergência? |
|---|---|---|---|
| `testes` (unit) | 374, 0 falhas, exit 0 | **ok** — `mvn -B -f financas_bot_telegram/pom.xml test -Dtest='!*IntegrationTest'` → `Tests run: 374, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, exit 0 | não |
| `WhatsAppWebhookControllerTest` 9 → 10 | 10 | **ok** — `Tests run: 10, Failures: 0` | não |
| `testes_novos: 5` | 5 | **ok** — 2 (`ProdProperties…`, arquivo novo) + 2 (`MetaSignatureValidatorTest`, 7→9) + 1 (controller, 9→10). `git diff` dos testes modificados tem exatamente 3 linhas `+…@Test` | não |
| `build` | `mvn package -DskipTests` exit 0 | **ok** — `BUILD SUCCESS`, exit 0, `spring-boot:repackage` executado, jar de 81.150.307 bytes | não |
| mutação M1/M2/M3 | não relatado | **executado** (§9.1) | — |
| `branch_convencao` | ok | **ok** — `fix/006-whatsapp-defaults-no-properties` casa `^fix/\d{3}-`; `git merge-base --is-ancestor origin/develop HEAD` passa | não |
| `territorio` | ok | **não verificável ainda** — `git diff --name-only origin/develop...HEAD` sai **vazio** (nenhum commit). Ver §9.6 | **sim (formal)** |
| status report | presente | **ok** — existe em `docs/sprints/03-folha-pagamento/status/FIX-006-whatsapp-defaults-no-properties.md` | não |

**Cross-check aritmético independente do `testes_novos`.** O log do CI da run `31285612236` (base = `develop` antes do FIX-006, com Docker disponível) registra `Tests run: 417`. Localmente a árvore atual dá 374 unit + 48 IT = 422. `422 − 417 = 5`. O número bate por um caminho que não depende de nada que o implementador relatou. De quebra, isso **corrobora a P5 por uma quarta via**: os 48 ITs rodaram **verdes** no CI naquela run — os 48 erros locais são inequivocamente falta de daemon Docker nesta máquina.

### 9.5 Revisão do status report (pergunta 4)

**Frontmatter — schema válido.** Conferido campo a campo contra `docs/templates/_TEMPLATE-status.md` e `docs/runbooks/PRE-MERGE-CHECKLIST.md`:

| Campo | Valor | Válido? |
|---|---|---|
| `task` | `FIX-006` | ok — casa `^(BE\|FE\|DEP\|FIX\|HOTFIX\|EVO\|CI\|WF\|QA)-\d{3}[a-z]?$` |
| `data` / `branch` / `responsavel` | `2026-08-09` / `fix/006-…` / `claude-back` | ok |
| `estado` | `parcial` | ok — ver análise abaixo |
| `build` / `lint` / `testes` | `ok` / `na` / `ok` | ok — `lint: na` correto, backend não tem linter (checklist §Gates) |
| `testes_total` / `testes_novos` | `374` / `5` | **ok — ambos reproduzidos** (§9.4) |
| `cobertura_pct` | `na` | aceitável — **não há JaCoCo no `pom.xml`** (`grep jacoco` → nada), então o comando que o checklist sugere não existe neste projeto. Anoto como débito de tooling, não do report |
| `branch_convencao` | `ok` | ok — reproduzido |
| `territorio` | `ok` | **prematuro** — ver §9.6 |
| `commits` | `- pendente` | fora do schema — ver §9.7 |
| `pr` | `null` | ok, coerente |
| `desvios` | `1` | aceito — ver abaixo |
| `pendencias_humano` | `2` | ok — bate com as 2 entradas em prosa |

**`estado: parcial` é a chamada certa, não `concluido`.** O `PRE-MERGE-CHECKLIST.md` §"Definição de pronto" é explícito: `concluido` só vale com todos os gates ok **e** `pendencias_humano: 0`. Com 2 pendências, `concluido` seria inválido — e há precedente de rejeição por isso nesta base (`docs/sprints/01-mvp/avaliacoes/backend-dep-04-pipeline-deploy-front.md:32` reprovou exatamente esse combo). Vale registrar que `WF-01`, `WF-02` e `WF-03` estão hoje com `concluido` + `pendencias_humano: 1`, ou seja, **fora da regra**; o FIX-006 é que está certo, não eles. Também não é `bloqueado`: nada está travado esperando decisão — o código está inteiro, os gates verdes, e as duas pendências são de execução (push/PR) e de verificação pós-deploy. `parcial` descreve isso com precisão.

**`desvios: 1` é justo.** Fui checar o plano em vez de aceitar o enquadramento. O plano proíbe em dois lugares: §Escopo linha 111 ("Sem mudar lógica de validação", escopada ao `MetaSignatureValidator`) e §"Não tocar" linha 117 ("Lógica de HMAC, handshake, mapper, allow-list runtime — só configuração declarativa"). A entrega cruzou os dois. Contar como 2 desvios (um por superfície) seria igualmente defensável, mas o critério do checklist é que o número **bata com as seções em prosa** — e bate: há uma seção de desvio, numerada `1`, que **nomeia explicitamente os dois arquivos e as duas guardas**. Nada foi escondido; é uma decisão única (fail-closed com sentinela) aplicada em dois pontos pela mesma causa. Aceito. Sugestão cosmética: citar também a linha 117 (§"Não tocar"), que é a proibição mais forte, e não só a 111.

**Citação dos fatos de prod — exata, sem exagero. Reconferi tudo com `gh` agora:**

| Afirmação no report | Verificação | Veredito |
|---|---|---|
| run `31285612236`, workflow `Deploy to Production`, failure | `gh run view` → `"workflowName":"Deploy to Production"`, `"conclusion":"failure"`, `"event":"push"`, `"headBranch":"main"`, `"displayTitle":"Merge pull request #118 from SSteringS/develop"` | **exato** |
| iniciado `2026-08-09T00:10:49Z`, 2m01s | `startedAt 00:10:49Z`, `updatedAt 00:12:50Z` → 2m01s | **exato** |
| `is-active` imprimiu `activating` em `2026-08-09T00:12:47Z`, exit code 3 | log: `00:12:47.6224317Z activating` / `00:12:47.6270434Z ##[error]Process completed with exit code 3.` | **exato, ao segundo** |
| "o log do CI **não** tem stack trace, só `activating`/exit 3" | `gh run view --log \| grep -icE "placeholder\|whatsapp_app_secret"` → **0** | **exato** |
| "a exceção veio do `journalctl` do humano e foi reproduzida localmente pelo Reviewer" | é a procedência real (§0/P1 desta avaliação) | **exato** |
| último deploy verde: run `26519270281`, PR #68, `2026-05-27T14:57:45Z` | `gh run view` → `success`, `startedAt 2026-05-27T14:57:45Z`, `"Merge pull request #68"` | **exato** |

A distinção CI-vs-`journalctl` está redigida corretamente ("o CI evidencia o crash-loop; a reprodução local evidencia a causa"). **Não encontrei nenhuma evidência superestimada no report** — e procurei especificamente por isso, porque atribuir ao log do CI um stack trace que ele não tem seria o erro fácil de cometer aqui. O report também é honesto ao marcar que `testes_total: 374` exclui os ITs, e ao creditar ao Reviewer as verificações que foram minhas em vez de reivindicá-las.

**Seções em prosa.** Todas as obrigatórias presentes; §"Padrões técnicos" preenchida (obrigatória para FIX com lógica não-trivial). Os 5 débitos listados correspondem aos itens 2-6 da minha §8.8, com o item 1 corretamente marcado como resolvido e o item da Observação 6 corretamente ausente por ter sido corrigido nesta rodada. `desvios`/`pendencias_humano` batem com as seções.

### 9.6 `territorio: ok` — declarado antes de ser verificável

`git diff --name-only origin/develop...HEAD` sai **vazio**: não há commit na branch. O gate, como o checklist o define, não tem o que medir — então `territorio: ok` hoje é uma afirmação sobre a intenção, não sobre o diff. Não é desonestidade (o report declara em §Decisões que `.claude/agents/backend.md` ficará fora do commit, que é a chamada certa), mas é um `ok` que ninguém consegue conferir.

Estado atual da árvore, para o commit: 6 arquivos de código (5 modificados + 1 novo, todos sob `financas_bot_telegram/`) e 2 arquivos em `docs/` — todos dentro do território de `claude-back`. `.claude/agents/backend.md` continua modificado e **não staged**; `.claude/` não está entre os caminhos permitidos na tabela de territórios. **Commitar com paths explícitos, nunca `git add -A`/`.`** — os 2 docs já estão no índice, então um `git commit` sem paths os levaria junto (o que é aceitável, `docs/` é território de todos), mas o `.claude/` não pode entrar.

### 9.7 `commits: - pendente` (pergunta 5) — **commite antes; preencha o hash**

Minha resposta: **não deixe `pendente`. Commite primeiro.**

Três razões, em ordem de peso:

1. **O campo tem tipo.** O template define `commits:` como lista de SHAs (`- 18b3f92`). `pendente` é um sentinela não documentado em lugar nenhum do schema. O ganho declarado do frontmatter é ser parseável (`PRE-MERGE-CHECKLIST.md` §Agregação); um valor fora de tipo é exatamente o que quebra isso.
2. **Sem commit, `territorio: ok` não é verificável** (§9.6). Commitar converte três campos de "declarados" em "conferíveis": `commits`, `territorio` e a própria coluna de diff da minha auditoria.
3. **Commit ≠ push, e o processo já distingue os dois.** O checklist operacional pede "**Não** fez push pra `develop` — parou pra revisão" — ele proíbe o *push*, e ao fazê-lo pressupõe que o commit local já existe. O checklist do Reviewer, no mesmo arquivo, manda auditar "depois que o implementador abre o PR". Commitar localmente não consome nenhuma autorização do humano e não é irreversível.

Depois do commit, atualize: `commits: - <sha>`, mantenha `pr: null`, mantenha `territorio: ok` (agora verificado) e reescreva a pendência 1 de "commit, push e PR" para "push e PR". `pendencias_humano` continua `2` e `estado` continua `parcial` — a pendência de validação em prod não muda.

Se por alguma razão o humano exigir que nem o commit local aconteça, então o correto é `commits: []` com a pendência descrevendo o motivo — nunca uma string livre num campo tipado. Mas a recomendação é commitar.

### 9.8 Débito técnico após a rodada 3

Atualiza a §8.8 (o planner consolida em `docs/PENDENCIAS-TECNICAS.md`):

1. ~~`MetaSignatureValidator` fail-closed~~ — resolvido na rodada 2.
2. ~~Handshake GET ecoando `hub.challenge` com sentinela~~ — **resolvido na rodada 3**, provado por M1/M2/M3 e por probe HTTP.
3. `secretSentinela_qualquerAssinatura_retornaFalse` sobrevive à mutação que remove a guarda de assinatura — teste de fronteira, não de regressão. Não contar como cobertura fail-closed.
4. Guard de placeholders não cobre `application.properties` (base, ativo em `prod`).
5. `${x:}` (default vazio) passa no guard e é tratado como *configurado* pelas duas guardas novas; para `app-secret` daria `IllegalArgumentException: Empty key` fora do `catch` de `isValid()`. Estado de config hipotético.
6. Comentário de `WhatsAppWebhookController:61` — reescrito nesta rodada, a afirmação falsa sobre `[""]` **saiu**; o filtro `isBlank` que ela justificava permanece (inofensivo). Débito reduzido a "filtro sem motivo declarado".
7. Migrar os `whatsapp.*` para `@ConfigurationProperties` com validação.
8. `STATE.md:96` desatualizado (planner) — PR #118 aberto, mergeado, deploy falho.
9. **NOVO — `cobertura_pct` é um gate sem ferramenta no backend.** O `PRE-MERGE-CHECKLIST.md` manda usar `mvn jacoco:report`, mas o `pom.xml` não tem o plugin JaCoCo. Todo status report de backend é forçado a `na`. Ou instalar o plugin ou remover a instrução do checklist.
10. **NOVO — `WF-01`/`WF-02`/`WF-03` têm `estado: concluido` com `pendencias_humano: 1`**, combinação inválida pela §"Definição de pronto". Corrigir para não virar precedente contra a regra.
11. **NOVO — comentário de `application-prod.properties:36-37`** credita a recusa só ao `MetaSignatureValidator`; com duas guardas agora, mencionar as duas. Puramente redacional.

### 9.9 Veredito final

| Item | Resultado |
|---|---|
| Código do delta (guarda de handshake + teste) | **aprovado** — correto, mínimo, na camada certa |
| Teste novo não-vacuoso | **provado** — morto por M1 (guarda ausente) e por M3 (guarda tardia); fixture independente do código |
| Guarda cedo o bastante / sem reflexão de `hub.challenge` | **confirmado** — ponto de reflexão único, atrás da guarda; probe HTTP: 403, corpo vazio, sem `Content-Type` |
| Caso configurado | **sem regressão** — 200 + challenge; M2 confirma pelo lado oposto |
| Comentário do `application-prod.properties` | **exato nas duas frases** — Observação 6 fechada |
| Status report | **schema válido**, números reproduzidos, fatos de prod exatos, `parcial` correto, `desvios: 1` justo |
| Premissas P1–P7 | P1-P5 `pass`; P6 e P7 eram falhas **do plano**, ambas endereçadas — P6 corrigida no código (POST na rodada 2, GET na rodada 3), P7 documentada no status report |
| Gates contra a realidade | build, testes, `testes_novos`, branch: **ok, reproduzidos**. `territorio`: pendente de commit (§9.6) |
| Observações abertas | **nenhuma** |
| **Veredito final** | **APROVADO — liberado para merge** |

Ressalva única, e ela é operacional, não de código: commitar antes do PR e trocar `commits: - pendente` pelo SHA (§9.7), commitando com paths explícitos para deixar `.claude/agents/backend.md` de fora. Isso não reabre revisão — não precisa voltar para mim.

A entrega chegou onde precisava. As duas afirmações de segurança que o plano fazia e que eram falsas agora são **fatos garantidos por código**, cada uma com um teste que morre se a garantia for removida ou apenas deslocada. E prod está caída desde `2026-08-09T00:12Z` — isto aqui é a restauração. Merge.

**Nota de escrita:** nesta rodada modifiquei **apenas** este arquivo de avaliação. Nenhum arquivo de código, teste, plano ou status foi tocado; toda a mutação rodou numa cópia no scratchpad e `git status` do repositório permaneceu idêntico do início ao fim.
