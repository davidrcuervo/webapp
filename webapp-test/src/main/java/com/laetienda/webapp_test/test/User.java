package com.laetienda.webapp_test.test;

import com.laetienda.webapp_test.testApi.TestUserApi;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

@Service
public class User {

    @Autowired private TestUserApi testUserApi;

    public void run() throws HttpStatusCodeException, AssertionError {
        testUserApi.cycle();
        testUserApi.findEmailAddress();
    }
}
