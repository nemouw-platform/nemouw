package com.nemouw.common.mail;

/** A fully composed email. The transport only delivers it and never touches content. */
public record OutgoingEmail(String from, String to, String subject, String html, String text) {
}