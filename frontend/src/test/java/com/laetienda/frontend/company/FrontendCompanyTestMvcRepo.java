package com.laetienda.frontend.company;

import com.laetienda.frontend.FrontendTestMvcRepository;
import com.laetienda.lib.service.ToolBoxService;
import com.laetienda.model.user.TestUserDto;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@Component
public class FrontendCompanyTestMvcRepo {

    @Autowired private MockMvc mvc;
    @Autowired private FrontendTestMvcRepository repo;
	@Autowired private ToolBoxService tb;

    public void getCreateLayout(TestUserDto user) throws Exception {
		mvc.perform(get(manageCompanyCreateHref)
						.accept(MediaType.TEXT_HTML_VALUE)
						.with(repo.loginAs(user)))
				.andExpect(status().isOk());
	}

    void create(
            TestUserDto user,
            String name, String vanityUrl, String description,
            String memberPolicy
    ) throws Exception{
        MvcResult posted = mvc.perform(post(manageCompanyCreateHref)
				  		.with(repo.loginAs(user))
				  		.with(csrf())
				  		.param("name", name)
				  		.param("vanityUrl", vanityUrl)
				  		.param("memberPolicy", memberPolicy)     // a valid CompanyMemberPolicy constant (PUBLIC | REGISTRATION_REQUIRED | AUTHORIZATION_REQUIRED)
				  		.param("description", description)
				  		.param("owner", user.getUserId()))
		  		.andExpect(status().is3xxRedirection())
		  		.andExpect(redirectedUrlPattern("/thankYou/company/create.html*"))
		  		.andReturn();

  		// carry the session (holds the created company under the `element` key) + the redirect target
  		MockHttpSession session = (MockHttpSession) posted.getRequest().getSession(false);
  		String thankYouUrl = posted.getResponse().getRedirectedUrl();

        Assertions.assertNotNull(thankYouUrl);
        Assertions.assertNotNull(session);
        mvc.perform(get(thankYouUrl)
				 		 .session(session)
				  		.with(repo.loginAs(user))
				  		.accept(MediaType.TEXT_HTML_VALUE))
		  		.andExpect(status().isOk());
    }

    public void getCompanyLayout(TestUserDto user, String vanityUrl) throws Exception {
		String address = tb.setAddressParams(null, companyUrl, vanityUrl);
		mvc.perform(get(address)
						.with(repo.loginAs(user)))
				.andExpect(status().isOk());
    }

	public void getAllCompanyLayout(TestUserDto user) throws Exception{
		mvc.perform(get(manageCompanyAllHref)
						.accept(MediaType.TEXT_HTML_VALUE)
						.with(repo.loginAs(user)))
				.andExpect(status().isOk());
	}

    public void getManageCompanyLayout(TestUserDto user, String vanityUrl) throws Exception{
		String address = tb.setAddressParams(null, manageCompanyHref, vanityUrl);

		mvc.perform(get(address)
						.accept(MediaType.TEXT_HTML)
						.with(repo.loginAs(user)))
				.andExpect(status().isOk());
    }

    @Value("${seo.manage.company.create}") private String manageCompanyCreateHref;
    @Value("${seo.company.file}") private String companyUrl;
	@Value("${seo.manage.company.all}") private String manageCompanyAllHref;
	@Value("${seo.manage.company.find}") private String manageCompanyHref;
}
