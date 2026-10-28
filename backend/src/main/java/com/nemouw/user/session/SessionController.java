package com.nemouw.user.session;

import com.nemouw.common.config.AppProperties;
import com.nemouw.user.session.SessionDtos.LoginRequest;
import com.nemouw.user.session.SessionDtos.SessionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class SessionController {
    private final AuthService auth;
    private final AuthCookies cookies;
    private final int accessTtlSeconds;

    SessionController(AuthService auth, AuthCookies cookies, AppProperties props) {
        this.auth = auth;
        this.cookies = cookies;
        this.accessTtlSeconds = props.security().accessTtlSeconds();
    }

    @PostMapping("/login")
    ResponseEntity<SessionResponse> login(@RequestBody LoginRequest req, ClientInfo client) {
        return startSession(auth.login(req, client));
    }

    @PostMapping("/refresh")
    ResponseEntity<SessionResponse> refresh(
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken) {
        return startSession(auth.refresh(refreshToken));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken) {
        auth.logout(refreshToken);
        // Cookies are cleared even if the cookie was missing or the token unknown
        return ResponseEntity.noContent().headers(cookies.clearHeaders()).build();
    }

    /**
     * Tokens go into cookies only. The body is SessionResponse, which has no secrets.
     */
    private ResponseEntity<SessionResponse> startSession(IssuedSession s) {
        return ResponseEntity.ok()
                .headers(cookies.sessionHeaders(s))
                .body(SessionResponse.from(s, accessTtlSeconds));
    }
}