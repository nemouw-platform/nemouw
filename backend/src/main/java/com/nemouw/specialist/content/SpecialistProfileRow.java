package com.nemouw.specialist.content;

import java.time.OffsetDateTime;
import java.util.UUID;

record SpecialistProfileRow(UUID specialistId, String localeCode, String title, String headline, String bio,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}