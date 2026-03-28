package com.laetienda.webapp_test.module;

import com.laetienda.model.user.Group;
import com.laetienda.model.user.GroupList;
import com.laetienda.model.user.Usuario;
import com.laetienda.webapp_test.service.GroupTestService;
import com.laetienda.webapp_test.service.UserTestService;
import com.laetienda.utils.service.api.GroupApi;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class GroupModuleImplementation implements GroupModule {
    final private static Logger log = LoggerFactory.getLogger(GroupModuleImplementation.class);

    private int port;

    @Value("${admuser.username}")
    private String admuser;

    @Value("${api.group.create}")
    private String uriCreateGroup;

    @Value("${api.group.addMember}")
    private String uriAddMember;

    @Value("${admuser.password}")
    private String admuserPassword;

//    @Autowired
//    private ApiClientService client;

    @Autowired
    private GroupApi groupApi;

    @Autowired
    private StringEncryptor jasypte;

    @Autowired
    private UserTestService userTest;

    @Autowired
    private GroupTestService groupTest;

@Override
public void setPort(int port){
        this.port = port;
        groupApi.setPort(port);
        groupTest.setPort(port);
        userTest.setPort(port);
    }

//    @Test
    @Override
    public void testGroupCycle(){
        Usuario user = new Usuario(
                "testGroupCycle",
                "Cycle",null,"Test Group",
                "testGroupCycle@mail.com", false,
                "secretpassword","secretpassword");

        Usuario member = new Usuario(
                "memberOfTestGroupCycle",
                "Member","Cycle","Test Group",
                "memberOfTestGroupCycle@mail.com", false,
                "secretpassword","secretpassword");

        ResponseEntity<Usuario> response = userTest.create(user);
        userTest.emailValidation(response.getBody().getEncToken(), user.getUsername(), user.getPassword());

        ResponseEntity<Usuario> response2 = userTest.create(member);
        userTest.emailValidation(response2.getBody().getEncToken(), member.getUsername(), member.getPassword());

        Group group = new Group("testGroupCycle", null);

        groupTest.create(group, user.getUsername(), user.getPassword());
        addMember(group.getName(), response2.getBody(), user.getUsername(), user.getPassword());
        addOwner(group.getName(), user.getUsername(), user.getPassword());

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            userTest.delete(user.getUsername());
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        HttpClientErrorException ex2 = assertThrows(HttpClientErrorException.class, () -> {
            groupTest.removeOwner(group.getName(), user.getUsername(), user.getUsername(), user.getPassword());
        });
        assertEquals(HttpStatus.FORBIDDEN, ex2.getStatusCode(), "Can't remove last owner of the group");

        updateDescription(group);
        groupTest.delete(group.getName(), user.getUsername(), user.getPassword());

        userTest.delete(user.getUsername());
        userTest.delete(member.getUsername());
    }

    private void addMember(String gName, Usuario member, String loginUsername, String password){
        log.trace("TEST::addMemeber. $groupname: {}, $username: {}, $loginUsername: {}", gName, member.getUsername(), password);

        groupTest.isNotMember(gName, member.getUsername(), loginUsername, password);

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            groupTest.addMember(gName, member.getUsername(), member.getUsername(), password);
        });
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        log.trace("TEST::addMember. $error: {}.", ex.getMessage());

        groupTest.addMember(gName, member.getUsername(), loginUsername, password);
        groupTest.isMember(gName, member.getUsername(), loginUsername, password);
        groupTest.removeMember(gName, member.getUsername(), loginUsername, password);
        groupTest.isNotMember(gName, member.getUsername(), loginUsername, password);
    }

    private void addOwner(String groupname, String loginUsername, String password) {
        Usuario owner = new Usuario(
                "ownerOfTestGroupCycle",
                "Owner","Cycle","Test Group",
                "OwnerOfTestGroupCycle@mail.com", false,
                "secretpassword","secretpassword");

        ResponseEntity<Usuario> response = userTest.create(owner);
        userTest.emailValidation(response.getBody().getEncToken(), owner.getUsername(), owner.getPassword());

        groupTest.isNotOwner(groupname, response.getBody().getUsername(), loginUsername, password);
        groupTest.addOwner(groupname, response.getBody().getUsername(), loginUsername, password);
        groupTest.isOwner(groupname, response.getBody().getUsername(), loginUsername, password);
        Assertions.assertTrue(groupTest.findByName(groupname, loginUsername, password).getBody().getOwners().containsKey(owner.getUsername()));

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            groupTest.removeMember(groupname, owner.getUsername(), loginUsername, password);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        groupTest.removeOwner(groupname, response.getBody().getUsername(), loginUsername, password);
        groupTest.removeMember(groupname, owner.getUsername(), loginUsername, password);
        userTest.delete(owner.getUsername());
    }

//    @Test
    @Override
    public void testChangeNameOfGroup(){
        Group group = groupTest.create(new Group("testNameOfGroup", null)).getBody();

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
           groupTest.findByName("testChangeNameOfGroup");
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        group.setName("testChangeNameOfGroup");
        groupTest.update("testNameOfGroup", group);
        groupTest.findByName("testChangeNameOfGroup");

        ex = assertThrows(HttpClientErrorException.class, () -> {
            groupTest.findByName("testNameOfGroup");
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        groupTest.delete("testChangeNameOfGroup");
    }

//    @Test
    @Override
    public void findAll(){
        Usuario user = new Usuario(
                "testFindAllManagerGroups",
                "Find","All","Group Test",
                "testFindAllManagerGroups@email.com", false,
                "secretpassword","secretpassword"
        );

        ResponseEntity<Usuario> response = userTest.create(user);
        userTest.emailValidation(response.getBody().getEncToken(), user.getUsername(), user.getPassword());

        Group group = new Group("testFindAll", null);
        groupTest.create(group, user.getUsername(), user.getPassword());

        ResponseEntity<GroupList> response2 = groupTest.findAll();
        assertEquals(3, response2.getBody().getGroups().size());
        assertTrue(response2.getBody().getGroups().containsKey(group.getName()));

        response2 = groupTest.findAll(user.getUsername(), user.getPassword());
        assertEquals(2, response2.getBody().getGroups().size());
        assertTrue(response2.getBody().getGroups().containsKey(group.getName()));

        response2.getBody().getGroups().forEach((groupname, group2) -> {
            assertTrue(group2.getMembers().containsKey(user.getUsername()));
            assertTrue(group2.getMembers().size() > 0);
            assertTrue(group2.getOwners().size() > 0);
        });

        groupTest.delete(group.getName());
        userTest.delete(user.getUsername());
    }

//    @Test
    @Override
    public void testFindAllByManager(){
        Usuario user = new Usuario(
                "testFindAllByManager",
                "Findall","Bymanager","Group Test",
                "testFindAllByManager@mail.com", false,
                "secretpassword","secretpassword"
        );
        ResponseEntity<Usuario> resp = userTest.create(user);
        userTest.emailValidation(resp.getBody().getEncToken(), user.getUsername(), user.getPassword());

        Group group = new Group("testFindAllByManager", null);
        groupTest.create(group, user.getUsername(), user.getPassword());

        ResponseEntity<GroupList> resp2 = groupTest.findAll();
        Map<String, Group> groups = resp2.getBody().getGroups();
        assertTrue(groups.containsKey(group.getName()));
        assertTrue(groups.containsKey("manager"));
        assertTrue(groups.containsKey("validUserAccounts"));

        groupTest.delete(group.getName());
        userTest.delete(user.getUsername(), user.getUsername(), user.getPassword());
    }

//    @Test
    @Override
    public void testFindByNameNotFound(){
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            groupTest.findByName("testFindByNameNotFound");
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

//    @Test
    @Override
    public void testFindByNameUnauthorized(){
        Usuario user = new Usuario(
                "testFindByNameUnauthorized",
                "Find","Unauthroized","Group Test",
                "testFindByNameUnauthorized@mail.com", false,
                "secretpassword", "secretpassword"
        );
        ResponseEntity<Usuario> resp1 = userTest.create(user);
        userTest.emailValidation(resp1.getBody().getEncToken(), user.getUsername(), user.getPassword());
        groupTest.findByName("validUserAccounts");

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            groupTest.findByName("manager", user.getUsername(), user.getPassword());
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        userTest.delete(user.getUsername());
    }

//     @Test
     @Override
     public void testCreateEmptyGroup(){
         Group group = new Group();
         HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
             groupTest.create(group);
         });
         assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
     }

//     @Test
     @Override
     public void testCreateInavalidNameGroup(){
         Group group = new Group("manager", null);

         HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
             ((GroupApi)groupApi.setPort(port).setCredentials(admuser, admuserPassword))
                     .create(group);
         });

         assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
     }

//     @Test
     @Override
     public void testCreateGroupWithInvalidMember(){
        Group group = new Group("testCreateGroupWithInvalidMember", null);
        Usuario user = new Usuario(
                "testCreateGroupWithInvalidMember",
                "Invalid","Member","Test Group",
                "testCreateGroupWithInvalidMember@mail.com", false,
                "secretpassword","secretpassword"
        );
        userTest.create(user);

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
             ((GroupApi)groupApi.setPort(port).setCredentials(user.getUsername(), user.getPassword()))
                     .create(group);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        userTest.delete(user.getUsername(), user.getUsername(), user.getPassword());

     }

    private void updateDescription(Group group){
        Group temp = groupTest.findByName(group.getName()).getBody();
        assertNull(temp.getDescription());

        temp.setDescription("This is a test group");
        temp = groupTest.update(temp.getName(), temp).getBody();
        assertNotNull(temp.getDescription());
    }

//    @Test
    @Override
    public void testRemoveInvalidGroup(){
        HttpClientErrorException ex;

        ex = assertThrows(HttpClientErrorException.class, () -> {
            ((GroupApi)groupApi.setPort(port).setCredentials(admuser, admuserPassword))
                .delete("manager");
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        ex = assertThrows(HttpClientErrorException.class, () -> {
            ((GroupApi)groupApi.setPort(port).setCredentials(admuser, admuserPassword))
                    .delete("validUserAccounts");
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        Usuario user = new Usuario(
                "testRemoveInvalidGroup",
                "Remove","InvalidGroup","Group Test",
                "testRemoveInvalidGroup@mail.com", false,
                "secretpassword","secretpassword"
        );
        ResponseEntity<Usuario> resp1 = userTest.create(user);
        userTest.emailValidation(resp1.getBody().getEncToken(), user.getUsername(), user.getPassword());

        Group group = new Group("testRemoveInvalidGroup", null);
        groupTest.create(group);
        groupTest.addMember(group.getName(),user.getUsername(),admuser,admuserPassword);

        ex = assertThrows(HttpClientErrorException.class, () -> {
            ((GroupApi)groupApi.setPort(port).setCredentials(user.getUsername(), user.getPassword()))
                    .delete("testRemoveInvalidGroup");
        });
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());

        groupTest.addOwner(group.getName(),user.getUsername(),admuser, admuserPassword);
        groupTest.delete(group.getName(),user.getUsername(),user.getPassword());
        userTest.delete(user.getUsername(),user.getUsername(),user.getPassword());
    }

//    @Test
    @Override
    public void testFindAllByMember() {
        Usuario user = new Usuario(
                "testFindAllByMember",
                "Test","FindAllByMember","Group Test",
                "testFindAllByMember@mail.com", false,
                "secretpassword", "secretpassword"
        );

        ResponseEntity<Usuario> resp1 = userTest.create(user);
        userTest.emailValidation(resp1.getBody().getEncToken(), user.getUsername(), user.getPassword());

        ResponseEntity<GroupList> resp2 = ((GroupApi)groupApi.setPort(port).setCredentials(admuser, admuserPassword))
                .findAllByMember(user.getUsername());
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        assertNotNull(resp2.getBody());
        assertTrue(resp2.getBody().getGroups().size() > 0);
        assertTrue(resp2.getBody().getGroups().containsKey("validUserAccounts"));
        userTest.delete("testFindAllByMember", user.getUsername(), user.getPassword());
    }

//    @Test
    @Override
    public void createGroupWithMembersAndOwners(){
        Usuario member = new Usuario(
                "createGroupWithMembers",
                "Create","WithMemebers","Group Test",
                "createGroupWithMembers@mail.com", false,
                "secretpassword","secretpassword");
        userTest.create(member);

        Group group = new Group("createGroupWithMembers", null);
        group.addMember(member);
        groupTest.create(group);

        Group result = groupTest.findByName(group.getName()).getBody();
        assertTrue(result.getMembers().containsKey(member.getUsername()));

        groupTest.delete(group.getName());
        userTest.delete(member.getUsername());
    }
}