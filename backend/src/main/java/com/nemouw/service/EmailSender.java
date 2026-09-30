package com.nemouw.service;

import java.time.OffsetDateTime;

public interface EmailSender {
    void sendSignupOtp(String toEmail, String code, OffsetDateTime expiresAt);
}