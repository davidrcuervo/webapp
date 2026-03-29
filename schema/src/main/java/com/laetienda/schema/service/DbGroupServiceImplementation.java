package com.laetienda.schema.service;

import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.schema.DbGroup;
import com.laetienda.schema.repository.DbGroupRepository;
import com.laetienda.utils.service.api.ApiUser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class DbGroupServiceImplementation implements DbGroupService {
    private final static Logger log = LoggerFactory.getLogger(DbGroupServiceImplementation.class);

    private DbGroupRepository repo;

    @Autowired ApiUser apiUser;
    @Autowired Validator validator;

    DbGroupServiceImplementation(DbGroupRepository dbGroupRepository) {
        this.repo = dbGroupRepository;
    }

    @Override
    public DbGroup create(DbGroup dbGroup) throws HttpStatusCodeException {
        log.info("DbGROUP_CONTROLLER::create");

        try {
            String currentUserId = apiUser.getCurrentUserId();
            String message;

            if(dbGroup.getOwner() == null){
                dbGroup.setOwner(currentUserId);
            }else{
                message = "Owner can't be set for new group";
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
            }

            if((message = areValidUsers(dbGroup)) != null){
                log.warn("DbGROUP_CONTROLLER::create. {}", message);
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
            }

            Set<ConstraintViolation<DbGroup>> violations = validator.validate(dbGroup);
            if(!violations.isEmpty()){
                message = violations.iterator().next().getMessage();
                log.warn("DbGROUP_CONTROLLER::create. {}", message);
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
            }

            return repo.save(dbGroup);
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }

    @Override
    public DbGroup find(String groupId) throws HttpStatusCodeException {
        throw new HttpServerErrorException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public DbGroup update(String groupId, Map<String, Object> body) throws HttpStatusCodeException {
        throw new HttpServerErrorException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public void delete(String groupId) throws HttpStatusCodeException {
        throw new HttpServerErrorException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public DbGroup addMember(String groupId, String userId) throws HttpStatusCodeException {
        throw new HttpServerErrorException(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public DbGroup removeMember(String groupId, String userId) throws HttpStatusCodeException {
        throw new HttpServerErrorException(HttpStatus.NOT_IMPLEMENTED);
    }

    private String areValidUsers(DbGroup dbGroup) throws HttpStatusCodeException {

        Set<String> memberIds = dbGroup.getMembers();

        if(memberIds != null && !memberIds.isEmpty()) {
            memberIds.forEach(memberId -> {
                try {
                    apiUser.isUserIdValid(memberId);

                } catch (NotValidCustomException e) {
                    if (e.getStatus() == HttpStatus.NOT_FOUND) {
                        String result = String.format("User, %s, does not exist", memberId);
                        throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, result);
                    }

                    throw e.getHttpStatusCodeException();
                }
            });
        }

        return null;
    }
}
