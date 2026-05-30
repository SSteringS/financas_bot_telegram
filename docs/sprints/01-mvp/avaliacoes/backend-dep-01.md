# Avaliação — DEP-01 (Hosted Zone Route 53 + Certificado ACM)

**Branch:** `feature/dep-01-route53-zone-acm` (já mergeada em `develop` via PR #56, commit `1d74ef2`)
**Commit de feature:** `ff39bd0` (+ `6fe34a9`, docs de workflow em paralelo)
**Implementador:** Claude do back
**Avaliado por:** Reviewer (sessão independente, ADR 0005) — análise contra o código/diff real
**Data:** 2026-05-26

---

## Veredito: **aprovado com observações**

O código de infra entregue **bate fielmente com o plano** e é idiomático. As observações são de **processo e de precisão documental**, não de defeito de código. Nenhuma observação justifica reverter o merge; todas são corrigíveis em follow-up.

> **Transparência sobre o que NÃO foi reproduzido.** Este sandbox não tem `terraform` instalado (instalação bloqueada pelo proxy — HTTP 403), nem credenciais AWS, nem acesso de rede à AWS. Portanto **não consegui rodar `terraform validate`/`fmt`/`plan`/`apply`** nem confirmar contra a AWS as afirmações do report (cert `ISSUED`, `4 added`, idempotência "No changes"). Essas afirmações **repousam no autorrelato do implementador** e não foram verificadas contra a realidade. A revisão de HCL abaixo é por inspeção manual.

---

## Notas por dimensão

| Dimensão | Peso | Nota | Observação |
|---|---|---|---|
| Fidelidade ao plano | 30% | 10 | Código idêntico ao especificado (dns.tf, acm.tf, outputs.tf, variables.tf, tfvars). |
| Qualidade técnica (HCL) | 25% | 9 | Idiomático e correto por inspeção. `for_each` por `domain_name` é o padrão; `allow_overwrite` cobre a dedupe. |
| Conformidade de processo | 20% | 5 | Status report **sem frontmatter/gates**; revisão acontecendo **pós-merge**; commit de docs de planejamento misturado na branch. |
| Precisão documental | 15% | 4 | Pendência registrada descreve um problema **que não existe** (state nunca foi versionado). |
| Verificabilidade da entrega | 10% | — | Não pontuável neste ambiente (sem terraform/AWS). Claims de apply não reproduzíveis aqui. |

**Nota final: ~7,5/10** — entrega de código sólida, lastreada por gaps de processo e uma imprecisão documental concreta.

---

## Conformidade de processo (sintaxe do workflow)

- **Branch convenção — ok.** `feature/dep-01-route53-zone-acm` bate `^feature/dep-\d+-` e saiu de `develop` (está na história linear).
- **Território — ok (gate passa).** O commit de feature `ff39bd0` tocou só `docs/` + `financas_bot_telegram/infra/` — território de `claude-back`. O gate `territorio` não dispara.
- **Status report sem frontmatter — FALHA de schema.** `docs/sprints/01-mvp/status/DEP-01.md` usa o formato livre antigo (Data/Branch/Executor), **sem** o bloco `gates:` que o `_TEMPLATE.md` e o `PRE-MERGE-CHECKLIST.md` exigem. **Atenuante de justiça:** o schema de frontmatter foi introduzido na *própria branch* (commit `6fe34a9`, que reescreveu `_TEMPLATE.md`); quando o DEP-01.md foi escrito, o template ainda era o antigo. Mas o par dele, `DEP-02.md`, escrito logo depois no mesmo dia, **já segue o schema completo**. Resultado: pelo template canônico hoje em `develop`, o report da DEP-01 não é parseável/agregável e está fora de conformidade. **Deve ser retrofitado** com frontmatter (gates: build/lint/testes = `na`; branch_convencao/territorio = `ok`; desvios: 2; pendencias_humano: 0).
- **Revisão pós-merge, não pré-merge.** A DEP-01 (e DEP-02, e o fix gitattributes) **já estão em `develop`**. O checklist diz "parar pra revisão antes de mergear". O ganho de independência do ADR 0005 fica diluído quando a revisão chega depois do merge — vira retrospectiva, não gate. (Pode ter sido decisão consciente do humano-integrador; registro como sinal de processo.)
- **Commit de escopo misto na branch.** `6fe34a9` ("planejamentos e regras adicionais... feitas em paralelo com DEP-01") trouxe `CLAUDE.md`, `docs/runbooks/PRE-MERGE-CHECKLIST.md`, `docs/templates/_TEMPLATE-status.md` e `docs/aprendizado/` — material **estrutural de planejamento** empacotado numa branch de back, sob mensagem vaga. Não fere o gate `territorio` (docs/raiz são livres), mas mistura responsabilidades e dificulta a leitura do PR. Idealmente docs de planejamento entram pelo fluxo do planejador.

## Qualidade técnica (semântica do código)

Pontos fortes:

- **`dns.tf` referencia a zona via `data source`** em vez de criar `aws_route53_zone` — decisão correta documentada no plano, evita duplicar zona e protege contra `terraform destroy` apagar a zona registrada.
- **Certificado apex + wildcard SAN** (`domain_name = apex`, `subject_alternative_names = ["*.apex"]`) cobre `api.`, `www.` etc. — e a nota do plano sobre o wildcard não cobrir o apex sozinho está correta.
- **`create_before_destroy` no cert** e **`aws_acm_certificate_validation`** encadeando a validação antes de expor o ARN: o output `acm_certificate_arn` aponta para o recurso *validado*, então o DEP-02 só consome um cert pronto. Bom contrato.
- **`allow_overwrite = true` + `ttl = 60`** nos registros de validação: trata a dedupe apex/wildcard (mesmo CNAME) e acelera a primeira validação. Padrão correto.

Observações de qualidade (não-bloqueantes):

- **`for_each` por `dvo.domain_name`** gera duas entradas (apex e `*.apex`) que, para cert wildcard, resolvem ao **mesmo** registro DNS. Funciona graças ao `allow_overwrite`, mas é o conhecido wart de "dois recursos gerenciando o mesmo registro". O report até admite ("Route 53 deduplicou"). Aceitável; se algum dia incomodar, chavear por `resource_record_name`.
- **`lifecycle { ignore_changes = [ami] }` na EC2 (Desvio 1).** Resolve o replace indevido da EC2 de prod por drift de AMi (`most_recent = true`), e foi aprovado pelo humano. **Consequência latente a registrar:** o Terraform agora **nunca** vai atualizar a AMI, nem quando for intencional — uma futura troca de AMI exige remover o `ignore_changes` manualmente. Trade-off razoável para o estágio, mas vale uma linha em PENDÊNCIAS para não virar surpresa.

---

## Imprecisão documental concreta (verificado contra a realidade)

A "Pendência gerada" do report aponta para o item de PENDÊNCIAS-TECNICAS intitulado **"`terraform.tfstate` e `.tfstate.backup` commitados em `financas_bot_telegram/infra/`"**, que afirma que esses arquivos "**estão versionados no repositório**" e sugere como fix adicioná-los ao `.gitignore` e rodar `git rm --cached`.

**Isso é falso, e dá pra verificar:**

- `git ls-files financas_bot_telegram/infra/ | grep tfstate` → **vazio** (nenhum state versionado).
- `git log --all -- '.../terraform.tfstate'` → **vazio** (nunca esteve no histórico).
- `git check-ignore -v` → os states batem nas regras `*.tfstate` / `*.tfstate.*` do `.gitignore` da raiz; `git status --ignored` os marca como `!!` (ignorados).
- Essas regras (`**/.terraform/*`, `*.tfstate`, `*.tfstate.*`) **já existiam no pai do commit DEP-01** (`ff39bd0~1`) — estão lá desde `fce7035`.

Ou seja: o state local existe **no disco** (resíduo de runs locais), mas **já está corretamente ignorado e nunca foi commitado**. A premissa do plano ("há tfstate versionado") era equivocada, e o implementador a promoveu a uma pendência firme em vez de verificar. O fix sugerido é redundante (as regras existem) e o `git rm --cached` não tem o que remover. **Recomendação:** corrigir ou remover esse item de PENDÊNCIAS — descrever, no máximo, "state local presente no disco como resíduo; já gitignorado; opcionalmente limpar a cópia local".

---

## Fora de escopo, mas sinalizo

A working tree de `develop` tem **alterações não-commitadas** em `infra/` (incl. `ec2.tf`, `outputs.tf`, `dev.tfvars`), backend (`PedidoController`, `RestExceptionHandler`, `JwtAuthenticationFilter`) e frontend. A maioria é **churn de fim-de-linha (CRLF→LF)** da normalização do `.gitattributes` recém-mergeada. **Risco a checar:** o diff do `dev.tfvars` aparece **removendo a linha `domain_name = "satyansaita.com"`**, o que quebraria `terraform apply -var-file=dev.tfvars` (variável sem default). Isso é estado solto no `develop`, fora da DEP-01 — mas código não-commitado direto no `develop` foge do fluxo de branches do CLAUDE.md e merece o humano dar um `git status` antes de seguir.

---

## Recomendações de processo

1. **Retrofitar `docs/sprints/01-mvp/status/DEP-01.md` com o frontmatter canônico** (gates + desvios: 2 + pendencias_humano: 0) — para ficar agregável como os demais.
2. **Corrigir o item de PENDÊNCIAS sobre tfstate** — está factualmente errado; o state nunca foi versionado.
3. **Quando possível, rodar a verificação independente ANTES do merge** (ou ao menos antes do PR `develop→main`), para a independência do ADR 0005 funcionar como gate e não como retrospectiva.
4. **Não empacotar docs estruturais de planejamento em branch de implementador** — `6fe34a9` deveria ter ido pelo fluxo do planejador.
5. **Registrar em PENDÊNCIAS o trade-off do `ignore_changes = [ami]`** (Terraform não atualiza mais a AMI sem intervenção manual).
6. **Validação de HCL precisa ser reproduzível por quem revisa.** Como o sandbox não tem terraform, considerar rodar `terraform validate && terraform fmt -check && terraform plan` na máquina do humano e colar a saída no status report, para a revisão não depender só do autorrelato.

## Recomendação específica para esta entrega

**Manter o merge.** O código está correto e fiel ao plano. Abrir um `fix/` pequeno para os itens 1, 2 e 5 acima (todos em `docs/` + `.gitignore`/PENDÊNCIAS, esforço baixo). A imprecisão da pendência é o único item que eu trataria com alguma urgência, porque documentação errada sobre state de infra é exatamente o tipo de coisa que confunde no momento errado.
