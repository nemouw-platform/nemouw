package com.nemouw.common.mail;

import com.nemouw.common.enums.LocaleCode;
import com.resend.Resend;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.nemouw.support.Fixtures.props;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailSenderTest {

    private final Resend resend = mock(Resend.class);
    private final EmailTemplates templates = mock(EmailTemplates.class);
    private final TaskExecutor executor = mock(TaskExecutor.class);

    /** Runs the delivery immediately and stops it right after rendering, so no network call or retry sleep happens. */
    private AtomicReference<Map<String, Object>> captureTemplateVariables(EmailSender sender) {
        AtomicReference<Map<String, Object>> captured = new AtomicReference<>();
        when(templates.render(any(), any(), any())).thenAnswer(call -> {
            captured.set(call.getArgument(2));
            throw new IllegalStateException("stop before the network call");
        });
        return captured;
    }

    @Test
    void buildsTheResetLinkFromTheConfiguredBaseUrlWithTheTokenInTheFragment() {
        EmailSender sender = new EmailSender(resend, props(), templates, Runnable::run);
        AtomicReference<Map<String, Object>> variables = captureTemplateVariables(sender);

        sender.sendPasswordReset("amira.haddad@clinicmail.org", "Zx9-Kq2_mT7vLb4NwR1pYc8HdJ5sUe0AfG3iOa6BkQ", LocaleCode.EN);

        assertThat(variables.get()).containsEntry("link",
                "https://app.nemouw.com/reset-password#token=Zx9-Kq2_mT7vLb4NwR1pYc8HdJ5sUe0AfG3iOa6BkQ");
        assertThat(variables.get()).containsEntry("minutes", "60");
    }

    @Test
    void urlEncodesATokenThatContainsReservedCharacters() {
        EmailSender sender = new EmailSender(resend, props(), templates, Runnable::run);
        AtomicReference<Map<String, Object>> variables = captureTemplateVariables(sender);

        sender.sendPasswordReset("amira.haddad@clinicmail.org", "a+b/c=d", LocaleCode.EN);

        assertThat(variables.get()).containsEntry("link", "https://app.nemouw.com/reset-password#token=a%2Bb%2Fc%3Dd");
    }

    @Test
    void passesTheOtpCodeAndItsTenMinuteLifetimeToTheTemplate() {
        EmailSender sender = new EmailSender(resend, props(), templates, Runnable::run);
        AtomicReference<Map<String, Object>> variables = captureTemplateVariables(sender);

        sender.sendSignupOtp("amira.haddad@clinicmail.org", "482913", LocaleCode.FR);

        assertThat(variables.get()).containsEntry("code", "482913").containsEntry("minutes", "10");
    }

    @Test
    void doesNotFailTheUsersRequestWhenTheTemplateCannotBeRendered() {
        EmailSender sender = new EmailSender(resend, props(), templates, Runnable::run);
        captureTemplateVariables(sender);

        assertThatCode(() -> sender.sendSignupOtp("amira.haddad@clinicmail.org", "482913", LocaleCode.EN))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNotFailTheUsersRequestWhenTheMailQueueIsFull() {
        doThrow(new TaskRejectedException("mail queue is full")).when(executor).execute(any());
        EmailSender sender = new EmailSender(resend, props(), templates, executor);

        assertThatCode(() -> sender.sendPasswordReset("amira.haddad@clinicmail.org", "token", LocaleCode.EN))
                .doesNotThrowAnyException();
    }
}
