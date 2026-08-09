# DISPATCH OVERNIGHT — Frontend Sprint 03 (FE-015 → FE-016 → FE-017)

> **Sessão autônoma.** Execute as 3 tasks do frontend em sequência sem interrupção.
> Após cada task: cria PR, mergeia em `integration/03-folha-pagamento`, parte para a próxima.
>
> **Pré-condição para iniciar a sessão:** BE-025 deve estar mergeada em integration.
> Se não estiver ainda, aguardar com o loop de verificação abaixo antes de começar FE-015.
>
> **Revisor:** gate de revisão independente está DIFERIDO para o humano de manhã.
> O que substitui: `npm test` + `npm run build` DEVEM estar verdes antes de mergear cada task.
>
> **Em caso de falha irrecuperável:** parar na task atual, documentar no status report,
> NÃO continuar para a próxima. Deixar o estado claro para o humano encontrar ao acordar.

---

## PROTOCOLO — repetir para cada task

```
1. git fetch origin
2. Verificar pré-condição da task (ver seção específica abaixo)
   Se a pré-condição não estiver satisfeita: aguardar com loop (ver abaixo)
3. git checkout -b feature/<id>-<slug> origin/integration/03-folha-pagamento
4. Ler: docs/sprints/03-folha-pagamento/plans/<TASK-ID>-<slug>.md  (plano completo)
5. Implementar conforme o plano
6. npm run build && npm test   ← AMBOS devem ser verdes. Se falhar: PARE (ver Regra de Parada)
7. Escrever status report: docs/sprints/03-folha-pagamento/status/<TASK-ID>-<slug>.md
8. git add <arquivos do território frontend/> docs/sprints/03-folha-pagamento/status/<TASK-ID>-*.md
9. git commit -m "<mensagem definida no plano>"
10. gh pr create \
      --base integration/03-folha-pagamento \
      --head feature/<id>-<slug> \
      --title "<TASK-ID>: <título>" \
      --body "Overnight autônomo. Status report em docs/sprints/03-folha-pagamento/status/."
11. gh pr merge --squash --delete-branch
12. git fetch origin
13. git log origin/integration/03-folha-pagamento --oneline | head -5  ← confirmar merge
14. → PRÓXIMA TASK
```

### Loop de espera de pré-condição (usar quando BE ainda não mergeou)

```bash
# Exemplo: aguardar be-028 aparecer em integration
while true; do
  git fetch origin
  if git log origin/integration/03-folha-pagamento --oneline | grep -q "be-028"; then
    echo "BE-028 detectada em integration. Iniciando task."
    break
  fi
  echo "BE-028 ainda não mergeada. Aguardando 10 minutos..."
  sleep 600
done
```
Substituir `"be-028"` pela task esperada. Timeout implícito: se after 3h a pré-condição não
aparecer, registrar no status report e parar.

---

## REGRA DE PARADA

Parar imediatamente (sem continuar para a próxima task) se:
- `npm run build` falhar com erro TypeScript não trivial
- `npm test` falhar em testes existentes (não novos testes que você está criando)
- Uma pré-condição não está satisfeita após 3h de espera
- O shape de um endpoint retorna diferente do esperado E a adaptação não é óbvia
- Surge decisão de UX que o plano não previu e não tem resposta clara

→ Escrever o status report com `estado: bloqueado`, descrever o problema com precisão.
→ **NÃO** continuar para a próxima task.

---

## TASK 1 — FE-015: Tela Funcionários

**Pré-condição:** BE-025 mergeada em integration (endpoints /api/funcionarios disponíveis).
**Verificar (com espera se necessário):**
```bash
# Loop de espera
while true; do
  git fetch origin
  if git log origin/integration/03-folha-pagamento --oneline | grep -q "be-025"; then
    echo "BE-025 detectada. Iniciando FE-015."
    break
  fi
  echo "BE-025 ainda não mergeada. Aguardando 10 minutos..."
  sleep 600
done
```

**Prompt para a implementação:**

```
Task: FE-015 — Tela Funcionários — lista + formulário de cadastro/edição.

Leia o plano completo:
  docs/sprints/03-folha-pagamento/plans/FE-015-tela-funcionarios.md
Leia também:
  docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §6 (Tela: Lista de Funcionários)
  docs/decisions/0016-evo09-folha-pagamento.md
  Padrão: src/paginas/Home.tsx, src/api/pedidos.ts, src/hooks/usePedidos.ts

## A TASK

Criar a nova aba "Folha de pagamento" com lista de funcionários e formulário.

Criar:
1. src/paginas/folha/FuncionariosPage.tsx — tabela de funcionários ativos.
2. src/components/folha/FuncionarioForm.tsx — formulário com campos condicionais PIX/TED.
3. src/api/folha.ts — listarFuncionarios(), criarFuncionario(), atualizarFuncionario(), desativarFuncionario().
   ⚠️ FE-016 vai ESTENDER este arquivo — criar já pensando em extensão.
4. src/types/folha.ts — Funcionario, FormaPagamento (enum), FuncionarioRequest.
   ⚠️ FE-016 vai ESTENDER este arquivo — criar já pensando em extensão.

Modificar:
5. src/App.tsx — adicionar rota /folha e /folha/funcionarios/:id (segunda apontará para FE-016).
6. Navegação/menu — adicionar aba "Folha de pagamento".

## VERIFICAÇÃO ANTES DE CODAR

Confirmar contrato real da API (BE-025 já em integration):
  grep -r "FuncionarioController\|/api/funcionarios" \
    financas_bot_telegram/src/main/java/ | head -20
OU usar Swagger se disponível: curl -s http://localhost:8080/v3/api-docs | jq '.paths | keys[]' | grep funcionario

## REGRAS

1. Branch: feature/fe-015-tela-funcionarios de origin/integration/03-folha-pagamento
2. Território: SÓ frontend/. Zero financas_bot_telegram/.
3. Badge laranja para conta_propria=false.
4. Campos condicionais: PIX selecionado → só chave_pix; TED → banco/agência/conta/tipo_conta.
   Usar watch('forma_pagamento') do react-hook-form.
5. Clique na linha → navega para /folha/funcionarios/{id} (link pode ser vazio por ora).
6. 1 commit: feat(FE-015): tela funcionarios — lista + formulario cadastro/edicao
7. npm run build && npm test verdes antes de abrir PR.
8. testes_novos >= 6
9. Status report: docs/sprints/03-folha-pagamento/status/FE-015-tela-funcionarios.md
   Incluir: shape real do response BE-025 + decisão sobre campos condicionais.
10. Após status report: criar PR para integration/03-folha-pagamento e mergear.
```

**Mensagem de commit:** `feat(FE-015): tela funcionarios — lista + formulario cadastro/edicao`

---

## TASK 2 — FE-016: Tela Folha do Funcionário

**Pré-condição dupla:** FE-015 E BE-028 ambas mergeadas em integration.
**Verificar (com espera se necessário):**
```bash
# Loop — espera FE-015 e BE-028
while true; do
  git fetch origin
  LOG=$(git log origin/integration/03-folha-pagamento --oneline)
  FE015=$(echo "$LOG" | grep -c "fe-015")
  BE028=$(echo "$LOG" | grep -c "be-028")
  if [ "$FE015" -gt 0 ] && [ "$BE028" -gt 0 ]; then
    echo "FE-015 e BE-028 detectadas. Iniciando FE-016."
    break
  fi
  echo "Aguardando: FE-015=$FE015 BE-028=$BE028. Próxima verificação em 10 min..."
  sleep 600
done
```

**Prompt para a implementação:**

```
Task: FE-016 — Tela Folha do Funcionário — vales, adiantamentos, fechamentos.

Leia o plano completo:
  docs/sprints/03-folha-pagamento/plans/FE-016-tela-folha-funcionario.md
Leia também:
  docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §6 (Tela: Folha do Funcionário)
  docs/decisions/0016-evo09-folha-pagamento.md
  src/types/folha.ts (criado em FE-015 — ESTENDER com novos types)
  src/api/folha.ts (criado em FE-015 — ESTENDER com novas funções)
  src/paginas/folha/FuncionariosPage.tsx (padrão visual a seguir)

## DECISÃO §7 JÁ RESOLVIDA

Opção A (PO, 2026-06-04): vales NÃO aparecem na lista do Pedro.
NÃO tocar src/paginas/Home.tsx — esta tela não é afetada.

## A TASK

Rota: /folha/funcionarios/{id}. Tela de operação do filho (Pedro pai não acessa).

Criar:
1. src/paginas/folha/FolhaFuncionarioPage.tsx — tela principal com 3 seções + seletor de mês.
2. src/components/folha/ValesSection.tsx — vales do mês com seletor de mês (últimos 6 meses).
3. src/components/folha/AdiantamentosSection.tsx — adiantamentos ativos com parcelas.
4. src/components/folha/FechamentosSection.tsx — accordion de fechamentos anteriores.
5. src/components/folha/ValeForm.tsx — form de registro de vale (inline ou modal).
6. src/components/folha/AdiantamentoForm.tsx — form de novo adiantamento.
7. src/hooks/folha/useFolhaFuncionario.ts — hook agregador das 3 queries (TanStack Query).

Estender (arquivos de FE-015):
8. src/api/folha.ts — listarVales(), criarVale(), listarAdiantamentos(), criarAdiantamento(),
   cancelarAdiantamento(), listarFechamentos().
9. src/types/folha.ts — Vale, ValeRequest, Adiantamento, AdiantamentoRequest, Fechamento.

Modificar:
10. src/App.tsx — confirmar/adicionar rota /folha/funcionarios/:id.

## REGRAS IMPORTANTES

- Botão "Fechar mês YYYY-MM": aparecer SOMENTE se não existe Pedido FOLHA com mes_referencia
  do mês selecionado. Implementar via dados já carregados (sem roundtrip extra).
- Vales com fechado=true: exibir em cinza/tachado.
- Cancelar adiantamento: confirmação antes de DELETE.
- Clique no botão fechar mês: abrir ModalFechamento.
  FE-017 ainda não existe → criar STUB: componente ModalFechamento que recebe as props
  corretas (ver abaixo) mas exibe apenas "Em construção".
  FE-017 vai substituir o stub.

Props do stub ModalFechamento (EXATAS — FE-017 vai implementar essas mesmas props):
  interface ModalFechamentoProps {
    funcionarioId: number;
    mes: string;              // "YYYY-MM"
    salarioBase: number;
    vales: Vale[];            // lista completa do mês selecionado
    adiantamentosAtivos: Adiantamento[];
    onConfirm: () => void;    // callback para refetch em FE-016
    onClose: () => void;
  }

## REGRAS

1. Branch: feature/fe-016-tela-folha-funcionario de origin/integration/03-folha-pagamento
2. Território: SÓ frontend/. Zero financas_bot_telegram/.
3. NÃO tocar src/paginas/Home.tsx.
4. 1 commit: feat(FE-016): tela folha funcionario — vales, adiantamentos, fechamentos
5. npm run build && npm test verdes antes de abrir PR.
6. testes_novos >= 8
7. Status report: docs/sprints/03-folha-pagamento/status/FE-016-tela-folha-funcionario.md
   Incluir: decisão ValeForm inline vs modal + confirmação que stub ModalFechamento tem as props corretas.
8. Após status report: criar PR para integration/03-folha-pagamento e mergear.
```

**Mensagem de commit:** `feat(FE-016): tela folha funcionario — vales, adiantamentos, fechamentos`

---

## TASK 3 — FE-017: Modal Fechamento

**Pré-condição:** FE-016 mergeada em integration (FolhaFuncionarioPage.tsx + stub ModalFechamento existem).
**Verificar:**
```bash
git fetch origin
git log origin/integration/03-folha-pagamento --oneline | grep fe-016
```

**Prompt para a implementação:**

```
Task: FE-017 — Modal fechamento com cálculo em tempo real.

Leia o plano completo:
  docs/sprints/03-folha-pagamento/plans/FE-017-modal-fechamento.md
Leia também:
  docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §6 (Modal: Fechar Mês)
  docs/decisions/0016-evo09-folha-pagamento.md
  src/paginas/folha/FolhaFuncionarioPage.tsx (onde o modal é invocado — FE-016)
  src/api/folha.ts (adicionar fecharMes() aqui)

## VERIFICAÇÃO ANTES DE CODAR

Confirmar que o stub ModalFechamento existe em FolhaFuncionarioPage.tsx:
  grep -n "ModalFechamento" frontend/src/paginas/folha/FolhaFuncionarioPage.tsx
Se não encontrar: FE-016 não mergeou → PARAR.

## A TASK

Substituir o stub ModalFechamento pela implementação real.

Criar:
1. src/components/folha/ModalFechamento.tsx — modal com:
   - Preview: salário base, total vales, total parcelas, ajuste (editável, default 0.00), valor final.
   - Cálculo em tempo real: valorFinal = salarioBase - totalVales - totalParcelas + ajuste.
     ZERO roundtrips — tudo local com os dados recebidos via props.
   - Badge vermelho se valorFinal < 0.
   - Botão "Confirmar fechamento" → POST /api/funcionarios/{id}/fechamentos.
   - Loading state no botão durante POST.
   - Sucesso: toast + fechar modal + onConfirm() (FE-016 invalida cache).
   - Erro 409: mensagem "Mês já fechado".

Estender:
2. src/api/folha.ts — adicionar fecharMes(funcionarioId, mes, ajuste): Promise<PedidoFolhaResponse>.

Modificar:
3. src/paginas/folha/FolhaFuncionarioPage.tsx — substituir stub por ModalFechamento real.

## PROPS (mesmo contrato do stub de FE-016):
  interface ModalFechamentoProps {
    funcionarioId: number;
    mes: string;              // "YYYY-MM"
    salarioBase: number;
    vales: Vale[];
    adiantamentosAtivos: Adiantamento[];
    onConfirm: () => void;
    onClose: () => void;
  }

## REGRAS

1. Branch: feature/fe-017-modal-fechamento de origin/integration/03-folha-pagamento
2. Território: SÓ frontend/. Zero financas_bot_telegram/.
3. Preview: ZERO chamadas de API — cálculo 100% local.
4. Erro 409 → "Mês já fechado" (não exibir stack trace).
5. 1 commit: feat(FE-017): ModalFechamento com calculo em tempo real
6. npm run build && npm test verdes.
7. testes_novos >= 5
8. Status report: docs/sprints/03-folha-pagamento/status/FE-017-modal-fechamento.md
   Incluir: confirmação cálculo 100% local + como ficou o tratamento do 409.
9. Após status report: criar PR para integration/03-folha-pagamento e mergear.
```

**Mensagem de commit:** `feat(FE-017): ModalFechamento com calculo em tempo real`

---

## AO FINAL (todas as tasks concluídas)

```bash
git fetch origin
git log origin/integration/03-folha-pagamento --oneline | grep -E "fe-015|fe-016|fe-017"
```
As 3 devem aparecer. Frontend Sprint 03 completo. Humano revisa de manhã.
