# SPEC — Alta — Extração do valor do boleto por código de barras

> **Congelada em 2026-08-10.** Score de complexidade: **7**.
> Entrada idêntica para todos os runs. Não editar durante a execução do experimento.

## Objetivo

Quando o usuário manda a foto ou o PDF de um boleto **sem digitar o valor**, o sistema extrai o valor do próprio boleto e cria o pedido de pagamento, respondendo com o valor lido para conferência.

Ganho: quem usa o bot fotografa o boleto e não digita mais nada.

## Contexto mínimo

Hoje, criar pedido exige legenda no formato `<valor> <descrição>` (regex `^(\d+([.,]\d{1,2})?)\s+(.+)$`). Sem valor na legenda, não há pedido.

Boleto brasileiro carrega o valor codificado no **código de barras** e na **linha digitável**, junto com vencimento e banco. Não é necessário "interpretar" a imagem — é leitura estruturada, com **dígito verificador** que permite detectar leitura errada.

O repositório já aceita `photo` e `document` (PDF) como mídia entrante.

## Requisitos funcionais

1. Mensagem com mídia cuja legenda **não** casa com o formato de pedido e **não** começa com `#` dispara tentativa de extração.
2. A extração lê o código de barras ou a linha digitável da mídia e obtém o **valor** do boleto.
3. A extração roda **localmente, sem chamada de rede**. Nenhuma API externa, nenhum serviço de IA.
4. O resultado da extração é **validado pelo dígito verificador** do boleto antes de ser usado.
5. Extração bem-sucedida cria o pedido com o valor extraído e responde ao usuário informando **o valor lido** e o **id do pedido**, para conferência.
6. Extração que falha — mídia ilegível, dígito verificador inconsistente, documento que não é boleto — **não cria pedido** e responde com mensagem orientativa pedindo o valor pelo formato atual.
7. São suportados **boleto bancário** e **boleto de concessionária** (água, luz, telefone), que têm formatos distintos.
8. O fluxo atual permanece intacto: legenda com valor explícito continua criando pedido sem passar por extração.
9. A extração nunca deixa a aplicação em estado inconsistente nem derruba o processamento da mensagem — falha é tratada, não propagada.

## Critérios de aceitação

Cada item é verificável por teste automatizado, com entradas conhecidas.

1. Linha digitável válida de **boleto bancário** produz o valor correto.
2. Linha digitável válida de **boleto de concessionária** produz o valor correto.
3. Linha digitável com **dígito verificador inválido** é rejeitada — não cria pedido.
4. Boleto **sem valor** (campo zerado, comum em boleto de valor aberto) é rejeitado com mensagem orientativa — não cria pedido com valor zero.
5. Mídia sem código de barras legível é rejeitada com mensagem orientativa — não cria pedido.
6. Extração bem-sucedida cria pedido com o valor extraído e status inicial igual ao do fluxo atual.
7. A resposta ao usuário contém o valor lido em formato legível e o id do pedido.
8. Legenda com valor explícito (`150,00 luz`) continua criando pedido pelo caminho atual, **sem** invocar extração.
9. Legenda começando com `#` continua indo para o fluxo de comprovante, **sem** invocar extração.
10. Falha na extração não impede o processamento da mensagem nem gera exceção não tratada.
11. Valor é interpretado em centavos sem erro de arredondamento — verificado com pelo menos um caso de centavos não triviais.
12. `mvn test` verde, sem regressão.

## Restrições

- **Sem chamada de rede na extração.** Requisito 3 é inegociável: é o que mantém a feature determinística, gratuita e testável.
- **A aplicação roda em EC2 `t4g.micro` (ARM, memória escassa).** Qualquer dependência nova precisa caber nesse envelope. Peso da dependência e consumo de memória são parte da avaliação.
- **A dependência precisa ser compatível com ARM64** — a EC2 é Graviton.

## Fora de escopo

- OCR de texto livre e leitura de documentos que não sejam boleto.
- Extração de vencimento, banco emissor ou beneficiário — **apenas o valor** nesta entrega.
- Confirmação em duas etapas antes de salvar. O pedido nasce salvo; o usuário confere pela resposta e corrige depois se necessário. Confirmação exigiria estado conversacional, que não existe no repositório.
- Qualquer uso de API de IA ou serviço externo, pago ou gratuito.
- Canal WhatsApp.
- Reprocessar boletos já recebidos no histórico.

## O que esta spec deliberadamente não decide

- Qual biblioteca usar para ler código de barras, e se haverá fallback por OCR.
- Se a leitura de PDF passa por extração de texto, renderização para imagem, ou ambos.
- Onde mora o parser da linha digitável e onde mora a validação do dígito verificador.
- Se a extração é síncrona no processamento da mensagem ou assíncrona.
- Como a descrição do pedido é preenchida quando o usuário não digitou nada.
- Nomes de classe, pacote, arquivo, e o contrato interno do extrator.
- Quebra em tasks, sequência e estratégia de teste.

## Pesquisa que faz parte da task

Os itens abaixo **não** foram verificados no desenho e são trabalho legítimo da execução:

- Posições exatas dos campos de valor na linha digitável e no código de barras, conforme especificação **FEBRABAN**.
- Regra de cálculo dos dígitos verificadores (módulo 10 e módulo 11) e onde cada um se aplica.
- Diferenças estruturais entre boleto bancário e de concessionária, incluindo quantidade de dígitos e identificação do tipo.

Errar por não ter pesquisado conta como defeito. A spec informa **que** precisa ser correto, não **qual** é a resposta.

## Referências

- `docs/plans/BACKLOG-produto.md` — EVO-03, registrado originalmente como OCR via Textract; esta spec é a variante local e determinística
- Convenções do repositório: `03-features.md` §4
