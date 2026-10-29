package com.nemouw.specialist.profile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Stopgap until the media module owns URL resolution. Returns null while no base URL is configured. */
@Component
class MediaUrls {
    private final String base;

    MediaUrls(@Value("${app.media.public-base-url:}") String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    String urlFor(String s3Key) {
        if (s3Key == null || base.isBlank()) return null;
        return base + "/" + s3Key;
    }
}