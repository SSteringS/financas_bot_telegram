---
task: BE-024
titulo: "Entidades JPA + repositórios — Funcionario e Adiantamento"
data: 2026-06-04
branch: feature/be-024-entidades-jpa-repositorios-folha
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 300
  testes_novos: 12
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - placeholder
pr: null
desvios: 0
pendencias_humano: 0
---

# BE-024 — Entidades JPA + repositórios — Funcionario e Adiantamento

## O que foi feito

- Criados enums de domínio `FormaPagamento` (PIX/TED) e `CategoriaPedido` (VALE/FOLHA) em `domain/vo/` com Javadoc explicitando ortogonalidade entre si e com `TipoPagamento`.
- Criados POJOs de domínio `Funcionario` e `Adiantamento` em `domain/entity/` — sem anotações JPA.
- Criados ports de saída `FuncionarioRepositoryPortOut` e `AdiantamentoRepositoryPortOut` em `application/port/out/` — interfaces puras.
- Estendido `PedidoPagamentoRepositoryPort` com 4 novos métodos: `findValesAbertos`, `existsFolha`, `markAllClosed`, `findFolhasByFuncionario`.
- Criadas entidades JPA `FuncionarioEntity` e `AdiantamentoEntity` mapeando todas as colunas da V6.
- Criados `FuncionarioJpaRepository` e `AdiantamentoJpaRepository` (Spring Data JPA).
- Criados adapters `FuncionarioRepositoryAdapter` e `AdiantamentoRepositoryAdapter` implementando os ports.
- Criados mappers `FuncionarioMapper` e `AdiantamentoMapper` com round-trip entity↔domain.
- Adicionados 5 campos V6 em `PedidoPagamentoEntity` (categoria, funcionario_id, fechado, observacao, mes_referencia).
- Adicionados campos equivalentes em `PedidoPagamento` (domínio) e atualizados mapper e JPA repository para suportá-los.
- `mvn test` verde: 300 testes, 0 falhas, 12 novos.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

**`PedidoPagamentoRepositoryPort` (não `PedidoRepositoryPortOut`):** O plano referencia "PedidoRepositoryPortOut" mas o arquivo existente é `PedidoPagamentoRepositoryPort`. Mantido o nome existente — renomear quebraria referências e não agrega valor.

**`requisitante_id` NOT NULL:** Verificado: `PedidoPagamentoEntity.requisitanteId` tem `nullable = false`. Para pedidos FOLHA (categoria=FOLHA), o `requisitante_id` precisará ser populado com um valor sentinel ou a coluna tornada nullable. **Decisão adiada para BE-028**, onde o FecharMesUseCase cria o Pedido FOLHA — o status report de BE-028 deve registrar a solução escolhida.

**`tipoConta` como String na entity JPA:** A tabela tem `ENUM('CORRENTE','POUPANCA')` mas mapeamos como `String` na entity para evitar criar um enum JPA separado (o domínio usa String também). O banco aplica a restrição de enum; o código Java valida no use case de cadastro de funcionário (BE-025).

**Soft delete na `deleteById`:** O adapter implementa `ativo=false` em vez de DELETE físico, alinhado com o padrão do projeto.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **BE-025, BE-026, BE-027 podem rodar em paralelo** agora que as entidades e ports existem.
- **`requisitante_id` NOT NULL** é um ponto de atenção para BE-028: ao criar Pedido FOLHA, o use case precisará resolver o valor de `requisitante_id`. Opções: (a) nullable via ALTER TABLE V6b; (b) valor sentinel (ex: `0L`). Ver plano BE-028 §"Verificar (pré-implementação)".
- A tabela `adiantamento` tem CHECK constraint `chk_adiant_consistencia` — o BE-027 deve validar `|valorTotal - valorParcela * numParcelas| <= 0.01` **antes** de chegar no banco, conforme plano.

---

## Padrões técnicos

**SRP (Single Responsibility Principle):**
- `FuncionarioMapper` (mapper/) — única responsabilidade: traduzir entity↔domain. Não conhece ports nem use cases.
- `FuncionarioRepositoryAdapter` (adapters/out/) — única responsabilidade: implementar o port de saída. Não conhece nenhum use case.

**DIP (Dependency Inversion Principle):**
- `FuncionarioRepositoryPortOut` e `AdiantamentoRepositoryPortOut` são interfaces puras (sem anotações Spring) em `application/port/out/`. Use cases futuros (BE-025..028) dependerão dessas interfaces, não das implementações JPA.
- O domínio (`domain/entity/Funcionario.java`, `domain/entity/Adiantamento.java`) não importa nenhuma classe de `adapters/` — isolamento total.

**OCP (Open/Closed Principle):**
- `PedidoPagamentoRepositoryPort` foi estendido com 4 novos métodos sem quebrar os existentes (`save`, `findById`). A implementação `PedidoPagamentoRepositoryAdapter` foi estendida de forma aditiva.

**Arquitetura hexagonal:**
- Domínio (`domain/entity/`, `domain/vo/`): POJOs puros — zero dependências de frameworks.
- Application layer (`application/port/out/`): interfaces puras — definem o contrato sem vincular à implementação.
- Adapters out (`adapters/out/persistence/`): única camada que conhece JPA, Spring Data e Hibernate.
- O fluxo de dependência é sempre de fora para dentro: `AdiantamentoRepositoryAdapter` → `AdiantamentoRepositoryPortOut` ← use cases futuros. O adapter implementa o port; o use case depende do port — nunca do adapter diretamente.

**Javadoc ortogonalidade `CategoriaPedido` vs `TipoPagamento`:**
- Comentário explícito em `CategoriaPedido.java` e `FormaPagamento.java` documentando que são dimensões independentes — endereça o risco identificado no plano.

---

## Arquivos criados/modificados

- `domain/vo/FormaPagamento.java` (novo)
- `domain/vo/CategoriaPedido.java` (novo)
- `domain/entity/Funcionario.java` (novo)
- `domain/entity/Adiantamento.java` (novo)
- `application/port/out/FuncionarioRepositoryPortOut.java` (novo)
- `application/port/out/AdiantamentoRepositoryPortOut.java` (novo)
- `application/port/out/PedidoPagamentoRepositoryPort.java` (modificado: +4 métodos V6)
- `adapters/out/persistence/entity/FuncionarioEntity.java` (novo)
- `adapters/out/persistence/entity/AdiantamentoEntity.java` (novo)
- `adapters/out/persistence/entity/PedidoPagamentoEntity.java` (modificado: +5 campos V6)
- `adapters/out/persistence/FuncionarioJpaRepository.java` (novo)
- `adapters/out/persistence/AdiantamentoJpaRepository.java` (novo)
- `adapters/out/persistence/FuncionarioRepositoryAdapter.java` (novo)
- `adapters/out/persistence/AdiantamentoRepositoryAdapter.java` (novo)
- `adapters/out/persistence/PedidoPagamentoJpaRepository.java` (modificado: +4 queries V6)
- `adapters/out/persistence/PedidoPagamentoRepositoryAdapter.java` (modificado: +4 métodos V6)
- `adapters/out/persistence/mapper/FuncionarioMapper.java` (novo)
- `adapters/out/persistence/mapper/AdiantamentoMapper.java` (novo)
- `adapters/out/persistence/mapper/PedidoPagamentoMapper.java` (modificado: V6 fields)
- `domain/model/PedidoPagamento.java` (modificado: +5 campos V6)
- `test/.../mapper/FuncionarioMapperTest.java` (novo — 6 testes)
- `test/.../mapper/AdiantamentoMapperTest.java` (novo — 6 testes)
