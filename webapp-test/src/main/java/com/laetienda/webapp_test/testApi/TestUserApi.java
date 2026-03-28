package com.laetienda.webapp_test.testApi;

import com.laetienda.model.user.Usuario;
import org.springframework.web.client.HttpStatusCodeException;

public interface TestUserApi {
    void cycle() throws HttpStatusCodeException;
}
