package com.nemouw.specialist.credential;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Method-security entry point for {@link AdminOrCredentialReviewer}. Public because SpEL calls it by bean name
 * from another package; keep the bean name and method name in step with the annotation.
 */
@Component("credentialReviewAccess")
public class CredentialReviewAccess {
    private final CredentialRepository credentials;

    CredentialReviewAccess(CredentialRepository credentials) {
        this.credentials = credentials;
    }

    public boolean isReviewer(UUID credentialId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) return false;
        return credentials.isStaffOfCredentialSyndicate(credentialId,
                UUID.fromString(Objects.requireNonNull(jwt.getSubject())));
    }
}