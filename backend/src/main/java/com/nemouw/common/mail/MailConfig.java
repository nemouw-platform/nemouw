package com.nemouw.common.mail;

import com.nemouw.common.config.AppProperties;
import com.resend.Resend;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MailConfig {
    public static final String MAIL_EXECUTOR = "mailExecutor";

    @Bean
    Resend resend(AppProperties props) {
        return new Resend(props.mail().resendApiKey());   // one shared instance
    }

    /** Bounded on purpose: when full, EmailSender gets a TaskRejectedException and logs instead of queueing forever. */
    @Bean(name = MAIL_EXECUTOR)
    ThreadPoolTaskExecutor mailExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("mail-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);          // max only grows once the queue is full, so keep equal to core
        executor.setQueueCapacity(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}