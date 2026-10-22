package com.nemouw.auth.service;

import com.nemouw.auth.model.ClientInfo;
import com.nemouw.auth.model.IssuedSession;
import com.nemouw.auth.model.IssuedSetupToken;
import com.nemouw.auth.repository.UserRepository;
import com.nemouw.auth.repository.UserRepository.EmailIdentity;
import com.nemouw.auth.repository.VerificationTokenRepository;
import com.nemouw.auth.service.VerificationTokenIssuer.IssuedOtp;
import com.nemouw.enums.LocaleCode;
import com.nemouw.enums.UserRole;
import com.nemouw.enums.UserStatus;
import com.nemouw.enums.VerificationPurpose;
import com.nemouw.error.Errors;
import com.nemouw.error.FieldErrors;
import com.nemouw.mail.EmailSender;
import com.nemouw.util.TextNormalizer;
import com.nemouw.validation.Validator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static com.nemouw.auth.AuthConstants.*;
import static com.nemouw.auth.dto.AuthDtos.*;
import static com.nemouw.util.TextNormalizer.emailLocalPart;


@Service
public class SignupService {
    private final UserRepository users;
    private final VerificationTokenRepository tokenRepo;
    private final PasswordHasher hasher;
    private final VerificationTokenService tokenService;
    private final VerificationTokenIssuer tokenIssuer;
    private final SetupTokenService setupTokens;
    private final SessionService sessions;
    private final EmailSender emailSender;
    private final TransactionTemplate tx;

    public SignupService(UserRepository users, VerificationTokenRepository tokenRepo, PasswordHasher hasher,
                         VerificationTokenService tokenService, VerificationTokenIssuer tokenIssuer,
                         SetupTokenService setupTokens, SessionService sessions, EmailSender emailSender,
                         TransactionTemplate tx) {
        this.users = users;
        this.tokenRepo = tokenRepo;
        this.hasher = hasher;
        this.tokenService = tokenService;
        this.tokenIssuer = tokenIssuer;
        this.setupTokens = setupTokens;
        this.sessions = sessions;
        this.emailSender = emailSender;
        this.tx = tx;
    }

    private record Registration(UUID userId, IssuedOtp otp) {
    }

    // ---- Step 1: signup ----

    public SignupResponse signup(SignupRequest req) {
        UserRole role = UserRole.tryFrom(req.role()).orElse(null);

        new Validator()
                .check(role != null && SELF_REGISTERABLE.contains(role), FieldErrors.invalidRole("role"))
                .required("first_name", req.firstName())
                .required("last_name", req.lastName())
                .requiredEmail("email", req.email())
                .throwIfInvalid();

        String email = TextNormalizer.normalizeEmail(req.email());
        String firstName = req.firstName().trim();
        String lastName = req.lastName().trim();

        EmailIdentity exist = users.findByEmail(email).orElse(null);
        if (exist != null) {
            if (exist.userStatus() != UserStatus.PENDING) throw Errors.emailTaken();
            int wait = tokenIssuer.secondsUntilAllowed(exist.identifierId(), VerificationPurpose.SIGNUP_OTP);
            if (wait > 0) throw Errors.tooManyRequests(wait);
        }

        Registration registration;
        try {
            registration = tx.execute(status -> users.findByEmail(email)
                    .map(this::restartPending)
                    .orElseGet(() -> registerNew(role, firstName, lastName, email)));
        } catch (DuplicateKeyException e) {
            throw Errors.emailTaken();   // two signups raced for the same email
        }

        IssuedOtp issued = registration.otp();
        emailSender.sendSignupOtp(email, issued.code(), LocaleCode.orDefault(req.locale()));
        return new SignupResponse(registration.userId(), email, issued.expiresAt());
    }

    private Registration registerNew(UserRole role, String firstName, String lastName, String email) {
        UUID userId = users.insertUser(role, firstName, lastName);
        UUID identifierId = users.insertEmailIdentifier(userId, email);
        return new Registration(userId,
                tokenIssuer.issueOtp(userId, identifierId, VerificationPurpose.SIGNUP_OTP).orElseThrow());
    }

    /**
     * Signing up again with a still-pending email only re-issues the OTP. The stored name is NOT touched:
     * the OTP goes to the mailbox owner, so an anonymous signup must not be able to rewrite the account that
     * owner is about to claim. (There is no password to protect any more, but the same rule keeps the profile safe.)
     */
    private Registration restartPending(EmailIdentity existing) {
        UUID userId = existing.userId();

        // Lock first, then trust the locked status instead of the earlier unlocked read
        if (users.lockStatus(userId).orElse(null) != UserStatus.PENDING) {
            throw Errors.emailTaken();
        }

        IssuedOtp issued = tokenIssuer.issueOtp(userId, existing.identifierId(), VerificationPurpose.SIGNUP_OTP)
                .orElseThrow(() -> Errors.tooManyRequests(Math.max(1,
                        tokenIssuer.secondsUntilAllowed(existing.identifierId(), VerificationPurpose.SIGNUP_OTP))));
        return new Registration(userId, issued);
    }

    // ---- Step 2: verify the mailbox ----

    public IssuedSetupToken verifyOtp(VerifyOtpRequest req) {
        String code = req.code() == null ? null : req.code().trim();
        new Validator()
                .requiredEmail("email", req.email())
                .required("code", code)
                .check(code == null || code.matches("\\d{" + OTP_CODE_DIGITS + "}"),
                        FieldErrors.invalidFormat("code", "must be " + OTP_CODE_DIGITS + " digits"))
                .throwIfInvalid();

        String email = TextNormalizer.normalizeEmail(req.email());

        // Unknown email gets the same answer as a wrong code, so this endpoint cannot be used to probe registrations
        EmailIdentity identity = users.findByEmail(email)
                .orElseThrow(Errors::invalidOrExpiredCode);

        // consumeOtp never throws on a bad code: throwing would roll back the spent attempt
        if (!Boolean.TRUE.equals(tx.execute(status -> consumeOtp(identity, code)))) {
            throw Errors.invalidOrExpiredCode();
        }
        return setupTokens.issue(identity.userId());
    }

    private boolean consumeOtp(EmailIdentity identity, String code) {
        if (users.lockStatus(identity.userId()).orElse(null) != UserStatus.PENDING) {
            return false;
        }

        var claimed = tokenRepo.claimAttempt(identity.userId(), VerificationPurpose.SIGNUP_OTP, OTP_MAX_ATTEMPTS);
        if (claimed.isEmpty() || !tokenService.matches(identity.userId(), code, claimed.get().tokenHash())) {
            return false;
        }

        tokenRepo.delete(claimed.get().id());          // a code works once
        users.markVerified(identity.identifierId());   // the account stays PENDING until a password is set
        return true;
    }

    public void resendOtp(ResendOtpRequest req) {
        new Validator().requiredEmail("email", req.email()).throwIfInvalid();
        String email = TextNormalizer.normalizeEmail(req.email());

        EmailIdentity identity = users.findByEmail(email).orElse(null);
        if (identity == null || identity.userStatus() != UserStatus.PENDING) {
            return;   // silent: same 202 regardless of whether the account exists
        }

        Optional<IssuedOtp> issued = tx.execute(status -> reissue(identity));
        LocaleCode locale = LocaleCode.orDefault(req.locale());
        issued.ifPresent(o -> emailSender.sendSignupOtp(email, o.code(), locale));
    }

    /**
     * Empty means the account is no longer pending (silent 202, as for unknown emails).
     * Throttled throws 429 with Retry-After, same as forgot-password. Nothing was written when it throws,
     * so the rollback loses nothing.
     */
    private Optional<IssuedOtp> reissue(EmailIdentity identity) {
        UUID userId = identity.userId();
        if (users.lockStatus(userId).orElse(null) != UserStatus.PENDING) return Optional.empty();
        return Optional.of(tokenIssuer.issueOtp(userId, identity.identifierId(), VerificationPurpose.SIGNUP_OTP)
                .orElseThrow(() -> Errors.tooManyRequests(Math.max(1,
                        tokenIssuer.secondsUntilAllowed(identity.identifierId(), VerificationPurpose.SIGNUP_OTP)))));
    }

    // ---- Step 3: choose the password ----

    /**
     * @param userId the subject of the already-authenticated setup token (Spring Security checked signature,
     *               expiry and the password_setup scope before this runs)
     */
    public IssuedSession setPassword(UUID userId, SetPasswordRequest req, ClientInfo client) {
        // Cheap guard before Argon2. A completed, suspended or deleted account cannot use a setup token.
        EmailIdentity identity = users.findPrimaryEmail(userId)
                .filter(i -> i.userStatus() == UserStatus.PENDING)
                .orElseThrow(Errors::invalidAccessToken);

        new Validator()
                .password("password", req.password(), emailLocalPart(identity.email()))
                .throwIfInvalid();

        String passwordHash = hasher.hash(req.password());   // Argon2 stays outside the transaction

        return tx.execute(status -> {
            // The lock is what makes the token single use: of two racing requests, the second sees ACTIVE.
            if (users.lockStatus(userId).orElse(null) != UserStatus.PENDING) {
                throw Errors.invalidAccessToken();
            }
            users.replacePassword(userId, passwordHash);
            users.activate(userId);
            users.touchLastLogin(userId);
            return sessions.issue(userId, identity.roleCode(), client);
        });
    }
}