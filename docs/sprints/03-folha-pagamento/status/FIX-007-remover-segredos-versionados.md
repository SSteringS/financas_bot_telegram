---
task: FIX-007
titulo: "Remover segredos versionados do estado atual do repositório (repo público)"
data: 2026-08-09
branch: fix/007-remover-segredos-versionados
responsavel: claude-back
estado: parcial
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
pr: null
desvios: 2
pendencias_humano: 3
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
- **`financas_bot_telegram/infra/provision/bootstrap.sh`** — o `-passout pass:<literal>` virou leitura de `keystore_password` a partir de `finbot-prod-secrets`, com **quatro caminhos de falha explícitos** (AWS CLI ausente, região não descoberta via IMDS, `get-secret-value` falhando, chave ausente/vazia/JSON inválido). Todos logam e fazem `exit 1` — nenhum gera keystore com senha vazia. O `-passout env:` (em vez de `pass:`) mantém a senha fora da lista de processos.

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

| Caso | Payload do `aws` stub | Resultado |
|---|---|---|
| 1 — caminho feliz | `{"keystore_password":"Kx7-senha-de-teste",...}` | `keystore.p12` gerado; abre com a senha vinda do secret; **não** abre com a senha literal antiga |
| 2 — chave ausente | `{"db_password":"x"}` | `ERRO: chave 'keystore_password' ausente ou vazia...` + `exit 1`, sem keystore |
| 3 — chave vazia | `{"keystore_password":""}` | idem caso 2 |
| 4 — secret não é JSON | `isto-nao-e-json` | `ERRO: conteúdo de finbot-prod-secrets não é JSON parseável.` + `exit 1`, sem keystore |
| 5 — `aws` falha (AccessDenied) | `exit 255` | `ERRO: falha ao ler o segredo finbot-prod-secrets. Verifique a policy...` + `exit 1`, sem keystore |

Nenhum caminho de erro produz keystore com senha vazia. No caso 1, dois ruídos do ambiente Windows apareceram e **não são defeitos do script**: o MSYS converte o argumento `-subj "/CN=..."` em caminho (contornado com `MSYS2_ARG_CONV_EXCL=/CN=`) e o `chown finbot:finbot` falha porque não existe usuário `finbot` local — ambas as linhas são idênticas às de antes deste FIX.

O harness fica em scratchpad, não versionado — não há camada de teste para user_data no projeto e criar uma está fora do escopo deste FIX (registrado abaixo como pendência técnica).

---

## Desvios do plano

**2 desvios.**

**1. O plano mascarou o próprio arquivo do plano.** `docs/sprints/03-folha-pagamento/plans/FIX-007-remover-segredos-versionados.md` continha a senha literal **5 vezes** e está em `develop` num repo público. O critério de aceitação ("zero ocorrências na árvore de trabalho") era literalmente insatisfatível sem tocar no plano, e deixá-lo intacto anularia o objetivo do FIX — a senha continuaria indexável. As 5 ocorrências foram mascaradas. Em 3 delas bastou trocar o valor. Em 2 o literal era **argumento de busca**, não valor citado, e mascarar sem mais nada produziria um comando errado:

- §Critérios de aceitação: o critério era `git grep -n "<literal>"`. Foi reescrito para descrever a mesma verificação, com a mesma severidade ("retorna **zero** ocorrências"), mas recuperando o literal do histórico em vez de embuti-lo. **O critério não foi afrouxado.**
- §Referências: "Ocorrências de `<literal>` em docs" → "Ocorrências da senha literal do keystore em docs". A lista de arquivos/linhas ficou intacta.

`docs/plans/` é território do planner. A mudança é de valor, não estrutural, e `docs/**` está no território declarado deste FIX — mas o planner deve revisar se aceita a reformulação do próprio critério.

**2. A verificação IAM não estava onde o plano dizia.** O plano apontava `infra/iam*.tf`; a policy está em `infra/security.tf`. A verificação foi feita mesmo assim (tabela acima) e a conclusão é a que o plano previa para o caso "se tiver". Nenhuma permissão foi criada.

### Coisas do plano que a realidade não confirmou (sem impacto)

- §Referências lista `runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md:208` entre as ocorrências da senha do keystore. **Não há** — a linha 202 fala de `keystore_password` (nome da chave), não do valor. O total de 18 em `docs/` continua correto sem ela; a soma dos demais arquivos citados dá exatamente 18.

---

## Decisões tomadas durante a execução

- **Secret id `finbot-prod-secrets` hardcoded no `bootstrap.sh`**, como o plano escreveu explicitamente, em vez de passá-lo via `templatefile` a partir de `finbot-${var.env}-secrets`. Mantém o escopo no arquivo que o plano autorizou e evita mexer em `ec2.tf`. Custo: o script fica acoplado a prod (registrado como pendência técnica abaixo).
- **Região via IMDS, sem default.** O CLI v2 resolveria a região sozinho, mas explícito falha de forma diagnosticável. Não há fallback `us-east-1` — cair para um default silencioso é exatamente o tipo de defensividade que esconde erro de provisionamento.
- **`jq` instalado junto do `openssl`** no mesmo `dnf install`. `SecretString` volta como string JSON; `--query` do CLI não entra nela. `jq` está no repo do AL2023.
- **`-passout env:KEYSTORE_PASSWORD`** em vez de `pass:$KEYSTORE_PASSWORD` — `pass:` deixaria a senha visível em `ps` durante o `openssl pkcs12`.
- **`TOKEN_AQUI` como placeholder no `.http`** em vez de inventar um novo — o comentário do passo 1 já mandava substituir exatamente esse texto.
- **Placeholders do runbook apontam a property, não só o arquivo.** `<sua-admin-api-key>` sozinho obrigaria o leitor a caçar; com `app.admin.api-key` ele resolve direto.

---

## Decisões pendentes (esperando humano)

**3 pendências.**

1. **Rotacionar os segredos.** Este FIX **não é remediação** — só interrompe a exposição contínua. Os valores já commitados num repo público devem ser considerados coletados. Rotacionar: `admin_api_key` de dev, `keystore_password` (em `finbot-prod-secrets` **e** reassinando o `/opt/finbot/keystore.p12` da EC2, senão o app não abre o keystore), senha do MySQL local. O token do Telegram gen-1 já foi rotacionado em 2026-08-09.
2. **Confirmar que a chave `keystore_password` existe em `finbot-prod-secrets` antes do próximo recreate da EC2.** `docs/sprints/01-mvp/status/DEP-07.md:98` afirma que sim ("confirmado"), mas isso é relato de 2026-05 e **não foi verificado nesta task** — não há credencial AWS nesta sessão. Se a chave não existir, o `bootstrap.sh` agora **aborta o provisionamento** em vez de gerar um keystore com senha literal. É o comportamento correto, e é uma mudança real de blast radius: antes o bootstrap sempre passava.
3. **Push da branch, abertura do PR para `develop` e merge.** Não executados — nada foi enviado ao remoto.

---

## Próximos passos / observações pro próximo

- **O `bootstrap.sh` só é exercitado em recreate da EC2.** Se o próximo recreate falhar no keystore, o log tem a mensagem exata: `journalctl -u cloud-init` ou `/var/log/bootstrap.log`, prefixo `[bootstrap] ERRO:`.
- **Contrato entre o script e a aplicação:** a senha que o `bootstrap.sh` usa para gerar o p12 e a que `application-prod.properties:8` lê (`server.ssl.key-store-password=${keystore_password}`) são **a mesma chave do mesmo segredo**. Trocar o valor no Secrets Manager sem reassinar o keystore existente quebra o boot do app — o keystore só é regerado quando `/opt/finbot/keystore.p12` não existe.
- **Escapes do `templatefile`:** o `bootstrap.sh` é template do Terraform. `${...}` é interpolação; para um `${...}` literal em bash é preciso `$${...}`. O comentário novo sobre `keystore_password` usa `$${` justamente por isso. `$VAR` sem chaves passa direto.
- Um novo dev agora precisa copiar `http-client.env.json.example` → `http-client.env.json` antes de usar o `.http` no IntelliJ. O passo 0 no cabeçalho do arquivo diz isso.

### Pendências técnicas identificadas (para o planner consolidar)

1. **`bootstrap.sh` acoplado a prod.** O secret id `finbot-prod-secrets` está hardcoded, enquanto o Terraform parametriza `finbot-${var.env}-secrets` (`security.tf:25`). Provisionar uma EC2 de dev com este script quebraria. Correção natural: passar `secret_id` via `templatefile` em `ec2.tf:26-28`. Fora do escopo autorizado deste FIX.
2. **`user_data` não tem camada de teste.** O harness usado aqui é descartável. Um script de smoke reutilizável em `infra/provision/` daria cobertura repetível ao único executável do repo que o CI não toca.
3. **Sem gate de secret scanning no CI.** É a causa-raiz: `docs/sprints/01-mvp/plans/FIX-keystore-password-secret.md` moveu a senha para o Secrets Manager **e documentou o valor em claro**, anulando a própria mitigação. O plano já aponta `CI-002`.
4. **`infra/prod.tfvars`** ainda tem Account ID e `my_ip`. O plano classificou como não-segredo e adiou; segue em aberto.
5. **Porta 8443 aberta no security group** — adiamento deliberado do plano (rollback do webhook para a Caddy). Revisitar após alguns dias de tráfego limpo.

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
