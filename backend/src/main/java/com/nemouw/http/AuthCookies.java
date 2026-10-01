package com.nemouw.http;

import com.nemouw.Config;
import com.nemouw.model.AuthDtos.IssuedSession;
import spark.Response;

import static com.nemouw.Constants.ACCESS_TTL_SECONDS;
import static com.nemouw.Constants.REFRESH_TTL_DAYS;

public final class AuthCookies {
    private AuthCookies() {}

    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";

    // The refresh cookie is only sent to the auth endpoints, never with ordinary API calls
    private static final String REFRESH_PATH = "/v1/auth";

    public static void set(Response res, IssuedSession s) {
        add(res, ACCESS, s.accessToken(), ACCESS_TTL_SECONDS, "/", "Lax");
        add(res, REFRESH, s.refreshToken(), REFRESH_TTL_DAYS * 86400, REFRESH_PATH, "Strict");
    }

    /** Max-Age=0 deletes a cookie, but only if Path matches the one it was set with. */
    public static void clear(Response res) {
        add(res, ACCESS, "", 0, "/", "Lax");
        add(res, REFRESH, "", 0, REFRESH_PATH, "Strict");
    }

    private static void add(Response res, String name, String value, int maxAgeSeconds, String path, String sameSite) {
        StringBuilder sb = new StringBuilder(name).append('=').append(value)
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; Path=").append(path)
                .append("; HttpOnly; SameSite=").append(sameSite);
        if (Config.cookieSecure()) sb.append("; Secure");
        // addHeader, not header(): there are two Set-Cookie headers and neither may replace the other
        res.raw().addHeader("Set-Cookie", sb.toString());
    }
}