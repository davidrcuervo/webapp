package com.laetienda.schema.controller;

import com.laetienda.model.schema.DbGroup;
import com.laetienda.schema.service.DbGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;

@RestController
@RequestMapping("${api.schema.root}")
public class DbGroupController {
    private final static Logger log = LoggerFactory.getLogger(DbGroupController.class);

    private final DbGroupService service;

    DbGroupController(DbGroupService service) {
        this.service = service;
    }

    @GetMapping("${api.schema.group.file.findByName}") //group/find/{groupName}
    public ResponseEntity<DbGroup> findByName(@PathVariable String groupName) {
        log.info("DbGROUP_CONTROLLER::findByName. $groupName: {}", groupName);
        return ResponseEntity.ok(service.findByName(groupName));
    }

    @PutMapping("${api.schema.group.file.update}")  //group/{groupId}/update
    public ResponseEntity<DbGroup> update(@RequestBody Map<String, Object> body, @PathVariable String groupId) throws HttpStatusCodeException {
        log.info("DbGROUP_CONTROLLER::update. $groupId: {}", groupId);
        return ResponseEntity.ok(service.update(groupId, body));
    }

    @DeleteMapping("${api.schema.group.file.delete}") //group/{groupId}/delete
    public ResponseEntity<Void> delete(@PathVariable String groupId) throws HttpStatusCodeException {
        log.info("DbGROUP_CONTROLLER::delete. $groupId: {}", groupId);
        service.delete(groupId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("${api.schema.group.file.member.add}") //group/{groupId}/member/add/{userId}
    public ResponseEntity<DbGroup> addMember(@PathVariable String groupId, @PathVariable String userId) throws HttpStatusCodeException {
        log.info("DbGROUP_CONTROLLER::addMember. $groupId: {} | $userId: {}", groupId, userId);
        return ResponseEntity.ok(service.addMember(groupId, userId));
    }

    @DeleteMapping("${api.schema.group.file.member.remove}") //group/{groupId}/member/remove/{userId}
    public ResponseEntity<DbGroup> removeMember(@PathVariable String groupId, @PathVariable String userId) throws HttpStatusCodeException {
        log.info("DbGROUP_CONTROLLER::remove. $groupId: {} | $userId: {}", groupId, userId);
        return ResponseEntity.ok(service.removeMember(groupId, userId));
    }
}
