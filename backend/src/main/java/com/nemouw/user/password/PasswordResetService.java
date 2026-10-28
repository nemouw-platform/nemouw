package com.nemouw.user.password;

import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.mail.EmailSender;
import com.nemouw.common.util.TextNormalizer;
import com.nemouw.common.util.Validator;
import com.nemouw.user.identity.PasswordHasher;
import com.nemouw.user.identity.PasswordPolicy;
import com.nemouw.user.identity.UserRepository;
import com.nemouw.user.identity.UserRepository.EmailIdentity;
import com.nemouw.user.session.SessionService;
import com.nemouw.user.token.VerificationPurpose;
import com.nemouw.user.token.VerificationTokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import static com.nemouw.common.util.TextNormalizer.emailLocalPart;
import static com.nemouw.user.password.PasswordDtos.ForgotPasswordRequest;
import static com.nemouw.user.password.PasswordDtos.ResetPasswordRequest;

/**
 * Forgot-password and reset-password. Also, what an admin or syndicate invite uses
 * to set a first password, since the same token flow activates a pending account.
 */
@Service
class PasswordResetService {
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final VerificationTokens tokens;
    private final SessionService sessions;
    private final EmailSender emailSender;
    private final TransactionTemplate tx;

    PasswordResetService(UserRepository users, PasswordHasher hasher, VerificationTokens tokens,
                         SessionService sessions, EmailSender emailSender, TransactionTemplate tx) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.sessions = sessions;
        this.emailSender = emailSender;
        this.tx = tx;
    }

    void forgotPassword(ForgotPasswordRequest req) {
        new Validator().requiredEmail("email", req.email()).throwIfInvalid();
        String email = TextNormalizer.normalizeEmail(req.email());

        EmailIdentity identity = users.findByEmail(email).orElse(null);
        if (identity == null || identity.userStatus() == UserStatus.DELETED || identity.userStatus() == UserStatus.SUSPENDED) {
            return;   // silent, same 202
        }

        var issued = tx.execute(status ->
                tokens.issueResetToken(identity.userId(), identity.identifierId())
                        .orElseThrow(() -> Errors.tooManyRequests(Math.max(1,
                                tokens.secondsUntilAllowed(identity.identifierId(),
                                        VerificationPurpose.PASSWORD_RESET)))));
        LocaleCode locale = LocaleCode.orDefault(req.locale());
        emailSender.sendPasswordReset(email, issued.token(), tokens.resetTtl(), locale);
    }

    void resetPassword(ResetPasswordRequest req) {
        new Validator()
                .required("token", req.token())
                .required("new_password", req.newPassword())
                .throwIfInvalid();

        String token = req.token().trim();
        String email = tokens.findLiveResetEmail(token).orElseThrow(Errors::invalidOrExpiredCode);

        new Validator()
                .addAll(PasswordPolicy.check("new_password", req.newPassword(), emailLocalPart(email)))
                .throwIfInvalid();

        String passwordHash = hasher.hash(req.newPassword());

        tx.executeWithoutResult(status -> {
            // Any throw below rolls the delete back, so a failed reset keeps the token.
            var consumed = tokens.consumeResetToken(token).orElseThrow(Errors::invalidOrExpiredCode);

            UserStatus userStatus = users.lockStatus(consumed.userId()).orElse(null);
            if (userStatus == null || userStatus == UserStatus.DELETED || userStatus == UserStatus.SUSPENDED) {
                throw Errors.invalidOrExpiredCode();
            }

            users.replacePassword(consumed.userId(), passwordHash);
            if (userStatus == UserStatus.PENDING) {
                users.activate(consumed.userId(), consumed.identifierId());
            }
            tokens.revoke(consumed.userId(), VerificationPurpose.SIGNUP_OTP);
            sessions.revokeAllForUser(consumed.userId());
        });
    }
}