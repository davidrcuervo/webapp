package com.laetienda.frontend.configuration;

import com.laetienda.lib.service.KeycloakGrantedAuthoritiesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.net.URI;
import java.util.*;

@Configuration
@EnableWebSecurity
public class FrontendKeycloakSecurityConfiguration {
    private final static Logger log = LoggerFactory.getLogger(FrontendKeycloakSecurityConfiguration.class);

    @Autowired
    private Environment env;

    @Value("${seo.home.file}")
    private String homeUri;

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy(){
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Bean
    OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository){
        OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        successHandler.setPostLogoutRedirectUri("{baseUrl}" +  homeUri);
        return successHandler;
    }

    @Bean
    public SecurityFilterChain frontendSecurityFilterChain(HttpSecurity http, OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler) throws Exception{
        String actuator = String.format("%s/*", env.getProperty("api.actuator.folder", "/actuator"));
        http.authorizeHttpRequests((requests) ->
                requests.
                        requestMatchers("/anonymous*").anonymous().
                        requestMatchers(actuator).permitAll().
                        requestMatchers("/home.html").permitAll().
                        requestMatchers("/home", "/", "/home.html", "/index", "/index.html", "/user/signup.html").permitAll().
                        requestMatchers("/bootstrap/**", "/styles/**", "/scripts/**").permitAll().
                        requestMatchers("/login*").permitAll().
//                        requestMatchers("/manage/**").hasAuthority("role_manager").
                        anyRequest().authenticated()
                );
        http.csrf(csrf ->
                csrf.ignoringRequestMatchers(new AntPathRequestMatcher(actuator))
        );

        http.oauth2Login(Customizer.withDefaults())
                .logout((logout) -> {
                    logout.logoutSuccessHandler(oidcLogoutSuccessHandler);
                });

        http.oauth2Client(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public KeycloakGrantedAuthoritiesConverter kcAuthoritiesConverter(){
        return new KeycloakGrantedAuthoritiesConverter();
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        log.trace("FRONTEND_SECURITY::userAuthoritiesMapper");

        return (authorities) -> {
            Optional<OidcUserInfo> oidcUserInfo =
                    authorities.stream()
                            .filter(authority -> authority instanceof OidcUserAuthority)
                            .map(OidcUserAuthority.class::cast)
                            .map(OidcUserAuthority::getUserInfo)
                            .findFirst();

            if (oidcUserInfo.isPresent()) {
                Map<String, Object> realmAccess = oidcUserInfo.get().getClaim("realm_access");
                return kcAuthoritiesConverter().getKcRealmRoles(realmAccess);
            } else {
                log.warn("FRONTEND_SECURITY::userAuthoritiesMapper. No realm access found");
                return null;
            }
        };
    }
}
