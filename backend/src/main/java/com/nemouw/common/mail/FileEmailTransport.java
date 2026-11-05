package com.nemouw.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local-only transport: writes each email as an RFC-compliant .eml file and sends nothing. Plain Java, no mail library.
 * Subject and non-ASCII From names use RFC 2047 encoded-words. The HTML part is always base64 (76-char lines, CRLF),
 * and the text part is base64 only if it has a line over the 998-byte limit.
 */
final class FileEmailTransport implements EmailTransport {
    private static final Logger log = LoggerFactory.getLogger(FileEmailTransport.class);

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final String CRLF = "\r\n";
    private static final int MAX_LINE_BYTES = 998;   // RFC 5322 hard limit, excluding CRLF
    private static final Pattern NAME_ADDR = Pattern.compile("^(.*?)\\s*<([^<>]+)>$");
    /** 45 UTF-8 bytes -> 60 base64 chars; plus 12 chars of "=?UTF-8?B?...?=" stays under the 75-char word limit. */
    private static final int WORD_BYTES = 45;
    private static final Base64.Encoder MIME_BASE64 =
            Base64.getMimeEncoder(76, CRLF.getBytes(StandardCharsets.UTF_8));

    private final Path dir;

    FileEmailTransport(Path dir) {
        this.dir = dir.toAbsolutePath();
        log.warn("EMAIL DELIVERY IS DISABLED: emails are written to {} and NOT sent", this.dir);
    }

    @Override
    public String send(OutgoingEmail m) throws Exception {
        requireNoLineBreaks("from", m.from());
        requireNoLineBreaks("to", m.to());

        String boundary = "=_nemouw_" + UUID.randomUUID().toString().replace("-", "");
        var eml = new StringBuilder(m.html().length() * 2 + m.text().length() + 512);

        header(eml, "From", encodeAddress(m.from()));
        header(eml, "To", m.to());
        header(eml, "Subject", encodeWords(m.subject()));
        header(eml, "Date", ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME));
        header(eml, "Message-ID", "<" + UUID.randomUUID() + "@nemouw.local>");
        header(eml, "MIME-Version", "1.0");
        header(eml, "Content-Type", "multipart/alternative; boundary=\"" + boundary + "\"");
        eml.append(CRLF);

        part(eml, boundary, "text/plain", m.text(), false);   // plain text first, HTML last (preferred by clients)
        part(eml, boundary, "text/html", m.html(), true);
        eml.append("--").append(boundary).append("--").append(CRLF);

        Files.createDirectories(dir);
        String name = STAMP.format(LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 6)
                + "-" + m.to().replaceAll("[^A-Za-z0-9._-]", "_") + ".eml";
        // The content is pure ASCII except for the 8bit text part, so the explicit UTF-8 here is what keeps that part correct.
        Files.writeString(dir.resolve(name), eml, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);

        log.info("Email written to {}", dir.resolve(name));   // path only, never the body (it holds the OTP or link)
        return name;
    }

    private static void header(StringBuilder eml, String name, String value) {
        eml.append(name).append(": ").append(value).append(CRLF);
    }

    private static void part(StringBuilder eml, String boundary, String type, String body, boolean forceBase64) {
        String normalized = body.replace("\r\n", "\n").replace("\n", CRLF);
        boolean base64 = forceBase64 || hasOverlongLine(normalized);

        eml.append("--").append(boundary).append(CRLF);
        eml.append("Content-Type: ").append(type).append("; charset=UTF-8").append(CRLF);
        eml.append("Content-Transfer-Encoding: ").append(base64 ? "base64" : "8bit").append(CRLF);
        eml.append(CRLF);
        eml.append(base64
                ? MIME_BASE64.encodeToString(normalized.getBytes(StandardCharsets.UTF_8))   // no trailing separator
                : normalized).append(CRLF);
    }

    private static boolean hasOverlongLine(String crlfText) {
        for (String line : crlfText.split(CRLF, -1)) {
            if (line.getBytes(StandardCharsets.UTF_8).length > MAX_LINE_BYTES) return true;
        }
        return false;
    }

    /** "Nemouw <no-reply@x.com>": only the display name is encoded, never the address. */
    private static String encodeAddress(String address) {
        Matcher m = NAME_ADDR.matcher(address.trim());
        if (!m.matches() || isAscii(m.group(1))) return address;
        return encodeWords(m.group(1).replaceAll("^\"|\"$", "")) + " <" + m.group(2) + ">";
    }

    /** RFC 2047 encoded-words (base64, UTF-8), split on code point boundaries so no character is cut in half. */
    private static String encodeWords(String value) {
        String s = value.replaceAll("[\\r\\n]+", " ");   // a header value must never contain a line break
        if (isAscii(s)) return s;

        List<String> words = new ArrayList<>();
        var chunk = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            byte[] bytes = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8);
            if (chunk.size() + bytes.length > WORD_BYTES) {
                words.add(word(chunk));
                chunk.reset();
            }
            chunk.writeBytes(bytes);
        }
        if (chunk.size() > 0) words.add(word(chunk));
        return String.join(CRLF + " ", words);   // folded: each continuation line starts with a space
    }

    private static String word(ByteArrayOutputStream chunk) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(chunk.toByteArray()) + "?=";
    }

    private static boolean isAscii(String s) {
        return s.chars().allMatch(c -> c < 0x80);
    }

    private static void requireNoLineBreaks(String field, String value) {
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("line break in email " + field);   // header injection guard
        }
    }
}