package com.nemouw.user.api;

import com.nemouw.user.identity.UserRepository;
import com.nemouw.user.identity.UserRepository.SummaryRow;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Who a user is, for other modules. Unknown ids are simply absent from the result. */
@Service
public class UserDirectory {
    private final UserRepository users;

    UserDirectory(UserRepository users) {
        this.users = users;
    }

    public Map<UUID, UserSummary> summaries(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();   // "IN ()" is invalid SQL
        return users.findSummaries(userIds).stream()
                .collect(Collectors.toMap(SummaryRow::id, UserDirectory::toSummary));
    }

    private static UserSummary toSummary(SummaryRow r) {
        return new UserSummary(r.id(), r.firstName(), r.lastName(), r.email(),
                UserRole.fromCode(r.roleCode()), UserStatus.fromCode(r.status()),
                r.createdAt().toInstant());
    }
}