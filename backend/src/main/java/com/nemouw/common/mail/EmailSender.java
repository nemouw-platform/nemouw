package com.nemouw.common.mail;

import com.nemouw.common.config.AppProperties;
import com.nemouw.common.enums.LocaleCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Composes transactional emails and hands them to the EmailTransport synchronously. A failure is
 * logged as EMAIL_UNDELIVERED and never fails the user's request.
 */
@Service
public class EmailSender {
    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final EmailTransport transport;
    private final String from;
    private final String urlBase;
    private final EmailTemplates templates;

    EmailSender(EmailTransport transport, AppProperties props, EmailTemplates templates) {
        this.transport = transport;
        this.from = props.mail().from();
        this.urlBase = props.mail().urlBase();
        this.templates = templates;
    }

    public void sendInvite(String to, String token, Duration ttl, LocaleCode locale) {
        send(EmailType.INVITE, to, locale, Map.of(
                "link", resetLink(token),
                "hours", String.valueOf(ttl.toHours())));
    }

    public void sendSignupOtp(String to, String code, Duration ttl, LocaleCode locale) {
        send(EmailType.SIGNUP_OTP, to, locale, Map.of(
                "code", code,
                "minutes", String.valueOf(ttl.toMinutes())));
    }

    public void sendPasswordReset(String to, String token, Duration ttl, LocaleCode locale) {
        send(EmailType.PASSWORD_RESET, to, locale, Map.of(
                "link", resetLink(token),
                "minutes", String.valueOf(ttl.toMinutes())));
    }

    private String resetLink(String token) {
        return urlBase + "/reset-password#token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private void send(EmailType type, String to, LocaleCode locale, Map<String, Object> vars) {
        try {
            EmailTemplates.Rendered mail = templates.render(type, locale, vars);
            String id = transport.send(new OutgoingEmail(from, to, mail.subject(), mail.html(), mail.text()));
            log.info("Email sent type={} to={} id={}", type, to, id);
        } catch (Exception e) {
            // Never log the mail body or variables: they contain the OTP or reset link.
            log.error("EMAIL_UNDELIVERED type={} to={} cause={}", type, to, e.toString());
        }
    }
}