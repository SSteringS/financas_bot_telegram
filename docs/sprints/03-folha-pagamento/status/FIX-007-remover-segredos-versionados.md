---
task: FIX-007
titulo: "Remover segredos versionados do estado atual do repositório (repo público)"
data: 2026-08-09
branch: fix/007-remover-segredos-versionados
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 422
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - d1c5e08
  - 49856b5
  - ab12ba4
pr: https://github.com/SSteringS/financas_bot_telegram/pull/121
desvios: 2
pendencias_humano: 0
---

# FIX-007 — Remover segredos versionados do estado atual do repositório

---

## O que foi feito

Limpeza do **estado atual** de `develop` (o histórico não foi tocado — decisão do plano §Decisão/abordagem).

- **`README.md:128`** — o `curl` de exemplo do `setWebhook` tinha o token do bot gen-1 e a URL de ngrok reais. Viraram `<SEU_TOKEN_AQUI>` (mesmo placeholder já usado na linha 65 do próprio README) e `<sua-url-ngrok>`.
- **`docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md`** — 3 pontos. A senha do MySQL local (§Pré-requisitos) e a `admin_api_key` de dev (tabela "Dados úteis" e o `curl` do passo 0.3) viraram `<sua-senha-mysql-local>` e `<sua-admin-api-key>`. Cada placeholder ganhou o **nome exato da property** de onde o leitor tira o valor (`spring.datasource.password`, `app.admin.api-key`), e o bloco do `curl` ganhou uma linha explicando a substituição — o runbook continua seguível sem consultar mais nada.
- **`financas_bot_telegram/http/BE-16-testes.http`** — o token de sessão literal virou `TOKEN_AQUI`, que já era o termo usado pelo comentário do próprio passo 1 ("Substitua TOKEN_AQUI pelo valor copiado no Passo 0"). Adicionado um passo 0 no cabeçalho apontando para o `.example`, já que o `http-client.env.json` deixou de vir no clone.
- **`financas_bot_telegram/http/http-client.env.json`** — destrackeado com `git rm --cached` (arquivo local do humano preservado no disco), coberto por nova regra no `.gitignore` da raiz, e substituído por `http-client.env.json.example` com a mesma estrutura e `admin_key` como placeholder.
- **18 ocorrências da senha do keystore em `docs/**`** — trocadas por `<keystore-password>` em 8 arquivos. Só o valor foi mascarado; a prosa ao redor dos status reports e das avaliações ficou intacta, então a narrativa histórica continua legível.
- **`financas_bot_telegram/infra/provision/bootstrap.sh`** — o `-passout pass:<literal>` virou leitura de `keystore_password` a partir de `finbot-prod-secrets`, com **quatro caminhos de falha explícitos** (AWS CLI ausente, `get-secret-value` falhando, secret não parseável como JSON, chave ausente ou vazia). Todos logam e fazem `exit 1` — nenhum gera keystore com senha vazia. O `-passout env:` (em vez de `pass:`) mantém a senha fora da lista de processos. Região e credenciais ficam por conta do próprio AWS CLI v2 (ver §Correções pós-revisão, R1).

### Verificação obrigatória do plano — permissão IAM: **PRESENTE**

O plano mandava confirmar em `financas_bot_telegram/infra/iam*.tf`. **A permissão não está em `iam*.tf`** — o único arquivo com esse glob é `iam-github-oidc.tf`, que trata só da role OIDC do GitHub Actions. A permissão da EC2 está em **`financas_bot_telegram/infra/security.tf`**:

| Linha | Conteúdo | Papel |
|---|---|---|
| `security.tf:2` | `resource "aws_iam_role" "ec2_role"` | role da EC2 |
| `security.tf:18-21` | `aws_iam_instance_profile.ec2_profile` → `role = aws_iam_role.ec2_role.name` | instance profile |
| `ec2.tf:24` | `iam_instance_profile = aws_iam_instance_profile.ec2_profile.name` | profile ligado à instância |
| `security.tf:24-26` | `data "aws_secretsmanager_secret" "app_secrets"` com `name = "finbot-${var.env}-secrets"` | resolve `finbot-prod-secrets` |
| `security.tf:36` | `Action = ["secretsmanager:GetSecretValue"]` | ação concedida |
| `security.tf:37` | `Resource = [data.aws_secretsmanager_secret.app_secrets.arn]` | escopo = o segredo certo |
| `security.tf:42-45` | `aws_iam_role_policy_attachment.ec2_secrets_attach` | policy anexada à role |

Cadeia completa e fechada → **caso "se tiver": implementado como planejado.** Nenhuma permissão IAM foi adicionada.

### Evidência dos critérios de aceitação

**Nenhum dos literais é reproduzido aqui** — este status report também é público, e colar os valores nas linhas de comando os reintroduziria no repo, anulando o FIX. Os comandos abaixo recuperam cada literal do estado anterior e usam a variável; `ad6a60a` é o merge-base desta branch com `develop`.

```
$ B=ad6a60a

# senha do keystore
$ SECRET=$(git show $B:financas_bot_telegram/infra/provision/bootstrap.sh \
           | sed -n 's/.*-passout pass://p' | tr -d '[:space:]')
$ git grep -n --text "$SECRET"
exit=1 (1 = zero ocorrências)

# admin api-key de dev — cobre http-client.env.json e as 2 linhas do runbook
$ ADMIN=$(git show $B:financas_bot_telegram/http/http-client.env.json | jq -r '.dev.admin_key')
$ git grep -n --text "$ADMIN"
exit=1 (1 = zero ocorrências)

# senha do MySQL local, do runbook
$ MYSQL=$(git show $B:docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md \
          | sed -n 's/.*usuário `root`, senha `\([^`]*\)`.*/\1/p')
$ git grep -n --text "$MYSQL"
exit=1 (1 = zero ocorrências)

# token de sessão do BE-16-testes.http
$ SESSAO=$(git show $B:financas_bot_telegram/http/BE-16-testes.http \
           | sed -n 's/.*"token": "\([^"]*\)".*/\1/p')
$ git grep -n --text "$SESSAO"
exit=1 (1 = zero ocorrências)

# token do Telegram — por padrão, pega qualquer token de bot em qualquer formato
$ git grep -nE "[0-9]{8,10}:AA[A-Za-z0-9_-]{30,}"
exit=1 (1 = zero ocorrências)

$ git grep -n "X-Admin-Key\|admin_key\|Admin API key" \
    -- docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md financas_bot_telegram/http/
docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md:79:| Admin API key (dev) | `<sua-admin-api-key>` — valor de `app.admin.api-key` no seu `application-dev.properties` (local, gitignored) |
docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md:99:  -H "X-Admin-Key: <sua-admin-api-key>"
docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md:186:| `401` ao gerar convite | `X-Admin-Key` ausente/errada | Usar a chave da tabela "Dados úteis" |
financas_bot_telegram/http/BE-16-testes.http:19:X-Admin-Key: {{admin_key}}
  → os 3 pontos citados no plano estão mascarados; os 2 restantes são referências sem valor.

$ git ls-files financas_bot_telegram/http/http-client.env.json
(vazio — destrackeado)

$ git check-ignore -v financas_bot_telegram/http/http-client.env.json
.gitignore:54:http-client.env.json	financas_bot_telegram/http/http-client.env.json

$ jq empty financas_bot_telegram/http/http-client.env.json.example
JSON valido

$ bash -n financas_bot_telegram/infra/provision/bootstrap.sh
OK

$ mvn -f financas_bot_telegram/pom.xml test
Tests run: 422, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS  (46,5 s)
```

### Verificação funcional do `bootstrap.sh` (além do `bash -n`)

O plano e o dispatch avisam que este é o único ponto executável tocado e que **não tem cobertura de CI**. `bash -n` só prova sintaxe, então o bloco do keystore foi **executado de verdade** num harness: o script foi renderizado como o `templatefile` do Terraform renderiza (`$${` → `${`, `${domain_name}` interpolado), o bloco `KEYSTORE_PATH=...fi` foi extraído do arquivo renderizado (não reescrito à mão) e rodado com stubs de `dnf`, `curl` (IMDS) e `aws`.

| Caso | Cenário simulado | Caminho de falha exercitado | Resultado |
|---|---|---|---|
| 1 | secret íntegro | — (caminho feliz) | `keystore.p12` gerado; abre com a senha vinda do secret; **não** abre com a senha literal antiga |
| 2 | `{"db_password":"x"}` | chave ausente | `ERRO: chave 'keystore_password' ausente ou vazia...` + `exit 1`, sem keystore |
| 3 | `{"keystore_password":""}` | chave vazia | idem caso 2 |
| 4 | `isto-nao-e-json` | secret não parseável | `ERRO: conteúdo de finbot-prod-secrets não é JSON parseável.` + `exit 1`, sem keystore |
| 5 | `aws` sai 255 (AccessDenied) | `get-secret-value` falhando | `ERRO: falha ao ler o segredo finbot-prod-secrets. Verifique a policy...` + `exit 1`, sem keystore |
| 6 | `PATH` sem o AWS CLI | binário ausente | `ERRO: AWS CLI ausente — impossível ler keystore_password...` + `exit 1`, sem keystore |

**Os 4 caminhos de falha do script estão cobertos** (casos 2/3 são o mesmo caminho com entradas diferentes). Nenhum produz keystore com senha vazia.

No caso 1, dois ruídos do ambiente Windows apareceram e **não são defeitos do script**: o MSYS converte o argumento `-subj "/CN=..."` em caminho (contornado com `MSYS2_ARG_CONV_EXCL=/CN=`) e o `chown finbot:finbot` falha porque não existe usuário `finbot` local — ambas as linhas são idênticas às de antes deste FIX.

O harness fica em scratchpad, não versionado — não há camada de teste para user_data no projeto e criar uma está fora do escopo deste FIX (registrado abaixo como pendência técnica).

---

## Correções pós-revisão

Foram **2 rodadas de revisão**. Na rodada 1 o Reviewer aprovou com ressalvas e apontou **1 correção obrigatória** (R1). Correções em `ab12ba4`; a rodada 2 **aprovou sem correções obrigatórias**. Relatório: `docs/sprints/03-folha-pagamento/avaliacoes/FIX-007-remover-segredos-versionados.md`.

**R1 (alto, obrigatório) — dependência de IMDSv1 sem fallback.** A primeira versão descobria a região com `curl http://169.254.169.254/latest/meta-data/placement/region` e abortava se falhasse. AMIs AL2023 são publicadas com `imds-support = v2.0`, então instâncias lançadas a partir delas nascem com `HttpTokens = required`, e `ec2.tf:18-41` não declara `metadata_options`. Um GET sem token falharia — e o bloco **só roda em recreate**, exatamente o cenário que aplica esse default. O Reviewer reproduziu o aborto. As 5 linhas foram **removidas**: o AWS CLI v2 resolve região e credenciais sozinho via IMDS negociando token IMDSv2. Se ainda assim falhar, o caminho de erro do `get-secret-value` pega, com mensagem que agora cita explicitamente região/credenciais. O comentário no script registra por que a consulta manual ao IMDS foi deliberadamente evitada.

Na rodada 2 o Reviewer reexecutou o bloco renderizado sob `set -euo pipefail` com um stub de `curl` emulando `HttpTokens=required` (GET sem token → `curl -sf` sai 22) e confirmou: **o cenário que antes abortava o provisionamento agora completa** (`exit 0`, keystore gerado). Adicionou 4 casos além dos meus, caçando o caminho silencioso — `aws` saindo 253 por região ausente, 253 por credenciais ausentes, **0 com saída vazia**, e `None` (secret binário). Todos caem em algum handler com `exit 1` e sem keystore. Nenhum caminho silencioso restou.

**R2 (médio) — cobertura superdeclarada do harness.** A versão anterior afirmava 4 caminhos de falha e exercitava 2; pior, o stub de `curl` respondia `us-east-1` incondicionalmente, ou seja, **a fixture afirmava a premissa que carregava o risco em vez de testá-la** — exatamente o defeito que o R1 revelou. Corrigido: o caminho de IMDS deixou de existir e o caso 6 (AWS CLI ausente) foi adicionado. A tabela acima agora reflete cobertura real.

**R3 (médio) — blast radius maior que "sem keystore".** O `exit 1` acontece antes do `systemctl start finbot/caddy`. Uma falha de secret deixa a instância recém-criada **sem reverse proxy e sem aplicação**, não só sem keystore. Redigido na pendência 2. Na rodada 2 o Reviewer corrigiu minha redação, que era otimista: eu havia escrito que um reboot recupera os serviços, e isso vale **só para o Caddy** — o finbot não volta, porque o keystore que falta é justamente o que `application-prod.properties:7` exige, e `user_data` não reexecuta em reboot. A pendência 2 agora diz isso. Mover o bloco para depois do start dos serviços é decisão do planner, não deste FIX.

**R4 (baixo)** — `commits:` do frontmatter completado. Não lista o commit que edita o próprio frontmatter (autorreferência impossível); o Reviewer aceitou.

**R5 (baixo, planner)** — o comando no critério de aceitação reescrito tinha `<sha-anterior>` como placeholder e faltava o `tr -d '[:space:]'`; corrigido no plano para a forma exata que foi executada. A ratificação da reformulação em si continua pendente do planner (pendência 3).

Na rodada 2 o Reviewer notou que o **critério vizinho** também era frouxo: `git grep -n "UJ"` busca um fragmento de 2 caracteres, e ele mediu que o maior prefixo comum entre a `admin_key` e qualquer outro token do repo é exatamente 2 — ou seja, o critério não verificava nada. Reescrito para buscar o valor inteiro recuperado de `ad6a60a`, mesmo padrão dos demais. Entra na mesma pendência de ratificação.

**R6 (baixo, operacional)** e **R7 (informativo, pré-existente)** — registrados como pendências abaixo; nenhum é regressão deste FIX. O Reviewer confirmou o R7 **empiricamente**, não em tese: no caso feliz do harness o cert saiu com `subject=CN=localhost`, porque o `curl` de `public-ipv4` (linha 137, pré-existente) falha sob `HttpTokens=required` e cai no `|| echo "localhost"`. O comentário novo no script foi ajustado para não dar a entender que o script inteiro deixou de falar IMDSv1.

---

## Desvios do plano

**2 desvios.**

**1. O plano mascarou o próprio arquivo do plano.** `docs/sprints/03-folha-pagamento/plans/FIX-007-remover-segredos-versionados.md` continha a senha literal **5 vezes** e está em `develop` num repo público. O critério de aceitação ("zero ocorrências na árvore de trabalho") era literalmente insatisfatível sem tocar no plano, e deixá-lo intacto anularia o objetivo do FIX — a senha continuaria indexável. As 5 ocorrências foram mascaradas. Em 3 delas bastou trocar o valor. Em 2 o literal era **argumento de busca**, não valor citado, e mascarar sem mais nada produziria um comando errado:

- §Critérios de aceitação: o critério era `git grep -n "<literal>"`. Foi reescrito para descrever a mesma verificação, com a mesma severidade ("retorna **zero** ocorrências"), mas recuperando o literal do histórico em vez de embuti-lo. **O critério não foi afrouxado.**
- §Referências: "Ocorrências de `<literal>` em docs" → "Ocorrências da senha literal do keystore em docs". A lista de arquivos/linhas ficou intacta.

Na rodada 2, por indicação do Reviewer, um **terceiro** critério de aceitação foi reescrito pelo mesmo motivo: `git grep -n "UJ"` buscava um fragmento de 2 caracteres da `admin_key`, o que não verificava nada. Passou a buscar o valor inteiro recuperado de `ad6a60a`.

`docs/plans/` é território do planner. A mudança é de valor, não estrutural, e `docs/**` está no território declarado deste FIX — mas o planner deve revisar se aceita a reformulação do próprio critério.

**2. A verificação IAM não estava onde o plano dizia.** O plano apontava `infra/iam*.tf`; a policy está em `infra/security.tf`. A verificação foi feita mesmo assim (tabela acima) e a conclusão é a que o plano previa para o caso "se tiver". Nenhuma permissão foi criada.

### Coisas do plano que a realidade não confirmou (sem impacto)

- §Referências lista `runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md:208` entre as ocorrências da senha do keystore. **Não há** — a linha 202 fala de `keystore_password` (nome da chave), não do valor. O total de 18 em `docs/` continua correto sem ela; a soma dos demais arquivos citados dá exatamente 18.

---

## Decisões tomadas durante a execução

- **Secret id `finbot-prod-secrets` hardcoded no `bootstrap.sh`**, como o plano escreveu explicitamente, em vez de passá-lo via `templatefile` a partir de `finbot-${var.env}-secrets`. Mantém o escopo no arquivo que o plano autorizou e evita mexer em `ec2.tf`. Custo: o script fica acoplado a prod (registrado como pendência técnica abaixo).
- **Região resolvida pelo próprio AWS CLI v2**, sem consulta manual ao IMDS e sem fallback `us-east-1`. Era o inverso na primeira versão; ver R1 em §Correções pós-revisão. Um default silencioso continua descartado — esconderia erro de provisionamento.
- **`jq` instalado junto do `openssl`** no mesmo `dnf install`. `SecretString` volta como string JSON; `--query` do CLI não entra nela. `jq` está no repo do AL2023.
- **`-passout env:KEYSTORE_PASSWORD`** em vez de `pass:$KEYSTORE_PASSWORD` — `pass:` deixaria a senha visível em `ps` durante o `openssl pkcs12`.
- **`TOKEN_AQUI` como placeholder no `.http`** em vez de inventar um novo — o comentário do passo 1 já mandava substituir exatamente esse texto.
- **Placeholders do runbook apontam a property, não só o arquivo.** `<sua-admin-api-key>` sozinho obrigaria o leitor a caçar; com `app.admin.api-key` ele resolve direto.

---

## Decisões pendentes (esperando humano)

> **Encerradas em 2026-08-12 pelo planner, por decisão do humano no fechamento da sprint 03.** A task passou de `parcial` para `concluido`. Duas pendências foram migradas para `docs/PENDENCIAS-TECNICAS.md`, uma foi ratificada pelo planner e uma já estava feita — **nenhuma foi descartada**. Estado de cada uma marcado abaixo; o texto original fica como registro.
>
> | # | Destino |
> |---|---|
> | 1 | 🔴 **migrada** → §"Rotacionar os segredos que estiveram versionados (FIX-007)" — **prioridade alta, segue em aberto** |
> | 2 | 🟡 **migrada** → §"`keystore_password` não confirmado em `finbot-prod-secrets`" |
> | 3 | ✅ **ratificada** pelo planner em 2026-08-12 (ver nota ao fim do item) |
> | 4 | ✅ **feita** — PR #121 mergeado em `develop`, e daí para `main` via #122 |

**4 pendências.**

1. **Rotacionar os segredos.** Este FIX **não é remediação** — só interrompe a exposição contínua. Os valores já commitados num repo público devem ser considerados coletados. Rotacionar: `admin_api_key` de dev, `keystore_password` (em `finbot-prod-secrets` **e** reassinando o `/opt/finbot/keystore.p12` da EC2, senão o app não abre o keystore), senha do MySQL local. O token do Telegram gen-1 já foi rotacionado em 2026-08-09.
2. **Confirmar que a chave `keystore_password` existe em `finbot-prod-secrets` antes do próximo recreate da EC2.** `docs/sprints/01-mvp/status/DEP-07.md:98` afirma que sim ("confirmado"), mas isso é relato de 2026-05 e **não foi verificado nesta task** — não há credencial AWS nesta sessão. Mudança real de blast radius: antes o bootstrap sempre passava; agora, se a chave não vier, ele **aborta o provisionamento** com `exit 1`. E o `exit 1` ocorre **antes** do `systemctl start finbot`/`caddy`, então a instância recém-criada fica **sem reverse proxy e sem aplicação**, não apenas sem keystore. Um reboot recupera **só o Caddy** (`systemctl enable` na linha 113): o finbot **não** volta, porque `application-prod.properties:7` aponta `server.ssl.key-store=/opt/finbot/keystore.p12` — exatamente o arquivo que não foi criado — e o `Restart=on-failure` do unit o joga em crash-loop; `user_data` não reexecuta em reboot, então o keystore não é gerado sozinho. Recuperar exige intervenção manual (popular o secret e rodar o bloco do bootstrap à mão, ou recriar a instância). Falhar alto continua sendo o comportamento correto — gerar keystore com senha errada quebraria o boot do app de forma mais obscura. Cabe ao planner decidir se vale mover o bloco do keystore para depois do start dos serviços.
3. **Ratificar (planner) a reformulação do §Critérios de aceitação e da §Referências do plano** — ver Desvio 1. `docs/plans/` é território do planner; a mudança está feita e declarada, mas não autorizada previamente. O Reviewer considerou a substância correta e o critério não afrouxado, e pediu ratificação formal.

   > ✅ **Ratificado pelo planner em 2026-08-12.** Li o §Desvios antes de ratificar. A reformulação era **obrigatória, não discricionária**: o próprio arquivo do plano continha a senha literal 5 vezes e está em `develop` num repositório público — o critério "zero ocorrências na árvore de trabalho" era insatisfatível sem tocá-lo, e deixá-lo intacto anularia o objetivo do FIX. Nos 3 critérios reescritos o literal era **argumento de busca**, não valor citado; mascarar sem reformular produziria um comando que não verifica nada (foi exatamente o caso do `git grep -n "UJ"`, fragmento de 2 caracteres que o Reviewer pegou na rodada 2). A severidade foi preservada em todos ("retorna **zero** ocorrências") e o literal passou a ser recuperado do histórico. **Nenhum critério foi afrouxado.**
4. **Push da branch, abertura do PR para `develop` e merge.** Não executados — nada foi enviado ao remoto.

---

## Próximos passos / observações pro próximo

- **O `bootstrap.sh` só é exercitado em recreate da EC2.** Se o próximo recreate falhar no keystore, o log tem a mensagem exata: `journalctl -u cloud-init` ou `/var/log/bootstrap.log`, prefixo `[bootstrap] ERRO:`.
- **Contrato entre o script e a aplicação:** a senha que o `bootstrap.sh` usa para gerar o p12 e a que `application-prod.properties:8` lê (`server.ssl.key-store-password=${keystore_password}`) são **a mesma chave do mesmo segredo**. Trocar o valor no Secrets Manager sem reassinar o keystore existente quebra o boot do app — o keystore só é regerado quando `/opt/finbot/keystore.p12` não existe.
- **Escapes do `templatefile`:** o `bootstrap.sh` é template do Terraform. `${...}` é interpolação; para um `${...}` literal em bash é preciso `$${...}`. O comentário novo sobre `keystore_password` usa `$${` justamente por isso. `$VAR` sem chaves passa direto. O Reviewer rodou um simulador de `templatefile` e confirmou **zero variáveis desconhecidas** — o `terraform plan` não quebra.
- **Worktree do planner** (`financas_bot_telegram-planner`) ainda tem `http-client.env.json` rastreado e vai perder a cópia local no `pull` pós-merge. Impacto baixo — o arquivo é do fluxo do IntelliJ, não do planner; se importar, salvar cópia antes.
- Um novo dev agora precisa copiar `http-client.env.json.example` → `http-client.env.json` antes de usar o `.http` no IntelliJ. O passo 0 no cabeçalho do arquivo diz isso.

### Pendências técnicas identificadas (para o planner consolidar)

1. **`bootstrap.sh` acoplado a prod.** O secret id `finbot-prod-secrets` está hardcoded, enquanto o Terraform parametriza `finbot-${var.env}-secrets` (`security.tf:25`). Provisionar uma EC2 de dev com este script quebraria. Correção natural: passar `secret_id` via `templatefile` em `ec2.tf:26-28`. Fora do escopo autorizado deste FIX.
2. **`user_data` não tem camada de teste.** O harness usado aqui é descartável. Um script de smoke reutilizável em `infra/provision/` daria cobertura repetível ao único executável do repo que o CI não toca.
3. **Sem gate de secret scanning no CI.** É a causa-raiz: `docs/sprints/01-mvp/plans/FIX-keystore-password-secret.md` moveu a senha para o Secrets Manager **e documentou o valor em claro**, anulando a própria mitigação. O plano já aponta `CI-002`.
4. **`infra/prod.tfvars`** ainda tem Account ID e `my_ip`. O plano classificou como não-segredo e adiou; segue em aberto.
5. **Porta 8443 aberta no security group** — adiamento deliberado do plano (rollback do webhook para a Caddy). Revisitar após alguns dias de tráfego limpo.
6. **`bootstrap.sh:137` depende de IMDSv1 e degrada em silêncio** (pré-existente, não é regressão deste FIX). Sob `HttpTokens=required` — o default de instâncias novas em AL2023 — o `curl` de `public-ipv4` falha e o `|| echo "localhost"` gera um cert self-signed com `CN=localhost`. Correção natural: buscar token IMDSv2 uma vez e reusar, ou declarar `metadata_options` em `ec2.tf`. Levantado pelo Reviewer como R7.
7. **`ec2.tf:18-41` não declara `metadata_options`.** A instância fica com o default da AMI, que mudou entre AL2 e AL2023. Tornar explícito remove a classe inteira de surpresa que gerou o R1 e o R7.

---

## Padrões técnicos

Não se aplica — o FIX não altera código de aplicação nem lógica de domínio. O único artefato executável tocado é um script de provisionamento shell, fora da arquitetura hexagonal. Os 422 testes rodaram apenas como confirmação de ausência de regressão; nenhum teste novo foi criado porque nenhuma lógica Java foi adicionada.

---

## Arquivos criados/modificados

- `README.md` (modificado: token do Telegram e URL de ngrok → placeholders)
- `.gitignore` (modificado: passa a ignorar `http-client.env.json`)
- `docs/runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md` (modificado: senha MySQL local e admin api-key → placeholders com ponteiro para a property)
- `docs/PENDENCIAS-TECNICAS.md` (modificado: senha do keystore mascarada)
- `docs/architecture/estado-atual-dev.md` (modificado: idem, 2 ocorrências)
- `docs/sprints/01-mvp/avaliacoes/backend-polish-evo07.md` (modificado: idem)
- `docs/sprints/01-mvp/plans/FIX-keystore-password-secret.md` (modificado: idem, 4 ocorrências)
- `docs/sprints/01-mvp/status/FIX-keystore-password-secret.md` (modificado: idem, 4 ocorrências)
- `docs/sprints/01-mvp/status/DEP-07.md` (modificado: idem, 2 ocorrências)
- `docs/sprints/01-mvp/status/_RESUMO-overnight-back-2.md` (modificado: idem, 3 ocorrências)
- `docs/sprints/01-mvp/status/_RESUMO-overnight-deploy.md` (modificado: idem)
- `docs/sprints/03-folha-pagamento/plans/FIX-007-remover-segredos-versionados.md` (modificado: 5 ocorrências no próprio plano — ver Desvio 1)
- `financas_bot_telegram/http/BE-16-testes.http` (modificado: token de sessão → `TOKEN_AQUI`; passo 0 apontando o `.example`)
- `financas_bot_telegram/http/http-client.env.json` (destrackeado via `git rm --cached`; arquivo local preservado)
- `financas_bot_telegram/http/http-client.env.json.example` (novo: mesma estrutura, `admin_key` como placeholder)
- `financas_bot_telegram/infra/provision/bootstrap.sh` (modificado: `keystore_password` lido de `finbot-prod-secrets`, 4 caminhos de falha explícitos com `exit 1`)
