package com.laetienda.kcUser.service;

import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.kc.KcUser;
import com.laetienda.model.user.Usuario;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;

public interface KcUserService {
    KcUser find();
    String getToken(MultiValueMap<String, String> creds);
    String isUsernameValid(String username) throws NotValidCustomException;
    String isUserIdValid(String userId) throws NotValidCustomException;
    String getEmailAddress(String userId) throws HttpStatusCodeException;
    KcUser createUser(Usuario user) throws NotValidCustomException;
    void deleteUser(String userId) throws HttpStatusCodeException;
}
