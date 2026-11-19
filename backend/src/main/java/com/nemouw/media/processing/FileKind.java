package com.nemouw.media.processing;

import java.util.Optional;

/** File types recognised by their leading bytes. */
public enum FileKind {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    PDF("application/pdf", "pdf");

    private final String mimeType;
    private final String extension;

    FileKind(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<FileKind> detect(byte[] h) {
        if (h == null) return Optional.empty();
        if (at(h, 0, 0xFF, 0xD8, 0xFF)) return Optional.of(JPEG);
        if (at(h, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return Optional.of(PNG);
        if (at(h, 0, '%', 'P', 'D', 'F', '-')) return Optional.of(PDF);
        if (at(h, 0, 'R', 'I', 'F', 'F') && at(h, 8, 'W', 'E', 'B', 'P')) return Optional.of(WEBP);
        return Optional.empty();
    }

    private static boolean at(byte[] h, int offset, int... expected) {
        if (h.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((h[offset + i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }
}