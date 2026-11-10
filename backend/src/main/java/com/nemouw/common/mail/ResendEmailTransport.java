package com.nemouw.common.mail;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;

final class ResendEmailTransport implements EmailTransport {
    private final Resend resend;

    ResendEmailTransport(Resend resend) {
        this.resend = resend;
    }

    @Override
    public String send(OutgoingEmail m) throws Exception {
        return resend.emails().send(CreateEmailOptions.builder()
                .from(m.from()).to(m.to())
                .subject(m.subject()).html(m.html()).text(m.text())
                .build()).getId();
    }
}