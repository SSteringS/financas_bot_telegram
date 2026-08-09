---
task: FIX-007
sprint: 03-folha-pagamento
data: 2026-08-09
avaliador: claude-reviewer
status_report: docs/sprints/03-folha-pagamento/status/FIX-007-remover-segredos-versionados.md
veredito_codigo: aprovado_com_ressalvas
veredito_final: aprovado_com_ressalvas
correcoes_obrigatorias: 1                  # R1 — dependência de IMDSv1 no bootstrap.sh
observacoes_count: 7
rodadas: 1
roteiro_executado: false
gates_verificados_contra_realidade: confere
skills_eficazes: []
skills_gaps: []
veredito_qa: nao_aplicavel
fluxos_qa_executados: []
fluxos_qa_adicionar: []
fluxos_qa_remover: []
---

# Avaliação — FIX-007 (remover segredos versionados do estado atual do repositório)

**Branch:** `fix/007-remover-segredos-versionados` (`d1c5e08` implementação, `49856b5` status)
**Base:** `ad6a60a` (= ponta de `develop`)
**Implementador:** claude-back
**Plano:** `docs/sprints/03-folha-pagamento/plans/FIX-007-remover-segredos-versionados.md`

> **Nota de higiene deste artefato:** nenhum literal de segredo aparece aqui. Todas as verificações abaixo recuperaram os valores de `ad6a60a` via variável de shell. Este arquivo é público como o resto do repo.

> **Nota de localização:** o dispatch do agente pede `ia-docs/features/<FEATURE-FOLDER>/avaliacoes/`. Essa árvore não existe neste repositório; segui a convenção vigente do `CLAUDE.md` (`docs/sprints/<NN>-<slug>/avaliacoes/`).

---

## 0. Checagem de premissas (feita antes de julgar o código)

| # | Premissa | Resultado | Como foi verificado |
|---|---|---|---|
| P1 | Arquitetura: a mudança não toca a hexagonal do backend | **pass** | `git show --name-only d1c5e08` — zero arquivos sob `financas_bot_telegram/src/`. O único executável é script de provisionamento, fora da arquitetura |
| P2 | O instance profile da EC2 tem `secretsmanager:GetSecretValue` sobre `finbot-prod-secrets` | **pass** | Cadeia lida por mim em `security.tf` + `ec2.tf` — ver §2 |
| P3 | O nome da chave lida pelo script bate com o que a aplicação espera | **pass** | `bootstrap.sh:162` lê `.keystore_password`; `application-prod.properties:8` lê `${keystore_password}` do mesmo secret declarado em `application-prod.properties:2` |
| P4 | O template renderiza como bash válido **e** semanticamente correto depois do `templatefile` | **pass** | Renderização simulada + `bash -n` no renderizado + inspeção do bloco — ver §3 |
| P5 | O harness funcional prova o que afirma (independência de fixture) | **FAIL parcial** | O stub de `curl` do harness *afirma* a premissa mais arriscada em vez de testá-la — ver §4 e R1/R2 |
| P6 | O ambiente responde IMDS sem token (premissa implícita do código novo) | **not-checked / sem fonte autoritativa** | Não há credencial AWS nesta sessão. O default do AMI AL2023 aponta para o contrário — ver R1 |
| P7 | As 18 ocorrências em `docs/**` existiam e foram todas removidas | **pass** | Contagem independente em `ad6a60a` — ver §1 |

**P5/P6 são a razão de o veredito não ser "aprovado" limpo.** Não bloqueiam a limpeza de segredos, que está correta e comprovada; bloqueiam o `bootstrap.sh` como está.

---

## 1. Critérios de aceitação — verificação independente

Todos os `git grep` abaixo foram rodados por mim, recuperando o literal de `ad6a60a` via `$(git show …)` numa variável de shell. Nenhum valor foi escrito em arquivo.

| # | Critério | Resultado | Evidência |
|---|---|---|---|
| 1 | Senha do keystore: zero ocorrências na árvore | **ok** | `git grep --text -F "$SECRET"` → `exit=1`. Também com `--untracked` → `exit=1` |
| 2 | `git grep -nE "[0-9]{8,10}:AA[A-Za-z0-9_-]{30,}"` zero | **ok** | `exit=1` |
| 3 | `admin_key` de dev fora do `.env.json` e do runbook | **ok** | grep do literal (44 chars) → `exit=1`. Os 3 pontos do plano estão mascarados; sobram só referências sem valor (`runbook:186`, `BE-16-testes.http:19` via `{{admin_key}}`) |
| 4 | `git ls-files …/http-client.env.json` vazio | **ok** | `git ls-files financas_bot_telegram/http/` lista só `BE-16-testes.http` e o `.example` |
| 5 | `.example` existe e é JSON válido | **ok** | `json.load` OK; `admin_key` é placeholder descritivo, **nenhum segredo novo** |
| 6 | `.gitignore` cobre `http-client.env.json` | **ok** | `git check-ignore -v` → `.gitignore:54`. O arquivo local do humano continua no disco (254 bytes, mtime de 23/mai preservado) |
| 7 | `bootstrap.sh` sem literal; lê do secret; falha explícita | **ok** com ressalva | Lógica correta; ver R1/R3 |
| 8 | `bash -n` no script | **ok** | Passa no template **e** no renderizado |
| 9 | Runbook continua seguível | **ok** | Lido de ponta a ponta; ver §5 |
| 10 | `mvn test` verde | **ok** | Reproduzido por mim: 81 classes, **Tests run: 422, Failures: 0, Errors: 0, Skipped: 0** |
| 11 | Branch saiu de `develop` | **ok** | `git merge-base develop HEAD` = `ad6a60a` = `git rev-parse develop` |
| 12 | Status report com frontmatter válido incl. resultado IAM | **ok** | Confere com `docs/templates/_TEMPLATE-status.md`; `estado: parcial` é o valor certo com `pendencias_humano: 3` |

**Contagem das 18 ocorrências (independente).** Em `ad6a60a`, linhas com o literal: `PENDENCIAS-TECNICAS.md` 1, `architecture/estado-atual-dev.md` 2, `01-mvp/avaliacoes/backend-polish-evo07.md` 1, `01-mvp/plans/FIX-keystore-password-secret.md` 4, `01-mvp/status/DEP-07.md` 2, `01-mvp/status/FIX-keystore-password-secret.md` 4, `01-mvp/status/_RESUMO-overnight-back-2.md` 3, `01-mvp/status/_RESUMO-overnight-deploy.md` 1 = **18 linhas em 8 arquivos**, exatamente como o status report afirma; mais 5 no próprio plano do FIX-007 e 1 no `bootstrap.sh`. Todas zeradas em `HEAD`.

**Confirmo o achado do implementador:** `runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md:208` listado na §Referências do plano **não** contém o literal em `ad6a60a` (zero matches naquele arquivo). O plano está errado nesse ponto; o implementador reportou e não alterou — correto, é território do planner (R5).

**Varredura extra minha (não pedida pelo plano):** `AKIA[0-9A-Z]{16}`, `BEGIN … PRIVATE KEY`, `aws_secret_access_key` → zero. Heurística de `password|secret|token = <valor longo>` fora de `docs/` → só placeholders (`CHANGE_ME`, `NAO_CONFIGURADO`) e fixtures de teste (`application-integration-test.properties:10-11`). `terraform.tfstate` **não** está versionado (`.gitignore:35`). Nenhum segredo residual encontrado.

---

## 2. Verificação IAM — confirmada, com arquivo e linha (li eu mesmo)

O plano mandava olhar `infra/iam*.tf`. O único arquivo com esse glob é `iam-github-oidc.tf` (role OIDC do GitHub Actions) — o desvio 2 do implementador procede. A cadeia real, que eu li linha a linha:

| Linha | Conteúdo | Papel |
|---|---|---|
| `financas_bot_telegram/infra/security.tf:2-15` | `aws_iam_role.ec2_role`, assume role de `ec2.amazonaws.com` | role da EC2 |
| `security.tf:18-21` | `aws_iam_instance_profile.ec2_profile` → `role = aws_iam_role.ec2_role.name` | instance profile |
| `financas_bot_telegram/infra/ec2.tf:24` | `iam_instance_profile = aws_iam_instance_profile.ec2_profile.name` | profile ligado à instância |
| `security.tf:24-26` | `data "aws_secretsmanager_secret" "app_secrets"` com `name = "finbot-${var.env}-secrets"` | resolve o segredo |
| `financas_bot_telegram/infra/prod.tfvars:1` | `env = "prod"` | → `finbot-prod-secrets`, o mesmo id hardcoded em `bootstrap.sh:156` |
| `security.tf:36` | `Action = ["secretsmanager:GetSecretValue"]` | ação concedida |
| `security.tf:37` | `Resource = [data.aws_secretsmanager_secret.app_secrets.arn]` | escopo correto |
| `security.tf:42-45` | `aws_iam_role_policy_attachment.ec2_secrets_attach` | policy anexada à role |

**Cadeia fechada. Caso "se tiver" do plano. Nenhuma permissão IAM foi adicionada pelo FIX** (`git show --name-only d1c5e08` não inclui nenhum `.tf`). A tabela do status report §"permissão IAM" confere linha a linha com o que li.

---

## 3. Terraform `templatefile` — renderização verificada

Não confiei no `bash -n` do template. Escrevi um simulador do `templatefile` (escapes `$${`/`%%{`, interpolação de variáveis conhecidas, detecção de variáveis desconhecidas) e renderizei `bootstrap.sh` com `domain_name` preenchido.

- **Variáveis desconhecidas: 0.** Se houvesse um `${…}` não escapado com nome fora do mapa de `ec2.tf:26-28`, `terraform plan` quebraria. Não há.
- As três sequências no arquivo são: `${domain_name}` (linha 7, interpolação legítima), `$${CADDY_VERSION}` (linha 62, pré-existente) e `$${keystore_password}` (linha 141, **novo**).
- A linha 141 renderiza para `# … (server.ssl.key-store-password=${keystore_password}),` — **dentro de um comentário bash**, portanto nunca expandida em runtime. Correto.
- `bash -n` no arquivo **renderizado**: passa.
- `%{ … }` (diretivas de template): nenhuma ocorrência.

**Contrato com a aplicação: bate.** `bootstrap.sh:156` lê `finbot-prod-secrets`; `application-prod.properties:2` importa `aws-secretsmanager:finbot-prod-secrets`. `bootstrap.sh:162` extrai `.keystore_password`; `application-prod.properties:8` resolve `${keystore_password}`. Mesmo segredo, mesma chave, grafia idêntica.

---

## 4. `bootstrap.sh` lido linha a linha + harness reexecutado

### O que está certo (e é melhoria real)

- `-passout env:KEYSTORE_PASSWORD` (linha 180) em vez de `pass:` — tira a senha de `ps`. Ganho concreto sobre o estado anterior.
- `jq -r '.keystore_password // empty'` (162) + teste `-z` (166) colapsa "ausente", "null" e "vazia" num caminho de erro só. Correto.
- `set -euo pipefail` (linha 5) × `VAR=$(cmd) || { … }`: a interação é a esperada — `pipefail` faz o pipeline do `jq` propagar falha para o `||`. **Verifiquei empiricamente**, não por leitura.
- Idempotência preservada: o bloco só roda se `keystore.p12` não existir (135).
- `unset KEYSTORE_PASSWORD SECRET_JSON` (181) após o uso.
- Nenhum caminho gera keystore com senha vazia — reproduzi os 4 casos de erro e em todos `keystore.p12` não é criado.

### Reexecução independente do harness

Rodei o `harness.sh` do implementador contra a minha própria renderização:

| Caso | Resultado obtido por mim |
|---|---|
| feliz | `keystore.p12` gerado; **abre** com a senha do secret; **não abre** com a senha literal antiga |
| chave ausente | mensagem correta + `exit 1`, sem keystore |
| não-JSON | mensagem correta + `exit 1`, sem keystore |
| `aws` exit 255 | mensagem correta + `exit 1`, sem keystore |

O harness **não é circular na extração**: `awk '/^KEYSTORE_PATH=/,/^fi$/'` pega o bloco real do arquivo renderizado (os `fi` internos são indentados, então o range fecha no `fi` da linha 186 — conferi). A asserção do caso feliz é forte de verdade: abre com a senha nova, não abre com a antiga.

**Mas o harness é frouxo em dois pontos, e um deles é exatamente o ponto de maior risco** — ver R1 e R2.

---

## 5. Docs, runbook e `.example`

- **Narrativa histórica preservada.** Percorri o diff dos 8 arquivos de `docs/`: toda substituição é do valor na mesma linha, sem reescrita da prosa ao redor. Os status reports do 01-mvp continuam legíveis como registro histórico. Requisito do plano cumprido.
- **Runbook seguível.** Li `ROTEIRO-INTEGRACAO-FRONT-BACK.md` como se fosse executá-lo do zero. Os dois placeholders apontam a property exata de onde tirar o valor, e o `curl` do passo 0.3 (linha 99) ganhou uma linha de instrução (101). **Confirmei que as properties citadas existem**: `application-dev.properties.example:17` (`spring.datasource.password`) e `:47` (`app.admin.api-key`). A linha 186 ("Usar a chave da tabela 'Dados úteis'") continua coerente porque a tabela agora ensina como obter o valor.
- **`.example` limpo.** Estrutura idêntica ao original, `admin_key` como texto descritivo. **Nenhum segredo novo entrou** — nem no `.example`, nem no status report (que deliberadamente não cola literal nenhum; decisão certa).
- **`BE-16-testes.http`** ficou coerente: o passo 0 novo (linhas 5-6) fecha o buraco criado pelo destrackeamento, e `TOKEN_AQUI` casa com o comentário pré-existente da linha 27.

---

## 6. Julgamento do Desvio 1 (mascarar e reescrever o próprio plano)

**(a) Foi a decisão certa?** **Sim, na substância.** O arquivo do plano está em `develop` num repo público e continha o literal 5 vezes — deixá-lo intacto anularia o objetivo do FIX e tornaria o critério de aceitação insatisfatível. `docs/**` está no território declarado do próprio plano (§Coordenação). Não havia leitura em que "não tocar" fosse a resposta correta.

**(b) O critério foi afrouxado?** **Não.** Comparei as duas redações: a severidade ("retorna **zero** ocorrências na árvore de trabalho") é idêntica, e a nova forma é *mais* auditável, porque descreve como recuperar o literal sem reintroduzi-lo. Duas imprecisões cosméticas, sem efeito sobre o rigor: o `<sha-anterior>` é placeholder (não é copy-paste executável) e falta o `tr -d '[:space:]'` que o status report usou na prática.

**(c) Deveria ter parado e perguntado?** **Parcialmente.** Mascarar as 3 ocorrências que eram só valor não exigia autorização nenhuma. Reescrever a §Critérios de aceitação e a §Referências mexe na semântica de um artefato do planner — o caminho ideal era uma pergunta de uma linha. Como (i) o desvio foi declarado, (ii) a mudança é mínima e reversível e (iii) a alternativa era entregar o FIX incompleto, considero **aceitável ex-post**, condicionado a **ratificação explícita do planner** (R5). Não bloqueio por isso.

---

## 7. Findings

### R1 — obrigatório antes do merge | **alto** | `financas_bot_telegram/infra/provision/bootstrap.sh:149-153`

O código novo introduz uma dependência dura de **IMDSv1** (GET sem token em `169.254.169.254`) **sem fallback**, e faz `exit 1` se ela falhar.

AMIs do Amazon Linux 2023 são publicadas com `imds-support = v2.0`; instâncias lançadas a partir delas recebem `HttpTokens = required` por padrão. `ec2.tf:18-41` **não declara `metadata_options`**, então o default do AMI vale. Sob IMDSv2 obrigatório, um GET sem `X-aws-ec2-metadata-token` responde 401, `curl -sf` sai com 22, e o script aborta.

Isso não é hipotético no momento em que importa: o bloco **só roda em recreate**, e recreate significa exatamente "instância nova lançada do AMI AL2023" — o cenário em que o default IMDSv2 se aplica.

Simulei um ambiente com IMDSv2 obrigatório contra o bloco real renderizado:

```
[bootstrap] Gerando keystore self-signed para o webhook...
[bootstrap] ERRO: não foi possível descobrir a região via IMDS. Abortando.
exit=1
```

O agravante é que **a região é uma constante conhecida em todo o resto do projeto** — `provider.tf:18` (`region = "us-east-1"`) e `application-prod.properties:12` (`spring.cloud.aws.region.static=us-east-1`). O script criou um ponto de falha para descobrir em runtime um valor que já é fixo em build time.

Correções possíveis, em ordem de preferência:

1. **Remover o bloco 149-153.** O AWS CLI v2 resolve a região sozinho e já fala IMDSv2 com token. Se não conseguir, o `aws` da linha 155 falha e o caminho de erro da 158 pega, com mensagem clara. Menor diff, elimina a premissa em vez de verificá-la, fica dentro do arquivo autorizado pelo plano.
2. Passar a região pelo `templatefile`: adicionar `aws_region = "us-east-1"` (ou uma var) em `ec2.tf:26-28` e trocar as linhas 149-153 por `export AWS_DEFAULT_REGION="${aws_region}"`. Determinístico, mas toca `ec2.tf` (fora do escopo que o plano autorizou).
3. Falar IMDSv2: buscar token com `curl -sf -X PUT …/latest/api/token -H "X-aws-ec2-metadata-token-ttl-seconds: 300"` e mandá-lo nos dois GETs. Só vale a pena se também quiserem consertar a linha 137 (ver R7).

**Enquanto R1 não for corrigido, o FIX não deve ser mergeado** — não pelo risco de segurança (não há), mas porque troca uma senha hardcoded por um provisionamento que provavelmente aborta no próximo recreate.

### R2 — médio | status report §"Verificação funcional" + `harness.sh:20-27`

A cobertura do harness está superdeclarada. O status report (linha 37) fala em "**quatro** caminhos de falha explícitos" e a tabela apresenta 5 casos como validação. Na prática o harness exercita **2 dos 4** caminhos de falha (`get-secret-value` falhando; chave ausente/vazia/JSON inválido). Não exercita:

- `command -v aws` ausente (`bootstrap.sh:144-147`);
- **falha do IMDS** (`bootstrap.sh:149-152`) — e pior: o stub de `curl` do harness responde `us-east-1` incondicionalmente, ou seja, **a fixture afirma a premissa que carrega o risco em vez de testá-la**. É o caso clássico de suíte verde que não é evidência.

Detalhe menor de leitura: no caso feliz o harness sai com `exit code: 1` por causa do `chown finbot:finbot` (usuário inexistente no Windows) — a atribuição do implementador ao ambiente está correta, mas significa que o caminho feliz foi verificado **até o `chown`**, não até o fim do bloco. O status report não superdeclara isso; só não diz.

**Ação:** ajustar o texto para descrever a cobertura real e, se o harness for promovido a script reutilizável (débito 2), incluir o caso IMDS-falhando como cenário de primeira classe.

### R3 — médio | `financas_bot_telegram/infra/provision/bootstrap.sh:135-186` vs `188-194`

Mudança de blast radius maior do que a pendência 2 do status report descreve. O `exit 1` do bloco do keystore acontece **antes** de `systemctl start finbot` (192) e `systemctl start caddy` (194) — verifiquei empiricamente que a execução não segue adiante. Ou seja: uma falha ao ler o secret não deixa apenas "sem keystore", deixa uma instância recém-criada **sem reverse proxy e sem aplicação**, e não só sem o TLS do webhook.

Mitigante: `systemctl enable finbot` (56) e `enable caddy` (113) já rodaram, então um reboot recupera os serviços. Ainda assim o estado imediato pós-`apply` é uma instância morta.

Falhar alto é a decisão **certa** — não peço reversão. Peço que (a) a pendência 2 diga isso com essas palavras, e (b) o planner avalie mover o bloco do keystore para depois do `systemctl start caddy`, para que um problema de secret não derrube o proxy junto.

### R4 — baixo | `docs/sprints/03-folha-pagamento/status/FIX-007-remover-segredos-versionados.md:17-18`

`commits:` lista só `d1c5e08`; falta `49856b5`. Mesmo padrão já corrigido em follow-up no FIX-006 (`dce58fe`).

### R5 — baixo | planner | `docs/sprints/03-folha-pagamento/plans/FIX-007-remover-segredos-versionados.md`

Dois itens para o planner ratificar/corrigir:
1. **Ratificar explicitamente** a reformulação da §Critérios de aceitação e da §Referências feita pelo implementador (Desvio 1). Minha análise em §6: substância certa, critério não afrouxado, mas a alteração é de artefato do planner e precisa de aceite formal.
2. Corrigir a §Referências: `runbooks/ROTEIRO-INTEGRACAO-FRONT-BACK.md:208` **não** era ocorrência do literal (zero matches naquele arquivo em `ad6a60a`). Confirmei independentemente.

### R6 — baixo | operacional

`git rm --cached` significa que qualquer **outro** worktree/clone perde `financas_bot_telegram/http/http-client.env.json` no próximo `pull` depois do merge. Confirmei que o worktree do planner (`C:\Users\satya\src\financas_bot_telegram-planner\financas_bot_telegram\http\http-client.env.json`) ainda tem a cópia rastreada e vai perdê-la. Impacto baixo (a cópia útil vive no worktree do implementador e o valor precisa ser rotacionado de qualquer forma), mas é bom o humano saber antes de estranhar.

### R7 — informativo | pré-existente, não introduzido por este FIX

`bootstrap.sh:137` — `PUBLIC_IP=$(curl -sf …/public-ipv4 || echo "localhost")` degrada **silenciosamente** para `localhost` sob IMDSv2 obrigatório, e esse valor vira o `CN` do certificado (174). Um cert self-signed com `CN=localhost` num host acessado por IP é um problema silencioso para o webhook. Não é regressão deste FIX, mas se a correção de R1 for pela opção 3 (token IMDSv2), esta linha é consertada de graça no mesmo diff.

---

## 8. Débito técnico registrado (para o planner consolidar)

Os 5 itens da §"Pendências técnicas identificadas" do status report são legítimos e eu os confirmo:

1. **`bootstrap.sh` acoplado a prod** — `finbot-prod-secrets` hardcoded (156) enquanto o Terraform parametriza `finbot-${var.env}-secrets` (`security.tf:25`). Confirmado. O plano autorizou o hardcode, então não é desvio; é dívida.
2. **`user_data` sem camada de teste** — confirmado; é a razão de R2 existir. Um smoke script versionado em `infra/provision/` daria cobertura repetível.
3. **Sem gate de secret scanning no CI (`CI-002`)** — causa-raiz confirmada: `docs/sprints/01-mvp/plans/FIX-keystore-password-secret.md` moveu a senha para o Secrets Manager e documentou o valor em claro no mesmo commit.
4. **`infra/prod.tfvars`** — reforço: além do Account ID (`:7`), há `my_ip = <IP residencial>` (`:4`). Num repo público, o IP é dado pessoal, não só metadado de infra. O plano adiou; vale reclassificar.
5. **Porta 8443 aberta no security group** — adiamento deliberado, ok.

**Novo, deste review:** `bash.exe.stackdump` está **rastreado** na raiz do repo (aparece como ` M` no `git status`). É lixo de crash do MSYS versionado. Fora do escopo deste FIX (e não incluído no commit, corretamente), mas alguém deveria removê-lo e ignorá-lo.

---

## 9. Validações bloqueadas

Explícitas, não omitidas:

1. **Existência da chave `keystore_password` em `finbot-prod-secrets`** — sem credencial AWS nesta sessão. É a pendência 2 do implementador e continua aberta. `docs/sprints/01-mvp/status/DEP-07.md:98` afirma que existe, mas é relato de 2026-05.
2. **`HttpTokens` da instância / do AMI em uso** — sem credencial AWS. É o cerne do R1. `aws ec2 describe-instances --query 'Reservations[].Instances[].MetadataOptions'` resolveria em 10 segundos; enquanto não for rodado, tratar R1 como provável, não como hipótese.
3. **`terraform plan`/`validate`** — sem acesso ao backend S3 (`provider.tf:10-14`). Substituí por um simulador local de `templatefile` que checa variáveis desconhecidas e escapes (§3). Cobre a classe de erro que quebraria o `plan`; não cobre erros de provider.
4. **Execução real em AL2023** — o harness roda em MSYS. Diferenças de `openssl`/`jq` entre MSYS e AL2023 não foram avaliadas.

---

## 10. Veredito

### **APROVADO COM RESSALVAS — com 1 correção obrigatória antes do merge (R1).**

**O objetivo central do FIX está cumprido e verificado por mim, não por relato.** Os quatro segredos saíram do estado atual da árvore (grep independente, `exit=1` em todos), o arquivo com a chave de dev foi destrackeado e ignorado sem perder a cópia local do humano, nenhum segredo novo entrou no `.example` nem no status report, a narrativa histórica dos status reports foi preservada, o runbook continua seguível, a verificação IAM está correta e é sustentada por linha de arquivo, o contrato script↔aplicação bate exatamente, o template renderiza válido, e os 422 testes passam na minha execução.

**O que impede o "aprovado" limpo** é um único ponto, e ele não é sobre segredos: `bootstrap.sh:149-153` troca uma senha hardcoded por uma dependência não verificada de IMDSv1 que, se falhar, aborta o provisionamento inteiro — e o cenário em que o código roda (recreate a partir de AMI AL2023) é justamente o que aplica IMDSv2 obrigatório por default. A correção é de uma a três linhas e **elimina** a premissa em vez de exigir que alguém a verifique.

**Não considero motivo para reprovação** nenhum dos outros pontos: o Desvio 1 foi bem julgado e bem declarado, o Desvio 2 é factualmente correto, e as pendências para o humano estão nomeadas.

Ordem sugerida: corrigir R1 → ajustar o texto de R2 e R3 no status report → merge. R4 a R7 podem ir em follow-up.

**Lembrete que o plano pede que seja repetido no merge:** este FIX **não é remediação**. Os valores já commitados num repo público devem ser considerados coletados. A rotação (pendência 1 do status report) é o que resolve, e ela continua pendente do humano.
