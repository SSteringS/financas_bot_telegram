# FE-14 — Botão "ver foto/PDF original do pedido" (menor destaque, no `PedidoCard`)

> **Intake (contrato de entrada da task)**
>
> - **Origem:** pedido do humano (2026-05-27): *"vamos mudar um pouco o design no front pra ter um botão tbm só que menor destaque pra visualizar a imagem ou arquivo com a prova do pedido de pagamento"*. Plano original foi escrito anteriormente mas **sumiu no sync gremlin do OneDrive** — este é o replanejamento.
> - **Prioridade:** baixa-média. UX nice-to-have; resolve "como vejo o que foi enviado no pedido original sem ir ver no Telegram/WhatsApp?". Não bloqueia nenhuma frente.
> - **Esforço:** **baixo** (~1-2h). Reusa o endpoint backend `ObterUrlImagemPedidoServiceImpl` que **já existe**; reusa o padrão de modal já feito (`ModalComprovante`); só adiciona um botão e abre o modal generalizado.
> - **Território / quem executa:** `frontend/src/` → **Claude do front**. Não toca back.
> - **Branch:** `feature/fe-14-botao-ver-arquivo-original`, a partir de `develop`.
> - **Dependências:** todas em develop ✅. Endpoint `GET /api/v1/pedidos/{id}/imagem` (ou nome equivalente — confirmar no `tipos.ts` gerado do OpenAPI) já existente do MVP.
> - **Riscos:** baixíssimo. Componente isolado; sem mudança de contrato API; cobertura por teste do `PedidoCard` e do modal.

---

## Contexto

O `PedidoCard` (em `frontend/src/components/PedidoCard.tsx`) hoje:
- Mostra descrição, valor, status, legenda de data.
- Quando `pago && temComprovante`, mostra um botão **grande verde** "Ver comprovante" → abre `ModalComprovante` que carrega `/api/v1/pedidos/{id}/comprovante` (presigned URL S3).

O que falta: ver a **foto/PDF original do pedido** (que foi enviada quando o pedido foi registrado, antes de virar pago). Atualmente sem essa visualização — pra conferir o que foi enviado, precisa abrir o histórico no Telegram/WhatsApp.

**Endpoint backend:** existe (`ObterUrlImagemPedidoServiceImpl` em `application/services/`). Provavelmente exposto como `GET /api/v1/pedidos/{id}/imagem` ou similar; confirmar no OpenAPI/tipos.ts.

**Visibilidade:** todo pedido tem imagem/PDF original (foi como veio do canal), então o botão **aparece sempre**, independentemente de `status`. Diferentemente do "Ver comprovante" que depende de `pago && temComprovante`.

## Decisão / abordagem

**Botão secundário de menor destaque** no `PedidoCard`, abaixo (ou ao lado, mobile-first) do "Ver comprovante". Estilo "ghost"/"outline" — não compete visualmente com o verde do comprovante.

**Modal reutilizado** — generalizar `ModalComprovante` em `ModalArquivo`, parametrizado com `tipo: 'comprovante' | 'imagem-pedido'` + `pedidoId`. Internamente escolhe o endpoint correto e usa o mesmo wrapper visual (overlay + frame + botão fechar + ESC + scroll lock).

**Por que generalizar e não duplicar:** os dois modais teriam ~95% do código igual (focus management, ESC handler, scroll lock, layout responsivo). Duplicar agora vira problema na próxima vez (terceiro tipo de arquivo a mostrar = 3 cópias). Refator pequeno e local.

**Aceitável alternativa** (decisão do implementador): manter `ModalComprovante` como está + criar `ModalImagemPedido` quase-idêntico. Menos refator agora, mais duplicação. Anotar a decisão.

## Escopo / arquivos

### Criar

- (Recomendado) `frontend/src/components/ModalArquivo.tsx` — versão genérica do modal atual, recebe `pedidoId: number | null` + `tipo: 'comprovante' | 'imagem-pedido'` + `onClose`. Internamente:
  ```tsx
  const src = tipo === 'comprovante'
    ? urlComprovante(pedidoId!)
    : urlImagemPedido(pedidoId!)
  ```
- **OU** (alternativa) `ModalImagemPedido.tsx` — clone direto. Pior pra manutenção; aceitável.

### Modificar

- `frontend/src/api/pedidos.ts` (ou onde estão os helpers de URL):
  - Adicionar `export function urlImagemPedido(id: number): string` análogo a `urlComprovante`. Ler do OpenAPI quais paths existem; provavelmente `/api/v1/pedidos/{id}/imagem`.
- `frontend/src/components/PedidoCard.tsx`:
  - Adicionar prop `onAbrirImagemPedido: () => void` (sibling de `onAbrirComprovante`).
  - Renderizar botão secundário **sempre** (não condicional a `pago`/`temComprovante`):
    ```tsx
    <button
      className="w-full mt-2 border border-zinc-300 bg-white text-zinc-700 py-1.5 rounded-lg text-xs font-medium flex items-center justify-center gap-1.5 hover:bg-zinc-50 transition-colors"
      onClick={onAbrirImagemPedido}
      aria-label={`Ver foto/PDF original do pedido ${pedido.descricao}`}
      style={{ minHeight: '36px' }}
    >
      <svg className="w-3.5 h-3.5" /* ícone de "image" ou "paperclip" */ />
      Ver foto/PDF do pedido
    </button>
    ```
  - Tamanho/peso visual menor que o "Ver comprovante" verde (cor neutra, padding menor, fonte menor, ícone menor, altura `36px` vs `44px`).
- `frontend/src/paginas/Home.tsx`:
  - Adicionar estado `pedidoIdAbrirImagem: number | null` (sibling do já existente `pedidoIdAberto` que é pra comprovante).
  - Renderizar segundo modal (`ModalArquivo tipo="imagem-pedido" pedidoId={pedidoIdAbrirImagem}`) ou usar uma única peça generalizada com discriminator.
  - Passar `onAbrirImagemPedido={() => setPedidoIdAbrirImagem(pedido.id)}` pro `PedidoCard`.
- `frontend/src/components/ModalComprovante.tsx`:
  - Se for pelo caminho **generalizar**: substituir por `ModalArquivo` e remover. Atualizar imports.
  - Se for pelo caminho **clonar**: deixar como está; só ajustar imports da nova peça em outros lugares.
- **Testes (atualizar)**:
  - `PedidoCard.test.tsx` (se existir; senão criar) — botão "Ver foto/PDF do pedido" sempre renderiza; click chama `onAbrirImagemPedido`.
  - `ModalArquivo.test.tsx` (se generalizar) ou `ModalImagemPedido.test.tsx` (se clonar) — abre/fecha, foco no botão de fechar, ESC fecha, src correto por `tipo`.
  - `Home.test.tsx` — dois modais podem coexistir? Estado independente?

### Não tocar

- API backend (endpoint já existe).
- Estilo global / Tailwind config.
- Lógica de listagem / paginação / filtros.
- `Timeline.tsx`.

## Critérios de aceitação

- [ ] Botão "Ver foto/PDF do pedido" aparece em **todos** os `PedidoCard`s (independente de status).
- [ ] Tamanho/peso visual menor que o "Ver comprovante" (validar com print mobile + desktop no PR).
- [ ] Click abre modal com a foto/PDF original do pedido (`/api/v1/pedidos/{id}/imagem` redireciona pra presigned URL).
- [ ] Modal de "Ver comprovante" (caso exista pro pedido em questão) continua funcionando independente.
- [ ] Os dois modais **não podem abrir ao mesmo tempo** (UX) — só um aberto por vez (state independente, mas se um abrir, o outro fecha; OU desabilita o outro botão enquanto um está aberto). Decisão do implementador.
- [ ] Acessibilidade: `aria-label` no botão, focus trap no modal, ESC fecha, focus volta pro botão que abriu.
- [ ] Responsivo: botão e modal funcionam em mobile (375px width mínimo).
- [ ] `npm run lint` ok, `npm run build` ok, `npm test` verde com testes novos passando.
- [ ] Branch saiu de `develop` (fluxo novo: `git fetch && git checkout -b feature/fe-14-botao-ver-arquivo-original develop`).
- [ ] Território só `frontend/`.
- [ ] Status report `docs/sprints/02-canal-whatsapp/status/FE-14.md` com frontmatter válido + anotar:
  - Decisão: generalizar `ModalArquivo` ou clonar (`ModalImagemPedido`)?
  - Decisão: estado independente vs mutuamente exclusivo dos dois modais?
  - Print da UI antes/depois (mobile + desktop) se possível.

## Fora de escopo (explicitamente)

- **Download direto** (forçar `download` em vez de visualizar) — fica pra outra task se virar demanda.
- **Carrossel** se um pedido tiver múltiplos arquivos — hoje só tem um por pedido; arquitetura não suporta.
- **PDF viewer in-app** (PDF.js etc.) — confiar no viewer nativo do browser via `<embed>` ou `<iframe>` que já é o padrão do `ModalComprovante`.
- **Cache local** dos arquivos baixados — presigned URL expira; cache iria criar URLs quebradas. Pular.
- **Mostrar o `caption` original** (texto que veio com a foto, ex.: "150.00 Almoço") — útil mas separado; entra como FE-15 se virar prioridade.
- **Mudar a UI do modal de comprovante** — preservar a aparência atual; só refatorar o código se for pelo caminho "generalizar".

## Riscos & mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Endpoint `/api/v1/pedidos/{id}/imagem` não está exposto no controller (só o service existe) | Média | Bloqueia até fix | Verificar no OpenAPI antes de escrever; se faltar, **a task vira FE-14 (front) + uma FIX backend de exposição**. Decisão: anotar como pendência e abrir FIX. |
| Generalização do modal quebra os testes existentes do `ModalComprovante` | Média | Baixo | Manter API do modal antiga compatível (`ModalArquivo` aceita `tipo`); testes existentes só precisam ajustar import + passar `tipo='comprovante'`. |
| Botão a mais polui o `PedidoCard` em mobile | Média | Baixo | Botão de altura `36px` (menor que o `44px` do principal), padding pequeno. Print no PR pra Reviewer validar. |
| Pedidos antigos (pré-migração de S3) sem imagem disponível → 404 | Baixa | Médio (UX ruim) | Backend já trata? Confirmar — se sim, modal mostra erro elegante; se não, tratar no front com fallback "imagem não disponível". |
| Conflito de merge com FE-12 (codegen de tipos OpenAPI) se ainda não mergeada | Baixa | Baixo | Confirmar se FE-13 já está em develop antes (item #1 do backlog dizia que sim — codegen está). Se sim, `urlImagemPedido` deve ter helper análogo ao `urlComprovante`. |

## Coordenação

- **Sem conflito de território com back** — front e back disjuntos.
- **Pode rodar em paralelo** com qualquer task do back (FIX-idempotencia, BE-19, BE-22, BE-17b).
- **Após merge:** smoke manual mobile + desktop conferindo UX. Atualizar `STATE.md` se aplicável (provavelmente não — UX iterativa não pesa no estado macro).
- **Reviewer foca em:** (a) botão realmente menos destacado visualmente (não competir com "Ver comprovante"); (b) acessibilidade preservada; (c) decisão "generalizar vs clonar" documentada.

## Definição de pronto

Gates do `docs/runbooks/PRE-MERGE-CHECKLIST.md`, status report com **prints da UI antes/depois** (mobile + desktop), e **revisão do Reviewer** (UX-sensível). Abrir PR pra `develop`; **não mergear sozinho**.

## Referências

- Pedido original do humano (chat 2026-05-27).
- `frontend/src/components/PedidoCard.tsx` — local onde o botão entra.
- `frontend/src/components/ModalComprovante.tsx` — modelo a reusar/generalizar.
- `frontend/src/api/pedidos.ts` (ou similar) — helpers de URL; adicionar `urlImagemPedido`.
- OpenAPI `/v3/api-docs` — confirmar nome do endpoint backend.
- Backend: `application/services/ObterUrlImagemPedidoServiceImpl` (service existente — confirmar exposição no controller).
- Princípios de UX da fase 3 (sprint 01): mobile-first, acessibilidade WCAG AA, tap targets ≥ 36px (preferencialmente 44px).
- `docs/plans/FE-13-codegen-tipos-openapi.md` (se mergeada, `tipos.ts` deve ter a operação correspondente).
