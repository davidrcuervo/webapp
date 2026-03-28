package com.laetienda.utils.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.kc.KcUser;
import com.laetienda.model.user.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

@Component
public class ApiUserImplementation implements ApiUser{
    private final static Logger log = LoggerFactory.getLogger(ApiUserImplementation.class);
    private final RestClient client;

    @Autowired Environment env;
    @Autowired ObjectMapper json;

    @Value("${kc.client-registration-id.webapp}") String webappClientId;

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
            log.warn("API_USER::isUserIdValid: $code: {} | $message: {}", e.getRawStatusCode(), e.getMessage());
            log.debug("API_USER::isUserIdValid", e);
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public Boolean userIdExists(String userId, String clientRegistrationId) throws HttpStatusCodeException {
        String address =  env.getProperty("api.kcUser.uri.userIdExists", "/userIdExists/{userId}");
        log.debug("API_USER::userIdExists. $userId: {} | $address: {}", userId, address);

        client.get().uri(address, userId)
                .attributes(clientRegistrationId(clientRegistrationId))
                .retrieve().toEntity(Boolean.class).getBody();
        return true;
    }

    @Override
    public KcUser create(Usuario usuario, String clientRegistrationId) throws HttpStatusCodeException {
        String address = env.getProperty("api.kcUser.uri.create", "/create");
        log.debug("API_USER::create. $address: {}", address);

        try {
            return client.post().uri(address)
                    .attributes(clientRegistrationId(clientRegistrationId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsBytes(usuario))
                    .retrieve().toEntity(KcUser.class).getBody();
        } catch (JsonProcessingException e) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    public void enable(String userId, String clientRegistrationId) throws HttpStatusCodeException {
        String address = env.getProperty("api.kcUser.uri.enable", "/enable/{userId}");
        log.debug("API_USER::enable. $userId: {} | $address: {}", userId, address);

        client.put().uri(address, userId)
                .attributes(clientRegistrationId(clientRegistrationId))
                .retrieve().toBodilessEntity();
    }

    @Override
    public ResponseEntity<Void> delete(String userId, String jwtToken) throws HttpStatusCodeException {
        String address = env.getProperty("api.kcUser.uri.delete", "/delete/{userId}");
        log.debug("API_USER::delete. $userId: {}, $address: {}", userId, address);

        return client.delete().uri(address, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .retrieve().toEntity(void.class);
    }

    @Override
    public String getToken(String username, String password) throws HttpStatusCodeException {
        String address = env.getProperty("api.kcUser.token.uri", "/token");
        log.debug("USER_API::getToken. $username: {} | $address: {}", username, address);

        MultiValueMap<String, String> credentials = new LinkedMultiValueMap<>();
        credentials.add("username",username);
        credentials.add("password",password);

        ResponseEntity<String> resp = client.post().uri(address)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(credentials)
                .retrieve().toEntity(String.class);
        log.trace("API_USER::getToken. $status: {}", resp.getStatusCode());
        log.trace("API_USER::getToken. $token: {}", resp.getBody());
        return resp.getBody();
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