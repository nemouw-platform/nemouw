package com.nemouw.auth.model;

import java.util.UUID;

/**
 * Server-side only: contains a secret. It goes into the access cookie, never into a response body.
 * Proves "this person controls the mailbox"; it is NOT a session and only unlocks the password-setup step.
 */
public record IssuedSetupToken(String token, UUID userId) {

    /** Keeps the token out of logs and exception messages. */
    @Override
    public String toString() {
        return "IssuedSetupToken[userId=" + userId + "]";
    }
}