package com.nemouw.user.signup;

import com.nemouw.common.config.AppProperties;
import com.nemouw.user.session.AuthCookies;
import com.nemouw.user.session.ClientInfo;
import com.nemouw.user.session.IssuedSession;
import com.nemouw.user.session.SessionDtos.SessionResponse;
import com.nemouw.user.signup.SignupDtos.PasswordSetupResponse;
import com.nemouw.user.signup.SignupDtos.ResendOtpRequest;
import com.nemouw.user.signup.SignupDtos.SetPasswordRequest;
import com.nemouw.user.signup.SignupDtos.SignupRequest;
import com.nemouw.user.signup.SignupDtos.SignupResponse;
import com.nemouw.user.signup.SignupDtos.VerifyOtpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

import static com.nemouw.user.AuthConstants.SETUP_TTL_SECONDS;

@RestController
@RequestMapping("/auth")
public class SignupController {
    private final SignupService signup;
    private final AuthCookies cookies;
    private final int accessTtlSeconds;

    SignupController(SignupService signup, AuthCookies cookies, AppProperties props) {
        this.signup = signup;
        this.cookies = cookies;
        this.accessTtlSeconds = props.security().accessTtlSeconds();
    }

    @PostMapping("/signup")
    ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signup.signup(req));
    }

    @PostMapping("/verify-otp")
    ResponseEntity<PasswordSetupResponse> verifyOtp(@RequestBody VerifyOtpRequest req) {
        IssuedSetupToken setup = signup.verifyOtp(req);
        return ResponseEntity.ok()
                .headers(cookies.setupHeaders(setup.token(), SETUP_TTL_SECONDS))
                .body(new PasswordSetupResponse(SETUP_TTL_SECONDS));
    }

    @PostMapping("/resend-otp")
    ResponseEntity<Void> resendOtp(@RequestBody ResendOtpRequest req) {
        signup.resendOtp(req);
        return ResponseEntity.accepted().build();
    }

    /**
     * Final signup step. Spring Security only lets a valid setup token through (see SecurityConfig), and the
     * token's subject is the account to finish, so the caller cannot name another user.
     */
    @PostMapping("/password/setup")
    ResponseEntity<SessionResponse> setPassword(@RequestBody SetPasswordRequest req,
                                                @AuthenticationPrincipal Jwt jwt, ClientInfo client) {
        IssuedSession session = signup.setPassword(UUID.fromString(Objects.requireNonNull(jwt.getSubject())), req, client);
        return ResponseEntity.ok()
                .headers(cookies.sessionHeaders(session))
                .body(SessionResponse.from(session, accessTtlSeconds));
    }
}