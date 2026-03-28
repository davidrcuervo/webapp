package com.laetienda.kcUser.service;

import com.laetienda.kcUser.respository.KcUserRepository;
import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.lib.service.ToolBoxService;
import com.laetienda.model.kc.KcUser;
import com.laetienda.model.user.Usuario;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CertificateRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class KcUserServiceImplementation implements KcUserService{
    final static private Logger log = LoggerFactory.getLogger(KcUserServiceImplementation.class);

    private final Keycloak keycloak;

    @Value("${api.kc.realm}")
    private String realm;

    @Autowired private KcUserRepository repo;
    @Autowired private ToolBoxService tb;

    public KcUserServiceImplementation(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    @Override
    public KcUser find() {
        log.debug("USER_SERVICE::find");
        return repo.find();
    }

    @Override
    public String getToken(MultiValueMap<String, String> creds) {
        log.debug("USER_SERVICE::getToken. $username: {}", creds.get("username"));
        return repo.getToken(creds).getAccessToken();
    }

    @Override
    public String isUsernameValid(String username) throws NotValidCustomException {
        log.debug("USER_SERVICE::isUsernameValid. $username: {}", username);
        return findByUsername(username).getId();
    }

    @Override
    public String isUserIdValid(String userId) throws NotValidCustomException {
        log.debug("USER_SERVICE::isUserIdValid. $userId: {}", userId);
        KcUser result = repo.findByUserId(userId);

        if(isUserValid(result)){
            log.trace("USER_SERVICE::isUserIdValid: TRUE. $userId: {}", result.getId());
            return result.getId();
        } else{
            return null;
        }
    }

    @Override
    public Boolean userIdExists(String userId) throws HttpStatusCodeException {
        log.debug("USER_SERVICE::userIdExists. $userId: {}", userId);
        KcUser result = repo.findByUserId(userId);
        return true;
    }

    @Override
    public String getEmailAddress(String userId) throws HttpStatusCodeException {
        log.debug("USER_SERVICE::getEmailAddress. $userId: {}", userId);
        return repo.findByUserId(userId).getEmail();
    }

    private KcUser findByUsername(String username) throws NotValidCustomException{
        log.trace("USER_SERVICE::findByUsername. $username: {}", username);
        List<KcUser> result = repo.findByUsername(username);

        if (result == null || result.isEmpty()) {
            String message = String.format("User, %s, does not exist.", username);
            throw new NotValidCustomException(message, HttpStatus.NOT_FOUND, "username");
        }

        if (result.size() > 1){
            String message = String.format("Username, %s, exists more than once", "username");
            log.error(message);
            throw new NotValidCustomException(message, HttpStatus.CONFLICT, "username");
        }

        isUserValid(result.getFirst());

        return result.getFirst();
    }

    private boolean isUserValid(KcUser user) throws HttpStatusCodeException {
        log.trace("USER_SERVICE::isUserValid");
        boolean result = true;

        if(user == null){
            String message = "Username or user id does not exist";
            log.info("USER_SERVICE::isUserValid. {}", message);
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, message);
        }

        if(!user.isEnabled()){
            String message = String.format("User, %s, is not enabled", user.getId());
            log.info("USER_SERVICE::isUserValid. {}", message);
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
        }

        if(!user.isEmailVerified() || user.getEmail() == null || user.getEmail().isBlank()){
            String message = String.format("Username, %s, does not have a verified email address", user.getUsername());
            log.error("USER_SERVICE::isUserValid. {}", message);
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
        }

        return result;
    }

    @Override
    public KcUser createUser(Usuario user) throws NotValidCustomException {
        log.debug("USER_SERVICE::createUser. $username: {}", user.getUsername());

        if(!user.getPassword().equals(user.getPassword2())){
            String message = "Passwords do not match.";
            log.warn(message);
            throw new NotValidCustomException(message, HttpStatus.BAD_REQUEST, "password");
        }

        List<KcUser> temp = repo.findByUsername(user.getUsername());
        if(temp == null || temp.isEmpty() || temp.size() == 0){
            UserRepresentation userRepresentation = getUserRepresentation(user);

            try (Response resp = keycloak.realm(realm).users().create(userRepresentation)) {

                if (resp.getStatus() != HttpStatus.CREATED.value()) {
                    String message = resp.getEntity().toString();
                    log.error("USER_SERVICE::createUser. Failed to save user in Keycloak. $code: {} - $error: {}", resp.getStatus(), message);
                    throw new NotValidCustomException(message, HttpStatus.valueOf(resp.getStatus()), "user");
                }

                return repo.findByUsername(user.getUsername()).getFirst();
            }

        }else {
            String message = String.format("Username, %s, already exists.", user.getUsername());
            log.warn("USER_SERVICE::createUser. {}", message);
            throw new NotValidCustomException(message, HttpStatus.FORBIDDEN, "username");
        }
    }

    @Override
    public KcUser enable(String userId) throws HttpStatusCodeException {
        Map<String, Object> content = new HashMap<>();
        content.put("enabled", true);
        content.put("emailVerified", true);

        repo.modify(userId, content);

        return findById(userId);
    }

    private KcUser findById(String userId) throws HttpStatusCodeException {

        KcUser result = repo.findByUserId(userId);
        return isUserValid(result) ? result : null;
    }

    private UserRepresentation getUserRepresentation(Usuario user) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(user.getUsername());
        userRepresentation.setEmail(user.getEmail());
        userRepresentation.setEmailVerified(user.isEmailVerified());
        userRepresentation.setFirstName(user.getFirstname());
        userRepresentation.setLastName(user.getLastname());

        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(user.getPassword());
        userRepresentation.setCredentials(Collections.singletonList(passwordCred));
        return userRepresentation;
    }

    @Override
    public void deleteUser(String userId) throws HttpStatusCodeException {
        log.debug("USER_SERVICE::deleteUser. $userId: {}", userId);

        String uid = SecurityContextHolder.getContext().getAuthentication().getName();;

        if(!uid.equals(userId)){
            String message = String.format("User, %s, can't remove different users.", uid);
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "User, ");
        }

        try(Response resp = keycloak.realm(realm).users().delete(userId)){

            if(resp.getStatus() != HttpStatus.NO_CONTENT.value()){
                HttpStatusCode httpStatus = HttpStatus.valueOf(resp.getStatus());
                String statusText = resp.getStatusInfo().getReasonPhrase();

                if(httpStatus.is4xxClientError()){
                    throw new HttpClientErrorException(httpStatus, statusText);
                }

                if(httpStatus.is5xxServerError()){
                    throw new HttpServerErrorException(httpStatus, statusText);
                }
            }
        }
    }
}
