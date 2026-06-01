---
task: FE-017
titulo: "Modal fechamento com cálculo em tempo real"
sprint: 03-folha-pagamento
data_planejamento: 2026-05-31
branch_alvo: feature/fe-017-modal-fechamento
prioridade: alta
esforco: medio
territorio: front
estado: pronto-pra-execucao
depende_de: [BE-028]
bloqueia: []
skills_dispatched: [boas-praticas-react, ecossistema-frontend]
---

# FE-017 — Modal fechamento com cálculo em tempo real

## Intake

- **Origem:** spec EVO-09 §6 (Modal: Fechar Mês).
- **Por quê agora:** último passo do fluxo de fechamento — o filho confirma o mês antes de gerar o Pedido FOLHA.
- **Esforço:** médio — cálculo em tempo real no front (sem roundtrip), preview do breakdown, confirmação com loading state.
- **Riscos resumidos:** cálculo em tempo real requer dados de vales e adiantamentos já carregados na tela (vêm de FE-016). Se o valor final for negativo, exibir aviso visual.

---

## Contexto

Este modal é invocado a partir de FE-016 (Tela Folha do Funcionário) via botão "Fechar mês YYYY-MM". Os dados necessários para o preview (salário base, vales do mês, adiantamentos ativos) já estão carregados em FE-016 — o modal recebe via props.

Endpoint utilizado: `POST /api/funcionarios/{id}/fechamentos` com body `{ "mes": "2026-05", "ajuste": 0.00 }`.

---

## Decisão / abordagem

**Cálculo em tempo real no front:** o modal recebe `salarioBase`, `totalVales`, `listaAdiantamentosAtivos` como props de FE-016. Calcula localmente `totalParcelas = soma(valorParcela)` e `valorFinal = salarioBase - totalVales - totalParcelas + ajuste`. Sem roundtrip pra calcular preview — só o POST final vai ao backend.

Campo `ajuste` (BigDecimal): input numérico, default `0.00`. Pode ser positivo (bonificação) ou negativo (desconto extra). O cálculo atualiza em tempo real conforme o usuário digita.

**Aviso de valor negativo:** se `valorFinal < 0`, exibir badge vermelho de alerta ("Valor negativo — verificar vales e adiantamentos").

**Confirmação:** botão "Confirmar fechamento" faz POST; ao sucesso exibe toast de confirmação e fecha o modal; FE-016 invalida o cache (refetch da lista de fechamentos e dos vales).

---

## Escopo / arquivos

### Criar
- `src/components/folha/ModalFechamento.tsx` — modal com preview + confirmação.

### Modificar
- `src/paginas/folha/FolhaFuncionarioPage.tsx` (FE-016) — integrar abertura do modal + invalidação de cache após confirmação.
- `src/api/folha.ts` — adicionar `fecharMes(funcionarioId, mes, ajuste)`.

---

## Testes

- **Componente:** `ModalFechamento` renderiza preview correto com dados mockados; campo ajuste atualiza cálculo; aviso vermelho aparece para valor negativo; botão desabilitado durante loading.
- **Integração leve:** mock do POST, verificar toast de sucesso.

`testes_total` esperado: ≥ testes_existentes + 5. `testes_novos` ≥ 5.

---

## Critérios de aceitação

- [ ] Modal exibe: salário base, total vales (com count), total parcelas (com count), ajuste (editável), valor final calculado em tempo real.
- [ ] Campo ajuste (default 0.00) atualiza `valorFinal` imediatamente ao digitar.
- [ ] `valorFinal < 0` → badge vermelho de alerta.
- [ ] Botão "Confirmar fechamento" faz POST e exibe loading state.
- [ ] Sucesso: toast de confirmação + modal fecha + FE-016 atualiza (refetch vales + fechamentos).
- [ ] Erro 409 (já fechado): mensagem clara "Mês já fechado".
- [ ] `npm test` verde com `testes_novos ≥ 5`.
- [ ] Branch: `feature/fe-017-modal-fechamento`.
- [ ] Status report com frontmatter válido.

---

## Fora de escopo

- Reabertura de mês fechado — não está no MVP.
- Cálculo server-side do preview — o backend só calcula no momento do POST.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Dados de adiantamentos desatualizados no momento do fechar | Baixa | Baixo | FE-016 carrega adiantamentos com `staleTime` curto; informar no tooltip que o preview é baseado nos dados carregados |
| Erro 409 não tratado visualmente | Baixa | Médio | Mapear `ApiError.codigo=409` para mensagem específica |

---

## Coordenação

- **Pode rodar em paralelo com:** BE-029, FE-016.
- **Depende sequencialmente de:** BE-028 (endpoint de fechamento).
- **Bloqueia:** nada (última task de front nesta sprint).
- **Atenção pro Reviewer:** verificar que o cálculo local bate com o que o backend retorna no POST (observação do Pedido FOLHA); confirmar tratamento de 409.
- **Após merge:** sprint 03 de front está completa.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR pra `develop`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §6 (Modal: Fechar Mês)
- `docs/decisions/0016-evo09-folha-pagamento.md`
