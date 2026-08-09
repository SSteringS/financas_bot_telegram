---
name: arquitetura-hexagonal
description: Arquitetura hexagonal do projeto — estrutura de camadas, regras de dependência, como implementar uma feature respeitando as fronteiras, como projetar novas portas/adapters, e como detectar violações em review. Carregar quando a task toca application/, domain/, adapters/, ou quando o plano/proposta define como uma feature entra na arquitetura.
allowed-tools: Read, Grep, Glob, Bash
load_pattern: shared
used_by: [backend, architect, reviewer, planner]
created: 2026-05-30
status: ativa
---

# Skill — Arquitetura Hexagonal

## Quando carregar

- **Backend:** ao implementar qualquer classe em `application/`, `domain/` ou adapters. Antes de decidir onde uma nova classe mora.
- **Architect:** ao propor como uma feature entra na arquitetura — definir porta, adapter, contrato de interface.
- **Reviewer:** ao revisar PRs que tocam `application/` ou `infra/` — detectar violações.
- **Planner:** ao escrever spec de feature que cria ou modifica portas/adapters ou cruza fronteiras de camada. Saber onde a feature "mora" (qual porta, qual adapter) antes de definir o critério de aceite.

## Resumo

A arquitetura hexagonal isola o domínio de qualquer framework, banco ou canal externo. Teste-chave: **o domínio compila sem Spring, sem JPA, sem Feign**. Se não compila, há vazamento.

## Estrutura de camadas

```
financas_bot_telegram/
  domain/             ← entidades, value objects, exceções. Zero dependência externa.
  application/
    ports/
      in/             ← interfaces que o mundo externo chama (ex: ProcessarMensagemUseCase)
      out/            ← interfaces que o domínio usa pra sair (ex: MensagemRepository, MensagemSenderPort)
    services/         ← casos de uso. Depende só de domain/ e ports/.
  adapters/
    in/               ← controllers REST, webhook handlers (telegram/, whatsapp/)
    out/              ← JPA, Feign, S3 (persistence/, messaging/)
```

## Regras de dependência

| Camada | Pode importar | NÃO pode importar |
|---|---|---|
| `domain/` | Nada externo | Spring, JPA, Feign, qualquer lib de infra |
| `application/` | `domain/`, `ports/` | `adapters/`, Spring beans concretos, JPA |
| `adapters/in/` | `application/ports/in/`, `domain/` DTOs | Outros adapters, `adapters/out/` concreto |
| `adapters/out/` | `application/ports/out/`, `domain/` | `adapters/in/`, services concretos |

## Como implementar uma nova feature (passo a passo)

1. **Definir a porta de entrada** em `application/ports/in/` — interface com o caso de uso.
2. **Definir a porta de saída** (se precisar persistir ou enviar) em `application/ports/out/`.
3. **Implementar o serviço** em `application/services/` — orquestra, não conhece infra.
4. **Implementar o adapter de entrada** em `adapters/in/<canal>/` — chama a porta in.
5. **Implementar o adapter de saída** em `adapters/out/<tecnologia>/` — implementa a porta out.
6. **Wiring** no módulo Spring: `@Bean` que injeta implementações concretas nas portas.

## Como projetar uma feature (Architect)

- Identificar **quantas portas novas** são necessárias (in + out).
- Verificar se existe porta similar que pode ser reutilizada ou estendida.
- Definir o **contrato da porta** (interface) antes de qualquer implementação — é o artefato que planner referencia no plano.
- Documentar em `docs/architecture/` com diagrama de sequência quando o fluxo é não-óbvio.

## Como escrever spec de feature (Planner)

- Identificar em qual(ais) camadas a feature vive antes de escrever os critérios de aceite.
- Se a feature precisa de nova porta: o critério de aceite inclui a interface da porta (nome, assinatura).
- Se a feature reutiliza porta existente: verificar em `docs/architecture/especificacao-tecnica.md` o contrato atual.
- Sinal de decisão arquitetural aberta: planner não consegue definir o critério de aceite sem saber qual porta usar → chamar `@architect` como subagente.

## Detecção de violações (Reviewer + Backend)

```bash
# Infra dentro de domain/ ou application/
grep -r "JdbcTemplate\|@Entity\|@Repository\|FeignClient\|@Table" \
  financas_bot_telegram/domain/ financas_bot_telegram/application/

# Application conhecendo adapter concreto
grep -r "import.*adapters\." financas_bot_telegram/application/

# Instanciação direta em vez de injeção por porta
grep -r "new.*Repository\|new.*Adapter" financas_bot_telegram/application/
```

Qualquer resultado é violação — registrar como observação na avaliação (Reviewer) ou corrigir antes do commit (Backend).

## Checklist de conformidade

- [ ] Nenhum import de `adapters/` dentro de `application/`.
- [ ] `domain/` sem `@Component`, `@Service`, `@Autowired`, `@Entity`.
- [ ] Novo adapter de saída implementa interface de `application/ports/out/`.
- [ ] Novo adapter de entrada chama interface de `application/ports/in/`, não service concreto.
- [ ] Greps de violação retornam vazio para os arquivos tocados.

## Ler junto

- Skill `padroes-qualidade-codigo` — SOLID + patterns que se aplicam dentro desta estrutura
- `docs/architecture/especificacao-tecnica.md` — spec completa do produto
