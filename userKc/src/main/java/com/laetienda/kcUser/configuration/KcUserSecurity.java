package com.laetienda.kcUser.configuration;

import com.laetienda.lib.service.KeycloakGrantedAuthoritiesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class KcUserSecurity {
    private final static Logger log = LoggerFactory.getLogger(KcUserSecurity.class);

    @Autowired private Environment env;

    @Bean
    public SecurityFilterChain userKcSecurityFilterChain(HttpSecurity http) throws Exception {
        String userPath = env.getProperty("api.usuario.folder");
        String actuatorPath = String.format("%s/*",
                env.getProperty("api.kcUser.actuator.path", "/actuator"));

        http.authorizeHttpRequests((authorize) -> {
            authorize
                    .requestMatchers(env.getProperty("api.usuario.test.path")).permitAll()
                    .requestMatchers(env.getProperty("api.kcUser.token.path")).permitAll() //api/v0/user/token
                    .requestMatchers(env.getProperty("api.kcUser.path.create")).hasAuthority("role_service")
                    .requestMatchers(actuatorPath).permitAll() //actuator/*
//                    requestMatchers(env.getProperty("api.usuario.login.path")).permitAll().
                    .requestMatchers(env.getProperty("api.usuario.testAuthorization.path")).hasAuthority("role_manager")
                    .anyRequest().fullyAuthenticated();
        });

        http.oauth2ResourceServer((oath2) -> {
            oath2.jwt(jwtDecoder -> {
                jwtDecoder.jwtAuthenticationConverter(kcAuthoritiesConverter());
            });
        });

        http.sessionManagement(sessions -> {
            sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });

        http.csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public KeycloakGrantedAuthoritiesConverter kcAuthoritiesConverter(){
        return new KeycloakGrantedAuthoritiesConverter();
    }
}