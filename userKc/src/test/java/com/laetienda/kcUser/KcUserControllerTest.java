package com.laetienda.kcUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class KcUserControllerTest {

	@Autowired MockMvc mvc;
	@Autowired Environment env;

//	@Test
    void shutdown() throws Exception {
        String actuator = env.getProperty("api.actuator.folder", "shutdown");
        String address = String.format("%s/shutdown", actuator);
        mvc.perform(post(address))
                .andExpect(status().isOk());
    }
}
