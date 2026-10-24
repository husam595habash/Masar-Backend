package com.masar.backend.dto.application;

import com.masar.backend.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusRequest {

    @NotNull(message = "Status is required")
    private ApplicationStatus status;
}
