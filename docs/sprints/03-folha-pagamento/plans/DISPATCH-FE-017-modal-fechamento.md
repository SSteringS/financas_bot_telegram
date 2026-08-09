# DISPATCH — FE-017-modal-fechamento (single-task)

> **Quando usar:** após FE-016 mergeada em `integration/03-folha-pagamento`.
> FE-017 modifica `FolhaFuncionarioPage.tsx` (criado em FE-016) e substitui o stub de ModalFechamento.
> Também pode iniciar após BE-028 (endpoint de fechamento) para validar o POST real.

---

## Pré-condições (git)

- **`feature/fe-016-tela-folha-funcionario` mergeada em `integration/03-folha-pagamento`** — FolhaFuncionarioPage.tsx e o stub do ModalFechamento precisam existir.
  Confirmar: `git log origin/integration/03-folha-pagamento --oneline | grep fe-016`.
- BE-028 já deve estar em integration também (para testar o POST real de fechamento).

---

## O prompt (cole tudo numa sessão `--agent frontend`)

```
Task: FE-017 — Modal fechamento com cálculo em tempo real.

Localize e leia o plano completo (initialPrompt orienta o Glob).
Leia também:
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §6 (Modal: Fechar Mês)
- docs/decisions/0016-evo09-folha-pagamento.md
- src/paginas/folha/FolhaFuncionarioPage.tsx (FE-016 — onde o modal é invocado)
- src/api/folha.ts (FE-015/016 — adicionar fecharMes() aqui)
- O stub atual do ModalFechamento (se existir de FE-016) — substituir pela implementação real

## A TASK

Modal invocado pelo botão "Fechar mês YYYY-MM" em FolhaFuncionarioPage (FE-016).
Recebe dados via props (sem roundtrip para preview), calcula em tempo real no front.

### Criar:
1. src/components/folha/ModalFechamento.tsx — modal com:
   - Preview: salário base, total vales (count + valor), total parcelas (count + valor), ajuste (editável, default 0.00), valor final calculado.
   - Cálculo em tempo real: valorFinal = salarioBase - totalVales - totalParcelas + ajuste (atualiza ao digitar no campo ajuste).
   - Badge vermelho se valorFinal < 0 ("Valor negativo — verificar vales e adiantamentos").
   - Botão "Confirmar fechamento" → POST /api/funcionarios/{id}/fechamentos com {mes, ajuste}.
   - Loading state no botão durante o POST.
   - Sucesso: toast de confirmação + fechar modal + FE-016 invalida cache (refetch vales + fechamentos).
   - Erro 409: mensagem clara "Mês já fechado".

### Estender:
2. src/api/folha.ts — adicionar fecharMes(funcionarioId, mes, ajuste): Promise<PedidoFolhaResponse>.

### Modificar:
3. src/paginas/folha/FolhaFuncionarioPage.tsx — substituir stub por ModalFechamento real + lógica de invalidação de cache após confirmação.

## Props do ModalFechamento (contrato com FE-016):
```typescript
interface ModalFechamentoProps {
  funcionarioId: number;
  mes: string;            // "YYYY-MM"
  salarioBase: number;
  vales: Vale[];          // lista completa do mês selecionado
  adiantamentosAtivos: Adiantamento[];  // lista atual
  onConfirm: () => void;  // callback para refetch em FE-016
  onClose: () => void;
}
```
Calcular internamente: totalVales = soma(vales.valor), totalParcelas = soma(adiantamentos.valorParcela).

## REGRAS DURAS

1. Branch: `feature/fe-017-modal-fechamento` saindo de `origin/integration/03-folha-pagamento`
   (develop já tem FE-016 com FolhaFuncionarioPage.tsx e o stub).
2. Território: SÓ `frontend/`. Zero `financas_bot_telegram/`.
3. NUNCA a partir de outra feature branch — sempre de origin/integration/03-folha-pagamento (ver CLAUDE.md §Fluxo de branches).
4. Cálculo de preview: ZERO roundtrips — tudo local com os dados recebidos via props.
5. Erro 409: mapear para mensagem "Mês já fechado" — não exibir stack trace.
6. 1 commit: `feat(FE-017): ModalFechamento com calculo em tempo real`.
7. NÃO mergeie. PR pra `integration/03-folha-pagamento` após status report + Reviewer.

## VERIFICAÇÃO ANTES DE CODAR

Confirmar o shape do response do POST /api/funcionarios/{id}/fechamentos:
  curl -s https://api.satyansaita.com/v3/api-docs | jq '.paths["/api/funcionarios/{id}/fechamentos"]["post"]'
OU ler PedidoFolhaResponse.java em develop (criado em BE-028).

Confirmar se o stub de ModalFechamento existe em FolhaFuncionarioPage.tsx:
  grep -n "ModalFechamento" frontend/src/paginas/folha/FolhaFuncionarioPage.tsx

## TESTES

- Componente (mínimo 4):
  ModalFechamento: preview correto com dados mockados (salário - vales - parcelas + ajuste); campo ajuste atualiza cálculo em tempo real; badge vermelho aparece quando valorFinal < 0; botão desabilitado durante loading.
- Integração leve (mínimo 1):
  Mock do POST, verificar toast de sucesso + modal fecha + onConfirm chamado.

`testes_novos` ≥ 5.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/FE-017-modal-fechamento.md`

Incluir:
- Confirmação que o cálculo de preview é 100% local (sem chamada de API no preview).
- Resultado do teste de 409 (como ficou a mensagem de erro).
- Verificar que o cálculo do front bate com o breakdown na observação do Pedido FOLHA retornado pelo back (comparar valores).

## SE QUEBRAR

Cenário — FolhaFuncionarioPage.tsx não existe em develop:
  FE-016 não foi mergeada ainda. Verificar pré-condição antes de continuar.

Cenário — POST retorna 409 mas o tratamento de erro não mapeia corretamente:
  Verificar como os outros componentes tratam erros HTTP no projeto. Seguir o mesmo padrão.

Cenário — cálculo do front diverge do back (observação do Pedido FOLHA):
  Verificar a spec §4 passo 6 para a fórmula exata. Se a divergência for de centavos (arredondamento):
  ajustar para usar Math.round() ou BigDecimal equivalente no front. Anotar no status report.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano

- **Última task de front da sprint 03.** Após merge: sprint 03 de front completa.
- **Pode rodar em paralelo com BE-029** (back e front são disjuntos).
- **Estimativa:** 1-1.5h. Modal com cálculo em tempo real é a parte mais trabalhosa; o resto é integração com FE-016.
- **Atenção do Reviewer:** verificar que o cálculo local do front bate com o breakdown na observação gerada pelo back (BE-028 passo 7). Divergência de arredondamento é aceitável (front usa float, back usa BigDecimal); divergência lógica não é.

## Referências

- `docs/sprints/03-folha-pagamento/plans/FE-017-modal-fechamento.md`
- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §6 (Modal: Fechar Mês)
- `docs/decisions/0016-evo09-folha-pagamento.md`
