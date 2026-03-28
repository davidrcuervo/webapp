package com.laetienda.kcUser.respository;

import com.laetienda.model.kc.KcToken;
import com.laetienda.model.kc.KcUser;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.List;
import java.util.Map;

public interface KcUserRepository {
    KcUser find();
    KcToken getToken(MultiValueMap<String, String> creds);
    List<KcUser> findByUsername(String username);
    KcUser findByUserId(String userId) throws HttpStatusCodeException;
    void modify(String userId, Map<String, Object> content) throws HttpStatusCodeException;
}
