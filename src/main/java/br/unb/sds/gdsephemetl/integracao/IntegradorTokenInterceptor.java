package br.unb.sds.gdsephemetl.integracao;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Envia o token do ETL ao integrador GDS-ePHEM (header Authorization: Bearer).
 * O integrador passa a exigir token quando INTEGRACAO_AUTH_ENABLED=true; ver docs/autenticacao-api.md
 * no repositorio do integrador.
 */
public class IntegradorTokenInterceptor implements ClientHttpRequestInterceptor {
    private final String token;

    public IntegradorTokenInterceptor(String token) {
        this.token = token;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            request.getHeaders().setBearerAuth(token);
        }
        return execution.execute(request, body);
    }
}
