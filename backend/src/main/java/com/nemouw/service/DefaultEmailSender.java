package com.nemouw.service;

import com.nemouw.util.EmailTemplates;
import com.nemouw.util.EmailUtil;

import static com.nemouw.Constants.OTP_TTL_SECONDS;
import static com.nemouw.Constants.RESET_TTL_SECONDS;

import java.util.Map;

public class DefaultEmailSender implements EmailSender {
    private final String resetUrlBase;

    public DefaultEmailSender(String urlBase) {
        this.resetUrlBase = urlBase + "/reset-password";
    }

    @Override
    public void sendSignupOtp(String toEmail, String code) {
        long minutes = OTP_TTL_SECONDS / 60;

        Map<String, String> vars = Map.of("code", code, "minutes", String.valueOf(minutes));
        EmailUtil.send(toEmail,
                "Your Nemouw verification code",
                EmailTemplates.html("signup-otp", vars),
                EmailTemplates.text("signup-otp", vars));
    }

    @Override
    public void sendPasswordReset(String toEmail, String token) {
        String link = resetUrlBase + "#token=" + token;   // fragment keeps the token out of server logs
        Map<String, String> vars = Map.of(
                "link", link,
                "minutes", String.valueOf(RESET_TTL_SECONDS / 60));
        EmailUtil.send(toEmail,
                "Reset your Nemouw password",
                EmailTemplates.html("password-reset", vars),
                EmailTemplates.text("password-reset", vars));
    }
}