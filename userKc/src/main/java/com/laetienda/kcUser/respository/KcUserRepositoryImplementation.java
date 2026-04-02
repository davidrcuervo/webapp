package com.laetienda.kcUser.respository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.model.kc.KcToken;
import com.laetienda.model.kc.KcUser;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

@Repository
public class KcUserRepositoryImplementation implements KcUserRepository {
    final private static Logger log = LoggerFactory.getLogger(KcUserRepositoryImplementation.class);

    final private RestClient client;
    @Autowired private Environment env;
    @Autowired private ObjectMapper json;

    @Value("${kc.client-registration-id.etuser}")
    private String clientRegistrationId;

    KcUserRepositoryImplementation(RestClient restClient){
        this.client = restClient;
    }

    @Override
    public KcUser find() {
        String address = env.getProperty("api.kc.realm.account", "/realms/{realm}/account");
        log.trace("KC_USER_REPOSITORY::find. $address: {}", address);

        return client.get().uri(address)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(KcUser.class).getBody();
    }

    @Override
    public void create(UserRepresentation userRepresentation) throws HttpStatusCodeException {
        String address = env.getProperty("api.kc.realm.admin.user.create", "/admin/realms/{realm}/users");
        log.debug("USER_REPOSITORY::create. $address: {}", address);

        try {
            client.post().uri(address)
                    .attributes(clientRegistrationId(clientRegistrationId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsBytes(userRepresentation))
                    .retrieve().toBodilessEntity();
        } catch (JsonProcessingException e) {
            log.error("USER_REPOSITORY::create. {}", e.getMessage());
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public void delete(String userId) throws HttpStatusCodeException {
        String address = env.getProperty("api.kc.realm.admin.user.byUserId", "/admin/realms/{realm}/users/{user-id}");
        log.debug("USER_REPOSITORY::delete. $userId: {} | $address: {}", userId, address);

        client.delete().uri(address, userId)
                .attributes(clientRegistrationId(clientRegistrationId))
                .retrieve().toBodilessEntity();
    }

    @Override
    public KcToken getToken(MultiValueMap<String, String> creds) {
        String address = env.getProperty("api.kc.realm.token.uri");
        String clientId = env.getProperty("kc.user.client.id");
        String clientSecret = env.getProperty("kc.user.client.password");
        log.debug("USER_REPOSITORY::getToken. $username: {}, | $clientId: {}, | $address: {}", creds.getFirst("username"), clientId, address);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", creds.getFirst("username"));
        body.add("password", creds.getFirst("password"));

        return address == null ? null :
                client.post().uri(address)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(body)
                        .retrieve()
                        .toEntity(KcToken.class).getBody();
    }

    @Override
    public List<KcUser> findByUsername(String username) {
        String address = env.getProperty("api.kc.realm.admin.user.byUsername", "/admin/master/users");
        log.debug("USER_REPOSITORY::findByUsername. $username: {} | clientId: {} | $address: {}", username, clientRegistrationId, address);

        List<KcUser> result = client.get().uri(address, username)
                .accept(MediaType.APPLICATION_JSON)
                .attributes(clientRegistrationId(clientRegistrationId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<KcUser>>() {});
        log.trace("USER_REPOSITORY::findByUsername. $result: {}", result != null && result.isEmpty() ? "null" : result.getFirst().getFullName());
        return result;
    }

    @Override
    public KcUser findByUserId(String userId) throws HttpStatusCodeException {
        String address = env.getProperty("api.kc.realm.admin.user.byUserId", "/admin/master/users/{userId}");
        log.debug("USER_REPOSITORY::findByUserId $userId: {} | $clientId: {} | $address: {}", userId, clientRegistrationId, address);

        return client.get().uri(address, userId)
                .accept(MediaType.APPLICATION_JSON)
                .attributes(clientRegistrationId(clientRegistrationId))
                .retrieve().toEntity(KcUser.class).getBody();
    }

    @Override
    public void modify(String userId, Map<String, Object> content) throws HttpStatusCodeException {
        String address = env.getProperty("api.kc.realm.admin.user.byUserId", "/admin/master/users/{userId}");
        log.debug("USER_REPOSITORY::modify. $userId: {} | $address: {}", userId, address);

        client.put().uri(address, userId)
                .attributes(clientRegistrationId(clientRegistrationId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(content)
                .retrieve().toBodilessEntity();
    }

    @Override
    public List<KcUser> findRoleUsers(String roleName) throws HttpStatusCodeException {
        String address = env.getProperty("api.kc.realm.admin.role.users", "/admin/realms/{realm}/roles/{role-name}/users");
        log.debug("USER_REPOSITORY::findRoleUsers $roleName: {} | $address: {}", roleName, address);

        return client.get().uri(address, roleName)
                .attributes(clientRegistrationId(clientRegistrationId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().toEntity(new ParameterizedTypeReference<List<KcUser>>() {
                }).getBody();
    }
}