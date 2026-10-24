package com.masar.backend.dto.job;

import com.masar.backend.enums.JobStatus;
import com.masar.backend.enums.JobType;
import com.masar.backend.model.Job;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private Long id;
    private String title;
    private String description;
    private String location;
    private JobType type;
    private JobStatus status;
    private Integer salaryMin;
    private Integer salaryMax;
    private Long companyId;
    private String companyName;
    private Instant createdAt;

    public static JobResponse from(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .location(job.getLocation())
                .type(job.getType())
                .status(job.getStatus())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
