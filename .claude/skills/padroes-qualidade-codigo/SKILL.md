---
name: padroes-qualidade-codigo
description: Princípios SOLID, design patterns e boas práticas de código. Carregar quando a task envolve implementação de lógica de domínio, revisão de qualidade de código, ou decisão de design (escolha de pattern, estrutura de classe). Gatilhos: criação de serviços/domínio, detecção de violações em review, proposta técnica pelo arquiteto.
allowed-tools: Read, Grep, Glob, Bash
load_pattern: shared
used_by: [backend, architect, reviewer]
created: 2026-05-30
status: ativa
---

# Skill — Padrões de Qualidade de Código

## Quando carregar

- **Backend:** antes de implementar qualquer classe de domínio, serviço ou adapter.
- **Architect:** ao propor solução técnica ou avaliar opções de design.
- **Reviewer:** ao avaliar qualidade do código além da conformidade de processo.

## Resumo

SOLID + design patterns são ferramentas para resolver problemas de design — não decoração. Cada princípio tem um "sintoma" que indica quando está sendo violado. Cada pattern tem um "problema que resolve" — sem o problema, o pattern é overhead.

## SOLID — regra + sintoma de violação

| Princípio | Regra prática | Sintoma de violação |
|---|---|---|
| **S** Single Responsibility | Uma classe tem um motivo pra mudar | `PedidoService` que salva, envia e-mail E gera PDF |
| **O** Open/Closed | Estender sem modificar | `if/else` crescendo a cada novo tipo ou canal |
| **L** Liskov | Subtipo honra contrato do supertipo | Override lança exceção que o pai não lança |
| **I** Interface Segregation | Interface pequena por cliente | Interface com 8 métodos onde cada impl usa 2 |
| **D** Dependency Inversion | Depender de abstrações, não de implementações | `JdbcTemplate` em `application/`; `new ConcreteService()` inline |

## Design patterns — problema → pattern (e quando não usar)

| Problema | Pattern | Quando NÃO usar |
|---|---|---|
| Múltiplos algoritmos intercambiáveis (ex: canais de envio) | **Strategy** | Se só existe 1 implementação agora e nenhuma prevista |
| Acesso a dados com interface uniforme | **Repository** | Spring Data já fornece — não reimplementar |
| Construção de objeto com muitos campos opcionais | **Builder** | Objeto simples com ≤3 campos |
| Criação sem expor lógica de instanciação | **Factory** | Quando `new` direto é suficiente |
| Encapsular operação como objeto (fila, undo) | **Command** | Operações simples sem histórico |

## Boas práticas

- **Nomes expressivos:** `processarMensagemWhatsapp()` > `process()`. Classe = substantivo. Método = verbo.
- **Métodos curtos:** ~20 linhas. Se não cabe, extrair método privado com nome descritivo.
- **Sem comentários que explicam o "o quê"** — o código já diz. Comentar só o "por quê" quando não óbvio.
- **Sem magic numbers:** `MAX_TENTATIVAS = 3` > `if (tentativas > 3)`.
- **Tratamento de erro na borda:** adapter/controller captura e traduz; domínio lança exceção própria.

## Exemplos concretos do repo

- **Strategy:** canais de envio — `TelegramStrategy`, `WhatsAppStrategy` implementam `MensagemSenderPort`.
- **DIP violado (smell histórico):** `MensagemProcessadaService` dependia de `JdbcTemplate` — detectado na BE-19a.
- **Repository correto:** `PedidoRepository` como interface em `domain/`; implementação JPA em `adapters/out/persistence/`.

## Ler junto

- Skill `arquitetura-hexagonal` — onde cada pattern mora na estrutura de camadas
