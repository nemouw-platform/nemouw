package com.nemouw.user.session;

/**
 * JWT claim names and values shared by the code that signs tokens (session, signup) and the code that checks
 * them (ActiveSessionValidator, SecurityConfig). Changing one invalidates every token already issued.
 */
public final class TokenClaims {
    private TokenClaims() {
    }

    public static final String ROLE = "role";
    public static final String SESSION_ID = "sid";
    public static final String SCOPE = "scope";

    /** Scope of the setup token issued after the OTP. Spring Security turns it into SCOPE_password_setup. */
    public static final String PASSWORD_SETUP_SCOPE = "password_setup";
}