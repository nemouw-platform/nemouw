package com.nemouw.user.signup;

import com.nemouw.user.identity.PasswordHasher;
import com.nemouw.user.identity.PasswordPolicy;
import com.nemouw.user.session.ClientInfo;
import com.nemouw.user.session.IssuedSession;
import com.nemouw.user.identity.UserRepository;
import com.nemouw.user.identity.UserRepository.EmailIdentity;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.user.session.SessionService;
import com.nemouw.user.token.VerificationPurpose;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.mail.EmailSender;
import com.nemouw.common.util.TextNormalizer;
import com.nemouw.common.util.Validator;
import com.nemouw.user.token.VerificationTokens;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import  com.nemouw.user.token.VerificationTokens.IssuedOtp;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static com.nemouw.user.AuthConstants.*;
import static com.nemouw.user.signup.SignupDtos.*;
import static com.nemouw.common.util.TextNormalizer.emailLocalPart;
import static com.nemouw.common.util.ValidationLimits.NAME_MAX_LENGTH;

@Service
class SignupService {
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final VerificationTokens tokens;
    private final SetupTokenService setupTokens;
    private final SessionService sessions;
    private final EmailSender emailSender;
    private final TransactionTemplate tx;

    SignupService(UserRepository users, PasswordHasher hasher, VerificationTokens tokens,
                  SetupTokenService setupTokens, SessionService sessions, EmailSender emailSender,
                  TransactionTemplate tx) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.setupTokens = setupTokens;
        this.sessions = sessions;
        this.emailSender = emailSender;
        this.tx = tx;
    }

    private record Registration(UUID userId, IssuedOtp otp) {
    }

    SignupResponse signup(SignupRequest req) {
        UserRole role = UserRole.tryFrom(req.role()).orElse(null);

        new Validator()
                .check(role != null && SELF_REGISTERABLE.contains(role), FieldErrors.invalidRole("role"))
                .required("first_name", req.firstName())
                .maxLength("first_name", req.firstName(), NAME_MAX_LENGTH)
                .required("last_name", req.lastName())
                .maxLength("last_name", req.lastName(), NAME_MAX_LENGTH)
                .requiredEmail("email", req.email())
                .throwIfInvalid();

        String email = TextNormalizer.normalizeEmail(req.email());
        String firstName = req.firstName().trim();
        String lastName = req.lastName().trim();

        EmailIdentity exist = users.findByEmail(email).orElse(null);
        if (exist != null) {
            if (exist.userStatus() != UserStatus.PENDING) throw Errors.emailTaken();
            int wait = tokens.secondsUntilAllowed(exist.identifierId(), VerificationPurpose.SIGNUP_OTP);
            if (wait > 0) throw Errors.tooManyRequests(wait);
        }

        Registration registration;
        try {
            registration = tx.execute(status -> users.findByEmail(email)
                    .map(existing -> restartPending(existing, firstName, lastName))
                    .orElseGet(() -> registerNew(role, firstName, lastName, email)));
        } catch (DuplicateKeyException e) {
            throw Errors.emailTaken();   // two signups raced for the same email
        }

        IssuedOtp issued = registration.otp();
        emailSender.sendSignupOtp(email, issued.code(), Duration.ofSeconds(OTP_TTL_SECONDS), LocaleCode.orDefault(req.locale()));
        return new SignupResponse(email, issued.expiresAt());
    }

    private Registration registerNew(UserRole role, String firstName, String lastName, String email) {
        UUID userId = users.insertUser(role, firstName, lastName);
        UUID identifierId = users.insertEmailIdentifier(userId, email);
        return new Registration(userId,
                tokens.issueSignupOtp(userId, identifierId).orElseThrow());
    }

    /**
     * Signing up again with a still-pending email re-issues the OTP and rewrites the stored name with the one
     * just submitted, so the person who actually owns the mailbox (and receives the code) ends up with the name
     * they typed last. Only a PENDING account is touched: the status is re-read under the row lock, and the name
     * update shares the transaction with the OTP issue, so a throttled attempt changes nothing.
     */
    private Registration restartPending(EmailIdentity existing, String firstName, String lastName) {
        UUID userId = existing.userId();

        // Lock first, then trust the locked status instead of the earlier unlocked read
        if (users.lockStatus(userId).orElse(null) != UserStatus.PENDING) {
            throw Errors.emailTaken();
        }

        IssuedOtp issued = tokens.issueSignupOtp(userId, existing.identifierId())
                .orElseThrow(() -> Errors.tooManyRequests(Math.max(1,
                        tokens.secondsUntilAllowed(existing.identifierId(), VerificationPurpose.SIGNUP_OTP))));
        users.updateName(userId, firstName, lastName);
        return new Registration(userId, issued);
    }

    IssuedSetupToken verifyOtp(VerifyOtpRequest req) {
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
        if (!tokens.consumeSignupOtp(identity.userId(), code)) {
            return false;
        }
        users.markVerified(identity.identifierId());   // the account stays PENDING until a password is set
        return true;
    }

    void resendOtp(ResendOtpRequest req) {
        new Validator().requiredEmail("email", req.email()).throwIfInvalid();
        String email = TextNormalizer.normalizeEmail(req.email());

        EmailIdentity identity = users.findByEmail(email).orElse(null);
        if (identity == null || identity.userStatus() != UserStatus.PENDING) {
            return;   // silent: same 202 regardless of whether the account exists
        }

        Optional<IssuedOtp> issued = tx.execute(status -> reissue(identity));
        LocaleCode locale = LocaleCode.orDefault(req.locale());
        issued.ifPresent(o -> emailSender.sendSignupOtp(email, o.code(), Duration.ofSeconds(OTP_TTL_SECONDS), locale));
    }

    /**
     * Empty means the account is no longer pending (silent 202, as for unknown emails).
     * Throttled throws 429 with Retry-After, same as forgot-password. Nothing was written when it throws,
     * so the rollback loses nothing.
     */
    private Optional<IssuedOtp> reissue(EmailIdentity identity) {
        UUID userId = identity.userId();
        if (users.lockStatus(userId).orElse(null) != UserStatus.PENDING) return Optional.empty();
        return Optional.of(tokens.issueSignupOtp(userId, identity.identifierId())
                .orElseThrow(() -> Errors.tooManyRequests(Math.max(1,
                        tokens.secondsUntilAllowed(identity.identifierId(), VerificationPurpose.SIGNUP_OTP)))));
    }

    /**
     * @param userId the subject of the already-authenticated setup token (Spring Security checked signature,
     *               expiry and the password_setup scope before this runs)
     */
    IssuedSession setPassword(UUID userId, SetPasswordRequest req, ClientInfo client) {
        // Cheap guard before Argon2. A completed, suspended or deleted account cannot use a setup token.
        EmailIdentity identity = users.findPrimaryEmail(userId)
                .filter(i -> i.userStatus() == UserStatus.PENDING)
                .orElseThrow(Errors::invalidAccessToken);

        new Validator()
                .required("password", req.password())
                .addAll(PasswordPolicy.check("password", req.password(), emailLocalPart(identity.email())))
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