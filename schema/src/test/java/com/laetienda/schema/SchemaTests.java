package com.laetienda.schema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.model.schema.ItemTypeA;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SchemaTestConfiguration.class)
@AutoConfigureMockMvc
class SchemaTests {
	private final static Logger log = LoggerFactory.getLogger(SchemaTests.class);
	private final String clazzName = Base64.getUrlEncoder().encodeToString(ItemTypeA.class.getName().getBytes(StandardCharsets.UTF_8));

	@Autowired private Environment env;
	@Autowired private MockMvc mvc;
	@Autowired private ObjectMapper mapper;

	@Value("${webapp.user.test.username}")
	private String testUsername;

	@Value("${webapp.user.test.userId}")
	private String testUserId;

	@Value("${webapp.user.admin.userId}")
	private String adminUserId;

    @Value("${webapp.user.service.userId}")
    private String serviceUserId;

	@Value("${api.schema.update.uri}")
	private String updateAddress;

	@Value("${api.schema.deleteById.uri}")
	private String deleteAddress;

    @Value("${api.schema.create.uri}")
    private String createAddress;

	@Value("${api.schema.findById.uri}")
	private String findByIdAddress;

    @Value("${api.schema.find.uri}")
    private String findAddress;

//	@LocalServerPort
//	private int port;

	@Test
	void health() throws Exception {
		String address = env.getProperty("api.actuator.health.path");
		assertNotNull(address);
		mvc.perform(get(address))
				.andExpect(status().isOk());
	}

	@Test
	void login() throws Exception {
//		schemaTest.login();
		String address = env.getProperty("api.schema.login.uri");
		assertNotNull(address);
		mvc.perform(post(address).with(jwt()))
				.andExpect(status().isOk());
	}

	@Test
	void cycle() throws Exception {
//		schemaTest.cycle();
//		String clazzName = Base64.getUrlEncoder().encodeToString(ItemTypeA.class.getName().getBytes(StandardCharsets.UTF_8));
		ItemTypeA item = new ItemTypeA();
        item.setAddress("1453 Villeray");
        item.setAge(44);
        item.setUsername("myself");

		create(item, clazzName);
		item = find(item, clazzName);
		assertEquals("1453 Villeray", item.getAddress());
		item = update(item, clazzName);
		assertNotEquals("1453 Villeray", item.getAddress());
		deleteItem(item, clazzName);

	}

	private void create(ItemTypeA item, String clazzName) throws Exception{

		mvc.perform(post(createAddress, clazzName)
						.with(jwt()
								.jwt(jwt -> jwt
										.claim("preferred_username", testUsername)
										.claim("sub", testUserId)))
						.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk());
	}

	private ItemTypeA find(ItemTypeA item, String clazzName) throws Exception {
		String address = env.getProperty("api.schema.find.uri");
		assertNotNull(address);

		Map<String, String> body = new HashMap<String, String>();
		body.put("username", item.getUsername());

		MvcResult result = mvc.perform(post(address, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.content(mapper.writeValueAsBytes(body))
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address").value(item.getAddress()))
				.andReturn();

		return mapper.readValue(result.getResponse().getContentAsString(), ItemTypeA.class);
	}

    @Test
    void findNotExistentItem() throws Exception{
        Map<String, String> body = new HashMap<String, String>();
        body.put("username", "notExistentItemUsername");

        mvc.perform(post(findAddress, clazzName)
                .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
                .content(mapper.writeValueAsBytes(body))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();
    }

	private ItemTypeA update(ItemTypeA item, String clazzName) throws Exception {
		String address = env.getProperty("api.schema.update.uri");
		assertNotNull(address);

		item.setAddress("5 Place Ville Marie");
		MvcResult response = mvc.perform(put(address, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address").value("5 Place Ville Marie"))
				.andReturn();

		return mapper.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);
	}

	private void deleteItem(ItemTypeA item, String clazzName) throws Exception {
		String address = env.getProperty("api.schema.delete.uri");
		assertNotNull(address);

		Map<String, String> body = new HashMap<String, String>();
		body.put("username", item.getUsername());

		MvcResult response = mvc.perform(post(address, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(body)))
				.andExpect(status().isOk())
				.andReturn();

		assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()));

		String findAddress = env.getProperty("api.schema.find.uri");
		assertNotNull(address);

		mvc.perform(post(address, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(body)))
				.andExpect(status().isNotFound());
	}

	@Test void setEditor() throws Exception{
//		schemaTest.createBadEditor();
//		schemaTest.addEditor();

		String address = env.getProperty("api.schema.create.uri");
		assertNotNull(address);

		ItemTypeA item = new ItemTypeA("createBadEditor", 22, "7775 Des Erables");
		item.addReader(testUserId);

		//create item
		MvcResult response = mvc.perform(post(address, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk())
				.andReturn();
		item = mapper.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);

		//try to edit by not valid editor
		item.setAge(44);
		mvc.perform(put(updateAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isUnauthorized());

		//add editor
		item.addEditor(testUserId);
		item.setAge(22);
		MvcResult response2 = mvc.perform(put(updateAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk())
				.andReturn();
		item = mapper.readValue(response2.getResponse().getContentAsString(), ItemTypeA.class);

		//try to edit by valid editor
		item.setAge(42);
		mvc.perform(put(updateAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.age").value(42));

		//remove editor
		item.removeEditor(testUserId);
		mvc.perform(put(updateAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk());

		//delete by using removed editor
		mvc.perform(MockMvcRequestBuilders.delete(deleteAddress, item.getId(), clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId))))
				.andExpect(status().isUnauthorized());

		//delete by using owner editor
		mvc.perform(MockMvcRequestBuilders.delete(deleteAddress, item.getId(), clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId))))
				.andExpect(status().isOk());
	}

	@Test void setReader() throws Exception{
//		schemaTest.addReader();
		ItemTypeA item = new ItemTypeA("schemaAddReader", 22, "Calle 70B # 87B - 24");
		Map<String, String> body = new HashMap<>();
		body.put("username", item.getUsername());

		//Create item
		String createAddress = env.getProperty("api.schema.create.uri");
		assertNotNull(createAddress);
		MvcResult response = mvc.perform(post(createAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk())
				.andReturn();
		item = mapper.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);

		//Read item as test user. it should fail
		String findAddress = env.getProperty("api.schema.find.uri");
		assertNotNull(findAddress);
		mvc.perform(post(findAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(body)))
				.andExpect(status().isUnauthorized());

		//Add reader and try again
		item.addReader(testUserId);
		String updateAddress = env.getProperty("api.schema.update.uri");
		assertNotNull(updateAddress);
		mvc.perform(put(updateAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk());

		//Read item as test user again. It should work now
		mvc.perform(post(findAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(body)))
				.andExpect(status().isOk());

		//Remove reader
		//schemaTest.removeReader();
		item.removeReader(testUserId);
		mvc.perform(put(updateAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk());

		mvc.perform(post(findAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(body)))
				.andExpect(status().isUnauthorized());

		//delete item from database to clean up
		String deleteAddress = env.getProperty("api.schema.delete.uri");
		assertNotNull(deleteAddress);
		mvc.perform(post(deleteAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(body)))
				.andExpect(status().isOk());
	}

	@Test void readByBackend() throws Exception {
        ItemTypeA item = new ItemTypeA("readByBackend", 37, "1140 St. Catherine");
        mvc.perform(post(createAddress, clazzName)
                .with(jwt().jwt(jwt -> jwt.claim("sub", serviceUserId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(item)))
                .andExpect(status().isUnauthorized());

        //create test item by using test user
        MvcResult response = mvc.perform(post(createAddress, clazzName)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(item)))
                .andExpect(status().isOk())
                .andReturn();
        ItemTypeA itemResponse = mapper.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);

        //try to add service user as reader
        itemResponse.addEditor(serviceUserId);
        mvc.perform(MockMvcRequestBuilders.put(updateAddress, clazzName)
                .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(itemResponse)))
                .andExpect(status().isBadRequest());

        //remove test item
        mvc.perform(MockMvcRequestBuilders.delete(deleteAddress, itemResponse.getId(), clazzName)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId))))
                .andExpect(status().isOk());
    }

	@Test void createWithDifferentOwner() throws Exception{
        ItemTypeA item = new ItemTypeA("createWithDifferentOwner", 69, "1433 Microsoft Sql");
        item.setOwner(serviceUserId);

        mvc.perform(post(createAddress, clazzName)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(item)))
                .andExpect(status().isBadRequest());

        Map<String, String> body = new HashMap<String, String>();
        body.put("username", item.getUsername());

        mvc.perform(post(findAddress, clazzName)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", serviceUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(body))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
	}

	@Test void modifyOwner() throws Exception {
		ItemTypeA item = new ItemTypeA("modifyOwner", 19, "17 Villaluz");

		//create a test item
		MvcResult response = mvc.perform(post(createAddress, clazzName)
						.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk())
				.andReturn();
		ItemTypeA itemResponse = mapper.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);

		//test by setting wrong owner
		itemResponse.setOwner(serviceUserId);
		mvc.perform(put(updateAddress, clazzName)
						.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsBytes(itemResponse)))
				.andExpect(status().isBadRequest());

		//test by updating owner by not current owner
		itemResponse.setOwner(testUserId);
		mvc.perform(put(updateAddress, clazzName)
						.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsBytes(itemResponse)))
				.andExpect(status().isUnauthorized());

		//update owner successfully
		mvc.perform(put(updateAddress, clazzName)
						.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsBytes(itemResponse)))
				.andExpect(status().isOk());

		//remove test item
		mvc.perform(MockMvcRequestBuilders.delete(deleteAddress, itemResponse.getId(), clazzName)
						.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId))))
				.andExpect(status().isUnauthorized());

		mvc.perform(MockMvcRequestBuilders.delete(deleteAddress, itemResponse.getId(), clazzName)
						.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId))))
				.andExpect(status().isOk());
	}

	@Test void deleteUser() throws Exception {
		String address = env.getProperty("api.schema.deleteUserById.uri");
		assertNotNull(address);
		ItemTypeA item = new ItemTypeA("deleteUser", 30, "63 St. Laurent");
		item.addEditor(testUserId);
		item.addReader(testUserId);

		MvcResult response = mvc.perform(post(createAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(item)))
				.andExpect(status().isOk())
				.andReturn();
		ItemTypeA itemResponse = mapper.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);

		mvc.perform(delete(address, adminUserId)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId))))
				.andExpect(status().isUnauthorized());

		mvc.perform(delete(address, testUserId)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId))))
				.andExpect(status().isForbidden());

		itemResponse.setOwner(adminUserId);
		mvc.perform(put(updateAddress, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(itemResponse)))
				.andExpect(status().isOk());

		mvc.perform(get(findByIdAddress, itemResponse.getId(), clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readers").value(testUserId));

		mvc.perform(delete(address, testUserId)
				.with(jwt()
						.authorities(new SimpleGrantedAuthority("role_manager"))
						.jwt(jwt -> jwt
						.claim("sub", adminUserId))))
				.andExpect(status().isOk());

		mvc.perform(get(findByIdAddress, itemResponse.getId(), clazzName)
						.with(jwt().jwt(jwt -> jwt.claim("sub", testUserId))))
				.andExpect(status().isUnauthorized());

		mvc.perform(get(findByIdAddress, itemResponse.getId(), clazzName)
						.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readers").isArray())
				.andExpect(jsonPath("$.readers", hasSize(0)));

		mvc.perform(delete(deleteAddress, itemResponse.getId(), clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId))))
				.andExpect(status().isOk());
	}

    @Test
    void isItemIdValid() throws Exception{
        ItemTypeA item = new ItemTypeA("isItemValid", 33, "5 Place Ville Marie");
        String address = env.getProperty("api.schema.isItemValid.uri");
        assertNotNull(address);

        //Create item
        MvcResult response = mvc.perform(post(createAddress, clazzName)
                    .with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsBytes(item)))
                .andExpect(status().isOk())
                .andReturn();

        item = mapper.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);
        Long itemId = item.getId();

        //test if item is valid
        response = mvc.perform(get(address, itemId, clazzName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        String strId = response.getResponse().getContentAsString();
        assertEquals(itemId, Long.parseLong(strId));

        //delete item
        mvc.perform(delete(deleteAddress, itemId, clazzName)
				.with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId))))
				.andExpect(status().isOk());

        //test if item is invalid
        mvc.perform(get(address, itemId, clazzName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void serviceUserCanReadItem() throws Exception {
        ItemTypeA item = new ItemTypeA("serviceUserCanReadItem", 33, "5432 Postgres");

        MvcResult response = mvc.perform(post(createAddress, clazzName)
                .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(item)))
                .andExpect(status().isOk()).andReturn();
        item = mapper.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);
        assertNotNull(item);
        Long itemId = item.getId();

        response = mvc.perform(get(findByIdAddress, item.getId(), clazzName)
                .with(jwt().jwt(jwt -> jwt.claim("sub", serviceUserId)))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        item = mapper.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);
        assertNotNull(item);
        assertEquals(itemId, item.getId());

        mvc.perform(delete(deleteAddress, itemId, clazzName)
                .with(jwt().jwt(jwt -> jwt.claim("sub", serviceUserId)))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete(deleteAddress, itemId, clazzName)
                .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void findByQuery() throws Exception {
        String address = env.getProperty("api.schema.findByQuery.uri");
        assertNotNull(address);

        ItemTypeA item1 = new ItemTypeA("findSingleResultWithQuery", 18, "1400 Boulevard Rosemont");
        ItemTypeA item2 = new ItemTypeA("findSingleResultWithQuery2", 18, "1389 Boulevard Rosemont");
        ItemTypeA item3 = new ItemTypeA("findSingleResultWithQuery3", 18, "1269 Boulevard Rosemont");

        mvc.perform(post(createAddress, clazzName)
                .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(item1)))
                .andExpect(status().isOk()).andReturn();

        mvc.perform(post(createAddress, clazzName)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(item2)))
                .andExpect(status().isOk()).andReturn();

        MvcResult response = mvc.perform(post(createAddress, clazzName)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(item3)))
                .andExpect(status().isOk()).andReturn();
        item3 = mapper.readValue(response.getResponse().getContentAsString(), ItemTypeA.class);

        String query = String.format("SELECT i FROM %s i WHERE i.age = %d", ItemTypeA.class.getName(), 18);
        Map<String, String> params = new HashMap<String, String>();
        params.put("query", query);

        response = mvc.perform(post(address, clazzName)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(params)))
                .andExpect(status().isOk()).andReturn();
        List<ItemTypeA> items = mapper.readValue(response.getResponse().getContentAsString(), new TypeReference<List<ItemTypeA>>() {});

        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(i -> i.getUsername().equals(item1.getUsername())));
        assertTrue(items.stream().anyMatch(i -> i.getUsername().equals(item2.getUsername())));

        //delete item
        for(ItemTypeA item : items) {
            mvc.perform(delete(deleteAddress, item.getId(), clazzName)
                            .with(jwt().jwt(jwt -> jwt.claim("sub", testUserId))))
                    .andExpect(status().isOk());
        }

        mvc.perform(delete(deleteAddress, item3.getId(), clazzName)
                .with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId))))
                .andExpect(status().isOk());
    }
}