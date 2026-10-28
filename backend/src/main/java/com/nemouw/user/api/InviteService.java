package com.nemouw.user.api;

import com.nemouw.user.identity.UserRepository;
import com.nemouw.user.identity.UserRepository.EmailIdentity;
import com.nemouw.user.token.VerificationTokens;
import com.nemouw.common.enums.LocaleCode;
import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.user.token.VerificationPurpose;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.util.TextNormalizer;
import com.nemouw.common.mail.EmailSender;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import  com.nemouw.user.token.VerificationTokens.IssuedResetToken;

import java.time.Duration;
import java.util.UUID;

import static com.nemouw.user.AuthConstants.INVITE_TTL_SECONDS;

/**
 * Accounts that someone else creates (syndicate staff now, admins later). The account starts pending and the
 * person activates it through the password-reset flow, which proves they own the mailbox.
 * Methods join the caller's transaction; emails are sent separately, after it commits.
 */
@Service
public class InviteService {
    public record Invite(UUID userId, String email, IssuedResetToken token) {
    }

    private final UserRepository users;
    private final VerificationTokens tokens;
    private final EmailSender emailSender;

    InviteService(UserRepository users, VerificationTokens tokens, EmailSender emailSender) {
        this.users = users;
        this.tokens = tokens;
        this.emailSender = emailSender;
    }

    /** Throws EMAIL_TAKEN if the address already belongs to any account. */
    public Invite createPendingAccount(UserRole role, String firstName, String lastName, String email) {
        String normalized = TextNormalizer.normalizeEmail(email);
        if (users.findByEmail(normalized).isPresent()) throw Errors.emailTaken();
        try {
            UUID userId = users.insertUser(role, firstName.trim(), lastName.trim());
            UUID identifierId = users.insertEmailIdentifier(userId, normalized);
            IssuedResetToken token = tokens.issueInviteToken(userId, identifierId).orElseThrow();
            return new Invite(userId, normalized, token);
        } catch (DuplicateKeyException e) {
            throw Errors.emailTaken();   // two requests raced for the same address
        }
    }

    /**
     * Fixes a mistyped invite address: points the pending account at the new email, kills the old link and
     * issues a fresh one. Passing the current address just resends. Refuses accounts already activated.
     */
    public Invite changePendingEmail(UUID userId, String email) {
        String normalized = TextNormalizer.normalizeEmail(email);

        UserStatus status = users.lockStatus(userId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (status != UserStatus.PENDING) throw new ApiException(ErrorCode.ACCOUNT_ALREADY_ACTIVE);

        EmailIdentity current = users.findPrimaryEmail(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!current.email().equals(normalized)) {
            if (users.findByEmail(normalized).isPresent()) throw Errors.emailTaken();
            try {
                users.updateEmail(current.identifierId(), normalized);
            } catch (DuplicateKeyException e) {
                throw Errors.emailTaken();   // raced with another signup or invite
            }
        }

        // Delete first: the old link dies, and the fresh row is not blocked by the resend cooldown.
        tokens.revoke(userId, VerificationPurpose.PASSWORD_RESET);
        IssuedResetToken token = tokens.issueInviteToken(userId, current.identifierId()).orElseThrow();
        return new Invite(userId, normalized, token);
    }

    /** Call only after the transaction that created the invite has committed. */
    public void sendInvite(Invite invite, LocaleCode locale) {
        emailSender.sendInvite(invite.email(), invite.token().token(), Duration.ofSeconds(INVITE_TTL_SECONDS), locale);
    }
}