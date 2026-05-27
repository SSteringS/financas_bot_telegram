# ADR 0009 — Provisionamento da EC2 codificado (fim do snowflake server)

**Data:** 2026-05-26
**Status:** Accepted
**Decisores:** humano (com ajuda do Claude planejador)
**Relacionado:** complementa ADR 0008 (Terraform módulo único) — define como a *configuração da instância* (não só os recursos AWS) fica reprodutível.

---

## Contexto

A infra de recursos AWS está em Terraform (ADR 0008), mas a **configuração de dentro da EC2** cresceu via SSH manual e **não está em lugar nenhum do código**:

- O `user_data` atual só instala Java, cria o usuário `finbot` e a pasta `/opt/finbot`.
- Foram feitos manualmente: a unit `finbot.service` (o `user_data` não a cria; o `deploy.yml` só dá `restart`), o **Caddy** + `Caddyfile` + unit (proxy reverso do DEP-03), o cap do **journald** (FIX do disco cheio), e o **keystore.p12** self-signed do webhook em `/opt/finbot/`.

Resultado: a instância é um **snowflake server** — se morrer (falha da AWS, recriação por engano, ou um `terraform apply` que venha como replace), toda essa config se perde e teria que ser refeita de memória. O humano levantou exatamente esse risco.

Contexto do projeto: solo, instância única, filosofia de simplicidade (ADR 0008).

---

## Decisão

**O provisionamento da EC2 é codificado num script de bootstrap versionado no repo, ligado ao `user_data` do Terraform. SSH manual fica reservado a operações pontuais (debug, ação única) — nunca pra configuração durável.**

Concretamente:

1. **Script versionado** (ex.: `infra/provision/bootstrap.sh` ou cloud-init) referenciado no `user_data` via `templatefile()`/`file()` — legível, versionado, separado do `.tf`.
2. **Idempotente** — escrito pra ser seguro rodar mais de uma vez (check-before-install), de modo que reproduza limpo num recreate e possa, no futuro, reconciliar uma instância viva.
3. **Captura tudo o que hoje é manual:** `finbot.service`, Caddy + `Caddyfile` + unit, config do journald, e o tratamento do keystore do webhook.
4. **Sem segredos no `user_data`** — ele é visível nos metadados da instância. Segredos seguem no Secrets Manager; o Let's Encrypt do Caddy é automático (sem segredo).
5. **Escopo desta decisão: garantir o recreate futuro.** A instância **atual não é reconciliada agora** (decisão consciente do humano) — ela segue como está; o script existe pra que uma recriação (planejada ou forçada) nasça configurada.

Implementação: plano `DEP-07`.

---

## Razões

- **Mata o snowflake:** a config da instância passa a viver no código, igual aos recursos AWS. Reprodutível e auditável.
- **`user_data` é o caminho Terraform-nativo** — sem tooling novo. Packer e Ansible resolveriam também, mas adicionam build de AMI / runtime de config management que um projeto solo de uma instância não paga agora (mesma lógica de YAGNI do ADR 0008).
- **Script separado > inline:** um `user_data` inline gigante no `.tf` fica ilegível; um script versionado é legível, testável (shellcheck) e diffável.
- **Idempotência** dá a opção futura de reconciliar a instância viva sem reescrever nada.

---

## Consequências

**Positivas:**
- Recriar a EC2 (por escolha ou por força maior) reproduz o servidor inteiro a partir do código.
- A config deixa de morar só no SSH/na memória.
- Caminho preparado pra, se um dia crescer, evoluir pra Packer/Ansible.

**Negativas / limites aceitos:**
- **A instância atual continua um floco até o próximo recreate** (escopo escolhido). Se ela morrer antes de qualquer recriação, ainda assim o script encurta o rebuild — mas não é "zero toque" hoje.
- `user_data` **só roda na criação** — mudar o script não re-aplica na instância viva. Manter o script em sincronia com a realidade é disciplina (todo novo passo manual deve ir pro script).
- O **keystore self-signed do webhook** é o ponto chato de reproduzir (cert muda → re-registrar no Telegram). O caminho limpo é migrar o webhook pra Caddy + Let's Encrypt (débito em `PENDENCIAS-TECNICAS.md`), aposentando o keystore — tratado fora desta decisão.

---

## Alternativas consideradas

- **`user_data` inline no `ec2.tf`:** descartado — fica ilegível conforme cresce; script versionado é mais limpo.
- **Packer (AMI pré-assada):** adiado — reprodutível e boot rápido, mas adiciona pipeline de build de imagem. Revisitar se o nº de instâncias/complexidade crescer.
- **Ansible (config management):** adiado — idempotente e roda na instância viva, mas é mais uma ferramenta pra um caso de uma instância. Gatilho: frota de instâncias ou config muito mais rica.
- **Status quo (config manual):** descartado — é o problema que motivou a decisão.

---

## Referências

- ADR 0008 (Terraform módulo único — a parte de recursos AWS; este ADR cobre a config da instância)
- `docs/plans/DEP-07-codificar-provisionamento-ec2.md` (execução)
- `docs/plans/DEP-03-api-subdominio-proxy.md` (Caddy, a ser capturado) e `docs/plans/FIX-crescer-volume-ec2.md` (journald, a ser capturado)
- `docs/PENDENCIAS-TECNICAS.md` (migrar webhook pra Let's Encrypt, que aposenta o keystore)
</content>
