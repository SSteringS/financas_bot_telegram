---
task: FE-015
titulo: "Tela Funcionários — lista + formulário de cadastro/edição"
sprint: 03-folha-pagamento
data_planejamento: 2026-05-31
branch_alvo: feature/fe-015-tela-funcionarios
prioridade: alta
esforco: medio
territorio: front
estado: pronto-pra-execucao
depende_de: [BE-025]
bloqueia: [FE-016]
skills_dispatched: [boas-praticas-react, ecossistema-frontend, seguranca-web-frontend]
fluxos_qa: []
---

# FE-015 — Tela Funcionários — lista + formulário de cadastro/edição

## Intake

- **Origem:** spec EVO-09 §6 (Tela: Lista de Funcionários).
- **Por quê agora:** FE-016 (tela de folha) navega a partir desta tela.
- **Esforço:** médio — lista, formulário com campos condicionais por forma de pagamento, badge de conta de terceiro.
- **Riscos resumidos:** campos condicionais PIX/TED no formulário precisam de validação dinâmica no front que espelhe a validação do back.

---

## Contexto

Esta é a **aba nova "Folha de pagamento"** no front. Só o filho acessa (mesma auth existente).

Endpoints disponíveis após BE-025:
- `GET /api/funcionarios` — lista ativos.
- `POST /api/funcionarios` — cria.
- `PUT /api/funcionarios/{id}` — atualiza.
- `DELETE /api/funcionarios/{id}` — desativa.

---

## Decisão / abordagem

Nova rota `/folha` no router existente (`src/App.tsx`). Aba nova no layout principal.

Formulário único para cadastro e edição (controlado por modo: "novo" vs "editar"). Campos bancários exibidos condicionalmente conforme `forma_pagamento` selecionado (PIX → chave_pix; TED → banco, agência, conta, tipo_conta).

Badge laranja para `conta_propria = false` (pagamento para terceiro — ex: familiar).

Clique na linha da tabela navega para `FE-016` (`/folha/funcionarios/{id}`).

---

## Escopo / arquivos

### Criar
- `src/paginas/folha/FuncionariosPage.tsx` — lista de funcionários.
- `src/components/folha/FuncionarioCard.tsx` ou linha de tabela.
- `src/components/folha/FuncionarioForm.tsx` — formulário com campos condicionais.
- `src/api/folha.ts` — funções: `listarFuncionarios()`, `criarFuncionario()`, `atualizarFuncionario()`, `desativarFuncionario()`.
- `src/types/folha.ts` — types: `Funcionario`, `FormaPagamento`, `FuncionarioRequest`.
- Rotas novas em `src/App.tsx`.

### Modificar
- Navegação/menu principal — adicionar aba "Folha de pagamento".

### Não tocar
- `src/paginas/Home.tsx` e componentes de pedidos — sem escopo aqui.

---

## Testes

- **Unitários/componente:** `FuncionarioForm` — renderiza campos PIX quando forma=PIX, renderiza campos TED quando forma=TED, esconde campos do outro formato.
- **Integração leve:** mock das chamadas de API (MSW ou similar), verificar que lista renderiza, formulário submete.

`testes_total` esperado: ≥ testes_existentes + 6. `testes_novos` ≥ 6.

---

## Critérios de aceitação

- [ ] Rota `/folha` existe e renderiza a lista de funcionários ativos.
- [ ] Tabela exibe: nome, salário base, status ativo/inativo.
- [ ] Badge laranja aparece para `conta_propria = false`.
- [ ] Botão "Novo funcionário" abre formulário de cadastro.
- [ ] Formulário: ao selecionar PIX → exibe campo `chave_pix`; ao selecionar TED → exibe banco, agência, conta, tipo_conta; campos do outro formato ficam ocultos.
- [ ] Submeter formulário válido cria funcionário e atualiza a lista.
- [ ] Edição via clique no funcionário da lista (ou botão de edição) preenche o form com dados existentes.
- [ ] "Desativar" remove da lista (soft delete via DELETE).
- [ ] Clique na linha navega para `/folha/funcionarios/{id}` (FE-016).
- [ ] `npm test` verde com `testes_novos ≥ 6`.
- [ ] Branch: `feature/fe-015-tela-funcionarios`.
- [ ] Status report com frontmatter válido.

---

## Fora de escopo

- Tela de vales, adiantamentos, fechamento — FE-016 e FE-017.
- Visualização de funcionários inativos no histórico — nice-to-have, não MVP.

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Validação condicional do form difícil com react-hook-form | Média | Baixo | Usar `watch('forma_pagamento')` + `register` condicional; ver docs react-hook-form |
| Tipos TS do backend desalinhados | Baixa | Médio | Checar `GET /api/funcionarios` response contra `Funcionario` type antes de implementar |

---

## Coordenação

- **Pode rodar em paralelo com:** BE-026, BE-027, BE-028 (não depende deles).
- **Depende sequencialmente de:** BE-025 (endpoints de funcionário).
- **Bloqueia:** FE-016.
- **Atenção pro Reviewer:** verificar campos condicionais (nenhum campo TED aparece no modo PIX e vice-versa); confirmar navegação para FE-016.
- **Após merge:** despachar FE-016.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, revisão do Reviewer. PR pra `develop`.

---

## Referências

- `docs/sprints/03-folha-pagamento/specs/evo-09-folha-pagamento.md` §6
- `docs/decisions/0016-evo09-folha-pagamento.md` §6
- Padrão existente: `src/paginas/Home.tsx`, `src/api/pedidos.ts`
