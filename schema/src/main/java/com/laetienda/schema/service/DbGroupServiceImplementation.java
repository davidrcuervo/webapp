package com.laetienda.schema.service;

import com.laetienda.model.schema.DbGroup;
import com.laetienda.model.schema.DbItem;
import com.laetienda.schema.repository.DbGroupRepository;
import com.laetienda.utils.service.api.ApiUser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;
import java.util.Set;

@Service
public class DbGroupServiceImplementation implements DbGroupService {
    private final static Logger log = LoggerFactory.getLogger(DbGroupServiceImplementation.class);

    private final DbGroupRepository repo;
    private final ApiUser apiUser;
    private final Validator validator;

    DbGroupServiceImplementation(
            DbGroupRepository dbGroupRepository,
            Validator validator,
            ApiUser apiUser) {
        this.repo = dbGroupRepository;
        this.validator = validator;
        this.apiUser = apiUser;
    }

    @Override
    public DbGroup findByName(String name) throws HttpStatusCodeException {
        log.debug("DbGROUP_SERVICE::findByName. $name: {}", name);
        DbGroup result = repo.findByName(name);

        if (result == null) {
            String message = String.format("Group, with that name, does not exist: %s", name);
            log.warn("DbGROUP_SERVICE::findByName. {}", message);
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, message);
        }

        return isValid(result) ? result : null;
    }

    @Override
    public void create(DbItem item) throws HttpStatusCodeException {
        String uid = apiUser.getCurrentUserId();
        processNewItemGroups(item.getReaderGroups(), uid, item, false);
        processNewItemGroups(item.getEditorGroups(), uid, item, true);
    }

    void processNewItemGroups(Set<DbGroup> dbGroups, String userId, DbItem item, boolean isEditorList) throws HttpStatusCodeException {
        if(dbGroups != null){
            dbGroups.forEach(group -> {
                log.trace("DbGROUP_SERVICE::processNewItemGroups. $groupId: {} | $groupName: {} | isEditorList: {}",
                        group.getId(),
                        group.getName(),
                        isEditorList
                );

                if(isEditorList) {
                    if(group.getEditorItems() != null && group.getEditorItems().contains(item)) {
                        log.trace("DbGROUP_SERVICE::processNewItemGroups. Item already exists in editor group");
                    }else{
                        group.addEditorItem(item);
                    }
                } else {
                    if (group.getReaderItems() != null && group.getReaderItems().contains(item)) {
                        log.trace("DbGROUP_SERVICE::processNewItemGroups. Item already exists in reader group");
                    } else {
                        group.addReaderItem(item);
                    }
                }

                log.trace("DbGROUP_SERVICE::processNewItemGroups. No. Of items: {}", group.getReaderItems().size());
                group.getReaderItems().forEach(i -> {
                    log.trace("DbGROUP_SERVICE::processNewItemGroups. id: {} | owner: {}", i.getId(), i.getOwner());
                });

                if(group.getId() == null){

                    if(repo.findByName(group.getName()) != null){
                        String message = String.format("A group with that name already exist. $name: %s", group.getName());
                        log.warn("DbGROUP_SERVICE::processNewItemGroups. {}", message);
                        throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
                    }

                    if(group.getOwner() == null){
                        group.setOwner(userId);
                    }else{
                        String message = String.format("Owner can't be assigned to any group. $ownerUserId: %s", group.getOwner());
                        log.warn("DbGROUP_SERVICE::processNewItemGroups.. {}", message);
                        throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid owner");
                    }

//                    if(!group.getItems().contains(item)){
//                        group.addItem(item);
//                    }

                    repo.save(group);

                }
//                else{
//                    repo.findById(group.getId()).ifPresent(g -> {
//                if(isEditorList){
//                    item.addEditorGroup(group);
//                }else{
//                    item.addReaderGroup(group);
//                }
//                    });
//                }

               if(!isValid(group)){
                   log.error("DbGROUP_SERVICE::processNewItemGroups. This message should never happen");
               }
            });
        }
    }

    @Override
    public boolean isValid(DbGroup dbGroup) throws HttpStatusCodeException {
        log.info("DbGROUP_SERVICE::isValid");

        String message;

        if((message = areValidUsers(dbGroup)) != null){
            log.warn("DbGROUP_SERVICE::isValid. {}", message);
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
        }

        Set<ConstraintViolation<DbGroup>> violations = validator.validate(dbGroup);
        if(!violations.isEmpty()){

            violations.forEach(violation -> {
                log.trace("DbGROUP_SERVICE::isValid. $groupName: {} | {} | {} | {}",
                        dbGroup.getName(),
                        violation.getPropertyPath(),
                        violation.getInvalidValue(),
                        violation.getMessage());
            });

            message = violations.iterator().next().getMessage();
            log.warn("DbGROUP_SERVICE::isValid. dbGroup is not valid. | $message: {}", message);
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
        }

        return true;
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
                } catch (HttpStatusCodeException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        String result = String.format("User, %s, does not exist", memberId);
                        log.warn("DbGROUP_SERVICE::areValidUsers. {}", result);
                        throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, result);
                    }else{
                        throw e;
                    }
                }
            });
        }

        return null;
    }
}
