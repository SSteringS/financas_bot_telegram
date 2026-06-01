# DISPATCH — BE-024-entidades-jpa-repositorios-folha (single-task)

> **Quando usar:** após o PR da BE-023 ser mergeado em `develop`. BE-024 precisa do V6 SQL no classpath pra que os testes de integração com Flyway funcionem.

---

## Pré-condições (git)

- **`feature/be-023-migracao-v6-folha-pagamento` mergeada em `develop`** — V6__folha_pagamento.sql deve estar em `develop` antes de criar esta branch. Confirmar com `git log origin/develop --oneline | grep be-023`.
- Nenhuma outra task de BE em voo que mexa em `PedidoPagamentoEntity` ou `PedidoRepositoryPortOut`.

---

## O prompt (cole tudo numa sessão `--agent backend`)

```
Task: BE-024 — Entidades JPA + repositórios — Funcionario e Adiantamento.

Localize e leia o plano completo (initialPrompt orienta o Glob).
Leia também:
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §3 (estrutura hexagonal)
- docs/decisions/0016-evo09-folha-pagamento.md
- Arquivos existentes para seguir o padrão:
  adapters/out/persistence/entity/PedidoPagamentoEntity.java
  application/port/out/PedidoRepositoryPortOut.java (ou equivalente)
  domain/entity/PedidoPagamento.java

## A TASK

Entregar as camadas de domínio e persistência para as entidades novas (Funcionario, Adiantamento)
e estender as existentes (PedidoPagamento) com os campos da V6.

Camadas a criar:
1. Domain POJOs: Funcionario.java, Adiantamento.java, enums FormaPagamento e CategoriaPedido.
2. Ports (out): FuncionarioRepositoryPortOut, AdiantamentoRepositoryPortOut,
   + 4 métodos novos em PedidoRepositoryPortOut (findValesAbertos, existsFolha, markAllClosed, findFolhasByFuncionario).
3. JPA Entities: FuncionarioEntity, AdiantamentoEntity (mapeadas pras tabelas V6).
4. JPA Repositories (Spring Data): FuncionarioJpaRepository, AdiantamentoJpaRepository.
5. Adapters: FuncionarioRepositoryAdapter, AdiantamentoRepositoryAdapter, + implementar os 4 métodos novos no PedidoRepositoryAdapter existente.
6. Mappers: FuncionarioMapper, AdiantamentoMapper.
7. Modificar: PedidoPagamentoEntity (5 campos novos da V6), PedidoPagamento domain (campos equivalentes).

## REGRAS DURAS

1. Branch: `feature/be-024-entidades-jpa-repositorios-folha` saindo de `origin/develop`
   (develop já tem V6 SQL mergeado de BE-023).
2. Território: SÓ `financas_bot_telegram/`. Zero `frontend/`.
3. Nenhuma anotação JPA no domain (Funcionario.java, Adiantamento.java são POJOs puros).
4. Ports são interfaces puras: zero @Autowired, zero @Repository, zero Spring no pacote application/port/.
5. 1 commit: `feat(BE-024): entidades JPA + repositorios + ports folha de pagamento`.
6. NÃO mergeie. PR pra develop após status report + Reviewer.

## VERIFICAÇÃO ANTES DE IMPLEMENTAR

Verificar se `CategoriaPedido` já existe no projeto (pode ter sido criado antes):
  grep -r "CategoriaPedido" financas_bot_telegram/src/main/java/

Se existir: reusar e não duplicar. Se não existir: criar em domain/vo/.

Verificar se `TipoPagamento` (enum existente) tem valores que colidem semanticamente com VALE/FOLHA:
  grep -r "TipoPagamento" financas_bot_telegram/src/main/java/ | head -20
São dimensões ortogonais (categoria=propósito; tipo=forma de pagamento) — documentar com Javadoc.

Verificar nullability de `requisitante_id` em PedidoPagamentoEntity:
  grep -A5 "requisitanteId\|requisitante_id" financas_bot_telegram/src/main/java/.../PedidoPagamentoEntity.java
Se NOT NULL: registrar no status report como bloqueio potencial para BE-028.

## TESTES

- Unitário: FuncionarioMapper round-trip (entity→domain→entity), idem AdiantamentoMapper.
- Integração (recomendado): FuncionarioRepositoryAdapter salva e busca funcionário via banco de teste.

`testes_novos` ≥ 6.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/BE-024-entidades-jpa-repositorios-folha.md`

Incluir na seção `## Padrões e decisões técnicas`:
- Como a separação entity/domain/port respeita hexagonal (onde ficou a fronteira).
- Decisão sobre CategoriaPedido (criado novo ou reutilizado existente) e ortogonalidade com TipoPagamento.
- Resultado da verificação de `requisitante_id` (nullable ou NOT NULL) — impacto em BE-028.

## SE QUEBRAR

Cenário 1 — `mvn test` falha por Flyway não encontrar V6 SQL:
  Confirmar que `git log origin/develop | grep be-023` mostra o merge. Se não, BE-023 não foi mergeada ainda — PARE e aguarde.

Cenário 2 — `PedidoPagamentoEntity` tem `requisitante_id` NOT NULL:
  Registrar no status report: "BE-028 vai precisar de migration V6b ou workaround antes de criar Pedido FOLHA."
  NÃO resolver agora — escopo desta task é só mapear a situação.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano

- **Pré-condição crítica:** BE-023 mergeada antes de despachar esta. O agente vai criar a branch de `origin/develop` — se V6 não estiver lá, os testes de integração vão falhar misteriosamente.
- **Estimativa:** 1-2h. Volume de classes é grande, mas nenhuma lógica complexa.
- **Após merge:** despachar BE-025 E BE-026 em paralelo (ambas dependem só desta).
  ⚠️ BE-026 e BE-027 NÃO devem rodar em paralelo (conflito em FolhaController.java) — ver DISPATCH-BE-026 e DISPATCH-BE-027.

## Referências

- `docs/sprints/03-folha-pagamento/plans/BE-024-entidades-jpa-repositorios-folha.md`
- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §3
- `docs/decisions/0016-evo09-folha-pagamento.md`
