package com.nemouw.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.OffsetDateTime;

/** DEV ONLY: prints the OTP to the log. Replace before deploying. */
public class LoggingEmailSender implements EmailSender {
    private static final Logger log = LogManager.getLogger(LoggingEmailSender.class);

    @Override
    public void sendSignupOtp(String toEmail, String code, OffsetDateTime expiresAt) {
        log.info("[DEV EMAIL] to={} otp={} expires={}", toEmail, code, expiresAt);
    }
}