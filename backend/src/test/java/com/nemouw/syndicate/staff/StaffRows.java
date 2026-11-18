package com.nemouw.syndicate.staff;

import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import com.nemouw.user.api.UserSummary;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

final class StaffRows {

    static final UUID SYNDICATE_ID = UUID.fromString("a1c4e7f0-3b52-4d96-8e1a-60f2b9d7c314");
    static final UUID STAFF_ID = UUID.fromString("0f3e9b2a-5c71-4d88-b6a4-21d7e8c05f93");
    static final UUID STAFF_USER_ID = UUID.fromString("7b92d4e1-0a63-4c57-9e18-f4a05c6b3d72");
    static final UUID OWNER_STAFF_ID = UUID.fromString("3d6a1f80-92b4-4e17-a5c3-8b0e7d2f4961");
    static final UUID OWNER_USER_ID = UUID.fromString("e41b8c07-6d29-4f53-8a90-1c5d3b7e2a68");
    private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-02-01T09:30:00Z");

    private StaffRows() {
    }

    static SyndicateStaffRow member() {
        return new SyndicateStaffRow(STAFF_ID, SYNDICATE_ID, STAFF_USER_ID, "Registrar", false, CREATED);
    }

    /** Owners are created without a title. */
    static SyndicateStaffRow owner() {
        return new SyndicateStaffRow(OWNER_STAFF_ID, SYNDICATE_ID, OWNER_USER_ID, null, true, CREATED);
    }

    static UserSummary summary(UUID userId) {
        return new UserSummary(userId, "Layla", "Khoury", "layla@order.org", UserRole.SYNDICATE_STAFF,
                UserStatus.ACTIVE, Instant.parse("2026-01-15T08:00:00Z"));
    }

    static SyndicateStaffResponse response(SyndicateStaffRow row) {
        return SyndicateStaffResponse.from(row, summary(row.userId()));
    }
}