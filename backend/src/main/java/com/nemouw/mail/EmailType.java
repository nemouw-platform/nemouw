package com.nemouw.mail;

/**
 * Registry of transactional emails. To add one: add an entry here, create
 * resources/mail/<template>.html and .txt, and add the message keys to all messages_*.properties.
 */
public enum EmailType {
    SIGNUP_OTP("signup-otp", "mail.signup_otp"),
    PASSWORD_RESET("password-reset", "mail.password_reset");

    private final String template;
    private final String messageKey;

    EmailType(String template, String messageKey) {
        this.template = template;
        this.messageKey = messageKey;
    }

    public String htmlTemplate() { return template + ".html"; }

    public String textTemplate() { return template + ".txt"; }

    public String subjectKey() { return messageKey + ".subject"; }

    public String preheaderKey() { return messageKey + ".preheader"; }
}