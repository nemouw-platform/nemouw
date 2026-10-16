package com.nemouw.service;

import com.nemouw.enums.UserStatus;
import com.nemouw.enums.VerificationPurpose;
import com.nemouw.error.Errors;
import com.nemouw.model.AuthDtos.ForgotPasswordRequest;
import com.nemouw.model.AuthDtos.ResetPasswordRequest;
import com.nemouw.repository.SessionRepository;
import com.nemouw.repository.UserRepository;
import com.nemouw.repository.UserRepository.EmailIdentity;
import com.nemouw.repository.VerificationTokenRepository;
import com.nemouw.security.PasswordHasher;
import com.nemouw.util.TextNormalizer;
import com.nemouw.validation.Validator;
import org.jdbi.v3.core.Jdbi;

import static com.nemouw.util.TextNormalizer.emailLocalPart;

/**
 * Forgot-password and reset-password. Also, what an admin or syndicate invite uses
 * to set a first password, since the same token flow activates a pending account.
 */
public class PasswordResetService {
    private final Jdbi jdbi;
    private final UserRepository users;
    private final VerificationTokenRepository tokenRepo;
    private final PasswordHasher hasher;
    private final VerificationTokenService tokenService;
    private final VerificationTokenIssuer tokenIssuer;
    private final SessionRepository sessionRepo;
    private final EmailSender emailSender;

    public PasswordResetService(AuthDependencies d) {
        this.jdbi = d.jdbi();
        this.users = d.users();
        this.tokenRepo = d.tokenRepo();
        this.hasher = d.hasher();
        this.tokenService = d.tokenService();
        this.tokenIssuer = d.tokenIssuer();
        this.sessionRepo = d.sessionRepo();
        this.emailSender = d.emailSender();
    }

    public void forgotPassword(ForgotPasswordRequest req) {
        new Validator().requiredEmail("email", req.email()).throwIfInvalid();
        String email = TextNormalizer.normalizeEmail(req.email());

        EmailIdentity identity = jdbi.withHandle(h -> users.findByEmail(h, email)).orElse(null);
        if (identity == null || identity.userStatus() == UserStatus.DELETED || identity.userStatus() == UserStatus.SUSPENDED) {
            return;   // silent, same 202
        }

        var issued = jdbi.inTransaction(h ->
                tokenIssuer.issueResetToken(h, identity.userId(), identity.identifierId())
                        .orElseThrow(() -> Errors.tooManyRequests(Math.max(1,
                                tokenIssuer.secondsUntilAllowed(h, identity.identifierId(),
                                        VerificationPurpose.PASSWORD_RESET)))));
        emailSender.sendPasswordReset(email, issued.token());
    }

    public void resetPassword(ResetPasswordRequest req) {
        new Validator()
                .required("token", req.token())
                .required("new_password", req.newPassword())
                .throwIfInvalid();

        String tokenHash = tokenService.hashToken(req.token().trim());

        String email = jdbi.withHandle(h ->
                        tokenRepo.findLiveIdentifier(h, VerificationPurpose.PASSWORD_RESET, tokenHash))
                .orElseThrow(Errors::invalidOrExpiredCode);

        new Validator()
                .password("new_password", req.newPassword(), emailLocalPart(email))
                .throwIfInvalid();

        String passwordHash = hasher.hash(req.newPassword());

        jdbi.inTransaction(h -> {
            // Atomic single use. Any throw below rolls the delete back, so a failed reset keeps the token.
            var token = tokenRepo.consumeByHash(h, VerificationPurpose.PASSWORD_RESET, tokenHash)
                    .orElseThrow(Errors::invalidOrExpiredCode);

            UserStatus status = users.lockStatus(h, token.userId()).orElse(null);
            if (status == null || status == UserStatus.DELETED || status == UserStatus.SUSPENDED) throw Errors.invalidOrExpiredCode();

            users.replacePassword(h, token.userId(), passwordHash);
            if (status == UserStatus.PENDING) {
                users.activate(h, token.userId(), token.identifierId());
            }
            // A leftover signup OTP is stale now, and for a just-activated user it is meaningless
            tokenRepo.deleteByUserAndPurpose(h, token.userId(), VerificationPurpose.SIGNUP_OTP);
            sessionRepo.revokeAllForUser(h, token.userId());
            return token.userId();
        });
    }
}