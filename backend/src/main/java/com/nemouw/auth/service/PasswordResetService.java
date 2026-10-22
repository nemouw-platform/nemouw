package com.nemouw.auth.service;

import com.nemouw.auth.repository.SessionRepository;
import com.nemouw.auth.repository.UserRepository;
import com.nemouw.auth.repository.UserRepository.EmailIdentity;
import com.nemouw.auth.repository.VerificationTokenRepository;
import com.nemouw.enums.LocaleCode;
import com.nemouw.enums.UserStatus;
import com.nemouw.enums.VerificationPurpose;
import com.nemouw.error.Errors;
import com.nemouw.mail.EmailSender;
import com.nemouw.util.TextNormalizer;
import com.nemouw.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import static com.nemouw.auth.dto.AuthDtos.ForgotPasswordRequest;
import static com.nemouw.auth.dto.AuthDtos.ResetPasswordRequest;
import static com.nemouw.util.TextNormalizer.emailLocalPart;

/**
 * Forgot-password and reset-password. Also, what an admin or syndicate invite uses
 * to set a first password, since the same token flow activates a pending account.
 */
@Service
public class PasswordResetService {
    private final UserRepository users;
    private final VerificationTokenRepository tokenRepo;
    private final PasswordHasher hasher;
    private final VerificationTokenService tokenService;
    private final VerificationTokenIssuer tokenIssuer;
    private final SessionRepository sessionRepo;
    private final EmailSender emailSender;
    private final TransactionTemplate tx;

    public PasswordResetService(UserRepository users, VerificationTokenRepository tokenRepo, PasswordHasher hasher,
                                VerificationTokenService tokenService, VerificationTokenIssuer tokenIssuer,
                                SessionRepository sessionRepo, EmailSender emailSender, TransactionTemplate tx) {
        this.users = users;
        this.tokenRepo = tokenRepo;
        this.hasher = hasher;
        this.tokenService = tokenService;
        this.tokenIssuer = tokenIssuer;
        this.sessionRepo = sessionRepo;
        this.emailSender = emailSender;
        this.tx = tx;
    }

    public void forgotPassword(ForgotPasswordRequest req) {
        new Validator().requiredEmail("email", req.email()).throwIfInvalid();
        String email = TextNormalizer.normalizeEmail(req.email());

        EmailIdentity identity = users.findByEmail(email).orElse(null);
        if (identity == null || identity.userStatus() == UserStatus.DELETED || identity.userStatus() == UserStatus.SUSPENDED) {
            return;   // silent, same 202
        }

        var issued = tx.execute(status ->
                tokenIssuer.issueResetToken(identity.userId(), identity.identifierId())
                        .orElseThrow(() -> Errors.tooManyRequests(Math.max(1,
                                tokenIssuer.secondsUntilAllowed(identity.identifierId(),
                                        VerificationPurpose.PASSWORD_RESET)))));
        LocaleCode locale = LocaleCode.orDefault(req.locale());
        emailSender.sendPasswordReset(email, issued.token(), locale);
    }

    public void resetPassword(ResetPasswordRequest req) {
        new Validator()
                .required("token", req.token())
                .required("new_password", req.newPassword())
                .throwIfInvalid();

        String tokenHash = tokenService.hashToken(req.token().trim());

        String email = tokenRepo.findLiveIdentifier(VerificationPurpose.PASSWORD_RESET, tokenHash)
                .orElseThrow(Errors::invalidOrExpiredCode);

        new Validator()
                .password("new_password", req.newPassword(), emailLocalPart(email))
                .throwIfInvalid();

        String passwordHash = hasher.hash(req.newPassword());

        tx.executeWithoutResult(status -> {
            // Atomic single use. Any throw below rolls the delete back, so a failed reset keeps the token.
            var token = tokenRepo.consumeByHash(VerificationPurpose.PASSWORD_RESET, tokenHash)
                    .orElseThrow(Errors::invalidOrExpiredCode);

            UserStatus userStatus = users.lockStatus(token.userId()).orElse(null);
            if (userStatus == null || userStatus == UserStatus.DELETED || userStatus == UserStatus.SUSPENDED) {
                throw Errors.invalidOrExpiredCode();
            }

            users.replacePassword(token.userId(), passwordHash);
            if (userStatus == UserStatus.PENDING) {
                users.activate(token.userId(), token.identifierId());
            }
            // A leftover signup OTP is stale now, and for a just-activated user it is meaningless
            tokenRepo.deleteByUserAndPurpose(token.userId(), VerificationPurpose.SIGNUP_OTP);
            sessionRepo.revokeAllForUser(token.userId());
        });
    }
}
