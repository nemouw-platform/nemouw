package com.nemouw;

import com.nemouw.controller.*;
import com.nemouw.database.DatabaseConnection;
import com.nemouw.repository.*;
import com.nemouw.security.*;
import com.nemouw.service.*;
import org.jdbi.v3.core.Jdbi;
import java.util.List;
import static spark.Spark.path;

public class AppModule {

    public static void registerRoutes() {
        // Initialize core infrastructure dependencies
        Jdbi jdbi = DatabaseConnection.jdbi();
        SessionRepository sessions = new SessionRepository();
        UserRepository users = new UserRepository();
        VerificationTokenRepository verificationTokens = new VerificationTokenRepository();

        // Initialize core services
        OtpService otpService = new OtpService(Config.otpHashSecret());
        TokenService tokenService = new TokenService(sessions, Config.jwtSecret());
        OtpIssuer otpIssuer = new OtpIssuer(otpService, verificationTokens);
        EmailSender emailSender = new DefaultEmailSender();

        AuthService authService = new AuthService(
                jdbi, users, verificationTokens, new PasswordHasher(),
                otpService, otpIssuer, tokenService, emailSender
        );

        // Collect all controllers into the list
        List<Controller> controllers = List.of(
                new AuthController(authService)
        );

        // Register all routes under the API prefix
        path("/v1", () -> controllers.forEach(Controller::register));
    }
}
