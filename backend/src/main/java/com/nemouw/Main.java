package com.nemouw;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nemouw.controller.AuthController;
import com.nemouw.database.DatabaseConnection;
import com.nemouw.http.ApiException;
import com.nemouw.http.ErrorHandling;
import com.nemouw.repository.SessionRepository;
import com.nemouw.repository.UserRepository;
import com.nemouw.repository.VerificationTokenRepository;
import com.nemouw.service.*;
import io.github.cdimascio.dotenv.Dotenv;

import static com.nemouw.http.CORSFilter.enableCORS;
import static spark.Spark.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nemouw.database.DatabaseInit.initializeDB;

public class Main {

    public static void main(String[] args) {
        // Initialize the database first. If it crashes, the server never opens a dead port.
        initializeDB();

        // Open HTTP Server Ports and Configurations
        port(4567);

        ErrorHandling.install();

        enableCORS();
        runDdosShield();

        // Tells all bots to stay out of API subdomain entirely
        get("/robots.txt", (request, response) -> {
            response.type("text/plain");
            return "User-agent: *\nDisallow: /";
        });

        // Endpoints to test
        Dotenv env = Dotenv.load();
        SessionRepository sessions = new SessionRepository();
        VerificationTokenRepository verificationTokens = new VerificationTokenRepository();
        OtpService otpService = new OtpService(env.get("OTP_HASH_SECRET"));
        TokenService tokenService = new TokenService(sessions, env.get("JWT_SECRET"));

        new AuthController(new AuthService(
                DatabaseConnection.jdbi(), new UserRepository(), verificationTokens,
                new PasswordHasher(), otpService, new OtpIssuer(otpService, verificationTokens),
                tokenService, new LoggingEmailSender())).register();
    }

    /**
     * In-memory DDoS Rate Limiter (Production-Ready)
     */
    private static void runDdosShield() {
        // Enforce the rate limit strictly per-second to prevent bursts
        int requestLimitPerSecond = Config.getDdosShieldRequestLimitPerSecond();
        int retryWindowSeconds = 1;

        // Self-cleaning memory shield. IPs are completely erased from RAM exactly 1 second after they stop attacking.
        Cache<String, AtomicInteger> accessPerSecond = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();

        before("/*", (request, response) -> {
            if (request.requestMethod().equalsIgnoreCase("OPTIONS")) {
                return;
            }

            String ip = request.headers("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.ip();
            }

            AtomicInteger counter = accessPerSecond.get(ip, key -> new AtomicInteger(0));
            int currentAccessCount = counter.incrementAndGet();
            if (currentAccessCount > requestLimitPerSecond) {
                throw ApiException.tooManyRequests(retryWindowSeconds);
            }
        });
    }
}