package com.laetienda.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.lib.options.DbGroupPolicy;
import com.laetienda.model.kc.KcUser;
import com.laetienda.model.schema.DbGroup;
import com.laetienda.model.schema.ItemTypeA;
import com.laetienda.model.user.TestUserDto;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SchemaTestConfiguration.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DbGroupTest {
    private static TestUserDto[] USERS;

    private final String clazzName = Base64.getUrlEncoder().encodeToString(ItemTypeA.class.getName().getBytes(StandardCharsets.UTF_8));

    private ItemTypeA[] items;
    private DbGroup[] groups;

    @Autowired private Environment env;
	@Autowired private MockMvc mvc;
	@Autowired private ObjectMapper json;
    @Autowired private ApiUser apiUser;

    @Value("${api.schema.create.uri}")
    private String createUri;

    @Value("${api.schema.group.uri.findByName}")
    private String findGroupByNameUri;

    @Value("${api.schema.find.uri}")
    private String findTestItemUri;

    private void build(int numberOfEntries, String name){

        groups = new DbGroup[numberOfEntries +1];
        items = new ItemTypeA[numberOfEntries +1];

        for(int g = 1; g < groups.length; g++){
            groups[g] = new DbGroup(String.format("testGroup_%d_%s", g, name));
        }

        for(int i=1; i < items.length; i++){
            items[i] = new ItemTypeA(
                    String.format("testItemGroup_%d_%s", i, name), 18+i,
                    String.format("1453 Villeray. Apto %d", i));
        }
    }

    @Test
    void cycle() throws Exception {
        build(2, "cycle");
        groups[1] = createFirstGroup();
        groups[2] = createSecondGroup();
        deleteEntries();
    }

    private DbGroup createFirstGroup() throws Exception {
        groups[1].setPolicy(DbGroupPolicy.MANAGE_BY_OWNER_ONLY);
        items[1].addReaderGroup(groups[1]);

        //SUCCESSFUL: Create group
        MvcResult response = mvc.perform(post(createUri, clazzName)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[1].getToken())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(items[1])))
                .andExpect(status().isOk()).andReturn();
        items[1] = json.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);
        assertNotNull(items[1]);
        assertNotNull(items[1].getId());

        getItem(items[1].getUsername(), USERS[1].getToken());

        //Find group by name
        response = mvc.perform(get(findGroupByNameUri, groups[1].getName())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[1].getToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        DbGroup result = json.readValue(response.getResponse().getContentAsString(), DbGroup.class);
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(groups[1].getName(), result.getName());
        assertEquals(result.getOwner(), USERS[1].getUserId());

        return result;
    }

    private DbGroup createSecondGroup() throws Exception {
        groups[2].setPolicy(DbGroupPolicy.MANAGE_BY_ALL);

        items[2].addReaderGroup(groups[2]);
        items[2].addReaderGroup(groups[1]);

        //Create second group
        MvcResult response = mvc.perform(post(createUri, clazzName)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[2].getToken())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(items[2])))
                .andExpect(status().isOk()).andReturn();
        items[2] = json.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);

        //Find group by name
        response = mvc.perform(get(findGroupByNameUri, groups[2].getName())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[2].getToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        DbGroup result = json.readValue(response.getResponse().getContentAsString(), DbGroup.class);
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(groups[2].getName(), result.getName());
        assertEquals(result.getOwner(), USERS[2].getUserId());

        getItem(items[2].getUsername(), USERS[1].getToken());

        return result;
    }

    private void deleteEntries() throws Exception {
        String deleteGroupUri = env.getProperty("api.schema.group.uri.delete");
        assertNotNull(deleteGroupUri);

        String deleteItemUri = env.getProperty("api.schema.deleteById.uri");
        assertNotNull(deleteItemUri);

        for(int i=1; i < groups.length; i++){
            mvc.perform(get(findGroupByNameUri, groups[i].getName())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[i].getToken())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mvc.perform(MockMvcRequestBuilders.delete(deleteGroupUri, groups[i].getId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[i].getToken()))
                    .andExpect(status().isNoContent());

            mvc.perform(get(findGroupByNameUri, groups[i].getName())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[i].getToken())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        for(int i=1; i < items.length; i++){
            Map<String, String> body = Map.of("username", items[i].getUsername());

            mvc.perform(post(findTestItemUri, clazzName)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[i].getToken())
                            .accept(MediaType.APPLICATION_JSON)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(body)))
                    .andExpect(status().isOk());

            mvc.perform(delete(deleteItemUri, items[i].getId(), clazzName)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[i].getToken()))
                    .andExpect(status().isNoContent());

            mvc.perform(post(findTestItemUri, clazzName)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[i].getToken())
                            .accept(MediaType.APPLICATION_JSON)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(body)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void createGroupWithoutPolicy() throws Exception {
        build(1, "createGroupWithoutPolicy");
        groups[1].setName("testDbGroup_createGroupWithoutPolicy");
        items[1].addReaderGroup(groups[1]);

        //BAD_REQUEST: Create group without policy
        mvc.perform(post(createUri,clazzName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[1].getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(items[1])))
        .andExpect(status().isBadRequest());
    }

    @Test
    void createWithWrongUserIdList() throws Exception {
        build(1, "createWithWrongUserIdList");
        //TODO: Try to create group with service-account. It should not be allowed
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

    @Test
    void updateItemAfterAddingGroup() throws Exception {
        fail();
    }

    @Test
    void readerMember(){
        fail();
    }

    @Test
    void editorMember(){
        fail();
    }

    @BeforeAll
    static void setup(@Autowired ApiUser apiUser, @Autowired Environment env) throws Exception {

        int numberOfUsers = 2;
        String clientRegistrationId = env.getProperty("kc.client-registration-id.webapp");
        assertNotNull(clientRegistrationId);

        USERS = new TestUserDto[numberOfUsers + 1];

        String userName = "schemaGroup";
        String[] firstName = {"First", "Second", "Third", "Forth",  "Fifth", "Sixth"};

        for(int j = 1; j <= numberOfUsers; j++) {
            String u = String.format("testUser%d_%s", j, userName);
            Usuario user = new Usuario(u,
                    firstName[j], null, userName,
                    u+"@address.com", false,
                    "secreteTestPassword"+j, "secreteTestPassword"+j
            );

            KcUser kcUser = apiUser.create(user, clientRegistrationId);
            apiUser.enable(kcUser.getId(), clientRegistrationId);
            String token = apiUser.getToken(user.getUsername(), user.getPassword());

            USERS[j] = new TestUserDto(kcUser.getId(), token);
        }
    }

    @AfterAll
    static void tearDown(@Autowired ApiUser apiUser) {

        for(int j = 1; j < USERS.length; j++) {
            apiUser.delete(USERS[j].userId, USERS[j].getToken());
        }
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