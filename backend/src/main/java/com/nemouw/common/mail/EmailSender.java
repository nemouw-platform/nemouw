package com.nemouw.common.mail;

import com.nemouw.common.config.AppProperties;
import com.nemouw.common.enums.LocaleCode;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Sends transactional emails through Resend. Rendering and delivery run on a bounded executor with retries,
 * and anything undeliverable is logged as EMAIL_UNDELIVERED.
 */
@Service
public class EmailSender {
    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_MS = {1_000, 4_000};   // waits after attempt 1 and 2

    private final Resend resend;
    private final String from;
    private final String urlBase;
    private final EmailTemplates templates;
    private final TaskExecutor executor;

    EmailSender(Resend resend, AppProperties props, EmailTemplates templates,
                @Qualifier(MailConfig.MAIL_EXECUTOR) TaskExecutor executor) {
        this.resend = resend;
        this.from = props.mail().from();
        this.urlBase = props.mail().urlBase();
        this.templates = templates;
        this.executor = executor;
    }

    public void sendInvite(String to, String token, Duration ttl, LocaleCode locale) {
        submit(EmailType.INVITE, to, locale, Map.of(
                "link", resetLink(token),
                "hours", String.valueOf(ttl.toHours())));
    }

    public void sendSignupOtp(String to, String code, Duration ttl, LocaleCode locale) {
        submit(EmailType.SIGNUP_OTP, to, locale, Map.of(
                "code", code,
                "minutes", String.valueOf(ttl.toMinutes())));
    }

    public void sendPasswordReset(String to, String token, Duration ttl, LocaleCode locale) {
        submit(EmailType.PASSWORD_RESET, to, locale, Map.of(
                "link", resetLink(token),
                "minutes", String.valueOf(ttl.toMinutes())));
    }

    private String resetLink(String token) {
        return urlBase + "/reset-password#token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private void submit(EmailType type, String to, LocaleCode locale, Map<String, Object> vars) {
        try {
            executor.execute(() -> deliver(type, to, locale, vars));
        } catch (TaskRejectedException e) {
            undelivered(type, to, e);   // pool and queue are full: don't fail the user's request
        }
    }

    private void deliver(EmailType type, String to, LocaleCode locale, Map<String, Object> vars) {
        EmailTemplates.Rendered mail;
        try {
            mail = templates.render(type, locale, vars);
        } catch (RuntimeException e) {
            undelivered(type, to, e);   // a template bug will not fix itself on retry
            return;
        }

        Exception last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String id = resend.emails().send(CreateEmailOptions.builder()
                        .from(from).to(to)
                        .subject(mail.subject()).html(mail.html()).text(mail.text())
                        .build()).getId();
                log.info("Email sent type={} to={} id={}", type, to, id);
                return;
            } catch (Exception e) {
                last = e;
                log.warn("Email attempt {}/{} failed type={} to={} err={}",
                        attempt, MAX_ATTEMPTS, type, to, e.toString());
                if (attempt < MAX_ATTEMPTS && !pause(BACKOFF_MS[attempt - 1])) break;
            }
        }
        undelivered(type, to, last);
    }

    /** Never log the mail body or variables: they contain the OTP or reset link. */
    private void undelivered(EmailType type, String to, Throwable cause) {
        log.error("EMAIL_UNDELIVERED type={} to={} cause={}", type, to, String.valueOf(cause));
    }

    /** False if interrupted (app shutting down): stop retrying. */
    private static boolean pause(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}