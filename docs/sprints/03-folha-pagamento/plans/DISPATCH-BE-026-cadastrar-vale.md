# DISPATCH — BE-026-cadastrar-vale (single-task)

> **Quando usar:** após BE-024 mergeada em `integration/03-folha-pagamento`. Pode rodar em paralelo com BE-025.
> ⚠️ **BE-027 NÃO deve iniciar antes desta mergear** — ambas tocam `FolhaController.java`.
> Esta task é responsável por **criar** o `FolhaController.java`; BE-027 vai apenas **adicionar** endpoints nele.

---

## Pré-condições (git)

- **`feature/be-024-entidades-jpa-repositorios-folha` mergeada em `integration/03-folha-pagamento`** — CadastrarValePortOut e as entidades precisam existir.
  Confirmar: `git log origin/integration/03-folha-pagamento --oneline | grep be-024`.
- **Decisão §7 (vales na lista do Pedro):** PENDENTE — esta task explicitamente NÃO toca `GET /api/pedidos`. Registrar a decisão que o PO tomar no status report (qual opção: A ou B).

---

## O prompt (cole tudo numa sessão `--agent backend`)

```
Task: BE-026 — CadastrarVale + endpoint de vales.

Localize e leia o plano completo (initialPrompt orienta o Glob).
Leia também:
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §3 e §5
- docs/decisions/0016-evo09-folha-pagamento.md
- Padrão de controllers existente em adapters/in/web/

## A TASK

Vale = Pedido com categoria=VALE, funcionario_id=<id>, fechado=FALSE.

Criar:
1. CadastrarValePortIn (interface em application/port/in/).
2. CadastrarValeUseCase (em application/usecase/).
3. FolhaController.java — CRIAR este controller (não existe ainda).
   Adicionar 2 endpoints:
   - POST /api/funcionarios/{id}/vales
   - GET  /api/funcionarios/{id}/vales?mes=YYYY-MM
4. DTOs: ValeRequest.java, ValeResponse.java.

## REGRAS DURAS

1. Branch: `feature/be-026-cadastrar-vale` saindo de `origin/integration/03-folha-pagamento`.
2. Território: SÓ `financas_bot_telegram/`. Zero `frontend/`.
3. ESTA TASK CRIA FolhaController.java — BE-027 vai adicionar endpoints nele depois do merge.
   Estruturar o controller para ser extensível (sem lógica hardcoded de roteamento).
4. NÃO tocar em PedidoController.java nem em GET /api/pedidos — decisão §7 é tarefa separada.
5. `status` do vale: esclarecer com PO antes de implementar.
   - PENDENTE = vale dado mas ainda "em aberto" administrativamente.
   - PAGO = vale entregue em espécie, sem necessidade de comprovante.
   Se não conseguir esclarecer: usar PENDENTE como padrão seguro e documentar no status report.
6. 1 commit: `feat(BE-026): CadastrarVale + endpoints de vales`.
7. NÃO mergeie. PR pra `integration/03-folha-pagamento` após status report + Reviewer.

## VERIFICAÇÃO DE ESCOPO

Antes de implementar, confirmar que FolhaController.java NÃO existe ainda:
  ls financas_bot_telegram/src/main/java/.../adapters/in/web/FolhaController.java
Se existir (de outra task em paralelo), PARE e use AskUserQuestion para coordenar.

## TESTES

- Unitários (mínimo 3): CadastrarValeUseCase
  1. Funcionário ativo → cria vale (ok).
  2. Funcionário inativo → rejeita (400).
  3. Valor negativo/zero → rejeita (400).
- Integração (mínimo 2): POST cria vale; GET lista por mês (filtra por mes=YYYY-MM).

`testes_novos` ≥ 5.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/BE-026-cadastrar-vale.md`

Incluir obrigatoriamente:
- Decisão sobre `status` do vale (PENDENTE ou PAGO) e como foi esclarecida com PO.
- Decisão §7 (qual opção o PO escolheu, ou que está pendente).
- Confirmação que GET /api/pedidos NÃO foi tocado.

## SE QUEBRAR

Cenário — FolhaController já existe (outra task criou em paralelo):
  PARE. Use AskUserQuestion: "FolhaController.java já existe — coordenar com planner antes de prosseguir."

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano

- **Pode rodar em paralelo com BE-025** (FuncionarioController ≠ FolhaController).
- ⚠️ **BE-027 deve iniciar SOMENTE APÓS esta mergear** — não despachar BE-027 enquanto esta PR estiver aberta.
- **Estimativa:** 1h. Estrutura análoga ao CRUD padrão do projeto.
- **Após merge desta:** despachar BE-027 imediatamente.

## Referências

- `docs/sprints/03-folha-pagamento/plans/BE-026-cadastrar-vale.md`
- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §3, §5, §7
