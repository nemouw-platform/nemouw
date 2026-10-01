package com.nemouw;

import com.nemouw.database.DatabaseConnection;
import com.nemouw.http.ErrorHandling;
import com.nemouw.middleware.AuthMiddleware;
import com.nemouw.middleware.CsrfGuard;
import com.nemouw.repository.SessionRepository;
import com.nemouw.security.TokenService;

import static com.nemouw.middleware.CORSFilter.enableCORS;
import static com.nemouw.database.DatabaseInit.initializeDB;
import static com.nemouw.middleware.RateLimitFilter.runDdosShield;
import static spark.Spark.*;

public class Main {

    public static void main(String[] args) {
        SessionRepository sessions = new SessionRepository();
        TokenService tokenService = new TokenService(sessions, Config.jwtSecret());

        // fail fast, before touching the DB or opening a port
        Config.validate();

        // Initialize the database first. If it crashes, the server never opens a dead port.
        initializeDB();

        // Open HTTP Server Ports and Configurations
        port(4567);

        ErrorHandling.install();
        enableCORS();
        runDdosShield();
        CsrfGuard.install();
        new AuthMiddleware(DatabaseConnection.jdbi(), tokenService, sessions).install();

        // Tells all bots to stay out of API subdomain entirely
        get("/robots.txt", (request, response) -> {
            response.type("text/plain");
            return "User-agent: *\nDisallow: /";
        });

        AppModule.registerRoutes();
    }
}