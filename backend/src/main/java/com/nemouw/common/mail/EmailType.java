package com.nemouw.common.mail;

/**
 * Registry of transactional emails. To add one: add an entry here, create
 * resources/mail/<template>.html and .txt, and add the message keys to all messages_*.properties.
 */
public enum EmailType {
    SIGNUP_OTP("signup-otp", "mail.signup_otp"),
    PASSWORD_RESET("password-reset", "mail.password_reset"),
    INVITE("invite", "mail.invite");

    private final String template;
    private final String messageKey;

    EmailType(String template, String messageKey) {
        this.template = template;
        this.messageKey = messageKey;
    }

    String htmlTemplate() { return template + ".html"; }

    String textTemplate() { return template + ".txt"; }

    String subjectKey() { return messageKey + ".subject"; }

    String preheaderKey() { return messageKey + ".preheader"; }
}