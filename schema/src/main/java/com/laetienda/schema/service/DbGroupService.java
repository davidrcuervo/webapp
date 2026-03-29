package com.laetienda.schema.service;

import com.laetienda.model.schema.DbGroup;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;

public interface DbGroupService {
    DbGroup create(DbGroup dbGroup) throws HttpStatusCodeException;
    DbGroup find(String groupId) throws HttpStatusCodeException;
    DbGroup update(String groupId, Map<String, Object> body)  throws HttpStatusCodeException;
    void delete(String groupId) throws HttpStatusCodeException;
    DbGroup addMember(String groupId, String userId) throws HttpStatusCodeException;
    DbGroup removeMember(String groupId, String userId) throws HttpStatusCodeException;
}
