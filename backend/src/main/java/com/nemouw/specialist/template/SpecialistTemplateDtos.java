package com.nemouw.specialist.template;

import java.time.Instant;
import java.util.UUID;

public final class SpecialistTemplateDtos {
    private SpecialistTemplateDtos() {
    }

    // PUT has no request DTO on purpose: omitted and explicit null must be told apart, so the body is a Map.

    public record SpecialistTemplateResponse(UUID specialistId, String primaryColor, Instant createdAt,
                                             Instant updatedAt) {
        static SpecialistTemplateResponse from(SpecialistTemplateRow r) {
            return new SpecialistTemplateResponse(r.specialistId(), r.primaryColor(),
                    r.createdAt().toInstant(), r.updatedAt().toInstant());
        }
    }
}