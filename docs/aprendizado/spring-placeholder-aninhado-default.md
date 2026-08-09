# Placeholder aninhado no Spring não herda o default do placeholder externo

## Contexto da dúvida

Deploy em prod (EC2) quebrou no boot em 2026-08-08 com:

```
Could not resolve placeholder 'whatsapp_app_secret' in value "${whatsapp_app_secret}"
  <-- "${whatsapp.app-secret:NAO_CONFIGURADO}"
```

O estranho: o código **tinha** default. `MetaSignatureValidator` injeta `@Value("${whatsapp.app-secret:NAO_CONFIGURADO}")`. O FIX-001 tinha adicionado esse default justamente pra a app subir sem os secrets do WhatsApp populados na AWS. Mesmo assim o boot morreu.

Pergunta: por que um `@Value` com default explícito falha por placeholder não resolvido?

## Resumo destilado

O default de um placeholder Spring (`${chave:default}`) só entra em ação quando **a chave não existe em nenhum `PropertySource`**. Ele **não** é um fallback pra "qualquer coisa que der errado ao resolver o valor".

No nosso caso a chave existia. `application-prod.properties` tinha:

```properties
whatsapp.app-secret=${whatsapp_app_secret}
```

A resolução acontece em duas etapas, e o default só cobre a primeira:

1. Spring resolve `whatsapp.app-secret` → **encontra**, com o valor literal `${whatsapp_app_secret}`. Achou ⇒ o default `:NAO_CONFIGURADO` é **descartado aqui e nunca mais considerado**.
2. Spring vê que o valor obtido é ele próprio um placeholder e resolve `${whatsapp_app_secret}` contra os PropertySources (aqui, o do AWS Secrets Manager). Essa chave não existe, esse placeholder **não tem default** → `IllegalArgumentException` e o boot morre.

A seta `<--` da mensagem de erro é literalmente a cadeia de resolução, lida da direita pra esquerda: "o placeholder externo levou até o interno, e o interno é que falhou".

**Correção:** o default tem que ficar no nível onde a resolução falha — o placeholder mais interno:

```properties
whatsapp.app-secret=${whatsapp_app_secret:NAO_CONFIGURADO}
```

## Pontos-chave

- `${chave:default}` significa "se **a chave** não existir". Não significa "se der ruim".
- Uma property cujo valor é outro placeholder cria uma **cadeia**. Cada elo precisa do próprio default; defaults **não se propagam** pra dentro.
- **Onde colocar o default:** no elo que pode não resolver. Com Secrets Manager / variável de ambiente, isso é quase sempre o `.properties`, **não** o `@Value` do Java.
- Ler a mensagem de erro pela seta `<--`: à esquerda o placeholder que efetivamente falhou; à direita quem o referenciou. Quem falhou é sempre o da **esquerda**.
- O `@Value` com default ainda tem valor — cobre profiles onde a property não é declarada (testes, `dev`). Manter ambos custa nada.
- **Spring aborta no primeiro bean que falha.** Ver só um placeholder no erro **não** significa que os demais resolvem — só que aquele bean foi construído antes. Corrigir um de cada vez = um deploy quebrado por vez.
- Guarda de regressão barata: teste que varre o `.properties` procurando `\$\{[^}:]+\}` (placeholder sem `:`) e falha fora de uma allow-list de secrets genuinamente obrigatórios. Roda sem AWS e mata a classe inteira de bug.

## Lição de processo

O FIX-001 escreveu no plano: *"`application-prod.properties` — **não muda**; o default do código cobre o caso de secret ausente."* Era uma **afirmação técnica não verificada**, apresentada como fato, e passou pelo Reviewer. O critério de aceitação dizia "app sobe localmente com profile prod e os secrets ausentes" — mas localmente o PropertySource do Secrets Manager nem existe, então a cadeia de dois níveis **nunca foi exercitada**. O teste validou um cenário diferente do de produção e deu falso verde.

Régua: quando o critério de aceitação é "sobe em ambiente X", reproduzir **a topologia de PropertySources de X** — ou aceitar que o gate não cobre o risco e dizer isso explicitamente.

## Pra aprofundar

- `PropertySourcesPlaceholderConfigurer` e a ordem de precedência dos `PropertySource`
- `spring.config.import` e como o Spring Cloud AWS injeta o secret como PropertySource
- `@ConfigurationProperties` + `@Validated` como alternativa ao `@Value` avulso: agrupa config por prefixo e valida no boot com mensagem de erro decente
- Ordem de criação de beans e por que ela torna erros de config não-determinísticos
