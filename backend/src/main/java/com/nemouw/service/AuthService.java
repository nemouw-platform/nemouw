package com.nemouw.service;

import com.nemouw.database.Db;
import com.nemouw.enums.UserRole;
import com.nemouw.enums.VerificationPurpose;
import com.nemouw.http.ApiException;
import com.nemouw.http.validation.Validator;
import com.nemouw.model.AuthDtos.SignupRequest;
import com.nemouw.model.AuthDtos.SignupResponse;
import com.nemouw.repository.UserRepository;
import com.nemouw.repository.VerificationTokenRepository;
import org.jdbi.v3.core.Jdbi;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AuthService {
    /** Roles a visitor may pick for themselves at signup. Everything else is assigned by an admin. */
    private static final Set<UserRole> SELF_REGISTERABLE = EnumSet.of(UserRole.SPECIALIST);
    private static final int OTP_TTL_SECONDS = 600;   // 10 minutes

    private final Jdbi jdbi;
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final VerificationTokenRepository tokens;
    private final OtpService otp;
    private final EmailSender emailSender;


    public AuthService(Jdbi jdbi, UserRepository users, VerificationTokenRepository tokens,
                       PasswordHasher hasher, OtpService otp, EmailSender emailSender) {
        this.jdbi = jdbi;
        this.users = users;
        this.tokens = tokens;
        this.hasher = hasher;
        this.otp = otp;
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

    private static String emailLocalPart(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : null;
    }
}
