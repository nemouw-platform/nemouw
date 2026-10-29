package com.nemouw.specialist.profile;

import java.time.Instant;
import java.util.UUID;

public final class SpecialistDtos {
    private SpecialistDtos() {
    }

    public record SpecialistOnboardingRequest(String username, UUID syndicateId, String syndicateMemberNumber) {
    }

    // PATCH has no DTO on purpose: omitted and explicit null must be told apart, so the body is a Map.

    public record SpecialistResponse(UUID id, UUID userId, UUID specialtyId, UUID syndicateId,
                                     String syndicateMemberNumber, String username, String gender,
                                     String whatsappNumber, String primaryLanguage, Integer yearsOfExperience,
                                     String profilePictureUrl, String coverImageUrl, Integer minAgeTreated,
                                     Integer maxAgeTreated, SpecialistVerificationStatus verificationStatus,
                                     Instant verifiedAt, Instant createdAt, Instant updatedAt) {
        static SpecialistResponse from(SpecialistRow r, MediaUrls urls) {
            return new SpecialistResponse(r.id(), r.userId(), r.specialtyId(), r.syndicateId(),
                    r.syndicateMemberNumber(), r.username(), r.gender(), r.whatsappNumber(), r.primaryLanguage(),
                    r.yearsOfExperience(), urls.urlFor(r.profilePictureKey()), urls.urlFor(r.coverImageKey()),
                    r.minAgeTreated(), r.maxAgeTreated(), r.verificationStatus(),
                    r.verifiedAt() == null ? null : r.verifiedAt().toInstant(),
                    r.createdAt().toInstant(), r.updatedAt().toInstant());
        }
    }
}