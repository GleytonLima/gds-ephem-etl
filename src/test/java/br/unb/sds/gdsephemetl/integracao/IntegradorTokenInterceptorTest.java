package br.unb.sds.gdsephemetl.integracao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IntegradorTokenInterceptorTest {

    @Test
    @DisplayName("envia Authorization: Bearer <token> nas chamadas ao integrador")
    void enviaToken() {
        final var restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new IntegradorTokenInterceptor("token-do-etl"));
        final var server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://integrador.test/api-integracao/v1/signals?page=0&size=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-do-etl"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        restTemplate.getForObject("https://integrador.test/api-integracao/v1/signals?page=0&size=100", String.class);

        server.verify();
    }

    @Test
    @DisplayName("nao sobrescreve um Authorization ja definido na chamada")
    void naoSobrescreve() {
        final var restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new IntegradorTokenInterceptor("token-do-etl"));
        final var server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://integrador.test/x"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer outro"))
                .andRespond(withSuccess());

        final var headers = new HttpHeaders();
        headers.setBearerAuth("outro");
        restTemplate.exchange("https://integrador.test/x", HttpMethod.GET, new org.springframework.http.HttpEntity<>(headers), String.class);

        server.verify();
    }
}
