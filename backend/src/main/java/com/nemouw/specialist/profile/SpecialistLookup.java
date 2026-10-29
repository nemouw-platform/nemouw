package com.nemouw.specialist.profile;

import com.nemouw.common.exception.Errors;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Resolves the specialist behind an authenticated user. */
@Service
public class SpecialistLookup {
    private final SpecialistRepository specialists;

    SpecialistLookup(SpecialistRepository specialists) {
        this.specialists = specialists;
    }

    public UUID requireIdOf(UUID userId) {
        return specialists.findIdByUserId(userId).orElseThrow(Errors::onboardingRequired);
    }
}