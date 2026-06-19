package com.resumematcher.api.service;

import com.resumematcher.api.entity.Company;
import com.resumematcher.api.exception.ResourceNotFoundException;
import com.resumematcher.api.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    public List<Company> searchByName(String name) {
        return companyRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public Company create(Company company) {
        return companyRepository.save(company);
    }

    @Transactional
    public Company update(Long id, Company updatedCompany) {
        Company existing = findById(id);
        existing.setName(updatedCompany.getName());
        existing.setIndustry(updatedCompany.getIndustry());
        existing.setDescription(updatedCompany.getDescription());
        existing.setWebsite(updatedCompany.getWebsite());
        existing.setLocation(updatedCompany.getLocation());
        existing.setLogoUrl(updatedCompany.getLogoUrl());
        return companyRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Company", id);
        }
        companyRepository.deleteById(id);
    }
}
