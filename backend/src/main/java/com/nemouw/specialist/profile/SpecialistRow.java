package com.nemouw.specialist.profile;

import com.nemouw.common.enums.UserStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

record SpecialistRow(UUID id, UUID userId, UUID specialtyId, UUID syndicateId, String syndicateMemberNumber,
                     String username, String gender, String whatsappNumber, String primaryLanguage,
                     Integer yearsOfExperience, UUID profilePictureId, String profilePictureKey, UUID coverImageId,
                     String coverImageKey, Integer minAgeTreated, Integer maxAgeTreated, String verificationStatusCode,
                     OffsetDateTime reviewedAt, String rejectionReason, String userStatusCode,
                     BigDecimal sessionPriceMin, BigDecimal sessionPriceMax, String priceCurrency,
                     OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    SpecialistVerificationStatus verificationStatus() {
        return SpecialistVerificationStatus.fromCode(verificationStatusCode);
    }

    UserStatus userStatus() {
        return UserStatus.fromCode(userStatusCode);
    }
}