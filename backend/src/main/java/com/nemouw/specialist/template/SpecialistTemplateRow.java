package com.nemouw.specialist.template;

import java.time.OffsetDateTime;
import java.util.UUID;

record SpecialistTemplateRow(UUID specialistId, String primaryColor, OffsetDateTime createdAt,
                             OffsetDateTime updatedAt) {
}