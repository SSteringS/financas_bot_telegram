# DISPATCH — FE-016-tela-folha-funcionario (single-task)

> **Quando usar:** após BE-028 E FE-015 **ambas mergeadas** em `integration/03-folha-pagamento`.
> - BE-028: endpoints de vales, adiantamentos e fechamento precisam existir.
> - FE-015: src/types/folha.ts e src/api/folha.ts precisam existir (FE-016 os estende).
> ⚠️ FE-016 modifica arquivos criados por FE-015 — não pode rodar antes do merge de FE-015.

---

## Pré-condições (git)

Confirmar as duas antes de despachar:
```
git log origin/integration/03-folha-pagamento --oneline | grep -E "be-028|fe-015"
```
Ambas devem aparecer. Se alguma faltar, aguardar o merge.

---

## O prompt (cole tudo numa sessão `--agent frontend`)

```
Task: FE-016 — Tela Folha do Funcionário — vales, adiantamentos, fechamentos.

Localize e leia o plano completo (initialPrompt orienta o Glob).
Leia também:
- docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md §6 (Tela Folha do Funcionário)
- docs/decisions/0016-evo09-folha-pagamento.md
- src/types/folha.ts (criado em FE-015 — estender com novos types)
- src/api/folha.ts (criado em FE-015 — estender com novas funções)
- src/paginas/folha/FuncionariosPage.tsx (FE-015 — padrão visual a seguir)

## A TASK

Tela acessada via /folha/funcionarios/{id}. Esta é a operação central do filho.
Exibe: header com nome + salário base, e 3 seções independentes.

### Criar:
1. src/paginas/folha/FolhaFuncionarioPage.tsx — tela principal com as 3 seções + seletor de mês.
2. src/components/folha/ValesSection.tsx — vales do mês com seletor de mês (últimos 6 meses).
3. src/components/folha/AdiantamentosSection.tsx — adiantamentos ativos com parcelas.
4. src/components/folha/FechamentosSection.tsx — accordion de fechamentos anteriores.
5. src/components/folha/ValeForm.tsx — form inline ou modal para registrar vale.
6. src/components/folha/AdiantamentoForm.tsx — form para novo adiantamento.
7. src/hooks/folha/useFolhaFuncionario.ts — hook agregador das 3 queries (TanStack Query).

### Estender (arquivos de FE-015):
8. src/api/folha.ts — adicionar: listarVales(), criarVale(), listarAdiantamentos(), criarAdiantamento(), cancelarAdiantamento(), listarFechamentos().
9. src/types/folha.ts — adicionar: Vale, ValeRequest, Adiantamento, AdiantamentoRequest, Fechamento.

### Modificar:
10. src/App.tsx — confirmar/adicionar rota /folha/funcionarios/:id.

## REGRAS DURAS

1. Branch: `feature/fe-016-tela-folha-funcionario` saindo de `origin/integration/03-folha-pagamento`
   (develop já tem FE-015 com src/types/folha.ts e src/api/folha.ts).
2. Território: SÓ `frontend/`. Zero `financas_bot_telegram/`.
3. NUNCA a partir de outra feature branch — sempre de origin/integration/03-folha-pagamento (ver CLAUDE.md §Fluxo de branches).
4. NÃO tocar src/paginas/Home.tsx — decisão §7 é tarefa separada.
5. Botão "Fechar mês YYYY-MM": aparecer SOMENTE se não existe Pedido FOLHA com mes_referencia do mês selecionado. Implementar verificação nos dados de fechamentos já carregados (sem roundtrip extra).
6. Vales com fechado=true: exibir em cinza/tachado (já fechados).
7. Cancelar adiantamento: confirmação antes de chamar DELETE.
8. Clique no botão de fechar mês: abrir ModalFechamento (FE-017). Como FE-017 ainda não existe, criar um placeholder/stub: `ModalFechamento` recebe as props corretas mas exibe "Em construção". FE-017 vai substituir o stub.
9. 1 commit: `feat(FE-016): tela folha funcionario — vales, adiantamentos, fechamentos`.
10. NÃO mergeie. PR pra `integration/03-folha-pagamento` após status report + Reviewer.

## DECISÕES A TOMAR (anotar no status report)

A. **ValeForm inline vs modal:** form inline na tabela de vales (mais leve, menos CSS) ou modal separado?
B. **Seletor de mês:** <select> nativo com os últimos 6 meses, ou componente customizado?

## TESTES

- Componente (mínimo 5):
  ValesSection: renderiza vales do mês selecionado; vales fechados aparecem em cinza/tachado; seletor de mês muda a query.
  FechamentosSection: accordion expande a observação ao clicar.
  FolhaFuncionarioPage: botão "Fechar mês" aparece quando mês não foi fechado; NÃO aparece quando já fechado.
- Integração leve (mínimo 3):
  mock das 3 queries, tela renderiza sem erro; form de vale submete e atualiza lista; cancelar adiantamento com confirmação.

`testes_novos` ≥ 8.

## STATUS REPORT

`docs/sprints/03-folha-pagamento/status/FE-016-tela-folha-funcionario.md`

Incluir:
- Decisões A e B (form inline vs modal; seletor de mês) com justificativa.
- Nota sobre decisão §7 (qual opção PO escolheu, se já decidida).
- Confirmação que stub de ModalFechamento tem as props corretas para FE-017 integrar.
- Resultado dos testes de condicionalidade do botão "Fechar mês".

## SE QUEBRAR

Cenário — src/types/folha.ts ou src/api/folha.ts não existem em develop:
  FE-015 não foi mergeada. Verificar pré-condição antes de continuar.

Cenário — endpoint de vales ou adiantamentos tem shape diferente do plano:
  Adaptar os types. Anotar divergência no status report. NÃO inventar shape — usar o que o backend retorna.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano

- **Pode rodar em paralelo com BE-029** (front e back são disjuntos).
- **Pré-condição dupla:** FE-015 + BE-028. A mais lenta das duas determina quando FE-016 pode iniciar.
- **Estimativa:** 2-3h (a task mais trabalhosa do front: 3 seções + accordion + seletor de mês + forms).
- **Após merge desta:** despachar FE-017 imediatamente. FE-017 modifica FolhaFuncionarioPage.tsx — precisa desta mergeada.

## Referências

- `docs/sprints/03-folha-pagamento/plans/FE-016-tela-folha-funcionario.md`
- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §6
- `docs/decisions/0016-evo09-folha-pagamento.md`
