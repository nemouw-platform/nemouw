package com.nemouw.syndicate.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nemouw.user.api.UserSummary;

import java.time.Instant;
import java.util.UUID;

public final class SyndicateStaffDtos {
    private SyndicateStaffDtos() {
    }

    public record SyndicateStaffInviteRequest(String firstName, String lastName, String email, String title) {
    }

    public record SyndicateStaffUpdateRequest(String title) {
    }

    public record SyndicateStaffEmailRequest(String email) {
    }

    public record SyndicateOwnerChangeRequest(UUID staffId) {
    }

    public record SyndicateStaffResponse(UUID id, UUID syndicateId, UserSummary user, String title,
                                         @JsonProperty("is_owner") boolean owner, Instant createdAt) {
        static SyndicateStaffResponse from(SyndicateStaffRow r, UserSummary user) {
            return new SyndicateStaffResponse(r.id(), r.syndicateId(), user,
                    r.title(), r.isOwner(), r.createdAt().toInstant());
        }
    }
}