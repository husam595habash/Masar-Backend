package com.masar.backend.dto.recruiter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Body of PUT /api/recruiters/me. The max sizes match the column lengths, so a too-long
 * value gets a clean 400 instead of a database error.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;

    @Size(max = 100, message = "Job title must be at most 100 characters")
    private String jobTitle;
}
