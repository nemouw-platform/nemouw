package com.nemouw.specialist.profile;

import java.time.OffsetDateTime;
import java.util.UUID;

record SpecialistRow(UUID id, UUID userId, UUID specialtyId, UUID syndicateId, String syndicateMemberNumber,
                     String username, String gender, String whatsappNumber, String primaryLanguage,
                     Integer yearsOfExperience, UUID profilePictureId, String profilePictureKey,
                     UUID coverImageId, String coverImageKey, Integer minAgeTreated, Integer maxAgeTreated,
                     String verificationStatusCode, OffsetDateTime verifiedAt, OffsetDateTime createdAt,
                     OffsetDateTime updatedAt) {
    SpecialistVerificationStatus verificationStatus() {
        return SpecialistVerificationStatus.fromCode(verificationStatusCode);
    }
}