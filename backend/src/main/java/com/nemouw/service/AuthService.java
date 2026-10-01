package com.nemouw.service;

import com.nemouw.database.Db;
import com.nemouw.enums.UserRole;
import com.nemouw.enums.UserStatus;
import com.nemouw.enums.VerificationPurpose;
import com.nemouw.error.ApiException;
import com.nemouw.error.Errors;
import com.nemouw.error.FieldErrors;
import com.nemouw.model.ClientInfo;
import com.nemouw.security.PasswordHasher;
import com.nemouw.security.TokenService;
import com.nemouw.validation.Validator;
import com.nemouw.model.AuthDtos.*;
import com.nemouw.repository.UserRepository;
import com.nemouw.repository.UserRepository.EmailIdentity;
import com.nemouw.repository.VerificationTokenRepository;
import com.nemouw.service.OtpIssuer.IssuedOtp;
import com.nemouw.util.TextNormalizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.*;

import static com.nemouw.Constants.*;
import static com.nemouw.error.Errors.invalidCredentials;

public class AuthService {
    /**
     * Roles a visitor may pick for themselves at signup. Everything else is assigned by an admin.
     */
    private static final Set<UserRole> SELF_REGISTERABLE = EnumSet.of(UserRole.SPECIALIST);

    private final Jdbi jdbi;
    private final UserRepository users;
    private final VerificationTokenRepository tokens;
    private final PasswordHasher hasher;
    private final OtpService otp;
    private final OtpIssuer otpIssuer;
    private final TokenService tokenService;
    private final EmailSender emailSender;

    private static final Logger log = LogManager.getLogger(AuthService.class);

    public AuthService(Jdbi jdbi, UserRepository users, VerificationTokenRepository tokens,
                       PasswordHasher hasher, OtpService otp, OtpIssuer otpIssuer,
                       TokenService tokenService, EmailSender emailSender) {
        this.jdbi = jdbi;
        this.users = users;
        this.tokens = tokens;
        this.hasher = hasher;
        this.otp = otp;
        this.otpIssuer = otpIssuer;
        this.tokenService = tokenService;
        this.emailSender = emailSender;
    }

    private record Registration(UUID userId, IssuedOtp otp) {
    }

    public SignupResponse signup(SignupRequest req) {
        UserRole role = UserRole.tryFrom(req.role()).orElse(null);

        new Validator()
                .check(role != null && SELF_REGISTERABLE.contains(role), FieldErrors.invalidRole("role"))
                .required("first_name", req.firstName())
                .required("last_name", req.lastName())
                .requiredEmail("email", req.email())
                .password("password", req.password(), emailLocalPart(req.email()))
                .throwIfInvalid();

        String email = TextNormalizer.normalize(req.email());
        String firstName = req.firstName().trim();
        String lastName = req.lastName().trim();
        String passwordHash = hasher.hash(req.password());   // slow, so done BEFORE holding a DB connection

        Registration registration;
        try {
            registration = jdbi.inTransaction(h -> users.findByEmail(h, email)
                    .map(existing -> restartPending(h, existing, firstName, lastName, passwordHash))
                    .orElseGet(() -> registerNew(h, role, firstName, lastName, email, passwordHash)));
        } catch (RuntimeException e) {
            if (Db.isUniqueViolation(e)) throw Errors.emailTaken();
            throw e;
        }

        IssuedOtp issued = registration.otp();
        emailSender.sendSignupOtp(email, issued.code(), issued.expiresAt());
        return new SignupResponse(registration.userId(), email, issued.expiresAt());
    }

    private Registration registerNew(Handle h, UserRole role, String firstName, String lastName,
                                     String email, String passwordHash) {
        UUID userId = users.insertUser(h, role, firstName, lastName);
        UUID identifierId = users.insertEmailIdentifier(h, userId, email);
        users.insertPassword(h, userId, passwordHash);
        return new Registration(userId, otpIssuer.issue(h, userId, identifierId));
    }

    /**
     * Signing up again with a still-pending email: the latest signup wins.
     * Credentials are overwritten and a fresh OTP replaces the old one, so whoever
     * controls the mailbox ends up with the password they chose.
     */
    private Registration restartPending(Handle h, EmailIdentity existing, String firstName, String lastName,
                                        String passwordHash) {
        UUID userId = existing.userId();

        // Lock first, then trust the locked status instead of the earlier unlocked read
        if (users.lockStatus(h, userId).orElse(null) != UserStatus.PENDING) {
            throw Errors.emailTaken();
        }
        if (otpIssuer.isThrottled(h, userId)) {
            throw Errors.tooManyRequests(otpIssuer.retryAfterSeconds());
        }

        users.updateName(h, userId, firstName, lastName);
        users.replacePassword(h, userId, passwordHash);
        return new Registration(userId, otpIssuer.issue(h, userId, existing.identifierId()));
    }

    public IssuedSession verifyOtp(VerifyOtpRequest req, ClientInfo client) {
        new Validator()
                .requiredEmail("email", req.email())
                .required("code", req.code())
                .check(req.code() == null || req.code().matches("\\d{" + OTP_CODE_DIGITS + "}"),
                        FieldErrors.invalidFormat("code", "must be " + OTP_CODE_DIGITS + " digits"))
                .throwIfInvalid();

        String email = TextNormalizer.normalize(req.email());

        // Unknown email gets the same answer as a wrong code, so this endpoint cannot be used to probe registrations
        EmailIdentity identity = jdbi.withHandle(h -> users.findByEmail(h, email))
                .orElseThrow(Errors::invalidOrExpiredCode);

        // tryVerify never throws on a bad code: throwing would roll back the spent attempt
        return jdbi.inTransaction(h -> tryVerify(h, identity, req.code(), client))
                .orElseThrow(Errors::invalidOrExpiredCode);
    }

    private Optional<IssuedSession> tryVerify(Handle h, EmailIdentity identity, String code, ClientInfo client) {
        var claimed = tokens.claimAttempt(h, identity.userId(), VerificationPurpose.SIGNUP_OTP, OTP_MAX_ATTEMPTS);

        // Order matters: activate runs last, and only for a matching code
        if (claimed.isEmpty()
                || identity.userStatus() != UserStatus.PENDING
                || !otp.matches(identity.userId(), code, claimed.get().tokenHash())
                || !users.activate(h, identity.userId(), identity.identifierId())) {
            return Optional.empty();
        }

        tokens.consume(h, claimed.get().id());
        return Optional.of(tokenService.issue(h, identity.userId(), identity.roleCode(), client));
    }

    public void resendOtp(ResendOtpRequest req) {
        new Validator().requiredEmail("email", req.email()).throwIfInvalid();
        String email = TextNormalizer.normalize(req.email());

        EmailIdentity identity = jdbi.withHandle(h -> users.findByEmail(h, email)).orElse(null);
        if (identity == null || identity.userStatus() != UserStatus.PENDING) {
            return;   // silent: same 202 regardless of whether the account exists
        }

        Optional<IssuedOtp> issued = jdbi.inTransaction(h -> reissueIfAllowed(h, identity));
        issued.ifPresent(o -> emailSender.sendSignupOtp(email, o.code(), o.expiresAt()));
    }

    public IssuedSession login(LoginRequest req, ClientInfo client) {
        new Validator()
                .required("identifier", req.identifier())
                .required("password", req.password())
                .throwIfInvalid();

        String identifier = TextNormalizer.normalize(req.identifier());
        var row = jdbi.withHandle(h -> users.findLoginByIdentifier(h, identifier)).orElse(null);

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

        return jdbi.inTransaction(h -> {
            users.touchLastLogin(h, row.id());
            return tokenService.issue(h, row.id(), row.roleCode(), client);
        });
    }

    public IssuedSession refresh(String refreshToken) {
        // A missing cookie means "not authenticated" (401), not a validation problem (422)
        if (refreshToken == null || refreshToken.isBlank()) throw Errors.invalidRefreshToken();
        String presented = refreshToken.trim();

        // Never throw inside the transaction: a revocation must commit
        Optional<IssuedSession> result = jdbi.inTransaction(h -> {
            Optional<IssuedSession> rotated = tokenService.rotate(h, presented);
            if (rotated.isEmpty()) {
                tokenService.revokeIfReplayed(h, presented).ifPresent(s ->
                        log.warn("refresh_token_reuse session={} user={}", s.sessionId(), s.userId()));
            }
            return rotated;
        });
        return result.orElseThrow(Errors::invalidRefreshToken);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        jdbi.useHandle(h -> tokenService.revokeByRefreshToken(h, refreshToken.trim()));
    }

    private Optional<IssuedOtp> reissueIfAllowed(Handle h, EmailIdentity identity) {
        UUID userId = identity.userId();
        if (users.lockStatus(h, userId).orElse(null) != UserStatus.PENDING || otpIssuer.isThrottled(h, userId)) {
            return Optional.empty();   // throttled or no longer pending: do nothing, still answer 202
        }
        return Optional.of(otpIssuer.issue(h, userId, identity.identifierId()));
    }

    private static String emailLocalPart(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : null;
    }
}