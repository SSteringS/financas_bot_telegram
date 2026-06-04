---
task: FIX-004
titulo: "Consolidar domain/vo/ em domain/enums/ — eliminar pacote duplicado"
data: 2026-06-04
branch: fix/004-consolidar-pacote-vo-em-enums
responsavel: claude-back
estado: concluido
gates:
  build: ok
  lint: na
  testes: ok
  testes_total: 352
  testes_novos: 0
  cobertura_pct: na
  branch_convencao: ok
  territorio: ok
commits: [633f7d5]
pr: https://github.com/SSteringS/financas_bot_telegram/pull/107
desvios: 0
pendencias_humano: 0
---

# FIX-004 — Consolidar `domain/vo/` em `domain/enums/`

## O que foi feito

Movidos `CategoriaPedido` e `FormaPagamento` do pacote `domain.vo` para `domain.enums`,
unificando a convenção de pacotes do domínio. A pasta `domain/vo/` foi deletada.

Todos os 18 arquivos Java que importavam `domain.vo.CategoriaPedido` ou
`domain.vo.FormaPagamento` foram atualizados com `sed` de uma vez. `mvn compile`
confirmou zero erros de import; `mvn test` confirmou zero regressões (321 testes, 0 falhas).

`domain/enums/` agora consolida os 6 enums do domínio:
- Pré-existentes: `StatusPedido`, `TipoArquivo`, `TipoPagamento`, `TipoUploadS3`
- Migrados: `CategoriaPedido`, `FormaPagamento`

---

## Desvios do plano

Nenhum.

---

## Decisões tomadas durante a execução

**`sed` em lote para os 18 arquivos:** em vez de editar cada arquivo manualmente, `grep -l` listou todos os arquivos com referências a `domain.vo.` e `xargs sed -i` atualizou todos de uma vez. Approach mais seguro que edição manual — sem risco de esquecer um arquivo.

---

## Decisões pendentes (esperando humano)

Nenhuma — tarefa fechada.

---

## Próximos passos / observações pro próximo

- Marcar o item "Dois pacotes paralelos para enums de domínio" em `docs/PENDENCIAS-TECNICAS.md` como resolvido após o merge.
- Convenção estabelecida: qualquer novo enum de domínio vai em `domain/enums/`. Se um dia surgir um Value Object real (objeto imutável com comportamento, como `Money` ou `CPF`), criar `domain/vo/` então — mas não antes.

---

## Padrões técnicos

**Refactor puro — sem mudança de comportamento.** O nome do pacote é metadado de organização; os valores dos enums (`VALE`, `FOLHA`, `PIX`, `TED`) e a serialização JPA (`@Enumerated(EnumType.STRING)`) continuam idênticos. O banco não é afetado — o discriminador STI é o nome do valor do enum (`"VALE"`, `"FOLHA"`), não o pacote Java.

**Convenção de pacotes como documentação:** `domain/enums/` agora é auto-descritivo — qualquer dev que abrir o projeto sabe onde ficam os enums do domínio. Elimina a ambiguidade `vo/` vs `enums/` para contribuições futuras.

---

## Arquivos criados/modificados

- `domain/enums/CategoriaPedido.java` (novo — migrado de `domain/vo/`)
- `domain/enums/FormaPagamento.java` (novo — migrado de `domain/vo/`)
- `domain/vo/CategoriaPedido.java` (deletado)
- `domain/vo/FormaPagamento.java` (deletado)
- 18 arquivos `.java` com imports atualizados de `domain.vo.` → `domain.enums.`
