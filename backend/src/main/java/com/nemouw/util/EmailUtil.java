package com.nemouw.util;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.nemouw.Config.resendApiKey;

public final class EmailUtil {
    private static final Logger log = LogManager.getLogger(EmailUtil.class);

    private EmailUtil() {}

    // Resend Client
    private static final class ClientHolder {
        static final Resend CLIENT = new Resend(resendApiKey());
    }

    public static void send(String to, String subject, String html, String text) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("onboarding@resend.dev")
                .to(to)
                .subject(subject)
                .html(html)
                .text(text)
                .build();
        try {
            CreateEmailResponse data = ClientHolder.CLIENT.emails().send(params);
            log.info("Email sent, subject='{}', id={}", subject, data.getId());
        } catch (ResendException e) {
            log.error("Failed to send email, subject='{}'", subject, e);
            throw new RuntimeException(e);
        }
    }
}