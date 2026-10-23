package com.masar.backend.dto.company;

import com.masar.backend.model.Company;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {

    private Long id;
    private String name;
    private String website;
    private String location;
    private String description;

    public static CompanyResponse from(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .website(company.getWebsite())
                .location(company.getLocation())
                .description(company.getDescription())
                .build();
    }
}
