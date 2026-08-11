# Testes de arquitetura — ArchUnit vs. reflection puro

## Contexto da dúvida

Sprint 04, revisão da QA-012 (piloto do PIT). O débito #3 da task: o `excludedTestClasses` do mutation testing filtra por `*IntegrationTest`, e **nada força essa convenção** — um teste de integração com outro sufixo escapa do filtro e trava o run (Testcontainers sobe um container por mutante).

A proposta foi criar um teste que verifique a convenção. Surgiram duas implementações possíveis, e a pergunta do humano foi: **"preciso entender melhor esse assunto"** — o que é ArchUnit, o que é "reflection puro", e como escolher.

---

## Resumo destilado

### O conceito antes da ferramenta: *fitness function*

Existe uma classe de regra de projeto que **não é sobre comportamento**. "JPA não vive no domínio", "adapter não conhece adapter", "teste de integração termina em `IntegrationTest`". Nenhuma dessas quebra funcionalidade quando violada — o código compila, os testes passam, o app sobe. Elas quebram **manutenibilidade**, e a violação só aparece meses depois, quando alguém tropeça.

O padrão para isso chama-se **fitness function**: um teste automatizado que verifica uma propriedade *estrutural* do código, não um comportamento. Ele responde "o código continua tendo a forma que decidimos?" em vez de "o código faz a coisa certa?".

A sacada operacional é que ele é **um teste comum**. Roda no `mvn test`, cai no CI, falha o build. Não precisa de tooling novo, de gate de PR, nem de alguém lembrar de rodar. É a diferença entre uma regra escrita em `CLAUDE.md` (que é uma promessa) e uma regra verificada (que é um fato).

### Como funciona por baixo — reflection e o classpath de teste

Java carrega classes em tempo de execução e permite **inspecionar** essas classes pelo próprio código: nome, anotações, superclasse, interfaces, métodos. Isso é *reflection*.

Um teste de arquitetura em reflection puro faz, grosso modo:

1. Varre o diretório `target/test-classes` (ou usa uma lib de scan de classpath) para listar todas as classes de teste compiladas.
2. Para cada uma, pergunta: tem a anotação `@Testcontainers`? estende `AbstractIntegrationTest`?
3. Se sim, checa se o nome simples termina em `IntegrationTest`.
4. Acumula as violações e falha com `assertThat(violacoes).isEmpty()`, listando os nomes.

Sem dependência nova. ~30 linhas. O trabalho chato não é a regra — é o **passo 1**: varrer classpath de forma confiável dá mais problema do que parece (classes internas com `$` no nome, classes abstratas que devem ser ignoradas, diferença entre rodar pela IDE e pelo Maven, JAR vs. diretório).

### O que ArchUnit acrescenta

ArchUnit é uma biblioteca Java (escopo `test`) que embrulha exatamente esse trabalho. Ela importa as classes uma vez e oferece uma DSL declarativa:

```java
@Test
void teste_de_integracao_deve_terminar_em_IntegrationTest() {
    JavaClasses classes = new ClassFileImporter().importPackages("br.com.satyan...");

    ArchRule regra = classes()
        .that().areAnnotatedWith(Testcontainers.class)
        .should().haveSimpleNameEndingWith("IntegrationTest");

    regra.check(classes);
}
```

O ganho não é digitar menos. É que a varredura de classpath, o tratamento de casos-limite e a **mensagem de erro** (que lista cada classe violadora com o caminho do arquivo) vêm prontos e testados. Escrever isso à mão significa reimplementar — e depurar — a parte que não é o seu problema.

E a DSL cobre relações entre classes, não só nomes: dependências entre pacotes, ciclos, quem pode acessar quem. Coisas que em reflection puro exigem parsear bytecode.

### A régua de decisão

**A pergunta não é "qual é melhor".** É **"quantos invariantes você pretende verificar?"**

| | 1 invariante | vários invariantes |
|---|---|---|
| **Reflection puro** | ✅ adequado — dependência zero por ~30 linhas é bom negócio | ❌ vira uma mini-ArchUnit caseira, mal testada |
| **ArchUnit** | ⚠️ dependência para pouco uso | ✅ paga com folga |

No caso deste repo, o desempate é concreto. O `financas_bot_telegram/CLAUDE.md` §"Convenções que valem hoje" lista **várias** regras que hoje existem só em prosa e nunca foram verificadas:

- JPA vive no adapter, não no domínio (`@Entity` só em `adapters/out/persistence/entity/`)
- Adapters não conhecem outros adapters
- Nada novo em `application/usecases/` (pacote legado)
- Persistência = 3 arquivos por agregado
- DTOs com sufixo `Request`/`Response`

Todas são fitness functions naturais. Todas são expressáveis em ArchUnit em poucas linhas. Nenhuma é trivial em reflection puro — as duas primeiras exigem analisar **dependências entre classes**, não atributos de uma classe isolada.

**Conclusão para este repo:** se o objetivo for só destravar o PIT, reflection puro basta e é honesto. Se a intenção for começar a verificar as convenções que hoje são promessa, ArchUnit é a única das duas que escala — e a decisão real não é sobre esta task, é sobre se o repo quer esse tipo de verificação de forma geral.

### O custo que ninguém menciona

Fitness function tem um modo de falha próprio: **regra escrita cedo demais, ou rígida demais, vira atrito**. O time começa a adicionar exceções (`@ArchIgnore`, allowlists) até a regra não significar mais nada — ou pior, refatora o código para satisfazer a ferramenta em vez do desenho.

A mitigação é escolher invariantes que já são consenso e já estão escritos. Não usar a ferramenta para *decidir* arquitetura; usar para *travar* decisão já tomada. As regras do `CLAUDE.md` deste repo passam nesse teste — foram decididas, escritas, e são violáveis por acidente.

---

## Pontos-chave

- **Fitness function** = teste que verifica a *forma* do código, não o comportamento. Roda no `mvn test`, cai no CI de graça.
- Regra em documento é **promessa**; regra em teste é **fato**. A QA-012 mostrou o custo da diferença: a viabilidade de todo o mutation testing dependia de uma convenção que nada verificava.
- **Reflection puro:** zero dependência, ~30 linhas. O difícil não é a regra, é varrer o classpath sem bug.
- **ArchUnit:** embrulha a varredura + mensagens de erro + relações entre classes. Paga a partir do 2º ou 3º invariante.
- **Régua:** 1 invariante → reflection. Vários → ArchUnit. Este repo tem pelo menos 5 candidatos já escritos em `CLAUDE.md`.
- ArchUnit cobre o que reflection não cobre barato: **dependências entre pacotes, ciclos, quem acessa quem**.
- **Risco:** regra rígida demais gera allowlist até virar decorativa. Travar decisão já tomada, nunca decidir arquitetura pela ferramenta.

## Pra aprofundar

- **Fitness functions** — conceito de *Building Evolutionary Architectures* (Ford, Parsons, Kua). O framing de "arquitetura evolutiva guarda-corpos automatizados" é a origem do termo.
- **ArchUnit** — `com.tngtech.archunit:archunit-junit5`. Vale olhar as *"Library APIs"* (`Architectures.layeredArchitecture()`, `Architectures.onionArchitecture()`), que expressam arquitetura hexagonal inteira em um teste — encaixa direto no desenho deste repo.
- **Como isso conversa com o PIT** — as duas ferramentas atacam a mesma lacuna por lados opostos: ArchUnit verifica o que o código *é*, o PIT verifica se o teste *perceberia* uma quebra. Ver [`teste-mutante-e-pit.md`](teste-mutante-e-pit.md).
- **Onde a decisão deste repo está registrada:** item #8 do backlog da sprint 04 (`docs/sprints/04-instrumentacao-qualidade/backlog-s04.md`) e `docs/PENDENCIAS-TECNICAS.md` §"Convenção `*IntegrationTest` não é verificada por nada".
