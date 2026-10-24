package com.masar.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Extra data that only a JOB_SEEKER has. One row per seeker: the primary key is
 * the user's id (@MapsId), so a user can never have two seeker profiles.
 */
@Entity
@Table(name = "seeker_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeekerProfile {

    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 150)
    private String headline;

    @Column(length = 120)
    private String location;

    @Column(nullable = false)
    @Builder.Default
    private short yearsExperience = 0;

    /** Path of the uploaded CV in the storage (null when there is none). */
    @Column(length = 255)
    private String cvPath;
}
