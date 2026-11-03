package com.nemouw.media.api;

import java.time.Instant;
import java.util.UUID;

/** {@code url} is null for private kinds. */
public record StoredMedia(UUID id, String url, String mimeType, Instant createdAt) {
}