package com.laetienda.utils.service.api;

import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.kc.KcToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

@Component
public class ApiUserImplementation implements ApiUser{
    private final static Logger log = LoggerFactory.getLogger(ApiUserImplementation.class);
    private final RestClient client;

    @Autowired Environment env;
    @Value("${kc.client-id}") String webappClientId;

    public ApiUserImplementation(RestClient restClient){
        this.client = restClient;
    }

    @Override
    public String isUsernameValid(String username) throws NotValidCustomException {
        String address = env.getProperty("api.kcUser.isUsernameValid.uri", "#");
        log.debug("API_USER::isValidUser. $username: {} | $address: {}", username, address);
        try {
            return client.get().uri(address, username)
                    .attributes(clientRegistrationId(webappClientId))
                    .retrieve().toEntity(String.class).getBody();
        }catch(Exception e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public String isUserIdValid(String userId) throws NotValidCustomException {
        String address = env.getProperty("api.kcUser.isUserIdValid.uri", "#");
        log.debug("API_USER::isUserIdValid. $userId: {} | $address: {}", userId, address);

        try{
            return client.get().uri(address, userId)
                    .attributes(clientRegistrationId(webappClientId))
                    .retrieve().toEntity(String.class).getBody();
        }catch(HttpClientErrorException e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public String getToken(String username, String password) throws NotValidCustomException {
        String address = env.getProperty("api.kcUser.token.uri", "/token");
        log.debug("USER_API::getToken. $username: {} | $address: {}", username, address);

        MultiValueMap<String, String> creds = new LinkedMultiValueMap<>();
        creds.add("username",username);
        creds.add("password",password);

        try {
            ResponseEntity<String> resp = client.post().uri(address)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(creds)
                    .retrieve().toEntity(String.class);
            log.trace("API_USER::getToken. $status: {}", resp.getStatusCode());
            log.trace("API_USER::getToken. $token: {}", resp.getBody());
            return resp.getBody();

        }catch(Exception e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public String getCurrentUserId() throws NotValidCustomException{
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("API_USER::getCurrentUser. $loggedUser: {}", userId);

        String result = isUserIdValid(userId);
        log.trace("API_USER::getCurrentUser. $apiResponse: {}", result);

        return result;
    }

    @Override
    public String getEmailAddress(String userId) throws HttpStatusCodeException {
        String address = env.getProperty("api.kcUser.uri.findEmailAddress", "/findEmailAddress/{userId}");
        log.debug("API_USER::getEmailAddress. $userId: {}", userId);

        return client.get().uri(address, userId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().toEntity(String.class).getBody();
    }
}