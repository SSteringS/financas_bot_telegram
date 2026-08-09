package br.com.satyan.stering.saita.financasbottelegram.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guarda de regressao sobre application-prod.properties.
 *
 * <p>Um placeholder sem default neste arquivo derruba o boot quando a chave correspondente nao
 * existe no Secrets Manager. Isso e aceitavel apenas para os secrets genuinamente obrigatorios
 * listados em {@link #SECRETS_OBRIGATORIOS}; qualquer outro precisa de default para manter o
 * deploy seguro.
 *
 * <p>Le o recurso real de producao, sem subir contexto Spring e sem acesso a AWS.
 */
class ProdPropertiesPlaceholderDefaultsTest {

    private static final String RECURSO = "application-prod.properties";

    /** Placeholder sem default: {@code ${chave}}. Um {@code :} antes do {@code }} indica default. */
    private static final Pattern PLACEHOLDER_SEM_DEFAULT = Pattern.compile("\\$\\{([^}:]+)\\}");

    /** Secrets que DEVEM derrubar o boot se faltarem — a aplicacao nao funciona sem eles. */
    private static final Set<String> SECRETS_OBRIGATORIOS = Set.of(
        "db_host",
        "db_username",
        "db_password",
        "telegram_token",
        "keystore_password",
        "admin_api_key",
        "jwt_secret"
    );

    private static final List<String> PROPERTIES_WHATSAPP = List.of(
        "whatsapp.phone-number-id",
        "whatsapp.access-token",
        "whatsapp.verify-token",
        "whatsapp.app-secret",
        "whatsapp.allowed-wa-ids"
    );

    private static Properties propriedades;

    @BeforeAll
    static void carregarRecurso() throws IOException {
        propriedades = new Properties();
        try (InputStream in =
                ProdPropertiesPlaceholderDefaultsTest.class.getClassLoader().getResourceAsStream(RECURSO)) {
            assertThat(in).as("recurso %s no classpath", RECURSO).isNotNull();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                propriedades.load(reader);
            }
        }
    }

    @Test
    void placeholdersSemDefault_apenasSecretsObrigatorios() {
        Set<String> semDefault = new LinkedHashSet<>();
        for (String chave : propriedades.stringPropertyNames()) {
            Matcher matcher = PLACEHOLDER_SEM_DEFAULT.matcher(propriedades.getProperty(chave));
            while (matcher.find()) {
                semDefault.add(matcher.group(1));
            }
        }

        assertThat(semDefault)
            .as(
                "placeholders sem default em %s derrubam o boot quando a chave falta no Secrets Manager; "
                    + "adicione ':<default>' ou justifique a inclusao em SECRETS_OBRIGATORIOS",
                RECURSO
            )
            .isSubsetOf(SECRETS_OBRIGATORIOS);
    }

    @Test
    void propriedadesWhatsApp_temDefaultNoPlaceholder() {
        for (String chave : PROPERTIES_WHATSAPP) {
            String valor = propriedades.getProperty(chave);

            assertThat(valor).as("%s declarada em %s", chave, RECURSO).isNotNull();
            assertThat(PLACEHOLDER_SEM_DEFAULT.matcher(valor).find())
                .as(
                    "%s='%s' precisa de default no placeholder — o canal WhatsApp fica inerte, "
                        + "mas o boot nao pode quebrar enquanto os secrets nao sao populados (BE-20)",
                    chave,
                    valor
                )
                .isFalse();
        }
    }
}
