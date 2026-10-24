package com.masar.backend.dto.application;

import com.masar.backend.enums.ApplicationStatus;
import com.masar.backend.model.Application;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String seekerName;
    private String seekerEmail;
    private ApplicationStatus status;
    private Instant createdAt;

    public static ApplicationResponse from(Application application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(application.getJob().getId())
                .jobTitle(application.getJob().getTitle())
                .companyName(application.getJob().getCompany().getName())
                .seekerName(application.getSeeker().getFullName())
                .seekerEmail(application.getSeeker().getEmail())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .build();
    }
}
