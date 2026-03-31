package com.laetienda.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.lib.options.DbGroupPolicy;
import com.laetienda.model.kc.KcUser;
import com.laetienda.model.schema.DbGroup;
import com.laetienda.model.schema.ItemTypeA;
import com.laetienda.model.user.Usuario;
import com.laetienda.utils.service.api.ApiUser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SchemaTestConfiguration.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DbGroupTest {
    private static String TEST_USER1_ID;
    private static String TEST_USER1_JWT;
    private static String TEST_USER2_ID;
    private static String TEST_USER2_JWT;

    private final String clazzName = Base64.getUrlEncoder().encodeToString(ItemTypeA.class.getName().getBytes(StandardCharsets.UTF_8));

    @Autowired private Environment env;
	@Autowired private MockMvc mvc;
	@Autowired private ObjectMapper json;
    @Autowired private ApiUser apiUser;

    @Value("${api.schema.create.uri}")
    private String createUri;

    @Value("${api.schema.group.uri.findByName}")
    private String findByNameUri;

    @Value("${api.schema.find.uri}")
    private String findTestItemUri;

    @Test
    void cycle() throws Exception {
        DbGroup dbg1 = createFirstGroup();
        DbGroup dbg2 = createSecondGroup(dbg1);

//        delete(dbg);
    }

    private DbGroup createFirstGroup() throws Exception {
        DbGroup temp = new DbGroup();
        temp.setPolicy(DbGroupPolicy.MANAGE_BY_OWNER_ONLY);
        temp.setName("testDbGroup1");

        ItemTypeA item = getItem();
        item.addReaderGroup(temp);
        item.setUsername("testItem_createFirstGroup");

        //SUCCESSFUL: Create group
        MvcResult response = mvc.perform(post(createUri, clazzName)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_USER1_JWT)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(item)))
                .andExpect(status().isOk()).andReturn();
        ItemTypeA itemResult = json.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);
        assertNotNull(itemResult);
        assertNotNull(itemResult.getId());

        getItem(item.getUsername(), TEST_USER1_JWT);

        //Find group by name
        response = mvc.perform(get(findByNameUri, temp.getName())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_USER1_JWT)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        DbGroup result = json.readValue(response.getResponse().getContentAsString(), DbGroup.class);
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(temp.getName(), result.getName());
        assertEquals(result.getOwner(), TEST_USER1_ID);

        return result;
    }

    private DbGroup createSecondGroup(DbGroup dbg) throws Exception {
        DbGroup temp = new DbGroup();
        temp.setPolicy(DbGroupPolicy.MANAGE_BY_ALL);
        temp.setName("testDbGroup2");

        ItemTypeA item = getItem();
        item.addReaderGroup(temp);
        item.addReaderGroup(dbg);
        item.setUsername("testItem_createSecondGroup");

        //Create second group
        mvc.perform(post(createUri, clazzName)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_USER2_JWT)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(item)))
                .andExpect(status().isOk());

        //Find group by name
        MvcResult response = mvc.perform(get(findByNameUri, temp.getName())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_USER2_JWT)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        DbGroup result = json.readValue(response.getResponse().getContentAsString(), DbGroup.class);
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(temp.getName(), result.getName());
        assertEquals(result.getOwner(), TEST_USER2_ID);

        getItem(item.getUsername(), TEST_USER1_JWT);

        return result;
    }

    private void delete(DbGroup dbg) throws Exception {
        String address = env.getProperty("api.schema.group.uri.delete");
        assertNotNull(address);

        fail();
    }

    @Test
    void createGroupWithoutPolicy() throws Exception {
        DbGroup temp = new DbGroup();
        ItemTypeA item = getItem();
        item.addReaderGroup(temp);

        //BAD_REQUEST: Create group without policy
        mvc.perform(post(createUri,clazzName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_USER1_JWT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(item)))
        .andExpect(status().isBadRequest());
    }

    @Test
    void createWithWrongUserIdList() throws Exception {
        fail();
    }

    @Test
    void createGroupWithOwner() throws Exception{
        fail();
    }

    @Test
    void createGroupWithRepeatedName() throws Exception {
        fail();
    }

    void updateItemAfterAddingGroup() throws Exception {
        fail();
    }

    @BeforeAll
    static void setup(@Autowired ApiUser apiUser, @Autowired Environment env) throws Exception {
        Usuario user1 = new Usuario("testUser1_schemaGroup",
                "Group", "Schema", "First Test User",
                "testUser1_schemaGroup@email.com", false,
                "secretTestPassword1", "secretTestPassword1");

        Usuario user2 = new Usuario("testUser2_schemaGroup",
                "Group", "Schema", "Second Test User",
                "testUser2_schemaGroup@email.com", false,
                "secretTestPassword2", "secretTestPassword2");

        TEST_USER1_ID = addUser(user1, env, apiUser);
        TEST_USER1_JWT = apiUser.getToken(user1.getUsername(), user1.getPassword());

        TEST_USER2_ID = addUser(user2, env, apiUser);
        TEST_USER2_JWT = apiUser.getToken(user2.getUsername(), user2.getPassword());
    }

    private static String addUser(Usuario user, Environment env, ApiUser apiUser) throws Exception {
        String clientRegistrationId = env.getProperty("kc.client-registration-id.webapp");
        assertNotNull(clientRegistrationId);

        KcUser kcUser = apiUser.create(user, clientRegistrationId);
        apiUser.enable(kcUser.getId(), clientRegistrationId);
        return kcUser.getId();
    }

    @AfterAll
    static void tearDown(@Autowired ApiUser apiUser, @Autowired Environment env) {
        apiUser.delete(TEST_USER1_ID, TEST_USER1_JWT);
        apiUser.delete(TEST_USER2_ID, TEST_USER2_JWT);
    }

    private ItemTypeA getItem(){
        ItemTypeA item = new ItemTypeA();
        item.setAddress("1453 Villeray");
        item.setAge(45);
        item.setUsername("testDbGroup");

        return item;
    }

    private ItemTypeA getItem(String username, String jwtToken) throws Exception {
        Map<String, String> body = Map.of("username", username);

        MvcResult resp = mvc.perform(post(findTestItemUri, clazzName)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn();

        return  json.readValue(resp.getResponse().getContentAsString(), ItemTypeA.class);
    }
}