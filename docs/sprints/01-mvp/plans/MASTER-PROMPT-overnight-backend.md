# Prompt mestre — execução overnight do backend da Fase 3

> Documento pra você copiar e colar inteiro na sessão do Claude do back. Cobre as 12 tarefas restantes na ordem certa. O Claude executa tudo, comita cada tarefa, e para em pontos de erro pra você revisar de manhã.

---

## Copie tudo abaixo desta linha pra dentro do Claude Code do IntelliJ

---

Você vai trabalhar overnight executando uma sequência de 12 tarefas do backend da Fase 3. Vou te dar a lista, as regras de execução, e os critérios pra parar e me esperar acordar.

**Antes de tudo, leia obrigatoriamente:**

- `docs/README.md` — regras da pasta de docs e do fluxo de status
- `docs/architecture/estado-atual.md` — contexto do código real

**Regras gerais de execução (NÃO QUEBRE):**

1. **Crie uma única branch** chamada `feature/backend-fase3-api-completa` a partir de `develop` atualizada. Trabalhe nela do começo ao fim. Não faça push pra `develop` em momento nenhum — eu reviso todos os commits e mergeio manualmente de manhã.

2. **Cada tarefa = um commit dedicado nesta branch.** Mensagem do commit no formato:
   ```
   feat(BE-XX): título curto da tarefa
   
   <descrição opcional do que mudou>
   ```
   (use `fix(BE-XX)` quando a tarefa for de correção/parsing/handler genérico, `test(BE-XX)` quando for só testes — use seu julgamento, mas tipo + ID da tarefa é obrigatório.)

3. **Antes de começar cada tarefa:** leia o plano correspondente em `docs/plans/BE-XX-*.md`. Confira que as pré-requisitos listados no plano estão satisfeitos olhando o estado do código local (não só o que outros status reports disseram).

4. **Implemente seguindo o plano.** O plano contém código pronto pra colar; use como ponto de partida e ajuste só se realmente fizer sentido. Se quiser desviar significativamente, **pare e documente no status**, não improvise.

5. **Após implementar, rode `./mvnw test` na raiz do módulo Maven (`financas_bot_telegram/`).** Tudo verde é pré-requisito pra commit. Se algum teste vermelho:
   - Se o teste vermelho é dos testes novos que você acabou de escrever, ajuste e re-rode.
   - Se é teste antigo (anterior à tarefa) que quebrou, investiga: a tarefa atual quebrou contrato existente? Isso é sinal de bug — **pare** e relate (criterio de aceitação implícito é "não quebrar testes existentes").

6. **Escreva o status report** em `docs/status/BE-XX-<slug>.md` seguindo `docs/status/_TEMPLATE.md`. Status report é parte do commit (não commit separado).

7. **Commit** com a mensagem padrão. Não faça push intermediário (mantém local na sua máquina ou push só quando terminar tudo — eu reviso pelo git log).

8. **Passe pra próxima tarefa.** Sem pedir permissão, sem aguardar.

**Pontos pra PARAR e me esperar:**

Pare e **deixe um arquivo `docs/status/BE-XX-PARADO.md`** explicando o motivo se:

- Testes existentes (anteriores à sua tarefa) começam a falhar e não é óbvio o porquê
- Pré-requisito do plano não está satisfeito (ex: BE-10 manda usar `RequisitanteRepositoryAdapter` que não existe ainda)
- Compilação quebra e o fix não é trivial (>15 min investigando)
- O plano tem ambiguidade que você não consegue resolver sozinho
- Você bate em uma decisão de produto não especificada (ex: "deveria filtrar de outra forma?")
- Docker não está disponível pra BE-14 (Testcontainers). Nesse caso, marca BE-14 como bloqueada e segue pulando ela; o resto pode continuar.

**Não pare** por:
- Pequenos ajustes de import, formatação, ordem de campos — isso é normal
- Diferenças entre o que está no plano e seu conhecimento atual da codebase — siga o plano como guia, ajuste pra que faça sentido com o estado real

**Sequência das 12 tarefas, na ordem exata:**

1. `docs/plans/BE-10-auth-token-admin.md`  — Requisitante + auth_token + endpoint admin
2. `docs/plans/BE-11-auth-exchange-jwt.md` — POST /auth/exchange + JWT + cookie
3. `docs/plans/BE-12-auth-filter.md`       — Filter de auth + GET /auth/me
4. `docs/plans/BE-13-cors.md`              — CORS
5. `docs/plans/BE-05-listar-pedidos.md`    — GET /pedidos com filtros
6. `docs/plans/BE-06-detalhe-pedido.md`    — GET /pedidos/{id}
7. `docs/plans/BE-07-presigned-url.md`     — Service de pre-signed URL S3
8. `docs/plans/BE-08-endpoints-imagem.md`  — GET /pedidos/{id}/foto-pedido e /comprovante
9. `docs/plans/BE-09-resumo.md`            — GET /resumo
10. `docs/plans/BE-03-parsing-tipo-legenda.md` — Extrair tipo da legenda do bot
11. `docs/plans/BE-15-handler-generico-excecoes.md` — Handler genérico do webhook
12. `docs/plans/BE-14-testes-integracao.md` — Testcontainers + integration tests

**Lembretes finais:**

- `architecture/especificacao-tecnica.md` tem exemplos de WebFlux em algumas seções (CORS, auth filter). Esses estão **desatualizados** — o projeto migrou pra Spring MVC. Os planos individuais (BE-XX-*.md) já estão corretos pra MVC. Em caso de conflito, **plano vale mais que especificação técnica**.
- Domínio usa POJOs em `domain/model/` + JPA entities em `adapters/out/persistence/entity/` + mappers explícitos. Mantenha esse padrão pra qualquer nova entidade.
- Cada repositório tem **um único port** (`*RepositoryPort`), sem o pattern de `extends JpaRepository` exposto na camada de aplicação. Mantenha esse padrão.
- Testes unitários são **obrigatórios** em toda classe com lógica não-trivial — caminho feliz + pelo menos 1-2 cenários de erro.
- Configs sensíveis (jwt secret, admin api key) vão pro Secrets Manager em prod. Em dev, valores placeholder em `application-dev.properties.example`. Os planos detalham quais chaves novas eu (humano) preciso adicionar ao `finbot-prod-secrets` — **liste consolidado no último status report (BE-14)** pra eu fazer antes do deploy.
- Cada commit deve ser cirúrgico: só os arquivos da tarefa atual. Sem mudanças "de oportunidade" em código não relacionado.

Quando terminar todas as 12 ou parar em alguma, deixa um resumo final em `docs/status/_RESUMO-overnight.md` listando o que foi feito, o que ficou parado, e qualquer coisa que precise da minha atenção. Esse arquivo é o que eu vou ler primeiro de manhã.

Boa execução. Roda!

---

## Fim do prompt — daqui pra baixo são notas pra você (humano)

---

## Checklist do que esperar de manhã

Se tudo correr bem, ao acordar você terá:

- 12 commits novos na branch `feature/backend-fase3-api-completa`, com mensagens padronizadas
- 12 status reports em `docs/status/BE-XX-*.md`
- 1 ADR novo em `docs/decisions/0003-controller-webhook-nunca-retorna-5xx.md`
- 1 arquivo `docs/status/_RESUMO-overnight.md` resumindo tudo

**Antes de mergear em develop, eu sugiro fazer:**

1. **Revisão dos 12 commits via `git log --oneline -15` + `git diff develop..feature/backend-fase3-api-completa`** — pega coisas estranhas, código duplicado, decisões questionáveis
2. **Smoke test manual via Swagger UI** (rodar app em dev):
   - Gerar convite via `POST /admin/api/v1/requisitantes/1/convite`
   - Fazer exchange via `POST /api/v1/auth/exchange`
   - Listar pedidos `GET /api/v1/pedidos`
   - Detalhe `GET /api/v1/pedidos/{id}`
   - Comprovante `GET /api/v1/pedidos/{id}/comprovante` (deve fazer 302)
   - Resumo `GET /api/v1/resumo`
3. **Smoke test do bot via Telegram** (mandar um pedido com legenda `100 pix Maria`, verificar que `tipo='PIX'` no banco)
4. **Conferir segredos pendentes**: ler o resumo final pra ver quais chaves o Secrets Manager precisa receber (`admin_api_key`, `jwt_secret`) — adicionar no console AWS ANTES de mergear pra `develop` (porque o merge eventualmente vira deploy em prod)
5. Se tudo ok: PR `feature/backend-fase3-api-completa` → `develop`, mergear

Se algum commit estiver problemático: cherry-pick os bons, descartar o ruim, ajustar e fazer um commit final de "fix".

## Cuidados pra ter na hora de mergear

- O merge eventualmente sobe pra `main` e dispara deploy em prod. Antes de promover `develop` pra `main`:
  - Garantir que `admin_api_key` e `jwt_secret` estão no Secrets Manager
  - Garantir que `app.cors.allowed-origin` em `application-prod.properties` está apontando pro domínio do front quando ele existir (por enquanto, valor placeholder está ok desde que o front esteja em dev)
  - Smoke test em prod logo após o deploy: `getWebhookInfo` ok, mandar mensagem de teste pelo bot, confirmar persistência

## Em caso de problemas overnight

Se você acordar e o Claude do back tiver parado em uma tarefa:
- Leia `docs/status/BE-XX-PARADO.md` (ou o último status report)
- Decida: deixa o problema pra mim resolver (planejador), ou corrige direto
- Se for ajustar e re-executar, basta dar continue: "Voltei. Resolve o problema da BE-XX e continua a sequência."
