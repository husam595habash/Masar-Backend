package com.masar.backend.dto.seeker;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Body of PUT /api/seekers/me. The max sizes match the column lengths in SeekerProfile,
 * so a too-long value gets a clean 400 instead of a database error.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeekerProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;

    @Size(max = 150, message = "Headline must be at most 150 characters")
    private String headline;

    @Size(max = 120, message = "Location must be at most 120 characters")
    private String location;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 60, message = "Years of experience must be 60 or less")
    private short yearsExperience;

}
