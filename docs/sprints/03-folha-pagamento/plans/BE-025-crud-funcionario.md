---
task: BE-025
titulo: "CadastrarFuncionario + CRUD /api/funcionarios"
sprint: 03-folha-pagamento
data_planejamento: 2026-05-31
branch_alvo: feature/be-025-crud-funcionario
prioridade: alta
esforco: medio
territorio: back
estado: pronto-pra-execucao
depende_de: [BE-024]
bloqueia: [FE-015]
skills_dispatched: [arquitetura-hexagonal, ecossistema-spring]
---

# BE-025 — CadastrarFuncionario + CRUD /api/funcionarios

## Intake

- **Origem:** spec EVO-09 §3 (ports in) + §5 (endpoints) + §8 (BE-folha-3).
- **Por quê agora:** FE-015 (tela de funcionários) depende destes endpoints.
- **Esforço:** médio — 4 endpoints, 1 use case de cadastro, validações de dados bancários condicionais.
- **Riscos resumidos:** validação condicional PIX/TED (campos obrigatórios dependem de `forma_pagamento`) — implementar como `@AssertTrue` ou validator customizado.

---

## Contexto

Após BE-024, as entidades e ports existem. Esta task entrega a camada de use case + controller para gerenciar funcionários.

Padrão existente: ver `adapters/in/web/` para Controllers existentes e `application/usecase/` para Use Cases.

---

## Decisão / abordagem

Use cases: `CadastrarFuncionarioUseCase` (implementa `CadastrarFuncionarioPortIn`) e `AtualizarFuncionarioUseCase` (implementa `AtualizarFuncionarioPortIn`). Desativar (`DELETE`) é operação de repositório direta — pode ficar no controller via use case simples ou inlinear se for trivial.

Validação condicional:
- PIX → `chave_pix` obrigatório.
- TED → `banco`, `agencia`, `conta`, `tipo_conta` obrigatórios.
- Implementar via Bean Validation (`@AssertTrue`) ou validator customizado no DTO de entrada.

`DELETE /api/funcionarios/{id}` = desativar (soft delete: `ativo=false`). Não remove do banco.

---

## Escopo / arquivos

### Criar
- `application/port/in/CadastrarFuncionarioPortIn.java`
- `application/port/in/AtualizarFuncionarioPortIn.java`
- `application/usecase/CadastrarFuncionarioUseCase.java`
- `application/usecase/AtualizarFuncionarioUseCase.java`
- `adapters/in/web/FuncionarioController.java` — 4 endpoints (POST, GET, PUT, DELETE).
- DTOs: `FuncionarioRequest.java`, `FuncionarioResponse.java` em pacote adequado.

### Não tocar
- `FolhaController.java` — BE-026/027/028.
- Endpoints de vales/adiantamentos/fechamento — tasks seguintes.

---

## Testes

- **Unitários:** `CadastrarFuncionarioUseCase` — validação com PIX sem chave_pix (deve rejeitar), com TED sem banco (deve rejeitar), cadastro válido PIX, cadastro válido TED.
- **Integração:** `FuncionarioController` — POST cria, GET lista, PUT atualiza salário, DELETE desativa.

`testes_total` esperado: ≥ testes_existentes + 8. `testes_novos` ≥ 8.

---

## Critérios de aceitação

- [ ] `POST /api/funcionarios` cria funcionário e retorna 201 com `FuncionarioResponse`.
- [ ] `GET /api/funcionarios` lista apenas ativos (`ativo=true`).
- [ ] `PUT /api/funcionarios/{id}` atualiza `salario_base`, `conta_propria`, `obs_pagamento` (e dados bancários).
- [ ] `DELETE /api/funcionarios/{id}` seta `ativo=false` (não remove).
- [ ] POST com `forma_pagamento=PIX` e sem `chave_pix` → 400.
- [ ] POST com `forma_pagamento=TED` e sem `banco` → 400.
- [ ] `mvn test` verde com `testes_novos ≥ 8`.
- [ ] Branch: `feature/be-025-crud-funcionario`.
- [ ] Território: apenas `financas_bot_telegram/`.
- [ ] Status report com frontmatter válido.

---

## Fora de escopo

- Endpoints de vales, adiantamentos, fechamento — tasks BE-026, 027, 028.
- Tela de front-end — FE-015.
- Autenticação/autorização diferenciada — mesma auth existente do projeto.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Validação condicional PIX/TED complexa de expressar com Bean Validation | Média | Baixo | Usar `@AssertTrue` no DTO ou validator de classe; documentar padrão |
| `FuncionarioController` sem autenticação no MVP | Baixa | Baixo | Mesmo padrão de auth existente no projeto (filho é o único operador) |

---

## Coordenação

- **Pode rodar em paralelo com:** BE-026, BE-027 (após BE-024 mergear).
- **Depende sequencialmente de:** BE-024.
- **Bloqueia:** FE-015.
- **Atenção pro Reviewer:** verificar validação condicional; confirmar que DELETE é soft (ativo=false), não hard delete.
- **Após merge:** despachar FE-015.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR pra `develop`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §3 e §5
- `docs/decisions/0016-evo09-folha-pagamento.md`
