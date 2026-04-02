package com.laetienda.kcUser.configuration;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AdminClientConfiguration {
    private final static Logger log = LoggerFactory.getLogger(AdminClientConfiguration.class);

    private final Environment env;

    @Value("${kc.user.client.password}")
    private String secret;

    public AdminClientConfiguration(Environment env) {
        this.env = env;
    }

    @Bean
    public Keycloak keycloak() {
        log.trace("CONFIGURATION_kcAdminClient. $secret: {}", secret);
        return KeycloakBuilder.builder()
                .serverUrl(env.getProperty("api.kc.url", "https://localhost:8443"))
                .realm(env.getProperty("api.kc.realm.file", "master"))
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(env.getProperty("kc.user.client.id", "frontend-client-id"))
                .clientSecret(secret)
                .build();
    }
}
