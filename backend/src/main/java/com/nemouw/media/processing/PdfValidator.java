package com.nemouw.media.processing;

import com.nemouw.common.exception.Errors;

import java.nio.charset.StandardCharsets;

public final class PdfValidator {
    private static final int TAIL_BYTES = 1024;

    private PdfValidator() {
    }

    /** Header is already matched by magic bytes; a truncated or non-PDF file lacks the end marker. */
    public static byte[] validated(byte[] pdf) {
        int from = Math.max(0, pdf.length - TAIL_BYTES);
        String tail = new String(pdf, from, pdf.length - from, StandardCharsets.ISO_8859_1);
        if (!tail.contains("%%EOF")) throw Errors.invalidField("file", "the file could not be read");
        return pdf;
    }
}