package com.laetienda.schema.service;

import com.laetienda.lib.options.DbGroupPolicy;
import com.laetienda.model.schema.DbGroup;
import com.laetienda.model.schema.DbItem;
import com.laetienda.schema.repository.DbGroupRepository;
import com.laetienda.schema.repository.ItemRepository;
import com.laetienda.schema.repository.SchemaRepository;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
public class DbGroupServiceImplementation implements DbGroupService {
    private final static Logger log = LoggerFactory.getLogger(DbGroupServiceImplementation.class);

    private final DbGroupRepository groupRepo;
    private final ItemRepository itemRepo;
    private final ApiUser apiUser;
    private final Validator validator;

    DbGroupServiceImplementation(
            DbGroupRepository dbGroupRepository,
            ItemRepository itemRepository,
            Validator validator,
            ApiUser apiUser) {
        this.groupRepo = dbGroupRepository;
        this.itemRepo = itemRepository;
        this.validator = validator;
        this.apiUser = apiUser;
    }

    @Override
    public DbGroup findByName(String name) throws HttpStatusCodeException {
        log.debug("DbGROUP_SERVICE::findByName. $name: {}", name);
        DbGroup result = groupRepo.findByName(name);

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

                if(group.getId() == null){

                    if(groupRepo.findByName(group.getName()) != null){
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

                    if(isValid(group)) {
                        groupRepo.save(group);
                    }else{
                        log.error("DbGROUP_SERVICE::processNewItemGroups. This message should never happen");
                    }
                }
            });
        }
    }

    @Override
    public boolean isValid(DbGroup dbGroup) throws HttpStatusCodeException {
        log.debug("DbGROUP_SERVICE::isValid");

        String message;

        if((message = areValidUsers(dbGroup)) != null){
            log.warn("DbGROUP_SERVICE::isValid. {}", message);
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, message);
        }

        Set<ConstraintViolation<DbGroup>> violations = validator.validate(dbGroup);
        if(!violations.isEmpty()){

            violations.forEach(v -> {
                log.trace("DbGROUP_SERVICE::isValid. $groupName: {} | {} | {} | {}",
                        dbGroup.getName(),
                        v.getPropertyPath(),
                        v.getInvalidValue(),
                        v.getMessage());
            });

            ConstraintViolation<DbGroup> violation = violations.iterator().next();
            message = String.format("Item group is not valid: %s | %s | %s",
                    violation.getPropertyPath().toString(),
                    violation.getInvalidValue(),
                    violation.getMessage()
                    );
            log.warn("DbGROUP_SERVICE::isValid. {}", message);
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
        log.debug("DbGROUP_SERVICE::delete $groupId: {}", groupId);

        try {
            Long gid = Long.parseLong(groupId);

            groupRepo.findById(gid).ifPresent(group -> {
                if (canEdit(group)) {

                    group.getEditorItems().forEach(item -> {
                        itemRepo.findById(item.getId()).ifPresent(editorItem -> {
                            editorItem.removeEditorGroup(group);
                            itemRepo.save(item);
                        });
                    });

                    group.getReaderItems().forEach(item -> {
                        itemRepo.findById(item.getId()).ifPresent(readerItem -> {
                            readerItem.removeReaderGroup(group);
                            itemRepo.save(item);
                        });
                    });

                    groupRepo.delete(group);

                } else {
                    String message = String.format("You don't have privileges to edit group. $groupId: %s", groupId);
                    log.warn("DbGROUP_SERVICE::delete. {}", message);
                    throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, message);
                }
            });

        }catch(NoSuchElementException e){
            log.debug("DbGROUP_SERVICE::delete. $groupId: {} | $message: {}", groupId, e.getMessage());
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, e.getMessage());

        }catch(NumberFormatException e){
            log.debug("DbGROUP_SERVICE::delete.. $groupId: {} | $message: {}", groupId, e.getMessage());
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
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

    private boolean canEdit(DbGroup dbGroup) throws HttpStatusCodeException {
        String uid = apiUser.getCurrentUserId();

        if(dbGroup.getOwner().equals(uid))
            return true;

        if (dbGroup.getPolicy().equals(DbGroupPolicy.MANAGE_BY_OWNER_ONLY))
            return false;

        else if(dbGroup.getPolicy().equals(DbGroupPolicy.MANAGE_BY_ALL))
            return dbGroup.getMembers().contains(uid);

        else{
            String message = String.format("SEVERE | Group contains an undefined policy. $groupPolicy: %s", dbGroup.getPolicy().toString());
            log.error("DbGROUP_SERVICE::canEdit. {}", message);
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }
}
