package com.nemouw.syndicate.management;

import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;

import java.time.Instant;
import java.util.UUID;

public final class SyndicateDtos {
    private SyndicateDtos() {
    }

    /** Public-safe: no contact or staff detail. */
    public record SyndicateSummary(UUID id, String name, UUID specialtyId, UUID countryId) {
        static SyndicateSummary from(Syndicate s) {
            return new SyndicateSummary(s.id(), s.name(), s.specialtyId(), s.countryId());
        }
    }

    public record SyndicateCreateRequest(String name, UUID specialtyId, UUID countryId, String contactEmail,
                                         String contactPhone, String ownerFirstName, String ownerLastName,
                                         String ownerEmail) {
    }

    /** On update, null means "leave unchanged". */
    public record SyndicateUpdateRequest(String name, String contactEmail, String contactPhone) {
    }

    public record SyndicateResponse(UUID id, String name, UUID specialtyId, UUID countryId, String contactEmail,
                                    String contactPhone, SyndicateStatus status, SyndicateStaffResponse owner,
                                    Instant createdAt) {
        static SyndicateResponse from(Syndicate s, SyndicateStaffResponse owner) {
            return new SyndicateResponse(s.id(), s.name(), s.specialtyId(), s.countryId(),
                    s.contactEmail(), s.contactPhone(), s.status(), owner, s.createdAt().toInstant());
        }
    }
}