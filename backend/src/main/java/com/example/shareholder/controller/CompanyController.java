package com.example.shareholder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.shareholder.model.Company;
import com.example.shareholder.service.CompanyService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/company")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping()
    public ResponseEntity<Company> getCompanyById() {
        Company company = companyService.getCompany();
        return ResponseEntity.ok().body(company);
    }

    @PutMapping()
    public ResponseEntity<Company> updateCompany(@RequestBody Company company) {
        Company updatedCompany = companyService.updateCompany(company);
        return ResponseEntity.ok().body(updatedCompany);
    }
}