package br.unb.sds.gdsephemetl.configs;

import br.unb.sds.gdsephemetl.application.model.Configuracao;
import br.unb.sds.gdsephemetl.integracao.IntegradorTokenInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.time.Duration;

@Component
public class RestConfig implements RepositoryRestConfigurer {
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.exposeIdsFor(Configuracao.class);
    }

    /**
     * RestTemplate usado para ler o integrador GDS-ePHEM. Com integracao.api-token (INTEGRACAO_API_TOKEN)
     * preenchido, toda chamada leva Authorization: Bearer &lt;token&gt;.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder,
                                     @Value("${integracao.api-token:}") String integracaoApiToken) {
        var builder = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10));
        if (integracaoApiToken != null && !integracaoApiToken.isBlank()) {
            builder = builder.additionalInterceptors(new IntegradorTokenInterceptor(integracaoApiToken.trim()));
        }
        return builder.build();
    }
}
