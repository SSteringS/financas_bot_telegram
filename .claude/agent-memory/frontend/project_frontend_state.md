---
name: Projeto — estado do frontend
description: Sprint ativa, tasks FE concluídas, pendências conhecidas no frontend
type: project
---

Sprint atual: **03-folha-pagamento** (em andamento — só spec EVO-09 criada até agora, sem tasks FE planejadas)

Tasks FE concluídas (histórico relevante):
- FE-12: Resumo parametrizado e contadores (sprint 01)
- FE-13: Codegen tipos OpenAPI (sprint 01)
- FE-14: Botão ver foto/PDF original no PedidoCard (sprint 02) — estado: concluído

Branch de feature ativa: `feature/fe-14-botao-ver-arquivo-original` (já mergeada/concluída)

Pendências conhecidas do FE-14 (para tarefa futura):
- Botão de foto aparece mesmo em pedidos sem imagem; backend retorna 404 nesses casos — o iframe mostra a página de erro do browser. Fallback visual ("imagem não disponível") foi deixado para tarefa futura, caso o humano queira.
- `ModalArquivo` usa `id="modal-arquivo-titulo"` fixo — seguro enquanto modais forem mutuamente exclusivos.

**Why:** Manter contexto das decisões e pendências abertas para que futuras tasks FE partam de um estado conhecido.

**How to apply:** Antes de qualquer task FE, verificar se as pendências acima são escopo da nova tarefa.
