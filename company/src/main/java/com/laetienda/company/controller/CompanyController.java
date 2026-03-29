package com.laetienda.company.controller;

import com.laetienda.company.service.CompanyService;
import com.laetienda.lib.exception.NotValidCustomException;
import com.laetienda.model.company.Company;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

@RestController
@RequestMapping("${api.company.folder}") //api/v0/Company
public class CompanyController {
    private final static Logger log = LoggerFactory.getLogger(CompanyController.class);

    @Autowired private CompanyService service;

    @PostMapping("${api.company.create.file}") //api/v0/Company/create
    public ResponseEntity<Company> create(@RequestBody @Valid Company company) throws NotValidCustomException {
        log.info("COMPANY_CONTROLLER::create. $company: {}", company.getName());
        return ResponseEntity.ok(service.create(company));
    }

    @GetMapping("${api.company.isValid.file}") //api/v0/company/isValid/{companyId}
    public ResponseEntity<String> isCompanyValid(@PathVariable String companyId) throws NotValidCustomException{
        log.info("COMPANY_CONTROLLER::isCompanyValid. $companyId: {}", companyId);
        return ResponseEntity.ok(service.isCompanyValid(companyId).toString());
    }

    @GetMapping("${api.company.find.file}") //api/v0/company/find/{id}
    public ResponseEntity<Company> find(@PathVariable String id) throws NotValidCustomException {
        log.info("COMPANY_CONTROLLER::find. $id: {}", id);
        return ResponseEntity.ok(service.find(id));
    }

    @GetMapping("${api.company.findByName.file}") //api/v0/company/findByName/{name}
    public ResponseEntity<Company> findByName(@PathVariable String name) throws NotValidCustomException {
        log.info("COMPANY_CONTROLLER::findByName. $name: {}", name);
        return ResponseEntity.ok(service.findByName(name));
    }

    @PutMapping("${api.company.update.file.name}") //api/v0/company/update/{companyId}/name/{value}
    public ResponseEntity<Company> updateName(@PathVariable String companyId, @PathVariable String value) throws NotValidCustomException {
        log.info("COMPANY_CONTROLLER::updateName. $companyId: {} | $value: {}", companyId, value);
        return ResponseEntity.ok(service.updateName(companyId, value));
    }

    @PostMapping("${api.company.update.file.description}") //api/v0/company/update/{companyId}/description
    public ResponseEntity<Company> updateMemberPolicy(@PathVariable String companyId, @RequestBody String description) throws NotValidCustomException {
        log.info("COMPANY_CONTROLLER::updateDescription. $companyId: {} | $description: {}", companyId, description);
        return ResponseEntity.ok(service.updateDescription(companyId, description));
    }

    @PutMapping("${api.company.manager.file.add}") //api/v0/company/{companyId}/manager/add/{userId}
    public ResponseEntity<Company> addManager(@PathVariable String companyId, @PathVariable String userId) throws HttpStatusCodeException {
        log.info("COMPANY_CONTROLLER::addManager. $companyId: {} | $userId: {}", companyId, userId);
        return ResponseEntity.ok(service.addManager(companyId, userId));
    }

//    @PostMapping("${api.company.update.file.memberPolicy}") //api/v0/company/update/{companyId}/description
//    public ResponseEntity<Company> updateDescription(@PathVariable String companyId, @RequestBody String value) throws NotValidCustomException {
//        log.info("COMPANY_CONTROLLER::updateMemberPolicy. $companyId: {} | $value: {}", companyId, value);
//        return ResponseEntity.ok(service.updateMemberPolicy(companyId, value));
//    }

//    @PutMapping("${api.company.update.file}") //api/v0/company/update
//    public ResponseEntity<Company> update(@RequestBody @Valid Company company) throws NotValidCustomException {
//        log.info("COMPANY_CONTROLLER::update. $company: {}", company.getName());
//        return ResponseEntity.ok(service.updateCompany(company));
//    }

    @DeleteMapping("${api.company.delete.file}") //api/v0/company/delete/{id}
    public ResponseEntity<Void> delete(@PathVariable String id) throws NotValidCustomException {
        log.info("COMPANY_CONTROLLER::delete $id: {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
