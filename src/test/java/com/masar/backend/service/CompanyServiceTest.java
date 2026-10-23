package com.masar.backend.service;

import com.masar.backend.dto.company.CompanyRequest;
import com.masar.backend.dto.company.CompanyResponse;
import com.masar.backend.exception.ConflictException;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.model.Company;
import com.masar.backend.model.RecruiterProfile;
import com.masar.backend.model.User;
import com.masar.backend.repository.CompanyRepository;
import com.masar.backend.repository.RecruiterProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private RecruiterProfileRepository recruiterProfileRepository;

    @InjectMocks
    private CompanyService companyService;

    private User user;
    private RecruiterProfile profile;
    private CompanyRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder().id("u-1").fullName("Sara").email("sara@mail.com").build();
        profile = RecruiterProfile.builder().user(user).build();

        request = new CompanyRequest();
        request.setName("  Acme ");
        request.setWebsite("https://acme.com");
        request.setLocation("Ramallah");
        request.setDescription("We build things");
    }

    @Nested
    @DisplayName("Create company")
    class CreateCompany {

        @Test
        @DisplayName("saves the company and links it to the recruiter profile")
        void success() {
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.of(profile));

            CompanyResponse response = companyService.createCompany("sara@mail.com", request);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Acme");
            assertThat(saved.getWebsite()).isEqualTo("https://acme.com");
            assertThat(saved.getLocation()).isEqualTo("Ramallah");
            assertThat(saved.getDescription()).isEqualTo("We build things");
            assertThat(saved.getCreatedBy()).isSameAs(user);

            assertThat(profile.getCompany()).isSameAs(saved);
            verify(recruiterProfileRepository).save(profile);
            assertThat(response.getName()).isEqualTo("Acme");
        }

        @Test
        @DisplayName("throws 409 when the recruiter already has a company")
        void alreadyHasCompany() {
            profile.setCompany(Company.builder().name("Old").build());
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> companyService.createCompany("sara@mail.com", request))
                    .isInstanceOf(ConflictException.class);

            verify(companyRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws 404 when the recruiter profile does not exist")
        void profileNotFound() {
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> companyService.createCompany("sara@mail.com", request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(companyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get my company")
    class GetMyCompany {

        @Test
        @DisplayName("returns the recruiter's company")
        void success() {
            profile.setCompany(Company.builder().id(1L).name("Acme").location("Ramallah").build());
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.of(profile));

            CompanyResponse response = companyService.getMyCompany("sara@mail.com");

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Acme");
            assertThat(response.getLocation()).isEqualTo("Ramallah");
        }

        @Test
        @DisplayName("throws 404 when the recruiter has no company")
        void noCompany() {
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> companyService.getMyCompany("sara@mail.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update my company")
    class UpdateMyCompany {

        @Test
        @DisplayName("updates the fields and trims the name")
        void success() {
            Company company = Company.builder().id(1L).name("Old").website("old").location("old").build();
            profile.setCompany(company);
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.of(profile));

            CompanyResponse response = companyService.updateMyCompany("sara@mail.com", request);

            assertThat(company.getName()).isEqualTo("Acme");
            assertThat(company.getWebsite()).isEqualTo("https://acme.com");
            assertThat(company.getLocation()).isEqualTo("Ramallah");
            assertThat(company.getDescription()).isEqualTo("We build things");
            verify(companyRepository).save(company);
            assertThat(response.getName()).isEqualTo("Acme");
        }

        @Test
        @DisplayName("throws 404 and saves nothing when the recruiter has no company")
        void noCompany() {
            when(recruiterProfileRepository.findByUserEmail("sara@mail.com")).thenReturn(Optional.of(profile));

            assertThatThrownBy(() -> companyService.updateMyCompany("sara@mail.com", request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(companyRepository, never()).save(any());
        }
    }
}
