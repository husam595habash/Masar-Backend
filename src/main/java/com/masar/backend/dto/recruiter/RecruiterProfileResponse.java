package com.masar.backend.dto.recruiter;

import com.masar.backend.model.RecruiterProfile;
import com.masar.backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What the API returns for a recruiter profile page: the user's name and email plus the
 * profile fields. It never exposes the User entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterProfileResponse {

    private String fullName;
    private String email;
    private String jobTitle;

    /**
     * Builds the response from the user and their saved profile.
     */
    public static RecruiterProfileResponse from(User user, RecruiterProfile profile) {
        return RecruiterProfileResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .jobTitle(profile.getJobTitle())
                .build();
    }
}
