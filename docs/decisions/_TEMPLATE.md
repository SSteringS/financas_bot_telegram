# NNNN — Título da decisão

> **Não edite este arquivo.** Copie pra `NNNN-titulo-em-kebab-case.md` (ex: `0003-react-pwa-em-vez-de-native.md`) e preencha lá.
>
> ADRs são imutáveis depois de aceitos. Se uma decisão for revogada no futuro, **não edite este arquivo** — crie um novo ADR que **supersedes** este, e mude o status deste pra `Superseded by NNNN`.

---

**Data:** YYYY-MM-DD
**Status:** `Proposed` | `Accepted` | `Deprecated` | `Superseded by NNNN`
**Decisores:** humano (com ajuda do Claude planejador)

---

## Contexto

Qual é o problema ou a situação que motivou essa decisão? Descreva o que estava em jogo, o que era ambíguo, e por que precisava decidir agora.

Exemplo:
> O front-end precisa virar um app no celular do usuário. Existem três caminhos: PWA instalável, Capacitor (wrap do React em shell nativo), ou React Native (reescrita parcial pra runtime nativo). Cada caminho tem custo e benefício diferente, e a escolha cascateia em decisões de stack e tempo de implementação.

---

## Decisão

O que foi decidido. Seja específico — uma frase curta e clara.

Exemplo:
> Vamos com **PWA via React + vite-plugin-pwa**. Capacitor entra no roadmap pra quando o usuário pedir presença em loja oficial. React Native fica fora de cogitação pelos próximos 12 meses.

---

## Razões

Por que essa decisão e não as alternativas.

Exemplo:
- O caso de uso (visualizar dados, baixar arquivos) não requer APIs nativas (câmera, GPS, push pesado).
- PWA com `add to home screen` já é "como app" pro usuário não-técnico.
- Reaproveitamento 100% — quando precisarmos de loja, Capacitor reaproveita 95% sem reescrever.
- React Native exigiria 50% de reescrita do UI, sem benefício proporcional.

---

## Consequências

O que muda no projeto por causa dessa decisão. Liste tanto o positivo quanto o negativo.

**Positivas:**
- Tempo de implementação curto.
- Mesma codebase em web e "app".
- Sem custos com App Store / Play Store nesta fase.

**Negativas:**
- Sem presença em lojas oficiais (alguns usuários esperam isso).
- Limites de PWA: notificações iOS são fracas até iOS 16.4+, alguns recursos nativos indisponíveis.

---

## Alternativas consideradas

Liste as opções que foram avaliadas e por que não foram escolhidas. Mesmo que pareça óbvio agora, o "eu do futuro" vai querer saber que essas opções foram pensadas.

- **Capacitor agora**: descartado por enquanto. Adiciona ~1 semana de trabalho e taxas anuais ($99 Apple, $25 Google) sem benefício claro nesta fase.
- **React Native**: descartado pelos próximos 12 meses. Custo de implementação alto demais pro caso de uso atual; PWA cobre a necessidade.
- **Web sem PWA**: descartado. Pedro não vai entender "abrir o site" sem ter um ícone. PWA dá ícone na tela inicial sem custo extra.

---

## Referências

Links pra discussões, documentação, benchmarks que embasaram a decisão.

- `docs/architecture/especificacao-tecnica.md` seção 4
- Conversa com humano em [data]
