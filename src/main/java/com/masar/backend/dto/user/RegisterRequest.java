package com.masar.backend.dto.user;

import com.masar.backend.enums.Role;
import com.masar.backend.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull
    private Role role;

    public User toEntity(String encodedPassword) {
        return User.builder()
                .fullName(fullName.trim())
                .email(email.trim())
                .password(encodedPassword)
                .role(role)
                .verified(false)
                .build();
    }
}
