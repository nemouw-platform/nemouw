package com.nemouw.user.session;

import java.util.UUID;

/** Server-side only: contains secrets. NEVER return this from a controller. It goes into cookies. */
public record IssuedSession(String accessToken, String refreshToken, UUID userId, String role) {

    /** Keeps the tokens out of logs and exception messages. */
    @Override
    public String toString() {
        return "IssuedSession[userId=" + userId + ", role=" + role + "]";
    }
}