---
task: FIX-005
titulo: "Padronizar /api/funcionarios/** → /api/v1/funcionarios/** + proteger com JWT (back)"
data: 2026-06-04
branch: fix/005-padronizar-api-v1
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 352
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits: []
pr: null
desvios: 0
pendencias_humano: 0
---

# FIX-005 (back) — Padronizar `/api/funcionarios/**` → `/api/v1/funcionarios/**` + JWT allowlist

## O que foi feito

**Fix primário — padronização de URL:**
`FolhaController` e `FuncionarioController` agora mapeiam `@RequestMapping("/api/v1/funcionarios")`. Os 11 endpoints que estavam sob `/api/funcionarios/**` foram movidos para `/api/v1/funcionarios/**`, eliminando o prefixo fora do padrão do projeto. Javadocs de ambos os controllers atualizados.

**Fix secundário — `shouldNotFilter()` com allowlist explícita:**
`JwtAuthenticationFilter.shouldNotFilter()` foi refatorado de lógica negativa (`!path.startsWith("/api/v1/")`) para allowlist positiva. A nova lógica lista explicitamente os paths públicos (`/api/v1/auth/exchange`, `/webhook`, `/actuator`) e protege qualquer path sob `/api/` que não esteja na lista. Endpoints futuros sob `/api/v2/`, `/api/internal/` etc. são automaticamente protegidos sem alterar o filtro.

**`AbstractIntegrationTest` — helpers de POST e DELETE autenticados:**
Adicionados `postAutenticado(url, cookie, jsonBody, responseType)` e `deleteAutenticado(url, cookie)`. Eliminam a necessidade de montar `HttpEntity` com headers duplicados em cada teste.

**`FecharMesIntegrationTest` — autenticação + URLs atualizadas:**
Todos os 5 testes agora chamam `autenticarComo(1L)` no `@BeforeEach` e usam `postAutenticado`/`getAutenticado` com as novas URLs `/api/v1/funcionarios/**`. O helper privado `requestBodyEntity()` foi removido por redundância. Zero regressões.

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

**`autenticarComo(1L)` em vez de `autenticarComo(12345L)`:** o plano sugeria `12345L` como exemplo, mas `GerarTokenConviteServiceImpl.gerar()` valida a existência do requisitante via FK. A migration V2 só insere id=1 (`Satyan Saita`). Usando id=1, consistente com o padrão já adotado em `PedidosListIntegrationTest` e `AuthFlowIntegrationTest`.

**`requestBodyEntity()` removido:** com `postAutenticado()` disponível na superclasse, o helper privado de `FecharMesIntegrationTest` que montava `HttpEntity` com Content-Type ficou duplicado. Removido — DRY.

**Cleanup de `auth_token` não adicionado:** os testes existentes (`PedidosListIntegrationTest`, `AuthFlowIntegrationTest`) que já usam `autenticarComo(1L)` não limpam `auth_token` em `@AfterEach`. O token gerado é consumido no `exchange` (campo `usado_em` preenchido) — não bloqueia testes subsequentes. Mantido o padrão pré-existente.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- O **front** (branch `fix/005-padronizar-api-v1-front` saindo de `develop`) deve atualizar todas as chamadas `/api/funcionarios` → `/api/v1/funcionarios` em `frontend/src/`.
- Após back + front mergearem: planner desbloqueia **QA-009** (remoção da nota "não escrever 401/403"; Sub-área A deve incluir cenários 401 + URLs corretas).
- Planner deve adicionar smell de "consistência de prefixo de URL" ao checklist de `docs/roles/reviewer.md` (issue identificada no plano FIX-005).
- Marcar item "JWT" em `docs/PENDENCIAS-TECNICAS.md` como `~~resolvido~~` após merge.

---

## Padrões e decisões técnicas

**OCP (Open/Closed Principle) em `shouldNotFilter()`:** a lógica anterior baseada em negação (`!path.startsWith("/api/v1/")`) estava "aberta à quebra" — qualquer novo prefixo de API (`/api/v2/`, `/api/internal/`) herdaria o gap silenciosamente. A allowlist explícita fecha esse vetor: novos prefixos são protegidos *por padrão*, não por omissão.

**Arquitetura hexagonal — adapter in/rest isolado do domínio:** a mudança de URL é puramente no adapter de entrada (`adapters/in/rest/folha/FolhaController`, `adapters/in/rest/funcionario/FuncionarioController`). Nenhuma port, use case ou entidade de domínio foi tocada — o contrato interno entre camadas continua idêntico.

**DRY em testes via herança:** `postAutenticado()` e `deleteAutenticado()` vão em `AbstractIntegrationTest` (camada de test infrastructure), não duplicados em cada subclasse. `FecharMesIntegrationTest` herda e usa diretamente. Quando QA-009 criar `ValeIntegrationTest`, `AdiantamentoIntegrationTest` etc., todos herdarão os mesmos helpers sem cópia.

**Consistência de URL como princípio arquitetural (LSP análogo):** controllers REST são "subclasses comportamentais" da API. Se `PedidoController`, `AuthController`, `ResumoController` usam `/api/v1/`, todo controller novo deve seguir o mesmo padrão — um consumidor que assume o prefixo `/api/v1/` não deve ser surpreendido por um `/api/funcionarios/`. O fix restaura esse invariante.

---

## Arquivos criados/modificados

- `adapters/in/rest/folha/FolhaController.java` (modificado: `@RequestMapping` + Javadoc → `/api/v1/funcionarios`)
- `adapters/in/rest/funcionario/FuncionarioController.java` (modificado: idem)
- `infra/security/JwtAuthenticationFilter.java` (modificado: `shouldNotFilter()` → allowlist explícita)
- `integration/AbstractIntegrationTest.java` (modificado: adicionados `postAutenticado()` e `deleteAutenticado()`)
- `integration/FecharMesIntegrationTest.java` (modificado: auth + URLs + remover `requestBodyEntity()`)
- `docs/sprints/03-folha-pagamento/status/FIX-005-back-padronizar-api-v1.md` (novo — este arquivo)
