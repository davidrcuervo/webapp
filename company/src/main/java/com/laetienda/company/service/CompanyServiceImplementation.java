package com.laetienda.company.service;

import com.laetienda.company.repository.CompanyRepository;
import com.laetienda.company.repository.FriendRepository;
import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.lib.options.CompanyMemberPolicy;
import com.laetienda.lib.options.CompanyMemberStatus;
import com.laetienda.model.company.Company;
import com.laetienda.model.company.Friend;
import com.laetienda.model.company.Member;
import com.laetienda.utils.service.api.ApiSchema;
import com.laetienda.utils.service.api.ApiUser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CompanyServiceImplementation implements CompanyService{
    private final static Logger log = LoggerFactory.getLogger(CompanyServiceImplementation.class);

    private final Validator validator;
    @Autowired private CompanyRepository repo;
    @Autowired private ApiUser apiUser;
    @Autowired private ApiSchema apiSchema;
    @Autowired private FriendRepository repoFriend;

    public CompanyServiceImplementation(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Company create(@NotNull Company company) throws NotValidCustomException {
        String userId = apiUser.getCurrentUserId();
        log.debug("COMPANY_SERVICE::create. $company: {}. $currentUserId: {}", company.getName(), userId);

        try {
            Company temp = repo.findByName(company.getName());
            String message = String.format("Company %s already exists.", company.getName());
            throw new NotValidCustomException(message, HttpStatus.FORBIDDEN, "company");

        }catch(NotValidCustomException e){
            if(e.getStatus() == HttpStatus.NOT_FOUND){
                Company result = repo.create(company);
                Member member = new Member(result, userId, CompanyMemberStatus.ACCEPTED);
                repo.addMember(member);
                return result;

            }else{
                throw e;
            }
        }
    }

    @Override
    public Long isCompanyValid(String companyId) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::companyId. $companyId: {}", companyId);

        try {
            Long id = Long.parseLong(companyId);
            return repo.isCompanyValid(id);
        }catch(NumberFormatException e){
            String message = String.format("COMPANY_SERVICE::isCompanyValid. companyId must be number of Long format. $companyId: %s", companyId);
            log.warn(message);
            throw new NotValidCustomException(e);
        }
    }

    @Override
    public Company find(String strId) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::find. $id: {}", strId);
        return repo.find(isCompanyValid(strId));
    }

    @Override
    public Company findByName(String name) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::findByName. $name: {}", name);
        return repo.findByName(name);
    }

    @Override
    public void delete(String companyId) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::delete. $id: {}", companyId);
        Long cid = isCompanyValid(companyId);
        List<Member> members = findAllMembers(cid);

        for(Member member : members){
            deleteMember(cid, member.getUserId());
        }

        repo.deleteById(cid);
    }

    @Override
    public void deleteMember(String companyId, String userId) throws NotValidCustomException {
        Long cid = isCompanyValid(companyId);
        deleteMember(cid, userId);
    }

    @Override
    public void deleteMember(Long companyId, String userId) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::removeMember. $company: {} | $userId: {}", companyId, userId);

        Member member = findMemberByIds(companyId.toString(), userId);

        for(Friend f : repoFriend.findAll(companyId, userId)){
            repoFriend.delete(f);
        }

        repo.removeMember(member);
    }

    @Override
    public Member addMember(String companyId, String userId) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::addMember. $company: {} | $userId: {}", companyId, userId);

        Long cid = isCompanyValid(companyId);
        String uid = apiUser.isUserIdValid(userId);
        Company company = repo.findNoJwt(cid);
        CompanyMemberPolicy policy = company.getMemberPolicy();
        CompanyMemberStatus status = CompanyMemberStatus.REQUESTED;

        List<Member> temp = repo.findMemberByUserIdNoJwt(cid, userId);

        if(temp != null && !temp.isEmpty()){
            String message = String.format("Member already exists. $companyId: %d | $userId: %s", cid, uid);
            log.warn("COMPANY_MEMBER::addMember. $message: {}", message);
            throw new NotValidCustomException(message, HttpStatus.FORBIDDEN, "member");
        }

        if(policy == CompanyMemberPolicy.PUBLIC || policy == CompanyMemberPolicy.REGISTRATION_REQUIRED) {
            status = CompanyMemberStatus.ACCEPTED;
        }

        Member member = new Member(company, uid, status);
        company.addMember(member);

        return repo.addMember(member);
    }

    @Override
    public Member findMemberByIds(String companyId, String userId) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::findMemberByIds. $companyid: {} | $userId: {}", companyId, userId);

        String uid = apiUser.isUserIdValid(userId);
        Long cid = isCompanyValid(companyId);

        List<Member> result = repo.findMemberByUserId(cid, uid);

        if (result == null || result.isEmpty()) {
            String message = String.format("COMPANY_SERVICE::findMemberByIds. User is not member of company. $companyId: %d | $user: '%s'd", cid, uid);
            log.warn(message);
            throw new NotValidCustomException(message, HttpStatus.NOT_FOUND, "member");

        } else if (result.size() > 1) {
            String message = String.format("COMPANY_SERVICE::findMemberByIds. There are more than one member in company with same userId. $companyId: %s | $user: %s", cid, uid);
            log.error(message);
            throw new NotValidCustomException(message, HttpStatus.INTERNAL_SERVER_ERROR, "member");
        }

        return result.getFirst();
    }

    @Override
    public List<Member> findAllMembers(Long cid) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::findAllMembers. $cid: {}", cid);
        return repo.findAllMembers(cid);
    }

    @Override
    public Member updateMember(Member member) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::updateMember. $memberId: {}", member.getId());

        Member temp = apiSchema.findById(Member.class, member.getId()).getBody();
        String currentUserId = apiUser.getCurrentUserId();

        if(temp == null){
            String message = String.format("Member does not exist. $memberId: %d", member.getId());
            log.warn(message);
            throw new NotValidCustomException(message, HttpStatus.BAD_REQUEST, "member");
        }

        if(!temp.getUserId().equals(member.getUserId())){
            String message = String.format("User of member can't be modified. $userId: %s", member.getUserId());
            log.error(message);
            throw new NotValidCustomException(message, HttpStatus.BAD_REQUEST, "member");
        }

        if(!temp.getCompany().getId().equals(member.getCompany().getId())){
            String message = String.format("Company of member can't be modified. $companyId: %s", member.getCompany().getId());
            log.error(message);
            throw new NotValidCustomException(message, HttpStatus.BAD_REQUEST, "member");
        }

        if(!temp.getStatus().equals(member.getStatus()) &&
                !(member.getCompany().getOwner().equals(currentUserId)) || member.getCompany().getEditors().contains(currentUserId)){
            String message = String.format("Current user does not have privileges to modify status of member. $currentUserId: %s | $memberId: %d", currentUserId, member.getId());
            log.warn(message);
            throw new NotValidCustomException(message, HttpStatus.UNAUTHORIZED, "member");
        }

        return repo.updateMember(member);
    }

    @Override
    public Company updateName(String companyId, String value) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::updateName. $companyId: {} | $value: {}", companyId, value);

        if(!isCompanyNameValid(value)) {
            String message = String.format("Company name has been modified but new name is not valid. $companyName: %s", value);
            log.warn(message);
            throw new NotValidCustomException(message, HttpStatus.FORBIDDEN, "company");
        }

        Long cid = isCompanyValid(companyId);
        Company temp = repo.find(cid);
        temp.setName(value);

        return repo.updateCompany(temp);
    }

//    @Override
//    public Company updateMemberPolicy(String companyId, String value) throws NotValidCustomException {
//        log.debug("COMPANY_SERVICE::updateMemberPolicy. $companyId: {} | $value: {}", companyId, value);
//        Long cid = isCompanyValid(companyId);
//        Company temp = repo.find(cid);
//        return null;
//    }

    @Override
    public Company updateDescription(String companyId, String value) throws NotValidCustomException {
        log.debug("COMPANY_SERVICE::updateDescription. $companyId: {} | $value: {}", companyId, value);
        Long cid = isCompanyValid(companyId);
        Company temp = repo.find(cid);
        temp.setDescription(value);

        Set<ConstraintViolation<Company>> violations = validator.validate(temp);
        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder();

            for(ConstraintViolation<Company> violation : violations) {
                message.append(violation.getMessage()).append(", ");
                log.warn(violation.getMessage());
            }

            throw new  NotValidCustomException(message.toString(), HttpStatus.BAD_REQUEST, "company");
        }

        return repo.updateCompany(temp);
    }

//    @Override
//    public Company updateCompany(Company company) throws NotValidCustomException {
//        log.debug("COMPANY_SERVICE::updateCompany. $company: {}", company.getName());
//
//        Company temp = apiSchema.findById(Company.class, company.getId()).getBody();
//
//        if(temp == null){
//            String message = String.format("Company does not exist or it can't be modified by current user. $companyId: %d", company.getId());
//            log.warn(message);
//            throw new NotValidCustomException(message, HttpStatus.BAD_REQUEST, "company");
//        }
//
//        //if the name is different check that new name is valid
//        if(!temp.getName().equals(company.getName()) && !isCompanyNameValid(company)){
//            String message = String.format("Company name has been modified but new name is not valid. $companyName: %s", company.getName());
//            log.warn(message);
//            throw new NotValidCustomException(message, HttpStatus.FORBIDDEN, "company");
//        }
//
//        //it is forbidden to modify company member policy
//        if(!temp.getMemberPolicy().equals(company.getMemberPolicy())){
//            String message = String.format("Company member policy can't be modified. $companyMemberPolicy: %s", company.getMemberPolicy().toString());
//            log.warn(message);
//            throw new NotValidCustomException(message, HttpStatus.FORBIDDEN, "company");
//        }
//
//        if(new HashSet<String>(company.getEditors()).containsAll(temp.getEditors())
//                || new HashSet<String>(temp.getEditors()).containsAll(company.getEditors())){
//            companyAddManager(temp, company);
//        }
//
//        if(!temp.getOwner().equals(company.getOwner())){
//            modifyCompanyOwner(temp, company);
//        }
//
//        return repo.updateCompany(company);
//    }

    private Boolean isCompanyNameValid(String value) throws NotValidCustomException{
        log.debug("COMPANY_SERVICE::isCompanyNameValid. $companyName: {}", value);

        try{
            Company temp = repo.findByNameNoJwt(value);
            return false;
        }catch(NotValidCustomException e){
            if(e.getStatus().equals(HttpStatus.NOT_FOUND)){
                return true;
            }else{
                throw e;
            }
        }
    }

    private void modifyCompanyOwner(Company temp, Company company) throws NotValidCustomException {

        String newOwnerUserId = company.getOwner();
        List<Member> members = findAllMembers(temp.getId());

        for(Member member : members){
            if(!member.getEditors().contains(newOwnerUserId)) {
                member.addEditor(newOwnerUserId);
                repo.updateMember(member);
            }
        }
    }

    private void companyAddManager(Company temp, Company company) throws NotValidCustomException {
        Set<String> tempManagers = new HashSet<>(temp.getEditors());
        Set<String> companyManagers = new HashSet<>(company.getEditors());
        List<Member> members = findAllMembers(temp.getId());

        tempManagers.removeAll(companyManagers);
        for(String userId : tempManagers){
            for(Member member : members){
                member.removeEditor(userId);
            }
        }

        companyManagers.removeAll(new HashSet<>(temp.getEditors()));
        for(String userId : companyManagers){
            for(Member member: members){
                member.addEditor(userId);
            }
        }
    }
}