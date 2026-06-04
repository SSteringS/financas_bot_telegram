---
task: BE-027
titulo: "CadastrarAdiantamento + endpoints adiantamentos + cancelamento"
sprint: 03-folha-pagamento
data_planejamento: 2026-05-31
branch_alvo: feature/be-027-cadastrar-adiantamento
prioridade: alta
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: [BE-024]
bloqueia: [BE-028]
skills_dispatched: [arquitetura-hexagonal, ecossistema-spring]
fluxos_qa: []
---

# BE-027 — CadastrarAdiantamento + endpoints adiantamentos + cancelamento

## Intake

- **Origem:** spec EVO-09 §3 (ports in) + §5 (endpoints) + §8 (BE-folha-5).
- **Por quê agora:** BE-028 (FecharMes) precisa de adiantamentos ativos pra calcular o desconto.
- **Esforço:** baixo — estrutura análoga ao BE-026; lógica de domínio é simples (CHECK matemático já está no banco).
- **Riscos resumidos:** consistência entre `valor_total`, `valor_parcela` e `num_parcelas` — validar também no backend (não depender só do CHECK do banco). Cancelamento deve verificar se adiantamento já está quitado.

---

## Contexto

Após BE-024, `AdiantamentoRepositoryPortOut` existe. Esta task entrega endpoints de adiantamento.

Adiantamento é um **plano de desconto parcelado**. `ativo=false` pode ser quitado (automaticamente quando `parcelas_pagas == num_parcelas`) ou cancelado (manualmente pelo filho).

---

## Decisão / abordagem

Use case `CadastrarAdiantamentoUseCase` valida a consistência matemática do plano antes de salvar (`|valor_total - valor_parcela * num_parcelas| <= 0.01`). Se inconsistente, rejeita com 400 antes de chegar no banco.

`DELETE /adiantamentos/{id}` cancela (soft: `ativo=false`). Verificar que já não está quitado (parcelas_pagas == num_parcelas) — se quitado, retornar 409 com mensagem clara.

`GET /funcionarios/{id}/adiantamentos` retorna apenas os **ativos** por padrão. Adicionar `?incluirInativos=true` se necessário (decidir na implementação se o front precisa ver histórico).

---

## Escopo / arquivos

### Criar
- `application/port/in/CadastrarAdiantamentoPortIn.java`
- `application/port/in/CancelarAdiantamentoPortIn.java`
- `application/usecase/CadastrarAdiantamentoUseCase.java`
- `application/usecase/CancelarAdiantamentoUseCase.java`
- DTOs: `AdiantamentoRequest.java`, `AdiantamentoResponse.java`.

### Modificar
- `adapters/in/web/FolhaController.java` (ou criar se ainda não existe) — adicionar endpoints de adiantamento.

### Não tocar
- `CadastrarValeUseCase` — BE-026.
- `FecharMesUseCase` — BE-028.

---

## Testes

- **Unitários:** `CadastrarAdiantamentoUseCase` — plano consistente (ok), plano inconsistente (400), `CancelarAdiantamentoUseCase` — cancelar ativo (ok), cancelar quitado (409).
- **Integração:** endpoints `POST`, `GET`, `DELETE` de adiantamento.

`testes_total` esperado: ≥ testes_existentes + 6. `testes_novos` ≥ 6.

---

## Critérios de aceitação

- [ ] `POST /api/funcionarios/{id}/adiantamentos` cria adiantamento e retorna 201.
- [ ] Plano matemático inconsistente → 400 (validado antes do banco).
- [ ] `GET /api/funcionarios/{id}/adiantamentos` lista ativos.
- [ ] `DELETE /api/adiantamentos/{id}` seta `ativo=false` (soft).
- [ ] Cancel de adiantamento já quitado → 409.
- [ ] `mvn test` verde com `testes_novos ≥ 6`.
- [ ] Branch: `feature/be-027-cadastrar-adiantamento`.
- [ ] Status report com frontmatter válido.

---

## Fora de escopo

- Incremento de `parcelas_pagas` — ocorre no `FecharMesUseCase` (BE-028).
- Lógica de fechamento mensal — BE-028.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| `DataIntegrityViolationException` do CHECK do banco não tratada | Baixa | Baixo | Validar antes no use case; tratar a exceção como fallback com mensagem clara |
| `FolhaController` sendo criado em BE-026 e BE-027 em paralelo | Média | Médio | Coordenar com BE-026: um cria o controller, o outro adiciona endpoints; ou cada um tem seu controller separado |

---

## Coordenação

- **Pode rodar em paralelo com:** BE-025, BE-026.
- **Depende sequencialmente de:** BE-024.
- **Bloqueia:** BE-028.
- **Atenção pro Reviewer:** verificar validação matemática no use case (não só no banco); confirmar cancelamento de quitado retorna 409 não 200.
- **Após merge:** BE-028 pode iniciar.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR pra `develop`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §3 e §5
- `docs/decisions/0016-evo09-folha-pagamento.md` §4
