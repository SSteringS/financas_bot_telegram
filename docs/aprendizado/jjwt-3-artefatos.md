# jjwt — por que a lib JWT tem 3 dependências no pom

## Contexto da dúvida

Na BE-11, o `pom.xml` ganhou três dependências do `io.jsonwebtoken`: `jjwt-api`, `jjwt-impl`, e `jjwt-jackson`. Parecia redundância. A pergunta foi: por que três entradas pra mesma biblioteca?

## Resumo destilado

Não são três cópias da mesma coisa. São **três artefatos diferentes da mesma biblioteca**, cada um com função distinta:

| Artefato | Scope | Função |
|---|---|---|
| `jjwt-api` | `compile` (default) | Interfaces e classes abstratas. É o que seu código importa: `Jwts`, `JwtBuilder`, `Claims`, `Keys`, etc. |
| `jjwt-impl` | `runtime` | Implementações concretas das interfaces. Seu código nunca importa direto. |
| `jjwt-jackson` | `runtime` | Adapter pra serialização JSON via Jackson. Existe também `jjwt-gson` e `jjwt-orgjson` como alternativas. |

A separação API/impl é proposital:
1. Seu código compila contra interfaces estáveis (`jjwt-api`)
2. Implementações internas podem mudar entre versões sem te obrigar a recompilar
3. Como `jjwt-impl` está em scope `runtime`, ele nem aparece no compile classpath — você não consegue importar classes internas por engano

A escolha do serializador JSON é plugável porque a `jjwt` precisa serializar o payload (`{"sub":"1","exp":...}`) em JSON pra meter dentro do token. Como Spring Boot já usa Jackson nativamente, `jjwt-jackson` é a escolha natural — reusa o ObjectMapper que o Spring já configurou.

## Pontos-chave

- **Três artefatos, mesma biblioteca** — `groupId` igual, `artifactId` diferente
- **Você só importa de `jjwt-api`** no seu código — nunca de impl ou jackson direto
- **`runtime` scope nos dois últimos** = "preciso no classpath rodando, mas não na compilação"
- **`Jwts.builder()...build()`** internamente usa `ServiceLoader/SPI` pra encontrar a implementação registrada por `jjwt-impl`
- **Se faltar `jjwt-impl`**: compila ok, explode em runtime com `NoClassDefFoundError`
- **Se faltar `jjwt-jackson`**: explode quando for serializar payload "nenhum serializador JSON disponível"
- **Padrão idiomático** — qualquer projeto Java sério que usa jjwt declara os 3

## Pra aprofundar

- Padrão "API + Impl" — comum em libs Java (`slf4j-api` + `logback-classic` segue mesma ideia)
- Maven scope `runtime` vs `compile` vs `provided` vs `test` — entender quando usar cada
- ServiceLoader / SPI (Service Provider Interface) — o mecanismo Java que conecta API a impl em tempo de execução
- Documentação oficial: https://github.com/jwtk/jjwt#install
