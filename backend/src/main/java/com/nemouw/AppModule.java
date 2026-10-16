package com.nemouw;

import com.nemouw.controller.*;
import com.nemouw.http.ErrorHandling;
import com.nemouw.middleware.AuthMiddleware;
import com.nemouw.middleware.CsrfGuard;
import com.nemouw.repository.*;
import com.nemouw.security.*;
import com.nemouw.service.*;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

import static com.nemouw.middleware.CORSFilter.enableCORS;
import static com.nemouw.middleware.RateLimitFilter.runDdosShield;
import static spark.Spark.path;

public class AppModule {

    public record Secrets(String jwtSecret, String otpSecret) {
    }

    /**
     * Filters, then routes. Main and the integration tests both call this, so tests run the real wiring.
     */
    public static void configure(Jdbi jdbi, Secrets secrets, EmailSender emailSender, boolean ddosShield) {
        SessionRepository sessions = new SessionRepository();
        UserRepository users = new UserRepository();
        VerificationTokenRepository verificationTokens = new VerificationTokenRepository();
        PasswordHasher hasher = new PasswordHasher();
        VerificationTokenService verificationTokenService = new VerificationTokenService(secrets.otpSecret());
        SessionService sessionService = new SessionService(sessions, secrets.jwtSecret());
        VerificationTokenIssuer verificationTokenIssuer = new VerificationTokenIssuer(verificationTokenService, verificationTokens);
        SessionRepository sessionRepository = new SessionRepository();

        // Filters run in registration order
        ErrorHandling.install();
        enableCORS();
        if (ddosShield) runDdosShield();
        CsrfGuard.install();
        new AuthMiddleware(jdbi, sessionService, sessions).install();

        AuthDependencies dependencies = new AuthDependencies(jdbi, users, verificationTokens, hasher, verificationTokenService, verificationTokenIssuer, sessionService, sessionRepository, emailSender);
        AuthService auth = new AuthService(dependencies);
        PasswordResetService passwordReset = new PasswordResetService(dependencies);

        List<Controller> controllers = List.of(new AuthController(auth, passwordReset));
        path("/v1", () -> controllers.forEach(Controller::register));
    }
}