---
task: BE-026
titulo: "CadastrarVale + endpoint de vales"
sprint: 03-folha-pagamento
data_planejamento: 2026-05-31
branch_alvo: feature/be-026-cadastrar-vale
prioridade: alta
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: [BE-024]
bloqueia: [BE-028]
skills_dispatched: [arquitetura-hexagonal, ecossistema-spring]
fluxos_qa: []
---

# BE-026 — CadastrarVale + endpoint de vales

## Intake

- **Origem:** spec EVO-09 §3 (CadastrarValePortIn) + §5 (endpoints de vales).
- **Por quê agora:** BE-028 (FecharMes) busca vales abertos para calcular o fechamento.
- **Esforço:** baixo — um use case simples, 2 endpoints, sem lógica complexa.
- **Riscos resumidos:** ✅ **Decisão §7 resolvida (2026-06-04 — PO):** vales NÃO aparecem na lista do Pedro. **Opção A** — `GET /api/pedidos` fica intocado. Esta task apenas entrega o CRUD de vales em `/api/funcionarios/{id}/vales`; nada em `PedidoController.java`.

---

## Contexto

Vale = Pedido com `categoria=VALE`, `funcionario_id=<id>`, `fechado=FALSE`, `status=PENDENTE`. Não tem comprovante (PO confirmou: vale é dado em dinheiro na hora → nasce pago ou sem necessidade de comprovante — verificar com PO se `status=PENDENTE` ou `status=PAGO` ao criar).

**Pergunta a esclarecer antes de implementar:** vale nasce `PENDENTE` (aguarda comprovante) ou `PAGO` (entregue em espécie)?

Endpoints desta task:
```
POST   /api/funcionarios/{id}/vales
GET    /api/funcionarios/{id}/vales?mes=YYYY-MM
```

**NÃO está no escopo desta task:** modificar `GET /api/pedidos` — isso depende da decisão §7.

---

## Decisão / abordagem

Use case `CadastrarValeUseCase`:
- Verificar que funcionário existe e está ativo.
- Criar Pedido com `categoria=VALE`, `funcionario_id`, `fechado=false`.
- `status`: definir conforme resposta do PO (PENDENTE ou PAGO).
- `descricao`: obrigatório (ex: "Vale alimentação").
- `valor`: obrigatório e positivo.
- `data_pedido`: data do vale (padrão: hoje, mas permitir informar data retroativa).

`GET /api/funcionarios/{id}/vales?mes=YYYY-MM` filtra `funcionario_id=? AND categoria=VALE AND mes=YYYY-MM` (por `data_criacao` ou `data_pedido`). Se `mes` ausente, retorna mês corrente.

---

## Escopo / arquivos

### Criar
- `application/port/in/CadastrarValePortIn.java`
- `application/usecase/CadastrarValeUseCase.java`
- DTOs: `ValeRequest.java`, `ValeResponse.java`.

### Modificar
- `adapters/in/web/FolhaController.java` — adicionar endpoints de vale.

### Explicitamente NÃO tocar
- `PedidoController.java` / `GET /api/pedidos` — decisão §7 pendente; tarefa separada.

---

## Testes

- **Unitários:** `CadastrarValeUseCase` — funcionário ativo cria vale (ok), funcionário inativo rejeita (400), valor negativo rejeita (400).
- **Integração:** `POST` cria, `GET` lista por mês.

`testes_total` esperado: ≥ testes_existentes + 5. `testes_novos` ≥ 5.

---

## Critérios de aceitação

- [ ] `POST /api/funcionarios/{id}/vales` cria vale e retorna 201.
- [ ] Funcionário inativo → 400.
- [ ] Valor negativo/zero → 400.
- [ ] `GET /api/funcionarios/{id}/vales?mes=2026-05` retorna só vales do mês.
- [ ] Vale criado tem `categoria=VALE`, `funcionario_id` correto, `fechado=false`.
- [ ] `mvn test` verde com `testes_novos ≥ 5`.
- [ ] Branch: `feature/be-026-cadastrar-vale`.
- [ ] Status report com frontmatter válido — incluindo decisão sobre `status` do vale (PENDENTE vs PAGO) confirmada com PO.

---

## Fora de escopo

- Modificar `GET /api/pedidos` para filtrar vales — decisão §7, tarefa separada.
- Marcar vales como `fechado=true` — ocorre no `FecharMesUseCase` (BE-028).

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| ~~Decisão §7 atrasar outras tasks~~ | ✅ Resolvida | — | Opção A decidida pelo PO em 2026-06-04 — sem bloqueio |
| `status` do vale incorreto (PO não confirmou) | Média | Baixo | Esclarecer antes de implementar; padrão seguro: PENDENTE |

---

## Coordenação

- **Pode rodar em paralelo com:** BE-025, BE-027.
- **Depende sequencialmente de:** BE-024.
- **Bloqueia:** BE-028.
- **Atenção pro Reviewer:** verificar que `GET /api/pedidos` NÃO foi tocado; confirmar `categoria=VALE` e `fechado=false` no Pedido criado.
- **Após merge:** BE-028 pode iniciar (junto com BE-027).

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR pra `develop`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §3, §5, §7
- `docs/decisions/0016-evo09-folha-pagamento.md`
