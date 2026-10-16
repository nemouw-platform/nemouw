package com.nemouw.service;

public interface EmailSender {
    void sendSignupOtp(String toEmail, String code);
    void sendPasswordReset(String toEmail, String token);
}