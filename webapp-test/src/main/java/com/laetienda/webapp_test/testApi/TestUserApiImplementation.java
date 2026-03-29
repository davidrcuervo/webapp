package com.laetienda.webapp_test.testApi;

import com.laetienda.model.kc.KcUser;
import com.laetienda.model.user.Usuario;
import com.laetienda.utils.service.api.ApiUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import static org.junit.jupiter.api.Assertions.*;

@Service
public class TestUserApiImplementation implements TestUserApi {
    private final static Logger log = LoggerFactory.getLogger(TestUserApiImplementation.class);
    private final String userPassword = "secretPassword";

    @Autowired private ApiUser apiUser;

    @Value("${kc.client-registration-id.webapp}")
    private String clientRegistrationId;

    @Value("${webapp.user.test.userId}")
    private String testUserId;

    @Override
    public void cycle() throws HttpStatusCodeException, AssertionError {
        log.info("TEST_USER_API::cycle | Test starting...");

        Usuario usuario = new Usuario("testUserApiImplementation",
                "User", "Api", "Test",
                "testuserapiimplementation@mail.com", false,
                userPassword, userPassword
        );

        //SUCCESSFUL: Create new user
        KcUser user = apiUser.create(usuario, clientRegistrationId);
        assertNotNull(user.getId());
        apiUser.userIdExists(user.getId(), clientRegistrationId);

        //FORBIDDEN: Create same user twice
        HttpStatusCodeException e = assertThrows(HttpStatusCodeException.class, () -> {
            apiUser.create(usuario, clientRegistrationId);
        });
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());

        //GET_TOKEN::BAD_REQUEST.
        e =  assertThrows(HttpStatusCodeException.class, () -> {
            apiUser.getToken(user.getUsername(), userPassword);
        });
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());

        //ENABLE::SUCCESSFUL
        apiUser.enable(user.getId(), clientRegistrationId);
        String jwtToken = apiUser.getToken(user.getUsername(), userPassword);

        //SUCCESSFUL: Delete user
        ResponseEntity<Void> resp = apiUser.delete(user.getId(), jwtToken);
        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());

        //NOT FOUND: Find if user exists
        e = assertThrows(HttpStatusCodeException.class, () -> {
            apiUser.userIdExists(user.getId(), clientRegistrationId);
        });
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());

        log.info("TEST_USER_API::cycle | Test finishes successfully.");
    }

    @Override
    public void findEmailAddress() throws HttpStatusCodeException {
        log.info("TEST_API_USER::findEmailAddress | Test starting...");

        HttpStatusCodeException e = assertThrows(HttpStatusCodeException.class, () -> {
           apiUser.getEmailAddress(testUserId);
        });
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());

        String emailAddress = apiUser.getEmailAddress(testUserId, clientRegistrationId);
        log.trace("TEST_API_USER::findEmailAddress. $emailAddress : {}", emailAddress);

        log.info("TEST_API_USER::findEmailAddress | Test finished successfully.");
    }
}
