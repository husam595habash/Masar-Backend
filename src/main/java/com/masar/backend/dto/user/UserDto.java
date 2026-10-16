package com.masar.backend.dto.user;

import com.masar.backend.enums.Role;
import com.masar.backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private String id;
    private String fullName;
    private String email;
    private Role role;
    private boolean verified;
    private Instant createdAt;

    /**
     * Converts a User entity to a safe DTO (no password) for API responses.
     */
    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .verified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Converts this DTO back to a User entity (currently unused).
     */
    public User toEntity() {
        return User.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .role(role)
                .verified(verified)
                .build();
    }
}
