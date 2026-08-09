# DISPATCH — BE-029-testes-fechar-mes (single-task)

> **Quando usar:** após BE-028 mergeada em `integration/03-folha-pagamento` e o Reviewer ter aprovado a implementação.
> O objetivo desta task é escrever testes que validam a implementação do FecharMesUseCase — não mudar o use case.
> Por isso ela vem DEPOIS da revisão: evita que testes validem comportamento errado sem contestar.

---

## Pré-condições (git)

- **`feature/be-028-fechar-mes` mergeada em `integration/03-folha-pagamento`** — FecharMesUseCase, FechamentoDuplicadoException e o endpoint precisam existir.
  Confirmar: `git log origin/integration/03-folha-pagamento --oneline | grep be-028`.

---

## O prompt (cole tudo numa sessão `--agent backend`)

```
Task: BE-029 — Testes FecharMesUseCase (unitário) + integração endpoint fechamento.

Localize e leia o plano completo (initialPrompt orienta o Glob).
Leia também:
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §4 (algoritmo — base dos cenários)
- docs/decisions/0016-evo09-folha-pagamento.md §3 (idempotência + race condition)
- docs/runbooks/ROTEIRO-TESTES-BACKEND.md
- FecharMesUseCase.java (implementação de BE-028 — já em develop)
- FecharMesEndpoint (FolhaController) — ver quais endpoints existem

## A TASK

Escrever APENAS testes — não alterar a implementação do use case.
Se encontrar bug na implementação durante os testes: documentar no status report e usar AskUserQuestion para reportar ao humano. NÃO consertar o use case nesta task.

### Criar:
1. FecharMesUseCaseTest.java (unitário, em src/test/.../usecase/) — 8 cenários com Mockito.
2. FecharMesEndpointIT.java (integração, em src/test/.../integration/) — 2+ cenários com @SpringBootTest.

### Os 8 cenários unitários (obrigatórios):
1. Fechamento padrão: 3 vales + 2 adiantamentos + ajuste=0 → valorFinal correto + texto observacao preenchido + vales fechados + parcelas incrementadas.
2. Adiantamento quitado (última parcela): ativo=false após fechamento.
3. Sem vales nem adiantamentos: valorFinal = salarioBase.
4. Ajuste positivo (bonificação): valorFinal = salarioBase + ajuste.
5. Ajuste negativo (desconto extra): valorFinal = salarioBase - |ajuste|.
6. Valor final negativo (vales > salário): aceita e registra (domínio permite — não rejeitar).
7. Fechamento duplicado (existsFolha=true): lança FechamentoDuplicadoException.
8. Funcionário inativo: lança FuncionarioNaoEncontradoException.

### Os cenários de integração (mínimo 2):
1. Fluxo completo: criar funcionário + vales + adiantamento via API → POST fechamento → verificar Pedido FOLHA criado, vales com fechado=true, parcelas_pagas incrementadas.
2. Idempotência: fechar mesmo mês duas vezes → segundo retorna 409.

## REGRAS DURAS

1. Branch: `feature/be-029-testes-fechar-mes` saindo de `origin/integration/03-folha-pagamento`.
2. Território: SÓ `financas_bot_telegram/src/test/`. Zero `src/main/`. Zero `frontend/`.
3. NÃO alterar FecharMesUseCase.java nem FolhaController.java — só escrever testes.
4. Integração deve usar banco real (Testcontainers MySQL OU H2 com compatibilidade MySQL 8).
   Se H2: verificar que CHECK constraints da V6 estão desabilitadas ou emuladas no perfil de teste.
5. 1 commit: `test(BE-029): cobertura FecharMesUseCase — unitario + integracao endpoint`.
6. NÃO mergeie. PR pra `integration/03-folha-pagamento` após status report + Reviewer.

## TESTES

`testes_novos` ≥ 11 (8 unitários + 2+ integração + 1 reserva pra race condition se conseguir simular).

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/BE-029-testes-fechar-mes.md`

Incluir:
- Lista dos cenários implementados (checklist dos 8 unitários + cenários de integração).
- Decisão sobre H2 vs Testcontainers (qual usou e por quê).
- Se encontrou bug na implementação durante os testes: descrever o bug e marcar `estado: parcial`.

## SE QUEBRAR

Cenário — H2 rejeita CHECK constraints do V6 SQL:
  Opção 1: usar Testcontainers MySQL (mais fiel, mais lento).
  Opção 2: criar application-test.properties com `spring.jpa.properties.javax.persistence.validation.mode=none`
    e `spring.datasource.url=jdbc:h2:mem:...;MODE=MySQL` para emular.
  Documentar qual opção foi usada.

Cenário — bug encontrado em FecharMesUseCase:
  NÃO corrigir. Registrar no status report com `estado: parcial` e descrição precisa do bug.
  Usar AskUserQuestion para reportar ao humano.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano

- **Pode rodar em paralelo com FE-016 e FE-017** (back e front são disjuntos).
- **Última task de back da sprint 03.** Após merge: sprint 03 back completa.
- **Estimativa:** 1.5-2h. Cenários ricos, mas a implementação já existe — foco é cobertura.
- **Bug encontrado?** O agente vai parar e reportar. Você decide se cria um FIX separado ou se manda o agente BE-028 corrigir e re-revisar antes de BE-029 mergear.

## Referências

- `docs/sprints/03-folha-pagamento/plans/BE-029-testes-fechar-mes.md`
- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §4
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md`
