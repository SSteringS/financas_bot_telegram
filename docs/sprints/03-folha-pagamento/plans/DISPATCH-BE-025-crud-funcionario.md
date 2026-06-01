# DISPATCH — BE-025-crud-funcionario (single-task)

> **Quando usar:** após o PR da BE-024 ser mergeado em `develop`. Pode rodar em paralelo com BE-026.

---

## Pré-condições (git)

- **`feature/be-024-entidades-jpa-repositorios-folha` mergeada em `develop`** — entidades JPA, ports e adapters de Funcionario precisam existir antes de compilar o use case desta task.
  Confirmar: `git log origin/develop --oneline | grep be-024`.

---

## O prompt (cole tudo numa sessão `--agent backend`)

```
Task: BE-025 — CadastrarFuncionario + CRUD /api/funcionarios.

Localize e leia o plano completo (initialPrompt orienta o Glob).
Leia também:
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §3 e §5
- docs/decisions/0016-evo09-folha-pagamento.md
- Padrão de controllers existente: adapters/in/web/ (escolher um controller existente pra seguir o padrão)
- Padrão de use cases: application/usecase/

## A TASK

Entregar a camada de use case + controller para CRUD de funcionários.

Criar:
1. CadastrarFuncionarioPortIn + AtualizarFuncionarioPortIn (interfaces em application/port/in/).
2. CadastrarFuncionarioUseCase + AtualizarFuncionarioUseCase (em application/usecase/).
3. FuncionarioController com 4 endpoints:
   - POST   /api/funcionarios       → criar
   - GET    /api/funcionarios       → listar ativos
   - PUT    /api/funcionarios/{id}  → atualizar
   - DELETE /api/funcionarios/{id}  → desativar (ativo=false, soft delete)
4. DTOs: FuncionarioRequest.java, FuncionarioResponse.java.

## REGRAS DURAS

1. Branch: `feature/be-025-crud-funcionario` saindo de `origin/develop`.
2. Território: SÓ `financas_bot_telegram/`. Zero `frontend/`.
3. Validação condicional PIX/TED: implementar no DTO (Bean Validation @AssertTrue na classe)
   OU validator customizado. Documentar a escolha no status report.
4. DELETE é soft (ativo=false). NÃO remover do banco.
5. 1 commit: `feat(BE-025): CadastrarFuncionario + CRUD /api/funcionarios`.
6. NÃO mergeie. PR pra develop após status report + Reviewer.

## DECISÃO A TOMAR

**Validação condicional PIX/TED:** @AssertTrue no DTO ou validator de classe (`implements Validator`)?
Documentar a escolha no status report com justificativa.
Ambas as opções são aceitáveis — o critério é que a mensagem de erro 400 seja clara pra o front.

## TESTES

- Unitários (mínimo 4): CadastrarFuncionarioUseCase
  1. PIX sem chave_pix → rejeita (400).
  2. TED sem banco → rejeita (400).
  3. PIX válido → cria funcionário.
  4. TED válido → cria funcionário.
- Integração (mínimo 4): FuncionarioController
  POST, GET (lista apenas ativos), PUT (atualiza salário), DELETE (ativo=false).

`testes_novos` ≥ 8.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/BE-025-crud-funcionario.md`

Incluir:
- Decisão sobre validação condicional (abordagem + justificativa).
- Seção `## Padrões e decisões técnicas` com SOLID aplicado (especialmente SRP entre use case e controller).

## SE QUEBRAR

Cenário — compilação falha por `FuncionarioRepositoryPortOut` não encontrado:
  BE-024 não foi mergeada ainda em develop. Confirmar pré-condição antes de continuar.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano

- **Pode rodar em paralelo com BE-026** (controladores e use cases diferentes; territories disjuntos dentro do back).
- **Estimativa:** 1-2h.
- **Após merge:** despachar FE-015 (único bloqueio para a tela de funcionários).
  ⚠️ Não despachar BE-027 antes de BE-026 mergear — ver DISPATCH-BE-026 e DISPATCH-BE-027.

## Referências

- `docs/sprints/03-folha-pagamento/plans/BE-025-crud-funcionario.md`
- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §3, §5
