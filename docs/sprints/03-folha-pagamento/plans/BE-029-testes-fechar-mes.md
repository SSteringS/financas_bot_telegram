---
task: BE-029
titulo: "Testes — FecharMesUseCase (unitário) + integração endpoint fechamento"
sprint: 03-folha-pagamento
data_planejamento: 2026-05-31
branch_alvo: feature/be-029-testes-fechar-mes
prioridade: alta
esforco: medio
territorio: back
estado: pronto-pra-execucao
depende_de: [BE-028]
bloqueia: []
skills_dispatched: [qualidade-de-testes, arquitetura-hexagonal]
fluxos_qa: []
---

# BE-029 — Testes — FecharMesUseCase + integração endpoint fechamento

## Intake

- **Origem:** spec EVO-09 §8 (BE-folha-7). Separada de BE-028 para que a implementação seja revisada antes de escrever os testes — evita que testes validem comportamento errado.
- **Por quê agora:** cobertura de testes é critério de aceite da sprint; FecharMes é a operação mais crítica.
- **Esforço:** médio — cenários de teste são ricos (idempotência, adiantamentos, valor negativo, race condition).
- **Riscos resumidos:** mock dos repositórios pode esconder bug de query; recomendar pelo menos 1 teste de integração com banco real (H2 ou Testcontainers).

---

## Contexto

BE-028 entregou `FecharMesUseCase` com os 11 passos. Esta task cobre:
- Testes **unitários** do use case (mockar ports, verificar comportamento lógico).
- Testes de **integração** do endpoint `POST /api/funcionarios/{id}/fechamentos`.

---

## Decisão / abordagem

**Testes unitários (Mockito):** instanciar `FecharMesUseCase` com mocks dos ports, exercitar os cenários.

**Testes de integração:** usar `@SpringBootTest` + Testcontainers MySQL (ou H2 com modo MySQL) + `MockMvc`. Verificar que:
- A transação reverte se uma etapa falhar.
- O UNIQUE INDEX barra o segundo fechamento em concorrência simulada.

---

## Escopo / arquivos

### Criar
- `FecharMesUseCaseTest.java` (unitário, em `src/test/.../usecase/`).
- `FecharMesEndpointIT.java` (integração, em `src/test/.../integration/`).

### Não tocar
- Implementação do use case — BE-028.
- Outros use cases — sem escopo aqui.

---

## Testes

### Cenários unitários (mínimo)

1. **Fechamento padrão:** 3 vales, 2 adiantamentos, ajuste zero → valor correto, texto de observação preenchido, vales marcados como fechados, parcelas incrementadas.
2. **Adiantamento quitado:** última parcela → `ativo=false`.
3. **Sem vales nem adiantamentos:** salário base = valor final.
4. **Ajuste positivo (bonificação):** valor final = salário + ajuste.
5. **Ajuste negativo (desconto extra):** valor final = salário - |ajuste|.
6. **Valor final negativo (vales > salário):** aceita e registra (domínio permite).
7. **Fechamento duplicado (check no use case):** `existsFolha=true` → `FechamentoDuplicadoException`.
8. **Funcionário inativo:** `findAtivoById` não encontra → `FuncionarioNaoEncontradoException`.

### Cenários de integração (mínimo)

1. **Fluxo completo:** criar funcionário + vales + adiantamento → fechar mês → verificar Pedido FOLHA criado, vales fechados, parcelas incrementadas.
2. **Idempotência:** fechar o mesmo mês duas vezes → segundo retorna 409.
3. **Race condition simulada:** testar que UNIQUE INDEX barra concurrent insert (opcional se difícil de simular com H2).

`testes_total` esperado: ≥ testes_existentes + 11. `testes_novos` ≥ 11.

---

## Critérios de aceitação

- [ ] 8 cenários unitários de `FecharMesUseCase` verdes.
- [ ] 2+ cenários de integração do endpoint verdes.
- [ ] Cenário de fechamento duplicado retorna 409 no teste de integração.
- [ ] `mvn test` verde com `testes_novos ≥ 11`.
- [ ] Branch: `feature/be-029-testes-fechar-mes`.
- [ ] Status report com frontmatter válido.

---

## Fora de escopo

- Testes de CRUD de funcionários/vales/adiantamentos — cobertos em BE-025/026/027.
- Testes de front-end — FE-016/017.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| H2 não suporta CHECK constraints MySQL | Média | Médio | Usar Testcontainers MySQL, ou desabilitar CHECKs no perfil de teste H2 |
| Mock de repositório esconde bug de query real | Média | Médio | Pelo menos 1 teste de integração com banco real |

---

## Coordenação

- **Pode rodar em paralelo com:** FE-016, FE-017.
- **Depende sequencialmente de:** BE-028.
- **Bloqueia:** nada (última task de back nesta sprint).
- **Atenção pro Reviewer:** verificar cobertura dos 8 cenários unitários; confirmar que integração usa banco real (não só mocks).
- **Após merge:** sprint 03 está completa do lado back.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR pra `develop`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §4 (algoritmo — base dos cenários)
- `docs/decisions/0016-evo09-folha-pagamento.md` §3 (idempotência + race condition)
- `docs/runbooks/ROTEIRO-TESTES-BACKEND.md`
