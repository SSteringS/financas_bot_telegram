# BE-09 — GET /api/v1/resumo (agregado de pendentes + pagos do mês)

**Data:** 2026-05-12
**Branch:** feature/backend-fase3-api-completa
**Responsável (instância):** Claude Code (CLI, overnight)

---

## O que foi feito

- `AgregadoStatus` record criado em `adapters/out/persistence/`
- `PedidoPagamentoJpaRepository` — adicionada query JPQL `agregarPorStatusNoIntervalo()` com `GROUP BY p.status` (uma única query)
- `AppConfig` — adicionado bean `Clock systemClock()` para injeção e testabilidade
- `ResumoMesUseCase` (interface) criada
- `ResumoMesServiceImpl`:
  - Usa `Clock` injetado → testável com `Clock.fixed()`
  - Intervalo: primeiro ao último dia do mês corrente
  - `CANCELADO` é ignorado automaticamente (não está nos campos `pendentes`/`pagos`)
  - Quando status ausente, retorna `ResumoStatusDTO(0, BigDecimal.ZERO)`
- `ResumoController` criado em `adapters/in/rest/resumo/` com `GET /api/v1/resumo`
- 162 testes totais, todos verdes

---

## mvn test — resultado

```
Tests run: 162, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Marco

Toda a superfície da API REST de leitura está implementada (BEs 05, 06, 07, 08, 09).

---

## Desvios do plano

Nenhum.

---

## Próximos passos

- BE-03: parsing `tipo` de pagamento da legenda (Telegram)

---

## Arquivos criados/modificados

**Modificados:**
- `adapters/out/persistence/PedidoPagamentoJpaRepository.java` (+ agregarPorStatusNoIntervalo)
- `infra/AppConfig.java` (+ Clock bean)

**Novos (produção):**
- `adapters/out/persistence/AgregadoStatus.java`
- `application/usecases/ResumoMesUseCase.java`
- `application/services/ResumoMesServiceImpl.java`
- `adapters/in/rest/resumo/ResumoController.java`

**Novos (testes):**
- `ResumoMesServiceImplTest`
- `ResumoControllerTest`
