# ♟️ Shogi Board - Finanças Bot Telegram

## 🎯 MVP para Produção — 2 usuários

**Arquitetura escolhida:** EC2 t4g.micro + RDS MySQL (default VPC, acesso restrito ao IP da EC2) + S3
**Custo estimado:** ~$8–15/mês
**Decisão:** Lambda removido em favor de EC2 — elimina NAT Gateway ($32/mês), cold starts e complexidade de deploy.

---

## ✅ Fase 0 — Segurança Imediata (CONCLUÍDA)

- [x] **[Segurança] Rotacionar token do bot** via @BotFather — token antigo revogado
- [x] **[Segurança] Limpar credenciais do git** — `application-dev.properties` desrastreado; `application.properties` sanitizado com placeholders; `application-dev.properties.example` criado como template
- [x] **[Segurança] Atualizar novo token** no Secrets Manager — `finbot-prod-secrets` atualizado com token, db_host, db_username e db_password
    
---

## ✅ Fase 1 — Completar o Código (CONCLUÍDA)

- [x] **[Backend] Integração Secrets Manager** — `application-prod.properties` criado; credenciais injetadas via `${db_host}`, `${db_username}`, `${db_password}`, `${telegram_token}`
- [x] **[Backend] Teste fluxo completo local** — fluxo de pedido e comprovante testados com S3 upload em dev
- [x] **[Backend] Migrations SQL** — `migration_s3_integration.sql` executado no RDS; schema consolidado em `schema.sql`
- [x] **[Backend] Ambiente dev separado** — bot DEV criado no @BotFather, bucket S3 `bot-financas-pagamentos-dev` criado, `application-dev.properties` configurado localmente

---

## ✅ Fase 2 — Infraestrutura (Terraform) (CONCLUÍDA)

- [x] **[Infra] Adicionar EC2 t4g.micro** — `ec2.tf` criado: subnet pública, Internet Gateway, Elastic IP (`3.228.138.109`), Amazon Linux 2023 ARM64, Java 21
- [x] **[Infra] Security Group EC2** — porta 8443 inbound aberta ao mundo + SSH restrito ao IP do dev
- [x] **[Infra] Remover Lambda** — `lambda.tf` esvaziado; recursos Lambda removidos
- [x] **[Infra] Backend remoto Terraform** — S3 backend em `provider.tf` (bucket: `finbot-tfstate-satyans`)
- [x] **[Infra] Permissão S3 + Secrets Manager** — IAM role da EC2 com acesso ao bucket de imagens e leitura do secret
- [x] **[Infra] Liberar EC2 no RDS SG** — regra inbound 3306 adicionada no SG do RDS para o Elastic IP da EC2

---

## ✅ Fase 3 — Deploy e Webhook (CONCLUÍDA)

- [x] **[Deploy] Build do JAR** — `mvn package -DskipTests`
- [x] **[Deploy] Configurar systemd** — `finbot.service` criado e habilitado na EC2; app roda como usuário `finbot`
- [x] **[Deploy] SSL auto-assinado** — keystore PKCS12 gerado em `/opt/finbot/keystore.p12`; HTTPS na porta 8443
- [x] **[Deploy] Deploy do JAR** — JAR enviado via SCP para `/opt/finbot/app.jar`; serviço ativo e healthy
- [x] **[Deploy] Configurar webhook Telegram** — apontando para `https://3.228.138.109:8443/webhook` com certificado custom; `has_custom_certificate: true`
- [x] **[Deploy] Teste em produção** — fluxo de pedido e comprovante validados com 2 usuários reais

---

## 📡 Fase 4 — Operação Básica

- [ ] **[Ops] CloudWatch alarm** — alarme para erros no log do Spring Boot
- [ ] **[Ops] Ajustar horário RDS** — confirmar que auto start/stop do EventBridge cobre o horário de uso real

---

## 🔭 Backlog (Futuro — pós MVP)

- [ ] **[CI/CD] Reativar testes na pipeline** — testes estão sendo pulados (`-DskipTests`) por falta de configuração de teste isolada; **obrigatório antes da próxima feature**: criar `application-test.properties` com H2 em memória e reativar `mvn test` no `deploy.yml`
- [ ] **[Infra] Mover RDS para VPC do Terraform** — atualmente no default VPC com acesso público restrito; mover para subnet privada é melhoria de segurança
- [ ] **Discovery:** Analisar integração direta com API do WhatsApp (Morpheus)
- [ ] **Feature:** OCR para extração automática de valores de boletos/prints
- [ ] **UX:** Comandos de resumo de gastos mensais
- [ ] **Infra:** Ajustes finos de produção (alertas avançados, backup S3 de imagens)
- [ ] **Feature: valor real pago vs valor previsto** — boleto entra com valor X (previsto/pedido), mas o pagamento real pode ser X + juros / X − desconto. Hoje só persistimos o `valor` em `pedidos_pagamento` (= previsto); o valor real desembolsado se perde, e a diferença (juros pagos, desconto obtido) fica invisível. **Direção provável** (a discutir): coluna `valor_pago` em `comprovantes` separada do `valor` do pedido; capturar como? (legenda do comprovante? mensagem complementar? OCR?); o que mostrar no front (delta? só o real?). *Identificado no uso real do MVP, 2026-05-27.*
- [ ] **Feature: editar pedido pendente** — pedido cadastrado com valor ou descrição errados não tem como ser corrigido hoje; única "saída" é conviver com o erro ou ter pedido fantasma. **Direção provável** (a discutir): novo tipo de mensagem/comando de edição (ex.: `EDIT #123 valor=200.00` ou `#123 valor 200.00 nova descrição`), implementado como **nova strategy** de mensagem entrante. Encaixa limpinho no dispatcher agnóstico pós-refactor (ADR 0013): uma strategy nova em `application/strategy/` vale pra Telegram, WhatsApp e canais futuros **sem duplicação**. Definir escopo: só pedido em status `PENDENTE`? quais campos editáveis (valor, descrição, tipo)? guardar histórico de edição (auditoria)? *Identificado no uso real do MVP, 2026-05-27.*
