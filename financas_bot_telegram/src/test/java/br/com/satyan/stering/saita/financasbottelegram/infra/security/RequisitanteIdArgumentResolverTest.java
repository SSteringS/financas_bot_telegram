package br.com.satyan.stering.saita.financasbottelegram.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

@ExtendWith(MockitoExtension.class)
class RequisitanteIdArgumentResolverTest {

    @InjectMocks
    private RequisitanteIdArgumentResolver resolver;

    @Mock
    private MethodParameter parameter;

    @Test
    void deveRetornarRequisitanteIdDoRequest() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute("requisitanteId", 42L);
        NativeWebRequest webRequest = new ServletWebRequest(req);

        Object result = resolver.resolveArgument(parameter, null, webRequest, null);

        assertThat(result).isEqualTo(42L);
    }

    @Test
    void deveLancarExcecaoSeAtributoAusente() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        NativeWebRequest webRequest = new ServletWebRequest(req);

        assertThatThrownBy(() -> resolver.resolveArgument(parameter, null, webRequest, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requisitanteId");
    }

    @Test
    void deveSuportarParametroAnnotadoComLong() {
        when(parameter.hasParameterAnnotation(RequisitanteId.class)).thenReturn(true);
        when(parameter.getParameterType()).thenAnswer(inv -> Long.class);

        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    void naoDeveSuportarParametroSemAnnotation() {
        when(parameter.hasParameterAnnotation(RequisitanteId.class)).thenReturn(false);

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }
}
