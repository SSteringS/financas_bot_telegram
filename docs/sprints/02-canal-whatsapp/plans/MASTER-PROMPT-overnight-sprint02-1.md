# MASTER PROMPT — Overnight 1 da sprint 02 (DEP-09 + BE-17)

Cole o bloco abaixo numa sessão nova do **Claude do back** (Claude Code no IntelliJ/CLI, ideal com `claude --dangerously-skip-permissions`) antes de dormir. Ele produz **só o código** de duas tasks **independentes** (cada uma na sua branch a partir de `develop`), parando antes de qualquer apply/SSH/merge. Os passos manuais (apply do Terraform, SSH pra instalar o agent, revisão do Reviewer, merge) ficam pra você de manhã — há uma checklist no fim.

> **Por que essas duas:** são as **únicas** da sprint 02 que (a) saem direto de `develop` sem depender de outra task e (b) destravam tudo o resto. **DEP-09** é a rede de segurança da migração WhatsApp; **BE-17** é a fundação que os adapters do WhatsApp vão usar. Tudo o resto (BE-17b, BE-19a, BE-18, BE-19, BE-20, BE-21a/b, BE-22) depende destes — e o nosso `CLAUDE.md` proíbe branch a partir de branch (incidente FE-12), então elas têm que esperar o merge.

> **Por que code-only:** uma sessão desacompanhada não deve rodar `terraform apply`, fazer SSH, mexer no console AWS, nem mergear. Ela escreve o código, valida localmente (`terraform validate`, `mvn test/package`), commita e escreve o status report. O resto é seu + Reviewer.

---

```
Você é o Claude do back deste projeto. Leia, nesta ordem:
- CLAUDE.md (regras globais)
- docs/roles/backend.md (seu papel)
- docs/STATE.md
- docs/sprints/02-canal-whatsapp/README.md (objetivo da sprint)
- docs/architecture/adapter-whatsapp-cloud-api.md (a spec do arquiteto — contexto pro BE-17 e §8.1 pro DEP-09)
- docs/architecture/estado-atual.md §3, §5, §6 (mapa do código atual — essencial pro BE-17)
- docs/sprints/02-canal-whatsapp/plans/DEP-09-observability-infra.md
- docs/sprints/02-canal-whatsapp/plans/BE-17-refactor-porta-agnostica.md
- docs/aprendizado/observability-logs-externalizar.md (contexto pro DEP-09)

## REGRAS DURAS (não violar)
1. CODE-ONLY. NÃO rode `terraform apply`. NÃO faça SSH. NÃO mexa no console/CLI da AWS. NÃO faça `git push` pra `develop` nem merge. NÃO rode testes em produção.
2. Validação permitida é LOCAL: `terraform fmt -check`, `terraform validate`, `mvn test`, `mvn package -DskipTests`.
3. UMA BRANCH POR TASK, criada **a partir de `develop`** — NUNCA a partir de outra feature branch (incidente FE-12). Pra cada task: `git checkout develop && git pull && git checkout -b feature/<id>-<slug>`. UM commit por task no padrão `feat(<ID>): ...`.
4. Ao terminar CADA task: escreva o status report em `docs/sprints/02-canal-whatsapp/status/<ID>.md` com frontmatter válido (modelo em `docs/status/_TEMPLATE.md`), e PARE — não mergeie. Reviewer + apply são humanos.
5. Território: só `financas_bot_telegram/`, `infra/`. NÃO toque em `frontend/`.
6. Se bater numa decisão de produto ou ambiguidade que o plano não cobre, NÃO invente: registra como pendência no status report daquela task (estado: bloqueado) e segue pra próxima.

## ORDEM E ESCOPO

### Task 1 — DEP-09 (independente, faz primeiro porque é menor e dá warm-up)
Branch: `feature/dep-09-observability-infra`
Siga `docs/sprints/02-canal-whatsapp/plans/DEP-09-observability-infra.md`. Resumo:
- Criar `infra/observability.tf`: `aws_cloudwatch_log_group` `/finbot/app` (retenção 30 dias), `aws_cloudwatch_log_metric_filter` que conta "ERROR" → métrica `finbot/app/errors`, 3 `aws_cloudwatch_metric_alarm` (disco/CPU/error_rate) **sem SNS por ora**.
- Criar/ajustar `logback-spring.xml` pra escrever `/var/log/finbot/app.log` em JSON (manter appender de console). Ativar via profile prod.
- Criar `infra/observability/cloudwatch-agent-config.json` (versionado; instalação é manual).
- `terraform fmt -check` + `terraform validate` + `mvn test` + `mvn package -DskipTests` — todos verdes.
- NÃO inclua Micrometer (é BE-22, depois do BE-17).
- Status report em `docs/sprints/02-canal-whatsapp/status/DEP-09.md` — incluir os comandos manuais pra apply/install do agent.

### Task 2 — BE-17 (independente, fundação — a mais densa)
Branch: `feature/be-17-refactor-porta-agnostica` (a partir de `develop`, NÃO da DEP-09).
Siga `docs/sprints/02-canal-whatsapp/plans/BE-17-refactor-porta-agnostica.md`. Resumo:
- Criar: `application/port/in/MensagemEntrantePortIn.java`, `application/services/MensagemEntranteService.java`, `application/strategy/MensagemProcessingStrategy.java`, `adapters/in/telegram/mapper/TelegramMessageMapper.java`. Criar `PaymentMessageDTO` se ainda não existir.
- Mover (com `git mv` pra preservar histórico): strategies de `adapters/in/telegram/strategy/` pra `application/strategy/`, refatoradas pra operar sobre `PaymentMessageDTO`.
- Refatorar `TelegramWebhookController` pra usar mapper + porta. **Manter a rota `POST /webhook` inalterada** (rename é BE-17b, NÃO faz aqui).
- Remover `UpdateOrchestratorService` (substituído por `MensagemEntranteService`).
- Atualizar tests existentes pra novo contrato; criar tests novos pra `MensagemEntranteService` (escolha de strategy) e `TelegramMessageMapper` (foto+caption, document/PDF EVO-07, texto, sem-caption).
- `mvn test` verde (incluindo TODOS os testes existentes do Telegram) + `mvn package -DskipTests` ok.
- NÃO criar nada de WhatsApp; NÃO mudar a rota.
- Status report em `docs/sprints/02-canal-whatsapp/status/BE-17.md` — preencher `testes_total` e `testes_novos`.

## SE ALGUMA COISA QUEBRAR

- DEP-09 falhar no `terraform validate`: registre o erro no status report (estado: bloqueado) e siga pra BE-17. Não force.
- BE-17 quebrar testes existentes do Telegram: PARE essa task, registre o teste que quebrou no status report (estado: parcial), commita o que tem de progresso numa branch separada se útil pro Reviewer entender, e NÃO siga adiante.
- Qualquer ambiguidade no plano vs. estado-atual: registrar pendência no status report e seguir/parar conforme o caso.

## AO FINAL

Escreva `docs/sprints/02-canal-whatsapp/status/_RESUMO-overnight-sprint02-1.md` com:
- Tabela: task | branch | commit | resultado das checagens locais | estado.
- CHECKLIST MANUAL PRA MANHÃ (humano), na ordem:
  1. Revisar BE-17 com calma (alto risco — toca canal Telegram vivo). Reviewer obrigatório antes de qualquer coisa.
  2. DEP-09: revisar diff Terraform; `terraform plan` confirmando só adds (sem replace EC2); `terraform apply`.
  3. DEP-09 manual SSH: instalar CW agent na EC2, copiar `cloudwatch-agent-config.json` pra `/opt/aws/amazon-cloudwatch-agent/etc/`, `systemctl enable --now amazon-cloudwatch-agent`, smoke test (log aparece em `/finbot/app`).
  4. BE-17: após Reviewer aprovar, merge pra develop. Aí destravam BE-17b, BE-19a, BE-18.
- Pendências/decisões que precisam do humano (se houver).

NÃO mergeie nada. Pare aqui.
```

---

## Notas pra você (humano), fora do prompt

- As duas tasks são **independentes** (cada uma sai de `develop`) — não viola a regra de "branch-a-partir-de-branch" do `CLAUDE.md`.
- Tudo que vem depois na sprint 02 (BE-17b, BE-19a, BE-18, BE-19, BE-20, BE-21a/b, BE-22) **espera BE-17 mergeado**. Por isso o BE-17 é prioridade máxima na revisão de manhã.
- **PREP-WA e TPL-WA** seguem fora desta overnight — são manuais seus na Meta, e têm lead time. Quanto antes você abrir, melhor.
- Próxima overnight (depois do merge do BE-17): **BE-19a + BE-18** em paralelo (também independentes entre si quando o BE-17 estiver em `develop`).

## Referências

- Planos: `docs/sprints/02-canal-whatsapp/plans/DEP-09-observability-infra.md`, `docs/sprints/02-canal-whatsapp/plans/BE-17-refactor-porta-agnostica.md`.
- Spec: `docs/architecture/adapter-whatsapp-cloud-api.md`.
- Padrão de overnight anterior: `docs/sprints/01-mvp/plans/MASTER-PROMPT-overnight-deploy.md` (ou onde tiver ficado após o `git mv` do arquivamento).
</content>
