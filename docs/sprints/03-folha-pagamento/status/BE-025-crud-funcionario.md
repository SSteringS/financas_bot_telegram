---
task: BE-025
titulo: "CadastrarFuncionario + CRUD /api/funcionarios"
data: 2026-06-04
branch: feature/be-025-crud-funcionario
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 314
  testes_novos: 14
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - placeholder
pr: null
desvios: 1
pendencias_humano: 0
---

# BE-025 — CadastrarFuncionario + CRUD /api/funcionarios

## O que foi feito

- Ports in: `CadastrarFuncionarioPortIn`, `AtualizarFuncionarioPortIn` em `application/port/in/`.
- Service implementations: `CadastrarFuncionarioServiceImpl`, `AtualizarFuncionarioServiceImpl` em `application/services/`.
- DTOs: `FuncionarioRequest` (com Bean Validation em campos comuns) e `FuncionarioResponse` (com factory `from(Funcionario)`).
- Controller: `FuncionarioController` em `adapters/in/rest/funcionario/` com 5 endpoints (POST, GET lista, GET por id, PUT, DELETE soft).
- Exceção de domínio `FuncionarioNaoEncontradoException` criada em `domain/exceptions/`.
- Handler adicionado em `RestExceptionHandler` → 404 para `FuncionarioNaoEncontradoException`.
- Validação condicional PIX/TED implementada no use case (`CadastrarFuncionarioServiceImpl.validarDadosPagamento`).
- `mvn test` verde: 314 testes, 0 falhas, 14 novos.

---

## Desvios do plano

1. **Controller em `adapters/in/rest/funcionario/`** em vez de `adapters/in/web/` — o projeto usa `adapters/in/rest/` como convenção para todos os controllers REST (pattern já estabelecido em PedidoController, ResumoController, etc.). Seguir a estrutura existente evita inconsistência no package layout.

---

## Decisões tomadas durante a execução

**Validação condicional PIX/TED no use case (não em Bean Validation):** O plano sugeria `@AssertTrue` mas a validação foi implementada como método estático em `CadastrarFuncionarioServiceImpl.validarDadosPagamento()`, reutilizado também em `AtualizarFuncionarioServiceImpl`. Isso mantém a lógica de domínio no use case (não no DTO) e permite mensagens de erro descritivas e testáveis.

**`AtualizarFuncionarioServiceImpl` reutiliza `validarDadosPagamento`:** Em vez de duplicar a validação, o atualizar chama o mesmo método estático após aplicar as mudanças no funcionário existente. Isso garante que um PUT parcial não quebre as invariantes PIX/TED.

**Testes de controller com Mockito puro** (não `@WebMvcTest`): O projeto usa `@ExtendWith(MockitoExtension.class)` para testes de controller — padrão estabelecido em `PedidoControllerDetalheTest`. O `@WebMvcTest` falhava por dependências do `GlobalTelegramExceptionHandler` que precisaria de mais mocks.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- **FE-015** pode iniciar após este merge.
- Os endpoints usam path `/api/funcionarios` (sem `/v1/`) — consistente com a decisão de não versionamento na API da folha (novo domínio, pode estabelecer a convenção). Se o FE esperar `/api/v1/funcionarios`, ajustar antes do FE-015.

---

## Padrões técnicos

**SRP:** `CadastrarFuncionarioServiceImpl` — responsabilidade única: cadastrar com validação. `AtualizarFuncionarioServiceImpl` — responsabilidade única: aplicar patch e revalidar. `FuncionarioController` — responsabilidade única: traduzir HTTP ↔ domínio.

**DIP:** `FuncionarioController` depende de `CadastrarFuncionarioPortIn`, `AtualizarFuncionarioPortIn` e `FuncionarioRepositoryPortOut` — interfaces puras. Não conhece `CadastrarFuncionarioServiceImpl` nem `FuncionarioRepositoryAdapter`.

**OCP (Strategy implícita):** `validarDadosPagamento` é extensível — se um terceiro `FormaPagamento` for adicionado, basta adicionar um `else if` sem quebrar os casos existentes. O método é `package-private static` para ser testável e reutilizável.

**Arquitetura hexagonal:**
- `FuncionarioRequest`/`FuncionarioResponse` ficam em `application/dto/` — são contratos da API, não do domínio.
- A conversão `FuncionarioRequest → Funcionario` acontece no controller (`toFuncionario()`).
- `FuncionarioResponse.from(Funcionario)` é um factory no DTO — evita que o domínio conheça os DTOs.
- Os use cases recebem e retornam `Funcionario` (domínio) — não DTOs. Isolamento mantido.

**Trade-off:** `FuncionarioController` acessa `FuncionarioRepositoryPortOut` diretamente para o GET por ID e o DELETE. Alternativa seria criar `BuscarFuncionarioPortIn` e `DesativarFuncionarioPortIn`, mas para operações triviais isso seria over-engineering. Documentado aqui caso o Reviewer queira padronizar.

---

## Arquivos criados/modificados

- `application/port/in/CadastrarFuncionarioPortIn.java` (novo)
- `application/port/in/AtualizarFuncionarioPortIn.java` (novo)
- `application/services/CadastrarFuncionarioServiceImpl.java` (novo)
- `application/services/AtualizarFuncionarioServiceImpl.java` (novo)
- `application/dto/FuncionarioRequest.java` (novo)
- `application/dto/FuncionarioResponse.java` (novo)
- `adapters/in/rest/funcionario/FuncionarioController.java` (novo)
- `domain/exceptions/FuncionarioNaoEncontradoException.java` (novo)
- `adapters/in/rest/RestExceptionHandler.java` (modificado: +handler FuncionarioNaoEncontradoException)
- `test/.../services/CadastrarFuncionarioServiceImplTest.java` (novo — 6 testes)
- `test/.../funcionario/FuncionarioControllerTest.java` (novo — 8 testes)
