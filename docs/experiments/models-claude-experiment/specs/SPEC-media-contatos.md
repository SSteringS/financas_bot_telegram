# SPEC — Média — Contatos: agenda de chaves PIX

> **Congelada em 2026-08-10.** Score de complexidade: **5**.
> Entrada idêntica para todos os runs. Não editar durante a execução do experimento.

## Objetivo

Permitir que um requisitante guarde chaves PIX que usa com frequência, com apelido, para não precisar redigitar a chave a cada uso.

## Contexto mínimo

Hoje `chavePix` existe apenas como `String` crua em `Funcionario` (`FuncionarioEntity:33`), validada só por `!isBlank()` quando `formaPagamento == PIX`. Não existe entidade, tabela, endpoint, value object nem enum de tipo de chave.

`Pedido` é escopado por requisitante (`@RequisitanteId` + `IsolamentoRequisitanteIntegrationTest`). `Funcionario` é global. **`Contato` segue o padrão de `Pedido`.**

## Requisitos funcionais

1. Um `Contato` tem: apelido, chave PIX, tipo da chave, estado ativo/inativo, `criado_em`, `atualizado_em`.
2. Tipos de chave suportados: `CPF`, `CNPJ`, `EMAIL`, `TELEFONE`, `ALEATORIA`.
3. A chave é **validada conforme o tipo declarado**. CPF e CNPJ conferem dígito verificador. Telefone segue E.164. Aleatória é UUID. E-mail segue formato de e-mail.
4. A chave é **normalizada** antes de persistir, de forma consistente por tipo — a mesma chave informada com máscaras diferentes resulta no mesmo valor armazenado.
5. Contato é **escopado por requisitante**. Um requisitante nunca enxerga nem altera contato de outro.
6. Não pode haver dois contatos ativos com a **mesma chave normalizada** para o mesmo requisitante. Violação retorna **409**.
7. Cinco operações REST sob `/api/v1/contatos`: criar, listar, buscar por id, atualizar, remover.
8. Remoção é **soft delete** (`ativo = FALSE`), seguindo a convenção do repositório. Contato removido não aparece na listagem.
9. Chave inválida para o tipo declarado retorna **400** com `ErroDTO`.

## Critérios de aceitação

Cada item é verificável por teste automatizado.

1. Criar contato com CPF válido persiste e retorna 201 (ou 200, conforme a convenção adotada) com o recurso.
2. Criar contato com CPF de dígito verificador inválido retorna **400** e não persiste.
3. Criar contato com CNPJ de dígito verificador inválido retorna **400** e não persiste.
4. Criar contato com telefone fora de E.164 retorna **400** e não persiste.
5. Criar contato com chave aleatória que não é UUID retorna **400** e não persiste.
6. A mesma chave enviada com e sem máscara (ex.: CPF com e sem pontuação) resulta no **mesmo valor normalizado** persistido.
7. Criar segundo contato com chave normalizada já existente **para o mesmo requisitante** retorna **409**.
8. Criar contato com chave já existente **para outro requisitante** é permitido e retorna sucesso.
9. Requisitante A não consegue listar, buscar, atualizar nem remover contato do requisitante B — teste de isolamento explícito.
10. Após remoção, o contato não aparece na listagem, e a chave pode ser cadastrada de novo sem conflito.
11. Chamada sem JWT válido a qualquer rota de `/api/v1/contatos` é rejeitada.
12. `mvn test` verde, sem regressão.

## Fora de escopo

- **Frontend.** Não existe subagente de frontend no repositório. Entregue depois, fora do experimento.
- **Interface pelo bot.** Cadastrar ou consultar contato por mensagem exige estado conversacional, que não existe.
- **Backfill de `funcionario.chave_pix`.** A tabela `funcionario` está vazia em produção — a folha de pagamento nunca foi deployada. Backfill sobre conjunto vazio é trabalho morto. **A migration cria a tabela e nada mais.**
- Usar contatos no fluxo de pedido ou de folha. É agenda isolada nesta entrega.
- Importar contatos de fonte externa.

## O que esta spec deliberadamente não decide

- Se tipo de chave é enum, value object, ou combinação.
- Onde mora a validação e a normalização — domínio, aplicação, ou DTO.
- Se a unicidade é garantida por índice no banco, por checagem no serviço, ou ambos.
- Como o 409 é sinalizado e traduzido.
- Nomes de classe, pacote, arquivo, e o shape dos DTOs.
- Verbo e semântica exata de cada uma das 5 operações (ex.: `PUT` total vs `PATCH` parcial).
- Quebra em tasks, sequência e estratégia de teste.

## Nota sobre risco de teto

`Funcionario` é template quase literal para esta feature. Sem o backfill, o trabalho ficou mais próximo de CRUD puro. **Se todas as configurações do piloto acertarem esta feature, ela perde poder discriminante e o screening precisa ser reavaliado** — ver `03-features.md` §5 risco 4.

## Referências

- Padrão referencial de vertical: `Funcionario` (controller, service, adapter, mapper, testes)
- Padrão referencial de isolamento: `Pedido` + `IsolamentoRequisitanteIntegrationTest`
- Convenções do repositório: `03-features.md` §4
