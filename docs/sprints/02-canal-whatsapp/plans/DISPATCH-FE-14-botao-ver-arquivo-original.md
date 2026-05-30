# DISPATCH — FE-14-botao-ver-arquivo-original (single-task)

> **Sobre este arquivo:** prompt pronto pra colar numa sessão nova do **Claude do front** pra executar a FE-14. Estilo **single-task dispatch** — irmão do `DISPATCH-FIX-idempotencia-porta-application.md`.
>
> **Quando colar:** a qualquer momento após 2026-05-30. Front está disjunto do back; pode rodar em paralelo com o PR `develop → main` (deploy do código WhatsApp inerte) ou com qualquer task nova de back. **Última task de produto pendente da sprint 02.**

---

## Pré-condições

- FIX-001 (PR #78), BE-17b (PR #79), BE-22 (PR #77), BE-19 (PR #76), BE-19a, BE-21a, FE-13 (codegen `tipos.ts`) — **todos mergeados em `develop`** ✅.
- Worktree do planner em `develop` (CLAUDE.md §"Worktrees git"). Implementador deve usar `git fetch && git checkout -b <branch> develop` direto — sem `git checkout develop` antes.
- **Endpoint backend a confirmar:** `GET /api/v1/pedidos/{id}/imagem` (ou nome análogo). Service `ObterUrlImagemPedidoServiceImpl` existe; **exposição no controller a verificar no `/v3/api-docs` OU no `tipos.ts` gerado pela FE-13**. Se faltar, ver §"Se quebrar".

## O prompt (cole tudo a partir daqui na sessão do Claude do front)

```
Você é o Claude do front deste projeto. Leia, NESTA ORDEM:

- CLAUDE.md (regras globais — atenção especial à seção "Worktrees git" e ao Fluxo de branches atualizado em 2026-05-29 — convenção FIX-NNN zero-padded, território, definição de pronto)
- docs/roles/frontend.md (seu papel)
- docs/STATE.md (onde estamos no ciclo — sprint 02 em modo fechamento)
- docs/sprints/02-canal-whatsapp/README.md (objetivo da sprint)
- docs/sprints/02-canal-whatsapp/plans/FE-14-botao-ver-arquivo-original.md (a task — leia inteiro, intake até referências)

## A TASK

Adicionar um botão de menor destaque no `PedidoCard` pra abrir a foto/PDF original do pedido (o arquivo que veio quando o pedido foi registrado, antes de virar pago). Reusar o padrão visual e técnico do `ModalComprovante` existente, parametrizado por tipo de arquivo. Botão aparece em TODOS os PedidoCards (não condicional a status pago/temComprovante).

## REGRAS DURAS

1. Branch: `feature/fe-14-botao-ver-arquivo-original`. Padrão novo (CLAUDE.md): `feature/<id>-<slug>` — id é `fe-14`.
2. Criação da branch (atenção à mudança do CLAUDE.md): NÃO faça `git checkout develop` (develop está checada no worktree do planner — git vai recusar). Em vez disso:
   git fetch
   git checkout -b feature/fe-14-botao-ver-arquivo-original develop
3. UM commit, padrão `feat(FE-14): botão ver foto/PDF original no PedidoCard`.
4. Território: SÓ `frontend/`. NÃO toque em `financas_bot_telegram/`, `infra/`, `.github/`, `docs/architecture/`.
5. NÃO mergeie. Abrir PR pra develop após status report + Reviewer (CLAUDE.md §"Definição de pronto").

## DECISÕES A TOMAR (anotar no status report)

A. **Generalizar `ModalComprovante` em `ModalArquivo`** (recomendado pelo plano) **OU clonar pra `ModalImagemPedido`** (alternativa aceitável). Decisão é tua — mas anote a justificativa.
B. **Estado dos dois modais:** independente (ambos podem estar abertos teoricamente — só um visível por z-index) OU mutuamente exclusivo (abrir um fecha o outro). Decisão UX tua; anote.

## CONFIRMAÇÃO ANTES DE CODAR

Antes de escrever código, confirme:

1. **Endpoint existe e está exposto?** Rode:
   curl -s https://api.satyansaita.com/v3/api-docs | jq '.paths | keys' | grep -i imagem
   OU procure no `frontend/src/api/tipos.ts` (codegen FE-13) por algo tipo `pedidoIdImagemGet` / `obterUrlImagem` / análogo.
   - Se EXISTE: prossiga.
   - Se NÃO EXISTE no OpenAPI mas o service `ObterUrlImagemPedidoServiceImpl` está no back: o problema é exposição no controller. Ver §"Se quebrar".

2. **Estilo Tailwind do botão secundário:** confirme classes do plano (linha 57-66 do plano) — altura `36px`, cor neutra `border-zinc-300 bg-white text-zinc-700`. Ajuste se design system do projeto preferir token diferente.

## VALIDAÇÃO LOCAL OBRIGATÓRIA

- `npm run lint` ok
- `npm run build` ok
- `npm test` verde, com testes novos passando:
  - `PedidoCard.test.tsx`: botão "Ver foto/PDF do pedido" sempre renderiza; click chama callback.
  - `ModalArquivo.test.tsx` (se generalizar) OU `ModalImagemPedido.test.tsx` (se clonar): abre/fecha, foco no botão fechar, ESC fecha, src correto por tipo.
  - `Home.test.tsx`: dois modais coexistem conforme decisão B.

## STATUS REPORT

Escrever em `docs/sprints/02-canal-whatsapp/status/FE-14.md` seguindo `docs/status/_TEMPLATE.md`. Frontmatter válido (incluindo `estado`, `testes_total`, `testes_novos`, `lint`, `build`). Anotar:

- Decisão A (generalizar vs clonar) + justificativa
- Decisão B (estado independente vs mutuamente exclusivo) + justificativa
- Resultado da confirmação do endpoint (existia? path exato?)
- Prints da UI antes/depois (mobile 375px + desktop) — opcional mas recomendado pro Reviewer
- testes_total e testes_novos

## SE QUEBRAR

Cenário 1 — **endpoint não exposto** (service existe, controller não):
- Crie status report parcial (`estado: parcial`) explicando o bloqueio.
- Abra issue/plano `FIX-NNN-expor-endpoint-imagem-pedido.md` em `docs/sprints/02-canal-whatsapp/plans/` (próximo NNN livre — confirmar com planner).
- Sua FE-14 fica esperando o FIX backend mergear.
- NÃO crie código mockado tipo "endpoint hipotético". Pare e reporte.

Cenário 2 — **endpoint retorna 404 pra pedidos sem imagem** (pré-migração S3):
- Tratar no front com fallback "imagem não disponível" + log do erro.
- Anotar no status report.

Cenário 3 — **teste do `ModalComprovante` existente quebra após generalização**:
- Esperado: ajustar import + passar `tipo='comprovante'` é OK.
- Se quebrar comportamento (não só ajuste de mock): PARE, registre `estado: parcial`, e abra discussão. A generalização não deve mudar UX do modal existente.

Pare ao final do status report. Não mergeie.
```

---

## Notas pro humano (fora do prompt)

- **Estimativa de duração da sessão:** 1-2h (alinhado com o plano). Tempo real depende de (a) decisão entre generalizar/clonar; (b) se endpoint backend já está exposto.
- **Coordenação com outras tarefas:** Pode rodar em **paralelo** com o PR `develop → main` (deploy do back). Front e back disjuntos.
- **Próxima task depois desta:** se FE-14 mergear limpo, sprint 02 fica em estado de fechamento total — só faltaria a RETRO-02 + plano sprint 03. Se cair em "endpoint não exposto", a sprint 02 ganha mais um FIX backend antes de fechar.
- **UX-sensível** — Reviewer vai pedir prints. Vale a pena fazer prints mesmo se a sessão estiver com pressa.

## Referências

- Plano completo: `docs/sprints/02-canal-whatsapp/plans/FE-14-botao-ver-arquivo-original.md`.
- CLAUDE.md §"Worktrees git" + §"Fluxo de branches" + §"Definição de pronto".
- `docs/roles/frontend.md`.
- `docs/status/_TEMPLATE.md`.
- Modelo de DISPATCH: `docs/sprints/02-canal-whatsapp/plans/DISPATCH-FIX-idempotencia-porta-application.md`.
- Backend: `application/services/ObterUrlImagemPedidoServiceImpl` (service existente — exposição a confirmar).
