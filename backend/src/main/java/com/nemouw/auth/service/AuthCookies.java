package com.nemouw.auth.service;

import com.nemouw.auth.model.IssuedSession;
import com.nemouw.auth.model.IssuedSetupToken;
import com.nemouw.config.AppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.nemouw.auth.AuthConstants.ACCESS_TTL_SECONDS;
import static com.nemouw.auth.AuthConstants.REFRESH_TTL_DAYS;
import static com.nemouw.auth.AuthConstants.SETUP_TTL_SECONDS;

/** Builds the two session cookies. Returns headers so controllers stay free of servlet types. */
@Component
public class AuthCookies {
    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";

    // The refresh cookie is only sent to the auth endpoints, never with ordinary API calls
    private static final String REFRESH_PATH = "/v1/auth";

    private final boolean secure;

    public AuthCookies(AppProperties props) {
        this.secure = props.security().cookieSecure();
    }

    public HttpHeaders sessionHeaders(IssuedSession s) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE,
                cookie(ACCESS, s.accessToken(), Duration.ofSeconds(ACCESS_TTL_SECONDS), "/", "Lax"));
        headers.add(HttpHeaders.SET_COOKIE,
                cookie(REFRESH, s.refreshToken(), Duration.ofDays(REFRESH_TTL_DAYS), REFRESH_PATH, "Strict"));
        return headers;
    }

    /**
     * The password-setup token travels in the same access cookie, so Spring Security reads it like any other
     * token. It has its own short lifetime and no refresh cookie, so it can never be renewed into a session.
     */
    public HttpHeaders setupHeaders(IssuedSetupToken t) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE,
                cookie(ACCESS, t.token(), Duration.ofSeconds(SETUP_TTL_SECONDS), "/", "Lax"));
        return headers;
    }

    /** Max-Age=0 deletes a cookie, but only if Path matches the one it was set with. */
    public HttpHeaders clearHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie(ACCESS, "", Duration.ZERO, "/", "Lax"));
        headers.add(HttpHeaders.SET_COOKIE, cookie(REFRESH, "", Duration.ZERO, REFRESH_PATH, "Strict"));
        return headers;
    }

    private String cookie(String name, String value, Duration maxAge, String path, String sameSite) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(maxAge)
                .sameSite(sameSite)
                .build()
                .toString();
    }
}