package com.nemouw.user.management;

import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.user.api.UserSummary;

import java.time.OffsetDateTime;
import java.util.UUID;

record ManagedUserRow(UUID id, String firstName, String lastName, String email, String roleCode, String status,
                      UUID countryId, String preferredLanguage, OffsetDateTime createdAt,
                      OffsetDateTime lastLoginAt, OffsetDateTime deletedAt) {

    UserRole role() {
        return UserRole.fromCode(roleCode);
    }

    UserStatus userStatus() {
        return UserStatus.fromCode(status);
    }

    UserSummary summary() {
        return new UserSummary(id, firstName, lastName, email, role(), userStatus(), createdAt.toInstant());
    }
}