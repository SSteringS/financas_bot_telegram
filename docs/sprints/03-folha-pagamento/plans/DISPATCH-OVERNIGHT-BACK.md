# DISPATCH OVERNIGHT — Backend Sprint 03 (BE-024 → BE-029)

> **Sessão autônoma.** Execute as 6 tasks do backend em sequência sem parar para aguardar
> confirmação humana. Após cada task: cria PR, mergeia em `integration/03-folha-pagamento`,
> parte para a próxima. O humano revisa tudo de manhã antes do PR integration→develop.
>
> **Revisor:** gate de revisão independente está DIFERIDO para o humano de manhã.
> O que substitui: `mvn test` + `mvn compile` DEVEM estar verdes antes de mergear cada task.
>
> **Em caso de falha irrecuperável:** parar na task atual, documentar no status report,
> NÃO continuar para a próxima. Deixar o estado claro para o humano encontrar ao acordar.

---

## PROTOCOLO — repetir para cada task

```
1. git fetch origin
2. git checkout -b feature/<id>-<slug> origin/integration/03-folha-pagamento
3. Ler: docs/sprints/03-folha-pagamento/plans/<TASK-ID>-<slug>.md  (plano completo)
4. Implementar conforme o plano
5. mvn compile && mvn test   ← AMBOS devem ser verdes. Se falhar: PARE (ver Regra de Parada)
6. Escrever status report:  docs/sprints/03-folha-pagamento/status/<TASK-ID>-<slug>.md
7. git add <arquivos do território> docs/sprints/03-folha-pagamento/status/<TASK-ID>-*.md
8. git commit -m "<mensagem definida no plano>"
9. gh pr create \
     --base integration/03-folha-pagamento \
     --head feature/<id>-<slug> \
     --title "<TASK-ID>: <título>" \
     --body "Overnight autônomo. Status report em docs/sprints/03-folha-pagamento/status/."
10. gh pr merge --squash --delete-branch
11. git fetch origin
12. git log origin/integration/03-folha-pagamento --oneline | head -5  ← confirmar merge
13. → PRÓXIMA TASK
```

---

## REGRA DE PARADA

Parar imediatamente (sem continuar para a próxima task) se:
- `mvn test` falhar com erro não trivial (não é flakiness ou teste de setup)
- Uma pré-condição não está satisfeita (arquivo/classe que deveria existir não existe no integration)
- Surge decisão arquitetural que o plano não previu e não tem resposta segura
- Um cenário "SE QUEBRAR" do plano não tem resolução clara

→ Escrever o status report com `estado: bloqueado`, descrever o problema com precisão.
→ **NÃO** continuar para a próxima task — deixar o estado claro para o humano.

---

## TASK 1 — BE-024: Entidades JPA + repositórios

**Pré-condição:** BE-023 em develop (V6 SQL) ✅ + ADR 0016 homologada ✅
**Verificar antes de começar:**
```bash
git fetch origin
git log origin/integration/03-folha-pagamento --oneline | grep be-023
```

**Prompt para a implementação** (executar em sessão `--agent backend`):

```
Task: BE-024 — Entidades JPA + repositórios — Funcionario e Adiantamento.

Leia o plano completo:
  docs/sprints/03-folha-pagamento/plans/BE-024-entidades-jpa-repositorios-folha.md
Leia também:
  docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §3
  docs/decisions/0016-evo09-folha-pagamento.md
  adapters/out/persistence/entity/PedidoPagamentoEntity.java  (padrão a seguir)

## A TASK

Entregar as camadas de domínio e persistência para Funcionario e Adiantamento,
estendendo PedidoPagamento com os campos da V6.

Camadas a criar:
1. Domain POJOs: Funcionario.java, Adiantamento.java, enums FormaPagamento e CategoriaPedido.
2. Ports (out): FuncionarioRepositoryPortOut, AdiantamentoRepositoryPortOut,
   + 4 métodos em PedidoRepositoryPortOut (findValesAbertos, existsFolha, markAllClosed, findFolhasByFuncionario).
3. JPA Entities: FuncionarioEntity, AdiantamentoEntity.
4. JPA Repositories (Spring Data): FuncionarioJpaRepository, AdiantamentoJpaRepository.
5. Adapters: FuncionarioRepositoryAdapter, AdiantamentoRepositoryAdapter,
   + implementar os 4 métodos novos no PedidoRepositoryAdapter existente.
6. Mappers: FuncionarioMapper, AdiantamentoMapper.
7. Modificar: PedidoPagamentoEntity (5 campos novos da V6), PedidoPagamento domain (campos equivalentes).

## VERIFICAÇÃO ANTES DE IMPLEMENTAR

Verificar se CategoriaPedido já existe:
  grep -r "CategoriaPedido" financas_bot_telegram/src/main/java/
Se existir: reusar. Se não: criar em domain/vo/.

Verificar nullability de requisitante_id em PedidoPagamentoEntity — registrar no status report.

## REGRAS

1. Branch: feature/be-024-entidades-jpa-repositorios-folha de origin/integration/03-folha-pagamento
2. Território: SÓ financas_bot_telegram/. Zero frontend/.
3. Domain POJOs SEM anotações JPA. Ports são interfaces puras.
4. 1 commit: feat(BE-024): entidades JPA + repositorios + ports folha de pagamento
5. mvn compile && mvn test verdes antes de abrir PR.
6. Status report: docs/sprints/03-folha-pagamento/status/BE-024-entidades-jpa-repositorios-folha.md
   Incluir: decisão sobre CategoriaPedido + resultado de nullability de requisitante_id.
7. testes_novos >= 6
8. Após status report: criar PR para integration/03-folha-pagamento e mergear (sessão overnight autônoma).
```

**Mensagem de commit:** `feat(BE-024): entidades JPA + repositorios + ports folha de pagamento`

---

## TASK 2 — BE-025: CRUD Funcionário

**Pré-condição:** BE-024 mergeada em integration.
**Verificar antes de começar:**
```bash
git fetch origin
git log origin/integration/03-folha-pagamento --oneline | grep be-024
```

**Prompt para a implementação:**

```
Task: BE-025 — CadastrarFuncionario + CRUD /api/funcionarios.

Leia o plano completo:
  docs/sprints/03-folha-pagamento/plans/BE-025-crud-funcionario.md
Leia também:
  docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §3 §5
  docs/decisions/0016-evo09-folha-pagamento.md

## A TASK

Criar:
1. CadastrarFuncionarioPortIn + AtualizarFuncionarioPortIn (em application/port/in/).
2. CadastrarFuncionarioUseCase + AtualizarFuncionarioUseCase (em application/usecase/).
3. FuncionarioController com 4 endpoints:
   - POST   /api/funcionarios
   - GET    /api/funcionarios        (lista apenas ativos)
   - PUT    /api/funcionarios/{id}
   - DELETE /api/funcionarios/{id}   (soft delete: ativo=false)
4. DTOs: FuncionarioRequest.java, FuncionarioResponse.java.

## DECISÃO A TOMAR

Validação condicional PIX/TED: @AssertTrue no DTO ou validator de classe.
Escolher o mais claro para mensagem de erro 400. Documentar no status report.

## REGRAS

1. Branch: feature/be-025-crud-funcionario de origin/integration/03-folha-pagamento
2. Território: SÓ financas_bot_telegram/. Zero frontend/.
3. DELETE é soft (ativo=false). Não remover do banco.
4. 1 commit: feat(BE-025): CadastrarFuncionario + CRUD /api/funcionarios
5. testes_novos >= 8 (4 unitários + 4 integração)
6. Status report: docs/sprints/03-folha-pagamento/status/BE-025-crud-funcionario.md
   Incluir: decisão sobre validação condicional PIX/TED + justificativa.
7. Após status report: criar PR para integration/03-folha-pagamento e mergear.
```

**Mensagem de commit:** `feat(BE-025): CadastrarFuncionario + CRUD /api/funcionarios`

---

## TASK 3 — BE-026: CadastrarVale

**Pré-condição:** BE-024 mergeada em integration.
**Verificar antes de começar:**
```bash
git log origin/integration/03-folha-pagamento --oneline | grep be-024
```

> ⚠️ Pode rodar imediatamente após BE-025 (paralela em sessões diferentes), mas nesta sessão
> serial roda logo após BE-025. BE-027 NÃO deve iniciar antes desta mergear.

**Prompt para a implementação:**

```
Task: BE-026 — CadastrarVale + endpoint de vales.

Leia o plano completo:
  docs/sprints/03-folha-pagamento/plans/BE-026-cadastrar-vale.md
Leia também:
  docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §3 §5
  docs/decisions/0016-evo09-folha-pagamento.md

## A TASK

Vale = Pedido com categoria=VALE, funcionario_id=<id>, fechado=false.
Decisão §7 JÁ RESOLVIDA: vales NÃO aparecem na lista do Pedro. NÃO tocar GET /api/pedidos.

Criar:
1. CadastrarValePortIn (em application/port/in/).
2. CadastrarValeUseCase (em application/usecase/).
3. FolhaController.java — CRIAR este controller (verificar antes que não existe).
   ATENÇÃO: BE-027 vai adicionar endpoints nele depois — criar extensível.
   2 endpoints:
   - POST /api/funcionarios/{id}/vales
   - GET  /api/funcionarios/{id}/vales?mes=YYYY-MM
4. DTOs: ValeRequest.java, ValeResponse.java.

## VERIFICAÇÃO ANTES DE IMPLEMENTAR

Confirmar que FolhaController.java NÃO existe ainda:
  ls financas_bot_telegram/src/main/java/.../adapters/in/web/FolhaController.java
Se existir: PARE e use AskUserQuestion.

## DECISÃO A TOMAR

Status do vale ao criar: PENDENTE (aguarda comprovante) ou PAGO (dinheiro na hora)?
Se não conseguir esclarecer pelo código existente: usar PENDENTE como padrão seguro. Documentar.

## REGRAS

1. Branch: feature/be-026-cadastrar-vale de origin/integration/03-folha-pagamento
2. Território: SÓ financas_bot_telegram/. Zero frontend/.
3. NÃO tocar PedidoController.java nem GET /api/pedidos (Decisão §7 Opção A já resolvida).
4. 1 commit: feat(BE-026): CadastrarVale + endpoints de vales
5. testes_novos >= 5
6. Status report: docs/sprints/03-folha-pagamento/status/BE-026-cadastrar-vale.md
   Incluir: decisão status do vale + confirmação que GET /api/pedidos NÃO foi tocado.
7. Após status report: criar PR para integration/03-folha-pagamento e mergear.
```

**Mensagem de commit:** `feat(BE-026): CadastrarVale + endpoints de vales`

---

## TASK 4 — BE-027: CadastrarAdiantamento

**Pré-condição:** BE-024 E BE-026 mergeadas em integration.
**Verificar antes de começar:**
```bash
git fetch origin
git log origin/integration/03-folha-pagamento --oneline | grep -E "be-024|be-026"
# Ambas devem aparecer. Se be-026 não estiver: AGUARDAR (não iniciar).
```

> ⚠️ Serializada intencionalmente após BE-026 — ambas tocam FolhaController.java.
> BE-026 o CRIA; esta task ADICIONA endpoints nele.

**Prompt para a implementação:**

```
Task: BE-027 — CadastrarAdiantamento + endpoints adiantamentos.

Leia o plano completo:
  docs/sprints/03-folha-pagamento/plans/BE-027-cadastrar-adiantamento.md
Leia também:
  docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §3 §5
  docs/decisions/0016-evo09-folha-pagamento.md §4
  FolhaController.java existente (criado em BE-026 — já em integration)

## A TASK

Adiantamento = plano de desconto parcelado vinculado a funcionário.

Criar:
1. CadastrarAdiantamentoPortIn + CancelarAdiantamentoPortIn (em application/port/in/).
2. CadastrarAdiantamentoUseCase + CancelarAdiantamentoUseCase (em application/usecase/).
3. DTOs: AdiantamentoRequest.java, AdiantamentoResponse.java.

Modificar:
4. FolhaController.java (JÁ EXISTE de BE-026) — ADICIONAR 3 endpoints:
   - POST   /api/funcionarios/{id}/adiantamentos
   - GET    /api/funcionarios/{id}/adiantamentos
   - DELETE /api/adiantamentos/{adiantamentoId}
   NÃO reescrever o controller — apenas adicionar os 3 endpoints.

## VERIFICAÇÃO ANTES DE IMPLEMENTAR

FolhaController.java DEVE existir em integration (BE-026 já mergeada):
  ls financas_bot_telegram/src/main/java/.../adapters/in/web/FolhaController.java
Se não existir: pré-condição não satisfeita → PARAR (ver Regra de Parada).

## REGRAS

1. Branch: feature/be-027-cadastrar-adiantamento de origin/integration/03-folha-pagamento
2. Território: SÓ financas_bot_telegram/. Zero frontend/.
3. Validação matemática NO use case: |valor_total - valor_parcela * num_parcelas| <= 0.01
4. DELETE adiantamento: soft (ativo=false). Se já quitado: 409.
5. 1 commit: feat(BE-027): CadastrarAdiantamento + endpoints adiantamentos
6. testes_novos >= 6
7. Status report: docs/sprints/03-folha-pagamento/status/BE-027-cadastrar-adiantamento.md
   Incluir: decisão sobre ?incluirInativos no GET + justificativa.
8. Após status report: criar PR para integration/03-folha-pagamento e mergear.
```

**Mensagem de commit:** `feat(BE-027): CadastrarAdiantamento + endpoints adiantamentos`

---

## TASK 5 — BE-028: FecharMes

**Pré-condição:** BE-024 + BE-026 + BE-027 mergeadas em integration.
**Verificar antes de começar:**
```bash
git fetch origin
git log origin/integration/03-folha-pagamento --oneline | grep -E "be-024|be-026|be-027"
# Os três devem aparecer. Se algum faltar: não iniciar.
```

**Prompt para a implementação:**

```
Task: BE-028 — FecharMesUseCase + endpoint de fechamento.

Leia o plano completo:
  docs/sprints/03-folha-pagamento/plans/BE-028-fechar-mes.md
Leia também:
  docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §4 (algoritmo 11 passos — FONTE CANÔNICA)
  docs/decisions/0016-evo09-folha-pagamento.md §3

## PASSO ZERO — verificar requisitante_id ANTES de qualquer código

Abrir PedidoPagamentoEntity.java e verificar se requisitante_id é NOT NULL:
  grep -n "requisitanteId\|requisitante_id" financas_bot_telegram/src/main/java/.../entity/PedidoPagamentoEntity.java

Se NOT NULL sem default:
  Criar src/main/resources/db/migration/V6b__folha_requisitante_nullable.sql:
    ALTER TABLE pedidos_pagamento MODIFY COLUMN requisitante_id BIGINT NULL;
  Registrar a decisão no status report. Só então implementar o passo 8 do algoritmo.

## A TASK

Criar:
1. FecharMesPortIn + ConsultarFolhaPortIn (em application/port/in/).
2. FecharMesUseCase (em application/usecase/) — algoritmo de 11 passos, @Transactional obrigatório.
3. ConsultarFolhaUseCase.
4. FechamentoDuplicadoException + FuncionarioNaoEncontradoException (em domain/exception/).
5. DTOs: FecharMesRequest, PedidoFolhaResponse.

Modificar:
6. FolhaController.java — adicionar:
   - POST /api/funcionarios/{id}/fechamentos  (body: {mes: "2026-05", ajuste: 0.00})
   - GET  /api/funcionarios/{id}/fechamentos
7. Exception handler global — mapear FechamentoDuplicadoException → 409.

## OS 11 PASSOS (implementar nesta ordem exata):
1. existsFolha(funcionarioId, mesReferencia) → se true, lança FechamentoDuplicadoException
2. Buscar funcionário ativo ou lança FuncionarioNaoEncontradoException
3. Calcular período: primeiro e último dia do mês
4. Buscar vales abertos do período (findValesAbertos)
5. Buscar adiantamentos ativos (findAdiantamentosAtivos)
6. Calcular: valorFinal = salarioBase - totalVales - totalParcelas + ajuste
7. Gerar texto observacao com breakdown (formato exato: ver spec §4)
8. Criar Pedido FOLHA (categoria=FOLHA, status=PENDENTE, funcionario_id, mes_referencia, observacao)
9. Marcar vales como fechado=true (markAllClosed)
10. Para cada adiantamento: incrementar parcelas_pagas; se quitado, ativo=false
11. Retornar o Pedido FOLHA criado

## REGRAS

1. Branch: feature/be-028-fechar-mes de origin/integration/03-folha-pagamento
2. Território: SÓ financas_bot_telegram/. Zero frontend/.
3. FecharMesUseCase DEVE ter @Transactional.
4. DataIntegrityViolationException (race condition) → capturar e relançar como FechamentoDuplicadoException.
5. 1 commit: feat(BE-028): FecharMesUseCase + endpoint de fechamento
6. testes_novos >= 2 (smoke only — cobertura completa vem em BE-029)
7. Status report: docs/sprints/03-folha-pagamento/status/BE-028-fechar-mes.md
   Incluir OBRIGATORIAMENTE: decisão sobre requisitante_id (nullable por migration, sentinel, etc).
8. Após status report: criar PR para integration/03-folha-pagamento e mergear.
```

**Mensagem de commit:** `feat(BE-028): FecharMesUseCase + endpoint de fechamento`

---

## TASK 6 — BE-029: Testes FecharMes

**Pré-condição:** BE-028 mergeada em integration.
**Verificar antes de começar:**
```bash
git fetch origin
git log origin/integration/03-folha-pagamento --oneline | grep be-028
```

**Prompt para a implementação:**

```
Task: BE-029 — Testes FecharMesUseCase (unitário) + integração endpoint fechamento.

Leia o plano completo:
  docs/sprints/03-folha-pagamento/plans/BE-029-testes-fechar-mes.md
Leia também:
  docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §4
  docs/decisions/0016-evo09-folha-pagamento.md §3
  docs/runbooks/ROTEIRO-TESTES-BACKEND.md
  FecharMesUseCase.java (já em integration — NÃO alterar)

## A TASK

Escrever APENAS testes. NÃO alterar a implementação do use case.
Se encontrar bug: documentar no status report, marcar estado: parcial, NÃO corrigir.

Criar:
1. FecharMesUseCaseTest.java (unitário, Mockito) — 8 cenários:
   1. Fechamento padrão: 3 vales + 2 adiantamentos + ajuste=0 → valorFinal correto
   2. Adiantamento quitado (última parcela): ativo=false após fechamento
   3. Sem vales nem adiantamentos: valorFinal = salarioBase
   4. Ajuste positivo (bonificação)
   5. Ajuste negativo (desconto extra)
   6. Valor final negativo (vales > salário): aceita e registra
   7. Fechamento duplicado: lança FechamentoDuplicadoException
   8. Funcionário inativo: lança FuncionarioNaoEncontradoException

2. FecharMesEndpointIT.java (integração, @SpringBootTest) — 2 cenários:
   1. Fluxo completo: criar funcionário + vales + adiantamento → POST fechamento → verificar resultado
   2. Idempotência: fechar mesmo mês duas vezes → 409

## REGRAS

1. Branch: feature/be-029-testes-fechar-mes de origin/integration/03-folha-pagamento
2. Território: SÓ financas_bot_telegram/src/test/. Zero src/main/. Zero frontend/.
3. NÃO alterar FecharMesUseCase.java nem FolhaController.java.
4. Se H2 rejeitar CHECK constraints: usar MODE=MySQL ou Testcontainers. Documentar.
5. 1 commit: test(BE-029): cobertura FecharMesUseCase — unitario + integracao endpoint
6. testes_novos >= 11
7. Status report: docs/sprints/03-folha-pagamento/status/BE-029-testes-fechar-mes.md
   Incluir: lista dos 8 cenários unitários + decisão H2 vs Testcontainers.
8. Após status report: criar PR para integration/03-folha-pagamento e mergear.
```

**Mensagem de commit:** `test(BE-029): cobertura FecharMesUseCase — unitario + integracao endpoint`

---

## AO FINAL (todas as tasks concluídas)

```bash
git fetch origin
git log origin/integration/03-folha-pagamento --oneline | grep -E "be-024|be-025|be-026|be-027|be-028|be-029"
```
Todas as 6 devem aparecer. Backend Sprint 03 completo. Humano revisa de manhã.
