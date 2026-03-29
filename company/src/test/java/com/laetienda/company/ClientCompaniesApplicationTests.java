package com.laetienda.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.lib.options.CompanyFriendStatus;
import com.laetienda.lib.options.CompanyMemberPolicy;
import com.laetienda.lib.options.CompanyMemberStatus;
import com.laetienda.model.company.Company;
import com.laetienda.model.company.Friend;
import com.laetienda.model.company.Member;
import com.laetienda.utils.service.api.ApiUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ClientCompaniesApplicationTests {
    private final static Logger log = LoggerFactory.getLogger(ClientCompaniesApplicationTests.class);

	private static String jwtTestUser;
    private static String jwtAdminUser;

	@Autowired Environment env;
	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;
	@Autowired private ApiUser apiUser;

	@Value("${api.company.create.uri}")
	private String createAddress;

	@Value("${api.company.find.uri}")
	private String findAddress;

    @Value("${api.company.delete.uri}")
    private String deleteAddress;

	@Value("${webapp.user.test.userId}")
	private String testUserId;

    @Value("${webapp.user.admin.userId}")
    private String adminUserId;

    @Value("${api.company.member.find.uri}")
    private String findMemberAddress; //api/v0/company/member/find/{companyId}/{userId}

    @Value("${api.company.isValid.uri}")
    private String isCompanyValidUri; //api/v0/company/isValid/{companyId}

//    @Value("${api.company.update.uri}") //api/v0/company/update
//    private String updateCompanyAddress;

    @Value("${api.company.member.add.uri}")
    private String addMemberAddress;

    @Value("${api.company.member.update.uri}")
    private String updateMemberAddress;

    @Value("${api.company.member.delete.uri}")
    private String deleteMemberAddress; //api/v0/company/member/delete/{memberId}

    @Value("${api.company.friend.uri.find}")
    private String findFriendAddress;

    @Value("${api.company.friend.uri.add}")
    private String addFriendAddress;

	@Test
	void health() throws Exception {
		String address = env.getProperty("api.actuator.health.path");
		assertNotNull(address);
		mvc.perform(get(address))
				.andExpect(status().isOk());
	}

	@Test
	void cycle() throws Exception{
        Company company = new Company("testCycleCompany", CompanyMemberPolicy.AUTHORIZATION_REQUIRED);
		company.setOwner(testUserId);

		Company comp = create(company);
		comp = findByName(comp.getName());
		comp = findById(comp.getId());
        Member member = addMember(comp);
        comp = updateCompany(comp);
        member = updateMember(member);
        Friend friend = sendFriendRequest(member);
        friend = acceptFriend(friend);
        friend = blockFriend(friend);
        friend = unblockFriend(friend);
        comp = companyAddManager(comp);
        //TODO: modifyCompanyOwner
        //TODO: companyRemoveManager
        deleteMember(member);
        deleteCompany(comp);
	}

    private Company create(Company company) throws Exception {

		MvcResult response = mvc.perform(post(createAddress)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsBytes(company)))
				.andExpect(status().isOk())
				.andReturn();

		return json.readValue(response.getResponse().getContentAsString(), Company.class);
	}

	private Company findById(Long id) throws Exception{
		MvcResult response = mvc.perform(get(findAddress, id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		return json.readValue(response.getResponse().getContentAsString(), Company.class);
	}

	private Company findByName(String companyName) throws Exception {
		String address = env.getProperty("api.company.findByName.uri");
		assertNotNull(address);

		MvcResult response = mvc.perform(get(address, companyName)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		return json.readValue(response.getResponse().getContentAsString(), Company.class);
	}

    private Company updateCompany(Company company) throws Exception {

        //UPDATE DESCRIPTION
        String description = "Description of the company has been added.";
        String address = env.getProperty("api.company.update.uri.description");
        assertNotNull(address);
        assertNull(company.getDescription());

//                company.setDescription(description);
//        response = mvc.perform(put(updateCompanyAddress)
//                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(json.writeValueAsBytes(company)))
//                .andExpect(status().isOk())
//                .andReturn();
//        result = json.readValue(response.getResponse().getContentAsString(), Company.class);
//        assertEquals(description, result.getDescription());

        MvcResult response = mvc.perform(post(address, company.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(description))
                .andExpect(status().isOk())
                .andReturn();

        Company result = json.readValue(response.getResponse().getContentAsString(), Company.class);
        assertEquals(description, result.getDescription());

        //CREATE COMPANY FOR TESTING UPDATES
        Company temp = new Company("updateCompany", CompanyMemberPolicy.PUBLIC);
        temp.setOwner(adminUserId);

        response = mvc.perform(post(createAddress)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(temp)))
                .andExpect(status().isOk())
                .andReturn();
        temp = json.readValue(response.getResponse().getContentAsString(), Company.class);

        //UPDATE COMPANY NAME
//      company.setName(temp.getName());
        address = env.getProperty("api.company.update.uri.name");
        assertNotNull(address);

        mvc.perform(put(address, company.getId(), temp.getName())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        company.setName(result.getName());

        //UPDATE COMPANY MEMBER POLICY
//      company.setMemberPolicy(CompanyMemberPolicy.PUBLIC);
//        address = env.getProperty("api.company.update.uri.memberPolicy");
//        assertNotNull(address);
//
//        mvc.perform(put(address, company.getId(), CompanyMemberPolicy.PUBLIC)
//                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
//                        .accept(MediaType.APPLICATION_JSON))
//                .andExpect(status().isNotFound());

        //DELETE TEMP COMPANY
        mvc.perform(delete(deleteAddress, temp.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        return result;
    }

    private void deleteCompany(Company company) throws Exception {
        assertNotNull(deleteAddress);
        assertNotNull(company);

        Long id = company.getId();

        MvcResult response = mvc.perform(get(findAddress, id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        mvc.perform(get(isCompanyValidUri, id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(delete(deleteAddress, id)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        mvc.perform(get(findAddress, id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        mvc.perform(delete(deleteAddress, id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        mvc.perform(get(isCompanyValidUri, id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private Member addMember(Company company) throws Exception{

        String tmp = env.getProperty("webapp.user.admin.userId");
        String uid = apiUser.isUserIdValid(tmp);

        // check if member does not exist
        log.trace("COMPANY_TEST::addMember. $findMemberAddress: {}", findMemberAddress);
        mvc.perform(get(findMemberAddress, company.getId(), uid)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        //add member that already exists
        mvc.perform(put(addMemberAddress, company.getId(), testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser))
                .andExpect(status().isForbidden());

        // add member request to company
        MvcResult resp = mvc.perform(put(addMemberAddress, company.getId(), uid)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        Member result = json.readValue(resp.getResponse().getContentAsString(), Member.class);
        assertEquals(CompanyMemberStatus.REQUESTED, result.getStatus());

        //check again if member exists
        resp = mvc.perform(get(findMemberAddress, company.getId(), uid)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        Member memb1 = json.readValue(resp.getResponse().getContentAsString(), Member.class);
        assertNotNull(memb1.getId());

        return result;
    }

    private Member updateMember(Member member) throws Exception{

        assertEquals(CompanyMemberStatus.REQUESTED, member.getStatus());
        member.setStatus(CompanyMemberStatus.ACCEPTED);

        mvc.perform(put(updateMemberAddress)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(member)))
                .andExpect(status().isUnauthorized());

        MvcResult response = mvc.perform(put(updateMemberAddress)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(member)))
                .andExpect(status().isOk()).andReturn();
        Member result = json.readValue(response.getResponse().getContentAsString(), Member.class);
        assertEquals(CompanyMemberStatus.ACCEPTED, result.getStatus());

        member.setUserId(testUserId);
        mvc.perform(put(updateMemberAddress)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(member)))
                .andExpect(status().isBadRequest());

        return result;
    }

    private void deleteMember(Member member) throws Exception {
        mvc.perform(get(findMemberAddress, member.getCompany().getId(), member.getUserId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(delete(deleteMemberAddress, member.getCompany().getId(), member.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser))
                .andExpect(status().isNoContent());

        mvc.perform(get(findMemberAddress, member.getCompany().getId(), member.getUserId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private Friend sendFriendRequest(Member member)throws Exception{

        Long cid = member.getCompany().getId();

        //friend/find/{companyId}/{memberUserId}/{friendUserId}
        mvc.perform(get(findFriendAddress, cid, testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser()))
                .andExpect(status().isNotFound());

        //friend/add/{companyId}/{memberUserId}/{friendUserId}
        MvcResult response = mvc.perform(put(addFriendAddress, cid, testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        Friend result = json.readValue(response.getResponse().getContentAsString(), Friend.class);
        assertEquals(CompanyFriendStatus.REQUESTED, result.getStatus());

        mvc.perform(get(findFriendAddress, cid, testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(put(addFriendAddress, cid, adminUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser))
                .andExpect(status().isForbidden());

        mvc.perform(put(addFriendAddress, cid, testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser))
                .andExpect(status().isForbidden());

        mvc.perform(get(findFriendAddress, cid, testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser))
                .andExpect(status().isForbidden());

        return result;
    }

    private Friend acceptFriend(Friend friend) throws Exception {
        String address = env.getProperty("api.company.friend.uri.accept"); //api/v0/company/friend/accept/{companyId}/{memberUserId}/{buddyUserId}
        assertNotNull(address);

        Long cid = friend.getMember().getCompany().getId();

        MvcResult response = mvc.perform(put(address, cid, testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser()))
                .andExpect(status().isOk())
                .andReturn();
        Friend result = json.readValue(response.getResponse().getContentAsString(), Friend.class);
        assertEquals(CompanyFriendStatus.ACCEPTED, result.getStatus());

        mvc.perform(put(address, cid, testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser()))
                .andExpect(status().isForbidden());

        mvc.perform(put(address, cid, "not-valid-user-id")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser()))
                .andExpect(status().isNotFound());

        return result;
    }

    private Friend blockFriend(Friend friend) throws Exception {
        String addressBlock = env.getProperty("api.company.friend.uri.block");
        assertNotNull(addressBlock);

        Long cid = friend.getMember().getCompany().getId();

        MvcResult response = mvc.perform(put(addressBlock, cid, adminUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        Friend result = json.readValue(response.getResponse().getContentAsString(), Friend.class);
        assertEquals(CompanyFriendStatus.BLOCKED_BY_RECEIVER, result.getStatus());

        mvc.perform(put(addressBlock, cid, "not-valid-user-id")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser))
                .andExpect(status().isNotFound());

        return result;
    }

    private Friend unblockFriend(Friend friend) throws Exception {
        String address = env.getProperty("api.company.friend.uri.unblock");
        assertNotNull(address);

        Long cid = friend.getMember().getCompany().getId();

        mvc.perform(put(address, cid, testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser()))
                .andExpect(status().isForbidden());

        MvcResult response = mvc.perform(put(address, cid, adminUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        Friend result = json.readValue(response.getResponse().getContentAsString(), Friend.class);
        assertEquals(CompanyFriendStatus.ACCEPTED, result.getStatus());

        mvc.perform(put(address, cid, adminUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser))
                .andExpect(status().isBadRequest());

        return result;
    }

    private Company companyAddManager(Company comp) throws Exception {
        String address = env.getProperty("api.company.manager.uri.add");
        assertNotNull(address);

        assertFalse(comp.getEditors().contains(adminUserId));

        //BAD_REQUEST: Add manager that is same owner
        mvc.perform(put(address, comp.getId(), testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser))
                .andExpect(status().isBadRequest());

        //UNAUTHORIZED: Add manager by user that is not manager
        mvc.perform(put(address, comp.getId(), adminUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser()))
                .andExpect(status().isUnauthorized());

        //SUCCESSFUL: Add manager by owner.
        MvcResult response = mvc.perform(put(address, comp.getId(), adminUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        Company result = json.readValue(response.getResponse().getContentAsString(), Company.class);
        assertTrue(result.getEditors().contains(adminUserId));

        //BAD_REQUEST: Add manager that is already manager
        mvc.perform(put(address, comp.getId(), adminUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser))
                .andExpect(status().isBadRequest());

        //FORBIDDEN: Block manager by new manager
        response = mvc.perform(get(findMemberAddress, comp.getId(), testUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTestUser)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        Member member = json.readValue(response.getResponse().getContentAsString(), Member.class);

        member.setStatus(CompanyMemberStatus.BLOCKED);
        mvc.perform(put(updateMemberAddress)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getJwtAdminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(member)))
                .andExpect(status().isForbidden());

        //TODO -.OK.- add another manager by manager (requires extra user).
        //TODO -.OK.- unblock member by third manager

        fail();
        return null;
    }

	@Test
	public void createCompanyWithRepeatedName(){
		fail();
	}

    @Test
    public void findWrongCompany(){
        fail();
    }

    public String getJwtAdminUser() throws Exception{

        if(jwtAdminUser == null){
            String username = env.getProperty("webapp.user.admin.username");
            String password = env.getProperty("webapp.user.admin.password");
            jwtAdminUser = apiUser.getToken(username, password);
        }

        return jwtAdminUser;

    }

	@BeforeAll
	public static void setJwtTestUser(@Autowired ApiUser apiUSer, @Autowired Environment env) throws Exception{
		String testUsername = env.getProperty("webapp.user.test.username");
		assertNotNull(testUsername);

		String testUserPassword = env.getProperty("webapp.user.test.password");
		assertNotNull(testUserPassword);

		jwtTestUser = apiUSer.getToken(testUsername, testUserPassword);
	}
}
