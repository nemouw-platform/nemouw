package com.nemouw.specialist.content;

import com.nemouw.common.enums.LocaleCode;

import java.time.Instant;
import java.util.UUID;

public final class SpecialistProfileDtos {
    private SpecialistProfileDtos() {
    }

    /** Full replacement: an omitted field is stored as null. */
    public record SpecialistProfileWriteRequest(String title, String headline, String bio) {
    }

    public record SpecialistProfileResponse(UUID specialistId, LocaleCode localeCode, String title, String headline,
                                            String bio, Instant createdAt, Instant updatedAt) {
        static SpecialistProfileResponse from(SpecialistProfileRow r) {
            return new SpecialistProfileResponse(r.specialistId(), LocaleCode.tryFrom(r.localeCode()).orElseThrow(),
                    r.title(), r.headline(), r.bio(), r.createdAt().toInstant(), r.updatedAt().toInstant());
        }
    }
}