package com.nemouw.auth.controller;

import com.nemouw.auth.dto.AuthDtos.*;
import com.nemouw.auth.model.ClientInfo;
import com.nemouw.auth.model.IssuedSession;
import com.nemouw.auth.model.IssuedSetupToken;
import com.nemouw.auth.service.AuthCookies;
import com.nemouw.auth.service.AuthService;
import com.nemouw.auth.service.PasswordResetService;
import com.nemouw.auth.service.SignupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/** Mounted at /v1/auth (the /v1 prefix is added for every controller in WebConfig). */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final SignupService signup;
    private final AuthService auth;
    private final PasswordResetService passwordReset;
    private final AuthCookies cookies;

    public AuthController(SignupService signup, AuthService auth, PasswordResetService passwordReset,
                          AuthCookies cookies) {
        this.signup = signup;
        this.auth = auth;
        this.passwordReset = passwordReset;
        this.cookies = cookies;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signup.signup(req));
    }

    /** Verifies the mailbox. The reply is NOT a session: it only unlocks POST /auth/password/setup. */
    @PostMapping("/verify-otp")
    public ResponseEntity<PasswordSetupResponse> verifyOtp(@RequestBody VerifyOtpRequest req) {
        IssuedSetupToken setup = signup.verifyOtp(req);
        return ResponseEntity.ok()
                .headers(cookies.setupHeaders(setup))
                .body(PasswordSetupResponse.from(setup));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@RequestBody ResendOtpRequest req) {
        signup.resendOtp(req);
        return ResponseEntity.accepted().build();
    }

    /**
     * Final signup step. Spring Security only lets a valid setup token through (see SecurityConfig), and the
     * token's subject is the account to finish, so the caller cannot name another user.
     */
    @PostMapping("/password/setup")
    public ResponseEntity<SessionResponse> setPassword(@RequestBody SetPasswordRequest req,
                                                       @AuthenticationPrincipal Jwt jwt, ClientInfo client) {
        return startSession(signup.setPassword(UUID.fromString(Objects.requireNonNull(jwt.getSubject())), req, client));
    }

    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@RequestBody LoginRequest req, ClientInfo client) {
        return startSession(auth.login(req, client));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SessionResponse> refresh(
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken) {
        return startSession(auth.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken) {
        auth.logout(refreshToken);
        // Cookies are cleared even if the cookie was missing or the token unknown
        return ResponseEntity.noContent().headers(cookies.clearHeaders()).build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        passwordReset.forgotPassword(req);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest req) {
        passwordReset.resetPassword(req);
        return ResponseEntity.ok().build();
    }

    /** Tokens go into cookies only. The body is SessionResponse, which has no secrets. */
    private ResponseEntity<SessionResponse> startSession(IssuedSession s) {
        return ResponseEntity.ok()
                .headers(cookies.sessionHeaders(s))
                .body(SessionResponse.from(s));
    }
}