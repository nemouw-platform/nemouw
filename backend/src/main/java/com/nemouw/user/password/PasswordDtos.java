package com.nemouw.user.password;

public final class PasswordDtos {
    private PasswordDtos() {}

    public record ForgotPasswordRequest(String email, String locale) {}

    public record ResetPasswordRequest(String token, String newPassword) {}
}