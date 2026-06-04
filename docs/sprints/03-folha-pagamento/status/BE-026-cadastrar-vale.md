---
task: BE-026
titulo: "CadastrarVale + endpoint de vales"
data: 2026-06-04
branch: feature/be-026-cadastrar-vale
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 320
  testes_novos: 6
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - placeholder
pr: null
desvios: 1
pendencias_humano: 0
---

# BE-026 — CadastrarVale + endpoint de vales

## O que foi feito

- Port in `CadastrarValePortIn` em `application/port/in/`.
- Service `CadastrarValeServiceImpl`: valida funcionário ativo, valor positivo, descrição obrigatória; cria Pedido com `categoria=VALE`, `status=PENDENTE`, `fechado=false`.
- DTOs: `ValeRequest` (Bean Validation), `ValeResponse` (factory `from(PedidoPagamento)`).
- `FolhaController` em `adapters/in/rest/folha/` com 2 endpoints: `POST /api/funcionarios/{id}/vales` e `GET /api/funcionarios/{id}/vales?mes=YYYY-MM`.
- `PedidoPagamentoRepositoryPort` + JPA repository + adapter estendidos com `findValesByFuncionarioAndPeriodo`.
- 6 novos testes unitários no `CadastrarValeServiceImplTest`.
- `mvn test` verde: 320 testes, 0 falhas.

**Decisão sobre status do vale:** vale nasce `PENDENTE` — representa um compromisso a ser descontado no fechamento do mês. `PAGO` seria prematuro, pois o desconto no salário ainda não ocorreu mesmo que o dinheiro tenha sido entregue em espécie. Confirmado como padrão seguro pelo plano.

---

## Desvios do plano

1. **`FolhaController` em `adapters/in/rest/folha/`** em vez de `adapters/in/web/` — mesma justificativa de BE-025: o projeto usa `adapters/in/rest/` para todos os controllers REST. O controller é criado aqui (BE-026) e será expandido em BE-027 e BE-028.

---

## Decisões tomadas durante a execução

**`findValesByFuncionarioAndPeriodo` adicionado ao port:** O `findValesAbertos` existente filtra apenas `fechado=false`. O endpoint GET de listagem deve mostrar todos os vales do mês (abertos e fechados). Adicionado novo método ao port em vez de reusar `findValesAbertos`, que tem semântica diferente.

**Injeção direta de `PedidoPagamentoRepositoryPort` no controller** para o GET de listagem: o caso é simples o suficiente para não justificar um use case separado para listar vales. Documentado como trade-off (ver seção de padrões).

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **BE-027** deve adicionar endpoints ao `FolhaController` existente (já criado em `adapters/in/rest/folha/FolhaController.java`). Não criar um novo controller.
- **BE-028** também adiciona ao `FolhaController` — com cuidado para o `requisitante_id NOT NULL` na criação do Pedido FOLHA.

---

## Padrões técnicos

**SRP:** `CadastrarValeServiceImpl` — responsabilidade única: validar e criar o vale. Não conhece nem a entidade JPA nem o mapper — só trabalha com domínio.

**DIP:** `CadastrarValeServiceImpl` depende de `FuncionarioRepositoryPortOut` e `PedidoPagamentoRepositoryPort` (interfaces puras). O controller depende de `CadastrarValePortIn` (interface). Nenhuma classe concreta é injetada diretamente nas fronteiras.

**Trade-off — controller usando repository diretamente:** O `FolhaController.listarVales()` usa `PedidoPagamentoRepositoryPort` diretamente, sem use case intermediário. Trade-off consciente: criar `ListarValesUseCase` para uma query simples seria over-engineering. O port é puro (sem JPA) — o isolamento hexagonal é preservado.

**Arquitetura hexagonal:**
- `ValeRequest` e `ValeResponse` em `application/dto/` — contratos da API, não do domínio.
- A conversão `ValeRequest → PedidoPagamento` ocorre no controller (adapter in).
- O use case recebe e retorna `PedidoPagamento` (domínio).
- `FolhaController` em `adapters/in/rest/folha/` — claramente separado do domínio.

---

## Arquivos criados/modificados

- `application/port/in/CadastrarValePortIn.java` (novo)
- `application/services/CadastrarValeServiceImpl.java` (novo)
- `application/dto/ValeRequest.java` (novo)
- `application/dto/ValeResponse.java` (novo)
- `adapters/in/rest/folha/FolhaController.java` (novo)
- `application/port/out/PedidoPagamentoRepositoryPort.java` (modificado: +findValesByFuncionarioAndPeriodo)
- `adapters/out/persistence/PedidoPagamentoJpaRepository.java` (modificado: +query findValesByFuncionarioAndPeriodo)
- `adapters/out/persistence/PedidoPagamentoRepositoryAdapter.java` (modificado: +impl findValesByFuncionarioAndPeriodo)
- `test/.../services/CadastrarValeServiceImplTest.java` (novo — 6 testes)
