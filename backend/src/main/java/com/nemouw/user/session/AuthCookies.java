package com.nemouw.user.session;

import com.nemouw.common.config.AppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.nemouw.common.ApiPaths.API_PREFIX;

/** Builds the two session cookies. Returns headers so controllers stay free of servlet types. */
@Component
public class AuthCookies {
    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";

    // The refresh cookie is only sent to the auth endpoints, never with ordinary API calls
    private static final String REFRESH_PATH = API_PREFIX + "/auth";

    private final boolean secure;
    private final int accessTtlSeconds;
    private final int refreshTtlDays;

    AuthCookies(AppProperties props) {
        this.secure = props.security().cookieSecure();
        this.accessTtlSeconds = props.security().accessTtlSeconds();
        this.refreshTtlDays = props.security().refreshTtlDays();
    }

    public HttpHeaders sessionHeaders(IssuedSession s) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE,
                cookie(ACCESS, s.accessToken(), Duration.ofSeconds(accessTtlSeconds), "/", "Lax"));
        headers.add(HttpHeaders.SET_COOKIE,
                cookie(REFRESH, s.refreshToken(), Duration.ofDays(refreshTtlDays), REFRESH_PATH, "Strict"));
        return headers;
    }

    /**
     * The password-setup token travels in the same access cookie, so Spring Security reads it like any other
     * token. It has its own short lifetime and no refresh cookie, so it can never be renewed into a session.
     */
    public HttpHeaders setupHeaders(String token, int ttlSeconds) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE,
                cookie(ACCESS, token, Duration.ofSeconds(ttlSeconds), "/", "Lax"));
        return headers;
    }

    /** Max-Age=0 deletes a cookie, but only if Path matches the one it was set with. */
    HttpHeaders clearHeaders() {
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