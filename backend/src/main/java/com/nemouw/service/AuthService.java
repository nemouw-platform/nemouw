package com.nemouw.service;

import com.nemouw.database.Db;
import com.nemouw.enums.UserRole;
import com.nemouw.enums.VerificationPurpose;
import com.nemouw.http.ApiException;
import com.nemouw.http.validation.Validator;
import com.nemouw.model.AuthDtos.*;
import com.nemouw.repository.UserRepository;
import com.nemouw.repository.VerificationTokenRepository;
import org.jdbi.v3.core.Jdbi;

import java.time.OffsetDateTime;
import java.util.*;

public class AuthService {
    /** Roles a visitor may pick for themselves at signup. Everything else is assigned by an admin. */
    private static final Set<UserRole> SELF_REGISTERABLE = EnumSet.of(UserRole.SPECIALIST);
    private static final int OTP_TTL_SECONDS = 600;   // 10 minutes
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final Jdbi jdbi;
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final VerificationTokenRepository tokens;
    private final OtpService otp;
    private final EmailSender emailSender;
    private final TokenService tokenService;


    public AuthService(Jdbi jdbi, UserRepository users, VerificationTokenRepository tokens,
                       PasswordHasher hasher, OtpService otp, TokenService tokenService,
                       EmailSender emailSender) {
        this.jdbi = jdbi;
        this.users = users;
        this.tokens = tokens;
        this.hasher = hasher;
        this.otp = otp;
        this.tokenService = tokenService;
        this.emailSender = emailSender;
    }

    private record Created(UUID userId, OffsetDateTime expiresAt) {}

    public SignupResponse signup(SignupRequest req) {
        UserRole role = UserRole.tryFrom(req.role()).orElse(null);

        new Validator()
                .check(role != null && SELF_REGISTERABLE.contains(role), "role", "INVALID_ROLE",
                        "only 'specialist' can self-register")
                .required("first_name", req.firstName())
                .required("last_name", req.lastName())
                .required("email", req.email()).email("email", req.email())
                .required("password", req.password())
                .password("password", req.password(), emailLocalPart(req.email()))
                .throwIfInvalid();

        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String passwordHash = hasher.hash(req.password());   // slow, so done BEFORE holding a DB connection

        String code = otp.generateCode();

        Created created;
        try {
            created = jdbi.inTransaction(h -> {
                assert role != null;
                UUID userId = users.insertUser(h, role, req.firstName().trim(), req.lastName().trim());
                UUID identifierId = users.insertEmailIdentifier(h, userId, email);
                users.insertPassword(h, userId, passwordHash);
                OffsetDateTime expiresAt = tokens.insert(h, userId, identifierId, VerificationPurpose.SIGNUP_OTP,
                        otp.hash(userId, code), OTP_TTL_SECONDS);
                return new Created(userId, expiresAt);
            });
        } catch (RuntimeException e) {
            if (Db.isUniqueViolation(e)) throw ApiException.conflict("EMAIL_TAKEN", "Email is already registered");
            throw e;
        }

        emailSender.sendSignupOtp(email, code, created.expiresAt());
        return new SignupResponse(created.userId(), email, created.expiresAt());
    }

    public AuthTokens verifyOtp(VerifyOtpRequest req, String ip, String userAgent) {
        new Validator()
                .required("email", req.email()).email("email", req.email())
                .required("code", req.code())
                .check(req.code() == null || req.code().matches("\\d{6}"), "code", "INVALID_FORMAT", "must be 6 digits")
                .throwIfInvalid();

        String email = req.email().trim().toLowerCase(Locale.ROOT);
        var identity = jdbi.withHandle(h -> users.findByEmail(h, email)).orElse(null);
        if (identity == null) {
            // Same answer as a wrong or expired code, so this endpoint cannot be used to test whether an email is registered
            throw ApiException.badRequest("INVALID_OR_EXPIRED_TOKEN", "Code is invalid, expired, or locked");
        }

        // A wrong code must NOT throw in here: that would roll back the spent attempt.
        // Return empty, let the transaction commit, then throw below.
        Optional<AuthTokens> result = jdbi.inTransaction(h -> {
            var claimed = tokens.claimAttempt(h, identity.userId(), VerificationPurpose.SIGNUP_OTP, OTP_MAX_ATTEMPTS);
            if (claimed.isEmpty()
                    || !"pending".equals(identity.status())
                    || !otp.matches(identity.userId(), req.code(), claimed.get().tokenHash())) {
                return Optional.empty();
            }
            tokens.consume(h, claimed.get().id());
            users.activate(h, identity.userId(), identity.identifierId());
            return Optional.of(tokenService.issue(h, identity.userId(), identity.roleCode(), userAgent, ip));
        });

        return result.orElseThrow(() ->
                ApiException.badRequest("INVALID_OR_EXPIRED_TOKEN", "Code is invalid, expired, or locked"));
    }

    private static String emailLocalPart(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : null;
    }
}
