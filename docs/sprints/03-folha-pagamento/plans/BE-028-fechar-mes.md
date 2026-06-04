---
task: BE-028
titulo: "FecharMesUseCase + endpoint de fechamento"
sprint: 03-folha-pagamento
data_planejamento: 2026-05-31
branch_alvo: feature/be-028-fechar-mes
prioridade: alta
esforco: medio
territorio: back
estado: pronto-pra-execucao
depende_de: [BE-024, BE-026, BE-027]
bloqueia: [BE-029, FE-016, FE-017]
skills_dispatched: [arquitetura-hexagonal, ecossistema-spring, qualidade-de-testes]
fluxos_qa: []
---

# BE-028 — FecharMesUseCase + endpoint de fechamento

## Intake

- **Origem:** spec EVO-09 §4 (lógica do FecharMes) + §5 (endpoints de fechamento) + ADR 0016 §3.
- **Por quê agora:** o fechamento é a operação central da folha; FE-016 e FE-017 dependem dele.
- **Esforço:** médio — algoritmo de 11 passos, dupla proteção de idempotência (check + UNIQUE INDEX), transação completa.
- **Riscos resumidos:** `requisitante_id` pode ser NOT NULL na tabela — verificar e resolver antes de criar Pedido FOLHA. Race condition coberta pelo UNIQUE INDEX mas `DataIntegrityViolationException` precisa ser traduzida.

---

## Contexto

O `FecharMesUseCase` implementa o algoritmo de 11 passos da spec §4:
1. Idempotência (check no banco + UNIQUE INDEX como guarda no banco)
2. Buscar funcionário ativo
3. Calcular período do mês
4. Buscar vales abertos do período
5. Buscar adiantamentos ativos para o mês
6. Calcular valor final (`salario_base - totalVales - totalParcelas + ajuste`)
7. Gerar texto de observação com o breakdown
8. Criar Pedido FOLHA (categoria=FOLHA, status=PENDENTE)
9. Marcar vales como `fechado=true`
10. Incrementar `parcelas_pagas` em adiantamentos; desativar quitados
11. Retornar o Pedido FOLHA criado

**Atenção: `requisitante_id` em `pedidos_pagamento`** — verificar se aceita NULL antes de criar o Pedido FOLHA. Se NOT NULL, avaliar: (a) tornar nullable via ALTER (nova migration V6b?), ou (b) usar valor sentinel (ex: `requisitante_id=0` ou o telegram_user_id do operador). Registrar a decisão no status report.

---

## Decisão / abordagem

- Use case anotado com `@Transactional` — todos os 11 passos em uma transação.
- Passo 1: `existsFolha(funcionarioId, mesReferencia)` → se true, lança `FechamentoDuplicadoException` (mapeada para 409).
- Passo 8: `pedidoRepo.criar(...)` — se UNIQUE INDEX rejeitar (race condition), capturar `DataIntegrityViolationException` e relançar como `FechamentoDuplicadoException`.
- Texto da observação: formato humano conforme spec §4 (passo 7 — exemplo de saída fornecido).
- `ajuste` é um parâmetro do endpoint (body: `{ "mes": "2026-05", "ajuste": 0.00 }`). Pode ser zero ou positivo/negativo para bonificação/desconto extra.

**Endpoint GET /api/funcionarios/{id}/fechamentos:** retorna lista de Pedidos WHERE `categoria=FOLHA AND funcionario_id={id}`, ordenada por `mes_referencia DESC`.

---

## Escopo / arquivos

### Criar
- `application/port/in/FecharMesPortIn.java`
- `application/port/in/ConsultarFolhaPortIn.java`
- `application/usecase/FecharMesUseCase.java` — algoritmo de 11 passos, `@Transactional`.
- `application/usecase/ConsultarFolhaUseCase.java`
- `domain/exception/FechamentoDuplicadoException.java`
- `domain/exception/FuncionarioNaoEncontradoException.java` (se não existe já).
- DTOs: `FecharMesRequest.java`, `PedidoFolhaResponse.java`.

### Modificar
- `adapters/in/web/FolhaController.java` — adicionar: `POST /api/funcionarios/{id}/fechamentos` e `GET /api/funcionarios/{id}/fechamentos`.
- Exception handler global — mapear `FechamentoDuplicadoException` → 409.

### Verificar (pré-implementação)
- `PedidoPagamentoEntity.requisitanteId` — nullable? Se NOT NULL, criar migration V6b ou usar workaround antes de implementar o passo 8.

---

## Testes

Cobertos por BE-029 (teste unitário do FecharMesUseCase + integração do endpoint). Esta task entrega a implementação; BE-029 entrega a cobertura de testes completa.

Testes mínimos aqui (smoke):
- Compilação limpa.
- `mvn test` verde (testes existentes não quebram).

`testes_novos` aqui: ≥ 2 (smoke). Cobertura completa: BE-029.

---

## Critérios de aceitação

- [ ] `POST /api/funcionarios/{id}/fechamentos` com body `{ "mes": "2026-05", "ajuste": 0.00 }` retorna 200 com Pedido FOLHA criado (campos: id, valor, status=PENDENTE, observacao preenchida, mes_referencia).
- [ ] Segundo POST com mesmo `{id, mes}` → 409 (`FechamentoDuplicadoException`).
- [ ] `GET /api/funcionarios/{id}/fechamentos` lista fechamentos anteriores ordenados por `mes_referencia DESC`.
- [ ] Observação do Pedido FOLHA contém breakdown legível (salário base, vales, adiantamentos, ajuste, líquido).
- [ ] Vales do período ficam com `fechado=true` após o fechamento.
- [ ] Adiantamentos têm `parcelas_pagas` incrementado; quitados têm `ativo=false`.
- [ ] `requisitante_id` resolvido (nullable ou workaround documentado no status report).
- [ ] `mvn test` verde.
- [ ] Branch: `feature/be-028-fechar-mes`.
- [ ] Status report com frontmatter válido, incluindo decisão sobre `requisitante_id`.

---

## Fora de escopo

- Testes de cobertura completa do FecharMes — BE-029.
- Reabertura de mês fechado — não está no MVP (PO confirmou: ajuste no mês seguinte).
- Notificação automática após fechamento — fora do MVP.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| `requisitante_id` NOT NULL bloqueia criação de Pedido FOLHA | Média | Alto | Verificar schema antes de implementar passo 8; criar V6b se necessário |
| Race condition duplo-clique passa pelo check mas bate no UNIQUE INDEX | Baixa | Médio | Capturar `DataIntegrityViolationException` e relançar como `FechamentoDuplicadoException` |
| Valor final negativo (vales > salário) | Baixa | Baixo | Domínio aceita; registrar no texto da observação; FE exibe aviso (FE-017) |
| Transação parcialmente aplicada (falha no passo 9 ou 10) | Muito baixa | Alto | `@Transactional` reverte tudo em caso de exceção — verificar que Spring gerencia a transação corretamente |

---

## Coordenação

- **Pode rodar em paralelo com:** nada (depende de BE-024, 026, 027).
- **Depende sequencialmente de:** BE-024, BE-026, BE-027.
- **Bloqueia:** BE-029, FE-016, FE-017.
- **Atenção pro Reviewer:** verificar `@Transactional`; confirmar que `DataIntegrityViolationException` é capturada e traduzida; verificar que ALL 11 passos estão presentes e na ordem correta; verificar tratamento de `requisitante_id`.
- **Após merge:** despachar BE-029, FE-016, FE-017 em paralelo.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido (com decisão sobre `requisitante_id` documentada), revisão do Reviewer. PR pra `develop`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §4 (algoritmo completo) e §5
- `docs/decisions/0016-evo09-folha-pagamento.md` §3 (idempotência)
- `docs/architecture/estado-atual-dev.md` (verificar `requisitante_id`)
