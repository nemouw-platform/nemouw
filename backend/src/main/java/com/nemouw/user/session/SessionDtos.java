package com.nemouw.user.session;

import java.util.UUID;

public final class SessionDtos {
    private SessionDtos() {}

    public record LoginRequest(String identifier, String password) {}

    public record SessionResponse(UUID userId, String role, int expiresIn) {
        public static SessionResponse from(IssuedSession s, int accessTtlSeconds) {
            return new SessionResponse(s.userId(), s.role(), accessTtlSeconds);
        }
    }
}