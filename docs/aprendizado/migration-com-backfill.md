# Migration com backfill

## Contexto da dúvida

Apareceu em 2026-08-09, no desenho do experimento de alocação de modelo por papel (`docs/experiments/models-claude-experiment/`). A rubrica de complexidade das features dá **`+2` para "migration com backfill"** — o item de maior peso junto com integração externa. A pergunta foi direta: *o que quer dizer migration com backfill?*

Importa porque é esse item que separa a feature de complexidade Média (Contatos) da Alta (reabertura de mês) no experimento.

## Resumo destilado

**Migration** é uma mudança versionada de schema. No projeto é Flyway: `V<n>__nome.sql`, executados em ordem, uma vez cada, com histórico numa tabela de controle.

**Backfill** é a parte da migration que **preenche a estrutura nova com dados derivados dos que já existem**. Criar coluna ou tabela é trivial — nasce vazia. O difícil é que produção já tem dados, e os registros antigos precisam fazer sentido dentro do modelo novo.

### O espectro (com exemplos reais do repo)

**1. Sem backfill** — `V7__pedido_requisitante_nullable.sql`:
```sql
ALTER TABLE pedidos_pagamento MODIFY COLUMN requisitante_id BIGINT NULL;
```
Só relaxa uma restrição. Nenhum dado antigo muda.

**2. Backfill por valor default** — `V3__add_tipo_arquivo_comprovantes.sql`:
```sql
ALTER TABLE comprovantes
    ADD COLUMN tipo_arquivo ENUM('IMAGEM','PDF') NOT NULL DEFAULT 'IMAGEM';
```
Tem backfill, mas o MySQL faz sozinho: todo comprovante antigo vira `IMAGEM`. Funcionou porque a premissa era verdadeira (antes do V3 só existia imagem). Caso degenerado — **atenção: se a premissa estiver errada, corrompe silenciosamente**.

**3. Backfill derivado** — o que pesa `+2`. Os dados novos são **calculados** a partir dos antigos, com regra de negócio no meio:

- *Cópia com transformação* (Contatos): ler `funcionario.chave_pix` (String crua), **inferir o tipo** (CPF? e-mail? telefone?), **normalizar**, inserir como `Contato`. Decisão embutida: chave que não casa com tipo nenhum — descarta, vira `ALEATORIA`, ou aborta?
- *Reconstrução* (reabertura de mês): não existe tabela de fechamento. O backfill **reconstrói** os fechamentos históricos agrupando `pedidos_pagamento` por `(funcionario_id, mes_referencia)` onde `categoria='FOLHA'`. O dado nunca existiu como tal — está sendo inferido de um efeito colateral do modelo antigo.

## Pontos-chave

- **Migration ≠ backfill.** Migration muda a forma; backfill preenche o conteúdo histórico. Muitas migrations não têm backfill nenhum.
- **`DEFAULT` em `ADD COLUMN NOT NULL` já é backfill** — o banco aplica a todas as linhas existentes. Barato, mas só serve quando um valor único serve pra todo mundo.
- **Ordem importa:** preencher **antes** de aplicar `NOT NULL` / `UNIQUE` / `FK`. Inverter funciona em dev (tabela vazia) e quebra em produção.
- **MySQL não tem DDL transacional.** DDL faz auto-commit. Não existe rollback da migration inteira — se falhar no meio, fica meio aplicada e o Flyway trava exigindo intervenção manual.
- **Idempotência não vem de graça.** Pensar no que acontece se rodar duas vezes ou retomar após falha parcial. `INSERT ... SELECT ... WHERE NOT EXISTS` costuma ser a forma defensiva.
- **Produção tem o registro que ninguém previu.** A regra precisa decidir explicitamente entre pular, corrigir ou abortar — e isso é decisão de negócio, não técnica.
- **Lock:** `UPDATE` de backfill em tabela grande segura a tabela. Por isso o `CLAUDE.md` do backend exige comentário sobre impacto de lock no cabeçalho da migration.
- **Banco de dev não valida nada disso.** Vazio, qualquer backfill passa. Só Testcontainers com dados semeados (padrão do repo) expõe os defeitos reais.

## Por que é bom discriminador de qualidade

Backfill derivado concentra armadilhas que não aparecem em código novo: idempotência, ordem de aplicação, ausência de rollback, dados sujos, lock, e um ambiente de teste que por padrão não pega nada. Uma implementação fraca escreve o `INSERT ... SELECT` e para por aí — e passa em todos os testes locais.

## Pra aprofundar

- Migrations expand/contract (ou parallel change) — separar em passos para deploy sem downtime
- `pt-online-schema-change` / `gh-ost` para ALTER em tabela grande sem lock
- Backfill em lotes com checkpoint, para tabelas onde um `UPDATE` único é inviável
- Flyway: `repair`, migrations versionadas vs repetíveis, e por que checksum muda trava o deploy
