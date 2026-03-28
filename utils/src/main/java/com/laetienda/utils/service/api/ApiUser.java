package com.laetienda.utils.service.api;

import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.kc.KcUser;
import com.laetienda.model.user.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

public interface ApiUser {
    String isUsernameValid(String username) throws NotValidCustomException;
    String isUserIdValid(String userId) throws NotValidCustomException;
    Boolean userIdExists(String userId, String clientRegistrationId) throws HttpStatusCodeException;
    KcUser create(Usuario usuario, String clientRegistrationId) throws HttpStatusCodeException;
    void enable(String userId, String clientRegistrationId) throws HttpStatusCodeException;
    ResponseEntity<Void> delete(String userId, String jwtToken) throws HttpStatusCodeException;
    String getToken(String username, String password) throws  HttpStatusCodeException;
    String getCurrentUserId() throws NotValidCustomException;
    String getEmailAddress(String userId) throws HttpStatusCodeException;
}
