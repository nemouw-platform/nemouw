package com.nemouw.media.storage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MediaRow(UUID id, String s3Key, String mimeType, OffsetDateTime createdAt) {
}