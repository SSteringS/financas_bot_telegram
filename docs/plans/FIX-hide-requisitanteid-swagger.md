# FIX — Esconder `@RequisitanteId` do Swagger UI

> Mini-patch. Springdoc não conhece a annotation custom `@RequisitanteId` e mostra ela como query param editável no Swagger UI. O servidor ignora o valor (o real vem do JWT), mas a UI engana. Resolver com 1 config.

---

## Pré-requisitos

- `RequisitanteId` annotation existe em `infra/security/` ✓
- `OpenApiConfig.java` existe em `infra/` ✓

---

## Arquivo modificado

**`infra/OpenApiConfig.java`** — adicionar bloco static que registra a annotation pra ignorar.

### Código

```java
package br.com.satyan.stering.saita.financasbottelegram.infra;

import br.com.satyan.stering.saita.financasbottelegram.infra.security.RequisitanteId;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(RequisitanteId.class);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        // ... bean existente, sem mudança
    }
}
```

Só adicionar o bloco `static {...}` antes do `@Bean`. Mais nada muda.

---

## Critério de aceitação

- [ ] Bloco `static` adicionado no `OpenApiConfig.java`
- [ ] `import org.springdoc.core.utils.SpringDocUtils;` adicionado
- [ ] `import br.com.satyan...infra.security.RequisitanteId;` adicionado
- [ ] `./mvnw clean test` continua verde (não quebrou nada)
- [ ] `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` sobe sem erro
- [ ] Em `http://localhost:8080/swagger-ui.html`, no endpoint `GET /api/v1/pedidos`, o campo `requisitanteId` **NÃO aparece mais** como parâmetro

---

## Status report

`docs/status/FIX-hide-requisitanteid-swagger.md`. Cobrir:
- Confirmação visual: print/cópia do "Parameters" do endpoint listar antes e depois (pode ser textual "antes tinha campo requisitanteId, depois tem só status/tipo/de/ate/busca/page/tamanho")
- Output de `./mvnw test`

Atualizar `docs/PENDENCIAS-TECNICAS.md` movendo este item da seção "Itens abertos" pra "Itens resolvidos" com referência ao commit.
