package br.unb.sds.gdsephemetl.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

//@Configuration
//@Profile("localhomol || local")
public class WebSecurityConfiguration {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz ->
                authz.antMatchers("/v2/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/v2/api-docs/**").permitAll()
                        .anyRequest().authenticated());
        http.oauth2ResourceServer().jwt();
        return http.build();
    }
}
