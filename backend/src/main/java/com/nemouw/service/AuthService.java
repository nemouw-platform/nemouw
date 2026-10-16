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
import com.nemouw.validation.Validator;
import com.nemouw.model.AuthDtos.*;
import com.nemouw.repository.UserRepository;
import com.nemouw.repository.UserRepository.EmailIdentity;
import com.nemouw.repository.VerificationTokenRepository;
import com.nemouw.service.VerificationTokenIssuer.IssuedOtp;
import com.nemouw.util.TextNormalizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.*;

import static com.nemouw.Constants.*;
import static com.nemouw.error.Errors.invalidCredentials;
import static com.nemouw.util.TextNormalizer.emailLocalPart;

public class AuthService {
    private static final Logger log = LogManager.getLogger(AuthService.class);

    private final Jdbi jdbi;
    private final UserRepository users;
    private final VerificationTokenRepository tokenRepo;
    private final PasswordHasher hasher;
    private final VerificationTokenService tokenService;
    private final VerificationTokenIssuer tokenIssuer;
    private final SessionService sessions;
    private final EmailSender emailSender;

    public AuthService(AuthDependencies d) {
        this.jdbi = d.jdbi();
        this.users = d.users();
        this.tokenRepo = d.tokenRepo();
        this.hasher = d.hasher();
        this.tokenService = d.tokenService();
        this.tokenIssuer = d.tokenIssuer();
        this.sessions = d.sessions();
        this.emailSender = d.emailSender();
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

        String email = TextNormalizer.normalizeEmail(req.email());
        String firstName = req.firstName().trim();
        String lastName = req.lastName().trim();

        EmailIdentity exist = jdbi.withHandle(h -> users.findByEmail(h, email)).orElse(null);
        if (exist != null) {
            if (exist.userStatus() != UserStatus.PENDING) throw Errors.emailTaken();
            int wait = jdbi.withHandle(h ->
                    tokenIssuer.secondsUntilAllowed(h, exist.identifierId(), VerificationPurpose.SIGNUP_OTP));
            if (wait > 0) throw Errors.tooManyRequests(wait);
        }

        String passwordHash = hasher.hash(req.password());

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
        emailSender.sendSignupOtp(email, issued.code());
        return new SignupResponse(registration.userId(), email, issued.expiresAt());
    }

    private Registration registerNew(Handle h, UserRole role, String firstName, String lastName,
                                     String email, String passwordHash) {
        UUID userId = users.insertUser(h, role, firstName, lastName);
        UUID identifierId = users.insertEmailIdentifier(h, userId, email);
        users.insertPassword(h, userId, passwordHash);
        return new Registration(userId,
                tokenIssuer.issueOtp(h, userId, identifierId, VerificationPurpose.SIGNUP_OTP).orElseThrow());
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

        users.updateName(h, userId, firstName, lastName);
        users.replacePassword(h, userId, passwordHash);
        IssuedOtp issued = tokenIssuer.issueOtp(h, userId, existing.identifierId(), VerificationPurpose.SIGNUP_OTP)
                .orElseThrow(() -> Errors.tooManyRequests(Math.max(1,
                        tokenIssuer.secondsUntilAllowed(h, existing.identifierId(), VerificationPurpose.SIGNUP_OTP))));
        return new Registration(userId, issued);
    }

    public IssuedSession verifyOtp(VerifyOtpRequest req, ClientInfo client) {
        new Validator()
                .requiredEmail("email", req.email())
                .required("code", req.code())
                .check(req.code() == null || req.code().matches("\\d{" + OTP_CODE_DIGITS + "}"),
                        FieldErrors.invalidFormat("code", "must be " + OTP_CODE_DIGITS + " digits"))
                .throwIfInvalid();

        String email = TextNormalizer.normalizeEmail(req.email());

        // Unknown email gets the same answer as a wrong code, so this endpoint cannot be used to probe registrations
        EmailIdentity identity = jdbi.withHandle(h -> users.findByEmail(h, email))
                .orElseThrow(Errors::invalidOrExpiredCode);

        // tryVerify never throws on a bad code: throwing would roll back the spent attempt
        return jdbi.inTransaction(h -> tryVerify(h, identity, req.code(), client))
                .orElseThrow(Errors::invalidOrExpiredCode);
    }

    private Optional<IssuedSession> tryVerify(Handle h, EmailIdentity identity, String code, ClientInfo client) {
        if (users.lockStatus(h, identity.userId()).orElse(null) != UserStatus.PENDING) {
            return Optional.empty();
        }

        var claimed = tokenRepo.claimAttempt(h, identity.userId(), VerificationPurpose.SIGNUP_OTP, OTP_MAX_ATTEMPTS);

        // Order matters: activate runs last, and only for a matching code
        if (claimed.isEmpty()
                || !tokenService.matches(identity.userId(), code, claimed.get().tokenHash())
                || !users.activate(h, identity.userId(), identity.identifierId())) {
            return Optional.empty();
        }

        tokenRepo.delete(h, claimed.get().id());
        return Optional.of(sessions.issue(h, identity.userId(), identity.roleCode(), client));
    }

    public void resendOtp(ResendOtpRequest req) {
        new Validator().requiredEmail("email", req.email()).throwIfInvalid();
        String email = TextNormalizer.normalizeEmail(req.email());

        EmailIdentity identity = jdbi.withHandle(h -> users.findByEmail(h, email)).orElse(null);
        if (identity == null || identity.userStatus() != UserStatus.PENDING) {
            return;   // silent: same 202 regardless of whether the account exists
        }

        Optional<IssuedOtp> issued = jdbi.inTransaction(h -> reissueIfAllowed(h, identity));
        issued.ifPresent(o -> emailSender.sendSignupOtp(email, o.code()));
    }

    private Optional<IssuedOtp> reissueIfAllowed(Handle h, EmailIdentity identity) {
        UUID userId = identity.userId();
        if (users.lockStatus(h, userId).orElse(null) != UserStatus.PENDING) return Optional.empty();
        return tokenIssuer.issueOtp(h, userId, identity.identifierId(), VerificationPurpose.SIGNUP_OTP);
    }

    public IssuedSession login(LoginRequest req, ClientInfo client) {
        new Validator()
                .required("identifier", req.identifier())
                .required("password", req.password())
                .throwIfInvalid();

        String identifier = TextNormalizer.normalizeEmail(req.identifier());
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
            return sessions.issue(h, row.id(), row.roleCode(), client);
        });
    }

    public IssuedSession refresh(String refreshToken) {
        // A missing cookie means "not authenticated" (401), not a validation problem (422)
        if (refreshToken == null || refreshToken.isBlank()) throw Errors.invalidRefreshToken();
        String presented = refreshToken.trim();

        // Never throw inside the transaction
        Optional<IssuedSession> result = jdbi.inTransaction(h -> {
            Optional<IssuedSession> rotated = sessions.rotate(h, presented);
            if (rotated.isEmpty()) {
                sessions.revokeIfReplayed(h, presented).ifPresent(s ->
                        log.warn("refresh_token_reuse session={} user={}", s.sessionId(), s.userId()));
            }
            return rotated;
        });
        return result.orElseThrow(Errors::invalidRefreshToken);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        jdbi.useHandle(h -> sessions.revokeByRefreshToken(h, refreshToken.trim()));
    }
}