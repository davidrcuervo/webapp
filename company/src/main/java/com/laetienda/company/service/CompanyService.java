package com.laetienda.company.service;

import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.company.Company;
import com.laetienda.model.company.Member;
import jakarta.validation.Valid;

import java.util.List;

public interface CompanyService {
    Company create(Company company) throws NotValidCustomException;
    Long isCompanyValid(String companyId) throws NotValidCustomException;
    Company find(String id) throws NotValidCustomException;
    Company findByName(String name) throws NotValidCustomException;
    Company updateName(String companyId, String value) throws NotValidCustomException;
    Company updateDescription(String companyId, String value)  throws NotValidCustomException;
    void delete(String idStr) throws NotValidCustomException;
    void deleteMember(String companyId, String userId) throws NotValidCustomException;
    void deleteMember(Long companyId, String userId) throws NotValidCustomException;
    Member addMember(String companyName, String userId) throws NotValidCustomException;
    Member findMemberByIds(String companyId, String userId) throws NotValidCustomException;
    List<Member> findAllMembers(Long cid) throws NotValidCustomException;
    Member updateMember(@Valid Member member) throws NotValidCustomException;
//    Company updateMemberPolicy(String companyId, String value)  throws NotValidCustomException;
//    Company updateCompany(@Valid Company company) throws NotValidCustomException;
}
