# Fase 0 — Marco zero: release de produção e congelamento do baseline

> Bloqueante. Nenhum run experimental inicia antes desta fase concluída e verificada.

## 1. Por que esta fase existe

"Fazer deploy em produção e ver se tudo ainda funciona" não é um passo preliminar deste plano. **É o maior risco isolado dele.**

Produção está parada em 27/05/2026 enquanto `develop` avançou até 14/07/2026. Um único merge dispara, simultaneamente e sem gate, quatro migrations em MySQL de produção, troca de JAR sem backup e invalidação de CloudFront — verificado apenas por `systemctl is-active`. Não existe plano de volta.

## 2. Estado atual de produção — levantamento factual

| Fato | Valor | Fonte |
|---|---|---|
| `origin/main` (produção) | `042deef` · 27/05/2026 | `git log -1 origin/main` |
| `origin/develop` | `d27ee98` · 14/07/2026 | `git log -1 origin/develop` |
| Delta | **208 commits · 580 arquivos · +44.784 / −1.804** | `git diff --stat origin/main...origin/develop` |
| Schema em produção | **V3** | `git ls-tree origin/main .../db/migration/` |
| Schema em develop | **V7** | mesmo diretório |
| Migrations que entram no próximo deploy | **V4, V5, V6, V7 — de uma vez, no restart** | `application-prod.properties` (`ddl-auto=none` + Flyway) |
| CI em PR para `main` | **não roda** — `ci.yml` só dispara para `develop` e `integration/**` | `.github/workflows/ci.yml` |
| Verificação pós-deploy no pipeline | `sleep 20 && systemctl is-active finbot`. Só isso. | `.github/workflows/deploy.yml` |
| Health check | `/actuator/health` existe e está no bypass do JWT — **nunca é chamado pelo pipeline** | `application.properties:61`, `JwtAuthenticationFilter.java:50` |
| Backup do JAR anterior | **não existe** — o workflow sobrescreve `/opt/finbot/app.jar` | `deploy.yml` |
| Runbook de rollback | **não existe** | `grep -ri rollback docs/runbooks/` → zero |
| Down-script de migration | **não existe** (Flyway sem undo) | — |
| Drift de infra | **Verificado em 2026-08-08 via `terraform plan`:** os recursos de `observability.tf` (log group `/finbot/app`, metric filter `finbot-prod-error-count`, alarmes `error_rate`/`cpu_high`/`disk_used_high`) **já estão aplicados em produção e sincronizados**. O único delta pendente é o `user_data` de `aws_instance.finbot_app`. `terraform apply` segue 100% manual — nenhum workflow o roda | `terraform plan` de 2026-08-08; detalhe em §4 Passo 2 |
| Webhook do Telegram | re-registro manual com cert self-signed; já quebrou silenciosamente por `setWebhook` sem `certificate=@` | `infra/provision/bootstrap.sh:127-150`, `PENDENCIAS-TECNICAS.md` item 6 |
| Rotação do token do Telegram | **pendente, prioridade alta** — token colado em chat durante incidente | `PENDENCIAS-TECNICAS.md:119` |
| Smoke test de prod | apenas `RUNBOOK-dep06-e2e-prod.md`, manual, e **já todo marcado `[x]`** — é registro histórico, não formulário reutilizável | `docs/runbooks/` |
| Tasks abertas na sprint 03 | **QA-010** e **QA-011**, `pronto-pra-execucao`, sem status report | `docs/sprints/03-folha-pagamento/README.md` |

## 3. Trade-off avaliado

| | **A1 — Big bang** | **A2 — Release com rede de segurança** | **A3 — Fatiar em releases menores** |
|---|---|---|---|
| Como | PR develop→main, aceitar | Antes: snapshot RDS + backup de JAR + health check no pipeline + smoke script + runbook de rollback. Depois: deploy, verificar, congelar baseline | Cherry-pick por blocos (schema → app → infra), 3–4 deploys |
| Esforço | ~0 | ~1–2 dias | ~1 semana |
| Risco de perda de dados | **Alto** — 4 migrations sem backup e sem undo | Baixo | Baixo |
| Tempo até detectar falha | Indefinido | Minutos | Minutos |
| Rollback | Impossível na prática | Documentado e testado | Documentado |
| Subproduto | nenhum | Gera a instrumentação e o runbook que o experimento precisa | idem, mais caro |
| Risco global | **Alto** | Baixo | Médio |

### Decisão registrada

> **Escolhida: A1 — Big bang.** Decisão do humano, 2026-08-08.
>
> A recomendação técnica original era A2. A decisão de assumir o risco é do humano e está registrada como tal.
>
> **Salvaguardas de custo aproximadamente zero foram mantidas** — snapshot do RDS e cópia do JAR atual não são A2: não exigem código, workflow nem runbook, custam dois comandos, e são a diferença entre "deu errado" e "perdi dados de produção". Estão no Passo 1.
>
> **Riscos aceitos explicitamente** estão listados na §6.

## 4. Passo a passo da release — A1 Big bang

> Convenções: comandos `git` de leitura rodam do worktree do planner. Comandos AWS assumem região `us-east-1`. Placeholders entre `<>` precisam ser resolvidos no momento da execução.
>
> **Janela sugerida:** fora do horário de uso do bot. Nenhuma sessão de implementador ativa no worktree compartilhado.

### Passo 0 — Pré-voo (~20 min, apenas leitura)

- [ ] **0.1** Confirmar que não há sessão do Claude do back/front ativa no worktree `C:\Users\satya\src\financas_bot_telegram` (o `.git/` é compartilhado).
- [ ] **0.2** Atualizar refs e anotar os SHAs no formulário do Passo 8:
  ```
  git fetch
  git log -1 --format="%H %ad %s" origin/main
  git log -1 --format="%H %ad %s" origin/develop
  git rev-list --count origin/main..origin/develop
  ```
- [ ] **0.3** Confirmar que `integration/03-folha-pagamento` já foi mergeada em `develop` e que não há trabalho pendente fora de `develop`:
  ```
  git log --oneline origin/develop..origin/integration/03-folha-pagamento
  ```
  Saída vazia = nada pendente.
- [ ] **0.4** Confirmar que o segredo `finbot-prod-secrets` contém a chave `keystore_password` (pré-requisito registrado em `docs/PENDENCIAS-TECNICAS.md`, seção de itens resolvidos):
  ```
  aws secretsmanager get-secret-value --secret-id finbot-prod-secrets --query SecretString --output text
  ```
- [ ] **0.5** **Registrar a configuração ATUAL do webhook do Telegram — antes de qualquer mudança.** Este é o estado a ser restaurado no Passo 6.
  ```
  curl -s "https://api.telegram.org/bot<TELEGRAM_TOKEN>/getWebhookInfo"
  ```
  Anotar: `url`, `has_custom_certificate`, `last_error_message`, `pending_update_count`.
- [ ] **0.6** Rodar os gates localmente, já que o CI **não roda** em PR para `main`:
  ```
  cd financas_bot_telegram && ./mvnw test && ./mvnw package -DskipTests
  cd ../frontend && npm ci && npm run lint && npm test && npm run build
  ```
  Qualquer vermelho aqui → **abortar**, corrigir em `develop` primeiro.

### Passo 1 — Salvaguardas manuais (~10 min, custo próximo de zero)

> Não é A2. São dois comandos e um `ls`. **Não pule o 1.1** — sem ele não existe rollback possível (ver §5).

- [ ] **1.1** **Snapshot manual do RDS.**
  ```
  aws rds describe-db-instances --query "DBInstances[].DBInstanceIdentifier" --output text
  aws rds create-db-snapshot \
    --db-instance-identifier <db-instance-identifier> \
    --db-snapshot-identifier finbot-pre-t0-<YYYYMMDD>
  aws rds wait db-snapshot-available --db-snapshot-identifier finbot-pre-t0-<YYYYMMDD>
  ```
  Anotar identificador e horário exatos. **Tudo que for escrito no banco após este instante se perde num rollback.**
- [ ] **1.2** **Backup do JAR em produção.**
  ```
  ssh -i <chave.pem> ec2-user@3.228.138.109 \
    "sudo cp /opt/finbot/app.jar /opt/finbot/app.jar.pre-t0 && ls -la /opt/finbot/"
  ```
- [ ] **1.3** Anotar o SHA atual de `origin/main` como **ponto de retorno**.
- [ ] **1.4** Registrar a versão de schema atualmente aplicada:
  ```
  ssh -i <chave.pem> ec2-user@3.228.138.109 \
    "sudo journalctl -u finbot --no-pager | grep -i flyway | tail -20"
  ```
  Esperado: histórico até **V3**.

### Passo 2 — Infra (terraform), ANTES da aplicação

> ✅ **EXECUTADO em 2026-08-08.** Resultado e decisão registrados abaixo. Só reexecutar se `develop` receber novas mudanças em `financas_bot_telegram/infra/`.

- [x] **2.1**
  ```
  cd financas_bot_telegram/infra
  terraform init
  terraform plan -var-file=prod.tfvars -out=tfplan
  ```

  > ⚠️ **Quirk de ambiente (PowerShell 5.1):** sem aspas nas flags, o comando falha com `Error: Too many command line arguments`. Forma que funciona:
  > ```
  > terraform plan -no-color "-var-file=prod.tfvars" "-out=tfplan" "-input=false"
  > ```

- [x] **2.2** **LER O PLAN INTEIRO antes de aplicar.** Procurar por `must be replaced` e `forces replacement`.

  > 🛑 **PARADA OBRIGATÓRIA:** se `aws_instance.finbot_app` aparecer como *replaced*, a EC2 será destruída e recriada. Isso re-executa `provision/bootstrap.sh`, que **regenera o keystore self-signed** — o certificado do webhook do Telegram muda e o bot quebra silenciosamente. Também há downtime e reassociação do Elastic IP.
  >
  > Se isso aparecer: **não aplique**. Reavalie — provavelmente é a mudança de `user_data`. Trate como decisão separada, não como parte desta release.

#### Resultado da execução — 2026-08-08

| Item | Valor |
|---|---|
| Conta AWS | `776658251579` confirmada (usuário IAM `financas-bot-app_dev`) |
| Terraform / provider | 1.14.3 · `hashicorp/aws` v5.100.0 · backend S3 `finbot-tfstate-satyans`, key `prod/terraform.tfstate` |
| Sumário | **`Plan: 0 to add, 1 to change, 0 to destroy.`** |
| `must be replaced` / `forces replacement` / `will be destroyed` / `-/+` | **zero ocorrências** |
| `aws_instance.finbot_app` replaced? | **NÃO** — `will be updated in-place` |
| `aws_eip.finbot_eip` replaced? | **NÃO** — não consta no plano de ações |
| Erros / avisos do plan | nenhum |

**Gate do 2.2: PASSA.** A parada obrigatória não foi acionada. Causa: `ec2.tf` não define `user_data_replace_on_change` (default `false` no provider v5) e tem `lifecycle { ignore_changes = [ami] }`.

**Único delta do plan:**

```
  # aws_instance.finbot_app will be updated in-place
  ~ resource "aws_instance" "finbot_app" {
        id        = "i-00fcd1b3fe47da399"
      ~ user_data = "6c6a283c..." -> "2da14e6f..."
      ~ public_ip = "3.228.138.109" -> (known after apply)
    }
```

Corresponde ao commit `a185f94` (`feat(DEP-07): bootstrap.sh idempotente + user_data via templatefile`), presente em `develop` e ausente de `origin/main`: troca do script inline de 6 linhas pelo `templatefile(provision/bootstrap.sh)`.

**Duas descobertas que contradizem a premissa original do Passo 2:**

1. **Não há parcela aditiva.** Os recursos de `observability.tf` já estão aplicados e sincronizados — o refresh os encontrou todos no state sem mudança pendente. A justificativa original do passo ("não deixar o serviço subir contra infra que não existe") **já está satisfeita sem apply nenhum**.
2. **O único delta é justamente o item de risco.** E ele tem dois problemas somados:
   - **Custa downtime.** A API da AWS exige a instância **parada** para modificar `user-data`; o provider faz `StopInstances` → `ModifyInstanceAttribute` → `StartInstances`. "Update in-place" no Terraform significa parar e subir a EC2 de produção.
   - **Não produz efeito.** `user_data` com shebang roda *once-per-instance* no cloud-init, e stop/start preserva o instance-id — o `bootstrap.sh` **não executa**. O apply seria cosmético: sincroniza o state e nada do DEP-07 (Caddy, `finbot.service`, agente CloudWatch, keystore) chega à máquina.

- [x] **2.3** **DECISÃO: não aplicar.** Passo 2 fechado com plan verificado e **nenhuma ação executada**.

  Justificativa: o apply trocaria downtime real de produção por benefício operacional zero, e adicionaria o risco de o serviço não voltar (ver §7). O `user_data` do DEP-07 fica como **decisão separada da release**, que é exatamente o encaminhamento previsto no 2.2.

  Pré-requisito para qualquer apply futuro deste delta: confirmar por SSH que o serviço está habilitado no boot —
  ```
  ssh -i <chave.pem> ec2-user@3.228.138.109 "systemctl is-enabled finbot"
  ```

- [x] **2.4** Outputs conferidos no refresh: `ec2_public_ip` = `3.228.138.109`, `cloudfront_distribution_id` = `E1WG4Q8MG3V9HY`, `frontend_bucket_name` = `finbot-frontend-prod-776658251579` — batem com o que os workflows usam. Nenhum bloco `Changes to Outputs` no plan.

### Passo 3 — Merge para `main`

- [ ] **3.1** Abrir PR `develop → main`.
- [ ] **3.2** Registrar no PR que **o CI não roda neste gate** e que os gates foram executados localmente no Passo 0.6.
- [ ] **3.3** Merge.

### Passo 4 — Acompanhar os dois deploys

O merge dispara **dois** workflows: `deploy.yml` (backend, SSH/EC2) e `deploy-frontend.yml` (OIDC/S3/CloudFront, porque `frontend/**` foi tocado).

- [ ] **4.1** Acompanhar as Actions ao vivo.
- [ ] **4.2** Marcos de decisão do `deploy.yml`:
  - falha em `mvn test` ou `mvn package` → **nada chegou a produção**. Corrigir em `develop`, novo PR. Estado seguro.
  - falha **depois** do `scp`/`ssh` → **estado indeterminado**. Ir direto para §5 (rollback).
- [ ] **4.3** O último passo do workflow é `sleep 20 && systemctl is-active finbot`. Verde aqui significa apenas que o processo está de pé — **não** que as migrations aplicaram nem que a API responde.

### Passo 5 — Migrations

- [ ] **5.1** Ler o log do Flyway no boot:
  ```
  ssh -i <chave.pem> ec2-user@3.228.138.109 \
    "sudo journalctl -u finbot --no-pager -n 300 | grep -iE 'flyway|migrat|error|exception'"
  ```
- [ ] **5.2** Confirmar que V4, V5, V6 e V7 foram aplicadas e que a versão final é **7**.
- [ ] **5.3** Se alguma migration falhou, o Flyway aborta o boot e o serviço não sobe. Ir para §5.

### Passo 6 — Webhook do Telegram

> Historicamente o ponto de falha mais provável, e o mais difícil de perceber, porque falha em silêncio.

- [ ] **6.1** Reconsultar:
  ```
  curl -s "https://api.telegram.org/bot<TELEGRAM_TOKEN>/getWebhookInfo"
  ```
- [ ] **6.2** Comparar com o registrado no Passo 0.5. Se `url` e `has_custom_certificate` continuam iguais e `last_error_message` está vazio → **nada a fazer**.
- [ ] **6.3** Se divergiu ou há erro, restaurar **exatamente o padrão anterior**, não um padrão novo.
  - Se o registro anterior tinha `has_custom_certificate: false` (TLS terminado pelo Caddy no domínio público):
    ```
    curl -F "url=https://api.satyansaita.com/webhook/telegram" \
      "https://api.telegram.org/bot<TELEGRAM_TOKEN>/setWebhook"
    ```
  - Se tinha `has_custom_certificate: true` (acesso direto ao 8443 com self-signed), extrair o certificado do keystore e reenviá-lo:
    ```
    ssh -i <chave.pem> ec2-user@3.228.138.109 \
      "sudo openssl pkcs12 -in /opt/finbot/keystore.p12 -clcerts -nokeys -out /tmp/cert.pem -passin pass:<keystore_password>"
    scp -i <chave.pem> ec2-user@3.228.138.109:/tmp/cert.pem ./cert.pem
    curl -F "url=<url-anterior>" -F "certificate=@cert.pem" \
      "https://api.telegram.org/bot<TELEGRAM_TOKEN>/setWebhook"
    ```
- [ ] **6.4** Reconferir `getWebhookInfo`: `last_error_message` vazio e `pending_update_count` não crescendo.

### Passo 7 — Verificação funcional

- [ ] **7.1** Health:
  ```
  curl -s https://api.satyansaita.com/actuator/health
  ```
  Esperado: `{"status":"UP"}`.
- [ ] **7.2** API autenticada — obter sessão via link mágico (`X-Admin-Key`, conforme `RUNBOOK-dep06-e2e-prod.md`) e chamar `GET /api/v1/pedidos`. Esperado: 200 com dados reais.
- [ ] **7.3** Frontend:
  ```
  curl -sI https://satyansaita.com | head -1
  ```
  Esperado: `200`. Abrir no navegador e confirmar que a listagem carrega (valida CORS, cookie e o `VITE_API_BASE_URL`).
- [ ] **7.4** **Bot de ponta a ponta** — enviar uma mensagem real e conferir que o pedido aparece. Testar as duas strategies:
  - `10,00 teste pos-deploy` (pedido) com foto anexada
  - `#<id> pix` (comprovante) com foto anexada
- [ ] **7.5** Rodar `docs/runbooks/RUNBOOK-dep06-e2e-prod.md` como **formulário em branco**, não como registro histórico.
- [ ] **7.6** Conferir no CloudWatch se algum alarme de `observability.tf` disparou e se o metric filter de erros está capturando algo.

### Passo 8 — Registro e congelamento

- [ ] **8.1** Criar `docs/experiments/models-claude-experiment/BASELINE-t0.md` com:
  - SHA de `main` (novo) e de `develop`, data e hora do deploy
  - identificador e horário do snapshot de RDS (Passo 1.1)
  - versão de schema aplicada (esperado: 7)
  - contagem de testes por camada — backend, frontend, E2E
  - cobertura de linha/branch e mutation score **após** instalar JaCoCo e PIT (Fase 1) — deixar marcado `pendente` até lá
  - inventário das pendências técnicas abertas
  - resultado de cada item do Passo 7
  - versão do Claude Code e IDs completos dos modelos disponíveis
- [ ] **8.2** Tag `exp/base-t0` no commit de `develop` usado como base. Todos os runs experimentais partem dela.
- [ ] **8.3** Decidir e registrar: **QA-010 e QA-011 rodam antes do t0 ou depois do experimento?** Executá-las no meio muda a baseline de testes e invalida a comparação. Recomendação: depois.

## 5. Rollback de emergência

> Não existe runbook testado. Este é um procedimento de emergência escrito, não validado. Ler **antes** de precisar.

### O ponto que muda tudo

**Restaurar apenas o JAR não é rollback.** Com o schema em V7 e o JAR antigo (que só conhece V1–V3), o Flyway executa `validate` no boot, encontra V4–V7 aplicadas e ausentes do classpath, e aborta com `Detected applied migration not resolved locally`. A aplicação **não sobe**.

Consequência: **rollback = restaurar banco e JAR juntos.** É por isso que o snapshot do Passo 1.1 não é opcional.

### Procedimento

1. **Parar o serviço:**
   ```
   ssh -i <chave.pem> ec2-user@3.228.138.109 "sudo systemctl stop finbot"
   ```
2. **Restaurar o JAR:**
   ```
   ssh -i <chave.pem> ec2-user@3.228.138.109 \
     "sudo cp /opt/finbot/app.jar.pre-t0 /opt/finbot/app.jar && sudo chown finbot:finbot /opt/finbot/app.jar"
   ```
3. **Restaurar o banco a partir do snapshot.** `restore-db-snapshot` cria uma **instância nova** — o endpoint muda:
   ```
   aws rds restore-db-instance-from-db-snapshot \
     --db-instance-identifier <novo-id> \
     --db-snapshot-identifier finbot-pre-t0-<YYYYMMDD>
   ```
   Depois: atualizar `db_host` em `finbot-prod-secrets` para o novo endpoint, liberar o security group para o Elastic IP da EC2, e reiniciar o serviço.
4. **Subir e verificar:**
   ```
   ssh -i <chave.pem> ec2-user@3.228.138.109 "sudo systemctl start finbot && sleep 20 && sudo systemctl is-active finbot"
   curl -s https://api.satyansaita.com/actuator/health
   ```
5. **Reverter `main`** para o SHA do Passo 1.3 — atenção: o push dispara `deploy.yml` de novo, agora com o código antigo, o que é o desejado.
6. **Reconferir o webhook** (Passo 6) — o restart pode ter alterado o estado.

**Perda de dados assumida:** tudo escrito no banco entre o snapshot (Passo 1.1) e o rollback.

### Critério de abortar

Abortar e ir para rollback se: alguma migration falhar (5.3) · a aplicação não subir após 2 tentativas · `/actuator/health` não retornar `UP` em 10 minutos · o webhook não voltar a funcionar após o Passo 6.

## 6. Riscos aceitos com a opção A1

Consequência direta de não fazer A2. Registrados para que a escolha seja informada, não implícita:

1. **Nenhuma verificação automatizada no pipeline.** O health check continua sendo `systemctl is-active`. Toda verificação real é manual (Passo 7) — se o operador não rodar, a falha só aparece quando um usuário reclamar.
2. **Nenhum smoke test repetível.** O Passo 7 é uma lista de comandos executada à mão, não um script. Não serve de gate para deploys futuros e não produz artefato comparável.
3. **Rollback não testado.** O procedimento da §5 está escrito, nunca foi executado. Descobrir um erro nele durante um incidente é o cenário caro.
4. **CI segue sem rodar em PR para `main`.** O gate do Passo 0.6 depende de disciplina humana. Se alguém pular, entra em produção sem nenhuma verificação automatizada.
5. **Janela de perda de dados** = intervalo entre o snapshot (1.1) e a detecção da falha. Como a detecção é manual, essa janela pode ser longa.
6. **Sem `app.jar.prev` automático no workflow.** O backup do Passo 1.2 vale para esta release; a próxima volta a sobrescrever o JAR sem cópia.
7. **Token do Telegram continua não rotacionado** (`PENDENCIAS-TECNICAS.md`, prioridade alta) — risco pré-existente, carregado adiante conscientemente.
8. **`data_pagamento` continua "mentindo" via API** (`PENDENCIAS` item 14) — coluna `NULL` para todo pedido novo PAGO, mas exposta. Durante o Passo 7 isso pode ser confundido com bug de deploy.

## 7. Riscos operacionais da execução

1. ~~**`terraform plan` indicando replacement da EC2**~~ — **verificado em 2026-08-08: não ocorre.** `Plan: 0 to add, 1 to change, 0 to destroy`, sem replacement. Gate do 2.2 passou. Reavaliar apenas se `financas_bot_telegram/infra/` mudar em `develop`.
2. **Ordem invertida** (aplicação antes da infra) dificulta muito o diagnóstico. Mitigado nesta release: não há apply de infra pendente.
3. **Webhook com cert self-signed** — falha silenciosa, já ocorreu antes. Mitigado por registrar o estado anterior no Passo 0.5 e restaurar o mesmo padrão, em vez de inventar um novo.
4. **Sessão de implementador ativa no worktree compartilhado** durante os comandos git — Passo 0.1.
5. **`RUNBOOK-dep06-e2e-prod.md` já está todo marcado `[x]`.** Precisa virar formulário reutilizável, senão a verificação é copiar um documento que diz que deu certo em maio.
6. **A EC2 de produção é um *pet* configurado à mão — descoberta de 2026-08-08.** O `user_data` efetivamente aplicado na instância `i-00fcd1b3fe47da399` é o script inline antigo de 6 linhas (`dnf update`, Java 21, `mkdir /opt/finbot`, `useradd finbot`). O `bootstrap.sh` **nunca rodou nesta instância**. Logo, Caddy, a unit `finbot.service`, o agente CloudWatch, a config do journald e o keystore self-signed foram instalados manualmente. Consequências:
   - O Terraform descreve uma máquina **diferente** da que está rodando. É drift no nível de configuração, **invisível ao `terraform plan`**, que só rastreia a string do `user_data`.
   - **Não há garantia de que `finbot` esteja `systemctl enable`d.** Quem faz esse enable é `bootstrap.sh:56`, que nunca executou. Qualquer stop/start da instância — inclusive o causado por um `terraform apply` do delta de `user_data` — pode trazer a EC2 de volta **sem o serviço**, com produção fora do ar até intervenção manual por SSH.
   - Se a instância morrer ou for recriada algum dia, o `bootstrap.sh` roda **pela primeira vez em produção**, sem nunca ter sido testado, e **regenera o keystore** — quebrando o webhook do Telegram.
   - Ação sugerida (fora do escopo desta release): verificar `systemctl is-enabled finbot`, e promover a validação do `bootstrap.sh` num ambiente descartável a item de `docs/PENDENCIAS-TECNICAS.md`.
7. **Credencial de produção com nome de dev.** O `aws sts get-caller-identity` do Passo 2.1 retornou o usuário IAM `financas-bot-app_dev` operando infra de produção na conta `776658251579`. Não afeta esta release; candidato a `docs/PENDENCIAS-TECNICAS.md`.
8. **Artefatos do `terraform plan` fora do controle de versão.** O `plan -out=tfplan` gera um plano binário com valores resolvidos do state, incluindo atributos sensíveis, e o `.gitignore` da raiz não o cobria. Resolvido em 2026-08-08 com a adição de `tfplan`, `*.tfplan`, `plan-output.txt` e `*.stackdump` ao `.gitignore`. Reverificar antes de qualquer `git add .` nesta pasta.
