package com.nemouw.common.mail;

public interface EmailTransport {
    /** Delivers the email and returns an id (the Resend id, or the file name). Throws on failure. */
    String send(OutgoingEmail email) throws Exception;
}