# Propostas de Design — Front-end de visualização

Três variantes de design pro site que seu pai vai usar pra ver os pedidos e baixar comprovantes. Abra os arquivos `.html` no navegador (idealmente no celular também) pra sentir como cada um se comporta.

## Como avaliar

O critério principal não é estético — é: **se você passasse o site pro seu pai sem nenhuma instrução, ele saberia onde clicar pra achar o comprovante de um boleto pago em março?**

Pense em três casos de uso enquanto navega:

1. **Caso comum**: ele abre o site logo depois que você manda um link "olha o comprovante aqui". Quer ver o último pedido pago e baixar.
2. **Caso retrospectivo**: ele precisa do comprovante de algo de meses atrás. Vai filtrar/buscar.
3. **Caso de status**: ele quer saber "o que ainda tá pendente?".

A variante que melhor resolve os três é a vencedora. Não precisa ser uma só — você pode pegar elementos de cada.

## As três variantes

### `variante-a-visual.html` — Cartões grandes, foco em clareza
Cada pedido é um cartão grande com foto destacada, valor em fonte enorme e botões generosos. Espaço sobrando, tipografia respirada. Visual mais "moderno-amigável", tipo Notion / Linear / apps de delivery.

**A favor**: zero ambiguidade, qualquer pessoa não-técnica entende. Toques são fáceis (área grande). Bonito.
**Contra**: mostra menos itens por tela. Com 100 pedidos por semana, o usuário vai rolar bastante (mas tem filtro por mês e busca, então não é problema na prática).

### `variante-b-bancaria.html` — Lista densa estilo app de banco
Layout mais informacional, denso, com tabs no topo (Pendentes / Pagos / Todos) e linhas separadas por divisor. Visual que o brasileiro mais velho associa com app de banco — territoriedade familiar.

**A favor**: ele já viu esse padrão em Itaú/Bradesco/Caixa. Curva de aprendizado zero. Mostra mais por tela.
**Contra**: visual mais "frio" e profissional. Botões menores podem ser difíceis pra dedo grande.

### `variante-c-timeline.html` — Linha do tempo cronológica
A informação principal é a **data**. Items são agrupados por dia ("Hoje", "Ontem", "Sexta 1 de maio"), com seletor de mês no topo. Otimizado pro caso "preciso do comprovante de uma data específica".

**A favor**: o caso de uso retrospectivo (achar comprovante antigo por data) fica trivial. Diferente das outras opções, esse é o único onde "quando aconteceu" é o eixo principal.
**Contra**: pode parecer mais complexo na primeira impressão.

## Próximos passos

Depois de escolher uma variante (ou pedir ajustes em alguma), eu posso:
- Escafolder o projeto React de verdade com a variante escolhida
- Misturar elementos das três num design final customizado
- Iterar em cima de uma delas com mudanças específicas (mais cores, tipografia diferente, layout alternativo)
