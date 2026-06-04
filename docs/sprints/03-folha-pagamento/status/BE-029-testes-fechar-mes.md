---
task: BE-029
titulo: "Testes — FecharMesUseCase (unitário) + integração endpoint fechamento"
data: 2026-06-04
branch: feature/be-029-testes-fechar-mes
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: ok
  testes: ok
  testes_total: 348
  testes_novos: 16
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits:
  - null
pr: null
desvios: 0
pendencias_humano: 0
---

# BE-029 — Testes — FecharMesUseCase + integração endpoint fechamento

## O que foi feito

Cobertura completa do `FecharMesServiceImpl` com 16 novos testes (plan pedia ≥ 11):

**Unitários — `FecharMesServiceImplTest` (12 testes):**
- `deveFazerFechamentoComValesEAdiantamentos` — fluxo principal, verifica `markAllClosed` com IDs corretos e 2 chamadas a `adiantamentoRepository.save`.
- `deveSalvarPedidoFolhaComCamposCorretos` — captura o pedido salvo e verifica `categoria=FOLHA`, `status=PENDENTE`, `mesReferencia`, `observacao` não-vazio, e `requisitanteId=null` (pedido sistema).
- `deveDesativarAdiantamentoNaUltimaParcela` — após incremento de `parcelasPagas`, `ativo=false`.
- `deveFazerFechamentoSemValesNemAdiantamentos` — `valor = salárioBase`, zero chamadas a `markAllClosed` e `adiantamentoRepository.save`.
- `deveAplicarAjustePositivo` — `valor = salárioBase + ajuste`.
- `deveAplicarAjusteNegativo` — `valor = salárioBase - |ajuste|`.
- `devePermitirValorFinalNegativo` — domínio aceita sem lançar exceção.
- `deveRejeitarFechamentoDuplicado` — `existsFolha=true` → `FechamentoDuplicadoException`; `save` nunca chamado.
- `deveRejeitarFuncionarioInativo` — `findAtivoById` vazio → `FuncionarioNaoEncontradoException`.
- `deveGerarTextoFechamentoComFormatoCorreto` — verifica formato PT-BR ("R$ 2.000,00", vírgula decimal, ponto milhar).
- `deveUsarSingularParaUmValeEUmaParcela` — "1 vale" e "1 parcela" (singular).
- `deveGerarTextoParaFechamentoSemDescontos` — "0 vales", "0 parcelas".

**Integração — `FecharMesIntegrationTest` (4 testes, MySQL Testcontainers):**
- `deveFecharMesComSucesso` — cria funcionário + vale via SQL, chama endpoint, verifica 200 + vale marcado `fechado=true` + Pedido FOLHA criado no banco.
- `deveRetornar409NoFechamentoDuplicado` — dois POSTs no mesmo mês; segundo retorna 409 com `FECHAMENTO_DUPLICADO`.
- `deveRetornar404ParaFuncionarioInexistente` — id 999999 não existe → 404.
- `deveListarFechamentosAnteriores` — GET `/fechamentos` retorna dois meses em ordem DESC.

Total: 348 testes, 0 falhas.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

- **Naming `FecharMesIntegrationTest` (não `FecharMesEndpointIT`)**: Maven Surefire não detecta classes com sufixo `IT` por padrão (convenção Failsafe). Todos os integration tests existentes no projeto usam `*IntegrationTest`. Seguido o padrão estabelecido.
- **`gerarTextoFechamento` testado como static package-private**: o método é `static` no `FecharMesServiceImpl` — testado diretamente sem instanciar o serviço, o que foca o teste na função de formatação sem necessidade de mocks. Assertiva de pluralização (singular/plural) coberta em testes separados.
- **Cleanup via `@AfterEach`** no teste de integração: cada teste de integração cria seu próprio funcionário e limpa ao final, garantindo isolamento entre testes na mesma sessão do MySQL compartilhado (Singleton Container).

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- Sprint 03 back está completa. FE-016 e FE-017 podem iniciar.
- A V7 migration (`requisitante_id` nullable) aplicará automaticamente via Flyway no próximo deploy.
- Os testes de integração são skipped localmente quando Docker não está disponível (pre-existente; CI resolve). Ver `project_docker_testcontainers.md` no memory.

---

## Padrões técnicos (BE e FE: obrigatório; DEP/CI/EVO: omitir)

**SOLID:**
- **DIP nos testes unitários**: `FecharMesServiceImpl` é instanciado com mocks dos ports (`FuncionarioRepositoryPortOut`, `PedidoPagamentoRepositoryPort`, `AdiantamentoRepositoryPortOut`) — os testes verificam o comportamento lógico sem precisar de banco ou Spring context.

**Design Patterns:**
- **Singleton Container (Testcontainers)**: o MySQL é iniciado uma vez por JVM (`static final MySQLContainer`) e compartilhado por todos os testes de integração, conforme padrão do `AbstractIntegrationTest`. Isolar cada teste via `@BeforeEach` + `@AfterEach` SQL é a prática correta com este padrão.
- **Arrange-Act-Assert**: todos os testes unitários seguem AAA explicitamente — setup dos mocks, chamada do método, assertivas nos resultados e verificações de interação (`verify`).

**Arquitetura hexagonal:**
- Os testes unitários verificam o **comportamento do use case** (lógica dos 11 passos), não a implementação dos adapters. Os testes de integração verificam a **stack completa** (HTTP → controller → use case → adapters → banco).
- O teste `deveSalvarPedidoFolhaComCamposCorretos` captura o `PedidoPagamento` passado para o port out — confirmando que o use case não vaza detalhes de infraestrutura (ex: `requisitanteId=null` é preenchido pelo use case, não pelo adapter).

---

## Arquivos criados/modificados

- `test/.../services/FecharMesServiceImplTest.java` (novo: 12 testes unitários)
- `test/.../integration/FecharMesIntegrationTest.java` (novo: 4 testes de integração)
