package com.nemouw.common.mail;

import com.nemouw.common.enums.LocaleCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.nemouw.support.Fixtures.props;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailSenderTest {

    private static final String TO = "amira.haddad@clinicmail.org";
    private static final Duration RESET_TTL = Duration.ofHours(1);
    private static final Duration OTP_TTL = Duration.ofMinutes(10);

    private final EmailTransport transport = mock(EmailTransport.class);
    private final EmailTemplates templates = mock(EmailTemplates.class);
    private final EmailSender sender = new EmailSender(transport, props(), templates);

    /** Stops each send right after rendering, so no transport call happens and the variables can be inspected. */
    private AtomicReference<Map<String, Object>> captureTemplateVariables() {
        AtomicReference<Map<String, Object>> captured = new AtomicReference<>();
        when(templates.render(any(), any(), any())).thenAnswer(call -> {
            captured.set(call.getArgument(2));
            throw new IllegalStateException("stop before the transport call");
        });
        return captured;
    }

    @Test
    void buildsTheResetLinkFromTheConfiguredBaseUrlWithTheTokenInTheFragment() {
        AtomicReference<Map<String, Object>> variables = captureTemplateVariables();

        sender.sendPasswordReset(TO, "Zx9-Kq2_mT7vLb4NwR1pYc8HdJ5sUe0AfG3iOa6BkQ", RESET_TTL, LocaleCode.EN);

        assertThat(variables.get()).containsEntry("link",
                "https://app.nemouw.com/reset-password#token=Zx9-Kq2_mT7vLb4NwR1pYc8HdJ5sUe0AfG3iOa6BkQ");
        assertThat(variables.get()).containsEntry("minutes", "60");
    }

    @Test
    void urlEncodesATokenThatContainsReservedCharacters() {
        AtomicReference<Map<String, Object>> variables = captureTemplateVariables();

        sender.sendPasswordReset(TO, "a+b/c=d", RESET_TTL, LocaleCode.EN);

        assertThat(variables.get()).containsEntry("link", "https://app.nemouw.com/reset-password#token=a%2Bb%2Fc%3Dd");
    }

    @Test
    void passesTheOtpCodeAndItsTenMinuteLifetimeToTheTemplate() {
        AtomicReference<Map<String, Object>> variables = captureTemplateVariables();

        sender.sendSignupOtp(TO, "482913", OTP_TTL, LocaleCode.FR);

        assertThat(variables.get()).containsEntry("code", "482913").containsEntry("minutes", "10");
    }

    @Test
    void doesNotFailTheUsersRequestWhenTheTemplateCannotBeRendered() {
        captureTemplateVariables();

        assertThatCode(() -> sender.sendSignupOtp(TO, "482913", OTP_TTL, LocaleCode.EN))
                .doesNotThrowAnyException();
        verifyNothingWasSent();
    }

    @Test
    void handsTheRenderedMailToTheTransportWithTheConfiguredSender() throws Exception {
        when(templates.render(any(), any(), any()))
                .thenReturn(new EmailTemplates.Rendered("Your code: 482913", "<p>482913</p>", "482913"));
        when(transport.send(any())).thenReturn("msg-1");

        sender.sendSignupOtp(TO, "482913", OTP_TTL, LocaleCode.EN);

        verify(transport).send(new OutgoingEmail("Nemouw <no-reply@nemouw.com>", TO,
                "Your code: 482913", "<p>482913</p>", "482913"));
    }

    @Test
    void doesNotFailTheUsersRequestWhenTheTransportFails() throws Exception {
        when(templates.render(any(), any(), any()))
                .thenReturn(new EmailTemplates.Rendered("subject", "<p>html</p>", "text"));
        when(transport.send(any())).thenThrow(new IOException("resend is down"));

        assertThatCode(() -> sender.sendPasswordReset(TO, "token", OTP_TTL, LocaleCode.EN))
                .doesNotThrowAnyException();
    }

    private void verifyNothingWasSent() {
        try {
            verify(transport, never()).send(any());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}