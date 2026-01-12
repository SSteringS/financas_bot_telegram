# ♟️ Shogi Board - Finanças Bot Telegram

## 🎯 Backlog (Futuro)
- [ ] **Discovery:** Analisar integração direta com API do WhatsApp (Morpheus).
- [ ] **Feature:** Implementar OCR para extração automática de valores de boletos/prints.
- [ ] **UX:** Criar comandos de resumo de gastos mensais.

## 🚀 To Do (Próximos Ataques)
- [ ] **[Backend] Adapter Serverless:** Adicionar `aws-serverless-java-container` e criar `LambdaHandler`.
- [ ] **[Backend] Integração S3:** Finalizar persistência de imagens no Bucket em vez de usar `file_id` do Telegram.
- [ ] **[Backend] Segurança:** Migrar chaves (Bot/DB) do `application.properties` para AWS Secrets Manager.
- [ ] **[Infra] Ajustes de Prod:** Configurar SnapStart na Lambda e desligamento agendado do RDS.

## 🛠️ In Progress (No Campo)
- [ ] **[Infra] Terraform Base:** Finalizar provisionamento de VPC e RDS MySQL (Tempo Estimado: 2h).

## ✅ Done (Vitória)
- [x] **[Backend] Tratamento de Erros:** GlobalExceptionHandler implementado com mensagens amigáveis.
- [x] **[Backend] Orquestração:** UpdateOrchestrator e Strategies implementadas (Pedido vs Comprovante).
- [x] **[Backend] Persistência:** Entidades JPA e Repositórios alinhados à nova especificação.
- [x] **[Infra] Setup Inicial:** Terraform configurado (Tempo Real: 2.5h).