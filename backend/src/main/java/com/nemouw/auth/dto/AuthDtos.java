package com.nemouw.auth.dto;

import com.nemouw.auth.model.IssuedSession;
import com.nemouw.auth.model.IssuedSetupToken;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.nemouw.auth.AuthConstants.ACCESS_TTL_SECONDS;
import static com.nemouw.auth.AuthConstants.SETUP_TTL_SECONDS;

public final class AuthDtos {
    private AuthDtos() {}

    // No password here: it is chosen after the email is verified (see SetPasswordRequest).
    public record SignupRequest(String role, String firstName, String lastName, String email, String locale) {}

    public record SignupResponse(UUID userId, String email, OffsetDateTime otpExpiresAt) {}

    public record VerifyOtpRequest(String email, String code) {}

    // Verified, but still no session: the only thing the caller may do next is set a password.
    public record PasswordSetupResponse(UUID userId, int expiresIn) {
        public static PasswordSetupResponse from(IssuedSetupToken t) {
            return new PasswordSetupResponse(t.userId(), SETUP_TTL_SECONDS);
        }
    }

    public record SetPasswordRequest(String password) {}

    public record ResendOtpRequest(String email, String locale) {}

    public record LoginRequest(String identifier, String password) {}

    public record ForgotPasswordRequest(String email, String locale) {}

    public record ResetPasswordRequest(String token, String newPassword) {}

    // What the client is allowed to see.
    public record SessionResponse(UUID userId, String role, int expiresIn) {
        public static SessionResponse from(IssuedSession s) {
            return new SessionResponse(s.userId(), s.role(), ACCESS_TTL_SECONDS);
        }
    }
}