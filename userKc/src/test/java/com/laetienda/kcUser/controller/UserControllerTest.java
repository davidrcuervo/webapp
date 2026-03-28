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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    private final static Logger log = LoggerFactory.getLogger(UserControllerTest.class);
    private final String userPassword = "secretPassword";

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
        mvc.perform(get(address)
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
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
        mvc.perform(get(address, username)
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
                .andExpect(status().isOk()
                );

        //Test is user exists if should return 404 not found
        mvc.perform(get(address, "invalidusername")
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
                .andExpect(status().isNotFound());

        //Test service account. Should return 404 not found
        mvc.perform(get(address, service)
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void isUserIdValid() throws Exception {
        String address = env.getProperty("api.kcUser.isUserIdValid.uri");
        assertNotNull(address);
        String userId = env.getProperty("webapp.user.test.userId");
        assertNotNull(userId);

        mvc.perform(get(address,userId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
                .andExpect(status().isOk());
    }

    @Test
    void isUserIdValidError() throws Exception {
        String address = env.getProperty("api.kcUser.isUserIdValid.uri");
        assertNotNull(address);
        String serviceUserId = env.getProperty("webapp.user.service.userId");
        assertNotNull(serviceUserId);

        mvc.perform(get(address, serviceUserId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
                .andExpect(status().isBadRequest());

        mvc.perform(get(address, "invalid-service-id")
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void findEmailAddress() throws Exception{
        String address = env.getProperty("api.kcUser.uri.findEmailAddress");
        assertNotNull(address);

        MvcResult response = mvc.perform(get(address, testUserId)
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("sub", adminUserId))
                                .authorities(new SimpleGrantedAuthority("role_service")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        log.trace("USER_TEST::findEmailAddress. $response: {}", response.getResponse().getContentAsString());
        String result = response.getResponse().getContentAsString();
        assertEquals("myself@la-etienda.com", result);

        mvc.perform(get(address, "not-valid-user-id")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("sub", testUserId))
                                .authorities(new SimpleGrantedAuthority("role_service")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        mvc.perform(get(address, "not-valid-user-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cycle() throws Exception {
        KcUser user = createUser();
        validateUser(user);
        String jwtToken = enableUser(user);
        deleteUser(user, jwtToken);
    }

    KcUser createUser() throws Exception {

        String address = env.getProperty("api.kcUser.uri.create");
        assertNotNull(address);

        String testUsername = env.getProperty("webapp.user.test.username");
        assertNotNull(testUsername);

        Usuario user = new Usuario(testUsername,
                "Test", "User", "KcUser",
                "test.kcuser@la-etienda.com", false,
                userPassword, "badPasswd");

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

        //CREATE::FORBIDDEN: Create user with same username
        mvc.perform(post(address)
                .with(jwt().authorities(new SimpleGrantedAuthority("role_service")))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(user)))
            .andExpect(status().isForbidden());

        return result;
    }

    void validateUser(KcUser user) throws Exception {

        String userExistsAddress = env.getProperty("api.kcUser.uri.userIdExists");
        assertNotNull(userExistsAddress);

        String isUserIdValidAddress = env.getProperty("api.kcUser.isUserIdValid.uri");
        assertNotNull(isUserIdValidAddress);

        //IS_USER_VALID::BAD_REQUEST. Test if user is valid should fail because email is not confirmed.
        mvc.perform(get(isUserIdValidAddress, user.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
                .andExpect(status().isBadRequest());

        //USER_EXISTS::SUCCESSFUL.
        mvc.perform(get(userExistsAddress, user.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
                .andExpect(status().isNoContent());
    }

    private String enableUser(KcUser user) throws Exception {
        String getTokenAddress = env.getProperty("api.kcUser.token.uri");
        assertNotNull(getTokenAddress);

        String address = env.getProperty("api.kcUser.uri.enable");
        assertNotNull(address);

        //GET_TOKEN::BAD_REQUEST
        MultiValueMap<String, String> credentials = new LinkedMultiValueMap<>();
        credentials.add("username", user.getUsername());
        credentials.add("password", userPassword);

        mvc.perform(post(getTokenAddress)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .params(credentials))
            .andExpect(status().isBadRequest());

        //ENABLE_USER::IS_CREATED
        mvc.perform(put(address, user.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
                .andExpect(status().isCreated());

        //GET_TOKEN::SUCCESSFUL
        MvcResult resp = mvc.perform(post(getTokenAddress)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .params(credentials))
            .andExpect(status().isOk()).andReturn();

        return resp.getResponse().getContentAsString();
    }

    void deleteUser(KcUser user, String jwtToken) throws Exception {

        //DELETE::UNAUTHORIZED: Try to remove user by using a different user account.
        String address = env.getProperty("api.kcUser.uri.delete");
        assertNotNull(address);

        mvc.perform(delete(address, user.getId())
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("sub", testUserId))))
                .andExpect(status().isUnauthorized());

        //DELETE::SUCCESS: Delete user.
        mvc.perform(delete(address, user.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        //USER_EXISTS::NOT_FOUND.
        address = env.getProperty("api.kcUser.uri.userIdExists");
        assertNotNull(address);

        mvc.perform(get(address, user.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("role_service"))))
                .andExpect(status().isNotFound());
    }
}