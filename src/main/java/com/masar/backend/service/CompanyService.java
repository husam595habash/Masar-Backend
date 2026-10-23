package com.masar.backend.service;

import com.masar.backend.dto.company.CompanyRequest;
import com.masar.backend.dto.company.CompanyResponse;
import com.masar.backend.exception.ConflictException;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.model.Company;
import com.masar.backend.model.RecruiterProfile;
import com.masar.backend.repository.CompanyRepository;
import com.masar.backend.repository.RecruiterProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    public CompanyService(CompanyRepository companyRepository,
                          RecruiterProfileRepository recruiterProfileRepository) {
        this.companyRepository = companyRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    /**
     * Creates a company and links it to the recruiter's profile.
     * 409 if the recruiter already has a company.
     */
    @Transactional
    public CompanyResponse createCompany(String email, CompanyRequest request) {
        RecruiterProfile profile = findProfile(email);

        if (profile.getCompany() != null) {
            throw new ConflictException("You already have a company");
        }

        Company company = Company.builder()
                .name(request.getName().trim())
                .website(request.getWebsite())
                .location(request.getLocation())
                .description(request.getDescription())
                .createdBy(profile.getUser())
                .build();
        companyRepository.save(company);

        profile.setCompany(company);
        recruiterProfileRepository.save(profile);

        return CompanyResponse.from(company);
    }

    /**
     * Returns the recruiter's company. 404 if the recruiter has none.
     */
    @Transactional(readOnly = true)
    public CompanyResponse getMyCompany(String email) {
        return CompanyResponse.from(findCompany(email));
    }

    /**
     * Updates the recruiter's company. 404 if none.
     */
    public CompanyResponse updateMyCompany(String email, CompanyRequest request) {
        Company company = findCompany(email);

        company.setName(request.getName().trim());
        company.setWebsite(request.getWebsite());
        company.setLocation(request.getLocation());
        company.setDescription(request.getDescription());
        companyRepository.save(company);

        return CompanyResponse.from(company);
    }

    private RecruiterProfile findProfile(String email) {
        return recruiterProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
    }

    private Company findCompany(String email) {
        Company company = findProfile(email).getCompany();
        if (company == null) {
            throw new ResourceNotFoundException("Company not found");
        }
        return company;
    }
}
