package com.laetienda.kcUser.controller;

import com.laetienda.kcUser.service.KcUserService;
import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.kc.KcUser;
import com.laetienda.model.user.Usuario;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

import java.security.Principal;

@RestController
@RequestMapping("${api.kcUser.folder}") // api/v0/user
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired private Environment env;
    @Autowired private KcUserService service;

    @GetMapping("${api.kcUser.find.file}") //find.html
    public ResponseEntity<KcUser> find(){
        log.info("USER_CONTROLLER::find");
        return ResponseEntity.ok(service.find());
    }

    @PostMapping("${api.kcUser.token.file}")
    public ResponseEntity<String> getToken(@RequestParam MultiValueMap<String, String> creds){
        log.info("USER_CONTROLLER::getToken $username: {}", creds.get("username"));
        return ResponseEntity.ok(service.getToken(creds));
    }

    @GetMapping("${api.kcUser.isUsernameValid.file}")
    public ResponseEntity<String> isUsernameValid(@PathVariable String username) throws NotValidCustomException {
        log.info("USER_CONTROLLER::isUsernameValid. $username: {}", username);
        return ResponseEntity.ok(service.isUsernameValid(username));
    }

    @GetMapping("${api.kcUser.isUserIdValid.file}")
    public ResponseEntity<String> isUserIdValid(@PathVariable String userId) throws NotValidCustomException {
        log.info("USER_CONTROLLER::isUserIdValid. $userId: {}", userId);
        return ResponseEntity.ok(service.isUserIdValid(userId));
    }

    @GetMapping("${api.usuario.test.file}") //api/v0/user/test.html
    public ResponseEntity<String> test(Principal principal){
        log.trace("USER_CONTROLLER::test. $api.usuario.test.file: {}", env.getProperty("api.usuario.test.file"));
        log.trace("USER_CONTROLLER::test $api.usuario.login.path: {}", env.getProperty("api.usuario.login.path"));

        if(principal instanceof OAuth2AuthenticationToken authentication){
            log.trace("USER_CONTROLLER::test $principal.name: {}", principal.getName());

        }else if(principal != null){
            log.trace("USER_CONTROLLER::test (non-OAuth2): {}", principal.getName());

        }else{
            log.trace("USER_CONTROLLER::test No authenticated user found");
        }

        return ResponseEntity.ok("Successful test.");
    }

    @GetMapping("${api.kcUser.login.file}") //api/v0/user/login
    public ResponseEntity<String> login(Principal principal){
        log.trace("USER_CONTROLLER::login");
        log.trace("USER_CONTROLLER::login $user: {}", principal.getName());

        if(principal instanceof OAuth2AuthenticationToken authentication) {
            authentication.getPrincipal().getAttributes().forEach((key,value) -> {
                log.trace("USER_CONTROLLER::login. OAuth2AuthenticationToken. $attribute: {}, $value: {}", key, value);
            });

        }else if(principal instanceof JwtAuthenticationToken jwt){
            jwt.getTokenAttributes().forEach((key, value) -> {
                log.trace("USER_CONTROLLER::login. JwtAuthenticationToken. $attribute: {}, value: {}", key, value);
            });
            jwt.getAuthorities().forEach(authority -> {
                log.trace("USER_CONTROLLER::login. $authority: {}", authority.getAuthority());
            });

        }else{
            log.trace("USER_CONTROLLER::login No authenticated user found");
        }

        return ResponseEntity.ok(String.format("Successful login. $user: %s", principal.getName()));
//        return ResponseEntity.ok("Successful login");
    }

    @GetMapping("${api.kcUser.file.findEmailAddress}") //api/v0/user/findEmailAddress/{userId}
    public ResponseEntity<String> getEmailAddress(@PathVariable String userId) throws HttpStatusCodeException {
        log.info("USER_CONTROLLER::getEmailAddress. $userId: {}", userId);
        return ResponseEntity.ok(service.getEmailAddress(userId));
    }

    @GetMapping("${api.usuario.testAuthorization.file}")//api/v0/user/testAuthorization.html
    public ResponseEntity<String> testAuthorization(Principal principal){
        log.trace("USER_CONTROLLER::testAuthorization. $user: {}", principal.getName());
        return ResponseEntity.ok(String.format(
                "Successful authorization logging. $User: %s",
                principal.getName()));
    }

    @PostMapping("${api.kcUser.file.create}") //api/v0/user/create
    public ResponseEntity<KcUser> create (@Valid @RequestBody Usuario usuario) throws NotValidCustomException {
        log.info("USER_CONTROLLER::create $user: {}", usuario.getUsername());
        return ResponseEntity.ok(service.createUser(usuario));
    }

    @DeleteMapping("${api.kcUser.file.delete}") //api/v0/user/delete/{userId}
    public ResponseEntity<Void> delete (@PathVariable String userId) throws NotValidCustomException {
        log.info("USER_CONTROLLER::delete $userId: {}", userId);
        service.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
