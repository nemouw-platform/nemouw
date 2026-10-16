package com.nemouw.service;

import com.nemouw.repository.SessionRepository;
import com.nemouw.repository.UserRepository;
import com.nemouw.repository.VerificationTokenRepository;
import com.nemouw.security.PasswordHasher;
import org.jdbi.v3.core.Jdbi;

/**
 * Everything the auth services share. Add a dependency here once and
 * both {@link AuthService} and {@link PasswordResetService} can use it.
 */
public record AuthDependencies(
        Jdbi jdbi,
        UserRepository users,
        VerificationTokenRepository tokenRepo,
        PasswordHasher hasher,
        VerificationTokenService tokenService,
        VerificationTokenIssuer tokenIssuer,
        SessionService sessions,
        SessionRepository sessionRepo,
        EmailSender emailSender) {
}
