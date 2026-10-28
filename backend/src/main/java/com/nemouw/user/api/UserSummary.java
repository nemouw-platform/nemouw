package com.nemouw.user.api;

import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

/** Account-level view of a user, shared by every module that returns one (syndicate staff, admins, ...). */
public record UserSummary(UUID id, String firstName, String lastName, String email, UserRole role,
                          UserStatus userStatus, Instant createdAt) {
}