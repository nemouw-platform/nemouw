package com.nemouw.common.mail;

import com.nemouw.common.config.AppProperties;
import com.resend.Resend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/** Exactly one EmailTransport exists, chosen by app.mail.delivery. A missing or unknown value fails startup. */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "app.mail.delivery", havingValue = "live")
    EmailTransport resendTransport(AppProperties props) {
        String key = props.mail().resendApiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("app.mail.delivery=live requires RESEND_API_KEY");
        }
        return new ResendEmailTransport(new Resend(key));
    }

    @Bean
    @ConditionalOnProperty(name = "app.mail.delivery", havingValue = "file")
    EmailTransport fileTransport(AppProperties props) {
        return new FileEmailTransport(Path.of(props.mail().fileDir()));
    }
}