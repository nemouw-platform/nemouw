package com.nemouw.syndicate.access;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Method-security entry point for syndicate rules. Spring Security reaches it only through the
 * {@link AdminOrSyndicateOwner} and {@link AdminOrSyndicateStaff} meta-annotations, whose SpEL calls this bean by
 * name ({@code @syndicateAccess}). Keep the bean name and the two method names in step with those expressions.
 * Public because SpEL invokes the methods reflectively from another package.
 */
@Component("syndicateAccess")
public class SyndicateAccess {
    private final SyndicateMembershipRepository membership;

    SyndicateAccess(SyndicateMembershipRepository membership) {
        this.membership = membership;
    }

    public boolean isStaffOf(UUID syndicateId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) return false;
        return membership.isStaff(syndicateId, UUID.fromString(Objects.requireNonNull(jwt.getSubject())));
    }

    public boolean isOwnerOf(UUID syndicateId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) return false;
        return membership.isOwner(syndicateId, UUID.fromString(Objects.requireNonNull(jwt.getSubject())));
    }
}