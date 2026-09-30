package com.nemouw.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record SignupRequest(String role, String firstName, String lastName, String email, String password) {}
    public record SignupResponse(UUID userId, String email, OffsetDateTime otpExpiresAt) {}}