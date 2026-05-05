# ♟️ Shogi Board - Finanças Bot Telegram

## 🎯 MVP para Produção — 2 usuários

**Arquitetura escolhida:** EC2 t4g.micro + RDS MySQL (private subnet) + S3
**Custo estimado:** ~$8–15/mês
**Decisão:** Lambda removido em favor de EC2 — elimina NAT Gateway ($32/mês), cold starts e complexidade de deploy.

---

## ✅ Fase 0 — Segurança Imediata (CONCLUÍDA)

- [x] **[Segurança] Rotacionar token do bot** via @BotFather — token antigo revogado
- [x] **[Segurança] Limpar credenciais do git** — `application-dev.properties` desrastreado; `application.properties` sanitizado com placeholders; `application-dev.properties.example` criado como template
- [x] **[Segurança] Atualizar novo tblz, agoeraoken** no Secrets Manager — `finbot-prod-secrets` atualizado com token, db_host, db_username e db_password

---

## 🛠️ Fase 1 — Completar o Código

- [x] **[Backend] Integração Secrets Manager** — `application-prod.properties` criado com `spring.config.import=aws-secretsmanager:/finbot-prod-secrets`; credenciais injetadas via `${db_host}`, `${db_username}`, `${db_password}`, `${telegram_token}`
- [ ] **[Backend] Teste fluxo completo** — testar ngrok + webhook + S3 upload em dev
- [ ] **[Backend] Executar migrations SQL** — rodar `migration_s3_integration.sql` no RDS

---

## 🏗️ Fase 2 — Infraestrutura (Terraform)

- [ ] **[Infra] Adicionar EC2 t4g.micro** — subnet pública, Internet Gateway, Elastic IP
- [ ] **[Infra] Security Group EC2** — porta 8443 inbound (Telegram IPs) + SSH restrito ao IP pessoal
- [ ] **[Infra] Remover Lambda** — remover recursos Lambda, alias, Function URL e NAT (não existe, mas confirmar limpeza)
- [ ] **[Infra] Backend remoto Terraform** — mover `terraform.tfstate` para bucket S3 (evita conflito entre 2 pessoas)
- [ ] **[Infra] Permissão S3 de upload** — adicionar policy `s3:PutObject` na role EC2 (atualmente só tem `s3:GetObject` para o artefato)

---

## 🚀 Fase 3 — Deploy e Webhook

- [ ] **[Deploy] Build do JAR** — `mvn package -DskipTests`
- [ ] **[Deploy] Configurar systemd** — service file para Spring Boot rodar como daemon na EC2
- [ ] **[Deploy] Script de deploy** — `scp` do JAR + restart do serviço
- [ ] **[Deploy] Configurar webhook Telegram** — apontar para `https://<EC2-IP>:8443/webhook`

---

## 📡 Fase 4 — Operação Básica

- [ ] **[Ops] CloudWatch alarm** — alarme para erros no log do Spring Boot
- [ ] **[Ops] Ajustar horário RDS** — confirmar que auto start/stop do EventBridge cobre o horário de uso real (atualmente: liga 11h UTC, desliga 1h UTC)

---

## ✅ Done (Vitória)

- [x] **[Backend] Tratamento de Erros** — GlobalExceptionHandler com mensagens amigáveis
- [x] **[Backend] Orquestração** — UpdateOrchestrator e Strategies (Pedido vs Comprovante)
- [x] **[Backend] Persistência** — Entidades JPA e Repositórios
- [x] **[Backend] Integração S3** — S3ImageUploadService, TelegramFileDownloaderService, refatoração dos Strategies
- [x] **[Infra] VPC + RDS** — Terraform provisionado (VPC, subnets privadas, RDS MySQL, Secrets Manager, EventBridge)

---

## 🔭 Backlog (Futuro — pós MVP)

- [ ] **Discovery:** Analisar integração direta com API do WhatsApp (Morpheus)
- [ ] **Feature:** OCR para extração automática de valores de boletos/prints
- [ ] **UX:** Comandos de resumo de gastos mensais
- [ ] **Infra:** Ajustes finos de produção (alertas avançados, backup S3 de imagens)
