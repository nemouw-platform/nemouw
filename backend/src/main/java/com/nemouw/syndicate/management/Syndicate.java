package com.nemouw.syndicate.management;

import java.time.OffsetDateTime;
import java.util.UUID;

record Syndicate(UUID id, String name, UUID specialtyId, UUID countryId, String contactEmail,
                 String contactPhone, String statusCode, OffsetDateTime createdAt) {
    public SyndicateStatus status() {
        return SyndicateStatus.fromCode(statusCode);
    }
}