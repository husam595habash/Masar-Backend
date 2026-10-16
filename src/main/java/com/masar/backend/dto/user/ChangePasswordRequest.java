package com.masar.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of PUT /api/users/me/password.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    // 72 is the most BCrypt can use (it ignores anything after 72 bytes)
    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;
}
