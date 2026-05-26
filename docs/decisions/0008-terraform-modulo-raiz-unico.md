# ADR 0008 — Terraform como módulo raiz único (sem child modules)

**Data:** 2026-05-26
**Status:** Accepted
**Decisores:** humano (com ajuda do Claude planejador)

> **Homologado pelo humano em 2026-05-26:** "seguir com o Terraform do jeito que está por enquanto". As razões abaixo (reconstruídas pelo planejador) foram ratificadas sem ajuste. A decisão é explicitamente *por ora* — vale até disparar um dos gatilhos de reavaliação listados no fim.

---

## Contexto

A infraestrutura do projeto vive em `financas_bot_telegram/infra/` como um conjunto de arquivos `.tf` no **diretório raiz**, organizados por recurso/assunto (`provider.tf`, `network.tf`, `security.tf`, `ec2.tf`, `lambda.tf`, `acm.tf`, `dns.tf`, `frontend.tf`, `variables.tf`, `outputs.tf`). **Não há diretório `modules/`** nem uso de child modules próprios — a separação é só por arquivo, dentro de um único módulo raiz.

À medida que a infra cresceu (rede, EC2, RDS, S3/CloudFront, DNS/ACM), valeria registrar se essa topologia é uma **decisão** consciente a manter, ou um default a revisitar. Este ADR fixa a decisão (pendente de homologação).

---

## Decisão

Manter a infra como **um único módulo raiz**, organizada por **arquivo por assunto**, sem extrair child modules (`modules/`) **enquanto o projeto for de ambiente único e dono solo**. Reavaliar só quando surgir uma necessidade concreta (multiplicidade de ambientes que justifique reuso, ou um bloco grande o suficiente pra valer encapsular).

---

## Razões

- **YAGNI / simplicidade.** Módulos do Terraform existem pra **reuso** e **encapsulamento** quando há repetição (vários ambientes/instâncias do mesmo conjunto de recursos). Com um único ambiente de produção e um dono solo, extrair módulos adiciona indireção (variáveis de entrada, outputs, versionamento) sem reuso real que a pague.
- **Legibilidade > abstração nesta escala.** Arquivo-por-assunto já dá navegação clara (`dns.tf`, `frontend.tf`) sem o custo cognitivo de saltar entre módulo-chamador e módulo-chamado.
- **Mudança barata depois.** Extrair um módulo quando a dor aparecer é refactor local e incremental; não é uma porta que se fecha agora.

---

## Consequências

**Positivas:**
- Setup simples de ler e mexer; `terraform plan/apply` direto na pasta, sem orquestração de módulos.
- Menos boilerplate (sem interfaces de entrada/saída de módulo a manter).

**Negativas:**
- Sem reuso encapsulado — se um dia existir um segundo ambiente (staging), haverá duplicação ou refactor pra parametrizar.
- Estado único: tudo no mesmo state file (raio de explosão maior de um `apply` que dê errado). Mitigação atual: revisão de `plan` antes de `apply` (e o fix do drift CRLF/LF, FIX-gitattributes-eol, que mantém o `plan` limpo e legível).

---

## Alternativas consideradas

- **Quebrar em child modules (`modules/network`, `modules/compute`, `modules/frontend`, …):** descartado por enquanto. Indireção e versionamento sem reuso que justifique no estágio atual (ambiente único, solo).
- **Workspaces / múltiplos states por ambiente:** fora de escopo — não há multiplicidade de ambientes hoje.

---

## Gatilho de reavaliação

Promover a child modules quando **qualquer** destes aparecer: (a) um segundo ambiente real (staging/QA) que faria duplicar recursos; (b) um bloco de infra grande e coeso o bastante pra que encapsular melhore a legibilidade; (c) intenção de publicar/reaproveitar parte da infra fora deste repo.

---

## Referências

- `financas_bot_telegram/infra/*.tf` (a topologia atual)
- `docs/aprendizado/git-line-endings-crlf-lf.md` e `docs/status/FIX-gitattributes-eol.md` (estado limpo do `plan`, relacionado ao raio de explosão do state único)
- ADR 0004 (taxonomia: por que isto é ADR e não aprendizado/CLAUDE.md)
</content>
