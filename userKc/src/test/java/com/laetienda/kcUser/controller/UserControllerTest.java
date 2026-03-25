package com.laetienda.kcUser.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.model.kc.KcUser;
import com.laetienda.model.user.Usuario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    private final static Logger log = LoggerFactory.getLogger(UserControllerTest.class);

    @Autowired private MockMvc mvc;
    @Autowired private Environment env;
    @Autowired private ObjectMapper json;

    @Value("${webapp.user.test.userId}")
    private String testUserId;

    @Value("${webapp.user.admin.username}")
    private String adminUserId;

    @Test
    void unrestricted() throws Exception {
        String address = env.getProperty("api.usuario.test.path"); //api/v0/user/test.html
        Assertions.assertNotNull(address);
        mvc.perform(get(address))
                .andExpect(status().isOk());
    }

    @Test
    void authentication() throws Exception {
        String address = env.getProperty("api.kcUser.login.uri"); //api/v0/user/login.html
        assertNotNull(address);
        mvc.perform(get(address).with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void authorization() throws Exception {
        String address = env.getProperty("api.usuario.testAuthorization.path"); //api/v0/user/testAuthorization.html
        assertNotNull(address);
        mvc.perform(get(address).with(jwt()
                        .authorities(new SimpleGrantedAuthority("role_manager"))))
                .andExpect(status().isOk());
    }

    @Test
    void health() throws Exception {
        String address = env.getProperty("api.kcUser.actuator.health.uri", "health");
        mvc.perform(get(address))
                .andExpect(status().isOk());
    }

    @Test
    void login() throws Exception{
        String username = env.getProperty("webapp.user.test.username", "");
        String secret = env.getProperty("webapp.user.test.password", "");
        String address = env.getProperty("api.kcUser.login.uri", "login"); //http://127.0.0.1:$8001/api/v0/user/login
        String token = getToken(username,secret);

        mvc.perform(get(address)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get(address).with(jwt()
                        .jwt(jwt -> jwt.claim("preferred_username", "testuser"))
                        .authorities(new SimpleGrantedAuthority("role_test"))))
                .andExpect(status().isOk());
    }

    @Test
    void find() throws Exception {
        String username = env.getProperty("webapp.user.test.username", "");
        String secret = env.getProperty("webapp.user.test.password", "");
        String address = env.getProperty("api.kcUser.find.uri", ""); //http://127.0.0.1:$8001/api/v0/user/find
        String token = getToken(username,secret);

        mvc.perform(get(address)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test User"))
                .andExpect(jsonPath("$.username").value("samsepi0l"));
    }

    @Test
    void token() throws Exception {
        String username = env.getProperty("webapp.user.test.username", "");
        String secret = env.getProperty("webapp.user.test.password", "");
        getToken(username, secret);
    }

    String getToken(String username, String password) throws Exception {
        String address = env.getProperty("api.kcUser.token.uri"); //http://127.0.0.1:8081/token
        assertNotNull(address);

        log.debug("KC_USER_TEST::getToken. $address: {}", address);

        MultiValueMap<String, String> creds = new LinkedMultiValueMap<>();
        creds.add("username",username);
        creds.add("password",password);

        MvcResult result = mvc.perform(post(address)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(creds))
                .andDo(response -> {
                    log.trace("KC_USER_TEST::token. $response: {}", response);
                })
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());
        log.trace("token: {}", result.getResponse().getContentAsString());
        return result.getResponse().getContentAsString();
    }

    @Test
    @WithMockUser
    void isUsernameValid() throws Exception {
        String address = env.getProperty("api.kcUser.isUsernameValid.uri");
        assertNotNull(address);
        String service = env.getProperty("spring.security.oauth2.client.registration.webapp.client-id");
        assertNotNull(service);
        service = String.format("service-account-%s", service);
        String username = env.getProperty("webapp.user.test.username");
        assertNotNull(username);

        //Test if user exists it should reply ok and id of user
        mvc.perform(get(address, username))
                .andExpect(status().isOk()
                );

        //Test is user exists if should return 404 not found
        mvc.perform(get(address, "invalidusername"))
                .andExpect(status().isNotFound());

        //Test service account. Should return 404 not found
        mvc.perform(get(address, service))
                .andExpect(status().isBadRequest());
    }

    @Test
    void isUserIdValid() throws Exception {
        String address = env.getProperty("api.kcUser.isUserIdValid.uri");
        assertNotNull(address);
        String userId = env.getProperty("webapp.user.test.userId");
        assertNotNull(userId);

        mvc.perform(get(address,userId).with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void isUserIdValidError() throws Exception {
        String address = env.getProperty("api.kcUser.isUserIdValid.uri");
        assertNotNull(address);
        String serviceUserId = env.getProperty("webapp.user.service.userId");
        assertNotNull(serviceUserId);

        mvc.perform(get(address, serviceUserId).with(jwt()))
                .andExpect(status().isBadRequest());

        mvc.perform(get(address, "invalid-service-id").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findEmailAddress() throws Exception{
        String address = env.getProperty("api.kcUser.uri.findEmailAddress");
        assertNotNull(address);

        MvcResult response = mvc.perform(get(address, testUserId)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        log.trace("USER_TEST::findEmailAddress. $response: {}", response.getResponse().getContentAsString());
        String result = response.getResponse().getContentAsString();
        assertEquals("myself@la-etienda.com", result);

        mvc.perform(get(address, "not-valid-user-id")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        mvc.perform(get(address, "not-valid-user-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser() throws Exception {

        String address = env.getProperty("api.kcUser.uri.create");
        assertNotNull(address);

        String testUsername = env.getProperty("webapp.user.test.username");
        assertNotNull(testUsername);

        Usuario user = new Usuario(testUsername,
                "Test", "User", "KcUser",
                "test.kcuser@la-etienda.com", false,
                "secretPassword", "secretPassword2");

        //CREATE::BAD_REQUEST: Create user while passwords are different
        mvc.perform(post(address)
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(user)))
                .andExpect(status().isBadRequest());

        //CREATE::FORBIDDEN: Create user by using a username that already exists.
        user.setPassword2(user.getPassword());
        mvc.perform(post(address)
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(user)))
                .andExpect(status().isForbidden());

        //CREATE::SUCCESS: Create new user.
        user.setUsername("userKcCreateUserTest");
        MvcResult response = mvc.perform(post(address)
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(user)))
                    .andExpect(status().isOk()).andReturn();
        KcUser result = json.readValue(response.getResponse().getContentAsString(), KcUser.class);
        assertNotNull(result.getId());

        //DELETE::UNAUTHORIZED: Try to remove user by using a different user account.
        address = env.getProperty("api.kcUser.uri.delete");
        assertNotNull(address);

        mvc.perform(delete(address, result.getId())
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("sub", testUserId))))
                .andExpect(status().isUnauthorized());

        //DELETE::SUCCESS: Delete user.
        mvc.perform(delete(address, result.getId())
                        .with(jwt().jwt(jwt -> jwt.claim("sub", result.getId()))))
                .andExpect(status().isNoContent());
    }
}