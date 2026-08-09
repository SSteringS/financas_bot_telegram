# API de Gerenciamento de Tarefas com IA

## Visão Geral
- **Nome:** TaskAI API
- **Status:** em andamento
- **Tipo:** projeto pessoal
- **Problema que resolve:** API REST que usa IA para categorizar e priorizar tarefas automaticamente com base no contexto

## Stack Tecnica
- **Linguagem:** Java 21
- **Framework:** Spring Boot 3.3
- **Banco de dados:** PostgreSQL + PgVector (para embeddings)
- **Mensageria:** Kafka (para processar categorizações em background)
- **Cloud/Deploy:** AWS ECS (planejado) / Docker Compose local
- **Frontend (se houver):** nenhum por enquanto
- **IA integrada:** Spring AI com OpenAI GPT-4o
- **Outros:** Docker, Flyway, Spring Security 6 com JWT

## Decisões Técnicas Relevantes
- Escolhi processar as categorizações via Kafka em vez de de forma síncrona, porque chamadas ao LLM têm latência de 3-8 segundos — não posso bloquear o request do usuário
- Usei PgVector como vector store porque já estava usando PostgreSQL — evitei adicionar outro serviço na infraestrutura
- Structured Output do Spring AI para mapear a resposta do LLM diretamente em record Java, eliminando parsing manual

## O Que Aprendi Construindo
- LLMs são lentos para uso síncrono em APIs — design assíncrono é obrigatório
- Custo por token escala rapidamente — implementei cache Redis para respostas repetidas (reduziu custo em ~40%)
- Testes com LLM precisam de mock do ChatClient — senão os testes são lentos e custosos

## O Que Faria Diferente
- Teria começado com RAG desde o início em vez de mandar todo o contexto no prompt

## Marco Atual / Próximo Passo
- **Concluído:** CRUD de tarefas, autenticação JWT, integração básica com Spring AI, Kafka para processamento assíncrono
- **Próximo passo:** Implementar RAG com histórico de tarefas para melhorar a categorização

## Link do GitHub
ainda não publicado

## Potencial para Post
- [x] Portfólio (quando concluir o RAG)
- [x] Decisão técnica (por que Kafka para chamadas ao LLM)
- [x] Erro que aprendi (custo de tokens em produção)
- [x] Tutorial (Spring AI do zero com Java 21)
- [x] Artigo no Medium (arquitetura de IA assíncrona em Java)
