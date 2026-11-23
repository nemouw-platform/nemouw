package com.nemouw.user.management;

import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ManagementDtos {
    private ManagementDtos() {
    }

    public record AdminInviteRequest(String firstName, String lastName, String email) {
    }

    // PATCH has no DTO on purpose: omitted and explicit null must be told apart, so the body is a Map.

    public record UserDetail(UUID id, String firstName, String lastName, String email, UserRole role,
                             UserStatus userStatus, Instant createdAt, UUID countryId, String preferredLanguage,
                             Instant lastLoginAt, Instant deletedAt) {
        static UserDetail from(ManagedUserRow r) {
            return new UserDetail(r.id(), r.firstName(), r.lastName(), r.email(), r.role(), r.userStatus(),
                    r.createdAt().toInstant(), r.countryId(), r.preferredLanguage(),
                    instant(r.lastLoginAt()), instant(r.deletedAt()));
        }

        private static Instant instant(OffsetDateTime time) {
            return time == null ? null : time.toInstant();
        }
    }

    public record UserPage(List<UserDetail> data, int page, int pageSize, long total) {
    }
}