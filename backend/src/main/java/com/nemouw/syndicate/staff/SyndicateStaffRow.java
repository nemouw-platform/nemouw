package com.nemouw.syndicate.staff;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A staff membership. Who the person is lives in user: see UserDirectory.
 */
record SyndicateStaffRow(UUID id, UUID syndicateId, UUID userId, String title,
                         boolean isOwner, OffsetDateTime createdAt) {
}