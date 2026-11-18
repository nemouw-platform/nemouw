package com.nemouw.specialist.profile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.nemouw.support.Fixtures.NOW;
import static com.nemouw.support.Fixtures.USER_ID;

final class SpecialistRows {

    static final UUID SPECIALIST_ID = UUID.fromString("5d0a7c34-81b2-4e69-a3f7-2c9e6b14d085");
    static final UUID SYNDICATE_ID = UUID.fromString("a1c4e7f0-3b52-4d96-8e1a-60f2b9d7c314");
    static final UUID OTHER_SYNDICATE_ID = UUID.fromString("c8d29b41-6f07-4a3e-b5d1-9e40a7f2c685");
    private static final UUID SPECIALTY_ID = UUID.fromString("e2f5a8c1-7d34-4b60-9a1e-3c8d5f0b7a42");

    private SpecialistRows() {
    }

    /** Username dr.amira, member number M-1001, ages 5 to 17, no prices, in the given verification state. */
    static SpecialistRow row(SpecialistVerificationStatus status) {
        OffsetDateTime now = NOW.atOffset(ZoneOffset.UTC);
        return new SpecialistRow(SPECIALIST_ID, USER_ID, SPECIALTY_ID, SYNDICATE_ID, "M-1001", "dr.amira",
                null, null, null, null, null, null, null, null, 5, 17, status.code(),
                null, null, "active", null, null, "USD", now, now);
    }

    static SpecialistRow pending() {
        return row(SpecialistVerificationStatus.PENDING);
    }
}