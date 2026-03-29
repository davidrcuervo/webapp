package com.laetienda.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.lib.options.DbGroupPolicy;
import com.laetienda.model.kc.KcUser;
import com.laetienda.model.schema.DbGroup;
import com.laetienda.model.user.Usuario;
import com.laetienda.utils.service.api.ApiUser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SchemaTestConfiguration.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DbGroupTest {
    private static String TEST_USER_ID;
    private static String TEST_USER_JWT;

    @Autowired private Environment env;
	@Autowired private MockMvc mvc;
	@Autowired private ObjectMapper json;
    @Autowired private ApiUser apiUser;

    @Test
    void cycle() throws Exception {
        DbGroup dbg = create();
        delete(dbg);
    }

    private DbGroup create() throws Exception {
        DbGroup temp = new DbGroup();

        String address = env.getProperty("api.schema.group.uri.create");
        assertNotNull(address);

        //UNAUTHORIZED: Create group with not service role
        mvc.perform(post(address)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(temp)))
                .andExpect(status().isUnauthorized());

        //BAD_REQUEST: Create group without policy
        mvc.perform(post(address)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_USER_JWT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(temp)))
                .andExpect(status().isBadRequest());

        //SUCCESSFUL: Create group
        temp.setPolicy(DbGroupPolicy.MANAGE_BY_OWNER_ONLY);
        MvcResult response = mvc.perform(post(address)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_USER_JWT)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(temp)))
                .andExpect(status().isOk()).andReturn();
        DbGroup result = json.readValue(response.getResponse().getContentAsString(), DbGroup.class);
        assertNotNull(result);
        assertNotNull(result.getId());

        return result;
    }

    private void delete(DbGroup dbg) throws Exception {
        String address = env.getProperty("api.schema.group.uri.delete");
        assertNotNull(address);
    }

    @Test
    void createWithWrongUserIdList() throws Exception {
        fail();
    }

    @BeforeAll
    static void setup(@Autowired ApiUser apiUser, @Autowired Environment env) throws Exception {
        Usuario temp = new Usuario("testUser_schemaGroup",
                "Group", "Schema", "Test User",
                "testUser_schemaGroup@email.com", false,
                "secretTestPassword", "secretTestPassword");

        String clientRegistrationId = env.getProperty("kc.client-registration-id.webapp");
        assertNotNull(clientRegistrationId);

        KcUser user = apiUser.create(temp, clientRegistrationId);
        TEST_USER_ID = user.getId();
        apiUser.enable(TEST_USER_ID, clientRegistrationId);

        TEST_USER_JWT = apiUser.getToken(temp.getUsername(), temp.getPassword());
    }

    @AfterAll
    static void tearDown(@Autowired ApiUser apiUser, @Autowired Environment env) {
        apiUser.delete(TEST_USER_ID,  TEST_USER_JWT);
    }
}