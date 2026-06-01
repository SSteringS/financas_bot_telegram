---
task: BE-024
titulo: "Entidades JPA + repositórios — Funcionario e Adiantamento"
sprint: 03-folha-pagamento
data_planejamento: 2026-05-31
branch_alvo: feature/be-024-entidades-jpa-repositorios-folha
prioridade: alta
esforco: medio
territorio: back
estado: pronto-pra-execucao
depende_de: [BE-023]
bloqueia: [BE-025, BE-026, BE-027, BE-028]
skills_dispatched: [arquitetura-hexagonal, ecossistema-spring]
---

# BE-024 — Entidades JPA + repositórios — Funcionario e Adiantamento

## Intake

- **Origem:** spec EVO-09 §3 (estrutura hexagonal). Habilita todas as tasks de domínio seguintes.
- **Por quê agora:** sem as entidades e ports, nenhuma use case pode ser implementada.
- **Esforço:** médio — várias classes novas mas nenhuma lógica de negócio complexa aqui.
- **Riscos resumidos:** confirmar ortogonalidade entre `CategoriaPedido` e `TipoPagamento` (enums separados, não conflitam). Verificar nullability de `requisitante_id` em `pedidos_pagamento` antes de be-028.

---

## Contexto

Após BE-023, o banco tem `funcionario`, `adiantamento` e as 5 novas colunas em `pedidos_pagamento`.

Padrões existentes no projeto (seguir):
- Entidades JPA em `adapters/out/persistence/entity/` — usar `@Entity`, `@Table`, etc.
- Repositórios JPA em `adapters/out/persistence/` — interfaces `extends JpaRepository`.
- Ports (out) em `application/port/out/` — interfaces puras, sem anotações Spring.
- Adapters (out) em `adapters/out/persistence/` — implementam os ports.
- Domain entities em `domain/entity/` — POJOs, sem anotações JPA.
- Enums de domínio em `domain/vo/`.

---

## Decisão / abordagem

Seguir rigorosamente a estrutura hexagonal da spec §3. Camadas:

**Domain:**
- `Funcionario.java` — POJO com todos os campos da tabela.
- `Adiantamento.java` — POJO com campos do plano de desconto.
- `FormaPagamento.java` — enum: `PIX`, `TED`.
- `CategoriaPedido.java` — enum: `VALE`, `FOLHA`. **Confirmar que não colide com `TipoPagamento` existente** — são dimensões ortogonais (categoria = propósito; tipo = forma de pagamento).

**Application ports (out):**
- `FuncionarioRepositoryPortOut.java` — métodos mínimos para as use cases previstas.
- `AdiantamentoRepositoryPortOut.java` — idem.
- Estender `PedidoRepositoryPortOut.java` (existente) com 4 novos métodos: `findValesAbertos`, `existsFolha`, `markAllClosed`, `findFolhasByFuncionario`.

**Adapters out (JPA):**
- `FuncionarioJpaRepository.java` (interface Spring Data JPA).
- `AdiantamentoJpaRepository.java` (interface Spring Data JPA).
- `FuncionarioRepositoryAdapter.java` (implementa o port, usa o JpaRepository).
- `AdiantamentoRepositoryAdapter.java` (implementa o port, usa o JpaRepository).
- `FuncionarioEntity.java`, `AdiantamentoEntity.java` — entidades JPA mapeadas para as tabelas V6.

**Mappers:** `FuncionarioMapper.java`, `AdiantamentoMapper.java` (entity ↔ domain).

---

## Escopo / arquivos

### Criar
- `domain/entity/Funcionario.java`
- `domain/entity/Adiantamento.java`
- `domain/vo/FormaPagamento.java`
- `domain/vo/CategoriaPedido.java`
- `application/port/out/FuncionarioRepositoryPortOut.java`
- `application/port/out/AdiantamentoRepositoryPortOut.java`
- `adapters/out/persistence/entity/FuncionarioEntity.java`
- `adapters/out/persistence/entity/AdiantamentoEntity.java`
- `adapters/out/persistence/FuncionarioJpaRepository.java`
- `adapters/out/persistence/AdiantamentoJpaRepository.java`
- `adapters/out/persistence/FuncionarioRepositoryAdapter.java`
- `adapters/out/persistence/AdiantamentoRepositoryAdapter.java`
- `adapters/out/persistence/mapper/FuncionarioMapper.java`
- `adapters/out/persistence/mapper/AdiantamentoMapper.java`

### Modificar
- `application/port/out/PedidoRepositoryPortOut.java` — adicionar: `findValesAbertos(Long funcionarioId, LocalDate inicio, LocalDate fim)`, `existsFolha(Long funcionarioId, LocalDate mesReferencia)`, `markAllClosed(List<Long> pedidoIds)`, `findFolhasByFuncionario(Long funcionarioId)`.
- `adapters/out/persistence/PedidoRepositoryAdapter.java` (ou equivalente) — implementar os 4 novos métodos.
- `adapters/out/persistence/entity/PedidoPagamentoEntity.java` — adicionar os 5 novos campos (categoria, funcionario_id, fechado, observacao, mes_referencia).
- `domain/entity/PedidoPagamento.java` — adicionar os campos equivalentes se necessário para FecharMes.

### Não tocar
- Controllers, use cases — tasks seguintes.
- `V6__folha_pagamento.sql` — já criado em BE-023.

---

## Testes

- **Unitários:** mappers `FuncionarioMapper` e `AdiantamentoMapper` — round-trip entity↔domain.
- **Integração (recomendado):** `FuncionarioRepositoryAdapter` + H2/Testcontainers — salvar e buscar um funcionário; confirmar que CHECK constraints do banco são respeitadas.
- **Verificação:** `CategoriaPedido` vs `TipoPagamento` — adicionar comentário Javadoc explicitando ortogonalidade.

`testes_total` esperado: ≥ testes_existentes + 6. `testes_novos` ≥ 6.

---

## Critérios de aceitação

- [ ] `Funcionario` e `Adiantamento` existem como POJOs em `domain/entity/`.
- [ ] `FormaPagamento` e `CategoriaPedido` existem em `domain/vo/`; Javadoc explica que são dimensões ortogonais.
- [ ] `FuncionarioRepositoryPortOut` e `AdiantamentoRepositoryPortOut` existem em `application/port/out/`.
- [ ] `PedidoRepositoryPortOut` tem os 4 novos métodos; adapter implementa todos.
- [ ] `PedidoPagamentoEntity` reflete os 5 novos campos da V6.
- [ ] `FuncionarioEntity` mapeia todas as colunas da tabela `funcionario` (incluindo `atualizado_em`).
- [ ] `AdiantamentoEntity` mapeia todas as colunas de `adiantamento` (incluindo CHECK fields).
- [ ] Adapters registrados como `@Component` / `@Repository` — Spring wiring funciona.
- [ ] `mvn test` verde.
- [ ] Branch: `feature/be-024-entidades-jpa-repositorios-folha`.
- [ ] Território: apenas `financas_bot_telegram/`.
- [ ] Status report com frontmatter válido.

---

## Fora de escopo

- Use cases (CadastrarFuncionario, FecharMes, etc.) — BE-025 em diante.
- Controllers e endpoints — BE-025 em diante.
- Testes de integração completos do FecharMes — BE-029.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| `CategoriaPedido` colide semanticamente com `TipoPagamento` | Baixa | Médio | Javadoc + comentário na spec; são enums separados |
| `requisitante_id` NOT NULL em `PedidoPagamentoEntity` impede FOLHA | Média | Médio | Verificar schema; se NOT NULL, registrar como bloqueio para BE-028 (ALTER ou workaround) |
| Mapper perde campo `atualizado_em` | Baixa | Baixo | Incluir no mapeamento; cobrir no teste de round-trip |

---

## Coordenação

- **Pode rodar em paralelo com:** nada (gate para BE-025, 026, 027).
- **Depende sequencialmente de:** BE-023 (tabelas precisam existir para teste de integração).
- **Bloqueia:** BE-025, BE-026, BE-027, BE-028.
- **Atenção pro Reviewer:** verificar que nenhuma anotação JPA vazou para o domain; confirmar que ports são interfaces puras (sem @Autowired, sem @Repository); confirmar 4 novos métodos em PedidoRepositoryPortOut.
- **Após merge:** despachar BE-025, BE-026, BE-027 em paralelo.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR pra `develop`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §3
- `docs/decisions/0016-evo09-folha-pagamento.md`
- Padrão existente: `adapters/out/persistence/entity/PedidoPagamentoEntity.java`
