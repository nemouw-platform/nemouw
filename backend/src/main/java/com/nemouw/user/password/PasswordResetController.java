package com.nemouw.user.password;

import com.nemouw.user.password.PasswordDtos.ForgotPasswordRequest;
import com.nemouw.user.password.PasswordDtos.ResetPasswordRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/password")
public class PasswordResetController {
    private final PasswordResetService passwordReset;

    PasswordResetController(PasswordResetService passwordReset) {
        this.passwordReset = passwordReset;
    }

    @PostMapping("/forgot")
    ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        passwordReset.forgotPassword(req);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset")
    ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest req) {
        passwordReset.resetPassword(req);
        return ResponseEntity.ok().build();
    }
}