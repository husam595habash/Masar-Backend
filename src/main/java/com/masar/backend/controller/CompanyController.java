package com.masar.backend.controller;

import com.masar.backend.dto.company.CompanyRequest;
import com.masar.backend.dto.company.CompanyResponse;
import com.masar.backend.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the logged-in recruiter's own company. The email comes from the token.
 * Access is limited to the RECRUITER role in SecurityConfig.
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * POST /api/companies: creates a company and links it to the recruiter (201).
     */
    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(Authentication auth,
                                                         @Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.createCompany(auth.getName(), request));
    }

    /**
     * GET /api/companies/me: returns the recruiter's company.
     */
    @GetMapping("/me")
    public ResponseEntity<CompanyResponse> getMyCompany(Authentication auth) {
        return ResponseEntity.ok(companyService.getMyCompany(auth.getName()));
    }

    /**
     * PUT /api/companies/me: updates the recruiter's company.
     */
    @PutMapping("/me")
    public ResponseEntity<CompanyResponse> updateMyCompany(Authentication auth,
                                                           @Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.ok(companyService.updateMyCompany(auth.getName(), request));
    }
}
