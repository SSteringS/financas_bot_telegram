---
task: FIX-004
titulo: "Consolidar domain/vo/ em domain/enums/ — eliminar pacote duplicado"
sprint: 03-folha-pagamento
data_planejamento: 2026-06-04
branch_alvo: fix/004-consolidar-pacote-vo-em-enums
prioridade: baixa
esforco: baixo
territorio: back
estado: pronto-pra-execucao
depende_de: []
bloqueia: []
skills_dispatched: []
integration_branch: null
fluxos_qa: []
---

# FIX-004 — Consolidar `domain/vo/` em `domain/enums/`

## Intake

- **Origem:** identificado pelo humano no code review manual da sprint 03 (2026-06-04). Registrado em `docs/PENDENCIAS-TECNICAS.md` como "Dois pacotes paralelos para enums de domínio".
- **Por quê agora:** `domain/vo/` foi criado na sprint 03 para `CategoriaPedido` e `FormaPagamento`. O pacote `domain/enums/` já existia com `StatusPedido`, `TipoArquivo`, `TipoPagamento`, `TipoUploadS3`. São dois pacotes com o mesmo propósito — qualquer dev que adicionar um enum no futuro não sabe onde colocar.
- **Esforço:** baixo — renomear pacote + atualizar imports. Sem lógica nova, sem risco funcional.
- **Riscos resumidos:** zero risco funcional. Risco de compilação se algum import for esquecido — coberto pelo `mvn compile`.

---

## Contexto

`vo` vem de Value Object (DDD): objetos imutáveis com igualdade baseada em valor (ex.: `Money`, `CPF`, `Email`). Enums são constantes nominais — o conceito correto é diferente. O implementador da sprint 03 usou `domain/vo/` provavelmente porque o plano não especificava o pacote de destino e `enums/` não estava documentado como convenção.

Resultado: `domain/enums/` tem os enums pré-existentes e `domain/vo/` tem dois enums novos. Funcionam igual, mas criam confusão estrutural.

**Decisão de consolidação:** mover tudo para `domain/enums/` (padrão pré-existente do projeto) e deletar `domain/vo/`.

---

## Decisão / abordagem

1. Mover `CategoriaPedido` e `FormaPagamento` de `domain/vo/` para `domain/enums/`.
2. Atualizar todos os imports que referenciam `domain.vo.CategoriaPedido` e `domain.vo.FormaPagamento`.
3. Deletar o pacote `domain/vo/` (pasta e diretório).
4. `mvn compile` para confirmar zero erros de import.
5. `mvn test` para confirmar zero regressões.

---

## Escopo / arquivos

### Mover
- `domain/vo/CategoriaPedido.java` → `domain/enums/CategoriaPedido.java`
- `domain/vo/FormaPagamento.java` → `domain/enums/FormaPagamento.java`

### Atualizar imports (buscar com grep)
```bash
grep -r "domain\.vo\." financas_bot_telegram/src/ --include="*.java" -l
```
Todos os arquivos listados precisam ter `domain.vo.` substituído por `domain.enums.`.

### Deletar
- Pasta `domain/vo/` (após verificar que está vazia)

### Não tocar
- `frontend/` — zero mudança no front
- Nenhuma lógica de negócio — apenas renomear pacote e atualizar imports

---

## Testes

`testes_novos: 0` — não adiciona testes. Validação: `mvn compile` + `mvn test` verdes.

---

## Critérios de aceitação

- [ ] `domain/vo/` não existe mais no projeto.
- [ ] `CategoriaPedido` e `FormaPagamento` estão em `domain/enums/`.
- [ ] `grep -r "domain\.vo\." financas_bot_telegram/src/` retorna zero resultados.
- [ ] `mvn compile` verde.
- [ ] `mvn test` verde (sem regressões).
- [ ] Branch: `fix/004-consolidar-pacote-vo-em-enums` saindo de `develop`.
- [ ] 1 commit: `refactor(FIX-004): mover domain/vo/ para domain/enums/ — eliminar pacote duplicado`.
- [ ] Status report em `docs/sprints/03-folha-pagamento/status/FIX-004-*.md`.

---

## Fora de escopo

- Criar Value Objects reais em `domain/vo/` — não há demanda agora.
- Documentar convenção de pacotes no `CLAUDE.md` — pode ser feito junto pelo engenheiro de IA ao implementar ADR 0019 (onde ficam as convenções de domínio).

---

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Import esquecido causa `mvn compile` vermelho | Baixa | Baixo | `grep` lista todos os arquivos; critério exige compile verde |
| Pasta `vo/` não deletada por engano | Baixa | Baixo | Critério de aceitação exige ausência de `domain/vo/` |

---

## Coordenação

- **Pode rodar em paralelo com:** qualquer task front (território disjunto).
- **Depende sequencialmente de:** nada — sai direto de `develop`.
- **Bloqueia:** nada.
- **Após merge:** marcar o item de PENDENCIAS-TECNICAS.md "Dois pacotes paralelos" como `~~resolvido~~`.

---

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report válido, ciclo reviewer→QA conforme ADR 0019. PR `fix/004-... → develop` (FIX vai direto para develop).

---

## Referências

- `docs/PENDENCIAS-TECNICAS.md` §"Dois pacotes paralelos para enums de domínio"
- `docs/sprints/03-folha-pagamento/avaliacoes/BE-026-029-sprint03-folha.md §8`
- `docs/decisions/0019-workflow-reviewer-qa-loop.md` — contexto do code review que identificou o problema
