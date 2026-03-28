package com.laetienda.webapp_test.module;

import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.schema.DbItem;
import com.laetienda.model.schema.ItemTypeA;
import com.laetienda.model.user.Usuario;
import com.laetienda.webapp_test.service.UserTestService;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.laetienda.webapp_test.service.SchemaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaModuleImplementation implements SchemaModule {
    private final static Logger log = LoggerFactory.getLogger(SchemaModuleImplementation.class);

    @Autowired private SchemaTest schemaTest;
    @Autowired private UserTestService userTest;

    @Value("${admuser.username}")
    private String admuser;

    @Value("${admuser.password}")
    private String admuserPassword;

    @BeforeEach
    void setSchemaTest(){

    }

    @Override
    public void setPort(int port){
        schemaTest.setPort(port);
    }

    @Override
    public void cycle() throws NotValidCustomException{
        ItemTypeA item = new ItemTypeA();
        item.setAddress("1453 Villeray");
        item.setAge(43);
        item.setUsername("myself");

        schemaTest.startSession(admuser, admuserPassword);
        item = create(item);
        find(item);
        update(item);
        delete(item);
        schemaTest.endSession();
    }

    @Override
    public void login() {
        schemaTest.startSession(admuser, admuserPassword);
        schemaTest.endSession();
    }

    @Override
    public void createBadEditor() {
        schemaTest.startSession(admuser, admuserPassword);

        ItemTypeA item = new ItemTypeA("createBadEditor", 22, "7775 Des Erables");
        item.addReader("nonExistUser");

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            schemaTest.create(ItemTypeA.class, item);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        schemaTest.endSession();
    }

    @Override
    public void addReader() throws NotValidCustomException {
        ItemTypeA item = new ItemTypeA("schemaAddReader", 22, "Calle 70B # 87B - 24");
        Usuario user = new Usuario(
                "schemaAddReader",
                "Add","Reader","Schema Test",
                "schemaAddReader@mail.com", false,
                "secretpassword","secretpassword"
        );
        user = userTest.create(user).getBody();
        userTest.emailValidation(user.getEncToken(),user.getUsername(), user.getPassword());

        //create item
        schemaTest.startSession(admuser, admuserPassword);
        item = schemaTest.create(ItemTypeA.class, item).getBody();
        final long itemId = item.getId();
        schemaTest.endSession();

        //find item should fail. user is not reader of the item
        schemaTest.startSession(user.getUsername(), user.getPassword());
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            schemaTest.findById(ItemTypeA.class, itemId);
        });
        schemaTest.endSession();

        //add reader to item
        schemaTest.startSession(admuser, admuserPassword);
        item.addReader(user.getUsername());
        schemaTest.update(ItemTypeA.class, item);
        schemaTest.endSession();

        //find item. this time it should work.
        schemaTest.startSession(user.getUsername(), user.getPassword());
        schemaTest.findById(ItemTypeA.class, itemId);
        schemaTest.endSession();

        //delete user and item
        schemaTest.startSession(admuser, admuserPassword);
        schemaTest.deleteById(ItemTypeA.class, item.getId());
        schemaTest.endSession();
        userTest.delete(user.getUsername(), user.getUsername(), user.getPassword());
    }

    @Override
    public void addEditor() throws NotValidCustomException {
        Usuario user = new Usuario(
                "schemaAddEditor",
                "Add","Editor","Schema Test",
                "schemaEditorReader@mail.com", false,
                "secretpassword","secretpassword"
        );
        ItemTypeA item = new ItemTypeA("schemaAddEditor", 22, "Calle 70B # 87B - 24");
        item.addReader(user.getUsername());

        user = userTest.create(user).getBody();
        userTest.emailValidation(user.getEncToken(), user.getUsername(), user.getPassword());

        //create item
        schemaTest.startSession(admuser, admuserPassword);
        ItemTypeA itemResp = schemaTest.create(ItemTypeA.class, item).getBody();
        schemaTest.endSession();

        //try to update item. it should throw and unauthorized exception
        schemaTest.startSession(user.getUsername(), user.getPassword());
        itemResp.addEditor(user.getUsername());
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            schemaTest.update(ItemTypeA.class, itemResp);
        });
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        schemaTest.endSession();

        //add reader by using the owner
        schemaTest.startSession(admuser, admuserPassword);
        schemaTest.update(ItemTypeA.class, itemResp);
        schemaTest.endSession();

        //try to update item. this time it should work.
        schemaTest.startSession(user.getUsername(), user.getPassword());
        itemResp.setAge(43);
        itemResp.setAddress("1453 Villeray");
        ItemTypeA itemResp2 = schemaTest.update(ItemTypeA.class, itemResp).getBody();
        assertEquals("schemaAddEditor", itemResp2.getUsername());
        assertEquals(43, itemResp2.getAge());
        assertEquals("1453 Villeray", itemResp2.getAddress());
        schemaTest.deleteById(ItemTypeA.class, itemResp2.getId());
        schemaTest.endSession();

        userTest.delete(user.getUsername(), user.getUsername(), user.getPassword());
    }

    @Override
    public void removeReader() throws NotValidCustomException{
        Usuario user = new Usuario(
                "schemaRemoveReader",
                "Remove","Reader","Schema Test",
                "schemaRemoveReader@mail.com", false,
                "secretpassword","secretpassword"
        );

        user = userTest.create(user).getBody();
        userTest.emailValidation(user.getEncToken(), user.getUsername(), user.getPassword());

        ItemTypeA item = new ItemTypeA("schemaRemoveReader", 22, "Calle 70B # 87B - 24");
        item.addReader(user.getUsername());

        //create item
        schemaTest.startSession(admuser, admuserPassword);
        item = schemaTest.create(ItemTypeA.class, item).getBody();
        Long itemId = item.getId();
        schemaTest.endSession();

        //test reader has privileges
        schemaTest.startSession(user.getUsername(), user.getPassword());
        schemaTest.findById(ItemTypeA.class, item.getId());
        schemaTest.endSession();

        //Remove reader
        schemaTest.startSession(admuser, admuserPassword);
        item.removeReader(user.getUsername());
        schemaTest.update(ItemTypeA.class, item);
        schemaTest.endSession();

        //test reader can't find item
        schemaTest.startSession(user.getUsername(), user.getPassword());
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            schemaTest.findById(ItemTypeA.class, itemId);
        });
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        schemaTest.endSession();

        //delete test objects from db and ldap
        schemaTest.startSession(admuser, admuserPassword);
        schemaTest.deleteById(ItemTypeA.class, itemId);
        schemaTest.endSession();
        userTest.delete(user.getUsername(), user.getUsername(), user.getPassword());
    }

    private ItemTypeA create(ItemTypeA item) throws NotValidCustomException{
        ResponseEntity<ItemTypeA> resp = schemaTest.create(ItemTypeA.class, item);
        ItemTypeA itemResp = resp.getBody();
        assertTrue(itemResp.getId() > 0);
        assertEquals(item.getAge(), itemResp.getAge());
        assertEquals(item.getUsername(), itemResp.getUsername());
        assertEquals(item.getAddress(), itemResp.getAddress());

        return itemResp;
    }

	private void find(ItemTypeA item) throws NotValidCustomException {
		Map<String, String> body = new HashMap<String, String>();
		body.put("username", item.getUsername());
		ResponseEntity<ItemTypeA> resp = schemaTest.find(ItemTypeA.class, body);
		assertEquals(item.getId(), resp.getBody().getId());
	}

    private void update(ItemTypeA item) throws NotValidCustomException {
        assertNotNull(item.getId());
        assertTrue(item.getId() > 0);
        item.setAddress("5 Place Ville Marie");
        item.setAge(44);

        ResponseEntity<ItemTypeA> resp = schemaTest.update(ItemTypeA.class, item);

        Map<String, String> params = new HashMap<String, String>();
        params.put("username", item.getUsername());
        ItemTypeA itemResp = schemaTest.find(ItemTypeA.class, params).getBody();

        assertEquals("5 Place Ville Marie", itemResp.getAddress());
        assertEquals(44, itemResp.getAge());
    }

	private void delete(ItemTypeA item){
		Map<String, String> body = new HashMap<String, String>();
		body.put("username", item.getUsername());
		schemaTest.delete(ItemTypeA.class, body);
		schemaTest.notFound(ItemTypeA.class, body);
	}
}
