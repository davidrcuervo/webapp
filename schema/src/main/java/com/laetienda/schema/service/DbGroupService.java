package com.laetienda.schema.service;

import com.laetienda.model.schema.DbGroup;
import com.laetienda.model.schema.DbItem;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;
import java.util.Set;

public interface DbGroupService {
    DbGroup findByName(String name) throws HttpStatusCodeException;
    void create(DbItem item) throws HttpStatusCodeException;
    boolean isValid(DbGroup dbGroup) throws HttpStatusCodeException;
    DbGroup find(String groupId) throws HttpStatusCodeException;
    DbGroup update(String groupId, Map<String, Object> body)  throws HttpStatusCodeException;
    void updateItem(DbItem newItem, DbItem oldItem) throws HttpStatusCodeException;
    void delete(String groupId) throws HttpStatusCodeException;
    DbGroup addMember(String groupId, String userId) throws HttpStatusCodeException;
    DbGroup removeMember(String groupId, String userId) throws HttpStatusCodeException;

}
