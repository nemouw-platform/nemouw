package com.nemouw.user.session;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.nemouw.user.AuthConstants.CLAIM_SCOPE;
import static com.nemouw.user.AuthConstants.CLAIM_SESSION_ID;
import static com.nemouw.user.AuthConstants.PASSWORD_SETUP_SCOPE;

@Component
class ActiveSessionValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2TokenValidatorResult REJECTED = OAuth2TokenValidatorResult.failure(
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The session is not active", null));

    private final SessionRepository sessions;

    ActiveSessionValidator(SessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String sessionId = jwt.getClaimAsString(CLAIM_SESSION_ID);
        boolean ok = sessionId == null
                ? PASSWORD_SETUP_SCOPE.equals(jwt.getClaimAsString(CLAIM_SCOPE))
                : isLive(sessionId, jwt.getSubject());
        return ok ? OAuth2TokenValidatorResult.success() : REJECTED;
    }

    private boolean isLive(String sessionId, String userId) {
        try {
            return sessions.isActive(UUID.fromString(sessionId), UUID.fromString(userId));
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;   // malformed sid / sub
        }
    }
}