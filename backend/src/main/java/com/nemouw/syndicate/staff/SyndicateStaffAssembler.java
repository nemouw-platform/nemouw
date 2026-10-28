package com.nemouw.syndicate.staff;

import com.nemouw.user.api.UserDirectory;
import com.nemouw.user.api.UserSummary;
import com.nemouw.syndicate.staff.SyndicateStaffDtos.SyndicateStaffResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Turns staff rows into responses by fetching the people from the user module. */
@Component
class SyndicateStaffAssembler {
    private final UserDirectory users;

    SyndicateStaffAssembler(UserDirectory users) {
        this.users = users;
    }

    SyndicateStaffResponse toResponse(SyndicateStaffRow row) {
        return toResponses(List.of(row)).getFirst();
    }

    List<SyndicateStaffResponse> toResponses(List<SyndicateStaffRow> rows) {
        Map<UUID, UserSummary> people = users.summaries(rows.stream().map(SyndicateStaffRow::userId).toList());
        return rows.stream().map(r -> SyndicateStaffResponse.from(r, person(people, r))).toList();
    }

    private static UserSummary person(Map<UUID, UserSummary> people, SyndicateStaffRow row) {
        UserSummary user = people.get(row.userId());
        if (user == null) throw new IllegalStateException("Staff " + row.id() + " has no user " + row.userId());
        return user;
    }
}