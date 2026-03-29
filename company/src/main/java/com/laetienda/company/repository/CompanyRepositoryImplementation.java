package com.laetienda.company.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.company.Company;
import com.laetienda.model.company.Member;
import com.laetienda.utils.service.api.ApiSchema;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

@Repository
public class CompanyRepositoryImplementation implements CompanyRepository{
    private final static Logger log = LoggerFactory.getLogger(CompanyRepositoryImplementation.class);

    private final RestClient client;
    @Autowired private ApiSchema schema;
    @Autowired private Environment env;
    @Autowired private ObjectMapper json;

    @Value("${kc.client-registration-id.webapp}")
    String webappClientId;

    public CompanyRepositoryImplementation(RestClient restClient){
        this.client= restClient;
    }

    @Override
    public Company create(@NotNull Company company) throws HttpStatusCodeException {
        log.debug("COMPANY_REPOSITORY::create. $company: {}", company.getName());

        try {
            return schema.create(Company.class, company).getBody();
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }

    @Override
    public Long isCompanyValid(Long id) throws HttpStatusCodeException {
        log.debug("COMPANY_REPOSITORY::isCompanyValid. $companyId: {}", id);

        try{
            String companyId = schema.isItemValid(Company.class, id).getBody();
            return Long.parseLong(companyId);
        } catch (NumberFormatException e) {
            String message = String.format("COMPANY_REPOSITORY::isCompanyValid. Invalid long id format. $error: %s", e.getMessage());
            log.error("COMPANY_REPOSITORY::isCompanyValid. {}", message);
            log.trace(message, e);
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        } catch(NotValidCustomException ce){
            throw ce.getHttpStatusCodeException();
        }
    }

    @Override
    public Company findByName(String name) throws HttpStatusCodeException {
        Map<String, String> body = new HashMap<String, String>();
        body.put("name", name);

        try {
            return schema.find(Company.class, body).getBody();
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }

    @Override
    public Company findByNameNoJwt(String name) throws HttpStatusCodeException {
        log.debug("COMPANY_REPOSITORY::findByNameNoJwt. $name: {}", name);
        String address = env.getProperty("api.schema.find.uri", "/api/v0/schema/find?clase={clazzName}");

        Map<String, String> body = new HashMap<String, String>();
        body.put("name", name);
        String clazzName = schema.getClazzName(Company.class);

        try{
            return client.post().uri(address, clazzName)
                    .attributes(clientRegistrationId(webappClientId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsBytes(body))
                    .retrieve().toEntity(Company.class).getBody();
        }catch(Exception e){
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public Company find(Long id) throws HttpStatusCodeException {

        try {
            return schema.findById(Company.class, id).getBody();
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }

    @Override
    public Company findNoJwt(Long id) throws HttpStatusCodeException {
        log.debug("COMPANY_REPO::findNoJwt. $id: {}", id);

        String address = env.getProperty("api.schema.findById.uri", "findById");
        String clazzName = schema.getClazzName(Company.class);
        return client.get().uri(address, id.toString(), clazzName)
                .accept(MediaType.APPLICATION_JSON)
                .attributes(clientRegistrationId(webappClientId))
                .retrieve().toEntity(Company.class).getBody();
    }

    @Override
    public void deleteById(Long id) throws HttpStatusCodeException {
        log.debug("COMPANY_REPOSITORY::deleteById. $id: {}", id);

        try {
            schema.deleteById(Company.class, id);
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }

    @Override
    public List<Member> findMemberByUserId(Long cid, String userId) throws HttpStatusCodeException {
        log.debug("COMPANY_REPO::findMemberByUserId. $companyId: {} | $user: {}", cid, userId);
        String query = getQueryFindMemberByUserId(cid, userId);
        return findMembersByQuery(query);
    }

    @Override
    public List<Member> findMemberByUserIdNoJwt(Long cid, String userId) throws HttpStatusCodeException {
        log.debug("COMPANY_REPO::findMemberByUserIdNoJwt. $companyId: {} | $user: {}", cid, userId);
        String query = getQueryFindMemberByUserId(cid, userId);
        return findMembersByQueryNoJwt(query);
    }

    private String getQueryFindMemberByUserId(Long cid, String userId) throws HttpStatusCodeException {
        return String.format("SELECT m FROM %s m INNER JOIN m.company c WHERE c.id = %d AND m.userId = '%s'", Member.class.getName(), cid, userId);
    }

    @Override
    public List<Member> findAllMembers(Long cid) throws HttpStatusCodeException {
        log.debug("COMPANY_REPO::findAllMembers. $cid: {}", cid);
        String query = String.format("SELECT m FROM %s m INNER JOIN m.company c WHERE c.id = %d", Member.class.getName(), cid);
        return findMembersByQuery(query);
    }

    private List<Member> findMembersByQuery(String query) throws HttpStatusCodeException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("query", query);

        ResponseEntity<String> response = null;

        try {
            response = schema.findByQuery(Member.class, params);
            return findMembersByQuery(response);
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }

    private List<Member> findMembersByQueryNoJwt(String query) throws HttpStatusCodeException{
        Map<String, String> params = new HashMap<String, String>();
        params.put("query", query);

        try {
            ResponseEntity<String> response = schema.findByQueryNoJwt(Member.class, params);
            return findMembersByQuery(response);
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }

    private List<Member> findMembersByQuery(ResponseEntity<String> response) throws NotValidCustomException {
        log.trace("COMPANY_REPO::findMemberByUserId. $response: {}", response.getBody());

        try {
            return json.readValue(response.getBody(), new TypeReference<List<Member>>() {});
        } catch (JsonProcessingException e) {
            String message = String.format("COMPANY_REPO::findMemberByUserId. $error: %s", e.getMessage());
            log.error(message);
            log.trace(message, e);
            throw new NotValidCustomException(message, HttpStatus.INTERNAL_SERVER_ERROR, "company");
        }
    }

    @Override
    public Company removeMember(Member member) throws HttpStatusCodeException {
        log.debug("COMPANY_REPOSITORY::removeMember. $company: {}, $user: {}", member.getCompany().getName(), member.getUserId());

        try {
            schema.deleteById(Member.class, member.getId());
            return find(member.getCompany().getId());
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }

    @Override
    public Member updateMember(Member member) throws HttpStatusCodeException {
        log.debug("COMPANY_REPOSITORY::updateMember. $memberId: {}", member.getId());

        try {
            return schema.update(Member.class, member).getBody();
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }

    @Override
    public Company updateCompany(Company company) throws HttpStatusCodeException {
        log.debug("COMPANY_REPOSITORY::updateCompany. $company: {}", company.getName());

        try {
            return schema.update(Company.class, company).getBody();
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }

    @Override
    public Member addMember(Member member) throws HttpStatusCodeException {
        log.debug("COMPANY_REPOSITORY::addMember. $company: {}, $user: {}", member.getCompany().getName(), member.getUserId());

        try {
            return schema.create(Member.class, member).getBody();
        } catch (NotValidCustomException e) {
            throw e.getHttpStatusCodeException();
        }
    }
}
