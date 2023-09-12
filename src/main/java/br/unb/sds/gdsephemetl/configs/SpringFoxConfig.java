package br.unb.sds.gdsephemetl.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpringFoxConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build().apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "Vigilância Baseada em Eventos - API",
                "Api para vigilância baseada em eventos, responsável pelo processo de ETL",
                "0.0.1",
                "Terms of service",
                new Contact("Sala de Situação UNB", "https://sds.unb.br/", "sds@unb.br"),
                "MIT", "https://choosealicense.com/licenses/mit/", Collections.emptyList());
    }
}
