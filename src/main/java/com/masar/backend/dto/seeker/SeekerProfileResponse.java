package com.masar.backend.dto.seeker;

import com.masar.backend.model.SeekerProfile;
import com.masar.backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What the API returns for a seeker profile page: the user's name and email plus the
 * profile fields. It never exposes the User entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerProfileResponse {

    private String fullName;
    private String email;
    private String headline;
    private String location;
    private short yearsExperience;

    /**
     * Builds the response from the user and their saved profile.
     */
    public static SeekerProfileResponse from(User user, SeekerProfile profile) {
        return SeekerProfileResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .headline(profile.getHeadline())
                .location(profile.getLocation())
                .yearsExperience(profile.getYearsExperience())
                .build();
    }
}
