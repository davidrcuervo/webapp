package com.laetienda.frontend;

import com.laetienda.model.user.TestUserDto;
import com.laetienda.utils.lib.UtilsBox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@SpringBootTest
@AutoConfigureMockMvc
@Import(FrontendTestConfiguration.class)
class FrontendTests {

	private static TestUserDto[] USERS;

	@Autowired private Environment env;
	@Autowired private MockMvc mvc;
	@Autowired private FrontendTestMvcRepository repo;

    @Test
	void health() throws Exception {
		String address = env.getProperty("api.actuator.health.path", "#");
		mvc.perform(get(address))
				.andExpect(status().isOk());
	}

//	@Test
	void shutdown() throws Exception {
		String address = String.format("%s/shutdown", env.getProperty("api.actuator.folder", "actuator"));
		mvc.perform(post(address))
				.andExpect(status().isOk());
	}

	@Test
	public void notFoundTemplate() throws Exception {
		mvc.perform(get("/wrongAddress")
						.accept(MediaType.TEXT_HTML_VALUE)
//						.header(HttpHeaders.AUTHORIZATION, "Bearer " + USERS[1].getToken()))
						.with(repo.loginAs(USERS[1])))
				.andExpect(status().isNotFound());
	}

	@Test
	public void homeHtmlAuthenticated() throws Exception {
		mvc.perform(get(homeHref)
						.accept(MediaType.TEXT_HTML_VALUE)
						.with(repo.loginAs(USERS[1])))
				.andExpect(status().isOk());
	}

	@Test
	public void homeHtmlAnonymous() throws Exception {
		mvc.perform(get(homeHref)
						.accept(MediaType.TEXT_HTML_VALUE))
				.andExpect(status().isOk());
	}

	@Test
	public void settingsLayout() throws Exception {
		mvc.perform(get(settingsHref)
						.accept(MediaType.TEXT_HTML_VALUE)
						.with(repo.loginAs(USERS[1])))
				.andExpect(status().isOk());
	}



	@Test
  	public void companyCycleCreateShowModifyRemove() throws Exception {
  		                            // thank-you layout renders
	}

	@BeforeAll
	static void setup(@Autowired UtilsBox utils) {
		USERS = utils.getTestUsers(2, "FRONTEND");
	}

	@AfterAll
	static void tearDown(@Autowired UtilsBox utilsBox) {
		utilsBox.deleteTestUsers(USERS);
	}

	@Value("${seo.home.file}") private String homeHref;
	@Value("${seo.manage.file}") private String settingsHref;



}
