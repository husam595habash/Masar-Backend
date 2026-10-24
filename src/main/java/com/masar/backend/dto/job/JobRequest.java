package com.masar.backend.dto.job;

import com.masar.backend.enums.JobType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @Size(max = 120, message = "Location must be at most 120 characters")
    private String location;

    @NotNull(message = "Job type is required")
    private JobType type;

    @Min(value = 0, message = "Minimum salary cannot be negative")
    private Integer salaryMin;

    @Min(value = 0, message = "Maximum salary cannot be negative")
    private Integer salaryMax;
}
