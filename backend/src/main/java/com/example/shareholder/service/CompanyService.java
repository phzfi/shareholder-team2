package com.example.shareholder.service;

import org.springframework.stereotype.Service;
import java.util.Optional;
import com.example.shareholder.model.Company;
import com.example.shareholder.repository.CompanyRepository;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company getCompany() {
        return companyRepository.findById(1L)
                .orElseThrow(() -> new IllegalArgumentException("Yhtiötä ei löytynyt"));
    }

    public Company updateCompany(Company company) {
        Optional<Company> companyOptional = companyRepository.findById(1L);

        if (companyOptional.isPresent()) {
            Company existingCompany = companyOptional.get();
            existingCompany.setName(company.getName());
            existingCompany.setCompanyId(company.getCompanyId());
            existingCompany.setCity(company.getCity());
            existingCompany.setUrl(company.getUrl());
            return companyRepository.save(existingCompany);
        } else {
            Company newCompany = new Company(company.getName(),
                    company.getCompanyId(),
                    company.getCity(),
                    company.getUrl());
            return companyRepository.save(newCompany);
        }
    }
}