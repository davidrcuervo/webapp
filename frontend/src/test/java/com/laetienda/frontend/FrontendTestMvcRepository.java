package com.laetienda.frontend;

import com.laetienda.model.user.TestUserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

@Component
public class FrontendTestMvcRepository {

    @Autowired private InMemoryClientRegistrationRepository clientRegistrationRepository;
    @Autowired private OAuth2AuthorizedClientService authorizedClientService;

    public SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor loginAs(TestUserDto user){
        ClientRegistration keycloak = clientRegistrationRepository.findByRegistrationId("keycloak");

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                user.getToken(),
                Instant.now(),
                Instant.now().plusSeconds(290));
        authorizedClientService.saveAuthorizedClient(
                new OAuth2AuthorizedClient(keycloak, user.getUserId(), token),
                new UsernamePasswordAuthenticationToken(user.getUserId(), "n/a"));

        return oidcLogin()
                .clientRegistration(keycloak)
                .idToken(t -> t.subject(user.getUserId()));
    }

}
