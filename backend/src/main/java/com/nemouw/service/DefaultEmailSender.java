package com.nemouw.service;

import com.nemouw.util.EmailTemplates;
import com.nemouw.util.EmailUtil;

import static com.nemouw.Constants.OTP_TTL_SECONDS;
import java.util.Map;

public class DefaultEmailSender implements EmailSender {
    @Override
    public void sendSignupOtp(String toEmail, String code) {
        long minutes = OTP_TTL_SECONDS / 60;

        Map<String, String> vars = Map.of("code", code, "minutes", String.valueOf(minutes));
        EmailUtil.send(toEmail,
                "Your Nemouw verification code",
                EmailTemplates.html("signup-otp", vars),
                EmailTemplates.text("signup-otp", vars));
    }
}