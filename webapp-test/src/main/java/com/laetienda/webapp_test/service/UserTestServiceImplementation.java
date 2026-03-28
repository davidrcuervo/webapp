package com.laetienda.webapp_test.service;

import com.laetienda.model.user.Usuario;
import com.laetienda.utils.service.api.UserApiDeprecated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.*;

public class UserTestServiceImplementation implements UserTestService {
    private final static Logger log = LoggerFactory.getLogger(UserTestServiceImplementation.class);

    @Autowired
    private UserApiDeprecated userApiDeprecated;

    @Value("${admuser.username}")
    private String admuser;

    private String port;

    @Value("${admuser.password}")
    private String admuserPassword;

    @Override
    public UserTestService setPort(String port) {
        this.port = port;
        return this;
    }

    @Override
    public UserTestService setPort(Integer port) {
        setPort(Integer.toString(port));
        return this;
    }

    @Override
    public ResponseEntity<Usuario> findByUsername(String username, String loginUsername, String password) throws HttpClientErrorException {
        ResponseEntity<Usuario> response =
                ((UserApiDeprecated) userApiDeprecated.setPort(port).setCredentials(loginUsername, password))
                .findByUsername(username);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response;
    }

    @Override
    public ResponseEntity<Usuario> findByUsername(String username) throws HttpClientErrorException {
        return findByUsername(username, admuser, admuserPassword);
    }

    private void findUserNotExists(String username){
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            findByUsername(username);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Override
    public ResponseEntity<Usuario> create(Usuario user) throws HttpClientErrorException {
        findUserNotExists(user.getUsername());

        ResponseEntity<Usuario> response = ((UserApiDeprecated) userApiDeprecated.setPort(port)).create(user);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getEncToken());
        assertFalse(response.getBody().getEncToken().isBlank());

        findByUsername(user.getUsername());
        return response;
    }

    @Override
    public ResponseEntity<String> delete(String username, String loginUsername, String password) throws HttpClientErrorException {
        findByUsername(username);
        log.debug("USER_TEST::delete $username: {}, $loginusername: {}", username, loginUsername);
        ResponseEntity<String> response =
                ((UserApiDeprecated) userApiDeprecated.setPort(port).setCredentials(loginUsername, password))
                .delete(username);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("true", response.getBody());

        findUserNotExists(username);
        return response;
    }

    @Override
    public ResponseEntity<String> delete(String username) throws HttpClientErrorException {
        return delete(username, admuser, admuserPassword);
    }

    @Override
    public ResponseEntity<String> emailValidation(String token, String loginUsername, String password) throws HttpClientErrorException {
        ResponseEntity<String> response =
                ((UserApiDeprecated) userApiDeprecated.setPort(port).setCredentials(loginUsername, password))
                .emailValidation(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(Boolean.parseBoolean(response.getBody()));
        return response;
    }

    @Override
    public ResponseEntity<String> login(String username, String password) throws HttpClientErrorException {
        ResponseEntity<String> response =
                ((UserApiDeprecated) userApiDeprecated.setPort(port).setCredentials(username, password))
                        .login();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response;
    }

    @Override
    public ResponseEntity<String> login(String sessionId) throws HttpClientErrorException {
        log.debug("USER_TEST::login. $sessionId: {}", sessionId);
        ResponseEntity<String> response =
                ((UserApiDeprecated) userApiDeprecated.setPort(port).setSessionId(sessionId))
                        .login();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response;
    }

    @Override
    public ResponseEntity<String> logout(String sessionId) throws HttpClientErrorException {
        login(sessionId); //First. test if it is possible to login and sessionId is still valid
        log.debug("USER_TEST::logout. $sessionId: {}", sessionId);

        userApiDeprecated.setPort(port);
        ResponseEntity<String> response = userApiDeprecated.logout();
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(userApiDeprecated.getSession());

        response.getHeaders().forEach((key, value) -> {
            log.trace("USER_TEST::logout. $headers.key: {}, $header.value: {}", key, value.getFirst());
        });

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            ((UserApiDeprecated) userApiDeprecated.setPort(port).setSessionId(sessionId)).login();
        });
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        userApiDeprecated.setSessionId(null);

        return response;
    }

    @Override
    public void session() throws HttpClientErrorException {
        log.debug("USER_TEST::session");
        Usuario user = new Usuario(
                "sessionTestUser",
                "Session",null,"Test User",
                "sessionTestUser@mail.com", false,
                "secretpassword", "secretpassword"
        );

        ResponseEntity<Usuario> respUser = create(user);
        emailValidation(respUser.getBody().getEncToken(), user.getUsername(), user.getPassword());

//        assertNull(userApiDeprecated.getSession());
        userApiDeprecated.setCredentials(user.getUsername(), user.getPassword());
        userApiDeprecated.startSession();
        assertNotNull(userApiDeprecated.getSession());
        assertNull(userApiDeprecated.getUsername());

        ResponseEntity<String> respLogin = userApiDeprecated.login();
        assertEquals(HttpStatus.OK, respLogin.getStatusCode());
        assertNotNull(respLogin.getBody());

        ResponseEntity<String> respLogout = userApiDeprecated.endSession();
        assertEquals(HttpStatus.NO_CONTENT, respLogout.getStatusCode());
        assertNull(userApiDeprecated.getSession());

        delete(user.getUsername(), user.getUsername(), user.getPassword());
    }
}