package com.nemouw.specialist.profile;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component("specialistReviewAccess")
public class SpecialistReviewAccess {
    private final SpecialistRepository specialists;

    SpecialistReviewAccess(SpecialistRepository specialists) {
        this.specialists = specialists;
    }

    public boolean isReviewer(UUID specialistId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) return false;
        return specialists.isStaffOfSyndicateOf(specialistId,
                UUID.fromString(Objects.requireNonNull(jwt.getSubject())));
    }
}