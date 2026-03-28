package com.laetienda.webapp_test.module;

import com.laetienda.lib.model.AuthCredentials;
import com.laetienda.lib.service.TestRestClient;
import com.laetienda.lib.service.ToolBoxService;
import com.laetienda.model.user.Group;
import com.laetienda.model.user.GroupList;
import com.laetienda.model.user.Usuario;
import com.laetienda.model.user.UsuarioList;

import com.laetienda.utils.service.api.UserApiDeprecated;
import com.laetienda.webapp_test.service.GroupTestService;
import com.laetienda.webapp_test.service.UserTestService;
import org.apache.tomcat.util.codec.binary.Base64;
import org.jasypt.encryption.StringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public class UserModuleImplementation implements UserModule {
    final private static String ADMUSER = "admuser";
    final private static String ADMUSER_PASSWORD = "secret";
    final private static Logger log = LoggerFactory.getLogger(UserModuleImplementation.class);
    final private static String AUTHENTICATE = "http://localhost:{port}/api/v0/user/authenticate.html";
    final private static String CREATE = "http://localhost:{port}/api/v0/user/create.html";
    final private static String DELETE = "http://localhost:{port}/api/v0/user/delete.html?username={username}";
    final private static String GROUP_CREATE = "http://localhost:{port}/api/v0/group/create.html";
    final private static String GROUP_DELETE = "http://localhost:{port}/api/v0/group/delete.html?name={gname}";
    final private static String GROUP_ADD_MEMBER = "http://localhost:{port}/api/v0/group/addMember.html?user={username}&group={gname}";
    final private static String GROUP_IS_MEMBER = "http://localhost:{port}/api/v0/group/isMember.html?group={gname}&user={username}";
    final private static String GROUP_FIND_ALL_BY_MEMBER = "http://localhost:{port}/api/v0/group/groups.html?user={username}";
    final private static String GROUP_FIND_BY_NAME = "http://localhost:{port}/api/v0/group/group.html?name={gname}";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private TestRestClient testRestTemplate;
    @Autowired private StringEncryptor jasypte;
    @Autowired private ToolBoxService tb;
    @Autowired private UserApiDeprecated userApiDeprecated;
    @Autowired private UserTestService userTest;
    @Autowired private GroupTestService groupTest;

    private int port;

    @Value("${test.api.user.emailvalidation}")
    private String urlTestApiUserEmailValidation;

    @Value("${test.api.group.ismember}")
    private String urlTestApiGroupIsMember;

    @Value("${admuser.username}")
    private String admuser;

    @Value("${admuser.hashed.password}")
    private String admuserHashedPassword;

    @Value("${admuser.password}")
    private String admuserpassword;

    @Value("${backend.username}")
    private String backendUsername;

    @Value("${backend.password}")
    private String backendPassword;

    private final String apiurl = "/api/v0/user";

    private ResponseEntity<Usuario> findByUsername(String username){
        String address = "http://localhost:{port}/api/v0/user/user.html?username={username}";
        Map<String, String> params = new HashMap<>();
        params.put("port", Integer.toString(port));
        params.put("username", username);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.add("Authorization", getEncode64("admuser", "secret"));
        HttpEntity entity = new HttpEntity(headers);

        return restTemplate.exchange(address, HttpMethod.GET, entity, Usuario.class, params);
    }

//    @BeforeEach
@Override
public void setPort(int port){
        this.port=port;
        userApiDeprecated.setPort(port);
        userTest.setPort(port);
        groupTest.setPort(port);
    }

//    @Test
    @Override
    public void testAuthentication(){

        Map<String, String> params = new HashMap<>();
        params.put("port", Integer.toString(port));

        AuthCredentials user = new AuthCredentials("admuser", "secret");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        HttpEntity<AuthCredentials> entity = new HttpEntity<>(user, headers);

        ResponseEntity<GroupList> response = restTemplate.exchange(AUTHENTICATE, HttpMethod.POST, entity, GroupList.class, params);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getGroups().size() > 0);
        response.getBody().getGroups().forEach((name, group) -> {
            log.trace("$gname: {}, $description: {}, #ofOwners: {}, #ofMembers");
        });

        //TODO
        //CREATE TEST USER
        //TEST AUTHENTICATION
        //REMOVE TEST USER
    }

//    @Test
    @Override
    public void testAuthenticationWithIvalidUsername(){
        String address = String.format("http://localhost:%d/api/v0/user/authenticate.html", port);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            tb.getHttpClient().post()
                    .uri(address).contentType(APPLICATION_JSON).body(Map.ofEntries(
                            entry("username", admuser),
                            entry("password", "incorrectpassword")
                    )).retrieve().toEntity(GroupList.class);
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        exception = assertThrows(HttpClientErrorException.class, () -> {
            tb.getHttpClient().post().uri(address).body(Map.ofEntries(
                    entry("username", "invaliduser"),
                    entry("password","anypassword")
            )).retrieve().toEntity(GroupList.class);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

//    @Test
    @Override
    public void testFindAll(){
        String address = "http://localhost:{port}/api/v0/user/users.html";

        Map<String, String> params = new HashMap<>();
        params.put("port", Integer.toString(port));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.add("Authorization", getEncode64(backendUsername,backendPassword));

        HttpEntity entity = new HttpEntity<>(headers);

        ResponseEntity<UsuarioList> response = restTemplate.exchange(address, HttpMethod.GET, entity, UsuarioList.class, params);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getUsers().size() > 0);
    }

//    @Test
    @Override
    public void testFindAllUnautorized(){
        String address = "http://localhost:{port}/api/v0/user/users.html";

        Map<String, String> params = new HashMap<>();
        params.put("port", Integer.toString(port));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.add("Authorization", getEncode64("myself", "password"));

        HttpEntity entity = new HttpEntity<>(headers);

        ResponseEntity<UsuarioList> response = restTemplate.exchange(address, HttpMethod.GET, entity, UsuarioList.class, params);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Override
    public void testFindByUsername(){
//        String address = "http://localhost:{port}/api/v0/user/user.html?username={username}";
//
//        Map<String, String> params = new HashMap<>();
//        params.put("port", Integer.toString(port));
//        params.put("username", "admuser");
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(APPLICATION_JSON);
//        headers.add("Authorization", getEncode64("admuser", "secret"));

//        HttpEntity entity = new HttpEntity<>(headers);
//
//        ResponseEntity<Usuario> response = restTemplate.exchange(address, HttpMethod.GET, entity, Usuario.class, params);
        ResponseEntity<Usuario> response = userTest.findByUsername(admuser, backendUsername, backendPassword);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("admuser", response.getBody().getUsername());
    }

    @Override
    public void testFindByUsernameRoleManager(){
//        headers.add("Authorization", getEncode64("admuser", "secret"));
        Usuario user = new Usuario(
                "testFindByUsernameRoleManager",
                "test",
                "find",
                "By Role Manager",
                "testFindByUsernameRoleManager@testmail.com", false,
                "secretpassword",
                "secretpassword");

        ResponseEntity<Usuario> resp1 = userApiDeprecated.create(user);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        assertNotNull(resp1.getBody());
        assertNotNull(resp1.getBody().getEncToken());
        assertFalse(resp1.getBody().getEncToken().isBlank());

        ResponseEntity<Usuario> resp2 = ((UserApiDeprecated) userApiDeprecated.setCredentials(ADMUSER, ADMUSER_PASSWORD)).findByUsername(user.getUsername());
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        assertNotNull(resp2.getBody());
        assertNotNull(resp2.getBody().getEmail());
        assertFalse(resp2.getBody().getEmail().isBlank());
        assertEquals(user.getEmail(), resp2.getBody().getEmail());

        delete(user.getUsername(), user.getUsername(), user.getPassword());
    }

//    @Test
    @Override
    public void testFindByUsernameUnauthorized(){
        String address = "http://localhost:{port}/api/v0/user/user.html?username={username}";

        Map<String, String> params = new HashMap<>();
        params.put("port", Integer.toString(port));
        params.put("username", "admuser");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.add("Authorization", getEncode64("myself", "password"));

        HttpEntity entity = new HttpEntity<>(headers);

        ResponseEntity<Usuario> response = restTemplate.exchange(address, HttpMethod.GET, entity, Usuario.class, params);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

//    @Test
    @Override
    public void testFindByUsernameNotFound(){
//        String address = "http://localhost:{port}/api/v0/user/user.html?username={username}";
//
//        Map<String, String> params = new HashMap<>();
//        params.put("port", Integer.toString(port));
//        params.put("username", "invalidusername");
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(APPLICATION_JSON);
//        headers.add("Authorization", getEncode64("admuser", "secret"));
//
//        HttpEntity entity = new HttpEntity<>(headers);

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            ResponseEntity<Usuario> resp1 = userTest.findByUsername("invalidUsername");
        });
//        ResponseEntity<Usuario> response = restTemplate.exchange(address, HttpMethod.GET, entity, Usuario.class, params);
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

//    @Test
    @Override
    public void testUserCycle(){
        Usuario user = testUserCycleCreate("testuser");
        testUserCycleConfirmEmail(user);
        testUserCycleUpdate("testuser");
        testUserCycleResetPassword("testuser");
        testUserCycleDelete("testuser", "newsecretpassword");
    }

    private void testUserCycleDelete(String username, String password) {
        String address = String.format("http://localhost:%d/api/v0/user/delete.html?username=%s", port, username);

        ResponseEntity<Usuario> response = findByUsername(username);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response  .getBody());

        ResponseEntity<String> response1 = tb.getHttpClient(username, password).delete().uri(address).retrieve().toEntity(String.class);

        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertTrue(Boolean.valueOf(response1.getBody()));

        response = findByUsername(username);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private void testUserCycleUpdate(String username) {
        String address = "http://localhost:{port}/api/v0/user/update.html";

        ResponseEntity<Usuario> response = findByUsername(username);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Usuario user = response.getBody();
        assertEquals(username, user.getUsername());
        assertNull(user.getMiddlename());
        assertEquals("Test Surename", user.getFullName());

        user.setMiddlename("Middle");
        response = testRestTemplate.send(address, port, HttpMethod.PUT, user, Usuario.class, null, username, "secretpassword");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(username, user.getUsername());
        assertEquals("Middle", response.getBody().getMiddlename());
        assertEquals("Test Middle Surename", response.getBody().getFullName());
    }

//    @Test
    public void resetTestUserPassword(){
        testUserCycleResetPassword("testuser");
    }

    private void testUserCycleResetPassword(String username){
        String strPort = Integer.toString(port);
        RestClient httpClient = tb.getHttpClient();
        ResponseEntity<String> response = httpClient.post()
                .uri("http://localhost:" + strPort + "/api/v0/user/requestpasswordrecovery.html")
                .body(Map.ofEntries(entry("username", username)))
                .retrieve().toEntity(String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        String token = tb.encrypt(response.getBody(), System.getProperty("jasypt.encryptor.password"));

        ResponseEntity<Usuario> response2 = httpClient.post()
                .uri("http://localhost:" + strPort + "/api/v0/user/passwordrecovery.html?token=" + token)
                .body(Map.ofEntries(
                        entry("token",token),
                        entry("username", username),
                        entry("password", "newsecretpassword"),
                        entry("password2", "newsecretpassword")
                ))
                .retrieve().toEntity(Usuario.class);

        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(username, response2.getBody().getUsername());

    }

    private void testUserCycleConfirmEmail(Usuario user){
//        String encToken = tb.encrypt(user.getToken(), System.getProperty("jasypt.encryptor.password"));
//        Map<String, String> params1 = new HashMap<>();
//        params1.put("gName", "validUserAccounts");
//        params1.put("username", user.getUsername());


        //CHECK USER IS NOT IN VALID ROLE
        groupTest.isNotMember("validUserAccounts", user.getUsername(), admuser, admuserpassword);
//        ResponseEntity<String> response = testRestTemplate.send(urlTestApiGroupIsMember, port, HttpMethod.GET, null, String.class, params1, "admuser", "secret");
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertFalse(Boolean.parseBoolean(response.getBody()));

        //CONFIRM EMAIL ADDRESS
        userTest.emailValidation(user.getEncToken(), user.getUsername(), user.getPassword());
//        Map<String, String> params2 = new HashMap<>();
//        params2.put("token", user.getEncToken());
//        response = testRestTemplate.send(urlTestApiUserEmailValidation, port, HttpMethod.GET, user, String.class, params2, user.getUsername(), getUser().getPassword());
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertTrue(Boolean.parseBoolean(response.getBody()));

        //CHECK USER IS IN VALID ROLE
        groupTest.isMember("validUserAccounts", user.getUsername(), user.getUsername(), user.getPassword());
//        response = testRestTemplate.send(urlTestApiGroupIsMember, port, HttpMethod.GET, null, String.class, params1, user.getUsername(), getUser().getPassword());
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertTrue(Boolean.parseBoolean(response.getBody()));
    }


    private Usuario testUserCycleCreate(String username) {
        ResponseEntity<Usuario> response = findByUsername(username);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        Usuario user = new Usuario();
        user.setPassword("secretpassword");
        user.setPassword2("secretpassword");
        user.setUsername(username);
        user.setEmail("emailaddress@domain.com");
        user.setFirstname("Test");
        user.setLastname("Surename");

        String address = "http://localhost:{port}/api/v0/user/create.html";

        Map<String, String> params = new HashMap<>();
        params.put("port", Integer.toString(port));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);

        HttpEntity<Usuario> entity = new HttpEntity<>(user, headers);

        response = restTemplate.exchange(address, HttpMethod.POST, entity, Usuario.class, params);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(username, response.getBody().getUsername());
        Usuario result = response.getBody();

        response = findByUsername(username);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(username, response.getBody().getUsername());

        ResponseEntity<GroupList> response2 = testRestTemplate.send(AUTHENTICATE, port, HttpMethod.POST, null, GroupList.class, null, user.getUsername(), user.getPassword());
        assertEquals(HttpStatus.OK, response.getStatusCode());

        return result;
    }

//    @Test
    @Override
    public void testCreateUserRepeatedUsername(){
        String address = "http://localhost:{port}/api/v0/user/create.html";

        Usuario user = new Usuario();
        user.setUsername("admuser");
        user.setFirstname("Test");
        user.setLastname("Last");
        user.setEmail("emailaddress@domain.com");
        user.setPassword2("secretpassword");
        user.setPassword("secretpassword");

        ResponseEntity<Usuario> response = findByUsername(user.getUsername());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        response = testRestTemplate.send(address, port, HttpMethod.POST, user, Usuario.class, null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    }

//    @Test
    @Override
    public void testCreateUserRepeatedEmail(){
        String address = "http://localhost:{port}/api/v0/user/create.html";
        Usuario user = findByUsername("admuser").getBody();
        String email = user.getEmail();
        user = getUser();
        user.setEmail(email);
        ResponseEntity<Usuario> response = testRestTemplate.send(address, port, HttpMethod.POST, user, Usuario.class, null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

//    @Test
    @Override
    public void testCreateUserBadPassword(){
        String address = "http://localhost:{port}/api/v0/user/create.html";
        Usuario user = getUser();
        user.setPassword2("differentpassword");
        ResponseEntity<Usuario> response = testRestTemplate.send(address, port, HttpMethod.POST, user, Usuario.class, null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

//    @Test
    @Override
    public void testDeleteNotFound(){
        String address = "http://localhost:{port}/api/v0/user/delete.html?username={username}";
        Map<String, String> params = new HashMap<>();
        params.put("username", "novalidusername");

        ResponseEntity<String> response = testRestTemplate.send(address, port, HttpMethod.DELETE, null, String.class, params, "admuser", "secret");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

//    @Test
    @Override
    public void testDeleteUnauthorized(){
        //Create first user
        Usuario user1 = new Usuario(
                "testDeleteFirstUser",
                "First", "User", "Test Delete",
                "testDeleteFirstUser@mail.com", false,
                "secretpassword", "secretpassword"
        );
        create(user1);

        //Create second user
        Usuario user2 = new Usuario(
                "testDeleteSecondUser",
                "Second", "User", "Test Delete",
                "testDeleteSecondUser@mail.com", false,
                "secretpassword", "secretpassword"
        );
        create(user2);

        //Try to delete unauthorized
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            ((UserApiDeprecated) userApiDeprecated.setCredentials(user2.getUsername(), user2.getPassword())).delete(user1.getUsername());
        });
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        //Delete first user
        delete(user1.getUsername(), user1.getUsername(), user1.getPassword());

        //Delete second user
        delete(user2.getUsername(), user2.getUsername(), user2.getPassword());
    }

//    @Test
    @Override
    public void testDeleteAdmuser(){
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            ((UserApiDeprecated) userApiDeprecated.setCredentials( ADMUSER, ADMUSER_PASSWORD)).delete(ADMUSER);
        });
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

//    @Test
    @Override
    public void testFindApplicationProfiles(){

        RestClient httpClient = tb.getHttpClient(admuser, jasypte.decrypt(admuserHashedPassword));
        ResponseEntity<String> profiles = httpClient.get()
                .uri("http://localhost:{port}/api/v0/user/findApplicationProfiles.html", Integer.toString(port))
                .retrieve().toEntity(String.class);

        assertEquals(HttpStatus.OK, profiles.getStatusCode());
        assertNull(profiles.getBody());
    }

//    @Test
    public void testDeleteUser(){
        Map<String, String> params = new HashMap<>();

        //ADD USER
        Usuario user = getUser();
        params.put("username", user.getUsername());
        ResponseEntity<Usuario> resp1 = testRestTemplate.send(CREATE, port, HttpMethod.POST, user, Usuario.class, null, null, null);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        assertNotNull(resp1.getBody());

        //CREATE GROUP AS OWNER
        Group userGroup = new Group("testDeleteLastOwnerOfGroup", null);
        params.put("gname", userGroup.getName());
        ResponseEntity<Group> resp2 = testRestTemplate.send(GROUP_CREATE, port, HttpMethod.POST, userGroup, Group.class, null, user.getUsername(), user.getPassword());
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        assertNotNull(resp2.getBody());
        assertTrue(resp2.getBody().getOwners().containsKey(user.getUsername()));

        //DELETE USER
        ResponseEntity<String> resp3 = testRestTemplate.send(DELETE, port, HttpMethod.DELETE, null, String.class, params, user.getUsername(), user.getPassword());
        assertEquals(HttpStatus.FORBIDDEN, resp3.getStatusCode());

        //DELETE GROUP
        resp3 = testRestTemplate.send(GROUP_DELETE, port, HttpMethod.DELETE, null, String.class, params, user.getUsername(), user.getPassword());
        assertEquals(HttpStatus.OK, resp3.getStatusCode());
        assertTrue(Boolean.valueOf(resp3.getBody()));

        //CREATE GROUP AS MANAGER
        Group managerGroup = new Group("testRemoveMemberGroup", null);
        params.put("gname", managerGroup.getName());
        resp2 = testRestTemplate.send(GROUP_CREATE, port, HttpMethod.POST, managerGroup, Group.class, null, ADMUSER, ADMUSER_PASSWORD);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        assertNotNull(resp2.getBody());

        //ADD MEMBER TO GROUP
        resp2 = testRestTemplate.send(GROUP_ADD_MEMBER, port, HttpMethod.PUT, null, Group.class, params, ADMUSER, ADMUSER_PASSWORD);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        assertNotNull(resp2.getBody());
        assertTrue(resp2.getBody().getMembers().containsKey(user.getUsername()));

        //TEST USER IS MEMBER OF THE GROUP
        resp3 = testRestTemplate.send(GROUP_IS_MEMBER, port, HttpMethod.GET, null, String.class, params, user.getUsername(), user.getPassword());
        assertEquals(HttpStatus.OK, resp3.getStatusCode());
        assertTrue(Boolean.valueOf(resp3.getBody()));

        //DELETE USER
        resp3 = testRestTemplate.send(DELETE, port, HttpMethod.DELETE, null, String.class, params, user.getUsername(), user.getPassword());
        assertEquals(HttpStatus.OK, resp3.getStatusCode());
        assertTrue(Boolean.valueOf(resp3.getBody()));
        resp1 = findByUsername(user.getUsername());
        assertEquals(HttpStatus.NOT_FOUND, resp1.getStatusCode());

        //TEST USER SHOULD NOT BE MEMBER OF ANY GROUP
        ResponseEntity<GroupList> resp4 = testRestTemplate.send(GROUP_FIND_ALL_BY_MEMBER, port, HttpMethod.GET, null, GroupList.class, params, ADMUSER, ADMUSER_PASSWORD);
        assertEquals(HttpStatus.OK, resp4.getStatusCode());
        assertNotNull(resp4.getBody());
        assertFalse(resp4.getBody().getGroups().size() > 0);

        //REMOVE GROUP AS MANAGER
        resp3 = testRestTemplate.send(GROUP_DELETE, port, HttpMethod.DELETE, null, String.class, params, ADMUSER, ADMUSER_PASSWORD);
        assertEquals(HttpStatus.OK, resp3.getStatusCode());
        assertTrue(Boolean.valueOf(resp3.getBody()));
        resp2 = testRestTemplate.send(GROUP_FIND_BY_NAME, port, HttpMethod.GET, null, Group.class, params, ADMUSER, ADMUSER_PASSWORD);
        assertEquals(HttpStatus.NOT_FOUND, resp2.getStatusCode());
    }

    private String getEncode64(String username, String password){
        String creds = String.format("%s:%s", username, password);
        String result = new String(Base64.encodeBase64String(creds.getBytes()));
        return "Basic " + result;
    }

    private Usuario getUser(){
        Usuario user = new Usuario();
        user.setUsername("testuser");
        user.setFirstname("Test");
        user.setLastname("Last");
        user.setEmail("emailaddress@domain.com");
        user.setPassword2("secretpassword");
        user.setPassword("secretpassword");
        return user;
    }

    private Usuario getSecondUser(){
        Usuario user = new Usuario();
        user.setUsername("anothertestuser");
        user.setFirstname("Test");
        user.setLastname("Last");
        user.setEmail("address@domain.com");
        user.setPassword2("secretpassword");
        user.setPassword("secretpassword");
        return user;
    }

//    @Test
    @Override
    public void testApi(){
        Usuario user = new Usuario(
                "testapicreate",
                "Create",
                "Usuario",
                "Api Test",
                "testapicreate@testmail.com", false,
                "secretpassword",
                "secretpassword");

        create(user);
        delete("testapicreate", "testapicreate", "secretpassword");
    }

    public ResponseEntity<Usuario> create(Usuario user){
        ResponseEntity<Usuario> response = userApiDeprecated.create(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getEncToken());
        assertFalse(response.getBody().getEncToken().isBlank());

        testUserExists(user.getUsername());
        return response;
    }

//    @Test
    @Override
    public void deleteApiUser(){
//        testApiDelete("anothertestuser", "anothertestuser", "secretpassword");
//        testApiDelete("testuser", ADMUSER, ADMUSER_PASSWORD);
//        testApiDelete("testapicreate", ADMUSER, ADMUSER_PASSWORD);
//        testApiDelete("testDeleteFirstUser", ADMUSER, ADMUSER_PASSWORD);
//        testApiDelete("testDeleteSecondUser", ADMUSER, ADMUSER_PASSWORD);
//        testApiDelete("testFindByUsernameRoleManager", ADMUSER, ADMUSER_PASSWORD);
//        testApiDelete("testUserDeleteBySessionId", ADMUSER, ADMUSER_PASSWORD);
//        delete("junittestuser", ADMUSER, ADMUSER_PASSWORD);
//        delete("testGroupCycle", ADMUSER, ADMUSER_PASSWORD);
//        delete("memberOfTestGroupCycle", ADMUSER, ADMUSER_PASSWORD);
    }

    public void delete(String username){
        delete(username, admuser, jasypte.decrypt(admuserHashedPassword));
    }

    public void delete(String username, String loginUsername, String password){
        testUserExists(username);

        //Delete user
        ResponseEntity<String> response = ((UserApiDeprecated) userApiDeprecated.setCredentials(loginUsername,password)).delete(username);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("true", response.getBody());

        testUserDoesNotExists(username);
    }

    private void testUserExists(String username){
        ResponseEntity<Usuario> response =
                ((UserApiDeprecated) userApiDeprecated.setCredentials(admuser, jasypte.decrypt(admuserHashedPassword)))
                        .findByUsername(username);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getEmail().isBlank());
    }

    private void testUserDoesNotExists(String username){
        HttpClientErrorException exception = assertThrows( HttpClientErrorException.class, () -> {
            ((UserApiDeprecated) userApiDeprecated.setCredentials(admuser, jasypte.decrypt(admuserHashedPassword)))
                    .findByUsername(username);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

//    @Test
    @Override
    public void testCreateWithAuthenticatedUser(){
        Usuario user = new Usuario(
                "testCreateWithAuthenticatedUser",
                "Create","Withauthenticateduser","User Test",
                "testCreateWithAuthenticatedUser@mail.com", false,
                "secretpassword","secretpassword"
        );

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            ((UserApiDeprecated) userApiDeprecated.setPort(port).setCredentials(admuser, admuserpassword))
                    .create(user);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        ex = assertThrows(HttpClientErrorException.class, () -> {
            ((UserApiDeprecated) userApiDeprecated.setCredentials(admuser, admuserpassword))
                    .findByUsername(user.getUsername());
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

//    @Test
    @Override
    public void login(){
        Usuario user = new Usuario(
                "testLogin",
                "Login",null,"User Test",
                "testLogin@email.com", false,
                "secretpassword", "secretpassword"
        );

        ResponseEntity<Usuario> resp1 = userTest.create(user);
        userTest.emailValidation(resp1.getBody().getEncToken(), user.getUsername(), user.getPassword());

        ResponseEntity<String> response = userTest.login(user.getUsername(), user.getPassword());

        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        log.trace("USER_TEST::login. $cookie: {}", cookie);
        String sessionId = cookie.split(";")[0];
        log.trace("USER_TEST::login. $sessionId: {}", sessionId);

        response = userTest.login(sessionId);
        response = userTest.logout(sessionId);
        userTest.delete(user.getUsername(), user.getUsername(), user.getPassword());
    }

//    @Test
    @Override
    public void session(){
        userTest.session();
    }
}
