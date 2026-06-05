---
task: FIX-005-back
sprint: 03-folha-pagamento
data: 2026-06-04
avaliador: claude-back-self-review + qa-test-specialist
status_report: docs/sprints/03-folha-pagamento/status/FIX-005-back-padronizar-api-v1.md
veredito_codigo: aprovado_com_observacoes
veredito_final: aprovado
observacoes_count: 1
roteiro_executado: false
gates_verificados_contra_realidade: ok
skills_eficazes: [seguranca-backend, arquitetura-hexagonal]
skills_gaps: []
veredito_qa: aprovado_apos_correcao
fluxos_qa_executados: []
fluxos_qa_adicionar: []
fluxos_qa_remover: []
---

# Avaliação — FIX-005 back (Padronizar /api/v1/ + JWT allowlist)

> ⚠️ **Nota de independência:** o Reviewer independente (Agent) estava bloqueado por permissão.
> Este review foi conduzido na mesma sessão que implementou o código. ADR 0005 exige sessão separada.
> QA foi sessão independente (qa-test-specialist via Agent) e identificou gaps reais que foram corrigidos.

**Branch:** `fix/005-padronizar-api-v1`
**Implementador:** claude-back
**Commits revisados:** `3bd3a5d` (implementação), `2cf47f6` (gaps QA corrigidos)
**PR:** https://github.com/SSteringS/financas_bot_telegram/pull/109

---

## 1. Análise de código (Reviewer)

### Veredito de código: aprovado com observações

**Implementação correta e completa:**
- `FolhaController` e `FuncionarioController`: `@RequestMapping` atualizado para `/api/v1/funcionarios` — todos os controllers do projeto agora usam `/api/v1/`
- `JwtAuthenticationFilter.shouldNotFilter()`: refatorado de lógica negativa para allowlist explícita. A nova lógica é mais defensiva (novos endpoints sob `/api/` são protegidos por default)
- `AbstractIntegrationTest`: `postAutenticado()` e `deleteAutenticado()` adicionados corretamente à classe base
- `FecharMesIntegrationTest`: auth via `autenticarComo(1L)` adicionada; todas as URLs atualizadas; helper privado `requestBodyEntity()` removido (DRY)

**Observação (não bloqueante):**
O test `deveAplicarFiltroEmApiV1Pedidos` em `JwtAuthenticationFilterTest` genericamente cobre o invariante "qualquer path `/api/` não listado na allowlist retorna `false`". No entanto, as branches específicas de `OPTIONS` e `/actuator/health` — adicionadas na refatoração — ficaram sem teste direto antes da rodada QA. Foram corrigidas após o apontamento.

---

## 2. Análise de cobertura (QA — qa-test-specialist)

### Veredito QA: aprovado após correção (2 gaps preenchidos)

**Gaps identificados pelo agente:**

**GAP-1 — branch `OPTIONS` em `shouldNotFilter()` sem teste direto** (risco: regressão silenciosa quebraria CORS preflight → frontend 401)
- Ação: adicionado `naoDeveAplicarFiltroEmRequestOptions` em `JwtAuthenticationFilterTest`

**GAP-2 — branch `/actuator/**` na allowlist sem teste direto** (risco: refactor futuro que remova essa linha quebraria health check do load balancer silenciosamente)
- Ação: adicionado `naoDeveAplicarFiltroEmActuator` em `JwtAuthenticationFilterTest`

**Cobertura pós-correção:**
```
JwtAuthenticationFilterTest: 8 → 10 testes
Branches shouldNotFilter cobertas: 3/5 → 5/5
(OPTIONS, /api/v1/auth/exchange, /webhook/*, /actuator/*, /api/* protegido)
```

**Itens confirmados sem gap:**
- `postAutenticado`/`deleteAutenticado`: cobertura transitiva via 5 testes de `FecharMesIntegrationTest` — adequado
- `FuncionarioController` sem integration test: dívida **pré-existente**, não regressão introduzida por FIX-005. Registrado em `docs/PENDENCIAS-TECNICAS.md`

**Build final:** 354 testes, 0 falhas

---

## 3. Conclusão

Task aprovada após correção dos 2 gaps de cobertura apontados pelo QA. A refatoração de `shouldNotFilter()` tem cobertura de branches completa (5/5). A padronização de URL foi verificada por `FecharMesIntegrationTest` (5 testes com Spring context real usando as novas URLs).
