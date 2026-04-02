package com.laetienda.kcUser.respository;

import com.laetienda.model.kc.KcToken;
import com.laetienda.model.kc.KcUser;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.List;
import java.util.Map;

public interface KcUserRepository {
    KcUser find();
    void create(UserRepresentation userRepresentation) throws HttpStatusCodeException;
    void delete(String userId) throws HttpStatusCodeException;
    KcToken getToken(MultiValueMap<String, String> creds);
    List<KcUser> findByUsername(String username);
    KcUser findByUserId(String userId) throws HttpStatusCodeException;
    void modify(String userId, Map<String, Object> content) throws HttpStatusCodeException;
    List<KcUser> findRoleUsers(String roleName) throws HttpStatusCodeException;

}
