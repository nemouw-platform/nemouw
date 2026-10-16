package com.nemouw.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record SignupRequest(String role, String firstName, String lastName, String email, String password) {}

    public record SignupResponse(UUID userId, String email, OffsetDateTime otpExpiresAt) {}

    public record VerifyOtpRequest(String email, String code) {}

    // Server-side only: contains secrets. NEVER return this from a route. It goes into cookies.
    public record IssuedSession(String accessToken, String refreshToken, UUID userId, String role) {}

    public record ResendOtpRequest(String email) {}

    public record LoginRequest(String identifier, String password) {}

    public record ForgotPasswordRequest(String email) {}

    public record ResetPasswordRequest(String token, String newPassword) {}

    // What the client is allowed to see.
    public record SessionResponse(UUID userId, String role, int expiresIn) {
        public static SessionResponse from(IssuedSession s) {
            return new SessionResponse(s.userId(), s.role(), com.nemouw.Constants.ACCESS_TTL_SECONDS);
        }
    }

}