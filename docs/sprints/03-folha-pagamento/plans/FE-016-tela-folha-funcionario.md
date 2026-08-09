---
task: FE-016
titulo: "Tela Folha do Funcionário — vales, adiantamentos, fechamentos"
sprint: 03-folha-pagamento
data_planejamento: 2026-05-31
branch_alvo: feature/fe-016-tela-folha-funcionario
prioridade: alta
esforco: alto
territorio: front
estado: pronto-pra-execucao
depende_de: [BE-026, BE-027, BE-028, FE-015]
bloqueia: [FE-017]
skills_dispatched: [boas-praticas-react, ecossistema-frontend]
fluxos_qa: []
---

# FE-016 — Tela Folha do Funcionário — vales, adiantamentos, fechamentos

## Intake

- **Origem:** spec EVO-09 §6 (Tela: Folha do Funcionário). Maior task de front desta sprint.
- **Por quê agora:** agrega vales + adiantamentos + histórico de fechamentos em uma só tela; FE-017 (modal de fechamento) depende dela.
- **Esforço:** alto — 3 seções com queries independentes, seletor de mês, accordion de fechamentos, botão condicional de fechar mês.
- **Riscos resumidos:** ✅ **Decisão §7 resolvida (2026-06-04 — PO): Opção A** — vales NÃO aparecem na lista do Pedro. `GET /api/pedidos` e `Home.tsx` ficam intocados por enquanto. Se necessário filtrar no futuro, entra como FIX separado.

---

## Contexto

Acessada via clique em funcionário na `FuncionariosPage` (FE-015). Rota: `/folha/funcionarios/{id}`.

Esta é a tela de **operação do filho** — Pedro (pai) não acessa esta rota.

Endpoints disponíveis após BE-025/026/027/028:
```
GET  /api/funcionarios/{id}
GET  /api/funcionarios/{id}/vales?mes=YYYY-MM
GET  /api/funcionarios/{id}/adiantamentos
GET  /api/funcionarios/{id}/fechamentos
POST /api/funcionarios/{id}/fechamentos          → abre FE-017
POST /api/funcionarios/{id}/adiantamentos
DELETE /api/adiantamentos/{adiantamentoId}
```

---

## Decisão / abordagem

**Seletor de mês:** controla qual mês é exibido nas seções de vales e no botão de fechar mês. Default: mês corrente. Exibir os últimos 6 meses no seletor.

**Seção Vales do mês:** tabela com descrição, valor, status (`fechado` vs `aberto`). Botão "Registrar vale" abre form inline ou modal simples (definir na implementação — form inline é mais leve). Vales com `fechado=true` aparecem em cinza/tachado.

**Seção Adiantamentos ativos:** tabela com descrição, `parcela X/total`, valor da parcela. Botão "Novo adiantamento" abre modal/form. "Cancelar" chama DELETE com confirmação.

**Seção Fechamentos anteriores:** lista de Pedidos FOLHA ordenados por `mes_referencia DESC`. Clique expande (accordion) mostrando o campo `observacao` (breakdown do cálculo). Badge de status do Pedido FOLHA (PENDENTE/PAGO).

**Botão "Fechar mês YYYY-MM":** aparece **somente se** não existe Pedido FOLHA com `mes_referencia` do mês selecionado. Abre `ModalFechamento` (FE-017).

### Decisão §7 (vales na lista do Pedro) — ✅ Resolvida
**Opção A aprovada pelo PO em 2026-06-04:** vales NÃO aparecem na lista do Pedro. `GET /api/pedidos` e `Home.tsx` ficam intocados. Esta tela não é afetada — é exclusiva do filho. Se no futuro precisar filtrar vales da lista do Pedro, entra como FIX separado pós-sprint.

---

## Escopo / arquivos

### Criar
- `src/paginas/folha/FolhaFuncionarioPage.tsx` — tela principal com as 3 seções.
- `src/components/folha/ValesSection.tsx` — seção de vales com seletor de mês.
- `src/components/folha/AdiantamentosSection.tsx` — seção de adiantamentos.
- `src/components/folha/FechamentosSection.tsx` — accordion de fechamentos anteriores.
- `src/components/folha/ValeForm.tsx` — form de registro de vale (inline ou modal).
- `src/components/folha/AdiantamentoForm.tsx` — form de novo adiantamento.
- `src/hooks/folha/useFolhaFuncionario.ts` — hook agregador das 3 queries (TanStack Query).
- Adicionar funções em `src/api/folha.ts`: `listarVales()`, `criarVale()`, `listarAdiantamentos()`, `criarAdiantamento()`, `cancelarAdiantamento()`, `listarFechamentos()`.
- Adicionar types em `src/types/folha.ts`: `Vale`, `ValeRequest`, `Adiantamento`, `AdiantamentoRequest`, `Fechamento`.

### Modificar
- `src/App.tsx` — rota `/folha/funcionarios/:id`.

### Não tocar
- `src/paginas/Home.tsx` — decisão §7 resolve em tarefa separada.

---

## Testes

- **Componente:** `ValesSection` renderiza vales com seletor de mês; vales fechados aparecem diferentes; `FechamentosSection` accordion expande a observação.
- **Integração leve:** mock das 3 queries, verificar que a tela renderiza sem erro.

`testes_total` esperado: ≥ testes_existentes + 8. `testes_novos` ≥ 8.

---

## Critérios de aceitação

- [ ] Rota `/folha/funcionarios/{id}` renderiza a tela com header (nome + salário base).
- [ ] Badge de conta de terceiro visível se `conta_propria = false`.
- [ ] Seção Vales: seletor de mês funciona; tabela exibe vales do mês selecionado; vales `fechado=true` aparecem em cinza/tachado.
- [ ] Botão "Registrar vale" abre form; submeter cria vale e atualiza a lista.
- [ ] Seção Adiantamentos: tabela exibe `parcela X/total` e valor da parcela; "Cancelar" com confirmação chama DELETE.
- [ ] Seção Fechamentos: lista em ordem decrescente de `mes_referencia`; clique expande `observacao`.
- [ ] Botão "Fechar mês YYYY-MM" aparece **somente** se o mês não foi fechado.
- [ ] Clique no botão de fechar mês abre `ModalFechamento` (FE-017) com props corretos.
- [ ] Após confirmação do fechamento (FE-017), a tela atualiza (refetch vales + fechamentos).
- [ ] `npm test` verde com `testes_novos ≥ 8`.
- [ ] Branch: `feature/fe-016-tela-folha-funcionario`.
- [ ] Status report com frontmatter válido — incluindo nota sobre decisão §7 (qual opção o PO escolheu e qual tarefa de acompanhamento foi criada, se necessário).

---

## Fora de escopo

- Modal de fechamento — FE-017.
- Modificação da lista do Pedro (`Home.tsx`) — FIX separado após §7 ser decidido.
- Visualização de vales históricos (meses já fechados com `fechado=true`) — exibidos normalmente na tabela do mês correspondente.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| 3 queries paralelas gerando loading states conflitantes | Média | Baixo | `useFolhaFuncionario` agrega com `isLoading` combinado |
| `FolhaFuncionarioPage` fica grande demais | Média | Baixo | Extrair seções como componentes separados (já planejado) |
| Adiantamento cancelado ainda aparece na lista | Baixa | Baixo | `GET /adiantamentos` retorna só ativos; invalidar cache após DELETE |

---

## Coordenação

- **Pode rodar em paralelo com:** FE-017 (conceitualmente, mas FE-017 depende do botão fechar mês desta tela).
- **Depende sequencialmente de:** BE-026, BE-027, BE-028, FE-015.
- **Bloqueia:** FE-017.
- **Atenção pro Reviewer:** verificar que botão "Fechar mês" é condicional (não aparece se já fechado); confirmar que as 3 seções têm queries independentes; verificar que decisão §7 está anotada no status report.
- **Após merge:** despachar FE-017.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR pra `develop`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §6
- `docs/decisions/0016-evo09-folha-pagamento.md`
- Padrão existente: `src/paginas/Home.tsx`, `src/hooks/usePedidos.ts`
