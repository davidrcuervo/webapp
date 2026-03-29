package com.laetienda.company.repository;

import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.company.Company;
import com.laetienda.model.company.Member;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.List;

public interface CompanyRepository {
    Company create(Company company) throws HttpStatusCodeException;
    Company findByName(String name) throws HttpStatusCodeException;
    Company findByNameNoJwt(String name) throws HttpStatusCodeException;
    Long isCompanyValid(Long id) throws HttpStatusCodeException;
    Company find(Long id) throws HttpStatusCodeException;
    Company findNoJwt(Long id) throws HttpStatusCodeException;
    void deleteById(Long id) throws HttpStatusCodeException;
    List<Member> findAllMembers(Long cid) throws HttpStatusCodeException;
    List<Member> findMemberByUserId(Long companyId, String userId) throws HttpStatusCodeException;
    List<Member> findMemberByUserIdNoJwt(Long cid, String userId) throws HttpStatusCodeException;
    Member addMember(Member member) throws HttpStatusCodeException;
    Company removeMember(Member member) throws HttpStatusCodeException;
    Member updateMember(Member member) throws HttpStatusCodeException;
    Company updateCompany(Company company) throws HttpStatusCodeException;

}
