package com.nemouw.user.signup;

import java.time.OffsetDateTime;

public final class SignupDtos {
    private SignupDtos() {}

    // No password here: it is chosen after the email is verified
    public record SignupRequest(String role, String firstName, String lastName, String email, String locale) {}

    public record SignupResponse(String email, OffsetDateTime otpExpiresAt) {}

    public record VerifyOtpRequest(String email, String code) {}

    public record PasswordSetupResponse(int expiresIn) {
    }

    public record SetPasswordRequest(String password) {}

    public record ResendOtpRequest(String email, String locale) {}
}