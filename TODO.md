# Plano de Ação - MVP Backend (Kanban)

Este arquivo organiza as tarefas para o desenvolvimento do backend, alinhado com a `ESPECIFICACAO_TECNICA.md`.

## Backlog

### Fase 2: Implementação de Novas Features

### Fase 3: Infraestrutura e Deploy (Tarefas do Kanban)
- [ ] **MVP - Infraestrutura (Geral):**
  - [ ] Provisionar infraestrutura como código usando Terraform.
  - [ ] Criar banco de dados MySQL (Amazon RDS).
  - [ ] Criar bucket S3 para armazenamento futuro (se necessário).
  - [ ] Configurar repositório de imagens Docker no Amazon ECR.
  - [ ] Configurar ECS Fargate para deploy do backend.
  - [ ] Configurar Pipeline CI/CD (CodePipeline/CodeBuild).

## To Do

_(Nenhuma tarefa aqui ainda)_

## In Progress

- [ ] **Configurar Tratamento de Erros:**
  - [ ] Criar exceções customizadas (ex: `PedidoNaoEncontradoException`, `UsuarioNaoAutorizadoException`).
  - [ ] Implementar um `ControllerAdvice` para tratar exceções e retornar respostas de erro claras para o webhook.
- [ ] **Configurar Logs:**
  - [ ] Adicionar logs nos pontos críticos do fluxo (recebimento, persistência, erros).

- [ ] **Refatoração da Fase 1**

## Done

- [x] **Implementar Fluxo de Registro de Comprovante:**
  - [x] Desenvolver a lógica para identificar se uma mensagem com foto é um comprovante (pela legenda).
  - [x] Implementar a extração do ID do pedido e do tipo de pagamento da legenda usando Regex (Ex: "#45 PIX").
  - [x] Criar o serviço para buscar o `PedidoPagamento` correspondente.
  - [x] Persistir o novo `Comprovante` no banco de dados.
  - [x] Atualizar o status do `PedidoPagamento` para `PAGO`.
  - [x] Implementar a resposta ao usuário confirmando o pagamento.

### Fase 1: Refatoração e Alinhamento com a Especificação
- [x] **Revisar Modelo de Dados (Entidades JPA):**
  - [x] Criar/Ajustar a entidade `PedidoPagamento` para corresponder à tabela `pedidos_pagamento`.
  - [x] Criar a entidade `Comprovante` para corresponder à tabela `comprovantes`.
  - [x] Garantir que os relacionamentos e tipos de dados estejam corretos (ex: ENUM para Status).
- [x] **Refatorar Camada de Persistência (Repositórios):**
  - [x] Atualizar os repositórios JPA para as novas entidades.
  - [x] Remover a lógica antiga de salvamento que está em desacordo com a especificação (ex: salvar URL de S3 diretamente, categorias na criação).
- [x] **Ajustar Lógica de Recebimento de Mensagem (Webhook):**
  - [x] Implementar a validação de `telegram_user_id` (Whitelist).
  - [x] Ajustar o serviço para persistir um `PedidoPagamento` com status `PENDENTE` ao receber uma nova imagem de cobrança.
  - [x] Implementar a resposta ao usuário com o ID do pedido gerado (Ex: "Registrado! Pedido #45").

