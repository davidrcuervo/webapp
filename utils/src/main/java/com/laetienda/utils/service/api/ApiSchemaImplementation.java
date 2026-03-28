package com.laetienda.utils.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.schema.DbItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

@Component
public class ApiSchemaImplementation implements ApiSchema{
    private final static Logger log = LoggerFactory.getLogger(ApiSchemaImplementation.class);

    private final RestClient client;
    @Autowired private Environment env;
    @Autowired private ObjectMapper json;

    @Value("${kc.client-registration-id.webapp}")
    private String webappClientId;

    public ApiSchemaImplementation(RestClient restClient){
        this.client = restClient;
    }

    @Override
    public ResponseEntity<String> helloAll() throws HttpClientErrorException {
        return null;
    }

    @Override
    public ResponseEntity<String> helloUser() throws HttpClientErrorException {
        return null;
    }

    @Override
    public ResponseEntity<String> login() throws HttpClientErrorException {
        return null;
    }

    @Override
    public ResponseEntity<String> startSession() throws HttpClientErrorException {
        return null;
    }

    @Override
    public ResponseEntity<String> endSession() throws HttpClientErrorException {
        return null;
    }

    @Override
    public <T> ResponseEntity<T> create(Class<T> clazz, DbItem item) throws NotValidCustomException {
        String address = env.getProperty("api.schema.create.uri", "create");
        log.debug("SCHEMA_API::create. $clazz: {}", clazz.getName());

        try {
            return client.post().uri(address, getClazzName(clazz))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsBytes(item))
                    .retrieve().toEntity(clazz);
        }catch(Exception e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public <T> ResponseEntity<T> find(Class<T> clazz, Map<String, String> body) throws NotValidCustomException {
        String address = env.getProperty("api.schema.find.uri", "find");
        log.debug("SCHEMA_API::find. $clazz: {} | $address: {}", clazz.getName(), address);

        try{
            return client.post().uri(address, getClazzName(clazz))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsBytes(body))
                    .retrieve().toEntity(clazz);
        }catch (Exception e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public <T> ResponseEntity<String> isItemValid(Class<T> clazz, Long itemId) throws NotValidCustomException {
        String address = env.getProperty("api.schema.isItemValid.uri", "isItemValid/{id}?clase={clazzName}");
        log.debug("SCHEMA_API::isItemValid. $itemId: {} | $clazz: {} | $address: {}", itemId, clazz.getName(), address);

        try{
            return client.get().uri(address, itemId, getClazzName(clazz))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().toEntity(String.class);
        }catch(Exception e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public <T> ResponseEntity<T> findById(Class<T> clazz, Long id) throws NotValidCustomException {
        String address = env.getProperty("api.schema.findById.uri", "findById");
        log.debug("SCHEMA_API::findById. $id: {} | $clazz: {} | $address: {}", id, clazz.getName(), address);

        try{
            return client.get().uri(address, id.toString(), getClazzName(clazz))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().toEntity(clazz);
        }catch(Exception e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public <T> ResponseEntity<String> findByQuery(Class<T> clazz, Map<String, String> body) throws NotValidCustomException {
        String address = env.getProperty("api.schema.findByQuery.uri", "findByQuery?{clazzName}");
        log.debug("API_SCHEMA::findByQuery. $clazz: {} | $address: {}", clazz.getName(), address);

        try {
             return client.post().uri(address, getClazzName(clazz))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsBytes(body))
                    .retrieve().toEntity(String.class);
        }catch(Exception e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public <T> ResponseEntity<String> findByQueryNoJwt(Class<T> clazz, Map<String, String> body) throws NotValidCustomException {
        String address = env.getProperty("api.schema.findByQuery.uri", "findByQuery?{clazzName}");
        log.debug("API_SCHEMA::findByQueryNoJwt. $clazz: {} | $address: {}", clazz.getName(), address);

        try{
            return client.post().uri(address, getClazzName(clazz))
                    .attributes(clientRegistrationId(webappClientId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsBytes(body))
                    .retrieve().toEntity(String.class);
        }catch(Exception e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public <T> ResponseEntity<String> delete(Class<T> clazz, Map<String, String> body) throws HttpClientErrorException {
        return null;
    }

    @Override
    public <T> ResponseEntity<String> deleteById(Class<T> clazz, Long id) throws NotValidCustomException {
        String address = env.getProperty("api.schema.deleteById.uri", "delete/{id}");
        log.debug("SCHEMA_API::deleteById. $idStr: {} | $clazz: {} | $address: {}", id, clazz.getName(), address);

        try{
            return client.delete().uri(address, id, getClazzName(clazz))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().toEntity(String.class);
        }catch(Exception e){
            log.trace(e.getMessage(), e);
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public <T> ResponseEntity<T> update(Class<T> clazz, DbItem item) throws NotValidCustomException {
        String address = env.getProperty("api.schema.update.uri", "update");
        log.debug("SCHEMA_API::update. $clazz: {} | $address: {}", clazz.getName(), address);

        try{
            return client.put().uri(address, getClazzName(clazz))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsBytes(item))
                    .retrieve().toEntity(clazz);
        }catch(Exception e){
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public RestClient getRestClient() {
        return null;
    }

    @Override
    public ApiClient setSessionId(String sessionId) {
        return null;
    }

    @Override
    public ApiClient setCredentials(String loginUsername, String password) {
        return null;
    }

    @Override
    public ApiClient setPort(String port) {
        return null;
    }

    @Override
    public ApiClient setPort(Integer port) {
        return null;
    }

    @Override
    public String getPort() {
        return "";
    }

    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public String getSession() {
        return "";
    }

    @Override
    public ResponseEntity<String> startSession(String loginAddres, String logoutAddress) throws HttpClientErrorException {
        return null;
    }

    @Override
    public ResponseEntity<String> endSession(String logoutAddress) throws HttpClientErrorException {
        return null;
    }

    @Override
    public <T> String getClazzName(Class<T> clazz){
        return Base64.getUrlEncoder().encodeToString(clazz.getName().getBytes(StandardCharsets.UTF_8));
    }
}
