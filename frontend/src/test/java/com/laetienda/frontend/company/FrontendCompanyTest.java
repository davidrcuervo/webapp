package com.laetienda.frontend.company;

import com.laetienda.frontend.FrontendTestConfiguration;
import com.laetienda.model.user.TestUserDto;
import com.laetienda.utils.lib.UtilsBox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FrontendTestConfiguration.class)
public class FrontendCompanyTest {

    private static TestUserDto[] USERS;

    @Autowired FrontendCompanyTestMvcRepo compRepo;

    @Test
    public void showCreateLayout() throws Exception{
        compRepo.getCreateLayout(USERS[1]);
    }

    @Test
    public void getAllCompanyLayout() throws Exception{
        compRepo.getAllCompanyLayout(USERS[1]);
    }

    @Test
    public void cycle() throws Exception{
        compRepo.create(USERS[1], "Test Company", "test-company", "A test company", "PUBLIC");
        compRepo.getAllCompanyLayout(USERS[1]);
//        compRepo.getCompanyLayout(USERS[1], "test-company");
        compRepo.getManageCompanyLayout(USERS[1], "test-company");
    }

    @BeforeAll
    static void setup(@Autowired UtilsBox utils) {
        USERS = utils.getTestUsers(2, "FRONTEND");
    }

    @AfterAll
    static void tearDown(@Autowired UtilsBox utilsBox) {
        utilsBox.deleteTestUsers(USERS);
    }
}
