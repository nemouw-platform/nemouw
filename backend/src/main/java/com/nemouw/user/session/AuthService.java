package com.nemouw.user.session;

import com.nemouw.user.identity.UserRepository;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.util.TextNormalizer;
import com.nemouw.common.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static com.nemouw.user.session.SessionDtos.LoginRequest;
import static com.nemouw.common.exception.Errors.invalidCredentials;
import com.nemouw.user.identity.PasswordHasher;

@Service
class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final SessionService sessions;
    private final TransactionTemplate tx;

    AuthService(UserRepository users, PasswordHasher hasher, SessionService sessions, TransactionTemplate tx) {
        this.users = users;
        this.hasher = hasher;
        this.sessions = sessions;
        this.tx = tx;
    }

    IssuedSession login(LoginRequest req, ClientInfo client) {
        new Validator()
                .required("identifier", req.identifier())
                .required("password", req.password())
                .throwIfInvalid();

        String identifier = TextNormalizer.normalizeEmail(req.identifier());
        var row = users.findLoginByIdentifier(identifier).orElse(null);

        // An account that has not chosen a password yet (verify-first signup not finished) has no hash,
        // so it lands in the first branch and looks exactly like an unknown account.
        boolean passwordOk;
        if (row == null || row.passwordHash() == null) {
            hasher.verifyDummy(req.password());      // burn the same time as a real check
            passwordOk = false;
        } else {
            passwordOk = hasher.verify(req.password(), row.passwordHash());
        }
        if (!passwordOk) throw invalidCredentials();

        ApiException blocked = switch (row.userStatus()) {
            case ACTIVE -> null;
            case PENDING -> Errors.accountNotVerified();
            case SUSPENDED -> Errors.accountSuspended();
            case DELETED -> invalidCredentials();
        };
        if (blocked != null) throw blocked;

        return tx.execute(status -> {
            users.touchLastLogin(row.id());
            return sessions.issue(row.id(), row.roleCode(), client);
        });
    }

    IssuedSession refresh(String refreshToken) {
        // A missing cookie means "not authenticated" (401), not a validation problem (422)
        if (refreshToken == null || refreshToken.isBlank()) throw Errors.invalidRefreshToken();
        String presented = refreshToken.trim();

        // Never throw inside the transaction
        Optional<IssuedSession> result = tx.execute(status -> {
            Optional<IssuedSession> rotated = sessions.rotate(presented);
            if (rotated.isEmpty()) {
                sessions.revokeIfReplayed(presented).ifPresent(s ->
                        log.warn("refresh_token_reuse session={} user={}", s.sessionId(), s.userId()));
            }
            return rotated;
        });
        return result.orElseThrow(Errors::invalidRefreshToken);
    }

    void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        sessions.revokeByRefreshToken(refreshToken.trim());
    }
}