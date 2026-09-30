package com.nemouw.service;

import com.nemouw.database.Db;
import com.nemouw.enums.UserRole;
import com.nemouw.enums.UserStatus;
import com.nemouw.enums.VerificationPurpose;
import com.nemouw.http.ApiException;
import com.nemouw.http.validation.Validator;
import com.nemouw.model.AuthDtos.*;
import com.nemouw.repository.UserRepository;
import com.nemouw.repository.UserRepository.EmailIdentity;
import com.nemouw.repository.VerificationTokenRepository;
import com.nemouw.service.OtpIssuer.IssuedOtp;
import com.nemouw.util.TextNormalizer;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.*;

public class AuthService {
    /**
     * Roles a visitor may pick for themselves at signup. Everything else is assigned by an admin.
     */
    private static final Set<UserRole> SELF_REGISTERABLE = EnumSet.of(UserRole.SPECIALIST);
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final Jdbi jdbi;
    private final UserRepository users;
    private final VerificationTokenRepository tokens;
    private final PasswordHasher hasher;
    private final OtpService otp;
    private final OtpIssuer otpIssuer;
    private final TokenService tokenService;
    private final EmailSender emailSender;

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
                .check(role != null && SELF_REGISTERABLE.contains(role), "role", "INVALID_ROLE",
                        "only 'specialist' can self-register")
                .required("first_name", req.firstName())
                .required("last_name", req.lastName())
                .required("email", req.email()).email("email", req.email())
                .required("password", req.password())
                .password("password", req.password(), emailLocalPart(req.email()))
                .throwIfInvalid();

        String email = TextNormalizer.email(req.email());
        String firstName = req.firstName().trim();
        String lastName = req.lastName().trim();
        String passwordHash = hasher.hash(req.password());   // slow, so done BEFORE holding a DB connection

        Registration registration;
        try {
            registration = jdbi.inTransaction(h -> users.findByEmail(h, email)
                    .map(existing -> restartPending(h, existing, firstName, lastName, passwordHash))
                    .orElseGet(() -> registerNew(h, role, firstName, lastName, email, passwordHash)));
        } catch (RuntimeException e) {
            if (Db.isUniqueViolation(e)) throw ApiException.conflict("EMAIL_TAKEN", "Email is already registered");
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
            throw ApiException.conflict("EMAIL_TAKEN", "Email is already registered");
        }
        if (otpIssuer.isThrottled(h, userId)) {
            throw ApiException.tooManyRequests(otpIssuer.retryAfterSeconds());
        }

        users.updateName(h, userId, firstName, lastName);
        users.replacePassword(h, userId, passwordHash);
        return new Registration(userId, otpIssuer.issue(h, userId, existing.identifierId()));
    }

    public AuthTokens verifyOtp(VerifyOtpRequest req, String ip, String userAgent) {
        new Validator()
                .required("email", req.email()).email("email", req.email())
                .required("code", req.code())
                .check(req.code() == null || req.code().matches("\\d{6}"), "code", "INVALID_FORMAT", "must be 6 digits")
                .throwIfInvalid();

        String email = TextNormalizer.email(req.email());

        // Unknown email gets the same answer as a wrong code, so this endpoint cannot be used to probe registrations
        EmailIdentity identity = jdbi.withHandle(h -> users.findByEmail(h, email))
                .orElseThrow(AuthService::invalidCode);

        // tryVerify never throws on a bad code: throwing would roll back the spent attempt
        return jdbi.inTransaction(h -> tryVerify(h, identity, req.code(), ip, userAgent))
                .orElseThrow(AuthService::invalidCode);
    }

    private Optional<AuthTokens> tryVerify(Handle h, EmailIdentity identity, String code, String ip, String userAgent) {
        var claimed = tokens.claimAttempt(h, identity.userId(), VerificationPurpose.SIGNUP_OTP, OTP_MAX_ATTEMPTS);

        // Order matters: activate runs last, and only for a matching code
        if (claimed.isEmpty()
                || identity.userStatus() != UserStatus.PENDING
                || !otp.matches(identity.userId(), code, claimed.get().tokenHash())
                || !users.activate(h, identity.userId(), identity.identifierId())) {
            return Optional.empty();
        }

        tokens.consume(h, claimed.get().id());
        return Optional.of(tokenService.issue(h, identity.userId(), identity.roleCode(), userAgent, ip));
    }

    public void resendOtp(ResendOtpRequest req) {
        new Validator().required("email", req.email()).email("email", req.email()).throwIfInvalid();
        String email = TextNormalizer.email(req.email());

        EmailIdentity identity = jdbi.withHandle(h -> users.findByEmail(h, email)).orElse(null);
        if (identity == null || identity.userStatus() != UserStatus.PENDING) {
            return;   // silent: same 202 whether or not the account exists
        }

        Optional<IssuedOtp> issued = jdbi.inTransaction(h -> reissueIfAllowed(h, identity));
        issued.ifPresent(o -> emailSender.sendSignupOtp(email, o.code(), o.expiresAt()));
    }

    private Optional<IssuedOtp> reissueIfAllowed(Handle h, EmailIdentity identity) {
        UUID userId = identity.userId();
        if (users.lockStatus(h, userId).orElse(null) != UserStatus.PENDING || otpIssuer.isThrottled(h, userId)) {
            return Optional.empty();   // throttled or no longer pending: do nothing, still answer 202
        }
        return Optional.of(otpIssuer.issue(h, userId, identity.identifierId()));
    }

    private static ApiException invalidCode() {
        return ApiException.badRequest("INVALID_OR_EXPIRED_TOKEN", "Code is invalid, expired, or locked");
    }

    private static String emailLocalPart(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : null;
    }
}